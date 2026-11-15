package com.example.domain

import kotlin.math.max
import kotlin.math.roundToInt

object BrandColors {
    val AGRALE_GREEN = rgb(0.11, 0.48, 0.22)
    val AGRALE_CREAM = rgb(0.88, 0.86, 0.76)
    val AGRALE_RED = rgb(0.76, 0.16, 0.14)
    val VALMET_YELLOW = rgb(0.90, 0.65, 0.08)
    val VALMET_ORANGE = rgb(0.88, 0.44, 0.09)
    val VALTRA_GOLD = rgb(0.88, 0.62, 0.08)
    val VALTRA_RED = rgb(0.76, 0.12, 0.14)
    val VALTRA_BLUE = rgb(0.10, 0.36, 0.70)
    val VALTRA_BLACK = rgb(0.16, 0.17, 0.18)
    val CBT_YELLOW = rgb(0.92, 0.68, 0.10)
    val CBT_CHASSIS = rgb(0.20, 0.20, 0.22)
    val JD_GREEN = rgb(0.12, 0.50, 0.17)
    val JD_YELLOW = rgb(0.96, 0.80, 0.08)
    val MF_RED = rgb(0.80, 0.11, 0.13)
    val MF_GRAY = rgb(0.56, 0.58, 0.60)
    val NH_BLUE = rgb(0.08, 0.37, 0.80)
    val NH_WHITE = rgb(0.92, 0.92, 0.92)
    val CASE_RED = rgb(0.78, 0.09, 0.11)
    val CASE_BLACK = rgb(0.14, 0.14, 0.16)
    val FENDT_GREEN = rgb(0.22, 0.52, 0.18)
    val FENDT_GRAY = rgb(0.36, 0.38, 0.40)
    val DEUTZ_GREEN = rgb(0.32, 0.66, 0.16)
    val CLAAS_GREEN = rgb(0.46, 0.68, 0.18)
    val KUBOTA_ORANGE = rgb(0.92, 0.36, 0.08)
    val LS_BLUE = rgb(0.12, 0.34, 0.72)
    val YANMAR_RED = rgb(0.82, 0.16, 0.16)
    val FORD_BLUE = rgb(0.10, 0.34, 0.68)
    val FORD_GRAY = rgb(0.84, 0.83, 0.78)
    val FIAT_TERRACOTTA = rgb(0.83, 0.27, 0.11)
    val FIAT_WHITE = rgb(0.92, 0.91, 0.88)
    val IH_RED = rgb(0.76, 0.14, 0.12)
    val IH_WHITE = rgb(0.94, 0.93, 0.90)
    val URSUS_OCHRE = rgb(0.84, 0.54, 0.14)
    val SAME_RED = rgb(0.79, 0.15, 0.12)
    val LANDINI_BLUE = rgb(0.12, 0.44, 0.74)
    val STEYR_RED = rgb(0.78, 0.14, 0.14)
    val JCB_YELLOW = rgb(0.93, 0.69, 0.06)
}

internal fun tr(
    id: String,
    brand: String,
    model: String,
    series: String,
    tractorClass: TractorClass,
    modelYear: Int,
    hp: Double,
    maxSpeedKmh: Double,
    weightKg: Double,
    priceBrl: Double,
    bodyStyle: VehicleBodyStyle,
    wheelLayout: WheelLayout,
    hasCab: Boolean,
    primaryColorRgb: RgbTriple,
    accentColorRgb: RgbTriple,
    hoodProfile: HoodProfile,
    exhaustStyle: ExhaustStyle,
    soundProfile: String,
    description: String,
    requiredModPackId: String? = null,
    subTypeLabel: String = "",
): VehicleDef {
    val era = when {
        modelYear < 1995 -> "Clássico"
        modelYear < 2016 -> "Intermediário"
        else -> "Moderno"
    }
    val torqueNm = (hp * if (hp >= 350) 4.45 else if (hp >= 180) 4.15 else 3.9).roundToInt().toDouble()
    val fuelCapacityL = max(32.0, hp * if (hp >= 400) 2.45 else if (hp >= 180) 1.95 else 1.45).roundToInt().toDouble()

    val dims: DimTriple
    val turnRate: Double
    when (bodyStyle) {
        VehicleBodyStyle.QUAD_TRACK -> {
            dims = dim(7.15, 3.12, 3.78)
            turnRate = 33.0
        }
        VehicleBodyStyle.ARTICULATED_4WD -> {
            dims = dim(6.75, 3.24, 3.62)
            turnRate = 36.0
        }
        VehicleBodyStyle.HIGH_POWER_CAB -> {
            val dual = wheelLayout == WheelLayout.DUAL_REAR
            dims = dim(if (hp >= 350) 5.65 else 5.08, if (dual) 2.84 else 2.52, if (hp >= 350) 3.34 else 3.12)
            turnRate = if (hp >= 350) 43.0 else 47.0
        }
        VehicleBodyStyle.STANDARD_CAB -> {
            dims = dim(if (hp >= 120) 4.42 else 3.92, if (hp >= 120) 2.24 else 1.96, if (hp >= 120) 2.84 else 2.58)
            turnRate = if (hp >= 120) 50.0 else 55.0
        }
        VehicleBodyStyle.CLASSIC_2WD -> {
            dims = dim(if (hp >= 100) 4.18 else 3.68, if (hp >= 100) 2.12 else 1.88, if (hp >= 100) 2.34 else 2.08)
            turnRate = if (hp >= 100) 49.0 else 55.0
        }
        else -> {
            dims = dim(if (tractorClass == TractorClass.COMPACTO) 3.32 else 2.95, 1.56, 1.92)
            turnRate = 63.0
        }
    }

    val supportedHitches = when {
        bodyStyle == VehicleBodyStyle.QUAD_TRACK || (bodyStyle == VehicleBodyStyle.ARTICULATED_4WD && hp >= 500) ->
            listOf(HitchType.DRAWBAR, HitchType.THREE_POINT)
        hp <= 40 ->
            listOf(HitchType.THREE_POINT, HitchType.DRAWBAR)
        hp >= 250 ->
            listOf(HitchType.THREE_POINT, HitchType.DRAWBAR)
        else ->
            listOf(HitchType.THREE_POINT, HitchType.DRAWBAR, HitchType.FRONT_LOADER)
    }

    val silhouette = when {
        id.startsWith("cbt_") -> "cbt_classico"
        id.startsWith("valmet_") && modelYear < 2002 -> "valmet_classico"
        id.startsWith("agrale_") -> "agrale_bx"
        id.startsWith("mf_") && (modelYear <= 2016 || id.contains("mf_2")) -> "mf_200"
        id.startsWith("jd_7") || id.startsWith("jd_8") || id.startsWith("jd_9") -> "jd_7j_8r"
        else -> null
    }

    return VehicleDef(
        id = id,
        brand = brand,
        model = model,
        category = VehicleCategory.TRATOR,
        era = era,
        modelYear = modelYear,
        horsepower = hp,
        maxTorqueNm = torqueNm,
        maxSpeedKmh = maxSpeedKmh,
        fuelCapacityL = fuelCapacityL,
        weightKg = weightKg,
        priceBrl = priceBrl,
        supportedHitches = supportedHitches,
        primaryColorRgb = primaryColorRgb,
        accentColorRgb = accentColorRgb,
        bodyStyle = bodyStyle,
        wheelLayout = wheelLayout,
        hasCab = hasCab,
        dimensionsM = dims,
        turnRateDegPerSec = turnRate,
        soundProfile = soundProfile,
        hasGrainTank = false,
        tankCapacityL = 0.0,
        gpsReady = modelYear >= 2010 || hp >= 130,
        description = description,
        requiredModPackId = requiredModPackId,
        subTypeLabel = subTypeLabel.ifEmpty { tractorClass.label },
        series = series,
        tractorClass = tractorClass,
        exhaustStyle = exhaustStyle,
        hoodProfile = hoodProfile,
        visualSilhouette = silhouette,
    )
}

internal val C = BrandColors
internal val CO = VehicleBodyStyle.COMPACT_OPEN
internal val C2 = VehicleBodyStyle.CLASSIC_2WD
internal val SC = VehicleBodyStyle.STANDARD_CAB
internal val HP = VehicleBodyStyle.HIGH_POWER_CAB
internal val AR = VehicleBodyStyle.ARTICULATED_4WD
internal val QT = VehicleBodyStyle.QUAD_TRACK

