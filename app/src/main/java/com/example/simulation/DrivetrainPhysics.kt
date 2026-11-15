package com.example.simulation

import com.example.domain.DrivetrainSpec
import com.example.domain.HitchType
import com.example.domain.ImplementCategory
import com.example.domain.ImplementDef
import com.example.domain.OwnedImplement
import com.example.domain.OwnedVehicle
import com.example.domain.SoilState
import com.example.domain.TerrainCell
import com.example.domain.TractorClass
import com.example.domain.TransmissionType
import com.example.domain.VehicleBodyStyle
import com.example.domain.VehicleCategory
import com.example.domain.VehicleDef
import com.example.domain.VehiclePhysicsSpec
import com.example.domain.ResolvedVehicleTuning
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ImplementPhysicsLoad(
    val draftForceN: Double,
    val ptoPowerKw: Double,
    val maxRecommendedWorkSpeedMs: Double,
    val isGroundEngaging: Boolean,
    val usesPto: Boolean,
    val powerSufficiencyRatio: Double,
)

data class MachineImplementCompatibility(
    val hitchCompatible: Boolean,
    val hpSufficient: Boolean,
    val powerRatio: Double,
    val warningMessage: String?,
    val blockMessage: String?,
)

fun isPtoDrivenImplement(category: ImplementCategory): Boolean = when (category) {
    ImplementCategory.PLANTADEIRA,
    ImplementCategory.SEMEADORA,
    ImplementCategory.PULVERIZADOR,
    ImplementCategory.DISTRIBUIDOR,
    ImplementCategory.SEGADORA,
    ImplementCategory.ENLEIRADOR,
    ImplementCategory.ENFARDADORA,
    ImplementCategory.ENSILADEIRA,
    ImplementCategory.ENSACADORA,
    ImplementCategory.ROCADEIRA,
    ImplementCategory.PLATAFORMA_MILHO -> true
    else -> false
}

fun isGroundEngagingImplement(category: ImplementCategory): Boolean = when (category) {
    ImplementCategory.ARADO,
    ImplementCategory.GRADE,
    ImplementCategory.SUBSOLADOR,
    ImplementCategory.ESCARIFICADOR,
    ImplementCategory.CULTIVADOR,
    ImplementCategory.PLANTADEIRA,
    ImplementCategory.SEMEADORA,
    ImplementCategory.ROCADEIRA -> true
    else -> false
}

fun checkMachineImplementCompatibility(
    vehDef: VehicleDef,
    impDef: ImplementDef,
    effectiveHp: Double = vehDef.horsepower,
): MachineImplementCompatibility {
    val isHeader = impDef.hitchType == HitchType.HEADER || impDef.category == ImplementCategory.PLATAFORMA_MILHO
    if (isHeader && vehDef.category != VehicleCategory.COLHEITADEIRA) {
        return MachineImplementCompatibility(
            hitchCompatible = false,
            hpSufficient = false,
            powerRatio = 0.0,
            warningMessage = null,
            blockMessage = "${impDef.model} é uma plataforma frontal exclusiva para colheitadeiras.",
        )
    }

    val hitchOk = if (isHeader) {
        vehDef.category == VehicleCategory.COLHEITADEIRA && vehDef.supportedHitches.contains(HitchType.HEADER)
    } else {
        vehDef.supportedHitches.contains(impDef.hitchType) ||
            (vehDef.category == VehicleCategory.TRATOR && impDef.hitchType != HitchType.FIFTH_WHEEL && impDef.hitchType != HitchType.HEADER) ||
            (vehDef.category == VehicleCategory.CAMINHAO && (impDef.hitchType == HitchType.DRAWBAR || impDef.hitchType == HitchType.FIFTH_WHEEL)) ||
            (vehDef.category == VehicleCategory.CAMINHONETE && impDef.hitchType == HitchType.DRAWBAR)
    }

    if (!hitchOk) {
        return MachineImplementCompatibility(
            hitchCompatible = false,
            hpSufficient = false,
            powerRatio = 0.0,
            warningMessage = null,
            blockMessage = "Engate incompatível entre ${vehDef.model} e ${impDef.model}.",
        )
    }

    val minReq = max(impDef.requiredHp, 10.0)
    val ratio = effectiveHp / minReq
    if (ratio < 0.62) {
        return MachineImplementCompatibility(
            hitchCompatible = true,
            hpSufficient = false,
            powerRatio = ratio,
            warningMessage = "Potência muito baixa (${effectiveHp.roundToInt()} cv p/ ${minReq.roundToInt()} cv req.)",
            blockMessage = "${vehDef.model} (${effectiveHp.roundToInt()} cv) é fraco para ${impDef.model} (mín. ${minReq.roundToInt()} cv).",
        )
    }

    val warn = if (ratio < 0.95) {
        "Potência justa (${effectiveHp.roundToInt()} cv / ${minReq.roundToInt()} cv req.): perda de rotação sob carga."
    } else null

    return MachineImplementCompatibility(
        hitchCompatible = true,
        hpSufficient = true,
        powerRatio = ratio,
        warningMessage = warn,
        blockMessage = null,
    )
}

