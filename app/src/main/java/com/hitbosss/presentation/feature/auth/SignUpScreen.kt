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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitTextField
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.HitbosssType

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onGoToMain: () -> Unit,
    onGoToCompleteProfile: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            when (it) {
                AuthNavEvent.ToMain -> onGoToMain()
                AuthNavEvent.ToCompleteProfile -> onGoToCompleteProfile()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = "Crear una cuenta", onBack = onBack)

        Column(
            modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            HitTextField(state.email, viewModel::onEmailChange, "Correo electrónico", keyboardType = KeyboardType.Email)
            HitTextField(state.password, viewModel::onPasswordChange, "Contraseña (mín. 6)", isSecure = true)
            HitTextField(state.confirmPassword, viewModel::onConfirmPasswordChange, "Repite la contraseña", isSecure = true)
        }

        HitButton(
            text = "Crear cuenta",
            onClick = viewModel::signUp,
            type = HitButtonType.Primary,
            enabled = state.isFormValid,
            loading = state.isLoading,
            modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
        )

        Text(
            "Al crear una cuenta, aceptas la Política de privacidad y los Términos y condiciones.",
            style = HitbosssType.bodySmallRegular,
            color = Gray500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
