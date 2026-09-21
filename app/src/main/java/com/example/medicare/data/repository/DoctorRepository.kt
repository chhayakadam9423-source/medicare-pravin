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
            val profiles = db.from("profiles")
                .select {
                    filter {
                        eq("role", "doctor")
                    }
                }.decodeList<Profile>().associateBy { it.id }

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
            val doctor = db.from("doctors")
                .select {
                    filter {
                        eq("id", doctorId)
                    }
                }.decodeSingle<Doctor>()

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
}
