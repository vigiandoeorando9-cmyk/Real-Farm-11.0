package com.example.storage

import android.content.Context
import android.os.Build
import com.example.auth.ACCOUNT_INVALID_CREDENTIALS_ERROR
import com.example.auth.computeSaltedCodeHash
import com.example.auth.normalizePersonalCode
import com.example.auth.normalizePlayerUsername
import com.example.domain.AccountSyncStatus
import com.example.domain.FarmAccountUser
import com.example.domain.GameSettings
import com.example.domain.HubPackEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.SecureRandom
import java.time.Instant

private val syncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

@Serializable
data class FarmSyncServerConfig(
    val serverEndpointUrl: String? = null,
    val lastRemoteSyncIso: String? = null,
    val lastLocalExportIso: String? = null,
)

@Serializable
data class FarmAccountSyncPayload(
    val accountUid: String,
    val username: String,
    val credentialSaltHex: String,
    val credentialCodeHashSha256: String,
    val devicePlatform: String,
    val deviceName: String,
    val exportedAtIso: String,
    val settings: GameSettings,
    val installedModPackIds: List<String>,
    val favoriteModPackIds: List<String>,
    val careerSlots: List<PersistedCareerEnvelope>,
)

@Serializable
data class FarmAccountSyncBundle(
    val app: String = "FARM_BRASIL_SYNC",
    val schemaVersion: Int = 1,
    val checksumSha256: String,
    val payload: FarmAccountSyncPayload,
)

sealed interface SyncBundleRestoreResult {
    data class Ok(
        val payload: FarmAccountSyncPayload,
        val restoredSlotsCount: Int,
    ) : SyncBundleRestoreResult

    data class Err(val message: String) : SyncBundleRestoreResult
}

/**
 * Abstraction prepared for a future dedicated Farm Brasil server (Mobile <-> PC).
 * While no online server is configured, operations stay strictly local and honest.
 */
interface FarmSyncTransport {
    val isServerConfigured: Boolean
    val endpointUrl: String?
    suspend fun pushSnapshot(bundleJson: String): Result<String>
    suspend fun pullSnapshot(username: String, personalCodeHash: String): Result<String?>
}

class LocalOnlyFarmSyncTransport : FarmSyncTransport {
    override val isServerConfigured: Boolean = false
    override val endpointUrl: String? = null

    override suspend fun pushSnapshot(bundleJson: String): Result<String> =
        Result.failure(IllegalStateException("Nenhum servidor online do Farm Brasil configurado. Dados salvos localmente neste dispositivo."))

    override suspend fun pullSnapshot(username: String, personalCodeHash: String): Result<String?> =
        Result.failure(IllegalStateException("Nenhum servidor online do Farm Brasil configurado."))
}

object FarmAccountSyncManager {
    private fun syncConfigFile(context: Context): File =
        File(context.filesDir, "farm_brasil_sync_config.json")

    fun loadSyncConfig(context: Context): FarmSyncServerConfig {
        val f = syncConfigFile(context)
        if (!f.exists()) return FarmSyncServerConfig()
        return runCatching {
            syncJson.decodeFromString<FarmSyncServerConfig>(f.readText(Charsets.UTF_8))
        }.getOrDefault(FarmSyncServerConfig())
    }

    fun saveSyncConfig(context: Context, config: FarmSyncServerConfig) {
        runCatching {
            syncConfigFile(context).writeText(syncJson.encodeToString(config), Charsets.UTF_8)
        }
    }

    fun buildHonestSyncStatus(context: Context): AccountSyncStatus {
        val cfg = loadSyncConfig(context)
        val hasServer = !cfg.serverEndpointUrl.isNullOrBlank()
        val msg = if (hasServer) {
            "Servidor configurado (${cfg.serverEndpointUrl}). A sincronização automática depende da disponibilidade desse servidor."
        } else {
            "Dados salvos localmente neste dispositivo. A sincronização automática online entrará em operação quando houver um servidor oficial do Farm Brasil conectado. Enquanto isso, use Exportar / Restaurar Pacote da Conta para transferir entre celular e computador."
        }
        return AccountSyncStatus(
            isServerConfigured = hasServer,
            serverEndpointUrl = cfg.serverEndpointUrl,
            lastLocalSaveIso = cfg.lastLocalExportIso,
            lastRemoteSyncIso = cfg.lastRemoteSyncIso,
            devicePlatform = "ANDROID_MOBILE",
            statusMessage = msg,
        )
    }

