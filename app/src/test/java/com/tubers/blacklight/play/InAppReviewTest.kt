package com.tubers.blacklight.play

import com.tubers.blacklight.play.InAppReview.Companion.DAY_MS
import com.tubers.blacklight.play.InAppReview.Companion.isDue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewTest {

    private val start = 1_000_000_000_000L

    @Test fun newUser_isNotAskedOnFirstExit() {
        assertFalse(isDue(now = start + 60_000, firstSeenAt = start, lastAskedAt = 0L))
    }

    @Test fun newUser_isNotAskedBeforeThreeDays() {
        assertFalse(isDue(now = start + 3 * DAY_MS - 1, firstSeenAt = start, lastAskedAt = 0L))
    }

    @Test fun neverAsked_isAskedAfterThreeDays() {
        assertTrue(isDue(now = start + 3 * DAY_MS, firstSeenAt = start, lastAskedAt = 0L))
    }

    @Test fun askedRecently_waitsFourteenDays() {
        val asked = start + 5 * DAY_MS
        assertFalse(isDue(now = asked + 14 * DAY_MS - 1, firstSeenAt = start, lastAskedAt = asked))
        assertTrue(isDue(now = asked + 14 * DAY_MS, firstSeenAt = start, lastAskedAt = asked))
    }
}
