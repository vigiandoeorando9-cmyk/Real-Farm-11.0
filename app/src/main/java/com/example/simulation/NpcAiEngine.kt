package com.example.simulation

import com.example.domain.NpcAgentState
import com.example.domain.SoilState
import com.example.domain.WEATHER_META
import com.example.domain.WorldState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private fun toRad(deg: Double): Double = (deg * PI) / 180.0
private fun toDeg(rad: Double): Double = (rad * 180.0) / PI

private fun createNpcRng(seed: Int): () -> Double {
    var s = seed.toLong() and 0xFFFFFFFFL
    if (s == 0L) s = 1L
    return {
        s = (s * 1664525L + 1013904223L) and 0xFFFFFFFFL
        s.toDouble() / 4294967296.0
    }
}

private data class NpcTemplate(
    val uid: String,
    val name: String,
    val role: String,
    val homeX: Double,
    val homeZ: Double,
    val activity: String,
    val dialogLine: String,
    val skinHex: String,
    val shirtHex: String,
    val pantsHex: String,
    val hatHex: String?,
)

private val NPC_TEMPLATES = listOf(
    NpcTemplate(
        "npc_ze_mecanico", "Seu Zé da Oficina", "mecanico", 25.0, 24.0,
        "Revisando ferramentas e tratores perto da oficina",
        "Bom dia! Se o motor perder força ou a embreagem patinar, encosta na oficina que damos um talento.",
        "#c88c66", "#2e4a7d", "#1f2937", "#991b1b"
    ),
    NpcTemplate(
        "npc_dona_aparecida", "Dona Aparecida", "vizinho", -72.0, 18.0,
        "Cuidando do canteiro de rosas da agricultura familiar",
        "As rosas familiares dão um aroma danado e têm ótimo valor no CEASA local. Cuide bem da umidade!",
        "#d9a17a", "#be185d", "#3f3f46", "#e7c675"
    ),
    NpcTemplate(
        "npc_joao_trab", "João Batista", "trabalhador_rural", -12.0, 14.0,
        "Organizando insumos no pátio da sede",
        "Os talhões foram demarcados seguindo as curvas do rio. Precisando de braço extra, contrate na aba Equipe!",
        "#9d6847", "#3f6212", "#27272a", "#d4a359"
    ),
    NpcTemplate(
        "npc_maria_comerciante", "Dona Helena (Mercado)", "comerciante", 151.0, -84.0,
        "Atendendo clientes no Mercado Rural da Vila",
        "Aqui na Vila Nova Esperança compramos flores, frutas e hortaliças direto dos sítios da região.",
        "#e2b291", "#047857", "#1e293b", null
    ),
    NpcTemplate(
        "npc_marcos_motorista", "Marcos Estrada", "motorista_caminhao", -60.0, -64.0,
        "Conferindo pneus e lona perto da rodovia",
        "A rodovia estadual corta a região de ponta a ponta. Cuidado na ponte do rio com bitrem carregado!",
        "#b87b56", "#b45309", "#1e3a5f", "#18181b"
    ),
    NpcTemplate(
        "npc_sr_antonio", "Sr. Antônio (Proprietário)", "proprietario_terra", 128.0, -102.0,
        "Vistoriando lotes urbanos e áreas para arrendamento",
        "Tenho talhões pequenos para começar devagar e glebas grandes para safra cheia. Arrendamento mensal ou safral!",
        "#d19874", "#78350f", "#44403c", "#f5deb3"
    ),
    NpcTemplate(
        "npc_paula_vila", "Paula Florista", "morador_vila", 145.0, -95.0,
        "Conversando na praça da vila rural",
        "As estufas de rosas e flores estão trazendo renda nova para as famílias do vilarejo.",
        "#e0ad8a", "#9d174d", "#334155", null
    ),
    NpcTemplate(
        "npc_lucas_vila", "Lucas da Cooperativa", "morador_vila", 147.0, -93.0,
        "Trocando ideia sobre fretes e serviços terceirizados",
        "Sem dinheiro para comprar colheitadeira agora? Contrate uma empresa terceirizada ou aceite contratos com máquina emprestada!",
        "#c68a63", "#1d4ed8", "#1f2937", "#ca8a04"
    ),
    NpcTemplate(
        "npc_tiao_pasto", "Tião Boiadeiro", "trabalhador_rural", 28.0, 6.0,
        "Cuidando do manejo do curral e aguada",
        "Gado bem nutrido com feno e silagem rende leite todo dia e aumenta o rebanho.",
        "#8d583a", "#7c2d12", "#292524", "#a16207"
    ),
    NpcTemplate(
        "npc_claudio_concessionaria", "Cláudio Consultor", "comerciante", -148.0, -64.0,
        "Apresentando tratores e colheitadeiras históricas na revenda",
        "Temos modelos históricos brasileiros verificados como o JD 6300 e a linha SLC 2200 a 1185 prontos para inspeção!",
        "#d59d78", "#15803d", "#1e293b", "#166534"
    ),
    NpcTemplate(
        "npc_fernando_silagem", "Fernando Vizinho", "vizinho", -78.0, -45.0,
        "Acompanhando lavoura vizinha",
        "Quando você comprar dois talhões colados, dá até para mesclar as áreas e tirar as cercas divisórias.",
        "#c2835e", "#4d7c0f", "#27272a", "#eab308"
    ),
    NpcTemplate(
        "npc_ana_agronoma", "Dra. Ana Agrônoma", "vizinho", 168.0, -88.0,
        "Analisando solo e flores na estufa da vila",
        "Corrija o pH com calcário antes de semear rosas ou soja para destravar a produtividade máxima.",
        "#dfa985", "#0f766e", "#334155", "#f8fafc"
    ),
)

