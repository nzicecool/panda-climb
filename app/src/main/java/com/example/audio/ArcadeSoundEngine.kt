package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

enum class SoundEffect {
  JUMP,
  DOUBLE_JUMP,
  COIN,
  POWER_UP,
  SMASH,
  SPRING,
  HURT,
  SHIELD_POP,
  RIVET,
  VICTORY,
}

class ArcadeSoundEngine {
  @Volatile var enabled: Boolean = true
  private val executor = Executors.newSingleThreadExecutor()

  fun play(effect: SoundEffect) {
    if (!enabled) return
    executor.execute {
      try {
        val sampleRate = 22050
        val pcm = generatePcm(effect, sampleRate)
        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(pcm.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        val durationMs = (pcm.size * 1000L) / sampleRate + 40L
        Thread.sleep(durationMs)
        track.release()
      } catch (_: Throwable) {
        // Ignore audio hardware failures on headless test environments
      }
    }
  }

  private fun generatePcm(effect: SoundEffect, sampleRate: Int): ShortArray {
    return when (effect) {
      SoundEffect.JUMP -> sweep(sampleRate, 0.13f, 290f, 640f, 0.35f)
      SoundEffect.DOUBLE_JUMP -> sweep(sampleRate, 0.15f, 480f, 980f, 0.35f)
      SoundEffect.SPRING -> sweep(sampleRate, 0.22f, 220f, 920f, 0.4f)
      SoundEffect.COIN -> notes(sampleRate, listOf(880f to 0.06f, 1318f to 0.10f), 0.35f)
      SoundEffect.POWER_UP -> notes(sampleRate, listOf(523f to 0.07f, 659f to 0.07f, 784f to 0.07f, 1046f to 0.14f), 0.38f)
      SoundEffect.SMASH -> crunch(sampleRate, 0.16f, 0.45f)
      SoundEffect.HURT -> sweep(sampleRate, 0.26f, 360f, 110f, 0.42f)
      SoundEffect.SHIELD_POP -> notes(sampleRate, listOf(740f to 0.06f, 440f to 0.12f), 0.36f)
      SoundEffect.RIVET -> notes(sampleRate, listOf(440f to 0.06f, 880f to 0.09f), 0.38f)
      SoundEffect.VICTORY -> notes(
        sampleRate,
        listOf(523f to 0.10f, 659f to 0.10f, 784f to 0.10f, 1046f to 0.24f),
        0.4f
      )
    }
  }

  private fun sweep(sampleRate: Int, seconds: Float, startHz: Float, endHz: Float, amp: Float): ShortArray {
    val count = (sampleRate * seconds).toInt().coerceAtLeast(1)
    val out = ShortArray(count)
    var phase = 0.0
    for (i in 0 until count) {
      val t = i.toFloat() / count
      val freq = startHz + (endHz - startHz) * t
      val env = (1f - t)
      phase += 2.0 * PI * freq / sampleRate
      val wave = sin(phase)
      out[i] = (wave * env * amp * Short.MAX_VALUE).toInt().toShort()
    }
    return out
  }

  private fun notes(sampleRate: Int, seq: List<Pair<Float, Float>>, amp: Float): ShortArray {
    val totalSamples = seq.sumOf { (sampleRate * it.second).toInt() }.coerceAtLeast(1)
    val out = ShortArray(totalSamples)
    var idx = 0
    for ((freq, dur) in seq) {
      val count = (sampleRate * dur).toInt()
      var phase = 0.0
      for (i in 0 until count) {
        if (idx >= totalSamples) break
        val t = i.toFloat() / count.coerceAtLeast(1)
        val env = 1f - t * 0.7f
        phase += 2.0 * PI * freq / sampleRate
        out[idx++] = (sin(phase) * env * amp * Short.MAX_VALUE).toInt().toShort()
      }
    }
    return out
  }

  private fun crunch(sampleRate: Int, seconds: Float, amp: Float): ShortArray {
    val count = (sampleRate * seconds).toInt().coerceAtLeast(1)
    val out = ShortArray(count)
    var seed = 1337
    var phase = 0.0
    for (i in 0 until count) {
      val t = i.toFloat() / count
      seed = (seed * 1103515245 + 12345) and 0x7fffffff
      val noise = ((seed % 2000) / 1000f) - 1f
      phase += 2.0 * PI * (140f * (1f - t * 0.5f)) / sampleRate
      val punch = sin(phase).toFloat() * 0.6f + noise * 0.4f
      val env = (1f - t) * (1f - t)
      out[i] = (punch * env * amp * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return out
  }

  fun release() {
    executor.shutdownNow()
  }
}
