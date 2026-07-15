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
data class ParishStatusResponse(
    @SerialName("is_active") val isActive: Int
)

@Serializable
data class Intention(
    val id: Int,
    val content: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    val category: String?,
    @SerialName("prayer_count") val prayerCount: Int,
    @SerialName("prayed_by_me") val prayedByMe: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("creator_id") val creatorId: String?,
    @SerialName("author_parish_id") val authorParishId: String?,
    @SerialName("praying_parishes") val prayingParishes: List<PrayingParish>? = emptyList(),
    @SerialName("candles") val candles: List<Candle>? = emptyList(),
    val country: String? = "PL",
    @SerialName("is_pinned") val isPinned: Boolean = false
)

@Serializable
data class Candle(
    val id: Int,
    @SerialName("device_id") val deviceId: String,
    @SerialName("candle_type") val candleType: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("lighter_parish_name") val lighterParishName: String? = null
)

@Serializable
data class PrayingParish(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double
)

@Serializable
data class CandleInventory(
    @SerialName("candle_8h") val candle8h: Int = 0,
    @SerialName("candle_12h") val candle12h: Int = 0,
    @SerialName("candle_24h") val candle24h: Int = 0
)

@Serializable
data class AddIntentionRequest(
    val content: String,
    @SerialName("device_id") val deviceId: String,
    val category: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean,
    val country: String
)

@Serializable
data class PrayRequest(
    @SerialName("intention_id") val intentionId: Int,
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class SetHomeParishRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("parish_id") val parishId: String?
)

@Serializable
data class DeleteIntentionRequest(
    val id: Int,
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class UpdateTokenRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("fcm_token") val fcmToken: String
)

@Serializable
data class UpdateIntentionRequest(
    @SerialName("intention_id") val intentionId: Int,
    @SerialName("device_id") val deviceId: String,
    val content: String,
    val category: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean
)

@Serializable
data class LightCandleRequest(
    @SerialName("intention_id") val intentionId: Int,
    @SerialName("device_id") val deviceId: String,
    @SerialName("candle_type") val candleType: String,
    @SerialName("duration_hours") val durationHours: Int
)

@Serializable
data class AddInventoryRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("duration_hours") val durationHours: Int,
    val amount: Int = 1
)

@Serializable
data class ExtinguishRequest(
    @SerialName("candle_id") val candleId: Int,
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class RenewRequest(
    @SerialName("intention_id") val intentionId: Int,
    @SerialName("device_id") val deviceId: String,
    val days: Int
)

@Serializable
data class PinRequest(
    @SerialName("intention_id") val intentionId: Int,
    @SerialName("device_id") val deviceId: String
)