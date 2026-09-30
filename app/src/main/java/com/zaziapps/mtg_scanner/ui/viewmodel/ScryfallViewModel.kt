package com.zaziapps.mtg_scanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zaziapps.mtg_scanner.data.local.lang.CardTranslations
import com.zaziapps.mtg_scanner.data.model.MagicCard
import com.zaziapps.mtg_scanner.data.repository.CardRepository
import com.zaziapps.mtg_scanner.data.repository.ScryfallRepository
import com.zaziapps.mtg_scanner.ui.themes.CardDetailBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Closed interface defining the explicit structural states of the collection network retrieval flow.
 */
sealed interface CollectionUiState {
    object Loading : CollectionUiState
    data class Success(val cards: List<MagicCard>) : CollectionUiState
    data class Error(val message: String) : CollectionUiState
}

/**
 * Architecture state controller coordinating local lookup caching and external network data matching.
 * Implements concurrent query interception mechanisms to reduce data layer load.
 *
 * @property cardRepository Central database query repository providing access to localized translations.
 */
class ScryfallViewModel(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val scryfallRepository = ScryfallRepository()
    private val manas = setOf('W', 'U', 'B', 'R', 'G')

    private val _isSearchingCard = MutableStateFlow(false)
    val isSearchingCard: StateFlow<Boolean> = _isSearchingCard

    private val _searchResults = MutableStateFlow<List<CardTranslations>>(emptyList())
    val searchResults: StateFlow<List<CardTranslations>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val activeSearches = mutableSetOf<String>()

    /**
     * Executes a localized search query across offline title databases while filtering duplicates.
     * Intercepts overlapping active execution calls asynchronously using thread-safe map lookups.
     *
     * @param text The extracted text snippet used for text-matching database verification.
     * @return A collection containing all matching [CardTranslations] records discovered locally.
     */
    suspend fun searchCard(text: String): List<CardTranslations> {
        if (text.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            synchronized(activeSearches) {
                // Intercept execution pathways to block duplicated overlapping processing jobs.
                if (activeSearches.contains(text)) {
                    return@withContext emptyList()
                }

                activeSearches.add(text)
                _isSearching.value = true
            }

            try {
                // Delegate the finalized string processing sequence down to the local storage interface.
                cardRepository.searchCardByText(text)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            } finally {
                synchronized(activeSearches) {
                    activeSearches.remove(text)
                    // Mutate state flags only after confirming all matching requests have cleared processing lanes.
                    _isSearching.value = activeSearches.isNotEmpty()
                }
            }
        }
    }

    /**
     * Loads all cards from a specified set expansion in the chosen language.
     *
     * @param setCode The unique 3 or 4 letter expansion/set identifier code.
     * @param lang The language code filter for the returned cards.
     * @return A collection of matching [MagicCard] structures returned from the remote source.
     */
    suspend fun searchCollection(setCode: String, lang: String = "en"): List<MagicCard> {
        return withContext(Dispatchers.IO) {
            try {
                scryfallRepository.getCardsFromSetAndLanguage(setCode, lang)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Searches a card by name in the specified input language and resolves it in the desired output language.
     *
     * @param name Name of the card used for network query matches.
     * @param langInput Language profile string corresponding to the search parameter text.
     * @param langOutput Targeted localization code format for the final card instance.
     * @return A complete [MagicCard] metadata structure model profile instance, or null if an error occurs.
     */
    suspend fun searchCardByName(name: String, langInput: String = "en", langOutput: String = "en"): MagicCard? {
        if (name.isBlank()) return null

        return withContext(Dispatchers.IO) {
            _isSearchingCard.value = true
            try {
                val originalCard = scryfallRepository.searchByNameAndLanguage(name, langInput)

                if (langInput == langOutput) {
                    return@withContext originalCard
                }

                // Fetch the cross-referenced localized card profile via the persistent oracle tracking key.
                scryfallRepository.searchByOracleIdAndLanguage(originalCard?.oracleId, langOutput)
            } finally {
                _isSearchingCard.value = false
            }
        }
    }

    /**
     * Extracts and maps all distinct mana symbols present inside the card cost profile.
     *
     * @param card The specific card entity container structure to parse colors from.
     * @return A list containing matching [CardDetailBackground] theme configuration profiles.
     */
    fun extractAllColors(card: MagicCard?): List<CardDetailBackground> {
        val manaCost = card?.manaCost.toString()

        val colors = manaCost
            .filter { it in manas }
            .toSet()
            .map { manaChar ->
                when (manaChar) {
                    'W' -> CardDetailBackground.WHITE
                    'U' -> CardDetailBackground.BLUE
                    'B' -> CardDetailBackground.BLACK
                    'R' -> CardDetailBackground.RED
                    'G' -> CardDetailBackground.GREEN
                    else -> CardDetailBackground.UNCOLORED
                }
            }

        return colors.ifEmpty { listOf(CardDetailBackground.UNCOLORED) }
    }
}

/**
 * Factory class handler providing lifecycle initialization parameters for custom view-models.
 * Manages dependency injections manually across application execution segments.
 */
class ScryfallViewModelFactory(
    private val cardRepository: CardRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScryfallViewModel::class.java)) {
            return ScryfallViewModel(cardRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class assignment configuration requested")
    }
}
