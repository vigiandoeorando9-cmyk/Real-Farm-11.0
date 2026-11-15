package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.domain.BrazilianMapDef
import com.example.domain.CameraMode
import com.example.domain.DEFAULT_PLAYER_APPEARANCE
import com.example.domain.GraphicsConfig
import com.example.domain.SoilState
import com.example.domain.TerrainCell
import com.example.domain.WorldPropType
import com.example.domain.WorldState
import com.example.domain.getCropById
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal fun DrawScope.drawSkyAndAtmosphere(
    w: Float,
    h: Float,
    horizonY: Float,
    hour: Float,
    cloudCover: Float,
    pose: CameraPose3D,
    focalPx: Float,
    mapDef: BrazilianMapDef,
    gfx: GraphicsConfig? = null,
    animClockSec: Float = 0f,
) {
    val lightingQ = gfx?.lightingQuality ?: 2
    val isSemiArid = !mapDef.hasRiver || mapDef.id in setOf("map_ce", "map_pe", "map_ba", "map_al")
    val isFloodplain = mapDef.id in setOf("map_pa", "map_ro", "map_ac", "map_to", "mod_map_pantanal_sul")
    val skyTop: Color
    val skyHorizon: Color
    when {
        hour < 5.2f || hour > 20.5f -> {
            skyTop = Color(0xFF050A14)
            skyHorizon = Color(0xFF132238)
        }
        hour < 7.2f -> {
            val t = (hour - 5.2f) / 2.0f
            skyTop = lerpColor(Color(0xFF172742), Color(0xFF2874A6), t)
            skyHorizon = lerpColor(Color(0xFFE57348), Color(0xFFB5DAF0), t)
        }
        hour > 17.5f -> {
            val t = ((hour - 17.5f) / 3.0f).coerceIn(0f, 1f)
            skyTop = lerpColor(Color(0xFF256CA1), Color(0xFF0F192A), t)
            skyHorizon = lerpColor(Color(0xFFA6D3EA), Color(0xFFD96D3B), t)
        }
        else -> {
            val dayTop = if (isSemiArid) Color(0xFF1A73B8) else Color(0xFF1C62A4)
            val dayHorizon = if (isSemiArid) Color(0xFFD6E7ED) else Color(0xFFB8E0F4)
            skyTop = lerpColor(dayTop, Color(0xFF4F606D), cloudCover * 0.7f)
            skyHorizon = lerpColor(dayHorizon, Color(0xFF83939E), cloudCover * 0.65f)
        }
    }

    val skyMid = lerpColor(skyTop, skyHorizon, 0.54f)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyTop, skyMid, skyHorizon),
            startY = 0f,
            endY = horizonY + 18f,
        ),
        topLeft = Offset.Zero,
        size = Size(w, horizonY + 20f),
    )

    // Projected Sun / Moon with volumetric corona & atmospheric bloom
    val sunAngle = ((hour - 6f) / 12f) * PI.toFloat()
    val isDaySun = hour in 5.8f..18.5f
    val bodyDirX = cos(sunAngle) * 180f
    val bodyDirY = abs(sin(sunAngle)) * 110f + 24f
    val bodyDirZ = 140f
    val bodyProj = projectRaw(
        pose.camX + bodyDirX,
        pose.camY + bodyDirY,
        pose.camZ + bodyDirZ,
        pose, focalPx, w, h,
    )
    if (bodyProj.visible && bodyProj.screenY < horizonY) {
        if (isDaySun) {
            val hazeAlpha = (1f - cloudCover * 0.65f).coerceIn(0.15f, 1f)
            if (lightingQ >= 2) {
                drawCircle(Color(0xFFFFF8E1).copy(alpha = 0.08f * hazeAlpha), radius = 112f, center = Offset(bodyProj.screenX, bodyProj.screenY))
            }
            drawCircle(Color(0xFFFFF59D).copy(alpha = 0.18f * hazeAlpha), radius = 58f, center = Offset(bodyProj.screenX, bodyProj.screenY))
            drawCircle(Color(0xFFFFF59D).copy(alpha = 0.36f * hazeAlpha), radius = 32f, center = Offset(bodyProj.screenX, bodyProj.screenY))
            drawCircle(Color(0xFFFFFDE7).copy(alpha = 0.94f * hazeAlpha), radius = 14f, center = Offset(bodyProj.screenX, bodyProj.screenY))
        } else {
            drawCircle(Color(0xFFE3F2FD).copy(alpha = 0.18f), radius = 28f, center = Offset(bodyProj.screenX, bodyProj.screenY))
            drawCircle(Color(0xFFE3F2FD).copy(alpha = 0.80f), radius = 11f, center = Offset(bodyProj.screenX, bodyProj.screenY))
        }
    }

    // Layered procedural cumulus clouds (scales with weather cloudCover and lightingQuality)
    val effectiveClouds = if (lightingQ >= 2) max(cloudCover, 0.20f) else cloudCover
    if (effectiveClouds > 0.12f && horizonY > 40f) {
        val isNightSky = hour < 5.6f || hour > 19.8f
        val cloudCol = if (isNightSky) Color(0xFF28384A) else Color(0xFFF7FBFE)
        val cloudShade = if (isNightSky) Color(0xFF1B2734) else Color(0xFFC5D5E0)
        val cloudAlpha = (0.14f + cloudCover * 0.28f).coerceIn(0.14f, 0.48f)
        val drift = (animClockSec * 1.4f) % 240f
        val cloudCount = if (lightingQ >= 3) 7 else 5
        for (c in 0 until cloudCount) {
            val wx = pose.camX + ((c - cloudCount / 2) * 84f) + drift * 0.12f
            val wz = pose.camZ + 165f + (c % 3) * 32f
            val cp = projectRaw(wx, pose.camY + 46f + (c % 3) * 9f, wz, pose, focalPx, w, h)
            if (cp.visible && cp.screenY < horizonY - 8f) {
                val cw = 135f + (c % 3) * 42f
                drawOval(
                    color = cloudShade.copy(alpha = cloudAlpha * 0.72f),
                    topLeft = Offset(cp.screenX - cw * 0.48f, cp.screenY - 6f),
                    size = Size(cw * 0.96f, 22f),
                )
                drawOval(
                    color = cloudCol.copy(alpha = cloudAlpha),
                    topLeft = Offset(cp.screenX - cw * 0.5f, cp.screenY - 14f),
                    size = Size(cw, 24f),
                )
                if (lightingQ >= 2) {
                    drawOval(
                        color = cloudCol.copy(alpha = cloudAlpha * 0.85f),
                        topLeft = Offset(cp.screenX - cw * 0.24f, cp.screenY - 22f),
                        size = Size(cw * 0.52f, 20f),
                    )
                }
            }
        }
    }

    // Horizon atmospheric scattering ribbon
    if (lightingQ >= 2) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, skyHorizon.copy(alpha = 0.26f)),
                startY = max(0f, horizonY - 38f),
                endY = horizonY + 8f,
            ),
            topLeft = Offset(0f, max(0f, horizonY - 38f)),
            size = Size(w, 46f),
        )
    }

    // Far horizon ground blend tinted by biome
    val groundFill = when {
        hour < 5.5f || hour > 20.0f -> Color(0xFF0E1B12)
        isSemiArid -> Color(0xFF6B5A3B)
        isFloodplain -> Color(0xFF234D2A)
        else -> mapDef.grassColorRgb.toComposeColor()
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyHorizon.copy(alpha = 0.88f), groundFill),
            startY = horizonY,
            endY = h,
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(w, h - horizonY),
    )
}

