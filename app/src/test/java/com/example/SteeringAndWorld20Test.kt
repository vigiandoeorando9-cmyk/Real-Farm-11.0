package com.example

import android.app.Application
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.test.core.app.ApplicationProvider
import com.example.domain.BuildingType
import com.example.domain.CameraMode
import com.example.domain.ESTABLISHMENTS
import com.example.domain.GameSettings
import com.example.domain.LANDMARKS
import com.example.domain.OwnedImplement
import com.example.domain.SoilState
import com.example.domain.WorldState
import com.example.domain.getVehicleDef
import com.example.domain.getVehiclePhysicsSpec
import com.example.simulation.ControlInput
import com.example.simulation.buyVehicleInWorld
import com.example.simulation.createNewWorld
import com.example.simulation.depositToBank
import com.example.simulation.findNearbyFuelSource
import com.example.simulation.refuelVehicleInWorld
import com.example.simulation.repayBankLoan
import com.example.simulation.sellCargoAtNearbyStation
import com.example.simulation.stepSimulation
import com.example.simulation.takeBankLoan
import com.example.simulation.withdrawFromBank
import com.example.state.FarmViewModel
import com.example.ui.screens.GameplayHudScreen
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
@Config(sdk = [35], qualifiers = "w840dp-h412dp-land")
class SteeringAndWorld20Test {

    @get:Rule
    val composeRule = createComposeRule()

    private fun spawnFlatDrivingWorld(defId: String): WorldState {
        val base = createNewWorld(0, "Fazenda Direção", "map_pr", 500000.0, "Padrão", 64)
        val template = base.vehicles.first()
        val veh = template.copy(
            uid = "v_steer",
            defId = defId,
            x = 0.0,
            z = 0.0,
            headingDeg = 0.0,
            speedMs = 0.0,
            steerAngleDeg = 0.0,
            gear = 1,
            rpm = 1200.0,
            fuelL = getVehicleDef(defId).fuelCapacityL,
            engineRunning = true,
            handbrakeOn = false,
            attachedImplementUid = null,
        )
        val flatCells = base.cells.map {
            it.copy(height = 2.0, soilState = SoilState.ESTRADA_ASFALTO, moisture = 0.15, compaction = 0.1)
        }
        return base.copy(
            cells = flatCells,
            buildings = emptyList(),
            props = emptyList(),
            roadVehicles = emptyList(),
            vehicles = listOf(veh),
            activeVehicleUid = veh.uid,
            statusAlert = null,
        )
    }

    @Test
    fun steeringLeftAndRight_turnFrontWheelsAndVehicleHeadingCorrectlyAcrossAllVehicleTypes() {
        val vehicleIds = listOf("agrale_5055", "jd_9570r", "jd_s770", "mb_1113")
        for (defId in vehicleIds) {
            // Press LEFT (moveX = -1.0) while accelerating forward from heading 0° (+Z forward, -X left)
            var leftWorld = spawnFlatDrivingWorld(defId)
            repeat(50) {
                leftWorld = stepSimulation(leftWorld, ControlInput(throttle = 0.9, moveX = -1.0), dtSec = 0.04, timeScale = 1.0)
            }
            val leftVeh = leftWorld.vehicles.first()
            assertThat(leftVeh.steerAngleDeg).isLessThan(-5.0)
            assertThat(leftVeh.x).isLessThan(-0.3)

            // Press RIGHT (moveX = +1.0) while accelerating forward from heading 0° (+Z forward, +X right)
            var rightWorld = spawnFlatDrivingWorld(defId)
            repeat(50) {
                rightWorld = stepSimulation(rightWorld, ControlInput(throttle = 0.9, moveX = 1.0), dtSec = 0.04, timeScale = 1.0)
            }
            val rightVeh = rightWorld.vehicles.first()
            assertThat(rightVeh.steerAngleDeg).isGreaterThan(5.0)
            assertThat(rightVeh.x).isGreaterThan(0.3)
        }
    }

    @Test
    fun steeringSmoothReturnToCenterSpeedConfigAndMaxAngleClamp() {
        val defId = "agrale_bx6150"
        val maxSteer = getVehiclePhysicsSpec(getVehicleDef(defId)).maxSteerAngleDeg

        // Higher steerSpeedMultiplier reaches target wheel angle faster, never exceeding maxSteerAngleDeg
        var slowWorld = spawnFlatDrivingWorld(defId)
        var fastWorld = spawnFlatDrivingWorld(defId)
        repeat(8) {
            slowWorld = stepSimulation(slowWorld, ControlInput(throttle = 0.4, moveX = -1.0), dtSec = 0.04, timeScale = 1.0, steerSpeedMultiplier = 0.6)
            fastWorld = stepSimulation(fastWorld, ControlInput(throttle = 0.4, moveX = -1.0), dtSec = 0.04, timeScale = 1.0, steerSpeedMultiplier = 1.8)
        }
        assertThat(abs(fastWorld.vehicles.first().steerAngleDeg)).isGreaterThan(abs(slowWorld.vehicles.first().steerAngleDeg))
        assertThat(abs(fastWorld.vehicles.first().steerAngleDeg)).isAtMost(maxSteer)

        // Hold further to confirm clamp, then release -> smooth return toward 0°
        repeat(35) {
            fastWorld = stepSimulation(fastWorld, ControlInput(throttle = 0.4, moveX = -1.0), dtSec = 0.04, timeScale = 1.0, steerSpeedMultiplier = 1.8)
        }
        val heldAngle = abs(fastWorld.vehicles.first().steerAngleDeg)
        assertThat(heldAngle).isAtMost(maxSteer + 0.01)

        repeat(45) {
            fastWorld = stepSimulation(fastWorld, ControlInput(throttle = 0.4, moveX = 0.0), dtSec = 0.04, timeScale = 1.0, steerSpeedMultiplier = 1.0)
        }
        assertThat(abs(fastWorld.vehicles.first().steerAngleDeg)).isLessThan(heldAngle * 0.2)
    }

