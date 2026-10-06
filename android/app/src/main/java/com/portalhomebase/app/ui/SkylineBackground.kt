package com.portalhomebase.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.portalhomebase.app.ui.theme.LocalIsLight
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// Temporary storm-check override; null means live weather. Flip for a
// screenshot, verify, flip back — never ship non-null.
private val DEBUG_WEATHER: String? = null

// Hand-drawn animated Austin skyline from across Lady Bird Lake, in a warm
// flat-poster style: the Independent, 360 tower, Texas Capitol dome, Frost
// Bank crown, Austonian, Google sail, Congress bridge bats. Day <-> night
// crossfades with the theme, and the sky reacts to the live weather feed.
private data class Slab(val x0: Float, val x1: Float, val h: Float, val day: Long, val night: Long)
private data class Puff(val bx: Float, val y: Float, val s: Float)
private data class Mote(val x: Float, val y: Float, val r: Float, val phase: Float)
private data class Flier(val bx: Float, val y: Float, val speed: Float, val phase: Float, val s: Float)
private data class Craft(val bx: Float, val y: Float, val speed: Float, val dir: Float, val kayak: Boolean, val phase: Float, val hull: Long)

// Back-layer fillers in warm poster hues; landmarks are drawn separately.
private val BackSlabs = listOf(
    Slab(0.000f, 0.050f, 0.090f, 0xFFC98F6B, 0xFF232032),
    Slab(0.130f, 0.180f, 0.130f, 0xFFA8563E, 0xFF2A222E),
    Slab(0.255f, 0.305f, 0.150f, 0xFFD9B36A, 0xFF2B2430),
    Slab(0.310f, 0.350f, 0.190f, 0xFF8A4A3A, 0xFF241F2C),
    Slab(0.425f, 0.475f, 0.120f, 0xFF5E7A72, 0xFF20262E),
    Slab(0.535f, 0.585f, 0.160f, 0xFFC05B3A, 0xFF28212E),
    Slab(0.590f, 0.640f, 0.110f, 0xFFD9C08A, 0xFF2A2532),
    Slab(0.645f, 0.690f, 0.130f, 0xFF96502E, 0xFF251F2B),
    Slab(0.695f, 0.765f, 0.170f, 0xFFB4764E, 0xFF262130),
    Slab(0.838f, 0.860f, 0.130f, 0xFF7A5A48, 0xFF221F2C),
    Slab(0.972f, 1.005f, 0.170f, 0xFFA8563E, 0xFF28222E),
)
private val FrontSlabs = listOf(
    Slab(0.015f, 0.085f, 0.065f, 0xFF6E4A36, 0xFF171219),
    Slab(0.095f, 0.145f, 0.105f, 0xFF5C3E30, 0xFF131017),
    Slab(0.160f, 0.225f, 0.055f, 0xFF75523C, 0xFF171219),
    Slab(0.245f, 0.295f, 0.085f, 0xFF63452F, 0xFF131017),
    Slab(0.325f, 0.395f, 0.065f, 0xFF6E4A36, 0xFF171219),
    Slab(0.415f, 0.475f, 0.095f, 0xFF5C3E30, 0xFF131017),
    Slab(0.495f, 0.550f, 0.060f, 0xFF75523C, 0xFF171219),
    Slab(0.565f, 0.610f, 0.085f, 0xFF63452F, 0xFF131017),
    Slab(0.660f, 0.705f, 0.070f, 0xFF6E4A36, 0xFF171219),
    Slab(0.720f, 0.775f, 0.100f, 0xFF5C3E30, 0xFF131017),
    Slab(0.805f, 0.865f, 0.055f, 0xFF75523C, 0xFF171219),
    Slab(0.875f, 0.935f, 0.085f, 0xFF63452F, 0xFF131017),
    Slab(0.945f, 1.000f, 0.065f, 0xFF6E4A36, 0xFF171219),
)
// Landmark footprints (x0, x1, height above the shore). Board cards cover the
// middle band, so the stars (Capitol, Frost, Austonian) stand in the open
// right strip; the rest of the parade shows on Week/Home and in card gaps.
private const val IND_X0 = 0.055f; private const val IND_X1 = 0.125f; private const val IND_H = 0.300f
private const val IND_DAY = 0xFFC05B3A; private const val IND_NIGHT = 0xFF241E2E
private const val SAIL_X0 = 0.185f; private const val SAIL_X1 = 0.250f; private const val SAIL_H = 0.260f
private const val SAIL_DAY = 0xFF9E3B30; private const val SAIL_NIGHT = 0xFF221D2B
private const val CAP_CX = 0.885f
private const val FROST_X0 = 0.770f; private const val FROST_X1 = 0.835f; private const val FROST_H = 0.270f
private const val FROST_DAY = 0xFFEAD9B0; private const val FROST_NIGHT = 0xFF2E2A33
private const val AUST_X0 = 0.915f; private const val AUST_X1 = 0.970f; private const val AUST_H = 0.285f
private const val AUST_DAY = 0xFFC2692C; private const val AUST_NIGHT = 0xFF2A2129
private const val GSAIL_X0 = 0.355f; private const val GSAIL_X1 = 0.420f; private const val GSAIL_H = 0.215f
private const val GSAIL_DAY = 0xFF4E7A72; private const val GSAIL_NIGHT = 0xFF1E242C
private const val ONEC_X0 = 0.480f; private const val ONEC_X1 = 0.530f; private const val ONEC_H = 0.150f
private const val ONEC_DAY = 0xFFD99A2B; private const val ONEC_NIGHT = 0xFF2A231F

