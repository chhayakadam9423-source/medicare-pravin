package com.example.medicare.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.databinding.ActivitySplashBinding
import com.example.medicare.ui.admin.AdminDashboardActivity
import com.example.medicare.ui.auth.WelcomeActivity
import com.example.medicare.ui.doctor.DoctorDashboardActivity
import com.example.medicare.ui.patient.PatientDashboardActivity
import com.example.medicare.utils.SessionManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        startAnimations()
        navigateNext()
    }

    private fun startAnimations() {
        binding.ivLogo.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(900)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start()
                }
                .start()
        }

        binding.tvAppName.apply {
            alpha = 0f
            translationY = 20f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(200)
                .start()
        }

        binding.tvAppTagline.apply {
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(700)
                .setStartDelay(400)
                .start()
        }
    }

    private fun navigateNext() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (sessionManager.isLoggedIn()) {
                val role = sessionManager.getUserRole() ?: "patient"
                val intent = when (role.lowercase()) {
                    "doctor" -> Intent(this, DoctorDashboardActivity::class.java)
                    "admin" -> Intent(this, AdminDashboardActivity::class.java)
                    else -> Intent(this, PatientDashboardActivity::class.java)
                }
                startActivity(intent)
            } else {
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
            finish()
        }, 1600)
    }
}
