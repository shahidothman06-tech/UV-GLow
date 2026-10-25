package com.tubers.blacklight.ui.state

import android.app.Activity
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tubers.blacklight.BuildConfig
import com.tubers.blacklight.UvGlowApp
import com.tubers.blacklight.billing.BillingGateway
import com.tubers.blacklight.billing.PlanType
import com.tubers.blacklight.billing.PurchaseOutcome
import com.tubers.blacklight.billing.RestoreOutcome
import com.tubers.blacklight.domain.logic.ColorMath
import com.tubers.blacklight.domain.logic.PremiumRules
import com.tubers.blacklight.domain.logic.StudioLogic
import com.tubers.blacklight.domain.logic.TimerLogic
import com.tubers.blacklight.domain.model.AppPreferences
import com.tubers.blacklight.domain.model.LightMode
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.domain.model.Preset
import com.tubers.blacklight.domain.model.Swatches
import com.tubers.blacklight.domain.model.ThemeItem
import com.tubers.blacklight.domain.model.TimerState
import com.tubers.blacklight.domain.model.TimerStatus
import com.tubers.blacklight.domain.repository.PresetRepository
import com.tubers.blacklight.domain.repository.SettingsRepository
import com.tubers.blacklight.domain.repository.ThemeRepository
import com.tubers.blacklight.hardware.TorchController
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The app's single source of UI state, shared by every screen.
 *
 * The look ([LightSettings]) and preferences are held in memory for instant feedback and written
 * to DataStore shortly after they stop changing, so dragging a slider doesn't hammer the disk.
 * Presets and themes stream straight from Room.
 */
