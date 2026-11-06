package com.example.smooth60

import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

/**
 * RIFE v4.9 ONNX single-intermediate-frame runner.
 *
 * Expected model contract:
 *   img0:     float32 [1, 3, H, W], RGB planar, range 0..1
 *   img1:     float32 [1, 3, H, W], RGB planar, range 0..1
 *   timestep: float32 [1], normally 0.5
 *   output:   float32 [1, 3, H, W], RGB planar, range 0..1
 */
class RifeOnnxInterpolator(
    private val modelPath: String
) : AutoCloseable {
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(
        modelPath,
        OrtSession.SessionOptions()
    )

    fun interpolate(first: Bitmap, second: Bitmap, timestep: Float = 0.5f): Bitmap {
        require(first.width == second.width && first.height == second.height) {
            "Input frames must have identical dimensions"
        }
        require(timestep in 0f..1f)

        val srcW = first.width
        val srcH = first.height
        val paddedW = ((srcW + 31) / 32) * 32
        val paddedH = ((srcH + 31) / 32) * 32
        val a = toPlanarRgb(first, paddedW, paddedH)
        val b = toPlanarRgb(second, paddedW, paddedH)
        val shape = longArrayOf(1, 3, paddedH.toLong(), paddedW.toLong())
        val tShape = longArrayOf(1)

        OnnxTensor.createTensor(env, FloatBuffer.wrap(a), shape).use { t0 ->
            OnnxTensor.createTensor(env, FloatBuffer.wrap(b), shape).use { t1 ->
                OnnxTensor.createTensor(env, FloatBuffer.wrap(floatArrayOf(timestep)), tShape).use { tt ->
                    val inputs = mapOf("img0" to t0, "img1" to t1, "timestep" to tt)
                    session.run(inputs).use { result ->
                        val values = flattenOutput(result[0].value)
                        return fromPlanarRgb(values, paddedW, paddedH, srcW, srcH)
                    }
                }
            }
        }
    }

    private fun toPlanarRgb(bitmap: Bitmap, outW: Int, outH: Int): FloatArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val out = FloatArray(3 * outW * outH)
        val plane = outW * outH
        for (y in 0 until outH) {
            val sy = y.coerceAtMost(bitmap.height - 1)
            for (x in 0 until outW) {
                val sx = x.coerceAtMost(bitmap.width - 1)
                val c = pixels[sy * bitmap.width + sx]
                val i = y * outW + x
                out[i] = ((c shr 16) and 0xff) / 255f
                out[plane + i] = ((c shr 8) and 0xff) / 255f
                out[2 * plane + i] = (c and 0xff) / 255f
            }
        }
        return out
    }

    private fun flattenOutput(value: Any): FloatArray {
        val result = ArrayList<Float>()
        fun visit(v: Any?) {
            when (v) {
                is FloatArray -> v.forEach(result::add)
                is Array<*> -> v.forEach(::visit)
                is Number -> result.add(v.toFloat())
                else -> error("Unsupported RIFE output type: ${v?.javaClass}")
            }
        }
        visit(value)
        return result.toFloatArray()
    }

    private fun fromPlanarRgb(
        data: FloatArray, paddedW: Int, paddedH: Int, outW: Int, outH: Int
    ): Bitmap {
        val plane = paddedW * paddedH
        require(data.size >= plane * 3) { "Unexpected RIFE output tensor size" }
        val pixels = IntArray(outW * outH)
        for (y in 0 until outH) {
            for (x in 0 until outW) {
                val i = y * paddedW + x
                val r = (data[i].coerceIn(0f, 1f) * 255f).toInt()
                val g = (data[plane + i].coerceIn(0f, 1f) * 255f).toInt()
                val b = (data[2 * plane + i].coerceIn(0f, 1f) * 255f).toInt()
                pixels[y * outW + x] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(pixels, outW, outH, Bitmap.Config.ARGB_8888)
    }

    override fun close() {
        session.close()
    }
}
