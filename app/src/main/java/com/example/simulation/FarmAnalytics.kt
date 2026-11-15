package com.example.simulation

import com.example.domain.ACHIEVEMENT_CATALOG
import com.example.domain.ANIMAL_META
import com.example.domain.AnimalSpecies
import com.example.domain.ContractType
import com.example.domain.FarmEmployee
import com.example.domain.LedgerEntry
import com.example.domain.PurchaseHistoryEntry
import com.example.domain.SoilState
import com.example.domain.WorldState
import com.example.domain.getCropById
import com.example.domain.getVehicleDef
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private fun fmtBrlInt(v: Double): String = String.format(Locale("pt", "BR"), "%,.0f", v)

fun evaluateAchievements(state: WorldState): WorldState {
    val unlocked = (state.achievementsUnlocked ?: emptyList()).toMutableSet()
    var bonusBrl = 0.0
    var lastTitle: String? = null

    fun unlock(id: String) {
        if (unlocked.contains(id)) return
        val def = ACHIEVEMENT_CATALOG.find { it.id == id } ?: return
        unlocked.add(id)
        bonusBrl += def.rewardBrl
        lastTitle = def.title
    }

    if (state.ledger.any { it.description.contains("Colheita") || it.description.contains("Balança") }) {
        unlock("ach_first_harvest")
    }
    if (state.cells.any { (it.fertilizedStages ?: 0) >= 2 }) unlock("ach_soil_master")
    if (state.cells.any { it.cropId != null && it.lastCropId != null && it.cropId != it.lastCropId }) {
        unlock("ach_rotation_pro")
    }
    if (state.vehicles.size >= 5) unlock("ach_fleet_4")
    if (state.moneyBrl >= 1_000_000.0) unlock("ach_millionaire")
    if (state.bankLoanBrl <= 0.0 && state.ledger.any { it.description.contains("Amortização") }) {
        unlock("ach_debt_free")
    }
    if ((state.employees ?: emptyList()).any { it.assignedPlotId != null }) {
        unlock("ach_team_leader")
    }
    if (state.herds.sumOf { it.count } >= 40) unlock("ach_livestock_50")

    if (bonusBrl <= 0.0) return state

    return state.copy(
        moneyBrl = state.moneyBrl + bonusBrl,
        achievementsUnlocked = unlocked.toList(),
        ledger = state.ledger + LedgerEntry(state.day, "Conquista: $lastTitle", bonusBrl),
        statusAlert = "Conquista Desbloqueada: $lastTitle! Prêmio +R$ ${fmtBrlInt(bonusBrl)}.",
    )
}

fun irrigatePlotInWorld(state: WorldState, plotId: Int): WorldState {
    val plot = state.plots.find { it.id == plotId } ?: return state
    if (!plot.owned && !plot.leased) {
        return state.copy(statusAlert = "Adquira ou arrende este talhão antes de acionar irrigação.")
    }
    val cost = (plot.areaHa * 240.0).roundToInt().toDouble()
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para acionar pivô de irrigação (R$ ${fmtBrlInt(cost)}).")
    }
    val nextCells = state.cells.map { c ->
        if (c.plotId == plotId) {
            c.copy(
                moisture = min(1.0, c.moisture + 0.34),
                waterStress = 0.0,
                isIrrigated = true,
                irrigatedUntilDay = state.day + 2,
            )
        } else c
    }
    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        cells = nextCells,
        ledger = state.ledger + LedgerEntry(state.day, "Irrigação Pivô Central: ${plot.name}", -cost),
        statusAlert = "${plot.name} irrigado por aspersão até Dia ${state.day + 2} (-R$ ${fmtBrlInt(cost)}).",
    )
}

