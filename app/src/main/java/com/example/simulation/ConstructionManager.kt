package com.example.simulation

import com.example.domain.BuildableStructureDef
import com.example.domain.BuildingType
import com.example.domain.BuildingVisualProfile
import com.example.domain.HOUSE_CUSTOMIZATION_PRESETS
import com.example.domain.LedgerEntry
import com.example.domain.PlacedBuilding
import com.example.domain.PropertyEvolutionTier
import com.example.domain.PurchaseHistoryEntry
import com.example.domain.SoilState
import com.example.domain.WorldPropType
import com.example.domain.WorldState
import com.example.domain.getImplementDef
import com.example.domain.getStructureDef
import com.example.domain.getVehicleDef
import com.example.domain.resolveBuildingVisualProfile
import com.example.domain.resolveRegionalArchStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class ConstructionValidationResult(
    val isValid: Boolean,
    val reason: String? = null,
    val plotId: Int? = null,
    val groundY: Double = 0.0,
)

data class ConstructionPlacementPreview(
    val structure: BuildableStructureDef,
    val x: Double,
    val z: Double,
    val rotationDeg: Double,
    val isValid: Boolean,
    val canAfford: Boolean,
    val invalidReason: String? = null,
    val targetPlotId: Int? = null,
    val groundY: Double = 0.0,
    val replaceHouseUid: String? = null,
)

data class PropertyEvolutionStatus(
    val currentTier: PropertyEvolutionTier,
    val nextTier: PropertyEvolutionTier?,
    val score: Int,
    val ownedPlotsCount: Int,
    val housesCount: Int,
    val shedsAndGaragesCount: Int,
    val silosAndBarnsCount: Int,
    val decorAndFencesCount: Int,
    val hasMasonryOrLargeHouse: Boolean,
    val checklist: List<Pair<String, Boolean>>,
)

private fun toRadCon(deg: Double): Double = deg * PI / 180.0

private fun rectsOverlapObbApprox(
    ax: Double,
    az: Double,
    ahw: Double,
    ahd: Double,
    aRotDeg: Double,
    bx: Double,
    bz: Double,
    bhw: Double,
    bhd: Double,
    bRotDeg: Double,
): Boolean {
    val radA = toRadCon(aRotDeg)
    val cosA = cos(radA)
    val sinA = sin(radA)
    val radB = toRadCon(bRotDeg)
    val cosB = cos(radB)
    val sinB = sin(radB)

    // Axes to test (SAT on 2D OBBs)
    val axes = arrayOf(
        cosA to -sinA,
        sinA to cosA,
        cosB to -sinB,
        sinB to cosB,
    )
    val dx = bx - ax
    val dz = bz - az

    val aUxX = cosA; val aUxZ = -sinA
    val aUzX = sinA; val aUzZ = cosA
    val bUxX = cosB; val bUxZ = -sinB
    val bUzX = sinB; val bUzZ = cosB

    for ((axisX, axisZ) in axes) {
        val dist = abs(dx * axisX + dz * axisZ)
        val projA = ahw * abs(aUxX * axisX + aUxZ * axisZ) + ahd * abs(aUzX * axisX + aUzZ * axisZ)
        val projB = bhw * abs(bUxX * axisX + bUxZ * axisZ) + bhd * abs(bUzX * axisX + bUzZ * axisZ)
        if (dist >= projA + projB) return false
    }
    return true
}

