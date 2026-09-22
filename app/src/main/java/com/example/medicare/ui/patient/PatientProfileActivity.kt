package com.example.medicare.ui.patient

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.R
import com.example.medicare.data.model.Patient
import com.example.medicare.data.repository.AuthRepository
import com.example.medicare.data.repository.PatientRepository
import com.example.medicare.databinding.ActivityPatientProfileBinding
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch

class PatientProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientProfileBinding
    private lateinit var sessionManager: SessionManager
    private val authRepository = AuthRepository()
    private val patientRepository = PatientRepository()
    private var currentPatient: Patient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
        loadPatientProfile()
    }

    private fun setupUI() {
        binding.toolbarPatientProfile.setNavigationOnClickListener { finish() }

        val name = sessionManager.getUserName() ?: "Alex Johnson"
        val phone = sessionManager.getUserPhone() ?: "+1-555-0201"

        binding.tvProfileName.text = name
        binding.tvProfileAvatarInitial.text = name.take(1).uppercase()
        binding.tvProfilePhone.text = phone
    }

    private fun loadPatientProfile() {
        val profileId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            val result = patientRepository.getPatientByProfileId(profileId)
            result.fold(
                onSuccess = { patient ->
                    currentPatient = patient
                    bindPatientData(patient)
                },
                onFailure = {
                    // Retain session data
                }
            )
        }
    }

    private fun bindPatientData(patient: Patient) {
        val name = patient.profile?.name ?: sessionManager.getUserName() ?: "Alex Johnson"
        val phone = patient.profile?.phone ?: sessionManager.getUserPhone() ?: ""

        binding.tvProfileName.text = name
        binding.tvProfileAvatarInitial.text = name.take(1).uppercase()
        binding.tvProfilePhone.text = phone.ifEmpty { "Not specified" }
        binding.tvProfileBloodGroup.text = patient.bloodGroup ?: "O+"
        binding.tvProfileGender.text = patient.gender ?: "Not specified"
        binding.tvProfileDOB.text = patient.dateOfBirth ?: "Not specified"
        binding.tvProfileAddress.text = patient.address ?: "Not specified"
        binding.tvProfileEmergency.text = patient.emergencyContact ?: "Not specified"
    }

    private fun setupListeners() {
        AnimationUtils.applyPressAnimation(binding.btnPatientEditProfile) {
            showEditDialog()
        }

        AnimationUtils.applyPressAnimation(binding.btnProfileLogout) {
            DialogUtils.showConfirmation(
                this,
                title = "Log Out",
                message = "Are you sure you want to sign out from your Medicare account?"
            ) {
                lifecycleScope.launch {
                    authRepository.signOut()
                    sessionManager.clearSession()
                    val intent = Intent(this@PatientProfileActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun showEditDialog() {
        val profileId = sessionManager.getUserId() ?: return
        val current = currentPatient

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_patient_profile, null)
        val etName = view.findViewById<EditText>(R.id.etEditPatientName)
        val etGender = view.findViewById<EditText>(R.id.etEditPatientGender)
        val etDOB = view.findViewById<EditText>(R.id.etEditPatientDOB)
        val etBlood = view.findViewById<EditText>(R.id.etEditPatientBloodGroup)
        val etAddress = view.findViewById<EditText>(R.id.etEditPatientAddress)
        val etEmergency = view.findViewById<EditText>(R.id.etEditPatientEmergency)

        etName.setText(current?.profile?.name ?: sessionManager.getUserName().orEmpty())
        etGender.setText(current?.gender.orEmpty())
        etDOB.setText(current?.dateOfBirth.orEmpty())
        etBlood.setText(current?.bloodGroup.orEmpty())
        etAddress.setText(current?.address.orEmpty())
        etEmergency.setText(current?.emergencyContact.orEmpty())

        AlertDialog.Builder(this)
            .setTitle("Edit Personal Details")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val gender = etGender.text.toString().trim()
                val dob = etDOB.text.toString().trim()
                val blood = etBlood.text.toString().trim()
                val address = etAddress.text.toString().trim()
                val emergency = etEmergency.text.toString().trim()

                lifecycleScope.launch {
                    val result = patientRepository.updatePatientProfile(
                        profileId = profileId,
                        name = name,
                        gender = gender,
                        dob = dob,
                        bloodGroup = blood,
                        address = address,
                        emergencyContact = emergency
                    )

                    result.fold(
                        onSuccess = {
                            if (name.isNotBlank()) {
                                sessionManager.saveSession(
                                    userId = profileId,
                                    name = name,
                                    phone = sessionManager.getUserPhone().orEmpty(),
                                    role = sessionManager.getUserRole().orEmpty()
                                )
                            }
                            DialogUtils.showSuccess(
                                this@PatientProfileActivity,
                                title = "Profile Saved",
                                message = "Your personal medical information has been updated."
                            )
                            loadPatientProfile()
                        },
                        onFailure = { error ->
                            DialogUtils.showError(
                                this@PatientProfileActivity,
                                title = "Update Failed",
                                message = error.localizedMessage ?: "Could not save details"
                            )
                        }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
