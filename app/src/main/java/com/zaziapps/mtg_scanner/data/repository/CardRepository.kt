package com.zaziapps.mtg_scanner.data.repository

import com.zaziapps.mtg_scanner.data.local.lang.CardLangDao
import com.zaziapps.mtg_scanner.data.local.lang.CardTranslations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository component mediating data operations between the application domain and local persistence layers.
 * Coordinates localized card information lookups by bridging background execution scopes with database access objects.
 *
 * @property cardLangDao The data access object handling structural queries on the card translations database table.
 */
class CardRepository(private val cardLangDao: CardLangDao) {

    /**
     * Executes an asynchronous partial search across database records using cleansed string sequences.
     * Evaluates search arguments against both English and Spanish title properties inside a safe dispatcher context.
     *
     * @param text The raw extracted string sequence segment targeted for database filtering lookup.
     * @return A collection containing matching [CardTranslations] entity items, or an empty list if data is invalid.
     */
    suspend fun searchCardByText(text: String): List<CardTranslations> {
        // Enforce boundary safety checks to intercept blank strings or fallback placeholder tags before invoking database routines.
        if (text.isBlank() || text == "none") return emptyList()

        // Move execution context explicitly onto an optimal background thread pool designated for data input and output operations.
        return withContext(Dispatchers.IO) {
            cardLangDao.searchCardsByName(text)
        }
    }

    /**
     * Resolves a distinct card localization record matching a specific operational identifier sequence.
     *
     * @param oracleId The unique operational string identifier tracking the mechanical identity of the target card.
     * @return The specific matching [CardTranslations] database instance sequence if found, or null if no match exists.
     */
    suspend fun getCardByOracleId(oracleId: String): CardTranslations? {
        // Shift task execution away from the primary interface processing pipeline to maintain layout responsiveness.
        return withContext(Dispatchers.IO) {
            cardLangDao.getCardByOracleId(oracleId)
        }
    }
}
