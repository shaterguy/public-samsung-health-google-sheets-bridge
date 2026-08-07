package com.example.healthbridge

import android.app.Application
import com.example.healthbridge.data.SecureSettings
import com.example.healthbridge.sync.SyncWorker

class HealthBridgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (SecureSettings(this).syncEnabled) SyncWorker.schedule(this)
    }
}