fun validateBuildingPlacement(
    state: WorldState,
    def: BuildableStructureDef,
    x: Double,
    z: Double,
    rotationDeg: Double,
    excludeBuildingUid: String? = null,
): ConstructionValidationResult {
    val gridSide = getGridSide(state)
    val cellSize = getCellSize(gridSide)
    val worldHalf = (gridSide * cellSize) / 2.0

    val hw = def.widthM * 0.5
    val hd = def.depthM * 0.5
    val rad = toRadCon(rotationDeg)
    val cosA = cos(rad)
    val sinA = sin(rad)
    val maxExtent = hypot(hw, hd)

    if (x - maxExtent < -worldHalf + 6.0 || x + maxExtent > worldHalf - 6.0 ||
        z - maxExtent < -worldHalf + 6.0 || z + maxExtent > worldHalf - 6.0
    ) {
        return ConstructionValidationResult(false, "Fora dos limites do mapa.")
    }

    val sampleLocalPoints = listOf(
        0.0 to 0.0,
        -hw * 0.85 to -hd * 0.85,
        hw * 0.85 to -hd * 0.85,
        -hw * 0.85 to hd * 0.85,
        hw * 0.85 to hd * 0.85,
        0.0 to -hd * 0.85,
        0.0 to hd * 0.85,
    )

    var minH = Double.POSITIVE_INFINITY
    var maxH = Double.NEGATIVE_INFINITY
    val isRoadOrGate = def.buildingType == BuildingType.CAMINHO_TERRA ||
        def.buildingType == BuildingType.ESTRADA_INTERNA ||
        def.buildingType == BuildingType.PORTEIRA
    val isFence = def.buildingType == BuildingType.CERCA

    val centerCell = getCellIndex(x, z, gridSide)?.let { state.cells.getOrNull(it) }
        ?: return ConstructionValidationResult(false, "Posição fora da grade do terreno.")
    val centerPlotId = centerCell.plotId
    val coordScale = (gridSide * cellSize).toDouble() / (128.0 * 6.0)
    val inFarmyardZone = abs(x) <= max(44.0, 78.0 * coordScale) &&
        z in (-max(22.0, 26.0 * coordScale))..(max(48.0, 72.0 * coordScale))

    for ((lx, lz) in sampleLocalPoints) {
        val wx = x + lx * cosA + lz * sinA
        val wz = z - lx * sinA + lz * cosA
        val cell = getCellIndex(wx, wz, gridSide)?.let { state.cells.getOrNull(it) }
            ?: return ConstructionValidationResult(false, "Área ultrapassa os limites da propriedade.")
        val h = getTerrainHeightAt(state, wx, wz)
        if (h < minH) minH = h
        if (h > maxH) maxH = h

        when (cell.soilState) {
            SoilState.AGUA_RIO -> return ConstructionValidationResult(false, "Não é permitido construir sobre água, rio ou açude.")
            SoilState.ESTRADA_ASFALTO, SoilState.PONTE -> return ConstructionValidationResult(false, "Área bloqueada pela rodovia ou ponte.")
            SoilState.ESTRADA_TERRA -> if (!isRoadOrGate && !isFence) {
                return ConstructionValidationResult(false, "Não é permitido obstruir estradas rurais.")
            }
            SoilState.AREA_PRESERVACAO -> return ConstructionValidationResult(false, "Área de Preservação Permanente (APP) protegida.")
            else -> Unit
        }
        if (cell.plotId > 0) {
            val plot = state.plots.firstOrNull { it.id == cell.plotId }
            val allowedByOwnership = plot != null && (plot.owned || (plot.leased && def.isWalkableSurface))
            if (!allowedByOwnership) {
                val ownerLabel = plot?.name ?: "Terreno #${cell.plotId}"
                return ConstructionValidationResult(false, "Terreno pertence a outro dono ($ownerLabel). Compre o lote primeiro.")
            }
        } else if (!inFarmyardZone) {
            // Outside main farmyard, allow lightweight fences/paths/decor or near owned plots
            val nearOwnedPlot = state.plots.any { p ->
                p.owned && p.centerX != null && p.centerZ != null &&
                    hypot(wx - p.centerX, wz - p.centerZ) <= max(45.0, 75.0 * coordScale)
            }
            if (!nearOwnedPlot && !def.isWalkableSurface && !isFence && !isRoadOrGate) {
                return ConstructionValidationResult(false, "Escolha um ponto na área da sede ou em um terreno próprio.")
            }
        }
    }

    val slopeSpan = maxH - minH
    val maxSlopeAllowed = if (def.isHouse) 4.4 else 3.6
    if (!def.isWalkableSurface && !isFence && slopeSpan > maxSlopeAllowed) {
        return ConstructionValidationResult(false, "Terreno muito inclinado (${"%.1f".format(slopeSpan)}m de desnível).")
    }

    // Check overlaps against existing buildings
    for (existing in state.buildings) {
        if (existing.uid == excludeBuildingUid) continue
        val exProfile = resolveBuildingVisualProfile(existing, state.mapId)
        // Walkable ground surfaces (pátio/caminhos) can underlie structures and vice versa cleanly
        if (def.isWalkableSurface && exProfile.isWalkableSurface) {
            if (hypot(x - existing.x, z - existing.z) < 1.2) {
                return ConstructionValidationResult(false, "Já existe um piso/caminho exatamente nesse ponto.")
            }
            continue
        }
        if (def.isWalkableSurface != exProfile.isWalkableSurface) {
            // Allow placing a shed/house on top of a maneuvering patio, or path next to door
            if (def.buildingType == BuildingType.PATIO || exProfile.buildingTypeAllowedUnderStructure(existing.type)) {
                continue
            }
        }
        val extraPad = if (def.isHouse || exProfile.isHouse) 0.45 else 0.25
        val overlap = rectsOverlapObbApprox(
            x, z, hw + extraPad, hd + extraPad, rotationDeg,
            existing.x, existing.z, exProfile.widthM * 0.5, exProfile.depthM * 0.5, existing.rotationDeg,
        )
        if (overlap) {
            return ConstructionValidationResult(false, "Colisão com ${exProfile.displayLabel}.")
        }
    }

    // Check vehicles & implements (non-walkable structures cannot be dropped onto parked tractors)
    if (!def.isWalkableSurface) {
        for (v in state.vehicles) {
            val vDef = getVehicleDef(v.defId)
            val overlap = rectsOverlapObbApprox(
                x, z, hw + 0.3, hd + 0.3, rotationDeg,
                v.x, v.z, vDef.dimensionsM[1] * 0.5, vDef.dimensionsM[0] * 0.5, v.headingDeg,
            )
            if (overlap) {
                return ConstructionValidationResult(false, "Veículo (${vDef.brand} ${vDef.model}) está no local.")
            }
        }
        for (imp in state.implements) {
            val iDef = getImplementDef(imp.defId)
            val overlap = rectsOverlapObbApprox(
                x, z, hw + 0.2, hd + 0.2, rotationDeg,
                imp.x, imp.z, max(1.1, iDef.workingWidthM * 0.4), 1.8, imp.headingDeg,
            )
            if (overlap) {
                return ConstructionValidationResult(false, "Implemento (${iDef.model}) está no local.")
            }
        }
        for (p in state.props) {
            val isBlockingProp = p.type != WorldPropType.PLANT_CLUSTER &&
                p.type != WorldPropType.FLOWER_BED_AZALEA &&
                p.type != WorldPropType.ORNAMENTAL_HEDGE &&
                p.type != WorldPropType.BUSH &&
                p.type != WorldPropType.ROSE_CLUSTER
            if (!isBlockingProp) continue
            if (hypot(x - p.x, z - p.z) < max(hw, hd) * 0.72 + 0.8) {
                return ConstructionValidationResult(false, "Obstáculo natural no terreno (árvore, rocha ou poste).")
            }
        }
    }

    val groundY = getTerrainHeightAt(state, x, z)
    return ConstructionValidationResult(
        isValid = true,
        reason = null,
        plotId = if (centerPlotId > 0) centerPlotId else null,
        groundY = groundY,
    )
}

