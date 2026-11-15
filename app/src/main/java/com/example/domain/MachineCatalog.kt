package com.example.domain

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
data class ImplementDef(
    val id: String,
    val brand: String,
    val model: String,
    val category: ImplementCategory,
    val hitchType: HitchType,
    val requiredHp: Double,
    val workingWidthM: Double,
    val weightKg: Double,
    val capacityL: Double,
    val priceBrl: Double,
    val colorRgb: RgbTriple,
    val description: String,
    val requiredModPackId: String? = null,
    val subTypeLabel: String = "",
)

data class WheelOption(
    val id: String,
    val name: String,
    val tractionBonus: Double,
    val compactionPenaltyReduction: Double,
    val priceBrl: Double,
)

data class EngineUpgradeOption(
    val id: String,
    val name: String,
    val hpMultiplier: Double,
    val fuelUseMultiplier: Double,
    val priceBrl: Double,
)

data class PaintOption(
    val id: String,
    val name: String,
    val rgb: RgbTriple?,
    val priceBrl: Double,
)

private fun v(
    id: String,
    brand: String,
    model: String,
    category: VehicleCategory,
    era: String,
    horsepower: Double,
    maxTorqueNm: Double,
    maxSpeedKmh: Double,
    fuelCapacityL: Double,
    weightKg: Double,
    priceBrl: Double,
    supportedHitches: List<HitchType>,
    primaryColorRgb: RgbTriple,
    accentColorRgb: RgbTriple,
    description: String,
    modelYear: Int = 2024,
    bodyStyle: VehicleBodyStyle = VehicleBodyStyle.STANDARD_CAB,
    wheelLayout: WheelLayout = WheelLayout.STANDARD,
    hasCab: Boolean = true,
    dimensionsM: DimTriple = dim(4.2, 2.1, 2.8),
    turnRateDegPerSec: Double = 52.0,
    soundProfile: String = "Diesel 4-Cilindros Turbo",
    hasGrainTank: Boolean = false,
    tankCapacityL: Double = 0.0,
    gpsReady: Boolean = true,
    requiredModPackId: String? = null,
    subTypeLabel: String = "",
    visualSilhouette: String? = null,
    driveLabel: String? = null,
    specSourceNote: String? = null,
): VehicleDef = VehicleDef(
    id = id,
    brand = brand,
    model = model,
    category = category,
    era = era,
    modelYear = modelYear,
    horsepower = horsepower,
    maxTorqueNm = maxTorqueNm,
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
    dimensionsM = dimensionsM,
    turnRateDegPerSec = turnRateDegPerSec,
    soundProfile = soundProfile,
    hasGrainTank = hasGrainTank,
    tankCapacityL = tankCapacityL,
    gpsReady = gpsReady,
    description = description,
    requiredModPackId = requiredModPackId,
    subTypeLabel = subTypeLabel,
    visualSilhouette = visualSilhouette,
    driveLabel = driveLabel,
    specSourceNote = specSourceNote,
)

