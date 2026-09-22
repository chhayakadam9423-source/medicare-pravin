package com.example.medicare.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private var allPatients: List<Patient> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminManagePatientsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarManagePatients.setNavigationOnClickListener { finish() }

        setupUI()
        setupSearch()
        loadPatients()
    }

    private fun setupUI() {
        patientAdapter = PatientAdapter(
            patients = emptyList(),
            onPatientClick = { patient ->
                showPatientDetailsDialog(patient)
            }
        )

        binding.rvAdminPatients.apply {
            layoutManager = LinearLayoutManager(this@AdminManagePatientsActivity)
            adapter = patientAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchPatients.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPatients(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterPatients(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            allPatients
        } else {
            allPatients.filter { p ->
                val name = p.profile?.name?.lowercase().orEmpty()
                val phone = p.profile?.phone?.lowercase().orEmpty()
                val blood = p.bloodGroup?.lowercase().orEmpty()
                name.contains(q) || phone.contains(q) || blood.contains(q)
            }
        }
        patientAdapter.updateData(filtered)
        binding.tvEmptyPatients.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadPatients() {
        binding.pbAdminPatientsLoading.visibility = View.VISIBLE
        binding.tvEmptyPatients.visibility = View.GONE

        lifecycleScope.launch {
            val result = adminRepository.getAllPatients()
            binding.pbAdminPatientsLoading.visibility = View.GONE

            result.fold(
                onSuccess = { list ->
                    allPatients = list
                    filterPatients(binding.etSearchPatients.text?.toString().orEmpty())
                },
                onFailure = {
                    binding.tvEmptyPatients.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun showPatientDetailsDialog(patient: Patient) {
        val name = patient.profile?.name ?: "Patient"
        val phone = patient.profile?.phone ?: "N/A"
        val gender = patient.gender ?: "N/A"
        val dob = patient.dateOfBirth ?: "N/A"
        val bloodGroup = patient.bloodGroup ?: "N/A"
        val address = patient.address ?: "N/A"
        val emergency = patient.emergencyContact ?: "N/A"

        val detailsMessage = """
            📱 Mobile: $phone
            👤 Gender: $gender
            🎂 Date of Birth: $dob
            🩸 Blood Group: $bloodGroup
            🏠 Address: $address
            🚨 Emergency Contact: $emergency
        """.trimIndent()

        DialogUtils.showConfirmation(
            this,
            title = name,
            message = "$detailsMessage\n\nWould you like to delete this patient record?"
        ) {
            deletePatient(patient.id)
        }
    }

    private fun deletePatient(patientId: String) {
        binding.pbAdminPatientsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = adminRepository.deletePatient(patientId)
            binding.pbAdminPatientsLoading.visibility = View.GONE

            result.fold(
                onSuccess = {
                    DialogUtils.showSuccess(
                        this@AdminManagePatientsActivity,
                        title = "Patient Removed",
                        message = "The patient profile has been removed."
                    )
                    loadPatients()
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@AdminManagePatientsActivity,
                        title = "Action Failed",
                        message = error.localizedMessage ?: "Could not remove patient"
                    )
                }
            )
        }
    }
}
