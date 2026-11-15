package com.example.simulation

import com.example.domain.AgroServiceCompany
import com.example.domain.LedgerEntry
import com.example.domain.OUTSOURCING_SERVICE_META
import com.example.domain.OutsourcingContract
import com.example.domain.PurchaseHistoryEntry
import com.example.domain.SoilState
import com.example.domain.TerrainCell
import com.example.domain.WEATHER_META
import com.example.domain.WorldPropType
import com.example.domain.WorldState
import com.example.domain.getCropById
import com.example.domain.getVehicleDef
import com.example.domain.getVehiclePhysicsSpec
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private fun clamp(v: Double, minVal: Double, maxVal: Double): Double = v.coerceIn(minVal, maxVal)
private fun toDeg(rad: Double): Double = (rad * 180.0) / PI
private fun fmtBrl(v: Double): String = String.format(Locale("pt", "BR"), "%,.0f", v)

fun createDefaultOutsourcingCompanies(): List<AgroServiceCompany> = listOf(
    AgroServiceCompany(
        id = "emp_coop_parana",
        name = "CoopServiços Paraná Agro",
        badgeColorHex = "#16a34a",
        regionFocus = "Lavoura Completa & Safra",
        hqX = -145.0,
        hqZ = -64.0,
        specialties = listOf("PREPARO_SOLO", "PLANTIO", "ADUBACAO", "CALAGEM", "COLHEITA"),
        fleetVehicleDefIds = listOf("jd_6300_4x4_cab", "slc_7700_turbo", "valmet_685"),
        fleetImplementDefIds = listOf("plantadeira_15l", "arado_6l", "distribuidor_18m"),
        priceMultiplier = 1.0,
        reputation = 4.8,
        reliabilityPct = 96.0,
        availableMachinesCount = 6,
        totalJobsCompleted = 34,
        description = "Cooperativa tradicional com frota moderna e históricos revisados para plantio e colheita de precisão.",
    ),
    AgroServiceCompany(
        id = "emp_horizontina_colheitas",
        name = "Horizontina Colheitas Históricas",
        badgeColorHex = "#eab308",
        regionFocus = "Colheita & Transporte de Grãos",
        hqX = 162.0,
        hqZ = -68.0,
        specialties = listOf("COLHEITA", "TRANSPORTE_GRAOS", "CARREGAMENTO", "FENACAO"),
        fleetVehicleDefIds = listOf("slc_6200_turbo_88", "slc_7200_turbo", "slc_1175_jd", "mb_1113"),
        fleetImplementDefIds = listOf("carreta_18t", "enfardadora_redonda"),
        priceMultiplier = 0.94,
        reputation = 4.6,
        reliabilityPct = 92.0,
        availableMachinesCount = 5,
        totalJobsCompleted = 27,
        description = "Especialista em colheitadeiras SLC clássicas revisadas com ótimo custo por hectare para sítios e fazendas.",
    ),
    AgroServiceCompany(
        id = "emp_agrofrota_terraroxa",
        name = "AgroFrota Terra Roxa",
        badgeColorHex = "#ea580c",
        regionFocus = "Abertura, Terraceamento & Solo Pesado",
        hqX = -162.0,
        hqZ = 54.0,
        specialties = listOf("ARACAO", "GRADAGEM", "SUBSOLAGEM", "TERRACO_CURVA", "LIMPEZA_CAMPO", "DESMATAMENTO_LEGAL"),
        fleetVehicleDefIds = listOf("jd_6300_4x2_rops", "cbt_8440", "agrale_bx6150"),
        fleetImplementDefIds = listOf("subsolador_7h", "arado_4l", "grade_niveladora_36d", "rocadeira_3m"),
        priceMultiplier = 1.04,
        reputation = 4.7,
        reliabilityPct = 94.0,
        availableMachinesCount = 5,
        totalJobsCompleted = 19,
        description = "Focada em descompactação profunda, abertura autorizada de glebas e curvas de nível.",
    ),
    AgroServiceCompany(
        id = "emp_verdesul_pulv",
        name = "VerdeSul Pulverização & Nutrição",
        badgeColorHex = "#0284c7",
        regionFocus = "Defensivos, Floricultura & Adubação",
        hqX = 140.0,
        hqZ = -115.0,
        specialties = listOf("PULVERIZACAO", "ADUBACAO", "CALAGEM", "SEMEADURA", "PLANTIO"),
        fleetVehicleDefIds = listOf("agrale_4100", "valmet_685", "jd_6300_4x4_cab"),
        fleetImplementDefIds = listOf("pulverizador_24m", "distribuidor_18m", "plantadeira_9l"),
        priceMultiplier = 0.98,
        reputation = 4.9,
        reliabilityPct = 97.0,
        availableMachinesCount = 4,
        totalJobsCompleted = 41,
        description = "Atende desde canteiros delicados de rosas familiares até lavouras extensivas com controle de seção.",
    ),
    AgroServiceCompany(
        id = "emp_rodograos_log",
        name = "RodoGrãos & Boiadeiro Sul",
        badgeColorHex = "#7c3aed",
        regionFocus = "Fretes Rodoviários, Prancha & Gado",
        hqX = -54.0,
        hqZ = -64.0,
        specialties = listOf("TRANSPORTE_GRAOS", "TRANSPORTE_MAQUINAS", "TRANSPORTE_ANIMAIS", "CARREGAMENTO"),
        fleetVehicleDefIds = listOf("mb_1113", "agrale_bx6150"),
        fleetImplementDefIds = listOf("carreta_12t", "carreta_18t"),
        priceMultiplier = 0.92,
        reputation = 4.5,
        reliabilityPct = 91.0,
        availableMachinesCount = 4,
        totalJobsCompleted = 22,
        description = "Frota rodoviária pronta para escoamento de safras, prancha de tratores e transporte boiadeiro.",
    ),
    AgroServiceCompany(
        id = "emp_pinhal_silagem",
        name = "Pinhal Silagem & Florestal",
        badgeColorHex = "#0f766e",
        regionFocus = "Forragem, Silagem, Feno & Eucalipto",
        hqX = -120.0,
        hqZ = -180.0,
        specialties = listOf("SILAGEM", "FENACAO", "MANEJO_FLORESTAL", "LIMPEZA_CAMPO"),
        fleetVehicleDefIds = listOf("slc_2200_hist", "valmet_685", "cbt_8440"),
        fleetImplementDefIds = listOf("segadora_3m", "enleirador_4m", "enfardadora_redonda", "ensiladeira_2l"),
        priceMultiplier = 0.96,
        reputation = 4.6,
        reliabilityPct = 93.0,
        availableMachinesCount = 4,
        totalJobsCompleted = 16,
        description = "Equipe especializada em silagem ensacada, fenação para pecuária e manejo florestal legalizado.",
    ),
)

