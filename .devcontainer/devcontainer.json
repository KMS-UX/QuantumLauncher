package com.quantumos.core

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/*
 * Seed tests for the pure-logic core (runbook Step 4) — NO emulator required.
 * Requires test deps: org.jetbrains.kotlinx:kotlinx-coroutines-test, kotlin("test").
 * These exercise the highest-confidence part of the spike.
 */
class QuantumStateEngineTest {

    @Test
    fun coldBoot_reachesActive_and_runsOnce() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        engine.executeColdBootSequence()
        advanceUntilIdle()
        assertEquals(BootLifecycleState.ACTIVE, engine.masterState.value.bootLifecycle)

        // Second call must be ignored (cold-boot-only guard, Bible decision 59).
        engine.executeColdBootSequence()
        advanceUntilIdle()
        assertEquals(BootLifecycleState.ACTIVE, engine.masterState.value.bootLifecycle)
    }

    @Test
    fun hueCommand_switchesPhosphor() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        parser.parseInput("hue.amber")
        advanceUntilIdle()
        assertEquals(PhosphorHue.AMBER, engine.masterState.value.environment.activeHue)
    }

    @Test
    fun lockAndUnlock_togglesSealedState() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        engine.executeColdBootSequence()
        advanceUntilIdle()

        parser.parseInput("sys.lock")
        advanceUntilIdle()
        assertEquals(BootLifecycleState.DEVICE_SECURED, engine.masterState.value.bootLifecycle)
        assertTrue(engine.masterState.value.environment.isSystemLocked)

        parser.parseInput("sys.unlock")
        advanceUntilIdle()
        assertFalse(engine.masterState.value.environment.isSystemLocked)
        assertEquals(BootLifecycleState.ACTIVE, engine.masterState.value.bootLifecycle)
    }

    @Test
    fun telemetry_setsCriticalReadiness_onHighTemp() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        engine.incomingTelemetryUpdate(bat = 80, chg = false, upMs = 0L, con = 4, temp = 46.0f)
        advanceUntilIdle()
        assertEquals(SystemReadiness.CRITICAL, engine.masterState.value.vitality.readiness)
    }
}
