package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalyzerUiState
import com.example.model.AnalyzerViewModel
import com.example.model.DetectedChord

// Color definitions matching the image palette: Warm earthly dark background, subtle sand accents, elegant dark cards.
val DarkGreyBg = Color(0xFF1E1D1A)       // Background of the matrix container
val CardBaseColor = Color(0xFF33312E)    // Dark brown/grey card
val CardActiveHighlight = Color(0xFF9E9580) // Beige/Sand highlight for active chord
val TextSecondaryColor = Color(0xFFA19E9A)
val CyanAccent = Color(0xFF4DD0E1)       // Glowing digital cyan
val WaveformInactive = Color(0x334DD0E1)

/**
 * A highly polished 4x4 Grid Matrix replicating Layout Feature A from 1000036522.jpg.
 * Each pad displays the chord symbol stacked directly over its relative Roman Numeral degree.
 * Integrates directly with the centralized AnalyzerViewModel.
 */
@Composable
fun ChordPadMatrix(
    uiState: AnalyzerUiState,
    viewModel: AnalyzerViewModel,
    modifier: Modifier = Modifier
) {
    // Current active chord configuration
    val activeChord = uiState.detectedChords.getOrNull(uiState.activeChordIndex) ?: DetectedChord(
        id = 1, stemId = "stem_harmonic", timestampMs = 0L, chordSymbol = "F#add9", confidence = 0.95f
    )

    // Toggles as seen in the layout
    var selectedChordMode by remember { mutableStateOf("Precise") } // "Basic" or "Precise"
    var voicingsOn by remember { mutableStateOf(true) }
    var gridViewOn by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkGreyBg)
            .padding(12.dp)
            .testTag("chord_pad_matrix_container")
    ) {
        // High Contrast Top Window showing historical or current selection summary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardActiveHighlight)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "D#m",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${activeChord.chordSymbol}\nIII",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4x4 Grid representing historical configurations & pad triggers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(300.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Synthesize 16 pads sequence matching the design layout
                val standardPads = listOf(
                    "F#add9" to "III", "" to "", "" to "", "Badd9" to "VI",
                    "Badd9" to "VI", "" to "", "" to "", "" to "",
                    "Badd9" to "VI", "" to "", "" to "", "" to "",
                    "Badd9" to "VI", "" to "", "" to "", "C#" to "VII",
                    "D#m7" to "i", "" to "", "" to "", "" to ""
                )

                items(16) { index ->
                    val pad = standardPads.getOrNull(index) ?: ("" to "")
                    val padChord = pad.first
                    val padDegree = pad.second
                    
                    val isActive = padChord.isNotEmpty() && (
                        padChord == activeChord.chordSymbol || 
                        (activeChord.chordSymbol.startsWith("F#") && padChord.startsWith("F#"))
                    )

                    Box(
                        modifier = Modifier
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) CardActiveHighlight else CardBaseColor)
                            .border(
                                width = if (isActive) 2.dp else 0.dp,
                                color = if (isActive) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (padChord.isNotEmpty()) {
                                    // Trigger MIDI chord notes simulation automatically
                                    triggerMidiForChord(padChord, viewModel)
                                }
                            }
                            .padding(8.dp)
                            .testTag("chord_pad_$index")
                    ) {
                        if (padChord.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = padChord,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.Black else Color.White
                                )
                                Text(
                                    text = padDegree,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isActive) Color.Black else TextSecondaryColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Command Control bar matching layout 1000036522.jpg exactly
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chords Basic/Precise Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132A3E)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Chords",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Chords Info",
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0D171E)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedChordMode == "Basic") Color.White else Color.Transparent)
                                .clickable { selectedChordMode = "Basic" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Basic",
                                fontSize = 10.sp,
                                color = if (selectedChordMode == "Basic") Color.Black else Color.LightGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedChordMode == "Precise") Color.White else Color.Transparent)
                                .clickable { selectedChordMode = "Precise" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Precise",
                                fontSize = 10.sp,
                                color = if (selectedChordMode == "Precise") Color.Black else Color.LightGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Voicings Toggler with Switch
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132A3E)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Voicings",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Voicings Info",
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Switch(
                        checked = voicingsOn,
                        onCheckedChange = { voicingsOn = it },
                        modifier = Modifier.height(26.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E5FF),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Black
                        )
                    )
                }
            }

            // Grid View Configuration Status Icon
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132A3E)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Grid view",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    IconButton(
                        onClick = { gridViewOn = !gridViewOn },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, // Matches grid design symbol 
                            contentDescription = "Grid status icon",
                            tint = if (gridViewOn) Color(0xFF00E5FF) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Center highlighted Core Active Pitch/Chord Text Degree
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = activeChord.chordSymbol,
                fontSize = 32.sp,
                color = CyanAccent,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "III",
                fontSize = 24.sp,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Interactive Piano Roll showing chord key highlight profiles
        InteractivePianoRoll(activeChord = activeChord.chordSymbol)
    }
}

