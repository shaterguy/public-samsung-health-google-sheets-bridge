package com.example.healthbridge.data

data class UploadRecord(
    val recordType: String,
    val sourceRecordId: String,
    val dataOrigin: String,
    val startTime: String,
    val endTime: String,
    val localDate: String,
    val clientModifiedAt: String,
    val payload: Map<String, Any?>,
)

data class SyncReport(
    val uploaded: Int,
    val startedAt: String,
    val completedAt: String,
    val fullSync: Boolean,
)
