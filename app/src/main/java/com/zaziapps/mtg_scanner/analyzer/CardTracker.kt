package com.zaziapps.mtg_scanner.analyzer

import android.graphics.RectF
import com.zaziapps.mtg_scanner.data.local.lang.CardTranslations
import com.zaziapps.mtg_scanner.data.model.TrackedCard
import com.zaziapps.mtg_scanner.data.states.RecognitionState
import kotlin.math.abs

/**
 * Tracks and stabilizes object detections across sequential video frames.
 * Reduces visual flickering and smooths spatial movement by managing the lifecycle
 * of detected bounding boxes.
 */
class CardTracker {

    private companion object {
        /** Minimum confidence score required to initialize a new tracking instance. */
        const val CONFIDENCE_THRESHOLD = 0.90f

        /** Maximum number of continuous frames a card can be missing before its track is terminated. */
        const val MAX_MISSED_FRAMES = 5

        /** Minimum consecutive frames required to consider a tracking instance stable. */
        const val MIN_DETECTION_FRAMES = 3

        /** Maximum distance tolerance between bounding box centers to consider them a match. */
        const val DISTANCE_THRESHOLD = 0.15f

        /** Minimum Intersection over Union (IoU) ratio required to merge a new detection into a track. */
        const val IOU_THRESHOLD = 0.50f

        /** Exponential smoothing interpolation weight applied to box updates (lower is smoother). */
        const val SMOOTHING_FACTOR = 0.35f

        /** Standard physical aspect ratio of a Magic: The Gathering card (63mm / 88mm). */
        const val MTG_ASPECT_RATIO = 0.716f

        /** Tolerance window percentage allowed for perspective distortion and camera angles. */
        const val ASPECT_RATIO_TOLERANCE = 0.50f
    }

    /**
     * Internal data structure representing the current tracking state lifecycle of an isolated card.
     */
    private data class TrackState(
        val id: Int,

        var boundingBox: RectF,
        var confidence: Float,
        var classId: Int,

        var detectionFrames: Int = 1,
        var missedFrames: Int = 0,

        var foundTranslation: CardTranslations? = null,

        /** New tracks default to CANDIDATE to prevent frame noise from causing false positives. */
        var recognitionState: RecognitionState = RecognitionState.CANDIDATE
    )

    /** Active pool of tracking instances maintained across frames. */
    private val tracks = mutableListOf<TrackState>()

    /** Monotonically increasing unique identifier counter assigned to new tracks. */
    private var idCounter = 0

    /**
     * Updates the persistent tracking pool with raw detections found in the current frame.
     * Computes optimal spatial matching, updates historical counts, and prunes dead tracks.
     *
     * @return A list of current active tracks converted into external immutable data structures.
     */
    fun update(
        newBoxes: List<RectF>,
        confidences: List<Float>,
        classes: List<Int>
    ): List<TrackedCard> {

        // Validate that structural dimensions match across all property vectors
        require(newBoxes.size == confidences.size)
        require(newBoxes.size == classes.size)

        val matchedTrackIds = mutableSetOf<Int>()

        // Process each fresh bounding box extraction sequentially
        for (i in newBoxes.indices) {

            val newBox = newBoxes[i]
            val confidence = confidences[i]
            val classId = classes[i]

            // Find the closest active track sharing the same classification label
            val match = tracks
                .asSequence()
                .filter { track ->
                    track.id !in matchedTrackIds &&
                            track.classId == classId
                }
                .minByOrNull { track ->
                    centerDistance(track.boundingBox, newBox)
                }

            // Verify both the centroid distance and bounding layout topology constraints match
            if (
                match != null &&
                centerDistance(match.boundingBox, newBox) < DISTANCE_THRESHOLD &&
                calculateIoU(match.boundingBox, newBox) >= IOU_THRESHOLD
            ) {
                // Match confirmed: Merge frame coordinates and increment stability metrics
                match.boundingBox = smoothBox(match.boundingBox, newBox)
                match.confidence = confidence
                match.classId = classId
                match.detectionFrames = (match.detectionFrames + 1).coerceAtMost(MIN_DETECTION_FRAMES)
                match.missedFrames = 0

                // Promote track state status once durability frame benchmarks are cleared
                if (match.recognitionState == RecognitionState.CANDIDATE &&
                    match.detectionFrames >= MIN_DETECTION_FRAMES) {
                    match.recognitionState = RecognitionState.TRACKING
                }

                matchedTrackIds.add(match.id)
            } else if (confidence > CONFIDENCE_THRESHOLD) {

                // Validate geometrical aspect logic to discard deformed shapes early
                if (isValidGeometry(newBox)) {
                    val newTrack = TrackState(
                        id = ++idCounter,
                        boundingBox = newBox,
                        confidence = confidence,
                        classId = classId,
                        recognitionState = RecognitionState.CANDIDATE
                    )
                    tracks.add(newTrack)
                    matchedTrackIds.add(newTrack.id)
                }
            }
        }

        // Increment structural decay counts for tracks skipped in this execution frame
        for (track in tracks) {
            if (track.id !in matchedTrackIds) {
                track.missedFrames++
            }
        }

        // Drop historical track instances that have dropped below visibility thresholds
        tracks.removeAll { track ->
            track.missedFrames > MAX_MISSED_FRAMES
        }

        // Transform internal objects into structured domain representation wrappers
        return tracks.map { it.toTrackedCard() }
    }

