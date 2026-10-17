package com.zaziapps.mtg_scanner.data.local.collections

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.zaziapps.mtg_scanner.data.model.MagicCard

@Entity(tableName = "custom_collections")
data class CustomCollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String
)

@Entity(
    tableName = "collection_cards_cross_ref",
    primaryKeys = ["collectionId", "cardId"],
    indices = [Index(value = ["cardId"])]
)
data class CardCollectionCrossRef(
    val collectionId: String,
    val cardId: String,
    val magicCard: MagicCard
)
