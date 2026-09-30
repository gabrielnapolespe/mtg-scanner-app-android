package com.zaziapps.mtg_scanner.ui

enum class ScanScreen {
    SCAN_CAM, // View to capture image with card info
    CARD_DETAIL, // View to show found card information in detail
    CARD_HISTORY, // View displaying the scrollable list of previously scanned cards
    COLLECTION_VIEW // View displaying the scrollable list of cards in collection
}