package com.quantumos.appshell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

/*
 * AppGlyphs — the themed icon pack for the APPS grid (Icon Direction, decision 60).
 *
 * The problem this solves: the APPS channel is a LAUNCHER, so every cell was drawing the installed
 * app's OWN icon straight from the PackageManager at 40dp — Google's multicoloured art, Meta's
 * blues, every vendor's gloss, tiled across a phosphor CRT. It is the single largest off-palette
 * surface in the OS and no amount of chrome around it helps.
 *
 * **Why this is a SEPARATE enum from [Glyph].** `Glyph` is the OS's own chrome vocabulary — controls,
 * channels, states, things the Operator acts on. This is a pack that stands in for THIRD-PARTY art,
 * matched heuristically and never load-bearing. Keeping them apart means the chrome set stays small
 * and auditable (every value reachable from a call site — a rule this pack cannot honour, since which
 * entries are reachable depends on what is installed on the device).
 *
 * **Why drawn and not generated.** The Director offered the ComfyUI pipeline. It is the right tool
 * for illustrative badges and the wrong one here, for the same reasons recorded for the UI glyph
 * sweep: these must retint live from the active hue, stay crisp at 40dp on every density, and hold a
 * uniform stroke weight across the whole set. Diffusion gives raster approximations with inconsistent
 * weight, needs one file per density, and would add megabytes to an APK that is already 274MB.
 * `reference/AppIconPackRef.png` is used as FORM reference — its vocabulary of subjects — not as art.
 *
 * Anything not matched here is NOT given a generic mark: the launcher's `AppCell` falls back to the
 * app's REAL icon put through a phosphor luminance map (`phosphorFilter`, LauncherUi.kt), so an
 * unrecognised app keeps its own silhouette -- still recognisable, still on-palette.
 */
enum class AppGlyph {
    Messages, Mail, Contacts, Calls, Camera, Gallery, Music, Video, Recorder,
    Notes, Calendar, Clock, Weather, MapPin, Browser, Store, Calculator,
    Settings, Wallet, Translate, Health, Game, News, Files, Security,
}

/*
 * Package-name → glyph. Substring matching on the package id, deliberately, so the pack generalises
 * to apps nobody listed: any package containing "camera" gets the camera mark whether it is Google's,
 * Samsung's, or an OEM fork nobody has heard of. Ordered most-specific-first, because "com.google.
 * android.apps.messaging" contains both "messaging" and "google".
 *
 * Matching is on the PACKAGE, not the label: labels are localised and change, package ids do not.
 */
private val PACKAGE_RULES: List<Pair<List<String>, AppGlyph>> = listOf(
    listOf("incallui", "dialer", ".phone", "contacts.dialer") to AppGlyph.Calls,
    listOf("contacts", "people") to AppGlyph.Contacts,
    listOf("messaging", "messages", ".mms", ".sms", "whatsapp", "telegram", "signal", "messenger")
        to AppGlyph.Messages,
    listOf("gm", "mail", "outlook", "inbox") to AppGlyph.Mail,
    listOf("camera") to AppGlyph.Camera,
    listOf("gallery", "photos", "album") to AppGlyph.Gallery,
    listOf("soundrecorder", "recorder", "voicerecorder") to AppGlyph.Recorder,
    listOf("music", "spotify", "audio") to AppGlyph.Music,
    listOf("youtube", "video", "netflix", "player") to AppGlyph.Video,
    listOf("notes", "keep", "memo") to AppGlyph.Notes,
    listOf("calendar") to AppGlyph.Calendar,
    listOf("deskclock", "clock", "alarm", "timer") to AppGlyph.Clock,
    listOf("weather") to AppGlyph.Weather,
    listOf("maps", "navigation", "waze") to AppGlyph.MapPin,
    listOf("chrome", "browser", "firefox", "webview") to AppGlyph.Browser,
    listOf("vending", "playstore", "store", "market") to AppGlyph.Store,
    listOf("calculator") to AppGlyph.Calculator,
    listOf("settings") to AppGlyph.Settings,
    listOf("wallet", "pay", "bank") to AppGlyph.Wallet,
    listOf("translate") to AppGlyph.Translate,
    listOf("health", "fit", "samsunghealth") to AppGlyph.Health,
    listOf("game", "play.games") to AppGlyph.Game,
    listOf("news", "reader", "rss") to AppGlyph.News,
    listOf("documentsui", "filemanager", "myfiles", "files") to AppGlyph.Files,
    listOf("security", "antivirus", "vpn", "authenticator") to AppGlyph.Security,
)

