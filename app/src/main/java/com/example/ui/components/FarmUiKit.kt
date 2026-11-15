package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.BrandVisualPalette
import com.example.domain.CloudSaveConflictState
import com.example.domain.CloudSaveSummary
import com.example.domain.ImplementCategory
import com.example.domain.ImplementDef
import com.example.domain.RgbTriple
import com.example.domain.VehicleBodyStyle
import com.example.domain.VehicleDef
import com.example.domain.WheelLayout
import com.example.domain.resolveBrandVisualPalette
import com.example.domain.resolveImplementBrandPalette
import com.example.simulation.formatBrl
import com.example.ui.theme.CabinCard
import com.example.ui.theme.CabinSurface
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusWarning
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

fun RgbTriple.toComposeColor(): Color = Color(
    red = r.toFloat().coerceIn(0f, 1f),
    green = g.toFloat().coerceIn(0f, 1f),
    blue = b.toFloat().coerceIn(0f, 1f),
)

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("header_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun StatBadge(
    label: String,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MachinePreviewCanvas(
    vehicleDef: VehicleDef? = null,
    implementDef: ImplementDef? = null,
    customPaintOverride: RgbTriple? = null,
    rimColorOverride: RgbTriple? = null,
    turntableAngleDeg: Float = 32f,
    zoomScale: Float = 1.0f,
    modifier: Modifier = Modifier,
) {
    val bgTop = CabinSurface
    val bgBot = CabinCard
    Canvas(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(bgTop)
            .border(1.dp, Color(0xFF2E3D34), MaterialTheme.shapes.medium)
    ) {
        drawRect(bgBot, topLeft = Offset(0f, size.height * 0.62f), size = Size(size.width, size.height * 0.38f))
        // Turntable ring shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(size.width * 0.14f, size.height * 0.72f),
            size = Size(size.width * 0.72f, size.height * 0.16f),
        )
        drawOval(
            color = Color(0xFF52B788).copy(alpha = 0.25f),
            topLeft = Offset(size.width * 0.14f, size.height * 0.72f),
            size = Size(size.width * 0.72f, size.height * 0.16f),
            style = Stroke(width = 2f),
        )

        val cx = size.width / 2f
        val cy = size.height * 0.58f
        val baseScale = min(size.width, size.height) * 0.32f * zoomScale
        val yawRad = (turntableAngleDeg * PI / 180.0).toFloat()
        val cosA = cos(yawRad)

        if (vehicleDef != null) {
            val basePalette = resolveBrandVisualPalette(vehicleDef, customPaintOverride, null)
            val finalPalette = if (rimColorOverride != null) basePalette.copy(rimRgb = rimColorOverride) else basePalette
            drawVehicleIllustration(
                cx = cx,
                cy = cy,
                scale = baseScale,
                cosAngle = cosA,
                bodyStyle = vehicleDef.bodyStyle,
                wheelLayout = vehicleDef.wheelLayout,
                hasCab = vehicleDef.hasCab,
                palette = finalPalette,
            )
        } else if (implementDef != null) {
            val impPalette = resolveImplementBrandPalette(implementDef, customPaintOverride, null)
            drawImplementIllustration(
                cx = cx,
                cy = cy,
                scale = baseScale,
                category = implementDef.category,
                palette = impPalette,
            )
        }
    }
}

