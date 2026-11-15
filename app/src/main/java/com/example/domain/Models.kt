package com.example.domain

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
data class RgbTriple(
    val r: Double,
    val g: Double,
    val b: Double,
) {
    operator fun get(index: Int): Double = when (index) {
        0 -> r
        1 -> g
        else -> b
    }
}

@Serializable
data class DimTriple(
    val width: Double,
    val height: Double,
    val length: Double,
) {
    operator fun get(index: Int): Double = when (index) {
        0 -> width
        1 -> height
        else -> length
    }
}

fun rgb(r: Double, g: Double, b: Double): RgbTriple = RgbTriple(r, g, b)
fun dim(w: Double, h: Double, l: Double): DimTriple = DimTriple(w, h, l)

@Serializable
enum class GraphicsPreset {
    DESKTOP_PC,
    CELULAR,
    EQUILIBRADO,
    MUITO_BAIXO,
    BAIXO,
    OTIMIZADO,
    MEDIO,
    BOM,
    ALTO,
    MUITO_ALTO,
    ULTRA_DOS_ULTRAS,
    MEGA_REALISTA,
    PERSONALIZADO,
}

@Serializable
data class GraphicsConfig(
    val textureResolutionScale: Double,
    val drawDistance: Double,
    val shadowQuality: Int,
    val lightingQuality: Int,
    val reflectionQuality: Int,
    val vegetationDensity: Double,
    val objectDensity: Double,
    val waterQuality: Int,
    val vehicleDetailLevel: Int,
    val terrainDetailStep: Int,
    val weatherEffectsDensity: Double,
    val textureTierLabel: String, // "512p" | "1K" | "2K" | "4K" | "8K"
    val saturation: Double,
    val contrast: Double,
    val exposure: Double,
    val sharpness: Double,
    val resolutionMode: String? = "nativa", // "nativa" | "maxima" | "escala"
    val resolutionScale: Double? = 0.94,
    val antialias: Boolean? = true,
    val anisotropyLevel: Int? = 8,
    val ambientOcclusion: Boolean? = true,
    val globalIllumLevel: Int? = 2,
    val materialQuality: Int? = 2,
    val npcDensity: Double? = 0.85,
    val animationDistance: Double? = 155.0,
    val particleQuality: Double? = 0.95,
    val adaptiveMobile: Boolean? = true,
    val textureResolutionPx: Int? = 2048, // 512 | 1024 | 2048 | 4096
)

data class GraphicsPresetMeta(
    val displayName: String,
    val levelNumber: Int,
    val badge: String? = null,
    val description: String? = null,
    val config: GraphicsConfig,
)

val TEXTURE_RESOLUTION_OPTIONS: List<Pair<Int, String>> = listOf(
    512 to "512x512 (Leve)",
    1024 to "1024x1024 (Padrão)",
    2048 to "2048x2048 (HD)",
    4096 to "4096x4096 (Ultra 4K)",
)

val GRAPHICS_PRESET_META: Map<GraphicsPreset, GraphicsPresetMeta> = mapOf(
    GraphicsPreset.DESKTOP_PC to GraphicsPresetMeta(
        displayName = "Desktop / PC (Qualidade Máxima)",
        levelNumber = 10,
        badge = "PC / GPU Dedicada",
        description = "Prioriza iluminação natural, sombras nítidas, reflexos em rios/lagos, 4K e distância máxima.",
        config = GraphicsConfig(
            textureResolutionScale = 0.96, drawDistance = 460.0, shadowQuality = 3, lightingQuality = 3,
            reflectionQuality = 3, vegetationDensity = 0.95, objectDensity = 0.96, waterQuality = 2,
            vehicleDetailLevel = 2, terrainDetailStep = 1, weatherEffectsDensity = 1.3, textureTierLabel = "4K",
            saturation = 1.1, contrast = 1.08, exposure = 1.02, sharpness = 0.72,
            resolutionMode = "maxima", resolutionScale = 1.0, antialias = true, anisotropyLevel = 16,
            ambientOcclusion = true, globalIllumLevel = 3, materialQuality = 3, npcDensity = 1.0,
            animationDistance = 220.0, particleQuality = 1.3, adaptiveMobile = false, textureResolutionPx = 4096,
        ),
    ),
    GraphicsPreset.CELULAR to GraphicsPresetMeta(
        displayName = "Celular (Eficiência & Bateria)",
        levelNumber = 11,
        badge = "Android / iOS Leve",
        description = "Prioriza FPS estável, baixo uso de memória RAM e controle térmico em celulares.",
        config = GraphicsConfig(
            textureResolutionScale = 0.58, drawDistance = 195.0, shadowQuality = 1, lightingQuality = 1,
            reflectionQuality = 0, vegetationDensity = 0.46, objectDensity = 0.52, waterQuality = 1,
            vehicleDetailLevel = 1, terrainDetailStep = 1, weatherEffectsDensity = 0.45, textureTierLabel = "1K",
            saturation = 1.03, contrast = 1.03, exposure = 1.0, sharpness = 0.25,
            resolutionMode = "escala", resolutionScale = 0.8, antialias = false, anisotropyLevel = 4,
            ambientOcclusion = false, globalIllumLevel = 1, materialQuality = 1, npcDensity = 0.55,
            animationDistance = 95.0, particleQuality = 0.55, adaptiveMobile = true, textureResolutionPx = 1024,
        ),
    ),
    GraphicsPreset.EQUILIBRADO to GraphicsPresetMeta(
        displayName = "Equilibrado (Padrão Fluido)",
        levelNumber = 12,
        badge = "Alta Fidelidade Fluida",
        description = "Gráficos PBR elevados com fluidez em celulares intermediários e PCs.",
        config = GraphicsConfig(
            textureResolutionScale = 0.84, drawDistance = 315.0, shadowQuality = 2, lightingQuality = 2,
            reflectionQuality = 2, vegetationDensity = 0.8, objectDensity = 0.84, waterQuality = 2,
            vehicleDetailLevel = 2, terrainDetailStep = 1, weatherEffectsDensity = 0.9, textureTierLabel = "2K",
            saturation = 1.08, contrast = 1.06, exposure = 1.02, sharpness = 0.54,
            resolutionMode = "nativa", resolutionScale = 0.94, antialias = true, anisotropyLevel = 8,
            ambientOcclusion = true, globalIllumLevel = 2, materialQuality = 2, npcDensity = 0.85,
            animationDistance = 155.0, particleQuality = 0.95, adaptiveMobile = true, textureResolutionPx = 2048,
        ),
    ),
    GraphicsPreset.MUITO_BAIXO to GraphicsPresetMeta(
        displayName = "Muito baixo",
        levelNumber = 1,
        badge = "Mínimo / Economia",
        description = "Reduz sombras, vegetação e distância de visão para máximo desempenho em aparelhos básicos.",
        config = GraphicsConfig(
            textureResolutionScale = 0.35, drawDistance = 115.0, shadowQuality = 0, lightingQuality = 0,
            reflectionQuality = 0, vegetationDensity = 0.2, objectDensity = 0.25, waterQuality = 0,
            vehicleDetailLevel = 0, terrainDetailStep = 2, weatherEffectsDensity = 0.0, textureTierLabel = "512p",
            saturation = 0.96, contrast = 1.0, exposure = 1.0, sharpness = 0.05,
            resolutionMode = "escala", resolutionScale = 0.72, antialias = false, anisotropyLevel = 2,
            ambientOcclusion = false, globalIllumLevel = 0, materialQuality = 0, npcDensity = 0.35,
            animationDistance = 65.0, particleQuality = 0.25, adaptiveMobile = true, textureResolutionPx = 512,
        ),
    ),
    GraphicsPreset.BAIXO to GraphicsPresetMeta(
        displayName = "Baixo",
        levelNumber = 2,
        badge = "Leve",
        description = "Visual limpo com sombras suaves e vegetação leve para celulares mais simples.",
        config = GraphicsConfig(
            textureResolutionScale = 0.5, drawDistance = 165.0, shadowQuality = 1, lightingQuality = 1,
            reflectionQuality = 0, vegetationDensity = 0.35, objectDensity = 0.4, waterQuality = 0,
            vehicleDetailLevel = 1, terrainDetailStep = 2, weatherEffectsDensity = 0.25, textureTierLabel = "512p",
            saturation = 1.0, contrast = 1.02, exposure = 1.0, sharpness = 0.15,
            resolutionMode = "escala", resolutionScale = 0.80, antialias = false, anisotropyLevel = 4,
            ambientOcclusion = false, globalIllumLevel = 1, materialQuality = 1, npcDensity = 0.50,
            animationDistance = 85.0, particleQuality = 0.45, adaptiveMobile = true, textureResolutionPx = 512,
        ),
    ),
    GraphicsPreset.OTIMIZADO to GraphicsPresetMeta(
        displayName = "Otimizado",
        levelNumber = 3,
        config = GraphicsConfig(
            textureResolutionScale = 0.62, drawDistance = 215.0, shadowQuality = 1, lightingQuality = 1,
            reflectionQuality = 1, vegetationDensity = 0.55, objectDensity = 0.6, waterQuality = 1,
            vehicleDetailLevel = 1, terrainDetailStep = 1, weatherEffectsDensity = 0.5, textureTierLabel = "1K",
            saturation = 1.04, contrast = 1.04, exposure = 1.0, sharpness = 0.28,
            resolutionMode = "escala", resolutionScale = 0.86, antialias = true, anisotropyLevel = 4,
            ambientOcclusion = false, globalIllumLevel = 1, materialQuality = 1, npcDensity = 0.65,
            animationDistance = 110.0, particleQuality = 0.65, adaptiveMobile = true, textureResolutionPx = 1024,
        ),
    ),
    GraphicsPreset.MEDIO to GraphicsPresetMeta(
        displayName = "Médio",
        levelNumber = 4,
        badge = "Balanceado",
        description = "Equilíbrio entre sombras, vegetação rural, estradas e fluidez em 1024x1024.",
        config = GraphicsConfig(
            textureResolutionScale = 0.74, drawDistance = 265.0, shadowQuality = 2, lightingQuality = 2,
            reflectionQuality = 1, vegetationDensity = 0.7, objectDensity = 0.75, waterQuality = 1,
            vehicleDetailLevel = 1, terrainDetailStep = 1, weatherEffectsDensity = 0.72, textureTierLabel = "1K",
            saturation = 1.06, contrast = 1.05, exposure = 1.0, sharpness = 0.4,
            resolutionMode = "nativa", resolutionScale = 0.90, antialias = true, anisotropyLevel = 8,
            ambientOcclusion = true, globalIllumLevel = 2, materialQuality = 2, npcDensity = 0.78,
            animationDistance = 135.0, particleQuality = 0.80, adaptiveMobile = true, textureResolutionPx = 1024,
        ),
    ),
    GraphicsPreset.BOM to GraphicsPresetMeta(
        displayName = "Bom",
        levelNumber = 5,
        config = GraphicsConfig(
            textureResolutionScale = 0.84, drawDistance = 320.0, shadowQuality = 2, lightingQuality = 2,
            reflectionQuality = 2, vegetationDensity = 0.82, objectDensity = 0.84, waterQuality = 2,
            vehicleDetailLevel = 2, terrainDetailStep = 1, weatherEffectsDensity = 0.92, textureTierLabel = "2K",
            saturation = 1.08, contrast = 1.06, exposure = 1.02, sharpness = 0.52,
            resolutionMode = "nativa", resolutionScale = 0.94, antialias = true, anisotropyLevel = 8,
            ambientOcclusion = true, globalIllumLevel = 2, materialQuality = 2, npcDensity = 0.86,
            animationDistance = 160.0, particleQuality = 0.95, adaptiveMobile = true, textureResolutionPx = 2048,
        ),
    ),
    GraphicsPreset.ALTO to GraphicsPresetMeta(
        displayName = "Alto",
        levelNumber = 6,
        badge = "Alta Definição",
        description = "Materiais metálicos, reflexos em água/vidros, sombras nítidas e texturas 2048x2048.",
        config = GraphicsConfig(
            textureResolutionScale = 0.92, drawDistance = 385.0, shadowQuality = 3, lightingQuality = 2,
            reflectionQuality = 2, vegetationDensity = 0.92, objectDensity = 0.92, waterQuality = 2,
            vehicleDetailLevel = 2, terrainDetailStep = 1, weatherEffectsDensity = 1.1, textureTierLabel = "2K",
            saturation = 1.1, contrast = 1.08, exposure = 1.02, sharpness = 0.65,
            resolutionMode = "nativa", resolutionScale = 0.98, antialias = true, anisotropyLevel = 16,
            ambientOcclusion = true, globalIllumLevel = 2, materialQuality = 3, npcDensity = 0.94,
            animationDistance = 185.0, particleQuality = 1.10, adaptiveMobile = true, textureResolutionPx = 2048,
        ),
    ),
    GraphicsPreset.MUITO_ALTO to GraphicsPresetMeta(
        displayName = "Muito alto",
        levelNumber = 7,
        config = GraphicsConfig(
            textureResolutionScale = 0.97, drawDistance = 470.0, shadowQuality = 3, lightingQuality = 3,
            reflectionQuality = 3, vegetationDensity = 1.0, objectDensity = 1.0, waterQuality = 2,
            vehicleDetailLevel = 2, terrainDetailStep = 1, weatherEffectsDensity = 1.28, textureTierLabel = "4K",
            saturation = 1.12, contrast = 1.1, exposure = 1.03, sharpness = 0.78,
            resolutionMode = "maxima", resolutionScale = 1.0, antialias = true, anisotropyLevel = 16,
            ambientOcclusion = true, globalIllumLevel = 3, materialQuality = 3, npcDensity = 1.0,
            animationDistance = 210.0, particleQuality = 1.25, adaptiveMobile = false, textureResolutionPx = 4096,
        ),
    ),
    GraphicsPreset.ULTRA_DOS_ULTRAS to GraphicsPresetMeta(
        displayName = "Ultra",
        levelNumber = 8,
        badge = "Simulação Máxima",
        description = "Iluminação atmosférica completa, nuvens em camadas, vegetação densa e reflexos em água/lataria.",
        config = GraphicsConfig(
            textureResolutionScale = 1.0, drawDistance = 560.0, shadowQuality = 3, lightingQuality = 3,
            reflectionQuality = 3, vegetationDensity = 1.0, objectDensity = 1.0, waterQuality = 2,
            vehicleDetailLevel = 2, terrainDetailStep = 1, weatherEffectsDensity = 1.4, textureTierLabel = "4K",
            saturation = 1.12, contrast = 1.1, exposure = 1.03, sharpness = 0.85,
            resolutionMode = "maxima", resolutionScale = 1.0, antialias = true, anisotropyLevel = 16,
            ambientOcclusion = true, globalIllumLevel = 3, materialQuality = 3, npcDensity = 1.0,
            animationDistance = 230.0, particleQuality = 1.35, adaptiveMobile = false, textureResolutionPx = 4096,
        ),
    ),
    GraphicsPreset.MEGA_REALISTA to GraphicsPresetMeta(
        displayName = "Mega-realista",
        levelNumber = 9,
        badge = "4096x4096 Realismo",
        description = "Máximo detalhe de sulcos de solo, reflexos em vidros/água, clima dinâmico, sombras longas e partículas.",
        config = GraphicsConfig(
            textureResolutionScale = 1.0, drawDistance = 640.0, shadowQuality = 3, lightingQuality = 3,
            reflectionQuality = 3, vegetationDensity = 1.0, objectDensity = 1.0, waterQuality = 2,
            vehicleDetailLevel = 2, terrainDetailStep = 1, weatherEffectsDensity = 1.5, textureTierLabel = "4K",
            saturation = 1.14, contrast = 1.12, exposure = 1.04, sharpness = 0.92,
            resolutionMode = "maxima", resolutionScale = 1.0, antialias = true, anisotropyLevel = 16,
            ambientOcclusion = true, globalIllumLevel = 3, materialQuality = 3, npcDensity = 1.0,
            animationDistance = 260.0, particleQuality = 1.5, adaptiveMobile = true, textureResolutionPx = 4096,
        ),
    ),
    GraphicsPreset.PERSONALIZADO to GraphicsPresetMeta(
        displayName = "Personalizado",
        levelNumber = 13,
        badge = "Configuração Manual",
        description = "Ajuste manualmente resolução, sombras, distância de visão, vegetação, clima e reflexos.",
        config = GraphicsConfig(
            textureResolutionScale = 0.86, drawDistance = 340.0, shadowQuality = 2, lightingQuality = 2,
            reflectionQuality = 2, vegetationDensity = 0.82, objectDensity = 0.85, waterQuality = 2,
            vehicleDetailLevel = 2, terrainDetailStep = 1, weatherEffectsDensity = 1.0, textureTierLabel = "2K",
            saturation = 1.08, contrast = 1.06, exposure = 1.0, sharpness = 0.55,
            resolutionMode = "nativa", resolutionScale = 0.94, antialias = true, anisotropyLevel = 8,
            ambientOcclusion = true, globalIllumLevel = 2, materialQuality = 2, npcDensity = 0.85,
            animationDistance = 165.0, particleQuality = 1.0, adaptiveMobile = true, textureResolutionPx = 2048,
        ),
    ),
)