fun resolveVehicleDrivetrainSpec(
    def: VehicleDef,
    spec: VehiclePhysicsSpec,
): DrivetrainSpec {
    val hp = max(def.horsepower, 12.0)
    val maxKmh = max(def.maxSpeedKmh, 12.0)
    val rearR = max(spec.rearWheelRadiusM, 0.30)

    val transType: TransmissionType
    val ratios: DoubleArray
    val reverseRatio: Double
    val eff: Double
    val idleRpm: Double
    val peakTorqueRpm: Double
    val ratedRpm: Double
    val maxRpm: Double
    val inertia: Double
    val airBrakes: Boolean
    val upshiftFrac: Double
    val downshiftFrac: Double

    when (def.category) {
        VehicleCategory.UTILITARIO_MOTO -> {
            transType = TransmissionType.MOTO_5
            ratios = doubleArrayOf(3.10, 2.15, 1.58, 1.24, 1.00)
            reverseRatio = 3.20
            eff = 0.90
            idleRpm = 1250.0
            peakTorqueRpm = 5200.0
            ratedRpm = 7400.0
            maxRpm = 8400.0
            inertia = 0.35
            airBrakes = false
            upshiftFrac = 0.88
            downshiftFrac = 0.42
        }
        VehicleCategory.CAMINHONETE -> {
            transType = TransmissionType.PICKUP_6
            ratios = doubleArrayOf(4.25, 2.55, 1.72, 1.24, 1.00, 0.78)
            reverseRatio = 3.85
            eff = 0.88
            idleRpm = 750.0
            peakTorqueRpm = 2200.0
            ratedRpm = 3800.0
            maxRpm = 4400.0
            inertia = 0.85
            airBrakes = false
            upshiftFrac = 0.85
            downshiftFrac = 0.44
        }
        VehicleCategory.CAMINHAO -> {
            val isModern = def.modelYear >= 2014
            transType = if (isModern) TransmissionType.TRUCK_AUTO else TransmissionType.TRUCK_12_MANUAL
            ratios = doubleArrayOf(11.3, 8.8, 6.9, 5.4, 4.2, 3.3, 2.55, 1.98, 1.54, 1.22, 1.00, 0.82)
            reverseRatio = 10.4
            eff = 0.87
            idleRpm = 600.0
            peakTorqueRpm = 1250.0
            ratedRpm = 1900.0
            maxRpm = 2300.0
            inertia = 2.6
            airBrakes = true
            upshiftFrac = 0.87
            downshiftFrac = 0.56
        }
        VehicleCategory.COLHEITADEIRA -> {
            transType = TransmissionType.CVT_HYDRO
            ratios = doubleArrayOf(4.20, 2.80, 1.85, 1.25, 1.00)
            reverseRatio = 3.60
            eff = 0.80
            idleRpm = 900.0
            peakTorqueRpm = 1600.0
            ratedRpm = 2150.0
            maxRpm = 2380.0
            inertia = 2.4
            airBrakes = false
            upshiftFrac = 0.90
            downshiftFrac = 0.62
        }
        VehicleCategory.PULVERIZADOR_AUTOPROPELIDO -> {
            transType = TransmissionType.CVT_HYDRO
            ratios = doubleArrayOf(4.00, 2.65, 1.75, 1.22, 1.00)
            reverseRatio = 3.40
            eff = 0.82
            idleRpm = 850.0
            peakTorqueRpm = 1550.0
            ratedRpm = 2200.0
            maxRpm = 2450.0
            inertia = 1.65
            airBrakes = false
            upshiftFrac = 0.89
            downshiftFrac = 0.60
        }
        VehicleCategory.TRATOR_ESTEIRA,
        VehicleCategory.ESCAVADEIRA,
        VehicleCategory.MOTONIVELADORA -> {
            transType = TransmissionType.POWERSHIFT_16
            ratios = doubleArrayOf(6.8, 5.6, 4.7, 3.9, 3.3, 2.75, 2.3, 1.95, 1.65, 1.42, 1.22, 1.08, 1.00)
            reverseRatio = 5.2
            eff = 0.81
            idleRpm = 780.0
            peakTorqueRpm = 1450.0
            ratedRpm = 2050.0
            maxRpm = 2300.0
            inertia = 2.3
            airBrakes = false
            upshiftFrac = 0.88
            downshiftFrac = 0.60
        }
        else -> {
            val tClass = def.tractorClass
            val isModernCvt = (def.modelYear >= 2018 && hp >= 240.0) ||
                def.id.contains("vario", ignoreCase = true) ||
                def.id.contains("cvt", ignoreCase = true) ||
                def.bodyStyle == VehicleBodyStyle.QUAD_TRACK
            val isPowershift = hp >= 170.0 || def.bodyStyle == VehicleBodyStyle.ARTICULATED_4WD || def.bodyStyle == VehicleBodyStyle.HIGH_POWER_CAB
            val isClassic = def.modelYear < 1998 || tClass == TractorClass.HISTORICO || tClass == TractorClass.COMPACTO || hp < 75.0

            when {
                isModernCvt -> {
                    transType = TransmissionType.CVT_HYDRO
                    ratios = doubleArrayOf(5.80, 4.10, 2.95, 2.10, 1.50, 1.15, 1.00)
                    reverseRatio = 4.6
                    eff = 0.84
                    idleRpm = 780.0
                    peakTorqueRpm = 1480.0
                    ratedRpm = 2100.0
                    maxRpm = 2350.0
                    inertia = 2.1
                    airBrakes = hp >= 250.0
                    upshiftFrac = 0.90
                    downshiftFrac = 0.62
                }
                isPowershift -> {
                    transType = TransmissionType.POWERSHIFT_16
                    ratios = doubleArrayOf(6.40, 5.45, 4.65, 3.95, 3.38, 2.88, 2.46, 2.10, 1.80, 1.54, 1.34, 1.18, 1.08, 1.00)
                    reverseRatio = 5.1
                    eff = 0.85
                    idleRpm = 780.0
                    peakTorqueRpm = 1450.0
                    ratedRpm = 2120.0
                    maxRpm = 2380.0
                    inertia = 2.0
                    airBrakes = hp >= 230.0
                    upshiftFrac = 0.89
                    downshiftFrac = 0.59
                }
                isClassic -> {
                    transType = TransmissionType.MANUAL_6
                    ratios = doubleArrayOf(5.40, 3.75, 2.65, 1.90, 1.36, 1.00)
                    reverseRatio = 4.4
                    eff = 0.83
                    idleRpm = 720.0
                    peakTorqueRpm = 1350.0
                    ratedRpm = 2180.0
                    maxRpm = 2450.0
                    inertia = 1.35
                    airBrakes = false
                    upshiftFrac = 0.88
                    downshiftFrac = 0.56
                }
                else -> {
                    transType = TransmissionType.SYNCHRO_12
                    ratios = doubleArrayOf(5.90, 4.75, 3.85, 3.12, 2.54, 2.08, 1.72, 1.44, 1.24, 1.10, 1.00)
                    reverseRatio = 4.8
                    eff = 0.84
                    idleRpm = 760.0
                    peakTorqueRpm = 1420.0
                    ratedRpm = 2160.0
                    maxRpm = 2420.0
                    inertia = 1.6
                    airBrakes = false
                    upshiftFrac = 0.88
                    downshiftFrac = 0.58
                }
            }
        }
    }

    val topGearRatio = ratios.last()
    val maxSpeedMsNominal = maxKmh / 3.6
    val ratedWheelAngularRadPerSec = maxSpeedMsNominal / rearR
    val ratedEngineRadPerSec = (ratedRpm * 2.0 * PI) / 60.0
    val finalDrive = max(1.2, (ratedEngineRadPerSec / max(ratedWheelAngularRadPerSec, 0.5)) / topGearRatio)

    return DrivetrainSpec(
        transmissionType = transType,
        gearRatios = ratios,
        reverseRatio = reverseRatio,
        finalDriveRatio = finalDrive,
        drivetrainEfficiency = eff,
        idleRpm = idleRpm,
        peakTorqueRpm = peakTorqueRpm,
        ratedRpm = ratedRpm,
        maxRpm = maxRpm,
        flywheelInertiaKgM2 = inertia,
        hasAirBrakes = airBrakes,
        upshiftRpmFraction = upshiftFrac,
        downshiftRpmFraction = downshiftFrac,
    )
}

