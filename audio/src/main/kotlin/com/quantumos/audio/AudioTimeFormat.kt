package com.quantumos.audio

/*
 * Pure, unit-testable time-formatting helpers shared by the recorder timer and the player's
 * elapsed/duration readouts. No Android/Compose deps so it exercises with plain JUnit (same
 * "logic stays pure and tested" habit the rest of the codebase follows in :core), even though
 * this module otherwise has no emulator-free logic layer of its own.
 */
object AudioTimeFormat {
    /** Formats a millisecond duration as MM:SS, clamping negative input to 00:00. */
    fun millisToClock(ms: Long): String {
        val safeMs = if (ms < 0L) 0L else ms
        val totalSec = safeMs / 1000L
        val sec = totalSec % 60L
        val min = totalSec / 60L
        return "%02d:%02d".format(min, sec)
    }

    /** Formats a tenths-of-a-second recording counter (100ms poll ticks) as MM:SS. */
    fun tenthsToClock(tenths: Int): String {
        val safeTenths = if (tenths < 0) 0 else tenths
        val totalSec = safeTenths / 10
        val sec = totalSec % 60
        val min = totalSec / 60
        return "%02d:%02d".format(min, sec)
    }
}
