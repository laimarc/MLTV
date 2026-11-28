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

    /** Vérif au démarrage : réessaie quelques fois (la release vient parfois d'être publiée). */
    fun checkAsync(activity: Activity) {
        checkInternal(activity, attempts = 3, manual = false)
    }

    /** Vérif manuelle (bouton Réglages) : avec retour visible à l'utilisateur. */
    fun checkManual(activity: Activity) {
        Toast.makeText(activity, "Recherche de mise à jour…", Toast.LENGTH_SHORT).show()
        checkInternal(activity, attempts = 2, manual = true)
    }

    private fun checkInternal(activity: Activity, attempts: Int, manual: Boolean) {
        Thread {
            var found = false
            var netError = false
            for (i in 0 until attempts) {
                if (i > 0) try { Thread.sleep(6000) } catch (_: InterruptedException) {}
                try {
                    val json = JSONObject(Http.get("$API_LATEST?t=${System.currentTimeMillis()}", noCache = true))
                    val tag = json.optString("tag_name", "") // "v1.<versionCode>"
                    val remote = Regex("(\\d+)\\s*$").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    var url = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (j in 0 until assets.length()) {
                            val a = assets.getJSONObject(j)
                            if (a.optString("name").endsWith(".apk")) {
                                url = a.optString("browser_download_url"); break
                            }
                        }
                    }
                    netError = false
                    if (remote > BuildConfig.VERSION_CODE && url.isNotEmpty()) {
                        found = true
                        activity.runOnUiThread { prompt(activity, url) }
                        break
                    }
                } catch (_: Exception) {
                    netError = true
                }
            }
            if (!found && manual) {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        if (netError) "Vérification impossible (réseau)." else "MLTV est à jour.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
        if (!file.exists() || file.length() < 500_000L) {
            Toast.makeText(activity, "Fichier de mise à jour invalide. Réessaie.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Impossible de lancer l'installation : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
