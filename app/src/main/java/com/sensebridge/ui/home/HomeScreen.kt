package com.sensebridge.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.ui.components.BigActionButton
import com.sensebridge.ui.components.StatusBanner
import com.sensebridge.ui.navigation.NavRoute
import com.sensebridge.ui.theme.BgDark
import com.sensebridge.ui.theme.BorderDark
import com.sensebridge.ui.theme.P0DangerRed
import com.sensebridge.ui.theme.P1WarningAmber
import com.sensebridge.ui.theme.P2AttentionGreen
import com.sensebridge.ui.theme.PrimaryBlue
import com.sensebridge.ui.theme.SurfaceCard
import com.sensebridge.ui.theme.TextMuted
import com.sensebridge.ui.theme.TextPrimary
import com.sensebridge.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val isPaused by viewModel.isPaused.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val currentDecibels by viewModel.currentDecibels.collectAsState()
    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val isMutedForPrivacy by viewModel.isMutedForPrivacy.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        containerColor = BgDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    val hasCustomName = userProfile.userName.isNotBlank() && !userProfile.userName.equals("bạn", ignoreCase = true)
                    val headerTitle = if (hasCustomName) {
                        "Xin chào, ${userProfile.userPronoun} ${userProfile.userName}!"
                    } else {
                        "SenseBridge"
                    }
                    val headerSubtitle = if (userProfile.aiName.isNotBlank() && !userProfile.aiName.equals("SenseBridge", ignoreCase = true)) {
                        "Trợ lý ${userProfile.aiName} luôn đồng hành cùng ${userProfile.userPronoun}"
                    } else {
                        "Trợ lý giác quan nhân tạo đa tác vụ"
                    }
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        color = PrimaryBlue
                    )
                    Text(
                        text = headerSubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }

            // System Status Banner
            item {
                StatusBanner(
                    isPaused = isPaused,
                    isBluetoothConnected = isBluetoothConnected,
                    connectedDeviceName = connectedDeviceName,
                    isMutedForPrivacy = isMutedForPrivacy,
                    onTogglePause = { viewModel.togglePause() },
                    onUnmutePrivacy = { viewModel.unmutePrivacy() }
                )
            }

            // Continuous Background Awareness Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecording) Color(0xFFDCFCE7) else SurfaceCard
                    ),
                    border = BorderStroke(2.dp, if (isRecording) P2AttentionGreen else BorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (isRecording) P2AttentionGreen.copy(alpha = 0.2f) else TextMuted.copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isRecording) P2AttentionGreen else TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isRecording) "Bảo vệ ngầm đang BẬT" else "Bảo vệ ngầm đang TẮT",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isRecording) P2AttentionGreen else TextPrimary
                                )
                                Text(
                                    text = if (isRecording) "Quét liên tục (${currentDecibels.toInt()} dB) • Rung khi có tiếng động lớn" else "Nhấn bật để quét âm thanh cả khi thoát app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isRecording,
                            onCheckedChange = { viewModel.toggleBackgroundMonitoring(context) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BgDark,
                                checkedTrackColor = P2AttentionGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceCard
                            )
                        )
                    }
                }
            }

            // Task Action Buttons (Assistive Modes)
            item {
                Text(
                    text = "CHẾ ĐỘ TRỢ NĂNG (ĐA NHIỆM)",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                BigActionButton(
                    title = "Sound Assist",
                    subtitle = "Radar âm thanh, còi xe, báo cháy & rung phản hồi",
                    icon = Icons.Default.Hearing,
                    iconTint = P1WarningAmber,
                    onClick = { onNavigate(NavRoute.DeafAssist.route) }
                )
            }

            item {
                BigActionButton(
                    title = "Vision Assist",
                    subtitle = "Quét vật thể, định vị không gian & đọc qua tai nghe",
                    icon = Icons.Default.Visibility,
                    iconTint = PrimaryBlue,
                    onClick = { onNavigate(NavRoute.BlindAssist.route) }
                )
            }

            item {
                BigActionButton(
                    title = "OCR Reader",
                    subtitle = "Hướng camera đọc biển hiệu & văn bản tức thì",
                    icon = Icons.Default.TextFields,
                    iconTint = P2AttentionGreen,
                    onClick = { onNavigate(NavRoute.OcrReader.route) }
                )
            }

            item {
                BigActionButton(
                    title = "Communication",
                    subtitle = "Thẻ câu trợ giúp & dịch giọng nói hai chiều",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    iconTint = Color(0xFFA855F7),
                    onClick = { onNavigate(NavRoute.Communication.route) }
                )
            }

            item {
                BigActionButton(
                    title = "Cài đặt & Độ nhạy",
                    subtitle = "Bật tắt rung, độ nhạy Decibel & chế độ chạy ngầm",
                    icon = Icons.Default.Settings,
                    iconTint = PrimaryBlue,
                    onClick = { onNavigate(NavRoute.Settings.route) }
                )
            }

            // Testing / Simulation Controls
            item {
                SimulationTestCard(
                    onSimulateLoudNoise = { viewModel.simulateLoudNoiseP0() },
                    onSimulateHorn = { viewModel.simulateHornP0() },
                    onSimulateFireAlarm = { viewModel.simulateFireAlarmP0() },
                    onSimulatePerson = { viewModel.simulatePersonLeftP2() },
                    onSimulateDoorbell = { viewModel.simulateDoorbellP2() }
                )
            }

            // Recent Event History
            if (recentEvents.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LỊCH SỬ CẢM BIẾN GẦN ĐÂY",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextMuted
                        )
                        Button(
                            onClick = { viewModel.clearHistory() },
                            colors = ButtonDefaults.textButtonColors()
                        ) {
                            Text("Xóa lịch sử", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                items(recentEvents) { event ->
                    EventHistoryItem(event = event)
                }
            }
        }
    }
}

@Composable
private fun SimulationTestCard(
    onSimulateLoudNoise: () -> Unit,
    onSimulateHorn: () -> Unit,
    onSimulateFireAlarm: () -> Unit,
    onSimulatePerson: () -> Unit,
    onSimulateDoorbell: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(2.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "THỬ NGHIỆM PHẢN HỒI RUNG ĐIỆN THOẠI",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )
            }
            Text(
                text = "Nhấn nút để kiểm tra mô tơ rung và cảnh báo tức thì:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Huge Loud Noise Test Button
            Button(
                onClick = onSimulateLoudNoise,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, BorderDark),
                colors = ButtonDefaults.buttonColors(containerColor = P0DangerRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("THỬ TIẾNG ĐỘNG LỚN (88 dB - RUNG MẠNH SOS)", style = MaterialTheme.typography.labelLarge)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSimulateHorn,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, BorderDark),
                    colors = ButtonDefaults.buttonColors(containerColor = P0DangerRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Còi xe (P0)", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onSimulateFireAlarm,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, BorderDark),
                    colors = ButtonDefaults.buttonColors(containerColor = P0DangerRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Báo cháy (P0)", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSimulatePerson,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, BorderDark),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Người (P2)", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onSimulateDoorbell,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, BorderDark),
                    colors = ButtonDefaults.buttonColors(containerColor = P2AttentionGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Chuông cửa (P2)", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun EventHistoryItem(event: SenseEvent) {
    val badgeColor = when (event.priority) {
        PriorityLevel.CRITICAL_P0 -> P0DangerRed
        PriorityLevel.WARNING_P1 -> P1WarningAmber
        PriorityLevel.ATTENTION_P2 -> P2AttentionGreen
        PriorityLevel.INFO_P3 -> PrimaryBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.5.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(badgeColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
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
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor
            )
        }
    }
}
