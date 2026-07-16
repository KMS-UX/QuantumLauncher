package com.quantumos.quarkbrain

import android.content.Context
import com.quantumos.core.AiAssistBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/*
 * QuarkBrainProvider — the process-wide home of the ONE [QuarkOnDeviceBrain] (QUARK Brain Promotion
 * Task Brief). :app (QuantumRuntime / the Assistant View), :files (FileExplorerViewModel's
 * AiAssistBridge default), and any future docked module all resolve the same instance here instead
 * of each holding a private copy — same "one source of truth, fix once in the shared module" pattern
 * as QuantumRuntime's own engine singleton. Owns its own application-scoped CoroutineScope so a
 * download/import in flight survives navigating away from whichever screen started it.
 */
object QuarkBrainProvider {
    private val brainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile private var brain: QuarkOnDeviceBrain? = null
    @Volatile private var bridge: AiAssistBridge? = null

    fun onDeviceBrain(context: Context): QuarkOnDeviceBrain =
        brain ?: synchronized(this) {
            brain ?: QuarkOnDeviceBrain(context.applicationContext, brainScope).also { brain = it }
        }

    fun bridge(context: Context): AiAssistBridge =
        bridge ?: synchronized(this) {
            bridge ?: QuarkAiAssistBridge(onDeviceBrain(context)).also { bridge = it }
        }
}
