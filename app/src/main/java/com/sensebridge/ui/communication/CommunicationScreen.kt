package com.sensebridge.ui.communication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sensebridge.ui.theme.BgDark
import com.sensebridge.ui.theme.P0DangerRed
import com.sensebridge.ui.theme.P1WarningAmber
import com.sensebridge.ui.theme.P2AttentionGreen
import com.sensebridge.ui.theme.PrimaryBlue
import com.sensebridge.ui.theme.SurfaceCard
import com.sensebridge.ui.theme.TextPrimary
import com.sensebridge.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Communication (Giao tiếp hai chiều)") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "TÔI MUỐN NÓI GÌ?",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlue
            )

            // Emergency AAC Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* TTS speak */ },
                    colors = ButtonDefaults.buttonColors(containerColor = P0DangerRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                ) {
                    Text("TÔI CẦN GIÚP ĐỠ", style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = { /* TTS speak */ },
                    colors = ButtonDefaults.buttonColors(containerColor = P1WarningAmber),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                ) {
                    Text("TÔI BỊ ĐAU", style = MaterialTheme.typography.titleMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* TTS speak */ },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                ) {
                    Text("GỌI NGƯỜI THÂN", style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = { /* TTS speak */ },
                    colors = ButtonDefaults.buttonColors(containerColor = P2AttentionGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                ) {
                    Text("CẦN ĐI VIỆN", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two-Way Listener Card (Speech to Text)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NGƯỜI ĐỐI DIỆN NÓI:",
                        style = MaterialTheme.typography.labelLarge,
                        color = P1WarningAmber
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"Bạn có cần tôi gọi xe cấp cứu giúp không?\"",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { /* Listen */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = PrimaryBlue)
                Text(
                    text = "  NHẤN ĐỂ NGHE NGƯỜI ĐỐI DIỆN",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue
                )
            }
        }
    }
}
