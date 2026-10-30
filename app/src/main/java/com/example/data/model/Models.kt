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
    val description: String?,
    val type: String?, // pdf, video, slide, autre
    val cheminFichier: String?,
    val lienTelechargement: String? = null,
    val nomFichierOriginal: String?,
    val tailleFichier: Long?,
    val statut: String?,
    val anneAcademique: String?,
    val vues: Int?,
    val telechargemements: Int?,
    val ue_id: Int?,
    val enseignant_id: Int?,
    val deja_telecharge: Boolean? = false,
    val disponible: Boolean? = true,
    val filiereCode: String? = null,
    val filiereNom: String? = null,
    val ecoleNom: String? = null
) {
    val downloadUrl: String?
        get() = lienTelechargement ?: cheminFichier
}

@JsonClass(generateAdapter = true)
data class Sujet(
    val id: Int,
    val titre: String,
    val type: String?, // partiel, rattrapage, terminal, tp, td
    val session: String?, // normale, rattrapage
    val annee: String?,
    val cheminFichier: String?,
    val lienTelechargement: String? = null,
    val avecCorrige: Boolean?,
    val cheminCorrige: String?,
    val lienCorrigeTelechargement: String? = null,
    val statut: String?,
    val telechargemements: Int?,
    val ue_id: Int?,
    val enseignant_id: Int?,
    val deja_telecharge: Boolean? = false,
    val disponible: Boolean? = true,
    val filiereCode: String? = null,
    val filiereNom: String? = null,
    val ecoleNom: String? = null
) {
    val downloadUrl: String?
        get() = lienTelechargement ?: cheminFichier

    fun getDownloadUrlForCorrige(isCorrige: Boolean): String? {
        return if (isCorrige) {
            lienCorrigeTelechargement ?: cheminCorrige ?: lienTelechargement ?: cheminFichier
        } else {
            lienTelechargement ?: cheminFichier
        }
    }

    val anneeAcademique: String?
        get() = annee

    val tailleFichier: Long?
        get() = null
}

@JsonClass(generateAdapter = true)
data class Pagination(
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean
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
    val data: SearchDocumentsData?,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class SearchDocumentsData(
    val nombre_resultats: Int? = 0,
    val documents: List<ApiDocumentItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiDocumentItem(
    val id: Int,
    val cours_id: Int? = null,
    val tag_ordre: String? = null, 
    val type_contenu: String? = null,
    val nom: String,
    val type: String? = null,
    val lien_telechargement: String? = null,
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

