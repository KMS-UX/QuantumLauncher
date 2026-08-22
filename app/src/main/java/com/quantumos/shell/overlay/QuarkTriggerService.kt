package com.quantumos.shell.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.quantumos.core.OverlayGeometry
import kotlin.math.hypot

/*
 * QuantumOS — M4 floating QUARK trigger.
 *
 * A persistent, app-icon-sized phosphor "iris" mark that floats over EVERY app (including apps
 * outside QuantumOS) via a TYPE_APPLICATION_OVERLAY window owned by a foreground Service — NOT an
 * Activity-scoped overlay, which wouldn't survive switching apps. Static at rest (no idle redraw);
 * draggable 1:1; snaps to the nearest edge on release; tap opens the M5 placeholder stub.
 *
 * Scope boundary (M4): this builds the TRIGGER only. What's behind it — the real QUARK Assistant
 * View — is M5. Tap therefore plays the PLEASE STANDBY beat and opens a placeholder stub.
 */
class QuarkTriggerService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var iris: IrisView? = null
    private val handler = Handler(Looper.getMainLooper())

    private var hueColor: Int = DEFAULT_HUE_COLOR
    private var viewSizePx: Int = 0
    private var foregroundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        viewSizePx = (TRIGGER_SIZE_DP * resources.displayMetrics.density).toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground promotion must happen promptly after startForegroundService(). Idempotent.
        if (!foregroundStarted) {
            startForegroundCompat()
            foregroundStarted = true
        }
        // Live hue sync: a redelivered start command (e.g. the Operator switched phosphor) re-tints
        // the mark in place rather than tearing the overlay down.
        intent?.takeIf { it.hasExtra(EXTRA_HUE_COLOR) }?.let {
            hueColor = it.getIntExtra(EXTRA_HUE_COLOR, DEFAULT_HUE_COLOR)
        }
        ensureOverlay()
        iris?.apply { setHue(hueColor); invalidate() }
        return START_STICKY
    }

    // ---------- overlay window ----------
    private fun ensureOverlay() {
        if (iris != null) return

        layoutParams = WindowManager.LayoutParams(
            viewSizePx,
            viewSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val bounds = windowManager.currentWindowMetrics.bounds
            // Default park (first launch): right edge, mid-height — clear of the bottom-centre
            // gesture area and the status bar. Computed by the unit-tested core helper.
            val (px, py) = OverlayGeometry.defaultPark(viewSizePx, bounds.width(), bounds.height())
            x = px
            y = py
        }

        val view = IrisView(this).also { it.setHue(hueColor) }
        attachTouch(view)
        iris = view
        windowManager.addView(view, layoutParams)
    }

    private fun attachTouch(view: View) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        val tapTimeout = ViewConfiguration.getTapTimeout().toLong()

        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var downTime = 0L
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = layoutParams.x
                    startY = layoutParams.y
                    downTime = SystemClock.elapsedRealtime()
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (hypot((event.rawX - downRawX).toDouble(), (event.rawY - downRawY).toDouble()) > touchSlop) {
                        moved = true
                    }
                    // 1:1, real-time follow — the one place "stepped" motion does NOT apply.
                    val bounds = windowManager.currentWindowMetrics.bounds
                    layoutParams.x = (startX + dx).coerceIn(0, bounds.width() - viewSizePx)
                    layoutParams.y = (startY + dy).coerceIn(0, bounds.height() - viewSizePx)
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = SystemClock.elapsedRealtime() - downTime
                    if (!moved && elapsed <= tapTimeout * 3) {
                        onTriggerTapped()
                    } else {
                        snapToEdge(view)
                    }
                    true
                }
                else -> false
            }
        }
    }

    // Quick, decisive, STEPPED settle to the nearest edge (matches the mechanical motion language —
    // not a slow elastic ease).
    private fun snapToEdge(view: View) {
        val bounds = windowManager.currentWindowMetrics.bounds
        val targetX = OverlayGeometry.nearestEdgeX(layoutParams.x, viewSizePx, bounds.width())
        val originX = layoutParams.x
        val steps = 6
        for (i in 1..steps) {
            handler.postDelayed({
                if (iris == null) return@postDelayed
                layoutParams.x = originX + (targetX - originX) * i / steps
                windowManager.updateViewLayout(view, layoutParams)
            }, i * STEP_INTERVAL_MS)
        }
    }

    private fun onTriggerTapped() {
        // Tap → PLEASE STANDBY beat → the M5 QUARK Assistant View. The beat lives in the Activity so
        // the trigger overlay stays a thin, static mark. NEW_TASK so it surfaces over whatever app
        // is currently in the foreground. (The hue extra is legacy; the Assistant now reads the live
        // phosphor hue from the shared engine, so it recolours without a restart.)
        val intent = Intent(this, QuarkAssistantActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_HUE_COLOR, hueColor)
        }
        startActivity(intent)
    }

    // ---------- foreground notification (minimal, unobtrusive — polish later) ----------
    private fun startForegroundCompat() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "QUARK Trigger",
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            nm.createNotificationChannel(channel)
        }
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("QUANTUMOS")
            .setContentText("QUARK TRIGGER // DEPLOYED")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        iris?.let { runCatching { windowManager.removeView(it) } }
        iris = null
    }

    // ---------- the QUARK trigger mark ----------
    //
    // The real artwork now (`reference/QUARKIcon.png`, downscaled to 256px): her face in a circular
    // HUD badge. It replaces the drawn iris rings that were labelled in this file as "static
    // placeholder art, NOT the final QUARK mascot" from the day they were written.
    //
    // **Phosphor-tinted at draw time, not baked.** The source art is full colour -- measured mean
    // saturation 0.55 -- and the house style is explicit that icons are themeable with the active
    // phosphor and that off-palette colour is not introduced. So the bitmap is mapped
    // luminance -> active hue through a ColorMatrix, exactly the maths the avatar plates use at
    // PHOSPHOR TINT 100%. One asset serves green, amber and cyan, and it re-tints live when the hue
    // changes rather than needing three files.
    //
    // The art survives that treatment: its luminance spans the full 0..255, so the face, the ring
    // ticks and the wordmark stay legible as monochrome rather than flattening into a disc.
    private class IrisView(context: Context) : View(context) {
        private var bright: Int = DEFAULT_HUE_COLOR

        private val icon: Bitmap? = runCatching {
            BitmapFactory.decodeResource(context.resources, com.quantumos.shell.R.drawable.quark_trigger)
        }.getOrNull()

        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val dst = RectF()

        // A CRT-ground disc behind her, so the badge reads against any wallpaper the Operator has
        // rather than only against a dark one.
        private val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = CRT_GROUND
        }
        // Kept from the placeholder: a thin bright ring is what makes a floating control read as a
        // control at 52dp, where the art alone is a small picture.
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        fun setHue(color: Int) {
            bright = color
            ringPaint.color = bright
            iconPaint.colorFilter = ColorMatrixColorFilter(phosphorMatrix(color))
            invalidate()
        }

        /** Map every channel to luminance x the hue, so the art becomes monochrome phosphor. */
        private fun phosphorMatrix(color: Int): ColorMatrix {
            val r = Color.red(color) / 255f
            val g = Color.green(color) / 255f
            val b = Color.blue(color) / 255f
            return ColorMatrix(
                floatArrayOf(
                    LUMA_R * r, LUMA_G * r, LUMA_B * r, 0f, 0f,
                    LUMA_R * g, LUMA_G * g, LUMA_B * g, 0f, 0f,
                    LUMA_R * b, LUMA_G * b, LUMA_B * b, 0f, 0f,
                    // Alpha untouched -- the badge's own transparent surround is the silhouette.
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = minOf(cx, cy)
            val stroke = r * 0.08f
            canvas.drawCircle(cx, cy, r - stroke, groundPaint)
            icon?.let {
                dst.set(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawBitmap(it, null, dst, iconPaint)
            }
            ringPaint.strokeWidth = stroke
            canvas.drawCircle(cx, cy, r - stroke / 2f, ringPaint)
        }
    }

    companion object {
        private const val CHANNEL_ID = "quark_trigger"
        private const val NOTIFICATION_ID = 0x9001
        private const val TRIGGER_SIZE_DP = 52f          // ≈ an APPS-grid icon footprint, not larger
        private const val STEP_INTERVAL_MS = 12L
        const val EXTRA_HUE_COLOR = "com.quantumos.shell.overlay.HUE_COLOR"

        // Phosphor green default (#00FF00) — the active hue at first launch. Live hue switches are
        // pushed in via redelivered start commands; see LauncherActivity.
        private const val DEFAULT_HUE_COLOR = 0xFF00FF00.toInt()
        private const val CRT_GROUND = 0xFF020402.toInt()

        // Rec.709 luminance, the same weights the avatar's plate and colour-grading paths use, so
        // the trigger and QUARK herself go monochrome by identical maths.
        private const val LUMA_R = 0.2126f
        private const val LUMA_G = 0.7152f
        private const val LUMA_B = 0.0722f

        // Dim pair derived by halving RGB — keeps the iris on-palette for any active phosphor hue
        // without the Service needing the PhosphorHue enum.
        private fun dimOf(color: Int): Int = Color.rgb(
            (Color.red(color) * 2) / 3,
            (Color.green(color) * 2) / 3,
            (Color.blue(color) * 2) / 3
        )

        /** Start (or re-tint) the floating trigger. Caller must hold the overlay permission. */
        fun deploy(context: Context, hueColor: Int) {
            val intent = Intent(context, QuarkTriggerService::class.java)
                .putExtra(EXTRA_HUE_COLOR, hueColor)
            context.startForegroundService(intent)
        }
    }
}
