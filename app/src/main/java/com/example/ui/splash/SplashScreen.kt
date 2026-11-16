package com.example.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.fake.AppContainer
import com.example.platform.DriverBiometricAccess
import com.example.ui.components.DriverHelmetMark
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate300
import com.example.ui.theme.ItaSurface
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import com.example.ui.theme.ItaTextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    onNavigateToLogin: (biometricAvailable: Boolean) -> Unit,
    onNavigateAuthenticated: (hasAcceptedLink: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(650)
        // Espera a sessão persistida terminar de carregar antes de decidir a rota.
        // O fluxo anterior lia o valor inicial nulo e enviava ao login cedo demais.
        val currentUser = AppContainer.authRepository.restoreSession().getOrNull()
        if (currentUser == null) {
            // A biometria só é oferecida no login quando foi de fato habilitada
            // neste aparelho (antes o valor era sempre false e o botão de
            // digital nunca aparecia).
            onNavigateToLogin(DriverBiometricAccess.isEnabled())
        } else {
            // Sessão viva = entra DIRETO no app, sem passar pela tela de login
            // (2.4.11). Antes, mesmo com a sessão restaurada, a Splash mandava
            // todo mundo ao login e a "volta" dependia da biometria — sem
            // biometria cadastrada, o motoboy tinha que digitar a senha a cada
            // abertura, como se tivesse sido deslogado. O destino espelha o do
            // login por senha: decide depois da primeira consulta real de
            // store_drivers (vínculo aceito -> painel; sem vínculo -> vínculo).
            val hasLink = runCatching {
                AppContainer.linkRepository.getLinks().first().any { it.status.name == "ACCEPTED" }
            }.getOrDefault(false)
            onNavigateAuthenticated(hasLink)
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val context = LocalContext.current
    val appVersion = remember {
        // Mesmo padrão do AppUpdateChecker: PackageManager com fallback
        // silencioso — sem versão conhecida, o rodapé some.
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Throwable) {
            null
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ItaBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { it / 8 },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // A arte do capacete é uma máscara branca: sem o tint
                    // laranja ela some no fundo claro.
                    DriverHelmetMark(
                        tint = ItaOrange,
                        contentDescription = "ItaSuper Entregador",
                        modifier = Modifier.size(120.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "ItaSuper Entregador",
                        style = MaterialTheme.typography.displaySmall,
                        color = ItaTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Operação de loja",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ItaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    SplashRouteMotif()

                    Spacer(modifier = Modifier.height(24.dp))

                    CircularProgressIndicator(
                        color = ItaOrange,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            if (!appVersion.isNullOrBlank()) {
                Text(
                    text = "Versão $appVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = ItaTextTertiary,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            }
        }
    }
}

/** A linha da rota, sutil: origem laranja + duas paradas neutras. */
@Composable
private fun SplashRouteMotif() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(ItaOrange)
        )
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(3.dp)
                .background(ItaSlate300)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(ItaSurface)
                .border(2.dp, ItaSlate300, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(3.dp)
                .background(ItaSlate300)
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(ItaSurface)
                .border(2.dp, ItaSlate300, CircleShape)
        )
    }
}
