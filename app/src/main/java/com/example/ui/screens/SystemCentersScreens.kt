package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.GameSettings
import com.example.domain.GraphicsPreset
import com.example.domain.StorageQuotaConfig
import com.example.state.PerfSample
import com.example.storage.StorageBreakdownReport
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionCard
import com.example.ui.components.StatBadge
import com.example.ui.theme.AgroSprout
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusInfo
import kotlin.math.roundToInt

@Composable
fun PerformanceMonitorScreen(
    perfStats: PerfSample,
    settings: GameSettings,
    onApplyPreset: (GraphicsPreset) -> Unit,
    onToggleAutoOptimize: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Monitor de Desempenho",
            subtitle = "Telemetria de renderização e ajuste dinâmico de FPS",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(
                    title = "Métricas da Sessão Atual",
                    subtitle = "Perfil ativo: ${settings.graphicsPreset.name}",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBadge("${perfStats.fps} FPS", if (perfStats.fps >= 50) StatusGreen else HarvestGold)
                        StatBadge("${perfStats.frameTimeMs} ms", StatusInfo)
                        StatBadge("${perfStats.activeCells} células", AgroSprout)
                    }
                    Text(
                        "Entidades ativas no mundo: ${perfStats.vehiclesCount} veículos • ${perfStats.implementsCount} implementos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto-Otimização Térmica (Mobile)", style = MaterialTheme.typography.titleSmall)
                            Text("Reduz sombras e partículas automaticamente se FPS cair abaixo de 40.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.autoOptimizePerformance,
                            onCheckedChange = onToggleAutoOptimize,
                        )
                    }
                }
            }
            item {
                SectionCard(title = "Atalhos de Ajuste Rápido") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { onApplyPreset(GraphicsPreset.CELULAR) },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text("Bateria 60 FPS") }
                        Button(
                            onClick = { onApplyPreset(GraphicsPreset.EQUILIBRADO) },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Equilibrado")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onApplyPreset(GraphicsPreset.ULTRA_DOS_ULTRAS) },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text("Ultra") }
                        Button(
                            onClick = { onApplyPreset(GraphicsPreset.MEGA_REALISTA) },
                            colors = ButtonDefaults.buttonColors(containerColor = HarvestGold, contentColor = Color(0xFF0E1310)),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("perf_mega_realista_btn"),
                        ) { Text("Mega-Realista", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageManagerScreen(
    quotaConfig: StorageQuotaConfig,
    computeBreakdown: () -> StorageBreakdownReport,
    onUpdateQuota: (StorageQuotaConfig) -> Unit,
    onClearBackups: () -> Int,
    onBack: () -> Unit,
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var lastMessage by remember { mutableStateOf<String?>(null) }
    val report = remember(refreshKey) { computeBreakdown() }

    fun fmtMb(bytes: Long): String = "%.2f MB".format(bytes / (1024.0 * 1024.0))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "Central de Armazenamento",
            subtitle = "Uso interno de carreiras, backups e pacotes ModHub",
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(
                    title = "Resumo de Espaço no Dispositivo",
                    subtitle = "Total dados locais: ${fmtMb(report.totalUsedBytes)} • Mods instalados: ${"%.1f".format(report.installedModPacksMb)} MB",
                ) {
                    val usedPlusModsMb = (report.totalUsedBytes / (1024.0 * 1024.0)) + report.installedModPacksMb
                    val ratio = (usedPlusModsMb / quotaConfig.mobileLimitMb.coerceAtLeast(512.0)).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = if (ratio > 0.85f) StatusDanger else AgroSprout,
                    )
                    Text(
                        "Carreiras: ${fmtMb(report.careersBytes)} • Backups SHA: ${fmtMb(report.backupsBytes)} • Configurações/Hub: ${fmtMb(report.hubAndSettingsBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    lastMessage?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = HarvestGold) }
                    Button(
                        onClick = {
                            val removed = onClearBackups()
                            refreshKey++
                            lastMessage = "$removed arquivo(s) .bak removidos com segurança."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HarvestGold, contentColor = Color(0xFF0E1310)),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("clear_backups_btn"),
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Limpar Backups Redundantes", fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                SectionCard(title = "Cota de Armazenamento de Mods") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Limitar cota em dispositivos móveis", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = quotaConfig.enforceMobileLimit,
                            onCheckedChange = { onUpdateQuota(quotaConfig.copy(enforceMobileLimit = it)) },
                        )
                    }
                    Text("Limite Mobile: ${(quotaConfig.mobileLimitMb / 1024.0).let { "%.1f".format(it) }} GB (${quotaConfig.mobileLimitMb.roundToInt()} MB)", style = MaterialTheme.typography.labelLarge, color = AgroSprout)
                    Slider(
                        value = quotaConfig.mobileLimitMb.toFloat().coerceIn(1024f, 16384f),
                        onValueChange = { onUpdateQuota(quotaConfig.copy(mobileLimitMb = it.toDouble())) },
                        valueRange = 1024f..16384f,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onUpdateQuota(quotaConfig.copy(mobileLimitMb = 8192.0, enforceMobileLimit = true)) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("quota_8gb_btn"),
                        ) {
                            Text("Padrão 8 GB (8192 MB)", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onUpdateQuota(quotaConfig.copy(mobileLimitMb = 12288.0, enforceMobileLimit = false)) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("quota_unlimited_btn"),
                        ) {
                            Text("Livre (Sem Limite)")
                        }
                    }
                }
            }
            if (report.slotDetails.isNotEmpty()) {
                item { Text("Detalhamento por Espaço (${report.slotDetails.size})", style = MaterialTheme.typography.titleMedium) }
                items(report.slotDetails, key = { it.slotIndex }) { sd ->
                    SectionCard(
                        title = "Espaço ${sd.slotIndex + 1}: ${sd.farmName}",
                        subtitle = "Tamanho: ${sd.sizeBytes / 1024} KB • ${if (sd.isCorrupted) "Corrompido" else "Íntegro"}",
                    ) { }
                }
            }
        }
    }
}
