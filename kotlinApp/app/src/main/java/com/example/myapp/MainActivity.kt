package com.example.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Default screen on launch: TasksFragment (which includes the dedicated TopBar)
        if (savedInstanceState == null) {
            replaceFragment(TasksFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_tasks -> {
                    replaceFragment(TasksFragment())
                    true
                }
                R.id.navigation_capture -> {
                    replaceFragment(GenericFragment.newInstance("Capture Screen"))
                    true
                }
                R.id.navigation_sync -> {
                    replaceFragment(GenericFragment.newInstance("Sync Screen"))
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(GenericFragment.newInstance("Profile Screen"))
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
