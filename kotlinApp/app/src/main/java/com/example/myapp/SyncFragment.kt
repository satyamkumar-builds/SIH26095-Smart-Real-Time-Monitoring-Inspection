package com.example.myapp

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File

class SyncFragment : Fragment() {

    private lateinit var containerSyncCards: LinearLayout
    private lateinit var layoutEmptySyncState: LinearLayout
    private lateinit var tvSyncQueueCount: TextView

    private val syncItemList = mutableListOf<SyncItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sync, container, false)

        containerSyncCards = view.findViewById(R.id.container_sync_cards)
        layoutEmptySyncState = view.findViewById(R.id.layout_empty_sync_state)
        tvSyncQueueCount = view.findViewById(R.id.tv_sync_queue_count)

        // Load captured evidence photos from app cache
        loadCapturedEvidences()

        return view
    }

    private fun loadCapturedEvidences() {
        syncItemList.clear()

        // Read cached evidence files from cache directory
        val cacheDir = requireContext().cacheDir
        val evidenceFiles = cacheDir.listFiles { file ->
            file.name.startsWith("evidence_") && file.name.endsWith(".jpg")
        }?.sortedByDescending { it.lastModified() } // Latest photos first at top

        if (evidenceFiles.isNullOrEmpty()) {
            // Add a mock item for visual preview if no real photo was taken yet
            syncItemList.add(
                SyncItem(
                    id = "MOCK-1",
                    inspectionId = "INS-2026-1042",
                    title = "Facility Inspection",
                    priority = "HIGH PRIORITY",
                    location = "District 4, Main Utility Plant",
                    time = "09:15:30 AM IST",
                    imageFile = File("mock")
                )
            )
        } else {
            evidenceFiles.forEachIndexed { index, file ->
                syncItemList.add(
                    SyncItem(
                        id = "EVID-${file.nameWithoutExtension}",
                        inspectionId = "INS-2026-1042",
                        title = "Facility Inspection Evidence",
                        priority = "HIGH PRIORITY",
                        location = "District 4, Main Utility Plant",
                        time = "Captured Evidence #${index + 1}",
                        imageFile = file
                    )
                )
            }
        }

        renderCards()
    }

    private fun renderCards() {
        containerSyncCards.removeAllViews()

        if (syncItemList.isEmpty()) {
            layoutEmptySyncState.visibility = View.VISIBLE
            tvSyncQueueCount.text = "0 Pending"
            return
        }

        layoutEmptySyncState.visibility = View.GONE
        tvSyncQueueCount.text = "${syncItemList.count { !it.isSynced }} Pending"

        val inflater = LayoutInflater.from(requireContext())

        // Inflate cards (Latest items are up top)
        syncItemList.forEach { item ->
            val cardView = inflater.inflate(R.layout.item_sync_card, containerSyncCards, false)

            val tvId = cardView.findViewById<TextView>(R.id.tv_sync_inspection_id)
            val tvPriority = cardView.findViewById<TextView>(R.id.tv_sync_priority_badge)
            val tvTitle = cardView.findViewById<TextView>(R.id.tv_sync_title)
            val imgThumbnail = cardView.findViewById<ImageView>(R.id.img_sync_evidence_thumbnail)
            val tvLocation = cardView.findViewById<TextView>(R.id.tv_sync_location)
            val tvTime = cardView.findViewById<TextView>(R.id.tv_sync_time)

            val btnDelete = cardView.findViewById<Button>(R.id.btn_delete_sync_item)
            val btnSync = cardView.findViewById<Button>(R.id.btn_upload_sync_item)

            tvId.text = item.inspectionId
            tvPriority.text = item.priority
            tvTitle.text = item.title
            tvLocation.text = item.location
            tvTime.text = item.time

            // Load Bitmap into Thumbnail
            if (item.imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(item.imageFile.absolutePath)
                imgThumbnail.setImageBitmap(bitmap)
            } else {
                imgThumbnail.setImageResource(R.drawable.ic_capture)
            }

            // Sync Button State
            if (item.isSynced) {
                btnSync.text = "Uploaded"
                btnSync.setBackgroundResource(R.drawable.bg_status_badge)
                btnSync.setTextColor(Color.parseColor("#4CAF50"))
                btnSync.isClickable = false
            } else {
                btnSync.text = "Sync"
                btnSync.setBackgroundResource(R.drawable.bg_sync_button)
                btnSync.setTextColor(Color.WHITE)
            }

            // Delete Action
            btnDelete.setOnClickListener {
                if (item.imageFile.exists()) {
                    item.imageFile.delete()
                }
                syncItemList.remove(item)
                renderCards()
                Toast.makeText(requireContext(), "Evidence deleted", Toast.LENGTH_SHORT).show()
            }

            // Sync Action (Future Proof Backend Placeholder)
            btnSync.setOnClickListener {
                item.isSynced = true
                btnSync.text = "Uploaded"
                btnSync.setBackgroundResource(R.drawable.bg_status_badge)
                btnSync.setTextColor(Color.parseColor("#4CAF50"))
                btnSync.isClickable = false
                tvSyncQueueCount.text = "${syncItemList.count { !it.isSynced }} Pending"
                Toast.makeText(requireContext(), "Syncing evidence to cloud...", Toast.LENGTH_SHORT).show()
            }

            containerSyncCards.addView(cardView)
        }
    }
}
