package com.tubers.blacklight.ui.state

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.tubers.blacklight.ads.AdsStatus
import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.domain.model.TimerStatus
import com.tubers.blacklight.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises the shared state end to end with in-memory fakes and virtual time:
 * restore, persistence, premium gating, presets, themes, the LED and the auto-off timer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var settings: FakeSettingsRepository
    private lateinit var presets: FakePresetRepository
    private lateinit var themes: FakeThemeRepository
    private lateinit var torch: FakeTorch

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settings = FakeSettingsRepository(LightSettings(brightness = 33, mode = LightMode.SOS))
        presets = FakePresetRepository()
        themes = FakeThemeRepository()
        torch = FakeTorch()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.newViewModel(demoAllowed: Boolean = true, appLifecycle: Lifecycle? = null) = StudioViewModel(
        settingsRepo = settings,
        presetRepo = presets,
        themeRepo = themes,
        torch = torch,
        billing = FakeBilling(),
        clock = { testScheduler.currentTime },
        demoAllowed = demoAllowed,
        appLifecycle = appLifecycle,
    )

    /** Stands in for ProcessLifecycleOwner; "unsafe" skips the main-thread check, which plain JVM tests can't satisfy. */
    private class FakeAppLifecycle : LifecycleOwner {
        val registry: LifecycleRegistry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
        fun foreground() = registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun background() = registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    @Test
    fun loadedState_alwaysCarriesTheRestoredPreferences() = runTest(dispatcher) {
        // The start screen (intro vs Home) is chosen from the first loaded state, so that state
        // must already hold the saved preferences, never the defaults.
        settings.prefsFlow.value = settings.prefsFlow.value.copy(introSeen = true, reducedMotion = true)
        // Like DataStore, reads suspend (disk IO) instead of returning on the spot.
        val slowSettings = object : SettingsRepository by settings {
            override val light = settings.lightFlow.onStart { delay(5) }
            override val preferences = settings.prefsFlow.onStart { delay(5) }
        }
        val vm = StudioViewModel(slowSettings, presets, themes, torch, FakeBilling(), clock = { testScheduler.currentTime })
        val seen = mutableListOf<StudioUiState>()
        val collector = launch { vm.uiState.collect { seen += it } }
        advanceUntilIdle()
        val firstLoaded = seen.first { it.loaded }
        assertTrue(firstLoaded.prefs.introSeen)
        assertTrue(firstLoaded.prefs.reducedMotion)
        assertEquals(33, firstLoaded.light.brightness)
        collector.cancel()
    }

    @Test
    fun noBannerWhileLightActive_andItReturnsWhenTheLightGoesOff() = runTest(dispatcher) {
        val ads = FakeAds(AdsStatus(configured = true, canRequestAds = true, initialized = true))
        settings.lightFlow.value = LightSettings(isOn = true, mode = LightMode.UV_GLOW)
        val vm = StudioViewModel(settings, presets, themes, torch, FakeBilling(), ads = ads, clock = { testScheduler.currentTime })
        runCurrent()
        assertFalse("light on: no banner on any ad screen", vm.uiState.value.bannerAllowed(screenAllowsBanner = true))

        vm.togglePower()
        runCurrent()
        assertTrue("light off on an ad screen", vm.uiState.value.bannerAllowed(screenAllowsBanner = true))
        assertFalse("never on Home", vm.uiState.value.bannerAllowed(screenAllowsBanner = false))

        vm.togglePower()
        runCurrent()
        assertFalse(vm.uiState.value.bannerAllowed(screenAllowsBanner = true))
    }

    @Test
    fun noBannerWithoutConsent_untilGoogleAllowsAds() = runTest(dispatcher) {
        val ads = FakeAds(AdsStatus(configured = true, canRequestAds = false))
        settings.lightFlow.value = LightSettings(isOn = false)
        val vm = StudioViewModel(settings, presets, themes, torch, FakeBilling(), ads = ads, clock = { testScheduler.currentTime })
        runCurrent()
        assertFalse("no consent yet", vm.uiState.value.bannerAllowed(screenAllowsBanner = true))

        ads.status.value = ads.status.value.copy(canRequestAds = true)
        runCurrent()
        assertFalse("consent alone: SDK not initialised yet", vm.uiState.value.bannerAllowed(screenAllowsBanner = true))

        ads.status.value = ads.status.value.copy(initialized = true)
        runCurrent()
        assertTrue(vm.uiState.value.bannerAllowed(screenAllowsBanner = true))
    }

    @Test
    fun adsNeverChangeTheLight() = runTest(dispatcher) {
        // Consent and ad state changes must not touch mode, speed or reduced motion (strobe cap safety).
        val ads = FakeAds()
        settings.lightFlow.value = LightSettings(isOn = false, mode = LightMode.SOS, speedTenths = 20)
        settings.prefsFlow.value = settings.prefsFlow.value.copy(reducedMotion = true, flashWarningAccepted = true)
        val vm = StudioViewModel(settings, presets, themes, torch, FakeBilling(), ads = ads, clock = { testScheduler.currentTime })
        runCurrent()
        val before = vm.uiState.value
        ads.status.value = AdsStatus(configured = true, canRequestAds = true, initialized = true, privacyOptionsRequired = true)
        runCurrent()
        val after = vm.uiState.value
        assertEquals(before.light, after.light)
        assertEquals(before.prefs, after.prefs)
        assertTrue(after.ads.privacyOptionsRequired)
    }

    @Test
    fun releaseBuild_premiumHidden_gatesExplainInsteadOfOpeningPremium() = runTest(dispatcher) {
        val vm = StudioViewModel(
            settings, presets, themes, torch, FakeBilling(),
            clock = { testScheduler.currentTime }, demoAllowed = false, premiumVisible = false,
        )
        val events = mutableListOf<UiEvent>()
        val collector = launch { vm.uiEvents.collect { events += it } }
        runCurrent()
        assertFalse(vm.uiState.value.premiumVisible)

        vm.selectMode(LightMode.BLACKLIGHT)
        runCurrent()
        assertFalse("no Premium screen to open", events.any { it is UiEvent.OpenPremium })
        assertTrue(events.any { it is UiEvent.Message && it.text == "Blacklight comes with Premium in the next update" })
        assertEquals(LightMode.SOS, vm.uiState.value.light.mode)
        collector.cancel()
    }

    @Test
    fun releaseBuild_ignoresDemoPremiumPreview() = runTest(dispatcher) {
        // A preview switched on in a debug install must not unlock Premium after updating to release.
        settings.prefsFlow.value = settings.prefsFlow.value.copy(premiumPreview = true)
        val vm = newViewModel(demoAllowed = false)
        runCurrent()
        assertFalse(vm.uiState.value.isPremium)

        vm.setPremiumPreview(true)
        vm.selectMode(LightMode.BLACKLIGHT)
        runCurrent()
        assertFalse(vm.uiState.value.isPremium)
        assertEquals(LightMode.SOS, vm.uiState.value.light.mode)
    }

    @Test
    fun restoresPersistedLook_andPersistsChanges() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        val s = vm.uiState.value
        assertTrue(s.loaded)
        assertEquals(33, s.light.brightness)
        assertEquals(LightMode.SOS, s.light.mode)

        vm.setBrightness(80)
        runCurrent()
        assertEquals(80, vm.uiState.value.light.brightness)
        advanceTimeBy(1_000)
        assertEquals(80, settings.lightFlow.value.brightness)
    }

    @Test
    fun premiumMode_isGatedAndOpensPremium() = runTest(dispatcher) {
        val vm = newViewModel()
        val events = mutableListOf<UiEvent>()
        val collector = launch { vm.uiEvents.collect { events += it } }
        runCurrent()

        vm.selectMode(LightMode.STROBE)
        runCurrent()
        assertEquals(LightMode.SOS, vm.uiState.value.light.mode)
        assertTrue(events.any { it is UiEvent.OpenPremium })

        vm.setPremiumPreview(true)
        vm.selectMode(LightMode.STROBE)
        vm.acceptFlashWarning()
        runCurrent()
        assertEquals(LightMode.STROBE, vm.uiState.value.light.mode)
        collector.cancel()
    }

    @Test
    fun flashingModes_waitForWarning_andAcceptanceIsRemembered() = runTest(dispatcher) {
        settings.lightFlow.value = LightSettings(mode = LightMode.UV_GLOW)
        val vm = newViewModel()
        runCurrent()
        assertEquals(null, vm.uiState.value.flashWarningFor)

        vm.selectMode(LightMode.SOS)
        runCurrent()
        assertEquals(LightMode.SOS, vm.uiState.value.flashWarningFor)
        assertEquals("nothing flashes before the answer", LightMode.UV_GLOW, vm.uiState.value.light.mode)

        vm.declineFlashWarning()
        runCurrent()
        assertEquals(null, vm.uiState.value.flashWarningFor)
        assertEquals(LightMode.UV_GLOW, vm.uiState.value.light.mode)

        vm.selectMode(LightMode.SOS)
        vm.acceptFlashWarning()
        runCurrent()
        assertEquals(LightMode.SOS, vm.uiState.value.light.mode)
        assertTrue(vm.uiState.value.flashingAllowed)
        advanceTimeBy(1_000)
        assertTrue("choice saved to settings", settings.prefsFlow.value.flashWarningAccepted)

        vm.setPremiumPreview(true)
        vm.selectMode(LightMode.STROBE)
        runCurrent()
        assertEquals("asked only once", null, vm.uiState.value.flashWarningFor)
        assertEquals(LightMode.STROBE, vm.uiState.value.light.mode)
    }

    @Test
    fun restoredFlashingMode_staysStillUntilWarningAnswered() = runTest(dispatcher) {
        // The fixture restores SOS with the warning never accepted (e.g. saved by an older version).
        val vm = newViewModel()
        runCurrent()
        val s = vm.uiState.value
        assertEquals(LightMode.SOS, s.flashWarningFor)
        assertFalse(s.flashingAllowed)

        vm.declineFlashWarning()
        runCurrent()
        assertEquals(LightMode.UV_GLOW, vm.uiState.value.light.mode)
        assertTrue(vm.uiState.value.flashingAllowed)
    }

    @Test
    fun presetWithFlashingMode_alsoWaitsForWarning() = runTest(dispatcher) {
        settings.lightFlow.value = LightSettings(mode = LightMode.UV_GLOW)
        val vm = newViewModel()
        runCurrent()
        vm.selectMode(LightMode.SOS)
        vm.acceptFlashWarning()
        vm.savePreset("Signal")
        runCurrent()
        vm.resetSettings() // also asks for the warning again
        runCurrent()

        vm.applyPreset(vm.uiState.value.presets.first { it.name == "Signal" })
        runCurrent()
        assertEquals(LightMode.SOS, vm.uiState.value.flashWarningFor)
        assertEquals(LightMode.UV_GLOW, vm.uiState.value.light.mode)
        vm.acceptFlashWarning()
        runCurrent()
        assertEquals(LightMode.SOS, vm.uiState.value.light.mode)
    }

    @Test
    fun presets_saveLimitApplyRenameDelete() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        repeat(5) { vm.savePreset("P$it"); runCurrent() }
        assertEquals(5, vm.uiState.value.presets.size)

        vm.savePreset("sixth")
        runCurrent()
        assertEquals("free plan stops at 5", 5, vm.uiState.value.presets.size)

        val first = vm.uiState.value.presets.last()
        vm.setBrightness(10)
        vm.applyPreset(first)
        runCurrent()
        assertEquals(33, vm.uiState.value.light.brightness)
        assertTrue(vm.uiState.value.light.isOn)

        vm.renamePreset(first, "  Cozy  ")
        runCurrent()
        assertEquals("Cozy", vm.uiState.value.presets.last().name)

        vm.deletePreset(vm.uiState.value.presets.last())
        runCurrent()
        assertEquals(4, vm.uiState.value.presets.size)
    }

    @Test
    fun themes_createAppliesAndDeleteClearsReference() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        vm.createTheme("Lime")
        runCurrent()
        val theme = vm.uiState.value.customThemes.single()
        assertEquals("Lime", theme.name)
        assertEquals(theme.id, vm.uiState.value.light.themeId)

        vm.createTheme("Second")
        runCurrent()
        assertEquals("free plan allows one custom theme", 1, vm.uiState.value.customThemes.size)

        vm.deleteTheme(theme)
        runCurrent()
        assertTrue(vm.uiState.value.customThemes.isEmpty())
        assertEquals(null, vm.uiState.value.light.themeId)
    }

    @Test
    fun timer_countsDown_pauses_andTurnsLightOffWhenItExpires() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        vm.setLedEnabled(true)
        runCurrent()
        assertTrue("LED follows the light", torch.isTorchOn.value)

        vm.selectTimerDuration(1)
        vm.startTimer()
        runCurrent()
        assertEquals(TimerStatus.RUNNING, vm.uiState.value.timer.status)

        advanceTimeBy(30_000)
        runCurrent()
        val left = vm.uiState.value.timer.remainingMs
        assertTrue("about 30 s left, was $left", left in 29_000..30_500)

        vm.pauseTimer()
        advanceTimeBy(120_000)
        assertEquals(TimerStatus.PAUSED, vm.uiState.value.timer.status)
        assertTrue(vm.uiState.value.light.isOn)

        vm.resumeTimer()
        advanceUntilIdle()
        assertEquals(TimerStatus.IDLE, vm.uiState.value.timer.status)
        assertFalse(vm.uiState.value.light.isOn)
        assertFalse("LED switches off with the light", torch.isTorchOn.value)
    }

    @Test
    fun powerOff_cancelsRunningTimer() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        vm.startTimer()
        runCurrent()
        vm.togglePower()
        runCurrent()
        assertFalse(vm.uiState.value.light.isOn)
        assertFalse(vm.uiState.value.timer.isActive)
    }

    @Test
    fun reset_restoresDefaultsButKeepsPresets() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        vm.savePreset("Keep me")
        vm.setReducedMotion(true)
        vm.setBrightness(12)
        runCurrent()
        vm.resetSettings()
        runCurrent()
        assertEquals(LightSettings(), vm.uiState.value.light)
        assertFalse(vm.uiState.value.prefs.reducedMotion)
        assertEquals(1, vm.uiState.value.presets.size)
    }

    @Test
    fun led_goesOffInBackground_andComesBackOnReturn() = runTest(dispatcher) {
        val app = FakeAppLifecycle().apply { foreground() }
        settings.lightFlow.value = LightSettings(isOn = true, mode = LightMode.UV_GLOW)
        settings.prefsFlow.value = settings.prefsFlow.value.copy(ledEnabled = true)
        newViewModel(appLifecycle = app.lifecycle)
        runCurrent()
        assertTrue("LED follows the light", torch.isTorchOn.value)

        app.background()
        runCurrent()
        assertFalse("LED off while the app is in the background", torch.isTorchOn.value)

        app.foreground()
        runCurrent()
        assertTrue("LED back on when the app returns", torch.isTorchOn.value)
        assertEquals(listOf(true, false, true), torch.requests)
    }

    @Test
    fun led_staysOffOnReturn_whenTheLightWasTurnedOffMeanwhile() = runTest(dispatcher) {
        val app = FakeAppLifecycle().apply { foreground() }
        settings.lightFlow.value = LightSettings(isOn = true, mode = LightMode.UV_GLOW)
        settings.prefsFlow.value = settings.prefsFlow.value.copy(ledEnabled = true)
        val vm = newViewModel(appLifecycle = app.lifecycle)
        runCurrent()

        app.background()
        runCurrent()
        vm.togglePower()
        runCurrent()
        app.foreground()
        runCurrent()
        assertFalse(torch.isTorchOn.value)
        assertEquals(listOf(true, false), torch.requests)
    }

    @Test
    fun background_neverSwitchesOffATorchWeDidNotStart() = runTest(dispatcher) {
        val app = FakeAppLifecycle().apply { foreground() }
        // Another app (or the quick-settings tile) has the flashlight on; our light is off.
        torch.isTorchOn.value = true
        settings.lightFlow.value = LightSettings(isOn = false, mode = LightMode.UV_GLOW)
        settings.prefsFlow.value = settings.prefsFlow.value.copy(ledEnabled = true)
        newViewModel(appLifecycle = app.lifecycle)
        runCurrent()

        app.background()
        runCurrent()
        app.foreground()
        runCurrent()
        assertTrue("someone else's flashlight is left alone", torch.isTorchOn.value)
        assertTrue(torch.requests.isEmpty())
    }
}
