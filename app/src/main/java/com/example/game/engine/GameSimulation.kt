package com.example.game.engine

import com.example.audio.ArcadeSoundEngine
import com.example.audio.SoundEffect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameSimulation(
  private val level: LevelDefinition,
  private val soundEngine: ArcadeSoundEngine,
  private val onVictory: (GameHudState) -> Unit,
  private val onGameOver: (GameHudState) -> Unit,
) {
  companion object {
    const val FIXED_DT = 1f / 60f
    private const val GRAVITY = -26f
    private const val JUMP_VY = 11.6f
    private const val DOUBLE_JUMP_VY = 11.0f
    private const val MOVE_SPEED = 5.5f
    private const val CLIMB_SPEED = 4.5f
    private const val WORLD_BOUND_X = 6.9f
  }

  private var time = 0f
  private var phase = GamePhase.PLAYING

  // Player state
  private var px = level.startX
  private var py = level.startY
  private var vx = 0f
  private var vy = 0f
  private var facing = 1f
  private var onGround = true
  private var onLadder = false
  private var activeLadder: LadderDef? = null
  private var coyoteTimer = 0f
  private var jumpBufferTimer = 0f
  private var walkPhase = 0f
  private var climbPhase = 0f
  private var spinAngle = 0f
  private var invulnTimer = 0f
  private var hammerTimer = 0f
  private var hammerSwingPhase = 0f
  private var doubleJumpTimer = 0f
  private var doubleJumpAvailable = false
  private var speedTimer = 0f
  private var hasShield = false

  // Progression / score
  private var score = 0
  private var lives = 3
  private var combo = 1
  private var comboTimer = 0f
  private var maxCombo = 1
  private var barrelsSmashed = 0
  private var barrelsJumped = 0
  private var powerUpsCollected = 0
  private val collectedShoots = mutableSetOf<Int>()
  private val stompedRivets = mutableSetOf<Int>()
  private val jumpedHazardIds = mutableSetOf<Int>()
  private var nextHazardId = 1
  private var nextPopupId = 1
  private var barrelSpawnTimer = 0.8f
  private var bossThrowAnim = 0f
  private var bossStunTimer = 0f
  private var shakeIntensity = 0f

  // Dynamic obstacles
  private val hazards = mutableListOf<MutableHazard>()
  private val movingPlatforms = level.movingPlatforms.map {
    MutableMovingPlatform(it, it.baseX, it.baseY, 0f, 0f)
  }
  private val crumbleBridges = level.crumbleBridges.map {
    MutableCrumbleBridge(it, true, 0f, 0f)
  }
  private val springs = level.springs.map { MutableSpring(it, 0f) }
  private val powerUps = level.powerUps.map { MutablePowerUp(it, true, 0f) }
  private val particles = mutableListOf<Particle3D>()
  private val popups = mutableListOf<ScorePopup>()

  private val rng = Random(level.id * 97 + 42)

  private data class MutableHazard(
    val id: Int,
    val type: HazardType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotationZ: Float = 0f,
    var onLadder: Boolean = false,
    var ladderCooldown: Float = 0f,
  )

  private data class MutableMovingPlatform(
    val def: MovingPlatformDef,
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
  )

  private data class MutableCrumbleBridge(
    val def: CrumbleBridgeDef,
    var intact: Boolean,
    var shakeTimer: Float,
    var respawnTimer: Float,
  )

  private data class MutableSpring(
    val def: SpringDef,
    var bounceAnim: Float,
  )

  private data class MutablePowerUp(
    val def: PowerUpSpawnDef,
    var available: Boolean,
    var respawnTimer: Float,
  )

  init {
    // Seed initial flame spirits
    level.initialFlames.forEach { (fx, fy) ->
      hazards.add(
        MutableHazard(
          id = nextHazardId++,
          type = HazardType.FLAME_SPIRIT,
          x = fx,
          y = fy,
          vx = if (fx >= 0f) -2.4f else 2.4f,
          vy = 0f,
        )
      )
    }
  }

  fun setPaused(paused: Boolean) {
    if (phase == GamePhase.VICTORY || phase == GamePhase.GAME_OVER) return
    phase = if (paused) GamePhase.PAUSED else GamePhase.PLAYING
  }

  fun step(input: InputState) {
    if (phase != GamePhase.PLAYING) {
      updateParticlesOnly(FIXED_DT)
      return
    }

    val dt = FIXED_DT
    time += dt
    shakeIntensity = (shakeIntensity - dt * 2.5f).coerceAtLeast(0f)
    bossThrowAnim = (bossThrowAnim - dt * 2.4f).coerceAtLeast(0f)
    bossStunTimer = (bossStunTimer - dt).coerceAtLeast(0f)

    if (invulnTimer > 0f) invulnTimer = (invulnTimer - dt).coerceAtLeast(0f)
    if (hammerTimer > 0f) {
      hammerTimer = (hammerTimer - dt).coerceAtLeast(0f)
      hammerSwingPhase += dt * 11f
    }
    if (doubleJumpTimer > 0f) {
      doubleJumpTimer = (doubleJumpTimer - dt).coerceAtLeast(0f)
    }
    if (speedTimer > 0f) {
      speedTimer = (speedTimer - dt).coerceAtLeast(0f)
    }
    if (comboTimer > 0f) {
      comboTimer -= dt
      if (comboTimer <= 0f) combo = 1
    }

    updateMovingPlatforms()
    updateCrumbleBridges(dt)
    updateSprings(dt)
    updatePowerUpRespawns(dt)
    updatePlayer(dt, input)
    updateSpawners(dt)
    updateHazards(dt)
    checkInteractions()
    updateParticlesAndPopups(dt)
  }

  private fun updateMovingPlatforms() {
    for (mp in movingPlatforms) {
      val prevX = mp.x
      val prevY = mp.y
      val angle = time * mp.def.speed + mp.def.phase
      mp.x = mp.def.baseX + sin(angle) * mp.def.moveRangeX
      mp.y = mp.def.baseY + sin(angle) * mp.def.moveRangeY
      mp.dx = mp.x - prevX
      mp.dy = mp.y - prevY
    }
  }

  private fun updateCrumbleBridges(dt: Float) {
    for (cb in crumbleBridges) {
      if (cb.intact) {
        if (cb.shakeTimer > 0f) {
          cb.shakeTimer -= dt
          if (cb.shakeTimer <= 0f) {
            cb.intact = false
            cb.respawnTimer = 2.8f
            spawnBurst((cb.def.xStart + cb.def.xEnd) * 0.5f, cb.def.y, 0.25f, 0.78f, 0.35f, 12)
          }
        }
      } else {
        cb.respawnTimer -= dt
        if (cb.respawnTimer <= 0f) {
          cb.intact = true
          cb.shakeTimer = 0f
        }
      }
    }
  }

  private fun updateSprings(dt: Float) {
    for (sp in springs) {
      if (sp.bounceAnim > 0f) {
        sp.bounceAnim = (sp.bounceAnim - dt * 3.5f).coerceAtLeast(0f)
      }
    }
  }

  private fun updatePowerUpRespawns(dt: Float) {
    for (pu in powerUps) {
      if (!pu.available) {
        pu.respawnTimer -= dt
        if (pu.respawnTimer <= 0f) pu.available = true
      }
    }
  }

  private fun updatePlayer(dt: Float, input: InputState) {
    val speedMult = if (speedTimer > 0f) 1.42f else 1f
    val moveInput = when {
      input.left && !input.right -> -1f
      input.right && !input.left -> 1f
      else -> 0f
    }
    if (moveInput != 0f) facing = moveInput

    if (input.consumeJumpPress()) {
      jumpBufferTimer = 0.13f
    } else if (jumpBufferTimer > 0f) {
      jumpBufferTimer = (jumpBufferTimer - dt).coerceAtLeast(0f)
    }

    // Check ladder grab
    val nearLadder = findClimbableLadder(px, py)
    if (!onLadder && nearLadder != null && (input.up || input.down)) {
      val canGrabUp = input.up && py < nearLadder.topY - 0.05f
      val canGrabDown = input.down && py > nearLadder.bottomY + 0.05f
      if (canGrabUp || canGrabDown) {
        onLadder = true
        activeLadder = nearLadder
        onGround = false
        vx = 0f
        vy = 0f
        px = px + (nearLadder.x - px) * 0.45f
      }
    }

    if (onLadder) {
      val ladder = activeLadder
      if (ladder == null) {
        onLadder = false
      } else {
        px += (ladder.x - px) * 0.35f
        val climbDir = when {
          input.up -> 1f
          input.down -> -1f
          else -> 0f
        }
        vy = climbDir * CLIMB_SPEED * speedMult
        py += vy * dt
        if (climbDir != 0f) climbPhase += dt * 11f * speedMult

        // Jump off ladder
        if (jumpBufferTimer > 0f) {
          onLadder = false
          activeLadder = null
          vy = JUMP_VY * 0.92f
          vx = moveInput * MOVE_SPEED * speedMult
          jumpBufferTimer = 0f
          doubleJumpAvailable = doubleJumpTimer > 0f
          soundEngine.play(SoundEffect.JUMP)
        } else if (py >= ladder.topY) {
          py = ladder.topY
          onLadder = false
          activeLadder = null
          onGround = true
          vy = 0f
          doubleJumpAvailable = doubleJumpTimer > 0f
        } else if (py <= ladder.bottomY) {
          py = ladder.bottomY
          onLadder = false
          activeLadder = null
          onGround = true
          vy = 0f
          doubleJumpAvailable = doubleJumpTimer > 0f
        }
        return
      }
    }

    // Horizontal motion
    val targetVx = moveInput * MOVE_SPEED * speedMult
    val accel = if (onGround) 42f else 28f
    vx = moveTowards(vx, targetVx, accel * dt)

    // Conveyor push when standing on a conveyor platform
    var conveyorPush = 0f
    var ridingDx = 0f
    var ridingDy = 0f
    if (onGround) {
      val plat = findPlatformBelow(px, py + 0.15f, py - 0.18f)
      if (plat != null) {
        conveyorPush = plat.conveyorSpeed
      } else {
        for (mp in movingPlatforms) {
          if (abs(px - mp.x) <= mp.def.width * 0.5f + 0.15f && abs(py - mp.y) < 0.25f) {
            ridingDx = mp.dx
            ridingDy = mp.dy
            break
          }
        }
      }
    }

    if (onGround) {
      coyoteTimer = 0.11f
      doubleJumpAvailable = doubleJumpTimer > 0f
    } else if (coyoteTimer > 0f) {
      coyoteTimer = (coyoteTimer - dt).coerceAtLeast(0f)
    }

    // Jump execution
    if (jumpBufferTimer > 0f) {
      if (coyoteTimer > 0f) {
        vy = JUMP_VY
        onGround = false
        coyoteTimer = 0f
        jumpBufferTimer = 0f
        doubleJumpAvailable = doubleJumpTimer > 0f
        spawnBurst(px, py, 0.9f, 0.9f, 0.85f, 5)
        soundEngine.play(SoundEffect.JUMP)
      } else if (doubleJumpTimer > 0f && doubleJumpAvailable) {
        vy = DOUBLE_JUMP_VY
        doubleJumpAvailable = false
        jumpBufferTimer = 0f
        spinAngle = 0.01f
        spawnBurst(px, py, 0.35f, 1.0f, 0.72f, 10)
        soundEngine.play(SoundEffect.DOUBLE_JUMP)
      }
    }

    // Variable jump height cut
    if (!input.jumpHeld && vy > 4.2f) {
      vy *= 0.88f
    }

    val prevY = py
    val nextX = (px + (vx + conveyorPush) * dt + ridingDx).coerceIn(-WORLD_BOUND_X, WORLD_BOUND_X)
    vy = (vy + GRAVITY * dt).coerceAtLeast(-22f)
    val nextY = py + vy * dt + ridingDy

    if (spinAngle > 0f) {
      spinAngle += dt * 16f
      if (spinAngle >= (2.0 * PI).toFloat()) spinAngle = 0f
    }

    // Platform landing & walking adhesion
    var landedY: Float? = null
    if (vy <= 0.1f) {
      // Static girders
      for (plat in level.platforms) {
        if (plat.containsX(nextX)) {
          val surfY = plat.surfaceY(nextX)
          val wasAbove = prevY >= surfY - 0.22f
          val nowAtOrBelow = nextY <= surfY + (if (onGround) 0.18f else 0.04f)
          if (wasAbove && nowAtOrBelow) {
            if (landedY == null || surfY > landedY) landedY = surfY
          }
        }
      }
      // Moving platforms
      for (mp in movingPlatforms) {
        if (abs(nextX - mp.x) <= mp.def.width * 0.5f + 0.16f) {
          val surfY = mp.y
          if (prevY >= surfY - 0.25f && nextY <= surfY + 0.16f) {
            if (landedY == null || surfY > landedY) landedY = surfY
          }
        }
      }
      // Crumbling bridges
      for (cb in crumbleBridges) {
        if (cb.intact && nextX >= cb.def.xStart - 0.15f && nextX <= cb.def.xEnd + 0.15f) {
          val surfY = cb.def.y
          if (prevY >= surfY - 0.22f && nextY <= surfY + 0.15f) {
            if (landedY == null || surfY > landedY) landedY = surfY
            if (cb.shakeTimer <= 0f) cb.shakeTimer = 0.65f
          }
        }
      }
    }

    px = nextX
    if (landedY != null) {
      py = landedY
      vy = 0f
      onGround = true
      spinAngle = 0f
    } else {
      py = nextY
      onGround = false
    }

    if (onGround && abs(vx) > 0.35f) {
      walkPhase += dt * abs(vx) * 2.3f
    }

    // Check Spring pads
    for (sp in springs) {
      if (abs(px - sp.def.x) < 0.72f && abs(py - sp.def.y) < 0.55f && vy <= 1.0f) {
        vy = sp.def.launchVy
        onGround = false
        coyoteTimer = 0f
        sp.bounceAnim = 1f
        doubleJumpAvailable = doubleJumpTimer > 0f
        spawnBurst(sp.def.x, sp.def.y + 0.2f, 1.0f, 0.85f, 0.2f, 10)
        soundEngine.play(SoundEffect.SPRING)
        break
      }
    }

    // Fall out of bottom safety respawn
    if (py < -3.0f) {
      handlePlayerHit(bypassShield = true)
    }
  }

  private fun updateSpawners(dt: Float) {
    val interval = if (bossStunTimer > 0f) level.barrelSpawnInterval * 1.8f else level.barrelSpawnInterval
    barrelSpawnTimer -= dt
    if (barrelSpawnTimer <= 0f) {
      barrelSpawnTimer = interval
      bossThrowAnim = 1.0f
      val roll = rng.nextFloat()
      val type = when {
        level.id >= 4 && roll < 0.22f -> HazardType.BARREL_SPIKE
        level.id >= 2 && roll < 0.45f -> HazardType.BARREL_FAST
        else -> HazardType.BARREL_WOOD
      }
      val dir = if (level.bossX <= 0f) 1f else -1f
      val speed = when (type) {
        HazardType.BARREL_FAST -> 4.9f
        HazardType.BARREL_SPIKE -> 0.4f
        else -> 3.4f
      }
      hazards.add(
        MutableHazard(
          id = nextHazardId++,
          type = type,
          x = level.bossX + dir * 0.9f,
          y = level.bossY + 0.35f,
          vx = if (type == HazardType.BARREL_SPIKE) (px - level.bossX).coerceIn(-1.2f, 1.2f) else dir * speed,
          vy = if (type == HazardType.BARREL_SPIKE) -2.0f else 0f,
        )
      )
    }
  }

  private fun updateHazards(dt: Float) {
    val iter = hazards.iterator()
    while (iter.hasNext()) {
      val h = iter.next()
      h.ladderCooldown = (h.ladderCooldown - dt).coerceAtLeast(0f)

      if (h.type == HazardType.BARREL_SPIKE) {
        h.vy = (h.vy + GRAVITY * 0.45f * dt).coerceAtLeast(-11f)
        h.x += h.vx * dt
        h.y += h.vy * dt
        h.rotationZ += dt * 7f
        if (h.y < -2.5f) iter.remove()
        continue
      }

      if (h.type == HazardType.FLAME_SPIRIT) {
        updateFlameSpirit(h, dt)
        continue
      }

      // Rolling barrels
      if (h.onLadder) {
        h.vy = -4.2f
        h.y += h.vy * dt
        h.rotationZ += dt * 4f
        val below = findPlatformBelow(h.x, h.y + 0.12f, h.y - 0.16f)
        if (below != null && h.y <= below.surfaceY(h.x) + 0.08f) {
          h.y = below.surfaceY(h.x)
          h.onLadder = false
          h.ladderCooldown = 0.6f
          val slope = below.slopeDirection()
          val baseSpeed = if (h.type == HazardType.BARREL_FAST) 4.9f else 3.45f
          h.vx = if (slope != 0f) slope * baseSpeed else -h.vx
        }
      } else {
        val plat = findPlatformBelow(h.x, h.y + 0.25f, h.y - 0.25f)
        if (plat != null && h.vy <= 0.2f) {
          h.y = plat.surfaceY(h.x)
          h.vy = 0f
          val slope = plat.slopeDirection()
          val baseSpeed = if (h.type == HazardType.BARREL_FAST) 4.9f else 3.45f
          if (slope != 0f && abs(h.vx) < 0.5f) {
            h.vx = slope * baseSpeed
          } else if (slope != 0f && h.type == HazardType.BARREL_WOOD) {
            h.vx = slope * baseSpeed
          } else if (abs(h.vx) < 0.5f) {
            h.vx = if (h.x > 0f) -baseSpeed else baseSpeed
          }

          // Chance for wood barrel to roll down an intact ladder
          if (h.type == HazardType.BARREL_WOOD && h.ladderCooldown <= 0f) {
            val ladder = level.ladders.firstOrNull {
              !it.isBroken && abs(it.x - h.x) < 0.20f && abs(it.topY - h.y) < 0.35f
            }
            if (ladder != null && rng.nextFloat() < 0.32f) {
              h.x = ladder.x
              h.onLadder = true
              h.vx = if (h.vx >= 0f) -3.45f else 3.45f
              continue
            }
          }
        } else {
          h.vy = (h.vy + GRAVITY * dt).coerceAtLeast(-16f)
        }

        h.x += h.vx * dt
        h.y += h.vy * dt
        if (h.x < -WORLD_BOUND_X) {
          h.x = -WORLD_BOUND_X
          h.vx = abs(h.vx)
        } else if (h.x > WORLD_BOUND_X) {
          h.x = WORLD_BOUND_X
          h.vx = -abs(h.vx)
        }
        h.rotationZ -= (h.vx * dt) / 0.36f

        if (h.y < -1.8f) {
          iter.remove()
        }
      }
    }
  }

  private fun updateFlameSpirit(h: MutableHazard, dt: Float) {
    if (h.onLadder) {
      h.y += 2.6f * dt
      val ladder = level.ladders.firstOrNull { !it.isBroken && abs(it.x - h.x) < 0.3f && h.y <= it.topY + 0.1f }
      if (ladder == null || h.y >= ladder.topY) {
        h.onLadder = false
        h.ladderCooldown = 2.0f
        h.vx = if (px >= h.x) 2.3f else -2.3f
      }
      return
    }

    val plat = findPlatformBelow(h.x, h.y + 0.25f, h.y - 0.25f)
    if (plat != null) {
      h.y = plat.surfaceY(h.x)
      h.vy = 0f
      val minX = minOf(plat.xStart, plat.xEnd) + 0.25f
      val maxX = maxOf(plat.xStart, plat.xEnd) - 0.25f
      if (h.x <= minX) h.vx = abs(h.vx)
      if (h.x >= maxX) h.vx = -abs(h.vx)

      if (h.ladderCooldown <= 0f && py > h.y + 1.0f) {
        val ladder = level.ladders.firstOrNull {
          !it.isBroken && abs(it.x - h.x) < 0.25f && abs(it.bottomY - h.y) < 0.35f
        }
        if (ladder != null && rng.nextFloat() < 0.45f) {
          h.x = ladder.x
          h.onLadder = true
          return
        }
      }
    } else {
      h.vy = (h.vy + GRAVITY * dt).coerceAtLeast(-12f)
    }

    h.x += h.vx * dt
    h.y += h.vy * dt
    h.rotationZ += dt * 5f
    if (h.y < -2f) {
      h.x = 0f
      h.y = 6.2f
    }
  }

  private fun checkInteractions() {
    val playerCenterY = py + 0.55f

    // 1. Collect Golden Bamboo Shoots
    for (shoot in level.collectibles) {
      if (shoot.id in collectedShoots) continue
      val dx = shoot.x - px
      val dy = shoot.y - playerCenterY
      val dist = sqrt(dx * dx + dy * dy)
      val magnetRadius = if (speedTimer > 0f) 2.2f else 0.85f
      if (dist < magnetRadius) {
        collectedShoots.add(shoot.id)
        val pts = 300 * combo
        score += pts
        addPopup("+$pts BAMBOO!", 0xFFFFCA28)
        spawnBurst(shoot.x, shoot.y, 1.0f, 0.85f, 0.2f, 14)
        soundEngine.play(SoundEffect.COIN)
      }
    }

    // 2. Collect Power-ups
    for (pu in powerUps) {
      if (!pu.available) continue
      val dx = pu.def.x - px
      val dy = pu.def.y - playerCenterY
      if (dx * dx + dy * dy < 0.9f * 0.9f) {
        pu.available = false
        pu.respawnTimer = 14f
        powerUpsCollected++
        when (pu.def.type) {
          PowerUpType.HAMMER -> {
            hammerTimer = 8.0f
            addPopup("BAMBOO HAMMER!", 0xFFFFCA28)
          }
          PowerUpType.DOUBLE_JUMP -> {
            doubleJumpTimer = 12.0f
            doubleJumpAvailable = true
            addPopup("JADE DOUBLE JUMP!", 0xFF2EE59D)
          }
          PowerUpType.SHIELD -> {
            hasShield = true
            addPopup("LOTUS SHIELD!", 0xFF29D4FF)
          }
          PowerUpType.SPEED_CHILI -> {
            speedTimer = 9.0f
            addPopup("BLAZE SPEED!", 0xFFFF7043)
          }
        }
        score += 150
        spawnBurst(pu.def.x, pu.def.y, 0.3f, 0.95f, 0.85f, 16)
        soundEngine.play(SoundEffect.POWER_UP)
      }
    }

    // 3. Stomp Golden Rivets (Level 5)
    for (rivet in level.rivets) {
      if (rivet.id in stompedRivets) continue
      if (abs(px - rivet.x) < 0.7f && abs(py - rivet.y) < 0.55f) {
        stompedRivets.add(rivet.id)
        bossStunTimer = 3.0f
        score += 400
        shakeIntensity = 0.25f
        addPopup("+400 RIVET!", 0xFFFFCA28)
        spawnBurst(rivet.x, rivet.y + 0.2f, 1.0f, 0.78f, 0.2f, 16)
        soundEngine.play(SoundEffect.RIVET)
      }
    }

    // 4. Hazard collisions, Hammer smashes & Skillful Jump-Over bonuses
    val iter = hazards.iterator()
    while (iter.hasNext()) {
      val h = iter.next()
      val hCenterY = h.y + 0.38f
      val dx = h.x - px
      val dy = hCenterY - playerCenterY
      val distSq = dx * dx + dy * dy

      // Hammer smash radius
      if (hammerTimer > 0f && distSq < 1.32f * 1.32f) {
        iter.remove()
        barrelsSmashed++
        combo = (combo + 1).coerceAtMost(5)
        maxCombo = maxOf(maxCombo, combo)
        comboTimer = 4.0f
        val pts = 250 * combo
        score += pts
        shakeIntensity = 0.22f
        addPopup("+$pts SMASH x$combo!", 0xFFFF7043)
        spawnBurst(h.x, hCenterY, 0.95f, 0.55f, 0.18f, 18)
        soundEngine.play(SoundEffect.SMASH)
        continue
      }

      // Direct hit check
      if (distSq < 0.62f * 0.62f) {
        if (invulnTimer <= 0f) {
          if (hasShield) {
            hasShield = false
            invulnTimer = 1.6f
            iter.remove()
            shakeIntensity = 0.25f
            addPopup("SHIELD SAVED!", 0xFF29D4FF)
            spawnBurst(px, playerCenterY, 0.25f, 0.85f, 1.0f, 20)
            soundEngine.play(SoundEffect.SHIELD_POP)
          } else {
            iter.remove()
            handlePlayerHit(bypassShield = false)
            if (phase != GamePhase.PLAYING) return
          }
        }
        continue
      }

      // Skillful Jump-Over detection
      if (!onGround && !onLadder && h.id !in jumpedHazardIds) {
        if (abs(px - h.x) < 0.45f && py > h.y + 0.35f && py < h.y + 2.3f) {
          jumpedHazardIds.add(h.id)
          barrelsJumped++
          combo = (combo + 1).coerceAtMost(5)
          maxCombo = maxOf(maxCombo, combo)
          comboTimer = 3.5f
          val pts = 100 * combo
          score += pts
          addPopup("+$pts JUMP x$combo!", 0xFF2EE59D)
        }
      }
    }

    // 5. Goal Shrine check
    val goalDx = px - level.goalX
    val goalDy = py - level.goalY
    if (goalDx * goalDx + goalDy * goalDy < 1.15f * 1.15f) {
      val rivetsReady = level.rivets.isEmpty() || stompedRivets.size >= level.rivets.size
      if (rivetsReady) {
        triggerVictory()
      } else if (popups.none { it.text.contains("RIVETS") }) {
        addPopup("STOMP ALL ${level.rivets.size} RIVETS!", 0xFFFFCA28)
      }
    }
  }

  private fun handlePlayerHit(bypassShield: Boolean) {
    if (bypassShield && hasShield) hasShield = false
    lives -= 1
    combo = 1
    comboTimer = 0f
    hammerTimer = 0f
    shakeIntensity = 0.45f
    soundEngine.play(SoundEffect.HURT)
    spawnBurst(px, py + 0.5f, 1.0f, 0.25f, 0.3f, 22)

    if (lives <= 0) {
      phase = GamePhase.GAME_OVER
      onGameOver(buildHudState())
    } else {
      // Respawn safely at start of level with brief invulnerability
      px = level.startX
      py = level.startY
      vx = 0f
      vy = 0f
      onLadder = false
      activeLadder = null
      onGround = true
      invulnTimer = 2.2f
      addPopup("TRY AGAIN! ${lives}❤", 0xFFFF3D57)
    }
  }

  private fun triggerVictory() {
    phase = GamePhase.VICTORY
    soundEngine.play(SoundEffect.VICTORY)
    val elapsed = time.toInt().coerceAtLeast(1)
    val timeBonus = ((level.parTimeSeconds - elapsed).coerceAtLeast(0) * 35)
    val heartBonus = lives * 300
    val clearBonus = 600
    score += clearBonus + timeBonus + heartBonus

    val stars = when {
      score >= level.targetThreeStarScore -> 3
      score >= level.targetTwoStarScore || lives == 3 -> 2
      else -> 1
    }

    // Fireworks burst!
    repeat(3) {
      spawnBurst(
        level.goalX + (rng.nextFloat() - 0.5f) * 2.5f,
        level.goalY + 1.2f + rng.nextFloat() * 1.5f,
        if (it == 0) 1f else 0.2f,
        if (it == 1) 0.95f else 0.8f,
        if (it == 2) 1f else 0.3f,
        24,
      )
    }
    onVictory(buildHudState(earnedStars = stars, timeBonus = timeBonus, heartBonus = heartBonus))
  }

  private fun findClimbableLadder(x: Float, y: Float): LadderDef? {
    return level.ladders.firstOrNull { ladder ->
      !ladder.isBroken &&
        abs(x - ladder.x) < 0.55f &&
        y >= ladder.bottomY - 0.25f &&
        y <= ladder.topY + 0.25f
    }
  }

  private fun findPlatformBelow(x: Float, upperY: Float, lowerY: Float): PlatformDef? {
    var best: PlatformDef? = null
    var bestY = Float.NEGATIVE_INFINITY
    for (p in level.platforms) {
      if (p.containsX(x, 0.12f)) {
        val sy = p.surfaceY(x)
        if (sy in lowerY..upperY && sy > bestY) {
          bestY = sy
          best = p
        }
      }
    }
    return best
  }

  private fun spawnBurst(x: Float, y: Float, r: Float, g: Float, b: Float, count: Int) {
    repeat(count) {
      if (particles.size >= 140) particles.removeAt(0)
      val angle = rng.nextFloat() * (2.0 * PI).toFloat()
      val speed = 1.8f + rng.nextFloat() * 4.5f
      val life = 0.35f + rng.nextFloat() * 0.45f
      particles.add(
        Particle3D(
          x = x,
          y = y,
          z = (rng.nextFloat() - 0.5f) * 0.8f,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed * 0.7f + 2.2f,
          vz = (rng.nextFloat() - 0.5f) * 3.5f,
          r = r,
          g = g,
          b = b,
          size = 0.12f + rng.nextFloat() * 0.14f,
          life = life,
          maxLife = life,
        )
      )
    }
  }

  private fun updateParticlesAndPopups(dt: Float) {
    updateParticlesOnly(dt)
    val pIter = popups.iterator()
    while (pIter.hasNext()) {
      val idx = popups.indexOf(pIter.next())
      if (idx >= 0) {
        val updated = popups[idx].copy(ageSeconds = popups[idx].ageSeconds + dt)
        if (updated.ageSeconds >= 1.25f) {
          pIter.remove()
        } else {
          popups[idx] = updated
        }
      }
    }
  }

  private fun updateParticlesOnly(dt: Float) {
    val iter = particles.iterator()
    while (iter.hasNext()) {
      val p = iter.next()
      val nextLife = p.life - dt
      if (nextLife <= 0f) {
        iter.remove()
      } else {
        val idx = particles.indexOf(p)
        if (idx >= 0) {
          particles[idx] = p.copy(
            x = p.x + p.vx * dt,
            y = p.y + p.vy * dt,
            z = p.z + p.vz * dt,
            vy = p.vy - 12f * dt,
            life = nextLife,
          )
        }
      }
    }
  }

  private fun addPopup(text: String, colorHex: Long) {
    if (popups.size >= 4) popups.removeAt(0)
    popups.add(ScorePopup(nextPopupId++, text, colorHex))
  }

  private fun moveTowards(current: Float, target: Float, maxDelta: Float): Float {
    val diff = target - current
    return when {
      diff > maxDelta -> current + maxDelta
      diff < -maxDelta -> current - maxDelta
      else -> target
    }
  }

  fun buildRenderSnapshot(): RenderSceneSnapshot {
    return RenderSceneSnapshot(
      time = time,
      level = level,
      player = PlayerSnapshot(
        x = px,
        y = py,
        vx = vx,
        vy = vy,
        facing = facing,
        onGround = onGround,
        onLadder = onLadder,
        walkPhase = walkPhase,
        climbPhase = climbPhase,
        spinAngle = spinAngle,
        invulnerableTimer = invulnTimer,
        hammerTimer = hammerTimer,
        hammerSwingPhase = hammerSwingPhase,
        doubleJumpTimer = doubleJumpTimer,
        doubleJumpAvailable = doubleJumpAvailable,
        speedTimer = speedTimer,
        hasShield = hasShield,
      ),
      hazards = hazards.map {
        HazardSnapshot(it.id, it.type, it.x, it.y, it.vx, it.vy, it.rotationZ, it.onLadder)
      },
      movingPlatforms = movingPlatforms.map {
        MovingPlatformState(it.def, it.x, it.y, it.dx, it.dy)
      },
      crumbleBridges = crumbleBridges.map {
        CrumbleBridgeState(it.def, it.intact, it.shakeTimer, it.respawnTimer)
      },
      springs = springs.map { SpringState(it.def, it.bounceAnim) },
      collectedShootIds = collectedShoots.toSet(),
      stompedRivetIds = stompedRivets.toSet(),
      powerUps = powerUps.map { PowerUpPickupState(it.def, it.available, it.respawnTimer) },
      particles = particles.toList(),
      bossThrowAnim = bossThrowAnim,
      bossStunTimer = bossStunTimer,
      shakeIntensity = shakeIntensity,
      isVictory = phase == GamePhase.VICTORY,
    )
  }

  fun buildHudState(
    earnedStars: Int = 0,
    timeBonus: Int = 0,
    heartBonus: Int = 0,
  ): GameHudState {
    return GameHudState(
      levelId = level.id,
      levelTitle = level.title,
      phase = phase,
      score = score,
      lives = lives,
      combo = combo,
      comboTimer = comboTimer,
      elapsedSeconds = time.toInt(),
      shootsCollected = collectedShoots.size,
      totalShoots = level.collectibles.size,
      rivetsStomped = stompedRivets.size,
      totalRivets = level.rivets.size,
      hasShield = hasShield,
      hammerSecondsLeft = hammerTimer,
      doubleJumpSecondsLeft = doubleJumpTimer,
      speedSecondsLeft = speedTimer,
      canDoubleJumpNow = doubleJumpAvailable,
      barrelsSmashed = barrelsSmashed,
      barrelsJumped = barrelsJumped,
      powerUpsCollected = powerUpsCollected,
      maxCombo = maxCombo,
      earnedStars = earnedStars,
      timeBonus = timeBonus,
      heartBonus = heartBonus,
      popups = popups.toList(),
    )
  }
}
