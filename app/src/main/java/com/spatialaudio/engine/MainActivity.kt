package com.spatialaudio.engine

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.view.ViewGroup

class MainActivity : Activity() {
    private val projectionRequest = 6012
    private lateinit var status: TextView
    private lateinit var start: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 15, 34)
        window.navigationBarColor = Color.rgb(8, 15, 34)
        val bg = Color.rgb(8, 15, 34)
        val cyan = Color.rgb(73, 224, 240)
        val white = Color.rgb(240, 246, 255)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(bg)
        }
        val title = TextView(this).apply {
            text = "SPATIAL AUDIO ENGINE"
            textSize = 23f
            setTextColor(cyan)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "Live audio capture feasibility prototype"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 24)
        }
        val panel = TextView(this).apply {
            text = "YOUR SETUP\nVivo Y400 Pro · OriginOS 6\nboAt Nirvana Ion ANC · YouTube"
            textSize = 15f
            setTextColor(white)
            setPadding(18, 18, 18, 18)
            setBackgroundColor(Color.rgb(20, 34, 63))
        }
        status = TextView(this).apply {
            text = "Status: idle\n\nAndroid may allow capturing eligible YouTube audio after you approve the system prompt. This app cannot silently take over or replace YouTube's output."
            textSize = 15f
            setTextColor(white)
            setPadding(0, 24, 0, 24)
        }
        start = Button(this).apply {
            text = "TEST LIVE CAPTURE"
            setTextColor(bg)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(cyan))
            setOnClickListener { requestCapture() }
        }
        val stop = Button(this).apply {
            text = "STOP TEST"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureService::class.java))
                status.text = "Status: stopped"
                start.isEnabled = true
            }
        }
        val note = TextView(this).apply {
            text = "IMPORTANT LIMITATION\nAndroid playback capture is a copy, not an audio-output replacement. This prototype measures whether capture works; it does not route processed sound back to the earbuds, because that can create doubled audio/echo while YouTube continues playing. A seamless system-wide replacement requires OS/OEM audio-effect integration or a supported device audio-effects API."
            textSize = 13f
            setTextColor(Color.rgb(255, 204, 128))
            setPadding(0, 18, 0, 0)
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(panel)
        root.addView(status)
        root.addView(start, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(stop, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(note)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun requestCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        status.text = "Waiting for Android's screen/audio capture approval…"
        @Suppress("DEPRECATION")
        startActivityForResult(manager.createScreenCaptureIntent(), projectionRequest)
    }

    @Deprecated("Deprecated by Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == projectionRequest) {
            if (resultCode == RESULT_OK && data != null) {
                val intent = Intent(this, CaptureService::class.java).apply {
                    putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(CaptureService.EXTRA_RESULT_DATA, data)
                }
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
                status.text = "Capture approved. Starting live YouTube capture test…\nPlay YouTube audio now. Results will appear here / in the notification."
                start.isEnabled = false
            } else {
                status.text = "Capture permission was denied. No audio was captured."
            }
        }
    }
}