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
import com.zaziapps.mtg_scanner.data.local.lang.CardTranslations
import java.util.concurrent.Executors

/**
 * Interface element providing a realtime viewfinder preview stream backed by the CameraX hardware pipeline.
 * Coordinates model detection analysis routines concurrently alongside interactive graphics layouts.
 *
 * @param imageCapture Shared asset layer configured to lock image frames for data capture executions.
 * @param modifier Structural styling constraints delegated down to surface view configurations.
 * @param cardTextAnalyzer Asynchronous character tracking framework running text recognition sweeps.
 * @param onCardDetectionChanged Functional validation dispatcher returning true when target objects occupy the preview framework.
 * @param onCardsGeometriesDetected Vector updates mapping individual bounding configurations across screen layout dimensions.
 * @param onCardNameExtracted Meta callback executing localized tracking properties back down to state controllers.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
    cardTextAnalyzer: TextAnalyzer,
    onCardDetectionChanged: (Boolean) -> Unit,
    onCardsGeometriesDetected: (cards: List<CardDetection>) -> Unit,
    onCardNameExtracted: (cards: List<CardDetection>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Maintain a concurrent thread-safe collection mapping cards currently visible within the viewer context.
    val persistentCardsOnScreen = remember { java.util.concurrent.ConcurrentHashMap<Int, CardDetection>() }
    val translationCache = remember { java.util.concurrent.ConcurrentHashMap<Int, CardTranslations>() }

    // Define the grace period window representing how many frames an item can drop visibility before total structural removal.
    val maxGraceFrames = 6

    DisposableEffect(Unit) {
        onDispose {
            // Relieve hardware threading dependencies and intercept execution memory leaks early during structural destruction.
            mainHandler.removeCallbacksAndMessages(null)
            analysisExecutor.shutdown()
            captureExecutor.shutdown()
        }
    }

    LaunchedEffect(lifecycleOwner) {
        try {
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

            val preview = Preview.Builder()
                .setResolutionSelector(targetResolutionSelector)
                .build()

            preview.setSurfaceProvider { request -> surfaceRequest = request }

            // Allocate hardware profile settings ensuring input frame backpressure streams retain only the most current imagery data.
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(targetResolutionSelector)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(analysisExecutor, CardAnalyzer(context) { incomingDetections ->
                // Process recent spatial inferences via indexed iterations to bypass implicit allocation loads.
                for (i in 0 until incomingDetections.size) {
                    val newCard = incomingDetections[i]
                    val existingCard = persistentCardsOnScreen[newCard.id]

                    if (existingCard != null) {
                        // Resynchronize spatial alignments while preserving active data attributes.
                        existingCard.foundTranslation = newCard.foundTranslation ?: translationCache[newCard.id]
                        persistentCardsOnScreen[newCard.id] = newCard.copy(framesAlive = maxGraceFrames).apply {
                            foundTranslation = existingCard.foundTranslation
                        }
                    } else {
                        newCard.framesAlive = maxGraceFrames
                        newCard.foundTranslation = translationCache[newCard.id]
                        persistentCardsOnScreen[newCard.id] = newCard
                    }
                }

                // Execute an in-place structural cleaning sweep across stored tracks to subtract lifespans from vanished targets.
                val iterator = persistentCardsOnScreen.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val savedId = entry.key
                    val savedCard = entry.value

                    var matchDiscovered = false
                    for (j in 0 until incomingDetections.size) {
                        if (incomingDetections[j].id == savedId) {
                            matchDiscovered = true
                            break
                        }
                    }

                    if (!matchDiscovered) {
                        savedCard.framesAlive -= 1
                        if (savedCard.framesAlive <= 0) {
                            iterator.remove()
                            translationCache.remove(savedId)
                        }
                    } else {
                        // Consolidate resolved translation models inside historical lookup buffers.
                        savedCard.foundTranslation?.let { translationCache[savedId] = it }
                    }
                }

                // Populate explicit dispatch structures to forward updated track collections directly over to screen layers.
                val listForUi = ArrayList<CardDetection>(persistentCardsOnScreen.size).apply {
                    addAll(persistentCardsOnScreen.values)
                }

                mainHandler.post {
                    onCardNameExtracted(listForUi)
                    onCardsGeometriesDetected(listForUi)
                    onCardDetectionChanged(listForUi.isNotEmpty())
                }
            })

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )

        } catch (e: Exception) {
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
