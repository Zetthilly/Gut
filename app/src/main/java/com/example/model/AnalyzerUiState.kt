package com.example.model

/**
 * Supported navigation tab routes in the HZ Chord AI workstation.
 */
enum class WorkstationTab {
    LIVE, STEMS, TIMELINE, SETTINGS
}

/**
 * Type-safe state configuration capturing everything dynamically rendered
 * across the workstation's real-time pitch, stem-separated, and chord-tracked views.
 */
data class AnalyzerUiState(
    val currentTab: String = "DASHBOARD",                     // DASHBOARD, MATRIX, TIMELINE, VOICINGS, THEORY, SESSIONS
    val isPlaying: Boolean = false,
    val currentProgressMs: Long = 0L,
    val totalDurationMs: Long = 120000L,
    
    // Music Theory & Transposition Settings
    val selectedKey: String = "C",
    val transposeSemitones: Int = 0,
    val useFlatNaming: Boolean = false,                 // Flat vs Sharp styling preference
    
    // Real-Time Pitch Tracking Indicators
    val livePitchHz: Float = 0f,
    val liveChordSymbol: String = "None",
    val liveChromagram: FloatArray = FloatArray(12) { 0.05f },
    
    // Arpeggio and African Style Recognition Stats
    val activeArpeggioTag: String = "Undecided",
    val styleName: String = "Acoustics offline",
    val styleConfidence: Float = 0.0f,
    val styleDescription: String = "Strike chord triggers or activate microphone input.",
    val recommendedTempoBpm: Int = 110,
    val impliedHarmony: String = "None",
    val activeNotes: List<NoteEvent> = emptyList(),

    // Timeline Analysis Results
    val detectedChords: List<DetectedChord> = emptyList(),
    val activeChordIndex: Int = 0,

    // Background AI Stem Separation Workspace Details
    val stems: List<AudioStem> = emptyList(),
    val isSeparating: Boolean = false,
    val isRecording: Boolean = false,
    val separationProgress: Float = 0f,

    // Local Persistence configuration states
    val tuningChoice: String = "Standard A=440Hz",
    val savedSessions: List<com.example.db.SessionEntity> = emptyList(),

    // Live Guitar Phrase/Gestures and Restoration properties
    val lastDetectedGuitarPhrase: String = "No dynamic articulation",
    val guitarPhraseHistory: List<String> = emptyList(),
    val restorationsApplied: List<String> = emptyList(),
    val isRestoring: Boolean = false,

    // Export tracking properties
    val exportType: String? = null,
    val exportProgress: Float = 0f,
    val exportStatusMessage: String = "",
    val showExportIndicator: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyzerUiState) return false

        if (currentTab != other.currentTab) return false
        if (isPlaying != other.isPlaying) return false
        if (currentProgressMs != other.currentProgressMs) return false
        if (totalDurationMs != other.totalDurationMs) return false
        if (selectedKey != other.selectedKey) return false
        if (transposeSemitones != other.transposeSemitones) return false
        if (useFlatNaming != other.useFlatNaming) return false
        if (livePitchHz != other.livePitchHz) return false
        if (liveChordSymbol != other.liveChordSymbol) return false
        if (!liveChromagram.contentEquals(other.liveChromagram)) return false
        if (activeArpeggioTag != other.activeArpeggioTag) return false
        if (styleName != other.styleName) return false
        if (styleConfidence != other.styleConfidence) return false
        if (styleDescription != other.styleDescription) return false
        if (recommendedTempoBpm != other.recommendedTempoBpm) return false
        if (impliedHarmony != other.impliedHarmony) return false
        if (activeNotes != other.activeNotes) return false
        if (detectedChords != other.detectedChords) return false
        if (activeChordIndex != other.activeChordIndex) return false
        if (stems != other.stems) return false
        if (isSeparating != other.isSeparating) return false
        if (isRecording != other.isRecording) return false
        if (separationProgress != other.separationProgress) return false
        if (tuningChoice != other.tuningChoice) return false
        if (savedSessions != other.savedSessions) return false
        if (lastDetectedGuitarPhrase != other.lastDetectedGuitarPhrase) return false
        if (guitarPhraseHistory != other.guitarPhraseHistory) return false
        if (restorationsApplied != other.restorationsApplied) return false
        if (isRestoring != other.isRestoring) return false
        if (exportType != other.exportType) return false
        if (exportProgress != other.exportProgress) return false
        if (exportStatusMessage != other.exportStatusMessage) return false
        if (showExportIndicator != other.showExportIndicator) return false

        return true
    }

    override fun hashCode(): Int {
        var result = currentTab.hashCode()
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + currentProgressMs.hashCode()
        result = 31 * result + totalDurationMs.hashCode()
        result = 31 * result + selectedKey.hashCode()
        result = 31 * result + transposeSemitones
        result = 31 * result + useFlatNaming.hashCode()
        result = 31 * result + livePitchHz.hashCode()
        result = 31 * result + liveChordSymbol.hashCode()
        result = 31 * result + liveChromagram.contentHashCode()
        result = 31 * result + activeArpeggioTag.hashCode()
        result = 31 * result + styleName.hashCode()
        result = 31 * result + styleConfidence.hashCode()
        result = 31 * result + styleDescription.hashCode()
        result = 31 * result + recommendedTempoBpm
        result = 31 * result + impliedHarmony.hashCode()
        result = 31 * result + activeNotes.hashCode()
        result = 31 * result + detectedChords.hashCode()
        result = 31 * result + activeChordIndex
        result = 31 * result + stems.hashCode()
        result = 31 * result + isSeparating.hashCode()
        result = 31 * result + isRecording.hashCode()
        result = 31 * result + separationProgress.hashCode()
        result = 31 * result + tuningChoice.hashCode()
        result = 31 * result + savedSessions.hashCode()
        result = 31 * result + lastDetectedGuitarPhrase.hashCode()
        result = 31 * result + guitarPhraseHistory.hashCode()
        result = 31 * result + restorationsApplied.hashCode()
        result = 31 * result + isRestoring.hashCode()
        result = 31 * result + (exportType?.hashCode() ?: 0)
        result = 31 * result + exportProgress.hashCode()
        result = 31 * result + exportStatusMessage.hashCode()
        result = 31 * result + showExportIndicator.hashCode()
        return result
    }
}
