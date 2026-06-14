package com.hitbosss.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hitbosss.presentation.designsystem.theme.Error100
import com.hitbosss.presentation.designsystem.theme.Error500
import com.hitbosss.presentation.designsystem.theme.Error600
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray200
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray400
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray700
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary100
import com.hitbosss.presentation.designsystem.theme.Primary500
import com.hitbosss.presentation.designsystem.theme.Primary600

/** Equivalente a RectangleButton de iOS (mismos colores por tipo y estado). */
enum class HitButtonType { Primary, Secondary, Tertiary, Destructive }

/** Tamaños de RectangleButton de iOS: medium (padding 12/16, body 14) y large (16/32, body 16). */
enum class HitButtonSize { Medium, Large }

private data class BtnColors(val text: Color, val bg: Color, val border: Color)

private fun colorsFor(type: HitButtonType, enabled: Boolean): BtnColors {
    if (!enabled) return BtnColors(Gray400, Gray200, Gray300)
    return when (type) {
        HitButtonType.Primary -> BtnColors(Primary100, Primary500, Primary600)
        HitButtonType.Secondary -> BtnColors(Gray100, Gray700, Gray500)
        HitButtonType.Tertiary -> BtnColors(Gray600, Color.Transparent, Gray600)
        HitButtonType.Destructive -> BtnColors(Error100, Error500, Error600)
    }
}

@Composable
fun HitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: HitButtonType = HitButtonType.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: HitButtonSize = HitButtonSize.Large,
) {
    val c = colorsFor(type, enabled)
    val vPad = if (size == HitButtonSize.Medium) 12.dp else 16.dp
    val hPad = if (size == HitButtonSize.Medium) 16.dp else 32.dp
    val textStyle = if (size == HitButtonSize.Medium) HitbosssType.bodyDefaultRegular else HitbosssType.bodyLargeRegular
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(c.bg)
            .border(1.dp, c.border, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled && !loading) { onClick() }
            .padding(vertical = vPad, horizontal = hPad),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = c.text, modifier = Modifier.size(20.dp))
        } else {
            Text(text, style = textStyle, color = c.text)
        }
    }
}
