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
    // Default fallback credentials if not provided in BuildConfig
    // Users can override via BuildConfig or App Settings
    var supabaseUrl: String = "https://your-project-id.supabase.co"
    var supabaseKey: String = "your-public-anon-key"

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
        val prefs = context.getSharedPreferences("medicare_config", Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("supabase_url", null)
        val savedKey = prefs.getString("supabase_key", null)

        val targetUrl = (savedUrl ?: BuildConfig.SUPABASE_URL.ifEmpty { supabaseUrl }).trim()
        val targetKey = (savedKey ?: BuildConfig.SUPABASE_ANON_KEY.ifEmpty { supabaseKey }).trim()

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

    fun updateConfig(context: Context, newUrl: String, newKey: String) {
        val prefs = context.getSharedPreferences("medicare_config", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("supabase_url", newUrl)
            .putString("supabase_key", newKey)
            .apply()
        initialize(context)
    }
}
