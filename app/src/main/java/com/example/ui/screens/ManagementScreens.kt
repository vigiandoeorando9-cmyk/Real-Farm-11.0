package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AccountSyncStatus
import com.example.domain.AudioChannelSettings
import com.example.domain.CloudSaveSummary
import com.example.domain.FEATURE_REGISTRY
import com.example.domain.FUTURE_50_EXPANSION_SLOTS
import com.example.domain.FarmAccountUser
import com.example.domain.GRAPHICS_PRESET_META
import com.example.domain.GameSettings
import com.example.domain.GraphicsConfig
import com.example.domain.GraphicsPreset
import com.example.domain.HUB_BRAND_FILTERS
import com.example.domain.HUB_CATEGORIES
import com.example.domain.HudLayoutConfig
import com.example.domain.HubPackEntity
import com.example.domain.IMPLEMENT_CATEGORY_LABELS
import com.example.domain.ImplementCategory
import com.example.domain.ImplementDef
import com.example.domain.ModDownloadProgress
import com.example.domain.ModDownloadStatus
import com.example.domain.SteeringControlStyle
import com.example.domain.TEXTURE_RESOLUTION_OPTIONS
import com.example.domain.VEHICLE_CATEGORY_LABELS
import com.example.domain.VehicleCategory
import com.example.domain.VehicleDef
import com.example.domain.WorldState
import com.example.domain.clampGraphicsConfigForDevice
import com.example.domain.getAvailableImplements
import com.example.domain.getAvailableMaps
import com.example.domain.getAvailableVehicles
import com.example.domain.getExternalGraphicsPacks
import com.example.domain.getHubMeta
import com.example.domain.getVehicleDef
import com.example.domain.hasHubUpdateAvailable
import com.example.domain.matchesHubCategory
import com.example.domain.validateModPack
import com.example.simulation.formatBrl
import com.example.storage.CareerSlotMeta
import com.example.ui.components.CloudSavesSection
import com.example.ui.components.MachineGalleryModal
import com.example.ui.components.MachinePreviewCanvas
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionCard
import com.example.ui.components.StatBadge
import com.example.ui.theme.AgroSprout
import com.example.ui.theme.CabinCard
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.WoodDark
import com.example.ui.theme.WoodTrim
import kotlin.math.roundToInt

