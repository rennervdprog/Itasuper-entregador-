package com.example.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.fake.AppContainer
import com.example.data.model.DriverLinkStatus
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaTextPrimary
import com.example.ui.theme.ItaTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(900) // Quick smooth splash check
        val currentUser = AppContainer.authRepository.getCurrentUser().first()
        if (currentUser == null) {
            onNavigateToLogin()
        } else {
            val links = AppContainer.linkRepository.getLinks().first()
            val hasAcceptedLink = links.any { it.status == DriverLinkStatus.ACCEPTED }
            if (hasAcceptedLink) {
                onNavigateToDashboard()
            } else {
                onNavigateToOnboarding()
            }
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
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ItaOrange, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeliveryDining,
                    contentDescription = "ItaSuper",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

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
