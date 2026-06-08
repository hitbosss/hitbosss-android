package com.hitbosss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
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
        enableEdgeToEdge()
        setContent {
            HitbosssTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Gray100) {
                    // statusBarsPadding: el contenido no se mete bajo la barra de estado
                    // (edge-to-edge es obligatorio en SDK 35). La barra de navegación inferior
                    // la gestiona la propia NavigationBar.
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        AppNavHost()
                    }
                }
            }
        }
    }
}
