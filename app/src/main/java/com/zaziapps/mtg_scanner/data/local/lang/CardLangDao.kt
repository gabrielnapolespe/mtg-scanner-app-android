package com.zaziapps.mtg_scanner.data.local.lang

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) providing access to the multi-language card catalog.
 * Defines the database interactions and SQL operations executed against the card translations data layer.
 */
@Dao
interface CardLangDao {

    /**
     * Inserts a single card translation record into the database.
     * Replaces the existing record if a primary key conflict occurs.
     *
     * @param card The [CardTranslations] entity to be persisted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardTranslations)

    /**
     * Inserts a batch of card translation records into the database.
     * Replaces existing records if primary key conflicts occur.
     *
     * @param cards The collection of [CardTranslations] entities to be persisted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCards(cards: List<CardTranslations>)

    /**
     * Updates an existing card translation record matching the entity's primary key identifier.
     *
     * @param card The [CardTranslations] entity containing the updated structural properties.
     */
    @Update
    suspend fun updateCard(card: CardTranslations)

    /**
     * Retrieves all localized information belonging to a specific card matching the provided unique Oracle ID.
     *
     * @param oracleId The unique operational string identifier assigned to the card profile.
     * @return The matching [CardTranslations] entity instance if found, or null if no record exists.
     */
    @Query("SELECT * FROM card_translations WHERE oracle_id = :oracleId LIMIT 1")
    suspend fun getCardByOracleId(oracleId: String): CardTranslations?

    /**
     * Executes a flexible lookup query matching partial string sequences within either English or Spanish card titles.
     *
     * @param searchQuery The search term string segment to find within the database records.
     * @return A collection containing all matching [CardTranslations] entities.
     */
    @Query("""
        SELECT * FROM card_translations 
        WHERE name_en LIKE '%' || :searchQuery || '%' 
           OR name_es LIKE '%' || :searchQuery || '%'
    """)
    suspend fun searchCardsByName(searchQuery: String): List<CardTranslations>

    /**
     * Filters and retrieves all card instances whose internal color identity markers match the targeted color parameter query.
     *
     * @param color The specific identity character code indicator used for filtering.
     * @return A collection containing all matching [CardTranslations] entities.
     */
    @Query("SELECT * FROM card_translations WHERE colors LIKE '%' || :color || '%'")
    suspend fun getCardsByColor(color: String): List<CardTranslations>

    /**
     * Stream-based query exposing the complete database collection reactively to continuous consumer channels.
     *
     * @return An observable cold [Flow] containing updated collections of all [CardTranslations] elements.
     */
    @Query("SELECT * FROM card_translations")
    fun getAllCardsFlow(): Flow<List<CardTranslations>>

}
