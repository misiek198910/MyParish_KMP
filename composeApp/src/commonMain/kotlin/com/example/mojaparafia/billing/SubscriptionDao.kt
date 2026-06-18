package com.example.mojaparafia.billing

import androidx.room.*

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription_status WHERE id = 1")
    suspend fun getStatus(): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: SubscriptionEntity)
}