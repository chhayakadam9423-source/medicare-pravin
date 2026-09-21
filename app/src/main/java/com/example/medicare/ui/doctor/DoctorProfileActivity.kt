package com.example.medicare.ui.doctor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.databinding.ActivityDoctorProfileBinding
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager

class DoctorProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorProfileBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.toolbarDoctorProfile.setNavigationOnClickListener { finish() }

        val name = sessionManager.getUserName() ?: "Dr. Sarah Jenkins"
        binding.tvDocProfileName.text = name
        binding.tvDocProfileInitial.text = name.take(2).uppercase()
    }

    private fun setupListeners() {
        AnimationUtils.applyPressAnimation(binding.btnDoctorLogout) {
            DialogUtils.showConfirmation(
                this,
                title = "Log Out",
                message = "Are you sure you want to log out from Doctor portal?"
            ) {
                sessionManager.clearSession()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}
