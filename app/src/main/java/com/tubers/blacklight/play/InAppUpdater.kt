package com.tubers.blacklight.play

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play in-app updates, IMMEDIATE type: when Play has a newer version, a full-screen Play
 * screen downloads and installs it, then the app restarts on the new version.
 *
 * Only works for installs that came from Google Play (internal testing counts). Debug or sideloaded
 * builds get an error from Play, which is ignored, so nothing shows.
 *
 * Create it in onCreate (it registers an activity-result launcher, which must happen before onStart).
 */
class InAppUpdater(activity: ComponentActivity) {

    private val manager = AppUpdateManagerFactory.create(activity)

    // If the person cancels, the app keeps running; Play offers the update again on the next launch.
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { }

    /** Call once per launch: starts the update if Play has a newer version. */
    fun checkForUpdate() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                start(info)
            }
        }
    }

    /** Call from onResume: brings the update screen back if an update was left half-way. */
    fun resumeIfInProgress() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                start(info)
            }
        }
    }

    private fun start(info: AppUpdateInfo) {
        // Never let an update problem crash the app.
        runCatching {
            manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
            )
        }
    }
}
