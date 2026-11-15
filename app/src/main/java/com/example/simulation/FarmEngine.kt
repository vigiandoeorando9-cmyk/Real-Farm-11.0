package com.example.simulation

import com.example.domain.ANIMAL_META
import com.example.domain.BUILDING_META
import com.example.domain.BuildingType
import com.example.domain.ContractType
import com.example.domain.ESTABLISHMENTS
import com.example.domain.FarmContract
import com.example.domain.HitchType
import com.example.domain.INDUSTRY_CHAINS
import com.example.domain.ImplementCategory
import com.example.domain.ImplementDef
import com.example.domain.LAND_UNIT_META
import com.example.domain.LedgerEntry
import com.example.domain.ManualTool
import com.example.domain.OwnedImplement
import com.example.domain.OwnedVehicle
import com.example.domain.PlacedBuilding
import com.example.domain.PurchaseHistoryEntry
import com.example.domain.RoadVehicle
import com.example.domain.SEASON_META
import com.example.domain.SoilState
import com.example.domain.TerrainCell
import com.example.domain.VehicleBodyStyle
import com.example.domain.VehicleCategory
import com.example.domain.VehicleDef
import com.example.domain.WEATHER_META
import com.example.domain.WeatherType
import com.example.domain.WeighbridgeTicket
import com.example.domain.WorldProp
import com.example.domain.WorldPropType
import com.example.domain.WorldState
import com.example.domain.getCropById
import com.example.domain.getImplementDef
import com.example.domain.getSeasonFromMonth
import com.example.domain.getVehicleDef
import com.example.domain.resolveVehicleTuning
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private fun clamp(v: Double, minVal: Double, maxVal: Double): Double = v.coerceIn(minVal, maxVal)
private fun toRad(deg: Double): Double = (deg * PI) / 180.0

fun formatBrl(amount: Double): String {
    val safe = if (amount.isFinite()) amount else 0.0
    val sign = if (safe < 0) "-" else ""
    return "${sign}R$ ${String.format(Locale("pt", "BR"), "%,.2f", abs(safe))}"
}

private fun makeUid(prefix: String): String =
    "${prefix}_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"

data class FuelSourceInfo(
    val name: String,
    val pricePerL: Double,
    val isOwnTank: Boolean,
)

fun findNearbyFuelSource(state: WorldState, x: Double, z: Double): FuelSourceInfo? {
    for (b in state.buildings) {
        val d = hypot(b.x - x, b.z - z)
        when {
            b.type == BuildingType.POSTO_COMBUSTIVEL && d <= 18.0 ->
                return FuelSourceInfo("Posto de Diesel S10", 6.15, false)
            b.type == BuildingType.TANQUE_COMBUSTIVEL && d <= 14.0 ->
                return FuelSourceInfo("Tanque Aéreo Próprio", 4.85, true)
            (b.type == BuildingType.GALPAO || b.type == BuildingType.GARAGEM) && d <= 14.0 ->
                return FuelSourceInfo("Bomba da Sede/Galpão", 5.25, true)
        }
    }
    return null
}

private fun stepRoadTrafficVehicles(roadVehicles: List<RoadVehicle>?, worldHalf: Double, dt: Double): List<RoadVehicle>? {
    if (roadVehicles.isNullOrEmpty()) return roadVehicles
    val limit = worldHalf - 12.0
    return roadVehicles.map { rv ->
        val rad = toRad(rv.headingDeg)
        var nx = rv.x + sin(rad) * rv.speedMs * dt
        var nz = rv.z + cos(rad) * rv.speedMs * dt
        var nextHeading = rv.headingDeg
        if (nx > limit || nx < -limit) {
            nx = clamp(nx, -limit, limit)
            nextHeading = (rv.headingDeg + 180.0) % 360.0
        }
        if (nz > limit || nz < -limit) {
            nz = clamp(nz, -limit, limit)
            nextHeading = (rv.headingDeg + 180.0) % 360.0
        }
        rv.copy(x = nx, z = nz, headingDeg = nextHeading)
    }
}

private fun stepPlayerOnFoot(state: WorldState, input: ControlInput, dt: Double): WorldState {
    val gridSide = getGridSide(state)
    val cellSize = getCellSize(gridSide)
    val worldHalf = (gridSide * cellSize) / 2.0

    val mag = min(hypot(input.moveX, input.moveY), 1.0)
    if (mag < 0.05) {
        val p = state.player
        if ((p.speedMs ?: 0.0) <= 0.05 && p.actionState == if (p.carryingPropUid != null) "carry" else "idle") {
            return state
        }
        val stopped = p.copy(
            speedMs = 0.0,
            actionState = if (p.carryingPropUid != null) "carry" else "idle",
        )
        return state.copy(player = stopped)
    }

    val moveSpeed = if (input.running) 7.4 else 4.2
    val targetAngle = kotlin.math.atan2(input.moveX, input.moveY) * 180.0 / PI
    val diff = normalizeAngle180(targetAngle - state.player.headingDeg)
    val heading = (state.player.headingDeg + clamp(diff, -540.0 * dt, 540.0 * dt) + 360.0) % 360.0

    var nx = clamp(state.player.x + input.moveX * moveSpeed * dt, -worldHalf + 4.0, worldHalf - 4.0)
    var nz = clamp(state.player.z + input.moveY * moveSpeed * dt, -worldHalf + 4.0, worldHalf - 4.0)

    val cIdx = getCellIndex(nx, nz, gridSide)
    if (cIdx != null && state.cells.getOrNull(cIdx)?.soilState == SoilState.AGUA_RIO) {
        nx = state.player.x
        nz = state.player.z
    }

    val pRadius = 0.46
    val pointCol = resolvePointWorldCollision(
        state = state,
        candX = nx,
        candZ = nz,
        radius = pRadius,
        excludeVehicleUid = null,
    )
    nx = pointCol.resolvedX
    nz = pointCol.resolvedZ

    var nextProps = state.props
    val carryingId = state.player.carryingPropUid
    if (carryingId != null) {
        val rad = toRad(heading)
        val carryX = nx + sin(rad) * 1.15
        val carryZ = nz + cos(rad) * 1.15
        nextProps = state.props.map { pr ->
            if (pr.uid == carryingId) pr.copy(x = carryX, z = carryZ) else pr
        }
    }

    val actState = when {
        carryingId != null -> "carry"
        input.running -> "run"
        else -> "walk"
    }
    val nextGait = (state.player.gaitPhase ?: 0.0) + moveSpeed * dt * 2.4

    return state.copy(
        player = state.player.copy(
            x = nx,
            z = nz,
            headingDeg = heading,
            speedMs = moveSpeed * mag,
            gaitPhase = nextGait,
            actionState = actState,
        ),
        props = nextProps,
    )
}

private fun applySelfPropelledSprayerToField(
    state: WorldState,
    veh: OwnedVehicle,
    def: VehicleDef,
): WorldState {
    if (veh.engineRunning == false) return state
    if (veh.isSprayerActive != true || (veh.sprayerFillL ?: 0.0) <= 1.0) return state
    if ((veh.sprayerBoomDeploy ?: 1.0) < 0.60) return state
    val gridSide = getGridSide(state)
    val cellSize = getCellSize(gridSide)
    val boomWidthM = if (def.id.contains("4000") || def.id.contains("4030")) 34.0 else 28.0
    val halfBoom = boomWidthM * 0.52
    val reach = max(2, (boomWidthM / (cellSize * 2.0)).roundToInt())

    val rad = toRad(veh.headingDeg)
    val fwdX = sin(rad)
    val fwdZ = cos(rad)
    val rightX = cos(rad)
    val rightZ = -sin(rad)

    val centerIdx = getCellIndex(veh.x, veh.z, gridSide) ?: return state
    val centerCell = state.cells.getOrNull(centerIdx) ?: return state
    val ownedSet = state.plots.filter { it.owned || it.leased }.map { it.id }.toSet()

    var nextCells: MutableList<TerrainCell>? = null
    var touched = 0
    for (dz in -reach..reach) {
        for (dx in -reach..reach) {
            val gx = centerCell.gx + dx
            val gz = centerCell.gz + dz
            if (gx !in 0 until gridSide || gz !in 0 until gridSide) continue
            val cellWx = (gx - gridSide / 2.0) * cellSize + cellSize / 2.0
            val cellWz = (gz - gridSide / 2.0) * cellSize + cellSize / 2.0
            val relX = cellWx - veh.x
            val relZ = cellWz - veh.z
            val latDist = abs(relX * rightX + relZ * rightZ)
            val lonDist = abs(relX * fwdX + relZ * fwdZ)
            if (latDist > halfBoom + cellSize * 0.45 || lonDist > cellSize * 1.45) continue

            val idx = gz * gridSide + gx
            val c = (nextCells ?: state.cells).getOrNull(idx) ?: continue
            if (c.plotId <= 0 || !ownedSet.contains(c.plotId)) continue
            if (c.soilState == SoilState.CRESCENDO || c.soilState == SoilState.SEMEADO || c.soilState == SoilState.PRONTO_COLHEITA) {
                if (c.weedLevel > 0.01 || (c.pestLevel ?: 0.0) > 0.01) {
                    val list = nextCells ?: state.cells.toMutableList().also { nextCells = it }
                    list[idx] = c.copy(weedLevel = 0.0, pestLevel = 0.0)
                    touched++
                }
            }
        }
    }
    val mutatedCells = nextCells ?: return state
    if (touched == 0) return state

    val burnL = touched * 4.5
    val nextFill = max(0.0, (veh.sprayerFillL ?: 0.0) - burnL)
    val alert = if (nextFill <= 1.0) "Calda do pulverizador ${def.model} esgotada! Reabasteça na Loja de Insumos." else state.statusAlert

    return state.copy(
        cells = mutatedCells,
        vehicles = state.vehicles.map {
            if (it.uid == veh.uid) {
                it.copy(
                    sprayerFillL = nextFill,
                    isSprayerActive = nextFill > 1.0,
                )
            } else it
        },
        statusAlert = alert,
    )
}

