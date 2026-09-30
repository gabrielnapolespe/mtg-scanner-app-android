package com.zaziapps.mtg_scanner.data.local.scanned

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.zaziapps.mtg_scanner.data.model.MagicCard

/**
 * Type converter implementation enabling the Room database engine to persist non-primitive complex entities.
 * Serializes custom data patterns into flat structures and maps them back into typed objects upon retrieval.
 */
class MagicCardConverter {

    /**
     * Shared operational serializer instance configured for JSON conversion tasks.
     */
    private val gson = Gson()

    /**
     * Serializes a complex card structural instance into a standard JSON text representation for column storage.
     *
     * @param card The targeted [MagicCard] structure instance to convert, accepting null values.
     * @return A flat string representation containing serialized object data, or null if the parameter is empty.
     */
    @TypeConverter
    fun fromMagicCard(card: MagicCard?): String? {
        return gson.toJson(card)
    }

    /**
     * Deserializes a standard JSON text configuration sequence back into its fully typed structural object instance.
     *
     * @param json The source data string containing serialized structural properties, accepting null values.
     * @return A newly mapped [MagicCard] object sequence instance initialized with properties, or null if string data is empty.
     */
    @TypeConverter
    fun toMagicCard(json: String?): MagicCard? {
        return gson.fromJson(json, MagicCard::class.java)
    }
}
