package com.example.compat

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ApkAnalysisResult(
    val isValid: Boolean,
    val packageName: String,
    val appTitle: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val supportedAbis: List<String>,
    val nativeLibraries: List<String>,
    val isPure32Bit: Boolean,
    val is64BitSupported: Boolean,
    val primaryAbi: String,
    val apkFilePath: String,
    val iconFilePath: String?,
    val dexFilesCount: Int,
    val totalSizeMb: Double,
    val permissions: List<String>,
    val error: String? = null
)

object ApkInspector {
    private const val TAG = "ApkInspector"

    fun inspectAndImportApk(context: Context, uri: Uri): ApkAnalysisResult {
        return try {
            val contentResolver = context.contentResolver
            val tempDir = File(context.cacheDir, "apk_temp")
            if (!tempDir.exists()) tempDir.mkdirs()

            val tempApk = File(tempDir, "temp_${System.currentTimeMillis()}.apk")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempApk).use { output ->
                    input.copyTo(output)
                }
            } ?: return errorResult("Unable to read selected APK file")

            val sizeMb = tempApk.length().toDouble() / (1024 * 1024)

            // Inspect ZIP entries for ABIs and .so native libraries
            val abisFound = mutableSetOf<String>()
            val nativeLibs = mutableListOf<String>()
            var dexCount = 0

            ZipInputStream(tempApk.inputStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.endsWith(".dex")) {
                        dexCount++
                    } else if (name.startsWith("lib/")) {
                        val parts = name.split("/")
                        if (parts.size >= 3) {
                            val abi = parts[1]
                            val libName = parts.last()
                            abisFound.add(abi)
                            if (libName.endsWith(".so") && !nativeLibs.contains(libName)) {
                                nativeLibs.add(libName)
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            val has32Bit = abisFound.contains("armeabi-v7a") || abisFound.contains("armeabi") || abisFound.contains("x86")
            val has64Bit = abisFound.contains("arm64-v8a") || abisFound.contains("x86_64")
            val isPure32Bit = has32Bit && !has64Bit

            val primaryAbi = when {
                abisFound.contains("armeabi-v7a") -> "armeabi-v7a"
                abisFound.contains("armeabi") -> "armeabi"
                abisFound.contains("arm64-v8a") -> "arm64-v8a"
                abisFound.contains("x86") -> "x86"
                abisFound.contains("x86_64") -> "x86_64"
                else -> "DEX (Universal)"
            }

            // Extract package info via PackageManager if possible
            val pm = context.packageManager
            val pkgInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageArchiveInfo(
                        tempApk.absolutePath,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(tempApk.absolutePath, PackageManager.GET_PERMISSIONS)
                }
            } catch (e: Exception) {
                null
            }

            val packageName = pkgInfo?.packageName ?: "app.game.${tempApk.nameWithoutExtension.lowercase()}"
            val versionName = pkgInfo?.versionName ?: "1.0.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo?.longVersionCode ?: 1L
            } else {
                @Suppress("DEPRECATION")
                pkgInfo?.versionCode?.toLong() ?: 1L
            }

            val targetSdk = pkgInfo?.applicationInfo?.targetSdkVersion ?: 22
            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pkgInfo?.applicationInfo?.minSdkVersion ?: 16
            } else {
                16
            }

            val appTitle = try {
                pkgInfo?.applicationInfo?.loadLabel(pm)?.toString()
                    ?: tempApk.nameWithoutExtension.replace(Regex("[_-]"), " ").capitalizeWords()
            } catch (e: Exception) {
                tempApk.nameWithoutExtension.capitalizeWords()
            }

            val permissions = pkgInfo?.requestedPermissions?.toList() ?: emptyList()

            // Prepare destination in sandbox storage
            val sandboxApkDir = File(context.filesDir, "sandbox/apks/$packageName")
            if (!sandboxApkDir.exists()) sandboxApkDir.mkdirs()

            val finalApk = File(sandboxApkDir, "base.apk")
            tempApk.copyTo(finalApk, overwrite = true)
            tempApk.delete()

            // Extract native libraries to sandbox lib directory
            val sandboxLibDir = File(sandboxApkDir, "lib/$primaryAbi")
            if (!sandboxLibDir.exists()) sandboxLibDir.mkdirs()

            extractLibs(finalApk, primaryAbi, sandboxLibDir)

            // Extract App Icon
            val iconFile = File(sandboxApkDir, "icon.png")
            var iconPath: String? = null
            try {
                pkgInfo?.applicationInfo?.sourceDir = finalApk.absolutePath
                pkgInfo?.applicationInfo?.publicSourceDir = finalApk.absolutePath
                val iconDrawable = pkgInfo?.applicationInfo?.loadIcon(pm)
                if (iconDrawable != null) {
                    val bitmap = drawableToBitmap(iconDrawable)
                    FileOutputStream(iconFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    }
                    iconPath = iconFile.absolutePath
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract icon from APK: ${e.message}")
            }

            ApkAnalysisResult(
                isValid = true,
                packageName = packageName,
                appTitle = appTitle,
                versionName = versionName,
                versionCode = versionCode,
                minSdk = minSdk,
                targetSdk = targetSdk,
                supportedAbis = abisFound.toList(),
                nativeLibraries = nativeLibs,
                isPure32Bit = isPure32Bit || !has64Bit,
                is64BitSupported = has64Bit,
                primaryAbi = primaryAbi,
                apkFilePath = finalApk.absolutePath,
                iconFilePath = iconPath,
                dexFilesCount = dexCount,
                totalSizeMb = String.format("%.2f", sizeMb).toDoubleOrNull() ?: sizeMb,
                permissions = permissions
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error importing APK", e)
            errorResult("Failed to inspect APK: ${e.localizedMessage ?: "Invalid file format"}")
        }
    }

    private fun extractLibs(apkFile: File, abi: String, destDir: File) {
        try {
            ZipInputStream(apkFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("lib/$abi/") && name.endsWith(".so")) {
                        val libFileName = name.substringAfterLast("/")
                        val destFile = File(destDir, libFileName)
                        FileOutputStream(destFile).use { out ->
                            zis.copyTo(out)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract native libs", e)
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun errorResult(msg: String): ApkAnalysisResult {
        return ApkAnalysisResult(
            isValid = false,
            packageName = "",
            appTitle = "",
            versionName = "",
            versionCode = 0,
            minSdk = 0,
            targetSdk = 0,
            supportedAbis = emptyList(),
            nativeLibraries = emptyList(),
            isPure32Bit = false,
            is64BitSupported = false,
            primaryAbi = "",
            apkFilePath = "",
            iconFilePath = null,
            dexFilesCount = 0,
            totalSizeMb = 0.0,
            permissions = emptyList(),
            error = msg
        )
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
