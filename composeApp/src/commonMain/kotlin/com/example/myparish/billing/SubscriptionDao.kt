package com.example.myparish.billing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription_status WHERE id = 1")
    suspend fun getStatus(): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: SubscriptionEntity)
}