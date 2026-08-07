package com.example.healthbridge.sheets

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GoogleSheetsClient(private val spreadsheetId: String) {
    private val http = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun readExistingRowKeys(accessToken: String): MutableSet<String> {
        val url = valuesUrl("health_records!A2:A", append = false)
            .addQueryParameter("majorDimension", "ROWS")
            .build()
        val body = execute(accessToken, url)
        val values = JsonParser.parseString(body).asJsonObject.getAsJsonArray("values")
            ?: return mutableSetOf()
        return values.mapNotNull { row ->
            row.asJsonArray.firstOrNull()?.takeUnless { it.isJsonNull }?.asString
        }.toMutableSet()
    }

    fun appendHealthRows(accessToken: String, rows: List<List<Any>>) {
        if (rows.isEmpty()) return
        appendValues(accessToken, "health_records!A:N", rows)
    }

    fun appendSyncLog(accessToken: String, row: List<Any>) {
        appendValues(accessToken, "sync_log!A:F", listOf(row))
    }

    private fun appendValues(accessToken: String, range: String, rows: List<List<Any>>) {
        val url = valuesUrl(range, append = true)
            .addQueryParameter("valueInputOption", "RAW")
            .addQueryParameter("insertDataOption", "INSERT_ROWS")
            .build()
        val payload = gson.toJson(mapOf("majorDimension" to "ROWS", "values" to rows))
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        http.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            check(response.isSuccessful) {
                "Google Sheets 쓰기 실패 (${response.code}): ${responseBody.take(300)}"
            }
        }
    }

    private fun valuesUrl(range: String, append: Boolean): HttpUrl.Builder = HttpUrl.Builder()
        .scheme("https")
        .host("sheets.googleapis.com")
        .addPathSegment("v4")
        .addPathSegment("spreadsheets")
        .addPathSegment(spreadsheetId)
        .addPathSegment("values")
        .addPathSegment(if (append) "$range:append" else range)

    private fun execute(accessToken: String, url: HttpUrl): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        return http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            check(response.isSuccessful) {
                "Google Sheets 읽기 실패 (${response.code}): ${body.take(300)}"
            }
            body
        }
    }
}