private fun applyHarvesterToField(
    state: WorldState,
    veh: OwnedVehicle,
    def: VehicleDef,
): WorldState {
    if (veh.engineRunning == false) return state
    if (veh.headerLowered == false) return state
    val maxTank = max(def.tankCapacityL, 4500.0)
    if (veh.grainTankFillL >= maxTank * 0.995) {
        return state.copy(
            statusAlert = "Graneleiro da ${def.model} cheio! Descarregue em uma carreta ou Silo."
        )
    }

    val gridSide = getGridSide(state)
    val cellSize = getCellSize(gridSide)
    val rad = toRad(veh.headingDeg)
    val fwdX = sin(rad)
    val fwdZ = cos(rad)
    val rightX = cos(rad)
    val rightZ = -sin(rad)

    val attachedImp = veh.attachedImplementUid?.let { uid -> state.implements.find { it.uid == uid } }
    val attachedImpDef = attachedImp?.let { getImplementDef(it.defId) }
    val hasCornHeader = attachedImpDef?.category == ImplementCategory.PLATAFORMA_MILHO
    val baseHeaderWidthM = ((def.headerWidthFt ?: (def.dimensionsM[1] * 3.28)) * 0.3048).coerceIn(3.6, 16.5)
    val headerWidthM = if (hasCornHeader && attachedImpDef != null) {
        max(baseHeaderWidthM, attachedImpDef.workingWidthM).coerceIn(3.6, 16.5)
    } else {
        baseHeaderWidthM
    }
    val halfHeader = headerWidthM * 0.54
    val frontReach = max(2.8, def.dimensionsM[0] * 0.46)
    val probeX = veh.x + fwdX * frontReach
    val probeZ = veh.z + fwdZ * frontReach
    val centerIdx = getCellIndex(probeX, probeZ, gridSide) ?: getCellIndex(veh.x, veh.z, gridSide) ?: return state
    val centerCell = state.cells.getOrNull(centerIdx) ?: return state
    val reach = max(2, (headerWidthM / (cellSize * 1.55)).roundToInt())

    val ownedOrLeased = state.plots.filter { it.owned || it.leased }.map { it.id }.toSet()
    val contractPlotIds = state.contracts
        .filter { it.accepted && !it.completed && it.type == ContractType.COLHEITA && it.plotId != null }
        .mapNotNull { it.plotId }
        .toSet()

    var nextCells: MutableList<TerrainCell>? = null
    var harvestedKg = 0.0
    var harvestedCropId: String? = null
    val harvestedPlotIds = mutableSetOf<Int>()

    for (dz in -reach..reach) {
        for (dx in -reach..reach) {
            val gx = centerCell.gx + dx
            val gz = centerCell.gz + dz
            if (gx !in 0 until gridSide || gz !in 0 until gridSide) continue
            val cellWx = (gx - gridSide / 2.0) * cellSize + cellSize / 2.0
            val cellWz = (gz - gridSide / 2.0) * cellSize + cellSize / 2.0
            val relX = cellWx - probeX
            val relZ = cellWz - probeZ
            val latDist = abs(relX * rightX + relZ * rightZ)
            val lonDist = abs(relX * fwdX + relZ * fwdZ)
            if (latDist > halfHeader + cellSize * 0.42 || lonDist > cellSize * 1.15) continue

            val idx = gz * gridSide + gx
            val c = (nextCells ?: state.cells).getOrNull(idx) ?: continue
            val canCut = c.plotId == 0 || ownedOrLeased.contains(c.plotId) || contractPlotIds.contains(c.plotId)
            if (canCut && c.soilState == SoilState.PRONTO_COLHEITA && c.cropId != null) {
                val crop = getCropById(c.cropId)
                val fertilityBonus = 0.65 + c.fertility * 0.45
                val weedPenalty = 1.0 - c.weedLevel * 0.25
                val cornHeaderMult = when {
                    hasCornHeader && c.cropId.contains("milho", ignoreCase = true) -> 1.18
                    hasCornHeader -> 0.85
                    else -> 1.0
                }
                val kgFromCell = (crop.baseYieldTonsPerHa * 1000.0 * 0.015) * fertilityBonus * weedPenalty * cornHeaderMult
                harvestedKg += kgFromCell
                harvestedCropId = c.cropId
                harvestedPlotIds.add(c.plotId)
                val list = nextCells ?: state.cells.toMutableList().also { nextCells = it }
                list[idx] = c.copy(
                    soilState = SoilState.PALHADA,
                    lastCropId = c.cropId,
                    cropId = null,
                    growthProgress = 0.0,
                    weedLevel = 0.02,
                    pestLevel = 0.0,
                )
            }
        }
    }

    val mutatedCells = nextCells
    if (mutatedCells == null || harvestedKg <= 0.0 || harvestedCropId == null) return state

    val roomL = max(0.0, maxTank - veh.grainTankFillL)
    val addedL = min(roomL, harvestedKg * 1.28)
    val nextFill = veh.grainTankFillL + addedL

    val nextContracts = state.contracts.map { ctr ->
        if (ctr.accepted && !ctr.completed && ctr.type == ContractType.COLHEITA &&
            (ctr.cropOrProductId == harvestedCropId || (ctr.plotId != null && harvestedPlotIds.contains(ctr.plotId)))
        ) {
            val nextDone = min(ctr.targetUnits, ctr.completedUnits + harvestedKg)
            ctr.copy(completedUnits = nextDone)
        } else ctr
    }

    val fullAlert = if (nextFill >= maxTank * 0.98) {
        "Graneleiro da ${def.model} cheio! Encoste em uma carreta ou Silo e descarregue."
    } else state.statusAlert

    val nextState = state.copy(
        cells = mutatedCells,
        vehicles = state.vehicles.map {
            if (it.uid == veh.uid) it.copy(grainTankFillL = nextFill, grainCropId = harvestedCropId) else it
        },
        contracts = nextContracts,
        statusAlert = fullAlert,
    )
    return evaluateAchievements(nextState)
}

