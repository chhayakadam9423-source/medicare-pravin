package com.example.medicare.ui.patient

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.R
import com.example.medicare.data.repository.AppointmentRepository
import com.example.medicare.databinding.ActivityBookAppointmentBinding
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import com.example.medicare.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAppointmentBinding
    private val appointmentRepository = AppointmentRepository()
    private lateinit var sessionManager: SessionManager

    private var doctorId: String = ""
    private var doctorName: String = ""
    private var selectedDate: String = "2026-09-22"
    private var selectedTime: String = "09:30 AM"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        doctorId = intent.getStringExtra("DOCTOR_ID").orEmpty()
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Doctor"
        val doctorSpecialty = intent.getStringExtra("DOCTOR_SPECIALIZATION") ?: "Specialist"
        val doctorFee = intent.getDoubleExtra("DOCTOR_FEE", 500.0)

        binding.toolbarBookAppointment.setNavigationOnClickListener { finish() }

        binding.tvBookDocName.text = doctorName
        binding.tvBookDocSpecialty.text = "$doctorSpecialty • Fee: ₹${doctorFee.toInt()}"
        binding.tvBookDocInitial.text = doctorName.replace("Dr.", "").trim().take(2).uppercase().ifBlank { "DR" }

        // Default Date (Tomorrow)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(calendar.time)
        binding.etBookDate.setText(selectedDate)

        setupListeners()
    }

    private fun setupListeners() {
        // Date Picker
        binding.etBookDate.setOnClickListener {
            showDatePicker()
        }
        binding.tilBookDate.setEndIconOnClickListener {
            showDatePicker()
        }

        // Time Slots
        binding.chipGroupTimeSlots.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedTime = when {
                checkedIds.contains(R.id.chipSlot1) -> "09:30 AM"
                checkedIds.contains(R.id.chipSlot2) -> "11:00 AM"
                checkedIds.contains(R.id.chipSlot3) -> "02:30 PM"
                checkedIds.contains(R.id.chipSlot4) -> "04:15 PM"
                else -> "10:00 AM"
            }
        }

        // Confirm Booking
        AnimationUtils.applyPressAnimation(binding.btnConfirmBooking) {
            if (!binding.btnConfirmBooking.isEnabled) return@applyPressAnimation

            val reason = binding.etBookReason.text?.toString()?.trim().orEmpty()

            if (reason.isEmpty()) {
                binding.tilBookReason.error = "Please explain the reason for consultation"
                return@applyPressAnimation
            }
            binding.tilBookReason.error = null

            val patientId = sessionManager.getPatientId()
                ?: sessionManager.getUserId()
                ?: ""

            if (patientId.isBlank()) {
                DialogUtils.showError(
                    this,
                    title = "Authentication Required",
                    message = "Please sign in to schedule an appointment."
                )
                return@applyPressAnimation
            }

            if (doctorId.isBlank()) {
                DialogUtils.showError(
                    this,
                    title = "Doctor Selection Error",
                    message = "No doctor was selected for this consultation."
                )
                return@applyPressAnimation
            }

            performBooking(patientId, doctorId, selectedDate, selectedTime, reason)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val chosenCalendar = Calendar.getInstance()
                chosenCalendar.set(selectedYear, selectedMonth, selectedDay)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = sdf.format(chosenCalendar.time)
                binding.etBookDate.setText(selectedDate)
            },
            year,
            month,
            day
        )
        // Prevent booking in the past
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun performBooking(
        patientId: String,
        docId: String,
        date: String,
        time: String,
        reason: String
    ) {
        setLoading(true)

        lifecycleScope.launch {
            val result = appointmentRepository.createAppointment(
                patientId = patientId,
                doctorId = docId,
                date = date,
                time = time,
                reason = reason
            )

            setLoading(false)

            result.fold(
                onSuccess = { _ ->
                    DialogUtils.showSuccess(
                        this@BookAppointmentActivity,
                        title = "Appointment Booked!",
                        message = "Your appointment with $doctorName on $date at $time has been scheduled."
                    ) {
                        val intent = Intent(this@BookAppointmentActivity, MyAppointmentsActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@BookAppointmentActivity,
                        title = "Booking Error",
                        message = error.localizedMessage ?: "Unable to schedule appointment. Please try again."
                    )
                }
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.pbBookingLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmBooking.isEnabled = !loading
        binding.btnConfirmBooking.text = if (loading) "" else getString(R.string.confirm_booking)
    }
}
