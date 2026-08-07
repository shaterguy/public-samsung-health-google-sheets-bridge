package com.example.healthbridge.sheets

import android.accounts.Account
import android.content.Context
import com.example.healthbridge.data.SecureSettings
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthorizationRequiredException :
    IllegalStateException("앱을 열고 Google Sheets 연결을 다시 승인해 주세요.")

object GoogleAuthorization {
    const val sheetsScope = "https://www.googleapis.com/auth/spreadsheets"
    private const val tokenScope = "oauth2:$sheetsScope"

    @Suppress("DEPRECATION")
    suspend fun accessToken(context: Context, accountName: String): String =
        withContext(Dispatchers.IO) {
            require(accountName.isNotBlank()) { "Google 계정을 먼저 선택해 주세요." }
            GoogleAuthUtil.getToken(
                context,
                Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE),
                tokenScope,
            )
        }

    suspend fun accessToken(context: Context): String {
        val accountName = SecureSettings(context).googleAccountEmail
            ?.takeIf { it.isNotBlank() }
            ?: throw GoogleAuthorizationRequiredException()
        return try {
            accessToken(context, accountName)
        } catch (_: UserRecoverableAuthException) {
            throw GoogleAuthorizationRequiredException()
        }
    }
}
