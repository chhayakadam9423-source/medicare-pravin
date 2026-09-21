package com.example.medicare.ui.patient

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.databinding.ActivityPatientProfileBinding
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager

class PatientProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientProfileBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.toolbarPatientProfile.setNavigationOnClickListener { finish() }

        val name = sessionManager.getUserName() ?: "Alex Johnson"
        val email = sessionManager.getUserEmail() ?: "alex.patient@medicare.com"
        val phone = sessionManager.getUserPhone() ?: "+1-555-0201"

        binding.tvProfileName.text = name
        binding.tvProfileAvatarInitial.text = name.take(1).uppercase()
        binding.tvProfileEmail.text = email
        binding.tvProfilePhone.text = phone
    }

    private fun setupListeners() {
        AnimationUtils.applyPressAnimation(binding.btnProfileLogout) {
            DialogUtils.showConfirmation(
                this,
                title = "Log Out",
                message = "Are you sure you want to sign out from your Medicare account?"
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