private fun applyImplementToField(
    state: WorldState,
    veh: OwnedVehicle,
    imp: OwnedImplement,
    impDef: ImplementDef,
): WorldState {
    if (veh.engineRunning == false) return state
    if (!imp.isLowered || imp.liftProgress > 0.42) return state
    if (isPtoDrivenImplement(impDef.category) && veh.isPtoEngaged != true && !imp.isActive) return state
    if (impDef.category == ImplementCategory.PULVERIZADOR && imp.boomDeployProgress < 0.60) return state

    val gridSide = getGridSide(state)
    val cellSize = getCellSize(gridSide)
    val workWidthM = max(impDef.workingWidthM, 1.5)
    val halfSwath = workWidthM * 0.52
    val reach = max(1, (workWidthM / (cellSize * 1.65)).roundToInt())
    val rad = toRad(imp.headingDeg)
    val fwdX = sin(rad)
    val fwdZ = cos(rad)
    val rightX = cos(rad)
    val rightZ = -sin(rad)

    val centerIdx = getCellIndex(imp.x, imp.z, gridSide) ?: return state
    val centerCell = state.cells.getOrNull(centerIdx) ?: return state

    val ownedOrLeased = state.plots.filter { it.owned || it.leased }.map { it.id }.toSet()
    var nextCells: MutableList<TerrainCell>? = null
    var cellsModified = false
    var nextProps = state.props
    var propsModified = false
    var impFillL = imp.fillL
    var impFillProduct = imp.fillProductId
    val selectedCrop = veh.selectedCropId
    var plantedCount = 0
    var outOfSeedsWarning = false
    var clearedPropsCount = 0
    var createdPlotCells = 0
    var createdBaleProp: WorldProp? = null

    fun editCell(idx: Int, newCell: TerrainCell) {
        val list = nextCells ?: state.cells.toMutableList().also { nextCells = it }
        list[idx] = newCell
        cellsModified = true
    }

    val isGroundTool = impDef.category == ImplementCategory.ARADO ||
        impDef.category == ImplementCategory.GRADE ||
        impDef.category == ImplementCategory.SUBSOLADOR ||
        impDef.category == ImplementCategory.ESCARIFICADOR

    for (dz in -reach..reach) {
        for (dx in -reach..reach) {
            val gx = centerCell.gx + dx
            val gz = centerCell.gz + dz
            if (gx !in 0 until gridSide || gz !in 0 until gridSide) continue

            val cellWx = (gx - gridSide / 2.0) * cellSize + cellSize / 2.0
            val cellWz = (gz - gridSide / 2.0) * cellSize + cellSize / 2.0
            val relX = cellWx - imp.x
            val relZ = cellWz - imp.z
            val latDist = abs(relX * rightX + relZ * rightZ)
            val lonDist = abs(relX * fwdX + relZ * fwdZ)
            if (latDist > halfSwath + cellSize * 0.42 || lonDist > cellSize * 1.15) continue

            val idx = gz * gridSide + gx
            val c = (nextCells ?: state.cells).getOrNull(idx) ?: continue

            if (c.soilState == SoilState.AREA_PRESERVACAO ||
                c.soilState == SoilState.AGUA_RIO ||
                c.soilState == SoilState.ESTRADA_ASFALTO ||
                c.soilState == SoilState.ESTRADA_TERRA ||
                c.soilState == SoilState.PONTE
            ) continue

            val isOwnedCell = c.plotId == 0 || ownedOrLeased.contains(c.plotId)

            when (impDef.category) {
                ImplementCategory.ARADO, ImplementCategory.GRADE -> {
                    if (isOwnedCell || veh.fieldCreationMode) {
                        val canConvert = veh.fieldCreationMode ||
                            (c.soilState != SoilState.PASTAGEM && c.soilState != SoilState.FLORESTA_NATIVA) ||
                            c.plotId > 0
                        if (canConvert && c.soilState != SoilState.ARADO && c.soilState != SoilState.PRONTO_COLHEITA) {
                            val nextPlotId = if (veh.fieldCreationMode && c.plotId == 0) {
                                createdPlotCells++
                                1
                            } else c.plotId
                            editCell(idx, c.copy(
                                plotId = nextPlotId,
                                soilState = SoilState.ARADO,
                                cropId = null,
                                growthProgress = 0.0,
                                weedLevel = 0.0,
                                compaction = max(0.06, c.compaction - 0.14),
                            ))
                        }
                    }
                }
                ImplementCategory.SUBSOLADOR, ImplementCategory.ESCARIFICADOR -> {
                    if (isOwnedCell && c.soilState != SoilState.SUBSOLADO && c.soilState != SoilState.PRONTO_COLHEITA) {
                        editCell(idx, c.copy(
                            soilState = SoilState.SUBSOLADO,
                            cropId = null,
                            growthProgress = 0.0,
                            compaction = 0.04,
                            weedLevel = 0.01,
                        ))
                    }
                }
                ImplementCategory.CULTIVADOR -> {
                    if (isOwnedCell || veh.fieldCreationMode) {
                        when (c.soilState) {
                            SoilState.SEMEADO, SoilState.CRESCENDO, SoilState.PRONTO_COLHEITA -> {
                                if (c.weedLevel > 0.01 || c.compaction > 0.08) {
                                    editCell(idx, c.copy(
                                        weedLevel = 0.0,
                                        compaction = max(0.05, c.compaction - 0.08),
                                        moisture = min(1.0, c.moisture + 0.02),
                                    ))
                                }
                            }
                            SoilState.PALHADA, SoilState.PASTAGEM, SoilState.SUBSOLADO -> {
                                val nextPlotId = if (veh.fieldCreationMode && c.plotId == 0) {
                                    createdPlotCells++
                                    1
                                } else c.plotId
                                editCell(idx, c.copy(
                                    plotId = nextPlotId,
                                    soilState = SoilState.ARADO,
                                    cropId = null,
                                    growthProgress = 0.0,
                                    weedLevel = 0.0,
                                    compaction = max(0.05, c.compaction - 0.12),
                                ))
                            }
                            else -> Unit
                        }
                    }
                }
                ImplementCategory.ROCADEIRA -> {
                    if (c.soilState == SoilState.PEDRAS_MATO || c.weedLevel > 0.04) {
                        editCell(idx, c.copy(soilState = SoilState.PASTAGEM, weedLevel = 0.0))
                    }
                    val beforeSize = nextProps.size
                    nextProps = nextProps.filterNot { pr ->
                        (pr.type == WorldPropType.BUSH || pr.type == WorldPropType.STONE || pr.type == WorldPropType.PLANT_CLUSTER) &&
                            hypot(pr.x - cellWx, pr.z - cellWz) < cellSize * 0.85
                    }
                    if (nextProps.size != beforeSize) {
                        propsModified = true
                        clearedPropsCount += (beforeSize - nextProps.size)
                    }
                }
                ImplementCategory.PLANTADEIRA, ImplementCategory.SEMEADORA -> {
                    val canPlantSoil = c.soilState == SoilState.ARADO ||
                        c.soilState == SoilState.SUBSOLADO ||
                        c.soilState == SoilState.PALHADA ||
                        c.soilState == SoilState.PASTAGEM
                    if (canPlantSoil && (c.plotId == 0 || ownedOrLeased.contains(c.plotId))) {
                        val isDirectPlanting = c.soilState == SoilState.PALHADA
                        val densityMult = ((veh.seedingDensityPct ?: 100.0) / 100.0).coerceIn(0.6, 1.5)
                        val seedDrain = 6.5 * densityMult
                        if (impFillL >= seedDrain) {
                            impFillL -= seedDrain
                            impFillProduct = selectedCrop
                            plantedCount++
                            editCell(idx, c.copy(
                                soilState = SoilState.SEMEADO,
                                cropId = selectedCrop,
                                growthProgress = 0.05,
                                fertility = clamp(c.fertility + (if (isDirectPlanting) 0.06 else 0.0), 0.35, 1.0),
                                weedLevel = 0.02,
                            ))
                        } else {
                            outOfSeedsWarning = true
                        }
                    }
                }
                ImplementCategory.PULVERIZADOR -> {
                    if ((c.soilState == SoilState.CRESCENDO || c.soilState == SoilState.SEMEADO || c.soilState == SoilState.PRONTO_COLHEITA) &&
                        (c.weedLevel > 0.01 || (c.pestLevel ?: 0.0) > 0.01) &&
                        impFillL >= 4.0
                    ) {
                        impFillL -= 4.0
                        editCell(idx, c.copy(weedLevel = 0.0, pestLevel = 0.0))
                    }
                }
                ImplementCategory.DISTRIBUIDOR -> {
                    if (impFillL >= 8.0 && (c.plotId == 0 || ownedOrLeased.contains(c.plotId))) {
                        impFillL -= 8.0
                        val prod = impFillProduct ?: "adubo"
                        if (prod == "calcario") {
                            editCell(idx, c.copy(phLevel = min(6.6, (c.phLevel ?: 5.6) + 0.35)))
                        } else {
                            editCell(idx, c.copy(
                                fertility = min(1.0, c.fertility + 0.14),
                                nitrogen = min(1.0, (c.nitrogen ?: 0.65) + 0.16),
                                phosphorus = min(1.0, (c.phosphorus ?: 0.62) + 0.14),
                                potassium = min(1.0, (c.potassium ?: 0.64) + 0.14),
                                fertilizedStages = min(3, (c.fertilizedStages ?: 0) + 1),
                            ))
                        }
                    }
                }
                ImplementCategory.SEGADORA -> {
                    if (c.soilState == SoilState.PASTAGEM && !c.isHayDried && c.pastureRestDays != 0) {
                        editCell(idx, c.copy(isHayDried = true, pastureRestDays = 0))
                    }
                }
                ImplementCategory.ENLEIRADOR -> {
                    if (c.soilState == SoilState.PASTAGEM && c.isHayDried && !c.isWindrowed) {
                        editCell(idx, c.copy(isWindrowed = true))
                    }
                }
                ImplementCategory.ENFARDADORA -> {
                    if (c.soilState == SoilState.PASTAGEM && c.isWindrowed) {
                        editCell(idx, c.copy(isHayDried = false, isWindrowed = false, isHayTedded = false))
                        impFillL += 85.0
                        if (impFillL >= 400.0 && createdBaleProp == null) {
                            impFillL -= 400.0
                            createdBaleProp = WorldProp(
                                uid = makeUid("bale"),
                                type = WorldPropType.BALE_ROUND,
                                x = imp.x - sin(toRad(imp.headingDeg)) * 2.2,
                                z = imp.z - cos(toRad(imp.headingDeg)) * 2.2,
                                amountKg = 380.0,
                                productId = "feno_fardo",
                            )
                        }
                    }
                }
                ImplementCategory.ENSILADEIRA -> {
                    if ((c.soilState == SoilState.PRONTO_COLHEITA || c.soilState == SoilState.CRESCENDO) && c.cropId != null) {
                        editCell(idx, c.copy(soilState = SoilState.PALHADA, cropId = null, growthProgress = 0.0))
                        impFillL = min(max(impDef.capacityL, 4000.0), impFillL + 190.0)
                        impFillProduct = "silagem_granel"
                    }
                }
                else -> Unit
            }
        }
    }

    if (createdBaleProp != null) {
        nextProps = nextProps + createdBaleProp
        propsModified = true
    }

    val nextContracts = if (plantedCount > 0 || createdBaleProp != null) {
        state.contracts.map { ctr ->
            when {
                ctr.accepted && !ctr.completed && ctr.type == ContractType.PLANTIO && plantedCount > 0 ->
                    ctr.copy(completedUnits = min(ctr.targetUnits, ctr.completedUnits + plantedCount * 24.0))
                ctr.accepted && !ctr.completed && ctr.type == ContractType.FENACAO && createdBaleProp != null ->
                    ctr.copy(completedUnits = min(ctr.targetUnits, ctr.completedUnits + 1.0))
                else -> ctr
            }
        }
    } else state.contracts

    val nextPlots = if (createdPlotCells > 0) {
        state.plots.map { p -> if (p.id == 1) p.copy(areaHa = ((p.areaHa + createdPlotCells * 0.0036) * 100.0).roundToInt() / 100.0) else p }
    } else state.plots

    val alert = when {
        outOfSeedsWarning && plantedCount == 0 -> "Sementes esgotadas na ${impDef.model}! Reabasteça o reservatório."
        clearedPropsCount > 0 -> "Limpeza de campo: $clearedPropsCount obstáculo(s) removido(s)!"
        isGroundTool && createdPlotCells > 0 -> "Modo Abrir Campo: +$createdPlotCells célula(s) incorporadas ao Talhão 1!"
        else -> state.statusAlert
    }

    if (!cellsModified && !propsModified && impFillL == imp.fillL && plantedCount == 0 && createdPlotCells == 0 && !outOfSeedsWarning) {
        return state
    }

    val nextState = state.copy(
        cells = nextCells ?: state.cells,
        plots = nextPlots,
        props = if (propsModified) nextProps else state.props,
        implements = state.implements.map {
            if (it.uid == imp.uid) it.copy(fillL = impFillL, fillProductId = impFillProduct) else it
        },
        contracts = nextContracts,
        statusAlert = alert,
    )
    return if (plantedCount > 0 || impDef.category == ImplementCategory.DISTRIBUIDOR) {
        evaluateAchievements(nextState)
    } else {
        nextState
    }
}

private fun stepDrivingVehicle(
    state: WorldState,
    activeUid: String,
    input: ControlInput,
    dt: Double,
    steerSpeedMultiplier: Double = 1.0,
): WorldState {
    val vehIdx = state.vehicles.indexOfFirst { it.uid == activeUid }
    if (vehIdx < 0) return state.copy(activeVehicleUid = null)

    val veh = state.vehicles[vehIdx]
    val def = getVehicleDef(veh.defId)
    val attachedImp = veh.attachedImplementUid?.let { iUid -> state.implements.find { it.uid == iUid } }
    val impDef = attachedImp?.let { getImplementDef(it.defId) }

    val physResult = stepVehiclePhysics(state, veh, attachedImp, input, dt, steerSpeedMultiplier)
    val decayedAugerVeh = if (physResult.vehicle.unloadingAugerProgress > 0.0 && abs(physResult.vehicle.speedMs) > 0.4) {
        physResult.vehicle.copy(unloadingAugerProgress = max(0.0, physResult.vehicle.unloadingAugerProgress - dt * 0.6))
    } else physResult.vehicle

    val nextVehicles = state.vehicles.toMutableList()
    nextVehicles[vehIdx] = decayedAugerVeh

    val nextImplements = if (physResult.implement != null) {
        state.implements.map { if (it.uid == physResult.implement.uid) physResult.implement else it }
    } else state.implements

    val nextTracks = if (physResult.newTireTrack != null) {
        (state.tireTracks + physResult.newTireTrack).takeLast(140)
    } else if (state.tireTracks.isEmpty()) {
        state.tireTracks
    } else {
        state.tireTracks.mapNotNull { t ->
            val nextAge = t.age + dt
            if (nextAge < 45.0) t.copy(age = nextAge) else null
        }
    }

    var nextState = state.copy(
        vehicles = nextVehicles,
        implements = nextImplements,
        tireTracks = nextTracks,
        player = state.player.copy(
            x = decayedAugerVeh.x,
            z = decayedAugerVeh.z,
            headingDeg = decayedAugerVeh.headingDeg,
            speedMs = 0.0,
            actionState = "idle",
        ),
        statusAlert = physResult.collisionAlert ?: state.statusAlert,
    )

    if (def.category == VehicleCategory.COLHEITADEIRA && abs(decayedAugerVeh.speedMs) > 0.25) {
        nextState = applyHarvesterToField(nextState, decayedAugerVeh, def)
    }
    if (def.category == VehicleCategory.PULVERIZADOR_AUTOPROPELIDO && decayedAugerVeh.isSprayerActive == true) {
        nextState = applySelfPropelledSprayerToField(nextState, decayedAugerVeh, def)
    }
    if (physResult.implement != null && impDef != null && abs(decayedAugerVeh.speedMs) > 0.2) {
        nextState = applyImplementToField(nextState, decayedAugerVeh, physResult.implement, impDef)
    }

    return nextState
}

