package com.zaziapps.mtg_scanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zaziapps.mtg_scanner.data.model.CustomCollection
import com.zaziapps.mtg_scanner.data.model.MagicCard
import com.zaziapps.mtg_scanner.data.repository.CardRepository
import com.zaziapps.mtg_scanner.ui.ScanScreen
import com.zaziapps.mtg_scanner.ui.themes.CardDetailBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the application's thematic UI state and screen navigation flows.
 * Coordinates layout mutations and expansion set updates by synchronizing screen state flows.
 *
 * @property scryfallViewModel Core business layer view-model utilized to parse card color identities.
 */
class ThemeViewModel(
    private val scryfallViewModel: ScryfallViewModel,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _currentManaList = MutableStateFlow<List<CardDetailBackground>>(listOf(CardDetailBackground.UNCOLORED))
    val currentManaList = _currentManaList.asStateFlow()

    private val _currentScreen = MutableStateFlow(ScanScreen.SCAN_CAM)
    val currentScreen = _currentScreen.asStateFlow()

    private val _selectedCard = MutableStateFlow<MagicCard?>(null)
    val selectedCard = _selectedCard.asStateFlow()

    private val _collectionUiState = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val collectionUiState: StateFlow<CollectionUiState> = _collectionUiState

    @OptIn(ExperimentalCoroutinesApi::class)
    val customCollections: StateFlow<List<CustomCollection>> = cardRepository.getAllCustomCollections()
        .flatMapLatest { entities ->
            val countFlows = entities.map { entity ->
                cardRepository.getCardCountForCollection(entity.id).map { count ->
                    CustomCollection(
                        id = entity.id,
                        name = entity.name,
                        description = entity.description,
                        cardCount = count
                    )
                }
            }

            if (countFlows.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                combine(countFlows) { it.toList() }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createCollection(name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = java.util.UUID.randomUUID().toString()
            cardRepository.createNewCollection(newId, name, description)
        }
    }

    fun loadCustomCollection(collectionId: String) {
        viewModelScope.launch {
            _collectionUiState.value = CollectionUiState.Loading
            try {
                val cards = cardRepository.getCardsFromCustomCollection(collectionId)
                _collectionUiState.value = CollectionUiState.Success(cards)
            } catch (e: Exception) {
                _collectionUiState.value = CollectionUiState.Error(e.localizedMessage ?: "Error al cargar la colección local.")
            }
        }
    }

    /**
     * Extracts card color combinations to update the background theme and navigates directly into the details interface.
     *
     * @param card The specific [MagicCard] structure model instance to map onto the layout screen.
     */
    fun showCardDetail(card: MagicCard?) {
        // Extract the complete color array profiles out of the card mechanical structures to update the backdrop fields.
        _currentManaList.value = scryfallViewModel.extractAllColors(card)
        _selectedCard.value = card
        _currentScreen.value = ScanScreen.CARD_DETAIL
    }

    /**
     * Loads all card components belonging to a designated set expansion using the targeted language parameter.
     *
     * @param setCode The unique 3 or 4 letter expansion/set identifier short code.
     * @param lang The localization language code filter used to restrict matching result data. java.util.Locale.getDefault().language
     */
    fun loadCollection(setCode: String, lang: String = java.util.Locale.getDefault().language) {
        viewModelScope.launch {
            // Emplace a loading status token onto the interface state channel to inform progress indicators.
            _collectionUiState.value = CollectionUiState.Loading
            try {
                // Delegate collection retrieval tasks over to underlying repository processing pipelines.
                val cards = scryfallViewModel.searchCollection(setCode, lang)

                if (cards.isNotEmpty()) {
                    _collectionUiState.value = CollectionUiState.Success(cards)
                } else {
                    _collectionUiState.value = CollectionUiState.Error("No cards were found matching this language specification.")
                }
            } catch (e: Exception) {
                _collectionUiState.value = CollectionUiState.Error(e.localizedMessage ?: "An error occurred while attempting to load the collection stream.")
            }
        }
    }

    fun navigateToCustomCollectionsList() {
        _currentScreen.value = ScanScreen.CUSTOM_COLLECTIONS_LIST
    }

    fun navigateToCustomCollectionDetail() {
        _currentScreen.value = ScanScreen.CUSTOM_COLLECTION_DETAIL
    }

    fun addCardToCollection(collectionId: String, card: MagicCard) {
        viewModelScope.launch(Dispatchers.IO) {
            cardRepository.addCardToCustomCollection(collectionId, card)
        }
    }

    /**
     * Resets active theme configurations back to uncolored defaults and routes layouts to the primary camera scanner view.
     */
    fun returnToScanning() {
        _currentManaList.value = listOf(CardDetailBackground.UNCOLORED)
        _selectedCard.value = null
        _currentScreen.value = ScanScreen.SCAN_CAM
    }

    /**
     * Navigates the viewport flow state channel directly into the historical scanning tracking records layout screen.
     */
    fun navigateToHistory() {
        _currentScreen.value = ScanScreen.CARD_HISTORY
    }

    /**
     * Routes active screen configuration state definitions into the broad collection inventory grid display interface.
     */
    fun navigateToCollection() {
        _currentScreen.value = ScanScreen.COLLECTION_VIEW
    }
}

/**
 * Factory class handler providing lifecycle initialization parameters for the custom theme state controller.
 * Manages cross-viewmodel property tracking boundaries inside the operational environment layer.
 */
class ThemeViewModelFactory(
    private val scryfallViewModel: ScryfallViewModel,
    private val cardRepository: CardRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            return ThemeViewModel(scryfallViewModel, cardRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class assignment configuration requested")
    }
}