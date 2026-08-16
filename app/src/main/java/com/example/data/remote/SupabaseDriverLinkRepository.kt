package com.example.data.remote

import com.example.data.model.DriverLinkStatus
import com.example.data.model.LinkedStore
import com.example.data.model.StoreDriverLink
import com.example.data.repository.DriverLinkRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class SupabaseDriverLinkRepository : DriverLinkRepository {
    private val supabase = ItaSuperSupabase.client

    override fun getLinks(): Flow<List<StoreDriverLink>> = flow {
        // O Capacitor mantém a tela em carregamento/retry quando o vínculo falha.
        // No nativo, uma falha não pode encerrar o processo: emitimos estado vazio
        // e a próxima abertura/atualização executa uma nova consulta.
        emit(runCatching { fetchLinks() }.getOrDefault(emptyList()))
    }

    override fun getHasAcceptedLink(): Flow<Boolean> = getLinks().map { links ->
        links.any { it.status == DriverLinkStatus.ACCEPTED }
    }

    override suspend fun acceptInvite(linkId: String) {
        supabase
            .from("store_drivers")
            .update({ set("status", "accepted") }) {
                filter { eq("id", linkId) }
            }
    }

    override suspend fun rejectInvite(linkId: String) {
        supabase
            .from("store_drivers")
            .delete {
                filter { eq("id", linkId) }
            }
    }

    override suspend fun checkNewInvites() {
        fetchLinks()
    }

    private suspend fun fetchLinks(): List<StoreDriverLink> {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return emptyList()

        val links = supabase
            .from("store_drivers")
            .select(columns = Columns.list("id", "store_id", "status", "created_at", "stores(name)")) {
                filter { eq("driver_user_id", userId) }
            }
            .decodeList<StoreDriverLinkRow>()

        if (links.isEmpty()) return emptyList()

        val stores = supabase
            .from("stores_driver_view")
            .select(columns = Columns.list("id", "name", "address_street", "address_neighborhood", "address_city", "latitude", "longitude", "driver_pin_autofill")) {
                filter { isIn("id", links.map { it.storeId }) }
            }
            .decodeList<StoreRow>()
            .associateBy { it.id }

        return links.map { link ->
            val store = stores[link.storeId]
            val storeName = store?.name?.takeIf { it.isNotBlank() }
                ?: link.stores?.name?.takeIf { it.isNotBlank() }
                ?: "Loja vinculada"
            StoreDriverLink(
                id = link.id,
                store = LinkedStore(
                    id = link.storeId,
                    name = storeName,
                    tradeName = storeName,
                    address = store?.addressStreet.orEmpty(),
                    neighborhood = store?.addressNeighborhood.orEmpty(),
                    city = store?.addressCity.orEmpty(),
                    phone = "",
                    cnpj = ""
                ),
                status = when (link.status.lowercase()) {
                    "accepted" -> DriverLinkStatus.ACCEPTED
                    "pending" -> DriverLinkStatus.PENDING
                    else -> DriverLinkStatus.REJECTED
                },
                invitedAt = link.createdAt.orEmpty(),
                // O schema ativo não expõe accepted_at/updated_at; created_at é a
                // única data disponível para apresentar o vínculo sem inventar dados.
                acceptedAt = if (link.status.lowercase() == "accepted") link.createdAt else null
            )
        }
    }
}
