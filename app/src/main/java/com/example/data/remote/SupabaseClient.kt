package com.example.data.remote

import com.example.BuildConfig
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Cliente único do backend ItaSuper usado exclusivamente pelo APK de entregador.
 * A chave configurada no build é publicável e todas as operações permanecem
 * sujeitas às políticas RLS do projeto Supabase.
 */
@OptIn(SupabaseExperimental::class)
object ItaSuperSupabase {
    private val configuredUrl: String = BuildConfig.SUPABASE_URL.trim()
    private val configuredPublishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    val isConfigured: Boolean
        get() = configuredUrl.isNotBlank() && configuredPublishableKey.isNotBlank()

    val configurationMessage: String
        get() = when {
            configuredUrl.isBlank() && configuredPublishableKey.isBlank() ->
                "As configurações de conexão do aplicativo não foram incluídas nesta instalação."
            configuredUrl.isBlank() ->
                "A configuração de endereço do serviço não foi incluída nesta instalação."
            else ->
                "A chave pública de conexão não foi incluída nesta instalação."
        }

    val client by lazy {
        check(isConfigured) {
            "Configuração do Supabase ausente no APK."
        }

        createSupabaseClient(
            supabaseUrl = configuredUrl,
            supabaseKey = configuredPublishableKey
        ) {
            install(Auth) {
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
                autoSaveToStorage = true
                enableLifecycleCallbacks = true
                checkSessionOnRequest = true
            }
            install(Postgrest)
            install(Realtime)
            install(Functions)
        }
    }
}
