package com.example.freshtrack.feature.scanner

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX analyzer that runs every preview frame through ML Kit's on-device
 * barcode model and reports grocery barcodes (UPC-A, UPC-E, EAN-13, EAN-8).
 *
 * To avoid acting on a misread, a code is only reported once it has been seen
 * in [requiredMatches] consecutive frames. Set [enabled] to false to pause
 * detection (e.g. while the result card is on screen) without unbinding the camera.
 */
class BarcodeAnalyzer(
    private val requiredMatches: Int = 2,
    private val onBarcodeDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile
    var enabled: Boolean = true
        set(value) {
            field = value
            if (value) resetCandidate()
        }

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
            )
            .build(),
    )

    private var candidate: String? = null
    private var candidateHits = 0

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (!enabled || mediaImage == null) {
            imageProxy.close()
            return
        }

        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                // ML Kit listeners run on the main thread, so it is safe to
                // update Compose state from onBarcodeDetected.
                val value = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                handleRead(value)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun handleRead(value: String?) {
        if (!enabled) return
        if (value == null) {
            resetCandidate()
            return
        }
        if (value == candidate) {
            candidateHits++
        } else {
            candidate = value
            candidateHits = 1
        }
        if (candidateHits >= requiredMatches) {
            enabled = false
            onBarcodeDetected(value)
        }
    }

    private fun resetCandidate() {
        candidate = null
        candidateHits = 0
    }

    fun close() = scanner.close()
}
