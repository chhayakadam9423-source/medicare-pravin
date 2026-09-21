package com.example.medicare.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.adapter.DoctorAppointmentAdapter
import com.example.medicare.data.model.Appointment
import com.example.medicare.data.repository.AppointmentRepository
import com.example.medicare.databinding.ActivityDoctorAppointmentsBinding
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch

class DoctorAppointmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorAppointmentsBinding
    private val appointmentRepository = AppointmentRepository()
    private lateinit var sessionManager: SessionManager
    private lateinit var appointmentAdapter: DoctorAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        loadAppointments()
    }

    private fun setupUI() {
        binding.toolbarDoctorAppointments.setNavigationOnClickListener { finish() }

        appointmentAdapter = DoctorAppointmentAdapter(
            appointments = emptyList(),
            onAcceptClick = { apt -> updateStatus(apt.id, "confirmed") },
            onRejectClick = { apt -> updateStatus(apt.id, "rejected") },
            onCompleteClick = { apt -> updateStatus(apt.id, "completed") },
            onAddPrescriptionClick = { apt ->
                val intent = Intent(this, AddPrescriptionActivity::class.java).apply {
                    putExtra("APPOINTMENT_ID", apt.id)
                    putExtra("PATIENT_NAME", apt.patient?.profile?.name)
                }
                startActivity(intent)
            }
        )

        binding.rvAllDoctorAppointments.apply {
            layoutManager = LinearLayoutManager(this@DoctorAppointmentsActivity)
            adapter = appointmentAdapter
        }

        binding.swipeRefreshDocAppointments.setOnRefreshListener {
            loadAppointments()
        }
    }

    private fun loadAppointments() {
        val doctorId = sessionManager.getUserId() ?: "doc-1"
        binding.pbDocAppointmentsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = appointmentRepository.getAppointmentsForDoctor(doctorId)
            binding.pbDocAppointmentsLoading.visibility = View.GONE
            binding.swipeRefreshDocAppointments.isRefreshing = false

            result.fold(
                onSuccess = { list ->
                    appointmentAdapter.updateData(list)
                },
                onFailure = {
                    // Safe handling
                }
            )
        }
    }

    private fun updateStatus(appointmentId: String, newStatus: String) {
        binding.pbDocAppointmentsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = appointmentRepository.updateAppointmentStatus(appointmentId, newStatus)
            binding.pbDocAppointmentsLoading.visibility = View.GONE

            result.fold(
                onSuccess = {
                    DialogUtils.showSuccess(
                        this@DoctorAppointmentsActivity,
                        title = "Appointment Updated",
                        message = "Status successfully changed to ${newStatus.uppercase()}."
                    )
                    loadAppointments()
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@DoctorAppointmentsActivity,
                        title = "Update Failed",
                        message = error.localizedMessage ?: "Could not update status."
                    )
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }
}
