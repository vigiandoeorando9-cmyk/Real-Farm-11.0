package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.domain.CameraMode
import com.example.domain.ESTABLISHMENTS
import com.example.domain.GameSettings
import com.example.domain.LANDMARKS
import com.example.domain.SoilState
import com.example.domain.WEATHER_META
import com.example.domain.WeatherType
import com.example.domain.WorldPropType
import com.example.domain.WorldState
import com.example.domain.clampGraphicsConfigForDevice
import com.example.domain.getActiveGraphicsConfig
import com.example.domain.getMapById
import com.example.domain.getVehicleDef
import com.example.firebase.RoomMemberDoc
import com.example.simulation.ConstructionPlacementPreview
import com.example.simulation.getTerrainHeightAt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

@Composable
fun GameplayViewport(
    worldState: WorldState,
    cameraMode: CameraMode,
    remoteMembers: List<RoomMemberDoc>,
    localUid: String?,
    cameraDistanceFactor: Float = 1.0f,
    settings: GameSettings = GameSettings(),
    constructionPreview: ConstructionPlacementPreview? = null,
    onMoveConstructionToWorld: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var panOffsetX by remember(worldState.mapId) { mutableFloatStateOf(0f) }
    var panOffsetZ by remember(worldState.mapId) { mutableFloatStateOf(0f) }
    var orbitYawDeg by remember(cameraMode) { mutableFloatStateOf(0f) }
    var orbitPitchDeg by remember(cameraMode) { mutableFloatStateOf(0f) }
    var pinchZoom by remember { mutableFloatStateOf(1.0f) }
    var lastFrameNanos by remember { mutableLongStateOf(0L) }
    var animClockSec by remember { mutableFloatStateOf(0f) }

    val scratch = remember { ViewportScratch3D() }
    val particlePool = remember { ParticlePool3D(capacity = 180) }

    val mapDef = remember(worldState.mapId) { getMapById(worldState.mapId) }
    val gfx = remember(settings) { clampGraphicsConfigForDevice(getActiveGraphicsConfig(settings)) }

    // Advance particles & ambient clock strictly with simulation state ticks (no infinite coroutine blocking UI tests)
    LaunchedEffect(worldState) {
        val now = System.nanoTime()
        val dtSec = if (lastFrameNanos == 0L) {
            0.033f
        } else {
            ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0.008f, 0.08f)
        }
        lastFrameNanos = now
        animClockSec += dtSec
        particlePool.stepAndEmit(worldState, dtSec, gfx)
    }

    val pose = computeCameraPose3D(
        worldState = worldState,
        cameraMode = cameraMode,
        cameraDistanceFactor = cameraDistanceFactor,
        fovDeg = settings.cameraFov.toFloat(),
        orbitYawDeg = orbitYawDeg,
        orbitPitchDeg = orbitPitchDeg,
        pinchZoom = pinchZoom,
        panOffsetX = panOffsetX,
        panOffsetZ = panOffsetZ,
    )

    var viewportSize by remember { androidx.compose.runtime.mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(cameraMode, constructionPreview != null) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val activePreview = constructionPreview
                        val moveCallback = onMoveConstructionToWorld
                        if (activePreview != null && moveCallback != null && viewportSize.width > 8 && viewportSize.height > 8) {
                            val vw = viewportSize.width.toFloat()
                            val vh = viewportSize.height.toFloat()
                            val halfFovR = (pose.fovDeg * 0.5f) * (PI / 180.0).toFloat()
                            val focal = (vh * 0.5f) / tan(halfFovR).coerceAtLeast(0.25f)
                            val camRayX = (tapOffset.x - vw * 0.5f) / focal
                            val camRayY = (vh * 0.5f - tapOffset.y) / focal
                            val dirX = camRayX * pose.rightX + camRayY * pose.upX + pose.fwdX
                            val dirY = camRayY * pose.upY + pose.fwdY
                            val dirZ = camRayX * pose.rightZ + camRayY * pose.upZ + pose.fwdZ
                            if (dirY < -0.04f) {
                                val tHit = (activePreview.groundY.toFloat() - pose.camY) / dirY
                                if (tHit in 2.5f..140f) {
                                    moveCallback((pose.camX + dirX * tHit).toDouble(), (pose.camZ + dirZ * tHit).toDouble())
                                }
                            }
                        }
                    },
                    onDoubleTap = {
                        orbitYawDeg = 0f
                        orbitPitchDeg = 0f
                        pinchZoom = 1.0f
                        panOffsetX = 0f
                        panOffsetZ = 0f
                    }
                )
            }
            .pointerInput(cameraMode, settings.cameraSensitivity) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val sens = settings.cameraSensitivity.toFloat().coerceIn(0.4f, 2.2f)
                    if (abs(zoom - 1f) > 0.002f) {
                        pinchZoom = (pinchZoom * zoom).coerceIn(0.55f, 2.2f)
                    }
                    if (cameraMode == CameraMode.LIVRE) {
                        val yawR = pose.yawDeg * (PI / 180.0).toFloat()
                        val rightX = cos(yawR)
                        val rightZ = -sin(yawR)
                        val fwdX = sin(yawR)
                        val fwdZ = cos(yawR)
                        panOffsetX = (panOffsetX - (pan.x * rightX - pan.y * fwdX) * 0.06f * sens).coerceIn(-220f, 220f)
                        panOffsetZ = (panOffsetZ - (pan.x * rightZ - pan.y * fwdZ) * 0.06f * sens).coerceIn(-220f, 220f)
                    } else {
                        orbitYawDeg = ((orbitYawDeg + pan.x * 0.22f * sens) % 360f)
                        orbitPitchDeg = (orbitPitchDeg + pan.y * 0.14f * sens).coerceIn(-8f, 26f)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w < 8f || h < 8f) return@Canvas

            val halfFovRad = (pose.fovDeg * 0.5f) * (PI / 180.0).toFloat()
            val focalPx = (h * 0.5f) / tan(halfFovRad).coerceAtLeast(0.25f)

            val hour = worldState.hourOfDay.toFloat()
            val weatherMeta = WEATHER_META[worldState.weather] ?: WEATHER_META[WeatherType.ENSOLARADO]!!
            val cloudCover = weatherMeta.cloudCover.toFloat()
            val rainInt = weatherMeta.rainIntensity.toFloat()

            val horizonScreenY = (h * 0.5f + tan(pose.pitchDeg * (PI / 180.0).toFloat()) * focalPx)
                .coerceIn(h * 0.08f, h * 0.88f)

            // 1. Dynamic 3D Sky & Sun/Moon Dome
            drawSkyAndAtmosphere(
                w = w,
                h = h,
                horizonY = horizonScreenY,
                hour = hour,
                cloudCover = cloudCover,
                pose = pose,
                focalPx = focalPx,
                mapDef = mapDef,
                gfx = gfx,
                animClockSec = animClockSec,
            )

            // 2. 3D Terrain Grid with slope sun-shading, road stripes, soil furrows, and multi-stage crops
            val maxDrawM = min(gfx.drawDistance.toFloat(), 245f).coerceIn(110f, 245f)
            val isNight = hour < 5.6f || hour > 19.6f
            val twilightDarken = when {
                hour < 5.5f || hour > 20.5f -> 0.42f
                hour < 7.0f -> 0.42f + ((hour - 5.5f) / 1.5f) * 0.58f
                hour > 18.0f -> 1.0f - ((hour - 18.0f) / 2.5f) * 0.58f
                else -> 1.0f
            }.coerceIn(0.38f, 1.0f) * (1f - cloudCover * 0.22f)

            drawDetailedTerrain(
                worldState = worldState,
                mapDef = mapDef,
                gfx = gfx,
                cameraMode = cameraMode,
                pose = pose,
                focalPx = focalPx,
                w = w,
                h = h,
                maxDrawM = maxDrawM,
                isNight = isNight,
                twilightDarken = twilightDarken,
                rainIntensity = rainInt,
                animClockSec = animClockSec,
                scratch = scratch,
            )

            // 3. Water puddles, recent tire tracks & GPS AB line
            for (p in (worldState.puddles ?: emptyList())) {
                val py = getTerrainHeightAt(worldState, p.x, p.z).toFloat() + 0.03f
                val r = p.radius.toFloat()
                drawProjectedQuad(
                    (p.x - r).toFloat(), py, (p.z - r * 0.7f).toFloat(),
                    (p.x + r).toFloat(), py, (p.z - r * 0.7f).toFloat(),
                    (p.x + r).toFloat(), py, (p.z + r * 0.7f).toFloat(),
                    (p.x - r).toFloat(), py, (p.z + r * 0.7f).toFloat(),
                    Color(0xFF285F78).copy(alpha = 0.50f),
                    pose, focalPx, w, h, scratch,
                )
            }

            for (t in worldState.tireTracks.takeLast(80)) {
                val dx = t.x.toFloat() - pose.camX
                val dz = t.z.toFloat() - pose.camZ
                if (dx * dx + dz * dz > 58f * 58f) continue
                val ty = getTerrainHeightAt(worldState, t.x, t.z).toFloat() + 0.03f
                val rad = t.headingDeg.toFloat() * (PI / 180.0).toFloat()
                val rx = cos(rad) * (t.trackWidthM.toFloat() * 0.44f)
                val rz = -sin(rad) * (t.trackWidthM.toFloat() * 0.44f)
                val fx = sin(rad) * 0.42f
                val fz = cos(rad) * 0.42f
                val alpha = (1.0 - (t.age / 25.0)).toFloat().coerceIn(0.08f, 0.45f)
                val tc = Color(0xFF281910).copy(alpha = alpha)
                drawProjectedLine((t.x - rx - fx).toFloat(), ty, (t.z - rz - fz).toFloat(), (t.x - rx + fx).toFloat(), ty, (t.z - rz + fz).toFloat(), tc, 3.2f, pose, focalPx, w, h)
                drawProjectedLine((t.x + rx - fx).toFloat(), ty, (t.z + rz - fz).toFloat(), (t.x + rx + fx).toFloat(), ty, (t.z + rz + fz).toFloat(), tc, 3.2f, pose, focalPx, w, h)
            }

            val activeVeh = worldState.activeVehicleUid?.let { uid -> worldState.vehicles.find { it.uid == uid } }
            activeVeh?.gpsPointA?.let { ptA ->
                val yA = getTerrainHeightAt(worldState, ptA.x, ptA.z).toFloat() + 0.25f
                val pA = projectRaw(ptA.x.toFloat(), yA, ptA.z.toFloat(), pose, focalPx, w, h)
                if (pA.visible) drawCircle(Color(0xFF00E676), 7f, Offset(pA.screenX, pA.screenY))
                activeVeh.gpsPointB?.let { ptB ->
                    val yB = getTerrainHeightAt(worldState, ptB.x, ptB.z).toFloat() + 0.25f
                    drawProjectedLine(ptA.x.toFloat(), yA, ptA.z.toFloat(), ptB.x.toFloat(), yB, ptB.z.toFloat(), Color(0xFF00E676).copy(alpha = 0.72f), 3.0f, pose, focalPx, w, h)
                    val pB = projectRaw(ptB.x.toFloat(), yB, ptB.z.toFloat(), pose, focalPx, w, h)
                    if (pB.visible) drawCircle(Color(0xFFFF5252), 7f, Offset(pB.screenX, pB.screenY))
                }
            }

            // 4. Collect & Depth-Sort All 3D Entities
            val items = scratch.items
            items.clear()
            val objCullSq = min(maxDrawM, 195f) * min(maxDrawM, 195f)
            val detailLevel = gfx.vehicleDetailLevel

            fun addIfInView(x: Double, z: Double, builder: (depth: Float, gy: Double) -> RenderItem3D) {
                val dx = x.toFloat() - pose.camX
                val dz = z.toFloat() - pose.camZ
                if (dx * dx + dz * dz > objCullSq) return
                val gy = getTerrainHeightAt(worldState, x, z)
                val depth = dx * pose.fwdX + (gy.toFloat() - pose.camY) * pose.fwdY + dz * pose.fwdZ
                if (depth > 0.3f) items.add(builder(depth, gy))
            }

            for (b in worldState.buildings) {
                addIfInView(b.x, b.z) { d, gy ->
                    RenderItem3D(
                        ItemKind3D.BUILDING, d, b.x, gy, b.z,
                        building = b,
                        buildingType = b.type,
                        buildingRotDeg = b.rotationDeg,
                        buildingGateOpen = b.isGateOpen,
                    )
                }
            }
            for (pr in worldState.props) {
                val isTree = pr.type == WorldPropType.TREE ||
                    pr.type == WorldPropType.TREE_PINE ||
                    pr.type == WorldPropType.TREE_EUCALYPTUS ||
                    pr.type == WorldPropType.TREE_FRUIT ||
                    pr.type == WorldPropType.TREE_NATIVE ||
                    pr.type == WorldPropType.TREE_ARAUCARIA ||
                    pr.type == WorldPropType.TREE_CERRADO ||
                    pr.type == WorldPropType.TREE_PALM_AMAZON ||
                    pr.type == WorldPropType.TREE_IPE_AMARELO ||
                    pr.type == WorldPropType.TREE_IPE_ROXO ||
                    pr.type == WorldPropType.TREE_SHADE_MANGUEIRA
                addIfInView(pr.x, pr.z) { d, gy ->
                    RenderItem3D(if (isTree) ItemKind3D.TREE else ItemKind3D.PROP, d, pr.x, gy, pr.z, prop = pr)
                }
            }
            for (imp in worldState.implements) {
                addIfInView(imp.x, imp.z) { d, gy ->
                    RenderItem3D(ItemKind3D.IMPLEMENT, d, imp.x, gy, imp.z, implement = imp)
                }
            }
            for (v in worldState.vehicles) {
                addIfInView(v.x, v.z) { d, gy ->
                    RenderItem3D(ItemKind3D.VEHICLE, d, v.x, gy, v.z, vehicle = v)
                }
            }
            for (rv in (worldState.roadVehicles ?: emptyList())) {
                addIfInView(rv.x, rv.z) { d, gy ->
                    RenderItem3D(ItemKind3D.ROAD_VEH, d, rv.x, gy, rv.z, roadVehicle = rv)
                }
            }
            for (ctr in (worldState.outsourcingContracts ?: emptyList())) {
                if (ctr.status == "EM_OPERACAO" || ctr.status == "PAUSADO_IMPREVISTO") {
                    addIfInView(ctr.vehicleX, ctr.vehicleZ) { d, gy ->
                        RenderItem3D(ItemKind3D.CONTRACTOR, d, ctr.vehicleX, gy, ctr.vehicleZ, headingDeg = ctr.vehicleHeadingDeg)
                    }
                }
            }
            for (npc in (worldState.npcAgents ?: emptyList())) {
                addIfInView(npc.x, npc.z) { d, gy ->
                    RenderItem3D(
                        ItemKind3D.NPC, d, npc.x, gy, npc.z,
                        labelColor = parseSafeColor(npc.shirtHex, Color(0xFF2196F3)),
                        altColor = parseSafeColor(npc.skinHex, Color(0xFFD9A17A)),
                        headingDeg = npc.headingDeg,
                    )
                }
            }
            for (m in remoteMembers) {
                if (m.memberUid == localUid || !m.isApproved) continue
                addIfInView(m.x, m.z) { d, gy ->
                    RenderItem3D(ItemKind3D.TEAMMATE, d, m.x, gy, m.z, labelColor = Color(0xFF29B6F6), headingDeg = m.headingDeg)
                }
            }
            if (worldState.activeVehicleUid == null) {
                val p = worldState.player
                addIfInView(p.x, p.z) { d, gy ->
                    RenderItem3D(
                        ItemKind3D.PLAYER, d, p.x, gy, p.z,
                        labelColor = parseSafeColor(p.appearance?.shirtColorHex, Color(0xFF1E2124)),
                        altColor = parseSafeColor(p.appearance?.hatColorHex, Color(0xFF3A2A20)),
                        headingDeg = p.headingDeg,
                        playerAppearance = p.appearance,
                    )
                }
            }

            items.sortByDescending { it.depth }

            for (item in items) {
                when (item.kind) {
                    ItemKind3D.BUILDING -> drawDetailedBuilding3D(item, worldState, pose, focalPx, w, h, twilightDarken, isNight, detailLevel, scratch)
                    ItemKind3D.TREE -> drawTree3D(item, pose, focalPx, w, h, twilightDarken, animClockSec, detailLevel, scratch)
                    ItemKind3D.PROP -> drawProp3D(item, pose, focalPx, w, h, twilightDarken, scratch)
                    ItemKind3D.IMPLEMENT -> item.implement?.let { drawImplement3D(it, item.y.toFloat(), gfx, pose, focalPx, w, h, twilightDarken, scratch) }
                    ItemKind3D.VEHICLE -> item.vehicle?.let {
                        val isCockpit = cameraMode == CameraMode.CABINE && it.uid == worldState.activeVehicleUid
                        val driverAppearance = if (it.uid == worldState.activeVehicleUid) worldState.player.appearance else null
                        drawVehicle3D(it, item.y.toFloat(), isCockpit, isNight, gfx, animClockSec, pose, focalPx, w, h, twilightDarken, scratch, driverAppearance)
                    }
                    ItemKind3D.ROAD_VEH -> item.roadVehicle?.let { rv ->
                        drawSimpleBoxVehicle3D(rv.x.toFloat(), item.y.toFloat(), rv.z.toFloat(), rv.headingDeg.toFloat(), 2.1f, 1.9f, 4.4f, parseSafeColor(rv.colorHex, Color(0xFF607D8B)), twilightDarken, pose, focalPx, w, h, scratch)
                    }
                    ItemKind3D.CONTRACTOR -> {
                        drawSimpleBoxVehicle3D(item.x.toFloat(), item.y.toFloat(), item.z.toFloat(), item.headingDeg.toFloat(), 2.5f, 2.8f, 4.6f, Color(0xFFF59E0B), twilightDarken, pose, focalPx, w, h, scratch)
                    }
                    ItemKind3D.PLAYER, ItemKind3D.NPC, ItemKind3D.TEAMMATE -> {
                        drawCharacter3D(item, pose, focalPx, w, h, twilightDarken, animClockSec, scratch)
                    }
                }
            }

            // 4b. Translucent 3D Ghost Preview for Property Construction Mode
            constructionPreview?.let { preview ->
                drawConstructionPreviewGhost3D(
                    preview = preview,
                    worldState = worldState,
                    pose = pose,
                    focalPx = focalPx,
                    w = w,
                    h = h,
                    animClockSec = animClockSec,
                    scratch = scratch,
                )
            }

            // 5. Real-time Particles (Exhaust Smoke, Soil Dust, Mud Splash, Spray Mist, Grain Stream)
            particlePool.render(this, pose, focalPx, w, h)

            // 6. Cab Interior Frame & Dashboard Gauge Bezel Overlay in CABINE mode
            if (cameraMode == CameraMode.CABINE && activeVeh != null) {
                drawRect(Color(0xFF111613).copy(alpha = 0.55f), topLeft = Offset(0f, 0f), size = Size(w * 0.042f, h))
                drawRect(Color(0xFF111613).copy(alpha = 0.55f), topLeft = Offset(w * 0.958f, 0f), size = Size(w * 0.042f, h))
                drawRect(Color(0xFF111613).copy(alpha = 0.65f), topLeft = Offset(0f, 0f), size = Size(w, h * 0.055f))
                if (settings.hudLayout?.showDashGauges != false) {
                    val vDef = getVehicleDef(activeVeh.defId)
                    val dashW = w * 0.34f
                    val dashH = (h * 0.105f).coerceIn(36f, 68f)
                    val dashLeft = (w - dashW) * 0.5f
                    val dashTop = h - dashH - h * 0.025f
                    drawRect(
                        color = Color(0xFF141A17).copy(alpha = 0.88f),
                        topLeft = Offset(dashLeft, dashTop),
                        size = Size(dashW, dashH),
                    )
                    // RPM bar
                    val rpmFrac = ((activeVeh.rpm - 700.0) / 1700.0).toFloat().coerceIn(0.05f, 1f)
                    drawRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(dashLeft + dashW * 0.06f, dashTop + dashH * 0.28f),
                        size = Size(dashW * 0.38f, dashH * 0.22f),
                    )
                    drawRect(
                        color = if (rpmFrac > 0.85f) Color(0xFFFF7043) else Color(0xFF10B981),
                        topLeft = Offset(dashLeft + dashW * 0.06f, dashTop + dashH * 0.28f),
                        size = Size(dashW * 0.38f * rpmFrac, dashH * 0.22f),
                    )
                    // Fuel bar
                    val fuelFrac = (activeVeh.fuelL / max(vDef.fuelCapacityL, 1.0)).toFloat().coerceIn(0f, 1f)
                    drawRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(dashLeft + dashW * 0.56f, dashTop + dashH * 0.28f),
                        size = Size(dashW * 0.38f, dashH * 0.22f),
                    )
                    drawRect(
                        color = if (fuelFrac < 0.20f) Color(0xFFEF5350) else Color(0xFFFBC02D),
                        topLeft = Offset(dashLeft + dashW * 0.56f, dashTop + dashH * 0.28f),
                        size = Size(dashW * 0.38f * fuelFrac, dashH * 0.22f),
                    )
                    // Steering wheel position indicator tick at bottom of dash
                    val steerTickNorm = (activeVeh.steerAngleDeg / 38.0).toFloat().coerceIn(-1f, 1f)
                    drawRect(
                        color = Color(0xFF37474F),
                        topLeft = Offset(dashLeft + dashW * 0.25f, dashTop + dashH * 0.68f),
                        size = Size(dashW * 0.50f, dashH * 0.14f),
                    )
                    drawCircle(
                        color = Color(0xFF52B788),
                        radius = dashH * 0.12f,
                        center = Offset(dashLeft + dashW * (0.50f + steerTickNorm * 0.22f), dashTop + dashH * 0.75f),
                    )
                }
            }

            // 7. Rain Streaks & Fog Atmospheric Pass
            if (rainInt > 0.1f) {
                drawRect(color = Color(0xFF162430).copy(alpha = 0.22f * rainInt))
                val drops = (rainInt * 65 * gfx.weatherEffectsDensity.toFloat().coerceIn(0.4f, 1.3f)).toInt()
                val phase = ((worldState.hourOfDay * 6000.0) % 1000.0).toInt()
                for (d in 0 until drops) {
                    val rx = ((d * 97 + phase * 3) % w.toInt().coerceAtLeast(1)).toFloat()
                    val ry = ((d * 173 + phase * 9) % h.toInt().coerceAtLeast(1)).toFloat()
                    drawLine(
                        color = Color(0xFFBBDEFB).copy(alpha = 0.42f),
                        start = Offset(rx, ry),
                        end = Offset(rx - 5f, ry + 20f),
                        strokeWidth = 1.6f,
                    )
                }
            }
            if (worldState.weather == WeatherType.NEBLINA) {
                drawRect(color = Color(0xFFD0DAE0).copy(alpha = 0.22f))
            }
        }
    }
}