private fun BuildingVisualProfile.buildingTypeAllowedUnderStructure(t: BuildingType): Boolean =
    t == BuildingType.PATIO || t == BuildingType.ESTACIONAMENTO

fun createInitialConstructionPreview(
    state: WorldState,
    def: BuildableStructureDef,
    replaceHouseUid: String? = null,
): ConstructionPlacementPreview {
    if (replaceHouseUid != null) {
        val oldHouse = state.buildings.firstOrNull { it.uid == replaceHouseUid }
        if (oldHouse != null) {
            val res = validateBuildingPlacement(state, def, oldHouse.x, oldHouse.z, oldHouse.rotationDeg, excludeBuildingUid = replaceHouseUid)
            return ConstructionPlacementPreview(
                structure = def,
                x = oldHouse.x,
                z = oldHouse.z,
                rotationDeg = oldHouse.rotationDeg,
                isValid = res.isValid,
                canAfford = state.moneyBrl >= def.priceBrl,
                invalidReason = res.reason,
                targetPlotId = res.plotId ?: oldHouse.plotId,
                groundY = res.groundY,
                replaceHouseUid = replaceHouseUid,
            )
        }
    }

    // Start preview ahead of player / camera target or nearest clear spot in farmyard
    val anchor = state.vehicles.firstOrNull { it.uid == state.activeVehicleUid }
    val baseX = anchor?.x ?: state.player.x
    val baseZ = anchor?.z ?: state.player.z
    val heading = anchor?.headingDeg ?: state.player.headingDeg
    val rad = toRadCon(heading)
    val aheadDist = max(9.5, max(def.widthM, def.depthM) * 0.85 + 4.0)
    var bestX = baseX + sin(rad) * aheadDist
    var bestZ = baseZ + cos(rad) * aheadDist
    var bestRes = validateBuildingPlacement(state, def, bestX, bestZ, 0.0, replaceHouseUid)

    if (!bestRes.isValid) {
        // Spiral scan nearby around anchor to suggest a valid default spot immediately if possible
        for (radius in listOf(8.0, 14.0, 20.0, 28.0)) {
            var found = false
            for (stepDeg in 0 until 360 step 45) {
                val r = toRadCon(stepDeg.toDouble())
                val cx = baseX + sin(r) * radius
                val cz = baseZ + cos(r) * radius
                val candidateRes = validateBuildingPlacement(state, def, cx, cz, 0.0, replaceHouseUid)
                if (candidateRes.isValid) {
                    bestX = cx
                    bestZ = cz
                    bestRes = candidateRes
                    found = true
                    break
                }
            }
            if (found) break
        }
    }

    return ConstructionPlacementPreview(
        structure = def,
        x = bestX,
        z = bestZ,
        rotationDeg = 0.0,
        isValid = bestRes.isValid,
        canAfford = state.moneyBrl >= def.priceBrl,
        invalidReason = bestRes.reason,
        targetPlotId = bestRes.plotId,
        groundY = bestRes.groundY,
        replaceHouseUid = replaceHouseUid,
    )
}

