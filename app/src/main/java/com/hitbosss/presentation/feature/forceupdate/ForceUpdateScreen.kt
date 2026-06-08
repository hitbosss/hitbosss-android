package com.hitbosss.presentation.feature.forceupdate

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500

/** Pantalla bloqueante de actualización obligatoria (equivale a ForceUpdateView de iOS). */
@Composable
fun ForceUpdateScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray100)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Actualiza HitBoss", style = HitbosssType.titleSection, color = Gray800, textAlign = TextAlign.Center)
        Text(
            "Necesitas la última versión para seguir compitiendo.",
            style = HitbosssType.bodyLargeRegular,
            color = Gray500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
                runCatching { context.startActivity(intent) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Primary500),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Actualizar", style = HitbosssType.bodyLargeEmphasis, color = Gray100)
        }
        Spacer8()
    }
}

@Composable
private fun Spacer8() = androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
