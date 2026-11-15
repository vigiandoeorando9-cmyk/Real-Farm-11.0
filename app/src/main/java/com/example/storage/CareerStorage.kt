package com.example.storage

import android.content.Context
import android.os.Build
import com.example.domain.CloudSaveSummary
import com.example.domain.DEFAULT_HUB_PACKS
import com.example.domain.FarmContract
import com.example.domain.FieldPlot
import com.example.domain.GRID_SIZE
import com.example.domain.GameSettings
import com.example.domain.GraphicsPreset
import com.example.domain.HubPackEntity
import com.example.domain.LivestockHerd
import com.example.domain.ModDownloadProgress
import com.example.domain.ModDownloadStatus
import com.example.domain.OwnedImplement
import com.example.domain.OwnedIndustry
import com.example.domain.OwnedVehicle
import com.example.domain.PlacedBuilding
import com.example.domain.PlayerCharacterState
import com.example.domain.SoilState
import com.example.domain.TerrainCell
import com.example.domain.WeatherType
import com.example.domain.WorldState
import com.example.domain.detectRecommendedGraphicsPreset
import com.example.domain.getHubMeta
import com.example.domain.getMapById
import com.example.domain.getVehicleDef
import com.example.firebase.CloudSaveDoc
import com.example.firebase.CloudSaveUploadPayload
import com.example.simulation.createNewWorld
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

const val MAX_CAREER_SLOTS = 20

private val storageJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

@Serializable
data class CareerSlotMeta(
    val slotIndex: Int,
    val farmName: String,
    val mapId: String,
    val mapName: String,
    val day: Int,
    val month: Int,
    val moneyBrl: Double,
    val savedAtEpochMs: Long,
    val startingMode: String,
    val checksumSha256: String,
    val schemaVersion: Int,
    val isBackupRecovered: Boolean = false,
)

@Serializable
data class PersistedCareerEnvelope(
    val schemaVersion: Int = 4,
    val savedAtEpochMs: Long,
    val checksumSha256: String,
    val state: WorldState,
)

@Serializable
data class PortableCareerExportEnvelope(
    val app: String = "FARM_BRASIL",
    val exportVersion: Int = 1,
    val exportedAtIso: String,
    val checksumSha256: String,
    val state: WorldState,
)

data class StorageSlotBreakdown(
    val slotIndex: Int,
    val farmName: String,
    val sizeBytes: Long,
    val isCorrupted: Boolean,
)

data class StorageBreakdownReport(
    val totalUsedBytes: Long,
    val careersBytes: Long,
    val backupsBytes: Long,
    val hubAndSettingsBytes: Long,
    val modBlobsBytes: Long,
    val estimatedApkBaseBytes: Long,
    val installedModPacksMb: Double,
    val pendingDownloadMb: Double,
    val slotDetails: List<StorageSlotBreakdown>,
)

val DEFAULT_SETTINGS: GameSettings by lazy {
    val preset = detectRecommendedGraphicsPreset()
    val meta = com.example.domain.GRAPHICS_PRESET_META[preset]
        ?: com.example.domain.GRAPHICS_PRESET_META.values.first()
    GameSettings(
        graphicsPreset = preset,
        customGraphics = meta.config,
    )
}

