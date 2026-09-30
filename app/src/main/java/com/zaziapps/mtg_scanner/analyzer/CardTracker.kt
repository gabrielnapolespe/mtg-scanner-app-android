package com.zaziapps.mtg_scanner.analyzer

import android.graphics.RectF
import com.zaziapps.mtg_scanner.data.model.CardDetection
import kotlin.math.abs

/**
 * Tracks and stabilizes object detections across sequential video frames.
 * Reduces visual flickering and smooths spatial movement by managing the lifecycle
 * of detected bounding boxes.
 */
class CardTracker {

    /** Cache keeping track of active card detections from preceding frames. */
    private var currentDetections = mutableListOf<CardDetection>()

    /** Incremental counter utilized to assign unique structural identifiers to newly discovered cards. */
    private var idCounter = 0

    /** Maximum consecutive frames an object remains in memory without an active model match before being dropped. */
    private val maxFramesToLiveWithoutDetection = 5

    /** Minimum frame visibility threshold required before a card is considered verified for UI rendering. */
    private val minFramesToDisplay = 3

    /** Spatial boundary threshold representing the maximum allowed center-point deviation between frames (15% screen limit). */
    private val distanceThreshold = 0.15f

    /**
     * Updates the internal tracking state with new model predictions and returns a stabilized list of active cards.
     *
     * @param newBoxes Extracted boundary coordinates from the latest model inference pass.
     * @param confidences Certainty precision scores corresponding to each new bounding box.
     * @param classes Categorical classification indices matching the bounding boxes.
     * @return A filtered collection of [CardDetection] structures meeting the temporal persistence requirements.
     */
    fun update(newBoxes: List<RectF>, confidences: List<Float>, classes: List<Int>): List<CardDetection> {
        val updatedList = mutableListOf<CardDetection>()

        // Attempt to match newly reported predictions with tracking instances stored in memory.
        for (i in newBoxes.indices) {
            val newBox = newBoxes[i]
            val confidence = confidences[i]
            val classId = classes[i]

            // Look for an existing tracked object whose center-point lies within the spatial proximity threshold.
            val match = currentDetections.find { old ->
                val distanceX = abs(old.boundingBox.centerX() - newBox.centerX())
                val distanceY = abs(old.boundingBox.centerY() - newBox.centerY())
                distanceX < distanceThreshold && distanceY < distanceThreshold
            }

            if (match != null) {
                // Confirm tracking continuity: update spatial location, increment frame age, and retain the original identifier.
                match.framesAlive += 1
                updatedList.add(
                    CardDetection(
                        id = match.id,
                        boundingBox = newBox,
                        confidence = confidence,
                        classId = classId,
                        framesAlive = match.framesAlive
                    )
                )
                // Remove the matched element from the historical pool to isolate remaining unmatched tracks.
                currentDetections.remove(match)
            } else if (confidence > 0.75f) {
                // Initialize a brand-new track sequence for high-confidence predictions that lack historical overlap.
                idCounter++
                updatedList.add(CardDetection(idCounter, newBox, confidence, classId, framesAlive = 1))
            }
        }

        // Apply a visibility penalty to historical tracks that failed to map to any incoming model predictions.
        currentDetections.forEach { old ->
            old.framesAlive -= 1

            // Allow historical entries to persist blindly for a grace period to cushion erratic detection dropouts.
            if (old.framesAlive > -maxFramesToLiveWithoutDetection) {
                updatedList.add(old)
            }
        }

        currentDetections = updatedList

        // Isolate and export only consolidated card elements that successfully crossed the display requirement threshold.
        return currentDetections.filter { it.framesAlive >= minFramesToDisplay }
    }
}