enum class ShopScreenMode {
    DEALERSHIP,
    IMPLEMENTS,
    USED,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapSelectionScreen(
    selectedMapId: String,
    installedPackIds: Set<String>,
    onSelectAndStart: (String) -> Unit,
    onBack: () -> Unit,
) {
    val maps = remember(installedPackIds) { getAvailableMaps(installedPackIds) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Regiões Agrícolas (${maps.size})",
            subtitle = "Escolha um bioma brasileiro ou expansão para iniciar sua propriedade",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(shape = MaterialTheme.shapes.large) {
                    Image(
                        painter = painterResource(id = R.drawable.img_brazil_regions),
                        contentDescription = "Mapa das regiões agrícolas do Brasil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                    )
                }
            }
            items(maps, key = { it.id }) { m ->
                val active = m.id == selectedMapId
                SectionCard(
                    title = "${m.stateName} — ${m.regionName}",
                    subtitle = "${m.biome} • Solo: ${m.soilName} • Terra: ${formatBrl(m.startingLandValuePerHa)}/ha",
                ) {
                    Text(m.description, style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        m.featuredCrops.forEach { c -> StatBadge(c.uppercase(), AgroSprout) }
                    }
                    Button(
                        onClick = { onSelectAndStart(m.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) HarvestGold else MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF0E1310),
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("select_map_${m.id}"),
                    ) {
                        Text(if (active) "Selecionado • Criar Fazenda" else "Escolher Região", fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                SectionCard(
                    title = "Banco de Expansões Futuras (${FUTURE_50_EXPANSION_SLOTS.size} Slots)",
                    subtitle = "Regiões planejadas sob demanda para próximas atualizações:",
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FUTURE_50_EXPANSION_SLOTS.take(15).forEach { exp ->
                            StatBadge("${exp.code}: ${exp.region}", MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopScreen(
    mode: ShopScreenMode,
    worldState: WorldState,
    installedPackIds: Set<String>,
    onBuyVehicle: (String) -> Unit,
    onBuyImplement: (String) -> Unit,
    onBidAuction: (String, Double) -> Unit,
    onBack: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<VehicleCategory?>(null) }
    var selectedImpCat by remember { mutableStateOf<ImplementCategory?>(null) }
    var selectedBrand by remember { mutableStateOf("Todas") }
    var galleryVeh by remember { mutableStateOf<VehicleDef?>(null) }
    var galleryImp by remember { mutableStateOf<ImplementDef?>(null) }
    var compareIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCompare by remember { mutableStateOf(false) }

    val allVehicles = remember(installedPackIds) { getAvailableVehicles(installedPackIds) }
    val allImplements = remember(installedPackIds) { getAvailableImplements(installedPackIds) }
    val brands = remember(mode, allVehicles, allImplements) {
        val raw = if (mode == ShopScreenMode.IMPLEMENTS) allImplements.map { it.brand } else allVehicles.map { it.brand }
        listOf("Todas") + raw.distinct().sorted().take(18)
    }

    val filteredVehicles = remember(allVehicles, search, selectedCategory, selectedBrand, mode) {
        allVehicles.filter { v ->
            val qOk = search.isBlank() || v.model.contains(search, true) || v.brand.contains(search, true)
            val cOk = selectedCategory == null || v.category == selectedCategory
            val bOk = selectedBrand == "Todas" || v.brand == selectedBrand
            qOk && cOk && bOk
        }.take(90)
    }
    val filteredImplements = remember(allImplements, search, selectedImpCat, selectedBrand) {
        allImplements.filter { i ->
            val qOk = search.isBlank() || i.model.contains(search, true) || i.brand.contains(search, true)
            val cOk = selectedImpCat == null || i.category == selectedImpCat
            val bOk = selectedBrand == "Todas" || i.brand == selectedBrand
            qOk && cOk && bOk
        }.take(90)
    }

    val title = when (mode) {
        ShopScreenMode.DEALERSHIP -> "Concessionária Nacional (${allVehicles.size} Máquinas)"
        ShopScreenMode.IMPLEMENTS -> "Loja de Implementos (${allImplements.size})"
        ShopScreenMode.USED -> "Mercado & Leilão de Usados"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = title,
            subtitle = "Caixa disponível: ${formatBrl(worldState.moneyBrl)}",
            onBack = onBack,
            actions = {
                if (compareIds.size >= 2) {
                    FilledTonalButton(onClick = { showCompare = true }) {
                        Icon(Icons.Rounded.CompareArrows, contentDescription = "Comparar", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${compareIds.size}")
                    }
                }
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(shape = MaterialTheme.shapes.large) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dealership_banner),
                        contentDescription = "Pátio da Concessionária Agrícola",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(125.dp),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Buscar por marca ou modelo...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shop_search_input"),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = if (mode == ShopScreenMode.IMPLEMENTS) selectedImpCat == null else selectedCategory == null,
                            onClick = { selectedCategory = null; selectedImpCat = null },
                            label = { Text("Todas Categorias") },
                        )
                        if (mode == ShopScreenMode.IMPLEMENTS) {
                            ImplementCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = selectedImpCat == cat,
                                    onClick = { selectedImpCat = cat },
                                    label = { Text(IMPLEMENT_CATEGORY_LABELS[cat] ?: cat.name) },
                                )
                            }
                        } else {
                            VehicleCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(VEHICLE_CATEGORY_LABELS[cat] ?: cat.name) },
                                )
                            }
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        brands.forEach { b ->
                            FilterChip(selected = selectedBrand == b, onClick = { selectedBrand = b }, label = { Text(b) })
                        }
                    }
                }
            }

            if (mode == ShopScreenMode.USED && !(worldState.auctionLots.isNullOrEmpty())) {
                item { Text("Lotes em Leilão Rotativo", style = MaterialTheme.typography.titleMedium, color = HarvestGold) }
                items(worldState.auctionLots, key = { it.uid }) { lot ->
                    val def = getVehicleDef(lot.defId)
                    val nextBid = lot.currentBidBrl + 2500.0
                    SectionCard(
                        title = "${def.brand} ${def.model} (${lot.hoursUsed}h • Cons. ${lot.conservationPct.roundToInt()}%)",
                        subtitle = "Maior lance: ${formatBrl(lot.currentBidBrl)} (${lot.highestBidder}) • Encerra Dia ${lot.endsAtDay}",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onBidAuction(lot.uid, nextBid) },
                                enabled = !lot.settled && worldState.moneyBrl >= nextBid,
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(Icons.Rounded.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (lot.settled) "Arrematado" else "Dar Lance ${formatBrl(nextBid)}")
                            }
                            OutlinedButton(onClick = { galleryVeh = def }, modifier = Modifier.height(48.dp)) {
                                Text("360°")
                            }
                        }
                    }
                }
            }

