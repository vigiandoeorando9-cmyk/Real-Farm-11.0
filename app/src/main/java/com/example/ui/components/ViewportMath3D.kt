package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.domain.BuildingType
import com.example.domain.CameraMode
import com.example.domain.OwnedImplement
import com.example.domain.OwnedVehicle
import com.example.domain.PlacedBuilding
import com.example.domain.PlayerAppearanceConfig
import com.example.domain.RgbTriple
import com.example.domain.RoadVehicle
import com.example.domain.WorldProp
import com.example.domain.WorldState
import com.example.domain.getVehicleDef
import com.example.simulation.clampCameraSegmentAgainstBuildings
import com.example.simulation.getTerrainHeightAt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class CameraPose3D(
    val camX: Float,
    val camY: Float,
    val camZ: Float,
    val targetX: Float,
    val targetY: Float,
    val targetZ: Float,
    val yawDeg: Float,
    val pitchDeg: Float,
    val fovDeg: Float,
    val fwdX: Float,
    val fwdY: Float,
    val fwdZ: Float,
    val rightX: Float,
    val rightZ: Float,
    val upX: Float,
    val upY: Float,
    val upZ: Float,
)

data class ProjectedPoint3D(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val visible: Boolean,
)

fun computeCameraPose3D(
    worldState: WorldState,
    cameraMode: CameraMode,
    cameraDistanceFactor: Float = 1.0f,
    fovDeg: Float = 60f,
    orbitYawDeg: Float = 0f,
    orbitPitchDeg: Float = 0f,
    pinchZoom: Float = 1.0f,
    panOffsetX: Float = 0f,
    panOffsetZ: Float = 0f,
): CameraPose3D {
    val activeVeh = worldState.activeVehicleUid?.let { uid ->
        worldState.vehicles.find { it.uid == uid }
    }
    val def = activeVeh?.let { getVehicleDef(it.defId) }
    val vehLength = def?.dimensionsM?.width?.toFloat()?.coerceIn(2.6f, 9.2f) ?: 4.2f
    val vehHeight = def?.dimensionsM?.length?.toFloat()?.coerceIn(1.6f, 4.2f) ?: 2.8f

    val baseFocusX = (activeVeh?.x ?: worldState.player.x).toFloat()
    val baseFocusZ = (activeVeh?.z ?: worldState.player.z).toFloat()
    val baseHeading = (activeVeh?.headingDeg ?: worldState.player.headingDeg).toFloat()
    val groundY = getTerrainHeightAt(worldState, baseFocusX.toDouble(), baseFocusZ.toDouble()).toFloat()

    val zoom = pinchZoom.coerceIn(0.55f, 2.2f)
    val distFactor = cameraDistanceFactor.coerceIn(0.65f, 1.8f)

    val rawYawDeg: Float
    val clampedPitchDeg: Float
    val targetX: Float
    val targetY: Float
    val targetZ: Float
    var camX: Float
    var camY: Float
    var camZ: Float

    when (cameraMode) {
        CameraMode.CABINE -> {
            rawYawDeg = baseHeading + orbitYawDeg.coerceIn(-75f, 75f)
            clampedPitchDeg = (8f + orbitPitchDeg).coerceIn(2f, 26f)
            val radH = rawYawDeg * (PI / 180.0).toFloat()
            val eyeH = if (activeVeh != null) (vehHeight * 0.78f).coerceIn(1.7f, 3.3f) else 1.68f
            val fwdShift = if (activeVeh != null) vehLength * 0.10f else 0.15f
            camX = baseFocusX + sin(radH) * fwdShift
            camY = groundY + eyeH + ((activeVeh?.cabBounceOffsetY ?: 0.0).toFloat())
            camZ = baseFocusZ + cos(radH) * fwdShift
            targetX = camX + sin(radH) * 14f
            targetY = camY - sin(clampedPitchDeg * (PI / 180.0).toFloat()) * 14f
            targetZ = camZ + cos(radH) * 14f
        }
        CameraMode.ORBITAL -> {
            rawYawDeg = baseHeading + orbitYawDeg
            clampedPitchDeg = (24f + orbitPitchDeg).coerceIn(12f, 52f)
            targetX = baseFocusX + panOffsetX * 0.4f
            targetY = groundY + (if (activeVeh != null) vehHeight * 0.45f else 1.2f)
            targetZ = baseFocusZ + panOffsetZ * 0.4f
            val dist = (vehLength * 1.65f + 5.0f) * distFactor / zoom
            val yawR = rawYawDeg * (PI / 180.0).toFloat()
            val pitR = clampedPitchDeg * (PI / 180.0).toFloat()
            val fx = sin(yawR) * cos(pitR)
            val fy = -sin(pitR)
            val fz = cos(yawR) * cos(pitR)
            camX = targetX - fx * dist
            camY = targetY - fy * dist
            camZ = targetZ - fz * dist
        }
        CameraMode.TOPO -> {
            rawYawDeg = orbitYawDeg
            clampedPitchDeg = (52f + orbitPitchDeg * 0.4f).coerceIn(38f, 60f)
            targetX = baseFocusX + panOffsetX
            targetY = groundY + 0.8f
            targetZ = baseFocusZ + panOffsetZ
            val dist = (28f * distFactor / zoom).coerceIn(14f, 52f)
            val yawR = rawYawDeg * (PI / 180.0).toFloat()
            val pitR = clampedPitchDeg * (PI / 180.0).toFloat()
            camX = targetX - sin(yawR) * cos(pitR) * dist
            camY = targetY + sin(pitR) * dist
            camZ = targetZ - cos(yawR) * cos(pitR) * dist
        }
        CameraMode.LIVRE -> {
            rawYawDeg = baseHeading + orbitYawDeg
            clampedPitchDeg = (22f + orbitPitchDeg).coerceIn(10f, 52f)
            targetX = baseFocusX + panOffsetX
            targetY = getTerrainHeightAt(worldState, targetX.toDouble(), (baseFocusZ + panOffsetZ).toDouble()).toFloat() + 1.4f
            targetZ = baseFocusZ + panOffsetZ
            val dist = (15f * distFactor / zoom).coerceIn(7f, 38f)
            val yawR = rawYawDeg * (PI / 180.0).toFloat()
            val pitR = clampedPitchDeg * (PI / 180.0).toFloat()
            camX = targetX - sin(yawR) * cos(pitR) * dist
            camY = targetY + sin(pitR) * dist
            camZ = targetZ - cos(yawR) * cos(pitR) * dist
        }
        CameraMode.TERCEIRA_PESSOA -> {
            rawYawDeg = baseHeading + orbitYawDeg
            clampedPitchDeg = (19f + orbitPitchDeg).coerceIn(11f, 46f)
            val hRad = baseHeading * (PI / 180.0).toFloat()
            val leadAhead = if (activeVeh != null) 1.6f else 1.1f
            targetX = baseFocusX + sin(hRad) * leadAhead
            targetY = groundY + (if (activeVeh != null) vehHeight * 0.50f else 1.25f)
            targetZ = baseFocusZ + cos(hRad) * leadAhead
            val baseDist = if (activeVeh != null) (vehLength * 1.45f + 4.8f).coerceIn(8.8f, 16.5f) else 6.8f
            val dist = (baseDist * distFactor / zoom).coerceIn(5.2f, 26f)
            val yawR = rawYawDeg * (PI / 180.0).toFloat()
            val pitR = clampedPitchDeg * (PI / 180.0).toFloat()
            camX = targetX - sin(yawR) * cos(pitR) * dist
            camY = targetY + sin(pitR) * dist
            camZ = targetZ - cos(yawR) * cos(pitR) * dist
        }
    }

    if (cameraMode == CameraMode.TERCEIRA_PESSOA || cameraMode == CameraMode.ORBITAL || cameraMode == CameraMode.LIVRE) {
        val clamped = clampCameraSegmentAgainstBuildings(
            targetX = targetX,
            targetY = targetY,
            targetZ = targetZ,
            camX = camX,
            camY = camY,
            camZ = camZ,
            state = worldState,
        )
        camX = clamped.first
        camY = clamped.second
        camZ = clamped.third
    }

    if (cameraMode != CameraMode.CABINE) {
        val camGround = getTerrainHeightAt(worldState, camX.toDouble(), camZ.toDouble()).toFloat()
        if (camY < camGround + 1.6f) {
            camY = camGround + 1.6f
        }
    }

    val yawR = rawYawDeg * (PI / 180.0).toFloat()
    val pitR = clampedPitchDeg * (PI / 180.0).toFloat()
    val sinY = sin(yawR)
    val cosY = cos(yawR)
    val sinP = sin(pitR)
    val cosP = cos(pitR)

    return CameraPose3D(
        camX = camX,
        camY = camY,
        camZ = camZ,
        targetX = targetX,
        targetY = targetY,
        targetZ = targetZ,
        yawDeg = rawYawDeg,
        pitchDeg = clampedPitchDeg,
        fovDeg = fovDeg.coerceIn(45f, 82f),
        fwdX = sinY * cosP,
        fwdY = -sinP,
        fwdZ = cosY * cosP,
        rightX = cosY,
        rightZ = -sinY,
        upX = sinY * sinP,
        upY = cosP,
        upZ = cosY * sinP,
    )
}

