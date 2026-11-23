package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.HighScoreEntity
import com.example.data.PlayerStatsEntity
import com.example.ui.theme.BambooGold
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyanShield
import com.example.ui.theme.JadePrimary
import com.example.ui.theme.SunsetAmber

@Composable
fun LeaderboardScreen(
  stats: PlayerStatsEntity,
  highScores: List<HighScoreEntity>,
  onResetProgress: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  var showResetConfirm by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(Color(0xFF0E1722), Color(0xFF13222E), Color(0xFF0B1118))
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("leaderboard_back"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back to home",
            tint = Color.White,
          )
        }
        Spacer(Modifier.width(4.dp))
        Column {
          Text("Hall of Fame", style = MaterialTheme.typography.headlineMedium, color = Color.White)
          Text("Stored locally on your phone", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      IconButton(
        onClick = { showResetConfirm = true },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("reset_progress_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.DeleteOutline,
          contentDescription = "Reset local scores",
          tint = CrimsonAccent,
        )
      }
    }

    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Career Stats Grid
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text("CAREER STATS • ${stats.playerNickname.uppercase()}", style = MaterialTheme.typography.labelSmall, color = BambooGold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              StatTile(Modifier.weight(1f), "TOTAL PTS", stats.totalScore.toString(), JadePrimary)
              StatTile(Modifier.weight(1f), "SMASHED", stats.barrelsSmashed.toString(), SunsetAmber)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              StatTile(Modifier.weight(1f), "BARREL JUMPS", stats.barrelsJumped.toString(), CyanShield)
              StatTile(Modifier.weight(1f), "POWER-UPS", stats.powerUpsCollected.toString(), BambooGold)
            }
          }
        }
      }

      item {
        Text(
          text = "TOP LOCAL RUNS",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
      }

      if (highScores.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = BambooGold, modifier = Modifier.size(40.dp))
              Text("No Completed Runs Yet", style = MaterialTheme.typography.titleLarge, color = Color.White)
              Text(
                "Climb a tower and ring the Red Panda Gong to record your first high score!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        itemsIndexed(highScores, key = { _, item -> item.id }) { index, entry ->
          val rankColor = when (index) {
            0 -> BambooGold
            1 -> Color(0xFFD7E2EC)
            2 -> SunsetAmber
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          }
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (index == 0) BambooGold.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.18f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("#${index + 1}", style = MaterialTheme.typography.titleMedium, color = rankColor)
                }
                Column {
                  Text("${entry.playerName} • Lvl ${entry.levelId}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                  Text(
                    "${entry.levelTitle} • ${entry.durationSeconds}s • Max x${entry.maxCombo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text("${entry.score}", style = MaterialTheme.typography.titleLarge, color = BambooGold)
                Row {
                  repeat(entry.stars) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = BambooGold, modifier = Modifier.size(14.dp))
                  }
                }
              }
            }
          }
        }
      }
      item { Spacer(Modifier.height(16.dp)) }
    }
  }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      title = { Text("Reset Local Data?") },
      text = { Text("This clears all saved scores, stars, and unlocked towers on this device.") },
      confirmButton = {
        Button(
          onClick = {
            onResetProgress()
            showResetConfirm = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
        ) {
          Text("Reset")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .padding(horizontal = 12.dp, vertical = 10.dp),
  ) {
    Column {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
    }
  }
}
