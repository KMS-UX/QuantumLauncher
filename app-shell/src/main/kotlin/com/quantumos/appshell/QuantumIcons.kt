package com.quantumos.appshell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * QuantumIcons — the ONE house line-icon set (Icon Direction, decision 60; Core Apps Polish Pass
 * §2). Original SVG-in-spirit line-icons, drawn on the GPU-cheap Canvas/Path route (same discipline
 * as the launcher's pre-existing InstrumentIcon working set), themed by whatever tint color the
 * caller passes in (always Phosphor.bright/dim(activeHue) at the call site — never hardcoded here).
 * Lives in :app-shell so every docked module reaches the exact same icon, "build once, ship
 * everywhere," same pattern as Phosphor/Fonts — not redrawn per app.
 *
 * Consistent stroke weight (proportional to size, ~9% of the box, matching InstrumentIcon), no fill
 * unless the glyph specifically needs a solid dot/mark. No platform emoji, no Material glyphs.
 *
 * Pixel-level icon masters + the Atom-Lockup app badge are explicitly a later identity pass (brief
 * §2) — this is the working icon set until then.
 */
private const val PI_F = PI.toFloat()

enum class Glyph {
    // shared shell chrome
    Back, ChannelHome, ChannelApps, ChannelStatus, ChannelLog,
    // Vitality panel / QUARK command rail (shared vocabulary — same glyph, same meaning everywhere)
    Stealth, Phosphor, Beacon, Lock, StatusReport, Say, Warn,
    // COMMS
    CommsChannel,
    // FILES
    Folder, FileDoc, Terminal, Decrypt, QuarkChat, Send,
    CategoryFieldLogs, CategoryCaptures, CategoryCommsCache, CategoryMaps,
    // AUDIO
    Record, Play, Pause, Stop, Mic,
    // CAM
    // ModeToggle removed: the Director closed PHOTO/VIDEO as a decision, not a gap -- a
    // retro-futuristic film camera is a photographic instrument, so there is no mode to toggle and
    // the glyph was dead vocabulary. Optics is photo-only by design.
    Shutter,
    // CAM -- focus scale + capture state (Glyph Sweep). These replace colour emoji that were
    // rendering as vendor artwork inside a phosphor viewfinder.
    FocusMacro, FocusPortrait, FocusMid, FocusLandscape, Infinity, Tilt, Develop,
    // MAPS
    Waypoint, YouMarker,
    // RADIO
    TunerDial, Preset, Close,
    // SIGNAL
    Cellular, Wifi, Gps, Bluetooth, RunScan,
    // CONFIG
    BootPace, Region,
    // Console marks (Glyph Sweep). Every one of these replaced a bare Unicode character sitting in
    // a Text(): monochrome and reasonable-looking, but resolved by FONT FALLBACK, so what actually
    // rendered was whatever glyph the device happened to substitute -- not house art, not
    // guaranteed present, and not measurable. Drawn, they are ours and they retint with the hue.
    Forward, TriangleLeft, TriangleRight, CaretUp, CaretDown,
    Dot, Diamond, Crosshair, Swap, Cycle, Atom, Charge
}

/*
 * One flexible icon composable — pass the [Glyph] and a tint; every call site supplies its own
 * active-hue color so recoloring lives entirely in Phosphor.bright/dim(), never here (Item 2's
 * live-hue-sync rule extends to icons too — nothing paints an off-palette or hardcoded color).
 */
@Composable
fun QuantumIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        val thinStroke = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
        fun pt(fx: Float, fy: Float) = Offset(w * fx, h * fy)

        when (glyph) {
            Glyph.Back -> {
                val p = Path().apply {
                    moveTo(w * 0.62f, h * 0.18f)
                    lineTo(w * 0.28f, h * 0.5f)
                    lineTo(w * 0.62f, h * 0.82f)
                }
                drawPath(p, tint, style = stroke)
            }

            Glyph.ChannelHome -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    lineTo(w * 0.85f, h * 0.45f)
                    lineTo(w * 0.85f, h * 0.85f)
                    lineTo(w * 0.15f, h * 0.85f)
                    lineTo(w * 0.15f, h * 0.45f)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }

            Glyph.ChannelApps -> {
                val cell = w * 0.28f
                val gap = w * 0.14f
                val origin = Offset(w * 0.5f - cell - gap / 2f, h * 0.5f - cell - gap / 2f)
                listOf(0, 1).forEach { row ->
                    listOf(0, 1).forEach { col ->
                        drawRect(
                            tint,
                            topLeft = origin + Offset(col * (cell + gap), row * (cell + gap)),
                            size = Size(cell, cell),
                            style = stroke
                        )
                    }
                }
            }

            Glyph.ChannelStatus -> {
                val r = w * 0.34f
                val c = pt(0.5f, 0.5f)
                drawCircle(tint, radius = r, center = c, style = stroke)
                drawLine(tint, c, c + Offset(0f, -r * 0.65f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, c, c + Offset(r * 0.5f, r * 0.2f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            Glyph.ChannelLog -> {
                listOf(0.32f, 0.5f, 0.68f).forEach { y ->
                    drawLine(tint, pt(0.16f, y), pt(0.84f, y), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                }
            }

            Glyph.Stealth -> {
                // eye with a diagonal slash — "unseen"
                val p = Path().apply {
                    moveTo(w * 0.12f, h * 0.5f)
                    quadraticTo(w * 0.5f, h * 0.2f, w * 0.88f, h * 0.5f)
                    quadraticTo(w * 0.5f, h * 0.8f, w * 0.12f, h * 0.5f)
                    close()
                }
                drawPath(p, tint, style = thinStroke)
                drawCircle(tint, radius = w * 0.1f, center = pt(0.5f, 0.5f), style = thinStroke)
                drawLine(tint, pt(0.14f, 0.82f), pt(0.86f, 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            Glyph.Phosphor -> {
                // cycle dial — a ring with a chase arrow, echoes CONFIG's InstrumentIcon dial
                val r = w * 0.3f
                val c = pt(0.5f, 0.5f)
                drawArc(
                    tint, startAngle = -220f, sweepAngle = 260f, useCenter = false,
                    topLeft = c - Offset(r, r), size = Size(r * 2f, r * 2f), style = stroke
                )
                val tipAngle = Math.toRadians(40.0)
                val tip = c + Offset((r * cos(tipAngle)).toFloat(), (r * sin(tipAngle)).toFloat())
                val arrow = Path().apply {
                    moveTo(tip.x - w * 0.1f, tip.y - h * 0.05f)
                    lineTo(tip.x + w * 0.06f, tip.y)
                    lineTo(tip.x - w * 0.02f, tip.y + h * 0.12f)
                }
                drawPath(arrow, tint, style = thinStroke)
            }

            Glyph.Beacon -> {
                // signal flag on a mast
                drawLine(tint, pt(0.28f, 0.15f), pt(0.28f, 0.85f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                val flag = Path().apply {
                    moveTo(w * 0.28f, h * 0.18f)
                    lineTo(w * 0.78f, h * 0.3f)
                    lineTo(w * 0.28f, h * 0.42f)
                    close()
                }
                drawPath(flag, tint, style = thinStroke)
            }

            Glyph.Lock -> {
                val body = androidx.compose.ui.geometry.Rect(pt(0.2f, 0.45f), pt(0.8f, 0.85f))
                drawRect(tint, topLeft = body.topLeft, size = body.size, style = stroke)
                drawArc(
                    tint, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = pt(0.3f, 0.18f), size = Size(w * 0.4f, w * 0.4f), style = thinStroke
                )
                drawCircle(tint, radius = w * 0.045f, center = pt(0.5f, 0.63f))
            }

            Glyph.StatusReport -> {
                listOf(0.3f to 0.75f, 0.5f to 0.55f, 0.7f to 0.35f).forEach { (x, top) ->
                    drawLine(tint, pt(x, top), pt(x, 0.82f), strokeWidth = stroke.width * 1.3f, cap = StrokeCap.Round)
                }
            }

            Glyph.Say -> {
                val p = Path().apply {
                    moveTo(w * 0.16f, h * 0.22f)
                    lineTo(w * 0.84f, h * 0.22f)
                    lineTo(w * 0.84f, h * 0.62f)
                    lineTo(w * 0.42f, h * 0.62f)
                    lineTo(w * 0.3f, h * 0.82f)
                    lineTo(w * 0.3f, h * 0.62f)
                    lineTo(w * 0.16f, h * 0.62f)
                    close()
                }
                drawPath(p, tint, style = thinStroke)
            }

            Glyph.Warn -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    lineTo(w * 0.88f, h * 0.82f)
                    lineTo(w * 0.12f, h * 0.82f)
                    close()
                }
                drawPath(p, tint, style = stroke)
                drawLine(tint, pt(0.5f, 0.4f), pt(0.5f, 0.62f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawCircle(tint, radius = w * 0.03f, center = pt(0.5f, 0.72f))
            }

            Glyph.CommsChannel -> {
                // transmission bars radiating from a mast tip
                drawLine(tint, pt(0.5f, 0.85f), pt(0.5f, 0.35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(tint, radius = w * 0.045f, center = pt(0.5f, 0.25f))
                for (i in 1..2) {
                    val r = w * 0.16f * i
                    drawArc(
                        tint, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                        topLeft = pt(0.5f, 0.25f) - Offset(r, r), size = Size(r * 2f, r * 2f), style = thinStroke
                    )
                }
            }

            Glyph.Folder -> {
                val p = Path().apply {
                    moveTo(w * 0.14f, h * 0.3f)
                    lineTo(w * 0.42f, h * 0.3f)
                    lineTo(w * 0.5f, h * 0.42f)
                    lineTo(w * 0.86f, h * 0.42f)
                    lineTo(w * 0.86f, h * 0.78f)
                    lineTo(w * 0.14f, h * 0.78f)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }

            Glyph.FileDoc -> {
                val p = Path().apply {
                    moveTo(w * 0.26f, h * 0.14f)
                    lineTo(w * 0.62f, h * 0.14f)
                    lineTo(w * 0.78f, h * 0.32f)
                    lineTo(w * 0.78f, h * 0.86f)
                    lineTo(w * 0.26f, h * 0.86f)
                    close()
                }
                drawPath(p, tint, style = thinStroke)
                listOf(0.46f, 0.6f, 0.74f).forEach { y ->
                    drawLine(tint, pt(0.36f, y), pt(0.68f, y), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
                }
            }

            Glyph.Terminal -> {
                drawRect(tint, topLeft = pt(0.14f, 0.2f), size = Size(w * 0.72f, h * 0.6f), style = stroke)
                val caret = Path().apply {
                    moveTo(w * 0.26f, h * 0.38f)
                    lineTo(w * 0.4f, h * 0.5f)
                    lineTo(w * 0.26f, h * 0.62f)
                }
                drawPath(caret, tint, style = thinStroke)
                drawLine(tint, pt(0.46f, 0.62f), pt(0.62f, 0.62f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            Glyph.Decrypt -> {
                // stylised node-lattice — "neural reasoner"
                val c = pt(0.5f, 0.5f)
                drawCircle(tint, radius = w * 0.1f, center = c, style = thinStroke)
                listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f).forEach { (dx, dy) ->
                    val node = c + Offset(dx * w * 0.28f, dy * h * 0.28f)
                    drawLine(tint, c, node, strokeWidth = thinStroke.width * 0.8f)
                    drawCircle(tint, radius = w * 0.045f, center = node)
                }
            }

            Glyph.QuarkChat -> {
                val p = Path().apply {
                    moveTo(w * 0.16f, h * 0.24f)
                    lineTo(w * 0.84f, h * 0.24f)
                    lineTo(w * 0.84f, h * 0.6f)
                    lineTo(w * 0.34f, h * 0.6f)
                    lineTo(w * 0.24f, h * 0.78f)
                    lineTo(w * 0.24f, h * 0.6f)
                    lineTo(w * 0.16f, h * 0.6f)
                    close()
                }
                drawPath(p, tint, style = thinStroke)
                drawCircle(tint, radius = w * 0.03f, center = pt(0.38f, 0.42f))
                drawCircle(tint, radius = w * 0.03f, center = pt(0.5f, 0.42f))
                drawCircle(tint, radius = w * 0.03f, center = pt(0.62f, 0.42f))
            }

            Glyph.Send -> {
                val p = Path().apply {
                    moveTo(w * 0.16f, h * 0.5f)
                    lineTo(w * 0.82f, h * 0.18f)
                    lineTo(w * 0.6f, h * 0.82f)
                    lineTo(w * 0.46f, h * 0.56f)
                    close()
                }
                drawPath(p, tint, style = thinStroke)
                drawLine(tint, pt(0.46f, 0.56f), pt(0.82f, 0.18f), strokeWidth = thinStroke.width * 0.7f)
            }

            Glyph.CategoryFieldLogs -> {
                listOf(0.32f, 0.48f, 0.64f).forEach { y ->
                    drawLine(tint, pt(0.2f, y), pt(0.8f, y), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                }
                drawLine(tint, pt(0.2f, 0.24f), pt(0.8f, 0.24f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            Glyph.CategoryCaptures -> {
                drawRect(tint, topLeft = pt(0.16f, 0.28f), size = Size(w * 0.68f, h * 0.5f), style = stroke)
                drawCircle(tint, radius = w * 0.14f, center = pt(0.5f, 0.53f), style = thinStroke)
                drawLine(tint, pt(0.36f, 0.28f), pt(0.44f, 0.18f), strokeWidth = stroke.width)
                drawLine(tint, pt(0.44f, 0.18f), pt(0.64f, 0.18f), strokeWidth = stroke.width)
            }

            Glyph.CategoryCommsCache -> {
                val p = Path().apply {
                    moveTo(w * 0.18f, h * 0.24f)
                    lineTo(w * 0.82f, h * 0.24f)
                    lineTo(w * 0.82f, h * 0.6f)
                    lineTo(w * 0.5f, h * 0.6f)
                    lineTo(w * 0.36f, h * 0.78f)
                    lineTo(w * 0.36f, h * 0.6f)
                    lineTo(w * 0.18f, h * 0.6f)
                    close()
                }
                drawPath(p, tint, style = thinStroke)
            }

            Glyph.CategoryMaps -> {
                val p = Path().apply {
                    moveTo(w * 0.16f, h * 0.26f)
                    lineTo(w * 0.4f, h * 0.18f)
                    lineTo(w * 0.62f, h * 0.28f)
                    lineTo(w * 0.86f, h * 0.2f)
                    lineTo(w * 0.86f, h * 0.72f)
                    lineTo(w * 0.62f, h * 0.8f)
                    lineTo(w * 0.4f, h * 0.7f)
                    lineTo(w * 0.16f, h * 0.78f)
                    close()
                }
                drawPath(p, tint, style = thinStroke)
                drawLine(tint, pt(0.4f, 0.18f), pt(0.4f, 0.7f), strokeWidth = thinStroke.width * 0.6f)
                drawLine(tint, pt(0.62f, 0.28f), pt(0.62f, 0.8f), strokeWidth = thinStroke.width * 0.6f)
            }

            Glyph.Record -> drawCircle(tint, radius = w * 0.28f, center = pt(0.5f, 0.5f))

            Glyph.Play -> {
                val p = Path().apply {
                    moveTo(w * 0.28f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.5f)
                    lineTo(w * 0.28f, h * 0.82f)
                    close()
                }
                drawPath(p, tint, style = thinStroke)
            }

            Glyph.Pause -> {
                drawLine(tint, pt(0.32f, 0.18f), pt(0.32f, 0.82f), strokeWidth = w * 0.14f, cap = StrokeCap.Square)
                drawLine(tint, pt(0.68f, 0.18f), pt(0.68f, 0.82f), strokeWidth = w * 0.14f, cap = StrokeCap.Square)
            }

            Glyph.Stop -> drawRect(tint, topLeft = pt(0.24f, 0.24f), size = Size(w * 0.52f, h * 0.52f))

            Glyph.Mic -> {
                drawRoundRect(
                    tint, topLeft = pt(0.36f, 0.14f), size = Size(w * 0.28f, h * 0.44f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f), style = thinStroke
                )
                drawArc(
                    tint, startAngle = 20f, sweepAngle = 140f, useCenter = false,
                    topLeft = pt(0.2f, 0.32f), size = Size(w * 0.6f, h * 0.42f), style = thinStroke
                )
                drawLine(tint, pt(0.5f, 0.72f), pt(0.5f, 0.88f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, pt(0.34f, 0.88f), pt(0.66f, 0.88f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            Glyph.Shutter -> {
                drawCircle(tint, radius = w * 0.34f, center = pt(0.5f, 0.5f), style = stroke)
                drawCircle(tint, radius = w * 0.14f, center = pt(0.5f, 0.5f))
            }


            Glyph.Waypoint -> {
                val r = w * 0.22f
                val c = pt(0.5f, 0.32f)
                drawCircle(tint, radius = r, center = c, style = stroke)
                drawCircle(tint, radius = r * 0.4f, center = c)
                val p = Path().apply {
                    moveTo(c.x - r * 0.5f, c.y + r * 0.85f)
                    lineTo(w * 0.5f, h * 0.86f)
                    lineTo(c.x + r * 0.5f, c.y + r * 0.85f)
                }
                drawPath(p, tint, style = thinStroke)
            }

            Glyph.YouMarker -> {
                drawCircle(tint, radius = w * 0.12f, center = pt(0.5f, 0.5f))
                drawCircle(tint, radius = w * 0.32f, center = pt(0.5f, 0.5f), style = thinStroke)
            }

            Glyph.TunerDial -> {
                val r = w * 0.3f
                val c = pt(0.5f, 0.5f)
                drawCircle(tint, radius = r, center = c, style = stroke)
                drawLine(tint, c, c + Offset(0f, -r * 0.8f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                listOf(-40f, 0f, 40f).forEach { deg ->
                    val rad = Math.toRadians((deg - 90).toDouble())
                    val inner = c + Offset((r * 1.15f * cos(rad)).toFloat(), (r * 1.15f * sin(rad)).toFloat())
                    val outer = c + Offset((r * 1.4f * cos(rad)).toFloat(), (r * 1.4f * sin(rad)).toFloat())
                    drawLine(tint, inner, outer, strokeWidth = thinStroke.width * 0.8f)
                }
            }

            Glyph.Preset -> {
                val cx = w * 0.5f; val cy = h * 0.5f; val rOuter = w * 0.34f; val rInner = w * 0.14f
                val p = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) rOuter else rInner
                    val a = Math.toRadians((i * 36 - 90).toDouble())
                    val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, tint, style = thinStroke)
            }

            Glyph.Close -> {
                drawLine(tint, pt(0.22f, 0.22f), pt(0.78f, 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, pt(0.78f, 0.22f), pt(0.22f, 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            Glyph.Cellular -> {
                val xs = listOf(0.24f, 0.42f, 0.6f, 0.78f)
                val heights = listOf(0.22f, 0.4f, 0.58f, 0.76f)
                xs.forEachIndexed { i, x ->
                    drawLine(
                        tint, pt(x, 0.82f - heights[i]), pt(x, 0.82f),
                        strokeWidth = stroke.width * 1.2f, cap = StrokeCap.Round
                    )
                }
            }

            Glyph.Wifi -> {
                val c = pt(0.5f, 0.78f)
                drawCircle(tint, radius = w * 0.035f, center = c)
                for (i in 1..3) {
                    val r = w * 0.15f * i
                    drawArc(
                        tint, startAngle = 220f, sweepAngle = 100f, useCenter = false,
                        topLeft = c - Offset(r, r), size = Size(r * 2f, r * 2f), style = thinStroke
                    )
                }
            }

            Glyph.Gps -> {
                val c = pt(0.5f, 0.5f)
                drawCircle(tint, radius = w * 0.3f, center = c, style = thinStroke)
                drawCircle(tint, radius = w * 0.07f, center = c)
                listOf(0f, 90f, 180f, 270f).forEach { deg ->
                    val rad = Math.toRadians(deg.toDouble())
                    val inner = c + Offset((w * 0.32f * cos(rad)).toFloat(), (w * 0.32f * sin(rad)).toFloat())
                    val outer = c + Offset((w * 0.44f * cos(rad)).toFloat(), (w * 0.44f * sin(rad)).toFloat())
                    drawLine(tint, inner, outer, strokeWidth = thinStroke.width)
                }
            }

            Glyph.Bluetooth -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    lineTo(w * 0.72f, h * 0.32f)
                    lineTo(w * 0.5f, h * 0.5f)
                    lineTo(w * 0.72f, h * 0.68f)
                    lineTo(w * 0.5f, h * 0.85f)
                    lineTo(w * 0.5f, h * 0.15f)
                    moveTo(w * 0.3f, h * 0.32f)
                    lineTo(w * 0.7f, h * 0.68f)
                    moveTo(w * 0.3f, h * 0.68f)
                    lineTo(w * 0.7f, h * 0.32f)
                }
                drawPath(p, tint, style = thinStroke)
            }

            Glyph.RunScan -> {
                val c = pt(0.5f, 0.5f)
                drawCircle(tint, radius = w * 0.32f, center = c, style = thinStroke)
                val sweep = Path().apply {
                    moveTo(c.x, c.y)
                    lineTo(c.x, c.y - w * 0.32f)
                    arcTo(
                        androidx.compose.ui.geometry.Rect(c - Offset(w * 0.32f, w * 0.32f), Size(w * 0.64f, w * 0.64f)),
                        -90f, 55f, false
                    )
                    close()
                }
                drawPath(sweep, tint.copy(alpha = 0.35f))
                drawLine(tint, c, c + Offset(0f, -w * 0.32f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            Glyph.BootPace -> {
                val c = pt(0.5f, 0.5f)
                drawCircle(tint, radius = w * 0.32f, center = c, style = thinStroke)
                drawLine(tint, c, c + Offset(0f, -w * 0.2f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, c, c + Offset(w * 0.14f, w * 0.06f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            // ---- CAM focus scale (Glyph Sweep) ----------------------------------------------
            // A hyperfocal scale reads as a RAMP, so these four are deliberately one family: the
            // same subject mark growing smaller and further back as the distance grows, rather than
            // four unrelated pictograms (which is what the flower/person/cyclist/mountain emoji
            // were). At 12dp what the Operator resolves is the silhouette's size and position, not
            // its detail.
            Glyph.FocusMacro -> {
                // closest: one large mark, filling the box, with focus ticks either side
                drawCircle(tint, radius = w * 0.22f, center = pt(0.5f, 0.5f), style = stroke)
                drawCircle(tint, radius = w * 0.07f, center = pt(0.5f, 0.5f))
                drawLine(tint, pt(0.06f, 0.5f), pt(0.2f, 0.5f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, pt(0.8f, 0.5f), pt(0.94f, 0.5f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            Glyph.FocusPortrait -> {
                // near: head and shoulders
                drawCircle(tint, radius = w * 0.15f, center = pt(0.5f, 0.33f), style = stroke)
                val sh = Path().apply {
                    moveTo(w * 0.2f, h * 0.86f)
                    quadraticTo(w * 0.5f, h * 0.52f, w * 0.8f, h * 0.86f)
                }
                drawPath(sh, tint, style = stroke)
            }

            Glyph.FocusMid -> {
                // mid: the same figure, smaller and standing on a ground line
                drawCircle(tint, radius = w * 0.1f, center = pt(0.5f, 0.3f), style = thinStroke)
                drawLine(tint, pt(0.5f, 0.4f), pt(0.5f, 0.66f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, pt(0.34f, 0.5f), pt(0.66f, 0.5f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, pt(0.5f, 0.66f), pt(0.37f, 0.84f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, pt(0.5f, 0.66f), pt(0.63f, 0.84f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            Glyph.FocusLandscape -> {
                // far: a horizon ridge, no figure at all
                val ridge = Path().apply {
                    moveTo(w * 0.08f, h * 0.76f)
                    lineTo(w * 0.34f, h * 0.36f)
                    lineTo(w * 0.5f, h * 0.58f)
                    lineTo(w * 0.68f, h * 0.28f)
                    lineTo(w * 0.92f, h * 0.76f)
                }
                drawPath(ridge, tint, style = stroke)
                drawLine(tint, pt(0.06f, 0.86f), pt(0.94f, 0.86f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            Glyph.Infinity -> {
                // A real lemniscate -- ONE continuous ribbon that crosses itself, the Moebius figure
                // the Director asked for. Two tangent circles were the first attempt and then two
                // overlapping ones; both read as "oo" on device, because the eye needs the CROSSING
                // to say infinity, and neither had one. This is a single closed path through the
                // centre, so the crossing is the shape rather than an artefact of two outlines
                // meeting.
                val cx = w * 0.5f
                val cy = h * 0.5f
                val lobe = w * 0.24f      // horizontal reach of each lobe from centre
                val rise = h * 0.26f      // how far the ribbon swings above/below the axis
                val p = Path().apply {
                    moveTo(cx, cy)
                    // left lobe, anticlockwise back to the crossing
                    cubicTo(cx - lobe * 0.4f, cy - rise, cx - lobe * 1.7f, cy - rise, cx - lobe * 1.7f, cy)
                    cubicTo(cx - lobe * 1.7f, cy + rise, cx - lobe * 0.4f, cy + rise, cx, cy)
                    // right lobe, mirrored
                    cubicTo(cx + lobe * 0.4f, cy - rise, cx + lobe * 1.7f, cy - rise, cx + lobe * 1.7f, cy)
                    cubicTo(cx + lobe * 1.7f, cy + rise, cx + lobe * 0.4f, cy + rise, cx, cy)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }

            Glyph.Tilt -> {
                // a levelled plate over a bubble line -- an instrument, not a phone pictogram
                val plate = Path().apply {
                    moveTo(w * 0.16f, h * 0.62f)
                    lineTo(w * 0.84f, h * 0.38f)
                }
                drawPath(plate, tint, style = stroke)
                drawLine(tint, pt(0.1f, 0.8f), pt(0.9f, 0.8f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawCircle(tint, radius = w * 0.09f, center = pt(0.5f, 0.5f))
            }

            Glyph.Develop -> {
                // a developing tank: film chemistry, in keeping with the film-camera register
                val body = Path().apply {
                    moveTo(w * 0.34f, h * 0.16f)
                    lineTo(w * 0.34f, h * 0.42f)
                    lineTo(w * 0.18f, h * 0.82f)
                    lineTo(w * 0.82f, h * 0.82f)
                    lineTo(w * 0.66f, h * 0.42f)
                    lineTo(w * 0.66f, h * 0.16f)
                }
                drawPath(body, tint, style = stroke)
                drawLine(tint, pt(0.28f, 0.16f), pt(0.72f, 0.16f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, pt(0.29f, 0.66f), pt(0.71f, 0.66f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            // ---- console marks (Glyph Sweep) --------------------------------------------------
            Glyph.Forward -> {
                val p = Path().apply {
                    moveTo(w * 0.38f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.5f)
                    lineTo(w * 0.38f, h * 0.82f)
                }
                drawPath(p, tint, style = stroke)
            }

            Glyph.TriangleLeft -> {
                val p = Path().apply {
                    moveTo(w * 0.72f, h * 0.18f)
                    lineTo(w * 0.28f, h * 0.5f)
                    lineTo(w * 0.72f, h * 0.82f)
                    close()
                }
                drawPath(p, tint)
            }

            Glyph.TriangleRight -> {
                val p = Path().apply {
                    moveTo(w * 0.28f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.5f)
                    lineTo(w * 0.28f, h * 0.82f)
                    close()
                }
                drawPath(p, tint)
            }

            Glyph.CaretUp -> {
                val p = Path().apply {
                    moveTo(w * 0.18f, h * 0.7f)
                    lineTo(w * 0.5f, h * 0.3f)
                    lineTo(w * 0.82f, h * 0.7f)
                    close()
                }
                drawPath(p, tint)
            }

            Glyph.CaretDown -> {
                val p = Path().apply {
                    moveTo(w * 0.18f, h * 0.3f)
                    lineTo(w * 0.5f, h * 0.7f)
                    lineTo(w * 0.82f, h * 0.3f)
                    close()
                }
                drawPath(p, tint)
            }

            Glyph.Dot -> drawCircle(tint, radius = w * 0.24f, center = pt(0.5f, 0.5f))

            Glyph.Diamond -> {
                // registration diamond -- the shell's own alignment mark
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.88f)
                    lineTo(w * 0.12f, h * 0.5f)
                    close()
                }
                drawPath(p, tint, style = stroke)
                drawCircle(tint, radius = w * 0.1f, center = pt(0.5f, 0.5f))
            }

            Glyph.Crosshair -> {
                drawCircle(tint, radius = w * 0.3f, center = pt(0.5f, 0.5f), style = stroke)
                drawLine(tint, pt(0.5f, 0.06f), pt(0.5f, 0.94f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, pt(0.06f, 0.5f), pt(0.94f, 0.5f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
            }

            Glyph.Swap -> {
                drawLine(tint, pt(0.34f, 0.18f), pt(0.34f, 0.82f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                drawLine(tint, pt(0.66f, 0.18f), pt(0.66f, 0.82f), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                val up = Path().apply {
                    moveTo(w * 0.18f, h * 0.34f); lineTo(w * 0.34f, h * 0.14f); lineTo(w * 0.5f, h * 0.34f); close()
                }
                val down = Path().apply {
                    moveTo(w * 0.5f, h * 0.66f); lineTo(w * 0.66f, h * 0.86f); lineTo(w * 0.82f, h * 0.66f); close()
                }
                drawPath(up, tint); drawPath(down, tint)
            }

            Glyph.Cycle -> {
                drawArc(
                    tint, startAngle = 40f, sweepAngle = 280f, useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.18f), size = Size(w * 0.64f, h * 0.64f), style = stroke
                )
                val head = Path().apply {
                    moveTo(w * 0.66f, h * 0.58f); lineTo(w * 0.9f, h * 0.62f); lineTo(w * 0.74f, h * 0.82f); close()
                }
                drawPath(head, tint)
            }

            Glyph.Atom -> {
                // The Home mark. Nucleus plus three orbitals at 60 degrees -- drawn, so it carries
                // the active hue and cannot fall back to a platform emoji.
                drawCircle(tint, radius = w * 0.11f, center = pt(0.5f, 0.5f))
                listOf(0f, 60f, 120f).forEach { deg ->
                    val rad = deg * PI_F / 180f
                    rotateRad(rad, pivot = Offset(w * 0.5f, h * 0.5f)) {
                        drawOval(
                            tint,
                            topLeft = Offset(w * 0.5f - w * 0.44f, h * 0.5f - h * 0.17f),
                            size = Size(w * 0.88f, h * 0.34f),
                            style = thinStroke,
                        )
                    }
                }
            }

            Glyph.Charge -> {
                val p = Path().apply {
                    moveTo(w * 0.56f, h * 0.08f)
                    lineTo(w * 0.28f, h * 0.54f)
                    lineTo(w * 0.48f, h * 0.54f)
                    lineTo(w * 0.42f, h * 0.92f)
                    lineTo(w * 0.72f, h * 0.44f)
                    lineTo(w * 0.52f, h * 0.44f)
                    close()
                }
                drawPath(p, tint)
            }

            Glyph.Region -> {
                val c = pt(0.5f, 0.46f)
                drawCircle(tint, radius = w * 0.26f, center = c, style = thinStroke)
                drawLine(tint, c - Offset(w * 0.26f, 0f), c + Offset(w * 0.26f, 0f), strokeWidth = thinStroke.width * 0.7f)
                drawArc(
                    tint, startAngle = -90f, sweepAngle = 180f, useCenter = false,
                    topLeft = c - Offset(w * 0.12f, w * 0.26f), size = Size(w * 0.24f, w * 0.52f), style = thinStroke
                )
                val pin = Path().apply {
                    moveTo(c.x - w * 0.1f, h * 0.7f)
                    lineTo(w * 0.5f, h * 0.9f)
                    lineTo(c.x + w * 0.1f, h * 0.7f)
                }
                drawPath(pin, tint, style = thinStroke)
            }
        }
    }
}

/*
 * One glyph beside one label, which is what nearly every replaced call site actually needed.
 *
 * The Glyph Sweep replaced ~40 bare Unicode marks that were sitting INSIDE text strings -- "◄ STOW",
 * "🌸 0.7m", "MODE: $mode ⇅". Each of those has to become an icon plus a text, and hand-rolling
 * forty Rows would have produced forty slightly different spacings and alignments. This is the one
 * shape, so the icon/label rhythm is identical everywhere and a change to it is a change everywhere.
 *
 * [trailing] puts the glyph AFTER the label, for the sites whose mark trailed the text ("TUNE + ▶",
 * "UP ◄") -- reading order is preserved rather than silently normalised.
 */
@Composable
fun GlyphLabel(
    glyph: Glyph,
    text: String,
    tint: Color,
    font: FontFamily,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    iconSize: Dp = 14.dp,
    trailing: Boolean = false,
    fontWeight: FontWeight? = null,
    textTint: Color = tint,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (!trailing) {
            QuantumIcon(glyph, tint, size = iconSize)
            Spacer(Modifier.width(iconSize * 0.42f))
        }
        Text(text, color = textTint, fontFamily = font, fontSize = fontSize, fontWeight = fontWeight)
        if (trailing) {
            Spacer(Modifier.width(iconSize * 0.42f))
            QuantumIcon(glyph, tint, size = iconSize)
        }
    }
}