fun projectPoint3D(
    wx: Float,
    wy: Float,
    wz: Float,
    pose: CameraPose3D,
    viewWidthPx: Float,
    viewHeightPx: Float,
): ProjectedPoint3D {
    val dx = wx - pose.camX
    val dy = wy - pose.camY
    val dz = wz - pose.camZ

    val camX = dx * pose.rightX + dz * pose.rightZ
    val camY = dx * pose.upX + dy * pose.upY + dz * pose.upZ
    val camZ = dx * pose.fwdX + dy * pose.fwdY + dz * pose.fwdZ

    val halfFovRad = (pose.fovDeg * 0.5f) * (PI / 180.0).toFloat()
    val focalPx = (viewHeightPx * 0.5f) / tan(halfFovRad).coerceAtLeast(0.25f)

    if (camZ <= 0.45f) {
        return ProjectedPoint3D(0f, 0f, camZ, visible = false)
    }
    val invZ = focalPx / camZ
    return ProjectedPoint3D(
        screenX = viewWidthPx * 0.5f + camX * invZ,
        screenY = viewHeightPx * 0.5f - camY * invZ,
        depth = camZ,
        visible = true,
    )
}

internal enum class ItemKind3D {
    VEHICLE,
    IMPLEMENT,
    BUILDING,
    TREE,
    PROP,
    ROAD_VEH,
    PLAYER,
    NPC,
    TEAMMATE,
    CONTRACTOR,
}

