package com.example.model

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

/**
 * High-performance digital signal processing (DSP) Audio Restoration Engine.
 * Runs offline restoration filters entirely client-side, including:
 * 1. Ground Hum Removal (targeting 50Hz/60Hz ground loop frequencies using a selective IIR Notch filter).
 * 2. Adaptive Noise Reduction (using thresholded envelope subtraction / spectral gating).
 * 3. Clipping Repair (reconstruction of saturated peak samples exceeding a threshold by cubic/linear interpolation).
 */
class AudioRestorationEngine {

    companion object {
        private const val DEFAULT_SAMPLE_RATE = 44100
        private const val CLIPPING_THRESHOLD = 0.98f
    }

    /**
     * Applies a high-precision digital IIR Notch Filter to isolate and remove hums (50Hz or 60Hz).
     *
     * @param inputSamples Normalized audio samples (-1.0f to 1.0f).
     * @param targetFrequency The frequency of the ground hum (typically 50.0f or 60.0f Hz).
     * @param sampleRate The sample rate of the audio file (e.g., 44100).
     * @param qFactor Quality factor of the notch filter. Higher values mean a narrower notch.
     * @return Restored samples with targeted hum attenuated.
     */
    fun removeGroundHum(
        inputSamples: FloatArray,
        targetFrequency: Float = 60f,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        qFactor: Float = 30f
    ): FloatArray {
        val output = FloatArray(inputSamples.size)
        
        // Calculate Notch filter coefficients
        val omega = (2.0 * PI * targetFrequency / sampleRate).toFloat()
        val alpha = (sin(omega.toDouble()) / (2.0 * qFactor)).toFloat()
        
        val b0 = 1f
        val b1 = -2f * cos(omega)
        val b2 = 1f
        val a0 = 1f + alpha
        val a1 = -2f * cos(omega)
        val a2 = 1f - alpha

        // Normalize coefficients
        val nb0 = b0 / a0
        val nb1 = b1 / a0
        val nb2 = b2 / a0
        val na1 = a1 / a0
        val na2 = a2 / a0

        // Filter state variables
        var x1 = 0f
        var x2 = 0f
        var y1 = 0f
        var y2 = 0f

        for (i in inputSamples.indices) {
            val x0 = inputSamples[i]
            val y0 = (nb0 * x0) + (nb1 * x1) + (nb2 * x2) - (na1 * y1) - (na2 * y2)
            
            // Shift delay line states
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0
            
            output[i] = y0.coerceIn(-1.0f, 1.0f)
        }
        return output
    }

    /**
     * Attenuates signal components below a running RMS-based noise floor envelope.
     * Operates as an offline expander/noise gate to minimize background system hisses.
     *
     * @param inputSamples Normalized audio samples.
     * @param thresholdDb The absolute gate threshold in decibels (e.g., -45.0f dB).
     * @param reductionRatio The amount of gain reduction applied to the noise below threshold (0.0f to 1.0f).
     * @return Denoised signal array.
     */
    fun reduceNoise(
        inputSamples: FloatArray,
        thresholdDb: Float = -40f,
        reductionRatio: Float = 0.2f
    ): FloatArray {
        val output = FloatArray(inputSamples.size)
        val thresholdAmp = Math.pow(10.0, thresholdDb.toDouble() / 20.0).toFloat()
        
        // Envelope follower parameters
        val attackTimeSec = 0.01f
        val releaseTimeSec = 0.1f
        val sampleRate = DEFAULT_SAMPLE_RATE.toFloat()
        val gAttack = Math.exp(-1.0 / (sampleRate * attackTimeSec)).toFloat()
        val gRelease = Math.exp(-1.0 / (sampleRate * releaseTimeSec)).toFloat()
        
        var envelope = 0f

        for (i in inputSamples.indices) {
            val input = inputSamples[i]
            val rectInput = Math.abs(input)
            
            // Envelope detection (attack vs release curves)
            if (rectInput > envelope) {
                envelope = gAttack * envelope + (1f - gAttack) * rectInput
            } else {
                envelope = gRelease * envelope + (1f - gRelease) * rectInput
            }

            // Gating / Attenuation factor calculation
            val gainFactor = if (envelope < thresholdAmp) {
                // Smooth crossover gain transition
                val ratio = envelope / (thresholdAmp + 1e-6f)
                reductionRatio + (1f - reductionRatio) * ratio
            } else {
                1f
            }

            output[i] = (input * gainFactor).coerceIn(-1.0f, 1.0f)
        }
        return output
    }

    /**
     * Scans for clipped (or flat-topped) waveform peaks and repairs them using cubic/Hermite spline interpolation.
     * Reconstructs lost peaks where digital waveforms hit digital maximums (clipping thresholds).
     */
    fun repairClipping(inputSamples: FloatArray): FloatArray {
        val output = inputSamples.clone()
        val size = inputSamples.size
        var i = 0

        while (i < size) {
            val sample = inputSamples[i]
            if (Math.abs(sample) >= CLIPPING_THRESHOLD) {
                // Found a clipped section. Trace boundaries.
                val startIdx = i - 1
                var endIdx = i
                while (endIdx < size && Math.abs(inputSamples[endIdx]) >= CLIPPING_THRESHOLD) {
                    endIdx++
                }
                
                // Boundaries must be safely within the signal limits to interpolate
                if (startIdx >= 2 && endIdx < size - 2) {
                    val gapLength = endIdx - startIdx
                    
                    // Left control points
                    val y0 = inputSamples[startIdx - 1]
                    val y1 = inputSamples[startIdx]
                    
                    // Right control points
                    val y2 = inputSamples[endIdx]
                    val y3 = inputSamples[endIdx + 1]

                    // Interpolate across the clipped valley
                    for (k in 0 until gapLength) {
                        val t = (k + 1).toFloat() / (gapLength + 1).toFloat()
                        val sampleValue = interpolateCubic(y0, y1, y2, y3, t)
                        output[startIdx + k + 1] = sampleValue.coerceIn(-1.1f, 1.1f)
                    }
                }
                i = endIdx
            } else {
                i++
            }
        }
        
        // Re-normalize to prevent clipping again from elevated repaired peaks
        var maxPeak = 0f
        for (s in output) {
            if (Math.abs(s) > maxPeak) maxPeak = Math.abs(s)
        }
        if (maxPeak > 1.0f) {
            val scale = 0.95f / maxPeak
            for (idx in output.indices) {
                output[idx] *= scale
            }
        }

        return output
    }

    /**
     * Hermite / Catmull-Rom cubic interpolation for peak reconstruction.
     */
    private fun interpolateCubic(y0: Float, y1: Float, y2: Float, y3: Float, t: Float): Float {
        val a = -0.5f * y0 + 1.5f * y1 - 1.5f * y2 + 0.5f * y3
        val b = y0 - 2.5f * y1 + 2f * y2 - 0.5f * y3
        val c = -0.5f * y0 + 0.5f * y2
        val d = y1
        return a * t * t * t + b * t * t + c * t + d
    }
}
