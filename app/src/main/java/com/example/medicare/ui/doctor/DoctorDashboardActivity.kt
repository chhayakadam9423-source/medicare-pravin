package com.example.medicare.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.R
import com.example.medicare.adapter.DoctorAppointmentAdapter
import com.example.medicare.data.model.Appointment
import com.example.medicare.data.repository.AppointmentRepository
import com.example.medicare.databinding.ActivityDoctorDashboardBinding
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorDashboardBinding
    private val appointmentRepository = AppointmentRepository()
    private lateinit var sessionManager: SessionManager
    private lateinit var appointmentAdapter: DoctorAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
        loadDoctorAppointments()
    }

    private fun setupUI() {
        val doctorName = sessionManager.getUserName() ?: "Dr. Sarah Jenkins"
        binding.tvDoctorDashGreeting.text = "Welcome, $doctorName"
        binding.tvDoctorAvatarInitial.text = doctorName.take(2).uppercase()

        appointmentAdapter = DoctorAppointmentAdapter(
            appointments = emptyList(),
            onAcceptClick = { apt -> updateStatus(apt.id, "confirmed") },
            onRejectClick = { apt -> updateStatus(apt.id, "rejected") },
            onCompleteClick = { apt -> updateStatus(apt.id, "completed") },
            onAddPrescriptionClick = { apt ->
                val intent = Intent(this, AddPrescriptionActivity::class.java).apply {
                    putExtra("APPOINTMENT_ID", apt.id)
                    putExtra("PATIENT_NAME", apt.patient?.profile?.name)
                    putExtra("PATIENT_ID", apt.patientId)
                    putExtra("DOCTOR_ID", apt.doctorId)
                }
                startActivity(intent)
            }
        )

        binding.rvDoctorDashboardApts.apply {
            layoutManager = LinearLayoutManager(this@DoctorDashboardActivity)
            adapter = appointmentAdapter
        }

        binding.bottomNavigationDoctor.selectedItemId = R.id.nav_doc_dashboard
        binding.bottomNavigationDoctor.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_doc_dashboard -> true
                R.id.nav_doc_appointments -> {
                    startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
                    false
                }
                R.id.nav_doc_profile -> {
                    startActivity(Intent(this, DoctorProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun setupListeners() {
        binding.tvDoctorViewAllApts.setOnClickListener {
            startActivity(Intent(this, DoctorAppointmentsActivity::class.java))
        }

        binding.btnDoctorProfileAvatar.setOnClickListener {
            startActivity(Intent(this, DoctorProfileActivity::class.java))
        }
    }

    private fun loadDoctorAppointments() {
        val doctorId = sessionManager.getUserId() ?: "doc-1"
        binding.pbDoctorDashLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = appointmentRepository.getDoctorAppointments(doctorId)
            binding.pbDoctorDashLoading.visibility = View.GONE

            result.fold(
                onSuccess = { list ->
                    // Calculate counts
                    val pending = list.count { it.status.equals("pending", ignoreCase = true) }
                    val confirmed = list.count { it.status.equals("confirmed", ignoreCase = true) }
                    val completed = list.count { it.status.equals("completed", ignoreCase = true) }

                    binding.tvPendingCount.text = pending.toString()
                    binding.tvConfirmedCount.text = confirmed.toString()
                    binding.tvCompletedCount.text = completed.toString()

                    // Display recent
                    appointmentAdapter.updateData(list)
                },
                onFailure = { error ->
                    android.util.Log.e("DoctorDashboard", "Error loading appointments: ${error.message}", error)
                }
            )
        }
    }

    private fun updateStatus(appointmentId: String, newStatus: String) {
        binding.pbDoctorDashLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = appointmentRepository.updateAppointmentStatus(appointmentId, newStatus)
            binding.pbDoctorDashLoading.visibility = View.GONE

            result.fold(
                onSuccess = {
                    DialogUtils.showSuccess(
                        this@DoctorDashboardActivity,
                        title = "Status Updated",
                        message = "Appointment status successfully updated to ${newStatus.uppercase()}."
                    )
                    loadDoctorAppointments()
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@DoctorDashboardActivity,
                        title = "Update Failed",
                        message = error.localizedMessage ?: "Could not update appointment"
                    )
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigationDoctor.selectedItemId = R.id.nav_doc_dashboard
        loadDoctorAppointments()
    }
}
