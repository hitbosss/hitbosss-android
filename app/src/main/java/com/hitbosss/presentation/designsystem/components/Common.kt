package com.hitbosss.presentation.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType

/** Equivalente a LabelledDivider de iOS ("─── or ───"). */
@Composable
fun LabelledDivider(label: String, modifier: Modifier = Modifier, color: Color = Gray800) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f).padding(horizontal = 20.dp), color = color)
        Text(label, style = HitbosssType.bodyDefaultRegular, color = color)
        HorizontalDivider(Modifier.weight(1f).padding(horizontal = 20.dp), color = color)
    }
}

/** Equivalente a CustomNavigationHeader de iOS (flecha atrás + título). */
@Composable
fun HitTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Gray800)
        }
        Spacer(Modifier.width(16.dp))
        Text(title, style = HitbosssType.titleSubsection, color = Gray800)
    }
}
