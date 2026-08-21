package com.example.ui.search

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.network.NetworkModule
import com.example.data.network.TokenProvider
import com.example.data.repository.DownloadRepository
import com.example.data.repository.DownloadStatus
import com.example.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Empty(val queryOrFilter: String? = null) : SearchUiState()
    data class NetworkError(val message: String) : SearchUiState()
    data class ServerError(val code: Int?, val message: String) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
    data class Success(val results: List<UeWithDocuments>) : SearchUiState()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val service = NetworkModule.createService(application)
    private val searchRepository = SearchRepository(service)
    private val downloadRepository = DownloadRepository(service)
    private val tokenProvider = TokenProvider(application)

    // Search & Filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFiliere = MutableStateFlow<Filiere?>(null)
    val selectedFiliere = _selectedFiliere.asStateFlow()

    private val _selectedEcole = MutableStateFlow<Ecole?>(null)
    val selectedEcole = _selectedEcole.asStateFlow()

    private val _selectedNiveau = MutableStateFlow("Tous")
    val selectedNiveau = _selectedNiveau.asStateFlow()

    private val _selectedType = MutableStateFlow("Tous")
    val selectedType = _selectedType.asStateFlow()

    private val _selectedAnnee = MutableStateFlow("Toutes")
    val selectedAnnee = _selectedAnnee.asStateFlow()

    // Loaded Filières & Écoles
    private val _filieres = MutableStateFlow<List<Filiere>>(emptyList())
    val filieres = _filieres.asStateFlow()

    private val _ecoles = MutableStateFlow<List<Ecole>>(emptyList())
    val ecoles = _ecoles.asStateFlow()

    // Results State & Count
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Network Loading indicator
    private val _isNetworkLoading = MutableStateFlow(false)
    val isNetworkLoading = _isNetworkLoading.asStateFlow()

    private val _resultCount = MutableStateFlow<Int?>(null)
    val resultCount = _resultCount.asStateFlow()

    // Pagination
    private val _pagination = MutableStateFlow<Pagination?>(null)
    val pagination = _pagination.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage = _currentPage.asStateFlow()

    // Access Token management
    private val _accessToken = MutableStateFlow("")
    val accessToken = _accessToken.asStateFlow()

    // Map of downloaded document status
    private val _downloadedDocuments = MutableStateFlow<Set<String>>(emptySet())
    val downloadedDocuments = _downloadedDocuments.asStateFlow()

    private val _allLocalFiles = MutableStateFlow<List<com.example.data.repository.LocalDownloadedDocument>>(emptyList())
    val allLocalFiles = _allLocalFiles.asStateFlow()

    // Map of active download jobs and statuses
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val activeDownloads = _activeDownloads.asStateFlow()

    // Bottom Sheet / Dialog trigger for authentication
    private val _showTokenDialog = MutableStateFlow(false)
    val showTokenDialog = _showTokenDialog.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Load stored token
        _accessToken.value = tokenProvider.getAccessToken() ?: ""
        
        // Fetch Filières and Écoles directly from API
        loadFilieres()
        loadEcoles()

        // Debounce input and combine with filters to trigger real-time searches
        viewModelScope.launch {
            combine(
                _searchQuery.debounce(300),
                _selectedFiliere,
                _selectedEcole,
                _selectedNiveau,
                combine(_selectedType, _selectedAnnee) { type, annee -> type to annee }
            ) { query, filiere, ecole, niveau, typeAndAnnee ->
                val (type, annee) = typeAndAnnee
                _currentPage.value = 1
                performSearch(query, filiere?.id, filiere?.code, ecole?.id, ecole?.nom, niveau, type, annee, page = 1, append = false)
            }.collect()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFiliere(filiere: Filiere?) {
        _selectedFiliere.value = filiere
    }

    fun selectEcole(ecole: Ecole?) {
        _selectedEcole.value = ecole
    }

    fun selectNiveau(niveau: String) {
        _selectedNiveau.value = niveau
    }

    fun selectType(type: String) {
        _selectedType.value = type
    }

    fun selectAnnee(annee: String) {
        _selectedAnnee.value = annee
    }

    fun setAccessToken(token: String) {
        _accessToken.value = token
        tokenProvider.setAccessToken(token)
        loadFilieres()
        loadEcoles()
    }

    fun setShowTokenDialog(show: Boolean) {
        _showTokenDialog.value = show
    }

    private fun deduplicateAndMergeFilieres(currentList: List<Filiere>, newFilieres: List<Filiere>): List<Filiere> {
        val result = currentList.toMutableList()
        for (newFiliere in newFilieres) {
            val newCode = newFiliere.code.lowercase().trim()
            val newNom = newFiliere.nom.lowercase().trim()

            val isDuplicate = result.any { existing ->
                val exCode = existing.code.lowercase().trim()
                val exNom = existing.nom.lowercase().trim()

                (newCode.isNotEmpty() && exCode.isNotEmpty() && exCode == newCode) ||
                (newNom.isNotEmpty() && exNom.isNotEmpty() && (exNom == newNom || exNom.contains(newNom) || newNom.contains(exNom))) ||
                (newCode.isNotEmpty() && exNom == newCode) ||
                (exCode.isNotEmpty() && newNom == exCode)
            }

            if (!isDuplicate) {
                result.add(newFiliere)
            }
        }
        return result
    }

    fun loadFilieres() {
        viewModelScope.launch {
            _isNetworkLoading.value = true
            try {
                val response = searchRepository.getFilieres()
                if (response.isSuccessful) {
                    val apiFilieres = response.body()?.data ?: emptyList()
                    if (apiFilieres.isNotEmpty()) {
                        _filieres.value = deduplicateAndMergeFilieres(_filieres.value, apiFilieres)
                    }
                }
            } catch (_: Exception) { 
            } finally {
                if (searchJob?.isActive != true) {
                    _isNetworkLoading.value = false
                }
            }
        }
    }

    private fun deduplicateAndMergeEcoles(currentList: List<Ecole>, newEcoles: List<Ecole>): List<Ecole> {
        val result = currentList.toMutableList()
        for (newEcole in newEcoles) {
            val newSigle = newEcole.sigle?.lowercase()?.trim() ?: ""
            val newNom = newEcole.nom.lowercase().trim()

            val isDuplicate = result.any { existing ->
                val exSigle = existing.sigle?.lowercase()?.trim() ?: ""
                val exNom = existing.nom.lowercase().trim()

                (newSigle.isNotEmpty() && exSigle.isNotEmpty() && exSigle == newSigle) ||
                (newNom.isNotEmpty() && exNom.isNotEmpty() && (exNom == newNom || exNom.contains(newNom) || newNom.contains(exNom))) ||
                (newSigle.isNotEmpty() && exNom == newSigle) ||
                (exSigle.isNotEmpty() && newNom == exSigle)
            }

            if (!isDuplicate) {
                result.add(newEcole)
            }
        }
        return result
    }

    fun loadEcoles() {
        viewModelScope.launch {
            _isNetworkLoading.value = true
            try {
                val response = searchRepository.getEcoles()
                if (response.isSuccessful) {
                    val apiEcoles = response.body()?.data ?: emptyList()
                    if (apiEcoles.isNotEmpty()) {
                        _ecoles.value = deduplicateAndMergeEcoles(_ecoles.value, apiEcoles)
                    }
                }
            } catch (_: Exception) { 
            } finally {
                if (searchJob?.isActive != true) {
                    _isNetworkLoading.value = false
                }
            }
        }
    }

    fun performSearch(
        query: String = _searchQuery.value,
        filiereId: Int? = _selectedFiliere.value?.id?.takeIf { it > 0 },
        filiereCode: String? = _selectedFiliere.value?.code,
        ecoleId: Int? = _selectedEcole.value?.id?.takeIf { it > 0 },
        ecoleName: String? = _selectedEcole.value?.nom,
        niveau: String = _selectedNiveau.value,
        type: String = _selectedType.value,
        annee: String = _selectedAnnee.value,
        page: Int = _currentPage.value,
        append: Boolean = false
    ) {
        searchJob?.cancel()

        val isQueryActive = query.isNotBlank()
        val isFiliereActive = filiereId != null || !filiereCode.isNullOrBlank()
        val isEcoleActive = ecoleId != null || !ecoleName.isNullOrBlank()
        val isNiveauActive = niveau.isNotBlank() && !niveau.equals("Tous", ignoreCase = true)
        val isTypeActive = type.isNotBlank() && !type.equals("Tous", ignoreCase = true)
        val isAnneeActive = annee.isNotBlank() && !annee.equals("Toutes", ignoreCase = true) && !annee.equals("Tous", ignoreCase = true)

        val isAnyCriteriaActive = isQueryActive || isFiliereActive || isEcoleActive || isNiveauActive || isTypeActive || isAnneeActive

        if (!isAnyCriteriaActive) {
            _uiState.value = SearchUiState.Idle
            _resultCount.value = null
            _pagination.value = null
            _isNetworkLoading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isNetworkLoading.value = true
            if (!append) {
                _uiState.value = SearchUiState.Loading
            }

            try {
                val formattedNiveau = if (niveau.isBlank() || niveau.equals("Tous", ignoreCase = true)) null else niveau
                val formattedType = if (type.isBlank() || type.equals("Tous", ignoreCase = true)) null else type
                val formattedAnnee = if (annee.isBlank() || annee.equals("Toutes", ignoreCase = true) || annee.equals("Tous", ignoreCase = true)) null else annee

                // Endpoint GET /api/search/documents
                val apiResponse = searchRepository.searchDocuments(
                    nom = if (query.isNotBlank()) query.trim() else null,
                    q = if (query.isNotBlank()) query.trim() else null,
                    niveau = formattedNiveau,
                    filiereCode = filiereCode,
                    filiereId = filiereId,
                    ecole = ecoleName,
                    ecoleId = ecoleId,
                    annee = formattedAnnee,
                    type = formattedType
                )

                if (apiResponse.isSuccessful && apiResponse.body()?.success == true) {
                    val searchData = apiResponse.body()?.data
                    val rawDocs = searchData?.documents ?: emptyList()
                    _pagination.value = null // /api/search/documents returns all items in one call

                    updateEcolesAndFilieresFromDocuments(rawDocs)

                    val convertedUes = mapDocumentsToUes(rawDocs)

                    val filteredResults = applyClientSideFiltering(
                        results = convertedUes,
                        niveau = niveau,
                        type = type,
                        query = query
                    )

                    val actualDocCount = filteredResults.sumOf { (it.cours?.size ?: 0) + (it.sujets?.size ?: 0) }
                    _resultCount.value = actualDocCount

                    if (filteredResults.isEmpty()) {
                        _uiState.value = SearchUiState.Empty(query)
                    } else {
                        _uiState.value = SearchUiState.Success(filteredResults)
                        updateDownloadedFilesSet(filteredResults)
                    }
                } else {
                    val code = apiResponse.code()
                    val errMsg = apiResponse.body()?.message ?: "Aucun document trouvé."
                    if (code in 500..599) {
                        _uiState.value = SearchUiState.ServerError(code, "Erreur Serveur HTTP $code : Le serveur ne peut pas traiter la demande.")
                    } else {
                        _uiState.value = SearchUiState.Error(errMsg)
                    }
                }
            } catch (e: Exception) {
                val causeMsg = e.cause?.message ?: e.message ?: ""
                val isNetworkErr = e is java.io.IOException ||
                        e is java.net.UnknownHostException ||
                        e is java.net.SocketTimeoutException ||
                        e is java.net.ConnectException ||
                        causeMsg.contains("Unable to resolve host", ignoreCase = true) ||
                        causeMsg.contains("Failed to connect", ignoreCase = true) ||
                        causeMsg.contains("timeout", ignoreCase = true)

                if (isNetworkErr) {
                    _uiState.value = SearchUiState.NetworkError("Erreur Réseau : Impossible d'accéder aux serveurs de Campus Edu. Vérifiez votre connexion Internet.")
                } else {
                    _uiState.value = SearchUiState.Error("Erreur : " + (e.localizedMessage ?: "Problème lors du chargement des données."))
                }
            } finally {
                _isNetworkLoading.value = false
            }
        }
    }

    private fun mapDocumentsToUes(documents: List<ApiDocumentItem>): List<UeWithDocuments> {
        val grouped = documents.groupBy { doc ->
            val code = doc.code_ue 
                ?: (if (doc.cours_id != null && doc.cours_id != 0) "COURS_${doc.cours_id}" else doc.filiere_code ?: "GEN")
            val intitule = doc.intitule_ue ?: doc.filiere_nom ?: "Documents académiques"
            code to intitule
        }

        var idCounter = 1
        return grouped.map { (key, docList) ->
            val (ueCode, ueIntitule) = key
            val sampleDoc = docList.firstOrNull()

            val coursList = mutableListOf<Cours>()
            val sujetsList = mutableListOf<Sujet>()

            // Sort documents by tag_ordre (if available) then by id
            val sortedDocs = docList.sortedWith(
                compareBy<ApiDocumentItem> { it.tag_ordre ?: "999" }
                    .thenBy { it.id }
            )

            sortedDocs.forEach { doc ->
                val isSujet = doc.type_contenu?.contains("sujet", ignoreCase = true) == true ||
                        listOf("partiel", "rattrapage", "terminal", "tp", "td").contains(doc.type?.lowercase())

                val docAnnee = doc.annee_academique ?: doc.annee?.toString()
                if (isSujet) {
                    sujetsList.add(
                        Sujet(
                            id = doc.id,
                            titre = doc.nom,
                            type = doc.type ?: "partiel",
                            session = "Normale",
                            annee = docAnnee,
                            cheminFichier = doc.lien_telechargement,
                            lienTelechargement = doc.lien_telechargement,
                            avecCorrige = false,
                            cheminCorrige = null,
                            lienCorrigeTelechargement = null,
                            statut = "publie",
                            telechargemements = doc.telechargements ?: 0,
                            ue_id = idCounter,
                            enseignant_id = 0,
                            deja_telecharge = doc.deja_telecharge ?: false,
                            disponible = doc.disponible ?: true
                        )
                    )
                } else {
                    val descParts = listOfNotNull(
                        doc.tag_ordre?.takeIf { it.isNotBlank() }?.let { "Ordre: $it" },
                        doc.filiere_nom ?: doc.ecole_nom,
                        doc.niveau,
                        doc.taille_lisible
                    )
                    coursList.add(
                        Cours(
                            id = doc.id,
                            titre = doc.nom,
                            description = descParts.joinToString(" • "),
                            type = doc.type ?: "pdf",
                            cheminFichier = doc.lien_telechargement,
                            lienTelechargement = doc.lien_telechargement,
                            nomFichierOriginal = doc.nom,
                            tailleFichier = doc.taille_octets,
                            statut = "publie",
                            anneAcademique = docAnnee,
                            vues = 0,
                            telechargemements = doc.telechargements ?: 0,
                            ue_id = idCounter,
                            enseignant_id = 0,
                            deja_telecharge = doc.deja_telecharge ?: false,
                            disponible = doc.disponible ?: true
                        )
                    )
                }
            }

            UeWithDocuments(
                id = idCounter++,
                code = ueCode,
                intitule = ueIntitule,
                niveau = sampleDoc?.niveau ?: "L1",
                cours = coursList,
                sujets = sujetsList
            )
        }
    }

    fun loadMore() {
        val pag = _pagination.value ?: return
        if (pag.hasNext && uiState.value is SearchUiState.Success) {
            val nextPage = pag.page + 1
            _currentPage.value = nextPage
            performSearch(page = nextPage, append = true)
        }
    }

    private fun mergeUeResults(currentList: List<UeWithDocuments>, newList: List<UeWithDocuments>): List<UeWithDocuments> {
        val map = currentList.associateBy { it.id }.toMutableMap()
        for (newUe in newList) {
            val existingUe = map[newUe.id]
            if (existingUe != null) {
                val updatedCours = (existingUe.cours ?: emptyList()) + (newUe.cours ?: emptyList())
                val updatedSujets = (existingUe.sujets ?: emptyList()) + (newUe.sujets ?: emptyList())
                map[newUe.id] = existingUe.copy(
                    cours = updatedCours.distinctBy { it.id },
                    sujets = updatedSujets.distinctBy { it.id }
                )
            } else {
                map[newUe.id] = newUe
            }
        }
        return map.values.toList()
    }

    private fun applyClientSideFiltering(
        results: List<UeWithDocuments>,
        niveau: String,
        type: String,
        query: String
    ): List<UeWithDocuments> {
        return results.mapNotNull { ue ->
            // 1. Filter by level (niveau) if specified and not "Tous"
            if (niveau.isNotBlank() && !niveau.equals("Tous", ignoreCase = true)) {
                val ueNiveau = ue.niveau ?: ""
                if (!ueNiveau.equals(niveau, ignoreCase = true)) {
                    return@mapNotNull null
                }
            }

            // 2. Filter cours and sujets by other criteria
            var filteredCours = ue.cours ?: emptyList()
            var filteredSujets = ue.sujets ?: emptyList()

            // Filter by type if specific type selected
            if (type.isNotBlank() && !type.equals("Tous", ignoreCase = true)) {
                val t = type.lowercase()
                if (t == "cours") {
                    filteredSujets = emptyList()
                } else if (t == "sujet" || t == "sujets") {
                    filteredCours = emptyList()
                } else {
                    filteredCours = filteredCours.filter { (it.type ?: "").equals(t, ignoreCase = true) }
                    filteredSujets = filteredSujets.filter { (it.type ?: "").equals(t, ignoreCase = true) }
                }
            }

            // Filter by query if query is not blank
            if (query.isNotBlank()) {
                val q = query.trim()
                val ueMatches = ue.code.contains(q, ignoreCase = true) || ue.intitule.contains(q, ignoreCase = true)
                
                if (!ueMatches) {
                    filteredCours = filteredCours.filter { c ->
                        c.titre.contains(q, ignoreCase = true) || (c.description ?: "").contains(q, ignoreCase = true)
                    }
                    filteredSujets = filteredSujets.filter { s ->
                        s.titre.contains(q, ignoreCase = true)
                    }
                }
            }

            // If we have neither cours nor sujets left, filter out the UE entirely
            if (filteredCours.isEmpty() && filteredSujets.isEmpty()) {
                null
            } else {
                ue.copy(cours = filteredCours, sujets = filteredSujets)
            }
        }
    }

    fun updateDownloadedFilesSet(results: List<UeWithDocuments>? = null) {
        val context = getApplication<Application>().applicationContext
        val localList = downloadRepository.getAllDownloadedFiles(context)
        _allLocalFiles.value = localList

        val currentSet = mutableSetOf<String>()
        val ues = results ?: (uiState.value as? SearchUiState.Success)?.results

        if (ues != null) {
            for (ue in ues) {
                ue.cours?.forEach { c ->
                    val file = downloadRepository.getLocalFileForCours(context, c.id)
                    if (file != null && file.exists()) {
                        currentSet.add("cours_${c.id}")
                    }
                }
                ue.sujets?.forEach { s ->
                    val fileSujet = downloadRepository.getLocalFileForSujet(context, s.id, isCorrige = false)
                    if (fileSujet != null && fileSujet.exists()) {
                        currentSet.add("sujet_${s.id}_sujet")
                    }
                    val fileCorrige = downloadRepository.getLocalFileForSujet(context, s.id, isCorrige = true)
                    if (fileCorrige != null && fileCorrige.exists()) {
                        currentSet.add("sujet_${s.id}_corrige")
                    }
                }
            }
        } else {
            // Fill currentSet from disk filenames directly
            localList.forEach { doc ->
                if (doc.name.startsWith("cours_")) {
                    val id = doc.name.substringAfter("cours_").substringBefore("_")
                    currentSet.add("cours_$id")
                } else if (doc.name.startsWith("sujet_")) {
                    val id = doc.name.substringAfter("sujet_").substringBefore("_")
                    currentSet.add("sujet_${id}_sujet")
                } else if (doc.name.startsWith("corrige_")) {
                    val id = doc.name.substringAfter("corrige_").substringBefore("_")
                    currentSet.add("sujet_${id}_corrige")
                }
            }
        }
        _downloadedDocuments.value = currentSet
    }

    fun openLocalFile(context: Context, localDoc: com.example.data.repository.LocalDownloadedDocument) {
        if (localDoc.file.exists()) {
            downloadRepository.openFile(context, localDoc.file)
        } else {
            Toast.makeText(context, "Le fichier n'a pas été trouvé localement.", Toast.LENGTH_SHORT).show()
            updateDownloadedFilesSet()
        }
    }

    fun deleteLocalFile(context: Context, localDoc: com.example.data.repository.LocalDownloadedDocument) {
        if (downloadRepository.deleteLocalFile(localDoc.file)) {
            Toast.makeText(context, "Fichier supprimé de la mémoire.", Toast.LENGTH_SHORT).show()
            updateDownloadedFilesSet()
        }
    }

    // Downloads handling
    fun downloadCours(context: Context, cours: Cours) {
        val key = "cours_${cours.id}"
        viewModelScope.launch {
            downloadRepository.downloadCours(context, cours.id, cours.titre, cours.downloadUrl).collect { status ->
                _activeDownloads.value = _activeDownloads.value + (key to status)
                if (status is DownloadStatus.Success) {
                    updateDownloadedFilesSet()
                }
            }
        }
    }

    fun downloadSujet(context: Context, sujet: Sujet, isCorrige: Boolean) {
        val key = if (isCorrige) "sujet_${sujet.id}_corrige" else "sujet_${sujet.id}_sujet"
        val downloadUrl = sujet.getDownloadUrlForCorrige(isCorrige)
        viewModelScope.launch {
            downloadRepository.downloadSujet(context, sujet.id, isCorrige, sujet.titre, downloadUrl).collect { status ->
                _activeDownloads.value = _activeDownloads.value + (key to status)
                if (status is DownloadStatus.Success) {
                    updateDownloadedFilesSet()
                }
            }
        }
    }

    fun openDownloadedCours(context: Context, cours: Cours) {
        val file = downloadRepository.getLocalFileForCours(context, cours.id)
        if (file != null && file.exists()) {
            downloadRepository.openFile(context, file)
        } else {
            Toast.makeText(context, "Le fichier n'a pas été trouvé localement.", Toast.LENGTH_SHORT).show()
            updateDownloadedFilesSet()
        }
    }

    fun openDownloadedSujet(context: Context, sujet: Sujet, isCorrige: Boolean) {
        val file = downloadRepository.getLocalFileForSujet(context, sujet.id, isCorrige)
        if (file != null && file.exists()) {
            downloadRepository.openFile(context, file)
        } else {
            Toast.makeText(context, "Le fichier n'a pas été trouvé localement.", Toast.LENGTH_SHORT).show()
            updateDownloadedFilesSet()
        }
    }

    fun deleteDownloadedCours(context: Context, cours: Cours) {
        val file = downloadRepository.getLocalFileForCours(context, cours.id)
        if (file != null) {
            downloadRepository.deleteLocalFile(file)
            _activeDownloads.value = _activeDownloads.value - "cours_${cours.id}"
            updateDownloadedFilesSet()
            Toast.makeText(context, "Cours supprimé de la mémoire.", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteDownloadedSujet(context: Context, sujet: Sujet, isCorrige: Boolean) {
        val file = downloadRepository.getLocalFileForSujet(context, sujet.id, isCorrige)
        if (file != null) {
            downloadRepository.deleteLocalFile(file)
            val key = if (isCorrige) "sujet_${sujet.id}_corrige" else "sujet_${sujet.id}_sujet"
            _activeDownloads.value = _activeDownloads.value - key
            updateDownloadedFilesSet()
            Toast.makeText(context, "Sujet supprimé de la mémoire.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEcolesAndFilieresFromDocuments(rawDocs: List<ApiDocumentItem>) {
        if (rawDocs.isEmpty()) return

        val extractedEcoles = rawDocs.mapNotNull { doc ->
            val realId = doc.ecole_id?.takeIf { it > 0 }
            val name = doc.ecole_nom?.takeIf { it.isNotBlank() }
            if (realId != null && name != null) {
                Ecole(id = realId, nom = name, sigle = name, description = name)
            } else null
        }.distinctBy { it.id }

        if (extractedEcoles.isNotEmpty()) {
            _ecoles.value = deduplicateAndMergeEcoles(_ecoles.value, extractedEcoles)
        }

        val extractedFilieres = rawDocs.mapNotNull { doc ->
            val realId = doc.filiere_id?.takeIf { it > 0 }
            val code = doc.filiere_code?.takeIf { it.isNotBlank() } ?: doc.code_ue
            val nom = doc.filiere_nom?.takeIf { it.isNotBlank() } ?: doc.intitule_ue
            if (realId != null && !code.isNullOrBlank() && !nom.isNullOrBlank()) {
                Filiere(id = realId, code = code, nom = nom, departement = null)
            } else null
        }.distinctBy { it.id }

        if (extractedFilieres.isNotEmpty()) {
            _filieres.value = deduplicateAndMergeFilieres(_filieres.value, extractedFilieres)
        }
    }
}