fun resolveSafeTextureTierPx(requestedPx: Int, adaptiveMobile: Boolean): Int {
    val maxMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
    val cores = Runtime.getRuntime().availableProcessors()
    if (!adaptiveMobile) return requestedPx.coerceIn(512, 4096)
    return when {
        maxMemMb < 192 || cores <= 4 -> requestedPx.coerceAtMost(1024)
        maxMemMb < 384 -> requestedPx.coerceAtMost(2048)
        else -> requestedPx.coerceIn(512, 4096)
    }
}

fun clampGraphicsConfigForDevice(config: GraphicsConfig): GraphicsConfig {
    val reqPx = config.textureResolutionPx ?: when (config.textureTierLabel) {
        "512p" -> 512
        "1K" -> 1024
        "2K" -> 2048
        else -> 4096
    }
    val safePx = resolveSafeTextureTierPx(reqPx, config.adaptiveMobile != false)
    val safeLabel = when {
        safePx <= 512 -> "512p"
        safePx <= 1024 -> "1K"
        safePx <= 2048 -> "2K"
        else -> "4K"
    }
    val safeDraw = if (config.adaptiveMobile == true && safePx <= 1024) {
        minOf(config.drawDistance, 240.0)
    } else config.drawDistance
    return config.copy(
        textureResolutionPx = safePx,
        textureTierLabel = safeLabel,
        drawDistance = safeDraw,
    )
}

fun detectRecommendedGraphicsPreset(): GraphicsPreset {
    val cores = Runtime.getRuntime().availableProcessors()
    val maxMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
    return when {
        cores >= 8 && maxMemMb >= 512 -> GraphicsPreset.ALTO
        cores >= 8 && maxMemMb >= 256 -> GraphicsPreset.EQUILIBRADO
        cores >= 4 && maxMemMb >= 192 -> GraphicsPreset.MEDIO
        else -> GraphicsPreset.BAIXO
    }
}

@Serializable
enum class VehicleBodyStyle {
    COMPACT_OPEN,
    CLASSIC_2WD,
    STANDARD_CAB,
    HIGH_POWER_CAB,
    ARTICULATED_4WD,
    QUAD_TRACK,
    HARVESTER,
    TRUCK,
    LOADER,
    FORESTRY,
    PICKUP,
    MOTO,
    EXCAVATOR,
    BACKHOE,
    FORKLIFT,
    TELEHANDLER,
    SKID_STEER,
    GRADER,
    DOZER,
    SELF_PROPELLED_SPRAYER,
}

@Serializable
enum class WheelLayout {
    STANDARD,
    DUAL_REAR,
    ARTICULATED_DUAL,
    CRAWLER_TRACKS,
}

@Serializable
enum class TractorClass(val label: String) {
    HISTORICO("Antigo / Histórico"),
    PEQUENO("Pequeno"),
    COMPACTO("Compacto"),
    MEDIO("Médio"),
    GRANDE("Grande"),
    ARTICULADO("Articulado"),
    ALTA_POTENCIA("Alta Potência"),
    ESPECIAL("Especial"),
}

@Serializable
enum class ExhaustStyle {
    RIGHT_PILLAR,
    CENTER_HOOD,
    DUAL_STACK,
    HORIZONTAL_LOW,
}

@Serializable
enum class HoodProfile {
    VINTAGE_ROUND,
    CLASSIC_SLOPE,
    MODERN_AERO,
    HEAVY_NOSE,
}

@Serializable
data class VehicleDef(
    val id: String,
    val brand: String,
    val model: String,
    val category: VehicleCategory,
    val era: String,
    val modelYear: Int,
    val horsepower: Double,
    val maxTorqueNm: Double,
    val maxSpeedKmh: Double,
    val fuelCapacityL: Double,
    val weightKg: Double,
    val priceBrl: Double,
    val supportedHitches: List<HitchType>,
    val primaryColorRgb: RgbTriple,
    val accentColorRgb: RgbTriple,
    val bodyStyle: VehicleBodyStyle,
    val wheelLayout: WheelLayout,
    val hasCab: Boolean,
    val dimensionsM: DimTriple,
    val turnRateDegPerSec: Double,
    val soundProfile: String,
    val hasGrainTank: Boolean,
    val tankCapacityL: Double,
    val gpsReady: Boolean,
    val description: String,
    val requiredModPackId: String? = null,
    val subTypeLabel: String = "",
    val series: String? = null,
    val tractorClass: TractorClass? = null,
    val exhaustStyle: ExhaustStyle? = null,
    val hoodProfile: HoodProfile? = null,
    val visualSilhouette: String? = null,
    val headerWidthFt: Double? = null,
    val driveLabel: String? = null,
    val specSourceNote: String? = null,
)

