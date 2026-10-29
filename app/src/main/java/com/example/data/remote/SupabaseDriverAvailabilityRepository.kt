package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.ItaSuperApplication
import com.example.data.model.DriverAvailability
import com.example.data.repository.DriverAvailabilityRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mantém a presença do motoboy renovada enquanto ele estiver Online.
 * A regra canônica do backend aceita presença normal até 180 minutos e
 * mantém tolerância até 183 minutos. O aplicativo renova a presença a cada
 * 8 minutos enquanto a intenção Online estiver ligada — em primeiro plano,
 * em rota e, em segundo plano, enquanto o processo viver (durante uma
 * entrega, o serviço de tracking em primeiro plano mantém o processo
 * ativo). Sair para entregar NÃO desliga a presença: quem define Offline
 * é só o próprio motoboy, pelo botão.
 */
class SupabaseDriverAvailabilityRepository : DriverAvailabilityRepository {
    private val supabase = ItaSuperSupabase.client
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val availability = MutableStateFlow(DriverAvailability(false, "Você está Offline"))
    private val preferences = ItaSuperApplication.appContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private var heartbeatJob: Job? = null
    private var foregroundRestoreJob: Job? = null
    private var routeStateKnown = false
    private var hasActiveRoute = false
    private var onlineIntent = false
    private var lastHeartbeatAtMillis: Long? = null

    init {
        scope.launch { restoreOnlinePresence() }
    }

    override fun getAvailability(): Flow<DriverAvailability> = availability.asStateFlow()

    override suspend fun setOnline(online: Boolean, hasActiveDeliveries: Boolean): Result<Unit> = runCatching {
        if (hasActiveDeliveries) {
            error("Você está em rota. Finalize suas entregas antes de alterar a disponibilidade.")
        }

        val user = supabase.auth.currentUserOrNull()
            ?: error("Sessão expirada. Entre novamente para continuar.")

        onlineIntent = online
        saveOnlineIntent(user.id, online)

        if (online) {
            ensureDriverRecord(user.id, online = true)
            publishAvailability()
            refreshPresenceIfEligible("ao ativar Online")
        } else {
            updateDriverOnline(user.id, online = false)
            stopHeartbeatLoop()
            publishAvailability()
        }
    }

    /**
     * Carrega a preferência do motoboy e o estado real do backend. A renovação
     * só começa depois que o repositório de pedidos confirmar o estado da rota.
     */
    override suspend fun restoreOnlinePresence(): Result<Unit> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("Sessão ainda sendo restaurada. Aguarde um instante.")

