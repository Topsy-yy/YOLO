package com.example.roaddamagedetector.ui.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.roaddamagedetector.data.model.RoadDamageReport
import com.example.roaddamagedetector.databinding.ItemReportCardBinding
import java.io.File

class ReportAdapter(
    private var reports: List<RoadDamageReport>,
    private val onItemClick: (RoadDamageReport) -> Unit
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    fun updateReports(newReports: List<RoadDamageReport>) {
        reports = newReports
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(reports[position])
    }

    override fun getItemCount(): Int = reports.size

    inner class ReportViewHolder(private val binding: ItemReportCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: RoadDamageReport) {
            binding.tvPrimaryDamage.text = report.primaryDamageClass
            binding.tvTimestamp.text = report.formattedDate
            binding.tvGpsText.text = report.formattedGps

            val count = report.detections.size
            binding.tvDetectionsCount.text = if (count == 1) "1 Defect Identified" else "$count Defects Identified"

            // Severity Badge Styling
            binding.tvSeverityBadge.text = report.severity
            when (report.severity.lowercase()) {
                "critical", "high" -> {
                    binding.tvSeverityBadge.setBackgroundColor(Color.parseColor("#FFEBEE"))
                    binding.tvSeverityBadge.setTextColor(Color.parseColor("#D32F2F"))
                }
                "medium" -> {
                    binding.tvSeverityBadge.setBackgroundColor(Color.parseColor("#FFF3E0"))
                    binding.tvSeverityBadge.setTextColor(Color.parseColor("#E65100"))
                }
                else -> {
                    binding.tvSeverityBadge.setBackgroundColor(Color.parseColor("#E8F5E9"))
                    binding.tvSeverityBadge.setTextColor(Color.parseColor("#2E7D32"))
                }
            }

            // Load Image Thumbnail
            try {
                val imgFile = File(report.imagePath)
                if (imgFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.ivThumbnail.setImageBitmap(bitmap)
                } else {
                    binding.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                binding.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.root.setOnClickListener {
                onItemClick(report)
            }
        }
    }
}
