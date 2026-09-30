package com.zaziapps.mtg_scanner.analyzer

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Handles Optical Character Recognition (OCR) processing for card components.
 * Utilizes Google ML Kit Vision Text Recognition to extract textual information from images asynchronously.
 */
class TextAnalyzer {

    /**
     * Extracts the first structurally relevant text component found within the provided bitmap.
     *
     * @param bitmap The source image snippet containing the target text area.
     * @return The text content isolated from the first relevant text block line, or "none" if no text is resolved.
     */
    suspend fun firstRelevantTextExtraction(bitmap: Bitmap): String {
        // Initialize the standard ML Kit Text Recognizer client for Latin scripts.
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Convert the raw Android Bitmap into a compatible ML Kit InputImage data wrapper.
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // Suspend the current coroutine execution context and wrap the asynchronous task callback.
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    // Process raw multi-line vision outputs to capture the primary relevant element.
                    val resultText = getFirstRelevantTextFromResult(visionText)
                    continuation.resume(resultText)
                }
                .addOnFailureListener { e ->
                    // Log execution exception traces and recover the flow returning an empty placeholder status.
                    e.printStackTrace()
                    continuation.resume("none")
                }
        }
    }

    /**
     * Extracts the first isolated line structure from the collection of detected text blocks.
     *
     * @param result The structural hierarchical text object populated by the vision model engine.
     * @return A trimmed string representation of the topmost text line, or "none" if blocks are missing.
     */
    private fun getFirstRelevantTextFromResult(result: Text): String {
        // Enforce structural safety boundaries to prevent index out of bounds exceptions when no visual data is detected.
        if (result.textBlocks.isEmpty() || result.textBlocks[0].lines.isEmpty()) {
            return "none"
        }

        // Isolate, sanitize, and return the primary localized line element belonging to the initial block.
        return result.textBlocks[0].lines[0].text.trim()
    }
}