@Serializable
data class WorkshopOption(
    val id: String,
    val name: String,
    val priceBrl: Double,
    val hpMultiplier: Double? = null,
    val fuelUseMultiplier: Double? = null,
    val tractionBonus: Double? = null,
    val compactionPenaltyReduction: Double? = null,
    val weightDeltaKg: Double? = null,
    val illuminationMultiplier: Double? = null,
    val colorRgb: RgbTriple? = null,
    val exhaustStyle: ExhaustStyle? = null,
    val description: String? = null,
)

@Serializable
enum class WeatherType {
    ENSOLARADO,
    PARCIALMENTE_NUBLADO,
    NUBLADO,
    CHUVA_FRACA,
    CHUVA,
    CHUVA_INTENSA,
    NEBLINA,
    TEMPESTADE,
}

data class WeatherMeta(
    val displayName: String,
    val fogDensity: Double,
    val rainIntensity: Double,
    val cloudCover: Double,
)

val WEATHER_META: Map<WeatherType, WeatherMeta> = mapOf(
    WeatherType.ENSOLARADO to WeatherMeta("Dia Ensolarado", 0.0012, 0.0, 0.16),
    WeatherType.PARCIALMENTE_NUBLADO to WeatherMeta("Parcialmente Nublado", 0.0018, 0.0, 0.48),
    WeatherType.NUBLADO to WeatherMeta("Completamente Nublado", 0.0029, 0.0, 0.86),
    WeatherType.CHUVA_FRACA to WeatherMeta("Chuva Fraca", 0.0038, 0.38, 0.84),
    WeatherType.CHUVA to WeatherMeta("Chuva Moderada", 0.0049, 0.75, 0.92),
    WeatherType.CHUVA_INTENSA to WeatherMeta("Chuva Intensa", 0.0061, 0.96, 0.98),
    WeatherType.NEBLINA to WeatherMeta("Neblina", 0.0094, 0.08, 0.65),
    WeatherType.TEMPESTADE to WeatherMeta("Tempestade", 0.0072, 1.0, 1.0),
)

@Serializable
enum class SoilState {
    FLORESTA_NATIVA,
    PEDRAS_MATO,
    AREA_PRESERVACAO,
    ESTRADA_TERRA,
    ESTRADA_ASFALTO,
    AGUA_RIO,
    PONTE,
    PASTAGEM,
    ARADO,
    SUBSOLADO,
    SEMEADO,
    CRESCENDO,
    PRONTO_COLHEITA,
    PALHADA,
}

data class SoilMeta(val displayName: String, val isFarmable: Boolean)

val SOIL_META: Map<SoilState, SoilMeta> = mapOf(
    SoilState.FLORESTA_NATIVA to SoilMeta("Floresta Nativa", false),
    SoilState.PEDRAS_MATO to SoilMeta("Pedras e Mato", false),
    SoilState.AREA_PRESERVACAO to SoilMeta("Área de Preservação", false),
    SoilState.ESTRADA_TERRA to SoilMeta("Estrada de Terra", false),
    SoilState.ESTRADA_ASFALTO to SoilMeta("Estrada Asfaltada", false),
    SoilState.AGUA_RIO to SoilMeta("Rio / Lago", false),
    SoilState.PONTE to SoilMeta("Ponte Rodoviária", false),
    SoilState.PASTAGEM to SoilMeta("Pastagem / Capim", true),
    SoilState.ARADO to SoilMeta("Solo Arado", true),
    SoilState.SUBSOLADO to SoilMeta("Solo Subsolado", true),
    SoilState.SEMEADO to SoilMeta("Semeado", true),
    SoilState.CRESCENDO to SoilMeta("Em Crescimento", true),
    SoilState.PRONTO_COLHEITA to SoilMeta("Pronto p/ Colheita", true),
    SoilState.PALHADA to SoilMeta("Palhada (Plantio Direto)", true),
)

@Serializable
enum class HitchType {
    THREE_POINT,
    DRAWBAR,
    FIFTH_WHEEL,
    FRONT_LOADER,
    HEADER,
}

val HITCH_LABELS: Map<HitchType, String> = mapOf(
    HitchType.THREE_POINT to "3 Pontos",
    HitchType.DRAWBAR to "Barra de Tração",
    HitchType.FIFTH_WHEEL to "Quinta Roda",
    HitchType.FRONT_LOADER to "Carregador Frontal",
    HitchType.HEADER to "Plataforma de Corte",
)

@Serializable
enum class VehicleCategory {
    TRATOR,
    COLHEITADEIRA,
    CAMINHAO,
    CAMINHONETE,
    UTILITARIO_MOTO,
    CARREGADEIRA,
    RETROESCAVADEIRA,
    FLORESTAL,
    ESCAVADEIRA,
    EMPILHADEIRA,
    MANIPULADOR,
    MINICARREGADEIRA,
    MOTONIVELADORA,
    TRATOR_ESTEIRA,
    PULVERIZADOR_AUTOPROPELIDO,
}

val VEHICLE_CATEGORY_LABELS: Map<VehicleCategory, String> = mapOf(
    VehicleCategory.TRATOR to "Trator",
    VehicleCategory.COLHEITADEIRA to "Colheitadeira",
    VehicleCategory.CAMINHAO to "Caminhão",
    VehicleCategory.CAMINHONETE to "Caminhonete",
    VehicleCategory.UTILITARIO_MOTO to "Utilitário / Moto",
    VehicleCategory.CARREGADEIRA to "Pá Carregadeira",
    VehicleCategory.RETROESCAVADEIRA to "Retroescavadeira",
    VehicleCategory.FLORESTAL to "Máquina Florestal",
    VehicleCategory.ESCAVADEIRA to "Escavadeira Hidráulica",
    VehicleCategory.EMPILHADEIRA to "Empilhadeira",
    VehicleCategory.MANIPULADOR to "Manipulador Telescópico",
    VehicleCategory.MINICARREGADEIRA to "Minicarregadeira",
    VehicleCategory.MOTONIVELADORA to "Motoniveladora",
    VehicleCategory.TRATOR_ESTEIRA to "Trator de Esteira",
    VehicleCategory.PULVERIZADOR_AUTOPROPELIDO to "Pulverizador Autopropelido",
)

@Serializable
enum class ImplementCategory {
    ARADO,
    GRADE,
    SUBSOLADOR,
    ESCARIFICADOR,
    CULTIVADOR,
    PLANTADEIRA,
    SEMEADORA,
    PULVERIZADOR,
    DISTRIBUIDOR,
    CARRETA,
    SEGADORA,
    ENLEIRADOR,
    ENFARDADORA,
    ENSILADEIRA,
    ENSACADORA,
    ROCADEIRA,
    PLATAFORMA_MILHO,
}

val IMPLEMENT_CATEGORY_LABELS: Map<ImplementCategory, String> = mapOf(
    ImplementCategory.ARADO to "Arado",
    ImplementCategory.GRADE to "Grade",
    ImplementCategory.SUBSOLADOR to "Subsolador",
    ImplementCategory.ESCARIFICADOR to "Escarificador",
    ImplementCategory.CULTIVADOR to "Cultivador",
    ImplementCategory.PLANTADEIRA to "Plantadeira",
    ImplementCategory.SEMEADORA to "Semeadora",
    ImplementCategory.PULVERIZADOR to "Pulverizador",
    ImplementCategory.DISTRIBUIDOR to "Distribuidor de Fertilizantes",
    ImplementCategory.CARRETA to "Carreta / Reboque",
    ImplementCategory.SEGADORA to "Segadora",
    ImplementCategory.ENLEIRADOR to "Ancinho / Enleirador",
    ImplementCategory.ENFARDADORA to "Enfardadora",
    ImplementCategory.ENSILADEIRA to "Forrageira / Ensiladeira",
    ImplementCategory.ENSACADORA to "Ensacadora de Silagem",
    ImplementCategory.ROCADEIRA to "Roçadeira / Limpeza",
    ImplementCategory.PLATAFORMA_MILHO to "Plataforma de Milho",
)

@Serializable
enum class CropCategory {
    GRAOS_LEGUMINOSAS,
    OLEAGINOSAS,
    INDUSTRIAIS,
    FRUTAS_PERMANENTES,
    HORTALICAS_TUBERCULOS,
    FLORESTAIS,
}

val CROP_CATEGORY_LABELS: Map<CropCategory, String> = mapOf(
    CropCategory.GRAOS_LEGUMINOSAS to "Grãos e Leguminosas",
    CropCategory.OLEAGINOSAS to "Oleaginosas",
    CropCategory.INDUSTRIAIS to "Culturas Industriais",
    CropCategory.FRUTAS_PERMANENTES to "Frutas e Permanentes",
    CropCategory.HORTALICAS_TUBERCULOS to "Hortaliças e Tubérculos",
    CropCategory.FLORESTAIS to "Culturas Florestais",
)

@Serializable
enum class AnimalSpecies {
    BOVINO_CORTE,
    VACA_LEITEIRA,
    SUINO,
    AVE,
    OVINO,
    CAPRINO,
}

data class AnimalMeta(
    val displayName: String,
    val unitPriceBrl: Double,
    val dailyFeedKg: Double,
    val dailyWaterL: Double,
    val productName: String,
    val productUnit: String,
)

val ANIMAL_META: Map<AnimalSpecies, AnimalMeta> = mapOf(
    AnimalSpecies.BOVINO_CORTE to AnimalMeta("Bovinos de Corte", 3800.0, 14.0, 45.0, "Gado Gordo", "arroba"),
    AnimalSpecies.VACA_LEITEIRA to AnimalMeta("Vacas Leiteiras", 5400.0, 18.0, 65.0, "Leite In Natura", "L"),
    AnimalSpecies.SUINO to AnimalMeta("Suínos", 950.0, 3.2, 10.0, "Suíno Terminado", "kg"),
    AnimalSpecies.AVE to AnimalMeta("Aves Poedeiras", 42.0, 0.14, 0.35, "Ovos Caipiras", "dúzia"),
    AnimalSpecies.OVINO to AnimalMeta("Ovinos", 780.0, 2.2, 6.0, "Lã e Cordeiro", "kg"),
    AnimalSpecies.CAPRINO to AnimalMeta("Caprinos", 820.0, 2.4, 6.0, "Leite de Cabra", "L"),
)

@Serializable
enum class BuildingType {
    GALPAO,
    SILO,
    SECADOR,
    OFICINA,
    CASA,
    CURRAL,
    ESTABULO,
    AVIARIO,
    POCILGA,
    CERCA,
    PORTEIRA,
    DEPOSITO,
    FABRICA,
    ESTRADA_INTERNA,
    PATIO,
    ESTUFA_FLORES,
    CASA_URBANA,
    MERCADO_RURAL,
    POSTO_COMBUSTIVEL,
    TANQUE_COMBUSTIVEL,
    ESCRITORIO_FAZENDA,
    LATICINIO_PLANTA,
    FABRICA_RACAO,
    MOINHO_GRAOS,
    GARAGEM,
    CELEIRO,
    JARDIM,
    CAMINHO_TERRA,
    ARVORE_ORNAMENTAL,
    CANTEIRO_FLORES,
    ESTACIONAMENTO,
    BANCO,
    CONCESSIONARIA,
    LOJA_AGROPECUARIA,
    COOPERATIVA,
    ESTACAO_GRAOS,
    MADEIREIRA,
    LOJA_MATERIAIS,
    PONTO_ANIMAIS,
    ARMAZEM,
}

