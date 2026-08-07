package com.example.healthbridge.sheets

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

class GoogleAuthorizationRequiredException :
    IllegalStateException("앱을 열고 Google Sheets 연결을 다시 승인해 주세요.")

object GoogleAuthorization {
    const val sheetsScope = "https://www.googleapis.com/auth/spreadsheets"

    fun request(): AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(sheetsScope)))
        .build()

    suspend fun accessToken(context: Context): String {
        val result = Identity.getAuthorizationClient(context)
            .authorize(request())
            .await()
        if (result.hasResolution()) throw GoogleAuthorizationRequiredException()
        return requireNotNull(result.accessToken) {
            "Google Sheets 액세스 토큰을 발급받지 못했습니다."
        }
    }
}
