package com.example.medicare.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.data.repository.AdminRepository
import com.example.medicare.data.repository.AuthRepository
import com.example.medicare.databinding.ActivityAdminDashboardBinding
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val adminRepository = AdminRepository()
    private val authRepository = AuthRepository()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupListeners()
        loadStats()
    }

    private fun setupListeners() {
        AnimationUtils.applyPressAnimation(binding.cardManageDoctors) {
            startActivity(Intent(this, AdminManageDoctorsActivity::class.java))
        }

        AnimationUtils.applyPressAnimation(binding.cardManagePatients) {
            startActivity(Intent(this, AdminManagePatientsActivity::class.java))
        }

        AnimationUtils.applyPressAnimation(binding.btnAdminLogout) {
            DialogUtils.showConfirmation(
                this,
                title = "Log Out",
                message = "Are you sure you want to exit the Administrative Console?"
            ) {
                lifecycleScope.launch {
                    authRepository.signOut()
                    sessionManager.clearSession()
                    val intent = Intent(this@AdminDashboardActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val result = adminRepository.getDashboardStats()
            result.fold(
                onSuccess = { stats ->
                    binding.tvAdminTotalDoctors.text = stats.totalDoctors.toString()
                    binding.tvAdminTotalPatients.text = stats.totalPatients.toString()
                    binding.tvAdminTotalAppointments.text = stats.totalAppointments.toString()
                    binding.tvAdminPendingApts.text = stats.pendingAppointments.toString()
                },
                onFailure = {
                    // Safe fallback
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }
}
