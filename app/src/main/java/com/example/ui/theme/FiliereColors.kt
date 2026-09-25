package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import java.util.Locale

data class FiliereColorPalette(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val accentColor: Color
)

object FiliereColorProvider {

    // Variantes subtiles et élégantes de la couleur unique : BLEU
    private val RoyalBluePalette = FiliereColorPalette(
        containerColor = Color(0xFFEEF4FF),
        contentColor = Color(0xFF1565C0),
        borderColor = Color(0xFFBFDBFE),
        accentColor = Color(0xFF2563EB)
    )

    private val DeepNavyPalette = FiliereColorPalette(
        containerColor = Color(0xFFE8EEF8),
        contentColor = Color(0xFF0F3B7A),
        borderColor = Color(0xFFB5CCE8),
        accentColor = Color(0xFF1D4ED8)
    )

    private val SkyBluePalette = FiliereColorPalette(
        containerColor = Color(0xFFF0F7FF),
        contentColor = Color(0xFF0284C7),
        borderColor = Color(0xFFBAE6FD),
        accentColor = Color(0xFF0EA5E9)
    )

    private val CobaltPalette = FiliereColorPalette(
        containerColor = Color(0xFFEDF2FD),
        contentColor = Color(0xFF1E40AF),
        borderColor = Color(0xFFC7D2FE),
        accentColor = Color(0xFF3B82F6)
    )

    private val OceanBluePalette = FiliereColorPalette(
        containerColor = Color(0xFFEBF5FB),
        contentColor = Color(0xFF0369A1),
        borderColor = Color(0xFFB9E2FE),
        accentColor = Color(0xFF0284C7)
    )

    private val SlateBluePalette = FiliereColorPalette(
        containerColor = Color(0xFFF0F4F8),
        contentColor = Color(0xFF243B53),
        borderColor = Color(0xFFCBDCEB),
        accentColor = Color(0xFF486581)
    )

    private val FallbackPalettes = listOf(
        RoyalBluePalette,
        DeepNavyPalette,
        SkyBluePalette,
        CobaltPalette,
        OceanBluePalette,
        SlateBluePalette
    )

    /**
     * Resolves the signature color palette for a given filière code, name or UE code.
     */
    fun getPalette(filiereCode: String?, filiereNom: String? = null, fallbackText: String? = null): FiliereColorPalette {
        val combined = "${filiereCode.orEmpty()} ${filiereNom.orEmpty()} ${fallbackText.orEmpty()}".lowercase(Locale.ROOT)
        
        if (combined.isBlank()) {
            return RoyalBluePalette
        }

        val hash = kotlin.math.abs(combined.trim().hashCode())
        return FallbackPalettes[hash % FallbackPalettes.size]
    }

    /**
     * Extracts a clean, short display label for the filière badge.
     */
    fun getDisplayLabel(filiereCode: String?, filiereNom: String?, fallbackUeCode: String?): String {
        filiereCode?.takeIf { it.isNotBlank() }?.let { return it.uppercase(Locale.ROOT) }
        filiereNom?.takeIf { it.isNotBlank() }?.let {
            return if (it.length <= 16) it else it.take(14).trim() + "…"
        }
        
        // If code UE has letter prefix like "INF101" -> "INF"
        fallbackUeCode?.takeIf { it.isNotBlank() }?.let { code ->
            val letters = code.takeWhile { !it.isDigit() }.trim()
            if (letters.length in 2..8) return letters.uppercase(Locale.ROOT)
            return code.take(6).uppercase(Locale.ROOT)
        }
        
        return "GÉNÉRAL"
    }
}
