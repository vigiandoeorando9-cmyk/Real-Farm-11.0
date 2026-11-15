# CHECKPOINT — FARM BRASIL (MEGA FASE 3 & 500 MELHORIAS)

Checkpoint técnico consolidado antes da suíte final de compilação, lint e validação E2E em navegador.

## Sistemas Preservados & Expandidos
- **Arquitetura**: React 19 + TypeScript 5.8 + Three.js WebGL PBR + Web Audio API + IndexedDB (20 slots + SHA-256 JSON export/import) + BroadcastChannel/WebSocket Multiplayer (`useFarmStore.tsx`).
- **Mapas & Terras**:
  - 22 mapas regionais + 6 mapas ModHub + 50 expansões futuras.
  - 4 tamanhos (`64x64`, `96x96`, `128x128`, `160x160`), agora gerando **56 glebas/lotes irregulares** (`1..10`, `12..57`) incluindo lavouras mecanizadas, pastagens, silvicultura, **Sítios da Agricultura Familiar**, **Reserva Legal (APP)** e lotes residenciais/comerciais na **Vila Nova Esperança**.
  - 5 unidades agrárias (`ha`, `alq. PR = 2,42 ha`, `m²`, `quadra(s)`, `lote(s)`).
  - Arrendamento com prazos (`DIARIO`, `MENSAL`, `SAFRAL`, `ANUAL` + renovação automática) e mesclagem de talhões vizinhos próprios com remoção opcional de cercas.
- **Culturas & Agroindústrias**:
  - **55 culturas** (`ALL_CROPS`), incorporando **Rosas (Agricultura Familiar)** (`rosas_familiares`) e **Morango (Agricultura Familiar)** (`morango_familiar`), integradas à CEASA Hortifrúti e à agroindústria `ind_floricultura_rosas`.
- **Máquinas Históricas & Catálogo 3D Fiel**:
  - Catálogo histórico (`brazilianHistoricalCatalog.ts`) com modelos **John Deere 6300** (4x2 ROPS e 4x4 Cabine) e linha clássica **SLC 2200–1185**, cada qual com silhueta 3D distinta em `meshFactories.ts`.
  - Miniaturas 3D reais offscreen (`machineThumbnailRenderer.ts`) via contexto único compartilhado (`MachinePreviewCanvas`) e inspeção interativa 360° com 4 ângulos predefinidos (`MachineGalleryModal`), rotuladas honestamente como *Renderização 3D Fiel* e acompanhadas de histórico de compras (`purchaseHistory`).
  - **46 pacotes no ModHub** em 12 categorias + importador de manifesto JSON.
- **Personagem Articulado & NPCs Autônomos**:
  - Personagem principal articulado (`characterFactory.ts`) com customização de chapéu e vestimenta (`PlayerAppearanceConfig`), zero deslizamento e áudio de passos por superfície (`terra`, `asfalto`, `grama`, `lama`).
  - 12 NPCs autônomos (`npcAiEngine.ts`) com desvio de obstáculos, abrigo em chuva forte/noite e diálogo contextual.
- **Terceirização Agrícola & Modo Fazenda Zerada**:
  - 6 empresas terceirizadas (`outsourcingEngine.ts`), 20 serviços, 3 modalidades (`A`, `B`, `C`), deslocamento 3D real no mapa, alteração real de células/estoque, ocorrências resolvíveis e avaliação por estrelas.
  - Modo **Fazenda Zerada** (`R$ 0`, 0 máquinas, 0 talhões próprios) com contratos iniciais que fornecem máquinas emprestadas e recolhem ao entregar.
- **Central Gráfica Avançada**:
  - Controles funcionais em `SettingsScreen` e `FarmViewport3D.tsx` para resolução (`nativa | maxima | escala`), anisotropia (`1x`–`8x`), iluminação global, distância de animações, densidade de NPCs e modo adaptativo mobile com preservação do fallback seguro para SwiftShader (`isSoftwareGl`).
