package com.example.ui.search

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.SavedDocumentMeta
import com.example.data.preferences.StudentProfile
import com.example.data.preferences.StudentProfileManager
import com.example.data.repository.LocalDownloadedDocument
import com.example.ui.profile.StudentOnboardingScreen
import com.example.ui.saved.EnregistresScreen
import com.example.ui.search.components.AiStudyChooserDialog
import com.example.ui.search.components.MissingAiAppDialog
import com.example.utils.AiAppTarget
import kotlinx.coroutines.delay

/**
 * Écran principal de CAMPUS EDUCATIF à 2 pages :
 *  - Onglet 0 : Recherche académique & classement par pertinence
 *  - Onglet 1 : Enregistrés (explorateur de dossiers en mosaïque : Filière -> Niveau -> UEs et Sujets TD TP)
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allLocalFiles by viewModel.allLocalFiles.collectAsState()
    val ecoles by viewModel.ecoles.collectAsState()
    val filieres by viewModel.filieres.collectAsState()

    val profileManager = remember { StudentProfileManager(context) }
    var studentProfile by remember { mutableStateOf(profileManager.getProfile()) }
    var showOnboarding by remember { mutableStateOf(!studentProfile.isConfigured) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedDocForAiOptions by remember { mutableStateOf<LocalDownloadedDocument?>(null) }
    var missingAiApp by remember { mutableStateOf<AiAppTarget?>(null) }
    var newlyDownloadedNoticeDoc by remember { mutableStateOf<LocalDownloadedDocument?>(null) }

    // Interception de retour : si sur l'onglet Enregistrés, revenir à l'onglet Recherche
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    // Réception des événements de fin de téléchargement
    LaunchedEffect(Unit) {
        viewModel.newlyDownloadedDocFlow.collect { localDoc ->
            newlyDownloadedNoticeDoc = localDoc
        }
    }

    if (showOnboarding) {
        StudentOnboardingScreen(
            ecoles = ecoles,
            filieres = filieres,
            initialProfile = studentProfile,
            onSaveProfile = { newProfile ->
                studentProfile = newProfile
                profileManager.saveProfile(newProfile)
                showOnboarding = false
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
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
                    // Page 1 : Recherche
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Recherche"
                            )
                        },
                        label = {
                            Text(
                                text = "Recherche",
                                fontSize = 12.sp,
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

                    // Page 2 : Enregistrés (classement en mosaïque)
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = "Enregistrés"
                            )
                        },
                        label = {
                            Text(
                                text = "Enregistrés",
                                fontSize = 12.sp,
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
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    SearchTabContent(
                        viewModel = viewModel,
                        profile = studentProfile,
                        onEditProfile = { showOnboarding = true },
                        onStudyCoursWithAi = { cours ->
                            val key = "cours_${cours.id}"
                            val local = allLocalFiles.find { it.originalKey == key || it.name.contains("${cours.id}") }
                            if (local != null) {
                                selectedDocForAiOptions = local
                            } else {
                                profileManager.saveDocumentMeta(
                                    SavedDocumentMeta(
                                        fileKey = key,
                                        title = cours.titre,
                                        ecoleNom = cours.ecoleNom ?: studentProfile.ecoleNom ?: "Université de Garoua",
                                        filiereNom = cours.filiereNom ?: studentProfile.filiereNom ?: "Filière Générale",
                                        niveau = studentProfile.niveau,
                                        ueCode = "UE",
                                        ueIntitule = cours.titre,
                                        isSujetOrTd = false
                                    )
                                )
                                viewModel.downloadCours(context, cours)
                                Toast.makeText(context, "Téléchargement lancé...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStudySujetWithAi = { sujet, isCorrige ->
                            val key = if (isCorrige) "corrige_${sujet.id}" else "sujet_${sujet.id}"
                            val local = allLocalFiles.find { it.originalKey == key || it.name.contains("${sujet.id}") }
                            if (local != null) {
                                selectedDocForAiOptions = local
                            } else {
                                profileManager.saveDocumentMeta(
                                    SavedDocumentMeta(
                                        fileKey = key,
                                        title = sujet.titre,
                                        ecoleNom = sujet.ecoleNom ?: studentProfile.ecoleNom ?: "Université de Garoua",
                                        filiereNom = sujet.filiereNom ?: studentProfile.filiereNom ?: "Filière Générale",
                                        niveau = studentProfile.niveau,
                                        ueCode = "UE",
                                        ueIntitule = sujet.titre,
                                        isSujetOrTd = true,
                                        isCorrige = isCorrige
                                    )
                                )
                                viewModel.downloadSujet(context, sujet, isCorrige)
                                Toast.makeText(context, "Téléchargement lancé...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                else -> {
                    EnregistresScreen(
                        viewModel = viewModel,
                        profile = studentProfile,
                        onStudyWithAi = { localDoc ->
                            selectedDocForAiOptions = localDoc
                        },
                        onNavigateToSearch = {
                            selectedTab = 0
                        }
                    )
                }
            }

            // Notification non bloquante de téléchargement terminé (ne bloque pas la navigation)
            AnimatedVisibility(
                visible = newlyDownloadedNoticeDoc != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                newlyDownloadedNoticeDoc?.let { doc ->
                    LaunchedEffect(doc) {
                        delay(6000)
                        if (newlyDownloadedNoticeDoc == doc) {
                            newlyDownloadedNoticeDoc = null
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = doc.formattedTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val target = doc
                                    newlyDownloadedNoticeDoc = null
                                    selectedDocForAiOptions = target
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ouvrir dans une IA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { newlyDownloadedNoticeDoc = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de sélection d'application d'IA externe (Claude AI, ChatGPT, DeepSeek, Gemini)
    selectedDocForAiOptions?.let { doc ->
        AiStudyChooserDialog(
            document = doc,
            onDismiss = { selectedDocForAiOptions = null },
            onAppMissing = { app ->
                selectedDocForAiOptions = null
                missingAiApp = app
            }
        )
    }

    // Modal Play Store si l'application IA sélectionnée n'est pas installée
    missingAiApp?.let { app ->
        MissingAiAppDialog(
            targetApp = app,
            onDismiss = { missingAiApp = null }
        )
    }
}
