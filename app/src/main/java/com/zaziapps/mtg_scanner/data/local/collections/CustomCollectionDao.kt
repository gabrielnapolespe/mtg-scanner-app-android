package com.zaziapps.mtg_scanner.data.local.collections

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCollectionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(collection: CustomCollectionEntity)

    @Query("SELECT * FROM custom_collections")
    fun getAllCollectionsFlow(): Flow<List<CustomCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCardToCollection(crossRef: CardCollectionCrossRef)

    @Query("SELECT magicCard FROM collection_cards_cross_ref WHERE collectionId = :collectionId")
    suspend fun getCardsForCollection(collectionId: String): List<com.zaziapps.mtg_scanner.data.model.MagicCard>

    @Query("SELECT COUNT(*) FROM collection_cards_cross_ref WHERE collectionId = :collectionId")
    fun getCardCountForCollectionFlow(collectionId: String): Flow<Int>
}