data class OutsourcingQuoteBreakdown(
    val machineCostBrl: Double,
    val operatorCostBrl: Double,
    val inputsCostBrl: Double,
    val mobilizationFeeBrl: Double,
    val totalBrl: Double,
    val estimatedHours: Double,
)

fun calculateOutsourcingQuote(
    company: AgroServiceCompany,
    serviceType: String,
    modality: String,
    areaHa: Double,
    distanceM: Double = 120.0,
): OutsourcingQuoteBreakdown {
    val meta = OUTSOURCING_SERVICE_META[serviceType] ?: OUTSOURCING_SERVICE_META.values.first()
    val safeHa = max(0.25, areaHa)
    val mult = company.priceMultiplier

    val fullServiceBase = meta.baseCostPerHaBrl * safeHa * mult
    var machineCostBrl = 0.0
    var operatorCostBrl = 0.0
    var inputsCostBrl = 0.0
    var mobilizationFeeBrl = (180.0 + distanceM * 1.15).roundToInt().toDouble()

    when (modality) {
        "A_MAQUINA_OPERADOR" -> {
            machineCostBrl = (fullServiceBase * 0.68).roundToInt().toDouble()
            operatorCostBrl = (fullServiceBase * 0.32).roundToInt().toDouble()
        }
        "B_COMPLETO_INSUMOS" -> {
            machineCostBrl = (fullServiceBase * 0.68).roundToInt().toDouble()
            operatorCostBrl = (fullServiceBase * 0.32).roundToInt().toDouble()
            inputsCostBrl = (meta.inputsCostPerHaBrl * safeHa * mult).roundToInt().toDouble()
        }
        else -> {
            machineCostBrl = 0.0
            operatorCostBrl = (fullServiceBase * 0.38).roundToInt().toDouble()
            inputsCostBrl = 0.0
            mobilizationFeeBrl = (90.0 + distanceM * 0.35).roundToInt().toDouble()
        }
    }

    val totalBrl = max(250.0, machineCostBrl + operatorCostBrl + inputsCostBrl + mobilizationFeeBrl)
    val estimatedHours = max(0.4, ((safeHa / meta.speedHaPerHour) * 10.0).roundToInt() / 10.0)

    return OutsourcingQuoteBreakdown(
        machineCostBrl = machineCostBrl,
        operatorCostBrl = operatorCostBrl,
        inputsCostBrl = inputsCostBrl,
        mobilizationFeeBrl = mobilizationFeeBrl,
        totalBrl = totalBrl,
        estimatedHours = estimatedHours,
    )
}