fun stepSimulation(
    state: WorldState,
    input: ControlInput,
    dtSec: Double,
    timeScale: Double = 1.0,
    steerSpeedMultiplier: Double = 1.0,
): WorldState {
    val dt = dtSec.coerceIn(0.001, 0.08)
    val gridSide = getGridSide(state)
    val worldHalf = (gridSide * getCellSize(gridSide)) / 2.0

    var nextState = if (state.activeVehicleUid != null) {
        stepDrivingVehicle(state, state.activeVehicleUid, input, dt, steerSpeedMultiplier)
    } else {
        stepPlayerOnFoot(state, input, dt)
    }

    val updatedNpc = stepNpcAgentsSimulation(nextState, dt)
    val updatedRoad = stepRoadTrafficVehicles(nextState.roadVehicles, worldHalf, dt)

    val hoursPassed = (dt * max(1.0, timeScale)) / 60.0
    var nextHour = nextState.hourOfDay + hoursPassed
    var shouldAdvanceDay = false
    if (nextHour >= 24.0) {
        nextHour -= 24.0
        shouldAdvanceDay = true
    }

    nextState = nextState.copy(
        hourOfDay = nextHour,
        npcAgents = updatedNpc,
        roadVehicles = updatedRoad,
    )
    nextState = tickIndustries(nextState, hoursPassed)
    nextState = stepOutsourcingContracts(nextState, dt, timeScale)

    if (shouldAdvanceDay) {
        nextState = advanceDay(nextState)
    }
    return nextState
}

fun toggleEnterExitVehicle(state: WorldState): WorldState {
    if (state.activeVehicleUid != null) {
        val veh = state.vehicles.find { it.uid == state.activeVehicleUid }
        val (exitX, exitZ) = if (veh != null) {
            findSafeExitSpot(state, veh)
        } else {
            state.player.x to state.player.z
        }
        return state.copy(
            activeVehicleUid = null,
            vehicles = state.vehicles.map {
                if (it.uid == veh?.uid) it.copy(cruiseControlActive = false) else it
            },
            player = state.player.copy(x = exitX, z = exitZ, speedMs = 0.0, actionState = "idle"),
            statusAlert = "Saiu do veículo.",
        )
    }

    var bestVeh: OwnedVehicle? = null
    var bestDist = 7.5
    for (v in state.vehicles) {
        val d = hypot(v.x - state.player.x, v.z - state.player.z)
        if (d < bestDist) {
            bestDist = d
            bestVeh = v
        }
    }
    if (bestVeh == null) {
        return state.copy(statusAlert = "Aproxime-se de uma máquina própria (até 7,5m) para embarcar.")
    }
    val def = getVehicleDef(bestVeh.defId)
    return state.copy(
        activeVehicleUid = bestVeh.uid,
        vehicles = state.vehicles.map { if (it.uid == bestVeh.uid) it.copy(engineRunning = true) else it },
        statusAlert = "Embarcou em ${def.brand} ${def.model}.",
    )
}

fun toggleHitchNearestImplement(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid
        ?: return state.copy(statusAlert = "Entre em um trator, caminhão ou colheitadeira para engatar implementos.")
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val vDef = getVehicleDef(veh.defId)

    if (veh.attachedImplementUid != null) {
        val imp = state.implements.find { it.uid == veh.attachedImplementUid }
        val impDef = imp?.let { getImplementDef(it.defId) }
        return state.copy(
            vehicles = state.vehicles.map {
                if (it.uid == activeUid) it.copy(attachedImplementUid = null, isPtoEngaged = false, ptoRpm = 0.0) else it
            },
            implements = state.implements.map {
                if (it.uid == veh.attachedImplementUid) it.copy(isLowered = false, isActive = false, compatibilityWarning = null) else it
            },
            statusAlert = "${impDef?.model ?: "Implemento"} desengatado com segurança.",
        )
    }

    val tuning = resolveVehicleTuning(veh, vDef)
    val candidates = state.implements
        .filter { imp -> state.vehicles.none { it.attachedImplementUid == imp.uid } }
        .map { imp -> imp to hypot(imp.x - veh.x, imp.z - veh.z) }
        .filter { it.second < 9.5 }
        .sortedBy { it.second }

    if (candidates.isEmpty()) {
        return state.copy(statusAlert = "Nenhum implemento livre num raio de 9m.")
    }

    // Prefer compatible implement first if multiple implements are nearby (e.g., combine near corn header + trailer)
    val compatibleCandidate = candidates.firstOrNull { (imp, _) ->
        val iDef = getImplementDef(imp.defId)
        checkMachineImplementCompatibility(vDef, iDef, tuning.effectiveHp).blockMessage == null
    }
    val bestImp = (compatibleCandidate ?: candidates.first()).first

    val impDef = getImplementDef(bestImp.defId)
    val compat = checkMachineImplementCompatibility(vDef, impDef, tuning.effectiveHp)
    if (compat.blockMessage != null) {
        return state.copy(statusAlert = compat.blockMessage)
    }

    val warnMsg = compat.warningMessage
    return state.copy(
        vehicles = state.vehicles.map {
            if (it.uid == activeUid) it.copy(attachedImplementUid = bestImp.uid, isPtoEngaged = false, ptoRpm = 0.0) else it
        },
        implements = state.implements.map {
            if (it.uid == bestImp.uid) it.copy(compatibilityWarning = warnMsg) else it
        },
        statusAlert = if (warnMsg != null) {
            "${impDef.model} engatado em ${vDef.model} — Aviso: $warnMsg"
        } else {
            "${impDef.model} engatado em ${vDef.model}!"
        },
    )
}

fun toggleImplementLowered(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val impUid = veh.attachedImplementUid
        ?: return state.copy(statusAlert = "Engate um implemento antes de acionar/abaixar.")
    val imp = state.implements.find { it.uid == impUid } ?: return state
    val impDef = getImplementDef(imp.defId)
    val nextLowered = !imp.isLowered
    val ptoNeeded = isPtoDrivenImplement(impDef.category)
    val nextPto = nextLowered && ptoNeeded && (veh.engineRunning != false)

    return state.copy(
        vehicles = state.vehicles.map {
            if (it.uid == activeUid) {
                it.copy(
                    isPtoEngaged = nextPto,
                    ptoRpm = if (nextPto) 540.0 else 0.0,
                )
            } else it
        },
        implements = state.implements.map {
            if (it.uid == impUid) {
                it.copy(
                    isLowered = nextLowered,
                    isActive = nextLowered,
                    liftProgress = if (nextLowered) 0.0 else 1.0,
                    boomDeployProgress = if (nextLowered && impDef.category == ImplementCategory.PULVERIZADOR) 1.0 else it.boomDeployProgress,
                )
            } else it
        },
        statusAlert = when {
            nextLowered && ptoNeeded -> "${impDef.model}: hidráulico abaixado e TDP acionada (540 RPM)."
            nextLowered -> "${impDef.model}: abaixado / em operação."
            else -> "${impDef.model}: erguido p/ transporte."
        },
    )
}

fun togglePtoInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val impUid = veh.attachedImplementUid
        ?: return state.copy(statusAlert = "Engate um implemento para acionar a TDP (Tomada de Força).")
    val imp = state.implements.find { it.uid == impUid } ?: return state
    val impDef = getImplementDef(imp.defId)
    if (!isPtoDrivenImplement(impDef.category)) {
        return state.copy(statusAlert = "${impDef.model} é de arrasto/hidráulico e não utiliza TDP.")
    }
    if (veh.engineRunning == false) {
        return state.copy(statusAlert = "Ligue o motor antes de acoplar a TDP.")
    }
    val nextPto = !(veh.isPtoEngaged == true)
    return state.copy(
        vehicles = state.vehicles.map {
            if (it.uid == activeUid) it.copy(isPtoEngaged = nextPto, ptoRpm = if (nextPto) 540.0 else 0.0) else it
        },
        implements = state.implements.map {
            if (it.uid == impUid) it.copy(isActive = nextPto && it.isLowered) else it
        },
        statusAlert = if (nextPto) "TDP acionada em 540 RPM para ${impDef.model}." else "TDP desligada.",
    )
}

fun toggleFieldCreationMode(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val next = !veh.fieldCreationMode
    return state.copy(
        vehicles = state.vehicles.map { if (it.uid == activeUid) it.copy(fieldCreationMode = next) else it },
        statusAlert = if (next) "Modo Abrir/Expandir Campos ATIVADO (passe com arado/grade)." else "Modo Abrir Campos desativado.",
    )
}

fun toggleVehicle4wd(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val next = !(veh.is4wdEngaged ?: true)
    return state.copy(
        vehicles = state.vehicles.map { if (it.uid == activeUid) it.copy(is4wdEngaged = next) else it },
        statusAlert = if (next) "Tração 4x4 Integral ATIVADA." else "Tração 4x2 (economia rodoviária).",
    )
}

fun toggleHarvesterHeader(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val next = !(veh.headerLowered ?: true)
    return state.copy(
        vehicles = state.vehicles.map { if (it.uid == activeUid) it.copy(headerLowered = next) else it },
        statusAlert = if (next) "Plataforma de corte ABAIXADA para colheita." else "Plataforma de corte ERGUIDA p/ transporte.",
    )
}

private fun defaultCruiseKmhFor(state: WorldState, veh: OwnedVehicle, def: VehicleDef): Double {
    val imp = veh.attachedImplementUid?.let { uid -> state.implements.find { it.uid == uid } }
    if (imp != null && imp.isLowered) {
        val impDef = getImplementDef(imp.defId)
        val recKmh = when (impDef.category) {
            ImplementCategory.PLANTADEIRA, ImplementCategory.SEMEADORA -> 9.0
            ImplementCategory.PULVERIZADOR -> 15.0
            ImplementCategory.DISTRIBUIDOR -> 14.0
            ImplementCategory.SUBSOLADOR, ImplementCategory.ARADO -> 8.0
            ImplementCategory.CULTIVADOR -> 12.0
            ImplementCategory.PLATAFORMA_MILHO -> 10.0
            else -> 11.0
        }
        return recKmh.coerceIn(3.0, def.maxSpeedKmh)
    }
    val curKmh = abs(veh.speedMs) * 3.6
    if (curKmh >= 4.0) {
        return curKmh.roundToInt().toDouble().coerceIn(3.0, def.maxSpeedKmh)
    }
    return when (def.category) {
        VehicleCategory.COLHEITADEIRA -> 10.0
        VehicleCategory.CAMINHAO -> 40.0
        VehicleCategory.PULVERIZADOR_AUTOPROPELIDO -> 16.0
        else -> 14.0
    }.coerceIn(3.0, def.maxSpeedKmh)
}

fun toggleCruiseControlInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val def = getVehicleDef(veh.defId)
    val nextActive = !(veh.cruiseControlActive ?: false)
    val targetKmh = (veh.cruiseTargetKmh ?: defaultCruiseKmhFor(state, veh, def)).coerceIn(3.0, def.maxSpeedKmh)
    return state.copy(
        vehicles = state.vehicles.map {
            if (it.uid == activeUid) {
                it.copy(
                    cruiseControlActive = nextActive,
                    cruiseTargetKmh = targetKmh,
                    handbrakeOn = if (nextActive) false else it.handbrakeOn,
                    engineRunning = if (nextActive) true else it.engineRunning,
                )
            } else it
        },
        statusAlert = if (nextActive) {
            "Piloto de cruzeiro ATIVADO: ${targetKmh.roundToInt()} km/h (${def.model})."
        } else {
            "Velocidade cruzeiro desativada."
        },
    )
}

