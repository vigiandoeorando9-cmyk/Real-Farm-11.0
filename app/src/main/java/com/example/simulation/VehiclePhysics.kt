package com.example.simulation

import com.example.domain.CELL_SIZE_M
import com.example.domain.CONSERVATION_TIER_META
import com.example.domain.ConservationTier
import com.example.domain.DriveType
import com.example.domain.GRID_SIZE
import com.example.domain.HitchType
import com.example.domain.ImplementCategory
import com.example.domain.ImplementDef
import com.example.domain.OwnedImplement
import com.example.domain.OwnedVehicle
import com.example.domain.SoilState
import com.example.domain.TerrainCell
import com.example.domain.TireCategory
import com.example.domain.TireTrack
import com.example.domain.TransmissionType
import com.example.domain.VehicleBodyStyle
import com.example.domain.VehicleCategory
import com.example.domain.VehicleDef
import com.example.domain.VehiclePhysicsSpec
import com.example.domain.WEATHER_META
import com.example.domain.WHEEL_OPTIONS
import com.example.domain.WheelOption
import com.example.domain.WorldState
import com.example.domain.conservationTierFromPct
import com.example.domain.getHarvesterChassisDimensions
import com.example.domain.getImplementDef
import com.example.domain.getImplementHitchOffset
import com.example.domain.getVehicleDef
import com.example.domain.getVehiclePhysicsSpec
import com.example.domain.resolveVehicleTuning
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class ControlInput(
    val moveX: Double = 0.0,
    val moveY: Double = 0.0,
    val throttle: Double = 0.0,
    val brake: Double = 0.0,
    val running: Boolean = false,
    val handbrake: Boolean = false,
)

val EMPTY_CONTROL_INPUT = ControlInput()

data class TerrainGripSample(
    val gripCoeff: Double,
    val rollingResistance: Double,
    val sinkageM: Double,
    val isMud: Boolean,
    val isDusty: Boolean,
    val trackR: Double,
    val trackG: Double,
    val trackB: Double,
)

data class VehiclePhysicsStepResult(
    val vehicle: OwnedVehicle,
    val implement: OwnedImplement?,
    val newTireTrack: TireTrack?,
    val collisionAlert: String?,
)

data class FuelConsumptionInput(
    val horsepower: Double,
    val category: VehicleCategory,
    val rpm: Double,
    val speedMs: Double,
    val tillageDragFactor: Double,
    val slope: Double,
    val implementWorkingWidthM: Double,
    val isImplementLowered: Boolean,
    val cargoMassKg: Double,
    val wheelSlip: Double,
    val isMud: Boolean,
    val tuningFuelMultiplier: Double,
    val tierFuelPenaltyMultiplier: Double,
    val engineRunning: Boolean,
)

data class FourWheelTerrainPose(
    val groundY: Double,
    val groundPlaneY: Double,
    val flY: Double,
    val frY: Double,
    val rlY: Double,
    val rrY: Double,
    val pitchDeg: Double,
    val rollDeg: Double,
    val wheelOffsetsY: List<Double>,
    val wheelContactCount: Int,
)

private fun clamp(v: Double, minVal: Double, maxVal: Double): Double = v.coerceIn(minVal, maxVal)
private fun toRad(deg: Double): Double = (deg * PI) / 180.0
private fun toDeg(rad: Double): Double = (rad * 180.0) / PI

fun normalizeAngle180(deg: Double): Double {
    var a = deg % 360.0
    if (a > 180.0) a -= 360.0
    if (a < -180.0) a += 360.0
    return a
}

fun computeFuelBurnRateLh(params: FuelConsumptionInput): Double {
    if (!params.engineRunning) return 0.0
    val hpFactor = max(0.32, params.horsepower / 110.0)
    val catFactor = when (params.category) {
        VehicleCategory.COLHEITADEIRA -> 1.48
        VehicleCategory.PULVERIZADOR_AUTOPROPELIDO -> 1.14
        VehicleCategory.TRATOR_ESTEIRA,
        VehicleCategory.ESCAVADEIRA,
        VehicleCategory.FLORESTAL -> 1.26
        VehicleCategory.CAMINHAO -> 1.12
        VehicleCategory.CAMINHONETE -> 0.56
        VehicleCategory.UTILITARIO_MOTO -> 0.22
        else -> 1.0
    }
    val rpmLoadRatio = clamp((params.rpm - 720.0) / 1880.0, 0.06, 1.18)
    val slopeLoad = when {
        params.slope > 0.015 -> 1.0 + clamp(params.slope * 3.8, 0.0, 0.85)
        params.slope < -0.025 -> 0.78
        else -> 1.0
    }
    val impWidthLoad = if (params.isImplementLowered && params.implementWorkingWidthM > 0.0) {
        1.0 + clamp((params.implementWorkingWidthM - 2.0) * 0.06, 0.0, 0.55)
    } else {
        1.0
    }
    val cargoLoad = 1.0 + clamp(params.cargoMassKg / 18000.0, 0.0, 0.48)
    val tractionLoad = 1.0 + params.wheelSlip * 0.45 + if (params.isMud) 0.22 else 0.0
    val speedDemand = if (abs(params.speedMs) > 0.35) 1.0 else 0.32

    return (1.6 +
        11.2 *
        hpFactor *
        catFactor *
        (0.24 + rpmLoadRatio * 0.76) *
        speedDemand *
        params.tillageDragFactor *
        slopeLoad *
        impWidthLoad *
        cargoLoad *
        tractionLoad) *
        params.tuningFuelMultiplier *
        params.tierFuelPenaltyMultiplier
}

fun getGridSide(state: WorldState): Int = when (state.cells.size) {
    4096 -> 64
    9216 -> 96
    16384 -> 128
    25600 -> 160
    36864 -> 192
    else -> max(GRID_SIZE, floor(sqrt(state.cells.size.toDouble())).toInt())
}

fun getCellSize(gridSide: Int): Int = when {
    gridSide <= 64 -> 4
    gridSide >= 192 -> 8
    else -> CELL_SIZE_M
}

fun worldToGridCoords(wx: Double, wz: Double, gridSide: Int = GRID_SIZE): Pair<Int, Int> {
    val cellSize = getCellSize(gridSide)
    val half = (gridSide * cellSize) / 2.0
    val gx = floor((wx + half) / cellSize).toInt().coerceIn(0, gridSide - 1)
    val gz = floor((wz + half) / cellSize).toInt().coerceIn(0, gridSide - 1)
    return gx to gz
}

fun getCellIndex(wx: Double, wz: Double, gridSide: Int = GRID_SIZE): Int? {
    val cellSize = getCellSize(gridSide)
    val half = (gridSide * cellSize) / 2.0
    val gx = floor((wx + half) / cellSize).toInt()
    val gz = floor((wz + half) / cellSize).toInt()
    if (gx < 0 || gx >= gridSide || gz < 0 || gz >= gridSide) return null
    return gz * gridSide + gx
}

fun getTerrainHeightAt(state: WorldState, wx: Double, wz: Double): Double {
    if (!wx.isFinite() || !wz.isFinite()) return 0.0
    val gridSide = getGridSide(state)
    val cellSize = getCellSize(gridSide)
    val half = (gridSide * cellSize) / 2.0
    if (state.cells.size < gridSide * gridSide) return 0.0

    val fx = clamp((wx + half) / cellSize, 0.0, gridSide - 1.001)
    val fz = clamp((wz + half) / cellSize, 0.0, gridSide - 1.001)
    val gx = floor(fx).toInt()
    val gz = floor(fz).toInt()
    val tx = fx - gx
    val tz = fz - gz

    val gx1 = min(gx + 1, gridSide - 1)
    val gz1 = min(gz + 1, gridSide - 1)

    val h00 = state.cells[gz * gridSide + gx].height
    val h10 = state.cells[gz * gridSide + gx1].height
    val h01 = state.cells[gz1 * gridSide + gx].height
    val h11 = state.cells[gz1 * gridSide + gx1].height

    val hx0 = h00 + (h10 - h00) * tx
    val hx1 = h01 + (h11 - h01) * tx
    return hx0 + (hx1 - hx0) * tz
}

fun getTerrainSlopeAlongHeading(state: WorldState, wx: Double, wz: Double, headingDeg: Double): Double {
    val rad = toRad(headingDeg)
    val dx = sin(rad) * 1.8
    val dz = cos(rad) * 1.8
    val hFront = getTerrainHeightAt(state, wx + dx, wz + dz)
    val hBack = getTerrainHeightAt(state, wx - dx, wz - dz)
    return clamp((hFront - hBack) / 3.6, -0.6, 0.6)
}

