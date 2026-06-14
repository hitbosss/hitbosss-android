package com.hitbosss.presentation.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.HitbosssType

/**
 * Estado de error de conexión/timeout (1:1 con iOS ErrorConectionView): ilustración + mensaje +
 * botón de reintento. Sustituye al texto plano de error.
 */
@Composable
fun ErrorConnectionView(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.error_conn_ranking),
    buttonText: String = stringResource(R.string.error_conn_refresh),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Image(
            painterResource(R.drawable.im_error_connection),
            contentDescription = null,
            modifier = Modifier.size(170.dp),
        )
        Text(
            message,
            style = HitbosssType.titleBody,
            color = Gray500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 70.dp),
        )
        HitButton(
            text = buttonText,
            onClick = onRetry,
            type = HitButtonType.Secondary,
            size = HitButtonSize.Medium,
            modifier = Modifier.padding(horizontal = 100.dp),
        )
    }
}
