package com.sensebridge.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sensebridge.ui.theme.BgDark
import com.sensebridge.ui.theme.P2AttentionGreen
import com.sensebridge.ui.theme.PrimaryBlue
import com.sensebridge.ui.theme.SurfaceCard
import com.sensebridge.ui.theme.TextMuted
import com.sensebridge.ui.theme.TextPrimary
import com.sensebridge.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReaderScreen(
    viewModel: OcrReaderViewModel,
    onBack: () -> Unit
) {
    val isReading by viewModel.isReading.collectAsState()
    val lastReadText by viewModel.lastReadText.collectAsState()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("OCR Reader (Đọc văn bản)") },
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
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Targeting Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Hướng camera vào biển báo, nhãn thuốc hoặc menu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Recognized Text Result Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (lastReadText != null) P2AttentionGreen.copy(alpha = 0.15f) else SurfaceCard
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "VĂN BẢN VỪA ĐỌC:",
                        style = MaterialTheme.typography.labelMedium,
                        color = P2AttentionGreen
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = lastReadText ?: "Chưa có văn bản nào. Nhấn nút bên dưới để đọc.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (lastReadText != null) TextPrimary else TextMuted
                    )
                }
            }

            // Big Tap-to-Read Action Button
            Button(
                onClick = { /* In production, passes current preview frame */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = P2AttentionGreen)
            ) {
                if (isReading) {
                    CircularProgressIndicator(color = BgDark, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("ĐANG QUÉT CHỮ...", style = MaterialTheme.typography.titleLarge, color = BgDark)
                } else {
                    Icon(imageVector = Icons.Default.TextFields, contentDescription = null, tint = BgDark)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("CHẠM ĐỂ ĐỌC CHỮ", style = MaterialTheme.typography.titleLarge, color = BgDark)
                }
            }
        }
    }
}