/** The pack's mark for [packageName], or null when nothing matches and the real icon should be used. */
fun appGlyphFor(packageName: String): AppGlyph? {
    val p = packageName.lowercase(Locale.US)
    for ((keys, glyph) in PACKAGE_RULES) {
        if (keys.any { it in p }) return glyph
    }
    return null
}

/**
 * One themed app mark. Same Canvas/Path discipline and stroke weight as [QuantumIcon] so the pack and
 * the OS chrome read as one hand, and the caller always passes the active phosphor.
 */
@Composable
fun AppIcon(glyph: AppGlyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
        val thin = Stroke(width = w * 0.055f, cap = StrokeCap.Round)
        fun pt(fx: Float, fy: Float) = Offset(w * fx, h * fy)
        fun line(a: Offset, b: Offset, s: Stroke = thin) =
            drawLine(tint, a, b, strokeWidth = s.width, cap = StrokeCap.Round)

        when (glyph) {
            AppGlyph.Messages -> {
                val p = Path().apply {
                    moveTo(w * 0.14f, h * 0.7f); lineTo(w * 0.14f, h * 0.24f)
                    lineTo(w * 0.86f, h * 0.24f); lineTo(w * 0.86f, h * 0.7f)
                    lineTo(w * 0.42f, h * 0.7f); lineTo(w * 0.26f, h * 0.86f)
                    lineTo(w * 0.26f, h * 0.7f); close()
                }
                drawPath(p, tint, style = stroke)
                listOf(0.34f, 0.5f, 0.66f).forEach { drawCircle(tint, w * 0.035f, pt(it, 0.47f)) }
            }

            AppGlyph.Mail -> {
                drawRect(tint, topLeft = pt(0.12f, 0.26f), size = Size(w * 0.76f, h * 0.48f), style = stroke)
                val flap = Path().apply {
                    moveTo(w * 0.12f, h * 0.26f); lineTo(w * 0.5f, h * 0.56f); lineTo(w * 0.88f, h * 0.26f)
                }
                drawPath(flap, tint, style = thin)
            }

            AppGlyph.Contacts -> {
                drawCircle(tint, w * 0.15f, pt(0.5f, 0.34f), style = stroke)
                val sh = Path().apply {
                    moveTo(w * 0.2f, h * 0.84f); quadraticTo(w * 0.5f, h * 0.52f, w * 0.8f, h * 0.84f)
                }
                drawPath(sh, tint, style = stroke)
            }

            AppGlyph.Calls -> {
                val p = Path().apply {
                    moveTo(w * 0.26f, h * 0.16f); lineTo(w * 0.42f, h * 0.3f)
                    lineTo(w * 0.32f, h * 0.46f); quadraticTo(w * 0.5f, h * 0.66f, w * 0.56f, h * 0.7f)
                    lineTo(w * 0.7f, h * 0.6f); lineTo(w * 0.86f, h * 0.76f)
                    quadraticTo(w * 0.6f, h * 0.98f, w * 0.26f, h * 0.16f)
                }
                drawPath(p, tint, style = stroke)
            }

            AppGlyph.Camera -> {
                drawRect(tint, topLeft = pt(0.1f, 0.3f), size = Size(w * 0.8f, h * 0.46f), style = stroke)
                drawCircle(tint, w * 0.15f, pt(0.5f, 0.53f), style = thin)
                line(pt(0.34f, 0.3f), pt(0.42f, 0.2f)); line(pt(0.42f, 0.2f), pt(0.58f, 0.2f))
                line(pt(0.58f, 0.2f), pt(0.66f, 0.3f))
            }

            AppGlyph.Gallery -> {
                drawRect(tint, topLeft = pt(0.12f, 0.24f), size = Size(w * 0.76f, h * 0.52f), style = stroke)
                val hill = Path().apply {
                    moveTo(w * 0.16f, h * 0.72f); lineTo(w * 0.38f, h * 0.46f)
                    lineTo(w * 0.54f, h * 0.62f); lineTo(w * 0.68f, h * 0.5f); lineTo(w * 0.84f, h * 0.72f)
                }
                drawPath(hill, tint, style = thin)
                drawCircle(tint, w * 0.05f, pt(0.68f, 0.34f))
            }

            AppGlyph.Music -> {
                line(pt(0.4f, 0.76f), pt(0.4f, 0.2f), stroke)
                line(pt(0.4f, 0.2f), pt(0.76f, 0.28f), stroke)
                line(pt(0.76f, 0.28f), pt(0.76f, 0.66f), stroke)
                drawCircle(tint, w * 0.1f, pt(0.3f, 0.76f), style = thin)
                drawCircle(tint, w * 0.1f, pt(0.66f, 0.66f), style = thin)
            }

            AppGlyph.Video -> {
                drawRect(tint, topLeft = pt(0.1f, 0.3f), size = Size(w * 0.56f, h * 0.42f), style = stroke)
                val lens = Path().apply {
                    moveTo(w * 0.72f, h * 0.44f); lineTo(w * 0.9f, h * 0.32f)
                    lineTo(w * 0.9f, h * 0.7f); lineTo(w * 0.72f, h * 0.58f); close()
                }
                drawPath(lens, tint, style = thin)
            }

            AppGlyph.Recorder -> {
                drawRoundRectMic(tint, w, h, stroke, thin)
            }

            AppGlyph.Notes -> {
                // A page WITH A PENCIL. Without it this is a rectangle full of rules, which is
                // exactly what News is -- the two were near-identical at 34dp in the preview.
                drawRect(tint, topLeft = pt(0.14f, 0.14f), size = Size(w * 0.56f, h * 0.72f), style = stroke)
                listOf(0.36f, 0.5f).forEach { line(pt(0.26f, it), pt(0.58f, it)) }
                val pencil = Path().apply {
                    moveTo(w * 0.58f, h * 0.82f); lineTo(w * 0.62f, h * 0.66f)
                    lineTo(w * 0.9f, h * 0.38f); lineTo(w * 0.98f, h * 0.46f)
                    lineTo(w * 0.7f, h * 0.74f); close()
                }
                drawPath(pencil, tint, style = thin)
            }

            AppGlyph.Calendar -> {
                drawRect(tint, topLeft = pt(0.13f, 0.22f), size = Size(w * 0.74f, h * 0.64f), style = stroke)
                line(pt(0.13f, 0.4f), pt(0.87f, 0.4f), stroke)
                line(pt(0.32f, 0.12f), pt(0.32f, 0.28f)); line(pt(0.68f, 0.12f), pt(0.68f, 0.28f))
                drawRect(tint, topLeft = pt(0.3f, 0.52f), size = Size(w * 0.12f, h * 0.12f))
            }

            AppGlyph.Clock -> {
                drawCircle(tint, w * 0.35f, pt(0.5f, 0.52f), style = stroke)
                line(pt(0.5f, 0.52f), pt(0.5f, 0.3f)); line(pt(0.5f, 0.52f), pt(0.66f, 0.6f))
            }

            AppGlyph.Weather -> {
                val cloud = Path().apply {
                    moveTo(w * 0.24f, h * 0.66f)
                    quadraticTo(w * 0.1f, h * 0.66f, w * 0.16f, h * 0.5f)
                    quadraticTo(w * 0.22f, h * 0.32f, w * 0.44f, h * 0.38f)
                    quadraticTo(w * 0.6f, h * 0.24f, w * 0.72f, h * 0.42f)
                    quadraticTo(w * 0.9f, h * 0.44f, w * 0.84f, h * 0.66f)
                    close()
                }
                drawPath(cloud, tint, style = stroke)
                listOf(0.34f, 0.52f, 0.7f).forEach { line(pt(it, 0.76f), pt(it - 0.05f, 0.9f)) }
            }

            AppGlyph.MapPin -> {
                val pin = Path().apply {
                    moveTo(w * 0.5f, h * 0.88f)
                    quadraticTo(w * 0.2f, h * 0.56f, w * 0.28f, h * 0.38f)
                    quadraticTo(w * 0.5f, h * 0.06f, w * 0.72f, h * 0.38f)
                    quadraticTo(w * 0.8f, h * 0.56f, w * 0.5f, h * 0.88f)
                }
                drawPath(pin, tint, style = stroke)
                drawCircle(tint, w * 0.09f, pt(0.5f, 0.42f))
            }

            AppGlyph.Browser -> {
                drawCircle(tint, w * 0.36f, pt(0.5f, 0.5f), style = stroke)
                line(pt(0.14f, 0.5f), pt(0.86f, 0.5f))
                drawOval(
                    tint, topLeft = pt(0.32f, 0.14f), size = Size(w * 0.36f, h * 0.72f), style = thin
                )
            }

            AppGlyph.Store -> {
                val bag = Path().apply {
                    moveTo(w * 0.2f, h * 0.36f); lineTo(w * 0.8f, h * 0.36f)
                    lineTo(w * 0.72f, h * 0.86f); lineTo(w * 0.28f, h * 0.86f); close()
                }
                drawPath(bag, tint, style = stroke)
                val handle = Path().apply {
                    moveTo(w * 0.36f, h * 0.36f)
                    quadraticTo(w * 0.5f, h * 0.06f, w * 0.64f, h * 0.36f)
                }
                drawPath(handle, tint, style = thin)
            }

            AppGlyph.Calculator -> {
                drawRect(tint, topLeft = pt(0.18f, 0.12f), size = Size(w * 0.64f, h * 0.76f), style = stroke)
                line(pt(0.28f, 0.32f), pt(0.72f, 0.32f), stroke)
                listOf(0.36f, 0.5f, 0.64f).forEach { x ->
                    listOf(0.52f, 0.7f).forEach { y -> drawCircle(tint, w * 0.035f, pt(x, y)) }
                }
            }

            AppGlyph.Settings -> {
                // Faders, not a gear. A ringed gear drawn at this weight reads as a SUN at 34dp and
                // collided with Weather in the geometry preview -- and a bank of faders is the more
                // honest metaphor for a field console anyway.
                listOf(0.28f to 0.66f, 0.5f to 0.36f, 0.72f to 0.58f).forEach { (y, knob) ->
                    line(pt(0.14f, y), pt(0.86f, y), stroke)
                    drawRect(
                        tint,
                        topLeft = Offset(w * knob - w * 0.045f, h * y - h * 0.11f),
                        size = Size(w * 0.09f, h * 0.22f),
                    )
                }
            }

            AppGlyph.Wallet -> {
                drawRect(tint, topLeft = pt(0.12f, 0.28f), size = Size(w * 0.76f, h * 0.44f), style = stroke)
                line(pt(0.12f, 0.42f), pt(0.88f, 0.42f), stroke)
                drawCircle(tint, w * 0.06f, pt(0.72f, 0.58f))
            }

            AppGlyph.Translate -> {
                line(pt(0.16f, 0.72f), pt(0.34f, 0.24f), stroke)
                line(pt(0.34f, 0.24f), pt(0.52f, 0.72f), stroke)
                line(pt(0.24f, 0.56f), pt(0.44f, 0.56f))
                val speech = Path().apply {
                    moveTo(w * 0.5f, h * 0.44f); lineTo(w * 0.9f, h * 0.44f)
                    lineTo(w * 0.9f, h * 0.8f); lineTo(w * 0.66f, h * 0.8f)
                    lineTo(w * 0.56f, h * 0.92f); lineTo(w * 0.56f, h * 0.8f)
                    lineTo(w * 0.5f, h * 0.8f); close()
                }
                drawPath(speech, tint, style = thin)
            }

            AppGlyph.Health -> {
                val heart = Path().apply {
                    moveTo(w * 0.5f, h * 0.84f)
                    quadraticTo(w * 0.08f, h * 0.54f, w * 0.28f, h * 0.28f)
                    quadraticTo(w * 0.5f, h * 0.16f, w * 0.5f, h * 0.4f)
                    quadraticTo(w * 0.5f, h * 0.16f, w * 0.72f, h * 0.28f)
                    quadraticTo(w * 0.92f, h * 0.54f, w * 0.5f, h * 0.84f)
                }
                drawPath(heart, tint, style = stroke)
            }

            AppGlyph.Game -> {
                drawRoundRectPad(tint, w, h, stroke, thin)
            }

            AppGlyph.News -> {
                drawRect(tint, topLeft = pt(0.12f, 0.2f), size = Size(w * 0.76f, h * 0.6f), style = stroke)
                line(pt(0.2f, 0.34f), pt(0.5f, 0.34f), stroke)
                listOf(0.48f, 0.6f, 0.72f).forEach { line(pt(0.2f, it), pt(0.8f, it)) }
            }

            AppGlyph.Files -> {
                val f = Path().apply {
                    moveTo(w * 0.12f, h * 0.78f); lineTo(w * 0.12f, h * 0.26f)
                    lineTo(w * 0.42f, h * 0.26f); lineTo(w * 0.5f, h * 0.36f)
                    lineTo(w * 0.88f, h * 0.36f); lineTo(w * 0.88f, h * 0.78f); close()
                }
                drawPath(f, tint, style = stroke)
            }

            AppGlyph.Security -> {
                val shield = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f); lineTo(w * 0.84f, h * 0.28f)
                    lineTo(w * 0.84f, h * 0.54f)
                    quadraticTo(w * 0.84f, h * 0.8f, w * 0.5f, h * 0.9f)
                    quadraticTo(w * 0.16f, h * 0.8f, w * 0.16f, h * 0.54f)
                    lineTo(w * 0.16f, h * 0.28f); close()
                }
                drawPath(shield, tint, style = stroke)
                line(pt(0.36f, 0.52f), pt(0.47f, 0.64f), stroke)
                line(pt(0.47f, 0.64f), pt(0.66f, 0.4f), stroke)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectMic(
    tint: Color, w: Float, h: Float, stroke: Stroke, thin: Stroke,
) {
    drawRoundRect(
        tint, topLeft = Offset(w * 0.38f, h * 0.12f), size = Size(w * 0.24f, h * 0.42f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = stroke,
    )
    drawArc(
        tint, startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(w * 0.26f, h * 0.36f), size = Size(w * 0.48f, h * 0.36f), style = thin,
    )
    drawLine(tint, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.88f), strokeWidth = thin.width, cap = StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectPad(
    tint: Color, w: Float, h: Float, stroke: Stroke, thin: Stroke,
) {
    drawRoundRect(
        tint, topLeft = Offset(w * 0.1f, h * 0.32f), size = Size(w * 0.8f, h * 0.36f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f), style = stroke,
    )
    drawLine(tint, Offset(w * 0.26f, h * 0.5f), Offset(w * 0.4f, h * 0.5f), strokeWidth = thin.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.33f, h * 0.43f), Offset(w * 0.33f, h * 0.57f), strokeWidth = thin.width, cap = StrokeCap.Round)
    drawCircle(tint, w * 0.05f, Offset(w * 0.66f, h * 0.44f))
    drawCircle(tint, w * 0.05f, Offset(w * 0.75f, h * 0.55f))
}
