package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ItaSuperApplication
import com.example.data.model.NavigationPreference
import com.example.data.repository.DriverLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.driverPreferences by preferencesDataStore(name = "itasuper_driver_preferences")

class DriverPreferencesRepository : DriverLocationRepository {
    private val store = ItaSuperApplication.appContext.driverPreferences
    private val optimizedRouteKey = booleanPreferencesKey("optimized_route")
    private val navigationPreferenceKey = stringPreferencesKey("navigation_preference")

    override fun getOptimizedRouteEnabled(): Flow<Boolean> = store.data.map { preferences ->
        preferences[optimizedRouteKey] ?: true
    }

    override suspend fun setOptimizedRouteEnabled(enabled: Boolean) {
        store.edit { preferences -> preferences[optimizedRouteKey] = enabled }
    }

    override fun getNavigationPreference(): Flow<NavigationPreference> = store.data.map { preferences ->
        when (preferences[navigationPreferenceKey]) {
            NavigationPreference.WAZE.name -> NavigationPreference.WAZE
            else -> NavigationPreference.GOOGLE_MAPS
        }
    }

    override suspend fun setNavigationPreference(pref: NavigationPreference) {
        store.edit { preferences -> preferences[navigationPreferenceKey] = pref.name }
    }
}