private val NON_TRACTOR_VEHICLES: List<VehicleDef> = listOf(
    v(
        id = "mf_5650",
        brand = "Massey Ferguson",
        model = "MF 5650 Advanced (Vermelho/Cinza)",
        category = VehicleCategory.COLHEITADEIRA,
        era = "Intermediário",
        modelYear = 2016,
        horsepower = 175.0,
        maxTorqueNm = 680.0,
        maxSpeedKmh = 26.0,
        fuelCapacityL = 360.0,
        weightKg = 9200.0,
        priceBrl = 490000.0,
        supportedHitches = listOf(HitchType.HEADER),
        primaryColorRgb = C.MF_RED,
        accentColorRgb = rgb(0.85, 0.85, 0.85),
        bodyStyle = VehicleBodyStyle.HARVESTER,
        hasCab = true,
        dimensionsM = dim(6.8, 4.8, 3.7),
        turnRateDegPerSec = 38.0,
        soundProfile = "Colheitadeira Diesel 6-Cilindros",
        hasGrainTank = true,
        tankCapacityL = 5500.0,
        gpsReady = true,
        description = "Colheitadeira confiável para soja, milho, trigo e feijão em médias propriedades.",
    ),
    v(
        id = "jd_s770",
        brand = "John Deere",
        model = "S770 Axial (Verde/Amarelo)",
        category = VehicleCategory.COLHEITADEIRA,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 390.0,
        maxTorqueNm = 1550.0,
        maxSpeedKmh = 30.0,
        fuelCapacityL = 950.0,
        weightKg = 17400.0,
        priceBrl = 1850000.0,
        supportedHitches = listOf(HitchType.HEADER),
        primaryColorRgb = C.JD_GREEN,
        accentColorRgb = C.JD_YELLOW,
        bodyStyle = VehicleBodyStyle.HARVESTER,
        hasCab = true,
        dimensionsM = dim(8.4, 6.2, 4.1),
        turnRateDegPerSec = 34.0,
        soundProfile = "Colheitadeira Axial 9.0L",
        hasGrainTank = true,
        tankCapacityL = 10600.0,
        gpsReady = true,
        description = "Colheitadeira axial de alta capacidade com telemetria e mapeamento de produtividade.",
    ),
    v(
        id = "case_8250",
        brand = "Case IH",
        model = "Axial-Flow 8250 (Vermelho/Preto)",
        category = VehicleCategory.COLHEITADEIRA,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 498.0,
        maxTorqueNm = 1980.0,
        maxSpeedKmh = 32.0,
        fuelCapacityL = 1120.0,
        weightKg = 19800.0,
        priceBrl = 2350000.0,
        supportedHitches = listOf(HitchType.HEADER),
        primaryColorRgb = C.CASE_RED,
        accentColorRgb = C.CASE_BLACK,
        bodyStyle = VehicleBodyStyle.HARVESTER,
        hasCab = true,
        dimensionsM = dim(8.8, 6.8, 4.2),
        turnRateDegPerSec = 33.0,
        soundProfile = "Colheitadeira Axial Cursor 13",
        hasGrainTank = true,
        tankCapacityL = 14400.0,
        gpsReady = true,
        description = "Colheitadeira de grande porte com rotor axial único para colheita de alta velocidade.",
    ),
    v(
        id = "mb_1113",
        brand = "Mercedes-Benz",
        model = "L-1113 Basculante (Clássico)",
        category = VehicleCategory.CAMINHAO,
        era = "Clássico",
        modelYear = 1980,
        horsepower = 147.0,
        maxTorqueNm = 480.0,
        maxSpeedKmh = 78.0,
        fuelCapacityL = 210.0,
        weightKg = 5400.0,
        priceBrl = 115000.0,
        supportedHitches = listOf(HitchType.DRAWBAR),
        primaryColorRgb = rgb(0.16, 0.44, 0.68),
        accentColorRgb = rgb(0.88, 0.88, 0.86),
        bodyStyle = VehicleBodyStyle.TRUCK,
        hasCab = true,
        dimensionsM = dim(6.6, 2.35, 2.75),
        turnRateDegPerSec = 44.0,
        soundProfile = "Mercedes OM-352 Diesel Clássico",
        hasGrainTank = true,
        tankCapacityL = 12000.0,
        gpsReady = false,
        description = "Ícone das estradas de terra brasileiras para escoamento de safra e insumos.",
    ),
    v(
        id = "jacto_uniport_3030",
        brand = "Jacto",
        model = "Uniport 3030 (32m • 3.000 L)",
        category = VehicleCategory.PULVERIZADOR_AUTOPROPELIDO,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 243.0,
        maxTorqueNm = 980.0,
        maxSpeedKmh = 50.0,
        fuelCapacityL = 280.0,
        weightKg = 9900.0,
        priceBrl = 1180000.0,
        supportedHitches = emptyList(),
        primaryColorRgb = rgb(0.06, 0.36, 0.72),
        accentColorRgb = rgb(0.95, 0.44, 0.08),
        bodyStyle = VehicleBodyStyle.SELF_PROPELLED_SPRAYER,
        wheelLayout = WheelLayout.STANDARD,
        hasCab = true,
        dimensionsM = dim(8.2, 2.95, 3.85),
        turnRateDegPerSec = 46.0,
        soundProfile = "Cummins QSB 6.7 Hidrostático",
        hasGrainTank = false,
        tankCapacityL = 3000.0,
        gpsReady = true,
        visualSilhouette = "uniport",
        subTypeLabel = "Barras 32m • Cabine Frontal",
        driveLabel = "4x4 Hidrostático Independente",
        specSourceNote = "Vão livre 1,65m, tanque 3.000 L, barra de 32m com corte bico a bico.",
        description = "Pulverizador autopropelido brasileiro líder em lavouras de soja e milho com cabine panorâmica frontal.",
    ),
    v(
        id = "jacto_uniport_4530",
        brand = "Jacto",
        model = "Uniport 4530 (36m • 4.500 L)",
        category = VehicleCategory.PULVERIZADOR_AUTOPROPELIDO,
        era = "Moderno",
        modelYear = 2025,
        horsepower = 243.0,
        maxTorqueNm = 1020.0,
        maxSpeedKmh = 52.0,
        fuelCapacityL = 320.0,
        weightKg = 11200.0,
        priceBrl = 1460000.0,
        supportedHitches = emptyList(),
        primaryColorRgb = rgb(0.06, 0.36, 0.72),
        accentColorRgb = rgb(0.95, 0.44, 0.08),
        bodyStyle = VehicleBodyStyle.SELF_PROPELLED_SPRAYER,
        wheelLayout = WheelLayout.STANDARD,
        hasCab = true,
        dimensionsM = dim(8.7, 3.05, 3.95),
        turnRateDegPerSec = 44.0,
        soundProfile = "Cummins QSB 6.7 243cv",
        hasGrainTank = false,
        tankCapacityL = 4500.0,
        gpsReady = true,
        visualSilhouette = "uniport",
        subTypeLabel = "Barras 36m • 4.500 L",
        driveLabel = "4x4 Hidrostático • Vão Livre 1,75m",
        specSourceNote = "Reservatório 4.500 L e barras de fibra/aço de 36m com suspensão pneumática.",
        description = "Autopropelido de grande autonomia fabricado em Pompeia-SP para grandes talhões do Cerrado.",
    ),
    v(
        id = "stara_imperador_30",
        brand = "Stara",
        model = "Imperador 3.0 Dual (Barras Centrais)",
        category = VehicleCategory.PULVERIZADOR_AUTOPROPELIDO,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 220.0,
        maxTorqueNm = 910.0,
        maxSpeedKmh = 48.0,
        fuelCapacityL = 290.0,
        weightKg = 10100.0,
        priceBrl = 1290000.0,
        supportedHitches = emptyList(),
        primaryColorRgb = rgb(0.91, 0.42, 0.06),
        accentColorRgb = rgb(0.16, 0.18, 0.2),
        bodyStyle = VehicleBodyStyle.SELF_PROPELLED_SPRAYER,
        wheelLayout = WheelLayout.STANDARD,
        hasCab = true,
        dimensionsM = dim(8.1, 2.95, 3.85),
        turnRateDegPerSec = 47.0,
        soundProfile = "Cummins 6.7 Hidro Stara",
        hasGrainTank = false,
        tankCapacityL = 2400.0,
        gpsReady = true,
        visualSilhouette = "imperador",
        subTypeLabel = "Barras Centrais 30m • Dual",
        driveLabel = "4x4 Hidrostático • Esterço 4 Rodas",
        specSourceNote = "Único pulverizador com barras centrais de 30m e distribuidor acoplado.",
        description = "Projeto nacional de Não-Me-Toque (RS) com estabilidade superior em barras centrais.",
    ),
    v(
        id = "stara_imperador_4000",
        brand = "Stara",
        model = "Imperador 4000 Eco Spray",
        category = VehicleCategory.PULVERIZADOR_AUTOPROPELIDO,
        era = "Moderno",
        modelYear = 2025,
        horsepower = 260.0,
        maxTorqueNm = 1080.0,
        maxSpeedKmh = 50.0,
        fuelCapacityL = 340.0,
        weightKg = 11800.0,
        priceBrl = 1580000.0,
        supportedHitches = emptyList(),
        primaryColorRgb = rgb(0.91, 0.42, 0.06),
        accentColorRgb = rgb(0.16, 0.18, 0.2),
        bodyStyle = VehicleBodyStyle.SELF_PROPELLED_SPRAYER,
        wheelLayout = WheelLayout.STANDARD,
        hasCab = true,
        dimensionsM = dim(8.6, 3.1, 3.98),
        turnRateDegPerSec = 45.0,
        soundProfile = "Cummins 6.7 High Torque",
        hasGrainTank = false,
        tankCapacityL = 4000.0,
        gpsReady = true,
        visualSilhouette = "imperador",
        subTypeLabel = "Barras Centrais 36m • 4.000 L",
        driveLabel = "4x4 Hidrostático • Sobe & Desce",
        specSourceNote = "Sistema Sobe e Desce de vão livre até 2,0m com 4.000 L e barras de 36m.",
        description = "Topo de linha Stara com aplicação localizada Eco Spray, barras centrais e rodado alto.",
    ),
    v(
        id = "vw_constellation",
        brand = "Volkswagen",
        model = "Constellation 24.280 Graneleiro",
        category = VehicleCategory.CAMINHAO,
        era = "Moderno",
        modelYear = 2023,
        horsepower = 275.0,
        maxTorqueNm = 1050.0,
        maxSpeedKmh = 92.0,
        fuelCapacityL = 400.0,
        weightKg = 7800.0,
        priceBrl = 430000.0,
        supportedHitches = listOf(HitchType.DRAWBAR, HitchType.FIFTH_WHEEL),
        primaryColorRgb = rgb(0.9, 0.9, 0.92),
        accentColorRgb = rgb(0.12, 0.24, 0.48),
        bodyStyle = VehicleBodyStyle.TRUCK,
        hasCab = true,
        dimensionsM = dim(7.8, 2.45, 3.1),
        turnRateDegPerSec = 42.0,
        soundProfile = "MAN D08 6-Cilindros",
        hasGrainTank = true,
        tankCapacityL = 20000.0,
        gpsReady = true,
        description = "Caminhão graneleiro robusto para entregas em cooperativas e balanças rodoviárias.",
    ),
    v(
        id = "scania_r450",
        brand = "Scania",
        model = "R450 Rodotrem Agro",
        category = VehicleCategory.CAMINHAO,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 450.0,
        maxTorqueNm = 2350.0,
        maxSpeedKmh = 95.0,
        fuelCapacityL = 750.0,
        weightKg = 9200.0,
        priceBrl = 790000.0,
        supportedHitches = listOf(HitchType.FIFTH_WHEEL, HitchType.DRAWBAR),
        primaryColorRgb = rgb(0.14, 0.28, 0.54),
        accentColorRgb = rgb(0.85, 0.85, 0.88),
        bodyStyle = VehicleBodyStyle.TRUCK,
        hasCab = true,
        dimensionsM = dim(8.4, 2.55, 3.5),
        turnRateDegPerSec = 38.0,
        soundProfile = "Scania DC13 6-Cilindros",
        hasGrainTank = true,
        tankCapacityL = 32000.0,
        gpsReady = true,
        description = "Cavalo mecânico de longa distância para transporte pesado de grãos e cana.",
    ),
    v(
        id = "volvo_fh540",
        brand = "Volvo",
        model = "FH 540 6x4 Florestal/Granel",
        category = VehicleCategory.CAMINHAO,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 540.0,
        maxTorqueNm = 2600.0,
        maxSpeedKmh = 95.0,
        fuelCapacityL = 810.0,
        weightKg = 9600.0,
        priceBrl = 940000.0,
        supportedHitches = listOf(HitchType.FIFTH_WHEEL, HitchType.DRAWBAR),
        primaryColorRgb = rgb(0.75, 0.78, 0.8),
        accentColorRgb = rgb(0.12, 0.16, 0.2),
        bodyStyle = VehicleBodyStyle.TRUCK,
        hasCab = true,
        dimensionsM = dim(8.6, 2.55, 3.6),
        turnRateDegPerSec = 38.0,
        soundProfile = "Volvo D13 540 Heavy",
        hasGrainTank = true,
        tankCapacityL = 36000.0,
        gpsReady = true,
        description = "Tração 6x4 extrema para escoamento em estradas de barro e madeira.",
    ),
    v(
        id = "ford_cargo2429",
        brand = "Ford",
        model = "Cargo 2429 Boiadeiro/Silagem",
        category = VehicleCategory.CAMINHAO,
        era = "Intermediário",
        modelYear = 2018,
        horsepower = 290.0,
        maxTorqueNm = 950.0,
        maxSpeedKmh = 88.0,
        fuelCapacityL = 350.0,
        weightKg = 7200.0,
        priceBrl = 360000.0,
        supportedHitches = listOf(HitchType.DRAWBAR),
        primaryColorRgb = rgb(0.88, 0.88, 0.88),
        accentColorRgb = rgb(0.12, 0.3, 0.65),
        bodyStyle = VehicleBodyStyle.TRUCK,
        hasCab = true,
        dimensionsM = dim(7.6, 2.45, 3.05),
        turnRateDegPerSec = 42.0,
        soundProfile = "Cummins ISB 6-Cilindros",
        hasGrainTank = true,
        tankCapacityL = 18000.0,
        gpsReady = true,
        description = "Transporte de animais, silagem a granel e fardos.",
    ),
    v(
        id = "pa_carregadeira",
        brand = "Case IH",
        model = "Pá Carregadeira W20",
        category = VehicleCategory.CARREGADEIRA,
        era = "Moderno",
        modelYear = 2022,
        horsepower = 142.0,
        maxTorqueNm = 560.0,
        maxSpeedKmh = 35.0,
        fuelCapacityL = 220.0,
        weightKg = 10200.0,
        priceBrl = 390000.0,
        supportedHitches = listOf(HitchType.FRONT_LOADER),
        primaryColorRgb = rgb(0.9, 0.68, 0.1),
        accentColorRgb = rgb(0.15, 0.15, 0.15),
        bodyStyle = VehicleBodyStyle.LOADER,
        hasCab = true,
        dimensionsM = dim(5.2, 2.35, 2.95),
        turnRateDegPerSec = 46.0,
        soundProfile = "Diesel Hidrostático Carregadeira",
        hasGrainTank = true,
        tankCapacityL = 2500.0,
        gpsReady = false,
        description = "Recolhimento mecanizado de pedras, compactação de silagem e carregamento de grãos.",
    ),
    v(
        id = "florestal_harvester",
        brand = "Valtra",
        model = "Harvester Florestal Eco-BR",
        category = VehicleCategory.FLORESTAL,
        era = "Moderno",
        modelYear = 2023,
        horsepower = 210.0,
        maxTorqueNm = 850.0,
        maxSpeedKmh = 25.0,
        fuelCapacityL = 390.0,
        weightKg = 13500.0,
        priceBrl = 860000.0,
        supportedHitches = listOf(HitchType.FRONT_LOADER),
        primaryColorRgb = C.VALTRA_GOLD,
        accentColorRgb = rgb(0.16, 0.22, 0.18),
        bodyStyle = VehicleBodyStyle.FORESTRY,
        hasCab = true,
        dimensionsM = dim(5.8, 2.6, 3.25),
        turnRateDegPerSec = 40.0,
        soundProfile = "AGCO Power Florestal",
        gpsReady = true,
        description = "Corte e processamento mecanizado de eucalipto, pinus e abertura autorizada de áreas.",
    ),
    // MODHUB EXPANSION NON-TRACTOR VEHICLES
    v(
        id = "case_a8810_cana_mod",
        brand = "Case IH",
        model = "Austoft A8810 Cana",
        category = VehicleCategory.COLHEITADEIRA,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 358.0,
        maxTorqueNm = 1520.0,
        maxSpeedKmh = 20.0,
        fuelCapacityL = 620.0,
        weightKg = 18500.0,
        priceBrl = 1920000.0,
        supportedHitches = listOf(HitchType.HEADER, HitchType.DRAWBAR),
        primaryColorRgb = C.CASE_RED,
        accentColorRgb = rgb(0.2, 0.2, 0.22),
        bodyStyle = VehicleBodyStyle.HARVESTER,
        wheelLayout = WheelLayout.CRAWLER_TRACKS,
        hasCab = true,
        dimensionsM = dim(8.4, 3.2, 4.1),
        turnRateDegPerSec = 30.0,
        soundProfile = "FPT Cursor 9 Canavieiro",
        hasGrainTank = true,
        tankCapacityL = 12000.0,
        gpsReady = true,
        description = "Mod Colheitadeiras Especiais: Colhedora de cana-de-açúcar com esteiras e elevador articulado.",
        requiredModPackId = "pack_colheitadeiras_agro",
        subTypeLabel = "Cana-de-açúcar",
    ),
    v(
        id = "claas_jaguar_980_mod",
        brand = "Claas",
        model = "Jaguar 980 Forrageira",
        category = VehicleCategory.COLHEITADEIRA,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 850.0,
        maxTorqueNm = 3400.0,
        maxSpeedKmh = 40.0,
        fuelCapacityL = 1350.0,
        weightKg = 15800.0,
        priceBrl = 2450000.0,
        supportedHitches = listOf(HitchType.HEADER, HitchType.DRAWBAR),
        primaryColorRgb = C.CLAAS_GREEN,
        accentColorRgb = rgb(0.82, 0.14, 0.14),
        bodyStyle = VehicleBodyStyle.HARVESTER,
        hasCab = true,
        dimensionsM = dim(7.5, 3.4, 3.9),
        turnRateDegPerSec = 35.0,
        soundProfile = "MAN V12 Forrageira Silagem",
        hasGrainTank = true,
        tankCapacityL = 16000.0,
        gpsReady = true,
        description = "Mod Colheitadeiras Especiais: Picadora autopropelida para silagem de milho em alta escala.",
        requiredModPackId = "pack_colheitadeiras_agro",
        subTypeLabel = "Forrageiras",
    ),
    v(
        id = "ford_f250_agro_mod",
        brand = "Ford",
        model = "F-250 Tropivan Agro 4x4",
        category = VehicleCategory.CAMINHONETE,
        era = "Intermediário",
        modelYear = 2011,
        horsepower = 203.0,
        maxTorqueNm = 500.0,
        maxSpeedKmh = 125.0,
        fuelCapacityL = 110.0,
        weightKg = 2850.0,
        priceBrl = 135000.0,
        supportedHitches = listOf(HitchType.DRAWBAR),
        primaryColorRgb = rgb(0.86, 0.87, 0.89),
        accentColorRgb = C.FORD_BLUE,
        bodyStyle = VehicleBodyStyle.PICKUP,
        hasCab = true,
        dimensionsM = dim(5.4, 2.05, 1.98),
        turnRateDegPerSec = 68.0,
        soundProfile = "MWM Sprint 3.9 Turbodiesel",
        hasGrainTank = true,
        tankCapacityL = 1600.0,
        gpsReady = false,
        description = "Mod Caminhonetes & Utilitários: Picape rural 4x4 robusta para estradas de terra, insumos e reboques.",
        requiredModPackId = "pack_utilitarios_campo",
        subTypeLabel = "Caminhonetes",
    ),
    v(
        id = "hilux_rural_mod",
        brand = "Toyota",
        model = "Hilux Agro Cabine Simples",
        category = VehicleCategory.CAMINHONETE,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 204.0,
        maxTorqueNm = 500.0,
        maxSpeedKmh = 130.0,
        fuelCapacityL = 80.0,
        weightKg = 2150.0,
        priceBrl = 215000.0,
        supportedHitches = listOf(HitchType.DRAWBAR),
        primaryColorRgb = rgb(0.74, 0.14, 0.14),
        accentColorRgb = rgb(0.18, 0.19, 0.21),
        bodyStyle = VehicleBodyStyle.PICKUP,
        hasCab = true,
        dimensionsM = dim(5.3, 1.9, 1.85),
        turnRateDegPerSec = 72.0,
        soundProfile = "2.8 16V Turbodiesel 4x4",
        hasGrainTank = true,
        tankCapacityL = 1200.0,
        gpsReady = true,
        description = "Mod Caminhonetes & Utilitários: Agilidade total para vistoria de talhões e transporte leve.",
        requiredModPackId = "pack_utilitarios_campo",
        subTypeLabel = "Caminhonetes",
    ),
    v(
        id = "moto_bros_agro_mod",
        brand = "Honda",
        model = "NXR 160 Agro Trilha",
        category = VehicleCategory.UTILITARIO_MOTO,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 15.0,
        maxTorqueNm = 16.0,
        maxSpeedKmh = 98.0,
        fuelCapacityL = 14.0,
        weightKg = 124.0,
        priceBrl = 21500.0,
        supportedHitches = emptyList(),
        primaryColorRgb = rgb(0.84, 0.14, 0.14),
        accentColorRgb = rgb(0.92, 0.92, 0.92),
        bodyStyle = VehicleBodyStyle.MOTO,
        hasCab = false,
        dimensionsM = dim(2.06, 0.82, 1.15),
        turnRateDegPerSec = 95.0,
        soundProfile = "Monocilíndrico Flex Trail",
        gpsReady = false,
        description = "Mod Caminhonetes & Utilitários: Motocicleta rural para inspeção rápida de cercas, rebanho e talhões.",
        requiredModPackId = "pack_utilitarios_campo",
        subTypeLabel = "Motos",
    ),
    v(
        id = "scania_r540_bitrem_mod",
        brand = "Scania",
        model = "R540 6x4 Graneleiro BR",
        category = VehicleCategory.CAMINHAO,
        era = "Moderno",
        modelYear = 2024,
        horsepower = 540.0,
        maxTorqueNm = 2700.0,
        maxSpeedKmh = 92.0,
        fuelCapacityL = 750.0,
        weightKg = 10400.0,
        priceBrl = 780000.0,
        supportedHitches = listOf(HitchType.DRAWBAR, HitchType.FIFTH_WHEEL),
        primaryColorRgb = rgb(0.14, 0.28, 0.56),
        accentColorRgb = rgb(0.88, 0.88, 0.9),
        bodyStyle = VehicleBodyStyle.TRUCK,
        hasCab = true,
        dimensionsM = dim(8.2, 2.55, 3.45),
        turnRateDegPerSec = 38.0,
        soundProfile = "Scania 13L 6-Cilindros Pesado",
        hasGrainTank = true,
        tankCapacityL = 32000.0,
        gpsReady = true,
        description = "Mod Rodotrem & Logística: Cavalo mecânico pesado para transporte regional de safras.",
        requiredModPackId = "pack_rodotrem_canavieiro",
        subTypeLabel = "Caminhões",
    ),
)

