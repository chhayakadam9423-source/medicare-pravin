package com.example.medicare.ui.doctor

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.data.repository.PrescriptionRepository
import com.example.medicare.databinding.ActivityAddPrescriptionBinding
import com.example.medicare.utils.AnimationUtils
import com.example.medicare.utils.DialogUtils
import kotlinx.coroutines.launch

class AddPrescriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPrescriptionBinding
    private val prescriptionRepository = PrescriptionRepository()
    private var appointmentId: String = ""
    private var patientId: String = ""
    private var doctorId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPrescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appointmentId = intent.getStringExtra("APPOINTMENT_ID") ?: ""
        patientId = intent.getStringExtra("PATIENT_ID") ?: ""
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        val patientName = intent.getStringExtra("PATIENT_NAME") ?: "Patient"

        binding.toolbarAddPrescription.setNavigationOnClickListener { finish() }
        binding.tvRxPatientName.text = patientName

        setupListeners()
    }

    private fun setupListeners() {
        AnimationUtils.applyPressAnimation(binding.btnSavePrescription) {
            val medicine = binding.etRxMedicine.text?.toString()?.trim().orEmpty()
            val dosage = binding.etRxDosage.text?.toString()?.trim().orEmpty()
            val frequency = binding.etRxFrequency.text?.toString()?.trim().orEmpty()
            val instructions = binding.etRxInstructions.text?.toString()?.trim().orEmpty()

            if (medicine.isEmpty()) {
                binding.tilRxMedicine.error = "Medicine name is required"
                return@applyPressAnimation
            }
            binding.tilRxMedicine.error = null

            if (dosage.isEmpty()) {
                binding.tilRxDosage.error = "Dosage is required (e.g. 500mg)"
                return@applyPressAnimation
            }
            binding.tilRxDosage.error = null

            if (frequency.isEmpty()) {
                binding.tilRxFrequency.error = "Frequency is required (e.g. Twice daily)"
                return@applyPressAnimation
            }
            binding.tilRxFrequency.error = null

            savePrescription(medicine, dosage, frequency, instructions)
        }
    }

    private fun savePrescription(med: String, dos: String, freq: String, inst: String) {
        setLoading(true)

        lifecycleScope.launch {
            val result = prescriptionRepository.createPrescription(
                appointmentId = appointmentId,
                patientId = patientId,
                doctorId = doctorId,
                medicine = med,
                dosage = dos,
                frequency = freq,
                instructions = inst
            )

            setLoading(false)

            result.fold(
                onSuccess = {
                    DialogUtils.showSuccess(
                        this@AddPrescriptionActivity,
                        title = "Prescription Issued",
                        message = "The prescription has been securely saved to the patient's digital health record."
                    ) {
                        finish()
                    }
                },
                onFailure = { error ->
                    DialogUtils.showError(
                        this@AddPrescriptionActivity,
                        title = "Error",
                        message = error.localizedMessage ?: "Could not save prescription"
                    )
                }
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.pbSaveRxLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSavePrescription.isEnabled = !loading
        binding.btnSavePrescription.text = if (loading) "" else "SAVE PRESCRIPTION"
    }
}
