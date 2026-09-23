package com.example.medicare.data.repository

import com.example.medicare.data.SupabaseManager
import com.example.medicare.data.model.Patient
import com.example.medicare.data.model.Profile
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PatientRepository {
    private val db = SupabaseManager.db

    suspend fun getPatientByProfileId(profileId: String): Result<Patient> = withContext(Dispatchers.IO) {
        try {
            var patient: Patient? = null

            try {
                patient = db.from("patients").select {
                    filter { eq("user_id", profileId) }
                }.decodeSingleOrNull<Patient>()
            } catch (_: Exception) {}

            if (patient == null) {
                try {
                    patient = db.from("patients").select {
                        filter { eq("profile_id", profileId) }
                    }.decodeSingleOrNull<Patient>()
                } catch (_: Exception) {}
            }

            if (patient == null) {
                try {
                    patient = db.from("patients").select {
                        filter { eq("id", profileId) }
                    }.decodeSingleOrNull<Patient>()
                } catch (_: Exception) {}
            }

            if (patient == null) {
                // Auto-create patient record if missing
                try {
                    patient = db.from("patients").insert(
                        mapOf("user_id" to profileId, "profile_id" to profileId)
                    ) {
                        select()
                    }.decodeSingle<Patient>()
                } catch (_: Exception) {
                    try {
                        patient = db.from("patients").insert(
                            mapOf("user_id" to profileId)
                        ) {
                            select()
                        }.decodeSingle<Patient>()
                    } catch (_: Exception) {
                        patient = db.from("patients").insert(
                            mapOf("profile_id" to profileId)
                        ) {
                            select()
                        }.decodeSingle<Patient>()
                    }
                }
            }

            val currentPatient = patient ?: Patient(id = profileId, profileId = profileId, userId = profileId)
            val actualProfileId = currentPatient.profileId.ifBlank { profileId }
            val profile = try {
                db.from("profiles").select {
                    filter { eq("id", actualProfileId) }
                }.decodeSingleOrNull<Profile>()
            } catch (_: Exception) {
                null
            }

            Result.success(currentPatient.copy(profile = profile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePatientProfile(
        profileId: String,
        name: String,
        gender: String,
        dob: String,
        bloodGroup: String,
        address: String,
        emergencyContact: String
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

            // Find existing patient record
            var patient: Patient? = null
            try {
                patient = db.from("patients").select {
                    filter { eq("user_id", profileId) }
                }.decodeSingleOrNull<Patient>()
            } catch (_: Exception) {}

            if (patient == null) {
                try {
                    patient = db.from("patients").select {
                        filter { eq("profile_id", profileId) }
                    }.decodeSingleOrNull<Patient>()
                } catch (_: Exception) {}
            }

            val updates = mutableMapOf<String, Any>()
            if (gender.isNotBlank()) updates["gender"] = gender
            if (dob.isNotBlank()) {
                updates["dob"] = dob
                updates["date_of_birth"] = dob
            }
            if (bloodGroup.isNotBlank()) updates["blood_group"] = bloodGroup
            if (address.isNotBlank()) updates["address"] = address
            if (emergencyContact.isNotBlank()) updates["emergency_contact"] = emergencyContact

            if (patient != null) {
                try {
                    db.from("patients").update(updates) {
                        filter { eq("id", patient.id) }
                    }
                } catch (_: Exception) {
                    // Try without date_of_birth
                    val fallback = updates.filterKeys { it != "date_of_birth" }
                    db.from("patients").update(fallback) {
                        filter { eq("id", patient.id) }
                    }
                }
            } else {
                updates["user_id"] = profileId
                updates["profile_id"] = profileId
                try {
                    db.from("patients").insert(updates)
                } catch (_: Exception) {
                    val fallback = updates.filterKeys { it != "date_of_birth" && it != "profile_id" }
                    db.from("patients").insert(fallback)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
