package com.example.ui.search

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.ui.catalogue.CatalogueScreen
import com.example.ui.blog.ScientificBlogScreen
import com.example.ui.gemini.GeminiStudyChatScreen
import com.example.data.repository.LocalDownloadedDocument
import com.example.ui.components.SearchSkeleton
import com.example.utils.GeminiShareHelper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.DownloadStatus
import com.example.ui.theme.FiliereColorPalette
import com.example.ui.theme.FiliereColorProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var selectedDocForAiOptions by remember { mutableStateOf<LocalDownloadedDocument?>(null) }
    var missingAiApp by remember { mutableStateOf<com.example.utils.AiAppTarget?>(null) }
    var newlyDownloadedNoticeDoc by remember { mutableStateOf<LocalDownloadedDocument?>(null) }

    // When a document finishes downloading, notify user and provide immediate "Étudier avec IA"
    LaunchedEffect(Unit) {
        viewModel.newlyDownloadedDocFlow.collect { localDoc ->
            newlyDownloadedNoticeDoc = localDoc
        }
    }

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
                    Column {
                        when (selectedTab) {
                            0 -> {
                                Text(
                                    text = "Campus Educatif",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Cours disponible",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            1 -> {
                                Text(
                                    text = "Recherche Académique",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Université de Garoua",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            2 -> {
                                Text(
                                    text = "Documents Téléchargés",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "${allLocalFiles.size} fichier(s) hors-ligne",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            else -> {
                                Text(
                                    text = "Notification",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Université de Garoua",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_univ_garoua),
                            contentDescription = "Logo Université de Garoua",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    // Tab 0 : Catalogue
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.AutoStories else Icons.Default.MenuBook,
                                contentDescription = "Catalogue"
                            )
                        },
                        label = {
                            Text(
                                text = "Catalogue",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Tab 1 : Recherche
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Recherche"
                            )
                        },
                        label = {
                            Text(
                                text = "Recherche",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Tab 2 : Fichiers Téléchargés
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
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
                                    imageVector = if (selectedTab == 2) Icons.Default.Folder else Icons.Default.FolderOpen,
                                    contentDescription = "Fichiers"
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "Fichiers",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Tab 3 : Notification
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notification"
                            )
                        },
                        label = {
                            Text(
                                text = "Notification",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> {
                    // Page 1 : Première page de catalogue de la disponibilité des cours sur la plateforme
                    CatalogueScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { selectedTab = 1 }
                    )
                }
                2 -> {
                    // Tab 2: Fichiers téléchargés en local avec classement par type
                    DownloadedFilesSection(
                        localFiles = allLocalFiles,
                        onOpenFile = { fileDoc ->
                            viewModel.openLocalFile(context, fileDoc)
                        },
                        onShareFile = { viewModel.shareLocalFile(context, it) },
                        onDeleteFile = { viewModel.deleteLocalFile(context, it) },
                        onStudyWithAi = { doc ->
                            selectedDocForAiOptions = doc
                        }
                    )
                }
                3 -> {
                    // Tab 3: Notifications et mises à jour de l'application
                    ScientificBlogScreen()
                }
                else -> {
                    // Tab 1: Recherche textuelle & filtres
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
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
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
                                            if (resultCount != null && uiState is SearchUiState.Success) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "$resultCount",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
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
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            // Search submitted
                                        }
                                    ),
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

                                    // Ligne 1: École & Filière
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // École selector
                                        Box(modifier = Modifier.weight(1f)) {
                                            FilterSelectButton(
                                                label = selectedEcole?.let { it.sigle ?: it.nom } ?: "École (Toutes)",
                                                isSelected = selectedEcole != null,
                                                onClick = {
                                                    ecoleExpanded = true
                                                },
                                                onClear = { viewModel.selectEcole(null) },
                                                modifier = Modifier.fillMaxWidth(),
                                                testTag = "ecole_filter_chip"
                                            )

                                            DropdownMenu(
                                                expanded = ecoleExpanded,
                                                onDismissRequest = { ecoleExpanded = false },
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .widthIn(min = 240.dp, max = 320.dp)
                                                    .heightIn(max = 280.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
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
                                                        val isFs = ecole.nom.equals("FS", ignoreCase = true) ||
                                                                ecole.sigle?.equals("FS", ignoreCase = true) == true ||
                                                                ecole.nom.contains("Sciences", ignoreCase = true)
                                                        val isFmsb = ecole.nom.contains("FMSB", ignoreCase = true) ||
                                                                ecole.sigle?.equals("FMSB", ignoreCase = true) == true ||
                                                                ecole.nom.contains("Médecine", ignoreCase = true) ||
                                                                ecole.nom.contains("Biomédicales", ignoreCase = true)
                                                        DropdownMenuItem(
                                                            leadingIcon = if (isFmsb) {
                                                                {
                                                                    Image(
                                                                        painter = painterResource(id = R.drawable.logo_fmsb),
                                                                        contentDescription = "Logo FMSB",
                                                                        modifier = Modifier
                                                                            .size(24.dp)
                                                                            .clip(CircleShape),
                                                                        contentScale = ContentScale.Fit
                                                                    )
                                                                }
                                                            } else if (isFs) {
                                                                {
                                                                    Image(
                                                                        painter = painterResource(id = R.drawable.logo_fs),
                                                                        contentDescription = "Logo FS",
                                                                        modifier = Modifier
                                                                            .size(24.dp)
                                                                            .clip(CircleShape),
                                                                        contentScale = ContentScale.Fit
                                                                    )
                                                                }
                                                            } else null,
                                                            text = {
                                                                Column {
                                                                    Text(
                                                                        text = if (ecole.nom.equals("FS", ignoreCase = true)) "FS - Faculté des Sciences"
                                                                        else if (ecole.nom.equals("FMSB", ignoreCase = true)) "FMSB - Faculté de Médecine et des Sciences Biomédicales"
                                                                        else if (!ecole.sigle.isNullOrBlank()) "${ecole.sigle} - ${ecole.nom}" else ecole.nom,
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
                                                    filiereExpanded = true
                                                },
                                                onClear = { viewModel.selectFiliere(null) },
                                                modifier = Modifier.fillMaxWidth(),
                                                testTag = "filiere_filter_chip"
                                            )

                                            DropdownMenu(
                                                expanded = filiereExpanded,
                                                onDismissRequest = { filiereExpanded = false },
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .widthIn(min = 260.dp, max = 340.dp)
                                                    .heightIn(max = 300.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
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
                                                        val filierePal = FiliereColorProvider.getPalette(filiere.code, filiere.nom)
                                                        DropdownMenuItem(
                                                            leadingIcon = {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(10.dp)
                                                                        .background(filierePal.accentColor, CircleShape)
                                                                )
                                                            },
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
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Ligne 2: Niveau Filter Selector & Document Type Selector
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Niveau Filter Selector (L1 - M2)
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
                                            Surface(
                                                onClick = {
                                                    if (isSelected) {
                                                        viewModel.selectNiveau("Tous")
                                                    } else {
                                                        viewModel.selectNiveau(niv)
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = niv,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(24.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )

                                        // Document Type Selector
                                        Box(modifier = Modifier.widthIn(min = 120.dp, max = 150.dp)) {
                                            var typeExpanded by remember { mutableStateOf(false) }
                                            FilterSelectButton(
                                                label = when (selectedType) {
                                                    "cours" -> "Cours"
                                                    "sujet" -> "Sujets Exam"
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
                                                onDismissRequest = { typeExpanded = false },
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .widthIn(min = 200.dp, max = 280.dp)
                                                    .heightIn(max = 280.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Tous les types", fontWeight = FontWeight.Bold) },
                                                    onClick = { viewModel.selectType("Tous"); typeExpanded = false }
                                                )
                                                Divider()
                                                DropdownMenuItem(
                                                    text = { Text("Cours", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                                    onClick = { viewModel.selectType("cours"); typeExpanded = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Tous les sujets d'examen", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                                    onClick = { viewModel.selectType("sujet"); typeExpanded = false }
                                                )
                                                Divider()
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
                            },
                            onDownloadCours = { viewModel.downloadCours(context, it) },
                            onDownloadSujet = { sujet, corrige -> viewModel.downloadSujet(context, sujet, corrige) },
                            onOpenCours = { cours ->
                                viewModel.openDownloadedCours(context, cours)
                            },
                            onOpenSujet = { sujet, corrige ->
                                viewModel.openDownloadedSujet(context, sujet, corrige)
                            },
                            onStudyCoursWithAi = { cours ->
                                val doc = viewModel.getLocalDocumentForCours(context, cours)
                                if (doc != null) {
                                    selectedDocForAiOptions = doc
                                } else {
                                    Toast.makeText(context, "Veuillez d'abord télécharger ce cours pour l'étudier avec l'IA.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onStudySujetWithAi = { sujet, isCorrige ->
                                val doc = viewModel.getLocalDocumentForSujet(context, sujet, isCorrige)
                                if (doc != null) {
                                    selectedDocForAiOptions = doc
                                } else {
                                    Toast.makeText(context, "Veuillez d'abord télécharger ce document pour l'étudier avec l'IA.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShareCours = { viewModel.shareDownloadedCours(context, it) },
                            onShareSujet = { sujet, corrige -> viewModel.shareDownloadedSujet(context, sujet, corrige) },
                            onDeleteCours = { viewModel.deleteDownloadedCours(context, it) },
                            onDeleteSujet = { sujet, corrige -> viewModel.deleteDownloadedSujet(context, sujet, corrige) },
                            onLoadMore = { viewModel.loadMore() }
                        )
                    }
                    else -> {
                        // In non-success states (Idle, Loading, Empty, Error), show search bar at top and state card below
                        Column(modifier = Modifier.fillMaxSize()) {
                            searchHeaderContent()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                when (state) {
                                    is SearchUiState.Idle -> {
                                        // Clean empty view before search is performed
                                    }
                                    is SearchUiState.Loading -> {
                                        SearchSkeleton()
                                    }
                                    is SearchUiState.Empty -> {
                                        StateDisplayCard(
                                            icon = Icons.Default.SearchOff,
                                            badgeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            iconTint = MaterialTheme.colorScheme.primary,
                                            title = "Aucun document trouvé",
                                            titleColor = MaterialTheme.colorScheme.onSurface,
                                            description = "Modifiez vos filtres ou mots-clés",
                                            buttonText = "Réinitialiser les filtres",
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
                                                badgeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                iconTint = Color(0xFF64748B),
                                                title = "Mode hors-ligne actif",
                                                titleColor = MaterialTheme.colorScheme.onSurface,
                                                description = "Accédez à vos documents enregistrés",
                                                buttonText = "Réessayer la connexion",
                                                buttonIcon = Icons.Default.Refresh,
                                                onButtonClick = { viewModel.performSearch() }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            DownloadedFilesSection(
                                                localFiles = allLocalFiles,
                                                onOpenFile = { fileDoc ->
                                                    viewModel.openLocalFile(context, fileDoc)
                                                },
                                                onShareFile = { fileDoc ->
                                                    viewModel.shareLocalFile(context, fileDoc)
                                                },
                                                onDeleteFile = { viewModel.deleteLocalFile(context, it) },
                                                onStudyWithAi = { doc ->
                                                    selectedDocForAiOptions = doc
                                                }
                                            )
                                        }
                                    }
                                    is SearchUiState.ServerError -> {
                                        StateDisplayCard(
                                            icon = Icons.Default.Dns,
                                            badgeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            iconTint = Color(0xFF64748B),
                                            title = "Serveur indisponible",
                                            titleColor = MaterialTheme.colorScheme.onSurface,
                                            description = "Veuillez réessayer dans un instant",
                                            buttonText = "Réessayer",
                                            buttonIcon = Icons.Default.Refresh,
                                            onButtonClick = { viewModel.performSearch() }
                                        )
                                    }
                                    is SearchUiState.Error -> {
                                        StateDisplayCard(
                                            icon = Icons.Default.Info,
                                            badgeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            iconTint = Color(0xFF64748B),
                                            title = "Chargement impossible",
                                            titleColor = MaterialTheme.colorScheme.onSurface,
                                            description = "Vérifiez vos critères et réessayez",
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

    // Dialogue d'alerte dès la fin d'un téléchargement pour proposer d'étudier immédiatement avec l'IA
    if (newlyDownloadedNoticeDoc != null) {
        val doc = newlyDownloadedNoticeDoc!!
        AlertDialog(
            onDismissRequest = { newlyDownloadedNoticeDoc = null },
            icon = {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Téléchargement terminé !",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = doc.formattedTitle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Le document est enregistré hors-ligne et prêt pour vos révisions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = doc
                        newlyDownloadedNoticeDoc = null
                        selectedDocForAiOptions = target
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Étudier avec IA")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            val target = doc
                            newlyDownloadedNoticeDoc = null
                            viewModel.openLocalFile(context, target)
                        }
                    ) {
                        Text("Lire")
                    }
                    TextButton(
                        onClick = { newlyDownloadedNoticeDoc = null }
                    ) {
                        Text("Fermer")
                    }
                }
            }
        )
    }

    // Composant de sélection d'IA pour étudier avec l'IA
    // Listing : Gemini, Claude AI, ChatGPT, DeepSeek
    // Sur chaque composant d'application : le nom de l'application uniquement
    if (selectedDocForAiOptions != null) {
        val doc = selectedDocForAiOptions!!
        AlertDialog(
            onDismissRequest = { selectedDocForAiOptions = null },
            icon = {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Étudier avec l'IA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = doc.formattedTitle,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${doc.typeLabel} • ${doc.sizeFormatted}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Applications d'IA : Claude AI, ChatGPT, DeepSeek, Gemini
                    // Ecrire sur chaque composant d'application le nom de l'application uniquement
                    val aiApps = com.example.utils.GeminiShareHelper.AI_APPS

                    aiApps.forEach { aiApp ->
                        Surface(
                            onClick = {
                                val target = doc
                                val targetApp = aiApp
                                selectedDocForAiOptions = null
                                com.example.utils.GeminiShareHelper.shareDocumentToAiApp(
                                    context = context,
                                    file = target.file,
                                    app = targetApp,
                                    onNotInstalled = {
                                        missingAiApp = targetApp
                                    }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = aiApp.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedDocForAiOptions = null }) {
                    Text("Fermer")
                }
            }
        )
    }

    if (missingAiApp != null) {
        val targetApp = missingAiApp!!
        AlertDialog(
            onDismissRequest = { missingAiApp = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "${targetApp.name} non installée",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "L'application ${targetApp.name} n'a pas été détectée sur votre appareil.\n\nVous pouvez l'installer depuis le Google Play Store ou partager votre document vers une autre application disponible.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val app = targetApp
                        missingAiApp = null
                        com.example.utils.GeminiShareHelper.promptInstallApp(context, app)
                    }
                ) {
                    Text("Installer ${targetApp.name}")
                }
            },
            dismissButton = {
                TextButton(onClick = { missingAiApp = null }) {
                    Text("Fermer")
                }
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
    onStudyCoursWithAi: (Cours) -> Unit = {},
    onStudySujetWithAi: (Sujet, Boolean) -> Unit = { _, _ -> },
    onShareCours: (Cours) -> Unit = {},
    onShareSujet: (Sujet, Boolean) -> Unit = { _, _ -> },
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

        itemsIndexed(ues, key = { index, ue -> "ue_card_${ue.id}_$index" }) { _, ue ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                UeUnifiedDocumentCard(
                    ue = ue,
                    downloadedDocs = downloadedDocs,
                    activeDownloads = activeDownloads,
                    onDownloadCours = onDownloadCours,
                    onDownloadSujet = onDownloadSujet,
                    onOpenCours = onOpenCours,
                    onOpenSujet = onOpenSujet,
                    onStudyCoursWithAi = onStudyCoursWithAi,
                    onStudySujetWithAi = onStudySujetWithAi,
                    onShareCours = onShareCours,
                    onShareSujet = onShareSujet,
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
    onStudyCoursWithAi: (Cours) -> Unit = {},
    onStudySujetWithAi: (Sujet, Boolean) -> Unit = { _, _ -> },
    onShareCours: (Cours) -> Unit = {},
    onShareSujet: (Sujet, Boolean) -> Unit = { _, _ -> },
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
                onStudyWithAi = { onStudyCoursWithAi(cours) },
                onShare = { onShareCours(cours) },
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
                onStudySujetWithAi = { onStudySujetWithAi(sujet, false) },
                onStudyCorrigeWithAi = { onStudySujetWithAi(sujet, true) },
                onShareSujet = { onShareSujet(sujet, false) },
                onShareCorrige = { onShareSujet(sujet, true) },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoursItemCard(
    ue: UeWithDocuments,
    cours: Cours,
    isDownloaded: Boolean,
    downloadStatus: DownloadStatus,
    onDownload: () -> Unit,
    onOpen: () -> Unit,
    onStudyWithAi: () -> Unit = {},
    onShare: () -> Unit = {},
    onDelete: () -> Unit
) {
    val formattedCode = remember(ue.code) { formatCodeUe(ue.code) }
    val anneEnCours = remember(cours) {
        cours.anneAcademique ?: "2023-2024"
    }
    val semestreEnCours = remember(ue) {
        if (ue.semestre.isNullOrBlank() || ue.semestre == "Semestre") "Semestre 1" else ue.semestre
    }
    val filierePalette = remember(cours.filiereCode, cours.filiereNom, ue.filiereCode, ue.filiereNom, ue.code) {
        FiliereColorProvider.getPalette(
            filiereCode = cours.filiereCode ?: ue.filiereCode,
            filiereNom = cours.filiereNom ?: ue.filiereNom,
            fallbackText = ue.code
        )
    }
    val filiereLabel = remember(cours.filiereCode, cours.filiereNom, ue.filiereCode, ue.filiereNom, ue.code) {
        FiliereColorProvider.getDisplayLabel(
            filiereCode = cours.filiereCode ?: ue.filiereCode,
            filiereNom = cours.filiereNom ?: ue.filiereNom,
            fallbackUeCode = ue.code
        )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left color stripe for the filière
            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .fillMaxHeight()
                    .background(filierePalette.accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Header: Code UE badge + Filière color badge + Download status badge + Type badge with Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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

                        // Filière Color Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = filierePalette.containerColor,
                            border = BorderStroke(1.dp, filierePalette.borderColor)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(filierePalette.accentColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = filiereLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = filierePalette.contentColor
                                )
                            }
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

                    Spacer(modifier = Modifier.width(6.dp))

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

                // Badges flow row: Année, Semestre, Niveau, Taille, Téléchargements
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        DownloadProgressItem(
                            downloadStatus = downloadStatus,
                            accentColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            label = "Téléchargement du cours..."
                        )
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

                // Interactive action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (downloadStatus is DownloadStatus.Progress) {
                        FilledTonalButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Téléchargement...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (isDownloaded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onOpen,
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Lire",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = onStudyWithAi,
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Étudier avec IA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onShare,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("share_cours_${cours.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Partager cours",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("delete_cours_${cours.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Télécharger",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    onStudySujetWithAi: () -> Unit = {},
    onStudyCorrigeWithAi: () -> Unit = {},
    onShareSujet: () -> Unit = {},
    onShareCorrige: () -> Unit = {},
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
    val filierePalette = remember(sujet.filiereCode, sujet.filiereNom, ue.filiereCode, ue.filiereNom, ue.code) {
        FiliereColorProvider.getPalette(
            filiereCode = sujet.filiereCode ?: ue.filiereCode,
            filiereNom = sujet.filiereNom ?: ue.filiereNom,
            fallbackText = ue.code
        )
    }
    val filiereLabel = remember(sujet.filiereCode, sujet.filiereNom, ue.filiereCode, ue.filiereNom, ue.code) {
        FiliereColorProvider.getDisplayLabel(
            filiereCode = sujet.filiereCode ?: ue.filiereCode,
            filiereNom = sujet.filiereNom ?: ue.filiereNom,
            fallbackUeCode = ue.code
        )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left color stripe for the filière
            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .fillMaxHeight()
                    .background(filierePalette.accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Header: Code UE badge + Filière color badge + Download status badge + Type badge with Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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

                        // Filière Color Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = filierePalette.containerColor,
                            border = BorderStroke(1.dp, filierePalette.borderColor)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(filierePalette.accentColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = filiereLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = filierePalette.contentColor
                                )
                            }
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

                    Spacer(modifier = Modifier.width(6.dp))

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

            // Badges flow row: Année, Semestre, Session, Téléchargements
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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

            // Progress sujet with animated DownloadProgressItem
            AnimatedVisibility(
                visible = sujetDownloadStatus is DownloadStatus.Progress,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (sujetDownloadStatus is DownloadStatus.Progress) {
                    DownloadProgressItem(
                        downloadStatus = sujetDownloadStatus,
                        accentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        label = "Téléchargement de l'épreuve..."
                    )
                }
            }

            if (sujetDownloadStatus is DownloadStatus.Error) {
                Text(
                    text = sujetDownloadStatus.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Interactive action buttons for Sujet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sujetDownloadStatus is DownloadStatus.Progress) {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Téléchargement...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else if (isSujetDownloaded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onOpenSujet,
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lire",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = onStudySujetWithAi,
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Étudier avec IA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onShareSujet,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("share_sujet_file_${sujet.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Partager sujet",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteSujet,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("delete_sujet_file_${sujet.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Supprimer sujet",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onDownloadSujet,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Télécharger l'épreuve",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Option 2: THE ANSWER SHEET (CORRIGÉ) if available
            if (sujet.avecCorrige == true) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                    if (isCorrigeDownloaded) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "Téléchargé ✅",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Progress corrigé
                AnimatedVisibility(
                    visible = corrigeDownloadStatus is DownloadStatus.Progress,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (corrigeDownloadStatus is DownloadStatus.Progress) {
                        DownloadProgressItem(
                            downloadStatus = corrigeDownloadStatus,
                            accentColor = Color(0xFFD84315),
                            containerColor = Color(0xFFFBE9E7),
                            label = "Téléchargement du corrigé..."
                        )
                    }
                }

                if (corrigeDownloadStatus is DownloadStatus.Error) {
                    Text(
                        text = corrigeDownloadStatus.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Corrigé action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (corrigeDownloadStatus is DownloadStatus.Progress) {
                        FilledTonalButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFD84315)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Téléchargement...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (isCorrigeDownloaded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onOpenCorrige,
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD84315),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lire", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onStudyCorrigeWithAi,
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Étudier avec IA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onShareCorrige,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("share_corrige_file_${sujet.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Partager corrigé",
                                    tint = Color(0xFFD84315),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onDeleteCorrige,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("delete_corrige_file_${sujet.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Supprimer corrigé",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDownloadCorrige,
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFD84315)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFD84315)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Télécharger corrigé", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
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
        shape = RoundedCornerShape(12.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadedFilesSection(
    localFiles: List<com.example.data.repository.LocalDownloadedDocument>,
    onOpenFile: (com.example.data.repository.LocalDownloadedDocument) -> Unit,
    onShareFile: (com.example.data.repository.LocalDownloadedDocument) -> Unit = {},
    onDeleteFile: (com.example.data.repository.LocalDownloadedDocument) -> Unit,
    onStudyWithAi: (com.example.data.repository.LocalDownloadedDocument) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Tous") }
    var isFilterExpanded by remember { mutableStateOf(false) }

    val types = listOf("Tous", "Cours", "Sujets", "Corrigés")

    val filteredFiles = remember(localFiles, searchQuery, selectedType) {
        localFiles.filter { doc ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                doc.formattedTitle.contains(searchQuery, ignoreCase = true) ||
                doc.name.contains(searchQuery, ignoreCase = true) ||
                doc.typeLabel.contains(searchQuery, ignoreCase = true) ||
                doc.niveau.contains(searchQuery, ignoreCase = true)
            }
            val matchesType = if (selectedType == "Tous") true else {
                when (selectedType) {
                    "Cours" -> doc.typeCategory == "Cours" || doc.typeLabel.contains("Cours", ignoreCase = true)
                    "Sujets" -> doc.typeCategory == "Sujets" || doc.typeLabel.contains("Sujet", ignoreCase = true) || doc.typeLabel.contains("Épreuve", ignoreCase = true)
                    "Corrigés" -> doc.typeCategory == "Corrigés" || doc.typeLabel.contains("Corrigé", ignoreCase = true)
                    else -> true
                }
            }
            matchesSearch && matchesType
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (localFiles.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aucun document téléchargé",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Téléchargez des cours magistraux, TD ou épreuves depuis le Catalogue ou la Recherche pour pouvoir les consulter ici et les étudier avec l'IA.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Barre de recherche textuelle avec bouton de paramètres déroulables
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher dans les téléchargements...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { isFilterExpanded = !isFilterExpanded },
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer", modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(
                            onClick = { isFilterExpanded = !isFilterExpanded },
                            modifier = Modifier.size(34.dp)
                        ) {
                            val hasActiveFilter = selectedType != "Tous"
                            BadgedBox(
                                badge = {
                                    if (hasActiveFilter) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(6.dp)
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFilterExpanded) Icons.Default.Tune else Icons.Default.FilterList,
                                    contentDescription = "Paramètres de filtrage",
                                    tint = if (isFilterExpanded || hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Paramètres déroulables (Type de document uniquement, sans classement par niveau)
            AnimatedVisibility(
                visible = isFilterExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Classement par Type de document (Cours, Sujets, Corrigés)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Type de document :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                types.forEach { type ->
                                    val isSelected = selectedType == type
                                    val count = if (type == "Tous") localFiles.size else localFiles.count {
                                        when (type) {
                                            "Cours" -> it.typeCategory == "Cours" || it.typeLabel.contains("Cours", ignoreCase = true)
                                            "Sujets" -> it.typeCategory == "Sujets" || it.typeLabel.contains("Sujet", ignoreCase = true) || it.typeLabel.contains("Épreuve", ignoreCase = true)
                                            "Corrigés" -> it.typeCategory == "Corrigés" || it.typeLabel.contains("Corrigé", ignoreCase = true)
                                            else -> true
                                        }
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedType = type },
                                        label = {
                                            Text(
                                                text = if (type == "Tous") "Tous les types ($count)" else "$type ($count)",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            val icon = when (type) {
                                                "Cours" -> Icons.Default.MenuBook
                                                "Sujets" -> Icons.Default.Assignment
                                                "Corrigés" -> Icons.Default.TaskAlt
                                                else -> Icons.Default.Layers
                                            }
                                            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp))
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredFiles.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aucun document ne correspond aux filtres",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Type : $selectedType",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                selectedType = "Tous"
                                searchQuery = ""
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Réinitialiser les filtres", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredFiles, key = { index, doc -> "${doc.name}_$index" }) { _, doc ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth()
                            ) {
                                // En-tête : Icône, Titre, Badges Niveau et Type
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        color = when {
                                            doc.typeLabel.contains("Sujet", ignoreCase = true) -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                            doc.typeLabel.contains("Corrigé", ignoreCase = true) -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.formattedTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Badges de Niveau et de Type
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Badge Niveau
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Niveau ${doc.niveau}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Badge Type
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = doc.typeLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = doc.sizeFormatted,
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Divider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    thickness = 0.5.dp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Boutons d'actions : IA Gemini, Lire, Partager, Supprimer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Bouton IA avec document importé
                                    Button(
                                        onClick = { onStudyWithAi(doc) },
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Étudier avec IA",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Bouton Lire
                                        Button(
                                            onClick = { onOpenFile(doc) },
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lire", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Partager
                                        IconButton(
                                            onClick = { onShareFile(doc) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("share_local_file_${doc.name}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Partager",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Supprimer
                                        IconButton(
                                            onClick = { onDeleteFile(doc) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Supprimer",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(17.dp)
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
                            text = { Text("Aperçu & PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = selectedSection == 3,
                            onClick = { selectedSection = 3 },
                            text = { Text("Hors-ligne", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Section Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 340.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedSection) {
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TutorialStepItem(
                                    number = "1",
                                    title = "Recherche par code d'UE ou matière",
                                    description = "Tapez le code officiel du cours (ex: INF101, MAT202, BIO111) ou le titre d'une matière pour trouver immédiatement les ressources."
                                )
                                TutorialStepItem(
                                    number = "2",
                                    title = "Validation et affichage instantané",
                                    description = "La liste des cours, épreuves et corrigés correspondants s'affiche avec leurs métadonnées complètes."
                                )
                                TutorialStepItem(
                                    number = "3",
                                    title = "Partage direct",
                                    description = "Partagez facilement un document ou corrigé avec vos camarades via le bouton de partage dédié."
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
                                    description = "Touchez le bouton « Filtres » sous la barre de recherche pour déplier tous les critères disponibles."
                                )
                                TutorialStepItem(
                                    number = "2",
                                    title = "Établissement & Filière",
                                    description = "Sélectionnez votre Faculté ou École (ex: IUT, FALSH, FS, FSEG, FSJP) puis votre Filière spécifique."
                                )
                                TutorialStepItem(
                                    number = "3",
                                    title = "Niveaux d'étude ultra-rapides",
                                    description = "Sélectionnez directement votre niveau (L1, L2, L3, M1, M2) et choisissez le format souhaité (Cours, Épreuves, Corrigés, TD, TP)."
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
                                    title = "Aperçu visuel des pages PDF",
                                    description = "Cliquez sur « Aperçu » sur n'importe quel document ou effectuez un appui long dans vos téléchargements pour ouvrir le visualiseur."
                                )
                                TutorialStepItem(
                                    number = "2",
                                    title = "Feuilleter les pages du document",
                                    description = "Parcourez directement les pages avec les boutons précédent/suivant sans avoir à quitter l'application."
                                )
                                TutorialStepItem(
                                    number = "3",
                                    title = "Partager depuis l'aperçu",
                                    description = "Un bouton de partage rapide vous permet d'envoyer le document en cours de visualisation vers WhatsApp, Email ou Bluetooth."
                                )
                            }
                        }
                        3 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TutorialStepItem(
                                    number = "1",
                                    title = "Téléchargement en un clic",
                                    description = "Appuyez sur le bouton vert « Télécharger » à droite de n'importe quel cours ou épreuve."
                                )
                                TutorialStepItem(
                                    number = "2",
                                    title = "Accès hors-ligne direct",
                                    description = "Basculez sur l'onglet « Téléchargés » dans le menu du bas pour consulter tous vos fichiers enregistrés sans connexion."
                                )
                                TutorialStepItem(
                                    number = "3",
                                    title = "Ouvrir, partager et gérer le stockage",
                                    description = "Ouvrez instantanément vos fichiers avec le bouton compact « Ouvrir », partagez-les en un clic ou supprimez les anciens pour libérer de la mémoire."
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

@Composable
fun DownloadProgressItem(
    downloadStatus: DownloadStatus.Progress,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
    label: String = "Téléchargement en cours..."
) {
    val speedText = remember(downloadStatus.speedKBps) { formatSpeed(downloadStatus.speedKBps) }
    val sizeText = remember(downloadStatus.downloadedBytes, downloadStatus.totalBytes) {
        formatProgressDetail(downloadStatus.downloadedBytes, downloadStatus.totalBytes)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (downloadStatus.progress > 0) downloadStatus.progress / 100f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "download_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(color = containerColor, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.5.dp,
                    color = accentColor
                )
                Text(
                    text = if (sizeText.isNotBlank()) "$label ($sizeText)" else label,
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (downloadStatus.speedKBps > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = speedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${downloadStatus.progress}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (downloadStatus.progress > 0) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        } else {
            LinearProgressIndicator(
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}
