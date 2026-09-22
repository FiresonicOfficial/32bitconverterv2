package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY lastPlayed DESC, installDate DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    fun getGameById(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE packageName = :packageName LIMIT 1")
    suspend fun getGameByPackage(packageName: String): GameEntity?

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGamesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Query("UPDATE games SET lastPlayed = :timestamp, playTimeMinutes = playTimeMinutes + :addedMinutes WHERE id = :id")
    suspend fun updatePlaySession(id: Long, timestamp: Long, addedMinutes: Int)

    @Query("UPDATE games SET highScore = :score WHERE id = :id AND :score > highScore")
    suspend fun updateHighScore(id: Long, score: Int)
}
