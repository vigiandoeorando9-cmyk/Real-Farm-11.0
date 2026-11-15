package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.ACHIEVEMENT_CATALOG
import com.example.domain.ALL_CROPS
import com.example.domain.ANIMAL_META
import com.example.domain.AnimalSpecies
import com.example.domain.CHARACTER_PRESETS
import com.example.domain.CONSERVATION_TIER_META
import com.example.domain.ESTABLISHMENTS
import com.example.domain.INDUSTRY_CHAINS
import com.example.domain.OUTSOURCING_SERVICE_META
import com.example.domain.WORKSHOP_COMPONENT_LABELS
import com.example.domain.WorkshopComponent
import com.example.domain.WorldState
import com.example.domain.conservationTierFromPct
import com.example.domain.formatLandArea
import com.example.domain.getAvailableImplements
import com.example.domain.getAvailableVehicles
import com.example.domain.getCropById
import com.example.domain.getImplementDef
import com.example.domain.getVehicleDef
import com.example.simulation.HireOutsourcingParams
import com.example.simulation.calculateOutsourcingQuote
import com.example.simulation.computeProfitAndLossReport
import com.example.simulation.formatBrl
import com.example.state.FarmViewModel
import com.example.state.IN_GAME_TAB_TITLES
import com.example.state.InGameTab
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

import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import com.example.domain.AppRoute
import com.example.domain.CAMERA_MODE_LABELS
import com.example.domain.CameraMode
import com.example.domain.ConservationTier
import com.example.domain.MANUAL_TOOL_LABELS
import com.example.domain.ManualTool
import com.example.domain.SteeringControlStyle
import com.example.domain.VehicleCategory
import com.example.domain.WEATHER_META
import com.example.domain.WeatherType
import com.example.simulation.buildFieldAssistantAdvice

