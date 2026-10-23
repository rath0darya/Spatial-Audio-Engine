package com.spatialaudio.engine

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.AudioSink
import java.util.Locale

class MainActivity : Activity() {
    private val bg = Color.rgb(9, 12, 19)
    private val panel = Color.rgb(22, 27, 39)
    private val panel2 = Color.rgb(32, 39, 54)
    private val green = Color.rgb(35, 230, 135)
    private val cyan = Color.rgb(67, 214, 245)
    private val violet = Color.rgb(176, 126, 255)
    private val white = Color.rgb(245, 248, 255)
    private val muted = Color.rgb(157, 169, 190)
    private lateinit var url: EditText
    private lateinit var stateText: TextView
    private lateinit var playButton: Button
    private lateinit var dsp: SpatialDspProcessor
    private var player: ExoPlayer? = null
    private var preset = "WIDE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(14))
            setBackgroundColor(bg)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(18), dp(12) + bars.top, dp(18), dp(14) + bars.bottom)
            insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; addView(root) }

        val brand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        brand.addView(label("◉", 30f, green, true))
        brand.addView(label(" SPATIAL", 23f, white, true).apply { typeface = Typeface.create("sans-serif-black", Typeface.BOLD) })
        brand.addView(label(" AUDIO", 23f, green, true).apply { typeface = Typeface.create("sans-serif-black", Typeface.BOLD) })
        root.addView(brand)
        root.addView(label("LIVE AUDIO  •  REAL-TIME DSP", 12f, cyan, true).apply { setPadding(0, dp(3), 0, dp(16)) })

        val source = card()
        source.addView(label("STREAM SOURCE", 11f, green, true))
        url = EditText(this).apply {
            hint = "Paste direct audio / radio stream URL"
            setHintTextColor(muted); setTextColor(white); textSize = 14f
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(13), dp(12), dp(13), dp(12))
            background = rounded(panel2, 14)
        }
        source.addView(url, match().apply { topMargin = dp(10); bottomMargin = dp(10) })
        source.addView(button("PROCESS & PLAY", green, bg) { startStream() }, match())
        source.addView(label("Live stream • processed in memory • not saved", 12f, muted).apply { setPadding(0, dp(9), 0, 0) })
        root.addView(source, match().apply { bottomMargin = dp(14) })

        val now = card()
        now.addView(label("NOW PLAYING", 11f, cyan, true))
        stateText = label("Ready for a stream", 16f, white, true).apply { setPadding(0, dp(8), 0, dp(14)) }
        now.addView(stateText)
        val transport = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        playButton = button("▶  PLAY", green, bg) { togglePlayback() }
        transport.addView(playButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(8) })
        transport.addView(button("■  STOP", panel2, white) { stopPlayback() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        now.addView(transport)
        root.addView(now, match().apply { bottomMargin = dp(14) })

        val sound = card()
        sound.addView(label("SOUND FIELD", 11f, violet, true))
        sound.addView(label("Choose a sound", 20f, white, true).apply { setPadding(0, dp(5), 0, dp(12)) })
        val presets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("WIDE", "CINEMA", "DEEP FIELD").forEachIndexed { i, name ->
            val tint = when (i) { 0 -> green; 1 -> cyan; else -> violet }
            presets.addView(button(name, if (preset == name) tint else panel2, if (preset == name) bg else white) {
                setPreset(name)
                for (j in 0 until presets.childCount) {
                    val b = presets.getChildAt(j) as Button
                    val active = b.text.toString() == preset
                    val c = when (b.text.toString()) { "WIDE" -> green; "CINEMA" -> cyan; else -> violet }
                    b.backgroundTintList = android.content.res.ColorStateList.valueOf(if (active) c else panel2)
                    b.setTextColor(if (active) bg else white)
                }
            }, LinearLayout.LayoutParams(0, dp(43), 1f).apply { if (i < 2) rightMargin = dp(6) })
        }
        sound.addView(presets)
        sound.addView(label("INTENSITY", 11f, muted, true).apply { setPadding(0, dp(17), 0, dp(2)) })
        val intensity = SeekBar(this).apply {
            max = 100; progress = 55
            progressTintList = android.content.res.ColorStateList.valueOf(violet)
            thumbTintList = android.content.res.ColorStateList.valueOf(violet)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) {
                    val v = value / 100f
                    dsp.width = 0.85f + v * 0.85f
                    dsp.bass = v * 0.65f
                    dsp.ambience = v * 0.20f
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        sound.addView(intensity, match())
        sound.addView(CheckBox(this).apply {
            text = "Audio processing ON"; setTextColor(white)
            buttonTintList = android.content.res.ColorStateList.valueOf(green); isChecked = true
            setOnCheckedChangeListener { _, checked ->
                dsp.enabled = checked
                text = if (checked) "Audio processing ON" else "Original audio (bypass)"
            }
        })
        root.addView(sound, match().apply { bottomMargin = dp(12) })
        root.addView(label("DSP applies only to audio decoded inside this player. YouTube links are not processed.", 12f, muted).apply {
            setPadding(dp(4), 0, dp(4), dp(8))
        })
        setContentView(scroll)
        setupPlayer()
    }

    private fun setupPlayer() {
        dsp = SpatialDspProcessor()
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf<AudioProcessor>(dsp))
                    .build()
            }
        }
        player = ExoPlayer.Builder(this, renderers).build().also { p ->
            p.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playButton.text = if (isPlaying) "Ⅱ  PAUSE" else "▶  PLAY"
                    if (isPlaying) stateText.text = if (dsp.enabled) "Spatial DSP active • live audio" else "Playing original audio • DSP bypassed"
                }
                override fun onPlayerError(error: PlaybackException) {
                    stateText.text = "Stream unavailable"
                    Toast.makeText(this@MainActivity, "Can't play this stream. Check the direct audio URL.", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun startStream() {
        val raw = url.text.toString().trim()
        val uri = runCatching { Uri.parse(raw) }.getOrNull()
        val scheme = uri?.scheme?.lowercase(Locale.ROOT)
        val host = uri?.host?.lowercase(Locale.ROOT).orEmpty()
        if (uri == null || scheme !in listOf("http", "https") || host.isBlank()) {
            Toast.makeText(this, "Paste a valid direct audio stream URL.", Toast.LENGTH_LONG).show()
            return
        }
        if (host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be" || host.endsWith(".youtu.be")) {
            stateText.text = "YouTube audio cannot enter the DSP"
            Toast.makeText(
                this,
                "This link would play outside the app without spatial processing. Paste a direct audio stream URL to hear the DSP effect.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        stateText.text = "Connecting to stream…"
        try {
            player?.setMediaItem(MediaItem.fromUri(uri))
            player?.prepare()
            player?.play()
        } catch (_: Exception) { stateText.text = "Couldn't open stream" }
    }

    private fun togglePlayback() {
        val p = player ?: return
        if (p.mediaItemCount == 0) Toast.makeText(this, "Paste a stream URL first.", Toast.LENGTH_SHORT).show()
        else if (p.isPlaying) p.pause() else p.play()
    }

    private fun stopPlayback() {
        player?.stop(); player?.clearMediaItems()
        playButton.text = "▶  PLAY"; stateText.text = "Stopped"
    }

    private fun setPreset(name: String) {
        preset = name
        when (name) {
            "WIDE" -> { dsp.width = 1.65f; dsp.bass = 0.14f; dsp.ambience = 0.05f }
            "CINEMA" -> { dsp.width = 1.35f; dsp.bass = 0.28f; dsp.ambience = 0.15f }
            else -> { dsp.width = 1.20f; dsp.bass = 0.55f; dsp.ambience = 0.20f }
        }
        Toast.makeText(this, "$name enabled", Toast.LENGTH_SHORT).show()
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(15), dp(15), dp(15))
        background = rounded(panel, 20)
    }
    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private fun button(value: String, fill: Int, textColor: Int, action: () -> Unit) = Button(this).apply {
        text = value; textSize = 11f; isAllCaps = false
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setTextColor(textColor); backgroundTintList = android.content.res.ColorStateList.valueOf(fill)
        minHeight = dp(42); minimumHeight = dp(42); setPadding(dp(7), dp(3), dp(7), dp(3))
        setOnClickListener { action() }
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun match() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onDestroy() { player?.release(); super.onDestroy() }
}