fun adjustCruiseSpeedInWorld(state: WorldState, deltaKmh: Double): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val def = getVehicleDef(veh.defId)
    val base = veh.cruiseTargetKmh ?: defaultCruiseKmhFor(state, veh, def)
    val nextTarget = clamp(base + deltaKmh, 3.0, def.maxSpeedKmh).roundToInt().toDouble()
    return state.copy(
        vehicles = state.vehicles.map {
            if (it.uid == activeUid) {
                it.copy(
                    cruiseControlActive = true,
                    cruiseTargetKmh = nextTarget,
                    handbrakeOn = false,
                    engineRunning = true,
                )
            } else it
        },
        statusAlert = "Cruzeiro ajustado para ${nextTarget.roundToInt()} km/h.",
    )
}

fun toggleSprayerInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val def = getVehicleDef(veh.defId)
    if (def.category != VehicleCategory.PULVERIZADOR_AUTOPROPELIDO && def.bodyStyle != VehicleBodyStyle.SELF_PROPELLED_SPRAYER) {
        return toggleImplementLowered(state)
    }
    val next = !(veh.isSprayerActive == true)
    val fill = veh.sprayerFillL ?: 0.0
    if (next && fill <= 1.0) {
        return state.copy(statusAlert = "Tanque de calda vazio! Reabasteça primeiro na Loja de Insumos.")
    }
    return state.copy(
        vehicles = state.vehicles.map { if (it.uid == activeUid) it.copy(isSprayerActive = next) else it },
        statusAlert = if (next) "Pulverização autopropelida ATIVADA (${def.model})." else "Barras de pulverização desligadas.",
    )
}

fun refillSprayerInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val def = getVehicleDef(veh.defId)
    val maxCap = max(def.tankCapacityL, 2500.0)
    val curFill = veh.sprayerFillL ?: 0.0
    val needed = max(0.0, maxCap - curFill)
    if (needed <= 5.0) return state.copy(statusAlert = "Tanque de pulverização já está completo.")
    val cost = (needed * 1.45).roundToInt().toDouble()
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para recarregar calda (${formatBrl(cost)}).")
    }
    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        vehicles = state.vehicles.map { if (it.uid == activeUid) it.copy(sprayerFillL = maxCap) else it },
        ledger = state.ledger + LedgerEntry(state.day, "Recarga Pulverizador: ${def.model}", -cost),
        statusAlert = "Tanque reabastecido (${maxCap.roundToInt()} L) por ${formatBrl(cost)}.",
    )
}

fun engageTowRescueInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    if (veh.isBogged != true) {
        return state.copy(statusAlert = "A máquina não está atolada no momento.")
    }
    val cost = 250.0
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente (R$ 250) para desatolamento imediato.")
    }
    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        vehicles = state.vehicles.map {
            if (it.uid == activeUid) it.copy(isBogged = false, bogSeverity = 0.0, speedMs = 0.0) else it
        },
        ledger = state.ledger + LedgerEntry(state.day, "Resgate / Guincho em Campo", -cost),
        statusAlert = "Cabo de reboque puxou a máquina do atoleiro (-R$ 250)!",
    )
}

fun toggleVehicleEngineInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val next = !(veh.engineRunning ?: true)
    return state.copy(
        vehicles = state.vehicles.map {
            if (it.uid == activeUid) it.copy(engineRunning = next, rpm = if (next) 850.0 else 0.0) else it
        },
        statusAlert = if (next) "Motor diesel ligado." else "Motor desligado (economia de combustível).",
    )
}

fun toggleVehicleHandbrakeInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val next = !(veh.handbrakeOn ?: false)
    return state.copy(
        vehicles = state.vehicles.map {
            if (it.uid == activeUid) {
                it.copy(
                    handbrakeOn = next,
                    isBraking = next,
                    cruiseControlActive = if (next) false else it.cruiseControlActive,
                )
            } else it
        },
        statusAlert = if (next) "Freio de mão acionado." else "Freio de mão liberado.",
    )
}

fun refuelVehicleInWorld(state: WorldState, fullTank: Boolean = true): WorldState {
    val activeUid = state.activeVehicleUid
        ?: return state.copy(statusAlert = "Entre na máquina para abastecer.")
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val def = getVehicleDef(veh.defId)
    val source = findNearbyFuelSource(state, veh.x, veh.z)
    val pricePerL = source?.pricePerL ?: 6.25
    val neededL = max(0.0, def.fuelCapacityL - veh.fuelL)
    if (neededL < 1.0) return state.copy(statusAlert = "${def.model} já está com o tanque cheio.")
    val targetL = if (fullTank) neededL else min(neededL, 40.0)
    val cost = (targetL * pricePerL).roundToInt().toDouble()
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para abastecer (${formatBrl(cost)}).")
    }
    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        vehicles = state.vehicles.map { if (it.uid == activeUid) it.copy(fuelL = min(def.fuelCapacityL, veh.fuelL + targetL)) else it },
        ledger = state.ledger + LedgerEntry(state.day, "Diesel S10 (${source?.name ?: "Entrega"}) - ${targetL.roundToInt()}L", -cost),
        statusAlert = "Abastecido +${targetL.roundToInt()} L em ${def.model} (-${formatBrl(cost)}).",
    )
}

fun refillImplementReservoirInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val impUid = veh.attachedImplementUid
        ?: return state.copy(statusAlert = "Engate uma plantadeira, pulverizador ou distribuidor para recarregar.")
    val imp = state.implements.find { it.uid == impUid } ?: return state
    val impDef = getImplementDef(imp.defId)
    val maxCap = max(impDef.capacityL, 1200.0)
    val needed = max(0.0, maxCap - imp.fillL)
    if (needed < 10.0) return state.copy(statusAlert = "${impDef.model} já está abastecido.")

    val unitCost = when (impDef.category) {
        ImplementCategory.PULVERIZADOR -> 1.35
        ImplementCategory.DISTRIBUIDOR -> 1.15
        else -> 1.65
    }
    val cost = (needed * unitCost).roundToInt().toDouble()
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para recarga de insumos (${formatBrl(cost)}).")
    }

    val prodId = when (impDef.category) {
        ImplementCategory.PULVERIZADOR -> "defensivo"
        ImplementCategory.DISTRIBUIDOR -> imp.fillProductId ?: "adubo"
        else -> veh.selectedCropId
    }

    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        implements = state.implements.map { if (it.uid == impUid) it.copy(fillL = maxCap, fillProductId = prodId) else it },
        ledger = state.ledger + LedgerEntry(state.day, "Insumos ${impDef.model}", -cost),
        statusAlert = "${impDef.model} recarregado com sucesso (-${formatBrl(cost)})!",
    )
}

fun unloadGrainInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    if (veh.grainTankFillL <= 5.0 || veh.grainCropId == null) {
        return state.copy(statusAlert = "O graneleiro desta máquina está vazio.")
    }
    val cropId = veh.grainCropId
    val crop = getCropById(cropId)

    val nearTrailer = state.implements.find {
        val iDef = getImplementDef(it.defId)
        val sameOrEmpty = it.fillL <= 5.0 || it.fillProductId == null || it.fillProductId == cropId
        iDef.category == ImplementCategory.CARRETA && sameOrEmpty && hypot(it.x - veh.x, it.z - veh.z) <= 14.0
    }
    if (nearTrailer != null) {
        val iDef = getImplementDef(nearTrailer.defId)
        val cap = max(iDef.capacityL, 12000.0)
        val roomL = max(0.0, cap - nearTrailer.fillL)
        if (roomL <= 5.0) {
            return state.copy(statusAlert = "${iDef.model} já está lotada! Descarregue a carreta no Silo ou use outra.")
        }
        val transferredL = min(roomL, veh.grainTankFillL)
        val transferredKg = (transferredL * 0.78).roundToInt().toDouble()
        val remainingHarvesterL = max(0.0, veh.grainTankFillL - transferredL)
        return state.copy(
            vehicles = state.vehicles.map {
                if (it.uid == activeUid) {
                    it.copy(
                        grainTankFillL = remainingHarvesterL,
                        grainCropId = if (remainingHarvesterL > 5.0) cropId else null,
                        unloadingAugerProgress = 1.0,
                    )
                } else it
            },
            implements = state.implements.map {
                if (it.uid == nearTrailer.uid) {
                    it.copy(fillL = nearTrailer.fillL + transferredL, fillProductId = cropId)
                } else it
            },
            statusAlert = "Transbordado ${transferredKg.roundToInt()} kg de ${crop.name} para ${iDef.model}!",
        )
    }

    val kg = (veh.grainTankFillL * 0.78).roundToInt().toDouble()
    val nearSilo = state.buildings.find {
        (it.type == BuildingType.SILO || it.type == BuildingType.SECADOR || it.type == BuildingType.DEPOSITO) &&
            hypot(it.x - veh.x, it.z - veh.z) <= 18.0
    }
    val nextInv = state.inventoryKg.toMutableMap()
    nextInv[cropId] = (nextInv[cropId] ?: 0.0) + kg

    return state.copy(
        inventoryKg = nextInv,
        vehicles = state.vehicles.map {
            if (it.uid == activeUid) {
                it.copy(grainTankFillL = 0.0, grainCropId = null, unloadingAugerProgress = 1.0)
            } else it
        },
        statusAlert = if (nearSilo != null) {
            "Armazenado no Silo: +${kg.roundToInt()} kg de ${crop.name}."
        } else {
            "Carga enviada ao estoque da Sede: +${kg.roundToInt()} kg de ${crop.name}."
        },
    )
}

fun unloadAttachedTrailerInWorld(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val impUid = veh.attachedImplementUid
        ?: return state.copy(statusAlert = "Nenhuma carreta engatada para descarregar.")
    val imp = state.implements.find { it.uid == impUid } ?: return state
    val impDef = getImplementDef(imp.defId)
    if (impDef.category != ImplementCategory.CARRETA) {
        return state.copy(statusAlert = "${impDef.model} não é uma carreta graneleira.")
    }
    if (imp.fillL <= 5.0 || imp.fillProductId == null) {
        return state.copy(statusAlert = "${impDef.model} está vazia.")
    }
    val productId = imp.fillProductId
    val crop = getCropById(productId)
    val kg = (imp.fillL * 0.78).roundToInt().toDouble()
    val gridSide = getGridSide(state)
    val nearSilo = state.buildings.find {
        (it.type == BuildingType.SILO ||
            it.type == BuildingType.SECADOR ||
            it.type == BuildingType.DEPOSITO ||
            it.type == BuildingType.ARMAZEM ||
            it.type == BuildingType.COOPERATIVA ||
            it.type == BuildingType.ESTACAO_GRAOS) &&
            hypot(it.x - imp.x, it.z - imp.z) <= 22.0
    }
    val nearEst = ESTABLISHMENTS.find {
        hypot(it.scaledWorldX(gridSide) - imp.x, it.scaledWorldZ(gridSide) - imp.z) <= 24.0
    }

    if (nearEst != null) {
        val nextInv = state.inventoryKg.toMutableMap()
        nextInv[productId] = (nextInv[productId] ?: 0.0) + kg
        val unloadedState = state.copy(
            inventoryKg = nextInv,
            implements = state.implements.map {
                if (it.uid == impUid) it.copy(fillL = 0.0, fillProductId = null) else it
            },
        )
        return sellProductAtWeighbridge(unloadedState, nearEst.id, productId, kg)
    }

    val nextInv = state.inventoryKg.toMutableMap()
    nextInv[productId] = (nextInv[productId] ?: 0.0) + kg
    val where = nearSilo?.let { "no Silo / Armazém" } ?: "no estoque da Sede"

    return state.copy(
        inventoryKg = nextInv,
        implements = state.implements.map {
            if (it.uid == impUid) it.copy(fillL = 0.0, fillProductId = null) else it
        },
        statusAlert = "Carreta ${impDef.model} descarregada $where: +${kg.roundToInt()} kg de ${crop.name}!",
    )
}

