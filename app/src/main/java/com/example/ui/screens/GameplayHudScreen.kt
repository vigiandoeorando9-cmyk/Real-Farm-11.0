package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CameraMode
import com.example.domain.GameSettings
import com.example.domain.SEASON_META
import com.example.domain.SeasonType
import com.example.domain.SteeringControlStyle
import com.example.domain.WEATHER_META
import com.example.domain.WeatherType
import com.example.domain.WorldState
import com.example.domain.getSeasonFromMonth
import com.example.domain.getVehicleDef
import com.example.firebase.RoomMemberDoc
import com.example.simulation.ControlInput
import com.example.simulation.EMPTY_CONTROL_INPUT
import com.example.simulation.formatBrl
import com.example.state.FarmViewModel
import com.example.state.InGameTab
import com.example.state.resolveContextualInteraction
import com.example.ui.components.AnalogJoystick
import com.example.ui.components.GameplayViewport
import com.example.ui.components.MinimapRadar
import com.example.ui.components.VirtualSteeringWheel
import com.example.ui.theme.AgroSprout
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusWarning
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GameplayHudScreen(
    viewModel: FarmViewModel,
    worldState: WorldState,
    settings: GameSettings,
    cameraMode: CameraMode,
    timeScale: Double,
    activeTab: InGameTab?,
    showFullMap: Boolean,
    hudHidden: Boolean,
    roomMembers: List<RoomMemberDoc>,
    localUid: String?,
) {
    var inputState by remember { mutableStateOf(EMPTY_CONTROL_INPUT) }
    var leftSteerPressed by remember { mutableStateOf(false) }
    var rightSteerPressed by remember { mutableStateOf(false) }

    fun pushInput(updater: (ControlInput) -> ControlInput) {
        val next = updater(inputState)
        inputState = next
        viewModel.updateTouchInput(next)
    }

    fun updateSteerButtons(left: Boolean = leftSteerPressed, right: Boolean = rightSteerPressed) {
        leftSteerPressed = left
        rightSteerPressed = right
        val steerAxis = (if (right) 1.0 else 0.0) - (if (left) 1.0 else 0.0)
        pushInput { it.copy(moveX = steerAxis) }
    }

    LaunchedEffect(worldState.activeVehicleUid) {
        leftSteerPressed = false
        rightSteerPressed = false
        inputState = EMPTY_CONTROL_INPUT
        viewModel.updateTouchInput(EMPTY_CONTROL_INPUT)
    }

    LaunchedEffect(worldState.statusAlert) {
        if (worldState.statusAlert != null) {
            delay(4500L)
            viewModel.clearStatusAlert()
        }
    }

    val activeVehicle = remember(worldState.activeVehicleUid, worldState.vehicles) {
        worldState.activeVehicleUid?.let { uid -> worldState.vehicles.find { it.uid == uid } }
    }
    val contextualAction = remember(
        worldState.activeVehicleUid,
        worldState.vehicles,
        worldState.implements,
        worldState.player.x,
        worldState.player.z,
        worldState.player.tool,
        worldState.player.carryingPropUid,
        worldState.buildings,
        worldState.props,
        worldState.npcAgents,
        worldState.mapGridSize,
    ) {
        resolveContextualInteraction(worldState)
    }

    val seasonMeta = SEASON_META[getSeasonFromMonth(worldState.month)]
        ?: SEASON_META[SeasonType.PRIMAVERA]!!
    val weatherMeta = WEATHER_META[worldState.weather]
        ?: WEATHER_META[WeatherType.ENSOLARADO]!!
    val layoutCfg = settings.hudLayout
    val isLeftHanded = layoutCfg?.preset == "CANHOTO"
    val hudScale = (layoutCfg?.scale ?: 1.0).toFloat().coerceIn(0.75f, 1.35f)
    val verticalOffsetDp = ((layoutCfg?.controlOffsetYDp ?: 0.0).toFloat().coerceIn(-64f, 64f)).dp
    val controlAlpha = settings.controlOpacity.toFloat().coerceIn(0.45f, 1.0f)

    val constructionPreview by viewModel.constructionPreview.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Real-time 3D Perspective Viewport
        GameplayViewport(
            worldState = worldState,
            cameraMode = cameraMode,
            remoteMembers = roomMembers,
            localUid = localUid,
            cameraDistanceFactor = settings.cameraDistance.toFloat(),
            settings = settings,
            constructionPreview = constructionPreview,
            onMoveConstructionToWorld = { wx, wz -> viewModel.setConstructionPreviewPosition(wx, wz) },
            modifier = Modifier.fillMaxSize(),
        )

        // Clean Farming Simulator-inspired HUD: max 3 general buttons (MENU, MINIMAPA, INTERACAO)
        if (!hudHidden) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // GENERAL BUTTON 1: MENU
                Button(
                    onClick = { viewModel.openInGameTab(InGameTab.MENU) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = BorderStroke(1.dp, AgroSprout.copy(alpha = 0.65f)),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("hud_menu_button"),
                ) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = HarvestGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("MENU", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                // Compact Non-Interactive Telemetry Status Pill
                if (layoutCfg?.showTelemetryBar != false) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f).testTag("hud_telemetry_bar"),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = worldState.farmName,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(6.dp))
                                val bankBal = worldState.bankBalanceBrl ?: 0.0
                                Text(
                                    text = if (bankBal > 1.0) "${formatBrl(worldState.moneyBrl)} • Banco ${formatBrl(bankBal)}" else formatBrl(worldState.moneyBrl),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = HarvestGold,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                            val h = worldState.hourOfDay.toInt()
                            val m = ((worldState.hourOfDay - h) * 60).roundToInt() % 60
                            val telemetryLine = if (activeVehicle != null) {
                                val def = getVehicleDef(activeVehicle.defId)
                                val kmh = (abs(activeVehicle.speedMs) * 3.6).roundToInt()
                                val gearStr = when {
                                    activeVehicle.gear > 0 -> "F${activeVehicle.gear}"
                                    activeVehicle.gear < 0 -> "R"
                                    else -> "N"
                                }
                                val fuelPct = ((activeVehicle.fuelL / kotlin.math.max(def.fuelCapacityL, 1.0)) * 100.0).roundToInt().coerceIn(0, 100)
                                val hb = if (activeVehicle.handbrakeOn == true) " • P" else ""
                                "${def.model} • $kmh km/h ($gearStr)$hb • Comb ${activeVehicle.fuelL.roundToInt()}L ($fuelPct%) • ${"%02d:%02d".format(h, m)} • ${weatherMeta.displayName}"
                            } else {
                                "Dia ${worldState.day} • ${"%02d:%02d".format(h, m)} • ${seasonMeta.label} • ${weatherMeta.displayName}"
                            }
                            Text(
                                text = telemetryLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = AgroSprout,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // GENERAL BUTTON 2: MINIMAPA
                Box(
                    modifier = Modifier
                        .clickable { viewModel.toggleFullMap(true) }
                        .testTag("hud_minimap_button"),
                ) {
                    MinimapRadar(worldState = worldState)
                    Surface(
                        color = Color(0xFF0E1310).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp),
                    ) {
                        Text(
                            text = "MAPA",
                            style = MaterialTheme.typography.labelSmall,
                            color = HarvestGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // Bottom Layer: Contextual Interaction (General Button #3) + Ergonomic Vehicle Controls
            val leftBlock: @Composable () -> Unit = {
                if (activeVehicle == null) {
                    AnalogJoystick(sizeDp = (110.dp * hudScale).coerceAtLeast(88.dp), opacity = controlAlpha) { jx, jy ->
                        pushInput { it.copy(moveX = jx, moveY = jy) }
                    }
                } else if (settings.steeringStyle == SteeringControlStyle.VOLANTE) {
                    VirtualSteeringWheel(sizeDp = (118.dp * hudScale).coerceAtLeast(94.dp), opacity = controlAlpha) { steer ->
                        pushInput { it.copy(moveX = steer) }
                    }
                } else {
                    val btnWidth = (58.dp * hudScale).coerceAtLeast(52.dp)
                    val btnHeight = (58.dp * hudScale).coerceAtLeast(52.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HoldPadButton(
                            label = "ESQ",
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = controlAlpha),
                            widthDp = btnWidth,
                            heightDp = btnHeight,
                            testTag = "steer_left_btn",
                            onPressedChange = { down -> updateSteerButtons(left = down) },
                        )
                        HoldPadButton(
                            label = "DIR",
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = controlAlpha),
                            widthDp = btnWidth,
                            heightDp = btnHeight,
                            testTag = "steer_right_btn",
                            onPressedChange = { down -> updateSteerButtons(right = down) },
                        )
                    }
                }
            }

            val rightBlock: @Composable () -> Unit = {
                if (activeVehicle != null) {
                    val engineOn = activeVehicle.engineRunning == true
                    val handbrakeOn = activeVehicle.handbrakeOn == true
                    val cruiseOn = activeVehicle.cruiseControlActive == true
                    val cruiseTargetKmh = (
                        activeVehicle.cruiseTargetKmh
                            ?: (getVehicleDef(activeVehicle.defId).maxSpeedKmh * 0.55).coerceIn(6.0, 45.0)
                        ).roundToInt()
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Cruise Control Strip (- | CRUZ km/h | +)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                onClick = { viewModel.adjustCruiseSpeed(-2.0) },
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f * controlAlpha),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("cruise_minus_btn"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                onClick = { viewModel.toggleCruiseControl() },
                                color = (if (cruiseOn) HarvestGold else MaterialTheme.colorScheme.surface).copy(alpha = 0.92f * controlAlpha),
                                border = BorderStroke(
                                    width = if (cruiseOn) 1.5.dp else 1.dp,
                                    color = if (cruiseOn) Color.White else MaterialTheme.colorScheme.outline,
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("cruise_toggle_btn"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Speed,
                                        contentDescription = "Velocidade cruzeiro",
                                        tint = if (cruiseOn) Color(0xFF0E1310) else HarvestGold,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = if (cruiseOn) "CRUZ $cruiseTargetKmh km/h" else "CRUZ $cruiseTargetKmh",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cruiseOn) Color(0xFF0E1310) else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Surface(
                                onClick = { viewModel.adjustCruiseSpeed(+2.0) },
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f * controlAlpha),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("cruise_plus_btn"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Vehicle Ignition Button (Ligar / Desligar)
                            Surface(
                                onClick = { viewModel.toggleEngine() },
                                color = (if (engineOn) StatusGreen else StatusWarning).copy(alpha = 0.90f * controlAlpha),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("vehicle_engine_btn"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Ligar ou desligar motor", tint = Color.White, modifier = Modifier.size(17.dp))
                                    Text(if (engineOn) "ON" else "LIGAR", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Handbrake Button (Freio de mão)
                            Surface(
                                onClick = { viewModel.toggleHandbrake() },
                                color = (if (handbrakeOn) StatusDanger else MaterialTheme.colorScheme.surface).copy(alpha = 0.92f * controlAlpha),
                                border = BorderStroke(1.dp, if (handbrakeOn) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("vehicle_handbrake_btn"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        if (handbrakeOn) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                        contentDescription = "Freio de mão",
                                        tint = if (handbrakeOn) Color.White else HarvestGold,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = if (handbrakeOn) "P ON" else "FREIO MÃO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (handbrakeOn) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        // Pedals: Freio / Ré & Acelerar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            HoldTextPedal(
                                label = "FREIO / RÉ",
                                containerColor = StatusDanger.copy(alpha = 0.88f * controlAlpha),
                                widthDp = (66f * hudScale).roundToInt().coerceAtLeast(56),
                                heightDp = (60f * hudScale).roundToInt().coerceAtLeast(50),
                                testTag = "pedal_brake",
                                onPressedChange = { down -> pushInput { it.copy(brake = if (down) 1.0 else 0.0) } },
                            )
                            HoldTextPedal(
                                label = "ACELERAR",
                                containerColor = StatusGreen.copy(alpha = 0.90f * controlAlpha),
                                widthDp = (68f * hudScale).roundToInt().coerceAtLeast(58),
                                heightDp = (76f * hudScale).roundToInt().coerceAtLeast(60),
                                testTag = "pedal_throttle",
                                onPressedChange = { down -> pushInput { it.copy(throttle = if (down) 1.0 else 0.0) } },
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.size(1.dp))
                }
            }

            if (constructionPreview == null) {
                BoxWithConstraints(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = (10.dp - verticalOffsetDp).coerceAtLeast(2.dp),
                            bottom = (10.dp + verticalOffsetDp).coerceAtLeast(4.dp),
                        ),
                ) {
                val isWideLandscape = maxWidth >= 520.dp
                if (isWideLandscape) {
                    // Wide landscape: steering/pedals at far corners, INTERAÇÃO centered low in the open gap
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        if (isLeftHanded) {
                            rightBlock()
                            leftBlock()
                        } else {
                            leftBlock()
                            rightBlock()
                        }
                    }

                    if (contextualAction != null) {
                        FilledTonalButton(
                            onClick = { viewModel.performContextualInteraction() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = HarvestGold.copy(alpha = 0.94f),
                                contentColor = Color(0xFF0E1310),
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                                .height(48.dp)
                                .testTag("hud_interaction_button"),
                        ) {
                            Icon(Icons.Rounded.TouchApp, contentDescription = contextualAction.label, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(contextualAction.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Narrow layout: stack contextual button cleanly above controls to prevent any overlap
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (contextualAction != null) {
                            FilledTonalButton(
                                onClick = { viewModel.performContextualInteraction() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = HarvestGold.copy(alpha = 0.94f),
                                    contentColor = Color(0xFF0E1310),
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("hud_interaction_button"),
                            ) {
                                Icon(Icons.Rounded.TouchApp, contentDescription = contextualAction.label, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(contextualAction.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            if (isLeftHanded) {
                                rightBlock()
                                leftBlock()
                            } else {
                                leftBlock()
                                rightBlock()
                            }
                        }
                    }
                }
            }
            }
        } else {
            // Minimal HUD restore surface when HUD is intentionally hidden from MENU -> Configuracoes
            Surface(
                onClick = { viewModel.toggleHudHidden() },
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .height(48.dp)
                    .testTag("hud_restore_button"),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text("Mostrar HUD", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Transient Auto-Dismiss Status Alert Banner (non-intrusive)
        worldState.statusAlert?.let { msg ->
            Surface(
                onClick = { viewModel.clearStatusAlert() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 66.dp, start = 24.dp, end = 24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, HarvestGold.copy(alpha = 0.65f)),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Agriculture, contentDescription = null, tint = HarvestGold, modifier = Modifier.size(16.dp))
                    Text(msg, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Interactive 3D Terrain Construction Placement Overlay
        val activePreview = constructionPreview
        if (activePreview != null && activeTab == null && !showFullMap) {
            ConstructionPlacementHudOverlay(
                preview = activePreview,
                moneyBrl = worldState.moneyBrl,
                onMove = { dx, dz -> viewModel.moveConstructionPreview(dx, dz) },
                onRotate = { deltaDeg -> viewModel.rotateConstructionPreview(deltaDeg) },
                onConfirm = { viewModel.confirmConstructionPlacement() },
                onCancel = { viewModel.cancelConstructionPlacement() },
            )
        }

        // Organized Menu / Systems Sheet
        if (activeTab != null) {
            InGamePanelsSheet(
                viewModel = viewModel,
                tab = activeTab,
                worldState = worldState,
                cameraMode = cameraMode,
                timeScale = timeScale,
                hudHidden = hudHidden,
                onDismiss = { viewModel.openInGameTab(null) },
            )
        }

        // Strategic Interactive Map / Minimapa Modal
        if (showFullMap) {
            FullMapScreen(
                worldState = worldState,
                onClose = { viewModel.toggleFullMap(false) },
                onTeleport = { x, z, lbl -> viewModel.teleportToLocation(x, z, lbl) },
                onSwitchVehicle = { uid -> viewModel.switchToVehicle(uid) },
                onBuyPlot = { id -> viewModel.buyPlot(id) },
                onLeasePlot = { id, term -> viewModel.leasePlotWithTerm(id, term) },
                onOpenSystemTab = { tab ->
                    viewModel.toggleFullMap(false)
                    viewModel.openInGameTab(tab)
                },
            )
        }
    }
}

@Composable
private fun HoldPadButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    widthDp: androidx.compose.ui.unit.Dp = 58.dp,
    heightDp: androidx.compose.ui.unit.Dp = 58.dp,
    testTag: String,
    onPressedChange: (Boolean) -> Unit,
) {
    Surface(
        color = color,
        border = BorderStroke(1.5.dp, Color(0xFF52B788).copy(alpha = 0.65f)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(width = widthDp.coerceAtLeast(48.dp), height = heightDp.coerceAtLeast(48.dp))
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressedChange(true)
                    waitForUpOrCancellation()
                    onPressedChange(false)
                }
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun HoldTextPedal(
    label: String,
    containerColor: Color,
    widthDp: Int,
    heightDp: Int,
    testTag: String,
    onPressedChange: (Boolean) -> Unit,
) {
    Surface(
        color = containerColor,
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(width = widthDp.dp, height = heightDp.dp)
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressedChange(true)
                    waitForUpOrCancellation()
                    onPressedChange(false)
                }
            },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
