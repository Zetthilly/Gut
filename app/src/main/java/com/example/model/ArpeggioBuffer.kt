package com.example.model

import kotlin.math.absoluteValue

/**
 * Represents an individual monophonic or polyphonic note event in the sliding temporal window.
 */
data class NoteEvent(
    val midiNote: Int,
    val timestampMs: Long,
    val velocity: Int = 80
) {
    val noteName: String
        get() = MusicTheoryUtils.sharps[midiNote % 12]
}

/**
 * Stateful sliding window processor that holds sequential musical notes in memory 
 * (typically within a 1.5s - 3.0s window) to deduce implied overarching harmony and chords 
 * from arpeggiated lines, scale runs, or bass walks.
 */
class ArpeggioBuffer(
    private val windowDurationMs: Long = 2500L
) {
    private val notes = mutableListOf<NoteEvent>()

    /**
     * Adds a newly detected note to the memory buffer and prunes stale historical events.
     */
    @Synchronized
    fun addNote(midiNote: Int, timestampMs: Long) {
        notes.add(NoteEvent(midiNote, timestampMs))
        pruneStaleNotes(timestampMs)
    }

    /**
     * Prunes notes that have fallen outside the sliding temporal window.
     */
    @Synchronized
    fun pruneStaleNotes(currentTimestampMs: Long) {
        val boundary = currentTimestampMs - windowDurationMs
        notes.removeAll { it.timestampMs < boundary }
    }

    /**
     * Returns a copy of currently active note events in chronological order.
     */
    @Synchronized
    fun getActiveNotes(): List<NoteEvent> = notes.toList()

    /**
     * Clears all notes in the current tracking session.
     */
    @Synchronized
    fun clear() {
        notes.clear()
    }

    /**
     * Analyzes chronological sequential intervals to detect scale runs vs. chordal arpeggios.
     */
    @Synchronized
    fun analyzeImpliedHarmony(): String {
        if (notes.size < 3) return "Undecided"

        val uniquePitchClasses = notes.map { it.midiNote % 12 }.distinct().sorted()
        
        // Match unique active note classes against common triads using chromatic indexes
        for (root in 0..11) {
            val majorTriad = listOf(root, (root + 4) % 12, (root + 7) % 12).sorted()
            val minorTriad = listOf(root, (root + 3) % 12, (root + 7) % 12).sorted()
            val major7th = listOf(root, (root + 4) % 12, (root + 7) % 12, (root + 11) % 12).sorted()
            val minor7th = listOf(root, (root + 3) % 12, (root + 7) % 12, (root + 10) % 12).sorted()
            val dom7th = listOf(root, (root + 4) % 12, (root + 7) % 12, (root + 10) % 12).sorted()

            val rootName = MusicTheoryUtils.sharps[root]

            if (uniquePitchClasses.containsAll(major7th)) return "${rootName}maj7 (Arpeggiated)"
            if (uniquePitchClasses.containsAll(minor7th)) return "${rootName}m7 (Arpeggiated)"
            if (uniquePitchClasses.containsAll(dom7th)) return "${rootName}7 (Arpeggiated)"
            if (uniquePitchClasses.containsAll(majorTriad)) return "$rootName (Arpeggiated)"
            if (uniquePitchClasses.containsAll(minorTriad)) return "${rootName}m (Arpeggiated)"
        }

        // Detect if the pattern represents a linear Scale Run (predominantly 1st/2nd intervals)
        val consecutiveIntervals = notes.zipWithNext { a: NoteEvent, b: NoteEvent -> (b.midiNote - a.midiNote).absoluteValue }
        val avgInterval = if (consecutiveIntervals.isNotEmpty()) consecutiveIntervals.average() else 0.0
        
        return if (avgInterval in 1.0..2.2) {
            "Scale Walk / Run"
        } else if (avgInterval in 3.0..5.0) {
            "Walking Bassline"
        } else {
            "Complex Polyphonic Row"
        }
    }
}