            if (mode == ShopScreenMode.IMPLEMENTS) {
                items(filteredImplements, key = { it.id }) { imp ->
                    SectionCard(
                        title = "${imp.brand} ${imp.model}",
                        subtitle = "${IMPLEMENT_CATEGORY_LABELS[imp.category]} • ${imp.requiredHp.roundToInt()}+ cv • Largura ${"%.1f".format(imp.workingWidthM)}m",
                    ) {
                        MachinePreviewCanvas(
                            implementDef = imp,
                            modifier = Modifier.fillMaxWidth().height(120.dp).clickable { galleryImp = imp },
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(formatBrl(imp.priceBrl), style = MaterialTheme.typography.titleMedium, color = HarvestGold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { galleryImp = imp }, modifier = Modifier.height(48.dp)) {
                                    Icon(Icons.Rounded.Visibility, contentDescription = "Ver 360", modifier = Modifier.size(16.dp))
                                }
                                Button(
                                    onClick = { onBuyImplement(imp.id) },
                                    enabled = worldState.moneyBrl >= imp.priceBrl,
                                    modifier = Modifier.height(48.dp),
                                ) { Text("Comprar") }
                            }
                        }
                    }
                }
            } else {
                items(filteredVehicles, key = { it.id }) { veh ->
                    val displayPrice = if (mode == ShopScreenMode.USED) veh.priceBrl * 0.68 else veh.priceBrl
                    val inCompare = compareIds.contains(veh.id)
                    SectionCard(
                        title = "${veh.brand} ${veh.model}",
                        subtitle = "${VEHICLE_CATEGORY_LABELS[veh.category]} • ${veh.horsepower.roundToInt()} cv • Ano ${veh.modelYear}",
                    ) {
                        MachinePreviewCanvas(
                            vehicleDef = veh,
                            modifier = Modifier.fillMaxWidth().height(130.dp).clickable { galleryVeh = veh },
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(formatBrl(displayPrice), style = MaterialTheme.typography.titleMedium, color = HarvestGold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        compareIds = if (inCompare) compareIds - veh.id else (compareIds + veh.id).takeLast(3)
                                    },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.CompareArrows,
                                        contentDescription = "Comparar máquina",
                                        tint = if (inCompare) HarvestGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                OutlinedButton(onClick = { galleryVeh = veh }, modifier = Modifier.height(48.dp)) {
                                    Text("360°")
                                }
                                Button(
                                    onClick = { onBuyVehicle(veh.id) },
                                    enabled = worldState.moneyBrl >= veh.priceBrl,
                                    modifier = Modifier.height(48.dp),
                                ) { Text("Comprar") }
                            }
                        }
                    }
                }
            }
        }
    }

    galleryVeh?.let { v ->
        MachineGalleryModal(
            vehicleDef = v,
            actionLabel = "Comprar ${formatBrl(v.priceBrl)}",
            onActionClick = { onBuyVehicle(v.id) },
            onDismiss = { galleryVeh = null },
        )
    }
    galleryImp?.let { i ->
        MachineGalleryModal(
            implementDef = i,
            actionLabel = "Comprar ${formatBrl(i.priceBrl)}",
            onActionClick = { onBuyImplement(i.id) },
            onDismiss = { galleryImp = null },
        )
    }
    if (showCompare && compareIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showCompare = false },
            title = { Text("Comparativo de Frota (${compareIds.size})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    compareIds.map { getVehicleDef(it) }.forEach { v ->
                        Surface(color = CabinCard, shape = MaterialTheme.shapes.small) {
                            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                Text("${v.brand} ${v.model}", style = MaterialTheme.typography.labelLarge)
                                Text("${v.horsepower.roundToInt()} cv • ${v.maxSpeedKmh.roundToInt()} km/h • ${formatBrl(v.priceBrl)}", style = MaterialTheme.typography.bodySmall, color = HarvestGold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCompare = false }) { Text("Fechar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { compareIds = emptyList(); showCompare = false }) { Text("Limpar") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HubScreen(
    packs: List<HubPackEntity>,
    favorites: List<String>,
    downloadStates: Map<String, ModDownloadProgress>,
    freeSpaceMb: Double = 2048.0,
    onStartDownload: (String) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onUpdatePack: (String) -> Unit,
    onUninstallPack: (String) -> Unit,
    onToggleEnabled: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit,
    onImportCustomJson: (String) -> Pair<Boolean, String>,
    onBack: () -> Unit,
) {
    var category by remember { mutableStateOf("Todos") }
    var brand by remember { mutableStateOf("Todas") }
    var onlyInstalled by remember { mutableStateOf(false) }
    var onlyFavorites by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var customJson by remember { mutableStateOf("") }
    var importResult by remember { mutableStateOf<String?>(null) }

    val filtered = remember(packs, favorites, category, brand, onlyInstalled, onlyFavorites) {
        packs.filter { p ->
            val meta = getHubMeta(p.packId)
            matchesHubCategory(p, category) &&
                (brand == "Todas" || meta.brand.contains(brand, true)) &&
                (!onlyInstalled || p.installed) &&
                (!onlyFavorites || favorites.contains(p.packId))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Farm Brasil ModHub (${packs.size})",
            subtitle = "${packs.count { it.installed }} instalados • Espaço livre: ${"%.0f".format(freeSpaceMb)} MB",
            onBack = onBack,
            actions = {
                FilledTonalButton(onClick = { showImportDialog = true }) { Text("+ JSON") }
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = onlyInstalled, onClick = { onlyInstalled = !onlyInstalled }, label = { Text("Instalados (${packs.count { it.installed }})") })
                        FilterChip(selected = onlyFavorites, onClick = { onlyFavorites = !onlyFavorites }, label = { Text("Favoritos (${favorites.size})") })
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HUB_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                modifier = Modifier.testTag("hub_cat_$cat"),
                            )
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HUB_BRAND_FILTERS.forEach { br ->
                            FilterChip(selected = brand == br, onClick = { brand = br }, label = { Text(br) })
                        }
                    }
                }
            }

            items(filtered, key = { it.packId }) { pack ->
                val meta = getHubMeta(pack.packId)
                val valRes = validateModPack(pack, packs, "android", freeSpaceMb)
                val dl = downloadStates[pack.packId]
                val isFav = favorites.contains(pack.packId)
                val hasUpdate = hasHubUpdateAvailable(pack)
                val previewColor = remember(meta.previewColorHex) {
                    runCatching { Color(android.graphics.Color.parseColor(meta.previewColorHex)) }
                        .getOrDefault(AgroSprout)
                }
                val statusLabel = when {
                    pack.installed && pack.enabled -> "Instalado e Ativo"
                    pack.installed && !pack.enabled -> "Instalado (Desativado)"
                    else -> valRes.status.label
                }

                SectionCard(
                    title = pack.name,
                    subtitle = "${pack.category} • ${meta.brand} • v${pack.version} • ${"%.1f".format(pack.sizeMb)} MB",
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(previewColor)
                        )
                        Text(pack.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatBadge(
                            label = statusLabel,
                            color = when {
                                pack.installed && pack.enabled -> StatusGreen
                                pack.installed && !pack.enabled -> StatusWarning
                                valRes.ok -> StatusInfo
                                else -> StatusWarning
                            },
                        )
                        StatBadge("Autor: ${pack.author}", MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (valRes.notes.isNotEmpty()) {
                        Text(valRes.notes.joinToString(" "), style = MaterialTheme.typography.labelSmall, color = StatusWarning)
                    }
                    if (dl?.status == ModDownloadStatus.ERROR && !dl.errorMessage.isNullOrBlank()) {
                        Text(dl.errorMessage, style = MaterialTheme.typography.labelSmall, color = StatusDanger)
                    }
                    if (dl != null && (dl.status == ModDownloadStatus.DOWNLOADING || dl.status == ModDownloadStatus.PAUSED)) {
                        LinearProgressIndicator(
                            progress = { (dl.progressPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = HarvestGold,
                        )
                        Text("${dl.progressPct}% • ${"%.1f".format(dl.downloadedMb)}/${"%.1f".format(dl.totalMb)} MB", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { onToggleFavorite(pack.packId) }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (isFav) StatusDanger else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when {
                            dl?.status == ModDownloadStatus.DOWNLOADING -> {
                                OutlinedButton(onClick = { onPauseDownload(pack.packId) }, modifier = Modifier.weight(1f).height(48.dp)) {
                                    Icon(Icons.Rounded.Pause, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Pausar")
                                }
                                IconButton(onClick = { onCancelDownload(pack.packId) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Rounded.Cancel, contentDescription = "Cancelar", tint = StatusDanger)
                                }
                            }
                            dl?.status == ModDownloadStatus.PAUSED -> {
                                Button(onClick = { onResumeDownload(pack.packId) }, modifier = Modifier.weight(1f).height(48.dp)) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Retomar")
                                }
                                IconButton(onClick = { onCancelDownload(pack.packId) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Rounded.Cancel, contentDescription = "Cancelar", tint = StatusDanger)
                                }
                            }
                            pack.installed -> {
                                FilledTonalButton(
                                    onClick = { onToggleEnabled(pack.packId) },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("hub_toggle_${pack.packId}"),
                                ) {
                                    Icon(
                                        Icons.Rounded.PowerSettingsNew,
                                        contentDescription = null,
                                        tint = if (pack.enabled) StatusGreen else StatusWarning,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (pack.enabled) "Desativar" else "Ativar")
                                }
                                if (hasUpdate) {
                                    Button(onClick = { onUpdatePack(pack.packId) }, modifier = Modifier.weight(1f).height(48.dp)) {
                                        Text("Atualizar v${meta.latestVersion}")
                                    }
                                }
                                OutlinedButton(onClick = { onUninstallPack(pack.packId) }, modifier = Modifier.height(48.dp)) {
                                    Text("Remover", color = StatusDanger)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { onStartDownload(pack.packId) },
                                    enabled = valRes.ok,
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("hub_install_${pack.packId}"),
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (valRes.ok) "Baixar & Instalar" else "Arquivo Necessário")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar Mod Manifesto JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customJson,
                        onValueChange = { customJson = it },
                        label = { Text("Cole o JSON do mod") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    importResult?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = HarvestGold) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val (_, msg) = onImportCustomJson(customJson)
                    importResult = msg
                }) { Text("Validar & Importar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportDialog = false }) { Text("Fechar") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: GameSettings,
    accountUser: FarmAccountUser? = null,
    syncStatus: AccountSyncStatus = AccountSyncStatus(),
    hubPacks: List<HubPackEntity> = emptyList(),
    onApplyPreset: (GraphicsPreset) -> Unit,
    onDetectRecommended: () -> Unit = {},
    onUpdateCustomGraphics: ((GraphicsConfig) -> GraphicsConfig) -> Unit,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onUpdateHudLayout: (HudLayoutConfig) -> Unit,
    onExportSyncBundle: () -> String = { "" },
    onRestoreSyncBundle: (String, String, String) -> Pair<Boolean, String> = { _, _, _ -> false to "" },
    onSignOut: () -> Unit = {},
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val g = settings.customGraphics
    val safeConfig = remember(g) { clampGraphicsConfigForDevice(g) }
    val layout = settings.hudLayout ?: HudLayoutConfig()
    val channels = settings.audioChannels
    val externalGfxPacks = remember(hubPacks) { getExternalGraphicsPacks(hubPacks) }

    var showPersonalCode by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreUsername by remember { mutableStateOf(accountUser?.displayName ?: "") }
    var restoreCode by remember { mutableStateOf("") }
    var restoreJson by remember { mutableStateOf("") }

    val exportBundleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val payload = onExportSyncBundle()
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
                syncMessage = "Pacote da conta exportado para transferência (Celular/PC)!"
            }.onFailure {
                syncMessage = "Erro ao exportar pacote: ${it.localizedMessage}"
            }
        }
    }

    val importBundleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                restoreJson = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
                showRestoreDialog = true
            }.onFailure {
                syncMessage = "Erro ao ler arquivo: ${it.localizedMessage}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Configurações, Conta & Gráficos",
            subtitle = "Perfil gráfico: ${GRAPHICS_PRESET_META[settings.graphicsPreset]?.displayName ?: settings.graphicsPreset.name}",
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Conta do Jogador & Sincronização Celular/PC
            if (accountUser != null) {
                SectionCard(
                    title = "Conta Farm Brasil & Sincronização (Celular / PC)",
                    subtitle = "Jogador: ${accountUser.displayName}",
                ) {
                    Surface(
                        color = WoodDark,
                        border = BorderStroke(1.dp, WoodTrim.copy(alpha = 0.7f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Seu código pessoal:", style = MaterialTheme.typography.labelSmall, color = AgroSprout)
                                val displayedCode = if (showPersonalCode) {
                                    accountUser.personalCode
                                } else {
                                    "FB-••••-${accountUser.personalCode.takeLast(4)}"
                                }
                                Text(
                                    text = displayedCode,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = HarvestGold,
                                    modifier = Modifier.testTag("settings_personal_code_text"),
                                )
                            }
                            IconButton(
                                onClick = { showPersonalCode = !showPersonalCode },
                                modifier = Modifier.size(48.dp).testTag("settings_toggle_code_btn"),
                            ) {
                                Icon(
                                    imageVector = if (showPersonalCode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPersonalCode) "Ocultar código" else "Consultar código",
                                    tint = HarvestGold,
                                )
                            }
                            IconButton(
                                onClick = {
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Código Pessoal Farm Brasil", accountUser.personalCode))
                                    Toast.makeText(ctx, "Código pessoal copiado!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(48.dp).testTag("settings_copy_code_btn"),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copiar código pessoal", tint = AgroSprout)
                            }
                        }
                    }

                    Text(
                        text = syncStatus.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { exportBundleLauncher.launch("farm_brasil_conta_${accountUser.displayName.replace(" ", "_")}.json") },
                            modifier = Modifier.height(48.dp).testTag("settings_export_sync_btn"),
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Exportar Pacote Celular/PC")
                        }
                        OutlinedButton(
                            onClick = { importBundleLauncher.launch(arrayOf("application/json", "text/plain")) },
                            modifier = Modifier.height(48.dp).testTag("settings_restore_sync_btn"),
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restaurar Pacote")
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.height(48.dp).testTag("settings_sign_out_btn"),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Trocar / Sair da Conta", color = StatusDanger)
                        }
                    }

                    syncMessage?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = HarvestGold)
                    }
                }
            }

            SectionCard(
                title = "Gráficos Mega-Realistas & Desempenho",
                subtitle = "Textura ativa: ${safeConfig.textureResolutionPx ?: 2048}x${safeConfig.textureResolutionPx ?: 2048} (${safeConfig.textureTierLabel})",
            ) {
                FilledTonalButton(
                    onClick = onDetectRecommended,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("detect_recommended_graphics_btn"),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = HarvestGold)
                    Spacer(Modifier.width(8.dp))
                    Text("Detectar Configuração Recomendada", fontWeight = FontWeight.Bold)
                }

                Text("Predefinições de Qualidade:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GraphicsPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = settings.graphicsPreset == preset,
                            onClick = { onApplyPreset(preset) },
                            label = { Text(GRAPHICS_PRESET_META[preset]?.displayName?.substringBefore(" (") ?: preset.name) },
                            modifier = Modifier.testTag("graphics_preset_${preset.name}"),
                        )
                    }
                }

                Text("Resolução de Texturas:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TEXTURE_RESOLUTION_OPTIONS.forEach { (px, label) ->
                        val activePx = g.textureResolutionPx ?: 2048
                        FilterChip(
                            selected = activePx == px,
                            onClick = {
                                val tier = when {
                                    px <= 512 -> "512p"
                                    px <= 1024 -> "1K"
                                    px <= 2048 -> "2K"
                                    else -> "4K"
                                }
                                onUpdateCustomGraphics { it.copy(textureResolutionPx = px, textureTierLabel = tier) }
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("texture_tier_$px"),
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sombras & Reflexos Realistas")
                    Switch(
                        checked = g.shadowQuality > 0,
                        onCheckedChange = { on ->
                            onUpdateCustomGraphics {
                                it.copy(shadowQuality = if (on) 3 else 0, reflectionQuality = if (on) 3 else 0)
                            }
                        },
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Proteção Térmica / Limite RAM Mobile")
                    Switch(
                        checked = g.adaptiveMobile != false,
                        onCheckedChange = { on -> onUpdateCustomGraphics { it.copy(adaptiveMobile = on) } },
                    )
                }

                SettingSlider("Distância de Visão (${g.drawDistance.roundToInt()}m)", g.drawDistance.toFloat(), 100f..640f) { v ->
                    onUpdateCustomGraphics { it.copy(drawDistance = v.toDouble()) }
                }
                SettingSlider("Vegetação & Lavouras (${(g.vegetationDensity * 100).roundToInt()}%)", g.vegetationDensity.toFloat(), 0.2f..1.0f) { v ->
                    onUpdateCustomGraphics { it.copy(vegetationDensity = v.toDouble()) }
                }
                SettingSlider("Clima, Nuvens & Poeira (${(g.weatherEffectsDensity * 100).roundToInt()}%)", g.weatherEffectsDensity.toFloat(), 0.0f..1.5f) { v ->
                    onUpdateCustomGraphics { it.copy(weatherEffectsDensity = v.toDouble(), particleQuality = v.toDouble()) }
                }
                SettingSlider("Saturação (${"%.2f".format(g.saturation)})", g.saturation.toFloat(), 0.8f..1.35f) { v ->
                    onUpdateCustomGraphics { it.copy(saturation = v.toDouble()) }
                }

                if (externalGfxPacks.isNotEmpty()) {
                    Text("Pacotes Gráficos Externos (${externalGfxPacks.size}):", style = MaterialTheme.typography.labelLarge, color = AgroSprout)
                    externalGfxPacks.forEach { gfxPack ->
                        val activeText = when {
                            gfxPack.installed && gfxPack.enabled -> "Ativo"
                            gfxPack.installed -> "Desativado"
                            else -> "Aguardando arquivo no ModHub"
                        }
                        Text(
                            text = "• ${gfxPack.name}: $activeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gfxPack.installed && gfxPack.enabled) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionCard(title = "HUD, Direção & Controles") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SteeringControlStyle.entries.forEach { st ->
                        FilterChip(
                            selected = settings.steeringStyle == st,
                            onClick = { onUpdateSettings { it.copy(steeringStyle = st) } },
                            label = { Text(if (st == SteeringControlStyle.BOTOES) "Setas Esq/Dir" else "Volante Virtual") },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PADRAO", "COMPACTO", "CANHOTO").forEach { p ->
                        FilterChip(
                            selected = layout.preset == p,
                            onClick = { onUpdateHudLayout(layout.copy(preset = p)) },
                            label = { Text(p) },
                        )
                    }
                }
                SettingSlider("Velocidade de Esterçamento (${"%.2f".format(settings.steerSpeedMultiplier)}x)", settings.steerSpeedMultiplier.toFloat(), 0.5f..2.0f) { v ->
                    onUpdateSettings { it.copy(steerSpeedMultiplier = v.toDouble()) }
                }
                SettingSlider("Tamanho dos Botões HUD (${(layout.scale * 100).roundToInt()}%)", layout.scale.toFloat(), 0.8f..1.3f) { v ->
                    onUpdateHudLayout(layout.copy(scale = v.toDouble()))
                }
                SettingSlider("Posição Vertical Controles (${layout.controlOffsetYDp.roundToInt()} dp)", layout.controlOffsetYDp.toFloat(), -60f..60f) { v ->
                    onUpdateHudLayout(layout.copy(controlOffsetYDp = v.toDouble()))
                }
                SettingSlider("Opacidade Controles (${(settings.controlOpacity * 100).roundToInt()}%)", settings.controlOpacity.toFloat(), 0.35f..1.0f) { v ->
                    onUpdateSettings { it.copy(controlOpacity = v.toDouble()) }
                }
                SettingSlider("Distância Câmera (${"%.2f".format(settings.cameraDistance)}x)", settings.cameraDistance.toFloat(), 0.6f..1.6f) { v ->
                    onUpdateSettings { it.copy(cameraDistance = v.toDouble()) }
                }
            }

            SectionCard(title = "Áudio Procedural (9 Canais Independentes)") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Som Geral Ativado")
                    Switch(checked = settings.soundEnabled, onCheckedChange = { enabled -> onUpdateSettings { it.copy(soundEnabled = enabled) } })
                }
                SettingSlider("Master", channels.master.toFloat(), 0f..1f) { v -> onUpdateSettings { it.copy(audioChannels = channels.copy(master = v.toDouble())) } }
                SettingSlider("Motores Diesel", channels.engine.toFloat(), 0f..1f) { v -> onUpdateSettings { it.copy(audioChannels = channels.copy(engine = v.toDouble())) } }
                SettingSlider("Implementos", channels.implements.toFloat(), 0f..1f) { v -> onUpdateSettings { it.copy(audioChannels = channels.copy(implements = v.toDouble())) } }
                SettingSlider("Clima & Chuva", channels.weather.toFloat(), 0f..1f) { v -> onUpdateSettings { it.copy(audioChannels = channels.copy(weather = v.toDouble())) } }
                SettingSlider("Música / Viola", channels.music.toFloat(), 0f..1f) { v -> onUpdateSettings { it.copy(audioChannels = channels.copy(music = v.toDouble())) } }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Confirmar Restauração de Conta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Informe o Nome do jogador e Código pessoal do pacote:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = restoreUsername,
                        onValueChange = { restoreUsername = it },
                        label = { Text("Nome do jogador") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = restoreCode,
                        onValueChange = { restoreCode = it.uppercase() },
                        label = { Text("Código pessoal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val (_, msg) = onRestoreSyncBundle(restoreUsername, restoreCode, restoreJson)
                    syncMessage = msg
                    showRestoreDialog = false
                }) { Text("Restaurar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SettingSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
fun ContentManagerScreen(
    careerSlots: List<CareerSlotMeta?>,
    worldState: WorldState,
    cloudSaves: List<CloudSaveSummary>,
    isSyncingCloud: Boolean,
    onLoadSlot: (Int) -> Unit,
    onDeleteSlot: (Int) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String, Int) -> Pair<Boolean, String>,
    onUploadCurrentToCloud: () -> Unit,
    onDownloadCloudSlot: (Int) -> Unit,
    onDeleteCloudSlot: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var targetImportSlot by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val payload = onExportJson()
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
                statusText = "Backup JSON exportado com sucesso!"
            }.onFailure {
                statusText = "Erro ao exportar: ${it.localizedMessage}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
                val (_, msg) = onImportJson(raw, targetImportSlot)
                statusText = msg
            }.onFailure {
                statusText = "Erro ao importar: ${it.localizedMessage}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "20 Espaços de Carreira & Nuvem",
            subtitle = "Backup JSON SHA-256 + Firebase Firestore",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CloudSavesSection(
                    cloudSaves = cloudSaves,
                    isSyncing = isSyncingCloud,
                    activeSlotIndex = worldState.slotIndex,
                    onUploadCurrent = onUploadCurrentToCloud,
                    onDownloadSlot = onDownloadCloudSlot,
                    onDeleteSlot = onDeleteCloudSlot,
                )
            }
            item {
                SectionCard(title = "Backup Portátil JSON (Exportar / Importar)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportLauncher.launch("farm_brasil_slot_${worldState.slotIndex + 1}.json") },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Exportar JSON")
                        }
                        FilledTonalButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Importar p/ Slot ${targetImportSlot + 1}")
                        }
                    }
                    statusText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = HarvestGold) }
                }
            }
            item { Text("Armazenamento Local (20 Espaços)", style = MaterialTheme.typography.titleMedium) }
            items(20) { idx ->
                val meta = careerSlots.getOrNull(idx)
                SectionCard(
                    title = if (meta != null) "Espaço ${idx + 1}: ${meta.farmName}" else "Espaço ${idx + 1}: Vazio",
                    subtitle = if (meta != null) "${meta.mapName} • Dia ${meta.day} • ${formatBrl(meta.moneyBrl)}" else "Disponível para nova carreira ou importação",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (meta != null) {
                            Button(onClick = { onLoadSlot(idx) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Carregar") }
                            IconButton(onClick = { onDeleteSlot(idx) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Apagar", tint = StatusDanger)
                            }
                        } else {
                            OutlinedButton(onClick = { targetImportSlot = idx }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text(if (targetImportSlot == idx) "Alvo selecionado para importação" else "Selecionar para importar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Créditos & Arquitetura Técnica",
            subtitle = "${FEATURE_REGISTRY.size} módulos implementados e testados",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(FEATURE_REGISTRY, key = { it.section }) { entry ->
                SectionCard(
                    title = "${entry.section} • ${entry.title}",
                    subtitle = "Status: ${entry.status.label}",
                ) {
                    Text(entry.details, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
