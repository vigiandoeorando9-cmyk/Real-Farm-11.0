package com.example.simulation

import com.example.domain.ConservationTier
import com.example.domain.LedgerEntry
import com.example.domain.OwnedVehicle
import com.example.domain.RepairRecord
import com.example.domain.VehicleCategory
import com.example.domain.WORKSHOP_COMPONENT_LABELS
import com.example.domain.WorkshopComponent
import com.example.domain.WorldState
import com.example.domain.conservationTierFromPct
import com.example.domain.getVehicleDef
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class GranularHealths(
    val oilFilter: Int,
    val belts: Int,
    val tiresWear: Int,
    val hydraulicHoses: Int,
    val cutterDiscs: Int,
)

data class WorkshopQuote(
    val vehicleUid: String,
    val title: String,
    val conservationPct: Double,
    val tier: ConservationTier,
    val dirtPct: Double,
    val componentHealths: Map<WorkshopComponent, Double>,
    val componentCostsBrl: Map<WorkshopComponent, Double>,
    val fullRepairCostBrl: Double,
    val preventiveMaintenanceCostBrl: Double,
    val washCostBrl: Double,
    val refuelCostBrl: Double,
    val towCostBrl: Double,
    val granularHealths: GranularHealths,
    val breakdownNote: String?,
)

private fun clamp(v: Double, minVal: Double, maxVal: Double): Double = v.coerceIn(minVal, maxVal)

private fun categoryLaborMultiplier(cat: VehicleCategory): Double = when (cat) {
    VehicleCategory.COLHEITADEIRA -> 1.35
    VehicleCategory.PULVERIZADOR_AUTOPROPELIDO -> 1.28
    VehicleCategory.CAMINHAO -> 1.20
    VehicleCategory.FLORESTAL,
    VehicleCategory.CARREGADEIRA,
    VehicleCategory.RETROESCAVADEIRA -> 1.25
    VehicleCategory.ESCAVADEIRA,
    VehicleCategory.MOTONIVELADORA,
    VehicleCategory.TRATOR_ESTEIRA -> 1.30
    VehicleCategory.EMPILHADEIRA,
    VehicleCategory.MANIPULADOR,
    VehicleCategory.MINICARREGADEIRA -> 1.10
    VehicleCategory.UTILITARIO_MOTO -> 0.55
    VehicleCategory.CAMINHONETE -> 0.85
    VehicleCategory.TRATOR -> 1.0
}

