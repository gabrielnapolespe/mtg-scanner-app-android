package com.zaziapps.mtg_scanner.ui.screens

import com.zaziapps.mtg_scanner.ui.components.CardScannerOverlay
import com.zaziapps.mtg_scanner.R
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
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
import com.zaziapps.mtg_scanner.data.model.CardDetection
import com.zaziapps.mtg_scanner.analyzer.TextAnalyzer
import com.zaziapps.mtg_scanner.data.local.lang.CardTranslations
import com.zaziapps.mtg_scanner.ui.components.LoadingDialog
import com.zaziapps.mtg_scanner.ui.themes.Black
import com.zaziapps.mtg_scanner.ui.themes.Gold


/**
 * Camera preview screen that handles card detection, OCR, and language translation.
 * Coordinates real-time tracking loops with database verification layers on an adaptive viewport framework.
 *
 * @param scryfallViewModel ViewModel interacting with the external Scryfall API.
 * @param onMenuClick Callback triggered to expand the global navigation sidebar menu.
 * @param onCardScanned Callback triggered when a card data object is fetched and confirmed.
 */
@Composable
fun CameraView(
    scryfallViewModel: ScryfallViewModel = viewModel(),
    onMenuClick: () -> Unit,
    onCardScanned: (MagicCard) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imageCapture = remember { ImageCapture.Builder().build() }

    val isSearchingCard by scryfallViewModel.isSearchingCard.collectAsState()
    val cardTextAnalyzer = remember { TextAnalyzer() }

    val localizedTranslations = remember { mutableStateMapOf<Int, CardTranslations>() }
    var showDialog by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var detectedCardsRects by remember { mutableStateOf<List<CardDetection>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
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

            // Adapt the viewfinder bounding container into a perfect square format layout.
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
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize(),
                        cardTextAnalyzer = cardTextAnalyzer,
                        onCardDetectionChanged = { isDetected ->
                            if (!isDetected) {
                                detectedCardsRects = emptyList()
                            }
                        },
                        onCardsGeometriesDetected = { geometryList ->
                            if (geometryList.isEmpty()) {
                                detectedCardsRects = emptyList()
                            } else {
                                // Inject previously resolved cached translations directly back into incoming camera target frames.
                                geometryList.forEach { card ->
                                    card.foundTranslation = localizedTranslations[card.id]
                                }
                                detectedCardsRects = geometryList
                            }
                        },
                        onCardNameExtracted = { activeTracks ->
                            coroutineScope.launch {
                                showDialog = false

                                try {
                                    activeTracks.forEach { card ->
                                        // Intercept analysis runs early if a localized validation already populates screen buffers.
                                        if (card.foundTranslation != null || localizedTranslations.containsKey(card.id)) {
                                            return@forEach
                                        }

                                        val bitmap = card.bitmap
                                        if (bitmap != null) {
                                            launch {
                                                val detectedText = cardTextAnalyzer.firstRelevantTextExtraction(bitmap)
                                                val matchResults = scryfallViewModel.searchCard(detectedText)

                                                if (matchResults.isNotEmpty()) {
                                                    val translation = matchResults[0]
                                                    Log.d("TEXT_SCANNER", "Resolved matching catalog entry: ${translation.nameEs}")

                                                    // Persist resolved data structures back inside screens states to inform layout painters.
                                                    localizedTranslations[card.id] = translation
                                                    card.foundTranslation = translation
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("CameraView", "OCR/Scryfall Error", e)
                                }
                            }
                        }
                    )

                    CardScannerOverlay(
                        boundingBoxes = detectedCardsRects,
                        modifier = Modifier.fillMaxSize()
                    )

                } else {
                    Text(
                        text = stringResource(id = R.string.required_access_camera_message),
                        color = Black,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

        }
    }
    LoadingDialog(isLoading = isSearchingCard)
}


