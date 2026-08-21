package com.example.ui.common

/**
 * Converte exceções técnicas em orientações curtas e seguras para a interface.
 * Mensagens brutas podem conter URLs, códigos de banco, headers ou detalhes do aparelho.
 */
object DriverUserMessage {
    fun generic(error: Throwable?, fallback: String): String {
        val detail = error?.message.orEmpty().lowercase()
        return when {
            detail.contains("sessão expirada") || detail.contains("jwt") || detail.contains("not authenticated") ->
                "Sua sessão expirou. Entre novamente para continuar."
            detail.contains("network") || detail.contains("timeout") || detail.contains("connection") ||
                detail.contains("socket") || detail.contains("internet") || detail.contains("host") ->
                "Não foi possível concluir agora. Verifique sua internet e tente novamente."
            detail.contains("permission denied") || detail.contains("not allowed") || detail.contains("403") ->
                "Esta ação não está disponível para sua conta no momento. Atualize a tela e tente novamente."
            detail.contains("already accepted") || detail.contains("já aceito") || detail.contains("not pending") ||
                detail.contains("não está pendente") || detail.contains("0 rows") ->
                "Esta solicitação já foi atualizada. Atualize a tela para ver a situação atual."
            else -> fallback
        }
    }

    fun orderAcceptance(error: Throwable?): String = generic(
        error,
        "Não foi possível aceitar esta entrega agora. Atualize os pedidos e tente novamente."
    )

    fun orderBatchAcceptance(error: Throwable?): String = generic(
        error,
        "Não foi possível aceitar estes pedidos agora. Atualize a lista e tente novamente."
    )

    fun routeStart(error: Throwable?): String = generic(
        error,
        "Não foi possível iniciar a rota agora. Confira os pedidos e tente novamente."
    )

    fun orderRefresh(error: Throwable?): String = generic(
        error,
        "Não foi possível atualizar os pedidos agora. Verifique sua internet e tente novamente."
    )

    fun availability(error: Throwable?): String = generic(
        error,
        "Não foi possível atualizar sua disponibilidade agora. Tente novamente em instantes."
    )

    fun directoryPreference(error: Throwable?): String = generic(
        error,
        "Não foi possível salvar sua preferência agora. Tente novamente."
    )

    fun support(error: Throwable?): String = generic(
        error,
        "Não foi possível enviar o chamado agora. Tente novamente em instantes."
    )

    fun externalApp(error: Throwable?): String = generic(
        error,
        "Não foi possível abrir o aplicativo solicitado neste aparelho."
    )
}
