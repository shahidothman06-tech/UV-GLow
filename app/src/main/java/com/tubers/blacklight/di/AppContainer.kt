package com.tubers.blacklight.di

import android.content.Context
import com.tubers.blacklight.BuildConfig
import com.tubers.blacklight.billing.BillingGateway
import com.tubers.blacklight.billing.PlayBillingGateway
import com.tubers.blacklight.data.db.UvGlowDatabase
import com.tubers.blacklight.data.prefs.DataStoreSettingsRepository
import com.tubers.blacklight.data.repo.RoomPresetRepository
import com.tubers.blacklight.data.repo.RoomThemeRepository
import com.tubers.blacklight.domain.repository.PresetRepository
import com.tubers.blacklight.domain.repository.SettingsRepository
import com.tubers.blacklight.domain.repository.ThemeRepository
import com.tubers.blacklight.hardware.AndroidTorchController
import com.tubers.blacklight.hardware.TorchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container, created once by [com.tubers.blacklight.UvGlowApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Lives as long as the process; used for work that must outlive a screen. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val database: UvGlowDatabase by lazy { UvGlowDatabase.build(appContext) }

    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(appContext) }
    val presetRepository: PresetRepository by lazy { RoomPresetRepository(database.presetDao()) }
    val themeRepository: ThemeRepository by lazy { RoomThemeRepository(database.themeDao()) }
    val torchController: TorchController by lazy { AndroidTorchController(appContext) }
    val billingGateway: BillingGateway by lazy {
        PlayBillingGateway(appContext, BuildConfig.BILLING_MONTHLY_ID, BuildConfig.BILLING_YEARLY_ID, appScope)
    }
}