        val status = loadDriverStatus(userId)
        onlineIntent = readOnlineIntent(userId, fallback = status?.isOnline == true)
        publishAvailability()
        refreshPresenceIfEligible("ao restaurar sessão")
    }

    /**
     * É chamado após cada sincronização da rota. Estar em rota NÃO pausa a
     * presença: o motoboy continua presente durante a entrega (a loja segue
     * aceitando pedidos de delivery) e o heartbeat continua rodando. Ao
     * concluir a última parada, a presença é renovada se ele estiver Online.
     */
    override suspend fun setRouteActive(active: Boolean): Result<Unit> = runCatching {
        routeStateKnown = true
        hasActiveRoute = active
        publishAvailability()
        refreshPresenceIfEligible(if (active) "ao iniciar rota" else "após concluir a rota")
    }

    override fun onAppForeground() {
        foregroundRestoreJob?.cancel()
        foregroundRestoreJob = scope.launch {
            // O callback de foreground pode ocorrer antes de o Auth terminar de
            // reidratar a sessão. Repetir evita o estado Offline no primeiro
            // retorno e elimina a necessidade de minimizar uma segunda vez.
            listOf(0L, 750L, 2_000L, 4_000L, 8_000L).forEach { waitMillis ->
                if (waitMillis > 0) delay(waitMillis)
                val restored = restoreOnlinePresence()
                if (restored.isSuccess) return@launch
            }
        }
    }

    override fun onAppBackground() {
        foregroundRestoreJob?.cancel()
        foregroundRestoreJob = null
        // O loop de presença NÃO para em segundo plano (melhor esforço):
        // durante uma entrega, o serviço de tracking em primeiro plano
        // mantém o processo vivo e o heartbeat continua; fora de rota, o
        // loop segue enquanto o processo viver. Se o Android encerrar o
        // processo, a janela de 180 min do backend cobre a ausência e a
        // presença é restaurada no próximo retorno ao primeiro plano.
    }

    /**
     * Executa a melhor tentativa de marcar o motorista como Offline antes de
     * invalidar a sessão. Falhas de rede não impedem o logout nem cancelam rota.
     */
    override suspend fun prepareForLogout(): Result<Unit> {
        val result = runCatching {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@runCatching
            onlineIntent = false
            saveOnlineIntent(userId, false)
            updateDriverOnline(userId, online = false)
        }
        stopHeartbeatLoop()
        publishAvailability()
        return result
    }

    private suspend fun refreshPresenceIfEligible(trigger: String) {
        if (!routeStateKnown || !onlineIntent) {
            stopHeartbeatLoop()
            return
        }
        // Se o loop já está rodando, ele mantém o ciclo de 8 min — inclusive
        // em rota e em segundo plano. Só força uma renovação imediata quando
        // o loop ainda não começou.
        if (heartbeatJob?.isActive != true) {
            runHeartbeatSafely(trigger)
        }
        startHeartbeatLoop()
    }

    private suspend fun ensureDriverRecord(userId: String, online: Boolean) {
        val existing = loadDriverStatus(userId)
        if (existing != null) {
            updateDriverOnline(userId, online)
            return
        }

        val name = runCatching {
            supabase
                .from("profiles")
                .select(columns = Columns.list("full_name")) {
                    filter { eq("user_id", userId) }
                }
                .decodeSingle<ProfileRow>()
                .fullName
        }.getOrNull().orEmpty().ifBlank { "Motoboy" }

        supabase
            .from("drivers")
            .insert(DriverStatusUpsert(userId, online, name))
    }

    private suspend fun loadDriverStatus(userId: String): DriverStatusRow? = runCatching {
        supabase
            .from("drivers")
            .select(columns = Columns.list("user_id", "is_online")) {
                filter { eq("user_id", userId) }
            }
            .decodeSingle<DriverStatusRow>()
    }.getOrNull()

    private suspend fun updateDriverOnline(userId: String, online: Boolean) {
        supabase
            .from("drivers")
            .update({ set("is_online", online) }) {
                filter { eq("user_id", userId) }
            }
    }

    private suspend fun runHeartbeatSafely(trigger: String) {
        runCatching {
            supabase.postgrest.rpc("driver_presence_heartbeat")
        }.onSuccess {
            lastHeartbeatAtMillis = System.currentTimeMillis()
            publishAvailability()
        }.onFailure { error ->
            Log.w(TAG, "Heartbeat do entregador falhou $trigger; nova tentativa será feita no próximo ciclo.", error)
        }
    }

    private fun startHeartbeatLoop() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            // O ciclo continua em rota e em segundo plano enquanto a
            // intenção Online estiver ligada; só para quando o motoboy fica
            // Offline, faz logout ou o processo é encerrado pelo Android.
            while (isActive && onlineIntent) {
                delay(HEARTBEAT_INTERVAL_MILLIS)
                if (isActive && onlineIntent) {
                    runHeartbeatSafely("no ciclo de 8 minutos")
                }
            }
        }
    }

    private fun stopHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun publishAvailability() {
        availability.value = DriverAvailability(
            isOnline = onlineIntent,
            statusText = when {
                !onlineIntent -> "Você está Offline"
                hasActiveRoute -> "Em rota · presença renovada ${lastRenewalText()}"
                else -> "Recebendo pedidos · presença renovada ${lastRenewalText()}"
            }
        )
    }

    /** Texto honesto da última renovação confirmada pelo backend. */
    private fun lastRenewalText(): String {
        val last = lastHeartbeatAtMillis ?: return "a cada 8 min"
        val minutes = ((System.currentTimeMillis() - last) / 60_000L).toInt()
        return when {
            minutes <= 0 -> "agora há pouco"
            minutes == 1 -> "há 1 min"
            else -> "há $minutes min"
        }
    }

    private fun readOnlineIntent(userId: String, fallback: Boolean): Boolean =
        preferences.getBoolean(onlineIntentKey(userId), fallback)

    private fun saveOnlineIntent(userId: String, online: Boolean) {
        preferences.edit().putBoolean(onlineIntentKey(userId), online).apply()
    }

    private fun onlineIntentKey(userId: String) = "online_intent_$userId"

    private companion object {
        const val TAG = "DriverAvailability"
        const val PREFERENCES_NAME = "itasuper_driver_presence"
        const val HEARTBEAT_INTERVAL_MILLIS = 8 * 60_000L
    }
}
