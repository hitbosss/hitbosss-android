package com.hitbosss

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    // Android 13+: sin POST_NOTIFICATIONS concedido, la notificación del foreground service de subida NO se
    // muestra en ningún sitio (ni en la pantalla de bloqueo). Se pide al arrancar.
    private val notificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* si deniega, no bloquea nada */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Deeplink con el que se abrió la app (hitbosss://group|event/{id}).
        DeepLinkBus.post(intent?.data)
        requestNotificationsPermission()
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

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Deeplink recibido con la app ya abierta. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinkBus.post(intent.data)
    }
}
