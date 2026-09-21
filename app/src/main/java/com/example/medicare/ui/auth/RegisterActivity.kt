package com.example.medicare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.R
import com.example.medicare.data.model.UserRole
import com.example.medicare.data.repository.AuthRepository
import com.example.medicare.databinding.ActivityRegisterBinding
import com.example.medicare.ui.admin.AdminDashboardActivity
import com.example.medicare.ui.doctor.DoctorDashboardActivity
import com.example.medicare.ui.patient.PatientDashboardActivity
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.toolbarRegister.setNavigationOnClickListener {
            finish()
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }

        AnimationUtils.applyPressAnimation(binding.btnRegister) {
            val name = binding.etRegisterName.text?.toString()?.trim().orEmpty()
            val email = binding.etRegisterEmail.text?.toString()?.trim().orEmpty()
            val phone = binding.etRegisterPhone.text?.toString()?.trim().orEmpty()
            val password = binding.etRegisterPassword.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                binding.tilRegisterName.error = "Full Name is required"
                return@applyPressAnimation
            }
            binding.tilRegisterName.error = null

            if (email.isEmpty()) {
                binding.tilRegisterEmail.error = "Email address is required"
                return@applyPressAnimation
            }
            binding.tilRegisterEmail.error = null

            if (password.length < 6) {
                binding.tilRegisterPassword.error = "Password must be at least 6 characters"
                return@applyPressAnimation
            }
            binding.tilRegisterPassword.error = null

            val selectedRole = if (binding.radioPatient.isChecked) UserRole.PATIENT else UserRole.DOCTOR

            performRegistration(name, email, phone, password, selectedRole)
        }
    }

    private fun performRegistration(
        name: String,
        email: String,
        phone: String,
        pass: String,
        role: UserRole
    ) {
        setLoading(true)

        lifecycleScope.launch {
            val result = authRepository.signUp(
                email = email,
                password = pass,
                name = name,
                role = role,
                phone = phone
            )

            setLoading(false)

            result.fold(
                onSuccess = { profile ->
                    sessionManager.saveUserSession(
                        userId = profile.id,
                        email = profile.email,
                        name = profile.name,
                        role = profile.role.name.lowercase(),
                        phone = profile.phone
                    )

                    DialogUtils.showSuccess(
                        this@RegisterActivity,
                        title = "Account Created!",
                        message = "Welcome to Medicare, ${profile.name}. Your account has been registered successfully."
                    ) {
                        val intent = when (profile.role) {
                            UserRole.PATIENT -> Intent(this@RegisterActivity, PatientDashboardActivity::class.java)
                            UserRole.DOCTOR -> Intent(this@RegisterActivity, DoctorDashboardActivity::class.java)
                            UserRole.ADMIN -> Intent(this@RegisterActivity, AdminDashboardActivity::class.java)
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@RegisterActivity,
                        title = "Registration Failed",
                        message = error.localizedMessage ?: "Unable to create account. Please try again."
                    )
                }
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.pbRegisterLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) "" else "CREATE ACCOUNT"
    }
}
