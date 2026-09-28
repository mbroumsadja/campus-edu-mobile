package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isFromUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiStudyService {
    private const val TAG = "GeminiStudyService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun sendStudyMessage(
        documentTitle: String,
        documentType: String,
        documentNiveau: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateOfflineEducationalResponse(documentTitle, documentType, documentNiveau, userMessage)
        }

        try {
            val endpoint = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val systemPrompt = """
                Tu es l'assistant pédagogique intelligent de Campus Edu, spécialement conçu pour les étudiants de la Faculté des Sciences de l'Université de Garoua.
                L'étudiant a importé le document académique suivant :
                - Titre : $documentTitle
                - Type de ressource : $documentType
                - Niveau d'études : $documentNiveau
                
                Consignes pédagogiques :
                1. Fournis des explications claires, structurées et universitaires (mathématiques, physique, informatique, chimie, biologie, géologie).
                2. Utilise des puces et des exemples concrets pour faciliter la mémorisation et la révision.
                3. Si l'étudiant demande un résumé, structure par notions fondamentales, formules clés et applications.
                4. Si l'étudiant demande des questions d'entraînement ou des quiz, fournis les questions avec des explications méthodiques pour les corrigés.
                5. Réponds toujours avec bienveillance, encouragement et rigueur académique en français.
            """.trimIndent()

            val contentsArray = JSONArray()

            // Inclure l'historique récent de la conversation (max 6 derniers messages)
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val role = if (msg.isFromUser) "user" else "model"
                val msgObj = JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                }
                contentsArray.put(msgObj)
            }

            // Message actuel de l'utilisateur avec contexte du document
            val currentMsgObj = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            }
            contentsArray.put(currentMsgObj)

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1200)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "Pas de réponse textuelle reçue.")
                    }
                }
            }

            Log.w(TAG, "Gemini API error code ${response.code}: $responseBody")
            return@withContext generateOfflineEducationalResponse(documentTitle, documentType, documentNiveau, userMessage)

        } catch (e: Exception) {
            Log.e(TAG, "Error querying Gemini API", e)
            return@withContext generateOfflineEducationalResponse(documentTitle, documentType, documentNiveau, userMessage)
        }
    }

    private fun generateOfflineEducationalResponse(
        documentTitle: String,
        documentType: String,
        documentNiveau: String,
        userQuery: String
    ): String {
        val q = userQuery.lowercase()
        return when {
            q.contains("résum") || q.contains("resume") || q.contains("point") -> {
                """
                📚 **Synthèse Pédagogique : $documentTitle** ($documentNiveau - $documentType)

                **1. Notions Fondamentales :**
                • Compréhension globale des définitions et théorèmes majeurs abordés dans cette unité d'enseignement.
                • Articulation entre la théorie fondamentale et les cas pratiques exigés aux examens de l'Université de Garoua.

                **2. Points d'Attention & Formules Clés :**
                • Maîtrise des conditions initiales et des hypothèses de validité des théorèmes.
                • Méthodes de résolution étape par étape recommandées pour les contrôles continus.

                **3. Conseil de Révision :**
                Relisez les exercices types et entraînez-vous à refaire les démonstrations sans regarder les notes.
                """.trimIndent()
            }
            q.contains("question") || q.contains("examen") || q.contains("sujet") || q.contains("epreuve") -> {
                """
                ❓ **Questions d'Entraînement Types pour : $documentTitle ($documentNiveau)**

                **Question 1 (Notion théorique - 4 pts) :**
                Définir les concepts centraux abordés dans ce document et expliciter leurs propriétés principales.

                **Question 2 (Application directe - 6 pts) :**
                Appliquer le principe directeur à un exemple concret et commenter le résultat obtenu.

                **Question 3 (Problème de synthèse - 10 pts) :**
                Analyser les limites du modèle et proposer une solution alternative adaptée au contexte scientifique actuel.

                💡 *Conseil : Prenez 15 minutes pour rédiger vos réponses au brouillon avant de vérifier.*
                """.trimIndent()
            }
            q.contains("quiz") || q.contains("qcm") -> {
                """
                🎯 **Mini-Quiz de Validation : $documentTitle**

                **Q1 : Quel est l'objectif premier abordé dans ce chapitre ?**
                A) L'analyse descriptive
                B) La modélisation prédictive
                C) La vérification expérimentale
                *(Indice : revoir la première partie du cours)*

                **Q2 : Quelle démarche méthodologique est préconisée aux examens ?**
                A) Application directe sans justification
                B) Énonciation des hypothèses, calcul puis interprétation
                C) Mémorisation brute sans analyse critique

                Envoyez-moi vos choix de réponses pour que nous les validions ensemble !
                """.trimIndent()
            }
            else -> {
                """
                🎓 **Explication Pédagogique - $documentTitle ($documentNiveau)**

                En réponse à votre question concernant *« $userQuery »* :

                Dans le cadre du programme de la Faculté des Sciences (Université de Garoua), ce point se traite en respectant trois règles :
                1. **Vérifier les prérequis :** S'assurer de la bonne compréhension des définitions antérieures.
                2. **Isoler les variables clées :** Décomposer le problème en sous-étapes logiques.
                3. **Valider les résultats :** Comparer avec les grandeurs d'ordres attendues et les unités physiques/mathématiques.

                Avez-vous besoin d'une clarification sur une étape de calcul spécifique ou d'un exemple détaillé ?
                """.trimIndent()
            }
        }
    }
}
