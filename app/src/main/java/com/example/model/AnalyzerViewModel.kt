package com.example.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.db.HzChordAiDatabase
import com.example.db.SessionEntity
import com.example.db.ChordTimelineEntity
import com.example.db.SessionRepository
import com.example.db.SessionRepositoryImpl
import com.example.db.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Flagship Centralized MVVM State Machine Driver.
 * Integrates real-time native Audio Capturing systems, background AI workers, 
 * and high-precision media synchronization via Jetpack Media3/ExoPlayer.
 */
class AnalyzerViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AnalyzerViewModel"

    private val _uiState = MutableStateFlow(AnalyzerUiState())
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    // Engines & Memory Buffers
    private val pitchDetector = PitchDetector()
    private val arpeggioBuffer = ArpeggioBuffer(windowDurationMs = 3000L)
    private val guitarAnalyzer = GuitarPhraseAnalyzer()
    private val restorationEngine = AudioRestorationEngine()

    // SQLite Room persistence and DataStore engines
    private val database = HzChordAiDatabase.getDatabase(application)
    private val repository: SessionRepository = SessionRepositoryImpl(database.sessionDao())
    private val preferencesManager = PreferencesManager(application)

    // Media3 Playback Core
    private var exoPlayer: ExoPlayer? = null
    private var timelineSyncJob: Job? = null
    
    // Default chord layout structure
    private val defaultTimelineChords = listOf(
        DetectedChord(id = 1, stemId = "stem_harmonic", timestampMs = 0L, chordSymbol = "Cmaj7", confidence = 0.97f),
        DetectedChord(id = 2, stemId = "stem_harmonic", timestampMs = 15000L, chordSymbol = "Am7", confidence = 0.94f),
        DetectedChord(id = 3, stemId = "stem_harmonic", timestampMs = 30000L, chordSymbol = "Dm7", confidence = 0.91f),
        DetectedChord(id = 4, stemId = "stem_harmonic", timestampMs = 45000L, chordSymbol = "G7", confidence = 0.96f),
        DetectedChord(id = 5, stemId = "stem_harmonic", timestampMs = 60000L, chordSymbol = "Em7", confidence = 0.89f),
        DetectedChord(id = 6, stemId = "stem_harmonic", timestampMs = 75000L, chordSymbol = "A7", confidence = 0.92f),
        DetectedChord(id = 7, stemId = "stem_harmonic", timestampMs = 90000L, chordSymbol = "Fmaj7", confidence = 0.95f),
        DetectedChord(id = 8, stemId = "stem_harmonic", timestampMs = 105000L, chordSymbol = "G/B", confidence = 0.88f)
    )

    private var micTrackingJob: Job? = null
    private var isMicStreaming = false

    init {
        // Initial setup
        _uiState.update { 
            it.copy(
                detectedChords = defaultTimelineChords,
                stems = StemMixerManager.stemsState.value
            )
        }

        // Connect global Stem Mix states to local UI status flow
        viewModelScope.launch {
            StemMixerManager.stemsState.collect { stemsList ->
                _uiState.update { it.copy(stems = stemsList) }
            }
        }
        viewModelScope.launch {
            StemMixerManager.isProcessing.collect { separating ->
                _uiState.update { it.copy(isSeparating = separating) }
            }
        }
        viewModelScope.launch {
            StemMixerManager.processingProgress.collect { progress ->
                _uiState.update { it.copy(separationProgress = progress) }
            }
        }

        // Collect preferences from DataStore
        viewModelScope.launch {
            preferencesManager.useFlatNamingFlow.collect { useFlat ->
                _uiState.update { it.copy(useFlatNaming = useFlat) }
                updateTranspose(_uiState.value.transposeSemitones)
            }
        }
        viewModelScope.launch {
            preferencesManager.tuningChoiceFlow.collect { tuning ->
                _uiState.update { it.copy(tuningChoice = tuning) }
            }
        }
        viewModelScope.launch {
            preferencesManager.visualLayoutModeFlow.collect { layoutMode ->
                _uiState.update { it.copy(currentTab = layoutMode) }
            }
        }

        // Collect saved sessions from local database
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions ->
                _uiState.update { it.copy(savedSessions = sessions) }
            }
        }

        initializePlayer()
    }

    /**
     * Set active workstation view navigation tab.
     */
    fun selectTab(tab: String) {
        _uiState.update { it.copy(currentTab = tab.uppercase()) }
    }

    /**
     * Set target reference key for Roman Numeral chord conversions.
     */
    fun selectKey(key: String) {
        _uiState.update { it.copy(selectedKey = key) }
    }

    /**
     * Initializes Media3 ExoPlayer with custom robust callbacks and fallback loops.
     */
    private fun initializePlayer() {
        try {
            exoPlayer = ExoPlayer.Builder(getApplication()).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = false
                
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.update { it.copy(isPlaying = isPlaying) }
                        if (isPlaying) {
                            startTimelineSync()
                        } else {
                            stopTimelineSync()
                        }
                    }
                })
            }
            Log.i(TAG, "Media3 ExoPlayer initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "ExoPlayer creation failed (likely headless or restricted environment). Utilizing internal high-fidelity tracker.", e)
        }
    }

    /**
     * Start/Resume master playback.
     */
    fun togglePlayback() {
        val nextPlaying = !_uiState.value.isPlaying
        if (exoPlayer != null) {
            if (nextPlaying) {
                // If there's an actual file to play, set it up. Otherwise we simulate.
                val masterStem = _uiState.value.stems.find { it.id == "stem_mix" || it.id == "vocals" }
                if (masterStem != null && masterStem.filePath.isNotEmpty() && File(masterStem.filePath).exists()) {
                    exoPlayer?.let { player ->
                        if (player.mediaItemCount == 0) {
                            player.setMediaItem(MediaItem.fromUri(masterStem.filePath))
                            player.prepare()
                        }
                    }
                }
                exoPlayer?.play()
            } else {
                exoPlayer?.pause()
            }
        } else {
            // High fidelity coroutine simulation mode
            _uiState.update { it.copy(isPlaying = nextPlaying) }
            if (nextPlaying) {
                startTimelineSync()
            } else {
                stopTimelineSync()
            }
        }
    }

    /**
     * Seek the timeline workstation to a specific millisecond segment.
     */
    fun seekTo(positionMs: Long) {
        if (exoPlayer != null) {
            exoPlayer?.seekTo(positionMs)
            _uiState.update { it.copy(currentProgressMs = positionMs) }
            updateSelectedChordIndex(positionMs)
        } else {
            _uiState.update { it.copy(currentProgressMs = positionMs.coerceIn(0L, _uiState.value.totalDurationMs)) }
            updateSelectedChordIndex(positionMs)
        }
    }

    /**
     * Launch high-precision synchronizing routine mapping chord steps and timeline metrics.
     */
    private fun startTimelineSync() {
        timelineSyncJob?.cancel()
        timelineSyncJob = viewModelScope.launch {
            while (true) {
                val currentPos = exoPlayer?.currentPosition ?: (_uiState.value.currentProgressMs + 100L)
                val wrappedPos = currentPos % _uiState.value.totalDurationMs
                
                _uiState.update { it.copy(currentProgressMs = wrappedPos) }
                updateSelectedChordIndex(wrappedPos)
                
                delay(100)
            }
        }
    }

    private fun stopTimelineSync() {
        timelineSyncJob?.cancel()
        timelineSyncJob = null
    }

    private fun updateSelectedChordIndex(positionMs: Long) {
        val chords = _uiState.value.detectedChords
        if (chords.isEmpty()) return
        
        // Find segment index (Each chord spans 15 seconds)
        val chordIntervalMs = 15000L
        val activeIdx = (positionMs / chordIntervalMs).toInt().coerceIn(0, chords.lastIndex)
        _uiState.update { it.copy(activeChordIndex = activeIdx) }
    }

    /**
     * Real-time semitone transpose configuration.
     */
    fun updateTranspose(semitones: Int) {
        val boundSemitones = semitones.coerceIn(-12, 12)
        _uiState.update { currentState ->
            val transposed = defaultTimelineChords.map { chord ->
                val shiftedSymbol = MusicTheoryUtils.transposeChord(chord.chordSymbol, boundSemitones)
                // Normalize Flat vs Sharp names based on naming preferences
                val stylizedSymbol = if (currentState.useFlatNaming) {
                    convertSharpsToFlats(shiftedSymbol)
                } else {
                    convertFlatsToSharps(shiftedSymbol)
                }
                chord.copy(chordSymbol = stylizedSymbol)
            }
            currentState.copy(
                transposeSemitones = boundSemitones,
                detectedChords = transposed
            )
        }
    }

    /**
     * Toggle between Flat (Db, Eb) and Sharp (C#, D#) nomenclature conventions.
     */
    fun toggleNamingConvention() {
        val nextFlat = !_uiState.value.useFlatNaming
        viewModelScope.launch {
            preferencesManager.setUseFlatNaming(nextFlat)
        }
    }

    private fun convertSharpsToFlats(symbol: String): String {
        var output = symbol
        MusicTheoryUtils.sharps.forEachIndexed { i, sharp ->
            if (sharp.endsWith("#")) {
                output = output.replace(sharp, MusicTheoryUtils.flats[i])
            }
        }
        return output
    }

    private fun convertFlatsToSharps(symbol: String): String {
        var output = symbol
        MusicTheoryUtils.flats.forEachIndexed { i, flat ->
            if (flat.endsWith("b")) {
                output = output.replace(flat, MusicTheoryUtils.sharps[i])
            }
        }
        return output
    }

    /**
     * Toggle live microphone capturing session and start/stop the pitch tracking loops.
     */
    fun toggleMicrophone(onGranted: () -> Unit) {
        if (isMicStreaming) {
            stopMicrophone()
        } else {
            onGranted()
            startMicrophone()
        }
    }

    private fun startMicrophone() {
        if (isMicStreaming) return
        isMicStreaming = true
        pitchDetector.start()
        guitarAnalyzer.reset()
        _uiState.update { it.copy(isRecording = true) }

        micTrackingJob = viewModelScope.launch {
            while (isMicStreaming) {
                val chroma = pitchDetector.chromagram.value
                val pitch = pitchDetector.currentPitchHz.value
                val symbol = pitchDetector.detectedChordSymbol.value

                // Stream into localized guitar phrase analyzer
                val maxAmp = chroma.maxOrNull() ?: 0.05f
                val expression = guitarAnalyzer.pushAndAnalyze(
                    timestampMs = System.currentTimeMillis(),
                    frequencyHz = pitch,
                    amplitude = maxAmp
                )

                _uiState.update { currentState ->
                    val updatedHistory = if (expression != null) {
                        val exprText = "[${expression.technique.name}] ${expression.startNoteName} ➔ ${expression.targetNoteName} (${expression.details})"
                        (listOf(exprText) + currentState.guitarPhraseHistory).take(10)
                    } else {
                        currentState.guitarPhraseHistory
                    }
                    currentState.copy(
                        liveChromagram = chroma,
                        livePitchHz = pitch,
                        liveChordSymbol = symbol,
                        lastDetectedGuitarPhrase = expression?.let { "${it.technique.name}: ${it.startNoteName} ➔ ${it.targetNoteName}" } ?: currentState.lastDetectedGuitarPhrase,
                        guitarPhraseHistory = updatedHistory,
                        isRecording = true
                    )
                }
                delay(33) // 30 FPS Update Polling
            }
        }
    }

    private fun stopMicrophone() {
        isMicStreaming = false
        pitchDetector.stop()
        micTrackingJob?.cancel()
        micTrackingJob = null
        _uiState.update {
            it.copy(
                liveChromagram = FloatArray(12) { 0.05f },
                livePitchHz = 0f,
                liveChordSymbol = "None",
                isRecording = false
            )
        }
    }

    /**
     * Interactive simulated keyboard strikes that interface with the Arpeggio sequential engine.
     */
    fun strikeSimulationNote(midiNote: Int) {
        val now = System.currentTimeMillis()
        arpeggioBuffer.addNote(midiNote, now)
        arpeggioBuffer.pruneStaleNotes(now)
        
        val active = arpeggioBuffer.getActiveNotes()
        val implied = arpeggioBuffer.analyzeImpliedHarmony()
        val classification = AfricanStyleClassifier.classifyStyle(active)

        // Stream simulated frequency to guitar gesture analyzer
        val simulatedFreqHz = 440f * Math.pow(2.0, (midiNote - 69).toDouble() / 12.0).toFloat()
        val expression = guitarAnalyzer.pushAndAnalyze(now, simulatedFreqHz, 0.25f)

        _uiState.update { currentState ->
            val updatedHistory = if (expression != null) {
                val exprText = "[${expression.technique.name}] ${expression.startNoteName} ➔ ${expression.targetNoteName} (${expression.details})"
                (listOf(exprText) + currentState.guitarPhraseHistory).take(10)
            } else {
                currentState.guitarPhraseHistory
            }
            currentState.copy(
                activeNotes = active,
                impliedHarmony = implied,
                styleName = classification.styleName,
                styleConfidence = classification.confidence,
                styleDescription = classification.description,
                recommendedTempoBpm = classification.recommendedTempoBpm,
                lastDetectedGuitarPhrase = expression?.let { "${it.technique.name}: ${it.startNoteName} ➔ ${it.targetNoteName}" } ?: currentState.lastDetectedGuitarPhrase,
                guitarPhraseHistory = updatedHistory
            )
        }
    }

    /**
     * Executes digital audio restoration loops using our high-fidelity AudioRestorationEngine modules.
     */
    fun runAudioRestoration(filterType: String) {
        if (_uiState.value.isRestoring) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            
            // 1. Generate standard mockup distorted waveforms to demonstrate actual DSP mathematically
            val sampleCount = 4410
            val testNoiseSamples = FloatArray(sampleCount) { index ->
                val t = index.toFloat() / 44100f
                val signalVal = kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * t).toFloat()
                val humVal = 0.15f * kotlin.math.sin(2.0 * kotlin.math.PI * 60.0 * t).toFloat()
                val noiseVal = 0.2f * (Math.random().toFloat() - 0.5f)
                (signalVal + humVal + noiseVal) * 1.5f
            }

            delay(1200) // Simulate processing lag

            // 2. Perform the actual high-performance DSP filtering corresponding to selected action
            val restoredResult = when (filterType) {
                "HUM_REMOVAL" -> restorationEngine.removeGroundHum(testNoiseSamples, targetFrequency = 60f)
                "NOISE_REDUCTION" -> restorationEngine.reduceNoise(testNoiseSamples, thresholdDb = -35f)
                "CLIPPING_REPAIR" -> restorationEngine.repairClipping(testNoiseSamples)
                else -> testNoiseSamples
            }

            _uiState.update { currentState ->
                val newRestorations = currentState.restorationsApplied + "$filterType (${restoredResult.size} samples filtered)"
                currentState.copy(
                    isRestoring = false,
                    restorationsApplied = newRestorations
                )
            }
        }
    }

    /**
     * Clears simulated MIDI buffer history.
     */
    fun clearSimulationBuffer() {
        arpeggioBuffer.clear()
        _uiState.update {
            it.copy(
                activeNotes = emptyList(),
                impliedHarmony = "Undecided",
                styleName = "Acoustics offline",
                styleConfidence = 0.0f,
                styleDescription = "Strike chord triggers or activate microphone input."
            )
        }
    }

    /**
     * Trigger Background Local AI 12-Stem separation routing.
     */
    fun triggerStemSeparation() {
        if (_uiState.value.isSeparating) return
        val workRequest = OneTimeWorkRequestBuilder<StemSeparationWorker>()
            .setInputData(workDataOf("input_file_path" to "/local/raw/master.wav"))
            .build()
        WorkManager.getInstance(getApplication()).enqueue(workRequest)
    }

    /**
     * Save currently tracked chords into the local SQLite database.
     */
    fun saveCurrentSession(title: String) {
        viewModelScope.launch {
            val session = SessionEntity(
                title = title,
                timestamp = System.currentTimeMillis(),
                durationMs = _uiState.value.totalDurationMs,
                rootKey = _uiState.value.selectedKey
            )
            val timelineEntities = _uiState.value.detectedChords.map { chord ->
                ChordTimelineEntity(
                    sessionId = 0L,
                    timestampMs = chord.timestampMs,
                    chordSymbol = chord.chordSymbol,
                    degree = MusicTheoryUtils.convertToRomanNumeral(chord.chordSymbol, _uiState.value.selectedKey),
                    confidence = chord.confidence
                )
            }
            repository.saveSessionWithChords(session, timelineEntities)
        }
    }

    /**
     * Delete a saved session from the local Room database interface.
     */
    fun deleteSavedSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    /**
     * Load chord sheet timeline from database into the active progress layout.
     */
    fun loadSavedSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                // Collect the chords once and update the state
                repository.getChordsForSession(sessionId).collect { dbChords ->
                    if (dbChords.isNotEmpty()) {
                        val activeChords = dbChords.map { dbChord ->
                            DetectedChord(
                                id = dbChord.id,
                                stemId = "stem_harmonic",
                                timestampMs = dbChord.timestampMs,
                                chordSymbol = dbChord.chordSymbol,
                                confidence = dbChord.confidence
                            )
                        }
                        _uiState.update { currentState ->
                            currentState.copy(
                                selectedKey = session.rootKey,
                                totalDurationMs = session.durationMs,
                                detectedChords = activeChords,
                                activeChordIndex = 0
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Set the selected visual workstation layout mode and persist it.
     */
    fun setVisualLayoutMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setVisualLayoutMode(mode)
        }
    }

    /**
     * Set selected tuning configuration preference and persist it.
     */
    fun setTuningChoice(tuning: String) {
        viewModelScope.launch {
            preferencesManager.setTuningChoice(tuning)
        }
    }

    /**
     * Triggers tracking for an export operation, showing live status and simulated progress increments.
     */
    fun startExportTracking(type: String, initialMessage: String, durationMs: Long = 2500L) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    exportType = type,
                    exportProgress = 0.0f,
                    exportStatusMessage = initialMessage,
                    showExportIndicator = true
                )
            }
            
            val steps = 10
            val delayPerStep = durationMs / steps
            for (step in 1..steps) {
                delay(delayPerStep)
                _uiState.update {
                    if (it.exportType == type && it.showExportIndicator) {
                        it.copy(
                            exportProgress = step / steps.toFloat(),
                            exportStatusMessage = "$initialMessage (${step * 10}%)"
                        )
                    } else {
                        it
                    }
                }
            }
        }
    }

    /**
     * Completes an export operation tracking loop, presenting final output/error messages.
     */
    fun completeExportTracking(type: String, finalResult: String, success: Boolean = true, autoDismissDelayMs: Long = 4000L) {
        viewModelScope.launch {
            _uiState.update {
                if (it.exportType == type && it.showExportIndicator) {
                    it.copy(
                        exportProgress = 1.0f,
                        exportStatusMessage = finalResult
                    )
                } else {
                    it
                }
            }
            delay(autoDismissDelayMs)
            _uiState.update {
                if (it.exportType == type && it.showExportIndicator) {
                    it.copy(showExportIndicator = false)
                } else {
                    it
                }
            }
        }
    }

    /**
     * Instantly dismisses active export state overlay.
     */
    fun dismissExportIndicator() {
        _uiState.update { it.copy(showExportIndicator = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimelineSync()
        stopMicrophone()
        exoPlayer?.release()
        exoPlayer = null
    }
}
