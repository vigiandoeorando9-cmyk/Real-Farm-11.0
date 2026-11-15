package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ALL_IMPLEMENTS
import com.example.domain.ALL_VEHICLES
import com.example.domain.AppRoute
import com.example.domain.CHARACTER_PRESETS
import com.example.domain.CharacterPresetOption
import com.example.domain.CloudSaveSummary
import com.example.domain.DEFAULT_HUB_PACKS
import com.example.domain.FarmAccountUser
import com.example.domain.MAP_SIZE_META
import com.example.domain.MapSizeOption
import com.example.domain.PlayerAppearanceConfig
import com.example.domain.getAvailableMaps
import com.example.domain.getMapById
import com.example.simulation.formatBrl
import com.example.storage.CareerSlotMeta
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionCard
import com.example.ui.components.StatBadge
import com.example.ui.theme.AgroSprout
import com.example.ui.theme.CabinCard
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.WoodTrim

private data class MenuActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: AppRoute,
    val accent: Color,
    val tag: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainMenuScreen(
    accountUser: FarmAccountUser?,
    hasActiveCareer: Boolean,
    careerSlots: List<CareerSlotMeta?>,
    cloudSaves: List<CloudSaveSummary>,
    installedPackCount: Int,
    onContinueCareer: () -> Unit,
    onLoadSlot: (Int) -> Unit,
    onDownloadCloudSlot: (Int) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSignOut: () -> Unit,
) {
    val filledSlots = remember(careerSlots) { careerSlots.filterNotNull() }

    val mapCount = remember { getAvailableMaps(emptySet()).size }
    val vehicleCount = remember { ALL_VEHICLES.size }
    val implementCount = remember { ALL_IMPLEMENTS.size }
    val modCount = remember { DEFAULT_HUB_PACKS.size }

    val quickActions = remember(mapCount, vehicleCount, implementCount, modCount) {
        listOf(
            MenuActionItem("Regiões do Brasil", "$mapCount mapas base + ModHub", Icons.Rounded.Map, AppRoute.MapSelection, AgroSprout, "menu_maps"),
            MenuActionItem("Multiplayer Online", "Salas cooperativas", Icons.Rounded.Groups, AppRoute.Multiplayer, StatusInfo, "menu_mp"),
            MenuActionItem("Concessionária", "$vehicleCount máquinas nacionais", Icons.Rounded.Agriculture, AppRoute.Dealership, HarvestGold, "menu_dealership"),
            MenuActionItem("Loja Implementos", "$implementCount implementos pesados", Icons.Rounded.LocalShipping, AppRoute.ImplementShop, AgroSprout, "menu_implements"),
            MenuActionItem("Mercado de Usados", "Leilões e seminovas", Icons.Rounded.Storefront, AppRoute.UsedMarket, HarvestGold, "menu_used"),
            MenuActionItem("Garagem da Sede", "Inspecionar frota", Icons.Rounded.Garage, AppRoute.Garage, StatusInfo, "menu_garage"),
            MenuActionItem("Oficina Universal", "10 sistemas de tuning", Icons.Rounded.Build, AppRoute.Workshop, HarvestGold, "menu_workshop"),
            MenuActionItem("Farm Brasil ModHub", "$modCount pacotes e gráficos", Icons.Rounded.Extension, AppRoute.Hub, AgroSprout, "menu_hub"),
            MenuActionItem("20 Espaços & Backup", "Exportar e sincronizar", Icons.Rounded.FolderCopy, AppRoute.ContentManager, StatusInfo, "menu_content"),
            MenuActionItem("Monitor 3D / FPS", "Auto-otimização móvel", Icons.Rounded.Speed, AppRoute.PerformanceMonitor, AgroSprout, "menu_perf"),
            MenuActionItem("Armazenamento", "Cotas e limpeza local", Icons.Rounded.Storage, AppRoute.StorageManager, HarvestGold, "menu_storage"),
            MenuActionItem("Configurações", "Conta, Mega-Realista e áudio", Icons.Rounded.Settings, AppRoute.Settings, Color(0xFFB5C4BA), "menu_settings"),
            MenuActionItem("Créditos & Sistemas", "Modelo original Farm Brasil", Icons.Rounded.Info, AppRoute.Credits, Color(0xFFB5C4BA), "menu_credits"),
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_farm_brasil),
                    contentDescription = "Paisagem rural Farm Brasil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color(0xFF0E1310).copy(alpha = 0.45f),
                                    Color(0xFF0E1310),
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // User profile header bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, WoodTrim.copy(alpha = 0.75f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(AgroSprout.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Agriculture,
                                        contentDescription = null,
                                        tint = HarvestGold,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = accountUser?.displayName ?: "Produtor Rural",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                    )
                                    val maskedCode = accountUser?.personalCode
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { "Conta Farm Brasil • ••••${it.takeLast(4)}" }
                                        ?: "Conta Local Farm Brasil"
                                    Text(
                                        text = maskedCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AgroSprout,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .testTag("sign_out_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Trocar ou sair da conta Farm Brasil",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "FARM BRASIL",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatBadge("${filledSlots.size}/20 Carreiras", AgroSprout)
                            StatBadge("${cloudSaves.size} em Nuvem", StatusInfo)
                            StatBadge("$installedPackCount Mods Ativos", HarvestGold)
                        }
                    }
                }
            }
        }

        // Primary Launch Actions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (hasActiveCareer || filledSlots.isNotEmpty()) {
                    Button(
                        onClick = onContinueCareer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HarvestGold,
                            contentColor = Color(0xFF0E1310),
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("continue_career_button"),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (hasActiveCareer) "Retomar Fazenda Atual" else "Continuar Última Carreira",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { onNavigate(AppRoute.NewCareerSetup) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("new_career_button"),
                ) {
                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nova Carreira em Fazenda", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Saved Careers Strip
        if (filledSlots.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Carreiras Salvas (${filledSlots.size}/20)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filledSlots, key = { it.slotIndex }) { slot ->
                            Card(
                                modifier = Modifier
                                    .width(235.dp)
                                    .clickable { onLoadSlot(slot.slotIndex) }
                                    .testTag("career_card_${slot.slotIndex}"),
                                colors = CardDefaults.cardColors(containerColor = CabinCard),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "${slot.slotIndex + 1}. ${slot.farmName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${slot.mapName} • Dia ${slot.day}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AgroSprout,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = formatBrl(slot.moneyBrl),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = HarvestGold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cloud Quick Restore
        if (cloudSaves.isNotEmpty() && filledSlots.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionCard(
                        title = "Saves Encontrados na Nuvem Firebase",
                        subtitle = "Restaure sua carreira diretamente de outro dispositivo:",
                    ) {
                        cloudSaves.take(3).forEach { cs ->
                            OutlinedButton(
                                onClick = { onDownloadCloudSlot(cs.slotIndex) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("quick_restore_cloud_${cs.slotIndex}"),
                            ) {
                                Icon(Icons.Rounded.CloudSync, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Baixar '${cs.farmName}' (Dia ${cs.day} • ${formatBrl(cs.moneyBrl)})")
                            }
                        }
                    }
                }
            }
        }

        // All Modules Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Centrais & Serviços do Simulador",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                quickActions.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigate(item.route) }
                                    .testTag(item.tag),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(
                                        color = item.accent.copy(alpha = 0.16f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                tint = item.accent,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class CareerModeOption(
    val id: String,
    val label: String,
    val cashBrl: Double,
    val desc: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewCareerSetupScreen(
    selectedMapId: String,
    installedPackIds: Set<String>,
    careerSlots: List<CareerSlotMeta?>,
    onSelectMap: (String) -> Unit,
    onStartCareer: (
        slotIndex: Int,
        farmName: String,
        mapId: String,
        startingMoneyBrl: Double,
        startingMode: String,
        gridSide: Int,
        appearance: PlayerAppearanceConfig,
    ) -> Unit,
    onBack: () -> Unit,
) {
    val availableMaps = remember(installedPackIds) { getAvailableMaps(installedPackIds) }
    val selectedMap = remember(selectedMapId) { getMapById(selectedMapId) }

    val modes = remember {
        listOf(
            CareerModeOption("Agricultura Familiar", "Agricultura Familiar", 145_000.0, "Foco em hortaliças, rosas, sítio produtivo e máquinas compactas."),
            CareerModeOption("Padrão", "Fazendeiro Padrão", 250_000.0, "Sede estruturada, trator médio, implementos iniciais e equilíbrio econômico."),
            CareerModeOption("Gerente", "Gerente Agrícola", 550_000.0, "Capital reforçado para frota múltipla e agroindústria."),
            CareerModeOption("Do Zero", "Começar do Zero", 120_000.0, "Terreno bruto para desbravar e conquistar linha de crédito."),
            CareerModeOption("Milionário", "Empresário do Agro", 2_500_000.0, "Caixa milionário para articulados e colheitadeiras axiais."),
        )
    }

    val firstEmptySlot = remember(careerSlots) {
        careerSlots.indexOfFirst { it == null }.takeIf { it >= 0 } ?: 0
    }

    var farmName by remember { mutableStateOf("Fazenda ${selectedMap.stateName}") }
    var selectedMode by remember { mutableStateOf(modes[1]) }
    var selectedSlot by remember { mutableIntStateOf(firstEmptySlot) }
    var selectedSize by remember { mutableStateOf(MapSizeOption.GRANDE) }
    var selectedPreset by remember { mutableStateOf(CHARACTER_PRESETS.first()) }
    var hatStyle by remember { mutableStateOf(selectedPreset.config.hatStyle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Nova Carreira Rural",
            subtitle = "Configure sua propriedade em ${selectedMap.stateName}",
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard(title = "1. Identificação & Personagem (4 Opções)") {
                OutlinedTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = { Text("Nome da Fazenda") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("farm_name_input"),
                )
                Text("Escolha seu Personagem:", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CHARACTER_PRESETS.forEachIndexed { idx, preset ->
                        val active = selectedPreset.id == preset.id
                        val jacketColor = Color(android.graphics.Color.parseColor(preset.config.shirtColorHex))
                        val pantsColor = Color(android.graphics.Color.parseColor(preset.config.pantsColorHex))
                        val skinColor = Color(android.graphics.Color.parseColor(preset.config.skinColorHex))
                        val hairColor = Color(android.graphics.Color.parseColor(preset.config.hairColorHex))
                        Surface(
                            color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else CabinCard,
                            border = BorderStroke(
                                width = if (active) 2.dp else 1.dp,
                                color = if (active) HarvestGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPreset = preset
                                    hatStyle = preset.config.hatStyle
                                }
                                .testTag("char_preset_${idx + 1}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Mini swatch preview matching character colors
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.width(34.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(skinColor)
                                            .border(2.dp, hairColor, CircleShape),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 22.dp, height = 14.dp)
                                            .background(jacketColor, MaterialTheme.shapes.extraSmall),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 18.dp, height = 10.dp)
                                            .background(pantsColor, MaterialTheme.shapes.extraSmall),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.titleSmall,
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
                Text("Acessório na Cabeça (Opcional):", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sem" to "Visual da Foto", "palha" to "Chapéu de Palha", "bone" to "Boné Agro", "vaqueiro" to "Chapéu Vaqueiro").forEach { (id, lbl) ->
                        FilterChip(
                            selected = hatStyle == id,
                            onClick = { hatStyle = id },
                            label = { Text(lbl) },
                        )
                    }
                }
            }

            SectionCard(title = "2. Modalidade Inicial & Capital") {
                modes.forEach { m ->
                    val active = selectedMode.id == m.id
                    Surface(
                        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else CabinCard,
                        border = BorderStroke(1.5.dp, if (active) AgroSprout else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = m }
                            .testTag("mode_${m.id}"),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(m.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(formatBrl(m.cashBrl), style = MaterialTheme.typography.labelLarge, color = HarvestGold)
                            }
                            Text(m.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            SectionCard(title = "3. Região do Brasil (${availableMaps.size} disponíveis)") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableMaps, key = { it.id }) { m ->
                        val selected = m.id == selectedMapId
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onSelectMap(m.id)
                                farmName = "Fazenda ${m.stateName}"
                            },
                            label = { Text(m.stateName) },
                        )
                    }
                }
                Text(
                    text = "${selectedMap.regionName} • ${selectedMap.biome} • ${selectedMap.soilName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = AgroSprout,
                )
                Text(
                    text = selectedMap.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "4. Tamanho do Mapa & Espaço de Salvamento (20 Slots)") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MapSizeOption.entries.forEach { opt ->
                        val meta = MAP_SIZE_META[opt] ?: MAP_SIZE_META.values.first()
                        FilterChip(
                            selected = selectedSize == opt,
                            onClick = { selectedSize = opt },
                            label = { Text("${meta.label} (${meta.spanLabel})") },
                        )
                    }
                }
                Text(
                    text = (MAP_SIZE_META[selectedSize] ?: MAP_SIZE_META.values.first()).description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text("Espaço de Carreira:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(20) { idx ->
                        val existing = careerSlots.getOrNull(idx)
                        FilterChip(
                            selected = selectedSlot == idx,
                            onClick = { selectedSlot = idx },
                            label = {
                                Text(
                                    if (existing != null) "Espaço ${idx + 1} (${existing.farmName})"
                                    else "Espaço ${idx + 1} (Vazio)"
                                )
                            },
                        )
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    val gridSide = (MAP_SIZE_META[selectedSize] ?: MAP_SIZE_META.values.first()).gridSide
                    onStartCareer(
                        selectedSlot,
                        farmName,
                        selectedMapId,
                        selectedMode.cashBrl,
                        selectedMode.id,
                        gridSide,
                        selectedPreset.config.copy(hatStyle = hatStyle),
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HarvestGold,
                    contentColor = Color(0xFF0E1310),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp)
                    .testTag("start_new_career_button"),
            ) {
                Text(
                    text = "Iniciar Fazenda no Espaço ${selectedSlot + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