data class BuildingMeta(
    val displayName: String,
    val costBrl: Double,
    val widthM: Double,
    val depthM: Double,
    val heightM: Double,
    val description: String,
)

val BUILDING_META: Map<BuildingType, BuildingMeta> = mapOf(
    BuildingType.GALPAO to BuildingMeta("Galpão de Máquinas", 65000.0, 12.0, 8.0, 5.5, "Abriga tratores e implementos contra desgaste climático."),
    BuildingType.SILO to BuildingMeta("Silo Graneleiro", 95000.0, 6.0, 6.0, 10.0, "Armazena até 250t de grãos com controle de umidade."),
    BuildingType.SECADOR to BuildingMeta("Secador de Grãos", 120000.0, 7.0, 7.0, 9.0, "Reduz umidade de colheita e aumenta valor comercial."),
    BuildingType.OFICINA to BuildingMeta("Oficina Mecânica", 55000.0, 8.0, 6.0, 4.5, "Permite reparo, manutenção e customização de máquinas."),
    BuildingType.CASA to BuildingMeta("Sede / Casa da Fazenda", 80000.0, 9.0, 7.0, 4.2, "Descanso para avançar o dia e administrar a fazenda."),
    BuildingType.CURRAL to BuildingMeta("Curral de Bovinos", 48000.0, 12.0, 10.0, 2.5, "Manejo e engorda de bovinos de corte com cochos."),
    BuildingType.ESTABULO to BuildingMeta("Estábulo Leiteiro", 74000.0, 11.0, 8.0, 4.8, "Ordenha de vacas leiteiras e tanque resfriador."),
    BuildingType.AVIARIO to BuildingMeta("Aviário Automatizado", 42000.0, 10.0, 6.0, 3.5, "Criação de aves e coleta diária de ovos."),
    BuildingType.POCILGA to BuildingMeta("Pocilga Tecnificada", 52000.0, 10.0, 7.0, 3.2, "Manejo de suínos com bebedouros e alimentação."),
    BuildingType.CERCA to BuildingMeta("Cerca de Madeira", 1200.0, 4.0, 0.4, 1.4, "Delimita talhões e pastagens."),
    BuildingType.PORTEIRA to BuildingMeta("Porteira Articulada", 2800.0, 4.0, 0.5, 1.6, "Acesso interativo para veículos e personagem."),
    BuildingType.DEPOSITO to BuildingMeta("Depósito de Feno/Silagem", 38000.0, 10.0, 7.0, 4.5, "Armazena fardos de feno e silagem ensacada."),
    BuildingType.FABRICA to BuildingMeta("Agroindústria Modular", 210000.0, 13.0, 9.0, 6.5, "Processa matérias-primas em produtos de alto valor."),
    BuildingType.ESTRADA_INTERNA to BuildingMeta("Estrada Interna Cascalhada", 1500.0, 4.0, 4.0, 0.1, "Melhora tração e acesso entre talhões."),
    BuildingType.PATIO to BuildingMeta("Pátio de Manobras", 9000.0, 8.0, 8.0, 0.1, "Base compactada para carga, descarga e estacionamento."),
    BuildingType.ESTUFA_FLORES to BuildingMeta("Estufa para Rosas & Hortaliças", 26000.0, 10.0, 6.0, 3.4, "Estrutura protegida para floricultura familiar e cultivo de rosas de corte."),
    BuildingType.CASA_URBANA to BuildingMeta("Residência na Vila", 52000.0, 8.0, 6.0, 3.8, "Moradia urbana construída em lote residencial da pequena cidade rural."),
    BuildingType.MERCADO_RURAL to BuildingMeta("Mercado / Armazém da Vila", 68000.0, 10.0, 7.0, 4.2, "Comércio local para escoamento direto da agricultura familiar e flores."),
    BuildingType.POSTO_COMBUSTIVEL to BuildingMeta("Posto de Diesel & Conveniência", 84000.0, 9.0, 7.0, 4.4, "Ponto de abastecimento de diesel S10 e Arla para frotas próprias e terceirizadas."),
    BuildingType.TANQUE_COMBUSTIVEL to BuildingMeta("Tanque Aéreo Diesel/Água 25.000L", 34000.0, 5.0, 4.0, 3.6, "Reserva própria de diesel S10 ou água para pulverizadores com bico de engate rápido."),
    BuildingType.ESCRITORIO_FAZENDA to BuildingMeta("Escritório Administrativo & Balança", 46000.0, 8.0, 6.0, 3.9, "Central de contratos, DRE, terceirização e controle de faturamento da propriedade."),
    BuildingType.LATICINIO_PLANTA to BuildingMeta("Laticínio Modular", 145000.0, 12.0, 8.0, 5.4, "Beneficia leite in natura em queijo minas, manteiga e iogurte (expansível em 3 estágios)."),
    BuildingType.FABRICA_RACAO to BuildingMeta("Fábrica de Ração Balanceada", 135000.0, 12.0, 9.0, 6.2, "Mistura milho, soja e farelo em ração peletizada de alto rendimento para pecuária."),
    BuildingType.MOINHO_GRAOS to BuildingMeta("Moinho & Esmagadora Industrial", 175000.0, 13.0, 9.0, 7.0, "Complexo industrial para farinha, óleo vegetal refinado e farelo proteico."),
    BuildingType.GARAGEM to BuildingMeta("Garagem Rural Coberta", 28000.0, 7.0, 6.0, 3.8, "Abrigo coberto para caminhonetes, carros utilitários e tratores compactos."),
    BuildingType.CELEIRO to BuildingMeta("Celeiro Tradicional", 58000.0, 11.0, 8.0, 6.0, "Celeiro de dois pavimentos em madeira ou alvenaria para feno, sacaria e implementos."),
    BuildingType.JARDIM to BuildingMeta("Jardim e Paisagismo", 4500.0, 6.0, 5.0, 1.8, "Área ajardinada com canteiros floridos, calçada e pergolado de sombra."),
    BuildingType.CAMINHO_TERRA to BuildingMeta("Caminho Rural / Acesso", 950.0, 4.0, 4.0, 0.08, "Trilha batida e cascalhada para ligar casas, galpões e porteiras."),
    BuildingType.ARVORE_ORNAMENTAL to BuildingMeta("Árvore de Sombra / Ipê", 1800.0, 2.2, 2.2, 6.5, "Árvore brasileira frondosa para sombra no quintal e arborização da sede."),
    BuildingType.CANTEIRO_FLORES to BuildingMeta("Canteiro de Azaleias e Flores", 1400.0, 4.0, 2.5, 0.9, "Canteiro ornamental colorido com arbustos floridos típicos de fazenda brasileira."),
    BuildingType.ESTACIONAMENTO to BuildingMeta("Estacionamento de Máquinas", 11000.0, 10.0, 8.0, 0.15, "Pátio nivelado com batentes para organizar frotas e carretas."),
    BuildingType.BANCO to BuildingMeta("Banco Rural Brasil", 140000.0, 10.0, 8.0, 4.8, "Agência para depósitos, saques, crédito rural e gestão de financiamentos."),
    BuildingType.CONCESSIONARIA to BuildingMeta("Concessionária Agrícola", 180000.0, 14.0, 9.0, 5.6, "Showroom de tratores, colheitadeiras, caminhões e implementos agrícolas."),
    BuildingType.LOJA_AGROPECUARIA to BuildingMeta("Loja Agropecuária & Insumos", 92000.0, 11.0, 8.0, 4.5, "Venda de sementes certificadas, fertilizantes NPK, calcário e defensivos."),
    BuildingType.COOPERATIVA to BuildingMeta("Cooperativa Agroindustrial", 220000.0, 14.0, 9.0, 6.4, "Recebimento de safra com balança rodoviária, moega e assistência técnica."),
    BuildingType.ESTACAO_GRAOS to BuildingMeta("Estação de Compra de Grãos", 165000.0, 12.0, 8.0, 5.2, "Ponto de descarga com balança rodoviária e moega para venda imediata de produção."),
    BuildingType.MADEIREIRA to BuildingMeta("Madeireira Brasil", 110000.0, 12.0, 8.0, 4.6, "Serraria, compra de toras de eucalipto/pinus e fornecimento de madeira para obras."),
    BuildingType.LOJA_MATERIAIS to BuildingMeta("Loja de Materiais de Construção", 98000.0, 11.0, 8.0, 4.4, "Cimento, tijolos, telhas e estruturas para expansão da propriedade."),
    BuildingType.PONTO_ANIMAIS to BuildingMeta("Feira & Ponto de Venda de Animais", 86000.0, 12.0, 9.0, 4.2, "Comércio e embarque de bovinos, leiteiras, suínos, ovinos e aves."),
    BuildingType.ARMAZEM to BuildingMeta("Armazém Geral Regional", 128000.0, 13.0, 9.0, 5.8, "Doca logística para estoque de sacaria, fardos e mercadorias."),
)

@Serializable
enum class FeatureStatus(val label: String) {
    PLANEJADO("Planejado"),
    EM_DESENVOLVIMENTO("Em Desenvolvimento"),
    IMPLEMENTADO("Implementado"),
    TESTADO("Testado"),
    PENDENTE_CORRECAO("Pendente de Correção"),
}

@Serializable
enum class CameraMode {
    TERCEIRA_PESSOA,
    CABINE,
    ORBITAL,
    TOPO,
    LIVRE,
}

val CAMERA_MODE_LABELS: Map<CameraMode, String> = mapOf(
    CameraMode.TERCEIRA_PESSOA to "3ª Pessoa",
    CameraMode.CABINE to "Cabine",
    CameraMode.ORBITAL to "Orbital",
    CameraMode.TOPO to "Vista Aérea",
    CameraMode.LIVRE to "Câmera Livre",
)

@Serializable
enum class MapSizeOption {
    PEQUENO,
    MEDIO,
    GRANDE,
    EXTRA_GRANDE,
    GIGANTE_CONTINENTAL,
}

data class MapSizeMeta(
    val label: String,
    val gridSide: Int,
    val cellSizeM: Int,
    val spanLabel: String,
    val description: String,
)

val MAP_SIZE_META: Map<MapSizeOption, MapSizeMeta> = mapOf(
    MapSizeOption.PEQUENO to MapSizeMeta("Pequeno (64×64)", 64, 4, "256×256m", "Leve para celular, carregamento instantâneo e fazendas familiares."),
    MapSizeOption.MEDIO to MapSizeMeta("Médio (96×96)", 96, 6, "576×576m", "Equilíbrio entre desempenho e distância de transporte."),
    MapSizeOption.GRANDE to MapSizeMeta("Grande (128×128)", 128, 6, "768×768m", "Tamanho clássico completo com 10 talhões, rios, rodovias e agroindústrias."),
    MapSizeOption.EXTRA_GRANDE to MapSizeMeta("Extra Grande (160×160)", 160, 6, "960×960m", "Extensão ampla para grandes máquinas articuladas e logística pesada."),
    MapSizeOption.GIGANTE_CONTINENTAL to MapSizeMeta("Gigante Continental (192×192 • 8 GB)", 192, 8, "1536×1536m", "Super mapa de 1,5 km com grandes talhões, logística regional e suporte ampliado de 8 GB."),
)