fun getPlotCentroid(state: WorldState, plotId: Int): Pair<Double, Double> {
    val plot = state.plots.find { it.id == plotId }
    if (plot?.centerX != null && plot.centerZ != null) {
        return plot.centerX to plot.centerZ
    }
    val gridSide = getGridSide(state)
    val cellSize = getCellSize(gridSide)
    val half = (gridSide * cellSize) / 2.0
    var sumX = 0.0
    var sumZ = 0.0
    var count = 0
    for (c in state.cells) {
        if (c.plotId == plotId) {
            sumX += (c.gx - gridSide / 2.0) * cellSize + cellSize / 2.0
            sumZ += (c.gz - gridSide / 2.0) * cellSize + cellSize / 2.0
            count++
        }
    }
    if (count == 0) return 0.0 to 0.0
    return clamp(sumX / count, -half + 10.0, half - 10.0) to clamp(sumZ / count, -half + 10.0, half - 10.0)
}

data class HireOutsourcingParams(
    val companyId: String,
    val serviceType: String,
    val modality: String,
    val plotId: Int,
    val cropId: String? = null,
    val playerVehicleUid: String? = null,
)

private val OPERATOR_NAMES = listOf(
    "Edson Oliveira",
    "Valdir Souza",
    "Rodrigo Campos",
    "Mateus Silveira",
    "Gilmar Ferreira",
    "Renato Moraes",
)

