package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Models a chord detected by local AI inference (TFLite LSTM / ONNX) at a specific time signature.
 */
@Entity(tableName = "detected_chords")
data class DetectedChord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stemId: String,             // Reference to the source audio stem
    val timestampMs: Long,          // Time location of chord in millisecond timeline
    val chordSymbol: String,        // e.g., "Cmaj7", "Am", "F", "G", "D7"
    val confidence: Float,          // Probability score [0.0 - 1.0] from local model output layer
    val midiNotes: String = ""      // Comma-separated MIDI note values being played, e.g. "60,64,67,71"
) {
    /**
     * Parses the comma-separated MIDI notes string.
     */
    fun getMidiNotesList(): List<Int> {
        if (midiNotes.isEmpty()) return emptyList()
        return try {
            midiNotes.split(",").mapNotNull { it.trim().toIntOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
