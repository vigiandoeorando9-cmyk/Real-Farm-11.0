package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ACCESSORY_OPTIONS
import com.example.domain.CAB_OPTIONS
import com.example.domain.CONSERVATION_TIER_META
import com.example.domain.ENGINE_MODELS
import com.example.domain.ENGINE_UPGRADES
import com.example.domain.EXHAUST_OPTIONS
import com.example.domain.FarmAccountUser
import com.example.domain.HEADLIGHT_OPTIONS
import com.example.domain.PAINT_PALETTE
import com.example.domain.RIM_COLORS
import com.example.domain.TURBO_OPTIONS
import com.example.domain.WHEEL_OPTIONS
import com.example.domain.WORKSHOP_COMPONENT_LABELS
import com.example.domain.WorkshopComponent
import com.example.domain.WorldState
import com.example.domain.getVehicleDef
import com.example.domain.resolveVehicleTuning
import com.example.firebase.MultiplayerRoomDoc
import com.example.firebase.RoomChatDoc
import com.example.firebase.RoomMemberDoc
import com.example.simulation.formatBrl
import com.example.state.MpConnectionStatus
import com.example.state.UniversalWorkshopPatch
import com.example.ui.components.MachinePreviewCanvas
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionCard
import com.example.ui.components.StatBadge
import com.example.ui.theme.AgroSprout
import com.example.ui.theme.CabinCard
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusWarning
import kotlin.math.roundToInt