fun hireOutsourcingServiceInWorld(state: WorldState, params: HireOutsourcingParams): WorldState {
    val companies = state.outsourcingCompanies?.takeIf { it.isNotEmpty() } ?: createDefaultOutsourcingCompanies()
    val company = companies.find { it.id == params.companyId }
        ?: return state.copy(statusAlert = "Empresa terceirizada não encontrada.")
    val plot = state.plots.find { it.id == params.plotId }
        ?: return state.copy(statusAlert = "Selecione um talhão válido para o serviço.")
    if (plot.isPreservation) {
        return state.copy(statusAlert = "Área de Preservação Permanente (APP): serviços agrícolas bloqueados.")
    }
    if (!plot.owned && !plot.leased) {
        return state.copy(statusAlert = "Compre ou arrende o talhão antes de contratar terceirização nele.")
    }

    if (params.modality == "C_SO_OPERADOR") {
        val ownedVeh = params.playerVehicleUid?.let { uid -> state.vehicles.find { it.uid == uid } }
            ?: state.vehicles.firstOrNull()
        if (ownedVeh == null) {
            return state.copy(statusAlert = "Modalidade C (Só Operador) requer pelo menos 1 máquina própria na fazenda.")
        }
    }

    val (tx, tz) = getPlotCentroid(state, plot.id)
    val distM = hypot(tx - company.hqX, tz - company.hqZ)
    val quote = calculateOutsourcingQuote(company, params.serviceType, params.modality, plot.areaHa, distM)

    if (state.moneyBrl < quote.totalBrl) {
        return state.copy(statusAlert = "Saldo insuficiente para contratar ${company.name} (Orçamento: R$ ${fmtBrl(quote.totalBrl)}).")
    }

    val plotCells = state.cells.filter { c ->
        c.plotId == plot.id &&
            c.soilState != SoilState.AREA_PRESERVACAO &&
            c.soilState != SoilState.AGUA_RIO &&
            c.soilState != SoilState.ESTRADA_ASFALTO &&
            c.soilState != SoilState.ESTRADA_TERRA
    }
    val targetCells = max(12, plotCells.size)

    val playerVeh = if (params.modality == "C_SO_OPERADOR") {
        params.playerVehicleUid?.let { uid -> state.vehicles.find { it.uid == uid } } ?: state.vehicles.firstOrNull()
    } else null

    val existingContracts = state.outsourcingContracts ?: emptyList()
    val assignedVehicleDefId = playerVeh?.defId
        ?: company.fleetVehicleDefIds.getOrNull(existingContracts.size % max(1, company.fleetVehicleDefIds.size))
        ?: "jd_6300_4x4_cab"
    val assignedImplementDefId = if (params.serviceType == "COLHEITA") null else company.fleetImplementDefIds.firstOrNull()

    val opName = OPERATOR_NAMES[existingContracts.size % OPERATOR_NAMES.size]
    val uid = "terc_${state.day}_${existingContracts.size + 1}_${Random.nextInt(1000)}"
    val startX = playerVeh?.x ?: company.hqX
    val startZ = playerVeh?.z ?: company.hqZ

    val newContract = OutsourcingContract(
        uid = uid,
        companyId = company.id,
        companyName = company.name,
        serviceType = params.serviceType,
        modality = params.modality,
        plotId = plot.id,
        cropId = params.cropId ?: "soja",
        targetAreaHa = plot.areaHa,
        progressPct = 0.0,
        processedCellsCount = 0,
        totalTargetCells = targetCells,
        quotedPriceBrl = quote.totalBrl,
        paidBrl = quote.totalBrl,
        status = "A_CAMINHO",
        incident = "NENHUM",
        incidentNote = null,
        assignedVehicleDefId = assignedVehicleDefId,
        assignedImplementDefId = assignedImplementDefId,
        playerVehicleUid = playerVeh?.uid,
        operatorName = opName,
        vehicleX = startX,
        vehicleZ = startZ,
        vehicleHeadingDeg = toDeg(atan2(tx - startX, tz - startZ)),
        createdDay = state.day,
        startedHour = state.hourOfDay,
        harvestedKgStored = 0.0,
    )

    val svcLabel = OUTSOURCING_SERVICE_META[params.serviceType]?.label ?: params.serviceType
    val histEntry = PurchaseHistoryEntry(
        id = "hist_$uid",
        day = state.day,
        kind = "TERCEIRIZACAO",
        itemName = "$svcLabel (${company.name})",
        amountBrl = -quote.totalBrl,
        details = "${plot.name} • Modalidade ${params.modality.first()} • Operador: $opName",
    )

    return state.copy(
        moneyBrl = state.moneyBrl - quote.totalBrl,
        outsourcingCompanies = companies,
        outsourcingContracts = existingContracts + newContract,
        purchaseHistory = (listOf(histEntry) + (state.purchaseHistory ?: emptyList())).take(60),
        ledger = state.ledger + LedgerEntry(
            day = state.day,
            description = "Terceirização: $svcLabel - ${company.name} (${plot.name})",
            amountBrl = -quote.totalBrl,
        ),
        statusAlert = "Contrato aceito! ${company.name} despachou $opName para ${plot.name}.",
    )
}

private data class OutsourcingCellWorkResult(
    val cell: TerrainCell,
    val harvestedKg: Double,
    val modified: Boolean,
)

