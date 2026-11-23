package com.example.game.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Mesh(vertices: FloatArray) {
  val vertexCount: Int = vertices.size / 6
  private val vboId: Int

  init {
    val buffers = IntArray(1)
    GLES20.glGenBuffers(1, buffers, 0)
    vboId = buffers[0]
    val fb = ByteBuffer.allocateDirect(vertices.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .apply {
        put(vertices)
        position(0)
      }
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.size * 4, fb, GLES20.GL_STATIC_DRAW)
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
  }

  fun draw(posAttr: Int, normAttr: Int) {
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
    GLES20.glEnableVertexAttribArray(posAttr)
    GLES20.glVertexAttribPointer(posAttr, 3, GLES20.GL_FLOAT, false, 24, 0)
    GLES20.glEnableVertexAttribArray(normAttr)
    GLES20.glVertexAttribPointer(normAttr, 3, GLES20.GL_FLOAT, false, 24, 12)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
  }
}

object MeshBuilder {
  fun createUnitCube(): Mesh {
    val data = floatArrayOf(
      // Front (+Z)
      -0.5f, -0.5f,  0.5f, 0f, 0f, 1f,   0.5f, -0.5f,  0.5f, 0f, 0f, 1f,   0.5f,  0.5f,  0.5f, 0f, 0f, 1f,
      -0.5f, -0.5f,  0.5f, 0f, 0f, 1f,   0.5f,  0.5f,  0.5f, 0f, 0f, 1f,  -0.5f,  0.5f,  0.5f, 0f, 0f, 1f,
      // Back (-Z)
       0.5f, -0.5f, -0.5f, 0f, 0f, -1f, -0.5f, -0.5f, -0.5f, 0f, 0f, -1f, -0.5f,  0.5f, -0.5f, 0f, 0f, -1f,
       0.5f, -0.5f, -0.5f, 0f, 0f, -1f, -0.5f,  0.5f, -0.5f, 0f, 0f, -1f,  0.5f,  0.5f, -0.5f, 0f, 0f, -1f,
      // Top (+Y)
      -0.5f,  0.5f,  0.5f, 0f, 1f, 0f,   0.5f,  0.5f,  0.5f, 0f, 1f, 0f,   0.5f,  0.5f, -0.5f, 0f, 1f, 0f,
      -0.5f,  0.5f,  0.5f, 0f, 1f, 0f,   0.5f,  0.5f, -0.5f, 0f, 1f, 0f,  -0.5f,  0.5f, -0.5f, 0f, 1f, 0f,
      // Bottom (-Y)
      -0.5f, -0.5f, -0.5f, 0f, -1f, 0f,  0.5f, -0.5f, -0.5f, 0f, -1f, 0f,  0.5f, -0.5f,  0.5f, 0f, -1f, 0f,
      -0.5f, -0.5f, -0.5f, 0f, -1f, 0f,  0.5f, -0.5f,  0.5f, 0f, -1f, 0f, -0.5f, -0.5f,  0.5f, 0f, -1f, 0f,
      // Right (+X)
       0.5f, -0.5f,  0.5f, 1f, 0f, 0f,   0.5f, -0.5f, -0.5f, 1f, 0f, 0f,   0.5f,  0.5f, -0.5f, 1f, 0f, 0f,
       0.5f, -0.5f,  0.5f, 1f, 0f, 0f,   0.5f,  0.5f, -0.5f, 1f, 0f, 0f,   0.5f,  0.5f,  0.5f, 1f, 0f, 0f,
      // Left (-X)
      -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, -0.5f, -0.5f,  0.5f, -1f, 0f, 0f, -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,
      -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, -0.5f,  0.5f,  0.5f, -1f, 0f, 0f, -0.5f,  0.5f, -0.5f, -1f, 0f, 0f,
    )
    return Mesh(data)
  }

  fun createSphere(stacks: Int = 14, slices: Int = 18): Mesh {
    val list = ArrayList<Float>(stacks * slices * 36)
    val r = 0.5f
    for (i in 0 until stacks) {
      val phi1 = (PI * i / stacks - PI / 2.0).toFloat()
      val phi2 = (PI * (i + 1) / stacks - PI / 2.0).toFloat()
      for (j in 0 until slices) {
        val th1 = (2.0 * PI * j / slices).toFloat()
        val th2 = (2.0 * PI * (j + 1) / slices).toFloat()

        val p1 = spherePt(r, phi1, th1)
        val p2 = spherePt(r, phi1, th2)
        val p3 = spherePt(r, phi2, th2)
        val p4 = spherePt(r, phi2, th1)

        addTri(list, p1, p2, p3)
        addTri(list, p1, p3, p4)
      }
    }
    return Mesh(list.toFloatArray())
  }

  private fun spherePt(r: Float, phi: Float, theta: Float): FloatArray {
    val nx = cos(phi) * cos(theta)
    val ny = sin(phi)
    val nz = cos(phi) * sin(theta)
    return floatArrayOf(r * nx, r * ny, r * nz, nx, ny, nz)
  }

