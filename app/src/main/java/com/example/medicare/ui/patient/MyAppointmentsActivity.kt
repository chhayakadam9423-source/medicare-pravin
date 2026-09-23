package com.example.medicare.ui.patient

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.adapter.AppointmentAdapter
import com.example.medicare.data.model.Appointment
import com.example.medicare.data.repository.AppointmentRepository
import com.example.medicare.databinding.ActivityMyAppointmentsBinding
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyAppointmentsBinding
    private val appointmentRepository = AppointmentRepository()
    private lateinit var sessionManager: SessionManager
    private lateinit var appointmentAdapter: AppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        loadAppointments()
    }

    private fun setupUI() {
        binding.toolbarMyAppointments.setNavigationOnClickListener { finish() }

        appointmentAdapter = AppointmentAdapter(
            appointments = emptyList(),
            onCancelClick = { appointment ->
                showCancelConfirmation(appointment)
            },
            onViewPrescriptionClick = { appointment ->
                val intent = Intent(this, PrescriptionActivity::class.java).apply {
                    putExtra("APPOINTMENT_ID", appointment.id)
                    putExtra("DOCTOR_NAME", appointment.doctor?.profile?.name)
                    putExtra("DOCTOR_SPECIALIZATION", appointment.doctor?.specialization)
                }
                startActivity(intent)
            }
        )

        binding.rvMyAppointments.apply {
            layoutManager = LinearLayoutManager(this@MyAppointmentsActivity)
            adapter = appointmentAdapter
        }

        binding.swipeRefreshAppointments.setOnRefreshListener {
            loadAppointments()
        }

        binding.btnEmptyBookDoctor.setOnClickListener {
            startActivity(Intent(this, DoctorListActivity::class.java))
            finish()
        }
    }

    private fun loadAppointments() {
        val patientId = sessionManager.getUserId() ?: "patient-user"

        binding.pbAppointmentsLoading.visibility = View.VISIBLE
        binding.llEmptyAppointments.visibility = View.GONE

        lifecycleScope.launch {
            val result = appointmentRepository.getPatientAppointments(patientId)
            binding.pbAppointmentsLoading.visibility = View.GONE
            binding.swipeRefreshAppointments.isRefreshing = false

            result.fold(
                onSuccess = { list ->
                    appointmentAdapter.updateData(list)
                    if (list.isEmpty()) {
                        binding.llEmptyAppointments.visibility = View.VISIBLE
                        binding.rvMyAppointments.visibility = View.GONE
                    } else {
                        binding.llEmptyAppointments.visibility = View.GONE
                        binding.rvMyAppointments.visibility = View.VISIBLE
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("MyAppointmentsActivity", "Error loading appointments: ${error.message}", error)
                    binding.llEmptyAppointments.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun showCancelConfirmation(appointment: Appointment) {
        DialogUtils.showConfirmation(
            this,
            title = "Cancel Appointment",
            message = "Are you sure you want to cancel your consultation with ${appointment.doctor?.profile?.name ?: "the doctor"}?"
        ) {
            cancelAppointment(appointment.id)
        }
    }

    private fun cancelAppointment(appointmentId: String) {
        binding.pbAppointmentsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = appointmentRepository.updateAppointmentStatus(appointmentId, "cancelled")
            binding.pbAppointmentsLoading.visibility = View.GONE

            result.fold(
                onSuccess = {
                    DialogUtils.showSuccess(
                        this@MyAppointmentsActivity,
                        title = "Cancelled",
                        message = "Your appointment has been cancelled successfully."
                    )
                    loadAppointments()
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@MyAppointmentsActivity,
                        title = "Error",
                        message = error.localizedMessage ?: "Failed to cancel appointment"
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
