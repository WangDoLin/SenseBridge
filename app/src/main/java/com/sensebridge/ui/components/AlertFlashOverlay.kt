package com.sensebridge.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sensebridge.output.visual.AlertOverlayManager
import com.sensebridge.ui.theme.BgDark
import com.sensebridge.ui.theme.TextPrimary
import kotlinx.coroutines.delay

/** Total time the full-screen flash stays visible before auto-dismissing. */
private const val FLASH_VISIBLE_MS = 4000L

/** 1.5 flashes/s keeps well under the WCAG 2.3.1 limit of 3 flashes/s (photosensitive epilepsy). */
private const val FLASH_HALF_PERIOD_MS = 333
private const val FLASH_MIN_ALPHA = 0.35f
private const val FLASH_MAX_ALPHA = 0.92f
private const val CARD_ALPHA = 0.88f

/**
 * Full-screen high-contrast flash for urgent sound alerts.
 *
 * A deaf user looking at the phone may not feel the vibration (e.g. phone on a table); a
 * pulsing colour field + very large text is visible from across the room. Tap anywhere to
 * dismiss. TalkBack users get the title announced through an assertive live region.
 */
@Composable
fun AlertFlashOverlay(overlayManager: AlertOverlayManager) {
    val state by overlayManager.visualState.collectAsState()
    val event = state.activeEvent
    if (event == null || !state.isFlashing) return

    LaunchedEffect(event.id) {
        // An alert that fired while the app was in the background must not flash stale on reopen
        val ageMs = System.currentTimeMillis() - event.timestamp
        delay((FLASH_VISIBLE_MS - ageMs).coerceAtLeast(0L))
        overlayManager.dismissAlert()
    }

    val transition = rememberInfiniteTransition(label = "AlertFlash")
    val alpha by transition.animateFloat(
        initialValue = FLASH_MIN_ALPHA,
        targetValue = FLASH_MAX_ALPHA,
        animationSpec = infiniteRepeatable(tween(FLASH_HALF_PERIOD_MS), RepeatMode.Reverse),
        label = "AlertFlashAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(state.alertColorHex).copy(alpha = alpha))
            .clickable { overlayManager.dismissAlert() }
            .semantics { liveRegion = LiveRegionMode.Assertive },
        contentAlignment = Alignment.Center
    ) {
        AlertFlashContent(title = event.displayTitle, message = event.spokenText)
    }
}

@Composable
private fun AlertFlashContent(title: String, message: String) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            // Solid card keeps text contrast constant while the coloured field pulses behind it
            .background(BgDark.copy(alpha = CARD_ALPHA), RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Chạm để đóng",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary.copy(alpha = 0.8f)
        )
    }
}
