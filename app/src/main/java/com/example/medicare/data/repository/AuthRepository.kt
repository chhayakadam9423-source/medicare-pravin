package com.example.medicare.data.repository

import com.example.medicare.data.SupabaseManager
import com.example.medicare.data.model.Profile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {
    private val auth = SupabaseManager.auth
    private val db = SupabaseManager.db

    /**
     * Normalizes phone number to digits only for consistent mapping.
     * Handles +91, +1, leading 0, spaces, hyphens, and whitespace consistently
     * so registration and login always generate the exact same internal identity.
     */
    fun normalizePhone(raw: String): String {
        if (raw.isBlank()) return ""
        var s = raw.trim().lowercase()
        
        // Remove international '+' prefixes explicitly
        if (s.startsWith("+91")) {
            s = s.substring(3)
        } else if (s.startsWith("+1")) {
            s = s.substring(2)
        } else if (s.startsWith("+")) {
            s = s.substring(1)
        }
        
        // Extract digits only (stripping hyphens, spaces, parens, etc.)
        var digits = s.filter { it.isDigit() }
        
        // Handle cases where '+' was omitted:
        // Indian country code (91) with 10-digit phone = 12 digits
        if (digits.length == 12 && digits.startsWith("91")) {
            digits = digits.substring(2)
        }
        // Leading zero (0) e.g. 09876543210
        if (digits.length > 10 && digits.startsWith("0")) {
            digits = digits.substring(1)
        }
        // US country code (1) with 10-digit phone = 11 digits
        if (digits.length == 11 && digits.startsWith("1")) {
            digits = digits.substring(1)
        }
        
        return digits
    }

    /**
     * Maps user's mobile number securely to Supabase Auth's email provider.
     * Keeps user credentials strictly mobile number + password in UI,
     * while utilizing Supabase's native bcrypt-hashed password system without OTP or SMS gateway.
     */
    fun phoneToAuthEmail(phone: String): String {
        val clean = normalizePhone(phone)
        return "$clean@medicare.local"
    }

    suspend fun signUp(
        name: String,
        phone: String,
        userPassword: String,
        role: String,
        extraData: Map<String, String> = emptyMap()
    ): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            // Ensure any previous session is cleared
            try { auth.signOut() } catch (_: Exception) {}

            val cleanPhone = normalizePhone(phone)
            val syntheticEmail = phoneToAuthEmail(cleanPhone)

            // Register user with Supabase Auth using synthetic mobile-number email
            auth.signUpWith(Email) {
                this.email = syntheticEmail
                this.password = userPassword
                data = buildJsonObject {
                    put("name", name)
                    put("phone", cleanPhone)
                    put("role", role)
                    extraData.forEach { (k, v) -> put(k, v) }
                }
            }

            // If session was not automatically established, perform instant sign-in
            if (auth.currentUserOrNull() == null) {
                try {
                    auth.signInWith(Email) {
                        this.email = syntheticEmail
                        this.password = userPassword
                    }
                } catch (_: Exception) {}
            }

            val uid = auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Registration completed, please sign in with your mobile number and password."))

            // 1. Ensure public.profiles record exists
            var profile = try {
                db.from("profiles")
                    .select {
                        filter {
                            eq("id", uid)
                        }
                    }.decodeSingleOrNull<Profile>()
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error fetching profile during signUp: ${e.message}", e)
                null
            }

            if (profile == null) {
                val newProfile = Profile(
                    id = uid,
                    name = name,
                    email = syntheticEmail,
                    phone = cleanPhone,
                    role = role
                )
                try {
                    db.from("profiles").insert(newProfile)
                    profile = newProfile
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepository", "Error inserting profile during signUp: ${e.message}", e)
                    profile = newProfile
                }
            }

            // 2. Ensure public.doctors or public.patients record exists
            if (role.equals("doctor", ignoreCase = true)) {
                ensureDoctorRecordExists(uid, name, extraData)
            } else if (role.equals("patient", ignoreCase = true)) {
                ensurePatientRecordExists(uid, extraData)
            }

            Result.success(profile ?: Profile(id = uid, name = name, email = syntheticEmail, phone = cleanPhone, role = role))
        } catch (e: Exception) {
            val detailedMsg = extractAndLogAuthError(e, "signUp")
            Result.failure(Exception(detailedMsg, e))
        }
    }

    suspend fun signIn(phone: String, userPassword: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            // Ensure any previous session is cleared before new sign-in
            try { auth.signOut() } catch (_: Exception) {}

            val cleanPhone = normalizePhone(phone)
            val syntheticEmail = phoneToAuthEmail(cleanPhone)

            auth.signInWith(Email) {
                this.email = syntheticEmail
                this.password = userPassword
            }

            val uid = auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Login failed: no user session"))

            var profile = try {
                db.from("profiles")
                    .select {
                        filter {
                            eq("id", uid)
                        }
                    }.decodeSingleOrNull<Profile>()
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error fetching profile during signIn: ${e.message}", e)
                null
            }

            if (profile == null) {
                // Read from user metadata if profile query failed
                val metadata = auth.currentUserOrNull()?.userMetadata
                val name = metadata?.get("name")?.toString()?.trim('"') ?: "User"
                val role = metadata?.get("role")?.toString()?.trim('"') ?: "patient"
                val fallback = Profile(id = uid, name = name, email = syntheticEmail, phone = cleanPhone, role = role)
                try {
                    db.from("profiles").insert(fallback)
                } catch (_: Exception) {}
                profile = fallback
            }

            val effectiveRole = profile.role?.lowercase() ?: "patient"
            if (effectiveRole == "doctor") {
                ensureDoctorRecordExists(uid, profile.name ?: "User", emptyMap())
            } else if (effectiveRole == "patient") {
                ensurePatientRecordExists(uid, emptyMap())
            }

            Result.success(profile)
        } catch (e: Exception) {
            val detailedMsg = extractAndLogAuthError(e, "signIn")
            Result.failure(Exception(detailedMsg, e))
        }
    }

    /**
     * Guarantees that a row in public.doctors exists for the given doctor profile_id.
     * Prevents duplicate doctor records: updates if already exists, inserts if missing.
     */
    suspend fun ensureDoctorRecordExists(
        profileId: String,
        doctorName: String,
        extraData: Map<String, String>
    ): Unit = withContext(Dispatchers.IO) {
        try {
            // Check if record already exists in doctors
            val existing = try {
                db.from("doctors").select {
                    filter {
                        or {
                            eq("profile_id", profileId)
                            eq("user_id", profileId)
                        }
                    }
                }.decodeList<com.example.medicare.data.model.Doctor>().firstOrNull()
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Checking existing doctor error: ${e.message}", e)
                null
            }

            val spec = extraData["specialization"]?.ifBlank { "General Medicine" } ?: "General Medicine"
            val qual = extraData["qualification"]?.ifBlank { "MBBS, MD" } ?: "MBBS, MD"
            val exp = extraData["experience"]?.ifBlank { "5+ Years" } ?: "5+ Years"
            val hosp = extraData["hospital_name"]?.ifBlank { "Medicare Central Hospital" } ?: "Medicare Central Hospital"
            val dept = extraData["department"]?.ifBlank { spec } ?: spec
            val fee = extraData["consultation_fee"]?.toDoubleOrNull() ?: 500.0
            val days = extraData["available_days"]?.ifBlank { "Mon,Tue,Wed,Thu,Fri" } ?: "Mon,Tue,Wed,Thu,Fri"
            val startTime = extraData["start_time"]?.ifBlank { "09:00 AM" } ?: "09:00 AM"
            val endTime = extraData["end_time"]?.ifBlank { "05:00 PM" } ?: "05:00 PM"
            val about = extraData["about"]?.ifBlank { "Experienced clinician dedicated to excellence in patient treatment." }
                ?: "Experienced clinician dedicated to excellence in patient treatment."
            val img = extraData["profile_image_url"].orEmpty()
            val lic = extraData["license_number"].orEmpty()

            if (existing != null) {
                // Doctor already exists. Update only if new data was explicitly provided.
                if (extraData.isNotEmpty()) {
                    try {
                        db.from("doctors").update(
                            mapOf(
                                "name" to doctorName,
                                "specialization" to spec,
                                "qualification" to qual,
                                "experience" to exp,
                                "experience_years" to exp,
                                "hospital" to hosp,
                                "hospital_name" to hosp,
                                "department" to dept,
                                "consultation_fee" to fee,
                                "available_days" to days,
                                "start_time" to startTime,
                                "end_time" to endTime,
                                "about" to about,
                                "bio" to about,
                                "available" to true
                            )
                        ) {
                            filter { eq("id", existing.id) }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthRepository", "Error updating doctor record: ${e.message}", e)
                    }
                }
            } else {
                // Doctor does not exist: insert new record
                val docMap = mutableMapOf<String, Any>(
                    "profile_id" to profileId,
                    "user_id" to profileId,
                    "name" to doctorName,
                    "specialization" to spec,
                    "qualification" to qual,
                    "experience" to exp,
                    "experience_years" to exp,
                    "hospital" to hosp,
                    "hospital_name" to hosp,
                    "department" to dept,
                    "consultation_fee" to fee,
                    "available_days" to days,
                    "start_time" to startTime,
                    "end_time" to endTime,
                    "about" to about,
                    "bio" to about,
                    "image_url" to img,
                    "profile_image_url" to img,
                    "available" to true
                )
                if (lic.isNotBlank()) {
                    docMap["license_number"] = lic
                }

                try {
                    db.from("doctors").insert(docMap)
                    android.util.Log.i("AuthRepository", "Successfully created doctor record for profile: $profileId")
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepository", "Error inserting doctor record with full fields: ${e.message}. Trying standard schema fallback.", e)
                    try {
                        val fallbackMap = mapOf(
                            "profile_id" to profileId,
                            "specialization" to spec,
                            "qualification" to qual,
                            "experience_years" to exp,
                            "hospital_name" to hosp,
                            "department" to dept,
                            "consultation_fee" to fee,
                            "available_days" to days,
                            "start_time" to startTime,
                            "end_time" to endTime,
                            "bio" to about,
                            "available" to true
                        )
                        db.from("doctors").insert(fallbackMap)
                        android.util.Log.i("AuthRepository", "Successfully created doctor record with fallback schema for profile: $profileId")
                    } catch (e2: Exception) {
                        android.util.Log.e("AuthRepository", "Fallback doctor insert also failed: ${e2.message}. Trying bare minimum.", e2)
                        try {
                            val bareMinimum = mapOf(
                                "profile_id" to profileId,
                                "specialization" to spec,
                                "qualification" to qual,
                                "experience_years" to exp,
                                "available" to true
                            )
                            db.from("doctors").insert(bareMinimum)
                            android.util.Log.i("AuthRepository", "Successfully created doctor record with bare minimum schema for profile: $profileId")
                        } catch (e3: Exception) {
                            android.util.Log.e("AuthRepository", "Bare minimum doctor insert failed: ${e3.message}", e3)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "ensureDoctorRecordExists failed: ${e.message}", e)
        }
    }

    /**
     * Guarantees that a row in public.patients exists for the given patient profile_id.
     */
    suspend fun ensurePatientRecordExists(
        profileId: String,
        extraData: Map<String, String>
    ): Unit = withContext(Dispatchers.IO) {
        try {
            val existing = try {
                db.from("patients").select {
                    filter {
                        or {
                            eq("profile_id", profileId)
                            eq("user_id", profileId)
                        }
                    }
                }.decodeList<com.example.medicare.data.model.Patient>().firstOrNull()
            } catch (_: Exception) { null }

            if (existing == null) {
                val dob = extraData["date_of_birth"]?.ifBlank { "1995-01-01" } ?: "1995-01-01"
                val gender = extraData["gender"]?.ifBlank { "Other" } ?: "Other"
                val blood = extraData["blood_group"]?.ifBlank { "O+" } ?: "O+"
                val addr = extraData["address"].orEmpty()
                val emergency = extraData["emergency_contact"].orEmpty()

                try {
                    db.from("patients").insert(
                        mapOf(
                            "profile_id" to profileId,
                            "user_id" to profileId,
                            "date_of_birth" to dob,
                            "dob" to dob,
                            "gender" to gender,
                            "blood_group" to blood,
                            "address" to addr,
                            "emergency_contact" to emergency
                        )
                    )
                    android.util.Log.i("AuthRepository", "Successfully created patient record for profile: $profileId")
                } catch (e: Exception) {
                    try {
                        db.from("patients").insert(
                            mapOf(
                                "profile_id" to profileId,
                                "date_of_birth" to dob,
                                "gender" to gender,
                                "blood_group" to blood,
                                "address" to addr,
                                "emergency_contact" to emergency
                            )
                        )
                    } catch (e2: Exception) {
                        android.util.Log.e("AuthRepository", "Failed to create patient record: ${e2.message}", e2)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "ensurePatientRecordExists failed: ${e.message}", e)
        }
    }

    /**
     * Extracts and logs detailed diagnostics for Supabase errors,
     * including HTTP status codes, error bodies, exception messages, and underlying causes.
     * Strictly avoids logging passwords, credentials, or Supabase secret keys.
     */
    private fun extractAndLogAuthError(e: Exception, operation: String): String {
        val className = e.javaClass.simpleName
        val rawMessage = e.message ?: "Unknown error"
        val causeMessage = e.cause?.message

        var statusCode: Any? = null
        var responseBody: Any? = null

        // Safely extract status code via reflection across various Supabase/Ktor exception types
        try {
            val statusGetter = e.javaClass.getMethod("getStatusCode")
            statusCode = statusGetter.invoke(e)
        } catch (_: Exception) {
            try {
                val field = e.javaClass.getDeclaredField("statusCode")
                field.isAccessible = true
                statusCode = field.get(e)
            } catch (_: Exception) {}
        }

        // Safely extract response body/description
        try {
            val errorGetter = e.javaClass.getMethod("getError")
            responseBody = errorGetter.invoke(e)
        } catch (_: Exception) {
            try {
                val field = e.javaClass.getDeclaredField("error")
                field.isAccessible = true
                responseBody = field.get(e)
            } catch (_: Exception) {
                try {
                    val descGetter = e.javaClass.getMethod("getDescription")
                    responseBody = descGetter.invoke(e)
                } catch (_: Exception) {}
            }
        }

        android.util.Log.e("AuthRepository", "==================================================")
        android.util.Log.e("AuthRepository", "Supabase Auth Error during [$operation]")
        android.util.Log.e("AuthRepository", "Exception: $className")
        android.util.Log.e("AuthRepository", "Message: $rawMessage")
        if (statusCode != null) {
            android.util.Log.e("AuthRepository", "Status Code: $statusCode")
        }
        if (responseBody != null) {
            android.util.Log.e("AuthRepository", "Response Body/Detail: $responseBody")
        }
        if (causeMessage != null) {
            android.util.Log.e("AuthRepository", "Underlying Cause: $causeMessage")
        }
        android.util.Log.e("AuthRepository", "==================================================")

        return buildString {
            if (rawMessage.contains("Database error saving new user", ignoreCase = true)) {
                append("Database error saving new user: The Supabase trigger or table schema encountered an error.")
                if (responseBody != null && responseBody.toString() != rawMessage) {
                    append(" Details: ").append(responseBody)
                }
                if (causeMessage != null && causeMessage != rawMessage) {
                    append(" Cause: ").append(causeMessage)
                }
            } else {
                append(rawMessage)
                if (responseBody != null && responseBody.toString() != rawMessage) {
                    append(" - Details: ").append(responseBody)
                } else if (causeMessage != null && causeMessage != rawMessage) {
                    append(" (").append(causeMessage).append(")")
                }
            }
        }
    }

    suspend fun getCurrentProfile(): Profile? = withContext(Dispatchers.IO) {
        val uid = auth.currentUserOrNull()?.id ?: return@withContext null
        try {
            db.from("profiles")
                .select {
                    filter {
                        eq("id", uid)
                    }
                }.decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            null
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentSessionOrNull() != null
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Log or ignore
        }
    }
}
