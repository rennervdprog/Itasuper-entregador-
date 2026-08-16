package com.example.data.remote

import com.example.data.model.DriverAvailability
import com.example.data.repository.DriverAvailabilityRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupabaseDriverAvailabilityRepository : DriverAvailabilityRepository {
    private val supabase = ItaSuperSupabase.client
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val availability = MutableStateFlow(DriverAvailability(false, "Você está Offline"))

    init {
        scope.launch { runCatching { refresh() } }
    }

    override fun getAvailability(): Flow<DriverAvailability> = availability.asStateFlow()

    override suspend fun setOnline(online: Boolean, hasActiveDeliveries: Boolean): Result<Unit> = runCatching {
        if (!online && hasActiveDeliveries) {
            error("Finalize suas entregas ativas antes de ficar offline.")
        }

        val user = supabase.auth.currentUserOrNull()
            ?: error("Sessão expirada. Entre novamente para continuar.")
        val name = runCatching {
            supabase
                .from("profiles")
                .select(columns = Columns.list("full_name")) {
                    filter { eq("user_id", user.id) }
                }
                .decodeSingle<ProfileRow>()
                .fullName
        }.getOrNull().orEmpty().ifBlank { "Motoboy" }

        val hasDriverRecord = supabase
            .from("drivers")
            .select(columns = Columns.list("user_id", "is_online")) {
                filter { eq("user_id", user.id) }
            }
            .decodeList<DriverStatusRow>()
            .isNotEmpty()

        if (hasDriverRecord) {
            // O nome do entregador é protegido no banco e não faz parte da
            // ação Online/Offline. Atualizamos exclusivamente is_online.
            supabase
                .from("drivers")
                .update({ set("is_online", online) }) {
                    filter { eq("user_id", user.id) }
                }
        } else {
            // Primeiro acesso: cria a linha exigida pelo fluxo do Capacitor.
            supabase
                .from("drivers")
                .insert(DriverStatusUpsert(user.id, online, name))
        }

        availability.value = DriverAvailability(
            isOnline = online,
            statusText = if (online) "Recebendo pedidos · toque para pausar" else "Você está Offline"
        )
    }

    suspend fun refresh() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        val status = runCatching {
            supabase
                .from("drivers")
                .select(columns = Columns.list("user_id", "is_online")) {
                    filter { eq("user_id", userId) }
                }
                .decodeSingle<DriverStatusRow>()
        }.getOrNull()

        availability.value = DriverAvailability(
            isOnline = status?.isOnline == true,
            statusText = if (status?.isOnline == true) "Recebendo pedidos · toque para pausar" else "Você está Offline"
        )
    }
}
