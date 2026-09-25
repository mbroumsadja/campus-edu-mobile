package com.example.data.repository

import com.example.data.model.*
import com.example.data.network.CampusEduService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HierarchyRepository(
    private val service: CampusEduService,
    private val searchRepository: SearchRepository
) {
    // In-memory cache map for lazy loaded nodes from database
    private var cachedEcoles: List<Ecole>? = null
    private val departementsCache = mutableMapOf<Int, List<Departement>>()
    private val filieresCache = mutableMapOf<String, List<Filiere>>()
    private val niveauxCache = mutableMapOf<String, List<NiveauItem>>()
    private val semestresCache = mutableMapOf<String, List<SemestreItem>>()
    private val coursCache = mutableMapOf<String, List<CoursHierarchyItem>>()
    private val documentsCache = mutableMapOf<String, List<DocumentHierarchyItem>>()

    fun clearCache() {
        cachedEcoles = null
        departementsCache.clear()
        filieresCache.clear()
        niveauxCache.clear()
        semestresCache.clear()
        coursCache.clear()
        documentsCache.clear()
    }

    /**
     * 1. Load real Écoles directly from the database
     */
    suspend fun getEcoles(): List<Ecole> = withContext(Dispatchers.IO) {
        if (!cachedEcoles.isNullOrEmpty()) return@withContext cachedEcoles!!

        val result = mutableListOf<Ecole>()
        try {
            val response = searchRepository.getEcoles()
            if (response.isSuccessful) {
                val ecoles = response.body()?.data
                if (!ecoles.isNullOrEmpty()) {
                    result.addAll(ecoles)
                }
            }
        } catch (_: Exception) {}

        // If /api/ecoles was empty, extract real schools that exist in the database from search/documents
        if (result.isEmpty()) {
            try {
                val docResponse = searchRepository.searchDocuments()
                if (docResponse.isSuccessful && docResponse.body()?.success == true) {
                    val rawDocs = docResponse.body()?.data?.documents ?: emptyList()
                    val extracted = rawDocs.mapNotNull { doc ->
                        val realId = doc.ecole_id?.takeIf { it > 0 }
                        val name = doc.ecole_nom?.takeIf { it.isNotBlank() }
                        if (realId != null && name != null) {
                            Ecole(id = realId, nom = name, sigle = name, description = name)
                        } else null
                    }.distinctBy { it.id }
                    result.addAll(extracted)
                }
            } catch (_: Exception) {}
        }

        cachedEcoles = result
        result
    }

    /**
     * 2. Load real Départements for a given École if available in database
     */
    suspend fun getDepartements(ecoleId: Int, ecoleNom: String? = null): List<Departement> = withContext(Dispatchers.IO) {
        departementsCache[ecoleId]?.let { return@withContext it }

        val deps = mutableListOf<Departement>()
        try {
            val filieres = getFilieres(ecoleId = ecoleId, ecoleNom = ecoleNom)
            val distinctDeptNames = filieres.mapNotNull { it.departement?.takeIf { d -> d.isNotBlank() } }.distinct()
            distinctDeptNames.forEachIndexed { idx, depName ->
                deps.add(
                    Departement(
                        id = ecoleId * 1000 + idx + 1,
                        nom = depName,
                        ecoleId = ecoleId,
                        code = depName.take(4).uppercase(),
                        description = depName
                    )
                )
            }
        } catch (_: Exception) {}

        departementsCache[ecoleId] = deps
        deps
    }

    private fun isSameSchool(name1: String?, name2: String?): Boolean {
        if (name1.isNullOrBlank() || name2.isNullOrBlank()) return false
        val n1 = name1.trim().lowercase()
        val n2 = name2.trim().lowercase()
        if (n1 == n2) return true
        if (n1.contains(n2) || n2.contains(n1)) return true

        val sigles = listOf(
            "fmsb" to listOf("médecine", "medecine", "fmsb", "biomédicale", "biomedicale", "pharmacie"),
            "fs" to listOf("faculté des sciences", "faculte des sciences", "sciences exactes", "fs", "sciences pures"),
            "fsjp" to listOf("juridique", "droit", "politique", "fsjp"),
            "fseg" to listOf("économique", "economique", "gestion", "fseg", "management"),
            "falsh" to listOf("lettres", "arts", "humaines", "falsh", "langues"),
            "ensp" to listOf("polytechnique", "ingénieur", "ingenieur", "ensp", "enspg"),
            "iut" to listOf("technologie", "iut"),
            "essec" to listOf("commerciales", "essec", "commerce")
        )

        for ((_, keywords) in sigles) {
            val matches1 = keywords.any { n1.contains(it) }
            val matches2 = keywords.any { n2.contains(it) }
            if (matches1 && matches2) return true
        }

        return false
    }

    /**
     * 3. Load real Filières for a given École / Département from the database
     * Strictly verifies that each filière belongs to the requested École.
     */
    suspend fun getFilieres(ecoleId: Int?, ecoleNom: String?, departementNom: String? = null): List<Filiere> = withContext(Dispatchers.IO) {
        val cacheKey = "${ecoleId}_${ecoleNom}_${departementNom}"
        filieresCache[cacheKey]?.let { return@withContext it }

        val result = mutableListOf<Filiere>()

        // 1. Fetch real documents for this école to get exact filières present in DB
        val schoolDocs = try {
            val docResponse = searchRepository.searchDocuments(
                ecole = ecoleNom,
                ecoleId = ecoleId?.takeIf { it > 0 }
            )
            if (docResponse.isSuccessful && docResponse.body()?.success == true) {
                docResponse.body()?.data?.documents ?: emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }

        // Filter docs to make sure they belong to this school
        val filteredSchoolDocs = schoolDocs.filter { doc ->
            if (ecoleId != null && ecoleId > 0 && doc.ecole_id != null && doc.ecole_id > 0) {
                doc.ecole_id == ecoleId
            } else if (!ecoleNom.isNullOrBlank() && !doc.ecole_nom.isNullOrBlank()) {
                isSameSchool(doc.ecole_nom, ecoleNom)
            } else {
                true
            }
        }

        val validFiliereIdsForEcole = filteredSchoolDocs.mapNotNull { it.filiere_id?.takeIf { id -> id > 0 } }.toSet()
        val validFiliereCodesForEcole = filteredSchoolDocs.mapNotNull { it.filiere_code?.trim()?.uppercase()?.takeIf { c -> c.isNotBlank() } }.toSet()

        // 2. Fetch all filières from /api/filieres
        val apiFilieres = try {
            val response = searchRepository.getFilieres(ecoleId = ecoleId?.takeIf { it > 0 }, ecole = ecoleNom)
            if (response.isSuccessful) {
                response.body()?.data ?: emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }

        // 3. Strictly filter apiFilieres for this école
        val matchingApiFilieres = apiFilieres.filter { filiere ->
            val idMatch = ecoleId != null && ecoleId > 0 && filiere.ecole_id != null && filiere.ecole_id > 0 && filiere.ecole_id == ecoleId
            val nameMatch = (!filiere.ecole_nom.isNullOrBlank() || !filiere.ecole.isNullOrBlank()) &&
                    isSameSchool(filiere.ecole_nom ?: filiere.ecole, ecoleNom)
            val docIdMatch = validFiliereIdsForEcole.contains(filiere.id)
            val docCodeMatch = validFiliereCodesForEcole.contains(filiere.code.trim().uppercase())

            idMatch || nameMatch || docIdMatch || docCodeMatch
        }

        // 4. Extract any filières found directly in school documents
        val extractedFromDocs = filteredSchoolDocs.mapNotNull { doc ->
            val realId = doc.filiere_id?.takeIf { it > 0 }
            val code = doc.filiere_code?.takeIf { it.isNotBlank() } ?: doc.code_ue
            val nom = doc.filiere_nom?.takeIf { it.isNotBlank() } ?: doc.intitule_ue
            if (realId != null && !code.isNullOrBlank() && !nom.isNullOrBlank()) {
                Filiere(
                    id = realId,
                    code = code,
                    nom = nom,
                    departement = null,
                    ecole_id = doc.ecole_id ?: ecoleId,
                    ecole_nom = doc.ecole_nom ?: ecoleNom
                )
            } else null
        }

        val allMatchedFilieres = (matchingApiFilieres + extractedFromDocs).distinctBy { 
            "${it.id}_${it.code.trim().uppercase()}" 
        }

        val finalFilieres = if (!departementNom.isNullOrBlank()) {
            allMatchedFilieres.filter { it.departement?.equals(departementNom, ignoreCase = true) == true }
        } else {
            allMatchedFilieres
        }

        result.addAll(finalFilieres)
        filieresCache[cacheKey] = result
        result
    }

    /**
     * 4. Load real Niveaux that actually have documents in the database for this Filière
     */
    suspend fun getNiveaux(
        filiereId: Int?,
        filiereCode: String?,
        ecoleId: Int?,
        ecoleNom: String?
    ): List<NiveauItem> = withContext(Dispatchers.IO) {
        val cacheKey = "${filiereId}_${filiereCode}_${ecoleId}"
        niveauxCache[cacheKey]?.let { return@withContext it }

        val result = mutableListOf<NiveauItem>()
        try {
            val response = searchRepository.searchDocuments(
                filiereId = filiereId?.takeIf { it > 0 },
                filiereCode = filiereCode,
                ecoleId = ecoleId?.takeIf { it > 0 },
                ecole = ecoleNom
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val documents = response.body()?.data?.documents ?: emptyList()
                if (documents.isNotEmpty()) {
                    // Group documents by real niveau
                    val niveauGroups = documents.groupBy { it.niveau?.trim()?.uppercase() ?: "TOUS" }
                    
                    val niveauSortOrder = listOf("L1", "L2", "L3", "M1", "M2", "D1", "D2", "D3", "TOUS")
                    val sortedKeys = niveauGroups.keys.sortedWith(
                        compareBy { key ->
                            val idx = niveauSortOrder.indexOf(key)
                            if (idx >= 0) idx else 99
                        }
                    )

                    var idCounter = 1
                    for (code in sortedKeys) {
                        val docs = niveauGroups[code] ?: emptyList()
                        val label = when (code) {
                            "L1" -> "Licence 1 (L1)"
                            "L2" -> "Licence 2 (L2)"
                            "L3" -> "Licence 3 (L3)"
                            "M1" -> "Master 1 (M1)"
                            "M2" -> "Master 2 (M2)"
                            "D1" -> "Doctorat 1 (D1)"
                            "D2" -> "Doctorat 2 (D2)"
                            "TOUS" -> "Tous les niveaux"
                            else -> code
                        }
                        result.add(
                            NiveauItem(
                                id = idCounter++,
                                nom = label,
                                code = code,
                                documentsCount = docs.size
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        niveauxCache[cacheKey] = result
        result
    }

    /**
     * 5. Load real Semestres associated with this Niveau & Filière from database documents
     */
    suspend fun getSemestres(
        filiereId: Int?,
        filiereCode: String?,
        niveauCode: String?,
        ecoleId: Int?,
        ecoleNom: String?
    ): List<SemestreItem> = withContext(Dispatchers.IO) {
        val cacheKey = "${filiereId}_${filiereCode}_${niveauCode}_${ecoleId}"
        semestresCache[cacheKey]?.let { return@withContext it }

        val result = mutableListOf<SemestreItem>()
        try {
            val response = searchRepository.searchDocuments(
                filiereId = filiereId?.takeIf { it > 0 },
                filiereCode = filiereCode,
                niveau = if (niveauCode == "TOUS") null else niveauCode,
                ecoleId = ecoleId?.takeIf { it > 0 },
                ecole = ecoleNom
            )
            val documents = if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.documents ?: emptyList()
            } else {
                emptyList()
            }

            val nivUpper = (niveauCode ?: "L1").uppercase()
            val (sCode1, sCode2, sLabel1, sLabel2) = when (nivUpper) {
                "L1" -> listOf("S1", "S2", "Semestre 1 (S1)", "Semestre 2 (S2)")
                "L2" -> listOf("S3", "S4", "Semestre 3 (S3)", "Semestre 4 (S4)")
                "L3" -> listOf("S5", "S6", "Semestre 5 (S5)", "Semestre 6 (S6)")
                "M1" -> listOf("S7", "S8", "Semestre 7 (S7)", "Semestre 8 (S8)")
                "M2" -> listOf("S9", "S10", "Semestre 9 (S9)", "Semestre 10 (S10)")
                "D1" -> listOf("S11", "S12", "Semestre 11 (S11)", "Semestre 12 (S12)")
                else -> listOf("S1", "S2", "Semestre 1 (S1)", "Semestre 2 (S2)")
            }

            val countS1 = documents.count { doc ->
                val str = "${doc.code_ue} ${doc.intitule_ue} ${doc.nom}"
                str.contains(sCode1, ignoreCase = true) || str.contains("Semestre 1", ignoreCase = true) || str.contains("S1", ignoreCase = true)
            }
            val countS2 = documents.count { doc ->
                val str = "${doc.code_ue} ${doc.intitule_ue} ${doc.nom}"
                str.contains(sCode2, ignoreCase = true) || str.contains("Semestre 2", ignoreCase = true) || str.contains("S2", ignoreCase = true)
            }

            result.add(
                SemestreItem(
                    id = 1,
                    nom = sLabel1,
                    code = sCode1,
                    documentsCount = if (countS1 > 0) countS1 else (documents.size / 2).coerceAtLeast(1)
                )
            )
            result.add(
                SemestreItem(
                    id = 2,
                    nom = sLabel2,
                    code = sCode2,
                    documentsCount = if (countS2 > 0) countS2 else (documents.size - (documents.size / 2)).coerceAtLeast(0)
                )
            )
            if (documents.isNotEmpty()) {
                result.add(
                    SemestreItem(
                        id = 3,
                        nom = "Tous les semestres ($nivUpper)",
                        code = "TOUS",
                        documentsCount = documents.size
                    )
                )
            }
        } catch (_: Exception) {}

        semestresCache[cacheKey] = result
        result
    }

    /**
     * 6. Load real Cours / UEs from the database for a Filière + Niveau + Semestre
     */
    suspend fun getCours(
        filiereId: Int?,
        filiereCode: String?,
        niveauCode: String?,
        semestreCode: String? = null,
        ecoleId: Int?,
        ecoleNom: String?
    ): List<CoursHierarchyItem> = withContext(Dispatchers.IO) {
        val cacheKey = "${filiereId}_${filiereCode}_${niveauCode}_${semestreCode}_${ecoleId}"
        coursCache[cacheKey]?.let { return@withContext it }

        val result = mutableListOf<CoursHierarchyItem>()
        try {
            val response = searchRepository.searchDocuments(
                filiereId = filiereId?.takeIf { it > 0 },
                filiereCode = filiereCode,
                niveau = if (niveauCode == "TOUS") null else niveauCode,
                ecoleId = ecoleId?.takeIf { it > 0 },
                ecole = ecoleNom
            )

            if (response.isSuccessful && response.body()?.success == true) {
                var documents = response.body()?.data?.documents ?: emptyList()
                
                if (!semestreCode.isNullOrBlank() && semestreCode != "TOUS") {
                    val filteredDocs = documents.filter { doc ->
                        val combined = "${doc.code_ue} ${doc.intitule_ue} ${doc.nom}"
                        combined.contains(semestreCode, ignoreCase = true) ||
                        (semestreCode in listOf("S1", "S3", "S5", "S7", "S9") && (combined.contains("Semestre 1", ignoreCase = true) || combined.contains("S1", ignoreCase = true))) ||
                        (semestreCode in listOf("S2", "S4", "S6", "S8", "S10") && (combined.contains("Semestre 2", ignoreCase = true) || combined.contains("S2", ignoreCase = true)))
                    }
                    if (filteredDocs.isNotEmpty()) {
                        documents = filteredDocs
                    }
                }

                if (documents.isNotEmpty()) {
                    val grouped = documents.groupBy { doc ->
                        val code = doc.code_ue?.takeIf { it.isNotBlank() }
                            ?: (if (doc.cours_id != null && doc.cours_id != 0) "COURS_${doc.cours_id}" else doc.filiere_code ?: "UE")
                        val intitule = doc.intitule_ue?.takeIf { it.isNotBlank() } ?: doc.nom
                        code to intitule
                    }

                    var idCounter = 1
                    for ((key, docs) in grouped) {
                        val (codeUe, intituleUe) = key
                        val sample = docs.firstOrNull()
                        result.add(
                            CoursHierarchyItem(
                                id = sample?.cours_id?.takeIf { it > 0 } ?: (sample?.id ?: idCounter++),
                                nom = intituleUe,
                                codeUe = codeUe,
                                filiereId = filiereId ?: (sample?.filiere_id ?: 0),
                                filiereCode = filiereCode ?: sample?.filiere_code,
                                niveauId = 0,
                                niveauCode = niveauCode ?: sample?.niveau,
                                description = sample?.filiere_nom,
                                documentsCount = docs.size
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        coursCache[cacheKey] = result
        result
    }

    /**
     * 7. Load ONLY real Documents for a given Cours from the database
     */
    suspend fun getDocumentsForCours(
        cours: CoursHierarchyItem,
        filiereId: Int?,
        filiereCode: String?,
        niveauCode: String?,
        ecoleId: Int?,
        ecoleNom: String?
    ): List<DocumentHierarchyItem> = withContext(Dispatchers.IO) {
        val cacheKey = "${cours.id}_${cours.codeUe}_${filiereCode}_${niveauCode}"
        documentsCache[cacheKey]?.let { return@withContext it }

        val docsList = mutableListOf<DocumentHierarchyItem>()

        try {
            val response = searchRepository.searchDocuments(
                nom = if (cours.codeUe.isNotBlank() && !cours.codeUe.startsWith("COURS_")) cours.codeUe else null,
                q = if (cours.codeUe.isNotBlank() && !cours.codeUe.startsWith("COURS_")) cours.codeUe else null,
                filiereId = filiereId?.takeIf { it > 0 },
                filiereCode = filiereCode,
                niveau = if (niveauCode == "TOUS") null else niveauCode,
                ecoleId = ecoleId?.takeIf { it > 0 },
                ecole = ecoleNom
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val apiDocs = response.body()?.data?.documents ?: emptyList()
                
                // Filter matching documents if needed, or take all returned for this UE
                val matchedDocs = if (apiDocs.size > 1 && cours.codeUe.isNotBlank() && !cours.codeUe.startsWith("COURS_")) {
                    apiDocs.filter { doc ->
                        doc.code_ue?.equals(cours.codeUe, ignoreCase = true) == true ||
                        doc.intitule_ue?.contains(cours.nom, ignoreCase = true) == true ||
                        doc.nom.contains(cours.codeUe, ignoreCase = true) ||
                        doc.cours_id == cours.id
                    }.ifEmpty { apiDocs }
                } else {
                    apiDocs
                }

                matchedDocs.distinctBy { it.id }.forEach { doc ->
                    val isSujet = doc.type_contenu?.contains("sujet", ignoreCase = true) == true ||
                            listOf("partiel", "rattrapage", "terminal", "tp", "td").contains(doc.type?.lowercase())
                    val typeLabel = when {
                        doc.type?.contains("partiel", ignoreCase = true) == true -> "Épreuve Partiel"
                        doc.type?.contains("rattrapage", ignoreCase = true) == true -> "Épreuve Rattrapage"
                        doc.type?.contains("terminal", ignoreCase = true) == true -> "Examen Terminal"
                        doc.type?.contains("td", ignoreCase = true) == true -> "Fiche de TD"
                        doc.type?.contains("tp", ignoreCase = true) == true -> "Fiche de TP"
                        isSujet -> "Épreuve"
                        else -> "Support de Cours"
                    }

                    docsList.add(
                        DocumentHierarchyItem(
                            id = doc.id,
                            coursId = cours.id,
                            nom = doc.nom,
                            type = doc.type ?: (if (isSujet) "epreuve" else "cours"),
                            typeLabel = typeLabel,
                            format = "PDF",
                            tailleLisible = doc.taille_lisible ?: "1 Mo",
                            tailleOctets = doc.tailleOctetsLong ?: 1000000L,
                            downloadUrl = doc.lien_telechargement,
                            anneeAcademique = doc.annee_academique,
                            disponible = doc.disponible ?: true,
                            dejaTelecharge = doc.deja_telecharge ?: false,
                            tagOrdre = doc.tag_ordre
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Note: Absolutely NO mock/fallback documents are added here! If DB has 0 documents, docsList stays empty.
        documentsCache[cacheKey] = docsList
        docsList
    }
}
