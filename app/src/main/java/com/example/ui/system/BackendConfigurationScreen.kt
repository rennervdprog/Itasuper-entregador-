package com.example.ui.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ItaBackground
import com.example.ui.theme.ItaOrange
import com.example.ui.theme.ItaSlate500
import com.example.ui.theme.ItaSlate900
import com.example.ui.theme.ItaSurface

/**
 * Protege a abertura do aplicativo quando um APK foi gerado sem as variáveis
 * públicas necessárias para conectar ao Supabase. Nenhuma credencial é exibida.
 */
@Composable
fun BackendConfigurationScreen(
    message: String,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(ItaBackground),
        color = ItaBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = ItaOrange,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.size(20.dp))
            Text(
                text = "Atualização necessária",
                color = ItaSlate900,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "Não foi possível iniciar o ItaSuper Entregador nesta versão.",
                color = ItaSlate500,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSurface)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = ItaSlate500,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.size(18.dp))
            Text(
                text = "Instale a versão mais recente enviada pela equipe ItaSuper.",
                color = ItaSlate500,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(24.dp))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = ItaOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Fechar aplicativo", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
