package com.example.medicare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.data.repository.AuthRepository
import com.example.medicare.data.repository.DoctorRepository
import com.example.medicare.data.repository.PatientRepository
import com.example.medicare.databinding.ActivityLoginBinding
import com.example.medicare.ui.admin.AdminDashboardActivity
import com.example.medicare.ui.doctor.DoctorDashboardActivity
import com.example.medicare.ui.patient.PatientDashboardActivity
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupListeners()
    }

    private fun setupListeners() {
        // Sign In Button
        AnimationUtils.applyPressAnimation(binding.btnLoginSubmit) {
            val phone = binding.etLoginPhone.text?.toString()?.trim().orEmpty()
            val password = binding.etLoginPassword.text?.toString()?.trim().orEmpty()

            if (phone.isEmpty()) {
                binding.tilLoginPhone.error = "Please enter your mobile number"
                return@applyPressAnimation
            }
            binding.tilLoginPhone.error = null

            if (password.isEmpty()) {
                binding.tilLoginPassword.error = "Please enter your password"
                return@applyPressAnimation
            }
            binding.tilLoginPassword.error = null

            performLogin(phone, password)
        }

        // Navigate to Register
        binding.tvLoginGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Demo Quick Fill Chips (Updating to phone numbers for testing based on schema seed)
        binding.btnQuickPatient.setOnClickListener {
            binding.etLoginPhone.setText("+1-555-0201")
            binding.etLoginPassword.setText("Password123!") // Or whatever the seed passwords are, assuming they use a default if it's not setup.
        }

        binding.btnQuickDoctor.setOnClickListener {
            binding.etLoginPhone.setText("+1-555-0101")
            binding.etLoginPassword.setText("Password123!")
        }

        binding.btnQuickAdmin.setOnClickListener {
            binding.etLoginPhone.setText("+1-555-0999")
            binding.etLoginPassword.setText("Password123!")
        }
    }

    private fun performLogin(phone: String, pass: String) {
        setLoading(true)

        lifecycleScope.launch {
            val result = authRepository.signIn(phone, pass)

            setLoading(false)

            result.fold(
                onSuccess = { profile ->
                    // Save Session
                    sessionManager.saveUserSession(profile)

                    // Resolve and store doctorId or patientId for instant fast dashboard queries
                    if (profile.role.equals("doctor", ignoreCase = true)) {
                        try {
                            val docResult = DoctorRepository().getDoctorByProfileId(profile.id)
                            docResult.getOrNull()?.id?.let { sessionManager.saveDoctorId(it) }
                        } catch (_: Exception) {}
                    } else if (profile.role.equals("patient", ignoreCase = true)) {
                        try {
                            val patResult = PatientRepository().getPatientByProfileId(profile.id)
                            patResult.getOrNull()?.id?.let { sessionManager.savePatientId(it) }
                        } catch (_: Exception) {}
                    }

                    // Navigate based on role
                    navigateToDashboard(profile.role)
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@LoginActivity,
                        title = "Login Failed",
                        message = error.localizedMessage ?: "Invalid mobile number or password. Please try again."
                    )
                }
            )
        }
    }

    private fun navigateToDashboard(role: String) {
        val intent = when (role.lowercase()) {
            "patient" -> Intent(this, PatientDashboardActivity::class.java)
            "doctor" -> Intent(this, DoctorDashboardActivity::class.java)
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            else -> Intent(this, PatientDashboardActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.pbLoginLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLoginSubmit.isEnabled = !loading
        binding.btnLoginSubmit.text = if (loading) "" else "SIGN IN"
    }
}
