package com.example.roaddamagedetector.data.model

import com.example.roaddamagedetector.DetectionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RoadDamageReport(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imagePath: String,
    val detections: List<DetectionResult> = emptyList(),
    val userNotes: String = "",
    val severity: String = "Medium",
    val status: String = "Saved"
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val primaryDamageClass: String
        get() {
            if (detections.isEmpty()) return "No Damage Detected"
            val topDetection = detections.maxByOrNull { it.score }
            return topDetection?.className ?: "Damage Detected"
        }

    val hasGps: Boolean
        get() = latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0

    val formattedGps: String
        get() {
            return if (hasGps) {
                String.format(Locale.US, "Lat %.5f, Lon %.5f", latitude, longitude)
            } else {
                "Location Unavailable"
            }
        }
}
