package com.hitbosss.presentation.feature.hit

import android.content.Context
import android.graphics.Matrix
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.MatrixTransformation

/**
 * Efectos para el outro del export de HIT (para que no sea soso al colgarlo en redes):
 * un zoom lento (Ken Burns) y un glitch al entrar.
 */

/** Zoom lento: escala de 1.0 a [maxZoom] en [durationSec] segundos (tiempo relativo al outro). */
@UnstableApi
fun slowZoom(durationSec: Float = 3f, maxZoom: Float = 1.12f): MatrixTransformation {
    var firstUs = -1L
    return MatrixTransformation { presentationTimeUs ->
        if (firstUs < 0) firstUs = presentationTimeUs
        val t = ((presentationTimeUs - firstUs) / 1_000_000f / durationSec).coerceIn(0f, 1f)
        val z = 1f + (maxZoom - 1f) * t
        Matrix().apply { postScale(z, z) }
    }
}

/** Glitch (separación RGB + desplazamiento por bandas) fuerte al inicio y que decae en [intenseSec]. */
@UnstableApi
class GlitchEffect(private val intenseSec: Float = 0.45f) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        GlitchShaderProgram(intenseSec)
}

@UnstableApi
private class GlitchShaderProgram(private val intenseSec: Float) :
    BaseGlShaderProgram(/* useHighPrecisionColorComponents= */ false, /* texturePoolCapacity= */ 1) {

    private var firstUs = -1L
    private val program = GlProgram(VERTEX, FRAGMENT)

    override fun configure(inputWidth: Int, inputHeight: Int): Size = Size(inputWidth, inputHeight)

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        if (firstUs < 0) firstUs = presentationTimeUs
        val t = (presentationTimeUs - firstUs) / 1_000_000f
        try {
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatUniform("uTime", t)
            program.setFloatUniform("uIntense", intenseSec)
            program.setBufferAttribute("aFramePosition", GlUtil.getNormalizedCoordinateBounds(), GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE)
            program.setBufferAttribute("aTexSamplingCoord", GlUtil.getTextureCoordinateBounds(), GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE)
            program.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GlUtil.checkGlError()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        runCatching { program.delete() }
    }

    companion object {
        private const val VERTEX = """#version 100
attribute vec4 aFramePosition;
attribute vec4 aTexSamplingCoord;
varying vec2 vTex;
void main() {
  gl_Position = aFramePosition;
  vTex = aTexSamplingCoord.xy;
}
"""
        private const val FRAGMENT = """#version 100
precision mediump float;
uniform sampler2D uTexSampler;
uniform float uTime;
uniform float uIntense;
varying vec2 vTex;
float rand(float x){ return fract(sin(x * 12.9898) * 43758.5453); }
void main() {
  float g = clamp(1.0 - uTime / uIntense, 0.0, 1.0);
  g = g * g;
  vec2 uv = vTex;
  // Desplazamiento horizontal por bandas (slice tearing) que cambia en el tiempo.
  float band = floor(uv.y * 18.0);
  float n = rand(band + floor(uTime * 30.0));
  uv.x += (n - 0.5) * 0.12 * g;
  // Separación de canales RGB.
  float sp = 0.02 * g;
  float r = texture2D(uTexSampler, vec2(uv.x + sp, uv.y)).r;
  float gg = texture2D(uTexSampler, uv).g;
  float b = texture2D(uTexSampler, vec2(uv.x - sp, uv.y)).b;
  gl_FragColor = vec4(r, gg, b, 1.0);
}
"""
    }
}
