package com.wakeup.esmoglogger.data

import android.util.Log
import org.json.JSONObject
import com.github.luben.zstd.Zstd
import org.json.JSONArray
import java.nio.charset.StandardCharsets

object JsonCompressor {
    private const val TAG = "JsonCompressor"

    // Compress a JSONObject using Zstandard
    fun compressJson(jsonObject: JSONArray): ByteArray? {
        return try {
            // Convert JSONObject to String, then to ByteArray
            val jsonString = jsonObject.toString()
            val inputBytes = jsonString.toByteArray(StandardCharsets.UTF_8)

            // Compress using Zstd with default compression level (3)
            val compressedBytes = Zstd.compress(inputBytes)
            compressedBytes
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed: ${e.message}", e)
            null
        }
    }

    // Decompress a ByteArray back to a JSONObject
    fun decompressJson(compressedBytes: ByteArray): JSONArray? {
        return try {
            // Decompress the bytes using Zstd
            val decompressedBytes = Zstd.decompress(compressedBytes, estimateOriginalSize(compressedBytes))
            val jsonString = String(decompressedBytes, StandardCharsets.UTF_8)

            // Convert back to JSONObject
            JSONArray(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Decompression failed: ${e.message}", e)
            null
        }
    }

    // Estimate the original size for decompression (Zstd requires a size hint)
    private fun estimateOriginalSize(compressedBytes: ByteArray): Int {
        // Zstd requires the decompressed size to be known or estimated.
        // A safe estimate is 10x the compressed size, but adjust based on your data.
        return compressedBytes.size * 10
    }
}
