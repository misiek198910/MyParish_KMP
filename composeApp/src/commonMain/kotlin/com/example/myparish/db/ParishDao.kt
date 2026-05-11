package com.example.myparish.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ParishDao {

    // --- SYNCHRONIZACJA (DELTA SYNC) ---

    @Query("SELECT MAX(last_update) FROM parishes")
    suspend fun getLatestUpdateTimestamp(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParishes(parishes: List<ParishEntity>)

    // --- FILTROWANIE I UI ---

    @Query("SELECT * FROM parishes ORDER BY name ASC")
    fun getAllParishes(): Flow<List<ParishEntity>>

    @Query("SELECT * FROM parishes WHERE hasMassForChildren = 1 ORDER BY name ASC")
    fun getParishesWithMassForChildren(): Flow<List<ParishEntity>>

    @Query("SELECT * FROM parishes WHERE hasMassSunday = 1 ORDER BY name ASC")
    fun getParishesWithVigilMass(): Flow<List<ParishEntity>>

    @Query("SELECT * FROM parishes WHERE adorationInfo IS NOT NULL AND adorationInfo != '' ORDER BY name ASC")
    fun getParishesWithAdoration(): Flow<List<ParishEntity>>

    @Query("SELECT * FROM parishes WHERE confessionInfo IS NOT NULL AND confessionInfo != '' ORDER BY name ASC")
    fun getParishesWithConfession(): Flow<List<ParishEntity>>

    @Query("SELECT * FROM parishes WHERE officeHoursText IS NOT NULL AND officeHoursText != '' ORDER BY name ASC")
    fun getParishesWithOfficeHours(): Flow<List<ParishEntity>>

    @Query("SELECT * FROM parishes WHERE id = :parishId")
    fun getParishById(parishId: String): Flow<ParishEntity>

    @Query("SELECT * FROM parishes WHERE isFavorite = 1")
    fun getFavoriteParishes(): Flow<List<ParishEntity>>

    @Query("""
        SELECT * FROM parishes WHERE 
        (:catActive = 0 OR isCathedral = 1) AND 
        (:chuActive = 0 OR isCathedral = 0) AND 
        (:childrenActive = 0 OR hasMassForChildren = 1) AND 
        (:vigilActive = 0 OR (hasMassSunday = 1 AND hasMassSundayHour != '')) AND 
        (:confessionActive = 0 OR (confessionInfo IS NOT NULL AND confessionInfo != '')) AND
        (:adorationActive = 0 OR (adorationInfo IS NOT NULL AND adorationInfo != '')) AND
        (:firstSatActive = 0 OR firstSaturdayOfMonth = 1) AND 
        (:favActive = 0 OR isFavorite = 1) AND
        (:multiActive = 0 OR (photoUrl != '' OR websiteUrl != '')) AND
        (:regionQuery = '' OR diocese LIKE '%' || :regionQuery || '%' OR deanery LIKE '%' || :regionQuery || '%')
        ORDER BY name ASC
    """)
    fun getFilteredParishes(
        catActive: Int, chuActive: Int, childrenActive: Int,
        vigilActive: Int, confessionActive: Int, adorationActive: Int,
        firstSatActive: Int,
        favActive: Int, multiActive: Int,
        regionQuery: String
    ): Flow<List<ParishEntity>>

    // --- OPERACJE BEZPOŚREDNIE (DODATKOWE) ---

    @Update
    suspend fun update(parish: ParishEntity)

    @Query("SELECT * FROM parishes WHERE id = :id LIMIT 1")
    suspend fun getParishByIdDirect(id: String): ParishEntity?

    @Query("DELETE FROM parishes")
    suspend fun deleteAll()

    @Query("SELECT id FROM parishes WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<String>

    @Query("SELECT * FROM parishes WHERE isCathedral = 1 ORDER BY name ASC")
    fun getCathedrals(): Flow<List<ParishEntity>>

    @Query("SELECT * FROM parishes WHERE isCathedral = 0 ORDER BY name ASC")
    fun getRegularChurches(): Flow<List<ParishEntity>>
}