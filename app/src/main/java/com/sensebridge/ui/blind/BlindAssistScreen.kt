package com.sensebridge.ui.blind

import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.ui.theme.BgDark
import com.sensebridge.ui.theme.P0DangerRed
import com.sensebridge.ui.theme.P1WarningAmber
import com.sensebridge.ui.theme.P2AttentionGreen
import com.sensebridge.ui.theme.PrimaryBlue
import com.sensebridge.ui.theme.SurfaceCard
import com.sensebridge.ui.theme.TextMuted
import com.sensebridge.ui.theme.TextPrimary
import com.sensebridge.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlindAssistScreen(
    viewModel: BlindAssistViewModel,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    val isCameraRunning by viewModel.isCameraRunning.collectAsState()
    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val lastAnnouncedEvent by viewModel.lastAnnouncedEvent.collectAsState()
    val recentVisionEvents by viewModel.recentVisionEvents.collectAsState()

    DisposableEffect(lifecycleOwner) {
        viewModel.startVision(lifecycleOwner, previewView)
        onDispose {
            viewModel.stopVision()
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Vision Assist (Trợ thị giác)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Live Viewport Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCameraRunning) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Camera đang tạm dừng", color = TextSecondary)
                        }
                    }
                }
            }

            // Headset and Running Status Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isBluetoothConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                                contentDescription = null,
                                tint = if (isBluetoothConnected) P2AttentionGreen else P1WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isBluetoothConnected) {
                                    connectedDeviceName ?: "Tai nghe đã kết nối"
                                } else {
                                    "Chưa kết nối tai nghe"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleCamera(lifecycleOwner, previewView) },
                            modifier = Modifier.semantics {
                                contentDescription = if (isCameraRunning) "Dừng camera" else "Bật camera"
                            }
                        ) {
                            Icon(
                                imageVector = if (isCameraRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                        }
                    }
                }
            }

            // Latest Voice Announcement Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lastAnnouncedEvent != null) PrimaryBlue.copy(alpha = 0.15f) else SurfaceCard
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "LỜI THOẠI VỪA PHÁT VÀO TAI NGHE:",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lastAnnouncedEvent?.spokenText ?: "Đang quét môi trường...",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Toggle Camera Action Button
            item {
                Button(
                    onClick = { viewModel.toggleCamera(lifecycleOwner, previewView) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCameraRunning) SurfaceCard else PrimaryBlue
                    )
                ) {
                    Icon(
                        imageVector = if (isCameraRunning) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCameraRunning) "TẠM DỪNG QUÉT CAMERA" else "TIẾP TỤC QUÉT CAMERA",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Recognized Visual Objects List
            item {
                Text(
                    text = "VẬT THỂ NHẬN DIỆN THỜI GIAN THỰC",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (recentVisionEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Text(
                            text = "Hướng camera ra phía trước để bắt đầu nhận diện người, xe cộ, bàn ghế hoặc cầu thang.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(recentVisionEvents) { event ->
                    VisionDetectedCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun VisionDetectedCard(event: SenseEvent) {
    val badgeColor = when (event.priority) {
        PriorityLevel.CRITICAL_P0 -> P0DangerRed
        PriorityLevel.WARNING_P1 -> P1WarningAmber
        PriorityLevel.ATTENTION_P2 -> P2AttentionGreen
        PriorityLevel.INFO_P3 -> PrimaryBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(badgeColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${event.displayTitle} (${event.spatialDirection.vietnameseLabel})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = event.spokenText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "${(event.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor
            )
        }
    }
}