@Composable
fun GarageScreen(
    worldState: WorldState,
    onSwitchVehicle: (String) -> Unit,
    onOpenWorkshop: (String) -> Unit,
    onSellVehicle: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Garagem da Sede",
            subtitle = "${worldState.vehicles.size} máquinas • ${worldState.implements.size} implementos",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(worldState.vehicles, key = { it.uid }) { veh ->
                val def = getVehicleDef(veh.defId)
                val tuning = remember(veh, def) { resolveVehicleTuning(veh, def) }
                SectionCard(
                    title = "${def.brand} ${def.model}",
                    subtitle = "${tuning.effectiveHp.roundToInt()} cv • Horas: ${"%.1f".format(veh.hoursWorked)}h • Cons: ${veh.conservationPct.roundToInt()}%",
                ) {
                    MachinePreviewCanvas(
                        vehicleDef = def,
                        customPaintOverride = veh.customPaintRgb?.takeIf { it.size >= 3 }?.let {
                            com.example.domain.rgb(it[0], it[1], it[2])
                        },
                        rimColorOverride = tuning.rimRgb,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSwitchVehicle(veh.uid) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("garage_drive_${veh.uid}"),
                        ) {
                            Icon(Icons.Rounded.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Dirigir")
                        }
                        FilledTonalButton(
                            onClick = { onOpenWorkshop(veh.uid) },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Oficina")
                        }
                        OutlinedButton(
                            onClick = { onSellVehicle(veh.uid) },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text("Vender", color = StatusDanger)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkshopScreen(
    worldState: WorldState,
    focusVehicleUid: String?,
    onCustomize: (String, UniversalWorkshopPatch) -> Unit,
    onRepairFull: (String, Boolean) -> Unit,
    onRepairComp: (String, WorkshopComponent) -> Unit,
    onPreventive: (String) -> Unit,
    onWash: (String) -> Unit,
    onTow: (String) -> Unit,
    inspectQuote: (String) -> com.example.simulation.WorkshopQuote?,
    onBack: () -> Unit,
) {
    var selectedUid by remember(focusVehicleUid, worldState.vehicles) {
        mutableStateOf(
            focusVehicleUid?.takeIf { id -> worldState.vehicles.any { it.uid == id } }
                ?: worldState.activeVehicleUid
                ?: worldState.vehicles.firstOrNull()?.uid
        )
    }
    val veh = worldState.vehicles.find { it.uid == selectedUid } ?: worldState.vehicles.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Oficina Universal 10 Sistemas",
            subtitle = "Caixa: ${formatBrl(worldState.moneyBrl)}",
            onBack = onBack,
        )

        if (veh == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma máquina própria para customizar.")
            }
            return
        }

        val def = getVehicleDef(veh.defId)
        val tuning = remember(veh, def) { resolveVehicleTuning(veh, def) }
        val quote = remember(veh) { inspectQuote(veh.uid) }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Vehicle switcher
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                worldState.vehicles.forEach { v ->
                    val vd = getVehicleDef(v.defId)
                    FilterChip(
                        selected = v.uid == veh.uid,
                        onClick = { selectedUid = v.uid },
                        label = { Text("${vd.brand} ${vd.model}") },
                    )
                }
            }

            MachinePreviewCanvas(
                vehicleDef = def,
                customPaintOverride = veh.customPaintRgb?.takeIf { it.size >= 3 }?.let {
                    com.example.domain.rgb(it[0], it[1], it[2])
                },
                rimColorOverride = tuning.rimRgb,
                modifier = Modifier.fillMaxWidth().height(190.dp),
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatBadge("${tuning.effectiveHp.roundToInt()} cv (efetivo)", HarvestGold)
                StatBadge("Tração ${(tuning.tractionMultiplier * 100).roundToInt()}%", AgroSprout)
                StatBadge("Farol ${(tuning.illuminationMultiplier * 100).roundToInt()}%", StatusInfo)
            }

            // Maintenance & Inspection Quote
            quote?.let { q ->
                SectionCard(
                    title = "Manutenção & 5 Indicadores Granulares",
                    subtitle = "${CONSERVATION_TIER_META[q.tier]?.label ?: q.tier.name} • Conservação ${q.conservationPct.roundToInt()}%",
                ) {
                    LinearProgressIndicator(
                        progress = { (q.conservationPct / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (q.conservationPct < 45) StatusDanger else StatusGreen,
                    )
                    Text(
                        "Filtro Óleo ${q.granularHealths.oilFilter}% • Correias ${q.granularHealths.belts}% • Pneus ${q.granularHealths.tiresWear}% • Mangueiras ${q.granularHealths.hydraulicHoses}% • Lâminas ${q.granularHealths.cutterDiscs}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onRepairFull(veh.uid, true) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("workshop_repair_all"),
                        ) { Text("Restaurar + Abastecer") }
                        FilledTonalButton(
                            onClick = { onPreventive(veh.uid) },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text("Preventiva ${formatBrl(q.preventiveMaintenanceCostBrl)}") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onWash(veh.uid) }, modifier = Modifier.weight(1f), enabled = q.washCostBrl > 0) {
                            Text("Lavar ${formatBrl(q.washCostBrl)}")
                        }
                        OutlinedButton(onClick = { onTow(veh.uid) }, modifier = Modifier.weight(1f)) {
                            Text("Guincho Sede")
                        }
                    }
                    WorkshopComponent.entries.forEach { comp ->
                        val hp = q.componentHealths[comp] ?: 100.0
                        val cost = q.componentCostsBrl[comp] ?: 0.0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${WORKSHOP_COMPONENT_LABELS[comp]}: ${hp.roundToInt()}%", style = MaterialTheme.typography.bodySmall)
                            if (cost > 0) {
                                OutlinedButton(onClick = { onRepairComp(veh.uid, comp) }) {
                                    Text("Reparar ${formatBrl(cost)}", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Text("100%", style = MaterialTheme.typography.labelSmall, color = StatusGreen)
                            }
                        }
                    }
                }
            }

            // 10-System Universal Customization
            SectionCard(title = "Pintura & Aros") {
                Text("Pintura:", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PAINT_PALETTE.forEach { p ->
                        val active = (p.rgb == null && veh.customPaintRgb == null) ||
                            (p.rgb != null && veh.customPaintRgb?.let { it[0] == p.rgb.r && it[1] == p.rgb.g } == true)
                        FilterChip(
                            selected = active,
                            onClick = {
                                onCustomize(
                                    veh.uid,
                                    if (p.rgb == null) UniversalWorkshopPatch(clearPaint = true)
                                    else UniversalWorkshopPatch(customPaintRgb = listOf(p.rgb.r, p.rgb.g, p.rgb.b)),
                                )
                            },
                            label = { Text("${p.name} (${formatBrl(p.priceBrl)})") },
                        )
                    }
                }
                Text("Aros:", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RIM_COLORS.forEach { r ->
                        FilterChip(
                            selected = (veh.rimColorId ?: "stock") == r.id,
                            onClick = { onCustomize(veh.uid, UniversalWorkshopPatch(rimColorId = r.id)) },
                            label = { Text("${r.name} (${formatBrl(r.priceBrl)})") },
                        )
                    }
                }
            }

            WorkshopChipSection("Motorização & Remap ECU") {
                WorkshopChipRow("Motor", ENGINE_MODELS.map { Triple(it.id, it.name, it.priceBrl) }, veh.engineModelId ?: "stock") {
                    onCustomize(veh.uid, UniversalWorkshopPatch(engineModelId = it))
                }
                WorkshopChipRow("Remap", ENGINE_UPGRADES.map { Triple(it.id, it.name, it.priceBrl) }, veh.engineUpgradeId) {
                    onCustomize(veh.uid, UniversalWorkshopPatch(engineUpgradeId = it))
                }
                WorkshopChipRow("Turbo", TURBO_OPTIONS.map { Triple(it.id, it.name, it.priceBrl) }, veh.turboId ?: "stock") {
                    onCustomize(veh.uid, UniversalWorkshopPatch(turboId = it))
                }
            }

            WorkshopChipSection("Rodados, Cabine, Faróis, Escape & Acessórios") {
                WorkshopChipRow("Pneus", WHEEL_OPTIONS.map { Triple(it.id, it.name, it.priceBrl) }, veh.wheelOptionId) {
                    onCustomize(veh.uid, UniversalWorkshopPatch(wheelOptionId = it))
                }
                WorkshopChipRow("Cabine", CAB_OPTIONS.map { Triple(it.id, it.name, it.priceBrl) }, veh.cabStyleId ?: "stock") {
                    onCustomize(veh.uid, UniversalWorkshopPatch(cabStyleId = it))
                }
                WorkshopChipRow("Faróis", HEADLIGHT_OPTIONS.map { Triple(it.id, it.name, it.priceBrl) }, veh.headlightStyleId ?: "stock") {
                    onCustomize(veh.uid, UniversalWorkshopPatch(headlightStyleId = it))
                }
                WorkshopChipRow("Escape", EXHAUST_OPTIONS.map { Triple(it.id, it.name, it.priceBrl) }, veh.exhaustStyleId ?: "stock") {
                    onCustomize(veh.uid, UniversalWorkshopPatch(exhaustStyleId = it))
                }
                WorkshopChipRow("Acessórios", ACCESSORY_OPTIONS.map { Triple(it.id, it.name, it.priceBrl) }, veh.accessoryId ?: "stock") {
                    onCustomize(veh.uid, UniversalWorkshopPatch(accessoryId = it))
                }
            }
        }
    }
}

@Composable
private fun WorkshopChipSection(title: String, content: @Composable () -> Unit) {
    SectionCard(title = title) {
        content()
    }
}

@Composable
private fun WorkshopChipRow(
    label: String,
    options: List<Triple<String, String, Double>>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (id, name, price) ->
                FilterChip(
                    selected = selectedId == id,
                    onClick = { onSelect(id) },
                    label = { Text("$name (${formatBrl(price)})") },
                )
            }
        }
    }
}

