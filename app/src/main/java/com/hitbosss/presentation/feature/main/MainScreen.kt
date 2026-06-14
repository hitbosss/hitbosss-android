package com.hitbosss.presentation.feature.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    onRecordHit: (String, Double) -> Unit = { _, _ -> },
    onSavedHits: () -> Unit = {},
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
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (selected) {
                MainTab.Ranking -> RankingScreen(onRecordHit = onRecordHit, onSavedHits = onSavedHits, onOpenUserProfile = onOpenUserProfile)
                MainTab.Community -> CommunityScreen(
                    onOpenGroup = onOpenGroup,
                    onOpenEvent = onOpenEvent,
                    onCreateGroup = onCreateGroup,
                    onCreateEvent = onCreateEvent,
                )
                MainTab.Profile -> ProfileScreen(onOpenSettings = onOpenSettings, onEditHit = onEditHit)
            }
        }
    }
}
