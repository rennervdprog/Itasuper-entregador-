package com.example.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.fake.AppContainer
import com.example.platform.DriverBiometricAccess
import com.example.R
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: (biometricAvailable: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(650)
        // Espera a sessão persistida terminar de carregar antes de decidir a rota.
        // O fluxo anterior lia o valor inicial nulo e enviava ao login cedo demais.
        val currentUser = AppContainer.authRepository.restoreSession().getOrNull()
        if (currentUser == null) {
            onNavigateToLogin(false)
        } else {
            onNavigateToLogin(DriverBiometricAccess.isEnabledFor(currentUser.id))
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(ItaBackground),
        color = ItaBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.itasuper_driver_helmet),
                contentDescription = "ItaSuper Entregador",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(88.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ItaSuper Entregador",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ItaTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Operação de loja",
                fontSize = 14.sp,
                color = ItaTextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = ItaOrange,
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
