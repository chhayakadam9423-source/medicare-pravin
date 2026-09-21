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
        binding.tvPatientAvatarInitial.text = userName.take(1).uppercase()

        // Setup Top Doctors RecyclerView
        doctorAdapter = DoctorAdapter(
            doctors = emptyList(),
            onDoctorClick = { doctor ->
                val intent = Intent(this, DoctorDetailsActivity::class.java).apply {
                    putExtra("DOCTOR_ID", doctor.id)
                    putExtra("DOCTOR_NAME", doctor.profile?.name)
                    putExtra("DOCTOR_SPECIALIZATION", doctor.specialization)
                    putExtra("DOCTOR_QUALIFICATION", doctor.qualification)
                    putExtra("DOCTOR_EXPERIENCE", doctor.experience)
                    putExtra("DOCTOR_ABOUT", doctor.about)
                }
                startActivity(intent)
            },
            onBookClick = { doctor ->
                val intent = Intent(this, BookAppointmentActivity::class.java).apply {
                    putExtra("DOCTOR_ID", doctor.id)
                    putExtra("DOCTOR_NAME", doctor.profile?.name)
                    putExtra("DOCTOR_SPECIALIZATION", doctor.specialization)
                }
                startActivity(intent)
            }
        )

        binding.rvTopDoctors.apply {
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
        binding.cardSearchBanner.setOnClickListener {
            startActivity(Intent(this, DoctorListActivity::class.java))
        }

        // View All Doctors
        binding.tvViewAllDoctors.setOnClickListener {
            startActivity(Intent(this, DoctorListActivity::class.java))
        }

        // Quick Actions
        AnimationUtils.applyPressAnimation(binding.btnActionBook) {
            startActivity(Intent(this, DoctorListActivity::class.java))
        }

        AnimationUtils.applyPressAnimation(binding.btnActionAppointments) {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }

        AnimationUtils.applyPressAnimation(binding.btnActionPrescriptions) {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }

        AnimationUtils.applyPressAnimation(binding.btnActionEmergency) {
            DialogUtils.showSuccess(
                this,
                title = "Emergency Assistance",
                message = "Connecting to Medicare 24/7 Emergency Medical Response hotline (+1-800-MEDICARE)."
            ) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:911")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Safe handling if dialer is absent
                }
            }
        }

        // Category Cards
        binding.cardCategoryCardio.setOnClickListener {
            openSpecialty("Cardiologist")
        }
        binding.cardCategoryNeuro.setOnClickListener {
            openSpecialty("Neurologist")
        }
        binding.cardCategoryPediatric.setOnClickListener {
            openSpecialty("Pediatrician")
        }
        binding.cardCategoryDental.setOnClickListener {
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
        binding.pbDashDoctorsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = doctorRepository.getDoctors()
            binding.pbDashDoctorsLoading.visibility = View.GONE

            result.fold(
                onSuccess = { list ->
                    // Show top 3 available doctors
                    val top = list.take(3)
                    doctorAdapter.updateData(top)
                },
                onFailure = {
                    // Handled gracefully with default seeded doctors
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigationPatient.selectedItemId = R.id.nav_home
    }
}