internal fun soilCellColor(
    cell: TerrainCell,
    baseGrass: Color,
    soilColor: Color,
    mapDef: BrazilianMapDef,
): Color =
    when (cell.soilState) {
        SoilState.AGUA_RIO -> Color(0xFF1D708E)
        SoilState.PONTE -> Color(0xFF8C6B4A)
        SoilState.ESTRADA_ASFALTO -> Color(0xFF343A3E)
        SoilState.ESTRADA_TERRA -> lerpColor(Color(0xFF8B5D3B), soilColor, 0.35f)
        SoilState.FLORESTA_NATIVA, SoilState.AREA_PRESERVACAO ->
            if (!mapDef.hasRiver) Color(0xFF355C2B) else Color(0xFF1C5328)
        SoilState.PEDRAS_MATO ->
            if (!mapDef.hasRiver) Color(0xFF6A6648) else Color(0xFF535E49)
        SoilState.PASTAGEM -> when {
            cell.isIrrigated == true -> lerpColor(baseGrass, Color(0xFF3C8D40), 0.42f)
            !mapDef.hasRiver -> lerpColor(baseGrass, soilColor, 0.24f)
            else -> baseGrass
        }
        SoilState.ARADO -> if (cell.moisture > 0.72) shadeColor(soilColor, 0.84f) else soilColor
        SoilState.SUBSOLADO -> shadeColor(soilColor, 0.74f)
        SoilState.PALHADA -> Color(0xFFA78E54)
        SoilState.SEMEADO -> Color(
            (soilColor.red * 0.90f).coerceIn(0f, 1f),
            (soilColor.green * 0.96f).coerceIn(0f, 1f),
            soilColor.blue,
        )
        SoilState.CRESCENDO -> {
            val crop = cell.cropId?.let { getCropById(it) }
            val t = cell.growthProgress.toFloat().coerceIn(0f, 1f)
            val sc = crop?.seedColorRgb?.toComposeColor() ?: Color(0xFF34983A)
            val rc = crop?.ripeColorRgb?.toComposeColor() ?: Color(0xFF8FB839)
            lerpColor(sc, rc, t * 0.65f)
        }
        SoilState.PRONTO_COLHEITA -> {
            cell.cropId?.let { getCropById(it).ripeColorRgb.toComposeColor() } ?: Color(0xFFD5A938)
        }
    }

