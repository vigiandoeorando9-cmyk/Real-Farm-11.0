package com.example.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
data class HubVersionEntry(
    val version: String,
    val dateIso: String,
    val notes: String,
)

@Serializable
data class HubPackEntity(
    val packId: String,
    val name: String,
    val category: String,
    val version: String,
    val sizeMb: Double,
    val author: String,
    val description: String,
    val minGameVersion: String,
    val installed: Boolean,
    val localPath: String? = null,
    val installedAtEpochMs: Long = 0L,
    val dependencies: List<String>? = null,
    val androidCompatible: Boolean? = true,
    val windowsCompatible: Boolean? = true,
    val lifecycleState: ModValidationStatus? = null,
    val sha256Preview: String? = null,
    val isCustomImported: Boolean? = false,
    val enabled: Boolean = true,
)

enum class ModAssetDeliveryMode {
    BUILTIN_ENGINE,
    EXTERNAL_FILE_REQUIRED,
    WIP_BLOCKED,
}

const val MOD_MISSING_FILE_ERROR = "Este mod ainda precisa de um arquivo de instalação válido."

@Serializable
data class HubModMeta(
    val packId: String,
    val brand: String,
    val machineType: String,
    val subCategory: String,
    val latestVersion: String,
    val changelog: String,
    val compatibilityNote: String,
    val unlockedVehicleIds: List<String>,
    val unlockedImplementIds: List<String>,
    val unlockedMapId: String?,
    val previewColorHex: String,
    val versionHistory: List<HubVersionEntry>? = null,
)

data class ModValidationResult(
    val status: ModValidationStatus,
    val ok: Boolean,
    val integrityOk: Boolean,
    val dependenciesOk: Boolean,
    val missingDependencies: List<String>,
    val platformCompatible: Boolean,
    val versionCompatible: Boolean,
    val spaceOk: Boolean,
    val duplicateConflict: Boolean,
    val notes: List<String>,
)

const val GAME_SEMVER = "3.0"

val HUB_CATEGORIES = listOf(
    "Todos",
    "Tratores",
    "Implementos",
    "Plantadeiras",
    "Pulverizadores",
    "Carretas",
    "Colheitadeiras",
    "Gráficos",
    "Texturas",
    "Mapas",
    "Caminhões",
    "Máquinas Especiais",
    "Construções",
    "Animais e Pecuária",
    "Gameplay e Sistemas",
)

val HUB_BRAND_FILTERS = listOf(
    "Todas", "Agrale", "CBT", "Valmet/Valtra", "John Deere", "Case IH", "New Holland",
    "Fendt", "Claas", "Baldan", "Tatu Marchesan", "Stara", "Piccin", "Ford/Toyota/Honda", "Scania", "Farm Brasil Studio",
)

val HUB_SUBTYPE_FILTERS = listOf(
    "Todos", "Compactos", "Médios", "Esteiras & Alta Potência", "Cana & Forrageiras",
    "Arados/Grades/Subsoladores", "Plantadeiras", "Abrir Campos", "Mapas BR",
    "Mapas Internacionais", "Caminhonetes & Motos", "Caminhões", "Silos/Galpões/Pecuária", "Visual & Sistemas",
)

private fun hp(
    id: String, name: String, category: String, version: String, sizeMb: Double, author: String,
    description: String, minVer: String = "3.0", installed: Boolean = false, localPath: String? = null,
    installedAt: Long = 0L, deps: List<String>? = null, android: Boolean? = true, windows: Boolean? = true,
    lifecycle: ModValidationStatus? = null,
) = HubPackEntity(
    packId = id, name = name, category = category, version = version, sizeMb = sizeMb, author = author,
    description = description, minGameVersion = minVer, installed = installed, localPath = localPath,
    installedAtEpochMs = installedAt, dependencies = deps, androidCompatible = android,
    windowsCompatible = windows, lifecycleState = lifecycle,
)

