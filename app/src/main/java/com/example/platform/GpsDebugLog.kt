package com.example.platform

// TODO(DEBUG): REMOVER ANTES DE PUBLICAR - singleton de logs para debug do GPS tracking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GpsDebugLog {
    private const val MAX_LOGS = 300
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    fun add(msg: String) {
        val ts = try {
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        } catch (t: Throwable) {
            "--:--:--"
        }
        val entry = "[$ts] $msg"
        val updated = (_logs.value + entry).takeLast(MAX_LOGS)
        _logs.value = updated
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
