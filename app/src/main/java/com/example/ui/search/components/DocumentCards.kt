package com.example.ui.search.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.DownloadStatus

fun extractFileExtension(chemin: String?, nomOriginal: String?, defaultType: String?): String {
    val fromOriginal = nomOriginal?.substringAfterLast('.', "")?.trim()?.lowercase()
    if (!fromOriginal.isNullOrBlank() && fromOriginal.length in 2..5) return fromOriginal

    val fromPath = chemin?.substringAfterLast('.', "")?.substringBefore('?')?.trim()?.lowercase()
    if (!fromPath.isNullOrBlank() && fromPath.length in 2..5) return fromPath

    val type = defaultType?.lowercase()?.trim() ?: "pdf"
    return if (type.contains("pdf")) "pdf"
    else if (type.contains("word") || type.contains("doc")) "docx"
    else if (type.contains("ppt")) "pptx"
    else if (type.contains("xls")) "xlsx"
    else "pdf" // Si le type n'est pas connu, mettre le logo d'un PDF
}

fun formatDownloadSpeed(speedKBps: Double, progress: Int): String {
    return when {
        speedKBps >= 1024.0 -> String.format(java.util.Locale.US, "%.1f Mo/s", speedKBps / 1024.0)
        speedKBps > 0 -> "${speedKBps.toInt()} Ko/s"
        else -> "$progress%"
    }
}

/**
 * Affiche le logo du format de fichier :
 * Par défaut ou si inconnu, affiche le logo d'un PDF.
 */
@Composable
fun FileLogoBadge(
    extension: String,
    modifier: Modifier = Modifier
) {
    val isPdf = extension.isBlank() || extension.equals("pdf", ignoreCase = true)

    val badgeColor = when {
        isPdf -> Color(0xFFD32F2F)
        extension.contains("doc") -> Color(0xFF1565C0)
        extension.contains("ppt") -> Color(0xFFE65100)
        extension.contains("xls") -> Color(0xFF2E7D32)
        else -> Color(0xFFD32F2F)
    }

    val containerColor = when {
        isPdf -> Color(0xFFFFEBEE)
        extension.contains("doc") -> Color(0xFFE3F2FD)
        extension.contains("ppt") -> Color(0xFFFFF3E0)
        extension.contains("xls") -> Color(0xFFE8F5E9)
        else -> Color(0xFFFFEBEE)
    }

    Surface(
        modifier = modifier.size(width = 54.dp, height = 64.dp),
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
        color = containerColor,
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
                contentDescription = if (isPdf) "Logo PDF" else "Fichier $extension",
                tint = badgeColor,
                modifier = Modifier.size(30.dp)
            )

            Surface(
                shape = RoundedCornerShape(3.dp),
                color = badgeColor
            ) {
                Text(
                    text = if (isPdf) "PDF" else extension.uppercase().take(4),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Carte de cours épurée :
 *  - Téléchargement au clic
 *  - Pas de barre de progression
 *  - Vitesse affichée à gauche du nom du cours pendant le téléchargement sans déformer le composant
 */
@Composable
fun CoursItemCard(
    cours: Cours,
    ue: UeWithDocuments,
    isDownloaded: Boolean,
    downloadStatus: DownloadStatus,
    onDownload: () -> Unit,
    onOpen: () -> Unit,
    onStudyWithAi: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extension = extractFileExtension(
        chemin = cours.downloadUrl,
        nomOriginal = cours.nomFichierOriginal,
        defaultType = cours.type
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isDownloaded) onOpen()
                else if (downloadStatus !is DownloadStatus.Progress) onDownload()
            }
            .semantics { contentDescription = "Cours ${cours.titre}" },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // En-tête : Badge UE & statut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = ue.code.trim().uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isDownloaded || cours.deja_telecharge == true) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Téléchargé",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logo PDF ou extension
            FileLogoBadge(
                extension = extension,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Ligne avec vitesse de téléchargement à gauche du nom du cours
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 34.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (downloadStatus is DownloadStatus.Progress) {
                    val speedText = formatDownloadSpeed(downloadStatus.speedKBps, downloadStatus.progress)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = speedText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = cours.titre,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Carte d'épreuve d'examen :
 * Téléchargement au clic avec vitesse affichée à gauche du nom du cours.
 */
@Composable
fun SujetItemCard(
    sujet: Sujet,
    ue: UeWithDocuments,
    isSujetDownloaded: Boolean,
    isCorrigeDownloaded: Boolean,
    sujetDownloadStatus: DownloadStatus,
    corrigeDownloadStatus: DownloadStatus,
    onDownloadSujet: () -> Unit,
    onOpenSujet: () -> Unit,
    onStudySujetWithAi: () -> Unit,
    onShareSujet: () -> Unit,
    onDeleteSujet: () -> Unit,
    onDownloadCorrige: () -> Unit,
    onOpenCorrige: () -> Unit,
    onStudyCorrigeWithAi: () -> Unit,
    onShareCorrige: () -> Unit,
    onDeleteCorrige: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extension = extractFileExtension(
        chemin = sujet.downloadUrl,
        nomOriginal = null,
        defaultType = sujet.type
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isSujetDownloaded) onOpenSujet()
                else if (sujetDownloadStatus !is DownloadStatus.Progress) onDownloadSujet()
            }
            .semantics { contentDescription = "Épreuve ${sujet.titre}" },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // En-tête : Badge UE & statut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = ue.code.trim().uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isSujetDownloaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Téléchargé",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logo PDF
            FileLogoBadge(
                extension = extension,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Ligne avec vitesse de téléchargement à gauche du nom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 34.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (sujetDownloadStatus is DownloadStatus.Progress) {
                    val speedText = formatDownloadSpeed(sujetDownloadStatus.speedKBps, sujetDownloadStatus.progress)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = speedText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = sujet.titre,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
