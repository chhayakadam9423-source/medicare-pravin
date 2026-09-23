package com.example.medicare.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.adapter.DoctorAdapter
import com.example.medicare.data.model.Doctor
import com.example.medicare.data.repository.AdminRepository
import com.example.medicare.data.repository.DoctorRepository
import com.example.medicare.databinding.ActivityAdminManageDoctorsBinding
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import kotlinx.coroutines.launch

class AdminManageDoctorsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminManageDoctorsBinding
    private val doctorRepository = DoctorRepository()
    private val adminRepository = AdminRepository()
    private lateinit var doctorAdapter: DoctorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminManageDoctorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        loadDoctors()
    }

    private fun setupUI() {
        binding.toolbarManageDoctors.setNavigationOnClickListener { finish() }

        doctorAdapter = DoctorAdapter(
            doctors = emptyList(),
            onDoctorClick = { doc ->
                showDoctorOptions(doc)
            },
            onBookClick = { doc ->
                showDoctorOptions(doc)
            }
        )

        binding.rvAdminDoctors.apply {
            layoutManager = LinearLayoutManager(this@AdminManageDoctorsActivity)
            adapter = doctorAdapter
        }
    }

    private fun setupListeners() {
        AnimationUtils.applyPressAnimation(binding.fabAddDoctor) {
            startActivity(Intent(this, AdminAddDoctorActivity::class.java))
        }
    }

    private fun loadDoctors() {
        binding.pbAdminDoctorsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = doctorRepository.getAllDoctors()
            binding.pbAdminDoctorsLoading.visibility = View.GONE

            result.fold(
                onSuccess = { list ->
                    doctorAdapter.updateData(list)
                },
                onFailure = { error ->
                    android.util.Log.e("AdminManageDoctorsActivity", "Error loading doctors: ${error.message}", error)
                }
            )
        }
    }

    private fun showDoctorOptions(doctor: Doctor) {
        val name = doctor.profile?.name ?: "Doctor"
        val currentStatus = if (doctor.available) "Available" else "Unavailable"
        val actionText = if (doctor.available) "Set to Unavailable" else "Set to Available"

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Manage $name")
            .setMessage("Specialization: ${doctor.specialization}\nStatus: $currentStatus\n\nChoose an action:")
            .setPositiveButton(actionText) { _, _ ->
                toggleDoctorAvailability(doctor.id, !doctor.available)
            }
            .setNegativeButton("Delete") { _, _ ->
                deleteDoctor(doctor.id)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun toggleDoctorAvailability(doctorId: String, newAvailable: Boolean) {
        binding.pbAdminDoctorsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = doctorRepository.updateDoctorAvailability(doctorId, newAvailable)
            binding.pbAdminDoctorsLoading.visibility = View.GONE

            result.fold(
                onSuccess = {
                    DialogUtils.showSuccess(
                        this@AdminManageDoctorsActivity,
                        title = "Availability Updated",
                        message = "Doctor availability changed to ${if (newAvailable) "Available" else "Unavailable"}."
                    )
                    loadDoctors()
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@AdminManageDoctorsActivity,
                        title = "Update Failed",
                        message = error.localizedMessage ?: "Could not update doctor availability"
                    )
                }
            )
        }
    }

    private fun deleteDoctor(doctorId: String) {
        binding.pbAdminDoctorsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = adminRepository.deleteDoctor(doctorId)
            binding.pbAdminDoctorsLoading.visibility = View.GONE

            result.fold(
                onSuccess = {
                    DialogUtils.showSuccess(
                        this@AdminManageDoctorsActivity,
                        title = "Success",
                        message = "Doctor record updated successfully."
                    )
                    loadDoctors()
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@AdminManageDoctorsActivity,
                        title = "Action Failed",
                        message = error.localizedMessage ?: "Could not remove doctor"
                    )
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadDoctors()
    }
}
