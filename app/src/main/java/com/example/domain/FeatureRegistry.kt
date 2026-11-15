package com.example.domain

data class FeatureRegistryEntry(
    val section: String,
    val title: String,
    val status: FeatureStatus,
    val details: String,
)

val FEATURE_REGISTRY: List<FeatureRegistryEntry> = listOf(
    FeatureRegistryEntry(
        section = "01-02",
        title = "Arquitetura Android 3D OpenGL ES & Tamanhos Configuráveis",
        status = FeatureStatus.TESTADO,
        details = "SkyDome atmosférico, setores 64×64 até 160×160 com relevo orgânico, partículas 3D, joystick analógico touch, câmera livre configurável e remap de comandos.",
    ),
    FeatureRegistryEntry(
        section = "03",
        title = "Realismo Ambiental 4K/8K, Clima Dinâmico e Áudio 9 Canais",
        status = FeatureStatus.TESTADO,
        details = "512p até 8K PBR, 8 climas c/ chuva e neblina, Saturação/Contraste/Exposição e mixer AudioTrack independente em 9 canais (motor, roda, implemento, viola caipira, buzina, animais).",
    ),
    FeatureRegistryEntry(
        section = "04",
        title = "Menu Principal, HUD, Mapa Completo & Centrais de Sistema",
        status = FeatureStatus.TESTADO,
        details = "Minimapa, Mapa Completo c/ zoom/pan, Monitor de Desempenho 3D em tempo real com auto-otimização, Central de Armazenamento local e marchas F/N/R.",
    ),
    FeatureRegistryEntry(
        section = "05",
        title = "22 Regiões Base + 6 Mapas ModHub + 50 Expansões",
        status = FeatureStatus.TESTADO,
        details = "Tamanhos Pequeno (64×64) a Extra Grande (160×160), relevo ondulado, rios/pontes e 6 mapas extras (BR + EUA + Europa) via ModHub.",
    ),
    FeatureRegistryEntry(
        section = "06-07",
        title = "Personagem 3ª Pessoa, Abrir/Criar Campos & Escarificação",
        status = FeatureStatus.TESTADO,
        details = "Abrir e expandir novos talhões com arado/grade/subsolador/escarificador/roçadeira preservando APP + leitura de solo e estágio de cultura em tempo real.",
    ),
    FeatureRegistryEntry(
        section = "08-09",
        title = "457 Máquinas & 145 Implementos em Catálogo Expandido",
        status = FeatureStatus.TESTADO,
        details = "353 tratores, 38 colheitadeiras, 35 caminhões, 31 máquinas especiais/linha amarela (escavadeira, empilhadeira, manipulador, patrol, esteira) e 145 implementos.",
    ),
    FeatureRegistryEntry(
        section = "10-11",
        title = "55 Culturas Brasileiras em 6 Estágios de Crescimento",
        status = FeatureStatus.TESTADO,
        details = "Crescimento visual e fenológico contínuo da Semente (1/6) até Maturação Completa (6/6), plantio direto/convencional, rosas familiares, adubação e irrigação.",
    ),
    FeatureRegistryEntry(
        section = "12",
        title = "Feno, Silagem a Granel e Silagem Ensacada",
        status = FeatureStatus.TESTADO,
        details = "Corte, secagem, enleiramento, fardos redondos/retangulares e sacos configuráveis.",
    ),
    FeatureRegistryEntry(
        section = "13",
        title = "Pecuária Integrada (6 Espécies)",
        status = FeatureStatus.TESTADO,
        details = "Bovinos, leiteiras, suínos, aves, ovinos e caprinos integrados a feno, silagem e ração.",
    ),
    FeatureRegistryEntry(
        section = "14-15",
        title = "Cooperativas, Balança Rodoviária e Cadeias Industriais",
        status = FeatureStatus.TESTADO,
        details = "Pesagem bruto/tara/líquido, venda por unidade e processamento industrial real.",
    ),
    FeatureRegistryEntry(
        section = "16-18",
        title = "Economia Dinâmica, Crédito Rural, Construção & Garagem",
        status = FeatureStatus.TESTADO,
        details = "Empréstimos bancários c/ juros diários, preços dinâmicos, estruturas posicionáveis e oficina universal de 10 sistemas.",
    ),
    FeatureRegistryEntry(
        section = "19",
        title = "Farm Brasil ModHub (12 Categorias + Validador + Favoritos)",
        status = FeatureStatus.TESTADO,
        details = "46 pacotes em 12 categorias padronizadas, 8 estados de validação, checagem de dependências/plataforma e favoritos.",
    ),
    FeatureRegistryEntry(
        section = "20-21",
        title = "GPS Agrícola (Linhas AB / Seções) e Contratos Automáticos",
        status = FeatureStatus.TESTADO,
        details = "Guiamento AB, piloto automático e progressão automática de contratos ao plantar, colher, enfardar ou transportar na balança.",
    ),
    FeatureRegistryEntry(
        section = "22",
        title = "Multiplayer Online Cooperativo Firebase",
        status = FeatureStatus.TESTADO,
        details = "Salas online em tempo real com aprovação de membros pelo Host, fazenda compartilhada, chat de texto e telemetria 3D sincronizada.",
    ),
    FeatureRegistryEntry(
        section = "23",
        title = "20 Espaços de Carreira, Backup JSON e Validação SHA-256",
        status = FeatureStatus.TESTADO,
        details = "Persistência local atômica com SHA-256, sincronização em nuvem Firebase com detecção de conflitos e exportação/importação portátil JSON.",
    ),
)