    @Test
    fun landscapeHudSteeringButtonsWorkIndependentlyAndSimultaneouslyAndResetCleanly() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FarmViewModel(app)
        val world = spawnFlatDrivingWorld("agrale_bx6150").copy(
            moneyBrl = 450000.0,
            bankBalanceBrl = 125000.0,
        )

        composeRule.setContent {
            FarmBrasilTheme {
                GameplayHudScreen(
                    viewModel = vm,
                    worldState = world,
                    settings = GameSettings(),
                    cameraMode = CameraMode.TERCEIRA_PESSOA,
                    timeScale = 1.0,
                    activeTab = null,
                    showFullMap = false,
                    hudHidden = false,
                    roomMembers = emptyList(),
                    localUid = null,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("steer_left_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("steer_right_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("hud_telemetry_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Comb", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Banco", substring = true).assertIsDisplayed()

        // Press LEFT alone -> moveX = -1.0
        composeRule.onNodeWithTag("steer_left_btn").performTouchInput { down(center) }
        composeRule.waitForIdle()
        assertThat(vm.currentEffectiveControlInput().moveX).isEqualTo(-1.0)

        // Press RIGHT simultaneously -> contradictory press cancels to 0.0 cleanly
        composeRule.onNodeWithTag("steer_right_btn").performTouchInput { down(center) }
        composeRule.waitForIdle()
        assertThat(vm.currentEffectiveControlInput().moveX).isEqualTo(0.0)

        // Release LEFT while keeping RIGHT held -> immediately steers RIGHT (+1.0)
        composeRule.onNodeWithTag("steer_left_btn").performTouchInput { up() }
        composeRule.waitForIdle()
        assertThat(vm.currentEffectiveControlInput().moveX).isEqualTo(1.0)

        // Release RIGHT -> returns to center (0.0)
        composeRule.onNodeWithTag("steer_right_btn").performTouchInput { up() }
        composeRule.waitForIdle()
        assertThat(vm.currentEffectiveControlInput().moveX).isEqualTo(0.0)
    }

    @Test
    fun keyboardAndGamepadInputsSteerLeftAndRightWithoutInversion() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = FarmViewModel(app)

        // Outside Gameplay, hardware steering keys are ignored safely
        assertThat(vm.onHardwareKeyEvent(KeyEvent.KEYCODE_A, isDown = true)).isFalse()

        vm.navigateTo(com.example.domain.AppRoute.Gameplay)

        // Keyboard A / Left -> moveX = -1.0; D / Right -> moveX = +1.0
        assertThat(vm.onHardwareKeyEvent(KeyEvent.KEYCODE_A, isDown = true)).isTrue()
        assertThat(vm.currentEffectiveControlInput().moveX).isEqualTo(-1.0)
        vm.onHardwareKeyEvent(KeyEvent.KEYCODE_A, isDown = false)
        assertThat(vm.currentEffectiveControlInput().moveX).isEqualTo(0.0)

        assertThat(vm.onHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, isDown = true)).isTrue()
        assertThat(vm.currentEffectiveControlInput().moveX).isEqualTo(1.0)
        vm.onHardwareKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, isDown = false)
        assertThat(vm.currentEffectiveControlInput().moveX).isEqualTo(0.0)

        // Gamepad stick Left -> negative moveX; Stick Right -> positive moveX
        assertThat(vm.onGamepadMotion(axisX = -0.85, axisY = 0.0, throttle = 0.6, brake = 0.0)).isTrue()
        assertThat(vm.currentEffectiveControlInput().moveX).isWithin(0.01).of(-0.85)
        assertThat(vm.currentEffectiveControlInput().throttle).isWithin(0.01).of(0.6)

        assertThat(vm.onGamepadMotion(axisX = 0.75, axisY = 0.0, throttle = 0.0, brake = 0.5)).isTrue()
        assertThat(vm.currentEffectiveControlInput().moveX).isWithin(0.01).of(0.75)
        assertThat(vm.currentEffectiveControlInput().brake).isWithin(0.01).of(0.5)

        vm.navigateTo(com.example.domain.AppRoute.MainMenu)
    }

    @Test
    fun world20StationsAndLandmarksAlignCleanlyAcrossGridSizesAndWorkEndToEnd() {
        for (gridSide in listOf(64, 128)) {
            val world = createNewWorld(0, "Fazenda 2.0", "map_pr", 500000.0, "Padrão", gridSide)

            // Every Establishment & Landmark matches an actual 3D building and has zero colliding trees/rocks on apron
            for (est in ESTABLISHMENTS) {
                val sx = est.scaledWorldX(gridSide)
                val sz = est.scaledWorldZ(gridSide)
                val hasBuilding = world.buildings.any { hypot(it.x - sx, it.z - sz) <= 10.0 }
                assertThat(hasBuilding).isTrue()
                val propOnApron = world.props.any { hypot(it.x - sx, it.z - sz) < 12.0 }
                assertThat(propOnApron).isFalse()
            }
            for (lm in LANDMARKS) {
                val lx = lm.scaledWorldX(gridSide)
                val lz = lm.scaledWorldZ(gridSide)
                val hasLandmarkStructure = world.buildings.any { hypot(it.x - lx, it.z - lz) <= 16.0 }
                assertThat(hasLandmarkStructure).isTrue()
                if (lm.id != "lm_garagem" && lm.id != "lm_silo_central") {
                    val propNearLm = world.props.any { hypot(it.x - lx, it.z - lz) < 10.0 }
                    assertThat(propNearLm).isFalse()
                }
            }
        }

        // Functional economy systems: Bank deposit / withdraw / loan, Posto refuel, Weighbridge trailer sale, Dealership spawn
        var world = createNewWorld(0, "Fazenda Economia", "map_pr", 300000.0, "Padrão", 64)
        world = depositToBank(world, 50000.0)
        assertThat(world.moneyBrl).isWithin(0.1).of(250000.0)
        assertThat(world.bankBalanceBrl).isWithin(0.1).of(50000.0)

        world = withdrawFromBank(world, 20000.0)
        assertThat(world.moneyBrl).isWithin(0.1).of(270000.0)
        assertThat(world.bankBalanceBrl).isWithin(0.1).of(30000.0)

        world = takeBankLoan(world, 40000.0)
        assertThat(world.bankLoanBrl).isWithin(0.1).of(40000.0)
        world = repayBankLoan(world, 15000.0)
        assertThat(world.bankLoanBrl).isWithin(0.1).of(25000.0)

        // Move active tractor next to Fuel Station and verify fueling works & charges money
        val fuelStation = world.buildings.first { it.type == BuildingType.POSTO_COMBUSTIVEL }
        val lowFuelTractor = world.vehicles.first().copy(x = fuelStation.x, z = fuelStation.z - 6.0, fuelL = 12.0)
        world = world.copy(vehicles = listOf(lowFuelTractor) + world.vehicles.drop(1), activeVehicleUid = lowFuelTractor.uid)
        assertThat(findNearbyFuelSource(world, lowFuelTractor.x, lowFuelTractor.z)).isNotNull()
        val cashBeforeFuel = world.moneyBrl
        world = refuelVehicleInWorld(world, fullTank = true)
        assertThat(world.vehicles.first { it.uid == lowFuelTractor.uid }.fuelL).isGreaterThan(100.0)
        assertThat(world.moneyBrl).isLessThan(cashBeforeFuel)

        // Move tractor + loaded trailer onto Cooperative weighbridge and sell cargo directly
        val coop = ESTABLISHMENTS.first { it.id == "coop_central" }
        val sx = coop.scaledWorldX(64)
        val sz = coop.scaledWorldZ(64)
        val trailer = OwnedImplement(
            uid = "imp_grain_sale",
            defId = "carreta_12t",
            x = sx,
            z = sz - 3.5,
            headingDeg = 0.0,
            fillL = 5000.0,
            fillProductId = "soja",
        )
        val truckAtScale = world.vehicles.first { it.uid == lowFuelTractor.uid }.copy(
            x = sx,
            z = sz,
            attachedImplementUid = trailer.uid,
        )
        world = world.copy(
            vehicles = listOf(truckAtScale) + world.vehicles.filterNot { it.uid == truckAtScale.uid },
            implements = world.implements + trailer,
            activeVehicleUid = truckAtScale.uid,
        )
        val cashBeforeSale = world.moneyBrl
        world = sellCargoAtNearbyStation(world)
        assertThat(world.moneyBrl).isGreaterThan(cashBeforeSale)
        assertThat(world.lastWeighbridgeTicket).isNotNull()
        assertThat(world.implements.first { it.uid == trailer.uid }.fillL).isEqualTo(0.0)

        // Buying a vehicle spawns it cleanly on the homestead garage apron without overlapping starter machines
        val countBeforeBuy = world.vehicles.size
        world = buyVehicleInWorld(world, "agrale_5055")
        assertThat(world.vehicles.size).isEqualTo(countBeforeBuy + 1)
        val newest = world.vehicles.last()
        assertThat(newest.z).isWithin(14.0).of(22.0)
    }
}