fun computeSha256Hex(text: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

private fun careersDir(context: Context): File =
    File(context.filesDir, "farm_brasil_careers").apply { mkdirs() }

private fun settingsFile(context: Context): File =
    File(context.filesDir, "farm_brasil_settings.json")

private fun hubPacksFile(context: Context): File =
    File(context.filesDir, "farm_brasil_hub_packs.json")

private fun hubFavsFile(context: Context): File =
    File(context.filesDir, "farm_brasil_hub_favorites.json")

private fun deviceIdentityFile(context: Context): File =
    File(context.filesDir, "farm_brasil_device_id.txt")

private fun slotFile(context: Context, slot: Int): File =
    File(careersDir(context), "slot_$slot.json")

private fun slotBackupFile(context: Context, slot: Int): File =
    File(careersDir(context), "slot_${slot}.bak.json")

fun saveWorldToSlot(context: Context, state: WorldState): Pair<Boolean, String> {
    return try {
        val stateJson = storageJson.encodeToString(state)
        val checksum = computeSha256Hex(stateJson)
        val envelope = PersistedCareerEnvelope(
            schemaVersion = 4,
            savedAtEpochMs = System.currentTimeMillis(),
            checksumSha256 = checksum,
            state = state,
        )
        val encoded = storageJson.encodeToString(envelope)
        val primary = slotFile(context, state.slotIndex)
        val backup = slotBackupFile(context, state.slotIndex)
        if (primary.exists() && primary.length() > 32) {
            primary.copyTo(backup, overwrite = true)
        }
        primary.writeText(encoded, Charsets.UTF_8)
        true to checksum
    } catch (_: Exception) {
        false to ""
    }
}

fun loadWorldFromSlot(context: Context, slotIndex: Int): Pair<WorldState?, Boolean> {
    val primary = slotFile(context, slotIndex)
    val backup = slotBackupFile(context, slotIndex)

    fun tryRead(file: File): Pair<WorldState, Boolean>? {
        if (!file.exists()) return null
        return try {
            val raw = file.readText(Charsets.UTF_8)
            val env = storageJson.decodeFromString<PersistedCareerEnvelope>(raw)
            val recomputed = computeSha256Hex(storageJson.encodeToString(env.state))
            if (recomputed != env.checksumSha256) return null
            env.state to false
        } catch (_: Exception) {
            null
        }
    }

    tryRead(primary)?.let { return it.first to false }
    tryRead(backup)?.let { return it.first to true }
    return null to false
}

fun listCareerSlots(context: Context): List<CareerSlotMeta?> {
    return (0 until MAX_CAREER_SLOTS).map { idx ->
        val primary = slotFile(context, idx)
        val backup = slotBackupFile(context, idx)
        val targetFile = when {
            primary.exists() -> primary
            backup.exists() -> backup
            else -> null
        } ?: return@map null

        try {
            val env = storageJson.decodeFromString<PersistedCareerEnvelope>(targetFile.readText(Charsets.UTF_8))
            val mapDef = getMapById(env.state.mapId)
            CareerSlotMeta(
                slotIndex = idx,
                farmName = env.state.farmName,
                mapId = env.state.mapId,
                mapName = mapDef.stateName,
                day = env.state.day,
                month = env.state.month,
                moneyBrl = env.state.moneyBrl,
                savedAtEpochMs = env.savedAtEpochMs,
                startingMode = env.state.startingMode,
                checksumSha256 = env.checksumSha256,
                schemaVersion = env.schemaVersion,
                isBackupRecovered = targetFile == backup,
            )
        } catch (_: Exception) {
            null
        }
    }
}

fun deleteCareerSlot(context: Context, slotIndex: Int) {
    runCatching { slotFile(context, slotIndex).delete() }
    runCatching { slotBackupFile(context, slotIndex).delete() }
}

fun exportCareerToJson(state: WorldState): String {
    val stateJson = storageJson.encodeToString(state)
    val checksum = computeSha256Hex(stateJson)
    val exportEnv = PortableCareerExportEnvelope(
        app = "FARM_BRASIL",
        exportVersion = 1,
        exportedAtIso = Instant.now().toString(),
        checksumSha256 = checksum,
        state = state,
    )
    return storageJson.encodeToString(exportEnv)
}

sealed interface CareerImportResult {
    data class Ok(val state: WorldState) : CareerImportResult
    data class Err(val error: String) : CareerImportResult
}

fun parseAndVerifyCareerImport(rawJson: String, targetSlot: Int): CareerImportResult {
    return try {
        val env = storageJson.decodeFromString<PortableCareerExportEnvelope>(rawJson)
        if (env.app != "FARM_BRASIL") {
            return CareerImportResult.Err("Arquivo não pertence ao Farm Brasil.")
        }
        val expectedChecksum = computeSha256Hex(storageJson.encodeToString(env.state))
        if (expectedChecksum != env.checksumSha256) {
            return CareerImportResult.Err("Falha na verificação SHA-256: arquivo corrompido ou adulterado.")
        }
        CareerImportResult.Ok(env.state.copy(slotIndex = targetSlot))
    } catch (_: Exception) {
        CareerImportResult.Err("JSON de backup inválido.")
    }
}

fun loadSettings(context: Context): GameSettings {
    val file = settingsFile(context)
    if (!file.exists()) return DEFAULT_SETTINGS
    return try {
        storageJson.decodeFromString<GameSettings>(file.readText(Charsets.UTF_8))
    } catch (_: Exception) {
        DEFAULT_SETTINGS
    }
}

fun saveSettings(context: Context, settings: GameSettings) {
    runCatching {
        settingsFile(context).writeText(storageJson.encodeToString(settings), Charsets.UTF_8)
    }
}

fun loadHubPacks(context: Context): List<HubPackEntity> {
    val file = hubPacksFile(context)
    if (!file.exists()) return DEFAULT_HUB_PACKS
    return try {
        val stored = storageJson.decodeFromString<List<HubPackEntity>>(file.readText(Charsets.UTF_8))
        val storedById = stored.associateBy { it.packId }
        val mergedBuiltins = DEFAULT_HUB_PACKS.map { defPack ->
            storedById[defPack.packId]?.let { existing ->
                defPack.copy(
                    installed = existing.installed,
                    localPath = existing.localPath,
                    installedAtEpochMs = existing.installedAtEpochMs,
                    version = if (existing.installed) existing.version else defPack.version,
                    enabled = existing.enabled,
                )
            } ?: defPack
        }
        val customImported = stored.filter { it.isCustomImported == true }
        mergedBuiltins + customImported
    } catch (_: Exception) {
        DEFAULT_HUB_PACKS
    }
}

fun saveHubPacks(context: Context, packs: List<HubPackEntity>) {
    runCatching {
        hubPacksFile(context).writeText(storageJson.encodeToString(packs), Charsets.UTF_8)
    }
}

fun installHubPackInList(packs: List<HubPackEntity>, packId: String): List<HubPackEntity> =
    packs.map {
        if (it.packId == packId) {
            it.copy(
                installed = true,
                enabled = true,
                localPath = it.localPath ?: "local://hub/$packId.json",
                installedAtEpochMs = System.currentTimeMillis(),
            )
        } else it
    }

fun toggleHubPackEnabledInList(packs: List<HubPackEntity>, packId: String): List<HubPackEntity> =
    packs.map {
        if (it.packId == packId && it.installed) {
            it.copy(enabled = !it.enabled)
        } else it
    }

fun updateHubPackInList(packs: List<HubPackEntity>, packId: String): List<HubPackEntity> =
    packs.map {
        if (it.packId == packId) {
            val latest = getHubMeta(packId).latestVersion
            it.copy(
                installed = true,
                enabled = true,
                version = latest,
                installedAtEpochMs = System.currentTimeMillis(),
            )
        } else it
    }

fun uninstallHubPackInList(packs: List<HubPackEntity>, packId: String): List<HubPackEntity> =
    packs.map {
        if (it.packId == packId) {
            it.copy(installed = false, enabled = true, localPath = null, installedAtEpochMs = 0L)
        } else it
    }

fun loadHubFavorites(context: Context): List<String> {
    val file = hubFavsFile(context)
    if (!file.exists()) return emptyList()
    return try {
        storageJson.decodeFromString<List<String>>(file.readText(Charsets.UTF_8))
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveHubFavorites(context: Context, favorites: List<String>) {
    runCatching {
        hubFavsFile(context).writeText(storageJson.encodeToString(favorites.distinct()), Charsets.UTF_8)
    }
}

fun getStorageBreakdown(
    context: Context,
    hubPacks: List<HubPackEntity>,
    downloads: Map<String, ModDownloadProgress>,
): StorageBreakdownReport {
    var careersBytes = 0L
    var backupsBytes = 0L
    val details = mutableListOf<StorageSlotBreakdown>()

    for (idx in 0 until MAX_CAREER_SLOTS) {
        val f = slotFile(context, idx)
        val bak = slotBackupFile(context, idx)
        if (f.exists()) {
            val sz = f.length()
            careersBytes += sz
            val (state, corrupted) = loadWorldFromSlot(context, idx)
            details.add(
                StorageSlotBreakdown(
                    slotIndex = idx,
                    farmName = state?.farmName ?: "Espaço ${idx + 1}",
                    sizeBytes = sz,
                    isCorrupted = state == null && !corrupted,
                )
            )
        }
        if (bak.exists()) {
            backupsBytes += bak.length()
        }
    }

    var sysBytes = 0L
    listOf(settingsFile(context), hubPacksFile(context), hubFavsFile(context)).forEach {
        if (it.exists()) sysBytes += it.length()
    }

    val modsDir = File(context.filesDir, "farm_brasil_mods")
    var modBlobsBytes = 0L
    if (modsDir.exists()) {
        modsDir.listFiles()?.forEach { modBlobsBytes += it.length() }
    }

    val installedModMb = hubPacks.filter { it.installed }.sumOf { it.sizeMb }
    val pendingDownloadMb = downloads.values
        .filter { it.status == ModDownloadStatus.DOWNLOADING || it.status == ModDownloadStatus.PAUSED }
        .sumOf { max(0.0, it.totalMb - it.downloadedMb) }

    return StorageBreakdownReport(
        totalUsedBytes = careersBytes + backupsBytes + sysBytes + modBlobsBytes,
        careersBytes = careersBytes,
        backupsBytes = backupsBytes,
        hubAndSettingsBytes = sysBytes,
        modBlobsBytes = modBlobsBytes,
        estimatedApkBaseBytes = 42L * 1024L * 1024L,
        installedModPacksMb = (installedModMb * 10.0).roundToInt() / 10.0,
        pendingDownloadMb = (pendingDownloadMb * 10.0).roundToInt() / 10.0,
        slotDetails = details,
    )
}

fun clearBackupSlots(context: Context): Int {
    var removed = 0
    for (i in 0 until MAX_CAREER_SLOTS) {
        val bak = slotBackupFile(context, i)
        if (bak.exists() && bak.delete()) removed++
    }
    return removed
}

fun getOrCreateDeviceIdentity(context: Context): Pair<String, String> {
    val file = deviceIdentityFile(context)
    val id = if (file.exists()) {
        file.readText(Charsets.UTF_8).trim().ifBlank { null }
    } else null
    val resolvedId = id ?: "android_${UUID.randomUUID().toString().replace("-", "").take(12)}".also {
        runCatching { file.writeText(it, Charsets.UTF_8) }
    }
    val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim()
        .take(48)
        .ifBlank { "Android Device" }
    return resolvedId to deviceName
}

// Compact RLE Terrain Serializer for Firestore Cloud Sync
fun encodeCellsCompactRle(cells: List<TerrainCell>, gridSide: Int): String {
    if (cells.isEmpty()) return "G$gridSide:"
    val sb = StringBuilder("G$gridSide:")
    var prevToken: String? = null
    var runCount = 0

    fun tokenOf(c: TerrainCell): String {
        val soilOrd = c.soilState.ordinal
        val cropPart = c.cropId ?: ""
        val progPct = (c.growthProgress * 100.0).roundToInt()
        val fertPct = (c.fertility * 100.0).roundToInt()
        return "${soilOrd}_${c.plotId}_${cropPart}_${progPct}_${fertPct}"
    }

    fun flush() {
        val tok = prevToken ?: return
        if (runCount > 1) sb.append("$runCount*$tok;") else sb.append("$tok;")
    }

    for (c in cells) {
        val tok = tokenOf(c)
        if (tok == prevToken) {
            runCount++
        } else {
            flush()
            prevToken = tok
            runCount = 1
        }
    }
    flush()
    return sb.toString()
}

fun decodeCellsCompactRle(
    encoded: String,
    baseCells: List<TerrainCell>,
): List<TerrainCell> {
    val colonIdx = encoded.indexOf(':')
    if (colonIdx < 0) return baseCells
    val body = encoded.substring(colonIdx + 1)
    if (body.isBlank()) return baseCells

    val result = baseCells.toMutableList()
    var cursor = 0
    val tokens = body.split(';')
    val soils = SoilState.entries

    for (raw in tokens) {
        if (raw.isBlank() || cursor >= result.size) continue
        val starIdx = raw.indexOf('*')
        val repeat = if (starIdx > 0) (raw.substring(0, starIdx).toIntOrNull() ?: 1) else 1
        val payload = if (starIdx > 0) raw.substring(starIdx + 1) else raw
        val parts = payload.split('_')
        if (parts.size < 5) {
            cursor += repeat
            continue
        }
        val soilOrd = (parts[0].toIntOrNull() ?: 7).coerceIn(0, soils.size - 1)
        val plotId = parts[1].toIntOrNull() ?: 0
        val cropId = parts[2].takeIf { it.isNotBlank() }
        val prog = ((parts[3].toIntOrNull() ?: 0) / 100.0).coerceIn(0.0, 1.0)
        val fert = ((parts[4].toIntOrNull() ?: 75) / 100.0).coerceIn(0.2, 1.0)

        for (r in 0 until repeat) {
            if (cursor >= result.size) break
            val base = result[cursor]
            result[cursor] = base.copy(
                soilState = soils[soilOrd],
                plotId = plotId,
                cropId = cropId,
                growthProgress = prog,
                fertility = fert,
            )
            cursor++
        }
    }
    return result
}

fun collectRequiredModIdsForWorld(state: WorldState): List<String> {
    val ids = linkedSetOf<String>()
    val mapDef = getMapById(state.mapId)
    mapDef.requiredPackId?.let { ids.add(it) }
    for (v in state.vehicles) {
        getVehicleDef(v.defId).requiredModPackId?.let { ids.add(it) }
    }
    for (imp in state.implements) {
        com.example.domain.getImplementDef(imp.defId).requiredModPackId?.let { ids.add(it) }
    }
    return ids.toList()
}

fun detectMissingModsForCloudSave(
    requiredModIds: List<String>,
    installedPacks: List<HubPackEntity>,
): List<HubPackEntity> {
    val installedSet = installedPacks.filter { it.installed }.map { it.packId }.toSet()
    return requiredModIds
        .filter { !installedSet.contains(it) }
        .mapNotNull { reqId -> installedPacks.find { it.packId == reqId } }
}

private fun JsonElement.toFirestoreValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull
        longOrNull != null -> longOrNull
        doubleOrNull != null -> doubleOrNull
        else -> content
    }
    is JsonObject -> entries.associate { (k, v) -> k to v.toFirestoreValue() }
    is JsonArray -> map { it.toFirestoreValue() }
}

private fun anyToJsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Map<*, *> -> JsonObject(
        value.entries
            .filter { it.key is String }
            .associate { (k, v) -> (k as String) to anyToJsonElement(v) }
    )
    is List<*> -> JsonArray(value.map { anyToJsonElement(it) })
    else -> JsonPrimitive(value.toString())
}

