package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.domain.BUILDING_META
import com.example.domain.BuildingType
import com.example.domain.BuildingVisualProfile
import com.example.domain.PlacedBuilding
import com.example.domain.RegionalArchStyle
import com.example.domain.WorldState
import com.example.domain.resolveBuildingVisualProfile
import com.example.simulation.ConstructionPlacementPreview
import com.example.simulation.getTerrainHeightAt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal fun DrawScope.drawDetailedBuilding3D(
    item: RenderItem3D,
    worldState: WorldState,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    light: Float,
    isNight: Boolean,
    detailLevel: Int,
    scratch: ViewportScratch3D,
) {
    val type = item.building?.type ?: item.buildingType ?: return
    val building = item.building ?: PlacedBuilding(
        uid = "tmp_bld",
        type = type,
        x = item.x,
        z = item.z,
        rotationDeg = item.buildingRotDeg,
        isGateOpen = item.buildingGateOpen,
    )
    val profile = resolveBuildingVisualProfile(building, worldState.mapId)
    val bw = profile.widthM.toFloat().coerceIn(0.35f, 16f) * 0.5f
    val bd = profile.depthM.toFloat().coerceIn(0.35f, 14f) * 0.5f
    val bh = profile.heightM.toFloat().coerceIn(0.08f, 12f)
    val rad = item.buildingRotDeg.toFloat() * (PI / 180.0).toFloat()
    val fx = sin(rad)
    val fz = cos(rad)
    val rx = cos(rad)
    val rz = -sin(rad)
    val bx = item.x.toFloat()
    val by = item.y.toFloat()
    val bz = item.z.toFloat()
    val cylSegs = if (detailLevel >= 1) 14 else 10

    // Compute local terrain height extremes across footprint so sloped-ground houses never float or sink
    val minCornerGroundY = computeFootprintMinGroundY(worldState, bx, by, bz, bw, bd, fx, fz, rx, rz)
    val skirtDrop = (by - minCornerGroundY).coerceIn(0f, 2.2f)

    when (type) {
        BuildingType.ESTRADA_INTERNA, BuildingType.PATIO, BuildingType.CAMINHO_TERRA, BuildingType.ESTACIONAMENTO -> {
            drawGroundSurfaceBuilding3D(type, profile, bx, by, bz, bw, bd, fx, fz, rx, rz, light, pose, focalPx, w, h, scratch)
            return
        }
        BuildingType.JARDIM, BuildingType.CANTEIRO_FLORES -> {
            drawGardenOrFlowerBed3D(type, profile, bx, by, bz, bw, bd, bh, fx, fz, rx, rz, light, pose, focalPx, w, h, scratch)
            return
        }
        BuildingType.ARVORE_ORNAMENTAL -> {
            drawOrnamentalTreeBuilding3D(profile, bx, by, bz, bh, light, cylSegs, pose, focalPx, w, h, scratch)
            return
        }
        BuildingType.CERCA, BuildingType.PORTEIRA -> {
            drawFenceOrGate3D(type, item.buildingGateOpen, profile, bx, by, bz, bw, bh, rad, fx, fz, rx, rz, light, pose, focalPx, w, h, scratch)
            return
        }
        BuildingType.SILO, BuildingType.TANQUE_COMBUSTIVEL -> {
            drawSiloOrTank3D(type, profile, bx, by, bz, bw, bd, bh, fx, fz, rx, rz, light, cylSegs, pose, focalPx, w, h, scratch)
            return
        }
        BuildingType.SECADOR, BuildingType.MOINHO_GRAOS, BuildingType.FABRICA_RACAO -> {
            val bodyCol = parseSafeColor(profile.wallColorHex, Color(0xFF9EA8A2))
            val roofCol = parseSafeColor(profile.roofColorHex, Color(0xFF455A64))
            drawRotatedBox3D(bx, by + 0.16f - skirtDrop * 0.5f, bz, bw * 1.02f, 0.16f + skirtDrop * 0.5f, bd * 1.02f, fx, fz, rx, rz, Color(0xFF5D6460), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(bx, by + bh * 0.44f, bz, bw * 0.78f, bh * 0.44f, bd * 0.78f, fx, fz, rx, rz, bodyCol, light, pose, focalPx, w, h, scratch)
            drawCylinderY3D(bx - rx * (bw * 0.60f), by + bh * 0.25f, by + bh * 0.92f, bz - rz * (bw * 0.60f), 0.88f, Color(0xFFB0BEC5), light, cylSegs, pose, focalPx, w, h, scratch)
            drawConeY3D(bx - rx * (bw * 0.60f), by + bh * 0.92f, by + bh * 1.12f, bz - rz * (bw * 0.60f), 0.98f, roofCol, light, cylSegs, pose, focalPx, w, h, scratch)
            return
        }
        BuildingType.CURRAL -> {
            drawCorral3D(profile, bx, by, bz, bw, bd, bh, fx, fz, rx, rz, light, pose, focalPx, w, h, scratch)
            return
        }
        BuildingType.CELEIRO -> {
            drawBarn3D(profile, bx, by, bz, bw, bd, bh, skirtDrop, fx, fz, rx, rz, light, isNight, pose, focalPx, w, h, scratch)
            return
        }
        BuildingType.GALPAO, BuildingType.GARAGEM, BuildingType.OFICINA -> {
            if (profile.isOpenBay || type == BuildingType.OFICINA) {
                drawOpenBayShedOrGarage3D(profile, type == BuildingType.OFICINA, bx, by, bz, bw, bd, bh, skirtDrop, fx, fz, rx, rz, light, isNight, pose, focalPx, w, h, scratch)
                return
            }
        }
        else -> Unit
    }

    if (profile.isHouse) {
        if (detailLevel >= 1) {
            drawRotatedBox3D(bx + 0.85f, by + 0.025f, bz + 0.85f, bw * 1.12f, 0.01f, bd * 1.12f, fx, fz, rx, rz, Color.Black.copy(alpha = 0.24f), 1f, pose, focalPx, w, h, scratch)
        }
        drawRegionalHouse3D(
            profile = profile,
            bx = bx,
            by = by,
            bz = bz,
            bw = bw,
            bd = bd,
            bh = bh,
            skirtDrop = skirtDrop,
            fx = fx,
            fz = fz,
            rx = rx,
            rz = rz,
            light = light,
            isNight = isNight,
            pose = pose,
            focalPx = focalPx,
            w = w,
            h = h,
            scratch = scratch,
        )
    } else {
        if (detailLevel >= 1) {
            drawRotatedBox3D(bx + 0.75f, by + 0.025f, bz + 0.75f, bw * 1.08f, 0.01f, bd * 1.08f, fx, fz, rx, rz, Color.Black.copy(alpha = 0.22f), 1f, pose, focalPx, w, h, scratch)
        }
        drawCommercialOrRuralFacility3D(type, profile, bx, by, bz, bw, bd, bh, skirtDrop, fx, fz, rx, rz, light, isNight, pose, focalPx, w, h, scratch)
    }
}

private fun computeFootprintMinGroundY(
    worldState: WorldState,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bd: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
): Float {
    var minY = by
    val offsets = arrayOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)
    for ((sx, sz) in offsets) {
        val wx = bx + rx * bw * sx + fx * bd * sz
        val wz = bz + rz * bw * sx + fz * bd * sz
        val hy = getTerrainHeightAt(worldState, wx.toDouble(), wz.toDouble()).toFloat()
        if (hy < minY) minY = hy
    }
    return minY
}

