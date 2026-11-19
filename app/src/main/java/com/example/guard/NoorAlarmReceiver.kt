package com.example.guard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AzanAudioPlayer
import com.example.audio.NoorVoiceEngine

class NoorAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_REMINDER
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Noor Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Boss Apnar Namaz Er Time Hoise, Oju Kore Asen"
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 1001)

        ensureChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (type == TYPE_AZAN) CHANNEL_AZAN else CHANNEL_REMINDER
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, notification)

        val guardPrefs = GuardPreferences(context)
        val guard = guardPrefs.getCurrentState()

        // Namaz Time: Set 15-minute Namaz window for Auto Silent + Lock and play Azan if enabled
        if (type == TYPE_AZAN) {
            guardPrefs.setActiveNamazWindowUntil(System.currentTimeMillis() + 15L * 60L * 1000L)
            if (guard.namazAutoSilentEnabled) {
                NoorGuardAccessibilityService.enableSilentModeIfPermitted(context)
            }
            if (guard.azanEnabled) {
                AzanAudioPlayer.playAzanChime()
            }
        } else if (type == TYPE_ALARM) {
            AzanAudioPlayer.playAzanChime()
        }

        // Female Voice ("Noor") spoken reminder using shared Female Voice Profile
        if (guard.femaleVoiceEnabled) {
            val pendingResult = goAsync()
            val mainHandler = Handler(Looper.getMainLooper())
            var finished = false
            var ttsRef: TextToSpeech? = null

            fun cleanup() {
                if (finished) return
                finished = true
                mainHandler.removeCallbacksAndMessages(null)
                runCatching { ttsRef?.shutdown() }
                runCatching { pendingResult.finish() }
            }

            // Safety timeout in case a device TTS engine never fires UtteranceProgressListener
            mainHandler.postDelayed({ cleanup() }, 8500L)

            ttsRef = TextToSpeech(context.applicationContext) { status ->
                val engine = ttsRef
                if (status == TextToSpeech.SUCCESS && engine != null) {
                    NoorVoiceEngine.configureFemaleVoiceProfile(engine, guard.femaleVoiceTone)
                    val utteranceId = "noor_alarm_$notifId"
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) = Unit
                        override fun onDone(id: String?) {
                            mainHandler.post { cleanup() }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            mainHandler.post { cleanup() }
                        }

                        override fun onError(id: String?, errorCode: Int) {
                            mainHandler.post { cleanup() }
                        }
                    })
                    val speechText = NoorVoiceEngine.prepareSpeechText(body)
                    engine.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                } else {
                    cleanup()
                }
            }
        }
    }

    companion object {
        const val CHANNEL_AZAN = "noor_azan_channel"
        const val CHANNEL_REMINDER = "noor_reminder_channel"

        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_NOTIF_ID = "extra_notif_id"

        const val TYPE_PRE_PRAYER = "pre_prayer"
        const val TYPE_AZAN = "azan"
        const val TYPE_ALARM = "boss_alarm"
        const val TYPE_REMINDER = "reminder"
        const val TYPE_NIGHT_SLEEP_DUA = "night_sleep_dua"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val azanChannel = NotificationChannel(
                    CHANNEL_AZAN,
                    "Noor Azan & Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Azan alerts and Boss wake-up alarms"
                }
                val remChannel = NotificationChannel(
                    CHANNEL_REMINDER,
                    "Noor Female Voice Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Noor Female Voice daily reminders, hydration, defense, and sleep alerts"
                }
                nm.createNotificationChannel(azanChannel)
                nm.createNotificationChannel(remChannel)
            }
        }
    }
}
