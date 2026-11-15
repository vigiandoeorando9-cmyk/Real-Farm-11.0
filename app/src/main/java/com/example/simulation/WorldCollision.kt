package com.example.simulation

import com.example.domain.BUILDING_META
import com.example.domain.BuildingType
import com.example.domain.ESTABLISHMENTS
import com.example.domain.OwnedImplement
import com.example.domain.OwnedVehicle
import com.example.domain.PlacedBuilding
import com.example.domain.VehicleBodyStyle
import com.example.domain.VehicleDef
import com.example.domain.WorldPropType
import com.example.domain.WorldState
import com.example.domain.getHarvesterChassisDimensions
import com.example.domain.getImplementDef
import com.example.domain.getVehicleDef
import com.example.domain.resolveBuildingVisualProfile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal data class VehicleCollisionOutcome(
    val collided: Boolean,
    val resolvedX: Double,
    val resolvedZ: Double,
    val postSpeedMs: Double,
    val impactSpeedMs: Double,
    val obstacleLabel: String,
)

internal data class PointCollisionOutcome(
    val collided: Boolean,
    val resolvedX: Double,
    val resolvedZ: Double,
)

private data class ObbBox2D(
    val cx: Double,
    val cz: Double,
    val halfW: Double,
    val halfD: Double,
    val cosA: Double,
    val sinA: Double,
    val label: String,
)

private fun toRadC(deg: Double): Double = deg * PI / 180.0

private fun pushCircleOutOfCircle(
    px: Double,
    pz: Double,
    pRad: Double,
    ox: Double,
    oz: Double,
    oRad: Double,
): Triple<Double, Double, Double>? {
    val dx = px - ox
    val dz = pz - oz
    val dist = hypot(dx, dz)
    val minDist = pRad + oRad
    if (dist >= minDist) return null
    val nx: Double
    val nz: Double
    if (dist > 1e-4) {
        nx = dx / dist
        nz = dz / dist
    } else {
        nx = 0.0
        nz = 1.0
    }
    val overlap = minDist - dist
    return Triple(nx, nz, overlap)
}

/**
 * Returns (normalX, normalZ, overlap) pushing circle (px, pz, pRad) out of oriented box.
 * Returns null if separated.
 */
private fun pushCircleOutOfObb(
    px: Double,
    pz: Double,
    pRad: Double,
    box: ObbBox2D,
): Triple<Double, Double, Double>? {
    val dx = px - box.cx
    val dz = pz - box.cz
    // Transform into box local coordinates
    val lx = dx * box.cosA - dz * box.sinA
    val lz = dx * box.sinA + dz * box.cosA

    val clampedX = lx.coerceIn(-box.halfW, box.halfW)
    val clampedZ = lz.coerceIn(-box.halfD, box.halfD)

    val diffX = lx - clampedX
    val diffZ = lz - clampedZ
    val distSq = diffX * diffX + diffZ * diffZ

    if (distSq >= pRad * pRad) return null

    val localNx: Double
    val localNz: Double
    val overlap: Double

    if (distSq > 1e-8) {
        val dist = kotlin.math.sqrt(distSq)
        localNx = diffX / dist
        localNz = diffZ / dist
        overlap = pRad - dist
    } else {
        // Circle center is inside the box; push out along minimum penetration axis
        val penX = box.halfW - abs(lx)
        val penZ = box.halfD - abs(lz)
        if (penX < penZ) {
            localNx = if (lx >= 0.0) 1.0 else -1.0
            localNz = 0.0
            overlap = penX + pRad
        } else {
            localNx = 0.0
            localNz = if (lz >= 0.0) 1.0 else -1.0
            overlap = penZ + pRad
        }
    }

    // Rotate normal back into world space (inverse rotation)
    val worldNx = localNx * box.cosA + localNz * box.sinA
    val worldNz = -localNx * box.sinA + localNz * box.cosA
    return Triple(worldNx, worldNz, overlap)
}

