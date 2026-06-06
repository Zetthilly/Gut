package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A highly immersive custom-drawn Double-Octave Piano Roll showing precise voicing label mappings
 * (such as marking Root, 3rd, 5th, or 9th chord intervals in yellow) corresponding to the currently active tracking chord.
 */
@Composable
fun ActiveVoicingPianoRoll(
    activeChordSymbol: String,
    modifier: Modifier = Modifier
) {
    // Map of active intervals with their representative octave-spanning keys
    // For simplified high-fidelity depiction:
    // Cmaj7: C(R), E(3rd), G(5th), B(7th) -> indices 0, 4, 7, 11
    // F#add9: F#(R), A#(3rd), C#(5th), G#(9th) -> indices 6, 10, 1, 8
    val voicingDetails = remember(activeChordSymbol) {
        val root = activeChordSymbol.take(2)
        when {
            root.startsWith("F#") -> mapOf(
                6 to "R",
                10 to "3rd",
                1 to "5th",
                8 to "9th",
                18 to "R",
                22 to "3rd",
                13 to "5th",
                20 to "9th"
            )
            root.startsWith("C#") -> mapOf(
                1 to "R",
                5 to "3rd",
                8 to "5th",
                13 to "R",
                17 to "3rd",
                20 to "5th"
            )
            root.startsWith("B") -> mapOf(
                11 to "R",
                3 to "3rd",
                6 to "5th",
                14 to "R",
                15 to "3rd",
                18 to "5th"
            )
            root.startsWith("D#") -> mapOf(
                3 to "R",
                6 to "3rd",
                10 to "5th",
                15 to "R",
                18 to "3rd",
                22 to "5th"
            )
            else -> mapOf(
                0 to "R",
                4 to "3rd",
                7 to "5th",
                11 to "7th",
                12 to "R",
                16 to "3rd",
                19 to "5th",
                23 to "7th"
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF131210))
            .padding(8.dp)
    ) {
        // Top mini legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE VOICING CONFIGURATION",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFF59D)))
                Text("Voicing Interval Highlight", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .border(BorderStroke(1.dp, Color.Black))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val keyCount = 24 // White keys
                val pianoWidth = size.width
                val whiteKeyWidth = pianoWidth / keyCount
                val pianoHeight = size.height
                val blackKeyWidth = whiteKeyWidth * 0.65f
                val blackKeyHeight = pianoHeight * 0.6f

                // 1. Draw White Keys with highlight color
                for (i in 0 until keyCount) {
                    val isInterval = voicingDetails.containsKey(i)
                    val keyColor = if (isInterval) Color(0xFFFFF59D) else Color.White
                    
                    drawRect(
                        color = keyColor,
                        topLeft = Offset(i * whiteKeyWidth, 0f),
                        size = androidx.compose.ui.geometry.Size(whiteKeyWidth - 1f, pianoHeight)
                    )
                    
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(i * whiteKeyWidth, pianoHeight),
                        end = Offset((i + 1) * whiteKeyWidth, pianoHeight),
                        strokeWidth = 2f
                    )
                }

                // 2. Draw Black Keys with highlights
                val blackKeyPatterns = listOf(0, 1, 3, 4, 5) // pattern within octaves
                for (i in 0 until 14) {
                    val octaveOffset = (i / 7) * 7
                    val patternIndex = i % 7
                    if (blackKeyPatterns.contains(patternIndex)) {
                        val whiteIndexIndex = when (patternIndex) {
                            0 -> 0
                            1 -> 1
                            3 -> 3
                            4 -> 4
                            5 -> 5
                            else -> 0
                        }
                        val actualWhiteKeyIndex = whiteIndexIndex + (octaveOffset * 12 / 7)
                        val isIntervalBlack = voicingDetails.containsKey(actualWhiteKeyIndex + 100)
                        val keyColor = if (isIntervalBlack) Color(0xFFFFF59D) else Color.Black
                        
                        val xPos = (actualWhiteKeyIndex + 1) * whiteKeyWidth - (blackKeyWidth / 2f)
                        drawRect(
                            color = keyColor,
                            topLeft = Offset(xPos, 0f),
                            size = androidx.compose.ui.geometry.Size(blackKeyWidth, blackKeyHeight)
                        )
                    }
                }
            }

            // Overlay intervals labels (R, 3rd, 5th, 9th) directly centered on active keys
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                val totalWhiteKeys = 24
                for (i in 0 until totalWhiteKeys) {
                    val label = voicingDetails[i]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (label != null) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
