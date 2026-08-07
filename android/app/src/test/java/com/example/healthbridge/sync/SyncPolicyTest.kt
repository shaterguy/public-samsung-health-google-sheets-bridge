package com.example.healthbridge.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPolicyTest {
    @Test
    fun defaultsMatchRequestedPolicy() {
        assertEquals(3, SyncPolicy.defaultIntervalHours)
        assertEquals(70, SyncPolicy.defaultBatteryPercentage)
    }

    @Test
    fun invalidOptionsFallBackToDefaults() {
        assertEquals(3, SyncPolicy.normalizeInterval(2))
        assertEquals(70, SyncPolicy.normalizeBattery(65))
    }

    @Test
    fun supportedOptionsArePreserved() {
        assertEquals(12, SyncPolicy.normalizeInterval(12))
        assertEquals(80, SyncPolicy.normalizeBattery(80))
    }
}
