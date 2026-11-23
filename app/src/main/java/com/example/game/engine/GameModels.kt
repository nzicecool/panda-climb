package com.example.game.engine

enum class PowerUpType(val label: String) {
  HAMMER("Bamboo Hammer"),
  DOUBLE_JUMP("Jade Plume"),
  SHIELD("Lotus Shield"),
  SPEED_CHILI("Blaze Chili"),
}

enum class HazardType {
  BARREL_WOOD,
  BARREL_FAST,
  BARREL_SPIKE,
  FLAME_SPIRIT,
}

enum class BiomeStyle {
  SUNLIT_BAMBOO,
  JADE_FOUNDRY,
  CRIMSON_PAGODA,
  AURORA_SKYBRIDGE,
  VOLCANIC_SUMMIT,
}

data class Vec3Color(val r: Float, val g: Float, val b: Float)

data class BiomePalette(
  val skyTop: Vec3Color,
  val skyBottom: Vec3Color,
  val girderMain: Vec3Color,
  val girderTrim: Vec3Color,
  val pillarColor: Vec3Color,
  val fogColor: Vec3Color,
  val accentGlow: Vec3Color,
)

data class PlatformDef(
  val id: Int,
  val xStart: Float,
  val xEnd: Float,
  val yStart: Float,
  val yEnd: Float,
  val conveyorSpeed: Float = 0f,
  val isBossDeck: Boolean = false,
) {
  fun containsX(x: Float, margin: Float = 0.18f): Boolean =
    x >= minOf(xStart, xEnd) - margin && x <= maxOf(xStart, xEnd) + margin

  fun surfaceY(x: Float): Float {
    val span = (xEnd - xStart)
    if (kotlin.math.abs(span) < 0.0001f) return yStart
    val t = ((x - xStart) / span).coerceIn(0f, 1f)
    return yStart + (yEnd - yStart) * t
  }

  fun slopeDirection(): Float {
    val dy = yEnd - yStart
    return when {
      dy > 0.05f -> -1f
      dy < -0.05f -> 1f
      else -> 0f
    }
  }
}

data class LadderDef(
  val id: Int,
  val x: Float,
  val bottomY: Float,
  val topY: Float,
  val isBroken: Boolean = false,
)

data class SpringDef(
  val id: Int,
  val x: Float,
  val y: Float,
  val launchVy: Float = 18.2f,
)

data class MovingPlatformDef(
  val id: Int,
  val baseX: Float,
  val baseY: Float,
  val width: Float,
  val moveRangeX: Float,
  val moveRangeY: Float,
  val speed: Float,
  val phase: Float = 0f,
)

data class CrumbleBridgeDef(
  val id: Int,
  val xStart: Float,
  val xEnd: Float,
  val y: Float,
)

data class RivetSwitchDef(
  val id: Int,
  val x: Float,
  val y: Float,
)

data class CollectibleDef(
  val id: Int,
  val x: Float,
  val y: Float,
)

data class PowerUpSpawnDef(
  val id: Int,
  val x: Float,
  val y: Float,
  val type: PowerUpType,
)

data class LevelDefinition(
  val id: Int,
  val title: String,
  val subtitle: String,
  val mechanicsTag: String,
  val biome: BiomeStyle,
  val palette: BiomePalette,
  val startX: Float,
  val startY: Float,
  val bossX: Float,
  val bossY: Float,
  val goalX: Float,
  val goalY: Float,
  val barrelSpawnInterval: Float,
  val targetTwoStarScore: Int,
  val targetThreeStarScore: Int,
  val parTimeSeconds: Int,
  val platforms: List<PlatformDef>,
  val ladders: List<LadderDef>,
  val springs: List<SpringDef> = emptyList(),
  val movingPlatforms: List<MovingPlatformDef> = emptyList(),
  val crumbleBridges: List<CrumbleBridgeDef> = emptyList(),
  val rivets: List<RivetSwitchDef> = emptyList(),
  val collectibles: List<CollectibleDef>,
  val powerUps: List<PowerUpSpawnDef>,
  val initialFlames: List<Pair<Float, Float>> = emptyList(),
)

// Live mutable/snapshot entities rendered in 3D

