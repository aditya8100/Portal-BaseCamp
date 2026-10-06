package com.portalhomebase.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Day/night palettes. Dark values are the original kitchen palette, untouched;
// light is a warm paper theme. Call sites use the semantic tokens, never these.
val LocalIsLight = compositionLocalOf { false }

// Auto mode follows the sun: light between today's sunrise and sunset from
// the weather feed (same wall clock as the house). Unparseable or missing
// times fall back to fixed 07:00-19:00.
fun isLightHour(hour: Int): Boolean = hour in 7..18

fun sunlit(now: java.time.LocalTime, rise: String, set: String): Boolean {
    val r = runCatching { java.time.LocalTime.parse(rise) }.getOrNull()
    val s = runCatching { java.time.LocalTime.parse(set) }.getOrNull()
    if (r == null || s == null || !s.isAfter(r)) return isLightHour(now.hour)
    return !now.isBefore(r) && now.isBefore(s)
}

fun resolveLightSun(mode: String, now: java.time.LocalTime, rise: String, set: String): Boolean =
    when (mode) {
        "light" -> true
        "dark" -> false
        else -> sunlit(now, rise, set)
    }

val Ink: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF1C1F24) else Color(0xFFF2F4F8)
val Muted: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF6E665C) else Color(0xFF9AA4B2)
val Content: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF33373E) else Color(0xFFDADADA)

val Backdrop: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFECE5D5) else Color(0xFF1A1A1A)
// Translucent so the skyline breathes through; text tokens stay fully opaque.
val SurfaceBg: Color
    @Composable get() = (if (LocalIsLight.current) Color(0xFFFFFFFF) else Color(0xFF2B2B2B)).copy(alpha = 0.86f)
val Panel: Color
    @Composable get() = (if (LocalIsLight.current) Color(0xFFFFFFFF) else Color(0xFF161B23)).copy(alpha = 0.86f)
val CardBg: Color
    @Composable get() = (if (LocalIsLight.current) Color(0xFFFFFFFF) else Color(0xFF1B212B)).copy(alpha = 0.86f)
val AlertCard: Color
    @Composable get() = (if (LocalIsLight.current) Color(0xFFFBE9D7) else Color(0xFF292019)).copy(alpha = 0.86f)
val Scrim: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFFFFDF7) else Color(0xFF0B0E12)

val Outline: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFDED3BC) else Color(0xFF2C3542)
val Selected: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFE2EAF6) else Color(0xFF2C3947)
val SelectedBorder: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFA9C2E8) else Color(0xFF4A5B70)

val Accent: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF1668D9) else Color(0xFF1990FF)
val AccentPressed: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF1257B8) else Color(0xFF1877F2)
val AccentDeep: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF0B3D91) else Color(0xFF004CB0)
val OnAccent: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFFFFFFF) else Color(0xFFF0F0F0)

val Event: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF1F5FAE) else Color(0xFF9FC1E8)
val Star: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF9A6B12) else Color(0xFFF0C96A)
val Success: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF1E7A34) else Color(0xFF6CD64F)
val OnSuccess: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFFFFFFF) else Color(0xFF101418)
val Toggle: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF1E7A34) else Color(0xFF1F6F43)
val ToggleBorder: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFF2A9D5F) else Color(0xFF2A9D5F)
val ToggleOff: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFE5DECF) else Color(0xFF262E3A)
val Error: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFC81E2B) else Color(0xFFFA484E)
val Danger: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFF6D2D2) else Color(0xFF5A2323)
val ButtonBg: Color
    @Composable get() = if (LocalIsLight.current) Color(0xFFEFE7D6) else Color(0xFF232B36)
