package com.example.roaddamagedetector

import com.example.roaddamagedetector.OverlayView
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var tvLocationHeader: TextView
    private lateinit var tvDetectionSummary: TextView
    private lateinit var btnCaptureReport: Button

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private lateinit var tfLiteDetector: TFLiteDetector
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLocation: Location? = null

    companion object {
        private const val TAG = "RoadDamageApp"
        private const val PERMISSIONS_REQUEST_CODE = 101
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. ADDED: Back navigation to Dashboard
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewFinder = findViewById(R.id.viewFinder)
        overlayView = findViewById(R.id.overlayView)
        tvLocationHeader = findViewById(R.id.tvLocationHeader)
        tvDetectionSummary = findViewById(R.id.tvDetectionSummary)
        btnCaptureReport = findViewById(R.id.btnCaptureReport)

        tfLiteDetector = TFLiteDetector(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
            fetchCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE
            )
        }

        btnCaptureReport.setOnClickListener {
            captureAndLogDamage()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(viewFinder.display.rotation)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processCameraFrame(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // 2. FIXED: Removed @OptIn(ExperimentalGetImage::class) which caused warnings
    private fun processCameraFrame(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        if (bitmap != null) {
            val rotatedBitmap = rotateBitmapIfNeeded(bitmap, imageProxy.imageInfo.rotationDegrees)
            val detections = tfLiteDetector.detect(rotatedBitmap, confidenceThreshold = 0.35f)

            runOnUiThread {
                // Update Bounding Box Canvas Overlay
                overlayView.setResults(detections)

                // Update Header Summary
                if (detections.isNotEmpty()) {
                    val topDetection = detections.maxByOrNull { it.score }
                    tvDetectionSummary.text = "Detected: ${topDetection?.className} (${(topDetection!!.score * 100).toInt()}%)"
                    tvDetectionSummary.setTextColor(android.graphics.Color.RED)
                } else {
                    tvDetectionSummary.text = "Damage Detected: None"
                    tvDetectionSummary.setTextColor(android.graphics.Color.GREEN)
                }
            }
        }
        imageProxy.close()
    }

    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                tvLocationHeader.text = "GPS: Lat ${String.format("%.5f", location.latitude)}, Lon ${String.format("%.5f", location.longitude)}"
            } else {
                tvLocationHeader.text = "GPS: Searching for satellite signal..."
            }
        }
    }

    private fun captureAndLogDamage() {
        fetchCurrentLocation()
        val lat = currentLocation?.latitude ?: 0.0
        val lon = currentLocation?.longitude ?: 0.0

        val statusMessage = "Logged damage event at GPS: [$lat, $lon]"
        Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
        Log.d(TAG, statusMessage)
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
                fetchCurrentLocation()
            } else {
                Toast.makeText(this, "Permissions not granted by user.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 3. ADDED: Handles the back arrow click in the Action Bar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        tfLiteDetector.close()
    }
}