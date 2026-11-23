package com.example.game.gl

import android.opengl.GLES20
import android.util.Log

class ShaderProgram(vertexSource: String, fragmentSource: String) {
  val programId: Int

  init {
    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    programId = GLES20.glCreateProgram().also { prog ->
      GLES20.glAttachShader(prog, vs)
      GLES20.glAttachShader(prog, fs)
      GLES20.glLinkProgram(prog)
      val status = IntArray(1)
      GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
      if (status[0] == 0) {
        Log.e("ShaderProgram", "Link failed: ${GLES20.glGetProgramInfoLog(prog)}")
      }
    }
    GLES20.glDeleteShader(vs)
    GLES20.glDeleteShader(fs)
  }

  private fun compileShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val status = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
    if (status[0] == 0) {
      Log.e("ShaderProgram", "Compile failed: ${GLES20.glGetShaderInfoLog(shader)}")
    }
    return shader
  }

  fun use() = GLES20.glUseProgram(programId)
  fun attrib(name: String): Int = GLES20.glGetAttribLocation(programId, name)
  fun uniform(name: String): Int = GLES20.glGetUniformLocation(programId, name)

  companion object {
    const val LIT_VERTEX_SHADER = """
      uniform mat4 uMVPMatrix;
      uniform mat4 uModelMatrix;
      uniform mat4 uNormalMatrix;

      attribute vec3 aPosition;
      attribute vec3 aNormal;

      varying vec3 vWorldPos;
      varying vec3 vWorldNormal;

      void main() {
        vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
        vWorldPos = worldPos.xyz;
        vWorldNormal = normalize((uNormalMatrix * vec4(aNormal, 0.0)).xyz);
        gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
      }
    """

    const val LIT_FRAGMENT_SHADER = """
      precision mediump float;

      uniform vec4 uColor;
      uniform vec3 uEmissive;
      uniform float uSpecular;
      uniform vec3 uRimColor;
      uniform vec3 uLightDir;
      uniform vec3 uCameraPos;
      uniform vec3 uFogColor;
      uniform float uFogDensity;

      varying vec3 vWorldPos;
      varying vec3 vWorldNormal;

      void main() {
        vec3 N = normalize(vWorldNormal);
        vec3 L = normalize(uLightDir);
        vec3 V = normalize(uCameraPos - vWorldPos);
        vec3 H = normalize(L + V);

        // Hemisphere ambient + key sunlight + subtle back fill
        float hemi = 0.5 + 0.5 * N.y;
        vec3 ambient = mix(vec3(0.20, 0.22, 0.30), vec3(0.46, 0.48, 0.52), hemi);

        float diff = max(dot(N, L), 0.0);
        vec3 fillDir = normalize(vec3(-0.6, 0.3, 0.5));
        float fill = max(dot(N, fillDir), 0.0) * 0.25;

        float spec = pow(max(dot(N, H), 0.0), 28.0) * uSpecular;
        float rim = pow(1.0 - max(dot(N, V), 0.0), 2.8) * 0.48;

        vec3 lit = uColor.rgb * (ambient + diff * 0.85 + fill)
                 + vec3(1.0, 0.96, 0.88) * spec
                 + uRimColor * rim
                 + uEmissive;

        float dist = length(uCameraPos - vWorldPos);
        float fogFactor = clamp(1.0 - exp(-max(dist - 12.0, 0.0) * uFogDensity), 0.0, 0.65);
        vec3 finalRgb = mix(lit, uFogColor, fogFactor);

        gl_FragColor = vec4(finalRgb, uColor.a);
      }
    """

    const val SKY_VERTEX_SHADER = """
      attribute vec2 aPosition;
      varying float vGrad;
      void main() {
        vGrad = aPosition.y * 0.5 + 0.5;
        gl_Position = vec4(aPosition, 0.999, 1.0);
      }
    """

    const val SKY_FRAGMENT_SHADER = """
      precision mediump float;
      uniform vec3 uSkyBottom;
      uniform vec3 uSkyTop;
      varying float vGrad;
      void main() {
        vec3 col = mix(uSkyBottom, uSkyTop, pow(clamp(vGrad, 0.0, 1.0), 0.82));
        gl_FragColor = vec4(col, 1.0);
      }
    """
  }
}
