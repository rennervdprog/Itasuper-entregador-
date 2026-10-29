package com.example.data.remote

import com.example.data.model.SupportTicket
import com.example.data.repository.DriverNotificationsRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseDriverNotificationsRepository : DriverNotificationsRepository {
    private val supabase = ItaSuperSupabase.client

    // getConnectivityBannerVisible/dismissConnectivityBanner removidos
    // (auditoria B13): emitiam false fixo p/ uma faixa "Sinal de dados
    // estável" que nunca aparecia — código morto nos dois lados.

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
                        createdAt = formatTicketDate(row.createdAt)
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
                    category = supportCategoryForDatabase(category),
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
            createdAt = formatTicketDate(inserted.createdAt)
        )
    }

    /**
     * A tela exibe categorias amigáveis, mas `support_tickets.category` é o
     * enum `ticket_category` do banco. Gravar o texto livre faz o Postgres
     * rejeitar o insert (auditoria C1), então a conversão acontece aqui, na
     * escrita. Mapeamento das categorias exibidas:
     * - "Dúvida sobre taxa de entrega"     -> financeiro (taxa/valores do entregador)
     * - "Problema de endereço ou cliente"  -> entrega (ocorrência durante a entrega)
     * - "Vínculo com nova loja"            -> conta (vínculo/cadastro do entregador)
     * - "Cancelamento ou recusa de pedido" -> pedido
     * - "Outros assuntos" e qualquer texto desconhecido (ou vazio) -> outro
     */
    private fun supportCategoryForDatabase(displayCategory: String): String =
        when (displayCategory.trim()) {
            "Dúvida sobre taxa de entrega" -> "financeiro"
            "Problema de endereço ou cliente" -> "entrega"
            "Vínculo com nova loja" -> "conta"
            "Cancelamento ou recusa de pedido" -> "pedido"
            else -> "outro"
        }
}

private val TICKET_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/**
 * O chamado exibia o timestamp ISO cru do banco ("2026-10-29T13:45:00Z").
 * Formata p/ data/hora local do aparelho (auditoria B4); se o valor não
 * parsear, devolve o original em vez de esconder a data.
 */
private fun formatTicketDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val instant = runCatching { Instant.parse(raw) }.getOrElse {
        runCatching { OffsetDateTime.parse(raw.replace(' ', 'T')).toInstant() }.getOrNull()
    } ?: return raw
    return instant.atZone(ZoneId.systemDefault()).format(TICKET_DATE_FORMATTER)
}
