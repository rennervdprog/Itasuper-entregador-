package com.example.platform

import android.content.Context
import com.example.ItaSuperApplication

/**
 * Guarda somente a opção local de oferecer biometria para a conta já autenticada.
 * A senha e os tokens não são gravados por este componente.
 */
object DriverBiometricAccess {
    private const val PREFERENCES_NAME = "itasuper_driver_biometric_access"
    private const val ENABLED_USER_ID_KEY = "enabled_user_id"

    private val preferences by lazy {
        ItaSuperApplication.appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun enableFor(userId: String) {
        preferences.edit().putString(ENABLED_USER_ID_KEY, userId).apply()
    }

    fun isEnabled(): Boolean = preferences.getString(ENABLED_USER_ID_KEY, null) != null

    fun isEnabledFor(userId: String): Boolean =
        preferences.getString(ENABLED_USER_ID_KEY, null) == userId

    fun disableFor(userId: String) {
        if (isEnabledFor(userId)) clear()
    }

    fun clear() {
        preferences.edit().remove(ENABLED_USER_ID_KEY).apply()
    }
}