internal data class RenderItem3D(
    val kind: ItemKind3D,
    val depth: Float,
    val x: Double,
    val y: Double,
    val z: Double,
    val vehicle: OwnedVehicle? = null,
    val implement: OwnedImplement? = null,
    val prop: WorldProp? = null,
    val building: PlacedBuilding? = null,
    val buildingType: BuildingType? = null,
    val buildingRotDeg: Double = 0.0,
    val buildingGateOpen: Boolean = false,
    val roadVehicle: RoadVehicle? = null,
    val labelColor: Color = Color.White,
    val altColor: Color = Color.DarkGray,
    val headingDeg: Double = 0.0,
    val playerAppearance: PlayerAppearanceConfig? = null,
)

internal class ViewportScratch3D {
    val path = Path()
    val items = ArrayList<RenderItem3D>(160)
    val cX = FloatArray(16)
    val cY = FloatArray(16)
    val cZ = FloatArray(16)
    val pX = FloatArray(32)
    val pY = FloatArray(32)
    val wX = FloatArray(32)
    val wY = FloatArray(32)
    val wZ = FloatArray(32)
    val w2X = FloatArray(32)
    val w2Y = FloatArray(32)
    val w2Z = FloatArray(32)
}


internal fun parseSafeColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

internal fun shadeColor(color: Color, factor: Float): Color = Color(
    red = (color.red * factor).coerceIn(0f, 1f),
    green = (color.green * factor).coerceIn(0f, 1f),
    blue = (color.blue * factor).coerceIn(0f, 1f),
    alpha = color.alpha,
)

internal fun shadeAndFog(color: Color, shade: Float, fogBlend: Float, fogColor: Color): Color {
    val r = (color.red * shade).coerceIn(0f, 1f)
    val g = (color.green * shade).coerceIn(0f, 1f)
    val b = (color.blue * shade).coerceIn(0f, 1f)
    return Color(
        red = r + (fogColor.red - r) * fogBlend,
        green = g + (fogColor.green - g) * fogBlend,
        blue = b + (fogColor.blue - b) * fogBlend,
        alpha = color.alpha,
    )
}

internal fun lerpColor(a: Color, b: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * k,
        green = a.green + (b.green - a.green) * k,
        blue = a.blue + (b.blue - a.blue) * k,
        alpha = a.alpha + (b.alpha - a.alpha) * k,
    )
}

internal fun projectRaw(
    wx: Float,
    wy: Float,
    wz: Float,
    pose: CameraPose3D,
    focalPx: Float,
    viewW: Float,
    viewH: Float,
): ProjectedPoint3D {
    val rx = wx - pose.camX
    val ry = wy - pose.camY
    val rz = wz - pose.camZ
    val cz = rx * pose.fwdX + ry * pose.fwdY + rz * pose.fwdZ
    if (cz <= 0.45f) return ProjectedPoint3D(0f, 0f, cz, false)
    val cx = rx * pose.rightX + rz * pose.rightZ
    val cy = rx * pose.upX + ry * pose.upY + rz * pose.upZ
    val inv = focalPx / cz
    return ProjectedPoint3D(viewW * 0.5f + cx * inv, viewH * 0.5f - cy * inv, cz, true)
}