fun inspectVehicle(veh: OwnedVehicle): WorkshopQuote {
    val def = getVehicleDef(veh.defId)
    val tier = conservationTierFromPct(veh.conservationPct)
    val baseRate = clamp(def.priceBrl * 0.00055, 38.0, 420.0) * categoryLaborMultiplier(def.category)

    val healths = mapOf(
        WorkshopComponent.MOTOR to clamp(veh.engineHealthPct, 0.0, 100.0),
        WorkshopComponent.TRANSMISSAO to clamp(veh.transmissionHealthPct, 0.0, 100.0),
        WorkshopComponent.SUSPENSAO_PNEUS to clamp(veh.suspensionTiresHealthPct, 0.0, 100.0),
        WorkshopComponent.HIDRAULICA to clamp(veh.hydraulicsHealthPct, 0.0, 100.0),
        WorkshopComponent.LATARIA to clamp(veh.bodyHealthPct, 0.0, 100.0),
    )

    val compWeights = mapOf(
        WorkshopComponent.MOTOR to 0.30,
        WorkshopComponent.TRANSMISSAO to 0.24,
        WorkshopComponent.SUSPENSAO_PNEUS to 0.20,
        WorkshopComponent.HIDRAULICA to 0.14,
        WorkshopComponent.LATARIA to 0.12,
    )

    val costs = mutableMapOf<WorkshopComponent, Double>()
    var compSum = 0.0
    for (comp in WorkshopComponent.entries) {
        val missing = max(0.0, 100.0 - (healths[comp] ?: veh.conservationPct))
        val cost = if (missing < 0.2) 0.0 else (missing * baseRate * (compWeights[comp] ?: 0.2)).roundToInt().toDouble()
        costs[comp] = cost
        compSum += cost
    }

    val consMissing = max(0.0, 100.0 - veh.conservationPct)
    val fullRepairCostBrl = max(compSum, (consMissing * baseRate * 0.85).roundToInt().toDouble())
    val preventiveMaintenanceCostBrl = (180.0 + def.horsepower * 1.4).roundToInt().toDouble()
    val washCostBrl = if (veh.dirtPct > 2.0) (45.0 + veh.dirtPct * 1.6).roundToInt().toDouble() else 0.0
    val fuelMissing = max(0.0, def.fuelCapacityL - veh.fuelL)
    val refuelCostBrl = (fuelMissing * 6.2).roundToInt().toDouble()
    val towCostBrl = if (tier == ConservationTier.IMOBILIZADO) (350.0 + def.weightKg * 0.06).roundToInt().toDouble() else 0.0

    return WorkshopQuote(
        vehicleUid = veh.uid,
        title = "${def.brand} ${def.model}",
        conservationPct = veh.conservationPct,
        tier = tier,
        dirtPct = veh.dirtPct,
        componentHealths = healths,
        componentCostsBrl = costs,
        fullRepairCostBrl = fullRepairCostBrl,
        preventiveMaintenanceCostBrl = preventiveMaintenanceCostBrl,
        washCostBrl = washCostBrl,
        refuelCostBrl = refuelCostBrl,
        towCostBrl = towCostBrl,
        granularHealths = GranularHealths(
            oilFilter = clamp(veh.oilFilterHealthPct ?: veh.engineHealthPct, 0.0, 100.0).roundToInt(),
            belts = clamp(veh.beltsHealthPct ?: veh.engineHealthPct, 0.0, 100.0).roundToInt(),
            tiresWear = clamp(veh.tiresWearPct ?: veh.suspensionTiresHealthPct, 0.0, 100.0).roundToInt(),
            hydraulicHoses = clamp(veh.hydraulicHosesHealthPct ?: veh.hydraulicsHealthPct, 0.0, 100.0).roundToInt(),
            cutterDiscs = clamp(veh.cutterDiscsHealthPct ?: 100.0, 0.0, 100.0).roundToInt(),
        ),
        breakdownNote = veh.lastBreakdownNote,
    )
}

fun repairVehicleFull(state: WorldState, vehicleUid: String, includeRefuel: Boolean = false): WorldState {
    val veh = state.vehicles.find { it.uid == vehicleUid } ?: return state
    val def = getVehicleDef(veh.defId)
    val quote = inspectVehicle(veh)
    val totalCost = quote.fullRepairCostBrl + (if (includeRefuel) quote.refuelCostBrl else 0.0)

    if (totalCost <= 0.0 && !includeRefuel) {
        return state.copy(statusAlert = "${def.model} já está 100% conservado!")
    }
    if (state.moneyBrl < totalCost) {
        return state.copy(statusAlert = "Saldo insuficiente para reparo completo (R$ ${totalCost.roundToInt()}).")
    }

    val record = RepairRecord(
        day = state.day,
        serviceType = "Reparo Completo",
        description = if (includeRefuel) "Restauração geral + abastecimento" else "Restauração completa de todos os sistemas",
        conservationBeforePct = veh.conservationPct,
        conservationAfterPct = 100.0,
        costBrl = totalCost,
    )

    val updatedVeh = veh.copy(
        conservationPct = 100.0,
        wearPct = 0.0,
        engineHealthPct = 100.0,
        transmissionHealthPct = 100.0,
        suspensionTiresHealthPct = 100.0,
        hydraulicsHealthPct = 100.0,
        bodyHealthPct = 100.0,
        oilFilterHealthPct = 100.0,
        beltsHealthPct = 100.0,
        tiresWearPct = 100.0,
        hydraulicHosesHealthPct = 100.0,
        cutterDiscsHealthPct = 100.0,
        lastBreakdownNote = null,
        isBogged = false,
        bogSeverity = 0.0,
        stallTimerSec = 0.0,
        lastImpactSeverity = 0.0,
        fuelL = if (includeRefuel) def.fuelCapacityL else veh.fuelL,
        repairHistory = veh.repairHistory + record,
    )

    return state.copy(
        moneyBrl = state.moneyBrl - totalCost,
        vehicles = state.vehicles.map { if (it.uid == vehicleUid) updatedVeh else it },
        ledger = if (totalCost > 0.0) state.ledger + LedgerEntry(state.day, "Oficina: Reparo ${def.model}", -totalCost) else state.ledger,
        statusAlert = "Oficina: ${def.model} restaurado para 100% (-R$ ${totalCost.roundToInt()}).",
    )
}

