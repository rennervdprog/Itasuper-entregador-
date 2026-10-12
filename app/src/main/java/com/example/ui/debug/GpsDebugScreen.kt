// TODO(DEBUG): REMOVER ANTES DE PUBLICAR - tela temporária para diagnosticar o GPS tracking
package com.example.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ItaSuperSupabase
import com.example.platform.DriverTrackingDatabase
import com.example.platform.GpsDebugLog
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsDebugScreen(onBack: () -> Unit) {
    val logs by GpsDebugLog.logs.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug GPS (temporário)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { GpsDebugLog.clear() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Limpar logs")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val count = try {
                                DriverTrackingDatabase.get(context).locations().getAll().size
                            } catch (t: Throwable) {
                                -1
                            }
                            GpsDebugLog.add("Buffer offline: $count localização(ões) pendente(s)")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver buffer offline")
                }
            }

            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        GpsDebugLog.add("Teste manual de upsert iniciado...")
                        try {
                            val userId = ItaSuperSupabase.client.auth.currentUserOrNull()?.id
                            GpsDebugLog.add("Usuário autenticado: ${userId ?: "NULO (não logado!)"}")
                            if (userId == null) {
                                GpsDebugLog.add("FALHA: sem usuário autenticado, upsert impossível")
                                return@launch
                            }
                            val payload = mapOf(
                                "driver_user_id" to userId,
                                "latitude" to -23.55,
                                "longitude" to -46.63
                            )
                            GpsDebugLog.add("Enviando upsert de teste para driver_locations...")
                            ItaSuperSupabase.client.from("driver_locations").upsert(payload) {
                                onConflict = "driver_user_id"
                            }
                            GpsDebugLog.add("SUCESSO: upsert de teste funcionou!")
                        } catch (t: Throwable) {
                            GpsDebugLog.add("FALHA no upsert de teste: ${t::class.simpleName}: ${t.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF26522))
            ) {
                Text("Testar upsert manual")
            }

            Text(
                text = "Logs (${logs.size})",
                style = MaterialTheme.typography.titleSmall
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = Color(0xFF00FF88),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

suspend fun debugSyncPendingNow(context: android.content.Context) {
    withContext(Dispatchers.IO) {
        try {
            val dao = DriverTrackingDatabase.get(context).locations()
            val pending = dao.getAll()
            GpsDebugLog.add("Sync manual: ${pending.size} pendente(s) no buffer")
            var ok = 0
            for (p in pending) {
                try {
                    ItaSuperSupabase.client.from("driver_locations").upsert(
                        mapOf(
                            "driver_user_id" to p.driverUserId,
                            "order_id" to p.orderId,
                            "latitude" to p.latitude,
                            "longitude" to p.longitude,
                            "accuracy" to p.accuracy,
                            "speed" to p.speed,
                            "heading" to p.heading
                        )
                    ) {
                        onConflict = "driver_user_id"
                    }
                    dao.delete(p.orderId)
                    ok++
                } catch (t: Throwable) {
                    GpsDebugLog.add("Sync manual falhou p/ ${p.orderId}: ${t.message}")
                }
            }
            GpsDebugLog.add("Sync manual concluído: $ok/${pending.size} enviada(s)")
        } catch (t: Throwable) {
            GpsDebugLog.add("Sync manual ERRO: ${t.message}")
        }
    }
}
