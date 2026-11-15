package com.example.domain

fun worldCoordScaleForGrid(gridSide: Int): Double {
    val cellSizeM = when (gridSide) {
        64 -> 4
        96 -> 6
        128 -> 6
        160 -> 6
        192 -> 8
        else -> CELL_SIZE_M
    }
    return (gridSide * cellSizeM).toDouble() / (GRID_SIZE * CELL_SIZE_M).toDouble()
}

fun scaledPoiCoord(rawCoord: Double, gridSide: Int): Double =
    rawCoord * worldCoordScaleForGrid(gridSide)

data class EstablishmentDef(
    val id: String,
    val name: String,
    val type: String,
    val acceptedProductIds: List<String>,
    val priceMultiplier: Double,
    val capacityTons: Double,
    val openHour: Int,
    val closeHour: Int,
    val hasWeighbridge: Boolean,
    val worldX: Double,
    val worldZ: Double,
) {
    fun scaledWorldX(gridSide: Int): Double = scaledPoiCoord(worldX, gridSide)
    fun scaledWorldZ(gridSide: Int): Double = scaledPoiCoord(worldZ, gridSide)
}

data class MapLandmark(
    val id: String,
    val name: String,
    val category: String,
    val worldX: Double,
    val worldZ: Double,
    val description: String,
    val scalesWithMap: Boolean = true,
) {
    fun scaledWorldX(gridSide: Int): Double = if (scalesWithMap) scaledPoiCoord(worldX, gridSide) else worldX
    fun scaledWorldZ(gridSide: Int): Double = if (scalesWithMap) scaledPoiCoord(worldZ, gridSide) else worldZ
}

data class IndustryOutputDef(
    val name: String,
    val kg: Double,
)

data class IndustryRecipeDef(
    val id: String,
    val industryName: String,
    val inputProductId: String,
    val inputAmountKg: Double,
    val outputs: List<IndustryOutputDef>,
    val outputPricesPerKg: Map<String, Double>,
    val processingHours: Double,
    val energyCostPerBatchBrl: Double,
    val employeeDailyWageBrl: Double,
    val plantCostBrl: Double,
)

data class FactoryStageDef(
    val stage: Int,
    val name: String,
    val description: String,
    val upgradeCostMultiplier: Double,
    val speedMultiplier: Double,
    val yieldBonusMultiplier: Double,
)

data class FactoryEquipmentDef(
    val id: String,
    val name: String,
    val priceBrl: Double,
    val speedBonus: Double,
    val yieldBonus: Double,
    val energyDiscount: Double,
    val description: String,
)

val LANDMARKS: List<MapLandmark> = listOf(
    MapLandmark("lm_garagem", "Garagem Central & Oficina", "Garagem", 15.0, 29.0, "Local de entrega de máquinas compradas, manutenção e pintura.", scalesWithMap = false),
    MapLandmark("lm_silo_central", "Silo & Armazém da Sede", "Silo", -18.0, 39.0, "Armazenagem principal de grãos da fazenda.", scalesWithMap = false),
    MapLandmark("lm_concessionaria", "Concessionária Nacional Agro", "Concessionária", -155.0, -72.0, "Showroom e venda de tratores, colheitadeiras, caminhões e implementos."),
    MapLandmark("lm_loja_insumos", "Loja Agropecuária & Insumos", "Loja Agro", 155.0, -72.0, "Sementes certificadas, adubo NPK, calcário, defensivos e implementos."),
    MapLandmark("lm_banco", "Banco Rural Brasil", "Banco", 112.0, -88.0, "Depósitos, saques, crédito rural e gestão de financiamentos."),
    MapLandmark("lm_oficina_vila", "Oficina Mecânica Central", "Oficina", 112.0, -108.0, "Elevador hidráulico, reparos completos, revisão preventiva e pintura."),
    MapLandmark("lm_mercado_rural", "Mercado & Empório Rural", "Mercado", 152.0, -88.0, "Compra e venda direta de hortaliças, flores, leite, queijo e ovos."),
    MapLandmark("lm_armazem_vila", "Armazém Geral Regional", "Armazém", 172.0, -108.0, "Docas de carga e armazenagem regional de sacarias e fardos."),
    MapLandmark("lm_posto_vila", "Posto de Combustível Diesel S10", "Posto", 192.0, -88.0, "Cobertura com bombas de diesel S10 e loja de conveniência."),
    MapLandmark("lm_madeireira", "Madeireira Brasil", "Madeireira", -132.0, -88.0, "Compra de toras e venda de madeira serrada para construções."),
    MapLandmark("lm_materiais", "Loja de Materiais de Construção", "Materiais", -172.0, -88.0, "Cimento, tijolos, telhas e kits para galpões e casas."),
    MapLandmark("lm_ponto_animais", "Feira & Ponto de Venda de Animais", "Pecuária", -152.0, -108.0, "Negociação e embarque de bovinos, leiteiras, suínos e aves."),
)

