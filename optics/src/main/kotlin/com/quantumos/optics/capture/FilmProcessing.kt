package com.quantumos.optics.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import java.io.File

enum class FilmProfile(val label: String, val desc: String) {
    BW_TRI_X("TRI-X 400", "QUANTUM SILVER (HIGH-CONTRAST ISOTOPE)"),
    COL_PORTRA("PORTRA 400", "BLACKHOLE CHROMATIC (WARM ATOMIC EMULSION)")
}

fun playMechanicalShutterSound() {
    Thread {
        try {
            val sampleRate = 44100
            val durationS = 0.28f
            val numSamples = (durationS * sampleRate).toInt()
            val samples = FloatArray(numSamples)
            val random = java.util.Random()

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                
                // 1. First curtain fabric tension latch snap (soft metal click)
                var click = 0f
                if (t < 0.015f) {
                    val clickFreq = 2500f + 500f * Math.sin(2.0 * Math.PI * 150.0 * t).toFloat()
                    val clickEnvelope = Math.exp(-t * 800.0).toFloat()
                    click = Math.sin(2.0 * Math.PI * clickFreq * t).toFloat() * clickEnvelope * 0.25f
                }

                // 2. Horizontal Rubberized Cloth Curtain travel friction (fabric rubbing whisper)
                var noise = 0f
                if (t < 0.150f) {
                    val noiseEnvelope = if (t < 0.030f) {
                        t / 0.030f
                    } else {
                        Math.exp(-(t - 0.030f) * 12.0).toFloat()
                    }
                    // A simple 2-point moving average filter to low-pass filter the white noise, 
                    // giving it a soft, non-harsh, "whispery fabric slide" cloth curtain timbre!
                    val rawNoise1 = (random.nextFloat() * 2f - 1f)
                    val rawNoise2 = (random.nextFloat() * 2f - 1f)
                    val filteredNoise = (rawNoise1 + rawNoise2) * 0.5f
                    noise = filteredNoise * noiseEnvelope * 0.22f
                }

                // 3. Second cloth curtain damp leather thud & braking latch snap
                var slap = 0f
                val slapDelay = 0.085f // Travel time is slower for rubberized cloth curtains (~85ms)
                if (t >= slapDelay && t < 0.260f) {
                    val st = t - slapDelay
                    // Soft, non-resonant, leather-on-felt-wood damp thud (110Hz base thud decaying rapidly)
                    val thudFreq = 110f - 20f * (st * 8f)
                    val thudEnv = Math.exp(-st * 50.0).toFloat()
                    val thud = Math.sin(2.0 * Math.PI * thudFreq * st).toFloat() * thudEnv * 0.48f

                    // Soft mechanical curtain latch lock tick (900Hz)
                    val latchEnv = Math.exp(-st * 250.0).toFloat()
                    val latch = Math.sin(2.0 * Math.PI * 900f * st).toFloat() * latchEnv * 0.12f
                    
                    slap = thud + latch
                }

                samples[i] = (click + noise + slap).coerceIn(-1.0f, 1.0f)
            }

            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                buffer[i] = (samples[i] * 32767).toInt().toShort()
            }

            val audioTrack = AudioTrack(
                AudioManager.STREAM_SYSTEM,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                numSamples * 2,
                AudioTrack.MODE_STATIC
            )
            
            audioTrack.write(buffer, 0, numSamples)
            audioTrack.play()
            
            Thread.sleep(280)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}

