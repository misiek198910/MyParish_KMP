package com.example.mojaparafia.network

import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.model.AddIntentionRequest
import com.example.mojaparafia.model.AdminConfig
import com.example.mojaparafia.model.DeleteIntentionRequest
import com.example.mojaparafia.model.Intention
import com.example.mojaparafia.model.LightCandleRequest
import com.example.mojaparafia.model.PinRequest
import com.example.mojaparafia.model.PrayRequest
import com.example.mojaparafia.model.RenewRequest
import com.example.mojaparafia.model.SetHomeParishRequest
import com.example.mojaparafia.model.UpdateIntentionRequest
import com.example.mojaparafia.model.UserStatsResponse
import com.example.mojaparafia.model.ExtinguishRequest
import com.example.mojaparafia.model.NewsResponse
import com.example.mojaparafia.model.IpLocationResponse
import com.example.mojaparafia.model.UpdateTokenRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class ParishApiService {

    private val baseUrl = "https://api-parafia.mivs.dev"

    suspend fun getParishes(since: String?, onProgress: (Int) -> Unit): List<ParishEntity>? {
        return try {
            networkClient.get("$baseUrl/v2/parishes") {
                if (since != null) parameter("since", since)

                onDownload { bytesDownloaded, totalBytes ->
                    if (totalBytes != null && totalBytes > 0) {
                        val percentage = ((bytesDownloaded * 100) / totalBytes).toInt()
                        onProgress(percentage)
                    }
                }
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun logFavoriteAction(parishId: String, parishName: String?, action: String): Boolean {
        return try {
            val response = networkClient.submitForm(
                url = "$baseUrl/favorites/log",
                formParameters = Parameters.build {
                    append("parish_id", parishId)
                    if (parishName != null) append("parish_name", parishName)
                    append("action", action)
                }
            )
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getIntentions(deviceId: String): List<Intention>? {
        return try {
            networkClient.get("$baseUrl/intentions") {
                parameter("device_id", deviceId)
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addIntention(request: AddIntentionRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/intentions/add") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerPrayer(request: PrayRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/intentions/pray") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteIntention(request: DeleteIntentionRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/intentions/delete") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateIntention(request: UpdateIntentionRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/intentions/update") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renewIntention(request: RenewRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/intentions/renew") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun pinIntention(request: PinRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/intentions/pin") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun lightCandle(request: LightCandleRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/v2/intentions/candle") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun extinguishCandle(request: ExtinguishRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/v2/intentions/candle/extinguish") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserStats(deviceId: String): UserStatsResponse? {
        return try {
            networkClient.get("$baseUrl/user/stats") {
                parameter("device_id", deviceId)
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setHomeParish(request: SetHomeParishRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/user/set-home-parish") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getNewsFeed(): List<NewsResponse>? {
        return try {
            networkClient.get("$baseUrl/news").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getIpLocation(): IpLocationResponse? {
        return try {
            networkClient.get("$baseUrl/api/locate-me").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateFcmToken(request: UpdateTokenRequest): Boolean {
        return try {
            networkClient.post("$baseUrl/user/update-token") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAdminConfig(): AdminConfig? {
        return try {
            networkClient.get("$baseUrl/api/admin/config").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateAdminFcmToken(deviceId: String, token: String): Boolean {
        return try {
            networkClient.post("$baseUrl/api/admin/update-credentials") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("device_id" to deviceId, "fcm_token" to token))
            }.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }
}

val apiService = ParishApiService()