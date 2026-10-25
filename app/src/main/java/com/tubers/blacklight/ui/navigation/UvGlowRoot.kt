package com.tubers.blacklight.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tubers.blacklight.ui.components.BottomNav
import com.tubers.blacklight.ui.components.FlashWarningDialog
import com.tubers.blacklight.ui.components.NavItem
import com.tubers.blacklight.ui.components.SideNav
import com.tubers.blacklight.ui.components.SubHeader
import com.tubers.blacklight.ui.components.TabHeader
import com.tubers.blacklight.ui.components.UvIcons
import com.tubers.blacklight.ui.screens.EffectsScreen
import com.tubers.blacklight.ui.screens.HomeScreen
import com.tubers.blacklight.ui.screens.IntroScreen
import com.tubers.blacklight.ui.screens.PremiumScreen
import com.tubers.blacklight.ui.screens.PresetsScreen
import com.tubers.blacklight.ui.screens.SettingsScreen
import com.tubers.blacklight.ui.screens.ThemesScreen
import com.tubers.blacklight.ui.screens.TimerScreen
import com.tubers.blacklight.ui.state.StudioUiState
import com.tubers.blacklight.ui.state.StudioViewModel
import com.tubers.blacklight.ui.state.UiEvent
import com.tubers.blacklight.ui.theme.Accent
import com.tubers.blacklight.ui.theme.UvColors
import com.tubers.blacklight.ui.theme.UvGlowTheme
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val EFFECTS = "effects"
    const val THEMES = "themes"
    const val PRESETS = "presets"
    const val SETTINGS = "settings"
    const val TIMER = "timer"
    const val PREMIUM = "premium"
    const val INTRO = "intro"
}

/** Windows at least this wide in landscape get the side rail instead of the bottom bar. */
private val RAIL_MIN_WIDTH = 560.dp

private val Tabs = listOf(
    NavItem(Routes.HOME, "Home", UvIcons.NavHome),
    NavItem(Routes.EFFECTS, "Effects", UvIcons.NavEffects),
    NavItem(Routes.THEMES, "Themes", UvIcons.NavThemes),
    NavItem(Routes.PRESETS, "Presets", UvIcons.NavPresets),
    NavItem(Routes.SETTINGS, "Settings", UvIcons.NavSettings),
)

/** Tab switching keeps one copy of each tab and restores its scroll position. */
private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.openScreen(route: String) {
    navigate(route) { launchSingleTop = true }
}

@Composable
fun UvGlowRoot(vm: StudioViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    if (!state.loaded) {
        // The system splash stays up until this is true; draw the ground color meanwhile.
        Box(Modifier.fillMaxSize().background(UvColors.Ground))
        return
    }

    val accent = remember(state.light.colorArgb, state.light.groundArgb) {
        Accent.from(state.light.colorArgb, state.light.groundArgb)
    }
    UvGlowTheme(accent) {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val ground by animateColorAsState(accent.ground, tween(400), label = "ground")

        // First launch starts on the welcome screen; later launches go straight to Home. Saved so a
        // rotation or window resize rebuilds the same graph the restored back stack belongs to.
        val startRoute = rememberSaveable { if (state.prefs.introSeen) Routes.HOME else Routes.INTRO }
        // One-off events from the ViewModel.
        LaunchedEffect(Unit) {
            vm.uiEvents.collect { event ->
                when (event) {
                    is UiEvent.Message -> scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(event.text)
                    }
                    UiEvent.OpenPremium -> nav.openScreen(Routes.PREMIUM)
                }
            }
        }

        // Photosensitivity warning before Strobe/SOS first runs, wherever it was chosen from.
        state.flashWarningFor?.let { mode ->
            FlashWarningDialog(mode.displayName, onContinue = vm::acceptFlashWarning, onCancel = vm::declineFlashWarning)
        }

        val showChrome = route != null && route != Routes.INTRO
        val isSubScreen = route == Routes.TIMER || route == Routes.PREMIUM

        // Side insets (landscape cutouts, 3-button nav on the side) pad the whole app once; the
        // bars below then only take their top/bottom insets.
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(ground)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            // Landscape and wide windows use a side rail so content keeps its height.
            val useRail = showChrome && maxWidth > maxHeight && maxWidth >= RAIL_MIN_WIDTH
            Row(Modifier.fillMaxSize()) {
                if (useRail) {
                    SideNav(Tabs, selectedRoute = if (isSubScreen) null else route, onSelect = { nav.openTab(it.route) })
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = ground,
                    // Without a bottom bar, content keeps clear of the gesture/nav bar itself.
                    contentWindowInsets = if (useRail) WindowInsets.navigationBars.only(WindowInsetsSides.Bottom) else WindowInsets(0, 0, 0, 0),
                    topBar = {
                        if (showChrome) {
                            if (isSubScreen) {
                                SubHeader(if (route == Routes.TIMER) "Auto-off timer" else "Premium", onBack = { nav.popBackStack() })
                            } else {
                                TabHeader(onPremium = { nav.openScreen(Routes.PREMIUM) })
                            }
                        }
                    },
                    bottomBar = {
                        if (showChrome && !useRail) {
                            BottomNav(Tabs, selectedRoute = if (isSubScreen) null else route, onSelect = { nav.openTab(it.route) })
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(snackbar) { data ->
                            Snackbar(containerColor = UvColors.Text, contentColor = Color(0xFF140E1C)) {
                                Text(data.visuals.message, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    },
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = startRoute,
                        modifier = Modifier.padding(padding).imePadding(),
                        enterTransition = { fadeIn(tween(220)) },
                        exitTransition = { fadeOut(tween(180)) },
                    ) {
                        // Each destination reads state itself, so the graph builder captures only stable
                        // objects and isn't rebuilt while a slider is being dragged.
                        composable(Routes.HOME) {
                            HomeScreen(
                                vm.state(), vm,
                                onOpenEffects = { nav.openTab(Routes.EFFECTS) },
                                onOpenThemes = { nav.openTab(Routes.THEMES) },
                                onOpenTimer = { nav.openScreen(Routes.TIMER) },
                                onOpenPresets = { nav.openTab(Routes.PRESETS) },
                            )
                        }
                        composable(Routes.EFFECTS) { EffectsScreen(vm.state(), vm, onOpenSettings = { nav.openTab(Routes.SETTINGS) }) }
                        composable(Routes.THEMES) { ThemesScreen(vm.state(), vm) }
                        composable(Routes.PRESETS) { PresetsScreen(vm.state(), vm) }
                        composable(Routes.SETTINGS) { SettingsScreen(vm.state(), vm, onReplayIntro = { nav.openScreen(Routes.INTRO) }) }
                        composable(Routes.TIMER) { TimerScreen(vm.state(), vm) }
                        composable(Routes.PREMIUM) { PremiumScreen(vm.state(), vm) }
                        composable(Routes.INTRO) {
                            IntroScreen(onGetStarted = {
                                vm.markIntroSeen()
                                if (nav.previousBackStackEntry != null) {
                                    nav.popBackStack() // replayed from Settings
                                } else {
                                    nav.navigate(Routes.HOME) { popUpTo(Routes.INTRO) { inclusive = true } }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioViewModel.state(): StudioUiState = uiState.collectAsStateWithLifecycle().value