internal fun DrawScope.drawProjectedLine(
    ax: Float, ay: Float, az: Float,
    bx: Float, by: Float, bz: Float,
    color: Color,
    strokeW: Float,
    pose: CameraPose3D,
    focalPx: Float,
    viewW: Float,
    viewH: Float,
) {
    val p1 = projectRaw(ax, ay, az, pose, focalPx, viewW, viewH)
    val p2 = projectRaw(bx, by, bz, pose, focalPx, viewW, viewH)
    if (p1.visible && p2.visible) {
        drawLine(color = color, start = Offset(p1.screenX, p1.screenY), end = Offset(p2.screenX, p2.screenY), strokeWidth = strokeW)
    }
}

internal fun DrawScope.drawProjectedQuad(
    ax: Float, ay: Float, az: Float,
    bx: Float, by: Float, bz: Float,
    cx: Float, cy: Float, cz: Float,
    dx: Float, dy: Float, dz: Float,
    color: Color,
    pose: CameraPose3D,
    focalPx: Float,
    viewW: Float,
    viewH: Float,
    scratch: ViewportScratch3D,
) {
    val cX = scratch.cX
    val cY = scratch.cY
    val cZ = scratch.cZ

    fun toCam(wx: Float, wy: Float, wz: Float, outIdx: Int) {
        val rx = wx - pose.camX
        val ry = wy - pose.camY
        val rz = wz - pose.camZ
        cX[outIdx] = rx * pose.rightX + rz * pose.rightZ
        cY[outIdx] = rx * pose.upX + ry * pose.upY + rz * pose.upZ
        cZ[outIdx] = rx * pose.fwdX + ry * pose.fwdY + rz * pose.fwdZ
    }
    toCam(ax, ay, az, 0)
    toCam(bx, by, bz, 1)
    toCam(cx, cy, cz, 2)
    toCam(dx, dy, dz, 3)

    val zNear = 0.45f
    var allBehind = true
    var anyBehind = false
    for (i in 0 until 4) {
        if (cZ[i] >= zNear) allBehind = false else anyBehind = true
    }
    if (allBehind) return

    val pX = scratch.pX
    val pY = scratch.pY
    var count = 0
    if (!anyBehind) {
        for (i in 0 until 4) {
            val inv = focalPx / cZ[i]
            pX[i] = viewW * 0.5f + cX[i] * inv
            pY[i] = viewH * 0.5f - cY[i] * inv
        }
        count = 4
    } else {
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            val z1 = cZ[i]
            val z2 = cZ[j]
            val in1 = z1 >= zNear
            val in2 = z2 >= zNear
            if (in1 && in2) {
                val inv2 = focalPx / z2
                pX[count] = viewW * 0.5f + cX[j] * inv2
                pY[count] = viewH * 0.5f - cY[j] * inv2
                count++
            } else if (in1 && !in2) {
                val t = ((zNear - z1) / (z2 - z1)).coerceIn(0f, 1f)
                val ix = cX[i] + (cX[j] - cX[i]) * t
                val iy = cY[i] + (cY[j] - cY[i]) * t
                val inv = focalPx / zNear
                pX[count] = viewW * 0.5f + ix * inv
                pY[count] = viewH * 0.5f - iy * inv
                count++
            } else if (!in1 && in2) {
                val t = ((zNear - z1) / (z2 - z1)).coerceIn(0f, 1f)
                val ix = cX[i] + (cX[j] - cX[i]) * t
                val iy = cY[i] + (cY[j] - cY[i]) * t
                val inv = focalPx / zNear
                pX[count] = viewW * 0.5f + ix * inv
                pY[count] = viewH * 0.5f - iy * inv
                count++
                val inv2 = focalPx / z2
                pX[count] = viewW * 0.5f + cX[j] * inv2
                pY[count] = viewH * 0.5f - cY[j] * inv2
                count++
            }
        }
    }
    if (count < 3) return

    var minX = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (i in 0 until count) {
        if (pX[i] < minX) minX = pX[i]
        if (pX[i] > maxX) maxX = pX[i]
        if (pY[i] < minY) minY = pY[i]
        if (pY[i] > maxY) maxY = pY[i]
    }
    if (maxX < -40f || minX > viewW + 40f || maxY < -40f || minY > viewH + 40f) return

    val path = scratch.path
    path.reset()
    path.moveTo(pX[0], pY[0])
    for (i in 1 until count) {
        path.lineTo(pX[i], pY[i])
    }
    path.close()
    drawPath(path, color = color)
}

