package com.example.domain

import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
enum class ConstructionCategory(
    val label: String,
    val description: String,
) {
    CASAS("Casas", "Residências rurais de madeira, alvenaria e sedes regionais"),
    GALPOES("Galpões", "Barracões e galpões de máquinas abertos ou fechados"),
    GARAGENS("Garagens", "Garagens cobertas para caminhonetes, carros e tratores"),
    CELEIROS("Celeiros", "Celeiros tradicionais de madeira ou alvenaria para feno e grãos"),
    SILOS("Silos", "Silos graneleiros e secadores de alta capacidade"),
    CURRAIS("Currais", "Currais, estábulos, aviários e pocilgas para pecuária"),
    DEPOSITOS("Depósitos", "Depósitos de feno, insumos, defensivos e tanques de diesel"),
    CERCAS("Cercas", "Cercas de madeira, porteiras articuladas e mourões"),
    JARDINS_DECORACAO("Jardins e decoração", "Jardins, canteiros floridos, árvores de sombra, caminhos e pátios"),
}

@Serializable
enum class RegionalArchStyle(
    val code: String,
    val regionTitle: String,
    val defaultWallHex: String,
    val defaultRoofHex: String,
    val defaultTrimHex: String,
    val defaultMaterial: String, // madeira | alvenaria | tijolinho | reboco_fazenda | enxaimel | palafita
    val hasSteepRoof: Boolean,
    val hasDeepVeranda: Boolean,
    val hasStiltBase: Boolean,
    val architectureNotes: String,
) {
    SUL_PR_MADEIRA(
        code = "sul_pr_madeira",
        regionTitle = "Sul • Paraná (Madeira & Colonial)",
        defaultWallHex = "#9B6A43",
        defaultRoofHex = "#9C3F28",
        defaultTrimHex = "#EDE3D2",
        defaultMaterial = "madeira",
        hasSteepRoof = true,
        hasDeepVeranda = true,
        hasStiltBase = false,
        architectureNotes = "Casas de araucária e alvenaria colonial com varanda em L, beiral largo, telha barro e galpão rústico.",
    ),
    PAMPA_RS_ESTANCIA(
        code = "pampa_rs_estancia",
        regionTitle = "Sul • Rio Grande do Sul (Estância)",
        defaultWallHex = "#EFE8DA",
        defaultRoofHex = "#8E3924",
        defaultTrimHex = "#6E432A",
        defaultMaterial = "alvenaria",
        hasSteepRoof = true,
        hasDeepVeranda = true,
        hasStiltBase = false,
        architectureNotes = "Estância campeira com paredes brancas, esquadrias escuras, varanda coberta e galpão crioulo.",
    ),
    SERRA_SC_COLONIAL(
        code = "serra_sc_colonial",
        regionTitle = "Sul • Santa Catarina (Serrana / Enxaimel)",
        defaultWallHex = "#F3EDE2",
        defaultRoofHex = "#7A2E1E",
        defaultTrimHex = "#4E3222",
        defaultMaterial = "enxaimel",
        hasSteepRoof = true,
        hasDeepVeranda = true,
        hasStiltBase = false,
        architectureNotes = "Influência serrana sulista com madeiramento aparente, telhado inclinado para chuvas e sótão.",
    ),
    CERRADO_MT_FAZENDA(
        code = "cerrado_mt_fazenda",
        regionTitle = "Centro-Oeste • Cerrado (Fazenda Ampla)",
        defaultWallHex = "#EFE6D5",
        defaultRoofHex = "#A5472A",
        defaultTrimHex = "#7D4E2D",
        defaultMaterial = "reboco_fazenda",
        hasSteepRoof = false,
        hasDeepVeranda = true,
        hasStiltBase = false,
        architectureNotes = "Sedes amplas e arejadas com pátios largos para grandes colheitadeiras, silos e barracões metálicos.",
    ),
    SUDESTE_MG_SERRA(
        code = "sudeste_mg_serra",
        regionTitle = "Sudeste • Minas Gerais / Serra (Fazenda Histórica)",
        defaultWallHex = "#F3EBD8",
        defaultRoofHex = "#A64628",
        defaultTrimHex = "#C26D24",
        defaultMaterial = "reboco_fazenda",
        hasSteepRoof = false,
        hasDeepVeranda = true,
        hasStiltBase = false,
        architectureNotes = "Casa de fazenda mineira/paulista com parede clara, barrado ocre, pilares na varanda e chaminé de tijolinho.",
    ),
    NORDESTE_SERTAO(
        code = "nordeste_sertao",
        regionTitle = "Nordeste • Sertão e Agreste (Alpendrada)",
        defaultWallHex = "#F7EBD6",
        defaultRoofHex = "#BD5832",
        defaultTrimHex = "#9E5C34",
        defaultMaterial = "alvenaria",
        hasSteepRoof = false,
        hasDeepVeranda = true,
        hasStiltBase = false,
        architectureNotes = "Arquitetura adaptada ao sol forte: alpendre fundo sombreado, paredes claras ventiladas e quintal arborizado.",
    ),
    NORTE_AMAZONIA(
        code = "norte_amazonia",
        regionTitle = "Norte • Amazônia (Madeira Elevada)",
        defaultWallHex = "#8C5835",
        defaultRoofHex = "#8E3E26",
        defaultTrimHex = "#C9A16B",
        defaultMaterial = "palafita",
        hasSteepRoof = true,
        hasDeepVeranda = true,
        hasStiltBase = true,
        architectureNotes = "Moradia de madeira de lei elevada sobre esteios/pilotis contra cheias, com varanda arejada e beiral amplo.",
    ),
    INTERNACIONAL_EUA(
        code = "internacional_eua",
        regionTitle = "Internacional • Meio-Oeste (EUA)",
        defaultWallHex = "#EAECEF",
        defaultRoofHex = "#3A424D",
        defaultTrimHex = "#FFFFFF",
        defaultMaterial = "madeira",
        hasSteepRoof = true,
        hasDeepVeranda = true,
        hasStiltBase = false,
        architectureNotes = "Farmhouse norte-americana com tábuas brancas horizontais, alpendre frontal e celeiro vermelho.",
    ),
    INTERNACIONAL_EU(
        code = "internacional_eu",
        regionTitle = "Internacional • Alpino (Alemanha)",
        defaultWallHex = "#EFE8DB",
        defaultRoofHex = "#7A2D22",
        defaultTrimHex = "#5A3B28",
        defaultMaterial = "enxaimel",
        hasSteepRoof = true,
        hasDeepVeranda = false,
        hasStiltBase = false,
        architectureNotes = "Construção bávara de dois pavimentos com balcão de madeira escura e telhado íngreme.",
    ),
}

