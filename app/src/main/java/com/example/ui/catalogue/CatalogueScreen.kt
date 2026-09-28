package com.example.ui.catalogue

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Cours
import com.example.data.model.Ecole
import com.example.data.model.Filiere
import com.example.data.model.Sujet
import com.example.data.model.UeWithDocuments
import com.example.data.repository.DownloadStatus
import com.example.ui.search.CatalogueStats
import com.example.ui.search.CatalogueUiState
import com.example.ui.search.EcoleAvailabilityItem
import com.example.ui.search.EcoleCatalogueGroup
import com.example.ui.search.FiliereAvailabilityItem
import com.example.ui.search.SearchViewModel
import com.example.ui.theme.FiliereColorProvider
import com.example.ui.components.CatalogueSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(
    viewModel: SearchViewModel,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalogueState by viewModel.catalogueUiState.collectAsState()
    val isRefreshing by viewModel.isCatalogueRefreshing.collectAsState()
    val downloadedDocs by viewModel.downloadedDocuments.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedViewMode by rememberSaveable { mutableIntStateOf(0) } // 0: Par École, 1: Filières, 2: Unités (UEs), 3: Tous les cours

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.loadCatalogue(isPullToRefresh = true) },
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = catalogueState) {
            is CatalogueUiState.Loading -> {
                CatalogueSkeleton()
            }
            is CatalogueUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Disponibilité non actualisée",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.message,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.loadCatalogue() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Réessayer")
                    }
                }
            }
            is CatalogueUiState.Success -> {
                CatalogueContent(
                    context = context,
                    state = state,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedViewMode = selectedViewMode,
                    onViewModeSelect = { selectedViewMode = it },
                    downloadedDocs = downloadedDocs,
                    activeDownloads = activeDownloads,
                    onDownloadCours = { cours -> viewModel.downloadCours(context, cours) },
                    onOpenCours = { cours -> viewModel.openDownloadedCours(context, cours) },
                    onShareCours = { cours -> viewModel.shareDownloadedCours(context, cours) },
                    onDeleteCours = { cours -> viewModel.deleteDownloadedCours(context, cours) },
                    onExploreEcole = { ecoleNom, ecoleId ->
                        viewModel.selectAndExploreEcole(
                            ecoleNom = ecoleNom,
                            ecoleId = ecoleId
                        )
                        onNavigateToSearch()
                    },
                    onExploreFiliere = { filiereCode, filiereNom, ecoleNom, ecoleId, filiereId ->
                        viewModel.selectAndExploreFiliere(
                            filiereCode = filiereCode,
                            filiereNom = filiereNom,
                            ecoleNom = ecoleNom,
                            ecoleId = ecoleId,
                            filiereId = filiereId
                        )
                        onNavigateToSearch()
                    },
                    onExploreUe = { ue ->
                        viewModel.selectAndExploreUe(
                            ueCode = ue.code,
                            ueIntitule = ue.intitule,
                            filiereCode = ue.filiereCode,
                            filiereNom = ue.filiereNom,
                            ecoleNom = ue.ecoleNom,
                            niveau = ue.niveau ?: "Tous"
                        )
                        onNavigateToSearch()
                    },
                    onExploreCours = { cours ->
                        viewModel.selectAndExploreCours(cours)
                        onNavigateToSearch()
                    }
                )
            }
        }
    }
}

