package com.l5rcm.companion.ui.imports.qr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * `full` flavor QR decoder backed by ML Kit (bundled on-device model, no Play Services,
 * but proprietary). Builds the CameraX analyzer that forwards every decoded QR payload to
 * [onValue]. The proprietary-free `floss` flavor provides the zxing-cpp counterpart.
 */
internal fun qrFrameAnalyzer(onValue: (String) -> Unit): ImageAnalysis.Analyzer {
    val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )
    return ImageAnalysis.Analyzer { proxy -> scanFrame(scanner, proxy, onValue) }
}

/** Runs ML Kit barcode detection on one frame, forwarding each decoded QR text to [onValue]. */
@OptIn(ExperimentalGetImage::class)
private fun scanFrame(scanner: BarcodeScanner, proxy: ImageProxy, onValue: (String) -> Unit) {
    val media = proxy.image
    if (media == null) {
        proxy.close()
        return
    }
    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes -> barcodes.forEach { it.rawValue?.let(onValue) } }
        .addOnCompleteListener { proxy.close() }
}