fun applyLimeToPlotInWorld(state: WorldState, plotId: Int): WorldState {
    val plot = state.plots.find { it.id == plotId } ?: return state
    if (!plot.owned && !plot.leased) {
        return state.copy(statusAlert = "Talhão precisa ser próprio ou arrendado para receber calcário.")
    }
    val cost = (plot.areaHa * 360.0).roundToInt().toDouble()
    val stockKg = state.inputsStockKg?.get("calcario") ?: 0.0
    val neededKg = (plot.areaHa * 180.0).roundToInt().toDouble()
    val useStock = stockKg >= neededKg
    val charge = if (useStock) (cost * 0.25).roundToInt().toDouble() else cost
    if (state.moneyBrl < charge) {
        return state.copy(statusAlert = "Saldo insuficiente para calagem (R$ ${fmtBrlInt(charge)}).")
    }

    val nextInputs = (state.inputsStockKg ?: emptyMap()).toMutableMap()
    if (useStock) nextInputs["calcario"] = max(0.0, stockKg - neededKg)

    val nextCells = state.cells.map { c ->
        if (c.plotId == plotId) {
            c.copy(phLevel = 6.45, fertility = min(1.0, c.fertility + 0.14))
        } else c
    }
    return evaluateAchievements(
        state.copy(
            moneyBrl = state.moneyBrl - charge,
            cells = nextCells,
            inputsStockKg = nextInputs,
            ledger = state.ledger + LedgerEntry(state.day, "Calagem (pH 6.4): ${plot.name}", -charge),
            statusAlert = "Calagem concluída em ${plot.name}! pH elevado para 6,45 (-R$ ${fmtBrlInt(charge)}).",
        )
    )
}

fun sprayPlotPestsInWorld(state: WorldState, plotId: Int): WorldState {
    val plot = state.plots.find { it.id == plotId } ?: return state
    if (!plot.owned && !plot.leased) {
        return state.copy(statusAlert = "Talhão precisa ser próprio ou arrendado para pulverizar.")
    }
    val cost = (plot.areaHa * 195.0).roundToInt().toDouble()
    val stockKg = state.inputsStockKg?.get("defensivo") ?: 0.0
    val useStock = stockKg >= 40.0
    val charge = if (useStock) (cost * 0.22).roundToInt().toDouble() else cost
    if (state.moneyBrl < charge) {
        return state.copy(statusAlert = "Saldo insuficiente para pulverização (R$ ${fmtBrlInt(charge)}).")
    }

    val nextInputs = (state.inputsStockKg ?: emptyMap()).toMutableMap()
    if (useStock) nextInputs["defensivo"] = max(0.0, stockKg - 40.0)

    val nextCells = state.cells.map { c ->
        if (c.plotId == plotId) c.copy(weedLevel = 0.0, pestLevel = 0.0) else c
    }
    return state.copy(
        moneyBrl = state.moneyBrl - charge,
        cells = nextCells,
        inputsStockKg = nextInputs,
        ledger = state.ledger + LedgerEntry(state.day, "Controle Fitossanitário: ${plot.name}", -charge),
        statusAlert = "Pragas e invasoras zeradas em ${plot.name} (-R$ ${fmtBrlInt(charge)}).",
    )
}

fun applyContourProtectionInWorld(state: WorldState, plotId: Int): WorldState {
    val plot = state.plots.find { it.id == plotId } ?: return state
    val cost = (plot.areaHa * 280.0).roundToInt().toDouble()
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para terraceamento em curvas de nível (R$ ${fmtBrlInt(cost)}).")
    }
    val nextCells = state.cells.map { c ->
        if (c.plotId == plotId) c.copy(contourProtected = true, erosionLevel = 0.0) else c
    }
    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        cells = nextCells,
        ledger = state.ledger + LedgerEntry(state.day, "Curvas de Nível: ${plot.name}", -cost),
        statusAlert = "${plot.name} protegido com curvas de nível contra erosão (-R$ ${fmtBrlInt(cost)}).",
    )
}

