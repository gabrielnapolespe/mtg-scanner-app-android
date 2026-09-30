package com.zaziapps.mtg_scanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaziapps.mtg_scanner.R
import com.zaziapps.mtg_scanner.data.model.LanguageItem
import com.zaziapps.mtg_scanner.data.model.MagicCard
import com.zaziapps.mtg_scanner.data.model.rememberAvailableLanguages
import com.zaziapps.mtg_scanner.ui.viewmodel.CollectionUiState
import com.zaziapps.mtg_scanner.ui.viewmodel.ScryfallViewModel
import com.zaziapps.mtg_scanner.ui.components.GridCardItem
import com.zaziapps.mtg_scanner.ui.components.LanguageSelector
import com.zaziapps.mtg_scanner.ui.viewmodel.ThemeViewModel

/**
 * Screen to display all Magic: The Gathering cards belonging to the same set in a 3-column grid layout,
 * featuring an integrated language selector filter.
 * @param setCode String | The unique 3 or 4 letter expansion/set identifier code (e.g., "NEO")
 * @param lang String | The initial language code filter for the returned set cards - Default = "en"
 * @param viewModel ScryfallViewModel | The state holder responsible for calling the Scryfall API data stream
 * @param onCardClick (MagicCard) -> Unit | Lambda expression triggered when a user clicks an individual card from the grid
 * @param onBackClick () -> Unit | Lambda expression triggered to pop the current screen off the navigation stack
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    setCode: String,
    lang: String = "en",
    viewModel: ThemeViewModel,
    onCardClick: (MagicCard) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.collectionUiState.collectAsState()

    // Dynamic state for the active language code
    var selectedLangCode by remember { mutableStateOf(lang) }

    val availableLanguages = rememberAvailableLanguages()

    // Look for the complete object that matches the selected code
    val currentLanguageItem = availableLanguages.find { it.code == selectedLangCode }
        ?: LanguageItem(selectedLangCode, selectedLangCode.uppercase())

    // Triggers every time setCode or selectedLangCode changes
    LaunchedEffect(setCode, selectedLangCode) {
        viewModel.loadCollection(setCode, selectedLangCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.collection_text) + ": " + setCode.uppercase()) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Arrange the Selector and the card content vertically
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Clean implementation of your custom component
            LanguageSelector(
                selectedLanguage = currentLanguageItem,
                languages = availableLanguages,
                onLanguageSelected = { newLangCode ->
                    selectedLangCode = newLangCode.code // Updates the state and triggers the LaunchedEffect
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // Container for the card grid or loading states
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is CollectionUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is CollectionUiState.Success -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.cards) { card ->
                                GridCardItem(card = card, onCardClick = { onCardClick(card) })
                            }
                        }
                    }
                    is CollectionUiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}