  fun createCylinder(segments: Int = 18): Mesh {
    val list = ArrayList<Float>(segments * 72)
    val r = 0.5f
    val yTop = 0.5f
    val yBot = -0.5f
    for (i in 0 until segments) {
      val a1 = (2.0 * PI * i / segments).toFloat()
      val a2 = (2.0 * PI * (i + 1) / segments).toFloat()
      val c1 = cos(a1)
      val s1 = sin(a1)
      val c2 = cos(a2)
      val s2 = sin(a2)

      val b1 = floatArrayOf(r * c1, yBot, r * s1, c1, 0f, s1)
      val b2 = floatArrayOf(r * c2, yBot, r * s2, c2, 0f, s2)
      val t2 = floatArrayOf(r * c2, yTop, r * s2, c2, 0f, s2)
      val t1 = floatArrayOf(r * c1, yTop, r * s1, c1, 0f, s1)
      addTri(list, b1, t1, t2)
      addTri(list, b1, t2, b2)

      // Top cap
      val tc = floatArrayOf(0f, yTop, 0f, 0f, 1f, 0f)
      val tp1 = floatArrayOf(r * c1, yTop, r * s1, 0f, 1f, 0f)
      val tp2 = floatArrayOf(r * c2, yTop, r * s2, 0f, 1f, 0f)
      addTri(list, tc, tp2, tp1)

      // Bottom cap
      val bc = floatArrayOf(0f, yBot, 0f, 0f, -1f, 0f)
      val bp1 = floatArrayOf(r * c1, yBot, r * s1, 0f, -1f, 0f)
      val bp2 = floatArrayOf(r * c2, yBot, r * s2, 0f, -1f, 0f)
      addTri(list, bc, bp1, bp2)
    }
    return Mesh(list.toFloatArray())
  }

  fun createCone(segments: Int = 16): Mesh {
    val list = ArrayList<Float>(segments * 36)
    val r = 0.5f
    for (i in 0 until segments) {
      val a1 = (2.0 * PI * i / segments).toFloat()
      val a2 = (2.0 * PI * (i + 1) / segments).toFloat()
      val c1 = cos(a1)
      val s1 = sin(a1)
      val c2 = cos(a2)
      val s2 = sin(a2)
      val mid = (a1 + a2) * 0.5f
      val cm = cos(mid)
      val sm = sin(mid)

      val apex = floatArrayOf(0f, 0.5f, 0f, cm * 0.89f, 0.45f, sm * 0.89f)
      val p1 = floatArrayOf(r * c1, -0.5f, r * s1, c1 * 0.89f, 0.45f, s1 * 0.89f)
      val p2 = floatArrayOf(r * c2, -0.5f, r * s2, c2 * 0.89f, 0.45f, s2 * 0.89f)
      addTri(list, p1, apex, p2)

      val bc = floatArrayOf(0f, -0.5f, 0f, 0f, -1f, 0f)
      val bp1 = floatArrayOf(r * c1, -0.5f, r * s1, 0f, -1f, 0f)
      val bp2 = floatArrayOf(r * c2, -0.5f, r * s2, 0f, -1f, 0f)
      addTri(list, bc, bp1, bp2)
    }
    return Mesh(list.toFloatArray())
  }

  fun createTorus(majorSegs: Int = 22, minorSegs: Int = 10, minorRadius: Float = 0.14f): Mesh {
    val majorR = 0.5f
    val list = ArrayList<Float>(majorSegs * minorSegs * 36)
    for (i in 0 until majorSegs) {
      val u1 = (2.0 * PI * i / majorSegs).toFloat()
      val u2 = (2.0 * PI * (i + 1) / majorSegs).toFloat()
      for (j in 0 until minorSegs) {
        val v1 = (2.0 * PI * j / minorSegs).toFloat()
        val v2 = (2.0 * PI * (j + 1) / minorSegs).toFloat()

        val p1 = torusPt(majorR, minorRadius, u1, v1)
        val p2 = torusPt(majorR, minorRadius, u2, v1)
        val p3 = torusPt(majorR, minorRadius, u2, v2)
        val p4 = torusPt(majorR, minorRadius, u1, v2)
        addTri(list, p1, p2, p3)
        addTri(list, p1, p3, p4)
      }
    }
    return Mesh(list.toFloatArray())
  }

  private fun torusPt(majorR: Float, minorR: Float, u: Float, v: Float): FloatArray {
    val cu = cos(u)
    val su = sin(u)
    val cv = cos(v)
    val sv = sin(v)
    val x = (majorR + minorR * cv) * cu
    val y = minorR * sv
    val z = (majorR + minorR * cv) * su
    return floatArrayOf(x, y, z, cv * cu, sv, cv * su)
  }

  private fun addTri(list: MutableList<Float>, a: FloatArray, b: FloatArray, c: FloatArray) {
    for (v in a) list.add(v)
    for (v in b) list.add(v)
    for (v in c) list.add(v)
  }
}