private fun applyOutsourcingWorkToCell(
    cell: TerrainCell,
    serviceType: String,
    cropId: String,
    day: Int,
    includeInputs: Boolean,
): OutsourcingCellWorkResult {
    return when (serviceType) {
        "PREPARO_SOLO", "ARACAO" -> {
            if (cell.soilState != SoilState.ARADO && cell.soilState != SoilState.PRONTO_COLHEITA) {
                OutsourcingCellWorkResult(
                    cell.copy(soilState = SoilState.ARADO, cropId = null, growthProgress = 0.0, weedLevel = 0.0, compaction = max(0.08, cell.compaction - 0.1)),
                    0.0, true
                )
            } else OutsourcingCellWorkResult(cell, 0.0, false)
        }
        "GRADAGEM" -> {
            if (cell.soilState != SoilState.ARADO) {
                OutsourcingCellWorkResult(cell.copy(soilState = SoilState.ARADO, weedLevel = 0.01), 0.0, true)
            } else OutsourcingCellWorkResult(cell, 0.0, false)
        }
        "SUBSOLAGEM", "TERRACO_CURVA" -> {
            if (cell.soilState != SoilState.SUBSOLADO || cell.compaction > 0.1) {
                OutsourcingCellWorkResult(cell.copy(soilState = SoilState.SUBSOLADO, compaction = 0.05, weedLevel = 0.02), 0.0, true)
            } else OutsourcingCellWorkResult(cell, 0.0, false)
        }
        "LIMPEZA_CAMPO", "DESMATAMENTO_LEGAL" -> {
            if (cell.soilState == SoilState.PEDRAS_MATO || cell.weedLevel > 0.05) {
                OutsourcingCellWorkResult(cell.copy(soilState = SoilState.PASTAGEM, weedLevel = 0.0), 0.0, true)
            } else OutsourcingCellWorkResult(cell, 0.0, false)
        }
        "PLANTIO", "SEMEADURA" -> {
            if (cell.soilState != SoilState.SEMEADO && cell.soilState != SoilState.CRESCENDO && cell.soilState != SoilState.PRONTO_COLHEITA) {
                OutsourcingCellWorkResult(
                    cell.copy(
                        soilState = SoilState.SEMEADO,
                        cropId = cropId,
                        growthProgress = 0.06,
                        fertility = clamp(cell.fertility + (if (includeInputs) 0.12 else 0.04), 0.4, 1.0),
                    ),
                    0.0, true
                )
            } else OutsourcingCellWorkResult(cell, 0.0, false)
        }
        "PULVERIZACAO" -> {
            if (cell.weedLevel > 0.01 || (cell.pestLevel ?: 0.0) > 0.01) {
                OutsourcingCellWorkResult(cell.copy(weedLevel = 0.0, pestLevel = 0.0), 0.0, true)
            } else OutsourcingCellWorkResult(cell, 0.0, false)
        }
        "ADUBACAO", "CALAGEM" -> {
            OutsourcingCellWorkResult(
                cell.copy(
                    fertility = min(1.0, cell.fertility + 0.2),
                    nitrogen = min(1.0, (cell.nitrogen ?: 0.65) + 0.22),
                    phosphorus = min(1.0, (cell.phosphorus ?: 0.62) + 0.2),
                    potassium = min(1.0, (cell.potassium ?: 0.64) + 0.2),
                    phLevel = if (serviceType == "CALAGEM") 6.45 else cell.phLevel,
                    fertilizedStages = min(3, (cell.fertilizedStages ?: 0) + 1),
                ),
                0.0, true
            )
        }
        "COLHEITA" -> {
            if (cell.soilState == SoilState.PRONTO_COLHEITA && cell.cropId != null) {
                val crop = getCropById(cell.cropId)
                val harvestedKg = crop.baseYieldTonsPerHa * 15.5
                OutsourcingCellWorkResult(
                    cell.copy(soilState = SoilState.PALHADA, lastCropId = cell.cropId, cropId = null, growthProgress = 0.0),
                    harvestedKg, true
                )
            } else OutsourcingCellWorkResult(cell, 0.0, false)
        }
        "SILAGEM" -> {
            if ((cell.soilState == SoilState.PRONTO_COLHEITA || cell.soilState == SoilState.CRESCENDO) && cell.cropId != null) {
                OutsourcingCellWorkResult(cell.copy(soilState = SoilState.PALHADA, cropId = null, growthProgress = 0.0), 520.0, true)
            } else OutsourcingCellWorkResult(cell, 0.0, false)
        }
        "FENACAO" -> {
            if (cell.soilState == SoilState.PASTAGEM) {
                OutsourcingCellWorkResult(cell.copy(isHayDried = false, isWindrowed = false, pastureRestDays = 0), 280.0, true)
            } else OutsourcingCellWorkResult(cell, 0.0, false)
        }
        else -> OutsourcingCellWorkResult(
            cell.copy(irrigatedUntilDay = max(cell.irrigatedUntilDay ?: 0, day)),
            0.0, true
        )
    }
}

