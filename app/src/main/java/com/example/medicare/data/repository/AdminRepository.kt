package com.example.medicare.data.repository

import com.example.medicare.data.SupabaseManager
import com.example.medicare.data.model.Appointment
import com.example.medicare.data.model.DashboardStats
import com.example.medicare.data.model.Doctor
import com.example.medicare.data.model.Patient
import com.example.medicare.data.model.Profile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminRepository {
    private val db = SupabaseManager.db

    suspend fun getDashboardStats(): Result<DashboardStats> = withContext(Dispatchers.IO) {
        try {
            val doctors = db.from("doctors").select().decodeList<Doctor>()
            val patients = db.from("patients").select().decodeList<Patient>()
            val appointments = db.from("appointments").select().decodeList<Appointment>()
            val pendingCount = appointments.count { it.status == "pending" }

            Result.success(
                DashboardStats(
                    totalDoctors = doctors.size,
                    totalPatients = patients.size,
                    totalAppointments = appointments.size,
                    pendingAppointments = pendingCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllPatients(): Result<List<Patient>> = withContext(Dispatchers.IO) {
        try {
            val patients = db.from("patients").select().decodeList<Patient>()
            val profiles = db.from("profiles")
                .select { filter { eq("role", "patient") } }
                .decodeList<Profile>()
                .associateBy { it.id }

            val enriched = patients.map { p ->
                p.copy(profile = profiles[p.profileId])
            }
            Result.success(enriched)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addDoctor(
        name: String,
        phone: String,
        specialization: String,
        qualification: String,
        experience: String,
        hospital: String = "",
        department: String = "",
        consultationFee: Double = 0.0,
        availableDays: String = "Mon,Tue,Wed,Thu,Fri",
        startTime: String = "09:00 AM",
        endTime: String = "05:00 PM",
        about: String = "",
        imageUrl: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = AuthRepository().normalizePhone(phone)
            val profileId = java.util.UUID.randomUUID().toString()
            
            db.from("profiles").insert(
                mapOf(
                    "id" to profileId,
                    "name" to name,
                    "phone" to cleanPhone,
                    "role" to "doctor"
                )
            )

            try {
                db.from("doctors").insert(
                    mapOf(
                        "profile_id" to profileId,
                        "specialization" to specialization,
                        "qualification" to qualification,
                        "experience" to experience,
                        "hospital" to hospital,
                        "department" to department,
                        "consultation_fee" to consultationFee,
                        "available_days" to availableDays,
                        "start_time" to startTime,
                        "end_time" to endTime,
                        "about" to about,
                        "bio" to about,
                        "image_url" to imageUrl,
                        "available" to true
                    )
                )
            } catch (_: Exception) {
                db.from("doctors").insert(
                    mapOf(
                        "profile_id" to profileId,
                        "specialization" to specialization,
                        "qualification" to qualification,
                        "experience_years" to experience,
                        "hospital_name" to hospital,
                        "department" to department,
                        "consultation_fee" to consultationFee,
                        "available_days" to availableDays,
                        "start_time" to startTime,
                        "end_time" to endTime,
                        "bio" to about,
                        "profile_image_url" to imageUrl,
                        "available" to true
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDoctor(doctorId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = db.from("doctors").select {
                filter { eq("id", doctorId) }
            }.decodeSingleOrNull<Doctor>()

            if (doc != null) {
                db.from("doctors").delete { filter { eq("id", doctorId) } }
                db.from("profiles").delete { filter { eq("id", doc.profileId) } }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePatient(patientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val patient = db.from("patients").select {
                filter { eq("id", patientId) }
            }.decodeSingleOrNull<Patient>()

            if (patient != null) {
                db.from("patients").delete { filter { eq("id", patientId) } }
                db.from("profiles").delete { filter { eq("id", patient.profileId) } }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
