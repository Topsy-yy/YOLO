package com.example.roaddamagedetector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.roaddamagedetector.data.db.ReportRepository
import com.example.roaddamagedetector.data.model.RoadDamageReport
import com.example.roaddamagedetector.databinding.ActivityReportCreationBinding
import com.example.roaddamagedetector.util.LocationHelper
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min

class ReportCreationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportCreationBinding
    private lateinit var repository: ReportRepository
    private lateinit var locationHelper: LocationHelper

    private var imagePath: String? = null
    private var detections: List<DetectionResult> = emptyList()
    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ReportRepository(this)
        locationHelper = LocationHelper(this)

        // Extract Intent Data
        imagePath = intent.getStringExtra("EXTRA_IMAGE_PATH")

        // 1. Extract raw detections (boundingBox is @Transient and will be null/empty)
        val rawDetections = intent.getSerializableExtra("EXTRA_DETECTIONS") as? List<DetectionResult> ?: emptyList()

        // 2. Reconstruct the bounding boxes for the OverlayView
        detections = reconstructBoundingBoxes(rawDetections)

        setupUI()
        loadImage()
        setupSeveritySpinner()
        fetchLocation()
    }

    private fun reconstructBoundingBoxes(rawDetections: List<DetectionResult>): List<DetectionResult> {
        return rawDetections.map { det ->
            // Recalculate the RectF using the normalized center and width/height
            val left = max(0.0f, det.xCenter - det.width / 2.0f)
            val top = max(0.0f, det.yCenter - det.height / 2.0f)
            val right = min(1.0f, det.xCenter + det.width / 2.0f)
            val bottom = min(1.0f, det.yCenter + det.height / 2.0f)

            // Return a copy of the object with the reconstructed RectF
            det.copy(boundingBox = RectF(left, top, right, bottom))
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        if (detections.isEmpty()) {
            binding.tvDetectionsDetail.text = "No damage detected."
        } else {
            val details = detections.joinToString("\n") { "${it.className} (${(it.score * 100).toInt()}%)" }
            binding.tvDetectionsDetail.text = details
        }

        binding.btnRefreshGps.setOnClickListener {
            binding.tvGpsCoordinates.text = "Fetching current location..."
            fetchLocation()
        }

        binding.btnSaveReport.setOnClickListener {
            if (isSaving) return@setOnClickListener
            saveReport()
        }
    }

    private fun loadImage() {
        val path = imagePath ?: return
        try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeFile(path, options)

            if (bitmap != null) {
                binding.ivReportImage.setImageBitmap(bitmap)

                // Delay the overlay drawing until the layout has been measured
                binding.overlayView.post {
                    binding.overlayView.setResults(detections)
                }
            } else {
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSeveritySpinner() {
        val severityLevels = arrayOf("Low", "Medium", "High", "Critical")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, severityLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSeverity.adapter = adapter
        binding.spinnerSeverity.setSelection(1) // Default to "Medium"
    }

    private fun fetchLocation() {
        // CRITICAL FIX: Prevent SecurityException crash
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            binding.tvGpsCoordinates.text = "Location Unavailable (Permission Denied)"
            return
        }

        locationHelper.getCurrentLocation { location ->
            // Ensure we update UI on the main thread
            runOnUiThread {
                if (location != null) {
                    currentLat = location.latitude
                    currentLon = location.longitude
                    binding.tvGpsCoordinates.text = String.format("Lat: %.5f, Lon: %.5f", currentLat, currentLon)
                } else {
                    currentLat = null
                    currentLon = null
                    binding.tvGpsCoordinates.text = "Location Unavailable (Timeout/Disabled)"
                }
            }
        }
    }

    private fun saveReport() {
        isSaving = true
        binding.btnSaveReport.isEnabled = false
        binding.btnSaveReport.text = "Saving..."

        // Use lifecycleScope for safe background execution
        lifecycleScope.launch {
            try {
                val path = imagePath
                if (path == null || !File(path).exists()) {
                    throw IllegalStateException("Image file missing.")
                }

                val bitmap = BitmapFactory.decodeFile(path)
                val permanentPath = repository.saveBitmapToAppStorage(bitmap)

                val report = RoadDamageReport(
                    imagePath = permanentPath,
                    detections = detections,
                    latitude = currentLat,
                    longitude = currentLon,
                    userNotes = binding.etUserNotes.text.toString(),
                    severity = binding.spinnerSeverity.selectedItem.toString(),
                    status = "LOCAL_DRAFT"
                )

                repository.saveReport(report)

                runOnUiThread {
                    Toast.makeText(this@ReportCreationActivity, "Report saved locally!", Toast.LENGTH_SHORT).show()
                    // Navigate to the Report History so the user sees their new report immediately
                    finish()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                isSaving = false
                binding.btnSaveReport.isEnabled = true
                binding.btnSaveReport.text = "Save Report Locally"
                runOnUiThread {
                    Toast.makeText(this@ReportCreationActivity, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}