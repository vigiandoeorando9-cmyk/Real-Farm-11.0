package com.example.auth

import android.content.Context
import com.example.domain.FarmAccountUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant

sealed interface FarmAccountResult {
    data class Success(val user: FarmAccountUser, val isNewAccount: Boolean = false) : FarmAccountResult
    data class Error(val message: String) : FarmAccountResult
}

@Serializable
data class StoredFarmAccountRecord(
    val uid: String,
    val normalizedUsername: String,
    val displayName: String,
    val saltHex: String,
    val codeHashSha256: String,
    val personalCode: String,
    val warningAcknowledged: Boolean = false,
    val avatarStyle: String = "palha",
    val createdAtIso: String = "",
)

@Serializable
private data class FarmAccountsVault(
    val schemaVersion: Int = 1,
    val activeUid: String? = null,
    val accounts: List<StoredFarmAccountRecord> = emptyList(),
)

private val accountJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

private const val CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
private val secureRandom = SecureRandom()

const val ACCOUNT_CODE_WARNING_TEXT =
    "Não esqueça seu código pessoal! Anote esse código e guarde em um lugar seguro. Ele será necessário para recuperar sua conta e seus dados em outro dispositivo."

const val ACCOUNT_INVALID_CREDENTIALS_ERROR =
    "Nome ou código incorreto. Confira seus dados e tente novamente."

fun normalizePlayerUsername(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ")

fun normalizePersonalCode(raw: String): String =
    raw.trim().uppercase().replace(Regex("\\s+"), "")

fun computeSaltedCodeHash(normalizedCode: String, saltHex: String): String {
    val payload = "$saltHex::$normalizedCode::FARM_BRASIL_ACCOUNT_V1"
    val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

private fun randomCodeSegment(length: Int): String {
    val sb = StringBuilder(length)
    repeat(length) {
        sb.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)])
    }
    return sb.toString()
}