private inline fun <reified T> toFirestoreMap(item: T): Map<String, Any?> {
    val obj = storageJson.encodeToJsonElement(item) as? JsonObject ?: return emptyMap()
    return obj.entries.associate { (k, v) -> k to v.toFirestoreValue() }
}

private inline fun <reified T> fromFirestoreMap(map: Map<String, Any?>): T {
    val elem = anyToJsonElement(map)
    return storageJson.decodeFromJsonElement(elem)
}

fun buildCloudSaveUploadPayload(
    state: WorldState,
    deviceId: String,
    deviceName: String,
): CloudSaveUploadPayload {
    val gridSide = state.mapGridSize ?: GRID_SIZE
    val rle = encodeCellsCompactRle(state.cells, gridSide)
    val stateHash = computeSha256Hex("${state.farmName}|${state.day}|${state.moneyBrl}|$rle").take(32)
    val reqMods = collectRequiredModIdsForWorld(state)

    return CloudSaveUploadPayload(
        slotIndex = state.slotIndex,
        farmName = state.farmName,
        mapId = state.mapId,
        mapGridSize = gridSide,
        startingMode = state.startingMode,
        day = state.day,
        month = state.month,
        hourOfDay = state.hourOfDay,
        weather = state.weather.name,
        moneyBrl = state.moneyBrl,
        bankLoanBrl = state.bankLoanBrl,
        localRevision = state.cloudRevision ?: 0,
        deviceId = deviceId,
        deviceName = deviceName,
        checksumSha256 = stateHash,
        requiredModIds = reqMods,
        compactCellsRle = rle,
        playerMap = toFirestoreMap(state.player),
        plotsList = state.plots.map { toFirestoreMap(it) },
        vehiclesList = state.vehicles.map { toFirestoreMap(it) },
        implementsList = state.implements.map { toFirestoreMap(it) },
        buildingsList = state.buildings.map { toFirestoreMap(it) },
        industriesList = state.industries.map { toFirestoreMap(it) },
        herdsList = state.herds.map { toFirestoreMap(it) },
        inventoryMap = state.inventoryKg.mapValues { it.value },
        contractsList = state.contracts.map { toFirestoreMap(it) },
        activeVehicleUid = state.activeVehicleUid,
    )
}

