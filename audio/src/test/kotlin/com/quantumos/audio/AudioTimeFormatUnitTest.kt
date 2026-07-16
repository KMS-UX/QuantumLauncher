package com.quantumos.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioTimeFormatUnitTest {

    @Test
    fun millisToClock_formatsZero() {
        assertEquals("00:00", AudioTimeFormat.millisToClock(0L))
    }

    @Test
    fun millisToClock_formatsUnderAMinute() {
        assertEquals("00:08", AudioTimeFormat.millisToClock(8_000L))
    }

    @Test
    fun millisToClock_formatsMinutesAndSeconds() {
        assertEquals("02:05", AudioTimeFormat.millisToClock(125_000L))
    }

    @Test
    fun millisToClock_clampsNegative() {
        assertEquals("00:00", AudioTimeFormat.millisToClock(-500L))
    }

    @Test
    fun tenthsToClock_formatsRecorderPollTicks() {
        // 100ms poll ticks: 123 ticks = 12.3s -> 00:12
        assertEquals("00:12", AudioTimeFormat.tenthsToClock(123))
    }

    @Test
    fun tenthsToClock_formatsPastAMinute() {
        // 605 ticks = 60.5s -> 01:00
        assertEquals("01:00", AudioTimeFormat.tenthsToClock(605))
    }
}
