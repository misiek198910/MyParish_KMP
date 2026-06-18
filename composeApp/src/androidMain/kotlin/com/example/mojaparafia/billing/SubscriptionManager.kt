package com.example.mojaparafia.billing

import android.content.Context

class SubscriptionManager private constructor(context: Context) {
    @JvmField
    val billingManager: BillingManager = BillingManager.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: SubscriptionManager? = null

        @JvmStatic
        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SubscriptionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}