val ALL_VEHICLES: List<VehicleDef> =
    TRACTOR_CATALOG_300 + NON_TRACTOR_VEHICLES + VEHICLE_EXPANSION_CATALOG + BRAZILIAN_HISTORICAL_MACHINES

private fun imp(
    id: String,
    brand: String,
    model: String,
    category: ImplementCategory,
    hitchType: HitchType,
    requiredHp: Double,
    workingWidthM: Double,
    weightKg: Double,
    capacityL: Double,
    priceBrl: Double,
    colorRgb: RgbTriple,
    description: String,
    requiredModPackId: String? = null,
    subTypeLabel: String = "",
): ImplementDef = ImplementDef(
    id = id,
    brand = brand,
    model = model,
    category = category,
    hitchType = hitchType,
    requiredHp = requiredHp,
    workingWidthM = workingWidthM,
    weightKg = weightKg,
    capacityL = capacityL,
    priceBrl = priceBrl,
    colorRgb = colorRgb,
    description = description,
    requiredModPackId = requiredModPackId,
    subTypeLabel = subTypeLabel,
)

val ALL_IMPLEMENTS: List<ImplementDef> = listOf(
    imp("arado_4l", "Baldan", "Arado de Discos 4L", ImplementCategory.ARADO, HitchType.THREE_POINT, 45.0, 2.4, 680.0, 0.0, 18500.0, rgb(0.75, 0.16, 0.14), "Prepara solo convencional revirando terra compactada."),
    imp("grade_28", "Tatu Marchesan", "Grade Pesada 28 Discos", ImplementCategory.GRADE, HitchType.DRAWBAR, 110.0, 3.6, 2100.0, 0.0, 44000.0, rgb(0.84, 0.45, 0.1), "Destorroamento e nivelamento de talhões e abertura de área."),
    imp("subsolador_5", "Jan", "Subsolador 5 Hastes", ImplementCategory.SUBSOLADOR, HitchType.THREE_POINT, 140.0, 3.0, 1350.0, 0.0, 32000.0, rgb(0.2, 0.52, 0.24), "Descompacta camadas profundas mantendo a palhada."),
    imp("plantadeira_9l", "Semeato", "Plantadeira Direta 9L", ImplementCategory.PLANTADEIRA, HitchType.DRAWBAR, 90.0, 4.5, 3400.0, 1800.0, 92000.0, rgb(0.16, 0.5, 0.22), "Plantio direto sobre palhada de soja, milho, feijão e algodão."),
    imp("plantadeira_24l", "John Deere", "Plantadeira DB 24 Linhas", ImplementCategory.PLANTADEIRA, HitchType.DRAWBAR, 240.0, 10.8, 8200.0, 4600.0, 340000.0, C.JD_GREEN, "Plantadeira articulada de grande largura para tratores acima de 240 cv."),
    imp("semeadora_15l", "Jumil", "Semeadora Fina 15 Linhas", ImplementCategory.SEMEADORA, HitchType.THREE_POINT, 65.0, 4.2, 1900.0, 1400.0, 56000.0, rgb(0.85, 0.2, 0.16), "Semeadura de trigo, arroz, aveia, canola e pastagens."),
    imp("pulverizador_24m", "Jacto", "Pulverizador Advance 24m", ImplementCategory.PULVERIZADOR, HitchType.DRAWBAR, 75.0, 8.0, 2400.0, 3000.0, 118000.0, rgb(0.9, 0.44, 0.08), "Controle de pragas e controle automático de seções via GPS."),
    imp("distribuidor_adubo", "Stara", "Distribuidor Bruttus 6000", ImplementCategory.DISTRIBUIDOR, HitchType.DRAWBAR, 85.0, 7.5, 2200.0, 6000.0, 84000.0, rgb(0.88, 0.4, 0.08), "Aplicação de calcário, adubo e fertilizantes de precisão."),
    imp("carreta_12t", "Noma", "Carreta Graneleira 12t", ImplementCategory.CARRETA, HitchType.DRAWBAR, 65.0, 2.5, 2800.0, 15000.0, 62000.0, rgb(0.18, 0.44, 0.24), "Transporte de grãos, fertilizantes e descarga em silos."),
    imp("carreta_bazuca24t", "Jan", "Carreta Bazuca Tanker 24t", ImplementCategory.CARRETA, HitchType.DRAWBAR, 175.0, 3.1, 5600.0, 28000.0, 165000.0, rgb(0.16, 0.48, 0.22), "Transbordo graneleiro de alta capacidade com tubo de descarga rápida."),
    imp("segadora_3m", "Kuhn", "Segadora Condicionadora 3.2m", ImplementCategory.SEGADORA, HitchType.THREE_POINT, 60.0, 3.2, 980.0, 0.0, 46000.0, rgb(0.82, 0.16, 0.14), "Corte de pastagem e forragem para fenação."),
    imp("enleirador_4m", "Nogueira", "Enleirador Duplo 4.2m", ImplementCategory.ENLEIRADOR, HitchType.THREE_POINT, 50.0, 4.2, 750.0, 0.0, 34000.0, rgb(0.16, 0.46, 0.72), "Revolve e junta o feno seco em leiras prontas para enfardar."),
    imp("enfardadora_redonda", "New Holland", "Enfardadora Redonda BR7060", ImplementCategory.ENFARDADORA, HitchType.DRAWBAR, 90.0, 2.8, 2900.0, 1.0, 135000.0, rgb(0.92, 0.7, 0.1), "Produz fardos redondos de feno de alta densidade."),
    imp("enfardadora_ret", "Massey Ferguson", "Enfardadora Retangular 1840", ImplementCategory.ENFARDADORA, HitchType.DRAWBAR, 70.0, 2.4, 2100.0, 1.0, 108000.0, rgb(0.78, 0.12, 0.14), "Produz fardos retangulares práticos para transporte e venda."),
    imp("ensiladeira_jf", "JF Máquinas", "Ensiladeira Forrageira C120", ImplementCategory.ENSILADEIRA, HitchType.THREE_POINT, 75.0, 2.2, 1150.0, 4000.0, 58000.0, rgb(0.18, 0.56, 0.24), "Colhe e pica milho/sorgo para silagem a granel."),
    imp("ensacadora_silagem", "Pinheiro", "Ensacadora de Silagem Pro", ImplementCategory.ENSACADORA, HitchType.DRAWBAR, 55.0, 2.0, 1400.0, 3000.0, 49000.0, rgb(0.24, 0.48, 0.68), "Compacta e ensaca silagem em sacos de 25, 40, 50 ou 200 kg."),
    imp("arado_abertura_pro", "Tatu Marchesan", "Arado Aiveca Abertura 6L", ImplementCategory.ARADO, HitchType.DRAWBAR, 110.0, 4.4, 2400.0, 0.0, 54000.0, rgb(0.86, 0.42, 0.1), "Mod Abrir Campos: Ideal para abrir novas áreas agrícolas e expandir talhões.", "pack_abrir_campos", "Arados"),
    imp("grade_baldan_cri44", "Baldan", "Grade Niveladora CRI 44 Discos", ImplementCategory.GRADE, HitchType.DRAWBAR, 155.0, 5.2, 3400.0, 0.0, 76000.0, rgb(0.78, 0.16, 0.14), "Mod Implementos Pesados: Nivelamento largo de alta produtividade e criação de campos.", "pack_implementos_pesados", "Grades"),
    imp("subsolador_piccin_7h", "Piccin", "Subsolador Pesado 7 Hastes", ImplementCategory.SUBSOLADOR, HitchType.DRAWBAR, 195.0, 4.6, 2650.0, 0.0, 68000.0, rgb(0.82, 0.48, 0.12), "Mod Implementos Pesados: Descompactação profunda para alta produtividade em plantio direto.", "pack_implementos_pesados", "Subsoladores"),
    imp("plantadeira_stara_32l", "Stara", "Plantadeira Estrela 32 Linhas", ImplementCategory.PLANTADEIRA, HitchType.DRAWBAR, 320.0, 14.4, 11400.0, 6200.0, 520000.0, rgb(0.9, 0.42, 0.08), "Mod Plantio Precisão: Desligamento linha a linha e alta largura operacional.", "pack_plantio_precisao", "Plantadeiras"),
    imp("rocadeira_tatu_3m", "Tatu Marchesan", "Roçadeira Hidráulica RMD 3.0m", ImplementCategory.ROCADEIRA, HitchType.THREE_POINT, 65.0, 3.0, 920.0, 0.0, 28500.0, rgb(0.84, 0.45, 0.1), "Mod Abrir Campos: Limpa vegetação rasteira e prepara pasto para abertura de campo.", "pack_abrir_campos", "Roçadeiras"),
) + IMPLEMENT_EXPANSION_CATALOG

