package com.zaziapps.mtg_scanner.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.lerp
import com.zaziapps.mtg_scanner.data.local.lang.CardTranslations
import com.zaziapps.mtg_scanner.data.model.CardDetection
import com.zaziapps.mtg_scanner.data.local.lang.getColorList
import com.zaziapps.mtg_scanner.data.model.TrackedCard
import com.zaziapps.mtg_scanner.data.states.RecognitionState
import com.zaziapps.mtg_scanner.ui.themes.Gold

/**
 * UI graphic overlay layer that draws animated, smoothed bounding boxes over tracked card items.
 * Computes spatial coordinate transformations to scale normalized camera boundaries onto the active display canvas.
 *
 * @param boundingBoxes The collection of incoming [TrackedCard] targets identified in the current video frame.
 * @param onItemClick Callback executed when a user taps within the geometric boundaries of a tracked card frame.
 * @param modifier Structural styling or layout modifiers delegated down to the drawing canvas layer.
 */
@Composable
fun CardScannerOverlay(
    boundingBoxes: List<TrackedCard>,
    modifier: Modifier = Modifier
) {
    // Local memory cache map holding stabilized box track data to prevent visual coordinate snapping
    val smoothedBoxes = remember {
        mutableStateOf<Map<Int, TrackedCard>>(emptyMap())
    }

    // Interpolation factor weight utilized to smooth out bounding box transformations across frame increments
    val lerpFactor = 0.35f

    // Recompute and linear-interpolate tracking changes whenever a fresh batch of items is emitted by the analyzer pipeline
    LaunchedEffect(boundingBoxes) {
        val currentMap = smoothedBoxes.value
        val newMap = mutableMapOf<Int, TrackedCard>()

        boundingBoxes.forEach { target ->
            val previous = currentMap[target.id]

            if (previous != null) {
                // Compute Linear Interpolation (LERP) coordinates across all four vector points of the box
                val smoothedRect = RectF(
                    lerp(previous.boundingBox.left, target.boundingBox.left, lerpFactor),
                    lerp(previous.boundingBox.top, target.boundingBox.top, lerpFactor),
                    lerp(previous.boundingBox.right, target.boundingBox.right, lerpFactor),
                    lerp(previous.boundingBox.bottom, target.boundingBox.bottom, lerpFactor)
                )

                // Clone entire metadata payload while merging the freshly smoothed geometric coordinates
                newMap[target.id] = target.copy(boundingBox = smoothedRect)
            } else {
                // Instantly append new track instances directly to the active map context to avoid missing frames
                newMap[target.id] = target
            }
        }
        smoothedBoxes.value = newMap
    }

    Canvas(
        modifier = modifier
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val cameraAspectRatio = 4f / 3f
        val widthScale = canvasWidth
        val heightScale = canvasHeight * cameraAspectRatio
        val yOffset = (canvasHeight - heightScale) / 2f

        // Draw structural frames onto the canvas view for each tracking record active in this loop iteration
        smoothedBoxes.value.values.forEach { card ->
            val rect = card.boundingBox
            val boundaries = getUiBoundaries(rect, widthScale, heightScale, yOffset)

            // Guard against negative boundaries, inverse dimensions, or corrupted frame geometry configurations
            if (boundaries.right > boundaries.left && boundaries.bottom > boundaries.top) {
                val topLeftOffset = Offset(boundaries.left, boundaries.top)
                val rectSize = Size(boundaries.right - boundaries.left, boundaries.bottom - boundaries.top)

                val uiColors = getCardColors(card.foundTranslation)

                when {
                    // Multicolor card profile containing exactly two identity flags -> Render balanced split linear gradient brush
                    uiColors.size == 2 -> {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                0.0f to uiColors[0],
                                0.5f to uiColors[0],
                                0.5f to uiColors[1],
                                1.0f to uiColors[1],
                                startX = boundaries.left,
                                endX = boundaries.right
                            ),
                            topLeft = topLeftOffset,
                            size = rectSize,
                            cornerRadius = CornerRadius(24f, 24f),
                            style = Stroke(width = 6f)
                        )
                    }
                    // Monocolor identity alignment -> Render unified solid tone structural frame
                    uiColors.size == 1 -> {
                        drawRoundRect(
                            color = uiColors.first(),
                            topLeft = topLeftOffset,
                            size = rectSize,
                            cornerRadius = CornerRadius(24f, 24f),
                            style = Stroke(width = 6f)
                        )
                    }
                    // Colorless identity profile or unparsed recognition data payload -> Delegate status colors based on execution phase
                    uiColors.isEmpty() -> {
                        val frameColor = if (card.recognitionState == RecognitionState.RESOLVED) {
                            Color(0xFF828283) // Colorless or land element structural gray
                        } else {
                            Color(0xFFE91E63) // Active system processing/loading indication pink
                        }

                        drawRoundRect(
                            color = frameColor,
                            topLeft = topLeftOffset,
                            size = rectSize,
                            cornerRadius = CornerRadius(24f, 24f),
                            style = Stroke(width = 6f)
                        )
                    }
                    // Hybrid multi-color identity (3 or more elements) -> Render high-tier premium Gold styling frame
                    else -> {
                        drawRoundRect(
                            color = Gold,
                            topLeft = topLeftOffset,
                            size = rectSize,
                            cornerRadius = CornerRadius(24f, 24f),
                            style = Stroke(width = 6f)
                        )
                    }
                }
            }
        }
    }
}



/**
 * Maps and rotates raw machine learning detection coordinates into scalable UI display boundaries.
 * Converts normalized sensor-space coordinates into an associative map of pixel-ready dimension tokens.
 *
 * @param rect The raw bounding box layout configuration from the model inference.
 * @param widthScale The calculated scaling factor applied to horizontal canvas dimensions.
 * @param heightScale The calculated scaling factor applied to vertical canvas dimensions.
 * @param yOffset The vertical spacing translation applied to center the viewfinder layer.
 * @return An associative map containing "left", "right", "top", and "bottom" coordinate tokens.
 */
private fun getUiBoundaries(
    rect: RectF,
    widthScale: Float,
    heightScale: Float,
    yOffset: Float
): RectF {
    return RectF(
        (1f - rect.bottom) * widthScale,
        (rect.left * heightScale) + yOffset,
        (1f - rect.top) * widthScale,
        (rect.right * heightScale) + yOffset
    )
}

/**
 * Maps the profile color codes of the cards into a list of Compose Color tokens.
 *
 * @param card The individual card track snapshot configuration to inspect.
 * @return A list of Compose Color tokens representing the card's color profile.
 */
private fun getCardColors(card: CardTranslations?): List<Color> {
    val colorCodes = card
        ?.getColorList()
        ?: return emptyList()

    return colorCodes.map { code ->
        when (code) {
            "W" -> Color(0xFFFFF4D6)
            "U" -> Color(0xFF42A5F5)
            "B" -> Color(0xFF000000)
            "R" -> Color(0xFFEF5350)
            "G" -> Color(0xFF66BB6A)
            else -> Color(0xFF828283)
        }
    }
}
