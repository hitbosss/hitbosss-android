package com.hitbosss.presentation.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitTextField
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Success600
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitPopup

/**
 * Recuperar contraseña — 1:1 con RecoverPasswordView de iOS: un solo paso (email) que envía un
 * enlace de Firebase y muestra el popup "Correo enviado", volviendo atrás al aceptar.
 */
@Composable
fun RecoverPasswordScreen(
    onBack: () -> Unit,
    viewModel: RecoverPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = stringResource(R.string.recover_title), onBack = onBack)

        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.recover_desc),
                style = HitbosssType.bodyDefaultRegular,
                color = Gray500,
                modifier = Modifier.padding(top = 16.dp),
            )
            HitTextField(
                state.email,
                viewModel::onEmailChange,
                stringResource(R.string.auth_email),
                keyboardType = KeyboardType.Email,
                modifier = Modifier.padding(top = 32.dp),
            )
            HitButton(
                text = stringResource(R.string.recover_button),
                onClick = viewModel::send,
                type = HitButtonType.Secondary,
                enabled = state.isFormValid,
                loading = state.isLoading,
                modifier = Modifier.padding(top = 56.dp).fillMaxWidth(),
            )
        }
    }

    // Éxito: popup "Correo enviado" y, al aceptar, volver atrás (igual que iOS).
    if (state.sent) {
        HitPopup(
            title = stringResource(R.string.recover_sent_title),
            message = stringResource(R.string.recover_sent_message),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = onBack,
            onDismissRequest = onBack,
        )
    }

    state.error?.let { error ->
        HitPopup(
            title = stringResource(R.string.common_something_wrong),
            message = error,
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::clearError,
            onDismissRequest = viewModel::clearError,
        )
    }
}
