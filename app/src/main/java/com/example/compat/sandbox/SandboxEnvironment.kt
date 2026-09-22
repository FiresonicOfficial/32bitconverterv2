package com.example.compat.sandbox

import android.content.Context
import java.io.File

data class VirtualContainerConfig(
    val packageName: String,
    val appTitle: String,
    val virtualAndroidVersion: String = "Android 9.0 (Pie)",
    val spoofedApiLevel: Int = 28,
    val targetAbi: String = "armeabi-v7a",
    val memoryLimitMb: Int = 1024,
    val translationMode: TranslationMode = TranslationMode.DYNAMIC_JIT_TRANSLATION,
    val enableCrtShader: Boolean = false,
    val touchHaptics: Boolean = true,
    val virtualResolution: String = "1280x720 (HD 16:9)"
)

enum class TranslationMode(val displayName: String, val description: String) {
    DYNAMIC_JIT_TRANSLATION("ARM32 Dynamic JIT Bridge", "Translates ARMv7 instructions to ARM64 on-the-fly with cached blocks for high FPS"),
    ARM32_INTERPRETER("ARM32 Core Interpreter", "Safe software-level ARMv7 instruction interpreter for maximum compatibility"),
    SYSCALL_HOOK_EMULATOR("Native Syscall Hook Bridge", "Intercepts JNI & Linux system calls and remaps 32-bit pointers to 64-bit address space")
}

class SandboxEnvironment(
    private val context: Context,
    val config: VirtualContainerConfig
) {
    val containerRoot: File = File(context.filesDir, "sandbox/containers/${config.packageName}")
    val dataDir: File = File(containerRoot, "data")
    val filesDir: File = File(dataDir, "files")
    val cacheDir: File = File(dataDir, "cache")
    val sharedPrefsDir: File = File(dataDir, "shared_prefs")
    val databasesDir: File = File(dataDir, "databases")
    val nativeLibDir: File = File(containerRoot, "lib/${config.targetAbi}")

    init {
        createSandboxDirectories()
    }

    private fun createSandboxDirectories() {
        listOf(containerRoot, dataDir, filesDir, cacheDir, sharedPrefsDir, databasesDir, nativeLibDir)
            .forEach { dir ->
                if (!dir.exists()) dir.mkdirs()
            }
    }

    fun getUsedStorageMb(): Double {
        val bytes = getFolderSize(containerRoot)
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format("%.2f", mb).toDoubleOrNull() ?: mb
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += getFolderSize(child)
        }
        return size
    }

    fun clearVirtualData(): Boolean {
        return try {
            filesDir.deleteRecursively()
            cacheDir.deleteRecursively()
            sharedPrefsDir.deleteRecursively()
            databasesDir.deleteRecursively()
            createSandboxDirectories()
            true
        } catch (e: Exception) {
            false
        }
    }
}
