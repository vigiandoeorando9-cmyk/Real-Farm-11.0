# Farm Brasil (Android Native — Kotlin & Jetpack Compose)

Simulador agrícola brasileiro completo desenvolvido em **Kotlin + Jetpack Compose + Material 3**, com persistência em nuvem via **Firebase Firestore** e autenticação **Google Sign-In (Credential Manager)**.

## Arquitetura & Recursos Principais

- **Interface Nativa Jetpack Compose & Material 3**:
  - Suporte `enableEdgeToEdge()`, insets de sistema, tema escuro/dinâmico inspirado em cabine agrícola (`FarmBrasilTheme`), fontes locais em `res/font/`, e navegação type-safe por estado com `BackHandler`.
  - HUD imersivo sobre viewport tático em `Canvas` interativo (`GameplayViewport` + `MinimapRadar`), pedais analógicos/volante virtual, espelhamento para modo canhoto e painel modal de 11 abas (`Frota`, `Oficina`, `Loja`, `Terceirização`, `Economia`, `Indústrias`, `Pecuária`, `Construção`, `Equipe`, `Calendário` e `Contratos`).
- **20 Mapas Regionais do Brasil + 50 Slots de Expansão**:
  - Terreno procedural `128×128` por bioma brasileiro (Terra Roxa, Cerrado, Pantanal, Pampa, Amazônia, Caatinga, etc.), mapas full-screen com zoom/pan, 4 filtros de calor (culturas, umidade, pH, produtividade) e marcadores interativos.
- **53 Culturas Nacionais + Economia, Pecuária & Agroindústrias**:
  - Calendário sazonal de plantio/colheita, cotações dinâmicas, venda direta na balança, cadeias industriais, criação de rebanhos e gestão fundiária/bancária.
- **Catálogo Brasileiro de Máquinas & Oficina Universal de 10 Sistemas**:
  - Catálogo extenso (CBT, Agrale, Valmet/Valtra, John Deere, Case IH, New Holland, Fendt, Claas, Baldan, Tatu Marchesan, Stara, Piccin, Scania, Ford, Toyota, Honda), concessionária com comparador 3-way, mercado de usados/leilões, e oficina com inspeção de 5 componentes granulares, manutenção preventiva, lavagem, guincho e customização completa.
- **Persistência Híbrida & Multiplayer Cooperativo (Firebase)**:
  - Autenticação segura via **Google Sign-In (`CredentialManager`)** e `FirebaseAuth`.
  - 20 slots locais com verificação SHA-256 + exportação/importação JSON via Storage Access Framework (`SAF`) e sincronização automática na nuvem via **Firebase Firestore** com detecção/resolução de conflitos.
  - Salas multiplayer cooperativas em tempo real (`MultiplayerScreen`) com aprovação de membros pelo anfitrião e chat sincronizado no Firestore.
  - Síntese de áudio de motor/ambiente via `AudioTrack` PCM e `ContentManager` / `ModHub` com 12 categorias.

## Build & Testes (Android Gradle)

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```
