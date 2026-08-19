package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.platform.DriverBiometricAccess
import com.example.platform.DriverBiometricAuthenticator
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaBorder
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaStatusDanger
import com.example.ui.theme.ItaSurface

@Composable
fun BiometricEnrollmentScreen(
    userId: String,
    onEnabled: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun enableBiometrics() {
        val host = activity
        if (host == null) {
            errorMessage = "Não foi possível iniciar a biometria neste aparelho."
            return
        }
        DriverBiometricAuthenticator.authenticate(
            activity = host,
            onSuccess = {
                DriverBiometricAccess.enableFor(userId)
                onEnabled()
            },
            onError = { message ->
                errorMessage = if (message.isBlank()) {
                    "A biometria não foi ativada. Você pode tentar novamente mais tarde."
                } else {
                    message
                }
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
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(ItaOrange, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Impressão digital",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Que tal entrar mais rápido?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ItaSlate900
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ative a biometria e entre no ItaSuper Entregador com um toque, sem digitar sua senha.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = ItaSlate500,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ItaSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ItaBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ItaOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "A sua senha não é salva. A biometria apenas protege o acesso à sessão deste aparelho.",
                        color = ItaSlate500,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = ItaStatusDanger,
                    fontSize = 13.sp,
                    modifier = Modifier.testTag("text_biometric_enrollment_error")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = ::enableBiometrics,
                colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_enable_biometric")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Ativar biometria", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSkip,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_skip_biometric")
            ) {
                Text("Agora não", color = ItaSlate500, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
