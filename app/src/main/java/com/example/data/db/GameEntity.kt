package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val title: String,
    val versionName: String,
    val targetSdk: Int,
    val abiType: String, // e.g. "armeabi-v7a", "armeabi", "arm64-v8a"
    val isPure32Bit: Boolean,
    val apkPath: String, // Path to local APK or "builtin://..."
    val iconUri: String? = null,
    val installDate: Long = System.currentTimeMillis(),
    val lastPlayed: Long = 0,
    val playTimeMinutes: Int = 0,
    val highScore: Int = 0,
    val nativeLibsCount: Int = 1,
    val nativeLibsList: String = "", // comma-separated .so files
    val virtualAndroidVersion: String = "Android 9.0 (Pie)",
    val memoryLimitMb: Int = 1024,
    val isBuiltinDemo: Boolean = false,
    val description: String = ""
)