fun buyInputStockInWorld(state: WorldState, inputId: String, kg: Double, totalCostBrl: Double): WorldState {
    if (state.moneyBrl < totalCostBrl) {
        return state.copy(statusAlert = "Saldo insuficiente para comprar $inputId (R$ ${fmtBrlInt(totalCostBrl)}).")
    }
    val nextInputs = (state.inputsStockKg ?: emptyMap()).toMutableMap()
    nextInputs[inputId] = (nextInputs[inputId] ?: 0.0) + kg
    val hist = PurchaseHistoryEntry(
        id = "in_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}",
        day = state.day,
        kind = "COMPRA",
        itemName = "Insumo: $inputId (${kg.roundToInt()} kg)",
        amountBrl = -totalCostBrl,
        details = "Estoque da fazenda",
    )
    return state.copy(
        moneyBrl = state.moneyBrl - totalCostBrl,
        inputsStockKg = nextInputs,
        purchaseHistory = (listOf(hist) + (state.purchaseHistory ?: emptyList())).take(60),
        ledger = state.ledger + LedgerEntry(state.day, "Compra de Insumo: $inputId (+${kg.roundToInt()} kg)", -totalCostBrl),
        statusAlert = "+${kg.roundToInt()} kg de $inputId estocado (-R$ ${fmtBrlInt(totalCostBrl)}).",
    )
}

fun sellPlotInWorld(state: WorldState, plotId: Int): WorldState {
    val plot = state.plots.find { it.id == plotId } ?: return state
    if (plot.isPreservation || !plot.owned) {
        return state.copy(statusAlert = "Somente terras próprias não-APP podem ser vendidas.")
    }
    val ownedCount = state.plots.count { it.owned && !it.isPreservation }
    if (ownedCount <= 1) {
        return state.copy(statusAlert = "Você precisa manter pelo menos 1 talhão próprio na fazenda.")
    }
    val saleBrl = max(15000.0, (plot.buyPriceBrl * 0.85).roundToInt().toDouble())
    val hist = PurchaseHistoryEntry(
        id = "sell_plot_${System.currentTimeMillis()}",
        day = state.day,
        kind = "VENDA",
        itemName = plot.name,
        amountBrl = saleBrl,
        details = "${plot.areaHa} ha vendidos",
    )
    return state.copy(
        moneyBrl = state.moneyBrl + saleBrl,
        plots = state.plots.map { if (it.id == plotId) it.copy(owned = false, leased = false) else it },
        purchaseHistory = (listOf(hist) + (state.purchaseHistory ?: emptyList())).take(60),
        ledger = state.ledger + LedgerEntry(state.day, "Venda de Terra: ${plot.name}", saleBrl),
        statusAlert = "${plot.name} vendido por R$ ${fmtBrlInt(saleBrl)}!",
    )
}

fun leasePlotWithTermInWorld(state: WorldState, plotId: Int, term: String): WorldState {
    val plot = state.plots.find { it.id == plotId } ?: return state
    if (plot.isPreservation || plot.owned) {
        return state.copy(statusAlert = "Área já é sua ou é Reserva Legal protegida.")
    }
    val termDays = when (term) {
        "MENSAL" -> 30
        "SAFRAL" -> 90
        "ANUAL" -> 180
        else -> 10
    }
    val upfront = (plot.leaseDailyBrl * min(termDays, 5)).roundToInt().toDouble()
    if (state.moneyBrl < upfront) {
        return state.copy(statusAlert = "Saldo insuficiente para entrada do arrendamento (R$ ${fmtBrlInt(upfront)}).")
    }
    val hist = PurchaseHistoryEntry(
        id = "lease_${System.currentTimeMillis()}",
        day = state.day,
        kind = "ARRENDAMENTO",
        itemName = "${plot.name} ($term)",
        amountBrl = -upfront,
        details = "Vigência até Dia ${state.day + termDays}",
    )
    return state.copy(
        moneyBrl = state.moneyBrl - upfront,
        plots = state.plots.map {
            if (it.id == plotId) {
                it.copy(
                    leased = true,
                    leaseTerm = term,
                    leaseStartDay = state.day,
                    leaseEndDay = state.day + termDays,
                )
            } else it
        },
        purchaseHistory = (listOf(hist) + (state.purchaseHistory ?: emptyList())).take(60),
        ledger = state.ledger + LedgerEntry(state.day, "Arrendamento ($term): ${plot.name}", -upfront),
        statusAlert = "${plot.name} arrendado ($term) até o Dia ${state.day + termDays}!",
    )
}

