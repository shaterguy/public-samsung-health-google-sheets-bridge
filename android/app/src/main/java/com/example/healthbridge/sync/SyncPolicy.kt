package com.example.healthbridge.sync

object SyncPolicy {
    val intervalHours = listOf(1, 3, 6, 12, 24)
    val batteryPercentages = listOf(50, 60, 70, 80, 90)

    const val defaultIntervalHours = 3
    const val defaultBatteryPercentage = 70

    fun normalizeInterval(value: Int): Int =
        value.takeIf(intervalHours::contains) ?: defaultIntervalHours

    fun normalizeBattery(value: Int): Int =
        value.takeIf(batteryPercentages::contains) ?: defaultBatteryPercentage
}
