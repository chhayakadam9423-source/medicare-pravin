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
                onFailure = {
                    // Safe handling
                }
            )
        }
    }

    private fun showDoctorOptions(doctor: Doctor) {
        val name = doctor.profile?.name ?: "Doctor"
        DialogUtils.showConfirmation(
            this,
            title = "Manage $name",
            message = "Specialization: ${doctor.specialization}\nQualification: ${doctor.qualification}\n\nWould you like to toggle availability or remove this physician?"
        ) {
            // Admin action
            deleteDoctor(doctor.id)
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
