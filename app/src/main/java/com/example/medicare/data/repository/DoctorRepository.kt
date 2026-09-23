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
            // 1. Fetch available doctors from public.doctors
            val doctors: List<Doctor> = try {
                db.from("doctors")
                    .select {
                        filter {
                            eq("available", true)
                        }
                    }
                    .decodeList<Doctor>()
            } catch (e: Exception) {
                android.util.Log.e("DoctorRepository", "Error filtering available doctors: ${e.message}. Attempting broad fetch.", e)
                try {
                    db.from("doctors")
                        .select()
                        .decodeList<Doctor>()
                        .filter { it.available }
                } catch (e2: Exception) {
                    android.util.Log.e("DoctorRepository", "Failed to fetch doctors: ${e2.message}", e2)
                    throw e2
                }
            }

            // 2. Fetch doctor profiles
            val doctorProfiles: List<Profile> = try {
                db.from("profiles")
                    .select {
                        filter {
                            eq("role", "doctor")
                        }
                    }
                    .decodeList<Profile>()
            } catch (e: Exception) {
                android.util.Log.e("DoctorRepository", "Error fetching doctor profiles: ${e.message}", e)
                try {
                    db.from("profiles").select().decodeList<Profile>().filter { it.role == "doctor" }
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val profilesMap = doctorProfiles.associateBy { it.id }

            // 3. Self-healing check: Are there doctor profiles that have NO row in public.doctors?
            // (e.g., doctor registered earlier before doctor row was inserted)
            val existingDoctorProfileIds = doctors.map { it.profileId }.filter { it.isNotBlank() }.toSet()
            val existingDoctorUserIds = doctors.map { it.userId }.filter { it.isNotBlank() }.toSet()
            val missingDoctorProfiles = doctorProfiles.filter { p ->
                !existingDoctorProfileIds.contains(p.id) && !existingDoctorUserIds.contains(p.id)
            }

            val healedDoctors = mutableListOf<Doctor>()
            for (p in missingDoctorProfiles) {
                try {
                    val existing = db.from("doctors").select {
                        filter {
                            or {
                                eq("profile_id", p.id)
                                eq("user_id", p.id)
                            }
                        }
                    }.decodeList<Doctor>().firstOrNull()

                    if (existing == null) {
                        val inserted = db.from("doctors").insert(
                            mapOf(
                                "profile_id" to p.id,
                                "user_id" to p.id,
                                "specialization" to "General Medicine",
                                "qualification" to "MBBS, MD",
                                "experience" to "5+ Years",
                                "experience_years" to "5+ Years",
                                "hospital" to "Medicare Central Hospital",
                                "hospital_name" to "Medicare Central Hospital",
                                "department" to "General Medicine",
                                "consultation_fee" to 500.0,
                                "available_days" to "Mon,Tue,Wed,Thu,Fri",
                                "start_time" to "09:00 AM",
                                "end_time" to "05:00 PM",
                                "about" to "Experienced physician dedicated to patient care.",
                                "bio" to "Experienced physician dedicated to patient care.",
                                "available" to true
                            )
                        ) {
                            select()
                        }.decodeSingle<Doctor>()
                        healedDoctors.add(inserted.copy(profile = p))
                        android.util.Log.i("DoctorRepository", "Auto-healed missing doctor for profile: ${p.name} (${p.id})")
                    } else if (existing.available) {
                        healedDoctors.add(existing.copy(profile = p))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DoctorRepository", "Auto-heal doctor error for profile ${p.id}: ${e.message}", e)
                }
            }

            // 4. Combine and resolve profiles
            val allDoctors = (doctors + healedDoctors).distinctBy { it.id }
            val joined = allDoctors.map { doc ->
                val resolvedProfile = profilesMap[doc.profileId]
                    ?: profilesMap[doc.userId]
                    ?: try {
                        val targetId = doc.profileId.ifBlank { doc.userId }
                        if (targetId.isNotBlank()) {
                            db.from("profiles")
                                .select {
                                    filter { eq("id", targetId) }
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
                    id = doc.profileId,
                    name = doc.name?.takeIf { it.isNotBlank() } ?: "Dr. ${doc.specialization.ifBlank { "Specialist" }}",
                    role = "doctor"
                )

                doc.copy(profile = safeProfile)
            }

            Result.success(joined)
        } catch (e: Exception) {
            android.util.Log.e("DoctorRepository", "Failed to get doctors: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getDoctorById(doctorId: String): Result<Doctor> = withContext(Dispatchers.IO) {
        try {
            val doctor = try {
                db.from("doctors")
                    .select {
                        filter {
                            eq("id", doctorId)
                        }
                    }.decodeList<Doctor>().firstOrNull()
            } catch (_: Exception) { null }
                ?: try {
                    db.from("doctors")
                        .select {
                            filter {
                                eq("profile_id", doctorId)
                            }
                        }.decodeList<Doctor>().firstOrNull()
                } catch (_: Exception) { null }
                ?: try {
                    db.from("doctors")
                        .select {
                            filter {
                                eq("user_id", doctorId)
                            }
                        }.decodeList<Doctor>().firstOrNull()
                } catch (_: Exception) { null }

            if (doctor == null) {
                return@withContext Result.failure(Exception("Doctor not found for ID: $doctorId"))
            }

            val targetProfileId = doctor.profileId.ifBlank { doctor.userId }
            val profile = try {
                db.from("profiles")
                    .select {
                        filter {
                            eq("id", targetProfileId)
                        }
                    }.decodeList<Profile>().firstOrNull()
            } catch (_: Exception) { null }

            val safeProfile = profile ?: Profile(
                id = targetProfileId,
                name = doctor.name?.takeIf { it.isNotBlank() } ?: "Dr. ${doctor.specialization.ifBlank { "Specialist" }}",
                role = "doctor"
            )

            Result.success(doctor.copy(profile = safeProfile))
        } catch (e: Exception) {
            android.util.Log.e("DoctorRepository", "Failed to get doctor by id $doctorId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getDoctorByProfileId(profileId: String): Result<Doctor> = withContext(Dispatchers.IO) {
        try {
            var doctor = try {
                db.from("doctors")
                    .select {
                        filter {
                            eq("profile_id", profileId)
                        }
                    }.decodeList<Doctor>().firstOrNull()
            } catch (_: Exception) { null }
                ?: try {
                    db.from("doctors")
                        .select {
                            filter {
                                eq("user_id", profileId)
                            }
                        }.decodeList<Doctor>().firstOrNull()
                } catch (_: Exception) { null }

            // If doctor record is missing, auto-create it
            if (doctor == null) {
                try {
                    doctor = db.from("doctors").insert(
                        mapOf(
                            "profile_id" to profileId,
                            "user_id" to profileId,
                            "specialization" to "General Medicine",
                            "qualification" to "MBBS, MD",
                            "experience" to "5+ Years",
                            "experience_years" to "5+ Years",
                            "hospital" to "Medicare Central Hospital",
                            "hospital_name" to "Medicare Central Hospital",
                            "department" to "General Medicine",
                            "consultation_fee" to 500.0,
                            "available_days" to "Mon,Tue,Wed,Thu,Fri",
                            "start_time" to "09:00 AM",
                            "end_time" to "05:00 PM",
                            "about" to "Experienced physician dedicated to patient care.",
                            "bio" to "Experienced physician dedicated to patient care.",
                            "available" to true
                        )
                    ) {
                        select()
                    }.decodeSingle<Doctor>()
                } catch (e: Exception) {
                    android.util.Log.e("DoctorRepository", "Error creating doctor record for profile $profileId: ${e.message}", e)
                }
            }

            if (doctor == null) {
                return@withContext Result.failure(Exception("Doctor not found for profile: $profileId"))
            }

            val profile = try {
                db.from("profiles")
                    .select {
                        filter {
                            eq("id", profileId)
                        }
                    }.decodeList<Profile>().firstOrNull()
            } catch (_: Exception) { null }

            val safeProfile = profile ?: Profile(
                id = profileId,
                name = doctor.name?.takeIf { it.isNotBlank() } ?: "Dr. ${doctor.specialization.ifBlank { "Specialist" }}",
                role = "doctor"
            )

            Result.success(doctor.copy(profile = safeProfile))
        } catch (e: Exception) {
            android.util.Log.e("DoctorRepository", "Failed to get doctor for profile $profileId: ${e.message}", e)
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