val DEFAULT_HUB_PACKS: List<HubPackEntity> = listOf(
    hp("pack_cbt_classico", "CBT 2105 Turbo Clássico BR", "Tratores", "1.0.2", 14.5, "AgroMod Brasil", "Desbloqueia o clássico nacional CBT 2105 Turbo (122 cv) na Concessionária e Loja Interna."),
    hp("pack_tratores_compactos_br", "Tratores Compactos Cafeeiros BR", "Tratores", "1.1.0", 16.8, "Oficina Sul", "Desbloqueia Valmet 785 Cafeeiro e Agrale 575.4 Compacto ideais para fruticultura e pequenas áreas."),
    hp("pack_articulados_esteiras", "Gigantes Articulados & Esteiras", "Tratores", "1.2.0", 28.4, "Heavy Agro BR", "Desbloqueia Case IH Steiger 620 Quadtrac (692 cv) e Fendt 1167 Vario MT (673 cv) de esteira."),
    hp("pack_colheitadeiras_agro", "Colheitadeiras Cana & Forrageira", "Colheitadeiras", "1.1.5", 24.0, "Equipe Cerrado", "Desbloqueia Case IH Austoft A8810 (Cana) e Claas Jaguar 980 (Forrageira de Milho/Silagem)."),
    hp("pack_abrir_campos", "Mod Abrir e Criar Campos Pro", "Implementos", "1.3.0", 11.2, "Farm Brasil Studio", "Habilita criação/expansão de campos + desbloqueia Arado Aiveca Tatu 6L e Roçadeira RMD 3.0m."),
    hp("pack_implementos_pesados", "Implementos Baldan & Piccin Pesados", "Implementos", "1.1.0", 13.6, "Implementos BR", "Desbloqueia Grade Niveladora Baldan CRI 44 Discos e Subsolador Piccin 7 Hastes."),
    hp("pack_plantio_precisao", "Stara Estrela 32L Plantio Direto", "Implementos", "1.2.1", 15.2, "Equipe SulAgro", "Desbloqueia Plantadeira Stara Estrela 32 Linhas (14,4m) com corte de seção."),
    hp("pack_mapa_campos_gerais", "Mapa Paraná — Campos Gerais", "Mapas", "1.1.0", 48.0, "Farm Brasil Studio", "Novo mapa jogável com colinas suaves, talhões curvos em curvas de nível e cooperativas de leite/grãos."),
    hp("pack_mapa_parecis", "Mapa Mato Grosso — Chapada Parecis", "Mapas", "1.0.5", 52.0, "AgroMT Cartografia", "Novo mapa jogável com mega chapadões, soja/algodão/cana e áreas amplas para abertura de campo."),
    hp("pack_exp_pantanal", "Mapa MS — Pantanal & Arrozais", "Mapas", "1.0.0", 44.0, "Farm Brasil Studio", "Novo mapa jogável no Pantanal Sul com planícies alagáveis, rios largos, pontes e pecuária."),
    hp("pack_mapa_serra_cafe", "Mapa MG — Serra do Café", "Mapas", "1.1.2", 46.5, "Sul de Minas Modding", "Novo mapa montanhoso com aclives/declives acentuados, estradas sinuosas e cafeicultura."),
    hp("pack_mapa_iowa", "Mapa EUA — Meio-Oeste (Iowa)", "Mapas", "1.0.4", 49.0, "Global Agro Maps", "Mapa internacional no Cinturão do Milho norte-americano com solo Mollisol negro e grandes silos."),
    hp("pack_mapa_bavaria", "Mapa Europa — Baviera Rural", "Mapas", "1.0.8", 51.0, "Global Agro Maps", "Mapa internacional europeu com vales alpinos, campos irregulares em encostas e estradas rurais."),
    hp("pack_utilitarios_campo", "Pacote Caminhonetes & Moto Rural", "Caminhões", "1.1.0", 19.4, "Garagem BR", "Desbloqueia Ford F-250 Agro 4x4, Toyota Hilux Cabine Simples e Moto Honda NXR 160 Trilha."),
    hp("pack_rodotrem_canavieiro", "Scania R540 6x4 Graneleiro BR", "Caminhões", "1.0.5", 21.0, "Logística Cerrado", "Desbloqueia caminhão pesado Scania R540 6x4 para transporte rodoviário de safras."),
    hp("pack_silos_coop", "Pacote Silos, Galpões & Pecuária", "Construções", "1.1.0", 12.1, "Engenharia Rural", "Otimiza custos e adiciona referências modulares para silos, galpões e pátios."),
    hp("pack_terra_roxa_hd", "Texturas Procedurais Ultra Solo & Chuva", "Gráficos e Iluminação", "1.2.0", 16.5, "Visual Agro", "Aprimora contraste de sulcos arados, palhada e brilho de solo molhado na chuva."),
    hp("pack_gps_rtk", "GPS RTK Linhas AB Avançado", "Gameplay e Sistemas", "1.2.5", 5.8, "Precisão BR", "Correção de guiamento paralelo e controle automático de seções em implementos largos."),
    hp("pack_carretas_transbordo_br", "Carretas Transbordo & Rodoviárias BR", "Carretas e Reboques", "1.0.0", 14.2, "Implementos Sul", "Pacote temático de carretas bazuca, tanques pipa, boiadeiras e pranchas carrega-tudo compatíveis com quinta-roda.", deps = listOf("pack_rodotrem_canavieiro")),
    hp("pack_especiais_construcao_br", "Linha Amarela & Terraplanagem Rural", "Máquinas Especiais", "1.0.0", 18.0, "Terraplenagem Agro", "Pacote de suporte e configuração para escavadeiras, motoniveladoras, retroescavadeiras e manipulação de pátio."),
    hp("pack_pecuaria_leite_pro", "Manejo Leiteiro & Nutrição Animal Pro", "Animais e Pecuária", "1.0.0", 9.4, "Pecuária Forte", "Expande rotinas de ordenha, monitoramento de saúde animal e controle de reserva de ração no estábulo.", deps = listOf("pack_silos_coop")),
    hp("pack_agro_clima_pbr_wip", "Biomas & Sombras Dinâmicas Volumétricas", "Gráficos e Iluminação", "0.9.0", 42.0, "Farm Brasil Lab", "Módulo experimental em desenvolvimento para sombras volumétricas em neblina densa e nuvens em camadas.", minVer = "3.2", lifecycle = ModValidationStatus.EM_DESENVOLVIMENTO),
    hp("pack_cooperativa_custom_bundle", "Pacote Cooperativa Sul (Assets Externos)", "Construções", "1.0.0", 64.0, "Comunidade PR", "Estrutura registrada para recebimento de modelos arquitetônicos personalizados da comunidade.", android = false, windows = true, lifecycle = ModValidationStatus.AGUARDANDO_ARQUIVOS),
    // Mega Fase 2 (+18)
    hp("pack_calcario_fertilizacao_pro", "Agronomia N-P-K, Calcário & Manejo de pH", "Gameplay e Sistemas", "2.0.0", 8.6, "Farm Brasil Studio", "Ativa análise de N-P-K/pH em tempo real, correção com calcário e eficiência de adubação em 3 estágios.", installed = true, localPath = "builtin://hub/pack_calcario_fertilizacao_pro.json", installedAt = 1730000001000L),
    hp("pack_pulverizadores_jacto_horsch", "Pulverizadores Automotrizes & Controle de Pragas", "Implementos", "2.0.1", 17.4, "Proteção Vegetal BR", "Pacote completo para controle de pragas e ervas daninhas com barras Jacto, Kuhn e Horsch."),
    hp("pack_feno_silagem_leite", "Pacote Fenação & Ensilagem de Alta Densidade", "Implementos", "1.4.0", 14.8, "SulForragem Mods", "Configura segadoras triplas, enleiradores Kuhn e vagões forrageiros Krone/Nogueira/Sermag."),
    hp("pack_valmet_valtra_classicos", "Coleção Valmet & Valtra Clássicos BR", "Tratores", "1.3.2", 22.4, "Mogi Agro Classics", "Homenagem aos tratores Valmet/Valtra nacionais das décadas de 70 a 2000 com som característico."),
    hp("pack_massey_serie_prata", "Massey Ferguson Série Prata & Colheita Clássica", "Colheitadeiras", "1.2.0", 19.6, "Oficina Sul", "Pacote temático das colheitadeiras saca-palhas MF 3640, 5650 e séries Trident modernas."),
    hp("pack_john_deere_s700_db", "John Deere Série S Axial & Plantadeiras DB", "Colheitadeiras", "2.0.0", 27.2, "Cerrado Precisão", "Integração completa entre colheitadeiras S770 e plantadeiras gigantes ExactEmerge DB50/DB90.", deps = listOf("pack_gps_rtk")),
    hp("pack_tanques_agua_diesel_br", "Comboios Diesel & Tanques Pipa de Campo", "Carretas e Reboques", "1.1.5", 12.8, "Logística Rural", "Sistemas móveis Randon, Noma, Mepel e Triel-HT para abastecimento de máquinas, irrigação e leite."),
    hp("pack_boiadeiras_transporte_vivo", "Carretas Boiadeiras & Manejo de Pastagens", "Animais e Pecuária", "1.2.4", 11.9, "Pecuária Forte", "Transporte bovino e ovino com carretas Boiforte/Triel-HT além de descanso programado de piquetes.", deps = listOf("pack_pecuaria_leite_pro")),
    hp("pack_florestal_eucalipto_pro", "Pacote Florestal Eucalipto & Toras Pesadas", "Máquinas Especiais", "1.3.0", 18.6, "Silvicultura Sul", "Suporte ampliado para extração de toras com carretas florestais gruadas e semirreboques tri-fueiro."),
    hp("pack_pranchas_carrega_tudo", "Pranchas Carrega-Tudo & Transporte de Máquinas", "Carretas e Reboques", "1.1.0", 13.5, "Transporte Pesado BR", "Pranchas baixas de 3 e 4 eixos Randon/Noma e suporte para transporte de plataformas de corte.", deps = listOf("pack_rodotrem_canavieiro")),
    hp("pack_irrigacao_pivos_brasil", "Pivôs Centrais & Irrigação Automatizada BR", "Gameplay e Sistemas", "2.0.0", 9.8, "HidroAgro Engenharia", "Irrigação de talhões por aspersão e pivô reduzindo estresse hídrico no inverno e estiagens.", installed = true, localPath = "builtin://hub/pack_irrigacao_pivos_brasil.json", installedAt = 1730000002000L),
    hp("pack_iluminacao_pbr_noturna", "Faróis LED, Giroflex & Iluminação Noturna PBR", "Gráficos e Iluminação", "2.0.0", 10.4, "Visual Agro", "Habilita faróis de trabalho traseiros/dianteiros, setas dinâmicas, pisca-alerta e luz interna de cabine.", installed = true, localPath = "builtin://hub/pack_iluminacao_pbr_noturna.json", installedAt = 1730000003000L),
    hp("pack_cabine_telemetria_hud", "Cabine Interativa, Mostradores & Limpadores HD", "Gráficos e Iluminação", "2.0.0", 11.1, "Farm Brasil Studio", "Painel vivo de instrumentos, limpadores na chuva e ajuste de banco em cabine.", installed = true, localPath = "builtin://hub/pack_cabine_telemetria_hud.json", installedAt = 1730000004000L),
    hp("pack_mapa_cerrado_goias", "Mapa GO — Cerrado Dourado & Pivôs", "Mapas", "1.2.0", 47.0, "AgroMT Cartografia", "Expande a região de Goiás com latossolo fértil, rios de chapada e áreas planas para alta mecanização."),
    hp("pack_mapa_matopiba_bahia", "Mapa BA — Oeste Baiano (MATOPIBA)", "Mapas", "1.1.0", 46.0, "Nordeste Agro Maps", "Grandes retas do Oeste da Bahia para algodão, soja e milho com irrigação estratégica."),
    hp("pack_economia_funcionarios_leiloes", "Gestão de Funcionários, DRE & Leilões de Usados", "Gameplay e Sistemas", "2.0.0", 7.5, "Farm Brasil Studio", "Contratação de operadores para lavoura, lances em leilão rotativo de usados e balanço financeiro DRE.", installed = true, localPath = "builtin://hub/pack_economia_funcionarios_leiloes.json", installedAt = 1730000005000L),
    hp("pack_trafego_rodoviario_vida", "Tráfego Rural Vivo, Aves & Poeira em Estradas", "Gameplay e Sistemas", "2.0.0", 8.9, "Visual Agro", "Adiciona caminhões e utilitários transitando nas rodovias, aves no céu e reação de suspensão a buracos.", installed = true, localPath = "builtin://hub/pack_trafego_rodoviario_vida.json", installedAt = 1730000006000L),
    hp("pack_escavadeiras_graders_infra", "Terraplanagem, Motoniveladoras & Carregadeiras Pro", "Máquinas Especiais", "1.1.8", 19.2, "Terraplenagem Agro", "Frota dedicada à conservação de carreadores, abertura de taipas, currais e movimentação de silagem."),
    // Mega Fase 3 (+5)
    hp("pack_slc_john_deere_historicos", "Clássicos Brasileiros SLC & John Deere 6300", "Colheitadeiras", "3.0.0", 21.6, "Memória Horizontina BR", "Pacote histórico com John Deere 6300 (4x2/4x4) e toda a linhagem SLC 2200, 6200, 6300, 7100, 7200, 7300, 7500, 7700, 1165, 1175 e 1185.", installed = true, localPath = "builtin://hub/pack_slc_john_deere_historicos.json", installedAt = 1731500001000L),
    hp("pack_floricultura_rosas_familiar", "Rosas, Morangos & Estufas da Agricultura Familiar", "Gameplay e Sistemas", "3.0.0", 9.4, "AgroFamiliar Brasil", "Cultivo de rosas de corte e morangos em pequenos sítios e estufas com beneficiamento em buquês e óleo essencial.", installed = true, localPath = "builtin://hub/pack_floricultura_rosas_familiar.json", installedAt = 1731500002000L),
    hp("pack_terceirizacao_agro_br", "Empresas de Terceirização Agrícola & Frotas Regionais", "Gameplay e Sistemas", "3.0.0", 12.2, "Farm Brasil Studio", "Sistema de contratação de prestadores terceirizados com máquinas próprias nas modalidades A, B e C e imprevistos operacionais.", installed = true, localPath = "builtin://hub/pack_terceirizacao_agro_br.json", installedAt = 1731500003000L),
    hp("pack_vilarejo_lotes_urbanos", "Vilarejos Rurais, Lotes Urbanos & Arrendamentos", "Construções", "3.0.0", 14.0, "Engenharia Rural", "Lotes urbanos edificáveis (casas, mercados, postos), arrendamento por prazo e medição em hectares, m² e alqueires do Paraná (2,42 ha).", installed = true, localPath = "builtin://hub/pack_vilarejo_lotes_urbanos.json", installedAt = 1731500004000L),
    hp("pack_central_grafica_ultra", "Central Gráfica Extrema & Turntable 3D de Catálogo", "Gráficos e Iluminação", "3.0.0", 13.5, "Visual Agro", "Resolução nativa ou máxima suportada, sombras ajustáveis, NPCs articulados e visualização multi-ângulo 3D fiel na Concessionária.", installed = true, localPath = "builtin://hub/pack_central_grafica_ultra.json", installedAt = 1731500005000L),
    // Expansão 11+ GB opcionais
    hp("pack_valmet_serie_pesada_mod", "Valmet 88 & 980 Turbo 4x4 (Frota Histórica BR)", "Tratores", "2.0.0", 285.0, "Mogi Agro Classics", "Desbloqueia Valmet 88 Clássico (82 cv) e Valmet 980 4x4 Turbo (98 cv) com modelagem PBR e escape vertical."),
    hp("pack_massey_serie_200_mod", "Massey Ferguson 275 & 290 4x4 (Linha Nacional 200)", "Tratores", "2.1.0", 310.0, "Oficina Sul", "Desbloqueia MF 275 e MF 290 4x4 com som Perkins 4 cilindros, capô vermelho clássico e faróis de trabalho."),
    hp("pack_ford_tratores_classicos_mod", "Ford 6600 & 7630 4x4 Azul Clássico", "Tratores", "1.5.0", 295.0, "AgroFord Brasil", "Desbloqueia Ford 6600 (78 cv) e Ford 7630 4x4 (105 cv) com silhueta azul Ford original e contrapesos dianteiros."),
    hp("pack_agrale_agricultura_familiar_mod", "Agrale 4100 Microtrator & BX 7215 Turbo", "Tratores", "1.8.0", 260.0, "Caxias Agro Mods", "Desbloqueia Agrale 4100 (18 cv) com som 2 cilindros e o robusto Agrale 7215 BX (215 cv) em alta resolução."),
    hp("pack_newholland_frota_br_mod", "New Holland TL75, TM150 & T7.260 Frota Nacional", "Tratores", "2.0.0", 440.0, "Cerrado Precisão", "Pacote completo New Holland azul com TL75 (78 cv), TM150 (150 cv) e T7.260 SideWinder (260 cv)."),
    hp("pack_johndeere_6j_brasil_mod", "John Deere 6110J Cabinada Nacional 4K", "Tratores", "1.4.0", 340.0, "Memória Horizontina BR", "Desbloqueia o versátil John Deere 6110J (110 cv) montado no Brasil com cabine GreenStar."),
    hp("pack_stara_imperador_autopropelido", "Stara Imperador 4000 Autopropelido Barra Central", "Máquinas Especiais", "2.0.0", 380.0, "Equipe SulAgro", "Desbloqueia o pulverizador autopropelido Stara Imperador 4000 (4.000L, barra central de 36m)."),
    hp("pack_bazucas_graneleiras_hd", "Bazucas Jan Tanker 24 & Stara Reboke 24 HD", "Carretas e Reboques", "1.6.0", 390.0, "Implementos BR", "Carretas graneleiras de transbordo de alta capacidade com pneus de flutuação e tubo articulado."),
    hp("pack_implementos_jacto_stara_hd", "Jacto Advance 3000 & Plantadeira Stara Cinderela", "Implementos", "1.5.0", 365.0, "Proteção Vegetal BR", "Desbloqueia pulverizador Jacto Advance 3000 Vortext e plantadeira articulada Stara Cinderela 15L."),
    hp("pack_feno_agropecuario_hd", "Pacote Feno Heavy: Jan Inox 3.2m & Krone BiG Pack", "Implementos", "1.3.0", 325.0, "SulForragem Mods", "Roçadeira dupla pesada Jan 3.2m e enfardadora prismática gigante Krone BiG Pack 1290 HDP II."),
    hp("pack_texturas_sul_araucarias_4k", "Bioma Sul & Araucárias — Texturas 4K & Solo Fértil", "Gráficos e Iluminação", "2.0.0", 920.0, "Visual Agro", "Pacote opcional de alta resolução para florestas de araucárias, pedra basalto, neblina matinal e rios do Sul."),
    hp("pack_texturas_cerrado_latossolo_4k", "Bioma Cerrado & Chapadões — Texturas PBR 4K", "Gráficos e Iluminação", "2.0.0", 940.0, "AgroMT Cartografia", "Poeira vermelha volumétrica, ipês floridos em 4K, estradas vicinais de cascalho e latossolo profundo."),
    hp("pack_texturas_nordeste_caatinga_4k", "Bioma Nordeste & MATOPIBA — Vegetação & Solo 4K", "Gráficos e Iluminação", "1.8.0", 860.0, "Nordeste Agro Maps", "Pacote visual de solos cristalinos, palmeiras nativas, açudes refletivos e iluminação solar intensa."),
    hp("pack_texturas_amazonia_pantanal_4k", "Bioma Amazônia & Pantanal — Águas & Matas 4K", "Gráficos e Iluminação", "1.9.0", 980.0, "Farm Brasil Studio", "Reflexos hídricos avançados em planícies alagadas, pontes de madeira detalhadas e chuvas tropicais intensas."),
    hp("pack_malhas_frota_historica_8k", "Remasterização PBR 8K — Cabines, Vidros & Pneus Reais", "Gráficos e Iluminação", "3.0.0", 1080.0, "Visual Agro", "Eleva normal maps de garras de pneus, metal escovado, vidros reflexivos e cabines para PC e celulares topo de linha."),
    hp("pack_audio_motores_diesel_hd", "Banco de Áudio Diesel Realista Multi-Cilindros HD", "Gameplay e Sistemas", "2.2.0", 640.0, "AgroSound Lab BR", "Amostras estendidas de MWM 4/6 cilindros, Perkins, Mercedes, freio a ar de caminhões e assobio de turbina sob carga."),
    hp("pack_audio_fauna_campo_br", "Paisagem Sonora Rural BR — Pássaros, Vento & Chuva HD", "Gameplay e Sistemas", "1.7.0", 480.0, "AgroSound Lab BR", "Canto de quero-quero, sabiá, cigarras, vento nos milharais e tempestades tropicais em áudio espacial."),
    hp("pack_agroindustria_modular_hd", "Complexo Agroindustrial Modular: Laticínios, Rações & Moinhos", "Construções", "2.0.0", 740.0, "Engenharia Rural", "Ativa texturas 4K para os 3 estágios de fábricas, caldeiras de biomassa, prensas de aço inox e tanques industriais."),
    hp("pack_caminhao_rodotrem_frota_hd", "Frota Rodoviária Brasileira: Graneleiros & Bitrens HD", "Caminhões", "2.0.0", 710.0, "Logística Cerrado", "Detalhamento estendido de chassis 6x4, tanques cromados, lameiros e lonas graneleiras para rotas longas."),
    hp("pack_colheitadeiras_slc_classic_hd", "Coleção Completa Colheitadeiras SLC & Série S 4K", "Colheitadeiras", "2.4.0", 790.0, "Memória Horizontina BR", "Acabamento avançado de plataformas de corte, molinetes articulados e tubos descarregadores nas séries SLC e JD S."),
    hp("pack_pecuaria_estabulo_intensivo_hd", "Confinamento Bovino, Ordenha Mecanizada & Pastos HD", "Animais e Pecuária", "1.6.0", 560.0, "Pecuária Forte", "Novos modelos de cochos, salas de ordenha e pastagens rotacionadas integradas às fábricas de ração e laticínios."),
    hp("pack_mapas_extras_serra_gaucha_4k", "Pacote Cartográfico Ultra: Relevo & Rios de Precisão HD", "Mapas", "2.0.0", 890.0, "Farm Brasil Studio", "Malhas de terreno aprimoradas com barrancos naturais, curvas de nível reais e estradas vicinais cascalhadas."),
    // Expansão Implementos Agrícolas + Plataformas de Milho (embutidos no motor)
    hp("pack_implementos_cultivadores_arados_br", "Cultivadores & Arados Nacionais Pro", "Implementos", "1.0.0", 16.4, "Implementos BR", "Desbloqueia Cultivador Quebra-Lombo Jan 5L e calibra profundidade de trabalho em entrelinhas."),
    hp("pack_plataformas_milho_safra", "Plataformas de Milho Safra Alta", "Colheitadeiras", "1.0.0", 18.2, "Equipe Cerrado", "Desbloqueia Plataforma de Milho Stara Brava+ 14 Linhas compatível com colheitadeiras axiais."),
    // Pacotes Gráficos Externos (requerem arquivo de instalação externo válido — sem download fictício)
    hp("gfx_terra_brasileira", "Terra Brasileira — Texturas 4K de Solo", "Texturas", "1.0.0", 640.0, "Farm Brasil Visual Lab", "Pacote gráfico externo para sulcos em latossolo roxo, solo arado e umidade realista.", lifecycle = ModValidationStatus.AGUARDANDO_ARQUIVOS),
    hp("gfx_vegetacao_rural", "Vegetação Rural — Grama, Culturas & Matas HD", "Gráficos", "1.0.0", 580.0, "Farm Brasil Visual Lab", "Pacote gráfico externo com folhagens densas de soja, milho, pastagem nativa e árvores regionais.", lifecycle = ModValidationStatus.AGUARDANDO_ARQUIVOS),
    hp("gfx_estradas_rurais", "Estradas Rurais — Terra, Cascalho & Asfalto 4K", "Texturas", "1.0.0", 510.0, "Farm Brasil Visual Lab", "Pacote gráfico externo para rodovias asfaltadas, acostamentos, cascalho e trilhas de barro.", lifecycle = ModValidationStatus.AGUARDANDO_ARQUIVOS),
    hp("gfx_tratores_hd", "Tratores HD — Materiais Metálicos & Cabines PBR", "Gráficos", "1.0.0", 720.0, "Farm Brasil Visual Lab", "Pacote gráfico externo de materiais metálicos, vidros reflexivos e pneus detalhados para tratores.", lifecycle = ModValidationStatus.AGUARDANDO_ARQUIVOS),
    hp("gfx_implementos_hd", "Implementos HD — Aço Agrícola & Desgaste Real", "Gráficos", "1.0.0", 490.0, "Farm Brasil Visual Lab", "Pacote gráfico externo de alta definição para discos, hastes, pintura e desgaste de implementos.", lifecycle = ModValidationStatus.AGUARDANDO_ARQUIVOS),
    hp("gfx_iluminacao_clima", "Iluminação & Clima — Sombras, Sol & Nuvens PBR", "Gráficos", "1.0.0", 430.0, "Farm Brasil Visual Lab", "Pacote gráfico externo com mapas de sombras estendidas, nuvens em camadas e chuva realista.", lifecycle = ModValidationStatus.AGUARDANDO_ARQUIVOS),
    hp("gfx_ambiente_rural", "Ambiente Rural — Águas, Galpões & Cercas 4K", "Texturas", "1.0.0", 610.0, "Farm Brasil Visual Lab", "Pacote gráfico externo para reflexos de água, madeira de currais, telhados metálicos e silos.", lifecycle = ModValidationStatus.AGUARDANDO_ARQUIVOS),
)