@Composable
private fun CatalogueContent(
    context: Context,
    state: CatalogueUiState.Success,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedViewMode: Int,
    onViewModeSelect: (Int) -> Unit,
    downloadedDocs: Set<String>,
    activeDownloads: Map<String, DownloadStatus>,
    onDownloadCours: (Cours) -> Unit,
    onOpenCours: (Cours) -> Unit,
    onShareCours: (Cours) -> Unit,
    onDeleteCours: (Cours) -> Unit,
    onExploreEcole: (String, Int?) -> Unit,
    onExploreFiliere: (String?, String?, String?, Int?, Int?) -> Unit,
    onExploreUe: (UeWithDocuments) -> Unit,
    onExploreCours: (Cours) -> Unit
) {
    // 1. Filtrage par recherche textuelle (Écoles)
    val filteredEcoleGroups = remember(state.ecoleGroups, searchQuery) {
        if (searchQuery.isBlank()) {
            state.ecoleGroups
        } else {
            state.ecoleGroups.filter { grp ->
                grp.nom.contains(searchQuery, ignoreCase = true) ||
                grp.sigle.contains(searchQuery, ignoreCase = true) ||
                grp.filieres.any { it.nom.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true) } ||
                grp.ues.any { it.code.contains(searchQuery, ignoreCase = true) || (it.intitule?.contains(searchQuery, ignoreCase = true) == true) } ||
                grp.courses.any { it.titre.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    // 2. Filtrage des filières
    val filteredFilieres = remember(state.filieres, searchQuery) {
        if (searchQuery.isBlank()) {
            state.filieres
        } else {
            state.filieres.filter { fil ->
                fil.nom.contains(searchQuery, ignoreCase = true) ||
                fil.code.contains(searchQuery, ignoreCase = true) ||
                (fil.ecoleNom?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    // 3. Filtrage des UEs
    val filteredUes = remember(state.allUes, searchQuery) {
        if (searchQuery.isBlank()) {
            state.allUes
        } else {
            state.allUes.filter { ue ->
                ue.code.contains(searchQuery, ignoreCase = true) ||
                (ue.intitule?.contains(searchQuery, ignoreCase = true) == true) ||
                (ue.filiereCode?.contains(searchQuery, ignoreCase = true) == true) ||
                (ue.filiereNom?.contains(searchQuery, ignoreCase = true) == true) ||
                (ue.ecoleNom?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    // 4. Filtrage des Cours
    val filteredCourses = remember(state.allAvailableCourses, searchQuery) {
        if (searchQuery.isBlank()) {
            state.allAvailableCourses
        } else {
            state.allAvailableCourses.filter { cours ->
                cours.titre.contains(searchQuery, ignoreCase = true) ||
                (cours.filiereCode?.contains(searchQuery, ignoreCase = true) == true) ||
                (cours.filiereNom?.contains(searchQuery, ignoreCase = true) == true) ||
                (cours.ecoleNom?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Barre de recherche sans paramètres de filtres
        item {
            CatalogueSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )
        }

        // 3. Sélecteur de vue : [Par École] [Filières] [Unités (UEs)] [Tous les cours]
        item {
            CatalogueViewModeTabs(
                selectedMode = selectedViewMode,
                onSelectMode = onViewModeSelect,
                ecolesCount = state.ecoleGroups.size,
                filieresCount = state.filieres.size,
                uesCount = state.allUes.size,
                coursesCount = state.allAvailableCourses.size
            )
        }

        // 5. Contenu Principal selon le Mode sélectionné
        when (selectedViewMode) {
            0 -> {
                // VUE 0 (Par École) : Présentation par Établissement avec filières, UEs et cours
                if (filteredEcoleGroups.isEmpty()) {
                    item {
                        EmptyCatalogueNotice(
                            message = "Aucun établissement ne correspond aux critères sélectionnés."
                        )
                    }
                } else {
                    items(filteredEcoleGroups, key = { "ecole_grp_${it.ecoleId}_${it.nom}" }) { ecoleGroup ->
                        EcoleDetailedCatalogueCard(
                            ecoleGroup = ecoleGroup,
                            downloadedDocs = downloadedDocs,
                            activeDownloads = activeDownloads,
                            onExploreEcole = { onExploreEcole(ecoleGroup.nom, ecoleGroup.ecoleId) },
                            onExploreFiliere = { fil ->
                                onExploreFiliere(fil.code, fil.nom, ecoleGroup.nom, ecoleGroup.ecoleId, fil.filiereId)
                            },
                            onExploreUe = onExploreUe,
                            onExploreCours = onExploreCours,
                            onDownloadCours = onDownloadCours,
                            onOpenCours = onOpenCours
                        )
                    }
                }
            }
            1 -> {
                // VUE 1 : Liste complète des filières disponibles
                if (filteredFilieres.isEmpty()) {
                    item {
                        EmptyCatalogueNotice(message = "Aucune filière trouvée pour cette recherche.")
                    }
                } else {
                    item {
                        Text(
                            text = "Filières disponibles sur la plateforme (${filteredFilieres.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(filteredFilieres, key = { "filiere_${it.filiereId}_${it.code}_${it.nom}" }) { filiereItem ->
                        FiliereClickableCard(
                            item = filiereItem,
                            onClick = {
                                onExploreFiliere(
                                    filiereItem.code,
                                    filiereItem.nom,
                                    filiereItem.ecoleNom,
                                    filiereItem.ecoleId,
                                    filiereItem.filiereId
                                )
                            }
                        )
                    }
                }
            }
            2 -> {
                // VUE 2 : Liste complète des Unités d'Enseignement (UEs)
                if (filteredUes.isEmpty()) {
                    item {
                        EmptyCatalogueNotice(message = "Aucune Unité d'Enseignement trouvée pour cette recherche.")
                    }
                } else {
                    item {
                        Text(
                            text = "Unités d'Enseignement (UE) disponibles (${filteredUes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(filteredUes, key = { "ue_${it.code}_${it.intitule}" }) { ue ->
                        UeClickableCard(
                            ue = ue,
                            onClick = { onExploreUe(ue) }
                        )
                    }
                }
            }
            3 -> {
                // VUE 3 : Liste des cours disponibles
                if (filteredCourses.isEmpty()) {
                    item {
                        EmptyCatalogueNotice(message = "Aucun cours trouvé.")
                    }
                } else {
                    item {
                        Text(
                            text = "Tous les cours disponibles (${filteredCourses.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(filteredCourses, key = { "cours_full_${it.id}" }) { cours ->
                        val isDownloaded = cours.deja_telecharge == true || downloadedDocs.contains("cours_${cours.id}")
                        val downloadStatus = activeDownloads["cours_${cours.id}"] ?: DownloadStatus.Idle

                        CatalogueCourseCompactCard(
                            cours = cours,
                            isDownloaded = isDownloaded,
                            downloadStatus = downloadStatus,
                            onCardClick = { onExploreCours(cours) },
                            onDownload = { onDownloadCours(cours) },
                            onOpen = { onOpenCours(cours) },
                            onShare = { onShareCours(cours) },
                            onDelete = { onDeleteCours(cours) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// COMPOSANTS PAR ÉCOLE & HIÉRARCHIE (ÉCOLE -> FILIÈRES -> UEs -> COURS)
// -------------------------------------------------------------------------------------------------

@Composable
fun EcoleDetailedCatalogueCard(
    ecoleGroup: EcoleCatalogueGroup,
    downloadedDocs: Set<String>,
    activeDownloads: Map<String, DownloadStatus>,
    onExploreEcole: () -> Unit,
    onExploreFiliere: (FiliereAvailabilityItem) -> Unit,
    onExploreUe: (UeWithDocuments) -> Unit,
    onExploreCours: (Cours) -> Unit,
    onDownloadCours: (Cours) -> Unit,
    onOpenCours: (Cours) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Entête de l'École : Nom, Sigle, Badge actif
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isFs = ecoleGroup.sigle.equals("FS", ignoreCase = true) ||
                            ecoleGroup.nom.contains("Sciences", ignoreCase = true) ||
                            ecoleGroup.nom.equals("FS", ignoreCase = true)
                    val isFmsb = ecoleGroup.sigle.equals("FMSB", ignoreCase = true) ||
                            ecoleGroup.nom.contains("FMSB", ignoreCase = true) ||
                            ecoleGroup.nom.contains("Médecine", ignoreCase = true) ||
                            ecoleGroup.nom.contains("Biomédicales", ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFs || isFmsb) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        if (isFmsb) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_fmsb),
                                contentDescription = "Logo FMSB",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else if (isFs) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_fs),
                                contentDescription = "Logo Faculté des Sciences",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = if (ecoleGroup.nom == "FS") "Faculté des Sciences (FS)"
                            else if (ecoleGroup.nom == "FMSB") "Faculté de Médecine et des Sciences Biomédicales (FMSB)"
                            else ecoleGroup.nom,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${ecoleGroup.filieres.size} filière(s) • ${ecoleGroup.totalUesCount} UE(s) • ${ecoleGroup.totalCoursesCount} cours",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Réduire" else "Développer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bouton Principal : En cliquant sur Explorer, les filières, unités et cours se déroulent
            Button(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("explore_ecole_button_${ecoleGroup.ecoleId}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant
                                     else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isExpanded) MaterialTheme.colorScheme.onSurfaceVariant
                                   else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Explore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isExpanded) "Réduire" else "Explorer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // SECTION 1 : Filières disponibles de cette école
                    if (ecoleGroup.filieres.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filières disponibles (${ecoleGroup.filieres.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Cliquez pour filtrer",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(ecoleGroup.filieres) { filiere ->
                                    val filierePalette = FiliereColorProvider.getPalette(
                                        filiereCode = filiere.code,
                                        filiereNom = filiere.nom,
                                        fallbackText = filiere.nom
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = filierePalette.containerColor,
                                        border = BorderStroke(1.dp, filierePalette.borderColor),
                                        modifier = Modifier.clickable { onExploreFiliere(filiere) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(filierePalette.accentColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = filiere.code,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = filierePalette.contentColor
                                                )
                                                Text(
                                                    text = "${filiere.coursesCount} cours",
                                                    fontSize = 9.sp,
                                                    color = filierePalette.contentColor.copy(alpha = 0.8f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = filierePalette.accentColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // LES DEUX DERNIERS ÉLÉMENTS : COMPOSANTS AFFICHANT LE NOMBRE TOTAL D'UEs ET DE COURS (COMPACTS & THÈME AJUSTÉ)
                    val isDark = isSystemInDarkTheme()
                    val coursBg = if (isDark) Color(0xFF1B3B26) else Color(0xFFE8F5E9)
                    val coursAccent = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                    val coursBorder = coursAccent.copy(alpha = 0.35f)
                    val coursText = if (isDark) Color(0xFFE8F5E9) else Color(0xFF1B5E20)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Composant 1 : Nombre total d'Unités d'Enseignement (UE) de cette école (Format compact)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.35f else 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onExploreEcole() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${ecoleGroup.totalUesCount}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Total UEs",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Explorer",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        // Composant 2 : Nombre total de Cours de cette école (Format compact & contraste sombre optimisé)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = coursBg,
                            border = BorderStroke(1.dp, coursBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onExploreEcole() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = coursAccent,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF1B3B26) else Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${ecoleGroup.totalCoursesCount}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = coursText
                                    )
                                    Text(
                                        text = "Total Cours",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFA5D6A7) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Explorer",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = coursAccent
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = coursAccent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// COMPOSANTS FILIÈRES & UES CLIQUABLES
// -------------------------------------------------------------------------------------------------

@Composable
fun FiliereClickableCard(
    item: FiliereAvailabilityItem,
    onClick: () -> Unit
) {
    val palette = FiliereColorProvider.getPalette(
        filiereCode = item.code,
        filiereNom = item.nom,
        fallbackText = item.nom
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, palette.borderColor.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .fillMaxHeight()
                    .background(palette.accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = palette.containerColor,
                        border = BorderStroke(1.dp, palette.borderColor)
                    ) {
                        Text(
                            text = item.code,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.contentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "${item.coursesCount + item.sujetsCount} doc(s)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = item.nom,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!item.ecoleNom.isNullOrBlank()) {
                    Text(
                        text = item.ecoleNom,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.coursesCount} cours • ${item.sujetsCount} épreuves • ${item.uesCount} UE(s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.accentColor
                    )

                    FilledTonalButton(
                        onClick = onClick,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text("Explorer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UeClickableCard(
    ue: UeWithDocuments,
    onClick: () -> Unit
) {
    val filierePalette = FiliereColorProvider.getPalette(
        filiereCode = ue.filiereCode,
        filiereNom = ue.filiereNom,
        fallbackText = ue.code
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(filierePalette.accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = ue.code,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (!ue.filiereCode.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = filierePalette.containerColor
                        ) {
                            Text(
                                text = ue.filiereCode,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = filierePalette.contentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = ue.intitule ?: "Unité d'Enseignement",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${ue.cours?.size ?: 0} cours • ${ue.sujets?.size ?: 0} épreuve(s)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = onClick,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text("Explorer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogueCourseCompactCard(
    cours: Cours,
    isDownloaded: Boolean,
    downloadStatus: DownloadStatus,
    onCardClick: () -> Unit,
    onDownload: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val filierePalette = FiliereColorProvider.getPalette(
        filiereCode = cours.filiereCode,
        filiereNom = cours.filiereNom,
        fallbackText = cours.titre
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .fillMaxHeight()
                    .background(filierePalette.accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "DISPONIBLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (!cours.filiereCode.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = filierePalette.containerColor
                            ) {
                                Text(
                                    text = cours.filiereCode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = filierePalette.contentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = (cours.type ?: "PDF").uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = cours.titre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!cours.ecoleNom.isNullOrBlank()) {
                    Text(
                        text = cours.ecoleNom,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Download / Open / Search Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCardClick,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Explorer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (downloadStatus is DownloadStatus.Progress) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else if (isDownloaded) {
                        Button(
                            onClick = onOpen,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lire", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Télécharger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// BARRES D'OUTILS, STATS & FILTRES
// -------------------------------------------------------------------------------------------------

@Composable
fun CatalogueHeroBanner(stats: CatalogueStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "UNIVERSITÉ DE GAROUA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF2E7D32), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Catalogue en direct",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                Text(
                    text = "Catalogue de Disponibilité des Cours",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "Sélectionnez une école, une filière, une UE ou un cours pour accéder directement aux résultats détaillés dans la recherche.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun CatalogueStatsRow(
    totalEcoles: Int,
    totalFilieres: Int,
    totalUes: Int,
    totalCourses: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Ligne 1 : Établissements & Filières
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Établissements
            CatalogueMetricCard(
                title = "Établissements",
                count = totalEcoles,
                icon = Icons.Default.AccountBalance,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                iconColor = MaterialTheme.colorScheme.primary,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                modifier = Modifier.weight(1f)
            )

            // 2. Filières
            CatalogueMetricCard(
                title = "Filières",
                count = totalFilieres,
                icon = Icons.Default.School,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                iconColor = MaterialTheme.colorScheme.tertiary,
                borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                modifier = Modifier.weight(1f)
            )
        }

        // Ligne 2 : Unités (UEs) & Cours
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. Unités d'Enseignement (UE)
            CatalogueMetricCard(
                title = "Total UEs",
                count = totalUes,
                icon = Icons.Default.BookmarkBorder,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                iconColor = MaterialTheme.colorScheme.secondary,
                borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                modifier = Modifier.weight(1f)
            )

            // 4. Total Cours
            CatalogueMetricCard(
                title = "Total Cours",
                count = totalCourses,
                icon = Icons.Default.MenuBook,
                containerColor = Color(0xFFE8F5E9).copy(alpha = 0.85f),
                iconColor = Color(0xFF2E7D32),
                borderColor = Color(0xFF2E7D32).copy(alpha = 0.3f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CatalogueMetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor,
                contentColor = Color.White,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "$count",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CatalogueSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag("catalogue_search_input"),
        placeholder = {
            Text(
                text = "Rechercher une école, filière, UE, cours...",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Rechercher",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Effacer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    )
}

@Composable
fun CatalogueViewModeTabs(
    selectedMode: Int,
    onSelectMode: (Int) -> Unit,
    ecolesCount: Int,
    filieresCount: Int,
    uesCount: Int,
    coursesCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val modes = listOf(
                "Par École ($ecolesCount)" to 0,
                "Filières ($filieresCount)" to 1,
                "Unités ($uesCount)" to 2,
                "Cours ($coursesCount)" to 3
            )

            modes.forEach { (label, mode) ->
                val isSelected = selectedMode == mode
                Surface(
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCatalogueNotice(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Aucun élément disponible",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
