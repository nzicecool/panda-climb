package com.example.ui.screens

import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.engine.GameHudState
import com.example.game.engine.GamePhase
import com.example.game.engine.InputState
import com.example.game.engine.LevelCatalog
import com.example.game.engine.RenderSceneSnapshot
import com.example.game.gl.Panda3DRenderer
import com.example.ui.theme.BambooGold
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyanShield
import com.example.ui.theme.JadePrimary
import com.example.ui.theme.SunsetAmber
import kotlin.math.ceil

@Composable
fun GameScreen(
  hud: GameHudState,
  soundEnabled: Boolean,
  inputState: InputState,
  snapshotProvider: () -> RenderSceneSnapshot?,
  onFrameStep: () -> Unit,
  onTogglePause: () -> Unit,
  onAutoPause: () -> Unit,
  onToggleSound: () -> Unit,
  onRetry: () -> Unit,
  onNextLevel: (Int) -> Unit,
  onExitToLevels: () -> Unit,
) {
  BackHandler {
    when (hud.phase) {
      GamePhase.PLAYING -> onTogglePause()
      GamePhase.PAUSED -> onTogglePause()
      GamePhase.VICTORY, GamePhase.GAME_OVER -> onExitToLevels()
    }
  }

  var glViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, glViewRef) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> glViewRef?.onResume()
        Lifecycle.Event.ON_PAUSE -> {
          glViewRef?.onPause()
          onAutoPause()
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      inputState.clearAll()
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .focusable()
      .onKeyEvent { event ->
        val isDown = event.type == KeyEventType.KeyDown
        val isUp = event.type == KeyEventType.KeyUp
        if (!isDown && !isUp) return@onKeyEvent false
        when (event.key) {
          Key.DirectionLeft, Key.A -> { inputState.left = isDown; true }
          Key.DirectionRight, Key.D -> { inputState.right = isDown; true }
          Key.DirectionUp, Key.W -> { inputState.up = isDown; true }
          Key.DirectionDown, Key.S -> { inputState.down = isDown; true }
          Key.Spacebar, Key.Z -> {
            if (isDown) inputState.triggerJumpPress() else inputState.releaseJump()
            true
          }
          else -> false
        }
      }
  ) {
    // Full-screen 3D OpenGL Viewport
    AndroidView(
      factory = { context ->
        GLSurfaceView(context).apply {
          setEGLContextClientVersion(2)
          setRenderer(Panda3DRenderer(snapshotProvider = snapshotProvider, onFrameStep = onFrameStep))
          renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
          glViewRef = this
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Top Arcade HUD
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left: Pause + Hearts
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            color = Color(0xC2101A24),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
          ) {
            IconButton(
              onClick = onTogglePause,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("pause_button"),
            ) {
              Icon(Icons.Rounded.Pause, contentDescription = "Pause game", tint = Color.White)
            }
          }

          Surface(
            color = Color(0xC2101A24),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              for (i in 1..3) {
                Icon(
                  imageVector = if (i <= hud.lives) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                  contentDescription = null,
                  tint = if (i <= hud.lives) CrimsonAccent else Color.White.copy(alpha = 0.35f),
                  modifier = Modifier.size(20.dp),
                )
              }
              if (hud.hasShield) {
                Spacer(Modifier.width(2.dp))
                Icon(
                  imageVector = Icons.Rounded.Shield,
                  contentDescription = "Shield active",
                  tint = CyanShield,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }

        // Center: Score & Combo
        Surface(
          color = Color(0xCC101A24),
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.5.dp, if (hud.combo > 1) BambooGold else Color.White.copy(alpha = 0.22f)),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "${hud.score}",
              style = MaterialTheme.typography.titleLarge,
              color = BambooGold,
              modifier = Modifier.testTag("hud_score"),
            )
            Text(
              text = if (hud.combo > 1) "COMBO x${hud.combo}!" else "LVL ${hud.levelId} • ${hud.elapsedSeconds}s",
              style = MaterialTheme.typography.labelSmall,
              color = if (hud.combo > 1) JadePrimary else Color.White.copy(alpha = 0.8f),
            )
          }
        }

        // Right: Sound toggle
        Surface(
          color = Color(0xC2101A24),
          shape = CircleShape,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        ) {
          IconButton(
            onClick = onToggleSound,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("game_sound_button"),
          ) {
            Icon(
              imageVector = if (soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
              contentDescription = if (soundEnabled) "Mute sound" else "Unmute sound",
              tint = if (soundEnabled) JadePrimary else Color.White.copy(alpha = 0.6f),
            )
          }
        }
      }

      // Secondary HUD Row: Collectibles, Rivets (if any) & Active Power-up Timers
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          HudChip(
            text = "SHOOTS ${hud.shootsCollected}/${hud.totalShoots}",
            color = BambooGold,
          )
          if (hud.totalRivets > 0) {
            HudChip(
              text = "RIVETS ${hud.rivetsStomped}/${hud.totalRivets}",
              color = SunsetAmber,
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (hud.hammerSecondsLeft > 0f) {
            PowerTimerChip(Icons.Rounded.Hardware, BambooGold, "${ceil(hud.hammerSecondsLeft).toInt()}s")
          }
          if (hud.doubleJumpSecondsLeft > 0f) {
            PowerTimerChip(Icons.Rounded.DoubleArrow, JadePrimary, "${ceil(hud.doubleJumpSecondsLeft).toInt()}s")
          }
          if (hud.speedSecondsLeft > 0f) {
            PowerTimerChip(Icons.Rounded.Bolt, CrimsonAccent, "${ceil(hud.speedSecondsLeft).toInt()}s")
          }
        }
      }

      // Floating Score / Skill Popups
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        hud.popups.takeLast(2).forEach { popup ->
          Surface(
            color = Color(0xCC0A1118),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, Color(popup.accentColorHex)),
          ) {
            Text(
              text = popup.text,
              style = MaterialTheme.typography.labelLarge,
              color = Color(popup.accentColorHex),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
          }
        }
      }
    }

    // Bottom Ergonomic Multi-Touch Arcade Controls
    if (hud.phase == GamePhase.PLAYING) {
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        // Left D-Pad Cluster (Climb + Move)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          HoldControlButton(
            icon = Icons.Rounded.KeyboardArrowUp,
            contentDescription = "Climb Up",
            accent = BambooGold,
            size = 56.dp,
            testTag = "btn_up",
            onPressChange = { inputState.up = it },
          )
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HoldControlButton(
              icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
              contentDescription = "Move Left",
              accent = Color.White,
              size = 64.dp,
              testTag = "btn_left",
              onPressChange = { inputState.left = it },
            )
            HoldControlButton(
              icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = "Move Right",
              accent = Color.White,
              size = 64.dp,
              testTag = "btn_right",
              onPressChange = { inputState.right = it },
            )
          }
          HoldControlButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Climb Down",
            accent = BambooGold,
            size = 56.dp,
            testTag = "btn_down",
            onPressChange = { inputState.down = it },
          )
        }

        // Right Action Cluster: Big Tactile JUMP Button
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (hud.doubleJumpSecondsLeft > 0f && hud.canDoubleJumpNow) {
            Surface(
              color = JadePrimary.copy(alpha = 0.25f),
              shape = RoundedCornerShape(50),
              border = BorderStroke(1.dp, JadePrimary),
            ) {
              Text(
                text = "2X JUMP READY",
                style = MaterialTheme.typography.labelSmall,
                color = JadePrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }
          JumpControlButton(
            doubleJumpActive = hud.doubleJumpSecondsLeft > 0f,
            onJumpDown = { inputState.triggerJumpPress() },
            onJumpUp = { inputState.releaseJump() },
          )
        }
      }
    }

    // Pause Overlay
    AnimatedVisibility(
      visible = hud.phase == GamePhase.PAUSED,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      OverlayScrim {
        OverlayModalCard(title = "PAUSED", accent = BambooGold) {
          Text(
            text = "Level ${hud.levelId}: ${hud.levelTitle}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(16.dp))
          Button(
            onClick = onTogglePause,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("resume_button"),
            colors = ButtonDefaults.buttonColors(containerColor = JadePrimary, contentColor = Color(0xFF042619)),
          ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("RESUME", fontWeight = FontWeight.Bold)
          }
          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("restart_button"),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("RESTART LEVEL")
          }
          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = onExitToLevels,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("exit_levels_button"),
          ) {
            Text("EXIT TO TOWERS")
          }
        }
      }
    }

    // Victory Summary Overlay
    AnimatedVisibility(
      visible = hud.phase == GamePhase.VICTORY,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      val hasNext = LevelCatalog.levels.any { it.id == hud.levelId + 1 }
      OverlayScrim {
        OverlayModalCard(title = "TOWER CLEARED!", accent = JadePrimary) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..3) {
              Icon(
                imageVector = if (i <= hud.earnedStars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = null,
                tint = if (i <= hud.earnedStars) BambooGold else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(44.dp),
              )
            }
          }
          Spacer(Modifier.height(12.dp))
          Text(
            text = "${hud.score} PTS",
            style = MaterialTheme.typography.displaySmall,
            color = BambooGold,
          )
          Spacer(Modifier.height(12.dp))
          BreakdownRow("Clear Time", "${hud.elapsedSeconds}s (+${hud.timeBonus})")
          BreakdownRow("Hearts Remaining", "${hud.lives}/3 (+${hud.heartBonus})")
          BreakdownRow("Bamboo Shoots", "${hud.shootsCollected}/${hud.totalShoots}")
          BreakdownRow("Max Combo • Smashes", "x${hud.maxCombo} • ${hud.barrelsSmashed}")
          Spacer(Modifier.height(18.dp))
          if (hasNext) {
            Button(
              onClick = { onNextLevel(hud.levelId + 1) },
              modifier = Modifier.fillMaxWidth().height(54.dp).testTag("next_level_button"),
              colors = ButtonDefaults.buttonColors(containerColor = JadePrimary, contentColor = Color(0xFF042619)),
            ) {
              Text("NEXT TOWER", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = onRetry,
              modifier = Modifier.weight(1f).height(48.dp).testTag("victory_replay_button"),
            ) {
              Text("REPLAY")
            }
            OutlinedButton(
              onClick = onExitToLevels,
              modifier = Modifier.weight(1f).height(48.dp).testTag("victory_levels_button"),
            ) {
              Text("TOWERS")
            }
          }
        }
      }
    }

    // Game Over Overlay
    AnimatedVisibility(
      visible = hud.phase == GamePhase.GAME_OVER,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      OverlayScrim {
        OverlayModalCard(title = "KNOCKED OUT!", accent = CrimsonAccent) {
          Text(
            text = "Don't let Mecha-Kong win — try another climb!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(Modifier.height(12.dp))
          Text(
            text = "SCORE: ${hud.score}",
            style = MaterialTheme.typography.headlineMedium,
            color = BambooGold,
          )
          Spacer(Modifier.height(18.dp))
          Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("retry_button"),
            colors = ButtonDefaults.buttonColors(containerColor = SunsetAmber, contentColor = Color.White),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("RETRY TOWER", fontWeight = FontWeight.Bold)
          }
          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = onExitToLevels,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("gameover_levels_button"),
          ) {
            Text("TOWER SELECT")
          }
        }
      }
    }
  }
}

