package com.example.ui.search

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.DownloadStatus
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFiliere by viewModel.selectedFiliere.collectAsState()
    val selectedEcole by viewModel.selectedEcole.collectAsState()
    val selectedNiveau by viewModel.selectedNiveau.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedAnnee by viewModel.selectedAnnee.collectAsState()
    
    val filieres by viewModel.filieres.collectAsState()
    val ecoles by viewModel.ecoles.collectAsState()
    val resultCount by viewModel.resultCount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isNetworkLoading by viewModel.isNetworkLoading.collectAsState()
    val pagination by viewModel.pagination.collectAsState()
    val accessToken by viewModel.accessToken.collectAsState()
    val downloadedDocs by viewModel.downloadedDocuments.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val allLocalFiles by viewModel.allLocalFiles.collectAsState()

    var filiereExpanded by remember { mutableStateOf(false) }
    var ecoleExpanded by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTutorialDialog by remember { mutableStateOf(false) }

    val activeFilterCount = listOfNotNull(
        selectedEcole,
        selectedFiliere,
        selectedType.takeIf { it != "Tous" },
        selectedNiveau.takeIf { it != "Tous" }
    ).size

    // Rescan local files when view enters
    LaunchedEffect(Unit) {
        viewModel.updateDownloadedFilesSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.campus_edu_logo_vector),
                            contentDescription = "Logo Campus Edu",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text(
                                text = "Campus Edu Docs",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Université de Garoua",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Tutorial Action Button
                    IconButton(
                        onClick = { showTutorialDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Tutoriel d'utilisation et de recherche",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Auth Status Badge
                    val hasToken = accessToken.isNotEmpty()
                    AssistChip(
                        onClick = { /* Nothing happens on click */ },
                        label = {
                            Text(
                                text = if (hasToken) "Authentifié" else "Accès Public",
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (hasToken) Icons.Default.VpnKey else Icons.Default.LockOpen,
                                contentDescription = "Token Status",
                                modifier = Modifier.size(14.dp),
                                tint = if (hasToken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (hasToken) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Explorer"
                        )
                    },
                    label = {
                        Text(
                            text = "Explorer",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (allLocalFiles.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${allLocalFiles.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Téléchargés"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Téléchargés",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (selectedTab == 1) {
                DownloadedFilesSection(
                    localFiles = allLocalFiles,
                    onOpenFile = { viewModel.openLocalFile(context, it) },
                    onDeleteFile = { viewModel.deleteLocalFile(context, it) }
                )
            } else {
                // Search Content with scrollable search header when results are loaded
                val searchHeaderContent = @Composable {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Search Bar Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    placeholder = {
                                        Text(
                                            text = "Rechercher un document...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Rechercher",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            if (isNetworkLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .padding(2.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { viewModel.updateSearchQuery("") },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Effacer",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("search_field_q"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                )

                                // Toggle button for parameters/filters
                                Surface(
                                    onClick = { showFilters = !showFilters },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (showFilters || activeFilterCount > 0) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = if (activeFilterCount > 0) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Paramètres de recherche",
                                            tint = if (showFilters || activeFilterCount > 0) MaterialTheme.colorScheme.primary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        if (activeFilterCount > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ) {
                                                Text(text = "$activeFilterCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Icon(
                                            imageVector = if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Collapsible Filters Panel
                            AnimatedVisibility(
                                visible = showFilters,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Panneau de Filtres UI
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // École selector
                                        Box(modifier = Modifier.weight(1f)) {
                                            FilterSelectButton(
                                                label = selectedEcole?.let { it.sigle ?: it.nom } ?: "École (Toutes)",
                                                isSelected = selectedEcole != null,
                                                onClick = {
                                                    viewModel.loadEcoles()
                                                    ecoleExpanded = true
                                                },
                                                onClear = { viewModel.selectEcole(null) },
                                                modifier = Modifier.fillMaxWidth(),
                                                testTag = "ecole_filter_chip"
                                            )

                                            DropdownMenu(
                                                expanded = ecoleExpanded,
                                                onDismissRequest = { ecoleExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Toutes les écoles", fontWeight = FontWeight.Bold) },
                                                    onClick = {
                                                        viewModel.selectEcole(null)
                                                        ecoleExpanded = false
                                                    }
                                                )
                                                Divider()
                                                if (ecoles.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(14.dp),
                                                                    strokeWidth = 2.dp,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                Text("Chargement des écoles...", fontSize = 12.sp, color = Color.Gray)
                                                            }
                                                        },
                                                        onClick = { }
                                                    )
                                                } else {
                                                    ecoles.forEach { ecole ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Column {
                                                                    Text(
                                                                        text = if (!ecole.sigle.isNullOrBlank()) "${ecole.sigle} - ${ecole.nom}" else ecole.nom,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        fontSize = 13.sp
                                                                    )
                                                                    ecole.description?.let { desc ->
                                                                        Text(desc, fontSize = 11.sp, color = Color.Gray)
                                                                    }
                                                                }
                                                            },
                                                            onClick = {
                                                                viewModel.selectEcole(ecole)
                                                                ecoleExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Filière selector
                                        Box(modifier = Modifier.weight(1f)) {
                                            FilterSelectButton(
                                                label = selectedFiliere?.let { it.code } ?: "Filière (Toutes)",
                                                isSelected = selectedFiliere != null,
                                                onClick = {
                                                    viewModel.loadFilieres()
                                                    filiereExpanded = true
                                                },
                                                onClear = { viewModel.selectFiliere(null) },
                                                modifier = Modifier.fillMaxWidth(),
                                                testTag = "filiere_filter_chip"
                                            )

                                            DropdownMenu(
                                                expanded = filiereExpanded,
                                                onDismissRequest = { filiereExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Toutes les filières", fontWeight = FontWeight.Bold) },
                                                    onClick = {
                                                        viewModel.selectFiliere(null)
                                                        filiereExpanded = false
                                                    }
                                                )
                                                Divider()
                                                if (filieres.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(14.dp),
                                                                    strokeWidth = 2.dp,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                Text("Chargement des filières...", fontSize = 12.sp, color = Color.Gray)
                                                            }
                                                        },
                                                        onClick = { }
                                                    )
                                                } else {
                                                    filieres.forEach { filiere ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Column {
                                                                    Text("${filiere.code} - ${filiere.nom}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                                    filiere.departement?.let { dept ->
                                                                        Text(dept, fontSize = 11.sp, color = Color.Gray)
                                                                    }
                                                                }
                                                            },
                                                            onClick = {
                                                                viewModel.selectFiliere(filiere)
                                                                filiereExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Document Type Selector
                                        Box(modifier = Modifier.weight(1f)) {
                                            var typeExpanded by remember { mutableStateOf(false) }
                                            FilterSelectButton(
                                                label = when (selectedType) {
                                                    "cours" -> "Cours"
                                                    "sujet" -> "Sujets Exam"
                                                    "pdf" -> "PDF"
                                                    "video" -> "Vidéo"
                                                    "slide" -> "Slides"
                                                    "partiel" -> "Partiel"
                                                    "rattrapage" -> "Rattrapage"
                                                    "terminal" -> "Terminal"
                                                    "tp" -> "TP"
                                                    "td" -> "TD"
                                                    else -> "Type (Tous)"
                                                },
                                                isSelected = selectedType != "Tous",
                                                onClick = { typeExpanded = true },
                                                onClear = { viewModel.selectType("Tous") },
                                                modifier = Modifier.fillMaxWidth(),
                                                testTag = "type_filter_chip"
                                            )

                                            DropdownMenu(
                                                expanded = typeExpanded,
                                                onDismissRequest = { typeExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Tous les types", fontWeight = FontWeight.Bold) },
                                                    onClick = { viewModel.selectType("Tous"); typeExpanded = false }
                                                )
                                                Divider()
                                                DropdownMenuItem(
                                                    text = { Text("Tous les cours", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("cours"); typeExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Tous les sujets d'examen", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("sujet"); typeExpanded = false }
                                                )
                                                Divider()
                                                DropdownMenuItem(
                                                    text = { Text("Format PDF", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("pdf"); typeExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Vidéo", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("video"); typeExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Support Slide", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("slide"); typeExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Sujet Partiel", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("partiel"); typeExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Sujet Rattrapage", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("rattrapage"); typeExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Examen Terminal", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("terminal"); typeExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("TP (Travaux Pratiques)", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("tp"); typeExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("TD (Travaux Dirigés)", fontSize = 13.sp) },
                                                    onClick = { viewModel.selectType("td"); typeExpanded = false }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Niveau Filter Selector (L1 - M2)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Niveau:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 2.dp)
                                        )
                                        val niveaux = listOf("L1", "L2", "L3", "M1", "M2")
                                        niveaux.forEach { niv ->
                                            val isSelected = selectedNiveau == niv
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable {
                                                        if (isSelected) {
                                                            viewModel.selectNiveau("Tous")
                                                        } else {
                                                            viewModel.selectNiveau(niv)
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = niv,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val resultCountContent = @Composable {
                    if (resultCount != null && uiState is SearchUiState.Success) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$resultCount document(s) trouvé(s)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (isNetworkLoading) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (!showFilters) {
                                    TextButton(
                                        onClick = { showFilters = true },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text(
                                            text = if (activeFilterCount > 0) "Filtres ($activeFilterCount) ⚙️" else "Ajuster filtres ⚙️",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                when (val state = uiState) {
                    is SearchUiState.Success -> {
                        // When documents are loaded, the search bar & filters are in the LazyColumn header and scroll UP smoothly!
                        DocumentList(
                            ues = state.results,
                            downloadedDocs = downloadedDocs,
                            activeDownloads = activeDownloads,
                            pagination = pagination,
                            headerContent = {
                                searchHeaderContent()
                                resultCountContent()
                            },
                            onDownloadCours = { viewModel.downloadCours(context, it) },
                            onDownloadSujet = { sujet, corrige -> viewModel.downloadSujet(context, sujet, corrige) },
                            onOpenCours = { viewModel.openDownloadedCours(context, it) },
                            onOpenSujet = { sujet, corrige -> viewModel.openDownloadedSujet(context, sujet, corrige) },
                            onDeleteCours = { viewModel.deleteDownloadedCours(context, it) },
                            onDeleteSujet = { sujet, corrige -> viewModel.deleteDownloadedSujet(context, sujet, corrige) },
                            onLoadMore = { viewModel.loadMore() }
                        )
                    }
                    else -> {
                        // In non-success states (Idle, Loading, Empty, Error), show search bar at top and state card below
                        Column(modifier = Modifier.fillMaxSize()) {
                            searchHeaderContent()
                            resultCountContent()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                when (state) {
                                    is SearchUiState.Idle -> {
                                        StateDisplayCard(
                                            icon = Icons.Default.School,
                                            badgeBackgroundColor = Color.Transparent,
                                            iconTint = MaterialTheme.colorScheme.primary,
                                            drawableRes = com.example.R.drawable.campus_edu_logo_vector,
                                            title = "Prêt à chercher !",
                                            titleColor = MaterialTheme.colorScheme.primary,
                                            description = "Saisissez un mot-clé (code UE, nom du cours, enseignant) ou utilisez les filtres pour explorer les cours et épreuves de Campus Edu.",
                                            buttonText = "Comment rechercher ? (Tutoriel)",
                                            buttonIcon = Icons.Default.HelpOutline,
                                            onButtonClick = { showTutorialDialog = true }
                                        )
                                    }
                                    is SearchUiState.Loading -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(24.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(48.dp),
                                                    strokeWidth = 4.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(20.dp))
                                                Text(
                                                    text = "Recherche de documents en cours...",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Interrogation de l'API Campus Edu...",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    is SearchUiState.Empty -> {
                                        val desc = if (!state.queryOrFilter.isNullOrBlank()) {
                                            "Aucun document trouvé pour « ${state.queryOrFilter} ». La recherche ne retourne aucun résultat. Modifiez vos mots-clés ou réinitialisez les filtres."
                                        } else {
                                            "Recherche vide : Aucun cours ni épreuve ne correspond aux filtres sélectionnés. Essayez d'élargir vos critères de recherche."
                                        }
                                        StateDisplayCard(
                                            icon = Icons.Default.SearchOff,
                                            badgeBackgroundColor = Color(0xFFFFF3E0),
                                            iconTint = Color(0xFFE65100),
                                            title = "Recherche vide — Aucun résultat",
                                            titleColor = Color(0xFFE65100),
                                            description = desc,
                                            buttonText = "Réinitialiser la recherche",
                                            buttonIcon = Icons.Default.Refresh,
                                            onButtonClick = {
                                                viewModel.updateSearchQuery("")
                                                viewModel.selectFiliere(null)
                                                viewModel.selectEcole(null)
                                                viewModel.selectNiveau("Tous")
                                                viewModel.selectType("Tous")
                                                viewModel.selectAnnee("Toutes")
                                            }
                                        )
                                    }
                                    is SearchUiState.NetworkError -> {
                                        Column {
                                            StateDisplayCard(
                                                icon = Icons.Default.WifiOff,
                                                badgeBackgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                                iconTint = MaterialTheme.colorScheme.error,
                                                title = "Mode Hors Connexion — Erreur Réseau",
                                                titleColor = MaterialTheme.colorScheme.error,
                                                description = "Impossible de joindre le serveur. Vous pouvez accéder à tous vos documents déjà téléchargés ci-dessous.",
                                                buttonText = "Réessayer la connexion",
                                                buttonIcon = Icons.Default.Refresh,
                                                onButtonClick = { viewModel.performSearch() }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            DownloadedFilesSection(
                                                localFiles = allLocalFiles,
                                                onOpenFile = { viewModel.openLocalFile(context, it) },
                                                onDeleteFile = { viewModel.deleteLocalFile(context, it) }
                                            )
                                        }
                                    }
                                    is SearchUiState.ServerError -> {
                                        StateDisplayCard(
                                            icon = Icons.Default.Dns,
                                            badgeBackgroundColor = Color(0xFFF3E5F5),
                                            iconTint = Color(0xFF6A1B9A),
                                            title = "Erreur Serveur (HTTP ${state.code ?: 500})",
                                            titleColor = Color(0xFF6A1B9A),
                                            description = state.message,
                                            buttonText = "Réessayer",
                                            buttonIcon = Icons.Default.Refresh,
                                            onButtonClick = { viewModel.performSearch() }
                                        )
                                    }
                                    is SearchUiState.Error -> {
                                        StateDisplayCard(
                                            icon = Icons.Default.ReportProblem,
                                            badgeBackgroundColor = Color(0xFFFFEBEE),
                                            iconTint = MaterialTheme.colorScheme.error,
                                            title = "Erreur de chargement",
                                            titleColor = MaterialTheme.colorScheme.error,
                                            description = state.message,
                                            buttonText = "Réessayer",
                                            buttonIcon = Icons.Default.Refresh,
                                            onButtonClick = { viewModel.performSearch() }
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTutorialDialog) {
        SearchTutorialDialog(
            onDismiss = { showTutorialDialog = false },
            onApplySearch = { query, type, niveau ->
                showTutorialDialog = false
                if (type != null) viewModel.selectType(type)
                if (niveau != null) viewModel.selectNiveau(niveau)
                viewModel.updateSearchQuery(query)
            }
        )
    }
}

@Composable
fun WelcomeHubView(
    ecoles: List<Ecole>,
    filieres: List<Filiere>,
    onSelectType: (String) -> Unit,
    onSelectNiveau: (String) -> Unit,
    onSelectEcole: (Ecole) -> Unit,
    onSelectFiliere: (Filiere) -> Unit,
    onSearchSuggestion: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Header Card with modern gradient
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E3A8A), // Deep Oxford Navy
                                Color(0xFF0F766E)  // Dark Spruce Teal
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "UNIVERSITÉ DE GAROUA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "Portail Académique",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "Accédez instantanément à l'ensemble des cours, travaux dirigés, travaux pratiques et épreuves officielles de votre filière.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Quick Category Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Explorer par type de ressource",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryQuickCard(
                    modifier = Modifier.weight(1f),
                    title = "Cours Magistraux",
                    subtitle = "Supports complets",
                    icon = Icons.Default.MenuBook,
                    accentColor = Color(0xFF1E3A8A),
                    containerColor = Color(0xFFE8EEF9),
                    onClick = { onSelectType("cours") }
                )
                CategoryQuickCard(
                    modifier = Modifier.weight(1f),
                    title = "Épreuves & Partiels",
                    subtitle = "Sessions normales",
                    icon = Icons.Default.Assignment,
                    accentColor = Color(0xFF0F766E),
                    containerColor = Color(0xFFE0F2F1),
                    onClick = { onSelectType("partiel") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryQuickCard(
                    modifier = Modifier.weight(1f),
                    title = "Travaux Pratiques",
                    subtitle = "Guides & protocoles",
                    icon = Icons.Default.Science,
                    accentColor = Color(0xFF7C3AED),
                    containerColor = Color(0xFFF3E8FF),
                    onClick = { onSelectType("tp") }
                )
                CategoryQuickCard(
                    modifier = Modifier.weight(1f),
                    title = "Travaux Dirigés",
                    subtitle = "Exercices & corrigés",
                    icon = Icons.Default.EditNote,
                    accentColor = Color(0xFFB45309),
                    containerColor = Color(0xFFFEF3C7),
                    onClick = { onSelectType("td") }
                )
            }
        }

        // Levels Quick Filter
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Sélection rapide par niveau d'étude",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val niveaux = listOf(
                "L1" to "Licence 1",
                "L2" to "Licence 2",
                "L3" to "Licence 3",
                "M1" to "Master 1",
                "M2" to "Master 2"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                niveaux.forEach { (code, label) ->
                    Surface(
                        onClick = { onSelectNiveau(code) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = code,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Établissements & Facultés
        if (ecoles.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Établissements & Facultés",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ecoles.forEach { ecole ->
                        Surface(
                            onClick = { onSelectEcole(ecole) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = ecole.sigle ?: ecole.nom,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = ecole.nom,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // App highlights info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = null,
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Mode hors-ligne disponible",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Téléchargez vos épreuves et cours une fois pour les consulter à tout moment depuis l'onglet « Téléchargés », même sans connexion internet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun CategoryQuickCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier.height(96.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = accentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StateDisplayCard(
    icon: ImageVector,
    badgeBackgroundColor: Color,
    iconTint: Color,
    title: String,
    titleColor: Color,
    description: String,
    drawableRes: Int? = null,
    buttonText: String? = null,
    buttonIcon: ImageVector? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = badgeBackgroundColor,
                modifier = Modifier.size(80.dp),
                border = if (drawableRes == null) BorderStroke(1.dp, iconTint.copy(alpha = 0.2f)) else null
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (drawableRes != null) {
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(42.dp),
                            tint = iconTint
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            if (!buttonText.isNullOrBlank() && onButtonClick != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onButtonClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = titleColor)
                ) {
                    if (buttonIcon != null) {
                        Icon(
                            imageVector = buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = buttonText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentList(
    ues: List<UeWithDocuments>,
    downloadedDocs: Set<String>,
    activeDownloads: Map<String, DownloadStatus>,
    pagination: Pagination?,
    headerContent: (@Composable () -> Unit)? = null,
    onDownloadCours: (Cours) -> Unit,
    onDownloadSujet: (Sujet, Boolean) -> Unit,
    onOpenCours: (Cours) -> Unit,
    onOpenSujet: (Sujet, Boolean) -> Unit,
    onDeleteCours: (Cours) -> Unit,
    onDeleteSujet: (Sujet, Boolean) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (headerContent != null) {
            item(key = "search_header_content") {
                headerContent()
            }
        }

        items(ues, key = { "ue_card_${it.id}" }) { ue ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                UeUnifiedDocumentCard(
                    ue = ue,
                    downloadedDocs = downloadedDocs,
                    activeDownloads = activeDownloads,
                    onDownloadCours = onDownloadCours,
                    onDownloadSujet = onDownloadSujet,
                    onOpenCours = onOpenCours,
                    onOpenSujet = onOpenSujet,
                    onDeleteCours = onDeleteCours,
                    onDeleteSujet = onDeleteSujet
                )
            }
        }

        // Pagination Footer
        if (pagination != null && pagination.hasNext) {
            item(key = "load_more_button") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onLoadMore,
                        modifier = Modifier.testTag("load_more_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Charger plus")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Charger plus (Page ${pagination.page}/${pagination.totalPages})")
                    }
                }
            }
        }
    }
}

@Composable
fun UeUnifiedDocumentCard(
    ue: UeWithDocuments,
    downloadedDocs: Set<String>,
    activeDownloads: Map<String, DownloadStatus>,
    onDownloadCours: (Cours) -> Unit,
    onDownloadSujet: (Sujet, Boolean) -> Unit,
    onOpenCours: (Cours) -> Unit,
    onOpenSujet: (Sujet, Boolean) -> Unit,
    onDeleteCours: (Cours) -> Unit,
    onDeleteSujet: (Sujet, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val coursList = ue.cours ?: emptyList()
        coursList.forEach { cours ->
            val isDownloaded = downloadedDocs.contains("cours_${cours.id}")
            val downloadStatus = activeDownloads["cours_${cours.id}"] ?: DownloadStatus.Idle

            CoursItemCard(
                ue = ue,
                cours = cours,
                isDownloaded = isDownloaded,
                downloadStatus = downloadStatus,
                onDownload = { onDownloadCours(cours) },
                onOpen = { onOpenCours(cours) },
                onDelete = { onDeleteCours(cours) }
            )
        }

        val sujetsList = ue.sujets ?: emptyList()
        sujetsList.forEach { sujet ->
            val isSujetDownloaded = downloadedDocs.contains("sujet_${sujet.id}_sujet")
            val isCorrigeDownloaded = downloadedDocs.contains("sujet_${sujet.id}_corrige")
            val sujetDownloadStatus = activeDownloads["sujet_${sujet.id}_sujet"] ?: DownloadStatus.Idle
            val corrigeDownloadStatus = activeDownloads["sujet_${sujet.id}_corrige"] ?: DownloadStatus.Idle

            SujetItemCard(
                ue = ue,
                sujet = sujet,
                isSujetDownloaded = isSujetDownloaded,
                isCorrigeDownloaded = isCorrigeDownloaded,
                sujetDownloadStatus = sujetDownloadStatus,
                corrigeDownloadStatus = corrigeDownloadStatus,
                onDownloadSujet = { onDownloadSujet(sujet, false) },
                onDownloadCorrige = { onDownloadSujet(sujet, true) },
                onOpenSujet = { onOpenSujet(sujet, false) },
                onOpenCorrige = { onOpenSujet(sujet, true) },
                onDeleteSujet = { onDeleteSujet(sujet, false) },
                onDeleteCorrige = { onDeleteSujet(sujet, true) }
            )
        }

        if (coursList.isEmpty() && sujetsList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = "Aucun document répertorié.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

fun formatCodeUe(code: String?): String {
    if (code.isNullOrBlank()) return ""
    return code
}

@Composable
fun UeHeaderItem(ue: UeWithDocuments) {
    val formattedCode = remember(ue.code) { formatCodeUe(ue.code) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // UE Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedCode.take(3),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (ue.credits != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("${ue.credits} Crédits", fontSize = 10.sp) },
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
                Text(
                    text = ue.intitule,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (ue.semestre != null || ue.niveau != null) {
                    Text(
                        text = "Semestre ${ue.semestre ?: "?"} • Niveau ${ue.niveau ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CoursItemCard(
    ue: UeWithDocuments,
    cours: Cours,
    isDownloaded: Boolean,
    downloadStatus: DownloadStatus,
    onDownload: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedCode = remember(ue.code) { formatCodeUe(ue.code) }
    val anneEnCours = remember(cours) {
        cours.anneAcademique ?: "2023-2024"
    }
    val semestreEnCours = remember(ue) {
        if (ue.semestre.isNullOrBlank() || ue.semestre == "Semestre") "Semestre 1" else ue.semestre
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isDownloaded) {
                    onOpen()
                } else if (downloadStatus !is DownloadStatus.Progress) {
                    onDownload()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header: Code UE badge + Download status badge + Type badge with Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = formattedCode,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (cours.deja_telecharge == true || isDownloaded) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Téléchargé",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (cours.type ?: "COURS").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Document Title & Description
            Text(
                text = cours.titre,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!cours.description.isNullOrEmpty()) {
                Text(
                    text = cours.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges row: Année, Semestre, Niveau, Taille, Téléchargements
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Année : $anneEnCours",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                Text(
                    text = semestreEnCours,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (ue.niveau != null) {
                    Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "Niveau ${ue.niveau}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (cours.tailleFichier != null) {
                    Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = formatFileSize(cours.tailleFichier),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                if ((cours.telechargemements ?: 0) > 0) {
                    Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Téléchargements",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " ${cours.telechargemements}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Progress bar if downloading
            AnimatedVisibility(
                visible = downloadStatus is DownloadStatus.Progress,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (downloadStatus is DownloadStatus.Progress) {
                    val progressInfo = downloadStatus as DownloadStatus.Progress
                    val speedText = formatSpeed(progressInfo.speedKBps)
                    val sizeText = formatProgressDetail(progressInfo.downloadedBytes, progressInfo.totalBytes)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (sizeText.isNotBlank()) "Téléchargement ($sizeText)" else "Téléchargement...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (progressInfo.speedKBps > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = speedText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${progressInfo.progress}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressInfo.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            if (downloadStatus is DownloadStatus.Error) {
                Text(
                    text = downloadStatus.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isDownloaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_cours_${cours.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SujetItemCard(
    ue: UeWithDocuments,
    sujet: Sujet,
    isSujetDownloaded: Boolean,
    isCorrigeDownloaded: Boolean,
    sujetDownloadStatus: DownloadStatus,
    corrigeDownloadStatus: DownloadStatus,
    onDownloadSujet: () -> Unit,
    onDownloadCorrige: () -> Unit,
    onOpenSujet: () -> Unit,
    onOpenCorrige: () -> Unit,
    onDeleteSujet: () -> Unit,
    onDeleteCorrige: () -> Unit
) {
    val formattedCode = remember(ue.code) { formatCodeUe(ue.code) }
    val anneEnCours = remember(sujet) {
        sujet.annee ?: "2023-2024"
    }
    val semestreEnCours = remember(ue) {
        if (ue.semestre.isNullOrBlank() || ue.semestre == "Semestre") "Semestre 1" else ue.semestre
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSujetDownloaded) {
                    onOpenSujet()
                } else if (sujetDownloadStatus !is DownloadStatus.Progress) {
                    onDownloadSujet()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Code UE badge + Download status badge + Type badge with Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = formattedCode,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (sujet.deja_telecharge == true || isSujetDownloaded) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Téléchargé",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (sujet.type ?: "EXAMEN").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = sujet.titre,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Badges row: Année, Semestre, Session, Téléchargements
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Année : $anneEnCours",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                Text(
                    text = semestreEnCours,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (sujet.session != null) {
                    Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "Session : ${sujet.session}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                if ((sujet.telechargemements ?: 0) > 0) {
                    Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Téléchargements",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " ${sujet.telechargemements}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Progress sujet
            AnimatedVisibility(visible = sujetDownloadStatus is DownloadStatus.Progress) {
                if (sujetDownloadStatus is DownloadStatus.Progress) {
                    val progressInfo = sujetDownloadStatus as DownloadStatus.Progress
                    val speedText = formatSpeed(progressInfo.speedKBps)
                    val sizeText = formatProgressDetail(progressInfo.downloadedBytes, progressInfo.totalBytes)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (sizeText.isNotBlank()) "Sujet ($sizeText)" else "Téléchargement du sujet...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (progressInfo.speedKBps > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = speedText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${progressInfo.progress}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressInfo.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            if (sujetDownloadStatus is DownloadStatus.Error) {
                Text(
                    text = sujetDownloadStatus.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isSujetDownloaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDeleteSujet,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_sujet_file_${sujet.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer sujet",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Option 2: THE ANSWER SHEET (CORRIGÉ) if available
            if (sujet.avecCorrige == true) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isCorrigeDownloaded) {
                                onOpenCorrige()
                            } else if (corrigeDownloadStatus !is DownloadStatus.Progress) {
                                onDownloadCorrige()
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFFD84315),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Corrigé officiel de l'enseignant",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFFD84315)
                        )
                    }

                    Text(
                        text = if (isCorrigeDownloaded) "Téléchargé ✅" else "Télécharger 📥",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrigeDownloaded) Color(0xFF2E7D32) else Color(0xFFD84315)
                    )
                }

                // Progress corrigé
                AnimatedVisibility(visible = corrigeDownloadStatus is DownloadStatus.Progress) {
                    if (corrigeDownloadStatus is DownloadStatus.Progress) {
                        val progressInfo = corrigeDownloadStatus as DownloadStatus.Progress
                        val speedText = formatSpeed(progressInfo.speedKBps)
                        val sizeText = formatProgressDetail(progressInfo.downloadedBytes, progressInfo.totalBytes)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .background(
                                    color = Color(0xFFFBE9E7),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (sizeText.isNotBlank()) "Corrigé ($sizeText)" else "Téléchargement du corrigé...",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD84315),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (progressInfo.speedKBps > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFFCCBC),
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = speedText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD84315),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${progressInfo.progress}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD84315)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progressInfo.progress / 100f },
                                color = Color(0xFFD84315),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }

                if (corrigeDownloadStatus is DownloadStatus.Error) {
                    Text(
                        text = corrigeDownloadStatus.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (isCorrigeDownloaded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDeleteCorrige,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("delete_corrige_file_${sujet.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer corrigé",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Dialog to input Authorization Access Token for sujets
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenInputDialog(
    initialToken: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tokenText by remember { mutableStateOf(initialToken) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuration d'accès JWT") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Les sujets d'examens et corrigés requièrent une authentification active par jeton d'accès (Bearer Token).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = tokenText,
                    onValueChange = { tokenText = it },
                    label = { Text("AccessToken Bearer") },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("jwt_token_field"),
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
                if (tokenText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { tokenText = "" }) {
                        Text("Vider le champ", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(tokenText) },
                modifier = Modifier.testTag("save_token_button")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.FRANCE, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatSpeed(speedKBps: Double): String {
    return if (speedKBps >= 1024) {
        String.format(java.util.Locale.FRANCE, "%.1f Mo/s", speedKBps / 1024.0)
    } else {
        String.format(java.util.Locale.FRANCE, "%.0f Ko/s", speedKBps)
    }
}

fun formatProgressDetail(downloadedBytes: Long, totalBytes: Long): String {
    fun formatBytes(bytes: Long): String {
        return if (bytes >= 1024 * 1024) {
            String.format(java.util.Locale.FRANCE, "%.1f Mo", bytes / (1024.0 * 1024.0))
        } else {
            String.format(java.util.Locale.FRANCE, "%.0f Ko", bytes / 1024.0)
        }
    }
    return if (totalBytes > 0) {
        "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
    } else if (downloadedBytes > 0) {
        formatBytes(downloadedBytes)
    } else {
        ""
    }
}

@Composable
fun FilterSelectButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(38.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isSelected) Icons.Default.Close else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .clickable {
                        if (isSelected) onClear() else onClick()
                    }
            )
        }
    }
}

@Composable
fun DownloadedFilesSection(
    localFiles: List<com.example.data.repository.LocalDownloadedDocument>,
    onOpenFile: (com.example.data.repository.LocalDownloadedDocument) -> Unit,
    onDeleteFile: (com.example.data.repository.LocalDownloadedDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredFiles = remember(localFiles, searchQuery) {
        if (searchQuery.isBlank()) localFiles
        else localFiles.filter { it.formattedTitle.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxWidth().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Documents Téléchargés (${localFiles.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (localFiles.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucun document téléchargé",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Téléchargez des cours ou épreuves en ligne pour pouvoir les consulter ici à tout moment, même sans connexion internet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filtrer vos fichiers hors ligne...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 600.dp)
            ) {
                items(filteredFiles, key = { it.name }) { doc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = doc.formattedTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = doc.typeLabel,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text("•", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            text = doc.sizeFormatted,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { onOpenFile(doc) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ouvrir", fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = { onDeleteFile(doc) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTutorialDialog(
    onDismiss: () -> Unit,
    onApplySearch: (query: String, type: String?, niveau: String?) -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Guide & Tutoriel",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Comment trouver vos documents",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Section Tabs
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    TabRow(
                        selectedTabIndex = selectedSection,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedSection == 0,
                            onClick = { selectedSection = 0 },
                            text = { Text("Recherche", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = selectedSection == 1,
                            onClick = { selectedSection = 1 },
                            text = { Text("Filtres", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = selectedSection == 2,
                            onClick = { selectedSection = 2 },
                            text = { Text("Hors-ligne", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Section Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedSection) {
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TutorialStepItem(
                                    number = "1",
                                    title = "Recherche par code d'UE",
                                    description = "Tapez le code officiel du cours (ex: INF101, MAT202, BIO111) pour trouver directement l'épreuve ou le support associé."
                                )
                                TutorialStepItem(
                                    number = "2",
                                    title = "Recherche par matière ou mot-clé",
                                    description = "Saisissez un mot-clé comme « Algèbre », « Programmation C », « Droit commercial » ou le nom de votre enseignant."
                                )
                                TutorialStepItem(
                                    number = "3",
                                    title = "Validation automatique",
                                    description = "La recherche se lance dès que vous appuyez sur Entrée ou sur le bouton « Rechercher » du clavier."
                                )

                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Essayer un exemple rapide :",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SuggestionChip(
                                        onClick = { onApplySearch("Informatique", null, null) },
                                        label = { Text("Informatique", fontSize = 11.sp) },
                                        icon = {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                    SuggestionChip(
                                        onClick = { onApplySearch("Mathématiques", null, null) },
                                        label = { Text("Mathématiques", fontSize = 11.sp) },
                                        icon = {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                    SuggestionChip(
                                        onClick = { onApplySearch("Économie", null, null) },
                                        label = { Text("Économie", fontSize = 11.sp) },
                                        icon = {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }
                        }
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TutorialStepItem(
                                    number = "1",
                                    title = "Afficher les filtres avancés",
                                    description = "Touchez le bouton « Filtres avancés » sous la barre de recherche pour déplier tous les critères disponibles."
                                )
                                TutorialStepItem(
                                    number = "2",
                                    title = "Établissement & Filière",
                                    description = "Sélectionnez votre Faculté ou École (ex: IUT, FALSH, FS, FSEG, FSJP) puis votre Filière spécifique."
                                )
                                TutorialStepItem(
                                    number = "3",
                                    title = "Niveau d'étude & Type de ressource",
                                    description = "Ciblez votre niveau (L1, L2, L3, M1, M2) et choisissez le format souhaité : Cours, Épreuve, TD ou TP."
                                )

                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Raccourcis de filtrage :",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SuggestionChip(
                                        onClick = { onApplySearch("", "partiel", "L1") },
                                        label = { Text("Épreuves L1", fontSize = 11.sp) },
                                        icon = {
                                            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                    SuggestionChip(
                                        onClick = { onApplySearch("", "cours", "L2") },
                                        label = { Text("Cours L2", fontSize = 11.sp) },
                                        icon = {
                                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                    SuggestionChip(
                                        onClick = { onApplySearch("", "tp", null) },
                                        label = { Text("Travaux Pratiques", fontSize = 11.sp) },
                                        icon = {
                                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }
                        }
                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TutorialStepItem(
                                    number = "1",
                                    title = "Téléchargement en un clic",
                                    description = "Appuyez sur le bouton vert « Télécharger » à droite de n'importe quel document trouvé."
                                )
                                TutorialStepItem(
                                    number = "2",
                                    title = "Consultation sans connexion",
                                    description = "Basculez sur l'onglet « Téléchargés » dans la barre en bas pour accéder à vos documents enregistrés hors-ligne."
                                )
                                TutorialStepItem(
                                    number = "3",
                                    title = "Gestion du stockage",
                                    description = "Vous pouvez ouvrir directement vos fichiers dans votre lecteur PDF favori ou supprimer les anciens documents pour libérer de l'espace."
                                )
                            }
                        }
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("C'est compris !", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialStepItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