fun mergeAdjacentPlotsInWorld(state: WorldState, primaryPlotId: Int, secondaryPlotId: Int): WorldState {
    if (primaryPlotId == secondaryPlotId) return state
    val p1 = state.plots.find { it.id == primaryPlotId } ?: return state
    val p2 = state.plots.find { it.id == secondaryPlotId } ?: return state
    if ((!p1.owned && !p1.leased) || (!p2.owned && !p2.leased) || p1.isPreservation || p2.isPreservation) {
        return state.copy(statusAlert = "Ambos os talhões devem ser próprios ou arrendados para unificação.")
    }
    val cost = 4500.0
    if (state.moneyBrl < cost) {
        return state.copy(statusAlert = "Saldo insuficiente para remover cercas e unificar talhões (R$ 4.500).")
    }
    val nextCells = state.cells.map { c ->
        if (c.plotId == secondaryPlotId) c.copy(plotId = primaryPlotId) else c
    }
    val mergedPlots = state.plots.filterNot { it.id == secondaryPlotId }.map { p ->
        if (p.id == primaryPlotId) {
            p.copy(
                name = "${p1.name} + ${p2.name}",
                areaHa = ((p1.areaHa + p2.areaHa) * 100.0).roundToInt() / 100.0,
                fencesRemoved = true,
                mergedFromPlotIds = (p1.mergedFromPlotIds ?: emptyList()) + secondaryPlotId,
            )
        } else p
    }
    return state.copy(
        moneyBrl = state.moneyBrl - cost,
        plots = mergedPlots,
        cells = nextCells,
        ledger = state.ledger + LedgerEntry(state.day, "Unificação de Talhões: ${p1.id} + ${p2.id}", -cost),
        statusAlert = "Talhões ${p1.id} e ${p2.id} mesclados! Área total: ${((p1.areaHa + p2.areaHa) * 10).roundToInt() / 10.0} ha.",
    )
}

fun sellAnimalHeadInWorld(state: WorldState, species: AnimalSpecies, count: Int): WorldState {
    val herd = state.herds.find { it.species == species } ?: return state
    val sellCount = min(herd.count, max(1, count))
    if (sellCount <= 0 || herd.count <= 0) {
        return state.copy(statusAlert = "Sem animais disponíveis desta espécie para venda.")
    }
    val meta = ANIMAL_META[species] ?: ANIMAL_META.values.first()
    val revenue = (sellCount * meta.unitPriceBrl * 1.18 * (herd.healthPct / 100.0)).roundToInt().toDouble()
    return state.copy(
        moneyBrl = state.moneyBrl + revenue,
        herds = state.herds.map {
            if (it.species == species) it.copy(count = max(0, it.count - sellCount)) else it
        },
        ledger = state.ledger + LedgerEntry(state.day, "Venda Rebanho: $sellCount× ${meta.displayName}", revenue),
        statusAlert = "Vendido $sellCount cabeça(s) de ${meta.displayName}: +R$ ${fmtBrlInt(revenue)}!",
    )
}

fun hireEmployeeInWorld(state: WorldState, name: String, role: String, dailyWageBrl: Double): WorldState {
    val current = state.employees ?: emptyList()
    if (current.size >= 12) {
        return state.copy(statusAlert = "Limite máximo de 12 funcionários na folha da fazenda.")
    }
    val hireBonus = (dailyWageBrl * 2.0).roundToInt().toDouble()
    if (state.moneyBrl < hireBonus) {
        return state.copy(statusAlert = "Saldo insuficiente para adiantamento de contratação (R$ ${fmtBrlInt(hireBonus)}).")
    }
    val newEmp = FarmEmployee(
        uid = "emp_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}",
        name = name,
        role = role,
        dailyWageBrl = dailyWageBrl,
        skillEfficiency = 0.92 + (current.size % 4) * 0.05,
        hiredDay = state.day,
    )
    return evaluateAchievements(
        state.copy(
            moneyBrl = state.moneyBrl - hireBonus,
            employees = current + newEmp,
            ledger = state.ledger + LedgerEntry(state.day, "Contratação: $name ($role)", -hireBonus),
            statusAlert = "$name contratado para a equipe (${role})!",
        )
    )
}