fun createInitialNpcAgents(seed: Int, coordScale: Double = 1.0): List<NpcAgentState> {
    val rng = createNpcRng(seed + 719)
    return NPC_TEMPLATES.mapIndexed { idx, tpl ->
        val hx = tpl.homeX * coordScale
        val hz = tpl.homeZ * coordScale
        val isConversingPair = tpl.uid == "npc_paula_vila" || tpl.uid == "npc_lucas_vila"
        NpcAgentState(
            uid = tpl.uid,
            name = tpl.name,
            role = tpl.role,
            x = hx,
            z = hz,
            headingDeg = (rng() * 360.0).roundToInt().toDouble(),
            speedMs = 0.0,
            gaitPhase = idx * 0.7,
            targetX = hx + (rng() - 0.5) * 14.0,
            targetZ = hz + (rng() - 0.5) * 14.0,
            homeX = hx,
            homeZ = hz,
            activity = tpl.activity,
            surfaceType = if (abs(hx - 150.0) < 40.0 && abs(hz + 90.0) < 30.0) "asfalto" else "terra",
            dialogLine = tpl.dialogLine,
            companionNpcUid = when (tpl.uid) {
                "npc_paula_vila" -> "npc_lucas_vila"
                "npc_lucas_vila" -> "npc_paula_vila"
                else -> null
            },
            ridingVehicleUid = null,
            skinHex = tpl.skinHex,
            shirtHex = tpl.shirtHex,
            pantsHex = tpl.pantsHex,
            hatHex = tpl.hatHex,
            state = if (isConversingPair) "conversing" else "walking",
            pauseTimerSec = if (isConversingPair) 4.0 + idx else rng() * 3.0,
        )
    }
}

fun detectSurfaceAt(state: WorldState, wx: Double, wz: Double): String {
    val gridSide = getGridSide(state)
    val idx = getCellIndex(wx, wz, gridSide) ?: return "grama"
    val cell = state.cells.getOrNull(idx) ?: return "grama"
    return when (cell.soilState) {
        SoilState.ESTRADA_ASFALTO, SoilState.PONTE -> "asfalto"
        SoilState.ESTRADA_TERRA -> if (cell.moisture > 0.78) "lama" else "terra"
        SoilState.ARADO, SoilState.SUBSOLADO -> if (cell.moisture > 0.75) "lama" else "terra"
        else -> "grama"
    }
}

