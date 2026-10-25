package com.tubers.blacklight.domain.logic

import com.tubers.blacklight.domain.model.TimerState
import com.tubers.blacklight.domain.model.TimerStatus
import kotlin.math.max

/**
 * Pure state machine for the auto-off timer. Every function takes the current time explicitly
 * so it can be tested with a fake clock. Invalid transitions return the state unchanged.
 */
object TimerLogic {

    data class TickResult(val state: TimerState, val expired: Boolean)

    /** Choose a duration. Only allowed while idle. */
    fun selectDuration(state: TimerState, minutes: Int): TimerState {
        if (state.status != TimerStatus.IDLE || minutes <= 0) return state
        val ms = minutes * 60_000L
        return state.copy(durationMs = ms, remainingMs = ms)
    }

    fun start(state: TimerState, nowMs: Long): TimerState {
        if (state.status != TimerStatus.IDLE) return state
        return state.copy(
            status = TimerStatus.RUNNING,
            remainingMs = state.durationMs,
            deadlineMs = nowMs + state.durationMs,
        )
    }

    fun pause(state: TimerState, nowMs: Long): TimerState {
        val deadline = state.deadlineMs
        if (state.status != TimerStatus.RUNNING || deadline == null) return state
        return state.copy(
            status = TimerStatus.PAUSED,
            remainingMs = max(0L, deadline - nowMs),
            deadlineMs = null,
        )
    }

    fun resume(state: TimerState, nowMs: Long): TimerState {
        if (state.status != TimerStatus.PAUSED) return state
        return state.copy(status = TimerStatus.RUNNING, deadlineMs = nowMs + state.remainingMs)
    }

    fun cancel(state: TimerState): TimerState =
        state.copy(status = TimerStatus.IDLE, remainingMs = state.durationMs, deadlineMs = null)

    /**
     * Recompute remaining time. When the deadline has passed the timer resets to idle and
     * [TickResult.expired] is true exactly once, which is the signal to switch the light off.
     */
    fun tick(state: TimerState, nowMs: Long): TickResult {
        val deadline = state.deadlineMs
        if (state.status != TimerStatus.RUNNING || deadline == null) return TickResult(state, false)
        val left = deadline - nowMs
        return if (left <= 0L) {
            TickResult(cancel(state), expired = true)
        } else {
            TickResult(state.copy(remainingMs = left), expired = false)
        }
    }

    /** Fraction of time remaining, 1.0 when full. */
    fun fractionRemaining(state: TimerState): Float =
        if (state.durationMs <= 0L) 0f else (state.remainingMs.toFloat() / state.durationMs).coerceIn(0f, 1f)

    /** mm:ss, rounding seconds up so a fresh 5-minute timer shows 05:00, not 04:59. */
    fun format(ms: Long): String {
        val totalSec = ((max(0L, ms) + 999L) / 1000L)
        val m = totalSec / 60
        val s = totalSec % 60
        return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}
