package com.zaziapps.mtg_scanner.ui.components

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.concurrent.futures.await
import com.zaziapps.mtg_scanner.data.model.CardDetection
import com.zaziapps.mtg_scanner.analyzer.TextAnalyzer
import com.zaziapps.mtg_scanner.analyzer.CardAnalyzer
import com.zaziapps.mtg_scanner.analyzer.CardTracker
import com.zaziapps.mtg_scanner.data.local.lang.CardTranslations
import com.zaziapps.mtg_scanner.data.model.TrackedCard
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Interface element providing a realtime viewfinder preview stream backed by the CameraX hardware pipeline.
 * Coordinates model detection analysis routines concurrently alongside interactive graphics layouts.
 *
 * @param mainHandler Handler attached to the main execution thread to safely dispatch UI update callbacks.
 * @param analysisExecutor Background thread service dedicated to running heavy computer vision inference jobs.
 * @param cardTracker Persistent tracking manager that filters, smooths, and maps raw target bounding boxes.
 * @param imageCapture Shared asset layer configured to lock image frames for data capture executions.
 * @param modifier Structural styling constraints delegated down to surface view configurations.
 * @param onCardUpdated Callback invoked whenever the tracking routine completes a processing sweep over a frame.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    mainHandler: Handler,
    analysisExecutor: ExecutorService,
    cardTracker: CardTracker,
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
    onCardUpdated: (List<TrackedCard>) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Holds the active request required to link the device camera stream to the Compose layout hierarchy
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    // Initialize and bind the hardware camera lifecycle whenever the owner host instance resets
    LaunchedEffect(lifecycleOwner) {
        try {
            // Asynchronously fetch the camera process orchestration singleton instance
            val cameraProvider = ProcessCameraProvider.getInstance(context).await()

            // Build target pixel selection constraints specialized to match local hardware processing engine constraints.
            val targetResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        AspectRatio.RATIO_4_3,
                        AspectRatioStrategy.FALLBACK_RULE_AUTO
                    )
                )
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(640, 640),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            // Set up the preview pipeline layer using the predefined resolution constraints configuration
            val preview = Preview.Builder()
                .setResolutionSelector(targetResolutionSelector)
                .build()

            // Connect the preview pipeline emitter directly to our observable UI surface variable pointer
            preview.setSurfaceProvider { request -> surfaceRequest = request }

            // Allocate hardware profile settings ensuring input frame backpressure streams retain only the most current imagery data.
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(targetResolutionSelector)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            // Attach the customized card segmentation engine analyzer routine to the background thread pool
            imageAnalysis.setAnalyzer(
                analysisExecutor,
                CardAnalyzer(context, cardTracker) { trackedCards ->
                    // Route processed frame results back onto the primary application thread context
                    mainHandler.post {
                        onCardUpdated(trackedCards)
                    }
                }
            )

            // Select the default physical rear camera component as the primary input sensor source
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Detach any previous use cases before appending fresh configurations to avoid pipeline state collision errors
            cameraProvider.unbindAll()

            // Register the configured use cases into the application lifecycle manager loop
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )

        } catch (e: Exception) {
            // Record failure footprints inside error diagnostics logs if the hardware connection aborts
            Log.e("CameraPreview", "Failed to bind CameraX provider configurations", e)
        }
    }

    // Project the camera stream overlay coordinates directly onto the runtime UI canvas whenever a valid surface is available.
    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            modifier = modifier.fillMaxSize()
        )
    }
}
