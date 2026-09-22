package com.example.medicare.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.adapter.PatientAdapter
import com.example.medicare.data.model.Patient
import com.example.medicare.data.repository.AdminRepository
import com.example.medicare.databinding.ActivityAdminManagePatientsBinding
import com.example.medicare.utils.DialogUtils
import kotlinx.coroutines.launch

class AdminManagePatientsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminManagePatientsBinding
    private val adminRepository = AdminRepository()
    private lateinit var patientAdapter: PatientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminManagePatientsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarManagePatients.setNavigationOnClickListener { finish() }

        setupUI()
        loadPatients()
    }

    private fun setupUI() {
        patientAdapter = PatientAdapter(
            patients = emptyList(),
            onPatientClick = { patient ->
                val name = patient.profile?.name ?: "Patient"
                DialogUtils.showSuccess(
                    this,
                    title = name,
                    message = "Phone: ${patient.profile?.phone}\nBlood Group: ${patient.bloodGroup}\nGender: ${patient.gender}"
                )
            }
        )

        binding.rvAdminPatients.apply {
            layoutManager = LinearLayoutManager(this@AdminManagePatientsActivity)
            adapter = patientAdapter
        }
    }

    private fun loadPatients() {
        binding.pbAdminPatientsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = adminRepository.getAllPatients()
            binding.pbAdminPatientsLoading.visibility = View.GONE

            result.fold(
                onSuccess = { list ->
                    patientAdapter.updateData(list)
                },
                onFailure = {
                    // Safe handling
                }
            )
        }
    }
}
