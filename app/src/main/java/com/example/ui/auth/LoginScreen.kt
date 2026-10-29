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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.DriverProfile
import com.example.platform.DriverBiometricAuthenticator
import com.example.ui.components.itaOutlinedFieldColors
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaGreen
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeLight
import com.example.ui.theme.ItaSlate400
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate600
import com.example.ui.theme.ItaSlate700
import com.example.ui.theme.ItaSlate900
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
    modifier: Modifier = Modifier
) {
    var showRegistration by remember { mutableStateOf(false) }
    if (showRegistration) {
        MotoboyRegistrationScreen(
            viewModel = viewModel,
            onRegistrationSuccess = onLoginSuccess,
            onBackToLogin = { showRegistration = false },
            modifier = modifier
        )
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? FragmentActivity
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var biometricError by remember { mutableStateOf<String?>(null) }
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
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp)
                .padding(top = 40.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Badge "PARA ENTREGADORES"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(ItaOrangeLight)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ItaGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PARA ENTREGADORES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaOrange,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Título
            Text(
                text = buildAnnotatedString {
                    append("Bem-vindo de\nvolta")
                    withStyle(SpanStyle(color = ItaOrange)) { append(".") }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ItaSlate900,
                lineHeight = 36.sp,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Acesse para gerenciar suas entregas e acompanhar seus ganhos em tempo real.",
                fontSize = 14.sp,
                color = ItaSlate500,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Label + campo E-mail
            Text(
                text = "E-mail",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = ItaSlate700,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                placeholder = { Text("voce@email.com", color = ItaSlate400) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = ItaSlate400,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = itaOutlinedFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_email")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Label + campo Senha
            Text(
                text = "Senha",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = ItaSlate700,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                placeholder = { Text("••••••••", color = ItaSlate400) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ItaSlate400,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.testTag("btn_toggle_password")
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                            tint = ItaSlate400,
                            modifier = Modifier.size(18.dp)
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
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_password")
            )

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage ?: "",
                    color = ItaStatusDanger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("text_login_error")
                )
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
                                color = if (rememberMe) ItaOrange else ItaSlate400,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rememberMe) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lembrar de mim",
                        fontSize = 13.5.sp,
                        color = ItaSlate600,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Esqueci a senha",
                    fontSize = 13.5.sp,
                    color = ItaOrange,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { /* TODO: fluxo de recuperação */ }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Botão Entrar (escuro com seta laranja)
            Button(
                onClick = { viewModel.login(onLoginSuccess) },
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ItaSlate900,
                    disabledContainerColor = ItaSlate900.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ItaOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (biometricAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ItaSlate400.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable(enabled = !uiState.isLoading) { requestBiometricLogin() }
                        .padding(vertical = 16.dp)
                        .testTag("btn_biometric_login"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = ItaOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Entrar com digital",
                        color = ItaSlate700,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (biometricError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = biometricError.orEmpty(),
                        color = ItaStatusDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("text_biometric_error")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ainda não tem conta? ",
                    fontSize = 14.sp,
                    color = ItaSlate500
                )
                Text(
                    text = "Criar conta",
                    fontSize = 14.sp,
                    color = ItaOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !uiState.isLoading) { showRegistration = true }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Ajuda no rodapé
            TextButton(
                onClick = { viewModel.setHelpDialogOpen(true) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("btn_need_help")
            ) {
                Text(
                    text = "Precisa de ajuda? Fale conosco",
                    color = ItaSlate400,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
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
                    fontWeight = FontWeight.Bold,
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
                    fontSize = 14.sp,
                    color = ItaTextSecondary,
                    lineHeight = 20.sp
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
}
