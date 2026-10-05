package com.sensebridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sensebridge.ui.theme.P0DangerRed
import com.sensebridge.ui.theme.P1WarningAmber
import com.sensebridge.ui.theme.P2AttentionGreen
import com.sensebridge.ui.theme.SurfaceCard
import com.sensebridge.ui.theme.TextMuted
import com.sensebridge.ui.theme.TextPrimary
import com.sensebridge.ui.theme.TextSecondary

@Composable
fun StatusBanner(
    isPaused: Boolean,
    isBluetoothConnected: Boolean,
    connectedDeviceName: String?,
    isMutedForPrivacy: Boolean,
    onTogglePause: () -> Unit,
    onUnmutePrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(2.dp, com.sensebridge.ui.theme.BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Sensing State & Pause/Resume
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isPaused) P1WarningAmber else P2AttentionGreen,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaused) "TẠM DỪNG CẢM BIẾN" else "HỆ THỐNG ĐANG QUÉT",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier.semantics {
                        contentDescription = if (isPaused) "Tiếp tục quét" else "Tạm dừng quét"
                    }
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = TextPrimary
                    )
                }
            }

            // Row 2: Bluetooth Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isBluetoothConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (isBluetoothConnected) P2AttentionGreen else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBluetoothConnected) {
                        "Tai nghe: ${connectedDeviceName ?: "Đã kết nối"}"
                    } else {
                        "Chưa kết nối tai nghe Bluetooth"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Privacy Mute Warning if active
            if (isMutedForPrivacy) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Đã ngắt tai nghe! Âm thanh tạm tắt để bảo vệ riêng tư.",
                        style = MaterialTheme.typography.bodySmall,
                        color = P0DangerRed,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onUnmutePrivacy,
                        colors = ButtonDefaults.buttonColors(containerColor = P0DangerRed)
                    ) {
                        Text("Phát loa ngoài", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
