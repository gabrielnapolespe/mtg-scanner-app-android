package com.zaziapps.mtg_scanner.ui.screens

import com.zaziapps.mtg_scanner.ui.components.CardScannerOverlay
import com.zaziapps.mtg_scanner.R
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zaziapps.mtg_scanner.ui.viewmodel.ScryfallViewModel
import com.zaziapps.mtg_scanner.data.model.MagicCard
import com.zaziapps.mtg_scanner.ui.components.CameraPreview
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.zaziapps.mtg_scanner.analyzer.CardTracker
import com.zaziapps.mtg_scanner.data.model.CardDetection
import com.zaziapps.mtg_scanner.analyzer.TextAnalyzer
import com.zaziapps.mtg_scanner.data.local.lang.CardTranslations
import com.zaziapps.mtg_scanner.data.model.TrackedCard
import com.zaziapps.mtg_scanner.data.states.RecognitionState
import com.zaziapps.mtg_scanner.ui.components.LoadingDialog
import com.zaziapps.mtg_scanner.ui.components.ScannedCardItem
import com.zaziapps.mtg_scanner.ui.components.ScannedCardsList
import com.zaziapps.mtg_scanner.ui.themes.Black
import com.zaziapps.mtg_scanner.ui.themes.Gold
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Camera preview screen that handles card detection, OCR, and language translation.
 * Coordinates real-time tracking loops with database verification layers on an adaptive viewport framework.
 *
 * @param scryfallViewModel ViewModel interacting with the external Scryfall API.
 * @param onMenuClick Callback triggered to expand the global navigation sidebar menu.
 * @param onScannedCardClick Callback triggered when a card overlay is clicked.
 */
