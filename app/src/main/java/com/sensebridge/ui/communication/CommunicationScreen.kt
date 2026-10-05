package com.sensebridge.ui.communication

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.data.local.entity.SavedPhraseEntity
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
fun CommunicationScreen(
    viewModel: CommunicationViewModel,
    onBack: () -> Unit
) {
    val phrases by viewModel.allPhrases.collectAsState()
    val isListening by viewModel.isListeningToOpponent.collectAsState()
    val opponentText by viewModel.opponentSpokenText.collectAsState()
    val sttError by viewModel.sttError.collectAsState()

    var customText by remember { mutableStateOf("") }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Two-Way Conversation (Listener for opposite speaker)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isListening) P1WarningAmber.copy(alpha = 0.15f) else SurfaceCard
                    ),
                    border = BorderStroke(2.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isListening) "ĐANG LẮNG NGHE NGƯỜI ĐỐI DIỆN NÓI..." else "LỜI NGƯỜI ĐỐI DIỆN:",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isListening) P1WarningAmber else PrimaryBlue
                            )
                            if (isListening) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(P1WarningAmber, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = opponentText ?: "Chưa có nội dung. Bấm nút phía dưới để nghe người đối diện nói.",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (opponentText != null) TextPrimary else TextMuted
                        )

                        if (sttError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = sttError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = P0DangerRed
                            )
                        }
                    }
                }
            }

            // Big Listen Toggle Button
            item {
                Button(
                    onClick = {
                        if (isListening) viewModel.stopListeningToOpponent() else viewModel.startListeningToOpponent()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, BorderDark),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) P0DangerRed else PrimaryBlue
                    )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = SurfaceCard
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = if (isListening) "DỪNG NGHE" else "NHẤN ĐỂ NGHE NGƯỜI ĐỐI DIỆN",
                        style = MaterialTheme.typography.titleMedium,
                        color = SurfaceCard
                    )
                }
            }

            // Section 2: Type to Speak Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(2.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "GÕ CHỮ ĐỂ NÓI NGAY:",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { customText = it },
                            placeholder = { Text("Nhập câu bạn muốn nói...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark.copy(alpha = 0.3f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            trailingIcon = {
                                if (customText.isNotEmpty()) {
                                    IconButton(onClick = { customText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Xóa chữ", tint = TextMuted)
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (customText.isNotBlank()) {
                                        viewModel.speakCustomText(customText)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                enabled = customText.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.5.dp, BorderDark),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = SurfaceCard
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Phát âm thanh", color = SurfaceCard)
                            }

                            Button(
                                onClick = {
                                    if (customText.isNotBlank()) {
                                        viewModel.addCustomPhrase(customText)
                                        customText = ""
                                    }
                                },
                                enabled = customText.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.5.dp, BorderDark),
                                colors = ButtonDefaults.buttonColors(containerColor = P2AttentionGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = SurfaceCard
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Lưu thẻ", color = SurfaceCard)
                            }
                        }
                    }
                }
            }

            // Section 3: Quick AAC Speech Cards
            item {
                Text(
                    text = "BẤM ĐỂ ĐIỆN THOẠI NÓI THAY BẠN",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(phrases) { phrase ->
                AacPhraseCard(
                    phrase = phrase,
                    onClick = { viewModel.speakPhrase(phrase) },
                    onDelete = if (phrase.category == "custom") { { viewModel.deletePhrase(phrase) } } else null
                )
            }
        }
    }
}

@Composable
private fun AacPhraseCard(
    phrase: SavedPhraseEntity,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val buttonColor = when (phrase.priority) {
        PriorityLevel.CRITICAL_P0 -> P0DangerRed
        PriorityLevel.WARNING_P1 -> P1WarningAmber
        PriorityLevel.ATTENTION_P2 -> PrimaryBlue
        PriorityLevel.INFO_P3 -> P2AttentionGreen
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(2.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = phrase.vietnameseText,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa thẻ",
                            tint = P0DangerRed.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(buttonColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.5.dp, BorderDark, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Đọc câu",
                        tint = buttonColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
