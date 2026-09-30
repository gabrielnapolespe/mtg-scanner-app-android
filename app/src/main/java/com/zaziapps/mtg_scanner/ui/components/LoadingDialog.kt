package com.zaziapps.mtg_scanner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zaziapps.mtg_scanner.ui.themes.Black
import com.zaziapps.mtg_scanner.ui.themes.DarkRed
import com.zaziapps.mtg_scanner.ui.themes.Gold
import com.zaziapps.mtg_scanner.ui.themes.LightBrown

/**
 * A modal loading dialog layout element that blocks interaction during long-running background tasks.
 * Intercepts structural user gestures to maintain transaction state continuity until processing finishes.
 *
 * @param isLoading Operational gate controlling the visual display visibility of the dialog.
 * @param modifier Structural styling constraints delegated down to the container layout box.
 */
@Composable
fun LoadingDialog(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        // Instantiate a blocking overlay window, explicitly preventing unexpected dismissal events.
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = modifier
                    .size(110.dp)
                    .background(Black, shape = RoundedCornerShape(16.dp))
                    .border(BorderStroke(2.dp, Gold), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Render a stylized spinning progress indicator anchored to the middle of the frame box canvas.
                CircularProgressIndicator(
                    color = DarkRed,
                    trackColor = LightBrown.copy(alpha = 0.3f),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(50.dp)
                )
            }
        }
    }
}
