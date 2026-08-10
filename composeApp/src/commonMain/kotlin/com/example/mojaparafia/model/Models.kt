package com.example.mojaparafia.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsResponse(
    val id: Int = 0,
    val title: String? = null,
    val content: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("action_link") val actionLink: String? = null,
    @SerialName("publish_date") val publishDate: String? = null,
    @SerialName("is_visible") val isVisible: Boolean? = false
)

@Serializable
data class SetHomeParishRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("parish_id") val parishId: String?
)
@Serializable
data class UpdateTokenRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("fcm_token") val fcmToken: String
)