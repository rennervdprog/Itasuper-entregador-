package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.itaOutlinedFieldColors
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSurface

@Composable
fun MotoboyRegistrationScreen(
    viewModel: AuthViewModel,
    onRegistrationSuccess: (hasAcceptedLink: Boolean) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.registrationUiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize().background(ItaBackground),
        color = ItaBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TextButton(onClick = onBackToLogin, enabled = !state.isLoading) {
                Text("Voltar para entrar", color = ItaSlate500)
            }

            Text(
                text = "Criar conta de entregador",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = ItaSlate900
            )
            Text(
                text = "Preencha seus dados para criar seu perfil de motoboy no ItaSuper.",
                fontSize = 14.sp,
                color = ItaSlate500,
                lineHeight = 20.sp
            )

            RegistrationField(
                value = state.fullName,
                onValueChange = { viewModel.onRegistrationChange(fullName = it) },
                label = "Nome completo",
                icon = { Icon(Icons.Default.Person, null) }
            )
            RegistrationField(
                value = state.document,
                onValueChange = { viewModel.onRegistrationChange(document = it) },
                label = "CPF",
                icon = { Icon(Icons.Default.VerifiedUser, null) },
                keyboardType = KeyboardType.Number
            )
            RegistrationField(
                value = state.vehicle,
                onValueChange = { viewModel.onRegistrationChange(vehicle = it) },
                label = "Modelo do veículo",
                icon = { Icon(Icons.Default.TwoWheeler, null) }
            )
            RegistrationField(
                value = state.whatsapp,
                onValueChange = { viewModel.onRegistrationChange(whatsapp = it) },
                label = "WhatsApp com DDD",
                icon = { Icon(Icons.Default.Phone, null) },
                keyboardType = KeyboardType.Phone
            )
            RegistrationField(
                value = state.email,
                onValueChange = { viewModel.onRegistrationChange(email = it) },
                label = "E-mail",
                icon = { Icon(Icons.Default.Email, null) },
                keyboardType = KeyboardType.Email
            )
            PasswordRegistrationField(
                value = state.password,
                onValueChange = { viewModel.onRegistrationChange(password = it) },
                label = "Crie uma senha",
                visible = passwordVisible,
                onVisibleChange = { passwordVisible = !passwordVisible }
            )
            PasswordRegistrationField(
                value = state.passwordConfirmation,
                onValueChange = { viewModel.onRegistrationChange(passwordConfirmation = it) },
                label = "Confirme sua senha",
                visible = confirmationVisible,
                onVisibleChange = { confirmationVisible = !confirmationVisible }
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = ItaStatusDanger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = { viewModel.registerMotoboy(onRegistrationSuccess) },
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ItaOrange,
                    disabledContainerColor = ItaOrange.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("Criar conta", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                color = ItaSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Após o cadastro, você poderá aceitar um convite de uma loja parceira. Os pedidos ficam disponíveis somente depois que o vínculo for aceito.",
                    color = ItaSlate500,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RegistrationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = icon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = itaOutlinedFieldColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordRegistrationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibleChange: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, null) },
        trailingIcon = {
            IconButton(onClick = onVisibleChange) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Ocultar senha" else "Mostrar senha"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = itaOutlinedFieldColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
