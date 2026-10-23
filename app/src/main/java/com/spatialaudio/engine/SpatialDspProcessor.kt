package com.spatialaudio.engine

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteOrder
import kotlin.math.tanh

/** In-memory PCM stereo widening, low-frequency enhancement and short cross-channel ambience. */
class SpatialDspProcessor : BaseAudioProcessor() {
    @Volatile var enabled = true
    @Volatile var width = 1.65f
    @Volatile var bass = 0.14f
    @Volatile var ambience = 0.05f
    private var lowL = 0.0
    private var lowR = 0.0
    private var delayL = FloatArray(0)
    private var delayR = FloatArray(0)
    private var delayIndex = 0
    private var delayFrames = 1
    private var monoDelay = FloatArray(0)
    private var monoIndex = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.channelCount !in 1..2) return AudioProcessor.AudioFormat.NOT_SET
        delayFrames = (inputAudioFormat.sampleRate * 0.013).toInt().coerceAtLeast(1)
        delayL = FloatArray(delayFrames)
        delayR = FloatArray(delayFrames)
        monoDelay = FloatArray((inputAudioFormat.sampleRate * 0.008).toInt().coerceAtLeast(1))
        monoIndex = 0
        // Expand mono streams to stereo so the spatial field can actually create left/right cues.
        return if (inputAudioFormat.channelCount == 1)
            AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, 2, C.ENCODING_PCM_16BIT)
        else inputAudioFormat
    }

    override fun onFlush() {
        lowL = 0.0; lowR = 0.0
        delayL.fill(0f); delayR.fill(0f); monoDelay.fill(0f); monoIndex = 0; delayIndex = 0
    }

    override fun queueInput(inputBuffer: java.nio.ByteBuffer) {
        val inputChannels = inputAudioFormat.channelCount
        val outputChannels = outputAudioFormat.channelCount
        val frames = inputBuffer.remaining() / (inputChannels * 2)
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        val output = replaceOutputBuffer(frames * outputChannels * 2).order(ByteOrder.nativeOrder())
        while (input.remaining() >= inputChannels * 2) {
            val l = input.short / 32768.0
            var r = if (inputChannels == 2) input.short / 32768.0 else l
            if (inputChannels == 1) {
                val delayedMono = monoDelay[monoIndex].toDouble()
                monoDelay[monoIndex] = l.toFloat()
                monoIndex = (monoIndex + 1) % monoDelay.size
                r = l * 0.38 + delayedMono * 0.62
            }
            var ol = l
            var or = r
            if (enabled) {
                lowL += 0.075 * (l - lowL)
                lowR += 0.075 * (r - lowR)
                val mid = (l + r) * 0.5
                val side = (l - r) * 0.5 * width.coerceIn(0.7f, 1.8f)
                val wl = mid + side
                val wr = mid - side
                val dl = delayL[delayIndex]
                val dr = delayR[delayIndex]
                delayL[delayIndex] = wl.toFloat()
                delayR[delayIndex] = wr.toFloat()
                delayIndex = (delayIndex + 1) % delayFrames
                ol = wl + lowL * bass.coerceIn(0f, 0.8f) + dr * ambience.coerceIn(0f, 0.22f)
                or = wr + lowR * bass.coerceIn(0f, 0.8f) + dl * ambience.coerceIn(0f, 0.22f)
                ol = tanh(ol * 1.04) / tanh(1.04)
                or = tanh(or * 1.04) / tanh(1.04)
            }
            output.putShort((ol.coerceIn(-1.0, 0.99997) * 32768.0).toInt().toShort())
            if (outputChannels == 2) output.putShort((or.coerceIn(-1.0, 0.99997) * 32768.0).toInt().toShort())
        }
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }
}