# CHECKPOINT — MEGA FASE 2 (FARM BRASIL 3.0)

**Data do Checkpoint:** Pós-Mega Fase 1 (base estável e compilável)  
**Repositório base:** `vigiandoeorando9-cmyk/Farm-Brasil-3.0` (`package.json` v2.0.0 / `GAME_SEMVER = '3.0'`)  
**Status da Base:** `npm test` (6 suítes), `npm run lint`, `npm run build` e Android `compile_applet` aprovados antes das alterações da Mega Fase 2.

---

## 1. Inventário Real da Base Atual

- **Veículos (`ALL_VEHICLES`)**: > 450 definições únicas (300+ tratores em `tractorCatalog300.ts`, colheitadeiras, caminhões, caminhonetes, utilitários/motos, linha amarela e máquinas florestais em `machineCatalog.ts` + `vehicleExpansionCatalog.ts`) cobrindo todos os 19 `VehicleBodyStyle`.
- **Implementos (`ALL_IMPLEMENTS`)**: 145+ definições únicas (arados, grades, subsoladores, escarificadores, plantadeiras, semeadoras, pulverizadores, distribuidores, carretas/reboques, segadoras, enleiradores, enfardadoras, ensiladeiras, ensacadoras e roçadeiras).
- **Culturas (`CROP_CATALOG`)**: 53 culturas brasileiras e regionais em 6 categorias, com progressão visual de 6 estágios (`SEMENTE` → `PRONTO_COLHEITA`).
- **Mapas (`MAP_CATALOG`)**: 28 mapas jogáveis/regionais + slots planejados, com suporte aos 4 tamanhos selecionáveis (`64×64`, `96×96`, `128×128`, `160×160`).
- **ModHub (`DEFAULT_HUB_PACKS`)**: 23 pacotes declarados (`hubCatalog.ts`).
- **Armazenamento**: IndexedDB (`farm_brasil_careers_db`) com checksum SHA-256 + backup automático + exportação/importação JSON + `localStorage` para configurações (`farm_brasil_settings_v2`), mods (`farm_brasil_hub_packs_v2`) e favoritos (`farm_brasil_hub_favorites_v1`).
- **Engine 3D**: Three.js `^0.176.0` com texturas procedurais em `CanvasTexture`, `meshFactories.ts` para veículos/implementos/construções/árvores, e `FarmViewport3D.tsx` com instanciamento de terreno, água, árvores, culturas, partículas de poeira/colheita/chuva e câmeras (`3ª Pessoa`, `Cabine`, `Orbital`, `Topo`, `Livre`).

---

## 2. Diagnóstico Técnico dos Gargalos Identificados

1. **Por que o ModHub estava preso em 23 mods:**
   - `DEFAULT_HUB_PACKS` em `src/domain/hubCatalog.ts` continha apenas um array estático de 23 itens.
   - `HUB_META_BY_PACK_ID` possuía metadados explícitos somente para 18 dos 23 pacotes (`pack_cbt_classico` até `pack_gps_rtk`), deixando os últimos 5 pacotes sem lista de itens desbloqueados ou changelog específico.
   - `loadHubPacks()` em `src/storage/careerStorage.ts` descartava qualquer pacote que não estivesse em `DEFAULT_HUB_PACKS` — não existia persistência de mods importados pelo usuário (`HUB_CUSTOM_PACKS_KEY`), validação de duplicidade de ID/SHA-256, histórico de versões nem barra de progresso de instalação.
2. **Qualidade PBR, Materiais e Cabine:**
   - `src/engine3d/textures.ts` gerava apenas mapas de cor difusa (`CanvasTexture` albedos) e um `bumpMap` simples de pneu, sem `normalMap`, `roughnessMap`, `metalnessMap` ou `aoMap` com `NoColorSpace`.
   - Vidros de cabine não usavam `depthWrite: false` + `MeshPhysicalMaterial` de pintura com `clearcoat` modulada por conservação/sujeira; faltavam mostradores de painel vivos, limpadores de para-brisa animados na chuva, setas/piscas/luzes de trabalho, engates 3 pontos e eixo PTO animados.
3. **Agronomia, Economia e Ambiente Vivo:**
   - Faltava análise individual de N-P-K e pH/calcário no solo, estações climáticas (`Primavera`, `Verão`, `Outono`, `Inverno`), rotação de culturas, surtos de pragas além de ervas daninhas, irrigação ativa, perda por maturação excessiva, rotação de pastagens e reprodução animal, funcionários contratáveis com assistente de campo, leilão de máquinas usadas, DRE contábil (Lucros e Perdas), inventário de insumos e conquistas.
   - Cenário não tinha tráfego rodoviário ambiente, pássaros, poças de chuva dinâmicas ou ondulações/buracos físicos em estradas de terra.
4. **Multiplayer:**
   - `MultiplayerScreen` não possuía cliente real de comunicação. Será implementado `src/realtime/multiplayerClient.ts` com protocolo real via **WebSocket (`wss://`/`ws://`)** com reconexão exponencial + sincronização real multi-aba via **`BroadcastChannel`** (sem simulações falsas por `setInterval`), permissões granulares, bate-papo, convite e log de eventos.

---

## 3. Regras de Preservação Obrigatórias

- Preservar integralmente todos os veículos (>450), implementos (145+), mapas, 23 mods existentes, contratos, oficinas, indústrias e saves v1/v2 (com migração automática v2→v3 no `loadCareerSlot`).
- Não criar botões decorativos nem fingir conexão multiplayer em nuvem quando desconectado.
- Todo texto, UI e documentação em **Português do Brasil (`pt-BR`)**.
