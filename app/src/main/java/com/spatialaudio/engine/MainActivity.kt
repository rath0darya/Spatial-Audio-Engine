package com.spatialaudio.engine

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.*

/** Spotify-inspired YouTube embed with best-effort Android output-mix effects. */
class MainActivity : Activity() {
    private val projectionRequest = 6012
    private val bg = Color.rgb(9, 11, 17)
    private val surface = Color.rgb(23, 26, 36)
    private val surface2 = Color.rgb(31, 35, 48)
    private val green = Color.rgb(29, 220, 126)
    private val cyan = Color.rgb(74, 219, 245)
    private val purple = Color.rgb(170, 117, 255)
    private val white = Color.rgb(245, 247, 252)
    private val muted = Color.rgb(169, 177, 194)

    private lateinit var urlInput: EditText
    private lateinit var player: WebView
    private lateinit var status: TextView
    private lateinit var effectStatus: TextView
    private lateinit var depthStatus: TextView
    private lateinit var captureButton: Button
    private var virtualizer: Virtualizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizerOn = false
    private var bassOn = false
    private var bassStrength = 650

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        }

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brandRow.addView(TextView(this).apply {
            text = "◉"
            textSize = 30f
            setTextColor(green)
            typeface = Typeface.DEFAULT_BOLD
        })
        brandRow.addView(TextView(this).apply {
            text = "  SPATIAL"
            textSize = 23f
            setTextColor(white)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        })
        brandRow.addView(TextView(this).apply {
            text = " AUDIO"
            textSize = 23f
            setTextColor(green)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        })
        root.addView(brandRow)
        root.addView(text("YOUR SOUND. A WIDER WORLD.", 12f, green, true).apply {
            letterSpacing = 0.12f
            setPadding(0, dp(4), 0, dp(18))
        })
        root.addView(text("Immersive listening", 29f, white, true))
        root.addView(text("YouTube playback · 3D stage · deep-field bass", 14f, muted).apply {
            setPadding(0, dp(5), 0, dp(18))
        })

        val setup = card()
        setup.addView(text("NOW PLAYING", 11f, green, true))
        setup.addView(text("Paste a YouTube video link", 18f, white, true).apply {
            setPadding(0, dp(5), 0, dp(10))
        })
        urlInput = EditText(this).apply {
            hint = "https://youtube.com/watch?v=..."
            setHintTextColor(Color.rgb(125, 133, 151))
            setTextColor(white)
            textSize = 14f
            singleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(surface2, 12)
        }
        setup.addView(urlInput, matchWrap().apply { bottomMargin = dp(10) })
        setup.addView(button("LOAD YOUTUBE", green, bg) { loadYoutube() }, matchWrap())
        root.addView(setup, matchWrap().apply { bottomMargin = dp(14) })

        val playerCard = card()
        playerCard.addView(text("PLAYER", 11f, cyan, true).apply {
            setPadding(0, 0, 0, dp(10))
        })
        player = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webChromeClient = WebChromeClient()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(224))
            loadDataWithBaseURL("https://www.youtube.com", playerPlaceholder(), "text/html", "UTF-8", null)
        }
        playerCard.addView(player)
        playerCard.addView(text("Playback uses YouTube's embedded player. Sign-in, age limits, ads and some videos remain controlled by YouTube.", 12f, muted).apply {
            setPadding(0, dp(10), 0, 0)
        })
        root.addView(playerCard, matchWrap().apply { bottomMargin = dp(14) })

        val soundCard = card()
        soundCard.addView(text("SPATIAL SOUND STUDIO", 11f, purple, true))
        soundCard.addView(text("Choose your sound field", 20f, white, true).apply {
            setPadding(0, dp(5), 0, dp(12))
        })
        val presets = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        presets.addView(button("WIDE", surface2, white) { setPreset("wide") },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(6) })
        presets.addView(button("CINEMA", surface2, white) { setPreset("cinema") },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(6) })
        presets.addView(button("DEEP FIELD", surface2, white) { setPreset("deep") },
            LinearLayout.LayoutParams(0, dp(44), 1f))
        soundCard.addView(presets)
        soundCard.addView(text("3D STAGE · OUTPUT VIRTUALIZER", 12f, white, true).apply {
            setPadding(0, dp(18), 0, dp(3))
        })
        soundCard.addView(text("Attempts Android's system output-mix virtualizer when the phone firmware allows it.", 12f, muted))
        soundCard.addView(button("ENABLE 3D STAGE", green, bg) { toggleVirtualizer() }, matchWrap().apply {
            topMargin = dp(10)
            bottomMargin = dp(8)
        })
        effectStatus = text("3D stage: not enabled", 13f, muted)
        soundCard.addView(effectStatus)

        soundCard.addView(text("DEEP FIELD · BASS DEPTH", 12f, white, true).apply {
            setPadding(0, dp(18), 0, 0)
        })
        val bassSeek = SeekBar(this).apply {
            max = 1000
            progress = bassStrength
            progressTintList = ColorStateList.valueOf(purple)
            thumbTintList = ColorStateList.valueOf(purple)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    bassStrength = progress
                    if (bassOn) applyBassStrength()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        soundCard.addView(bassSeek, matchWrap())
        soundCard.addView(button("ENABLE DEEP FIELD", purple, Color.WHITE) { toggleBass() }, matchWrap().apply {
            topMargin = dp(4)
        })
        depthStatus = text("Deep field: not enabled", 13f, muted).apply {
            setPadding(0, dp(8), 0, 0)
        }
        soundCard.addView(depthStatus)
        root.addView(soundCard, matchWrap().apply { bottomMargin = dp(14) })

        val diagnostic = card()
        diagnostic.addView(text("COMPATIBILITY CHECK", 11f, cyan, true))
        diagnostic.addView(text("Some Android builds block global effects or playback capture. Test them separately; successful capture does not mean audio output was processed.", 13f, muted).apply {
            setPadding(0, dp(7), 0, dp(12))
        })
        captureButton = button("TEST LIVE PLAYBACK CAPTURE", cyan, bg) { requestCapture() }
        diagnostic.addView(captureButton, matchWrap())
        status = text("Capture test: idle", 13f, muted).apply { setPadding(0, dp(10), 0, 0) }
        diagnostic.addView(status)
        diagnostic.addView(button("STOP CAPTURE TEST", surface2, white) {
            stopService(Intent(this@MainActivity, CaptureService::class.java))
            status.text = "Capture test stopped."
            captureButton.isEnabled = true
        }, matchWrap().apply { topMargin = dp(8) })
        root.addView(diagnostic, matchWrap().apply { bottomMargin = dp(16) })

        root.addView(text("HONEST COMPATIBILITY NOTE", 11f, Color.rgb(255, 190, 113), true))
        root.addView(text(
            "This app is not Dolby Atmos-certified. 3D Stage and Deep Field try Android output-mix effects; many manufacturers disable these effects for third-party apps. If firmware rejects them, the app cannot force a system-wide effect. No audio is downloaded or saved by this app.",
            12f, muted
        ).apply { setPadding(0, dp(6), 0, dp(12)) })
        setContentView(scroll)
    }

    private fun loadYoutube() {
        val raw = urlInput.text.toString().trim()
        val id = extractVideoId(raw)
        if (id == null) {
            Toast.makeText(this, "Enter a valid YouTube video URL or 11-character video ID.", Toast.LENGTH_LONG).show()
            return
        }
        val html = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
            <style>html,body{margin:0;padding:0;background:#000;color:#fff;font-family:Arial;height:100%}
            .frame{position:relative;width:100%;height:100%;overflow:hidden}iframe{position:absolute;inset:0;width:100%;height:100%;border:0}</style>
            </head><body><div class="frame"><iframe
            src="https://www.youtube.com/embed/$id?playsinline=1&controls=1&rel=0&autoplay=0"
            title="YouTube player" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe></div></body></html>
        """.trimIndent()
        player.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
    }

    private fun extractVideoId(raw: String): String? {
        if (raw.matches(Regex("^[A-Za-z0-9_-]{11}$"))) return raw
        return try {
            val uri = Uri.parse(raw)
            val host = (uri.host ?: "").lowercase()
            val id = when {
                host == "youtu.be" || host.endsWith(".youtu.be") -> uri.lastPathSegment
                host.contains("youtube.com") -> when {
                    uri.path == "/watch" -> uri.getQueryParameter("v")
                    uri.pathSegments.firstOrNull() in listOf("embed", "shorts", "live") -> uri.pathSegments.getOrNull(1)
                    else -> null
                }
                else -> null
            }
            id?.takeIf { it.matches(Regex("^[A-Za-z0-9_-]{11}$")) }
        } catch (_: Exception) { null }
    }

    private fun setPreset(preset: String) {
        when (preset) {
            "wide" -> {
                bassStrength = 250
                effectStatus.text = "Wide preset selected · 3D Stage " + if (virtualizerOn) "active" else "off"
                Toast.makeText(this, "Wide preset selected. Enable 3D Stage if supported.", Toast.LENGTH_SHORT).show()
            }
            "cinema" -> {
                bassStrength = 480
                effectStatus.text = "Cinema preset selected · 3D Stage " + if (virtualizerOn) "active" else "off"
                Toast.makeText(this, "Cinema preset selected. Enable 3D Stage if supported.", Toast.LENGTH_SHORT).show()
            }
            "deep" -> {
                bassStrength = 850
                if (!bassOn) toggleBass() else applyBassStrength()
                Toast.makeText(this, "Deep Field preset selected.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleVirtualizer() {
        try {
            if (virtualizerOn) {
                virtualizer?.enabled = false
                virtualizerOn = false
                effectStatus.text = "3D stage disabled."
            } else {
                val effect = virtualizer ?: Virtualizer(0, 0).also { virtualizer = it }
                effect.setStrength(1000.toShort())
                effect.enabled = true
                virtualizerOn = effect.enabled
                effectStatus.text = if (virtualizerOn) {
                    "3D stage enabled by Android audio effects. Listen and verify on your earbuds."
                } else {
                    "Phone firmware did not enable the output virtualizer."
                }
            }
        } catch (e: Exception) {
            virtualizerOn = false
            effectStatus.text = "3D stage unavailable on this firmware (" + e.javaClass.simpleName + ")."
            Toast.makeText(this, "System virtualizer is not supported by this device build.", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleBass() {
        try {
            if (bassOn) {
                bassBoost?.enabled = false
                bassOn = false
                depthStatus.text = "Deep field disabled."
            } else {
                val effect = bassBoost ?: BassBoost(0, 0).also { bassBoost = it }
                effect.setStrength(bassStrength.toShort())
                effect.enabled = true
                bassOn = effect.enabled
                depthStatus.text = if (bassOn) {
                    "Deep field enabled · strength " + bassStrength + "/1000"
                } else {
                    "Phone firmware did not enable deep-field bass."
                }
            }
        } catch (e: Exception) {
            bassOn = false
            depthStatus.text = "Deep field unavailable on this firmware (" + e.javaClass.simpleName + ")."
            Toast.makeText(this, "System bass effect is not supported by this device build.", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyBassStrength() {
        try {
            bassBoost?.setStrength(bassStrength.toShort())
            depthStatus.text = "Deep field active · strength " + bassStrength + "/1000"
        } catch (_: Exception) {
            depthStatus.text = "Could not update deep-field strength."
        }
    }

    private fun requestCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        status.text = "Waiting for Android's system capture approval…"
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
                status.text = "Capture approved. Play audio and check the ongoing notification. This test measures capture only."
                captureButton.isEnabled = false
            } else {
                status.text = "Capture permission denied; no audio was captured."
            }
        }
    }

    private fun playerPlaceholder(): String = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>body{margin:0;background:#10131b;color:#a9b1c2;font:14px Arial;display:flex;align-items:center;justify-content:center;height:100vh;text-align:center}</style>
        </head><body>Paste a YouTube link above and tap LOAD YOUTUBE</body></html>
    """.trimIndent()

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(surface, 20)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.START
        }

    private fun button(label: String, fill: Int, foreground: Int, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(foreground)
            backgroundTintList = ColorStateList.valueOf(fill)
            minHeight = dp(44)
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setOnClickListener { action() }
        }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        try { virtualizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try {
            player.stopLoading()
            player.loadUrl("about:blank")
            player.destroy()
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
