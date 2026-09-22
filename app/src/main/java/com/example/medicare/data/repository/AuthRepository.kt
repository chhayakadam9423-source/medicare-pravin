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
     */
    fun normalizePhone(phone: String): String {
        return phone.filter { it.isDigit() }
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
            val syntheticEmail = phoneToAuthEmail(phone)

            // Register user with Supabase Auth using synthetic mobile-number email
            auth.signUpWith(Email) {
                this.email = syntheticEmail
                this.password = userPassword
                data = buildJsonObject {
                    put("name", name)
                    put("phone", phone)
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
                val fallback = Profile(id = uid, name = name, email = null, phone = phone, role = role)
                Result.success(fallback)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(phone: String, userPassword: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val syntheticEmail = phoneToAuthEmail(phone)

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
                val fallback = Profile(id = uid, name = name, email = null, phone = phone, role = role)
                Result.success(fallback)
            }
        } catch (e: Exception) {
            Result.failure(e)
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
