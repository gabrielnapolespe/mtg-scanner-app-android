package com.zaziapps.mtg_scanner.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zaziapps.mtg_scanner.data.model.CardDetection
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

/**
 * Custom image analyzer for real-time Magic: The Gathering card detection.
 * Implements the CameraX [ImageAnalysis.Analyzer] interface and utilizes a TensorFlow Lite model
 * to perform edge-based object inference.
 *
 * @property context The application context used to load the model asset.
 * @property onCardsDetected Callback executed when cards are detected and processed within the video stream.
 */
class CardAnalyzer(
    private val context: Context,
    private val onCardsDetected: (List<CardDetection>) -> Unit
) : ImageAnalysis.Analyzer {

    /**
     * TensorFlow Lite interpreter instance for model inference execution.
     */
    private val interpreter: Interpreter

    /**
     * Tracker component responsible for consolidating and maintaining detection persistence across frames.
     */
    private val cardTracker = CardTracker()

    /**
     * Image processor configured to resize and normalize the Bitmap before inference.
     */
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
        .add(org.tensorflow.lite.support.common.ops.NormalizeOp(127.5f, 127.5f))
        .build()

    /** Memory buffer to store bounding box coordinates returned by the model. */
    private val outputLocations = arrayOf(Array(10) { FloatArray(4) })

    /** Memory buffer to store class indices of the detected objects. */
    private val outputClasses = arrayOf(FloatArray(10))

    /** Memory buffer to store confidence scores of each detection. */
    private val outputScores = arrayOf(FloatArray(10))

    /** Memory buffer to store the total count of valid detected objects from inference. */
    private val numDetections = FloatArray(1)

    /**
     * Output map structure linking TFLite model output indices to their respective pre-allocated memory buffers.
     */
    private val outputMap = mapOf(
        0 to outputScores,
        1 to outputLocations,
        2 to numDetections,
        3 to outputClasses
    )

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, "model_tflite.tflite")
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(modelBuffer, options)
    }

    /**
     * Analyzes a camera frame in real-time using TensorFlow Lite inference.
     * Converts the frame into a bitmap, executes the model, filters results through the tracker,
     * and extracts isolated card image crops.
     *
     * @param imageProxy The CameraX image proxy container providing access to the hardware frame.
     */
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                // Extract the raw image frame from CameraX pipeline and convert it into a standard Bitmap object.
                val originalBitmap = imageProxy.toBitmap()

                // Initialize a TensorImage container compatible with TensorFlow Lite using FLOAT32 data type.
                val tensorImage = TensorImage(DataType.FLOAT32)
                tensorImage.load(originalBitmap)

                // Run the image through the pre-configured processor pipeline to handle resizing (320x320) and normalization.
                val processedImage = imageProcessor.process(tensorImage)
                val byteBufferInput = processedImage.buffer

                // Execute the machine learning model inference using the input buffer and map outputs directly to pre-allocated arrays.
                interpreter.runForMultipleInputsOutputs(arrayOf(byteBufferInput), outputMap)

                val tempBoxes = mutableListOf<RectF>()
                val tempConfidences = mutableListOf<Float>()
                val tempClasses = mutableListOf<Int>()

                // Determine the exact number of objects detected by rounding the raw output float value, capped at a maximum of 10.
                val objectCount = (numDetections[0] + 0.5f).toInt().coerceIn(0, 10)

                // Iterate through the valid detection arrays to filter out predictions that fall below the minimum confidence threshold.
                for (i in 0 until objectCount) {
                    val confidence = outputScores[0][i]
                    if (confidence > 0.65f) {
                        val box = outputLocations[0][i]

                        // Map the raw bounding box indices correctly to account for standard sensor orientation rotations.
                        val rectF = RectF(box[1], box[0], box[3], box[2])

                        tempBoxes.add(rectF)
                        tempConfidences.add(confidence)
                        tempClasses.add(outputClasses[0][i].toInt())
                    }
                }

                // Pass raw frame detections to the tracker to maintain temporal continuity and reduce UI bounding box flickering.
                val consolidatedCards = cardTracker.update(
                    tempBoxes,
                    tempConfidences,
                    tempClasses
                )

                // Map over the consolidated card tracks to generate localized graphic crops from the high-resolution original bitmap.
                val cardsWithBitmap = consolidatedCards.map { detection ->
                    val croppedBitmap = cropCard(
                        bitmap = originalBitmap,
                        normalizedBox = detection.boundingBox
                    )
                    detection.copy(
                        bitmap = croppedBitmap
                    )
                }

                // Dispatch the final list of fully structured, tracked card objects back to the primary UI or state layer.
                onCardsDetected(cardsWithBitmap)

            } catch (e: Exception) {
                Log.e("CardAnalyzer", "Inference error: ${e.message}")
            } finally {
                // Explicitly close the ImageProxy instance to release the hardware buffer lock and prevent pipeline stall.
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    /**
     * Crops a specific region from a Bitmap using normalized coordinates (0.0 to 1.0).
     *
     * @param bitmap The source high-resolution Bitmap.
     * @param normalizedBox RectF structure containing the relative boundaries of the detected card.
     * @return A new [Bitmap] containing only the isolated card area, or null if the calculated dimensions are invalid.
     */
    private fun cropCard(
        bitmap: Bitmap,
        normalizedBox: RectF
    ): Bitmap? {
        // Scale normalized relative boundaries back to the physical pixel width dimensions of the source image.
        val left = (normalizedBox.left * bitmap.width)
            .toInt()
            .coerceIn(0, bitmap.width - 1)

        // Scale normalized relative boundaries back to the physical pixel height dimensions of the source image.
        val top = (normalizedBox.top * bitmap.height)
            .toInt()
            .coerceIn(0, bitmap.height - 1)

        // Compute the right boundary point, ensuring it remains within image coordinates and greater than the left coordinate.
        val right = (normalizedBox.right * bitmap.width)
            .toInt()
            .coerceIn(left + 1, bitmap.width)

        // Compute the bottom boundary point, ensuring it remains within image coordinates and greater than the top coordinate.
        val bottom = (normalizedBox.bottom * bitmap.height)
            .toInt()
            .coerceIn(top + 1, bitmap.height)

        // Derive total bounding box width and height values from calculated absolute coordinate positions.
        val width = right - left
        val height = bottom - top

        // Validate structural integrity of dimensions to prevent execution errors during sub-bitmap generation.
        if (width <= 0 || height <= 0) {
            return null
        }

        // Create and return a newly isolated, hardware-backed sub-bitmap slice from the source element.
        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            width,
            height
        )
    }
}
