package com.example.ui.saved

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.SavedDocumentMeta
import com.example.data.preferences.StudentProfile
import com.example.data.preferences.StudentProfileManager
import com.example.data.repository.LocalDownloadedDocument
import com.example.ui.search.SearchViewModel
import com.example.ui.search.components.FileLogoBadge

/**
 * Modèle de données pour chaque niveau d'arborescence :
 * 1. Filière
 * 2. Niveau (L1, L2, etc.)
 * 3. Deux dossiers : "UEs" et "Sujets, TD & TP"
 * 4. Code UE -> Cours en liste
 */
data class SavedDocRecord(
    val localDoc: LocalDownloadedDocument,
    val meta: SavedDocumentMeta
)

enum class FolderViewDepth {
    FILIERES_ROOT,       // 1. Liste des filières (en mosaïque)
    NIVEAUX_DE_FILIERE,  // 2. Dossiers niveaux de la filière (en mosaïque)
    CHOIX_UE_OU_SUJET,   // 3. Deux dossiers : "UEs" et "Sujets, TD & TP" (en mosaïque)
    LISTE_CODES_UE,      // 4a. Liste des codes UE dans le dossier UEs (en mosaïque)
    FICHIERS_COURS_UE,   // 5a. Cours de l'UE affichés en liste
    FICHIERS_TD_SUJETS   // 4b. Sujets, TD & TP affichés en liste
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnregistresScreen(
    viewModel: SearchViewModel,
    profile: StudentProfile,
    onStudyWithAi: (LocalDownloadedDocument) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allLocalFiles by viewModel.allLocalFiles.collectAsState()
    val profileManager = remember { StudentProfileManager(context) }

    var currentDepth by remember { mutableStateOf(FolderViewDepth.FILIERES_ROOT) }
    var selectedFiliereName by remember { mutableStateOf("") }
    var selectedNiveauName by remember { mutableStateOf("") }
    var selectedUeCode by remember { mutableStateOf("") }

    // Navigation arrière
    BackHandler(enabled = currentDepth != FolderViewDepth.FILIERES_ROOT) {
        when (currentDepth) {
            FolderViewDepth.FICHIERS_COURS_UE -> currentDepth = FolderViewDepth.LISTE_CODES_UE
            FolderViewDepth.LISTE_CODES_UE, FolderViewDepth.FICHIERS_TD_SUJETS -> currentDepth = FolderViewDepth.CHOIX_UE_OU_SUJET
            FolderViewDepth.CHOIX_UE_OU_SUJET -> currentDepth = FolderViewDepth.NIVEAUX_DE_FILIERE
            FolderViewDepth.NIVEAUX_DE_FILIERE -> currentDepth = FolderViewDepth.FILIERES_ROOT
            FolderViewDepth.FILIERES_ROOT -> {}
        }
    }

    // Regrouper tous les documents enregistrés
    val allRecords = remember(allLocalFiles, profile) {
        allLocalFiles.map { localDoc ->
            val meta = profileManager.getDocumentMeta(localDoc.originalKey)
                ?: profileManager.getDocumentMeta(localDoc.name)
                ?: SavedDocumentMeta(
                    fileKey = localDoc.originalKey,
                    title = localDoc.formattedTitle,
                    ecoleNom = profile.ecoleNom ?: "Université de Garoua",
                    filiereNom = profile.filiereNom ?: profile.filiereCode ?: "Filière Générale",
                    niveau = localDoc.niveau.ifBlank { profile.niveau },
                    ueCode = extractUeCode(localDoc.name),
                    ueIntitule = localDoc.formattedTitle.take(30),
                    isSujetOrTd = localDoc.typeCategory != "Cours",
                    isCorrige = localDoc.name.startsWith("corrige_")
                )
            SavedDocRecord(localDoc, meta)
        }
    }

    // Extraction des filières distinctes
    val distinctFilieres = remember(allRecords) {
        allRecords.map { it.meta.filiereNom }.distinct().sorted()
    }

    // Extraction des niveaux pour la filière sélectionnée
    val niveauxForSelectedFiliere = remember(allRecords, selectedFiliereName) {
        allRecords.filter { it.meta.filiereNom == selectedFiliereName }
            .map { it.meta.niveau }
            .distinct()
            .sorted()
    }

    // Documents de la filière et du niveau sélectionnés
    val currentNiveauRecords = remember(allRecords, selectedFiliereName, selectedNiveauName) {
        allRecords.filter { it.meta.filiereNom == selectedFiliereName && it.meta.niveau == selectedNiveauName }
    }

    val coursRecords = remember(currentNiveauRecords) {
        currentNiveauRecords.filter { !it.meta.isSujetOrTd }
    }

    val tdSujetsRecords = remember(currentNiveauRecords) {
        currentNiveauRecords.filter { it.meta.isSujetOrTd }
    }

    // Codes UE distincts dans le dossier UEs
    val ueCodesList = remember(coursRecords) {
        coursRecords.groupBy { it.meta.ueCode }.map { (code, docs) ->
            code to docs
        }.sortedBy { it.first }
    }

    // Cours spécifiques à l'UE choisie
    val coursForSelectedUe = remember(coursRecords, selectedUeCode) {
        coursRecords.filter { it.meta.ueCode == selectedUeCode }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CAMPUS EDUCATIF",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    if (currentDepth != FolderViewDepth.FILIERES_ROOT) {
                        IconButton(onClick = {
                            when (currentDepth) {
                                FolderViewDepth.FICHIERS_COURS_UE -> currentDepth = FolderViewDepth.LISTE_CODES_UE
                                FolderViewDepth.LISTE_CODES_UE, FolderViewDepth.FICHIERS_TD_SUJETS -> currentDepth = FolderViewDepth.CHOIX_UE_OU_SUJET
                                FolderViewDepth.CHOIX_UE_OU_SUJET -> currentDepth = FolderViewDepth.NIVEAUX_DE_FILIERE
                                FolderViewDepth.NIVEAUX_DE_FILIERE -> currentDepth = FolderViewDepth.FILIERES_ROOT
                                FolderViewDepth.FILIERES_ROOT -> {}
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_academic_cap_white),
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Fil d'Ariane sans "Ce PC"
            BreadcrumbMosaicBar(
                depth = currentDepth,
                filiere = selectedFiliereName,
                niveau = selectedNiveauName,
                ueCode = selectedUeCode,
                onNavigate = { targetDepth ->
                    currentDepth = targetDepth
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (allLocalFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                        Text(
                            text = "Aucun document enregistré",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = onNavigateToSearch,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Rechercher des cours", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                when (currentDepth) {
                    // 1. Classement par Filière d'abord (en mosaïque)
                    FolderViewDepth.FILIERES_ROOT -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Dossiers des Filières",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            val pairs = distinctFilieres.chunked(2)
                            items(pairs) { rowFilieres ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val f1 = rowFilieres[0]
                                    val count1 = allRecords.count { it.meta.filiereNom == f1 }
                                    MosaicFolderCard(
                                        title = f1,
                                        subtitle = "$count1 document(s)",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedFiliereName = f1
                                            currentDepth = FolderViewDepth.NIVEAUX_DE_FILIERE
                                        }
                                    )

                                    if (rowFilieres.size > 1) {
                                        val f2 = rowFilieres[1]
                                        val count2 = allRecords.count { it.meta.filiereNom == f2 }
                                        MosaicFolderCard(
                                            title = f2,
                                            subtitle = "$count2 document(s)",
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                selectedFiliereName = f2
                                                currentDepth = FolderViewDepth.NIVEAUX_DE_FILIERE
                                            }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 2. Dossiers par Niveau dans la filière (en mosaïque)
                    FolderViewDepth.NIVEAUX_DE_FILIERE -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "$selectedFiliereName - Choisissez le niveau",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            val pairs = niveauxForSelectedFiliere.chunked(2)
                            items(pairs) { rowNiveaux ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val n1 = rowNiveaux[0]
                                    val count1 = allRecords.count { it.meta.filiereNom == selectedFiliereName && it.meta.niveau == n1 }
                                    MosaicFolderCard(
                                        title = "Niveau $n1",
                                        subtitle = "$count1 document(s)",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedNiveauName = n1
                                            currentDepth = FolderViewDepth.CHOIX_UE_OU_SUJET
                                        }
                                    )

                                    if (rowNiveaux.size > 1) {
                                        val n2 = rowNiveaux[1]
                                        val count2 = allRecords.count { it.meta.filiereNom == selectedFiliereName && it.meta.niveau == n2 }
                                        MosaicFolderCard(
                                            title = "Niveau $n2",
                                            subtitle = "$count2 document(s)",
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                selectedNiveauName = n2
                                                currentDepth = FolderViewDepth.CHOIX_UE_OU_SUJET
                                            }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 3. Exactement deux dossiers en mosaïque : "UEs" et "Sujets, TD & TP"
                    FolderViewDepth.CHOIX_UE_OU_SUJET -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "$selectedFiliereName ($selectedNiveauName)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MosaicFolderCard(
                                    title = "Dossier UEs",
                                    subtitle = "${coursRecords.size} cours",
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentDepth = FolderViewDepth.LISTE_CODES_UE }
                                )

                                MosaicFolderCard(
                                    title = "Sujets, TD & TP",
                                    subtitle = "${tdSujetsRecords.size} épreuve(s)",
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentDepth = FolderViewDepth.FICHIERS_TD_SUJETS }
                                )
                            }
                        }
                    }

                    // 4a. Dans "UEs" : les codes unités en mosaïque
                    FolderViewDepth.LISTE_CODES_UE -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Codes d'Unités d'Enseignement (UE)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            val pairs = ueCodesList.chunked(2)
                            items(pairs) { rowUes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val ue1 = rowUes[0]
                                    MosaicFolderCard(
                                        title = "UE ${ue1.first}",
                                        subtitle = "${ue1.second.size} cours",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedUeCode = ue1.first
                                            currentDepth = FolderViewDepth.FICHIERS_COURS_UE
                                        }
                                    )

                                    if (rowUes.size > 1) {
                                        val ue2 = rowUes[1]
                                        MosaicFolderCard(
                                            title = "UE ${ue2.first}",
                                            subtitle = "${ue2.second.size} cours",
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                selectedUeCode = ue2.first
                                                currentDepth = FolderViewDepth.FICHIERS_COURS_UE
                                            }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 5a. Cours affichés en liste avec icône d'extension (ou PDF)
                    FolderViewDepth.FICHIERS_COURS_UE -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "Cours de l'UE $selectedUeCode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            items(coursForSelectedUe, key = { it.localDoc.name }) { item ->
                                FileListRow(
                                    item = item,
                                    onOpen = { viewModel.openLocalFile(context, item.localDoc) },
                                    onOpenInAi = { onStudyWithAi(item.localDoc) },
                                    onDelete = { viewModel.deleteLocalFile(context, item.localDoc) }
                                )
                            }
                        }
                    }

                    // 4b. Sujets, TD & TP affichés en liste
                    FolderViewDepth.FICHIERS_TD_SUJETS -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "Sujets, TD & TP - $selectedFiliereName ($selectedNiveauName)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            if (tdSujetsRecords.isEmpty()) {
                                item {
                                    Text("Aucun sujet ou TD dans ce dossier.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                items(tdSujetsRecords, key = { it.localDoc.name }) { item ->
                                    FileListRow(
                                        item = item,
                                        onOpen = { viewModel.openLocalFile(context, item.localDoc) },
                                        onOpenInAi = { onStudyWithAi(item.localDoc) },
                                        onDelete = { viewModel.deleteLocalFile(context, item.localDoc) }
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

/**
 * Carte de dossier affichée en mosaïque (grille 2 colonnes)
 */
@Composable
private fun MosaicFolderCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(105.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFFF59E0B), // Teinte dossier Windows
                modifier = Modifier.size(36.dp)
            )

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Ligne de fichier affiché en liste avec icône de son format (ou PDF par défaut),
 * nom du cours et boutons "Lire" & "Ouvrir dans une IA".
 */
@Composable
private fun FileListRow(
    item: SavedDocRecord,
    onOpen: () -> Unit,
    onOpenInAi: () -> Unit,
    onDelete: () -> Unit
) {
    val localDoc = item.localDoc
    val extension = localDoc.file.extension.ifBlank { "pdf" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileLogoBadge(
                extension = extension,
                modifier = Modifier.size(width = 42.dp, height = 50.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localDoc.formattedTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text("Lire", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onOpenInAi,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ouvrir dans une IA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Fil d'Ariane sans "Ce PC"
 */
@Composable
private fun BreadcrumbMosaicBar(
    depth: FolderViewDepth,
    filiere: String,
    niveau: String,
    ueCode: String,
    onNavigate: (FolderViewDepth) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dossiers",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigate(FolderViewDepth.FILIERES_ROOT) }
            )

            if (filiere.isNotBlank() && depth != FolderViewDepth.FILIERES_ROOT) {
                Text(text = " > ", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = filiere.take(12),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (depth == FolderViewDepth.NIVEAUX_DE_FILIERE) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigate(FolderViewDepth.NIVEAUX_DE_FILIERE) }
                )
            }

            if (niveau.isNotBlank() && (depth == FolderViewDepth.CHOIX_UE_OU_SUJET || depth == FolderViewDepth.LISTE_CODES_UE || depth == FolderViewDepth.FICHIERS_COURS_UE || depth == FolderViewDepth.FICHIERS_TD_SUJETS)) {
                Text(text = " > ", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = niveau,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (depth == FolderViewDepth.CHOIX_UE_OU_SUJET) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigate(FolderViewDepth.CHOIX_UE_OU_SUJET) }
                )
            }

            if (depth == FolderViewDepth.LISTE_CODES_UE || depth == FolderViewDepth.FICHIERS_COURS_UE) {
                Text(text = " > ", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = "UEs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (depth == FolderViewDepth.LISTE_CODES_UE) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigate(FolderViewDepth.LISTE_CODES_UE) }
                )
            } else if (depth == FolderViewDepth.FICHIERS_TD_SUJETS) {
                Text(text = " > ", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = "Sujets & TD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (ueCode.isNotBlank() && depth == FolderViewDepth.FICHIERS_COURS_UE) {
                Text(text = " > ", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = ueCode,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun extractUeCode(name: String): String {
    val clean = name.removePrefix("cours_").removePrefix("sujet_").removePrefix("corrige_")
    val parts = clean.split("_")
    for (part in parts) {
        if (part.matches(Regex("[A-Za-z]{2,5}[0-9]{2,4}[A-Za-z]?"))) {
            return part.uppercase()
        }
    }
    return "UE"
}