fun sampleFourWheelTerrainPose(
    state: WorldState,
    x: Double,
    z: Double,
    headingDeg: Double,
    frontAxleZ: Double,
    rearAxleZ: Double,
    halfTrack: Double,
    maxTravelM: Double = 0.16,
    cogHeightM: Double = 1.4,
): FourWheelTerrainPose {
    val rad = toRad(headingDeg)
    val fwdX = sin(rad)
    val fwdZ = cos(rad)
    val rightX = cos(rad)
    val rightZ = -sin(rad)

    val groundY = getTerrainHeightAt(state, x, z)
    val flY = getTerrainHeightAt(state, x - rightX * halfTrack + fwdX * frontAxleZ, z - rightZ * halfTrack + fwdZ * frontAxleZ)
    val frY = getTerrainHeightAt(state, x + rightX * halfTrack + fwdX * frontAxleZ, z + rightZ * halfTrack + fwdZ * frontAxleZ)
    val rlY = getTerrainHeightAt(state, x - rightX * halfTrack + fwdX * rearAxleZ, z - rightZ * halfTrack + fwdZ * rearAxleZ)
    val rrY = getTerrainHeightAt(state, x + rightX * halfTrack + fwdX * rearAxleZ, z + rightZ * halfTrack + fwdZ * rearAxleZ)

    val wheelbaseSpan = max(frontAxleZ - rearAxleZ, 1.2)
    val trackSpan = max(halfTrack * 2.0, 0.76)

    val wheelAvgY = (flY + frY + rlY + rrY) * 0.25
    val frontAvg = (flY + frY) * 0.5
    val rearAvg = (rlY + rrY) * 0.5
    val leftAvg = (flY + rlY) * 0.5
    val rightAvg = (frY + rrY) * 0.5

    val wheelPitchSlope = clamp((frontAvg - rearAvg) / wheelbaseSpan, -0.68, 0.68)
    val wheelRollSlope = clamp((rightAvg - leftAvg) / trackSpan, -0.68, 0.68)

    val rawPitchDeg = clamp(-toDeg(atan2(wheelPitchSlope, 1.0)), -26.0, 26.0)
    val rawRollDeg = clamp(toDeg(atan2(wheelRollSlope, 1.0)), -24.0, 24.0)

    val pSin = sin(toRad(rawPitchDeg))
    val rSin = sin(toRad(rawRollDeg))
    fun planeAt(wx: Double, wz: Double) = wheelAvgY - wz * pSin + wx * rSin

    val corners = listOf(
        Triple(-halfTrack, frontAxleZ, flY),
        Triple(halfTrack, frontAxleZ, frY),
        Triple(-halfTrack, rearAxleZ, rlY),
        Triple(halfTrack, rearAxleZ, rrY),
    )

    var maxExcess = 0.0
    for ((wx, wz, wy) in corners) {
        val excess = wy - planeAt(wx, wz)
        if (excess > maxExcess) maxExcess = excess
    }
    val centerBellyExcess = groundY - wheelAvgY
    val lift = max(0.0, max(maxExcess * 0.45, centerBellyExcess * 0.65))
    val groundPlaneY = wheelAvgY + lift

    val offsets = mutableListOf(0.0, 0.0, 0.0, 0.0)
    var contactCount = 0
    val minOff = -maxTravelM * 1.35
    val maxOff = maxTravelM * 1.15
    for (i in 0 until 4) {
        val (wx, wz, wy) = corners[i]
        val planeCornerY = groundPlaneY - wz * pSin + wx * rSin
        val rawOffset = wy - planeCornerY
        val clampedOffset = clamp(rawOffset, minOff, maxOff)
        offsets[i] = clampedOffset
        if (abs(rawOffset - clampedOffset) <= 0.055) {
            contactCount++
        }
    }

    val cogDamp = if (cogHeightM > 1.8) 0.96 else 1.0
    return FourWheelTerrainPose(
        groundY = groundY,
        groundPlaneY = groundPlaneY,
        flY = flY,
        frY = frY,
        rlY = rlY,
        rrY = rrY,
        pitchDeg = rawPitchDeg * cogDamp,
        rollDeg = rawRollDeg * cogDamp,
        wheelOffsetsY = offsets,
        wheelContactCount = contactCount,
    )
}

fun evaluateTerrainGrip(
    state: WorldState,
    cell: TerrainCell?,
    spec: VehiclePhysicsSpec,
    wheel: WheelOption,
): TerrainGripSample {
    val soil = cell?.soilState ?: SoilState.PASTAGEM
    val rain = WEATHER_META[state.weather]?.rainIntensity ?: 0.0
    val moisture = clamp((cell?.moisture ?: 0.5) + rain * 0.35, 0.0, 1.0)

    var baseGrip = 0.62
    when (soil) {
        SoilState.ESTRADA_ASFALTO, SoilState.PONTE -> {
            baseGrip = if (rain > 0.25) 0.78 - rain * 0.14 else 1.0
        }
        SoilState.ESTRADA_TERRA -> baseGrip = if (moisture > 0.72) 0.56 else 0.86
        SoilState.PASTAGEM -> baseGrip = if (moisture > 0.74) 0.54 else 0.80
        SoilState.PALHADA -> baseGrip = if (moisture > 0.75) 0.52 else 0.76
        SoilState.ARADO,
        SoilState.SUBSOLADO,
        SoilState.SEMEADO,
        SoilState.CRESCENDO,
        SoilState.PRONTO_COLHEITA -> baseGrip = if (moisture > 0.72) 0.42 else 0.68
        SoilState.PEDRAS_MATO -> baseGrip = 0.60
        else -> Unit
    }

    val tireSoilMult = when (spec.tireCategory) {
        TireCategory.HIGHWAY -> when (soil) {
            SoilState.ESTRADA_ASFALTO, SoilState.PONTE -> 1.08
            SoilState.ESTRADA_TERRA -> 0.86
            else -> 0.62
        }
        TireCategory.TRACKS -> if (soil == SoilState.ESTRADA_ASFALTO) 0.96 else 1.28
        TireCategory.FORESTRY -> 1.1
        TireCategory.AGRICULTURAL -> if (soil == SoilState.ESTRADA_ASFALTO) 0.95 else 1.05
    }

    val isSoftSoil = soil == SoilState.ARADO ||
        soil == SoilState.SUBSOLADO ||
        soil == SoilState.SEMEADO ||
        soil == SoilState.ESTRADA_TERRA ||
        soil == SoilState.PASTAGEM
    val isMud = isSoftSoil && (moisture > 0.72 || rain > 0.48)
    val isDusty = isSoftSoil && moisture < 0.56 && rain < 0.08

    val finalGrip = clamp(baseGrip * tireSoilMult * wheel.tractionBonus, 0.24, 1.25)
    var rollingResistance = 0.65
    if (soil == SoilState.ESTRADA_ASFALTO || soil == SoilState.PONTE) rollingResistance = 0.25
    else if (isMud) rollingResistance = 1.55
    else if (soil == SoilState.ARADO || soil == SoilState.SUBSOLADO) rollingResistance = 1.15

    var sinkageM = 0.0
    if (spec.tireCategory == TireCategory.TRACKS && isMud) sinkageM = 0.04
    else if (isMud && soil != SoilState.ESTRADA_TERRA) sinkageM = 0.11
    else if (isMud) sinkageM = 0.08
    else if (soil == SoilState.ARADO || soil == SoilState.SUBSOLADO) sinkageM = 0.055
    sinkageM *= (1.0 - wheel.compactionPenaltyReduction * 0.45)

    var trackR = 0.24
    var trackG = 0.18
    var trackB = 0.12
    if (isMud) {
        trackR = 0.14
        trackG = 0.09
        trackB = 0.06
    } else if (soil == SoilState.ARADO || soil == SoilState.SUBSOLADO) {
        trackR = 0.18
        trackG = 0.12
        trackB = 0.08
    }

    return TerrainGripSample(finalGrip, rollingResistance, sinkageM, isMud, isDusty, trackR, trackG, trackB)
}

private fun computeCollisionDamagePct(impactSpeedMs: Double): Double = when {
    impactSpeedMs < 2.0 -> 0.0
    impactSpeedMs < 4.5 -> 1.0 + ((impactSpeedMs - 2.0) / 2.5) * 1.0
    impactSpeedMs < 8.5 -> 3.0 + ((impactSpeedMs - 4.5) / 4.0) * 2.0
    impactSpeedMs < 13.5 -> 6.0 + ((impactSpeedMs - 8.5) / 5.0) * 4.0
    else -> min(28.0, 10.0 + (impactSpeedMs - 13.5) * 1.3)
}

