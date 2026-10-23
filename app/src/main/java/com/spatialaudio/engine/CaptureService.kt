package com.spatialaudio.engine

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class CaptureService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "spatial_capture"
        private const val NOTIFICATION_ID = 42
        private const val TAG = "SpatialAudioEngine"
    }

    private var projection: MediaProjection? = null
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private val running = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("Spatial Audio Engine")
            .setContentText("Testing live playback capture — no audio is saved")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        val code = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (code != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(code, data)
            startCapture()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start capture", e)
            updateNotification("Capture unavailable: ${e.javaClass.simpleName}")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startCapture() {
        val sampleRate = 48000
        val channelMask = AudioFormat.CHANNEL_IN_STEREO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)
        if (minBuffer <= 0) throw IllegalStateException("Unsupported audio format")
        val config = AudioPlaybackCaptureConfiguration.Builder(projection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build()
        recorder = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuffer * 2)
            .setAudioPlaybackCaptureConfig(config)
            .build()
        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord did not initialize")
        }
        running.set(true)
        recorder?.startRecording()
        worker = Thread {
            val buffer = ShortArray((minBuffer / 2).coerceAtLeast(1024))
            var lastReport = System.currentTimeMillis()
            var frames = 0L
            while (running.get()) {
                val count = try {
                    recorder?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: 0
                } catch (e: Exception) {
                    Log.e(TAG, "Capture read failed", e)
                    -1
                }
                if (count > 0) {
                    var sum = 0.0
                    for (i in 0 until count) {
                        val v = buffer[i].toDouble() / 32768.0
                        sum += v * v
                    }
                    val rms = sqrt(sum / count)
                    frames += count
                    val now = System.currentTimeMillis()
                    if (now - lastReport >= 2000) {
                        val message = if (rms > 0.0005) {
                            "Live capture active · signal detected · level ${"%.3f".format(rms)}"
                        } else {
                            "Capture running · no audible signal detected yet"
                        }
                        updateNotification(message)
                        Log.i(TAG, "$message samples=$frames")
                        lastReport = now
                    }
                } else if (count < 0) {
                    updateNotification("Capture read error: $count")
                    break
                }
            }
        }.apply { name = "SpatialAudioCapture"; start() }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Spatial capture", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun updateNotification(text: String) {
        val n = Notification.Builder(this, CHANNEL)
            .setContentTitle("Spatial Audio Engine")
            .setContentText(text.take(180))
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, n)
    }

    override fun onDestroy() {
        running.set(false)
        try { recorder?.stop() } catch (_: Exception) {}
        try { worker?.join(300) } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        projection?.stop()
        projection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}