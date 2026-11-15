package com.example.simulation

import com.example.domain.ALL_VEHICLES
import com.example.domain.AnimalSpecies
import com.example.domain.BUILDING_META
import com.example.domain.BrazilianMapDef
import com.example.domain.BuildingType
import com.example.domain.CELL_SIZE_M
import com.example.domain.ContractType
import com.example.domain.DEFAULT_PLAYER_APPEARANCE
import com.example.domain.ESTABLISHMENTS
import com.example.domain.FarmContract
import com.example.domain.FieldPlot
import com.example.domain.GRID_SIZE
import com.example.domain.INDUSTRY_CHAINS
import com.example.domain.LandUnit
import com.example.domain.LedgerEntry
import com.example.domain.LivestockHerd
import com.example.domain.ManualTool
import com.example.domain.OwnedImplement
import com.example.domain.OwnedIndustry
import com.example.domain.OwnedVehicle
import com.example.domain.PlacedBuilding
import com.example.domain.PlayerCharacterState
import com.example.domain.PlotZoneType
import com.example.domain.Puddle
import com.example.domain.ReliefProfile
import com.example.domain.RoadVehicle
import com.example.domain.SoilState
import com.example.domain.TerrainCell
import com.example.domain.UsedAuctionLot
import com.example.domain.WeatherType
import com.example.domain.WorldProp
import com.example.domain.WorldPropType
import com.example.domain.WorldState
import com.example.domain.RegionalArchStyle
import com.example.domain.getMapById
import com.example.domain.getStructureDef
import com.example.domain.resolveBuildingVisualProfile
import com.example.domain.resolveRegionalArchStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun createRng(seed: Int): () -> Double {
    var s = seed.toLong() and 0xFFFFFFFFL
    if (s == 0L) s = 1L
    return {
        s = (s * 1664525L + 1013904223L) and 0xFFFFFFFFL
        s.toDouble() / 4294967296.0
    }
}

private fun clamp(v: Double, minVal: Double, maxVal: Double): Double = v.coerceIn(minVal, maxVal)

private enum class RegionalArchetype {
    PARANA_TERRA_ROXA,
    PAMPA_RS,
    SERRA_PINHAIS_SC,
    CERRADO_CHAPADAO,
    PANTANAL_VARZEA,
    SERRA_CAFE_MG,
    CANA_CITROS_SP,
    CAATINGA_SERTAO,
    AMAZONIA_FLORESTA,
    PRADARIA_EUA,
}

private fun resolveRegionalArchetype(mapDef: BrazilianMapDef): RegionalArchetype = when (mapDef.id) {
    "map_pr", "map_pr_santa_lucia", "map_pr_norte", "mod_map_campos_gerais" -> RegionalArchetype.PARANA_TERRA_ROXA
    "map_rs" -> RegionalArchetype.PAMPA_RS
    "map_sc", "mod_map_bavaria_eu" -> RegionalArchetype.SERRA_PINHAIS_SC
    "map_mt", "map_ms", "map_go", "map_ma", "map_pi", "mod_map_parecis_mt" -> RegionalArchetype.CERRADO_CHAPADAO
    "map_to", "mod_map_pantanal_sul" -> RegionalArchetype.PANTANAL_VARZEA
    "map_mg", "map_es", "map_rj", "mod_map_serra_cafe_mg" -> RegionalArchetype.SERRA_CAFE_MG
    "map_sp" -> RegionalArchetype.CANA_CITROS_SP
    "map_ce", "map_pe", "map_ba", "map_al" -> RegionalArchetype.CAATINGA_SERTAO
    "map_pa", "map_ro", "map_ac" -> RegionalArchetype.AMAZONIA_FLORESTA
    "mod_map_iowa_midwest" -> RegionalArchetype.PRADARIA_EUA
    else -> when (mapDef.reliefProfile) {
        ReliefProfile.PLANICIE_CERRADO -> RegionalArchetype.CERRADO_CHAPADAO
        ReliefProfile.SERRAS_VALES -> RegionalArchetype.SERRA_CAFE_MG
        ReliefProfile.PLANICIE_ALAGAVEL -> RegionalArchetype.PANTANAL_VARZEA
        ReliefProfile.VARZEA_AMAZONICA -> RegionalArchetype.AMAZONIA_FLORESTA
        ReliefProfile.TABULEIRO_LITORANEO -> RegionalArchetype.CAATINGA_SERTAO
        ReliefProfile.COLINAS_SUAVES -> RegionalArchetype.PARANA_TERRA_ROXA
    }
}

private fun riverCenterZ(mapDef: BrazilianMapDef, wx: Double): Double {
    val phase = (mapDef.seed % 64) * 0.29
    return when (resolveRegionalArchetype(mapDef)) {
        RegionalArchetype.AMAZONIA_FLORESTA, RegionalArchetype.PANTANAL_VARZEA ->
            -128.0 + sin(wx * 0.021 + phase) * 38.0 + cos(wx * 0.009 - phase) * 18.0
        RegionalArchetype.CAATINGA_SERTAO ->
            -140.0 + sin(wx * 0.012 + phase) * 16.0
        RegionalArchetype.CERRADO_CHAPADAO ->
            -155.0 + sin(wx * 0.010 + phase) * 14.0
        else ->
            -118.0 + sin(wx * 0.017 + phase) * 26.0 + cos(wx * 0.008 - phase) * 14.0
    }
}

private fun isInNaturalLake(mapDef: BrazilianMapDef, wx: Double, wz: Double): Boolean {
    return when {
        !mapDef.hasRiver -> {
            // Semi-arid oasis acudes (Ceara / Piaui)
            val a1 = hypot((wx - 92.0) / 14.0, (wz - 48.0) / 11.0)
            val a2 = hypot((wx + 128.0) / 15.0, (wz + 82.0) / 12.0)
            val wobble = sin(wx * 0.16) * 0.11 + cos(wz * 0.15) * 0.10
            a1 < 1.0 + wobble || a2 < 1.0 - wobble * 0.6
        }
        resolveRegionalArchetype(mapDef) == RegionalArchetype.PANTANAL_VARZEA ||
            resolveRegionalArchetype(mapDef) == RegionalArchetype.AMAZONIA_FLORESTA -> {
            val d1 = hypot((wx - 134.0) / 22.0, (wz - 196.0) / 18.0)
            val d2 = hypot((wx + 158.0) / 24.0, (wz + 148.0) / 19.0)
            val d3 = hypot((wx - 148.0) / 20.0, (wz + 158.0) / 17.0)
            val wobble = sin(wx * 0.13) * 0.18 + cos(wz * 0.12) * 0.16
            d1 < 1.0 + wobble || d2 < 1.0 - wobble * 0.7 || d3 < 1.0 + wobble * 0.8
        }
        else -> {
            val d1 = hypot((wx - 134.0) / 16.0, (wz - 196.0) / 13.0)
            val d2 = hypot((wx + 158.0) / 17.0, (wz + 148.0) / 14.0)
            val d3 = hypot((wx - 148.0) / 14.0, (wz + 158.0) / 12.0)
            val wobble = sin(wx * 0.14) * 0.14 + cos(wz * 0.13) * 0.12
            d1 < 1.0 + wobble || d2 < 1.0 - wobble * 0.7 || d3 < 1.0 + wobble * 0.8
        }
    }
}

private fun inOrganicField(
    wx: Double,
    wz: Double,
    cx: Double,
    cz: Double,
    rx: Double,
    rz: Double,
    angleRad: Double,
    wobbleAmp: Double,
    seedPhase: Double,
): Boolean {
    val dx = wx - cx
    val dz = wz - cz
    val ca = cos(angleRad)
    val sa = sin(angleRad)
    val lx = dx * ca + dz * sa
    val lz = -dx * sa + dz * ca
    val nx = abs(lx) / max(rx, 1.0)
    val nz = abs(lz) / max(rz, 1.0)
    if (wobbleAmp <= 0.07) {
        // Large mechanized agro-blocks with smooth superellipse corners (n=4) instead of harsh jagged rectangles
        val nx4 = nx * nx * nx * nx
        val nz4 = nz * nz * nz * nz
        val superDist = kotlin.math.sqrt(kotlin.math.sqrt(nx4 + nz4))
        return superDist <= 0.97
    }
    val normDist = hypot(lx / max(rx, 1.0), lz / max(rz, 1.0))
    if (normDist > 1.32) return false
    val theta = atan2(lz, lx)
    val damp = wobbleAmp.coerceAtMost(0.14)
    val organicBorder = 1.0 +
        sin(theta * 2.0 + seedPhase) * (damp * 0.65) +
        cos(theta * 3.0 - seedPhase * 1.15) * (damp * 0.35)
    return normDist <= organicBorder
}

fun computeHeight(
    mapDef: BrazilianMapDef,
    x: Double,
    z: Double,
    isRoad: Boolean = false,
): Double {
    val yardDist = hypot(x, z - 18.0)
    val yardFlat = clamp((yardDist - 22.0) / 56.0, 0.04, 1.0)
    val villageDist = hypot(x - 155.0, z + 95.0)
    val villageFlat = clamp((villageDist - 35.0) / 45.0, 0.12, 1.0)
    val roadFactor = if (isRoad) 0.50 else 1.0

    val archetype = resolveRegionalArchetype(mapDef)
    val phase = (mapDef.seed % 100) * 0.17
    val raw = when (archetype) {
        RegionalArchetype.CERRADO_CHAPADAO, RegionalArchetype.PRADARIA_EUA -> {
            // Broad flat tableland chapadoes with gentle outer escarpments
            val plateau = sin(x * 0.008 + phase) * 0.65 + cos(z * 0.008 - phase) * 0.55
            val edgeDist = max(abs(x), abs(z))
            val rimSlope = if (edgeDist > 210.0) ((edgeDist - 210.0) / 90.0) * 1.1 else 0.0
            (plateau + rimSlope) * mapDef.heightScale
        }
        RegionalArchetype.PARANA_TERRA_ROXA, RegionalArchetype.PAMPA_RS -> {
            val baseHills = sin(x * 0.018 + phase) * 1.10 + cos(z * 0.016) * 1.00 + sin((x + z) * 0.012) * 0.70
            val terraceWave = sin(baseHills * 2.5 + z * 0.028) * 0.24
            (baseHills + terraceWave) * mapDef.heightScale
        }
        RegionalArchetype.SERRA_CAFE_MG, RegionalArchetype.SERRA_PINHAIS_SC -> {
            // Mares de morros / steep mountain serra ridges and valleys
            (sin(x * 0.025 + phase) * 1.60 + cos(z * 0.022) * 1.45 + sin(x * 0.048) * 0.65 + cos((x - z) * 0.016) * 0.95) * mapDef.heightScale
        }
        RegionalArchetype.CAATINGA_SERTAO -> {
            // Semi-arid tabular plains punctuated by rocky serrote outcrops
            val plain = sin(x * 0.014 + phase) * 0.65 + cos(z * 0.013 - phase) * 0.60
            val serrote1 = max(0.0, 1.0 - hypot((x + 175.0) / 55.0, (z - 140.0) / 48.0)) * 2.4
            val serrote2 = max(0.0, 1.0 - hypot((x - 195.0) / 50.0, (z + 185.0) / 44.0)) * 2.1
            (plain + serrote1 + serrote2) * mapDef.heightScale
        }
        RegionalArchetype.PANTANAL_VARZEA, RegionalArchetype.AMAZONIA_FLORESTA ->
            (sin(x * 0.012 + phase) * 0.60 + cos(z * 0.013) * 0.55) * mapDef.heightScale
        RegionalArchetype.CANA_CITROS_SP ->
            (sin(x * 0.015 + phase) * 0.92 + cos(z * 0.015 - phase) * 0.85 + sin(x * 0.026) * 0.28) * mapDef.heightScale
    }

    var h = raw * yardFlat * villageFlat * roadFactor
    if (mapDef.hasRiver) {
        val rz = riverCenterZ(mapDef, x)
        val distRiver = abs(z - rz)
        if (distRiver < 34.0) {
            val bankT = clamp((distRiver - 8.0) / 26.0, 0.0, 1.0)
            h = h * (0.25 + 0.75 * bankT) - (1.0 - bankT) * 0.75
        }
    }
    val lakeMin = if (!mapDef.hasRiver) {
        min(hypot(x - 92.0, z - 48.0), hypot(x + 128.0, z + 82.0))
    } else {
        min(
            hypot(x - 134.0, z - 196.0),
            min(hypot(x + 158.0, z + 148.0), hypot(x - 148.0, z + 158.0))
        )
    }
    if (lakeMin < 32.0) {
        val bankT = clamp((lakeMin - 14.0) / 18.0, 0.0, 1.0)
        h = h * (0.2 + 0.8 * bankT) - (1.0 - bankT) * 0.85
    }
    return h
}

