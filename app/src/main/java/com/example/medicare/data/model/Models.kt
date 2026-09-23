package com.example.medicare.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String? = "User",
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("role") val role: String? = "patient", // 'patient', 'doctor', 'admin'
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "User"

    val displayRole: String
        get() = role?.takeIf { it.isNotBlank() } ?: "patient"
}

@Serializable
data class Doctor(
    @SerialName("id") val id: String = "",
    @SerialName("profile_id") val profileId: String = "",
    @SerialName("user_id") val userId: String? = null,
    @SerialName("name") val rawName: String? = null,
    @SerialName("specialization") val rawSpecialization: String? = "General Medicine",
    @SerialName("qualification") val rawQualification: String? = "MBBS, MD",
    @SerialName("experience") val rawExperience: String? = "5+ Years",
    @SerialName("experience_years") val experienceYearsRaw: String? = null,
    @SerialName("license_number") val licenseNumber: String? = null,
    @SerialName("hospital") val rawHospital: String? = "Medicare Hospital",
    @SerialName("hospital_name") val hospitalNameRaw: String? = null,
    @SerialName("department") val rawDepartment: String? = "General Department",
    @SerialName("consultation_fee") val consultationFee: Double? = 500.0,
    @SerialName("available_days") val rawAvailableDays: String? = "Mon, Tue, Wed, Thu, Fri",
    @SerialName("start_time") val startTime: String? = "09:00 AM",
    @SerialName("end_time") val endTime: String? = "05:00 PM",
    @SerialName("about") val rawAbout: String? = null,
    @SerialName("bio") val rawBio: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("available") val available: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    // Expanded/joined profile info
    val profile: Profile? = null
) {
    // Relationship:
    // doctors.profile_id = profiles.id
    // Doctor name comes from profiles.name.
    // Doctor phone comes from profiles.phone.
    // Specialization, qualification and experience come from doctors.
    val doctorName: String
        get() = profile?.name?.takeIf { it.isNotBlank() }
            ?: rawName?.takeIf { it.isNotBlank() }
            ?: "Dr. $specialization"

    val doctorPhone: String
        get() = profile?.phone?.takeIf { it.isNotBlank() } ?: ""

    val specialization: String
        get() = rawSpecialization?.takeIf { it.isNotBlank() } ?: "General Medicine"

    val qualification: String
        get() = rawQualification?.takeIf { it.isNotBlank() } ?: "MBBS, MD"

    val experience: String
        get() = rawExperience?.takeIf { it.isNotBlank() }
            ?: experienceYearsRaw?.takeIf { it.isNotBlank() }
            ?: "5+ Years"

    val experienceYears: String
        get() = experience

    val hospital: String
        get() = rawHospital?.takeIf { it.isNotBlank() }
            ?: hospitalNameRaw?.takeIf { it.isNotBlank() }
            ?: "Medicare Hospital"

    val hospitalName: String
        get() = hospital

    val department: String
        get() = rawDepartment?.takeIf { it.isNotBlank() } ?: specialization

    val availableDays: String
        get() = rawAvailableDays?.takeIf { it.isNotBlank() } ?: "Mon, Tue, Wed, Thu, Fri"

    val about: String
        get() = rawBio?.takeIf { it.isNotBlank() }
            ?: rawAbout?.takeIf { it.isNotBlank() }
            ?: "Experienced doctor dedicated to providing professional care."

    val bio: String
        get() = about

    val workingHours: String
        get() {
            val s = startTime?.takeIf { it.isNotBlank() } ?: "09:00 AM"
            val e = endTime?.takeIf { it.isNotBlank() } ?: "05:00 PM"
            return "$s - $e"
        }
}

@Serializable
data class Patient(
    @SerialName("id") val id: String = "",
    @SerialName("profile_id") val profileId: String = "",
    @SerialName("user_id") val userId: String? = null,
    @SerialName("dob") val dob: String? = null,
    @SerialName("date_of_birth") val dateOfBirthRaw: String? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("blood_group") val bloodGroup: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    @SerialName("medical_history") val medicalHistory: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // Expanded/joined profile info
    val profile: Profile? = null
) {
    val dateOfBirth: String?
        get() = dob ?: dateOfBirthRaw

    val patientName: String
        get() = profile?.name?.takeIf { it.isNotBlank() } ?: "Patient"

    val patientPhone: String
        get() = profile?.phone?.takeIf { it.isNotBlank() } ?: ""
}

@Serializable
data class Appointment(
    @SerialName("id") val id: String = "",
    @SerialName("patient_id") val patientId: String = "",
    @SerialName("doctor_id") val doctorId: String = "",
    @SerialName("appointment_date") val appointmentDate: String = "",
    @SerialName("appointment_time") val appointmentTime: String = "",
    @SerialName("status") val status: String = "pending", // pending, confirmed, rejected, completed, cancelled
    @SerialName("reason") val reason: String? = "",
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
