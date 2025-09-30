package com.example.tensorflowlitegpu

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

class TensorFlowLiteGPUHandler(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    fun runDummyInference() {
        Log.d("TFLiteGPU", "Creating dummy model for GPU test...")

        try {
            // 簡単なダミーモデルを作成してGPUで実行
            val modelBuffer = createDummyModel()

            val options = Interpreter.Options()
            gpuDelegate?.let { options.addDelegate(it) }

            interpreter = Interpreter(modelBuffer, options)

            Log.d("TFLiteGPU", "Running dummy inference on GPU...")

            // ダミー入力
            val input = Array(1) { Array(1) { FloatArray(1) } }
            val output = Array(1) { FloatArray(1) }

            interpreter?.run(input, output)

            Log.d("TFLiteGPU", "GPU inference completed successfully")

        } catch (e: Exception) {
            Log.e("TFLiteGPU", "GPU inference failed: ${e.message}")
        }
    }

    private fun createDummyModel(): ByteBuffer {
        // 最小限のTensorFlow Liteモデルを作成
        // 実際の実装では、assetsからモデルファイルを読み込む
        return ByteBuffer.allocate(1024)
    }

    fun initializeGPUDelegate(): Boolean {
        return try {
            val compatList = CompatibilityList()

            val options = GpuDelegate.Options().apply {
                setPrecisionLossAllowed(true)
                setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER)
                // setSerializationDir(context.cacheDir) // この行を削除
            }

            if (compatList.isDelegateSupportedOnThisDevice) {
                gpuDelegate = GpuDelegate(options)
                Log.d("TFLite", "GPU delegate initialized successfully")
                true
            } else {
                Log.w("TFLite", "GPU delegate not supported on this device")
                false
            }
        } catch (e: Exception) {
            Log.e("TFLite", "Failed to initialize GPU delegate: ${e.message}")
            false
        }
    }

    fun cleanup() {
        interpreter?.close()
        gpuDelegate?.close()
    }
}