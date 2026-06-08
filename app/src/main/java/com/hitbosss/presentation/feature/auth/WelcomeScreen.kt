package com.hitbosss.presentation.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary500
import com.hitbosss.presentation.designsystem.theme.Secondary800

/** Item del carrusel de onboarding (equivale a OnboardingItem de iOS). */
private data class OnboardingItem(val title: String, val headline: String, val image: Int)

private val onboardingData = listOf(
    OnboardingItem(
        "Verifica tu hit en vídeo",
        "Cada levantamiento queda registrado en vídeo, validado y visible para toda la comunidad",
        R.drawable.im_onboarding_verify,
    ),
    OnboardingItem(
        "Demuestra tu fuerza al mundo",
        "Compite en tus ejercicios favoritos y mide tu fuerza frente a atletas de todo el mundo",
        R.drawable.im_onboarding_ranking,
    ),
    OnboardingItem(
        "Compite en grupos privados",
        "Crea o únete a un ranking privado y compite en los mismos ejercicios solo con tu grupo",
        R.drawable.im_onboarding_group,
    ),
    OnboardingItem(
        "Participa en eventos exclusivos",
        "Participa en eventos temporales, registra tu mejor marca y asciende en rankings especiales",
        R.drawable.im_onboarding_event,
    ),
)

/**
 * Pantalla de bienvenida (equivale a WelcomeView de iOS): banner superior secondary800 con el logo
 * blanco, carrusel de onboarding, indicador de página y botones Iniciar sesión / Crear una cuenta.
 */
@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { onboardingData.size })
    val density = LocalDensity.current
    // Igual que en iOS (imageFrame.maxY): la banda navy llega justo hasta el fondo de la imagen.
    var navyHeightPx by remember { mutableIntStateOf(0) }
    var rootTopPx by remember { mutableIntStateOf(0) }
    val navyHeight = if (navyHeightPx > 0) with(density) { navyHeightPx.toDp() } else 360.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(Gray100)
            .onGloballyPositioned { rootTopPx = it.positionInRoot().y.toInt() },
    ) {
        // Banda superior coloreada que queda detrás del logo y de la imagen del carrusel.
        Box(Modifier.fillMaxWidth().height(navyHeight).background(Secondary800).align(Alignment.TopCenter))

        Column(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.im_logo_white_horizontal),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 90.dp, vertical = 12.dp),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.Top,
            ) { page ->
                val item = onboardingData[page]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(item.image),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp)
                            .onGloballyPositioned { coords ->
                                if (page == pagerState.currentPage) {
                                    val bottom = coords.positionInRoot().y + coords.size.height - rootTopPx
                                    navyHeightPx = (bottom - coords.size.height * 0.01f).toInt()
                                }
                            },
                    )
                    Spacer(Modifier.height(26.dp))
                    Text(item.title, style = HitbosssType.titleSubsection, color = Gray800)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        item.headline,
                        style = HitbosssType.bodyLargeRegular,
                        color = Gray800,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }

            // Indicador de página (CustomPageControl).
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(onboardingData.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) Secondary500 else Gray300),
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HitButton("Iniciar sesión", onClick = onSignIn, type = HitButtonType.Secondary)
                HitButton("Crear una cuenta", onClick = onSignUp, type = HitButtonType.Tertiary)
            }
        }
    }
}
