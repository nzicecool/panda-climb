package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.ArcadeSoundEngine
import com.example.data.AppDatabase
import com.example.data.GameProgressRepository
import com.example.ui.AppRoute
import com.example.ui.PandaClimbViewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.theme.PandaClimbTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getInstance(applicationContext)
    val repository = GameProgressRepository(database.gameDao())
    val soundEngine = ArcadeSoundEngine()

    setContent {
      PandaClimbTheme {
        val viewModel: PandaClimbViewModel = viewModel(
          factory = PandaClimbViewModel.Factory(repository, soundEngine)
        )
        PandaClimbApp(viewModel)
      }
    }
  }
}

@Composable
fun PandaClimbApp(viewModel: PandaClimbViewModel) {
  val route by viewModel.route.collectAsStateWithLifecycle()
  val stats by viewModel.playerStats.collectAsStateWithLifecycle()
  val progress by viewModel.levelProgress.collectAsStateWithLifecycle()
  val highScores by viewModel.highScores.collectAsStateWithLifecycle()
  val hud by viewModel.hudState.collectAsStateWithLifecycle()

  when (val current = route) {
    AppRoute.Home -> {
      HomeScreen(
        stats = stats,
        progressList = progress,
        onQuickPlay = { levelId -> viewModel.startLevel(levelId) },
        onOpenLevelSelect = { viewModel.navigateTo(AppRoute.LevelSelect) },
        onOpenLeaderboard = { viewModel.navigateTo(AppRoute.Leaderboard) },
        onToggleSound = { viewModel.toggleSound() },
        onUpdateNickname = { viewModel.updateNickname(it) },
      )
    }

    AppRoute.LevelSelect -> {
      LevelSelectScreen(
        progressList = progress,
        practiceUnlockAll = stats.practiceUnlockAll,
        onTogglePracticeUnlock = { viewModel.setPracticeUnlockAll(it) },
        onSelectLevel = { levelId -> viewModel.startLevel(levelId) },
        onBack = { viewModel.navigateTo(AppRoute.Home) },
      )
    }

    is AppRoute.Game -> {
      GameScreen(
        hud = hud,
        soundEnabled = stats.soundEnabled,
        inputState = viewModel.inputState,
        snapshotProvider = { viewModel.currentRenderSnapshot() },
        onFrameStep = { viewModel.stepGameFrame() },
        onTogglePause = { viewModel.togglePause() },
        onAutoPause = { viewModel.setPaused(true) },
        onToggleSound = { viewModel.toggleSound() },
        onRetry = { viewModel.startLevel(current.levelId) },
        onNextLevel = { nextId -> viewModel.startLevel(nextId) },
        onExitToLevels = { viewModel.navigateTo(AppRoute.LevelSelect) },
      )
    }

    AppRoute.Leaderboard -> {
      LeaderboardScreen(
        stats = stats,
        highScores = highScores,
        onResetProgress = { viewModel.resetAllProgress() },
        onBack = { viewModel.navigateTo(AppRoute.Home) },
      )
    }
  }
}