fun stepOutsourcingContracts(state: WorldState, dtSec: Double, timeScale: Double = 1.0): WorldState {
    val contracts = state.outsourcingContracts ?: emptyList()
    if (contracts.isEmpty()) return state

    val hasActive = contracts.any {
        it.status == "AGUARDANDO" || it.status == "ACEITO" || it.status == "A_CAMINHO" || it.status == "CHEGOU" || it.status == "TRABALHANDO"
    }
    if (!hasActive) return state

    val dt = clamp(dtSec, 0.001, 0.08) * max(1.0, timeScale * 0.55)
    val companies = state.outsourcingCompanies ?: createDefaultOutsourcingCompanies()
    val rainIntensity = (WEATHER_META[state.weather] ?: WEATHER_META.values.first()).rainIntensity

    val nextCells = state.cells.toMutableList()
    val nextInventory = state.inventoryKg.toMutableMap()
    var nextProps = state.props
    var propsCopied = false
    var alert = state.statusAlert
    val nextCompanies = companies.toMutableList()

    val gridSide = getGridSide(state)
    val cellSize = getCellSize(gridSide)

    val updatedContracts = contracts.map { c ->
        if (c.status == "CONCLUIDO" || c.status == "CANCELADO" || c.status == "PAUSADO" || c.status == "PROBLEMA") {
            return@map c
        }
        if (c.status == "AGUARDANDO" || c.status == "ACEITO") {
            return@map c.copy(status = "A_CAMINHO")
        }

        val (cx, cz) = getPlotCentroid(state, c.plotId)
        val cSpec = getVehiclePhysicsSpec(getVehicleDef(c.assignedVehicleDefId))
        val cHalfTrack = max(cSpec.trackWidthM * 0.5, 0.45)

        if (c.status == "A_CAMINHO") {
            val dx = cx - c.vehicleX
            val dz = cz - c.vehicleZ
            val dist = hypot(dx, dz)
            val heading = toDeg(atan2(dx, dz))
            if (dist <= 6.5) {
                val pose = sampleFourWheelTerrainPose(
                    state, cx, cz, heading, cSpec.frontAxleZ, cSpec.rearAxleZ, cHalfTrack,
                    cSpec.suspensionMaxTravelM, cSpec.cogHeightM
                )
                alert = "${c.companyName} chegou ao talhão #${c.plotId} e iniciou operação."
                return@map c.copy(
                    vehicleX = cx,
                    vehicleZ = cz,
                    vehicleHeadingDeg = heading,
                    vehiclePitchDeg = pose.pitchDeg,
                    vehicleRollDeg = pose.rollDeg,
                    status = "TRABALHANDO",
                )
            }
            val travelStep = min(dist, 22.0 * dt)
            val nextX = c.vehicleX + (dx / max(dist, 0.001)) * travelStep
            val nextZ = c.vehicleZ + (dz / max(dist, 0.001)) * travelStep
            val pose = sampleFourWheelTerrainPose(
                state, nextX, nextZ, heading, cSpec.frontAxleZ, cSpec.rearAxleZ, cHalfTrack,
                cSpec.suspensionMaxTravelM, cSpec.cogHeightM
            )
            return@map c.copy(
                vehicleX = nextX,
                vehicleZ = nextZ,
                vehicleHeadingDeg = heading,
                vehiclePitchDeg = pose.pitchDeg,
                vehicleRollDeg = pose.rollDeg,
            )
        }

        if (c.status == "CHEGOU") {
            return@map c.copy(status = "TRABALHANDO")
        }

        if (rainIntensity > 0.72 && (c.serviceType == "COLHEITA" || c.serviceType == "PULVERIZACAO" || c.serviceType == "FENACAO")) {
            alert = "${c.companyName} pausou serviço no Talhão #${c.plotId} por chuva intensa."
            return@map c.copy(
                status = "PROBLEMA",
                incident = "CHUVA_FORTE",
                incidentNote = "Chuva intensa impede colheita/pulverização segura no momento.",
            )
        }

        val comp = companies.find { it.id == c.companyId }
        val shouldCheckIncident = c.processedCellsCount > 0 &&
            floor(c.progressPct).toInt() == 55 &&
            c.incident == "NENHUM" &&
            (comp?.reliabilityPct ?: 95.0) < 95.0
        if (shouldCheckIncident) {
            val incType = if ((c.createdDay + c.plotId) % 2 == 0) "QUEBRA_MECANICA" else "FALTA_COMBUSTIVEL"
            val note = if (incType == "QUEBRA_MECANICA") {
                "Mangueira hidráulica rompeu durante a operação. Autorize reparo rápido em campo."
            } else {
                "Tanque de diesel na reserva durante o turno. Autorize reabastecimento."
            }
            alert = "Ocorrência na terceirização (${c.companyName}): $note"
            return@map c.copy(
                status = "PROBLEMA",
                incident = incType,
                incidentNote = note,
            )
        }

        val cellsPerStep = max(1, (dt * 18.0).roundToInt())
        var workedThisStep = 0
        var lastCellX = c.vehicleX
        var lastCellZ = c.vehicleZ
        var stepHarvestedKg = 0.0

        val plotCellIndices = mutableListOf<Int>()
        for (i in nextCells.indices) {
            if (nextCells[i].plotId == c.plotId) plotCellIndices.add(i)
        }

        val total = max(1, plotCellIndices.size)
        val startCursor = min(c.processedCellsCount, total)
        val endCursor = min(total, startCursor + cellsPerStep)

        for (k in startCursor until endCursor) {
            val cellIdx = plotCellIndices[k]
            val cell = nextCells[cellIdx]
            val prevCropId = cell.cropId
            val res = applyOutsourcingWorkToCell(
                cell, c.serviceType, c.cropId, state.day, c.modality == "B_COMPLETO_INSUMOS"
            )
            if (res.modified) {
                nextCells[cellIdx] = res.cell
                if (res.harvestedKg > 0.0) {
                    val storeKey = when (c.serviceType) {
                        "SILAGEM" -> "silagem_granel"
                        "FENACAO" -> "feno_fardo"
                        else -> prevCropId ?: c.cropId
                    }
                    nextInventory[storeKey] = (nextInventory[storeKey] ?: 0.0) + res.harvestedKg
                    stepHarvestedKg += res.harvestedKg
                }
                if (c.serviceType == "LIMPEZA_CAMPO" || c.serviceType == "DESMATAMENTO_LEGAL") {
                    val cellX = (cell.gx - gridSide / 2.0) * cellSize + cellSize / 2.0
                    val cellZ = (cell.gz - gridSide / 2.0) * cellSize + cellSize / 2.0
                    nextProps = nextProps.filterNot { pr ->
                        (pr.type == WorldPropType.BUSH || pr.type == WorldPropType.STONE || pr.type == WorldPropType.PLANT_CLUSTER) &&
                            hypot(pr.x - cellX, pr.z - cellZ) < cellSize * 0.9
                    }
                    propsCopied = true
                }
            }
            lastCellX = (cell.gx - gridSide / 2.0) * cellSize + cellSize / 2.0
            lastCellZ = (cell.gz - gridSide / 2.0) * cellSize + cellSize / 2.0
            workedThisStep++
        }

        val newProcessed = startCursor + workedThisStep
        val progressPct = min(100.0, ((newProcessed.toDouble() / total) * 100.0).roundToInt().toDouble())
        val heading = if (hypot(lastCellX - c.vehicleX, lastCellZ - c.vehicleZ) > 0.2) {
            toDeg(atan2(lastCellX - c.vehicleX, lastCellZ - c.vehicleZ))
        } else c.vehicleHeadingDeg

        val workPose = sampleFourWheelTerrainPose(
            state, lastCellX, lastCellZ, heading, cSpec.frontAxleZ, cSpec.rearAxleZ, cHalfTrack,
            cSpec.suspensionMaxTravelM, cSpec.cogHeightM
        )

        if (newProcessed >= total) {
            for (ci in nextCompanies.indices) {
                if (nextCompanies[ci].id == c.companyId) {
                    nextCompanies[ci] = nextCompanies[ci].copy(totalJobsCompleted = nextCompanies[ci].totalJobsCompleted + 1)
                }
            }
            alert = "Serviço terceirizado CONCLUÍDO por ${c.companyName} no Talhão #${c.plotId}!"
            return@map c.copy(
                processedCellsCount = total,
                totalTargetCells = total,
                progressPct = 100.0,
                vehicleX = lastCellX,
                vehicleZ = lastCellZ,
                vehicleHeadingDeg = heading,
                vehiclePitchDeg = workPose.pitchDeg,
                vehicleRollDeg = workPose.rollDeg,
                status = "CONCLUIDO",
                completedDay = state.day,
                harvestedKgStored = (c.harvestedKgStored ?: 0.0) + stepHarvestedKg,
            )
        }

        c.copy(
            processedCellsCount = newProcessed,
            totalTargetCells = total,
            progressPct = progressPct,
            vehicleX = lastCellX,
            vehicleZ = lastCellZ,
            vehicleHeadingDeg = heading,
            vehiclePitchDeg = workPose.pitchDeg,
            vehicleRollDeg = workPose.rollDeg,
            harvestedKgStored = (c.harvestedKgStored ?: 0.0) + stepHarvestedKg,
        )
    }

    return state.copy(
        cells = nextCells,
        inventoryKg = nextInventory,
        props = if (propsCopied) nextProps else state.props,
        outsourcingCompanies = nextCompanies,
        outsourcingContracts = updatedContracts,
        statusAlert = alert,
    )
}

