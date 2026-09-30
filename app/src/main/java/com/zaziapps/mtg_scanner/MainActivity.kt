package com.zaziapps.mtg_scanner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zaziapps.mtg_scanner.ui.screens.CameraView
import com.zaziapps.mtg_scanner.ui.themes.MTGScannerTheme

import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.zaziapps.mtg_scanner.auth.AuthRepository
import com.zaziapps.mtg_scanner.data.local.AppDatabase
import com.zaziapps.mtg_scanner.data.local.scanned.ScannedCard
import com.zaziapps.mtg_scanner.data.repository.CardRepository
import com.zaziapps.mtg_scanner.ui.viewmodel.ScryfallViewModel
import com.zaziapps.mtg_scanner.ui.ScanScreen
import com.zaziapps.mtg_scanner.ui.screens.CollectionScreen
import com.zaziapps.mtg_scanner.ui.screens.HistoryView
import com.zaziapps.mtg_scanner.ui.screens.MainScreenContainer
import com.zaziapps.mtg_scanner.ui.themes.DarkBrown
import com.zaziapps.mtg_scanner.ui.themes.Gold
import com.zaziapps.mtg_scanner.ui.viewmodel.ThemeViewModel
import com.zaziapps.mtg_scanner.ui.viewmodel.ThemeViewModelFactory
import com.zaziapps.mtg_scanner.ui.viewmodel.CollectionUiState
import com.zaziapps.mtg_scanner.ui.viewmodel.ScryfallViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.getValue

/**
 * Main Activity of the application, serving as the single entry point.
 * It manages the root UI structure using Jetpack Compose and triggers the initial authentication flow.
 */
class MainActivity : ComponentActivity() {


    // Lazy initialization of the local Room database instance
    private val database by lazy {
        AppDatabase.getDatabase(applicationContext)
    }
    // Lazy access to the database data access object
    private val scannedCardDao by lazy { database.scannedCardDao() }

    private val cardLangDao by lazy { database.cardLangDao() }
    private val authRepository by lazy { AuthRepository(applicationContext) }

    val cardRepository by lazy { CardRepository(cardLangDao) }

