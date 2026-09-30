package com.zaziapps.mtg_scanner.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data container architecture mapping the top-level API response payload from Scryfall.
 * Encapsulates the collection envelope used when fetching batch card search results from remote network endpoints.
 *
 * @property data The collection array containing matching [MagicCard] structural objects populated by the server.
 */
data class ScryfallResponse(
    @SerializedName("data") val data: List<MagicCard>
)
