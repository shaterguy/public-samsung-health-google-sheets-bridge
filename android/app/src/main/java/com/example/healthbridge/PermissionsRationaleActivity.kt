package com.example.healthbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Rationale { finish() } }
    }
}

@Composable
private fun Rationale(onClose: () -> Unit) {
    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("건강 데이터 사용 안내", style = MaterialTheme.typography.headlineSmall)
            Text(
                "이 앱은 사용자가 허용한 Health Connect 건강 데이터를 읽어 사용자가 선택한 Google 스프레드시트에 직접 추가합니다. 별도 개인 서버나 제3자 분석 서비스로 건강 데이터를 전송하지 않으며, Health Connect 설정과 Google 계정 권한 화면에서 언제든 접근을 취소할 수 있습니다.",
                modifier = Modifier.padding(vertical = 20.dp),
            )
            Button(onClick = onClose) { Text("확인") }
        }
    }
}