fun worldStateFromCloudSaveDoc(doc: CloudSaveDoc): WorldState {
    val gridSide = doc.mapGridSize.toInt().coerceIn(64, 192)
    val base = createNewWorld(
        slotIndex = doc.slotIndex.toInt().coerceIn(0, MAX_CAREER_SLOTS - 1),
        farmName = doc.farmName.ifBlank { "Fazenda Brasil" },
        mapId = doc.mapId.ifBlank { "map_pr" },
        startingMoneyBrl = doc.moneyBrl,
        startingMode = doc.startingMode.ifBlank { "Padrão" },
        requestedGridSide = gridSide,
    )

    val decodedCells = if (doc.compactCellsRle.isNotBlank()) {
        decodeCellsCompactRle(doc.compactCellsRle, base.cells)
    } else base.cells

    val weather = runCatching { WeatherType.valueOf(doc.weather) }.getOrDefault(base.weather)
    val player = runCatching { fromFirestoreMap<PlayerCharacterState>(doc.playerMap) }.getOrDefault(base.player)
    val plots = doc.plotsList.mapNotNull { runCatching { fromFirestoreMap<FieldPlot>(it) }.getOrNull() }.ifEmpty { base.plots }
    val vehicles = doc.vehiclesList.mapNotNull { runCatching { fromFirestoreMap<OwnedVehicle>(it) }.getOrNull() }
    val implements = doc.implementsList.mapNotNull { runCatching { fromFirestoreMap<OwnedImplement>(it) }.getOrNull() }
    val buildings = doc.buildingsList.mapNotNull { runCatching { fromFirestoreMap<PlacedBuilding>(it) }.getOrNull() }.ifEmpty { base.buildings }
    val industries = doc.industriesList.mapNotNull { runCatching { fromFirestoreMap<OwnedIndustry>(it) }.getOrNull() }.ifEmpty { base.industries }
    val herds = doc.herdsList.mapNotNull { runCatching { fromFirestoreMap<LivestockHerd>(it) }.getOrNull() }.ifEmpty { base.herds }
    val contracts = doc.contractsList.mapNotNull { runCatching { fromFirestoreMap<FarmContract>(it) }.getOrNull() }.ifEmpty { base.contracts }
    val inventory = doc.inventoryMap.mapNotNull { (k, v) ->
        val num = (v as? Number)?.toDouble()
        if (num != null) k to num else null
    }.toMap()

    return base.copy(
        day = doc.day.toInt().coerceAtLeast(1),
        month = doc.month.toInt().coerceIn(1, 12),
        hourOfDay = doc.hourOfDay.coerceIn(0.0, 24.0),
        weather = weather,
        moneyBrl = doc.moneyBrl,
        bankLoanBrl = doc.bankLoanBrl,
        player = player,
        activeVehicleUid = doc.activeVehicleUid,
        plots = plots,
        cells = decodedCells,
        vehicles = vehicles,
        implements = implements,
        buildings = buildings,
        industries = industries,
        herds = herds,
        inventoryKg = inventory,
        contracts = contracts,
        cloudRevision = doc.revision.toInt(),
    )
}

fun cloudSaveDocToSummary(doc: CloudSaveDoc): CloudSaveSummary {
    val iso = doc.updatedAt?.toDate()?.toInstant()?.toString() ?: Instant.now().toString()
    return CloudSaveSummary(
        slotIndex = doc.slotIndex.toInt(),
        farmName = doc.farmName,
        mapId = doc.mapId,
        day = doc.day.toInt(),
        moneyBrl = doc.moneyBrl,
        revision = doc.revision.toInt(),
        deviceId = doc.deviceId,
        deviceName = doc.deviceName,
        checksumSha256 = doc.checksumSha256,
        requiredModIds = doc.requiredModIds,
        payloadSizeBytes = doc.payloadSizeBytes,
        updatedAtIso = iso,
    )
}