private fun appendBuildingColliders(
    b: PlacedBuilding,
    mapId: String,
    nearX: Double,
    nearZ: Double,
    boxesOut: MutableList<ObbBox2D>,
    circlesOut: MutableList<Triple<Double, Double, Double>>,
    circleLabelsOut: MutableList<String>,
) {
    val profile = resolveBuildingVisualProfile(b, mapId)
    if (profile.isWalkableSurface) return

    val maxReach = max(profile.widthM, profile.depthM) * 0.85 + 9.0
    if (abs(b.x - nearX) > maxReach || abs(b.z - nearZ) > maxReach) return

    val rotRad = toRadC(b.rotationDeg)
    val cosA = cos(rotRad)
    val sinA = sin(rotRad)
    val hw = profile.widthM * 0.5
    val hd = profile.depthM * 0.5
    val label = profile.displayLabel

    fun localOffset(lx: Double, lz: Double): Pair<Double, Double> {
        val wx = b.x + lx * cosA + lz * sinA
        val wz = b.z - lx * sinA + lz * cosA
        return wx to wz
    }

    when (b.type) {
        BuildingType.ARVORE_ORNAMENTAL -> {
            circlesOut.add(Triple(b.x, b.z, 0.65))
            circleLabelsOut.add(label)
        }

        BuildingType.SILO, BuildingType.TANQUE_COMBUSTIVEL -> {
            val rad = max(hw, hd) * 0.92
            circlesOut.add(Triple(b.x, b.z, rad))
            circleLabelsOut.add(label)
        }

        BuildingType.PORTEIRA -> {
            val postOffset = max(0.8, hw - 0.18)
            val p1 = localOffset(-postOffset, 0.0)
            val p2 = localOffset(postOffset, 0.0)
            circlesOut.add(Triple(p1.first, p1.second, 0.34))
            circleLabelsOut.add("mourão da porteira")
            circlesOut.add(Triple(p2.first, p2.second, 0.34))
            circleLabelsOut.add("mourão da porteira")
            if (!b.isGateOpen) {
                boxesOut.add(ObbBox2D(b.x, b.z, max(0.6, hw - 0.15), 0.32, cosA, sinA, label))
            }
        }

        BuildingType.CERCA -> {
            boxesOut.add(ObbBox2D(b.x, b.z, hw, max(0.32, hd), cosA, sinA, label))
        }

        BuildingType.GALPAO, BuildingType.GARAGEM -> {
            if (profile.isOpenBay) {
                // Drive-in open-front bay: 3 exterior walls (rear + left + right) so vehicles drive inside cleanly
                val wallT = 0.42
                val rearPos = localOffset(0.0, hd - wallT)
                val leftPos = localOffset(-hw + wallT, 0.0)
                val rightPos = localOffset(hw - wallT, 0.0)
                boxesOut.add(ObbBox2D(rearPos.first, rearPos.second, hw, wallT, cosA, sinA, "parede do $label"))
                boxesOut.add(ObbBox2D(leftPos.first, leftPos.second, wallT, hd, cosA, sinA, "parede do $label"))
                boxesOut.add(ObbBox2D(rightPos.first, rightPos.second, wallT, hd, cosA, sinA, "parede do $label"))
            } else {
                boxesOut.add(ObbBox2D(b.x, b.z, hw * 0.96, hd * 0.96, cosA, sinA, label))
            }
        }

        BuildingType.CURRAL -> {
            val wallT = 0.36
            val rearPos = localOffset(0.0, hd - wallT)
            val leftPos = localOffset(-hw + wallT, 0.0)
            val rightPos = localOffset(hw - wallT, 0.0)
            val frontLeft = localOffset(-hw * 0.68, -hd + wallT)
            val frontRight = localOffset(hw * 0.68, -hd + wallT)
            boxesOut.add(ObbBox2D(rearPos.first, rearPos.second, hw, wallT, cosA, sinA, label))
            boxesOut.add(ObbBox2D(leftPos.first, leftPos.second, wallT, hd, cosA, sinA, label))
            boxesOut.add(ObbBox2D(rightPos.first, rightPos.second, wallT, hd, cosA, sinA, label))
            boxesOut.add(ObbBox2D(frontLeft.first, frontLeft.second, hw * 0.32, wallT, cosA, sinA, label))
            boxesOut.add(ObbBox2D(frontRight.first, frontRight.second, hw * 0.32, wallT, cosA, sinA, label))
        }

        BuildingType.POSTO_COMBUSTIVEL -> {
            // Central fuel pump island & kiosk core; canopy overhangs freely so trucks & tractors park beside the pumps
            boxesOut.add(ObbBox2D(b.x, b.z, max(1.4, hw * 0.34), max(1.2, hd * 0.36), cosA, sinA, label))
        }

        BuildingType.ESTACAO_GRAOS, BuildingType.COOPERATIVA -> {
            // Side weighbridge booth off to one edge while weighbridge plate stays drivable
            val boothPos = localOffset(-hw * 0.62, 0.0)
            boxesOut.add(ObbBox2D(boothPos.first, boothPos.second, max(1.2, hw * 0.28), max(1.6, hd * 0.55), cosA, sinA, label))
        }

        else -> {
            // Houses, barns, offices, workshops, warehouses, factories: solid exterior footprint
            val coreW = if (profile.isHouse && profile.hasGarage) hw * 0.78 else hw * 0.96
            boxesOut.add(ObbBox2D(b.x, b.z, coreW, hd * 0.96, cosA, sinA, label))

            // Attached veranda pillars on front porch edge (leave steps walkable while stopping clipping through posts)
            if (profile.isHouse && profile.hasVeranda) {
                val porchZ = -hd - 0.95
                for (frac in listOf(-0.85, 0.85)) {
                    val post = localOffset(coreW * frac, porchZ)
                    circlesOut.add(Triple(post.first, post.second, 0.24))
                    circleLabelsOut.add("pilar da varanda")
                }
            }

            // Attached open-front side garage wing on large/upgraded houses
            if (profile.isHouse && profile.hasGarage) {
                val garHalfW = max(1.6, hw * 0.34)
                val garHalfD = max(2.2, hd * 0.72)
                val garCenterX = coreW + garHalfW
                val wallT = 0.28
                val rearG = localOffset(garCenterX, garHalfD - wallT)
                val outerG = localOffset(garCenterX + garHalfW - wallT, 0.0)
                boxesOut.add(ObbBox2D(rearG.first, rearG.second, garHalfW, wallT, cosA, sinA, "parede da garagem"))
                boxesOut.add(ObbBox2D(outerG.first, outerG.second, wallT, garHalfD, cosA, sinA, "parede da garagem"))
            }
        }
    }
}