private fun DrawScope.drawRegionalHouse3D(
    profile: BuildingVisualProfile,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bd: Float,
    bh: Float,
    skirtDrop: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    light: Float,
    isNight: Boolean,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val wallCol = parseSafeColor(profile.wallColorHex, Color(0xFFF3EBD8))
    val roofCol = parseSafeColor(profile.roofColorHex, Color(0xFFA64628))
    val trimCol = parseSafeColor(profile.trimColorHex, Color(0xFFC26D24))
    val stoneCol = Color(0xFF6B6A64)
    val darkWood = Color(0xFF4E3322)
    val winCol = if (isNight) Color(0xFFFFD54F) else Color(0xFF456D87)
    val winLight = if (isNight) 1.0f else light

    val coreW = if (profile.hasGarage) bw * 0.78f else bw
    val isStilted = profile.materialStyle == "palafita" || profile.archStyle == RegionalArchStyle.NORTE_AMAZONIA
    val deckElevation = if (isStilted) 1.05f else 0.18f

    // 1. Foundation / Hillside stone retaining wall or Amazon stilts
    if (isStilted) {
        // Raised timber deck & vertical wooden piloti stilts anchored below terrain
        val stiltBot = by - skirtDrop - 0.15f
        val stiltTop = by + deckElevation
        val stiltH = (stiltTop - stiltBot) * 0.5f
        val stiltMidY = (stiltBot + stiltTop) * 0.5f
        for (sx in listOf(-0.85f, 0f, 0.85f)) {
            for (sz in listOf(-0.88f, 0f, 0.88f)) {
                drawRotatedBox3D(
                    bx + rx * coreW * sx + fx * bd * sz,
                    stiltMidY,
                    bz + rz * coreW * sx + fz * bd * sz,
                    0.12f, stiltH, 0.12f, fx, fz, rx, rz, darkWood, light, pose, focalPx, w, h, scratch,
                )
            }
        }
        drawRotatedBox3D(bx, by + deckElevation - 0.08f, bz, coreW * 1.06f, 0.09f, bd * 1.08f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
    } else {
        // Stone/concrete retaining foundation + front entry sidewalk pad
        val plinthTop = by + deckElevation
        val plinthBot = by - skirtDrop - 0.12f
        val plinthH = (plinthTop - plinthBot) * 0.5f
        val plinthMid = (plinthTop + plinthBot) * 0.5f
        drawRotatedBox3D(bx, plinthMid, bz, coreW * 1.04f, plinthH, bd * 1.04f, fx, fz, rx, rz, stoneCol, light, pose, focalPx, w, h, scratch)
        // Concrete sidewalk & front steps leading out along -fx/-fz
        val stepDepth = if (profile.hasVeranda) 1.55f else 0.75f
        drawRotatedBox3D(bx - fx * (bd + stepDepth * 0.55f), by + 0.06f, bz - fz * (bd + stepDepth * 0.55f), coreW * 0.92f, 0.06f, stepDepth * 0.65f, fx, fz, rx, rz, Color(0xFF9C988F), light, pose, focalPx, w, h, scratch)
    }

    val floorY = by + deckElevation
    val wallH = (bh * 0.62f).coerceIn(2.1f, 3.6f)
    val wallMidY = floorY + wallH * 0.5f

    // 2. Main Structural Walls & Regional Material Accents
    drawRotatedBox3D(bx, wallMidY, bz, coreW, wallH * 0.5f, bd, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)

    when (profile.materialStyle) {
        "reboco_fazenda" -> {
            // Iconic Minas Gerais / Brazilian Fazenda lower ochre/terracotta wainscot band (barrado)
            val barradoH = wallH * 0.28f
            drawRotatedBox3D(bx, floorY + barradoH * 0.5f, bz, coreW * 1.015f, barradoH * 0.5f, bd * 1.015f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
            // Side exposed brick kitchen wing accent + brick wood-stove chimney
            drawRotatedBox3D(bx - rx * (coreW * 0.96f), floorY + wallH * 0.42f, bz - rz * (coreW * 0.96f), 0.22f, wallH * 0.40f, bd * 0.68f, fx, fz, rx, rz, Color(0xFFA9553B), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(bx - rx * (coreW * 0.72f) + fx * (bd * 0.45f), floorY + wallH + 0.65f, bz - rz * (coreW * 0.72f) + fz * (bd * 0.45f), 0.28f, 0.75f, 0.28f, fx, fz, rx, rz, Color(0xFF9F4E35), light, pose, focalPx, w, h, scratch)
        }
        "madeira", "palafita" -> {
            // Horizontal plank ribbing & corner timber posts
            val plankCol = shadeColor(wallCol, 0.82f)
            for (band in listOf(0.26f, 0.52f, 0.78f)) {
                drawRotatedBox3D(bx, floorY + wallH * band, bz, coreW * 1.012f, 0.055f, bd * 1.012f, fx, fz, rx, rz, plankCol, light, pose, focalPx, w, h, scratch)
            }
            for (sx in listOf(-1f, 1f)) {
                for (sz in listOf(-1f, 1f)) {
                    drawRotatedBox3D(bx + rx * coreW * sx + fx * bd * sz, wallMidY, bz + rz * coreW * sx + fz * bd * sz, 0.11f, wallH * 0.5f, 0.11f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
                }
            }
        }
        "enxaimel" -> {
            // Timber-frame horizontal and vertical beams over light plaster
            for (band in listOf(0.08f, 0.52f, 0.95f)) {
                drawRotatedBox3D(bx, floorY + wallH * band, bz, coreW * 1.015f, 0.07f, bd * 1.015f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
            }
            for (fxFrac in listOf(-0.98f, -0.35f, 0.35f, 0.98f)) {
                drawRotatedBox3D(bx + rx * (coreW * fxFrac), wallMidY, bz + rz * (coreW * fxFrac), 0.08f, wallH * 0.5f, bd * 1.015f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
            }
        }
        "tijolinho" -> {
            // Exposed brick trim bands & reinforced corner columns
            for (sx in listOf(-1f, 1f)) {
                for (sz in listOf(-1f, 1f)) {
                    drawRotatedBox3D(bx + rx * coreW * sx + fx * bd * sz, wallMidY, bz + rz * coreW * sx + fz * bd * sz, 0.18f, wallH * 0.5f, 0.18f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
                }
            }
        }
    }

    // 3. Recessed Doors & Framed Windows (Front face is -fx/-fz, matching WorldCollision porchZ = -hd - 0.95!)
    val frontWallX = bx - fx * (bd + 0.03f)
    val frontWallZ = bz - fz * (bd + 0.03f)
    val doorH = (wallH * 0.72f).coerceIn(1.75f, 2.25f)
    // Door trim & inner wood door
    drawRotatedBox3D(frontWallX, floorY + doorH * 0.5f, frontWallZ, 0.56f, doorH * 0.5f, 0.08f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(frontWallX - fx * 0.03f, floorY + doorH * 0.5f, frontWallZ - fz * 0.03f, 0.44f, doorH * 0.46f, 0.07f, fx, fz, rx, rz, Color(0xFF5D3A24), light, pose, focalPx, w, h, scratch)

    // Front & side windows with wooden frames
    for (sideSign in listOf(-1f, 1f)) {
        val wx = frontWallX + rx * (coreW * 0.56f * sideSign)
        val wz = frontWallZ + rz * (coreW * 0.56f * sideSign)
        drawRotatedBox3D(wx, floorY + wallH * 0.56f, wz, 0.62f, 0.46f, 0.07f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(wx - fx * 0.02f, floorY + wallH * 0.56f, wz - fz * 0.02f, 0.50f, 0.36f, 0.07f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
    }
    // Side windows
    for (sideSign in listOf(-1f, 1f)) {
        val sx = bx + rx * ((coreW + 0.03f) * sideSign)
        val sz = bz + rz * ((coreW + 0.03f) * sideSign)
        drawRotatedBox3D(sx, floorY + wallH * 0.56f, sz, 0.07f, 0.42f, 0.58f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
    }

    // 4. Pitched Brazilian Ceramic / Slate Roof with Overhanging Eaves & Ridge Cap (Cumeeira)
    val eavesY = floorY + wallH
    val roofPeakH = if (profile.archStyle.hasSteepRoof) (bh * 0.44f).coerceIn(1.25f, 2.35f) else (bh * 0.32f).coerceIn(0.95f, 1.65f)
    val eavesW = coreW * 1.14f
    val eavesD = bd * 1.12f
    // Fascia / eaves plate
    drawRotatedBox3D(bx, eavesY + 0.08f, bz, eavesW, 0.09f, eavesD, fx, fz, rx, rz, shadeColor(roofCol, 0.85f), light, pose, focalPx, w, h, scratch)
    // Lower hipped/pitched ceramic roof tiers + upper cumeeira ridge
    drawTiltedBox3D(bx, eavesY + roofPeakH * 0.34f, bz, eavesW * 0.96f, roofPeakH * 0.32f, eavesD * 0.96f, fx, fz, rx, rz, 0f, 0f, roofCol, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.62f)
    drawTiltedBox3D(bx, eavesY + roofPeakH * 0.72f, bz, eavesW * 0.60f, roofPeakH * 0.28f, eavesD * 0.64f, fx, fz, rx, rz, 0f, 0f, shadeColor(roofCol, 1.07f), light, pose, focalPx, w, h, scratch, taperTopFactor = 0.48f)
    // Ridge beam (cumeeira)
    drawRotatedBox3D(bx, eavesY + roofPeakH * 0.98f, bz, eavesW * 0.46f, 0.09f, 0.14f, fx, fz, rx, rz, shadeColor(roofCol, 1.15f), light, pose, focalPx, w, h, scratch)

    // 5. Front Pillared Veranda (matches WorldCollision pillars at porchZ = -hd - 0.95 & +/- coreW*0.85!)
    if (profile.hasVeranda) {
        val verandaD = if (profile.archStyle.hasDeepVeranda) 1.55f else 1.20f
        val verandaCenterDist = bd + verandaD * 0.5f
        val vx = bx - fx * verandaCenterDist
        val vz = bz - fz * verandaCenterDist
        // Veranda floor deck
        drawRotatedBox3D(vx, floorY - 0.04f, vz, coreW * 1.04f, 0.08f, verandaD * 0.55f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        // Veranda sloped roof overhang
        val porchRoofY = eavesY - 0.24f
        drawTiltedBox3D(vx, porchRoofY, vz, coreW * 1.10f, 0.11f, verandaD * 0.62f, fx, fz, rx, rz, 10f, 0f, shadeColor(roofCol, 0.96f), light, pose, focalPx, w, h, scratch)
        // Structural veranda columns (pillars) matching collision coordinates
        val pillarZDist = bd + verandaD * 0.82f
        val postBaseY = floorY
        val postTopY = porchRoofY - 0.05f
        val postMidY = (postBaseY + postTopY) * 0.5f
        val postHalfH = (postTopY - postBaseY) * 0.5f
        val pillarFracs = if (coreW >= 4.2f) listOf(-0.85f, -0.28f, 0.28f, 0.85f) else listOf(-0.85f, 0f, 0.85f)
        for (frac in pillarFracs) {
            val px = bx - fx * pillarZDist + rx * (coreW * frac)
            val pz = bz - fz * pillarZDist + rz * (coreW * frac)
            drawRotatedBox3D(px, postMidY, pz, 0.10f, postHalfH, 0.10f, fx, fz, rx, rz, darkWood, light, pose, focalPx, w, h, scratch)
            // Masonry pedestal base on pillars for Sudeste / Fazenda look
            if (profile.materialStyle == "reboco_fazenda" || profile.materialStyle == "alvenaria") {
                drawRotatedBox3D(px, floorY + 0.28f, pz, 0.15f, 0.28f, 0.15f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
            }
        }
    }

    // 6. Attached Open-Front Right-Wing Garage (matches WorldCollision garCenterX = coreW + garHalfW)
    if (profile.hasGarage) {
        val garHalfW = max(1.6f, bw * 0.34f)
        val garHalfD = max(2.2f, bd * 0.72f)
        val garCenterX = coreW + garHalfW
        val gx = bx + rx * garCenterX
        val gz = bz + rz * garCenterX
        val garH = (wallH * 0.88f).coerceIn(2.2f, 3.1f)
        // Concrete floor pad
        drawRotatedBox3D(gx, by + 0.08f, gz, garHalfW, 0.08f, garHalfD, fx, fz, rx, rz, Color(0xFF7B7D78), light, pose, focalPx, w, h, scratch)
        // Rear (+fx/+fz) wall & outer (+rx/+rz) wall — leaving front (-fx/-fz) open for vehicles!
        drawRotatedBox3D(gx + fx * (garHalfD - 0.16f), by + 0.16f + garH * 0.5f, gz + fz * (garHalfD - 0.16f), garHalfW, garH * 0.5f, 0.16f, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(gx + rx * (garHalfW - 0.16f), by + 0.16f + garH * 0.5f, gz + rz * (garHalfW - 0.16f), 0.16f, garH * 0.5f, garHalfD, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)
        // Front outer corner pillar
        drawRotatedBox3D(gx + rx * (garHalfW - 0.16f) - fx * (garHalfD - 0.16f), by + 0.16f + garH * 0.5f, gz + rz * (garHalfW - 0.16f) - fz * (garHalfD - 0.16f), 0.14f, garH * 0.5f, 0.14f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        // Garage roof connecting to house eaves
        drawTiltedBox3D(gx, by + 0.16f + garH + 0.14f, gz, garHalfW * 1.08f, 0.14f, garHalfD * 1.08f, fx, fz, rx, rz, 0f, -8f, roofCol, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.85f)
    }
}

private fun DrawScope.drawOpenBayShedOrGarage3D(
    profile: BuildingVisualProfile,
    isWorkshop: Boolean,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bd: Float,
    bh: Float,
    skirtDrop: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    light: Float,
    isNight: Boolean,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val wallCol = parseSafeColor(profile.wallColorHex, Color(0xFFD8D6CF))
    val roofCol = parseSafeColor(profile.roofColorHex, Color(0xFF3E5A70))
    val trimCol = parseSafeColor(profile.trimColorHex, Color(0xFF4E301D))
    val wallT = 0.24f
    val wallH = bh * 0.72f

    // Concrete slab + hillside skirt
    drawRotatedBox3D(bx, by + 0.08f - skirtDrop * 0.5f, bz, bw * 1.02f, 0.08f + skirtDrop * 0.5f, bd * 1.02f, fx, fz, rx, rz, Color(0xFF6B6F6C), light, pose, focalPx, w, h, scratch)
    // Open-front bay matching WorldCollision: rear (+fx/+fz) wall + left (-rx/-rz) wall + right (+rx/+rz) wall
    drawRotatedBox3D(bx + fx * (bd - wallT), by + 0.16f + wallH * 0.5f, bz + fz * (bd - wallT), bw, wallH * 0.5f, wallT, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(bx - rx * (bw - wallT), by + 0.16f + wallH * 0.5f, bz - rz * (bw - wallT), wallT, wallH * 0.5f, bd, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(bx + rx * (bw - wallT), by + 0.16f + wallH * 0.5f, bz + rz * (bw - wallT), wallT, wallH * 0.5f, bd, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)

    // Front steel/timber columns on left & right doorway edges
    for (sx in listOf(-1f, 1f)) {
        drawRotatedBox3D(bx + rx * (bw - wallT) * sx - fx * (bd - wallT), by + 0.16f + wallH * 0.5f, bz + rz * (bw - wallT) * sx - fz * (bd - wallT), 0.18f, wallH * 0.5f, 0.18f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
    }
    // Front header beam + workshop fascia / night floodlight strip
    drawRotatedBox3D(bx - fx * (bd - wallT), by + 0.16f + wallH - 0.18f, bz - fz * (bd - wallT), bw, 0.22f, 0.20f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
    if (isWorkshop) {
        // Workshop hydraulic 2-post lift inside bay, red rolling tool chest, and oil service pit markings
        val liftCol = Color(0xFFFFB300)
        val postOffset = min(1.85f, bw * 0.44f)
        drawRotatedBox3D(bx - rx * postOffset, by + 0.16f + 1.45f, bz - rz * postOffset, 0.14f, 1.45f, 0.14f, fx, fz, rx, rz, liftCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(bx + rx * postOffset, by + 0.16f + 1.45f, bz + rz * postOffset, 0.14f, 1.45f, 0.14f, fx, fz, rx, rz, liftCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(bx, by + 0.16f + 2.95f, bz, postOffset + 0.16f, 0.10f, 0.14f, fx, fz, rx, rz, liftCol, light, pose, focalPx, w, h, scratch)
        // Red tool cabinet & diagnostic workbench at rear corner
        drawRotatedBox3D(bx + fx * (bd * 0.62f) - rx * (bw * 0.54f), by + 0.16f + 0.55f, bz + fz * (bd * 0.62f) - rz * (bw * 0.54f), 0.72f, 0.55f, 0.38f, fx, fz, rx, rz, Color(0xFFD32F2F), light, pose, focalPx, w, h, scratch)
        // Yellow/black safety bay threshold strip
        drawRotatedBox3D(bx - fx * (bd * 0.85f), by + 0.17f, bz - fz * (bd * 0.85f), bw * 0.78f, 0.02f, 0.24f, fx, fz, rx, rz, Color(0xFFFBC02D), light, pose, focalPx, w, h, scratch)
    }
    if (isNight || isWorkshop) {
        drawRotatedBox3D(bx - fx * (bd - 0.25f), by + 0.16f + wallH - 0.40f, bz - fz * (bd - 0.25f), bw * 0.34f, 0.08f, 0.10f, fx, fz, rx, rz, if (isNight) Color(0xFFFFF176) else Color(0xFFFFD54F), if (isNight) 1.0f else light, pose, focalPx, w, h, scratch)
    }
    // High gable roof
    val roofBaseY = by + 0.16f + wallH
    val roofPeakH = bh * 0.28f
    drawRotatedBox3D(bx, roofBaseY + 0.08f, bz, bw * 1.08f, 0.08f, bd * 1.08f, fx, fz, rx, rz, shadeColor(roofCol, 0.86f), light, pose, focalPx, w, h, scratch)
    drawTiltedBox3D(bx, roofBaseY + roofPeakH * 0.55f, bz, bw * 0.92f, roofPeakH * 0.48f, bd * 0.96f, fx, fz, rx, rz, 0f, 0f, roofCol, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.52f)
}

private fun DrawScope.drawBarn3D(
    profile: BuildingVisualProfile,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bd: Float,
    bh: Float,
    skirtDrop: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    light: Float,
    isNight: Boolean,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val wallCol = parseSafeColor(profile.wallColorHex, Color(0xFF8C3328))
    val roofCol = parseSafeColor(profile.roofColorHex, Color(0xFF595D62))
    val trimCol = parseSafeColor(profile.trimColorHex, Color(0xFFF4EFE6))
    val wallH = bh * 0.56f

    drawRotatedBox3D(bx, by + 0.14f - skirtDrop * 0.5f, bz, bw * 1.03f, 0.14f + skirtDrop * 0.5f, bd * 1.03f, fx, fz, rx, rz, Color(0xFF606562), light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(bx, by + 0.24f + wallH * 0.5f, bz, bw, wallH * 0.5f, bd, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)
    // Gambrel two-stage barn roof
    val roofY = by + 0.24f + wallH
    drawTiltedBox3D(bx, roofY + bh * 0.14f, bz, bw * 1.06f, bh * 0.14f, bd * 1.05f, fx, fz, rx, rz, 0f, 0f, roofCol, light, pose, focalPx, w, h, scratch, taperTopFactor = 0.72f)
    drawTiltedBox3D(bx, roofY + bh * 0.34f, bz, bw * 0.74f, bh * 0.14f, bd * 0.92f, fx, fz, rx, rz, 0f, 0f, shadeColor(roofCol, 1.08f), light, pose, focalPx, w, h, scratch, taperTopFactor = 0.48f)

    // Contrasting X-braced double barn doors on front (-fx/-fz) + loft hay door
    val frontX = bx - fx * (bd + 0.04f)
    val frontZ = bz - fz * (bd + 0.04f)
    drawRotatedBox3D(frontX, by + 0.24f + wallH * 0.42f, frontZ, bw * 0.38f, wallH * 0.40f, 0.08f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(frontX - fx * 0.03f, by + 0.24f + wallH * 0.42f, frontZ - fz * 0.03f, bw * 0.31f, wallH * 0.34f, 0.06f, fx, fz, rx, rz, shadeColor(wallCol, 0.76f), light, pose, focalPx, w, h, scratch)
    val loftCol = if (isNight) Color(0xFFFFD54F) else trimCol
    drawRotatedBox3D(frontX, roofY + bh * 0.10f, frontZ, 0.65f, 0.52f, 0.10f, fx, fz, rx, rz, loftCol, if (isNight) 1f else light, pose, focalPx, w, h, scratch)
}

private fun DrawScope.drawCorral3D(
    profile: BuildingVisualProfile,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bd: Float,
    bh: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    light: Float,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val wood = parseSafeColor(profile.wallColorHex, Color(0xFF6B472B))
    val roofCol = parseSafeColor(profile.roofColorHex, Color(0xFF953E28))
    // Compacted earth corral floor
    drawRotatedBox3D(bx, by + 0.03f, bz, bw, 0.03f, bd, fx, fz, rx, rz, Color(0xFF7B634A), light, pose, focalPx, w, h, scratch)
    // Perimeter fence rails (rear, left, right, partial front with cattle chute gap)
    val railH = 1.45f
    drawRotatedBox3D(bx + fx * (bd - 0.12f), by + railH * 0.55f, bz + fz * (bd - 0.12f), bw, 0.10f, 0.10f, fx, fz, rx, rz, wood, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(bx + fx * (bd - 0.12f), by + railH * 0.28f, bz + fz * (bd - 0.12f), bw, 0.10f, 0.10f, fx, fz, rx, rz, wood, light, pose, focalPx, w, h, scratch)
    for (sx in listOf(-1f, 1f)) {
        drawRotatedBox3D(bx + rx * (bw - 0.12f) * sx, by + railH * 0.55f, bz + rz * (bw - 0.12f) * sx, 0.10f, 0.10f, bd, fx, fz, rx, rz, wood, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(bx + rx * (bw - 0.12f) * sx, by + railH * 0.28f, bz + rz * (bw - 0.12f) * sx, 0.10f, 0.10f, bd, fx, fz, rx, rz, wood, light, pose, focalPx, w, h, scratch)
        // Front partial wing fences
        drawRotatedBox3D(bx - fx * (bd - 0.12f) + rx * (bw * 0.68f * sx), by + railH * 0.55f, bz - fz * (bd - 0.12f) + rz * (bw * 0.68f * sx), bw * 0.32f, 0.10f, 0.10f, fx, fz, rx, rz, wood, light, pose, focalPx, w, h, scratch)
    }
    // Shade shelter roof along the back strip of corral
    drawRotatedBox3D(bx + fx * (bd * 0.62f), by + bh * 0.92f, bz + fz * (bd * 0.62f), bw * 0.92f, 0.10f, bd * 0.32f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
    for (sx in listOf(-0.85f, 0f, 0.85f)) {
        drawRotatedBox3D(bx + rx * bw * sx + fx * (bd * 0.45f), by + bh * 0.46f, bz + rz * bw * sx + fz * (bd * 0.45f), 0.12f, bh * 0.46f, 0.12f, fx, fz, rx, rz, wood, light, pose, focalPx, w, h, scratch)
    }
}

private fun DrawScope.drawCommercialOrRuralFacility3D(
    type: BuildingType,
    profile: BuildingVisualProfile,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bd: Float,
    bh: Float,
    skirtDrop: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    light: Float,
    isNight: Boolean,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val wallCol = when (type) {
        BuildingType.ESTUFA_FLORES -> Color(0xFFCDE9EE)
        BuildingType.MERCADO_RURAL -> Color(0xFFDEC39A)
        else -> parseSafeColor(profile.wallColorHex, Color(0xFFEAE2D3))
    }
    val roofCol = when (type) {
        BuildingType.ESTUFA_FLORES -> Color(0xFFB3E5FC)
        else -> parseSafeColor(profile.roofColorHex, Color(0xFF9C3F28))
    }
    val trimCol = parseSafeColor(profile.trimColorHex, Color(0xFF6B4A2D))
    val winCol = if (isNight) Color(0xFFFFD54F) else Color(0xFF6BA6C4)
    val winLight = if (isNight) 1f else light

    if (type == BuildingType.POSTO_COMBUSTIVEL) {
        // Fuel Station: wide forecourt canopy supported by 4 steel columns + central diesel S10 pump island + roadside totem + side convenience kiosk
        val canopyY = by + bh * 0.82f
        val islandW = bw * 0.36f
        val islandD = bd * 0.46f
        // Concrete forecourt apron
        drawRotatedBox3D(bx, by + 0.04f - skirtDrop * 0.4f, bz, bw * 1.10f, 0.04f + skirtDrop * 0.4f, bd * 1.05f, fx, fz, rx, rz, Color(0xFF565B5E), light, pose, focalPx, w, h, scratch)
        // Central concrete pump curb
        drawRotatedBox3D(bx, by + 0.16f, bz, islandW, 0.12f, islandD, fx, fz, rx, rz, Color(0xFFD5D9DC), light, pose, focalPx, w, h, scratch)
        // Two Diesel S10 dispensers on central island
        for (pzSign in listOf(-0.48f, 0.48f)) {
            val px = bx + fx * (islandD * pzSign)
            val pz = bz + fz * (islandD * pzSign)
            drawRotatedBox3D(px, by + 0.92f, pz, 0.48f, 0.68f, 0.36f, fx, fz, rx, rz, Color(0xFFEAEAEA), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(px, by + 1.38f, pz, 0.50f, 0.22f, 0.38f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(px, by + 1.02f, pz, 0.52f, 0.18f, 0.26f, fx, fz, rx, rz, Color(0xFF1565C0), 1f, pose, focalPx, w, h, scratch)
        }
        // Four canopy columns aligned on drive-through lanes
        for (sx in listOf(-0.78f, 0.78f)) {
            for (sz in listOf(-0.68f, 0.68f)) {
                drawRotatedBox3D(bx + rx * (bw * sx) + fx * (bd * sz), by + bh * 0.42f, bz + rz * (bw * sx) + fz * (bd * sz), 0.18f, bh * 0.40f, 0.18f, fx, fz, rx, rz, Color(0xFFCFD8DC), light, pose, focalPx, w, h, scratch)
            }
        }
        // Overhead fuel station canopy + glowing red/gold fascia band
        drawRotatedBox3D(bx, canopyY, bz, bw * 1.04f, 0.36f, bd * 0.96f, fx, fz, rx, rz, Color(0xFFF3F6F4), light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(bx, canopyY + 0.18f, bz, bw * 1.06f, 0.16f, bd * 0.98f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(bx - fx * (bd * 0.98f), canopyY + 0.10f, bz - fz * (bd * 0.98f), bw * 0.90f, 0.10f, 0.08f, fx, fz, rx, rz, trimCol, 1f, pose, focalPx, w, h, scratch)
        // Convenience kiosk booth offset to +rx side (matches WorldCollision station booth)
        val kx = bx + rx * (bw * 0.85f + 2.2f)
        val kz = bz + rz * (bw * 0.85f + 2.2f)
        drawRotatedBox3D(kx, by + 1.55f, kz, 1.9f, 1.55f, 1.9f, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(kx, by + 3.22f, kz, 2.15f, 0.18f, 2.15f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(kx - fx * 1.92f, by + 1.65f, kz - fz * 1.92f, 1.15f, 0.62f, 0.08f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
        // Tall price monolith sign
        val signX = bx - rx * (bw * 0.95f) - fx * (bd * 0.82f)
        val signZ = bz - rz * (bw * 0.95f) - fz * (bd * 0.82f)
        drawRotatedBox3D(signX, by + 2.4f, signZ, 0.55f, 2.4f, 0.22f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(signX, by + 3.4f, signZ, 0.45f, 0.9f, 0.26f, fx, fz, rx, rz, trimCol, 1f, pose, focalPx, w, h, scratch)
        return
    }

    if (type == BuildingType.ESTACAO_GRAOS || type == BuildingType.COOPERATIVA) {
        // Weighbridge station: steel scale deck, yellow/black safety curbs, sunken unloading hopper grate & scale operator booth
        drawRotatedBox3D(bx, by + 0.05f - skirtDrop * 0.4f, bz, bw * 1.05f, 0.05f + skirtDrop * 0.4f, bd * 1.05f, fx, fz, rx, rz, Color(0xFF606669), light, pose, focalPx, w, h, scratch)
        // Drivable steel weighbridge platform flush at ground level
        drawRotatedBox3D(bx, by + 0.11f, bz, bw * 0.82f, 0.05f, bd * 0.46f, fx, fz, rx, rz, Color(0xFF3E454B), light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(bx, by + 0.14f, bz - fz * (bd * 0.46f), bw * 0.84f, 0.05f, 0.14f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(bx, by + 0.14f, bz + fz * (bd * 0.46f), bw * 0.84f, 0.05f, 0.14f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        // Sunken grain unloading grate (moega) beside weighbridge
        drawRotatedBox3D(bx + rx * (bw * 0.34f), by + 0.12f, bz + fz * (bd * 0.64f), bw * 0.42f, 0.04f, bd * 0.26f, fx, fz, rx, rz, Color(0xFF21262A), light, pose, focalPx, w, h, scratch)
        // Operator scale booth offset cleanly to -rx / -fx corner (matching WorldCollision collider)
        val boothX = bx - rx * 5.2f - fx * 4.2f
        val boothZ = bz - rz * 5.2f - fz * 4.2f
        drawRotatedBox3D(boothX, by + 1.75f, boothZ, 1.55f, 1.75f, 1.45f, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(boothX, by + 3.68f, boothZ, 1.85f, 0.20f, 1.75f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(boothX + rx * 1.48f, by + 2.05f, boothZ + rz * 1.48f, 0.08f, 0.62f, 0.95f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
        // Overhead weighbridge portal gantry
        drawRotatedBox3D(bx, by + bh * 0.88f, bz, bw * 0.86f, 0.24f, 0.32f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(bx - rx * (bw * 0.82f), by + bh * 0.44f, bz - rz * (bw * 0.82f), 0.16f, bh * 0.44f, 0.16f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        drawRotatedBox3D(bx + rx * (bw * 0.82f), by + bh * 0.44f, bz + rz * (bw * 0.82f), 0.16f, bh * 0.44f, 0.16f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        return
    }

    // Standard commercial / institutional facility foundation + main volume + pitched roof
    drawRotatedBox3D(bx, by + 0.14f - skirtDrop * 0.5f, bz, bw * 1.03f, 0.14f + skirtDrop * 0.5f, bd * 1.03f, fx, fz, rx, rz, Color(0xFF606864), light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(bx, by + bh * 0.44f, bz, bw, bh * 0.36f, bd, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(bx, by + bh * 0.82f, bz, bw * 1.08f, bh * 0.10f, bd * 1.08f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
    drawTiltedBox3D(bx, by + bh * 0.96f, bz, bw * 0.72f, bh * 0.11f, bd * 0.78f, fx, fz, rx, rz, 0f, 0f, shadeColor(roofCol, 1.08f), light, pose, focalPx, w, h, scratch, taperTopFactor = 0.55f)

    val frontX = bx - fx * (bd * 1.01f)
    val frontZ = bz - fz * (bd * 1.01f)

    when (type) {
        BuildingType.BANCO -> {
            // Marble steps + 4 classical bank colonnade pillars + gold fascia plaque + glass double doors
            drawRotatedBox3D(frontX - fx * 0.95f, by + 0.12f, frontZ - fz * 0.95f, bw * 0.92f, 0.12f, 0.95f, fx, fz, rx, rz, Color(0xFFCCC6B8), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX - fx * 0.75f, by + bh * 0.74f, frontZ - fz * 0.75f, bw * 0.98f, 0.14f, 0.85f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX - fx * 1.56f, by + bh * 0.75f, frontZ - fz * 1.56f, bw * 0.65f, 0.18f, 0.08f, fx, fz, rx, rz, trimCol, 1f, pose, focalPx, w, h, scratch)
            for (frac in listOf(-0.82f, -0.32f, 0.32f, 0.82f)) {
                drawRotatedBox3D(frontX - fx * 1.35f + rx * (bw * frac), by + bh * 0.42f, frontZ - fz * 1.35f + rz * (bw * frac), 0.18f, bh * 0.34f, 0.18f, fx, fz, rx, rz, wallCol, light, pose, focalPx, w, h, scratch)
            }
            drawRotatedBox3D(frontX, by + bh * 0.36f, frontZ, 0.95f, bh * 0.28f, 0.12f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX - rx * (bw * 0.56f), by + bh * 0.46f, frontZ - rz * (bw * 0.56f), 0.72f, 0.52f, 0.12f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX + rx * (bw * 0.56f), by + bh * 0.46f, frontZ + rz * (bw * 0.56f), 0.72f, 0.52f, 0.12f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
        }
        BuildingType.CONCESSIONARIA -> {
            // Panoramic showroom glass + cantilevered green/gold dealership canopy
            drawRotatedBox3D(frontX, by + bh * 0.42f, frontZ, bw * 0.88f, bh * 0.32f, 0.14f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX - fx * 1.25f, by + bh * 0.76f, frontZ - fz * 1.25f, bw * 1.06f, 0.16f, 1.35f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX - fx * 2.55f, by + bh * 0.76f, frontZ - fz * 2.55f, bw * 0.85f, 0.18f, 0.08f, fx, fz, rx, rz, trimCol, 1f, pose, focalPx, w, h, scratch)
            for (frac in listOf(-0.88f, 0.88f)) {
                drawRotatedBox3D(frontX - fx * 2.2f + rx * (bw * frac), by + bh * 0.40f, frontZ - fz * 2.2f + rz * (bw * frac), 0.16f, bh * 0.38f, 0.16f, fx, fz, rx, rz, Color(0xFFB0BEC5), light, pose, focalPx, w, h, scratch)
            }
        }
        BuildingType.LOJA_AGROPECUARIA -> {
            // Front porch canopy + stacked fertilizer / seed pallets out front
            drawRotatedBox3D(frontX - fx * 0.95f, by + bh * 0.66f, frontZ - fz * 0.95f, bw * 1.02f, 0.12f, 1.05f, fx, fz, rx, rz, roofCol, light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX - fx * 1.95f, by + bh * 0.68f, frontZ - fz * 1.95f, bw * 0.68f, 0.15f, 0.08f, fx, fz, rx, rz, trimCol, 1f, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX - rx * (bw * 0.54f) - fx * 1.05f, by + 0.42f, frontZ - rz * (bw * 0.54f) - fz * 1.05f, 0.75f, 0.38f, 0.55f, fx, fz, rx, rz, Color(0xFFEEEEEE), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX + rx * (bw * 0.54f) - fx * 1.05f, by + 0.42f, frontZ + rz * (bw * 0.54f) - fz * 1.05f, 0.75f, 0.38f, 0.55f, fx, fz, rx, rz, Color(0xFF43A047), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX, by + bh * 0.38f, frontZ, 0.88f, bh * 0.28f, 0.12f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
        }
        BuildingType.MADEIREIRA -> {
            // Side timber & lumber stacks beside building
            val stackX = bx + rx * (bw + 1.35f)
            val stackZ = bz + rz * (bw + 1.35f)
            drawRotatedBox3D(stackX, by + 0.48f, stackZ, 0.95f, 0.44f, bd * 0.72f, fx, fz, rx, rz, Color(0xFFB7793E), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(stackX, by + 1.08f, stackZ, 0.78f, 0.28f, bd * 0.62f, fx, fz, rx, rz, Color(0xFFD69A56), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX, by + bh * 0.34f, frontZ, bw * 0.44f, bh * 0.28f, 0.14f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        }
        BuildingType.LOJA_MATERIAIS -> {
            // Cement/brick pallets beside entrance
            drawRotatedBox3D(frontX - rx * (bw * 0.58f) - fx * 1.0f, by + 0.48f, frontZ - rz * (bw * 0.58f) - fz * 1.0f, 0.80f, 0.44f, 0.62f, fx, fz, rx, rz, Color(0xFFA84A32), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX + rx * (bw * 0.58f) - fx * 1.0f, by + 0.42f, frontZ + rz * (bw * 0.58f) - fz * 1.0f, 0.75f, 0.36f, 0.58f, fx, fz, rx, rz, Color(0xFFBDBDBD), light, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX, by + bh * 0.36f, frontZ, bw * 0.42f, bh * 0.28f, 0.14f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
        }
        BuildingType.PONTO_ANIMAIS -> {
            // Timber loading chute rails projecting forward
            for (sx in listOf(-1f, 1f)) {
                drawRotatedBox3D(frontX - fx * 1.35f + rx * (1.25f * sx), by + 0.65f, frontZ - fz * 1.35f + rz * (1.25f * sx), 0.10f, 0.55f, 1.35f, fx, fz, rx, rz, Color(0xFF6B472B), light, pose, focalPx, w, h, scratch)
            }
            drawRotatedBox3D(frontX, by + bh * 0.34f, frontZ, bw * 0.46f, bh * 0.28f, 0.14f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        }
        BuildingType.ARMAZEM -> {
            // Elevated loading dock apron + triple roll-up warehouse bays
            drawRotatedBox3D(frontX - fx * 0.9f, by + 0.28f, frontZ - fz * 0.9f, bw * 0.96f, 0.28f, 0.9f, fx, fz, rx, rz, Color(0xFF8C928E), light, pose, focalPx, w, h, scratch)
            for (doorFrac in listOf(-0.56f, 0f, 0.56f)) {
                drawRotatedBox3D(frontX + rx * (bw * doorFrac), by + bh * 0.42f, frontZ + rz * (bw * doorFrac), bw * 0.20f, bh * 0.26f, 0.12f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
            }
        }
        in setOf(BuildingType.DEPOSITO, BuildingType.ESTABULO, BuildingType.FABRICA, BuildingType.AVIARIO) -> {
            drawRotatedBox3D(frontX, by + bh * 0.32f, frontZ, bw * 0.46f, bh * 0.28f, 0.14f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        }
        else -> {
            drawRotatedBox3D(frontX - rx * (bw * 0.42f), by + bh * 0.48f, frontZ - rz * (bw * 0.42f), 0.65f, 0.48f, 0.12f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
            drawRotatedBox3D(frontX + rx * (bw * 0.42f), by + bh * 0.48f, frontZ + rz * (bw * 0.42f), 0.65f, 0.48f, 0.12f, fx, fz, rx, rz, winCol, winLight, pose, focalPx, w, h, scratch)
        }
    }
}

private fun DrawScope.drawSiloOrTank3D(
    type: BuildingType,
    profile: BuildingVisualProfile,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bd: Float,
    bh: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    light: Float,
    cylSegs: Int,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val cylR = min(bw, bd) * 0.88f
    val bodyCol = parseSafeColor(profile.wallColorHex, if (type == BuildingType.TANQUE_COMBUSTIVEL) Color(0xFFD9822B) else Color(0xFFC4CEC8))
    val roofCol = parseSafeColor(profile.roofColorHex, if (type == BuildingType.TANQUE_COMBUSTIVEL) Color(0xFFB71C1C) else Color(0xFF90A198))
    // Concrete ring plinth
    drawRotatedBox3D(bx, by + 0.10f, bz, cylR * 1.08f, 0.10f, cylR * 1.08f, fx, fz, rx, rz, Color(0xFF5D6366), light, pose, focalPx, w, h, scratch)
    drawCylinderY3D(bx, by + 0.15f, by + bh * 0.78f, bz, cylR, bodyCol, light, cylSegs, pose, focalPx, w, h, scratch)
    drawConeY3D(bx, by + bh * 0.78f, by + bh * 1.02f, bz, cylR * 1.04f, roofCol, light, cylSegs, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(bx + rx * (cylR + 0.12f), by + bh * 0.42f, bz + rz * (cylR + 0.12f), 0.08f, bh * 0.40f, 0.14f, fx, fz, rx, rz, Color(0xFF424B47), light, pose, focalPx, w, h, scratch)
}

private fun DrawScope.drawFenceOrGate3D(
    type: BuildingType,
    isGateOpen: Boolean,
    profile: BuildingVisualProfile,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bh: Float,
    rad: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    light: Float,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val wood = parseSafeColor(profile.wallColorHex, if (type == BuildingType.PORTEIRA) Color(0xFFA05B2B) else Color(0xFF6E4726))
    drawRotatedBox3D(bx - rx * (bw * 0.88f), by + bh * 0.5f, bz - rz * (bw * 0.88f), 0.11f, bh * 0.5f, 0.11f, fx, fz, rx, rz, wood, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(bx + rx * (bw * 0.88f), by + bh * 0.5f, bz + rz * (bw * 0.88f), 0.11f, bh * 0.5f, 0.11f, fx, fz, rx, rz, wood, light, pose, focalPx, w, h, scratch)
    val openSwingRad = if (type == BuildingType.PORTEIRA && isGateOpen) 1.32f else 0f
    val gFx = sin(rad + openSwingRad)
    val gFz = cos(rad + openSwingRad)
    val gRx = cos(rad + openSwingRad)
    val gRz = -sin(rad + openSwingRad)
    val hingeX = bx - rx * (bw * 0.84f)
    val hingeZ = bz - rz * (bw * 0.84f)
    val gateMidX = hingeX + gRx * (bw * 0.84f)
    val gateMidZ = hingeZ + gRz * (bw * 0.84f)
    drawRotatedBox3D(gateMidX, by + bh * 0.66f, gateMidZ, bw * 0.84f, 0.08f, 0.07f, gFx, gFz, gRx, gRz, wood, light, pose, focalPx, w, h, scratch)
    drawRotatedBox3D(gateMidX, by + bh * 0.36f, gateMidZ, bw * 0.84f, 0.08f, 0.07f, gFx, gFz, gRx, gRz, wood, light, pose, focalPx, w, h, scratch)
}

private fun DrawScope.drawGroundSurfaceBuilding3D(
    type: BuildingType,
    profile: BuildingVisualProfile,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bd: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    light: Float,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val baseCol = parseSafeColor(
        profile.wallColorHex,
        when (type) {
            BuildingType.CAMINHO_TERRA -> Color(0xFF8A6C4B)
            BuildingType.ESTACIONAMENTO -> Color(0xFF7A7E82)
            else -> Color(0xFF8C8170)
        },
    )
    drawRotatedBox3D(bx, by + 0.04f, bz, bw, 0.04f, bd, fx, fz, rx, rz, baseCol, light, pose, focalPx, w, h, scratch)
    if (type == BuildingType.ESTACIONAMENTO) {
        val trimCol = parseSafeColor(profile.roofColorHex, Color(0xFFD69E2E))
        // Parking bay lane dividers & timber wheel stops
        for (laneX in listOf(-0.6f, 0f, 0.6f)) {
            drawRotatedBox3D(bx + rx * (bw * laneX), by + 0.07f, bz, 0.09f, 0.02f, bd * 0.86f, fx, fz, rx, rz, trimCol, light, pose, focalPx, w, h, scratch)
        }
        drawRotatedBox3D(bx, by + 0.13f, bz + fz * (bd * 0.72f), bw * 0.88f, 0.09f, 0.15f, fx, fz, rx, rz, Color(0xFF5D4037), light, pose, focalPx, w, h, scratch)
    }
}

private fun DrawScope.drawGardenOrFlowerBed3D(
    type: BuildingType,
    profile: BuildingVisualProfile,
    bx: Float,
    by: Float,
    bz: Float,
    bw: Float,
    bd: Float,
    bh: Float,
    fx: Float,
    fz: Float,
    rx: Float,
    rz: Float,
    light: Float,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val woodCol = parseSafeColor(profile.wallColorHex, Color(0xFF7B4F2C))
    val leafCol = parseSafeColor(profile.roofColorHex, Color(0xFF3E8B42))
    val flowerCol = parseSafeColor(profile.trimColorHex, Color(0xFFD63A7B))
    // Garden bed earth/stone border
    drawRotatedBox3D(bx, by + 0.05f, bz, bw, 0.05f, bd, fx, fz, rx, rz, Color(0xFF4F6B35), light, pose, focalPx, w, h, scratch)
    if (type == BuildingType.JARDIM) {
        // Wooden pergola posts & crossbeams + flagstone walkway
        drawRotatedBox3D(bx, by + 0.08f, bz, bw * 0.42f, 0.03f, bd * 0.92f, fx, fz, rx, rz, Color(0xFFADA28D), light, pose, focalPx, w, h, scratch)
        for (sx in listOf(-0.82f, 0.82f)) {
            for (sz in listOf(-0.78f, 0.78f)) {
                drawRotatedBox3D(bx + rx * bw * sx + fx * bd * sz, by + bh * 0.46f, bz + rz * bw * sx + fz * bd * sz, 0.11f, bh * 0.46f, 0.11f, fx, fz, rx, rz, woodCol, light, pose, focalPx, w, h, scratch)
            }
        }
        drawRotatedBox3D(bx, by + bh * 0.92f, bz, bw * 0.92f, 0.08f, bd * 0.88f, fx, fz, rx, rz, woodCol, light, pose, focalPx, w, h, scratch)
    }
    // Lush flowering azalea / rose bushes flanking the bed
    for (sx in listOf(-0.62f, 0f, 0.62f)) {
        val fxOff = bx + rx * (bw * sx)
        val fzOff = bz + rz * (bw * sx)
        drawEllipsoidDome3D(fxOff, by + 0.34f, fzOff, 0.62f, 0.34f, leafCol, light, 10, pose, focalPx, w, h, scratch, rings = 2)
        drawEllipsoidDome3D(fxOff, by + 0.56f, fzOff, 0.44f, 0.22f, flowerCol, light, 8, pose, focalPx, w, h, scratch, rings = 2)
    }
}

private fun DrawScope.drawOrnamentalTreeBuilding3D(
    profile: BuildingVisualProfile,
    bx: Float,
    by: Float,
    bz: Float,
    bh: Float,
    light: Float,
    cylSegs: Int,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    scratch: ViewportScratch3D,
) {
    val trunkCol = parseSafeColor(profile.wallColorHex, Color(0xFF5A3D28))
    val crownCol = parseSafeColor(profile.roofColorHex, Color(0xFFE5B82E))
    val underCol = parseSafeColor(profile.trimColorHex, Color(0xFF2E6D2F))
    val trunkH = (bh * 0.48f).coerceIn(2.0f, 4.2f)
    drawRotatedBox3D(bx + 0.6f, by + 0.02f, bz + 0.6f, 1.5f, 0.01f, 1.5f, 0f, 1f, 1f, 0f, Color.Black.copy(alpha = 0.25f), 1f, pose, focalPx, w, h, scratch)
    drawCylinderY3D(bx, by, by + trunkH, bz, 0.26f, trunkCol, light, cylSegs, pose, focalPx, w, h, scratch)
    drawEllipsoidDome3D(bx, by + trunkH + 0.85f, bz, 2.05f, 1.15f, underCol, light, cylSegs, pose, focalPx, w, h, scratch, rings = 2)
    drawEllipsoidDome3D(bx + 0.2f, by + trunkH + 1.35f, bz - 0.15f, 1.65f, 0.92f, crownCol, light, cylSegs, pose, focalPx, w, h, scratch, rings = 2)
}

internal fun DrawScope.drawConstructionPreviewGhost3D(
    preview: ConstructionPlacementPreview,
    worldState: WorldState,
    pose: CameraPose3D,
    focalPx: Float,
    w: Float,
    h: Float,
    animClockSec: Float,
    scratch: ViewportScratch3D,
) {
    val def = preview.structure
    val gx = preview.x.toFloat()
    val gz = preview.z.toFloat()
    val gy = getTerrainHeightAt(worldState, preview.x, preview.z).toFloat()
    val bw = def.widthM.toFloat().coerceIn(0.5f, 16f) * 0.5f
    val bd = def.depthM.toFloat().coerceIn(0.5f, 14f) * 0.5f
    val bh = def.heightM.toFloat().coerceIn(0.3f, 12f)
    val rad = preview.rotationDeg.toFloat() * (PI / 180.0).toFloat()
    val fx = sin(rad)
    val fz = cos(rad)
    val rx = cos(rad)
    val rz = -sin(rad)

    val pulse = 0.78f + 0.22f * sin(animClockSec * 4.5f)
    val canBuild = preview.isValid && preview.canAfford
    val tint = if (canBuild) Color(0xFF10B981) else Color(0xFFEF4444)
    val fillCol = tint.copy(alpha = 0.36f * pulse)
    val roofCol = tint.copy(alpha = 0.50f * pulse)
    val lineCol = if (canBuild) Color(0xFF34D399) else Color(0xFFF87171)

    // Footprint base corners (counter-clockwise)
    val x0 = gx - rx * bw - fx * bd
    val z0 = gz - rz * bw - fz * bd
    val x1 = gx + rx * bw - fx * bd
    val z1 = gz + rz * bw - fz * bd
    val x2 = gx + rx * bw + fx * bd
    val z2 = gz + rz * bw + fz * bd
    val x3 = gx - rx * bw + fx * bd
    val z3 = gz - rz * bw + fz * bd
    val padY = gy + 0.08f

    // Translucent ground footprint pad
    drawProjectedQuad(x0, padY, z0, x1, padY, z1, x2, padY, z2, x3, padY, z3, fillCol, pose, focalPx, w, h, scratch)

    // Crisp perimeter wireframe lines
    drawProjectedLine(x0, padY + 0.05f, z0, x1, padY + 0.05f, z1, lineCol, 4.0f, pose, focalPx, w, h)
    drawProjectedLine(x1, padY + 0.05f, z1, x2, padY + 0.05f, z2, lineCol, 4.0f, pose, focalPx, w, h)
    drawProjectedLine(x2, padY + 0.05f, z2, x3, padY + 0.05f, z3, lineCol, 4.0f, pose, focalPx, w, h)
    drawProjectedLine(x3, padY + 0.05f, z3, x0, padY + 0.05f, z0, lineCol, 4.0f, pose, focalPx, w, h)

    // Front entry direction arrow on the -fx/-fz side (matches house veranda / open shed door)
    val doorMidX = gx - fx * (bd + 1.1f)
    val doorMidZ = gz - fz * (bd + 1.1f)
    val doorTipX = gx - fx * (bd + 2.4f)
    val doorTipZ = gz - fz * (bd + 2.4f)
    drawProjectedLine(doorMidX, padY + 0.15f, doorMidZ, doorTipX, padY + 0.15f, doorTipZ, Color(0xFFFACC15), 4.2f, pose, focalPx, w, h)
    drawProjectedLine(doorTipX, padY + 0.15f, doorTipZ, doorMidX - rx * 0.55f, padY + 0.15f, doorMidZ - rz * 0.55f, Color(0xFFFACC15), 3.6f, pose, focalPx, w, h)
    drawProjectedLine(doorTipX, padY + 0.15f, doorTipZ, doorMidX + rx * 0.55f, padY + 0.15f, doorMidZ + rz * 0.55f, Color(0xFFFACC15), 3.6f, pose, focalPx, w, h)

    // Translucent 3D massing volume + roof silhouette
    if (bh > 0.35f) {
        val bodyH = (bh * 0.65f).coerceAtLeast(0.4f)
        drawRotatedBox3D(gx, gy + bodyH * 0.5f, gz, bw * 0.96f, bodyH * 0.5f, bd * 0.96f, fx, fz, rx, rz, fillCol, 1.0f, pose, focalPx, w, h, scratch)
        drawTiltedBox3D(gx, gy + bodyH + (bh * 0.18f), gz, bw * 1.04f, bh * 0.18f, bd * 1.04f, fx, fz, rx, rz, 0f, 0f, roofCol, 1.0f, pose, focalPx, w, h, scratch, taperTopFactor = 0.55f)
    }

    // Extra reference: if catalog structure has a BUILDING_META fallback label, keep zero warnings
    BUILDING_META[def.buildingType]
}
