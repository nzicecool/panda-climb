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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LevelProgressEntity
import com.example.game.engine.BiomeStyle
import com.example.game.engine.LevelCatalog
import com.example.game.engine.LevelDefinition
import com.example.ui.theme.BambooGold
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyanShield
import com.example.ui.theme.JadePrimary
import com.example.ui.theme.PlumeViolet
import com.example.ui.theme.SunsetAmber

@Composable
fun LevelSelectScreen(
  progressList: List<LevelProgressEntity>,
  practiceUnlockAll: Boolean,
  onTogglePracticeUnlock: (Boolean) -> Unit,
  onSelectLevel: (Int) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val progressMap = progressList.associateBy { it.levelId }

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
    // Top Bar
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
          modifier = Modifier.minimumInteractiveComponentSize().testTag("level_select_back"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back to home",
            tint = Color.White,
          )
        }
        Spacer(Modifier.width(4.dp))
        Column {
          Text("Tower Select", style = MaterialTheme.typography.headlineMedium, color = Color.White)
          Text("5 escalating 3D bamboo & steel towers", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Unlock All", style = MaterialTheme.typography.labelSmall, color = Color.White)
          Switch(
            checked = practiceUnlockAll,
            onCheckedChange = onTogglePracticeUnlock,
            modifier = Modifier.testTag("unlock_all_switch"),
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      items(LevelCatalog.levels, key = { it.id }) { level ->
        val progress = progressMap[level.id]
        val isUnlocked = practiceUnlockAll || (progress?.unlocked ?: (level.id == 1))
        LevelTowerCard(
          level = level,
          progress = progress,
          isUnlocked = isUnlocked,
          onPlay = { onSelectLevel(level.id) },
        )
      }
      item { Spacer(Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun LevelTowerCard(
  level: LevelDefinition,
  progress: LevelProgressEntity?,
  isUnlocked: Boolean,
  onPlay: () -> Unit,
) {
  val accent = when (level.biome) {
    BiomeStyle.SUNLIT_BAMBOO -> SunsetAmber
    BiomeStyle.JADE_FOUNDRY -> JadePrimary
    BiomeStyle.CRIMSON_PAGODA -> CrimsonAccent
    BiomeStyle.AURORA_SKYBRIDGE -> PlumeViolet
    BiomeStyle.VOLCANIC_SUMMIT -> BambooGold
  }
  val stars = progress?.bestStars ?: 0
  val bestScore = progress?.bestScore ?: 0
  val bestTime = progress?.bestTimeSeconds ?: 0

  Card(
    modifier = Modifier.fillMaxWidth().testTag("level_card_${level.id}"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
    ),
    border = BorderStroke(1.5.dp, if (isUnlocked) accent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(accent.copy(alpha = if (isUnlocked) 0.22f else 0.1f)),
            contentAlignment = Alignment.Center,
          ) {
            if (isUnlocked) {
              Text(
                text = "${level.id}",
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
              )
            } else {
              Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Column {
            Text(level.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
              level.mechanicsTag,
              style = MaterialTheme.typography.labelSmall,
              color = accent,
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          for (i in 1..3) {
            Icon(
              imageVector = if (i <= stars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
              contentDescription = null,
              tint = if (i <= stars) BambooGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }

      Spacer(Modifier.height(8.dp))
      Text(
        text = level.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (bestScore > 0) "BEST: $bestScore" else "TARGET: ${level.targetThreeStarScore}",
            style = MaterialTheme.typography.labelSmall,
            color = if (bestScore > 0) CyanShield else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (bestTime > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
              Icon(Icons.Rounded.Timer, contentDescription = null, tint = JadePrimary, modifier = Modifier.size(14.dp))
              Text("${bestTime}s", style = MaterialTheme.typography.labelSmall, color = JadePrimary)
            }
          }
        }

        Button(
          onClick = onPlay,
          enabled = isUnlocked,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = Color(0xFF0A141A),
          ),
          modifier = Modifier.height(48.dp).testTag("play_level_${level.id}"),
        ) {
          Icon(
            imageVector = if (isUnlocked) Icons.Rounded.PlayArrow else Icons.Rounded.Lock,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = if (isUnlocked) "CLIMB" else "LOCKED",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
