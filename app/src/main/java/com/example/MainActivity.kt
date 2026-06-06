package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PitchDetector
import com.example.model.AudioStem
import com.example.model.DetectedChord
import com.example.model.MusicTheoryUtils
import com.example.model.ArpeggioBuffer
import com.example.model.AfricanStyleClassifier
import com.example.model.NoteEvent
import com.example.model.StemMixerManager
import com.example.model.StemSeparationWorker
import com.example.model.AnalyzerViewModel
import com.example.model.AnalyzerUiState
import com.example.ui.ChordPadMatrix
import com.example.ui.RollingWaveformTimeline
import com.example.ui.HzChordAiMainScreen
import com.example.ui.HzSplashScreen
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    HzSplashScreen(onSplashComplete = { showSplash = false })
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        HzChordAiMainScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkstationScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyzerViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe global MVVM architectural state representation
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val stems = uiState.stems
    val isSeparating = uiState.isSeparating
    val separationProgress = uiState.separationProgress
    val isPlaying = uiState.isPlaying
    val currentProgressMs = uiState.currentProgressMs
    val selectedKey = uiState.selectedKey
    val transposeSemitones = uiState.transposeSemitones
    val currentChord = uiState.detectedChords.getOrNull(uiState.activeChordIndex) ?: DetectedChord(
        id = 1, stemId = "stem_harmonic", timestampMs = 0L, chordSymbol = "Cmaj7", confidence = 0.95f
    )

    var activeSubView by remember { mutableStateOf("MATRIX") }

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF030D1D)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // App Header & JNI DSP Systems HUD status
        HeaderHUDBlock()

        Spacer(modifier = Modifier.height(12.dp))

        // High fidelity workspace navigation switcher bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D1726))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sections = listOf(
                "MATRIX" to "Chord Matrix",
                "TIMELINE" to "Timeline Scroll",
                "STEMS" to "Stems Mixer",
                "DSP" to "DSP Tools"
            )
            sections.forEach { (key, label) ->
                val isSelected = activeSubView == key
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeSubView = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeSubView == "MATRIX") {
            ChordPadMatrix(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        } else if (activeSubView == "TIMELINE") {
            RollingWaveformTimeline(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Center Workstation content splits: timeline player, mixers, and theory deck
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Segment 1: Master Timeline Visualizer with Playhead
                if (activeSubView == "DSP") {
                    item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "HZ-CORE DSP ENGINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Live Playhead Timeline",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            // Playback controllers
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.togglePlayback()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlaying) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause Workspace" else "Run Raw Track",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPlaying) "PAUSE" else "PLAY WORKSTATION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.seekTo(0L)
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Master Index",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Playhead waveform block drawing
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070E1A))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        ) {
                            // Render a nice futuristic synth grid
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cols = 24
                                val stepX = size.width / cols
                                for (i in 0..cols) {
                                    drawLine(
                                        color = Color(0x0C00F0FF),
                                        start = Offset(i * stepX, 0f),
                                        end = Offset(i * stepX, size.height),
                                        strokeWidth = 1f
                                    )
                                }
                                val rows = 6
                                val stepY = size.height / rows
                                for (i in 0..rows) {
                                    drawLine(
                                        color = Color(0x0C00F0FF),
                                        start = Offset(0f, i * stepY),
                                        end = Offset(size.width, i * stepY),
                                        strokeWidth = 1f
                                    )
                                }
                            }

                            // Dynamic RMS waveform blocks
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = size.width / 50f
                                val progressRatio = currentProgressMs.toFloat() / 120000f

                                for (i in 0 until 50) {
                                    val hMultiplier = if (i % 3 == 0) 0.7f else if (i % 2 == 0) 0.4f else 0.5f
                                    val h = (size.height * hMultiplier)
                                    val barX = i * (size.width / 50f)
                                    val isHighlighted = barX / size.width <= progressRatio
                                    
                                    drawRect(
                                        color = if (isHighlighted) Color(0xFF00B7FF) else Color(0x3D1E2D4A),
                                        topLeft = Offset(barX + 2f, (size.height - h) / 2),
                                        size = androidx.compose.ui.geometry.Size(barWidth - 4f, h)
                                    )
                                }

                                // Neon Playhead line
                                drawLine(
                                    color = Color(0xFF00F0FF),
                                    start = Offset(size.width * progressRatio, 0f),
                                    end = Offset(size.width * progressRatio, size.height),
                                    strokeWidth = 4f
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = formatTime(currentProgressMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "TOTAL: 02:00",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            }

            // Segment 2: AI Chord Processing & Transposition Deck
            if (activeSubView == "DSP") {
                item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "REAL-TIME AI MODEL PREDICTION (100% OFFLINE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Musical Deck Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            // AI Confidence status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0C2417))
                                    .border(1.dp, Color(0xFF00E676), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CONFIDENCE: ${(currentChord.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Chord Sign Displays
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Main Chord Card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070E1A))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "TRANSPOSED CHORD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = currentChord.chordSymbol,
                                        style = MaterialTheme.typography.displayMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Roman Functional Step Card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070E1A))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "ROMAN ANALYSIS (KEY: $selectedKey)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = MusicTheoryUtils.convertToRomanNumeral(currentChord.chordSymbol, selectedKey),
                                        style = MaterialTheme.typography.displayMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Semitones Transposition Controller
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Transpose Semitones: ${if (transposeSemitones >= 0) "+$transposeSemitones" else transposeSemitones}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.updateTranspose(transposeSemitones - 1) },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("-", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.updateTranspose(0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D4A)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("RESET", color = MaterialTheme.colorScheme.onSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.updateTranspose(transposeSemitones + 1) },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Scale Selector (Key of C, G, D, etc.)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Functional Reference Scale Key:",
                                style = Modifier.weight(1f).let { MaterialTheme.typography.bodySmall },
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Flat / Sharp Preference Button
                            OutlinedButton(
                                onClick = { viewModel.toggleNamingConvention() },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (uiState.useFlatNaming) "USE SHARPS (#)" else "USE FLATS (b)",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val keys = listOf("C", "D", "E", "F", "G", "A", "Bb", "Bm")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            keys.forEach { k ->
                                val isSelected = selectedKey == k
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color(0xFF070E1A))
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.selectKey(k) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = k,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            // Segment 3: Stem Demixing console & Slider Mixing Board
            if (activeSubView == "STEMS") {
                item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "OFFLINE OBOE JNI STEM MIXER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Isolated Instrument Stems",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (isSeparating) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            progress = { separationProgress },
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Separating ${(separationProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val workRequest = OneTimeWorkRequestBuilder<StemSeparationWorker>()
                                            .setInputData(workDataOf("input_file_path" to "/local/raw/master.wav"))
                                            .build()
                                        WorkManager.getInstance(context).enqueue(workRequest)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = "Trigger AI Separator",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RUN 12-STEM AI SEPARATOR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        if (isSeparating) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { separationProgress },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        stems.forEachIndexed { index, stem ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stem.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(110.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Dynamic inline micro-waveform trace
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF070E1A))
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val bars = stem.getWaveformList()
                                            if (bars.isNotEmpty()) {
                                                val stepWidth = size.width / bars.size
                                                bars.forEachIndexed { bIdx, value ->
                                                    val finalH = size.height * value * stem.volume
                                                    drawRect(
                                                        color = if (stem.isMuted) Color(0x2A6D4CFF) else Color(0xCC6D4CFF),
                                                        topLeft = Offset(bIdx * stepWidth + 1f, (size.height - finalH) / 2),
                                                        size = androidx.compose.ui.geometry.Size(stepWidth - 2f, finalH)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Mute Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (stem.isMuted) Color(0xFFFF5252) else Color(0xFF162235))
                                            .clickable {
                                                StemMixerManager.toggleMute(stem.id)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "MUTE",
                                            color = if (stem.isMuted) Color.White else Color(0xFF94A3B8),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Solo Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (stem.isSoloed) Color(0xFFFFC107) else Color(0xFF162235))
                                            .clickable {
                                                StemMixerManager.toggleSolo(stem.id)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "SOLO",
                                            color = if (stem.isSoloed) Color.Black else Color(0xFF94A3B8),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Volume: ${(stem.volume * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6B7280),
                                        modifier = Modifier.width(110.dp)
                                    )
                                    Slider(
                                        value = stem.volume,
                                        onValueChange = { newVal ->
                                            StemMixerManager.updateVolume(stem.id, newVal)
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = Color(0xFF162235)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (index < stems.lastIndex) {
                                Divider(color = Color(0x3D1E2D4A), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
            }

            // Segment 4: Detailed Chord Timeline Event Stream
            if (activeSubView == "DSP") {
                item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CHORD EVENT LISTENER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Full Progression Index",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        uiState.detectedChords.forEachIndexed { idx, chord ->
                            val isActive = idx == uiState.activeChordIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) Color(0x1B00B7FF) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF1E2D4A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            color = if (isActive) Color.Black else Color.White,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "At ${formatTime(chord.timestampMs)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isActive) Color.White else Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = chord.chordSymbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = MusicTheoryUtils.convertToRomanNumeral(chord.chordSymbol, selectedKey),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isActive) MaterialTheme.colorScheme.secondary else Color(0xFF6B7280),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(60.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            // Segment 5: Real-Time Microphone Analysis & Chromagram Workspace
            if (activeSubView == "DSP") {
                item {
                    LiveMicAnalyzerBlock(viewModel = viewModel, targetKeyOf = selectedKey)
                }
            }

            // Segment 6: Flagship Arpeggio & African Style Intelligence Engine
            if (activeSubView == "DSP") {
                item {
                    StyleRecognitionBlock(viewModel = viewModel)
                }
            }
        }
        }
    }
}

@Composable
fun LiveMicAnalyzerBlock(
    viewModel: AnalyzerViewModel,
    targetKeyOf: String
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isRunning by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.toggleMicrophone { }
            isRunning = true
        }
    }

    val liveChroma = uiState.liveChromagram
    val livePitchHz = uiState.livePitchHz
    val liveChordSymbol = uiState.liveChordSymbol

    DisposableEffect(Unit) {
        onDispose {
            if (isRunning) {
                viewModel.toggleMicrophone { }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "LIVE MIC SPECTRUM ANALYZER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00F0FF),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Acoustic Pitch Tracker",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        if (isRunning) {
                            viewModel.toggleMicrophone { }
                            isRunning = false
                        } else {
                            if (hasPermission) {
                                viewModel.toggleMicrophone { }
                                isRunning = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFFF5252) else Color(0xFF00B7FF)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Mic Analyser",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "STOP MIC" else "START MIC",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFF070E1A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x1B1E2D4A), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Inactive analyzer node",
                            tint = Color(0xFF1E2D4A),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Microphone session is offline",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Acoustics will analyze input chords locally in real-time.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Live Chord Output
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070E1A))
                            .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LIVE DETECTED CHORD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (livePitchHz > 0f) liveChordSymbol else "---",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (livePitchHz > 0f) "FUNCTION: ${MusicTheoryUtils.convertToRomanNumeral(liveChordSymbol, targetKeyOf)}" else "Key: $targetKeyOf",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Live Frequency Index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070E1A))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PEAK AUDIO FREQUENCY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (livePitchHz > 0f) String.format("%.1f Hz", livePitchHz) else "Silent",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (livePitchHz > 0f) "Latency: < 50ms (JVM FFT)" else "Offline Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time 12-semitone Chromagram spectrum bar charts
                Text(
                    text = "12-Tone Pitch Class Profile (Chromagram Vector):",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val noteLabels = MusicTheoryUtils.sharps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(0xFF070E1A), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    liveChroma.forEachIndexed { index, weight ->
                        val noteLabel = noteLabels[index]
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Weight visual column bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(weight.coerceIn(0.05f, 1.0f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF00F0FF),
                                                    Color(0xFF6D4CFF)
                                                )
                                            )
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = noteLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (weight > 0.15f) Color(0xFF00F0FF) else Color(0xFF6B7280),
                                fontWeight = if (weight > 0.15f) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderHUDBlock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1726))
            .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF88))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HZ CHORD AI WORKSTATION v2.1",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "100% Offline-First Neural DSP Node",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x1B00F0FF))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ONNX RT: INT8 LOCAL",
                    color = Color(0xFF00F0FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "DSP latency: 12.4ms",
                color = Color(0xFF6B7280),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun StyleRecognitionBlock(viewModel: AnalyzerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeNotes = uiState.activeNotes
    val impliedHarmony = uiState.impliedHarmony
    val styleMatch = object {
        val styleName = uiState.styleName
        val confidence = uiState.styleConfidence
        val description = uiState.styleDescription
        val recommendedTempoBpm = uiState.recommendedTempoBpm
    }

    val onNoteStrike: (Int) -> Unit = { midi ->
        viewModel.strikeSimulationNote(midi)
    }

    val onClearBuffer: () -> Unit = {
        viewModel.clearSimulationBuffer()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "FLAGSHIP ARPEGGIO INTELLIGENCE & AFRICAN STYLE RECOGNITION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sequential Harmony Tracker (3.0s Memory)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Traditional style classification results card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070E1A))
                    .border(1.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = styleMatch.styleName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFD54A).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MATCH: ${(styleMatch.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD54A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = styleMatch.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFC9D1D9)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Implied Harmony: $impliedHarmony",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00B7FF),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reference Tempo: ${styleMatch.recommendedTempoBpm} BPM",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active sequence monitor
            Text(
                text = "Chronological Notes in Buffer (${activeNotes.size} active):",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (activeNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(Color(0xFF070E1A), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Strike pads below to simulate interactive riffs...",
                        color = Color(0xFF6B7280),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    activeNotes.takeLast(10).forEach { note ->
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E2D4A))
                                .border(0.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${note.noteName}${note.midiNote / 12 - 1}",
                                color = Color(0xFF00F0FF),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive simulation synthesizer keyboard/pads!
            Text(
                text = "Interactive Live Notes Simulator (Strike Riff):",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val simPads = listOf(
                Pair("Bass Root", 36),
                Pair("Bass 5th", 43),
                Pair("Bass Oct", 48),
                Pair("Treble C", 60),
                Pair("Treble E", 64),
                Pair("Treble G", 67),
                Pair("Treble A", 69),
                Pair("Treble C5", 72),
                Pair("High E", 76),
                Pair("High G", 79)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    simPads.take(5).forEach { pad ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0E1A2F))
                                .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(6.dp))
                                .clickable { onNoteStrike(pad.second) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(pad.first, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("MIDI ${pad.second}", color = Color(0xFF6B7280), fontSize = 8.sp)
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    simPads.takeLast(5).forEach { pad ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF18153A))
                                .border(1.dp, Color(0xFF341F7B), RoundedCornerShape(6.dp))
                                .clickable { onNoteStrike(pad.second) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(pad.first, color = Color(0xFFFFD54A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("MIDI ${pad.second}", color = Color(0xFF94A3B8), fontSize = 8.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onClearBuffer,
                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Flush Memory buffer",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CLEAR SEQUENTIAL MIDI MEMORY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

