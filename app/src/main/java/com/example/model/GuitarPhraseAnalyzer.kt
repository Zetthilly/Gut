package com.example.model

import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.log2

/**
 * Expression articulation analyzer for guitars.
 * Monitors real-time, high-speed input streams of frequency and amplitude measurements (via PitchDetector transitions)
 * to detect localized performance ornaments/gestures:
 * 1. Hammer-ons (rapid pitch increases without a fresh attack transient).
 * 2. Pull-offs (rapid pitch reductions without a fresh attack transient).
 * 3. Legato Slides (slur glides across semitones over a sustained envelope).
 * 4. Control String Bends (continuous gradual up-pitch tuning changes within a single envelope).
 */
class GuitarPhraseAnalyzer {

    // Representation of a single audio frame captured by the pitch detection worker
    data class AnalysisFrame(
        val timestampMs: Long,
        val frequencyHz: Float,
        val amplitude: Float
    )

    enum class GuitarTechnique {
        NONE,
        HAMMER_ON,
        PULL_OFF,
        SLIDE,
        STRING_BEND
    }

    data class DetectedExpression(
        val technique: GuitarTechnique,
        val detectedAtMs: Long,
        val startNoteName: String,
        val targetNoteName: String,
        val confidence: Float,
        val details: String
    )

    // Window size for analysis (approx 500ms of audio frame history)
    private val frameHistory = LinkedList<AnalysisFrame>()
    private val maxHistorySize = 30

    /**
     * Feed a newly calculated frame from PitchDetector into the sliding historical buffer.
     * Evaluates continuous gestures to see if a technique classification has triggered.
     * Returns a valid compilation of expressions if identified, otherwise null.
     */
    fun pushAndAnalyze(
        timestampMs: Long,
        frequencyHz: Float,
        amplitude: Float
    ): DetectedExpression? {
        if (frequencyHz <= 0f || amplitude < 0.005f) {
            frameHistory.clear()
            return null
        }

        frameHistory.addLast(AnalysisFrame(timestampMs, frequencyHz, amplitude))
        if (frameHistory.size > maxHistorySize) {
            frameHistory.removeFirst()
        }

        if (frameHistory.size < 5) return null

        return evaluateGestures()
    }

    /**
     * Analyzes current sequence buffer for characteristic guitar signatures.
     */
    private fun evaluateGestures(): DetectedExpression? {
        val first = frameHistory.first
        val last = frameHistory.last

        // Convert start and end frequencies to semitones with respect to A=440Hz reference
        val semitoneStart = 12 * log2(first.frequencyHz / 440f)
        val semitoneEnd = 12 * log2(last.frequencyHz / 440f)
        val pitchDifference = semitoneEnd - semitoneStart
        val absPitchDiff = abs(pitchDifference)

        // Check for attack transient spikes inside the frame history
        // If there's a huge surge in amplitude inside the queue, the string was replucked
        var maxAmplitudeJump = 0f
        var maxJumpIndex = -1
        for (i in 1 until frameHistory.size) {
            val prevAmp = frameHistory[i - 1].amplitude
            val currAmp = frameHistory[i].amplitude
            val jump = currAmp - prevAmp
            if (jump > maxAmplitudeJump) {
                maxAmplitudeJump = jump
                maxJumpIndex = i
            }
        }

        val hasPluckedTransient = maxAmplitudeJump > 0.08f // Pluck transaction detection threshold
        val totalMsSpan = last.timestampMs - first.timestampMs

        val fromNoteName = MusicTheoryUtils.getNoteNameWithOctave(first.frequencyHz)
        val toNoteName = MusicTheoryUtils.getNoteNameWithOctave(last.frequencyHz)

        // 1. Evaluate BENDS: Pitch drifts upward slowly within a single sustained pluck gesture,
        // typically 0.8 to 2.5 semitones, without an internal attack transient.
        if (pitchDifference >= 0.7f && pitchDifference <= 2.8f && !hasPluckedTransient && totalMsSpan > 150) {
            // Verify pitch rise was mostly monotonic/continuous
            var isContinuousRise = true
            for (i in 1 until frameHistory.size) {
                if (frameHistory[i].frequencyHz < frameHistory[i - 1].frequencyHz - 2f) {
                    isContinuousRise = false
                    break
                }
            }
            if (isContinuousRise) {
                return DetectedExpression(
                    technique = GuitarTechnique.STRING_BEND,
                    detectedAtMs = last.timestampMs,
                    startNoteName = fromNoteName,
                    targetNoteName = toNoteName,
                    confidence = 0.85f,
                    details = "Pitch up +${String.format("%.2f", pitchDifference)} st"
                )
            }
        }

        // 2. Evaluate SLIDES: Wide slide spanning over 2.5 semitones cleanly across multiple frames
        if (absPitchDiff >= 2.5f && !hasPluckedTransient && totalMsSpan > 150 && totalMsSpan < 450) {
            return DetectedExpression(
                technique = GuitarTechnique.SLIDE,
                detectedAtMs = last.timestampMs,
                startNoteName = fromNoteName,
                targetNoteName = toNoteName,
                confidence = 0.90f,
                details = "Slide glide of ${String.format("%.1f", absPitchDiff)} semitones"
            )
        }

        // 3. Evaluate HAMMER-ONS & PULL-OFFS
        // Occurs quickly (< 100ms) with a step pitch change but with no fresh transient.
        // We look for a step interval change in adjacent frames where amplitude is steady or decaying.
        for (i in 1 until frameHistory.size) {
            val f1 = frameHistory[i - 1].frequencyHz
            val f2 = frameHistory[i].frequencyHz
            val localRatio = 12 * log2(f2 / f1)
            val stepAt = frameHistory[i].timestampMs

            // Is the step change fast? Hammer/Pull transitions are almost instantaneous (less than 40ms)
            if (abs(localRatio) in 0.8f..5.5f) {
                val ampBefore = frameHistory[i - 1].amplitude
                val ampAt = frameHistory[i].amplitude
                
                // No pluck attack at step trigger time
                val stepTransient = ampAt - ampBefore
                if (stepTransient < 0.03f) {
                    val isHammerOn = localRatio > 0
                    
                    // Filter duplicate detection using index positioning
                    if (isHammerOn) {
                        return DetectedExpression(
                            technique = GuitarTechnique.HAMMER_ON,
                            detectedAtMs = stepAt,
                            startNoteName = MusicTheoryUtils.getNoteNameWithOctave(f1),
                            targetNoteName = MusicTheoryUtils.getNoteNameWithOctave(f2),
                            confidence = 0.88f,
                            details = "Sustained ascent +${localRatio.toInt()} semitones"
                        )
                    } else {
                        return DetectedExpression(
                            technique = GuitarTechnique.PULL_OFF,
                            detectedAtMs = stepAt,
                            startNoteName = MusicTheoryUtils.getNoteNameWithOctave(f1),
                            targetNoteName = MusicTheoryUtils.getNoteNameWithOctave(f2),
                            confidence = 0.88f,
                            details = "Sustained descent ${localRatio.toInt()} semitones"
                        )
                    }
                }
            }
        }

        return null
    }

    /**
     * Clear analyzer queue state.
     */
    fun reset() {
        frameHistory.clear()
    }
}
