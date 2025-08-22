package com.wakeup.esmoglogger.data

import android.util.Log
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun ByteArray.toHex(): String = joinToString("") { it.toUByte().toString(16).padStart(2, '0') }

fun String.hexToByteArray(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

object JsonCompressor {
    private const val TAG = "JsonCompressor"

    fun compressJson(jsonObject: JSONArray): String? {
        return try {
            val jsonString = jsonObject.toString()
            val inputBytes = jsonString.toByteArray(StandardCharsets.UTF_8)

            ByteArrayOutputStream().use { baos ->
                ZipOutputStream(baos).use { zos ->
                    val entry = ZipEntry("data.json")
                    zos.putNextEntry(entry)
                    zos.write(inputBytes)
                    zos.closeEntry()
                    zos.finish()
                }
                Log.d(TAG, "Compressed JSONArray to ByteArray")
                baos.toByteArray().toHex()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed: ${e.message}", e)
            null
        }
    }

    // Decompress a HEX string back to a JSONObject
    fun decompressJson(hex: String): JSONArray? {
        return try {
            val compressedBytes = hex.hexToByteArray()
            val decompressedBytes = ByteArrayInputStream(compressedBytes).use { bais ->
                ZipInputStream(bais).use { zis ->
                    zis.nextEntry?.let {
                        zis.readBytes()
                    } ?: throw IllegalStateException("No entries in ZIP")
                }
            }
            val jsonString = String(decompressedBytes, StandardCharsets.UTF_8)

            // Convert back to JSONObject
            JSONArray(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Decompression failed: ${e.message}", e)
            null
        }
    }
}