@Composable
fun CameraView(
    scryfallViewModel: ScryfallViewModel = viewModel(),
    onMenuClick: () -> Unit,
    onScannedCardClick: (String) -> Unit
) {
    val context = LocalContext.current

    // Initialize the CameraX high-resolution image capture component instance
    val imageCapture = remember {
        ImageCapture.Builder().build()
    }

    // Allocate a handler bound to the main thread to post UI events smoothly
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Pre-allocate background threads to isolate pipeline inference workloads from capturing jobs
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            // Relieve hardware threading dependencies and intercept execution memory leaks early during structural destruction.
            mainHandler.removeCallbacksAndMessages(null)
            analysisExecutor.shutdown()
            captureExecutor.shutdown()
        }
    }

    // Retain object state managers across standard composition cycles
    val cardTracker = remember { CardTracker() }
    val cardTextAnalyzer = remember { TextAnalyzer() }

    val coroutineScope = rememberCoroutineScope()

    // Collect active pipeline states directly from the data layers
    val isSearchingCard by scryfallViewModel.isSearchingCard.collectAsState()
    var isCapturing by remember { mutableStateOf(false) }

    // Read initial application hardware permissions status safely
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Local cached collection storing stabilized tracking coordinates for runtime UI box updates
    var detectedCardsRects by remember { mutableStateOf<List<TrackedCard>>(emptyList()) }
    var scannedCardsList by remember { mutableStateOf(listOf<ScannedCardItem>()) }

    // System contract bridge to request hardware camera resource authorizations
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    // Trigger permission requests immediately if access is missing upon initial view mounting
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Render theme aesthetics background image layer
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Backdrop contrast color overlay mask
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Upper toolbar structure layout containing action triggers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Menu",
                        tint = Gold,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.camera_view_title).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Gold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Centered bounding viewport container holding the viewfinder instance
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            width = 3.dp,
                            color = Gold.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .background(Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {

                        CameraPreview(
                            mainHandler = mainHandler,
                            analysisExecutor = analysisExecutor,
                            imageCapture = imageCapture,
                            modifier = Modifier.fillMaxSize(),
                            cardTracker = cardTracker,
                            onCardUpdated = { trackedCards ->
                                // Filter out volatile noise tracks before forwarding data down to the canvas box draw layers
                                detectedCardsRects = trackedCards.filter { it.recognitionState != RecognitionState.CANDIDATE }

                                // 1. Verify if at least one card is currently stable and awaiting its initial analysis run
                                val hasCardsToCapture = trackedCards.any {
                                    it.recognitionState === RecognitionState.TRACKING && it.foundTranslation === null
                                }

                                // 2. If eligible cards are found and the capture loop lock is open, trigger a single global snapshot
                                if (hasCardsToCapture && !isCapturing) {
                                    isCapturing = true // Close the synchronization lock to throttle incoming frames during execution

                                    coroutineScope.launch {
                                        // SINGLE CAPTURE: Execute a single high-resolution capture of the entire sensor viewport scene
                                        // CAPTURA ÚNICA: Tomamos la foto de la escena completa una sola vez
                                        val escenaCompletaBitmap = captureFullScene(imageCapture, captureExecutor)

                                        if (escenaCompletaBitmap != null) {
                                            // MULTI-CROP PROCESSING: Extract specific localized card image assets using the shared snapshot bitmap
                                            trackedCards.forEach { card ->
                                                if (card.recognitionState == RecognitionState.TRACKING && card.foundTranslation == null) {
                                                    // Dispatch parallel OCR execution tasks independently across separate coroutine slots
                                                    launch {
                                                        cardTracker.updateRecognitionState(card.id, RecognitionState.OCR_RUNNING)
                                                        card.recognitionState = RecognitionState.OCR_RUNNING
                                                        val croppedCard = cropCard(escenaCompletaBitmap, card.boundingBox)
                                                        if (croppedCard != null) {
                                                            // SOLUTION: If width exceeds height, the target card is rotated sideways relative to the sensor frame.
                                                            // Rotate the sub-bitmap 90 degrees clockwise to align text data vertically before submitting it to OCR.
                                                            val correctedBitmap = if (croppedCard.width > croppedCard.height) {
                                                                rotateBitmap(croppedCard, 90f)
                                                            } else {
                                                                croppedCard
                                                            }
                                                            // Feed the perfectly vertical and legible card image into the text extraction routine
                                                            val detectedText = cardTextAnalyzer.firstRelevantTextExtraction(correctedBitmap)
                                                            if (detectedText.isNotBlank()) {
                                                                val results = scryfallViewModel.searchCard(detectedText)
                                                                val translation = results.firstOrNull()

                                                                if (translation != null) {
                                                                    cardTracker.updateRecognition(card.id, translation, RecognitionState.RESOLVED)

                                                                    mainHandler.post {
                                                                        val cardName = translation.nameEn ?: "Unknown Card"

                                                                        val newItem = ScannedCardItem(
                                                                            id = card.id,
                                                                            name = cardName,
                                                                            bitmap = correctedBitmap
                                                                        )

                                                                        scannedCardsList = listOf(newItem) + scannedCardsList.filter { it.name != translation.nameEn }
                                                                    }
                                                                } else {
                                                                    cardTracker.updateRecognitionState(card.id, RecognitionState.FAILED)
                                                                }
                                                            } else {
                                                                // Text extraction blank or illegible: Mark tracking instance as FAILED
                                                                cardTracker.updateRecognitionState(card.id, RecognitionState.FAILED)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Fallback routing: If the global hardware capture failed, reset pending card tracks to a failed state
                                            trackedCards.forEach { card ->
                                                if (card.recognitionState == RecognitionState.CAPTURED) {
                                                    cardTracker.updateRecognitionState(card.id, RecognitionState.FAILED)
                                                    card.recognitionState = RecognitionState.FAILED
                                                }
                                            }
                                        }
                                        isCapturing = false // Re-open the synchronization lock to enable subsequent capture tasks for incoming tracks
                                    }
                                }
                            }
                        )

                        CardScannerOverlay(
                            boundingBoxes = detectedCardsRects,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Display missing authorization fallback message label
                        Text(
                            text = stringResource(id = R.string.required_access_camera_message),
                            color = Black,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            ScannedCardsList(
                scannedCards = scannedCardsList,
                onItemClick = { name ->
                    onScannedCardClick(name)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
            )
        }
    }
    // Manage modal loading layout blocks triggered by active remote repository queries
    LoadingDialog(isLoading = isSearchingCard)
}

/**
 * Rotates an input bitmap source by a specified degree amount.
 */
private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * Captures a single high-resolution photograph of the complete scene visible to the camera sensor.
 * Suspends execution until the hardware capture loop completes or throws an exception.
 */
private suspend fun captureFullScene(
    imageCapture: ImageCapture,
    executor: Executor
): Bitmap? = suspendCancellableCoroutine { continuation ->
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val fullBitmap = imageProxy.toBitmap()
                    continuation.resume(fullBitmap)
                } catch (e: Exception) {
                    Log.e("CAMERA_VIEW", "Failed to extract Bitmap from hardware image proxy wrapper", e)
                    continuation.resume(null)
                } finally {
                    imageProxy.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CAMERA_VIEW", "Hardware photo capture operations aborted", exception)
                continuation.resume(null)
            }
        }
    )
}

/**
 * Crops a localized region out of a full-size bitmap based on normalized bounding box parameters.
 */
private fun cropCard(
    bitmap: Bitmap,
    normalizedBox: RectF
): Bitmap? {
    // Map normalized floating coordinates to exact hardware pixel bounds
    val left = (normalizedBox.left * bitmap.width)
        .toInt()
        .coerceIn(0, bitmap.width - 1)

    val top = (normalizedBox.top * bitmap.height)
        .toInt()
        .coerceIn(0, bitmap.height - 1)

    val right = (normalizedBox.right * bitmap.width)
        .toInt()
        .coerceIn(left + 1, bitmap.width)

    val bottom = (normalizedBox.bottom * bitmap.height)
        .toInt()
        .coerceIn(top + 1, bitmap.height)

    val width = right - left
    val height = bottom - top

    // Guard against inverted crop configurations or collapsed dimensions
    if (width <= 0 || height <= 0) {
        return null
    }

    return Bitmap.createBitmap(
        bitmap,
        left,
        top,
        width,
        height
    )
}