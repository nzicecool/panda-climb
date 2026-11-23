package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
  @PrimaryKey val levelId: Int,
  val unlocked: Boolean = false,
  val completed: Boolean = false,
  val bestScore: Int = 0,
  val bestStars: Int = 0,
  val bestTimeSeconds: Int = 0,
  val bestShootsCollected: Int = 0,
  val totalShootsAvailable: Int = 3,
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "high_scores")
data class HighScoreEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val playerName: String,
  val levelId: Int,
  val levelTitle: String,
  val score: Int,
  val stars: Int,
  val maxCombo: Int,
  val durationSeconds: Int,
  val achievedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
  @PrimaryKey val id: Int = 1,
  val playerNickname: String = "Bamboo Hero",
  val totalScore: Long = 0L,
  val barrelsSmashed: Int = 0,
  val barrelsJumped: Int = 0,
  val powerUpsCollected: Int = 0,
  val soundEnabled: Boolean = true,
  val practiceUnlockAll: Boolean = false,
)
