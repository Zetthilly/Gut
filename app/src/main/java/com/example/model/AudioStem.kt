package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an isolated audio stem extracted offline (e.g., via ONNX-based demixing)
 * or imported into the HZ CHORD AI workstation.
 */
@Entity(tableName = "audio_stems")
data class AudioStem(
    @PrimaryKey 
    val id: String,
    val name: String,               // e.g., "Vocals", "Drums", "Bass", "Piano", "Full Mix"
    val filePath: String,           // Paths to local storage WAV/RAW files
    val durationMs: Long,
    val volume: Float = 0.8f,       // Slider value [0.0 - 1.0] for the mixing board
    val isMuted: Boolean = false,
    val isSoloed: Boolean = false,
    val waveformData: String = ""   // Comma-separated RMS amplitude values for rendering wave tracks
) {
    /**
     * Parses the waveformData string into a List of Floats for rendering on the UI.
     */
    fun getWaveformList(): List<Float> {
        if (waveformData.isEmpty()) return emptyList()
        return try {
            waveformData.split(",").map { it.toFloatOrNull() ?: 0.0f }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
