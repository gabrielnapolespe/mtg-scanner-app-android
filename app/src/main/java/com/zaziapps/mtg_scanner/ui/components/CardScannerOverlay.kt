package com.zaziapps.mtg_scanner.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.lerp
import com.zaziapps.mtg_scanner.data.model.CardDetection
import com.zaziapps.mtg_scanner.data.local.lang.getColorList

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

    Canvas(modifier = modifier) {
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
            val leftBoundary = (1f - rect.bottom) * widthScale
            val rightBoundary = (1f - rect.top) * widthScale
            val topBoundary = (rect.left * heightScale) + yOffset
            val bottomBoundary = (rect.right * heightScale) + yOffset

            if (rightBoundary > leftBoundary && bottomBoundary > topBoundary) {
                drawRoundRect(
                    color = getCardColor(card),
                    topLeft = Offset(leftBoundary, topBoundary),
                    size = Size(rightBoundary - leftBoundary, bottomBoundary - topBoundary),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 6f)
                )
            }
        }
    }
}

/**
 * Resolves a unique layout outline stroke color based on the mechanical identity properties parsed out of the translation instance.
 *
 * @param card The individual card track snapshot configuration to inspect.
 * @return A Compose color model token matching the card's color profile traits.
 */
private fun getCardColor(card: CardDetection): Color {
    val colors = card.foundTranslation
        ?.getColorList()
        ?: emptyList()

    return when {
        // Default color profile token assigned to colorless components or unmapped query datasets.
        colors.isEmpty() -> {
            Color(0xFFE91E63)
        }
        // Custom palette flag indicating complex multi-color entities.
        colors.size > 1 -> {
            Color(0xFFFFD700)
        }
        else -> {
            // Distribute distinct individual structural shading themes mapped to standard shorthand symbol codes.
            when (colors.first()) {
                "W" -> Color(0xFFFFF4D6)
                "U" -> Color(0xFF42A5F5)
                "B" -> Color(0xFF000000)
                "R" -> Color(0xFFEF5350)
                "G" -> Color(0xFF66BB6A)
                else -> Color(0xFF828283)
            }
        }
    }
}
