package com.example.ui.search

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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

/**
 * Page de recherche académique de CAMPUS EDUCATIF :
 *  - Logo du chapeau académique blanc.
 *  - Titre en majuscule "CAMPUS EDUCATIF" sans aucun sous-texte.
 *  - La barre de recherche reste au centre de la page au repos.
 *  - Ce n'est que lorsque l'utilisateur tape que les résultats se chargent.
 *  - Téléchargement automatique en cliquant sur les cartes.
 */
@Composable
fun SearchTabContent(
    viewModel: SearchViewModel,
    profile: StudentProfile,
    onEditProfile: () -> Unit,
    onStudyCoursWithAi: (Cours) -> Unit,
    onStudySujetWithAi: (Sujet, Boolean) -> Unit,
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

    var localQuery by remember(searchQuery) { mutableStateOf(searchQuery) }
    val isSearching = localQuery.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isSearching) {
            // Vue au repos : La barre de recherche reste au centre de la page
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo chapeau académique blanc
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(92.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_academic_cap_white),
                            contentDescription = "Logo Campus Educatif",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Titre CAMPUS EDUCATIF en MAJUSCULE sans sous-texte
                    Text(
                        text = "CAMPUS EDUCATIF",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Barre de recherche rectangulaire centrée avec bordure
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Vue active lorsque l'utilisateur commence à taper du texte
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primaryContainer),
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_academic_cap_white),
                        contentDescription = "Logo Campus Educatif",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "CAMPUS EDUCATIF",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barre de recherche rectangulaire
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
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Résultats chargés dès la saisie
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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            }
                        }
                    }

                    is SearchUiState.Success -> {
                        val rawUes = state.results

                        // Tri par pertinence (filière en premier)
                        val sortedUes = remember(rawUes, profile) {
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

                        if (sortedUes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucun résultat pour cette recherche.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(sortedUes, key = { it.id }) { ue ->
                                    SearchUeCard(
                                        ue = ue,
                                        downloadedDocs = downloadedDocs,
                                        activeDownloads = activeDownloads,
                                        onDownloadCours = { c -> viewModel.downloadCours(context, c) },
                                        onOpenCours = { c -> viewModel.openDownloadedCours(context, c) },
                                        onShareCours = { c -> viewModel.shareDownloadedCours(context, c) },
                                        onDeleteCours = { c -> viewModel.deleteDownloadedCours(context, c) },
                                        onStudyCoursWithAi = { c -> onStudyCoursWithAi(c) },
                                        onDownloadSujet = { s, isCorrige -> viewModel.downloadSujet(context, s, isCorrige) },
                                        onOpenSujet = { s, isCorrige -> viewModel.openDownloadedSujet(context, s, isCorrige) },
                                        onShareSujet = { s, isCorrige -> viewModel.shareDownloadedSujet(context, s, isCorrige) },
                                        onDeleteSujet = { s, isCorrige -> viewModel.deleteDownloadedSujet(context, s, isCorrige) },
                                        onStudySujetWithAi = { s, isCorrige -> onStudySujetWithAi(s, isCorrige) }
                                    )
                                }

                                // Pagination
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

                    is SearchUiState.Idle -> {
                        // État neutre
                    }
                }
            }
        }
    }
}

@Composable
private fun RectangularSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(6.dp)),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Rechercher un cours, épreuve, UE...",
                    fontSize = 14.sp,
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
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input")
        )
    }
}

@Composable
private fun SearchUeCard(
    ue: UeWithDocuments,
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
    onStudySujetWithAi: (Sujet, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // En-tête UE simple sans sous-titre
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Text(
                text = ue.intitule,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        // Cours de l'UE en grille 2 par ligne
        val coursList = ue.cours.orEmpty()
        if (coursList.isNotEmpty()) {
            coursList.chunked(2).forEach { rowPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        }

        // Sujets d'examen en grille 2 par ligne
        val sujetList = ue.sujets.orEmpty()
        if (sujetList.isNotEmpty()) {
            sujetList.chunked(2).forEach { rowPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Réessayer")
            }
        }
    }
}