fun computeEngineTorqueNm(
    def: VehicleDef,
    tuning: ResolvedVehicleTuning,
    drivetrain: DrivetrainSpec,
    rpm: Double,
    throttleCmd: Double,
): Double {
    val peakTorqueBase = max(def.maxTorqueNm, def.horsepower * 3.8)
    val hpScale = tuning.effectiveHp / max(def.horsepower, 1.0)
    val peakTorqueNm = peakTorqueBase * hpScale

    val curveShape = when {
        rpm <= drivetrain.idleRpm * 0.7 -> 0.38
        rpm < drivetrain.idleRpm -> {
            val t = (rpm - drivetrain.idleRpm * 0.7) / max(drivetrain.idleRpm * 0.3, 1.0)
            0.38 + 0.14 * t
        }
        rpm <= drivetrain.peakTorqueRpm -> {
            val t = ((rpm - drivetrain.idleRpm) / max(drivetrain.peakTorqueRpm - drivetrain.idleRpm, 1.0)).coerceIn(0.0, 1.0)
            0.52 + 0.48 * (1.0 - (1.0 - t) * (1.0 - t))
        }
        rpm <= drivetrain.ratedRpm -> {
            val t = ((rpm - drivetrain.peakTorqueRpm) / max(drivetrain.ratedRpm - drivetrain.peakTorqueRpm, 1.0)).coerceIn(0.0, 1.0)
            1.0 - 0.16 * t
        }
        rpm <= drivetrain.maxRpm -> {
            val t = ((rpm - drivetrain.ratedRpm) / max(drivetrain.maxRpm - drivetrain.ratedRpm, 1.0)).coerceIn(0.0, 1.0)
            0.84 * (1.0 - t * t)
        }
        else -> 0.0
    }

    val idleGovernorTorque = if (rpm < drivetrain.idleRpm && throttleCmd < 0.18) {
        val deficit = ((drivetrain.idleRpm - rpm) / max(drivetrain.idleRpm, 1.0)).coerceIn(0.0, 1.0)
        peakTorqueNm * 0.32 * deficit
    } else 0.0

    val activeThrottle = throttleCmd.coerceIn(0.0, 1.0)
    return max(idleGovernorTorque, peakTorqueNm * curveShape * (0.08 + 0.92 * activeThrottle))
}