private fun appendPropColliders(
    state: WorldState,
    nearX: Double,
    nearZ: Double,
    circlesOut: MutableList<Triple<Double, Double, Double>>,
    circleLabelsOut: MutableList<String>,
) {
    for (p in state.props) {
        if (abs(p.x - nearX) > 10.0 || abs(p.z - nearZ) > 10.0) continue
        val (rad, lbl) = when (p.type) {
            WorldPropType.TREE,
            WorldPropType.TREE_PINE,
            WorldPropType.TREE_EUCALYPTUS,
            WorldPropType.TREE_FRUIT,
            WorldPropType.TREE_NATIVE,
            WorldPropType.TREE_ARAUCARIA,
            WorldPropType.TREE_CERRADO,
            WorldPropType.TREE_PALM_AMAZON,
            WorldPropType.TREE_IPE_AMARELO,
            WorldPropType.TREE_IPE_ROXO,
            WorldPropType.TREE_SHADE_MANGUEIRA -> 0.72 to "tronco de árvore"
            WorldPropType.BANANA_CLUMP -> 0.58 to "bananeira"
            WorldPropType.UTILITY_POLE -> 0.42 to "poste de energia"
            WorldPropType.STONE -> 0.75 to "pedra"
            WorldPropType.LOG -> 0.56 to "tora de madeira"
            WorldPropType.BALE_ROUND, WorldPropType.BALE_RECT -> 0.72 to "fardo"
            WorldPropType.SILAGE_BAG -> 0.58 to "saco de silagem"
            WorldPropType.CACTUS_MANDACARU -> 0.55 to "mandacaru"
            WorldPropType.COFFEE_SHRUB -> 0.48 to "cafeeiro"
            else -> continue
        }
        circlesOut.add(Triple(p.x, p.z, rad))
        circleLabelsOut.add(lbl)
    }
}

private fun vehicleWidthAndLength(def: VehicleDef): Pair<Double, Double> {
    val w = if (def.bodyStyle == VehicleBodyStyle.HARVESTER) {
        getHarvesterChassisDimensions(def).chassisWidthM
    } else {
        def.dimensionsM[1]
    }
    val l = def.dimensionsM[0]
    return w to l
}

