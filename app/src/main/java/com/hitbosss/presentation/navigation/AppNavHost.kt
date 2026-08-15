package com.hitbosss.presentation.navigation

import com.hitbosss.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hitbosss.presentation.feature.community.CommunityMembersScreen
import com.hitbosss.presentation.feature.community.CreateEventScreen
import com.hitbosss.presentation.feature.community.CreateGroupScreen
import com.hitbosss.presentation.feature.community.EditEventScreen
import com.hitbosss.presentation.feature.community.EditGroupScreen
import com.hitbosss.presentation.feature.community.EventRankingScreen
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
import com.hitbosss.presentation.feature.settings.ExerciseTutorialScreen
import com.hitbosss.presentation.feature.hit.EditVideoScreen
import com.hitbosss.presentation.feature.hit.SavedHitsScreen

/** Grafo de navegación raíz (equivale al AppCoordinator de iOS). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    fun goAuthedTo(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.WELCOME) { inclusive = true }
        }
    }

    // Padding superior (altura de la barra de estado) en todas las pantallas de contenido para que
    // queden debajo de la barra. Launch y Welcome son a sangre (dibujan tras la barra).
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val fullBleed = currentRoute == Routes.LAUNCH || currentRoute == Routes.WELCOME
    val rootModifier = if (fullBleed) Modifier else Modifier.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)

    // Iconos de la barra de estado: oscuros sobre el fondo blanco de las pantallas de contenido,
    // claros sobre el fondo oscuro de Launch/Welcome.
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.LaunchedEffect(fullBleed) {
            val window = (view.context as android.app.Activity).window
            androidx.core.view.WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !fullBleed
        }
    }

    Box(rootModifier) {
    // Transiciones de navegación tipo "push" (deslizamiento horizontal) para todas las pantallas.
    val animSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(300)
    val fadeAnim = androidx.compose.animation.core.tween<Float>(300)
    NavHost(
        navController = navController,
        startDestination = Routes.LAUNCH,
        // Nueva pantalla entra desde la derecha; la actual se va un poco a la izquierda.
        enterTransition = { androidx.compose.animation.slideInHorizontally(animSpec) { it } + androidx.compose.animation.fadeIn(fadeAnim) },
        exitTransition = { androidx.compose.animation.slideOutHorizontally(animSpec) { -it / 4 } + androidx.compose.animation.fadeOut(fadeAnim) },
        // Al volver (pop): la actual sale a la derecha; la anterior vuelve desde la izquierda.
        popEnterTransition = { androidx.compose.animation.slideInHorizontally(animSpec) { -it / 4 } + androidx.compose.animation.fadeIn(fadeAnim) },
        popExitTransition = { androidx.compose.animation.slideOutHorizontally(animSpec) { it } + androidx.compose.animation.fadeOut(fadeAnim) },
    ) {

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
            // Procesa el deeplink entrante (hitbosss://group|event/{id}) una vez autenticado.
            val deepLink by com.hitbosss.core.deeplink.DeepLinkBus.pending.collectAsStateWithLifecycle()
            LaunchedEffect(deepLink) {
                deepLink?.let { uri ->
                    when (uri.host) {
                        "group" -> uri.lastPathSegment?.toIntOrNull()?.let { navController.navigate(Routes.groupRanking(it)) }
                        "event" -> uri.lastPathSegment?.toIntOrNull()?.let { navController.navigate(Routes.eventRanking(it)) }
                        "profile" -> uri.lastPathSegment?.let { navController.navigate(Routes.userProfile(it)) }
                        // Desde la notificación "Error al subir HIT" → Ir a 'HIT Guardados'.
                        "savedhits" -> navController.navigate(Routes.SAVED_HITS)
                    }
                    com.hitbosss.core.deeplink.DeepLinkBus.consume()
                }
            }
            MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                onRecordHit = { exercise, weight -> navController.navigate(Routes.recordHit(exercise, weight)) },
                onSavedHits = { navController.navigate(Routes.SAVED_HITS) },
                onTutorial = { apiKey -> navController.navigate(Routes.tutorialsExercise(apiKey)) },
                onOpenGroup = { id -> navController.navigate(Routes.groupRanking(id)) },
                onOpenEvent = { id -> navController.navigate(Routes.eventRanking(id)) },
                onCreateGroup = { navController.navigate(Routes.CREATE_GROUP) },
                onCreateEvent = { navController.navigate(Routes.CREATE_EVENT) },
                onOpenUserProfile = { userId -> navController.navigate(Routes.userProfile(userId)) },
                onEditHit = { e ->
                    navController.navigate(Routes.editUploadedHit(e.exercise, e.weight, e.hitId, e.performedAt, e.localPath))
                },
                onOpenMetricDetail = { type -> navController.navigate(Routes.metricDetail(type)) },
            )
        }

        composable(
            Routes.METRIC_DETAIL,
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
        ) { entry ->
            // Físico (weight/fat/muscle) → detalle de composición; cualquier otro type = ejercicio → detalle de fuerza.
            val type = entry.arguments?.getString("type")
            if (type in listOf("weight", "fat", "muscle")) {
                com.hitbosss.presentation.feature.metrics.MetricDetailScreen(onBack = { navController.popBackStack() })
            } else {
                com.hitbosss.presentation.feature.metrics.StrengthDetailScreen(onBack = { navController.popBackStack() })
            }
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
            Routes.COMMUNITY_MEMBERS,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
            ),
        ) { entry ->
            val type = entry.arguments?.getString("type") ?: "group"
            CommunityMembersScreen(
                titleRes = if (type == "event") R.string.event_members_screen_title else R.string.group_members_title,
                onBack = { navController.popBackStack() },
                onOpenUserProfile = { userId -> navController.navigate(Routes.userProfile(userId)) },
            )
        }

        composable(
            Routes.EDIT_GROUP,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            EditGroupScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }

        composable(
            Routes.EDIT_EVENT,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            EditEventScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }

        composable(
            Routes.EVENT_RANKING,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            val eventId = entry.arguments?.getInt("id") ?: 0
            EventRankingScreen(
                onBack = { navController.popBackStack() },
                onOpenMembers = { id -> navController.navigate(Routes.communityMembers("event", id)) },
                onEditEvent = { id -> navController.navigate(Routes.editEvent(id)) },
                // Sube el HIT al contexto del evento.
                onRecordHit = { exercise, weight -> navController.navigate(Routes.recordHit(exercise, weight, "e$eventId")) },
                onSavedHits = { navController.navigate(Routes.SAVED_HITS) },
                onTutorial = { apiKey -> navController.navigate(Routes.tutorialsExercise(apiKey)) },
                onOpenUserProfile = { userId -> navController.navigate(Routes.userProfile(userId)) },
            )
        }

        composable(
            Routes.GROUP_RANKING,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            val groupId = entry.arguments?.getInt("id") ?: 0
            GroupRankingScreen(
                onBack = { navController.popBackStack() },
                onOpenMembers = { id -> navController.navigate(Routes.communityMembers("group", id)) },
                onEditGroup = { id -> navController.navigate(Routes.editGroup(id)) },
                // Sube el HIT al contexto del grupo.
                onRecordHit = { exercise, weight -> navController.navigate(Routes.recordHit(exercise, weight, "g$groupId")) },
                onSavedHits = { navController.navigate(Routes.SAVED_HITS) },
                onTutorial = { apiKey -> navController.navigate(Routes.tutorialsExercise(apiKey)) },
                onOpenUserProfile = { userId -> navController.navigate(Routes.userProfile(userId)) },
            )
        }

        composable(
            Routes.RECORD_HIT,
            arguments = listOf(
                navArgument("exercise") { type = NavType.StringType },
                navArgument("weight") { type = NavType.StringType },
                navArgument("context") { type = NavType.StringType },
            ),
        ) { entry ->
            val exercise = entry.arguments?.getString("exercise").orEmpty()
            val weight = entry.arguments?.getString("weight")?.toDoubleOrNull() ?: 0.0
            val context = entry.arguments?.getString("context") ?: "global"
            RecordHitScreen(
                onClose = { navController.popBackStack() },
                // Tras grabar se va a "Divide el vídeo en dos partes" (no sube directo, igual que iOS).
                onRecorded = { path -> navController.navigate(Routes.editVideo(exercise, weight, context, path)) },
            )
        }

        composable(
            Routes.EDIT_VIDEO,
            arguments = listOf(
                navArgument("exercise") { type = NavType.StringType },
                navArgument("weight") { type = NavType.StringType },
                navArgument("context") { type = NavType.StringType },
                navArgument("video") { type = NavType.StringType },
            ),
        ) { entry ->
            // En modo edición (desde el perfil) no hay RECORD_HIT en la pila: se vuelve atrás directamente.
            val isEdit = entry.arguments?.getString("context")?.startsWith("edit") == true
            EditVideoScreen(
                onClose = { navController.popBackStack() },
                onUploaded = {
                    if (isEdit) navController.popBackStack()
                    else navController.popBackStack(Routes.RECORD_HIT, inclusive = true)
                },
            )
        }

        composable(Routes.SAVED_HITS) {
            SavedHitsScreen(onBack = { navController.popBackStack() })
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
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                // Al guardar, vuelve al perfil (que ya se recarga vía RefreshCoordinator), saltándose Ajustes.
                onSaved = { navController.popBackStack(Routes.MAIN, inclusive = false) },
            )
        }

        composable(Routes.COMMUNITY_RULES) {
            CommunityRulesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TUTORIALS) {
            TutorialScreen(
                onBack = { navController.popBackStack() },
                onOpenExercise = { apiKey -> navController.navigate(Routes.tutorialsExercise(apiKey)) },
            )
        }

        // Tutorial "Cómo grabar tu HIT" enfocado a un ejercicio (desde el modal de subir).
        composable(
            Routes.TUTORIALS_EXERCISE,
            arguments = listOf(navArgument("apiKey") { type = NavType.StringType }),
        ) { entry ->
            val apiKey = entry.arguments?.getString("apiKey") ?: "officialPowerlifting"
            val ctx = androidx.compose.ui.platform.LocalContext.current
            // Al abrirlo se marca visto (igual que iOS markTutorialSeen onAppear).
            LaunchedEffect(apiKey) {
                com.hitbosss.presentation.feature.ranking.TutorialTracker.markExerciseTutorialSeen(ctx, apiKey)
            }
            ExerciseTutorialScreen(apiKey = apiKey, onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CALCULATOR) {
            CalculatorScreen(onBack = { navController.popBackStack() })
        }
    }
    }
}
