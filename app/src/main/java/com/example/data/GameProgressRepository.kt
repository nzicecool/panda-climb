package com.example.data

import com.example.game.engine.LevelCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameProgressRepository(private val dao: GameDao) {
  val levelProgress: Flow<List<LevelProgressEntity>> = dao.observeAllLevelProgress()
  val topHighScores: Flow<List<HighScoreEntity>> = dao.observeTopHighScores()
  val playerStats: Flow<PlayerStatsEntity> = dao.observePlayerStats().map { it ?: PlayerStatsEntity() }

  suspend fun ensureSeeded() {
    val existing = dao.getPlayerStats()
    if (existing == null) {
      dao.upsertPlayerStats(PlayerStatsEntity())
    }
    val initialLevels = LevelCatalog.levels.map { level ->
      LevelProgressEntity(
        levelId = level.id,
        unlocked = level.id == 1,
        totalShootsAvailable = level.collectibles.size,
      )
    }
    dao.insertInitialLevels(initialLevels)
  }

  suspend fun recordLevelAttemptStats(
    scoreEarned: Int,
    barrelsSmashed: Int,
    barrelsJumped: Int,
    powerUpsCollected: Int,
  ) {
    val current = dao.getPlayerStats() ?: PlayerStatsEntity()
    dao.upsertPlayerStats(
      current.copy(
        totalScore = current.totalScore + scoreEarned.coerceAtLeast(0),
        barrelsSmashed = current.barrelsSmashed + barrelsSmashed.coerceAtLeast(0),
        barrelsJumped = current.barrelsJumped + barrelsJumped.coerceAtLeast(0),
        powerUpsCollected = current.powerUpsCollected + powerUpsCollected.coerceAtLeast(0),
      )
    )
  }

  suspend fun recordLevelCleared(
    levelId: Int,
    levelTitle: String,
    score: Int,
    stars: Int,
    durationSeconds: Int,
    shootsCollected: Int,
    totalShoots: Int,
    maxCombo: Int,
  ) {
    val now = System.currentTimeMillis()
    val existing = dao.getLevelProgress(levelId) ?: LevelProgressEntity(levelId = levelId, unlocked = true)
    val bestTime = if (existing.bestTimeSeconds == 0) durationSeconds else minOf(existing.bestTimeSeconds, durationSeconds)
    dao.upsertLevelProgress(
      existing.copy(
        unlocked = true,
        completed = true,
        bestScore = maxOf(existing.bestScore, score),
        bestStars = maxOf(existing.bestStars, stars),
        bestTimeSeconds = bestTime,
        bestShootsCollected = maxOf(existing.bestShootsCollected, shootsCollected),
        totalShootsAvailable = totalShoots,
        updatedAtMillis = now,
      )
    )

    // Unlock the next level sequentially
    val nextLevel = LevelCatalog.levels.firstOrNull { it.id == levelId + 1 }
    if (nextLevel != null) {
      val nextExisting = dao.getLevelProgress(nextLevel.id)
        ?: LevelProgressEntity(levelId = nextLevel.id, totalShootsAvailable = nextLevel.collectibles.size)
      if (!nextExisting.unlocked) {
        dao.upsertLevelProgress(nextExisting.copy(unlocked = true, updatedAtMillis = now))
      }
    }

    val stats = dao.getPlayerStats() ?: PlayerStatsEntity()
    dao.insertHighScore(
      HighScoreEntity(
        playerName = stats.playerNickname,
        levelId = levelId,
        levelTitle = levelTitle,
        score = score,
        stars = stars,
        maxCombo = maxCombo,
        durationSeconds = durationSeconds,
        achievedAtMillis = now,
      )
    )
  }

  suspend fun setSoundEnabled(enabled: Boolean) {
    val current = dao.getPlayerStats() ?: PlayerStatsEntity()
    dao.upsertPlayerStats(current.copy(soundEnabled = enabled))
  }

  suspend fun setPracticeUnlockAll(enabled: Boolean) {
    val current = dao.getPlayerStats() ?: PlayerStatsEntity()
    dao.upsertPlayerStats(current.copy(practiceUnlockAll = enabled))
  }

  suspend fun updateNickname(nickname: String) {
    val cleaned = nickname.trim().take(20).ifBlank { "Bamboo Hero" }
    val current = dao.getPlayerStats() ?: PlayerStatsEntity()
    dao.upsertPlayerStats(current.copy(playerNickname = cleaned))
  }

  suspend fun resetAllProgress() {
    dao.clearHighScores()
    dao.clearLevelProgress()
    dao.upsertPlayerStats(PlayerStatsEntity())
    ensureSeeded()
  }
}
