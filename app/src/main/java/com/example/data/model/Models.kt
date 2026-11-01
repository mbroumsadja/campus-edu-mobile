package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val success: Boolean,
    val data: List<UeWithDocuments>?,
    val message: String?,
    val pagination: Pagination?
)

@JsonClass(generateAdapter = true)
data class UeWithDocuments(
    val id: Int,
    val code: String,
    val intitule: String,
    val niveau: String?,
    val semestre: String? = null,
    val credits: Int? = null,
    val cours: List<Cours>?,
    val sujets: List<Sujet>?,
    val filiereCode: String? = null,
    val filiereNom: String? = null,
    val ecoleNom: String? = null
) {
    val nom: String
        get() = intitule
}

@JsonClass(generateAdapter = true)
data class Cours(
    val id: Int,
    val titre: String,
    val description: String? = null,
    val type: String? = null, // pdf, video, slide, autre
    val cheminFichier: String? = null,
    val lienTelechargement: String? = null,
    val nomFichierOriginal: String? = null,
    val tailleFichier: Long? = null,
    val statut: String? = null,
    val anneAcademique: String? = null,
    val annee_academique: String? = null,
    val taille_lisible: String? = null,
    val vues: Int? = null,
    val telechargemements: Int? = null,
    val ue_id: Int? = null,
    val enseignant_id: Int? = null,
    val deja_telecharge: Boolean? = false,
    val disponible: Boolean? = true,
    val filiereCode: String? = null,
    val filiereNom: String? = null,
    val ecoleNom: String? = null
) {
    val downloadUrl: String?
        get() = lienTelechargement ?: cheminFichier

    val anneeAcademique: String?
        get() = annee_academique ?: anneAcademique

    val tailleLisible: String?
        get() = taille_lisible
}

@JsonClass(generateAdapter = true)
data class Sujet(
    val id: Int,
    val titre: String,
    val type: String? = null, // partiel, rattrapage, terminal, tp, td
    val type_epreuve: String? = null,
    val session: String? = null, // normale, rattrapage
    val annee: String? = null,
    val annee_academique: String? = null,
    val cheminFichier: String? = null,
    val lienTelechargement: String? = null,
    val lien_telechargement: String? = null,
    val avecCorrige: Boolean? = null,
    val avec_corrige: Boolean? = null,
    val cheminCorrige: String? = null,
    val lienCorrigeTelechargement: String? = null,
    val lien_corrige: String? = null,
    val taille_lisible: String? = null,
    val taille_octets: Long? = null,
    val statut: String? = null,
    val telechargemements: Int? = null,
    val ue_id: Int? = null,
    val enseignant_id: Int? = null,
    val deja_telecharge: Boolean? = false,
    val disponible: Boolean? = true,
    val filiereCode: String? = null,
    val filiereNom: String? = null,
    val ecoleNom: String? = null
) {
    val downloadUrl: String?
        get() = lien_telechargement ?: lienTelechargement ?: cheminFichier

    fun getDownloadUrlForCorrige(isCorrige: Boolean): String? {
        return if (isCorrige) {
            lien_corrige ?: lienCorrigeTelechargement ?: cheminCorrige ?: downloadUrl
        } else {
            downloadUrl
        }
    }

    val anneeAcademique: String?
        get() = annee_academique ?: annee

    val typeEpreuve: String?
        get() = type_epreuve ?: type

    val hasCorrige: Boolean
        get() = (avec_corrige == true) || (avecCorrige == true) || !lien_corrige.isNullOrBlank() || !lienCorrigeTelechargement.isNullOrBlank()

    val tailleLisible: String?
        get() = taille_lisible
}

@JsonClass(generateAdapter = true)
data class Pagination(
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val totalPages: Int = 1,
    val hasNext: Boolean = false,
    val hasPrev: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Filiere(
    val id: Int,
    val code: String,
    val nom: String,
    val departement: String? = null,
    val ecole_id: Int? = null,
    val ecole_nom: String? = null,
    val ecole: String? = null
)

@JsonClass(generateAdapter = true)
data class FiliereResponse(
    val success: Boolean,
    val data: List<Filiere>?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class SearchDocumentsResponse(
    val success: Boolean,
    val data: SearchDocumentsData? = null,
    val pagination: Pagination? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchDocumentsData(
    val nombre_resultats: Int? = 0,
    val pagination: Pagination? = null,
    val documents: List<ApiDocumentItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiDocumentItem(
    val id: Int,
    val cours_id: Int? = null,
    val sujet_id: Int? = null,
    val tag_ordre: String? = null, 
    val type_contenu: String? = null,
    val nom: String,
    val type: String? = null,
    val type_epreuve: String? = null,
    val lien_telechargement: String? = null,
    val lien_corrige: String? = null,
    val taille_octets: String? = null,
    val taille_lisible: String? = null,
    val niveau: String? = null,
    val semestre: String? = null,
    val filiere_id: Int? = null,
    val filiere_code: String? = null,
    val filiere_nom: String? = null,
    val ecole_id: Int? = null,
    val ecole_nom: String? = null,
    val code_ue: String? = null,
    val intitule_ue: String? = null,
    val ue_id: Int? = null,
    val annee_academique: String? = null,
    val annee: String? = null,
    val session: String? = null,
    val avec_corrige: Boolean? = null,
    val telechargements: Int? = null,
    val disponible: Boolean? = true,
    val deja_telecharge: Boolean? = false
) {
    val lienTelechargement: String?
        get() = lien_telechargement

    val tailleOctetsLong: Long?
        get() = taille_octets?.toLongOrNull()
}

@JsonClass(generateAdapter = true)
data class DemandeRequest(
    val type: String, // "sujet_manquant" ou "signalement"
    val ue_id: Int? = null,
    val sujet_id: Int? = null,
    val message: String,
    val contact: String? = null
    // CRITICAL: NEVER include "site_web" field (honeypot trap)
)

@JsonClass(generateAdapter = true)
data class DemandeResponse(
    val success: Boolean = true,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AppConfigResponse(
    val success: Boolean = true,
    val version_minimale: String? = null,
    val data: AppConfigData? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AppConfigData(
    val version_minimale: String? = null,
    val maintenance: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class Ecole(
    val id: Int,
    @Json(name = "ecole") val nom: String,
    val sigle: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class EcoleResponse(
    val success: Boolean,
    val data: List<Ecole>?,
    val message: String?
)


