package com.example

import com.example.audio.VehicleAudioSnapshot
import com.example.audio.resolveEngineAudioProfile
import com.example.domain.OwnedImplement
import com.example.domain.SoilState
import com.example.domain.TransmissionType
import com.example.domain.WorldState
import com.example.domain.getVehicleDef
import com.example.domain.getVehiclePhysicsSpec
import com.example.simulation.ControlInput
import com.example.simulation.createNewWorld
import com.example.simulation.resolveVehicleDrivetrainSpec
import com.example.simulation.stepSimulation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class VehiclePhysicsRealismTest {

    private fun spawnSingleVehicleWorld(
        defId: String,
        x: Double = 0.0,
        z: Double = 0.0,
        soilState: SoilState = SoilState.ESTRADA_ASFALTO,
        terrainMoisture: Double = 0.25,
        slopeBump: Boolean = false,
        extraImplements: List<OwnedImplement> = emptyList(),
        attachedImplementUid: String? = null,
    ): WorldState {
        val base = createNewWorld(0, "Fazenda Física", "map_pr", 500000.0, "Padrão", 64)
        val template = base.vehicles.first()
        val veh = template.copy(
            uid = "v_test",
            defId = defId,
            x = x,
            z = z,
            headingDeg = 0.0,
            speedMs = 0.0,
            gear = 0,
            rpm = 800.0,
            fuelL = getVehicleDef(defId).fuelCapacityL,
            engineRunning = true,
            handbrakeOn = false,
            attachedImplementUid = attachedImplementUid,
        )
        val updatedCells = base.cells.mapIndexed { idx, cell ->
            val gz = idx / 64
            val h = if (slopeBump) gz * 0.65 else 2.0
            cell.copy(
                height = h,
                soilState = soilState,
                moisture = terrainMoisture,
                compaction = 0.2,
            )
        }
        return base.copy(
            cells = updatedCells,
            buildings = emptyList(),
            props = emptyList(),
            roadVehicles = emptyList(),
            vehicles = listOf(veh),
            implements = extraImplements,
            activeVehicleUid = veh.uid,
        )
    }

    @Test
    fun distinctVehicleClasses_differInTransmissionTopSpeedAndRpm() {
        val compactDef = getVehicleDef("agrale_5055")
        val heavyArticDef = getVehicleDef("jd_9570r")
        val truckDef = getVehicleDef("mb_1113")
        val combineDef = getVehicleDef("jd_s770")

        val compactDt = resolveVehicleDrivetrainSpec(compactDef, getVehiclePhysicsSpec(compactDef))
        val heavyDt = resolveVehicleDrivetrainSpec(heavyArticDef, getVehiclePhysicsSpec(heavyArticDef))
        val truckDt = resolveVehicleDrivetrainSpec(truckDef, getVehiclePhysicsSpec(truckDef))
        val combineDt = resolveVehicleDrivetrainSpec(combineDef, getVehiclePhysicsSpec(combineDef))

        assertThat(compactDt.transmissionType).isEqualTo(TransmissionType.MANUAL_6)
        assertThat(heavyDt.transmissionType).isEqualTo(TransmissionType.CVT_HYDRO)
        assertThat(truckDt.transmissionType).isEqualTo(TransmissionType.TRUCK_12_MANUAL)
        assertThat(combineDt.transmissionType).isEqualTo(TransmissionType.CVT_HYDRO)
        assertThat(truckDt.hasAirBrakes).isTrue()
        assertThat(compactDt.hasAirBrakes).isFalse()

        var wCompact = spawnSingleVehicleWorld("agrale_5055", z = -105.0)
        var wTruck = spawnSingleVehicleWorld("mb_1113", z = -105.0)
        var wHeavy = spawnSingleVehicleWorld("jd_9570r", z = -105.0)

        repeat(180) {
            wCompact = stepSimulation(wCompact, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
            wTruck = stepSimulation(wTruck, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
            wHeavy = stepSimulation(wHeavy, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }

        val vCompact = wCompact.vehicles.first()
        val vTruck = wTruck.vehicles.first()
        val vHeavy = wHeavy.vehicles.first()

        assertThat(vTruck.speedMs).isGreaterThan(vCompact.speedMs * 1.45)
        assertThat(vHeavy.gear).isGreaterThan(1)
        assertThat(vCompact.rpm).isGreaterThan(compactDt.idleRpm)
        assertThat(vTruck.gear).isGreaterThan(3)
    }

    @Test
    fun heavyImplementAndLoadedCargo_reduceAccelerationAndIncreaseBrakingDistance() {
        val loadedTrailer = OwnedImplement(
            uid = "imp_loaded",
            defId = "carreta_bazuca24t",
            x = 0.0,
            z = -4.0,
            headingDeg = 0.0,
            fillL = 26000.0,
            fillProductId = "soja",
        )
        var wEmpty = spawnSingleVehicleWorld("jd_9570r", z = -105.0)
        var wLoaded = spawnSingleVehicleWorld(
            defId = "jd_9570r",
            z = -105.0,
            extraImplements = listOf(loadedTrailer),
            attachedImplementUid = loadedTrailer.uid,
        )

        // Accelerate both from standstill for 3.5 seconds
        repeat(70) {
            wEmpty = stepSimulation(wEmpty, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
            wLoaded = stepSimulation(wLoaded, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        val emptyAccelSpeed = wEmpty.vehicles.first().speedMs
        val loadedAccelSpeed = wLoaded.vehicles.first().speedMs
        assertThat(emptyAccelSpeed).isGreaterThan(loadedAccelSpeed * 1.15)
        assertThat(wLoaded.vehicles.first().engineLoadPct ?: 0.0)
            .isGreaterThan(wEmpty.vehicles.first().engineLoadPct ?: 0.0)

        // Set identical high speed (11 m/s) and test full braking distance
        var brakeEmpty = wEmpty.copy(
            vehicles = wEmpty.vehicles.map { it.copy(speedMs = 11.0, x = 0.0, z = -105.0) }
        )
        var brakeLoaded = wLoaded.copy(
            vehicles = wLoaded.vehicles.map { it.copy(speedMs = 11.0, x = 0.0, z = -105.0) }
        )
        repeat(60) {
            brakeEmpty = stepSimulation(brakeEmpty, ControlInput(brake = 1.0), dtSec = 0.05, timeScale = 1.0)
            brakeLoaded = stepSimulation(brakeLoaded, ControlInput(brake = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        assertThat(brakeLoaded.vehicles.first().z).isGreaterThan(brakeEmpty.vehicles.first().z)
    }

    @Test
    fun uphillSlopeAndGroundEngagingTillage_causeRealisticLoadAndSpeedDrop() {
        val plow = OwnedImplement(
            uid = "imp_plow",
            defId = "subsolador_5",
            x = 0.0,
            z = -2.0,
            headingDeg = 0.0,
            isLowered = true,
            liftProgress = 0.0,
            isActive = true,
        )
        var wFlat = spawnSingleVehicleWorld(
            defId = "agrale_bx6180",
            soilState = SoilState.PASTAGEM,
            slopeBump = false,
            extraImplements = listOf(plow),
            attachedImplementUid = plow.uid,
        )
        var wHill = spawnSingleVehicleWorld(
            defId = "agrale_bx6180",
            soilState = SoilState.PASTAGEM,
            slopeBump = true,
            extraImplements = listOf(plow),
            attachedImplementUid = plow.uid,
        )

        repeat(100) {
            wFlat = stepSimulation(wFlat, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
            wHill = stepSimulation(wHill, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }

        val vFlat = wFlat.vehicles.first()
        val vHill = wHill.vehicles.first()
        assertThat(vFlat.speedMs).isGreaterThan(vHill.speedMs)
        assertThat(vHill.pitchDeg).isLessThan(-1.0)
        assertThat(vHill.engineLoadPct ?: 0.0).isGreaterThan(25.0)
    }

    @Test
    fun mudTractionVsAsphalt_causesHigherWheelSlipAndLowerGrip() {
        var wAsphalt = spawnSingleVehicleWorld("agrale_bx6150", soilState = SoilState.ESTRADA_ASFALTO, terrainMoisture = 0.15)
        var wMud = spawnSingleVehicleWorld("agrale_bx6150", soilState = SoilState.ARADO, terrainMoisture = 0.94)

        repeat(80) {
            wAsphalt = stepSimulation(wAsphalt, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
            wMud = stepSimulation(wMud, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }

        val vAsphalt = wAsphalt.vehicles.first()
        val vMud = wMud.vehicles.first()
        assertThat(vMud.surfaceType).isEqualTo("lama")
        assertThat(vAsphalt.surfaceType).isEqualTo("asfalto")
        assertThat(vMud.wheelSlip).isGreaterThan(vAsphalt.wheelSlip)
        assertThat(vAsphalt.speedMs).isGreaterThan(vMud.speedMs)
    }

    @Test
    fun corneringAndCabSuspension_produceArticulationLateralRollAndCabOscillation() {
        var wArtic = spawnSingleVehicleWorld("jd_9570r", soilState = SoilState.PASTAGEM, terrainMoisture = 0.4)

        // Accelerate first, then steer hard
        repeat(60) {
            wArtic = stepSimulation(wArtic, ControlInput(throttle = 1.0), dtSec = 0.05, timeScale = 1.0)
        }
        repeat(35) {
            wArtic = stepSimulation(wArtic, ControlInput(throttle = 0.9, moveX = 1.0), dtSec = 0.04, timeScale = 1.0)
        }

        val v = wArtic.vehicles.first()
        assertThat(abs(v.articulatedAngleDeg ?: 0.0)).isGreaterThan(2.0)
        assertThat(abs(v.rollDeg)).isGreaterThan(0.1)
        assertThat(abs(v.cabRollDeg ?: 0.0) + abs(v.cabBounceOffsetY ?: 0.0)).isGreaterThan(0.001)
    }

    @Test
    fun audioEngineSnapshotAndProfile_distinguishTrucksAndHeavyLoad() {
        val truckProf = resolveEngineAudioProfile("Mercedes OM-352 Diesel Clássico", 147.0)
        val compactProf = resolveEngineAudioProfile("MWM 229-3 Aspirado", 48.0)

        assertThat(truckProf.hasAirBrake).isTrue()
        assertThat(truckProf.cylinders).isEqualTo(6)
        assertThat(compactProf.cylinders).isEqualTo(3)
        assertThat(compactProf.hasAirBrake).isFalse()

        val snap = VehicleAudioSnapshot(
            inVehicle = true,
            speedMs = 12.0,
            rpm = 1950.0,
            gear = 8,
            engineRunning = true,
            surfaceType = "asfalto",
            soundProfile = "Mercedes OM-352",
            horsepower = 147.0,
            implementActive = false,
            isSprayerActive = false,
            isBraking = true,
            engineLoadPct = 82.0,
            hasAirBrake = true,
        )
        assertThat(snap.engineLoadPct).isEqualTo(82.0)
        assertThat(snap.hasAirBrake).isTrue()
    }
}
