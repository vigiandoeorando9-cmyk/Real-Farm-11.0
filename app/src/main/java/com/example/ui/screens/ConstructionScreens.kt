package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.AddHomeWork
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.BuildableStructureDef
import com.example.domain.CONSTRUCTION_CATALOG
import com.example.domain.ConstructionCategory
import com.example.domain.HOUSE_CUSTOMIZATION_PRESETS
import com.example.domain.LAND_UNIT_META
import com.example.domain.LandUnit
import com.example.domain.PropertyEvolutionTier
import com.example.domain.WorldState
import com.example.domain.formatLandArea
import com.example.domain.getRecommendedStructuresForMap
import com.example.domain.resolveBuildingVisualProfile
import com.example.domain.resolveRegionalArchStyle
import com.example.simulation.ConstructionPlacementPreview
import com.example.simulation.evaluatePropertyEvolution
import com.example.simulation.formatBrl
import com.example.state.FarmViewModel
import com.example.ui.components.SectionCard
import com.example.ui.components.StatBadge
import com.example.ui.theme.AgroSprout
import com.example.ui.theme.CabinCard
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusWarning
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConstrucaoPropriedadePanel(
    viewModel: FarmViewModel,
    worldState: WorldState,
) {
    var subMode by remember { mutableIntStateOf(0) } // 0 = CONSTRUIR, 1 = Evolução & Casas, 2 = Terras & Talhões
    var selectedCategory by remember { mutableStateOf(ConstructionCategory.CASAS) }
    var replaceHouseUid by remember { mutableStateOf<String?>(null) }
    var selectedPlotA by remember { mutableStateOf<Int?>(null) }

    val regionalArch = remember(worldState.mapId) { resolveRegionalArchStyle(worldState.mapId) }
    val evolution = remember(worldState.buildings, worldState.plots, worldState.mapId) {
        evaluatePropertyEvolution(worldState)
    }
    val orderedCatalog = remember(worldState.mapId, selectedCategory) {
        getRecommendedStructuresForMap(worldState.mapId).filter { it.category == selectedCategory }
    }
    val ownedHouses = remember(worldState.buildings, worldState.mapId) {
        worldState.buildings.filter { b ->
            val p = resolveBuildingVisualProfile(b, worldState.mapId)
            p.isHouse && !b.uid.startsWith("bld_vila_")
        }
    }
    val activeUnit = worldState.landUnit ?: LandUnit.HECTARE

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Segmented Bar: CONSTRUIR | Evolução & Casas | Terras & Talhões
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = subMode == 0,
                onClick = { subMode = 0 },
                leadingIcon = { Icon(Icons.Rounded.Construction, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("CONSTRUIR (9 Categorias)", fontWeight = FontWeight.Bold) },
                modifier = Modifier.height(48.dp).testTag("submode_construir"),
            )
            FilterChip(
                selected = subMode == 1,
                onClick = { subMode = 1 },
                leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Evolução & Casas (${ownedHouses.size})") },
                modifier = Modifier.height(48.dp).testTag("submode_casas_evolucao"),
            )
            FilterChip(
                selected = subMode == 2,
                onClick = { subMode = 2 },
                leadingIcon = { Icon(Icons.Rounded.Landscape, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Terras & Talhões (${worldState.plots.size})") },
                modifier = Modifier.height(48.dp).testTag("submode_terras"),
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
                .testTag("construction_catalog_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            when (subMode) {
                0 -> {
                    item {
                        SectionCard(
                            title = "Arquitetura da Região: ${regionalArch.regionTitle}",
                            subtitle = regionalArch.architectureNotes,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StatBadge(evolution.currentTier.title, AgroSprout)
                                StatBadge("${CONSTRUCTION_CATALOG.size} modelos 3D", HarvestGold)
                            }
                            if (replaceHouseUid != null) {
                                Surface(
                                    color = StatusWarning.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, StatusWarning),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "Modo Substituição de Casa ativo: a antiga só sai após confirmar a nova no terreno.",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                        OutlinedButton(onClick = { replaceHouseUid = null }) {
                                            Text("Cancelar")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 9 Categories selector
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Categorias de Construção (9)", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                ConstructionCategory.entries.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("cat_${cat.name.lowercase()}"),
                                    )
                                }
                            }
                            Text(
                                text = selectedCategory.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(orderedCatalog, key = { it.modelId }) { def ->
                        val isRecommended = regionalArch in def.preferredRegions
                        val canAfford = worldState.moneyBrl >= def.priceBrl
                        SectionCard(
                            title = def.name,
                            subtitle = "${def.widthM}m × ${def.depthM}m • Alt ${def.heightM}m • Material: ${def.materialStyle}",
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (isRecommended) StatBadge("Típica da Região", StatusGreen)
                                if (def.hasVeranda) StatBadge("Com Varanda", StatusInfo)
                                if (def.hasGarage) StatBadge("Com Garagem", HarvestGold)
                                if (def.isOpenBay) StatBadge("Acesso Veículos", AgroSprout)
                                if (def.isWalkableSurface) StatBadge("Piso Trafegável", AgroSprout)
                            }
                            Text(def.description, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "Espaço: ${def.spaceRequirementNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = formatBrl(def.priceBrl),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (canAfford) HarvestGold else StatusDanger,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (!canAfford) {
                                        Text("Saldo insuficiente", style = MaterialTheme.typography.labelSmall, color = StatusDanger)
                                    }
                                }
                                Button(
                                    onClick = {
                                        val rUid = if (def.isHouse) replaceHouseUid else null
                                        replaceHouseUid = null
                                        viewModel.startConstructionPlacement(def, replaceHouseUid = rUid)
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("construct_item_${def.modelId}"),
                                ) {
                                    Icon(Icons.Rounded.AddHomeWork, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (replaceHouseUid != null && def.isHouse) "Substituir no 3D" else "Posicionar 3D")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        val nextScore = evolution.nextTier?.targetScore ?: PropertyEvolutionTier.FAZENDA_DESENVOLVIDA.targetScore
                        val progress = if (evolution.nextTier == null) 1f else {
                            (evolution.score.toFloat() / nextScore.coerceAtLeast(1)).coerceIn(0f, 1f)
                        }
                        SectionCard(
                            title = "Evolução da Propriedade (4 Níveis)",
                            subtitle = evolution.currentTier.title,
                        ) {
                            Text(evolution.currentTier.subtitle, style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = AgroSprout,
                            )
                            Text(
                                text = "Pontuação rural: ${evolution.score} pts • Casas: ${evolution.housesCount} • Galpões/Garagens: ${evolution.shedsAndGaragesCount} • Silos/Celeiros: ${evolution.silosAndBarnsCount} • Talhões próprios: ${evolution.ownedPlotsCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = HarvestGold,
                            )
                            evolution.checklist.forEach { (itemLabel, done) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = if (done) StatusGreen else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(itemLabel, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(
                                text = "Nenhuma casa ou construção existente é alterada sozinha sem sua escolha e confirmação.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Suas Casas & Personalização (${ownedHouses.size})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    items(ownedHouses, key = { it.uid }) { house ->
                        val profile = resolveBuildingVisualProfile(house, worldState.mapId)
                        SectionCard(
                            title = profile.displayLabel,
                            subtitle = "${"%.1f".format(profile.widthM)}m × ${"%.1f".format(profile.depthM)}m • Material: ${profile.materialStyle} • Região: ${profile.archStyle.regionTitle}",
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatBadge(if (profile.hasVeranda) "Varanda Ativa" else "Sem Varanda", if (profile.hasVeranda) StatusGreen else MaterialTheme.colorScheme.outline)
                                StatBadge(if (profile.hasGarage) "Garagem Anexa" else "Sem Garagem", if (profile.hasGarage) HarvestGold else MaterialTheme.colorScheme.outline)
                            }
                            Text("Cores & Estilo (Reforma: R$ 3.500):", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                HOUSE_CUSTOMIZATION_PRESETS.forEach { preset ->
                                    val swatchCol = Color(android.graphics.Color.parseColor(preset.wallHex))
                                    FilterChip(
                                        selected = profile.wallColorHex.equals(preset.wallHex, ignoreCase = true) && profile.materialStyle == preset.materialStyle,
                                        onClick = { viewModel.customizeOwnedHouse(house.uid, presetId = preset.id) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(swatchCol)
                                            )
                                        },
                                        label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("house_preset_${preset.id}"),
                                    )
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (!profile.hasVeranda) {
                                    FilledTonalButton(
                                        onClick = { viewModel.customizeOwnedHouse(house.uid, addVeranda = true) },
                                        modifier = Modifier.height(48.dp).testTag("house_add_veranda_btn"),
                                    ) {
                                        Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("+ Varanda (R$ 12.000)")
                                    }
                                }
                                if (!profile.hasGarage) {
                                    FilledTonalButton(
                                        onClick = { viewModel.customizeOwnedHouse(house.uid, addGarage = true) },
                                        modifier = Modifier.height(48.dp).testTag("house_add_garage_btn"),
                                    ) {
                                        Icon(Icons.Rounded.AddHomeWork, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("+ Garagem (R$ 18.500)")
                                    }
                                }
                                Button(
                                    onClick = {
                                        replaceHouseUid = house.uid
                                        selectedCategory = ConstructionCategory.CASAS
                                        subMode = 0
                                    },
                                    modifier = Modifier.height(48.dp).testTag("replace_house_${house.uid}"),
                                ) {
                                    Text("Trocar por Novo Modelo")
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        SectionCard(title = "Unidade de Medida Agrária") {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LandUnit.entries.forEach { u ->
                                    FilterChip(
                                        selected = activeUnit == u,
                                        onClick = { viewModel.setLandUnit(u) },
                                        label = { Text((LAND_UNIT_META[u] ?: LAND_UNIT_META[LandUnit.HECTARE]!!).label) },
                                    )
                                }
                            }
                        }
                    }
                    items(worldState.plots.take(28), key = { it.id }) { plot ->
                        SectionCard(
                            title = "${plot.name} (${formatLandArea(plot.areaHa, activeUnit)})",
                            subtitle = when {
                                plot.isPreservation -> "Reserva Legal Protegida (APP)"
                                plot.owned -> "Próprio"
                                plot.leased -> "Arrendado (${plot.leaseTerm ?: "Ativo"} até Dia ${plot.leaseEndDay ?: "-"})"
                                else -> "Compra ${formatBrl(plot.buyPriceBrl)} • Arrend. ${formatBrl(plot.leaseDailyBrl)}/dia"
                            },
                        ) {
                            if (!plot.isPreservation) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!plot.owned) {
                                        Button(onClick = { viewModel.buyPlot(plot.id) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Comprar") }
                                        FilledTonalButton(onClick = { viewModel.leasePlotWithTerm(plot.id, "SAFRAL") }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Arrendar") }
                                    } else {
                                        OutlinedButton(onClick = { viewModel.sellPlot(plot.id) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Vender 85%") }
                                        OutlinedButton(onClick = {
                                            if (selectedPlotA == null) selectedPlotA = plot.id
                                            else {
                                                viewModel.mergeAdjacentPlots(selectedPlotA!!, plot.id)
                                                selectedPlotA = null
                                            }
                                        }, modifier = Modifier.weight(1f).height(48.dp)) {
                                            Text(if (selectedPlotA == plot.id) "Selecione 2º..." else if (selectedPlotA != null) "Unificar aqui" else "Unificar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConstructionPlacementHudOverlay(
    preview: ConstructionPlacementPreview,
    moneyBrl: Double,
    onMove: (Double, Double) -> Unit,
    onRotate: (Double) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canConfirm = preview.isValid && preview.canAfford && moneyBrl >= preview.structure.priceBrl
    val statusColor = if (canConfirm) StatusGreen else StatusDanger
    val statusText = when {
        !preview.isValid -> preview.invalidReason ?: "Local bloqueado para construção."
        !preview.canAfford || moneyBrl < preview.structure.priceBrl ->
            "Saldo insuficiente (${formatBrl(preview.structure.priceBrl)})"
        else -> "Posição válida • ${preview.structure.widthM}m × ${preview.structure.depthM}m • Ângulo ${preview.rotationDeg.roundToInt()}°"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(12.dp),
    ) {
        // Compact Top Status Banner for Construction Preview
        Surface(
            color = CabinCard.copy(alpha = 0.94f),
            border = BorderStroke(1.5.dp, statusColor),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .testTag("construction_placement_hud"),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${preview.structure.name} — ${formatBrl(preview.structure.priceBrl)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = HarvestGold,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Ergonomic Bottom Construction Controls (Positioning, Rotation, Confirm & Cancel)
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Move Nudges (2.0m steps)
                FilledTonalButton(onClick = { onMove(-2.0, 0.0) }, modifier = Modifier.height(48.dp).testTag("construction_move_west_btn")) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Mover Oeste", modifier = Modifier.size(18.dp))
                }
                FilledTonalButton(onClick = { onMove(0.0, -2.0) }, modifier = Modifier.height(48.dp).testTag("construction_move_north_btn")) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Mover Norte", modifier = Modifier.size(18.dp))
                }
                FilledTonalButton(onClick = { onMove(0.0, 2.0) }, modifier = Modifier.height(48.dp).testTag("construction_move_south_btn")) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Mover Sul", modifier = Modifier.size(18.dp))
                }
                FilledTonalButton(onClick = { onMove(2.0, 0.0) }, modifier = Modifier.height(48.dp).testTag("construction_move_east_btn")) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Mover Leste", modifier = Modifier.size(18.dp))
                }

                // Rotate Controls (-45 / -15 / +15 / +45)
                OutlinedButton(onClick = { onRotate(-45.0) }, modifier = Modifier.height(48.dp).testTag("construction_rotate_left_btn")) {
                    Icon(Icons.AutoMirrored.Rounded.RotateLeft, contentDescription = "Girar esquerda", modifier = Modifier.size(18.dp))
                    Text("45°", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = { onRotate(15.0) }, modifier = Modifier.height(48.dp).testTag("construction_rotate_fine_btn")) {
                    Text("+15°", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = { onRotate(45.0) }, modifier = Modifier.height(48.dp).testTag("construction_rotate_right_btn")) {
                    Text("45°", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = "Girar direita", modifier = Modifier.size(18.dp))
                }

                // Cancel (free, zero deduction)
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.height(48.dp).testTag("construction_cancel_btn"),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancelar obra", tint = StatusDanger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cancelar")
                }

                // Confirm (deducts price only here)
                Button(
                    onClick = onConfirm,
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White),
                    modifier = Modifier.height(48.dp).testTag("construction_confirm_btn"),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Confirmar construção", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Confirmar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
