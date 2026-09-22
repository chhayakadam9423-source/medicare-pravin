package com.example.medicare.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("role") val role: String, // 'patient', 'doctor', 'admin'
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class Doctor(
    @SerialName("id") val id: String = "",
    @SerialName("profile_id") val profileId: String = "",
    @SerialName("specialization") val specialization: String,
    @SerialName("qualification") val qualification: String,
    @SerialName("experience_years") val experience: String,
    @SerialName("license_number") val licenseNumber: String? = null,
    @SerialName("hospital_name") val hospitalName: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("consultation_fee") val consultationFee: Double? = null,
    @SerialName("available_days") val availableDays: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("bio") val about: String? = null,
    @SerialName("profile_image_url") val imageUrl: String? = null,
    @SerialName("available") val available: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    // Expanded/joined profile info
    val profile: Profile? = null
) {
    val doctorName: String
        get() = profile?.name ?: "Doctor"
}

@Serializable
data class Patient(
    @SerialName("id") val id: String = "",
    @SerialName("profile_id") val profileId: String = "",
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("blood_group") val bloodGroup: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // Expanded/joined profile info
    val profile: Profile? = null
) {
    val patientName: String
        get() = profile?.name ?: "Patient"
}

@Serializable
data class Appointment(
    @SerialName("id") val id: String = "",
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("appointment_date") val appointmentDate: String,
    @SerialName("appointment_time") val appointmentTime: String,
    @SerialName("status") val status: String = "pending", // pending, confirmed, rejected, completed, cancelled
    @SerialName("reason") val reason: String,
    @SerialName("created_at") val createdAt: String? = null,
    // Joined entities
    val doctor: Doctor? = null,
    val patient: Patient? = null
)

@Serializable
data class Prescription(
    @SerialName("id") val id: String = "",
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("medicine") val medicine: String,
    @SerialName("dosage") val dosage: String,
    @SerialName("frequency") val frequency: String,
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
