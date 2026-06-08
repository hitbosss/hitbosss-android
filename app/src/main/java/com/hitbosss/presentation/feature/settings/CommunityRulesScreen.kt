package com.hitbosss.presentation.feature.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Orange300
import com.hitbosss.presentation.designsystem.theme.Warning500
import com.hitbosss.presentation.designsystem.theme.Yellow300

private data class RuleSection(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val message: String,
    val rules: List<Pair<String, String>>,
)

private val rulesSections = listOf(
    RuleSection(
        title = "Las 7 reglas de HitBosss",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        iconColor = Warning500,
        message = "Compite con honestidad, respeta a la comunidad y alcanza tu mejor versión.",
        rules = listOf(
            "1. No mentir con los pesos" to "📌 La transparencia es clave. Sé honesto con los kilos que levantas.",
            "2. Técnica correcta siempre" to "✔️ Sigue los estándares de los tutoriales para validar tu levantamiento.",
            "3. Sube los hits en el ejercicio correspondiente" to "🎯 Registra cada levantamiento en su categoría correcta.",
            "4. No edites ni manipules los vídeos" to "🎥 Los vídeos deben grabarse y subirse desde la app, sin modificaciones.",
            "5. No hagas trampas" to "🚫 Cualquier intento de engaño será penalizado.",
            "6. Evita contenido inapropiado o engañoso" to "⚠️ Nada ofensivo, manipulado o ajeno a la app. Usa el sistema de reportes con responsabilidad.",
            "7. Participa con responsabilidad" to "💪 Asegúrate de estar en condiciones óptimas para levantar.",
        ),
    ),
    RuleSection(
        title = "Los 7 consejos de HitBosss",
        icon = Icons.Filled.WarningAmber,
        iconColor = Orange300,
        message = "Queremos una comunidad que sea justa y respetuosa.",
        rules = listOf(
            "1. Reportes falsos tienen consecuencias" to "🚫 Reportar sin motivo puede llevar a sanciones o restricciones.",
            "2. Mentir con los pesos o la técnica tiene consecuencias" to "🛑 Puedes ser eliminado del ranking o baneado.",
            "3. Manipular vídeos o datos = expulsión" to "❌ Tolerancia cero ante alteraciones.",
            "4. Burlas o faltas de respeto no serán toleradas" to "🙅‍♂️ Esto es una comunidad de apoyo, no de juicio.",
            "5. Nada de subir contenido ajeno o robado" to "📛 Cada hit debe ser tuyo. Si no lo es, serás sancionado.",
            "6. No manipules el sistema de clasificación" to "🎭 Si registras hits en categorías erróneas, perderás tu puesto.",
            "7. El mal uso reiterado de la app llevará a un baneo permanente" to "🚷 Seguir las reglas es esencial para seguir compitiendo.",
        ),
    ),
    RuleSection(
        title = "Los 7 consejos de HitBosss",
        icon = Icons.Filled.Lightbulb,
        iconColor = Yellow300,
        message = "Mejorar no es solo levantar más peso.",
        rules = listOf(
            "1. Acude a un entrenador profesional" to "🎓 Te ayudará a evitar lesiones y mejorar tu progreso.",
            "2. Domina la técnica antes de subir peso" to "✅ Un levantamiento limpio vale más que uno mal hecho.",
            "3. Calienta antes de tu intento máximo" to "🔥 Haz series progresivas para preparar cuerpo y mente.",
            "4. Escucha a tu cuerpo" to "🧠 Si hay dolor o molestias, descansa.",
            "5. La constancia es clave" to "📈 Sé paciente, sigue un plan y descansa bien.",
            "6. Usa equipamiento adecuado" to "🛡️ Rodilleras, cinturón, muñequeras... Seguridad ante todo.",
            "7. Disfruta el proceso y la comunidad" to "🤝 Aprende, comparte y crece con otros atletas.",
        ),
    ),
)

@Composable
fun CommunityRulesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = "HitBosss rules", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            rulesSections.forEach { RulesSection(it) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RulesSection(section: RuleSection) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(section.icon, contentDescription = null, tint = section.iconColor, modifier = Modifier.size(22.dp))
            Text(section.title, style = HitbosssType.bodyDefaultEmphasis, color = Gray800, modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Gray600,
                modifier = Modifier.size(22.dp).rotate(if (expanded) 0f else 180f),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Gray400))

        if (expanded) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Caja de mensaje
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Gray200)
                        .border(1.dp, Gray300, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Gray500, modifier = Modifier.size(20.dp))
                    Text(section.message, style = HitbosssType.bodyDefaultEmphasis, color = Gray500)
                }
                // Reglas (título en negrita + cuerpo)
                section.rules.forEach { (title, body) ->
                    Column {
                        Text(title, style = HitbosssType.bodyDefaultEmphasis, color = Gray500)
                        Text(body, style = HitbosssType.bodyDefaultRegular, color = Gray500)
                    }
                }
            }
        }
    }
}
