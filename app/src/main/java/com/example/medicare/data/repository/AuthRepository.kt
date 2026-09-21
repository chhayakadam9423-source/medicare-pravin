package com.example.medicare.data.repository

import com.example.medicare.data.SupabaseManager
import com.example.medicare.data.model.Profile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {
    private val auth = SupabaseManager.auth
    private val db = SupabaseManager.db

    suspend fun signUp(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            // Sign up user with Supabase GoTrue
            val user = auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("name", name)
                    put("phone", phone)
                    put("role", role)
                }
            }

            val uid = user?.id ?: auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Registration failed: no user ID returned"))

            // Fetch profile created by database trigger
            val profile = db.from("profiles")
                .select {
                    filter {
                        eq("id", uid)
                    }
                }.decodeSingleOrNull<Profile>()

            if (profile != null) {
                Result.success(profile)
            } else {
                // In case trigger has a delay or local fallback
                val fallback = Profile(id = uid, name = name, email = email, phone = phone, role = role)
                Result.success(fallback)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val uid = auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Login failed: no user session"))

            val profile = db.from("profiles")
                .select {
                    filter {
                        eq("id", uid)
                    }
                }.decodeSingleOrNull<Profile>()

            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(Exception("User profile not found in database"))
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
