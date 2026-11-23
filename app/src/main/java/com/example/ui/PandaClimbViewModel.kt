package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.ArcadeSoundEngine
import com.example.audio.SoundEffect
import com.example.data.GameProgressRepository
import com.example.data.HighScoreEntity
import com.example.data.LevelProgressEntity
import com.example.data.PlayerStatsEntity
import com.example.game.engine.GameHudState
import com.example.game.engine.GamePhase
import com.example.game.engine.GameSimulation
import com.example.game.engine.InputState
import com.example.game.engine.LevelCatalog
import com.example.game.engine.RenderSceneSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppRoute {
  data object Home : AppRoute
  data object LevelSelect : AppRoute
  data class Game(val levelId: Int) : AppRoute
  data object Leaderboard : AppRoute
}

class PandaClimbViewModel(
  private val repository: GameProgressRepository,
  val soundEngine: ArcadeSoundEngine,
) : ViewModel() {

  private val _route = MutableStateFlow<AppRoute>(AppRoute.Home)
  val route: StateFlow<AppRoute> = _route.asStateFlow()

  val levelProgress: StateFlow<List<LevelProgressEntity>> = repository.levelProgress
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val highScores: StateFlow<List<HighScoreEntity>> = repository.topHighScores
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val playerStats: StateFlow<PlayerStatsEntity> = repository.playerStats
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerStatsEntity())

  private val _hudState = MutableStateFlow(GameHudState())
  val hudState: StateFlow<GameHudState> = _hudState.asStateFlow()

  val inputState = InputState()

  private val simLock = Any()
  @Volatile private var simulation: GameSimulation? = null
  @Volatile private var latestSnapshot: RenderSceneSnapshot? = null
  private var hudFrameCounter = 0

  init {
    viewModelScope.launch {
      repository.ensureSeeded()
    }
    viewModelScope.launch {
      playerStats.collect { stats ->
        soundEngine.enabled = stats.soundEnabled
      }
    }
  }

  fun navigateTo(target: AppRoute) {
    if (target !is AppRoute.Game) {
      synchronized(simLock) {
        simulation = null
        latestSnapshot = null
        inputState.clearAll()
      }
    }
    _route.value = target
  }

  fun startLevel(levelId: Int) {
    val level = LevelCatalog.getLevel(levelId)
    synchronized(simLock) {
      inputState.clearAll()
      val sim = GameSimulation(
        level = level,
        soundEngine = soundEngine,
        onVictory = { victoryHud ->
          _hudState.value = victoryHud
          viewModelScope.launch {
            repository.recordLevelCleared(
              levelId = level.id,
              levelTitle = level.title,
              score = victoryHud.score,
              stars = victoryHud.earnedStars,
              durationSeconds = victoryHud.elapsedSeconds.coerceAtLeast(1),
              shootsCollected = victoryHud.shootsCollected,
              totalShoots = victoryHud.totalShoots,
              maxCombo = victoryHud.maxCombo,
            )
            repository.recordLevelAttemptStats(
              scoreEarned = victoryHud.score,
              barrelsSmashed = victoryHud.barrelsSmashed,
              barrelsJumped = victoryHud.barrelsJumped,
              powerUpsCollected = victoryHud.powerUpsCollected,
            )
          }
        },
        onGameOver = { overHud ->
          _hudState.value = overHud
          viewModelScope.launch {
            repository.recordLevelAttemptStats(
              scoreEarned = overHud.score,
              barrelsSmashed = overHud.barrelsSmashed,
              barrelsJumped = overHud.barrelsJumped,
              powerUpsCollected = overHud.powerUpsCollected,
            )
          }
        },
      )
      simulation = sim
      latestSnapshot = sim.buildRenderSnapshot()
      _hudState.value = sim.buildHudState()
      hudFrameCounter = 0
    }
    _route.value = AppRoute.Game(level.id)
  }

  fun stepGameFrame() {
    val sim = simulation ?: return
    synchronized(simLock) {
      if (simulation !== sim) return
      sim.step(inputState)
      latestSnapshot = sim.buildRenderSnapshot()
      hudFrameCounter++
      if (hudFrameCounter >= 3) {
        hudFrameCounter = 0
        val currentHud = _hudState.value
        if (currentHud.phase == GamePhase.PLAYING || currentHud.phase == GamePhase.PAUSED) {
          _hudState.value = sim.buildHudState()
        }
      }
    }
  }

  fun currentRenderSnapshot(): RenderSceneSnapshot? = latestSnapshot

  fun togglePause() {
    val sim = simulation ?: return
    synchronized(simLock) {
      val current = _hudState.value.phase
      if (current == GamePhase.PLAYING) {
        sim.setPaused(true)
        inputState.clearAll()
        _hudState.value = sim.buildHudState()
      } else if (current == GamePhase.PAUSED) {
        sim.setPaused(false)
        _hudState.value = sim.buildHudState()
      }
    }
  }

  fun setPaused(paused: Boolean) {
    val sim = simulation ?: return
    synchronized(simLock) {
      sim.setPaused(paused)
      if (paused) inputState.clearAll()
      val current = _hudState.value.phase
      if (current == GamePhase.PLAYING || current == GamePhase.PAUSED) {
        _hudState.value = sim.buildHudState()
      }
    }
  }

  fun toggleSound() {
    val next = !playerStats.value.soundEnabled
    soundEngine.enabled = next
    if (next) soundEngine.play(SoundEffect.COIN)
    viewModelScope.launch {
      repository.setSoundEnabled(next)
    }
  }

  fun setPracticeUnlockAll(enabled: Boolean) {
    viewModelScope.launch {
      repository.setPracticeUnlockAll(enabled)
    }
  }

  fun updateNickname(name: String) {
    viewModelScope.launch {
      repository.updateNickname(name)
    }
  }

  fun resetAllProgress() {
    viewModelScope.launch {
      repository.resetAllProgress()
    }
  }

  override fun onCleared() {
    super.onCleared()
    soundEngine.release()
  }

  class Factory(
    private val repository: GameProgressRepository,
    private val soundEngine: ArcadeSoundEngine,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return PandaClimbViewModel(repository, soundEngine) as T
    }
  }
}
