package com.example.medicare.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.medicare.data.model.Profile

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("medicare_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_DOCTOR_ID = "doctor_id"
        private const val KEY_PATIENT_ID = "patient_id"
    }

    fun saveUserSession(profile: Profile) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, profile.id)
            putString(KEY_USER_NAME, profile.name)
            putString(KEY_USER_EMAIL, profile.email)
            putString(KEY_USER_ROLE, profile.role)
            putString(KEY_USER_PHONE, profile.phone ?: "")
            apply()
        }
    }

    fun saveDoctorId(doctorId: String) {
        prefs.edit().putString(KEY_DOCTOR_ID, doctorId).apply()
    }

    fun getDoctorId(): String? = prefs.getString(KEY_DOCTOR_ID, null)

    fun savePatientId(patientId: String) {
        prefs.edit().putString(KEY_PATIENT_ID, patientId).apply()
    }

    fun getPatientId(): String? = prefs.getString(KEY_PATIENT_ID, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "User") ?: "User"
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "patient") ?: "patient"
    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "+1-555-0201") ?: "+1-555-0201"
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