private fun gatherNearbyObstacles(
    state: WorldState,
    nearX: Double,
    nearZ: Double,
    excludeVehicleUid: String?,
    excludeImplementUid: String?,
): Pair<List<ObbBox2D>, List<Pair<Triple<Double, Double, Double>, String>>> {
    val boxes = ArrayList<ObbBox2D>(24)
    val circles = ArrayList<Triple<Double, Double, Double>>(24)
    val circleLabels = ArrayList<String>(24)

    for (b in state.buildings) {
        appendBuildingColliders(b, state.mapId, nearX, nearZ, boxes, circles, circleLabels)
    }

    appendPropColliders(state, nearX, nearZ, circles, circleLabels)

    val gridSide = getGridSide(state)
    for (est in ESTABLISHMENTS) {
        val estX = est.scaledWorldX(gridSide)
        val estZ = est.scaledWorldZ(gridSide)
        if (abs(estX - nearX) > 14.0 || abs(estZ - nearZ) > 14.0) continue
        // Small side scale pillar so vehicles can drive straight onto the weighbridge pad without hitting center
        circles.add(Triple(estX - 5.2, estZ - 4.2, 1.35))
        circleLabels.add(est.name)
    }

    for (other in state.vehicles) {
        if (other.uid == excludeVehicleUid) continue
        if (abs(other.x - nearX) > 14.0 || abs(other.z - nearZ) > 14.0) continue
        val oDef = getVehicleDef(other.defId)
        val (ow, ol) = vehicleWidthAndLength(oDef)
        val rot = toRadC(other.headingDeg)
        boxes.add(
            ObbBox2D(
                cx = other.x,
                cz = other.z,
                halfW = max(0.75, ow * 0.47),
                halfD = max(1.1, ol * 0.47),
                cosA = cos(rot),
                sinA = sin(rot),
                label = "${oDef.brand} ${oDef.model}",
            )
        )
    }

    for (imp in state.implements) {
        if (imp.uid == excludeImplementUid) continue
        if (abs(imp.x - nearX) > 14.0 || abs(imp.z - nearZ) > 14.0) continue
        val iDef = getImplementDef(imp.defId)
        val halfW = (iDef.workingWidthM * 0.42).coerceIn(0.85, 3.2)
        val halfL = max(1.0, min(iDef.workingWidthM * 0.35 + 0.8, 3.0))
        val rot = toRadC(imp.headingDeg)
        boxes.add(
            ObbBox2D(
                cx = imp.x,
                cz = imp.z,
                halfW = halfW,
                halfD = halfL,
                cosA = cos(rot),
                sinA = sin(rot),
                label = "${iDef.brand} ${iDef.model}",
            )
        )
    }

    for (rv in state.roadVehicles.orEmpty()) {
        if (abs(rv.x - nearX) > 12.0 || abs(rv.z - nearZ) > 12.0) continue
        val isTruck = rv.kind == "truck"
        val rot = toRadC(rv.headingDeg)
        boxes.add(
            ObbBox2D(
                cx = rv.x,
                cz = rv.z,
                halfW = if (isTruck) 1.25 else 1.0,
                halfD = if (isTruck) 3.4 else 2.4,
                cosA = cos(rot),
                sinA = sin(rot),
                label = "tráfego rodoviário",
            )
        )
    }

    return boxes to circles.zip(circleLabels)
}

