package com.example.medicare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.data.model.UserRole
import com.example.medicare.data.repository.AuthRepository
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
        AnimationUtils.applyPressAnimation(binding.btnLogin) {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Please enter your email"
                return@applyPressAnimation
            }
            binding.tilEmail.error = null

            if (password.isEmpty()) {
                binding.tilPassword.error = "Please enter your password"
                return@applyPressAnimation
            }
            binding.tilPassword.error = null

            performLogin(email, password)
        }

        // Navigate to Register
        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Demo Quick Fill Chips
        binding.chipDemoPatient.setOnClickListener {
            binding.etEmail.setText("patient@medicare.com")
            binding.etPassword.setText("Password123!")
        }

        binding.chipDemoDoctor.setOnClickListener {
            binding.etEmail.setText("doctor@medicare.com")
            binding.etPassword.setText("Password123!")
        }

        binding.chipDemoAdmin.setOnClickListener {
            binding.etEmail.setText("admin@medicare.com")
            binding.etPassword.setText("Password123!")
        }
    }

    private fun performLogin(email: String, pass: String) {
        setLoading(true)

        lifecycleScope.launch {
            val result = authRepository.signIn(email, pass)

            setLoading(false)

            result.fold(
                onSuccess = { profile ->
                    // Save Session
                    sessionManager.saveUserSession(
                        userId = profile.id,
                        email = profile.email,
                        name = profile.name,
                        role = profile.role.name.lowercase(),
                        phone = profile.phone
                    )

                    // Navigate based on role
                    navigateToDashboard(profile.role)
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@LoginActivity,
                        title = "Login Failed",
                        message = error.localizedMessage ?: "Invalid email or password. Please try again."
                    )
                }
            )
        }
    }

    private fun navigateToDashboard(role: UserRole) {
        val intent = when (role) {
            UserRole.PATIENT -> Intent(this, PatientDashboardActivity::class.java)
            UserRole.DOCTOR -> Intent(this, DoctorDashboardActivity::class.java)
            UserRole.ADMIN -> Intent(this, AdminDashboardActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.pbLoginLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "" else "SIGN IN"
    }
}
