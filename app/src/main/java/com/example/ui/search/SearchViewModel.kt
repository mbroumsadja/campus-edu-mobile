package com.example.ui.search

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.network.NetworkModule
import com.example.data.network.TokenProvider
import com.example.data.repository.DownloadRepository
import com.example.data.repository.DownloadStatus
import com.example.data.repository.HierarchyRepository
import com.example.data.repository.SearchRepository
import java.io.File
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

data class CatalogueStats(
    val totalCoursesAvailable: Int = 0,
    val totalSujetsAvailable: Int = 0,
    val totalCorrigesAvailable: Int = 0,
    val totalEcolesCovered: Int = 0,
    val totalFilieresCovered: Int = 0,
    val totalDownloadsOnPlatform: Int = 0,
    val platformAvailabilityRate: Int = 100
)

data class EcoleAvailabilityItem(
    val ecoleId: Int,
    val nom: String,
    val sigle: String,
    val coursesCount: Int,
    val sujetsCount: Int,
    val filieresCount: Int,
    val sampleUes: List<String> = emptyList()
)

data class FiliereAvailabilityItem(
    val filiereId: Int,
    val code: String,
    val nom: String,
    val ecoleNom: String?,
    val ecoleId: Int? = null,
    val coursesCount: Int,
    val sujetsCount: Int,
    val uesCount: Int = 0,
    val isAvailable: Boolean = true
)

data class EcoleCatalogueGroup(
    val ecoleId: Int,
    val nom: String,
    val sigle: String,
    val filieres: List<FiliereAvailabilityItem>,
    val ues: List<UeWithDocuments>,
    val courses: List<Cours>,
    val sujets: List<Sujet>,
    val totalCoursesCount: Int,
    val totalSujetsCount: Int,
    val totalUesCount: Int
)

sealed class CatalogueUiState {
    object Loading : CatalogueUiState()
    data class Success(
        val stats: CatalogueStats,
        val ecoleGroups: List<EcoleCatalogueGroup>,
        val allUes: List<UeWithDocuments>,
        val ecoles: List<EcoleAvailabilityItem>,
        val filieres: List<FiliereAvailabilityItem>,
        val allAvailableCourses: List<Cours>,
        val allAvailableSujets: List<Sujet>
    ) : CatalogueUiState()
    data class Error(val message: String) : CatalogueUiState()
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val service = NetworkModule.createService(application)
    private val searchRepository = SearchRepository(service)
    private val downloadRepository = DownloadRepository(service)
    private val hierarchyRepository = HierarchyRepository(service, searchRepository)
    private val tokenProvider = TokenProvider(application)

    // Event flow emitted when a document finishes downloading to automatically open the reading window
    private val _newlyDownloadedDocFlow = MutableSharedFlow<com.example.data.repository.LocalDownloadedDocument>(extraBufferCapacity = 1)
    val newlyDownloadedDocFlow = _newlyDownloadedDocFlow.asSharedFlow()

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

    // -------------------------------------------------------------
    // HIERARCHY / FILE EXPLORER STATE (Lazy loading navigation)
    // -------------------------------------------------------------
    private val _currentHierarchyStep = MutableStateFlow(HierarchyStep.ECOLE)
    val currentHierarchyStep = _currentHierarchyStep.asStateFlow()

    private val _selectedHierarchyEcole = MutableStateFlow<Ecole?>(null)
    val selectedHierarchyEcole = _selectedHierarchyEcole.asStateFlow()

    private val _selectedHierarchyDepartement = MutableStateFlow<Departement?>(null)
    val selectedHierarchyDepartement = _selectedHierarchyDepartement.asStateFlow()

    private val _selectedHierarchyFiliere = MutableStateFlow<Filiere?>(null)
    val selectedHierarchyFiliere = _selectedHierarchyFiliere.asStateFlow()

    private val _selectedHierarchyNiveau = MutableStateFlow<NiveauItem?>(null)
    val selectedHierarchyNiveau = _selectedHierarchyNiveau.asStateFlow()

    private val _selectedHierarchySemestre = MutableStateFlow<SemestreItem?>(null)
    val selectedHierarchySemestre = _selectedHierarchySemestre.asStateFlow()

    private val _selectedHierarchyCours = MutableStateFlow<CoursHierarchyItem?>(null)
    val selectedHierarchyCours = _selectedHierarchyCours.asStateFlow()

    private val _hierarchyItems = MutableStateFlow<List<Any>>(emptyList())
    val hierarchyItems = _hierarchyItems.asStateFlow()

    private val _isHierarchyLoading = MutableStateFlow(false)
    val isHierarchyLoading = _isHierarchyLoading.asStateFlow()

    private val _hierarchyFilterQuery = MutableStateFlow("")
    val hierarchyFilterQuery = _hierarchyFilterQuery.asStateFlow()

    private var hierarchyJob: Job? = null
    private var searchJob: Job? = null
    private var filiereJob: Job? = null
    private var ecoleJob: Job? = null

    // -------------------------------------------------------------
    // CATALOGUE D'ACCUEIL & DISPONIBILITÉ DES COURS
    // -------------------------------------------------------------
    private val _catalogueUiState = MutableStateFlow<CatalogueUiState>(CatalogueUiState.Loading)
    val catalogueUiState = _catalogueUiState.asStateFlow()

