package com.hitbosss.presentation.feature.hit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary800

/** Botones flotantes (compartir + denunciar) abajo-derecha del visor, igual que iOS. */
@Composable
fun HitActionButtons(onShare: () -> Unit, onReport: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircleAction(Icons.AutoMirrored.Filled.Send, "Compartir", onShare)
        CircleAction(Icons.Filled.Flag, "Denunciar", onReport)
    }
}

@Composable
private fun CircleAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(Gray100).border(1.dp, Gray300, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = Gray600, modifier = Modifier.size(20.dp))
    }
}

/** Diálogo de denuncia (igual que el Popup de iOS: triángulo + texto + campo 0/1000 + Cancelar/Aceptar). */
@Composable
fun ReportHitDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Gray100).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Gray800, modifier = Modifier.size(48.dp))
            Text("¿Quieres denunciar este hit?", style = HitbosssType.titleSubsection, color = Gray800, textAlign = TextAlign.Center)
            Text(
                "Si este hit infringe las normas de la comunidad, puedes enviarnos una denuncia. " +
                    "Revisaremos el contenido y tomaremos las medidas necesarias. Las denuncias son confidenciales.",
                style = HitbosssType.bodyDefaultRegular, color = Gray500, textAlign = TextAlign.Center,
            )
            // Campo de texto con contador 0/1000
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(10.dp))
                        .background(Gray300).padding(12.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.length <= 1000) text = it },
                        textStyle = HitbosssType.bodyDefaultRegular.copy(color = Gray800),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    "${text.length}/1000", style = HitbosssType.bodySmallRegular, color = Gray500,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).border(1.dp, Gray300, RoundedCornerShape(10.dp))
                        .clickable { onDismiss() }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Cancelar", style = HitbosssType.bodyLargeRegular, color = Gray800) }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Secondary800)
                        .clickable { onConfirm(text) }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Aceptar", style = HitbosssType.bodyLargeEmphasis, color = Gray100) }
            }
        }
    }
}

/** Overlay mientras se exporta/descarga el vídeo para compartir. */
@Composable
fun ExportingOverlay() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Gray100)
            Spacer(Modifier.height(0.dp))
            Text("Exportando vídeo…", style = HitbosssType.bodyLargeEmphasis, color = Gray100)
        }
    }
}
