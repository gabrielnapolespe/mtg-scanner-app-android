package com.zaziapps.mtg_scanner.analyzer

import android.content.Context
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zaziapps.mtg_scanner.data.model.TrackedCard
import com.zaziapps.mtg_scanner.tracker.CardTracker
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
 * @property cardTracker The card tracker object.
 * @property onCardsUpdated Callback executed when cards are detected and processed within the video stream.
 */
class CardAnalyzer(
    private val context: Context,
    private val cardTracker: CardTracker,
    private val onCardsUpdated: (List<TrackedCard>) -> Unit
) : ImageAnalysis.Analyzer {

    /**
     * TensorFlow Lite interpreter instance for model inference execution.
     */
    private val interpreter: Interpreter

    /**
     * Image processor configured to resize and normalize the Bitmap before inference.
     * Resizes the image to 320x320 pixels using bilinear interpolation and maps pixel values.
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
        // Load the TFLite model file from the assets folder into a memory-mapped buffer
        val modelBuffer = FileUtil.loadMappedFile(context, "model_tflite.tflite")

        // Configure interpreter options, setting the execution thread count to 4 for optimization
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }

        // Initialize the interpreter with the model file and the configured options
        interpreter = Interpreter(modelBuffer, options)
    }

    /** Pre-allocated Float32 TensorImage structure used to feed pixel data into the processor. */
    private val tensorImage = TensorImage(DataType.FLOAT32)

    /**
     * Analyzes a camera frame in real-time using TensorFlow Lite inference.
     * Converts the frame into a bitmap, executes the model, filters results through the tracker,
     * and extracts isolated card image crops.
     *
     * @param imageProxy The CameraX image proxy container providing access to the hardware frame.
     */
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        try {
            // Early return if the proxy does not contain a valid media image object
            if (imageProxy.image == null) return

            // Convert the incoming hardware image frame into an actionable Android Bitmap object
            val originalBitmap = imageProxy.toBitmap()

            // Load the bitmap pixels into the pre-allocated TensorImage structure
            tensorImage.load(originalBitmap)

            // Apply resizing and normalization transformations to match model input requirements
            val processedImage = imageProcessor.process(tensorImage)

            // Reset the internal buffer pointer to zero to prepare it for reading operations
            processedImage.buffer.rewind()

            // Execute the neural network inference using the pre-allocated output target arrays
            interpreter.runForMultipleInputsOutputs(
                arrayOf(processedImage.buffer),
                outputMap
            )

            // Temporary collection lists initialized to collect prediction metrics for this frame
            val tempBoxes = mutableListOf<RectF>()
            val tempConfidences = mutableListOf<Float>()
            val tempClasses = mutableListOf<Int>()

            // Extract the reported count of valid objects, adding rounding padding and constraining limits
            val objectCount =
                (numDetections[0] + 0.5f)
                    .toInt()
                    .coerceIn(0, 10)

            // Iterate over each detected object up to the maximum constrained object count limit
            for (i in 0 until objectCount) {
                val confidence = outputScores[0][i]
                val box = outputLocations[0][i]

                // Construct a RectF bounding box mapped from the structured output coordinates array
                tempBoxes.add(
                    RectF(
                        box[1], box[0],
                        box[3], box[2]
                    )
                )
                tempConfidences.add(confidence)
                tempClasses.add(
                    outputClasses[0][i].toInt()
                )
            }

            // Submit the structural predictions into the tracker instance to handle multi-frame continuity
            val trackedCards = cardTracker.update(
                tempBoxes,
                tempConfidences,
                tempClasses
            )

            // Forward the updated list of tracked cards to the main consumer execution block
            onCardsUpdated(trackedCards)
        } catch (e: Exception) {
            // Log any unexpected internal runtime or inference exceptions encountered during analysis
            Log.e(
                "CardAnalyzer",
                "Inference error: ${e.message}"
            )
        } finally {
            // Close the frame proxy wrapper container to free up the hardware image resource allocation
            imageProxy.close()
        }
    }

}
