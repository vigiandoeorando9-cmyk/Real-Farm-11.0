package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.domain.GraphicsConfig
import com.example.domain.ImplementCategory
import com.example.domain.VehicleBodyStyle
import com.example.domain.WorldState
import com.example.domain.getImplementDef
import com.example.domain.getVehicleDef
import com.example.simulation.getTerrainHeightAt
import com.example.simulation.isGroundEngagingImplement
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal enum class ParticleKind3D {
    EXHAUST_SMOKE,
    SOIL_DUST,
    MUD_SPLASH,
    SPRAY_MIST,
    GRANULE_SCATTER,
    GRAIN_STREAM,
}

internal class Particle3D {
    var active: Boolean = false
    var kind: ParticleKind3D = ParticleKind3D.EXHAUST_SMOKE
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var vz: Float = 0f
    var ageSec: Float = 0f
    var maxLifeSec: Float = 1f
    var sizeM: Float = 0.25f
    var color: Color = Color.Gray
}

/**
 * Zero-GC fixed-capacity ring buffer for 3D machinery & terrain particles.
 */
internal class ParticlePool3D(private val capacity: Int = 180) {
    private val pool = Array(capacity) { Particle3D() }
    private var cursor = 0
    private var rngSeed = 1337
    private var exhaustCooldown = 0f
    private var dustCooldown = 0f
    private var sprayCooldown = 0f

    private fun nextRand(): Float {
        rngSeed = (rngSeed * 1664525 + 1013904223) and 0x7fffffff
        return (rngSeed % 10000) / 10000f
    }

    private fun spawn(
        kind: ParticleKind3D,
        x: Float, y: Float, z: Float,
        vx: Float, vy: Float, vz: Float,
        lifeSec: Float,
        sizeM: Float,
        color: Color,
    ) {
        val p = pool[cursor]
        cursor = (cursor + 1) % capacity
        p.active = true
        p.kind = kind
        p.x = x
        p.y = y
        p.z = z
        p.vx = vx
        p.vy = vy
        p.vz = vz
        p.ageSec = 0f
        p.maxLifeSec = lifeSec
        p.sizeM = sizeM
        p.color = color
    }

