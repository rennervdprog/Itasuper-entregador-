package com.example.data.remote

import com.example.data.model.DriverDirectoryPreference
import com.example.data.repository.DriverDirectoryRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Acesso exclusivo à preferência do motoboy autenticado na base voluntária por cidade.
 * Não consulta nem expõe dados de outros motoboys.
 */
class SupabaseDriverDirectoryRepository : DriverDirectoryRepository {
    private val supabase = ItaSuperSupabase.client

    override fun observeMyPreference(): Flow<DriverDirectoryPreference> = flow {
        emit(runCatching { fetchMyPreference() }.getOrDefault(DriverDirectoryPreference()))
    }

    override suspend fun savePreference(
        city: String,
        isListed: Boolean
    ): Result<DriverDirectoryPreference> = runCatching {
        val normalizedCity = city.trim().replace(Regex("\\s+"), " ")
        require(normalizedCity.length >= 2) { "Informe uma cidade de atuação com pelo menos 2 caracteres." }

        // A RPC concentra a autorização do papel motoboy, normalização da cidade
        // e registro/remoção do consentimento de contato.
        supabase.postgrest.rpc(
            "upsert_driver_directory_preference",
            buildJsonObject {
                put("_city", normalizedCity)
                put("_is_listed", isListed)
            }
        )

        fetchMyPreference()
    }

    private suspend fun fetchMyPreference(): DriverDirectoryPreference {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("Sessão expirada. Entre novamente para continuar.")

        val row = supabase
            .from("driver_directory_preferences")
            .select(columns = Columns.list("user_id", "city", "is_listed", "contact_consent_at")) {
                filter { eq("user_id", userId) }
            }
            .decodeList<DriverDirectoryPreferenceRow>()
            .firstOrNull()
            ?: return DriverDirectoryPreference()

        return DriverDirectoryPreference(
            city = row.city.orEmpty(),
            isListed = row.isListed,
            hasContactConsent = row.contactConsentAt != null
        )
    }
}