fun applyFilmFilterToUri(
    context: Context,
    uri: Uri,
    profile: FilmProfile,
    burnEnabled: Boolean,
    iso: Int,
    shutterSpeed: String,
    latitude: Double,
    longitude: Double,
    heading: Float,
    pitch: Float,
    frameIndex: Int,
    timestamp: Long,
    goodOldTimesEnabled: Boolean,
    activeFocusDistance: Float,
    subjectDistance: Float
) {
    try {
        val resolver = context.contentResolver
        
        // 1. Get rotation degrees from EXIF orientation
        var rotationDegrees = 0
        try {
            resolver.openInputStream(uri)?.use { inputStream ->
                val exif = android.media.ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
                rotationDegrees = when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Decode the raw bitmap
        val inputStream: java.io.InputStream? = resolver.openInputStream(uri)
        if (inputStream != null) {
            val options = android.graphics.BitmapFactory.Options()
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (originalBitmap != null) {
                // 3. Rotate bitmap if required based on EXIF
                val rotatedBitmap = if (rotationDegrees != 0) {
                    val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    val rot = android.graphics.Bitmap.createBitmap(
                        originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                    )
                    if (rot != originalBitmap) {
                        originalBitmap.recycle()
                    }
                    rot
                } else {
                    originalBitmap
                }

                // 3.5. Noctilux-M f/1.2 Spherical Aberration Bokeh / Depth-Of-Field Simulation
                val focusError = Math.abs(activeFocusDistance - subjectDistance)
                var processedBitmap = rotatedBitmap
                if (focusError > 0.12f) {
                    val blurIntensity = (focusError * 1.5f).coerceIn(0f, 10f)
                    // Vintage spherical bokeh: scale down, bilinear scale back up, then blend with original for f/1.2 halo!
                    val scale = (1.0f / (1.0f + blurIntensity * 1.8f)).coerceIn(0.04f, 0.45f)
                    val sw = (processedBitmap.width * scale).toInt().coerceAtLeast(16)
                    val sh = (processedBitmap.height * scale).toInt().coerceAtLeast(16)
                    
                    val scaledDown = android.graphics.Bitmap.createScaledBitmap(processedBitmap, sw, sh, true)
                    val blurred = android.graphics.Bitmap.createScaledBitmap(scaledDown, processedBitmap.width, processedBitmap.height, true)
                    scaledDown.recycle()
                    
                    // Noctilux signature f/1.2 glow: soft halo blend
                    val combined = android.graphics.Bitmap.createBitmap(processedBitmap.width, processedBitmap.height, android.graphics.Bitmap.Config.ARGB_8888)
                    val comboCanvas = android.graphics.Canvas(combined)
                    
                    // Draw blurred base (representing out of focus depth of field bokeh)
                    comboCanvas.drawBitmap(blurred, 0f, 0f, null)
                    blurred.recycle()
                    
                    // Blend a tiny fraction of sharp image (representing spherical aberration core glow)
                    val haloPaint = android.graphics.Paint().apply {
                        alpha = ((1.0f - (focusError / 10f).coerceIn(0f, 0.85f)) * 45).toInt().coerceAtLeast(10) // less sharp core as blur gets wider
                    }
                    comboCanvas.drawBitmap(processedBitmap, 0f, 0f, haloPaint)
                    
                    if (processedBitmap != rotatedBitmap) {
                        processedBitmap.recycle()
                    }
                    processedBitmap = combined
                }

                // 4. Create empty mutable bitmap of same rotated size to apply color matrix filter
                val mutableBitmap = android.graphics.Bitmap.createBitmap(
                    processedBitmap.width,
                    processedBitmap.height,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(mutableBitmap)
                val paint = android.graphics.Paint()

                val matrix = android.graphics.ColorMatrix()
                when (profile) {
                    FilmProfile.BW_TRI_X -> {
                        // Classic Leica Monochrom yellow/red filter emulation:
                        // Heavily weights Red channel (0.72) to darken skies, increase dynamic pop
                        // and make skin tones/stone highlight textures look dramatic and timeless.
                        val cr = 0.72f * (if (goodOldTimesEnabled) 1.45f else 1.35f) // Brand-new in 1950s gets slightly higher micro-contrast pop!
                        val cg = 0.23f * 1.35f
                        val cb = 0.05f * 1.35f
                        matrix.set(floatArrayOf(
                            cr, cg, cb, 0f, -12.0f,
                            cr, cg, cb, 0f, -12.0f,
                            cr, cg, cb, 0f, -12.0f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    }
                    FilmProfile.COL_PORTRA -> {
                        // Professional Portra 400 & Leica M Warm Emulsion signature:
                        // Red channel boosted for warm skin, G slightly desaturated to suppress harsh foliage yellows/greens,
                        // and soft reddish-magenta tint offsets injected in shadows for nostalgic analog warmth.
                        // 1950s pristine Kodachrome tone booster when Good Old Times is ON
                        if (goodOldTimesEnabled) {
                            matrix.set(floatArrayOf(
                                1.22f,  0.02f, -0.05f, 0f, 10.0f, // More vibrant red/warmth
                                0.03f,  1.08f, -0.03f, 0f,  4.0f,
                                -0.03f, 0.03f,  0.96f, 0f,  2.0f,
                                0f,     0f,     0f,    1f,  0f
                            ))
                        } else {
                            matrix.set(floatArrayOf(
                                1.16f,  0.03f, -0.04f, 0f,  8.0f,
                                0.04f,  1.04f, -0.02f, 0f,  3.0f,
                                -0.02f, 0.02f,  0.92f, 0f,  5.0f,
                                0f,     0f,     0f,    1f,  0f
                            ))
                        }
                    }
                }

                paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
                canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

                val w = mutableBitmap.width
                val h = mutableBitmap.height

                // 4.5. ADVANCED EMULATION: PROCEDURAL VIGNETTE (Noctilux/Summilux Optical Fall-off)
                val centerX = w / 2f
                val centerY = h / 2f
                val diagRadius = Math.hypot(w.toDouble(), h.toDouble()).toFloat() / 2f
                
                // Moody corner darkening adapted to the dynamic characteristics of the profile
                // Pristine 1950s lenses have slightly less vignette (better global illumination)
                val vignetteStrength = when (profile) {
                    FilmProfile.BW_TRI_X -> if (goodOldTimesEnabled) 0.30f else 0.42f
                    FilmProfile.COL_PORTRA -> if (goodOldTimesEnabled) 0.22f else 0.32f
                }
                val vignetteColors = intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.argb((vignetteStrength * 0.2f * 255).toInt(), 0, 0, 0),
                    android.graphics.Color.argb((vignetteStrength * 255).toInt(), 3, 2, 1) // Warm charcoal edge tint
                )
                val vignetteStops = floatArrayOf(0.35f, 0.72f, 1.0f)
                val vignettePaint = android.graphics.Paint().apply {
                    shader = android.graphics.RadialGradient(
                        centerX, centerY, diagRadius,
                        vignetteColors, vignetteStops,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    isAntiAlias = true
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vignettePaint)

                // 4.8. ADVANCED EMULATION: PROCEDURAL ORGANIC FILM GRAIN (Silver-Halide Simulation)
                val grainRandom = java.util.Random(timestamp)
                
                // Grain density scales adaptively with resolution and ISO
                val baseGrainCount = (w * h / 1000)
                val isoFactor = (iso.toFloat() / 400f).coerceIn(0.5f, 4.0f)
                val grainCount = (baseGrainCount * isoFactor).toInt().coerceIn(15000, 50000)
                
                val grainPaint = android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = false // Crisp particle structure
                }
                
                for (i in 0 until grainCount) {
                    val gx = grainRandom.nextFloat() * w
                    val gy = grainRandom.nextFloat() * h
                    
                    // Particle clumping increases with higher ISO levels
                    val sizeMultiplier = if (iso >= 1600) 2.0f else if (iso >= 800) 1.5f else 1.0f
                    // 1950s pristine film has finer, more uniform silver-halide grains!
                    val size = ((if (goodOldTimesEnabled) 0.4f else 0.5f) + grainRandom.nextFloat() * 1.5f) * sizeMultiplier
                    
                    // Sample pixel luminance to dynamically determine chemical grain response:
                    // Grains cluster strongly in midtones, fade in shadows (less light energy for crystallization),
                    // and are fully washed/bleached away in high-light clipping (fully exposed white).
                    val px = gx.toInt().coerceIn(0, w - 1)
                    val py = gy.toInt().coerceIn(0, h - 1)
                    val pixelColor = rotatedBitmap.getPixel(px, py)
                    val r = android.graphics.Color.red(pixelColor)
                    val g = android.graphics.Color.green(pixelColor)
                    val b = android.graphics.Color.blue(pixelColor)
                    val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                    
                    val luminanceWeight = if (luminance < 0.5f) {
                        0.15f + 0.85f * (luminance / 0.5f)
                    } else {
                        1.0f - 1.0f * ((luminance - 0.5f) / 0.5f)
                    }.coerceIn(0.01f, 1.0f)

                    // Emit 85% silver carbon grain clumps and 15% chemical gap highlights
                    val isLightGrain = grainRandom.nextFloat() < 0.15f
                    val baseAlpha = when (profile) {
                        FilmProfile.BW_TRI_X -> grainRandom.nextInt(18) + 4  // Textured B&W halide clumps
                        FilmProfile.COL_PORTRA -> grainRandom.nextInt(12) + 3 // Soft color Portra dye clouds
                    }
                    
                    val rawAlpha = (baseAlpha * (if (iso >= 1600) 1.4f else 1.0f))
                    // Pristine 1950s mode has slightly softer grain opacity (fine art grain)
                    val grainOpacityReduction = if (goodOldTimesEnabled) 0.75f else 1.0f
                    val alpha = (rawAlpha * luminanceWeight * grainOpacityReduction).toInt().coerceIn(1, 45)
                    val rgbColor = if (isLightGrain) 255 else 0
                    
                    grainPaint.color = android.graphics.Color.argb(alpha, rgbColor, rgbColor, rgbColor)
                    canvas.drawRect(gx, gy, gx + size, gy + size, grainPaint)
                }

                // 4.9. 70-YEAR VINTAGE DEGRADATION EFFECTS (If NOT in "Good Old Times" 1950s Mode)
                if (!goodOldTimesEnabled) {
                    val ageRandom = java.util.Random(timestamp + 12345)

                    // A. Warm amber/sepia retro patina representing decades of photo paper aging
                    val agedPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(38, 142, 92, 45) // classic warm brown amber patina
                        style = android.graphics.Paint.Style.FILL
                    }
                    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), agedPaint)

                    // B. Micro-Scratches on Emulsion (Simulating mechanical slide roller friction)
                    val scratchCount = ageRandom.nextInt(4) + 2 // 2 to 5 scratches
                    val scratchPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(45, 235, 235, 235) // Translucent light scratching
                        strokeWidth = 1f + ageRandom.nextFloat() * 1.8f
                        style = android.graphics.Paint.Style.STROKE
                        isAntiAlias = true
                    }
                    for (s in 0 until scratchCount) {
                        val path = android.graphics.Path()
                        val startX = ageRandom.nextFloat() * w
                        val startY = 0f
                        path.moveTo(startX, startY)
                        
                        var currX = startX
                        var currY = startY
                        val segments = 8
                        val segmentHeight = h.toFloat() / segments
                        for (seg in 1..segments) {
                            val nextY = seg * segmentHeight
                            val nextX = currX + (ageRandom.nextFloat() - 0.5f) * 16f // slight horizontal weave
                            path.lineTo(nextX, nextY)
                            currX = nextX
                            currY = nextY
                        }
                        canvas.drawPath(path, scratchPaint)
                    }

                    // C. Chemical Drying Stains / Water Spots (Degraded darkroom chemistry wash residues)
                    val stainCount = ageRandom.nextInt(3) + 2 // 2 to 4 stains
                    for (s in 0 until stainCount) {
                        val cx = ageRandom.nextFloat() * w
                        val cy = ageRandom.nextFloat() * h
                        val radius = 35f + ageRandom.nextFloat() * 55f
                        
                        val stainPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(16, 255, 255, 255) // faint white halo
                            style = android.graphics.Paint.Style.FILL
                            isAntiAlias = true
                        }
                        canvas.drawCircle(cx, cy, radius, stainPaint)
                        
                        val stainBorder = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(26, 255, 255, 255)
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 1.0f
                            isAntiAlias = true
                        }
                        canvas.drawCircle(cx, cy, radius, stainBorder)
                    }

                    // D. Organic Dust & Lint Particles
                    val dustCount = ageRandom.nextInt(6) + 6 // 6 to 11 specks
                    val dustPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(115, 12, 12, 12) // charcoal specks
                        style = android.graphics.Paint.Style.FILL
                        isAntiAlias = true
                    }
                    for (d in 0 until dustCount) {
                        val dx = ageRandom.nextFloat() * w
                        val dy = ageRandom.nextFloat() * h
                        val rx = 1.2f + ageRandom.nextFloat() * 3.5f
                        val ry = 1.2f + ageRandom.nextFloat() * 3.5f
                        canvas.drawOval(dx - rx, dy - ry, dx + rx, dy + ry, dustPaint)
                    }
                }

                // 5. Apply analog frame border and metadata imprint if burn is enabled
                if (burnEnabled) {
                    // Draw a subtle retro vignette frame
                    val borderPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = (w.coerceAtMost(h) * 0.02f)
                    }
                    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), borderPaint)

                    // Orange retro LED databack text style
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#FF5500") // Classic LED orange
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                        textSize = (w.coerceAtMost(h) * 0.022f).coerceAtLeast(18f)
                        // Add a subtle outer neon orange shadow glow
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.parseColor("#44FF3300"))
                    }

                    // Format timestamp to date string
                    val sdf = java.text.SimpleDateFormat("yy MM dd  HH:mm", java.util.Locale.US)
                    val formattedDate = sdf.format(java.util.Date(timestamp))

                    // Imprints
                    val line1 = "$formattedDate"
                    val line2 = "ISO $iso  $shutterSpeed  ${profile.label}"
                    val line3 = "GPS: ${String.format("%.4f", latitude)}N  ${String.format("%.4f", longitude)}E"
                    val line4 = "HDG: ${String.format("%.0f", heading)}° P: ${String.format("%.0f", pitch)}°  EXP-#${String.format("%02d", frameIndex)}"

                    val lineSpacing = textPaint.textSize * 1.3f
                    val startX = w * 0.05f
                    val startY = h - (lineSpacing * 4.5f)

                    // Draw background transparent dark box behind text for readability on bright photos
                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(120, 0, 0, 0)
                        style = android.graphics.Paint.Style.FILL
                    }
                    
                    // Robust maxOf evaluation ensures absolutely ZERO text bleeding in landscape or high resolutions
                    val textWidth = maxOf(
                        textPaint.measureText(line1),
                        textPaint.measureText(line2),
                        textPaint.measureText(line3),
                        textPaint.measureText(line4)
                    )
                    
                    canvas.drawRoundRect(
                        startX - 10f,
                        startY - lineSpacing + 5f,
                        startX + textWidth + 15f,
                        startY + (lineSpacing * 3.5f) + 10f,
                        10f, 10f,
                        bgPaint
                    )

                    // Draw imprints lines
                    canvas.drawText(line1, startX, startY, textPaint)
                    canvas.drawText(line2, startX, startY + lineSpacing, textPaint)
                    canvas.drawText(line3, startX, startY + (lineSpacing * 2f), textPaint)
                    canvas.drawText(line4, startX, startY + (lineSpacing * 3f), textPaint)
                }

                // 6. Save the processed bitmap back to the file
                val outputStream: java.io.OutputStream? = resolver.openOutputStream(uri, "w")
                if (outputStream != null) {
                    mutableBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
                if (processedBitmap != rotatedBitmap) {
                    processedBitmap.recycle()
                }
                rotatedBitmap.recycle()
                mutableBitmap.recycle()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun blendDoubleExposureBitmaps(context: android.content.Context, uri1: Uri, uri2: Uri): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val stream1 = context.contentResolver.openInputStream(uri1)
        val bitmap1 = BitmapFactory.decodeStream(stream1, null, options)
        stream1?.close()

        val stream2 = context.contentResolver.openInputStream(uri2)
        val bitmap2 = BitmapFactory.decodeStream(stream2, null, options)
        stream2?.close()

        if (bitmap1 == null || bitmap2 == null) return bitmap1 ?: bitmap2

        val width = bitmap1.width
        val height = bitmap1.height
        val mergedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(mergedBitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        canvas.drawBitmap(bitmap1, 0f, 0f, paint)
        paint.alpha = 127
        
        if (bitmap2.width == width && bitmap2.height == height) {
            canvas.drawBitmap(bitmap2, 0f, 0f, paint)
        } else {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap2, width, height, true)
            canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
            scaledBitmap.recycle()
        }

        bitmap1.recycle()
        bitmap2.recycle()
        mergedBitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun createSimulatedCaptureUri(context: android.content.Context, seedOffset: Int): Uri {
    val size = 1024
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    val random = java.util.Random(System.currentTimeMillis() + seedOffset)
    
    val skyColors = listOf(0xFF0F172A, 0xFF1E293B, 0xFF334155, 0xFF475569)
    val skyColor = skyColors[random.nextInt(skyColors.size)].toInt()
    canvas.drawColor(skyColor)
    
    paint.color = 0xFFFDE047.toInt()
    paint.alpha = 180
    val sunX = size * (0.3f + random.nextFloat() * 0.4f)
    val sunY = size * (0.3f + random.nextFloat() * 0.3f)
    val sunRadius = size * (0.1f + random.nextFloat() * 0.15f)
    canvas.drawCircle(sunX, sunY, sunRadius, paint)

    paint.color = 0xFF94A3B8.toInt()
    paint.alpha = 40
    for (i in 0 until 5) {
        val y = size * (0.5f + i * 0.08f)
        canvas.drawRect(0f, y, size.toFloat(), y + 30f, paint)
    }

    paint.color = 0xFF020617.toInt()
    paint.alpha = 255
    val path = android.graphics.Path()
    path.moveTo(0f, size.toFloat())
    val step = size / 8f
    path.lineTo(0f, size * 0.65f)
    for (i in 1..8) {
        val nextX = i * step
        val nextY = size * (0.55f + random.nextFloat() * 0.25f)
        path.lineTo(nextX, nextY)
    }
    path.lineTo(size.toFloat(), size.toFloat())
    path.close()
    canvas.drawPath(path, paint)

    paint.color = 0xFFFFFFFF.toInt()
    for (i in 0 until 5000) {
        val gx = random.nextFloat() * size
        val gy = random.nextFloat() * size
        paint.alpha = random.nextInt(25)
        canvas.drawRect(gx, gy, gx + 2f, gy + 2f, paint)
    }

    val file = File(context.cacheDir, "blackhole_sim_${System.currentTimeMillis()}_$seedOffset.jpg")
    try {
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    bitmap.recycle()
    return Uri.fromFile(file)
}

fun redevelopJpegFile(
    context: android.content.Context,
    uri: Uri,
    contrast: Float,
    exposureOffset: Float,
    grainDensity: Float
) {
    try {
        val stream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(stream)
        stream?.close() ?: return

        if (original == null) return

        val width = original.width
        val height = original.height
        val developed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(developed)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        val o = exposureOffset * 2.55f
        val colorMatrix = android.graphics.ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, o,
            0f, contrast, 0f, 0f, o,
            0f, 0f, contrast, 0f, o,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)
        paint.colorFilter = null

        val random = java.util.Random()
        val grainCount = (25000 * grainDensity).toInt().coerceAtMost(100000)
        paint.style = android.graphics.Paint.Style.FILL
        for (i in 0 until grainCount) {
            val gx = random.nextFloat() * width
            val gy = random.nextFloat() * height
            val size = 1f + random.nextFloat() * 3f
            
            val px = gx.toInt().coerceIn(0, width - 1)
            val py = gy.toInt().coerceIn(0, height - 1)
            val pixelColor = developed.getPixel(px, py)
            val r = android.graphics.Color.red(pixelColor)
            val g = android.graphics.Color.green(pixelColor)
            val b = android.graphics.Color.blue(pixelColor)
            val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            
            val luminanceWeight = if (luminance < 0.5f) {
                0.15f + 0.85f * (luminance / 0.5f)
            } else {
                1.0f - 1.0f * ((luminance - 0.5f) / 0.5f)
            }.coerceIn(0.01f, 1.0f)
            
            val isDark = random.nextFloat() > 0.15f
            val baseAlpha = 26 // Approx 10% opacity (0x1A)
            val finalAlpha = (baseAlpha * luminanceWeight).toInt().coerceIn(2, 255)
            
            paint.color = if (isDark) {
                android.graphics.Color.argb(finalAlpha, 0, 0, 0)
            } else {
                android.graphics.Color.argb(finalAlpha, 255, 255, 255)
            }
            canvas.drawRect(gx, gy, gx + size, gy + size, paint)
        }

        context.contentResolver.openOutputStream(uri, "w")?.use { os ->
            developed.compress(Bitmap.CompressFormat.JPEG, 90, os)
            os.flush()
        }

        original.recycle()
        developed.recycle()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun triggerHapticFeedback(context: android.content.Context) {
    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(30, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
}