val ESTABLISHMENTS: List<EstablishmentDef> = listOf(
    EstablishmentDef(
        id = "coop_central", name = "Cooperativa Agroindustrial Brasil", type = "Cooperativa",
        acceptedProductIds = listOf("soja", "milho", "trigo", "feijao_carioca", "feijao_preto", "arroz", "cafe", "algodao", "feno_fardo", "silagem_granel", "silagem_ensacada"),
        priceMultiplier = 1.04, capacityTons = 5000.0, openHour = 6, closeHour = 21, hasWeighbridge = true, worldX = -158.0, worldZ = -168.0,
    ),
    EstablishmentDef(
        id = "cerealista_sul", name = "Cerealista & Silos Grão Forte", type = "Cerealista",
        acceptedProductIds = listOf("soja", "milho", "sorgo", "girassol", "canola", "amendoim", "gergelim", "feijao_caupi"),
        priceMultiplier = 1.08, capacityTons = 3200.0, openHour = 7, closeHour = 20, hasWeighbridge = true, worldX = 168.0, worldZ = -162.0,
    ),
    EstablishmentDef(
        id = "usina_bio", name = "Usina Sucroalcooleira & Bioenergia", type = "Usina",
        acceptedProductIds = listOf("cana", "milho", "mamona", "eucalipto", "pinus"),
        priceMultiplier = 1.12, capacityTons = 12000.0, openHour = 0, closeHour = 24, hasWeighbridge = true, worldX = 176.0, worldZ = 152.0,
    ),
    EstablishmentDef(
        id = "ceasa_horti", name = "Distribuidora Frutas & Hortaliças", type = "Distribuidor",
        acceptedProductIds = listOf("laranja", "limao", "banana", "uva", "maca", "cacau", "acai", "manga", "mamao", "abacaxi", "coco", "batata", "tomate", "cebola", "alho", "cenoura", "alface", "melancia", "melao", "abobora", "mandioca", "rosas_familiares", "morango_familiar", "leite", "ovos"),
        priceMultiplier = 1.10, capacityTons = 1800.0, openHour = 5, closeHour = 19, hasWeighbridge = true, worldX = -164.0, worldZ = 158.0,
    ),
)