fun resolveRegionalArchStyle(mapId: String): RegionalArchStyle = when {
    mapId == "map_rs" -> RegionalArchStyle.PAMPA_RS_ESTANCIA
    mapId == "map_sc" -> RegionalArchStyle.SERRA_SC_COLONIAL
    mapId.startsWith("map_pr") || mapId == "mod_map_campos_gerais" -> RegionalArchStyle.SUL_PR_MADEIRA
    mapId in setOf("map_mt", "map_ms", "map_go", "map_to", "mod_map_parecis_mt", "mod_map_pantanal_sul") -> RegionalArchStyle.CERRADO_MT_FAZENDA
    mapId in setOf("map_mg", "map_sp", "map_es", "map_rj", "mod_map_serra_cafe_mg") -> RegionalArchStyle.SUDESTE_MG_SERRA
    mapId in setOf("map_ce", "map_pe", "map_ba", "map_al", "map_pi", "map_ma") -> RegionalArchStyle.NORDESTE_SERTAO
    mapId in setOf("map_pa", "map_ro", "map_ac") -> RegionalArchStyle.NORTE_AMAZONIA
    mapId == "mod_map_iowa_midwest" -> RegionalArchStyle.INTERNACIONAL_EUA
    mapId == "mod_map_bavaria_eu" -> RegionalArchStyle.INTERNACIONAL_EU
    else -> RegionalArchStyle.SUDESTE_MG_SERRA
}

@Serializable
enum class PropertyEvolutionTier(
    val level: Int,
    val title: String,
    val subtitle: String,
    val targetScore: Int,
) {
    PEQUENA_INICIAL(
        level = 1,
        title = "1. Pequena Propriedade Inicial",
        subtitle = "Sítio modesto com casa simples, quintal, cerca e área inicial de cultivo.",
        targetScore = 0,
    ),
    RURAL_INTERMEDIARIA(
        level = 2,
        title = "2. Propriedade Rural Intermediária",
        subtitle = "Casa de alvenaria ou varanda, telhado cerâmico, garagem/galpão e jardim.",
        targetScore = 35,
    ),
    FAZENDA_GRANDE(
        level = 3,
        title = "3. Fazenda Grande",
        subtitle = "Sede ampla com varanda, galpão de máquinas, garagem, pátio de manobras e múltiplos talhões.",
        targetScore = 75,
    ),
    FAZENDA_DESENVOLVIDA(
        level = 4,
        title = "4. Fazenda Altamente Desenvolvida",
        subtitle = "Complexo rural completo: sede grande personalizada, silos, celeiros, garagens, paisagismo e agroindústria.",
        targetScore = 130,
    ),
}

data class BuildableStructureDef(
    val modelId: String,
    val name: String,
    val category: ConstructionCategory,
    val buildingType: BuildingType,
    val priceBrl: Double,
    val widthM: Double,
    val depthM: Double,
    val heightM: Double,
    val materialStyle: String, // madeira | alvenaria | tijolinho | reboco_fazenda | metal | enxaimel | palafita | paisagismo
    val defaultWallHex: String,
    val defaultRoofHex: String,
    val defaultTrimHex: String,
    val hasVeranda: Boolean = false,
    val hasGarage: Boolean = false,
    val isHouse: Boolean = false,
    val isHouseReplacementCapable: Boolean = false,
    val isWalkableSurface: Boolean = false,
    val isOpenBay: Boolean = false,
    val preferredRegions: List<RegionalArchStyle> = emptyList(), // empty = universal
    val spaceRequirementNote: String,
    val description: String,
)

