package com.example.compat

import android.os.Build

data class SystemAbiInfo(
    val is64BitOnly: Boolean,
    val primaryAbi: String,
    val supportedAbis: List<String>,
    val supported64BitAbis: List<String>,
    val supported32BitAbis: List<String>,
    val osArch: String,
    val androidVersion: String,
    val apiLevel: Int,
    val compatibilityBridgeStatus: String
)

object AbiDetector {

    fun getSystemAbiInfo(): SystemAbiInfo {
        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        val supported64Bit = Build.SUPPORTED_64_BIT_ABIS.toList()
        val supported32Bit = Build.SUPPORTED_32_BIT_ABIS.toList()

        // Modern 64-bit-only devices (Pixel 7+, Android 14+ pure 64-bit configs) have no 32-bit ABIs or block them in zygote
        val is64BitOnly = supported32Bit.isEmpty() || (supported64Bit.isNotEmpty() && !supportedAbis.contains("armeabi-v7a"))

        val primaryAbi = if (supportedAbis.isNotEmpty()) supportedAbis[0] else "unknown"
        val osArch = System.getProperty("os.arch") ?: "unknown"

        val bridgeStatus = if (is64BitOnly) {
            "Active (ARM32-to-ARM64 Translation Sandboxing Engaged)"
        } else {
            "Ready (Direct Execution with Sandboxed Isolation)"
        }

        return SystemAbiInfo(
            is64BitOnly = is64BitOnly,
            primaryAbi = primaryAbi,
            supportedAbis = supportedAbis,
            supported64BitAbis = supported64Bit,
            supported32BitAbis = supported32Bit,
            osArch = osArch,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = Build.VERSION.SDK_INT,
            compatibilityBridgeStatus = bridgeStatus
        )
    }
}
