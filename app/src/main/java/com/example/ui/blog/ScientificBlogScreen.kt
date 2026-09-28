package com.example.ui.blog

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

/**
 * Modèle pour les notifications de mise à jour de l'application
 */
data class AppUpdateNotification(
    val id: Int,
    val version: String,
    val title: String,
    val date: String,
    val categoryBadge: String,
    val summary: String,
    val fullChangelog: String,
    val developedFeatures: List<String>,
    val isLatest: Boolean = false,
    var readCount: Int = 120
)

object AppNotificationsRepository {
    val updates = listOf(
        AppUpdateNotification(
            id = 4,
            version = "1.17",
            title = "Partage IA Universel & Flux d'Étude Immédiat",
            date = "27 Septembre 2026",
            categoryBadge = "Nouvelle Version",
            summary = "Intégration du partage direct des cours et épreuves vers Gemini, ChatGPT, Claude AI et DeepSeek, avec déclenchement immédiat dès la fin d'un téléchargement.",
            fullChangelog = """
                La version 1.17 franchit un nouveau cap dans l'assistance académique des étudiants de l'Université de Garoua.
                
                Désormais, tout document téléchargé (cours magistral, sujet d'examen ou corrigé) peut être transmis directement aux applications d'intelligence artificielle les plus performantes (Google Gemini, ChatGPT, Claude AI, DeepSeek) en conservant le fichier déjà attaché via le FileProvider Android sécurisé.
                
                Dès qu'un téléchargement se termine dans la recherche, un bouton 'Étudier avec IA' s'affiche pour vous permettre d'analyser vos supports de cours sans perte de temps.
            """.trimIndent(),
            developedFeatures = listOf(
                "Partage direct vers les applications d'IA : Gemini, ChatGPT, Claude AI et DeepSeek",
                "Génération sécurisée d'URI FileProvider avec droits temporaires de lecture",
                "Bouton 'Étudier avec IA' disponible immédiatement à la fin d'un téléchargement",
                "Composant de sélection d'IA épuré affichant le nom de chaque application",
                "Option d'installation rapide depuis Google Play si l'IA cible n'est pas installée"
            ),
            isLatest = true,
            readCount = 312
        ),
        AppUpdateNotification(
            id = 3,
            version = "1.16",
            title = "Refonte de la Recherche & Identités Visuelles Officielles",
            date = "26 Septembre 2026",
            categoryBadge = "Recherche & Design",
            summary = "Déploiement du panneau de filtres déroulants et intégration des logos officiels de l'Université de Garoua, de la FS et de la FMSB.",
            fullChangelog = """
                Cette mise à jour modernise l'interface utilisateur pour offrir une expérience plus fluide et institutionnelle.
                
                Les paramètres de recherche (sélection d'école, filière, niveau, semestre et type d'épreuve) s'affichent désormais sous forme de panneau interactif repliable.
                
                Les armoiries officielles de l'Université de Garoua ainsi que les logos respectifs de la Faculté des Sciences (FS) et de la Faculté de Médecine et des Sciences Biomédicales (FMSB) sont désormais intégrés dans les en-têtes et le catalogue.
            """.trimIndent(),
            developedFeatures = listOf(
                "Panneau déroulant des paramètres de recherche pour filtrer instantanément les UE",
                "Intégration des logos officiels de l'Université de Garoua, de la FS et de la FMSB",
                "En-têtes unifiés avec barre d'état et navigation par onglets",
                "Recherche textuelle optimisée avec suggestions en temps réel"
            ),
            isLatest = false,
            readCount = 485
        ),
        AppUpdateNotification(
            id = 2,
            version = "1.15",
            title = "Espace Téléchargements Hors-Ligne & Visualisation PDF",
            date = "25 Septembre 2026",
            categoryBadge = "Stockage Local",
            summary = "Nouvel onglet dédié aux documents téléchargés en local, avec organisation ciblée par types de documents et lecteur PDF intégré.",
            fullChangelog = """
                La version 1.15 apporte une gestion complète du mode hors-ligne pour permettre aux étudiants de réviser même sans connexion réseau.
                
                Tous les fichiers téléchargés sont répertoriés dans un espace dédié, triés par type (Cours, Sujets, Corrigés), avec indication de la taille et de la date.
                
                L'ouverture des fichiers s'effectue en un clic grâce au visualiseur PDF intégré au système.
            """.trimIndent(),
            developedFeatures = listOf(
                "Onglet 'Documents Téléchargés' avec consultation 100% hors-ligne",
                "Filtrage ciblé par types : Cours magistraux, Sujets d'examen et Corrigés officiels",
                "Ouverture rapide et sécurisée des fichiers PDF locaux",
                "Boutons de partage et de suppression pour libérer de l'espace de stockage"
            ),
            isLatest = false,
            readCount = 620
        ),
        AppUpdateNotification(
            id = 1,
            version = "1.14",
            title = "Catalogue Académique & Téléchargements des Épreuves",
            date = "24 Septembre 2026",
            categoryBadge = "Lancement Initial",
            summary = "Déploiement initial du catalogue des facultés et filières, arborescence des UE et moteur de téléchargement des cours et épreuves.",
            fullChangelog = """
                Lancement officiel de la plateforme Campus Éducatif de l'Université de Garoua.
                
                L'application permet de naviguer dans l'offre de formation des différentes facultés, de consulter la disponibilité des cours par semestre et de télécharger les sujets d'examen ainsi que leurs corrigés.
            """.trimIndent(),
            developedFeatures = listOf(
                "Catalogue structuré des facultés : FS, FMSB et leurs départements respectifs",
                "Navigation par niveau d'étude (L1 à M2) et semestres universitaires",
                "Moteur de téléchargement direct des fichiers avec barre de progression",
                "Badges de disponibilité des cours et indicateurs de crédits"
            ),
            isLatest = false,
            readCount = 890
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificBlogScreen(
    onExploreWithGemini: (title: String, category: String, summary: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedVersionFilter by rememberSaveable { mutableStateOf("Toutes") }
    var activeUpdateForDetails by remember { mutableStateOf<AppUpdateNotification?>(null) }
    val updates = remember { mutableStateListOf(*AppNotificationsRepository.updates.toTypedArray()) }

    val versionFilters = listOf("Toutes", "v1.17", "v1.16", "v1.15", "v1.14")

    val filteredUpdates = remember(updates.toList(), searchQuery, selectedVersionFilter) {
        updates.filter { notif ->
            val matchVersion = when (selectedVersionFilter) {
                "Toutes" -> true
                "v1.17" -> notif.version == "1.17"
                "v1.16" -> notif.version == "1.16"
                "v1.15" -> notif.version == "1.15"
                "v1.14" -> notif.version == "1.14"
                else -> true
            }
            val matchQuery = if (searchQuery.isBlank()) true else {
                notif.title.contains(searchQuery, ignoreCase = true) ||
                notif.version.contains(searchQuery, ignoreCase = true) ||
                notif.summary.contains(searchQuery, ignoreCase = true) ||
                notif.developedFeatures.any { it.contains(searchQuery, ignoreCase = true) }
            }
            matchVersion && matchQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 1. En-tête informatif du centre de notifications
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Notifications & Mises à Jour",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF2E7D32),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "v1.17",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Historique des versions et nouvelles fonctionnalités de l'application",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Champ de recherche
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Rechercher une mise à jour ou fonctionnalité...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // 3. Filtre par version
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(versionFilters) { filter ->
                        val isSelected = selectedVersionFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedVersionFilter = filter },
                            label = {
                                Text(
                                    text = filter,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // 4. Liste des cartes de mise à jour
            items(filteredUpdates, key = { it.id }) { notif ->
                UpdateNotificationCard(
                    notification = notif,
                    onClick = { activeUpdateForDetails = notif }
                )
            }
        }
    }

    // Modal de détails de la mise à jour
    activeUpdateForDetails?.let { notif ->
        UpdateDetailsDialog(
            notification = notif,
            onDismiss = { activeUpdateForDetails = null }
        )
    }
}

@Composable
fun UpdateNotificationCard(
    notification: AppUpdateNotification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (notification.isLatest) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isLatest) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Ligne d'en-tête : Badge version, Badge statut, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (notification.isLatest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "v${notification.version}",
                            color = if (notification.isLatest) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    if (notification.isLatest) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "Actuelle",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = notification.date,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Titre de la mise à jour
            Text(
                text = notification.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Résumé
            Text(
                text = notification.summary,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Liste succincte des fonctionnalités déjà développées
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                notification.developedFeatures.take(3).forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (notification.isLatest) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = feature,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pied de carte : En savoir plus & Partager
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Voir toutes les fonctionnalités (${notification.developedFeatures.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDetailsDialog(
    notification: AppUpdateNotification,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Version ${notification.version}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = notification.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fermer")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Mise à jour v${notification.version}")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Découvrez la version ${notification.version} de l'application Campus Éducatif - Université de Garoua :\n${notification.title}\n\n${notification.summary}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Partager la mise à jour"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Partager", tint = MaterialTheme.colorScheme.primary)
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(34.dp)
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = notification.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Déployée le ${notification.date} • Université de Garoua",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Présentation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notification.fullChangelog,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    item {
                        Text(
                            text = "Fonctionnalités développées & déployées",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            notification.developedFeatures.forEach { feat ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = feat,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
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
