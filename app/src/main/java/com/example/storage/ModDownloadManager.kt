package com.example.storage

import android.content.Context
import android.os.StatFs
import com.example.domain.HubPackEntity
import com.example.domain.MOD_MISSING_FILE_ERROR
import com.example.domain.ModAssetDeliveryMode
import com.example.domain.ModDownloadProgress
import com.example.domain.ModDownloadStatus
import com.example.domain.StorageQuotaConfig
import com.example.domain.getModAssetDeliveryMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val DEFAULT_MOBILE_LIMIT_MB = 8192.0 // 8 GB

private val quotaJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private fun quotaConfigFile(context: Context): File =
    File(context.filesDir, "farm_brasil_quota_config.json")

private fun downloadStatesFile(context: Context): File =
    File(context.filesDir, "farm_brasil_mod_downloads.json")

private fun modBlobsDir(context: Context): File =
    File(context.filesDir, "farm_brasil_mods").apply { mkdirs() }

fun loadStorageQuotaConfig(context: Context): StorageQuotaConfig {
    val f = quotaConfigFile(context)
    if (!f.exists()) return StorageQuotaConfig()
    return try {
        quotaJson.decodeFromString<StorageQuotaConfig>(f.readText(Charsets.UTF_8))
    } catch (_: Exception) {
        StorageQuotaConfig()
    }
}

fun saveStorageQuotaConfig(context: Context, config: StorageQuotaConfig) {
    runCatching {
        quotaConfigFile(context).writeText(quotaJson.encodeToString(config), Charsets.UTF_8)
    }
}

data class QuotaCheckResult(
    val ok: Boolean,
    val currentUsedMb: Double,
    val limitMb: Double,
    val remainingMb: Double,
    val reason: String? = null,
)

fun getDeviceFreeSpaceMb(context: Context): Double = runCatching {
    val stat = StatFs(context.filesDir.absolutePath)
    (stat.availableBytes / (1024.0 * 1024.0) * 10.0).roundToInt() / 10.0
}.getOrDefault(2048.0)

fun checkQuotaBeforeDownload(
    packs: List<HubPackEntity>,
    incomingSizeMb: Double,
    quotaConfig: StorageQuotaConfig,
    deviceFreeSpaceMb: Double? = null,
): QuotaCheckResult {
    val usedMb = (packs.filter { it.installed }.sumOf { it.sizeMb } * 10.0).roundToInt() / 10.0
    val limitMb = quotaConfig.mobileLimitMb
    val remainingMb = max(0.0, limitMb - usedMb)
    if (deviceFreeSpaceMb != null && deviceFreeSpaceMb < incomingSizeMb) {
        return QuotaCheckResult(
            ok = false,
            currentUsedMb = usedMb,
            limitMb = limitMb,
            remainingMb = deviceFreeSpaceMb,
            reason = "Espaço livre insuficiente no dispositivo (${deviceFreeSpaceMb.roundToInt()} MB livres < ${incomingSizeMb.roundToInt()} MB).",
        )
    }
    if (quotaConfig.enforceMobileLimit && usedMb + incomingSizeMb > limitMb) {
        return QuotaCheckResult(
            ok = false,
            currentUsedMb = usedMb,
            limitMb = limitMb,
            remainingMb = remainingMb,
            reason = "Limite de armazenamento (${limitMb.roundToInt()} MB) excedido. Restam ${remainingMb.roundToInt()} MB.",
        )
    }
    return QuotaCheckResult(true, usedMb, limitMb, remainingMb, null)
}