val ENGINE_MODELS: List<WorkshopOption> = listOf(
    WorkshopOption("stock", "Motor Original de Fábrica", 0.0, hpMultiplier = 1.0, fuelUseMultiplier = 1.0, weightDeltaKg = 0.0, description = "Bloco original homologado pelo fabricante."),
    WorkshopOption("mwm_agro", "MWM 229/Acteon Agro (+8% cv)", 14000.0, hpMultiplier = 1.08, fuelUseMultiplier = 0.96, weightDeltaKg = 65.0, description = "Torque em baixa rotação e menor consumo."),
    WorkshopOption("fpt_nef", "FPT NEF Common-Rail (+12% cv)", 22500.0, hpMultiplier = 1.12, fuelUseMultiplier = 0.94, weightDeltaKg = 90.0, description = "Injeção eletrônica de alta eficiência."),
    WorkshopOption("cummins_hd", "Cummins QSB Heavy Duty (+18% cv)", 34000.0, hpMultiplier = 1.18, fuelUseMultiplier = 1.04, weightDeltaKg = 145.0, description = "Força contínua para grades e subsoladores."),
)

val ENGINE_UPGRADES: List<EngineUpgradeOption> = listOf(
    EngineUpgradeOption("stock", "Mapa Original de Fábrica", 1.0, 1.0, 0.0),
    EngineUpgradeOption("stage1", "Reprogramação Agro Stage 1 (+15% cv)", 1.15, 1.05, 12000.0),
    EngineUpgradeOption("stage2", "Mapa Torque Heavy Stage 2 (+26% cv)", 1.26, 1.12, 24500.0),
    EngineUpgradeOption("stage3", "Mapa Competição Safra (+38% cv)", 1.38, 1.22, 39000.0),
)