fun assignEmployeeInWorld(
    state: WorldState,
    employeeUid: String,
    vehicleUid: String?,
    plotId: Int?,
    taskType: String,
): WorldState {
    val nextEmps = (state.employees ?: emptyList()).map { emp ->
        if (emp.uid == employeeUid) {
            emp.copy(
                assignedVehicleUid = vehicleUid,
                assignedPlotId = plotId,
                taskType = taskType,
            )
        } else emp
    }
    return evaluateAchievements(
        state.copy(
            employees = nextEmps,
            statusAlert = "Escala do funcionário atualizada ($taskType${plotId?.let { " • Talhão #$it" } ?: ""}).",
        )
    )
}

fun fireEmployeeInWorld(state: WorldState, employeeUid: String): WorldState {
    val target = (state.employees ?: emptyList()).find { it.uid == employeeUid } ?: return state
    return state.copy(
        employees = (state.employees ?: emptyList()).filterNot { it.uid == employeeUid },
        statusAlert = "${target.name} desligado da equipe.",
    )
}

fun placeAuctionBidInWorld(state: WorldState, lotUid: String, bidBrl: Double): WorldState {
    val lots = state.auctionLots ?: return state
    val lot = lots.find { it.uid == lotUid } ?: return state
    if (lot.settled) return state.copy(statusAlert = "Este lote de leilão já foi arrematado.")
    if (bidBrl <= lot.currentBidBrl) {
        return state.copy(statusAlert = "O lance precisa superar o atual (R$ ${fmtBrlInt(lot.currentBidBrl)}).")
    }
    if (state.moneyBrl < bidBrl) {
        return state.copy(statusAlert = "Saldo insuficiente para cobrir este lance (R$ ${fmtBrlInt(bidBrl)}).")
    }

    val def = getVehicleDef(lot.defId)
    val wonImmediately = bidBrl >= lot.baseValueBrl * 1.05
    if (wonImmediately) {
        val wonVeh = makeDefaultVehicle(
            uid = "veh_auc_${System.currentTimeMillis()}",
            defId = lot.defId,
            x = 8.0 + (state.vehicles.size % 4) * 4.5,
            z = 22.0,
            headingDeg = 180.0,
            fuelL = def.fuelCapacityL * 0.7,
            conservationPct = lot.conservationPct,
            hoursWorked = lot.hoursUsed.toDouble(),
        )
        return evaluateAchievements(
            state.copy(
                moneyBrl = state.moneyBrl - bidBrl,
                vehicles = state.vehicles + wonVeh,
                auctionLots = lots.map {
                    if (it.uid == lotUid) it.copy(currentBidBrl = bidBrl, highestBidder = "Você", settled = true) else it
                },
                ledger = state.ledger + LedgerEntry(state.day, "Arremate Leilão: ${def.brand} ${def.model}", -bidBrl),
                statusAlert = "Lote ARREMATADO! ${def.brand} ${def.model} entregue no pátio (-R$ ${fmtBrlInt(bidBrl)}).",
            )
        )
    }

    return state.copy(
        auctionLots = lots.map {
            if (it.uid == lotUid) it.copy(currentBidBrl = bidBrl, highestBidder = "Você") else it
        },
        statusAlert = "Lance registrado em ${def.model}: R$ ${fmtBrlInt(bidBrl)}!",
    )
}