private fun DrawScope.drawVehicleIllustration(
    cx: Float,
    cy: Float,
    scale: Float,
    cosAngle: Float,
    bodyStyle: VehicleBodyStyle,
    wheelLayout: WheelLayout,
    hasCab: Boolean,
    palette: BrandVisualPalette,
) {
    val primary = palette.primaryRgb.toComposeColor()
    val hoodColor = palette.hoodRgb.toComposeColor()
    val stripeColor = palette.hoodStripeRgb.toComposeColor()
    val rimColor = palette.rimRgb.toComposeColor()
    val cabRoofColor = palette.cabRoofRgb.toComposeColor()
    val seatColor = palette.seatRgb.toComposeColor()
    val chassisColor = palette.chassisRgb.toComposeColor()
    val exhaustColor = palette.exhaustRgb.toComposeColor()
    val headerCol = palette.headerFrameRgb.toComposeColor()
    val reelCol = palette.headerReelRgb.toComposeColor()
    val augerCol = palette.augerRgb.toComposeColor()

    val flip = if (cosAngle < 0f) -1f else 1f
    val xCompress = kotlin.math.max(0.45f, kotlin.math.abs(cosAngle))
    val w = scale * 1.95f * xCompress
    val h = scale * 0.92f
    val left = cx - w / 2f
    val tireColor = Color(0xFF1A1E1B)
    val glassColor = Color(0xFF81D4FA).copy(alpha = 0.58f)

    when (bodyStyle) {
        VehicleBodyStyle.HARVESTER -> {
            // Header cutter bar + spinning reel bar in brand colors
            val hx = if (flip > 0f) left - w * 0.28f else left + w * 0.96f
            drawRoundRect(
                color = headerCol,
                topLeft = Offset(hx, cy + h * 0.08f),
                size = Size(w * 0.32f, h * 0.28f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawRoundRect(
                color = reelCol,
                topLeft = Offset(hx + w * 0.02f, cy - h * 0.03f),
                size = Size(w * 0.26f, h * 0.11f),
                cornerRadius = CornerRadius(4f, 4f),
            )
            // Main harvester hull + thin model stripe
            drawRoundRect(
                color = primary,
                topLeft = Offset(left, cy - h * 0.48f),
                size = Size(w, h * 0.68f),
                cornerRadius = CornerRadius(14f, 14f),
            )
            drawRect(
                color = stripeColor,
                topLeft = Offset(left + w * 0.08f, cy - h * 0.16f),
                size = Size(w * 0.84f, h * 0.07f),
            )
            // Unloading auger tube
            drawLine(
                color = augerCol,
                start = Offset(cx, cy - h * 0.44f),
                end = Offset(cx + flip * w * 0.48f, cy - h * 0.62f),
                strokeWidth = scale * 0.11f,
                cap = StrokeCap.Round,
            )
            // Cab glass front + cab roof
            val cabLeft = if (flip > 0f) left + w * 0.08f else left + w * 0.66f
            drawRoundRect(
                color = cabRoofColor,
                topLeft = Offset(cabLeft - w * 0.02f, cy - h * 0.50f),
                size = Size(w * 0.30f, h * 0.10f),
                cornerRadius = CornerRadius(4f, 4f),
            )
            drawRoundRect(
                color = glassColor,
                topLeft = Offset(cabLeft, cy - h * 0.42f),
                size = Size(w * 0.26f, h * 0.30f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            drawWheelPair(left + w * 0.22f, cy + h * 0.22f, scale * 0.34f, tireColor, rimColor, wheelLayout == WheelLayout.CRAWLER_TRACKS)
            drawWheelPair(left + w * 0.78f, cy + h * 0.25f, scale * 0.24f, tireColor, rimColor, false)
        }
        VehicleBodyStyle.TRUCK, VehicleBodyStyle.PICKUP -> {
            drawRoundRect(
                color = chassisColor,
                topLeft = Offset(left + w * 0.06f, cy - h * 0.22f),
                size = Size(w * 0.88f, h * 0.36f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            val cabW = if (bodyStyle == VehicleBodyStyle.TRUCK) w * 0.34f else w * 0.42f
            val cabX = if (flip > 0f) left + w * 0.08f else left + w * 0.92f - cabW
            drawRoundRect(
                color = primary,
                topLeft = Offset(cabX, cy - h * 0.56f),
                size = Size(cabW, h * 0.52f),
                cornerRadius = CornerRadius(10f, 10f),
            )
            drawRoundRect(
                color = glassColor,
                topLeft = Offset(cabX + cabW * 0.12f, cy - h * 0.50f),
                size = Size(cabW * 0.76f, h * 0.24f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawWheelPair(left + w * 0.24f, cy + h * 0.24f, scale * 0.24f, tireColor, rimColor, false)
            drawWheelPair(left + w * 0.74f, cy + h * 0.24f, scale * 0.24f, tireColor, rimColor, false)
        }
        VehicleBodyStyle.MOTO -> {
            drawLine(
                color = primary,
                start = Offset(left + w * 0.22f, cy + h * 0.14f),
                end = Offset(left + w * 0.76f, cy - h * 0.18f),
                strokeWidth = scale * 0.14f,
                cap = StrokeCap.Round,
            )
            drawWheelPair(left + w * 0.24f, cy + h * 0.22f, scale * 0.24f, tireColor, rimColor, false)
            drawWheelPair(left + w * 0.76f, cy + h * 0.22f, scale * 0.24f, tireColor, rimColor, false)
        }
        else -> {
            val hoodX = if (flip > 0f) left + w * 0.06f else left + w * 0.42f
            // Dark chassis underbelly
            drawRoundRect(
                color = chassisColor,
                topLeft = Offset(left + w * 0.10f, cy - h * 0.02f),
                size = Size(w * 0.80f, h * 0.24f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            // Hood in authentic hoodColor (e.g. GREEN on John Deere!)
            drawRoundRect(
                color = hoodColor,
                topLeft = Offset(hoodX, cy - h * 0.34f),
                size = Size(w * 0.52f, h * 0.36f),
                cornerRadius = CornerRadius(10f, 10f),
            )
            // Thin model lettering pinstripe near upper flank (leaving hood body & nose green!)
            drawRect(
                color = stripeColor,
                topLeft = Offset(hoodX + 6f, cy - h * 0.26f),
                size = Size((w * 0.50f - 12f).coerceAtLeast(8f), h * 0.045f),
            )
            // Exhaust pipe
            drawRect(
                color = exhaustColor,
                topLeft = Offset(cx - w * 0.06f, cy - h * 0.62f),
                size = Size(scale * 0.07f, h * 0.35f),
            )
            // Cab or ROPS + brand seat (YELLOW on John Deere!)
            val cabW = w * 0.34f
            val cabX = if (flip > 0f) left + w * 0.54f else left + w * 0.12f
            if (hasCab) {
                drawRoundRect(
                    color = Color(0xFF1F2421),
                    topLeft = Offset(cabX, cy - h * 0.62f),
                    size = Size(cabW, h * 0.56f),
                    cornerRadius = CornerRadius(10f, 10f),
                )
                drawRoundRect(
                    color = glassColor,
                    topLeft = Offset(cabX + cabW * 0.10f, cy - h * 0.56f),
                    size = Size(cabW * 0.80f, h * 0.34f),
                    cornerRadius = CornerRadius(6f, 6f),
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(cabX + cabW * 0.18f, cy - h * 0.52f),
                    end = Offset(cabX + cabW * 0.44f, cy - h * 0.28f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
                // Visible operator seat inside cab
                drawRoundRect(
                    color = seatColor,
                    topLeft = Offset(cabX + cabW * 0.36f, cy - h * 0.36f),
                    size = Size(cabW * 0.28f, h * 0.18f),
                    cornerRadius = CornerRadius(3f, 3f),
                )
                // Brand cab roof cap
                drawRoundRect(
                    color = cabRoofColor,
                    topLeft = Offset(cabX - cabW * 0.06f, cy - h * 0.70f),
                    size = Size(cabW * 1.12f, h * 0.12f),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            } else {
                // Seat + ROPS frame
                drawRoundRect(
                    color = seatColor,
                    topLeft = Offset(cabX + cabW * 0.30f, cy - h * 0.34f),
                    size = Size(cabW * 0.36f, h * 0.18f),
                    cornerRadius = CornerRadius(4f, 4f),
                )
                drawRect(
                    color = Color(0xFF2A322D),
                    topLeft = Offset(cabX + cabW * 0.2f, cy - h * 0.56f),
                    size = Size(cabW * 0.6f, h * 0.5f),
                    style = Stroke(width = scale * 0.08f),
                )
            }
            val rearR = if (bodyStyle == VehicleBodyStyle.ARTICULATED_4WD || bodyStyle == VehicleBodyStyle.QUAD_TRACK) scale * 0.34f else scale * 0.38f
            val frontR = if (bodyStyle == VehicleBodyStyle.ARTICULATED_4WD || bodyStyle == VehicleBodyStyle.QUAD_TRACK) scale * 0.34f else scale * 0.26f
            val frontX = if (flip > 0f) left + w * 0.20f else left + w * 0.80f
            val rearX = if (flip > 0f) left + w * 0.78f else left + w * 0.22f
            val isCrawler = wheelLayout == WheelLayout.CRAWLER_TRACKS
            drawWheelPair(frontX, cy + h * 0.24f, frontR, tireColor, rimColor, isCrawler)
            drawWheelPair(rearX, cy + h * 0.20f, rearR, tireColor, rimColor, isCrawler)
        }
    }
}

private fun DrawScope.drawWheelPair(
    cx: Float,
    cy: Float,
    radius: Float,
    tireColor: Color,
    rimColor: Color,
    isTrack: Boolean,
) {
    if (isTrack) {
        drawRoundRect(
            color = tireColor,
            topLeft = Offset(cx - radius * 1.4f, cy - radius * 0.72f),
            size = Size(radius * 2.8f, radius * 1.44f),
            cornerRadius = CornerRadius(radius * 0.6f, radius * 0.6f),
        )
        drawCircle(color = rimColor, radius = radius * 0.38f, center = Offset(cx - radius * 0.7f, cy))
        drawCircle(color = rimColor, radius = radius * 0.38f, center = Offset(cx + radius * 0.7f, cy))
    } else {
        drawCircle(color = tireColor, radius = radius, center = Offset(cx, cy))
        drawCircle(color = rimColor, radius = radius * 0.56f, center = Offset(cx, cy))
        drawCircle(color = Color(0xFF26302B), radius = radius * 0.2f, center = Offset(cx, cy))
    }
}

private fun DrawScope.drawImplementIllustration(
    cx: Float,
    cy: Float,
    scale: Float,
    category: ImplementCategory,
    palette: BrandVisualPalette,
) {
    val primary = palette.primaryRgb.toComposeColor()
    val rimColor = palette.rimRgb.toComposeColor()
    val w = scale * 2.1f
    val h = scale * 0.85f
    val left = cx - w / 2f
    drawRoundRect(
        color = primary,
        topLeft = Offset(left, cy - h * 0.12f),
        size = Size(w, h * 0.28f),
        cornerRadius = CornerRadius(8f, 8f),
    )
    when (category) {
        ImplementCategory.CARRETA -> {
            drawRoundRect(
                color = primary.copy(alpha = 0.88f),
                topLeft = Offset(left + w * 0.08f, cy - h * 0.60f),
                size = Size(w * 0.84f, h * 0.56f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            drawWheelPair(left + w * 0.30f, cy + h * 0.24f, scale * 0.24f, Color(0xFF1A1E1B), rimColor, false)
            drawWheelPair(left + w * 0.68f, cy + h * 0.24f, scale * 0.24f, Color(0xFF1A1E1B), rimColor, false)
        }
        ImplementCategory.PULVERIZADOR -> {
            drawRoundRect(
                color = Color(0xFFF3F6F4),
                topLeft = Offset(cx - w * 0.16f, cy - h * 0.54f),
                size = Size(w * 0.32f, h * 0.45f),
                cornerRadius = CornerRadius(10f, 10f),
            )
            drawLine(
                color = primary,
                start = Offset(left - w * 0.12f, cy - h * 0.05f),
                end = Offset(left + w * 1.12f, cy - h * 0.05f),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
        }
        ImplementCategory.PLATAFORMA_MILHO -> {
            val snouts = 5
            drawRoundRect(
                color = Color(0xFF263238),
                topLeft = Offset(left + w * 0.04f, cy - h * 0.42f),
                size = Size(w * 0.92f, h * 0.24f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            for (i in 0 until snouts) {
                val sx = left + w * (0.06f + i * 0.19f)
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(sx, cy - h * 0.04f),
                    size = Size(w * 0.12f, h * 0.36f),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
        }
        ImplementCategory.CULTIVADOR -> {
            val shanks = 5
            for (i in 0 until shanks) {
                val sx = left + w * (0.12f + i * 0.19f)
                drawLine(
                    color = Color(0xFF78909C),
                    start = Offset(sx, cy),
                    end = Offset(sx - w * 0.04f, cy + h * 0.36f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round,
                )
            }
            drawRoundRect(
                color = Color(0xFF546E7A),
                topLeft = Offset(left + w * 0.08f, cy + h * 0.22f),
                size = Size(w * 0.84f, h * 0.12f),
                cornerRadius = CornerRadius(6f, 6f),
            )
        }
        ImplementCategory.ARADO, ImplementCategory.GRADE, ImplementCategory.SUBSOLADOR, ImplementCategory.ESCARIFICADOR -> {
            val discs = 5
            for (i in 0 until discs) {
                val dx = left + w * (0.14f + i * 0.18f)
                drawCircle(color = Color(0xFFB5C4BA), radius = scale * 0.18f, center = Offset(dx, cy + h * 0.22f))
            }
        }
        else -> {
            drawRoundRect(
                color = Color(0xFFF3F6F4),
                topLeft = Offset(left + w * 0.18f, cy - h * 0.45f),
                size = Size(w * 0.64f, h * 0.34f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            drawWheelPair(left + w * 0.18f, cy + h * 0.22f, scale * 0.18f, Color(0xFF1A1E1B), rimColor, false)
            drawWheelPair(left + w * 0.82f, cy + h * 0.22f, scale * 0.18f, Color(0xFF1A1E1B), rimColor, false)
        }
    }
}

@Composable
fun AnalogJoystick(
    sizeDp: Dp = 128.dp,
    opacity: Float = 0.85f,
    onMove: (x: Double, y: Double) -> Unit,
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(0xFF161E19).copy(alpha = 0.55f * opacity))
            .border(2.dp, Color(0xFF52B788).copy(alpha = 0.55f * opacity), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobX = 0f
                        knobY = 0f
                        onMove(0.0, 0.0)
                    },
                    onDragCancel = {
                        knobX = 0f
                        knobY = 0f
                        onMove(0.0, 0.0)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val maxR = size.width * 0.35f
                        var nx = knobX + dragAmount.x
                        var ny = knobY + dragAmount.y
                        val dist = hypot(nx.toDouble(), ny.toDouble()).toFloat()
                        if (dist > maxR && dist > 0f) {
                            nx = (nx / dist) * maxR
                            ny = (ny / dist) * maxR
                        }
                        knobX = nx
                        knobY = ny
                        val outX = (nx / maxR).coerceIn(-1f, 1f).toDouble()
                        val outY = (-ny / maxR).coerceIn(-1f, 1f).toDouble()
                        onMove(outX, outY)
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Color.White.copy(alpha = 0.12f * opacity),
                radius = size.minDimension * 0.18f,
                center = c,
            )
            drawCircle(
                color = Color(0xFFE9A825).copy(alpha = 0.90f * opacity),
                radius = size.minDimension * 0.21f,
                center = Offset(c.x + knobX, c.y + knobY),
            )
        }
    }
}

@Composable
fun VirtualSteeringWheel(
    sizeDp: Dp = 132.dp,
    opacity: Float = 0.85f,
    onSteerChanged: (Double) -> Unit,
) {
    var angleDeg by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(0xFF161E19).copy(alpha = 0.55f * opacity))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        angleDeg = 0f
                        onSteerChanged(0.0)
                    },
                    onDragCancel = {
                        angleDeg = 0f
                        onSteerChanged(0.0)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val rawAngle = (atan2((change.position.x - cx).toDouble(), -(change.position.y - cy).toDouble()) * 180.0 / PI).toFloat()
                        val clamped = rawAngle.coerceIn(-90f, 90f)
                        angleDeg = clamped
                        onSteerChanged((clamped / 90f).toDouble())
                    }
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.40f
            rotate(angleDeg, pivot = c) {
                drawCircle(
                    color = Color(0xFFEFF5F0).copy(alpha = 0.88f * opacity),
                    radius = r,
                    center = c,
                    style = Stroke(width = 12f),
                )
                drawCircle(
                    color = HarvestGold.copy(alpha = 0.92f * opacity),
                    radius = 12f,
                    center = Offset(c.x, c.y - r),
                )
                drawLine(
                    color = Color(0xFFB5C4BA).copy(alpha = 0.85f * opacity),
                    start = Offset(c.x - r, c.y),
                    end = Offset(c.x + r, c.y),
                    strokeWidth = 9f,
                )
                drawLine(
                    color = Color(0xFFB5C4BA).copy(alpha = 0.85f * opacity),
                    start = c,
                    end = Offset(c.x, c.y + r),
                    strokeWidth = 9f,
                )
                drawCircle(color = Color(0xFF1E2822), radius = 18f, center = c)
            }
        }
    }
}

@Composable
fun CloudSavesSection(
    cloudSaves: List<CloudSaveSummary>,
    isSyncing: Boolean,
    activeSlotIndex: Int,
    onUploadCurrent: () -> Unit,
    onDownloadSlot: (Int) -> Unit,
    onDeleteSlot: (Int) -> Unit,
) {
    SectionCard(
        title = "Nuvem Firebase (20 Espaços Protegidos)",
        subtitle = "Sincronize suas carreiras entre dispositivos com verificação SHA-256 e histórico.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Fazenda ativa: Espaço ${activeSlotIndex + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onUploadCurrent,
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("cloud_upload_button"),
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = "Enviar para nuvem", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Enviar Espaço Atual")
            }
        }

        if (cloudSaves.isEmpty()) {
            Text(
                text = "Nenhum backup encontrado na nuvem ainda. Clique em 'Enviar Espaço Atual' para proteger sua fazenda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cloudSaves.forEach { cs ->
                    Surface(
                        color = CabinCard,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = "Backup em nuvem",
                                tint = StatusGreen,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Espaço ${cs.slotIndex + 1}: ${cs.farmName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Dia ${cs.day} • ${formatBrl(cs.moneyBrl)} • Rev #${cs.revision} • ${cs.deviceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            FilledTonalButton(
                                onClick = { onDownloadSlot(cs.slotIndex) },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("cloud_download_${cs.slotIndex}"),
                            ) {
                                Icon(Icons.Rounded.CloudDownload, contentDescription = "Baixar", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onDeleteSlot(cs.slotIndex) },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("cloud_delete_${cs.slotIndex}"),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remover da nuvem", tint = StatusDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudConflictDialog(
    conflict: CloudSaveConflictState,
    onResolve: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onResolve("CANCEL") },
        icon = {
            Icon(Icons.Rounded.WarningAmber, contentDescription = "Conflito de nuvem", tint = StatusWarning)
        },
        title = {
            Text("Conflito de Revisão na Nuvem (Espaço ${conflict.slotIndex + 1})")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Outro dispositivo atualizou este espaço na nuvem com uma revisão mais recente.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Surface(color = CabinCard, shape = MaterialTheme.shapes.small) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Local: ${conflict.localFarmName} • Dia ${conflict.localDay} • ${formatBrl(conflict.localMoneyBrl)} (Rev #${conflict.localRevision})", style = MaterialTheme.typography.bodySmall)
                        Text("Nuvem: ${conflict.cloud.farmName} • Dia ${conflict.cloud.day} • ${formatBrl(conflict.cloud.moneyBrl)} (Rev #${conflict.cloud.revision})", style = MaterialTheme.typography.bodySmall, color = HarvestGold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onResolve("USE_CLOUD") },
                modifier = Modifier.testTag("conflict_use_cloud"),
            ) {
                Text("Usar Nuvem")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onResolve("CANCEL") },
                    modifier = Modifier.testTag("conflict_cancel"),
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onResolve("KEEP_LOCAL") },
                    colors = ButtonDefaults.buttonColors(containerColor = HarvestGold, contentColor = Color(0xFF0E1310)),
                    modifier = Modifier.testTag("conflict_keep_local"),
                ) {
                    Text("Sobrescrever Nuvem")
                }
            }
        }
    )
}
