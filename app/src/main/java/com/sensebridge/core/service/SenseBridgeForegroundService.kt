package com.sensebridge.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sensebridge.MainActivity
import com.sensebridge.R
import com.sensebridge.core.engine.EventEngine
import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SensorySource
import com.sensebridge.input.sound.AudioRecorderManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service compliant with Android 14+ requirements for continuous acoustic awareness,
 * multi-tasking, and background safety protection.
 */
@AndroidEntryPoint
class SenseBridgeForegroundService : Service() {

    companion object {
        private const val TAG = "SenseBridgeService"
        const val CHANNEL_ID = "sensebridge_awareness_channel"
        const val ALERT_CHANNEL_ID = "sensebridge_alert_channel"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_PAUSE = "ACTION_TOGGLE_PAUSE"

        fun start(context: Context) {
            try {
                val intent = Intent(context, SenseBridgeForegroundService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cannot start SenseBridgeForegroundService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, SenseBridgeForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot stop SenseBridgeForegroundService: ${e.message}", e)
            }
        }
    }

    @Inject
    lateinit var audioRecorderManager: AudioRecorderManager

    @Inject
    lateinit var eventEngine: EventEngine

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var eventObserverJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundMonitoring()
            ACTION_STOP -> stopForegroundMonitoring()
            ACTION_TOGGLE_PAUSE -> eventEngine.togglePause()
            null -> {
                Log.d(TAG, "Service restarted with null intent; stopping to respect Android 14 FGS restrictions.")
                stopForegroundMonitoring()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundMonitoring() {
        val notification = buildNotification(
            "SenseBridge đang bảo vệ ngầm",
            "Đang theo dõi âm thanh & tiếng động lớn liên tục."
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            audioRecorderManager.startListening()
            observeEmergencyEvents()
            Log.i(TAG, "Foreground acoustic monitoring started.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
        }
    }

    private fun observeEmergencyEvents() {
        eventObserverJob?.cancel()
        eventObserverJob = serviceScope.launch {
            eventEngine.recentEvents.collectLatest { events ->
                val latest = events.firstOrNull() ?: return@collectLatest
                // Filter to acoustic classifier: user-initiated AAC phrases or OCR reading must never trigger alarm notification
                if (latest.source == SensorySource.AUDIO_CLASSIFIER &&
                    (latest.priority == PriorityLevel.CRITICAL_P0 || latest.priority == PriorityLevel.WARNING_P1)
                ) {
                    postEmergencyNotification(
                        title = latest.displayTitle,
                        message = latest.spokenText,
                        isCritical = latest.priority == PriorityLevel.CRITICAL_P0
                    )
                }
            }
        }
    }

    private fun postEmergencyNotification(title: String, message: String, isCritical: Boolean) {
        try {
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(if (isCritical) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(ALERT_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post emergency notification: ${e.message}", e)
        }
    }

    private fun stopForegroundMonitoring() {
        eventObserverJob?.cancel()
        eventObserverJob = null
        audioRecorderManager.stopListening()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(this, SenseBridgeForegroundService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, SenseBridgeForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Tạm dừng", pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dừng", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Low priority ongoing service channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SenseBridge Dịch vụ nền",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Theo dõi cảm biến và âm thanh liên tục trong nền"
            }
            manager.createNotificationChannel(serviceChannel)

            // High priority heads-up alert channel
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "SenseBridge Cảnh báo khẩn cấp",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cảnh báo còi xe, tiếng động lớn và báo động khẩn cấp"
                enableVibration(true)
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        eventObserverJob?.cancel()
        audioRecorderManager.stopListening()
    }
}