val TURBO_OPTIONS: List<WorkshopOption> = listOf(
    WorkshopOption("stock", "Admissão Padrão", 0.0, hpMultiplier = 1.0, fuelUseMultiplier = 1.0, weightDeltaKg = 0.0),
    WorkshopOption("turbo_std", "Turbocompressor Agrícola (+10% cv)", 9800.0, hpMultiplier = 1.10, fuelUseMultiplier = 1.04, weightDeltaKg = 28.0),
    WorkshopOption("turbo_intercooler", "Turbo + Intercooler Frontal (+18% cv)", 19500.0, hpMultiplier = 1.18, fuelUseMultiplier = 1.07, weightDeltaKg = 54.0),
    WorkshopOption("biturbo_heavy", "Bi-Turbo Sequencial Heavy (+28% cv)", 36000.0, hpMultiplier = 1.28, fuelUseMultiplier = 1.16, weightDeltaKg = 92.0),
)

val WHEEL_OPTIONS: List<WheelOption> = listOf(
    WheelOption("std", "Pneu Padrão Agrícola", 1.0, 0.0, 0.0),
    WheelOption("radial", "Pneu Radial Largo Alta Flutuação", 1.16, 0.25, 14500.0),
    WheelOption("dual", "Rodado Duplo (Filipado)", 1.34, 0.45, 29000.0),
    WheelOption("arroz", "Pneu Arrozeiro Garra Alta (Lama)", 1.30, 0.2, 19000.0),
    WheelOption("florestal", "Pneu Florestal Kevlar Blindado", 1.24, 0.15, 24000.0),
)

val RIM_COLORS: List<WorkshopOption> = listOf(
    WorkshopOption("stock", "Aro Original da Marca", 0.0),
    WorkshopOption("prata", "Prata Metálico", 2200.0, colorRgb = rgb(0.80, 0.82, 0.85)),
    WorkshopOption("branco", "Branco Clássico Agro", 2400.0, colorRgb = rgb(0.94, 0.94, 0.90)),
    WorkshopOption("amarelo", "Amarelo Ouro", 2600.0, colorRgb = rgb(0.94, 0.76, 0.10)),
    WorkshopOption("preto", "Preto Fosco Industrial", 2800.0, colorRgb = rgb(0.16, 0.17, 0.19)),
    WorkshopOption("vermelho", "Vermelho Terra", 2800.0, colorRgb = rgb(0.76, 0.15, 0.14)),
)

val CAB_OPTIONS: List<WorkshopOption> = listOf(
    WorkshopOption("stock", "Configuração Original", 0.0, weightDeltaKg = 0.0),
    WorkshopOption("rops_bar", "Arco de Segurança ROPS + Toldo", 4500.0, weightDeltaKg = 85.0),
    WorkshopOption("cab_climatizada", "Cabine Climatizada Completa", 24000.0, weightDeltaKg = 260.0),
    WorkshopOption("cab_panoramica", "Cabine Panorâmica Luxo + Visão 360°", 34500.0, weightDeltaKg = 290.0),
)

