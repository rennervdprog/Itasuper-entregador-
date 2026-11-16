package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.DriverProfile
import com.example.platform.DriverBiometricAuthenticator
import com.example.ui.components.DriverHelmetMark
import com.example.ui.components.itaOutlinedFieldColors
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeDark
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate700
import com.example.ui.theme.ItaStatusCompleted
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (profile: DriverProfile, hasAcceptedLink: Boolean) -> Unit,
    biometricAvailable: Boolean = false,
    onBiometricSuccess: (profile: DriverProfile, hasAcceptedLink: Boolean) -> Unit = { _, _ -> },
    onNavigateToRegistration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? FragmentActivity
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var showPasswordReset by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    fun requestBiometricLogin() {
        val host = activity
        if (host == null) {
            biometricError = "Não foi possível iniciar a impressão digital neste aparelho."
            return
        }
        DriverBiometricAuthenticator.authenticate(
            activity = host,
            onSuccess = { viewModel.loginWithBiometrics(onBiometricSuccess) },
            onError = { message ->
                biometricError = if (message.isBlank()) "Não foi possível confirmar a impressão digital." else message
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ItaBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A arte do capacete é uma máscara branca: sem o tint laranja
            // ela some no fundo claro.
            DriverHelmetMark(
                tint = ItaOrange,
                contentDescription = "ItaSuper Entregador",
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PARA ENTREGADORES DE LOJA",
                style = MaterialTheme.typography.labelSmall,
                color = ItaOrangeDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Suas entregas,\ndireto ao ponto.",
                style = MaterialTheme.typography.headlineMedium,
                color = ItaTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Receba os pedidos das suas lojas, rode a rota e confirme com o código do cliente. Sem taxa, sem corrida por ponto.",
                style = MaterialTheme.typography.bodyMedium,
                color = ItaTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo E-mail
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                placeholder = { Text("Seu e-mail") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = itaOutlinedFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("input_email")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Senha
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                placeholder = { Text("Sua senha") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.testTag("btn_toggle_password")
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.login(onLoginSuccess) }
                ),
                colors = itaOutlinedFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("input_password")
            )

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage ?: "",
                    color = ItaStatusDanger,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.testTag("text_login_error")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão Entrar
            Button(
                onClick = { viewModel.login(onLoginSuccess) },
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ItaOrange,
                    disabledContainerColor = ItaOrange.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("btn_login")
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "Entrar",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Só aparece quando a biometria está de fato habilitada neste
            // aparelho (a Splash informa o valor real; antes era sempre
            // false e o botão nunca renderizava).
            if (biometricAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ItaSurface)
                        .border(1.dp, ItaSlate500.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable(enabled = !uiState.isLoading) { requestBiometricLogin() }
                        .testTag("btn_biometric_login"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = ItaOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Entrar com digital",
                        color = ItaSlate700,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (biometricError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = biometricError.orEmpty(),
                        color = ItaStatusDanger,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.testTag("text_biometric_error")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lembrar de mim + Esqueci a senha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (rememberMe) ItaOrange else Color.Transparent)
                            .border(
                                width = 1.5.dp,
                                color = if (rememberMe) ItaOrange else ItaSlate500,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rememberMe) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lembrar de mim",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ItaTextSecondary
                    )
                }
                Text(
                    text = "Esqueci a senha",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ItaOrangeDark,
                    modifier = Modifier
                        .clickable { showPasswordReset = true }
                        .testTag("btn_forgot_password")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ainda não tem conta? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ItaSlate500
                )
                Text(
                    text = "Criar conta",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = ItaOrangeDark,
                    modifier = Modifier.clickable(enabled = !uiState.isLoading) { onNavigateToRegistration() }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Ajuda no rodapé
            TextButton(
                onClick = { viewModel.setHelpDialogOpen(true) },
                modifier = Modifier.testTag("btn_need_help")
            ) {
                Text(
                    text = "Precisa de ajuda? Fale conosco",
                    color = ItaSlate500,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }

    if (uiState.isHelpDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setHelpDialogOpen(false) },
            title = {
                Text(
                    text = "Como funciona o acesso?",
                    style = MaterialTheme.typography.titleLarge,
                    color = ItaTextPrimary
                )
            },
            text = {
                Text(
                    text = "O ItaSuper Entregador é exclusivo para motoboys vinculados diretamente às lojas parceiras ItaSuper.\n\n" +
                            "Para acessar:\n" +
                            "1. Crie sua conta de entregador com seus dados.\n" +
                            "2. Entre com o mesmo e-mail e sua senha.\n" +
                            "3. Aceite o convite de uma loja parceira para começar a receber pedidos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ItaTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.setHelpDialogOpen(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange)
                ) {
                    Text("Entendido", color = Color.White)
                }
            },
            containerColor = ItaSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showPasswordReset) {
        PasswordResetDialog(
            viewModel = viewModel,
            initialEmail = uiState.email,
            onDismiss = { showPasswordReset = false }
        )
    }
}

/**
 * Recuperação de senha real via GoTrue: envia o e-mail com o link para criar
 * uma nova senha e mostra o resultado no próprio diálogo.
 */
@Composable
private fun PasswordResetDialog(
    viewModel: AuthViewModel,
    initialEmail: String,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var isSending by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackSuccess by remember { mutableStateOf(false) }

    fun sendReset() {
        if (isSending) return
        isSending = true
        feedback = null
        feedbackSuccess = false
        viewModel.requestPasswordReset(email) { success, message ->
            isSending = false
            feedbackSuccess = success
            feedback = message
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = {
            Text(
                text = "Recuperar senha",
                style = MaterialTheme.typography.titleLarge,
                color = ItaTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Informe o e-mail da sua conta. Vamos enviar um link para criar uma nova senha.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ItaTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Seu e-mail") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { sendReset() }),
                    colors = itaOutlinedFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reset_email")
                )
                if (feedback != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = feedback.orEmpty(),
                        color = if (feedbackSuccess) ItaStatusCompleted else ItaStatusDanger,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.testTag("text_reset_feedback")
                    )
                }
            }
        },
        confirmButton = {
            if (feedbackSuccess) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange)
                ) {
                    Text("Fechar", color = Color.White)
                }
            } else {
                Button(
                    onClick = { sendReset() },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                    modifier = Modifier.testTag("btn_send_reset")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Enviar e-mail", color = Color.White)
                    }
                }
            }
        },
        dismissButton = {
            if (!feedbackSuccess) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSending,
                    modifier = Modifier.testTag("btn_cancel_reset")
                ) {
                    Text("Cancelar", color = ItaSlate500)
                }
            }
        },
        containerColor = ItaSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
