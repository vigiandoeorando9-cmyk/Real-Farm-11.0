package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ESTABLISHMENTS
import com.example.domain.LANDMARKS
import com.example.domain.SoilState
import com.example.domain.WorldState
import com.example.domain.formatLandArea
import com.example.domain.getCropById
import com.example.domain.getMapById
import com.example.domain.getVehicleDef
import com.example.simulation.formatBrl
import com.example.state.InGameTab
import com.example.ui.theme.AgroSprout
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusInfo
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class MapHeatmapMode(val label: String) {
    NORMAL("Padrão"),
    PRODUTIVIDADE("Fertilidade N-P-K"),
    COMPACTACAO("Compactação"),
    UMIDADE("Umidade / Irrigação"),
}

private data class SelectedMapPin(
    val title: String,
    val subtitle: String,
    val worldX: Double,
    val worldZ: Double,
    val vehicleUid: String? = null,
    val plotId: Int? = null,
    val canBuyPlot: Boolean = false,
    val buyPriceBrl: Double = 0.0,
    val systemTab: InGameTab? = null,
    val systemTabLabel: String? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullMapScreen(
    worldState: WorldState,
    onClose: () -> Unit,
    onTeleport: (Double, Double, String) -> Unit,
    onSwitchVehicle: (String) -> Unit,
    onBuyPlot: (Int) -> Unit,
    onLeasePlot: (Int, String) -> Unit,
    onOpenSystemTab: (InGameTab) -> Unit = {},
) {
    var heatmap by remember { mutableStateOf(MapHeatmapMode.NORMAL) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var selectedPin by remember { mutableStateOf<SelectedMapPin?>(null) }

    val mapDef = remember(worldState.mapId) { getMapById(worldState.mapId) }
    val gridSide = worldState.mapGridSize ?: 128
    val cellSizeM = com.example.simulation.getCellSize(gridSide).toFloat()
    val worldSpanM = gridSide * cellSizeM
    val halfM = worldSpanM / 2f

    val activeVehRef = worldState.activeVehicleUid?.let { uid -> worldState.vehicles.find { it.uid == uid } }
    val refX = activeVehRef?.x ?: worldState.player.x
    val refZ = activeVehRef?.z ?: worldState.player.z
    fun distText(tx: Double, tz: Double): String = "a ${hypot(tx - refX, tz - refZ).roundToInt()}m"

    val interactiveMapCanvas: @Composable (Modifier) -> Unit = { canvasMod ->
        Canvas(
            modifier = canvasMod
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.8f, 4.5f)
                        panX = (panX + pan.x).coerceIn(-size.width * 0.7f, size.width * 0.7f)
                        panY = (panY + pan.y).coerceIn(-size.height * 0.7f, size.height * 0.7f)
                    }
                }
                .pointerInput(worldState.plots, worldState.vehicles, gridSide, zoom, panX, panY) {
                    detectTapGestures { tapOffset ->
                        val side = min(size.width, size.height) * zoom
                        val originX = (size.width - side) / 2f + panX
                        val originY = (size.height - side) / 2f + panY
                        val wx = (((tapOffset.x - originX) / side) * worldSpanM - halfM).toDouble()
                        val wz = (((tapOffset.y - originY) / side) * worldSpanM - halfM).toDouble()

                        var bestDist = 36.0
                        var bestPin: SelectedMapPin? = null

                        for (v in worldState.vehicles) {
                            val d = hypot(v.x - wx, v.z - wz)
                            if (d < bestDist) {
                                bestDist = d
                                val def = getVehicleDef(v.defId)
                                bestPin = SelectedMapPin(
                                    title = "${def.brand} ${def.model}",
                                    subtitle = "Combustível ${v.fuelL.roundToInt()} L • Cons. ${v.conservationPct.roundToInt()}% • ${distText(v.x, v.z)}",
                                    worldX = v.x, worldZ = v.z, vehicleUid = v.uid,
                                )
                            }
                        }
                        for (est in ESTABLISHMENTS) {
                            val ex = est.scaledWorldX(gridSide)
                            val ez = est.scaledWorldZ(gridSide)
                            val d = hypot(ex - wx, ez - wz)
                            if (d < bestDist) {
                                bestDist = d
                                bestPin = SelectedMapPin(
                                    title = est.name,
                                    subtitle = "${est.type} • ${est.openHour}h–${est.closeHour}h • ${distText(ex, ez)}",
                                    worldX = ex,
                                    worldZ = ez,
                                    systemTab = InGameTab.ECONOMIA,
                                    systemTabLabel = "Abrir Balança/Venda",
                                )
                            }
                        }
                        for (lm in LANDMARKS) {
                            val lx = lm.scaledWorldX(gridSide)
                            val lz = lm.scaledWorldZ(gridSide)
                            val d = hypot(lx - wx, lz - wz)
                            if (d < bestDist) {
                                bestDist = d
                                val (sysTab, sysLbl) = when (lm.id) {
                                    "lm_concessionaria" -> InGameTab.LOJA to "Abrir Concessionária"
                                    "lm_loja_insumos" -> InGameTab.EQUIPE to "Abrir Loja Agro"
                                    "lm_banco" -> InGameTab.ECONOMIA to "Abrir Banco"
                                    "lm_oficina_vila" -> InGameTab.OFICINA to "Abrir Oficina"
                                    "lm_ponto_animais" -> InGameTab.PECUARIA to "Abrir Pecuária"
                                    "lm_madeireira", "lm_materiais" -> InGameTab.CONSTRUCAO to "Abrir Obras"
                                    "lm_garagem" -> InGameTab.FROTA to "Abrir Garagem"
                                    "lm_posto_vila" -> null to null
                                    else -> InGameTab.ECONOMIA to "Abrir Mercado"
                                }
                                bestPin = SelectedMapPin(
                                    title = lm.name,
                                    subtitle = "${lm.description} (${distText(lx, lz)})",
                                    worldX = lx,
                                    worldZ = lz,
                                    systemTab = sysTab,
                                    systemTabLabel = sysLbl,
                                )
                            }
                        }
                        if (bestPin == null) {
                            val gx = ((wx + halfM) / cellSizeM).toInt().coerceIn(0, gridSide - 1)
                            val gz = ((wz + halfM) / cellSizeM).toInt().coerceIn(0, gridSide - 1)
                            val cell = worldState.cells.getOrNull(gz * gridSide + gx)
                            val plot = cell?.let { c -> worldState.plots.find { it.id == c.plotId } }
                            if (plot != null) {
                                val unit = worldState.landUnit ?: com.example.domain.LandUnit.HECTARE
                                val cropLabel = cell.cropId?.let { getCropById(it).name } ?: "Sem cultura"
                                val ownerStatus = when {
                                    plot.isPreservation -> "Reserva Legal (APP)"
                                    plot.owned -> "Próprio"
                                    plot.leased -> "Arrendado"
                                    else -> "À Venda (${formatBrl(plot.buyPriceBrl)})"
                                }
                                bestPin = SelectedMapPin(
                                    title = plot.name,
                                    subtitle = "${formatLandArea(plot.areaHa, unit)} • $ownerStatus • $cropLabel",
                                    worldX = wx, worldZ = wz,
                                    plotId = plot.id,
                                    canBuyPlot = !plot.owned && !plot.isPreservation,
                                    buyPriceBrl = plot.buyPriceBrl,
                                )
                            }
                        }
                        selectedPin = bestPin
                    }
                }
        ) {
            val side = min(size.width, size.height) * zoom
            val originX = (size.width - side) / 2f + panX
            val originY = (size.height - side) / 2f + panY

            fun toPx(wx: Double, wz: Double): Offset = Offset(
                originX + ((wx.toFloat() + halfM) / worldSpanM) * side,
                originY + ((wz.toFloat() + halfM) / worldSpanM) * side,
            )

            val step = max(1, gridSide / 64)
            val cw = (step.toFloat() / gridSide) * side + 1f
            for (gz in 0 until gridSide step step) {
                for (gx in 0 until gridSide step step) {
                    val c = worldState.cells.getOrNull(gz * gridSide + gx) ?: continue
                    val col = when (heatmap) {
                        MapHeatmapMode.NORMAL -> when (c.soilState) {
                            SoilState.AGUA_RIO -> Color(0xFF1F6F8B)
                            SoilState.ESTRADA_ASFALTO -> Color(0xFF3A3E42)
                            SoilState.ESTRADA_TERRA -> Color(0xFF8C6239)
                            SoilState.PRONTO_COLHEITA -> HarvestGold
                            SoilState.CRESCENDO, SoilState.SEMEADO -> Color(0xFF4CAF50)
                            SoilState.ARADO, SoilState.SUBSOLADO -> Color(0xFF7F3E28)
                            SoilState.FLORESTA_NATIVA, SoilState.AREA_PRESERVACAO -> Color(0xFF153D1E)
                            else -> Color(0xFF28482C)
                        }
                        MapHeatmapMode.PRODUTIVIDADE -> Color(
                            red = (1.0 - c.fertility).toFloat().coerceIn(0f, 1f),
                            green = c.fertility.toFloat().coerceIn(0f, 1f),
                            blue = 0.18f,
                        )
                        MapHeatmapMode.COMPACTACAO -> Color(
                            red = c.compaction.toFloat().coerceIn(0f, 1f),
                            green = (1.0 - c.compaction).toFloat().coerceIn(0f, 1f),
                            blue = 0.2f,
                        )
                        MapHeatmapMode.UMIDADE -> Color(
                            red = 0.15f,
                            green = (0.3f + c.moisture * 0.5f).toFloat().coerceIn(0f, 1f),
                            blue = c.moisture.toFloat().coerceIn(0f, 1f),
                        )
                    }
                    drawRect(
                        color = col,
                        topLeft = Offset(originX + (gx.toFloat() / gridSide) * side, originY + (gz.toFloat() / gridSide) * side),
                        size = Size(cw, cw),
                    )
                }
            }

            LANDMARKS.forEach { lm ->
                drawCircle(color = AgroSprout, radius = 8f, center = toPx(lm.scaledWorldX(gridSide), lm.scaledWorldZ(gridSide)))
            }
            ESTABLISHMENTS.forEach { est ->
                drawCircle(color = StatusInfo, radius = 8f, center = toPx(est.scaledWorldX(gridSide), est.scaledWorldZ(gridSide)))
            }
            worldState.vehicles.forEach { v ->
                val p = toPx(v.x, v.z)
                val act = v.uid == worldState.activeVehicleUid
                drawCircle(color = if (act) HarvestGold else Color.White, radius = if (act) 9f else 6f, center = p)
                if (act) drawCircle(color = Color.Black, radius = 11f, center = p, style = Stroke(2f))
            }
            if (worldState.activeVehicleUid == null) {
                val pp = toPx(worldState.player.x, worldState.player.z)
                drawCircle(color = Color(0xFF00E676), radius = 9f, center = pp)
                drawCircle(color = Color.White, radius = 11f, center = pp, style = Stroke(2f))
            }
            selectedPin?.let { sp ->
                drawCircle(color = StatusDanger, radius = 16f, center = toPx(sp.worldX, sp.worldZ), style = Stroke(3f))
            }
        }
    }

    val poiButtons: @Composable () -> Unit = {
        fun selectLandmark(id: String, tab: InGameTab?, tabLbl: String?) {
            val lm = LANDMARKS.find { it.id == id } ?: return
            val lx = lm.scaledWorldX(gridSide)
            val lz = lm.scaledWorldZ(gridSide)
            selectedPin = SelectedMapPin(
                title = lm.name,
                subtitle = "${lm.description} (${distText(lx, lz)})",
                worldX = lx,
                worldZ = lz,
                systemTab = tab,
                systemTabLabel = tabLbl,
            )
        }
        fun selectEst(id: String) {
            val est = ESTABLISHMENTS.find { it.id == id } ?: return
            val ex = est.scaledWorldX(gridSide)
            val ez = est.scaledWorldZ(gridSide)
            selectedPin = SelectedMapPin(
                title = est.name,
                subtitle = "${est.type} • ${est.openHour}h–${est.closeHour}h (${distText(ex, ez)})",
                worldX = ex,
                worldZ = ez,
                systemTab = InGameTab.ECONOMIA,
                systemTabLabel = "Abrir Balança/Venda",
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilledTonalButton(
                onClick = { selectLandmark("lm_banco", InGameTab.ECONOMIA, "Abrir Banco") },
                modifier = Modifier.height(48.dp).testTag("map_poi_banco"),
            ) {
                Icon(Icons.Rounded.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Banco", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectLandmark("lm_concessionaria", InGameTab.LOJA, "Abrir Concessionária") },
                modifier = Modifier.height(48.dp).testTag("map_poi_loja"),
            ) {
                Icon(Icons.Rounded.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Concessionária", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectLandmark("lm_loja_insumos", InGameTab.EQUIPE, "Abrir Loja Agro") },
                modifier = Modifier.height(48.dp).testTag("map_poi_loja_agro"),
            ) {
                Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Loja Agro", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectLandmark("lm_oficina_vila", InGameTab.OFICINA, "Abrir Oficina") },
                modifier = Modifier.height(48.dp).testTag("map_poi_oficina"),
            ) {
                Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Oficina", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectLandmark("lm_posto_vila", null, null) },
                modifier = Modifier.height(48.dp).testTag("map_poi_posto"),
            ) {
                Icon(Icons.Rounded.LocalGasStation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Posto Diesel", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectEst("coop_central") },
                modifier = Modifier.height(48.dp).testTag("map_poi_cooperativa"),
            ) {
                Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cooperativa", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectEst("cerealista_sul") },
                modifier = Modifier.height(48.dp).testTag("map_poi_cerealista"),
            ) {
                Icon(Icons.Rounded.Warehouse, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cerealista", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectLandmark("lm_madeireira", InGameTab.CONSTRUCAO, "Abrir Obras") },
                modifier = Modifier.height(48.dp).testTag("map_poi_madeireira"),
            ) {
                Text("Madeireira", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectLandmark("lm_materiais", InGameTab.CONSTRUCAO, "Abrir Materiais") },
                modifier = Modifier.height(48.dp).testTag("map_poi_materiais"),
            ) {
                Text("Materiais", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectLandmark("lm_ponto_animais", InGameTab.PECUARIA, "Abrir Pecuária") },
                modifier = Modifier.height(48.dp).testTag("map_poi_animais"),
            ) {
                Icon(Icons.Rounded.Pets, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Animais", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectLandmark("lm_armazem_vila", InGameTab.ECONOMIA, "Abrir Armazém") },
                modifier = Modifier.height(48.dp).testTag("map_poi_armazem"),
            ) {
                Text("Armazém", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(
                onClick = { selectLandmark("lm_garagem", InGameTab.FROTA, "Abrir Garagem") },
                modifier = Modifier.height(48.dp).testTag("map_poi_garagem"),
            ) {
                Icon(Icons.Rounded.Garage, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Garagem", style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    val selectedPinCard: @Composable (SelectedMapPin) -> Unit = { pin ->
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, AgroSprout.copy(alpha = 0.5f)),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(pin.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(pin.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { selectedPin = null }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar seleção")
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = { onTeleport(pin.worldX, pin.worldZ, pin.title) },
                        modifier = Modifier.height(48.dp).testTag("map_teleport_btn"),
                    ) {
                        Icon(Icons.Rounded.NearMe, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ir Até Aqui")
                    }
                    pin.systemTab?.let { targetTab ->
                        FilledTonalButton(
                            onClick = { onOpenSystemTab(targetTab) },
                            modifier = Modifier.height(48.dp).testTag("map_open_system_btn"),
                        ) {
                            Text(pin.systemTabLabel ?: "Abrir", maxLines = 1)
                        }
                    }
                    pin.vehicleUid?.let { uid ->
                        FilledTonalButton(
                            onClick = {
                                onSwitchVehicle(uid)
                                onClose()
                            },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Dirigir")
                        }
                    }
                    if (pin.canBuyPlot && pin.plotId != null) {
                        Button(
                            onClick = { onBuyPlot(pin.plotId) },
                            colors = ButtonDefaults.buttonColors(containerColor = HarvestGold, contentColor = Color(0xFF0E1310)),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Comprar")
                        }
                        FilledTonalButton(
                            onClick = { onLeasePlot(pin.plotId, "SAFRAL") },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text("Arrendar")
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1310))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val isLandscape = maxWidth >= 560.dp
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Map Canvas Pane
                Box(modifier = Modifier.weight(1.55f).fillMaxHeight()) {
                    interactiveMapCanvas(Modifier.fillMaxSize())
                    Surface(
                        color = Color(0xFF0E1310).copy(alpha = 0.80f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    ) {
                        Text(
                            text = "Arraste para mover • Pinça p/ zoom • Toque no talhão ou ícone",
                            style = MaterialTheme.typography.labelSmall,
                            color = AgroSprout,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                // Right Controls & Selected Pin Pane (clean landscape UX without cutoffs)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF141C17))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mapa • ${mapDef.stateName}",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("fullmap_region_title"),
                            )
                            Text(
                                text = "${mapDef.regionName} (${gridSide}×${gridSide})",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgroSprout,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { zoom = (zoom + 0.4f).coerceAtMost(4.5f) },
                                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
                            ) { Icon(Icons.Rounded.ZoomIn, contentDescription = "Aproximar mapa") }
                            IconButton(
                                onClick = { zoom = (zoom - 0.4f).coerceAtLeast(0.8f) },
                                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
                            ) { Icon(Icons.Rounded.ZoomOut, contentDescription = "Afastar mapa") }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(48.dp).background(StatusDanger.copy(alpha = 0.9f), MaterialTheme.shapes.small).testTag("fullmap_close"),
                            ) { Icon(Icons.Rounded.Close, contentDescription = "Fechar mapa", tint = Color.White) }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MapHeatmapMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = heatmap == mode,
                                    onClick = { heatmap = mode },
                                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }

                        poiButtons()

                        val currentPin = selectedPin
                        if (currentPin != null) {
                            selectedPinCard(currentPin)
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Toque em um talhão, máquina, Loja, Banco ou Garagem para consultar detalhes e ir até o local.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                interactiveMapCanvas(Modifier.fillMaxSize())

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), shape = MaterialTheme.shapes.medium) {
                            Text(
                                text = "Mapa • ${mapDef.stateName} (${gridSide}×${gridSide})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).testTag("fullmap_region_title"),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { zoom = (zoom + 0.4f).coerceAtMost(4.5f) },
                                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), MaterialTheme.shapes.small),
                            ) { Icon(Icons.Rounded.ZoomIn, contentDescription = "Aproximar mapa") }
                            IconButton(
                                onClick = { zoom = (zoom - 0.4f).coerceAtLeast(0.8f) },
                                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), MaterialTheme.shapes.small),
                            ) { Icon(Icons.Rounded.ZoomOut, contentDescription = "Afastar mapa") }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(48.dp).background(StatusDanger.copy(alpha = 0.9f), MaterialTheme.shapes.small).testTag("fullmap_close"),
                            ) { Icon(Icons.Rounded.Close, contentDescription = "Fechar mapa", tint = Color.White) }
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MapHeatmapMode.entries.forEach { mode ->
                            FilterChip(
                                selected = heatmap == mode,
                                onClick = { heatmap = mode },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }

                    poiButtons()
                }

                selectedPin?.let { pin ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        selectedPinCard(pin)
                    }
                }
            }
        }
    }
}
