package com.zaziapps.mtg_scanner.data.model

data class CustomCollection(
    val id: String,
    val name: String,
    val description: String = "",
    val cardCount: Int = 0
)
