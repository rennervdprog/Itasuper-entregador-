package com.example.platform

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Protege a abertura de uma sessão já persistida pelo Supabase. Nenhuma senha,
 * token ou chave é armazenada por este componente: a biometria apenas libera a
 * navegação após o Auth restaurar a sessão protegida no armazenamento do SDK.
 */
object DriverBiometricAuthenticator {
    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    enum class Availability {
        READY,
        NOT_ENROLLED,
        NOT_AVAILABLE
    }

    fun availability(activity: FragmentActivity): Availability = when (
        BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
    ) {
        BiometricManager.BIOMETRIC_SUCCESS -> Availability.READY
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NOT_ENROLLED
        else -> Availability.NOT_AVAILABLE
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (availability(activity) != Availability.READY) {
            onError("A impressão digital não está configurada neste aparelho.")
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Entrar no ItaSuper Entregador")
                .setSubtitle("Confirme sua identidade com a impressão digital")
                .setNegativeButtonText("Usar e-mail e senha")
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
        )
    }
}
