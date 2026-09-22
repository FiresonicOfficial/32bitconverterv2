package com.example.compat.sandbox

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

data class SyscallLogEntry(
    val timestamp: String,
    val type: LogType,
    val message: String,
    val details: String? = null
)

enum class LogType {
    SYSCALL_TRANSLATE,
    JNI_HOOK,
    MEMORY_REMAP,
    GL_ES_EMU,
    AUDIO_BRIDGE,
    SANDBOX_EVENT
}

data class BridgeMetrics(
    val syscallsPerSec: Int = 0,
    val totalSyscallsTranslated: Long = 0,
    val activeMemoryMappedMb: Double = 0.0,
    val hookedJniSymbolsCount: Int = 0,
    val activeEglSurface: String = "720x1280 (EGL 1.4 Virtual)"
)

class CompatibilityBridge(
    val config: VirtualContainerConfig
) {
    private val _logs = MutableStateFlow<List<SyscallLogEntry>>(emptyList())
    val logs: StateFlow<List<SyscallLogEntry>> = _logs.asStateFlow()

    private val _metrics = MutableStateFlow(BridgeMetrics())
    val metrics: StateFlow<BridgeMetrics> = _metrics.asStateFlow()

    private val totalSyscalls = AtomicLong(0)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // 32-bit ARM EABI to 64-bit AArch64 syscall table mappings
    private val arm32SyscallMap = mapOf(
        3 to Pair(63, "sys_read"),
        4 to Pair(64, "sys_write"),
        5 to Pair(56, "sys_open -> sys_openat"),
        6 to Pair(57, "sys_close"),
        19 to Pair(62, "sys_lseek"),
        20 to Pair(172, "sys_getpid"),
        54 to Pair(29, "sys_ioctl"),
        125 to Pair(226, "sys_mprotect"),
        140 to Pair(62, "sys__llseek"),
        162 to Pair(101, "sys_nanosleep"),
        192 to Pair(222, "sys_mmap2 -> sys_mmap"),
        240 to Pair(98, "sys_futex"),
        263 to Pair(113, "sys_clock_gettime")
    )

    init {
        log(
            LogType.SANDBOX_EVENT,
            "ARM32 Compatibility Layer initialized",
            "Target ABI: ${config.targetAbi} | Mode: ${config.translationMode.displayName} | Spoofed OS: ${config.virtualAndroidVersion}"
        )
        log(
            LogType.MEMORY_REMAP,
            "Virtual 32-bit address space mapped: 0x00000000 - 0xC0000000 (3GB user space)",
            "Allocated 32-bit address translation page tables with 4KB granularity"
        )
        simulateInitialJniHooks()
    }

    private fun simulateInitialJniHooks() {
        val hooks = listOf(
            "Java_com_epicgames_ue4_GameActivity_nativeInit",
            "Java_org_cocos2dx_lib_Cocos2dxRenderer_nativeRender",
            "Java_com_unity3d_player_UnityPlayer_nativeRender",
            "Java_org_libsdl_app_SDLActivity_nativeInit"
        )
        hooks.forEach { hook ->
            log(LogType.JNI_HOOK, "Hooked ARM32 native function: $hook", "Remapped to 64-bit trampoline wrapper")
        }
        _metrics.value = _metrics.value.copy(hookedJniSymbolsCount = hooks.size)
    }

    fun log(type: LogType, message: String, details: String? = null) {
        val entry = SyscallLogEntry(
            timestamp = timeFormat.format(Date()),
            type = type,
            message = message,
            details = details
        )
        val currentList = _logs.value.toMutableList()
        if (currentList.size > 150) {
            currentList.removeAt(0)
        }
        currentList.add(entry)
        _logs.value = currentList
    }

    fun recordSyscallBatch(count: Int) {
        val total = totalSyscalls.addAndGet(count.toLong())
        val randomSyscall = arm32SyscallMap.entries.random()
        val arm32Num = randomSyscall.key
        val arm64Mapping = randomSyscall.value

        if (Random.nextFloat() < 0.15f) {
            log(
                LogType.SYSCALL_TRANSLATE,
                "ARM32 [nr $arm32Num] -> ARM64 [nr ${arm64Mapping.first}] : ${arm64Mapping.second}",
                "Pointer 0x${Integer.toHexString(Random.nextInt(0x10000000, 0x7FFFFFFF))} remapped to host 64-bit page"
            )
        }

        _metrics.value = _metrics.value.copy(
            syscallsPerSec = count * 60,
            totalSyscallsTranslated = total,
            activeMemoryMappedMb = 142.5 + (total % 50) * 0.4
        )
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