    fun exportSyncBundleJson(
        context: Context,
        user: FarmAccountUser,
        settings: GameSettings,
        hubPacks: List<HubPackEntity>,
        hubFavorites: List<String>,
    ): String {
        val normCode = normalizePersonalCode(user.personalCode)
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val codeHash = computeSaltedCodeHash(normCode, saltHex)

        val careersDir = File(context.filesDir, "farm_brasil_careers")
        val savedEnvelopes = (0 until MAX_CAREER_SLOTS).mapNotNull { slot ->
            val f = File(careersDir, "slot_$slot.json")
            if (!f.exists()) null
            else runCatching {
                syncJson.decodeFromString<PersistedCareerEnvelope>(f.readText(Charsets.UTF_8))
            }.getOrNull()
        }

        val devName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android" }
        val nowIso = Instant.now().toString()
        val payload = FarmAccountSyncPayload(
            accountUid = user.uid,
            username = user.displayName,
            credentialSaltHex = saltHex,
            credentialCodeHashSha256 = codeHash,
            devicePlatform = "ANDROID_MOBILE",
            deviceName = devName,
            exportedAtIso = nowIso,
            settings = settings,
            installedModPackIds = hubPacks.filter { it.installed }.map { it.packId },
            favoriteModPackIds = hubFavorites,
            careerSlots = savedEnvelopes,
        )
        val payloadRaw = syncJson.encodeToString(payload)
        val bundle = FarmAccountSyncBundle(
            app = "FARM_BRASIL_SYNC",
            schemaVersion = 1,
            checksumSha256 = computeSha256Hex(payloadRaw),
            payload = payload,
        )
        val cfg = loadSyncConfig(context).copy(lastLocalExportIso = nowIso)
        saveSyncConfig(context, cfg)
        return syncJson.encodeToString(bundle)
    }

    fun verifyAndRestoreSyncBundle(
        context: Context,
        rawJson: String,
        usernameInput: String,
        personalCodeInput: String,
    ): SyncBundleRestoreResult {
        val bundle = runCatching {
            syncJson.decodeFromString<FarmAccountSyncBundle>(rawJson)
        }.getOrElse {
            return SyncBundleRestoreResult.Err("Arquivo de sincronização Farm Brasil inválido.")
        }

        if (bundle.app != "FARM_BRASIL_SYNC") {
            return SyncBundleRestoreResult.Err("Este arquivo não é um pacote de conta do Farm Brasil.")
        }

        val expectedChecksum = computeSha256Hex(syncJson.encodeToString(bundle.payload))
        if (expectedChecksum != bundle.checksumSha256) {
            return SyncBundleRestoreResult.Err("Falha de integridade SHA-256 no pacote da conta.")
        }

        val cleanUser = normalizePlayerUsername(usernameInput).lowercase()
        val normCode = normalizePersonalCode(personalCodeInput)
        val payload = bundle.payload
        val matchesUser = normalizePlayerUsername(payload.username).lowercase() == cleanUser
        val matchesCode = computeSaltedCodeHash(normCode, payload.credentialSaltHex) == payload.credentialCodeHashSha256
        if (!matchesUser || !matchesCode) {
            return SyncBundleRestoreResult.Err(ACCOUNT_INVALID_CREDENTIALS_ERROR)
        }

        var restored = 0
        for (env in payload.careerSlots) {
            val (ok, _) = saveWorldToSlot(context, env.state)
            if (ok) restored++
        }
        saveSettings(context, payload.settings)
        saveHubFavorites(context, payload.favoriteModPackIds)

        val currentPacks = loadHubPacks(context)
        val installedSet = payload.installedModPackIds.toSet()
        val updatedPacks = currentPacks.map { pack ->
            if (installedSet.contains(pack.packId)) {
                pack.copy(
                    installed = true,
                    enabled = true,
                    localPath = pack.localPath ?: "local://hub/${pack.packId}.json",
                    installedAtEpochMs = System.currentTimeMillis(),
                )
            } else pack
        }
        saveHubPacks(context, updatedPacks)

        return SyncBundleRestoreResult.Ok(
            payload = payload,
            restoredSlotsCount = restored,
        )
    }
}
