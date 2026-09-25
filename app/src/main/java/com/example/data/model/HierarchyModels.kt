package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Departement(
    val id: Int,
    val nom: String,
    @Json(name = "ecole_id") val ecoleId: Int,
    val code: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class NiveauItem(
    val id: Int,
    val nom: String,  // e.g. "Licence 1", "Licence 2", "Licence 3", "Master 1", "Master 2"
    val code: String, // e.g. "L1", "L2", "L3", "M1", "M2"
    val documentsCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class SemestreItem(
    val id: Int,
    val nom: String,  // e.g. "Semestre 1", "Semestre 2"
    val code: String, // e.g. "S1", "S2"
    val documentsCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class CoursHierarchyItem(
    val id: Int,
    val nom: String,
    val codeUe: String,
    @Json(name = "filiere_id") val filiereId: Int = 0,
    val filiereCode: String? = null,
    @Json(name = "niveau_id") val niveauId: Int = 0,
    val niveauCode: String? = null,
    @Json(name = "semestre_id") val semestreId: Int = 0,
    val description: String? = null,
    val credits: Int? = null,
    val documentsCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class DocumentHierarchyItem(
    val id: Int,
    @Json(name = "cours_id") val coursId: Int,
    val nom: String,
    val type: String, // "cours", "epreuve", "corrige", "td", "tp"
    val typeLabel: String,
    val format: String = "PDF",
    val tailleLisible: String? = null,
    val tailleOctets: Long? = null,
    val downloadUrl: String? = null,
    val anneeAcademique: String? = null,
    val disponible: Boolean = true,
    val dejaTelecharge: Boolean = false,
    val tagOrdre: String? = null
)

enum class HierarchyStep(val label: String, val levelNumber: Int) {
    ECOLE("Écoles", 1),
    DEPARTEMENT("Départements", 2),
    FILIERE("Filières", 3),
    NIVEAU("Niveaux", 4),
    SEMESTRE("Semestres", 5),
    COURS("Cours & UE", 6),
    DOCUMENTS("Documents", 7)
}
