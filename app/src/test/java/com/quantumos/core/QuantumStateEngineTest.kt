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

    // ---------- M3 Vitality-panel actions ----------

    @Test
    fun phosphorAction_cyclesHue_greenAmberCyanGreen() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        assertEquals(PhosphorHue.GREEN, engine.masterState.value.environment.activeHue)
        engine.cyclePhosphorHue()
        assertEquals(PhosphorHue.AMBER, engine.masterState.value.environment.activeHue)
        engine.cyclePhosphorHue()
        assertEquals(PhosphorHue.CYAN, engine.masterState.value.environment.activeHue)
        engine.cyclePhosphorHue()
        assertEquals(PhosphorHue.GREEN, engine.masterState.value.environment.activeHue)
    }

    @Test
    fun stealthAction_togglesReversibly() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        assertFalse(engine.masterState.value.environment.isStealthMode)
        engine.toggleStealthMode()
        assertTrue(engine.masterState.value.environment.isStealthMode)
        engine.toggleStealthMode()
        assertFalse(engine.masterState.value.environment.isStealthMode)
    }

    @Test
    fun beaconOn_forceDropsStealth_signallingTakesPriority() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        engine.toggleStealthMode()
        assertTrue(engine.masterState.value.environment.isStealthMode)

        engine.toggleBeacon() // turning Beacon ON must auto-release Stealth
        assertTrue(engine.masterState.value.environment.isBeaconActive)
        assertFalse(engine.masterState.value.environment.isStealthMode)
    }

    @Test
    fun beaconOff_leavesStealthUntouched() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        engine.toggleBeacon()                 // on
        engine.toggleStealthMode()            // stealth on after beacon (allowed)
        engine.toggleBeacon()                 // off — must not flip stealth
        assertFalse(engine.masterState.value.environment.isBeaconActive)
        assertTrue(engine.masterState.value.environment.isStealthMode)
    }

    @Test
    fun readinessPercent_isFullWhenAllNominal_andLowWhenDrained() {
        val full = VitalityState(batteryPercentage = 100, connectivityStrength = 4, coreTempCelsius = 25f)
        assertEquals(100, full.readinessPercent)
        val drained = VitalityState(batteryPercentage = 0, connectivityStrength = 0, coreTempCelsius = 50f)
        assertEquals(0, drained.readinessPercent)
    }
}
