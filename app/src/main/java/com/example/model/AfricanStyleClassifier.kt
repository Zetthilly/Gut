package com.example.model

import kotlin.math.absoluteValue

/**
 * Result data class representing identified traditional African musical style parameters.
 */
data class StyleMatchResult(
    val styleName: String,
    val confidence: Float,          // Range from [0.0 - 1.0]
    val description: String,        // Cultural/Structural profile description
    val recommendedTempoBpm: Int
)

/**
 * Heuristic Pattern Recognition matrix analyzing live/recorded midi sequences 
 * for traditional Sub-Saharan music production styles including Sungura, Jit, Soukous, and Rhumba.
 */
object AfricanStyleClassifier {

    /**
     * Analyzes notes from the sliding temporal window to match against stylistic rules.
     */
    fun classifyStyle(notes: List<NoteEvent>): StyleMatchResult {
        if (notes.size < 4) {
            return StyleMatchResult(
                styleName = "Undecided Session",
                confidence = 0.1f,
                description = "Input at least 4 contiguous notes to initiate style heuristic analyzer.",
                recommendedTempoBpm = 100
            )
        }

        // 1. Calculate General Feature Metrics
        val pitches = notes.map { it.midiNote }
        val times = notes.map { it.timestampMs }
        
        val avgMidiNote = pitches.average()
        
        // Duration intervals between consecutive strikes
        val intervals = times.zipWithNext { a, b -> (b - a).absoluteValue }
        val avgIntervalMs = if (intervals.isNotEmpty()) intervals.average() else 1000.0
        val tempoEstimateBpm = if (avgIntervalMs > 0.0) {
            (60000.0 / avgIntervalMs).coerceIn(60.0, 200.0).toInt()
        } else {
            120
        }

        // Semitone intervals between elements
        val noteJumps = pitches.zipWithNext { a, b -> (b - a).absoluteValue }
        val avgNoteJump = if (noteJumps.isNotEmpty()) noteJumps.average() else 0.0

        // Determine Register bounds
        val isTrebleHeavy = avgMidiNote >= 72 // MIDI Note 72 is C5
        val isBassHeavy = avgMidiNote <= 48   // MIDI Note 48 is C3

        // 2. Structural Heuristic Matching Matrix
        return when {
            // A. Sungura Lead Guitar (Fast, high-register arpeggio rolls, rapid repeating major scales, Zimbabwe)
            isTrebleHeavy && tempoEstimateBpm >= 130 && avgNoteJump in 1.5..4.0 -> {
                val matchScore = (0.7f + (tempoEstimateBpm - 130) * 0.002f + (4f - (avgNoteJump.toFloat() - 1.5f)) * 0.05f)
                    .coerceIn(0.6f, 0.98f)
                StyleMatchResult(
                    styleName = "Zimbabwean Sungura Roll",
                    confidence = matchScore,
                    description = "Fast treble register lead guitar arpeggios consisting of high-speed repeating triadic intervals and continuous dynamic scale runs.",
                    recommendedTempoBpm = 145
                )
            }

            // B. Jit Syncopated Beats (Super fast staccato, uniform interval spacing, Zimbabwe/Central Africa)
            tempoEstimateBpm >= 140 && !isBassHeavy && avgNoteJump in 0.5..3.0 -> {
                // Low interval variation indicates linear, interlocking single-note runs
                val matchScore = (0.65f + (tempoEstimateBpm - 140) * 0.003f).coerceIn(0.6f, 0.95f)
                StyleMatchResult(
                    styleName = "Jit Fast Staccato",
                    confidence = matchScore,
                    description = "Ultra high-tempo interlocking staccato guitar runs, heavily rhythmic with highly uniform, small interval progression bounds.",
                    recommendedTempoBpm = 160
                )
            }

            // C. Soukous Seben Bass Step (Bouncing bass/low register, jumping intervals, Root-Fifth-Octave-Root loop, Congo)
            isBassHeavy && avgNoteJump >= 4.5 -> {
                val matchScore = (0.75f + (avgNoteJump.toFloat() - 4.5f) * 0.03f).coerceIn(0.7f, 0.97f)
                StyleMatchResult(
                    styleName = "Congolese Soukous Bass",
                    confidence = matchScore,
                    description = "Bouncing classic 'Seben' lower register counterpoint utilizing syncopated wide interval jumps (Root to Perfect Fifth/Octave) to propel high energy dance grooves.",
                    recommendedTempoBpm = 125
                )
            }

            // D. Rhumba Syncopated Lines (Slower tempo, lyrical melody runs, syncopated step offsets)
            tempoEstimateBpm in 75..115 && avgNoteJump in 2.0..6.0 -> {
                val matchScore = 0.82f - ((tempoEstimateBpm - 90).absoluteValue * 0.002f)
                StyleMatchResult(
                    styleName = "Classic Congolese Rhumba",
                    confidence = matchScore.coerceIn(0.65f, 0.93f),
                    description = "Mid-tempo syncopated, expressive guitar phrases and melodic triad sweeps moving fluidly across vocal tracks.",
                    recommendedTempoBpm = 90
                )
            }

            // E. Generic Afrobeats Syncopation (Catch-all modern sub-Saharan rhythmic balance)
            else -> {
                val matchScore = if (tempoEstimateBpm in 100..128) 0.72f else 0.55f
                StyleMatchResult(
                    styleName = "Syncopated Afro-Groove",
                    confidence = matchScore,
                    description = "Modern syncopated rhythmic structure featuring accent patterns emphasizing downbeats and minor pentatonic melodic loops.",
                    recommendedTempoBpm = 116
                )
            }
        }
    }
}
