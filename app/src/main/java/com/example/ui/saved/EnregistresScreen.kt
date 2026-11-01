package com.example.ui.saved

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Modèle de données pour les documents enregistrés
 */
data class SavedDocRecord(
    val localDoc: LocalDownloadedDocument,
    val meta: SavedDocumentMeta
)

/**
 * Niveaux d'arborescence :
 * 1. FILIERES_ROOT : Dossiers des filières (ex: INF, RIM, etc.)
 * 2. CHOIX_COURS_OU_SUJETS : Deux dossiers ("Cours" et "Sujets")
 * 3. FICHIERS_COURS : Liste des cours de la filière
 * 4. FICHIERS_SUJETS : Liste des sujets de la filière
 */
enum class FolderViewDepth {
    FILIERES_ROOT,
    CHOIX_COURS_OU_SUJETS,
    FICHIERS_COURS,
    FICHIERS_SUJETS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnregistresScreen(
    viewModel: SearchViewModel,
    profile: StudentProfile,
    onEditProfile: () -> Unit,
    onStudyWithAi: (LocalDownloadedDocument) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allLocalFiles by viewModel.allLocalFiles.collectAsState()
    val profileManager = remember { StudentProfileManager(context) }

    var currentDepth by remember { mutableStateOf(FolderViewDepth.FILIERES_ROOT) }
    var selectedFiliereName by remember { mutableStateOf("") }

    // Navigation arrière
    BackHandler(enabled = currentDepth != FolderViewDepth.FILIERES_ROOT) {
        when (currentDepth) {
            FolderViewDepth.FICHIERS_COURS,
            FolderViewDepth.FICHIERS_SUJETS -> currentDepth = FolderViewDepth.CHOIX_COURS_OU_SUJETS
            FolderViewDepth.CHOIX_COURS_OU_SUJETS -> currentDepth = FolderViewDepth.FILIERES_ROOT
            FolderViewDepth.FILIERES_ROOT -> {}
        }
    }

    // Regrouper tous les documents enregistrés (cours et sujets)
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
                    isCorrige = localDoc.typeCategory == "Corrigés"
                )
            SavedDocRecord(localDoc, meta)
        }
    }

    // Extraction des filières distinctes (avec au moins la filière de profil de l'étudiant si configurée)
    val distinctFilieres = remember(allRecords, profile) {
        val list = allRecords.map { it.meta.filiereNom }.distinct().toMutableList()
        val studentFiliere = profile.filiereNom ?: profile.filiereCode
        if (!studentFiliere.isNullOrBlank() && !list.contains(studentFiliere)) {
            list.add(studentFiliere)
        }
        list.sorted()
    }

    // Documents de la filière sélectionnée
    val filiereRecords = remember(allRecords, selectedFiliereName) {
        allRecords.filter {
            it.meta.filiereNom.equals(selectedFiliereName, ignoreCase = true) ||
            extractFiliereCode(it.meta.filiereNom).equals(selectedFiliereName, ignoreCase = true)
        }
    }

    val coursRecords = remember(filiereRecords) {
        filiereRecords.filter { !it.meta.isSujetOrTd && it.localDoc.typeCategory == "Cours" }
    }

    val sujetsRecords = remember(filiereRecords) {
        filiereRecords.filter { it.meta.isSujetOrTd || it.localDoc.typeCategory != "Cours" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 5.dp, start = 14.dp, end = 14.dp)
    ) {
        // En-tête compact positionné exactement à 5px du top 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentDepth != FolderViewDepth.FILIERES_ROOT) {
                IconButton(
                    onClick = {
                        when (currentDepth) {
                            FolderViewDepth.FICHIERS_COURS,
                            FolderViewDepth.FICHIERS_SUJETS -> currentDepth = FolderViewDepth.CHOIX_COURS_OU_SUJETS
                            FolderViewDepth.CHOIX_COURS_OU_SUJETS -> currentDepth = FolderViewDepth.FILIERES_ROOT
                            FolderViewDepth.FILIERES_ROOT -> {}
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Logo standard dossier sans étoile
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "CAMPUS EDUCATIF",
                fontSize = 18.sp,
                fontFamily = com.example.ui.theme.AntonFontFamily,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onEditProfile,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Modifier mon profil",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fil d'Ariane épuré avec séparateurs espacés (aucun '>')
        BreadcrumbMosaicBar(
            depth = currentDepth,
            filiere = extractFiliereCode(selectedFiliereName),
            onNavigate = { newDepth -> currentDepth = newDepth }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Contenu selon la profondeur d'arborescence
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (currentDepth) {
                // 1. Liste des dossiers de filières
                FolderViewDepth.FILIERES_ROOT -> {
                    if (distinctFilieres.isEmpty()) {
                        EmptyEnregistresState(onNavigateToSearch = onNavigateToSearch)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val pairs = distinctFilieres.chunked(2)
                            items(pairs) { rowFilieres ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val f1 = rowFilieres[0]
                                    MosaicFolderCard(
                                        code = extractFiliereCode(f1),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            selectedFiliereName = f1
                                            currentDepth = FolderViewDepth.CHOIX_COURS_OU_SUJETS
                                        }
                                    )

                                    if (rowFilieres.size > 1) {
                                        val f2 = rowFilieres[1]
                                        MosaicFolderCard(
                                            code = extractFiliereCode(f2),
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                selectedFiliereName = f2
                                                currentDepth = FolderViewDepth.CHOIX_COURS_OU_SUJETS
                                            }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Deux dossiers : "Cours" et "Sujets"
                FolderViewDepth.CHOIX_COURS_OU_SUJETS -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MosaicFolderCard(
                                code = "Cours",
                                modifier = Modifier.weight(1f),
                                onClick = { currentDepth = FolderViewDepth.FICHIERS_COURS }
                            )

                            MosaicFolderCard(
                                code = "Sujets",
                                modifier = Modifier.weight(1f),
                                onClick = { currentDepth = FolderViewDepth.FICHIERS_SUJETS }
                            )
                        }
                    }
                }

                // 3a. Liste des cours (aucun texte "Dossier UEs - ...")
                FolderViewDepth.FICHIERS_COURS -> {
                    if (coursRecords.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun cours enregistré pour cette filière.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(coursRecords, key = { it.localDoc.name }) { item ->
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

                // 3b. Liste des sujets (aucun texte superflu)
                FolderViewDepth.FICHIERS_SUJETS -> {
                    if (sujetsRecords.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun sujet d'examen enregistré pour cette filière.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sujetsRecords, key = { it.localDoc.name }) { item ->
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

/**
 * Carte de dossier en mosaïque :
 * Affiche uniquement l'icône du dossier et le code/nom au centre.
 */
@Composable
private fun MosaicFolderCard(
    code: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Dossier $code",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = code,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Ligne de fichier téléchargé :
 * Logo extension à gauche, nom sans .pdf au centre, actions Lire / IA / Supprimer.
 */
@Composable
private fun FileListRow(
    item: SavedDocRecord,
    onOpen: () -> Unit,
    onOpenInAi: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileLogoBadge(
                extension = item.localDoc.file.extension
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.localDoc.formattedTitle.replace(Regex("(?i)\\.pdf$"), "").trim(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.meta.ueCode.isNotBlank() && item.meta.ueCode != "UE") {
                    Text(
                        text = item.meta.ueCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

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
 * Fil d'Ariane avec symbole moderne et espacements généreux (aucun '>')
 */
@Composable
private fun BreadcrumbMosaicBar(
    depth: FolderViewDepth,
    filiere: String,
    onNavigate: (FolderViewDepth) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Racine",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigate(FolderViewDepth.FILIERES_ROOT) }
            )

            if (filiere.isNotBlank() && depth != FolderViewDepth.FILIERES_ROOT) {
                BreadcrumbSeparator()

                Text(
                    text = filiere,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (depth == FolderViewDepth.CHOIX_COURS_OU_SUJETS) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigate(FolderViewDepth.CHOIX_COURS_OU_SUJETS) }
                )
            }

            if (depth == FolderViewDepth.FICHIERS_COURS) {
                BreadcrumbSeparator()

                Text(
                    text = "Cours",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else if (depth == FolderViewDepth.FICHIERS_SUJETS) {
                BreadcrumbSeparator()

                Text(
                    text = "Sujets",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Séparateur moderne avec espace généreux de chaque côté
 */
@Composable
private fun BreadcrumbSeparator() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(2.dp)
                .size(14.dp)
        )
    }
}

@Composable
private fun EmptyEnregistresState(onNavigateToSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Aucun document enregistré",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Téléchargez des cours et des sujets pour les retrouver ici classés par filière.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onNavigateToSearch,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Rechercher des documents", fontWeight = FontWeight.Bold)
        }
    }
}

private fun extractFiliereCode(filiereName: String): String {
    val clean = filiereName.trim()
    if (clean.length <= 5 && !clean.contains(" ")) {
        return clean.uppercase()
    }
    val parenMatch = Regex("""\(([A-Za-z0-9]+)\)""").find(clean)
    if (parenMatch != null) {
        return parenMatch.groupValues[1].uppercase()
    }
    val lower = clean.lowercase()
    if (lower.contains("informatique") || lower.contains("info")) return "INF"
    if (lower.contains("reseau") || lower.contains("réseau") || lower.contains("telecom") || lower.contains("télécom") || lower.contains("rim") || lower.contains("internet")) return "RIM"
    if (lower.contains("genie logiciel") || lower.contains("génie logiciel") || lower.contains("logiciel")) return "GL"
    if (lower.contains("medecine") || lower.contains("médecine") || lower.contains("fmsb") || lower.contains("sante") || lower.contains("santé")) return "FMSB"
    if (lower.contains("science") || lower.contains("fondamentale") || lower.contains("fs")) return "FS"
    if (lower.contains("mathematique") || lower.contains("mathématique") || lower.contains("math")) return "MATH"
    if (lower.contains("physique")) return "PHY"
    if (lower.contains("chimie")) return "CHIM"
    if (lower.contains("biologie") || lower.contains("bio")) return "BIO"
    if (lower.contains("droit")) return "DROIT"
    if (lower.contains("gestion") || lower.contains("eco") || lower.contains("éco")) return "ECO"

    val words = clean.split(" ", "-", "_").filter { it.length > 2 && it.lowercase() !in listOf("les", "des", "pour", "dans", "avec", "de", "du", "et", "en") }
    if (words.size >= 2) {
        return words.map { it.first() }.joinToString("").uppercase().take(4)
    }
    return clean.take(4).uppercase()
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
