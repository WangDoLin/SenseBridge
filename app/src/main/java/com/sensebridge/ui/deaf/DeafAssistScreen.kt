package com.sensebridge.ui.deaf

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
fun DeafAssistScreen(
    viewModel: DeafAssistViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isListening by viewModel.isListening.collectAsState()
    val isBackgroundEnabled by viewModel.isBackgroundEnabled.collectAsState()
    val currentDb by viewModel.currentDecibels.collectAsState()
    val recentAudioEvents by viewModel.recentAudioEvents.collectAsState()
    var isHighSensitivity by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.startListening(context)
        onDispose {
            // Only stop if background awareness is explicitly turned off
            if (!isBackgroundEnabled) {
                viewModel.stopListening(context)
            }
        }
    }

    // Dynamic decibel radar pulse animation
    val normalizedDb = ((currentDb - 30.0).coerceIn(0.0, 60.0) / 60.0).toFloat()
    val animatedPulseScale by animateFloatAsState(
        targetValue = if (isListening) 1.0f + (normalizedDb * 0.4f) else 1.0f,
        animationSpec = tween(150),
        label = "RadarPulse"
    )

    val radarColor by animateColorAsState(
        targetValue = when {
            normalizedDb > 0.7f -> P0DangerRed
            normalizedDb > 0.4f -> P1WarningAmber
            else -> P2AttentionGreen
        },
        label = "RadarColor"
    )

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Sound Assist (Trợ thính & Báo động)") },
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
            // Visual Sound Radar
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size((140 * animatedPulseScale).dp)
                        .background(radarColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(radarColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Hearing else Icons.Default.MicOff,
                            contentDescription = null,
                            tint = radarColor,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }

            // Realtime Decibel & State Indicator
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isListening) "ĐANG THEO DÕI ÂM THANH LIÊN TỤC" else "ĐÃ DỪNG LẮNG NGHE",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isListening) P2AttentionGreen else P1WarningAmber
                    )
                    Text(
                        text = "Âm lượng hiện tại: ${currentDb.toInt()} dB (Ngưỡng cảnh báo: ${viewModel.thresholdDb.toInt()} dB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { normalizedDb },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(8.dp),
                        color = radarColor,
                        trackColor = SurfaceCard
                    )
                }
            }

            // Background Monitoring Toggle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isBackgroundEnabled) P2AttentionGreen else TextMuted
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Quét ngầm khi thoát app",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Vẫn quét & rung khi khóa máy hoặc chuyển app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isBackgroundEnabled,
                            onCheckedChange = { viewModel.toggleBackgroundMonitoring(context) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BgDark,
                                checkedTrackColor = P2AttentionGreen
                            )
                        )
                    }
                }
            }

            // Sensitivity Switch Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = !isHighSensitivity,
                        onClick = {
                            isHighSensitivity = false
                            viewModel.setSensitivity(false)
                        },
                        label = { Text("Đường phố (68 dB)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = BgDark
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterChip(
                        selected = isHighSensitivity,
                        onClick = {
                            isHighSensitivity = true
                            viewModel.setSensitivity(true)
                        },
                        label = { Text("Phòng yên tĩnh (55 dB)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = BgDark
                        )
                    )
                }
            }

            // Toggle Listening Button
            item {
                Button(
                    onClick = {
                        if (isListening) viewModel.stopListening(context) else viewModel.startListening(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) SurfaceCard else P2AttentionGreen
                    )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "TẠM DỪNG MIC" else "BẮT ĐẦU LẮNG NGHE NGAY",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Detected Events Section
            item {
                Text(
                    text = "ÂM THANH KHẨN CẤP ĐÃ NHẬN DIỆN",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (recentAudioEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Text(
                            text = "Chưa phát hiện âm thanh bất thường. Khi có tiếng còi xe, báo cháy hoặc tiếng động lớn, điện thoại sẽ lập tức rung mạnh và gửi thông báo!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(recentAudioEvents) { event ->
                    AudioDetectedCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun AudioDetectedCard(event: SenseEvent) {
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
                    .size(14.dp)
                    .background(badgeColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.displayTitle,
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
                style = MaterialTheme.typography.labelLarge,
                color = badgeColor
            )
        }
    }
}