val PAINT_PALETTE: List<PaintOption> = listOf(
    PaintOption("factory", "Pintura Original de Fábrica", null, 0.0),
    PaintOption("verde_jd", "Verde Safra", rgb(0.12, 0.50, 0.17), 4800.0),
    PaintOption("vermelho_mf", "Vermelho Campo", rgb(0.80, 0.11, 0.13), 4800.0),
    PaintOption("amarelo_valtra", "Amarelo Valtra Ouro", rgb(0.90, 0.65, 0.08), 4800.0),
    PaintOption("azul_nh", "Azul Metálico", rgb(0.09, 0.37, 0.80), 5200.0),
    PaintOption("preto_black", "Preto Black Edition", rgb(0.16, 0.17, 0.19), 6500.0),
    PaintOption("branco_perola", "Branco Pérola Agro", rgb(0.91, 0.91, 0.89), 5500.0),
    PaintOption("laranja_terracota", "Laranja Terracota", rgb(0.88, 0.34, 0.10), 5200.0),
)

val HEADLIGHT_OPTIONS: List<WorkshopOption> = listOf(
    WorkshopOption("stock", "Faróis Halógenos Originais", 0.0, illuminationMultiplier = 1.0),
    WorkshopOption("led_front", "Faróis Dianteiros Full LED", 3800.0, illuminationMultiplier = 1.35),
    WorkshopOption("led_workbar", "Barra LED de Teto + Trabalho Noturno", 7900.0, illuminationMultiplier = 1.75),
)

val EXHAUST_OPTIONS: List<WorkshopOption> = listOf(
    WorkshopOption("stock", "Escapamento Original de Fábrica", 0.0, hpMultiplier = 1.0),
    WorkshopOption("inox_pillar", "Ponteira Inox na Coluna", 3200.0, hpMultiplier = 1.02, exhaustStyle = ExhaustStyle.RIGHT_PILLAR),
    WorkshopOption("direto_hood", "Escape Direto no Capô (Ronco Agro)", 4400.0, hpMultiplier = 1.04, exhaustStyle = ExhaustStyle.CENTER_HOOD),
    WorkshopOption("duplo_cromado", "Escapamento Duplo Cromado", 6900.0, hpMultiplier = 1.05, exhaustStyle = ExhaustStyle.DUAL_STACK),
)

val ACCESSORY_OPTIONS: List<WorkshopOption> = listOf(
    WorkshopOption("stock", "Sem Acessórios Extras", 0.0, weightDeltaKg = 0.0, tractionBonus = 1.0),
    WorkshopOption("front_weight", "Kit Contrapesos Dianteiros (+480 kg)", 5600.0, weightDeltaKg = 480.0, tractionBonus = 1.07),
    WorkshopOption("beacon_weight", "Giroflex Âmbar + Contrapesos (+480 kg)", 7400.0, weightDeltaKg = 480.0, tractionBonus = 1.07),
    WorkshopOption("gps_rtk_full", "Antena GPS RTK + Giroflex + Contrapesos", 15800.0, weightDeltaKg = 510.0, tractionBonus = 1.09),
)

data class ResolvedVehicleTuning(
    val effectiveHp: Double,
    val fuelMultiplier: Double,
    val tractionMultiplier: Double,
    val extraWeightKg: Double,
    val illuminationMultiplier: Double,
    val compactionReduction: Double,
    val hasDualRears: Boolean,
    val hasExtraFrontWeights: Boolean,
    val hasBeacon: Boolean,
    val hasGpsDome: Boolean,
    val hasLedBar: Boolean,
    val exhaustStyle: ExhaustStyle,
    val rimRgb: RgbTriple,
)

fun resolveVehicleTuning(veh: OwnedVehicle, def: VehicleDef): ResolvedVehicleTuning {
    val engineM = ENGINE_MODELS.find { it.id == (veh.engineModelId ?: "stock") } ?: ENGINE_MODELS[0]
    val remap = ENGINE_UPGRADES.find { it.id == veh.engineUpgradeId } ?: ENGINE_UPGRADES[0]
    val turbo = TURBO_OPTIONS.find { it.id == (veh.turboId ?: "stock") } ?: TURBO_OPTIONS[0]
    val wheel = WHEEL_OPTIONS.find { it.id == veh.wheelOptionId } ?: WHEEL_OPTIONS[0]
    val rim = RIM_COLORS.find { it.id == (veh.rimColorId ?: "stock") } ?: RIM_COLORS[0]
    val cab = CAB_OPTIONS.find { it.id == (veh.cabStyleId ?: "stock") } ?: CAB_OPTIONS[0]
    val lights = HEADLIGHT_OPTIONS.find { it.id == (veh.headlightStyleId ?: "stock") } ?: HEADLIGHT_OPTIONS[0]
    val exhaust = EXHAUST_OPTIONS.find { it.id == (veh.exhaustStyleId ?: "stock") } ?: EXHAUST_OPTIONS[0]
    val acc = ACCESSORY_OPTIONS.find { it.id == (veh.accessoryId ?: "stock") } ?: ACCESSORY_OPTIONS[0]

    val hpMul = (engineM.hpMultiplier ?: 1.0) * remap.hpMultiplier * (turbo.hpMultiplier ?: 1.0) * (exhaust.hpMultiplier ?: 1.0)
    val fuelMul = (engineM.fuelUseMultiplier ?: 1.0) * remap.fuelUseMultiplier * (turbo.fuelUseMultiplier ?: 1.0)
    val customPaint = veh.customPaintRgb?.takeIf { it.size >= 3 }?.let { rgb(it[0], it[1], it[2]) }
    val brandPalette = resolveBrandVisualPalette(def, customPaint, veh.rimColorId)

    return ResolvedVehicleTuning(
        effectiveHp = (def.horsepower * hpMul).roundToInt().toDouble(),
        fuelMultiplier = fuelMul,
        tractionMultiplier = wheel.tractionBonus * (acc.tractionBonus ?: 1.0),
        extraWeightKg = (engineM.weightDeltaKg ?: 0.0) + (turbo.weightDeltaKg ?: 0.0) + (cab.weightDeltaKg ?: 0.0) + (acc.weightDeltaKg ?: 0.0),
        illuminationMultiplier = lights.illuminationMultiplier ?: 1.0,
        compactionReduction = wheel.compactionPenaltyReduction,
        hasDualRears = veh.wheelOptionId == "dual" || def.wheelLayout == WheelLayout.DUAL_REAR,
        hasExtraFrontWeights = acc.id != "stock",
        hasBeacon = acc.id == "beacon_weight" || acc.id == "gps_rtk_full",
        hasGpsDome = acc.id == "gps_rtk_full" || def.gpsReady,
        hasLedBar = lights.id == "led_workbar",
        exhaustStyle = exhaust.exhaustStyle ?: def.exhaustStyle ?: ExhaustStyle.RIGHT_PILLAR,
        rimRgb = rim.colorRgb ?: brandPalette.rimRgb,
    )
}

fun getAvailableVehicles(installedPackIds: Set<String>): List<VehicleDef> =
    ALL_VEHICLES.filter { it.requiredModPackId == null || installedPackIds.contains(it.requiredModPackId) }

fun getAvailableImplements(installedPackIds: Set<String>): List<ImplementDef> =
    ALL_IMPLEMENTS.filter { it.requiredModPackId == null || installedPackIds.contains(it.requiredModPackId) }

fun getVehicleDef(id: String): VehicleDef =
    ALL_VEHICLES.find { it.id == id } ?: ALL_VEHICLES[0]

fun getImplementDef(id: String): ImplementDef =
    ALL_IMPLEMENTS.find { it.id == id } ?: ALL_IMPLEMENTS[0]

private fun clamp(v: Double, minVal: Double, maxVal: Double): Double = v.coerceIn(minVal, maxVal)

data class HarvesterChassisDimensions(
    val chassisWidthM: Double,
    val headerWidthM: Double,
    val isCanaHarvester: Boolean,
    val isForageHarvester: Boolean,
)

