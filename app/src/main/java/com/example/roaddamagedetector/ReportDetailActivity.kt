package com.example.roaddamagedetector

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.roaddamagedetector.data.db.ReportRepository
import com.example.roaddamagedetector.databinding.ActivityReportDetailBinding
import kotlinx.coroutines.launch
import java.io.File

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportDetailBinding
    private lateinit var repository: ReportRepository
    private var reportId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ReportRepository(this)
        reportId = intent.getLongExtra("REPORT_ID", -1)

        binding.btnBack.setOnClickListener { finish() }

        if (reportId == -1L) {
            Toast.makeText(this, "Error: Report not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadReportDetails()

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }
    }

    private fun loadReportDetails() {
        lifecycleScope.launch {
            val report = repository.getReportById(reportId)

            runOnUiThread {
                if (report == null) {
                    Toast.makeText(this@ReportDetailActivity, "Report no longer exists.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // 1. Populate Text Fields
                    binding.tvDetailTitle.text = report.primaryDamageClass
                    binding.tvDetailDate.text = report.formattedDate
                    binding.tvDetailSeverity.text = "Severity: ${report.severity}"
                    binding.tvDetailGps.text = "GPS: ${report.formattedGps}"
                    binding.tvDetailNotes.text = if (report.userNotes.isBlank()) "No additional notes provided." else report.userNotes

                    // 2. Populate Detections
                    if (report.detections.isEmpty()) {
                        binding.tvDetailDetections.text = "No defects identified."
                    } else {
                        val detectionsText = report.detections.mapIndexed { index, det ->
                            "${index + 1}. ${det.className} (${(det.score * 100).toInt()}%)"
                        }.joinToString("\n")
                        binding.tvDetailDetections.text = detectionsText
                        binding.overlayView.setResults(report.detections)
                    }

                    // 3. Load Image (Handle Missing File)
                    val imgFile = File(report.imagePath)
                    if (imgFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        binding.ivDetailImage.setImageBitmap(bitmap)
                    } else {
                        // Fallback if the image was deleted from storage
                        binding.ivDetailImage.setImageResource(android.R.drawable.ic_menu_report_image)
                        binding.ivDetailImage.setColorFilter(android.graphics.Color.LTGRAY)
                        Toast.makeText(this@ReportDetailActivity, "Warning: Image file is missing.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to permanently delete this report and its image?")
            .setPositiveButton("Delete") { _, _ ->
                deleteReport()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReport() {
        lifecycleScope.launch {
            // The repository handles deleting the DB row AND cleaning up the image file
            val success = repository.deleteReport(reportId)

            runOnUiThread {
                if (success) {
                    Toast.makeText(this@ReportDetailActivity, "Report deleted.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ReportDetailActivity, "Failed to delete report.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}