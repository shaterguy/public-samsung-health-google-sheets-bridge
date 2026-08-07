package com.example.healthbridge.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.healthbridge.data.SecureSettings
import com.example.healthbridge.health.HealthSyncRepository
import com.example.healthbridge.sheets.GoogleAuthorizationRequiredException
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = HealthSyncRepository(applicationContext)
        val settings = repository.settings
        if (!settings.syncEnabled || !settings.configured()) return Result.success()

        if (!isWifiConnected(applicationContext)) {
            settings.lastSyncMessage = "Wi-Fi에 연결되지 않아 자동 동기화를 건너뛰었습니다."
            return Result.success()
        }

        val battery = currentBatteryPercent(applicationContext)
        if (battery != null && battery < settings.minimumBatteryPercent) {
            settings.lastSyncMessage =
                "배터리 $battery%: 설정한 최소 ${settings.minimumBatteryPercent}%보다 낮아 동기화를 건너뛰었습니다."
            return Result.success()
        }

        return try {
            repository.sync()
            Result.success()
        } catch (error: GoogleAuthorizationRequiredException) {
            settings.googleAuthorized = false
            settings.lastSyncMessage = error.message
            Result.failure()
        } catch (error: SecurityException) {
            settings.lastSyncMessage = "Health Connect 권한이 필요합니다: ${error.message}"
            Result.failure()
        } catch (error: Exception) {
            settings.lastSyncMessage = "동기화 실패: ${error.message}"
            Result.retry()
        }
    }

    companion object {
        private const val uniqueName = "health-connect-periodic-sync"

        fun schedule(context: Context) {
            val settings = SecureSettings(context)
            val hours = SyncPolicy.normalizeInterval(settings.syncIntervalHours)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(hours.toLong(), TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) =
            WorkManager.getInstance(context).cancelUniqueWork(uniqueName)

        fun isWifiConnected(context: Context): Boolean {
            val connectivity = context.getSystemService(ConnectivityManager::class.java)
            val network = connectivity.activeNetwork ?: return false
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }

        fun currentBatteryPercent(context: Context): Int? {
            val battery = context.getSystemService(BatteryManager::class.java)
            val value = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            return value.takeIf { it in 0..100 }
        }
    }
}
