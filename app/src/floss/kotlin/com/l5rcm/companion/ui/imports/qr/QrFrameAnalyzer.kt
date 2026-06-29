package com.l5rcm.companion.ui.imports.qr

import androidx.camera.core.ImageAnalysis
import zxingcpp.BarcodeReader

/**
 * `floss` flavor QR decoder backed by zxing-cpp (Apache-2.0) — the proprietary-free scanner
 * used for the F-Droid build. Builds the CameraX analyzer that forwards every decoded QR
 * payload to [onValue]. The `full` flavor provides the ML Kit counterpart.
 *
 * `read(ImageProxy)` consumes the YUV luminance plane directly, so no `@ExperimentalGetImage`
 * opt-in is needed; the analyzer must close each proxy, which `use` handles.
 */
internal fun qrFrameAnalyzer(onValue: (String) -> Unit): ImageAnalysis.Analyzer {
    val reader = BarcodeReader(
        BarcodeReader.Options(
            formats = setOf(BarcodeReader.Format.QR_CODE),
            tryHarder = true,
            tryRotate = true,
            tryInvert = true,
        ),
    )
    return ImageAnalysis.Analyzer { proxy ->
        proxy.use { image ->
            reader.read(image).forEach { result -> result.text?.let(onValue) }
        }
    }
}
