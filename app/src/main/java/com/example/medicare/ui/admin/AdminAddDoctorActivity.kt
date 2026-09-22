package com.example.medicare.ui.admin

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.data.repository.AdminRepository
import com.example.medicare.databinding.ActivityAdminAddDoctorBinding
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import kotlinx.coroutines.launch
import java.util.Locale

class AdminAddDoctorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminAddDoctorBinding
    private val adminRepository = AdminRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAddDoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarAddDoctor.setNavigationOnClickListener { finish() }

        setupTimePickers()
        setupListeners()
    }

    private fun setupTimePickers() {
        binding.etAdminDocStartTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, min ->
                val amPm = if (hour >= 12) "PM" else "AM"
                val h = if (hour % 12 == 0) 12 else hour % 12
                binding.etAdminDocStartTime.setText(String.format(Locale.US, "%02d:%02d %s", h, min, amPm))
            }, 9, 0, false).show()
        }

        binding.etAdminDocEndTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, min ->
                val amPm = if (hour >= 12) "PM" else "AM"
                val h = if (hour % 12 == 0) 12 else hour % 12
                binding.etAdminDocEndTime.setText(String.format(Locale.US, "%02d:%02d %s", h, min, amPm))
            }, 17, 0, false).show()
        }
    }

    private fun setupListeners() {
        AnimationUtils.applyPressAnimation(binding.btnAdminSaveDoctor) {
            val name = binding.etAdminDocName.text?.toString()?.trim().orEmpty()
            val phone = binding.etAdminDocPhone.text?.toString()?.trim().orEmpty()
            val specialty = binding.etAdminDocSpecialization.text?.toString()?.trim().orEmpty()
            val qualification = binding.etAdminDocQualification.text?.toString()?.trim().orEmpty()
            val exp = binding.etAdminDocExp.text?.toString()?.trim().orEmpty()
            val hospital = binding.etAdminDocHospital.text?.toString()?.trim().orEmpty()
            val department = binding.etAdminDocDepartment.text?.toString()?.trim().orEmpty()
            val feeStr = binding.etAdminDocFee.text?.toString()?.trim().orEmpty()
            val fee = feeStr.toDoubleOrNull() ?: 50.0
            val days = binding.etAdminDocDays.text?.toString()?.trim().orEmpty().ifEmpty { "Mon,Tue,Wed,Thu,Fri" }
            val startTime = binding.etAdminDocStartTime.text?.toString()?.trim().orEmpty().ifEmpty { "09:00 AM" }
            val endTime = binding.etAdminDocEndTime.text?.toString()?.trim().orEmpty().ifEmpty { "05:00 PM" }
            val about = binding.etAdminDocAbout.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                binding.tilAdminDocName.error = "Name is required"
                return@applyPressAnimation
            }
            binding.tilAdminDocName.error = null

            if (phone.isEmpty()) {
                binding.tilAdminDocPhone.error = "Mobile Number is required"
                return@applyPressAnimation
            }
            binding.tilAdminDocPhone.error = null

            if (specialty.isEmpty()) {
                binding.tilAdminDocSpecialization.error = "Specialization is required"
                return@applyPressAnimation
            }
            binding.tilAdminDocSpecialization.error = null

            performSaveDoctor(name, phone, specialty, qualification, exp, hospital, department, fee, days, startTime, endTime, about)
        }
    }

    private fun performSaveDoctor(
        name: String,
        phone: String,
        specialty: String,
        qual: String,
        exp: String,
        hospital: String,
        department: String,
        fee: Double,
        days: String,
        startTime: String,
        endTime: String,
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
                hospital = hospital.ifEmpty { "Medicare Central Hospital" },
                department = department.ifEmpty { specialty },
                consultationFee = fee,
                availableDays = days,
                startTime = startTime,
                endTime = endTime,
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