private fun hm(
    id: String, brand: String, machineType: String, sub: String, latest: String, changelog: String,
    compat: String, colorHex: String, vehicles: List<String> = emptyList(), imps: List<String> = emptyList(),
    mapId: String? = null, history: List<HubVersionEntry>? = null,
) = HubModMeta(id, brand, machineType, sub, latest, changelog, compat, vehicles, imps, mapId, colorHex, history)

val HUB_META_BY_PACK_ID: Map<String, HubModMeta> = listOf(
    hm("pack_cbt_classico", "CBT", "Trator Clássico Médio", "Médios", "1.1.0", "Torque ajustado em declives e detalhamento do capô e escapamento.", "Compatível FARM BRASIL 3.0+", "#EAB308", vehicles = listOf("cbt_2105_mod")),
    hm("pack_tratores_compactos_br", "Valmet/Valtra", "Tratores Compactos", "Compactos", "1.1.0", "Adiciona Agrale 575.4 e Valmet 785 com raio de giro reduzido.", "Compatível FARM BRASIL 3.0+", "#22C55E", vehicles = listOf("valmet_785_mod", "agrale_575_mod")),
    hm("pack_articulados_esteiras", "Case IH", "Articulados & Esteiras", "Esteiras & Alta Potência", "1.3.0", "Novas esteiras articuladas de alta tração e pesos frontais.", "Compatível FARM BRASIL 3.0+", "#DC2626", vehicles = listOf("case_steiger_620_mod", "fendt_1167_mt_mod")),
    hm("pack_colheitadeiras_agro", "Claas", "Colheitadeiras Especiais", "Cana & Forrageiras", "1.2.0", "Adiciona colhedora de cana Case Austoft e picadora Claas Jaguar 980.", "Compatível FARM BRASIL 3.0+", "#84CC16", vehicles = listOf("case_a8810_cana_mod", "claas_jaguar_980_mod")),
    hm("pack_abrir_campos", "Tatu Marchesan", "Abertura de Campos", "Abrir Campos", "1.3.0", "Desbloqueia Arado Aiveca 6L, Roçadeira 3m e modo Abrir/Criar Campos.", "Compatível FARM BRASIL 3.0+", "#F97316", imps = listOf("arado_abertura_pro", "rocadeira_tatu_3m")),
    hm("pack_implementos_pesados", "Baldan", "Grades e Subsoladores", "Arados/Grades/Subsoladores", "1.1.0", "Adiciona Grade Baldan CRI 44 Discos e Subsolador Piccin 7 Hastes.", "Compatível FARM BRASIL 3.0+", "#EF4444", imps = listOf("grade_baldan_cri44", "subsolador_piccin_7h")),
    hm("pack_plantio_precisao", "Stara", "Plantadeira de Precisão", "Plantadeiras", "1.2.1", "Adiciona Stara Estrela 32 Linhas para grandes talhões.", "Compatível FARM BRASIL 3.0+", "#EA580C", imps = listOf("plantadeira_stara_32l")),
    hm("pack_mapa_campos_gerais", "Farm Brasil Studio", "Mapa Regional BR", "Mapas BR", "1.1.0", "Relevo suave ondulado com campos curvos e cooperativas.", "Compatível FARM BRASIL 3.0+ (Setores 128x128)", "#2E7D32", mapId = "mod_map_campos_gerais"),
    hm("pack_mapa_parecis", "Farm Brasil Studio", "Mapa Regional BR", "Mapas BR", "1.1.0", "Chapadões do MT com talhões extensos e rios sinuosos.", "Compatível FARM BRASIL 3.0+ (Setores 128x128)", "#B45309", mapId = "mod_map_parecis_mt"),
    hm("pack_exp_pantanal", "Farm Brasil Studio", "Mapa Regional BR", "Mapas BR", "1.1.0", "Expandido com rios largos, pontes e campos de arroz e pastagens.", "Compatível FARM BRASIL 3.0+ (Setores 128x128)", "#0284C7", mapId = "mod_map_pantanal_sul"),
    hm("pack_mapa_serra_cafe", "Farm Brasil Studio", "Mapa Regional BR", "Mapas BR", "1.1.2", "Montanhas mineiras e talhões em curvas de nível.", "Compatível FARM BRASIL 3.0+ (Setores 128x128)", "#15803D", mapId = "mod_map_serra_cafe_mg"),
    hm("pack_mapa_iowa", "Farm Brasil Studio", "Mapa Internacional", "Mapas Internacionais", "1.0.4", "Solo fértil negro norte-americano e extensos milharais.", "Compatível FARM BRASIL 3.0+ (Setores 128x128)", "#3F6212", mapId = "mod_map_iowa_midwest"),
    hm("pack_mapa_bavaria", "Farm Brasil Studio", "Mapa Internacional", "Mapas Internacionais", "1.0.8", "Campos europeus irregulares em colinas e vilarejos.", "Compatível FARM BRASIL 3.0+ (Setores 128x128)", "#0F766E", mapId = "mod_map_bavaria_eu"),
    hm("pack_utilitarios_campo", "Ford/Toyota/Honda", "Caminhonetes & Motos", "Caminhonetes & Motos", "1.2.0", "Adiciona F-250 Agro 4x4, Hilux Rural e Moto NXR 160 Trilha.", "Compatível FARM BRASIL 3.0+", "#2563EB", vehicles = listOf("ford_f250_agro_mod", "hilux_rural_mod", "moto_bros_agro_mod")),
    hm("pack_rodotrem_canavieiro", "Scania", "Caminhão Pesado", "Caminhões", "1.1.0", "Adiciona Scania R540 6x4 Graneleiro BR para cargas pesadas.", "Compatível FARM BRASIL 3.0+", "#1D4ED8", vehicles = listOf("scania_r540_bitrem_mod")),
    hm("pack_silos_coop", "Farm Brasil Studio", "Estruturas & Pecuária", "Silos/Galpões/Pecuária", "1.1.0", "Reforço estrutural para silos graneleiros, galpões e cercas.", "Compatível FARM BRASIL 3.0+", "#64748B"),
    hm("pack_terra_roxa_hd", "Farm Brasil Studio", "Shaders & Texturas", "Visual & Sistemas", "1.2.0", "Melhora profundidade procedural de sulcos e solo molhado.", "Compatível FARM BRASIL 3.0+", "#7C2D12"),
    hm("pack_gps_rtk", "Farm Brasil Studio", "Agricultura de Precisão", "Visual & Sistemas", "1.2.5", "Precisão aprimorada em linhas AB e controle de seções.", "Compatível FARM BRASIL 3.0+", "#10B981", history = listOf(HubVersionEntry("1.2.5", "2026-09-12", "Corte de seção automático e traçado AB de baixa deriva."), HubVersionEntry("1.0.0", "2026-05-10", "Lançamento inicial de guiamento paralelo."))),
    hm("pack_carretas_transbordo_br", "Baldan/Randon/Noma", "Carretas & Transbordos", "Caminhões", "1.1.0", "Integração completa com transbordos Santon/Sermag e bazuca Kuhn 28.000L.", "Compatível FARM BRASIL 3.0+ (Requer Scania R540)", "#EA580C", imps = listOf("carreta_santon_cana22", "carreta_sermag_cana16", "carreta_kuhn_transbordo28", "carreta_randon_rodobasc")),
    hm("pack_especiais_construcao_br", "Farm Brasil Studio", "Linha Amarela Rural", "Visual & Sistemas", "1.0.5", "Balanceamento de torque e estabilidade hidráulica para escavadeiras e pás carregadeiras.", "Compatível FARM BRASIL 3.0+", "#D97706"),
    hm("pack_pecuaria_leite_pro", "Farm Brasil Studio", "Manejo Animal Pro", "Silos/Galpões/Pecuária", "1.1.0", "Ordenha automatizada, coleta de ovos, mistura TMR e tanque leiteiro 28.000L.", "Compatível FARM BRASIL 3.0+ (Requer Pacote Silos)", "#0284C7", imps = listOf("carreta_casale_mix20", "carreta_leite_28000")),
    hm("pack_agro_clima_pbr_wip", "Farm Brasil Studio", "Módulo Experimental v3.2", "Visual & Sistemas", "0.9.0", "Protótipo experimental de nuvens volumétricas e neblina física.", "Em desenvolvimento (Requer v3.2)", "#6366F1"),
    hm("pack_cooperativa_custom_bundle", "Farm Brasil Studio", "Pacote Externo Comunitário", "Silos/Galpões/Pecuária", "1.0.0", "Slot reservado para importação de construções externas da comunidade.", "Somente Desktop / Aguardando pacotes externos", "#94A3B8"),
    hm("pack_calcario_fertilizacao_pro", "Farm Brasil Studio", "Agronomia N-P-K & pH", "Visual & Sistemas", "2.0.0", "Análise de solo N-P-K, correção com calcário e 3 estágios de adubação.", "Integrado nativamente v3.0 Mega Fase 2", "#16A34A", imps = listOf("dist_jan_lancer15000", "dist_stara_bruttus12000", "dist_jacto_tellus10000")),
    hm("pack_pulverizadores_jacto_horsch", "Stara", "Defensivos & Pragas", "Plantadeiras", "2.0.1", "Névoa de pulverização 3D e redução de pragas e ervas daninhas.", "Compatível FARM BRASIL 3.0+", "#EA580C", imps = listOf("pulv_jacto_condor800", "pulv_jacto_columbia2000", "pulv_kuhn_metris4100", "pulv_horsch_leeb6")),
    hm("pack_feno_silagem_leite", "Claas", "Fenação & Silagem", "Cana & Forrageiras", "1.4.0", "Otimiza conversão de biomassa de capim e milho em silagem ensacada.", "Compatível FARM BRASIL 3.0+", "#65A30D", imps = listOf("segadora_claas_disco3600", "segadora_pottinger_9200", "enleirador_kuhn_ga7501", "carreta_krone_silagem44")),
    hm("pack_valmet_valtra_classicos", "Valmet/Valtra", "Série Clássica Nacional", "Médios", "1.3.2", "Curva de torque realista para motores MWM 3 e 4 cilindros.", "Compatível FARM BRASIL 3.0+", "#EAB308", vehicles = listOf("valmet_785_mod")),
    hm("pack_massey_serie_prata", "Farm Brasil Studio", "Colheitadeiras MF", "Cana & Forrageiras", "1.2.0", "Ajuste de tanque graneleiro e barra de corte em série saca-palhas e Trident.", "Compatível FARM BRASIL 3.0+", "#DC2626", vehicles = listOf("mf_3640_hist", "mf_5650_turbo_98", "mf_9695_trident")),
    hm("pack_john_deere_s700_db", "John Deere", "Combo Alta Produção", "Esteiras & Alta Potência", "2.0.0", "Integração ExactEmerge DB50/DB90 com controle automático de seções RTK.", "Compatível FARM BRASIL 3.0+ (Requer GPS RTK)", "#15803D", vehicles = listOf("jd_s770"), imps = listOf("plantadeira_jd_db50", "plantadeira_jd_db90")),
    hm("pack_tanques_agua_diesel_br", "Scania", "Logística Líquida", "Caminhões", "1.1.5", "Abastecimento em campo e cisterna inox para pulverizadores.", "Compatível FARM BRASIL 3.0+", "#0EA5E9", imps = listOf("carreta_pipa_6000", "carreta_combustivel_18000", "carreta_noma_tanque_agro")),
    hm("pack_boiadeiras_transporte_vivo", "Farm Brasil Studio", "Logística Animal", "Silos/Galpões/Pecuária", "1.2.4", "Rotação de piquetes e carretas boiadeiras de 1 e 2 pisos.", "Compatível FARM BRASIL 3.0+", "#92400E", imps = listOf("carreta_boiadeira_trac", "carreta_boiadeira_2piso")),
    hm("pack_florestal_eucalipto_pro", "Farm Brasil Studio", "Silvicultura", "Caminhões", "1.3.0", "Suporte ampliado para extração e transporte de toras de eucalipto.", "Compatível FARM BRASIL 3.0+", "#166534", imps = listOf("carreta_florestal_grua12", "carreta_florestal_rodo")),
    hm("pack_pranchas_carrega_tudo", "Scania", "Transporte Pesado", "Caminhões", "1.1.0", "Pranchas de 35t e 60t para deslocamento rodoviário de tratores de esteira.", "Compatível FARM BRASIL 3.0+", "#334155", imps = listOf("carreta_prancha_baixa35t", "carreta_prancha_pesada60t", "carreta_plataforma_header")),
    hm("pack_irrigacao_pivos_brasil", "Farm Brasil Studio", "Irrigação Agrícola", "Visual & Sistemas", "2.0.0", "Eleva umidade do solo nos talhões irrigados e protege a produtividade na seca.", "Integrado nativamente v3.0 Mega Fase 2", "#0369A1", imps = listOf("carreta_agua_inox5000")),
    hm("pack_iluminacao_pbr_noturna", "Farm Brasil Studio", "Iluminação & Sinalização", "Visual & Sistemas", "2.0.0", "Faróis de trabalho, luz interna de cabine e setas direita/esquerda/alerta.", "Integrado nativamente v3.0 Mega Fase 2", "#FACC15"),
    hm("pack_cabine_telemetria_hud", "Farm Brasil Studio", "Cabine Interativa", "Visual & Sistemas", "2.0.0", "Painel vivo de instrumentos, limpadores na chuva e regulagem de assento.", "Integrado nativamente v3.0 Mega Fase 2", "#4F46E5"),
    hm("pack_mapa_cerrado_goias", "Farm Brasil Studio", "Mapa Regional BR", "Mapas BR", "1.2.0", "Terras goianas mecanizadas com pivôs centrais.", "Compatível FARM BRASIL 3.0+", "#CA8A04", mapId = "map_go"),
    hm("pack_mapa_matopiba_bahia", "Farm Brasil Studio", "Mapa Regional BR", "Mapas BR", "1.1.0", "Expansão Oeste Baiano para soja, milho e algodão em larga escala.", "Compatível FARM BRASIL 3.0+", "#D97706", mapId = "map_ba"),
    hm("pack_economia_funcionarios_leiloes", "Farm Brasil Studio", "Gestão & Economia", "Visual & Sistemas", "2.0.0", "Funcionários operacionais, leilão de máquinas usadas e relatório DRE.", "Integrado nativamente v3.0 Mega Fase 2", "#059669"),
    hm("pack_trafego_rodoviario_vida", "Farm Brasil Studio", "Ambiente Vivo", "Visual & Sistemas", "2.0.0", "Tráfego rodoviário nas rodovias principais, pássaros e irregularidades físicas de estradas.", "Integrado nativamente v3.0 Mega Fase 2", "#10B981"),
    hm("pack_escavadeiras_graders_infra", "Farm Brasil Studio", "Infraestrutura Rural", "Visual & Sistemas", "1.1.8", "Otimiza tração de motoniveladoras e escavadeiras em estradas de terra e barragens.", "Compatível FARM BRASIL 3.0+", "#EAB308", imps = listOf("carreta_randon_basc6", "carreta_noma_basc14")),
    hm("pack_slc_john_deere_historicos", "John Deere", "Históricos Nacionais SLC/JD", "Cana & Forrageiras", "3.0.0", "Adiciona JD 6300 (4x2/4x4) e série SLC 2200, 6200, 6300, 7100, 7200, 7300, 7500, 7700, 1165, 1175 e 1185.", "Integrado nativamente v3.0 Mega Fase 3", "#2E7D32", vehicles = listOf("jd_6300_4x2_rops", "jd_6300_4x4_cab", "slc_2200_hist", "slc_6200_turbo_88", "slc_7200_turbo", "slc_7700_turbo", "slc_1175_jd")),
    hm("pack_floricultura_rosas_familiar", "Farm Brasil Studio", "Agricultura Familiar", "Visual & Sistemas", "3.0.0", "Adiciona cultivo de rosas da agricultura familiar, morango, estufas e agroindústria de buquês/essências.", "Integrado nativamente v3.0 Mega Fase 3", "#E11D48"),
    hm("pack_terceirizacao_agro_br", "Farm Brasil Studio", "Terceirização Agrícola", "Visual & Sistemas", "3.0.0", "Empresas prestadoras com frotas próprias, orçamento dinâmico e imprevistos em campo.", "Integrado nativamente v3.0 Mega Fase 3", "#F59E0B"),
    hm("pack_vilarejo_lotes_urbanos", "Farm Brasil Studio", "Terras, Alqueires & Vila", "Silos/Galpões/Pecuária", "3.0.0", "Mais de 50 talhões irregulares, lotes urbanos edificáveis e conversão para Alqueire PR (2,42 ha).", "Integrado nativamente v3.0 Mega Fase 3", "#0EA5E9"),
    hm("pack_central_grafica_ultra", "Farm Brasil Studio", "Engine 3D & Gráficos", "Visual & Sistemas", "3.0.0", "Central Gráfica Extrema, NPCs articulados com colisão e prévia 3D multi-ângulo fiel de fábrica.", "Integrado nativamente v3.0 Mega Fase 3", "#8B5CF6"),
    hm("pack_valmet_serie_pesada_mod", "Valmet/Valtra", "Tratores Históricos Valmet", "Médios", "2.0.0", "Adiciona Valmet 88 Clássico e Valmet 980 4x4 Turbo com motor MWM.", "Compatível FARM BRASIL 3.0+", "#EAB308", vehicles = listOf("valmet_88_mod", "valmet_980_4x4_mod")),
    hm("pack_massey_serie_200_mod", "Farm Brasil Studio", "Massey Série 200", "Médios", "2.1.0", "Adiciona Massey Ferguson 275 e MF 290 4x4.", "Compatível FARM BRASIL 3.0+", "#DC2626", vehicles = listOf("mf_275_hist_mod", "mf_290_4x4_mod")),
    hm("pack_ford_tratores_classicos_mod", "Ford/Toyota/Honda", "Tratores Ford Clássicos", "Médios", "1.5.0", "Adiciona Ford 6600 e Ford 7630 4x4 azul original.", "Compatível FARM BRASIL 3.0+", "#2563EB", vehicles = listOf("ford_6600_hist_mod", "ford_7630_4x4_mod")),
    hm("pack_agrale_agricultura_familiar_mod", "Agrale", "Agrale Compacto & BX", "Compactos", "1.8.0", "Adiciona Agrale 4100 (18 cv) e Agrale 7215 BX (215 cv).", "Compatível FARM BRASIL 3.0+", "#EA580C", vehicles = listOf("agrale_4100_compacto_mod", "agrale_7215_bx_mod")),
    hm("pack_newholland_frota_br_mod", "New Holland", "Frota New Holland BR", "Médios", "2.0.0", "Adiciona NH TL75, TM150 e T7.260 SideWinder.", "Compatível FARM BRASIL 3.0+", "#1D4ED8", vehicles = listOf("nh_tl75_compacto_mod", "nh_tm150_br_mod", "nh_t7_260_mod")),
    hm("pack_johndeere_6j_brasil_mod", "John Deere", "John Deere Série 6J", "Médios", "1.4.0", "Adiciona John Deere 6110J Cabinada nacional.", "Compatível FARM BRASIL 3.0+", "#16A34A", vehicles = listOf("jd_6110j_mod")),
    hm("pack_stara_imperador_autopropelido", "Stara", "Autopropelido Barra Central", "Esteiras & Alta Potência", "2.0.0", "Adiciona pulverizador autopropelido Stara Imperador 4000.", "Compatível FARM BRASIL 3.0+", "#F97316", vehicles = listOf("stara_imperador_4000_mod")),
    hm("pack_bazucas_graneleiras_hd", "Stara", "Bazucas Graneleiras", "Caminhões", "1.6.0", "Adiciona Jan Tanker 24.000L e Stara Reboke 24.000 Ninja Inox.", "Compatível FARM BRASIL 3.0+", "#15803D", imps = listOf("carreta_bazuca_jan_tanker24_mod", "carreta_bazuca_stara_reboke24_mod")),
    hm("pack_implementos_jacto_stara_hd", "Stara", "Pulverizadores & Plantadeiras", "Plantadeiras", "1.5.0", "Adiciona Jacto Advance 3000 Vortext e Stara Cinderela 15L.", "Compatível FARM BRASIL 3.0+", "#EA580C", imps = listOf("pulv_jacto_advance3000_mod", "semeadora_stara_cinderela_mod")),
    hm("pack_feno_agropecuario_hd", "Claas", "Roçadeiras & Enfardadoras", "Cana & Forrageiras", "1.3.0", "Adiciona Roçadeira Jan 3.2m e Enfardadora Krone BiG Pack 1290.", "Compatível FARM BRASIL 3.0+", "#65A30D", imps = listOf("rocadeira_jan_inox_mod", "enfardadora_krone_bigpack_mod")),
    hm("pack_implementos_cultivadores_arados_br", "Jan", "Cultivadores & Preparo", "Arados/Grades/Subsoladores", "1.0.0", "Desbloqueia Cultivador Quebra-Lombo Jan 5L para cultivo mecânico.", "Compatível FARM BRASIL 3.0+", "#16A34A", imps = listOf("cultivador_jan_canavieiro_mod")),
    hm("pack_plataformas_milho_safra", "Stara", "Plataformas de Milho", "Cana & Forrageiras", "1.0.0", "Desbloqueia Plataforma de Milho Stara Brava+ 14 Linhas.", "Compatível FARM BRASIL 3.0+", "#EA580C", imps = listOf("plataforma_milho_stara_brava_mod")),
    hm("gfx_terra_brasileira", "Farm Brasil Studio", "Pacote Gráfico Externo", "Visual & Sistemas", "1.0.0", "Texturas 4K de Terra Brasileira para o motor Mega-Realista.", "Requer arquivo externo válido (.zip/.fbpack)", "#7C2D12"),
    hm("gfx_vegetacao_rural", "Farm Brasil Studio", "Pacote Gráfico Externo", "Visual & Sistemas", "1.0.0", "Folhagens e pastagens HD para vegetação densa.", "Requer arquivo externo válido (.zip/.fbpack)", "#15803D"),
    hm("gfx_estradas_rurais", "Farm Brasil Studio", "Pacote Gráfico Externo", "Visual & Sistemas", "1.0.0", "Mapas de textura 4K para estradas de terra e asfalto.", "Requer arquivo externo válido (.zip/.fbpack)", "#57534E"),
    hm("gfx_tratores_hd", "Farm Brasil Studio", "Pacote Gráfico Externo", "Visual & Sistemas", "1.0.0", "Shaders e texturas PBR de alta definição para tratores.", "Requer arquivo externo válido (.zip/.fbpack)", "#1E7F43"),
    hm("gfx_implementos_hd", "Farm Brasil Studio", "Pacote Gráfico Externo", "Visual & Sistemas", "1.0.0", "Detalhes de aço, discos e pistões hidráulicos HD.", "Requer arquivo externo válido (.zip/.fbpack)", "#B91C1C"),
    hm("gfx_iluminacao_clima", "Farm Brasil Studio", "Pacote Gráfico Externo", "Visual & Sistemas", "1.0.0", "Iluminação atmosférica, sombras estendidas e chuva.", "Requer arquivo externo válido (.zip/.fbpack)", "#0284C7"),
    hm("gfx_ambiente_rural", "Farm Brasil Studio", "Pacote Gráfico Externo", "Visual & Sistemas", "1.0.0", "Reflexos hídricos, galpões, madeira e construções HD.", "Requer arquivo externo válido (.zip/.fbpack)", "#8B5E34"),
).associateBy { it.packId }

