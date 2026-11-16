package com.example.ui.auth

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.itaOutlinedFieldColors
import com.example.data.model.DriverProfile
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaDivider
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaOrangeDark
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextTertiary

@Composable
fun MotoboyRegistrationScreen(
    viewModel: AuthViewModel,
    onRegistrationSuccess: (profile: DriverProfile, hasAcceptedLink: Boolean) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.registrationUiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    var plateError by remember { mutableStateOf<String?>(null) }

    // A placa é opcional no cadastro (o repositório só a grava quando
    // preenchida), mas, se informada, precisa estar num formato válido.
    // A checagem vive na tela porque a validação do ViewModel não pode ser
    // alterada neste trabalho — o submit existente é preservado.
    fun submit() {
        if (state.vehiclePlate.isNotBlank() && !isValidPlate(state.vehiclePlate)) {
            plateError = "Informe uma placa válida, como ABC-1234 ou ABC-1D23."
            return
        }
        plateError = null
        viewModel.registerMotoboy(onRegistrationSuccess)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBackToLogin, enabled = !state.isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = null,
                        tint = ItaSlate500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Voltar para entrar",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = ItaSlate500
                    )
                }
            }

            Text(
                text = "CRIAR CONTA",
                style = MaterialTheme.typography.labelSmall,
                color = ItaOrangeDark
            )
            Text(
                text = "Criar conta\nde entregador",
                style = MaterialTheme.typography.headlineMedium,
                color = ItaTextPrimary
            )
            Text(
                text = "Preencha seus dados para criar seu perfil de motoboy no ItaSuper.",
                style = MaterialTheme.typography.bodyMedium,
                color = ItaSlate500
            )

            RegistrationSectionTitle("Você")
            RegistrationField(
                value = state.fullName,
                onValueChange = { viewModel.onRegistrationChange(fullName = it) },
                label = "Nome completo",
                placeholder = "Seu nome completo",
                icon = { Icon(Icons.Filled.Person, null) }
            )
            RegistrationField(
                value = state.document,
                onValueChange = { viewModel.onRegistrationChange(document = formatCpf(it)) },
                label = "CPF",
                placeholder = "000.000.000-00",
                icon = { Icon(Icons.Filled.VerifiedUser, null) },
                keyboardType = KeyboardType.Number
            )
            RegistrationField(
                value = state.whatsapp,
                onValueChange = { viewModel.onRegistrationChange(whatsapp = formatPhone(it)) },
                label = "WhatsApp com DDD",
                placeholder = "(22) 99999-0000",
                icon = { Icon(Icons.Filled.Phone, null) },
                keyboardType = KeyboardType.Phone
            )

            RegistrationSectionTitle("Sua moto")
            RegistrationField(
                value = state.vehicle,
                onValueChange = { viewModel.onRegistrationChange(vehicle = it) },
                label = "Modelo do veículo",
                placeholder = "Ex.: Honda CG 160",
                icon = { Icon(Icons.Filled.TwoWheeler, null) }
            )
            RegistrationField(
                value = state.vehiclePlate,
                onValueChange = {
                    plateError = null
                    viewModel.onRegistrationChange(vehiclePlate = formatPlate(it))
                },
                label = "Placa do veículo",
                placeholder = "ABC-1234",
                icon = { Icon(Icons.Filled.Badge, null) },
                errorText = plateError
            )

            RegistrationSectionTitle("Acesso")
            RegistrationField(
                value = state.email,
                onValueChange = { viewModel.onRegistrationChange(email = it) },
                label = "E-mail",
                placeholder = "Seu e-mail",
                icon = { Icon(Icons.Filled.Email, null) },
                keyboardType = KeyboardType.Email
            )
            PasswordRegistrationField(
                value = state.password,
                onValueChange = { viewModel.onRegistrationChange(password = it) },
                label = "Crie uma senha",
                placeholder = "Mínimo 6 caracteres",
                visible = passwordVisible,
                onVisibleChange = { passwordVisible = !passwordVisible }
            )
            PasswordRegistrationField(
                value = state.passwordConfirmation,
                onValueChange = { viewModel.onRegistrationChange(passwordConfirmation = it) },
                label = "Confirme sua senha",
                placeholder = "Repita a senha",
                visible = confirmationVisible,
                onVisibleChange = { confirmationVisible = !confirmationVisible }
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = ItaStatusDanger,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                )
            }

            Button(
                onClick = { submit() },
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ItaOrange,
                    disabledContainerColor = ItaOrange.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "Criar conta",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ItaDivider),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Após o cadastro, você poderá aceitar um convite de uma loja parceira. Os pedidos ficam disponíveis somente depois que o vínculo for aceito.",
                    color = ItaSlate500,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RegistrationSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = ItaTextTertiary,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
private fun RegistrationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: @Composable () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = icon,
        singleLine = true,
        isError = errorText != null,
        supportingText = errorText?.let {
            {
                Text(
                    text = it,
                    color = ItaStatusDanger,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = itaOutlinedFieldColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordRegistrationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    visible: Boolean,
    onVisibleChange: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Lock, null) },
        trailingIcon = {
            IconButton(onClick = onVisibleChange) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar senha" else "Mostrar senha"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = itaOutlinedFieldColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Máscara 000.000.000-00. O repositório filtra só os dígitos. */
private fun formatCpf(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(11)
    return buildString {
        for ((index, ch) in digits.withIndex()) {
            if (index == 3 || index == 6) append('.')
            if (index == 9) append('-')
            append(ch)
        }
    }
}

/** Máscara (00) 00000-0000 (11 dígitos) ou (00) 0000-0000 (10 dígitos). */
private fun formatPhone(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(11)
    return buildString {
        for ((index, ch) in digits.withIndex()) {
            when (index) {
                0 -> append('(')
                2 -> append(") ")
                7 -> if (digits.length > 10) append('-')
                6 -> if (digits.length <= 10) append('-')
            }
            append(ch)
        }
    }
}

/** Máscara ABC-1234: letras e dígitos, maiúsculos, hífen após o 3º caractere. */
private fun formatPlate(raw: String): String {
    val clean = raw.uppercase().filter { it in 'A'..'Z' || it.isDigit() }.take(7)
    return if (clean.length > 3) clean.take(3) + "-" + clean.drop(3) else clean
}

/** Aceita o formato antigo (ABC-1234) e o Mercosul (ABC-1D23). */
private fun isValidPlate(plate: String): Boolean {
    val clean = plate.uppercase().filter { it in 'A'..'Z' || it.isDigit() }
    if (clean.length != 7) return false
    val letters = clean.take(3).all { it in 'A'..'Z' }
    val oldFormat = letters && clean.drop(3).all(Char::isDigit)
    val mercosul = letters && clean[3].isDigit() && clean[4] in 'A'..'Z' &&
        clean.drop(5).all(Char::isDigit)
    return oldFormat || mercosul
}
