package com.example.ui.search.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.DownloadStatus
import com.example.ui.theme.PlusJakartaSansFontFamily

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

/**
 * Nettoie le titre du document en retirant l'extension .pdf à la fin
 */
fun cleanDocumentTitle(rawTitle: String): String {
    return rawTitle.replace(Regex("(?i)\\.pdf$"), "").trim()
}

fun formatDownloadSpeed(speedKBps: Double, progress: Int): String {
    return when {
        speedKBps >= 1024.0 -> String.format(java.util.Locale.US, "%.1f Mo/s", speedKBps / 1024.0)
        speedKBps > 0 -> "${speedKBps.toInt()} Ko/s"
        else -> "$progress%"
    }
}

/**
 * Logo de format de fichier (PDF par défaut ou si inconnu)
 * Conçu avec une police moderne, nette et proportionnée.
 */
@Composable
fun FileLogoBadge(
    extension: String,
    modifier: Modifier = Modifier
) {
    val isPdf = extension.isBlank() || extension.equals("pdf", ignoreCase = true)

    val badgeColor = when {
        isPdf -> Color(0xFFEF4444)
        extension.contains("doc") -> Color(0xFF3B82F6)
        extension.contains("ppt") -> Color(0xFFF97316)
        extension.contains("xls") -> Color(0xFF10B981)
        else -> Color(0xFFEF4444)
    }

    val containerColor = when {
        isPdf -> Color(0xFF450A0A).copy(alpha = 0.45f)
        extension.contains("doc") -> Color(0xFF172554).copy(alpha = 0.45f)
        extension.contains("ppt") -> Color(0xFF431407).copy(alpha = 0.45f)
        extension.contains("xls") -> Color(0xFF064E3B).copy(alpha = 0.45f)
        else -> Color(0xFF450A0A).copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier.size(width = 34.dp, height = 42.dp),
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
        color = containerColor,
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 2.5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
                contentDescription = if (isPdf) "Logo PDF" else "Fichier $extension",
                tint = badgeColor,
                modifier = Modifier.size(17.dp)
            )

            Surface(
                shape = RoundedCornerShape(2.5.dp),
                color = badgeColor
            ) {
                Text(
                    text = if (isPdf) "PDF" else extension.uppercase().take(3),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                )
            }
        }
    }
}

/**
 * Carte de cours horizontale :
 *  - Typographie moderne Plus Jakarta Sans plus grande et claire
 *  - De gauche à droite : Logo d'extension, Titre lisible, Badges informatifs
 *  - Téléchargement direct au clic sans bouton superflu
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
    val displayTitle = cleanDocumentTitle(cours.titre)
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 62.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (isDownloaded) onOpen()
                else if (downloadStatus !is DownloadStatus.Progress) onDownload()
            }
            .semantics { contentDescription = "Cours $displayTitle" },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Gauche : Logo du format de fichier
            FileLogoBadge(
                extension = extension
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Centre : Titre du cours moderne + Badges
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (downloadStatus is DownloadStatus.Progress) {
                        val speedText = formatDownloadSpeed(downloadStatus.speedKBps, downloadStatus.progress)
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 5.dp)
                        ) {
                            Text(
                                text = speedText,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = displayTitle,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.1.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badges d'information : taille & année avec police agrandie et moderne
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val taille = cours.tailleLisible
                    if (!taille.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = taille,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                    val annee = cours.anneeAcademique
                    if (!annee.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = annee,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
            }

            // 3. Droite : Statut téléchargé
            if (isDownloaded || cours.deja_telecharge == true) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Téléchargé",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Carte d'épreuve d'examen horizontale :
 *  - Typographie moderne Plus Jakarta Sans agrandie et aérée
 *  - Logo d'extension net
 *  - Nom du sujet bien visible
 *  - Badges : Année, Session, Corrigé, Taille
 *  - Bouton Signaler accessible
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
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extension = extractFileExtension(
        chemin = sujet.downloadUrl,
        nomOriginal = null,
        defaultType = sujet.type
    )
    val displayTitle = cleanDocumentTitle(sujet.titre)
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 62.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (isSujetDownloaded) onOpenSujet()
                else if (sujetDownloadStatus !is DownloadStatus.Progress) onDownloadSujet()
            }
            .semantics { contentDescription = "Épreuve $displayTitle" },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Gauche : Logo du format de fichier
            FileLogoBadge(
                extension = extension
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Centre : Titre moderne + Badges (année, session, corrigé, taille)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (sujetDownloadStatus is DownloadStatus.Progress) {
                        val speedText = formatDownloadSpeed(sujetDownloadStatus.speedKBps, sujetDownloadStatus.progress)
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 5.dp)
                        ) {
                            Text(
                                text = speedText,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = displayTitle,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.1.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badges descriptifs modernes et lisibles
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val annee = sujet.anneeAcademique
                    if (!annee.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = annee,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    if (!sujet.session.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = sujet.session,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    if (sujet.hasCorrige) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Corrigé",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    val taille = sujet.tailleLisible
                    if (!taille.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = taille,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
            }

            // 3. Droite : Bouton Signaler & Statut téléchargé
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                IconButton(
                    onClick = onReport,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Signaler ce sujet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (isSujetDownloaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Téléchargé",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

