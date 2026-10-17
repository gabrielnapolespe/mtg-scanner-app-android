package com.zaziapps.mtg_scanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaziapps.mtg_scanner.data.model.LanguageItem
import com.zaziapps.mtg_scanner.data.model.MagicCard
import com.zaziapps.mtg_scanner.data.model.rememberAvailableLanguages
import com.zaziapps.mtg_scanner.ui.viewmodel.CollectionUiState
import com.zaziapps.mtg_scanner.ui.components.GridCardItem
import com.zaziapps.mtg_scanner.ui.components.LanguageSelector
import com.zaziapps.mtg_scanner.ui.viewmodel.ThemeViewModel


/**
 * Screen to display all Magic: The Gathering cards belonging to the same set in a 3-column grid layout,
 * featuring an integrated language selector filter.
 * @param title String | Title to show on the view
 * @param collectionId String | Collection identifier
 * @param isCustomCollection Boolean | Indicates if the collection is custom or official
 * @param lang String | The initial language code filter for the returned set cards - Default = "en"
 * @param viewModel ScryfallViewModel | The state holder responsible for calling the Scryfall API data stream
 * @param onCardClick (MagicCard) -> Unit | Lambda expression triggered when a user clicks an individual card from the grid
 * @param onBackClick () -> Unit | Lambda expression triggered to pop the current screen off the navigation stack
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    title: String,
    collectionId: String,
    isCustomCollection: Boolean = false,
    lang: String = "es",
    viewModel: ThemeViewModel,
    onCardClick: (MagicCard) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.collectionUiState.collectAsState()

    LaunchedEffect(collectionId) {
        if (!isCustomCollection) {
            viewModel.loadCollection(collectionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title.uppercase()) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {


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
                        if (state.cards.isEmpty()) {
                            Text(
                                text = "Esta colección está vacía.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
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
