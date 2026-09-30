package com.zaziapps.mtg_scanner.data.model

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing a comprehensive Magic: The Gathering card entity.
 * Maps the structural data payload returned from the Scryfall API or read from local persistence storage layers.
 *
 * @property id The unique tracking identifier assigned to this specific card printing.
 * @property oracleId The persistent tracking identifier linked to the card's core mechanical identity across multiple printings.
 * @property name The official reference name of the card in English.
 * @property printedName The localized card name as it appears on the physical printed card surface, allowing null values.
 * @property manaCost The symbolic cost representation structure required to cast the card, allowing null values.
 * @property typeLine The official mechanical classification and card type composition string, allowing null values.
 * @property printedTypeLine The localized type classification string as it appears on the physical printed card surface, allowing null values.
 * @property imageUris An [ImageUris] container holding remote asset paths for different graphic resolutions, allowing null values.
 * @property set The unique short code identifier representing the specific expansion set or release, allowing null values.
 * @property rarity The rarity classification tier assigned to the card printing, allowing null values.
 * @property oracleText The official rules text defining the current operational card mechanics, allowing null values.
 * @property printedText The localized rule description text exactly as it appears on the physical printed card surface, allowing null values.
 * @property lang The language localization code specifying the translation layout variant of the physical printing, allowing null values.
 */
data class MagicCard(
    @SerializedName("id") val id: String,
    @SerializedName("oracle_id") val oracleId: String,
    @SerializedName("name") val name: String,
    @SerializedName("printed_name") val printedName: String?,
    @SerializedName("mana_cost") val manaCost: String?,
    @SerializedName("type_line") val typeLine: String?,
    @SerializedName("printed_type_line") val printedTypeLine: String?,
    @SerializedName("image_uris") val imageUris: ImageUris?,
    @SerializedName("set") val set: String?,
    @SerializedName("rarity") val rarity: String?,
    @SerializedName("oracle_text") val oracleText: String?,
    @SerializedName("printed_text") val printedText: String?,
    @SerializedName("lang") val lang: String?
)