private fun updateArticulatedImplement(
    state: WorldState,
    vehDef: VehicleDef,
    veh: OwnedVehicle,
    imp: OwnedImplement,
    impDef: ImplementDef,
    vehSpeedMs: Double,
    grip: TerrainGripSample,
    dt: Double,
): OwnedImplement {
    val vehRad = toRad(veh.headingDeg)
    val isFrontHeader = impDef.hitchType == HitchType.HEADER || impDef.category == ImplementCategory.PLATAFORMA_MILHO
    val tongueLen = getImplementHitchOffset(impDef)
    val isRigid3Point = impDef.hitchType == HitchType.THREE_POINT || isFrontHeader

    val ix: Double
    val iz: Double
    var nextImpHeading = imp.headingDeg
    if (isFrontHeader) {
        val frontDist = vehDef.dimensionsM[0] * 0.48 + 1.10
        ix = veh.x + sin(vehRad) * frontDist
        iz = veh.z + cos(vehRad) * frontDist
        nextImpHeading = veh.headingDeg
    } else {
        val hitchDistBehindTractor = -(vehDef.dimensionsM[0] * 0.48)
        val hitchX = veh.x + sin(vehRad) * hitchDistBehindTractor
        val hitchZ = veh.z + cos(vehRad) * hitchDistBehindTractor

        if (isRigid3Point) {
            val diff = normalizeAngle180(veh.headingDeg - imp.headingDeg)
            nextImpHeading = (imp.headingDeg + diff * min(12.0 * dt, 1.0) + 360.0) % 360.0
        } else if (abs(vehSpeedMs) > 0.15) {
            val headingDiffRad = toRad(normalizeAngle180(veh.headingDeg - imp.headingDeg))
            val trailOmega = (vehSpeedMs / max(tongueLen, 1.2)) * sin(headingDiffRad)
            val rawH = imp.headingDeg + toDeg(trailOmega) * dt
            val rel = clamp(normalizeAngle180(rawH - veh.headingDeg), -75.0, 75.0)
            nextImpHeading = (veh.headingDeg + rel + 360.0) % 360.0
        }

        val impRad = toRad(nextImpHeading)
        ix = hitchX - sin(impRad) * tongueLen
        iz = hitchZ - cos(impRad) * tongueLen
    }

    val impHalfTrack = clamp(impDef.workingWidthM * 0.28, 0.72, 1.85)
    val impPose = sampleFourWheelTerrainPose(
        state, ix, iz, nextImpHeading, 0.85, -1.25, impHalfTrack, 0.14, 0.9
    )
    val impBlend = min(10.0 * dt, 1.0)
    val nextImpPitch = (imp.pitchDeg ?: impPose.pitchDeg) + (impPose.pitchDeg - (imp.pitchDeg ?: impPose.pitchDeg)) * impBlend
    val nextImpRoll = (imp.rollDeg ?: impPose.rollDeg) + (impPose.rollDeg - (imp.rollDeg ?: impPose.rollDeg)) * impBlend

    val deltaSpin = toDeg((vehSpeedMs / 0.45) * dt)
    val ptoActive = veh.isPtoEngaged == true || imp.isActive || (isFrontHeader && veh.headerLowered == true)
    val ptoFactor = clamp((veh.ptoRpm ?: 540.0) / 540.0, 0.4, 1.2)
    val nextRotor = if (ptoActive && (imp.isLowered || impDef.category == ImplementCategory.PULVERIZADOR || impDef.category == ImplementCategory.DISTRIBUIDOR || isFrontHeader)) {
        ((imp.rotorSpinDeg) + 260.0 * ptoFactor * dt) % 360.0
    } else {
        imp.rotorSpinDeg
    }

    val isDiscTool = impDef.category == ImplementCategory.ARADO ||
        impDef.category == ImplementCategory.GRADE ||
        impDef.category == ImplementCategory.CULTIVADOR ||
        impDef.category == ImplementCategory.PLANTADEIRA ||
        impDef.category == ImplementCategory.SEMEADORA
    val nextDiscSpin = if (isDiscTool && imp.isLowered && abs(vehSpeedMs) > 0.15) {
        ((imp.discSpinDeg ?: 0.0) + toDeg((vehSpeedMs / 0.28) * dt)) % 360.0
    } else {
        imp.discSpinDeg ?: 0.0
    }
    val nextAugerSpin = if ((ptoActive && imp.isActive) || isFrontHeader || (impDef.category == ImplementCategory.CARRETA && (veh.unloadingAugerProgress > 0.15))) {
        ((imp.augerSpinDeg ?: 0.0) + 320.0 * dt) % 360.0
    } else {
        imp.augerSpinDeg ?: 0.0
    }

    val boomTarget = if (imp.isLowered || imp.isActive) 1.0 else 0.0
    val nextBoom = imp.boomDeployProgress + (boomTarget - imp.boomDeployProgress) * min(2.4 * dt, 1.0)
    val liftTarget = if (imp.isLowered) 0.0 else 1.0
    val nextLift = clamp(imp.liftProgress + clamp(liftTarget - imp.liftProgress, -1.9 * dt, 1.9 * dt), 0.0, 1.0)
    val targetFurrow = if (imp.isLowered && isGroundEngagingImplement(impDef.category)) {
        when (impDef.category) {
            ImplementCategory.SUBSOLADOR -> 0.36
            ImplementCategory.ARADO -> 0.26
            ImplementCategory.ESCARIFICADOR -> 0.22
            ImplementCategory.GRADE -> 0.14
            ImplementCategory.CULTIVADOR -> 0.12
            else -> 0.08
        } * (1.0 - nextLift)
    } else 0.0

    return imp.copy(
        x = ix,
        z = iz,
        headingDeg = nextImpHeading,
        drawbarYawDeg = normalizeAngle180(nextImpHeading - veh.headingDeg),
        pitchDeg = nextImpPitch,
        rollDeg = nextImpRoll,
        groundPlaneY = impPose.groundPlaneY,
        wheelSpinDeg = (imp.wheelSpinDeg + deltaSpin) % 36000.0,
        rotorSpinDeg = nextRotor,
        discSpinDeg = nextDiscSpin,
        augerSpinDeg = nextAugerSpin,
        furrowDepthM = targetFurrow,
        boomDeployProgress = nextBoom,
        liftProgress = nextLift,
        groundSinkageM = grip.sinkageM * 0.7,
        dirtPct = clamp(imp.dirtPct + (if (imp.isLowered && abs(vehSpeedMs) > 0.4) 0.7 * dt else 0.0), 0.0, 100.0),
    )
}

