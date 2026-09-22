package com.example.data.repository

import com.example.data.db.GameDao
import com.example.data.db.GameEntity
import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {

    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    fun getGameById(id: Long): Flow<GameEntity?> = gameDao.getGameById(id)

    suspend fun getGameByPackage(packageName: String): GameEntity? = gameDao.getGameByPackage(packageName)

    suspend fun insertGame(game: GameEntity): Long = gameDao.insertGame(game)

    suspend fun updateGame(game: GameEntity) = gameDao.updateGame(game)

    suspend fun deleteGame(game: GameEntity) = gameDao.deleteGame(game)

    suspend fun deleteGameById(id: Long) = gameDao.deleteGameById(id)

    suspend fun updatePlaySession(id: Long, timestamp: Long, addedMinutes: Int) =
        gameDao.updatePlaySession(id, timestamp, addedMinutes)

    suspend fun updateHighScore(id: Long, score: Int) =
        gameDao.updateHighScore(id, score)

    suspend fun seedBuiltinDemoGamesIfEmpty() {
        val count = gameDao.getGamesCount()
        if (count == 0) {
            val demos = listOf(
                GameEntity(
                    packageName = "com.retro.flappy32",
                    title = "Flappy Bird 32",
                    versionName = "1.3 (Classic 2014)",
                    targetSdk = 19,
                    abiType = "armeabi-v7a",
                    isPure32Bit = true,
                    apkPath = "builtin://flappy32",
                    nativeLibsCount = 2,
                    nativeLibsList = "libgameplay.so, libretro_audio.so",
                    virtualAndroidVersion = "Android 4.4 (KitKat)",
                    memoryLimitMb = 512,
                    isBuiltinDemo = true,
                    description = "Legendary 32-bit arcade classic compiled exclusively for ARMv7-a with native physics engine."
                ),
                GameEntity(
                    packageName = "com.arcade.spaceraider32",
                    title = "Space Raider 32",
                    versionName = "2.1.0",
                    targetSdk = 22,
                    abiType = "armeabi-v7a",
                    isPure32Bit = true,
                    apkPath = "builtin://spaceraider32",
                    nativeLibsCount = 3,
                    nativeLibsList = "libunity.so, libmain.so, libmono.so",
                    virtualAndroidVersion = "Android 7.0 (Nougat)",
                    memoryLimitMb = 1024,
                    isBuiltinDemo = true,
                    description = "Classic vertical space bullet-hell built on 32-bit ARM Unity engine with neon particle shaders."
                ),
                GameEntity(
                    packageName = "com.puzzle.pocket2048",
                    title = "Pocket 2048 32-bit",
                    versionName = "1.0.4",
                    targetSdk = 21,
                    abiType = "armeabi",
                    isPure32Bit = true,
                    apkPath = "builtin://pocket2048",
                    nativeLibsCount = 1,
                    nativeLibsList = "libcocos2djs.so",
                    virtualAndroidVersion = "Android 5.1 (Lollipop)",
                    memoryLimitMb = 512,
                    isBuiltinDemo = true,
                    description = "Vintage 32-bit arithmetic tile merger with low-latency touch grid and undo states."
                ),
                GameEntity(
                    packageName = "com.retro.pixeldungeon32",
                    title = "Pixel Dungeon 32",
                    versionName = "1.7.5",
                    targetSdk = 23,
                    abiType = "armeabi-v7a",
                    isPure32Bit = true,
                    apkPath = "builtin://pixeldungeon32",
                    nativeLibsCount = 2,
                    nativeLibsList = "libgdx.so, libgdx-box2d.so",
                    virtualAndroidVersion = "Android 6.0 (Marshmallow)",
                    memoryLimitMb = 1024,
                    isBuiltinDemo = true,
                    description = "Traditional rogue-like dungeon crawler with 32-bit LibGDX physics and procedural dungeon levels."
                )
            )
            gameDao.insertAll(demos)
        }
    }
}