private fun computeAvoidanceVector(x: Double, z: Double, state: WorldState): Pair<Double, Double> {
    var ax = 0.0
    var az = 0.0
    for (b in state.buildings) {
        val dx = x - b.x
        val dz = z - b.z
        val d = hypot(dx, dz)
        if (d > 0.05 && d < 6.8) {
            val push = (6.8 - d) / 6.8
            ax += (dx / d) * push * 1.4
            az += (dz / d) * push * 1.4
        }
    }
    for (v in state.vehicles) {
        val dx = x - v.x
        val dz = z - v.z
        val d = hypot(dx, dz)
        if (d > 0.05 && d < 5.5) {
            val push = (5.5 - d) / 5.5
            ax += (dx / d) * push * 2.1
            az += (dz / d) * push * 2.1
        }
    }
    for (prop in state.props) {
        if (abs(x - prop.x) > 3.2 || abs(z - prop.z) > 3.2) continue
        val dx = x - prop.x
        val dz = z - prop.z
        val d = hypot(dx, dz)
        if (d > 0.04 && d < 2.6) {
            val push = (2.6 - d) / 2.6
            ax += (dx / d) * push * 1.65
            az += (dz / d) * push * 1.65
        }
    }
    val gridSide = getGridSide(state)
    val dirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    for ((sx, sz) in dirs) {
        val idx = getCellIndex(x + sx * 2.6, z + sz * 2.6, gridSide)
        if (idx != null && state.cells.getOrNull(idx)?.soilState == SoilState.AGUA_RIO) {
            ax -= sx * 2.2
            az -= sz * 2.2
        }
    }
    return ax to az
}

fun stepNpcAgentsSimulation(state: WorldState, dtSec: Double): List<NpcAgentState> {
    val agents = state.npcAgents ?: emptyList()
    if (agents.isEmpty()) return agents

    val dt = dtSec.coerceIn(0.001, 0.08)
    val wMeta = WEATHER_META[state.weather] ?: WEATHER_META.values.first()
    val isHeavyRain = wMeta.rainIntensity > 0.55
    val isNight = state.hourOfDay < 5.5 || state.hourOfDay > 20.5
    val activeVeh = state.activeVehicleUid?.let { uid -> state.vehicles.find { it.uid == uid } }

    return agents.mapIndexed { idx, npc ->
        if (activeVeh != null && abs(activeVeh.speedMs) > 1.2) {
            val distVeh = hypot(npc.x - activeVeh.x, npc.z - activeVeh.z)
            if (distVeh < 8.5) {
                val awayHeading = toDeg(atan2(npc.x - activeVeh.x, npc.z - activeVeh.z))
                val speed = 2.4
                val nx = npc.x + sin(toRad(awayHeading)) * speed * dt
                val nz = npc.z + cos(toRad(awayHeading)) * speed * dt
                return@mapIndexed npc.copy(
                    x = nx,
                    z = nz,
                    headingDeg = awayHeading,
                    speedMs = speed,
                    gaitPhase = npc.gaitPhase + speed * dt * 2.3,
                    state = "walking",
                    surfaceType = detectSurfaceAt(state, nx, nz),
                )
            }
        }

        if (npc.state == "conversing" && npc.companionNpcUid != null && !isHeavyRain && !isNight) {
            val partner = agents.find { it.uid == npc.companionNpcUid }
            val timer = (npc.pauseTimerSec ?: 6.0) - dt
            val faceHeading = if (partner != null) {
                toDeg(atan2(partner.x - npc.x, partner.z - npc.z))
            } else npc.headingDeg
            if (timer > 0.0) {
                return@mapIndexed npc.copy(
                    headingDeg = faceHeading,
                    speedMs = 0.0,
                    pauseTimerSec = timer,
                )
            }
            val nextAngle = ((state.day * 71 + idx * 53) % 360) * (PI / 180.0)
            return@mapIndexed npc.copy(
                state = "walking",
                targetX = npc.homeX + cos(nextAngle) * 14.0,
                targetZ = npc.homeZ + sin(nextAngle) * 14.0,
                pauseTimerSec = 0.0,
            )
        }

        val goalX = if (isHeavyRain || isNight) npc.homeX else npc.targetX
        val goalZ = if (isHeavyRain || isNight) npc.homeZ else npc.targetZ

        val dx = goalX - npc.x
        val dz = goalZ - npc.z
        val dist = hypot(dx, dz)

        if (dist < 1.35) {
            if (isHeavyRain || isNight) {
                return@mapIndexed npc.copy(
                    speedMs = 0.0,
                    state = "idle",
                    surfaceType = detectSurfaceAt(state, npc.x, npc.z),
                )
            }
            val pauseLeft = (npc.pauseTimerSec ?: 0.0) - dt
            if (pauseLeft > 0.0) {
                return@mapIndexed npc.copy(
                    speedMs = 0.0,
                    state = if (npc.role == "mecanico" || npc.role == "trabalhador_rural") "working" else "idle",
                    pauseTimerSec = pauseLeft,
                )
            }
            val orbitSeed = (state.day * 19 + floor(state.hourOfDay * 4.0) + idx * 11) * 1.618
            val roamRadius = if (npc.role == "motorista_caminhao") 24.0 else 16.0
            val nextGoalX = npc.homeX + cos(orbitSeed) * roamRadius
            val nextGoalZ = npc.homeZ + sin(orbitSeed * 1.3) * roamRadius
            return@mapIndexed npc.copy(
                targetX = nextGoalX,
                targetZ = nextGoalZ,
                state = if (npc.companionNpcUid != null && idx % 2 == 0) "conversing" else "walking",
                pauseTimerSec = if (npc.companionNpcUid != null) 7.5 else 3.2,
            )
        }

        val dirX = dx / dist
        val dirZ = dz / dist
        val (ax, az) = computeAvoidanceVector(npc.x, npc.z, state)
        val steerX = dirX + ax
        val steerZ = dirZ + az
        val steerMag = max(0.001, hypot(steerX, steerZ))
        val moveDirX = steerX / steerMag
        val moveDirZ = steerZ / steerMag

        val walkSpeed = if (isHeavyRain) 2.15 else 1.48
        val nx = npc.x + moveDirX * walkSpeed * dt
        val nz = npc.z + moveDirZ * walkSpeed * dt

        val surf = detectSurfaceAt(state, nx, nz)
        val gridSide = getGridSide(state)
        val nextIdx = getCellIndex(nx, nz, gridSide)
        if (nextIdx != null && state.cells.getOrNull(nextIdx)?.soilState == SoilState.AGUA_RIO) {
            return@mapIndexed npc.copy(
                targetX = npc.homeX,
                targetZ = npc.homeZ,
                speedMs = 0.0,
                state = "idle",
            )
        }

        val desiredHeading = toDeg(atan2(moveDirX, moveDirZ))
        val diff = normalizeAngle180(desiredHeading - npc.headingDeg)
        val heading = npc.headingDeg + diff.coerceIn(-240.0 * dt, 240.0 * dt)

        npc.copy(
            x = nx,
            z = nz,
            headingDeg = heading,
            speedMs = walkSpeed,
            gaitPhase = npc.gaitPhase + walkSpeed * dt * 2.45,
            state = "walking",
            surfaceType = surf,
        )
    }
}

