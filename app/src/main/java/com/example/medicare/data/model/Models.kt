package com.example.medicare.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class Profile(
    @SerialName("id") private val _id: String? = null,
    @SerialName("name") private val _name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("role") private val _role: String? = null, // 'patient', 'doctor', 'admin'
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val id: String
        get() = _id ?: ""

    val name: String
        get() = _name?.takeIf { it.isNotBlank() } ?: "User"

    val role: String
        get() = _role?.takeIf { it.isNotBlank() } ?: "patient"
}

@Serializable
data class Doctor(
    @SerialName("id") private val _id: String? = null,
    @SerialName("profile_id") private val _profileId: String? = null,
    @SerialName("user_id") private val _userId: String? = null,
    @SerialName("specialization") private val _specialization: String? = null,
    @SerialName("qualification") private val _qualification: String? = null,
    @SerialName("experience") private val _experience: String? = null,
    @SerialName("experience_years") private val _experienceYears: String? = null,
    @SerialName("license_number") val licenseNumber: String? = null,
    @SerialName("hospital") private val _hospital: String? = null,
    @SerialName("hospital_name") private val _hospitalName: String? = null,
    @SerialName("department") private val _department: String? = null,
    @SerialName("consultation_fee") private val _consultationFee: JsonElement? = null,
    @SerialName("available_days") private val _availableDays: String? = null,
    @SerialName("start_time") private val _startTime: String? = null,
    @SerialName("end_time") private val _endTime: String? = null,
    @SerialName("about") private val _about: String? = null,
    @SerialName("bio") private val _bio: String? = null,
    @SerialName("image_url") private val _imageUrl: String? = null,
    @SerialName("profile_image_url") private val _profileImageUrl: String? = null,
    @SerialName("available") private val _available: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // Expanded/joined profile info
    val profile: Profile? = null
) {
    val id: String
        get() = _id ?: ""

    val profileId: String
        get() = _profileId?.takeIf { it.isNotBlank() } ?: _userId ?: ""

    val userId: String
        get() = _userId?.takeIf { it.isNotBlank() } ?: _profileId ?: ""

    val specialization: String
        get() = _specialization?.takeIf { it.isNotBlank() } ?: "General Medicine"

    val qualification: String
        get() = _qualification?.takeIf { it.isNotBlank() } ?: "MBBS, MD"

    val experience: String
        get() = _experience?.takeIf { it.isNotBlank() } ?: _experienceYears?.takeIf { it.isNotBlank() } ?: "5+ Years"

    val hospital: String
        get() = _hospital?.takeIf { it.isNotBlank() } ?: _hospitalName?.takeIf { it.isNotBlank() } ?: "Medicare Hospital"

    val hospitalName: String?
        get() = hospital

    val department: String
        get() = _department?.takeIf { it.isNotBlank() } ?: "General Department"

    val consultationFee: Double?
        get() = try {
            when (_consultationFee) {
                is JsonPrimitive -> _consultationFee.contentOrNull?.toDoubleOrNull()
                else -> 500.0
            }
        } catch (_: Exception) {
            500.0
        }

    val availableDays: String
        get() = _availableDays?.takeIf { it.isNotBlank() } ?: "Mon, Tue, Wed, Thu, Fri"

    val startTime: String
        get() = _startTime?.takeIf { it.isNotBlank() } ?: "09:00 AM"

    val endTime: String
        get() = _endTime?.takeIf { it.isNotBlank() } ?: "05:00 PM"

    val workingHours: String
        get() = "$startTime - $endTime"

    val about: String?
        get() = _about?.takeIf { it.isNotBlank() } ?: _bio?.takeIf { it.isNotBlank() } ?: "Experienced doctor dedicated to providing professional care."

    val bio: String?
        get() = about

    val imageUrl: String?
        get() = _imageUrl ?: _profileImageUrl

    val available: Boolean
        get() = _available ?: true

    val doctorName: String
        get() = profile?.name?.takeIf { it.isNotBlank() } ?: "Dr. $specialization"
}

@Serializable
data class Patient(
    @SerialName("id") val id: String = "",
    @SerialName("profile_id") private val _profileId: String? = null,
    @SerialName("user_id") private val _userId: String? = null,
    @SerialName("dob") private val _dob: String? = null,
    @SerialName("date_of_birth") private val _dateOfBirth: String? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("blood_group") val bloodGroup: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    @SerialName("medical_history") val medicalHistory: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // Expanded/joined profile info
    val profile: Profile? = null
) {
    val profileId: String
        get() = _profileId ?: _userId ?: ""

    val userId: String
        get() = _userId ?: _profileId ?: ""

    val dateOfBirth: String?
        get() = _dob ?: _dateOfBirth

    val dob: String?
        get() = _dob ?: _dateOfBirth

    val patientName: String
        get() = profile?.name ?: "Patient"
}

@Serializable
data class Appointment(
    @SerialName("id") val id: String = "",
    @SerialName("patient_id") val patientId: String = "",
    @SerialName("doctor_id") val doctorId: String = "",
    @SerialName("appointment_date") val appointmentDate: String = "",
    @SerialName("appointment_time") val appointmentTime: String = "",
    @SerialName("status") val status: String = "pending", // pending, confirmed, rejected, completed, cancelled
    @SerialName("reason") val reason: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    // Joined entities
    val doctor: Doctor? = null,
    val patient: Patient? = null
)

@Serializable
data class Prescription(
    @SerialName("id") val id: String = "",
    @SerialName("appointment_id") val appointmentId: String = "",
    @SerialName("patient_id") val patientId: String = "",
    @SerialName("doctor_id") val doctorId: String = "",
    @SerialName("medicine") val medicine: String = "",
    @SerialName("dosage") val dosage: String = "",
    @SerialName("frequency") val frequency: String = "",
    @SerialName("instructions") val instructions: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val doctor: Doctor? = null
)

data class DashboardStats(
    val totalDoctors: Int = 0,
    val totalPatients: Int = 0,
    val totalAppointments: Int = 0,
    val pendingAppointments: Int = 0
)
