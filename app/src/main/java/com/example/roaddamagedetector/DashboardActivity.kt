package com.example.roaddamagedetector

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.roaddamagedetector.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize default stats (Empty state fallback)
        binding.tvTotalReportsCount.text = "0"
        binding.tvPotholesCount.text = "0"
        binding.tvCracksCount.text = "0"

        // 1. Navigation to Camera (MainActivity) -> Using cardLiveCamera
        binding.cardLiveCamera.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 2. Navigation to Gallery (ImagePickerActivity) -> Using cardGalleryPicker
        binding.cardGalleryPicker.setOnClickListener {
            val intent = Intent(this, ImagePickerActivity::class.java)
            startActivity(intent)
        }

        // 3. Intentionally Unavailable: Reports -> Using cardReportHistory
        binding.cardReportHistory.setOnClickListener {
            val intent = Intent(this, ReportHistoryActivity::class.java)
            startActivity(intent)
        }

        // 4. Intentionally Unavailable: Map -> Using cardDamageMap
        binding.cardDamageMap.setOnClickListener {
            Toast.makeText(this, "Map feature is coming soon!", Toast.LENGTH_SHORT).show()
        }

        // 5. Intentionally Unavailable: System Info -> Using cardSystemInfo
        binding.cardSystemInfo.setOnClickListener {
            Toast.makeText(this, "System Info feature is coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}