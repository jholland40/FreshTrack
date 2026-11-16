package com.example.freshtrack.feature.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.freshtrack.core.data.network.OpenFoodFactsService
import com.example.freshtrack.core.model.ProductInfo

/** Where the scanner is in the scan → look up → confirm flow. */
private sealed interface ScanState {
    data object Scanning : ScanState
    data class LookingUp(val barcode: String) : ScanState
    data class Found(val product: ProductInfo) : ScanState
    data class NotFound(val barcode: String) : ScanState
}

/**
 * Feature boundary for Member 3: Barcode recognition, CameraX, and confirmation workflow.
 *
 * Shows a live camera preview, detects UPC/EAN barcodes with ML Kit, looks the
 * code up on Open Food Facts, and lets the user confirm before handing the
 * product to the Add screen via [onAddProduct]. When the product isn't in the
 * database, [onAddProduct] still receives the barcode with a blank name so the
 * user can type the details in by hand.
 */
@Composable
fun ScannerScreen(
    onAddProduct: (ProductInfo) -> Unit = {},
) {
    val context = LocalContext.current
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    if (!hasCamera) {
        MessageScreen(
            icon = { Icon(Icons.Default.NoPhotography, null, Modifier.size(72.dp)) },
            title = "No camera found",
            body = "This device doesn't have a camera. You can still add packaged items by typing the barcode on the Add screen.",
        )
        return
    }

    CameraPermissionGate {
        BarcodeScannerContent(onAddProduct = onAddProduct)
    }
}

@Composable
private fun BarcodeScannerContent(
    onAddProduct: (ProductInfo) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var state by remember { mutableStateOf<ScanState>(ScanState.Scanning) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val analyzer = remember {
        BarcodeAnalyzer { barcode ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            state = ScanState.LookingUp(barcode)
        }
    }
    DisposableEffect(analyzer) {
        onDispose { analyzer.close() }
    }

    // Look the barcode up as soon as one is detected.
    LaunchedEffect(state) {
        val current = state
        if (current is ScanState.LookingUp) {
            val product = OpenFoodFactsService.lookup(current.barcode)
            state = if (product != null) ScanState.Found(product) else ScanState.NotFound(current.barcode)
        }
    }

    fun scanAgain() {
        state = ScanState.Scanning
        analyzer.enabled = true
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            analyzer = analyzer,
            onCameraReady = {
                camera = it
                torchOn = false
            },
            onError = { cameraError = "Couldn't start the camera. Close other camera apps and try again." },
            modifier = Modifier.fillMaxSize(),
        )

        ViewfinderOverlay(
            active = state is ScanState.Scanning,
            modifier = Modifier.fillMaxSize(),
        )

        // Hint text above the viewfinder
        Text(
            text = if (state is ScanState.Scanning) "Point the camera at a barcode" else "",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp, start = 24.dp, end = 24.dp),
        )

        // Torch toggle
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            FilledTonalIconButton(
                onClick = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = if (torchOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                    contentDescription = if (torchOn) "Turn flashlight off" else "Turn flashlight on",
                )
            }
        }

        // Result / error card
        val error = cameraError
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            when {
                error != null -> ResultCard(
                    icon = { Icon(Icons.Default.NoPhotography, null) },
                    title = "Camera unavailable",
                    body = error,
                )
                else -> when (val s = state) {
                    ScanState.Scanning -> Unit
                    is ScanState.LookingUp -> ResultCard(
                        icon = { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp) },
                        title = "Looking up product…",
                        body = "Barcode ${s.barcode}",
                    )
                    is ScanState.Found -> ResultCard(
                        icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
                        title = s.product.name,
                        body = listOfNotNull(
                            s.product.brand,
                            s.product.category?.replaceFirstChar { it.uppercase() },
                            "Barcode ${s.product.barcode}",
                        ).joinToString(" • "),
                        primaryLabel = "Add to pantry",
                        onPrimary = { onAddProduct(s.product) },
                        secondaryLabel = "Scan again",
                        onSecondary = { scanAgain() },
                    )
                    is ScanState.NotFound -> ResultCard(
                        icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) },
                        title = "Product not found",
                        body = "Barcode ${s.barcode} isn't in the Open Food Facts database (or you're offline). You can still add it and enter the details yourself.",
                        primaryLabel = "Add manually",
                        onPrimary = {
                            onAddProduct(ProductInfo(barcode = s.barcode, name = "", source = "Manual"))
                        },
                        secondaryLabel = "Scan again",
                        onSecondary = { scanAgain() },
                    )
                }
            }
        }
    }
}

/** Dims everything except a wide rounded window where the barcode should go. */
@Composable
private fun ViewfinderOverlay(active: Boolean, modifier: Modifier = Modifier) {
    val frameColor = if (active) Color.White else MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val windowWidth = size.width * 0.8f
        val windowHeight = windowWidth * 0.55f
        val topLeft = Offset(
            x = (size.width - windowWidth) / 2f,
            y = (size.height - windowHeight) / 2f - size.height * 0.08f,
        )
        val windowSize = Size(windowWidth, windowHeight)
        val radius = CornerRadius(24.dp.toPx())

        drawRect(Color.Black.copy(alpha = 0.55f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = topLeft,
            size = windowSize,
            cornerRadius = radius,
            blendMode = BlendMode.Clear,
        )
        drawRoundRect(
            color = frameColor,
            topLeft = topLeft,
            size = windowSize,
            cornerRadius = radius,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun ResultCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.size(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (primaryLabel != null || secondaryLabel != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (secondaryLabel != null) {
                        OutlinedButton(onClick = onSecondary, modifier = Modifier.weight(1f)) {
                            Text(secondaryLabel)
                        }
                    }
                    if (primaryLabel != null) {
                        Button(onClick = onPrimary, modifier = Modifier.weight(1f)) {
                            Text(primaryLabel)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Asks for the camera permission and only shows [content] once it's granted.
 * Re-checks when the user comes back from system Settings.
 */
@Composable
private fun CameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun isGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(isGranted()) }
    var askedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
        askedOnce = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = isGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ask straight away the first time the Scan tab opens.
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    if (granted) {
        content()
    } else {
        MessageScreen(
            icon = {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = "Camera access needed",
            body = "FreshTrack uses the camera to read barcodes on packaged food. Images stay on your phone; only the barcode number is looked up.",
            buttonLabel = if (askedOnce) "Open settings" else "Allow camera",
            onButton = {
                if (askedOnce) {
                    // After a denial Android may stop showing the dialog, so send the user to Settings.
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            },
            secondaryButtonLabel = if (askedOnce) "Try again" else null,
            onSecondaryButton = { launcher.launch(Manifest.permission.CAMERA) },
        )
    }
}

@Composable
private fun MessageScreen(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    buttonLabel: String? = null,
    onButton: () -> Unit = {},
    secondaryButtonLabel: String? = null,
    onSecondaryButton: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        if (buttonLabel != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onButton) { Text(buttonLabel) }
        }
        if (secondaryButtonLabel != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onSecondaryButton) { Text(secondaryButtonLabel) }
        }
    }
}