fun updateConstructionPreviewTransform(
    state: WorldState,
    preview: ConstructionPlacementPreview,
    newX: Double = preview.x,
    newZ: Double = preview.z,
    newRotDeg: Double = preview.rotationDeg,
): ConstructionPlacementPreview {
    val normRot = ((newRotDeg % 360.0) + 360.0) % 360.0
    val res = validateBuildingPlacement(
        state = state,
        def = preview.structure,
        x = newX,
        z = newZ,
        rotationDeg = normRot,
        excludeBuildingUid = preview.replaceHouseUid,
    )
    return preview.copy(
        x = newX,
        z = newZ,
        rotationDeg = normRot,
        isValid = res.isValid,
        canAfford = state.moneyBrl >= preview.structure.priceBrl,
        invalidReason = res.reason,
        targetPlotId = res.plotId,
        groundY = res.groundY,
    )
}

fun confirmConstructionPurchase(
    state: WorldState,
    preview: ConstructionPlacementPreview,
): Pair<WorldState, String> {
    val def = preview.structure
    val freshValidation = validateBuildingPlacement(
        state = state,
        def = def,
        x = preview.x,
        z = preview.z,
        rotationDeg = preview.rotationDeg,
        excludeBuildingUid = preview.replaceHouseUid,
    )
    if (!freshValidation.isValid) {
        return state to (freshValidation.reason ?: "Local inválido para construção.")
    }
    if (state.moneyBrl < def.priceBrl) {
        return state to "Saldo insuficiente: necessário R$ ${def.priceBrl.roundToInt()}, disponível R$ ${state.moneyBrl.roundToInt()}."
    }

    val archStyle = resolveRegionalArchStyle(state.mapId)
    val newUid = "bld_custom_${state.buildings.size + 1}_${(abs(preview.x * 17 + preview.z * 31)).roundToInt()}_${def.modelId.take(8)}"
    val newBuilding = PlacedBuilding(
        uid = newUid,
        type = def.buildingType,
        x = preview.x,
        z = preview.z,
        rotationDeg = preview.rotationDeg,
        isGateOpen = false,
        stage = 1,
        plotId = freshValidation.plotId,
        modelId = def.modelId,
        archStyle = archStyle.code,
        materialStyle = def.materialStyle,
        wallColorHex = def.defaultWallHex,
        roofColorHex = def.defaultRoofHex,
        trimColorHex = def.defaultTrimHex,
        hasVeranda = def.hasVeranda,
        hasGarage = def.hasGarage,
        customWidthM = def.widthM,
        customDepthM = def.depthM,
        customHeightM = def.heightM,
        customLabel = def.name,
    )

    val replaceUid = preview.replaceHouseUid
    val updatedBuildings = if (replaceUid != null) {
        state.buildings.filterNot { it.uid == replaceUid } + newBuilding
    } else {
        state.buildings + newBuilding
    }

    // Clear small ground tufts/bushes strictly inside the new footprint so interiors stay clean
    val halfSpan = hypot(def.widthM * 0.5, def.depthM * 0.5)
    val cleanedProps = if (!def.isWalkableSurface) {
        state.props.filterNot { p ->
            val softDecor = p.type == WorldPropType.BUSH || p.type == WorldPropType.PLANT_CLUSTER
            softDecor && hypot(p.x - preview.x, p.z - preview.z) < halfSpan * 0.85
        }
    } else {
        state.props
    }

    val actionDesc = if (replaceUid != null) {
        "Substituição/Construção: ${def.name}"
    } else {
        "Construção (${def.category.label}): ${def.name}"
    }

    val nextState = state.copy(
        moneyBrl = state.moneyBrl - def.priceBrl,
        buildings = updatedBuildings,
        props = cleanedProps,
        ledger = state.ledger + LedgerEntry(state.day, actionDesc, -def.priceBrl),
        purchaseHistory = state.purchaseHistory.orEmpty() + PurchaseHistoryEntry(
            id = "pur_bld_${state.day}_${updatedBuildings.size}",
            day = state.day,
            kind = "COMPRA",
            itemName = def.name,
            amountBrl = def.priceBrl,
            details = "${def.category.label} (${def.widthM}m × ${def.depthM}m)",
        ),
        statusAlert = "Construído: ${def.name}!",
    )
    return nextState to "Construção concluída: ${def.name}!"
}

