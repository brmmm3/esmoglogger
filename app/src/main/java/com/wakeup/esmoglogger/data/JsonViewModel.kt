package com.wakeup.esmoglogger.data

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
    fun saveJsonToStorage(contentResolver: ContentResolver, context: android.content.Context,
                          fileName: String, dataSeries: DataSeries) {
        try {
            val jsonString = dataSeries.toJson().toString()

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
                val sdCard = Environment.getExternalStorageDirectory()
                val file = File(sdCard, "${Environment.DIRECTORY_DOCUMENTS}/${fileName}")
                file.parentFile?.mkdirs()
                FileWriter(file).use { writer ->
                    writer.write(jsonString)
                }
                Toast.makeText(context, "JSON-file saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