@Serializable
enum class CropGrowthStage {
    SEMENTE,
    BROTO,
    CRESCIMENTO_INICIAL,
    DESENVOLVIMENTO,
    MATURACAO,
    PRONTO_COLHEITA,
}

data class CropStageInfo(
    val stage: CropGrowthStage,
    val stageNumber: Int,
    val label: String,
    val heightScale: Double,
)

fun getCropStageInfo(growthProgress: Double): CropStageInfo {
    return when {
        growthProgress >= 0.99 -> CropStageInfo(CropGrowthStage.PRONTO_COLHEITA, 6, "Estágio 6/6 • Pronto p/ Colheita", 1.0)
        growthProgress >= 0.78 -> CropStageInfo(CropGrowthStage.MATURACAO, 5, "Estágio 5/6 • Maturação", 0.88)
        growthProgress >= 0.56 -> CropStageInfo(CropGrowthStage.DESENVOLVIMENTO, 4, "Estágio 4/6 • Desenvolvimento", 0.7)
        growthProgress >= 0.34 -> CropStageInfo(CropGrowthStage.CRESCIMENTO_INICIAL, 3, "Estágio 3/6 • Crescimento Inicial", 0.5)
        growthProgress >= 0.14 -> CropStageInfo(CropGrowthStage.BROTO, 2, "Estágio 2/6 • Broto", 0.3)
        else -> CropStageInfo(CropGrowthStage.SEMENTE, 1, "Estágio 1/6 • Semente", 0.14)
    }
}

@Serializable
enum class ModValidationStatus(val label: String) {
    DISPONIVEL("Disponível"),
    EM_DESENVOLVIMENTO("Em desenvolvimento"),
    AGUARDANDO_ARQUIVOS("Aguardando arquivos"),
    EM_VALIDACAO("Em validação"),
    COMPATIVEL("Compatível"),
    INCOMPATIVEL("Incompatível"),
    INSTALADO("Instalado"),
    ATUALIZACAO_DISPONIVEL("Atualização disponível"),
}

@Serializable
enum class ManualTool {
    MAOS,
    MOTOSSERRA,
    ENXADA,
    CONSTRUTOR_CERCA,
}

val MANUAL_TOOL_LABELS: Map<ManualTool, String> = mapOf(
    ManualTool.MAOS to "Mãos Livres",
    ManualTool.MOTOSSERRA to "Motosserra",
    ManualTool.ENXADA to "Enxada / Limpeza",
    ManualTool.CONSTRUTOR_CERCA to "Construtor de Cerca",
)

@Serializable
enum class DriveType(val label: String) {
    RWD("4x2 Traseira"),
    FWD("4x2 Dianteira"),
    AWD("4x4 Integral"),
    TRACKED("Esteiras"),
}

@Serializable
enum class TireCategory(val label: String) {
    AGRICULTURAL("Agrícola"),
    HIGHWAY("Rodoviário"),
    FORESTRY("Florestal"),
    TRACKS("Esteira de Borracha/Aço"),
}

@Serializable
enum class WorkshopComponent {
    MOTOR,
    TRANSMISSAO,
    SUSPENSAO_PNEUS,
    HIDRAULICA,
    LATARIA,
}

val WORKSHOP_COMPONENT_LABELS: Map<WorkshopComponent, String> = mapOf(
    WorkshopComponent.MOTOR to "Motor",
    WorkshopComponent.TRANSMISSAO to "Transmissão",
    WorkshopComponent.SUSPENSAO_PNEUS to "Suspensão e Pneus",
    WorkshopComponent.HIDRAULICA to "Sistema Hidráulico",
    WorkshopComponent.LATARIA to "Lataria e Estrutura",
)

@Serializable
enum class ConservationTier {
    EXCELENTE,
    DESGASTE_LEVE,
    DESGASTE_MODERADO,
    DESGASTE_ALTO,
    CRITICO,
    IMOBILIZADO,
}

data class ConservationTierMeta(
    val label: String,
    val powerMultiplier: Double,
    val fuelPenaltyMultiplier: Double,
    val brakeMultiplier: Double,
    val stutterChancePerSec: Double,
)

val CONSERVATION_TIER_META: Map<ConservationTier, ConservationTierMeta> = mapOf(
    ConservationTier.EXCELENTE to ConservationTierMeta("Excelente", 1.0, 1.0, 1.0, 0.0),
    ConservationTier.DESGASTE_LEVE to ConservationTierMeta("Desgaste Leve", 0.93, 1.08, 0.94, 0.0),
    ConservationTier.DESGASTE_MODERADO to ConservationTierMeta("Desgaste Moderado", 0.8, 1.22, 0.84, 0.08),
    ConservationTier.DESGASTE_ALTO to ConservationTierMeta("Desgaste Acentuado", 0.63, 1.42, 0.7, 0.24),
    ConservationTier.CRITICO to ConservationTierMeta("Estado Crítico", 0.4, 1.72, 0.52, 0.5),
    ConservationTier.IMOBILIZADO to ConservationTierMeta("Imobilizado", 0.0, 1.0, 0.35, 1.0),
)

fun conservationTierFromPct(conservationPct: Double): ConservationTier = when {
    conservationPct <= 0.5 -> ConservationTier.IMOBILIZADO
    conservationPct < 30 -> ConservationTier.CRITICO
    conservationPct < 50 -> ConservationTier.DESGASTE_ALTO
    conservationPct < 70 -> ConservationTier.DESGASTE_MODERADO
    conservationPct < 90 -> ConservationTier.DESGASTE_LEVE
    else -> ConservationTier.EXCELENTE
}

data class VehiclePhysicsSpec(
    val driveType: DriveType,
    val tireCategory: TireCategory,
    val maxSteerAngleDeg: Double,
    val steerResponseRateDegPerSec: Double,
    val wheelbaseM: Double,
    val trackWidthM: Double,
    val frontWheelRadiusM: Double,
    val rearWheelRadiusM: Double,
    val frontAxleZ: Double,
    val rearAxleZ: Double,
    val rearSteerFactor: Double,
    val isTrackedFront: Boolean,
    val isTrackedRear: Boolean,
    val suspensionStiffness: Double,
    val suspensionDamping: Double,
    val suspensionMaxTravelM: Double,
    val cogHeightM: Double,
)

@Serializable
enum class TransmissionType(
    val label: String,
    val forwardGears: Int,
    val isAutomatic: Boolean,
) {
    MANUAL_6("Manual 6M", 6, false),
    SYNCHRO_12("Sincronizado 12M", 12, false),
    POWERSHIFT_16("Powershift 16M", 16, true),
    CVT_HYDRO("CVT / Hidrostático", 1, true),
    TRUCK_12_MANUAL("Câmbio 12M Rodoviário", 12, false),
    TRUCK_AUTO("Automatizado 12M", 12, true),
    PICKUP_6("6 Marchas", 6, true),
    MOTO_5("5 Marchas", 5, false),
}

data class DrivetrainSpec(
    val transmissionType: TransmissionType,
    val gearRatios: DoubleArray,
    val reverseRatio: Double,
    val finalDriveRatio: Double,
    val drivetrainEfficiency: Double,
    val idleRpm: Double,
    val peakTorqueRpm: Double,
    val ratedRpm: Double,
    val maxRpm: Double,
    val flywheelInertiaKgM2: Double,
    val hasAirBrakes: Boolean,
    val upshiftRpmFraction: Double,
    val downshiftRpmFraction: Double,
)

@Serializable
enum class SteeringControlStyle {
    BOTOES,
    VOLANTE,
}

@Serializable
data class AudioChannelSettings(
    val master: Double = 0.85,
    val engine: Double = 0.85,
    val vehicles: Double = 0.75,
    val implements: Double = 0.8,
    val ambient: Double = 0.7,
    val animals: Double = 0.65,
    val weather: Double = 0.8,
    val music: Double = 0.35,
    val ui: Double = 0.7,
)

@Serializable
data class KeyBindingsMap(
    val forward: String = "KeyW",
    val backward: String = "KeyS",
    val left: String = "KeyA",
    val right: String = "KeyD",
    val sprint: String = "ShiftLeft",
    val enterExit: String = "KeyE",
    val hitch: String = "KeyQ",
    val lowerActivate: String = "KeyV",
    val camera: String = "KeyC",
    val lights: String = "KeyL",
    val map: String = "KeyM",
    val horn: String = "KeyH",
)

@Serializable
enum class SeasonType(val label: String) {
    PRIMAVERA("Primavera"),
    VERAO("Verão"),
    OUTONO("Outono"),
    INVERNO("Inverno"),
}

data class SeasonMeta(
    val label: String,
    val growthMultiplier: Double,
    val rainChanceMultiplier: Double,
    val ambientTintHex: String,
    val tempRangeC: Pair<Int, Int>,
)

val SEASON_META: Map<SeasonType, SeasonMeta> = mapOf(
    SeasonType.PRIMAVERA to SeasonMeta("Primavera (Plantio)", 1.15, 1.2, "#d9f2d9", 18 to 29),
    SeasonType.VERAO to SeasonMeta("Verão (Safra Principal)", 1.25, 1.35, "#fff5d6", 23 to 36),
    SeasonType.OUTONO to SeasonMeta("Outono (Colheita / Safrinha)", 0.95, 0.8, "#fde2b8", 15 to 26),
    SeasonType.INVERNO to SeasonMeta("Inverno (Culturas de Inverno)", 0.72, 0.55, "#dbe9f6", 7 to 21),
)

fun getSeasonFromMonth(month: Int): SeasonType {
    val m = ((month - 1).mod(12)) + 1
    return when {
        m in 9..11 -> SeasonType.PRIMAVERA
        m == 12 || m <= 2 -> SeasonType.VERAO
        m in 3..5 -> SeasonType.OUTONO
        else -> SeasonType.INVERNO
    }
}

@Serializable
data class HudLayoutConfig(
    val preset: String = "PADRAO", // "PADRAO" | "COMPACTO" | "CANHOTO"
    val scale: Double = 1.0,
    val showMinimap: Boolean = true,
    val showTelemetryBar: Boolean = true,
    val showHelperWidget: Boolean = true,
    val showDashGauges: Boolean = true,
    val controlOffsetYDp: Double = 0.0,
)

@Serializable
data class GameSettings(
    val graphicsPreset: GraphicsPreset = GraphicsPreset.EQUILIBRADO,
    val customGraphics: GraphicsConfig = GRAPHICS_PRESET_META[GraphicsPreset.EQUILIBRADO]!!.config,
    val steeringStyle: SteeringControlStyle = SteeringControlStyle.BOTOES,
    val steerSpeedMultiplier: Double = 1.0,
    val controlOpacity: Double = 0.82,
    val hudLayout: HudLayoutConfig? = HudLayoutConfig(),
    val soundEnabled: Boolean = true,
    val gamepadAutoDetect: Boolean = true,
    val audioChannels: AudioChannelSettings = AudioChannelSettings(),
    val keyBindings: KeyBindingsMap = KeyBindingsMap(),
    val cameraSensitivity: Double = 1.0,
    val cameraDistance: Double = 1.0,
    val cameraFov: Double = 60.0,
    val autoOptimizePerformance: Boolean = false,
)

