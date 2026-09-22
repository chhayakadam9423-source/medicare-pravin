package com.example.medicare.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class Profile(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "User",
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("role") val role: String = "patient", // 'patient', 'doctor', 'admin'
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Doctor(
    @SerialName("id") val id: String = "",
    @SerialName("profile_id") val profileId: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String? = null,
    @SerialName("specialization") val specialization: String = "General Medicine",
    @SerialName("qualification") val qualification: String = "MBBS, MD",
    @SerialName("experience") val experience: String = "5+ Years",
    @SerialName("license_number") val licenseNumber: String? = null,
    @SerialName("hospital") val hospital: String = "Medicare Hospital",
    @SerialName("department") val department: String = "General Department",
    @SerialName("consultation_fee") val consultationFee: Double? = 500.0,
    @SerialName("available_days") val availableDays: String = "Mon, Tue, Wed, Thu, Fri",
    @SerialName("start_time") val startTime: String = "09:00 AM",
    @SerialName("end_time") val endTime: String = "05:00 PM",
    @SerialName("about") val about: String? = "Experienced doctor dedicated to providing professional care.",
    @SerialName("bio") val bio: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("available") val available: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    // Expanded/joined profile info
    val profile: Profile? = null
) {
    val hospitalName: String
        get() = hospital

    val experienceYears: String
        get() = experience

    val workingHours: String
        get() = "$startTime - $endTime"

    val doctorName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: profile?.name?.takeIf { it.isNotBlank() }
            ?: "Dr. $specialization"
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
