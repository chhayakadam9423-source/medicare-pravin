package com.example.medicare.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.data.repository.AdminRepository
import com.example.medicare.databinding.ActivityAdminAddDoctorBinding
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import kotlinx.coroutines.launch

class AdminAddDoctorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminAddDoctorBinding
    private val adminRepository = AdminRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAddDoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarAddDoctor.setNavigationOnClickListener { finish() }

        setupListeners()
    }

    private fun setupListeners() {
        AnimationUtils.applyPressAnimation(binding.btnAdminSaveDoctor) {
            val name = binding.etAdminDocName.text?.toString()?.trim().orEmpty()
            val phone = binding.etAdminDocPhone.text?.toString()?.trim().orEmpty()
            val specialty = binding.etAdminDocSpecialization.text?.toString()?.trim().orEmpty()
            val qualification = binding.etAdminDocQualification.text?.toString()?.trim().orEmpty()
            val exp = binding.etAdminDocExp.text?.toString()?.trim().orEmpty()
            val about = binding.etAdminDocAbout.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                binding.tilAdminDocName.error = "Name is required"
                return@applyPressAnimation
            }
            binding.tilAdminDocName.error = null

            if (specialty.isEmpty()) {
                binding.tilAdminDocSpecialization.error = "Specialization is required"
                return@applyPressAnimation
            }
            binding.tilAdminDocSpecialization.error = null

            performSaveDoctor(name, phone, specialty, qualification, exp, about)
        }
    }

    private fun performSaveDoctor(
        name: String,
        phone: String,
        specialty: String,
        qual: String,
        exp: String,
        about: String
    ) {
        setLoading(true)

        lifecycleScope.launch {
            val result = adminRepository.addDoctor(
                name = name,
                phone = phone,
                specialization = specialty,
                qualification = qual.ifEmpty { "MBBS, MD" },
                experience = exp.ifEmpty { "5 Years" },
                about = about.ifEmpty { "Experienced clinician dedicated to excellence in patient treatment." },
                imageUrl = ""
            )

            setLoading(false)

            result.fold(
                onSuccess = {
                    DialogUtils.showSuccess(
                        this@AdminAddDoctorActivity,
                        title = "Doctor Registered",
                        message = "Physician $name has been enrolled into Medicare and is ready to accept patient appointments."
                    ) {
                        finish()
                    }
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@AdminAddDoctorActivity,
                        title = "Registration Failed",
                        message = error.localizedMessage ?: "Could not add doctor"
                    )
                }
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.pbAdminSaveDocLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnAdminSaveDoctor.isEnabled = !loading
        binding.btnAdminSaveDoctor.text = if (loading) "" else "SAVE DOCTOR"
    }
}