fun setGpsPoint(state: WorldState, which: String): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val pt = com.example.domain.Vec2(veh.x, veh.z)
    val nextVeh = if (which.uppercase() == "A") veh.copy(gpsPointA = pt) else veh.copy(gpsPointB = pt)
    return state.copy(
        vehicles = state.vehicles.map { if (it.uid == activeUid) nextVeh else it },
        statusAlert = "Ponto GPS ${which.uppercase()} marcado (${veh.x.roundToInt()}m, ${veh.z.roundToInt()}m).",
    )
}

fun toggleGpsAutoSteer(state: WorldState): WorldState {
    val activeUid = state.activeVehicleUid ?: return state
    val veh = state.vehicles.find { it.uid == activeUid } ?: return state
    val next = !veh.gpsAutoSteer
    if (next && (veh.gpsPointA == null || veh.gpsPointB == null)) {
        return state.copy(statusAlert = "Marque os pontos GPS A e B no talhão antes de ativar o piloto automático.")
    }
    return state.copy(
        vehicles = state.vehicles.map { if (it.uid == activeUid) it.copy(gpsAutoSteer = next) else it },
        statusAlert = if (next) "Piloto Automático GPS RTK ATIVADO." else "Piloto Automático GPS desligado.",
    )
}

fun performManualAction(state: WorldState): WorldState {
    if (state.activeVehicleUid != null) {
        return toggleEnterExitVehicle(state)
    }

    val gridSide = getGridSide(state)
    val p = state.player
    if (p.carryingPropUid != null) {
        val nearSale = ESTABLISHMENTS.find {
            hypot(it.scaledWorldX(gridSide) - p.x, it.scaledWorldZ(gridSide) - p.z) < 16.0
        }
        val nearDeposit = state.buildings.find {
            (it.type == BuildingType.DEPOSITO ||
                it.type == BuildingType.SILO ||
                it.type == BuildingType.CURRAL ||
                it.type == BuildingType.ARMAZEM ||
                it.type == BuildingType.MADEIREIRA) &&
                hypot(it.x - p.x, it.z - p.z) < 14.0
        }
        val carriedProp = state.props.find { it.uid == p.carryingPropUid }
        if (carriedProp != null && nearSale != null) {
            val earn = (carriedProp.amountKg * 1.4).roundToInt().toDouble()
            return state.copy(
                moneyBrl = state.moneyBrl + earn,
                props = state.props.filterNot { it.uid == carriedProp.uid },
                player = p.copy(carryingPropUid = null, actionState = "idle"),
                ledger = state.ledger + LedgerEntry(state.day, "Entrega manual em ${nearSale.name}", earn),
                statusAlert = "Entregue em ${nearSale.name}: +${formatBrl(earn)}!",
            )
        }
        if (carriedProp != null && nearDeposit != null) {
            val nextInv = state.inventoryKg.toMutableMap()
            nextInv[carriedProp.productId] = (nextInv[carriedProp.productId] ?: 0.0) + carriedProp.amountKg
            return state.copy(
                inventoryKg = nextInv,
                props = state.props.filterNot { it.uid == carriedProp.uid },
                player = p.copy(carryingPropUid = null, actionState = "idle"),
                statusAlert = "Armazenado no depósito: +${carriedProp.amountKg.roundToInt()} kg.",
            )
        }
        return state.copy(
            player = p.copy(carryingPropUid = null, actionState = "idle"),
            statusAlert = "Objeto colocado no chão.",
        )
    }

    // Gate toggle
    val nearGate = state.buildings.find {
        it.type == BuildingType.PORTEIRA && hypot(it.x - p.x, it.z - p.z) < 5.2
    }
    if (nearGate != null) {
        val nextOpen = !nearGate.isGateOpen
        return state.copy(
            buildings = state.buildings.map { if (it.uid == nearGate.uid) it.copy(isGateOpen = nextOpen) else it },
            statusAlert = if (nextOpen) "Porteira aberta." else "Porteira fechada.",
        )
    }

    // NPC interaction
    val npcRes = interactWithNearestNpc(state)
    if (npcRes.npc != null) {
        return state.copy(
            npcAgents = npcRes.updatedNpcAgents,
            statusAlert = npcRes.message,
        )
    }

    // Pick up or chop/clean prop
    var nearestProp: WorldProp? = null
    var bestPropDist = 4.5
    for (pr in state.props) {
        val d = hypot(pr.x - p.x, pr.z - p.z)
        if (d < bestPropDist) {
            bestPropDist = d
            nearestProp = pr
        }
    }

    if (nearestProp != null) {
        when (p.tool) {
            ManualTool.MOTOSSERRA -> {
                if (nearestProp.type == WorldPropType.TREE ||
                    nearestProp.type == WorldPropType.TREE_PINE ||
                    nearestProp.type == WorldPropType.TREE_EUCALYPTUS ||
                    nearestProp.type == WorldPropType.TREE_FRUIT ||
                    nearestProp.type == WorldPropType.TREE_NATIVE ||
                    nearestProp.type == WorldPropType.TREE_ARAUCARIA ||
                    nearestProp.type == WorldPropType.TREE_CERRADO ||
                    nearestProp.type == WorldPropType.TREE_PALM_AMAZON ||
                    nearestProp.type == WorldPropType.TREE_IPE_AMARELO ||
                    nearestProp.type == WorldPropType.TREE_IPE_ROXO ||
                    nearestProp.type == WorldPropType.TREE_SHADE_MANGUEIRA
                ) {
                    val idx = getCellIndex(nearestProp.x, nearestProp.z, getGridSide(state))
                    if (idx != null && state.cells.getOrNull(idx)?.soilState == SoilState.AREA_PRESERVACAO) {
                        return state.copy(statusAlert = "APP Protegida! Corte proibido por legislação ambiental.")
                    }
                    val logProp = nearestProp.copy(type = WorldPropType.LOG, amountKg = 680.0, productId = "eucalipto")
                    return state.copy(
                        props = state.props.map { if (it.uid == nearestProp.uid) logProp else it },
                        statusAlert = "Árvore cortada em tora de madeira (680 kg)!",
                    )
                }
            }
            ManualTool.ENXADA -> {
                if (nearestProp.type == WorldPropType.BUSH ||
                    nearestProp.type == WorldPropType.STONE ||
                    nearestProp.type == WorldPropType.PLANT_CLUSTER
                ) {
                    return state.copy(
                        props = state.props.filterNot { it.uid == nearestProp.uid },
                        statusAlert = "Limpeza manual concluída com enxada.",
                    )
                }
            }
            ManualTool.CONSTRUTOR_CERCA -> {
                val fenceUid = makeUid("fence")
                val rad = toRad(p.headingDeg)
                val fx = p.x + sin(rad) * 2.5
                val fz = p.z + cos(rad) * 2.5
                if (state.moneyBrl < 1200.0) {
                    return state.copy(statusAlert = "Saldo insuficiente para construir cerca (R$ 1.200).")
                }
                return state.copy(
                    moneyBrl = state.moneyBrl - 1200.0,
                    buildings = state.buildings + PlacedBuilding(fenceUid, BuildingType.CERCA, fx, fz, p.headingDeg),
                    ledger = state.ledger + LedgerEntry(state.day, "Construção manual de cerca", -1200.0),
                    statusAlert = "Cerca construída (-R$ 1.200).",
                )
            }
            ManualTool.MAOS -> {
                if (nearestProp.type == WorldPropType.LOG ||
                    nearestProp.type == WorldPropType.BALE_ROUND ||
                    nearestProp.type == WorldPropType.BALE_RECT ||
                    nearestProp.type == WorldPropType.SILAGE_BAG ||
                    nearestProp.type == WorldPropType.ROSE_CLUSTER
                ) {
                    return state.copy(
                        player = p.copy(carryingPropUid = nearestProp.uid, actionState = "carry"),
                        statusAlert = "Carregando ${nearestProp.productId} (${nearestProp.amountKg.roundToInt()} kg).",
                    )
                }
            }
        }
    }

    if (p.tool == ManualTool.CONSTRUTOR_CERCA) {
        if (state.moneyBrl < 1200.0) {
            return state.copy(statusAlert = "Saldo insuficiente para construir cerca (R$ 1.200).")
        }
        val rad = toRad(p.headingDeg)
        val fx = p.x + sin(rad) * 2.5
        val fz = p.z + cos(rad) * 2.5
        return state.copy(
            moneyBrl = state.moneyBrl - 1200.0,
            buildings = state.buildings + PlacedBuilding(makeUid("fence"), BuildingType.CERCA, fx, fz, p.headingDeg),
            ledger = state.ledger + LedgerEntry(state.day, "Construção manual de cerca", -1200.0),
            statusAlert = "Trecho de cerca instalado (-R$ 1.200).",
        )
    }

    return state.copy(statusAlert = "Nenhuma interação disponível aqui. Troque a ferramenta ou aproxime-se de uma máquina/NPC.")
}

fun depositToBank(state: WorldState, amountBrl: Double): WorldState {
    if (amountBrl <= 0.0) return state
    val deposit = min(amountBrl, state.moneyBrl)
    if (deposit <= 0.0) {
        return state.copy(statusAlert = "Dinheiro em mãos insuficiente para depositar no Banco Rural.")
    }
    return state.copy(
        moneyBrl = state.moneyBrl - deposit,
        bankBalanceBrl = state.bankBalanceBrl + deposit,
        ledger = state.ledger + LedgerEntry(state.day, "Depósito no Banco Rural Brasil", -deposit),
        statusAlert = "Depositado ${formatBrl(deposit)} no Banco Rural (Saldo bancário: ${formatBrl(state.bankBalanceBrl + deposit)}).",
    )
}

