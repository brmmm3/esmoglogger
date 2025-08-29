package com.wakeup.esmoglogger.data

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileWriter

class JsonViewModel : ViewModel() {
    @SuppressLint("ObsoleteSdkInt")
    fun saveJsonToStorage(context: android.content.Context, filePath: String, recording: Recording) {
        try {
            val fileName = File(filePath).name
            val jsonString = recording.toJson().toString()
            val file = File(context.filesDir, filePath)

            file.parentFile?.mkdirs()
            FileWriter(file).use { writer ->
                writer.write(jsonString)
            }
            Toast.makeText(context, "JSON-file saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            recording.setSaved(fileName, File(filePath).length())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
