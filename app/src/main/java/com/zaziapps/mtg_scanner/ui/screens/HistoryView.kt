package com.zaziapps.mtg_scanner.ui.screens

import com.zaziapps.mtg_scanner.ui.components.HistoryCardItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaziapps.mtg_scanner.data.local.scanned.ScannedCardDao
import com.zaziapps.mtg_scanner.ui.themes.Gold
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.ui.res.stringResource
import com.zaziapps.mtg_scanner.R
import com.zaziapps.mtg_scanner.data.local.scanned.ScannedCard

// Consistent definition of the available sorting options
enum class OrderOption {
    NEWEST,
    OLDEST,
    NAME
}

/**
 * Screen that displays a list of all previously scanned Magic cards.
 * Reads data directly from Room and updates automatically on changes.
 *
 * @param scannedCardDao The Data Access Object used to fetch card history from Room database.
 * @param onMenuClick Callback triggered when the top navigation menu button is clicked.
 * @param onCardClick Callback triggered when a specific card item is selected from the list.
 */
@Composable
fun HistoryView(
    scannedCardDao: ScannedCardDao,
    onMenuClick: () -> Unit,
    onCardClick: (ScannedCard) -> Unit
) {
    // Observes the database Flow as a Composable state, defaulting to an empty list
    val cardsHistory by scannedCardDao.getHistory().collectAsState(initial = emptyList())

    // State management for sorting preferences and menu visibility
    var currentOrder by remember { mutableStateOf(OrderOption.NEWEST) }
    var showMenu by remember { mutableStateOf(false) }

    // Reactive sorting and deduplication logic tied to the selected OrderOption
    val sortedCards = remember(cardsHistory, currentOrder) {
        // 1. Group by name and keep ONLY the entry with the most recent scanDate to avoid duplicates
        val uniqueCards = cardsHistory
            .sortedByDescending { it.scanDate } // Ensures the first matching item found is the newest one
            .distinctBy { it.name }             // Filters out duplicates based on the card name

        // 2. Sort the deduplicated list according to the user's selected preference
        when (currentOrder) {
            OrderOption.NEWEST -> uniqueCards // Already sorted by descending date
            OrderOption.OLDEST -> uniqueCards.sortedBy { it.scanDate }
            OrderOption.NAME -> uniqueCards.sortedBy { it.name }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image and dark tint overlay...

        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

            // Top action bar for the history screen
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.open_menu_icon_content_desc),
                        tint = Gold,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.card_history_nav_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Gold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )

                // Sorting button and synchronized dropdown selection menu
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort_cards_icon_content_desc),
                            tint = Gold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.newest_first_dropdown_option)) },
                            onClick = {
                                currentOrder = OrderOption.NEWEST
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.oldest_first_dropdown_option)) },
                            onClick = {
                                currentOrder = OrderOption.OLDEST
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.by_name_dropdown_option)) },
                            onClick = {
                                currentOrder = OrderOption.NAME
                                showMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Renders either an empty state placeholder or the scrollable list
            if (sortedCards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_scanned_card_history_text), color = Color.LightGray)
                }
            } else {
                // Efficient scrolling container for the database records
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Rendering list items using the reactively filtered and sorted data stream
                    items(sortedCards) { card ->
                        HistoryCardItem(card = card, onClick = { onCardClick(card) })
                    }
                }
            }
        }
    }
}