internal fun DrawScope.drawProjectedTri(
    ax: Float, ay: Float, az: Float,
    bx: Float, by: Float, bz: Float,
    cx: Float, cy: Float, cz: Float,
    color: Color,
    pose: CameraPose3D,
    focalPx: Float,
    viewW: Float,
    viewH: Float,
    scratch: ViewportScratch3D,
) {
    drawProjectedQuad(ax, ay, az, bx, by, bz, cx, cy, cz, ax, ay, az, color, pose, focalPx, viewW, viewH, scratch)
}

internal fun DrawScope.drawRotatedBox3D(
    cx: Float,
    cy: Float,
    cz: Float,
    halfW: Float,
    halfH: Float,
    halfL: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    color: Color,
    light: Float,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val x0 = cx - rx * halfW - fx * halfL
    val z0 = cz - rz * halfW - fz * halfL
    val x1 = cx + rx * halfW - fx * halfL
    val z1 = cz + rz * halfW - fz * halfL
    val x2 = cx + rx * halfW + fx * halfL
    val z2 = cz + rz * halfW + fz * halfL
    val x3 = cx - rx * halfW + fx * halfL
    val z3 = cz - rz * halfW + fz * halfL
    val yBot = cy - halfH
    val yTop = cy + halfH

    val dCamX = pose.camX - cx
    val dCamZ = pose.camZ - cz
    val frontFaceDot = dCamX * fx + dCamZ * fz
    val rightFaceDot = dCamX * rx + dCamZ * rz

    if (halfH > 0.02f) {
        if (rightFaceDot > 0f) {
            drawProjectedQuad(x1, yBot, z1, x2, yBot, z2, x2, yTop, z2, x1, yTop, z1, shadeColor(color, 0.85f * light), pose, focalPx, w, h, scratch)
        } else {
            drawProjectedQuad(x3, yBot, z3, x0, yBot, z0, x0, yTop, z0, x3, yTop, z3, shadeColor(color, 0.72f * light), pose, focalPx, w, h, scratch)
        }
        if (frontFaceDot > 0f) {
            drawProjectedQuad(x2, yBot, z2, x3, yBot, z3, x3, yTop, z3, x2, yTop, z2, shadeColor(color, 0.94f * light), pose, focalPx, w, h, scratch)
        } else {
            drawProjectedQuad(x0, yBot, z0, x1, yBot, z1, x1, yTop, z1, x0, yTop, z0, shadeColor(color, 0.78f * light), pose, focalPx, w, h, scratch)
        }
    }
    if (pose.camY >= yTop - 0.15f) {
        drawProjectedQuad(x0, yTop, z0, x1, yTop, z1, x2, yTop, z2, x3, yTop, z3, shadeColor(color, 1.07f * light), pose, focalPx, w, h, scratch)
    }
}

/**
 * Full 3D oriented box supporting chassis pitch & roll + heading yaw.
 */
