package com.example.data.remote

import com.example.data.model.DriverProfile
import com.example.data.repository.DriverProfileRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseDriverProfileRepository : DriverProfileRepository {
    private val supabase = ItaSuperSupabase.client

    override fun getProfile(): Flow<DriverProfile> = flow {
        emit(runCatching { fetchProfile() }.getOrElse { fallbackProfile() })
    }

    override suspend fun updateProfile(profile: DriverProfile) {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("Sessão expirada. Entre novamente para continuar.")
        supabase
            .from("profiles")
            .update({
                set("full_name", profile.name)
                set("phone", profile.phone)
            }) {
                filter { eq("user_id", userId) }
            }
    }

    private fun fallbackProfile(): DriverProfile {
        val user = supabase.auth.currentUserOrNull()
        return DriverProfile(
            id = user?.id.orEmpty(),
            name = user?.email?.substringBefore("@") ?: "Entregador",
            email = user?.email.orEmpty(),
            phone = "",
            vehicleType = "",
            vehiclePlate = "",
            rating = 0.0,
            completedDeliveriesCount = 0,
            memberSince = ""
        )
    }

    private suspend fun fetchProfile(): DriverProfile {
        val user = supabase.auth.currentUserOrNull()
            ?: error("Sessão expirada. Entre novamente para continuar.")
        val row = supabase
            .from("profiles")
            .select(columns = Columns.list("user_id", "full_name", "phone", "role", "vehicle", "created_at")) {
                filter { eq("user_id", user.id) }
            }
            .decodeSingle<ProfileRow>()

        return DriverProfile(
            id = user.id,
            name = row.fullName?.ifBlank { null } ?: user.email?.substringBefore("@") ?: "Entregador",
            email = user.email.orEmpty(),
            phone = row.phone.orEmpty(),
            vehicleType = row.vehicle.orEmpty(),
            vehiclePlate = "",
            rating = 0.0,
            completedDeliveriesCount = 0,
            memberSince = row.createdAt?.take(10).orEmpty()
        )
    }
}