private val Clouds = listOf(
    Puff(0.05f, 0.13f, 1.25f), Puff(0.44f, 0.07f, 0.90f),
    Puff(0.66f, 0.19f, 1.45f), Puff(0.88f, 0.09f, 0.75f),
)
private val Birds = listOf(
    Flier(0.30f, 0.16f, 1.0f, 0.0f, 15f),
    Flier(0.52f, 0.24f, 0.7f, 0.4f, 11f),
    Flier(0.64f, 0.11f, 1.3f, 0.7f, 13f),
)

private enum class SkyWeather { CLEAR, OVERCAST, RAIN, STORM, SNOW, FOG }

private fun skyWeatherOf(condition: String?): SkyWeather {
    if (condition.isNullOrBlank()) return SkyWeather.CLEAR
    val s = condition.lowercase()
    return when {
        s.contains("storm") || s.contains("thunder") -> SkyWeather.STORM
        s.contains("snow") || s.contains("sleet") || s.contains("hail") ||
            s.contains("grain") || s.contains("icy") -> SkyWeather.SNOW
        s.contains("rain") || s.contains("drizzle") || s.contains("shower") ||
            s.contains("freezing") -> SkyWeather.RAIN
        s.contains("fog") || s.contains("mist") -> SkyWeather.FOG
        s.contains("overcast") || s.contains("cloud") -> SkyWeather.OVERCAST
        else -> SkyWeather.CLEAR
    }
}

private fun overcastOf(wx: SkyWeather): Float = when (wx) {
    SkyWeather.CLEAR -> 0f
    SkyWeather.OVERCAST -> 0.55f
    SkyWeather.FOG -> 0.60f
    SkyWeather.SNOW -> 0.70f
    SkyWeather.RAIN -> 0.78f
    SkyWeather.STORM -> 0.90f
}

private fun wrap01(x: Float): Float = ((x % 1f) + 1f) % 1f