fun resolveOutsourcingIncidentInWorld(state: WorldState, contractUid: String, action: String): WorldState {
    val contracts = state.outsourcingContracts ?: return state
    val target = contracts.find { it.uid == contractUid } ?: return state

    if (action == "CANCELAR") {
        val refund = (target.paidBrl * max(0.15, (100.0 - target.progressPct) / 100.0) * 0.7).roundToInt().toDouble()
        return state.copy(
            moneyBrl = state.moneyBrl + refund,
            outsourcingContracts = contracts.map { c ->
                if (c.uid == contractUid) c.copy(status = "CANCELADO", incidentNote = "Cancelado pelo contratante.") else c
            },
            ledger = if (refund > 0) {
                state.ledger + LedgerEntry(state.day, "Estorno parcial terceirização (${target.companyName})", refund)
            } else state.ledger,
            statusAlert = "Contrato cancelado. Reembolso proporcional de R$ ${fmtBrl(refund)}.",
        )
    }

    val cost = if (action == "FORNECER_APOIO") 350.0 else 0.0
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente (R$ 350) para apoio imediato em campo.")
    }

    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        outsourcingContracts = contracts.map { c ->
            if (c.uid == contractUid) {
                c.copy(
                    status = "TRABALHANDO",
                    incident = "NENHUM",
                    incidentNote = null,
                    processedCellsCount = c.processedCellsCount + 1,
                )
            } else c
        },
        ledger = if (cost > 0) {
            state.ledger + LedgerEntry(state.day, "Apoio em campo terceirização (${target.companyName})", -cost)
        } else state.ledger,
        statusAlert = "${target.companyName} retomou a operação no Talhão #${target.plotId}!",
    )
}

fun rateOutsourcingContractInWorld(state: WorldState, contractUid: String, stars: Int): WorldState {
    val cleanStars = stars.coerceIn(1, 5)
    val contracts = state.outsourcingContracts ?: return state
    val target = contracts.find { it.uid == contractUid } ?: return state
    if (target.status != "CONCLUIDO") return state
    val companies = state.outsourcingCompanies ?: createDefaultOutsourcingCompanies()

    return state.copy(
        outsourcingContracts = contracts.map { c ->
            if (c.uid == contractUid) c.copy(playerRating = cleanStars) else c
        },
        outsourcingCompanies = companies.map { co ->
            if (co.id != target.companyId) co
            else {
                val nextRep = (((co.reputation * 4.0 + cleanStars) / 5.0) * 10.0).roundToInt() / 10.0
                co.copy(reputation = clamp(nextRep, 1.0, 5.0))
            }
        },
        statusAlert = "Avaliação $cleanStars/5 enviada para ${target.companyName}!",
    )
}