fun getTotalHubCatalogSizeMb(packs: List<HubPackEntity> = DEFAULT_HUB_PACKS): Double =
    (packs.sumOf { it.sizeMb } * 10.0).roundToInt() / 10.0

fun getInstalledHubSizeMb(packs: List<HubPackEntity>): Double =
    (packs.filter { it.installed }.sumOf { it.sizeMb } * 10.0).roundToInt() / 10.0

fun getModAssetDeliveryMode(pack: HubPackEntity): ModAssetDeliveryMode {
    if (pack.lifecycleState == ModValidationStatus.EM_DESENVOLVIMENTO) {
        return ModAssetDeliveryMode.WIP_BLOCKED
    }
    if (pack.isCustomImported == true ||
        pack.localPath?.startsWith("builtin://") == true ||
        pack.localPath?.startsWith("custom://") == true
    ) {
        return ModAssetDeliveryMode.BUILTIN_ENGINE
    }
    val requiresExternalFile = pack.lifecycleState == ModValidationStatus.AGUARDANDO_ARQUIVOS ||
        pack.packId.startsWith("gfx_") ||
        pack.packId == "pack_cooperativa_custom_bundle" ||
        pack.packId.contains("texturas_", ignoreCase = true) ||
        pack.packId.contains("_8k", ignoreCase = true) ||
        pack.packId.contains("_4k", ignoreCase = true)
    return if (requiresExternalFile) {
        ModAssetDeliveryMode.EXTERNAL_FILE_REQUIRED
    } else {
        ModAssetDeliveryMode.BUILTIN_ENGINE
    }
}

