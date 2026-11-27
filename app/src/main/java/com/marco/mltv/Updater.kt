package com.marco.mltv

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

/** Mise à jour intégrée : compare la version en ligne et propose l'installation. */
object Updater {

    private const val VERSION_URL = "https://laimarc.github.io/MLTV/version.json"

    fun checkAsync(activity: Activity) {
        Thread {
            try {
                // paramètre anti-cache : force le CDN GitHub Pages à renvoyer la dernière version
                val json = JSONObject(Http.get("$VERSION_URL?t=${System.currentTimeMillis()}", noCache = true))
                val remote = json.optInt("versionCode", 0)
                var url = json.optString("url", "")
                if (remote > BuildConfig.VERSION_CODE && url.isNotEmpty()) {
                    // anti-cache aussi sur l'APK pour ne pas récupérer l'ancienne
                    url += if (url.contains("?")) "&t=${System.currentTimeMillis()}" else "?t=${System.currentTimeMillis()}"
                    val finalUrl = url
                    activity.runOnUiThread { prompt(activity, finalUrl) }
                }
            } catch (_: Exception) {
                // pas de réseau / pas de version : on ignore
            }
        }.start()
    }

    private fun prompt(activity: Activity, url: String) {
        if (activity.isFinishing) return
        AlertDialog.Builder(activity)
            .setTitle("Mise à jour disponible")
            .setMessage("Une nouvelle version de MLTV est disponible. L'installer maintenant ?")
            .setPositiveButton("Installer") { _, _ -> downloadAndInstall(activity, url) }
            .setNegativeButton("Plus tard", null)
            .show()
    }

    private fun downloadAndInstall(activity: Activity, url: String) {
        Toast.makeText(activity, "Téléchargement de la mise à jour…", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val dir = File(activity.getExternalFilesDir(null), "apk")
                dir.mkdirs()
                val file = File(dir, "MLTV.apk")
                Http.download(url, file)
                activity.runOnUiThread { install(activity, file) }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Échec du téléchargement : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun install(activity: Activity, file: File) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }
}
