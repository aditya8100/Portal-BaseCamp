package com.portalhomebase.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiException(val status: Int, msg: String) : Exception("http $status: $msg")

class BoardApi(baseUrl: String, private val token: String) {
    private val base = baseUrl.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val rb = body?.toString()?.toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(base + path)
                .header("Authorization", "Bearer $token")
                .method(method, if (method == "GET" || method == "DELETE") null else (rb ?: "{}".toRequestBody("application/json".toMediaType())))
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: "{}"
                if (!resp.isSuccessful) throw ApiException(resp.code, text.take(200))
                JSONObject(if (text.isBlank()) "{}" else text)
            }
        }

    suspend fun health(): Boolean = try {
        call("/api/health").optBoolean("ok")
    } catch (e: Exception) {
        false
    }

    suspend fun getCards(): List<Card> {
        val arr = call("/api/cards").optJSONArray("cards") ?: return emptyList()
        return (0 until arr.length()).map { Card.parse(arr.getJSONObject(it)) }
    }

    suspend fun toggleItem(id: String, index: Int, done: Boolean) {
        call("/api/cards/$id/items", "PUT", JSONObject().put("index", index).put("done", done))
    }

    suspend fun cooked(id: String) {
        call("/api/cards/$id/cooked", "POST", JSONObject())
    }

    suspend fun setFavorite(id: String, fav: Boolean) {
        call("/api/cards/$id", "PATCH", JSONObject().put("favorite", fav))
    }

    suspend fun setPinned(id: String, pinned: Boolean) {
        call("/api/cards/$id", "PATCH", JSONObject().put("pinned", pinned))
    }

    suspend fun deleteCard(id: String) {
        call("/api/cards/$id", "DELETE")
    }

    suspend fun getWeather(): Weather = Weather.parse(call("/api/weather"))

    suspend fun getEvents(): Pair<String, List<CalEvent>> {
        val o = call("/api/calendar")
        val arr = o.optJSONArray("events") ?: return Pair("America/Chicago", emptyList())
        return Pair(
            o.optString("tz", "America/Chicago"),
            (0 until arr.length()).map { CalEvent.parse(arr.getJSONObject(it)) },
        )
    }

    suspend fun getWeek(): List<WeekDay> {
        val arr = call("/api/calendar/week").optJSONArray("days") ?: return emptyList()
        return (0 until arr.length()).map { WeekDay.parse(arr.getJSONObject(it)) }
    }

    suspend fun getAccessories(): List<Accessory> {
        val arr = call("/api/home/accessories").optJSONArray("accessories") ?: return emptyList()
        return Accessory.parseList(arr)
    }

    suspend fun setCharacteristic(id: String, type: String, value: Any) {
        call(
            "/api/home/accessories/$id", "PUT",
            JSONObject().put("characteristicType", type).put("value", value),
        )
    }
}