fun acceptOrCompleteContractInWorld(state: WorldState, contractUid: String): WorldState {
    val ctr = state.contracts.find { it.uid == contractUid } ?: return state
    if (ctr.completed) return state

    if (!ctr.accepted) {
        val extraVehicles = if (ctr.providesLoanerMachine == true && ctr.loanerVehicleDefId != null) {
            val lDef = getVehicleDef(ctr.loanerVehicleDefId)
            val loanVeh = makeDefaultVehicle(
                uid = "loan_${ctr.uid}",
                defId = lDef.id,
                x = -6.0,
                z = -6.0,
                headingDeg = 0.0,
                fuelL = lDef.fuelCapacityL,
                selectedCropId = ctr.cropOrProductId,
            )
            if (state.vehicles.none { it.uid == loanVeh.uid }) state.vehicles + loanVeh else state.vehicles
        } else state.vehicles

        return state.copy(
            contracts = state.contracts.map { if (it.uid == contractUid) it.copy(accepted = true) else it },
            vehicles = extraVehicles,
            statusAlert = "Contrato aceito: ${ctr.title}!",
        )
    }

    // Check completion or deliver from inventory
    val currentDone = ctr.completedUnits
    val needed = max(0.0, ctr.targetUnits - currentDone)
    var inv = state.inventoryKg
    var finalDone = currentDone
    if (needed > 0.0 && (inv[ctr.cropOrProductId] ?: 0.0) >= needed) {
        val nextInv = inv.toMutableMap()
        val rem = (nextInv[ctr.cropOrProductId] ?: 0.0) - needed
        if (rem <= 0.1) nextInv.remove(ctr.cropOrProductId) else nextInv[ctr.cropOrProductId] = rem
        inv = nextInv
        finalDone = ctr.targetUnits
    }

    if (finalDone + 0.5 < ctr.targetUnits) {
        return state.copy(
            statusAlert = "Progresso insuficiente (${finalDone.roundToInt()}/${ctr.targetUnits.roundToInt()}). Conclua no campo ou entregue do estoque."
        )
    }

    return evaluateAchievements(
        state.copy(
            moneyBrl = state.moneyBrl + ctr.rewardBrl,
            inventoryKg = inv,
            vehicles = state.vehicles.filterNot { it.uid == "loan_${ctr.uid}" },
            contracts = state.contracts.map {
                if (it.uid == contractUid) it.copy(completed = true, completedUnits = ctr.targetUnits) else it
            },
            ledger = state.ledger + LedgerEntry(state.day, "Contrato Concluído: ${ctr.title}", ctr.rewardBrl),
            statusAlert = "Contrato CONCLUÍDO! Prêmio +R$ ${fmtBrlInt(ctr.rewardBrl)}.",
        )
    )
}

data class ProfitAndLossBreakdown(
    val grainAndCropSalesBrl: Double,
    val livestockAndDairySalesBrl: Double,
    val industryAndContractSalesBrl: Double,
    val creditReceivedBrl: Double,
    val inputsAndFuelCostBrl: Double,
    val maintenanceAndWorkersBrl: Double,
    val leasingAndLandCostBrl: Double,
    val machineryCapexBrl: Double,
)

data class ProfitAndLossSummary(
    val totalRevenueBrl: Double,
    val totalOperatingExpenseBrl: Double,
    val totalCapexBrl: Double,
    val netOperatingProfitBrl: Double,
    val breakdown: ProfitAndLossBreakdown,
    val assetValuationBrl: Double,
    val netWorthBrl: Double,
)

