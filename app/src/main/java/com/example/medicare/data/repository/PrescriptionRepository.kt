package com.example.medicare.data.repository

import com.example.medicare.data.SupabaseManager
import com.example.medicare.data.model.Doctor
import com.example.medicare.data.model.Prescription
import com.example.medicare.data.model.Profile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrescriptionRepository {
    private val db = SupabaseManager.db

    suspend fun createPrescription(
        appointmentId: String,
        patientId: String,
        doctorId: String,
        medicine: String,
        dosage: String,
        frequency: String,
        instructions: String
    ): Result<Prescription> = withContext(Dispatchers.IO) {
        try {
            val prescription = db.from("prescriptions").insert(
                mapOf(
                    "appointment_id" to appointmentId,
                    "patient_id" to patientId,
                    "doctor_id" to doctorId,
                    "medicine" to medicine,
                    "dosage" to dosage,
                    "frequency" to frequency,
                    "instructions" to instructions
                )
            ) {
                select()
            }.decodeSingle<Prescription>()

            Result.success(prescription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPrescriptionByAppointmentId(appointmentId: String): Result<Prescription?> = withContext(Dispatchers.IO) {
        try {
            val prescription = db.from("prescriptions")
                .select {
                    filter {
                        eq("appointment_id", appointmentId)
                    }
                }.decodeSingleOrNull<Prescription>()

            if (prescription != null) {
                val doctor = db.from("doctors").select {
                    filter { eq("id", prescription.doctorId) }
                }.decodeSingleOrNull<Doctor>()

                val docProfile = doctor?.let { d ->
                    db.from("profiles").select {
                        filter { eq("id", d.profileId) }
                    }.decodeSingleOrNull<Profile>()
                }

                Result.success(prescription.copy(doctor = doctor?.copy(profile = docProfile)))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
