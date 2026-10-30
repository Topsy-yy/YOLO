package com.example.roaddamagedetector

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.Executors
import com.example.roaddamagedetector.databinding.ActivityImagePickerBinding

class ImagePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePickerBinding
    private lateinit var detector: TFLiteDetector
    private var currentBitmap: Bitmap? = null
    private var currentResults: List<DetectionResult> = emptyList()

    private val executor = Executors.newSingleThreadExecutor()

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processSelectedImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        detector = TFLiteDetector(this)

        binding.tvSummaryText.text = "Results..."
        binding.btnAnalyze.isEnabled = false
        binding.btnProceedToReport.isEnabled = false

        binding.btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.btnProceedToReport.setOnClickListener {
            val bitmap = currentBitmap
            if (bitmap != null) {
                val intent = Intent(this, ReportCreationActivity::class.java).apply {
                    // Pass the bitmap via a cache file to avoid TransactionTooLarge
                    val cachePath = File(cacheDir, "temp_report_${System.currentTimeMillis()}.jpg").absolutePath
                    FileOutputStream(cachePath).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }

                    putExtra("EXTRA_IMAGE_PATH", cachePath)
                    putExtra("EXTRA_DETECTIONS", ArrayList(currentResults))
                }
                startActivity(intent)
            }
        }
    }

    private fun processSelectedImage(uri: Uri) {
        showLoading(true)
        binding.tvSummaryText.text = "Analyzing image..."
        binding.btnProceedToReport.isEnabled = false

        executor.execute {
            val bitmap = loadBitmapFromUri(uri)
            if (bitmap == null) {
                runOnUiThread {
                    showLoading(false)
                    binding.tvSummaryText.text = "Error: Unable to load selected image."
                    Toast.makeText(this, "Failed to decode image file.", Toast.LENGTH_SHORT).show()
                }
                return@execute
            }

            val results = detector.detect(bitmap, confidenceThreshold = 0.05f, iouThreshold = 0.45f)
            currentBitmap = bitmap
            currentResults = results

            runOnUiThread {
                showLoading(false)
                binding.ivSelectedImage.setImageBitmap(bitmap)
                binding.overlayView.setResults(results)
                binding.btnProceedToReport.isEnabled = true

                if (results.isEmpty()) {
                    binding.tvSummaryText.text = "Status: Clean (No Road Damage Detected)\nTotal Defects Found: 0"
                    binding.tvSummaryText.setTextColor(android.graphics.Color.GREEN)
                } else {
                    val topResult = results.maxByOrNull { it.score }
                    binding.tvSummaryText.text = "Top Defect: ${topResult?.className} (${(topResult!!.score * 100).toInt()}%)\nTotal Defects Found: ${results.size}"
                    binding.tvSummaryText.setTextColor(android.graphics.Color.RED)
                }
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: Exception) {
            Log.e("ImagePickerActivity", "Error reading image stream", e)
            null
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSelectImage.isEnabled = !isLoading
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
        detector.close()
    }
}