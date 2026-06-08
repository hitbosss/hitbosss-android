package com.hitbosss.presentation.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.R
import com.hitbosss.data.auth.GoogleAuthClient
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.components.HitTextField
import com.hitbosss.presentation.designsystem.components.HitTopBar
import com.hitbosss.presentation.designsystem.components.LabelledDivider
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray700
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onRecoverPassword: () -> Unit,
    onGoToMain: () -> Unit,
    onGoToCompleteProfile: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleClient = remember { GoogleAuthClient(context) }

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            when (it) {
                AuthNavEvent.ToMain -> onGoToMain()
                AuthNavEvent.ToCompleteProfile -> onGoToCompleteProfile()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Gray100)) {
        HitTopBar(title = "Iniciar sesión", onBack = onBack)

        Column(
            modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            HitTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "Correo electrónico",
                keyboardType = KeyboardType.Email,
            )
            HitTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Contraseña",
                isSecure = true,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "¿Has olvidado tu contraseña?",
                    style = HitbosssType.bodySmallLink,
                    color = Gray700,
                    modifier = Modifier.clickable { onRecoverPassword() },
                )
            }
        }

        HitButton(
            text = "Iniciar sesión",
            onClick = viewModel::signIn,
            type = HitButtonType.Primary,
            enabled = state.isFormValid,
            loading = state.isLoading,
            modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
        )

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LabelledDivider("o")

            // Continuar con Google
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gray100)
                    .border(1.dp, Gray500, RoundedCornerShape(8.dp))
                    .clickable(enabled = !state.isLoading) {
                        viewModel.setLoading()
                        scope.launch {
                            val webClientId = context.getString(R.string.default_web_client_id)
                            googleClient.getGoogleIdToken(webClientId)
                                .onSuccess { viewModel.onGoogleIdToken(it) }
                                .onFailure { viewModel.onGoogleError(it.message) }
                        }
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(R.drawable.im_icon_google),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Continuar con Google", style = HitbosssType.bodyDefaultRegular, color = Gray600)
            }
        }

        Text(
            "Al iniciar sesión, aceptas la Política de privacidad y los Términos y condiciones.",
            style = HitbosssType.bodySmallRegular,
            color = Gray800,
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
