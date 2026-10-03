package com.sensebridge.ui.home

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SenseEvent
import com.sensebridge.ui.components.BigActionButton
import com.sensebridge.ui.components.StatusBanner
import com.sensebridge.ui.navigation.NavRoute
import com.sensebridge.ui.theme.BgDark
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
    val isPaused by viewModel.isPaused.collectAsState()
    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val isMutedForPrivacy by viewModel.isMutedForPrivacy.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()

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
                    Text(
                        text = "SenseBridge",
                        style = MaterialTheme.typography.headlineLarge,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Trợ lý giác quan nhân tạo",
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

            // Primary Accessibility Modes
            item {
                Text(
                    text = "CHẾ ĐỘ TRỢ NĂNG",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                BigActionButton(
                    title = "👂 Sound Assist",
                    subtitle = "Nhận diện còi xe, báo cháy & rung phản hồi",
                    icon = Icons.Default.Hearing,
                    iconTint = P1WarningAmber,
                    onClick = { onNavigate(NavRoute.DeafAssist.route) }
                )
            }

            item {
                BigActionButton(
                    title = "👁️ Vision Assist",
                    subtitle = "Quét vật thể, định vị & đọc qua tai nghe",
                    icon = Icons.Default.Visibility,
                    iconTint = PrimaryBlue,
                    onClick = { onNavigate(NavRoute.BlindAssist.route) }
                )
            }

            item {
                BigActionButton(
                    title = "📖 OCR Reader",
                    subtitle = "Hướng camera đọc biển hiệu & văn bản",
                    icon = Icons.Default.TextFields,
                    iconTint = P2AttentionGreen,
                    onClick = { onNavigate(NavRoute.OcrReader.route) }
                )
            }

            item {
                BigActionButton(
                    title = "🗣️ Communication",
                    subtitle = "Thẻ câu trợ giúp & dịch giọng nói hai chiều",
                    icon = Icons.Default.Chat,
                    iconTint = Color(0xFFA855F7),
                    onClick = { onNavigate(NavRoute.Communication.route) }
                )
            }

            // Testing / Simulation Controls
            item {
                SimulationTestCard(
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
    onSimulateHorn: () -> Unit,
    onSimulateFireAlarm: () -> Unit,
    onSimulatePerson: () -> Unit,
    onSimulateDoorbell: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "⚡ KIỂM THỬ TÍN HIỆU (GIAI ĐOẠN 1)",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlue
            )
            Text(
                text = "Nhấn để kiểm tra phản hồi rung và giọng nói Bluetooth.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSimulateHorn,
                    colors = ButtonDefaults.buttonColors(containerColor = P0DangerRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Còi xe (P0)", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onSimulateFireAlarm,
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Người (P2)", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onSimulateDoorbell,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
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
