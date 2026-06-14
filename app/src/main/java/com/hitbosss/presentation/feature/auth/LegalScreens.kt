package com.hitbosss.presentation.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.hitbosss.R
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable

/** Hoja legal a mostrar desde el footer de Iniciar sesión / Crear cuenta. */
enum class LegalSheet { Privacy, Terms }

/**
 * Footer compartido por SignIn y SignUp (1:1 con iOS): "Al iniciar sesión aceptas la" +
 * enlaces a Política de privacidad / Términos y condiciones que abren su hoja inferior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalFooter(modifier: Modifier = Modifier) {
    var sheet by remember { mutableStateOf<LegalSheet?>(null) }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.legal_accept_prefix),
            style = HitbosssType.bodySmallRegular,
            color = Gray800,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.Center) {
            Text(
                stringResource(R.string.legal_privacy_policy),
                style = HitbosssType.bodySmallLink,
                color = Gray800,
                modifier = Modifier.clickable { sheet = LegalSheet.Privacy },
            )
            Text(stringResource(R.string.legal_and), style = HitbosssType.bodySmallRegular, color = Gray800)
            Text(
                stringResource(R.string.legal_terms),
                style = HitbosssType.bodySmallLink,
                color = Gray800,
                modifier = Modifier.clickable { sheet = LegalSheet.Terms },
            )
        }
    }
    sheet?.let { s ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Gray100,
        ) {
            when (s) {
                LegalSheet.Privacy -> PrivacyPolicyScreen()
                LegalSheet.Terms -> TermsConditionsScreen()
            }
        }
    }
}

/** Política de privacidad (1:1 con iOS PrivacyPolicyView). */
@Composable
fun PrivacyPolicyScreen() = LegalScreen(stringResource(R.string.legal_privacy_policy), LegalTexts.privacyPolicy)

/** Términos y condiciones (1:1 con iOS TermsConditionsView). */
@Composable
fun TermsConditionsScreen() = LegalScreen(stringResource(R.string.legal_terms), LegalTexts.termsConditions)

@Composable
private fun LegalScreen(title: String, body: String) {
    val annotated = remember(body) { body.parseMarkdownBold() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray100)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(title, style = HitbosssType.titleSection, color = Gray800)
        Spacer(Modifier.height(16.dp))
        Text(annotated, style = HitbosssType.bodySmallRegular, color = Gray800)
        Spacer(Modifier.height(24.dp))
    }
}

/** Convierte los **...** del texto de iOS en spans en negrita (igual que Text(.init) de SwiftUI). */
internal fun String.parseMarkdownBold(): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < length) {
        val open = indexOf("**", i)
        if (open < 0) { append(substring(i)); break }
        append(substring(i, open))
        val close = indexOf("**", open + 2)
        if (close < 0) { append(substring(open)); break }
        withStyleBold { append(substring(open + 2, close)) }
        i = close + 2
    }
}

private inline fun AnnotatedString.Builder.withStyleBold(block: AnnotatedString.Builder.() -> Unit) {
    val start = length
    block()
    addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, length)
}