private fun randomSaltHex(): String {
    val bytes = ByteArray(16)
    secureRandom.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

class FarmAccountManager(context: Context) {
    private val appContext = context.applicationContext
    private val vaultFile = File(appContext.filesDir, "farm_brasil_accounts.json")

    private val _currentUserFlow = MutableStateFlow<FarmAccountUser?>(loadActiveUser())
    val currentUserFlow: Flow<FarmAccountUser?> = _currentUserFlow.asStateFlow()

    fun currentUser(): FarmAccountUser? = _currentUserFlow.value

    private fun readVault(): FarmAccountsVault {
        if (!vaultFile.exists()) return FarmAccountsVault()
        return runCatching {
            accountJson.decodeFromString<FarmAccountsVault>(vaultFile.readText(Charsets.UTF_8))
        }.getOrDefault(FarmAccountsVault())
    }

    private fun writeVault(vault: FarmAccountsVault) {
        runCatching {
            vaultFile.writeText(accountJson.encodeToString(vault), Charsets.UTF_8)
        }
    }

    private fun StoredFarmAccountRecord.toUser(): FarmAccountUser = FarmAccountUser(
        uid = uid,
        email = "",
        displayName = displayName,
        emailVerified = false,
        avatarStyle = avatarStyle,
        photoUrl = null,
        personalCode = personalCode,
        warningAcknowledged = warningAcknowledged,
        accountSource = "FARM_BRASIL_LOCAL",
        createdAtIso = createdAtIso,
    )

    private fun loadActiveUser(): FarmAccountUser? {
        val vault = readVault()
        val activeUid = vault.activeUid ?: return null
        return vault.accounts.find { it.uid == activeUid }?.toUser()
    }

    fun hasAnyRegisteredAccount(): Boolean = readVault().accounts.isNotEmpty()

    fun generateUniquePersonalCode(): String {
        val vault = readVault()
        repeat(32) {
            val candidate = "FB-${randomCodeSegment(4)}-${randomCodeSegment(4)}"
            val norm = normalizePersonalCode(candidate)
            val collides = vault.accounts.any {
                computeSaltedCodeHash(norm, it.saltHex) == it.codeHashSha256
            }
            if (!collides) return candidate
        }
        return "FB-${randomCodeSegment(4)}-${randomCodeSegment(4)}"
    }

    fun createAccount(usernameInput: String, personalCodeInput: String): FarmAccountResult {
        val cleanName = normalizePlayerUsername(usernameInput)
        if (cleanName.length < 2) {
            return FarmAccountResult.Error("Digite um nome de jogador com pelo menos 2 caracteres.")
        }
        if (cleanName.length > 32) {
            return FarmAccountResult.Error("O nome do jogador deve ter no máximo 32 caracteres.")
        }

        val normCode = normalizePersonalCode(personalCodeInput)
        if (normCode.length < 6) {
            return FarmAccountResult.Error("Informe um código pessoal válido (mínimo 6 caracteres) ou clique em GERAR CÓDIGO.")
        }
        if (!normCode.matches(Regex("^[A-Z0-9\\-]{6,24}$"))) {
            return FarmAccountResult.Error("Use apenas letras, números ou hífen (-) no código pessoal.")
        }

        val vault = readVault()
        val normUser = cleanName.lowercase()
        if (vault.accounts.any { it.normalizedUsername == normUser }) {
            return FarmAccountResult.Error("Já existe uma conta neste dispositivo com esse nome. Use 'JÁ TENHO UMA CONTA' ou escolha outro nome.")
        }

        val salt = randomSaltHex()
        val hash = computeSaltedCodeHash(normCode, salt)
        val uidSuffix = computeSaltedCodeHash("$normUser|$hash", salt).take(16)
        val uid = "fb_$uidSuffix"
        val nowIso = Instant.now().toString()

        val record = StoredFarmAccountRecord(
            uid = uid,
            normalizedUsername = normUser,
            displayName = cleanName,
            saltHex = salt,
            codeHashSha256 = hash,
            personalCode = normCode,
            warningAcknowledged = false,
            avatarStyle = "palha",
            createdAtIso = nowIso,
        )

        val updatedVault = vault.copy(
            activeUid = uid,
            accounts = vault.accounts + record,
        )
        writeVault(updatedVault)
        val user = record.toUser()
        _currentUserFlow.value = user
        return FarmAccountResult.Success(user = user, isNewAccount = true)
    }

    fun signInOrRecover(usernameInput: String, personalCodeInput: String): FarmAccountResult {
        val cleanName = normalizePlayerUsername(usernameInput)
        val normCode = normalizePersonalCode(personalCodeInput)
        if (cleanName.isBlank() || normCode.isBlank()) {
            return FarmAccountResult.Error(ACCOUNT_INVALID_CREDENTIALS_ERROR)
        }

        val vault = readVault()
        val normUser = cleanName.lowercase()
        val matched = vault.accounts.find { rec ->
            rec.normalizedUsername == normUser &&
                computeSaltedCodeHash(normCode, rec.saltHex) == rec.codeHashSha256
        } ?: return FarmAccountResult.Error(ACCOUNT_INVALID_CREDENTIALS_ERROR)

        val updatedVault = vault.copy(activeUid = matched.uid)
        writeVault(updatedVault)
        val user = matched.toUser()
        _currentUserFlow.value = user
        return FarmAccountResult.Success(user = user, isNewAccount = false)
    }

    fun registerRecoveredExternalAccount(
        username: String,
        personalCode: String,
        uidOverride: String? = null,
        createdAtIso: String? = null,
    ): FarmAccountResult {
        val cleanName = normalizePlayerUsername(username)
        val normCode = normalizePersonalCode(personalCode)
        if (cleanName.length < 2 || normCode.length < 6) {
            return FarmAccountResult.Error(ACCOUNT_INVALID_CREDENTIALS_ERROR)
        }

        val vault = readVault()
        val normUser = cleanName.lowercase()
        val existing = vault.accounts.find { it.normalizedUsername == normUser }
        if (existing != null) {
            if (computeSaltedCodeHash(normCode, existing.saltHex) != existing.codeHashSha256) {
                return FarmAccountResult.Error(ACCOUNT_INVALID_CREDENTIALS_ERROR)
            }
            val updated = existing.copy(warningAcknowledged = true)
            val nextVault = vault.copy(
                activeUid = updated.uid,
                accounts = vault.accounts.map { if (it.uid == updated.uid) updated else it },
            )
            writeVault(nextVault)
            val user = updated.toUser()
            _currentUserFlow.value = user
            return FarmAccountResult.Success(user, isNewAccount = false)
        }

        val salt = randomSaltHex()
        val hash = computeSaltedCodeHash(normCode, salt)
        val uid = uidOverride?.takeIf { it.isNotBlank() } ?: "fb_${computeSaltedCodeHash("$normUser|$hash", salt).take(16)}"
        val record = StoredFarmAccountRecord(
            uid = uid,
            normalizedUsername = normUser,
            displayName = cleanName,
            saltHex = salt,
            codeHashSha256 = hash,
            personalCode = normCode,
            warningAcknowledged = true,
            avatarStyle = "palha",
            createdAtIso = createdAtIso ?: Instant.now().toString(),
        )
        writeVault(vault.copy(activeUid = record.uid, accounts = vault.accounts + record))
        val user = record.toUser()
        _currentUserFlow.value = user
        return FarmAccountResult.Success(user, isNewAccount = false)
    }

    fun acknowledgeCodeWarning() {
        val active = _currentUserFlow.value ?: return
        val vault = readVault()
        val nextAccounts = vault.accounts.map { rec ->
            if (rec.uid == active.uid) rec.copy(warningAcknowledged = true) else rec
        }
        writeVault(vault.copy(accounts = nextAccounts))
        _currentUserFlow.value = active.copy(warningAcknowledged = true)
    }

    fun signOut() {
        val vault = readVault()
        writeVault(vault.copy(activeUid = null))
        _currentUserFlow.value = null
    }
}