data class NpcInteractionResult(
    val npc: NpcAgentState?,
    val message: String,
    val updatedNpcAgents: List<NpcAgentState>?,
)

fun interactWithNearestNpc(state: WorldState): NpcInteractionResult {
    val agents = state.npcAgents ?: emptyList()
    if (agents.isEmpty()) {
        return NpcInteractionResult(null, "Nenhum morador ou trabalhador próximo no momento.", null)
    }
    var best: NpcAgentState? = null
    var bestDist = Double.POSITIVE_INFINITY
    for (a in agents) {
        val d = hypot(a.x - state.player.x, a.z - state.player.z)
        if (d < bestDist) {
            bestDist = d
            best = a
        }
    }
    if (best == null || bestDist > 6.5) {
        return NpcInteractionResult(null, "Aproxime-se de um NPC ou morador da vila para conversar.", null)
    }
    val facePlayerDeg = toDeg(atan2(state.player.x - best.x, state.player.z - best.z))
    val updatedNpcAgents = agents.map { a ->
        if (a.uid == best.uid) {
            a.copy(headingDeg = facePlayerDeg, speedMs = 0.0, state = "conversing", pauseTimerSec = 5.0)
        } else a
    }
    return NpcInteractionResult(
        npc = best,
        message = "${best.name}: \"${best.dialogLine}\"",
        updatedNpcAgents = updatedNpcAgents,
    )
}
