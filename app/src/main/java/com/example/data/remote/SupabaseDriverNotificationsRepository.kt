package com.example.data.remote

import com.example.data.model.SupportTicket
import com.example.data.repository.DriverNotificationsRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseDriverNotificationsRepository : DriverNotificationsRepository {
    private val supabase = ItaSuperSupabase.client

    // O app não possui medição de conectividade em tempo real. Não exibimos um
    // estado "estável" fixo que possa contradizer uma sincronização em retomada.
    override fun getConnectivityBannerVisible(): Flow<Boolean> = flow {
        emit(false)
    }

    override suspend fun dismissConnectivityBanner() = Unit

    override fun getSupportTickets(): Flow<List<SupportTicket>> = flow {
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        val tickets = runCatching {
            supabase
                .from("support_tickets")
                .select(columns = Columns.list("id", "ticket_number", "user_id", "category", "priority", "status", "subject", "first_message", "created_at")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SupportTicketRow>()
                .sortedByDescending { it.createdAt }
                .map { row ->
                    SupportTicket(
                        id = row.id,
                        subject = row.subject,
                        description = row.firstMessage,
                        category = row.category,
                        status = row.status.replace('_', ' ').replaceFirstChar(Char::uppercase),
                        createdAt = row.createdAt.orEmpty()
                    )
                }
        }.getOrDefault(emptyList())
        emit(tickets)
    }

    override suspend fun createSupportTicket(
        subject: String,
        category: String,
        description: String
    ): Result<SupportTicket> = runCatching {
        require(subject.isNotBlank()) { "Informe o assunto do chamado." }
        require(description.isNotBlank()) { "Descreva a solicitação para o suporte." }
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("Sessão expirada. Entre novamente para continuar.")
        val inserted = supabase
            .from("support_tickets")
            .insert(
                SupportTicketInsert(
                    userId = userId,
                    category = category.ifBlank { "outro" },
                    subject = subject.trim(),
                    firstMessage = description.trim()
                )
            ) {
                select(columns = Columns.list("id", "ticket_number", "user_id", "category", "priority", "status", "subject", "first_message", "created_at"))
            }
            .decodeSingle<SupportTicketRow>()

        SupportTicket(
            id = inserted.id,
            subject = inserted.subject,
            description = inserted.firstMessage,
            category = inserted.category,
            status = inserted.status.replace('_', ' ').replaceFirstChar(Char::uppercase),
            createdAt = inserted.createdAt.orEmpty()
        )
    }
}
