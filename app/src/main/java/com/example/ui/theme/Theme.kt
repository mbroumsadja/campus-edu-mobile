package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Palette sombre / nuit inspirée de l'icône de démarrage (logo au chapeau brillant sur fond noir/bleu nuit)
private val MidnightColorScheme =
    darkColorScheme(
        primary = MidnightPrimary,
        onPrimary = MidnightOnPrimary,
        primaryContainer = MidnightPrimaryContainer,
        onPrimaryContainer = MidnightOnPrimaryContainer,
        secondary = MidnightSecondary,
        onSecondary = MidnightOnSecondary,
        secondaryContainer = MidnightSecondaryContainer,
        onSecondaryContainer = MidnightOnSecondaryContainer,
        tertiary = MidnightTertiary,
        onTertiary = MidnightOnTertiary,
        tertiaryContainer = MidnightTertiaryContainer,
        onTertiaryContainer = MidnightOnTertiaryContainer,
        background = MidnightBackground,
        onBackground = MidnightOnBackground,
        surface = MidnightSurface,
        onSurface = MidnightOnSurface,
        surfaceVariant = MidnightSurfaceVariant,
        onSurfaceVariant = MidnightOnSurfaceVariant,
        outline = MidnightOutline,
        outlineVariant = MidnightOutlineVariant
    )

// Palette claire adaptée et lumineuse en bleu et blanc
private val LightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        secondary = LightSecondary,
        onSecondary = LightOnSecondary,
        secondaryContainer = LightSecondaryContainer,
        onSecondaryContainer = LightOnSecondaryContainer,
        tertiary = LightTertiary,
        onTertiary = LightOnTertiary,
        tertiaryContainer = LightTertiaryContainer,
        onTertiaryContainer = LightOnTertiaryContainer,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant
    )

// IndicationNodeFactory neutre sans effet de survol ni d'ondulation (hover / ripple désactivés)
private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node {
        return object : Modifier.Node() {}
    }

    override fun hashCode(): Int = -1
    override fun equals(other: Any?): Boolean = other === this
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // S'adapte automatiquement au mode sombre (Midnight) ou clair (Light blue/white) selon le thème du téléphone
    val colorScheme = if (darkTheme) MidnightColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Suppression globale de tous les effets de survol / hover / ripple dans l'application
    CompositionLocalProvider(
        LocalIndication provides NoIndication
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