@Composable
fun MultiplayerScreen(
    accountUser: FarmAccountUser?,
    openRooms: List<MultiplayerRoomDoc>,
    activeRoomCode: String?,
    roomMembers: List<RoomMemberDoc>,
    roomMessages: List<RoomChatDoc>,
    mpStatus: MpConnectionStatus,
    mpIsHost: Boolean,
    onHostRoom: (String, Boolean, Boolean) -> Unit,
    onJoinRoom: (String) -> Unit,
    onApproveMember: (String, Boolean) -> Unit,
    onSendChat: (String) -> Unit,
    onLeaveRoom: () -> Unit,
    onEnterGameplay: () -> Unit,
    onBack: () -> Unit,
) {
    var roomCodeInput by remember { mutableStateOf("BRASIL-${(100..999).random()}") }
    var sharedFarm by remember { mutableStateOf(true) }
    var pauseOnDisconnect by remember { mutableStateOf(true) }
    var chatInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Multiplayer Cooperativo Firebase",
            subtitle = "${mpStatus.label} • ${accountUser?.displayName ?: "Produtor"}",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (activeRoomCode == null) {
                item {
                    SectionCard(
                        title = "Criar ou Entrar em Sala",
                        subtitle = "Cooperação em tempo real no mesmo mapa e frota.",
                    ) {
                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.uppercase() },
                            label = { Text("Código da Sala") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("mp_room_code_input"),
                        )
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fazenda Compartilhada", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = sharedFarm, onCheckedChange = { sharedFarm = it })
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pausar ao Desconectar", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = pauseOnDisconnect, onCheckedChange = { pauseOnDisconnect = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onHostRoom(roomCodeInput, sharedFarm, pauseOnDisconnect) },
                                colors = ButtonDefaults.buttonColors(containerColor = HarvestGold, contentColor = Color(0xFF0E1310)),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("mp_host_button"),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Hospedar Sala", fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { onJoinRoom(roomCodeInput) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("mp_join_button"),
                            ) {
                                Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Entrar na Sala")
                            }
                        }
                    }
                }
                item {
                    Text("Salas Abertas Agora (${openRooms.size})", style = MaterialTheme.typography.titleMedium)
                }
                if (openRooms.isEmpty()) {
                    item {
                        SectionCard {
                            Text("Nenhuma sala aberta listada no momento. Crie uma sala acima e compartilhe o código!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(openRooms, key = { it.roomCode }) { room ->
                        SectionCard(
                            title = "Sala ${room.roomCode} • Host: ${room.hostName}",
                            subtitle = "Mapa: ${room.mapId} • Jogadores: ${room.memberCount}/${room.maxPlayers}",
                        ) {
                            Button(
                                onClick = { onJoinRoom(room.roomCode) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                Text("Conectar na Sala ${room.roomCode}")
                            }
                        }
                    }
                }
            } else {
                item {
                    SectionCard(
                        title = "Sala Ativa: $activeRoomCode (${if (mpIsHost) "HOST" else "CONVIDADO"})",
                        subtitle = "${roomMembers.size} participante(s) sincronizado(s)",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onEnterGameplay,
                                colors = ButtonDefaults.buttonColors(containerColor = AgroSprout, contentColor = Color(0xFF0E1310)),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("mp_play_now"),
                            ) {
                                Text("Ir para Campo Cooperativo", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onLeaveRoom,
                                modifier = Modifier.height(48.dp),
                            ) {
                                Text("Sair da Sala", color = StatusDanger)
                            }
                        }
                    }
                }
                item {
                    SectionCard(title = "Participantes & Telemetria") {
                        roomMembers.forEach { m ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text("${m.playerName} (${m.role})", style = MaterialTheme.typography.labelLarge)
                                    Text("Posição (${m.x.roundToInt()}, ${m.z.roundToInt()}) • ${if (m.isApproved) "Aprovado" else "Aguardando aprovação"}", style = MaterialTheme.typography.bodySmall, color = if (m.isApproved) StatusGreen else StatusWarning)
                                }
                                if (mpIsHost && m.memberUid != accountUser?.uid) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { onApproveMember(m.memberUid, true) }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Rounded.Check, contentDescription = "Aprovar", tint = StatusGreen)
                                        }
                                        IconButton(onClick = { onApproveMember(m.memberUid, false) }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Bloquear", tint = StatusDanger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    SectionCard(title = "Chat em Tempo Real da Sala") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            roomMessages.takeLast(12).forEach { msg ->
                                Surface(color = CabinCard, shape = MaterialTheme.shapes.small) {
                                    Text(
                                        text = "${msg.senderName}: ${msg.text}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Mensagem para equipe...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    onSendChat(chatInput)
                                    chatInput = ""
                                },
                                modifier = Modifier.size(48.dp).testTag("mp_send_chat"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar mensagem", tint = HarvestGold)
                            }
                        }
                    }
                }
            }
        }
    }
}
