package com.example.ui.screens

import android.opengl.GLSurfaceView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.LevelProgressEntity
import com.example.data.PlayerStatsEntity
import com.example.game.gl.Panda3DRenderer
import com.example.ui.theme.BambooGold
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyanShield
import com.example.ui.theme.JadePrimary
import com.example.ui.theme.SunsetAmber

@Composable
fun HomeScreen(
  stats: PlayerStatsEntity,
  progressList: List<LevelProgressEntity>,
  onQuickPlay: (Int) -> Unit,
  onOpenLevelSelect: () -> Unit,
  onOpenLeaderboard: () -> Unit,
  onToggleSound: () -> Unit,
  onUpdateNickname: (String) -> Unit,
) {
  var showGuideDialog by remember { mutableStateOf(false) }
  var showNameDialog by remember { mutableStateOf(false) }
  var glViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, glViewRef) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> glViewRef?.onResume()
        Lifecycle.Event.ON_PAUSE -> glViewRef?.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val totalStars = progressList.sumOf { it.bestStars }
  val maxStars = (progressList.size.coerceAtLeast(5)) * 3
  val nextPlayableLevel = progressList
    .filter { it.unlocked || stats.practiceUnlockAll }
    .lastOrNull { !it.completed }?.levelId
    ?: progressList.lastOrNull { it.unlocked || stats.practiceUnlockAll }?.levelId
    ?: 1

  Box(modifier = Modifier.fillMaxSize()) {
    // Live 3D animated Panda Diorama background
    AndroidView(
      factory = { context ->
        GLSurfaceView(context).apply {
          setEGLContextClientVersion(2)
          setRenderer(Panda3DRenderer(snapshotProvider = { null }))
          renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
          glViewRef = this
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Atmospheric vignette gradient so foreground UI pops cleanly
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            0.0f to Color(0xBF0A1219),
            0.35f to Color(0x400A1219),
            0.65f to Color(0x700A1219),
            1.0f to Color(0xEB0A1219),
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top utility row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable { showNameDialog = true }
            .testTag("nickname_chip"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_panda_foreground_1795405734292),
              contentDescription = "Panda Avatar",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            )
            Text(
              text = stats.playerNickname,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = "Edit player name",
              tint = JadePrimary,
              modifier = Modifier.size(16.dp),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            IconButton(
              onClick = { showGuideDialog = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("guide_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.HelpOutline,
                contentDescription = "Power-ups and controls guide",
                tint = BambooGold,
              )
            }
          }
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            IconButton(
              onClick = onToggleSound,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sound_toggle_button"),
            ) {
              Icon(
                imageVector = if (stats.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                contentDescription = if (stats.soundEnabled) "Mute sound" else "Enable sound",
                tint = if (stats.soundEnabled) JadePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Title Hero Header (leaves center clear for the 3D Panda diorama)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 4.dp),
      ) {
        Surface(
          color = BambooGold.copy(alpha = 0.18f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, BambooGold.copy(alpha = 0.6f)),
        ) {
          Text(
            text = "3D ARCADE PLATFORMER",
            style = MaterialTheme.typography.labelSmall,
            color = BambooGold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
          )
        }
        Spacer(Modifier.height(6.dp))
        Text(
          text = "PANDA CLIMB",
          style = MaterialTheme.typography.displayLarge,
          color = Color.White,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "Outsmart Mecha-Kong across 5 vibrant 3D towers",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFD6E4E8),
          textAlign = TextAlign.Center,
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      // Bottom Stats + Action Card
      Column(
        modifier = Modifier
          .widthIn(max = 520.dp)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          HomeStatPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Star,
            iconColor = BambooGold,
            label = "STARS",
            value = "$totalStars / $maxStars",
          )
          HomeStatPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.EmojiEvents,
            iconColor = JadePrimary,
            label = "CAREER PTS",
            value = stats.totalScore.toString(),
          )
        }

        Button(
          onClick = { onQuickPlay(nextPlayableLevel) },
          modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("play_button"),
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = JadePrimary,
            contentColor = Color(0xFF042619),
          ),
        ) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
          Spacer(Modifier.width(8.dp))
          Text(
            text = "PLAY LEVEL $nextPlayableLevel",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilledTonalButton(
            onClick = onOpenLevelSelect,
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("levels_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
              contentColor = Color.White,
            ),
          ) {
            Icon(Icons.Rounded.Layers, contentDescription = null, tint = BambooGold)
            Spacer(Modifier.width(6.dp))
            Text("TOWERS", style = MaterialTheme.typography.labelLarge)
          }

          OutlinedButton(
            onClick = onOpenLeaderboard,
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("leaderboard_button"),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
              contentColor = Color.White,
            ),
          ) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = SunsetAmber)
            Spacer(Modifier.width(6.dp))
            Text("SCORES", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }
  }

  if (showGuideDialog) {
    HowToPlayDialog(onDismiss = { showGuideDialog = false })
  }

  if (showNameDialog) {
    var draft by remember(stats.playerNickname) { mutableStateOf(stats.playerNickname) }
    AlertDialog(
      onDismissRequest = { showNameDialog = false },
      title = { Text("Hero Name", style = MaterialTheme.typography.headlineMedium) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Saved locally on your phone for your arcade high score table.",
            style = MaterialTheme.typography.bodyMedium,
          )
          OutlinedTextField(
            value = draft,
            onValueChange = { if (it.length <= 18) draft = it },
            singleLine = true,
            label = { Text("Nickname") },
            modifier = Modifier.fillMaxWidth().testTag("nickname_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateNickname(draft)
            showNameDialog = false
          },
          modifier = Modifier.testTag("save_nickname_button"),
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun HomeStatPill(
  modifier: Modifier = Modifier,
  icon: ImageVector,
  iconColor: Color,
  label: String,
  value: String,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shape = RoundedCornerShape(18.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(iconColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
      }
      Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White)
      }
    }
  }
}

@Composable
private fun HowToPlayDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Panda Climb Guide", style = MaterialTheme.typography.headlineMedium) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          "Climb to the Red Panda Shrine at the summit! Jump cleanly over rolling barrels to stack Combo Multipliers (up to x5).",
          style = MaterialTheme.typography.bodyMedium,
        )
        GuidePowerRow(Icons.Rounded.Hardware, BambooGold, "Bamboo Hammer", "Smash rolling barrels & flame spirits for +250 x Combo points!")
        GuidePowerRow(Icons.Rounded.DoubleArrow, JadePrimary, "Jade Plume", "Grants a mid-air Double Jump to cross wide gaps & skip tiers.")
        GuidePowerRow(Icons.Rounded.Shield, CyanShield, "Lotus Shield", "Absorbs one barrel or hazard hit with a cyan shockwave.")
        GuidePowerRow(Icons.Rounded.Bolt, CrimsonAccent, "Blaze Chili", "Boosts run/climb speed & magnetizes Golden Bamboo Shoots!")
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("Got It!") }
    },
  )
}

@Composable
private fun GuidePowerRow(icon: ImageVector, color: Color, title: String, desc: String) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(color.copy(alpha = 0.2f))
        .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
    Column {
      Text(title, style = MaterialTheme.typography.titleMedium, color = color)
      Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
