package com.example.medicare.data.repository

import com.example.medicare.data.SupabaseManager
import com.example.medicare.data.model.Appointment
import com.example.medicare.data.model.Doctor
import com.example.medicare.data.model.Patient
import com.example.medicare.data.model.Profile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppointmentRepository {
    private val db = SupabaseManager.db

    suspend fun createAppointment(
        patientId: String,
        doctorId: String,
        date: String,
        time: String,
        reason: String
    ): Result<Appointment> = withContext(Dispatchers.IO) {
        try {
            val inserted = db.from("appointments").insert(
                mapOf(
                    "patient_id" to patientId,
                    "doctor_id" to doctorId,
                    "appointment_date" to date,
                    "appointment_time" to time,
                    "reason" to reason,
                    "status" to "pending"
                )
            ) {
                select()
            }.decodeSingle<Appointment>()

            Result.success(inserted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPatientAppointments(patientId: String): Result<List<Appointment>> = withContext(Dispatchers.IO) {
        try {
            val list = db.from("appointments")
                .select {
                    filter {
                        eq("patient_id", patientId)
                    }
                }.decodeList<Appointment>()

            // Enrich with doctor and profile data
            val enriched = list.map { apt ->
                val doctor = db.from("doctors").select {
                    filter { eq("id", apt.doctorId) }
                }.decodeSingleOrNull<Doctor>()

                val docProfile = doctor?.let { d ->
                    db.from("profiles").select {
                        filter { eq("id", d.profileId) }
                    }.decodeSingleOrNull<Profile>()
                }

                apt.copy(doctor = doctor?.copy(profile = docProfile))
            }
            Result.success(enriched.sortedByDescending { it.appointmentDate })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoctorAppointments(doctorId: String): Result<List<Appointment>> = withContext(Dispatchers.IO) {
        try {
            val list = db.from("appointments")
                .select {
                    filter {
                        eq("doctor_id", doctorId)
                    }
                }.decodeList<Appointment>()

            val enriched = list.map { apt ->
                val patient = db.from("patients").select {
                    filter { eq("id", apt.patientId) }
                }.decodeSingleOrNull<Patient>()

                val patientProfile = patient?.let { p ->
                    db.from("profiles").select {
                        filter { eq("id", p.profileId) }
                    }.decodeSingleOrNull<Profile>()
                }

                apt.copy(patient = patient?.copy(profile = patientProfile))
            }
            Result.success(enriched.sortedByDescending { it.appointmentDate })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.from("appointments").update(
                mapOf("status" to newStatus)
            ) {
                filter {
                    eq("id", appointmentId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
