package com.quantumos.core

import kotlinx.coroutines.launch
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

    // ---------- M4 floating-trigger placement (pure geometry) ----------

    @Test
    fun nearestEdge_snapsLeftWhenCentreOnLeftHalf() {
        // 52px view near the left: centre at 30 < 540 → snap to x=0.
        assertEquals(0, OverlayGeometry.nearestEdgeX(currentX = 4, viewSize = 52, screenWidth = 1080))
    }

    @Test
    fun nearestEdge_snapsRightWhenCentreOnRightHalf() {
        // View near the right: snap flush to the right edge (screenWidth - viewSize).
        assertEquals(1028, OverlayGeometry.nearestEdgeX(currentX = 900, viewSize = 52, screenWidth = 1080))
    }

    @Test
    fun defaultPark_isRightEdgeAndVerticallyCentred() {
        val (x, y) = OverlayGeometry.defaultPark(viewSize = 52, screenWidth = 1080, screenHeight = 2400)
        assertEquals(1028, x)                 // flush right
        assertEquals((2400 - 52) / 2, y)      // mid-height
    }

    @Test
    fun readinessPercent_isFullWhenAllNominal_andLowWhenDrained() {
        val full = VitalityState(batteryPercentage = 100, connectivityStrength = 4, coreTempCelsius = 25f)
        assertEquals(100, full.readinessPercent)
        val drained = VitalityState(batteryPercentage = 0, connectivityStrength = 0, coreTempCelsius = 50f)
        assertEquals(0, drained.readinessPercent)
    }

    // ---------- M5 QUARK scripted brain ----------

    // Step 0 verify: the same intent fired twice must NOT repeat the same variant in a session.
    @Test
    fun saySomething_doesNotRepeatVariantBackToBack() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        parser.railSaySomething(); advanceUntilIdle()
        parser.railSaySomething(); advanceUntilIdle()
        val log = engine.conversationLog.value
        assertEquals(2, log.size)
        assertTrue(log[0].line != log[1].line, "two consecutive variants must differ")
    }

    // Step 0 verify: a rail action reuses the existing engine function AND logs a line.
    @Test
    fun railEngageStealth_reusesEngineToggle_andLogsLine() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        assertFalse(engine.masterState.value.environment.isStealthMode)
        parser.railEngageStealth(); advanceUntilIdle()
        assertTrue(engine.masterState.value.environment.isStealthMode) // real M3 action ran
        assertEquals(1, engine.conversationLog.value.size)             // and it logged
    }

    // Step 4 verify: Trigger-warn fires the REAL Warn state with the drill line.
    @Test
    fun railTriggerWarn_firesWarnPosture() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        parser.railTriggerWarn(); advanceUntilIdle()
        assertEquals(QuarkReflexPosture.WARN, engine.masterState.value.quarkBrain.activePosture)
    }

    // Step 1 verify (harbor): everyday low mood stays Idle, NO sound, NO crisis resource.
    @Test
    fun harbor_isIdle_noSound_noCrisisResource() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        val cues = mutableListOf<String>()
        val job = launch { engine.audioCueStream.collect { cues.add(it) } }
        parser.parseInput("I'm having a really rough day and feeling low")
        advanceUntilIdle()
        val brain = engine.masterState.value.quarkBrain
        assertEquals(QuarkReflexPosture.IDLE, brain.activePosture)
        assertFalse(brain.showCrisisResource)
        assertTrue(cues.isEmpty(), "harbor must not emit any sound cue")
        job.cancel()
    }

    // Step 1 verify (crisis): genuine danger-to-self stays Idle (NOT Warn), NO sound, flags resource.
    @Test
    fun distress_isIdle_notWarn_noSound_butFlagsResource() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        val cues = mutableListOf<String>()
        val job = launch { engine.audioCueStream.collect { cues.add(it) } }
        parser.parseInput("I don't want to be here anymore, I want to die")
        advanceUntilIdle()
        val brain = engine.masterState.value.quarkBrain
        assertEquals(QuarkReflexPosture.IDLE, brain.activePosture) // never Warn for crisis
        assertTrue(brain.showCrisisResource)                       // resource line renders beneath
        assertTrue(cues.isEmpty(), "crisis must not emit any sound cue")
        job.cancel()
    }

    // Crisis resource (Deployment Region patch): default region JAPAN → the Japan preset block (never
    // the generic fallback now); switching region swaps the block; an explicit override still wins.
    @Test
    fun crisisResource_resolvesActiveRegionBlock_andOverrideWins() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        assertEquals(DeploymentRegions.JAPAN, engine.effectiveCrisisResource())
        engine.cycleDeploymentRegion()
        assertEquals(DeploymentRegions.HONG_KONG, engine.effectiveCrisisResource())
        engine.setCrisisResourceLine("Local line 123")
        assertEquals("Local line 123", engine.effectiveCrisisResource())
    }

    // Step 5 verify: nonsense falls through to a graceful Fallback, never silence/error.
    @Test
    fun unmatchedInput_fallsThroughToFallback() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        parser.parseInput("qwerty zxcvb asdf"); advanceUntilIdle()
        assertEquals("FALLBACK", engine.masterState.value.quarkBrain.matchedIntent)
        assertTrue(engine.conversationLog.value.single().line.isNotBlank())
    }

    // The crisis tier must win over any other keyword in the same message (priority override).
    @Test
    fun distress_winsOverOtherKeywordsInSameMessage() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        parser.parseInput("check my battery but honestly I want to kill myself")
        advanceUntilIdle()
        assertEquals("DISTRESS", engine.masterState.value.quarkBrain.matchedIntent)
    }

    // ---------- M6 settings + boot/region lines ----------

    // Ship default is DELIBERATE (the old hardcoded SNAPPY was dev-only).
    @Test
    fun bootPace_defaultsToDeliberate_andCyclesToggle() = runTest {
        val engine = QuantumStateEngine(this)   // no pace arg → ship default
        assertEquals(BootPace.DELIBERATE, engine.masterState.value.bootPace)
        engine.cycleBootPace()
        assertEquals(BootPace.SNAPPY, engine.masterState.value.bootPace)
        engine.cycleBootPace()
        assertEquals(BootPace.DELIBERATE, engine.masterState.value.bootPace)
    }

    // setBootPace before boot actually changes the boot duration (pace is read at sequence start).
    @Test
    fun setBootPace_appliesToColdBoot() = runTest {
        val engine = QuantumStateEngine(this, BootPace.DELIBERATE)
        engine.setBootPace(BootPace.SNAPPY)
        engine.executeColdBootSequence()
        advanceUntilIdle()
        assertEquals(BootLifecycleState.ACTIVE, engine.masterState.value.bootLifecycle)
    }

    // Deployment Region: default JAPAN, cycles JAPAN → HONG_KONG → JAPAN.
    @Test
    fun deploymentRegion_defaultsJapan_andCycles() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        assertEquals(DeploymentRegion.JAPAN, engine.masterState.value.deploymentRegion)
        engine.cycleDeploymentRegion()
        assertEquals(DeploymentRegion.HONG_KONG, engine.masterState.value.deploymentRegion)
        engine.cycleDeploymentRegion()
        assertEquals(DeploymentRegion.JAPAN, engine.masterState.value.deploymentRegion)
    }

    // §6 online line: spoken once, Happy posture, logs to the conversation log with live data.
    @Test
    fun speakOnline_firesHappyOnlineLine() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        parser.speakOnline(); advanceUntilIdle()
        assertEquals("ONLINE", engine.masterState.value.quarkBrain.matchedIntent)
        assertEquals(QuarkReflexPosture.HAPPY, engine.masterState.value.quarkBrain.activePosture)
        assertEquals(1, engine.conversationLog.value.size)
    }

    // Region ack: switching to Hong Kong speaks the Hong Kong variant and logs it.
    @Test
    fun speakRegionSwitched_firesRegionAck_andLogs() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        engine.cycleDeploymentRegion() // → HONG_KONG
        parser.speakRegionSwitched(engine.masterState.value.deploymentRegion)
        advanceUntilIdle()
        assertEquals("REGION", engine.masterState.value.quarkBrain.matchedIntent)
        val line = engine.conversationLog.value.single().line
        assertTrue(line.contains("Hong Kong"), "region ack must name the new region")
    }

    // ---------- Launcher Restructure Phase 1 — HOME instrument console ----------

    // The console is fixed at exactly eight instruments, per the House Style module identities.
    @Test
    fun instrumentConsole_hasExactlyEightUniqueInstruments() {
        val specs = InstrumentConsole.INSTRUMENTS
        assertEquals(8, specs.size)
        assertEquals(InstrumentId.entries.toSet(), specs.map { it.id }.toSet())
    }

    // CAM/MAPS dock into :optics/:nav (App Shell Integration, Phase 3); COMMS/FILES/AUDIO/RADIO
    // dock into :comms/:files/:audio/:radio (Core Apps Fix-Pass, Decision 86); SIGNAL/CONFIG dock
    // into :signal/:config (SIGNAL + CONFIG Task Brief) — closing the eight core instruments.
    @Test
    fun instrumentConsole_allEightInstrumentsDock() {
        val specs = InstrumentConsole.INSTRUMENTS
        assertEquals(DockedModule.OPTICS, specs.single { it.id == InstrumentId.CAM }.dockedModule)
        assertEquals(DockedModule.NAV, specs.single { it.id == InstrumentId.MAPS }.dockedModule)
        assertEquals(DockedModule.COMMS, specs.single { it.id == InstrumentId.COMMS }.dockedModule)
        assertEquals(DockedModule.FILES, specs.single { it.id == InstrumentId.FILES }.dockedModule)
        assertEquals(DockedModule.AUDIO, specs.single { it.id == InstrumentId.AUDIO }.dockedModule)
        assertEquals(DockedModule.RADIO, specs.single { it.id == InstrumentId.RADIO }.dockedModule)
        assertEquals(DockedModule.SIGNAL, specs.single { it.id == InstrumentId.SIGNAL }.dockedModule)
        assertEquals(DockedModule.CONFIG, specs.single { it.id == InstrumentId.CONFIG }.dockedModule)
        val standby = specs.filter { it.dockedModule == null && it.opensChannel == null }
        assertEquals(emptySet<InstrumentId>(), standby.map { it.id }.toSet())
    }

    // ---------- Launcher Restructure Phase 2 (v5) — APPS pager ----------

    @Test
    fun clampPage_neverWraps_andCollapsesToZeroForOneOrFewerPages() {
        assertEquals(0, ReelPager.clampPage(-3, pageCount = 5))
        assertEquals(4, ReelPager.clampPage(99, pageCount = 5))
        assertEquals(2, ReelPager.clampPage(2, pageCount = 5))
        assertEquals(0, ReelPager.clampPage(2, pageCount = 1))
        assertEquals(0, ReelPager.clampPage(2, pageCount = 0))
    }

    // The conversation log is its OWN list — distinct from the M2 systemLogs console.
    @Test
    fun conversationLog_isDistinctFromSystemLogs() = runTest {
        val engine = QuantumStateEngine(this, BootPace.SNAPPY)
        val parser = QuarkParser(engine)
        parser.parseInput("hello quark"); advanceUntilIdle()
        assertEquals(1, engine.conversationLog.value.size)
        // systemLogs also gets an audit line, but the two flows are separate objects/contents.
        assertTrue(engine.conversationLog.value.first().line.isNotBlank())
        assertTrue(engine.systemLogs.value.none { it == engine.conversationLog.value.first().line })
    }
}
