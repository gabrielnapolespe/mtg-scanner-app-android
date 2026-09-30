package com.zaziapps.mtg_scanner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaziapps.mtg_scanner.R
import com.zaziapps.mtg_scanner.data.local.scanned.ScannedCard
import com.zaziapps.mtg_scanner.ui.themes.Brown
import com.zaziapps.mtg_scanner.ui.themes.DarkBrown
import com.zaziapps.mtg_scanner.ui.themes.Gold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Visual list component representing an individual historical entry item within the user's scan history.
 * Displays card artwork thumbnails alongside localization metadata records and date logging tokens.
 *
 * @param card Entity data framework populated from Room containing metadata attributes.
 * @param onClick Execution callback triggered when the user interacts with the list element.
 */
@Composable
fun HistoryCardItem(
    card: ScannedCard,
    onClick: () -> Unit
) {
    // Convert raw system timestamp millisecond configurations into highly readable localized string patterns.
    val formatedDate = remember(card.scanDate) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(card.scanDate))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = DarkBrown.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Brown)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stream remote visual assets asynchronously using clipped image frameworks scaled to standard layouts.
            AsyncImage(
                model = card.urlImage,
                contentDescription = card.name,
                modifier = Modifier
                    .size(60.dp, 84.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Arrange informational textual nodes vertically while establishing clear visual hierarchy limits.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = card.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.scanned_on_text) + ": $formatedDate",
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Render navigation signifiers to visually suggest deeper interactive drill-down options.
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View card details",
                tint = Gold,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
