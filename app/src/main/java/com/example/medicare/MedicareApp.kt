package com.example.medicare

import android.app.Application
import com.example.medicare.data.SupabaseManager

class MedicareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Supabase client
        SupabaseManager.initialize(this)
    }
}