internal fun resolveVehicleWorldCollision(
    state: WorldState,
    veh: OwnedVehicle,
    vehDef: VehicleDef,
    attachedImp: OwnedImplement?,
    candX: Double,
    candZ: Double,
    headingDeg: Double,
    speedMs: Double,
): VehicleCollisionOutcome {
    val (bodyW, bodyL) = vehicleWidthAndLength(vehDef)
    val sphereRad = (bodyW * 0.46).coerceIn(0.78, 1.95)
    val spineHalf = max(0.0, bodyL * 0.46 - sphereRad)
    val rad = toRadC(headingDeg)
    val fwdX = sin(rad)
    val fwdZ = cos(rad)

    val (boxes, circles) = gatherNearbyObstacles(
        state = state,
        nearX = candX,
        nearZ = candZ,
        excludeVehicleUid = veh.uid,
        excludeImplementUid = veh.attachedImplementUid,
    )

    var rx = candX
    var rz = candZ
    var hit = false
    var maxInwardImpact = 0.0
    var hitLabel = ""

    // Iterative MTV resolution across 3 longitudinal body spheres (front hood, center, rear)
    val offsets = if (spineHalf > 0.25) doubleArrayOf(-spineHalf, 0.0, spineHalf) else doubleArrayOf(0.0)
    var inwardNormalDot = 0.0

    repeat(2) {
        for (off in offsets) {
            val sx = rx + fwdX * off
            val sz = rz + fwdZ * off
            for (box in boxes) {
                val res = pushCircleOutOfObb(sx, sz, sphereRad, box) ?: continue
                val (nx, nz, overlap) = res
                rx += nx * overlap
                rz += nz * overlap
                hit = true
                val velX = fwdX * speedMs
                val velZ = fwdZ * speedMs
                val vn = velX * nx + velZ * nz
                if (vn < 0.0) {
                    inwardNormalDot = min(inwardNormalDot, vn)
                    if (-vn > maxInwardImpact) {
                        maxInwardImpact = -vn
                        hitLabel = box.label
                    }
                } else if (hitLabel.isEmpty()) {
                    hitLabel = box.label
                }
            }
            for ((circle, label) in circles) {
                val res = pushCircleOutOfCircle(sx, sz, sphereRad, circle.first, circle.second, circle.third) ?: continue
                val (nx, nz, overlap) = res
                rx += nx * overlap
                rz += nz * overlap
                hit = true
                val velX = fwdX * speedMs
                val velZ = fwdZ * speedMs
                val vn = velX * nx + velZ * nz
                if (vn < 0.0) {
                    inwardNormalDot = min(inwardNormalDot, vn)
                    if (-vn > maxInwardImpact) {
                        maxInwardImpact = -vn
                        hitLabel = label
                    }
                } else if (hitLabel.isEmpty()) {
                    hitLabel = label
                }
            }
        }
    }

    // If an implement is attached, verify its center doesn't clip through static walls/fences/trees
    if (attachedImp != null) {
        val impDef = getImplementDef(attachedImp.defId)
        val impRad = (impDef.workingWidthM * 0.28).coerceIn(0.65, 1.85)
        for (box in boxes) {
            // Skip vehicles & road traffic check here or include everything not excluded
            val res = pushCircleOutOfObb(attachedImp.x, attachedImp.z, impRad, box) ?: continue
            val (nx, nz, overlap) = res
            rx += nx * min(overlap, 0.45)
            rz += nz * min(overlap, 0.45)
            hit = true
            if (hitLabel.isEmpty()) hitLabel = "${box.label} (implemento)"
        }
    }

    // Clean velocity stopping/sliding: remove inward component so head-on stops cleanly & angled brushes slide
    val postSpeed = if (hit) {
        if (inwardNormalDot < -0.25) {
            val headOnFactor = (-inwardNormalDot / max(abs(speedMs), 0.1)).coerceIn(0.0, 1.0)
            if (headOnFactor > 0.65) {
                0.0
            } else {
                speedMs * (1.0 - headOnFactor * 0.85)
            }
        } else {
            speedMs * 0.55
        }
    } else {
        speedMs
    }

    return VehicleCollisionOutcome(
        collided = hit,
        resolvedX = rx,
        resolvedZ = rz,
        postSpeedMs = postSpeed,
        impactSpeedMs = maxInwardImpact,
        obstacleLabel = hitLabel.ifBlank { "obstáculo" },
    )
}

internal fun resolvePointWorldCollision(
    state: WorldState,
    candX: Double,
    candZ: Double,
    radius: Double = 0.45,
    excludeVehicleUid: String? = null,
): PointCollisionOutcome {
    val (boxes, circles) = gatherNearbyObstacles(
        state = state,
        nearX = candX,
        nearZ = candZ,
        excludeVehicleUid = excludeVehicleUid,
        excludeImplementUid = null,
    )
    var rx = candX
    var rz = candZ
    var hit = false

    repeat(2) {
        for (box in boxes) {
            val res = pushCircleOutOfObb(rx, rz, radius, box) ?: continue
            rx += res.first * res.third
            rz += res.second * res.third
            hit = true
        }
        for ((circle, _) in circles) {
            val res = pushCircleOutOfCircle(rx, rz, radius, circle.first, circle.second, circle.third) ?: continue
            rx += res.first * res.third
            rz += res.second * res.third
            hit = true
        }
    }
    return PointCollisionOutcome(hit, rx, rz)
}

