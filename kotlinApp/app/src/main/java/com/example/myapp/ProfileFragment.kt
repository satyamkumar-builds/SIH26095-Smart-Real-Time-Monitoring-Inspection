package com.example.myapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    private lateinit var imgProfileAvatar: ImageView
    private lateinit var tvProfileDisplayName: TextView
    private lateinit var tabBtnProfile: TextView
    private lateinit var tabBtnSettings: TextView

    private lateinit var layoutProfileForm: LinearLayout
    private lateinit var layoutSettingsForm: LinearLayout

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSaveProfile: Button

    private lateinit var rgTheme: RadioGroup
    private lateinit var rbThemeLight: RadioButton
    private lateinit var rbThemeDark: RadioButton

    private lateinit var tvFooterCredits: TextView

    // Gallery Picker launcher to update DP throughout the app
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("custom_dp_uri", it.toString()).apply()

            imgProfileAvatar.setImageURI(it)
            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        imgProfileAvatar = view.findViewById(R.id.img_profile_avatar)
        val containerDp = view.findViewById<View>(R.id.container_dp)
        tvProfileDisplayName = view.findViewById(R.id.tv_profile_display_name)

        tabBtnProfile = view.findViewById(R.id.tab_btn_profile)
        tabBtnSettings = view.findViewById(R.id.tab_btn_settings)

        layoutProfileForm = view.findViewById(R.id.layout_profile_form)
        layoutSettingsForm = view.findViewById(R.id.layout_settings_form)

        etFirstName = view.findViewById(R.id.et_first_name)
        etLastName = view.findViewById(R.id.et_last_name)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        btnSaveProfile = view.findViewById(R.id.btn_save_profile)

        rgTheme = view.findViewById(R.id.rg_theme)
        rbThemeLight = view.findViewById(R.id.rb_theme_light)
        rbThemeDark = view.findViewById(R.id.rb_theme_dark)

        tvFooterCredits = view.findViewById(R.id.tv_footer_credits)

        // 1. Load Saved Profile Info & Custom DP
        loadProfilePreferences()

        // 2. Open Gallery when Circular DP Container is clicked
        containerDp.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 3. Tab Switching Handlers (Profile vs Settings)
        tabBtnProfile.setOnClickListener {
            showTab(isProfile = true)
        }

        tabBtnSettings.setOnClickListener {
            showTab(isProfile = false)
        }

        // 4. Save Profile Form Details
        btnSaveProfile.setOnClickListener {
            val fName = etFirstName.text.toString().trim()
            val lName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (fName.isNotEmpty()) {
                val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("user_first_name", fName)
                    .putString("user_last_name", lName)
                    .putString("user_email", email)
                    .putString("user_phone", phone)
                    .apply()

                tvProfileDisplayName.text = fName
                Toast.makeText(requireContext(), "Profile details saved!", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Dark / Light Mode Theme Switching
        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val isDark = (checkedId == R.id.rb_theme_dark)
            val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_dark_mode", isDark).apply()

            if (isDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // 6. Footer Credits Click Handler -> Redirect to GitHub
        tvFooterCredits.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RanvirRox"))
            startActivity(browserIntent)
        }

        return view
    }

    private fun showTab(isProfile: Boolean) {
        if (isProfile) {
            tabBtnProfile.setBackgroundResource(R.drawable.bg_tab_selected)
            tabBtnProfile.setTextColor(Color.WHITE)
            tabBtnSettings.setBackgroundResource(R.drawable.bg_tab_unselected)
            tabBtnSettings.setTextColor(Color.parseColor("#666666"))

            layoutProfileForm.visibility = View.VISIBLE
            layoutSettingsForm.visibility = View.GONE
        } else {
            tabBtnSettings.setBackgroundResource(R.drawable.bg_tab_selected)
            tabBtnSettings.setTextColor(Color.WHITE)
            tabBtnProfile.setBackgroundResource(R.drawable.bg_tab_unselected)
            tabBtnProfile.setTextColor(Color.parseColor("#666666"))

            layoutSettingsForm.visibility = View.VISIBLE
            layoutProfileForm.visibility = View.GONE
        }
    }

    private fun loadProfilePreferences() {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val firstName = prefs.getString("user_first_name", "Rahul") ?: "Rahul"
        val lastName = prefs.getString("user_last_name", "Sharma") ?: "Sharma"
        val email = prefs.getString("user_email", "rahul.sharma@example.com")
        val phone = prefs.getString("user_phone", "+91 9876543210")

        etFirstName.setText(firstName)
        etLastName.setText(lastName)
        etEmail.setText(email)
        etPhone.setText(phone)
        tvProfileDisplayName.text = firstName

        val dpUriString = prefs.getString("custom_dp_uri", null)
        if (dpUriString != null) {
            imgProfileAvatar.setImageURI(Uri.parse(dpUriString))
        }

        val isDark = prefs.getBoolean("is_dark_mode", false)
        if (isDark) {
            rbThemeDark.isChecked = true
        } else {
            rbThemeLight.isChecked = true
        }
    }
}