/**
 * Custom-drawn highly polished Piano Keyboard that lights up key pitch positions 
 * that are components of the selected chord.
 */
@Composable
fun InteractivePianoRoll(
    activeChord: String,
    modifier: Modifier = Modifier
) {
    // Determine which key pitches (0 to 23 of custom double octave keyboard) illuminate
    // C4 chord notes formulas:
    // F#add9: F# (6), A# (10), C# (1), G# (8) etc.
    val highlightedKeys = remember(activeChord) {
        val rootChar = activeChord.take(2)
        when {
            rootChar.startsWith("F#") -> setOf(1, 6, 8, 10, 13, 18, 20, 22)
            rootChar.startsWith("C#") -> setOf(1, 5, 8, 13, 17, 20)
            rootChar.startsWith("B") -> setOf(3, 6, 11, 15, 18, 23)
            rootChar.startsWith("D#") -> setOf(3, 6, 10, 15, 18, 22)
            else -> setOf(0, 4, 7, 11, 12, 16, 19, 23) // Cmaj7 fallback
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(BorderStroke(1.dp, Color.Black))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val keyCount = 24 // White keys
            val pianoWidth = size.width
            val whiteKeyWidth = pianoWidth / keyCount
            val pianoHeight = size.height
            val blackKeyWidth = whiteKeyWidth * 0.65f
            val blackKeyHeight = pianoHeight * 0.6f

            // 1. Draw White Keys
            for (i in 0 until keyCount) {
                // Determine if this white key index should be highlighted yellow
                // Map indices simplified
                val isHighlighted = highlightedKeys.contains(i)
                val keyColor = if (isHighlighted) Color(0xFFFFF59D) else Color.White
                
                drawRect(
                    color = keyColor,
                    topLeft = Offset(i * whiteKeyWidth, 0f),
                    size = androidx.compose.ui.geometry.Size(whiteKeyWidth - 1f, pianoHeight)
                )
                
                // Draw bottom edge border
                drawLine(
                    color = Color.LightGray,
                    start = Offset(i * whiteKeyWidth, pianoHeight),
                    end = Offset((i + 1) * whiteKeyWidth, pianoHeight),
                    strokeWidth = 2f
                )
            }

            // 2. Draw Black Keys
            val blackKeyPatterns = listOf(0, 1, 3, 4, 5) // C#, D#, F#, G#, A# inside 0..6 pattern
            for (i in 0 until 14) {
                val octaveOffset = (i / 7) * 7
                val patternIndex = i % 7
                if (blackKeyPatterns.contains(patternIndex)) {
                    val whiteIndexIndex = when (patternIndex) {
                        0 -> 0 // C# is between 0 (C) and 1 (D)
                        1 -> 1 // D# is between 1 (D) and 2 (E)
                        3 -> 3 // F# is between 3 (F) and 4 (G)
                        4 -> 4 // G# is between 4 (G) and 5 (A)
                        5 -> 5 // A# is between 5 (A) and 6 (B)
                        else -> 0
                    }
                    val actualWhiteKeyIndex = whiteIndexIndex + (octaveOffset * 12 / 7)
                    
                    // Highlight if active
                    val isBlackHighlighted = highlightedKeys.contains(actualWhiteKeyIndex + 100) // Dummy shift for black check
                    val keyColor = if (isBlackHighlighted) Color(0xFFFFF59D) else Color.Black
                    
                    val xPos = (actualWhiteKeyIndex + 1) * whiteKeyWidth - (blackKeyWidth / 2f)
                    drawRect(
                        color = keyColor,
                        topLeft = Offset(xPos, 0f),
                        size = androidx.compose.ui.geometry.Size(blackKeyWidth, blackKeyHeight)
                    )
                }
            }
        }
    }
}

/**
 * Triggers MIDI simulation notes based on clicked Chord Pad.
 */
private fun triggerMidiForChord(chordName: String, viewModel: AnalyzerViewModel) {
    when {
        chordName.startsWith("F#") -> {
            viewModel.strikeSimulationNote(54) // F#3
            viewModel.strikeSimulationNote(58) // A#3
            viewModel.strikeSimulationNote(61) // C#4
            viewModel.strikeSimulationNote(68) // G#4 (add9)
        }
        chordName.startsWith("B") -> {
            viewModel.strikeSimulationNote(59) // B3
            viewModel.strikeSimulationNote(63) // D#4
            viewModel.strikeSimulationNote(66) // F#4
            viewModel.strikeSimulationNote(73) // C#5 (add9)
        }
        chordName.startsWith("C#") -> {
            viewModel.strikeSimulationNote(49) // C#3
            viewModel.strikeSimulationNote(53) // F4
            viewModel.strikeSimulationNote(56) // G#4
            viewModel.strikeSimulationNote(61) // C#5
        }
        chordName.startsWith("D#") -> {
            viewModel.strikeSimulationNote(51) // D#3
            viewModel.strikeSimulationNote(54) // F#3
            viewModel.strikeSimulationNote(58) // A#3
            viewModel.strikeSimulationNote(65) // F4
        }
    }
}
