package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Charte Campus Edu v1.0 : Nuit / Azur / Ciel + un seul accent chaud (Soleil).
// Le Soleil n'apparaît qu'une fois par écran (bouton Télécharger, badge « Nouveau », point hors-ligne).

// Valeurs de marque
val Nuit = Color(0xFF0A1B3D)
val Azur = Color(0xFF1456D6)
val Ciel = Color(0xFF4CC2FF)
val Soleil = Color(0xFFF5A524)
val Papier = Color(0xFFF6F8FC)
val Craie = Color(0xFFE6ECF6)
val Brume = Color(0xFFA3B4D1)
val Ardoise = Color(0xFF586783)
val Marine = Color(0xFF0F1A38)
val Minuit = Color(0xFF070E22)
val Succes = Color(0xFF12805C)
val Erreur = Color(0xFFC42B31)

// ---------- Mode sombre (Minuit / Marine / Ciel) ----------
val MidnightBackground = Minuit
val MidnightOnBackground = Color(0xFFFFFFFF)

val MidnightSurface = Marine
val MidnightOnSurface = Color(0xFFFFFFFF)
val MidnightSurfaceVariant = Color(0xFF1A2850)
val MidnightOnSurfaceVariant = Brume

val MidnightPrimary = Ciel
val MidnightOnPrimary = Nuit
val MidnightPrimaryContainer = Color(0xFF1D3C85)
val MidnightOnPrimaryContainer = Craie

val MidnightSecondary = Brume
val MidnightOnSecondary = Nuit
val MidnightSecondaryContainer = Color(0xFF1A2850)
val MidnightOnSecondaryContainer = Craie

val MidnightTertiary = Soleil
val MidnightOnTertiary = Nuit
val MidnightTertiaryContainer = Color(0xFF5C3D06)
val MidnightOnTertiaryContainer = Color(0xFFFDE9C4)

val MidnightOutline = Color(0xFF2B4F9E)
val MidnightOutlineVariant = Color(0xFF1A2850)

// ---------- Mode clair (Papier / Blanc / Azur) ----------
val LightBackground = Papier
val LightOnBackground = Nuit

val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Nuit
val LightSurfaceVariant = Craie
val LightOnSurfaceVariant = Ardoise

val LightPrimary = Azur
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDCE6FB)
val LightOnPrimaryContainer = Nuit

val LightSecondary = Color(0xFF2B4F9E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Craie
val LightOnSecondaryContainer = Nuit

val LightTertiary = Soleil
val LightOnTertiary = Nuit
val LightTertiaryContainer = Color(0xFFFDE9C4)
val LightOnTertiaryContainer = Nuit

val LightOutline = Brume
val LightOutlineVariant = Craie
