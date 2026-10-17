package com.zaziapps.mtg_scanner.data.model

import android.graphics.RectF
import com.zaziapps.mtg_scanner.data.local.lang.CardTranslations
import com.zaziapps.mtg_scanner.data.states.RecognitionState

/**
 * UI-ready domain model representing a stabilized card track emitted by the tracking system.
 * Contains both computer vision tracking geometry and real-time localization state properties.
 *
 * @property id The persistent unique identifier assigned to this specific card instance across frames.
 * @property boundingBox The smoothed geometric coordinates mapping the card's position on the screen space.
 * @property confidence The latest prediction confidence score assigned to this target by the model inference block.
 * @property classId The categorical identifier indexing the classification format group of the detected target.
 */
data class TrackedCard(
    val id: Int,
    val boundingBox: RectF,
    val confidence: Float,
    val classId: Int,

    /** Count of consecutive frames where this specific track has been successfully matched and updated. */
    val detectionFrames: Int,

    /** Count of consecutive frames where this track was missing or went undetected by the camera feed. */
    val missedFrames: Int,

    /** Lifecycle flag indicating if the target has cleared the required consecutive frame threshold to prevent UI flickering. */
    val isStable: Boolean,

    /** Visibility flag indicating whether this track is fresh enough to be drawn onto the layout overlay layer. */
    val isVisible: Boolean,

    /** Lazy-loaded localization and language translation metadata pulled from the local dictionary cache. */
    var foundTranslation: CardTranslations? = null,

    /** The runtime state machine status managing the processing lifecycle phase of this card detection. */
    var recognitionState: RecognitionState = RecognitionState.TRACKING
)