fun computeImplementPhysicsLoad(
    vehDef: VehicleDef,
    effectiveHp: Double,
    imp: OwnedImplement?,
    impDef: ImplementDef?,
    cellUnder: TerrainCell?,
    speedMs: Double,
): ImplementPhysicsLoad {
    if (imp == null || impDef == null) {
        return ImplementPhysicsLoad(0.0, 0.0, 99.0, false, false, 1.0)
    }

    val usesPto = isPtoDrivenImplement(impDef.category)
    val groundEngaging = isGroundEngagingImplement(impDef.category)
    val loweredActive = imp.isLowered || imp.liftProgress < 0.45
    val impCargoKg = imp.fillL * 0.85
    val totalImpKg = impDef.weightKg + impCargoKg
    val compaction = cellUnder?.compaction ?: 0.15
    val moisture = cellUnder?.moisture ?: 0.50
    val powerSufficiency = (effectiveHp / max(impDef.requiredHp, 10.0)).coerceIn(0.35, 2.5)

    if (!loweredActive) {
        // Transport mode: rolling resistance of implement wheels/tongue weight
        val transportRollN = totalImpKg * 9.81 * (0.022 + moisture * 0.015)
        return ImplementPhysicsLoad(
            draftForceN = transportRollN,
            ptoPowerKw = 0.0,
            maxRecommendedWorkSpeedMs = 99.0,
            isGroundEngaging = false,
            usesPto = usesPto,
            powerSufficiencyRatio = powerSufficiency,
        )
    }

    val w = max(impDef.workingWidthM, 1.2)
    val compMult = 0.85 + compaction * 0.65 + (if (moisture > 0.72) 0.20 else 0.0)
    val engageBlend = (1.0 - imp.liftProgress).coerceIn(0.25, 1.0)

    val baseDraftN: Double
    val ptoKw: Double
    val workSpeedCapMs: Double

    when (impDef.category) {
        ImplementCategory.SUBSOLADOR -> {
            baseDraftN = w * 3200.0 * compMult * engageBlend
            ptoKw = 0.0
            workSpeedCapMs = 3.1 // ~11 km/h
        }
        ImplementCategory.ARADO -> {
            baseDraftN = w * 2650.0 * compMult * engageBlend
            ptoKw = 0.0
            workSpeedCapMs = 3.5 // ~12.6 km/h
        }
        ImplementCategory.ESCARIFICADOR -> {
            baseDraftN = w * 2150.0 * compMult * engageBlend
            ptoKw = 0.0
            workSpeedCapMs = 3.9 // ~14 km/h
        }
        ImplementCategory.GRADE -> {
            baseDraftN = w * 1650.0 * compMult * engageBlend
            ptoKw = 0.0
            workSpeedCapMs = 4.4 // ~16 km/h
        }
        ImplementCategory.CULTIVADOR -> {
            baseDraftN = w * 1350.0 * compMult * engageBlend
            ptoKw = 0.0
            workSpeedCapMs = 4.5 // ~16.2 km/h inter-row cultivation
        }
        ImplementCategory.PLATAFORMA_MILHO -> {
            baseDraftN = w * 260.0 * engageBlend
            ptoKw = if (imp.isActive) w * 6.5 else 0.0
            workSpeedCapMs = 3.8 // ~13.7 km/h corn harvesting
        }
        ImplementCategory.PLANTADEIRA, ImplementCategory.SEMEADORA -> {
            baseDraftN = w * 1250.0 * compMult * engageBlend + impCargoKg * 0.45
            ptoKw = if (imp.isActive) w * 3.8 else 0.0
            workSpeedCapMs = 3.6 // ~13 km/h precision planting
        }
        ImplementCategory.ROCADEIRA -> {
            baseDraftN = w * 650.0 * engageBlend
            ptoKw = if (imp.isActive) w * 8.5 else 0.0
            workSpeedCapMs = 3.8
        }
        ImplementCategory.ENSILADEIRA -> {
            baseDraftN = w * 980.0 * engageBlend + impCargoKg * 0.35
            ptoKw = if (imp.isActive) max(26.0, impDef.requiredHp * 0.48) else 0.0
            workSpeedCapMs = 3.2
        }
        ImplementCategory.ENFARDADORA -> {
            baseDraftN = w * 820.0 * engageBlend + impCargoKg * 0.30
            ptoKw = if (imp.isActive) max(22.0, impDef.requiredHp * 0.42) else 0.0
            workSpeedCapMs = 4.0
        }
        ImplementCategory.SEGADORA -> {
            baseDraftN = w * 480.0 * engageBlend
            ptoKw = if (imp.isActive) w * 6.0 else 0.0
            workSpeedCapMs = 4.8
        }
        ImplementCategory.ENLEIRADOR -> {
            baseDraftN = w * 360.0 * engageBlend
            ptoKw = if (imp.isActive) w * 2.6 else 0.0
            workSpeedCapMs = 5.2
        }
        ImplementCategory.PULVERIZADOR, ImplementCategory.DISTRIBUIDOR -> {
            baseDraftN = totalImpKg * 9.81 * 0.035
            ptoKw = if (imp.isActive) 8.5 else 0.0
            workSpeedCapMs = 6.8 // ~24.5 km/h
        }
        ImplementCategory.CARRETA -> {
            baseDraftN = totalImpKg * 9.81 * (0.035 + moisture * 0.045)
            ptoKw = 0.0
            workSpeedCapMs = 99.0
        }
        else -> {
            baseDraftN = w * 500.0
            ptoKw = if (imp.isActive) 6.0 else 0.0
            workSpeedCapMs = 4.8
        }
    }

    // Soil draft scales moderately with speed squared for deep tillage
    val speedDragFactor = if (groundEngaging) 1.0 + (abs(speedMs) / 5.0) * 0.28 else 1.0
    return ImplementPhysicsLoad(
        draftForceN = baseDraftN * speedDragFactor,
        ptoPowerKw = ptoKw,
        maxRecommendedWorkSpeedMs = workSpeedCapMs,
        isGroundEngaging = groundEngaging,
        usesPto = usesPto,
        powerSufficiencyRatio = powerSufficiency,
    )
}