fun withdrawFromBank(state: WorldState, amountBrl: Double): WorldState {
    if (amountBrl <= 0.0) return state
    val withdraw = min(amountBrl, state.bankBalanceBrl)
    if (withdraw <= 0.0) {
        return state.copy(statusAlert = "Saldo bancário insuficiente no Banco Rural para saque.")
    }
    return state.copy(
        moneyBrl = state.moneyBrl + withdraw,
        bankBalanceBrl = state.bankBalanceBrl - withdraw,
        ledger = state.ledger + LedgerEntry(state.day, "Saque do Banco Rural Brasil", withdraw),
        statusAlert = "Sacado ${formatBrl(withdraw)} do Banco Rural para o caixa da fazenda.",
    )
}

fun takeBankLoan(state: WorldState, amountBrl: Double): WorldState {
    if (amountBrl <= 0.0) return state
    val maxCredit = 1500000.0
    if (state.bankLoanBrl + amountBrl > maxCredit) {
        return state.copy(statusAlert = "Limite de crédito rural atingido (máx. ${formatBrl(maxCredit)}).")
    }
    return state.copy(
        moneyBrl = state.moneyBrl + amountBrl,
        bankLoanBrl = state.bankLoanBrl + amountBrl,
        ledger = state.ledger + LedgerEntry(state.day, "Crédito Rural tomado", amountBrl),
        statusAlert = "Crédito rural de ${formatBrl(amountBrl)} creditado em conta!",
    )
}

fun repayBankLoan(state: WorldState, amountBrl: Double): WorldState {
    val pay = min(amountBrl, min(state.bankLoanBrl, state.moneyBrl))
    if (pay <= 0.0) {
        return state.copy(statusAlert = "Saldo insuficiente ou sem dívida bancária para amortizar.")
    }
    val nextState = state.copy(
        moneyBrl = state.moneyBrl - pay,
        bankLoanBrl = max(0.0, state.bankLoanBrl - pay),
        ledger = state.ledger + LedgerEntry(state.day, "Amortização de Crédito Rural", -pay),
        statusAlert = "Pagamento de ${formatBrl(pay)} realizado junto ao banco!",
    )
    return evaluateAchievements(nextState)
}

fun sellCargoAtNearbyStation(state: WorldState): WorldState {
    val gridSide = getGridSide(state)
    val activeVeh = state.activeVehicleUid?.let { uid -> state.vehicles.find { it.uid == uid } }
    val px = activeVeh?.x ?: state.player.x
    val pz = activeVeh?.z ?: state.player.z
    val nearEst = ESTABLISHMENTS.minByOrNull { hypot(it.scaledWorldX(gridSide) - px, it.scaledWorldZ(gridSide) - pz) }
        ?.takeIf { hypot(it.scaledWorldX(gridSide) - px, it.scaledWorldZ(gridSide) - pz) <= 26.0 }
        ?: ESTABLISHMENTS.find { it.id == "ceasa_horti" }?.takeIf {
            state.buildings.any { b -> b.type == BuildingType.MERCADO_RURAL && hypot(b.x - px, b.z - pz) <= 20.0 }
        }
        ?: return state.copy(statusAlert = "Aproxime-se de uma Cooperativa, Cerealista, Usina ou Mercado Rural para vender carga.")

    val attachedImp = activeVeh?.attachedImplementUid?.let { uid -> state.implements.find { it.uid == uid } }
    val attachedImpDef = attachedImp?.let { getImplementDef(it.defId) }
    if (attachedImp != null && attachedImpDef?.category == ImplementCategory.CARRETA &&
        attachedImp.fillL > 5.0 && attachedImp.fillProductId != null
    ) {
        val prodId = attachedImp.fillProductId
        val kg = (attachedImp.fillL * 0.78).roundToInt().toDouble()
        val nextInv = state.inventoryKg.toMutableMap()
        nextInv[prodId] = (nextInv[prodId] ?: 0.0) + kg
        val unloadedState = state.copy(
            inventoryKg = nextInv,
            implements = state.implements.map {
                if (it.uid == attachedImp.uid) it.copy(fillL = 0.0, fillProductId = null) else it
            },
        )
        return sellProductAtWeighbridge(unloadedState, nearEst.id, prodId, kg)
    }

    if (activeVeh != null && activeVeh.grainTankFillL > 5.0 && activeVeh.grainCropId != null) {
        val cropId = activeVeh.grainCropId
        val kg = (activeVeh.grainTankFillL * 0.78).roundToInt().toDouble()
        val nextInv = state.inventoryKg.toMutableMap()
        nextInv[cropId] = (nextInv[cropId] ?: 0.0) + kg
        val unloadedState = state.copy(
            inventoryKg = nextInv,
            vehicles = state.vehicles.map {
                if (it.uid == activeVeh.uid) it.copy(grainTankFillL = 0.0, grainCropId = null, unloadingAugerProgress = 1.0) else it
            },
        )
        return sellProductAtWeighbridge(unloadedState, nearEst.id, cropId, kg)
    }

    val matchingEntry = state.inventoryKg.entries
        .filter { it.value > 0.5 && nearEst.acceptedProductIds.contains(it.key) }
        .maxByOrNull { it.value }
        ?: state.inventoryKg.entries.filter { it.value > 0.5 }.maxByOrNull { it.value }
        ?: return state.copy(statusAlert = "Balança ${nearEst.name} pronta! Carregue uma carreta ou tenha estoque para pesar e vender.")

    return sellProductAtWeighbridge(state, nearEst.id, matchingEntry.key, matchingEntry.value)
}

fun cycleWeather(state: WorldState): WorldState {
    val all = WeatherType.entries
    val next = all[(all.indexOf(state.weather) + 1) % all.size]
    val label = WEATHER_META[next]?.displayName ?: next.name
    return state.copy(
        weather = next,
        statusAlert = "Clima alterado para: $label.",
    )
}

fun tickIndustries(state: WorldState, hoursElapsed: Double): WorldState {
    if (hoursElapsed <= 0.0) return state
    val hasActive = state.industries.any { it.purchased && it.isProcessing }
    if (!hasActive) return state

    val nextInv = state.inventoryKg.toMutableMap()
    val nextIndustries = state.industries.map { ind ->
        if (!ind.purchased || !ind.isProcessing) return@map ind
        val recipe = INDUSTRY_CHAINS.find { it.id == ind.recipeId } ?: return@map ind
        val stageMult = when (ind.stage ?: 1) {
            2 -> 1.45
            3 -> 2.1
            else -> 1.0
        }
        val equipCount = ind.purchasedEquipmentIds?.size ?: 0
        val speedMult = stageMult * (1.0 + equipCount * 0.25) * (ind.efficiencyMultiplier ?: 1.0)
        val newProg = ind.progressHours + hoursElapsed * speedMult
        if (newProg < recipe.processingHours) {
            return@map ind.copy(progressHours = newProg)
        }
        // Complete a batch
        val yieldMult = when (ind.stage ?: 1) {
            2 -> 1.12
            3 -> 1.28
            else -> 1.0
        } + equipCount * 0.08
        val newOutput = ind.outputStockKg.toMutableMap()
        for (out in recipe.outputs) {
            val produced = (out.kg * yieldMult * 10.0).roundToInt() / 10.0
            newOutput[out.name] = (newOutput[out.name] ?: 0.0) + produced
            nextInv[out.name] = (nextInv[out.name] ?: 0.0) + produced
        }
        val remainingRaw = max(0.0, ind.rawStockKg - recipe.inputAmountKg)
        val keepGoing = remainingRaw >= recipe.inputAmountKg
        ind.copy(
            rawStockKg = remainingRaw,
            progressHours = if (keepGoing) 0.0 else 0.0,
            isProcessing = keepGoing,
            outputStockKg = newOutput,
        )
    }

    return state.copy(
        industries = nextIndustries,
        inventoryKg = nextInv,
    )
}

fun advanceDay(state: WorldState): WorldState {
    val nextDay = state.day + 1
    val nextMonth = if (nextDay % 15 == 1) ((state.month % 12) + 1) else state.month
    val season = getSeasonFromMonth(nextMonth)
    val seasonMeta = SEASON_META[season] ?: SEASON_META.getValue(com.example.domain.SeasonType.PRIMAVERA)

    val rainIntensity = WEATHER_META[state.weather]?.rainIntensity ?: 0.0
    val isRaining = rainIntensity > 0.25

    // Advance crops, moisture & soil chemistry
    val nextCells = state.cells.map { c ->
        val irrigActive = (c.irrigatedUntilDay ?: 0) >= nextDay
        val nextMoisture = clamp(
            c.moisture + (if (isRaining) 0.22 else -0.07) + (if (irrigActive) 0.20 else 0.0),
            0.18,
            1.0
        )
        when (c.soilState) {
            SoilState.SEMEADO, SoilState.CRESCENDO -> {
                val crop = c.cropId?.let { getCropById(it) }
                val baseStep = 1.0 / max(2, crop?.growthDays ?: 4)
                val envMult = seasonMeta.growthMultiplier * (0.72 + c.fertility * 0.35) * (0.75 + nextMoisture * 0.25)
                val nextProg = clamp(c.growthProgress + baseStep * envMult, 0.0, 1.0)
                val nextSoil = if (nextProg >= 0.99) SoilState.PRONTO_COLHEITA else SoilState.CRESCENDO
                val nextWeed = clamp(c.weedLevel + 0.03, 0.0, 0.65)
                val nextPest = clamp((c.pestLevel ?: 0.0) + 0.025, 0.0, 0.55)
                c.copy(
                    soilState = nextSoil,
                    growthProgress = nextProg,
                    moisture = nextMoisture,
                    weedLevel = nextWeed,
                    pestLevel = nextPest,
                    isIrrigated = irrigActive,
                )
            }
            SoilState.PASTAGEM -> c.copy(
                moisture = nextMoisture,
                pastureRestDays = (c.pastureRestDays ?: 0) + 1,
                isIrrigated = irrigActive,
            )
            else -> c.copy(moisture = nextMoisture, isIrrigated = irrigActive)
        }
    }

    // Next weather roll weighted by season
    val weathers = WeatherType.entries
    val nextWeather = weathers[(nextDay * 3 + nextMonth * 5 + Random.nextInt(weathers.size)) % weathers.size]

    // Bank interest, lease fees & employee wages
    val dailyInterest = (state.bankLoanBrl * 0.0025).roundToInt().toDouble()
    val leaseDailyTotal = state.plots.filter { it.leased }.sumOf { it.leaseDailyBrl }
    val empWagesTotal = (state.employees ?: emptyList()).sumOf { it.dailyWageBrl }

    // Livestock daily feed & production
    val nextInv = state.inventoryKg.toMutableMap()
    val nextHerds = state.herds.map { herd ->
        if (herd.count <= 0) return@map herd
        val meta = ANIMAL_META[herd.species] ?: return@map herd
        val feedNeed = herd.count * meta.dailyFeedKg
        val waterNeed = herd.count * meta.dailyWaterL
        val fed = herd.feedReserveKg >= feedNeed * 0.5 && herd.waterReserveL >= waterNeed * 0.5
        val prodGain = if (fed) herd.count * 2.5 else 0.0
        if (herd.species == com.example.domain.AnimalSpecies.VACA_LEITEIRA && fed) {
            nextInv["leite"] = (nextInv["leite"] ?: 0.0) + herd.count * 16.0
        }
        if (herd.species == com.example.domain.AnimalSpecies.AVE && fed) {
            nextInv["ovos"] = (nextInv["ovos"] ?: 0.0) + herd.count * 0.08
        }
        herd.copy(
            avgAgeDays = herd.avgAgeDays + 1,
            feedReserveKg = max(0.0, herd.feedReserveKg - feedNeed),
            waterReserveL = max(0.0, herd.waterReserveL - waterNeed),
            healthPct = clamp(herd.healthPct + (if (fed) 1.5 else -6.0), 10.0, 100.0),
            pendingProductionUnits = herd.pendingProductionUnits + prodGain,
        )
    }

    // Dynamic market price fluctuations
    val nextMarket = state.marketMultiplier.toMutableMap()
    for (crop in com.example.domain.ALL_CROPS) {
        val swing = 0.86 + ((Random.nextDouble() * 0.30))
        nextMarket[crop.id] = (swing * 100.0).roundToInt() / 100.0
    }

    val totalFixedCosts = dailyInterest + leaseDailyTotal + empWagesTotal
    val newLedger = state.ledger.toMutableList()
    if (dailyInterest > 0) newLedger.add(LedgerEntry(nextDay, "Juros diários do Crédito Rural", -dailyInterest))
    if (leaseDailyTotal > 0) newLedger.add(LedgerEntry(nextDay, "Arrendamento diário de terras", -leaseDailyTotal))
    if (empWagesTotal > 0) newLedger.add(LedgerEntry(nextDay, "Folha diária de funcionários", -empWagesTotal))

    val nextState = state.copy(
        day = nextDay,
        month = nextMonth,
        hourOfDay = 7.0,
        weather = nextWeather,
        moneyBrl = state.moneyBrl - totalFixedCosts,
        cells = nextCells,
        herds = nextHerds,
        inventoryKg = nextInv,
        marketMultiplier = nextMarket,
        ledger = newLedger.takeLast(120),
        statusAlert = "Dia $nextDay (${seasonMeta.label}) • Custos fixos: ${formatBrl(totalFixedCosts)}.",
    )
    return evaluateAchievements(nextState)
}

