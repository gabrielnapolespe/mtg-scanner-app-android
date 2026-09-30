package com.zaziapps.mtg_scanner.ui.screens

import com.zaziapps.mtg_scanner.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.zaziapps.mtg_scanner.data.model.LanguageItem
import com.zaziapps.mtg_scanner.data.model.MagicCard
import com.zaziapps.mtg_scanner.data.model.rememberAvailableLanguages
import com.zaziapps.mtg_scanner.ui.components.LanguageSelector
import com.zaziapps.mtg_scanner.ui.themes.CardDetailBackground
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import com.zaziapps.mtg_scanner.ui.components.LoadingDialog
import androidx.compose.ui.graphics.Brush


/**
 * Background layout wrapper optimized for Magic: The Gathering thematic colors.
 * Renders a multidimensional cinematic background using combined linear gradients and shader overlays.
 *
 * @param manaColors Collection of background profile themes matching the active card color identities.
 * @param content Nested visual components to render inside the graphic wrapper.
 */
@Composable
fun BackgroundMTG(
    manaColors: List<CardDetailBackground>,
    content: @Composable () -> Unit
) {
    // Generate a foundational color gradient by smoothing transitions for multi-color configurations.
    val baseGradient = remember(manaColors) {
        if (manaColors.size == 1) {
            manaColors.first().gradient
        } else {
            val combinedColors = manaColors.map { it.mainColor }
            val finalColors = if (combinedColors.size == 2) {
                listOf(combinedColors[0], combinedColors[0], combinedColors[1], combinedColors[1])
            } else {
                combinedColors
            }

            Brush.linearGradient(
                colors = finalColors,
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }

    // Initialize a dark radial vignette overlay brush to simulate contextual canvas depth.
    val vignetteBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0xAA000000)),
            center = Offset.Unspecified,
            radius = Float.POSITIVE_INFINITY
        )
    }

    // Configure a specular metallic reflection gradient to replicate standard trading card foil visual patterns.
    val foilBrush = remember {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to Color(0x00FFFFFF),
                0.35f to Color(0x00FFFFFF),
                0.25f to Color(0x25FFFFFF),
                0.65f to Color(0x00FFFFFF),
                1.0f to Color(0x00FFFFFF)
            ),
            start = Offset(0f, Float.POSITIVE_INFINITY),
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseGradient)
            .drawWithContent {
                // Render the primary underlying layout content before compiling graphical layer modifications.
                drawContent()

                // Overlay spatial depth treatments onto the canvas rendering layer using multiplicative pixel values.
                drawRect(
                    brush = vignetteBrush,
                    blendMode = BlendMode.Multiply
                )

                // Blend light refraction highlights directly over base visuals using screen blending operations.
                drawRect(
                    brush = foilBrush,
                    blendMode = BlendMode.Screen
                )
            }
    ) {
        content()
    }
}


