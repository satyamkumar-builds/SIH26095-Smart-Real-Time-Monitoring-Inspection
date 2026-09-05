package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class TasksFragment : Fragment() {

    // Dynamic State Variables for User Info & Status
    private var userFirstName: String = "CR Didi"
    private var isOnline: Boolean = true
    private var syncStatus: String = "Sync Pending" // e.g. "Sync Pending", "Syncing", "Updated"
    private var hasUnreadNotifications: Boolean = true

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
        val imgProfileDp = view.findViewById<ImageView>(R.id.img_profile_dp)

        // greeting
        tvGreeting.text = "Good Morning, $userFirstName"

        // online status
        updateStatusDisplay(tvUserStatus, viewStatusDot)

        // notification dot
        viewNotificationDot.visibility = if (hasUnreadNotifications) View.VISIBLE else View.GONE

        // notification clickable
        btnNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }

        // switch to profile tab
        imgProfileDp.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_profile
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
}