fun getActiveGraphicsConfig(settings: GameSettings): GraphicsConfig =
    if (settings.graphicsPreset == GraphicsPreset.PERSONALIZADO) settings.customGraphics
    else (GRAPHICS_PRESET_META[settings.graphicsPreset] ?: GRAPHICS_PRESET_META[GraphicsPreset.EQUILIBRADO])!!.config

const val GRID_SIZE = 128
const val CELL_SIZE_M = 6
const val WORLD_HALF_M = (GRID_SIZE * CELL_SIZE_M) / 2.0 // 384m -> 768x768m

@Serializable
data class Vec2(val x: Double, val z: Double)

@Serializable
data class TerrainCell(
    val gx: Int,
    val gz: Int,
    val height: Double,
    val plotId: Int,
    val soilState: SoilState,
    val cropId: String? = null,
    val growthProgress: Double = 0.0,
    val moisture: Double = 0.65,
    val fertility: Double = 0.78,
    val compaction: Double = 0.1,
    val weedLevel: Double = 0.0,
    val isHayDried: Boolean = false,
    val isHayTedded: Boolean = false,
    val isWindrowed: Boolean = false,
    val nitrogen: Double? = null,
    val phosphorus: Double? = null,
    val potassium: Double? = null,
    val phLevel: Double? = null,
    val pestLevel: Double? = null,
    val fertilizedStages: Int? = null,
    val lastCropId: String? = null,
    val isIrrigated: Boolean? = null,
    val irrigatedUntilDay: Int? = null,
    val pastureRestDays: Int? = null,
    val erosionLevel: Double? = null,
    val contourProtected: Boolean? = null,
    val waterStress: Double? = null,
    val lastYieldKgPerHa: Double? = null,
)

@Serializable
enum class LandUnit {
    HECTARE,
    ALQUEIRE_PR,
    SQM,
    QUADRA_LOCAL,
    LOTE_URBANO,
}

data class LandUnitMeta(
    val label: String,
    val shortLabel: String,
    val haPerUnit: Double,
    val description: String,
)

val LAND_UNIT_META: Map<LandUnit, LandUnitMeta> = mapOf(
    LandUnit.HECTARE to LandUnitMeta("Hectares (ha)", "ha", 1.0, "Unidade métrica padrão agronômica (1 ha = 10.000 m²)."),
    LandUnit.ALQUEIRE_PR to LandUnitMeta("Alqueire Paulista/Paranaense (2,42 ha)", "alq. PR", 2.42, "Alqueire tradicional no Paraná e São Paulo equivalente a 2,42 hectares (24.200 m²)."),
    LandUnit.SQM to LandUnitMeta("Metros Quadrados (m²)", "m²", 0.0001, "Medida exata para pequenas chácaras, hortas, estufas e lotes urbanos."),
    LandUnit.QUADRA_LOCAL to LandUnitMeta("Quadras / Chácaras", "quadra(s)", 1.74, "Referência regional tradicional para divisão de glebas rurais."),
    LandUnit.LOTE_URBANO to LandUnitMeta("Lotes (360 m² ref.)", "lote(s)", 0.036, "Referência de loteamento em cidades pequenas e vilas rurais."),
)

fun convertHaToUnit(areaHa: Double, unit: LandUnit): Double {
    val meta = LAND_UNIT_META[unit] ?: LAND_UNIT_META[LandUnit.HECTARE]!!
    return areaHa / meta.haPerUnit
}

fun formatLandArea(areaHa: Double, unit: LandUnit = LandUnit.HECTARE): String {
    val meta = LAND_UNIT_META[unit] ?: LAND_UNIT_META[LandUnit.HECTARE]!!
    val value = areaHa / meta.haPerUnit
    if (unit == LandUnit.SQM) {
        return "${value.roundToInt()} m²"
    }
    val decimals = if (value < 10.0) 2 else 1
    val formatted = "%.${decimals}f".format(value).replace('.', ',')
    return "$formatted ${meta.shortLabel}"
}

@Serializable
enum class PlotZoneType(val label: String) {
    RURAL_CULTIVO("Lavoura Agrícola"),
    SITIO_FAMILIAR("Sítio / Agricultura Familiar"),
    PASTAGEM("Pastagem / Pecuária"),
    FLORESTAL("Silvicultura / Floresta"),
    URBANO_RESIDENCIAL("Lote Urbano Residencial"),
    URBANO_COMERCIAL("Lote Urbano Comercial"),
    AGROINDUSTRIAL_VAZIO("Terreno Industrial Vazio"),
    PRESERVACAO("Reserva Legal (APP)"),
}

@Serializable
data class FieldPlot(
    val id: Int,
    val name: String,
    val areaHa: Double,
    val owned: Boolean,
    val leased: Boolean,
    val isPreservation: Boolean,
    val buyPriceBrl: Double,
    val leaseDailyBrl: Double,
    val isCustomCreated: Boolean? = null,
    val zoneType: PlotZoneType? = null,
    val isUrbanLot: Boolean? = null,
    val isIndustrialLot: Boolean? = null,
    val ownerNpcName: String? = null,
    val leaseTerm: String? = null, // DIARIO | MENSAL | SAFRAL | ANUAL
    val leaseStartDay: Int? = null,
    val leaseEndDay: Int? = null,
    val leaseAutoRenew: Boolean? = null,
    val mergedFromPlotIds: List<Int>? = null,
    val neighborPlotIds: List<Int>? = null,
    val fencesRemoved: Boolean? = null,
    val centerX: Double? = null,
    val centerZ: Double? = null,
)

@Serializable
data class RepairRecord(
    val day: Int,
    val serviceType: String,
    val description: String,
    val conservationBeforePct: Double,
    val conservationAfterPct: Double,
    val costBrl: Double,
)

@Serializable
data class OwnedVehicle(
    val uid: String,
    val defId: String,
    val x: Double,
    val z: Double,
    val headingDeg: Double,
    val speedMs: Double,
    val gear: Int,
    val rpm: Double,
    val fuelL: Double,
    val wearPct: Double,
    val conservationPct: Double,
    val engineHealthPct: Double,
    val transmissionHealthPct: Double,
    val suspensionTiresHealthPct: Double,
    val hydraulicsHealthPct: Double,
    val bodyHealthPct: Double,
    val dirtPct: Double,
    val steerAngleDeg: Double,
    val wheelSpinDeg: Double,
    val wheelSlip: Double,
    val suspensionOffsetY: Double,
    val suspensionVelY: Double,
    val prevTerrainY: Double,
    val hasTerrainRef: Boolean,
    val pitchDeg: Double,
    val rollDeg: Double,
    val groundSinkageM: Double,
    val collisionCooldownSec: Double,
    val lastImpactSeverity: Double,
    val stallTimerSec: Double,
    val rotorSpinDeg: Double,
    val unloadingAugerProgress: Double,
    val preventiveBonusHours: Double,
    val repairHistory: List<RepairRecord> = emptyList(),
    val headlightsOn: Boolean = false,
    val attachedImplementUid: String? = null,
    val selectedCropId: String = "soja",
    val wheelOptionId: String = "std",
    val engineUpgradeId: String = "stock",
    val customPaintRgb: List<Double>? = null,
    val engineModelId: String? = null,
    val turboId: String? = null,
    val rimColorId: String? = null,
    val cabStyleId: String? = null,
    val headlightStyleId: String? = null,
    val exhaustStyleId: String? = null,
    val accessoryId: String? = null,
    val gpsPointA: Vec2? = null,
    val gpsPointB: Vec2? = null,
    val gpsAutoSteer: Boolean = false,
    val gpsSectionControl: Boolean = true,
    val grainTankFillL: Double = 0.0,
    val grainCropId: String? = null,
    val hoursWorked: Double = 0.0,
    val fieldCreationMode: Boolean = false,
    val blinkerMode: String? = "off", // off | left | right | hazard
    val workLightsOn: Boolean? = null,
    val interiorLightOn: Boolean? = null,
    val wipersOn: Boolean? = null,
    val wiperAngleRad: Double? = null,
    val doorsOpen: Boolean? = null,
    val cabSeatOffset: Double? = null,
    val engineTempC: Double? = null,
    val frontBallastKg: Double? = null,
    val seedingRowSpacingCm: Double? = null,
    val seedingDensityPct: Double? = null,
    val seedingDepthCm: Double? = null,
    val assignedEmployeeUid: String? = null,
    val engineRunning: Boolean? = null,
    val isBraking: Boolean? = null,
    val fuelSpentTotalL: Double? = null,
    val lastFuelBurnLh: Double? = null,
    val surfaceType: String? = null, // terra | cascalho | asfalto | lama
    val is4wdEngaged: Boolean? = null,
    val headerLowered: Boolean? = null,
    val groundPlaneY: Double? = null,
    val wheelOffsetsY: List<Double>? = null,
    val wheelContactCount: Int? = null,
    val isBogged: Boolean? = null,
    val bogSeverity: Double? = null,
    val towCableTargetUid: String? = null,
    val isSprayerActive: Boolean? = null,
    val sprayerFillL: Double? = null,
    val sprayerBoomDeploy: Double? = null,
    val oilFilterHealthPct: Double? = null,
    val beltsHealthPct: Double? = null,
    val tiresWearPct: Double? = null,
    val hydraulicHosesHealthPct: Double? = null,
    val cutterDiscsHealthPct: Double? = null,
    val lastBreakdownNote: String? = null,
    val coolingTicksRemainingSec: Double? = null,
    val handbrakeOn: Boolean? = false,
    val isPtoEngaged: Boolean? = false,
    val ptoRpm: Double? = 0.0,
    val engineLoadPct: Double? = 0.0,
    val cabPitchDeg: Double? = 0.0,
    val cabRollDeg: Double? = 0.0,
    val cabBounceOffsetY: Double? = 0.0,
    val transmissionMode: String? = null,
    val airBrakePressurePct: Double? = 100.0,
    val articulatedAngleDeg: Double? = 0.0,
    val cruiseControlActive: Boolean? = false,
    val cruiseTargetKmh: Double? = null,
)

@Serializable
data class OwnedImplement(
    val uid: String,
    val defId: String,
    val x: Double,
    val z: Double,
    val headingDeg: Double,
    val drawbarYawDeg: Double = 0.0,
    val wheelSpinDeg: Double = 0.0,
    val rotorSpinDeg: Double = 0.0,
    val boomDeployProgress: Double = 0.0,
    val liftProgress: Double = 1.0,
    val groundSinkageM: Double = 0.0,
    val dirtPct: Double = 0.0,
    val isLowered: Boolean = false,
    val isActive: Boolean = false,
    val fillL: Double = 0.0,
    val fillProductId: String? = null,
    val silageBagKgConfig: Int = 40,
    val wearPct: Double? = 0.0,
    val pitchDeg: Double? = 0.0,
    val rollDeg: Double? = 0.0,
    val groundPlaneY: Double? = 0.0,
    val discSpinDeg: Double? = 0.0,
    val augerSpinDeg: Double? = 0.0,
    val furrowDepthM: Double? = 0.0,
    val compatibilityWarning: String? = null,
)

