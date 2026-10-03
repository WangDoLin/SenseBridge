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
import com.sensebridge.input.sound.AudioRecorderManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground Service compliant with Android 14+ requirements for continuous acoustic awareness.
 */
@AndroidEntryPoint
class SenseBridgeForegroundService : Service() {

    companion object {
        private const val TAG = "SenseBridgeService"
        const val CHANNEL_ID = "sensebridge_awareness_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_PAUSE = "ACTION_TOGGLE_PAUSE"
    }

    @Inject
    lateinit var audioRecorderManager: AudioRecorderManager

    @Inject
    lateinit var eventEngine: EventEngine

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundMonitoring()
            ACTION_STOP -> stopForegroundMonitoring()
            ACTION_TOGGLE_PAUSE -> eventEngine.togglePause()
        }
        return START_STICKY
    }

    private fun startForegroundMonitoring() {
        val notification = buildNotification()

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
            Log.i(TAG, "Foreground acoustic monitoring started.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
        }
    }

    private fun stopForegroundMonitoring() {
        audioRecorderManager.stopListening()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SenseBridge đang hoạt động")
            .setContentText("Đang theo dõi âm thanh khẩn cấp để bảo vệ bạn.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cảnh báo giác quan SenseBridge",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kênh thông báo liên tục của dịch vụ theo dõi cảm biến SenseBridge"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        audioRecorderManager.stopListening()
    }
}
