package com.zaziapps.mtg_scanner.data.local.lang

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the database table structure mapped to store multi-language card names and color identities.
 * This entity serves as the centralized multilingual dictionary for looking up localized card metadata.
 *
 * @property oracleId The unique persistent operational primary identifier linked to a card's core mechanical design.
 * @property nameEn The reference name of the card in English.
 * @property colors A comma-separated string mapping the card's color components, or null if the card is colorless.
 * @property nameEs The localized name of the card in Spanish, allowing null values.
 * @property nameFr The localized name of the card in French, allowing null values.
 * @property nameDe The localized name of the card in German, allowing null values.
 * @property nameIt The localized name of the card in Italian, allowing null values.
 */
@Entity(tableName = "card_translations")
data class CardTranslations(
    @PrimaryKey
    @ColumnInfo(name = "oracle_id")
    val oracleId: String,

    @ColumnInfo(name = "name_en")
    val nameEn: String,

    @ColumnInfo(name = "colors")
    val colors: String?,

    @ColumnInfo(name = "name_es")
    val nameEs: String?,

    @ColumnInfo(name = "name_fr")
    val nameFr: String?,

    @ColumnInfo(name = "name_de")
    val nameDe: String?,

    @ColumnInfo(name = "name_it")
    val nameIt: String?
)

/**
 * Extracts and cleanses the raw comma-separated color string into a list of individual color identity strings.
 *
 * @return A collection of isolated, trimmed color code indicators, or an empty list if no colors are specified.
 */
fun CardTranslations.getColorList(): List<String> {
    return this.colors
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}