internal fun DrawScope.drawDetailedTerrain(
    worldState: WorldState,
    mapDef: BrazilianMapDef,
    gfx: GraphicsConfig,
    cameraMode: CameraMode,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    maxDrawM: Float,
    isNight: Boolean,
    twilightDarken: Float,
    rainIntensity: Float,
    animClockSec: Float,
    scratch: ViewportScratch3D,
) {
    val gridSide = worldState.mapGridSize ?: 128
    val cellSizeM = com.example.simulation.getCellSize(gridSide).toFloat()
    val worldHalfM = (gridSide * cellSizeM) / 2.0f
    val stepFar = if (gfx.terrainDetailStep >= 2) 3 else 2
    val baseGrass = mapDef.grassColorRgb.toComposeColor()
    val soilColor = mapDef.soilColorRgb.toComposeColor()
    val cells = worldState.cells
    val vegDensity = gfx.vegetationDensity.toFloat().coerceIn(0f, 1.0f)

    val camFwdLenXZ = hypot(pose.fwdX, pose.fwdZ).coerceAtLeast(0.001f)
    val camDirX = pose.fwdX / camFwdLenXZ
    val camDirZ = pose.fwdZ / camFwdLenXZ

    val camGx = floor(((pose.camX + worldHalfM) / cellSizeM).toDouble()).toInt().coerceIn(0, gridSide - 2)
    val camGz = floor(((pose.camZ + worldHalfM) / cellSizeM).toDouble()).toInt().coerceIn(0, gridSide - 2)

    val radiusCells = (maxDrawM / cellSizeM).toInt()
    val gxMin = max(0, camGx - radiusCells)
    val gxMax = min(gridSide - 2, camGx + radiusCells)
    val gzMin = max(0, camGz - radiusCells)
    val gzMax = min(gridSide - 2, camGz + radiusCells)

    val nearPad = if (cameraMode == CameraMode.CABINE) 9 else 7
    val nearGxMin = max(gxMin, floor(((pose.targetX + worldHalfM) / cellSizeM).toDouble()).toInt() - nearPad)
    val nearGxMax = min(gxMax, floor(((pose.targetX + worldHalfM) / cellSizeM).toDouble()).toInt() + nearPad)
    val nearGzMin = max(gzMin, floor(((pose.targetZ + worldHalfM) / cellSizeM).toDouble()).toInt() - nearPad)
    val nearGzMax = min(gzMax, floor(((pose.targetZ + worldHalfM) / cellSizeM).toDouble()).toInt() + nearPad)

    val fogCol = if (isNight) Color(0xFF101926) else Color(0xFFA9C4D4)

    fun drawTerrainRing(
        rGxMin: Int,
        rGxMax: Int,
        rGzMin: Int,
        rGzMax: Int,
        step: Int,
        skipInnerNear: Boolean,
    ) {
        val alignedGzMin = (rGzMin / step) * step
        val alignedGxMin = (rGxMin / step) * step
        val zSeq = if (camDirZ >= 0f) {
            (rGzMax downTo alignedGzMin step step)
        } else {
            (alignedGzMin..rGzMax step step)
        }

        for (gz in zSeq) {
            val gz1 = min(gridSide - 1, gz + step)
            val wz0 = -worldHalfM + gz * cellSizeM
            val wz1 = -worldHalfM + gz1 * cellSizeM

            val leftRange = (alignedGxMin..min(camGx, rGxMax) step step)
            val rightRange = (rGxMax downTo max(alignedGxMin, camGx + step) step step)

            for (pass in 0..1) {
                val xSeq = if (pass == 0) leftRange else rightRange
                for (gx in xSeq) {
                    if (skipInnerNear && gx in nearGxMin..nearGxMax && gz in nearGzMin..nearGzMax) continue
                    val gx1 = min(gridSide - 1, gx + step)
                    val wx0 = -worldHalfM + gx * cellSizeM
                    val wx1 = -worldHalfM + gx1 * cellSizeM
                    val centerX = (wx0 + wx1) * 0.5f
                    val centerZ = (wz0 + wz1) * 0.5f
                    val relX = centerX - pose.camX
                    val relZ = centerZ - pose.camZ
                    val distSq = relX * relX + relZ * relZ
                    if (distSq > maxDrawM * maxDrawM) continue
                    if (relX * camDirX + relZ * camDirZ < -step * cellSizeM * 2.2f) continue

                    val idx00 = gz * gridSide + gx
                    val idx10 = gz * gridSide + gx1
                    val idx01 = gz1 * gridSide + gx
                    val idx11 = gz1 * gridSide + gx1
                    if (idx00 < 0 || idx11 >= cells.size) continue

                    val c00 = cells[idx00]
                    val c10 = cells[idx10]
                    val c01 = cells[idx01]
                    val c11 = cells[idx11]
                    val h00 = c00.height.toFloat()
                    val h10 = c10.height.toFloat()
                    val h01 = c01.height.toFloat()
                    val h11 = c11.height.toFloat()

                    val dhdx = ((h10 - h00) + (h11 - h01)) * 0.5f / (step * cellSizeM)
                    val dhdz = ((h01 - h00) + (h11 - h10)) * 0.5f / (step * cellSizeM)
                    val slopeShade = (0.94f - dhdx * 0.36f + dhdz * 0.24f).coerceIn(0.65f, 1.18f) * twilightDarken

                    var baseCellCol = soilCellColor(c00, baseGrass, soilColor, mapDef)
                    if (gfx.textureResolutionScale >= 0.65) {
                        val grainNoise = (((gx * 73856093) xor (gz * 19349663)) and 15) / 15f
                        val grainShade = 0.94f + grainNoise * 0.12f
                        baseCellCol = shadeColor(baseCellCol, grainShade)
                    }
                    if (c00.soilState == SoilState.AGUA_RIO && gfx.waterQuality >= 1) {
                        val ripple = 0.5f + 0.5f * sin(animClockSec * 2.4f + gx * 0.7f + gz * 0.5f)
                        baseCellCol = lerpColor(baseCellCol, Color(0xFF3993B5), ripple * 0.24f)
                    } else if (c00.soilState == SoilState.ESTRADA_ASFALTO && rainIntensity > 0.18f && gfx.reflectionQuality >= 1) {
                        baseCellCol = lerpColor(baseCellCol, Color(0xFF566E7E), rainIntensity * 0.40f)
                    }
                    val distM = sqrt(distSq)
                    val fogBlend = ((distM - maxDrawM * 0.42f) / (maxDrawM * 0.58f)).coerceIn(0f, 0.72f)
                    val litColor = shadeAndFog(baseCellCol, slopeShade, fogBlend, fogCol)

                    drawProjectedQuad(
                        wx0, h00, wz0,
                        wx1, h10, wz0,
                        wx1, h11, wz1,
                        wx0, h01, wz1,
                        color = litColor,
                        pose = pose,
                        focalPx = focalPx,
                        viewW = w,
                        viewH = h,
                        scratch = scratch,
                    )

                    if (!skipInnerNear && distM < 58f) {
                        val midH = (h00 + h10 + h01 + h11) * 0.25f
                        when (c00.soilState) {
                            SoilState.AGUA_RIO -> {
                                if (gfx.reflectionQuality >= 1 && ((gx + gz) and 1) == 0) {
                                    val shim = 0.35f + 0.35f * sin(animClockSec * 2.8f + gx * 0.9f)
                                    val glintCol = Color(0xFFB3E5FC).copy(alpha = shim * 0.48f * twilightDarken)
                                    drawProjectedLine(
                                        wx0 + 0.5f, midH + 0.03f, wz0 + cellSizeM * 0.45f,
                                        wx1 - 0.5f, midH + 0.03f, wz0 + cellSizeM * 0.55f,
                                        glintCol, 2.0f, pose, focalPx, w, h,
                                    )
                                }
                            }
                            SoilState.ESTRADA_TERRA -> {
                                if (gfx.textureResolutionScale >= 0.70) {
                                    val rutCol = Color(0xFF3E2723).copy(alpha = 0.24f * twilightDarken)
                                    drawProjectedLine(
                                        wx0 + 0.3f, midH + 0.03f, wz0 + cellSizeM * 0.32f,
                                        wx1 - 0.3f, midH + 0.03f, wz0 + cellSizeM * 0.32f,
                                        rutCol, 2.0f, pose, focalPx, w, h,
                                    )
                                    drawProjectedLine(
                                        wx0 + 0.3f, midH + 0.03f, wz0 + cellSizeM * 0.68f,
                                        wx1 - 0.3f, midH + 0.03f, wz0 + cellSizeM * 0.68f,
                                        rutCol, 2.0f, pose, focalPx, w, h,
                                    )
                                }
                            }
                            SoilState.ESTRADA_ASFALTO -> {
                                // Crisp road shoulder & dashed yellow center stripe
                                val hasRoadZ = (gz > 0 && cells[(gz - 1) * gridSide + gx].soilState == SoilState.ESTRADA_ASFALTO) ||
                                    (gz + 1 < gridSide && cells[(gz + 1) * gridSide + gx].soilState == SoilState.ESTRADA_ASFALTO)
                                val hasRoadX = (gx > 0 && cells[gz * gridSide + (gx - 1)].soilState == SoilState.ESTRADA_ASFALTO) ||
                                    (gx + 1 < gridSide && cells[gz * gridSide + (gx + 1)].soilState == SoilState.ESTRADA_ASFALTO)
                                if (hasRoadZ && !hasRoadX && (gz % 2 == 0)) {
                                    drawProjectedLine(
                                        wx0 + cellSizeM * 0.5f, midH + 0.03f, wz0 + 0.6f,
                                        wx0 + cellSizeM * 0.5f, midH + 0.03f, wz1 - 0.6f,
                                        Color(0xFFFFD54F).copy(alpha = 0.78f * twilightDarken),
                                        2.5f, pose, focalPx, w, h,
                                    )
                                } else if (hasRoadX && (gx % 2 == 0)) {
                                    drawProjectedLine(
                                        wx0 + 0.6f, midH + 0.03f, wz0 + cellSizeM * 0.5f,
                                        wx1 - 0.6f, midH + 0.03f, wz0 + cellSizeM * 0.5f,
                                        Color(0xFFFFD54F).copy(alpha = 0.78f * twilightDarken),
                                        2.5f, pose, focalPx, w, h,
                                    )
                                }
                                if (gfx.textureResolutionScale >= 0.80) {
                                    val edgeCol = Color(0xFFE0E6E2).copy(alpha = 0.35f * twilightDarken)
                                    if (hasRoadZ && !hasRoadX) {
                                        drawProjectedLine(wx0 + 0.35f, midH + 0.03f, wz0, wx0 + 0.35f, midH + 0.03f, wz1, edgeCol, 1.5f, pose, focalPx, w, h)
                                        drawProjectedLine(wx1 - 0.35f, midH + 0.03f, wz0, wx1 - 0.35f, midH + 0.03f, wz1, edgeCol, 1.5f, pose, focalPx, w, h)
                                    } else if (hasRoadX) {
                                        drawProjectedLine(wx0, midH + 0.03f, wz0 + 0.35f, wx1, midH + 0.03f, wz0 + 0.35f, edgeCol, 1.5f, pose, focalPx, w, h)
                                        drawProjectedLine(wx0, midH + 0.03f, wz1 - 0.35f, wx1, midH + 0.03f, wz1 - 0.35f, edgeCol, 1.5f, pose, focalPx, w, h)
                                    }
                                }
                            }
                            SoilState.ARADO, SoilState.SUBSOLADO, SoilState.SEMEADO -> {
                                val ridgeCol = if (c00.soilState == SoilState.SUBSOLADO) {
                                    Color.Black.copy(alpha = 0.30f)
                                } else {
                                    Color.Black.copy(alpha = 0.19f)
                                }
                                drawProjectedLine(
                                    wx0 + 0.4f, midH + 0.04f, wz0 + cellSizeM * 0.25f,
                                    wx1 - 0.4f, midH + 0.04f, wz0 + cellSizeM * 0.25f,
                                    ridgeCol, 1.9f, pose, focalPx, w, h,
                                )
                                drawProjectedLine(
                                    wx0 + 0.4f, midH + 0.04f, wz0 + cellSizeM * 0.55f,
                                    wx1 - 0.4f, midH + 0.04f, wz0 + cellSizeM * 0.55f,
                                    ridgeCol, 1.8f, pose, focalPx, w, h,
                                )
                                if (gfx.textureResolutionScale >= 0.80) {
                                    drawProjectedLine(
                                        wx0 + 0.4f, midH + 0.04f, wz0 + cellSizeM * 0.82f,
                                        wx1 - 0.4f, midH + 0.04f, wz0 + cellSizeM * 0.82f,
                                        ridgeCol, 1.7f, pose, focalPx, w, h,
                                    )
                                }
                            }
                            SoilState.CRESCENDO, SoilState.PRONTO_COLHEITA -> {
                                if (vegDensity >= 0.30f && distM < 44f && c00.cropId != null) {
                                    drawCellCrops3D(
                                        cell = c00,
                                        wx0 = wx0,
                                        wz0 = wz0,
                                        cellSizeM = cellSizeM,
                                        midY = midH,
                                        baseCol = baseCellCol,
                                        twilight = twilightDarken,
                                        vegDensity = vegDensity,
                                        animClockSec = animClockSec,
                                        pose = pose,
                                        focalPx = focalPx,
                                        w = w,
                                        h = h,
                                    )
                                }
                            }
                            SoilState.PASTAGEM -> {
                                if (vegDensity >= 0.45f && distM < 30f && ((gx + gz) and 1) == 0) {
                                    val sway = sin(animClockSec * 1.9f + wx0 * 0.5f) * 0.08f
                                    val tuftCol = shadeColor(baseCellCol, 1.16f * twilightDarken)
                                    val bx = wx0 + cellSizeM * 0.45f
                                    val bz = wz0 + cellSizeM * 0.52f
                                    drawProjectedLine(bx, midH, bz, bx - 0.18f + sway, midH + 0.36f, bz, tuftCol, 2.0f, pose, focalPx, w, h)
                                    drawProjectedLine(bx, midH, bz, bx + 0.04f + sway, midH + 0.44f, bz, tuftCol, 2.2f, pose, focalPx, w, h)
                                    drawProjectedLine(bx, midH, bz, bx + 0.22f + sway, midH + 0.32f, bz, tuftCol, 1.8f, pose, focalPx, w, h)
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    drawTerrainRing(gxMin, gxMax, gzMin, gzMax, stepFar, skipInnerNear = true)
    drawTerrainRing(nearGxMin, nearGxMax, nearGzMin, nearGzMax, step = 1, skipInnerNear = false)
}

private fun DrawScope.drawCellCrops3D(
    cell: TerrainCell,
    wx0: Float,
    wz0: Float,
    cellSizeM: Float,
    midY: Float,
    baseCol: Color,
    twilight: Float,
    vegDensity: Float,
    animClockSec: Float,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
) {
    val cropId = cell.cropId ?: return
    val crop = getCropById(cropId)
    val ripe = cell.soilState == SoilState.PRONTO_COLHEITA
    val prog = if (ripe) 1.0f else cell.growthProgress.toFloat().coerceIn(0.18f, 1.0f)
    val growH = (crop.maxHeightMeters.toFloat().coerceIn(0.28f, 2.5f)) * prog
    val stalkCol = shadeColor(
        if (ripe) crop.ripeColorRgb.toComposeColor() else lerpColor(crop.seedColorRgb.toComposeColor(), baseCol, 0.35f),
        twilight,
    )
    val count = if (vegDensity > 0.72f) 4 else 2
    val sway = sin(animClockSec * 2.1f + wx0 * 0.35f + wz0 * 0.25f) * 0.09f * prog

    for (i in 0 until count) {
        val fracX = 0.22f + (i % 2) * 0.52f
        val fracZ = 0.24f + (i / 2) * 0.50f
        val sx = wx0 + cellSizeM * fracX
        val sz = wz0 + cellSizeM * fracZ
        val topX = sx + sway
        val topY = midY + growH

        val pBot = projectRaw(sx, midY, sz, pose, focalPx, w, h)
        val pTop = projectRaw(topX, topY, sz, pose, focalPx, w, h)
        if (!pBot.visible || !pTop.visible) continue

        when {
            cropId == "milho" || cropId == "sorgo" || cropId == "cana" -> {
                drawLine(stalkCol, Offset(pBot.screenX, pBot.screenY), Offset(pTop.screenX, pTop.screenY), strokeWidth = 3.0f, cap = StrokeCap.Round)
                // Side leaf blades
                val midX = (pBot.screenX + pTop.screenX) * 0.5f
                val midScY = (pBot.screenY + pTop.screenY) * 0.52f
                drawLine(stalkCol, Offset(midX, midScY), Offset(midX - 7f, midScY - 5f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                drawLine(stalkCol, Offset(midX, midScY), Offset(midX + 7f, midScY - 4f), strokeWidth = 2.0f, cap = StrokeCap.Round)
                if (ripe && cropId == "milho") {
                    drawCircle(Color(0xFFFFCA28).copy(alpha = 0.9f), radius = 3.0f, center = Offset(pTop.screenX, pTop.screenY))
                }
            }
            cropId == "algodao" -> {
                drawLine(Color(0xFF4E342E).copy(alpha = 0.85f), Offset(pBot.screenX, pBot.screenY), Offset(pTop.screenX, pTop.screenY), strokeWidth = 2.6f)
                if (ripe || prog > 0.78f) {
                    drawCircle(Color(0xFFFAFAFA), radius = 4.2f, center = Offset(pTop.screenX, pTop.screenY))
                    drawCircle(Color(0xFFF5F5F5), radius = 3.2f, center = Offset(pTop.screenX - 4f, pTop.screenY + 4f))
                } else {
                    drawCircle(stalkCol, radius = 4.0f, center = Offset(pTop.screenX, pTop.screenY))
                }
            }
            cropId == "girassol" -> {
                drawLine(Color(0xFF33691E), Offset(pBot.screenX, pBot.screenY), Offset(pTop.screenX, pTop.screenY), strokeWidth = 2.8f)
                if (ripe || prog > 0.65f) {
                    drawCircle(Color(0xFFFDD835), radius = 5.0f, center = Offset(pTop.screenX, pTop.screenY))
                    drawCircle(Color(0xFF5D4037), radius = 2.2f, center = Offset(pTop.screenX, pTop.screenY))
                }
            }
            cropId == "cafe" -> {
                drawLine(Color(0xFF3E2723), Offset(pBot.screenX, pBot.screenY), Offset(pTop.screenX, pTop.screenY), strokeWidth = 3.0f)
                drawCircle(Color(0xFF1B5E20), radius = 6.5f * prog, center = Offset(pTop.screenX, pTop.screenY))
                if (ripe) {
                    drawCircle(Color(0xFFD32F2F), radius = 2.0f, center = Offset(pTop.screenX - 2f, pTop.screenY - 1f))
                    drawCircle(Color(0xFFD32F2F), radius = 2.0f, center = Offset(pTop.screenX + 3f, pTop.screenY + 1f))
                }
            }
            cropId in setOf("trigo", "arroz", "aveia", "cevada", "centeio", "triticale", "milheto") -> {
                drawLine(stalkCol, Offset(pBot.screenX, pBot.screenY), Offset(pTop.screenX, pTop.screenY), strokeWidth = 2.0f, cap = StrokeCap.Round)
                drawLine(stalkCol, Offset(pBot.screenX - 4f, pBot.screenY), Offset(pTop.screenX - 3f, pTop.screenY + 2f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                if (ripe) {
                    drawCircle(Color(0xFFF4D03F), radius = 2.6f, center = Offset(pTop.screenX, pTop.screenY))
                }
            }
            else -> {
                // Soybeans, beans & leafy crops
                drawLine(stalkCol, Offset(pBot.screenX, pBot.screenY), Offset(pTop.screenX, pTop.screenY), strokeWidth = 2.5f, cap = StrokeCap.Round)
                drawCircle(stalkCol, radius = (3.4f * prog).coerceAtLeast(2.0f), center = Offset(pTop.screenX, pTop.screenY))
            }
        }
    }
}

internal fun DrawScope.drawTree3D(
    item: RenderItem3D,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    light: Float,
    animClockSec: Float,
    detailLevel: Int,
    scratch: ViewportScratch3D,
) {
    val sizeVar = ((item.prop?.uid?.hashCode() ?: 0) and 3) * 0.12f
    val scale = (0.95f + sizeVar).coerceIn(0.85f, 1.35f)
    val tx = item.x.toFloat()
    val ty = item.y.toFloat()
    val tz = item.z.toFloat()
    val pType = item.prop?.type
    val segs = if (detailLevel >= 1) 12 else 8
    val sway = sin(animClockSec * 1.4f + tx * 0.2f) * 0.08f

    drawRotatedBox3D(tx + 0.65f, ty + 0.03f, tz + 0.65f, 1.35f * scale, 0.01f, 1.35f * scale, 0f, 1f, 1f, 0f, Color.Black.copy(alpha = 0.28f), 1f, pose, focalPx, w, h, scratch)

    when (pType) {
        WorldPropType.TREE_ARAUCARIA -> {
            val trunkH = 4.1f * scale
            drawCylinderY3D(tx, ty, ty + trunkH, tz, 0.24f * scale, Color(0xFF533A2E), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + sway, ty + trunkH + 0.20f * scale, tz, 2.30f * scale, 0.62f * scale, Color(0xFF184A2C), light, segs, pose, focalPx, w, h, scratch, rings = 2)
            drawEllipsoidDome3D(tx + sway, ty + trunkH + 0.72f * scale, tz, 1.45f * scale, 0.45f * scale, Color(0xFF21623B), light, segs, pose, focalPx, w, h, scratch, rings = 2)
        }
        WorldPropType.TREE_CERRADO -> {
            val trunkH = 1.85f * scale
            drawCylinderY3D(tx, ty, ty + trunkH, tz, 0.25f * scale, Color(0xFF6B4E35), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + sway, ty + trunkH + 0.75f * scale, tz, 1.82f * scale, 0.95f * scale, Color(0xFF4B702D), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + 0.35f * scale + sway, ty + trunkH + 1.25f * scale, tz, 1.25f * scale, 0.72f * scale, Color(0xFF60883B), light, segs, pose, focalPx, w, h, scratch, rings = 2)
        }
        WorldPropType.TREE_PALM_AMAZON -> {
            val trunkH = 4.3f * scale
            drawCylinderY3D(tx, ty, ty + trunkH, tz, 0.17f * scale, Color(0xFF6D5544), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + sway, ty + trunkH + 0.32f * scale, tz, 1.95f * scale, 0.55f * scale, Color(0xFF277B3B), light, segs, pose, focalPx, w, h, scratch, rings = 2)
            drawEllipsoidDome3D(tx + sway, ty + trunkH + 0.52f * scale, tz, 1.15f * scale, 0.38f * scale, Color(0xFF2E8C45), light, segs, pose, focalPx, w, h, scratch, rings = 2)
        }
        WorldPropType.TREE_PINE -> {
            val trunkH = 2.1f * scale
            drawCylinderY3D(tx, ty, ty + trunkH, tz, 0.22f * scale, Color(0xFF5D4037), light, segs, pose, focalPx, w, h, scratch)
            drawConeY3D(tx + sway, ty + trunkH * 0.7f, ty + trunkH + 2.6f * scale, tz, 1.55f * scale, Color(0xFF194B2B), light, segs, pose, focalPx, w, h, scratch)
            drawConeY3D(tx + sway, ty + trunkH + 1.4f * scale, ty + trunkH + 3.8f * scale, tz, 1.05f * scale, Color(0xFF226139), light, segs, pose, focalPx, w, h, scratch)
        }
        WorldPropType.TREE_IPE_AMARELO -> {
            val trunkH = 2.6f * scale
            drawCylinderY3D(tx, ty, ty + trunkH, tz, 0.24f * scale, Color(0xFF5D4037), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + sway, ty + trunkH + 0.95f * scale, tz, 1.85f * scale, 1.15f * scale, Color(0xFFE5B526), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + 0.35f * scale + sway, ty + trunkH + 1.45f * scale, tz - 0.25f * scale, 1.32f * scale, 0.78f * scale, Color(0xFFF6CE46), light, segs, pose, focalPx, w, h, scratch, rings = 2)
        }
        WorldPropType.TREE_IPE_ROXO -> {
            val trunkH = 2.5f * scale
            drawCylinderY3D(tx, ty, ty + trunkH, tz, 0.23f * scale, Color(0xFF5A3F32), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + sway, ty + trunkH + 0.92f * scale, tz, 1.78f * scale, 1.12f * scale, Color(0xFFA3348E), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx - 0.32f * scale + sway, ty + trunkH + 1.42f * scale, tz + 0.20f * scale, 1.24f * scale, 0.74f * scale, Color(0xFFC854B4), light, segs, pose, focalPx, w, h, scratch, rings = 2)
        }
        WorldPropType.TREE_SHADE_MANGUEIRA -> {
            val trunkH = 2.4f * scale
            drawCylinderY3D(tx, ty, ty + trunkH, tz, 0.34f * scale, Color(0xFF4D3428), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + sway, ty + trunkH + 1.05f * scale, tz, 2.55f * scale, 1.35f * scale, Color(0xFF1F5A26), light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + 0.65f * scale + sway, ty + trunkH + 1.45f * scale, tz + 0.35f * scale, 1.70f * scale, 0.95f * scale, Color(0xFF2D7434), light, segs, pose, focalPx, w, h, scratch, rings = 2)
        }
        else -> {
            val trunkH = if (pType == WorldPropType.TREE_EUCALYPTUS) 3.5f * scale else 2.2f * scale
            drawCylinderY3D(tx, ty, ty + trunkH, tz, 0.24f * scale, Color(0xFF5D4037), light, segs, pose, focalPx, w, h, scratch)
            val leafCol = when (pType) {
                WorldPropType.TREE_EUCALYPTUS -> Color(0xFF336947)
                WorldPropType.TREE_FRUIT -> Color(0xFF2D7A32)
                else -> Color(0xFF1E622A)
            }
            val domeRY = if (pType == WorldPropType.TREE_EUCALYPTUS) 1.85f * scale else 1.42f * scale
            drawEllipsoidDome3D(tx + sway, ty + trunkH + 0.95f * scale, tz, 1.58f * scale, domeRY, leafCol, light, segs, pose, focalPx, w, h, scratch)
            drawEllipsoidDome3D(tx + sway - 0.35f * scale, ty + trunkH + 1.45f * scale, tz + 0.25f * scale, 1.05f * scale, 0.88f * scale, leafCol.copy(green = min(1f, leafCol.green * 1.12f)), light, segs, pose, focalPx, w, h, scratch, rings = 2)
        }
    }
}

internal fun DrawScope.drawProp3D(
    item: RenderItem3D,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    light: Float,
    scratch: ViewportScratch3D,
) {
    val pr = item.prop ?: return
    val px = item.x.toFloat()
    val py = item.y.toFloat()
    val pz = item.z.toFloat()
    when (pr.type) {
        WorldPropType.CACTUS_MANDACARU -> {
            val col = Color(0xFF3E7C3A)
            drawRotatedBox3D(px, py + 0.02f, pz, 0.48f, 0.01f, 0.48f, 0f, 1f, 1f, 0f, Color.Black.copy(alpha = 0.25f), 1f, pose, focalPx, w, h, scratch)
            drawCylinderY3D(px, py, py + 1.70f, pz, 0.17f, col, light, 10, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(px - 0.34f, py + 0.92f, pz, 0.22f, 0.11f, 0.12f, 0f, 1f, 1f, 0f, col, light, pose, focalPx, w, h, scratch)
            drawCylinderY3D(px - 0.48f, py + 0.92f, py + 1.60f, pz, 0.12f, col, light, 8, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(px + 0.34f, py + 0.78f, pz, 0.22f, 0.11f, 0.12f, 0f, 1f, 1f, 0f, col, light, pose, focalPx, w, h, scratch)
            drawCylinderY3D(px + 0.48f, py + 0.78f, py + 1.50f, pz, 0.12f, col, light, 8, pose, focalPx, w, h, scratch)
        }
        WorldPropType.BANANA_CLUMP -> {
            val stemCol = Color(0xFF4A6B32)
            val leafCol = Color(0xFF3C8838)
            for (dx in listOf(-0.26f, 0.24f, 0f)) {
                val dz = if (dx == 0f) 0.25f else -0.15f
                drawCylinderY3D(px + dx, py, py + 1.85f, pz + dz, 0.12f, stemCol, light, 8, pose, focalPx, w, h, scratch)
                drawEllipsoidDome3D(px + dx, py + 2.05f, pz + dz, 0.95f, 0.35f, leafCol, light, 8, pose, focalPx, w, h, scratch, rings = 2)
            }
        }
        WorldPropType.FLOWER_BED_AZALEA -> {
            drawEllipsoidDome3D(px, py + 0.38f, pz, 0.85f, 0.38f, Color(0xFF28692F), light, 10, pose, focalPx, w, h, scratch, rings = 2)
            drawEllipsoidDome3D(px - 0.25f, py + 0.56f, pz, 0.45f, 0.24f, Color(0xFFD61F78), light, 8, pose, focalPx, w, h, scratch, rings = 2)
            drawEllipsoidDome3D(px + 0.28f, py + 0.52f, pz + 0.12f, 0.42f, 0.22f, Color(0xFFE84A96), light, 8, pose, focalPx, w, h, scratch, rings = 2)
        }
        WorldPropType.ORNAMENTAL_HEDGE -> {
            drawRotatedBox3D(px, py + 0.42f, pz, 1.35f, 0.42f, 0.42f, 0f, 1f, 1f, 0f, Color(0xFF2B6830), light, pose, focalPx, w, h, scratch)
        }
        WorldPropType.COFFEE_SHRUB -> {
            drawEllipsoidDome3D(px, py + 0.55f, pz, 0.62f, 0.52f, Color(0xFF1D5628), light, 10, pose, focalPx, w, h, scratch, rings = 2)
            drawRotatedBox3D(px + 0.20f, py + 0.72f, pz + 0.12f, 0.09f, 0.07f, 0.09f, 0f, 1f, 1f, 0f, Color(0xFFC62828), light, pose, focalPx, w, h, scratch)
        }
        WorldPropType.ROSE_CLUSTER -> {
            drawEllipsoidDome3D(px, py + 0.42f, pz, 0.54f, 0.38f, Color(0xFF28662D), light, 10, pose, focalPx, w, h, scratch, rings = 2)
            drawEllipsoidDome3D(px, py + 0.68f, pz, 0.26f, 0.16f, Color(0xFFE11D48), light, 8, pose, focalPx, w, h, scratch, rings = 2)
        }
        WorldPropType.BUSH, WorldPropType.PLANT_CLUSTER -> {
            drawEllipsoidDome3D(px, py + 0.35f, pz, 0.62f, 0.36f, Color(0xFF2D6A32), light, 10, pose, focalPx, w, h, scratch, rings = 2)
        }
        WorldPropType.BALE_ROUND -> {
            drawCylinderWheel3D(px, py + 0.58f, pz, 0.58f, 0.55f, 0f, 0f, 1f, 1f, 0f, Color(0xFFD7AD46), Color(0xFFC59932), light, 14, pose, focalPx, w, h, scratch)
        }
        WorldPropType.BALE_RECT, WorldPropType.SILAGE_BAG -> {
            val col = if (pr.type == WorldPropType.SILAGE_BAG) Color(0xFFE8ECEF) else Color(0xFFD7AD46)
            drawRotatedBox3D(px, py + 0.45f, pz, 0.55f, 0.45f, 0.92f, 0f, 1f, 1f, 0f, col, light, pose, focalPx, w, h, scratch)
        }
        WorldPropType.UTILITY_POLE -> {
            drawCylinderY3D(px, py, py + 5.8f, pz, 0.14f, Color(0xFF5D4037), light, 10, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(px, py + 5.45f, pz, 0.95f, 0.09f, 0.09f, 0f, 1f, 1f, 0f, Color(0xFF4E342E), light, pose, focalPx, w, h, scratch)
        }
        WorldPropType.LOG -> {
            drawCylinderWheel3D(px, py + 0.24f, pz, 0.24f, 1.32f, 0f, 1f, 0f, 0f, 1f, Color(0xFF6D4C41), Color(0xFFA1785B), light, 10, pose, focalPx, w, h, scratch)
        }
        else -> {
            drawEllipsoidDome3D(px, py + 0.26f, pz, 0.52f, 0.34f, Color(0xFF7B8580), light, 10, pose, focalPx, w, h, scratch, rings = 2)
        }
    }
}

internal fun DrawScope.drawCharacter3D(
    item: RenderItem3D,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    light: Float,
    animClockSec: Float,
    scratch: ViewportScratch3D,
) {
    val px = item.x.toFloat()
    val py = item.y.toFloat()
    val pz = item.z.toFloat()
    val rad = item.headingDeg.toFloat() * (PI / 180.0).toFloat()
    val fx = sin(rad)
    val fz = cos(rad)
    val rx = cos(rad)
    val rz = -sin(rad)

    val app = item.playerAppearance
    val isStyledPlayer = item.kind == ItemKind3D.PLAYER
    val cfg = app ?: DEFAULT_PLAYER_APPEARANCE

    val shirtCol = if (isStyledPlayer) parseSafeColor(cfg.shirtColorHex, Color(0xFF1E2124)) else item.labelColor
    val pantsCol = if (isStyledPlayer) parseSafeColor(cfg.pantsColorHex, Color(0xFF363D2A)) else Color(0xFF263E57)
    val bootCol = if (isStyledPlayer) parseSafeColor(cfg.bootColorHex, Color(0xFF5C4332)) else Color(0xFF3E2723)
    val skinCol = if (isStyledPlayer) parseSafeColor(cfg.skinColorHex, Color(0xFFE4B898)) else item.altColor
    val hairCol = if (isStyledPlayer) parseSafeColor(cfg.hairColorHex, Color(0xFF5A2A22)) else Color(0xFF2B2118)
    val collarCol = parseSafeColor(cfg.collarHex, Color(0xFFDDD5C5))
    val glovesCol = cfg.glovesHex?.let { parseSafeColor(it, Color(0xFFE8E8E4)) }
    val outfitStyle = cfg.outfitStyle ?: "jacket_shearling"
    val hairStyle = cfg.hairStyle ?: "bob"
    val glassesStyle = cfg.glassesStyle ?: "sunglasses"
    val beardStyle = cfg.beardStyle ?: "none"

    // Ground shadow
    drawRotatedBox3D(px, py + 0.02f, pz, 0.36f, 0.01f, 0.36f, fx, fz, rx, rz, Color.Black.copy(alpha = 0.32f), 1f, pose, focalPx, w, h, scratch)

    val legSwing = sin(animClockSec * 6f) * 0.07f
    val armSwing = -legSwing * 0.8f

    // Boots (tall knee-high black riding boots for preset_3 flannel_plaid; ankle shoes otherwise)
    val isTallBoot = isStyledPlayer && outfitStyle == "flannel_plaid"
    val bootH = if (isTallBoot) 0.24f else 0.09f
    drawRotatedBox3D(px - rx * 0.11f + fx * legSwing, py + bootH, pz - rz * 0.11f + fz * legSwing, 0.085f, bootH, 0.12f, fx, fz, rx, rz, bootCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(px + rx * 0.11f - fx * legSwing, py + bootH, pz + rz * 0.11f - fz * legSwing, 0.085f, bootH, 0.12f, fx, fz, rx, rz, bootCol, light, pose, focalPx, w, h, scratch)

    // Upper Legs / Pants
    val legBot = bootH * 2f
    val legH = (0.86f - legBot) * 0.5f
    val legMidY = legBot + legH
    drawRotatedBox3D(px - rx * 0.105f + fx * legSwing, py + legMidY, pz - rz * 0.105f + fz * legSwing, 0.092f, legH, 0.105f, fx, fz, rx, rz, pantsCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(px + rx * 0.105f - fx * legSwing, py + legMidY, pz + rz * 0.105f - fz * legSwing, 0.092f, legH, 0.105f, fx, fz, rx, rz, pantsCol, light, pose, focalPx, w, h, scratch)

    // Torso / Jacket / Overcoat / Hoodie
    when {
        isStyledPlayer && outfitStyle == "overcoat" -> {
            // Long knee-length light-gray coat + dark lapel
            drawTiltedBox3D(px, py + 1.00f, pz, 0.25f, 0.36f, 0.16f, fx, fz, rx, rz, 0f, 0f, shirtCol, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.92f)
            drawRotatedBox3D(px + fx * 0.04f, py + 1.33f, pz + fz * 0.04f, 0.17f, 0.06f, 0.15f, fx, fz, rx, rz, collarCol, light, pose, focalPx, w, h, scratch)
        }
        isStyledPlayer && outfitStyle == "flannel_plaid" -> {
            // Red/brown plaid flannel jacket + tan shearling collar + inner placket
            drawTiltedBox3D(px, py + 1.12f, pz, 0.23f, 0.27f, 0.15f, fx, fz, rx, rz, 0f, 0f, shirtCol, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.94f)
            drawRotatedBox3D(px + fx * 0.152f, py + 1.12f, pz + fz * 0.152f, 0.06f, 0.23f, 0.015f, fx, fz, rx, rz, Color(0xFF4E2A22), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(px + fx * 0.03f, py + 1.37f, pz + fz * 0.03f, 0.19f, 0.055f, 0.15f, fx, fz, rx, rz, collarCol, light, pose, focalPx, w, h, scratch)
        }
        isStyledPlayer && outfitStyle == "hoodie" -> {
            // Dusty rose hoodie + hood collar + pocket
            drawTiltedBox3D(px, py + 1.12f, pz, 0.22f, 0.27f, 0.15f, fx, fz, rx, rz, 0f, 0f, shirtCol, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.92f)
            drawRotatedBox3D(px - fx * 0.04f, py + 1.37f, pz - fz * 0.04f, 0.18f, 0.06f, 0.15f, fx, fz, rx, rz, collarCol, light, pose, focalPx, w, h, scratch)
        }
        else -> {
            // Jacket shearling (preset_1 Clara): black jacket + white shirt center front + cream fleece collar
            drawTiltedBox3D(px, py + 1.12f, pz, 0.22f, 0.27f, 0.15f, fx, fz, rx, rz, 0f, 0f, shirtCol, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.94f)
            if (isStyledPlayer) {
                drawRotatedBox3D(px + fx * 0.152f, py + 1.11f, pz + fz * 0.152f, 0.08f, 0.24f, 0.015f, fx, fz, rx, rz, Color(0xFFEBEAE4), light, pose, focalPx, w, h, scratch)
                drawRotatedBox3D(px + fx * 0.03f, py + 1.37f, pz + fz * 0.03f, 0.19f, 0.055f, 0.15f, fx, fz, rx, rz, collarCol, light, pose, focalPx, w, h, scratch)
            }
        }
    }

    // Left & Right Arms + Hands (or white gloves for Mariana)
    val handCol = glovesCol ?: skinCol
    drawRotatedBox3D(px - rx * 0.28f - fx * armSwing, py + 1.12f, pz - rz * 0.28f - fz * armSwing, 0.065f, 0.24f, 0.065f, fx, fz, rx, rz, shirtCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(px + rx * 0.28f + fx * armSwing, py + 1.12f, pz + rz * 0.28f + fz * armSwing, 0.065f, 0.24f, 0.065f, fx, fz, rx, rz, shirtCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(px - rx * 0.28f - fx * armSwing, py + 0.85f, pz - rz * 0.28f - fz * armSwing, 0.055f, 0.06f, 0.055f, fx, fz, rx, rz, handCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(px + rx * 0.28f + fx * armSwing, py + 0.85f, pz + rz * 0.28f + fz * armSwing, 0.055f, 0.06f, 0.055f, fx, fz, rx, rz, handCol, light, pose, focalPx, w, h, scratch)

    // Rounded Head
    drawEllipsoidDome3D(px, py + 1.54f, pz, 0.135f, 0.145f, skinCol, light, 10, pose, focalPx, w, h, scratch, rings = 2)

    // Beard (Sr. Sebastiao)
    if (isStyledPlayer && beardStyle == "full_gray") {
        drawRotatedBox3D(px + fx * 0.08f, py + 1.46f, pz + fz * 0.08f, 0.105f, 0.055f, 0.07f, fx, fz, rx, rz, hairCol, light, pose, focalPx, w, h, scratch)
    }

    // Glasses (Clara sunglasses / Sr. Sebastiao reading glasses)
    if (isStyledPlayer && glassesStyle == "sunglasses") {
        drawRotatedBox3D(px + fx * 0.135f - rx * 0.045f, py + 1.56f, pz + fz * 0.135f - rz * 0.045f, 0.036f, 0.025f, 0.015f, fx, fz, rx, rz, Color(0xFF1A1412), light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(px + fx * 0.135f + rx * 0.045f, py + 1.56f, pz + fz * 0.135f + rz * 0.045f, 0.036f, 0.025f, 0.015f, fx, fz, rx, rz, Color(0xFF1A1412), light, pose, focalPx, w, h, scratch)
    } else if (isStyledPlayer && glassesStyle == "reading") {
        drawRotatedBox3D(px + fx * 0.135f, py + 1.56f, pz + fz * 0.135f, 0.095f, 0.018f, 0.014f, fx, fz, rx, rz, Color(0xFFCFD8DC), light, pose, focalPx, w, h, scratch)
    }

    // Hair / Optional Hat
    val hatStyle = if (isStyledPlayer) cfg.hatStyle else "bone"
    when {
        hatStyle != "sem" -> {
            val hatCol = if (isStyledPlayer) parseSafeColor(cfg.hatColorHex, Color(0xFFD7B46A)) else Color(0xFF2E7D32)
            drawEllipsoidDome3D(px, py + 1.66f, pz, 0.25f, 0.055f, hatCol, light, 10, pose, focalPx, w, h, scratch, rings = 2)
            drawEllipsoidDome3D(px, py + 1.72f, pz, 0.14f, 0.08f, hatCol, light, 10, pose, focalPx, w, h, scratch, rings = 2)
        }
        hairStyle == "bob" -> {
            drawEllipsoidDome3D(px, py + 1.63f, pz, 0.155f, 0.08f, hairCol, light, 10, pose, focalPx, w, h, scratch, rings = 2)
            drawRotatedBox3D(px - rx * 0.13f, py + 1.52f, pz - rz * 0.13f, 0.035f, 0.13f, 0.12f, fx, fz, rx, rz, hairCol, light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(px + rx * 0.13f, py + 1.52f, pz + rz * 0.13f, 0.035f, 0.13f, 0.12f, fx, fz, rx, rz, hairCol, light, pose, focalPx, w, h, scratch)
        }
        hairStyle == "shoulder_bangs" -> {
            drawEllipsoidDome3D(px, py + 1.63f, pz, 0.155f, 0.085f, hairCol, light, 10, pose, focalPx, w, h, scratch, rings = 2)
            drawRotatedBox3D(px + fx * 0.09f, py + 1.61f, pz + fz * 0.09f, 0.11f, 0.035f, 0.04f, fx, fz, rx, rz, hairCol, light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(px - rx * 0.135f, py + 1.49f, pz - rz * 0.135f, 0.035f, 0.16f, 0.13f, fx, fz, rx, rz, hairCol, light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(px + rx * 0.135f, py + 1.49f, pz + rz * 0.135f, 0.035f, 0.16f, 0.13f, fx, fz, rx, rz, hairCol, light, pose, focalPx, w, h, scratch)
        }
        else -> {
            // Clean short crop (Sr. Sebastiao / Lucas)
            drawEllipsoidDome3D(px, py + 1.64f, pz, 0.145f, 0.065f, hairCol, light, 10, pose, focalPx, w, h, scratch, rings = 2)
        }
    }
}