@Composable
fun InGamePanelsSheet(
    viewModel: FarmViewModel,
    tab: InGameTab,
    worldState: WorldState,
    cameraMode: CameraMode = CameraMode.TERCEIRA_PESSOA,
    timeScale: Double = 1.0,
    hudHidden: Boolean = false,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                // Header
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (tab != InGameTab.MENU) {
                                IconButton(
                                    onClick = { viewModel.openInGameTab(InGameTab.MENU) },
                                    modifier = Modifier.size(48.dp).testTag("panel_back_menu_btn"),
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar ao Menu")
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = IN_GAME_TAB_TITLES[tab] ?: tab.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Text(
                                    text = "${worldState.farmName} • Caixa ${formatBrl(worldState.moneyBrl)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HarvestGold,
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(48.dp).testTag("panel_close_btn"),
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Fechar painel")
                            }
                        }
                        // Tab switcher bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            InGameTab.entries.forEach { t ->
                                FilterChip(
                                    selected = t == tab,
                                    onClick = { viewModel.openInGameTab(t) },
                                    label = { Text(IN_GAME_TAB_TITLES[t] ?: t.name, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Body
                Box(modifier = Modifier.weight(1f)) {
                    when (tab) {
                        InGameTab.MENU -> MenuHubTabContent(viewModel, worldState, cameraMode, timeScale, onDismiss)
                        InGameTab.FROTA -> FrotaTabContent(viewModel, worldState)
                        InGameTab.OFICINA -> OficinaTabContent(viewModel, worldState)
                        InGameTab.LOJA -> LojaTabContent(viewModel, worldState)
                        InGameTab.TERCEIRIZACAO -> TerceirizacaoTabContent(viewModel, worldState)
                        InGameTab.ECONOMIA -> EconomiaTabContent(viewModel, worldState)
                        InGameTab.INDUSTRIAS -> IndustriasTabContent(viewModel, worldState)
                        InGameTab.PECUARIA -> PecuariaTabContent(viewModel, worldState)
                        InGameTab.CONSTRUCAO -> ConstrucaoPropriedadePanel(viewModel, worldState)
                        InGameTab.EQUIPE -> EquipeSoloTabContent(viewModel, worldState)
                        InGameTab.CALENDARIO -> CalendarioTabContent(worldState)
                        InGameTab.CONTRATOS -> ContratosDreTabContent(viewModel, worldState)
                        InGameTab.CONFIGURACOES -> ConfiguracoesPartidaTabContent(viewModel, worldState, cameraMode, timeScale, hudHidden, onDismiss)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuHubTabContent(
    viewModel: FarmViewModel,
    worldState: WorldState,
    cameraMode: CameraMode,
    timeScale: Double,
    onDismiss: () -> Unit,
) {
    val activeVeh = worldState.activeVehicleUid?.let { uid -> worldState.vehicles.find { it.uid == uid } }
    val attachedImp = activeVeh?.attachedImplementUid?.let { uid -> worldState.implements.find { it.uid == uid } }
    val tips = remember(worldState.day, worldState.vehicles.size) { buildFieldAssistantAdvice(worldState) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                title = "Navegação Principal",
                subtitle = "Todos os sistemas da fazenda organizados em um único menu.",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MenuCategoryButton("Loja", Icons.Rounded.Storefront, "menu_item_loja") { viewModel.openInGameTab(InGameTab.LOJA) }
                    MenuCategoryButton("Banco", Icons.Rounded.AccountBalance, "menu_item_banco") { viewModel.openInGameTab(InGameTab.ECONOMIA) }
                    MenuCategoryButton("Garagem", Icons.Rounded.Garage, "menu_item_garagem") { viewModel.openInGameTab(InGameTab.FROTA) }
                    MenuCategoryButton("Propriedades", Icons.Rounded.HomeWork, "menu_item_propriedades") { viewModel.openInGameTab(InGameTab.CONSTRUCAO) }
                    MenuCategoryButton("Missões", Icons.Rounded.Assignment, "menu_item_missoes") { viewModel.openInGameTab(InGameTab.CONTRATOS) }
                    MenuCategoryButton("Configurações", Icons.Rounded.Settings, "menu_item_configuracoes") { viewModel.openInGameTab(InGameTab.CONFIGURACOES) }
                }
            }
        }

        item {
            SectionCard(title = "Outros Sistemas da Fazenda") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MenuCategoryButton("Oficina", Icons.Rounded.Build, "menu_item_oficina") { viewModel.openInGameTab(InGameTab.OFICINA) }
                    MenuCategoryButton("Indústrias", Icons.Rounded.Factory, "menu_item_industrias") { viewModel.openInGameTab(InGameTab.INDUSTRIAS) }
                    MenuCategoryButton("Pecuária", Icons.Rounded.Pets, "menu_item_pecuaria") { viewModel.openInGameTab(InGameTab.PECUARIA) }
                    MenuCategoryButton("Terceirização", Icons.Rounded.Agriculture, "menu_item_terceirizacao") { viewModel.openInGameTab(InGameTab.TERCEIRIZACAO) }
                    MenuCategoryButton("Equipe & Solo", Icons.Rounded.Groups, "menu_item_equipe") { viewModel.openInGameTab(InGameTab.EQUIPE) }
                    MenuCategoryButton("Calendário", Icons.Rounded.CalendarMonth, "menu_item_calendario") { viewModel.openInGameTab(InGameTab.CALENDARIO) }
                }
            }
        }

        if (activeVeh != null) {
            val def = getVehicleDef(activeVeh.defId)
            val gearStr = when {
                activeVeh.gear > 0 -> "F${activeVeh.gear}"
                activeVeh.gear < 0 -> "R"
                else -> "N"
            }
            val transLabel = activeVeh.transmissionMode ?: "SYNCHRO"
            val ptoText = if (activeVeh.isPtoEngaged == true) " • TDP ${activeVeh.ptoRpm?.roundToInt() ?: 540}" else ""
            item {
                SectionCard(
                    title = "Comandos da Máquina (${def.brand} ${def.model})",
                    subtitle = "$gearStr ($transLabel) • ${activeVeh.rpm.roundToInt()} RPM • Carga ${(activeVeh.engineLoadPct ?: 0.0).roundToInt()}%$ptoText",
                ) {
                    attachedImp?.compatibilityWarning?.let { warn ->
                        StatBadge("Aviso: $warn", StatusWarning)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(onClick = { viewModel.toggleHitch() }, modifier = Modifier.height(48.dp)) {
                            Text(if (attachedImp != null) "Soltar Implem." else "Engatar Implem.")
                        }
                        if (attachedImp != null) {
                            val impDef = getImplementDef(attachedImp.defId)
                            FilledTonalButton(onClick = { viewModel.toggleLowerImp() }, modifier = Modifier.height(48.dp)) {
                                Text(if (attachedImp.isLowered) "Erguer Implem." else "Baixar Implem.")
                            }
                            if (com.example.simulation.isPtoDrivenImplement(impDef.category)) {
                                FilledTonalButton(onClick = { viewModel.togglePto() }, modifier = Modifier.height(48.dp)) {
                                    Text(if (activeVeh.isPtoEngaged == true) "TDP: ON" else "TDP: OFF")
                                }
                            }
                            if (impDef.category == com.example.domain.ImplementCategory.CARRETA && attachedImp.fillL > 5.0) {
                                FilledTonalButton(onClick = { viewModel.unloadAttachedTrailer() }, modifier = Modifier.height(48.dp)) {
                                    Text("Descarregar Carreta")
                                }
                            } else {
                                FilledTonalButton(onClick = { viewModel.refillAttachedImplement() }, modifier = Modifier.height(48.dp)) {
                                    Text("Reabast. Implem.")
                                }
                            }
                        }
                        if (def.hasGrainTank || def.category == VehicleCategory.COLHEITADEIRA) {
                            FilledTonalButton(onClick = { viewModel.toggleHarvesterHeader() }, modifier = Modifier.height(48.dp)) {
                                Text(if (activeVeh.headerLowered == true) "Erguer Plat." else "Baixar Plat.")
                            }
                            FilledTonalButton(onClick = { viewModel.unloadGrain() }, modifier = Modifier.height(48.dp)) {
                                Text("Descarregar Grãos")
                            }
                        }
                        if (def.category == VehicleCategory.PULVERIZADOR_AUTOPROPELIDO) {
                            FilledTonalButton(onClick = { viewModel.toggleSprayer() }, modifier = Modifier.height(48.dp)) {
                                Text(if (activeVeh.isSprayerActive == true) "Desligar Barra" else "Pulverizar")
                            }
                            FilledTonalButton(onClick = { viewModel.refillSprayer() }, modifier = Modifier.height(48.dp)) {
                                Text("Encher Calda")
                            }
                        }
                        OutlinedButton(onClick = { viewModel.toggle4wd() }, modifier = Modifier.height(48.dp)) {
                            Text(if (activeVeh.is4wdEngaged == true) "4x4: ON" else "4x4: OFF")
                        }
                        OutlinedButton(onClick = { viewModel.toggleFieldOpenMode() }, modifier = Modifier.height(48.dp)) {
                            Text(if (activeVeh.fieldCreationMode) "Abrir Campo: ON" else "Abrir Campo")
                        }
                        OutlinedButton(onClick = { viewModel.refuelActiveVehicle() }, modifier = Modifier.height(48.dp)) {
                            Text("Abastecer Diesel")
                        }
                        OutlinedButton(onClick = { viewModel.toggleHeadlights() }, modifier = Modifier.height(48.dp)) {
                            Text(if (activeVeh.headlightsOn) "Farol: ON" else "Farol: OFF")
                        }
                        OutlinedButton(onClick = { viewModel.markGpsA() }, modifier = Modifier.height(48.dp)) { Text("GPS A") }
                        OutlinedButton(onClick = { viewModel.markGpsB() }, modifier = Modifier.height(48.dp)) { Text("GPS B") }
                        OutlinedButton(onClick = { viewModel.toggleGpsAuto() }, modifier = Modifier.height(48.dp)) {
                            Text(if (activeVeh.gpsAutoSteer) "Piloto GPS: ON" else "Piloto GPS")
                        }
                        OutlinedButton(onClick = { viewModel.engageTowRescue() }, modifier = Modifier.height(48.dp)) { Text("Guincho / Resgate") }
                        Button(onClick = {
                            viewModel.toggleEnterExit()
                            onDismiss()
                        }, modifier = Modifier.height(48.dp)) {
                            Text("Sair do Veículo")
                        }
                    }
                }
            }
        } else {
            item {
                SectionCard(
                    title = "Ferramenta Manual do Produtor",
                    subtitle = "Usada pelo botão de Interação quando a pé.",
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ManualTool.entries.forEach { tool ->
                            FilterChip(
                                selected = worldState.player.tool == tool,
                                onClick = { viewModel.selectManualTool(tool) },
                                label = { Text(MANUAL_TOOL_LABELS[tool] ?: tool.name) },
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Atalhos Rápidos & Câmera (${CAMERA_MODE_LABELS[cameraMode] ?: cameraMode.name} • ${timeScale.roundToInt()}x)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.cycleCameraMode() },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Trocar Câmera")
                    }
                    Button(
                        onClick = { viewModel.saveCurrentCareer() },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Salvar Jogo")
                    }
                }
            }
        }

        if (tips.isNotEmpty()) {
            item {
                SectionCard(title = "Assistente Agronômico") {
                    tips.take(3).forEach { tip ->
                        Text("• ${tip.title}: ${tip.detail}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfiguracoesPartidaTabContent(
    viewModel: FarmViewModel,
    worldState: WorldState,
    cameraMode: CameraMode,
    timeScale: Double,
    hudHidden: Boolean,
    onDismiss: () -> Unit,
) {
    val weatherLabel = (WEATHER_META[worldState.weather] ?: WEATHER_META[WeatherType.ENSOLARADO])?.displayName ?: worldState.weather.name
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hudLayout = settings.hudLayout ?: com.example.domain.HudLayoutConfig()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(title = "Direção & Controles na Tela") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SteeringControlStyle.entries.forEach { st ->
                        FilterChip(
                            selected = settings.steeringStyle == st,
                            onClick = { viewModel.updateSettings { it.copy(steeringStyle = st) } },
                            label = { Text(if (st == SteeringControlStyle.BOTOES) "Setas Esq/Dir" else "Volante Virtual") },
                        )
                    }
                }
                Text("Velocidade de Esterçamento (${"%.2f".format(settings.steerSpeedMultiplier)}x)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = settings.steerSpeedMultiplier.toFloat().coerceIn(0.5f, 2.0f),
                    onValueChange = { v -> viewModel.updateSettings { it.copy(steerSpeedMultiplier = v.toDouble()) } },
                    valueRange = 0.5f..2.0f,
                )
                Text("Tamanho dos Botões HUD (${(hudLayout.scale * 100).roundToInt()}%)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hudLayout.scale.toFloat().coerceIn(0.8f, 1.3f),
                    onValueChange = { v -> viewModel.updateHudLayout(hudLayout.copy(scale = v.toDouble())) },
                    valueRange = 0.8f..1.3f,
                )
                Text("Posição Vertical Controles (${hudLayout.controlOffsetYDp.roundToInt()} dp)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hudLayout.controlOffsetYDp.toFloat().coerceIn(-60f, 60f),
                    onValueChange = { v -> viewModel.updateHudLayout(hudLayout.copy(controlOffsetYDp = v.toDouble())) },
                    valueRange = -60f..60f,
                )
                Text("Opacidade dos Controles (${(settings.controlOpacity * 100).roundToInt()}%)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = settings.controlOpacity.toFloat().coerceIn(0.45f, 1.0f),
                    onValueChange = { v -> viewModel.updateSettings { it.copy(controlOpacity = v.toDouble()) } },
                    valueRange = 0.45f..1.0f,
                )
            }
        }
        item {
            SectionCard(title = "Câmera 3D") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CameraMode.entries.forEach { mode ->
                        FilterChip(
                            selected = cameraMode == mode,
                            onClick = {
                                while (viewModel.cameraMode.value != mode) {
                                    viewModel.cycleCameraMode()
                                }
                            },
                            label = { Text(CAMERA_MODE_LABELS[mode] ?: mode.name) },
                        )
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Tempo, Clima & Fazenda",
                subtitle = "Dia ${worldState.day} • Clima: $weatherLabel • Velocidade ${timeScale.roundToInt()}x",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = { viewModel.cycleTimeScale() }, modifier = Modifier.height(48.dp)) {
                        Text("Velocidade (${timeScale.roundToInt()}x)")
                    }
                    FilledTonalButton(onClick = { viewModel.cycleWorldWeather() }, modifier = Modifier.height(48.dp)) {
                        Text("Mudar Clima")
                    }
                    FilledTonalButton(onClick = { viewModel.nextDay() }, modifier = Modifier.height(48.dp)) {
                        Text("Dormir / Próximo Dia")
                    }
                    OutlinedButton(onClick = {
                        viewModel.toggleHudHidden()
                        onDismiss()
                    }, modifier = Modifier.height(48.dp)) {
                        Text(if (hudHidden) "Mostrar HUD" else "Modo Foto (Ocultar HUD)")
                    }
                }
            }
        }
        item {
            val activePresetId = worldState.player.appearance?.presetId ?: "preset_1"
            SectionCard(title = "Personagem (4 Opções)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CHARACTER_PRESETS.forEachIndexed { idx, preset ->
                        val active = activePresetId == preset.id
                        val jacketColor = Color(android.graphics.Color.parseColor(preset.config.shirtColorHex))
                        val hairColor = Color(android.graphics.Color.parseColor(preset.config.hairColorHex))
                        Surface(
                            color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else CabinCard,
                            border = BorderStroke(
                                width = if (active) 2.dp else 1.dp,
                                color = if (active) HarvestGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPlayerAppearancePreset(preset.id) }
                                .testTag("ingame_char_preset_${idx + 1}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(jacketColor)
                                        .border(2.5.dp, hairColor, CircleShape),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) HarvestGold else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Sistema, Salvamento & Gráficos") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.saveCurrentCareer() }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("Salvar Local")
                        }
                        FilledTonalButton(onClick = { viewModel.uploadCurrentToCloud() }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("Salvar Nuvem")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                onDismiss()
                                viewModel.navigateTo(AppRoute.Settings)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Text("Gráficos & Controles")
                        }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                viewModel.navigateTo(AppRoute.MainMenu)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Menu Principal")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCategoryButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp).testTag(testTag),
    ) {
        Icon(icon, contentDescription = label, tint = HarvestGold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FrotaTabContent(viewModel: FarmViewModel, worldState: WorldState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Veículos Próprios (${worldState.vehicles.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(worldState.vehicles, key = { it.uid }) { veh ->
            val def = getVehicleDef(veh.defId)
            val isAct = veh.uid == worldState.activeVehicleUid
            val tier = CONSERVATION_TIER_META[conservationTierFromPct(veh.conservationPct)]
                ?: CONSERVATION_TIER_META[ConservationTier.EXCELENTE]!!
            SectionCard(
                title = "${def.brand} ${def.model}",
                subtitle = "${def.horsepower.roundToInt()} cv • Diesel ${veh.fuelL.roundToInt()}/${def.fuelCapacityL.roundToInt()} L • ${tier.label} (${veh.conservationPct.roundToInt()}%)",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBadge(if (isAct) "EM USO" else "ESTACIONADO", if (isAct) AgroSprout else MaterialTheme.colorScheme.onSurfaceVariant)
                    StatBadge("Semente: ${getCropById(veh.selectedCropId).name}", HarvestGold)
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("soja", "milho", "trigo", "feijao_carioca", "cafe", "cana", "algodao", "rosas_familiares").forEach { cId ->
                        FilterChip(
                            selected = veh.selectedCropId == cId,
                            onClick = { viewModel.selectVehicleSeedCrop(veh.uid, cId) },
                            label = { Text(getCropById(cId).name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.switchToVehicle(veh.uid) },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Assumir")
                    }
                    FilledTonalButton(
                        onClick = { viewModel.openWorkshopForVehicle(veh.uid) },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Oficina")
                    }
                    OutlinedButton(
                        onClick = { viewModel.sellVehicle(veh.uid) },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Vender", color = StatusDanger)
                    }
                }
            }
        }
        item {
            Text("Implementos na Fazenda (${worldState.implements.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(worldState.implements, key = { it.uid }) { imp ->
            val def = getImplementDef(imp.defId)
            SectionCard(
                title = "${def.brand} ${def.model}",
                subtitle = "Largura ${"%.1f".format(def.workingWidthM)}m • ${def.requiredHp.roundToInt()} cv mín.",
            ) {
                Text("Carga: ${imp.fillL.roundToInt()}/${def.capacityL.roundToInt()} L • Posição: (${imp.x.roundToInt()}, ${imp.z.roundToInt()})", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OficinaTabContent(viewModel: FarmViewModel, worldState: WorldState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(worldState.vehicles, key = { it.uid }) { veh ->
            val quote = remember(veh) { viewModel.inspectVehicleQuote(veh.uid) } ?: return@items
            SectionCard(
                title = quote.title,
                subtitle = "Conservação ${quote.conservationPct.roundToInt()}% (${(CONSERVATION_TIER_META[quote.tier] ?: CONSERVATION_TIER_META[ConservationTier.EXCELENTE]!!).label}) • Sujeira ${quote.dirtPct.roundToInt()}%",
            ) {
                LinearProgressIndicator(
                    progress = { (quote.conservationPct / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (quote.conservationPct < 45) StatusDanger else if (quote.conservationPct < 75) StatusWarning else StatusGreen,
                )
                WorkshopComponent.entries.forEach { comp ->
                    val hp = quote.componentHealths[comp] ?: 100.0
                    val cost = quote.componentCostsBrl[comp] ?: 0.0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${WORKSHOP_COMPONENT_LABELS[comp]} (${hp.roundToInt()}%)", style = MaterialTheme.typography.bodySmall)
                        if (cost > 0) {
                            OutlinedButton(onClick = { viewModel.repairVehicleCompAction(veh.uid, comp) }) {
                                Text("Reparar ${formatBrl(cost)}", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Text("OK", style = MaterialTheme.typography.labelSmall, color = StatusGreen)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.repairVehicleFullAction(veh.uid, includeRefuel = false) },
                        enabled = quote.fullRepairCostBrl > 0,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text("Reparar Tudo (${formatBrl(quote.fullRepairCostBrl)})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.openWorkshopForVehicle(veh.uid) },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Tuning 360°")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.preventiveMaintenanceAction(veh.uid) }, modifier = Modifier.weight(1f)) {
                        Text("Preventiva ${formatBrl(quote.preventiveMaintenanceCostBrl)}", maxLines = 1)
                    }
                    OutlinedButton(onClick = { viewModel.washVehicleAction(veh.uid) }, modifier = Modifier.weight(1f), enabled = quote.washCostBrl > 0) {
                        Text("Lavar ${formatBrl(quote.washCostBrl)}", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun LojaTabContent(viewModel: FarmViewModel, worldState: WorldState) {
    val installedPacks = viewModel.installedPackIds.value
    val vehicles = remember(installedPacks) { getAvailableVehicles(installedPacks).take(35) }
    val implements = remember(installedPacks) { getAvailableImplements(installedPacks).take(35) }
    var showImp by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showImp, onClick = { showImp = false }, label = { Text("Máquinas (${vehicles.size}+)") })
            FilterChip(selected = showImp, onClick = { showImp = true }, label = { Text("Implementos (${implements.size}+)") })
            Spacer(Modifier.weight(1f))
            FilledTonalButton(onClick = {
                viewModel.openInGameTab(null)
                viewModel.navigateTo(com.example.domain.AppRoute.Dealership)
            }) { Text("Catálogo Completo") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!showImp) {
                items(vehicles, key = { it.id }) { def ->
                    SectionCard(title = "${def.brand} ${def.model}", subtitle = "${def.horsepower.roundToInt()} cv • ${def.era}") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(formatBrl(def.priceBrl), style = MaterialTheme.typography.titleMedium, color = HarvestGold)
                            Button(
                                onClick = { viewModel.buyVehicle(def.id) },
                                enabled = worldState.moneyBrl >= def.priceBrl,
                                modifier = Modifier.height(48.dp),
                            ) { Text("Comprar") }
                        }
                    }
                }
            } else {
                items(implements, key = { it.id }) { def ->
                    SectionCard(title = "${def.brand} ${def.model}", subtitle = "Largura ${"%.1f".format(def.workingWidthM)}m • ${def.requiredHp.roundToInt()}+ cv") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(formatBrl(def.priceBrl), style = MaterialTheme.typography.titleMedium, color = HarvestGold)
                            Button(
                                onClick = { viewModel.buyImplement(def.id) },
                                enabled = worldState.moneyBrl >= def.priceBrl,
                                modifier = Modifier.height(48.dp),
                            ) { Text("Comprar") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TerceirizacaoTabContent(viewModel: FarmViewModel, worldState: WorldState) {
    val companies = worldState.outsourcingCompanies ?: emptyList()
    val farmablePlots = worldState.plots.filter { (it.owned || it.leased) && !it.isPreservation }
    var selectedCompanyId by remember(companies) { mutableStateOf(companies.firstOrNull()?.id ?: "emp_coop_parana") }
    var selectedService by remember { mutableStateOf("PREPARO_SOLO") }
    var selectedModality by remember { mutableStateOf("B_COMPLETO_INSUMOS") }
    var selectedPlotId by remember(farmablePlots) { mutableIntStateOf(farmablePlots.firstOrNull()?.id ?: 1) }
    var selectedCropId by remember { mutableStateOf("soja") }

    val company = companies.find { it.id == selectedCompanyId } ?: companies.firstOrNull()
    val plot = farmablePlots.find { it.id == selectedPlotId }
    val quote = if (company != null && plot != null) {
        calculateOutsourcingQuote(company, selectedService, selectedModality, plot.areaHa)
    } else null

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if ((worldState.outsourcingContracts ?: emptyList()).isNotEmpty()) {
            item { Text("Ordens Terceirizadas Ativas", style = MaterialTheme.typography.titleMedium) }
            items(worldState.outsourcingContracts ?: emptyList(), key = { it.uid }) { ctr ->
                SectionCard(
                    title = "${ctr.companyName} — ${OUTSOURCING_SERVICE_META[ctr.serviceType]?.label ?: ctr.serviceType}",
                    subtitle = "Talhão #${ctr.plotId} • Operador: ${ctr.operatorName} • Status: ${ctr.status}",
                ) {
                    LinearProgressIndicator(
                        progress = { (ctr.progressPct / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = AgroSprout,
                    )
                    Text("${ctr.progressPct.roundToInt()}% concluído (${ctr.processedCellsCount}/${ctr.totalTargetCells} setores)", style = MaterialTheme.typography.bodySmall)
                    if (ctr.incident != "NENHUM" && ctr.status == "PAUSADO_IMPREVISTO") {
                        Text("Imprevisto: ${ctr.incidentNote ?: ctr.incident}", style = MaterialTheme.typography.bodySmall, color = StatusWarning)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.resolveOutsourcingIncident(ctr.uid, "RESOLVER_PADRAO") }) { Text("Autorizar Solução") }
                            OutlinedButton(onClick = { viewModel.resolveOutsourcingIncident(ctr.uid, "CANCELAR") }) { Text("Cancelar") }
                        }
                    }
                    if (ctr.status == "CONCLUIDO" && ctr.playerRating == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { stars ->
                                OutlinedButton(onClick = { viewModel.rateOutsourcingContract(ctr.uid, stars) }) {
                                    Text("${stars}★")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Contratar Terceirização Agrícola",
                subtitle = "Escolha a empresa, serviço, modalidade e talhão para operação autônoma no mapa.",
            ) {
                Text("Empresa Prestadora:", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    companies.forEach { c ->
                        FilterChip(
                            selected = selectedCompanyId == c.id,
                            onClick = {
                                selectedCompanyId = c.id
                                if (c.specialties.isNotEmpty()) selectedService = c.specialties.first()
                            },
                            label = { Text("${c.name} (${c.reputation}★)") },
                        )
                    }
                }
                Text("Serviço:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (company?.specialties ?: OUTSOURCING_SERVICE_META.keys.take(6)).forEach { srv ->
                        FilterChip(
                            selected = selectedService == srv,
                            onClick = { selectedService = srv },
                            label = { Text(OUTSOURCING_SERVICE_META[srv]?.label ?: srv, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Text("Modalidade:", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "A_MAQUINA_OPERADOR" to "A: Máquina + Operador",
                        "B_COMPLETO_INSUMOS" to "B: Completo c/ Insumos",
                        "C_SO_OPERADOR" to "C: Só Operador (Sua Frota)",
                    ).forEach { (id, lbl) ->
                        FilterChip(selected = selectedModality == id, onClick = { selectedModality = id }, label = { Text(lbl) })
                    }
                }
                Text("Talhão:", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    farmablePlots.forEach { p ->
                        FilterChip(
                            selected = selectedPlotId == p.id,
                            onClick = { selectedPlotId = p.id },
                            label = { Text("${p.name} (${p.areaHa} ha)") },
                        )
                    }
                }
                if (selectedService == "PLANTIO" || selectedService == "SEMEADURA") {
                    Text("Cultura:", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("soja", "milho", "trigo", "feijao_carioca", "algodao", "cafe", "rosas_familiares").forEach { cId ->
                            FilterChip(selected = selectedCropId == cId, onClick = { selectedCropId = cId }, label = { Text(getCropById(cId).name) })
                        }
                    }
                }
                quote?.let { q ->
                    Surface(color = CabinCard, shape = MaterialTheme.shapes.small) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Orçamento Estimado: ${formatBrl(q.totalBrl)}", style = MaterialTheme.typography.titleMedium, color = HarvestGold)
                            Text("Duração estimada: ~${q.estimatedHours}h • Mobilização: ${formatBrl(q.mobilizationFeeBrl)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.hireOutsourcingService(
                                HireOutsourcingParams(
                                    companyId = selectedCompanyId,
                                    serviceType = selectedService,
                                    modality = selectedModality,
                                    plotId = selectedPlotId,
                                    cropId = selectedCropId,
                                    playerVehicleUid = worldState.activeVehicleUid ?: worldState.vehicles.firstOrNull()?.uid,
                                )
                            )
                        },
                        enabled = worldState.moneyBrl >= q.totalBrl,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("hire_outsourcing_btn"),
                    ) {
                        Text("Contratar Serviço Terceirizado")
                    }
                }
            }
        }
    }
}

@Composable
private fun EconomiaTabContent(viewModel: FarmViewModel, worldState: WorldState) {
    val nonZeroInv = worldState.inventoryKg.filterValues { it > 0.5 }
    var selectedEst by remember { mutableStateOf(ESTABLISHMENTS.first()) }
    var bankAmountText by remember { mutableStateOf("10000") }
    var loanAmountText by remember { mutableStateOf("50000") }

    val activeVeh = worldState.activeVehicleUid?.let { uid -> worldState.vehicles.find { it.uid == uid } }
    val attachedTrailer = activeVeh?.attachedImplementUid?.let { iUid ->
        worldState.implements.find { it.uid == iUid }?.takeIf { imp ->
            getImplementDef(imp.defId).category == com.example.domain.ImplementCategory.CARRETA && imp.fillL > 5.0
        }
    }
    val harvesterCargoKg = activeVeh?.takeIf { it.grainTankFillL > 5.0 }?.grainTankFillL ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                title = "Banco Rural Brasil — Conta & Finanças",
                subtitle = "Em Mãos: ${formatBrl(worldState.moneyBrl)} • Conta Bancária: ${formatBrl(worldState.bankBalanceBrl ?: 0.0)}",
            ) {
                Text(
                    "Dívida Rural Ativa: ${formatBrl(worldState.bankLoanBrl)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (worldState.bankLoanBrl > 0.0) StatusWarning else AgroSprout,
                )
                OutlinedTextField(
                    value = bankAmountText,
                    onValueChange = { bankAmountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Valor para Depósito / Saque (R$)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_amount_input"),
                )
                val bankAmt = bankAmountText.toDoubleOrNull() ?: 0.0
                val bankBal = worldState.bankBalanceBrl ?: 0.0
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.depositBank(bankAmt) },
                        enabled = bankAmt > 0 && worldState.moneyBrl >= bankAmt,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("bank_deposit_btn"),
                    ) {
                        Text("Depositar")
                    }
                    FilledTonalButton(
                        onClick = { viewModel.withdrawBank(bankAmt) },
                        enabled = bankAmt > 0 && bankBal >= bankAmt,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("bank_withdraw_btn"),
                    ) {
                        Text("Sacar")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.depositBank(10000.0) },
                        enabled = worldState.moneyBrl >= 10000.0,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text("+R$ 10 mil") }
                    OutlinedButton(
                        onClick = { viewModel.depositBank(50000.0) },
                        enabled = worldState.moneyBrl >= 50000.0,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text("+R$ 50 mil") }
                }
            }
        }
        item {
            SectionCard(title = "Balança Rodoviária & Pontos de Venda") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ESTABLISHMENTS.forEach { est ->
                        FilterChip(
                            selected = selectedEst.id == est.id,
                            onClick = { selectedEst = est },
                            label = { Text("${est.name} (${(est.priceMultiplier * 100).roundToInt()}%)") },
                        )
                    }
                }
                Text("Horário: ${selectedEst.openHour}h–${selectedEst.closeHour}h • Aceita ${selectedEst.acceptedProductIds.size} produtos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (attachedTrailer != null || harvesterCargoKg > 5.0) {
                    val cargoLabel = attachedTrailer?.let {
                        "${getCropById(it.fillProductId ?: "soja").name} na Carreta (${it.fillL.roundToInt()} kg)"
                    } ?: "Tanque Colheitadeira (${harvesterCargoKg.roundToInt()} kg)"
                    Button(
                        onClick = { viewModel.sellCargoAtStation() },
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestGold, contentColor = Color(0xFF0E1310)),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sell_cargo_station_btn"),
                    ) {
                        Text("Pesar & Vender $cargoLabel", fontWeight = FontWeight.Bold)
                    }
                }
                if (nonZeroInv.isEmpty()) {
                    Text("Silo e estoque vazios no momento. Colha e descarregue safras para vender.", style = MaterialTheme.typography.bodySmall)
                } else {
                    nonZeroInv.forEach { (prodId, kg) ->
                        val accepts = selectedEst.acceptedProductIds.contains(prodId)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("${getCropById(prodId).takeIf { it.id == prodId }?.name ?: prodId}: ${kg.roundToInt()} kg", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = { viewModel.sellAtWeighbridge(selectedEst, prodId, kg) },
                                enabled = accepts,
                                modifier = Modifier.height(48.dp),
                            ) {
                                Text(if (accepts) "Vender Tudo" else "Não aceita")
                            }
                        }
                    }
                }
                worldState.lastWeighbridgeTicket?.let { tk ->
                    Surface(color = CabinCard, shape = MaterialTheme.shapes.small) {
                        Text(
                            "Último Ticket (${tk.establishmentName}): ${tk.netKg.roundToInt()} kg líquido • Total ${formatBrl(tk.totalBrl)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = HarvestGold,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                        )
                    }
                }
            }
        }
        item {
            SectionCard(title = "Crédito Rural & Financiamento (Dívida: ${formatBrl(worldState.bankLoanBrl)})") {
                OutlinedTextField(
                    value = loanAmountText,
                    onValueChange = { loanAmountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Valor (R$)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val amt = loanAmountText.toDoubleOrNull() ?: 0.0
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.takeLoan(amt) }, enabled = amt > 0, modifier = Modifier.weight(1f).height(48.dp).testTag("bank_take_loan_btn")) {
                        Text("Tomar Crédito")
                    }
                    OutlinedButton(onClick = { viewModel.repayLoan(amt) }, enabled = amt > 0 && worldState.bankLoanBrl > 0, modifier = Modifier.weight(1f).height(48.dp).testTag("bank_repay_loan_btn")) {
                        Text("Amortizar")
                    }
                }
            }
        }
    }
}

@Composable
private fun IndustriasTabContent(viewModel: FarmViewModel, worldState: WorldState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(INDUSTRY_CHAINS, key = { it.id }) { recipe ->
            val owned = worldState.industries.find { it.recipeId == recipe.id }
            SectionCard(
                title = recipe.industryName,
                subtitle = "Entrada: ${recipe.inputAmountKg.roundToInt()} kg de ${recipe.inputProductId} • Tempo: ${recipe.processingHours}h",
            ) {
                Text("Produz: ${recipe.outputs.joinToString(", ") { "${it.kg.roundToInt()} kg ${it.name}" }}", style = MaterialTheme.typography.bodySmall)
                if (owned?.purchased == true) {
                    Text("Estoque bruto: ${owned.rawStockKg.roundToInt()} kg • Status: ${if (owned.isProcessing) "Processando" else "Aguardando"}", style = MaterialTheme.typography.labelSmall, color = AgroSprout)
                    Button(
                        onClick = { viewModel.supplyIndustry(recipe.id) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Enviar ${recipe.inputAmountKg.roundToInt()} kg de ${recipe.inputProductId}")
                    }
                } else {
                    Button(
                        onClick = { viewModel.buyIndustry(recipe.id) },
                        enabled = worldState.moneyBrl >= recipe.plantCostBrl,
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestGold, contentColor = Color(0xFF0E1310)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Instalar Planta (${formatBrl(recipe.plantCostBrl)})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PecuariaTabContent(viewModel: FarmViewModel, worldState: WorldState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(AnimalSpecies.entries) { sp ->
            val meta = ANIMAL_META[sp] ?: return@items
            val herd = worldState.herds.find { it.species == sp }
            SectionCard(
                title = "${meta.displayName} (${herd?.count ?: 0} cabeças)",
                subtitle = "Produção: ${meta.productName} • Preço: ${formatBrl(meta.unitPriceBrl)}/un",
            ) {
                if (herd != null && herd.count > 0) {
                    Text("Saúde: ${herd.healthPct.roundToInt()}% • Ração: ${herd.feedReserveKg.roundToInt()} kg • Água: ${herd.waterReserveL.roundToInt()} L", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.buyAnimals(sp, 5) },
                        enabled = worldState.moneyBrl >= meta.unitPriceBrl * 5,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text("+5 Comprar") }
                    FilledTonalButton(
                        onClick = { viewModel.feedAndWaterHerd(sp) },
                        enabled = (herd?.count ?: 0) > 0,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text("Tratar") }
                    OutlinedButton(
                        onClick = { viewModel.sellAnimalHeads(sp, 2) },
                        enabled = (herd?.count ?: 0) >= 2,
                        modifier = Modifier.height(48.dp),
                    ) { Text("-2 Vender") }
                }
            }
        }
    }
}

@Composable
private fun EquipeSoloTabContent(viewModel: FarmViewModel, worldState: WorldState) {
    val ownedPlots = worldState.plots.filter { (it.owned || it.leased) && !it.isPreservation }
    var empName by remember { mutableStateOf("João Silva") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionCard(title = "Loja Agropecuária — Insumos & Sementes") {
                val stock = worldState.inputsStockKg ?: emptyMap()
                Text("Calcário: ${(stock["calcario"] ?: 0.0).roundToInt()} kg • Adubo NPK: ${(stock["adubo"] ?: 0.0).roundToInt()} kg • Defensivo: ${(stock["defensivo"] ?: 0.0).roundToInt()} kg", style = MaterialTheme.typography.bodySmall)
                Text("Semente Soja: ${(stock["soja"] ?: 0.0).roundToInt()} kg • Semente Milho: ${(stock["milho"] ?: 0.0).roundToInt()} kg", style = MaterialTheme.typography.bodySmall, color = AgroSprout)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { viewModel.buyInputStock("calcario", 1000.0, 420.0) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("+1t Calcário") }
                    Button(onClick = { viewModel.buyInputStock("adubo", 500.0, 1450.0) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("+500kg NPK") }
                    Button(onClick = { viewModel.buyInputStock("defensivo", 100.0, 890.0) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("+100kg Def.") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.buyInputStock("soja", 500.0, 1850.0) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("buy_soja_seed_btn"),
                    ) { Text("+500kg Sem. Soja") }
                    FilledTonalButton(
                        onClick = { viewModel.buyInputStock("milho", 500.0, 2150.0) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("buy_milho_seed_btn"),
                    ) { Text("+500kg Sem. Milho") }
                }
            }
        }
        item {
            Text("Agronomia por Talhão", style = MaterialTheme.typography.titleMedium)
        }
        items(ownedPlots.take(12), key = { it.id }) { plot ->
            SectionCard(title = plot.name, subtitle = "${plot.areaHa} ha") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = { viewModel.irrigatePlot(plot.id) }) { Text("Irrigar Pivô") }
                    FilledTonalButton(onClick = { viewModel.applyLimeToPlot(plot.id) }) { Text("Calagem pH") }
                    FilledTonalButton(onClick = { viewModel.sprayPlotPests(plot.id) }) { Text("Pulverizar") }
                    FilledTonalButton(onClick = { viewModel.applyContourProtection(plot.id) }) { Text("Curvas Nível") }
                }
            }
        }
        item {
            SectionCard(title = "Contratar Operador (${(worldState.employees ?: emptyList()).size}/12)") {
                OutlinedTextField(value = empName, onValueChange = { empName = it }, label = { Text("Nome do Operador") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.hireEmployee(empName.ifBlank { "Operador Rural" }, "Operador de Máquinas", 240.0) }, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Contratar (R$ 240/dia)")
                    }
                    OutlinedButton(onClick = { viewModel.hireEmployee(empName.ifBlank { "Agrônomo" }, "Agrônomo / Pulverização", 320.0) }, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Agrônomo (R$ 320)")
                    }
                }
            }
        }
        items(worldState.employees ?: emptyList(), key = { it.uid }) { emp ->
            SectionCard(title = emp.name, subtitle = "${emp.role} • Tarefa: ${emp.taskType} • Diária ${formatBrl(emp.dailyWageBrl)}") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val targetPlotId = ownedPlots.firstOrNull()?.id
                    val targetVehUid = worldState.vehicles.firstOrNull()?.uid
                    Button(onClick = { viewModel.assignEmployee(emp.uid, targetVehUid, targetPlotId, "PLANTAR") }, modifier = Modifier.weight(1f)) { Text("Escalar Plantio") }
                    OutlinedButton(onClick = { viewModel.fireEmployee(emp.uid) }) { Text("Desligar", color = StatusDanger) }
                }
            }
        }
    }
}

@Composable
private fun CalendarioTabContent(worldState: WorldState) {
    val curMonth = ((worldState.month - 1).mod(12)) + 1
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Calendário Agrícola Brasileiro (${ALL_CROPS.size} Culturas • Mês Atual: $curMonth)", style = MaterialTheme.typography.titleMedium)
        }
        items(ALL_CROPS, key = { it.id }) { crop ->
            val inWindow = crop.plantingMonths.contains(curMonth)
            SectionCard(
                title = crop.name,
                subtitle = "${crop.category.name} • Ciclo ${crop.growthDays} dias • Prod. ${crop.baseYieldTonsPerHa} t/ha",
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Meses plantio: ${crop.plantingMonths.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    StatBadge(if (inWindow) "JANELA IDEAL" else "Entressafra", if (inWindow) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ContratosDreTabContent(viewModel: FarmViewModel, worldState: WorldState) {
    val pnl = remember(worldState.ledger, worldState.vehicles, worldState.plots) { computeProfitAndLossReport(worldState) }
    val unlockedIds = worldState.achievementsUnlocked ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionCard(title = "Balanço Financeiro (DRE) & Patrimônio") {
                Text("Receita Bruta: ${formatBrl(pnl.totalRevenueBrl)}", style = MaterialTheme.typography.labelLarge, color = StatusGreen)
                Text("Despesas Operacionais: -${formatBrl(pnl.totalOperatingExpenseBrl)}", style = MaterialTheme.typography.bodySmall, color = StatusDanger)
                Text("CAPEX Máquinas & Obras: -${formatBrl(pnl.totalCapexBrl)}", style = MaterialTheme.typography.bodySmall, color = StatusWarning)
                Text("Lucro Operacional Líquido: ${formatBrl(pnl.netOperatingProfitBrl)}", style = MaterialTheme.typography.titleMedium, color = HarvestGold)
                Text("Patrimônio Total (Terras + Frota - Dívida): ${formatBrl(pnl.netWorthBrl)}", style = MaterialTheme.typography.labelLarge, color = AgroSprout)
            }
        }
        item {
            Text("Contratos Agrícolas (${worldState.contracts.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(worldState.contracts, key = { it.uid }) { ctr ->
            SectionCard(
                title = ctr.title,
                subtitle = "${ctr.clientName} • Prêmio: ${formatBrl(ctr.rewardBrl)}",
            ) {
                Text("Progresso: ${ctr.completedUnits.roundToInt()}/${ctr.targetUnits.roundToInt()}", style = MaterialTheme.typography.bodySmall)
                if (!ctr.completed) {
                    Button(
                        onClick = { viewModel.acceptOrCompleteContract(ctr.uid) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(if (!ctr.accepted) "Aceitar Contrato" else "Concluir / Entregar")
                    }
                } else {
                    StatBadge("CONCLUÍDO", StatusGreen)
                }
            }
        }
        item {
            Text("Conquistas (${unlockedIds.size}/${ACHIEVEMENT_CATALOG.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(ACHIEVEMENT_CATALOG, key = { it.id }) { ach ->
            val unlocked = unlockedIds.contains(ach.id)
            SectionCard(title = "${ach.title} (+${formatBrl(ach.rewardBrl)})", subtitle = ach.description) {
                StatBadge(if (unlocked) "DESBLOQUEADA" else "Em andamento", if (unlocked) HarvestGold else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
