package com.example.medicare.data.repository

import com.example.medicare.data.SupabaseManager
import com.example.medicare.data.model.Doctor
import com.example.medicare.data.model.Profile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DoctorRepository {
    private val db = SupabaseManager.db

    suspend fun getAllDoctors(): Result<List<Doctor>> = withContext(Dispatchers.IO) {
        try {
            val doctors = db.from("doctors")
                .select()
                .decodeList<Doctor>()

            // Join profiles for each doctor
            val profiles = try {
                db.from("profiles")
                    .select()
                    .decodeList<Profile>()
                    .associateBy { it.id }
            } catch (_: Exception) {
                emptyMap()
            }

            val joined = doctors.map { doc ->
                doc.copy(profile = profiles[doc.profileId])
            }
            Result.success(joined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoctorById(doctorId: String): Result<Doctor> = withContext(Dispatchers.IO) {
        try {
            var doctor = db.from("doctors")
                .select {
                    filter {
                        eq("id", doctorId)
                    }
                }.decodeSingleOrNull<Doctor>()

            if (doctor == null) {
                doctor = db.from("doctors")
                    .select {
                        filter {
                            eq("profile_id", doctorId)
                        }
                    }.decodeSingleOrNull<Doctor>()
            }

            if (doctor == null) {
                return@withContext Result.failure(Exception("Doctor not found"))
            }

            val profile = db.from("profiles")
                .select {
                    filter {
                        eq("id", doctor.profileId)
                    }
                }.decodeSingleOrNull<Profile>()

            Result.success(doctor.copy(profile = profile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoctorByProfileId(profileId: String): Result<Doctor> = withContext(Dispatchers.IO) {
        try {
            val doctor = db.from("doctors")
                .select {
                    filter {
                        eq("profile_id", profileId)
                    }
                }.decodeSingle<Doctor>()

            val profile = db.from("profiles")
                .select {
                    filter {
                        eq("id", profileId)
                    }
                }.decodeSingleOrNull<Profile>()

            Result.success(doctor.copy(profile = profile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDoctorAvailability(doctorId: String, available: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.from("doctors").update(
                mapOf("available" to available)
            ) {
                filter {
                    eq("id", doctorId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDoctorProfile(
        doctorId: String,
        profileId: String,
        name: String,
        specialization: String,
        qualification: String,
        experience: String,
        hospital: String,
        department: String,
        fee: Double,
        days: String,
        startTime: String,
        endTime: String,
        bio: String,
        available: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (name.isNotBlank()) {
                try {
                    db.from("profiles").update(
                        mapOf("name" to name)
                    ) {
                        filter { eq("id", profileId) }
                    }
                } catch (_: Exception) {}
            }

            try {
                db.from("doctors").update(
                    mapOf(
                        "specialization" to specialization,
                        "qualification" to qualification,
                        "experience" to experience,
                        "hospital" to hospital,
                        "department" to department,
                        "consultation_fee" to fee,
                        "available_days" to days,
                        "start_time" to startTime,
                        "end_time" to endTime,
                        "about" to bio,
                        "bio" to bio,
                        "available" to available
                    )
                ) {
                    filter { eq("id", doctorId) }
                }
            } catch (_: Exception) {
                db.from("doctors").update(
                    mapOf(
                        "specialization" to specialization,
                        "qualification" to qualification,
                        "experience_years" to experience,
                        "hospital_name" to hospital,
                        "department" to department,
                        "consultation_fee" to fee,
                        "available_days" to days,
                        "start_time" to startTime,
                        "end_time" to endTime,
                        "bio" to bio,
                        "available" to available
                    )
                ) {
                    filter { eq("id", doctorId) }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
