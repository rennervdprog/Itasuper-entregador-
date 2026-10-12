// TODO(DEBUG): REMOVER ANTES DE PUBLICAR - activity temporária para diagnosticar o GPS tracking
package com.example.ui.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.theme.ItaSuperTheme

class GpsDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ItaSuperTheme {
                GpsDebugScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, GpsDebugActivity::class.java))
        }
    }
}