fun makeDefaultVehicle(
    uid: String,
    defId: String,
    x: Double,
    z: Double,
    headingDeg: Double = 0.0,
    fuelL: Double = 150.0,
    selectedCropId: String = "soja",
    conservationPct: Double = 100.0,
    hoursWorked: Double = 0.0,
): OwnedVehicle = OwnedVehicle(
    uid = uid,
    defId = defId,
    x = x,
    z = z,
    headingDeg = headingDeg,
    speedMs = 0.0,
    gear = 0,
    rpm = 850.0,
    fuelL = fuelL,
    wearPct = 100.0 - conservationPct,
    conservationPct = conservationPct,
    engineHealthPct = conservationPct,
    transmissionHealthPct = conservationPct,
    suspensionTiresHealthPct = conservationPct,
    hydraulicsHealthPct = conservationPct,
    bodyHealthPct = conservationPct,
    dirtPct = 0.0,
    steerAngleDeg = 0.0,
    wheelSpinDeg = 0.0,
    wheelSlip = 0.0,
    suspensionOffsetY = 0.0,
    suspensionVelY = 0.0,
    prevTerrainY = 0.0,
    hasTerrainRef = false,
    pitchDeg = 0.0,
    rollDeg = 0.0,
    groundSinkageM = 0.0,
    collisionCooldownSec = 0.0,
    lastImpactSeverity = 0.0,
    stallTimerSec = 0.0,
    rotorSpinDeg = 0.0,
    unloadingAugerProgress = 0.0,
    preventiveBonusHours = 0.0,
    repairHistory = emptyList(),
    selectedCropId = selectedCropId,
    hoursWorked = hoursWorked,
    workLightsOn = false,
    interiorLightOn = false,
    wipersOn = false,
    wiperAngleRad = 0.0,
    doorsOpen = false,
    cabSeatOffset = 0.0,
    engineTempC = 72.0,
    frontBallastKg = 0.0,
    seedingRowSpacingCm = 45.0,
    seedingDensityPct = 100.0,
    seedingDepthCm = 4.0,
    isBogged = false,
    bogSeverity = 0.0,
    isSprayerActive = false,
    sprayerFillL = 0.0,
    sprayerBoomDeploy = 0.0,
    oilFilterHealthPct = conservationPct,
    beltsHealthPct = conservationPct,
    tiresWearPct = conservationPct,
    hydraulicHosesHealthPct = conservationPct,
    cutterDiscsHealthPct = conservationPct,
    lastBreakdownNote = null,
    coolingTicksRemainingSec = 0.0,
)

fun makeDefaultImplement(
    uid: String,
    defId: String,
    x: Double,
    z: Double,
    headingDeg: Double = 0.0,
    fillL: Double = 0.0,
    fillProductId: String? = null,
): OwnedImplement = OwnedImplement(
    uid = uid,
    defId = defId,
    x = x,
    z = z,
    headingDeg = headingDeg,
    fillL = fillL,
    fillProductId = fillProductId,
)

private data class PlotShapeSeed(
    val id: Int,
    val name: String,
    val cx: Double,
    val cz: Double,
    val rx: Double,
    val rz: Double,
    val angle: Double,
    val wobble: Double,
    val areaHa: Double,
    val owned: Boolean,
    val isPreservation: Boolean,
    val zoneType: PlotZoneType,
    val isUrbanLot: Boolean,
    val initialSoil: SoilState,
    val initialCropId: String? = null,
    val initialGrowth: Double = 0.0,
    val ownerNpcName: String? = null,
    val isIrrigated: Boolean = false,
)

private val OWNER_NPC_LIST = listOf(
    "Sr. Antônio",
    "Dona Aparecida",
    "Cooperativa Sul",
    "Família Kowalski",
    "Sítio São José",
    "Agropecuária Horizonte",
    "Família Almeida",
    "Prefeitura da Vila",
)

private data class ExtraPlotSpec(
    val id: Int,
    val name: String,
    val cx: Double,
    val cz: Double,
    val rx: Double,
    val rz: Double,
    val angle: Double,
    val zoneType: PlotZoneType,
    val initialSoil: SoilState,
    val initialCropId: String?,
    val initialGrowth: Double,
)