val CONSTRUCTION_CATALOG: List<BuildableStructureDef> = listOf(
    // ===== 1. CASAS (Madeira, Alvenaria & Regionais) =====
    BuildableStructureDef(
        modelId = "casa_madeira_simples",
        name = "Casa de Madeira Simples (Sítio)",
        category = ConstructionCategory.CASAS,
        buildingType = BuildingType.CASA,
        priceBrl = 36000.0,
        widthM = 7.2,
        depthM = 6.2,
        heightM = 3.8,
        materialStyle = "madeira",
        defaultWallHex = "#9B6842",
        defaultRoofHex = "#9B3D26",
        defaultTrimHex = "#EDE2CE",
        hasVeranda = true,
        hasGarage = false,
        isHouse = true,
        isHouseReplacementCapable = true,
        preferredRegions = listOf(RegionalArchStyle.SUL_PR_MADEIRA, RegionalArchStyle.SERRA_SC_COLONIAL),
        spaceRequirementNote = "Requer área livre de 7,2m × 6,2m + acesso frontal",
        description = "Residência modesta em tábuas de araucária/pinus, varandinha coberta com pilares de madeira, porta central e telhado cerâmico.",
    ),
    BuildableStructureDef(
        modelId = "casa_madeira_varanda_sul",
        name = "Casa Sulista de Madeira com Varanda L",
        category = ConstructionCategory.CASAS,
        buildingType = BuildingType.CASA,
        priceBrl = 58000.0,
        widthM = 9.0,
        depthM = 7.5,
        heightM = 4.4,
        materialStyle = "madeira",
        defaultWallHex = "#8C5937",
        defaultRoofHex = "#8B321F",
        defaultTrimHex = "#F3EAD8",
        hasVeranda = true,
        hasGarage = false,
        isHouse = true,
        isHouseReplacementCapable = true,
        preferredRegions = listOf(RegionalArchStyle.SUL_PR_MADEIRA, RegionalArchStyle.PAMPA_RS_ESTANCIA, RegionalArchStyle.SERRA_SC_COLONIAL),
        spaceRequirementNote = "Requer terreno nivelado de 9,0m × 7,5m",
        description = "Arquitetura tradicional do Paraná e Serra Sulista: duas águas acentuadas, chaminé de fogão a lenha, janelas quadriculadas e varanda lateral.",
    ),
    BuildableStructureDef(
        modelId = "casa_amazonica_palafita",
        name = "Casa Amazônica Elevada em Madeira",
        category = ConstructionCategory.CASAS,
        buildingType = BuildingType.CASA,
        priceBrl = 49000.0,
        widthM = 8.4,
        depthM = 7.0,
        heightM = 4.6,
        materialStyle = "palafita",
        defaultWallHex = "#875333",
        defaultRoofHex = "#9A4228",
        defaultTrimHex = "#D6B27B",
        hasVeranda = true,
        hasGarage = false,
        isHouse = true,
        isHouseReplacementCapable = true,
        preferredRegions = listOf(RegionalArchStyle.NORTE_AMAZONIA),
        spaceRequirementNote = "Requer área de 8,4m × 7,0m em terra firme ou várzea",
        description = "Casa rural do Norte elevada sobre esteios de madeira de lei, escadaria frontal, deck sombreado e beiral largo contra chuvas tropicais.",
    ),
    BuildableStructureDef(
        modelId = "casa_alvenaria_media",
        name = "Casa de Alvenaria Colonial Média",
        category = ConstructionCategory.CASAS,
        buildingType = BuildingType.CASA,
        priceBrl = 78000.0,
        widthM = 9.4,
        depthM = 7.6,
        heightM = 4.3,
        materialStyle = "reboco_fazenda",
        defaultWallHex = "#F4ECE0",
        defaultRoofHex = "#A74426",
        defaultTrimHex = "#C26D24",
        hasVeranda = true,
        hasGarage = false,
        isHouse = true,
        isHouseReplacementCapable = true,
        preferredRegions = listOf(RegionalArchStyle.SUDESTE_MG_SERRA, RegionalArchStyle.SUL_PR_MADEIRA),
        spaceRequirementNote = "Requer terreno de 9,4m × 7,6m",
        description = "Inspirada nas autênticas sedes brasileiras: paredes brancas com barrado ocre, telhas coloniais de barro, varanda espaçosa com pilares e chaminé.",
    ),
    BuildableStructureDef(
        modelId = "casa_alpendre_nordeste",
        name = "Casa Nordestina de Alpendre Sombreado",
        category = ConstructionCategory.CASAS,
        buildingType = BuildingType.CASA,
        priceBrl = 68000.0,
        widthM = 9.8,
        depthM = 7.4,
        heightM = 4.0,
        materialStyle = "alvenaria",
        defaultWallHex = "#FAF0DF",
        defaultRoofHex = "#C05D34",
        defaultTrimHex = "#9E5C34",
        hasVeranda = true,
        hasGarage = false,
        isHouse = true,
        isHouseReplacementCapable = true,
        preferredRegions = listOf(RegionalArchStyle.NORDESTE_SERTAO),
        spaceRequirementNote = "Requer 9,8m × 7,4m em lote próprio",
        description = "Projetada para o clima quente do Nordeste: alpendre profundo em toda a fachada, paredes claras térmicas, janelas venezianas e anexo de tijolinho.",
    ),
    BuildableStructureDef(
        modelId = "casa_sede_grande_garagem",
        name = "Sede Ampla de Fazenda c/ Garagem",
        category = ConstructionCategory.CASAS,
        buildingType = BuildingType.CASA,
        priceBrl = 138000.0,
        widthM = 12.6,
        depthM = 9.2,
        heightM = 4.8,
        materialStyle = "reboco_fazenda",
        defaultWallHex = "#F2EAD8",
        defaultRoofHex = "#9A3C22",
        defaultTrimHex = "#B86621",
        hasVeranda = true,
        hasGarage = true,
        isHouse = true,
        isHouseReplacementCapable = true,
        preferredRegions = listOf(RegionalArchStyle.SUDESTE_MG_SERRA, RegionalArchStyle.CERRADO_MT_FAZENDA, RegionalArchStyle.PAMPA_RS_ESTANCIA),
        spaceRequirementNote = "Requer pátio amplo de 12,6m × 9,2m",
        description = "Sede patronal completa de alvenaria: varandão envolvente com pilares, garagem lateral coberta para caminhonete, cozinha caipira em tijolo à vista e jardim.",
    ),
    BuildableStructureDef(
        modelId = "casa_cerrado_moderna",
        name = "Sede Centro-Oeste Moderna & Tijolinho",
        category = ConstructionCategory.CASAS,
        buildingType = BuildingType.CASA,
        priceBrl = 152000.0,
        widthM = 13.0,
        depthM = 9.5,
        heightM = 4.9,
        materialStyle = "tijolinho",
        defaultWallHex = "#EAE1D0",
        defaultRoofHex = "#5A3D31",
        defaultTrimHex = "#A24C2D",
        hasVeranda = true,
        hasGarage = true,
        isHouse = true,
        isHouseReplacementCapable = true,
        preferredRegions = listOf(RegionalArchStyle.CERRADO_MT_FAZENDA),
        spaceRequirementNote = "Requer espaço amplo de 13,0m × 9,5m",
        description = "Casa ampla típica de grandes fazendas do Mato Grosso e Goiás: tijolo aparente nos pilares, cobertura dupla, varanda gourmet e garagem dupla.",
    ),
    BuildableStructureDef(
        modelId = "casa_enxaimel_sc",
        name = "Casa Serrano-Sulista Enxaimel / Chalé",
        category = ConstructionCategory.CASAS,
        buildingType = BuildingType.CASA,
        priceBrl = 96000.0,
        widthM = 9.6,
        depthM = 8.0,
        heightM = 5.4,
        materialStyle = "enxaimel",
        defaultWallHex = "#F4EEE3",
        defaultRoofHex = "#7A2D1E",
        defaultTrimHex = "#4C3120",
        hasVeranda = true,
        hasGarage = false,
        isHouse = true,
        isHouseReplacementCapable = true,
        preferredRegions = listOf(RegionalArchStyle.SERRA_SC_COLONIAL, RegionalArchStyle.INTERNACIONAL_EU),
        spaceRequirementNote = "Requer área de 9,6m × 8,0m",
        description = "Sobrado rural catarinense e colonial com vigamento de madeira escuro contrastando em alvenaria clara, sótão alto e varanda frontal.",
    ),

    // ===== 2. GALPÕES =====
    BuildableStructureDef(
        modelId = "galpao_madeira_sul",
        name = "Galpão Sulista de Madeira & Telha",
        category = ConstructionCategory.GALPOES,
        buildingType = BuildingType.GALPAO,
        priceBrl = 48000.0,
        widthM = 11.5,
        depthM = 8.0,
        heightM = 5.4,
        materialStyle = "madeira",
        defaultWallHex = "#7E4E2D",
        defaultRoofHex = "#953D27",
        defaultTrimHex = "#4E301D",
        isOpenBay = true,
        preferredRegions = listOf(RegionalArchStyle.SUL_PR_MADEIRA, RegionalArchStyle.PAMPA_RS_ESTANCIA, RegionalArchStyle.SERRA_SC_COLONIAL),
        spaceRequirementNote = "11,5m × 8,0m com frente aberta para entrada de tratores",
        description = "Galpão rústico brasileiro em tábuas de madeira tratada com vão livre para estacionar tratores médios e implementos.",
    ),
    BuildableStructureDef(
        modelId = "galpao_maquinas_grande",
        name = "Barracão Metálico / Alvenaria (Grande)",
        category = ConstructionCategory.GALPOES,
        buildingType = BuildingType.GALPAO,
        priceBrl = 86000.0,
        widthM = 14.5,
        depthM = 10.0,
        heightM = 6.4,
        materialStyle = "alvenaria",
        defaultWallHex = "#D8D6CF",
        defaultRoofHex = "#3E5A70",
        defaultTrimHex = "#2E404F",
        isOpenBay = true,
        preferredRegions = listOf(RegionalArchStyle.CERRADO_MT_FAZENDA, RegionalArchStyle.SUDESTE_MG_SERRA),
        spaceRequirementNote = "14,5m × 10,0m — comporta colheitadeiras e caminhões",
        description = "Estrutura ampla com mureta de alvenaria, pilares metálicos reforçados e cobertura alta para frotas pesadas.",
    ),
    BuildableStructureDef(
        modelId = "galpao_oficina_mecanica",
        name = "Oficina Rural & Manutenção",
        category = ConstructionCategory.GALPOES,
        buildingType = BuildingType.OFICINA,
        priceBrl = 62000.0,
        widthM = 9.5,
        depthM = 7.0,
        heightM = 4.8,
        materialStyle = "alvenaria",
        defaultWallHex = "#E4DDD0",
        defaultRoofHex = "#8C3B24",
        defaultTrimHex = "#395D44",
        isOpenBay = false,
        spaceRequirementNote = "9,5m × 7,0m junto ao pátio da fazenda",
        description = "Galpão equipado com bancada, compressor e área para revisão e pintura das máquinas.",
    ),

    // ===== 3. GARAGENS =====
    BuildableStructureDef(
        modelId = "garagem_madeira_simples",
        name = "Garagem Coberta de Madeira (1 Vaga)",
        category = ConstructionCategory.GARAGENS,
        buildingType = BuildingType.GARAGEM,
        priceBrl = 18500.0,
        widthM = 6.0,
        depthM = 5.8,
        heightM = 3.5,
        materialStyle = "madeira",
        defaultWallHex = "#8B5A37",
        defaultRoofHex = "#9B3F28",
        defaultTrimHex = "#5A3A22",
        isOpenBay = true,
        spaceRequirementNote = "6,0m × 5,8m próximo à casa ou estrada",
        description = "Abrigo aberto com pilares de madeira e telhado cerâmico para caminhonete, carro ou trator pequeno.",
    ),
    BuildableStructureDef(
        modelId = "garagem_alvenaria_dupla",
        name = "Garagem Dupla de Alvenaria",
        category = ConstructionCategory.GARAGENS,
        buildingType = BuildingType.GARAGEM,
        priceBrl = 34000.0,
        widthM = 8.4,
        depthM = 6.8,
        heightM = 3.9,
        materialStyle = "alvenaria",
        defaultWallHex = "#EFE7D8",
        defaultRoofHex = "#A24224",
        defaultTrimHex = "#B76522",
        isOpenBay = true,
        spaceRequirementNote = "8,4m × 6,8m com entrada desimpedida",
        description = "Garagem fechada nas laterais e fundos em alvenaria com barrado ocre, comportando duas caminhonetes ou tratores.",
    ),

    // ===== 4. CELEIROS =====
    BuildableStructureDef(
        modelId = "celeiro_madeira_tradicional",
        name = "Celeiro Tradicional de Madeira",
        category = ConstructionCategory.CELEIROS,
        buildingType = BuildingType.CELEIRO,
        priceBrl = 54000.0,
        widthM = 11.0,
        depthM = 8.0,
        heightM = 6.2,
        materialStyle = "madeira",
        defaultWallHex = "#8C3328",
        defaultRoofHex = "#595D62",
        defaultTrimHex = "#F4EFE6",
        spaceRequirementNote = "11,0m × 8,0m em piso firme",
        description = "Celeiro clássico de dois níveis com mezanino de feno, portão duplo contrastado e cobertura ventilada.",
    ),
    BuildableStructureDef(
        modelId = "celeiro_alvenaria_feno",
        name = "Celeiro Rural de Alvenaria & Tijolo",
        category = ConstructionCategory.CELEIROS,
        buildingType = BuildingType.CELEIRO,
        priceBrl = 67000.0,
        widthM = 12.0,
        depthM = 8.5,
        heightM = 5.8,
        materialStyle = "tijolinho",
        defaultWallHex = "#B45C3E",
        defaultRoofHex = "#8C3A23",
        defaultTrimHex = "#EAE2D3",
        spaceRequirementNote = "12,0m × 8,5m",
        description = "Estrutura reforçada em tijolinho e alvenaria para conservar fardos de feno, ração e sacaria seca.",
    ),

    // ===== 5. SILOS =====
    BuildableStructureDef(
        modelId = "silo_metalico_padrao",
        name = "Silo Graneleiro Metálico (250t)",
        category = ConstructionCategory.SILOS,
        buildingType = BuildingType.SILO,
        priceBrl = 95000.0,
        widthM = 6.2,
        depthM = 6.2,
        heightM = 10.5,
        materialStyle = "metal",
        defaultWallHex = "#B8C0C8",
        defaultRoofHex = "#7A848D",
        defaultTrimHex = "#3D464E",
        spaceRequirementNote = "6,2m × 6,2m próximo ao pátio de carga",
        description = "Silo cilíndrico galvanizado com elevador de canecas, escada helicoidal e base de descarga.",
    ),
    BuildableStructureDef(
        modelId = "silo_secador_agro",
        name = "Secador & Silo Pulmão de Grãos",
        category = ConstructionCategory.SILOS,
        buildingType = BuildingType.SECADOR,
        priceBrl = 120000.0,
        widthM = 7.4,
        depthM = 7.4,
        heightM = 9.5,
        materialStyle = "metal",
        defaultWallHex = "#8C98A4",
        defaultRoofHex = "#C24E2A",
        defaultTrimHex = "#2D343B",
        spaceRequirementNote = "7,4m × 7,4m",
        description = "Torre secadora de grãos com moega receptora e controle térmico da colheita.",
    ),

    // ===== 6. CURRAIS =====
    BuildableStructureDef(
        modelId = "curral_madeira_manejo",
        name = "Curral de Madeira c/ Embarcador",
        category = ConstructionCategory.CURRAIS,
        buildingType = BuildingType.CURRAL,
        priceBrl = 46000.0,
        widthM = 12.0,
        depthM = 10.0,
        heightM = 2.6,
        materialStyle = "madeira",
        defaultWallHex = "#6B472B",
        defaultRoofHex = "#953E28",
        defaultTrimHex = "#4B301C",
        spaceRequirementNote = "12,0m × 10,0m junto às pastagens",
        description = "Curral brasileiro em réguas de madeira de lei com brete, cocho coberto e mourões robustos.",
    ),
    BuildableStructureDef(
        modelId = "estabulo_leiteiro_coberto",
        name = "Estábulo Leiteiro Coberto",
        category = ConstructionCategory.CURRAIS,
        buildingType = BuildingType.ESTABULO,
        priceBrl = 74000.0,
        widthM = 11.2,
        depthM = 8.2,
        heightM = 4.9,
        materialStyle = "alvenaria",
        defaultWallHex = "#EDE6D6",
        defaultRoofHex = "#9F4125",
        defaultTrimHex = "#7A4C2A",
        spaceRequirementNote = "11,2m × 8,2m",
        description = "Instalação coberta para ordenha leiteira, bebedouros e sombreamento do rebanho.",
    ),
    BuildableStructureDef(
        modelId = "aviario_pocilga_rural",
        name = "Aviário / Galpão de Pequenos Animais",
        category = ConstructionCategory.CURRAIS,
        buildingType = BuildingType.AVIARIO,
        priceBrl = 42000.0,
        widthM = 10.0,
        depthM = 6.2,
        heightM = 3.6,
        materialStyle = "alvenaria",
        defaultWallHex = "#EFE9DE",
        defaultRoofHex = "#A34B2D",
        defaultTrimHex = "#6B4E35",
        spaceRequirementNote = "10,0m × 6,2m",
        description = "Galpão telado ventilado com cortinas laterais e cobertura cerâmica ou zinco.",
    ),

    // ===== 7. DEPÓSITOS =====
    BuildableStructureDef(
        modelId = "deposito_insumos_feno",
        name = "Depósito Fechado de Insumos & Feno",
        category = ConstructionCategory.DEPOSITOS,
        buildingType = BuildingType.DEPOSITO,
        priceBrl = 38000.0,
        widthM = 10.0,
        depthM = 7.0,
        heightM = 4.5,
        materialStyle = "alvenaria",
        defaultWallHex = "#E7DFCF",
        defaultRoofHex = "#8E3C26",
        defaultTrimHex = "#6A472C",
        spaceRequirementNote = "10,0m × 7,0m",
        description = "Barracão fechado para guardar sacos de sementes, adubos, defensivos e fardos.",
    ),
    BuildableStructureDef(
        modelId = "tanque_diesel_fazenda",
        name = "Posto / Tanque Aéreo Diesel 25.000L",
        category = ConstructionCategory.DEPOSITOS,
        buildingType = BuildingType.TANQUE_COMBUSTIVEL,
        priceBrl = 34000.0,
        widthM = 5.2,
        depthM = 4.2,
        heightM = 3.7,
        materialStyle = "metal",
        defaultWallHex = "#D9822B",
        defaultRoofHex = "#5A626B",
        defaultTrimHex = "#242A30",
        spaceRequirementNote = "5,2m × 4,2m em local de fácil manobra",
        description = "Tanque elevado com bacia de contenção e bomba para abastecer máquinas dentro da própria fazenda.",
    ),

    // ===== 8. CERCAS =====
    BuildableStructureDef(
        modelId = "cerca_madeira_rural",
        name = "Cerca de Madeira (Lance 4m)",
        category = ConstructionCategory.CERCAS,
        buildingType = BuildingType.CERCA,
        priceBrl = 1200.0,
        widthM = 4.0,
        depthM = 0.45,
        heightM = 1.45,
        materialStyle = "madeira",
        defaultWallHex = "#6E4A2D",
        defaultRoofHex = "#6E4A2D",
        defaultTrimHex = "#8C6340",
        spaceRequirementNote = "4,0m lineares ao redor de quintais e pastos",
        description = "Mourões roliços tratados e réguas horizontais tradicionais das divisas rurais.",
    ),
    BuildableStructureDef(
        modelId = "porteira_madeira_fazenda",
        name = "Porteira Articulada de Fazenda",
        category = ConstructionCategory.CERCAS,
        buildingType = BuildingType.PORTEIRA,
        priceBrl = 2800.0,
        widthM = 4.2,
        depthM = 0.55,
        heightM = 1.65,
        materialStyle = "madeira",
        defaultWallHex = "#7A4C29",
        defaultRoofHex = "#9E3B24",
        defaultTrimHex = "#D8C8AE",
        spaceRequirementNote = "4,2m de vão para passagem de tratores",
        description = "Porteira dupla com mourões principais altos; abre e fecha sob comando do jogador.",
    ),

    // ===== 9. JARDINS E DECORAÇÃO =====
    BuildableStructureDef(
        modelId = "jardim_pergolado_casa",
        name = "Jardim com Pergolado e Floreiras",
        category = ConstructionCategory.JARDINS_DECORACAO,
        buildingType = BuildingType.JARDIM,
        priceBrl = 4500.0,
        widthM = 6.0,
        depthM = 5.0,
        heightM = 2.6,
        materialStyle = "paisagismo",
        defaultWallHex = "#7B4F2C",
        defaultRoofHex = "#3E8B42",
        defaultTrimHex = "#D63A7B",
        isWalkableSurface = true,
        spaceRequirementNote = "6,0m × 5,0m ao lado da casa ou quintal",
        description = "Espaço aconchegante com pergolado de madeira, lajotas de pedra e flores coloridas.",
    ),
    BuildableStructureDef(
        modelId = "canteiro_azaleias_flores",
        name = "Canteiro de Azaleias & Flores Tropicais",
        category = ConstructionCategory.JARDINS_DECORACAO,
        buildingType = BuildingType.CANTEIRO_FLORES,
        priceBrl = 1400.0,
        widthM = 4.2,
        depthM = 2.4,
        heightM = 0.9,
        materialStyle = "paisagismo",
        defaultWallHex = "#785337",
        defaultRoofHex = "#D92E7B",
        defaultTrimHex = "#367A39",
        isWalkableSurface = true,
        spaceRequirementNote = "4,2m × 2,4m em bordas de varandas e entradas",
        description = "Canteiro denso com arbustos floridos rosa, magenta e folhagem verde vibrante.",
    ),
    BuildableStructureDef(
        modelId = "arvore_sombra_ipe",
        name = "Árvore de Sombra (Ipê / Mangueira)",
        category = ConstructionCategory.JARDINS_DECORACAO,
        buildingType = BuildingType.ARVORE_ORNAMENTAL,
        priceBrl = 1800.0,
        widthM = 2.4,
        depthM = 2.4,
        heightM = 7.2,
        materialStyle = "paisagismo",
        defaultWallHex = "#5A3D28",
        defaultRoofHex = "#E5B82E",
        defaultTrimHex = "#2E6D2F",
        spaceRequirementNote = "2,4m × 2,4m livres fora da pista e da lavoura",
        description = "Árvore brasileira de copa larga e florida para sombrear o quintal, varandas e cercas.",
    ),
    BuildableStructureDef(
        modelId = "caminho_terra_quintal",
        name = "Caminho de Terra / Cascalho",
        category = ConstructionCategory.JARDINS_DECORACAO,
        buildingType = BuildingType.CAMINHO_TERRA,
        priceBrl = 950.0,
        widthM = 4.0,
        depthM = 4.0,
        heightM = 0.1,
        materialStyle = "paisagismo",
        defaultWallHex = "#8A6C4B",
        defaultRoofHex = "#9B7B58",
        defaultTrimHex = "#6E553A",
        isWalkableSurface = true,
        spaceRequirementNote = "4,0m × 4,0m — totalmente transitável por tratores e pedestres",
        description = "Trecho de caminho rural compactado para conectar a casa da sede aos galpões e porteiras.",
    ),
    BuildableStructureDef(
        modelId = "patio_manobras_sede",
        name = "Pátio de Manobras Cascalhado",
        category = ConstructionCategory.JARDINS_DECORACAO,
        buildingType = BuildingType.PATIO,
        priceBrl = 9000.0,
        widthM = 10.0,
        depthM = 9.0,
        heightM = 0.12,
        materialStyle = "paisagismo",
        defaultWallHex = "#9A8F7D",
        defaultRoofHex = "#887D6C",
        defaultTrimHex = "#655C4E",
        isWalkableSurface = true,
        spaceRequirementNote = "10,0m × 9,0m — espaço amplo para manobrar carretas e implementos",
        description = "Pátio nivelado sem atolamento para organizar máquinas, carretas e descarga de grãos.",
    ),
    BuildableStructureDef(
        modelId = "estacionamento_maquinas_patio",
        name = "Estacionamento Demarcado de Máquinas",
        category = ConstructionCategory.JARDINS_DECORACAO,
        buildingType = BuildingType.ESTACIONAMENTO,
        priceBrl = 11000.0,
        widthM = 10.5,
        depthM = 8.0,
        heightM = 0.18,
        materialStyle = "paisagismo",
        defaultWallHex = "#7A7E82",
        defaultRoofHex = "#D69E2E",
        defaultTrimHex = "#4C5157",
        isWalkableSurface = true,
        spaceRequirementNote = "10,5m × 8,0m — 100% trafegável",
        description = "Área pavimentada em brita compactada com batentes de madeira para alinhar tratores e carretas.",
    ),
)