fun getHarvesterChassisDimensions(def: VehicleDef): HarvesterChassisDimensions {
    val wid = def.dimensionsM[1]
    val len = def.dimensionsM[0]
    val isCanaHarvester = def.id.contains("cana") || def.subTypeLabel.lowercase().contains("cana")
    val isForageHarvester = def.id.contains("jaguar") || def.subTypeLabel.lowercase().contains("forrag")
    val rawHeaderM = when {
        def.headerWidthFt != null && def.headerWidthFt > 0 -> def.headerWidthFt * 0.3048
        isCanaHarvester -> 2.2
        isForageHarvester -> 4.6
        else -> max(3.6, wid)
    }
    val isTracked = def.wheelLayout == WheelLayout.CRAWLER_TRACKS
    val isDual = def.wheelLayout == WheelLayout.DUAL_REAR
    val chassisWidthM = when {
        isCanaHarvester -> clamp(wid * 0.92, 2.75, 3.15)
        isForageHarvester -> clamp(wid * 0.88, 2.85, 3.3)
        else -> {
            val baseHull = clamp(2.58 + (max(len, 6.0) - 6.0) * 0.22, 2.58, 3.35)
            clamp(baseHull + (if (isDual) 0.32 else if (isTracked) 0.2 else 0.0), 2.58, 3.65)
        }
    }
    return HarvesterChassisDimensions(
        chassisWidthM = chassisWidthM,
        headerWidthM = clamp(rawHeaderM, chassisWidthM + 0.5, 11.2),
        isCanaHarvester = isCanaHarvester,
        isForageHarvester = isForageHarvester,
    )
}

