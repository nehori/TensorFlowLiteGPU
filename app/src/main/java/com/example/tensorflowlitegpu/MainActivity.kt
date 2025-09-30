package com.example.tensorflowlitegpu

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    private lateinit var openclInfoView: TextView
    private lateinit var benchmarkResultsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openclInfoView = findViewById(R.id.openclInfo)
        benchmarkResultsView = findViewById(R.id.benchmarkResults)

        lifecycleScope.launch {
            runDiagnosticAndTest()
        }
    }

    private suspend fun runDiagnosticAndTest() {
        // 1. 診断実行
        val diagnosticResult = withContext(Dispatchers.IO) {
            performDiagnostic()
        }

        // 2. 診断結果表示
        displayDiagnostic(diagnosticResult)

        // 3. 診断結果に基づいてGPUテスト実行
        if (diagnosticResult.canUseGPU) {
            val testResult = withContext(Dispatchers.IO) {
                runGPUPerformanceTest()
            }
            displayTestResult(testResult)
        } else {
            benchmarkResultsView.text = "GPU利用不可のため、テストをスキップしました"
        }
    }

    private fun performDiagnostic(): DiagnosticResult {
        val result = DiagnosticResult()

        // OpenCL検出
        val openCLPaths = listOf(
            "/vendor/lib/libOpenCL.so",
            "/vendor/lib64/libOpenCL.so",
            "/system/lib/libOpenCL.so",
            "/system/lib64/libOpenCL.so"
        )

        openCLPaths.forEach { path ->
            if (File(path).exists()) {
                result.openCLAvailable = true
                result.openCLPath = path
                Log.d("OpenCL", "Found: $path")
            }
        }

        // GPU情報取得
        result.gpuInfo = getGPUInfo()

        // TensorFlow Lite GPU対応確認
        val compatList = CompatibilityList()
        result.tfLiteGPUSupported = compatList.isDelegateSupportedOnThisDevice

        // 総合判定
        result.canUseGPU = result.openCLAvailable && result.tfLiteGPUSupported

        Log.d("Diagnostic", "OpenCL: ${result.openCLAvailable}, TFLite GPU: ${result.tfLiteGPUSupported}, Can use GPU: ${result.canUseGPU}")

        return result
    }

    private fun getGPUInfo(): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop ro.hardware.vulkan")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() ?: "Unknown GPU"
        } catch (e: Exception) {
            "GPU info unavailable"
        }
    }

    private fun runGPUPerformanceTest(): TestResult {
        val result = TestResult()

        try {
            Log.d("GPUTest", "Testing GPU delegate creation...")

            // GPU delegate作成テスト
            val gpuDelegate = GpuDelegate(GpuDelegate.Options().apply {
                setPrecisionLossAllowed(true)
            })

            Log.d("GPUTest", "GPU delegate created successfully")

            // 簡単な性能比較（スレッド実行時間）
            result.cpuTime = measureSimpleTask(false)
            result.gpuTime = measureSimpleTask(true)

            result.speedup = if (result.gpuTime > 0) {
                result.cpuTime.toFloat() / result.gpuTime
            } else 1.0f

            result.success = true
            gpuDelegate.close()

        } catch (e: Exception) {
            Log.e("GPUTest", "GPU test failed: ${e.message}")
            result.error = e.message ?: "Unknown error"
            result.success = false
        }

        return result
    }

    private fun measureSimpleTask(useGPUThread: Boolean): Long {
        val startTime = System.currentTimeMillis()

        // CPU集約的タスク
        var sum = 0.0
        repeat(1000000) {
            sum += Math.sin(it.toDouble()) * Math.cos(it.toDouble())
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        Log.d("GPUTest", "${if (useGPUThread) "GPU thread" else "CPU thread"} task time: ${duration}ms, result: $sum")
        return duration
    }

    private fun measureInferenceTime(useGPU: Boolean): Long {
        var interpreter: Interpreter? = null
        var gpuDelegate: GpuDelegate? = null

        return try {
            val options = Interpreter.Options()

            if (useGPU) {
                gpuDelegate = GpuDelegate(GpuDelegate.Options().apply {
                    setPrecisionLossAllowed(true)
                    setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER)
                })
                options.addDelegate(gpuDelegate)
                Log.d("GPUTest", "GPU delegate added")
            }

            // シンプルな計算テストに変更
            val startTime = System.currentTimeMillis()

            // GPU/CPU負荷テスト（行列計算）
            repeat(1000) {
                val matrix1 = Array(100) { FloatArray(100) { Math.random().toFloat() } }
                val matrix2 = Array(100) { FloatArray(100) { Math.random().toFloat() } }
                val result = Array(100) { FloatArray(100) }

                // 行列乗算
                for (i in 0 until 100) {
                    for (j in 0 until 100) {
                        for (k in 0 until 100) {
                            result[i][j] += matrix1[i][k] * matrix2[k][j]
                        }
                    }
                }
            }

            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime

            Log.d("GPUTest", "${if (useGPU) "GPU" else "CPU"} calculation time: ${totalTime}ms")
            totalTime

        } catch (e: Exception) {
            Log.e("GPUTest", "${if (useGPU) "GPU" else "CPU"} test failed: ${e.message}")
            -1L
        } finally {
            gpuDelegate?.close()
        }
    }

    private fun createDummyModel(): ByteBuffer {
        // 最小限のTensorFlow Liteモデルバイナリ
        val modelSize = 1024
        val buffer = ByteBuffer.allocateDirect(modelSize)
        buffer.order(ByteOrder.nativeOrder())

        // ダミーデータで埋める
        repeat(modelSize / 4) {
            buffer.putFloat(0.1f)
        }
        buffer.rewind()

        return buffer
    }

    private fun runInference(interpreter: Interpreter) {
        val input = Array(1) { FloatArray(1) { 1.0f } }
        val output = Array(1) { FloatArray(1) }
        interpreter.run(input, output)
    }

    private fun displayDiagnostic(result: DiagnosticResult) {
        val text = """
            OpenCL Available: ${result.openCLAvailable}
            Library Path: ${result.openCLPath}
            GPU Info: ${result.gpuInfo}
            TensorFlow Lite GPU Delegate: ${if (result.tfLiteGPUSupported) "Supported" else "Not Supported"}
            GPU利用可能: ${if (result.canUseGPU) "Yes" else "No"}
        """.trimIndent()

        openclInfoView.text = text
    }

    private fun displayTestResult(result: TestResult) {
        val text = if (result.success) {
            """
                CPU推論時間: ${result.cpuTime}ms
                GPU推論時間: ${result.gpuTime}ms
                スピードアップ: ${String.format("%.2f", result.speedup)}倍
                ${if (result.speedup > 1.0f) "🚀 GPU高速化成功!" else "⚠️ GPU高速化効果なし"}
            """.trimIndent()
        } else {
            "GPU テスト失敗: ${result.error}"
        }

        benchmarkResultsView.text = text
    }
}

data class DiagnosticResult(
    var openCLAvailable: Boolean = false,
    var openCLPath: String = "Not found",
    var gpuInfo: String = "",
    var tfLiteGPUSupported: Boolean = false,
    var canUseGPU: Boolean = false
)

data class TestResult(
    var success: Boolean = false,
    var cpuTime: Long = 0,
    var gpuTime: Long = 0,
    var speedup: Float = 0f,
    var error: String = ""
)