class StudioViewModel(
    private val settingsRepo: SettingsRepository,
    private val presetRepo: PresetRepository,
    private val themeRepo: ThemeRepository,
    private val torch: TorchController,
    private val billing: BillingGateway,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
    private val persistDelayMs: Long = 250L,
    /** Demo checkout and the Premium preview switch; never available in release builds. */
    private val demoAllowed: Boolean = BuildConfig.DEBUG,
) : ViewModel() {

    private val loaded = MutableStateFlow(false)
    private val light = MutableStateFlow(LightSettings())
    private val prefs = MutableStateFlow(AppPreferences())
    private val timer = MutableStateFlow(TimerState())

    private val events = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<UiEvent> = events.receiveAsFlow()

    private var timerJob: Job? = null
    private var weTurnedTorchOn = false

    private data class Core(
        val light: LightSettings,
        val prefs: AppPreferences,
        val presets: List<Preset>,
        val themes: List<ThemeItem>,
        val timer: TimerState,
    )

    val uiState: StateFlow<StudioUiState> =
        combine(
            combine(light, prefs, presetRepo.presets, themeRepo.customThemes, timer) { l, p, pr, th, t ->
                Core(l, p, pr, th, t)
            },
            torch.isTorchOn,
            billing.status,
            loaded,
        ) { core, torchOn, billingStatus, _ ->
            // The combines above only signal that something changed. In-memory state is read
            // directly so a snapshot can never pair loaded = true with not-yet-restored
            // preferences (the start screen is chosen from the first loaded snapshot).
            StudioUiState(
                loaded = loaded.value,
                light = light.value,
                prefs = prefs.value,
                presets = core.presets,
                customThemes = core.themes,
                timer = timer.value,
                torchAvailable = torch.isAvailable,
                torchOn = torchOn,
                billing = billingStatus,
                demoAllowed = demoAllowed,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, StudioUiState())

    init {
        viewModelScope.launch {
            // Restore the last session before showing anything.
            light.value = settingsRepo.light.first()
            prefs.value = settingsRepo.preferences.first()
            loaded.value = true

            launch { light.drop(1).collectLatest { delay(persistDelayMs); settingsRepo.updateLight { _ -> it } } }
            launch { prefs.drop(1).collectLatest { delay(persistDelayMs); settingsRepo.updatePreferences { _ -> it } } }
            launch { syncTorch() }
        }
        billing.connect()
    }

    private val isPremium: Boolean
        get() = billing.status.value.hasActiveSubscription || (demoAllowed && prefs.value.premiumPreview)

    private fun message(text: String) {
        events.trySend(UiEvent.Message(text))
    }

    /** For UI-only failures, such as no browser to open a link. */
    fun showMessage(text: String) = message(text)

    private fun gatePremium(feature: String) {
        events.trySend(UiEvent.OpenPremium)
        message("$feature is part of Premium")
    }

    // ---------------------------------------------------------------- light

    fun togglePower() {
        val turningOff = light.value.isOn
        if (turningOff && timer.value.isActive) {
            stopTimerJob()
            timer.update { TimerLogic.cancel(it) }
            light.update { it.copy(isOn = false) }
            message("Light off · timer cancelled")
            return
        }
        light.update(StudioLogic::togglePower)
    }

    fun setBrightness(v: Int) = light.update { StudioLogic.setBrightness(it, v) }
    fun setBeamWidth(v: Int) = light.update { StudioLogic.setBeamWidth(it, v) }
    fun setGlowRadius(v: Int) = light.update { StudioLogic.setGlowRadius(it, v) }
    fun setSpeed(tenths: Int) = light.update { StudioLogic.setSpeed(it, tenths) }

    fun selectMode(mode: LightMode) {
        val next = StudioLogic.selectMode(light.value, mode, isPremium)
        if (next == null) gatePremium(mode.displayName) else light.value = next
    }

    fun selectSwatch(swatch: Swatches) = light.update { StudioLogic.selectSwatch(it, swatch) }

    fun selectCustomColor(argb: Int) = light.update { StudioLogic.selectCustomColor(it, argb) }

    // ---------------------------------------------------------------- themes

    fun applyTheme(theme: ThemeItem) {
        light.update { StudioLogic.applyTheme(it, theme) }
        message("${theme.name} applied")
    }

    fun createTheme(rawName: String) {
        val customCount = uiState.value.customThemes.size
        if (!PremiumRules.canCreateCustomTheme(customCount, isPremium)) {
            gatePremium("More than one custom theme")
            return
        }
        val name = StudioLogic.cleanName(rawName, "My theme")
        viewModelScope.launch {
            val draft = StudioLogic.newCustomTheme(light.value, name)
            val id = themeRepo.insert(draft)
            light.update { StudioLogic.applyTheme(it, draft.copy(id = id)) }
            message("Theme “$name” saved")
        }
    }

    fun renameTheme(theme: ThemeItem, rawName: String) {
        if (!theme.isCustom) return
        val name = StudioLogic.cleanName(rawName, theme.name)
        viewModelScope.launch {
            themeRepo.rename(theme.id, name)
            if (light.value.themeId == theme.id) light.update { it.copy(colorName = name) }
        }
    }

    fun updateThemeToCurrentColor(theme: ThemeItem) {
        if (!theme.isCustom) return
        val accent = light.value.colorArgb
        val ground = ColorMath.groundFor(accent)
        viewModelScope.launch {
            themeRepo.updateColors(theme.id, accent, ground)
            light.update { it.copy(groundArgb = ground, colorName = theme.name, themeId = theme.id) }
            message("${theme.name} updated")
        }
    }

    fun deleteTheme(theme: ThemeItem) {
        if (!theme.isCustom) return
        viewModelScope.launch {
            themeRepo.delete(theme.id)
            light.update { StudioLogic.onThemeDeleted(it, theme.id) }
            message("Theme deleted")
        }
    }

    // ---------------------------------------------------------------- presets

    fun savePreset(rawName: String) {
        val count = uiState.value.presets.size
        if (!PremiumRules.canSavePreset(count, isPremium)) {
            gatePremium("More than ${PremiumRules.FREE_PRESET_LIMIT} presets")
            return
        }
        val name = StudioLogic.cleanName(rawName, StudioLogic.defaultPresetName(count))
        viewModelScope.launch {
            presetRepo.insert(StudioLogic.presetFrom(light.value, name))
            message("Saved “$name”")
        }
    }

    fun applyPreset(preset: Preset) {
        val next = StudioLogic.applyPreset(light.value, preset, isPremium)
        if (next == null) {
            gatePremium(preset.mode.displayName)
        } else {
            light.value = next
            message("“${preset.name}” applied")
        }
    }

    fun renamePreset(preset: Preset, rawName: String) {
        val name = StudioLogic.cleanName(rawName, preset.name)
        viewModelScope.launch { presetRepo.rename(preset.id, name) }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            presetRepo.delete(preset.id)
            message("“${preset.name}” deleted")
        }
    }

    // ---------------------------------------------------------------- timer

    fun selectTimerDuration(minutes: Int) = timer.update { TimerLogic.selectDuration(it, minutes) }

    fun startTimer() {
        timer.update { TimerLogic.start(it, clock()) }
        light.update { it.copy(isOn = true) }
        runTimerJob()
        message("Auto-off in ${timer.value.durationMinutes} min")
    }

    fun pauseTimer() {
        stopTimerJob()
        timer.update { TimerLogic.pause(it, clock()) }
    }

    fun resumeTimer() {
        timer.update { TimerLogic.resume(it, clock()) }
        light.update { it.copy(isOn = true) }
        runTimerJob()
    }

    fun cancelTimer() {
        stopTimerJob()
        timer.update { TimerLogic.cancel(it) }
        message("Timer cancelled")
    }

    /**
     * Ticks while the timer runs. Remaining time is derived from a monotonic deadline, so a slow
     * or skipped tick (e.g. while backgrounded) never makes the countdown drift.
     */
    private fun runTimerJob() {
        stopTimerJob()
        timerJob = viewModelScope.launch {
            while (isActive && timer.value.status == TimerStatus.RUNNING) {
                val result = TimerLogic.tick(timer.value, clock())
                timer.value = result.state
                if (result.expired) {
                    light.update { it.copy(isOn = false) }
                    message("Timer finished — light turned off")
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    private fun stopTimerJob() {
        timerJob?.cancel()
        timerJob = null
    }

    // ---------------------------------------------------------------- settings

    fun setReducedMotion(enabled: Boolean) = prefs.update { it.copy(reducedMotion = enabled) }

    fun setLedEnabled(enabled: Boolean) {
        if (enabled && !torch.isAvailable) {
            message("This phone has no flashlight LED")
            return
        }
        prefs.update { it.copy(ledEnabled = enabled) }
        message(if (enabled) "Phone LED will turn on with the light" else "Phone LED off")
    }

    fun setPremiumPreview(enabled: Boolean) {
        if (!demoAllowed) return
        prefs.update { it.copy(premiumPreview = enabled) }
        message(if (enabled) "Premium preview on (demo)" else "Premium preview off")
    }

    fun markIntroSeen() = prefs.update { it.copy(introSeen = true) }

    /** Restores the default look and comfort settings. Presets and themes are kept. */
    fun resetSettings() {
        light.value = StudioLogic.reset()
        prefs.update { it.copy(reducedMotion = false, ledEnabled = false) }
        message("Settings reset")
    }

    // ---------------------------------------------------------------- billing

    fun purchase(activity: Activity, plan: PlanType) {
        viewModelScope.launch {
            when (val outcome = billing.purchase(activity, plan)) {
                PurchaseOutcome.Launched -> Unit
                PurchaseOutcome.NotConfigured -> message(
                    if (demoAllowed) "Demo only — billing isn’t set up, nothing was charged" else BILLING_UNAVAILABLE
                )
                is PurchaseOutcome.Failed -> message(outcome.message)
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            when (val outcome = billing.restore()) {
                RestoreOutcome.Restored -> message("Premium restored")
                RestoreOutcome.NothingFound -> message("No purchases found")
                RestoreOutcome.NotConfigured -> message(
                    if (demoAllowed) "No purchases found (demo — billing isn’t set up)" else BILLING_UNAVAILABLE
                )
                is RestoreOutcome.Failed -> message(outcome.message)
            }
        }
    }

    // ---------------------------------------------------------------- hardware

    /** Keeps the camera LED in step with the light when the user opted in. */
    private suspend fun syncTorch() {
        combine(light, prefs) { l, p -> l.isOn && p.ledEnabled }
            .distinctUntilChanged()
            .collect { wantOn ->
                if (!torch.isAvailable) return@collect
                if (!wantOn && !weTurnedTorchOn) return@collect // never switch off a torch we didn't start
                torch.setTorch(wantOn)
                    .onSuccess { weTurnedTorchOn = wantOn }
                    .onFailure { e -> message(e.message ?: "Couldn't control the flashlight") }
            }
    }

    override fun onCleared() {
        // Leaving the app should not leave the LED burning.
        if (weTurnedTorchOn) torch.setTorch(false)
        super.onCleared()
    }

    companion object {
        private const val TICK_MS = 250L
        private const val BILLING_UNAVAILABLE = "Premium isn’t available right now — Google Play Billing couldn’t be reached"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as UvGlowApp
                val c = app.container
                StudioViewModel(
                    settingsRepo = c.settingsRepository,
                    presetRepo = c.presetRepository,
                    themeRepo = c.themeRepository,
                    torch = c.torchController,
                    billing = c.billingGateway,
                )
            }
        }
    }
}
