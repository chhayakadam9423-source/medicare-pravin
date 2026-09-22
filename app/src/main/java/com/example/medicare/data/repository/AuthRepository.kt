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

            // Fetch profile created by database trigger
            val profile = try {
                db.from("profiles")
                    .select {
                        filter {
                            eq("id", uid)
                        }
                    }.decodeSingleOrNull<Profile>()
            } catch (_: Exception) {
                null
            }

            if (profile != null) {
                Result.success(profile)
            } else {
                // Fallback profile if trigger has a slight execution latency
                val fallback = Profile(id = uid, name = name, email = null, phone = cleanPhone, role = role)
                Result.success(fallback)
            }
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

            val profile = try {
                db.from("profiles")
                    .select {
                        filter {
                            eq("id", uid)
                        }
                    }.decodeSingleOrNull<Profile>()
            } catch (_: Exception) {
                null
            }

            if (profile != null) {
                Result.success(profile)
            } else {
                // Read from user metadata if profile query failed
                val metadata = auth.currentUserOrNull()?.userMetadata
                val name = metadata?.get("name")?.toString()?.trim('"') ?: "User"
                val role = metadata?.get("role")?.toString()?.trim('"') ?: "patient"
                val fallback = Profile(id = uid, name = name, email = null, phone = cleanPhone, role = role)
                Result.success(fallback)
            }
        } catch (e: Exception) {
            val detailedMsg = extractAndLogAuthError(e, "signIn")
            Result.failure(Exception(detailedMsg, e))
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
