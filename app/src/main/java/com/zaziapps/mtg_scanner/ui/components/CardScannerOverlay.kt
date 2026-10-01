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
import com.zaziapps.mtg_scanner.data.model.CardDetection
import com.zaziapps.mtg_scanner.data.local.lang.getColorList
import com.zaziapps.mtg_scanner.ui.themes.Gold

/**
 * UI graphic overlay layer that draws animated, smoothed bounding boxes over tracked card items.
 * Computes spatial coordinate transformations to scale normalized camera boundaries onto the active display canvas.
 *
 * @param boundingBoxes The collection of incoming [CardDetection] targets identified in the current video frame.
 * @param modifier Structural styling or layout modifiers delegated down to the drawing canvas layer.
 */
@Composable
fun CardScannerOverlay(
    boundingBoxes: List<CardDetection>,
    onItemClick: (CardDetection) -> Unit,
    modifier: Modifier = Modifier
) {
    val smoothedBoxes = remember {
        mutableStateOf<Map<Int, CardDetection>>(emptyMap())
    }

    // Define the linear interpolation coefficient to regulate coordinate animation tracking speeds.
    val lerpFactor = 0.35f

    LaunchedEffect(boundingBoxes) {
        val currentMap = smoothedBoxes.value
        val newMap = mutableMapOf<Int, CardDetection>()

        boundingBoxes.forEach { target ->
            val previous = currentMap[target.id]

            if (previous != null) {
                // Apply a linear interpolation calculation to smooth out box tracking paths across spatial adjustments.
                val smoothedRect = RectF(
                    lerp(previous.boundingBox.left, target.boundingBox.left, lerpFactor),
                    lerp(previous.boundingBox.top, target.boundingBox.top, lerpFactor),
                    lerp(previous.boundingBox.right, target.boundingBox.right, lerpFactor),
                    lerp(previous.boundingBox.bottom, target.boundingBox.bottom, lerpFactor)
                )

                val copiedCard = target.copy(boundingBox = smoothedRect)

                // Retain historical metadata records manually across the target duplication pipeline process.
                copiedCard.foundTranslation = target.foundTranslation ?: previous.foundTranslation

                newMap[target.id] = copiedCard
            } else {
                newMap[target.id] = target
            }
        }

        smoothedBoxes.value = newMap
    }

    Canvas(modifier = modifier
        .pointerInput(smoothedBoxes.value) {
            detectTapGestures { tapOffset ->
                val canvasWidth = size.width.toFloat()
                val canvasHeight = size.height.toFloat()
                val cameraAspectRatio = 4f / 3f
                val widthScale = canvasWidth
                val heightScale = canvasHeight * cameraAspectRatio
                val yOffset = (canvasHeight - heightScale) / 2f

                smoothedBoxes.value.values.forEach { card ->
                    val rect = card.boundingBox

                    val boundaries = getUiBoundaries(rect, widthScale, heightScale, yOffset)
                    val leftBoundary = boundaries["left"] ?: 0f
                    val rightBoundary = boundaries["right"] ?: 0f
                    val topBoundary = boundaries["top"] ?: 0f
                    val bottomBoundary = boundaries["bottom"] ?: 0f

                    if (tapOffset.x in leftBoundary..rightBoundary && tapOffset.y in topBoundary..bottomBoundary) {
                        onItemClick(card)
                        return@detectTapGestures
                    }
                }
            }
        } ) {

        val canvasWidth = size.width
        val canvasHeight = size.height

        // Establish the expected baseline aspect ratio tracking standard camera sensor proportions.
        val cameraAspectRatio = 4f / 3f

        // Calculate absolute drawing scales accounting for inverted orientation distribution models.
        val widthScale = canvasWidth
        val heightScale = canvasHeight * cameraAspectRatio

        // Compute vertical shift variations required to center layout shapes post aspect correction.
        val yOffset = (canvasHeight - heightScale) / 2f

        smoothedBoxes.value.values.forEach { card ->
            val rect = card.boundingBox

            // Map and translate normalized relative float positions into absolute viewport drawing pixel boundaries.
            val boundaries = getUiBoundaries(rect, widthScale, heightScale, yOffset)
            val leftBoundary = boundaries["left"] ?: 0f
            val rightBoundary = boundaries["right"] ?: 0f
            val topBoundary = boundaries["top"] ?: 0f
            val bottomBoundary = boundaries["bottom"] ?: 0f

            if (rightBoundary > leftBoundary && bottomBoundary > topBoundary) {
                val topLeftOffset = Offset(leftBoundary, topBoundary)
                val rectSize = Size(rightBoundary - leftBoundary, bottomBoundary - topBoundary)

                // Obtain colors list
                val uiColors = getCardColors(card)

                when {
                    // If card has 2 colors, drawRoundRect is painted with brush
                    uiColors.size == 2 -> {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                0.0f to uiColors[0],
                                0.5f to uiColors[0],
                                0.5f to uiColors[1],
                                1.0f to uiColors[1],
                                startX = leftBoundary,
                                endX = rightBoundary
                            ),
                            topLeft = topLeftOffset,
                            size = rectSize,
                            cornerRadius = CornerRadius(24f, 24f),
                            style = Stroke(width = 6f)
                        )
                    }
                    // If card has 1 colors or none, drawRoundRect is painted normally using card color
                    uiColors.size <= 1 -> {
                        drawRoundRect(
                            color = uiColors.firstOrNull() ?: Color(0xFFE91E63), // If Card has none color, place loading color frame (Pink)
                            topLeft = topLeftOffset,
                            size = rectSize,
                            cornerRadius = CornerRadius(24f, 24f),
                            style = Stroke(width = 6f)
                        )
                    }
                    // If card has 3 colors or more, drawRoundRect is painted normally with Gold (0xFFD4AF37)
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
): Map<String, Float> {
    return mapOf(
        "left" to (1f - rect.bottom) * widthScale,
        "right" to (1f - rect.top) * widthScale,
        "top" to (rect.left * heightScale) + yOffset,
        "bottom" to (rect.right * heightScale) + yOffset
    )
}

/**
 * Maps the profile color codes of the cards into a list of Compose Color tokens.
 *
 * @param card The individual card track snapshot configuration to inspect.
 * @return A list of Compose Color tokens representing the card's color profile.
 */
private fun getCardColors(card: CardDetection): List<Color> {
    val colorCodes = card.foundTranslation
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