fun matchesHubCategory(pack: HubPackEntity, selectedCategory: String): Boolean {
    if (selectedCategory == "Todos") return true
    val meta = HUB_META_BY_PACK_ID[pack.packId]
    return when (selectedCategory) {
        "Carretas" -> pack.category == "Carretas" || pack.category == "Carretas e Reboques"
        "Gráficos" -> pack.category == "Gráficos" || pack.category == "Gráficos e Iluminação"
        "Texturas" -> pack.category == "Texturas" ||
            pack.name.contains("Textura", ignoreCase = true) ||
            pack.packId.contains("texturas_", ignoreCase = true) ||
            pack.packId.startsWith("gfx_")
        "Plantadeiras" -> pack.category == "Plantadeiras" ||
            meta?.subCategory == "Plantadeiras" ||
            pack.packId.contains("plantio", ignoreCase = true) ||
            pack.packId.contains("plantadeira", ignoreCase = true)
        "Pulverizadores" -> pack.category == "Pulverizadores" ||
            pack.packId.contains("pulv", ignoreCase = true) ||
            pack.packId.contains("imperador", ignoreCase = true)
        else -> pack.category == selectedCategory
    }
}

fun getExternalGraphicsPacks(packs: List<HubPackEntity> = DEFAULT_HUB_PACKS): List<HubPackEntity> =
    packs.filter { it.packId.startsWith("gfx_") }

