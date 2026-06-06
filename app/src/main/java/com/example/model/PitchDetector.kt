package com.example.model

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * High-performance, offline-first analyzer that coordinates raw audio streaming.
 * Coordinates with [JniBridge] and provides an elite pure-Kotlin fallback engine
 * executing windowed FFT auto-chroma arrays (12-semitone pitch class profile trackers).
 */
class PitchDetector(
    private val sampleRate: Int = 22050,
    private val fftSize: Int = 1024 // Optimized window for JVM to prevent micro-stuttering
) {
    private val TAG = "PitchDetector"
    private val ioScope = CoroutineScope(Dispatchers.Default + Job())
    private var recordJob: Job? = null
    private var isRunning = false

    private val _chromagram = MutableStateFlow(FloatArray(12) { 0.05f })
    val chromagram: StateFlow<FloatArray> = _chromagram

    private val _currentPitchHz = MutableStateFlow(0f)
    val currentPitchHz: StateFlow<Float> = _currentPitchHz

    private val _detectedChordSymbol = MutableStateFlow("None")
    val detectedChordSymbol: StateFlow<String> = _detectedChordSymbol

    /**
     * Start the DSP engine.
     */
    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        isRunning = true

        if (JniBridge.isNativeActive()) {
            Log.i(TAG, "Starting Low-latency native Oboe DSP pipeline")
            try {
                JniBridge.startNativeEngine(sampleRate, fftSize)
                // Start a Polling thread for native chromagram vectors
                recordJob = ioScope.launch {
                    while (isRunning) {
                        val nativeChroma = JniBridge.getNativeChromagram()
                        if (nativeChroma.size == 12) {
                            _chromagram.value = nativeChroma
                            updateChordFromChroma(nativeChroma)
                        }
                        kotlinx.coroutines.delay(30)
                    }
                }
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start native audio engine, failing back to JVM.", e)
            }
        }

        // JVM Fallback Audio Engine
        Log.i(TAG, "Initializing JVM Real-Time Audio Capture & FFT DSP Pipeline")
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, fftSize * 2)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord could not be initialized. Check permissions.")
            return
        }

        audioRecord.startRecording()

        recordJob = ioScope.launch {
            val audioBuffer = ShortArray(fftSize)
            val real = FloatArray(fftSize)
            val imag = FloatArray(fftSize)

            while (isRunning) {
                val readSamples = audioRecord.read(audioBuffer, 0, fftSize)
                if (readSamples <= 0) continue

                // 1. Hann Windowing
                for (i in 0 until fftSize) {
                    val multiplier = 0.5f * (1.0f - cos(2.0f * PI * i / (fftSize - 1))).toFloat()
                    real[i] = audioBuffer[i] * multiplier
                    imag[i] = 0f
                }

                // 2. Perform Radix-2 FFT
                fft(real, imag)

                // 3. Populate Chromagram / Pitch Profiles
                val localChroma = FloatArray(12) { 0.02f }
                var maxMag = 0f
                var peakFreq = 0f

                for (bin in 1 until fftSize / 2) {
                    val r = real[bin]
                    val im = imag[bin]
                    val mag = sqrt(r * r + im * im)
                    
                    if (mag > 10.0f) {
                        val freq = (bin * sampleRate).toFloat() / fftSize
                        // Keep within standard musical limits [A0 - C8] (approx 27.5Hz to 4186Hz)
                        if (freq in 50.0f..3000.0f) {
                            if (mag > maxMag) {
                                maxMag = mag
                                peakFreq = freq
                            }
                            // Calculate pitch class (MIDI semitone index)
                            val midiNote = 12.0 * log2(freq / 440.0) + 69.0
                            val pitchClass = (midiNote.roundToInt().mod(12) + 12) % 12
                            localChroma[pitchClass] += mag
                        }
                    }
                }

                _currentPitchHz.value = if (maxMag > 80.0f) peakFreq else 0f

                // Normalize Chromagram Vector
                val chromaSum = localChroma.sum()
                if (chromaSum > 0f) {
                    for (i in 0..11) {
                        localChroma[i] = (localChroma[i]/chromaSum).coerceAtLeast(0.04f)
                    }
                }

                // Smooth chromagram update for aesthetic layout visualization
                val currentChroma = _chromagram.value
                val smoothedChroma = FloatArray(12)
                for (i in 0..11) {
                    smoothedChroma[i] = currentChroma[i] * 0.7f + localChroma[i] * 0.3f
                }

                _chromagram.value = smoothedChroma
                updateChordFromChroma(smoothedChroma)

                // 30 FPS processing refresh loop
                kotlinx.coroutines.delay(33)
            }

            try {
                audioRecord.stop()
                audioRecord.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error joining AudioRecord shutdown queue", e)
            }
        }
    }

    /**
     * Terminate the analysis engine thread.
     */
    fun stop() {
        isRunning = false
        if (JniBridge.isNativeActive()) {
            JniBridge.stopNativeEngine()
        }
        recordJob?.cancel()
        recordJob = null
    }

    /**
     * Analyzes current Chromagram vector profiles to identify the closest musical chord.
     */
    private fun updateChordFromChroma(chroma: FloatArray) {
        val noteNames = MusicTheoryUtils.sharps
        
        // Find top 3 high intensity frequency classes
        val sortedIndices = chroma.indices.sortedByDescending { chroma[it] }
        val root = sortedIndices[0]
        val second = sortedIndices[1]
        val third = sortedIndices[2]

        val rootName = noteNames[root]

        // Pitch intervals from the root
        val i1 = (second - root).mod(12)
        val i2 = (third - root).mod(12)

        val chord = when {
            // Major triad intervals (4 semitones for Major Third, 7 semitones for Perfect Fifth)
            (i1 == 4 && i2 == 7) || (i1 == 7 && i2 == 4) -> "$rootName"
            // Minor triad (3 semitones for Minor Third, 7 semitones for Perfect Fifth)
            (i1 == 3 && i2 == 7) || (i1 == 7 && i2 == 3) -> "${rootName}m"
            // Major 7th (4 semitones, 11 semitones)
            (i1 == 4 && i2 == 11) || (i1 == 11 && i2 == 4) -> "${rootName}maj7"
            // Minor 7th (3 semitones, 10 semitones)
            (i1 == 3 && i2 == 10) || (i1 == 10 && i2 == 3) -> "${rootName}m7"
            // Suspended 4th (5 semitones, 7 semitones)
            (i1 == 5 && i2 == 7) || (i1 == 7 && i2 == 5) -> "${rootName}sus4"
            // Dominant 7th (4 semitones, 10 semitones)
            (i1 == 4 && i2 == 10) || (i1 == 10 && i2 == 4) -> "${rootName}7"
            else -> "$rootName" // Fallback to root note name if triad is undefined
        }

        _detectedChordSymbol.value = chord
    }

    /**
     * Standard complex Radix-2 decimation-in-time Fast Fourier Transform.
     */
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n <= 1) return

        // Bit reversal permutation
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        // Cooley-Tukey Radix-2 FFT
        var size = 2
        while (size <= n) {
            val halfSize = size shr 1
            val tabStep = n / size
            
            for (i in 0 until n step size) {
                for (k in 0 until halfSize) {
                    val angle = -2.0 * PI * k / size
                    val wr = cos(angle).toFloat()
                    val wi = sin(angle).toFloat()

                    val targetIdx = i + k + halfSize
                    val tr = real[targetIdx] * wr - imag[targetIdx] * wi
                    val ti = real[targetIdx] * wi + imag[targetIdx] * wr

                    real[targetIdx] = real[i + k] - tr
                    imag[targetIdx] = imag[i + k] - ti

                    real[i + k] += tr
                    imag[i + k] += ti
                }
            }
            size = size shl 1
        }
    }
}