internal fun DrawScope.drawTiltedBox3D(
    cx: Float,
    cy: Float,
    cz: Float,
    halfW: Float,
    halfH: Float,
    halfL: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    pitchDeg: Float,
    rollDeg: Float,
    color: Color,
    light: Float,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
    taperTopFactor: Float = 1.0f,
) {
    val pitchR = pitchDeg * (PI / 180.0).toFloat()
    val rollR = rollDeg * (PI / 180.0).toFloat()
    val sinP = sin(pitchR)
    val cosP = cos(pitchR)
    val sinR = sin(rollR)
    val cosR = cos(rollR)

    // Basis axes in world space with pitch & roll
    val fX = fx * cosP
    val fY = sinP
    val fZ = fz * cosP

    val rX = rx * cosR
    val rY = -sinR
    val rZ = rz * cosR

    // Cross product u = f x r roughly (unit upward vector)
    val uX = -fx * sinP + rx * sinR
    val uY = (cosP * cosR).coerceAtLeast(0.5f)
    val uZ = -fz * sinP + rz * sinR

    val topScale = taperTopFactor.coerceIn(0.45f, 1.2f)
    val wx = scratch.wX
    val wy = scratch.wY
    val wz = scratch.wZ

    // Bottom 4 corners (0..3) and Top 4 corners (4..7)
    for (layer in 0..1) {
        val signY = if (layer == 0) -1f else 1f
        val hw = if (layer == 1) halfW * topScale else halfW
        val hl = if (layer == 1) halfL * topScale else halfL
        val hh = halfH * signY
        val base = layer * 4

        wx[base + 0] = cx - rX * hw - fX * hl + uX * hh
        wy[base + 0] = cy - rY * hw - fY * hl + uY * hh
        wz[base + 0] = cz - rZ * hw - fZ * hl + uZ * hh

        wx[base + 1] = cx + rX * hw - fX * hl + uX * hh
        wy[base + 1] = cy + rY * hw - fY * hl + uY * hh
        wz[base + 1] = cz + rZ * hw - fZ * hl + uZ * hh

        wx[base + 2] = cx + rX * hw + fX * hl + uX * hh
        wy[base + 2] = cy + rY * hw + fY * hl + uY * hh
        wz[base + 2] = cz + rZ * hw + fZ * hl + uZ * hh

        wx[base + 3] = cx - rX * hw + fX * hl + uX * hh
        wy[base + 3] = cy - rY * hw + fY * hl + uY * hh
        wz[base + 3] = cz - rZ * hw + fZ * hl + uZ * hh
    }

    val dCamX = pose.camX - cx
    val dCamY = pose.camY - cy
    val dCamZ = pose.camZ - cz
    val frontFaceDot = dCamX * fX + dCamY * fY + dCamZ * fZ
    val rightFaceDot = dCamX * rX + dCamY * rY + dCamZ * rZ
    val topFaceDot = dCamX * uX + dCamY * uY + dCamZ * uZ

    if (rightFaceDot > 0f) {
        drawProjectedQuad(wx[1], wy[1], wz[1], wx[2], wy[2], wz[2], wx[6], wy[6], wz[6], wx[5], wy[5], wz[5], shadeColor(color, 0.85f * light), pose, focalPx, w, h, scratch)
    } else {
        drawProjectedQuad(wx[3], wy[3], wz[3], wx[0], wy[0], wz[0], wx[4], wy[4], wz[4], wx[7], wy[7], wz[7], shadeColor(color, 0.72f * light), pose, focalPx, w, h, scratch)
    }
    if (frontFaceDot > 0f) {
        drawProjectedQuad(wx[2], wy[2], wz[2], wx[3], wy[3], wz[3], wx[7], wy[7], wz[7], wx[6], wy[6], wz[6], shadeColor(color, 0.95f * light), pose, focalPx, w, h, scratch)
    } else {
        drawProjectedQuad(wx[0], wy[0], wz[0], wx[1], wy[1], wz[1], wx[5], wy[5], wz[5], wx[4], wy[4], wz[4], shadeColor(color, 0.78f * light), pose, focalPx, w, h, scratch)
    }
    if (topFaceDot > -0.15f) {
        drawProjectedQuad(wx[4], wy[4], wz[4], wx[5], wy[5], wz[5], wx[6], wy[6], wz[6], wx[7], wy[7], wz[7], shadeColor(color, 1.08f * light), pose, focalPx, w, h, scratch)
    }
}

/**
 * Multi-sided extruded lugged agricultural wheel with outer tire tread, inner colored deep-dish rim, and central hub.
 */
