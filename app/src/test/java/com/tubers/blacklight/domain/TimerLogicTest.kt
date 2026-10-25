package com.tubers.blacklight.domain

import com.tubers.blacklight.domain.logic.TimerLogic
import com.tubers.blacklight.domain.model.TimerState
import com.tubers.blacklight.domain.model.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerLogicTest {

    private val minute = 60_000L

    @Test
    fun selectDuration_onlyWhileIdle() {
        val idle = TimerLogic.selectDuration(TimerState(), 10)
        assertEquals(10 * minute, idle.durationMs)
        assertEquals(10 * minute, idle.remainingMs)

        val running = TimerLogic.start(idle, nowMs = 0)
        val unchanged = TimerLogic.selectDuration(running, 1)
        assertEquals(running, unchanged)
    }

    @Test
    fun start_setsDeadlineFromNow() {
        val s = TimerLogic.start(TimerLogic.selectDuration(TimerState(), 1), nowMs = 5_000)
        assertEquals(TimerStatus.RUNNING, s.status)
        assertEquals(5_000 + minute, s.deadlineMs)
        assertEquals(minute, s.remainingMs)
    }

    @Test
    fun tick_countsDownFromDeadline_evenWithIrregularTicks() {
        var s = TimerLogic.start(TimerLogic.selectDuration(TimerState(), 1), nowMs = 0)
        s = TimerLogic.tick(s, 250).state
        assertEquals(minute - 250, s.remainingMs)
        // A long gap (app in background) is handled because time is derived from the deadline.
        val r = TimerLogic.tick(s, 45_000)
        assertFalse(r.expired)
        assertEquals(15_000, r.state.remainingMs)
    }

    @Test
    fun tick_expiresExactlyOnce_andResetsToIdle() {
        val s = TimerLogic.start(TimerLogic.selectDuration(TimerState(), 1), nowMs = 0)
        val r = TimerLogic.tick(s, minute + 10)
        assertTrue(r.expired)
        assertEquals(TimerStatus.IDLE, r.state.status)
        assertEquals(minute, r.state.remainingMs)
        assertNull(r.state.deadlineMs)
        // Further ticks on an idle timer never report expiry again.
        assertFalse(TimerLogic.tick(r.state, minute * 2).expired)
    }

    @Test
    fun pauseAndResume_preserveRemainingTime() {
        var s = TimerLogic.start(TimerLogic.selectDuration(TimerState(), 5), nowMs = 0)
        s = TimerLogic.pause(s, nowMs = 2 * minute)
        assertEquals(TimerStatus.PAUSED, s.status)
        assertEquals(3 * minute, s.remainingMs)
        assertNull(s.deadlineMs)

        // Time passing while paused doesn't count.
        assertFalse(TimerLogic.tick(s, 100 * minute).expired)

        s = TimerLogic.resume(s, nowMs = 10 * minute)
        assertEquals(TimerStatus.RUNNING, s.status)
        assertEquals(13 * minute, s.deadlineMs)
        assertTrue(TimerLogic.tick(s, 13 * minute).expired)
    }

    @Test
    fun cancel_returnsToFullDuration() {
        var s = TimerLogic.start(TimerLogic.selectDuration(TimerState(), 10), nowMs = 0)
        s = TimerLogic.tick(s, 3 * minute).state
        s = TimerLogic.cancel(s)
        assertEquals(TimerStatus.IDLE, s.status)
        assertEquals(10 * minute, s.remainingMs)
        assertFalse(s.isActive)
    }

    @Test
    fun invalidTransitions_areIgnored() {
        val idle = TimerState()
        assertEquals(idle, TimerLogic.pause(idle, 0))
        assertEquals(idle, TimerLogic.resume(idle, 0))
        val running = TimerLogic.start(idle, 0)
        assertEquals(running, TimerLogic.start(running, 1_000))
    }

    @Test
    fun format_roundsUpToWholeSeconds() {
        assertEquals("05:00", TimerLogic.format(5 * minute))
        assertEquals("04:59", TimerLogic.format(5 * minute - 1_000))
        assertEquals("00:01", TimerLogic.format(1))
        assertEquals("00:00", TimerLogic.format(0))
        assertEquals("30:00", TimerLogic.format(30 * minute))
    }

    @Test
    fun fractionRemaining_isClamped() {
        val s = TimerLogic.start(TimerLogic.selectDuration(TimerState(), 1), 0)
        assertEquals(1f, TimerLogic.fractionRemaining(s), 0.0001f)
        assertEquals(0.5f, TimerLogic.fractionRemaining(TimerLogic.tick(s, 30_000).state), 0.0001f)
    }
}
