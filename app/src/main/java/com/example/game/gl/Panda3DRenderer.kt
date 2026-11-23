package com.example.game.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.BiomeStyle
import com.example.game.engine.HazardSnapshot
import com.example.game.engine.HazardType
import com.example.game.engine.LevelCatalog
import com.example.game.engine.LevelDefinition
import com.example.game.engine.PlayerSnapshot
import com.example.game.engine.PowerUpType
import com.example.game.engine.RenderSceneSnapshot
import com.example.game.engine.Vec3Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Panda3DRenderer(
  private val snapshotProvider: () -> RenderSceneSnapshot?,
  private val onFrameStep: (() -> Unit)? = null,
) : GLSurfaceView.Renderer {

  private lateinit var litShader: ShaderProgram
  private lateinit var skyShader: ShaderProgram
  private lateinit var cubeMesh: Mesh
  private lateinit var sphereMesh: Mesh
  private lateinit var cylinderMesh: Mesh
  private lateinit var coneMesh: Mesh
  private lateinit var torusMesh: Mesh
  private lateinit var skyQuadBuffer: FloatBuffer

  // Shader locations
  private var uMVPMatrix = 0
  private var uModelMatrix = 0
  private var uNormalMatrix = 0
  private var uColor = 0
  private var uEmissive = 0
  private var uSpecular = 0
  private var uRimColor = 0
  private var uLightDir = 0
  private var uCameraPos = 0
  private var uFogColor = 0
  private var uFogDensity = 0
  private var aPos = 0
  private var aNorm = 0

  private var skyPosAttr = 0
  private var uSkyBottom = 0
  private var uSkyTop = 0

  // Pre-allocated matrices to keep 60 FPS GC-free
  private val projMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val vpMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val invMatrix = FloatArray(16)
  private val normalMatrix = FloatArray(16)

  private var camX = 0f
  private var camY = 4.5f
  private var dioramaTime = 0f

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glDepthFunc(GLES20.GL_LEQUAL)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    litShader = ShaderProgram(ShaderProgram.LIT_VERTEX_SHADER, ShaderProgram.LIT_FRAGMENT_SHADER)
    skyShader = ShaderProgram(ShaderProgram.SKY_VERTEX_SHADER, ShaderProgram.SKY_FRAGMENT_SHADER)

    cubeMesh = MeshBuilder.createUnitCube()
    sphereMesh = MeshBuilder.createSphere()
    cylinderMesh = MeshBuilder.createCylinder()
    coneMesh = MeshBuilder.createCone()
    torusMesh = MeshBuilder.createTorus()

    val skyVerts = floatArrayOf(-1f, -1f, 1f, -1f, 1f, 1f, -1f, -1f, 1f, 1f, -1f, 1f)
    skyQuadBuffer = ByteBuffer.allocateDirect(skyVerts.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .apply { put(skyVerts); position(0) }

    uMVPMatrix = litShader.uniform("uMVPMatrix")
    uModelMatrix = litShader.uniform("uModelMatrix")
    uNormalMatrix = litShader.uniform("uNormalMatrix")
    uColor = litShader.uniform("uColor")
    uEmissive = litShader.uniform("uEmissive")
    uSpecular = litShader.uniform("uSpecular")
    uRimColor = litShader.uniform("uRimColor")
    uLightDir = litShader.uniform("uLightDir")
    uCameraPos = litShader.uniform("uCameraPos")
    uFogColor = litShader.uniform("uFogColor")
    uFogDensity = litShader.uniform("uFogDensity")
    aPos = litShader.attrib("aPosition")
    aNorm = litShader.attrib("aNormal")

    skyPosAttr = skyShader.attrib("aPosition")
    uSkyBottom = skyShader.uniform("uSkyBottom")
    uSkyTop = skyShader.uniform("uSkyTop")
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    val aspect = if (height > 0) width.toFloat() / height.toFloat() else 1f
    val fov = if (aspect < 0.75f) 52f else 44f
    Matrix.perspectiveM(projMatrix, 0, fov, aspect, 1.0f, 90f)
  }

  override fun onDrawFrame(gl: GL10?) {
    onFrameStep?.invoke()
    dioramaTime += 1f / 60f

    val snapshot = snapshotProvider()
    if (snapshot == null) {
      renderDiorama()
    } else {
      renderGameplayScene(snapshot)
    }
  }

  private fun drawSky(bottom: Vec3Color, top: Vec3Color) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    GLES20.glDepthMask(false)
    skyShader.use()
    GLES20.glUniform3f(uSkyBottom, bottom.r, bottom.g, bottom.b)
    GLES20.glUniform3f(uSkyTop, top.r, top.g, top.b)
    GLES20.glEnableVertexAttribArray(skyPosAttr)
    GLES20.glVertexAttribPointer(skyPosAttr, 2, GLES20.GL_FLOAT, false, 0, skyQuadBuffer)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
    GLES20.glDisableVertexAttribArray(skyPosAttr)
    GLES20.glDepthMask(true)
  }

  private fun renderDiorama() {
    val lv = LevelCatalog.levels[0]
    drawSky(lv.palette.skyBottom, lv.palette.skyTop)

    val orbitAngle = sin(dioramaTime * 0.45f) * 0.35f
    val eyeX = sin(orbitAngle) * 7.5f
    val eyeZ = cos(orbitAngle) * 9.5f
    val eyeY = 3.2f + sin(dioramaTime * 0.6f) * 0.3f

    Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, 0f, 1.4f, 0f, 0f, 1f, 0f)
    Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

    litShader.use()
    GLES20.glUniform3f(uLightDir, 0.45f, 0.82f, 0.55f)
    GLES20.glUniform3f(uCameraPos, eyeX, eyeY, eyeZ)
    GLES20.glUniform3f(uFogColor, lv.palette.fogColor.r, lv.palette.fogColor.g, lv.palette.fogColor.b)
    GLES20.glUniform1f(uFogDensity, 0.04f)

    // Pedestal girder & bamboo pillars
    drawBox(0f, -0.35f, 0f, 7.5f, 0.45f, 2.2f, 0f, lv.palette.girderMain, 0.45f, lv.palette.accentGlow)
    drawBox(0f, -0.10f, 0.98f, 7.5f, 0.10f, 0.25f, 0f, lv.palette.girderTrim, 0.6f)
    drawCylinder(-3.2f, -2.5f, -0.7f, 0.35f, 4.5f, 0.35f, 0f, 0f, lv.palette.pillarColor, 0.25f)
    drawCylinder(3.2f, -2.5f, -0.7f, 0.35f, 4.5f, 0.35f, 0f, 0f, lv.palette.pillarColor, 0.25f)

    // Bouncing heroic panda in the center
    val bounceY = abs(sin(dioramaTime * 2.8f)) * 0.42f
    val demoPanda = PlayerSnapshot(
      x = 0f,
      y = bounceY - 0.1f,
      vx = 1f,
      vy = 0f,
      facing = if (sin(dioramaTime * 0.6f) >= 0f) 1f else -1f,
      onGround = bounceY < 0.05f,
      onLadder = false,
      walkPhase = dioramaTime * 4.5f,
      climbPhase = 0f,
      spinAngle = 0f,
      invulnerableTimer = 0f,
      hammerTimer = 1f,
      hammerSwingPhase = sin(dioramaTime * 2.5f) * 0.4f,
      doubleJumpTimer = 1f,
      doubleJumpAvailable = true,
      speedTimer = 0f,
      hasShield = true,
    )
    drawPanda(demoPanda, dioramaTime)

    // Orbiting bamboo shoots and rolling barrel
    for (i in 0..2) {
      val a = dioramaTime * 1.4f + i * 2.1f
      drawBambooShoot(cos(a) * 2.4f, 1.0f + sin(dioramaTime * 2f + i) * 0.35f, sin(a) * 1.6f, dioramaTime + i)
    }
    drawBarrel(
      HazardSnapshot(1, HazardType.BARREL_WOOD, -2.4f, -0.12f, 0f, 0f, dioramaTime * 2.2f, false),
      dioramaTime
    )
    drawBackdrop(lv, 0f, 1.5f, dioramaTime)
  }

  private fun renderGameplayScene(s: RenderSceneSnapshot) {
    val lv = s.level
    drawSky(lv.palette.skyBottom, lv.palette.skyTop)

    // Smoothly track panda with dynamic 3D perspective angle
    val targetCamX = (s.player.x * 0.55f).coerceIn(-3.5f, 3.5f)
    val targetCamY = (s.player.y + 2.8f).coerceIn(3.5f, lv.goalY + 0.8f)
    camX += (targetCamX - camX) * 0.10f
    camY += (targetCamY - camY) * 0.10f

    val shakeX = if (s.shakeIntensity > 0f) sin(s.time * 55f) * s.shakeIntensity * 0.35f else 0f
    val shakeY = if (s.shakeIntensity > 0f) cos(s.time * 63f) * s.shakeIntensity * 0.35f else 0f

    val eyeX = camX + (s.player.x - camX) * 0.45f + shakeX
    val eyeY = camY + 1.85f + shakeY
    val eyeZ = 14.5f

    Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, camX, camY, 0f, 0f, 1f, 0f)
    Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

    litShader.use()
    GLES20.glUniform3f(uLightDir, 0.45f, 0.85f, 0.52f)
    GLES20.glUniform3f(uCameraPos, eyeX, eyeY, eyeZ)
    GLES20.glUniform3f(uFogColor, lv.palette.fogColor.r, lv.palette.fogColor.g, lv.palette.fogColor.b)
    GLES20.glUniform1f(uFogDensity, 0.032f)

    // 1. 3D Parallax Backdrop
    drawBackdrop(lv, camX, camY, s.time)

    // 2. Vertical Bamboo / Steel Support Columns framing the tower
    val towerTop = lv.goalY + 2.5f
    drawCylinder(-6.9f, towerTop * 0.5f, -0.8f, 0.34f, towerTop + 3f, 0.34f, 0f, 0f, lv.palette.pillarColor, 0.25f)
    drawCylinder(6.9f, towerTop * 0.5f, -0.8f, 0.34f, towerTop + 3f, 0.34f, 0f, 0f, lv.palette.pillarColor, 0.25f)

    // 3. Platforms (Slanted 3D I-Beam Girders)
    for (p in lv.platforms) {
      drawGirder(p.xStart, p.xEnd, p.yStart, p.yEnd, p.conveyorSpeed, s.time, lv)
    }

    // 4. Moving Platforms
    for (mp in s.movingPlatforms) {
      val hw = mp.def.width * 0.5f
      drawGirder(mp.currentX - hw, mp.currentX + hw, mp.currentY, mp.currentY, 0f, s.time, lv, isMoving = true)
    }

    // 5. Crumbling Bamboo Bridges
    for (cb in s.crumbleBridges) {
      if (cb.isIntact) {
        val shakeOffset = if (cb.shakeTimer > 0f) sin(s.time * 48f) * 0.08f else 0f
        val warnColor = if (cb.shakeTimer > 0f) Vec3Color(1.0f, 0.45f, 0.18f) else Vec3Color(0.32f, 0.78f, 0.34f)
        val cx = (cb.def.xStart + cb.def.xEnd) * 0.5f
        val len = abs(cb.def.xEnd - cb.def.xStart)
        val count = 4
        val logStep = len / count
        for (i in 0 until count) {
          val lx = cb.def.xStart + (i + 0.5f) * logStep
          drawCylinder(lx, cb.def.y - 0.14f + shakeOffset, 0f, 0.24f, 1.2f, 0.24f, 90f, 0f, warnColor, 0.3f)
        }
        drawBox(cx, cb.def.y - 0.04f + shakeOffset, 0f, len, 0.08f, 0.9f, 0f, Vec3Color(0.62f, 0.48f, 0.24f), 0.2f)
      }
    }

    // 6. 3D Ladders
    for (ladder in lv.ladders) {
      drawLadder(ladder.x, ladder.bottomY, ladder.topY, ladder.isBroken, lv)
    }

    // 7. Spring Pads
    for (sp in s.springs) {
      val compress = sin(sp.bounceAnim * PI.toFloat()) * 0.35f
      val baseH = 0.32f + compress
      drawCylinder(sp.def.x, sp.def.y + 0.06f, 0f, 0.75f, 0.12f, 0.75f, 0f, 0f, Vec3Color(0.25f, 0.28f, 0.34f), 0.5f)
      drawTorus(sp.def.x, sp.def.y + baseH * 0.55f, 0f, 0.45f, 0.45f, 0.45f, 0f, 0f, Vec3Color(1.0f, 0.82f, 0.2f), 0.8f)
      drawCylinder(sp.def.x, sp.def.y + baseH, 0f, 0.95f, 0.16f, 0.95f, 0f, 0f, Vec3Color(1.0f, 0.24f, 0.32f), 0.6f, Vec3Color(0.25f, 0.02f, 0.05f))
    }

    // 8. Golden Rivets (Level 5)
    for (rivet in lv.rivets) {
      val stomped = rivet.id in s.stompedRivetIds
      val yOff = if (stomped) 0.05f else 0.24f + sin(s.time * 4f + rivet.id) * 0.06f
      val col = if (stomped) Vec3Color(0.32f, 0.35f, 0.40f) else Vec3Color(1.0f, 0.82f, 0.16f)
      val emissive = if (stomped) Vec3Color(0f, 0f, 0f) else Vec3Color(0.35f, 0.22f, 0f)
      drawCylinder(rivet.x, rivet.y + yOff * 0.5f, 0f, 0.65f, yOff.coerceAtLeast(0.1f), 0.65f, 0f, 0f, col, 0.85f, emissive = emissive)
    }

    // 9. Golden Bamboo Shoots
    for (shoot in lv.collectibles) {
      if (shoot.id !in s.collectedShootIds) {
        drawBambooShoot(shoot.x, shoot.y + sin(s.time * 3.5f + shoot.id) * 0.14f, 0f, s.time + shoot.id)
      }
    }

    // 10. Power-ups
    for (pu in s.powerUps) {
      if (pu.available) {
        drawPowerUp(pu.def.x, pu.def.y + sin(s.time * 3.2f + pu.def.id) * 0.15f, pu.def.type, s.time)
      }
    }

    // 11. Boss & Goal Red Panda + Gong
    drawBossKong(lv.bossX, lv.bossY, s.time, s.bossThrowAnim, s.bossStunTimer)
    drawGoalShrine(lv.goalX, lv.goalY, s.time, s.isVictory)

    // 12. Hazards (Barrels & Flame Spirits)
    for (h in s.hazards) {
      if (h.type == HazardType.FLAME_SPIRIT) {
        drawFlameSpirit(h, s.time)
      } else {
        drawBarrel(h, s.time)
      }
    }

    // 13. Hero Panda
    val blinkHide = s.player.invulnerableTimer > 0f && ((s.time * 16f).toInt() % 2 == 0)
    if (!blinkHide) {
      drawPanda(s.player, s.time)
    }

    // 14. 3D Particles
    for (p in s.particles) {
      val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
      val sz = p.size * (0.4f + alpha * 0.8f)
      drawBox(p.x, p.y, p.z, sz, sz, sz, p.life * 280f, Vec3Color(p.r, p.g, p.b), 0.5f, Vec3Color(p.r * 0.35f, p.g * 0.35f, p.b * 0.35f))
    }
  }

  private fun drawBackdrop(lv: LevelDefinition, cx: Float, cy: Float, time: Float) {
    // Distant pagoda tower silhouettes & glowing lanterns in Z-depth
    val silColor = Vec3Color(
      lv.palette.skyTop.r * 0.8f + 0.05f,
      lv.palette.skyTop.g * 0.8f + 0.05f,
      lv.palette.skyTop.b * 0.8f + 0.08f,
    )
    val lanternGlow = lv.palette.accentGlow

    // Distant bamboo stalks
    val offsets = floatArrayOf(-11f, -7.5f, -3.5f, 3.8f, 8.2f, 11.8f)
    for (i in offsets.indices) {
      val bx = offsets[i] + cx * 0.25f
      val bz = -7.5f - (i % 3) * 2.5f
      drawCylinder(bx, cy * 0.4f + 6f, bz, 0.55f, 28f, 0.55f, 0f, 0f, silColor, 0.1f)
    }

    // Distant Pagoda centerpiece
    val px = cx * 0.3f
    val pz = -12.5f
    for (tier in 0..3) {
      val ty = 2.5f + tier * 4.2f
      val w = 7.5f - tier * 1.1f
      drawBox(px, ty, pz, w * 0.65f, 2.4f, 2.5f, 0f, silColor, 0.15f)
      drawCone(px, ty + 1.8f, pz, w, 1.5f, 3.2f, 45f, silColor, 0.25f)
    }

    // Floating 3D sky lanterns / crystals
    for (i in 0..5) {
      val lx = -8f + i * 3.2f + sin(time * 0.4f + i * 1.3f) * 0.6f
      val ly = 3.5f + (i % 3) * 5.2f + cos(time * 0.7f + i) * 0.5f
      val lz = -5.5f - (i % 2) * 3.0f
      if (lv.biome == BiomeStyle.AURORA_SKYBRIDGE) {
        drawCone(lx, ly, lz, 0.45f, 0.9f, 0.45f, time * 40f + i * 30f, lanternGlow, 0.9f, emissive = lanternGlow)
      } else {
        drawSphere(lx, ly, lz, 0.36f, 0.48f, 0.36f, lanternGlow, 0.3f, emissive = Vec3Color(lanternGlow.r * 0.65f, lanternGlow.g * 0.5f, lanternGlow.b * 0.35f))
      }
    }
  }

  private fun drawGirder(
    xStart: Float,
    xEnd: Float,
    yStart: Float,
    yEnd: Float,
    conveyorSpeed: Float,
    time: Float,
    lv: LevelDefinition,
    isMoving: Boolean = false,
  ) {
    val dx = xEnd - xStart
    val dy = yEnd - yStart
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.1f)
    val cx = (xStart + xEnd) * 0.5f
    val cy = (yStart + yEnd) * 0.5f - 0.18f
    val angleDeg = (atan2(dy, dx) * 180.0 / PI).toFloat()

    val mainCol = if (isMoving) Vec3Color(0.24f, 0.78f, 0.96f) else lv.palette.girderMain
    val trimCol = if (abs(conveyorSpeed) > 0.1f) Vec3Color(1.0f, 0.85f, 0.22f) else lv.palette.girderTrim

    // Main 3D girder beam
    drawBox(cx, cy, 0f, len, 0.36f, 1.35f, angleDeg, mainCol, 0.45f, lv.palette.accentGlow)
    // Bright top/front trim flange
    drawBox(cx, cy + 0.14f, 0.64f, len, 0.10f, 0.14f, angleDeg, trimCol, 0.75f, Vec3Color(trimCol.r * 0.2f, trimCol.g * 0.2f, trimCol.b * 0.2f))
    drawBox(cx, cy - 0.14f, 0.64f, len, 0.08f, 0.12f, angleDeg, trimCol, 0.55f)

    // Animated conveyor rollers / arrows
    if (abs(conveyorSpeed) > 0.1f) {
      val dir = if (conveyorSpeed > 0f) 1f else -1f
      val count = (len / 0.9f).toInt().coerceAtLeast(2)
      val step = len / count
      val phase = ((time * conveyorSpeed * 0.7f) % 1f + 1f) % 1f
      for (i in 0 until count) {
        val localX = -len * 0.5f + ((i + phase * dir + count) % count) * step
        val wx = cx + localX * cos(angleDeg * PI.toFloat() / 180f)
        val wy = cy + 0.20f + localX * sin(angleDeg * PI.toFloat() / 180f)
        drawBox(wx, wy, 0f, 0.22f, 0.06f, 1.15f, angleDeg, Vec3Color(1f, 0.92f, 0.35f), 0.8f, Vec3Color(0.35f, 0.25f, 0f))
      }
    }
  }

  private fun drawLadder(x: Float, bottomY: Float, topY: Float, isBroken: Boolean, lv: LevelDefinition) {
    val h = (topY - bottomY).coerceAtLeast(0.2f)
    val railColor = if (isBroken) Vec3Color(0.55f, 0.45f, 0.40f) else Vec3Color(0.95f, 0.85f, 0.45f)
    val rungColor = Vec3Color(0.95f, 0.65f, 0.22f)
    val zPos = 0.28f

    if (isBroken) {
      val segH = h * 0.30f
      drawCylinder(x - 0.30f, bottomY + segH * 0.5f, zPos, 0.08f, segH, 0.08f, 0f, 0f, railColor, 0.3f)
      drawCylinder(x + 0.30f, bottomY + segH * 0.5f, zPos, 0.08f, segH, 0.08f, 0f, 0f, railColor, 0.3f)
      drawCylinder(x - 0.30f, topY - segH * 0.5f, zPos, 0.08f, segH, 0.08f, 0f, 0f, railColor, 0.3f)
      drawCylinder(x + 0.30f, topY - segH * 0.5f, zPos, 0.08f, segH, 0.08f, 0f, 0f, railColor, 0.3f)
    } else {
      val cy = (bottomY + topY) * 0.5f
      drawCylinder(x - 0.30f, cy, zPos, 0.09f, h, 0.09f, 0f, 0f, railColor, 0.5f)
      drawCylinder(x + 0.30f, cy, zPos, 0.09f, h, 0.09f, 0f, 0f, railColor, 0.5f)
      val rungs = (h / 0.46f).toInt().coerceAtLeast(2)
      for (i in 1 until rungs) {
        val ry = bottomY + h * i / rungs
        drawCylinder(x, ry, zPos, 0.07f, 0.60f, 0.07f, 90f, 0f, rungColor, 0.5f, Vec3Color(0.15f, 0.08f, 0.0f))
      }
    }
  }

  private fun drawPanda(p: PlayerSnapshot, time: Float) {
    val whiteFur = Vec3Color(0.97f, 0.96f, 0.93f)
    val blackFur = Vec3Color(0.11f, 0.12f, 0.15f)
    val redBand = Vec3Color(0.95f, 0.20f, 0.25f)
    val gold = Vec3Color(1.0f, 0.82f, 0.20f)
    val pinkCheek = Vec3Color(1.0f, 0.55f, 0.62f)

    val dir = if (p.onLadder) 0f else p.facing
    val yaw = if (p.onLadder) 180f else if (dir >= 0f) 26f else -26f
    val swing = if (p.onLadder) sin(p.climbPhase) * 38f else if (p.onGround) sin(p.walkPhase) * 35f * (abs(p.vx).coerceAtMost(4f) / 4f) else 28f
    val bob = if (p.onGround && abs(p.vx) > 0.4f) abs(sin(p.walkPhase)) * 0.07f else 0f

    val baseX = p.x
    val baseY = p.y + 0.52f + bob
    val spinDeg = (p.spinAngle * 180.0 / PI).toFloat()

    // Lotus Shield glowing sphere/ring aura
    if (p.hasShield) {
      val shieldCol = Vec3Color(0.20f, 0.88f, 1.0f)
      drawTorus(baseX, baseY + 0.12f, 0f, 1.25f, 1.25f, 1.25f, 25f, time * 140f, shieldCol, 0.9f, emissive = Vec3Color(0.10f, 0.48f, 0.65f))
      drawTorus(baseX, baseY + 0.12f, 0f, 1.25f, 1.25f, 1.25f, -35f, -time * 110f, shieldCol, 0.9f, emissive = Vec3Color(0.10f, 0.48f, 0.65f))
    }

    // Blaze Speed chili trail ring
    if (p.speedTimer > 0f) {
      drawTorus(baseX, p.y + 0.15f, 0f, 0.95f, 0.95f, 0.95f, 0f, time * 260f, Vec3Color(1f, 0.45f, 0.15f), 0.8f, emissive = Vec3Color(0.45f, 0.15f, 0.02f))
    }

    // Legs
    val legOff = 0.17f
    drawSphere(baseX - legOff, p.y + 0.18f + maxOf(0f, sin(p.walkPhase)) * 0.08f, 0.05f * sin(swing * 0.03f), 0.24f, 0.30f, 0.26f, blackFur, 0.25f)
    drawSphere(baseX + legOff, p.y + 0.18f + maxOf(0f, -sin(p.walkPhase)) * 0.08f, -0.05f * sin(swing * 0.03f), 0.24f, 0.30f, 0.26f, blackFur, 0.25f)

    // Chubby Panda Torso + Black Shoulder Band
    drawSphere(baseX, baseY - 0.06f, 0f, 0.64f, 0.62f, 0.56f, whiteFur, 0.25f)
    drawCylinder(baseX, baseY + 0.08f, 0f, 0.58f, 0.18f, 0.52f, spinDeg, yaw, blackFur, 0.25f)

    // Jade Plume Wings on back when Double-Jump active
    if (p.doubleJumpTimer > 0f) {
      val flap = sin(time * 14f) * 0.14f
      val jade = Vec3Color(0.22f, 0.95f, 0.64f)
      drawBox(baseX - 0.36f, baseY + 0.12f + flap, -0.26f, 0.42f, 0.18f, 0.08f, 22f, jade, 0.7f, emissive = Vec3Color(0.08f, 0.4f, 0.22f))
      drawBox(baseX + 0.36f, baseY + 0.12f + flap, -0.26f, 0.42f, 0.18f, 0.08f, -22f, jade, 0.7f, emissive = Vec3Color(0.08f, 0.4f, 0.22f))
    }

    // Arms
    val armY = baseY + 0.06f
    drawSphere(baseX - 0.34f, armY + sin(swing * 0.03f) * 0.14f, 0.08f, 0.22f, 0.34f, 0.22f, blackFur, 0.3f)
    drawSphere(baseX + 0.34f, armY - sin(swing * 0.03f) * 0.14f, 0.08f, 0.22f, 0.34f, 0.22f, blackFur, 0.3f)

    // Head
    val headY = baseY + 0.44f
    drawSphere(baseX, headY, 0.02f, 0.62f, 0.56f, 0.56f, whiteFur, 0.25f)

    // Ears
    drawSphere(baseX - 0.24f, headY + 0.24f, -0.02f, 0.21f, 0.21f, 0.18f, blackFur, 0.3f)
    drawSphere(baseX + 0.24f, headY + 0.24f, -0.02f, 0.21f, 0.21f, 0.18f, blackFur, 0.3f)

    // Red-and-Gold Hero Headband
    drawCylinder(baseX, headY + 0.14f, 0.02f, 0.61f, 0.11f, 0.55f, 0f, yaw, redBand, 0.4f)
    drawSphere(baseX + dir * 0.05f, headY + 0.14f, 0.28f, 0.11f, 0.11f, 0.08f, gold, 0.9f, emissive = Vec3Color(0.3f, 0.2f, 0f))

    // Face features facing camera/direction
    val fx = dir * 0.08f
    // Eye patches
    drawSphere(baseX - 0.12f + fx, headY + 0.02f, 0.25f, 0.16f, 0.18f, 0.08f, blackFur, 0.2f)
    drawSphere(baseX + 0.12f + fx, headY + 0.02f, 0.25f, 0.16f, 0.18f, 0.08f, blackFur, 0.2f)
    // Sparkling white eyes
    drawSphere(baseX - 0.11f + fx, headY + 0.03f, 0.29f, 0.07f, 0.08f, 0.05f, whiteFur, 0.9f, emissive = Vec3Color(0.25f, 0.25f, 0.25f))
    drawSphere(baseX + 0.11f + fx, headY + 0.03f, 0.29f, 0.07f, 0.08f, 0.05f, whiteFur, 0.9f, emissive = Vec3Color(0.25f, 0.25f, 0.25f))
    // Nose & rosy cheeks
    drawSphere(baseX + fx, headY - 0.06f, 0.30f, 0.10f, 0.07f, 0.08f, blackFur, 0.8f)
    drawSphere(baseX - 0.20f + fx, headY - 0.08f, 0.24f, 0.10f, 0.07f, 0.06f, pinkCheek, 0.2f)
    drawSphere(baseX + 0.20f + fx, headY - 0.08f, 0.24f, 0.10f, 0.07f, 0.06f, pinkCheek, 0.2f)

    // Swinging 3D Golden Bamboo Hammer when active
    if (p.hammerTimer > 0f) {
      val swingAngle = sin(p.hammerSwingPhase) * 65f
      val hx = baseX + dir * 0.56f
      val hy = baseY + 0.25f + cos(p.hammerSwingPhase) * 0.25f
      val bambooGreen = Vec3Color(0.45f, 0.85f, 0.25f)
      drawCylinder(hx, hy, 0.22f, 0.09f, 0.75f, 0.09f, swingAngle, 0f, bambooGreen, 0.5f)
      drawCylinder(hx + dir * 0.1f, hy + 0.32f, 0.22f, 0.34f, 0.55f, 0.34f, swingAngle + 90f, 0f, gold, 0.9f, emissive = Vec3Color(0.35f, 0.22f, 0f))
    }
  }

  private fun drawBossKong(x: Float, y: Float, time: Float, throwAnim: Float, stunTimer: Float) {
    val armor = Vec3Color(0.24f, 0.18f, 0.22f)
    val fur = Vec3Color(0.46f, 0.24f, 0.14f)
    val crimson = Vec3Color(0.92f, 0.22f, 0.22f)
    val eyeGlow = if (stunTimer > 0f) Vec3Color(0.35f, 0.85f, 1.0f) else Vec3Color(1.0f, 0.82f, 0.15f)

    val beat = abs(sin(time * 4.5f)) * 0.12f
    val armRaise = if (throwAnim > 0f) sin(throwAnim * PI.toFloat()) * 0.65f else beat * 1.5f

    // Barrel stack beside boss
    drawCylinder(x - 1.15f, y + 0.32f, -0.15f, 0.55f, 0.64f, 0.55f, 0f, 0f, Vec3Color(0.72f, 0.42f, 0.18f), 0.35f)
    drawCylinder(x - 1.15f, y + 0.94f, -0.15f, 0.55f, 0.64f, 0.55f, 0f, 0f, Vec3Color(0.72f, 0.42f, 0.18f), 0.35f)

    // Bulky torso + armor chest plate
    drawSphere(x, y + 0.72f, 0f, 1.35f, 1.20f, 0.98f, fur, 0.2f)
    drawBox(x, y + 0.78f, 0.22f, 1.05f, 0.62f, 0.65f, 0f, crimson, 0.7f)

    // Muscular arms
    drawSphere(x - 0.78f, y + 0.75f + armRaise, 0.12f, 0.44f, 0.78f, 0.44f, armor, 0.4f)
    drawSphere(x + 0.78f, y + 0.75f + armRaise, 0.12f, 0.44f, 0.78f, 0.44f, armor, 0.4f)

    // Head + fierce glowing eyes
    drawSphere(x, y + 1.48f, 0.08f, 0.82f, 0.72f, 0.75f, fur, 0.3f)
    drawSphere(x - 0.16f, y + 1.56f, 0.42f, 0.13f, 0.11f, 0.08f, eyeGlow, 0.9f, emissive = eyeGlow)
    drawSphere(x + 0.16f, y + 1.56f, 0.42f, 0.13f, 0.11f, 0.08f, eyeGlow, 0.9f, emissive = eyeGlow)

    if (stunTimer > 0f) {
      drawTorus(x, y + 2.0f, 0f, 0.8f, 0.8f, 0.8f, 0f, time * 260f, Vec3Color(1f, 0.9f, 0.2f), 0.9f, emissive = Vec3Color(0.5f, 0.4f, 0f))
    }
  }

  private fun drawGoalShrine(x: Float, y: Float, time: Float, isVictory: Boolean) {
    val orange = Vec3Color(0.95f, 0.46f, 0.18f)
    val cream = Vec3Color(0.98f, 0.93f, 0.85f)
    val gold = Vec3Color(1.0f, 0.84f, 0.20f)

    val hop = abs(sin(time * (if (isVictory) 7f else 3.2f))) * 0.28f
    // Red Panda friend hopping happily
    drawSphere(x + 0.35f, y + 0.36f + hop, 0f, 0.42f, 0.46f, 0.38f, orange, 0.25f)
    drawSphere(x + 0.35f, y + 0.74f + hop, 0.02f, 0.45f, 0.40f, 0.42f, orange, 0.25f)
    drawSphere(x + 0.35f, y + 0.72f + hop, 0.16f, 0.28f, 0.24f, 0.18f, cream, 0.2f)
    drawSphere(x + 0.18f, y + 0.95f + hop, 0f, 0.15f, 0.18f, 0.12f, orange, 0.2f)
    drawSphere(x + 0.52f, y + 0.95f + hop, 0f, 0.15f, 0.18f, 0.12f, orange, 0.2f)

    // Golden Rescue Gong Arch
    drawCylinder(x - 0.55f, y + 0.75f, -0.15f, 0.12f, 1.5f, 0.12f, 0f, 0f, Vec3Color(0.85f, 0.22f, 0.22f), 0.4f)
    drawCylinder(x - 0.05f, y + 0.75f, -0.15f, 0.12f, 1.5f, 0.12f, 0f, 0f, Vec3Color(0.85f, 0.22f, 0.22f), 0.4f)
    drawBox(x - 0.30f, y + 1.52f, -0.15f, 0.95f, 0.16f, 0.24f, 0f, gold, 0.8f)
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x - 0.30f, y + 0.95f, -0.15f)
    Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
    Matrix.scaleM(modelMatrix, 0, 0.52f, 0.10f, 0.52f)
    submitMesh(cylinderMesh, gold, 0.9f, Vec3Color(0.32f, 0.22f, 0.0f), gold)
  }

  private fun drawBarrel(h: HazardSnapshot, time: Float) {
    val cy = h.y + 0.36f
    val rotDeg = (h.rotationZ * 180.0 / PI).toFloat()
    val bodyCol = when (h.type) {
      HazardType.BARREL_FAST -> Vec3Color(0.18f, 0.58f, 0.98f)
      HazardType.BARREL_SPIKE -> Vec3Color(0.92f, 0.20f, 0.26f)
      else -> Vec3Color(0.76f, 0.44f, 0.18f)
    }
    val hoopCol = when (h.type) {
      HazardType.BARREL_FAST -> Vec3Color(0.55f, 0.95f, 1.0f)
      HazardType.BARREL_SPIKE -> Vec3Color(1.0f, 0.84f, 0.18f)
      else -> Vec3Color(0.22f, 0.24f, 0.28f)
    }
    val emissive = if (h.type == HazardType.BARREL_SPIKE) Vec3Color(0.3f, 0.06f, 0.06f) else Vec3Color(0f, 0f, 0f)

    // Orient cylinder along Z axis (90 deg on X) and spin around Z as it rolls!
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, h.x, cy, 0f)
    Matrix.rotateM(modelMatrix, 0, rotDeg, 0f, 0f, 1f)
    Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
    Matrix.scaleM(modelMatrix, 0, 0.68f, 0.64f, 0.68f)
    submitMesh(cylinderMesh, bodyCol, 0.45f, emissive, hoopCol)

    // Barrel hoops wrapping the cylinder circumference
    for (hz in floatArrayOf(-0.18f, 0.18f)) {
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, h.x, cy, hz)
      Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
      Matrix.scaleM(modelMatrix, 0, 0.66f, 0.66f, 0.66f)
      submitMesh(torusMesh, hoopCol, 0.8f, emissive, hoopCol)
    }

    if (h.type == HazardType.BARREL_SPIKE) {
      drawSphere(h.x, cy, 0f, 0.82f, 0.82f, 0.82f, Vec3Color(1f, 0.85f, 0.2f), 0.9f, emissive = Vec3Color(0.35f, 0.2f, 0f))
    }
  }

  private fun drawFlameSpirit(h: HazardSnapshot, time: Float) {
    val cy = h.y + 0.34f + abs(sin(time * 6.5f + h.id)) * 0.14f
    val core = Vec3Color(1.0f, 0.55f, 0.12f)
    val tip = Vec3Color(1.0f, 0.22f, 0.18f)
    drawSphere(h.x, cy, 0f, 0.54f, 0.54f, 0.54f, core, 0.8f, emissive = Vec3Color(0.75f, 0.25f, 0.04f))
    drawCone(h.x, cy + 0.34f, 0f, 0.42f, 0.48f, 0.42f, time * 180f, tip, 0.7f, emissive = Vec3Color(0.55f, 0.12f, 0.02f))
  }

  private fun drawBambooShoot(x: Float, y: Float, z: Float, phase: Float) {
    val gold = Vec3Color(1.0f, 0.84f, 0.22f)
    val leaf = Vec3Color(0.28f, 0.95f, 0.52f)
    drawCone(x, y, z, 0.42f, 0.68f, 0.42f, phase * 110f, gold, 0.95f, emissive = Vec3Color(0.35f, 0.24f, 0.02f))
    drawTorus(x, y - 0.08f, z, 0.56f, 0.56f, 0.56f, 20f, phase * 90f, leaf, 0.8f, emissive = Vec3Color(0.08f, 0.3f, 0.12f))
  }

  private fun drawPowerUp(x: Float, y: Float, type: PowerUpType, time: Float) {
    val (col, em) = when (type) {
      PowerUpType.HAMMER -> Vec3Color(1.0f, 0.82f, 0.18f) to Vec3Color(0.40f, 0.24f, 0.02f)
      PowerUpType.DOUBLE_JUMP -> Vec3Color(0.20f, 0.95f, 0.62f) to Vec3Color(0.05f, 0.38f, 0.20f)
      PowerUpType.SHIELD -> Vec3Color(0.20f, 0.86f, 1.0f) to Vec3Color(0.06f, 0.32f, 0.45f)
      PowerUpType.SPEED_CHILI -> Vec3Color(1.0f, 0.36f, 0.20f) to Vec3Color(0.45f, 0.10f, 0.03f)
    }
    drawTorus(x, y, 0f, 0.78f, 0.78f, 0.78f, 25f, time * 140f, col, 0.9f, emissive = em)
    when (type) {
      PowerUpType.HAMMER -> {
        drawCylinder(x, y, 0f, 0.36f, 0.56f, 0.36f, 90f, time * 100f, col, 0.9f, emissive = em)
      }
      PowerUpType.DOUBLE_JUMP -> {
        drawCone(x, y + 0.08f, 0f, 0.44f, 0.56f, 0.20f, time * 120f, col, 0.9f, emissive = em)
      }
      PowerUpType.SHIELD -> {
        drawSphere(x, y, 0f, 0.46f, 0.46f, 0.46f, col, 0.95f, emissive = em)
      }
      PowerUpType.SPEED_CHILI -> {
        drawCone(x, y, 0f, 0.36f, 0.64f, 0.36f, time * 150f, col, 0.9f, emissive = em)
      }
    }
  }

  private fun drawBox(
    x: Float, y: Float, z: Float,
    sx: Float, sy: Float, sz: Float,
    rotZDeg: Float,
    col: Vec3Color,
    spec: Float,
    emissive: Vec3Color = Vec3Color(0f, 0f, 0f),
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (rotZDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotZDeg, 0f, 0f, 1f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitMesh(cubeMesh, col, spec, emissive, col)
  }

  private fun drawSphere(
    x: Float, y: Float, z: Float,
    sx: Float, sy: Float, sz: Float,
    col: Vec3Color,
    spec: Float,
    emissive: Vec3Color = Vec3Color(0f, 0f, 0f),
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitMesh(sphereMesh, col, spec, emissive, col)
  }

  private fun drawCylinder(
    x: Float, y: Float, z: Float,
    sx: Float, sy: Float, sz: Float,
    rotZDeg: Float,
    rotYDeg: Float,
    col: Vec3Color,
    spec: Float,
    emissive: Vec3Color = Vec3Color(0f, 0f, 0f),
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (rotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotYDeg, 0f, 1f, 0f)
    if (rotZDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotZDeg, 0f, 0f, 1f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitMesh(cylinderMesh, col, spec, emissive, col)
  }

  private fun drawCone(
    x: Float, y: Float, z: Float,
    sx: Float, sy: Float, sz: Float,
    rotYDeg: Float,
    col: Vec3Color,
    spec: Float,
    emissive: Vec3Color = Vec3Color(0f, 0f, 0f),
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (rotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotYDeg, 0f, 1f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitMesh(coneMesh, col, spec, emissive, col)
  }

  private fun drawTorus(
    x: Float, y: Float, z: Float,
    sx: Float, sy: Float, sz: Float,
    rotXDeg: Float,
    rotYDeg: Float,
    col: Vec3Color,
    spec: Float,
    emissive: Vec3Color = Vec3Color(0f, 0f, 0f),
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (rotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotYDeg, 0f, 1f, 0f)
    if (rotXDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotXDeg, 1f, 0f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitMesh(torusMesh, col, spec, emissive, col)
  }

  private fun submitMesh(
    mesh: Mesh,
    col: Vec3Color,
    spec: Float,
    emissive: Vec3Color,
    rim: Vec3Color,
  ) {
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
    if (Matrix.invertM(invMatrix, 0, modelMatrix, 0)) {
      Matrix.transposeM(normalMatrix, 0, invMatrix, 0)
    } else {
      Matrix.setIdentityM(normalMatrix, 0)
    }
    GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(uModelMatrix, 1, false, modelMatrix, 0)
    GLES20.glUniformMatrix4fv(uNormalMatrix, 1, false, normalMatrix, 0)
    GLES20.glUniform4f(uColor, col.r, col.g, col.b, 1.0f)
    GLES20.glUniform3f(uEmissive, emissive.r, emissive.g, emissive.b)
    GLES20.glUniform1f(uSpecular, spec)
    GLES20.glUniform3f(uRimColor, rim.r * 0.55f + 0.2f, rim.g * 0.55f + 0.2f, rim.b * 0.55f + 0.2f)
    mesh.draw(aPos, aNorm)
  }
}
