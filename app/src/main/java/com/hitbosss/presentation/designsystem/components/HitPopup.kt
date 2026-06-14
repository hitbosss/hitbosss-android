package com.hitbosss.presentation.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary800
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

/**
 * Modal de notificación/error/confirmación — 1:1 con el componente Popup de iOS:
 * scrim secondary800 @ 0.8, card gris100 radio 12 (padding horizontal 60), icono opcional 70x70,
 * título titleSubsection, mensaje bodySmallRegular, y botones medium en fila
 * (cancelar = tertiary, confirmar = confirmType). Mensaje admite negrita vía AnnotatedString.
 */
@Composable
fun HitPopup(
    title: String,
    message: AnnotatedString,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    confirmType: HitButtonType = HitButtonType.Secondary,
    cancelText: String? = null,
    onCancel: (() -> Unit)? = null,
    allowDismissOnBackground: Boolean = true,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Secondary800.copy(alpha = 0.8f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = allowDismissOnBackground,
                ) { onDismissRequest() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Gray100)
                    // Consume el tap para que pulsar la card no cierre el popup.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                icon?.let {
                    Image(it, contentDescription = null, modifier = Modifier.padding(top = 31.dp).size(70.dp))
                }
                Text(
                    title,
                    style = HitbosssType.titleSubsection,
                    color = Gray800,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = if (icon == null) 24.dp else 16.dp).padding(horizontal = 16.dp),
                )
                Text(
                    message,
                    style = HitbosssType.bodySmallRegular,
                    color = Gray500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).padding(horizontal = 16.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 32.dp).padding(horizontal = 16.dp).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (cancelText != null) {
                        HitButton(
                            cancelText, { onCancel?.invoke() }, Modifier.weight(1f),
                            type = HitButtonType.Tertiary, size = HitButtonSize.Medium,
                        )
                    }
                    HitButton(
                        confirmText, onConfirm, Modifier.weight(1f),
                        type = confirmType, size = HitButtonSize.Medium,
                    )
                }
            }
        }
    }
}

/** Conveniencia: mensaje en texto plano (sin negrita). */
@Composable
fun HitPopup(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    confirmType: HitButtonType = HitButtonType.Secondary,
    cancelText: String? = null,
    onCancel: (() -> Unit)? = null,
    allowDismissOnBackground: Boolean = true,
) = HitPopup(
    title = title,
    message = AnnotatedString(message),
    confirmText = confirmText,
    onConfirm = onConfirm,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    icon = icon,
    confirmType = confirmType,
    cancelText = cancelText,
    onCancel = onCancel,
    allowDismissOnBackground = allowDismissOnBackground,
)
