package com.example.ui.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.data.preferences.StudentProfile
import com.example.data.repository.DownloadStatus
import com.example.ui.components.SearchSkeleton
import com.example.ui.search.components.CoursItemCard
import com.example.ui.search.components.SujetItemCard
import com.example.ui.theme.PlusJakartaSansFontFamily

/**
 * Page de recherche académique de CAMPUS EDUCATIF.
 * Conçue pour une fluidité de frappe 100% stable :
 *  - Un seul composant de barre de recherche dans l'arbre Compose pour ne JAMAIS couper à la première lettre.
 *  - Transition douce entre l'état centré et les résultats.
 *  - Espace de 26dp sous la barre de recherche.
 *  - Logo du chapeau académique blanc et titre en majuscule sans sous-texte.
 *  - Téléchargement au clic et classement par pertinence.
 */
@Composable
fun SearchTabContent(
    viewModel: SearchViewModel,
    profile: StudentProfile,
    onEditProfile: () -> Unit,
    onDownloadCours: (Cours, UeWithDocuments) -> Unit,
    onDownloadSujet: (Sujet, Boolean, UeWithDocuments) -> Unit,
    onStudyCoursWithAi: (Cours, UeWithDocuments) -> Unit,
    onStudySujetWithAi: (Sujet, Boolean, UeWithDocuments) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val resultCount by viewModel.resultCount.collectAsState()
    val pagination by viewModel.pagination.collectAsState()
    val downloadedDocs by viewModel.downloadedDocuments.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val isOfflineSearch by viewModel.isOfflineSearch.collectAsState()
    val isUpdateRequired by com.example.data.network.VersionUpdateManager.isUpdateRequired.collectAsState()

    var reportingSujet by remember { mutableStateOf<Sujet?>(null) }
    var showMissingSujetDialog by remember { mutableStateOf(false) }

    // État local stable indépendant pour éviter toute désynchronisation du clavier
    var localQuery by remember { mutableStateOf(searchQuery) }
    // Mode d'affichage des cours et sujets : grille (2 colonnes) ou liste (1 colonne)
    var isGridView by remember { mutableStateOf(true) }

    // Synchronisation uniquement si réinitialisé extérieurement
    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty() && localQuery.isNotEmpty()) {
            localQuery = ""
        }
    }

    val isSearchingOrActive = localQuery.isNotBlank() || uiState !is SearchUiState.Idle

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isSearchingOrActive) {
            // ÉTAT REPOS : Espace pour centrer
            Spacer(modifier = Modifier.weight(0.5f))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primaryContainer),
                shadowElevation = 6.dp,
                modifier = Modifier.size(86.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_academic_cap_white),
                    contentDescription = "Logo Campus Educatif",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CAMPUS EDUCATIF",
                fontSize = 28.sp,
                fontFamily = com.example.ui.theme.AntonFontFamily,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))
        } else {
            // ÉTAT CHARGEMENT / RECHERCHE ACTIVE : Positionnement en haut à exactement 5px du top
            Spacer(modifier = Modifier.height(5.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(34.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_academic_cap_white),
                        contentDescription = "Logo Campus Educatif",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "CAMPUS EDUCATIF",
                    fontSize = 20.sp,
                    fontFamily = com.example.ui.theme.AntonFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )

                // Accès direct et informations de la filière et du niveau (icône Edit)
                Surface(
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("top_profile_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier filière et niveau",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${profile.filiereCode ?: profile.filiereNom ?: "Filière"} • ${profile.niveau}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // ==============================================================
        // BARRE DE RECHERCHE UNIQUE ET PERSISTANTE
        // Ne jamais recréer ce composant lors de la frappe
        // ==============================================================
        RectangularSearchBar(
            query = localQuery,
            onQueryChange = { newValue ->
                localQuery = newValue
                viewModel.updateSearchQuery(newValue)
            },
            onClear = {
                localQuery = ""
                viewModel.updateSearchQuery("")
            },
            onSearch = { focusManager.clearFocus() },
            isGridView = isGridView,
            onToggleViewMode = { isGridView = !isGridView },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
        )

        Spacer(modifier = Modifier.height(if (isSearchingOrActive) 6.dp else 10.dp))

        if (!isSearchingOrActive) {
            // Informations complètes et modification du profil d'études (Filière, Niveau, École)
            Surface(
                onClick = onEditProfile,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("edit_profile_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = (profile.filiereCode ?: "FILIÈRE").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Filière : ${profile.filiereNom ?: profile.filiereCode ?: "Non configurée"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Niveau : ${profile.niveau}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!profile.ecoleNom.isNullOrBlank()) {
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = profile.ecoleNom,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Modifier",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.9f))
        } else {
            // Zone de résultats et chargement
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is SearchUiState.Loading -> {
                        SearchSkeleton()
                    }

                    is SearchUiState.NetworkError -> {
                        SearchErrorBox(
                            message = state.message,
                            onRetry = { viewModel.retrySearch() }
                        )
                    }

                    is SearchUiState.ServerError -> {
                        SearchErrorBox(
                            message = "Erreur serveur (${state.code ?: "inconnue"}) : ${state.message}",
                            onRetry = { viewModel.retrySearch() }
                        )
                    }

                    is SearchUiState.Error -> {
                        SearchErrorBox(
                            message = state.message,
                            onRetry = { viewModel.retrySearch() }
                        )
                    }

                    is SearchUiState.Empty -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "Aucun document trouvé",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { showMissingSujetDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Il manque un sujet ?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is SearchUiState.Success -> {
                        val rawUes = state.results

                        // Filtrage strict si "Uniquement mes cours" est coché, sinon classement par pertinence
                        val sortedUes = remember(rawUes, profile) {
                            if (profile.onlyMyCourses && profile.isConfigured) {
                                rawUes.mapNotNull { ue ->
                                    val matchFiliere = !profile.filiereCode.isNullOrBlank() &&
                                        (ue.filiereCode?.equals(profile.filiereCode, ignoreCase = true) == true ||
                                         ue.filiereNom?.contains(profile.filiereNom ?: "", ignoreCase = true) == true ||
                                         (profile.filiereNom != null && ue.filiereNom?.equals(profile.filiereNom, ignoreCase = true) == true))
                                    val matchNiveau = !profile.niveau.isBlank() &&
                                        ue.niveau?.equals(profile.niveau, ignoreCase = true) == true

                                    if (matchFiliere && matchNiveau && !ue.cours.isNullOrEmpty()) {
                                        // Uniquement les cours de cette filière et ce niveau (sujets vidés)
                                        ue.copy(sujets = emptyList())
                                    } else {
                                        null
                                    }
                                }
                            } else {
                                rawUes.sortedWith(
                                    compareByDescending<UeWithDocuments> { ue ->
                                        val matchFiliere = !profile.filiereCode.isNullOrBlank() &&
                                            (ue.filiereCode?.equals(profile.filiereCode, ignoreCase = true) == true ||
                                             ue.filiereNom?.contains(profile.filiereNom ?: "", ignoreCase = true) == true)
                                        val matchNiveau = !profile.niveau.isBlank() &&
                                            ue.niveau?.equals(profile.niveau, ignoreCase = true) == true

                                        when {
                                            matchFiliere && matchNiveau -> 3
                                            matchFiliere -> 2
                                            !profile.ecoleNom.isNullOrBlank() && ue.ecoleNom?.contains(profile.ecoleNom, ignoreCase = true) == true -> 1
                                            else -> 0
                                        }
                                    }
                                )
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            if (isOfflineSearch) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Connexion internet indisponible",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Gray
                                    )
                                }
                            }

                            if (sortedUes.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Aucun résultat pour cette recherche.",
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedButton(
                                            onClick = { showMissingSujetDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Il manque un sujet ?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(sortedUes, key = { it.id }) { ue ->
                                        SearchUeCard(
                                            ue = ue,
                                            isGridView = isGridView,
                                            downloadedDocs = downloadedDocs,
                                            activeDownloads = activeDownloads,
                                            onDownloadCours = { c -> onDownloadCours(c, ue) },
                                            onOpenCours = { c -> viewModel.openDownloadedCours(context, c) },
                                            onShareCours = { c -> viewModel.shareDownloadedCours(context, c) },
                                            onDeleteCours = { c -> viewModel.deleteDownloadedCours(context, c) },
                                            onStudyCoursWithAi = { c -> onStudyCoursWithAi(c, ue) },
                                            onDownloadSujet = { s, isCorrige -> onDownloadSujet(s, isCorrige, ue) },
                                            onOpenSujet = { s, isCorrige -> viewModel.openDownloadedSujet(context, s, isCorrige) },
                                            onShareSujet = { s, isCorrige -> viewModel.shareDownloadedSujet(context, s, isCorrige) },
                                            onDeleteSujet = { s, isCorrige -> viewModel.deleteDownloadedSujet(context, s, isCorrige) },
                                            onStudySujetWithAi = { s, isCorrige -> onStudySujetWithAi(s, isCorrige, ue) },
                                            onReportSujet = { s -> reportingSujet = s }
                                        )
                                    }

                                    item {
                                        pagination?.let { pag ->
                                            if (pag.hasNext) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { viewModel.loadMore() },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            contentColor = MaterialTheme.colorScheme.primary
                                                        ),
                                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                                    ) {
                                                        Text("Charger plus de documents", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is SearchUiState.Idle -> {
                        // État neutre
                    }
                }
            }
        }
    }

    if (isUpdateRequired) {
        ForceUpdateDialog(
            onUpdateClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                    context.startActivity(intent)
                }
            }
        )
    }

    reportingSujet?.let { sujet ->
        SignalementDialog(
            sujet = sujet,
            onDismiss = { reportingSujet = null },
            onSubmit = { msg, contact ->
                viewModel.submitDemande(
                    type = "signalement",
                    sujetId = sujet.id,
                    message = msg,
                    contact = contact
                ) { success, note ->
                    reportingSujet = null
                    Toast.makeText(context, note, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showMissingSujetDialog) {
        DemandeMissingSujetDialog(
            onDismiss = { showMissingSujetDialog = false },
            onSubmit = { msg, contact ->
                viewModel.submitDemande(
                    type = "sujet_manquant",
                    ueId = profile.filiereId ?: 1,
                    message = msg,
                    contact = contact
                ) { success, note ->
                    showMissingSujetDialog = false
                    Toast.makeText(context, note, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun RectangularSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    isGridView: Boolean = true,
    onToggleViewMode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Rechercher un cours, épreuve, UE...",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Recherche",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Effacer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    onToggleViewMode?.let { toggle ->
                        IconButton(
                            onClick = toggle,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("view_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (isGridView) "Afficher en liste" else "Afficher en grille",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input")
        )
    }
}

@Composable
private fun SearchUeCard(
    ue: UeWithDocuments,
    isGridView: Boolean = true,
    downloadedDocs: Set<String>,
    activeDownloads: Map<String, DownloadStatus>,
    onDownloadCours: (Cours) -> Unit,
    onOpenCours: (Cours) -> Unit,
    onShareCours: (Cours) -> Unit,
    onDeleteCours: (Cours) -> Unit,
    onStudyCoursWithAi: (Cours) -> Unit,
    onDownloadSujet: (Sujet, Boolean) -> Unit,
    onOpenSujet: (Sujet, Boolean) -> Unit,
    onShareSujet: (Sujet, Boolean) -> Unit,
    onDeleteSujet: (Sujet, Boolean) -> Unit,
    onStudySujetWithAi: (Sujet, Boolean) -> Unit,
    onReportSujet: (Sujet) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // En-tête UE moderne et lisible
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = ue.code.uppercase(),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                )
            }

            Text(
                text = ue.intitule,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        // Cours de l'UE en grille 2 par ligne ou en liste pleine largeur
        val coursList = ue.cours.orEmpty()
        if (coursList.isNotEmpty()) {
            if (isGridView) {
                coursList.chunked(2).forEach { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val c1 = rowPair[0]
                        val c1Key = "cours_${c1.id}"
                        CoursItemCard(
                            cours = c1,
                            ue = ue,
                            isDownloaded = downloadedDocs.contains(c1Key),
                            downloadStatus = activeDownloads[c1Key] ?: DownloadStatus.Idle,
                            onDownload = { onDownloadCours(c1) },
                            onOpen = { onOpenCours(c1) },
                            onStudyWithAi = { onStudyCoursWithAi(c1) },
                            onShare = { onShareCours(c1) },
                            onDelete = { onDeleteCours(c1) },
                            modifier = Modifier.weight(1f)
                        )

                        if (rowPair.size > 1) {
                            val c2 = rowPair[1]
                            val c2Key = "cours_${c2.id}"
                            CoursItemCard(
                                cours = c2,
                                ue = ue,
                                isDownloaded = downloadedDocs.contains(c2Key),
                                downloadStatus = activeDownloads[c2Key] ?: DownloadStatus.Idle,
                                onDownload = { onDownloadCours(c2) },
                                onOpen = { onOpenCours(c2) },
                                onStudyWithAi = { onStudyCoursWithAi(c2) },
                                onShare = { onShareCours(c2) },
                                onDelete = { onDeleteCours(c2) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                coursList.forEach { c ->
                    val cKey = "cours_${c.id}"
                    CoursItemCard(
                        cours = c,
                        ue = ue,
                        isDownloaded = downloadedDocs.contains(cKey),
                        downloadStatus = activeDownloads[cKey] ?: DownloadStatus.Idle,
                        onDownload = { onDownloadCours(c) },
                        onOpen = { onOpenCours(c) },
                        onStudyWithAi = { onStudyCoursWithAi(c) },
                        onShare = { onShareCours(c) },
                        onDelete = { onDeleteCours(c) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Sujets d'examen en grille 2 par ligne ou en liste pleine largeur
        val sujetList = ue.sujets.orEmpty()
        if (sujetList.isNotEmpty()) {
            if (isGridView) {
                sujetList.chunked(2).forEach { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val s1 = rowPair[0]
                        val s1Key = "sujet_${s1.id}"
                        val corr1Key = "corrige_${s1.id}"
                        SujetItemCard(
                            sujet = s1,
                            ue = ue,
                            isSujetDownloaded = downloadedDocs.contains(s1Key),
                            isCorrigeDownloaded = downloadedDocs.contains(corr1Key),
                            sujetDownloadStatus = activeDownloads[s1Key] ?: DownloadStatus.Idle,
                            corrigeDownloadStatus = activeDownloads[corr1Key] ?: DownloadStatus.Idle,
                            onDownloadSujet = { onDownloadSujet(s1, false) },
                            onOpenSujet = { onOpenSujet(s1, false) },
                            onStudySujetWithAi = { onStudySujetWithAi(s1, false) },
                            onShareSujet = { onShareSujet(s1, false) },
                            onDeleteSujet = { onDeleteSujet(s1, false) },
                            onDownloadCorrige = { onDownloadSujet(s1, true) },
                            onOpenCorrige = { onOpenSujet(s1, true) },
                            onStudyCorrigeWithAi = { onStudySujetWithAi(s1, true) },
                            onShareCorrige = { onShareSujet(s1, true) },
                            onDeleteCorrige = { onDeleteSujet(s1, true) },
                            onReport = { onReportSujet(s1) },
                            modifier = Modifier.weight(1f)
                        )

                        if (rowPair.size > 1) {
                            val s2 = rowPair[1]
                            val s2Key = "sujet_${s2.id}"
                            val corr2Key = "corrige_${s2.id}"
                            SujetItemCard(
                                sujet = s2,
                                ue = ue,
                                isSujetDownloaded = downloadedDocs.contains(s2Key),
                                isCorrigeDownloaded = downloadedDocs.contains(corr2Key),
                                sujetDownloadStatus = activeDownloads[s2Key] ?: DownloadStatus.Idle,
                                corrigeDownloadStatus = activeDownloads[corr2Key] ?: DownloadStatus.Idle,
                                onDownloadSujet = { onDownloadSujet(s2, false) },
                                onOpenSujet = { onOpenSujet(s2, false) },
                                onStudySujetWithAi = { onStudySujetWithAi(s2, false) },
                                onShareSujet = { onShareSujet(s2, false) },
                                onDeleteSujet = { onDeleteSujet(s2, false) },
                                onDownloadCorrige = { onDownloadSujet(s2, true) },
                                onOpenCorrige = { onOpenSujet(s2, true) },
                                onStudyCorrigeWithAi = { onStudySujetWithAi(s2, true) },
                                onShareCorrige = { onShareSujet(s2, true) },
                                onDeleteCorrige = { onDeleteSujet(s2, true) },
                                onReport = { onReportSujet(s2) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                sujetList.forEach { s ->
                    val sKey = "sujet_${s.id}"
                    val corrKey = "corrige_${s.id}"
                    SujetItemCard(
                        sujet = s,
                        ue = ue,
                        isSujetDownloaded = downloadedDocs.contains(sKey),
                        isCorrigeDownloaded = downloadedDocs.contains(corrKey),
                        sujetDownloadStatus = activeDownloads[sKey] ?: DownloadStatus.Idle,
                        corrigeDownloadStatus = activeDownloads[corrKey] ?: DownloadStatus.Idle,
                        onDownloadSujet = { onDownloadSujet(s, false) },
                        onOpenSujet = { onOpenSujet(s, false) },
                        onStudySujetWithAi = { onStudySujetWithAi(s, false) },
                        onShareSujet = { onShareSujet(s, false) },
                        onDeleteSujet = { onDeleteSujet(s, false) },
                        onDownloadCorrige = { onDownloadSujet(s, true) },
                        onOpenCorrige = { onOpenSujet(s, true) },
                        onStudyCorrigeWithAi = { onStudySujetWithAi(s, true) },
                        onShareCorrige = { onShareSujet(s, true) },
                        onDeleteCorrige = { onDeleteSujet(s, true) },
                        onReport = { onReportSujet(s) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchErrorBox(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Gray
            )
            Text(
                text = "Connexion internet indisponible",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SignalementDialog(
    sujet: Sujet,
    onDismiss: () -> Unit,
    onSubmit: (String, String?) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Signaler un problème", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Épreuve : ${sujet.titre}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it; errorMessage = null },
                    label = { Text("Description du problème *") },
                    placeholder = { Text("Ex: Page manquante, mauvaise session... (10 à 1000 car.)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact (facultatif)") },
                    placeholder = { Text("Email ou téléphone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.trim().length < 10) {
                        errorMessage = "Le message doit comporter au moins 10 caractères."
                    } else if (message.trim().length > 1000) {
                        errorMessage = "Le message ne doit pas dépasser 1000 caractères."
                    } else {
                        isSubmitting = true
                        onSubmit(message.trim(), contact.trim().takeIf { it.isNotBlank() })
                    }
                },
                enabled = !isSubmitting
            ) {
                Text("Envoyer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun DemandeMissingSujetDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String?) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Demande de sujet manquant", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Indiquez l'épreuve ou le cours que vous recherchez.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it; errorMessage = null },
                    label = { Text("Détails du document *") },
                    placeholder = { Text("Filière, UE, niveau, année, session... (10 à 1000 car.)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact (facultatif)") },
                    placeholder = { Text("Email ou téléphone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.trim().length < 10) {
                        errorMessage = "Le message doit comporter au moins 10 caractères."
                    } else if (message.trim().length > 1000) {
                        errorMessage = "Le message ne doit pas dépasser 1000 caractères."
                    } else {
                        isSubmitting = true
                        onSubmit(message.trim(), contact.trim().takeIf { it.isNotBlank() })
                    }
                },
                enabled = !isSubmitting
            ) {
                Text("Envoyer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun ForceUpdateDialog(
    onUpdateClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Mise à jour obligatoire bloquante */ },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mise à jour obligatoire", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Text(
                text = "Une nouvelle version de Campus Éducatif est disponible et requise pour continuer à télécharger et explorer les épreuves et cours.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = onUpdateClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Mettre à jour")
            }
        }
    )
}