fun getHubMeta(packId: String): HubModMeta = HUB_META_BY_PACK_ID[packId] ?: HubModMeta(
    packId = packId, brand = "Farm Brasil Studio", machineType = "Pacote Modular",
    subCategory = "Visual & Sistemas", latestVersion = "1.0.0",
    changelog = "Pacote modular compatível com FARM BRASIL.",
    compatibilityNote = "Compatível FARM BRASIL 3.0+",
    unlockedVehicleIds = emptyList(), unlockedImplementIds = emptyList(), unlockedMapId = null,
    previewColorHex = "#4CAF50",
    versionHistory = listOf(HubVersionEntry("1.0.0", "2026-11-01", "Pacote modular compatível.")),
)

fun computePackSignature(pack: HubPackEntity): String {
    val payload = "${pack.packId}|${pack.version}|${pack.author}|${pack.minGameVersion}|${pack.sizeMb}"
    var h = 0x811c9dc5u
    for (ch in payload) {
        h = (h xor ch.code.toUInt()) * 0x01000193u
    }
    return "fb2-${h.toString(16).padStart(8, '0')}"
}

fun detectDuplicateHubPacks(packs: List<HubPackEntity>): Pair<List<String>, List<String>> {
    val seenIds = mutableSetOf<String>()
    val seenSigs = mutableSetOf<String>()
    val dupIds = mutableListOf<String>()
    val dupSigs = mutableListOf<String>()
    for (p in packs) {
        if (!seenIds.add(p.packId)) dupIds.add(p.packId)
        val sig = p.sha256Preview ?: computePackSignature(p)
        if (!seenSigs.add(sig)) dupSigs.add(p.packId)
    }
    return dupIds to dupSigs
}

