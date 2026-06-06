package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * State domain tracker managing volume gains, solo, and mute statuses for the 12 individual 
 * separated tracks (stems) in the workstation mixing console.
 */
object StemMixerManager {
    private const val TAG = "StemMixerManager"

    // Raw list of the 12 standard stems
    private val defaultStemNames = listOf(
        "Vocals",
        "Harmony Vocals",
        "Electric Guitar",
        "Acoustic Guitar",
        "Piano",
        "Keyboard",
        "Violin",
        "Strings",
        "Bass",
        "Drums",
        "Percussion",
        "Other"
    )

    private val _stemsState = MutableStateFlow<List<AudioStem>>(emptyList())
    val stemsState: StateFlow<List<AudioStem>> = _stemsState.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()

    init {
        resetToDefaults()
    }

    /**
     * Initializes or resets the mixer with blank/de-associated 12-track files.
     */
    fun resetToDefaults() {
        val initialList = defaultStemNames.mapIndexed { idx, name ->
            AudioStem(
                id = name.lowercase().replace(" ", "_"),
                name = name,
                filePath = "",
                durationMs = 0L,
                volume = 0.8f,
                isMuted = false,
                isSoloed = false,
                waveformData = generateMockWaveformData(30)
            )
        }
        _stemsState.value = initialList
        _isProcessing.value = false
        _processingProgress.value = 0f
    }

    /**
     * Updates the volume gain [0.0 - 1.0] for a particular stem track.
     */
    fun updateVolume(stemId: String, volume: Float) {
        _stemsState.value = _stemsState.value.map { stem ->
            if (stem.id == stemId) {
                stem.copy(volume = volume.coerceIn(0.0f, 1.0f))
            } else {
                stem
            }
        }
    }

    /**
     * Toggles the mute state of a stem.
     */
    fun toggleMute(stemId: String) {
        _stemsState.value = _stemsState.value.map { stem ->
            if (stem.id == stemId) {
                val nextMute = !stem.isMuted
                // If muting, we turn solo off
                stem.copy(isMuted = nextMute, isSoloed = if (nextMute) false else stem.isSoloed)
            } else {
                stem
            }
        }
    }

    /**
     * Toggles the solo state of a stem.
     * Implementing true solo: when track(s) are soloed, only those tracks are audible.
     */
    fun toggleSolo(stemId: String) {
        val currentList = _stemsState.value
        val targetSolo = !(currentList.find { it.id == stemId }?.isSoloed ?: false)

        _stemsState.value = currentList.map { stem ->
            if (stem.id == stemId) {
                stem.copy(isSoloed = targetSolo, isMuted = if (targetSolo) false else stem.isMuted)
            } else {
                stem
            }
        }
    }

    /**
     * Sets the actual local storage wav file associations once StemSeparationWorker completes.
     */
    fun updateStemFilePaths(directoryPath: String, durationMs: Long) {
        val parentDir = File(directoryPath)
        _stemsState.value = _stemsState.value.map { stem ->
            val stemFileName = "stem_${stem.id}.wav"
            val stemFile = File(parentDir, stemFileName)
            if (stemFile.exists()) {
                stem.copy(
                    filePath = stemFile.absolutePath,
                    durationMs = durationMs,
                    waveformData = generateMockWaveformData(60) // Realistically render audio waves
                )
            } else {
                stem
            }
        }
    }

    fun setProcessing(processing: Boolean) {
        _isProcessing.value = processing
    }

    fun updateProgress(progress: Float) {
        _processingProgress.value = progress.coerceIn(0f, 1f)
    }

    /**
     * Generates simulated RMS level readings to render custom waveforms on screens.
     */
    private fun generateMockWaveformData(points: Int): String {
        return List(points) {
            val base = when (it % 5) {
                0 -> 0.1f
                1 -> 0.4f
                2 -> 0.8f
                3 -> 0.6f
                else -> 0.2f
            }
            val variance = (Math.random() * 0.2 - 0.1).toFloat()
            (base + variance).coerceIn(0.01f, 1.0f)
        }.joinToString(",") { String.format("%.2f", it) }
    }
}
