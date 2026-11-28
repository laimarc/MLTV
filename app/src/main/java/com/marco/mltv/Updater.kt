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

    // API GitHub Releases : non mise en cache comme GitHub Pages, et l'APK a un lien
    // unique par build (tag v1.N) donc jamais d'ancienne version servie par le cache.
    private const val API_LATEST = "https://api.github.com/repos/laimarc/MLTV/releases/latest"

    fun checkAsync(activity: Activity) {
        Thread {
            try {
                val json = JSONObject(Http.get("$API_LATEST?t=${System.currentTimeMillis()}", noCache = true))
                // tag_name attendu : "v1.<versionCode>"
                val tag = json.optString("tag_name", "")
                val remote = Regex("(\\d+)\\s*$").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                // retrouver l'APK dans les assets de la release
                var url = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk")) {
                            url = a.optString("browser_download_url"); break
                        }
                    }
                }
                if (remote > BuildConfig.VERSION_CODE && url.isNotEmpty()) {
                    activity.runOnUiThread { prompt(activity, url) }
                }
            } catch (_: Exception) {
                // pas de réseau / pas de release : on ignore
            }
        }.start()
    }

    private fun prompt(activity: Activity, url: String) {
        if (activity.isFinishing) return
        AlertDialog.Builder(activity, R.style.MLTVDialog)
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
