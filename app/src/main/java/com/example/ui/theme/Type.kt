package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Police Anton : lettres particulièrement épaisses, compactes et denses
val AntonFontFamily = FontFamily(
    Font(R.font.anton, FontWeight.Normal)
)

// Police Montserrat : variante Black / ExtraBold très compacte
val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat, FontWeight.Black)
)

// Typographie Material 3 standard de l'application
val Typography =
    Typography(
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = AntonFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.5.sp
            ),
        titleLarge =
            TextStyle(
                fontFamily = AntonFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.5.sp
            )
    )
