package com.hitbosss

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hitbosss.core.deeplink.DeepLinkBus
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.HitbosssTheme
import com.hitbosss.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point. Arranca el grafo de navegación: Launch -> (ForceUpdate | Welcome | CompleteProfile | Main).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Deeplink con el que se abrió la app (hitbosss://group|event/{id}).
        DeepLinkBus.post(intent?.data)
        enableEdgeToEdge()
        setContent {
            HitbosssTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Gray100) {
                    // Barra de estado visible; el contenido queda debajo gracias al padding superior
                    // de AppNavHost. El color de sus iconos (claro/oscuro) lo ajusta AppNavHost según
                    // la pantalla (oscuros sobre el fondo blanco, claros en Launch/Welcome).
                    Box(Modifier.fillMaxSize()) {
                        AppNavHost()
                    }
                }
            }
        }
    }

    /** Deeplink recibido con la app ya abierta. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinkBus.post(intent.data)
    }
}