    /**
     * Checks if the aspect ratio of the bounding box matches standard card dimensions.
     * Evaluates both vertical and horizontal layouts, accounting for distortion tolerances.
     */
    private fun isValidGeometry(box: RectF): Boolean {
        val width = box.width()
        val height = box.height()

        // Guard against zero-area boundaries or initialization value errors
        if (width <= 0f || height <= 0f) return false

        val aspectRatio = width / height

        // Compute acceptable variance thresholds for vertical alignment
        val minRatio = MTG_ASPECT_RATIO * (1f - ASPECT_RATIO_TOLERANCE)
        val maxRatio = MTG_ASPECT_RATIO * (1f + ASPECT_RATIO_TOLERANCE)

        // Compute acceptable variance thresholds for inverted horizontal profiles
        val inverseMtgRatio = 1f / MTG_ASPECT_RATIO
        val minInverseRatio = inverseMtgRatio * (1f - ASPECT_RATIO_TOLERANCE)
        val maxInverseRatio = inverseMtgRatio * (1f + ASPECT_RATIO_TOLERANCE)

        // Evaluate orientation compliance metrics against limits
        val isVerticalCard = aspectRatio in minRatio..maxRatio
        val isHorizontalCard = aspectRatio in minInverseRatio..maxInverseRatio

        return isVerticalCard || isHorizontalCard
    }

    /**
     * Calculates the Intersection over Union (IoU) overlap coefficient between two boxes.
     */
    private fun calculateIoU(a: RectF, b: RectF): Float {
        val intersectionLeft = maxOf(a.left, b.left)
        val intersectionTop = maxOf(a.top, b.top)
        val intersectionRight = minOf(a.right, b.right)
        val intersectionBottom = minOf(a.bottom, b.bottom)

        val intersectionWidth = intersectionRight - intersectionLeft
        val intersectionHeight = intersectionBottom - intersectionTop

        if (intersectionWidth <= 0f || intersectionHeight <= 0f) {
            return 0.0f
        }
        val intersectionArea = intersectionWidth * intersectionHeight

        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)

        val unionArea = areaA + areaB - intersectionArea

        if (unionArea <= 0f) return 0.0f

        return intersectionArea / unionArea
    }

    /**
     * Evaluates the absolute maximum coordinate difference between the center points of two boxes.
     */
    private fun centerDistance(a: RectF, b: RectF): Float {
        val dx = abs(a.centerX() - b.centerX())
        val dy = abs(a.centerY() - b.centerY())
        return maxOf(dx, dy)
    }

    /**
     * Smooths structural box jitter over time via Linear Interpolation (LERP).
     */
    private fun smoothBox(old: RectF, new: RectF): RectF {
        return RectF(
            old.left + (new.left - old.left) * SMOOTHING_FACTOR,
            old.top + (new.top - old.top) * SMOOTHING_FACTOR,
            old.right + (new.right - old.right) * SMOOTHING_FACTOR,
            old.bottom + (new.bottom - old.bottom) * SMOOTHING_FACTOR
        )
    }

    /**
     * Factory mapping method translating persistent track internals into immutable object outputs.
     */
    private fun TrackState.toTrackedCard(): TrackedCard {
        return TrackedCard(
            id = id,
            boundingBox = boundingBox,
            confidence = confidence,
            classId = classId,
            detectionFrames = detectionFrames,
            missedFrames = missedFrames,
            isStable = detectionFrames >= MIN_DETECTION_FRAMES,
            isVisible = missedFrames <= MAX_MISSED_FRAMES,
            foundTranslation = foundTranslation,
            recognitionState = recognitionState
        )
    }

    /**
     * Updates an active track with its localized database translation match and state flag.
     * Returns true if the target identifier was matched and updated successfully.
     */
    fun updateRecognition(
        trackId: Int,
        translation: CardTranslations?,
        state: RecognitionState
    ): Boolean {
        val track = tracks.find { it.id == trackId } ?: return false
        track.foundTranslation = translation
        track.recognitionState = state
        return true
    }

    /**
     * Directly transitions the processing status value of an explicit tracking instance.
     * Returns true if the target identifier was matched and updated successfully.
     */
    fun updateRecognitionState(
        trackId: Int,
        state: RecognitionState
    ): Boolean {
        val track = tracks.find { it.id == trackId } ?: return false
        track.recognitionState = state
        return true
    }
}
