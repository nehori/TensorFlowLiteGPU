package com.example.tensorflowlitegpu

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class OpenCLDetector {
    fun detectOpenCLSupport(): OpenCLInfo {
        val openCLInfo = OpenCLInfo()

        try {
            val openCLPaths = listOf(
                "/vendor/lib/libOpenCL.so",
                "/vendor/lib64/libOpenCL.so",
                "/system/lib/libOpenCL.so",
                "/system/lib64/libOpenCL.so"
            )

            openCLPaths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    openCLInfo.libraryPath = path
                    openCLInfo.isAvailable = true
                    Log.d("OpenCL", "Found OpenCL library at: $path")
                }
            }

            openCLInfo.gpuInfo = getGPUInfo()

        } catch (e: Exception) {
            Log.e("OpenCL", "Error detecting OpenCL: ${e.message}")
        }

        return openCLInfo
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
}

data class OpenCLInfo(
    var isAvailable: Boolean = false,
    var libraryPath: String = "",
    var gpuInfo: String = ""
)