@Composable
fun SkylineBackground(condition: String?) {
    val light = LocalIsLight.current
    val wx = skyWeatherOf(DEBUG_WEATHER ?: condition)
    val mix by animateFloatAsState(
        targetValue = if (light) 0f else 1f,
        animationSpec = tween(durationMillis = 2500),
        label = "nightMix",
    )
    // Weather fronts roll in smoothly instead of popping.
    val over by animateFloatAsState(overcastOf(wx), tween(3000), label = "overcast")
    val rainMix by animateFloatAsState(
        if (wx == SkyWeather.RAIN || wx == SkyWeather.STORM) 1f else 0f,
        tween(2500), label = "rainMix",
    )
    val snowMix by animateFloatAsState(
        if (wx == SkyWeather.SNOW) 1f else 0f, tween(2500), label = "snowMix",
    )
    val fogMix by animateFloatAsState(
        if (wx == SkyWeather.FOG) 1f else 0f, tween(3000), label = "fogMix",
    )
    val stormMix by animateFloatAsState(
        if (wx == SkyWeather.STORM) 1f else 0f, tween(2000), label = "stormMix",
    )
    val loop = rememberInfiniteTransition(label = "skyline")
    // Explicit Float type args: K2 cannot infer through nested infiniteRepeatable(tween()).
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(140000, easing = LinearEasing)), label = "drift")
    val twinkle by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(4500, easing = LinearEasing)), label = "twinkle")
    val batT by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(34000, easing = LinearEasing)), label = "batT")
    val birdT by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(70000, easing = LinearEasing)), label = "birdT")
    val wingT by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(1100, easing = LinearEasing)), label = "wingT")
    val shimmer by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(5200, easing = LinearEasing)), label = "shimmer")
    val glowT by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(8000, easing = LinearEasing)), label = "glowT")
    val rainT by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(800, easing = LinearEasing)), label = "rainT")
    val snowT by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(14000, easing = LinearEasing)), label = "snowT")
    val stormT by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(11000, easing = LinearEasing)), label = "stormT")
    val fogT by loop.animateFloat(0f, 1f, infiniteRepeatable(tween<Float>(60000, easing = LinearEasing)), label = "fogT")

    val stars = remember {
        val rnd = Random(42)
        List(90) { Mote(rnd.nextFloat(), 0.02f + rnd.nextFloat() * 0.58f, 1f + rnd.nextFloat() * 1.8f, rnd.nextFloat()) }
    }
    val sparkles = remember {
        val rnd = Random(11)
        List(26) { Mote(rnd.nextFloat(), 0.815f + rnd.nextFloat() * 0.165f, 8f + rnd.nextFloat() * 22f, rnd.nextFloat()) }
    }
    val bats = remember {
        val rnd = Random(5)
        List(8) { Flier(0f, 0f, 0.75f + rnd.nextFloat() * 0.6f, rnd.nextFloat(), 8f + (it % 3) * 2.5f) }
    }
    // Day-only paddlers: nobody is on the water at night (or in a storm).
    val crafts = listOf(
        Craft(0.10f, 0.880f, 0.10f, 1f, true, 0.0f, 0xFFE07856),
        Craft(0.55f, 0.920f, 0.07f, -1f, false, 0.4f, 0xFF3E8E8A),
        Craft(0.80f, 0.900f, 0.12f, 1f, false, 0.7f, 0xFFE0A83C),
    )
    val drops = remember {
        val rnd = Random(23)
        List(100) { Mote(rnd.nextFloat(), rnd.nextFloat(), 14f + rnd.nextFloat() * 14f, rnd.nextFloat()) }
    }
    val flakes = remember {
        val rnd = Random(31)
        List(70) { Mote(rnd.nextFloat(), rnd.nextFloat(), 2f + rnd.nextFloat() * 2.5f, rnd.nextFloat()) }
    }
    val trees = remember {
        val rnd = Random(17)
        List(48) { Mote(rnd.nextFloat() * 1.02f - 0.01f, if (it % 2 == 0) 0f else 1f, 18f + rnd.nextFloat() * 12f, rnd.nextFloat()) }
    }
    val pines = remember {
        val rnd = Random(29)
        List(14) { Mote(rnd.nextFloat() * 1.02f - 0.01f, 0f, 60f + rnd.nextFloat() * 50f, rnd.nextFloat()) }
    }
    val windows = remember {
        // Fraction-space window rects over slabs and landmark shafts.
        val rnd = Random(9)
        val out = mutableListOf<Triple<Offset, Size, Float>>()
        fun gen(x0: Float, x1: Float, h: Float, base: Float, prob: Float, ww: Float, hh: Float) {
            val cols = ((x1 - x0) * 100 / 1.4f).toInt().coerceAtLeast(2)
            val rows = (h * 100 / 2.6f).toInt().coerceAtLeast(2)
            for (c in 0 until cols) for (r in 0 until rows) {
                if (rnd.nextFloat() > prob) continue
                out.add(
                    Triple(
                        Offset(x0 + (x1 - x0) * (c + 0.5f) / cols, base - h + h * (r + 0.5f) / rows),
                        Size(ww, hh), rnd.nextFloat(),
                    ),
                )
            }
        }
        for (b in FrontSlabs) gen(b.x0, b.x1, b.h, 0.80f, 0.55f, 0.0035f, 0.008f)
        for (b in BackSlabs) gen(b.x0, b.x1, b.h, 0.80f, 0.30f, 0.0028f, 0.006f)
        gen(IND_X0, IND_X1, 0.22f, 0.80f, 0.35f, 0.0028f, 0.006f)
        gen(FROST_X0, FROST_X1, FROST_H, 0.80f, 0.35f, 0.0028f, 0.006f)
        gen(AUST_X0, AUST_X1, AUST_H, 0.80f, 0.35f, 0.0028f, 0.006f)
        out
    }

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.80f
        val t = mix
        // Day/night building tone, grayed slightly as weather rolls in.
        fun bcol(day: Long, night: Long): Color = lerp(
            lerp(Color(day), Color(night), t),
            lerp(Color(0xFF8E99AC), Color(0xFF232B44), t),
            over * 0.35f,
        )

        // --- Sky (warm cream by day, grayed by incoming weather) ---
        val gray = over * 0.85f
        drawRect(
            Brush.verticalGradient(
                listOf(
                    lerp(lerp(Color(0xFFE7D8BC), Color(0xFF05081A), t), lerp(Color(0xFF93A3B5), Color(0xFF131A2C), t), gray),
                    lerp(lerp(Color(0xFFF1E5CB), Color(0xFF101838), t), lerp(Color(0xFFA8B2BC), Color(0xFF1A2233), t), gray),
                    lerp(lerp(Color(0xFFF7E0B8), Color(0xFF43304A), t), lerp(Color(0xFFB9B3A4), Color(0xFF232030), t), gray),
                ),
            ),
        )
        val dim = 1 - over * 0.9f

        // --- Stars ---
        if (t > 0.02f && dim > 0.03f) {
            for (s in stars) {
                val a = t * dim * (0.25f + 0.75f * abs(sin((twinkle + s.phase) * 2 * PI).toFloat()))
                drawCircle(Color.White.copy(alpha = a.coerceIn(0f, 1f)), s.r, Offset(s.x * w, s.y * h))
            }
        }

        // --- Sun / moon (same perch, crossfading) ---
        val orb = Offset(w * 0.386f, h * 0.20f)
        val orbR = size.minDimension * 0.042f
        val breathe = 0.85f + 0.15f * sin(glowT * 2 * PI).toFloat()
        if (t < 0.98f && (1 - t) * dim > 0.03f) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFFFE9A8).copy(alpha = 0.55f * (1 - t) * dim * breathe), Color.Transparent),
                    center = orb, radius = orbR * 3.2f,
                ),
                orbR * 3.2f, orb,
            )
            drawCircle(Color(0xFFFFC94D).copy(alpha = ((1 - t) * dim).coerceIn(0f, 1f)), orbR, orb)
        }
        if (t > 0.02f && t * dim > 0.03f) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFB9C8FF).copy(alpha = 0.45f * t * dim * breathe), Color.Transparent),
                    center = orb, radius = orbR * 3.0f,
                ),
                orbR * 3.0f, orb,
            )
            drawCircle(Color(0xFFF5F2E6).copy(alpha = (t * dim).coerceIn(0f, 1f)), orbR * 0.92f, orb)
            drawCircle(
                lerp(Color(0xFFF1E5CB), Color(0xFF101838), t).copy(alpha = 0.35f * t * dim),
                orbR * 0.20f, orb + Offset(-orbR * 0.3f, -orbR * 0.2f),
            )
            drawCircle(
                lerp(Color(0xFFF1E5CB), Color(0xFF101838), t).copy(alpha = 0.30f * t * dim),
                orbR * 0.13f, orb + Offset(orbR * 0.25f, orbR * 0.3f),
            )
        }

        // --- Clouds (thicken + darken with weather) ---
        val cloudCol = lerp(
            lerp(Color.White, Color(0xFF2B3556), t),
            lerp(Color(0xFF8E99AC), Color(0xFF202842), t),
            over * 0.7f,
        ).copy(alpha = (0.90f - 0.45f * t + over * 0.25f).coerceAtMost(1f))
        for (c in Clouds) {
            val cx = (wrap01(c.bx + drift * 0.22f) * 1.3f - 0.15f) * w
            val cy = c.y * h
            val r = size.minDimension * 0.026f * c.s * (1 + over * 0.35f)
            drawCircle(cloudCol, r, Offset(cx, cy))
            drawCircle(cloudCol, r * 0.78f, Offset(cx + r * 1.05f, cy - r * 0.42f))
            drawCircle(cloudCol, r * 0.85f, Offset(cx + r * 2.0f, cy + r * 0.10f))
            drawCircle(cloudCol, r * 0.70f, Offset(cx + r * 1.0f, cy + r * 0.32f))
        }

        // --- Day birds (grounded by bad weather) ---
        if (t < 0.98f && (1 - t) * (1 - over) > 0.05f) {
            val birdCol = Color(0xFF6B5A48).copy(alpha = (0.75f * (1 - t) * (1 - over)).coerceIn(0f, 1f))
            for ((i, b) in Birds.withIndex()) {
                val cx = (wrap01(b.bx + birdT * 0.18f * b.speed) * 1.3f - 0.15f) * w
                val cy = b.y * h + sin((birdT * 6 + b.phase) * 2 * PI).toFloat() * h * 0.006f
                val flap = 0.6f + 0.4f * sin((wingT * 2 + b.phase) * 2 * PI).toFloat()
                val s = b.s
                val path = Path().apply {
                    moveTo(cx - s, cy - s * 0.5f * flap)
                    quadraticTo(cx - s / 2, cy + s * 0.35f, cx, cy)
                    quadraticTo(cx + s / 2, cy + s * 0.35f, cx + s, cy - s * 0.5f * flap)
                }
                drawPath(path, birdCol, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            }
        }

        // --- Skyline ---
        for (b in BackSlabs) drawSlab(b, horizon, w, h, bcol(b.day, b.night))

        // The Independent: stacked Jenga crown.
        run {
            val ind = bcol(IND_DAY, IND_NIGHT)
            val x0 = IND_X0 * w; val x1 = IND_X1 * w
            val shaftTop = horizon - 0.22f * h
            drawRect(ind, Offset(x0, shaftTop), Size(x1 - x0, 0.22f * h))
            val stepH = h * 0.02f
            val offs = listOf(-0.010f, 0.008f, -0.006f, 0.004f)
            for ((i, o) in offs.withIndex()) {
                val sy = shaftTop - stepH * (i + 1)
                drawRect(ind, Offset(x0 + o * w, sy), Size(x1 - x0, stepH - 1.5f))
            }
            val capY = shaftTop - stepH * 4
            drawPath(
                Path().apply {
                    moveTo(x0 - 0.006f * w, capY)
                    lineTo(x1 + 0.002f * w, capY)
                    lineTo(x1 - 0.010f * w, capY - h * 0.018f)
                    lineTo(x0 - 0.002f * w, capY - h * 0.018f)
                    close()
                },
                ind,
            )
        }
        // 360 tower: curved sail fin.
        run {
            val x0 = SAIL_X0 * w; val x1 = SAIL_X1 * w
            val top = horizon - SAIL_H * h
            drawPath(
                Path().apply {
                    moveTo(x0, horizon)
                    lineTo(x1, horizon)
                    quadraticTo(x1 - 4f, horizon - 0.16f * h, x0 + (x1 - x0) * 0.66f, top)
                    quadraticTo(x0 + (x1 - x0) * 0.44f, horizon - 0.13f * h, x0, horizon)
                    close()
                },
                bcol(SAIL_DAY, SAIL_NIGHT),
            )
        }
        // Texas Capitol: pink granite + slate dome by day, floodlit by night.
        run {
            val cx = CAP_CX * w
            val base = horizon - 0.008f * h
            val capCol = lerp(Color(0xFFC08E7E), Color(0xFFF5DEB0), t).copy(alpha = 0.85f + 0.15f * t)
            val domeCol = lerp(Color(0xFF5E7386), Color(0xFFE8D9B0), t).copy(alpha = 0.9f + 0.1f * t)
            if (t > 0.05f) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFD9A0).copy(alpha = 0.45f * t), Color.Transparent),
                        center = Offset(cx, base - 0.035f * h), radius = w * 0.055f,
                    ),
                    w * 0.055f, Offset(cx, base - 0.035f * h),
                )
            }
            val bw = 0.032f * w
            drawRect(capCol, Offset(cx - bw, base - 0.042f * h), Size(bw * 2, 0.042f * h))
            drawRect(capCol, Offset(cx - bw - 0.008f * w, base - 0.028f * h), Size(0.008f * w, 0.028f * h))
            drawRect(capCol, Offset(cx + bw, base - 0.028f * h), Size(0.008f * w, 0.028f * h))
            val dr = 0.012f * w
            val drumTop = base - 0.060f * h
            drawRect(capCol, Offset(cx - dr, drumTop), Size(dr * 2, 0.018f * h))
            drawPath(
                Path().apply {
                    moveTo(cx - dr, drumTop)
                    quadraticTo(cx - dr * 0.9f, drumTop - 0.030f * h, cx, drumTop - 0.034f * h)
                    quadraticTo(cx + dr * 0.9f, drumTop - 0.030f * h, cx + dr, drumTop)
                    close()
                },
                domeCol,
            )
            drawLine(domeCol, Offset(cx, drumTop - 0.034f * h), Offset(cx, drumTop - 0.044f * h), strokeWidth = 3f)
            drawCircle(domeCol, 2.5f, Offset(cx, drumTop - 0.045f * h))
        }
        // Frost Bank Tower: cream shaft with ribs, stepped crown, glowing cyan by night.
        run {
            val fx0 = FROST_X0 * w; val fx1 = FROST_X1 * w
            val top = horizon - FROST_H * h
            val frost = bcol(FROST_DAY, FROST_NIGHT)
            drawRect(frost, Offset(fx0, top), Size(fx1 - fx0, FROST_H * h))
            if (t < 0.5f) {
                val rib = Color(0xFFC9B183).copy(alpha = 0.8f * (1 - t * 2))
                for (i in 1..4) {
                    val rx = fx0 + (fx1 - fx0) * i / 5
                    drawLine(rib, Offset(rx, top + 8f), Offset(rx, horizon), strokeWidth = 3f)
                }
            }
            drawCircle(
                lerp(Color(0xFF8A6F3C), Color(0xFFFFD9A0), t),
                6f, Offset((fx0 + fx1) / 2, top + 40f),
            )
            val crown = lerp(frost, Color(0xFF6FB6E8), t * 0.85f)
            drawRect(crown, Offset(fx0 + (fx1 - fx0) * 0.12f, top - 14f), Size((fx1 - fx0) * 0.76f, 14f))
            val pw = (fx1 - fx0) * 0.20f
            drawRect(crown, Offset(fx0 + (fx1 - fx0) * 0.18f, top - 30f), Size(pw, 17f))
            drawRect(crown, Offset(fx1 - (fx1 - fx0) * 0.18f - pw, top - 30f), Size(pw, 17f))
        }
        // Austonian: tallest slab + setback cap.
        run {
            val aust = bcol(AUST_DAY, AUST_NIGHT)
            val x0 = AUST_X0 * w; val x1 = AUST_X1 * w
            val top = horizon - AUST_H * h
            drawRect(aust, Offset(x0, top), Size(x1 - x0, AUST_H * h))
            drawRect(aust, Offset(x0 + (x1 - x0) * 0.18f, top - 12f), Size((x1 - x0) * 0.64f, 12f))
        }
        // Google sail: leaning tapered slab.
        run {
            val x0 = GSAIL_X0 * w; val x1 = GSAIL_X1 * w
            val top = horizon - GSAIL_H * h
            drawPath(
                Path().apply {
                    moveTo(x0, horizon)
                    lineTo(x1, horizon)
                    lineTo(x1 - 0.018f * w, top)
                    lineTo(x0 + 0.006f * w, top)
                    close()
                },
                bcol(GSAIL_DAY, GSAIL_NIGHT),
            )
        }
        // One Congress Plaza: stepped pyramid cap.
        run {
            val one = bcol(ONEC_DAY, ONEC_NIGHT)
            val x0 = ONEC_X0 * w; val x1 = ONEC_X1 * w
            val top = horizon - ONEC_H * h
            drawRect(one, Offset(x0, top), Size(x1 - x0, ONEC_H * h))
            var sy = top
            var inset = 0.06f
            repeat(3) {
                sy -= 9f
                drawRect(one, Offset(x0 + (x1 - x0) * inset, sy), Size((x1 - x0) * (1 - inset * 2), 9f))
                inset += 0.10f
            }
        }
        for (b in FrontSlabs) drawSlab(b, horizon, w, h, bcol(b.day, b.night))

        // Lit windows.
        if (t > 0.05f) {
            for ((pos, sz, phase) in windows) {
                val a = t * (0.55f + 0.45f * sin((shimmer * 2 + phase) * 2 * PI).toFloat())
                if (a < 0.08f) continue
                drawRect(
                    Color(0xFFFFC46B).copy(alpha = a.coerceIn(0f, 1f)),
                    Offset(pos.x * w, pos.y * h), Size(sz.width * w, sz.height * h),
                )
            }
        }

        // --- Town Lake ---
        drawRect(
            Brush.verticalGradient(
                listOf(
                    lerp(Color(0xFFB4D0C9), Color(0xFF101A30), t),
                    lerp(Color(0xFF8FB0AA), Color(0xFF060A16), t),
                ),
            ),
            Offset(0f, horizon), Size(w, h - horizon),
        )
        // Sun/moon glint column + scattered shimmer.
        val glint = lerp(Color(0xFFFFE9A8), Color(0xFFD8E4FF), t)
        val glintDim = 1 - over * 0.7f
        for (r in 0 until 8) {
            val fy = horizon + (h - horizon) * (0.06f + r * 0.115f)
            val half = 8f + r * 5.5f
            val a = (0.5f - r * 0.035f) * glintDim * (0.6f + 0.4f * sin((shimmer * 3 + r * 0.37f) * 2 * PI).toFloat())
            if (a <= 0.03f) continue
            drawRect(glint.copy(alpha = a.coerceIn(0f, 1f)), Offset(w * 0.386f - half, fy), Size(half * 2, 3.5f))
        }
        val sparkCol = lerp(Color.White, Color(0xFF8FA5D6), t)
        for (s in sparkles) {
            val a = (0.45f - 0.15f * t) * (0.5f + 0.5f * sin((shimmer * 2 + s.phase) * 2 * PI).toFloat())
            if (a <= 0.03f) continue
            drawRect(sparkCol.copy(alpha = a), Offset(s.x * w - s.r / 2, s.y * h), Size(s.r, 3f))
        }

        // --- Paddlers (day only) ---
        run {
            val vis = (1 - t) * (1 - over)
            if (vis > 0.03f) {
                val person = Color(0xFF4A3428).copy(alpha = vis)
                for (c in crafts) {
                    val cx = (wrap01(c.bx + drift * c.speed * c.dir) * 1.2f - 0.1f) * w
                    val cy = c.y * h + sin((shimmer * 2 + c.phase) * 2 * PI).toFloat() * 3f
                    val stroke = sin((batT * 8 + c.phase) * 2 * PI).toFloat()
                    val hull = Color(c.hull).copy(alpha = vis)
                    drawOval(
                        Color.White.copy(alpha = 0.20f * vis),
                        Offset(cx - c.dir * 40f - 18f, cy - 2f), Size(36f, 6f),
                    )
                    if (c.kayak) {
                        drawOval(hull, Offset(cx - 30f, cy - 4f), Size(60f, 9f))
                        drawOval(person, Offset(cx - 8f, cy - 5f), Size(16f, 7f))
                        drawCircle(person, 4f, Offset(cx, cy - 12f))
                        val tilt = stroke * 5f
                        drawLine(person, Offset(cx - 20f, cy - 14f + tilt), Offset(cx + 20f, cy - 14f - tilt), strokeWidth = 2.5f, cap = StrokeCap.Round)
                    } else {
                        drawOval(hull, Offset(cx - 24f, cy - 5f), Size(48f, 11f))
                        val px = cx - c.dir * 2f
                        drawLine(person, Offset(px, cy - 6f), Offset(px, cy - 20f), strokeWidth = 5f, cap = StrokeCap.Round)
                        drawCircle(person, 4.2f, Offset(px, cy - 24f))
                        drawLine(
                            person, Offset(px + c.dir * 4f, cy - 26f),
                            Offset(px + c.dir * 14f + stroke * 6f, cy + 4f),
                            strokeWidth = 2.5f, cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }

        // --- Shoreline treeline (swaying oaks + pines) ---
        run {
            val backTree = bcol(0xFF4E3A2C, 0xFF131A16)
            val frontTree = bcol(0xFF3A2A20, 0xFF0C120E)
            val trunk = bcol(0xFF2E2118, 0xFF090D0A)
            // Thin dark shore grounds every trunk.
            drawRect(frontTree, Offset(0f, horizon + 2f), Size(w, 12f))
            fun swayOf(phase: Float) = sin((shimmer * 2 + phase) * 2 * PI).toFloat() * 2.5f
            // Back oaks: trunk stays planted, canopy sways.
            for (tr in trees) {
                if (tr.y != 0f) continue
                val base = tr.x * w
                val cx = base + swayOf(tr.phase) * 1.6f
                val cy = horizon - 34f
                drawRect(trunk, Offset(base - 4f, cy + 8f), Size(8f, horizon + 10f - cy - 8f))
                drawCircle(backTree, tr.r, Offset(cx, cy))
                drawCircle(backTree, tr.r * 0.62f, Offset(cx - tr.r * 0.55f, cy + tr.r * 0.35f))
                drawCircle(backTree, tr.r * 0.62f, Offset(cx + tr.r * 0.55f, cy + tr.r * 0.35f))
            }
            // Pines: trunk planted, tops bend with height.
            for (p in pines) {
                val base = p.x * w
                val lean = swayOf(p.phase)
                val baseY = horizon + 8f
                val hp = p.r
                drawRect(trunk, Offset(base - 3.5f, baseY - 12f), Size(7f, 16f))
                drawPath(
                    Path().apply {
                        moveTo(base - 17f, baseY - 8f); lineTo(base + 17f, baseY - 8f)
                        lineTo(base + lean * 0.5f, baseY - 8f - hp * 0.55f); close()
                    },
                    backTree,
                )
                drawPath(
                    Path().apply {
                        moveTo(base - 12f + lean * 0.5f, baseY - 8f - hp * 0.38f)
                        lineTo(base + 12f + lean * 0.5f, baseY - 8f - hp * 0.38f)
                        lineTo(base + lean * 1.2f, baseY - 8f - hp); close()
                    },
                    backTree,
                )
            }
            // Front oaks over everything.
            for (tr in trees) {
                if (tr.y == 0f) continue
                val base = tr.x * w
                val cx = base + swayOf(tr.phase) * 1.6f
                val cy = horizon - 26f
                drawRect(trunk, Offset(base - 5f, cy + 8f), Size(10f, horizon + 12f - cy - 8f))
                drawCircle(frontTree, tr.r, Offset(cx, cy))
                drawCircle(frontTree, tr.r * 0.62f, Offset(cx - tr.r * 0.55f, cy + tr.r * 0.35f))
                drawCircle(frontTree, tr.r * 0.62f, Offset(cx + tr.r * 0.55f, cy + tr.r * 0.35f))
            }
        }

        // --- Congress bridge (distant, left) + bats ---
        run {
            val bridge = bcol(0xFF5C3E30, 0xFF0A0E1E)
            val deckY = h * 0.775f
            drawRect(bridge, Offset(w * 0.03f, deckY), Size(w * 0.17f, h * 0.012f))
            for (i in 0 until 4) {
                val px = w * (0.045f + i * 0.042f)
                drawRect(bridge, Offset(px, deckY), Size(w * 0.009f, horizon - deckY + 8f))
            }
            drawRect(bridge, Offset(w * 0.03f, deckY - h * 0.008f), Size(w * 0.17f, 2.5f))
            if (t > 0.05f) {
                for (i in 0 until 6) {
                    val lx = w * (0.04f + i * 0.03f)
                    drawCircle(Color(0xFFFFC46B).copy(alpha = 0.9f * t), 3f, Offset(lx, deckY - h * 0.008f))
                }
            }
        }
        if (t > 0.02f) {
            val batCol = Color(0xFF04060D)
            val batVis = t * (1 - over * 0.5f)
            for ((i, b) in bats.withIndex()) {
                val p = wrap01(batT * b.speed + b.phase)
                val fade = minOf(p * 9f, (1 - p) * 9f, 1f)
                if (fade <= 0f) continue
                val cx = (0.115f + p * 0.55f) * w
                val cy = (0.76f - sin(p * PI).toFloat() * 0.18f - p * 0.06f +
                    sin((p * 12 + b.phase * 9)) * 0.006f) * h
                val flap = sin((wingT * 3 + b.phase * 7) * 2 * PI).toFloat() * b.s * 0.9f
                val path = Path().apply {
                    moveTo(cx - b.s, cy - flap)
                    quadraticTo(cx - b.s / 2, cy + b.s * 0.4f, cx, cy)
                    quadraticTo(cx + b.s / 2, cy + b.s * 0.4f, cx + b.s, cy - flap)
                }
                drawPath(path, batCol.copy(alpha = (batVis * fade).coerceIn(0f, 1f)), style = Stroke(width = 3f, cap = StrokeCap.Round))
            }
        }

        // --- Rain ---
        if (rainMix > 0.02f) {
            val rainCol = lerp(Color(0xFFDCE8F2), Color(0xFF9FB2D8), t)
            for (d in drops) {
                val yy = wrap01(d.y + rainT * (0.5f + d.phase * 0.7f))
                val y = (yy * 1.15f - 0.075f) * h
                val x = d.x * w
                drawLine(
                    rainCol.copy(alpha = 0.40f * rainMix),
                    Offset(x, y), Offset(x + d.r * 0.18f, y + d.r),
                    strokeWidth = 2.5f, cap = StrokeCap.Round,
                )
            }
        }

        // --- Snow ---
        if (snowMix > 0.02f) {
            for (f in flakes) {
                val yy = wrap01(f.y + snowT * (0.35f + f.phase * 0.4f))
                val y = (yy * 1.15f - 0.075f) * h
                val x = f.x * w + sin((snowT * 4 + f.phase * 8) * 2 * PI).toFloat() * 18f
                drawCircle(Color.White.copy(alpha = 0.85f * snowMix), f.r, Offset(x, y))
            }
        }

        // --- Fog banks over the lake ---
        if (fogMix > 0.02f) {
            val fogCol = lerp(Color(0xFFE8E4DA), Color(0xFF39415E), t)
            val bands = listOf(
                Triple(0.30f, 0.52f, 46f), Triple(0.65f, 0.62f, 62f), Triple(0.42f, 0.71f, 80f),
            )
            for ((i, b) in bands.withIndex()) {
                val cx = (wrap01(b.first + fogT * (0.05f + i * 0.02f)) * 1.4f - 0.2f) * w
                drawOval(
                    fogCol.copy(alpha = 0.30f * fogMix),
                    Offset(cx - w * 0.35f, b.second * h), Size(w * 0.7f, b.third),
                )
            }
        }

        // --- Lightning ---
        if (stormMix > 0.02f) {
            val inFlash = (stormT in 0.30f..0.37f) || (stormT in 0.74f..0.82f)
            if (inFlash) {
                val flicker = 0.35f + 0.65f * abs(sin(stormT * 400f))
                val flash = stormMix * flicker
                drawRect(Color.White.copy(alpha = 0.22f * flash))
                val bolt = Path().apply {
                    val pts = listOf(
                        0.700f to 0.08f, 0.662f to 0.22f, 0.690f to 0.24f,
                        0.632f to 0.40f, 0.660f to 0.42f, 0.600f to 0.58f,
                    )
                    moveTo(pts[0].first * w, pts[0].second * h)
                    for (k in 1 until pts.size) lineTo(pts[k].first * w, pts[k].second * h)
                }
                drawPath(bolt, Color.White.copy(alpha = 0.35f * flash), style = Stroke(width = 14f, cap = StrokeCap.Round))
                drawPath(bolt, Color(0xFFFFF6D8).copy(alpha = 0.90f * flash), style = Stroke(width = 5f, cap = StrokeCap.Round))
            }
        }

        // Tower beacons.
        if (t > 0.05f) {
            val blink = { off: Float ->
                if (wrap01(shimmer * 2 + off) < 0.12f) 1f else 0.08f
            }
            drawCircle(Color(0xFFFF5A5A).copy(alpha = t * blink(0f)), 3.5f, Offset(w * 0.8025f, horizon - FROST_H * h - 32f))
            drawCircle(Color(0xFFFF5A5A).copy(alpha = t * blink(0.5f)), 3.5f, Offset(w * 0.9425f, horizon - AUST_H * h - 14f))
        }
    }
}

private fun DrawScope.drawSlab(b: Slab, horizon: Float, w: Float, h: Float, col: Color) {
    drawRect(col, Offset(b.x0 * w, horizon - b.h * h), Size((b.x1 - b.x0) * w, b.h * h))
}
