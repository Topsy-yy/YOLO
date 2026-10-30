package com.example.roaddamagedetector.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.roaddamagedetector.DetectionResult
import com.example.roaddamagedetector.data.model.RoadDamageReport
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ReportDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val gson = Gson()

    companion object {
        private const val DATABASE_NAME = "road_damage_reports.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_REPORTS = "reports"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_IMAGE_PATH = "image_path"
        private const val COLUMN_DETECTIONS_JSON = "detections_json"
        private const val COLUMN_USER_NOTES = "user_notes"
        private const val COLUMN_SEVERITY = "severity"
        private const val COLUMN_STATUS = "status"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_REPORTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_IMAGE_PATH TEXT NOT NULL,
                $COLUMN_DETECTIONS_JSON TEXT,
                $COLUMN_USER_NOTES TEXT,
                $COLUMN_SEVERITY TEXT,
                $COLUMN_STATUS TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTS")
        onCreate(db)
    }

    fun insertReport(report: RoadDamageReport): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TIMESTAMP, report.timestamp)
            put(COLUMN_LATITUDE, report.latitude)
            put(COLUMN_LONGITUDE, report.longitude)
            put(COLUMN_IMAGE_PATH, report.imagePath)
            put(COLUMN_DETECTIONS_JSON, gson.toJson(report.detections))
            put(COLUMN_USER_NOTES, report.userNotes)
            put(COLUMN_SEVERITY, report.severity)
            put(COLUMN_STATUS, report.status)
        }
        val id = db.insert(TABLE_REPORTS, null, values)
        db.close()
        return id
    }

    fun getAllReports(): List<RoadDamageReport> {
        val reports = mutableListOf<RoadDamageReport>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_REPORTS ORDER BY $COLUMN_TIMESTAMP DESC", null)

        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val timeIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val latIndex = cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)
            val lonIndex = cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)
            val pathIndex = cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)
            val jsonIndex = cursor.getColumnIndexOrThrow(COLUMN_DETECTIONS_JSON)
            val notesIndex = cursor.getColumnIndexOrThrow(COLUMN_USER_NOTES)
            val severityIndex = cursor.getColumnIndexOrThrow(COLUMN_SEVERITY)
            val statusIndex = cursor.getColumnIndexOrThrow(COLUMN_STATUS)

            val typeToken = object : TypeToken<List<DetectionResult>>() {}.type

            do {
                val id = cursor.getLong(idIndex)
                val timestamp = cursor.getLong(timeIndex)
                val latitude = if (cursor.isNull(latIndex)) null else cursor.getDouble(latIndex)
                val longitude = if (cursor.isNull(lonIndex)) null else cursor.getDouble(lonIndex)
                val imagePath = cursor.getString(pathIndex)
                val jsonStr = cursor.getString(jsonIndex)
                val userNotes = cursor.getString(notesIndex) ?: ""
                val severity = cursor.getString(severityIndex) ?: "Medium"
                val status = cursor.getString(statusIndex) ?: "Saved"

                val detections: List<DetectionResult> = if (!jsonStr.isNullOrEmpty()) {
                    try {
                        gson.fromJson(jsonStr, typeToken)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()

                reports.add(
                    RoadDamageReport(
                        id = id,
                        timestamp = timestamp,
                        latitude = latitude,
                        longitude = longitude,
                        imagePath = imagePath,
                        detections = detections,
                        userNotes = userNotes,
                        severity = severity,
                        status = status
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return reports
    }

    fun getReportById(id: Long): RoadDamageReport? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_REPORTS WHERE $COLUMN_ID = ?", arrayOf(id.toString()))
        var report: RoadDamageReport? = null

        if (cursor.moveToFirst()) {
            val timeIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val latIndex = cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)
            val lonIndex = cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)
            val pathIndex = cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)
            val jsonIndex = cursor.getColumnIndexOrThrow(COLUMN_DETECTIONS_JSON)
            val notesIndex = cursor.getColumnIndexOrThrow(COLUMN_USER_NOTES)
            val severityIndex = cursor.getColumnIndexOrThrow(COLUMN_SEVERITY)
            val statusIndex = cursor.getColumnIndexOrThrow(COLUMN_STATUS)

            val typeToken = object : TypeToken<List<DetectionResult>>() {}.type

            val timestamp = cursor.getLong(timeIndex)
            val latitude = if (cursor.isNull(latIndex)) null else cursor.getDouble(latIndex)
            val longitude = if (cursor.isNull(lonIndex)) null else cursor.getDouble(lonIndex)
            val imagePath = cursor.getString(pathIndex)
            val jsonStr = cursor.getString(jsonIndex)
            val userNotes = cursor.getString(notesIndex) ?: ""
            val severity = cursor.getString(severityIndex) ?: "Medium"
            val status = cursor.getString(statusIndex) ?: "Saved"

            val detections: List<DetectionResult> = if (!jsonStr.isNullOrEmpty()) {
                try {
                    gson.fromJson(jsonStr, typeToken)
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()

            report = RoadDamageReport(
                id = id,
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude,
                imagePath = imagePath,
                detections = detections,
                userNotes = userNotes,
                severity = severity,
                status = status
            )
        }

        cursor.close()
        db.close()
        return report
    }

    fun deleteReport(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_REPORTS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
        return rows > 0
    }
}
