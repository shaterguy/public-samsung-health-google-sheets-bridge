package com.example.healthbridge

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthbridge.health.HealthPermissions
import com.example.healthbridge.health.HealthSyncRepository
import com.example.healthbridge.sync.SyncPolicy
import com.example.healthbridge.sync.SyncWorker
import com.google.android.gms.auth.api.identity.AuthorizationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val providerStatus: Int = HealthConnectClient.SDK_UNAVAILABLE,
    val granted: Set<String> = emptySet(),
    val googleAuthorized: Boolean = false,
    val googleAccountEmail: String? = null,
    val spreadsheetId: String = "",
    val syncEnabled: Boolean = false,
    val syncIntervalHours: Int = SyncPolicy.defaultIntervalHours,
    val minimumBatteryPercent: Int = SyncPolicy.defaultBatteryPercentage,
    val initialSyncComplete: Boolean = false,
    val lastSyncMessage: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
) {
    val dataPermissionsGranted: Boolean
        get() = granted.containsAll(HealthPermissions.dataRead)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HealthSyncRepository(application)
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val granted = if (repository.providerStatus == HealthConnectClient.SDK_AVAILABLE) {
                repository.grantedPermissions()
            } else {
                emptySet()
            }
            _state.value = _state.value.copy(
                providerStatus = repository.providerStatus,
                granted = granted,
                googleAuthorized = repository.settings.googleAuthorized,
                googleAccountEmail = repository.settings.googleAccountEmail,
                spreadsheetId = repository.settings.spreadsheetId,
                syncEnabled = repository.settings.syncEnabled,
                syncIntervalHours = repository.settings.syncIntervalHours,
                minimumBatteryPercent = repository.settings.minimumBatteryPercent,
                initialSyncComplete = repository.settings.initialSyncComplete,
                lastSyncMessage = repository.settings.lastSyncMessage,
            )
        }
    }

    fun requestedPermissions(): Set<String> = repository.requestedPermissions()

    fun saveSpreadsheetId(value: String) {
        val normalized = value.trim()
        if (!normalized.matches(Regex("[A-Za-z0-9_-]{20,}"))) {
            _state.value = _state.value.copy(
                message = "Google 스프레드시트 ID 형식을 확인해 주세요.",
            )
            return
        }

        val changed = normalized != repository.settings.spreadsheetId
        repository.settings.spreadsheetId = normalized
        if (changed) {
            repository.settings.clearSyncCursor()
        }
        _state.value = _state.value.copy(
            spreadsheetId = normalized,
            initialSyncComplete = if (changed) false else _state.value.initialSyncComplete,
            message = "스프레드시트 ID를 암호화 저장했습니다.",
        )
    }

    fun onGoogleAuthorized(result: AuthorizationResult) {
        if (repository.settings.spreadsheetId.length < 20) {
            onGoogleAuthorizationError("먼저 Google 스프레드시트 ID를 저장해 주세요.")
            return
        }
        if (result.hasResolution() || result.accessToken.isNullOrBlank()) {
            onGoogleAuthorizationError("Google Sheets 승인이 완료되지 않았습니다.")
            return
        }
        val email = result.toGoogleSignInAccount()?.email
        repository.settings.googleAuthorized = true
        repository.settings.googleAccountEmail = email
        if (repository.settings.syncEnabled) {
            SyncWorker.schedule(getApplication())
        }
        _state.value = _state.value.copy(
            googleAuthorized = true,
            googleAccountEmail = email,
            message = "Google Sheets 연결을 승인했습니다.",
        )
    }

    fun onGoogleAuthorizationError(message: String) {
        repository.settings.googleAuthorized = false
        _state.value = _state.value.copy(
            googleAuthorized = false,
            message = message,
        )
    }

    fun saveSyncOptions(intervalHours: Int, minimumBatteryPercent: Int) {
        repository.settings.syncIntervalHours = intervalHours
        repository.settings.minimumBatteryPercent = minimumBatteryPercent
        if (repository.settings.syncEnabled) {
            SyncWorker.schedule(getApplication())
        }
        _state.value = _state.value.copy(
            syncIntervalHours = repository.settings.syncIntervalHours,
            minimumBatteryPercent = repository.settings.minimumBatteryPercent,
            message = "자동 동기화 조건을 저장했습니다.",
        )
    }

    fun setSyncEnabled(enabled: Boolean) {
        if (enabled && !repository.settings.configured()) {
            _state.value = _state.value.copy(
                syncEnabled = false,
                message = "스프레드시트 ID를 저장하고 Google Sheets 연결을 승인해 주세요.",
            )
            return
        }
        repository.settings.syncEnabled = enabled
        if (enabled) {
            SyncWorker.schedule(getApplication())
        } else {
            SyncWorker.cancel(getApplication())
        }
        _state.value = _state.value.copy(
            syncEnabled = enabled,
            message = if (enabled) {
                "${repository.settings.syncIntervalHours}시간 간격 자동 동기화를 예약했습니다."
            } else {
                "자동 동기화를 중지했습니다."
            },
        )
    }

    fun sync(full: Boolean) {
        if (!repository.settings.configured()) {
            _state.value = _state.value.copy(
                message = "스프레드시트 ID를 저장하고 Google Sheets 연결을 승인해 주세요.",
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(
                busy = true,
                message = if (full) {
                    "전체 이력을 Google Sheets와 대조하는 중입니다."
                } else {
                    "새 건강정보를 Google Sheets에 동기화하는 중입니다."
                },
            )
            try {
                val report = withContext(Dispatchers.IO) {
                    repository.sync(forceFull = full)
                }
                _state.value = _state.value.copy(
                    busy = false,
                    initialSyncComplete = true,
                    lastSyncMessage = repository.settings.lastSyncMessage,
                    message = "${report.uploaded}개 원본 행을 동기화했습니다.",
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    busy = false,
                    message = "동기화 실패: ${error.message}",
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
