package com.quantumos.appshell

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Put an Activity into the Field Unit's display mode: edge to edge, **and with the system bars
 * hidden**.
 *
 * Found on the Fold 6, sideloaded (the first real-hardware pass this project has had). Every one of
 * the eleven activities called `enableEdgeToEdge()` and nothing anywhere ever hid the system bars.
 * `enableEdgeToEdge()` only says "draw BEHIND the bars" -- it does not remove them. So Android's own
 * clock, signal and battery sat on top of the CRT surface on every single screen, launcher and QUARK
 * included.
 *
 * That is not a cosmetic problem for this product. QuantumOS is a locked shell that reads as a
 * device rather than an app: the house style forbids a drawn bezel precisely so nothing frames the
 * phosphor, and the launcher takes the HOME intent so it IS the surface. A second OS's status bar
 * across the top contradicts the whole premise, and it also overlaps the App Shell's own nameplate
 * header, which carries the same information the Operator actually needs.
 *
 * **Transient rather than sticky-hidden.** `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` brings the bars
 * back for a few seconds on an edge swipe and then hides them again. A field tool must not trap its
 * Operator away from the system -- and the Fold 6 is also the Director's daily phone, which is the
 * same reasoning behind the M1 rollback rule.
 *
 * `enableEdgeToEdge()` and not `setDecorFitsSystemWindows`: CLAUDE.md pins that call specifically,
 * and we own inset handling ourselves.
 */
fun ComponentActivity.engageFieldUnitDisplay() {
    enableEdgeToEdge()
    hideSystemBars()
}

/**
 * Re-hide the bars.
 *
 * Needed because a transient reveal, a fold/unfold, or returning from another app can leave them
 * showing. Call from `onWindowFocusChanged` when focus is regained -- on a foldable this happens on
 * every posture change, which is exactly when it is needed.
 */
fun Activity.hideSystemBars() {
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
}