    private val scryfallViewModel: ScryfallViewModel by viewModels {
        ScryfallViewModelFactory(cardRepository)
    }
    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModelFactory(scryfallViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var targetSetCode by remember { mutableStateOf("") }
            MTGScannerTheme {
                val activeManas by themeViewModel.currentManaList.collectAsState()
                val activeScreen by themeViewModel.currentScreen.collectAsState()
                val activeCard by themeViewModel.selectedCard.collectAsState()
                val isSearchingCard by scryfallViewModel.isSearchingCard.collectAsState()

                // Global navigation drawer management at the Activity lifecycle level
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

                // Triggers an anonymous silent fallback login if no Firebase user session is currently active
                LaunchedEffect(Unit) {
                    if (FirebaseAuth.getInstance().currentUser == null) {
                        val result = authRepository.loginWithGoogle(this@MainActivity)
                        result.onSuccess { loggedIn ->
                            if (loggedIn) {
                                Toast.makeText(this@MainActivity, "OK: Temporary session started", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "ERROR: Guest mode active", Toast.LENGTH_LONG).show()
                            }
                        }.onFailure {
                            Toast.makeText(this@MainActivity, "Connection error. Guest mode active", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = DarkBrown,
                            modifier = Modifier.width(280.dp)
                        ) {
                            Spacer(modifier = Modifier.height(48.dp))

                            Text(
                                text = "MTG SCANNER",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )

                            HorizontalDivider(color = Gold.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Option 1: Navigate to the Card Scanner
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Gold) },
                                label = { Text("Card Scanner", color = Color.White) },
                                selected = activeScreen == ScanScreen.SCAN_CAM,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    themeViewModel.returnToScanning() // Clears states and routes back to SCAN_CAM
                                },
                                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            // Option 2: Navigate to the Card Scan History
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.History, contentDescription = null, tint = Gold) },
                                label = { Text("Scan History", color = Color.White) },
                                selected = activeScreen == ScanScreen.CARD_HISTORY,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    themeViewModel.navigateToHistory()
                                },
                                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                ) {
                    // Dynamic navigation content shell handled by crossfade transitions
                    Crossfade(
                        targetState = activeScreen,
                        animationSpec = tween(600),
                        label = "AppNavigation"
                    ) { screen ->
                        when (screen) {
                            ScanScreen.SCAN_CAM -> {
                                CameraView(
                                    onMenuClick = { coroutineScope.launch { drawerState.open() } }, // Handles menu open event
                                    onCardScanned = {card ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            try {
                                                val cardEntity = ScannedCard(
                                                    scryfallId = card.id,
                                                    name = card.printedName ?: "Unknown",
                                                    urlImage = card.imageUris?.normal ?: "",
                                                    magicCard = card
                                                )
                                                scannedCardDao.insertCardIntoHistory(cardEntity)

                                                // Once saved, show card detail UI
                                                launch(Dispatchers.Main) {
                                                    themeViewModel.showCardDetail(card)
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("RoomMainActivity", "Error saving scanning card", e)
                                            }
                                        }
                                    }
                                )
                            }
                            ScanScreen.CARD_DETAIL -> {
                                // Consume the current set collection state reactively from the Scryfall ViewModel
                                val collectionUiState by themeViewModel.collectionUiState.collectAsState()
                                val collectionList = if (collectionUiState is CollectionUiState.Success) {
                                    (collectionUiState as CollectionUiState.Success).cards
                                } else {
                                    emptyList()
                                }

                                MainScreenContainer(
                                    card = activeCard,
                                    collectionCards = collectionList, // Passes down the current set list for sequential pagination
                                    selectedManaList = activeManas,
                                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                    isCardLoading = isSearchingCard,
                                    onCardSelected = { newCard ->
                                        // Updates global application state and dynamically shifts the mana color theme mapping
                                        themeViewModel.showCardDetail(newCard)
                                    },
                                    onViewCollectionClick = { setCode ->
                                        targetSetCode = setCode
                                        themeViewModel.navigateToCollection()
                                    },
                                    onBackButtonClick = { themeViewModel.returnToScanning() },
                                    onLanguageChanged = { requestedLang, currentSetCode ->
                                        lifecycleScope.launch {
                                            activeCard?.name?.let { cardName ->
                                                // Trigger background collection pre-fetching operations smoothly
                                                if (collectionList.isNotEmpty()) {
                                                    themeViewModel.loadCollection(setCode = currentSetCode, lang = requestedLang)
                                                }

                                                // Always fetch individual translations alongside the background operations
                                                val translatedCard = scryfallViewModel.searchCardByName(
                                                    name = cardName,
                                                    langInput = activeCard!!.lang ?: "en",
                                                    langOutput = requestedLang
                                                )
                                                if (translatedCard != null) {
                                                    themeViewModel.showCardDetail(translatedCard)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            ScanScreen.COLLECTION_VIEW -> {
                                CollectionScreen(
                                    setCode = targetSetCode,
                                    lang = activeCard?.lang ?: "en",
                                    viewModel = themeViewModel,
                                    onBackClick = {
                                        themeViewModel.showCardDetail(activeCard)
                                    },
                                    onCardClick = { clickedCard ->
                                        // Extracts details to populate state and view the clicked card's data sheet
                                        themeViewModel.showCardDetail(clickedCard)
                                    }
                                )
                            }

                            ScanScreen.CARD_HISTORY -> {
                                HistoryView(
                                    scannedCardDao = scannedCardDao,
                                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                    onCardClick = { clickedCard ->
                                        // Extracts details to populate state and view the clicked card's data sheet
                                        themeViewModel.showCardDetail(clickedCard.magicCard)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes lifecycle teardown parameters when dismantling active resources.
     * Evaluates termination flags to wipe sensitive credential profiles when closing.
     */
    override fun onDestroy() {
        super.onDestroy()
        // Differentiate complete programmatic execution finalization runs from simple hardware rotation re-allocations.
        if (isFinishing) {
            authRepository.logout()
        }
    }
}
