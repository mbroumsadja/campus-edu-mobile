package com.example.data.network

import com.example.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface Retrofit pour Campus Edu API v1.
 * Contrat complet API Node/Express :
 * - Préfixe /api/v1/ partout
 * - Pagination sur /search/documents
 * - Route unique de téléchargement /search/documents/telecharger
 * - Cache ETag / Cache-Control sur écoles et filières
 * - Demandes et signalements via /demandes
 * - Configuration et version minimale via /config
 */
interface CampusEduService {

    // --- Configuration générale & version minimale ---
    @GET("api/v1/config")
    suspend fun getConfig(): Response<AppConfigResponse>

    // --- Recherche de documents (cours, sujets, corrigés) ---
    @GET("api/v1/search/documents")
    suspend fun searchDocuments(
        @Query("nom") nom: String? = null,
        @Query("q") q: String? = null,
        @Query("niveau") niveau: String? = null,
        @Query("filiere") filiere: String? = null,
        @Query("filiere_id") filiereId: Int? = null,
        @Query("ecole") ecole: String? = null,
        @Query("ecole_id") ecoleId: Int? = null,
        @Query("annee") annee: String? = null,
        @Query("type") type: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<SearchDocumentsResponse>

    // --- Écoles & Facultés (mis en cache ETag) ---
    @GET("api/v1/ecoles")
    suspend fun getEcoles(): Response<EcoleResponse>

    // --- Filières (mis en cache ETag) ---
    @GET("api/v1/filieres")
    suspend fun getFilieres(
        @Query("ecole_id") ecoleId: Int? = null,
        @Query("ecole") ecole: String? = null
    ): Response<FiliereResponse>

    // --- Téléchargements directs et via API v1 ---
    // Route unique de téléchargement de l'API v1 : GET /api/v1/search/documents/telecharger
    @GET("api/v1/search/documents/telecharger")
    @Streaming
    suspend fun downloadDocumentApi(
        @Query("type") type: String, // "sujet" ou "cours"
        @Query("id") id: Int,
        @Query("document_id") documentId: Int? = null,
        @Query("corrige") corrige: Boolean? = null
    ): Response<ResponseBody>

    @GET
    @Streaming
    suspend fun downloadFromUrl(
        @Url url: String
    ): Response<ResponseBody>

    // --- Demande de sujet manquant ou signalement d'erreur ---
    @POST("api/v1/demandes")
    suspend fun submitDemande(
        @Body demande: DemandeRequest
    ): Response<DemandeResponse>
}

