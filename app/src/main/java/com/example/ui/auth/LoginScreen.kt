package com.example.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverProfile
import com.example.platform.DriverBiometricAuthenticator
import com.example.ui.components.DriverHelmetMark
import com.example.ui.components.itaOutlinedFieldColors
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
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
import com.example.ui.theme.ItaTextTertiary

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
        modifier = modifier
            .fillMaxSize()
            .background(ItaBackground),
        color = ItaBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(28.dp))

                // Brand Header Badge (Geometric 16dp rounded orange)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(ItaOrange, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DriverHelmetMark(
                        contentDescription = "ItaSuper Entregador",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ItaSuper Entregador",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaSlate900
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Acesso exclusivo para motoboys de lojas parceiras",
                    fontSize = 14.sp,
                    color = ItaSlate500
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Email Input
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text("E-mail cadastrado") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = com.example.ui.theme.ItaFieldIcon
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    colors = itaOutlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = { Text("Senha") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = com.example.ui.theme.ItaFieldIcon
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
                                tint = com.example.ui.theme.ItaFieldIcon
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
                    shape = RoundedCornerShape(12.dp),
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

                Spacer(modifier = Modifier.height(24.dp))

                // Login CTA Button
                Button(
                    onClick = { viewModel.login(onLoginSuccess) },
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ItaOrange,
                        disabledContainerColor = ItaOrange.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_login")
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Entrar",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (biometricAvailable) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = ::requestBiometricLogin,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_biometric_login")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = ItaOrange,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Entrar com biometria",
                            color = ItaOrange,
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

                TextButton(
                    onClick = { showRegistration = true },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "Ainda não tem conta? Criar conta de entregador",
                        color = ItaOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bottom Help Action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                TextButton(
                    onClick = { viewModel.setHelpDialogOpen(true) },
                    modifier = Modifier.testTag("btn_need_help")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = ItaSlate500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Precisa de ajuda?",
                        color = ItaSlate500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
