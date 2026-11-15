package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.HITCH_LABELS
import com.example.domain.IMPLEMENT_CATEGORY_LABELS
import com.example.domain.ImplementDef
import com.example.domain.VEHICLE_CATEGORY_LABELS
import com.example.domain.VehicleDef
import com.example.simulation.formatBrl
import com.example.ui.theme.AgroSprout
import com.example.ui.theme.HarvestGold
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MachineGalleryModal(
    vehicleDef: VehicleDef? = null,
    implementDef: ImplementDef? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    if (vehicleDef == null && implementDef == null) return
    var angleDeg by remember(vehicleDef?.id, implementDef?.id) { mutableFloatStateOf(35f) }
    var zoomScale by remember(vehicleDef?.id, implementDef?.id) { mutableFloatStateOf(1.0f) }

    val title = vehicleDef?.let { "${it.brand} ${it.model}" }
        ?: implementDef?.let { "${it.brand} ${it.model}" }
        ?: ""
    val subtitle = vehicleDef?.let {
        "${VEHICLE_CATEGORY_LABELS[it.category] ?: it.category.name} • ${it.era} (${it.modelYear})"
    } ?: implementDef?.let {
        "${IMPLEMENT_CATEGORY_LABELS[it.category] ?: it.category.name} • ${HITCH_LABELS[it.hitchType] ?: it.hitchType.name}"
    } ?: ""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroSprout,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("gallery_close_button"),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar galeria")
                    }
                }

                MachinePreviewCanvas(
                    vehicleDef = vehicleDef,
                    implementDef = implementDef,
                    turntableAngleDeg = angleDeg,
                    zoomScale = zoomScale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                )

                // Angle presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val presets = listOf(
                        "Frente 3/4" to 35f,
                        "Lateral" to 0f,
                        "Oposto" to 180f,
                        "3/4 Tras." to 145f,
                    )
                    presets.forEach { (label, target) ->
                        FilterChip(
                            selected = kotlin.math.abs(angleDeg - target) < 6f,
                            onClick = { angleDeg = target },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.RotateRight, contentDescription = "Ângulo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = angleDeg,
                        onValueChange = { angleDeg = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${angleDeg.roundToInt()}°", style = MaterialTheme.typography.labelMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Zoom", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = zoomScale,
                        onValueChange = { zoomScale = it },
                        valueRange = 0.7f..1.35f,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${(zoomScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (vehicleDef != null) {
                        StatBadge("${vehicleDef.horsepower.roundToInt()} cv", HarvestGold)
                        StatBadge("${vehicleDef.maxSpeedKmh.roundToInt()} km/h", AgroSprout)
                        StatBadge("Tanque ${vehicleDef.fuelCapacityL.roundToInt()} L", MaterialTheme.colorScheme.primary)
                        StatBadge("${(vehicleDef.weightKg / 1000.0).let { "%.1f".format(it) }} t", MaterialTheme.colorScheme.onSurfaceVariant)
                        if (vehicleDef.gpsReady) StatBadge("GPS RTK", AgroSprout)
                        if (vehicleDef.hasGrainTank) StatBadge("Graneleiro ${vehicleDef.tankCapacityL.roundToInt()} L", HarvestGold)
                    }
                    if (implementDef != null) {
                        StatBadge("${implementDef.requiredHp.roundToInt()}+ cv mín.", HarvestGold)
                        StatBadge("Largura ${"%.1f".format(implementDef.workingWidthM)} m", AgroSprout)
                        StatBadge("${implementDef.weightKg.roundToInt()} kg", MaterialTheme.colorScheme.onSurfaceVariant)
                        if (implementDef.capacityL > 0) StatBadge("Cap. ${implementDef.capacityL.roundToInt()} L", MaterialTheme.colorScheme.primary)
                    }
                }

                Text(
                    text = vehicleDef?.description ?: implementDef?.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val price = vehicleDef?.priceBrl ?: implementDef?.priceBrl ?: 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Valor de Tabela", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatBrl(price), style = MaterialTheme.typography.titleLarge, color = HarvestGold, fontWeight = FontWeight.Bold)
                    }
                    if (actionLabel != null && onActionClick != null) {
                        Button(
                            onClick = {
                                onActionClick()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HarvestGold,
                                contentColor = Color(0xFF0E1310),
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("gallery_action_button"),
                        ) {
                            Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(actionLabel, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
