package com.example.data.remote

import com.example.data.model.DriverProfile
import com.example.platform.DriverBiometricAccess
import com.example.platform.DriverPushRegistration
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverAvailabilityRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val availabilityRepository: DriverAvailabilityRepository
) : AuthRepository {
    private val supabase = ItaSuperSupabase.client
    private val currentUser = MutableStateFlow<DriverProfile?>(null)

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch { restoreSession() }
    }

    override fun getCurrentUser(): Flow<DriverProfile?> = currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<DriverProfile> = runCatching {
        require(email.isNotBlank() && password.isNotBlank()) {
            "E-mail e senha são obrigatórios."
        }

        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }

        loadAuthenticatedProfile()
    }.onSuccess { profile ->
        currentUser.value = profile
        DriverPushRegistration.reclaimForAuthenticatedDriver()
        availabilityRepository.restoreOnlinePresence()
    }

    override suspend fun registerMotoboy(
        fullName: String,
        document: String,
        vehicle: String,
        whatsapp: String,
        email: String,
        password: String
    ): Result<DriverProfile> = runCatching {
        val normalizedName = fullName.trim()
        val normalizedDocument = document.filter(Char::isDigit)
        val normalizedVehicle = vehicle.trim()
        val normalizedWhatsappDigits = whatsapp.filter(Char::isDigit)
        val normalizedWhatsapp = if (normalizedWhatsappDigits.startsWith("55")) {
            normalizedWhatsappDigits
        } else {
            "55$normalizedWhatsappDigits"
        }
        val normalizedEmail = email.trim()

        require(normalizedName.length in 3..100) { "Informe seu nome completo." }
        require(normalizedDocument.length in 11..14) { "Informe um CPF válido." }
        require(normalizedVehicle.length in 3..100) { "Informe o modelo do veículo." }
        require(normalizedWhatsappDigits.length in 10..13) { "Informe um WhatsApp válido com DDD." }
        require(normalizedEmail.contains("@")) { "Informe um e-mail válido." }
        require(password.length >= 6) { "A senha deve conter pelo menos 6 caracteres." }

        // Espelha o Capacitor/web: a conta nasce no Auth com perfil de cliente e é
        // convertida para motoboy exclusivamente pela RPC versionada no Supabase.
        supabase.auth.signUpWith(Email) {
            this.email = normalizedEmail
            this.password = password
            data = buildJsonObject {
                put("role", "cliente")
                put("full_name", normalizedName)
                put("whatsapp", normalizedWhatsapp)
            }
        }

        check(supabase.auth.currentUserOrNull() != null) {
            "Não foi possível iniciar a sessão após criar a conta. Tente entrar com os dados cadastrados."
        }

        supabase.postgrest.rpc("register_as_motoboy", buildJsonObject {
            put("_full_name", normalizedName)
            put("_document", normalizedDocument)
            put("_vehicle", normalizedVehicle)
            put("_whatsapp", normalizedWhatsapp)
        })

        loadAuthenticatedProfile()
    }.onSuccess { profile ->
        currentUser.value = profile
        DriverPushRegistration.reclaimForAuthenticatedDriver()
    }

    override suspend fun logout() {
        // O logout jamais cancela pedidos em andamento. Ele apenas interrompe a
        // disponibilidade futura antes de invalidar a sessão atual.
        runCatching { availabilityRepository.prepareForLogout() }
        supabase.auth.signOut()
        DriverBiometricAccess.clear()
        currentUser.value = null
    }

    override suspend fun restoreSession(): Result<DriverProfile?> = runCatching {
        // O Auth carrega a sessão persistida de forma assíncrona. A Splash deve
        // aguardar o estado final, e não interpretar o valor inicial nulo como logout.
        val status = supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        if (status !is SessionStatus.Authenticated) {
            currentUser.value = null
            return@runCatching null
        }
        loadAuthenticatedProfile()
    }.onSuccess { profile ->
        currentUser.value = profile
        if (profile != null) {
            DriverPushRegistration.reclaimForAuthenticatedDriver()
            availabilityRepository.restoreOnlinePresence()
        }
    }

    override fun isAuthenticated(): Flow<Boolean> = currentUser.map { it != null }

    suspend fun refreshCurrentUser() {
        restoreSession()
    }

    private suspend fun loadAuthenticatedProfile(): DriverProfile {
        val user = supabase.auth.currentUserOrNull()
            ?: error("Sessão expirada. Entre novamente para continuar.")

        val profile = supabase
            .from("profiles")
            .select(columns = Columns.list("user_id", "full_name", "phone", "role", "vehicle", "created_at")) {
                filter { eq("user_id", user.id) }
            }
            .decodeSingle<ProfileRow>()

        check(profile.role == "motoboy" || profile.role == "entregador") {
            "Esta conta não possui acesso de motoboy de loja."
        }

        return DriverProfile(
            id = user.id,
            name = profile.fullName?.ifBlank { null } ?: user.email?.substringBefore("@") ?: "Entregador",
            email = user.email ?: "",
            phone = profile.phone.orEmpty(),
            vehicleType = profile.vehicle.orEmpty(),
            vehiclePlate = "",
            rating = 0.0,
            completedDeliveriesCount = 0,
            memberSince = profile.createdAt?.take(10).orEmpty()
        )
    }
}
