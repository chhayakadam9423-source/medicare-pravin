package com.example.medicare.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.databinding.ActivityWelcomeBinding
import com.example.medicare.utils.AnimationUtils

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        AnimationUtils.applyPressAnimation(binding.btnWelcomeLogin) {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        AnimationUtils.applyPressAnimation(binding.btnWelcomeRegister) {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