/**
 * Container screen to display detailed information and image layout of a specific Magic: The Gathering card.
 * Reactively triggers a language update whenever a new language item selection is made.
 *
 * @param card MagicCard? | The card object containing image URLs and Scryfall metadata
 * @param collectionCards List<MagicCard> | The list of cards currently in the user's collection for the associated set
 * @param selectedMana CardDetailBackground | Dynamic background theme definition based on the card's mana identity
 * @param onMenuClick () -> Unit | Lambda expression triggered when the user opens the side navigation menu
 * @param isCardLoading Boolean | Flag indicating whether the card details or images are currently loading
 * @param onCardSelected (MagicCard) -> Unit | Lambda expression triggered when a card from the collection list is selected
 * @param onViewCollectionClick (String) -> Unit | Lambda expression triggered to navigate to the set collection view
 * @param onBackButtonClick () -> Unit | Lambda expression triggered to return to the scanning screen sequence
 * @param onLanguageChanged (String, String) -> Unit | Lambda expression reactively triggered to fetch the card in a new language code (receives language code and card ID)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreenContainer(
    card: MagicCard?,
    collectionCards: List<MagicCard> = emptyList(),
    selectedManaList: List<CardDetailBackground>,
    onMenuClick: () -> Unit,
    isCardLoading: Boolean,
    onCardSelected: (MagicCard) -> Unit,
    onViewCollectionClick: (String) -> Unit,
    onBackButtonClick: () -> Unit,
    onLanguageChanged: (String, String) -> Unit
) {
    val context = LocalContext.current

    // Dynamic local state to control the selected language code (defaulting to current card's language)
    var selectedLangCode by remember(card?.id) { mutableStateOf(card?.lang ?: "en") }

    val availableLanguages = rememberAvailableLanguages()

    // Reactively monitors selectedLangCode and triggers a query request only if it differs from the active card's language
    LaunchedEffect(selectedLangCode) {
        if (card != null && selectedLangCode != card.lang) {
            onLanguageChanged(selectedLangCode, card.set ?: "")
        }
    }


    // Look for the complete object that matches the selected code
    val currentLanguageItem = availableLanguages.find { it.code == selectedLangCode }
        ?: LanguageItem(selectedLangCode, selectedLangCode.uppercase())

    // Calculate boundary indices if collection context data is populated

    // Find the exact card in the collection list (checks ID first, then Oracle ID for translations)
    val currentCardIndex = remember(card?.id, collectionCards) {
        if (card == null || collectionCards.isEmpty()) -1
        else {
            val directIndex = collectionCards.indexOfFirst { it.id == card.id }
            if (directIndex >= 0) directIndex
            else collectionCards.indexOfFirst { it.oracleId == card.oracleId } // Oracle ID fallback for translation stability
        }
    }

    // Crucial change to ensure 100% data consistency
    // We look up the card directly from the stable collection list, using external 'card' only as a fallback
    val validatedCard = remember(currentCardIndex, collectionCards, card) {
        if (currentCardIndex >= 0 && currentCardIndex < collectionCards.size) {
            collectionCards[currentCardIndex]
        } else {
            card
        }
    }


    val hasPreviousCard = currentCardIndex > 0
    val hasNextCard = collectionCards.isNotEmpty() && currentCardIndex >= 0 && currentCardIndex < collectionCards.lastIndex

    // Added 'key' parameter to anchor and force state recreation whenever the active card instance shifts
    val pagerState = androidx.compose.runtime.key(collectionCards.size) {
        rememberPagerState(
            initialPage = if (currentCardIndex >= 0) currentCardIndex else 0,
            pageCount = { collectionCards.size.coerceAtLeast(1) }
        )
    }


    // Synchronize pager swipe gesture ONLY if the user actually swiped a valid populated list
    LaunchedEffect(pagerState.currentPage, collectionCards) {
        if (collectionCards.isNotEmpty() &&
            currentCardIndex >= 0 &&
            pagerState.currentPage != currentCardIndex &&
            pagerState.currentPage < collectionCards.size
        ) {
            onCardSelected(collectionCards[pagerState.currentPage])
        }
    }

    // Force the pager to strictly snap back to the correct card index as soon as the collection list updates
    LaunchedEffect(currentCardIndex, collectionCards) {
        if (collectionCards.isNotEmpty() && currentCardIndex >= 0 && pagerState.currentPage != currentCardIndex) {
            pagerState.scrollToPage(currentCardIndex)
        }
    }

    BackgroundMTG(manaColors = selectedManaList) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Top action bar containing the menu button and screen title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Menu",
                        tint = selectedManaList.first().textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.card_details_nav_title).uppercase(),
                    color = selectedManaList.first().textColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            // Integrated custom LanguageSelector component
            LanguageSelector(
                selectedLanguage = currentLanguageItem,
                languages = availableLanguages,
                onLanguageSelected = { newLang ->
                    selectedLangCode = newLang.code // State change automatically satisfies and fires the LaunchedEffect block above
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // Container Layer with built-in HorizontalPager and overlayed navigation arrows
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // HorizontalPager handles the swipe gesture layout logic smoothly
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) { page ->
                    // Resolve the card instance for the current page context safely
                    val pagerCard = collectionCards.getOrNull(page) ?: validatedCard
                    val pagerImageUrl = pagerCard?.imageUris?.normal

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(0.71f)
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pagerImageUrl != null) {
                            val secureRequest = remember(pagerImageUrl) {
                                ImageRequest.Builder(context)
                                    .data(pagerImageUrl)
                                    .crossfade(true)
                                    .setHeader("User-Agent", "MTGScannerApp/1.0 (com.example.mtg_scanner)")
                                    .setHeader("Accept", "image/webp,image/png,image/*;q=0.8")
                                    .build()
                            }

                            SubcomposeAsyncImage(
                                model = secureRequest,
                                contentDescription = stringResource(id = R.string.card_ilustration_alt_text),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = selectedManaList.first().textColor, modifier = Modifier.size(40.dp))
                                    }
                                },
                                error = {
                                    CardFallbackDataContainer(card = pagerCard, selectedMana = selectedManaList.first())
                                }
                            )
                        } else {
                            CardFallbackDataContainer(card = pagerCard, selectedMana = selectedManaList.first())
                        }
                    }
                }

                // Displays only if a valid index exists behind the current location
                if (hasPreviousCard) {
                    IconButton(
                        onClick = { onCardSelected(collectionCards[currentCardIndex - 1]) },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .shadow(4.dp, RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Card",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Right Arrow Action Node: Displays only if a valid forward index exists within collection limits
                if (hasNextCard) {
                    IconButton(
                        onClick = { onCardSelected(collectionCards[currentCardIndex + 1]) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .shadow(4.dp, RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Card",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // Scryfall API data identifier logs
                Text(
                    text = "Scryfall ID: ${validatedCard?.id ?: "---"}",
                    color = selectedManaList.first().textColor.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Button 1: Shows collection card list view (CollectionScreen)
                Button(
                    onClick = { validatedCard?.set?.let { setCode -> onViewCollectionClick(setCode) } },
                    enabled = validatedCard?.set != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedManaList.first().textColor.copy(alpha = 0.15f),
                        contentColor = selectedManaList.first().textColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.see_collection_button_text) + ": (${validatedCard?.set?.uppercase() ?: "---"})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Button 1: Redirects to scan cards view (CameraView)
                Button(
                    onClick = onBackButtonClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedManaList.first().textColor,
                        contentColor = if (selectedManaList.first() == CardDetailBackground.WHITE) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.scan_button_text),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
        LoadingDialog(isLoading = isCardLoading)
    }
}

/**
 * Renders the readable descriptive layout text and properties of an MTG card when its official artwork illustration cannot be loaded.
 * @param card MagicCard? | Target data structures containing properties to frame inside the text box
 * @param selectedMana CardDetailBackground | Active theme context references used to paint boundaries and typography layers
 */
