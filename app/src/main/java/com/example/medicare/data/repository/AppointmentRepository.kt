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

    /**
     * Resolves the actual patients.id from either a patient record id or a user profile_id
     */
    private suspend fun resolvePatientTableId(patientOrProfileId: String): String {
        // First check if it matches a patient by id
        try {
            val direct = db.from("patients").select {
                filter { eq("id", patientOrProfileId) }
            }.decodeSingleOrNull<Patient>()
            if (direct != null) return direct.id
        } catch (_: Exception) {}

        // Check by user_id
        try {
            val byUser = db.from("patients").select {
                filter { eq("user_id", patientOrProfileId) }
            }.decodeSingleOrNull<Patient>()
            if (byUser != null) return byUser.id
        } catch (_: Exception) {}

        // Check by profile_id
        try {
            val byProfile = db.from("patients").select {
                filter { eq("profile_id", patientOrProfileId) }
            }.decodeSingleOrNull<Patient>()
            if (byProfile != null) return byProfile.id
        } catch (_: Exception) {}

        // If not found yet, ensure a patient record exists for this profile_id
        try {
            val created = db.from("patients").insert(
                mapOf("user_id" to patientOrProfileId, "profile_id" to patientOrProfileId)
            ) {
                select()
            }.decodeSingle<Patient>()
            return created.id
        } catch (_: Exception) {
            try {
                val created = db.from("patients").insert(
                    mapOf("user_id" to patientOrProfileId)
                ) {
                    select()
                }.decodeSingle<Patient>()
                return created.id
            } catch (_: Exception) {
                try {
                    val created = db.from("patients").insert(
                        mapOf("profile_id" to patientOrProfileId)
                    ) {
                        select()
                    }.decodeSingle<Patient>()
                    return created.id
                } catch (_: Exception) {}
            }
        }

        return patientOrProfileId
    }

    /**
     * Resolves the actual doctors.id from either a doctor record id or a user profile_id
     */
    private suspend fun resolveDoctorTableId(doctorOrProfileId: String): String {
        // First check if it matches a doctor by id
        try {
            val direct = db.from("doctors").select {
                filter { eq("id", doctorOrProfileId) }
            }.decodeSingleOrNull<Doctor>()
            if (direct != null) return direct.id
        } catch (_: Exception) {}

        // Next check by profile_id
        try {
            val byProfile = db.from("doctors").select {
                filter { eq("profile_id", doctorOrProfileId) }
            }.decodeSingleOrNull<Doctor>()
            if (byProfile != null) return byProfile.id
        } catch (_: Exception) {}

        return doctorOrProfileId
    }

    suspend fun createAppointment(
        patientId: String,
        doctorId: String,
        date: String,
        time: String,
        reason: String
    ): Result<Appointment> = withContext(Dispatchers.IO) {
        try {
            val actualPatientId = resolvePatientTableId(patientId)
            val actualDoctorId = resolveDoctorTableId(doctorId)

            val inserted = db.from("appointments").insert(
                mapOf(
                    "patient_id" to actualPatientId,
                    "doctor_id" to actualDoctorId,
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
            val actualPatientId = resolvePatientTableId(patientId)

            val list = try {
                db.from("appointments")
                    .select {
                        filter {
                            or {
                                eq("patient_id", actualPatientId)
                                eq("patient_id", patientId)
                            }
                        }
                    }.decodeList<Appointment>()
            } catch (_: Exception) {
                db.from("appointments")
                    .select {
                        filter {
                            eq("patient_id", actualPatientId)
                        }
                    }.decodeList<Appointment>()
            }

            // Enrich with doctor and doctor's profile data
            val doctorsMap = try {
                db.from("doctors").select().decodeList<Doctor>().associateBy { it.id }
            } catch (_: Exception) {
                emptyMap()
            }
            val profilesMap = try {
                db.from("profiles").select().decodeList<Profile>().associateBy { it.id }
            } catch (_: Exception) {
                emptyMap()
            }

            val enriched = list.map { apt ->
                val doctor = (doctorsMap[apt.doctorId] ?: doctorsMap.values.find { it.profileId == apt.doctorId })?.let { d ->
                    d.copy(profile = profilesMap[d.profileId])
                }
                apt.copy(doctor = doctor)
            }
            Result.success(enriched.sortedByDescending { it.appointmentDate })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDoctorAppointments(doctorId: String): Result<List<Appointment>> = withContext(Dispatchers.IO) {
        try {
            val actualDoctorId = resolveDoctorTableId(doctorId)

            val list = try {
                db.from("appointments")
                    .select {
                        filter {
                            or {
                                eq("doctor_id", actualDoctorId)
                                eq("doctor_id", doctorId)
                            }
                        }
                    }.decodeList<Appointment>()
            } catch (_: Exception) {
                db.from("appointments")
                    .select {
                        filter {
                            eq("doctor_id", actualDoctorId)
                        }
                    }.decodeList<Appointment>()
            }

            val patientsMap = try {
                db.from("patients").select().decodeList<Patient>().associateBy { it.id }
            } catch (_: Exception) {
                emptyMap()
            }
            val profilesMap = try {
                db.from("profiles").select().decodeList<Profile>().associateBy { it.id }
            } catch (_: Exception) {
                emptyMap()
            }

            val enriched = list.map { apt ->
                val patient = (patientsMap[apt.patientId] ?: patientsMap.values.find { it.profileId == apt.patientId })?.let { p ->
                    p.copy(profile = profilesMap[p.profileId])
                }
                apt.copy(patient = patient)
            }
            Result.success(enriched.sortedByDescending { it.appointmentDate })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllAppointments(): Result<List<Appointment>> = withContext(Dispatchers.IO) {
        try {
            val list = db.from("appointments").select().decodeList<Appointment>()

            val doctorsMap = db.from("doctors").select().decodeList<Doctor>().associateBy { it.id }
            val patientsMap = db.from("patients").select().decodeList<Patient>().associateBy { it.id }
            val profilesMap = db.from("profiles").select().decodeList<Profile>().associateBy { it.id }

            val enriched = list.map { apt ->
                val doctor = doctorsMap[apt.doctorId]?.let { d ->
                    d.copy(profile = profilesMap[d.profileId])
                }
                val patient = patientsMap[apt.patientId]?.let { p ->
                    p.copy(profile = profilesMap[p.profileId])
                }
                apt.copy(doctor = doctor, patient = patient)
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