fun customizeOwnedHouseBuilding(
    state: WorldState,
    buildingUid: String,
    presetId: String? = null,
    wallColorHex: String? = null,
    roofColorHex: String? = null,
    trimColorHex: String? = null,
    materialStyle: String? = null,
    addVeranda: Boolean? = null,
    addGarage: Boolean? = null,
): Pair<WorldState, String> {
    val target = state.buildings.firstOrNull { it.uid == buildingUid }
        ?: return state to "Construção não encontrada."
    val profile = resolveBuildingVisualProfile(target, state.mapId)
    if (!profile.isHouse) {
        return state to "Somente casas/sedes podem ser personalizadas neste menu."
    }

    val preset = presetId?.let { id -> HOUSE_CUSTOMIZATION_PRESETS.firstOrNull { it.id == id } }
    val nextWall = preset?.wallHex ?: wallColorHex ?: profile.wallColorHex
    val nextRoof = preset?.roofHex ?: roofColorHex ?: profile.roofColorHex
    val nextTrim = preset?.trimHex ?: trimColorHex ?: profile.trimColorHex
    val nextMat = preset?.materialStyle ?: materialStyle ?: profile.materialStyle
    val nextVeranda = addVeranda ?: profile.hasVeranda
    val nextGarage = addGarage ?: profile.hasGarage

    var costBrl = 0.0
    if (nextWall != profile.wallColorHex || nextRoof != profile.roofColorHex || nextTrim != profile.trimColorHex || nextMat != profile.materialStyle) {
        costBrl += 3500.0
    }
    if (nextVeranda && !profile.hasVeranda) costBrl += 12000.0
    if (nextGarage && !profile.hasGarage) costBrl += 18500.0

    if (costBrl <= 0.0 && addVeranda == profile.hasVeranda && addGarage == profile.hasGarage) {
        return state to "Nenhuma alteração nova selecionada."
    }
    if (state.moneyBrl < costBrl) {
        return state to "Saldo insuficiente para reforma (custo: R$ ${costBrl.roundToInt()})."
    }

    val updated = target.copy(
        wallColorHex = nextWall,
        roofColorHex = nextRoof,
        trimColorHex = nextTrim,
        materialStyle = nextMat,
        hasVeranda = nextVeranda,
        hasGarage = nextGarage,
    )
    val nextBuildings = state.buildings.map { if (it.uid == buildingUid) updated else it }
    val nextState = state.copy(
        moneyBrl = state.moneyBrl - costBrl,
        buildings = nextBuildings,
        ledger = if (costBrl > 0) state.ledger + LedgerEntry(state.day, "Reforma/Personalização de Casa", -costBrl) else state.ledger,
        statusAlert = "Casa personalizada com sucesso!",
    )
    return nextState to "Reforma concluída (${if (costBrl > 0) "R$ ${costBrl.roundToInt()}" else "sem custo adicional"})."
}