internal val WS = WheelLayout.STANDARD
internal val WD = WheelLayout.DUAL_REAR
internal val WA = WheelLayout.ARTICULATED_DUAL
internal val WC = WheelLayout.CRAWLER_TRACKS

internal val VR = HoodProfile.VINTAGE_ROUND
internal val CS = HoodProfile.CLASSIC_SLOPE
internal val MA = HoodProfile.MODERN_AERO
internal val HN = HoodProfile.HEAVY_NOSE

internal val RP = ExhaustStyle.RIGHT_PILLAR
internal val CH = ExhaustStyle.CENTER_HOOD
internal val DS = ExhaustStyle.DUAL_STACK
internal val HL = ExhaustStyle.HORIZONTAL_LOW

internal typealias TC = TractorClass

val TRACTOR_CATALOG_PART1: List<VehicleDef> = listOf(
    // ================= AGRALE (17) =================
    tr("agrale_4100", "Agrale", "4100 Compacto (Verde)", "Série 4000", TC.HISTORICO, 1986, 18.0, 22.0, 1150.0, 34000.0, CO, WS, false, C.AGRALE_GREEN, C.AGRALE_CREAM, VR, CH, "Diesel Monocilíndrico Agrale", "Microtrator brasileiro histórico de capô verde, ágil para hortaliças, estufas e pequenas propriedades."),
    tr("agrale_4200", "Agrale", "4200 Fruticultura (Verde)", "Série 4000", TC.HISTORICO, 1994, 30.0, 25.0, 1520.0, 46000.0, CO, WS, false, C.AGRALE_GREEN, C.AGRALE_CREAM, VR, CH, "Diesel 2-Cilindros Refrigerado a Ar", "Compacto tradicional verde ideal para pomares, cafezais adensados e pequenas carretas."),
    tr("agrale_4230", "Agrale", "4230 Cargo 4x4", "Série 4000", TC.PEQUENO, 2008, 30.0, 26.0, 1620.0, 52000.0, CO, WS, false, C.AGRALE_RED, C.AGRALE_CREAM, CS, HL, "Agrale M790 2-Cilindros", "Versão 4x4 versátil para hortas, estufas e serviços em aviários."),
    tr("agrale_4300", "Agrale", "4300 4x4 (Verde)", "Série 4000", TC.PEQUENO, 2002, 36.0, 27.0, 1780.0, 58000.0, CO, WS, false, C.AGRALE_GREEN, rgb(0.18, 0.18, 0.18), CS, HL, "Diesel Compacto 3-Cilindros", "Versão 4x4 verde de alta manobrabilidade para cafeicultura de montanha e manejo de estábulos."),
    tr("agrale_540", "Agrale", "540.4 Compacto Frutal", "Linha 500", TC.COMPACTO, 2016, 40.0, 29.0, 2050.0, 74000.0, CO, WS, false, C.AGRALE_RED, rgb(0.18, 0.18, 0.20), CS, HL, "Lombardini 4-Cilindros", "Trator estreito projetado para entrelinhas de parreirais e pomares paranaenses."),
    tr("agrale_575_mod", "Agrale", "575.4 Compacto Fruticultor", "Linha 500", TC.COMPACTO, 2024, 75.0, 34.0, 2950.0, 138000.0, CO, WS, false, C.AGRALE_GREEN, C.AGRALE_CREAM, MA, HL, "MWM 4-Cil Compacto", "Mod Compactos BR: Raio de giro curto para granjas, cafeicultura e pequenas propriedades.", "pack_tratores_compactos_br", "Compactos"),
    tr("agrale_5055", "Agrale", "5055 4x4 Cafeeiro", "Linha 5000", TC.COMPACTO, 2012, 55.0, 31.0, 2450.0, 89000.0, CO, WS, false, C.AGRALE_RED, C.AGRALE_CREAM, CS, RP, "MWM 229-3 Aspirado", "Robusto e simples de manter, popular na agricultura familiar do Sul."),
    tr("agrale_5065", "Agrale", "5065 4x4 Plataforma", "Linha 5000", TC.COMPACTO, 2014, 65.0, 33.0, 2750.0, 108000.0, CO, WS, false, C.AGRALE_RED, rgb(0.16, 0.16, 0.18), CS, RP, "MWM 3-Cilindros Turbo", "Tração dianteira assistida para áreas declivosas e fenação leve."),
    tr("agrale_5075", "Agrale", "5075.4 Compacto", "Linha 5000", TC.COMPACTO, 2019, 75.0, 35.0, 3050.0, 132000.0, CO, WS, false, C.AGRALE_RED, rgb(0.18, 0.18, 0.20), MA, RP, "MWM 4-Cilindros Agrale", "Bitola ajustável para café e fruticultura com inversor mecânico."),
    tr("agrale_5085", "Agrale", "5085 4x4 Plataformado", "Linha 5000", TC.MEDIO, 2020, 85.0, 36.0, 3350.0, 149000.0, C2, WS, false, C.AGRALE_RED, rgb(0.18, 0.18, 0.20), MA, RP, "MWM 4.10T Turbo", "Alta reserva de torque nacional para grades médias e distribuidores."),
    tr("agrale_5085_cab", "Agrale", "5085.4 Cabinado", "Linha 5000", TC.MEDIO, 2022, 85.0, 38.0, 3650.0, 174000.0, SC, WS, true, C.AGRALE_RED, rgb(0.15, 0.15, 0.17), MA, RP, "MWM 4.10T Turbo Cabinado", "Cabine climatizada nacional com excelente visibilidade para pulverização."),
    tr("agrale_bx490", "Agrale", "BX 4.90 Clássico", "Série BX", TC.HISTORICO, 1996, 88.0, 32.0, 3900.0, 94000.0, C2, WS, false, C.AGRALE_GREEN, C.AGRALE_CREAM, CS, CH, "MWM D229-4 Clássico", "Herdeiro da engenharia Deutz no Brasil, muito procurado para ensiladeira."),
    tr("agrale_bx6110", "Agrale", "BX 6110 Traçado", "Série BX", TC.MEDIO, 2011, 105.0, 37.0, 4650.0, 168000.0, SC, WS, true, C.AGRALE_RED, rgb(0.16, 0.16, 0.18), CS, RP, "MWM 4-Cil Turbo Intercooler", "Trator médio de chassi reforçado para plantio de 7 a 9 linhas."),
    tr("agrale_bx6150", "Agrale", "BX 6150 Nacional (Verde)", "Série BX", TC.MEDIO, 2014, 150.0, 38.0, 5600.0, 245000.0, SC, WS, true, C.AGRALE_GREEN, rgb(0.15, 0.15, 0.15), CS, RP, "Diesel MWM 6-Cilindros Turbo", "Trator brasileiro de pintura verde tradicional com ótimo torque para plantio e fenação."),
    tr("agrale_bx6180", "Agrale", "BX 6180 Heavy 6x6", "Série BX", TC.GRANDE, 2017, 178.0, 40.0, 6450.0, 315000.0, HP, WS, true, C.AGRALE_RED, rgb(0.14, 0.14, 0.16), HN, RP, "MWM 6.10 TCA 6-Cilindros", "Topo da linha BX para grades pesadas e carretas graneleiras."),
    tr("agrale_7215", "Agrale", "7215 TDi Nacional", "Linha 7000", TC.GRANDE, 2023, 215.0, 42.0, 7600.0, 445000.0, HP, WD, true, C.AGRALE_RED, rgb(0.15, 0.15, 0.17), MA, RP, "MWM MaxxForce 6-Cilindros", "Maior trator agrícola rígido fabricado pela Agrale em Caxias do Sul."),
    tr("agrale_7215_artic", "Agrale", "7215 Dual Rodado Especial", "Linha 7000", TC.ESPECIAL, 2024, 225.0, 42.0, 8150.0, 488000.0, HP, WD, true, C.AGRALE_GREEN, C.AGRALE_CREAM, MA, RP, "MWM MaxxForce Arrozeiro", "Configuração especial filipada verde clássico para várzeas e arrozeiras do Sul."),

    // ================= CBT (17) =================
    tr("cbt_1000", "CBT", "1000 Perkins 4-Cil", "Série 1000", TC.HISTORICO, 1971, 62.0, 26.0, 3350.0, 49000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, VR, CH, "Perkins 4.236 Clássico", "Pioneiro da Companhia Brasileira de Tratores de São Carlos, robusto e simples."),
    tr("cbt_1020", "CBT", "1020 Mercedes Clássico", "Série 1000", TC.HISTORICO, 1973, 72.0, 27.0, 3650.0, 56000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, VR, CH, "Mercedes OM-314 Aspirado", "Clássico equipado com motor Mercedes-Benz de longa durabilidade."),
    tr("cbt_1065", "CBT", "1065 Agrícola Clássico", "Série 1000", TC.HISTORICO, 1976, 65.0, 28.0, 3450.0, 54000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, VR, CH, "Mercedes OM-314", "Trator histórico versátil para aração e tracionamento de carretas de madeira."),
    tr("cbt_1090a", "CBT", "1090A Clássico (Amarelo)", "Série 1000", TC.HISTORICO, 1978, 91.0, 29.0, 4350.0, 72000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, VR, CH, "Diesel Perkins 6-Cilindros Aspirado", "Clássico amarelo de capô longo sem cabine fechada, famoso na abertura do cerrado."),
    tr("cbt_1105", "CBT", "1105 6-Cilindros", "Série 1000", TC.HISTORICO, 1979, 105.0, 30.0, 4650.0, 81000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, VR, CH, "Perkins 6.357", "Chassi alongado com alto peso na dianteira para grades aradoras pesadas."),
    tr("cbt_2070", "CBT", "2070 Rural Clássico", "Série 2000", TC.HISTORICO, 1981, 68.0, 29.0, 3600.0, 62000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, CS, CH, "Perkins 4.236 Agrícola", "Evolução da linha 1000 com direção hidráulica e embreagem reforçada."),
    tr("cbt_2080", "CBT", "2080 MWM Clássico", "Série 2000", TC.HISTORICO, 1983, 82.0, 30.0, 3950.0, 70000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, CS, CH, "MWM D229-4", "Econômico e confiável para preparo de solo e ensilagem."),
    tr("cbt_2100", "CBT", "2100 Canavieiro 6-Cil", "Série 2000", TC.HISTORICO, 1982, 100.0, 30.0, 4800.0, 79000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, CS, CH, "Mercedes OM-352 Clássico", "Equipada com o lendário OM-352, imbatível no arranque pesado."),
    tr("cbt_2105_mod", "CBT", "2105 Turbo Clássico BR", "Série 2000", TC.MEDIO, 1986, 122.0, 30.0, 5400.0, 92000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, CS, CH, "Perkins 6.354 Aspirado/Turbo", "Mod Clássicos BR: CBT 2105 com torque elevado para arados e grades pesadas.", "pack_cbt_classico", "Médios"),
    tr("cbt_2400", "CBT", "2400 Detroit V6 Pesado", "Série 2000", TC.HISTORICO, 1984, 130.0, 31.0, 6200.0, 108000.0, C2, WD, false, C.CBT_YELLOW, C.CBT_CHASSIS, HN, CH, "Detroit Diesel 4-53", "Raridade brasileira de rodado duplo traseiro com ronco característico 2-tempos."),
    tr("cbt_2500", "CBT", "2500 Puxada Pesada", "Série 2000", TC.HISTORICO, 1985, 118.0, 32.0, 5400.0, 96000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, HN, CH, "Perkins 6.354.4", "Trator de força bruta utilizado em destoca e nivelamento no Paraná."),
    tr("cbt_2600", "CBT", "2600 4x4 Articulado Pioneiro", "Série 2000", TC.ARTICULADO, 1986, 135.0, 31.0, 6800.0, 138000.0, AR, WA, false, C.CBT_YELLOW, C.CBT_CHASSIS, HN, CH, "Perkins 6-Cilindros Pesado", "Clássico articulado nacional de plataforma aberta e tração integral."),
    tr("cbt_8060", "CBT", "8060 4x4 Turbo", "Série 8000", TC.MEDIO, 1988, 108.0, 33.0, 4900.0, 94000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, CS, CH, "Mercedes OM-352A", "Primeira geração 8000 com eixo dianteiro motriz reforçado."),
    tr("cbt_8240", "CBT", "8240 4x4 Florestal/Agro", "Série 8000", TC.MEDIO, 1989, 102.0, 33.0, 4750.0, 91000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, CS, CH, "Perkins 6-Cilindros", "Excelente aderência em barro e subidas íngremes."),
    tr("cbt_8440", "CBT", "8440 Turbo (Amarelo)", "Série 8000", TC.MEDIO, 1991, 112.0, 34.0, 4900.0, 96000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, CS, CH, "Diesel MWM Turbo Clássico", "Lenda brasileira amarela de tração pesada, ideal para grade aradora e abertura de solo."),
    tr("cbt_8450", "CBT", "8450 4x4 Intercooler", "Série 8000", TC.MEDIO, 1993, 128.0, 35.0, 5350.0, 114000.0, C2, WS, false, C.CBT_YELLOW, C.CBT_CHASSIS, CS, CH, "MWM TD229-6 Turbo", "Última evolução da série 8000 com maior vazão hidráulica."),
    tr("cbt_9450", "CBT", "9450 Turbo 4x4 Cabinado", "Série 9000", TC.GRANDE, 1994, 165.0, 36.0, 6700.0, 152000.0, SC, WS, true, C.CBT_YELLOW, C.CBT_CHASSIS, HN, RP, "Cummins 6BT 5.9 Turbo", "Rara série 9000 cabinada com motor Cummins de altíssimo torque."),

    // ================= VALMET (21) =================
    tr("valmet_360d", "Valmet", "360 D Pioneiro", "Clássicos BR", TC.HISTORICO, 1962, 36.0, 22.0, 1950.0, 38000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), VR, CH, "MWM KD12-3 Clássico", "Um dos primeiros tratores fabricados em Mogi das Cruzes, marco histórico nacional."),
    tr("valmet_600d", "Valmet", "600 D Aspirado", "Clássicos BR", TC.HISTORICO, 1965, 52.0, 25.0, 2480.0, 46000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), VR, CH, "MWM D222-4", "Simplicidade mecânica extrema para pequenas propriedades rurais."),
    tr("valmet_60id", "Valmet", "60 id Clássico", "Linha ID", TC.HISTORICO, 1968, 55.0, 26.0, 2580.0, 51000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), VR, CH, "MWM D225-3", "Injeção direta pioneira com baixíssimo consumo de diesel."),
    tr("valmet_62id", "Valmet", "62 id Agrícola", "Linha ID", TC.HISTORICO, 1974, 60.0, 27.0, 2720.0, 56000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), VR, CH, "MWM D225-3", "Excelente equilíbrio para roçadeira, arado 3 discos e carreta leve."),
    tr("valmet_65id", "Valmet", "65 id (Amarelo Antigo)", "Linha ID", TC.HISTORICO, 1976, 62.0, 28.0, 2850.0, 61000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), VR, CH, "Diesel MWM 3-Cilindros Clássico", "Valmet antigo amarelo de plataforma aberta, resistente e econômico."),
    tr("valmet_80id", "Valmet", "80 id Standard", "Linha ID", TC.HISTORICO, 1970, 74.0, 28.0, 3300.0, 64000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), VR, CH, "MWM D225-4", "4 cilindros muito valorizado por sua longevidade no campo brasileiro."),
    tr("valmet_85id", "Valmet", "85 id Puxada", "Linha ID", TC.HISTORICO, 1977, 81.0, 29.0, 3550.0, 69000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), VR, CH, "MWM D225-4 Aspirado", "Clássico das fazendas de café e milho no Paraná e São Paulo."),
    tr("valmet_88", "Valmet", "88 Clássico (1980)", "Linha 88", TC.HISTORICO, 1980, 82.0, 30.0, 3680.0, 74000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), CS, CH, "MWM D229-4 Clássico", "Ícone brasileiro da linha Valmet com capô amarelo estreito, grade frontal preta e plataforma clássica."),
    tr("valmet_110id", "Valmet", "110 id 6-Cilindros", "Linha ID", TC.HISTORICO, 1975, 102.0, 30.0, 4450.0, 82000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), CS, CH, "MWM D225-6", "Primeiro Valmet nacional de 6 cilindros, famoso na abertura de terras."),
    tr("valmet_118_4", "Valmet", "118-4 4x4 Pesado", "Série 100", TC.MEDIO, 1984, 118.0, 32.0, 4950.0, 98000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), CS, CH, "MWM D229-6", "Tração dianteira ZF e estrutura robusta para grade niveladora e aradora."),
    tr("valmet_128_4", "Valmet", "128-4 Traçado", "Série 100", TC.MEDIO, 1986, 128.0, 33.0, 5200.0, 106000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), CS, CH, "MWM D229-6 Reforçado", "Alto desempenho em solo úmido e subsolagem intermediária."),
    tr("valmet_138_4", "Valmet", "138-4 Turbo 4x4", "Série 100", TC.MEDIO, 1987, 138.0, 34.0, 5450.0, 118000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), CS, CH, "MWM TD229-6 Turbo", "Motor turbinado de fábrica para alta produtividade em grandes glebas."),
    tr("valmet_148_4", "Valmet", "148-4 Turbo Pesado", "Série 100", TC.GRANDE, 1989, 148.0, 34.0, 5850.0, 132000.0, C2, WD, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), HN, CH, "MWM TD229-EC6", "Lenda canavieira com opção de rodado duplo e grande capacidade de levante."),
    tr("valmet_685", "Valmet", "685 4x4 (Amarelo)", "Série Leve", TC.COMPACTO, 1993, 68.0, 32.0, 3150.0, 78000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), CS, CH, "Diesel Valmet 3-Cilindros", "Trator nacional clássico amarelo econômico para propriedades pequenas e serviços diários."),
    tr("valmet_785_mod", "Valmet", "785 4x4 Cafeeiro", "Série Leve", TC.COMPACTO, 1998, 78.0, 32.0, 3450.0, 86000.0, CO, WS, false, C.VALMET_YELLOW, rgb(0.22, 0.22, 0.24), CS, HL, "MWM D229-4 Clássico", "Mod Compactos BR: Bitola estreita ideal para cafezais, pomares e terrenos inclinados.", "pack_tratores_compactos_br", "Compactos"),
    tr("valmet_880", "Valmet", "880 4x2/4x4 Clássico", "Série Leve", TC.MEDIO, 1992, 82.0, 33.0, 3750.0, 96000.0, C2, WS, false, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), CS, CH, "Valmet 420D 4-Cilindros", "Clássico intermediário de excelente relação consumo x trabalho diário."),
    tr("valmet_885", "Valmet", "885 4x4 (Amarelo)", "Série Leve", TC.MEDIO, 1998, 85.0, 35.0, 3950.0, 115000.0, SC, WS, true, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), CS, RP, "Diesel 4-Cilindros Valmet", "Intermediário robusto com cabine climatizada e ótima relação peso-potência."),
    tr("valmet_985", "Valmet", "985 4x4 Turbo", "Série Leve", TC.MEDIO, 1997, 105.0, 36.0, 4350.0, 134000.0, SC, WS, true, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), CS, RP, "Valmet 420DS Turbo", "Versátil para plantadeiras de 7 a 9 linhas e pulverizadores de barra."),
    tr("valmet_1180", "Valmet", "1180 S 4x4", "Série Pesada", TC.MEDIO, 1994, 118.0, 35.0, 5150.0, 128000.0, C2, WS, false, C.VALMET_ORANGE, rgb(0.18, 0.18, 0.18), CS, RP, "Valmet 620D 6-Cilindros", "Sucessor da série 100 com visual renovado e comando hidráulico duplo."),
    tr("valmet_1580", "Valmet", "1580 Turbo 4x4", "Série Pesada", TC.GRANDE, 1995, 145.0, 36.0, 5950.0, 158000.0, SC, WS, true, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), HN, RP, "Valmet 620DS Turbo", "Potência de 6 cilindros para grades pesadas e preparo profundo de solo."),
    tr("valmet_1780", "Valmet", "1780 Turbo Intercooler", "Série Pesada", TC.GRANDE, 1997, 168.0, 37.0, 6500.0, 184000.0, SC, WD, true, C.VALMET_YELLOW, rgb(0.18, 0.18, 0.18), HN, RP, "Valmet 620DS Intercooler", "Ícone nacional dos anos 90 com excelente estabilidade em relevo ondulado."),

    // ================= VALTRA (26) =================
    tr("valtra_a550", "Valtra", "A550 Geração II", "Linha A", TC.PEQUENO, 2014, 52.0, 32.0, 2650.0, 112000.0, CO, WS, false, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 3-Cilindros", "Leve e ágil para pecuária leiteira, silagem ensacada e manutenção de pátios."),
    tr("valtra_a650", "Valtra", "A650 4x4 Compacto", "Linha A", TC.COMPACTO, 2015, 66.0, 33.0, 2950.0, 128000.0, CO, WS, false, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 33D", "Operação simples com reversor sincronizado e ótima altura livre do solo."),
    tr("valtra_a750", "Valtra", "A750 4x4 (Amarelo)", "Linha A", TC.COMPACTO, 2016, 78.0, 35.0, 3250.0, 148000.0, SC, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 33DT Turbo", "Cabinado compacto preferido em pequenas lavouras paranaenses e manejo de gado."),
    tr("valtra_a850", "Valtra", "A850 Rádio/Agro", "Linha A", TC.MEDIO, 2016, 85.0, 36.0, 3550.0, 162000.0, SC, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 44D 4-Cil", "Robustez de 4 cilindros para fenação e carregador frontal."),
    tr("valtra_a950", "Valtra", "A950 Geração II", "Linha A", TC.MEDIO, 2017, 95.0, 37.0, 3750.0, 179000.0, SC, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 44DT", "Consagrado no manejo de pastagem, pulverização e distribuidores de calcário."),
    tr("valtra_a990", "Valtra", "A990R Especial", "Linha A", TC.MEDIO, 2018, 105.0, 38.0, 3950.0, 198000.0, SC, WS, true, C.VALTRA_RED, C.VALTRA_BLACK, MA, RP, "AGCO Power 4.4L Turbo", "Potência extra em chassi ágil para terrenos acidentados."),
    tr("valtra_a104", "Valtra", "A104 HiTech4", "Linha A4", TC.MEDIO, 2022, 105.0, 40.0, 4350.0, 238000.0, SC, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 44CW3", "Transmissão HiTech4 com trocas sem embreagem e conforto acústico."),
    tr("valtra_a114", "Valtra", "A114 HiTech4", "Linha A4", TC.MEDIO, 2023, 115.0, 40.0, 4450.0, 258000.0, SC, WS, true, C.VALTRA_RED, C.VALTRA_BLACK, MA, RP, "AGCO Power 4.4L", "Equilíbrio ideal para plantadeiras de 9 a 11 linhas em plantio direto."),
    tr("valtra_a124", "Valtra", "A124 PowerShift", "Linha A4", TC.MEDIO, 2024, 125.0, 40.0, 4650.0, 285000.0, SC, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 4-Cil Heavy", "Inversor eletro-hidráulico com alta vazão para implementos modernos."),
    tr("valtra_a134", "Valtra", "A134 Linha A4", "Linha A4", TC.MEDIO, 2024, 135.0, 40.0, 4850.0, 310000.0, SC, WS, true, C.VALTRA_BLUE, C.VALTRA_BLACK, MA, RP, "AGCO Power 4.4L Turbo", "Edição azul metálico de 135 cv com alta eficiência em campo."),
    tr("valtra_bl88", "Valtra", "BL 88 4x4", "Linha BL", TC.MEDIO, 2008, 88.0, 35.0, 3850.0, 138000.0, C2, WS, false, C.VALTRA_GOLD, C.VALTRA_BLACK, CS, RP, "Valtra 420D Aspirado", "Plataformado resistente muito utilizado em currais e roçadas pesadas."),
    tr("valtra_bm100", "Valtra", "BM 100 4x4", "Linha BM", TC.MEDIO, 2010, 105.0, 36.0, 4650.0, 178000.0, SC, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, CS, RP, "AGCO Sisu 420DS", "Um dos campeões de vendas do Brasil em médias propriedades."),
    tr("valtra_bm110", "Valtra", "BM 110 Geração II", "Linha BM", TC.MEDIO, 2013, 116.0, 37.0, 4950.0, 198000.0, SC, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, CS, RP, "AGCO Sisu 4-Cil Turbo", "Estrutura pesada com MultiTorque para operação contínua."),
    tr("valtra_bm125", "Valtra", "BM 125i (Amarelo/Preto)", "Linha BM", TC.MEDIO, 2012, 132.0, 38.0, 5200.0, 215000.0, SC, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, CS, RP, "AGCO Power 4-Cilindros Turbo", "Linha BM amarela consagrada nas lavouras de soja e milho do Sul e Centro-Oeste."),
    tr("valtra_bh144", "Valtra", "BH 144 G4", "Linha BH", TC.GRANDE, 2020, 145.0, 40.0, 6500.0, 335000.0, HP, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, HN, RP, "AGCO Power 49CWA", "Chassi longo e robusto ideal para canaviais e grades aradoras."),
    tr("valtra_bh165", "Valtra", "BH 165 Geração III", "Linha BH", TC.GRANDE, 2015, 165.0, 39.0, 6850.0, 340000.0, HP, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, CS, RP, "AGCO Power 620DS 6-Cil", "Motor 6 cilindros mecânico confiável de altíssima durabilidade."),
    tr("valtra_bh184", "Valtra", "BH 184 HiTech", "Linha BH", TC.GRANDE, 2021, 180.0, 41.0, 7100.0, 415000.0, HP, WS, true, C.VALTRA_RED, C.VALTRA_BLACK, MA, RP, "AGCO Power 6-Cilindros", "Transmissão HiTech3 com trocas automatizadas em trabalho pesado."),
    tr("valtra_bh194", "Valtra", "BH 194 HiTech (Amarelo Ouro)", "Linha BH", TC.GRANDE, 2021, 195.0, 42.0, 7400.0, 460000.0, HP, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 6-Cilindros Heavy", "Preferido nos canaviais e cerrado para subsolagem, plantio direto e puxada de transbordo."),
    tr("valtra_bh214", "Valtra", "BH 214 HiTech", "Linha BH", TC.GRANDE, 2023, 210.0, 42.0, 7650.0, 520000.0, HP, WD, true, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 6.6L Turbo", "Rodado duplo traseiro de fábrica com excelente capacidade de tração."),
    tr("valtra_bh224", "Valtra", "BH 224 HiTech Heavy", "Linha BH", TC.GRANDE, 2024, 220.0, 43.0, 7900.0, 565000.0, HP, WD, true, C.VALTRA_RED, C.VALTRA_BLACK, MA, RP, "AGCO Power 6.6L", "O mais potente da família BH, pronto para grandes subsoladores."),
    tr("valtra_t195", "Valtra", "T195 CVT", "Linha T", TC.GRANDE, 2022, 195.0, 50.0, 8100.0, 595000.0, HP, WS, true, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power CVT", "Transmissão continuamente variável com suspensão pneumática no eixo dianteiro."),
    tr("valtra_t210", "Valtra", "T210 CVT Direct", "Linha T", TC.GRANDE, 2023, 210.0, 50.0, 8300.0, 645000.0, HP, WS, true, C.VALTRA_BLACK, C.VALTRA_GOLD, MA, RP, "AGCO Power 6-Cil CVT", "Edição Black com piloto automático de precisão e alto conforto operacional."),
    tr("valtra_t230", "Valtra", "T230 CVT Heavy", "Linha T", TC.GRANDE, 2024, 230.0, 50.0, 8450.0, 695000.0, HP, WD, true, C.VALTRA_GOLD, C.VALTRA_BLACK, MA, RP, "AGCO Power 7.4L CVT", "Alto fluxo hidráulico de 200 L/min para plantadeiras pneumáticas."),
    tr("valtra_t250", "Valtra", "T250 CVT (Vermelho Valtra)", "Linha T", TC.GRANDE, 2024, 250.0, 50.0, 8600.0, 740000.0, HP, WD, true, C.VALTRA_RED, C.VALTRA_BLACK, MA, RP, "AGCO Power CVT Alta Rotação", "Geração moderna Valtra vermelha com transmissão CVT e rodado duplo traseiro."),
    tr("valtra_s294", "Valtra", "S294 CVT TwinTrac", "Linha S", TC.ALTA_POTENCIA, 2023, 295.0, 50.0, 10800.0, 990000.0, HP, WD, true, C.VALTRA_RED, C.VALTRA_BLACK, HN, RP, "AGCO Power 8.4L TwinTurbo", "Posto reversível TwinTrac e potência de sobra para implementos largos."),
    tr("valtra_s374", "Valtra", "S374 CVT Bi-Turbo", "Linha S", TC.ALTA_POTENCIA, 2024, 405.0, 50.0, 12100.0, 1460000.0, HP, WD, true, C.VALTRA_BLACK, C.VALTRA_GOLD, HN, RP, "AGCO Power 84AWF Bi-Turbo", "Gigante finlandês-brasileiro de 405 cv com gestão inteligente de carga."),

    // ================= MASSEY FERGUSON (27) =================
    tr("mf_50x", "Massey Ferguson", "MF 50X Cinquentinha Clássico", "Clássicos X", TC.HISTORICO, 1966, 42.0, 26.0, 1950.0, 41000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, VR, CH, "Perkins 3.152 Clássico", "O famoso Cinquentinha que mecanizou pequenas lavouras em todo o Paraná."),
    tr("mf_55x", "Massey Ferguson", "MF 55X Clássico", "Clássicos X", TC.HISTORICO, 1971, 46.0, 27.0, 2120.0, 44000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, VR, CH, "Perkins 3.152", "Famoso pela leveza e economia extrema no transporte em carretas."),
    tr("mf_65x", "Massey Ferguson", "MF 65X Rural Clássico", "Clássicos X", TC.HISTORICO, 1973, 60.0, 28.0, 2550.0, 52000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, VR, CH, "Perkins 4.203", "Motor de 4 cilindros suave e torque linear para arados e roçadeiras."),
    tr("mf_85x", "Massey Ferguson", "MF 85X Pesado Clássico", "Clássicos X", TC.HISTORICO, 1975, 78.0, 29.0, 3250.0, 66000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, VR, CH, "Perkins 4.248 Clássico", "Clássico 4x2 pesado muito respeitado na abertura de terras."),
    tr("mf_235", "Massey Ferguson", "MF 235 Série 200 (Vermelho/Cinza)", "Série 200", TC.HISTORICO, 1982, 50.0, 30.0, 2350.0, 56000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, CS, CH, "Perkins 3-Cilindros Clássico", "Clássico da série 200 em vermelho e cinza, ágil para fenação leve, pulverização e pátio."),
    tr("mf_250x", "Massey Ferguson", "MF 250X Econômico", "Série 200", TC.PEQUENO, 1998, 50.0, 31.0, 2400.0, 69000.0, CO, WS, false, C.MF_RED, C.MF_GRAY, CS, CH, "Perkins 3-Cilindros Modernizado", "Excelente autonomia para hortifruti, estufas e manejo diário do rebanho."),
    tr("mf_265", "Massey Ferguson", "MF 265 Série 200", "Série 200", TC.HISTORICO, 1986, 65.0, 32.0, 2950.0, 74000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, CS, CH, "Perkins 4.203", "Um dos tratores mais produzidos na história agrícola brasileira."),
    tr("mf_275", "Massey Ferguson", "MF 275 Café (Vermelho/Cinza)", "Série 200", TC.HISTORICO, 1990, 75.0, 33.0, 3400.0, 88000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, CS, CH, "Perkins 4-Cilindros Aspirado", "Versátil para lavouras estreitas, manejo de gado e carregamento de fardos."),
    tr("mf_283", "Massey Ferguson", "MF 283 4x4 Série 200", "Série 200", TC.MEDIO, 2004, 86.0, 34.0, 3750.0, 118000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, CS, RP, "Perkins 4.248 4x4", "Tração integral mecânica robusta para encostas e lavouras de feijão."),
    tr("mf_290", "Massey Ferguson", "MF 290 Clássico 4x4", "Série 200", TC.MEDIO, 1995, 86.0, 34.0, 3850.0, 112000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, CS, CH, "Perkins 4-Cilindros Clássico", "Ícone de confiabilidade mecânica presente em quase todo município paranaense."),
    tr("mf_292", "Massey Ferguson", "MF 292 Turbo 4x4", "Série 200", TC.MEDIO, 2002, 105.0, 35.0, 4350.0, 138000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, CS, RP, "Perkins 4.236 Turbo", "Potência turbinada em porte médio para ensilagem e grades médias."),
    tr("mf_297", "Massey Ferguson", "MF 297 6-Cilindros 4x4", "Série 200", TC.MEDIO, 2003, 120.0, 36.0, 5100.0, 156000.0, C2, WS, false, C.MF_RED, C.MF_GRAY, HN, RP, "Perkins 1006-6 6-Cilindros", "Torque vigoroso de 6 cilindros para plantadeiras de 9 a 11 linhas."),
    tr("mf_299", "Massey Ferguson", "MF 299 Turbo 4x4", "Série 200", TC.MEDIO, 2005, 130.0, 36.0, 5300.0, 174000.0, SC, WS, true, C.MF_RED, C.MF_GRAY, HN, RP, "Perkins 1006-6T Turbo", "Topo da lendária série 200 com cabine e excelente peso de tração."),
    tr("mf_4265", "Massey Ferguson", "MF 4265 Compacto 4x4", "Série 4200", TC.COMPACTO, 2012, 65.0, 34.0, 3150.0, 118000.0, CO, WS, false, C.MF_RED, C.MF_GRAY, CS, RP, "AGCO Power 3-Cil", "Substituto moderno da série 200 para café, feno e pequenas propriedades."),
    tr("mf_4275", "Massey Ferguson", "MF 4275 Série 400 (Vermelho/Cinza)", "Série 4200", TC.COMPACTO, 2013, 78.0, 36.0, 3700.0, 132000.0, SC, WS, true, C.MF_RED, C.MF_GRAY, CS, RP, "AGCO Power 4-Cilindros", "Evolução da série 400/4200 com cabine ergonômica e pintura clássica vermelho e cinza."),
    tr("mf_4290", "Massey Ferguson", "MF 4290 Cabinado 4x4", "Série 4200", TC.MEDIO, 2014, 95.0, 37.0, 4250.0, 169000.0, SC, WS, true, C.MF_RED, C.MF_GRAY, CS, RP, "AGCO Power 44D", "Excelente para pulverizadores de arrasto e carregadores frontais."),
    tr("mf_4292", "Massey Ferguson", "MF 4292 Turbo", "Série 4200", TC.MEDIO, 2015, 110.0, 38.0, 4650.0, 192000.0, SC, WS, true, C.MF_RED, C.MF_GRAY, CS, RP, "AGCO Power 4-Cil Turbo", "Transmissão 12x4 sincronizada de alta durabilidade."),
    tr("mf_4299", "Massey Ferguson", "MF 4299 6-Cilindros", "Série 4200", TC.MEDIO, 2016, 130.0, 39.0, 5350.0, 228000.0, SC, WS, true, C.MF_RED, C.MF_GRAY, HN, RP, "AGCO Power 6-Cilindros", "Força contínua para grades aradoras e plantio direto em declives."),
    tr("mf_4707", "Massey Ferguson", "MF 4707 Global Series", "Série 4700", TC.COMPACTO, 2023, 79.0, 40.0, 3850.0, 188000.0, SC, WS, true, C.MF_RED, C.MF_GRAY, MA, RP, "AGCO Power 3.3L Turbo", "Arquitetura global moderna com inversor eletro-hidráulico."),
    tr("mf_5710", "Massey Ferguson", "MF 5710 Dyna-4", "Série 5700", TC.MEDIO, 2023, 105.0, 40.0, 4600.0, 255000.0, SC, WS, true, C.MF_RED, C.MF_GRAY, MA, RP, "AGCO Power 4.4L Dyna-4", "Capô inclinado de ótima visibilidade para carregador frontal e fenação."),
    tr("mf_6711", "Massey Ferguson", "MF 6711 Cabinado", "Série 6700", TC.MEDIO, 2022, 115.0, 40.0, 5150.0, 282000.0, SC, WS, true, C.MF_RED, C.MF_GRAY, MA, RP, "AGCO Power 4-Cilindros", "Ideal para plantio direto e pulverização tecnificada."),
    tr("mf_6713", "Massey Ferguson", "MF 6713 Série 600 (Vermelho/Cinza)", "Série 6700", TC.MEDIO, 2022, 135.0, 40.0, 5400.0, 310000.0, SC, WS, true, C.MF_RED, C.MF_GRAY, MA, RP, "Turbo Intercooler Médio", "Série 600/6700 moderna para plantio direto médio, pulverização e silagem."),
    tr("mf_7215", "Massey Ferguson", "MF 7215 Heavy Duty", "Série 7200", TC.GRANDE, 2021, 155.0, 41.0, 6450.0, 375000.0, HP, WS, true, C.MF_RED, C.MF_GRAY, MA, RP, "AGCO Power 6.6L", "Chassi reforçado para operações severas de preparo de solo."),
    tr("mf_7318", "Massey Ferguson", "MF 7318 Dyna-3", "Série 7300", TC.GRANDE, 2023, 180.0, 42.0, 7400.0, 470000.0, HP, WS, true, C.MF_RED, C.MF_GRAY, MA, RP, "AGCO Power 6-Cil Dyna-3", "Transmissão robusta para transbordo canavieiro e grandes plantadeiras."),
    tr("mf_7720", "Massey Ferguson", "MF 7720 Série 7000 Dyna-6", "Série 7700", TC.GRANDE, 2023, 215.0, 45.0, 8200.0, 610000.0, HP, WD, true, C.MF_RED, C.MF_GRAY, MA, RP, "Diesel 6-Cilindros Dyna-6", "Série 7000 de alta potência com capô aerodinâmico vermelho/cinza e transmissão Dyna-6."),
    tr("mf_8737", "Massey Ferguson", "MF 8737 S Dyna-VT", "Série 8700S", TC.ALTA_POTENCIA, 2024, 370.0, 50.0, 12600.0, 1380000.0, HP, WD, true, C.MF_RED, C.MF_GRAY, HN, RP, "AGCO Power 8.4L TwinTurbo CVT", "Transmissão contínua Dyna-VT com rodado duplo para grandes extensões."),
    tr("mf_9s425", "Massey Ferguson", "MF 9S.425 Dyna-VT Flagship", "Série 9S", TC.ALTA_POTENCIA, 2025, 425.0, 50.0, 14200.0, 1690000.0, HP, WD, true, C.MF_RED, rgb(0.22, 0.22, 0.24), MA, RP, "AGCO Power 8.4L Protect-U", "Cabine Protect-U isolada do motor com 425 cv e telemetria completa."),

    // ================= JOHN DEERE (28) =================
    tr("jd_3036e", "John Deere", "3036E Compact Utility", "Série 3E", TC.PEQUENO, 2022, 36.0, 28.0, 1350.0, 108000.0, CO, WS, false, C.JD_GREEN, C.JD_YELLOW, MA, HL, "Yanmar 3-Cilindros JD", "Compacto verde e amarelo ideal para granjas, estufas e pequenas chácaras."),
    tr("jd_4049m", "John Deere", "4049M Heavy Compact", "Série 4M", TC.PEQUENO, 2023, 49.0, 30.0, 1780.0, 136000.0, CO, WS, false, C.JD_GREEN, C.JD_YELLOW, MA, HL, "Yanmar 4-Cil Turbo", "Transmissão eHydro e ótimo levante para carregador frontal."),
    tr("jd_5055e", "John Deere", "5055E 4x4 Série 5", "Série 5E", TC.COMPACTO, 2021, 55.0, 32.0, 2850.0, 142000.0, CO, WS, false, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 3029 3-Cil", "Plataformado ágil para manejo de pastagens e feno."),
    tr("jd_5065e", "John Deere", "5065E 4x4 Série 5", "Série 5E", TC.COMPACTO, 2022, 65.0, 33.0, 3050.0, 154000.0, CO, WS, false, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 3029T Turbo", "Excelente torque em baixa rotação para roçadeiras e ensiladeiras."),
    tr("jd_5075e", "John Deere", "5075E Série 5 (Verde/Amarelo)", "Série 5E", TC.COMPACTO, 2023, 75.0, 34.0, 3250.0, 168000.0, SC, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 3-Cilindros Turbo", "Série 5 verde e amarela compacta-média para pecuária, feno e lavouras familiares."),
    tr("jd_5078e", "John Deere", "5078E 4-Cilindros", "Série 5E", TC.COMPACTO, 2020, 78.0, 35.0, 3480.0, 176000.0, SC, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 4.5L Nacional", "Versão brasileira de 4 cilindros muito valorizada em pequenas lavouras."),
    tr("jd_5085e", "John Deere", "5085E Cabinado", "Série 5E", TC.MEDIO, 2023, 85.0, 37.0, 3750.0, 212000.0, SC, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 4045T Turbo", "Cabine silenciosa para pulverização e manejo diário de gado leiteiro."),
    tr("jd_5090e", "John Deere", "5090E PowerReverser", "Série 5E", TC.MEDIO, 2024, 90.0, 38.0, 3950.0, 234000.0, SC, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 4.5L 16V", "Inversor eletro-hidráulico e alta capacidade de levante traseiro."),
    tr("jd_6100j", "John Deere", "6100J Série 6", "Série 6J", TC.MEDIO, 2021, 100.0, 39.0, 4950.0, 298000.0, SC, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 4.5L Heavy", "Chassi integral de aço para maior durabilidade com carregador frontal."),
    tr("jd_6110j", "John Deere", "6110J PowerQuad", "Série 6J", TC.MEDIO, 2022, 110.0, 40.0, 5150.0, 325000.0, SC, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 4045 Turbo", "Trocas PowerQuad sob carga para produtividade em plantio e transporte."),
    tr("jd_6115j", "John Deere", "6115J Série 6", "Série 6J", TC.MEDIO, 2022, 115.0, 40.0, 5250.0, 342000.0, SC, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 4.5L", "Consagrado no Sul do Brasil pela precisão hidráulica e economia."),
    tr("jd_6125j", "John Deere", "6125J Série 6 (Verde/Amarelo)", "Série 6J", TC.MEDIO, 2022, 125.0, 40.0, 5450.0, 365000.0, SC, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 4-Cilindros Heavy", "Série 6 consagrada no Brasil pela força hidráulica e precisão em plantio."),
    tr("jd_6135m", "John Deere", "6135M Nova Geração", "Série 6M", TC.MEDIO, 2024, 135.0, 42.0, 6200.0, 435000.0, SC, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech PSS 4.5L", "Capô rebaixado de excelente visibilidade e suspensão dianteira TLS."),
    tr("jd_6150j", "John Deere", "6150J 6-Cilindros", "Série 6J", TC.GRANDE, 2022, 150.0, 40.0, 6650.0, 455000.0, HP, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 6.8L 6-Cil", "Suavidade de 6 cilindros com estrutura de chassi completo para grades pesadas."),
    tr("jd_6170j", "John Deere", "6170J Precision", "Série 6J", TC.GRANDE, 2023, 170.0, 41.0, 7150.0, 520000.0, HP, WS, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 6068T", "Ideal para plantadeiras de 13 a 15 linhas com taxa variável."),
    tr("jd_6190j", "John Deere", "6190J Heavy", "Série 6J", TC.GRANDE, 2023, 190.0, 42.0, 7600.0, 585000.0, HP, WD, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech 6.8L Heavy", "Rodado duplo traseiro e alto torque para subsoladores médios."),
    tr("jd_6210r", "John Deere", "6210R AutoPowr IVT", "Série 6R", TC.GRANDE, 2024, 210.0, 50.0, 8450.0, 745000.0, HP, WD, true, C.JD_GREEN, C.JD_YELLOW, MA, RP, "PowerTech PVX 6.8L IVT", "Transmissão infinitamente variável e cabine CommandView premium."),
    tr("jd_7200j", "John Deere", "7200J Série 7", "Série 7J", TC.GRANDE, 2022, 200.0, 42.0, 8400.0, 610000.0, HP, WS, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "PowerTech Plus 6.8L", "Projeto brasileiro robusto focado em alto rendimento em soja e cana."),
    tr("jd_7215j", "John Deere", "7215J CommandQuad", "Série 7J", TC.GRANDE, 2023, 215.0, 43.0, 8650.0, 645000.0, HP, WD, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "PowerTech 6.8L", "Transmissão automática CommandQuad e receptor StarFire integrado."),
    tr("jd_7230j", "John Deere", "7230J Série 7 Precision", "Série 7J", TC.GRANDE, 2023, 230.0, 45.0, 8900.0, 680000.0, HP, WS, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "PowerTech 6.8L 6-Cilindros", "Série 7 de alta tecnologia embarcada, piloto automático de fábrica e tração de precisão."),
    tr("jd_8295r", "John Deere", "8295R e23 PowerShift", "Série 8R", TC.ALTA_POTENCIA, 2021, 295.0, 44.0, 12900.0, 1080000.0, HP, WD, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "PowerTech PSS 9.0L", "Distribuição ideal de peso para grandes plantadeiras de milho e soja."),
    tr("jd_8320r", "John Deere", "8320R ILS Dual", "Série 8R", TC.ALTA_POTENCIA, 2022, 320.0, 46.0, 13400.0, 1210000.0, HP, WD, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "PowerTech PSS 9.0L", "Suspensão independente ILS no eixo dianteiro para alta velocidade em campo."),
    tr("jd_8370r", "John Deere", "8370R Série 8 (Verde/Amarelo)", "Série 8R", TC.ALTA_POTENCIA, 2024, 370.0, 48.0, 14200.0, 1380000.0, HP, WD, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "PowerTech 9.0L Heavy", "Série 8 de capô imponente e rodado duplo traseiro para grandes plantadeiras."),
    tr("jd_8r410", "John Deere", "8R 410 e23 Heavy", "Série 8R", TC.ALTA_POTENCIA, 2025, 410.0, 50.0, 14900.0, 1650000.0, HP, WD, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "PowerTech 9.0L High Output", "Nova geração 8R com até 443 cv de potência máxima e cabine CommandView 4."),
    tr("jd_9470r", "John Deere", "9470R Série 9 Articulado 4WD", "Série 9R", TC.ARTICULADO, 2024, 470.0, 42.0, 20400.0, 1980000.0, AR, WA, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "Articulado 13.5L Bi-Turbo", "Gigante articulado Série 9 com rodagem dupla nos dois eixos para chapadões do Cerrado."),
    tr("jd_9570r", "John Deere", "9570R Articulado Dual", "Série 9R", TC.ARTICULADO, 2024, 570.0, 42.0, 22400.0, 2380000.0, AR, WA, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "Cummins QSX15 15L", "Chassi articulado extremo com 8 pneus largos para abertura e subsolagem."),
    tr("jd_9rx640", "John Deere", "9RX 640 Série 9RX Esteiras", "Série 9RX", TC.ALTA_POTENCIA, 2025, 640.0, 40.0, 27800.0, 2850000.0, QT, WC, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "Motor V8/15L Esteiras Quádruplas", "Topo de linha Série 9RX com 4 esteiras triangulares para máxima tração e mínima compactação."),
    tr("jd_9rx830", "John Deere", "9RX 830 Extreme Crawler", "Série 9RX", TC.ALTA_POTENCIA, 2025, 830.0, 40.0, 33500.0, 3550000.0, QT, WC, true, C.JD_GREEN, C.JD_YELLOW, HN, RP, "JD18X 18.0L Twin-Turbo 830cv", "O trator agrícola mais potente da marca, com 830 cv nominais e 4 esteiras gigantes."),

    // ================= NEW HOLLAND (25) =================
    tr("nh_tt3840", "New Holland", "TT 3840 Plataformado", "Linha TT", TC.PEQUENO, 2016, 55.0, 31.0, 2450.0, 108000.0, CO, WS, false, C.NH_BLUE, C.NH_WHITE, CS, HL, "FPT S8000 3-Cil", "Compacto simples e robusto para horticultura e propriedades familiares."),
    tr("nh_tt4030", "New Holland", "TT 4030 4x4", "Linha TT", TC.COMPACTO, 2018, 75.0, 33.0, 2950.0, 138000.0, CO, WS, false, C.NH_BLUE, C.NH_WHITE, CS, HL, "FPT S8000 4-Cil", "Ótimo custo-benefício em tracionamento de carretas e roçadeiras."),
    tr("nh_tt455", "New Holland", "TT4.55 4WD", "Linha TT4", TC.PEQUENO, 2022, 55.0, 33.0, 2750.0, 129000.0, CO, WS, false, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT 3-Cilindros", "Plataforma ergonômica e capô moderno inclinado para visão frontal limpa."),
    tr("nh_tt475", "New Holland", "TT4.75 Plataformado", "Linha TT4", TC.COMPACTO, 2023, 75.0, 35.0, 3100.0, 152000.0, CO, WS, false, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT 3.9L Turbo", "Excelente reserva de torque em pequenas propriedades de leite e grãos."),
    tr("nh_tl580", "New Holland", "TL5.80 (Azul)", "Linha TL5", TC.MEDIO, 2023, 80.0, 36.0, 3550.0, 179000.0, SC, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT S8000 4-Cilindros", "Azul clássico brasileiro de porte médio para fenação, café e pecuária leiteira."),
    tr("nh_tl590", "New Holland", "TL5.90 PowerShuttle", "Linha TL5", TC.MEDIO, 2023, 90.0, 38.0, 3780.0, 204000.0, SC, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT S8000 Turbo", "Cabine espaçosa e reversor hidráulico para agilidade no curral."),
    tr("nh_tl5100", "New Holland", "TL5.100 4x4 Cabinado", "Linha TL5", TC.MEDIO, 2024, 100.0, 40.0, 3980.0, 228000.0, SC, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT 3.9L Intercooler", "Sucesso absoluto na agricultura paranaense para pulverização e plantio."),
    tr("nh_t475v", "New Holland", "T4.75V Fruticultor Estreito", "Linha T4", TC.ESPECIAL, 2023, 75.0, 36.0, 2680.0, 189000.0, SC, WS, true, C.NH_BLUE, C.NH_WHITE, MA, HL, "FPT F5C 3.4L Compacto", "Bitola estreita cabinada especial para pomares de maçã, uva e café adensado."),
    tr("nh_t5110", "New Holland", "T5.110 Electro Command", "Linha T5", TC.MEDIO, 2023, 110.0, 40.0, 4750.0, 275000.0, SC, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT F5C 3.4L", "Cabine Horizon com excelente conforto para longas jornadas de trabalho."),
    tr("nh_t6110", "New Holland", "T6.110 DualCommand", "Linha T6", TC.MEDIO, 2022, 112.0, 40.0, 5200.0, 315000.0, SC, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT NEF 4.5L", "Motor de 4.5 litros com alto rendimento hidráulico em plantio direto."),
    tr("nh_t6130", "New Holland", "T6.130 ElectroCommand", "Linha T6", TC.MEDIO, 2023, 134.0, 42.0, 5600.0, 368000.0, SC, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT NEF 4.5L 16V", "Líder de versatilidade para plantadeiras de 9 a 11 linhas e fardos grandes."),
    tr("nh_t6140", "New Holland", "T6.140 AutoCommand CVT", "Linha T6", TC.MEDIO, 2024, 145.0, 45.0, 5950.0, 425000.0, SC, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT NEF 6-Cilindros CVT", "Transmissão contínua CVT em chassi ágil para transporte rápido."),
    tr("nh_t7175", "New Holland", "T7.175 Semi-PowerShift", "Linha T7", TC.GRANDE, 2022, 175.0, 42.0, 7250.0, 510000.0, HP, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT NEF 6.7L", "6 cilindros equilibrado para preparo de solo e semeadura média."),
    tr("nh_t7190", "New Holland", "T7.190 RangeCommand", "Linha T7", TC.GRANDE, 2023, 190.0, 43.0, 7650.0, 555000.0, HP, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT NEF 6.7L Turbo", "Excelente autonomia e conforto com suspensão de cabine Comfort Ride."),
    tr("nh_t7205", "New Holland", "T7.205 PowerCommand", "Linha T7", TC.GRANDE, 2023, 205.0, 44.0, 8250.0, 615000.0, HP, WD, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT NEF 6.7L Heavy", "Full PowerShift ideal para plantadeiras largas e transbordo de cana."),
    tr("nh_t7245", "New Holland", "T7.245 AutoCommand (Azul)", "Linha T7", TC.GRANDE, 2024, 245.0, 46.0, 9100.0, 720000.0, HP, WS, true, C.NH_BLUE, C.NH_WHITE, MA, RP, "FPT NEF 6-Cilindros CVT", "Transmissão contínua CVT azul para plantadeiras largas e carretas graneleiras pesadas."),
    tr("nh_t7260", "New Holland", "T7.260 Heavy Duty", "Linha T7", TC.GRANDE, 2024, 260.0, 50.0, 9600.0, 815000.0, HP, WD, true, C.NH_BLUE, C.NH_WHITE, HN, RP, "FPT NEF 6.7L EPM", "Boost de potência para até 260 cv na TDP e transporte rodoviário."),
    tr("nh_t7300", "New Holland", "T7.300 PLM Intelligence", "Linha T7 HD", TC.ALTA_POTENCIA, 2025, 300.0, 50.0, 10800.0, 1050000.0, HP, WD, true, C.NH_BLUE, C.NH_WHITE, HN, RP, "FPT NEF 6.7L High Output", "Chassi HD com conectividade PLM e pneus de grande diâmetro."),
    tr("nh_t8325", "New Holland", "T8.325 UltraCommand", "Linha T8", TC.ALTA_POTENCIA, 2022, 325.0, 44.0, 12600.0, 1180000.0, HP, WD, true, C.NH_BLUE, C.NH_WHITE, HN, RP, "FPT Cursor 9 8.7L", "Entre-eixos longo para máxima estabilidade longitudinal em sulcos fundos."),
    tr("nh_t8385", "New Holland", "T8.385 PLM Intelligence", "Linha T8", TC.ALTA_POTENCIA, 2023, 385.0, 46.0, 13600.0, 1390000.0, HP, WD, true, C.NH_BLUE, C.NH_WHITE, HN, RP, "FPT Cursor 9 Heavy", "Capô imponente azul com rodado duplo e telemetria avançada."),
    tr("nh_t8435", "New Holland", "T8.435 SmartTrax", "Linha T8", TC.ALTA_POTENCIA, 2025, 435.0, 42.0, 16400.0, 1790000.0, HP, WD, true, C.NH_BLUE, C.NH_WHITE, HN, RP, "Cursor 9 435cv Heavy", "Maior trator convencional da New Holland para grandes fazendas de grãos."),
    tr("nh_t9560", "New Holland", "T9.560 Articulado", "Linha T9", TC.ARTICULADO, 2023, 557.0, 40.0, 21200.0, 2080000.0, AR, WA, true, C.NH_BLUE, C.NH_WHITE, HN, RP, "FPT Cursor 13 12.9L", "Articulado de chassi longo para grandes grades aradoras e escarificadores."),
    tr("nh_t9600", "New Holland", "T9.600 Articulado (Azul)", "Linha T9", TC.ARTICULADO, 2024, 540.0, 41.0, 22500.0, 2250000.0, AR, WA, true, C.NH_BLUE, C.NH_WHITE, HN, RP, "Cursor 13 Articulado Pesado", "Articulado gigante azul com chassi duplo para subsoladores e plantio em grande escala."),
    tr("nh_t9700", "New Holland", "T9.700 PLM Heavy 4WD", "Linha T9", TC.ARTICULADO, 2025, 699.0, 40.0, 25600.0, 2790000.0, AR, WA, true, C.NH_BLUE, C.NH_WHITE, HN, RP, "Cursor 13 Two-Stage Turbo 699cv", "Quase 700 cv em chassi articulado de rodado duplo nos dois eixos."),
)
