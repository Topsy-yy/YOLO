package com.example.roaddamagedetector

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roaddamagedetector.data.db.ReportRepository
import com.example.roaddamagedetector.databinding.ActivityReportHistoryBinding
import com.example.roaddamagedetector.ui.adapter.ReportAdapter
import kotlinx.coroutines.launch

class ReportHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportHistoryBinding
    private lateinit var repository: ReportRepository
    private lateinit var adapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ReportRepository(this)

        binding.btnBack.setOnClickListener { finish() }

        // Setup RecyclerView
        adapter = ReportAdapter(emptyList()) { report ->
            val intent = Intent(this, ReportDetailActivity::class.java)
            intent.putExtra("REPORT_ID", report.id)
            startActivity(intent)
        }

        binding.rvReportHistory.layoutManager = LinearLayoutManager(this)
        binding.rvReportHistory.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadReports()
    }

    private fun loadReports() {
        lifecycleScope.launch {
            val reports = repository.getAllReports()

            // Ensure operations run on the main thread after DB fetch
            runOnUiThread {
                if (reports.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.rvReportHistory.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvReportHistory.visibility = View.VISIBLE
                    adapter.updateReports(reports)
                }
            }
        }
    }
}