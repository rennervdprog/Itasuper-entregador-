package com.example.data.remote

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Guarda a sessão do Supabase em disco (SharedPreferences do próprio app).
 *
 * Sem um SessionManager configurado, a biblioteca (supabase-kt) usa o
 * MemorySessionManager: a sessão vive só na RAM e some quando o processo
 * morre — o app voltava para a tela de login a cada fechamento. Este
 * gerenciador grava a sessão serializada no aparelho, no mesmo formato
 * usado pelo SettingsSessionManager oficial da biblioteca, e o login passa
 * a sobreviver ao fechamento do app (o token é renovado automaticamente).
 *
 * A gravação usa commit() (síncrono) de propósito: apply() é assíncrono e
 * a sessão recém-criada no login poderia se perder se o processo morresse
 * logo em seguida.
 */
class DurableSessionManager(context: Context) : SessionManager {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val json = Json { encodeDefaults = true }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit().putString(KEY_SESSION, json.encodeToString(session)).commit()
    }

    override suspend fun loadSession(): UserSession {
        val raw = prefs.getString(KEY_SESSION, null) ?: throw NoSessionFoundException()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            // Sessão corrompida/incompatível: trata como "sem sessão" em vez
            // de travar a abertura do app.
            throw NoSessionFoundException()
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY_SESSION).commit()
    }

    private companion object {
        const val PREFS_NAME = "itasuper_supabase_session"
        const val KEY_SESSION = "session"
    }
}
