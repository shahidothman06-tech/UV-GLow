package com.tubers.blacklight.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tubers.blacklight.ads.AdaptiveBanner
import com.tubers.blacklight.ui.components.BottomNav
import com.tubers.blacklight.ui.components.FlashWarningDialog
import com.tubers.blacklight.ui.components.findActivity
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
import com.tubers.blacklight.ui.screens.PrivacyPolicyScreen
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
    const val PRIVACY = "privacy"
    const val INTRO = "intro"

    /**
     * Header title for screens opened on top of a tab (they get a back button and no tab is
     * highlighted), or null for the tabs themselves and the intro.
     */
    /** Screens with a banner slot at the bottom. Never Home, the intro or the sub-screens. */
    fun showsBanner(route: String?): Boolean = route in setOf(EFFECTS, THEMES, PRESETS, SETTINGS)

    fun subScreenTitle(route: String?): String? = when (route) {
        TIMER -> "Auto-off timer"
        PREMIUM -> "Premium"
        PRIVACY -> "Privacy policy"
        else -> null
    }
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

    // The screen is the light, so it must not dim or lock while the light is on. Cleared when the
    // light goes off or this UI leaves the window.
    val view = LocalView.current
    val keepAwake = state.loaded && state.light.isOn
    DisposableEffect(view, keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

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

        // Back on Home would close the app: free users see Premium first, and backing out of that
        // closes the app. Leaving Premium any other way, or subscribing, cancels it.
        val context = LocalContext.current
        var exitPaywall by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(route, state.isPremium) {
            if (route != Routes.PREMIUM || state.isPremium) exitPaywall = false
        }
        val showExitPaywall = state.premiumVisible && !state.isPremium
        val closeExitPaywall: () -> Unit = { context.findActivity()?.finish() }
        // One-off events from the ViewModel.
        LaunchedEffect(Unit) {
            vm.uiEvents.collect { event ->
                when (event) {
                    is UiEvent.Message -> scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(event.text)
                    }
                    UiEvent.OpenPremium -> if (state.premiumVisible) nav.openScreen(Routes.PREMIUM)
                }
            }
        }

        // Photosensitivity warning before Strobe/SOS first runs, wherever it was chosen from.
        state.flashWarningFor?.let { mode ->
            FlashWarningDialog(mode.displayName, onContinue = vm::acceptFlashWarning, onCancel = vm::declineFlashWarning)
        }

        val showChrome = route != null && route != Routes.INTRO
        val subScreenTitle = Routes.subScreenTitle(route)
        val isSubScreen = subScreenTitle != null

        // Landscape and wide windows use a side rail so content keeps its height. The window size
        // comes from the configuration, not BoxWithConstraints: with the NavHost inside that
        // sub-composition, a second rotation restored an out-of-date back stack.
        val window = LocalConfiguration.current
        val useRail = showChrome && window.screenWidthDp > window.screenHeightDp && window.screenWidthDp.dp >= RAIL_MIN_WIDTH
        // Side insets (landscape cutouts, 3-button nav on the side) pad the whole app once; the
        // bars below then only take their top/bottom insets.
        Box(
            Modifier
                .fillMaxSize()
                .background(ground)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
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
                                SubHeader(subScreenTitle.orEmpty(), onBack = {
                                    if (exitPaywall && route == Routes.PREMIUM) closeExitPaywall() else nav.popBackStack()
                                })
                            } else {
                                TabHeader(onPremium = if (state.premiumVisible) ({ nav.openScreen(Routes.PREMIUM) }) else null)
                            }
                        }
                    },
                    bottomBar = {
                        // The banner sits in the bottom bar slot, above the tabs, so the Scaffold
                        // pads content above it: it never covers controls.
                        val showBanner = state.bannerAllowed(Routes.showsBanner(route))
                        Column {
                            if (showBanner) {
                                AdaptiveBanner(
                                    vm.adUnitId, UvColors.NavBackground,
                                    // With the side rail there's no tab bar below to clear the gesture bar.
                                    if (useRail) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier,
                                )
                            }
                            if (showChrome && !useRail) {
                                BottomNav(Tabs, selectedRoute = if (isSubScreen) null else route, onSelect = { nav.openTab(it.route) })
                            }
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
                            BackHandler(enabled = showExitPaywall) {
                                exitPaywall = true
                                nav.openScreen(Routes.PREMIUM)
                            }
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
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                vm.state(), vm,
                                onReplayIntro = { nav.openScreen(Routes.INTRO) },
                                onOpenPrivacy = { nav.openScreen(Routes.PRIVACY) },
                            )
                        }
                        composable(Routes.TIMER) { TimerScreen(vm.state(), vm) }
                        composable(Routes.PREMIUM) {
                            BackHandler(enabled = exitPaywall) { closeExitPaywall() }
                            PremiumScreen(vm.state(), vm)
                        }
                        composable(Routes.PRIVACY) {
                            PrivacyPolicyScreen(
                                onBack = { nav.popBackStack() },
                                onLinkFailed = { vm.showMessage("No app found to open that link") },
                            )
                        }
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