private fun build56PlotBlueprints(
    mapDef: BrazilianMapDef,
    isZeroMode: Boolean,
    defaultCrop: String,
    secondCrop: String,
    thirdCrop: String,
): List<PlotShapeSeed> {
    val zeroOwned = !isZeroMode
    val archetype = resolveRegionalArchetype(mapDef)

    val corePlots = when (archetype) {
        RegionalArchetype.CERRADO_CHAPADAO, RegionalArchetype.PRADARIA_EUA -> listOf(
            PlotShapeSeed(1, "Talhão 1 - Chapadão Sede", -42.0, -28.0, 44.0, 34.0, 0.0, 0.04, 14.5, zeroOwned, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, defaultCrop, 1.0, if (zeroOwned) "Você" else "Sr. Antônio"),
            PlotShapeSeed(2, "Talhão 2 - Leste Mecanizado", 56.0, -26.0, 46.0, 34.0, 0.0, 0.04, 16.0, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, defaultCrop, 1.0, "Agropecuária Horizonte"),
            PlotShapeSeed(3, "Talhão 3 - Norte Safrinha", -40.0, 74.0, 42.0, 32.0, 0.0, 0.05, 15.2, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, secondCrop, 0.62, "Família Kowalski"),
            PlotShapeSeed(4, "Talhão 4 - Área de Abertura Cerrado", 52.0, 72.0, 38.0, 32.0, 0.0, 0.05, 12.8, zeroOwned, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PEDRAS_MATO, null, 0.0, if (zeroOwned) "Você" else "Fernando Vizinho"),
            PlotShapeSeed(5, "Reserva Legal Cerrado (APP)", -215.0, -12.0, 58.0, 48.0, 0.08, 0.14, 13.0, zeroOwned, true, PlotZoneType.PRESERVACAO, false, SoilState.AREA_PRESERVACAO, null, 0.0, "APP Ambiental"),
            PlotShapeSeed(6, "Fazenda Chapadão Norte (Mega)", -115.0, 218.0, 88.0, 56.0, 0.0, 0.04, 29.5, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, thirdCrop, 0.68, "Mega Silo Central", isIrrigated = mapDef.id == "map_go"),
            PlotShapeSeed(7, "Chácara Buriti (Hortifruti)", -118.0, 44.0, 28.0, 22.0, 0.12, 0.14, 3.0, false, false, PlotZoneType.SITIO_FAMILIAR, false, SoilState.PRONTO_COLHEITA, "rosas_familiares", 1.0, "Dona Aparecida"),
            PlotShapeSeed(8, "Mega Talhão Algodão & Grãos", 222.0, 46.0, 92.0, 68.0, 0.0, 0.04, 34.0, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, thirdCrop, 1.0, "Cooperativa Cerrado"),
            PlotShapeSeed(9, "Invernada Nelore Extensiva", 198.0, 222.0, 72.0, 50.0, 0.0, 0.06, 18.0, false, false, PlotZoneType.PASTAGEM, false, SoilState.PASTAGEM, null, 0.0, "Haras Pantanal"),
            PlotShapeSeed(10, "Reserva Florestal Savana", -195.0, -225.0, 68.0, 50.0, 0.12, 0.18, 15.0, false, false, PlotZoneType.FLORESTAL, false, SoilState.FLORESTA_NATIVA, null, 0.0, "Florestal Pinhal"),
        )
        RegionalArchetype.SERRA_CAFE_MG -> listOf(
            PlotShapeSeed(1, "Talhão 1 - Morro do Cafezal", -40.0, -26.0, 32.0, 24.0, -0.45, 0.24, 6.8, zeroOwned, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, "cafe", 1.0, if (zeroOwned) "Você" else "Sr. Antônio"),
            PlotShapeSeed(2, "Talhão 2 - Encosta Leste (Café)", 48.0, -24.0, 34.0, 25.0, 0.52, 0.25, 8.6, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, "cafe", 0.72, "Cooperativa Cafeeira"),
            PlotShapeSeed(3, "Talhão 3 - Curva do Milho Silagem", -36.0, 66.0, 32.0, 24.0, 0.36, 0.26, 7.9, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, secondCrop, 1.0, "Laticínio Mantiqueira"),
            PlotShapeSeed(4, "Talhão 4 - Pasto Alto da Serra", 46.0, 66.0, 30.0, 24.0, -0.50, 0.22, 6.4, zeroOwned, false, PlotZoneType.PASTAGEM, false, SoilState.PASTAGEM, null, 0.0, if (zeroOwned) "Você" else "Fernando Vizinho"),
            PlotShapeSeed(5, "Reserva Mata Atlântica (APP)", -208.0, -8.0, 64.0, 54.0, 0.22, 0.28, 13.5, zeroOwned, true, PlotZoneType.PRESERVACAO, false, SoilState.AREA_PRESERVACAO, null, 0.0, "APP Ambiental"),
            PlotShapeSeed(6, "Fazenda Serra Verde (Café)", -108.0, 212.0, 62.0, 42.0, -0.38, 0.25, 14.5, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, "cafe", 1.0, "Agropecuária Horizonte"),
            PlotShapeSeed(7, "Sítio Ribeirão Leiteiro", -112.0, 42.0, 30.0, 22.0, 0.45, 0.22, 2.8, false, false, PlotZoneType.SITIO_FAMILIAR, false, SoilState.PRONTO_COLHEITA, "rosas_familiares", 1.0, "Dona Aparecida"),
            PlotShapeSeed(8, "Espigão Grãos & Hortaliças", 218.0, 44.0, 64.0, 48.0, 0.42, 0.24, 17.0, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, thirdCrop, 0.64, "Cooperativa Mineira"),
            PlotShapeSeed(9, "Piquete Vacas Holandesas", 195.0, 225.0, 66.0, 46.0, -0.35, 0.21, 13.5, false, false, PlotZoneType.PASTAGEM, false, SoilState.PASTAGEM, null, 0.0, "Laticínio Serra"),
            PlotShapeSeed(10, "Capão Serra Nativa", -195.0, -225.0, 72.0, 52.0, 0.26, 0.25, 15.0, false, false, PlotZoneType.FLORESTAL, false, SoilState.FLORESTA_NATIVA, null, 0.0, "Florestal Serra"),
        )
        RegionalArchetype.CAATINGA_SERTAO -> listOf(
            PlotShapeSeed(1, "Talhão 1 - Perímetro Irrigado Sede", -38.0, -28.0, 34.0, 26.0, -0.18, 0.14, 7.6, zeroOwned, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, defaultCrop, 1.0, if (zeroOwned) "Você" else "Sr. Antônio", isIrrigated = true),
            PlotShapeSeed(2, "Talhão 2 - Fruticultura Irrigada", 46.0, -26.0, 34.0, 26.0, 0.25, 0.16, 8.8, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, secondCrop, 0.78, "Cooperativa Frutícola", isIrrigated = true),
            PlotShapeSeed(3, "Talhão 3 - Várzea do Açude", -36.0, 64.0, 32.0, 26.0, 0.20, 0.18, 8.2, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, thirdCrop, 1.0, "Sítio Bom Jesus", isIrrigated = true),
            PlotShapeSeed(4, "Talhão 4 - Lajedo da Caatinga", 44.0, 64.0, 32.0, 26.0, -0.34, 0.22, 9.0, zeroOwned, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PEDRAS_MATO, null, 0.0, if (zeroOwned) "Você" else "Fernando Vizinho"),
            PlotShapeSeed(5, "Reserva Caatinga Preservada", -210.0, -8.0, 62.0, 52.0, 0.18, 0.25, 12.0, zeroOwned, true, PlotZoneType.PRESERVACAO, false, SoilState.AREA_PRESERVACAO, null, 0.0, "APP Caatinga"),
            PlotShapeSeed(6, "Fazenda Cajueiral do Sertão", -110.0, 215.0, 64.0, 44.0, -0.20, 0.18, 16.5, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, defaultCrop, 1.0, "Agropecuária Sertão", isIrrigated = true),
            PlotShapeSeed(7, "Sítio Mandacaru (Agricultura Familiar)", -112.0, 42.0, 30.0, 22.0, 0.35, 0.18, 2.4, false, false, PlotZoneType.SITIO_FAMILIAR, false, SoilState.PRONTO_COLHEITA, "rosas_familiares", 1.0, "Dona Aparecida", isIrrigated = true),
            PlotShapeSeed(8, "Polo Irrigado Exportação", 218.0, 42.0, 68.0, 54.0, 0.16, 0.14, 21.0, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, secondCrop, 0.65, "Packing House Nordeste", isIrrigated = true),
            PlotShapeSeed(9, "Caprinocultura & Palma Forrageira", 195.0, 225.0, 64.0, 44.0, -0.28, 0.20, 12.5, false, false, PlotZoneType.PASTAGEM, false, SoilState.PASTAGEM, null, 0.0, "Fazenda Sertaneja"),
            PlotShapeSeed(10, "Caatinga Nativa Arbórea", -195.0, -225.0, 72.0, 52.0, 0.26, 0.25, 15.0, false, false, PlotZoneType.FLORESTAL, false, SoilState.FLORESTA_NATIVA, null, 0.0, "Reserva Sertão"),
        )
        RegionalArchetype.AMAZONIA_FLORESTA -> listOf(
            PlotShapeSeed(1, "Talhão 1 - Agrofloresta Sede", -38.0, -28.0, 32.0, 25.0, -0.28, 0.22, 7.2, zeroOwned, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, defaultCrop, 1.0, if (zeroOwned) "Você" else "Sr. Antônio"),
            PlotShapeSeed(2, "Talhão 2 - Cacaual da Mata", 46.0, -26.0, 34.0, 26.0, 0.35, 0.22, 8.9, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, secondCrop, 0.72, "Cooperativa Amazônica"),
            PlotShapeSeed(3, "Talhão 3 - Várzea Ribeirinha", -34.0, 64.0, 32.0, 25.0, 0.22, 0.24, 8.4, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, thirdCrop, 1.0, "Família Ribeirinha"),
            PlotShapeSeed(4, "Talhão 4 - Pasto Aberto", 44.0, 64.0, 32.0, 26.0, -0.35, 0.20, 8.8, zeroOwned, false, PlotZoneType.PASTAGEM, false, SoilState.PASTAGEM, null, 0.0, if (zeroOwned) "Você" else "Fernando Vizinho"),
            PlotShapeSeed(5, "Reserva Florestal Amazônica (APP)", -206.0, -8.0, 70.0, 58.0, 0.18, 0.26, 18.0, zeroOwned, true, PlotZoneType.PRESERVACAO, false, SoilState.AREA_PRESERVACAO, null, 0.0, "APP Amazônia"),
            PlotShapeSeed(6, "Fazenda Tapajós (Grãos)", -110.0, 215.0, 66.0, 44.0, -0.24, 0.22, 16.0, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, "soja", 0.58, "Agropecuária Norte"),
            PlotShapeSeed(7, "Sítio Igarapé Claro", -112.0, 42.0, 30.0, 22.0, 0.45, 0.20, 2.4, false, false, PlotZoneType.SITIO_FAMILIAR, false, SoilState.PRONTO_COLHEITA, "rosas_familiares", 1.0, "Dona Aparecida"),
            PlotShapeSeed(8, "Polo Açaí & Mandioca", 215.0, 42.0, 68.0, 52.0, 0.30, 0.24, 19.0, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, defaultCrop, 1.0, "Polpadora Fluvial"),
            PlotShapeSeed(9, "Pastagem Brachiaria Norte", 195.0, 225.0, 66.0, 46.0, -0.35, 0.21, 13.5, false, false, PlotZoneType.PASTAGEM, false, SoilState.PASTAGEM, null, 0.0, "Fazenda Pará"),
            PlotShapeSeed(10, "Floresta Densa Primária", -192.0, -222.0, 78.0, 58.0, 0.26, 0.25, 22.0, false, false, PlotZoneType.FLORESTAL, false, SoilState.FLORESTA_NATIVA, null, 0.0, "Florestal Norte"),
        )
        else -> listOf(
            PlotShapeSeed(1, "Talhão 1 - Sede (Terra Roxa)", -38.0, -28.0, 34.0, 26.0, -0.28, 0.16, 8.2, zeroOwned, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, defaultCrop, 1.0, if (zeroOwned) "Você" else "Sr. Antônio"),
            PlotShapeSeed(2, "Talhão 2 - Leste (Espigão)", 46.0, -26.0, 36.0, 28.0, 0.35, 0.2, 11.4, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, defaultCrop, 1.0, "Sr. Antônio"),
            PlotShapeSeed(3, "Talhão 3 - Norte (Beira-Rio)", -34.0, 64.0, 34.0, 26.0, 0.22, 0.24, 12.8, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, secondCrop, 0.65, "Família Kowalski"),
            PlotShapeSeed(4, "Talhão 4 - Área de Abertura", 44.0, 64.0, 32.0, 28.0, -0.42, 0.19, 9.6, zeroOwned, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PEDRAS_MATO, null, 0.0, if (zeroOwned) "Você" else "Fernando Vizinho"),
            PlotShapeSeed(5, "Reserva Legal Araucárias (APP)", -210.0, -8.0, 62.0, 52.0, 0.18, 0.26, 12.0, zeroOwned, true, PlotZoneType.PRESERVACAO, false, SoilState.AREA_PRESERVACAO, null, 0.0, "APP Ambiental"),
            PlotShapeSeed(6, "Fazenda Boa Esperança (Grande)", -110.0, 215.0, 72.0, 46.0, -0.24, 0.22, 19.5, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.CRESCENDO, secondCrop, 0.55, "Agropecuária Horizonte"),
            PlotShapeSeed(7, "Sítio Ribeirão Claro (Rosas Familiares)", -112.0, 42.0, 32.0, 22.0, 0.45, 0.2, 2.4, false, false, PlotZoneType.SITIO_FAMILIAR, false, SoilState.PRONTO_COLHEITA, "rosas_familiares", 1.0, "Dona Aparecida"),
            PlotShapeSeed(8, "Fazenda Cooperativa (Mega Talhão)", 220.0, 42.0, 76.0, 62.0, 0.3, 0.23, 26.0, false, false, PlotZoneType.RURAL_CULTIVO, false, SoilState.PRONTO_COLHEITA, thirdCrop, 1.0, "Cooperativa Sul"),
            PlotShapeSeed(9, "Pastagem do Vale (Pecuária)", 195.0, 225.0, 66.0, 46.0, -0.35, 0.21, 13.5, false, false, PlotZoneType.PASTAGEM, false, SoilState.PASTAGEM, null, 0.0, "Haras Sul"),
            PlotShapeSeed(10, "Gleba Florestal Pinhal", -195.0, -225.0, 72.0, 52.0, 0.26, 0.25, 15.0, false, false, PlotZoneType.FLORESTAL, false, SoilState.FLORESTA_NATIVA, null, 0.0, "Florestal Pinhal"),
        )
    }

    val extraSpecs = listOf(
        ExtraPlotSpec(12, "Chácara Flor do Campo (Rosas)", -78.0, 18.0, 22.0, 18.0, 0.3, PlotZoneType.SITIO_FAMILIAR, SoilState.CRESCENDO, "rosas_familiares", 0.72),
        ExtraPlotSpec(13, "Sítio Morango da Serra", -145.0, -18.0, 24.0, 19.0, -0.4, PlotZoneType.SITIO_FAMILIAR, SoilState.PRONTO_COLHEITA, "morango_familiar", 1.0),
        ExtraPlotSpec(14, "Horta Comunitária Vila", 112.0, -45.0, 22.0, 18.0, 0.2, PlotZoneType.SITIO_FAMILIAR, SoilState.CRESCENDO, "rosas_familiares", 0.6),
        ExtraPlotSpec(15, "Sítio Santa Luzia", -96.0, -155.0, 28.0, 22.0, 0.5, PlotZoneType.SITIO_FAMILIAR, SoilState.ARADO, null, 0.0),
        ExtraPlotSpec(16, "Chácara Ipê Amarelo", 110.0, 42.0, 26.0, 21.0, -0.25, PlotZoneType.SITIO_FAMILIAR, SoilState.PALHADA, null, 0.0),
        ExtraPlotSpec(17, "Sítio Água Limpa", -260.0, 82.0, 32.0, 24.0, 0.42, PlotZoneType.SITIO_FAMILIAR, SoilState.CRESCENDO, defaultCrop, 0.5),
        ExtraPlotSpec(18, "Chácara Primavera (Rosas)", 225.0, -45.0, 25.0, 20.0, -0.35, PlotZoneType.SITIO_FAMILIAR, SoilState.PRONTO_COLHEITA, "rosas_familiares", 1.0),
        ExtraPlotSpec(19, "Sítio Capão Redondo", -28.0, 128.0, 28.0, 22.0, 0.15, PlotZoneType.SITIO_FAMILIAR, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(20, "Talhão 20 - Curva do Cedro", -155.0, 112.0, 38.0, 28.0, -0.4, PlotZoneType.RURAL_CULTIVO, SoilState.PALHADA, null, 0.0),
        ExtraPlotSpec(21, "Talhão 21 - Chapadão Oeste", -265.0, 165.0, 46.0, 36.0, 0.25, PlotZoneType.RURAL_CULTIVO, SoilState.CRESCENDO, defaultCrop, 0.48),
        ExtraPlotSpec(22, "Talhão 22 - Várzea Sul", -265.0, -145.0, 44.0, 34.0, -0.3, PlotZoneType.RURAL_CULTIVO, SoilState.ARADO, null, 0.0),
        ExtraPlotSpec(23, "Talhão 23 - Encosta Alta", -105.0, -265.0, 45.0, 32.0, 0.38, PlotZoneType.RURAL_CULTIVO, SoilState.PALHADA, null, 0.0),
        ExtraPlotSpec(24, "Talhão 24 - Espigão Central", -18.0, -265.0, 42.0, 32.0, -0.18, PlotZoneType.RURAL_CULTIVO, SoilState.CRESCENDO, secondCrop, 0.64),
        ExtraPlotSpec(25, "Talhão 25 - Baixada Fértil", 64.0, -265.0, 44.0, 34.0, 0.29, PlotZoneType.RURAL_CULTIVO, SoilState.PRONTO_COLHEITA, defaultCrop, 1.0),
        ExtraPlotSpec(26, "Talhão 26 - Córrego Fundo", 155.0, -265.0, 46.0, 34.0, -0.32, PlotZoneType.RURAL_CULTIVO, SoilState.ARADO, null, 0.0),
        ExtraPlotSpec(27, "Talhão 27 - Alto Regional", 255.0, -255.0, 48.0, 36.0, 0.44, PlotZoneType.RURAL_CULTIVO, SoilState.PALHADA, null, 0.0),
        ExtraPlotSpec(28, "Talhão 28 - Gleba Leste", 275.0, -165.0, 44.0, 34.0, -0.22, PlotZoneType.RURAL_CULTIVO, SoilState.CRESCENDO, thirdCrop, 0.7),
        ExtraPlotSpec(29, "Talhão 29 - Divisa Leste", 305.0, -45.0, 40.0, 34.0, 0.16, PlotZoneType.RURAL_CULTIVO, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(30, "Talhão 30 - Mirante do Sol", 310.0, 125.0, 44.0, 38.0, -0.36, PlotZoneType.RURAL_CULTIVO, SoilState.PRONTO_COLHEITA, secondCrop, 1.0),
        ExtraPlotSpec(31, "Talhão 31 - Campos Regionais", 295.0, 255.0, 46.0, 36.0, 0.28, PlotZoneType.RURAL_CULTIVO, SoilState.ARADO, null, 0.0),
        ExtraPlotSpec(32, "Talhão 32 - Planalto Norte", 105.0, 285.0, 48.0, 36.0, -0.24, PlotZoneType.RURAL_CULTIVO, SoilState.CRESCENDO, defaultCrop, 0.42),
        ExtraPlotSpec(33, "Talhão 33 - Vale Verde", 15.0, 285.0, 44.0, 34.0, 0.33, PlotZoneType.RURAL_CULTIVO, SoilState.PALHADA, null, 0.0),
        ExtraPlotSpec(34, "Talhão 34 - Sudoeste", -195.0, 290.0, 48.0, 34.0, -0.14, PlotZoneType.RURAL_CULTIVO, SoilState.PRONTO_COLHEITA, thirdCrop, 1.0),
        ExtraPlotSpec(35, "Talhão 35 - Fronteira Sul", -295.0, 275.0, 44.0, 36.0, 0.41, PlotZoneType.RURAL_CULTIVO, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(36, "Talhão 36 - Rio Abaixo", -305.0, -38.0, 42.0, 34.0, -0.29, PlotZoneType.RURAL_CULTIVO, SoilState.PALHADA, null, 0.0),
        ExtraPlotSpec(37, "Talhão 37 - Pontal Norte", -295.0, -275.0, 44.0, 34.0, 0.21, PlotZoneType.RURAL_CULTIVO, SoilState.ARADO, null, 0.0),
        ExtraPlotSpec(38, "Talhão 38 - Pedreira Antiga", -138.0, -96.0, 30.0, 22.0, -0.45, PlotZoneType.PASTAGEM, SoilState.PEDRAS_MATO, null, 0.0),
        ExtraPlotSpec(39, "Talhão 39 - Pasto Regional", 42.0, 188.0, 38.0, 28.0, 0.19, PlotZoneType.PASTAGEM, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(40, "Talhão 40 - Invernada Norte", 115.0, 132.0, 36.0, 26.0, -0.38, PlotZoneType.PASTAGEM, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(41, "Talhão 41 - Piquete Beira-Lago", 55.0, 275.0, 34.0, 25.0, 0.27, PlotZoneType.PASTAGEM, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(42, "Talhão 42 - Capoeirão Leste", 285.0, 195.0, 34.0, 28.0, -0.25, PlotZoneType.RURAL_CULTIVO, SoilState.PEDRAS_MATO, null, 0.0),
        ExtraPlotSpec(43, "Talhão 43 - Mata Regional", 18.0, -172.0, 36.0, 26.0, 0.34, PlotZoneType.FLORESTAL, SoilState.FLORESTA_NATIVA, null, 0.0),
        ExtraPlotSpec(44, "Talhão 44 - Reflorestamento Sul", -95.0, 128.0, 34.0, 24.0, -0.21, PlotZoneType.FLORESTAL, SoilState.FLORESTA_NATIVA, null, 0.0),
        ExtraPlotSpec(45, "Talhão 45 - Lavoura Tabuleiro", 124.0, -182.0, 36.0, 28.0, 0.42, PlotZoneType.RURAL_CULTIVO, SoilState.CRESCENDO, defaultCrop, 0.81),
        ExtraPlotSpec(46, "Talhão 46 - Serra Dourada", -220.0, 122.0, 36.0, 28.0, -0.31, PlotZoneType.RURAL_CULTIVO, SoilState.ARADO, null, 0.0),
        ExtraPlotSpec(47, "Talhão 47 - Colina Boa Vista", 152.0, 16.0, 32.0, 24.0, 0.24, PlotZoneType.RURAL_CULTIVO, SoilState.PALHADA, null, 0.0),
        ExtraPlotSpec(48, "Lote Urbano 01 - Praça Central", 132.0, -88.0, 12.0, 11.0, 0.0, PlotZoneType.URBANO_RESIDENCIAL, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(49, "Lote Urbano 02 - Rua das Flores", 152.0, -88.0, 12.0, 11.0, 0.0, PlotZoneType.URBANO_COMERCIAL, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(50, "Lote Urbano 03 - Estufa Sul", 172.0, -88.0, 12.0, 11.0, 0.0, PlotZoneType.URBANO_COMERCIAL, SoilState.CRESCENDO, "rosas_familiares", 0.85),
        ExtraPlotSpec(51, "Lote Urbano 04 - Esquina Posto", 192.0, -88.0, 12.0, 11.0, 0.0, PlotZoneType.URBANO_COMERCIAL, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(52, "Lote Urbano 05 - Residencial Ipê", 132.0, -108.0, 12.0, 11.0, 0.0, PlotZoneType.URBANO_RESIDENCIAL, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(53, "Lote Urbano 06 - Jardim da Vila", 152.0, -108.0, 12.0, 11.0, 0.0, PlotZoneType.URBANO_RESIDENCIAL, SoilState.PRONTO_COLHEITA, "rosas_familiares", 1.0),
        ExtraPlotSpec(54, "Lote Urbano 07 - Cooperativa Urbana", 172.0, -108.0, 12.0, 11.0, 0.0, PlotZoneType.URBANO_COMERCIAL, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(55, "Lote Urbano 08 - Chacrinha Vila", 192.0, -108.0, 12.0, 11.0, 0.0, PlotZoneType.URBANO_RESIDENCIAL, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(56, "Lote Urbano 09 - Quadra Norte", 142.0, -128.0, 13.0, 11.0, 0.0, PlotZoneType.URBANO_RESIDENCIAL, SoilState.PASTAGEM, null, 0.0),
        ExtraPlotSpec(57, "Lote Urbano 10 - Quadra Comercial Norte", 174.0, -128.0, 13.0, 11.0, 0.0, PlotZoneType.URBANO_COMERCIAL, SoilState.PASTAGEM, null, 0.0),
    )

    val extraPlots = extraSpecs.mapIndexed { idx, spec ->
        val isUrban = spec.zoneType == PlotZoneType.URBANO_RESIDENCIAL || spec.zoneType == PlotZoneType.URBANO_COMERCIAL
        val isCerradoSquare = !isUrban && (archetype == RegionalArchetype.CERRADO_CHAPADAO || archetype == RegionalArchetype.PRADARIA_EUA)
        val scaleRx = if (isCerradoSquare) spec.rx * 1.18 else spec.rx
        val scaleRz = if (isCerradoSquare) spec.rz * 1.15 else spec.rz
        val wobble = when {
            isUrban -> 0.02
            isCerradoSquare -> 0.05
            archetype == RegionalArchetype.SERRA_CAFE_MG -> 0.25
            else -> 0.2 + ((idx * 7) % 10) * 0.01
        }
        val approxHa = if (isUrban) {
            ((0.04 + (idx % 4) * 0.02) * 100.0).roundToInt() / 100.0
        } else {
            (((scaleRx * scaleRz * PI) / 1000.0) * 10.0).roundToInt() / 10.0
        }
        PlotShapeSeed(
            id = spec.id,
            name = spec.name,
            cx = spec.cx,
            cz = spec.cz,
            rx = scaleRx,
            rz = scaleRz,
            angle = if (isCerradoSquare) 0.0 else spec.angle,
            wobble = wobble,
            areaHa = approxHa,
            owned = false,
            isPreservation = false,
            zoneType = spec.zoneType,
            isUrbanLot = isUrban,
            initialSoil = spec.initialSoil,
            initialCropId = spec.initialCropId,
            initialGrowth = spec.initialGrowth,
            ownerNpcName = OWNER_NPC_LIST[idx % OWNER_NPC_LIST.size],
            isIrrigated = archetype == RegionalArchetype.CAATINGA_SERTAO && spec.initialCropId != null,
        )
    }

    return corePlots + extraPlots
}

fun createNewWorld(
    slotIndex: Int,
    farmName: stringCompat,
    mapId: String,
    startingMoneyBrl: Double,
    startingMode: String = "Padrão",
    requestedGridSide: Int = GRID_SIZE,
): WorldState {
    val mapDef = getMapById(mapId)
    val archetype = resolveRegionalArchetype(mapDef)
    val rng = createRng(mapDef.seed)
    val isZeroMode = startingMode == "Fazenda Zerada"
    val effectiveStartingMoney = if (isZeroMode) 0.0 else startingMoneyBrl

    val gridSide = if (requestedGridSide in setOf(64, 96, 128, 160, 192)) requestedGridSide else GRID_SIZE
    val cellSizeM = getCellSize(gridSide)
    val coordScale = (gridSide * cellSizeM).toDouble() / (GRID_SIZE * CELL_SIZE_M).toDouble()

    val defaultCrop = mapDef.featuredCrops.getOrElse(0) { "soja" }
    val secondCrop = mapDef.featuredCrops.getOrElse(1) { "milho" }
    val thirdCrop = mapDef.featuredCrops.getOrElse(2) { defaultCrop }

    val blueprints = build56PlotBlueprints(mapDef, isZeroMode, defaultCrop, secondCrop, thirdCrop)
    val bpMap = blueprints.associateBy { it.id }

    val cells = ArrayList<TerrainCell>(gridSide * gridSide)
    val props = mutableListOf<WorldProp>()
    val phase = (mapDef.seed % 97) * 0.23

    for (gz in 0 until gridSide) {
        for (gx in 0 until gridSide) {
            val wxRaw = (gx - gridSide / 2.0) * cellSizeM + cellSizeM / 2.0
            val wzRaw = (gz - gridSide / 2.0) * cellSizeM + cellSizeM / 2.0
            val wx = wxRaw / coordScale
            val wz = wzRaw / coordScale

            val riverZ = riverCenterZ(mapDef, wx)
            val riverHalfWidth = when (archetype) {
                RegionalArchetype.AMAZONIA_FLORESTA -> 12.5
                RegionalArchetype.PANTANAL_VARZEA -> 11.0
                RegionalArchetype.CAATINGA_SERTAO -> 6.5
                else -> 8.0
            }
            val isRiverBand = mapDef.hasRiver && abs(wz - riverZ) < riverHalfWidth
            val isNaturalLake = isInNaturalLake(mapDef, wx, wz)

            val highwayZ = when (archetype) {
                RegionalArchetype.CERRADO_CHAPADAO, RegionalArchetype.PRADARIA_EUA, RegionalArchetype.CANA_CITROS_SP -> -70.0
                RegionalArchetype.SERRA_CAFE_MG -> -66.0 + sin(wx * 0.015 + phase) * 18.0
                else -> -66.0 + sin(wx * 0.008 + phase * 0.3) * 9.0
            }
            val highwayX = when (archetype) {
                RegionalArchetype.CERRADO_CHAPADAO, RegionalArchetype.PRADARIA_EUA -> -88.0
                RegionalArchetype.SERRA_CAFE_MG -> -68.0 + cos(wz * 0.016 + phase) * 18.0
                else -> -66.0 + cos(wz * 0.007 + phase * 0.2) * 8.0
            }
            val highwayHalf = if (archetype == RegionalArchetype.CANA_CITROS_SP) 7.5 else 6.2
            val isMainHighwayEW = abs(wz - highwayZ) < highwayHalf
            val isMainHighwayNS = abs(wx - highwayX) < highwayHalf

            val isVillageEastStreet = wx in 104.0..205.0 &&
                wz in -135.0..-66.0 &&
                (abs(wz + 98.0) < 3.6 || abs(wz + 118.0) < 3.6 || abs(wx - 162.0) < 3.6 || abs(wx - 112.0) < 4.2)
            val isVillageWestStreet = wx in -184.0..-122.0 &&
                wz in -122.0..-66.0 &&
                (abs(wz + 98.0) < 3.6 || abs(wx - (-152.0)) < 4.0)
            val isVillageStreet = isVillageEastStreet || isVillageWestStreet

            val isStationYard = com.example.domain.ESTABLISHMENTS.any { est ->
                hypot(wx - est.worldX, wz - est.worldZ) < 16.5
            } || (wx in 104.0..204.0 && wz in -120.0..-66.0) || (wx in -184.0..-122.0 && wz in -120.0..-66.0)

            val isHomesteadCompound = wxRaw in -28.0..40.0 && wzRaw in 14.0..43.0

            val isBridge = isRiverBand &&
                (abs(wx - highwayX) < 8.5 || wx in 78.0..94.0 || wx in -228.0..-212.0)
            val isAsphalt = !isRiverBand && !isNaturalLake && (isMainHighwayEW || isMainHighwayNS || isVillageStreet)

            val isDirtRoad = !isAsphalt && !isRiverBand && !isNaturalLake && !isHomesteadCompound && (isStationYard || when (archetype) {
                RegionalArchetype.CERRADO_CHAPADAO, RegionalArchetype.PRADARIA_EUA -> (
                    (wx in -6.0..2.0 && wz in -315.0..315.0) ||
                        (abs(wz - 14.0) < 4.8 && wx in -310.0..310.0) ||
                        (abs(wx - 104.0) < 5.2 && wz in -310.0..310.0) ||
                        (abs(wz - 155.0) < 4.8 && wx in -310.0..310.0) ||
                        (abs(wz + 158.0) < 4.8 && wx in -310.0..310.0)
                    )
                RegionalArchetype.SERRA_CAFE_MG, RegionalArchetype.SERRA_PINHAIS_SC -> (
                    (abs(wx - (-4.0 + sin(wz * 0.025 + phase) * 11.0)) < 4.2 && wz in -300.0..300.0) ||
                        (abs(wz - (12.0 + sin(wx * 0.022 + phase) * 14.0)) < 4.2 && wx in -295.0..295.0) ||
                        (abs(wz - (142.0 + cos(wx * 0.020) * 18.0)) < 4.2 && wx in -280.0..280.0) ||
                        (abs(wx - (84.0 + sin(wz * 0.019) * 15.0)) < 4.2 && wz in -290.0..290.0)
                    )
                RegionalArchetype.CAATINGA_SERTAO -> (
                    (wx in -6.0..1.0 && wz in -290.0..290.0) ||
                        (abs(wz - (10.0 + cos(wx * 0.014 + phase) * 8.0)) < 4.0 && wx in -295.0..295.0) ||
                        (abs(wx - (82.0 + sin(wz * 0.015) * 9.0)) < 4.0 && wz in -285.0..285.0) ||
                        (abs(wz - 145.0) < 4.0 && wx in -275.0..275.0)
                    )
                else -> {
                    val dirtCurveA = 12.0 + sin(wx * 0.011 + phase) * 5.5
                    val dirtCurveB = 148.0 + cos(wx * 0.01 - phase) * 12.0
                    val dirtCurveX = 86.0 + sin(wz * 0.012 + phase) * 8.0
                    (wx in -7.0..1.0 && wz in -310.0..310.0) ||
                        (abs(wz - dirtCurveA) < 4.4 && wx in -300.0..300.0) ||
                        (abs(wx - dirtCurveX) < 4.8 && wz in -300.0..300.0) ||
                        (abs(wz - dirtCurveB) < 4.5 && wx in -290.0..295.0) ||
                        (wz in -218.0..-209.0 && wx in -290.0..295.0)
                }
            })

            val baseH = computeHeight(mapDef, wxRaw, wzRaw, isAsphalt || isDirtRoad)
            val h = when {
                isBridge -> max(baseH + 0.6, 0.45)
                isRiverBand -> -1.45
                isNaturalLake -> -1.25
                else -> baseH
            }

            var plotId = 0
            if (isAsphalt || isDirtRoad || isRiverBand || isNaturalLake || isStationYard) {
                plotId = 0
            } else if ((wx in -12.0..38.0 && wz in 4.0..48.0) || isHomesteadCompound) {
                plotId = 0
            } else {
                for (bp in blueprints) {
                    if (bp.id == 1 &&
                        (inOrganicField(wx, wz, bp.cx, bp.cz, bp.rx, bp.rz, bp.angle, bp.wobble, phase) ||
                            (wx in -28.0..-11.0 && wz in -44.0..-2.0))
                    ) {
                        plotId = 1
                        break
                    }
                    if (inOrganicField(wx, wz, bp.cx, bp.cz, bp.rx, bp.rz, bp.angle, bp.wobble, phase + bp.id * 0.7)) {
                        plotId = bp.id
                        break
                    }
                }
            }

            val assignedBp = if (plotId > 0) bpMap[plotId] else null
            val isSertaoRockOutcrop = archetype == RegionalArchetype.CAATINGA_SERTAO &&
                assignedBp == null &&
                (hypot(wx + 175.0, wz - 140.0) < 38.0 || hypot(wx - 195.0, wz + 185.0) < 34.0)
            val soil = when {
                isBridge -> SoilState.PONTE
                isRiverBand || isNaturalLake -> SoilState.AGUA_RIO
                isAsphalt -> SoilState.ESTRADA_ASFALTO
                isDirtRoad -> SoilState.ESTRADA_TERRA
                plotId == 1 -> when {
                    wz < -24.0 -> SoilState.PRONTO_COLHEITA
                    wz < -14.0 -> SoilState.PALHADA
                    else -> SoilState.ARADO
                }
                assignedBp != null -> assignedBp.initialSoil
                isSertaoRockOutcrop -> SoilState.PEDRAS_MATO
                abs(wx) > 332.0 || abs(wz) > 332.0 -> SoilState.FLORESTA_NATIVA
                else -> SoilState.PASTAGEM
            }

            var cropId: String? = null
            var growthProgress = 0.0
            if (soil == SoilState.PRONTO_COLHEITA) {
                cropId = assignedBp?.initialCropId ?: defaultCrop
                growthProgress = 1.0
            } else if (soil == SoilState.CRESCENDO) {
                cropId = assignedBp?.initialCropId ?: secondCrop
                growthProgress = assignedBp?.initialGrowth ?: 0.55
            }

            val baseSoilN = when (soil) {
                SoilState.PALHADA -> 0.78
                SoilState.PASTAGEM -> 0.64
                else -> 0.62
            }
            val baseSoilP = if (soil == SoilState.PALHADA) 0.72 else 0.58
            val baseSoilK = if (soil == SoilState.PALHADA) 0.76 else 0.61
            val basePh = when (archetype) {
                RegionalArchetype.CERRADO_CHAPADAO -> 5.4
                RegionalArchetype.CAATINGA_SERTAO -> 6.6
                RegionalArchetype.AMAZONIA_FLORESTA -> 5.1
                else -> 6.0
            }
            val cellIrrigated = assignedBp?.isIrrigated == true
            val cellMoisture = when {
                cellIrrigated -> 0.80
                archetype == RegionalArchetype.CAATINGA_SERTAO -> 0.34
                archetype == RegionalArchetype.PANTANAL_VARZEA || archetype == RegionalArchetype.AMAZONIA_FLORESTA -> 0.78
                else -> 0.62
            }

            cells.add(
                TerrainCell(
                    gx = gx,
                    gz = gz,
                    height = h,
                    plotId = plotId,
                    soilState = soil,
                    cropId = cropId,
                    growthProgress = growthProgress,
                    moisture = cellMoisture,
                    fertility = if (soil == SoilState.PALHADA) 0.86 else 0.74,
                    compaction = if (soil == SoilState.ESTRADA_TERRA) 0.42 else 0.12,
                    weedLevel = 0.05,
                    nitrogen = baseSoilN,
                    phosphorus = baseSoilP,
                    potassium = baseSoilK,
                    phLevel = basePh,
                    pestLevel = 0.02,
                    fertilizedStages = if (soil == SoilState.PALHADA) 1 else 0,
                    lastCropId = if (soil == SoilState.PALHADA) defaultCrop else null,
                    isIrrigated = cellIrrigated,
                    irrigatedUntilDay = if (cellIrrigated) 14 else 0,
                    pastureRestDays = if (soil == SoilState.PASTAGEM) 4 else 0,
                    erosionLevel = 0.0,
                    contourProtected = archetype == RegionalArchetype.SERRA_CAFE_MG || archetype == RegionalArchetype.PARANA_TERRA_ROXA,
                    waterStress = 0.0,
                    lastYieldKgPerHa = 0.0,
                )
            )
        }
    }

    // Single-pass morphological smoothing to eliminate isolated 1-cell border jaggedness on plots
    val originalPlotIds = IntArray(cells.size) { cells[it].plotId }
    for (gz in 1 until gridSide - 1) {
        for (gx in 1 until gridSide - 1) {
            val idx = gz * gridSide + gx
            val c = cells[idx]
            if (c.soilState == SoilState.ESTRADA_ASFALTO ||
                c.soilState == SoilState.ESTRADA_TERRA ||
                c.soilState == SoilState.AGUA_RIO ||
                c.soilState == SoilState.PONTE
            ) continue
            val nLeft = originalPlotIds[idx - 1]
            val nRight = originalPlotIds[idx + 1]
            val nUp = originalPlotIds[idx - gridSide]
            val nDown = originalPlotIds[idx + gridSide]
            if (c.plotId == 0) {
                val match = listOf(nLeft, nRight, nUp, nDown).filter { it > 0 }
                    .groupingBy { it }.eachCount().maxByOrNull { it.value }
                if (match != null && match.value >= 3) {
                    val bp = bpMap[match.key]
                    if (bp != null) {
                        val s = if (match.key == 1) SoilState.ARADO else bp.initialSoil
                        cells[idx] = c.copy(
                            plotId = bp.id,
                            soilState = s,
                            cropId = if (s == SoilState.PRONTO_COLHEITA || s == SoilState.CRESCENDO) (bp.initialCropId ?: defaultCrop) else null,
                            growthProgress = if (s == SoilState.PRONTO_COLHEITA) 1.0 else if (s == SoilState.CRESCENDO) bp.initialGrowth else 0.0,
                        )
                    }
                }
            } else {
                val differentCount = listOf(nLeft, nRight, nUp, nDown).count { it != c.plotId }
                if (differentCount >= 3 && c.plotId != 1) {
                    cells[idx] = c.copy(plotId = 0, soilState = SoilState.PASTAGEM, cropId = null, growthProgress = 0.0)
                }
            }
        }
    }

    val cellCountsByPlot = mutableMapOf<Int, Int>()
    for (c in cells) {
        if (c.plotId > 0) cellCountsByPlot[c.plotId] = (cellCountsByPlot[c.plotId] ?: 0) + 1
    }
    for (bp in blueprints) {
        if ((cellCountsByPlot[bp.id] ?: 0) > 0) continue
        var bestIdx = -1
        var bestDist = Double.POSITIVE_INFINITY
        for (i in cells.indices) {
            val c = cells[i]
            if (c.plotId != 0 ||
                c.soilState == SoilState.AGUA_RIO ||
                c.soilState == SoilState.ESTRADA_ASFALTO ||
                c.soilState == SoilState.ESTRADA_TERRA ||
                c.soilState == SoilState.PONTE
            ) continue
            val wx = ((c.gx - gridSide / 2.0) * cellSizeM + cellSizeM / 2.0) / coordScale
            val wz = ((c.gz - gridSide / 2.0) * cellSizeM + cellSizeM / 2.0) / coordScale
            val d = hypot(wx - bp.cx, wz - bp.cz)
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        if (bestIdx >= 0) {
            cells[bestIdx] = cells[bestIdx].copy(
                plotId = bp.id,
                soilState = bp.initialSoil,
                cropId = bp.initialCropId,
                growthProgress = bp.initialGrowth,
            )
            cellCountsByPlot[bp.id] = 1
        }
    }

    val plots = blueprints.map { bp ->
        val rawBuy = (mapDef.startingLandValuePerHa * bp.areaHa * (if (bp.isUrbanLot) 2.4 else 1.0)).roundToInt().toDouble()
        val buyPrice = if (bp.owned || bp.isPreservation) 0.0 else max(4500.0, rawBuy)
        val leaseDaily = if (bp.owned || bp.isPreservation) 0.0 else max(85.0, (bp.areaHa * (if (bp.isUrbanLot) 420.0 else 185.0)).roundToInt().toDouble())
        val neighborIds = blueprints
            .filter { other -> other.id != bp.id && hypot(other.cx - bp.cx, other.cz - bp.cz) <= 105.0 }
            .map { it.id }

        FieldPlot(
            id = bp.id,
            name = bp.name,
            areaHa = bp.areaHa,
            owned = bp.owned,
            leased = false,
            isPreservation = bp.isPreservation,
            buyPriceBrl = buyPrice,
            leaseDailyBrl = leaseDaily,
            zoneType = bp.zoneType,
            isUrbanLot = bp.isUrbanLot,
            ownerNpcName = bp.ownerNpcName,
            neighborPlotIds = neighborIds,
            centerX = bp.cx * coordScale,
            centerZ = bp.cz * coordScale,
        )
    }

    for (gz in 1 until gridSide - 1) {
        for (gx in 1 until gridSide - 1) {
            val cell = cells[gz * gridSide + gx]
            val wxRaw = (gx - gridSide / 2.0) * cellSizeM + cellSizeM / 2.0
            val wzRaw = (gz - gridSide / 2.0) * cellSizeM + cellSizeM / 2.0
            val wx = wxRaw / coordScale
            val wz = wzRaw / coordScale

            if (wx in -16.0..42.0 && wz in -2.0..52.0) continue
            val soil = cell.soilState
            if (soil == SoilState.ESTRADA_ASFALTO ||
                soil == SoilState.ESTRADA_TERRA ||
                soil == SoilState.AGUA_RIO ||
                soil == SoilState.PONTE ||
                soil == SoilState.ARADO ||
                soil == SoilState.SUBSOLADO ||
                soil == SoilState.SEMEADO ||
                soil == SoilState.CRESCENDO ||
                soil == SoilState.PRONTO_COLHEITA ||
                soil == SoilState.PALHADA
            ) continue

            val n1 = cells[gz * gridSide + (gx - 1)]
            val n2 = cells[gz * gridSide + (gx + 1)]
            val n3 = cells[(gz - 1) * gridSide + gx]
            val n4 = cells[(gz + 1) * gridSide + gx]

            val nearWater = n1.soilState == SoilState.AGUA_RIO || n2.soilState == SoilState.AGUA_RIO ||
                n3.soilState == SoilState.AGUA_RIO || n4.soilState == SoilState.AGUA_RIO
            val nearRoad = n1.soilState == SoilState.ESTRADA_TERRA || n2.soilState == SoilState.ESTRADA_TERRA ||
                n3.soilState == SoilState.ESTRADA_TERRA || n4.soilState == SoilState.ESTRADA_TERRA ||
                n1.soilState == SoilState.ESTRADA_ASFALTO || n2.soilState == SoilState.ESTRADA_ASFALTO ||
                n3.soilState == SoilState.ESTRADA_ASFALTO || n4.soilState == SoilState.ESTRADA_ASFALTO
            val nearAsphalt = n1.soilState == SoilState.ESTRADA_ASFALTO || n3.soilState == SoilState.ESTRADA_ASFALTO
            val nearBorder = n1.plotId != cell.plotId || n2.plotId != cell.plotId ||
                n3.plotId != cell.plotId || n4.plotId != cell.plotId

            val px = wxRaw + (rng() - 0.5) * (cellSizeM * 0.72)
            val pz = wzRaw + (rng() - 0.5) * (cellSizeM * 0.72)
            val roll = rng()

            if (nearAsphalt && (gx + gz) % 7 == 0 && roll < 0.42) {
                props.add(WorldProp("pole_${gx}_$gz", WorldPropType.UTILITY_POLE, px, pz, 0.0, "energia"))
                continue
            }

            val cellPlot = if (cell.plotId > 0) bpMap[cell.plotId] else null
            if (cellPlot?.zoneType == PlotZoneType.SITIO_FAMILIAR && roll < 0.08) {
                props.add(WorldProp("rose_${gx}_$gz", WorldPropType.ROSE_CLUSTER, px, pz, 45.0, "rosas_familiares"))
                continue
            }

            if (soil == SoilState.FLORESTA_NATIVA || soil == SoilState.AREA_PRESERVACAO) {
                if (roll < 0.095) {
                    val sub = rng()
                    val type = when (archetype) {
                        RegionalArchetype.PARANA_TERRA_ROXA, RegionalArchetype.SERRA_PINHAIS_SC -> when {
                            sub < 0.44 -> WorldPropType.TREE_ARAUCARIA
                            sub < 0.62 -> WorldPropType.TREE_PINE
                            sub < 0.72 -> WorldPropType.TREE_IPE_AMARELO
                            sub < 0.86 -> WorldPropType.TREE_NATIVE
                            else -> WorldPropType.BUSH
                        }
                        RegionalArchetype.CERRADO_CHAPADAO -> when {
                            sub < 0.46 -> WorldPropType.TREE_CERRADO
                            sub < 0.62 -> WorldPropType.TREE_IPE_AMARELO
                            sub < 0.74 -> WorldPropType.TREE_IPE_ROXO
                            sub < 0.88 -> WorldPropType.BUSH
                            else -> WorldPropType.TREE_EUCALYPTUS
                        }
                        RegionalArchetype.CAATINGA_SERTAO -> when {
                            sub < 0.36 -> WorldPropType.CACTUS_MANDACARU
                            sub < 0.64 -> WorldPropType.BUSH
                            sub < 0.84 -> WorldPropType.TREE_CERRADO
                            sub < 0.92 -> WorldPropType.TREE_IPE_AMARELO
                            else -> WorldPropType.STONE
                        }
                        RegionalArchetype.AMAZONIA_FLORESTA -> when {
                            sub < 0.38 -> WorldPropType.TREE_PALM_AMAZON
                            sub < 0.50 -> WorldPropType.BANANA_CLUMP
                            sub < 0.86 -> WorldPropType.TREE_NATIVE
                            else -> WorldPropType.BUSH
                        }
                        RegionalArchetype.SERRA_CAFE_MG -> when {
                            sub < 0.24 -> WorldPropType.COFFEE_SHRUB
                            sub < 0.42 -> WorldPropType.TREE_IPE_AMARELO
                            sub < 0.56 -> WorldPropType.TREE_IPE_ROXO
                            sub < 0.76 -> WorldPropType.TREE_SHADE_MANGUEIRA
                            sub < 0.88 -> WorldPropType.BANANA_CLUMP
                            else -> WorldPropType.BUSH
                        }
                        else -> when {
                            sub < 0.30 -> WorldPropType.TREE_NATIVE
                            sub < 0.46 -> WorldPropType.TREE_SHADE_MANGUEIRA
                            sub < 0.60 -> WorldPropType.TREE_EUCALYPTUS
                            sub < 0.74 -> WorldPropType.TREE_IPE_AMARELO
                            sub < 0.86 -> WorldPropType.TREE_FRUIT
                            sub < 0.94 -> WorldPropType.BUSH
                            else -> WorldPropType.PLANT_CLUSTER
                        }
                    }
                    props.add(WorldProp("fl_${gx}_$gz", type, px, pz, 1400.0, "flora_nativa"))
                }
            } else if (soil == SoilState.PEDRAS_MATO) {
                if (roll < 0.12) {
                    val sub = rng()
                    val type = when {
                        archetype == RegionalArchetype.CAATINGA_SERTAO && sub < 0.35 -> WorldPropType.CACTUS_MANDACARU
                        sub < 0.52 -> WorldPropType.STONE
                        sub < 0.80 -> WorldPropType.BUSH
                        else -> WorldPropType.PLANT_CLUSTER
                    }
                    props.add(WorldProp("pm_${gx}_$gz", type, px, pz, 240.0, "pedra"))
                }
            } else if (nearWater) {
                if (roll < 0.14) {
                    val sub = rng()
                    val type = when (archetype) {
                        RegionalArchetype.AMAZONIA_FLORESTA, RegionalArchetype.PANTANAL_VARZEA, RegionalArchetype.CAATINGA_SERTAO ->
                            if (sub < 0.44) WorldPropType.TREE_PALM_AMAZON else if (sub < 0.62) WorldPropType.BANANA_CLUMP else if (sub < 0.84) WorldPropType.TREE_NATIVE else WorldPropType.BUSH
                        RegionalArchetype.PARANA_TERRA_ROXA, RegionalArchetype.SERRA_PINHAIS_SC ->
                            if (sub < 0.36) WorldPropType.TREE_ARAUCARIA else if (sub < 0.7) WorldPropType.TREE_NATIVE else WorldPropType.BUSH
                        else -> if (sub < 0.38) WorldPropType.TREE_SHADE_MANGUEIRA else if (sub < 0.62) WorldPropType.BANANA_CLUMP else if (sub < 0.84) WorldPropType.BUSH else WorldPropType.PLANT_CLUSTER
                    }
                    props.add(WorldProp("rip_${gx}_$gz", type, px, pz, 900.0, "mata_ciliar"))
                }
            } else if (nearRoad || nearBorder) {
                if (roll < 0.085) {
                    val sub = rng()
                    val type = when (archetype) {
                        RegionalArchetype.PARANA_TERRA_ROXA ->
                            if (sub < 0.28) WorldPropType.TREE_ARAUCARIA else if (sub < 0.42) WorldPropType.TREE_IPE_AMARELO else if (sub < 0.72) WorldPropType.PLANT_CLUSTER else WorldPropType.BUSH
                        RegionalArchetype.CERRADO_CHAPADAO ->
                            if (sub < 0.28) WorldPropType.TREE_CERRADO else if (sub < 0.44) WorldPropType.TREE_IPE_AMARELO else if (sub < 0.74) WorldPropType.BUSH else WorldPropType.PLANT_CLUSTER
                        RegionalArchetype.CAATINGA_SERTAO ->
                            if (sub < 0.32) WorldPropType.CACTUS_MANDACARU else if (sub < 0.62) WorldPropType.STONE else WorldPropType.BUSH
                        RegionalArchetype.SERRA_CAFE_MG ->
                            if (sub < 0.32) WorldPropType.COFFEE_SHRUB else if (sub < 0.50) WorldPropType.FLOWER_BED_AZALEA else if (sub < 0.76) WorldPropType.BUSH else WorldPropType.TREE_FRUIT
                        RegionalArchetype.AMAZONIA_FLORESTA ->
                            if (sub < 0.36) WorldPropType.TREE_PALM_AMAZON else if (sub < 0.56) WorldPropType.BANANA_CLUMP else if (sub < 0.78) WorldPropType.TREE_NATIVE else WorldPropType.BUSH
                        else ->
                            if (sub < 0.46) WorldPropType.PLANT_CLUSTER else if (sub < 0.76) WorldPropType.BUSH else WorldPropType.TREE_NATIVE
                    }
                    props.add(WorldProp("brd_${gx}_$gz", type, px, pz, 160.0, "vegetacao_borda"))
                }
            } else if (soil == SoilState.PASTAGEM && cell.plotId == 0) {
                val regionPatch = sin(wx * 0.032 + phase) * cos(wz * 0.029 - phase)
                val threshold = if (regionPatch > 0.2) 0.075 else 0.035
                if (roll < threshold) {
                    val sub = rng()
                    val type = when (archetype) {
                        RegionalArchetype.PARANA_TERRA_ROXA, RegionalArchetype.SERRA_PINHAIS_SC -> when {
                            sub < 0.34 -> WorldPropType.TREE_ARAUCARIA
                            sub < 0.46 -> WorldPropType.TREE_IPE_AMARELO
                            sub < 0.68 -> WorldPropType.PLANT_CLUSTER
                            sub < 0.88 -> WorldPropType.BUSH
                            else -> WorldPropType.TREE_NATIVE
                        }
                        RegionalArchetype.CERRADO_CHAPADAO -> when {
                            sub < 0.34 -> WorldPropType.TREE_CERRADO
                            sub < 0.48 -> WorldPropType.TREE_IPE_AMARELO
                            sub < 0.78 -> WorldPropType.BUSH
                            else -> WorldPropType.PLANT_CLUSTER
                        }
                        RegionalArchetype.CAATINGA_SERTAO -> when {
                            sub < 0.38 -> WorldPropType.CACTUS_MANDACARU
                            sub < 0.66 -> WorldPropType.STONE
                            sub < 0.88 -> WorldPropType.BUSH
                            else -> WorldPropType.TREE_CERRADO
                        }
                        RegionalArchetype.AMAZONIA_FLORESTA -> when {
                            sub < 0.38 -> WorldPropType.TREE_PALM_AMAZON
                            sub < 0.54 -> WorldPropType.BANANA_CLUMP
                            sub < 0.82 -> WorldPropType.TREE_NATIVE
                            else -> WorldPropType.BUSH
                        }
                        else -> when {
                            sub < 0.36 -> WorldPropType.PLANT_CLUSTER
                            sub < 0.64 -> WorldPropType.BUSH
                            sub < 0.76 -> WorldPropType.TREE_SHADE_MANGUEIRA
                            sub < 0.86 -> WorldPropType.TREE_IPE_ROXO
                            sub < 0.94 -> WorldPropType.TREE_FRUIT
                            else -> WorldPropType.STONE
                        }
                    }
                    props.add(WorldProp("nat_${gx}_$gz", type, px, pz, 320.0, "nativa"))
                }
            } else if (soil == SoilState.PASTAGEM && (cell.plotId == 2 || cell.plotId == 9)) {
                if (roll < 0.024) {
                    val sub = rng()
                    val type = when {
                        archetype == RegionalArchetype.CAATINGA_SERTAO && sub < 0.35 -> WorldPropType.CACTUS_MANDACARU
                        sub < 0.5 -> WorldPropType.PLANT_CLUSTER
                        sub < 0.82 -> WorldPropType.BUSH
                        else -> WorldPropType.TREE_FRUIT
                    }
                    props.add(WorldProp("past_${gx}_$gz", type, px, pz, 280.0, "pasto"))
                }
            }
        }
    }

    // Immediate visible regional landmark flora + lush homestead landscaping around farmyard (outside crops/roads)
    val regionArch = resolveRegionalArchStyle(mapDef.id)
    when (archetype) {
        RegionalArchetype.PARANA_TERRA_ROXA, RegionalArchetype.SERRA_PINHAIS_SC -> {
            props.add(WorldProp("reg_arauc_1", WorldPropType.TREE_ARAUCARIA, -22.0, 26.0, 950.0, "araucaria"))
            props.add(WorldProp("reg_arauc_2", WorldPropType.TREE_ARAUCARIA, 34.0, 14.0, 950.0, "araucaria"))
            props.add(WorldProp("reg_ipe_sul", WorldPropType.TREE_IPE_AMARELO, -16.0, 34.0, 600.0, "ipe_amarelo"))
        }
        RegionalArchetype.CERRADO_CHAPADAO -> {
            props.add(WorldProp("reg_cerr_1", WorldPropType.TREE_CERRADO, -24.0, 28.0, 620.0, "cerrado"))
            props.add(WorldProp("reg_cerr_2", WorldPropType.TREE_IPE_AMARELO, 35.0, 14.0, 620.0, "ipe_amarelo"))
            props.add(WorldProp("reg_ipe_co", WorldPropType.TREE_IPE_ROXO, -14.0, 35.0, 620.0, "ipe_roxo"))
        }
        RegionalArchetype.SERRA_CAFE_MG -> {
            props.add(WorldProp("reg_cafe_1", WorldPropType.COFFEE_SHRUB, -22.0, 24.0, 120.0, "cafe"))
            props.add(WorldProp("reg_cafe_2", WorldPropType.COFFEE_SHRUB, -24.0, 20.0, 120.0, "cafe"))
            props.add(WorldProp("reg_mg_shade", WorldPropType.TREE_SHADE_MANGUEIRA, -14.0, 33.0, 750.0, "mangueira"))
            props.add(WorldProp("reg_mg_banana", WorldPropType.BANANA_CLUMP, 34.0, 18.0, 240.0, "bananeira"))
        }
        RegionalArchetype.CAATINGA_SERTAO -> {
            props.add(WorldProp("reg_cact_1", WorldPropType.CACTUS_MANDACARU, -22.0, 28.0, 180.0, "mandacaru"))
            props.add(WorldProp("reg_cact_2", WorldPropType.CACTUS_MANDACARU, 34.0, 14.0, 180.0, "mandacaru"))
            props.add(WorldProp("reg_ne_shade", WorldPropType.TREE_SHADE_MANGUEIRA, -13.5, 34.0, 650.0, "cajueiro"))
        }
        RegionalArchetype.AMAZONIA_FLORESTA -> {
            props.add(WorldProp("reg_palm_1", WorldPropType.TREE_PALM_AMAZON, -21.0, 26.0, 480.0, "acai"))
            props.add(WorldProp("reg_palm_2", WorldPropType.TREE_PALM_AMAZON, 32.0, 16.0, 480.0, "acai"))
            props.add(WorldProp("reg_am_banana", WorldPropType.BANANA_CLUMP, -14.0, 33.0, 220.0, "bananeira"))
        }
        else -> {
            props.add(WorldProp("reg_def_shade", WorldPropType.TREE_SHADE_MANGUEIRA, -14.0, 34.0, 720.0, "arvore_sombra"))
            props.add(WorldProp("reg_def_ipe", WorldPropType.TREE_IPE_AMARELO, 34.0, 16.0, 620.0, "ipe_amarelo"))
        }
    }

    // Ornamental azalea & rose flower beds flanking the homestead yard (away from tractors and roads)
    props.add(WorldProp("rose_yard_1", WorldPropType.FLOWER_BED_AZALEA, -9.5, 22.0, 35.0, "azaleias"))
    props.add(WorldProp("rose_yard_2", WorldPropType.FLOWER_BED_AZALEA, -9.5, 26.5, 35.0, "azaleias"))
    props.add(WorldProp("hedge_yard_1", WorldPropType.ORNAMENTAL_HEDGE, 4.5, 25.5, 25.0, "cerca_viva"))

    val buildings = buildRegionalFarmsteadBuildings(
        mapDef = mapDef,
        regionArch = regionArch,
        archetype = archetype,
        startingMode = startingMode,
        coordScale = coordScale,
    )

    val starterVehicles = when {
        isZeroMode -> emptyList()
        startingMode == "Do Zero" -> listOf(
            makeDefaultVehicle("veh_1", "agrale_4100", 13.0, 21.5, 180.0, 75.0, defaultCrop),
            makeDefaultVehicle("veh_2", "valmet_65id", 18.5, 21.5, 180.0, 70.0, defaultCrop),
        )
        else -> listOf(
            makeDefaultVehicle("veh_1", "jd_6300_4x4_cab", 13.0, 21.5, 180.0, 160.0, defaultCrop),
            makeDefaultVehicle("veh_2", "slc_6200_turbo_88", -22.0, -16.0, 0.0, 280.0, defaultCrop),
            makeDefaultVehicle("veh_3", "mb_1113", 6.0, 17.5, 180.0, 190.0),
            makeDefaultVehicle("veh_4", "valmet_685", 20.0, 22.0, 180.0, 95.0, defaultCrop),
        )
    }

    val starterImplements = if (isZeroMode) {
        emptyList()
    } else {
        listOf(
            makeDefaultImplement("imp_1", "arado_4l", 10.0, 12.0, 180.0),
            makeDefaultImplement("imp_2", "plantadeira_9l", 14.5, 12.0, 180.0, 1600.0, defaultCrop),
            makeDefaultImplement("imp_3", "carreta_12t", 2.0, 2.0, 90.0),
            makeDefaultImplement("imp_4", "pulverizador_24m", 19.0, 12.0, 180.0, 2600.0, "defensivo"),
            makeDefaultImplement("imp_5", "segadora_3m", 23.5, 12.0, 180.0),
        )
    }

    val filteredProps = props.filter { p ->
        for (b in buildings) {
            val profile = resolveBuildingVisualProfile(b, mapDef.id)
            if (profile.isWalkableSurface) continue
            val padRadius = if (b.type == BuildingType.CERCA || b.type == BuildingType.PORTEIRA) {
                2.8
            } else {
                max(profile.widthM, profile.depthM) * 0.65 + 2.2
            }
            if (hypot(p.x - b.x, p.z - b.z) < padRadius) return@filter false
        }
        for (v in starterVehicles) {
            if (hypot(p.x - v.x, p.z - v.z) < 4.5) return@filter false
        }
        for (imp in starterImplements) {
            if (hypot(p.x - imp.x, p.z - imp.z) < 4.2) return@filter false
        }
        for (est in ESTABLISHMENTS) {
            if (hypot(p.x - est.scaledWorldX(gridSide), p.z - est.scaledWorldZ(gridSide)) < 15.0) return@filter false
        }
        for (lm in com.example.domain.LANDMARKS) {
            if (hypot(p.x - lm.scaledWorldX(gridSide), p.z - lm.scaledWorldZ(gridSide)) < 13.0) return@filter false
        }
        true
    }

    val herds = AnimalSpecies.entries.map { sp ->
        val count = when {
            isZeroMode -> 0
            sp == AnimalSpecies.BOVINO_CORTE -> if (startingMode == "Do Zero") 2 else 8
            sp == AnimalSpecies.VACA_LEITEIRA -> if (startingMode == "Do Zero") 1 else 4
            sp == AnimalSpecies.AVE -> 20
            else -> 0
        }
        LivestockHerd(
            species = sp,
            count = count,
            avgAgeDays = 180,
            healthPct = 95.0,
            feedReserveKg = if (isZeroMode) 0.0 else 450.0,
            waterReserveL = if (isZeroMode) 0.0 else 900.0,
            pendingProductionUnits = 0.0,
        )
    }

    val industries = INDUSTRY_CHAINS.map { ind ->
        OwnedIndustry(
            recipeId = ind.id,
            purchased = false,
            isProcessing = false,
            rawStockKg = 0.0,
            progressHours = 0.0,
            outputStockKg = emptyMap(),
            employees = 2,
        )
    }

    val capCrop = defaultCrop.replaceFirstChar { it.uppercaseChar() }
    val contracts = if (isZeroMode) {
        listOf(
            FarmContract("ctr_zero_1", "Colheita de Rosas Familiares (Máquina Emprestada)", "Dona Aparecida • Sítio Ribeirão Claro", ContractType.COLHEITA, "rosas_familiares", 180.0, 0.0, 14500.0, false, false, true, "slc_2200_hist", null, 7),
            FarmContract("ctr_zero_2", "Colheita de $capCrop no Talhão 2 (SLC 6200 Emprestada)", "Sr. Antônio", ContractType.COLHEITA, defaultCrop, 600.0, 0.0, 21500.0, false, false, true, "slc_6200_turbo_88", null, 2),
            FarmContract("ctr_zero_3", "Plantio no Talhão 3 (JD 6300 + Plantadeira Emprestados)", "Família Kowalski", ContractType.PLANTIO, "milho", 320.0, 0.0, 16200.0, false, false, true, "jd_6300_4x2_rops", "plantadeira_9l", 3),
            FarmContract("ctr_zero_4", "Colheita em Chácara Primavera (Flores)", "Vila Nova Esperança", ContractType.COLHEITA, "rosas_familiares", 140.0, 0.0, 11800.0, false, false, true, "agrale_4100", null, 18),
        )
    } else {
        listOf(
            FarmContract("ctr_1", "Colheita de $capCrop", "Fazenda Santa Rita", ContractType.COLHEITA, defaultCrop, 1200.0, 0.0, 18500.0, false, false, true, "slc_6200_turbo_88", null, 2),
            FarmContract("ctr_2", "Plantio Direto de Milho", "Sítio Boa Vista", ContractType.PLANTIO, "milho", 800.0, 0.0, 12400.0, false, false, true, "jd_6300_4x2_rops", "plantadeira_9l", 3),
            FarmContract("ctr_3", "Fenação e Entrega de Fardos", "Haras Pantanal", ContractType.FENACAO, "feno_fardo", 5.0, 0.0, 9200.0, false, false),
            FarmContract("ctr_4", "Silagem de Milho Ensacada", "Laticínio Serra", ContractType.SILAGEM, "silagem_ensacada", 10.0, 0.0, 11800.0, false, false),
        )
    }

    val initialInventory = if (isZeroMode) {
        emptyMap()
    } else {
        mapOf(
            "semente_$defaultCrop" to 1200.0,
            "adubo" to 2500.0,
            "feno_fardo" to 1500.0,
            "silagem_granel" to 2000.0,
        )
    }

    val baseVehiclesPool = ALL_VEHICLES.filter { it.requiredModPackId == null }
    val auctionLots = List(4) { idx ->
        val pickIdx = floor(rng() * baseVehiclesPool.size).toInt().coerceIn(0, max(0, baseVehiclesPool.size - 1))
        val vDef = baseVehiclesPool.getOrElse(pickIdx) { baseVehiclesPool[0] }
        val hoursUsed = (450.0 + rng() * 3200.0).roundToInt()
        val cons = (54.0 + rng() * 34.0).roundToInt().toDouble()
        val discountRatio = 0.42 + (cons / 100.0) * 0.22
        val baseVal = (vDef.priceBrl * discountRatio).roundToInt().toDouble()
        UsedAuctionLot(
            uid = "auc_d1_$idx",
            defId = vDef.id,
            hoursUsed = hoursUsed,
            conservationPct = cons,
            baseValueBrl = baseVal,
            currentBidBrl = (baseVal * 0.88).roundToInt().toDouble(),
            highestBidder = "Ninguém",
            endsAtDay = 3 + idx,
            settled = false,
        )
    }

    return WorldState(
        slotIndex = slotIndex,
        schemaVersion = 4,
        farmName = farmName,
        mapId = mapId,
        mapGridSize = gridSide,
        startingMode = startingMode,
        day = 1,
        month = 10,
        hourOfDay = 8.5,
        weather = WeatherType.ENSOLARADO,
        moneyBrl = effectiveStartingMoney,
        bankLoanBrl = 0.0,
        player = PlayerCharacterState(
            x = -6.0,
            z = 14.0,
            headingDeg = 0.0,
            tool = ManualTool.MAOS,
            carryingPropUid = null,
            appearance = DEFAULT_PLAYER_APPEARANCE,
            speedMs = 0.0,
            gaitPhase = 0.0,
            actionState = "idle",
        ),
        activeVehicleUid = null,
        plots = plots,
        cells = cells,
        vehicles = starterVehicles,
        implements = starterImplements,
        props = filteredProps,
        buildings = buildings,
        herds = herds,
        industries = industries,
        inventoryKg = initialInventory,
        marketMultiplier = emptyMap(),
        contracts = contracts,
        ledger = listOf(
            LedgerEntry(
                day = 1,
                description = "Capital inicial ($startingMode) em ${mapDef.stateName}",
                amountBrl = effectiveStartingMoney,
            )
        ),
        lastWeighbridgeTicket = null,
        tireTracks = emptyList(),
        statusAlert = if (isZeroMode) {
            "Modo Fazenda Zerada iniciado (R$ 0): abra Contratos & DRE e aceite um serviço com máquina emprestada!"
        } else null,
        employees = emptyList(),
        auctionLots = auctionLots,
        achievementsUnlocked = emptyList(),
        inputsStockKg = if (isZeroMode) {
            mapOf("calcario" to 0.0, "adubo_npk" to 0.0, "defensivo" to 0.0)
        } else {
            mapOf(
                "calcario" to 2000.0,
                "adubo_npk" to 2500.0,
                "defensivo" to 1000.0,
                "semente_$defaultCrop" to 1200.0,
                "semente_milho" to 800.0,
            )
        },
        landUnit = LandUnit.HECTARE,
        npcAgents = createInitialNpcAgents(mapDef.seed, coordScale),
        outsourcingCompanies = createDefaultOutsourcingCompanies(),
        outsourcingContracts = emptyList(),
        purchaseHistory = emptyList(),
        roadVehicles = listOf(
            RoadVehicle("road_truck_1", -120.0 * coordScale, -66.0 * coordScale, 90.0, 11.0, "#e11d48", "truck"),
            RoadVehicle("road_pickup_1", 60.0 * coordScale, -66.0 * coordScale, -90.0, 13.5, "#f8fafc", "pickup"),
        ),
        puddles = listOf(
            Puddle(12.0 * coordScale, -8.0 * coordScale, 4.5),
            Puddle(-18.0 * coordScale, 32.0 * coordScale, 3.8),
        ),
    )
}

private fun buildRegionalFarmsteadBuildings(
    mapDef: BrazilianMapDef,
    regionArch: RegionalArchStyle,
    archetype: RegionalArchetype,
    startingMode: String,
    coordScale: Double,
): List<PlacedBuilding> {
    val isSmallTier = startingMode in setOf("Do Zero", "Fazenda Zerada", "Agricultura Familiar")
    val isLargeTier = startingMode in setOf("Gerente", "Milionário")

    val houseModelId = when {
        isSmallTier -> when (regionArch) {
            RegionalArchStyle.SUL_PR_MADEIRA, RegionalArchStyle.SERRA_SC_COLONIAL, RegionalArchStyle.PAMPA_RS_ESTANCIA -> "casa_madeira_simples"
            RegionalArchStyle.NORTE_AMAZONIA -> "casa_amazonica_palafita"
            RegionalArchStyle.NORDESTE_SERTAO -> "casa_alpendre_nordeste"
            else -> "casa_madeira_simples"
        }
        isLargeTier -> when (regionArch) {
            RegionalArchStyle.CERRADO_MT_FAZENDA -> "casa_cerrado_moderna"
            RegionalArchStyle.NORTE_AMAZONIA -> "casa_amazonica_palafita"
            else -> "casa_sede_grande_garagem"
        }
        else -> when (regionArch) {
            RegionalArchStyle.SUL_PR_MADEIRA, RegionalArchStyle.PAMPA_RS_ESTANCIA -> "casa_madeira_varanda_sul"
            RegionalArchStyle.SERRA_SC_COLONIAL, RegionalArchStyle.INTERNACIONAL_EU -> "casa_enxaimel_sc"
            RegionalArchStyle.NORDESTE_SERTAO -> "casa_alpendre_nordeste"
            RegionalArchStyle.NORTE_AMAZONIA -> "casa_amazonica_palafita"
            RegionalArchStyle.CERRADO_MT_FAZENDA -> "casa_cerrado_moderna"
            else -> "casa_alvenaria_media"
        }
    }
    val houseDef = getStructureDef(houseModelId)

    val houseWidth = when {
        isSmallTier && houseModelId == "casa_alpendre_nordeste" -> 7.6
        isSmallTier -> houseDef?.widthM ?: 7.2
        isLargeTier -> houseDef?.widthM ?: 12.6
        else -> houseDef?.widthM ?: 9.2
    }
    val houseDepth = when {
        isSmallTier && houseModelId == "casa_alpendre_nordeste" -> 6.2
        isSmallTier -> houseDef?.depthM ?: 6.2
        isLargeTier -> houseDef?.depthM ?: 9.2
        else -> houseDef?.depthM ?: 7.4
    }

    val houseBuilding = PlacedBuilding(
        uid = "bld_casa",
        type = BuildingType.CASA,
        x = -2.5,
        z = 25.0,
        rotationDeg = 0.0,
        modelId = houseModelId,
        archStyle = regionArch.code,
        materialStyle = houseDef?.materialStyle ?: regionArch.defaultMaterial,
        wallColorHex = regionArch.defaultWallHex,
        roofColorHex = regionArch.defaultRoofHex,
        trimColorHex = regionArch.defaultTrimHex,
        hasVeranda = true,
        hasGarage = isLargeTier,
        customWidthM = houseWidth,
        customDepthM = houseDepth,
        customHeightM = houseDef?.heightM ?: 4.2,
        customLabel = houseDef?.name ?: "Sede (${regionArch.regionTitle})",
    )

    val shedModelId = when (regionArch) {
        RegionalArchStyle.SUL_PR_MADEIRA, RegionalArchStyle.PAMPA_RS_ESTANCIA, RegionalArchStyle.SERRA_SC_COLONIAL, RegionalArchStyle.NORTE_AMAZONIA -> "galpao_madeira_sul"
        else -> "galpao_maquinas_grande"
    }
    val shedDef = getStructureDef(shedModelId)

    val list = mutableListOf(
        houseBuilding,
        PlacedBuilding(
            uid = "bld_caminho_sede",
            type = BuildingType.CAMINHO_TERRA,
            x = -2.5,
            z = 18.0,
            rotationDeg = 0.0,
            modelId = "caminho_terra_quintal",
            archStyle = regionArch.code,
        ),
        PlacedBuilding(
            uid = "bld_porteira",
            type = BuildingType.PORTEIRA,
            x = -14.0,
            z = -14.0,
            rotationDeg = 90.0,
            modelId = "porteira_madeira_fazenda",
            archStyle = regionArch.code,
        ),
        // === Polo Comercial Oeste (Concessionária, Madeireira, Materiais & Ponto de Animais) ===
        PlacedBuilding(
            uid = "bld_concessionaria",
            type = BuildingType.CONCESSIONARIA,
            x = -155.0 * coordScale,
            z = -72.0 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 14.5,
            customDepthM = 10.0,
            customHeightM = 5.4,
            customLabel = "Concessionária Nacional Agro",
        ),
        PlacedBuilding(
            uid = "bld_conc_galpao",
            type = BuildingType.GALPAO,
            x = -170.0 * coordScale,
            z = -72.0 * coordScale,
            rotationDeg = 0.0,
            modelId = "galpao_maquinas_grande",
            archStyle = regionArch.code,
            customLabel = "Pátio Coberto de Máquinas",
        ),
        PlacedBuilding(
            uid = "bld_conc_patio",
            type = BuildingType.ESTACIONAMENTO,
            x = -155.0 * coordScale,
            z = -80.0 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 18.0,
            customDepthM = 8.0,
            customLabel = "Exposição Concessionária",
        ),
        PlacedBuilding(
            uid = "bld_vila_madeireira",
            type = BuildingType.MADEIREIRA,
            x = -132.0 * coordScale,
            z = -88.0 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 12.5,
            customDepthM = 9.0,
            customHeightM = 4.6,
            customLabel = "Madeireira Brasil",
        ),
        PlacedBuilding(
            uid = "bld_vila_materiais",
            type = BuildingType.LOJA_MATERIAIS,
            x = -172.0 * coordScale,
            z = -88.0 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 12.5,
            customDepthM = 9.0,
            customHeightM = 4.6,
            customLabel = "Materiais de Construção",
        ),
        PlacedBuilding(
            uid = "bld_vila_animais",
            type = BuildingType.PONTO_ANIMAIS,
            x = -152.0 * coordScale,
            z = -108.0 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 12.0,
            customDepthM = 9.5,
            customHeightM = 4.2,
            customLabel = "Feira de Animais",
        ),
        PlacedBuilding(
            uid = "bld_animais_curral",
            type = BuildingType.CURRAL,
            x = -165.0 * coordScale,
            z = -108.0 * coordScale,
            rotationDeg = 0.0,
            modelId = "curral_madeira_manejo",
            archStyle = regionArch.code,
            customLabel = "Curral de Embarque",
        ),
        // === Polo Comercial Leste / Vila Rural (Banco, Oficina, Loja Agro, Mercado, Armazém, Posto) ===
        PlacedBuilding(
            uid = "bld_banco_rural",
            type = BuildingType.BANCO,
            x = 112.0 * coordScale,
            z = -88.0 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 11.5,
            customDepthM = 8.5,
            customHeightM = 5.2,
            customLabel = "Banco Rural Brasil",
        ),
        PlacedBuilding(
            uid = "bld_banco_patio",
            type = BuildingType.PATIO,
            x = 112.0 * coordScale,
            z = -80.5 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 13.0,
            customDepthM = 6.5,
        ),
        PlacedBuilding(
            uid = "bld_vila_oficina",
            type = BuildingType.OFICINA,
            x = 112.0 * coordScale,
            z = -108.0 * coordScale,
            rotationDeg = 0.0,
            modelId = "galpao_oficina_mecanica",
            archStyle = regionArch.code,
            customWidthM = 12.0,
            customDepthM = 9.5,
            customHeightM = 5.0,
            customLabel = "Oficina Mecânica Central",
        ),
        PlacedBuilding(
            uid = "bld_oficina_patio",
            type = BuildingType.PATIO,
            x = 112.0 * coordScale,
            z = -100.5 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 13.0,
            customDepthM = 6.5,
        ),
        PlacedBuilding(
            uid = "bld_loja",
            type = BuildingType.LOJA_AGROPECUARIA,
            x = 155.0 * coordScale,
            z = -72.0 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 13.0,
            customDepthM = 9.0,
            customHeightM = 4.8,
            customLabel = "Loja Agropecuária & Insumos",
        ),
        PlacedBuilding(
            uid = "bld_loja_deposito",
            type = BuildingType.DEPOSITO,
            x = 169.0 * coordScale,
            z = -72.0 * coordScale,
            rotationDeg = 0.0,
            modelId = "deposito_insumos_feno",
            archStyle = regionArch.code,
            customLabel = "Depósito de Insumos",
        ),
        PlacedBuilding(
            uid = "bld_vila_mercado",
            type = BuildingType.MERCADO_RURAL,
            x = 152.0 * coordScale,
            z = -88.0 * coordScale,
            rotationDeg = 180.0,
            archStyle = regionArch.code,
            wallColorHex = regionArch.defaultWallHex,
            roofColorHex = regionArch.defaultRoofHex,
            customWidthM = 11.5,
            customDepthM = 8.5,
            customLabel = "Mercado & Empório Rural",
        ),
        PlacedBuilding(
            uid = "bld_vila_armazem",
            type = BuildingType.ARMAZEM,
            x = 172.0 * coordScale,
            z = -108.0 * coordScale,
            rotationDeg = 0.0,
            archStyle = regionArch.code,
            customWidthM = 14.0,
            customDepthM = 10.0,
            customHeightM = 5.4,
            customLabel = "Armazém Geral Regional",
        ),
        PlacedBuilding("bld_vila_estufa", BuildingType.ESTUFA_FLORES, 172.0 * coordScale, -88.0 * coordScale, 0.0, archStyle = regionArch.code),
        PlacedBuilding(
            uid = "bld_vila_posto",
            type = BuildingType.POSTO_COMBUSTIVEL,
            x = 192.0 * coordScale,
            z = -88.0 * coordScale,
            rotationDeg = 180.0,
            archStyle = regionArch.code,
            customWidthM = 13.5,
            customDepthM = 10.0,
            customHeightM = 4.8,
            customLabel = "Posto Diesel S10",
        ),
        PlacedBuilding("bld_vila_posto_patio", BuildingType.PATIO, 192.0 * coordScale, -88.0 * coordScale, 0.0, archStyle = regionArch.code, customWidthM = 16.0, customDepthM = 14.0),
        PlacedBuilding("bld_vila_posto_tanque", BuildingType.TANQUE_COMBUSTIVEL, 200.5 * coordScale, -88.0 * coordScale, 0.0, modelId = "tanque_diesel_fazenda", archStyle = regionArch.code),
        PlacedBuilding(
            "bld_vila_casa1", BuildingType.CASA_URBANA, 132.0 * coordScale, -88.0 * coordScale, 0.0,
            archStyle = regionArch.code, materialStyle = regionArch.defaultMaterial, wallColorHex = regionArch.defaultWallHex, roofColorHex = regionArch.defaultRoofHex, trimColorHex = regionArch.defaultTrimHex, hasVeranda = true,
        ),
        PlacedBuilding(
            "bld_vila_casa2", BuildingType.CASA_URBANA, 132.0 * coordScale, -108.0 * coordScale, 90.0,
            archStyle = regionArch.code, materialStyle = "alvenaria", wallColorHex = "#E5ECE3", roofColorHex = regionArch.defaultRoofHex, trimColorHex = "#FFFFFF", hasVeranda = true,
        ),
        PlacedBuilding(
            "bld_vila_casa3", BuildingType.CASA_URBANA, 152.0 * coordScale, -108.0 * coordScale, 0.0,
            archStyle = regionArch.code, materialStyle = "madeira", wallColorHex = "#9E6A42", roofColorHex = "#8E3924", trimColorHex = "#EFE4D2", hasVeranda = true,
        ),
        PlacedBuilding("bld_cerca_n1", BuildingType.CERCA, -14.0, -22.0, 90.0, archStyle = regionArch.code),
        PlacedBuilding("bld_cerca_n2", BuildingType.CERCA, -14.0, -28.0, 90.0, archStyle = regionArch.code),
        PlacedBuilding("bld_cerca_e1", BuildingType.CERCA, 14.0, -5.0, 0.0, archStyle = regionArch.code),
        PlacedBuilding("bld_cerca_e2", BuildingType.CERCA, 20.0, -5.0, 0.0, archStyle = regionArch.code),
        PlacedBuilding("bld_cerca_e3", BuildingType.CERCA, 26.0, -5.0, 0.0, archStyle = regionArch.code),
        PlacedBuilding("bld_cerca_p1", BuildingType.CERCA, -24.0, 6.0, 0.0, archStyle = regionArch.code),
        PlacedBuilding("bld_cerca_p2", BuildingType.CERCA, -30.0, 6.0, 0.0, archStyle = regionArch.code),
    )

    // Complexos físicos das 4 Estações de Recebimento de Grãos, Usina & Ceasa (pátio, balança, silos e armazém)
    for (est in com.example.domain.ESTABLISHMENTS) {
        val ex = est.worldX * coordScale
        val ez = est.worldZ * coordScale
        val mainType = when (est.id) {
            "coop_central" -> BuildingType.COOPERATIVA
            "usina_bio" -> BuildingType.FABRICA
            "ceasa_horti" -> BuildingType.ARMAZEM
            else -> BuildingType.ESTACAO_GRAOS
        }
        list.add(PlacedBuilding("bld_${est.id}_patio", BuildingType.PATIO, ex, ez, 0.0, archStyle = regionArch.code, customWidthM = 20.0, customDepthM = 16.0))
        list.add(
            PlacedBuilding(
                uid = "bld_${est.id}_main",
                type = mainType,
                x = ex,
                z = ez,
                rotationDeg = 0.0,
                archStyle = regionArch.code,
                customWidthM = 13.5,
                customDepthM = 9.5,
                customHeightM = 5.4,
                customLabel = est.name,
            )
        )
        list.add(PlacedBuilding("bld_${est.id}_silo1", BuildingType.SILO, ex - 8.5, ez - 8.0, 0.0, modelId = "silo_metalico_padrao", archStyle = regionArch.code))
        list.add(PlacedBuilding("bld_${est.id}_silo2", BuildingType.SILO, ex + 8.5, ez - 8.0, 0.0, modelId = "silo_metalico_padrao", archStyle = regionArch.code))
        list.add(PlacedBuilding("bld_${est.id}_galpao", BuildingType.GALPAO, ex + 12.0, ez + 1.0, 270.0, modelId = "galpao_maquinas_grande", archStyle = regionArch.code, customWidthM = 10.0, customDepthM = 8.0))
    }

    if (isSmallTier) {
        // Pequena propriedade: pequena garagem/galpão modesto, canteiro de flores e quintal rústico
        list.add(
            PlacedBuilding(
                uid = "bld_galpao",
                type = BuildingType.GARAGEM,
                x = 15.0,
                z = 29.0,
                rotationDeg = 0.0,
                modelId = "garagem_madeira_simples",
                archStyle = regionArch.code,
                customLabel = "Pequeno Abrigo Rural",
            )
        )
        list.add(
            PlacedBuilding(
                uid = "bld_canteiro_sede",
                type = BuildingType.CANTEIRO_FLORES,
                x = -9.5,
                z = 21.0,
                rotationDeg = 0.0,
                modelId = "canteiro_azaleias_flores",
                archStyle = regionArch.code,
            )
        )
        list.add(
            PlacedBuilding(
                uid = "bld_curral",
                type = BuildingType.CURRAL,
                x = 24.0,
                z = 4.0,
                rotationDeg = 90.0,
                modelId = "curral_madeira_manejo",
                archStyle = regionArch.code,
            )
        )
    } else {
        // Média & Grande propriedade: galpão de máquinas, oficina, silo, garagem, jardim e pátio
        list.add(
            PlacedBuilding(
                uid = "bld_galpao",
                type = BuildingType.GALPAO,
                x = 15.0,
                z = 30.0,
                rotationDeg = 0.0,
                modelId = shedModelId,
                archStyle = regionArch.code,
                materialStyle = shedDef?.materialStyle ?: regionArch.defaultMaterial,
            )
        )
        list.add(
            PlacedBuilding(
                uid = "bld_garagem_sede",
                type = BuildingType.GARAGEM,
                x = -16.5,
                z = 25.0,
                rotationDeg = 0.0,
                modelId = if (regionArch.defaultMaterial == "madeira") "garagem_madeira_simples" else "garagem_alvenaria_dupla",
                archStyle = regionArch.code,
            )
        )
        list.add(
            PlacedBuilding(
                uid = "bld_oficina",
                type = BuildingType.OFICINA,
                x = 30.0,
                z = 30.0,
                rotationDeg = 0.0,
                modelId = "galpao_oficina_mecanica",
                archStyle = regionArch.code,
            )
        )
        list.add(
            PlacedBuilding(
                uid = "bld_silo",
                type = BuildingType.SILO,
                x = -18.0,
                z = 39.0,
                rotationDeg = 0.0,
                modelId = "silo_metalico_padrao",
                archStyle = regionArch.code,
            )
        )
        list.add(
            PlacedBuilding(
                uid = "bld_curral",
                type = BuildingType.CURRAL,
                x = 24.0,
                z = 4.0,
                rotationDeg = 90.0,
                modelId = "curral_madeira_manejo",
                archStyle = regionArch.code,
            )
        )
        list.add(PlacedBuilding("bld_patio", BuildingType.PATIO, 6.0, 20.0, 0.0, modelId = "patio_manobras_sede", archStyle = regionArch.code))
        list.add(PlacedBuilding("bld_jardim_sede", BuildingType.JARDIM, -11.0, 32.0, 0.0, modelId = "jardim_pergolado_casa", archStyle = regionArch.code))
    }

    if (isLargeTier) {
        list.add(
            PlacedBuilding(
                uid = "bld_celeiro_grande",
                type = BuildingType.CELEIRO,
                x = 34.0,
                z = 16.0,
                rotationDeg = 270.0,
                modelId = if (regionArch.defaultMaterial == "madeira") "celeiro_madeira_tradicional" else "celeiro_alvenaria_feno",
                archStyle = regionArch.code,
            )
        )
        list.add(PlacedBuilding("bld_silo_2", BuildingType.SILO, -26.0, 39.0, 0.0, modelId = "silo_metalico_padrao", archStyle = regionArch.code))
        list.add(PlacedBuilding("bld_tanque_diesel", BuildingType.TANQUE_COMBUSTIVEL, 36.0, 38.0, 0.0, modelId = "tanque_diesel_fazenda", archStyle = regionArch.code))
    }

    when (archetype) {
        RegionalArchetype.CERRADO_CHAPADAO, RegionalArchetype.PRADARIA_EUA -> if (!isSmallTier) {
            if (list.none { it.uid == "bld_silo_2" }) {
                list.add(PlacedBuilding("bld_silo_2", BuildingType.SILO, -26.0, 39.0, 0.0, modelId = "silo_metalico_padrao", archStyle = regionArch.code))
            }
            if (list.none { it.uid == "bld_tanque_diesel" }) {
                list.add(PlacedBuilding("bld_tanque_diesel", BuildingType.TANQUE_COMBUSTIVEL, 36.0, 38.0, 0.0, modelId = "tanque_diesel_fazenda", archStyle = regionArch.code))
            }
        }
        RegionalArchetype.PARANA_TERRA_ROXA -> {
            list.add(PlacedBuilding("bld_coop_pr", BuildingType.SILO, -155.0 * coordScale, -56.0 * coordScale, 0.0, modelId = "silo_metalico_padrao", archStyle = regionArch.code))
        }
        RegionalArchetype.SERRA_CAFE_MG -> {
            list.add(PlacedBuilding("bld_terreiro_cafe", BuildingType.PATIO, -14.0, 32.0, 0.0, modelId = "patio_manobras_sede", archStyle = regionArch.code))
        }
        RegionalArchetype.CAATINGA_SERTAO -> {
            list.add(PlacedBuilding("bld_caixa_irrigacao", BuildingType.TANQUE_COMBUSTIVEL, -25.0, 34.0, 0.0, modelId = "tanque_diesel_fazenda", archStyle = regionArch.code))
        }
        else -> Unit
    }

    return list
}

private typealias stringCompat = String