internal fun isPointBlockedBySolidWorld(
    state: WorldState,
    px: Double,
    pz: Double,
    radius: Double = 0.50,
    excludeVehicleUid: String? = null,
): Boolean {
    val (boxes, circles) = gatherNearbyObstacles(
        state = state,
        nearX = px,
        nearZ = pz,
        excludeVehicleUid = excludeVehicleUid,
        excludeImplementUid = null,
    )
    for (box in boxes) {
        if (pushCircleOutOfObb(px, pz, radius, box) != null) return true
    }
    for ((circle, _) in circles) {
        if (pushCircleOutOfCircle(px, pz, radius, circle.first, circle.second, circle.third) != null) return true
    }
    return false
}

internal fun findSafeExitSpot(state: WorldState, veh: OwnedVehicle): Pair<Double, Double> {
    val def = getVehicleDef(veh.defId)
    val (w, l) = vehicleWidthAndLength(def)
    val sideOffset = max(1.85, w * 0.55 + 0.9)
    val endOffset = max(2.2, l * 0.55 + 1.1)
    val rad = toRadC(veh.headingDeg)
    val fwdX = sin(rad)
    val fwdZ = cos(rad)
    val rightX = cos(rad)
    val rightZ = -sin(rad)

    val candidates = listOf(
        // Left door (standard tractor/truck exit side)
        (veh.x - rightX * sideOffset) to (veh.z - rightZ * sideOffset),
        // Right door
        (veh.x + rightX * sideOffset) to (veh.z + rightZ * sideOffset),
        // Further left / right
        (veh.x - rightX * (sideOffset + 1.1)) to (veh.z - rightZ * (sideOffset + 1.1)),
        (veh.x + rightX * (sideOffset + 1.1)) to (veh.z + rightZ * (sideOffset + 1.1)),
        // Behind / Ahead
        (veh.x - fwdX * endOffset) to (veh.z - fwdZ * endOffset),
        (veh.x + fwdX * endOffset) to (veh.z + fwdZ * endOffset),
    )

    val gridSide = getGridSide(state)
    for ((cx, cz) in candidates) {
        val cell = getCellIndex(cx, cz, gridSide)?.let { state.cells.getOrNull(it) }
        if (cell?.soilState == com.example.domain.SoilState.AGUA_RIO) continue
        if (!isPointBlockedBySolidWorld(state, cx, cz, radius = 0.45, excludeVehicleUid = null)) {
            return cx to cz
        }
    }
    val fallback = candidates.first()
    val resolved = resolvePointWorldCollision(state, fallback.first, fallback.second, radius = 0.45, excludeVehicleUid = null)
    return resolved.resolvedX to resolved.resolvedZ
}

/**
 * Pulls camera forward along view segment if any closed building or silo blocks the camera position.
 */
fun clampCameraSegmentAgainstBuildings(
    targetX: Float,
    targetY: Float,
    targetZ: Float,
    camX: Float,
    camY: Float,
    camZ: Float,
    state: WorldState,
): Triple<Float, Float, Float> {
    val boxes = ArrayList<ObbBox2D>(8)
    val circles = ArrayList<Triple<Double, Double, Double>>(4)
    val labels = ArrayList<String>(4)
    val midX = (targetX + camX) * 0.5
    val midZ = (targetZ + camZ) * 0.5
    for (b in state.buildings) {
        if (b.type == BuildingType.CERCA || b.type == BuildingType.PORTEIRA || b.type == BuildingType.CURRAL || b.type == BuildingType.ARVORE_ORNAMENTAL) continue
        appendBuildingColliders(b, state.mapId, midX, midZ, boxes, circles, labels)
    }
    if (boxes.isEmpty() && circles.isEmpty()) return Triple(camX, camY, camZ)

    val steps = 8
    var safeT = 1.0f
    for (i in 1..steps) {
        val t = i.toFloat() / steps
        val sx = (targetX + (camX - targetX) * t).toDouble()
        val sz = (targetZ + (camZ - targetZ) * t).toDouble()
        val blocked = boxes.any { pushCircleOutOfObb(sx, sz, 0.45, it) != null } ||
            circles.any { pushCircleOutOfCircle(sx, sz, 0.45, it.first, it.second, it.third) != null }
        if (blocked) {
            safeT = max(0.18f, (i - 1).toFloat() / steps)
            break
        }
    }
    return Triple(
        targetX + (camX - targetX) * safeT,
        targetY + (camY - targetY) * safeT,
        targetZ + (camZ - targetZ) * safeT,
    )
}