fun computeProfitAndLossReport(state: WorldState): ProfitAndLossSummary {
    var grainAndCropSalesBrl = 0.0
    var livestockAndDairySalesBrl = 0.0
    var industryAndContractSalesBrl = 0.0
    var creditReceivedBrl = 0.0
    var inputsAndFuelCostBrl = 0.0
    var maintenanceAndWorkersBrl = 0.0
    var leasingAndLandCostBrl = 0.0
    var machineryCapexBrl = 0.0

    for (entry in state.ledger) {
        val desc = entry.description.lowercase()
        val amt = entry.amountBrl
        if (amt >= 0) {
            when {
                desc.contains("crédito") || desc.contains("empréstimo") || desc.contains("capital inicial") -> creditReceivedBrl += amt
                desc.contains("rebanho") || desc.contains("leite") || desc.contains("ovos") || desc.contains("suíno") -> livestockAndDairySalesBrl += amt
                desc.contains("indústria") || desc.contains("contrato") || desc.contains("benefici") -> industryAndContractSalesBrl += amt
                else -> grainAndCropSalesBrl += amt
            }
        } else {
            val absAmt = abs(amt)
            when {
                desc.contains("máquina") || desc.contains("implemento") || desc.contains("leilão") || desc.contains("constru") -> machineryCapexBrl += absAmt
                desc.contains("arrend") || desc.contains("terra") || desc.contains("juros") -> leasingAndLandCostBrl += absAmt
                desc.contains("oficina") || desc.contains("reparo") || desc.contains("preventiva") || desc.contains("lavagem") || desc.contains("guincho") || desc.contains("funcion") -> maintenanceAndWorkersBrl += absAmt
                else -> inputsAndFuelCostBrl += absAmt
            }
        }
    }

    val totalRevenue = grainAndCropSalesBrl + livestockAndDairySalesBrl + industryAndContractSalesBrl
    val totalOpex = inputsAndFuelCostBrl + maintenanceAndWorkersBrl + leasingAndLandCostBrl

    val fleetVal = state.vehicles.sumOf { v ->
        getVehicleDef(v.defId).priceBrl * 0.65 * (0.4 + 0.6 * (v.conservationPct / 100.0))
    }
    val landVal = state.plots.filter { it.owned && !it.isPreservation }.sumOf { it.areaHa * 35000.0 }
    val assetValuation = (fleetVal + landVal).roundToInt().toDouble()

    return ProfitAndLossSummary(
        totalRevenueBrl = totalRevenue,
        totalOperatingExpenseBrl = totalOpex,
        totalCapexBrl = machineryCapexBrl,
        netOperatingProfitBrl = totalRevenue - totalOpex,
        breakdown = ProfitAndLossBreakdown(
            grainAndCropSalesBrl = grainAndCropSalesBrl,
            livestockAndDairySalesBrl = livestockAndDairySalesBrl,
            industryAndContractSalesBrl = industryAndContractSalesBrl,
            creditReceivedBrl = creditReceivedBrl,
            inputsAndFuelCostBrl = inputsAndFuelCostBrl,
            maintenanceAndWorkersBrl = maintenanceAndWorkersBrl,
            leasingAndLandCostBrl = leasingAndLandCostBrl,
            machineryCapexBrl = machineryCapexBrl,
        ),
        assetValuationBrl = assetValuation,
        netWorthBrl = state.moneyBrl + assetValuation - state.bankLoanBrl,
    )
}

data class AssistantTip(
    val severity: String, // info | warning | critical
    val title: String,
    val detail: String,
)

fun buildFieldAssistantAdvice(state: WorldState): List<AssistantTip> {
    val tips = mutableListOf<AssistantTip>()
    val readyCount = state.cells.count { it.soilState == SoilState.PRONTO_COLHEITA }
    if (readyCount > 15) {
        tips.add(AssistantTip("critical", "Safra Madura no Campo", "Existem $readyCount setores prontos para colheita. Acione a colheitadeira ou contrate terceirização!"))
    }
    val lowFuelVeh = state.vehicles.find { v -> v.fuelL < getVehicleDef(v.defId).fuelCapacityL * 0.18 }
    if (lowFuelVeh != null) {
        val def = getVehicleDef(lowFuelVeh.defId)
        tips.add(AssistantTip("warning", "Diesel Baixo", "${def.model} está na reserva (${lowFuelVeh.fuelL.roundToInt()} L). Abasteça no Posto ou bomba da Sede."))
    }
    val wornVeh = state.vehicles.find { it.conservationPct < 55.0 }
    if (wornVeh != null) {
        val def = getVehicleDef(wornVeh.defId)
        tips.add(AssistantTip("warning", "Revisão na Oficina", "${def.model} está com ${wornVeh.conservationPct.roundToInt()}% de conservação. Faça revisão preventiva."))
    }
    val acidCell = state.cells.find { it.plotId > 0 && (it.phLevel ?: 6.2) < 5.6 }
    if (acidCell != null) {
        tips.add(AssistantTip("info", "Correção de Solo (pH)", "O Talhão #${acidCell.plotId} está com pH ácido. Aplique calcário na aba Agronomia."))
    }
    if (tips.isEmpty()) {
        tips.add(AssistantTip("info", "Fazenda em Ordem", "Operação estável! Monitore preços da Cooperativa e avance o dia quando concluir o manejo."))
    }
    return tips
}
