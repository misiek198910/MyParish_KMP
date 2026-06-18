package com.example.mojaparafia.billing

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_status")
data class SubscriptionEntity(
    @PrimaryKey val id: Int = 1,
    val isPremium: Boolean,
    val purchaseToken: String?
)