fun compareVersions(a: String, b: String): Int {
    val pa = a.split(".").mapNotNull { it.toIntOrNull() }
    val pb = b.split(".").mapNotNull { it.toIntOrNull() }
    val n = max(pa.size, pb.size)
    for (i in 0 until n) {
        val va = pa.getOrElse(i) { 0 }
        val vb = pb.getOrElse(i) { 0 }
        if (va != vb) return if (va > vb) 1 else -1
    }
    return 0
}

fun hasHubUpdateAvailable(pack: HubPackEntity): Boolean {
    if (!pack.installed) return false
    val latest = getHubMeta(pack.packId).latestVersion
    return compareVersions(latest, pack.version) > 0
}

sealed interface CustomModParseResult {
    data class Ok(val pack: HubPackEntity) : CustomModParseResult
    data class Err(val error: String) : CustomModParseResult
}

fun validateAndParseCustomModManifest(rawJson: String, existingPacks: List<HubPackEntity>): CustomModParseResult {
    return try {
        val obj = Json.parseToJsonElement(rawJson) as? JsonObject
            ?: return CustomModParseResult.Err("Manifesto inválido: objeto JSON esperado.")
        val packId = obj["packId"]?.jsonPrimitive?.content?.trim().orEmpty()
        val name = obj["name"]?.jsonPrimitive?.content?.trim().orEmpty()
        val version = obj["version"]?.jsonPrimitive?.content?.trim().orEmpty().ifEmpty { "1.0.0" }
        val author = obj["author"]?.jsonPrimitive?.content?.trim().orEmpty().ifEmpty { "Comunidade BR" }
        val rawCat = obj["category"]?.jsonPrimitive?.content.orEmpty()
        val category = if (rawCat in HUB_CATEGORIES) rawCat else "Gameplay e Sistemas"
        if (packId.length < 4 || !packId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            return CustomModParseResult.Err("packId inválido (use letras, números, \"_\" ou \"-\", mín. 4 caracteres).")
        }
        if (name.length < 3) return CustomModParseResult.Err("Nome do mod inválido.")
        if (existingPacks.any { it.packId == packId }) {
            return CustomModParseResult.Err("Duplicidade detectada: já existe um mod com packId \"$packId\".")
        }
        val minVer = obj["minGameVersion"]?.jsonPrimitive?.content?.trim().orEmpty().ifEmpty { "3.0" }
        if (compareVersions(GAME_SEMVER, minVer) < 0) {
            return CustomModParseResult.Err("Incompatível: requer jogo v$minVer (atual v$GAME_SEMVER).")
        }
        val deps = (obj["dependencies"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.content.takeIf { s -> s.isNotBlank() } } ?: emptyList()
        val sizeMb = max(0.5, minOf(512.0, obj["sizeMb"]?.jsonPrimitive?.doubleOrNull ?: 5.0))
        val desc = obj["description"]?.jsonPrimitive?.content?.trim().orEmpty().ifEmpty { "Mod personalizado validado por manifesto JSON." }
        val androidCompat = obj["androidCompatible"]?.jsonPrimitive?.booleanOrNull ?: true
        val winCompat = obj["windowsCompatible"]?.jsonPrimitive?.booleanOrNull ?: true
        val base = HubPackEntity(
            packId = packId, name = name, category = category, version = version, sizeMb = sizeMb,
            author = author, description = desc, minGameVersion = minVer, installed = true,
            localPath = "custom://hub/$packId.json", installedAtEpochMs = System.currentTimeMillis(),
            dependencies = deps, androidCompatible = androidCompat, windowsCompatible = winCompat, isCustomImported = true,
        )
        CustomModParseResult.Ok(base.copy(sha256Preview = computePackSignature(base)))
    } catch (_: Exception) {
        CustomModParseResult.Err("JSON inválido ao ler manifesto de mod.")
    }
}

fun validateModPack(
    pack: HubPackEntity,
    allPacks: List<HubPackEntity>,
    platform: String = "android",
    freeSpaceMb: Double = 2048.0,
): ModValidationResult {
    val notes = mutableListOf<String>()
    val missingDeps = (pack.dependencies ?: emptyList()).filter { depId ->
        allPacks.none { it.packId == depId && it.installed }
    }
    val depsOk = missingDeps.isEmpty()
    val versionOk = compareVersions(GAME_SEMVER, pack.minGameVersion) >= 0
    val platformOk = when (platform) {
        "android" -> pack.androidCompatible != false
        "windows" -> pack.windowsCompatible != false
        else -> true
    }
    val spaceOk = pack.installed || freeSpaceMb >= pack.sizeMb
    val deliveryMode = getModAssetDeliveryMode(pack)
    val externalMissing = !pack.installed && deliveryMode == ModAssetDeliveryMode.EXTERNAL_FILE_REQUIRED
    val integrityOk = pack.packId.isNotBlank() && pack.version.isNotBlank() && pack.name.isNotBlank() &&
        !externalMissing
    val dupConflict = allPacks.count { it.packId == pack.packId } > 1

    if (!versionOk) notes += "Requer jogo v${pack.minGameVersion} (atual: v$GAME_SEMVER)."
    if (!platformOk) notes += "Incompatível com plataforma ${platform.uppercase()}."
    if (!depsOk) notes += "Dependência(s) ausente(s): ${missingDeps.joinToString(", ")}."
    if (!spaceOk) notes += "Espaço livre insuficiente (${"%.1f".format(freeSpaceMb)} MB < ${pack.sizeMb} MB)."
    if (dupConflict) notes += "Conflito de duplicidade: packId \"${pack.packId}\" registrado mais de uma vez."
    if (deliveryMode == ModAssetDeliveryMode.WIP_BLOCKED) {
        notes += "Módulo em desenvolvimento: instalação bloqueada até conclusão."
    } else if (externalMissing) {
        notes += MOD_MISSING_FILE_ERROR
    }

    val status = when {
        deliveryMode == ModAssetDeliveryMode.WIP_BLOCKED -> ModValidationStatus.EM_DESENVOLVIMENTO
        externalMissing -> ModValidationStatus.AGUARDANDO_ARQUIVOS
        !versionOk || !platformOk || dupConflict -> ModValidationStatus.INCOMPATIVEL
        pack.installed && hasHubUpdateAvailable(pack) -> ModValidationStatus.ATUALIZACAO_DISPONIVEL
        pack.installed -> ModValidationStatus.INSTALADO
        !depsOk || !spaceOk -> ModValidationStatus.DISPONIVEL
        else -> ModValidationStatus.COMPATIVEL
    }
    val ok = integrityOk && versionOk && platformOk && spaceOk && !dupConflict &&
        deliveryMode != ModAssetDeliveryMode.WIP_BLOCKED
    return ModValidationResult(status, ok, integrityOk, depsOk, missingDeps, platformOk, versionOk, spaceOk, dupConflict, notes)
}
