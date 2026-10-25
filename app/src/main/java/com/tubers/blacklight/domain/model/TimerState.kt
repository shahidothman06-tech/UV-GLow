package com.tubers.blacklight.domain.model

enum class TimerStatus { IDLE, RUNNING, PAUSED }

/**
 * Auto-off timer state. While RUNNING the source of truth is [deadlineMs] (a monotonic clock
 * value), so the countdown stays correct across recompositions, backgrounding and missed ticks.
 */
data class TimerState(
    val durationMs: Long = 5 * 60_000L,
    val remainingMs: Long = durationMs,
    val status: TimerStatus = TimerStatus.IDLE,
    val deadlineMs: Long? = null,
) {
    val isActive: Boolean get() = status != TimerStatus.IDLE
    val durationMinutes: Int get() = (durationMs / 60_000L).toInt()

    companion object {
        val DURATION_CHOICES_MIN: List<Int> = listOf(1, 5, 10, 30)
    }
}
