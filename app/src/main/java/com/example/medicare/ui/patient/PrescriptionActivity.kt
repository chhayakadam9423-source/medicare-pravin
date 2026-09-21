package com.example.medicare.ui.patient

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.data.repository.PrescriptionRepository
import com.example.medicare.databinding.ActivityPrescriptionBinding
import kotlinx.coroutines.launch

class PrescriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrescriptionBinding
    private val prescriptionRepository = PrescriptionRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appointmentId = intent.getStringExtra("APPOINTMENT_ID") ?: ""
        val doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Dr. Specialist"
        val specialty = intent.getStringExtra("DOCTOR_SPECIALIZATION") ?: "Consultant"

        binding.toolbarPrescription.setNavigationOnClickListener { finish() }

        binding.tvRxDoctorName.text = doctorName
        binding.tvRxSpecialization.text = "$specialty • Medicare General Hospital"

        loadPrescription(appointmentId)
    }

    private fun loadPrescription(appointmentId: String) {
        lifecycleScope.launch {
            val result = prescriptionRepository.getPrescriptionForAppointment(appointmentId)

            result.fold(
                onSuccess = { prescription ->
                    prescription?.let {
                        binding.tvRxMedicine.text = it.medicine
                        binding.tvRxDosage.text = it.dosage
                        binding.tvRxFrequency.text = it.frequency
                        binding.tvRxInstructions.text = it.instructions
                    }
                },
                onFailure = {
                    // Uses default sample values in the layout
                }
            )
        }
    }
}
