package com.zaziapps.mtg_scanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.zaziapps.mtg_scanner.data.model.MagicCard

/**
 * Renders an individual Magic: The Gathering card item optimized for a grid layout presentation.
 * It automatically handles compliant Scryfall API header requests and image download fallback behaviors.
 *
 * @param card The data model instance containing the card information and artwork URIs.
 * @param onCardClick Lambda expression triggered when this grid item is clicked.
 */
@Composable
fun GridCardItem(
    card: MagicCard,
    onCardClick: (MagicCard) -> Unit
) {
    val context = LocalContext.current
    val imageUrl = card.imageUris?.normal ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.714f)
            .clickable { onCardClick(card) },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        // Intercept missing visual asset allocations immediately to enforce title fallback renderings.
        if (imageUrl.isEmpty()) {
            FallbackCardName(cardName = card.name)
        } else {
            // Package secure header interceptors explicitly conforming to user-agent guidelines enforced by external data engines.
            val secureRequest = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .setHeader("User-Agent", "MTGScannerApp/1.0 (com.example.mtg_scanner)")
                    .setHeader("Accept", "image/webp,image/png,image/*;q=0.8")
                    .build()
            }

            // Execute network graphic retrieval using isolated multi-state rendering contexts to safely handle stream failures.
            SubcomposeAsyncImage(
                model = secureRequest,
                contentDescription = card.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    // Recover gracefully from asset retrieval disruptions or broken connection loops by displaying local metadata titles.
                    FallbackCardName(cardName = card.printedName)
                }
            )
        }
    }
}

/**
 * Internal component to render a subtle neutral background with the card name centered.
 *
 * @param cardName The localized or default name text string of the card to display.
 */
@Composable
private fun FallbackCardName(cardName: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (cardName != null) {
            Text(
                text = cardName,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