    fun stepAndEmit(worldState: WorldState, dtSec: Float, gfx: GraphicsConfig) {
        val q = (gfx.particleQuality ?: 0.9).toFloat().coerceIn(0f, 1.4f)
        for (i in 0 until capacity) {
            val p = pool[i]
            if (!p.active) continue
            p.ageSec += dtSec
            if (p.ageSec >= p.maxLifeSec) {
                p.active = false
                continue
            }
            p.x += p.vx * dtSec
            p.y += p.vy * dtSec
            p.z += p.vz * dtSec
            when (p.kind) {
                ParticleKind3D.EXHAUST_SMOKE, ParticleKind3D.SOIL_DUST -> {
                    p.vy *= 0.96f
                    p.sizeM += dtSec * 0.42f
                }
                ParticleKind3D.MUD_SPLASH, ParticleKind3D.GRAIN_STREAM, ParticleKind3D.GRANULE_SCATTER -> {
                    p.vy -= 5.8f * dtSec
                }
                ParticleKind3D.SPRAY_MIST -> {
                    p.vy -= 0.9f * dtSec
                    p.sizeM += dtSec * 0.25f
                }
            }
        }

        if (q < 0.08f) return
        val activeVeh = worldState.activeVehicleUid?.let { uid -> worldState.vehicles.find { it.uid == uid } } ?: return
        val def = getVehicleDef(activeVeh.defId)
        val gy = getTerrainHeightAt(worldState, activeVeh.x, activeVeh.z).toFloat()
        val rad = activeVeh.headingDeg.toFloat() * (PI / 180.0).toFloat()
        val fx = sin(rad)
        val fz = cos(rad)
        val rx = cos(rad)
        val rz = -sin(rad)
        val lenM = def.dimensionsM.width.toFloat().coerceIn(2.2f, 9.2f)
        val widthM = def.dimensionsM.height.toFloat().coerceIn(1.3f, 4.0f)
        val heightM = def.dimensionsM.length.toFloat().coerceIn(1.5f, 4.2f)
        val vx = activeVeh.x.toFloat()
        val vz = activeVeh.z.toFloat()

        // 1. Diesel exhaust smoke proportional to RPM & engine load
        exhaustCooldown -= dtSec
        if (activeVeh.engineRunning == true && exhaustCooldown <= 0f) {
            val loadNorm = ((activeVeh.engineLoadPct ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
            val rpmNorm = ((activeVeh.rpm - 700.0) / 1800.0).toFloat().coerceIn(0f, 1.1f)
            exhaustCooldown = ((0.18f - loadNorm * 0.09f) / max(0.35f, q)).coerceIn(0.05f, 0.30f)
            val exX = vx + fx * (lenM * 0.08f) + rx * (widthM * 0.30f)
            val exZ = vz + fz * (lenM * 0.08f) + rz * (widthM * 0.30f)
            val exY = gy + heightM * 0.96f
            val smokeDark = (0.68f - loadNorm * 0.48f).coerceIn(0.14f, 0.72f)
            val alpha = (0.16f + loadNorm * 0.30f + rpmNorm * 0.10f).coerceIn(0.14f, 0.52f)
            spawn(
                kind = ParticleKind3D.EXHAUST_SMOKE,
                x = exX,
                y = exY,
                z = exZ,
                vx = (nextRand() - 0.5f) * 0.5f - fx * 0.35f,
                vy = 1.4f + rpmNorm * 1.6f,
                vz = (nextRand() - 0.5f) * 0.5f - fz * 0.35f,
                lifeSec = 0.85f + loadNorm * 0.45f,
                sizeM = 0.18f + loadNorm * 0.16f,
                color = Color(smokeDark, smokeDark, smokeDark, alpha),
            )
        }

        // 2. Tire dust / Mud splash / Tillage & Harvest dust
        dustCooldown -= dtSec
        val absSpeed = abs(activeVeh.speedMs).toFloat()
        val imp = activeVeh.attachedImplementUid?.let { iUid -> worldState.implements.find { it.uid == iUid } }
        val impDef = imp?.let { getImplementDef(it.defId) }
        val isTilling = imp != null && impDef != null && imp.isLowered && imp.liftProgress < 0.45 &&
            isGroundEngagingImplement(impDef.category) && absSpeed > 0.4f
        val isHarvestingDust = ((def.bodyStyle == VehicleBodyStyle.HARVESTER && activeVeh.headerLowered == true) ||
            (impDef?.category == ImplementCategory.PLATAFORMA_MILHO && imp?.isLowered == true)) && absSpeed > 0.4f

        if (dustCooldown <= 0f && (absSpeed > 1.4f || isTilling || isHarvestingDust || activeVeh.wheelSlip > 0.25)) {
            dustCooldown = (0.08f / max(0.35f, q)).coerceIn(0.035f, 0.20f)
            val isMuddy = activeVeh.surfaceType == "lama"
            val side = if (nextRand() > 0.5f) 1f else -1f
            if (isMuddy && activeVeh.surfaceType != "asfalto") {
                spawn(
                    kind = ParticleKind3D.MUD_SPLASH,
                    x = vx - fx * (lenM * 0.30f) + rx * (widthM * 0.42f * side),
                    y = gy + 0.35f,
                    z = vz - fz * (lenM * 0.30f) + rz * (widthM * 0.42f * side),
                    vx = (nextRand() - 0.5f) * 1.8f - fx * absSpeed * 0.25f,
                    vy = 1.8f + nextRand() * 1.4f,
                    vz = (nextRand() - 0.5f) * 1.8f - fz * absSpeed * 0.25f,
                    lifeSec = 0.48f,
                    sizeM = 0.14f,
                    color = Color(0xFF3E2723).copy(alpha = 0.72f),
                )
            } else if (activeVeh.surfaceType != "asfalto") {
                val tx = when {
                    isTilling && imp != null -> imp.x.toFloat() + rx * ((nextRand() - 0.5f) * impDef!!.workingWidthM.toFloat())
                    isHarvestingDust -> vx - fx * (lenM * 0.45f) + rx * ((nextRand() - 0.5f) * 1.8f)
                    else -> vx - fx * (lenM * 0.34f) + rx * (widthM * 0.42f * side)
                }
                val tz = when {
                    isTilling && imp != null -> imp.z.toFloat() + rz * ((nextRand() - 0.5f) * impDef!!.workingWidthM.toFloat())
                    isHarvestingDust -> vz - fz * (lenM * 0.45f) + rz * ((nextRand() - 0.5f) * 1.8f)
                    else -> vz - fz * (lenM * 0.34f) + rz * (widthM * 0.42f * side)
                }
                val dustColor = if (isHarvestingDust) {
                    Color(0xFFD9B96A).copy(alpha = 0.38f)
                } else {
                    Color(0xFFB38D62).copy(alpha = 0.36f)
                }
                spawn(
                    kind = ParticleKind3D.SOIL_DUST,
                    x = tx,
                    y = gy + if (isHarvestingDust) 1.05f else 0.25f,
                    z = tz,
                    vx = (nextRand() - 0.5f) * 0.9f,
                    vy = 0.8f + (if (isTilling || isHarvestingDust) 0.75f else 0.3f),
                    vz = (nextRand() - 0.5f) * 0.9f,
                    lifeSec = 0.78f,
                    sizeM = if (isTilling || isHarvestingDust) 0.46f else 0.32f,
                    color = dustColor,
                )
            }
        }

        // 3. Sprayer nozzle mist, Fertilizer scatter, or Grain unloading stream
        sprayCooldown -= dtSec
        if (sprayCooldown <= 0f) {
            sprayCooldown = (0.06f / max(0.35f, q)).coerceIn(0.04f, 0.16f)
            if (def.category == com.example.domain.VehicleCategory.PULVERIZADOR_AUTOPROPELIDO &&
                activeVeh.isSprayerActive == true && (activeVeh.sprayerBoomDeploy ?: 1.0) >= 0.6
            ) {
                val offsetLat = (nextRand() - 0.5f) * 18f
                spawn(
                    ParticleKind3D.SPRAY_MIST,
                    vx - fx * (lenM * 0.45f) + rx * offsetLat,
                    gy + 1.25f,
                    vz - fz * (lenM * 0.45f) + rz * offsetLat,
                    0f, -0.9f, 0f,
                    lifeSec = 0.45f,
                    sizeM = 0.26f,
                    color = Color(0xFFB3E5FC).copy(alpha = 0.55f),
                )
            } else if (imp != null && impDef != null && imp.isLowered && imp.isActive && imp.fillL > 1.0) {
                if (impDef.category == ImplementCategory.PULVERIZADOR && imp.boomDeployProgress >= 0.6) {
                    val lat = (nextRand() - 0.5f) * impDef.workingWidthM.toFloat()
                    spawn(
                        ParticleKind3D.SPRAY_MIST,
                        imp.x.toFloat() + rx * lat,
                        gy + 0.85f,
                        imp.z.toFloat() + rz * lat,
                        0f, -0.8f, 0f,
                        lifeSec = 0.42f,
                        sizeM = 0.24f,
                        color = Color(0xFFB3E5FC).copy(alpha = 0.52f),
                    )
                } else if (impDef.category == ImplementCategory.DISTRIBUIDOR) {
                    val lat = (nextRand() - 0.5f) * 4.5f
                    val col = if (imp.fillProductId == "calcario") Color(0xFFEFEFEF) else Color(0xFFD7CCC8)
                    spawn(
                        ParticleKind3D.GRANULE_SCATTER,
                        imp.x.toFloat() - fx * 0.6f,
                        gy + 0.85f,
                        imp.z.toFloat() - fz * 0.6f,
                        rx * lat * 1.4f - fx * 2.0f,
                        1.1f,
                        rz * lat * 1.4f - fz * 2.0f,
                        lifeSec = 0.48f,
                        sizeM = 0.10f,
                        color = col.copy(alpha = 0.72f),
                    )
                }
            }

            if (activeVeh.unloadingAugerProgress > 0.35 && def.bodyStyle == VehicleBodyStyle.HARVESTER) {
                val pipeEndX = vx - rx * (widthM * 0.5f + 2.8f) + fx * (lenM * 0.1f)
                val pipeEndZ = vz - rz * (widthM * 0.5f + 2.8f) + fz * (lenM * 0.1f)
                spawn(
                    ParticleKind3D.GRAIN_STREAM,
                    pipeEndX + (nextRand() - 0.5f) * 0.15f,
                    gy + heightM * 0.95f,
                    pipeEndZ + (nextRand() - 0.5f) * 0.15f,
                    (nextRand() - 0.5f) * 0.3f,
                    -2.4f,
                    (nextRand() - 0.5f) * 0.3f,
                    lifeSec = 0.55f,
                    sizeM = 0.16f,
                    color = Color(0xFFFFD54F).copy(alpha = 0.90f),
                )
            }
        }
    }

    fun render(
        drawScope: DrawScope,
        pose: CameraPose3D,
        focalPx: Float,
        w: Float,
        h: Float,
    ) {
        for (i in 0 until capacity) {
            val p = pool[i]
            if (!p.active) continue
            val proj = projectRaw(p.x, p.y, p.z, pose, focalPx, w, h)
            if (!proj.visible || proj.depth > 95f) continue
            val lifeT = (1f - (p.ageSec / p.maxLifeSec)).coerceIn(0f, 1f)
            val pxSize = ((p.sizeM * focalPx) / max(proj.depth, 1f)).coerceIn(1.5f, 36f)
            drawScope.drawCircle(
                color = p.color.copy(alpha = p.color.alpha * lifeT),
                radius = pxSize,
                center = Offset(proj.screenX, proj.screenY),
            )
        }
    }
}
