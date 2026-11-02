package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Charte Campus Edu : Sora pour les titres, Inter pour la lecture.
// Fichiers statiques par graisse (compatibles Android 7+, pas de police variable).
val SoraFontFamily = FontFamily(
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold)
)

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

// Échelle mobile : titre 32 / H2 24 / H3 18 / corps 16 / secondaire 14 / étiquette 12.
// Le corps ne descend jamais sous 16 sp dans les champs.
val Typography =
    Typography(
        displayLarge = TextStyle(
            fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = SoraFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp
        ),
        titleLarge = TextStyle(
            fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp
        ),
        titleMedium = TextStyle(
            fontFamily = SoraFontFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp, lineHeight = 24.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
            fontSize = 16.sp, lineHeight = 26.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
            fontSize = 14.sp, lineHeight = 22.sp
        ),
        labelLarge = TextStyle(
            fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp, lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp
        )
    )
