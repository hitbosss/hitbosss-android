package com.hitbosss.presentation.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun RecoverPasswordScreen(
    onBack: () -> Unit,
    viewModel: RecoverPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = "Recuperar contraseña", onBack = onBack)

        Column(
            modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                "Introduce tu correo y te enviaremos un enlace para restablecer la contraseña.",
                style = HitbosssType.bodyDefaultRegular,
                color = Gray800,
            )
            HitTextField(state.email, viewModel::onEmailChange, "Correo electrónico", keyboardType = KeyboardType.Email)

            if (state.sent) {
                Text(
                    "✓ Enlace enviado. Revisa tu correo.",
                    style = HitbosssType.bodyDefaultEmphasis,
                    color = Success600,
                )
            }
        }

        HitButton(
            text = "Enviar enlace",
            onClick = viewModel::send,
            type = HitButtonType.Primary,
            enabled = state.isFormValid,
            loading = state.isLoading,
            modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
        )
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("Aceptar") } },
            title = { Text("Algo ha salido mal", style = HitbosssType.titleBody) },
            text = { Text(error, style = HitbosssType.bodyDefaultRegular) },
        )
    }
}
