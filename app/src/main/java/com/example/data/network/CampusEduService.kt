package com.example.data.network

import com.example.data.model.EcoleResponse
import com.example.data.model.FiliereResponse
import com.example.data.model.SearchDocumentsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface CampusEduService {
    @GET("api/search/documents")
    suspend fun searchDocuments(
        @Query("nom") nom: String? = null,
        @Query("q") q: String? = null,
        @Query("niveau") niveau: String? = null,
        @Query("filiere") filiere: String? = null,
        @Query("filiere_id") filiereId: Int? = null,
        @Query("ecole") ecole: String? = null,
        @Query("ecole_id") ecoleId: Int? = null,
        @Query("annee") annee: String? = null,
        @Query("type") type: String? = null
    ): Response<SearchDocumentsResponse>

    @GET("api/ecoles")
    suspend fun getEcoles(): Response<EcoleResponse>

    @GET("api/ecole")
    suspend fun getEcoleSingle(): Response<EcoleResponse>

    @GET("ecole")
    suspend fun getEcoleDirect(): Response<EcoleResponse>

    @GET("ecoles")
    suspend fun getEcolesDirect(): Response<EcoleResponse>

    @GET("api/search/documents/telecharger")
    @Streaming
    suspend fun downloadDocumentApi(
        @Query("type") type: String,
        @Query("id") id: Int,
        @Query("corrige") corrige: Boolean? = null
    ): Response<ResponseBody>

    @GET("api/filieres")
    suspend fun getFilieres(): Response<FiliereResponse>

    @GET("api/filiere")
    suspend fun getFiliereSingle(): Response<FiliereResponse>

    @GET("filiere")
    suspend fun getFiliereDirect(): Response<FiliereResponse>

    @GET("filieres")
    suspend fun getFilieresDirect(): Response<FiliereResponse>

    @GET("api/cours/{id}/telecharger")
    @Streaming
    suspend fun downloadCours(
        @Path("id") id: Int
    ): Response<ResponseBody>

    @GET("api/sujets/{id}/telecharger")
    @Streaming
    suspend fun downloadSujet(
        @Path("id") id: Int,
        @Query("corrige") corrige: Boolean
    ): Response<ResponseBody>

    @GET
    @Streaming
    suspend fun downloadFromUrl(
        @Url url: String
    ): Response<ResponseBody>

    @GET
    @Streaming
    suspend fun downloadDirectUrl(
        @Url url: String
    ): Response<ResponseBody>
}
