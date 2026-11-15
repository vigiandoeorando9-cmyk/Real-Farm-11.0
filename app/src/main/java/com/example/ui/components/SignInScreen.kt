package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.auth.ACCOUNT_CODE_WARNING_TEXT
import com.example.domain.FarmAccountUser
import com.example.ui.theme.AgroEmerald
import com.example.ui.theme.AgroForest
import com.example.ui.theme.AgroLeaf
import com.example.ui.theme.AgroSprout
import com.example.ui.theme.CabinDark
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WoodDark
import com.example.ui.theme.WoodRich
import com.example.ui.theme.WoodTrim
import com.example.ui.theme.WoodWarm

enum class AccountScreenMode {
    CREATE,
    SIGN_IN_OR_RECOVER,
}

@Composable
fun SignInScreen(
    authLoading: Boolean = false,
    authError: String? = null,
    pendingWarningUser: FarmAccountUser? = null,
    hasRegisteredAccounts: Boolean = false,
    onGenerateCode: () -> String = { "FB-7K29-M4PX" },
    onCreateAccount: (username: String, personalCode: String) -> Unit = { _, _ -> },
    onSignInOrRecover: (username: String, personalCode: String) -> Unit = { _, _ -> },
    onRestoreSyncBundle: (username: String, personalCode: String, rawJson: String) -> Unit = { _, _, _ -> },
    onAcknowledgeWarning: () -> Unit = {},
    onClearError: () -> Unit = {},
) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(AccountScreenMode.CREATE) }
    var username by rememberSaveable { mutableStateOf("") }
    var personalCode by rememberSaveable { mutableStateOf("") }
    var showImportBundleField by rememberSaveable { mutableStateOf(false) }
    var syncBundleJson by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Rural landscape background
        Image(
            painter = painterResource(id = R.drawable.img_hero_farm_brasil),
            contentDescription = "Paisagem rural brasileira",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CabinDark.copy(alpha = 0.72f),
                            AgroForest.copy(alpha = 0.78f),
                            WoodDark.copy(alpha = 0.90f),
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val isWide = maxWidth >= 700.dp
            Card(
                modifier = Modifier
                    .widthIn(max = if (isWide) 880.dp else 560.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15221B).copy(alpha = 0.94f)),
                border = BorderStroke(2.dp, WoodTrim.copy(alpha = 0.75f)),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Agricultural green + timber header strip
                    Surface(
                        color = AgroForest,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Surface(
                                color = WoodRich,
                                border = BorderStroke(1.5.dp, WoodTrim),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.size(54.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Agriculture,
                                        contentDescription = "Trator Farm Brasil",
                                        tint = HarvestGold,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BEM-VINDO AO FARM BRASIL",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    modifier = Modifier.testTag("welcome_title"),
                                )
                                Text(
                                    text = "Conta própria de produtor rural • Sem Google, Gmail ou senha externa",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AgroSprout,
                                )
                            }
                            Surface(
                                color = WoodDark.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, WoodWarm.copy(alpha = 0.7f)),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Badge,
                                        contentDescription = null,
                                        tint = HarvestGold,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = if (mode == AccountScreenMode.CREATE) "Novo Produtor" else "Recuperação",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(thickness = 2.dp, color = WoodTrim.copy(alpha = 0.65f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (mode == AccountScreenMode.CREATE) {
                            Text(
                                text = "Crie seu registro de fazendeiro usando apenas seu nome e um código pessoal exclusivo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        } else {
                            Text(
                                text = "Informe exatamente o Nome do jogador e o Código pessoal para carregar ou recuperar sua conta neste dispositivo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }

                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AgroSprout,
                            unfocusedBorderColor = WoodWarm.copy(alpha = 0.7f),
                            focusedLabelColor = HarvestGold,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = HarvestGold,
                        )

                        // Nome do jogador
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                onClearError()
                            },
                            label = { Text("Nome do jogador") },
                            placeholder = { Text("Digite seu nome") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "Nome do jogador",
                                    tint = AgroSprout,
                                )
                            },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_username_input"),
                        )

                        // Código pessoal + Gerar Código button
                        if (mode == AccountScreenMode.CREATE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = personalCode,
                                    onValueChange = {
                                        personalCode = it.uppercase()
                                        onClearError()
                                    },
                                    label = { Text("Código pessoal") },
                                    placeholder = { Text("Digite ou gere seu código") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Key,
                                            contentDescription = "Código pessoal",
                                            tint = HarvestGold,
                                        )
                                    },
                                    singleLine = true,
                                    colors = fieldColors,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("account_code_input"),
                                )
                                Button(
                                    onClick = {
                                        personalCode = onGenerateCode()
                                        onClearError()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WoodRich,
                                        contentColor = HarvestGold,
                                    ),
                                    border = BorderStroke(1.dp, WoodTrim),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("generate_code_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Gerar código pessoal",
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("GERAR CÓDIGO", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = personalCode,
                                onValueChange = {
                                    personalCode = it.uppercase()
                                    onClearError()
                                },
                                label = { Text("Código pessoal") },
                                placeholder = { Text("Ex: FB-7K29-M4PX") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Key,
                                        contentDescription = "Código pessoal",
                                        tint = HarvestGold,
                                    )
                                },
                                singleLine = true,
                                colors = fieldColors,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("account_code_input"),
                            )
                        }

                        if (!authError.isNullOrBlank()) {
                            Surface(
                                color = StatusDanger.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.7f)),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("account_error_banner"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = StatusDanger)
                                    Text(
                                        text = authError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                    )
                                }
                            }
                        }

                        if (mode == AccountScreenMode.CREATE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Button(
                                    onClick = { onCreateAccount(username, personalCode) },
                                    enabled = !authLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AgroEmerald,
                                        contentColor = TextPrimary,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .testTag("create_account_button"),
                                ) {
                                    if (authLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextPrimary)
                                    } else {
                                        Icon(Icons.Rounded.Agriculture, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("CRIAR CONTA", fontWeight = FontWeight.ExtraBold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        mode = AccountScreenMode.SIGN_IN_OR_RECOVER
                                        onClearError()
                                    },
                                    enabled = !authLoading,
                                    border = BorderStroke(1.5.dp, WoodTrim),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HarvestGold),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .testTag("already_have_account_button"),
                                ) {
                                    Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("JÁ TENHO UMA CONTA", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Button(
                                    onClick = {
                                        if (showImportBundleField && syncBundleJson.isNotBlank()) {
                                            onRestoreSyncBundle(username, personalCode, syncBundleJson)
                                        } else {
                                            onSignInOrRecover(username, personalCode)
                                        }
                                    },
                                    enabled = !authLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HarvestGold,
                                        contentColor = CabinDark,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .testTag("login_account_button"),
                                ) {
                                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("ENTRAR", fontWeight = FontWeight.ExtraBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        mode = AccountScreenMode.CREATE
                                        showImportBundleField = false
                                        onClearError()
                                    },
                                    enabled = !authLoading,
                                    border = BorderStroke(1.5.dp, AgroLeaf),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .testTag("back_to_create_account_button"),
                                ) {
                                    Text("CRIAR NOVA CONTA", fontWeight = FontWeight.Bold)
                                }
                            }

                            TextButton(
                                onClick = { showImportBundleField = !showImportBundleField },
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .testTag("toggle_sync_bundle_import"),
                            ) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = AgroSprout, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (showImportBundleField) {
                                        "Ocultar restauração por arquivo de sincronização (Celular/PC)"
                                    } else {
                                        "Recuperar conta de outro dispositivo (colar pacote JSON Celular/PC)"
                                    },
                                    color = AgroSprout,
                                )
                            }

                            if (showImportBundleField) {
                                OutlinedTextField(
                                    value = syncBundleJson,
                                    onValueChange = {
                                        syncBundleJson = it
                                        onClearError()
                                    },
                                    label = { Text("Pacote de Sincronização Farm Brasil (JSON)") },
                                    placeholder = { Text("Cole aqui o conteúdo exportado do outro celular ou computador...") },
                                    minLines = 3,
                                    maxLines = 5,
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sync_bundle_json_input"),
                                )
                            }
                        }

                        // Honest local storage & future server sync notice
                        Surface(
                            color = WoodDark.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, WoodWarm.copy(alpha = 0.55f)),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudOff,
                                    contentDescription = "Armazenamento local",
                                    tint = HarvestGold,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = if (hasRegisteredAccounts) {
                                        "Seus dados e fazendas ficam salvos localmente neste dispositivo. Estrutura pronta para sincronização entre celular e computador quando o servidor Farm Brasil estiver conectado."
                                    } else {
                                        "Sem servidor online ativo no momento: sua conta, progresso, fazendas, máquinas, mods e preferências gráficas são salvos com segurança neste dispositivo, prontos para sincronização futura entre celular e PC."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // One-time personal code warning dialog after account creation
    if (pendingWarningUser != null) {
        AlertDialog(
            onDismissRequest = {}, // Mandatory explicit acknowledgment via ENTENDI
            containerColor = Color(0xFF1A2820),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = "Aviso importante",
                    tint = HarvestGold,
                    modifier = Modifier.size(34.dp),
                )
            },
            title = {
                Text(
                    text = "AVISO IMPORTANTE",
                    fontWeight = FontWeight.ExtraBold,
                    color = HarvestGold,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = ACCOUNT_CODE_WARNING_TEXT,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                    Surface(
                        color = WoodDark,
                        border = BorderStroke(1.5.dp, WoodTrim),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Jogador: ${pendingWarningUser.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AgroSprout,
                            )
                            Text(
                                text = pendingWarningUser.personalCode,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                color = HarvestGold,
                                modifier = Modifier.testTag("created_personal_code_text"),
                            )
                            TextButton(
                                onClick = {
                                    copyToClipboard(context, pendingWarningUser.personalCode)
                                    Toast.makeText(context, "Código copiado!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_personal_code_button"),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                                Spacer(Modifier.width(6.dp))
                                Text("Copiar código pessoal", color = TextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onAcknowledgeWarning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusGreen,
                        contentColor = CabinDark,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("acknowledge_code_warning_button"),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ENTENDI", fontWeight = FontWeight.ExtraBold)
                }
            },
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Código Pessoal Farm Brasil", text))
}
