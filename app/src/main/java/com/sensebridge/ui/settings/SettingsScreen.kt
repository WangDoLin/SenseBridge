package com.sensebridge.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsState()
    val isBackgroundEnabled by viewModel.isBackgroundEnabled.collectAsState()
    val thresholdDb by viewModel.thresholdDb.collectAsState()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt & Độ nhạy") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Background Monitoring Master Switch
            item {
                Text(
                    text = "CHẾ ĐỘ NỀN & BẢO VỆ LIÊN TỤC",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                SettingSwitchCard(
                    title = "Chạy ngầm khi thoát ứng dụng",
                    subtitle = "Duy trì quét âm thanh & rung ngay cả khi tắt màn hình hoặc chuyển sang ứng dụng khác",
                    checked = isBackgroundEnabled,
                    onCheckedChange = { viewModel.toggleBackgroundMonitoring(context) }
                )
            }

            item {
                Text(
                    text = "KÊNH PHẢN HỒI GIÁC QUAN",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Haptic Switch
            item {
                SettingSwitchCard(
                    title = "Phản hồi xúc giác (Rung)",
                    subtitle = "Rung điện thoại khi có còi xe, báo cháy & tiếng động lớn",
                    checked = config.enableHaptic,
                    onCheckedChange = { viewModel.setHapticEnabled(it) }
                )
            }

            // Test Vibration Button
            item {
                Button(
                    onClick = { viewModel.testHapticVibration() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, BorderDark),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = SurfaceCard)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("THỬ RUNG ĐIỆN THOẠI NGAY", style = MaterialTheme.typography.labelLarge, color = SurfaceCard)
                }
            }

            // Voice Switch
            item {
                SettingSwitchCard(
                    title = "Phát giọng nói (TTS Bluetooth / Loa ngoài)",
                    subtitle = "Đọc cảnh báo và ngữ cảnh không gian",
                    checked = config.enableVoice,
                    onCheckedChange = { viewModel.setVoiceEnabled(it) }
                )
            }

            // Visual Switch
            item {
                SettingSwitchCard(
                    title = "Chớp màn hình trực quan",
                    subtitle = "Màn hình đổi màu khi có báo động khẩn cấp",
                    checked = config.enableVisual,
                    onCheckedChange = { viewModel.setVisualEnabled(it) }
                )
            }

            // Sensitivity Slider
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(2.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Ngưỡng kích hoạt âm thanh: ${thresholdDb.toInt()} dB",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Mức âm lượng tương đối (dBFS): 50-58 dB nhạy hơn cho phòng yên tĩnh; 60-70 dB phù hợp môi trường ngoài phố. Âm thanh đột biến lớn (>84 dB) luôn kích hoạt rung cảnh báo tức thì.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = thresholdDb.toFloat(),
                            onValueChange = { viewModel.updateThreshold(it.toDouble()) },
                            valueRange = 50f..80f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryBlue,
                                activeTrackColor = PrimaryBlue
                            )
                        )
                    }
                }
            }

            // Safety Disclaimer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(2.dp, BorderDark)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = P1WarningAmber,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = "LƯU Ý AN TOÀN QUAN TRỌNG",
                                style = MaterialTheme.typography.labelLarge,
                                color = P1WarningAmber
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "SenseBridge là giải pháp hỗ trợ tăng cường nhận thức (Augmented Awareness), không phải hệ thống bảo đảm an toàn tuyệt đối. Luôn kết hợp quan sát và sử dụng gậy dẫn đường / người đồng hành khi tham gia giao thông.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Clear Log Button
            item {
                Button(
                    onClick = { viewModel.clearAllLogs() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, BorderDark),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE0E0))
                ) {
                    Text("XÓA LỊCH SỬ SỰ KIỆN", color = P0DangerRed, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(2.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryBlue,
                    checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f)
                )
            )
        }
    }
}