fun getStructureDef(modelId: String?): BuildableStructureDef? =
    if (modelId == null) null else CONSTRUCTION_CATALOG.firstOrNull { it.modelId == modelId }

fun getRecommendedStructuresForMap(mapId: String): List<BuildableStructureDef> {
    val region = resolveRegionalArchStyle(mapId)
    return CONSTRUCTION_CATALOG.sortedByDescending { def ->
        when {
            region in def.preferredRegions -> 2
            def.preferredRegions.isEmpty() -> 1
            else -> 0
        }
    }
}

data class BuildingVisualProfile(
    val widthM: Double,
    val depthM: Double,
    val heightM: Double,
    val archStyle: RegionalArchStyle,
    val materialStyle: String,
    val wallColorHex: String,
    val roofColorHex: String,
    val trimColorHex: String,
    val hasVeranda: Boolean,
    val hasGarage: Boolean,
    val isHouse: Boolean,
    val isWalkableSurface: Boolean,
    val isOpenBay: Boolean,
    val displayLabel: String,
)

fun resolveBuildingVisualProfile(b: PlacedBuilding, mapId: String): BuildingVisualProfile {
    val regionStyle = b.archStyle?.let { code ->
        RegionalArchStyle.entries.firstOrNull { it.code == code }
    } ?: resolveRegionalArchStyle(mapId)
    val def = getStructureDef(b.modelId)
    val meta = BUILDING_META[b.type]

    val width = max(0.35, b.customWidthM ?: def?.widthM ?: meta?.widthM ?: 6.0)
    val depth = max(0.35, b.customDepthM ?: def?.depthM ?: meta?.depthM ?: 6.0)
    val height = max(0.06, b.customHeightM ?: def?.heightM ?: meta?.heightM ?: 4.0)

    val isHouse = def?.isHouse ?: (b.type == BuildingType.CASA || b.type == BuildingType.CASA_URBANA)
    val hasVeranda = b.hasVeranda ?: def?.hasVeranda ?: (isHouse && width >= 6.8)
    val hasGarage = b.hasGarage ?: def?.hasGarage ?: false

    val material = b.materialStyle
        ?: def?.materialStyle
        ?: if (isHouse) regionStyle.defaultMaterial else "alvenaria"

    val defaultWallByType = when (b.type) {
        BuildingType.BANCO -> "#EFE9DC"
        BuildingType.CONCESSIONARIA -> "#E4EBE6"
        BuildingType.LOJA_AGROPECUARIA -> "#E5EDD9"
        BuildingType.COOPERATIVA, BuildingType.ESTACAO_GRAOS -> "#DCE3E0"
        BuildingType.MADEIREIRA -> "#9B6B43"
        BuildingType.LOJA_MATERIAIS -> "#D8CFC4"
        BuildingType.PONTO_ANIMAIS -> "#8E5E3B"
        BuildingType.ARMAZEM -> "#CFD6DA"
        BuildingType.POSTO_COMBUSTIVEL -> "#F3F4F6"
        else -> if (isHouse) regionStyle.defaultWallHex else "#EAE2D3"
    }
    val defaultRoofByType = when (b.type) {
        BuildingType.BANCO -> "#1B4332"
        BuildingType.CONCESSIONARIA -> "#2D6A4F"
        BuildingType.LOJA_AGROPECUARIA -> "#386641"
        BuildingType.COOPERATIVA, BuildingType.ESTACAO_GRAOS -> "#37474F"
        BuildingType.MADEIREIRA -> "#7A3622"
        BuildingType.LOJA_MATERIAIS -> "#B45309"
        BuildingType.PONTO_ANIMAIS -> "#8D3E28"
        BuildingType.ARMAZEM -> "#455A64"
        BuildingType.POSTO_COMBUSTIVEL -> "#C62828"
        else -> if (isHouse) regionStyle.defaultRoofHex else "#9C3F28"
    }
    val defaultTrimByType = when (b.type) {
        BuildingType.BANCO -> "#D4A017"
        BuildingType.CONCESSIONARIA -> "#F4B400"
        BuildingType.LOJA_AGROPECUARIA -> "#D97706"
        BuildingType.COOPERATIVA, BuildingType.ESTACAO_GRAOS -> "#E9A825"
        BuildingType.MADEIREIRA -> "#5D3A22"
        BuildingType.LOJA_MATERIAIS -> "#374151"
        BuildingType.PONTO_ANIMAIS -> "#4E3322"
        BuildingType.ARMAZEM -> "#263238"
        BuildingType.POSTO_COMBUSTIVEL -> "#F59E0B"
        else -> if (isHouse) regionStyle.defaultTrimHex else "#6B4A2D"
    }

    val wallHex = b.wallColorHex
        ?: def?.defaultWallHex
        ?: defaultWallByType
    val roofHex = b.roofColorHex
        ?: def?.defaultRoofHex
        ?: defaultRoofByType
    val trimHex = b.trimColorHex
        ?: def?.defaultTrimHex
        ?: defaultTrimByType

    val isWalkableSurface = def?.isWalkableSurface ?: (
        b.type == BuildingType.ESTRADA_INTERNA ||
            b.type == BuildingType.PATIO ||
            b.type == BuildingType.JARDIM ||
            b.type == BuildingType.CAMINHO_TERRA ||
            b.type == BuildingType.CANTEIRO_FLORES ||
            b.type == BuildingType.ESTACIONAMENTO
        )
    val isOpenBay = def?.isOpenBay ?: (
        b.type == BuildingType.GALPAO ||
            b.type == BuildingType.GARAGEM ||
            b.type == BuildingType.OFICINA
        )

    return BuildingVisualProfile(
        widthM = width,
        depthM = depth,
        heightM = height,
        archStyle = regionStyle,
        materialStyle = material,
        wallColorHex = wallHex,
        roofColorHex = roofHex,
        trimColorHex = trimHex,
        hasVeranda = hasVeranda,
        hasGarage = hasGarage,
        isHouse = isHouse,
        isWalkableSurface = isWalkableSurface,
        isOpenBay = isOpenBay,
        displayLabel = b.customLabel ?: def?.name ?: meta?.displayName ?: b.type.name,
    )
}