data class PlayerSnapshot(
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val facing: Float,
  val onGround: Boolean,
  val onLadder: Boolean,
  val walkPhase: Float,
  val climbPhase: Float,
  val spinAngle: Float,
  val invulnerableTimer: Float,
  val hammerTimer: Float,
  val hammerSwingPhase: Float,
  val doubleJumpTimer: Float,
  val doubleJumpAvailable: Boolean,
  val speedTimer: Float,
  val hasShield: Boolean,
)

data class HazardSnapshot(
  val id: Int,
  val type: HazardType,
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val rotationZ: Float,
  val onLadder: Boolean,
)

data class MovingPlatformState(
  val def: MovingPlatformDef,
  val currentX: Float,
  val currentY: Float,
  val dx: Float,
  val dy: Float,
)

data class CrumbleBridgeState(
  val def: CrumbleBridgeDef,
  val isIntact: Boolean,
  val shakeTimer: Float,
  val respawnTimer: Float,
)

data class SpringState(
  val def: SpringDef,
  val bounceAnim: Float,
)

data class PowerUpPickupState(
  val def: PowerUpSpawnDef,
  val available: Boolean,
  val respawnTimer: Float,
)

data class Particle3D(
  val x: Float,
  val y: Float,
  val z: Float,
  val vx: Float,
  val vy: Float,
  val vz: Float,
  val r: Float,
  val g: Float,
  val b: Float,
  val size: Float,
  val life: Float,
  val maxLife: Float,
)

data class ScorePopup(
  val id: Int,
  val text: String,
  val accentColorHex: Long,
  val ageSeconds: Float = 0f,
)

enum class GamePhase {
  PLAYING,
  PAUSED,
  VICTORY,
  GAME_OVER,
}

data class GameHudState(
  val levelId: Int = 1,
  val levelTitle: String = "",
  val phase: GamePhase = GamePhase.PLAYING,
  val score: Int = 0,
  val lives: Int = 3,
  val combo: Int = 1,
  val comboTimer: Float = 0f,
  val elapsedSeconds: Int = 0,
  val shootsCollected: Int = 0,
  val totalShoots: Int = 3,
  val rivetsStomped: Int = 0,
  val totalRivets: Int = 0,
  val hasShield: Boolean = false,
  val hammerSecondsLeft: Float = 0f,
  val doubleJumpSecondsLeft: Float = 0f,
  val speedSecondsLeft: Float = 0f,
  val canDoubleJumpNow: Boolean = false,
  val barrelsSmashed: Int = 0,
  val barrelsJumped: Int = 0,
  val powerUpsCollected: Int = 0,
  val maxCombo: Int = 1,
  val earnedStars: Int = 0,
  val timeBonus: Int = 0,
  val heartBonus: Int = 0,
  val popups: List<ScorePopup> = emptyList(),
)

data class RenderSceneSnapshot(
  val time: Float,
  val level: LevelDefinition,
  val player: PlayerSnapshot,
  val hazards: List<HazardSnapshot>,
  val movingPlatforms: List<MovingPlatformState>,
  val crumbleBridges: List<CrumbleBridgeState>,
  val springs: List<SpringState>,
  val collectedShootIds: Set<Int>,
  val stompedRivetIds: Set<Int>,
  val powerUps: List<PowerUpPickupState>,
  val particles: List<Particle3D>,
  val bossThrowAnim: Float,
  val bossStunTimer: Float,
  val shakeIntensity: Float,
  val isVictory: Boolean,
)

class InputState {
  @Volatile var left: Boolean = false
  @Volatile var right: Boolean = false
  @Volatile var up: Boolean = false
  @Volatile var down: Boolean = false
  @Volatile var jumpHeld: Boolean = false
  @Volatile private var jumpTapQueued: Boolean = false

  fun triggerJumpPress() {
    jumpHeld = true
    jumpTapQueued = true
  }

  fun releaseJump() {
    jumpHeld = false
  }

  fun consumeJumpPress(): Boolean {
    return if (jumpTapQueued) {
      jumpTapQueued = false
      true
    } else {
      false
    }
  }

  fun clearAll() {
    left = false
    right = false
    up = false
    down = false
    jumpHeld = false
    jumpTapQueued = false
  }
}
