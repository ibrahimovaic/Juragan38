package com.juragan38.pos.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Informasi versi aplikasi yang sedang terpasang di HP. */
data class InstalledVersion(
    val versionName: String,
    val versionCode: Long,
    val installedAt: Long
)

/** Info pembaruan yang tersedia di server. */
data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val fallbackUrl: String = "",
    val notes: String = "",
    val force: Boolean = false
) {
    fun apkUrls(): List<String> = listOf(apkUrl, fallbackUrl).filter { it.isNotBlank() }
}

/** Hasil pengecekan pembaruan. */
sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

/**
 * Pengecekan versi + unduh APK + pasang.
 *
 * Sumber informasi pembaruan: file `update.json` di repositori GitHub publik
 * (lihat [MANIFEST_URL]). Setiap rilis baru cukup ubah `versionCode` di file
 * tersebut dan unggah APK-nya ke `apk/JURAGAN-38.apk`.
 *
 * Catatan: Android tidak mengizinkan pemasangan benar-benar otomatis untuk
 * aplikasi biasa (bukan dari Play Store). Aplikasi mengunduh APK lalu
 * menampilkan layar instalasi Android, dan pengguna cukup menekan "Instal".
 */
object UpdateChecker {

    const val MANIFEST_URL =
        "https://raw.githubusercontent.com/ibrahimovaic/Juragan38/main/update.json"

    private const val APK_FILE_NAME = "JURAGAN-38-update.apk"
    private const val CONNECT_TIMEOUT = 15_000
    private const val READ_TIMEOUT = 30_000

    // ------------------------------------------------------------- versi

    fun installedVersion(context: Context): InstalledVersion = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        InstalledVersion(
            versionName = info.versionName ?: "-",
            versionCode = code,
            installedAt = info.lastUpdateTime
        )
    }.getOrDefault(InstalledVersion("-", 0L, 0L))

    // ------------------------------------------------------------- cek

    suspend fun check(currentVersionCode: Long): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            try {
                // Tambah query unik agar CDN GitHub tidak menyajikan file lama
                // tepat setelah rilis baru di-push.
                val json = fetchText("$MANIFEST_URL?t=${System.currentTimeMillis()}")
                val obj = JSONObject(json)
                val code = obj.optLong("versionCode", 0L)
                val apkUrl = obj.optString("apkUrl").trim()
                if (apkUrl.isBlank()) {
                    return@withContext UpdateCheckResult.Failed(
                        "Informasi pembaruan tidak valid (apkUrl kosong)."
                    )
                }
                if (code <= currentVersionCode) {
                    return@withContext UpdateCheckResult.UpToDate
                }
                UpdateCheckResult.Available(
                    UpdateInfo(
                        versionCode = code,
                        versionName = obj.optString("versionName")
                            .ifBlank { code.toString() },
                        apkUrl = apkUrl,
                        fallbackUrl = obj.optString("apkUrlFallback").trim(),
                        notes = obj.optString("notes").trim(),
                        force = obj.optBoolean("force", false)
                    )
                )
            } catch (e: Exception) {
                UpdateCheckResult.Failed(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Gagal memeriksa pembaruan. Periksa koneksi internet."
                )
            }
        }

    // --------------------------------------------------------- unduh

    /**
     * Unduh APK pembaruan ke cache. [onProgress] menerima 0..100
     * (0 = ukuran tidak diketahui, progress bar jadi animasi).
     */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, APK_FILE_NAME)

        var lastError: Exception? = null
        for (url in info.apkUrls()) {
            try {
                downloadTo(url, target, onProgress)
                if (!looksLikeApk(target)) {
                    target.delete()
                    throw IOException("File yang diunduh bukan APK.")
                }
                onProgress(100)
                return@withContext target
            } catch (e: Exception) {
                lastError = e
                target.delete()
            }
        }
        throw lastError ?: IOException("Gagal mengunduh pembaruan.")
    }

    private fun downloadTo(url: String, target: File, onProgress: (Int) -> Unit) {
        val conn = openConnection(url)
        try {
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = (downloaded * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun looksLikeApk(file: File): Boolean {
        if (!file.exists() || file.length() < 1024) return false
        return runCatching {
            file.inputStream().use { input ->
                val head = ByteArray(2)
                input.read(head) == 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
            }
        }.getOrDefault(false)
    }

    // --------------------------------------------------------- pasang

    /** Apakah aplikasi sudah diizinkan memasang APK dari luar toko aplikasi. */
    fun canInstall(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /** Buka layar izin "Instal aplikasi tidak dikenal" untuk aplikasi ini. */
    fun openInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Tampilkan layar instalasi Android untuk APK yang sudah diunduh. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    // -------------------------------------------------------- helper

    fun formatBytes(bytes: Long): String = when {
        bytes <= 0 -> "-"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    private fun fetchText(url: String): String {
        val conn = openConnection(url)
        return try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("Server mengembalikan kode $code.")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        var current = URL(url)
        var redirects = 0
        while (true) {
            val conn = (current.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Juragan38POS-Android")
                setRequestProperty("Cache-Control", "no-cache")
            }
            val code = conn.responseCode
            val location = conn.getHeaderField("Location")
            val isRedirect = code in 300..399 && !location.isNullOrBlank()
            if (isRedirect && redirects < 5) {
                conn.disconnect()
                current = URL(current, location)
                redirects++
                continue
            }
            if (code !in 200..299) {
                conn.disconnect()
                throw IOException("Server mengembalikan kode $code.")
            }
            return conn
        }
    }
}
