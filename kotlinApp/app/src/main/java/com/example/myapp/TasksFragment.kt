package com.example.myapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONArray
import java.io.File
import java.util.Calendar

class TasksFragment : Fragment() {

    private var userFirstName: String = "Rahul"
    private var isOnline: Boolean = true
    private var syncStatus: String = "Sync Pending"
    private var hasUnreadNotifications: Boolean = true

    private var assignedCount: Int = 8
    private var inProgressCount: Int = 5
    private var completedCount: Int = 24
    private var overdueCount: Int = 2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        val tvGreeting = view.findViewById<TextView>(R.id.tv_greeting)
        val tvUserStatus = view.findViewById<TextView>(R.id.tv_user_status)
        val viewStatusDot = view.findViewById<View>(R.id.view_status_dot)
        val viewNotificationDot = view.findViewById<View>(R.id.view_notification_dot)
        val btnNotification = view.findViewById<View>(R.id.btn_notification)
        val imgProfileDp = view.findViewById<CircleImageView>(R.id.img_profile_dp)

        val tvCountAssigned = view.findViewById<TextView>(R.id.tv_count_assigned)
        val tvCountInProgress = view.findViewById<TextView>(R.id.tv_count_in_progress)
        val tvCountCompleted = view.findViewById<TextView>(R.id.tv_count_completed)
        val tvCountOverdue = view.findViewById<TextView>(R.id.tv_count_overdue)

        val btnStartInspection = view.findViewById<Button>(R.id.btn_start_inspection)
        val containerCards = view.findViewById<LinearLayout>(R.id.container_inspection_cards)

        // 1. Load User Preferences (First Name & Saved DP File Path)
        loadUserPreferences(imgProfileDp)

        // 2. Set Dynamic Time-Based Greeting
        val timeGreeting = getGreetingForCurrentTime()
        tvGreeting.text = "$timeGreeting, $userFirstName"

        // 3. Set Status & Counts
        updateStatusDisplay(tvUserStatus, viewStatusDot)
        viewNotificationDot.visibility = if (hasUnreadNotifications) View.VISIBLE else View.GONE

        tvCountAssigned.text = assignedCount.toString()
        tvCountInProgress.text = inProgressCount.toString()
        tvCountCompleted.text = completedCount.toString()
        tvCountOverdue.text = overdueCount.toString()

        // 4. Load Inspections from JSON
        loadInspectionsFromJson(inflater, containerCards)

        // 5. Click Listeners
        btnNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }

        imgProfileDp.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_profile
        }

        btnStartInspection.setOnClickListener {
            Toast.makeText(requireContext(), "Start Inspection clicked", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadUserPreferences(imgProfileDp: CircleImageView) {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userFirstName = prefs.getString("user_first_name", "Rahul") ?: "Rahul"

        val dpPath = prefs.getString("custom_dp_path", null)
        if (dpPath != null) {
            val file = File(dpPath)
            if (file.exists()) {
                imgProfileDp.setImageURI(Uri.fromFile(file))
            }
        }
    }

    private fun getGreetingForCurrentTime(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    private fun updateStatusDisplay(tvUserStatus: TextView, viewStatusDot: View) {
        val networkState = if (isOnline) "Online" else "Offline"
        tvUserStatus.text = "$networkState - $syncStatus"
        viewStatusDot.setBackgroundResource(
            if (isOnline) R.drawable.bg_online_dot else R.drawable.bg_offline_dot
        )
    }

    private fun loadInspectionsFromJson(inflater: LayoutInflater, container: LinearLayout) {
        try {
            val jsonString = requireContext().assets.open("inspections.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            container.removeAllViews()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val item = InspectionItem(
                    id = jsonObject.getString("id"),
                    priority = jsonObject.getString("priority"),
                    title = jsonObject.getString("title"),
                    location = jsonObject.getString("location"),
                    time = jsonObject.getString("time"),
                    status = jsonObject.getString("status")
                )

                val cardView = inflater.inflate(R.layout.item_inspection_card, container, false)

                val tvId = cardView.findViewById<TextView>(R.id.tv_inspection_id)
                val tvPriority = cardView.findViewById<TextView>(R.id.tv_priority_badge)
                val tvTitle = cardView.findViewById<TextView>(R.id.tv_inspection_title)
                val tvLocation = cardView.findViewById<TextView>(R.id.tv_inspection_location)
                val tvTime = cardView.findViewById<TextView>(R.id.tv_inspection_time)
                val tvStatus = cardView.findViewById<TextView>(R.id.tv_inspection_status)

                tvId.text = item.id
                tvPriority.text = item.priority
                tvTitle.text = item.title
                tvLocation.text = item.location
                tvTime.text = item.time
                tvStatus.text = item.status

                container.addView(cardView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
