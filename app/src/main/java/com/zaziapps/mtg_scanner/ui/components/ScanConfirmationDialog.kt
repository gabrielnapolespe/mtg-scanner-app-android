package com.zaziapps.mtg_scanner.ui.components

import com.zaziapps.mtg_scanner.ui.themes.ManaFaction
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zaziapps.mtg_scanner.R

/**
 * A modal confirmation dialog interface element displayed immediately after a card frame capture event.
 * Blends visual aesthetic themes from opposing mana factions to create a split hybrid design layout.
 *
 * @param capturedBitmap The isolated high-resolution graphic slice of the scanned card to review.
 * @param leftFaction Theme data properties governing styling attributes for the left side and discard actions.
 * @param rightFaction Theme data properties governing styling attributes for the right side and confirmation actions.
 * @param onDismiss Invoked when the user cancels the confirmation flow to reject the captured item.
 * @param onScanClick Dispatcher callback triggering further ingestion, text extraction, or hashing routines on the bitmap.
 */
@Composable
fun ScanConfirmationDialog(
    capturedBitmap: Bitmap,
    leftFaction: ManaFaction,
    rightFaction: ManaFaction,
    onDismiss: () -> Unit,
    onScanClick: (Bitmap) -> Unit
) {
    // Cache text style definitions to optimize composition processing cycles across layout redrawing events.
    val leftButtonTextStyle = remember(leftFaction.fontFamily) {
        TextStyle(
            fontFamily = leftFaction.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
    }

    val rightButtonTextStyle = remember(rightFaction.fontFamily) {
        TextStyle(
            fontFamily = rightFaction.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
    }

    val dialogShape = leftFaction.componentShape

    // Formulate a horizontal linear gradient to merge boundary frame colors between the distinct styles.
    val hybridBorderBrush = remember(leftFaction.borderColor, rightFaction.borderColor) {
        Brush.horizontalGradient(colors = listOf(leftFaction.borderColor, rightFaction.borderColor))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(BorderStroke(2.dp, hybridBorderBrush), shape = dialogShape),
            shape = dialogShape,
            color = Color.Transparent,
            tonalElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {

                // Arrange graphical faction textures side by side to establish the background template structure.
                Row(modifier = Modifier.matchParentSize()) {
                    Image(
                        painter = painterResource(leftFaction.backgroundPattern),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Image(
                        painter = painterResource(rightFaction.backgroundPattern),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Place interactive elements over the split background layer while positioning components vertically.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Configure a bounded viewing container mirroring the vertical dimensional ratios of a standard trading card.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(5f / 7f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 2.dp,
                                color = rightFaction.accentColor.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = capturedBitmap.asImageBitmap(),
                            contentDescription = stringResource(id = R.string.card_preview_alt_text),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Align control interfaces side by side with consistent padding distributions.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Render the localized cancel option styled exclusively to match left faction parameters.
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = leftFaction.componentShape,
                            border = BorderStroke(1.5.dp, leftFaction.borderColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = leftFaction.titleColor
                            )
                        ) {
                            Text(
                                text = stringResource(id = R.string.discard_button_text).uppercase(),
                                style = leftButtonTextStyle
                            )
                        }

                        // Render the localized accept option styled exclusively to match right faction parameters.
                        Button(
                            onClick = { onScanClick(capturedBitmap) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = rightFaction.componentShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = rightFaction.accentColor,
                                contentColor = rightFaction.textColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.scan_button_text).uppercase(),
                                style = rightButtonTextStyle
                            )
                        }
                    }
                }
            }
        }
    }
}
