package com.zaziapps.mtg_scanner.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data container architecture mapping the external image endpoint asset distribution structures.
 * Encapsulates remote image address paths parsed from serialization payloads.
 *
 * @property normal The remote URL string pointing to a standard resolution image of the card artwork layout.
 */
data class ImageUris(
    @SerializedName("normal") val normal: String?
)
