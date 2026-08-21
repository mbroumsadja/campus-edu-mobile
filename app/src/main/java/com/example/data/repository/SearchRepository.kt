package com.example.data.repository

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

    suspend fun searchDocuments(
        nom: String? = null,
        q: String? = null,
        niveau: String? = null,
        filiereCode: String? = null,
        filiereId: Int? = null,
        ecole: String? = null,
        ecoleId: Int? = null,
        annee: String? = null,
        type: String? = null
    ): Response<SearchDocumentsResponse> {
        val formattedNom = if (nom.isNullOrBlank()) null else nom
        val formattedQ = if (q.isNullOrBlank()) null else q
        val formattedNiveau = if (niveau.isNullOrBlank() || niveau.equals("Tous", ignoreCase = true) || niveau.equals("Tout", ignoreCase = true)) null else niveau
        val formattedFiliere = if (filiereCode.isNullOrBlank() || filiereCode.equals("Toutes", ignoreCase = true) || filiereCode.equals("Tout", ignoreCase = true)) null else filiereCode
        val formattedEcole = if (ecole.isNullOrBlank() || ecole.equals("Toutes", ignoreCase = true) || ecole.equals("Tout", ignoreCase = true)) null else ecole
        val formattedAnnee = if (annee.isNullOrBlank() || annee.equals("Toutes", ignoreCase = true) || annee.equals("Tout", ignoreCase = true)) null else annee
        val formattedType = if (type.isNullOrBlank() || type.equals("Tous", ignoreCase = true) || type.equals("Tout", ignoreCase = true)) null else type.lowercase()

        return service.searchDocuments(
            nom = formattedNom,
            q = formattedQ ?: formattedNom,
            niveau = formattedNiveau,
            filiere = formattedFiliere,
            filiereId = filiereId,
            ecole = formattedEcole,
            ecoleId = ecoleId,
            annee = formattedAnnee,
            type = formattedType
        )
    }

    suspend fun getFilieres(): Response<FiliereResponse> {
        return try {
            val res1 = service.getFilieres()
            if (res1.isSuccessful && res1.body()?.data?.isNotEmpty() == true) return res1
            
            val res2 = service.getFiliereSingle()
            if (res2.isSuccessful && res2.body()?.data?.isNotEmpty() == true) return res2

            val res3 = service.getFiliereDirect()
            if (res3.isSuccessful && res3.body()?.data?.isNotEmpty() == true) return res3

            val res4 = service.getFilieresDirect()
            if (res4.isSuccessful && res4.body()?.data?.isNotEmpty() == true) return res4

            res1
        } catch (e: Exception) {
            try { service.getFiliereSingle() } catch (_: Exception) { service.getFilieres() }
        }
    }

    suspend fun getEcoles(): Response<EcoleResponse> {
        return try {
            val res1 = service.getEcoles()
            if (res1.isSuccessful && res1.body()?.data?.isNotEmpty() == true) return res1

            val res2 = service.getEcoleSingle()
            if (res2.isSuccessful && res2.body()?.data?.isNotEmpty() == true) return res2

            val res3 = service.getEcoleDirect()
            if (res3.isSuccessful && res3.body()?.data?.isNotEmpty() == true) return res3

            val res4 = service.getEcolesDirect()
            if (res4.isSuccessful && res4.body()?.data?.isNotEmpty() == true) return res4

            res1
        } catch (e: Exception) {
            try { service.getEcoleSingle() } catch (_: Exception) { service.getEcoles() }
        }
    }
}
