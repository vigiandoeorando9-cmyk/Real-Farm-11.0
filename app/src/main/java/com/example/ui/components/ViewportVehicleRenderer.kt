package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.domain.DEFAULT_PLAYER_APPEARANCE
import com.example.domain.ExhaustStyle
import com.example.domain.GraphicsConfig
import com.example.domain.HoodProfile
import com.example.domain.ImplementCategory
import com.example.domain.OwnedImplement
import com.example.domain.OwnedVehicle
import com.example.domain.PlayerAppearanceConfig
import com.example.domain.VehicleBodyStyle
import com.example.domain.WheelLayout
import com.example.domain.getImplementDef
import com.example.domain.getVehicleDef
import com.example.domain.resolveBrandVisualPalette
import com.example.domain.resolveImplementBrandPalette
import com.example.domain.resolveVehicleTuning
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal fun DrawScope.drawVehicle3D(
    v: OwnedVehicle,
    gy: Float,
    isCockpit: Boolean,
    isNight: Boolean,
    gfx: GraphicsConfig,
    animClockSec: Float,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    light: Float,
    scratch: ViewportScratch3D,
    driverAppearance: PlayerAppearanceConfig? = null,
) {
    val def = getVehicleDef(v.defId)
    val tuning = resolveVehicleTuning(v, def)
    val lenM = def.dimensionsM.width.toFloat().coerceIn(2.2f, 9.2f)
    val widthM = def.dimensionsM.height.toFloat().coerceIn(1.3f, 4.0f)
    val heightM = def.dimensionsM.length.toFloat().coerceIn(1.5f, 4.2f)

    val rad = v.headingDeg.toFloat() * (PI / 180.0).toFloat()
    val fx = sin(rad)
    val fz = cos(rad)
    val rx = cos(rad)
    val rz = -sin(rad)

    val vx = v.x.toFloat()
    val vz = v.z.toFloat()
    val baseY = gy + v.suspensionOffsetY.toFloat().coerceIn(-0.18f, 0.25f) - v.groundSinkageM.toFloat().coerceIn(0f, 0.22f)
    val pitchDeg = v.pitchDeg.toFloat().coerceIn(-18f, 18f)
    val rollDeg = v.rollDeg.toFloat().coerceIn(-18f, 18f)

    val customPaint = v.customPaintRgb?.takeIf { it.size >= 3 }?.let { com.example.domain.rgb(it[0], it[1], it[2]) }
    val palette = resolveBrandVisualPalette(def, customPaint, v.rimColorId)
    val bodyColor = palette.primaryRgb.toComposeColor()
    val hoodColor = palette.hoodRgb.toComposeColor()
    val stripeColor = palette.hoodStripeRgb.toComposeColor()
    val decalColor = palette.decalTextRgb.toComposeColor()
    val rimColor = palette.rimRgb.toComposeColor()
    val cabRoofColor = palette.cabRoofRgb.toComposeColor()
    val fenderColor = palette.fenderRgb.toComposeColor()
    val chassisColor = palette.chassisRgb.toComposeColor()
    val seatColor = palette.seatRgb.toComposeColor()
    val exhaustColor = palette.exhaustRgb.toComposeColor()
    val accentColor = stripeColor

    val detailLevel = gfx.vehicleDetailLevel
    val wheelSegs = when {
        detailLevel >= 2 -> 16
        detailLevel == 1 -> 12
        else -> 10
    }

    if (isCockpit) {
        // Draw detailed tractor/combine nose & exhaust visible through the windshield
        drawTiltedBox3D(
            cx = vx + fx * (lenM * 0.28f),
            cy = baseY + heightM * 0.44f,
            cz = vz + fz * (lenM * 0.28f),
            halfW = widthM * 0.26f,
            halfH = heightM * 0.14f,
            halfL = lenM * 0.22f,
            fx = fx, fz = fz, rx = rx, rz = rz,
            pitchDeg = pitchDeg, rollDeg = rollDeg,
            color = hoodColor,
            light = light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
            taperTopFactor = 0.88f,
        )
        // Thin flank pinstripe visible over hood side
        drawTiltedBox3D(
            cx = vx + fx * (lenM * 0.28f),
            cy = baseY + heightM * 0.48f,
            cz = vz + fz * (lenM * 0.28f),
            halfW = widthM * 0.255f,
            halfH = 0.018f,
            halfL = lenM * 0.20f,
            fx = fx, fz = fz, rx = rx, rz = rz,
            pitchDeg = pitchDeg, rollDeg = rollDeg,
            color = stripeColor,
            light = light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
        drawTiltedBox3D(
            cx = vx + fx * (lenM * 0.16f) + rx * (widthM * 0.27f),
            cy = baseY + heightM * 0.68f,
            cz = vz + fz * (lenM * 0.16f) + rz * (widthM * 0.27f),
            halfW = 0.055f, halfH = heightM * 0.22f, halfL = 0.055f,
            fx = fx, fz = fz, rx = rx, rz = rz,
            pitchDeg = pitchDeg, rollDeg = rollDeg,
            color = Color(0xFF282D2B), light = light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
        // 3D Interior Cockpit Dashboard (Painel), Side Armrest Console & Steering Wheel
        val cabYOff = (v.cabBounceOffsetY ?: 0.0).toFloat()
        val dashZ = lenM * 0.11f
        val dashY = baseY + heightM * 0.61f + cabYOff
        val dashX = vx + fx * dashZ
        val dashPz = vz + fz * dashZ
        // Sculpted cowl & dash deck
        drawTiltedBox3D(
            cx = dashX, cy = dashY, cz = dashPz,
            halfW = widthM * 0.24f, halfH = 0.11f, halfL = 0.14f,
            fx = fx, fz = fz, rx = rx, rz = rz,
            pitchDeg = pitchDeg + 14f, rollDeg = rollDeg,
            color = Color(0xFF1D2220), light = light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
        // Glowing digital & analog instrument cluster screen
        drawTiltedBox3D(
            cx = dashX - fx * 0.06f, cy = dashY + 0.08f, cz = dashPz - fz * 0.06f,
            halfW = 0.14f, halfH = 0.055f, halfL = 0.02f,
            fx = fx, fz = fz, rx = rx, rz = rz,
            pitchDeg = pitchDeg + 22f, rollDeg = rollDeg,
            color = if (v.engineRunning == true) Color(0xFF10B981) else Color(0xFF37474F),
            light = if (v.engineRunning == true) 1.0f else light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
        // Right-side operator console & hydraulic implement joystick
        drawTiltedBox3D(
            cx = dashX - fx * 0.14f + rx * (widthM * 0.21f),
            cy = dashY - 0.05f,
            cz = dashPz - fz * 0.14f + rz * (widthM * 0.21f),
            halfW = 0.09f, halfH = 0.06f, halfL = 0.22f,
            fx = fx, fz = fz, rx = rx, rz = rz,
            pitchDeg = pitchDeg, rollDeg = rollDeg,
            color = Color(0xFF262B29), light = light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
        drawTiltedBox3D(
            cx = dashX - fx * 0.10f + rx * (widthM * 0.21f),
            cy = dashY + 0.04f,
            cz = dashPz - fz * 0.10f + rz * (widthM * 0.21f),
            halfW = 0.025f, halfH = 0.08f, halfL = 0.025f,
            fx = fx, fz = fz, rx = rx, rz = rz,
            pitchDeg = pitchDeg, rollDeg = rollDeg,
            color = accentColor, light = light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
        // Steering column & steering wheel tilted smoothly by steerAngleDeg
        val wheelTurnRad = (v.steerAngleDeg.toFloat() * 1.4f) * (PI / 180.0).toFloat()
        val wFx = sin(rad + wheelTurnRad)
        val wFz = cos(rad + wheelTurnRad)
        val wRx = cos(rad + wheelTurnRad)
        val wRz = -sin(rad + wheelTurnRad)
        drawTiltedBox3D(
            cx = dashX - fx * 0.14f, cy = dashY + 0.05f, cz = dashPz - fz * 0.14f,
            halfW = 0.032f, halfH = 0.09f, halfL = 0.032f,
            fx = fx, fz = fz, rx = rx, rz = rz,
            pitchDeg = pitchDeg + 34f, rollDeg = rollDeg,
            color = Color(0xFF141816), light = light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
        drawTiltedBox3D(
            cx = dashX - fx * 0.18f, cy = dashY + 0.12f, cz = dashPz - fz * 0.18f,
            halfW = 0.15f, halfH = 0.022f, halfL = 0.15f,
            fx = wFx, fz = wFz, rx = wRx, rz = wRz,
            pitchDeg = pitchDeg + 38f, rollDeg = rollDeg,
            color = Color(0xFF191D1B), light = light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
        drawTiltedBox3D(
            cx = dashX - fx * 0.18f, cy = dashY + 0.125f, cz = dashPz - fz * 0.18f,
            halfW = 0.038f, halfH = 0.024f, halfL = 0.038f,
            fx = wFx, fz = wFz, rx = wRx, rz = wRz,
            pitchDeg = pitchDeg + 38f, rollDeg = rollDeg,
            color = decalColor, light = light,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
        return
    }

    // Ground contact shadow + directional sun shadow when shadowQuality >= 2
    drawRotatedBox3D(
        cx = vx + 0.14f, cy = gy + 0.03f, cz = vz + 0.14f,
        halfW = widthM * 0.56f, halfH = 0.01f, halfL = lenM * 0.54f,
        fx = fx, fz = fz, rx = rx, rz = rz,
        color = Color.Black.copy(alpha = if (gfx.shadowQuality >= 2) 0.40f else 0.32f), light = 1f,
        pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
    )
    if (gfx.shadowQuality >= 2) {
        drawRotatedBox3D(
            cx = vx + 0.65f, cy = gy + 0.022f, cz = vz + 0.65f,
            halfW = widthM * 0.62f, halfH = 0.01f, halfL = lenM * 0.60f,
            fx = fx, fz = fz, rx = rx, rz = rz,
            color = Color.Black.copy(alpha = 0.18f), light = 1f,
            pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
        )
    }

    // Headlight ground cones + work LED lights
    if (v.headlightsOn || v.workLightsOn == true) {
        val beamAlpha = if (isNight) 0.30f else 0.12f
        val beamL = lenM * 0.52f
        val beamFar = if (v.workLightsOn == true) 18f else 14f
        val bL = projectRaw(vx + fx * beamL - rx * (widthM * 0.30f), baseY + 0.7f, vz + fz * beamL - rz * (widthM * 0.30f), pose, focalPx, w, h)
        val bR = projectRaw(vx + fx * beamL + rx * (widthM * 0.30f), baseY + 0.7f, vz + fz * beamL + rz * (widthM * 0.30f), pose, focalPx, w, h)
        val fL = projectRaw(vx + fx * (beamL + beamFar) - rx * (widthM * 1.45f), gy + 0.15f, vz + fz * (beamL + beamFar) - rz * (widthM * 1.45f), pose, focalPx, w, h)
        val fR = projectRaw(vx + fx * (beamL + beamFar) + rx * (widthM * 1.45f), gy + 0.15f, vz + fz * (beamL + beamFar) + rz * (widthM * 1.45f), pose, focalPx, w, h)
        if (bL.visible && bR.visible && fL.visible && fR.visible) {
            scratch.path.reset()
            scratch.path.moveTo(bL.screenX, bL.screenY)
            scratch.path.lineTo(bR.screenX, bR.screenY)
            scratch.path.lineTo(fR.screenX, fR.screenY)
            scratch.path.lineTo(fL.screenX, fL.screenY)
            scratch.path.close()
            drawPath(scratch.path, Color(0xFFFFF59D).copy(alpha = beamAlpha))
        }
    }

    // Articulated rear steering frame vectors for articulated 4WD
    val artRad = (v.articulatedAngleDeg ?: 0.0).toFloat() * (PI / 180.0).toFloat()
    val rFx = sin(rad + artRad)
    val rFz = cos(rad + artRad)
    val rRx = cos(rad + artRad)
    val rRz = -sin(rad + artRad)

    val wheelX = widthM * 0.44f
    val rearZ = -lenM * 0.28f
    val frontZ = lenM * 0.30f
    val tireCol = Color(0xFF1A1E1C)
    val spinDeg = v.wheelSpinDeg.toFloat()

    // Wheels / Crawler tracks
    if (def.wheelLayout == WheelLayout.CRAWLER_TRACKS) {
        drawTiltedBox3D(vx - rx * wheelX, baseY + 0.42f, vz - rz * wheelX, 0.28f, 0.40f, lenM * 0.36f, fx, fz, rx, rz, pitchDeg, rollDeg, tireCol, light, pose, focalPx, w, h, scratch)
        drawTiltedBox3D(vx + rx * wheelX, baseY + 0.42f, vz + rz * wheelX, 0.28f, 0.40f, lenM * 0.36f, fx, fz, rx, rz, pitchDeg, rollDeg, tireCol, light, pose, focalPx, w, h, scratch)
    } else {
        val rearR = when (def.bodyStyle) {
            VehicleBodyStyle.TRUCK -> 0.50f
            VehicleBodyStyle.PICKUP -> 0.42f
            VehicleBodyStyle.MOTO -> 0.35f
            VehicleBodyStyle.HARVESTER -> 0.56f
            VehicleBodyStyle.SELF_PROPELLED_SPRAYER -> 0.78f
            VehicleBodyStyle.ARTICULATED_4WD -> 0.86f
            else -> 0.74f
        }
        val frontR = when (def.bodyStyle) {
            VehicleBodyStyle.TRUCK -> 0.50f
            VehicleBodyStyle.PICKUP -> 0.42f
            VehicleBodyStyle.MOTO -> 0.35f
            VehicleBodyStyle.HARVESTER -> 0.82f
            VehicleBodyStyle.SELF_PROPELLED_SPRAYER -> 0.78f
            VehicleBodyStyle.ARTICULATED_4WD -> 0.86f
            else -> 0.52f
        }
        val steerRad = v.steerAngleDeg.toFloat() * (PI / 180.0).toFloat()
        val sFx = sin(rad + steerRad)
        val sFz = cos(rad + steerRad)
        val sRx = cos(rad + steerRad)
        val sRz = -sin(rad + steerRad)

        val rearAxisFx = if (def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD) rFx else fx
        val rearAxisFz = if (def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD) rFz else fz
        val rearAxisRx = if (def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD) rRx else rx
        val rearAxisRz = if (def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD) rRz else rz

        if (def.bodyStyle == VehicleBodyStyle.MOTO) {
            drawCylinderWheel3D(vx + fx * rearZ, baseY + rearR, vz + fz * rearZ, rearR, 0.12f, spinDeg, fx, fz, rx, rz, tireCol, rimColor, light, wheelSegs, pose, focalPx, w, h, scratch)
            drawCylinderWheel3D(vx + fx * frontZ, baseY + frontR, vz + fz * frontZ, frontR, 0.11f, spinDeg, sFx, sFz, sRx, sRz, tireCol, rimColor, light, wheelSegs, pose, focalPx, w, h, scratch)
        } else {
            // Rear axle
            drawCylinderWheel3D(vx + rearAxisFx * rearZ - rearAxisRx * wheelX, baseY + rearR * 0.94f, vz + rearAxisFz * rearZ - rearAxisRz * wheelX, rearR, 0.25f, spinDeg, rearAxisFx, rearAxisFz, rearAxisRx, rearAxisRz, tireCol, rimColor, light, wheelSegs, pose, focalPx, w, h, scratch)
            drawCylinderWheel3D(vx + rearAxisFx * rearZ + rearAxisRx * wheelX, baseY + rearR * 0.94f, vz + rearAxisFz * rearZ + rearAxisRz * wheelX, rearR, 0.25f, spinDeg, rearAxisFx, rearAxisFz, rearAxisRx, rearAxisRz, tireCol, rimColor, light, wheelSegs, pose, focalPx, w, h, scratch)
            if (tuning.hasDualRears || def.wheelLayout == WheelLayout.ARTICULATED_DUAL) {
                val dualX = wheelX + 0.44f
                drawCylinderWheel3D(vx + rearAxisFx * rearZ - rearAxisRx * dualX, baseY + rearR * 0.94f, vz + rearAxisFz * rearZ - rearAxisRz * dualX, rearR, 0.23f, spinDeg, rearAxisFx, rearAxisFz, rearAxisRx, rearAxisRz, tireCol, rimColor, light, wheelSegs, pose, focalPx, w, h, scratch)
                drawCylinderWheel3D(vx + rearAxisFx * rearZ + rearAxisRx * dualX, baseY + rearR * 0.94f, vz + rearAxisFz * rearZ + rearAxisRz * dualX, rearR, 0.23f, spinDeg, rearAxisFx, rearAxisFz, rearAxisRx, rearAxisRz, tireCol, rimColor, light, wheelSegs, pose, focalPx, w, h, scratch)
            }
            // Front steered axle
            val frontLatX = if (def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD) wheelX else wheelX * 0.92f
            drawCylinderWheel3D(vx + fx * frontZ - rx * frontLatX, baseY + frontR * 0.94f, vz + fz * frontZ - rz * frontLatX, frontR, 0.22f, spinDeg, sFx, sFz, sRx, sRz, tireCol, rimColor, light, wheelSegs, pose, focalPx, w, h, scratch)
            drawCylinderWheel3D(vx + fx * frontZ + rx * frontLatX, baseY + frontR * 0.94f, vz + fz * frontZ + rz * frontLatX, frontR, 0.22f, spinDeg, sFx, sFz, sRx, sRz, tireCol, rimColor, light, wheelSegs, pose, focalPx, w, h, scratch)
        }
    }

    // Chassis lower frame rails
    drawTiltedBox3D(
        cx = vx, cy = baseY + 0.62f, cz = vz,
        halfW = widthM * 0.25f, halfH = 0.20f, halfL = lenM * 0.42f,
        fx = fx, fz = fz, rx = rx, rz = rz,
        pitchDeg = pitchDeg, rollDeg = rollDeg,
        color = chassisColor, light = light,
        pose = pose, focalPx = focalPx, w = w, h = h, scratch = scratch,
    )

    val cabYOffset = (v.cabBounceOffsetY ?: 0.0).toFloat()
    val cabPitch = (pitchDeg + (v.cabPitchDeg ?: 0.0).toFloat()).coerceIn(-22f, 22f)
    val cabRoll = (rollDeg + (v.cabRollDeg ?: 0.0).toFloat()).coerceIn(-22f, 22f)
    val drvApp = driverAppearance ?: DEFAULT_PLAYER_APPEARANCE
    val drvShirtCol = parseSafeColor(drvApp.shirtColorHex, Color(0xFF1E2124))
    val drvSkinCol = parseSafeColor(drvApp.skinColorHex, Color(0xFFE4B898))
    val drvHairCol = parseSafeColor(drvApp.hairColorHex, Color(0xFF3E2723))

    when (def.bodyStyle) {
        VehicleBodyStyle.HARVESTER -> {
            val headerFrameCol = palette.headerFrameRgb.toComposeColor()
            val headerReelCol = palette.headerReelRgb.toComposeColor()
            val augerCol = palette.augerRgb.toComposeColor()

            // Main combine hull + rear engine radiator grille + thin side model stripe
            drawTiltedBox3D(vx, baseY + heightM * 0.50f, vz, widthM * 0.41f, heightM * 0.28f, lenM * 0.42f, fx, fz, rx, rz, pitchDeg, rollDeg, bodyColor, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(vx, baseY + heightM * 0.55f, vz, widthM * 0.418f, 0.04f, lenM * 0.36f, fx, fz, rx, rz, pitchDeg, rollDeg, stripeColor, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(vx - fx * (lenM * 0.40f), baseY + heightM * 0.56f, vz - fz * (lenM * 0.40f), widthM * 0.36f, heightM * 0.20f, 0.14f, fx, fz, rx, rz, pitchDeg, rollDeg, chassisColor, light, pose, focalPx, w, h, scratch)

            // Upper grain tank hopper + live visible grain pile inside
            drawTiltedBox3D(vx - fx * (lenM * 0.04f), baseY + heightM * 0.80f, vz - fz * (lenM * 0.04f), widthM * 0.37f, heightM * 0.11f, lenM * 0.26f, fx, fz, rx, rz, pitchDeg, rollDeg, bodyColor, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.88f)
            drawTiltedBox3D(vx - fx * (lenM * 0.04f), baseY + heightM * 0.88f, vz - fz * (lenM * 0.04f), widthM * 0.35f, 0.03f, lenM * 0.24f, fx, fz, rx, rz, pitchDeg, rollDeg, decalColor, light, pose, focalPx, w, h, scratch)
            val tankFillRatio = (v.grainTankFillL / max(def.tankCapacityL, 1.0)).toFloat().coerceIn(0f, 1f)
            if (tankFillRatio > 0.03f) {
                drawTiltedBox3D(vx - fx * (lenM * 0.04f), baseY + heightM * (0.86f + tankFillRatio * 0.10f), vz - fz * (lenM * 0.04f), widthM * 0.31f, 0.10f + tankFillRatio * 0.14f, lenM * 0.20f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFE7BD4A), light, pose, focalPx, w, h, scratch, taperTopFactor = 0.72f)
            }

            // Forward panoramic cab + colored operator seat + styled driver + brand roof
            val cabCx = vx + fx * (lenM * 0.30f)
            val cabCz = vz + fz * (lenM * 0.30f)
            drawTiltedBox3D(cabCx - fx * 0.05f, baseY + heightM * 0.62f + cabYOffset, cabCz - fz * 0.05f, 0.18f, 0.16f, 0.18f, fx, fz, rx, rz, cabPitch, cabRoll, seatColor, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(cabCx, baseY + heightM * 0.69f + cabYOffset, cabCz, 0.16f, 0.18f, 0.12f, fx, fz, rx, rz, cabPitch, cabRoll, drvShirtCol, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(cabCx, baseY + heightM * 0.81f + cabYOffset, cabCz, 0.10f, 0.10f, 0.10f, fx, fz, rx, rz, cabPitch, cabRoll, drvSkinCol, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(cabCx, baseY + heightM * 0.68f + cabYOffset, cabCz, widthM * 0.28f, heightM * 0.20f, lenM * 0.15f, fx, fz, rx, rz, cabPitch, cabRoll, Color(0xFF3E6C80).copy(alpha = 0.70f), light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(cabCx, baseY + heightM * 0.90f + cabYOffset, cabCz, widthM * 0.31f, 0.06f, lenM * 0.18f, fx, fz, rx, rz, cabPitch, cabRoll, cabRoofColor, light, pose, focalPx, w, h, scratch)

            // Unloading auger pipe (pivots outward when active)
            val augerSwing = v.unloadingAugerProgress.toFloat().coerceIn(0f, 1f)
            val pipeRad = rad - (1.28f * augerSwing + 0.18f)
            val pFx = sin(pipeRad)
            val pFz = cos(pipeRad)
            val pRx = cos(pipeRad)
            val pRz = -sin(pipeRad)
            drawTiltedBox3D(vx - pRx * (1.2f + augerSwing * 1.4f), baseY + heightM * 0.88f, vz - pRz * (1.2f + augerSwing * 1.4f), 1.45f, 0.12f, 0.12f, pFx, pFz, pRx, pRz, pitchDeg, rollDeg, augerCol, light, pose, focalPx, w, h, scratch)

            // Feeder house + wide cutting header + spinning multi-bar pickup reel
            val headerW = ((def.headerWidthFt ?: 22.0).toFloat() * 0.3048f).coerceIn(widthM * 1.2f, 10.2f)
            val hdrY = if (v.headerLowered == true) gy + 0.32f else gy + 0.95f
            drawTiltedBox3D(vx + fx * (lenM * 0.48f), baseY + 0.72f, vz + fz * (lenM * 0.48f), 0.55f, 0.24f, 0.55f, fx, fz, rx, rz, pitchDeg, rollDeg, chassisColor, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(vx + fx * (lenM * 0.60f), hdrY, vz + fz * (lenM * 0.60f), headerW * 0.5f, 0.28f, 0.52f, fx, fz, rx, rz, pitchDeg, rollDeg, headerFrameCol, light, pose, focalPx, w, h, scratch)
            // Side crop dividers on header ends
            drawTiltedBox3D(vx + fx * (lenM * 0.64f) - rx * (headerW * 0.50f), hdrY + 0.08f, vz + fz * (lenM * 0.64f) - rz * (headerW * 0.50f), 0.10f, 0.16f, 0.62f, fx, fz, rx, rz, pitchDeg, rollDeg, headerReelCol, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(vx + fx * (lenM * 0.64f) + rx * (headerW * 0.50f), hdrY + 0.08f, vz + fz * (lenM * 0.64f) + rz * (headerW * 0.50f), 0.10f, 0.16f, 0.62f, fx, fz, rx, rz, pitchDeg, rollDeg, headerReelCol, light, pose, focalPx, w, h, scratch)
            // Rotating 4-bat reel around reel axis
            val reelCx = vx + fx * (lenM * 0.64f)
            val reelCy = hdrY + 0.42f
            val reelCz = vz + fz * (lenM * 0.64f)
            val reelPhase = v.rotorSpinDeg.toFloat() * (PI / 180.0).toFloat()
            val reelR = 0.24f
            for (b in 0 until 4) {
                val a = reelPhase + b * (PI.toFloat() * 0.5f)
                val barF = cos(a) * reelR
                val barY = sin(a) * reelR
                drawTiltedBox3D(reelCx + fx * barF, reelCy + barY, reelCz + fz * barF, headerW * 0.48f, 0.035f, 0.035f, fx, fz, rx, rz, pitchDeg, rollDeg, headerReelCol, light, pose, focalPx, w, h, scratch)
            }
        }
        VehicleBodyStyle.TRUCK, VehicleBodyStyle.PICKUP -> {
            // Cargo bed + live grain pile
            val bedX = vx - fx * (lenM * 0.14f)
            val bedZ = vz - fz * (lenM * 0.14f)
            drawTiltedBox3D(bedX, baseY + heightM * 0.48f, bedZ, widthM * 0.44f, heightM * 0.24f, lenM * 0.32f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFF333D38), light, pose, focalPx, w, h, scratch)
            if (def.hasGrainTank && v.grainTankFillL > 10.0) {
                val fillRatio = (v.grainTankFillL / max(def.tankCapacityL, 1.0)).toFloat().coerceIn(0.08f, 1f)
                drawTiltedBox3D(bedX, baseY + heightM * (0.64f + fillRatio * 0.14f), bedZ, widthM * 0.40f, 0.08f + fillRatio * 0.16f, lenM * 0.29f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFE7BD4A), light, pose, focalPx, w, h, scratch, taperTopFactor = 0.72f)
            }
            // Truck cab (classic snout for pre-1998 MB-1113, cab-over for modern)
            val isSnout = def.modelYear < 1998 || def.id == "mb_1113"
            val cabOffset = if (isSnout) lenM * 0.24f else lenM * 0.30f
            drawTiltedBox3D(vx + fx * cabOffset, baseY + heightM * 0.56f + cabYOffset, vz + fz * cabOffset, widthM * 0.41f, heightM * 0.30f, lenM * 0.17f, fx, fz, rx, rz, cabPitch, cabRoll, bodyColor, light, pose, focalPx, w, h, scratch)
            if (isSnout) {
                drawTiltedBox3D(vx + fx * (lenM * 0.40f), baseY + heightM * 0.44f, vz + fz * (lenM * 0.40f), widthM * 0.34f, heightM * 0.16f, lenM * 0.10f, fx, fz, rx, rz, pitchDeg, rollDeg, bodyColor, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.85f)
            }
            // Windshield + mirrors
            drawTiltedBox3D(vx + fx * (cabOffset + lenM * 0.08f), baseY + heightM * 0.68f + cabYOffset, vz + fz * (cabOffset + lenM * 0.08f), widthM * 0.35f, heightM * 0.14f, 0.12f, fx, fz, rx, rz, cabPitch, cabRoll, Color(0xFF5FA8C9), light, pose, focalPx, w, h, scratch)
        }
        VehicleBodyStyle.SELF_PROPELLED_SPRAYER -> {
            drawTiltedBox3D(vx, baseY + heightM * 0.54f, vz, widthM * 0.34f, heightM * 0.20f, lenM * 0.36f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFF0F4F2), light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(vx + fx * (lenM * 0.26f), baseY + heightM * 0.74f + cabYOffset, vz + fz * (lenM * 0.26f), widthM * 0.28f, heightM * 0.20f, lenM * 0.15f, fx, fz, rx, rz, cabPitch, cabRoll, bodyColor, light, pose, focalPx, w, h, scratch)
            val deploy = (v.sprayerBoomDeploy ?: (if (v.isSprayerActive == true) 1.0 else 0.0)).toFloat().coerceIn(0f, 1f)
            val boomHalf = 1.6f + deploy * 6.8f
            drawTiltedBox3D(vx - fx * (lenM * 0.44f), baseY + heightM * 0.48f, vz - fz * (lenM * 0.44f), boomHalf, 0.08f, 0.12f, fx, fz, rx, rz, pitchDeg, rollDeg, accentColor, light, pose, focalPx, w, h, scratch)
        }
        VehicleBodyStyle.MOTO -> {
            drawTiltedBox3D(vx, baseY + 0.75f, vz, 0.16f, 0.18f, lenM * 0.35f, fx, fz, rx, rz, pitchDeg, rollDeg, bodyColor, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(vx + fx * 0.35f, baseY + 1.05f, vz + fz * 0.35f, 0.38f, 0.05f, 0.05f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFF202421), light, pose, focalPx, w, h, scratch)
        }
        else -> {
            // High-detail Tractors (Compact, Classic, Modern High-Power, Articulated 4WD)
            val isArticulated = def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD
            val rearFrameFx = if (isArticulated) rFx else fx
            val rearFrameFz = if (isArticulated) rFz else fz
            val rearFrameRx = if (isArticulated) rRx else rx
            val rearFrameRz = if (isArticulated) rRz else rz

            val taper = when (def.hoodProfile) {
                HoodProfile.MODERN_AERO -> 0.82f
                HoodProfile.VINTAGE_ROUND -> 0.78f
                else -> 0.90f
            }

            // Sculpted Hood (TRUE brand hood color — e.g. GREEN on John Deere) + thin side brand stripe/lettering
            val hoodCx = vx + fx * (lenM * 0.20f)
            val hoodCy = baseY + heightM * 0.46f
            val hoodCz = vz + fz * (lenM * 0.20f)
            drawTiltedBox3D(hoodCx, hoodCy, hoodCz, widthM * 0.26f, heightM * 0.20f, lenM * 0.24f, fx, fz, rx, rz, pitchDeg, rollDeg, hoodColor, light, pose, focalPx, w, h, scratch, taperTopFactor = taper)
            // Metallic hood specular sheen in High/Ultra/Mega-Realista reflection tiers
            if (gfx.reflectionQuality >= 2) {
                val hoodHighlight = lerpColor(hoodColor, Color.White, 0.26f)
                drawTiltedBox3D(hoodCx, hoodCy + heightM * 0.195f, hoodCz, widthM * 0.12f, 0.012f, lenM * 0.21f, fx, fz, rx, rz, pitchDeg, rollDeg, hoodHighlight, light, pose, focalPx, w, h, scratch)
            }
            // Thin flank decals on LEFT and RIGHT sides ONLY (so top of hood remains clean hoodColor!)
            val stripeHalfW = widthM * 0.262f
            drawTiltedBox3D(hoodCx - rx * stripeHalfW, baseY + heightM * 0.52f, hoodCz - rz * stripeHalfW, 0.015f, 0.028f, lenM * 0.20f, fx, fz, rx, rz, pitchDeg, rollDeg, stripeColor, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(hoodCx + rx * stripeHalfW, baseY + heightM * 0.52f, hoodCz + rz * stripeHalfW, 0.015f, 0.028f, lenM * 0.20f, fx, fz, rx, rz, pitchDeg, rollDeg, stripeColor, light, pose, focalPx, w, h, scratch)
            // Model number badge near nose flank
            drawTiltedBox3D(hoodCx + fx * (lenM * 0.14f) - rx * (stripeHalfW + 0.005f), baseY + heightM * 0.52f, hoodCz + fz * (lenM * 0.14f) - rz * (stripeHalfW + 0.005f), 0.012f, 0.036f, 0.14f, fx, fz, rx, rz, pitchDeg, rollDeg, decalColor, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(hoodCx + fx * (lenM * 0.14f) + rx * (stripeHalfW + 0.005f), baseY + heightM * 0.52f, hoodCz + fz * (lenM * 0.14f) + rz * (stripeHalfW + 0.005f), 0.012f, 0.036f, 0.14f, fx, fz, rx, rz, pitchDeg, rollDeg, decalColor, light, pose, focalPx, w, h, scratch)
            // Front dark radiator grille + brand emblem + LED headlights on nose
            drawTiltedBox3D(vx + fx * (lenM * 0.445f), baseY + heightM * 0.45f, vz + fz * (lenM * 0.445f), widthM * 0.20f, heightM * 0.16f, 0.06f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFF151817), light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(vx + fx * (lenM * 0.46f), baseY + heightM * 0.47f, vz + fz * (lenM * 0.46f), 0.06f, 0.05f, 0.02f, fx, fz, rx, rz, pitchDeg, rollDeg, decalColor, light, pose, focalPx, w, h, scratch)
            val headCol = if (v.headlightsOn) Color(0xFFFFFFFF) else Color(0xFFB0BEC5)
            drawTiltedBox3D(vx + fx * (lenM * 0.45f) - rx * (widthM * 0.13f), baseY + heightM * 0.50f, vz + fz * (lenM * 0.45f) - rz * (widthM * 0.13f), 0.07f, 0.05f, 0.04f, fx, fz, rx, rz, pitchDeg, rollDeg, headCol, 1f, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(vx + fx * (lenM * 0.45f) + rx * (widthM * 0.13f), baseY + heightM * 0.50f, vz + fz * (lenM * 0.45f) + rz * (widthM * 0.13f), 0.07f, 0.05f, 0.04f, fx, fz, rx, rz, pitchDeg, rollDeg, headCol, 1f, pose, focalPx, w, h, scratch)

            // Front 3-Point Hitch & Iron Suitcase Weights (green carrier on JD, chassis dark otherwise)
            if (tuning.hasExtraFrontWeights || (v.frontBallastKg ?: 0.0) > 50.0 || def.horsepower >= 140.0) {
                val noseX = vx + fx * (lenM * 0.52f)
                val noseZ = vz + fz * (lenM * 0.52f)
                val weightCol = if (def.brand.contains("John Deere", ignoreCase = true)) bodyColor else chassisColor
                drawTiltedBox3D(noseX, baseY + heightM * 0.34f, noseZ, widthM * 0.20f, 0.16f, 0.16f, fx, fz, rx, rz, pitchDeg, rollDeg, weightCol, light, pose, focalPx, w, h, scratch)
            }

            // Rear mudguard fenders in authentic fenderColor
            drawTiltedBox3D(vx + rearFrameFx * rearZ - rearFrameRx * (widthM * 0.43f), baseY + heightM * 0.46f, vz + rearFrameFz * rearZ - rearFrameRz * (widthM * 0.43f), 0.18f, 0.06f, lenM * 0.16f, rearFrameFx, rearFrameFz, rearFrameRx, rearFrameRz, pitchDeg, rollDeg, fenderColor, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(vx + rearFrameFx * rearZ + rearFrameRx * (widthM * 0.43f), baseY + heightM * 0.46f, vz + rearFrameFz * rearZ + rearFrameRz * (widthM * 0.43f), 0.18f, 0.06f, lenM * 0.16f, rearFrameFx, rearFrameFz, rearFrameRx, rearFrameRz, pitchDeg, rollDeg, fenderColor, light, pose, focalPx, w, h, scratch)

            // Exhaust stack(s) based on tuning and brand exhaustColor
            when (tuning.exhaustStyle) {
                ExhaustStyle.DUAL_STACK -> {
                    drawTiltedBox3D(vx + fx * (lenM * 0.08f) - rx * (widthM * 0.29f), baseY + heightM * 0.74f, vz + fz * (lenM * 0.08f) - rz * (widthM * 0.29f), 0.055f, heightM * 0.25f, 0.055f, fx, fz, rx, rz, pitchDeg, rollDeg, exhaustColor, light, pose, focalPx, w, h, scratch)
                    drawTiltedBox3D(vx + fx * (lenM * 0.08f) + rx * (widthM * 0.29f), baseY + heightM * 0.74f, vz + fz * (lenM * 0.08f) + rz * (widthM * 0.29f), 0.055f, heightM * 0.25f, 0.055f, fx, fz, rx, rz, pitchDeg, rollDeg, exhaustColor, light, pose, focalPx, w, h, scratch)
                }
                ExhaustStyle.CENTER_HOOD -> {
                    drawTiltedBox3D(vx + fx * (lenM * 0.22f), baseY + heightM * 0.70f, vz + fz * (lenM * 0.22f), 0.05f, heightM * 0.22f, 0.05f, fx, fz, rx, rz, pitchDeg, rollDeg, exhaustColor, light, pose, focalPx, w, h, scratch)
                }
                else -> {
                    drawTiltedBox3D(vx + fx * (lenM * 0.08f) + rx * (widthM * 0.29f), baseY + heightM * 0.74f, vz + fz * (lenM * 0.08f) + rz * (widthM * 0.29f), 0.06f, heightM * 0.25f, 0.06f, fx, fz, rx, rz, pitchDeg, rollDeg, exhaustColor, light, pose, focalPx, w, h, scratch)
                }
            }

            // Cab + Brand-Colored Operator Seat (e.g. YELLOW seat on John Deere) + Seated Driver + Steering Wheel + Roof
            val cabFx = if (isArticulated) fx else rearFrameFx
            val cabFz = if (isArticulated) fz else rearFrameFz
            val cabRx = if (isArticulated) rx else rearFrameRx
            val cabRz = if (isArticulated) rz else rearFrameRz
            val cabZ = if (isArticulated) lenM * 0.04f else -lenM * 0.16f
            val cabCx = vx + cabFx * cabZ
            val cabCz = vz + cabFz * cabZ

            if (def.hasCab) {
                // Brand-accurate seat cushion & backrest (YELLOW on John Deere!)
                drawTiltedBox3D(cabCx - cabFx * 0.10f, baseY + heightM * 0.56f + cabYOffset, cabCz - cabFz * 0.10f, 0.20f, 0.07f, 0.18f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, seatColor, light, pose, focalPx, w, h, scratch)
                drawTiltedBox3D(cabCx - cabFx * 0.22f, baseY + heightM * 0.67f + cabYOffset, cabCz - cabFz * 0.22f, 0.18f, 0.14f, 0.05f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, seatColor, light, pose, focalPx, w, h, scratch)
                // Seated operator torso, head & hair
                drawTiltedBox3D(cabCx - cabFx * 0.06f, baseY + heightM * 0.68f + cabYOffset, cabCz - cabFz * 0.06f, 0.16f, 0.17f, 0.13f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, drvShirtCol, light, pose, focalPx, w, h, scratch)
                drawTiltedBox3D(cabCx - cabFx * 0.05f, baseY + heightM * 0.81f + cabYOffset, cabCz - cabFz * 0.05f, 0.10f, 0.10f, 0.10f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, drvSkinCol, light, pose, focalPx, w, h, scratch)
                drawTiltedBox3D(cabCx - cabFx * 0.06f, baseY + heightM * 0.87f + cabYOffset, cabCz - cabFz * 0.06f, 0.11f, 0.045f, 0.11f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, drvHairCol, light, pose, focalPx, w, h, scratch)
                // Sculpted dashboard cowl + glowing instrument panel + steering wheel inside cab
                drawTiltedBox3D(cabCx + cabFx * 0.24f, baseY + heightM * 0.60f + cabYOffset, cabCz + cabFz * 0.24f, widthM * 0.22f, 0.09f, 0.12f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFF1F2421), light, pose, focalPx, w, h, scratch)
                if (v.engineRunning == true) {
                    drawTiltedBox3D(cabCx + cabFx * 0.20f, baseY + heightM * 0.66f + cabYOffset, cabCz + cabFz * 0.20f, 0.12f, 0.035f, 0.02f, cabFx, cabFz, cabRx, cabRz, cabPitch + 18f, cabRoll, Color(0xFF10B981), 1f, pose, focalPx, w, h, scratch)
                }
                drawTiltedBox3D(cabCx + cabFx * 0.18f, baseY + heightM * 0.67f + cabYOffset, cabCz + cabFz * 0.18f, 0.13f, 0.03f, 0.13f, cabFx, cabFz, cabRx, cabRz, cabPitch + 24f, cabRoll, Color(0xFF1A1E1B), light, pose, focalPx, w, h, scratch)

                // Glazed cab enclosure + sky glass reflection highlight + structural dark corner pillars
                drawTiltedBox3D(cabCx, baseY + heightM * 0.72f + cabYOffset, cabCz, widthM * 0.30f, heightM * 0.23f, lenM * 0.18f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFF3E6F88).copy(alpha = 0.56f), light, pose, focalPx, w, h, scratch)
                if (gfx.reflectionQuality >= 2 && !isNight) {
                    drawTiltedBox3D(cabCx + cabFx * (lenM * 0.175f), baseY + heightM * 0.78f + cabYOffset, cabCz + cabFz * (lenM * 0.175f), widthM * 0.22f, 0.03f, 0.015f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFFE1F5FE).copy(alpha = 0.42f), 1f, pose, focalPx, w, h, scratch)
                }
                if (detailLevel >= 1) {
                    val pillarX = widthM * 0.29f
                    val pillarZ = lenM * 0.17f
                    val pH = heightM * 0.23f
                    val pY = baseY + heightM * 0.72f + cabYOffset
                    drawTiltedBox3D(cabCx + cabFx * pillarZ - cabRx * pillarX, pY, cabCz + cabFz * pillarZ - cabRz * pillarX, 0.035f, pH, 0.035f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFF1B1F1D), light, pose, focalPx, w, h, scratch)
                    drawTiltedBox3D(cabCx + cabFx * pillarZ + cabRx * pillarX, pY, cabCz + cabFz * pillarZ + cabRz * pillarX, 0.035f, pH, 0.035f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFF1B1F1D), light, pose, focalPx, w, h, scratch)
                    drawTiltedBox3D(cabCx + cabFx * pillarZ - cabRx * (pillarX + 0.14f), pY + 0.15f, cabCz + cabFz * pillarZ - cabRz * (pillarX + 0.14f), 0.06f, 0.14f, 0.04f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFF1F2421), light, pose, focalPx, w, h, scratch)
                    drawTiltedBox3D(cabCx + cabFx * pillarZ + cabRx * (pillarX + 0.14f), pY + 0.15f, cabCz + cabFz * pillarZ + cabRz * (pillarX + 0.14f), 0.06f, 0.14f, 0.04f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFF1F2421), light, pose, focalPx, w, h, scratch)
                }

                // Cab roof cap in brand-faithful cabRoofColor
                val roofY = baseY + heightM * 0.96f + cabYOffset
                drawTiltedBox3D(cabCx, roofY, cabCz, widthM * 0.33f, 0.06f, lenM * 0.21f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, cabRoofColor, light, pose, focalPx, w, h, scratch)

                if (tuning.hasGpsDome) {
                    drawTiltedBox3D(cabCx + cabFx * (lenM * 0.13f), roofY + 0.09f, cabCz + cabFz * (lenM * 0.13f), 0.11f, 0.055f, 0.11f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFFFBC02D), light, pose, focalPx, w, h, scratch)
                }
                if (tuning.hasBeacon || v.engineRunning == true) {
                    val flash = if (sin(animClockSec * 8.5f) > 0f) 1.0f else 0.45f
                    drawTiltedBox3D(cabCx - cabFx * (lenM * 0.13f) - cabRx * (widthM * 0.18f), roofY + 0.10f, cabCz - cabFz * (lenM * 0.13f) - cabRz * (widthM * 0.18f), 0.06f, 0.07f, 0.06f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFFFF8F00).copy(alpha = flash), 1f, pose, focalPx, w, h, scratch)
                }
                if (tuning.hasLedBar || v.workLightsOn == true) {
                    drawTiltedBox3D(cabCx + cabFx * (lenM * 0.19f), roofY + 0.03f, cabCz + cabFz * (lenM * 0.19f), widthM * 0.25f, 0.035f, 0.04f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFFE1F5FE), 1f, pose, focalPx, w, h, scratch)
                }
            } else {
                // Open-station ROPS + brand seat (yellow on JD!) + styled operator
                drawTiltedBox3D(cabCx - cabFx * 0.05f, baseY + heightM * 0.49f + cabYOffset, cabCz - cabFz * 0.05f, 0.20f, 0.06f, 0.18f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, seatColor, light, pose, focalPx, w, h, scratch)
                drawTiltedBox3D(cabCx - cabFx * 0.16f, baseY + heightM * 0.58f + cabYOffset, cabCz - cabFz * 0.16f, 0.18f, 0.12f, 0.05f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, seatColor, light, pose, focalPx, w, h, scratch)
                drawTiltedBox3D(cabCx, baseY + heightM * 0.58f + cabYOffset, cabCz, 0.18f, 0.16f, 0.14f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, drvShirtCol, light, pose, focalPx, w, h, scratch)
                drawTiltedBox3D(cabCx, baseY + heightM * 0.70f + cabYOffset, cabCz, 0.11f, 0.11f, 0.11f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, drvSkinCol, light, pose, focalPx, w, h, scratch)
                drawTiltedBox3D(cabCx, baseY + heightM * 0.76f + cabYOffset, cabCz, 0.12f, 0.04f, 0.12f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, drvHairCol, light, pose, focalPx, w, h, scratch)
                drawTiltedBox3D(cabCx - cabFx * 0.14f, baseY + heightM * 0.84f + cabYOffset, cabCz - cabFz * 0.14f, widthM * 0.28f, 0.045f, 0.045f, cabFx, cabFz, cabRx, cabRz, cabPitch, cabRoll, Color(0xFF202421), light, pose, focalPx, w, h, scratch)
            }

            // Rear 3-Point Hitch Links
            drawTiltedBox3D(vx - rearFrameFx * (lenM * 0.44f), baseY + 0.45f, vz - rearFrameFz * (lenM * 0.44f), widthM * 0.18f, 0.07f, 0.18f, rearFrameFx, rearFrameFz, rearFrameRx, rearFrameRz, pitchDeg, rollDeg, chassisColor, light, pose, focalPx, w, h, scratch)
        }
    }

    // Brake / tail lights glow when braking or handbrake engaged
    if (v.isBraking == true || v.handbrakeOn == true) {
        val tCol = Color(0xFFFF1744)
        drawTiltedBox3D(vx - rearAxisSafeFx(def, rFx, fx) * (lenM * 0.44f) - rx * (widthM * 0.28f), baseY + 0.94f, vz - rearAxisSafeFz(def, rFz, fz) * (lenM * 0.44f) - rz * (widthM * 0.28f), 0.09f, 0.07f, 0.05f, fx, fz, rx, rz, pitchDeg, rollDeg, tCol, 1f, pose, focalPx, w, h, scratch)
        drawTiltedBox3D(vx - rearAxisSafeFx(def, rFx, fx) * (lenM * 0.44f) + rx * (widthM * 0.28f), baseY + 0.94f, vz - rearAxisSafeFz(def, rFz, fz) * (lenM * 0.44f) + rz * (widthM * 0.28f), 0.09f, 0.07f, 0.05f, fx, fz, rx, rz, pitchDeg, rollDeg, tCol, 1f, pose, focalPx, w, h, scratch)
    }
}

private fun rearAxisSafeFx(def: com.example.domain.VehicleDef, rFx: Float, fx: Float): Float =
    if (def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD) rFx else fx

private fun rearAxisSafeFz(def: com.example.domain.VehicleDef, rFz: Float, fz: Float): Float =
    if (def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD) rFz else fz

internal fun DrawScope.drawImplement3D(
    imp: OwnedImplement,
    gy: Float,
    gfx: GraphicsConfig,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    light: Float,
    scratch: ViewportScratch3D,
) {
    val def = getImplementDef(imp.defId)
    val rad = imp.headingDeg.toFloat() * (PI / 180.0).toFloat()
    val fx = sin(rad)
    val fz = cos(rad)
    val rx = cos(rad)
    val rz = -sin(rad)
    val ix = imp.x.toFloat()
    val iz = imp.z.toFloat()
    val liftY = (imp.liftProgress.toFloat().coerceIn(0f, 1f)) * 0.44f
    val pitchDeg = (imp.pitchDeg ?: 0.0).toFloat().coerceIn(-16f, 16f)
    val rollDeg = (imp.rollDeg ?: 0.0).toFloat().coerceIn(-16f, 16f)
    val impPalette = resolveImplementBrandPalette(def)
    val col = impPalette.primaryRgb.toComposeColor()
    val impRimCol = impPalette.rimRgb.toComposeColor()
    val halfW = (def.workingWidthM.toFloat().coerceIn(1.8f, 9.5f)) * 0.48f
    val wheelSegs = if (gfx.vehicleDetailLevel >= 1) 12 else 8

    drawRotatedBox3D(ix, gy + 0.02f, iz, min(halfW, 2.8f), 0.01f, 1.2f, fx, fz, rx, rz, Color.Black.copy(alpha = 0.28f), 1f, pose, focalPx, w, h, scratch)

    when (def.category) {
        ImplementCategory.CARRETA -> {
            val bedCol = col
            drawTiltedBox3D(ix, gy + 0.98f, iz, 1.15f, 0.52f, 2.25f, fx, fz, rx, rz, pitchDeg, rollDeg, bedCol, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(ix, gy + 0.48f, iz + fz * 1.4f, 0.16f, 0.08f, 0.95f, fx, fz, rx, rz, pitchDeg, rollDeg, impPalette.chassisRgb.toComposeColor(), light, pose, focalPx, w, h, scratch)
            // Hydraulic front piston + rear brake strip
            drawTiltedBox3D(ix + fx * 1.85f, gy + 0.88f, iz + fz * 1.85f, 0.09f, 0.38f, 0.09f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFB0BEC5), light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(ix - fx * 2.26f, gy + 0.58f, iz - fz * 2.26f, 0.92f, 0.07f, 0.04f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFD32F2F), light, pose, focalPx, w, h, scratch)
            if (imp.fillL > 10.0) {
                val fillRatio = (imp.fillL / max(def.capacityL, 1.0)).toFloat().coerceIn(0.08f, 1f)
                drawTiltedBox3D(ix, gy + 1.38f + fillRatio * 0.24f, iz, 1.05f, 0.12f + fillRatio * 0.24f, 2.08f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFE7BD4A), light, pose, focalPx, w, h, scratch, taperTopFactor = 0.68f)
            }
            val spin = imp.wheelSpinDeg.toFloat()
            drawCylinderWheel3D(ix - rx * 1.10f - fx * 0.6f, gy + 0.44f, iz - rz * 1.10f - fz * 0.6f, 0.44f, 0.18f, spin, fx, fz, rx, rz, Color(0xFF1A1E1B), impRimCol, light, wheelSegs, pose, focalPx, w, h, scratch)
            drawCylinderWheel3D(ix + rx * 1.10f - fx * 0.6f, gy + 0.44f, iz + rz * 1.10f - fz * 0.6f, 0.44f, 0.18f, spin, fx, fz, rx, rz, Color(0xFF1A1E1B), impRimCol, light, wheelSegs, pose, focalPx, w, h, scratch)
            drawCylinderWheel3D(ix - rx * 1.10f + fx * 0.7f, gy + 0.44f, iz - rz * 1.10f + fz * 0.7f, 0.44f, 0.18f, spin, fx, fz, rx, rz, Color(0xFF1A1E1B), impRimCol, light, wheelSegs, pose, focalPx, w, h, scratch)
            drawCylinderWheel3D(ix + rx * 1.10f + fx * 0.7f, gy + 0.44f, iz + rz * 1.10f + fz * 0.7f, 0.44f, 0.18f, spin, fx, fz, rx, rz, Color(0xFF1A1E1B), impRimCol, light, wheelSegs, pose, focalPx, w, h, scratch)
        }
        ImplementCategory.PLATAFORMA_MILHO -> {
            // Front-mounted corn header: rear trough frame, auger, and tapered row snouts forward
            val span = min(halfW, 4.8f)
            val liftCornY = liftY * 0.9f
            drawTiltedBox3D(ix, gy + 0.56f + liftCornY, iz, span, 0.30f, 0.42f, fx, fz, rx, rz, pitchDeg, rollDeg, col, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(ix - fx * 0.22f, gy + 0.92f + liftCornY, iz - fz * 0.22f, span * 0.96f, 0.14f, 0.08f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFF263238), light, pose, focalPx, w, h, scratch)
            // Rotating intake cross-auger
            drawTiltedBox3D(ix + fx * 0.16f, gy + 0.62f + liftCornY, iz + fz * 0.16f, span * 0.92f, 0.12f, 0.12f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFB0BEC5), light, pose, focalPx, w, h, scratch)
            val snouts = if (span >= 3.2f) 7 else 5
            val halfCount = snouts / 2
            for (s in -halfCount..halfCount) {
                val lat = s * (span / max(1, halfCount + 1))
                drawTiltedBox3D(
                    ix + fx * 0.72f + rx * lat,
                    gy + 0.38f + liftCornY,
                    iz + fz * 0.72f + rz * lat,
                    0.13f, 0.15f, 0.52f,
                    fx, fz, rx, rz, pitchDeg + 6f, rollDeg, col, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.58f,
                )
            }
        }
        ImplementCategory.CULTIVADOR -> {
            val span = min(halfW, 3.6f)
            drawTiltedBox3D(ix, gy + 0.52f + liftY, iz, span, 0.12f, 0.56f, fx, fz, rx, rz, pitchDeg, rollDeg, col, light, pose, focalPx, w, h, scratch)
            // Heavy S-tine shanks + duckfoot points dipping into soil when lowered
            for (s in -2..2) {
                val lat = s * (span * 0.42f)
                drawTiltedBox3D(
                    ix + rx * lat, gy + 0.26f + liftY * 0.7f, iz + rz * lat,
                    0.06f, 0.24f, 0.12f, fx, fz, rx, rz, pitchDeg, rollDeg,
                    Color(0xFF455A64), light, pose, focalPx, w, h, scratch,
                )
            }
            // Rear crumbling cage roller
            val discSpin = (imp.discSpinDeg ?: 0.0).toFloat()
            drawCylinderWheel3D(ix - fx * 0.62f - rx * (span * 0.45f), gy + 0.22f + liftY * 0.8f, iz - fz * 0.62f - rz * (span * 0.45f), 0.20f, span * 0.42f, discSpin, fx, fz, rx, rz, Color(0xFF78909C), impRimCol, light, 8, pose, focalPx, w, h, scratch)
            drawCylinderWheel3D(ix - fx * 0.62f + rx * (span * 0.45f), gy + 0.22f + liftY * 0.8f, iz - fz * 0.62f + rz * (span * 0.45f), 0.20f, span * 0.42f, discSpin, fx, fz, rx, rz, Color(0xFF78909C), impRimCol, light, 8, pose, focalPx, w, h, scratch)
        }
        ImplementCategory.PLANTADEIRA, ImplementCategory.SEMEADORA -> {
            drawTiltedBox3D(ix, gy + 0.56f + liftY, iz, min(halfW, 3.6f), 0.14f, 0.35f, fx, fz, rx, rz, pitchDeg, rollDeg, col, light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(ix - rx * 0.9f, gy + 1.05f + liftY, iz - rz * 0.9f, 0.75f, 0.36f, 0.48f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFF3F6F4), light, pose, focalPx, w, h, scratch, taperTopFactor = 1.12f)
            drawTiltedBox3D(ix + rx * 0.9f, gy + 1.05f + liftY, iz + rz * 0.9f, 0.75f, 0.36f, 0.48f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFF3F6F4), light, pose, focalPx, w, h, scratch, taperTopFactor = 1.12f)
            val discSpin = (imp.discSpinDeg ?: 0.0).toFloat()
            val rowSpan = min(halfW, 3.2f)
            for (r in -2..2) {
                val lat = r * (rowSpan * 0.42f)
                drawCylinderWheel3D(ix + rx * lat - fx * 0.25f, gy + 0.24f + liftY, iz + rz * lat - fz * 0.25f, 0.24f, 0.04f, discSpin, fx, fz, rx, rz, Color(0xFF607D8B), impRimCol, light, 10, pose, focalPx, w, h, scratch)
            }
        }
        ImplementCategory.PULVERIZADOR -> {
            val deploy = imp.boomDeployProgress.toFloat().coerceIn(0f, 1f)
            val wingSpan = 1.1f + deploy * (halfW - 1.1f)
            drawTiltedBox3D(ix, gy + 0.96f + liftY, iz, 0.85f, 0.46f, 1.08f, fx, fz, rx, rz, pitchDeg, rollDeg, Color(0xFFF3F6F4), light, pose, focalPx, w, h, scratch)
            drawTiltedBox3D(ix - fx * 0.92f, gy + 0.82f + liftY, iz - fz * 0.92f, wingSpan, 0.07f, 0.09f, fx, fz, rx, rz, pitchDeg, rollDeg, col, light, pose, focalPx, w, h, scratch)
        }
        ImplementCategory.DISTRIBUIDOR -> {
            drawTiltedBox3D(ix, gy + 0.98f + liftY, iz, 0.82f, 0.48f, 0.82f, fx, fz, rx, rz, pitchDeg, rollDeg, col, light, pose, focalPx, w, h, scratch, taperTopFactor = 1.18f)
            val discA = imp.rotorSpinDeg.toFloat() * (PI / 180.0).toFloat()
            val dFx = sin(rad + discA)
            val dFz = cos(rad + discA)
            val dRx = cos(rad + discA)
            val dRz = -sin(rad + discA)
            drawTiltedBox3D(ix - fx * 0.4f, gy + 0.48f + liftY, iz - fz * 0.4f, 0.34f, 0.04f, 0.34f, dFx, dFz, dRx, dRz, pitchDeg, rollDeg, Color(0xFFCFD8DC), light, pose, focalPx, w, h, scratch)
        }
        else -> {
            drawTiltedBox3D(ix, gy + 0.48f + liftY, iz, min(halfW, 3.2f), 0.14f, 0.72f, fx, fz, rx, rz, pitchDeg, rollDeg, col, light, pose, focalPx, w, h, scratch)
            val discSpin = (imp.discSpinDeg ?: 0.0).toFloat()
            val discSpan = min(halfW, 2.8f)
            for (d in -2..2) {
                val lat = d * (discSpan * 0.44f)
                drawCylinderWheel3D(ix + rx * lat, gy + 0.28f + liftY * 0.8f, iz + rz * lat, 0.28f, 0.04f, discSpin, fx, fz, rx, rz, Color(0xFF78909C), Color(0xFF37474F), light, 10, pose, focalPx, w, h, scratch)
            }
        }
    }
}

internal fun DrawScope.drawSimpleBoxVehicle3D(
    x: Float,
    gy: Float,
    z: Float,
    headingDeg: Float,
    widthM: Float,
    heightM: Float,
    lenM: Float,
    color: Color,
    light: Float,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val rad = headingDeg * (PI / 180.0).toFloat()
    val fx = sin(rad)
    val fz = cos(rad)
    val rx = cos(rad)
    val rz = -sin(rad)
    drawRotatedBox3D(x, gy + heightM * 0.42f, z, widthM * 0.46f, heightM * 0.36f, lenM * 0.48f, fx, fz, rx, rz, color, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(x, gy + heightM * 0.78f, z, widthM * 0.38f, heightM * 0.18f, lenM * 0.28f, fx, fz, rx, rz, Color(0xFF283740), light, pose, focalPx, w, h, scratch)
}
