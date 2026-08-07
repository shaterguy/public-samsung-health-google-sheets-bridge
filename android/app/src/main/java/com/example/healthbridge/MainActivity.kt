package com.example.healthbridge

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthbridge.sheets.GoogleAuthorization
import com.example.healthbridge.sync.SyncPolicy
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.common.AccountPicker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var vm: MainViewModel
    private var pendingGoogleAccount: String? = null

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        vm.refresh()
    }

    private val googleAccountLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (result.resultCode == Activity.RESULT_OK && !accountName.isNullOrBlank()) {
            authorizeGoogleAccount(accountName)
        } else {
            vm.onGoogleAuthorizationError("Google 계정 선택이 취소되었습니다.")
        }
    }

    private val googleConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = pendingGoogleAccount
        if (result.resultCode == Activity.RESULT_OK && !accountName.isNullOrBlank()) {
            authorizeGoogleAccount(accountName)
        } else {
            pendingGoogleAccount = null
            vm.onGoogleAuthorizationError("Google Sheets 승인이 취소되었습니다.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentVm: MainViewModel = viewModel()
            vm = currentVm
            val state by currentVm.state.collectAsState()
            HealthBridgeScreen(
                state = state,
                onRequestPermissions = {
                    permissionLauncher.launch(currentVm.requestedPermissions())
                },
                onOpenHealthConnect = {
                    startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
                },
                onInstallHealthConnect = {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=com.google.android.apps.healthdata"),
                        )
                    )
                },
                onSaveSpreadsheetId = currentVm::saveSpreadsheetId,
                onConnectGoogle = ::requestGoogleAuthorization,
                onSaveOptions = currentVm::saveSyncOptions,
                onToggleSync = currentVm::setSyncEnabled,
                onSync = currentVm::sync,
                onMessageShown = currentVm::clearMessage,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized) vm.refresh()
    }

    private fun requestGoogleAuthorization() {
        val options = AccountPicker.AccountChooserOptions.Builder()
            .setAllowableAccountsTypes(listOf(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE))
            .setAlwaysShowAccountPicker(true)
            .build()
        googleAccountLauncher.launch(AccountPicker.newChooseAccountIntent(options))
    }

    private fun authorizeGoogleAccount(accountName: String) {
        pendingGoogleAccount = accountName
        lifecycleScope.launch {
            try {
                GoogleAuthorization.accessToken(this@MainActivity, accountName)
                pendingGoogleAccount = null
                vm.onGoogleAuthorized(accountName)
            } catch (error: UserRecoverableAuthException) {
                @Suppress("DEPRECATION")
                val recoveryIntent: Intent? = error.intent
                if (recoveryIntent != null) {
                    googleConsentLauncher.launch(recoveryIntent)
                } else {
                    pendingGoogleAccount = null
                    vm.onGoogleAuthorizationError("Google Sheets 승인 화면을 열 수 없습니다.")
                }
            } catch (error: Exception) {
                pendingGoogleAccount = null
                vm.onGoogleAuthorizationError(
                    "Google Sheets 연결 실패: ${error.message ?: error.javaClass.simpleName}"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthBridgeScreen(
    state: MainUiState,
    onRequestPermissions: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    onSaveSpreadsheetId: (String) -> Unit,
    onConnectGoogle: () -> Unit,
    onSaveOptions: (Int, Int) -> Unit,
    onToggleSync: (Boolean) -> Unit,
    onSync: (Boolean) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    var spreadsheetId by remember(state.spreadsheetId) {
        mutableStateOf(state.spreadsheetId)
    }
    var intervalHours by remember(state.syncIntervalHours) {
        mutableIntStateOf(state.syncIntervalHours)
    }
    var minimumBattery by remember(state.minimumBatteryPercent) {
        mutableIntStateOf(state.minimumBatteryPercent)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Samsung Health Bridge") })
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Health Connect 건강정보를 별도 개인 서버 없이 사용자가 지정한 Google Sheets 문서에 직접 누적합니다."
                )

                StatusCard(
                    "1. Health Connect",
                    if (state.dataPermissionsGranted) {
                        "17종 데이터 읽기 권한 허용됨"
                    } else {
                        "건강정보 읽기 권한이 필요합니다."
                    },
                ) {
                    if (state.providerStatus == HealthConnectClient.SDK_AVAILABLE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onRequestPermissions) {
                                Text("권한 요청")
                            }
                            OutlinedButton(onClick = onOpenHealthConnect) {
                                Text("접근 관리")
                            }
                        }
                    } else {
                        Button(onClick = onInstallHealthConnect) {
                            Text("Health Connect 설치·업데이트")
                        }
                    }
                }

                StatusCard(
                    "2. Google Sheets 직접 연결",
                    if (state.googleAuthorized) {
                        state.googleAccountEmail?.let { "연결됨: $it" } ?: "Google 계정 연결됨"
                    } else {
                        "스프레드시트 ID 저장 후 Google 계정 승인이 필요합니다."
                    },
                ) {
                    OutlinedTextField(
                        value = spreadsheetId,
                        onValueChange = { spreadsheetId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Google 스프레드시트 ID") },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = spreadsheetId.trim().length >= 20,
                            onClick = { onSaveSpreadsheetId(spreadsheetId) },
                        ) {
                            Text("스프레드시트 ID 저장")
                        }
                        Button(
                            enabled = state.spreadsheetId.length >= 20,
                            onClick = onConnectGoogle,
                        ) {
                            Text(
                                if (state.googleAuthorized) {
                                    "Google 권한 갱신"
                                } else {
                                    "Google Sheets 연결"
                                }
                            )
                        }
                    }
                    Text(
                        if (state.spreadsheetId.isBlank()) {
                            "스프레드시트 ID는 앱의 암호화 저장소에만 보관됩니다."
                        } else {
                            "스프레드시트 ID 저장됨"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                StatusCard(
                    "3. 자동 동기화 조건",
                    state.lastSyncMessage ?: "아직 직접 동기화하지 않았습니다.",
                ) {
                    Text("동기화 주기")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SyncPolicy.intervalHours.take(3).forEach { value ->
                            ChoiceButton(
                                selected = intervalHours == value,
                                label = "${value}시간",
                            ) {
                                intervalHours = value
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SyncPolicy.intervalHours.drop(3).forEach { value ->
                            ChoiceButton(
                                selected = intervalHours == value,
                                label = "${value}시간",
                            ) {
                                intervalHours = value
                            }
                        }
                    }

                    Text("자동 동기화 최소 배터리")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SyncPolicy.batteryPercentages.take(3).forEach { value ->
                            ChoiceButton(
                                selected = minimumBattery == value,
                                label = "$value%",
                            ) {
                                minimumBattery = value
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SyncPolicy.batteryPercentages.drop(3).forEach { value ->
                            ChoiceButton(
                                selected = minimumBattery == value,
                                label = "$value%",
                            ) {
                                minimumBattery = value
                            }
                        }
                    }

                    Text("네트워크: Wi-Fi 연결 시에만 자동 실행")
                    Button(
                        onClick = {
                            onSaveOptions(intervalHours, minimumBattery)
                        }
                    ) {
                        Text("자동 동기화 조건 저장")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${state.syncIntervalHours}시간마다 · 배터리 ${state.minimumBatteryPercent}% 이상"
                        )
                        Switch(
                            checked = state.syncEnabled,
                            onCheckedChange = onToggleSync,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !state.busy &&
                                state.dataPermissionsGranted &&
                                state.googleAuthorized &&
                                state.spreadsheetId.length >= 20,
                            onClick = { onSync(false) },
                        ) {
                            Text("지금 동기화")
                        }
                        OutlinedButton(
                            enabled = !state.busy &&
                                state.dataPermissionsGranted &&
                                state.googleAuthorized &&
                                state.spreadsheetId.length >= 20,
                            onClick = { onSync(true) },
                        ) {
                            Text("전체 다시 대조")
                        }
                    }

                    if (state.busy) {
                        Text("처리 중입니다. 앱을 종료하지 마세요.")
                    }
                }

                Text(
                    "자동 동기화는 Android 절전 정책 때문에 정확한 시각보다 늦게 실행될 수 있습니다. 조건을 만족하는 다음 실행에서 누락된 원본 행을 함께 추가합니다.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ChoiceButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label)
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    status: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(status, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}
