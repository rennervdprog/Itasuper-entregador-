package com.example.data.fake

import com.example.data.local.DriverPreferencesRepository
import com.example.data.remote.SupabaseAuthRepository
import com.example.data.remote.SupabaseDriverAvailabilityRepository
import com.example.data.remote.SupabaseDriverDirectoryRepository
import com.example.data.remote.SupabaseDriverHistoryRepository
import com.example.data.remote.SupabaseDriverLinkRepository
import com.example.data.remote.SupabaseDriverNotificationsRepository
import com.example.data.remote.SupabaseDriverOrdersRepository
import com.example.data.remote.SupabaseDriverProfileRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverAvailabilityRepository
import com.example.data.repository.DriverDirectoryRepository
import com.example.data.repository.DriverHistoryRepository
import com.example.data.repository.DriverLinkRepository
import com.example.data.repository.DriverLocationRepository
import com.example.data.repository.DriverNotificationsRepository
import com.example.data.repository.DriverOrdersRepository
import com.example.data.repository.DriverProfileRepository

/**
 * Contêiner de dependências do aplicativo Entregador.
 *
 * Todas as implementações conectadas abaixo usam dados reais autorizados da
 * sessão Supabase ou preferências locais do próprio dispositivo. Não há
 * repositórios de demonstração ativos neste aplicativo.
 */
object AppContainer {
    val availabilityRepository: DriverAvailabilityRepository by lazy { SupabaseDriverAvailabilityRepository() }
    val authRepository: AuthRepository by lazy { SupabaseAuthRepository(availabilityRepository) }
    val profileRepository: DriverProfileRepository by lazy { SupabaseDriverProfileRepository() }
    val directoryRepository: DriverDirectoryRepository by lazy { SupabaseDriverDirectoryRepository() }
    val linkRepository: DriverLinkRepository by lazy { SupabaseDriverLinkRepository() }
    val historyRepository: DriverHistoryRepository by lazy { SupabaseDriverHistoryRepository() }
    val ordersRepository: DriverOrdersRepository by lazy { SupabaseDriverOrdersRepository(availabilityRepository) }
    val locationRepository: DriverLocationRepository by lazy { DriverPreferencesRepository() }
    val notificationsRepository: DriverNotificationsRepository by lazy { SupabaseDriverNotificationsRepository() }
}
