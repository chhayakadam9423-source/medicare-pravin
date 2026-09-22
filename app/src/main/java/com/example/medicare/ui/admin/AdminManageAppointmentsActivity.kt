package com.example.medicare.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.adapter.AdminAppointmentAdapter
import com.example.medicare.data.model.Appointment
import com.example.medicare.data.repository.AppointmentRepository
import com.example.medicare.databinding.ActivityAdminManageAppointmentsBinding
import com.example.medicare.utils.DialogUtils
import kotlinx.coroutines.launch

class AdminManageAppointmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminManageAppointmentsBinding
    private val appointmentRepository = AppointmentRepository()
    private lateinit var adapter: AdminAppointmentAdapter
    private var allAppointments: List<Appointment> = emptyList()
    private var selectedFilterStatus: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminManageAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarManageAppointments.setNavigationOnClickListener { finish() }

        setupUI()
        setupListeners()
        loadAppointments()
    }

    private fun setupUI() {
        adapter = AdminAppointmentAdapter(
            appointments = emptyList(),
            onUpdateStatusClick = { apt ->
                showStatusChangeDialog(apt)
            }
        )

        binding.rvAdminAppointments.apply {
            layoutManager = LinearLayoutManager(this@AdminManageAppointmentsActivity)
            adapter = this@AdminManageAppointmentsActivity.adapter
        }
    }

    private fun setupListeners() {
        binding.chipGroupAdminAptFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedFilterStatus = when {
                checkedIds.contains(binding.chipFilterPending.id) -> "pending"
                checkedIds.contains(binding.chipFilterConfirmed.id) -> "confirmed"
                checkedIds.contains(binding.chipFilterCompleted.id) -> "completed"
                checkedIds.contains(binding.chipFilterCancelled.id) -> "cancelled"
                else -> "all"
            }
            applyFilters()
        }

        binding.etSearchAppointments.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadAppointments() {
        binding.pbAdminAppointmentsLoading.visibility = View.VISIBLE
        binding.tvEmptyAppointments.visibility = View.GONE

        lifecycleScope.launch {
            val result = appointmentRepository.getAllAppointments()
            binding.pbAdminAppointmentsLoading.visibility = View.GONE

            result.fold(
                onSuccess = { list ->
                    allAppointments = list
                    applyFilters()
                },
                onFailure = {
                    binding.tvEmptyAppointments.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun applyFilters() {
        val query = binding.etSearchAppointments.text?.toString()?.trim()?.lowercase().orEmpty()

        val filtered = allAppointments.filter { apt ->
            val matchesStatus = if (selectedFilterStatus == "all") {
                true
            } else {
                apt.status.equals(selectedFilterStatus, ignoreCase = true)
            }

            val patientName = apt.patient?.profile?.name?.lowercase().orEmpty()
            val doctorName = apt.doctor?.profile?.name?.lowercase().orEmpty()
            val specialty = apt.doctor?.specialization?.lowercase().orEmpty()

            val matchesSearch = query.isEmpty() ||
                    patientName.contains(query) ||
                    doctorName.contains(query) ||
                    specialty.contains(query)

            matchesStatus && matchesSearch
        }

        adapter.updateData(filtered)
        binding.tvEmptyAppointments.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showStatusChangeDialog(apt: Appointment) {
        val options = arrayOf("Confirm Appointment", "Mark as Completed", "Cancel Appointment")
        val statusValues = arrayOf("confirmed", "completed", "cancelled")

        AlertDialog.Builder(this)
            .setTitle("Update Appointment Status")
            .setItems(options) { _, which ->
                val newStatus = statusValues[which]
                updateStatus(apt.id, newStatus)
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    private fun updateStatus(appointmentId: String, newStatus: String) {
        binding.pbAdminAppointmentsLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = appointmentRepository.updateAppointmentStatus(appointmentId, newStatus)
            binding.pbAdminAppointmentsLoading.visibility = View.GONE

            result.fold(
                onSuccess = {
                    DialogUtils.showSuccess(
                        this@AdminManageAppointmentsActivity,
                        title = "Status Updated",
                        message = "The appointment status is now '$newStatus'."
                    )
                    loadAppointments()
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@AdminManageAppointmentsActivity,
                        title = "Update Failed",
                        message = error.localizedMessage ?: "Could not update status"
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
