package com.portalhomebase.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Day/night Material schemes driven by LocalIsLight (provided in App).
// Portal: dynamic color NEVER on.
@Composable
fun HomebaseTheme(content: @Composable () -> Unit) {
    val scheme = if (LocalIsLight.current) {
        lightColorScheme(
            primary = Accent,
            onPrimary = OnAccent,
            primaryContainer = Selected,
            onPrimaryContainer = Ink,
            secondary = Muted,
            onSecondary = OnAccent,
            error = Error,
            onError = OnAccent,
            background = Backdrop,
            onBackground = Ink,
            surface = SurfaceBg,
            onSurface = Ink,
        )
    } else {
        darkColorScheme(
            primary = Accent,
            onPrimary = OnAccent,
            primaryContainer = AccentDeep,
            onPrimaryContainer = OnAccent,
            secondary = Muted,
            onSecondary = OnAccent,
            error = Error,
            onError = OnAccent,
            background = Backdrop,
            onBackground = Content,
            surface = SurfaceBg,
            onSurface = Content,
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content,
    )
}
