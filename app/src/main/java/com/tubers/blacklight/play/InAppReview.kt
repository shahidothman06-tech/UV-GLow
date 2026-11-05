package com.tubers.blacklight.play

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Google Play in-app review card ("Rate UV Glow" with stars), shown when the person leaves the app.
 *
 * Play decides whether the card really shows (it has its own limit per person), and the app never
 * learns whether a rating was given. Google's rules say not to open it from a button, so the
 * Settings › Rate us row opens the Play Store page instead (see AppLinks.playStoreUrls).
 *
 * The card is asked for ahead of time with [prepare], so leaving the app is never slowed down by
 * a network call. On top of Play's limit, the app asks at most once every [MIN_DAYS_BETWEEN] days.
 */
class InAppReview(context: Context) {

    private val appContext = context.applicationContext
    private val manager = ReviewManagerFactory.create(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var ready: ReviewInfo? = null

    /** Fetches the review card in the background, if it's time to ask again. */
    fun prepare() {
        if (ready != null || !dueNow()) return
        runCatching {
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) ready = task.result
            }
        }
    }

    /**
     * Shows the review card (when one is ready), then runs [then] once it closes. Without a ready
     * card, [then] runs straight away.
     */
    fun showThen(activity: Activity, then: () -> Unit) {
        val info = ready
        if (info == null) {
            then()
            return
        }
        ready = null
        prefs.edit().putLong(KEY_LAST_ASKED, System.currentTimeMillis()).apply()
        val started = runCatching {
            manager.launchReviewFlow(activity, info).addOnCompleteListener { then() }
        }.isSuccess
        if (!started) then()
    }

    private fun dueNow(): Boolean {
        val last = prefs.getLong(KEY_LAST_ASKED, 0L)
        return System.currentTimeMillis() - last >= MIN_DAYS_BETWEEN * DAY_MS
    }

    private companion object {
        const val PREFS = "in_app_review"
        const val KEY_LAST_ASKED = "last_asked_at"
        const val MIN_DAYS_BETWEEN = 14L
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