fun getVehiclePhysicsSpec(def: VehicleDef): VehiclePhysicsSpec {
    val len = def.dimensionsM[0]
    val wid = def.dimensionsM[1]
    val hgt = def.dimensionsM[2]
    val isCana = def.id.contains("cana") || def.subTypeLabel.lowercase().contains("cana")
    val isForage = def.id.contains("jaguar") || def.subTypeLabel.lowercase().contains("forrag")
    val isTrack = def.wheelLayout == WheelLayout.CRAWLER_TRACKS
    val isClassicSlc = (def.visualSilhouette ?: "").startsWith("slc_") || def.era == "Clássico"

    val driveType = when {
        isTrack || isCana -> DriveType.TRACKED
        def.bodyStyle == VehicleBodyStyle.CLASSIC_2WD ||
            def.id.contains("1090") ||
            def.id.contains("mf_235") ||
            def.id.contains("65id") ||
            def.category == VehicleCategory.UTILITARIO_MOTO ||
            (def.category == VehicleCategory.CAMINHAO && !def.id.contains("6x4") && !def.id.contains("fh540")) -> DriveType.RWD
        else -> DriveType.AWD
    }

    val tireCategory = when {
        isTrack || isCana -> TireCategory.TRACKS
        def.category == VehicleCategory.CAMINHAO -> TireCategory.HIGHWAY
        def.category == VehicleCategory.FLORESTAL -> TireCategory.FORESTRY
        else -> TireCategory.AGRICULTURAL
    }

    return when (def.bodyStyle) {
        VehicleBodyStyle.COMPACT_OPEN, VehicleBodyStyle.CLASSIC_2WD -> {
            val rearR = clamp(hgt * 0.3, 0.56, 0.84)
            val frontR = clamp(rearR * 0.64, 0.36, 0.54)
            val fZ = len * 0.31
            val rZ = -len * 0.24
            VehiclePhysicsSpec(
                driveType = driveType, tireCategory = tireCategory, maxSteerAngleDeg = 36.0, steerResponseRateDegPerSec = 95.0,
                wheelbaseM = max(fZ - rZ, 1.4), trackWidthM = wid * 0.82, frontWheelRadiusM = frontR, rearWheelRadiusM = rearR,
                frontAxleZ = fZ, rearAxleZ = rZ, rearSteerFactor = 0.0, isTrackedFront = false, isTrackedRear = false,
                suspensionStiffness = 44.0, suspensionDamping = 9.0, suspensionMaxTravelM = 0.14, cogHeightM = hgt * 0.44,
            )
        }
        VehicleBodyStyle.STANDARD_CAB, VehicleBodyStyle.HIGH_POWER_CAB -> {
            val isHigh = def.bodyStyle == VehicleBodyStyle.HIGH_POWER_CAB
            val hasDuals = def.wheelLayout == WheelLayout.DUAL_REAR
            val bodyW = if (hasDuals) wid * 0.82 else wid
            val rearR = clamp(hgt * (if (isHigh) 0.29 else 0.28), 0.64, 0.98)
            val frontR = clamp(rearR * 0.72, 0.46, 0.72)
            val fZ = len * 0.3
            val rZ = -len * 0.25
            VehiclePhysicsSpec(
                driveType = driveType, tireCategory = tireCategory,
                maxSteerAngleDeg = if (isHigh) 30.0 else 33.0, steerResponseRateDegPerSec = if (isHigh) 74.0 else 84.0,
                wheelbaseM = max(fZ - rZ, 2.0), trackWidthM = if (hasDuals) bodyW * 0.84 else wid * 0.82,
                frontWheelRadiusM = frontR, rearWheelRadiusM = rearR, frontAxleZ = fZ, rearAxleZ = rZ,
                rearSteerFactor = 0.0, isTrackedFront = false, isTrackedRear = false,
                suspensionStiffness = if (isHigh) 36.0 else 40.0, suspensionDamping = 8.4, suspensionMaxTravelM = 0.16, cogHeightM = hgt * 0.42,
            )
        }
        VehicleBodyStyle.ARTICULATED_4WD, VehicleBodyStyle.QUAD_TRACK -> {
            val fZ = len * 0.25
            val rZ = -len * 0.25
            VehiclePhysicsSpec(
                driveType = if (isTrack) DriveType.TRACKED else DriveType.AWD,
                tireCategory = if (isTrack) TireCategory.TRACKS else TireCategory.AGRICULTURAL,
                maxSteerAngleDeg = 27.0, steerResponseRateDegPerSec = 58.0, wheelbaseM = max(fZ - rZ, 2.8),
                trackWidthM = wid * 0.84, frontWheelRadiusM = 0.86, rearWheelRadiusM = 0.86,
                frontAxleZ = fZ, rearAxleZ = rZ, rearSteerFactor = -0.45,
                isTrackedFront = isTrack, isTrackedRear = isTrack,
                suspensionStiffness = 32.0, suspensionDamping = 9.5, suspensionMaxTravelM = 0.14, cogHeightM = hgt * 0.45,
            )
        }
        VehicleBodyStyle.HARVESTER -> {
            val harvDims = getHarvesterChassisDimensions(def)
            if (isCana) {
                val fZ = len * 0.16
                val rZ = -len * 0.18
                VehiclePhysicsSpec(
                    driveType = DriveType.TRACKED, tireCategory = TireCategory.TRACKS,
                    maxSteerAngleDeg = 24.0, steerResponseRateDegPerSec = 52.0, wheelbaseM = max(fZ - rZ, 2.4),
                    trackWidthM = harvDims.chassisWidthM * 0.78, frontWheelRadiusM = 0.56, rearWheelRadiusM = 0.56,
                    frontAxleZ = fZ, rearAxleZ = rZ, rearSteerFactor = 0.0, isTrackedFront = true, isTrackedRear = true,
                    suspensionStiffness = 34.0, suspensionDamping = 10.0, suspensionMaxTravelM = 0.14, cogHeightM = hgt * 0.48,
                )
            } else {
                val fZ = len * 0.17
                val rZ = if (isForage) -len * 0.3 else -len * 0.31
                val frontR = if (isForage) 0.82 else if (isClassicSlc) 0.76 else 0.84
                val rearR = if (isClassicSlc) 0.5 else 0.56
                VehiclePhysicsSpec(
                    driveType = if (isTrack) DriveType.TRACKED else DriveType.FWD,
                    tireCategory = if (isTrack) TireCategory.TRACKS else TireCategory.AGRICULTURAL,
                    maxSteerAngleDeg = 26.0, steerResponseRateDegPerSec = 60.0, wheelbaseM = max(fZ - rZ, 2.5),
                    trackWidthM = harvDims.chassisWidthM * 0.86, frontWheelRadiusM = frontR, rearWheelRadiusM = rearR,
                    frontAxleZ = fZ, rearAxleZ = rZ, rearSteerFactor = 1.0, isTrackedFront = isTrack, isTrackedRear = false,
                    suspensionStiffness = 32.0, suspensionDamping = 9.2, suspensionMaxTravelM = 0.16, cogHeightM = hgt * 0.47,
                )
            }
        }
        VehicleBodyStyle.TRUCK -> VehiclePhysicsSpec(
            driveType = driveType, tireCategory = TireCategory.HIGHWAY, maxSteerAngleDeg = 31.0, steerResponseRateDegPerSec = 82.0,
            wheelbaseM = len * 0.54, trackWidthM = wid * 0.88, frontWheelRadiusM = 0.52, rearWheelRadiusM = 0.52,
            frontAxleZ = len * 0.31, rearAxleZ = -len * 0.25, rearSteerFactor = 0.0, isTrackedFront = false, isTrackedRear = false,
            suspensionStiffness = 34.0, suspensionDamping = 7.4, suspensionMaxTravelM = 0.18, cogHeightM = hgt * 0.48,
        )
        VehicleBodyStyle.PICKUP -> VehiclePhysicsSpec(
            driveType = DriveType.AWD, tireCategory = TireCategory.HIGHWAY, maxSteerAngleDeg = 35.0, steerResponseRateDegPerSec = 105.0,
            wheelbaseM = len * 0.54, trackWidthM = wid * 0.86, frontWheelRadiusM = 0.42, rearWheelRadiusM = 0.42,
            frontAxleZ = len * 0.28, rearAxleZ = -len * 0.26, rearSteerFactor = 0.0, isTrackedFront = false, isTrackedRear = false,
            suspensionStiffness = 42.0, suspensionDamping = 7.6, suspensionMaxTravelM = 0.2, cogHeightM = hgt * 0.4,
        )
        VehicleBodyStyle.MOTO -> VehiclePhysicsSpec(
            driveType = DriveType.RWD, tireCategory = TireCategory.AGRICULTURAL, maxSteerAngleDeg = 40.0, steerResponseRateDegPerSec = 135.0,
            wheelbaseM = len * 0.72, trackWidthM = 0.2, frontWheelRadiusM = 0.36, rearWheelRadiusM = 0.34,
            frontAxleZ = len * 0.36, rearAxleZ = -len * 0.36, rearSteerFactor = 0.0, isTrackedFront = false, isTrackedRear = false,
            suspensionStiffness = 52.0, suspensionDamping = 7.0, suspensionMaxTravelM = 0.22, cogHeightM = hgt * 0.45,
        )
        VehicleBodyStyle.LOADER, VehicleBodyStyle.FORESTRY, VehicleBodyStyle.BACKHOE, VehicleBodyStyle.TELEHANDLER -> {
            val r = when (def.bodyStyle) {
                VehicleBodyStyle.LOADER, VehicleBodyStyle.BACKHOE -> 0.72
                VehicleBodyStyle.TELEHANDLER -> 0.62
                else -> 0.68
            }
            VehiclePhysicsSpec(
                driveType = DriveType.AWD,
                tireCategory = if (def.bodyStyle == VehicleBodyStyle.FORESTRY) TireCategory.FORESTRY else TireCategory.AGRICULTURAL,
                maxSteerAngleDeg = if (def.bodyStyle == VehicleBodyStyle.TELEHANDLER) 34.0 else 29.0,
                steerResponseRateDegPerSec = if (def.bodyStyle == VehicleBodyStyle.TELEHANDLER) 84.0 else 66.0,
                wheelbaseM = len * 0.44, trackWidthM = wid * 0.88,
                frontWheelRadiusM = if (def.bodyStyle == VehicleBodyStyle.BACKHOE) r * 0.72 else r,
                rearWheelRadiusM = r, frontAxleZ = len * 0.2, rearAxleZ = -len * 0.23,
                rearSteerFactor = if (def.bodyStyle == VehicleBodyStyle.TELEHANDLER) -0.65 else -0.35,
                isTrackedFront = false, isTrackedRear = false,
                suspensionStiffness = 38.0, suspensionDamping = 9.0, suspensionMaxTravelM = 0.14, cogHeightM = hgt * 0.44,
            )
        }
        VehicleBodyStyle.EXCAVATOR, VehicleBodyStyle.DOZER -> VehiclePhysicsSpec(
            driveType = DriveType.TRACKED, tireCategory = TireCategory.TRACKS, maxSteerAngleDeg = 32.0, steerResponseRateDegPerSec = 90.0,
            wheelbaseM = len * 0.52, trackWidthM = wid * 0.84, frontWheelRadiusM = 0.56, rearWheelRadiusM = 0.56,
            frontAxleZ = len * 0.26, rearAxleZ = -len * 0.26, rearSteerFactor = 0.0, isTrackedFront = true, isTrackedRear = true,
            suspensionStiffness = 46.0, suspensionDamping = 11.0, suspensionMaxTravelM = 0.08, cogHeightM = hgt * 0.42,
        )
        VehicleBodyStyle.GRADER -> VehiclePhysicsSpec(
            driveType = DriveType.RWD, tireCategory = TireCategory.AGRICULTURAL, maxSteerAngleDeg = 34.0, steerResponseRateDegPerSec = 72.0,
            wheelbaseM = len * 0.62, trackWidthM = wid * 0.86, frontWheelRadiusM = 0.62, rearWheelRadiusM = 0.64,
            frontAxleZ = len * 0.34, rearAxleZ = -len * 0.28, rearSteerFactor = -0.22, isTrackedFront = false, isTrackedRear = false,
            suspensionStiffness = 40.0, suspensionDamping = 9.2, suspensionMaxTravelM = 0.12, cogHeightM = hgt * 0.4,
        )
        VehicleBodyStyle.SELF_PROPELLED_SPRAYER -> VehiclePhysicsSpec(
            driveType = DriveType.AWD, tireCategory = TireCategory.AGRICULTURAL, maxSteerAngleDeg = 35.0, steerResponseRateDegPerSec = 88.0,
            wheelbaseM = len * 0.52, trackWidthM = wid * 0.88, frontWheelRadiusM = 0.78, rearWheelRadiusM = 0.78,
            frontAxleZ = len * 0.27, rearAxleZ = -len * 0.25, rearSteerFactor = -0.75, isTrackedFront = false, isTrackedRear = false,
            suspensionStiffness = 30.0, suspensionDamping = 8.8, suspensionMaxTravelM = 0.2, cogHeightM = hgt * 0.52,
        )
        VehicleBodyStyle.FORKLIFT, VehicleBodyStyle.SKID_STEER -> VehiclePhysicsSpec(
            driveType = DriveType.AWD,
            tireCategory = if (def.bodyStyle == VehicleBodyStyle.FORKLIFT) TireCategory.HIGHWAY else TireCategory.AGRICULTURAL,
            maxSteerAngleDeg = 40.0, steerResponseRateDegPerSec = 115.0, wheelbaseM = len * 0.52, trackWidthM = wid * 0.82,
            frontWheelRadiusM = 0.42, rearWheelRadiusM = if (def.bodyStyle == VehicleBodyStyle.FORKLIFT) 0.34 else 0.42,
            frontAxleZ = len * 0.26, rearAxleZ = -len * 0.26,
            rearSteerFactor = if (def.bodyStyle == VehicleBodyStyle.FORKLIFT) 1.0 else -0.4,
            isTrackedFront = false, isTrackedRear = false,
            suspensionStiffness = 48.0, suspensionDamping = 9.5, suspensionMaxTravelM = 0.1, cogHeightM = hgt * 0.4,
        )
    }
}

fun getImplementHitchOffset(def: ImplementDef): Double = when (def.category) {
    ImplementCategory.CARRETA -> if (def.hitchType == HitchType.FIFTH_WHEEL) 3.8 else 3.05
    ImplementCategory.PULVERIZADOR, ImplementCategory.DISTRIBUIDOR, ImplementCategory.ENFARDADORA -> 2.35
    ImplementCategory.GRADE, ImplementCategory.PLANTADEIRA, ImplementCategory.ESCARIFICADOR -> 1.75
    ImplementCategory.CULTIVADOR -> 1.60
    ImplementCategory.PLATAFORMA_MILHO -> 1.15
    else -> 1.15
}
