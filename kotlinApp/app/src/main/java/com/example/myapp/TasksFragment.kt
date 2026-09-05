package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class TasksFragment : Fragment() {

    // Dynamic State Variables for User Info & Status
    private var userFirstName: String = "CR DIDI"
    private var isOnline: Boolean = true
    private var syncStatus: String = "Sync Pending"
    private var hasUnreadNotifications: Boolean = true

    // Dynamic Variables for Status Card Values
    private var assignedCount: Int = 8
    private var inProgressCount: Int = 2
    private var completedCount: Int = 17
    private var overdueCount: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        // TopBar Views
        val tvGreeting = view.findViewById<TextView>(R.id.tv_greeting)
        val tvUserStatus = view.findViewById<TextView>(R.id.tv_user_status)
        val viewStatusDot = view.findViewById<View>(R.id.view_status_dot)
        val viewNotificationDot = view.findViewById<View>(R.id.view_notification_dot)
        val btnNotification = view.findViewById<View>(R.id.btn_notification)
        val imgProfileDp = view.findViewById<ImageView>(R.id.img_profile_dp)

        // Grid Card Count Views
        val tvCountAssigned = view.findViewById<TextView>(R.id.tv_count_assigned)
        val tvCountInProgress = view.findViewById<TextView>(R.id.tv_count_in_progress)
        val tvCountCompleted = view.findViewById<TextView>(R.id.tv_count_completed)
        val tvCountOverdue = view.findViewById<TextView>(R.id.tv_count_overdue)

        // Action Button
        val btnStartInspection = view.findViewById<Button>(R.id.btn_start_inspection)

        // 1. Set Greeting & Status
        tvGreeting.text = "Good Morning, $userFirstName"
        updateStatusDisplay(tvUserStatus, viewStatusDot)
        viewNotificationDot.visibility = if (hasUnreadNotifications) View.VISIBLE else View.GONE

        // 2. Set Status Card Numbers Dynamically
        tvCountAssigned.text = assignedCount.toString()
        tvCountInProgress.text = inProgressCount.toString()
        tvCountCompleted.text = completedCount.toString()
        tvCountOverdue.text = overdueCount.toString()

        // 3. TopBar Click Listeners
        btnNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }

        imgProfileDp.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_profile
        }

        // 4. Start Inspection Button Click Listener (Placeholder)
        btnStartInspection.setOnClickListener {
            Toast.makeText(requireContext(), "Start Inspection clicked", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun updateStatusDisplay(tvUserStatus: TextView, viewStatusDot: View) {
        val networkState = if (isOnline) "Online" else "Offline"
        tvUserStatus.text = "$networkState - $syncStatus"
        viewStatusDot.setBackgroundResource(
            if (isOnline) R.drawable.bg_online_dot else R.drawable.bg_offline_dot
        )
    }

    // Helper functions to dynamically update card counts from Kotlin code
    fun updateTaskCounts(assigned: Int, inProgress: Int, completed: Int, overdue: Int) {
        this.assignedCount = assigned
        this.inProgressCount = inProgress
        this.completedCount = completed
        this.overdueCount = overdue
    }
}
