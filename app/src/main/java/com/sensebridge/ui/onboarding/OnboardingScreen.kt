package com.sensebridge.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sensebridge.ui.theme.BgDark
import com.sensebridge.ui.theme.BorderDark
import com.sensebridge.ui.theme.P2AttentionGreen
import com.sensebridge.ui.theme.PrimaryBlue
import com.sensebridge.ui.theme.SurfaceCard
import com.sensebridge.ui.theme.TextMuted
import com.sensebridge.ui.theme.TextPrimary
import com.sensebridge.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onCompleted: () -> Unit
) {
    val userName by viewModel.userName.collectAsState()
    val userPronoun by viewModel.userPronoun.collectAsState()
    val aiName by viewModel.aiName.collectAsState()
    val aiPronoun by viewModel.aiPronoun.collectAsState()

    val commonUserPronouns = listOf("bạn", "anh", "chị", "em", "bác", "cô", "chú")
    val commonAiPronouns = listOf("em", "cháu", "mình", "tôi")
    val commonAiNames = listOf("SenseBridge", "Lumi", "Mimi", "Bé Bưởi", "Trợ lý")

    Scaffold(
        containerColor = BgDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CHÀO MỪNG ĐẾN VỚI SENSEBRIDGE",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hãy thiết lập tên gọi và cách xưng hô để AI đồng hành cùng bạn một cách tự nhiên và ấm áp nhất.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Card 1: User Profile
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(2.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1. THÔNG TIN CỦA BẠN",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Tên gọi của bạn:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { viewModel.setUserName(it) },
                            placeholder = { Text("Ví dụ: Lâm, Tuấn, Hương...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Bạn muốn AI gọi bạn là gì? (Đại từ xưng hô):",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonUserPronouns.forEach { pronoun ->
                                val isSelected = userPronoun.equals(pronoun, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setUserPronoun(pronoun) },
                                    label = { Text(pronoun.replaceFirstChar { it.uppercase() }) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = BgDark,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Card 2: AI Assistant Profile
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(2.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = P2AttentionGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. THÔNG TIN TRỢ LÝ AI",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Đặt tên cho Trợ lý AI:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = aiName,
                            onValueChange = { viewModel.setAiName(it) },
                            placeholder = { Text("SenseBridge, Mimi, Lumi...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonAiNames.forEach { suggestedName ->
                                val isSelected = aiName.equals(suggestedName, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAiName(suggestedName) },
                                    label = { Text(suggestedName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = BgDark,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Trợ lý AI nên xưng với bạn là gì?",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonAiPronouns.forEach { pronoun ->
                                val isSelected = aiPronoun.equals(pronoun, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAiPronoun(pronoun) },
                                    label = { Text(pronoun.replaceFirstChar { it.uppercase() }) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = BgDark,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Card 3: Preview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(2.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "✨ BẢN XEM TRƯỚC CÂU CHÀO & XƯNG HÔ",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${viewModel.getPreviewGreeting()}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.speakPreview() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BgDark),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NGHE THỬ CÂU CHÀO", color = TextPrimary)
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = { viewModel.completeOnboarding(onCompleted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BẮT ĐẦU ĐỒNG HÀNH",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
