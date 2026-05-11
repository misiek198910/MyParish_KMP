package com.example.myparish.billing

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "subscription_status")
data class SubscriptionEntity(
    @PrimaryKey val id: Int = 1, // Stałe ID = 1, aby mieć tylko jeden rekord
    val isPremium: Boolean,
    val purchaseToken: String?
)