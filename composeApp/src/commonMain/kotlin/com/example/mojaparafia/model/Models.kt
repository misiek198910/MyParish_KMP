package com.example.mojaparafia.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminConfig(
    val admin_device_id: String,
    val admin_fcm_token: String
)
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
data class UserStatsResponse(
    val points: Int,
    @SerialName("next_reward") val nextReward: Int,
    @SerialName("has_crown") val hasCrown: Boolean,
    @SerialName("has_premium_reward") val hasPremiumReward: Boolean
)

@Serializable
data class IpLocationResponse(
    val lat: Double,
    val lon: Double,
    val country: String
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