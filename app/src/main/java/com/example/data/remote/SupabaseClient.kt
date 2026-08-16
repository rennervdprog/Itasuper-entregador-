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
    val client by lazy {
        check(BuildConfig.SUPABASE_URL.isNotBlank()) {
            "SUPABASE_URL não configurada em local.properties"
        }
        check(BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
            "SUPABASE_PUBLISHABLE_KEY não configurada em local.properties"
        }

        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
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