fun repairVehicleComponent(state: WorldState, vehicleUid: String, component: WorkshopComponent): WorldState {
    val veh = state.vehicles.find { it.uid == vehicleUid } ?: return state
    val def = getVehicleDef(veh.defId)
    val quote = inspectVehicle(veh)
    val cost = quote.componentCostsBrl[component] ?: 0.0

    val compLabel = WORKSHOP_COMPONENT_LABELS[component] ?: component.name
    if (cost <= 0.0) {
        return state.copy(statusAlert = "$compLabel de ${def.model} já está em ótimo estado.")
    }
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para reparar $compLabel (R$ ${cost.roundToInt()}).")
    }

    var updated = when (component) {
        WorkshopComponent.MOTOR -> veh.copy(
            engineHealthPct = 100.0,
            oilFilterHealthPct = 100.0,
            beltsHealthPct = 100.0,
            lastBreakdownNote = null,
            stallTimerSec = 0.0,
        )
        WorkshopComponent.TRANSMISSAO -> veh.copy(transmissionHealthPct = 100.0)
        WorkshopComponent.SUSPENSAO_PNEUS -> veh.copy(suspensionTiresHealthPct = 100.0, tiresWearPct = 100.0)
        WorkshopComponent.HIDRAULICA -> veh.copy(hydraulicsHealthPct = 100.0, hydraulicHosesHealthPct = 100.0)
        WorkshopComponent.LATARIA -> veh.copy(bodyHealthPct = 100.0, cutterDiscsHealthPct = 100.0)
    }

    val avg = clamp(
        updated.engineHealthPct * 0.25 +
            updated.transmissionHealthPct * 0.22 +
            updated.suspensionTiresHealthPct * 0.20 +
            updated.hydraulicsHealthPct * 0.15 +
            updated.bodyHealthPct * 0.18,
        0.0,
        100.0
    )

    val record = RepairRecord(
        day = state.day,
        serviceType = "Reparo Parcial",
        description = "Manutenção em $compLabel",
        conservationBeforePct = veh.conservationPct,
        conservationAfterPct = avg,
        costBrl = cost,
    )

    updated = updated.copy(
        conservationPct = avg,
        wearPct = clamp(100.0 - avg, 0.0, 100.0),
        repairHistory = updated.repairHistory + record,
    )

    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        vehicles = state.vehicles.map { if (it.uid == vehicleUid) updated else it },
        ledger = state.ledger + LedgerEntry(state.day, "Oficina: $compLabel (${def.model})", -cost),
        statusAlert = "Oficina: $compLabel reparado! Conservação agora em ${avg.toInt()}%.",
    )
}