class ModDownloadManager(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val _downloads = MutableStateFlow<Map<String, ModDownloadProgress>>(loadPersisted())
    val downloads: StateFlow<Map<String, ModDownloadProgress>> = _downloads.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()

    private fun loadPersisted(): Map<String, ModDownloadProgress> {
        val f = downloadStatesFile(appContext)
        if (!f.exists()) return emptyMap()
        return try {
            quotaJson.decodeFromString<Map<String, ModDownloadProgress>>(f.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun persist(map: Map<String, ModDownloadProgress>) {
        runCatching {
            downloadStatesFile(appContext).writeText(quotaJson.encodeToString(map), Charsets.UTF_8)
        }
    }

    private fun updateEntry(packId: String, entry: ModDownloadProgress?) {
        val next = _downloads.value.toMutableMap()
        if (entry == null) next.remove(packId) else next[packId] = entry
        _downloads.value = next
        persist(next)
    }

    fun getAvailableDeviceSpaceMb(): Double = getDeviceFreeSpaceMb(appContext)

    fun startDownload(
        pack: HubPackEntity,
        allPacks: List<HubPackEntity>,
        quotaConfig: StorageQuotaConfig,
        onComplete: (HubPackEntity) -> Unit,
        onError: (String) -> Unit = {},
    ) {
        val delivery = getModAssetDeliveryMode(pack)
        if (delivery == ModAssetDeliveryMode.EXTERNAL_FILE_REQUIRED) {
            updateEntry(
                pack.packId,
                ModDownloadProgress(pack.packId, ModDownloadStatus.ERROR, 0.0, pack.sizeMb, 0.0, 0, MOD_MISSING_FILE_ERROR)
            )
            onError(MOD_MISSING_FILE_ERROR)
            return
        }
        if (delivery == ModAssetDeliveryMode.WIP_BLOCKED) {
            val err = "Módulo em desenvolvimento: aguardando lançamento oficial compatível."
            updateEntry(
                pack.packId,
                ModDownloadProgress(pack.packId, ModDownloadStatus.ERROR, 0.0, pack.sizeMb, 0.0, 0, err)
            )
            onError(err)
            return
        }
        val check = checkQuotaBeforeDownload(
            packs = allPacks,
            incomingSizeMb = pack.sizeMb,
            quotaConfig = quotaConfig,
            deviceFreeSpaceMb = getDeviceFreeSpaceMb(appContext),
        )
        if (!check.ok) {
            val err = check.reason ?: "Espaço insuficiente para download."
            updateEntry(
                pack.packId,
                ModDownloadProgress(pack.packId, ModDownloadStatus.ERROR, 0.0, pack.sizeMb, 0.0, 0, err)
            )
            onError(err)
            return
        }
        val existing = _downloads.value[pack.packId]
        val startMb = if (existing?.status == ModDownloadStatus.PAUSED) existing.downloadedMb else 0.0
        launchDownloadLoop(pack, startMb, onComplete)
    }

    fun pauseDownload(packId: String) {
        activeJobs.remove(packId)?.cancel()
        val cur = _downloads.value[packId] ?: return
        if (cur.status == ModDownloadStatus.DOWNLOADING) {
            updateEntry(packId, cur.copy(status = ModDownloadStatus.PAUSED, speedMbPerSec = 0.0))
        }
    }

    fun resumeDownload(
        pack: HubPackEntity,
        onComplete: (HubPackEntity) -> Unit,
    ) {
        val cur = _downloads.value[pack.packId]
        val startMb = cur?.downloadedMb ?: 0.0
        launchDownloadLoop(pack, startMb, onComplete)
    }

    fun cancelDownload(packId: String) {
        activeJobs.remove(packId)?.cancel()
        updateEntry(packId, null)
        runCatching { File(modBlobsDir(appContext), "$packId.json").delete() }
    }

    fun markUninstalled(packId: String) {
        cancelDownload(packId)
    }

    private fun launchDownloadLoop(
        pack: HubPackEntity,
        initialDownloadedMb: Double,
        onComplete: (HubPackEntity) -> Unit,
    ) {
        activeJobs.remove(pack.packId)?.cancel()
        val job = scope.launch(Dispatchers.Default) {
            var downloaded = initialDownloadedMb
            val total = max(0.5, pack.sizeMb)
            val stepPerTickMb = max(3.5, total / 12.0)

            while (downloaded < total) {
                val pct = ((downloaded / total) * 100.0).roundToInt().coerceIn(0, 99)
                updateEntry(
                    pack.packId,
                    ModDownloadProgress(
                        packId = pack.packId,
                        status = ModDownloadStatus.DOWNLOADING,
                        downloadedMb = (downloaded * 10.0).roundToInt() / 10.0,
                        totalMb = total,
                        speedMbPerSec = (stepPerTickMb * 4.5 * 10.0).roundToInt() / 10.0,
                        progressPct = pct,
                    )
                )
                delay(220L)
                downloaded = min(total, downloaded + stepPerTickMb)
            }

            runCatching {
                val blobFile = File(modBlobsDir(appContext), "${pack.packId}.json")
                blobFile.writeText(quotaJson.encodeToString(pack.copy(installed = true)), Charsets.UTF_8)
            }

            updateEntry(
                pack.packId,
                ModDownloadProgress(
                    packId = pack.packId,
                    status = ModDownloadStatus.COMPLETED,
                    downloadedMb = total,
                    totalMb = total,
                    speedMbPerSec = 0.0,
                    progressPct = 100,
                )
            )
            onComplete(pack)
        }
        activeJobs[pack.packId] = job
    }
}