@Composable
fun CardFallbackDataContainer(
    card: MagicCard?,
    selectedMana: CardDetailBackground
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(0.71f)
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .border(1.dp, selectedMana.textColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Card Title/Printed Name and Mana Cost Profile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card?.printedName ?: card?.name ?: "Unknown Card",
                    color = selectedMana.textColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                card?.manaCost?.let { cost ->
                    Text(
                        text = cost,
                        color = selectedMana.textColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            HorizontalDivider(color = selectedMana.textColor.copy(alpha = 0.2f))

            // Section 2: Card Type Line Properties
            Text(
                text = card?.printedTypeLine ?: card?.typeLine ?: "---",
                color = selectedMana.textColor.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            HorizontalDivider(color = selectedMana.textColor.copy(alpha = 0.2f))

            // Section 3: Scrollable Card Abilities Text Layout Boundary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = card?.printedText ?: card?.oracleText ?: stringResource(R.string.error_download_art_text),
                    color = selectedMana.textColor.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = if (card?.oracleText == null && card?.printedText == null) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.align(if (card?.oracleText == null && card?.printedText == null) Alignment.Center else Alignment.TopStart)
                )
            }

            // Footer Layer inside the box: Rarity status log
            card?.rarity?.let { rarityStatus ->
                Text(
                    text = rarityStatus.uppercase(),
                    color = selectedMana.textColor.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
