package com.example.medicare.ui.patient

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.R
import com.example.medicare.adapter.DoctorAdapter
import com.example.medicare.data.model.Doctor
import com.example.medicare.data.repository.DoctorRepository
import com.example.medicare.databinding.ActivityPatientDashboardBinding
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch

class PatientDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDashboardBinding
    private val doctorRepository = DoctorRepository()
    private lateinit var sessionManager: SessionManager
    private lateinit var doctorAdapter: DoctorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
        loadTopDoctors()
    }

    private fun setupUI() {
        val userName = sessionManager.getUserName() ?: "Patient"
        binding.tvPatientGreeting.text = "Hello, $userName"
        binding.tvAvatarInitial.text = userName.take(1).uppercase()

        // Setup Top Doctors RecyclerView
        doctorAdapter = DoctorAdapter(
            doctors = emptyList(),
            onDoctorClick = { doctor ->
                val intent = Intent(this, DoctorDetailsActivity::class.java).apply {
                    putExtra("DOCTOR_ID", doctor.id)
                    putExtra("DOCTOR_NAME", doctor.doctorName)
                    putExtra("DOCTOR_SPECIALIZATION", doctor.specialization)
                    putExtra("DOCTOR_QUALIFICATION", doctor.qualification)
                    putExtra("DOCTOR_EXPERIENCE", doctor.experience)
                    putExtra("DOCTOR_HOSPITAL", doctor.hospital)
                    putExtra("DOCTOR_DEPARTMENT", doctor.department)
                    putExtra("DOCTOR_FEE", doctor.consultationFee ?: 500.0)
                    putExtra("DOCTOR_DAYS", doctor.availableDays)
                    putExtra("DOCTOR_HOURS", doctor.workingHours)
                    putExtra("DOCTOR_ABOUT", doctor.about)
                    putExtra("DOCTOR_AVAILABLE", doctor.available)
                }
                startActivity(intent)
            },
            onBookClick = { doctor ->
                val intent = Intent(this, BookAppointmentActivity::class.java).apply {
                    putExtra("DOCTOR_ID", doctor.id)
                    putExtra("DOCTOR_NAME", doctor.doctorName)
                    putExtra("DOCTOR_SPECIALIZATION", doctor.specialization)
                    putExtra("DOCTOR_FEE", doctor.consultationFee ?: 500.0)
                }
                startActivity(intent)
            }
        )

        binding.rvDashboardDoctors.apply {
            layoutManager = LinearLayoutManager(this@PatientDashboardActivity)
            adapter = doctorAdapter
        }

        // Setup Bottom Navigation
        binding.bottomNavigationPatient.selectedItemId = R.id.nav_home
        binding.bottomNavigationPatient.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_doctors -> {
                    startActivity(Intent(this, DoctorListActivity::class.java))
                    false
                }
                R.id.nav_appointments -> {
                    startActivity(Intent(this, MyAppointmentsActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, PatientProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun setupListeners() {
        // Search Banner
        binding.cardSearchBar.setOnClickListener {
            startActivity(Intent(this, DoctorListActivity::class.java))
        }

        // View All Doctors
        binding.tvSeeAllDoctors.setOnClickListener {
            startActivity(Intent(this, DoctorListActivity::class.java))
        }

        // Category Cards
        binding.catCardiology.setOnClickListener {
            openSpecialty("Cardiologist")
        }
        binding.catNeurology.setOnClickListener {
            openSpecialty("Neurologist")
        }
        binding.catPediatrics.setOnClickListener {
            openSpecialty("Pediatrician")
        }
        binding.catOrthopedics.setOnClickListener {
            openSpecialty("Dentist")
        }

        // Profile Avatar
        binding.btnPatientProfileAvatar.setOnClickListener {
            startActivity(Intent(this, PatientProfileActivity::class.java))
        }
    }

    private fun openSpecialty(specialty: String) {
        val intent = Intent(this, DoctorListActivity::class.java).apply {
            putExtra("SPECIALTY_FILTER", specialty)
        }
        startActivity(intent)
    }

    private fun loadTopDoctors() {
        binding.pbDashboardDoctors.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = doctorRepository.getAllDoctors()
            binding.pbDashboardDoctors.visibility = View.GONE

            result.fold(
                onSuccess = { list ->
                    // Display all dynamically loaded available doctors from Supabase
                    doctorAdapter.updateData(list)
                },
                onFailure = {
                    // Handled gracefully with fallback
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigationPatient.selectedItemId = R.id.nav_home
        loadTopDoctors()
    }
}