@Serializable
enum class WorldPropType {
    STONE,
    BUSH,
    TREE,
    TREE_PINE,
    TREE_EUCALYPTUS,
    TREE_FRUIT,
    TREE_NATIVE,
    PLANT_CLUSTER,
    ROSE_CLUSTER,
    UTILITY_POLE,
    LOG,
    BALE_ROUND,
    BALE_RECT,
    SILAGE_BULK,
    SILAGE_BAG,
    TREE_ARAUCARIA,
    CACTUS_MANDACARU,
    TREE_CERRADO,
    TREE_PALM_AMAZON,
    COFFEE_SHRUB,
    TREE_IPE_AMARELO,
    TREE_IPE_ROXO,
    TREE_SHADE_MANGUEIRA,
    BANANA_CLUMP,
    FLOWER_BED_AZALEA,
    ORNAMENTAL_HEDGE,
}

@Serializable
data class WorldProp(
    val uid: String,
    val type: WorldPropType,
    val x: Double,
    val z: Double,
    val amountKg: Double,
    val productId: String,
)

@Serializable
data class PlacedBuilding(
    val uid: String,
    val type: BuildingType,
    val x: Double,
    val z: Double,
    val rotationDeg: Double,
    val isGateOpen: Boolean = false,
    val stage: Int? = null,
    val linkedIndustryRecipeId: String? = null,
    val purchasedEquipmentIds: List<String>? = null,
    val plotId: Int? = null,
    val modelId: String? = null,
    val archStyle: String? = null,
    val materialStyle: String? = null,
    val wallColorHex: String? = null,
    val roofColorHex: String? = null,
    val trimColorHex: String? = null,
    val hasVeranda: Boolean? = null,
    val hasGarage: Boolean? = null,
    val customWidthM: Double? = null,
    val customDepthM: Double? = null,
    val customHeightM: Double? = null,
    val customLabel: String? = null,
)

@Serializable
data class LivestockHerd(
    val species: AnimalSpecies,
    val count: Int,
    val avgAgeDays: Int,
    val healthPct: Double,
    val feedReserveKg: Double,
    val waterReserveL: Double,
    val pendingProductionUnits: Double,
)

@Serializable
data class OwnedIndustry(
    val recipeId: String,
    val purchased: Boolean,
    val isProcessing: Boolean,
    val rawStockKg: Double,
    val progressHours: Double,
    val outputStockKg: Map<String, Double> = emptyMap(),
    val employees: Int = 0,
    val stage: Int? = null,
    val purchasedEquipmentIds: List<String>? = null,
    val linkedBuildingUid: String? = null,
    val lotPlotId: Int? = null,
    val efficiencyMultiplier: Double? = null,
)

@Serializable
enum class ContractType(val label: String) {
    PLANTIO("Plantio"),
    COLHEITA("Colheita"),
    TRANSPORTE("Transporte"),
    SILAGEM("Silagem"),
    FENACAO("Fenação"),
    PECUARIA("Manejo Animal"),
}

@Serializable
data class FarmContract(
    val uid: String,
    val title: String,
    val clientName: String,
    val type: ContractType,
    val cropOrProductId: String,
    val targetUnits: Double,
    val completedUnits: Double,
    val rewardBrl: Double,
    val accepted: Boolean,
    val completed: Boolean,
    val providesLoanerMachine: Boolean? = null,
    val loanerVehicleDefId: String? = null,
    val loanerImplementDefId: String? = null,
    val plotId: Int? = null,
)

@Serializable
data class LedgerEntry(
    val day: Int,
    val description: String,
    val amountBrl: Double,
)

@Serializable
data class WeighbridgeTicket(
    val establishmentName: String,
    val productName: String,
    val grossKg: Double,
    val tareKg: Double,
    val netKg: Double,
    val pricePerKgBrl: Double,
    val totalBrl: Double,
    val day: Int,
)

@Serializable
data class TireTrack(
    val x: Double,
    val z: Double,
    val headingDeg: Double,
    val age: Double,
    val trackWidthM: Double,
    val tireWidthM: Double,
    val isMud: Boolean,
    val isSlip: Boolean,
    val soilR: Double,
    val soilG: Double,
    val soilB: Double,
)

@Serializable
data class PlayerAppearanceConfig(
    val hatStyle: String = "sem", // palha | bone | vaqueiro | sem
    val hatColorHex: String = "#d8b467",
    val shirtColorHex: String = "#1e2124",
    val pantsColorHex: String = "#363d2a",
    val bootColorHex: String = "#5c4332",
    val skinColorHex: String = "#e4b898",
    val hairColorHex: String = "#6b2d24",
    val presetId: String? = "preset_1",
    val outfitStyle: String? = "jacket_shearling", // jacket_shearling | overcoat | flannel_plaid | hoodie
    val hairStyle: String? = "bob", // bob | short | short_crop | shoulder_bangs
    val glassesStyle: String? = "sunglasses", // none | sunglasses | reading
    val beardStyle: String? = "none", // none | full_gray
    val glovesHex: String? = null,
    val collarHex: String? = "#ddd5c5",
)

data class CharacterPresetOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val config: PlayerAppearanceConfig,
)

val CHARACTER_PRESETS: List<CharacterPresetOption> = listOf(
    CharacterPresetOption(
        id = "preset_1",
        title = "Opç. 1 • Clara",
        subtitle = "Jaqueta preta gola clara, óculos escuros e calça oliva",
        config = PlayerAppearanceConfig(
            hatStyle = "sem",
            hatColorHex = "#3a2a20",
            shirtColorHex = "#1e2124",
            pantsColorHex = "#363d2a",
            bootColorHex = "#5c4332",
            skinColorHex = "#e4b898",
            hairColorHex = "#6b2d24",
            presetId = "preset_1",
            outfitStyle = "jacket_shearling",
            hairStyle = "bob",
            glassesStyle = "sunglasses",
            beardStyle = "none",
            glovesHex = null,
            collarHex = "#ddd5c5",
        ),
    ),
    CharacterPresetOption(
        id = "preset_2",
        title = "Opç. 2 • Sr. Sebastião",
        subtitle = "Sobretudo cinza claro, barba grisalha e óculos",
        config = PlayerAppearanceConfig(
            hatStyle = "sem",
            hatColorHex = "#8a8f96",
            shirtColorHex = "#a5aab0",
            pantsColorHex = "#24272c",
            bootColorHex = "#5a4030",
            skinColorHex = "#8d583c",
            hairColorHex = "#adb3ba",
            presetId = "preset_2",
            outfitStyle = "overcoat",
            hairStyle = "short",
            glassesStyle = "reading",
            beardStyle = "full_gray",
            glovesHex = null,
            collarHex = "#353944",
        ),
    ),
    CharacterPresetOption(
        id = "preset_3",
        title = "Opç. 3 • Lucas",
        subtitle = "Jaqueta xadrez vermelha, calça cáqui e botas pretas",
        config = PlayerAppearanceConfig(
            hatStyle = "sem",
            hatColorHex = "#c9b28a",
            shirtColorHex = "#7e2521",
            pantsColorHex = "#a89166",
            bootColorHex = "#151619",
            skinColorHex = "#d8a07a",
            hairColorHex = "#bec4ca",
            presetId = "preset_3",
            outfitStyle = "flannel_plaid",
            hairStyle = "short_crop",
            glassesStyle = "none",
            beardStyle = "none",
            glovesHex = null,
            collarHex = "#d1ba90",
        ),
    ),
    CharacterPresetOption(
        id = "preset_4",
        title = "Opç. 4 • Mariana",
        subtitle = "Moletom rosê com capuz, luvas brancas e calça clara",
        config = PlayerAppearanceConfig(
            hatStyle = "sem",
            hatColorHex = "#99606f",
            shirtColorHex = "#99606f",
            pantsColorHex = "#b8aba2",
            bootColorHex = "#d8a8b2",
            skinColorHex = "#d5a27e",
            hairColorHex = "#1e1e24",
            presetId = "preset_4",
            outfitStyle = "hoodie",
            hairStyle = "shoulder_bangs",
            glassesStyle = "none",
            beardStyle = "none",
            glovesHex = "#e8e8e4",
            collarHex = "#854e5d",
        ),
    ),
)

val DEFAULT_PLAYER_APPEARANCE = CHARACTER_PRESETS.first().config

@Serializable
data class PlayerCharacterState(
    val x: Double,
    val z: Double,
    val headingDeg: Double,
    val tool: ManualTool = ManualTool.MAOS,
    val carryingPropUid: String? = null,
    val appearance: PlayerAppearanceConfig? = DEFAULT_PLAYER_APPEARANCE,
    val speedMs: Double? = 0.0,
    val gaitPhase: Double? = 0.0,
    val actionState: String? = "idle", // idle | walk | run | carry | interact | work
)

@Serializable
data class NpcAgentState(
    val uid: String,
    val name: String,
    val role: String,
    val x: Double,
    val z: Double,
    val headingDeg: Double,
    val speedMs: Double,
    val gaitPhase: Double,
    val targetX: Double,
    val targetZ: Double,
    val homeX: Double,
    val homeZ: Double,
    val activity: String,
    val surfaceType: String,
    val dialogLine: String,
    val companionNpcUid: String? = null,
    val ridingVehicleUid: String? = null,
    val skinHex: String,
    val shirtHex: String,
    val pantsHex: String,
    val hatHex: String? = null,
    val state: String, // idle | walking | working | conversing | riding
    val pauseTimerSec: Double? = null,
)

data class OutsourcingServiceMeta(
    val label: String,
    val baseCostPerHaBrl: Double,
    val inputsCostPerHaBrl: Double,
    val speedHaPerHour: Double,
)