fun evaluatePropertyEvolution(state: WorldState): PropertyEvolutionStatus {
    val ownedPlotsCount = state.plots.count { it.owned }
    var housesCount = 0
    var masonryOrLargeHouses = 0
    var shedsAndGarages = 0
    var silosAndBarns = 0
    var decorAndFences = 0

    for (b in state.buildings) {
        val p = resolveBuildingVisualProfile(b, state.mapId)
        val isPlayerOrFarmBuilding = !b.uid.startsWith("bld_vila_") && b.uid != "bld_concessionaria" && b.uid != "bld_loja"
        if (!isPlayerOrFarmBuilding) continue

        when {
            p.isHouse -> {
                housesCount++
                if (p.materialStyle in setOf("alvenaria", "reboco_fazenda", "tijolinho", "enxaimel") || p.widthM >= 8.8) {
                    masonryOrLargeHouses++
                }
                if (p.hasGarage) shedsAndGarages++
            }
            b.type in setOf(BuildingType.GALPAO, BuildingType.GARAGEM, BuildingType.OFICINA) -> shedsAndGarages++
            b.type in setOf(BuildingType.SILO, BuildingType.SECADOR, BuildingType.CELEIRO, BuildingType.DEPOSITO, BuildingType.FABRICA, BuildingType.LATICINIO_PLANTA, BuildingType.MOINHO_GRAOS) -> silosAndBarns++
            b.type in setOf(BuildingType.JARDIM, BuildingType.CANTEIRO_FLORES, BuildingType.ARVORE_ORNAMENTAL, BuildingType.CERCA, BuildingType.PORTEIRA, BuildingType.PATIO, BuildingType.ESTACIONAMENTO, BuildingType.CAMINHO_TERRA) -> decorAndFences++
        }
    }

    val score = (ownedPlotsCount * 16) +
        (housesCount * 10) +
        (masonryOrLargeHouses * 15) +
        (shedsAndGarages * 14) +
        (silosAndBarns * 16) +
        min(decorAndFences * 3, 24)

    val currentTier = when {
        score >= PropertyEvolutionTier.FAZENDA_DESENVOLVIDA.targetScore && masonryOrLargeHouses >= 1 && silosAndBarns >= 2 && shedsAndGarages >= 2 -> PropertyEvolutionTier.FAZENDA_DESENVOLVIDA
        score >= PropertyEvolutionTier.FAZENDA_GRANDE.targetScore && shedsAndGarages >= 1 && ownedPlotsCount >= 2 -> PropertyEvolutionTier.FAZENDA_GRANDE
        score >= PropertyEvolutionTier.RURAL_INTERMEDIARIA.targetScore && (masonryOrLargeHouses >= 1 || shedsAndGarages >= 1) -> PropertyEvolutionTier.RURAL_INTERMEDIARIA
        else -> PropertyEvolutionTier.PEQUENA_INICIAL
    }

    val nextTier = PropertyEvolutionTier.entries.firstOrNull { it.level == currentTier.level + 1 }

    val checklist = listOf(
        "Casa de alvenaria, varanda ou porte médio/grande" to (masonryOrLargeHouses >= 1),
        "Galpão de máquinas ou garagem coberta" to (shedsAndGarages >= 1),
        "Silo graneleiro ou celeiro tradicional" to (silosAndBarns >= 1),
        "Jardim, cercas e pátio organizado (3+ itens)" to (decorAndFences >= 3),
        "Expansão de área (2+ talhões próprios)" to (ownedPlotsCount >= 2),
        "Complexo desenvolvido (2+ galpões/garagens + 2+ silos/celeiros)" to (shedsAndGarages >= 2 && silosAndBarns >= 2),
    )

    return PropertyEvolutionStatus(
        currentTier = currentTier,
        nextTier = nextTier,
        score = score,
        ownedPlotsCount = ownedPlotsCount,
        housesCount = housesCount,
        shedsAndGaragesCount = shedsAndGarages,
        silosAndBarnsCount = silosAndBarns,
        decorAndFencesCount = decorAndFences,
        hasMasonryOrLargeHouse = masonryOrLargeHouses >= 1,
        checklist = checklist,
    )
}
