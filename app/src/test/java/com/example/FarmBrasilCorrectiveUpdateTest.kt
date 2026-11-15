package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.domain.CameraMode
import com.example.domain.GameSettings
import com.example.simulation.ControlInput
import com.example.simulation.createNewWorld
import com.example.simulation.getTerrainHeightAt
import com.example.simulation.stepSimulation
import com.example.simulation.stepVehiclePhysics
import com.example.simulation.toggleEnterExitVehicle
import com.example.simulation.toggleVehicleHandbrakeInWorld
import com.example.state.FarmViewModel
import com.example.state.InGameTab
import com.example.ui.components.computeCameraPose3D
import com.example.ui.components.projectPoint3D
import android.content.pm.ActivityInfo
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertTextContains
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AppRoute
import com.example.domain.SoilState
import com.example.domain.WorldPropType
import com.example.ui.components.SignInScreen
import com.example.ui.screens.FullMapScreen
import com.example.ui.screens.GameplayHudScreen
import com.example.ui.theme.FarmBrasilTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FarmBrasilCorrectiveUpdateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun thirdPersonCamera_tracksBehindVehicleWithHorizonPerspectiveAndNoTopDownPitch() {
        val base = createNewWorld(0, "Fazenda 3D", "map_pr", 250000.0, "Padrão", 64)
        val veh = base.vehicles.first()
        val world = base.copy(
            activeVehicleUid = veh.uid,
            vehicles = base.vehicles.map {
                if (it.uid == veh.uid) it.copy(headingDeg = 45.0) else it
            },
        )

        val pose = computeCameraPose3D(world, CameraMode.TERCEIRA_PESSOA)
        val groundY = getTerrainHeightAt(world, veh.x, veh.z).toFloat()

        // Camera must be pitched gently forward (10..48 deg), never straight-down 90 deg 2D
        assertThat(pose.pitchDeg).isAtLeast(10f)
        assertThat(pose.pitchDeg).isAtMost(48f)
        // Camera must be elevated above the vehicle ground level
        assertThat(pose.camY).isGreaterThan(groundY + 1.5f)

        // Camera must sit BEHIND the vehicle along its heading
        val rad = 45.0 * PI / 180.0
        val fwdX = sin(rad)
        val fwdZ = cos(rad)
        val camOffsetAlongFwd = (pose.camX - veh.x) * fwdX + (pose.camZ - veh.z) * fwdZ
        assertThat(camOffsetAlongFwd).isLessThan(-3.0)

        // Project vehicle center & a point 40m ahead: ahead point must appear higher on screen (smaller Y)
        val pVeh = projectPoint3D(veh.x.toFloat(), groundY + 1.2f, veh.z.toFloat(), pose, 1080f, 1920f)
        val aheadGroundY = getTerrainHeightAt(world, veh.x + fwdX * 40.0, veh.z + fwdZ * 40.0).toFloat()
        val pAhead = projectPoint3D(
            (veh.x + fwdX * 40.0).toFloat(),
            aheadGroundY + 1.2f,
            (veh.z + fwdZ * 40.0).toFloat(),
            pose,
            1080f,
            1920f,
        )

        assertThat(pVeh.visible).isTrue()
        assertThat(pAhead.visible).isTrue()
        assertThat(pAhead.screenY).isLessThan(pVeh.screenY)
    }

    @Test
    fun simulationStability_lazyCellAllocationAndHandbrakeHold() {
        val world = createNewWorld(0, "Fazenda Estável", "map_pr", 250000.0, "Padrão", 64)
        val veh = world.vehicles.first().copy(engineRunning = true, speedMs = 6.5)
        var state = world.copy(
            activeVehicleUid = veh.uid,
            vehicles = world.vehicles.map { if (it.uid == veh.uid) veh else it },
        )

        val initialCellsRef = state.cells

        // Normal driving tick without lowering implement must NOT clone all terrain cells
        state = stepSimulation(state, ControlInput(throttle = 0.6), dtSec = 0.033, timeScale = 1.0)
        assertThat(state.cells).isSameInstanceAs(initialCellsRef)

        // Engaging handbrake stops the moving vehicle safely
        state = toggleVehicleHandbrakeInWorld(state)
        assertThat(state.vehicles.first { it.uid == veh.uid }.handbrakeOn).isTrue()

        repeat(45) {
            state = stepSimulation(state, ControlInput(handbrake = true), dtSec = 0.04, timeScale = 1.0)
        }
        val stopped = state.vehicles.first { it.uid == veh.uid }
        assertThat(kotlin.math.abs(stopped.speedMs)).isLessThan(0.15)
        assertThat(stopped.isBraking).isTrue()
    }

    @Test
    fun hudReorganization_threeGeneralButtonsPlusVehicleControlsMenuAndMapFlows() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FarmViewModel(app)
        vm.startNewCareer(
            slotIndex = 0,
            farmName = "Fazenda HUD",
            mapId = "map_pr",
            startingMoneyBrl = 250000.0,
            startingMode = "Padrão",
            gridSide = 64,
        )
        // Place player inside first vehicle for driving HUD verification
        val firstVehUid = vm.worldState.value.vehicles.first().uid
        vm.switchToVehicle(firstVehUid)

        composeRule.setContent {
            val ws by vm.worldState.collectAsStateWithLifecycle()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val cameraMode by vm.cameraMode.collectAsStateWithLifecycle()
            val timeScale by vm.timeScale.collectAsStateWithLifecycle()
            val activeTab by vm.activeTab.collectAsStateWithLifecycle()
            val showFullMap by vm.showFullMap.collectAsStateWithLifecycle()
            val hudHidden by vm.hudHidden.collectAsStateWithLifecycle()
            FarmBrasilTheme {
                GameplayHudScreen(
                    viewModel = vm,
                    worldState = ws,
                    settings = settings,
                    cameraMode = cameraMode,
                    timeScale = timeScale,
                    activeTab = activeTab,
                    showFullMap = showFullMap,
                    hudHidden = hudHidden,
                    roomMembers = emptyList(),
                    localUid = "local_test",
                )
            }
        }
        composeRule.waitForIdle()

        // 1. At most 3 general HUD buttons: MENU, MAPA (tag hud_minimap_button), contextual INTERAÇÃO
        composeRule.onNodeWithTag("hud_menu_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_minimap_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_interaction_button").assertIsDisplayed()

        // Old loose clutter chips must NOT appear on HUD
        assertThat(composeRule.onAllNodesWithText("Dormir").fetchSemanticsNodes()).isEmpty()
        assertThat(composeRule.onAllNodesWithText("Terceirização").fetchSemanticsNodes()).isEmpty()
        assertThat(composeRule.onAllNodesWithTag("hud_tab_loja").fetchSemanticsNodes()).isEmpty()

        // 2. Vehicle controls (steering, brake, throttle, handbrake, ignition) visible while driving
        composeRule.onNodeWithTag("steer_left_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("steer_right_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("pedal_brake").assertIsDisplayed()
        composeRule.onNodeWithTag("pedal_throttle").assertIsDisplayed()
        composeRule.onNodeWithTag("vehicle_handbrake_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("vehicle_engine_btn").assertIsDisplayed()

        // 3. Tapping contextual INTERAÇÃO exits vehicle cleanly
        composeRule.onNodeWithTag("hud_interaction_button").performClick()
        composeRule.waitForIdle()
        assertThat(vm.worldState.value.activeVehicleUid).isNull()

        // Tapping contextual INTERAÇÃO again re-enters nearby vehicle
        composeRule.onNodeWithTag("hud_interaction_button").performClick()
        composeRule.waitForIdle()
        assertThat(vm.worldState.value.activeVehicleUid).isEqualTo(firstVehUid)

        // 4. Tapping MENU opens organized Menu panel with Loja, Banco, Garagem, Propriedades, Missões, Configurações
        composeRule.onNodeWithTag("hud_menu_button").performClick()
        composeRule.waitForIdle()
        assertThat(vm.activeTab.value).isEqualTo(InGameTab.MENU)
    }

    @Test
    fun reactiveMenuAndMapIntegration_selectLojaAndBancoFromMapAndMenu() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FarmViewModel(app)
        vm.startNewCareer(0, "Fazenda Reativa", "map_pr", 250000.0, "Padrão", 64)
        vm.switchToVehicle(vm.worldState.value.vehicles.first().uid)

        composeRule.setContent {
            val ws by vm.worldState.collectAsStateWithLifecycle()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val cameraMode by vm.cameraMode.collectAsStateWithLifecycle()
            val timeScale by vm.timeScale.collectAsStateWithLifecycle()
            val activeTab by vm.activeTab.collectAsStateWithLifecycle()
            val showFullMap by vm.showFullMap.collectAsStateWithLifecycle()
            val hudHidden by vm.hudHidden.collectAsStateWithLifecycle()
            FarmBrasilTheme {
                GameplayHudScreen(
                    viewModel = vm,
                    worldState = ws,
                    settings = settings,
                    cameraMode = cameraMode,
                    timeScale = timeScale,
                    activeTab = activeTab,
                    showFullMap = showFullMap,
                    hudHidden = hudHidden,
                    roomMembers = emptyList(),
                    localUid = "local_test",
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("hud_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_item_loja").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_item_banco").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_item_garagem").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_item_propriedades").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_item_missoes").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_item_configuracoes").assertIsDisplayed()

        composeRule.onNodeWithTag("menu_item_loja").performClick()
        composeRule.waitForIdle()
        assertThat(vm.activeTab.value).isEqualTo(InGameTab.LOJA)

        composeRule.onNodeWithTag("panel_close_btn").performClick()
        composeRule.waitForIdle()
        assertThat(vm.activeTab.value).isNull()

        composeRule.onNodeWithTag("hud_minimap_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_poi_banco").assertIsDisplayed()
        composeRule.onNodeWithTag("map_poi_loja").assertIsDisplayed()
        composeRule.onNodeWithTag("map_poi_garagem").assertIsDisplayed()

        composeRule.onNodeWithTag("map_poi_banco").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_open_system_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertThat(vm.showFullMap.value).isFalse()
        assertThat(vm.activeTab.value).isEqualTo(InGameTab.ECONOMIA)
    }

    @Test
    fun regionalMaps_distinctReliefFloraCropsAndPlotLayoutsAcrossStates() {
        val pr = createNewWorld(0, "Fazenda PR", "map_pr", 250000.0, "Padrão", 64)
        val mt = createNewWorld(0, "Fazenda MT", "map_mt", 250000.0, "Padrão", 64)
        val mg = createNewWorld(0, "Fazenda MG", "map_mg", 250000.0, "Padrão", 64)
        val ce = createNewWorld(0, "Fazenda CE", "map_ce", 250000.0, "Padrão", 64)
        val pa = createNewWorld(0, "Fazenda PA", "map_pa", 250000.0, "Padrão", 64)

        // 1. Flora & Biome signatures differ authentically by state
        assertThat(pr.props.any { it.type == WorldPropType.TREE_ARAUCARIA }).isTrue()
        assertThat(mt.props.any { it.type == WorldPropType.TREE_CERRADO }).isTrue()
        assertThat(mg.props.any { it.type == WorldPropType.COFFEE_SHRUB }).isTrue()
        assertThat(ce.props.any { it.type == WorldPropType.CACTUS_MANDACARU }).isTrue()
        assertThat(pa.props.any { it.type == WorldPropType.TREE_PALM_AMAZON }).isTrue()

        // 2. Relief amplitude: Minas Gerais mountains have stronger vertical relief range than Mato Grosso plateau
        val mgHeights = mg.cells.map { it.height }
        val mtHeights = mt.cells.map { it.height }
        val mgRange = (mgHeights.maxOrNull() ?: 0.0) - (mgHeights.minOrNull() ?: 0.0)
        val mtRange = (mtHeights.maxOrNull() ?: 0.0) - (mtHeights.minOrNull() ?: 0.0)
        assertThat(mgRange).isGreaterThan(mtRange * 1.15)

        // 3. Plot structure & regional crops:
        //    MT has mega mechanized blocks & cotton; MG has smaller hillside coffee plots; CE has irrigated oasis acudes
        val mtPlot8 = mt.plots.first { it.id == 8 }
        val mgPlot8 = mg.plots.first { it.id == 8 }
        assertThat(mtPlot8.areaHa).isGreaterThan(mgPlot8.areaHa)
        assertThat(mt.cells.any { it.cropId == "algodao" }).isTrue()
        assertThat(mg.cells.any { it.cropId == "cafe" }).isTrue()
        assertThat(pr.cells.any { it.cropId == "trigo" || it.cropId == "soja" }).isTrue()

        // 4. Semi-arid Ceara still provides water via natural oasis acudes + irrigated plots
        assertThat(ce.cells.any { it.soilState == SoilState.AGUA_RIO }).isTrue()
        assertThat(ce.cells.any { it.isIrrigated == true }).isTrue()
    }

    @Test
    fun offlineSignInAndManifest_workWithoutFirebaseOrInternet() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FarmViewModel(app)
        if (vm.accountUser.value != null) {
            vm.farmAccountManager.signOut()
        }

        // Verify SignInScreen works completely without Google Sign-In or internet
        composeRule.setContent {
            val acc by vm.accountUser.collectAsStateWithLifecycle()
            val err by vm.authError.collectAsStateWithLifecycle()
            val loading by vm.authLoading.collectAsStateWithLifecycle()
            FarmBrasilTheme {
                SignInScreen(
                    authLoading = loading,
                    authError = err,
                    pendingWarningUser = acc?.takeIf { !it.warningAcknowledged },
                    hasRegisteredAccounts = false,
                    onGenerateCode = { vm.generatePersonalCode() },
                    onCreateAccount = { u, c -> vm.createFarmAccount(u, c) },
                    onSignInOrRecover = { u, c -> vm.signInOrRecoverFarmAccount(u, c) },
                    onRestoreSyncBundle = { u, c, j -> vm.restoreAccountSyncBundle(u, c, j) },
                    onAcknowledgeWarning = { vm.acknowledgeAccountWarning() },
                    onClearError = { vm.clearAuthError() },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("welcome_title").assertIsDisplayed()
        composeRule.onNodeWithTag("account_username_input").performTextInput("FazendeiroTeste")
        composeRule.onNodeWithTag("generate_code_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("create_account_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(vm.accountUser.value).isNotNull()
        assertThat(vm.accountUser.value?.warningAcknowledged).isFalse()
        composeRule.onNodeWithTag("acknowledge_code_warning_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertThat(vm.accountUser.value?.warningAcknowledged).isTrue()
        assertThat(vm.route.value).isEqualTo(AppRoute.MainMenu)

        // Verify MainActivity manifest declares landscape orientation for comfortable mobile gameplay
        val pm = app.packageManager
        val pkgInfo = pm.getPackageInfo(app.packageName, android.content.pm.PackageManager.GET_ACTIVITIES)
        val mainAct = pkgInfo.activities?.firstOrNull { it.name.endsWith("MainActivity") }
        assertThat(mainAct).isNotNull()
        assertThat(mainAct!!.screenOrientation).isEqualTo(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
    }

    @Test
    @Config(sdk = [35], qualifiers = "w800dp-h400dp-land")
    fun landscapeFullMapAndHud_renderCleanlyWithRegionalTitleAndControls() {
        val mgWorld = createNewWorld(0, "Fazenda Mineira", "map_mg", 250000.0, "Padrão", 64)
        composeRule.setContent {
            FarmBrasilTheme {
                FullMapScreen(
                    worldState = mgWorld,
                    onClose = {},
                    onTeleport = { _, _, _ -> },
                    onSwitchVehicle = {},
                    onBuyPlot = {},
                    onLeasePlot = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fullmap_region_title").assertIsDisplayed().assertTextContains("Minas Gerais", substring = true)
        composeRule.onNodeWithTag("map_poi_loja").assertIsDisplayed()
        composeRule.onNodeWithTag("map_poi_banco").assertIsDisplayed()
        composeRule.onNodeWithTag("map_poi_garagem").assertIsDisplayed()
        composeRule.onNodeWithTag("fullmap_close").assertIsDisplayed()
    }
}