    private val _isCatalogueRefreshing = MutableStateFlow(false)
    val isCatalogueRefreshing = _isCatalogueRefreshing.asStateFlow()

    private var catalogueJob: Job? = null

    init {
        // Load stored token
        _accessToken.value = tokenProvider.getAccessToken() ?: ""
        
        // Anticipate/Preload Ecoles & Filieres directly in the background on load
        loadEcoles()
        loadFilieres()
        loadCatalogue()

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
        val previousEcole = _selectedEcole.value
        _selectedEcole.value = ecole
        
        // When école changes, reset selected filière and filter filières for the chosen école
        if (previousEcole != ecole) {
            _selectedFiliere.value = null
            loadFilieres(
                ecoleId = ecole?.id?.takeIf { it > 0 },
                ecoleNom = ecole?.nom
            )
        }
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
        loadEcoles()
        loadFilieres(
            ecoleId = _selectedEcole.value?.id?.takeIf { it > 0 },
            ecoleNom = _selectedEcole.value?.nom
        )
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

    fun loadFilieres(
        ecoleId: Int? = _selectedEcole.value?.id?.takeIf { it > 0 },
        ecoleNom: String? = _selectedEcole.value?.nom
    ) {
        filiereJob?.cancel()
        filiereJob = viewModelScope.launch {
            _isNetworkLoading.value = true
            try {
                val response = searchRepository.getFilieres(ecoleId = ecoleId, ecole = ecoleNom)
                if (response.isSuccessful) {
                    val apiFilieres = response.body()?.data ?: emptyList()
                    
                    val filteredFilieres = if (ecoleId != null || !ecoleNom.isNullOrBlank()) {
                        apiFilieres.filter { filiere ->
                            (ecoleId != null && filiere.ecole_id != null && filiere.ecole_id == ecoleId) ||
                            (!ecoleNom.isNullOrBlank() && filiere.ecole_nom != null && filiere.ecole_nom.equals(ecoleNom, ignoreCase = true)) ||
                            (!ecoleNom.isNullOrBlank() && filiere.ecole != null && filiere.ecole.equals(ecoleNom, ignoreCase = true)) ||
                            (filiere.ecole_id == null && filiere.ecole_nom == null && filiere.ecole == null)
                        }
                    } else {
                        apiFilieres
                    }

                    if (ecoleId != null || !ecoleNom.isNullOrBlank()) {
                        _filieres.value = filteredFilieres
                    } else {
                        if (filteredFilieres.isNotEmpty()) {
                            _filieres.value = deduplicateAndMergeFilieres(_filieres.value, filteredFilieres)
                        }
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
        ecoleJob?.cancel()
        ecoleJob = viewModelScope.launch {
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

    /**
     * Charge le catalogue complet de la disponibilité des cours sur la plateforme
     */
    fun loadCatalogue(isPullToRefresh: Boolean = false) {
        catalogueJob?.cancel()
        catalogueJob = viewModelScope.launch {
            if (isPullToRefresh) {
                _isCatalogueRefreshing.value = true
            } else if (_catalogueUiState.value !is CatalogueUiState.Success) {
                _catalogueUiState.value = CatalogueUiState.Loading
            }
            try {
                val rawDocs = searchRepository.getAllCatalogueDocuments().toMutableList()
                if (rawDocs.isEmpty()) {
                    val fallbackRes = searchRepository.searchDocuments()
                    if (fallbackRes.isSuccessful && fallbackRes.body()?.success == true) {
                        fallbackRes.body()?.data?.documents?.let { rawDocs.addAll(it) }
                    }
                }

                if (rawDocs.isNotEmpty()) {
                    updateEcolesAndFilieresFromDocuments(rawDocs)
                    val mappedUes = mapDocumentsToUes(rawDocs)
                    updateDownloadedFilesSet(mappedUes)

                    val allCourses = mappedUes.flatMap { it.cours ?: emptyList() }
                    val allSujets = mappedUes.flatMap { it.sujets ?: emptyList() }

                    val availableCoursesCount = allCourses.count { 
                        it.disponible != false && (!it.downloadUrl.isNullOrBlank() || it.deja_telecharge == true) 
                    }
                    val availableSujetsCount = allSujets.count { it.disponible != false }
                    val avecCorrigeCount = allSujets.count { it.avecCorrige == true }
                    val totalDownloads = allCourses.sumOf { it.telechargemements ?: 0 } + allSujets.sumOf { it.telechargemements ?: 0 }

                    // Grouper par École en groupes riches (École -> Filières -> UEs -> Cours)
                    val ecolesMap = rawDocs.groupBy { it.ecole_nom?.trim()?.takeIf { name -> name.isNotBlank() } ?: "Université de Garoua" }
                    
                    val ecoleGroups = ecolesMap.map { (ecoleName, docs) ->
                        val ecoleId = docs.firstOrNull()?.ecole_id ?: 0
                        val uesForThisEcole = mappedUes.filter { ue ->
                            ue.ecoleNom.equals(ecoleName, ignoreCase = true) ||
                            docs.any { d -> d.code_ue != null && d.code_ue.equals(ue.code, ignoreCase = true) }
                        }
                        
                        val filieresMapForEcole = docs.groupBy { 
                            (it.filiere_code?.trim() ?: "GEN") to (it.filiere_nom?.trim() ?: "Filière Générale")
                        }
                        val filieresForThisEcole = filieresMapForEcole.map { (key, fDocs) ->
                            val (code, nom) = key
                            val fId = fDocs.firstOrNull()?.filiere_id ?: 0
                            val courses = fDocs.count { it.type_contenu?.contains("sujet", ignoreCase = true) != true }
                            val sujets = fDocs.count { it.type_contenu?.contains("sujet", ignoreCase = true) == true }
                            val uesCount = uesForThisEcole.count { it.filiereCode.equals(code, ignoreCase = true) }
                            FiliereAvailabilityItem(
                                filiereId = fId,
                                code = code,
                                nom = nom,
                                ecoleNom = ecoleName,
                                ecoleId = ecoleId,
                                coursesCount = courses,
                                sujetsCount = sujets,
                                uesCount = if (uesCount > 0) uesCount else fDocs.mapNotNull { it.code_ue }.distinct().size,
                                isAvailable = true
                            )
                        }.sortedByDescending { it.coursesCount + it.sujetsCount }

                        val coursesForThisEcole = uesForThisEcole.flatMap { it.cours ?: emptyList() }
                        val sujetsForThisEcole = uesForThisEcole.flatMap { it.sujets ?: emptyList() }

                        EcoleCatalogueGroup(
                            ecoleId = ecoleId,
                            nom = ecoleName,
                            sigle = docs.firstOrNull()?.ecole_nom?.take(6)?.uppercase() ?: ecoleName.take(6).uppercase(),
                            filieres = filieresForThisEcole,
                            ues = uesForThisEcole,
                            courses = coursesForThisEcole,
                            sujets = sujetsForThisEcole,
                            totalCoursesCount = if (coursesForThisEcole.isNotEmpty()) coursesForThisEcole.size else docs.count { it.type_contenu?.contains("sujet", ignoreCase = true) != true },
                            totalSujetsCount = if (sujetsForThisEcole.isNotEmpty()) sujetsForThisEcole.size else docs.count { it.type_contenu?.contains("sujet", ignoreCase = true) == true },
                            totalUesCount = if (uesForThisEcole.isNotEmpty()) uesForThisEcole.size else docs.mapNotNull { it.code_ue }.distinct().size
                        )
                    }.sortedByDescending { it.totalCoursesCount + it.totalSujetsCount }

                    val ecolesAvailability = ecoleGroups.map { grp ->
                        EcoleAvailabilityItem(
                            ecoleId = grp.ecoleId,
                            nom = grp.nom,
                            sigle = grp.sigle,
                            coursesCount = grp.totalCoursesCount,
                            sujetsCount = grp.totalSujetsCount,
                            filieresCount = grp.filieres.size.coerceAtLeast(1),
                            sampleUes = grp.ues.map { it.intitule ?: it.code }.take(4)
                        )
                    }

                    // Grouper par Filière
                    val filieresMap = rawDocs.groupBy { 
                        (it.filiere_code?.trim() ?: "GEN") to (it.filiere_nom?.trim() ?: "Filière Générale")
                    }
                    val filieresAvailability = filieresMap.map { (key, docs) ->
                        val (code, nom) = key
                        val fId = docs.firstOrNull()?.filiere_id ?: 0
                        val ecoleNom = docs.firstOrNull()?.ecole_nom
                        val courses = docs.count { it.type_contenu?.contains("sujet", ignoreCase = true) != true }
                        val sujets = docs.count { it.type_contenu?.contains("sujet", ignoreCase = true) == true }
                        val uesCount = mappedUes.count { it.filiereCode.equals(code, ignoreCase = true) }
                        FiliereAvailabilityItem(
                            filiereId = fId,
                            code = code,
                            nom = nom,
                            ecoleNom = ecoleNom,
                            coursesCount = courses,
                            sujetsCount = sujets,
                            uesCount = if (uesCount > 0) uesCount else docs.mapNotNull { it.code_ue }.distinct().size,
                            isAvailable = true
                        )
                    }.sortedByDescending { it.coursesCount + it.sujetsCount }

                    val stats = CatalogueStats(
                        totalCoursesAvailable = if (availableCoursesCount > 0) availableCoursesCount else allCourses.size,
                        totalSujetsAvailable = allSujets.size,
                        totalCorrigesAvailable = avecCorrigeCount,
                        totalEcolesCovered = ecolesAvailability.size,
                        totalFilieresCovered = filieresAvailability.size,
                        totalDownloadsOnPlatform = totalDownloads,
                        platformAvailabilityRate = 100
                    )

                    _catalogueUiState.value = CatalogueUiState.Success(
                        stats = stats,
                        ecoleGroups = ecoleGroups,
                        allUes = mappedUes,
                        ecoles = ecolesAvailability,
                        filieres = filieresAvailability,
                        allAvailableCourses = allCourses,
                        allAvailableSujets = allSujets
                    )
                } else {
                    _catalogueUiState.value = CatalogueUiState.Error("Aucun document académique disponible actuellement.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!isPullToRefresh || _catalogueUiState.value !is CatalogueUiState.Success) {
                    _catalogueUiState.value = CatalogueUiState.Error(
                        if (e is java.io.IOException) "Connexion indisponible. Vérifiez votre accès réseau."
                        else "Erreur lors du traitement des données académiques."
                    )
                }
            } finally {
                _isCatalogueRefreshing.value = false
            }
        }
    }

    /**
     * Sélectionne une école depuis le catalogue pour l'explorer dans la recherche
     */
    fun selectAndExploreEcole(
        ecoleNom: String,
        ecoleId: Int? = null,
        niveau: String = "Tous",
        type: String = "Tous"
    ) {
        val ecole = Ecole(id = ecoleId ?: 0, nom = ecoleNom)
        _selectedEcole.value = ecole
        _selectedFiliere.value = null
        _searchQuery.value = ""
        _selectedNiveau.value = niveau
        _selectedType.value = type
        _selectedAnnee.value = "Toutes"
        _currentPage.value = 1
        performSearch(
            query = "",
            ecoleId = ecole.id.takeIf { it > 0 },
            ecoleName = ecole.nom,
            filiereId = null,
            filiereCode = null,
            niveau = niveau,
            type = type,
            page = 1,
            append = false
        )
    }

    /**
     * Sélectionne une filière depuis le catalogue pour l'explorer dans la recherche
     */
    fun selectAndExploreFiliere(
        filiereCode: String?,
        filiereNom: String?,
        ecoleNom: String? = null,
        ecoleId: Int? = null,
        filiereId: Int? = null,
        niveau: String = "Tous",
        type: String = "Tous"
    ) {
        val ecole = ecoleNom?.takeIf { it.isNotBlank() }?.let { Ecole(id = ecoleId ?: 0, nom = it) }
        val filiere = filiereCode?.takeIf { it.isNotBlank() }?.let {
            Filiere(id = filiereId ?: 0, code = it, nom = filiereNom ?: "", ecole_nom = ecoleNom)
        }
        _selectedEcole.value = ecole
        _selectedFiliere.value = filiere
        _searchQuery.value = ""
        _selectedNiveau.value = niveau
        _selectedType.value = type
        _selectedAnnee.value = "Toutes"
        _currentPage.value = 1
        performSearch(
            query = "",
            filiereId = filiere?.id?.takeIf { it > 0 },
            filiereCode = filiere?.code?.takeIf { it.isNotBlank() },
            ecoleId = ecole?.id?.takeIf { it > 0 },
            ecoleName = ecole?.nom,
            niveau = niveau,
            type = type,
            page = 1,
            append = false
        )
    }

    /**
     * Sélectionne une Unité d'Enseignement (UE) depuis le catalogue pour l'explorer dans la recherche
     */
    fun selectAndExploreUe(
        ueCode: String,
        ueIntitule: String? = null,
        filiereCode: String? = null,
        filiereNom: String? = null,
        ecoleNom: String? = null,
        niveau: String = "Tous",
        type: String = "Tous"
    ) {
        val ecole = ecoleNom?.takeIf { it.isNotBlank() }?.let { Ecole(id = 0, nom = it) }
        val filiere = filiereCode?.takeIf { it.isNotBlank() }?.let {
            Filiere(id = 0, code = it, nom = filiereNom ?: "", ecole_nom = ecoleNom)
        }
        if (ecole != null) _selectedEcole.value = ecole
        if (filiere != null) _selectedFiliere.value = filiere
        _searchQuery.value = ueCode
        _selectedNiveau.value = niveau
        _selectedType.value = type
        _selectedAnnee.value = "Toutes"
        _currentPage.value = 1
        performSearch(
            query = ueCode,
            filiereCode = filiere?.code,
            ecoleName = ecole?.nom,
            niveau = niveau,
            type = type,
            page = 1,
            append = false
        )
    }

    /**
     * Sélectionne un cours depuis le catalogue pour l'explorer dans la recherche
     */
    fun selectAndExploreCours(cours: Cours) {
        val ecole = cours.ecoleNom?.takeIf { it.isNotBlank() }?.let { Ecole(id = 0, nom = it) }
        val filiere = cours.filiereCode?.takeIf { it.isNotBlank() }?.let {
            Filiere(id = 0, code = it, nom = cours.filiereNom ?: "", ecole_nom = cours.ecoleNom)
        }
        if (ecole != null) _selectedEcole.value = ecole
        if (filiere != null) _selectedFiliere.value = filiere
        _searchQuery.value = cours.titre
        _currentPage.value = 1
        performSearch(
            query = cours.titre,
            filiereCode = filiere?.code,
            ecoleName = ecole?.nom,
            page = 1,
            append = false
        )
    }

    fun selectAndExploreFiliere(filiere: Filiere?, ecole: Ecole? = null) {
        _selectedEcole.value = ecole
        _selectedFiliere.value = filiere
        _currentPage.value = 1
        performSearch(
            filiereId = filiere?.id,
            filiereCode = filiere?.code,
            ecoleId = ecole?.id,
            ecoleName = ecole?.nom,
            page = 1,
            append = false
        )
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
                    val errMsg = apiResponse.body()?.message ?: "Aucun document trouvé"
                    if (code in 500..599) {
                        _uiState.value = SearchUiState.ServerError(code, "Serveur momentanément indisponible")
                    } else {
                        _uiState.value = SearchUiState.Error(errMsg.take(40))
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
                    _uiState.value = SearchUiState.NetworkError("Mode hors-ligne actif")
                } else {
                    _uiState.value = SearchUiState.Error("Chargement impossible, réessayez")
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
                            disponible = doc.disponible ?: true,
                            filiereCode = doc.filiere_code,
                            filiereNom = doc.filiere_nom,
                            ecoleNom = doc.ecole_nom
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
                            tailleFichier = doc.tailleOctetsLong ?: doc.taille_octets?.toLongOrNull(),
                            statut = "publie",
                            anneAcademique = docAnnee,
                            vues = 0,
                            telechargemements = doc.telechargements ?: 0,
                            ue_id = idCounter,
                            enseignant_id = 0,
                            deja_telecharge = doc.deja_telecharge ?: false,
                            disponible = doc.disponible ?: true,
                            filiereCode = doc.filiere_code,
                            filiereNom = doc.filiere_nom,
                            ecoleNom = doc.ecole_nom
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
                sujets = sujetsList,
                filiereCode = sampleDoc?.filiere_code,
                filiereNom = sampleDoc?.filiere_nom,
                ecoleNom = sampleDoc?.ecole_nom
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

    private fun playDownloadSuccessSound(context: Context) {
        try {
            // First attempt to play system notification sound
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            if (ringtone != null) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone.play()
            } else {
                // Fallback to ToneGenerator for pleasant confirmation beep
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            }
        } catch (e: Exception) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            } catch (_: Exception) {
                // Silently ignore if audio service is unavailable
            }
        }
    }

    // Downloads handling
    fun downloadCours(context: Context, cours: Cours) {
        val key = "cours_${cours.id}"
        viewModelScope.launch {
            downloadRepository.downloadCours(context, cours.id, cours.titre, cours.downloadUrl).collect { status ->
                _activeDownloads.value = _activeDownloads.value + (key to status)
                if (status is DownloadStatus.Success) {
                    playDownloadSuccessSound(context)
                    updateDownloadedFilesSet()
                    // Emit newly downloaded document to automatically open reading window
                    val file = downloadRepository.getLocalFileForCours(context, cours.id)
                    if (file != null && file.exists()) {
                        val localDoc = com.example.data.repository.LocalDownloadedDocument(
                            file = file,
                            name = file.name,
                            formattedTitle = cours.titre ?: file.name,
                            typeLabel = "Cours PDF",
                            sizeFormatted = "${(file.length() / 1024).coerceAtLeast(1)} Ko",
                            lastModifiedMillis = file.lastModified()
                        )
                        _newlyDownloadedDocFlow.tryEmit(localDoc)
                    }
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
                    playDownloadSuccessSound(context)
                    updateDownloadedFilesSet()
                    // Emit newly downloaded document to automatically open reading window
                    val file = downloadRepository.getLocalFileForSujet(context, sujet.id, isCorrige)
                    if (file != null && file.exists()) {
                        val docTitle = "${sujet.titre ?: "Épreuve"}${if (isCorrige) " (Corrigé)" else ""}"
                        val localDoc = com.example.data.repository.LocalDownloadedDocument(
                            file = file,
                            name = file.name,
                            formattedTitle = docTitle,
                            typeLabel = if (isCorrige) "Corrigé PDF" else "Épreuve PDF",
                            sizeFormatted = "${(file.length() / 1024).coerceAtLeast(1)} Ko",
                            lastModifiedMillis = file.lastModified()
                        )
                        _newlyDownloadedDocFlow.tryEmit(localDoc)
                    }
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

    fun getLocalFileForCours(context: Context, cours: Cours): File? {
        val file = downloadRepository.getLocalFileForCours(context, cours.id)
        return if (file != null && file.exists()) file else null
    }

    fun getLocalDocumentForCours(context: Context, cours: Cours): com.example.data.repository.LocalDownloadedDocument? {
        val file = getLocalFileForCours(context, cours) ?: return null
        return com.example.data.repository.LocalDownloadedDocument(
            file = file,
            name = file.name,
            formattedTitle = cours.titre ?: file.name,
            typeLabel = "Cours PDF",
            sizeFormatted = "${(file.length() / 1024).coerceAtLeast(1)} Ko",
            lastModifiedMillis = file.lastModified(),
            typeCategory = "Cours"
        )
    }

    fun getLocalFileForSujet(context: Context, sujet: Sujet, isCorrige: Boolean): File? {
        val file = downloadRepository.getLocalFileForSujet(context, sujet.id, isCorrige)
        return if (file != null && file.exists()) file else null
    }

    fun getLocalDocumentForSujet(context: Context, sujet: Sujet, isCorrige: Boolean): com.example.data.repository.LocalDownloadedDocument? {
        val file = getLocalFileForSujet(context, sujet, isCorrige) ?: return null
        val docTitle = "${sujet.titre ?: "Épreuve"}${if (isCorrige) " (Corrigé)" else ""}"
        return com.example.data.repository.LocalDownloadedDocument(
            file = file,
            name = file.name,
            formattedTitle = docTitle,
            typeLabel = if (isCorrige) "Corrigé PDF" else "Épreuve PDF",
            sizeFormatted = "${(file.length() / 1024).coerceAtLeast(1)} Ko",
            lastModifiedMillis = file.lastModified(),
            typeCategory = if (isCorrige) "Corrigés" else "Sujets"
        )
    }

    fun shareDownloadedCours(context: Context, cours: Cours) {
        val file = downloadRepository.getLocalFileForCours(context, cours.id)
        if (file != null && file.exists()) {
            downloadRepository.shareFile(context, file, cours.titre)
        } else {
            Toast.makeText(context, "Veuillez d'abord télécharger ce cours pour le partager.", Toast.LENGTH_SHORT).show()
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

    fun shareDownloadedSujet(context: Context, sujet: Sujet, isCorrige: Boolean) {
        val file = downloadRepository.getLocalFileForSujet(context, sujet.id, isCorrige)
        if (file != null && file.exists()) {
            val title = "${sujet.titre ?: "Épreuve"}${if (isCorrige) " (Corrigé)" else ""}"
            downloadRepository.shareFile(context, file, title)
        } else {
            Toast.makeText(context, "Veuillez d'abord télécharger ce document pour le partager.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareLocalFile(context: Context, localDoc: com.example.data.repository.LocalDownloadedDocument) {
        if (localDoc.file.exists()) {
            downloadRepository.shareFile(context, localDoc.file, localDoc.formattedTitle)
        } else {
            Toast.makeText(context, "Le fichier local n'a pas été trouvé.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareLocalFileToGeminiApp(
        context: Context,
        localDoc: com.example.data.repository.LocalDownloadedDocument,
        onNotInstalled: (() -> Unit)? = null
    ) {
        if (localDoc.file.exists()) {
            downloadRepository.shareToGemini(
                context = context,
                file = localDoc.file,
                preferGeminiDirect = true,
                onNotInstalled = onNotInstalled
            )
        } else {
            Toast.makeText(context, "Le fichier local n'a pas été trouvé.", Toast.LENGTH_SHORT).show()
        }
    }

    fun isGeminiAppInstalled(context: Context): Boolean {
        return com.example.utils.GeminiShareHelper.isGeminiInstalled(context)
    }

    fun promptInstallGeminiApp(context: Context) {
        com.example.utils.GeminiShareHelper.promptInstallGemini(context)
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

        val currentEcoleId = _selectedEcole.value?.id?.takeIf { it > 0 }
        val currentEcoleNom = _selectedEcole.value?.nom

        val extractedFilieres = rawDocs.mapNotNull { doc ->
            val realId = doc.filiere_id?.takeIf { it > 0 }
            val code = doc.filiere_code?.takeIf { it.isNotBlank() } ?: doc.code_ue
            val nom = doc.filiere_nom?.takeIf { it.isNotBlank() } ?: doc.intitule_ue
            val docEcoleId = doc.ecole_id?.takeIf { it > 0 }
            val docEcoleNom = doc.ecole_nom?.takeIf { it.isNotBlank() }

            val matchesEcole = if (currentEcoleId != null) {
                docEcoleId == currentEcoleId || (currentEcoleNom != null && docEcoleNom?.equals(currentEcoleNom, ignoreCase = true) == true)
            } else true

            if (realId != null && !code.isNullOrBlank() && !nom.isNullOrBlank() && matchesEcole) {
                Filiere(
                    id = realId,
                    code = code,
                    nom = nom,
                    departement = null,
                    ecole_id = docEcoleId,
                    ecole_nom = docEcoleNom
                )
            } else null
        }.distinctBy { it.id }

        if (extractedFilieres.isNotEmpty()) {
            _filieres.value = deduplicateAndMergeFilieres(_filieres.value, extractedFilieres)
        }
    }

    // ------------------------------------------------------------------------
    // HIERARCHY NAVIGATION IMPLEMENTATION (Lazy loading level by level)
    // ------------------------------------------------------------------------

    fun initHierarchy() {
        if (_hierarchyItems.value.isEmpty() && _currentHierarchyStep.value == HierarchyStep.ECOLE) {
            loadHierarchyEcoles()
        }
    }

    fun updateHierarchyFilterQuery(query: String) {
        _hierarchyFilterQuery.value = query
    }

    fun loadHierarchyEcoles() {
        hierarchyJob?.cancel()
        hierarchyJob = viewModelScope.launch {
            _isHierarchyLoading.value = true
            _currentHierarchyStep.value = HierarchyStep.ECOLE
            try {
                val ecoles = hierarchyRepository.getEcoles()
                _hierarchyItems.value = ecoles
            } catch (_: Exception) {
                _hierarchyItems.value = emptyList()
            } finally {
                _isHierarchyLoading.value = false
            }
        }
    }

    fun selectHierarchyEcole(ecole: Ecole) {
        _selectedHierarchyEcole.value = ecole
        _selectedHierarchyDepartement.value = null
        _selectedHierarchyFiliere.value = null
        _selectedHierarchyNiveau.value = null
        _selectedHierarchySemestre.value = null
        _selectedHierarchyCours.value = null
        _hierarchyFilterQuery.value = ""

        hierarchyJob?.cancel()
        hierarchyJob = viewModelScope.launch {
            _isHierarchyLoading.value = true
            _hierarchyItems.value = emptyList()
            try {
                val departements = hierarchyRepository.getDepartements(ecole.id, ecole.nom)
                if (departements.isNotEmpty()) {
                    _currentHierarchyStep.value = HierarchyStep.DEPARTEMENT
                    _hierarchyItems.value = departements
                } else {
                    _currentHierarchyStep.value = HierarchyStep.FILIERE
                    val filieres = hierarchyRepository.getFilieres(ecole.id, ecole.nom)
                    _hierarchyItems.value = filieres
                }
            } catch (_: Exception) {
                _hierarchyItems.value = emptyList()
            } finally {
                _isHierarchyLoading.value = false
            }
        }
    }

    fun selectHierarchyDepartement(departement: Departement) {
        _selectedHierarchyDepartement.value = departement
        _selectedHierarchyFiliere.value = null
        _selectedHierarchyNiveau.value = null
        _selectedHierarchySemestre.value = null
        _selectedHierarchyCours.value = null
        _currentHierarchyStep.value = HierarchyStep.FILIERE
        _hierarchyFilterQuery.value = ""

        val ecole = _selectedHierarchyEcole.value
        hierarchyJob?.cancel()
        hierarchyJob = viewModelScope.launch {
            _isHierarchyLoading.value = true
            _hierarchyItems.value = emptyList()
            try {
                val filieres = hierarchyRepository.getFilieres(
                    ecoleId = ecole?.id,
                    ecoleNom = ecole?.nom,
                    departementNom = departement.nom
                )
                _hierarchyItems.value = filieres
            } catch (_: Exception) {
                _hierarchyItems.value = emptyList()
            } finally {
                _isHierarchyLoading.value = false
            }
        }
    }

    fun selectHierarchyFiliere(filiere: Filiere) {
        _selectedHierarchyFiliere.value = filiere
        _selectedHierarchyNiveau.value = null
        _selectedHierarchySemestre.value = null
        _selectedHierarchyCours.value = null
        _currentHierarchyStep.value = HierarchyStep.NIVEAU
        _hierarchyFilterQuery.value = ""

        val ecole = _selectedHierarchyEcole.value
        hierarchyJob?.cancel()
        hierarchyJob = viewModelScope.launch {
            _isHierarchyLoading.value = true
            _hierarchyItems.value = emptyList()
            try {
                val niveaux = hierarchyRepository.getNiveaux(
                    filiereId = filiere.id,
                    filiereCode = filiere.code,
                    ecoleId = ecole?.id,
                    ecoleNom = ecole?.nom
                )
                _hierarchyItems.value = niveaux
            } catch (_: Exception) {
                _hierarchyItems.value = emptyList()
            } finally {
                _isHierarchyLoading.value = false
            }
        }
    }

    fun selectHierarchyNiveau(niveau: NiveauItem) {
        _selectedHierarchyNiveau.value = niveau
        _selectedHierarchySemestre.value = null
        _selectedHierarchyCours.value = null
        _currentHierarchyStep.value = HierarchyStep.SEMESTRE
        _hierarchyFilterQuery.value = ""

        val filiere = _selectedHierarchyFiliere.value
        val ecole = _selectedHierarchyEcole.value
        hierarchyJob?.cancel()
        hierarchyJob = viewModelScope.launch {
            _isHierarchyLoading.value = true
            _hierarchyItems.value = emptyList()
            try {
                val semestres = hierarchyRepository.getSemestres(
                    filiereId = filiere?.id,
                    filiereCode = filiere?.code,
                    niveauCode = niveau.code,
                    ecoleId = ecole?.id,
                    ecoleNom = ecole?.nom
                )
                _hierarchyItems.value = semestres
            } catch (_: Exception) {
                _hierarchyItems.value = emptyList()
            } finally {
                _isHierarchyLoading.value = false
            }
        }
    }

    fun selectHierarchySemestre(semestre: SemestreItem) {
        _selectedHierarchySemestre.value = semestre
        _selectedHierarchyCours.value = null
        _currentHierarchyStep.value = HierarchyStep.COURS
        _hierarchyFilterQuery.value = ""

        val filiere = _selectedHierarchyFiliere.value
        val niveau = _selectedHierarchyNiveau.value
        val ecole = _selectedHierarchyEcole.value
        hierarchyJob?.cancel()
        hierarchyJob = viewModelScope.launch {
            _isHierarchyLoading.value = true
            _hierarchyItems.value = emptyList()
            try {
                val coursList = hierarchyRepository.getCours(
                    filiereId = filiere?.id,
                    filiereCode = filiere?.code,
                    niveauCode = niveau?.code,
                    semestreCode = semestre.code,
                    ecoleId = ecole?.id,
                    ecoleNom = ecole?.nom
                )
                _hierarchyItems.value = coursList
            } catch (_: Exception) {
                _hierarchyItems.value = emptyList()
            } finally {
                _isHierarchyLoading.value = false
            }
        }
    }

    fun selectHierarchyCours(cours: CoursHierarchyItem) {
        _selectedHierarchyCours.value = cours
        _currentHierarchyStep.value = HierarchyStep.DOCUMENTS
        _hierarchyFilterQuery.value = ""

        val filiere = _selectedHierarchyFiliere.value
        val niveau = _selectedHierarchyNiveau.value
        val ecole = _selectedHierarchyEcole.value
        hierarchyJob?.cancel()
        hierarchyJob = viewModelScope.launch {
            _isHierarchyLoading.value = true
            _hierarchyItems.value = emptyList()
            try {
                val docs = hierarchyRepository.getDocumentsForCours(
                    cours = cours,
                    filiereId = filiere?.id ?: cours.filiereId,
                    filiereCode = filiere?.code ?: cours.filiereCode,
                    niveauCode = niveau?.code ?: cours.niveauCode,
                    ecoleId = ecole?.id,
                    ecoleNom = ecole?.nom
                )
                _hierarchyItems.value = docs
            } catch (_: Exception) {
                _hierarchyItems.value = emptyList()
            } finally {
                _isHierarchyLoading.value = false
            }
        }
    }

    fun navigateBackHierarchy() {
        when (_currentHierarchyStep.value) {
            HierarchyStep.DOCUMENTS -> {
                val sem = _selectedHierarchySemestre.value
                val niv = _selectedHierarchyNiveau.value
                if (sem != null) {
                    selectHierarchySemestre(sem)
                } else if (niv != null) {
                    selectHierarchyNiveau(niv)
                } else {
                    navigateToHierarchyStep(HierarchyStep.COURS)
                }
            }
            HierarchyStep.COURS -> {
                val niv = _selectedHierarchyNiveau.value
                val fil = _selectedHierarchyFiliere.value
                if (niv != null) {
                    selectHierarchyNiveau(niv)
                } else if (fil != null) {
                    selectHierarchyFiliere(fil)
                } else {
                    navigateToHierarchyStep(HierarchyStep.NIVEAU)
                }
            }
            HierarchyStep.SEMESTRE -> {
                val fil = _selectedHierarchyFiliere.value
                if (fil != null) {
                    selectHierarchyFiliere(fil)
                } else {
                    navigateToHierarchyStep(HierarchyStep.NIVEAU)
                }
            }
            HierarchyStep.NIVEAU -> {
                val dep = _selectedHierarchyDepartement.value
                val ecole = _selectedHierarchyEcole.value
                if (dep != null) {
                    selectHierarchyDepartement(dep)
                } else if (ecole != null) {
                    selectHierarchyEcole(ecole)
                } else {
                    navigateToHierarchyStep(HierarchyStep.FILIERE)
                }
            }
            HierarchyStep.FILIERE -> {
                val dep = _selectedHierarchyDepartement.value
                val ecole = _selectedHierarchyEcole.value
                if (dep != null) {
                    selectHierarchyDepartement(dep)
                } else if (ecole != null) {
                    selectHierarchyEcole(ecole)
                } else {
                    resetHierarchy()
                }
            }
            HierarchyStep.DEPARTEMENT -> {
                resetHierarchy()
            }
            HierarchyStep.ECOLE -> {
                // Already at root
            }
        }
    }

    fun navigateToHierarchyStep(targetStep: HierarchyStep) {
        _hierarchyFilterQuery.value = ""
        when (targetStep) {
            HierarchyStep.ECOLE -> resetHierarchy()
            HierarchyStep.DEPARTEMENT -> {
                val ecole = _selectedHierarchyEcole.value
                if (ecole != null) selectHierarchyEcole(ecole) else resetHierarchy()
            }
            HierarchyStep.FILIERE -> {
                val dep = _selectedHierarchyDepartement.value
                if (dep != null) selectHierarchyDepartement(dep) else navigateToHierarchyStep(HierarchyStep.DEPARTEMENT)
            }
            HierarchyStep.NIVEAU -> {
                val fil = _selectedHierarchyFiliere.value
                if (fil != null) selectHierarchyFiliere(fil) else navigateToHierarchyStep(HierarchyStep.FILIERE)
            }
            HierarchyStep.SEMESTRE -> {
                val niv = _selectedHierarchyNiveau.value
                if (niv != null) selectHierarchyNiveau(niv) else navigateToHierarchyStep(HierarchyStep.NIVEAU)
            }
            HierarchyStep.COURS -> {
                val sem = _selectedHierarchySemestre.value
                if (sem != null) selectHierarchySemestre(sem) else navigateToHierarchyStep(HierarchyStep.SEMESTRE)
            }
            HierarchyStep.DOCUMENTS -> {
                val cours = _selectedHierarchyCours.value
                if (cours != null) selectHierarchyCours(cours) else navigateToHierarchyStep(HierarchyStep.COURS)
            }
        }
    }

    fun resetHierarchy() {
        _selectedHierarchyEcole.value = null
        _selectedHierarchyDepartement.value = null
        _selectedHierarchyFiliere.value = null
        _selectedHierarchyNiveau.value = null
        _selectedHierarchySemestre.value = null
        _selectedHierarchyCours.value = null
        _hierarchyFilterQuery.value = ""
        loadHierarchyEcoles()
    }

    fun downloadHierarchyDocument(context: Context, doc: DocumentHierarchyItem) {
        val cours = Cours(
            id = doc.id,
            titre = doc.nom,
            description = doc.typeLabel,
            type = doc.type,
            cheminFichier = doc.downloadUrl,
            lienTelechargement = doc.downloadUrl,
            nomFichierOriginal = doc.nom,
            tailleFichier = doc.tailleOctets,
            statut = "publie",
            anneAcademique = doc.anneeAcademique,
            vues = 0,
            telechargemements = 0,
            ue_id = doc.coursId,
            enseignant_id = 0,
            deja_telecharge = false,
            disponible = doc.disponible
        )
        downloadCours(context, cours)
    }

    fun getLocalFileForHierarchyDoc(context: Context, doc: DocumentHierarchyItem): File? {
        val file = downloadRepository.getLocalFileForCours(context, doc.id)
        return if (file != null && file.exists()) file else null
    }
}