@Composable
fun MinimapRadar(
    worldState: WorldState,
    modifier: Modifier = Modifier,
) {
    val gridSide = worldState.mapGridSize ?: 128
    val cellSizeM = com.example.simulation.getCellSize(gridSide).toFloat()
    val worldSpanM = gridSide * cellSizeM
    val halfM = worldSpanM / 2f

    Box(
        modifier = modifier
            .size(104.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0E1310).copy(alpha = 0.88f))
            .border(1.5.dp, Color(0xFF52B788).copy(alpha = 0.65f), RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun mx(wx: Double): Float = (((wx.toFloat() + halfM) / worldSpanM) * w).coerceIn(2f, w - 2f)
            fun mz(wz: Double): Float = (((wz.toFloat() + halfM) / worldSpanM) * h).coerceIn(2f, h - 2f)

            val step = max(2, gridSide / 32)
            val cw = (step.toFloat() / gridSide) * w + 1f
            for (gz in 0 until gridSide step step) {
                for (gx in 0 until gridSide step step) {
                    val idx = gz * gridSide + gx
                    if (idx >= worldState.cells.size) continue
                    val cell = worldState.cells[idx]
                    val c = when (cell.soilState) {
                        SoilState.AGUA_RIO -> Color(0xFF217696)
                        SoilState.ESTRADA_ASFALTO, SoilState.ESTRADA_TERRA -> Color(0xFF6B655B)
                        SoilState.PRONTO_COLHEITA -> Color(0xFFE9A825)
                        SoilState.CRESCENDO, SoilState.SEMEADO -> Color(0xFF4CAF50)
                        SoilState.ARADO, SoilState.SUBSOLADO -> Color(0xFF7F3E28)
                        SoilState.AREA_PRESERVACAO, SoilState.FLORESTA_NATIVA -> Color(0xFF153F1F)
                        else -> Color(0xFF244329)
                    }
                    drawRect(
                        color = c,
                        topLeft = Offset((gx.toFloat() / gridSide) * w, (gz.toFloat() / gridSide) * h),
                        size = Size(cw, cw),
                    )
                }
            }

            for (lm in LANDMARKS) {
                drawCircle(
                    color = Color(0xFF52B788),
                    radius = 2.8f,
                    center = Offset(mx(lm.scaledWorldX(gridSide)), mz(lm.scaledWorldZ(gridSide))),
                )
            }
            for (est in ESTABLISHMENTS) {
                drawCircle(
                    color = Color(0xFF29B6F6),
                    radius = 3.2f,
                    center = Offset(mx(est.scaledWorldX(gridSide)), mz(est.scaledWorldZ(gridSide))),
                )
            }
            for (v in worldState.vehicles) {
                val isAct = v.uid == worldState.activeVehicleUid
                drawCircle(
                    color = if (isAct) Color(0xFFFFD54F) else Color.White,
                    radius = if (isAct) 4.2f else 2.5f,
                    center = Offset(mx(v.x), mz(v.z)),
                )
            }
            if (worldState.activeVehicleUid == null) {
                drawCircle(
                    color = Color(0xFF52B788),
                    radius = 4.2f,
                    center = Offset(mx(worldState.player.x), mz(worldState.player.z)),
                )
            }
        }
    }
}