fun performPreventiveMaintenance(state: WorldState, vehicleUid: String): WorldState {
    val veh = state.vehicles.find { it.uid == vehicleUid } ?: return state
    val def = getVehicleDef(veh.defId)
    val quote = inspectVehicle(veh)
    val cost = quote.preventiveMaintenanceCostBrl

    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para manutenção preventiva (R$ ${cost.roundToInt()}).")
    }

    val nextCons = clamp(veh.conservationPct + 10.0, 0.0, 100.0)
    val record = RepairRecord(
        day = state.day,
        serviceType = "Manutenção Preventiva",
        description = "Troca de óleo, filtros e lubrificação (-28% desgaste por 12h)",
        conservationBeforePct = veh.conservationPct,
        conservationAfterPct = nextCons,
        costBrl = cost,
    )

    val updated = veh.copy(
        conservationPct = nextCons,
        wearPct = clamp(100.0 - nextCons, 0.0, 100.0),
        engineHealthPct = min(100.0, veh.engineHealthPct + 10.0),
        transmissionHealthPct = min(100.0, veh.transmissionHealthPct + 10.0),
        suspensionTiresHealthPct = min(100.0, veh.suspensionTiresHealthPct + 10.0),
        hydraulicsHealthPct = min(100.0, veh.hydraulicsHealthPct + 10.0),
        bodyHealthPct = min(100.0, veh.bodyHealthPct + 6.0),
        oilFilterHealthPct = 100.0,
        beltsHealthPct = 100.0,
        tiresWearPct = min(100.0, (veh.tiresWearPct ?: 100.0) + 15.0),
        hydraulicHosesHealthPct = min(100.0, (veh.hydraulicHosesHealthPct ?: 100.0) + 18.0),
        cutterDiscsHealthPct = min(100.0, (veh.cutterDiscsHealthPct ?: 100.0) + 20.0),
        lastBreakdownNote = null,
        preventiveBonusHours = 12.0,
        stallTimerSec = 0.0,
        repairHistory = veh.repairHistory + record,
    )

    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        vehicles = state.vehicles.map { if (it.uid == vehicleUid) updated else it },
        ledger = state.ledger + LedgerEntry(state.day, "Oficina: Preventiva ${def.model}", -cost),
        statusAlert = "Preventiva concluída em ${def.model}! Proteção ativa por 12h.",
    )
}

fun washVehicle(state: WorldState, vehicleUid: String): WorldState {
    val veh = state.vehicles.find { it.uid == vehicleUid } ?: return state
    val def = getVehicleDef(veh.defId)
    if (veh.dirtPct < 2.0) {
        return state.copy(statusAlert = "${def.model} já está limpo.")
    }
    val quote = inspectVehicle(veh)
    val cost = quote.washCostBrl
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para lavagem (R$ ${cost.roundToInt()}).")
    }

    val record = RepairRecord(
        day = state.day,
        serviceType = "Lavagem",
        description = "Lavagem completa de chassi, rodado e cabine",
        conservationBeforePct = veh.conservationPct,
        conservationAfterPct = veh.conservationPct,
        costBrl = cost,
    )

    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        vehicles = state.vehicles.map {
            if (it.uid == vehicleUid) it.copy(dirtPct = 0.0, repairHistory = it.repairHistory + record) else it
        },
        statusAlert = "${def.model} lavado na oficina (-R$ ${cost.roundToInt()}).",
    )
}

fun towAndRecoverVehicle(state: WorldState, vehicleUid: String): WorldState {
    val veh = state.vehicles.find { it.uid == vehicleUid } ?: return state
    val def = getVehicleDef(veh.defId)
    val quote = inspectVehicle(veh)
    val cost = max(quote.towCostBrl, 280.0)
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para guincho (R$ ${cost.roundToInt()}).")
    }

    val recovPct = max(veh.conservationPct, 35.0)
    val record = RepairRecord(
        day = state.day,
        serviceType = "Guincho & Reparo Emergencial",
        description = "Reboque até a Oficina Mecânica + recuperação operacional",
        conservationBeforePct = veh.conservationPct,
        conservationAfterPct = recovPct,
        costBrl = cost,
    )

    val updated = veh.copy(
        x = 26.0,
        z = 26.0,
        speedMs = 0.0,
        conservationPct = recovPct,
        wearPct = clamp(100.0 - recovPct, 0.0, 100.0),
        engineHealthPct = max(veh.engineHealthPct, 35.0),
        transmissionHealthPct = max(veh.transmissionHealthPct, 35.0),
        suspensionTiresHealthPct = max(veh.suspensionTiresHealthPct, 35.0),
        hydraulicsHealthPct = max(veh.hydraulicsHealthPct, 35.0),
        bodyHealthPct = max(veh.bodyHealthPct, 35.0),
        isBogged = false,
        bogSeverity = 0.0,
        towCableTargetUid = null,
        stallTimerSec = 0.0,
        repairHistory = veh.repairHistory + record,
    )

    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        vehicles = state.vehicles.map { if (it.uid == vehicleUid) updated else it },
        ledger = state.ledger + LedgerEntry(state.day, "Guincho Oficina: ${def.model}", -cost),
        statusAlert = "${def.model} guinchado para a Oficina Mecânica (-R$ ${cost.roundToInt()}).",
    )
}
