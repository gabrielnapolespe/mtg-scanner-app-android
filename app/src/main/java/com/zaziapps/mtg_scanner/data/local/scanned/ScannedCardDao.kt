package com.zaziapps.mtg_scanner.data.local.scanned

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) providing access to the scanned cards history catalog.
 * Defines the database interactions and SQL operations executed against the personal tracking data layer.
 */
@Dao
interface ScannedCardDao {

    /**
     * Retrieves all recorded scan events ordered sequentially from the most recent transaction entry down.
     *
     * @return An observable cold [Flow] containing updated collections of historical [ScannedCard] elements.
     */
    @Query("SELECT * FROM scanned_cards ORDER BY scanDate DESC")
    fun getHistory(): Flow<List<ScannedCard>>

    /**
     * Inserts a single scanned card event record into the tracking history collection database.
     * Replaces the existing record if a primary key or unique identifier conflict occurs.
     *
     * @param card The [ScannedCard] history structural instance entity to be persisted.
     * @return The auto-generated row identifier integer value assigned to the newly created tracking record entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardIntoHistory(card: ScannedCard): Long
}
