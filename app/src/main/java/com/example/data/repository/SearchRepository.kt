package com.example.data.repository

import com.example.data.model.ApiDocumentItem
import com.example.data.model.EcoleResponse
import com.example.data.model.FiliereResponse
import com.example.data.model.SearchDocumentsResponse
import com.example.data.network.CampusEduService
import okhttp3.ResponseBody
import retrofit2.Response

class SearchRepository(private val service: CampusEduService) {

    suspend fun downloadFromUrl(url: String): Response<ResponseBody> {
        return service.downloadFromUrl(url)
    }

    /**
     * Récupère tous les documents du catalogue (cours et épreuves)
     * sans déclencher l'erreur 400 de l'API (qui requiert au moins un paramètre).
     */
    suspend fun getAllCatalogueDocuments(): List<ApiDocumentItem> {
        val result = mutableListOf<ApiDocumentItem>()
        try {
            val resCours = service.searchDocuments(type = "cours")
            if (resCours.isSuccessful && resCours.body()?.success == true) {
                resCours.body()?.data?.documents?.let { result.addAll(it) }
            }
        } catch (_: Exception) {}

        try {
            val resSujet = service.searchDocuments(type = "sujet")
            if (resSujet.isSuccessful && resSujet.body()?.success == true) {
                resSujet.body()?.data?.documents?.let { result.addAll(it) }
            }
        } catch (_: Exception) {}

        // Si aucun résultat, repli sur une recherche large 'e'
        if (result.isEmpty()) {
            try {
                val resFallback = service.searchDocuments(q = "e")
                if (resFallback.isSuccessful && resFallback.body()?.success == true) {
                    resFallback.body()?.data?.documents?.let { result.addAll(it) }
                }
            } catch (_: Exception) {}
        }

        return result.distinctBy { it.id }
    }

    suspend fun searchDocuments(
        nom: String? = null,
        q: String? = null,
        niveau: String? = null,
        filiereCode: String? = null,
        filiereId: Int? = null,
        ecole: String? = null,
        ecoleId: Int? = null,
        annee: String? = null,
        type: String? = null,
        page: Int? = null,
        limit: Int? = null
    ): Response<SearchDocumentsResponse> {
        val formattedNom = if (nom.isNullOrBlank()) null else nom
        val formattedQ = if (q.isNullOrBlank()) null else q
        val formattedNiveau = if (niveau.isNullOrBlank() || niveau.equals("Tous", ignoreCase = true) || niveau.equals("Tout", ignoreCase = true)) null else niveau
        val formattedFiliere = if (filiereCode.isNullOrBlank() || filiereCode.equals("Toutes", ignoreCase = true) || filiereCode.equals("Tout", ignoreCase = true)) null else filiereCode
        val formattedEcole = if (ecole.isNullOrBlank() || ecole.equals("Toutes", ignoreCase = true) || ecole.equals("Tout", ignoreCase = true)) null else ecole
        val formattedAnnee = if (annee.isNullOrBlank() || annee.equals("Toutes", ignoreCase = true) || annee.equals("Tous", ignoreCase = true)) null else annee
        val formattedType = if (type.isNullOrBlank() || type.equals("Tous", ignoreCase = true) || type.equals("Tout", ignoreCase = true)) null else type.lowercase()

        // L'API Vercel exige au moins un filtre ou mot-clé pour éviter un HTTP 400
        val isAllNull = formattedNom == null && formattedQ == null && formattedNiveau == null &&
                formattedFiliere == null && filiereId == null && formattedEcole == null &&
                ecoleId == null && formattedAnnee == null && formattedType == null

        val safeQ = if (isAllNull) "e" else formattedQ

        return service.searchDocuments(
            nom = formattedNom,
            q = safeQ ?: formattedNom,
            niveau = formattedNiveau,
            filiere = formattedFiliere,
            filiereId = filiereId,
            ecole = formattedEcole,
            ecoleId = ecoleId,
            annee = formattedAnnee,
            type = formattedType,
            page = page,
            limit = limit
        )
    }

    suspend fun getFilieres(ecoleId: Int? = null, ecole: String? = null): Response<FiliereResponse> {
        val formattedEcole = if (ecole.isNullOrBlank() || ecole.equals("Toutes", ignoreCase = true) || ecole.equals("Tout", ignoreCase = true)) null else ecole
        val realEcoleId = ecoleId?.takeIf { it > 0 }
        return service.getFilieres(ecoleId = realEcoleId, ecole = formattedEcole)
    }

    suspend fun getEcoles(): Response<EcoleResponse> {
        return service.getEcoles()
    }

    suspend fun getConfig(): Response<com.example.data.model.AppConfigResponse> {
        return service.getConfig()
    }

    suspend fun submitDemande(demande: com.example.data.model.DemandeRequest): Response<com.example.data.model.DemandeResponse> {
        return service.submitDemande(demande)
    }
}

