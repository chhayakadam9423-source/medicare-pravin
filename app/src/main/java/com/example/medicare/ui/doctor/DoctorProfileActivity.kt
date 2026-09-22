package com.example.medicare.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.R
import com.example.medicare.data.model.Doctor
import com.example.medicare.data.repository.AuthRepository
import com.example.medicare.data.repository.DoctorRepository
import com.example.medicare.databinding.ActivityDoctorProfileBinding
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch

class DoctorProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorProfileBinding
    private lateinit var sessionManager: SessionManager
    private val authRepository = AuthRepository()
    private val doctorRepository = DoctorRepository()
    private var currentDoctor: Doctor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
        loadDoctorProfile()
    }

    private fun setupUI() {
        binding.toolbarDoctorProfile.setNavigationOnClickListener { finish() }

        val name = sessionManager.getUserName() ?: "Doctor"
        binding.tvDocProfileName.text = name
        binding.tvDocProfileInitial.text = name.take(2).uppercase()
    }

    private fun loadDoctorProfile() {
        val userId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            val result = doctorRepository.getDoctorById(userId)
            result.fold(
                onSuccess = { doc ->
                    currentDoctor = doc
                    bindDoctorData(doc)
                },
                onFailure = {
                    // Try fallback
                }
            )
        }
    }

    private fun bindDoctorData(doc: Doctor) {
        val name = doc.profile?.name ?: sessionManager.getUserName() ?: "Doctor"
        binding.tvDocProfileName.text = name
        binding.tvDocProfileInitial.text = name.take(2).uppercase()
        binding.tvDocProfileSpecialty.text = doc.specialization.ifEmpty { "General Physician" }

        binding.switchDocAvailability.isChecked = doc.available
        binding.tvDocProfileHospital.text = doc.hospitalName?.ifEmpty { "Medicare Central Hospital" } ?: "Medicare Central Hospital"
        binding.tvDocProfileDepartment.text = doc.department?.ifEmpty { doc.specialization } ?: doc.specialization
        binding.tvDocProfileQualExp.text = "${doc.qualification.ifEmpty { "MBBS" }} • ${doc.experience.ifEmpty { "5 Years" }}"
        val fee = doc.consultationFee ?: 50.0
        binding.tvDocProfileFee.text = String.format("$%.2f", fee)
        val days = doc.availableDays ?: "Mon,Tue,Wed,Thu,Fri"
        val start = doc.startTime ?: "09:00 AM"
        val end = doc.endTime ?: "05:00 PM"
        binding.tvDocProfileSchedule.text = "$days • $start - $end"
        binding.tvDocProfileBio.text = doc.about?.ifEmpty { "Dedicated clinician committed to patient care." }
            ?: "Dedicated clinician committed to patient care."
    }

    private fun setupListeners() {
        binding.switchDocAvailability.setOnCheckedChangeListener { _, isChecked ->
            val docId = currentDoctor?.id ?: return@setOnCheckedChangeListener
            lifecycleScope.launch {
                doctorRepository.updateDoctorAvailability(docId, isChecked)
            }
        }

        AnimationUtils.applyPressAnimation(binding.btnDoctorEditProfile) {
            showEditDialog()
        }

        AnimationUtils.applyPressAnimation(binding.btnDoctorLogout) {
            DialogUtils.showConfirmation(
                this,
                title = "Log Out",
                message = "Are you sure you want to log out from Doctor portal?"
            ) {
                lifecycleScope.launch {
                    authRepository.signOut()
                    sessionManager.clearSession()
                    val intent = Intent(this@DoctorProfileActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun showEditDialog() {
        val doc = currentDoctor ?: return

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_doctor_profile, null)
        val etSpecialty = view.findViewById<EditText>(R.id.etEditDocSpecialty)
        val etQual = view.findViewById<EditText>(R.id.etEditDocQual)
        val etExp = view.findViewById<EditText>(R.id.etEditDocExp)
        val etHospital = view.findViewById<EditText>(R.id.etEditDocHospital)
        val etDepartment = view.findViewById<EditText>(R.id.etEditDocDepartment)
        val etFee = view.findViewById<EditText>(R.id.etEditDocFee)
        val etDays = view.findViewById<EditText>(R.id.etEditDocDays)
        val etBio = view.findViewById<EditText>(R.id.etEditDocBio)

        etSpecialty.setText(doc.specialization)
        etQual.setText(doc.qualification)
        etExp.setText(doc.experience)
        etHospital.setText(doc.hospitalName.orEmpty())
        etDepartment.setText(doc.department.orEmpty())
        etFee.setText((doc.consultationFee ?: 50.0).toString())
        etDays.setText(doc.availableDays ?: "Mon,Tue,Wed,Thu,Fri")
        etBio.setText(doc.about.orEmpty())

        AlertDialog.Builder(this)
            .setTitle("Edit Doctor Profile")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val specialty = etSpecialty.text.toString().trim()
                val qual = etQual.text.toString().trim()
                val exp = etExp.text.toString().trim()
                val hospital = etHospital.text.toString().trim()
                val department = etDepartment.text.toString().trim()
                val fee = etFee.text.toString().toDoubleOrNull() ?: 50.0
                val days = etDays.text.toString().trim().ifEmpty { "Mon,Tue,Wed,Thu,Fri" }
                val bio = etBio.text.toString().trim()

                lifecycleScope.launch {
                    val result = doctorRepository.updateDoctorProfile(
                        doctorId = doc.id,
                        profileId = doc.profileId,
                        name = doc.profile?.name ?: "",
                        specialization = specialty,
                        qualification = qual,
                        experience = exp,
                        hospital = hospital,
                        department = department,
                        fee = fee,
                        days = days,
                        startTime = doc.startTime ?: "09:00 AM",
                        endTime = doc.endTime ?: "05:00 PM",
                        bio = bio,
                        available = binding.switchDocAvailability.isChecked
                    )

                    result.fold(
                        onSuccess = {
                            DialogUtils.showSuccess(
                                this@DoctorProfileActivity,
                                title = "Profile Updated",
                                message = "Your professional profile has been saved."
                            )
                            loadDoctorProfile()
                        },
                        onFailure = { error ->
                            DialogUtils.showError(
                                this@DoctorProfileActivity,
                                title = "Update Failed",
                                message = error.localizedMessage ?: "Could not update profile"
                            )
                        }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