data class HouseColorPreset(
    val id: String,
    val label: String,
    val wallHex: String,
    val roofHex: String,
    val trimHex: String,
    val materialStyle: String,
)

val HOUSE_CUSTOMIZATION_PRESETS: List<HouseColorPreset> = listOf(
    HouseColorPreset("mineira_ocre", "Fazenda Mineira (Branco + Ocre)", "#F4ECE0", "#A74426", "#C26D24", "reboco_fazenda"),
    HouseColorPreset("araucaria_sul", "Madeira Sulista (Cedro + Barro)", "#945F3B", "#8D3421", "#EFE5D4", "madeira"),
    HouseColorPreset("tijolinho_cerrado", "Tijolinho Rústico + Telha Escura", "#B86B4B", "#563B2F", "#EFE6D6", "tijolinho"),
    HouseColorPreset("nordeste_areia", "Alpendre Claro (Areia + Terracota)", "#FAF1DF", "#C05D34", "#9E5C34", "alvenaria"),
    HouseColorPreset("serra_enxaimel", "Enxaimel Serrano (Creme + Mogno)", "#F3EDE2", "#7A2D1E", "#4A2E1D", "enxaimel"),
    HouseColorPreset("verde_fazenda", "Verde Campo + Branco Colonial", "#DCE8D8", "#953D27", "#FFFFFF", "alvenaria"),
    HouseColorPreset("azul_colonial", "Azul Colonial + Branco", "#DCE7EE", "#9A3F25", "#F8F5EE", "alvenaria"),
)
