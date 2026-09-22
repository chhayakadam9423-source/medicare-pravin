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
            // Live dynamic query equivalent to: SELECT * FROM public.doctors WHERE available = true
            val doctors: List<Doctor> = try {
                db.from("doctors")
                    .select {
                        filter {
                            eq("available", true)
                        }
                    }
                    .decodeList<Doctor>()
            } catch (e: Exception) {
                try {
                    db.from("doctors")
                        .select()
                        .decodeList<Doctor>()
                        .filter { it.available }
                } catch (e2: Exception) {
                    emptyList()
                }
            }

            // Resolve profiles for each doctor using doctors.profile_id -> profiles.id
            val profilesMap: Map<String, Profile> = try {
                db.from("profiles")
                    .select()
                    .decodeList<Profile>()
                    .associateBy { it.id }
            } catch (_: Exception) {
                emptyMap()
            }

            val joined = doctors.map { doc ->
                val resolvedProfile = profilesMap[doc.profileId] ?: try {
                    if (doc.profileId.isNotBlank()) {
                        db.from("profiles")
                            .select {
                                filter { eq("id", doc.profileId) }
                            }
                            .decodeList<Profile>()
                            .firstOrNull()
                    } else null
                } catch (_: Exception) {
                    null
                }

                // If a profile lookup fails for one doctor, do NOT remove the doctor from the list.
                // Show safe fallback values and continue displaying the doctor.
                val safeProfile = resolvedProfile ?: Profile(
                    _id = doc.profileId,
                    _name = "Dr. ${doc.specialization.ifBlank { "Specialist" }}",
                    _role = "doctor"
                )

                doc.copy(profile = safeProfile)
            }

            Result.success(joined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoctorById(doctorId: String): Result<Doctor> = withContext(Dispatchers.IO) {
        try {
            val doctors = try {
                db.from("doctors")
                    .select {
                        filter {
                            eq("id", doctorId)
                        }
                    }.decodeList<Doctor>()
            } catch (_: Exception) { emptyList() }

            val doctor = doctors.firstOrNull() ?: try {
                db.from("doctors")
                    .select {
                        filter {
                            eq("profile_id", doctorId)
                        }
                    }.decodeList<Doctor>().firstOrNull()
            } catch (_: Exception) { null }

            if (doctor == null) {
                return@withContext Result.failure(Exception("Doctor not found"))
            }

            val profile = try {
                db.from("profiles")
                    .select {
                        filter {
                            eq("id", doctor.profileId)
                        }
                    }.decodeList<Profile>().firstOrNull()
            } catch (_: Exception) { null }

            val safeProfile = profile ?: Profile(
                _id = doctor.profileId,
                _name = "Dr. ${doctor.specialization.ifBlank { "Specialist" }}",
                _role = "doctor"
            )

            Result.success(doctor.copy(profile = safeProfile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoctorByProfileId(profileId: String): Result<Doctor> = withContext(Dispatchers.IO) {
        try {
            val doctors = db.from("doctors")
                .select {
                    filter {
                        eq("profile_id", profileId)
                    }
                }.decodeList<Doctor>()

            val doctor = doctors.firstOrNull()
                ?: return@withContext Result.failure(Exception("Doctor not found for profile: $profileId"))

            val profile = try {
                db.from("profiles")
                    .select {
                        filter {
                            eq("id", profileId)
                        }
                    }.decodeList<Profile>().firstOrNull()
            } catch (_: Exception) { null }

            val safeProfile = profile ?: Profile(
                _id = profileId,
                _name = "Dr. ${doctor.specialization.ifBlank { "Specialist" }}",
                _role = "doctor"
            )

            Result.success(doctor.copy(profile = safeProfile))
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
