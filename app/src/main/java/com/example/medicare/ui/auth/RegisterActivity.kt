package com.example.medicare.ui.auth

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.R
import com.example.medicare.data.repository.AuthRepository
import com.example.medicare.databinding.ActivityRegisterBinding
import com.example.medicare.ui.admin.AdminDashboardActivity
import com.example.medicare.ui.doctor.DoctorDashboardActivity
import com.example.medicare.ui.patient.PatientDashboardActivity
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val genders = arrayOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.actvGender.setAdapter(genderAdapter)

        val bloodGroups = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val bloodAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodGroups)
        binding.actvBloodGroup.setAdapter(bloodAdapter)

        binding.etRegisterDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val dob = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                binding.etRegisterDob.setText(dob)
            }, calendar.get(Calendar.YEAR) - 20, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.etStartTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, min ->
                val amPm = if (hour >= 12) "PM" else "AM"
                val h = if (hour % 12 == 0) 12 else hour % 12
                binding.etStartTime.setText(String.format(Locale.US, "%02d:%02d %s", h, min, amPm))
            }, 9, 0, false).show()
        }

        binding.etEndTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, min ->
                val amPm = if (hour >= 12) "PM" else "AM"
                val h = if (hour % 12 == 0) 12 else hour % 12
                binding.etEndTime.setText(String.format(Locale.US, "%02d:%02d %s", h, min, amPm))
            }, 17, 0, false).show()
        }
    }

    private fun setupListeners() {
        binding.tvRegisterGoToLogin.setOnClickListener {
            finish()
        }

        binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbPatient) {
                binding.llPatientFields.visibility = View.VISIBLE
                binding.llDoctorFields.visibility = View.GONE
            } else {
                binding.llPatientFields.visibility = View.GONE
                binding.llDoctorFields.visibility = View.VISIBLE
            }
        }

        AnimationUtils.applyPressAnimation(binding.btnRegisterSubmit) {
            val name = binding.etRegisterName.text?.toString()?.trim().orEmpty()
            val phone = binding.etRegisterPhone.text?.toString()?.trim().orEmpty()
            val password = binding.etRegisterPassword.text?.toString()?.trim().orEmpty()
            val confirmPassword = binding.etRegisterConfirmPassword.text?.toString()?.trim().orEmpty()
            val dob = binding.etRegisterDob.text?.toString()?.trim().orEmpty()
            val gender = binding.actvGender.text?.toString()?.trim().orEmpty()
            val address = binding.etRegisterAddress.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                binding.tilRegisterName.error = "Full Name is required"
                return@applyPressAnimation
            }
            binding.tilRegisterName.error = null

            if (phone.isEmpty()) {
                binding.tilRegisterPhone.error = "Mobile Number is required"
                return@applyPressAnimation
            }
            binding.tilRegisterPhone.error = null

            if (password.length < 6) {
                binding.tilRegisterPassword.error = "Password must be at least 6 characters"
                return@applyPressAnimation
            }
            binding.tilRegisterPassword.error = null
            
            if (password != confirmPassword) {
                binding.tilRegisterConfirmPassword.error = "Passwords do not match"
                return@applyPressAnimation
            }
            binding.tilRegisterConfirmPassword.error = null

            val selectedRole = if (binding.rbPatient.isChecked) "patient" else "doctor"
            val extraData = mutableMapOf<String, String>()
            extraData["date_of_birth"] = dob
            extraData["gender"] = gender
            extraData["address"] = address

            if (selectedRole == "patient") {
                val emergency = binding.etEmergencyContact.text?.toString()?.trim().orEmpty()
                val bloodGroup = binding.actvBloodGroup.text?.toString()?.trim().orEmpty()
                extraData["emergency_contact"] = emergency
                extraData["blood_group"] = bloodGroup
            } else {
                extraData["specialization"] = binding.etSpecialization.text?.toString()?.trim().orEmpty()
                extraData["qualification"] = binding.etQualification.text?.toString()?.trim().orEmpty()
                extraData["experience"] = binding.etExperience.text?.toString()?.trim().orEmpty()
                extraData["license_number"] = binding.etLicenseNumber.text?.toString()?.trim().orEmpty()
                extraData["hospital_name"] = binding.etHospitalName.text?.toString()?.trim().orEmpty()
                extraData["department"] = binding.etDepartment.text?.toString()?.trim().orEmpty()
                extraData["consultation_fee"] = binding.etConsultationFee.text?.toString()?.trim().orEmpty()
                extraData["start_time"] = binding.etStartTime.text?.toString()?.trim().orEmpty()
                extraData["end_time"] = binding.etEndTime.text?.toString()?.trim().orEmpty()
                extraData["about"] = binding.etAbout.text?.toString()?.trim().orEmpty()
                extraData["profile_image_url"] = binding.etProfileImageUrl.text?.toString()?.trim().orEmpty()
                
                // Collect days
                val days = mutableListOf<String>()
                if (binding.chipMon.isChecked) days.add("Mon")
                if (binding.chipTue.isChecked) days.add("Tue")
                if (binding.chipWed.isChecked) days.add("Wed")
                if (binding.chipThu.isChecked) days.add("Thu")
                if (binding.chipFri.isChecked) days.add("Fri")
                if (binding.chipSat.isChecked) days.add("Sat")
                if (binding.chipSun.isChecked) days.add("Sun")
                extraData["available_days"] = days.joinToString(",")
            }

            performRegistration(name, phone, password, selectedRole, extraData)
        }
    }

    private fun performRegistration(
        name: String,
        phone: String,
        pass: String,
        role: String,
        extraData: Map<String, String>
    ) {
        setLoading(true)

        lifecycleScope.launch {
            val result = authRepository.signUp(
                name = name,
                phone = phone,
                userPassword = pass,
                role = role,
                extraData = extraData
            )

            setLoading(false)

            result.fold(
                onSuccess = { profile ->
                    sessionManager.saveUserSession(profile)

                    DialogUtils.showSuccess(
                        this@RegisterActivity,
                        title = "Account Created!",
                        message = "Welcome to Medicare, ${profile.name}. Your account has been registered successfully."
                    ) {
                        val intent = when (profile.role.lowercase()) {
                            "patient" -> Intent(this@RegisterActivity, PatientDashboardActivity::class.java)
                            "doctor" -> Intent(this@RegisterActivity, DoctorDashboardActivity::class.java)
                            "admin" -> Intent(this@RegisterActivity, AdminDashboardActivity::class.java)
                            else -> Intent(this@RegisterActivity, PatientDashboardActivity::class.java)
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
        binding.btnRegisterSubmit.isEnabled = !loading
        binding.btnRegisterSubmit.text = if (loading) "" else "CREATE ACCOUNT"
    }
}
