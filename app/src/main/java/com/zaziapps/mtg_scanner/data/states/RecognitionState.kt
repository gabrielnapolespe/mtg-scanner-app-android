package com.zaziapps.mtg_scanner.data.states

/**
 * State machine enum defining the processing and tracking lifecycle phases of a detected card.
 * Orchestrates transitions between raw frame extraction, stabilization, data capture, and text resolution.
 */
enum class RecognitionState {
    /** The target is actively matched across frames and its geometric coordinates are being smoothed. */
    TRACKING,

    /** Initial probationary state assigned to new detections to filter out transient frame noise and false positives. */
    CANDIDATE,

    /** The tracking system has locked onto the target and high-resolution frame buffer assets have been captured. */
    CAPTURED,

    /** Asynchronous Optical Character Recognition (OCR) routines are currently sweeping the image crop for text strings. */
    OCR_RUNNING,

    /** The card entity has been successfully identified, translated, and matched against the local inventory database. */
    RESOLVED,

    /** The processing pipeline was unable to resolve or extract legible data from this track before it expired. */
    FAILED
}