val INDUSTRY_CHAINS: List<IndustryRecipeDef> = listOf(
    IndustryRecipeDef("ind_oleo_soja", "Esmagadora de Óleos Vegetais (Soja/Girassol/Canola)", "soja", 1000.0, listOf(IndustryOutputDef("Óleo Vegetal Refinado", 200.0), IndustryOutputDef("Farelo Proteico", 760.0)), mapOf("Óleo Vegetal Refinado" to 7.8, "Farelo Proteico" to 2.35), 4.0, 145.0, 240.0, 240000.0),
    IndustryRecipeDef("ind_cana", "Usina de Cana (Açúcar, Etanol e Bagaço)", "cana", 2000.0, listOf(IndustryOutputDef("Açúcar Cristal", 240.0), IndustryOutputDef("Etanol Hidratado (L)", 170.0), IndustryOutputDef("Bagaço Energético", 520.0)), mapOf("Açúcar Cristal" to 3.4, "Etanol Hidratado (L)" to 4.15, "Bagaço Energético" to 0.35), 5.0, 180.0, 310.0, 390000.0),
    IndustryRecipeDef("ind_milho", "Indústria de Milho (Fubá, Amido e Ração)", "milho", 1000.0, listOf(IndustryOutputDef("Fubá / Farinha de Milho", 520.0), IndustryOutputDef("Amido de Milho", 180.0), IndustryOutputDef("Ração Balanceada", 260.0)), mapOf("Fubá / Farinha de Milho" to 3.6, "Amido de Milho" to 5.4, "Ração Balanceada" to 2.1), 3.5, 120.0, 220.0, 195000.0),
    IndustryRecipeDef("ind_trigo", "Moinho de Trigo & Panificação", "trigo", 1000.0, listOf(IndustryOutputDef("Farinha de Trigo Tipo 1", 750.0), IndustryOutputDef("Massas e Farelo", 220.0)), mapOf("Farinha de Trigo Tipo 1" to 4.2, "Massas e Farelo" to 4.9), 4.0, 135.0, 230.0, 215000.0),
    IndustryRecipeDef("ind_arroz", "Beneficiadora de Arroz", "arroz", 1000.0, listOf(IndustryOutputDef("Arroz Branco Beneficiado", 680.0), IndustryOutputDef("Farelo de Arroz", 120.0)), mapOf("Arroz Branco Beneficiado" to 5.1, "Farelo de Arroz" to 1.6), 3.0, 95.0, 190.0, 165000.0),
    IndustryRecipeDef("ind_feijao", "Beneficiadora & Empacotadora de Feijão", "feijao_carioca", 1000.0, listOf(IndustryOutputDef("Feijão Selecionado Embalado", 940.0)), mapOf("Feijão Selecionado Embalado" to 6.9), 2.5, 85.0, 180.0, 140000.0),
    IndustryRecipeDef("ind_frutas_cafe", "Indústria de Sucos & Torrefação de Café", "cafe", 600.0, listOf(IndustryOutputDef("Café Torrado e Moído", 480.0)), mapOf("Café Torrado e Moído" to 44.0), 4.0, 140.0, 240.0, 225000.0),
    IndustryRecipeDef("ind_mandioca", "Fecularia de Mandioca (Farinha e Polvilho)", "mandioca", 1500.0, listOf(IndustryOutputDef("Farinha de Mandioca", 420.0), IndustryOutputDef("Polvilho / Fécula", 310.0)), mapOf("Farinha de Mandioca" to 5.6, "Polvilho / Fécula" to 7.4), 3.5, 110.0, 200.0, 155000.0),
    IndustryRecipeDef("ind_floricultura_rosas", "Casa de Beneficiamento de Rosas & Essências", "rosas_familiares", 200.0, listOf(IndustryOutputDef("Buquês Selecionados", 150.0), IndustryOutputDef("Óleo Essencial de Rosas", 18.0)), mapOf("Buquês Selecionados" to 24.0, "Óleo Essencial de Rosas" to 92.0), 2.5, 75.0, 165.0, 98000.0),
    IndustryRecipeDef("ind_laticinios", "Laticínio Modular (Queijos, Manteiga & Iogurte)", "leite", 800.0, listOf(IndustryOutputDef("Queijo Minas Artesanal", 110.0), IndustryOutputDef("Manteiga & Creme Pasteurizado", 65.0)), mapOf("Queijo Minas Artesanal" to 38.0, "Manteiga & Creme Pasteurizado" to 46.0), 3.0, 115.0, 210.0, 145000.0),
    IndustryRecipeDef("ind_racao", "Fábrica de Ração Balanceada Agro", "milho", 1200.0, listOf(IndustryOutputDef("Ração Bovina Confinamento", 720.0), IndustryOutputDef("Ração Suína & Aves Premium", 440.0)), mapOf("Ração Bovina Confinamento" to 3.1, "Ração Suína & Aves Premium" to 3.65), 3.0, 105.0, 195.0, 135000.0),
)

val FACTORY_STAGE_DEFS: List<FactoryStageDef> = listOf(
    FactoryStageDef(1, "Estágio 1 — Fundação & Estrutura Simples", "Terraplanagem do terreno, base de concreto, cobertura metálica e recepção básica de carga.", 0.35, 1.0, 1.0),
    FactoryStageDef(2, "Estágio 2 — Linha de Produção Operacional", "Instalação de moegas, elevadores de caneca, esteiras de processo e painel elétrico trifásico.", 0.45, 1.45, 1.12),
    FactoryStageDef(3, "Estágio 3 — Planta Industrial Automatizada", "Silos pulmão integrados, ensaque automático, laboratório de qualidade e expedição em paletes.", 0.65, 2.1, 1.28),
)

val FACTORY_EQUIPMENT_DEFS: List<FactoryEquipmentDef> = listOf(
    FactoryEquipmentDef("eq_prensa_inox", "Prensa Extratora / Pasteurizador Inox", 42000.0, 0.25, 0.1, 0.05, "Aumenta extração em óleo vegetal, sucos e pasteurização de laticínios."),
    FactoryEquipmentDef("eq_moinho_martelos", "Moinho de Martelos & Misturador Horizontal", 38000.0, 0.3, 0.08, 0.04, "Acelera moagem de milho, trigo e peletização de ração balanceada."),
    FactoryEquipmentDef("eq_caldeira_biomassa", "Caldeira a Biomassa de Alta Eficiência", 54000.0, 0.15, 0.05, 0.28, "Reduz em 28% o gasto elétrico por lote processado."),
    FactoryEquipmentDef("eq_ensacadeira_robot", "Ensacadeira & Paletizadora Automática", 64000.0, 0.35, 0.12, 0.06, "Elimina perdas de empacotamento e turbina a saída industrial."),
)