internal fun DrawScope.drawCylinderWheel3D(
    cx: Float,
    cy: Float,
    cz: Float,
    radius: Float,
    halfWidth: Float,
    spinDeg: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    tireColor: Color,
    rimColor: Color,
    light: Float,
    segments: Int,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val segs = segments.coerceIn(8, 20)
    val spinRad = spinDeg * (PI / 180.0).toFloat()
    val wX = scratch.wX
    val wY = scratch.wY
    val wZ = scratch.wZ
    val rX = scratch.w2X
    val rY = scratch.w2Y
    val rZ = scratch.w2Z

    val rimR = radius * 0.56f
    val hubR = radius * 0.22f
    val sideDot = (pose.camX - cx) * rx + (pose.camZ - cz) * rz
    val outerSign = if (sideDot >= 0f) 1f else -1f
    val innerSign = -outerSign

    for (i in 0 until segs) {
        val a = spinRad + (i.toFloat() / segs.toFloat()) * (2f * PI.toFloat())
        val c = cos(a)
        val s = sin(a)
        val offF = c * radius
        val offU = s * radius
        wX[i] = cx + fx * offF
        wY[i] = cy + offU
        wZ[i] = cz + fz * offF

        val rOffF = c * rimR
        val rOffU = s * rimR
        rX[i] = cx + fx * rOffF
        rY[i] = cy + rOffU
        rZ[i] = cz + fz * rOffF
    }

    // Tread faces around circumference
    for (i in 0 until segs) {
        val j = (i + 1) % segs
        val midY = (wY[i] + wY[j]) * 0.5f - cy
        val midF = ((wX[i] + wX[j]) * 0.5f - cx) * fx + ((wZ[i] + wZ[j]) * 0.5f - cz) * fz
        // Only draw tread quads roughly facing upward or toward camera
        val normalDotCam = (pose.camY - cy) * (midY / radius) +
            ((pose.camX - cx) * fx + (pose.camZ - cz) * fz) * (midF / radius)
        if (normalDotCam > -0.25f) {
            val treadShade = if (i % 2 == 0) 0.92f else 0.76f // Alternating chevron lug shading!
            drawProjectedQuad(
                wX[i] + rx * halfWidth * innerSign, wY[i], wZ[i] + rz * halfWidth * innerSign,
                wX[j] + rx * halfWidth * innerSign, wY[j], wZ[j] + rz * halfWidth * innerSign,
                wX[j] + rx * halfWidth * outerSign, wY[j], wZ[j] + rz * halfWidth * outerSign,
                wX[i] + rx * halfWidth * outerSign, wY[i], wZ[i] + rz * halfWidth * outerSign,
                shadeColor(tireColor, treadShade * light),
                pose, focalPx, w, h, scratch,
            )
        }
    }

    // Visible outer sidewall ring
    val outX = rx * halfWidth * outerSign
    val outZ = rz * halfWidth * outerSign
    for (i in 0 until segs) {
        val j = (i + 1) % segs
        drawProjectedQuad(
            rX[i] + outX, rY[i], rZ[i] + outZ,
            wX[i] + outX, wY[i], wZ[i] + outZ,
            wX[j] + outX, wY[j], wZ[j] + outZ,
            rX[j] + outX, rY[j], rZ[j] + outZ,
            shadeColor(tireColor, 0.88f * light),
            pose, focalPx, w, h, scratch,
        )
    }

    // Recessed deep-dish rim + hub
    val rimOffX = rx * (halfWidth * 0.86f) * outerSign
    val rimOffZ = rz * (halfWidth * 0.86f) * outerSign
    val hubCol = shadeColor(rimColor, 0.78f * light)
    for (i in 0 until segs) {
        val j = (i + 1) % segs
        val dishShade = if (i % 2 == 0) 1.04f else 0.94f
        drawProjectedTri(
            cx + rimOffX, cy, cz + rimOffZ,
            rX[i] + rimOffX, rY[i], rZ[i] + rimOffZ,
            rX[j] + rimOffX, rY[j], rZ[j] + rimOffZ,
            shadeColor(rimColor, dishShade * light),
            pose, focalPx, w, h, scratch,
        )
    }
    // Central hub cap / planetary reduction
    val hubOffX = rx * (halfWidth * 1.03f) * outerSign
    val hubOffZ = rz * (halfWidth * 1.03f) * outerSign
    val hubP = projectRaw(cx + hubOffX, cy, cz + hubOffZ, pose, focalPx, w, h)
    val rimEdgeP = projectRaw(cx + hubOffX + fx * hubR, cy, cz + hubOffZ + fz * hubR, pose, focalPx, w, h)
    if (hubP.visible && rimEdgeP.visible) {
        val pxRadius = max(2.2f, hypot(rimEdgeP.screenX - hubP.screenX, rimEdgeP.screenY - hubP.screenY))
        drawCircle(color = hubCol, radius = pxRadius, center = Offset(hubP.screenX, hubP.screenY))
    }
}

internal fun DrawScope.drawCylinderY3D(
    cx: Float,
    yBot: Float,
    yTop: Float,
    cz: Float,
    radius: Float,
    color: Color,
    light: Float,
    segments: Int,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val segs = segments.coerceIn(8, 20)
    val wX = scratch.wX
    val wZ = scratch.wZ
    for (i in 0 until segs) {
        val a = (i.toFloat() / segs.toFloat()) * (2f * PI.toFloat())
        wX[i] = cx + cos(a) * radius
        wZ[i] = cz + sin(a) * radius
    }
    for (i in 0 until segs) {
        val j = (i + 1) % segs
        val midX = (wX[i] + wX[j]) * 0.5f - cx
        val midZ = (wZ[i] + wZ[j]) * 0.5f - cz
        val dCam = (pose.camX - cx) * midX + (pose.camZ - cz) * midZ
        if (dCam > -0.15f * radius) {
            val sunDot = ((midX / radius) * 0.6f + (midZ / radius) * 0.5f).coerceIn(-0.4f, 0.5f)
            val shade = (0.85f + sunDot * 0.32f) * light
            drawProjectedQuad(
                wX[i], yBot, wZ[i],
                wX[j], yBot, wZ[j],
                wX[j], yTop, wZ[j],
                wX[i], yTop, wZ[i],
                shadeColor(color, shade),
                pose, focalPx, w, h, scratch,
            )
        }
    }
}

