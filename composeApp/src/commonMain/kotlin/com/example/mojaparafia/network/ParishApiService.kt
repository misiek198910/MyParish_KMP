package com.example.mojaparafia.network

import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.db.ParishEventEntity
import com.example.mojaparafia.model.NewsResponse
import com.example.mojaparafia.model.SetHomeParishRequest
import com.example.mojaparafia.model.UpdateTokenRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
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

    suspend fun getEvents(
        parishId: String? = null,
        since: String? = null
    ): List<ParishEventEntity>? {
        return try {
            networkClient.get("$baseUrl/panel/events/list") {
                if (!parishId.isNullOrEmpty()) parameter("parish_id", parishId)
                if (!since.isNullOrEmpty()) parameter("since", since)
            }.body()
        } catch (e: Exception) {
            null
        }
    }
}

val apiService = ParishApiService()