val OUTSOURCING_SERVICE_META: Map<String, OutsourcingServiceMeta> = mapOf(
    "PREPARO_SOLO" to OutsourcingServiceMeta("Preparo Completo de Solo", 340.0, 0.0, 2.2),
    "ARACAO" to OutsourcingServiceMeta("Aração Profunda", 290.0, 0.0, 2.4),
    "GRADAGEM" to OutsourcingServiceMeta("Gradagem Niveladora", 230.0, 0.0, 3.1),
    "SUBSOLAGEM" to OutsourcingServiceMeta("Subsolagem / Escarificação", 380.0, 0.0, 1.9),
    "PLANTIO" to OutsourcingServiceMeta("Plantio Direto / Convencional", 320.0, 520.0, 2.6),
    "SEMEADURA" to OutsourcingServiceMeta("Semeadura de Inverno / Pasto", 280.0, 410.0, 2.9),
    "PULVERIZACAO" to OutsourcingServiceMeta("Pulverização de Defensivos", 145.0, 290.0, 5.5),
    "ADUBACAO" to OutsourcingServiceMeta("Adubação de Cobertura / NPK", 165.0, 480.0, 4.5),
    "CALAGEM" to OutsourcingServiceMeta("Aplicação de Calcário", 195.0, 360.0, 4.2),
    "COLHEITA" to OutsourcingServiceMeta("Colheita Mecanizada", 540.0, 0.0, 2.3),
    "TRANSPORTE_GRAOS" to OutsourcingServiceMeta("Transporte de Grãos / Carga", 210.0, 0.0, 4.0),
    "CARREGAMENTO" to OutsourcingServiceMeta("Carregamento & Movimentação", 185.0, 0.0, 3.6),
    "SILAGEM" to OutsourcingServiceMeta("Corte e Ensilagem", 620.0, 140.0, 1.8),
    "FENACAO" to OutsourcingServiceMeta("Corte e Enfardamento de Feno", 430.0, 90.0, 2.5),
    "TERRACO_CURVA" to OutsourcingServiceMeta("Curvas de Nível / Terraceamento", 480.0, 0.0, 1.7),
    "LIMPEZA_CAMPO" to OutsourcingServiceMeta("Roçada e Limpeza de Gleba", 260.0, 0.0, 2.8),
    "DESMATAMENTO_LEGAL" to OutsourcingServiceMeta("Abertura Autorizada de Área", 690.0, 0.0, 1.4),
    "TRANSPORTE_MAQUINAS" to OutsourcingServiceMeta("Prancha / Transporte de Máquinas", 310.0, 0.0, 4.5),
    "TRANSPORTE_ANIMAIS" to OutsourcingServiceMeta("Frete Boiadeiro de Animais", 290.0, 0.0, 4.2),
    "MANEJO_FLORESTAL" to OutsourcingServiceMeta("Extração e Manejo Florestal", 580.0, 0.0, 1.6),
)

@Serializable
data class AgroServiceCompany(
    val id: String,
    val name: String,
    val badgeColorHex: String,
    val regionFocus: String,
    val hqX: Double,
    val hqZ: Double,
    val specialties: List<String>,
    val fleetVehicleDefIds: List<String>,
    val fleetImplementDefIds: List<String>,
    val priceMultiplier: Double,
    val reputation: Double,
    val reliabilityPct: Double,
    val availableMachinesCount: Int,
    val totalJobsCompleted: Int,
    val description: String,
)

@Serializable
data class OutsourcingContract(
    val uid: String,
    val companyId: String,
    val companyName: String,
    val serviceType: String,
    val modality: String, // A_MAQUINA_OPERADOR | B_COMPLETO_INSUMOS | C_SO_OPERADOR
    val plotId: Int,
    val cropId: String,
    val targetAreaHa: Double,
    val progressPct: Double,
    val processedCellsCount: Int,
    val totalTargetCells: Int,
    val quotedPriceBrl: Double,
    val paidBrl: Double,
    val status: String,
    val incident: String,
    val incidentNote: String? = null,
    val assignedVehicleDefId: String,
    val assignedImplementDefId: String? = null,
    val playerVehicleUid: String? = null,
    val operatorName: String,
    val vehicleX: Double,
    val vehicleZ: Double,
    val vehicleHeadingDeg: Double,
    val vehiclePitchDeg: Double? = null,
    val vehicleRollDeg: Double? = null,
    val createdDay: Int,
    val startedHour: Double,
    val completedDay: Int? = null,
    val playerRating: Int? = null,
    val harvestedKgStored: Double? = null,
)

@Serializable
data class PurchaseHistoryEntry(
    val id: String,
    val day: Int,
    val kind: String, // COMPRA | VENDA | ARRENDAMENTO | TERCEIRIZACAO
    val itemName: String,
    val amountBrl: Double,
    val details: String,
)

@Serializable
data class FarmEmployee(
    val uid: String,
    val name: String,
    val role: String, // 'Operador de Máquinas' | 'Agrônomo / Pulverização' | 'Logística & Caminhões' | 'Pecuária'
    val dailyWageBrl: Double,
    val skillEfficiency: Double,
    val assignedVehicleUid: String? = null,
    val assignedPlotId: Int? = null,
    val taskType: String = "IDLE", // IDLE | ARAR | PLANTAR | PULVERIZAR | COLHER | IRRIGATION
    val hiredDay: Int,
    val totalHoursWorked: Double = 0.0,
)

@Serializable
data class UsedAuctionLot(
    val uid: String,
    val defId: String,
    val hoursUsed: Int,
    val conservationPct: Double,
    val baseValueBrl: Double,
    val currentBidBrl: Double,
    val highestBidder: String, // 'Ninguém' | 'Você' | 'Fazendeiro Vizinho'
    val endsAtDay: Int,
    val settled: Boolean,
)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val rewardBrl: Double,
)

val ACHIEVEMENT_CATALOG: List<AchievementDef> = listOf(
    AchievementDef("ach_first_harvest", "Primeira Colheita", "Colha sua primeira safra no campo.", "Produção", 5000.0),
    AchievementDef("ach_soil_master", "Mestre do Solo N-P-K", "Realize calagem ou adubação em talhão próprio.", "Agronomia", 4500.0),
    AchievementDef("ach_rotation_pro", "Rotação Inteligente", "Plante uma cultura diferente da safra anterior no talhão.", "Agronomia", 6000.0),
    AchievementDef("ach_fleet_4", "Frota em Expansão", "Possua 5 ou mais máquinas próprias na fazenda.", "Frota", 8000.0),
    AchievementDef("ach_millionaire", "Agro Milionário", "Alcance R$ 1.000.000,00 em caixa disponível.", "Finanças", 25000.0),
    AchievementDef("ach_debt_free", "Sem Dívidas Bancárias", "Quite um empréstimo bancário por completo.", "Finanças", 7500.0),
    AchievementDef("ach_team_leader", "Gestor de Equipe", "Contrate e coloque um funcionário para operar um talhão.", "Frota", 4000.0),
    AchievementDef("ach_livestock_50", "Rebanho Produtivo", "Mantenha 40+ animais saudáveis na propriedade.", "Pecuária", 6500.0),
)

@Serializable
data class RoadVehicle(
    val uid: String,
    val x: Double,
    val z: Double,
    val headingDeg: Double,
    val speedMs: Double,
    val colorHex: String,
    val kind: String, // truck | pickup
)

@Serializable
data class Puddle(
    val x: Double,
    val z: Double,
    val radius: Double,
)

@Serializable
data class WorldState(
    val slotIndex: Int,
    val schemaVersion: Int = 4,
    val farmName: String,
    val mapId: String,
    val mapGridSize: Int? = 128,
    val startingMode: String,
    val day: Int,
    val month: Int,
    val hourOfDay: Double,
    val weather: WeatherType,
    val moneyBrl: Double,
    val bankLoanBrl: Double,
    val bankBalanceBrl: Double = 0.0,
    val player: PlayerCharacterState,
    val activeVehicleUid: String? = null,
    val plots: List<FieldPlot>,
    val cells: List<TerrainCell>,
    val vehicles: List<OwnedVehicle>,
    val implements: List<OwnedImplement>,
    val props: List<WorldProp>,
    val buildings: List<PlacedBuilding>,
    val herds: List<LivestockHerd>,
    val industries: List<OwnedIndustry>,
    val inventoryKg: Map<String, Double>,
    val marketMultiplier: Map<String, Double>,
    val contracts: List<FarmContract>,
    val ledger: List<LedgerEntry>,
    val lastWeighbridgeTicket: WeighbridgeTicket? = null,
    val tireTracks: List<TireTrack> = emptyList(),
    val statusAlert: String? = null,
    val employees: List<FarmEmployee>? = emptyList(),
    val auctionLots: List<UsedAuctionLot>? = emptyList(),
    val achievementsUnlocked: List<String>? = emptyList(),
    val inputsStockKg: Map<String, Double>? = emptyMap(),
    val landUnit: LandUnit? = LandUnit.HECTARE,
    val npcAgents: List<NpcAgentState>? = emptyList(),
    val outsourcingCompanies: List<AgroServiceCompany>? = emptyList(),
    val outsourcingContracts: List<OutsourcingContract>? = emptyList(),
    val purchaseHistory: List<PurchaseHistoryEntry>? = emptyList(),
    val roadVehicles: List<RoadVehicle>? = emptyList(),
    val puddles: List<Puddle>? = emptyList(),
    val cloudRevision: Int? = 0,
)

@Serializable
enum class ModDownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    ERROR,
}

@Serializable
data class ModDownloadProgress(
    val packId: String,
    val status: ModDownloadStatus,
    val downloadedMb: Double,
    val totalMb: Double,
    val speedMbPerSec: Double,
    val progressPct: Int,
    val errorMessage: String? = null,
)

@Serializable
data class StorageQuotaConfig(
    val mobileLimitMb: Double = 8192.0,
    val enforceMobileLimit: Boolean = true,
    val allowUnlimitedOnDesktop: Boolean = true,
)

@Serializable
data class FarmAccountUser(
    val uid: String,
    val email: String = "",
    val displayName: String,
    val emailVerified: Boolean = false,
    val avatarStyle: String = "palha",
    val photoUrl: String? = null,
    val personalCode: String = "",
    val warningAcknowledged: Boolean = false,
    val accountSource: String = "FARM_BRASIL_LOCAL",
    val createdAtIso: String = "",
)

@Serializable
data class AccountSyncStatus(
    val isServerConfigured: Boolean = false,
    val serverEndpointUrl: String? = null,
    val lastLocalSaveIso: String? = null,
    val lastRemoteSyncIso: String? = null,
    val devicePlatform: String = "ANDROID_MOBILE",
    val statusMessage: String = "Dados salvos localmente neste dispositivo. Servidor próprio de sincronização ainda não conectado.",
)

@Serializable
data class CloudSaveSummary(
    val slotIndex: Int,
    val farmName: String,
    val mapId: String,
    val day: Int,
    val moneyBrl: Double,
    val revision: Int,
    val deviceId: String,
    val deviceName: String,
    val checksumSha256: String,
    val requiredModIds: List<String>,
    val payloadSizeBytes: Long,
    val updatedAtIso: String,
)

data class CloudSaveConflictState(
    val slotIndex: Int,
    val localFarmName: String,
    val localDay: Int,
    val localRevision: Int,
    val localMoneyBrl: Double,
    val cloud: CloudSaveSummary,
)

@Serializable
sealed interface AppRoute {
    @Serializable data object MainMenu : AppRoute
    @Serializable data object NewCareerSetup : AppRoute
    @Serializable data object Gameplay : AppRoute
    @Serializable data object MapSelection : AppRoute
    @Serializable data object Dealership : AppRoute
    @Serializable data object ImplementShop : AppRoute
    @Serializable data object UsedMarket : AppRoute
    @Serializable data object Garage : AppRoute
    @Serializable data object Workshop : AppRoute
    @Serializable data object Hub : AppRoute
    @Serializable data object Multiplayer : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object ContentManager : AppRoute
    @Serializable data object PerformanceMonitor : AppRoute
    @Serializable data object StorageManager : AppRoute
    @Serializable data object Credits : AppRoute
}
