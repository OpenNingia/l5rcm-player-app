package com.l5rcm.companion.ui.imports.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.l5rcm.companion.data.save.qr.QrChunkAssembler
import com.l5rcm.companion.data.save.qr.QrTransferException
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Radii
import com.l5rcm.companion.ui.theme.Spacing
import com.l5rcm.companion.ui.widgets.RicePaperOverlay
import java.util.concurrent.Executors

/**
 * Scans an animated multi-frame QR sequence exported by the desktop app and hands the
 * reassembled `.l5r` JSON to [onResult]. The wire format / reassembly lives in
 * [QrChunkAssembler]; this screen only drives the camera and the progress UI.
 */
@Composable
fun QrScanScreen(onResult: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(L5RTheme.colors.paper)) {
        if (hasPermission) {
            CameraScanner(onResult = onResult, onCancel = onCancel)
        } else {
            RicePaperOverlay(Modifier.fillMaxSize())
            PermissionPrompt(
                onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun CameraScanner(onResult: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val assembler = remember { QrChunkAssembler() }
    var progress by remember { mutableStateOf(QrChunkAssembler.Progress(0, 0)) }
    var error by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(
                        analysisExecutor,
                        qrFrameAnalyzer { value ->
                            if (finished) return@qrFrameAnalyzer
                            val json = try {
                                assembler.offer(value)
                            } catch (e: QrTransferException) {
                                error = e.message
                                assembler.reset()
                                null
                            }
                            progress = assembler.progress
                            if (json != null) {
                                finished = true
                                ContextCompat.getMainExecutor(ctx).execute { onResult(json) }
                            }
                        },
                    )
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )
        ScanOverlay(progress = progress, error = error, onCancel = onCancel)
    }
}

@Composable
private fun ScanOverlay(progress: QrChunkAssembler.Progress, error: String?, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        val status = when {
            error != null -> error
            progress.total == 0 -> "Point the camera at the character's QR code."
            else -> "Scanning… ${progress.collected} / ${progress.total} frames"
        }
        Text(
            status,
            style = L5RTheme.type.body.copy(
                color = if (error != null) L5RTheme.colors.error else L5RTheme.colors.whiteWash,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(L5RTheme.colors.ink.copy(alpha = 0.6f), Radii.button)
                .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        )
        OutlinedButton(
            onClick = onCancel,
            shape = Radii.button,
            modifier = Modifier.padding(top = Spacing.s4),
        ) {
            Text("CANCEL", style = L5RTheme.type.label.copy(color = L5RTheme.colors.whiteWash))
        }
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Camera access is needed to scan a character QR code.",
            style = L5RTheme.type.body.copy(color = L5RTheme.colors.inkMuted),
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onGrant,
            shape = Radii.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = L5RTheme.colors.accentCrimson,
                contentColor = L5RTheme.colors.whiteWash,
            ),
            modifier = Modifier.padding(top = Spacing.s5),
        ) {
            Text("GRANT CAMERA ACCESS", style = L5RTheme.type.label)
        }
        OutlinedButton(
            onClick = onCancel,
            shape = Radii.button,
            modifier = Modifier.padding(top = Spacing.s2),
        ) {
            Text("CANCEL", style = L5RTheme.type.label.copy(color = L5RTheme.colors.inkMuted))
        }
    }
}
