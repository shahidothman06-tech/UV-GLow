package com.tubers.blacklight.ui.state

import android.app.Activity
import com.tubers.blacklight.billing.BillingGateway
import com.tubers.blacklight.billing.BillingStatus
import com.tubers.blacklight.billing.PlanType
import com.tubers.blacklight.billing.PurchaseOutcome
import com.tubers.blacklight.billing.RestoreOutcome
import com.tubers.blacklight.domain.model.AppPreferences
import com.tubers.blacklight.domain.model.LightSettings
import com.tubers.blacklight.domain.model.Preset
import com.tubers.blacklight.domain.model.ThemeItem
import com.tubers.blacklight.domain.repository.PresetRepository
import com.tubers.blacklight.domain.repository.SettingsRepository
import com.tubers.blacklight.domain.repository.ThemeRepository
import com.tubers.blacklight.hardware.TorchController
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    light: LightSettings = LightSettings(),
    prefs: AppPreferences = AppPreferences(introSeen = true),
) : SettingsRepository {
    val lightFlow = MutableStateFlow(light)
    val prefsFlow = MutableStateFlow(prefs)
    override val light = lightFlow
    override val preferences = prefsFlow
    override suspend fun updateLight(transform: (LightSettings) -> LightSettings) { lightFlow.value = transform(lightFlow.value) }
    override suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences) { prefsFlow.value = transform(prefsFlow.value) }
}

class FakePresetRepository : PresetRepository {
    val list = MutableStateFlow<List<Preset>>(emptyList())
    private var nextId = 1L
    override val presets = list
    override suspend fun insert(preset: Preset): Long {
        val id = nextId++
        list.value = listOf(preset.copy(id = id)) + list.value
        return id
    }
    override suspend fun rename(id: Long, name: String) { list.value = list.value.map { if (it.id == id) it.copy(name = name) else it } }
    override suspend fun delete(id: Long) { list.value = list.value.filter { it.id != id } }
}

class FakeThemeRepository : ThemeRepository {
    val list = MutableStateFlow<List<ThemeItem>>(emptyList())
    private var nextId = 1L
    override val customThemes = list
    override suspend fun insert(theme: ThemeItem): String {
        val id = "custom-${nextId++}"
        list.value = list.value + theme.copy(id = id)
        return id
    }
    override suspend fun rename(themeId: String, name: String) { list.value = list.value.map { if (it.id == themeId) it.copy(name = name) else it } }
    override suspend fun updateColors(themeId: String, accentArgb: Int, groundArgb: Int) {
        list.value = list.value.map { if (it.id == themeId) it.copy(accentArgb = accentArgb, groundArgb = groundArgb) else it }
    }
    override suspend fun delete(themeId: String) { list.value = list.value.filter { it.id != themeId } }
}

class FakeTorch(override val isAvailable: Boolean = true) : TorchController {
    override val isTorchOn = MutableStateFlow(false)
    override fun setTorch(on: Boolean): Result<Unit> {
        isTorchOn.value = on
        return Result.success(Unit)
    }
}

class FakeBilling : BillingGateway {
    override val status = MutableStateFlow(BillingStatus())
    override fun connect() = Unit
    override suspend fun purchase(activity: Activity, plan: PlanType): PurchaseOutcome = PurchaseOutcome.NotConfigured
    override suspend fun restore(): RestoreOutcome = RestoreOutcome.NotConfigured
}
