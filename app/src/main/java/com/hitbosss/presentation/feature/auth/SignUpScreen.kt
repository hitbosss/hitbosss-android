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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import com.hitbosss.data.auth.GoogleAuthClient
import com.hitbosss.presentation.designsystem.components.LabelledDivider
import com.hitbosss.presentation.designsystem.theme.Error700
import com.hitbosss.presentation.designsystem.theme.Gray600
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onGoToMain: () -> Unit,
    onGoToCompleteProfile: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
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
        HitTopBar(title = stringResource(R.string.auth_create_account), onBack = onBack)

        Column(
            modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            HitTextField(state.email, viewModel::onEmailChange, stringResource(R.string.auth_email), keyboardType = KeyboardType.Email)
            HitTextField(state.password, viewModel::onPasswordChange, stringResource(R.string.auth_password), isSecure = true)
            HitTextField(state.confirmPassword, viewModel::onConfirmPasswordChange, stringResource(R.string.auth_confirm_password), isSecure = true)
        }

        // Error inline (1:1 con iOS: línea roja a la izquierda bajo los campos).
        state.error?.let { error ->
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                Text(error, style = HitbosssType.bodySmallRegular, color = Error700)
            }
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HitButton(
                text = stringResource(R.string.auth_create_account),
                onClick = viewModel::signUp,
                type = HitButtonType.Primary,
                enabled = state.isFormValid,
                loading = state.isLoading,
            )

            LabelledDivider(stringResource(R.string.common_or))

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
                Text(stringResource(R.string.auth_continue_google), style = HitbosssType.bodyDefaultRegular, color = Gray600)
            }
        }

        LegalFooter(
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp)
                .fillMaxWidth(),
        )
    }
}
