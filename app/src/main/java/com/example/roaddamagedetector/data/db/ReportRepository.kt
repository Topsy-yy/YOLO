package com.example.roaddamagedetector.data.db

import android.content.Context
import android.graphics.Bitmap
import com.example.roaddamagedetector.data.model.RoadDamageReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ReportRepository(private val context: Context) {

    private val dbHelper = ReportDatabaseHelper(context)

    suspend fun saveReport(report: RoadDamageReport): Long = withContext(Dispatchers.IO) {
        dbHelper.insertReport(report)
    }

    suspend fun getAllReports(): List<RoadDamageReport> = withContext(Dispatchers.IO) {
        dbHelper.getAllReports()
    }

    suspend fun getReportById(id: Long): RoadDamageReport? = withContext(Dispatchers.IO) {
        dbHelper.getReportById(id)
    }

    suspend fun deleteReport(id: Long): Boolean = withContext(Dispatchers.IO) {
        val report = dbHelper.getReportById(id)
        if (report != null) {
            // Delete image file if stored in internal app directory
            try {
                val file = File(report.imagePath)
                if (file.exists() && file.absolutePath.startsWith(context.filesDir.absolutePath)) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dbHelper.deleteReport(id)
    }

    suspend fun saveBitmapToAppStorage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val reportsDir = File(context.filesDir, "report_images")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val fileName = "damage_${System.currentTimeMillis()}.jpg"
        val imageFile = File(reportsDir, fileName)

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        imageFile.absolutePath
    }
}
