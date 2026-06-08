package com.hitbosss.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hitbosss.presentation.feature.community.CreateEventScreen
import com.hitbosss.presentation.feature.community.CreateGroupScreen
import com.hitbosss.presentation.feature.community.EventDetailScreen
import com.hitbosss.presentation.feature.community.EventRankingScreen
import com.hitbosss.presentation.feature.community.GroupDetailScreen
import com.hitbosss.presentation.feature.community.GroupRankingScreen
import com.hitbosss.presentation.feature.auth.RecoverPasswordScreen
import com.hitbosss.presentation.feature.auth.SignInScreen
import com.hitbosss.presentation.feature.auth.SignUpScreen
import com.hitbosss.presentation.feature.auth.WelcomeScreen
import com.hitbosss.presentation.feature.forceupdate.ForceUpdateScreen
import com.hitbosss.presentation.feature.launch.LaunchScreen
import com.hitbosss.presentation.feature.launch.LaunchTarget
import com.hitbosss.presentation.feature.launch.LaunchViewModel
import com.hitbosss.presentation.feature.main.MainScreen
import com.hitbosss.presentation.feature.hit.RecordHitScreen
import com.hitbosss.presentation.feature.profile.CompleteProfileScreen
import com.hitbosss.presentation.feature.profile.ProfileScreen
import com.hitbosss.presentation.feature.settings.CalculatorScreen
import com.hitbosss.presentation.feature.settings.CommunityRulesScreen
import com.hitbosss.presentation.feature.settings.EditProfileScreen
import com.hitbosss.presentation.feature.settings.PrivacyPolicyScreen
import com.hitbosss.presentation.feature.settings.SettingsScreen
import com.hitbosss.presentation.feature.settings.TutorialScreen

/** Grafo de navegación raíz (equivale al AppCoordinator de iOS). */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    fun goAuthedTo(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.WELCOME) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LAUNCH) {

        composable(Routes.LAUNCH) {
            val viewModel: LaunchViewModel = hiltViewModel()
            val target by viewModel.target.collectAsStateWithLifecycle()
            LaunchScreen()
            LaunchedEffect(target) {
                val route = when (target) {
                    LaunchTarget.LOADING -> null
                    LaunchTarget.FORCE_UPDATE -> Routes.FORCE_UPDATE
                    LaunchTarget.WELCOME -> Routes.WELCOME
                    LaunchTarget.COMPLETE_PROFILE -> Routes.COMPLETE_PROFILE
                    LaunchTarget.MAIN -> Routes.MAIN
                }
                route?.let {
                    navController.navigate(it) { popUpTo(Routes.LAUNCH) { inclusive = true } }
                }
            }
        }

        composable(Routes.FORCE_UPDATE) { ForceUpdateScreen() }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onSignIn = { navController.navigate(Routes.SIGN_IN) },
                onSignUp = { navController.navigate(Routes.SIGN_UP) },
            )
        }

        composable(Routes.SIGN_IN) {
            SignInScreen(
                onBack = { navController.popBackStack() },
                onRecoverPassword = { navController.navigate(Routes.RECOVER_PASSWORD) },
                onGoToMain = { goAuthedTo(Routes.MAIN) },
                onGoToCompleteProfile = { goAuthedTo(Routes.COMPLETE_PROFILE) },
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onGoToMain = { goAuthedTo(Routes.MAIN) },
                onGoToCompleteProfile = { goAuthedTo(Routes.COMPLETE_PROFILE) },
            )
        }

        composable(Routes.RECOVER_PASSWORD) {
            RecoverPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.COMPLETE_PROFILE) {
            CompleteProfileScreen(
                onRegistered = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.COMPLETE_PROFILE) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.COMPLETE_PROFILE) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onRecordHit = { exercise, weight -> navController.navigate(Routes.recordHit(exercise, weight)) },
                onOpenGroup = { id -> navController.navigate(Routes.groupRanking(id)) },
                onOpenEvent = { id -> navController.navigate(Routes.eventRanking(id)) },
                onCreateGroup = { navController.navigate(Routes.CREATE_GROUP) },
                onCreateEvent = { navController.navigate(Routes.CREATE_EVENT) },
                onOpenUserProfile = { userId -> navController.navigate(Routes.userProfile(userId)) },
            )
        }

        composable(
            Routes.USER_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CREATE_GROUP) {
            CreateGroupScreen(onBack = { navController.popBackStack() }, onCreated = { navController.popBackStack() })
        }

        composable(Routes.CREATE_EVENT) {
            CreateEventScreen(onBack = { navController.popBackStack() }, onCreated = { navController.popBackStack() })
        }

        composable(
            Routes.GROUP_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            GroupDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.EVENT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            EventDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.EVENT_RANKING,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            EventRankingScreen(
                onBack = { navController.popBackStack() },
                onInfo = { id -> navController.navigate(Routes.eventDetail(id)) },
                onRecordHit = { exercise, weight -> navController.navigate(Routes.recordHit(exercise, weight)) },
            )
        }

        composable(
            Routes.GROUP_RANKING,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            GroupRankingScreen(
                onBack = { navController.popBackStack() },
                onInfo = { id -> navController.navigate(Routes.groupDetail(id)) },
                onRecordHit = { exercise, weight -> navController.navigate(Routes.recordHit(exercise, weight)) },
            )
        }

        composable(
            Routes.RECORD_HIT,
            arguments = listOf(
                navArgument("exercise") { type = NavType.StringType },
                navArgument("weight") { type = NavType.StringType },
            ),
        ) {
            RecordHitScreen(
                onClose = { navController.popBackStack() },
                onUploaded = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenCalculator = { navController.navigate(Routes.CALCULATOR) },
                onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                onCommunityRules = { navController.navigate(Routes.COMMUNITY_RULES) },
                onTutorials = { navController.navigate(Routes.TUTORIALS) },
                onPrivacyPolicy = { navController.navigate(Routes.PRIVACY_POLICY) },
                onLoggedOut = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.COMMUNITY_RULES) {
            CommunityRulesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TUTORIALS) {
            TutorialScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CALCULATOR) {
            CalculatorScreen(onBack = { navController.popBackStack() })
        }
    }
}
