package com.hitbosss.presentation.feature.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hitbosss.R
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Primary500
import androidx.compose.animation.togetherWith
import com.hitbosss.presentation.feature.community.CommunityScreen
import com.hitbosss.presentation.feature.profile.ProfileScreen
import com.hitbosss.presentation.feature.ranking.RankingScreen
import androidx.compose.ui.res.stringResource

private enum class MainTab(@androidx.annotation.StringRes val label: Int, @DrawableRes val icon: Int) {
    Ranking(R.string.tab_ranking, R.drawable.im_tab_ranking),
    Community(R.string.tab_community, R.drawable.im_tab_community),
    Profile(R.string.tab_profile, R.drawable.im_tab_profile),
}

/**
 * Contenedor principal tras el login. Barra de pestañas equivalente al MainTabView de iOS
 * (Ranking, Community, Profile · tint primary500 · fondo gray100).
 */
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onRecordHit: (String, Double) -> Unit = { _, _ -> },
    onSavedHits: () -> Unit = {},
    onTutorial: (String) -> Unit = {},
    onOpenGroup: (Int) -> Unit = {},
    onOpenEvent: (Int) -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onEditHit: (com.hitbosss.presentation.feature.profile.EditHitNav) -> Unit = {},
) {
    // rememberSaveable para conservar la pestaña al volver de pantallas que sacan a MainScreen de composición.
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selected = MainTab.entries[selectedIndex]

    // Precarga: se instancian los 3 ViewModels al entrar a la app, así sus datos se cargan en
    // paralelo desde el arranque y la primera visita a cada pestaña ya está lista (sin spinner).
    // Viven en el scope de MAIN, por lo que persisten entre pestañas y NO recargan al cambiar de tab
    // (solo por acción/RefreshCoordinator, pull-to-refresh o antigüedad).
    val rankingVM: com.hitbosss.presentation.feature.ranking.RankingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val communityVM: com.hitbosss.presentation.feature.community.CommunityViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val profileVM: com.hitbosss.presentation.feature.profile.ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // Refresco por antigüedad: al cambiar de pestaña, si los datos llevan mucho tiempo sin
    // actualizarse se recargan en segundo plano (sin spinner). Ver no recarga; solo el tiempo.
    androidx.compose.runtime.LaunchedEffect(selectedIndex) {
        when (MainTab.entries[selectedIndex]) {
            MainTab.Ranking -> rankingVM.refreshIfStale()
            MainTab.Community -> communityVM.refreshIfStale()
            MainTab.Profile -> profileVM.refreshIfStale()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Gray100) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selectedIndex = tab.ordinal },
                        icon = {
                            // Iconos de iOS (tabRanking/tabCommunity/tabProfile), tintados según selección.
                            Image(
                                painterResource(tab.icon),
                                contentDescription = stringResource(tab.label),
                                colorFilter = ColorFilter.tint(if (selected == tab) Primary500 else Gray500),
                                modifier = Modifier.height(26.dp),
                            )
                        },
                        label = { Text(stringResource(tab.label), style = HitbosssType.bodySmallRegular) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary500,
                            selectedTextColor = Primary500,
                            indicatorColor = Gray100,
                            unselectedIconColor = Gray500,
                            unselectedTextColor = Gray500,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(innerPadding)) {
            // Banner de subida en curso (iOS MainTabView #649): "Subiendo HIT… X%" + barra.
            val uploadVM: UploadBannerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val uploadState by uploadVM.uploadState.collectAsStateWithLifecycle()
            if (uploadState.inProgress) {
                androidx.compose.foundation.layout.Row(
                    Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
                ) {
                    Text(stringResource(R.string.upload_notif_title), style = HitbosssType.bodySmallRegular, color = com.hitbosss.presentation.designsystem.theme.Gray800)
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    Text("${(uploadState.progress * 100).toInt()}%", style = HitbosssType.bodySmallRegular, color = com.hitbosss.presentation.designsystem.theme.Gray800)
                }
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { uploadState.progress },
                    color = Primary500,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        Box(Modifier.fillMaxSize()) {
            // Barrido horizontal entre pestañas según la dirección del cambio (izq/der).
            androidx.compose.animation.AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    val spec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(280)
                    val fadeSpec = androidx.compose.animation.core.tween<Float>(280)
                    (androidx.compose.animation.slideInHorizontally(spec) { w -> dir * w } +
                        androidx.compose.animation.fadeIn(fadeSpec)) togetherWith
                        (androidx.compose.animation.slideOutHorizontally(spec) { w -> -dir * w } +
                            androidx.compose.animation.fadeOut(fadeSpec))
                },
                label = "mainTabSwipe",
            ) { index ->
                when (MainTab.entries[index]) {
                    MainTab.Ranking -> RankingScreen(onRecordHit = onRecordHit, onSavedHits = onSavedHits, onTutorial = onTutorial, onOpenUserProfile = onOpenUserProfile, viewModel = rankingVM)
                    MainTab.Community -> CommunityScreen(
                        onOpenGroup = onOpenGroup,
                        onOpenEvent = onOpenEvent,
                        onCreateGroup = onCreateGroup,
                        onCreateEvent = onCreateEvent,
                        viewModel = communityVM,
                    )
                    MainTab.Profile -> ProfileScreen(onOpenSettings = onOpenSettings, onEditProfile = onEditProfile, onEditHit = onEditHit, viewModel = profileVM)
                }
            }
        }
        }
    }
}