fun stepVehiclePhysics(
    state: WorldState,
    veh: OwnedVehicle,
    attachedImp: OwnedImplement?,
    input: ControlInput,
    dt: Double,
    steerSpeedMultiplier: Double = 1.0,
): VehiclePhysicsStepResult {
    val def = getVehicleDef(veh.defId)
    val spec = getVehiclePhysicsSpec(def)
    val drivetrain = resolveVehicleDrivetrainSpec(def, spec)
    val wheel = WHEEL_OPTIONS.find { it.id == veh.wheelOptionId } ?: WHEEL_OPTIONS[0]
    val tuning = resolveVehicleTuning(veh, def)
    val effectiveWheel = wheel.copy(
        tractionBonus = tuning.tractionMultiplier,
        compactionPenaltyReduction = tuning.compactionReduction,
    )
    val impDef = attachedImp?.let { getImplementDef(it.defId) }

    val gridSide = getGridSide(state)
    val cellSize = getCellSize(gridSide)
    val worldHalf = (gridSide * cellSize) / 2.0

    val cellIdx = getCellIndex(veh.x, veh.z, gridSide)
    val cellUnder = cellIdx?.let { state.cells.getOrNull(it) }
    val grip = evaluateTerrainGrip(state, cellUnder, spec, effectiveWheel)

    val consPct = clamp(veh.conservationPct, 0.0, 100.0)
    val tier = conservationTierFromPct(consPct)
    val tierMeta = CONSERVATION_TIER_META[tier] ?: CONSERVATION_TIER_META.getValue(ConservationTier.EXCELENTE)

    val engineHealthFactor = 0.65 + 0.35 * (veh.engineHealthPct / 100.0)
    val transHealthFactor = 0.70 + 0.30 * (veh.transmissionHealthPct / 100.0)
    val tireSuspHealthFactor = 0.75 + 0.25 * (veh.suspensionTiresHealthPct / 100.0)

    val vehCargoKg = veh.grainTankFillL * 0.78
    val impBaseKg = impDef?.weightKg ?: 0.0
    val impCargoKg = (attachedImp?.fillL ?: 0.0) * 0.85
    val frontBallastKg = veh.frontBallastKg ?: 0.0
    val ownMassKg = max(def.weightKg + tuning.extraWeightKg + frontBallastKg, 120.0)
    val totalMassKg = max(ownMassKg + vehCargoKg + impBaseKg + impCargoKg, 120.0)
    val massRatio = clamp(ownMassKg / totalMassKg, 0.26, 1.0)
    val ballastBalanceBonus = if (frontBallastKg > 0 && impBaseKg > 0) {
        clamp((frontBallastKg / max(impBaseKg, 1200.0)) * 0.12, 0.0, 0.12)
    } else 0.0

    val effectiveHp = tuning.effectiveHp * tierMeta.powerMultiplier * engineHealthFactor * transHealthFactor

    val impLoad = computeImplementPhysicsLoad(
        vehDef = def,
        effectiveHp = effectiveHp,
        imp = attachedImp,
        impDef = impDef,
        cellUnder = cellUnder,
        speedMs = veh.speedMs,
    )
    val tillageDragFactor = if (attachedImp != null && attachedImp.isLowered && impDef != null) {
        if (impLoad.isGroundEngaging) clamp(impDef.requiredHp / max(effectiveHp, 10.0), 0.45, 1.65) else 0.65
    } else 0.25

    val tb = clamp(input.throttle - input.brake, -1.0, 1.0)
    val rawAccelCmd = if (abs(tb) < 0.05) input.moveY else tb

    val isEngineRunning = (veh.engineRunning ?: true) && veh.fuelL > 0.05 && tier != ConservationTier.IMOBILIZADO
    val handbrakeEngaged = input.handbrake || (veh.handbrakeOn == true)

    var stallTimer = max(veh.stallTimerSec - dt, 0.0)
    if (tierMeta.stutterChancePerSec > 0 && abs(rawAccelCmd) > 0.2 && stallTimer <= 0.0 && isEngineRunning) {
        val phase = ((veh.hoursWorked * 3600.0 + abs(veh.x) + abs(veh.z)) * 1.7) % 10.0
        if (phase < tierMeta.stutterChancePerSec * 2.5) {
            stallTimer = 0.35
        }
    }

    val slope = getTerrainSlopeAlongHeading(state, veh.x, veh.z, veh.headingDeg)
    val slopeGravityMs2 = 9.81 * slope * (0.72 + (1.0 - massRatio) * 0.55)

    // Working implement speed limit + terrain grip limit
    val workSpeedCap = if (attachedImp?.isLowered == true) {
        impLoad.maxRecommendedWorkSpeedMs
    } else if (def.category == VehicleCategory.COLHEITADEIRA && (veh.headerLowered == true)) {
        4.2 // ~15 km/h max realistic combine harvesting speed
    } else {
        99.0
    }
    val nominalMaxMs = def.maxSpeedKmh / 3.6
    var maxSpeedLimitMs = min(
        workSpeedCap,
        nominalMaxMs * (0.78 + 0.22 * tierMeta.powerMultiplier) * clamp(grip.gripCoeff * 1.15, 0.55, 1.06)
    ).coerceAtLeast(1.8)

    // Cruise control ("Velocidade Cruzeiro"): auto-cancel on brake/handbrake/engine-off; otherwise smoothly hold target km/h
    val brakePressed = input.brake > 0.15 || tb < -0.15 || input.moveY < -0.15
    val cruiseTargetKmh = (veh.cruiseTargetKmh ?: 12.0).coerceIn(3.0, def.maxSpeedKmh)
    val cruiseActive = (veh.cruiseControlActive == true) &&
        isEngineRunning &&
        !handbrakeEngaged &&
        !brakePressed

    if (cruiseActive) {
        maxSpeedLimitMs = min(maxSpeedLimitMs, cruiseTargetKmh / 3.6).coerceAtLeast(0.8)
    }

    val effectiveAccelCmd = when {
        !isEngineRunning || stallTimer > 0.12 -> 0.0
        cruiseActive && abs(rawAccelCmd) < 0.10 -> {
            val targetMs = min(cruiseTargetKmh / 3.6, maxSpeedLimitMs)
            val errMs = targetMs - veh.speedMs
            if (errMs > -0.25) clamp(errMs * 0.65 + 0.35, 0.05, 1.0) else 0.0
        }
        else -> rawAccelCmd
    }

    var speed = veh.speedMs
    var wheelSlip = 0.0
    var isBraking = false
    val effectiveGrip = clamp(grip.gripCoeff * tireSuspHealthFactor + ballastBalanceBonus, 0.18, 1.28)
    val has4wdOption = spec.driveType == DriveType.AWD
    val is4wdEngaged = veh.is4wdEngaged ?: has4wdOption
    val isSoftOrField = grip.isMud ||
        grip.isDusty ||
        cellUnder?.soilState == SoilState.ARADO ||
        cellUnder?.soilState == SoilState.SUBSOLADO ||
        cellUnder?.soilState == SoilState.PASTAGEM

    val halfTrack = max(spec.trackWidthM * 0.5, 0.38)
    val prePose = sampleFourWheelTerrainPose(
        state, veh.x, veh.z, veh.headingDeg, spec.frontAxleZ, spec.rearAxleZ, halfTrack,
        spec.suspensionMaxTravelM, spec.cogHeightM
    )
    val contactRatio = max(0.25, prePose.wheelContactCount / 4.0)
    val contactTractionFactor = 0.45 + 0.55 * contactRatio

    val driveMult = when {
        spec.driveType == DriveType.TRACKED -> 1.24
        has4wdOption -> if (is4wdEngaged) (if (isSoftOrField) 1.18 else 1.05) else 0.84
        spec.driveType == DriveType.FWD -> 0.85
        spec.driveType == DriveType.RWD -> 0.82
        else -> 1.02
    }

    // Weight transfer & normal force on driven wheels (front/rear + trailer tongue weight)
    val drivenNormalWeightN = ownMassKg * 9.81 * (if (has4wdOption && is4wdEngaged) 0.94 else 0.62) +
        (impBaseKg + impCargoKg) * 0.18 * 9.81
    val tractionCapForceN = drivenNormalWeightN * effectiveGrip * driveMult * contactTractionFactor
    val tractionCapMs2 = tractionCapForceN / totalMassKg

    // PTO engine load & torque available for wheels
    val ptoEngaged = isEngineRunning && (
        (veh.isPtoEngaged == true) ||
            (attachedImp?.isActive == true && impLoad.usesPto) ||
            (def.category == VehicleCategory.COLHEITADEIRA && veh.headerLowered == true) ||
            (def.category == VehicleCategory.PULVERIZADOR_AUTOPROPELIDO && veh.isSprayerActive == true)
        )
    val ptoDrainKw = when {
        def.category == VehicleCategory.COLHEITADEIRA && veh.headerLowered == true -> max(35.0, effectiveHp * 0.28)
        def.category == VehicleCategory.PULVERIZADOR_AUTOPROPELIDO && veh.isSprayerActive == true -> 18.0
        ptoEngaged -> impLoad.ptoPowerKw
        else -> 0.0
    }
    val ptoDrainRatio = clamp(ptoDrainKw / max(effectiveHp * 0.7355, 8.0), 0.0, 0.55)

    // Current gear & CVT / Powershift / Manual ratio resolution
    val gearCount = drivetrain.gearRatios.size
    val rearWheelR = max(spec.rearWheelRadiusM, 0.30)
    var currentGear = veh.gear
    val isReversingCmd = effectiveAccelCmd < -0.05 && speed <= 0.35
    val isForwardCmd = effectiveAccelCmd > 0.05 && speed >= -0.35

    if (handbrakeEngaged || !isEngineRunning || tier == ConservationTier.IMOBILIZADO) {
        if (abs(speed) < 0.15) currentGear = 0
    } else if (isReversingCmd) {
        currentGear = -1
    } else if (speed < -0.25) {
        currentGear = -1
    } else if (abs(speed) < 0.12 && abs(effectiveAccelCmd) <= 0.05) {
        currentGear = 0
    } else if (isForwardCmd || speed >= 0.12) {
        if (currentGear <= 0) currentGear = 1
        currentGear = currentGear.coerceIn(1, gearCount)
        if (drivetrain.transmissionType == TransmissionType.CVT_HYDRO) {
            val spdNorm = clamp(abs(speed) / max(maxSpeedLimitMs, 1.0), 0.0, 1.0)
            currentGear = (1 + (spdNorm * (gearCount - 1)).roundToInt()).coerceIn(1, gearCount)
        } else {
            val ratioNow = drivetrain.gearRatios[currentGear - 1]
            val wheelRpmNow = (abs(speed) / (2.0 * PI * rearWheelR)) * 60.0
            val coupledRpmNow = wheelRpmNow * ratioNow * drivetrain.finalDriveRatio
            val upshiftThreshold = drivetrain.ratedRpm * drivetrain.upshiftRpmFraction
            val downshiftThreshold = drivetrain.ratedRpm * drivetrain.downshiftRpmFraction

            // Load prevents early upshift and forces downshift when lugging
            val heavyLoad = impLoad.draftForceN > ownMassKg * 1.4 || slope > 0.05
            val effectiveUpshift = if (heavyLoad) drivetrain.ratedRpm * 0.94 else upshiftThreshold
            if (coupledRpmNow > effectiveUpshift && currentGear < gearCount && effectiveAccelCmd > 0.15) {
                currentGear++
            } else if (coupledRpmNow < downshiftThreshold && currentGear > 1) {
                currentGear--
            }
        }
    }

    val activeGearRatio = when {
        currentGear < 0 -> drivetrain.reverseRatio
        currentGear in 1..gearCount -> {
            if (drivetrain.transmissionType == TransmissionType.CVT_HYDRO) {
                val lowR = drivetrain.gearRatios.first()
                val highR = drivetrain.gearRatios.last()
                val curWheelRpm = (abs(speed) / (2.0 * PI * rearWheelR)) * 60.0
                val demandCvtRpm = drivetrain.peakTorqueRpm +
                    (drivetrain.ratedRpm * 0.96 - drivetrain.peakTorqueRpm) * clamp(abs(effectiveAccelCmd), 0.15, 1.0)
                val targetRatio = if (curWheelRpm > 0.5) {
                    demandCvtRpm / max(curWheelRpm * drivetrain.finalDriveRatio, 0.5)
                } else {
                    lowR
                }
                clamp(targetRatio, highR, lowR)
            } else {
                drivetrain.gearRatios[currentGear - 1]
            }
        }
        else -> drivetrain.gearRatios.first()
    }

    // Air brake reservoir pressure management (trucks & heavy tractors)
    var nextAirPressure = veh.airBrakePressurePct ?: 100.0
    if (drivetrain.hasAirBrakes) {
        if (isEngineRunning) {
            nextAirPressure = clamp(nextAirPressure + 8.5 * dt, 35.0, 100.0)
        }
    }

    // Compute coupled RPM from wheel speed vs throttle revving
    val wheelRpm = (abs(speed) / (2.0 * PI * rearWheelR)) * 60.0
    val coupledEngineRpm = wheelRpm * activeGearRatio * drivetrain.finalDriveRatio
    val currentRpm = max(veh.rpm, 0.0)
    val prevSpeed = speed
    var engineLoadPct = 0.0

    val rollingResistForceN = totalMassKg * 9.81 * (0.015 + grip.rollingResistance * 0.028)
    val aeroDragForceN = 0.5 * 1.225 * 0.85 * (def.dimensionsM[1] * def.dimensionsM[2] * 0.72) * speed * abs(speed)
    val totalDragN = rollingResistForceN + aeroDragForceN + impLoad.draftForceN

    if (tier == ConservationTier.IMOBILIZADO) {
        speed *= max(1.0 - 3.5 * dt, 0.0)
        if (abs(speed) < 0.08) speed = 0.0
    } else if (handbrakeEngaged) {
        isBraking = true
        val hbDecel = clamp(12.5 * effectiveGrip * tierMeta.brakeMultiplier, 4.0, 16.0)
        speed = when {
            abs(speed) <= hbDecel * dt + 0.12 -> 0.0
            speed > 0.0 -> max(0.0, speed - hbDecel * dt)
            else -> min(0.0, speed + hbDecel * dt)
        }
        if (drivetrain.hasAirBrakes && abs(prevSpeed) > 0.5) {
            nextAirPressure = max(35.0, nextAirPressure - 6.0 * dt)
        }
    } else if (!isEngineRunning || veh.fuelL <= 0.05) {
        if (rawAccelCmd < -0.05 && speed > 0.2) {
            isBraking = true
            val unpoweredBrake = clamp(6.8 * effectiveGrip * massRatio, 2.0, 8.5)
            speed = max(speed - unpoweredBrake * dt, 0.0)
        } else {
            val dragDecel = (totalDragN / totalMassKg) + 0.55
            val sign = if (speed >= 0) 1.0 else -1.0
            speed = if (abs(speed) > dragDecel * dt) speed - sign * dragDecel * dt else 0.0
            speed -= slopeGravityMs2 * 0.65 * dt
            if (abs(speed) < 0.20 && abs(slope) < 0.34) speed = 0.0
        }
    } else if (effectiveAccelCmd > 0.05) {
        val throttle = effectiveAccelCmd
        val workingRpm = max(drivetrain.idleRpm + 260.0, max(currentRpm, coupledEngineRpm))
        val engTorqueNm = computeEngineTorqueNm(def, tuning, drivetrain, workingRpm, throttle) *
            tierMeta.powerMultiplier * engineHealthFactor * transHealthFactor * (1.0 - ptoDrainRatio * 0.65)
        val wheelTorqueNm = engTorqueNm * activeGearRatio * drivetrain.finalDriveRatio * drivetrain.drivetrainEfficiency
        val rawDriveForceN = wheelTorqueNm / rearWheelR
        val actualDriveForceN = min(rawDriveForceN, tractionCapForceN)

        if (rawDriveForceN > tractionCapForceN * 0.90) {
            wheelSlip = clamp((rawDriveForceN - tractionCapForceN * 0.90) / max(tractionCapForceN, 1.0), 0.05, 0.88)
        }
        if (grip.isMud && abs(speed) > 0.35) {
            val mudSlipBase = if (is4wdEngaged) 0.22 else 0.36
            wheelSlip = max(wheelSlip, clamp(mudSlipBase * (1.15 - effectiveGrip), 0.0, 0.72))
        }

        val netForceN = actualDriveForceN - totalDragN * (if (speed >= -0.05) 1.0 else -1.0)
        val netAccelMs2 = (netForceN / totalMassKg) - slopeGravityMs2
        engineLoadPct = clamp((totalDragN / max(actualDriveForceN, 200.0)) * 75.0 + throttle * 25.0 + ptoDrainRatio * 40.0, 12.0, 100.0)

        // Governor smooth speed ceiling
        val governorSoftCap = if (speed > maxSpeedLimitMs * 0.94) {
            clamp((maxSpeedLimitMs - speed) / max(maxSpeedLimitMs * 0.06, 0.2), 0.0, 1.0)
        } else 1.0
        val effectiveDelta = if (netAccelMs2 > 0.0) netAccelMs2 * governorSoftCap else netAccelMs2
        speed = min(speed + effectiveDelta * dt, maxSpeedLimitMs)
    } else if (effectiveAccelCmd < -0.05) {
        if (speed > 0.35) {
            isBraking = true
            val airMult = if (drivetrain.hasAirBrakes) (0.70 + 0.30 * (nextAirPressure / 100.0)) else 1.0
            val baseBrakeG = if (def.category == VehicleCategory.CAMINHAO) 0.78 else 0.92
            val brakeDecel = clamp(9.81 * baseBrakeG * effectiveGrip * tierMeta.brakeMultiplier * clamp(massRatio, 0.48, 1.0) * airMult, 2.0, 11.5)
            speed = max(speed - brakeDecel * abs(effectiveAccelCmd) * dt, 0.0)
            if (drivetrain.hasAirBrakes) {
                nextAirPressure = max(35.0, nextAirPressure - 5.5 * dt)
            }
        } else {
            // Reverse drive using reverse gear ratio
            val revThrottle = abs(effectiveAccelCmd)
            val workingRpm = max(drivetrain.idleRpm + 220.0, max(currentRpm, coupledEngineRpm))
            val engTorqueNm = computeEngineTorqueNm(def, tuning, drivetrain, workingRpm, revThrottle) *
                tierMeta.powerMultiplier * engineHealthFactor * transHealthFactor
            val wheelTorqueNm = engTorqueNm * drivetrain.reverseRatio * drivetrain.finalDriveRatio * drivetrain.drivetrainEfficiency
            val rawRevForceN = wheelTorqueNm / rearWheelR
            val actualRevForceN = min(rawRevForceN, tractionCapForceN * 0.85)
            val netRevAccel = (actualRevForceN - totalDragN * 0.65) / totalMassKg
            engineLoadPct = clamp(revThrottle * 65.0 + ptoDrainRatio * 35.0, 15.0, 100.0)
            speed = max(speed - netRevAccel * dt - slopeGravityMs2 * dt, -maxSpeedLimitMs * 0.42)
        }
    } else {
        // Coasting with engine braking + rolling/implement drag
        val engineBrakeDrag = if (abs(speed) > 0.5 && currentGear != 0) 0.55 + (currentGear.coerceAtLeast(1) * 0.04) else 0.25
        val coastDragMs2 = (totalDragN / totalMassKg) + engineBrakeDrag
        val sign = if (speed >= 0) 1.0 else -1.0
        val decelSpeed = if (abs(speed) > coastDragMs2 * dt) speed - sign * coastDragMs2 * dt else 0.0
        speed = decelSpeed - slopeGravityMs2 * 0.62 * dt
        if (abs(speed) < 0.22 && abs(rawAccelCmd) < 0.05 && abs(slope) < 0.34) {
            speed = 0.0
        }
        engineLoadPct = if (ptoEngaged) clamp(25.0 + ptoDrainRatio * 65.0, 15.0, 85.0) else 8.0
    }

    var nextBogSeverity = veh.bogSeverity ?: 0.0
    var nextIsBogged = veh.isBogged == true
    val deepMudTrap = grip.isMud &&
        (cellUnder?.moisture ?: 0.0) > 0.74 &&
        (cellUnder?.compaction ?: 0.0) > 0.42 &&
        spec.driveType != DriveType.TRACKED
    if (deepMudTrap && (wheelSlip > 0.42 || (has4wdOption && !is4wdEngaged && abs(speed) > 0.4))) {
        nextBogSeverity = clamp(nextBogSeverity + dt * (if (is4wdEngaged) 0.16 else 0.34), 0.0, 1.0)
        if (nextBogSeverity >= 0.72) nextIsBogged = true
    } else if (nextIsBogged && has4wdOption && is4wdEngaged && abs(effectiveAccelCmd) > 0.35 && effectiveHp >= 135.0) {
        nextBogSeverity = max(0.0, nextBogSeverity - dt * 0.24)
        if (nextBogSeverity < 0.22) {
            nextIsBogged = false
            nextBogSeverity = 0.0
        }
    } else if (!deepMudTrap && !nextIsBogged) {
        nextBogSeverity = max(0.0, nextBogSeverity - dt * 0.45)
    }
    if (nextIsBogged) {
        speed = clamp(speed, -0.45, 0.55)
        wheelSlip = max(wheelSlip, 0.72)
    }

    val speedSteerDamp = if (abs(speed) > 14.0) clamp(14.0 / abs(speed), 0.42, 1.0) else 1.0
    val ptA = veh.gpsPointA
    val ptB = veh.gpsPointB
    val targetSteerDeg = if (veh.gpsAutoSteer && ptA != null && ptB != null && abs(input.moveX) < 0.12) {
        val abAngle = toDeg(atan2(ptB.x - ptA.x, ptB.z - ptA.z))
        var diff = normalizeAngle180(abAngle - veh.headingDeg)
        if (abs(diff) > 90.0) diff = normalizeAngle180(abAngle + 180.0 - veh.headingDeg)
        clamp(diff, -spec.maxSteerAngleDeg, spec.maxSteerAngleDeg)
    } else {
        clamp(input.moveX * spec.maxSteerAngleDeg * speedSteerDamp, -spec.maxSteerAngleDeg, spec.maxSteerAngleDeg)
    }

    val steerSpeedClamped = steerSpeedMultiplier.coerceIn(0.4, 2.2)
    val isReturningToCenter = abs(input.moveX) < 0.05 && !veh.gpsAutoSteer && abs(veh.steerAngleDeg) > 0.25
    val effectiveSteerRate = spec.steerResponseRateDegPerSec * steerSpeedClamped * (if (isReturningToCenter) 1.15 else 1.0)
    val maxSteerDelta = effectiveSteerRate * dt
    val nextSteerDeg = clamp(
        veh.steerAngleDeg + clamp(targetSteerDeg - veh.steerAngleDeg, -maxSteerDelta, maxSteerDelta),
        -spec.maxSteerAngleDeg,
        spec.maxSteerAngleDeg,
    )

    val steerRad = toRad(nextSteerDeg)
    val awdSteerDrag = if (has4wdOption && is4wdEngaged) 0.94 else 1.0
    val turnAuthority = (0.72 + 0.28 * effectiveGrip) * (1.0 - wheelSlip * 0.35) * awdSteerDrag * (0.55 + 0.45 * contactRatio)
    val yawRateRad = if (abs(speed) > 0.18) (speed / max(spec.wheelbaseM, 1.2)) * tan(steerRad) * turnAuthority else 0.0

    // Cornering stability: lateral G induces slip & mild speed scrub on sharp high-speed turns
    val lateralAccelMs2 = abs(speed * yawRateRad)
    val lateralLimitMs2 = effectiveGrip * 9.81 * clamp(spec.trackWidthM / max(spec.cogHeightM * 2.0, 1.2), 0.48, 0.98)
    if (lateralAccelMs2 > lateralLimitMs2 && abs(speed) > 4.0) {
        val excess = clamp((lateralAccelMs2 - lateralLimitMs2) / max(lateralLimitMs2, 1.0), 0.0, 0.75)
        wheelSlip = max(wheelSlip, excess * 0.55)
        speed *= max(1.0 - excess * 0.85 * dt, 0.88)
    }

    val nextHeading = (veh.headingDeg + toDeg(yawRateRad) * dt + 3600.0) % 360.0
    val nextArticulatedDeg = if (def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD) {
        clamp(nextSteerDeg * 0.95, -42.0, 42.0)
    } else 0.0

    val rad = toRad(nextHeading)
    var candX = veh.x + sin(rad) * speed * dt
    var candZ = veh.z + cos(rad) * speed * dt

    var cooldown = max(veh.collisionCooldownSec - dt, 0.0)
    var damagePct = 0.0
    var impactType = ""
    var collisionAlert: String? = null

    if (candX < -worldHalf + 6.0 || candX > worldHalf - 6.0 || candZ < -worldHalf + 6.0 || candZ > worldHalf - 6.0) {
        candX = clamp(candX, -worldHalf + 6.0, worldHalf - 6.0)
        candZ = clamp(candZ, -worldHalf + 6.0, worldHalf - 6.0)
        if (abs(speed) > 2.2 && cooldown <= 0.0) {
            damagePct = computeCollisionDamagePct(abs(speed))
            impactType = "limite da área"
        }
        speed *= -0.2
    }

    val targetIdx = getCellIndex(candX, candZ, gridSide)
    val targetCell = targetIdx?.let { state.cells.getOrNull(it) }
    if (targetCell?.soilState == SoilState.AGUA_RIO) {
        candX = veh.x
        candZ = veh.z
        if (abs(speed) > 2.4 && cooldown <= 0.0) {
            damagePct = computeCollisionDamagePct(abs(speed) * 0.85)
            impactType = "barranco do rio"
        }
        speed = 0.0
    }

    val colResult = resolveVehicleWorldCollision(
        state = state,
        veh = veh,
        vehDef = def,
        attachedImp = attachedImp,
        candX = candX,
        candZ = candZ,
        headingDeg = nextHeading,
        speedMs = speed,
    )
    candX = colResult.resolvedX
    candZ = colResult.resolvedZ
    if (colResult.collided) {
        if (colResult.impactSpeedMs >= 2.0 && cooldown <= 0.0) {
            val cDmg = computeCollisionDamagePct(colResult.impactSpeedMs)
            if (cDmg > damagePct) {
                damagePct = cDmg
                impactType = colResult.obstacleLabel
            }
        }
        speed = colResult.postSpeedMs
    }

    val postPose = sampleFourWheelTerrainPose(
        state, candX, candZ, nextHeading, spec.frontAxleZ, spec.rearAxleZ, halfTrack,
        spec.suspensionMaxTravelM, spec.cogHeightM
    )
    val terrainY = postPose.groundY
    val wheelAvgY = (postPose.flY + postPose.frY + postPose.rlY + postPose.rrY) * 0.25
    val effectiveTerrainY = terrainY * 0.35 + wheelAvgY * 0.65
    val diagonalTwist = abs(postPose.flY + postPose.rrY - (postPose.frY + postPose.rlY))

    val isDirtRoadPothole = cellUnder?.soilState == SoilState.ESTRADA_TERRA &&
        sin(candX * 0.37) * cos(candZ * 0.43) > 0.86
    val potholeBump = if (isDirtRoadPothole) min(abs(speed), 12.0) * 0.24 else 0.0

    val rutMicroImpulse = if (cellUnder?.soilState == SoilState.ESTRADA_TERRA ||
        cellUnder?.soilState == SoilState.ARADO ||
        cellUnder?.soilState == SoilState.SUBSOLADO ||
        cellUnder?.soilState == SoilState.PEDRAS_MATO
    ) {
        sin(candX * 1.15 + candZ * 0.7) * min(abs(speed), 10.0) * 0.16 + potholeBump
    } else 0.0

    val terrainBumpVel = if (veh.hasTerrainRef) {
        clamp((effectiveTerrainY - veh.prevTerrainY) / dt + diagonalTwist * 0.35 + rutMicroImpulse, -4.5, 4.5)
    } else 0.0

    val trailerLoadRatio = if (impBaseKg + impCargoKg > 0.0) clamp((impBaseKg + impCargoKg) / 14000.0, 0.0, 0.85) else 0.0
    val massSag = -clamp((totalMassKg - def.weightKg) / max(def.weightKg, 500.0), 0.0, 0.65) * 0.048 - trailerLoadRatio * 0.022
    val springForce = -spec.suspensionStiffness * veh.suspensionOffsetY -
        spec.suspensionDamping * veh.suspensionVelY -
        terrainBumpVel * 0.42 +
        massSag * spec.suspensionStiffness
    val nextSusVel = clamp(veh.suspensionVelY + springForce * dt, -1.6, 1.6)
    val nextSusOffset = clamp(veh.suspensionOffsetY + nextSusVel * dt, -spec.suspensionMaxTravelM, spec.suspensionMaxTravelM)

    if (abs(terrainBumpVel) > 3.6 && cooldown <= 0.0 && abs(speed) > 6.0) {
        damagePct = max(damagePct, 1.5)
        impactType = "buraco / impacto severo"
    }

    val longAccel = clamp((speed - prevSpeed) / dt, -8.0, 8.0)
    val pitchTarget = clamp(postPose.pitchDeg - longAccel * 0.48 - trailerLoadRatio * 2.0, -25.0, 25.0)
    val rollTarget = clamp(postPose.rollDeg - speed * yawRateRad * spec.cogHeightM * 1.55, -23.0, 23.0)
    val pitchRollBlend = min(9.5 * dt, 1.0)
    val nextPitch = veh.pitchDeg + (pitchTarget - veh.pitchDeg) * pitchRollBlend
    val nextRoll = veh.rollDeg + (rollTarget - veh.rollDeg) * pitchRollBlend

    // Cab independent micro-oscillation (spring-damped seat/cab suspension + diesel vibration)
    val engineIdleVib = if (isEngineRunning) sin(veh.hoursWorked * 3600.0 * 28.0) * 0.004 else 0.0
    val cabBounceTarget = clamp(-nextSusVel * 0.028 + engineIdleVib, -0.055, 0.055)
    val cabBounce = (veh.cabBounceOffsetY ?: 0.0) + (cabBounceTarget - (veh.cabBounceOffsetY ?: 0.0)) * min(11.0 * dt, 1.0)
    val cabPitchTarget = clamp(-longAccel * 0.35 + (nextPitch - postPose.pitchDeg) * 0.4, -4.5, 4.5)
    val cabRollTarget = clamp(-speed * yawRateRad * 0.65 + (nextRoll - postPose.rollDeg) * 0.4, -4.5, 4.5)
    val nextCabPitch = (veh.cabPitchDeg ?: 0.0) + (cabPitchTarget - (veh.cabPitchDeg ?: 0.0)) * min(10.0 * dt, 1.0)
    val nextCabRoll = (veh.cabRollDeg ?: 0.0) + (cabRollTarget - (veh.cabRollDeg ?: 0.0)) * min(10.0 * dt, 1.0)

    val prevPlaneY = veh.groundPlaneY ?: postPose.groundPlaneY
    val nextGroundPlaneY = prevPlaneY + (postPose.groundPlaneY - prevPlaneY) * min(18.0 * dt, 1.0)
    val prevWheelOffsets = veh.wheelOffsetsY ?: listOf(0.0, 0.0, 0.0, 0.0)
    val wheelBlend = min(14.0 * dt, 1.0)
    val nextWheelOffsetsY = (0 until 4).map { idx ->
        val prevW = prevWheelOffsets.getOrElse(idx) { 0.0 }
        prevW + (postPose.wheelOffsetsY[idx] - prevW) * wheelBlend
    }

    val avgWheelR = max((spec.frontWheelRadiusM + spec.rearWheelRadiusM) * 0.5, 0.28)
    val effectiveWheelSpeedMs = abs(speed) * (1.0 + wheelSlip * 2.2) +
        (if (effectiveAccelCmd > 0.25 && wheelSlip > 0.15) wheelSlip * 3.2 else 0.0)
    val wheelSignedVel = if (speed < -0.1) -effectiveWheelSpeedMs else effectiveWheelSpeedMs
    val deltaWheelDeg = toDeg((wheelSignedVel / avgWheelR) * dt)
    val nextWheelSpin = (veh.wheelSpinDeg + deltaWheelDeg) % 36000.0

    val isHarvestingMove = def.category == VehicleCategory.COLHEITADEIRA && (veh.headerLowered == true) && abs(speed) > 0.22
    val nextRotorSpin = if (isHarvestingMove) (veh.rotorSpinDeg + 210.0 * dt) % 360.0 else veh.rotorSpinDeg
    val augerTarget = if (veh.grainTankFillL > def.tankCapacityL * 0.88) 1.0 else 0.0
    val nextAugerProg = veh.unloadingAugerProgress + (augerTarget - veh.unloadingAugerProgress) * min(2.5 * dt, 1.0)
    val boomTarget = if (veh.isSprayerActive == true) 1.0 else 0.0
    val curBoom = veh.sprayerBoomDeploy ?: 0.0
    val nextBoomDeploy = curBoom + (boomTarget - curBoom) * min(3.4 * dt, 1.0)

    // Final coupled Engine RPM & PTO RPM
    val shiftedGear = currentGear != veh.gear && currentGear > 0 && veh.gear > 0
    val newWheelRpm = (abs(speed) / (2.0 * PI * rearWheelR)) * 60.0
    val newCoupledRpm = newWheelRpm * activeGearRatio * drivetrain.finalDriveRatio
    val targetRpm = if (!isEngineRunning) {
        0.0
    } else {
        val revFloor = if (currentGear == 0) {
            drivetrain.idleRpm + abs(effectiveAccelCmd) * (drivetrain.ratedRpm - drivetrain.idleRpm) * 0.72
        } else {
            val throttleDemandRpm = drivetrain.idleRpm + abs(effectiveAccelCmd) * (drivetrain.ratedRpm - drivetrain.idleRpm) * 0.45 +
                (if (ptoEngaged) 380.0 else 0.0) + wheelSlip * 420.0
            max(newCoupledRpm, throttleDemandRpm)
        }
        val loadDroop = clamp(engineLoadPct / 100.0, 0.0, 1.0) * 140.0
        clamp(
            revFloor - loadDroop - (if (shiftedGear) 240.0 else 0.0),
            drivetrain.idleRpm,
            drivetrain.maxRpm,
        )
    }
    val rpmResponse = clamp(7.5 / max(drivetrain.flywheelInertiaKgM2 * 0.55, 0.5), 3.8, 14.0)
    val rpm = if (!isEngineRunning) {
        max(0.0, veh.rpm - 1250.0 * dt)
    } else {
        veh.rpm + (targetRpm - veh.rpm) * min(rpmResponse * dt, 1.0)
    }
    val nextPtoRpm = if (ptoEngaged && isEngineRunning) {
        clamp((rpm / max(drivetrain.ratedRpm, 1200.0)) * 540.0, 280.0, 600.0)
    } else 0.0

    val awdFuelMult = if (has4wdOption && is4wdEngaged) 1.04 else 1.0
    val fuelBurnLh = computeFuelBurnRateLh(
        FuelConsumptionInput(
            horsepower = tuning.effectiveHp,
            category = def.category,
            rpm = rpm,
            speedMs = speed,
            tillageDragFactor = tillageDragFactor,
            slope = slope,
            implementWorkingWidthM = impDef?.workingWidthM ?: 0.0,
            isImplementLowered = attachedImp?.isLowered == true,
            cargoMassKg = vehCargoKg + impCargoKg,
            wheelSlip = wheelSlip,
            isMud = grip.isMud,
            tuningFuelMultiplier = tuning.fuelMultiplier * awdFuelMult,
            tierFuelPenaltyMultiplier = tierMeta.fuelPenaltyMultiplier,
            engineRunning = isEngineRunning,
        )
    )
    val fuelBurn = (fuelBurnLh / 3600.0) * dt * 6.5
    val newFuel = max(veh.fuelL - fuelBurn, 0.0)
    val actualBurned = max(0.0, veh.fuelL - newFuel)
    val ranOutNow = veh.fuelL > 0.05 && newFuel <= 0.05
    if (ranOutNow && collisionAlert == null) {
        collisionAlert = "Combustível esgotado em ${def.model}! Abasteça no Posto ou bomba da Sede."
    } else if (isEngineRunning && veh.fuelL > def.fuelCapacityL * 0.15 && newFuel <= def.fuelCapacityL * 0.15 && collisionAlert == null) {
        collisionAlert = "Reserva de diesel (< 15%) em ${def.model}! Procure o Posto de Combustível."
    }

    val surfaceType = when {
        cellUnder?.soilState == SoilState.ESTRADA_ASFALTO || cellUnder?.soilState == SoilState.PONTE -> "asfalto"
        grip.isMud -> "lama"
        cellUnder?.soilState == SoilState.ESTRADA_TERRA -> "cascalho"
        else -> "terra"
    }

    val preventiveProtection = if (veh.preventiveBonusHours > 0.0) 0.72 else 1.0
    val workIntensity = if (attachedImp?.isLowered == true) 1.75 else 1.0
    val workConsLoss = abs(speed) * (0.0032 * workIntensity * preventiveProtection) * dt

    var engH = veh.engineHealthPct
    var transH = veh.transmissionHealthPct
    var suspH = veh.suspensionTiresHealthPct
    var hydH = veh.hydraulicsHealthPct
    var bodyH = veh.bodyHealthPct

    var oilFilterH = veh.oilFilterHealthPct ?: 100.0
    var beltsH = veh.beltsHealthPct ?: 100.0
    var tiresWearH = veh.tiresWearPct ?: 100.0
    var hosesH = veh.hydraulicHosesHealthPct ?: 100.0
    var discsH = veh.cutterDiscsHealthPct ?: 100.0
    var nextBreakdownNote = veh.lastBreakdownNote

    if (workConsLoss > 0.0) {
        engH = max(0.0, engH - workConsLoss * 0.9)
        transH = max(0.0, transH - workConsLoss * (0.95 + wheelSlip * 0.6))
        suspH = max(0.0, suspH - workConsLoss * (1.1 + if (grip.isMud) 0.4 else 0.0))
        hydH = max(0.0, hydH - workConsLoss * (if (attachedImp?.isLowered == true) 1.3 else 0.55))
        bodyH = max(0.0, bodyH - workConsLoss * 0.35)

        oilFilterH = max(0.0, oilFilterH - workConsLoss * 1.35)
        beltsH = max(0.0, beltsH - workConsLoss * (1.1 + wheelSlip * 0.4))
        tiresWearH = max(0.0, tiresWearH - workConsLoss * (if (surfaceType == "asfalto") 1.45 else 1.05))
        hosesH = max(0.0, hosesH - workConsLoss * (if (attachedImp?.isLowered == true || veh.isSprayerActive == true) 1.4 else 0.5))
        if (isHarvestingMove) {
            discsH = max(0.0, discsH - workConsLoss * 1.65)
        }
        if ((beltsH < 15.0 || oilFilterH < 15.0) && stallTimer <= 0.0 && abs(speed) > 0.5) {
            stallTimer = 0.55
            nextBreakdownNote = if (beltsH < 15.0) {
                "Correia patinando! Faça revisão preventiva na Oficina."
            } else {
                "Filtro de óleo saturado! Perda de pressão no motor."
            }
        }
    }

    if (damagePct > 0.0) {
        cooldown = 1.2
        bodyH = max(0.0, bodyH - damagePct * 1.45)
        suspH = max(0.0, suspH - damagePct * 1.05)
        engH = max(0.0, engH - damagePct * 0.85)
        transH = max(0.0, transH - damagePct * 0.75)
        hydH = max(0.0, hydH - damagePct * 0.65)
        collisionAlert = "Colisão ($impactType): -${max(1, damagePct.roundToInt())}% conservação!"
    }

    val nextCons = clamp(engH * 0.25 + transH * 0.22 + suspH * 0.2 + hydH * 0.15 + bodyH * 0.18, 0.0, 100.0)

    val rainLevel = WEATHER_META[state.weather]?.rainIntensity ?: 0.0
    val dirtGain = when {
        grip.isMud && abs(speed) > 0.5 -> (1.6 + wheelSlip * 2.2) * dt
        attachedImp?.isLowered == true && abs(speed) > 0.4 -> 0.85 * dt
        grip.isDusty && abs(speed) > 1.5 -> 0.45 * dt
        rainLevel > 0.4 && grip.trackR < 0.2 -> -0.35 * dt
        else -> 0.08 * abs(speed) * dt
    }
    val nextDirt = clamp(veh.dirtPct + dirtGain, 0.0, 100.0)

    val autoWipers = rainLevel >= 0.22
    val wipersRunning = (veh.wipersOn == true) || autoWipers
    val nextWiperAngle = if (wipersRunning) {
        sin((veh.hoursWorked * 3600.0 + dt) * (3.2 + rainLevel * 2.4)) * 0.68
    } else 0.0

    val loadHeat = (rpm / max(drivetrain.maxRpm, 1800.0)) * 18.0 + (engineLoadPct * 0.08) + max(0.0, 100.0 - engH) * 0.18
    val targetTempC = if (!isEngineRunning) 32.0 else clamp(74.0 + loadHeat, 65.0, 114.0)
    val currentTemp = veh.engineTempC ?: 74.0
    val nextTempC = currentTemp + (targetTempC - currentTemp) * min(dt * (if (isEngineRunning) 0.45 else 0.16), 1.0)
    var nextCoolingTicks = veh.coolingTicksRemainingSec ?: 0.0
    if (!isEngineRunning && nextTempC > 62.0) {
        nextCoolingTicks = if (nextCoolingTicks > 0.0) max(0.0, nextCoolingTicks - dt) else 14.0
    } else if (isEngineRunning) {
        nextCoolingTicks = 0.0
    }

    val updatedVeh = veh.copy(
        x = candX,
        z = candZ,
        headingDeg = nextHeading,
        speedMs = speed,
        gear = currentGear,
        rpm = rpm,
        fuelL = newFuel,
        engineRunning = isEngineRunning && newFuel > 0.05,
        isBraking = isBraking,
        fuelSpentTotalL = (veh.fuelSpentTotalL ?: 0.0) + actualBurned,
        lastFuelBurnLh = (fuelBurnLh * 10.0).roundToInt() / 10.0,
        surfaceType = surfaceType,
        engineTempC = nextTempC,
        wipersOn = veh.wipersOn ?: false,
        wiperAngleRad = nextWiperAngle,
        conservationPct = nextCons,
        wearPct = clamp(100.0 - nextCons, 0.0, 100.0),
        engineHealthPct = engH,
        transmissionHealthPct = transH,
        suspensionTiresHealthPct = suspH,
        hydraulicsHealthPct = hydH,
        bodyHealthPct = bodyH,
        oilFilterHealthPct = oilFilterH,
        beltsHealthPct = beltsH,
        tiresWearPct = tiresWearH,
        hydraulicHosesHealthPct = hosesH,
        cutterDiscsHealthPct = discsH,
        lastBreakdownNote = nextBreakdownNote,
        coolingTicksRemainingSec = nextCoolingTicks,
        handbrakeOn = handbrakeEngaged,
        isBogged = nextIsBogged,
        bogSeverity = nextBogSeverity,
        sprayerBoomDeploy = nextBoomDeploy,
        dirtPct = nextDirt,
        steerAngleDeg = nextSteerDeg,
        wheelSpinDeg = nextWheelSpin,
        wheelSlip = wheelSlip,
        suspensionOffsetY = nextSusOffset,
        suspensionVelY = nextSusVel,
        prevTerrainY = effectiveTerrainY,
        hasTerrainRef = true,
        pitchDeg = nextPitch,
        rollDeg = nextRoll,
        groundPlaneY = nextGroundPlaneY,
        wheelOffsetsY = nextWheelOffsetsY,
        wheelContactCount = postPose.wheelContactCount,
        groundSinkageM = grip.sinkageM + (if (nextIsBogged) nextBogSeverity * 0.14 else 0.0),
        collisionCooldownSec = cooldown,
        lastImpactSeverity = if (damagePct > 0.0) damagePct else veh.lastImpactSeverity,
        stallTimerSec = stallTimer,
        rotorSpinDeg = nextRotorSpin,
        unloadingAugerProgress = nextAugerProg,
        preventiveBonusHours = max(0.0, veh.preventiveBonusHours - dt / 3600.0),
        hoursWorked = veh.hoursWorked + dt / 3600.0,
        isPtoEngaged = ptoEngaged,
        ptoRpm = nextPtoRpm,
        engineLoadPct = engineLoadPct,
        cabPitchDeg = nextCabPitch,
        cabRollDeg = nextCabRoll,
        cabBounceOffsetY = cabBounce,
        transmissionMode = drivetrain.transmissionType.label,
        airBrakePressurePct = nextAirPressure,
        articulatedAngleDeg = nextArticulatedDeg,
        cruiseControlActive = cruiseActive,
        cruiseTargetKmh = cruiseTargetKmh,
    )

    val updatedImp = if (attachedImp != null && impDef != null) {
        updateArticulatedImplement(state, def, updatedVeh, attachedImp, impDef, speed, grip, dt)
    } else null

    var distSinceTrack = 999.0
    val recentTracks = state.tireTracks.takeLast(16)
    for (t in recentTracks) {
        val d = hypot(candX - t.x, candZ - t.z)
        if (d < distSinceTrack) distSinceTrack = d
    }
    val shouldEmitTrack = (abs(speed) > 0.65 && distSinceTrack > 1.35) || (wheelSlip > 0.3 && distSinceTrack > 0.65)
    val effectiveTrackWidth = if (tuning.hasDualRears) spec.trackWidthM * 1.15 else spec.trackWidthM
    val effectiveTireWidth = if (tuning.hasDualRears) 0.54 else 0.34

    val newTireTrack = if (shouldEmitTrack &&
        cellUnder?.soilState != SoilState.ESTRADA_ASFALTO &&
        cellUnder?.soilState != SoilState.PONTE
    ) {
        TireTrack(
            x = candX,
            z = candZ,
            headingDeg = nextHeading,
            age = 0.0,
            trackWidthM = effectiveTrackWidth,
            tireWidthM = effectiveTireWidth,
            isMud = grip.isMud,
            isSlip = wheelSlip > 0.25,
            soilR = grip.trackR,
            soilG = grip.trackG,
            soilB = grip.trackB,
        )
    } else null

    return VehiclePhysicsStepResult(
        vehicle = updatedVeh,
        implement = updatedImp,
        newTireTrack = newTireTrack,
        collisionAlert = collisionAlert,
    )
}
