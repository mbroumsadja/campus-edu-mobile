package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class StudentProfile(
    val ecoleId: Int? = null,
    val ecoleNom: String? = null,
    val filiereId: Int? = null,
    val filiereCode: String? = null,
    val filiereNom: String? = null,
    val niveau: String = "L1",
    val onlyMyCourses: Boolean = false,
    val isConfigured: Boolean = false
) {
    val displaySummary: String
        get() {
            if (!isConfigured) return "Non configuré"
            val ecole = ecoleNom?.take(10) ?: ""
            val filiere = filiereCode ?: filiereNom?.take(12) ?: ""
            return listOfNotNull(ecole.ifBlank { null }, filiere.ifBlank { null }, niveau).joinToString(" • ")
        }
}

data class SavedDocumentMeta(
    val fileKey: String,
    val title: String,
    val ecoleNom: String = "Université de Garoua",
    val filiereNom: String = "Tronc Commun",
    val niveau: String = "L1",
    val ueCode: String = "UE",
    val ueIntitule: String = "Général",
    val isSujetOrTd: Boolean = false,
    val isCorrige: Boolean = false,
    val downloadDateMillis: Long = System.currentTimeMillis()
)

class StudentProfileManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("campus_edu_student_profile", Context.MODE_PRIVATE)
    private val metaPrefs: SharedPreferences = context.getSharedPreferences("campus_edu_doc_metadata", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val metaAdapter = moshi.adapter(SavedDocumentMeta::class.java)

    fun getProfile(): StudentProfile {
        val isConfigured = prefs.getBoolean("is_configured", false)
        val ecoleId = if (prefs.contains("ecole_id")) prefs.getInt("ecole_id", 0).takeIf { it > 0 } else null
        val ecoleNom = prefs.getString("ecole_nom", null)
        val filiereId = if (prefs.contains("filiere_id")) prefs.getInt("filiere_id", 0).takeIf { it > 0 } else null
        val filiereCode = prefs.getString("filiere_code", null)
        val filiereNom = prefs.getString("filiere_nom", null)
        val niveau = prefs.getString("niveau", "L1") ?: "L1"
        val onlyMyCourses = prefs.getBoolean("only_my_courses", false)

        return StudentProfile(
            ecoleId = ecoleId,
            ecoleNom = ecoleNom,
            filiereId = filiereId,
            filiereCode = filiereCode,
            filiereNom = filiereNom,
            niveau = niveau,
            onlyMyCourses = onlyMyCourses,
            isConfigured = isConfigured
        )
    }

    fun saveProfile(profile: StudentProfile) {
        prefs.edit()
            .putBoolean("is_configured", true)
            .apply {
                profile.ecoleId?.let { putInt("ecole_id", it) } ?: remove("ecole_id")
                profile.ecoleNom?.let { putString("ecole_nom", it) } ?: remove("ecole_nom")
                profile.filiereId?.let { putInt("filiere_id", it) } ?: remove("filiere_id")
                profile.filiereCode?.let { putString("filiere_code", it) } ?: remove("filiere_code")
                profile.filiereNom?.let { putString("filiere_nom", it) } ?: remove("filiere_nom")
                putString("niveau", profile.niveau)
                putBoolean("only_my_courses", profile.onlyMyCourses)
            }
            .apply()
    }

    fun clearProfile() {
        prefs.edit().clear().apply()
    }

    fun saveDocumentMeta(meta: SavedDocumentMeta) {
        try {
            val json = metaAdapter.toJson(meta)
            metaPrefs.edit().putString(meta.fileKey, json).apply()
        } catch (_: Exception) {
        }
    }

    fun getDocumentMeta(fileKey: String): SavedDocumentMeta? {
        val json = metaPrefs.getString(fileKey, null) ?: return null
        return try {
            metaAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }
}