@Composable
private fun HudChip(text: String, color: Color) {
  Surface(
    color = Color(0xC2101A24),
    shape = RoundedCornerShape(50),
    border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}

@Composable
private fun PowerTimerChip(icon: ImageVector, color: Color, text: String) {
  Surface(
    color = Color(0xD9101A24),
    shape = RoundedCornerShape(50),
    border = BorderStroke(1.5.dp, color),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
      Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
  }
}

@Composable
private fun HoldControlButton(
  icon: ImageVector,
  contentDescription: String,
  accent: Color,
  size: androidx.compose.ui.unit.Dp,
  testTag: String,
  onPressChange: (Boolean) -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(if (pressed) accent.copy(alpha = 0.42f) else Color(0xB3101B26))
      .border(2.dp, if (pressed) accent else accent.copy(alpha = 0.55f), CircleShape)
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()
            val anyDown = event.changes.any { it.pressed }
            if (anyDown != pressed) {
              pressed = anyDown
              onPressChange(anyDown)
            }
          }
        }
      }
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(32.dp))
  }
}

@Composable
private fun JumpControlButton(
  doubleJumpActive: Boolean,
  onJumpDown: () -> Unit,
  onJumpUp: () -> Unit,
) {
  var pressed by remember { mutableStateOf(false) }
  val accent = if (doubleJumpActive) JadePrimary else SunsetAmber
  Box(
    modifier = Modifier
      .size(94.dp)
      .clip(CircleShape)
      .background(if (pressed) accent.copy(alpha = 0.72f) else accent.copy(alpha = 0.38f))
      .border(3.dp, accent, CircleShape)
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()
            val anyDown = event.changes.any { it.pressed }
            if (anyDown && !pressed) {
              pressed = true
              onJumpDown()
            } else if (!anyDown && pressed) {
              pressed = false
              onJumpUp()
            }
          }
        }
      }
      .testTag("btn_jump"),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Jump", tint = Color.White, modifier = Modifier.size(34.dp))
      Text("JUMP", style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
  }
}

@Composable
private fun OverlayScrim(content: @Composable () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xB8060B10))
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

@Composable
private fun OverlayModalCard(
  title: String,
  accent: Color,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(2.dp, accent),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(title, style = MaterialTheme.typography.headlineMedium, color = accent, textAlign = TextAlign.Center)
      Spacer(Modifier.height(8.dp))
      content()
    }
  }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White)
  }
}