fun sellProductAtWeighbridge(
    state: WorldState,
    establishmentId: String,
    productId: String,
    kgAmount: Double,
): WorldState {
    val est = ESTABLISHMENTS.find { it.id == establishmentId }
        ?: return state.copy(statusAlert = "Estabelecimento comprador não encontrado.")
    val available = state.inventoryKg[productId] ?: 0.0
    val sellKg = min(available, max(0.0, kgAmount))
    if (sellKg <= 0.1) {
        return state.copy(statusAlert = "Sem estoque suficiente de '$productId' para pesar e vender.")
    }

    val cropOpt = com.example.domain.ALL_CROPS.find { it.id == productId }
    val marketMult = state.marketMultiplier[productId] ?: 1.0
    val basePerKg = when {
        cropOpt != null -> (cropOpt.basePricePerUnitBrl / max(cropOpt.kgPerUnit, 1.0)) * marketMult
        productId == "leite" -> 2.75
        productId == "ovos" -> 8.50
        productId == "feno_fardo" -> 0.95
        productId == "silagem_granel" -> 0.58
        productId == "silagem_ensacada" -> 0.85
        else -> {
            // Check industrial output price
            val indPrice = INDUSTRY_CHAINS.firstNotNullOfOrNull { it.outputPricesPerKg[productId] }
            indPrice ?: 3.20
        }
    }

    val pricePerKg = basePerKg * est.priceMultiplier
    val totalBrl = (sellKg * pricePerKg * 100.0).roundToInt() / 100.0
    val tareKg = 14800.0
    val ticket = WeighbridgeTicket(
        establishmentName = est.name,
        productName = cropOpt?.name ?: productId,
        grossKg = tareKg + sellKg,
        tareKg = tareKg,
        netKg = sellKg,
        pricePerKgBrl = (pricePerKg * 100.0).roundToInt() / 100.0,
        totalBrl = totalBrl,
        day = state.day,
    )

    val nextInv = state.inventoryKg.toMutableMap()
    val rem = available - sellKg
    if (rem <= 0.1) nextInv.remove(productId) else nextInv[productId] = rem

    val nextContracts = state.contracts.map { ctr ->
        if (ctr.accepted && !ctr.completed && ctr.type == ContractType.TRANSPORTE && ctr.cropOrProductId == productId) {
            ctr.copy(completedUnits = min(ctr.targetUnits, ctr.completedUnits + sellKg))
        } else ctr
    }

    val nextState = state.copy(
        moneyBrl = state.moneyBrl + totalBrl,
        inventoryKg = nextInv,
        lastWeighbridgeTicket = ticket,
        contracts = nextContracts,
        ledger = state.ledger + LedgerEntry(state.day, "Balança ${est.name}: ${cropOpt?.name ?: productId} (${sellKg.roundToInt()} kg)", totalBrl),
        statusAlert = "Ticket Balança ${est.name}: +${formatBrl(totalBrl)} (${sellKg.roundToInt()} kg).",
    )
    return evaluateAchievements(nextState)
}

fun buyVehicleInWorld(state: WorldState, defId: String): WorldState {
    val def = getVehicleDef(defId)
    if (state.moneyBrl < def.priceBrl) {
        return state.copy(statusAlert = "Saldo insuficiente para adquirir ${def.brand} ${def.model} (${formatBrl(def.priceBrl)}).")
    }
    val offsetX = (state.vehicles.size % 4) * 5.0
    val rowZ = ((state.vehicles.size / 4) % 3) * 5.2
    val newVeh = makeDefaultVehicle(
        uid = makeUid("veh"),
        defId = def.id,
        x = 10.0 + offsetX,
        z = 19.0 + rowZ,
        headingDeg = 180.0,
        fuelL = def.fuelCapacityL,
        conservationPct = 100.0,
    )
    val hist = PurchaseHistoryEntry(
        id = makeUid("hist"),
        day = state.day,
        kind = "COMPRA",
        itemName = "${def.brand} ${def.model}",
        amountBrl = -def.priceBrl,
        details = "Entregue na Garagem Central (${def.horsepower.roundToInt()} cv)",
    )
    val nextState = state.copy(
        moneyBrl = state.moneyBrl - def.priceBrl,
        vehicles = state.vehicles + newVeh,
        purchaseHistory = (listOf(hist) + (state.purchaseHistory ?: emptyList())).take(60),
        ledger = state.ledger + LedgerEntry(state.day, "Compra Máquina: ${def.brand} ${def.model}", -def.priceBrl),
        statusAlert = "${def.brand} ${def.model} entregue no pátio da Garagem Central!",
    )
    return evaluateAchievements(nextState)
}

fun buyImplementInWorld(state: WorldState, defId: String): WorldState {
    val def = getImplementDef(defId)
    if (state.moneyBrl < def.priceBrl) {
        return state.copy(statusAlert = "Saldo insuficiente para adquirir ${def.brand} ${def.model} (${formatBrl(def.priceBrl)}).")
    }
    val offset = (state.implements.size % 6) * 4.0
    val initFill = if (def.category == ImplementCategory.PULVERIZADOR || def.category == ImplementCategory.PLANTADEIRA) def.capacityL * 0.5 else 0.0
    val newImp = makeDefaultImplement(
        uid = makeUid("imp"),
        defId = def.id,
        x = 12.0 + offset,
        z = 12.0 + (state.implements.size / 6) * 4.5,
        headingDeg = 180.0,
        fillL = initFill,
        fillProductId = if (def.category == ImplementCategory.PULVERIZADOR) "defensivo" else null,
    )
    val hist = PurchaseHistoryEntry(
        id = makeUid("hist"),
        day = state.day,
        kind = "COMPRA",
        itemName = "${def.brand} ${def.model}",
        amountBrl = -def.priceBrl,
        details = "Largura ${def.workingWidthM}m • Entregue no pátio",
    )
    return state.copy(
        moneyBrl = state.moneyBrl - def.priceBrl,
        implements = state.implements + newImp,
        purchaseHistory = (listOf(hist) + (state.purchaseHistory ?: emptyList())).take(60),
        ledger = state.ledger + LedgerEntry(state.day, "Compra Implemento: ${def.brand} ${def.model}", -def.priceBrl),
        statusAlert = "${def.brand} ${def.model} entregue no pátio!",
    )
}

fun sellVehicleInWorld(state: WorldState, uid: String): WorldState {
    val veh = state.vehicles.find { it.uid == uid } ?: return state
    val def = getVehicleDef(veh.defId)
    val resale = ((def.priceBrl * 0.65) * (0.4 + 0.6 * (veh.conservationPct / 100.0))).roundToInt().toDouble()
    val hist = PurchaseHistoryEntry(
        id = makeUid("hist"),
        day = state.day,
        kind = "VENDA",
        itemName = "${def.brand} ${def.model}",
        amountBrl = resale,
        details = "Conservação ${veh.conservationPct.roundToInt()}%",
    )
    return state.copy(
        moneyBrl = state.moneyBrl + resale,
        activeVehicleUid = if (state.activeVehicleUid == uid) null else state.activeVehicleUid,
        vehicles = state.vehicles.filterNot { it.uid == uid },
        purchaseHistory = (listOf(hist) + (state.purchaseHistory ?: emptyList())).take(60),
        ledger = state.ledger + LedgerEntry(state.day, "Venda Máquina: ${def.brand} ${def.model}", resale),
        statusAlert = "${def.brand} ${def.model} vendido por ${formatBrl(resale)}!",
    )
}

fun placeBuildingInWorld(state: WorldState, type: BuildingType, x: Double, z: Double): WorldState {
    val meta = BUILDING_META[type] ?: return state
    if (state.moneyBrl < meta.costBrl) {
        return state.copy(statusAlert = "Saldo insuficiente para construir ${meta.displayName} (${formatBrl(meta.costBrl)}).")
    }
    val building = PlacedBuilding(
        uid = makeUid("bld"),
        type = type,
        x = x,
        z = z,
        rotationDeg = 0.0,
    )
    val hist = PurchaseHistoryEntry(
        id = makeUid("hist"),
        day = state.day,
        kind = "COMPRA",
        itemName = "Construção: ${meta.displayName}",
        amountBrl = -meta.costBrl,
        details = "Posição (${x.roundToInt()}m, ${z.roundToInt()}m)",
    )
    return state.copy(
        moneyBrl = state.moneyBrl - meta.costBrl,
        buildings = state.buildings + building,
        purchaseHistory = (listOf(hist) + (state.purchaseHistory ?: emptyList())).take(60),
        ledger = state.ledger + LedgerEntry(state.day, "Construção: ${meta.displayName}", -meta.costBrl),
        statusAlert = "${meta.displayName} construído por ${formatBrl(meta.costBrl)}!",
    )
}
