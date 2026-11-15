package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.domain.ALL_IMPLEMENTS
import com.example.domain.ALL_VEHICLES
import com.example.domain.BrandColors
import com.example.domain.BuildingType
import com.example.domain.CHARACTER_PRESETS
import com.example.domain.CameraMode
import com.example.domain.OwnedImplement
import com.example.domain.PlacedBuilding
import com.example.domain.PlayerAppearanceConfig
import com.example.domain.SoilState
import com.example.domain.VehicleCategory
import com.example.domain.WorldProp
import com.example.domain.WorldPropType
import com.example.domain.getVehicleDef
import com.example.domain.resolveBrandVisualPalette
import com.example.domain.resolveImplementBrandPalette
import com.example.domain.resolveVehicleTuning
import com.example.simulation.ControlInput
import com.example.simulation.EMPTY_CONTROL_INPUT
import com.example.simulation.adjustCruiseSpeedInWorld
import com.example.simulation.createNewWorld
import com.example.simulation.findSafeExitSpot
import com.example.simulation.isPointBlockedBySolidWorld
import com.example.simulation.stepSimulation
import com.example.simulation.toggleCruiseControlInWorld
import com.example.simulation.toggleEnterExitVehicle
import com.example.simulation.toggleVehicleHandbrakeInWorld
import com.example.state.FarmViewModel
import com.example.ui.components.computeCameraPose3D
import com.example.ui.screens.GameplayHudScreen
import com.example.ui.screens.NewCareerSetupScreen
import com.example.ui.theme.FarmBrasilTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FarmBrasilPolishAndCollisionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun brandColorFidelity_johnDeereAndOtherManufacturersMatchRealIdentities() {
        // 1. John Deere tractors & combines: green body, GREEN hood, yellow rims, yellow decal/stripe, yellow seat
        val jdMachines = ALL_VEHICLES.filter { it.brand.contains("John Deere", ignoreCase = true) }
        assertThat(jdMachines).isNotEmpty()
        for (jd in jdMachines) {
            val palette = resolveBrandVisualPalette(jd)
            assertThat(palette.primaryRgb).isEqualTo(BrandColors.JD_GREEN)
            assertThat(palette.hoodRgb).isEqualTo(BrandColors.JD_GREEN) // NEVER yellow hood
            assertThat(palette.hoodRgb).isNotEqualTo(BrandColors.JD_YELLOW)
            assertThat(palette.cabRoofRgb).isEqualTo(BrandColors.JD_GREEN)
            assertThat(palette.hoodStripeRgb).isEqualTo(BrandColors.JD_YELLOW)
            assertThat(palette.decalTextRgb).isEqualTo(BrandColors.JD_YELLOW)
            assertThat(palette.seatRgb).isEqualTo(BrandColors.JD_YELLOW)
            if (jd.category != VehicleCategory.TRATOR_ESTEIRA) {
                assertThat(palette.rimRgb).isEqualTo(BrandColors.JD_YELLOW)
            }
        }

        // Verify tuning also propagates JD yellow rims when rimColorId is stock/null
        val baseWorld = createNewWorld(0, "Teste JD", "map_pr", 500000.0, "Padrão", 64)
        val jdOwned = baseWorld.vehicles.first().copy(defId = "jd_8370r", rimColorId = "stock")
        val jdTuning = resolveVehicleTuning(jdOwned, getVehicleDef("jd_8370r"))
        assertThat(jdTuning.rimRgb).isEqualTo(BrandColors.JD_YELLOW)

        // 2. Massey Ferguson: MF Red hood/body, silver rims (except IDEAL graphite + red rims)
        val mfTractors = ALL_VEHICLES.filter {
            it.brand.contains("Massey", ignoreCase = true) && it.category == VehicleCategory.TRATOR
        }
        assertThat(mfTractors).isNotEmpty()
        for (mf in mfTractors) {
            val p = resolveBrandVisualPalette(mf)
            assertThat(p.primaryRgb).isEqualTo(BrandColors.MF_RED)
            assertThat(p.hoodRgb).isEqualTo(BrandColors.MF_RED)
            assertThat(p.rimRgb.r).isGreaterThan(0.8)
            assertThat(p.rimRgb.g).isGreaterThan(0.8)
            assertThat(p.rimRgb.b).isGreaterThan(0.8)
        }

        // 3. New Holland: Blue tractors + White roof/rims; Yellow combines + Blue accents
        val nhTractor = ALL_VEHICLES.first { it.brand.contains("New Holland", ignoreCase = true) && it.category == VehicleCategory.TRATOR }
        val nhTractorPal = resolveBrandVisualPalette(nhTractor)
        assertThat(nhTractorPal.primaryRgb).isEqualTo(BrandColors.NH_BLUE)
        assertThat(nhTractorPal.hoodRgb).isEqualTo(BrandColors.NH_BLUE)
        assertThat(nhTractorPal.cabRoofRgb).isEqualTo(BrandColors.NH_WHITE)
        assertThat(nhTractorPal.rimRgb).isEqualTo(BrandColors.NH_WHITE)

        val nhCombine = ALL_VEHICLES.firstOrNull { it.brand.contains("New Holland", ignoreCase = true) && it.category == VehicleCategory.COLHEITADEIRA }
        if (nhCombine != null) {
            val p = resolveBrandVisualPalette(nhCombine)
            assertThat(p.primaryRgb.r).isGreaterThan(0.85)
            assertThat(p.primaryRgb.g).isGreaterThan(0.65)
            assertThat(p.hoodStripeRgb).isEqualTo(BrandColors.NH_BLUE)
            assertThat(p.cabRoofRgb).isEqualTo(BrandColors.NH_WHITE)
        }

        // 4. Case IH agricultural: Case Red body/hood + Black cab roof/chassis
        val caseTractor = ALL_VEHICLES.first { it.brand.contains("Case", ignoreCase = true) && it.category == VehicleCategory.TRATOR }
        val casePal = resolveBrandVisualPalette(caseTractor)
        assertThat(casePal.primaryRgb).isEqualTo(BrandColors.CASE_RED)
        assertThat(casePal.hoodRgb).isEqualTo(BrandColors.CASE_RED)
        assertThat(casePal.cabRoofRgb).isEqualTo(BrandColors.CASE_BLACK)

        // 5. Fendt: Nature Green + Terra Red rims
        val fendt = ALL_VEHICLES.first { it.brand.contains("Fendt", ignoreCase = true) }
        val fendtPal = resolveBrandVisualPalette(fendt)
        assertThat(fendtPal.primaryRgb).isEqualTo(BrandColors.FENDT_GREEN)
        assertThat(fendtPal.rimRgb.r).isGreaterThan(0.75)
        assertThat(fendtPal.rimRgb.g).isLessThan(0.25)

        // 6. Claas: Seed Green + Red rims + White roof
        val claas = ALL_VEHICLES.first { it.brand.contains("Claas", ignoreCase = true) }
        val claasPal = resolveBrandVisualPalette(claas)
        assertThat(claasPal.primaryRgb).isEqualTo(BrandColors.CLAAS_GREEN)
        assertThat(claasPal.rimRgb.r).isGreaterThan(0.75)
        assertThat(claasPal.rimRgb.g).isLessThan(0.25)
        assertThat(claasPal.cabRoofRgb.r).isGreaterThan(0.9)

        // 7. Valtra, Valmet, CBT, Agrale: distinct brand identities
        val valtra = ALL_VEHICLES.first { it.brand.contains("Valtra", ignoreCase = true) }
        val valmet = ALL_VEHICLES.first { it.brand.contains("Valmet", ignoreCase = true) }
        val cbt = ALL_VEHICLES.first { it.brand.contains("CBT", ignoreCase = true) }
        val agrale = ALL_VEHICLES.first { it.brand.contains("Agrale", ignoreCase = true) }
        assertThat(resolveBrandVisualPalette(cbt).primaryRgb).isEqualTo(BrandColors.CBT_YELLOW)
        assertThat(resolveBrandVisualPalette(agrale).rimRgb).isEqualTo(BrandColors.AGRALE_CREAM)
        assertThat(resolveBrandVisualPalette(valtra).chassisRgb).isEqualTo(BrandColors.VALTRA_BLACK)
        assertThat(resolveBrandVisualPalette(valmet).primaryRgb).isNotEqualTo(BrandColors.JD_GREEN)

        // 8. SLC 2200 classic terracotta-orange vs SLC 6200 green+yellow
        val slc2200 = getVehicleDef("slc_2200_hist")
        val slc6200 = getVehicleDef("slc_6200_turbo_88")
        assertThat(resolveBrandVisualPalette(slc2200).primaryRgb.r).isGreaterThan(0.75)
        assertThat(resolveBrandVisualPalette(slc6200).primaryRgb).isEqualTo(BrandColors.JD_GREEN)
        assertThat(resolveBrandVisualPalette(slc6200).rimRgb).isEqualTo(BrandColors.JD_YELLOW)

        // 9. Implements inherit faithful brand palettes
        val jdImp = ALL_IMPLEMENTS.firstOrNull { it.brand.contains("John Deere", ignoreCase = true) }
        if (jdImp != null) {
            val impPal = resolveImplementBrandPalette(jdImp)
            assertThat(impPal.primaryRgb).isEqualTo(BrandColors.JD_GREEN)
            assertThat(impPal.rimRgb).isEqualTo(BrandColors.JD_YELLOW)
        }
    }

    @Test
    fun starterWorld_hasZeroInitialVehicleOverlapAndSmoothFieldPlots() {
        for (mapId in listOf("map_pr", "map_mt", "map_mg")) {
            val world = createNewWorld(0, "Fazenda $mapId", mapId, 250000.0, "Padrão", 64)
            for (veh in world.vehicles) {
                val blocked = isPointBlockedBySolidWorld(
                    state = world,
                    px = veh.x,
                    pz = veh.z,
                    radius = 0.75,
                    excludeVehicleUid = veh.uid,
                )
                assertThat(blocked).isFalse()
            }
            for (i in world.vehicles.indices) {
                for (j in (i + 1) until world.vehicles.size) {
                    val v1 = world.vehicles[i]
                    val v2 = world.vehicles[j]
                    assertThat(hypot(v1.x - v2.x, v1.z - v2.z)).isGreaterThan(2.5)
                }
            }
            assertThat(world.plots.size).isAtLeast(6)
            assertThat(world.cells.count { it.plotId != null }).isGreaterThan(250)
        }
    }

    @Test
    fun worldCollisions_vehiclesAndPlayerStopAtWallsFencesTreesAndOtherMachines() {
        val base = createNewWorld(0, "Fazenda Colisão", "map_pr", 500000.0, "Padrão", 64)
        val flatCells = base.cells.map { it.copy(height = 2.0, soilState = SoilState.ESTRADA_TERRA) }

        // 1. Driving straight into a CASA wall stops outside and does not pass through
        val house = PlacedBuilding("test_casa", BuildingType.CASA, x = 0.0, z = 20.0, rotationDeg = 0.0)
        val tractorNearHouse = base.vehicles.first().copy(
            uid = "v_test",
            defId = "jd_6150j",
            x = 0.0,
            z = 9.0,
            headingDeg = 0.0, // +Z direction straight at house at z=20 (depth 7m -> front wall at z=16.64)
            engineRunning = true,
            handbrakeOn = false,
            speedMs = 6.0,
        )
        var state = base.copy(
            cells = flatCells,
            buildings = listOf(house),
            props = emptyList(),
            implements = emptyList(),
            roadVehicles = emptyList(),
            vehicles = listOf(tractorNearHouse),
            activeVehicleUid = "v_test",
        )
        repeat(60) {
            state = stepSimulation(state, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        val afterHouse = state.vehicles.first()
        // Tractor must NEVER pass inside or behind house front wall (z=16.64)
        assertThat(afterHouse.z).isLessThan(15.2)

        // 2. Driving straight into a CERCA fence blocks passage
        val fence = PlacedBuilding("test_cerca", BuildingType.CERCA, x = 0.0, z = 14.0, rotationDeg = 0.0)
        var fenceState = state.copy(
            buildings = listOf(fence),
            vehicles = listOf(tractorNearHouse.copy(z = 7.0, speedMs = 5.0)),
        )
        repeat(60) {
            fenceState = stepSimulation(fenceState, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(fenceState.vehicles.first().z).isLessThan(13.2)

        // 3. Closed PORTEIRA blocks vehicle; open PORTEIRA allows passage through gateway
        val closedGate = PlacedBuilding("test_gate", BuildingType.PORTEIRA, x = 0.0, z = 14.0, rotationDeg = 0.0, isGateOpen = false)
        var closedGateState = state.copy(
            buildings = listOf(closedGate),
            vehicles = listOf(tractorNearHouse.copy(z = 7.0, speedMs = 5.0)),
        )
        repeat(60) {
            closedGateState = stepSimulation(closedGateState, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(closedGateState.vehicles.first().z).isLessThan(13.2)

        val openGate = closedGate.copy(isGateOpen = true)
        var openGateState = state.copy(
            buildings = listOf(openGate),
            vehicles = listOf(tractorNearHouse.copy(z = 7.0, speedMs = 5.0)),
        )
        repeat(60) {
            openGateState = stepSimulation(openGateState, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(openGateState.vehicles.first().z).isGreaterThan(15.5)

        // 4. Parked second vehicle blocks moving tractor (vehicles collide physically)
        val parkedVeh = tractorNearHouse.copy(uid = "v_parked", x = 0.0, z = 16.0, speedMs = 0.0, handbrakeOn = true)
        var vehColState = state.copy(
            buildings = emptyList(),
            vehicles = listOf(tractorNearHouse.copy(z = 7.0, speedMs = 5.0), parkedVeh),
        )
        repeat(50) {
            vehColState = stepSimulation(vehColState, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        val moving = vehColState.vehicles.first { it.uid == "v_test" }
        val parked = vehColState.vehicles.first { it.uid == "v_parked" }
        assertThat(moving.z).isLessThan(parked.z - 2.0)

        // 5. Tree prop blocks tractor
        val tree = WorldProp("test_tree", WorldPropType.TREE_ARAUCARIA, x = 0.0, z = 14.0, amountKg = 0.0, productId = "")
        var treeState = state.copy(
            buildings = emptyList(),
            props = listOf(tree),
            vehicles = listOf(tractorNearHouse.copy(z = 7.0, speedMs = 5.0)),
        )
        repeat(50) {
            treeState = stepSimulation(treeState, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(treeState.vehicles.first().z).isLessThan(13.0)

        // 6. Player on foot cannot walk through wall or fence (house front wall at z=16.64)
        var footState = state.copy(
            activeVehicleUid = null,
            buildings = listOf(house),
            player = state.player.copy(x = 0.0, z = 12.5, headingDeg = 0.0),
        )
        repeat(40) {
            footState = stepSimulation(footState, ControlInput(moveX = 0.0, moveY = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(footState.player.z).isLessThan(16.4)

        // 7. Safe exit spot never places player inside a wall when exiting a tractor parked beside house
        val tightTractor = tractorNearHouse.copy(x = 0.0, z = 12.5, headingDeg = 90.0)
        val tightState = state.copy(buildings = listOf(house), vehicles = listOf(tightTractor))
        val (exitX, exitZ) = findSafeExitSpot(tightState, tightTractor)
        assertThat(isPointBlockedBySolidWorld(tightState, exitX, exitZ, radius = 0.4)).isFalse()

        // 8. Camera pose clamps against solid buildings so camera never sits inside a house wall
        val camPose = computeCameraPose3D(
            state.copy(
                buildings = listOf(PlacedBuilding("cam_wall", BuildingType.CASA, x = 0.0, z = 3.0, rotationDeg = 0.0)),
                vehicles = listOf(tractorNearHouse.copy(x = 0.0, z = 9.0, headingDeg = 0.0)),
                activeVehicleUid = "v_test",
            ),
            CameraMode.TERCEIRA_PESSOA,
        )
        // House occupies z in [-0.5, +6.5]; camera behind tractor at z=9 must be pulled forward outside z=6.5
        assertThat(camPose.camZ).isAtLeast(6.8f)
    }

    @Test
    fun cruiseControl_holdsConfiguredSpeedAndDisengagesOnBrakeHandbrakeOrExit() {
        val base = createNewWorld(0, "Fazenda Cruzeiro", "map_pr", 500000.0, "Padrão", 64)
        val flatCells = base.cells.map { it.copy(height = 2.0, soilState = SoilState.ESTRADA_ASFALTO) }
        val tractor = base.vehicles.first().copy(
            uid = "v_cruise",
            defId = "jd_8370r",
            x = -60.0,
            z = 0.0,
            headingDeg = 90.0,
            engineRunning = true,
            handbrakeOn = false,
            speedMs = 0.0,
        )
        var state = base.copy(
            cells = flatCells,
            buildings = emptyList(),
            props = emptyList(),
            implements = emptyList(),
            roadVehicles = emptyList(),
            vehicles = listOf(tractor),
            activeVehicleUid = "v_cruise",
        )

        // Enable cruise and set to 18 km/h
        state = toggleCruiseControlInWorld(state)
        assertThat(state.vehicles.first().cruiseControlActive).isTrue()
        val initialTarget = state.vehicles.first().cruiseTargetKmh!!
        state = adjustCruiseSpeedInWorld(state, 18.0 - initialTarget)
        assertThat(state.vehicles.first().cruiseTargetKmh).isWithin(0.5).of(18.0)

        // Step simulation with EMPTY touch input -> vehicle automatically accelerates to ~18 km/h
        repeat(120) {
            state = stepSimulation(state, EMPTY_CONTROL_INPUT, dtSec = 0.05, timeScale = 1.0)
        }
        val kmh = state.vehicles.first().speedMs * 3.6
        assertThat(kmh).isWithin(2.2).of(18.0)

        // Pressing brake cancels cruise control immediately
        val afterBrake = stepSimulation(state, ControlInput(brake = 1.0), dtSec = 0.05, timeScale = 1.0)
        assertThat(afterBrake.vehicles.first().cruiseControlActive).isFalse()

        // Re-enable then engage handbrake -> cancels cruise control
        val reEnabled = toggleCruiseControlInWorld(afterBrake)
        assertThat(reEnabled.vehicles.first().cruiseControlActive).isTrue()
        val afterHandbrake = toggleVehicleHandbrakeInWorld(reEnabled)
        assertThat(afterHandbrake.vehicles.first().cruiseControlActive).isFalse()

        // Re-enable then exit vehicle -> cancels cruise control
        val reEnabled2 = toggleCruiseControlInWorld(toggleVehicleHandbrakeInWorld(afterHandbrake))
        assertThat(reEnabled2.vehicles.first().cruiseControlActive).isTrue()
        val afterExit = toggleEnterExitVehicle(reEnabled2)
        assertThat(afterExit.vehicles.first().cruiseControlActive).isFalse()
    }

    @Test
    fun cruiseControlHudControls_renderAndAdjustTargetSpeedViaViewModel() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FarmViewModel(app)
        vm.startNewCareer(0, "Fazenda HUD Cruz", "map_pr", 250000.0, "Padrão", 64)
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

        composeRule.onNodeWithTag("cruise_toggle_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        val firstVeh = vm.worldState.value.vehicles.first { it.uid == vm.worldState.value.activeVehicleUid }
        assertThat(firstVeh.cruiseControlActive).isTrue()
        val startTarget = firstVeh.cruiseTargetKmh!!

        composeRule.onNodeWithTag("cruise_plus_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        val afterPlus = vm.worldState.value.vehicles.first { it.uid == vm.worldState.value.activeVehicleUid }
        assertThat(afterPlus.cruiseTargetKmh).isWithin(0.1).of(startTarget + 2.0)

        composeRule.onNodeWithTag("cruise_minus_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        val afterMinus = vm.worldState.value.vehicles.first { it.uid == vm.worldState.value.activeVehicleUid }
        assertThat(afterMinus.cruiseTargetKmh).isWithin(0.1).of(startTarget)
    }

    @Test
    fun combineHarvesters_cutRipeCropsToStrawAndFillTankAcrossModels() {
        val base = createNewWorld(0, "Fazenda Ceifa", "map_pr", 500000.0, "Padrão", 64)
        val ripeCells = base.cells.map {
            it.copy(height = 2.0, soilState = SoilState.PRONTO_COLHEITA, cropId = "soja", growthProgress = 1.0)
        }
        for (combId in listOf("jd_s770", "slc_6200_turbo_88", "nh_tc5070", "case_8250")) {
            val comb = base.vehicles.first().copy(
                uid = "v_$combId",
                defId = combId,
                x = 0.0,
                z = -20.0,
                headingDeg = 0.0,
                engineRunning = true,
                handbrakeOn = false,
                headerLowered = true,
                grainTankFillL = 0.0,
                grainCropId = null,
            )
            var state = base.copy(
                cells = ripeCells,
                buildings = emptyList(),
                props = emptyList(),
                implements = emptyList(),
                roadVehicles = emptyList(),
                vehicles = listOf(comb),
                activeVehicleUid = comb.uid,
            )
            repeat(40) {
                state = stepSimulation(state, ControlInput(throttle = 0.8), dtSec = 0.05, timeScale = 1.0)
            }
            val updated = state.vehicles.first()
            assertThat(updated.grainTankFillL).isGreaterThan(0.0)
            assertThat(updated.rotorSpinDeg).isGreaterThan(10.0)
            assertThat(state.cells.any { it.soilState == SoilState.PALHADA }).isTrue()
        }
    }

    @Test
    fun characterPresets_fourPhotoOptionsSelectableInSetupAndViewModel() {
        assertThat(CHARACTER_PRESETS).hasSize(4)
        assertThat(CHARACTER_PRESETS.map { it.config.outfitStyle }.distinct()).hasSize(4)

        var chosenAppearance: PlayerAppearanceConfig? = null
        composeRule.setContent {
            FarmBrasilTheme {
                NewCareerSetupScreen(
                    selectedMapId = "map_pr",
                    installedPackIds = emptySet(),
                    careerSlots = List(20) { null },
                    onSelectMap = {},
                    onStartCareer = { _, _, _, _, _, _, appearance ->
                        chosenAppearance = appearance
                    },
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("char_preset_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("char_preset_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("char_preset_3").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("char_preset_4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("start_new_career_button").performClick()
        composeRule.waitForIdle()

        assertThat(chosenAppearance?.presetId).isEqualTo("preset_3")
        assertThat(chosenAppearance?.outfitStyle).isEqualTo("flannel_plaid")

        // Verify ViewModel can also switch character presets mid-career
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FarmViewModel(app)
        vm.startNewCareer(0, "Fazenda Personagem", "map_pr", 250000.0, "Padrão", 64)
        vm.setPlayerAppearancePreset("preset_2")
        assertThat(vm.worldState.value.player.appearance?.presetId).isEqualTo("preset_2")
        assertThat(vm.worldState.value.player.appearance?.glassesStyle).isEqualTo("reading")
    }
}
