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
 * a network call. On top of Play's limit, the app waits [FIRST_ASK_AFTER_DAYS] days after it is
 * first used before the first ask (so new users aren't asked before they've tried the app), then
 * asks at most once every [MIN_DAYS_BETWEEN] days.
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
        val now = System.currentTimeMillis()
        var firstSeen = prefs.getLong(KEY_FIRST_SEEN, 0L)
        if (firstSeen == 0L) {
            firstSeen = now
            prefs.edit().putLong(KEY_FIRST_SEEN, now).apply()
        }
        return isDue(now, firstSeen, prefs.getLong(KEY_LAST_ASKED, 0L))
    }

    companion object {
        private const val PREFS = "in_app_review"
        private const val KEY_FIRST_SEEN = "first_seen_at"
        private const val KEY_LAST_ASKED = "last_asked_at"
        const val FIRST_ASK_AFTER_DAYS = 3L
        const val MIN_DAYS_BETWEEN = 14L
        const val DAY_MS = 24L * 60 * 60 * 1000

        /**
         * True when it's time to ask: at least [FIRST_ASK_AFTER_DAYS] days since the app was first
         * used, and at least [MIN_DAYS_BETWEEN] days since the last ask (0 = never asked).
         */
        fun isDue(now: Long, firstSeenAt: Long, lastAskedAt: Long): Boolean =
            now - firstSeenAt >= FIRST_ASK_AFTER_DAYS * DAY_MS &&
                (lastAskedAt == 0L || now - lastAskedAt >= MIN_DAYS_BETWEEN * DAY_MS)
    }
}
