package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
  @Query("SELECT * FROM level_progress ORDER BY levelId ASC")
  fun observeAllLevelProgress(): Flow<List<LevelProgressEntity>>

  @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
  suspend fun getLevelProgress(levelId: Int): LevelProgressEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertLevelProgress(progress: LevelProgressEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertInitialLevels(levels: List<LevelProgressEntity>)

  @Query("SELECT * FROM high_scores ORDER BY score DESC, achievedAtMillis DESC LIMIT 30")
  fun observeTopHighScores(): Flow<List<HighScoreEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHighScore(score: HighScoreEntity)

  @Query("SELECT * FROM player_stats WHERE id = 1")
  fun observePlayerStats(): Flow<PlayerStatsEntity?>

  @Query("SELECT * FROM player_stats WHERE id = 1")
  suspend fun getPlayerStats(): PlayerStatsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPlayerStats(stats: PlayerStatsEntity)

  @Query("DELETE FROM high_scores")
  suspend fun clearHighScores()

  @Query("DELETE FROM level_progress")
  suspend fun clearLevelProgress()
}
