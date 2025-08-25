package com.wakeup.esmoglogger.data

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileWriter

class JsonViewModel : ViewModel() {
    @SuppressLint("ObsoleteSdkInt")
    fun saveJsonToStorage(contentResolver: ContentResolver, context: android.content.Context,
                          filePath: String, recording: Recording) {
        try {
            val fileName = File(filePath).name
            val jsonString = recording.toJson().toString()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                    Toast.makeText(context, "JSON-file saved in Documents/${fileName}", Toast.LENGTH_LONG).show()
                } ?: throw Exception("Failed to save file")
            } else {
                val file = File(filePath)
                file.parentFile?.mkdirs()
                FileWriter(file).use { writer ->
                    writer.write(jsonString)
                }
                Toast.makeText(context, "JSON-file saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
            recording.setSaved(fileName, File(filePath).length())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
