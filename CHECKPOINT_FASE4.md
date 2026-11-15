# CHECKPOINT FASE 4 — Diagnóstico Técnico e Backup Pré-Atualização (400 Melhorias)

**Data do Checkpoint:** Fase 4 — Revisão Técnica, Realismo Agrícola, Física, Áudio e Correção Geral  
**Projeto Base Preservado:** FARM BRASIL 3.0 (React 19, TypeScript 5.8, Three.js, IndexedDB + SHA-256, Multiplayer Broadcast/WebSocket, ModHub 46+ pacotes, 55 Culturas, 56 Talhões, Terceirização Agrícola, 493+ Veículos e 148+ Implementos).

---

## 1. Relatório Técnico de Diagnóstico Real do Código (Item 025)

Auditoria direta realizada nos módulos `src/domain/*`, `src/simulation/*`, `src/engine3d/*`, `src/audio/*`, `src/storage/*`, `src/state/*` e `src/screens/*`:

### 1.1 Colheita, Tanque Graneleiro e Silos (`src/simulation/farmEngine.ts`)
- **Problema encontrado:** `applyHarvesterToField` adicionava `harvestedKg` diretamente ao inventário global `inventoryKg` *no mesmo tick* em que enchia `veh.grainTankFillL`, gerando acúmulo indevido sem exigir descarga real do graneleiro.
- **Ausência funcional:** Não existia ação/mecânica de descarregar o tanque da colheitadeira para uma `Carreta Graneleira`, `Caminhão Basculante` ou `Silo` próximo (`unloadGrainFromVehicle`), nem alerta de tanque cheio (`>= 95%`) bloqueando a colheita com abertura do tubo descarregador.
- **Área de corte e palhada:** A largura de corte usava caixa fixa `[-3, +3]` alinhada aos eixos X/Z ignorando a largura real da plataforma (`headerWidthFt` / categoria) e a direção frontal (`headingDeg`), e o estado `SoilState.PALHADA` pós-colheita não possuía representação 3D visível no campo.

### 1.2 Combustível, Consumo por Esforço e Abastecimento (`src/simulation/vehiclePhysics.ts`, `src/simulation/farmEngine.ts`)
- **Problema encontrado:** O consumo de combustível (`fuelBurn`) dependia apenas de `0.004 + speed * 0.0022 * tillageDragFactor`, ignorando potência do motor (`horsepower`), categoria da máquina (trator, colheitadeira, caminhão, utilitário), rotação (`rpm`), aclives (`pitchDeg` / gradiente de terreno), peso da carga rebocada, largura de trabalho do implemento e patinagem em solo mole/lama.
- **Ausência funcional:** O prédio `BuildingType.POSTO_COMBUSTIVEL` existia visualmente no mapa e para construção, mas não abastecia veículos próximos; faltava também bomba de diesel na sede/galpão/silo, telemetria de litros consumidos (`fuelSpentL`), alerta de reserva (`< 15%`), parada suave do motor ao zerar diesel e lançamento de custo operacional no DRE (`ledger`).

### 1.3 Implementos, Reservatórios e Desgaste (`src/simulation/farmEngine.ts`, `src/domain/models.ts`)
- **Problema encontrado:** Plantadeiras, pulverizadores e distribuidores consumiam `fillL` até esvaziar, sem função prática para reabastecer sementes, adubo NPK ou defensivos no pátio/depósito a partir de `inputsStockKg`. Além disso, implementos operavam por caixa alinhada aos eixos e não contabilizavam desgaste de uso (`wearPct`).

### 1.4 Iluminação, Clima, Fumaça e Física Visual 3D (`src/engine3d/FarmViewport3D.tsx`, `src/engine3d/meshFactories.ts`, `src/engine3d/textures.ts`)
- **Problema encontrado:**
  1. `sunLight` mantinha posição fixa no céu sem percorrer o arco solar Leste → Zênite → Oeste conforme `hourOfDay`.
  2. Luzes de freio (`tailLightMat`) permaneciam acesas com intensidade alta quando o veículo estava apenas estacionado (`Math.abs(speedMs) < 0.3`), em vez de responder apenas ao comando de frenagem.
  3. Ausência de campo estelar noturno, fumaça de escapamento sincronizada com carga/RPM/desgaste e luz interna da cabine ao entardecer/noite.
  4. `textures.ts` não passava `{ willReadFrequently: true }` no `canvas.getContext('2d')` de `makeCanvas`, gerando alertas de leitura em software (`getImageData`) no Chromium.

### 1.5 Lavoura Natural, Plantas Daninhas e Vegetação (`src/engine3d/FarmViewport3D.tsx`, `src/simulation/terrainGenerator.ts`)
- **Problema encontrado:**
  1. `syncCropInstances` ocultava células `PALHADA` sem renderizar restos de palhada visíveis após a colheita.
  2. Plantas daninhas (`cell.weedLevel`) existiam nos dados agronômicos mas não possuíam instâncias 3D visíveis dentro dos talhões infestados, nem impacto completo no rendimento final e controle via enxada/pulverizador/roçadeira.
  3. Culturas e copas de árvores não tinham variação sutil de altura/cor por fertilidade/umidade ou oscilação orgânica ao vento.

### 1.6 Áudio Procedural e Espacialização (`src/audio/ambientAudio.ts`, `src/state/useFarmStore.tsx`)
- **Problema encontrado:** Faltavam eventos sonoros dedicados de ignição/partida do motor, desligamento, troca de marcha, ruído diferenciado de rolamento de pneus por tipo de piso (`terra`, `cascalho`, `asfalto`, `lama`), som de descarga de grãos/engate e atenuação espacial por distância para veículos/NPCs externos.

### 1.7 Compatibilidade de Salvamentos (`src/storage/careerStorage.ts`)
- **Risco prevenido:** Saves antigos da Fase 1–3 não possuem campos novos opcionais (`fuelSpentL`, `wearPct` em implementos, `engineRunning`, `lastServiceHours`, etc.), exigindo migração automática determinística em `migrateLoaded` e `parseAndVerifyCareerImport`.

---

## 2. Escopo Preservado e Garantias
- Todos os 56 talhões, 55 culturas, 493+ veículos, 148+ implementos, 46+ pacotes do ModHub, Terceirização, Pecuária, Agroindústrias, Mapas regionais e Multiplayer permanecem intactos e compatíveis.
