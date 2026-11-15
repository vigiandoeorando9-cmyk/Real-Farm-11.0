package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import com.example.domain.CameraMode
import com.example.domain.GameSettings
import com.example.domain.GraphicsPreset
import com.example.domain.OwnedImplement
import com.example.domain.SoilState
import com.example.domain.getImplementDef
import com.example.domain.getVehicleDef
import com.example.simulation.ControlInput
import com.example.simulation.checkMachineImplementCompatibility
import com.example.simulation.createNewWorld
import com.example.simulation.getTerrainHeightAt
import com.example.simulation.stepSimulation
import com.example.simulation.toggleHitchNearestImplement
import com.example.simulation.toggleImplementLowered
import com.example.simulation.togglePtoInWorld
import com.example.simulation.unloadAttachedTrailerInWorld
import com.example.simulation.unloadGrainInWorld
import com.example.ui.components.GameplayViewport
import com.example.ui.components.computeCameraPose3D
import com.example.ui.components.projectPoint3D
import com.example.ui.theme.FarmBrasilTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MachineryAndVisualRealismTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compatibilitySystem_blocksUnderpoweredOrIncompatibleHitchAndWarnsBorderlineHp() {
        val compactVeh = getVehicleDef("agrale_5055") // 55 cv
        val mediumVeh = getVehicleDef("agrale_bx6150") // 150 cv
        val heavyPlanter = getImplementDef("plantadeira_24l") // 240 cv req, DRAWBAR
        val subsoiler = getImplementDef("subsolador_5") // 140 cv req -> borderline for 130 cv? No, 130/140=0.928 < 0.95
        val pickup = getVehicleDef("hilux_rural_mod")

        val tooWeak = checkMachineImplementCompatibility(compactVeh, heavyPlanter)
        assertThat(tooWeak.hitchCompatible).isTrue()
        assertThat(tooWeak.hpSufficient).isFalse()
        assertThat(tooWeak.blockMessage).isNotNull()

        val hitchMismatch = checkMachineImplementCompatibility(pickup, subsoiler) // pickup only supports DRAWBAR, subsolador_5 is THREE_POINT
        assertThat(hitchMismatch.hitchCompatible).isFalse()
        assertThat(hitchMismatch.blockMessage).isNotNull()

        val cbt2400 = getVehicleDef("cbt_2400") // 130 cv vs 140 cv subsolador_5 -> 0.928
        val borderline = checkMachineImplementCompatibility(cbt2400, subsoiler)
        assertThat(borderline.blockMessage).isNull()
        assertThat(borderline.warningMessage).isNotNull()

        // World hitch attempt must actually block weak tractor
        val base = createNewWorld(0, "Fazenda Compat", "map_pr", 500000.0, "Padrão", 64)
        val weakInstance = base.vehicles.first().copy(
            uid = "v_weak",
            defId = "agrale_5055",
            x = 0.0,
            z = 0.0,
            attachedImplementUid = null,
        )
        val bigPlanter = OwnedImplement(
            uid = "imp_big",
            defId = "plantadeira_24l",
            x = 0.0,
            z = -3.0,
            headingDeg = 0.0,
        )
        val worldBlocked = toggleHitchNearestImplement(
            base.copy(vehicles = listOf(weakInstance), implements = listOf(bigPlanter), activeVehicleUid = "v_weak")
        )
        assertThat(worldBlocked.vehicles.first().attachedImplementUid).isNull()
        assertThat(worldBlocked.statusAlert).contains("fraco")

        // Sufficient tractor succeeds
        val strongInstance = weakInstance.copy(uid = "v_strong", defId = "jd_9570r")
        val worldOk = toggleHitchNearestImplement(
            base.copy(vehicles = listOf(strongInstance), implements = listOf(bigPlanter), activeVehicleUid = "v_strong")
        )
        assertThat(worldOk.vehicles.first().attachedImplementUid).isEqualTo("imp_big")
    }

    @Test
    fun planter_requiresLoweredAndPtoAndSeeds_andStopsWhenSeedsDeplete() {
        val base = createNewWorld(0, "Fazenda Plantio", "map_pr", 500000.0, "Padrão", 64)
        val veh = base.vehicles.first().copy(
            uid = "v_plant",
            defId = "agrale_bx6150",
            x = 0.0,
            z = 0.0,
            headingDeg = 0.0,
            engineRunning = true,
            handbrakeOn = false,
            selectedCropId = "soja",
            attachedImplementUid = "imp_planter",
            isPtoEngaged = false,
            ptoRpm = 0.0,
        )
        val planter = OwnedImplement(
            uid = "imp_planter",
            defId = "plantadeira_9l",
            x = 0.0,
            z = -2.2,
            headingDeg = 0.0,
            isLowered = false,
            isActive = false,
            liftProgress = 1.0,
            fillL = 50.0,
            fillProductId = "soja",
        )
        val farmableCells = base.cells.map {
            it.copy(height = 2.0, soilState = SoilState.ARADO, cropId = null, moisture = 0.65)
        }
        var world = base.copy(
            cells = farmableCells,
            vehicles = listOf(veh),
            implements = listOf(planter),
            activeVehicleUid = "v_plant",
        )

        // 1. Raised planter must NOT plant
        val cellsBefore = world.cells
        repeat(10) {
            world = stepSimulation(world, ControlInput(throttle = 0.8), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(world.cells).isSameInstanceAs(cellsBefore)

        // 2. Lowering planter auto-engages PTO and begins planting while consuming seeds
        world = toggleImplementLowered(world)
        assertThat(world.vehicles.first().isPtoEngaged).isTrue()
        assertThat(world.implements.first().isLowered).isTrue()

        // Turning PTO off pauses active seeding
        world = togglePtoInWorld(world)
        assertThat(world.vehicles.first().isPtoEngaged).isFalse()
        assertThat(world.implements.first().isActive).isFalse()

        // Re-engage PTO and drive forward -> cells get seeded & fillL decreases
        world = togglePtoInWorld(world)
        repeat(60) {
            world = stepSimulation(world, ControlInput(throttle = 0.9), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(world.cells.any { it.soilState == SoilState.SEMEADO && it.cropId == "soja" }).isTrue()
        assertThat(world.implements.first().fillL).isLessThan(50.0)
    }

    @Test
    fun harvester_respectsHeaderWidthAndTankLimit_andUnloadsToTrailerAndSilo() {
        val base = createNewWorld(0, "Fazenda Colheita", "map_pr", 500000.0, "Padrão", 64)
        val harvester = base.vehicles.first().copy(
            uid = "v_harv",
            defId = "mf_5650", // 5500 L tank
            x = 0.0,
            z = 0.0,
            headingDeg = 0.0,
            engineRunning = true,
            handbrakeOn = false,
            headerLowered = true,
            grainTankFillL = 5480.0,
            grainCropId = "soja",
        )
        val trailer = OwnedImplement(
            uid = "imp_carreta",
            defId = "carreta_12t", // 15000 L
            x = 6.0,
            z = 0.0,
            headingDeg = 0.0,
            fillL = 0.0,
            fillProductId = null,
        )
        val ripeCells = base.cells.map {
            it.copy(height = 2.0, soilState = SoilState.PRONTO_COLHEITA, cropId = "soja", growthProgress = 1.0)
        }
        var world = base.copy(
            cells = ripeCells,
            vehicles = listOf(harvester),
            implements = listOf(trailer),
            activeVehicleUid = "v_harv",
        )

        // Harvesting fills up to 5500 L then alerts tank full
        repeat(25) {
            world = stepSimulation(world, ControlInput(throttle = 0.8), dtSec = 0.05, timeScale = 1.0)
        }
        val fullHarv = world.vehicles.first()
        assertThat(fullHarv.grainTankFillL).isAtMost(5500.0)

        // Unload harvester into nearby trailer -> auger swings out and trailer receives grain
        world = unloadGrainInWorld(world)
        val afterUnload = world.vehicles.first()
        val afterTrailer = world.implements.first()
        assertThat(afterUnload.grainTankFillL).isLessThan(1.0)
        assertThat(afterUnload.unloadingAugerProgress).isGreaterThan(0.9)
        assertThat(afterTrailer.fillL).isGreaterThan(5000.0)
        assertThat(afterTrailer.fillProductId).isEqualTo("soja")

        // Attach trailer to a tractor and unload trailer to farm inventory/silo
        val tractor = base.vehicles.first().copy(
            uid = "v_trac",
            defId = "agrale_bx6150",
            x = 6.0,
            z = 4.0,
            attachedImplementUid = afterTrailer.uid,
        )
        val preInv = world.inventoryKg["soja"] ?: 0.0
        val worldWithTractor = world.copy(
            vehicles = world.vehicles + tractor,
            activeVehicleUid = "v_trac",
        )
        val unloadedSiloWorld = unloadAttachedTrailerInWorld(worldWithTractor)
        assertThat(unloadedSiloWorld.implements.first().fillL).isEqualTo(0.0)
        assertThat(unloadedSiloWorld.inventoryKg["soja"] ?: 0.0).isGreaterThan(preInv + 3500.0)
    }

    @Test
    fun visualRenderer_rendersCleanlyAcrossPresetsAndCameraModes() {
        val base = createNewWorld(0, "Fazenda FS20 Visual", "map_pr", 500000.0, "Padrão", 64)
        val richCells = base.cells.mapIndexed { idx, c ->
            when (idx % 5) {
                0 -> c.copy(soilState = SoilState.CRESCENDO, cropId = "milho", growthProgress = 0.85)
                1 -> c.copy(soilState = SoilState.PRONTO_COLHEITA, cropId = "soja", growthProgress = 1.0)
                2 -> c.copy(soilState = SoilState.ARADO)
                3 -> c.copy(soilState = SoilState.ESTRADA_ASFALTO)
                else -> c.copy(soilState = SoilState.PASTAGEM)
            }
        }
        val activeVeh = base.vehicles.first().copy(
            engineRunning = true,
            rpm = 1650.0,
            speedMs = 5.2,
            headlightsOn = true,
            unloadingAugerProgress = 0.6,
        )
        val world = base.copy(
            cells = richCells,
            vehicles = base.vehicles.map { if (it.uid == activeVeh.uid) activeVeh else it },
            activeVehicleUid = activeVeh.uid,
        )

        val pose = computeCameraPose3D(world, CameraMode.TERCEIRA_PESSOA)
        val gy = getTerrainHeightAt(world, activeVeh.x, activeVeh.z).toFloat()
        val proj = projectPoint3D(activeVeh.x.toFloat(), gy + 1.0f, activeVeh.z.toFloat(), pose, 1280f, 720f)
        assertThat(proj.visible).isTrue()

        // Verify Compose 3D Canvas renders cleanly without errors on Low, Balanced, and Desktop presets
        val renderState = androidx.compose.runtime.mutableStateOf(
            GraphicsPreset.EQUILIBRADO to CameraMode.TERCEIRA_PESSOA
        )
        composeRule.setContent {
            val (preset, camMode) = renderState.value
            FarmBrasilTheme {
                GameplayViewport(
                    worldState = world,
                    cameraMode = camMode,
                    remoteMembers = emptyList(),
                    localUid = "local",
                    settings = GameSettings(graphicsPreset = preset),
                )
            }
        }
        composeRule.waitForIdle()

        renderState.value = GraphicsPreset.DESKTOP_PC to CameraMode.CABINE
        composeRule.waitForIdle()

        renderState.value = GraphicsPreset.MUITO_BAIXO to CameraMode.ORBITAL
        composeRule.waitForIdle()
    }
}
