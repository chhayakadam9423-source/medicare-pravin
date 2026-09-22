package com.example.medicare.data

import android.content.Context
import com.example.medicare.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseManager {

    lateinit var client: SupabaseClient
        private set

    val auth: Auth
        get() = client.auth

    val db: Postgrest
        get() = client.postgrest

    val storage: Storage
        get() = client.storage

    val realtime: Realtime
        get() = client.realtime

    fun isInitialized(): Boolean = ::client.isInitialized

    fun initialize(context: Context) {

        // Read credentials from BuildConfig
        var targetUrl = BuildConfig.SUPABASE_URL.trim()
        val targetKey = BuildConfig.SUPABASE_ANON_KEY.trim()

        // Remove accidental Supabase API endpoint suffixes.
        // supabase-kt adds these endpoints automatically.
        targetUrl = targetUrl.replace("(rest/v1)|(auth/v1)|(storage/v1)|(realtime/v1)".toRegex(), "")
            .removeSuffix("/")

        require(targetUrl.isNotBlank()) {
            "SUPABASE_URL is empty"
        }

        require(targetKey.isNotBlank()) {
            "SUPABASE_ANON_KEY is empty"
        }

        require(!targetUrl.contains("/rest/v1")) {
            "SUPABASE_URL must be the base Supabase project URL"
        }

        client = createSupabaseClient(
            supabaseUrl = targetUrl,
            supabaseKey = targetKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}