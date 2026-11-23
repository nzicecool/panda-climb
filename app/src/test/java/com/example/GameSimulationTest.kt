package com.example

import com.example.audio.ArcadeSoundEngine
import com.example.game.engine.GamePhase
import com.example.game.engine.GameSimulation
import com.example.game.engine.InputState
import com.example.game.engine.LevelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSimulationTest {

  @Test
  fun allLevelsHaveValidPlatformsLaddersAndGoals() {
    assertEquals(5, LevelCatalog.levels.size)
    for (level in LevelCatalog.levels) {
      assertTrue("Level ${level.id} must have platforms", level.platforms.isNotEmpty())
      assertTrue("Level ${level.id} must have ladders", level.ladders.isNotEmpty())
      assertTrue("Level ${level.id} must have collectibles", level.collectibles.size == 3)
      assertTrue("Level ${level.id} must have power-ups", level.powerUps.isNotEmpty())
      for (ladder in level.ladders) {
        assertTrue("Ladder ${ladder.id} in level ${level.id} topY > bottomY", ladder.topY > ladder.bottomY)
      }
    }
  }

  @Test
  fun pandaCanWalkJumpAndClimbLadder() {
    val sound = ArcadeSoundEngine().apply { enabled = false }
    val level = LevelCatalog.getLevel(1)
    val sim = GameSimulation(level, sound, onVictory = {}, onGameOver = {})
    val input = InputState()

    val startX = sim.buildRenderSnapshot().player.x
    input.right = true
    repeat(30) { sim.step(input) }
    val movedX = sim.buildRenderSnapshot().player.x
    assertTrue("Panda should walk right (start=$startX, moved=$movedX)", movedX > startX + 1.5f)

    // Jump into the air
    input.right = false
    input.triggerJumpPress()
    repeat(10) { sim.step(input) }
    input.releaseJump()
    val snapshot = sim.buildRenderSnapshot()
    assertTrue("Panda should gain height when jumping", snapshot.player.y > level.startY + 0.5f)
    assertEquals(GamePhase.PLAYING, sim.buildHudState().phase)
  }

  @Test
  fun pauseAndResumeHaltsSimulationTime() {
    val sound = ArcadeSoundEngine().apply { enabled = false }
    val level = LevelCatalog.getLevel(1)
    val sim = GameSimulation(level, sound, onVictory = {}, onGameOver = {})
    val input = InputState()

    repeat(30) { sim.step(input) }
    val timeBeforePause = sim.buildRenderSnapshot().time

    sim.setPaused(true)
    repeat(60) { sim.step(input) }
    val timeDuringPause = sim.buildRenderSnapshot().time
    assertEquals(timeBeforePause, timeDuringPause, 0.0001f)
    assertEquals(GamePhase.PAUSED, sim.buildHudState().phase)

    sim.setPaused(false)
    repeat(30) { sim.step(input) }
    assertTrue(sim.buildRenderSnapshot().time > timeDuringPause)
  }
}
