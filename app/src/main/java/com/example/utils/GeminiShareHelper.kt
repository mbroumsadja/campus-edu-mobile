package com.example.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Modèle représentant une application d'intelligence artificielle cible
 */
data class AiAppTarget(
    val id: String,
    val name: String,
    val packageName: String,
    val playStoreUrl: String
)

/**
 * Utilitaire pour le partage de documents locaux vers les applications d'IA
 * (Gemini, ChatGPT, Claude AI, DeepSeek) via Intent.ACTION_SEND + FileProvider.
 */
object GeminiShareHelper {

    // Packages officiels des applications IA
    const val GEMINI_PACKAGE_NAME = "com.google.android.apps.bard"
    const val CHATGPT_PACKAGE_NAME = "com.openai.chatgpt"
    const val CLAUDE_PACKAGE_NAME = "com.anthropic.claude"
    const val DEEPSEEK_PACKAGE_NAME = "com.deepseek.chat"

    val AI_APPS = listOf(
        AiAppTarget(
            id = "claude",
            name = "Claude AI",
            packageName = CLAUDE_PACKAGE_NAME,
            playStoreUrl = "https://play.google.com/store/apps/details?id=$CLAUDE_PACKAGE_NAME"
        ),
        AiAppTarget(
            id = "chatgpt",
            name = "ChatGPT",
            packageName = CHATGPT_PACKAGE_NAME,
            playStoreUrl = "https://play.google.com/store/apps/details?id=$CHATGPT_PACKAGE_NAME"
        ),
        AiAppTarget(
            id = "deepseek",
            name = "DeepSeek",
            packageName = DEEPSEEK_PACKAGE_NAME,
            playStoreUrl = "https://play.google.com/store/apps/details?id=$DEEPSEEK_PACKAGE_NAME"
        ),
        AiAppTarget(
            id = "gemini",
            name = "Gemini",
            packageName = GEMINI_PACKAGE_NAME,
            playStoreUrl = "https://play.google.com/store/apps/details?id=$GEMINI_PACKAGE_NAME"
        )
    )

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isGeminiInstalled(context: Context): Boolean = isAppInstalled(context, GEMINI_PACKAGE_NAME)

    fun shareDocumentToAiApp(
        context: Context,
        file: File,
        app: AiAppTarget,
        onNotInstalled: (() -> Unit)? = null
    ) {
        if (!file.exists()) {
            Toast.makeText(context, "Le document sélectionné est introuvable.", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = getMimeType(file)
        val fileUri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur FileProvider: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return
        }

        val installed = isAppInstalled(context, app.packageName)
        if (!installed) {
            if (onNotInstalled != null) {
                onNotInstalled()
                return
            } else {
                Toast.makeText(
                    context,
                    "${app.name} n'est pas installée. Redirection vers le sélecteur d'applications.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (installed) {
            sendIntent.setPackage(app.packageName)
            try {
                context.startActivity(sendIntent)
                return
            } catch (_: ActivityNotFoundException) {
                sendIntent.setPackage(null)
            }
        }

        // Chooser fallback
        val chooser = Intent.createChooser(sendIntent, "Ouvrir avec ${app.name}...").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Aucune application disponible pour partager ce fichier.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDocumentToGemini(
        context: Context,
        file: File,
        preferGeminiDirect: Boolean = true,
        onGeminiNotInstalled: (() -> Unit)? = null
    ) {
        val geminiApp = AI_APPS.first { it.id == "gemini" }
        shareDocumentToAiApp(
            context = context,
            file = file,
            app = geminiApp,
            onNotInstalled = onGeminiNotInstalled
        )
    }

    fun promptInstallApp(context: Context, app: AiAppTarget) {
        try {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${app.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(app.playStoreUrl)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun promptInstallGemini(context: Context) {
        val geminiApp = AI_APPS.first { it.id == "gemini" }
        promptInstallApp(context, geminiApp)
    }

    fun getMimeType(file: File): String {
        val ext = file.extension.lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt" -> "text/plain"
            "rtf" -> "application/rtf"
            "csv" -> "text/csv"
            "tsv" -> "text/tab-separated-values"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }
}