internal fun DrawScope.drawConeY3D(
    cx: Float,
    yBase: Float,
    yTip: Float,
    cz: Float,
    radius: Float,
    color: Color,
    light: Float,
    segments: Int,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val segs = segments.coerceIn(8, 20)
    val wX = scratch.wX
    val wZ = scratch.wZ
    for (i in 0 until segs) {
        val a = (i.toFloat() / segs.toFloat()) * (2f * PI.toFloat())
        wX[i] = cx + cos(a) * radius
        wZ[i] = cz + sin(a) * radius
    }
    for (i in 0 until segs) {
        val j = (i + 1) % segs
        val midX = (wX[i] + wX[j]) * 0.5f - cx
        val midZ = (wZ[i] + wZ[j]) * 0.5f - cz
        val sunDot = ((midX / radius) * 0.6f + (midZ / radius) * 0.5f).coerceIn(-0.4f, 0.5f)
        drawProjectedTri(
            wX[i], yBase, wZ[i],
            wX[j], yBase, wZ[j],
            cx, yTip, cz,
            shadeColor(color, (0.94f + sunDot * 0.25f) * light),
            pose, focalPx, w, h, scratch,
        )
    }
}

/**
 * Smooth multi-ring rounded ellipsoid dome for rounded tree canopies, shrubs, heads & organic shapes.
 */
internal fun DrawScope.drawEllipsoidDome3D(
    cx: Float,
    cy: Float,
    cz: Float,
    radiusXZ: Float,
    radiusY: Float,
    color: Color,
    light: Float,
    segments: Int,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
    rings: Int = 3,
) {
    val segs = segments.coerceIn(8, 16)
    val rSteps = rings.coerceIn(2, 4)
    val rx1 = scratch.wX
    val rz1 = scratch.wZ
    val rx2 = scratch.w2X
    val rz2 = scratch.w2Z

    for (rIdx in 0 until rSteps) {
        val phi0 = -PI.toFloat() * 0.35f + (rIdx.toFloat() / rSteps) * (PI.toFloat() * 0.80f)
        val phi1 = -PI.toFloat() * 0.35f + ((rIdx + 1).toFloat() / rSteps) * (PI.toFloat() * 0.80f)
        val y0 = cy + sin(phi0) * radiusY
        val y1 = cy + sin(phi1) * radiusY
        val rad0 = cos(phi0).coerceAtLeast(0.12f) * radiusXZ
        val rad1 = cos(phi1).coerceAtLeast(0.06f) * radiusXZ
        val ringLit = 0.82f + (rIdx.toFloat() / rSteps) * 0.26f

        for (i in 0 until segs) {
            val a = (i.toFloat() / segs.toFloat()) * (2f * PI.toFloat())
            val ca = cos(a)
            val sa = sin(a)
            rx1[i] = cx + ca * rad0
            rz1[i] = cz + sa * rad0
            rx2[i] = cx + ca * rad1
            rz2[i] = cz + sa * rad1
        }
        for (i in 0 until segs) {
            val j = (i + 1) % segs
            val midX = ((rx1[i] + rx1[j]) * 0.5f - cx) / max(radiusXZ, 0.1f)
            val midZ = ((rz1[i] + rz1[j]) * 0.5f - cz) / max(radiusXZ, 0.1f)
            val dCam = (pose.camX - cx) * midX + (pose.camZ - cz) * midZ + (pose.camY - cy) * 0.35f
            if (dCam > -0.25f) {
                val sunDot = (midX * 0.55f + midZ * 0.45f).coerceIn(-0.35f, 0.45f)
                drawProjectedQuad(
                    rx1[i], y0, rz1[i],
                    rx1[j], y0, rz1[j],
                    rx2[j], y1, rz2[j],
                    rx2[i], y1, rz2[i],
                    shadeColor(color, (ringLit + sunDot * 0.25f) * light),
                    pose, focalPx, w, h, scratch,
                )
            }
        }
    }
}

private fun hypot(dx: Float, dy: Float): Float = sqrt(dx * dx + dy * dy)
