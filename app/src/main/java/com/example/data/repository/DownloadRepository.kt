package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.network.CampusEduService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Progress(
        val progress: Int,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val speedKBps: Double = 0.0
    ) : DownloadStatus()
    data class Success(val file: File) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

data class LocalDownloadedDocument(
    val file: File,
    val name: String,
    val formattedTitle: String,
    val typeLabel: String,
    val sizeFormatted: String,
    val lastModifiedMillis: Long,
    val niveau: String = "L1",
    val typeCategory: String = "Cours"
)

class DownloadRepository(private val service: CampusEduService) {

    fun getAllDownloadedFiles(context: Context): List<LocalDownloadedDocument> {
        val dir = context.getExternalFilesDir("documents") ?: context.filesDir
        val files = dir.listFiles() ?: return emptyList()
        return files.filter { it.isFile && (it.extension.equals("pdf", ignoreCase = true) || it.name.contains("_")) }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val rawName = file.name
                val typeLabel = when {
                    rawName.startsWith("cours_") -> "Cours PDF"
                    rawName.startsWith("corrige_") -> "Corrigé PDF"
                    rawName.startsWith("sujet_") -> "Sujet d'Épreuve"
                    else -> "Document PDF"
                }
                val typeCategory = when {
                    rawName.startsWith("cours_") -> "Cours"
                    rawName.startsWith("corrige_") -> "Corrigés"
                    rawName.startsWith("sujet_") -> "Sujets"
                    else -> "Cours"
                }
                val titleClean = rawName.substringAfter("_")
                    .substringAfter("_")
                    .removeSuffix(".pdf")
                    .replace("_", " ")
                    .ifBlank { rawName }

                val niveau = when {
                    rawName.contains("_M2", ignoreCase = true) || rawName.contains("Master 2", ignoreCase = true) || titleClean.contains("M2", ignoreCase = true) -> "M2"
                    rawName.contains("_M1", ignoreCase = true) || rawName.contains("Master 1", ignoreCase = true) || titleClean.contains("M1", ignoreCase = true) -> "M1"
                    rawName.contains("_L3", ignoreCase = true) || rawName.contains("Licence 3", ignoreCase = true) || titleClean.contains("L3", ignoreCase = true) -> "L3"
                    rawName.contains("_L2", ignoreCase = true) || rawName.contains("Licence 2", ignoreCase = true) || titleClean.contains("L2", ignoreCase = true) -> "L2"
                    else -> "L1"
                }

                val bytes = file.length()
                val sizeFormatted = when {
                    bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f Mo", bytes / (1024.0 * 1024.0))
                    bytes >= 1024 -> String.format(java.util.Locale.US, "%.0f Ko", bytes / 1024.0)
                    else -> "$bytes B"
                }

                LocalDownloadedDocument(
                    file = file,
                    name = rawName,
                    formattedTitle = titleClean,
                    typeLabel = typeLabel,
                    sizeFormatted = sizeFormatted,
                    lastModifiedMillis = file.lastModified(),
                    niveau = niveau,
                    typeCategory = typeCategory
                )
            }
    }

    fun getLocalFileForCours(context: Context, id: Int): File? {
        val dir = context.getExternalFilesDir("documents") ?: context.filesDir
        val files = dir.listFiles() ?: return null
        return files.firstOrNull { it.name.startsWith("cours_${id}_") }
    }

    fun getLocalFileForSujet(context: Context, id: Int, isCorrige: Boolean): File? {
        val prefix = if (isCorrige) "corrige_${id}_" else "sujet_${id}_"
        val dir = context.getExternalFilesDir("documents") ?: context.filesDir
        val files = dir.listFiles() ?: return null
        return files.firstOrNull { it.name.startsWith(prefix) }
    }

    fun deleteLocalFile(file: File): Boolean {
        return file.exists() && file.delete()
    }

    fun downloadCours(context: Context, id: Int, defaultTitle: String, cheminFichier: String? = null): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Progress(0))
        try {
            var response = if (!cheminFichier.isNullOrBlank()) {
                val cleanedUrl = cheminFichier.trim()
                service.downloadFromUrl(cleanedUrl)
            } else {
                service.downloadDocumentApi(type = "cours", id = id)
            }

            if (!response.isSuccessful) {
                // Fallback to direct document API if URL failed
                response = service.downloadDocumentApi(type = "cours", id = id)
                if (!response.isSuccessful) {
                    response = service.downloadCours(id)
                }
            }

            if (response.code() == 401 || response.code() == 403) {
                emit(DownloadStatus.Error("Authentification requise : Token absent ou invalide pour télécharger ce document."))
                return@flow
            }
            if (!response.isSuccessful) {
                emit(DownloadStatus.Error("Erreur de téléchargement : Code ${response.code()}"))
                return@flow
            }
            val body = response.body()
            if (body == null) {
                emit(DownloadStatus.Error("Fichier vide ou corrompu."))
                return@flow
            }

            val filename = getFilenameFromResponse(response, "cours_${id}_${sanitizeFilename(defaultTitle)}.pdf")
            val localName = "cours_${id}_$filename"
            val destDir = context.getExternalFilesDir("documents") ?: context.filesDir
            destDir.mkdirs()
            val destFile = File(destDir, localName)

            saveResponseBodyToFile(body, destFile) { progress, downloaded, total, speed ->
                emit(DownloadStatus.Progress(progress, downloaded, total, speed))
            }
            emit(DownloadStatus.Success(destFile))
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.localizedMessage ?: "Erreur réseau indéterminée"))
        }
    }.flowOn(Dispatchers.IO)

    fun downloadSujet(context: Context, id: Int, isCorrige: Boolean, defaultTitle: String, cheminFichier: String? = null): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Progress(0))
        try {
            var response = if (!cheminFichier.isNullOrBlank()) {
                val cleanedUrl = cheminFichier.trim()
                service.downloadFromUrl(cleanedUrl)
            } else {
                service.downloadDocumentApi(
                    type = "sujet",
                    id = id,
                    corrige = if (isCorrige) true else null
                )
            }

            if (!response.isSuccessful) {
                response = service.downloadDocumentApi(
                    type = "sujet",
                    id = id,
                    corrige = if (isCorrige) true else null
                )
                if (!response.isSuccessful) {
                    response = service.downloadSujet(id, isCorrige)
                }
            }

            if (response.code() == 401 || response.code() == 403) {
                emit(DownloadStatus.Error("Authentification requise : Token absent ou invalide pour télécharger ce document."))
                return@flow
            }
            if (!response.isSuccessful) {
                emit(DownloadStatus.Error("Erreur : ${response.message()} (Code ${response.code()})"))
                return@flow
            }
            val body = response.body()
            if (body == null) {
                emit(DownloadStatus.Error("Fichier vide ou corrompu."))
                return@flow
            }

            val filePrefix = if (isCorrige) "corrige" else "sujet"
            val fallbackExt = "pdf"
            val filename = getFilenameFromResponse(response, "${filePrefix}_${id}_${sanitizeFilename(defaultTitle)}.$fallbackExt")
            val localName = "${filePrefix}_${id}_$filename"
            val destDir = context.getExternalFilesDir("documents") ?: context.filesDir
            destDir.mkdirs()
            val destFile = File(destDir, localName)

            saveResponseBodyToFile(body, destFile) { progress, downloaded, total, speed ->
                emit(DownloadStatus.Progress(progress, downloaded, total, speed))
            }
            emit(DownloadStatus.Success(destFile))
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.localizedMessage ?: "Erreur réseau indéterminée"))
        }
    }.flowOn(Dispatchers.IO)

    private fun getFilenameFromResponse(response: retrofit2.Response<*>, fallback: String): String {
        val contentDisposition = response.headers()["Content-Disposition"]
        if (!contentDisposition.isNullOrEmpty()) {
            val regex = "filename=\"?([^\";]+)\"?".toRegex()
            val match = regex.find(contentDisposition)
            if (match != null) {
                return sanitizeFilename(match.groupValues[1])
            }
        }
        return fallback
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private suspend fun saveResponseBodyToFile(
        body: ResponseBody,
        file: File,
        onProgress: suspend (progress: Int, downloaded: Long, total: Long, speedKBps: Double) -> Unit
    ) {
        val length = body.contentLength()
        val inputStream: InputStream = body.byteStream()
        val outputStream = FileOutputStream(file)
        val buffer = ByteArray(8192)
        var totalBytesRead: Long = 0
        var bytesRead: Int
        var lastEmittedProgress = -1

        val startTime = System.currentTimeMillis()
        var lastEmittedTime = startTime

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                val currentTime = System.currentTimeMillis()
                val elapsedTimeSec = (currentTime - startTime) / 1000.0
                val speedKBps = if (elapsedTimeSec > 0.05) (totalBytesRead / 1024.0) / elapsedTimeSec else 0.0
                val progress = if (length > 0) ((totalBytesRead * 100) / length).toInt() else 0

                if (currentTime - lastEmittedTime >= 100 || progress != lastEmittedProgress) {
                    lastEmittedProgress = progress
                    lastEmittedTime = currentTime
                    onProgress(progress, totalBytesRead, length, speedKBps)
                }
            }
            outputStream.flush()
        } finally {
            outputStream.close()
            inputStream.close()
        }
    }

    fun openFile(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "Le fichier est introuvable sur l'appareil.", Toast.LENGTH_SHORT).show()
                return
            }
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val extension = file.extension.lowercase()
            val mimeType = if (extension == "pdf") "application/pdf" else (MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/pdf")
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                val chooser = Intent.createChooser(intent, "Ouvrir le document PDF").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible d'ouvrir le fichier. Aucun lecteur compatible trouvé.", Toast.LENGTH_LONG).show()
        }
    }

    fun shareFile(context: Context, file: File, title: String? = null) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "Le fichier à partager est introuvable.", Toast.LENGTH_SHORT).show()
                return
            }
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/pdf"
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title ?: file.nameWithoutExtension)
                putExtra(Intent.EXTRA_TEXT, "Voici le document : ${title ?: file.nameWithoutExtension}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Partager le document via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible de partager le document : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareToGemini(
        context: Context,
        file: File,
        preferGeminiDirect: Boolean = true,
        onNotInstalled: (() -> Unit)? = null
    ) {
        com.example.utils.GeminiShareHelper.shareDocumentToGemini(
            context = context,
            file = file,
            preferGeminiDirect = preferGeminiDirect,
            onGeminiNotInstalled = onNotInstalled
        )
    }
}
