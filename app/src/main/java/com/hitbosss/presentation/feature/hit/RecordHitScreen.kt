package com.hitbosss.presentation.feature.hit

import android.Manifest
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hitbosss.presentation.designsystem.components.HitButton
import com.hitbosss.presentation.designsystem.components.HitButtonType
import com.hitbosss.presentation.designsystem.theme.Gray100
import com.hitbosss.presentation.designsystem.theme.Gray300
import com.hitbosss.presentation.designsystem.theme.Gray500
import com.hitbosss.presentation.designsystem.theme.Gray600
import com.hitbosss.presentation.designsystem.theme.Gray800
import com.hitbosss.presentation.designsystem.theme.HitbosssType
import com.hitbosss.presentation.designsystem.theme.Secondary800
import com.hitbosss.presentation.designsystem.theme.Success500
import java.io.File
import java.util.concurrent.Executors
import androidx.compose.ui.res.stringResource
import com.hitbosss.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private const val MAX_SECONDS = 105 // 01:45, igual que iOS

/**
 * Pantalla de grabación del HIT (equivale a RecordHitView de iOS): vista de cámara, área de
 * seguridad verde, contador 00:00 / 01:45, botón de grabar/parar y cambio de cámara.
 */
@Composable
fun RecordHitScreen(
    onClose: () -> Unit,
    onRecorded: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermissions = result.values.all { it }
    }
    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0) }
    var showPopup by remember { mutableStateOf(true) }

    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val videoCapture = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recording = remember { mutableStateOf<Recording?>(null) }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    // Vincula la cámara cada vez que cambian permisos o el objetivo.
    LaunchedEffect(hasPermissions, lensFacing) {
        if (!hasPermissions) return@LaunchedEffect
        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
        val capture = VideoCapture.withOutput(recorder).apply {
            // Orientación de grabación según la pantalla (portrait) para que el vídeo no salga girado.
            targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
        }
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        provider.unbindAll()
        runCatching { provider.bindToLifecycle(lifecycleOwner, selector, preview, capture) }
        videoCapture.value = capture
    }

    // Contador mientras graba; corta a 01:45.
    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        while (isRecording && elapsed < MAX_SECONDS) {
            kotlinx.coroutines.delay(1000)
            elapsed += 1
        }
        if (elapsed >= MAX_SECONDS) recording.value?.stop()
    }

    fun toggleRecording() {
        val capture = videoCapture.value ?: return
        if (isRecording) {
            recording.value?.stop()
            return
        }
        val dir = File(context.cacheDir, "hit_videos").apply { mkdirs() }
        val file = File(dir, "hit_${System.currentTimeMillis()}.mp4")
        val options = FileOutputOptions.Builder(file).build()
        elapsed = 0
        recording.value = capture.output
            .prepareRecording(context, options)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> isRecording = true
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        recording.value = null
                        // Tras grabar se navega a stringResource(R.string.hit_split_title) (no se sube aquí).
                        if (file.exists() && file.length() > 0) onRecorded(file.absolutePath)
                    }
                }
            }
    }

    val timeText = "%02d:%02d".format(elapsed / 60, elapsed % 60)

    Box(Modifier.fillMaxSize().background(Gray800)) {
        if (hasPermissions) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }

        Column(Modifier.fillMaxSize()) {
            // Barra superior (fondo blanco) con cerrar y cambiar cámara.
            Row(
                modifier = Modifier.fillMaxWidth().background(Gray100).statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Gray800,
                    modifier = Modifier.size(28.dp).clickable { recording.value?.stop(); onClose() },
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Cameraswitch, contentDescription = stringResource(R.string.record_switch_camera), tint = Gray800,
                    modifier = Modifier.size(28.dp).clickable {
                        if (!isRecording) {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        }
                    },
                )
            }

            // Contador 00:00 / 01:45.
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Gray100).padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("$timeText / ", style = HitbosssType.bodyDefaultRegular, color = Secondary800)
                    Text("01:45", style = HitbosssType.bodyDefaultEmphasis, color = Secondary800)
                }
            }

            // Área de seguridad (verde) + botón de grabar.
            Box(Modifier.fillMaxSize().padding(top = 16.dp, bottom = 60.dp)) {
                Box(Modifier.fillMaxSize().padding(horizontal = 8.dp).border(5.dp, Success500))
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).size(56.dp).clickable { toggleRecording() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isRecording) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(Gray100).border(1.dp, Gray300, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Pause, contentDescription = stringResource(R.string.record_stop), tint = Gray600, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(Color.Red).border(5.dp, Gray100, CircleShape))
                    }
                }
            }
        }

        // Popup inicial stringResource(R.string.record_exercise).
        if (showPopup && hasPermissions) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.padding(horizontal = 60.dp).clip(RoundedCornerShape(20.dp)).background(Gray100).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.CropFree, contentDescription = null, tint = Gray800, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.record_exercise), style = HitbosssType.titleSection, color = Gray800, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.record_safety_desc),
                        style = HitbosssType.bodyDefaultRegular, color = Gray500, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    HitButton("Aceptar", onClick = { showPopup = false }, type = HitButtonType.Secondary)
                }
            }
        }

        // Sin permisos.
        if (!hasPermissions) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.record_perms_title), style = HitbosssType.titleSection, color = Gray100, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.record_perms_msg),
                    style = HitbosssType.bodyDefaultRegular, color = Gray300, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                HitButton(stringResource(R.string.record_grant), onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                })
            }
        }
    }
}
