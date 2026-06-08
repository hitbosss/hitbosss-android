package com.hitbosss.presentation.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Mapeo de la paleta a un ColorScheme de Material3. La app iOS es light-only;
 * mantenemos lo mismo. Para tokens que Material no cubre, usar los `val` de Color.kt.
 */
private val HitbosssColorScheme = lightColorScheme(
    primary = Primary500,
    onPrimary = Gray100,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary800,
    secondary = Secondary500,
    onSecondary = Gray100,
    secondaryContainer = Secondary100,
    onSecondaryContainer = Secondary800,
    background = Gray100,
    onBackground = Gray800,
    surface = Gray100,
    onSurface = Gray800,
    surfaceVariant = Gray200,
    onSurfaceVariant = Gray500,
    outline = Gray400,
    error = Error500,
    onError = Gray100,
    errorContainer = Error100,
    onErrorContainer = Error700,
)

/** Tipografía Material mapeada a la escala Inter (para componentes Material por defecto). */
private val HitbosssTypography = Typography(
    titleLarge = HitbosssType.titleSection,
    titleMedium = HitbosssType.titleBody,
    titleSmall = HitbosssType.titleGroup,
    bodyLarge = HitbosssType.bodyLargeRegular,
    bodyMedium = HitbosssType.bodyDefaultRegular,
    bodySmall = HitbosssType.bodySmallRegular,
    labelLarge = HitbosssType.bodyDefaultEmphasis,
    labelMedium = HitbosssType.bodySmallEmphasis,
)

@Composable
fun HitbosssTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HitbosssColorScheme,
        typography = HitbosssTypography,
        content = content,
    )
}
