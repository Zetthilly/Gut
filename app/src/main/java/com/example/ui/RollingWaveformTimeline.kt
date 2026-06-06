package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalyzerUiState
import com.example.model.AnalyzerViewModel
import com.example.model.DetectedChord

/**
 * A highly immersive look-ahead horizontal timeline replicating Layout Feature B from 1000036524.jpg.
 * Renders oncoming chord progression predictions, dotted dividers, a floating "BPM 73" tempo badge,
 * and standard responsive playback interfaces.
 */
@Composable
fun RollingWaveformTimeline(
    uiState: AnalyzerUiState,
    viewModel: AnalyzerViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val activeIdx = uiState.activeChordIndex
    val activeChord = uiState.detectedChords.getOrNull(activeIdx) ?: DetectedChord(
        id = 1, stemId = "stem_harmonic", timestampMs = 0L, chordSymbol = "F#add9", confidence = 0.95f
    )

    // Scroll automatically with playback progress
    LaunchedEffect(uiState.currentProgressMs) {
        val maxScroll = scrollState.maxValue
        if (maxScroll > 0) {
            val progressRatio = uiState.currentProgressMs.toFloat() / uiState.totalDurationMs.toFloat()
            scrollState.scrollTo((maxScroll * progressRatio).toInt())
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkGreyBg)
            .padding(12.dp)
            .testTag("rolling_waveform_timeline_container")
    ) {
        
        // Horizontal look-ahead timeline segment
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color.Black)
                .border(1.dp, CardBaseColor)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Fixed Left anchor showing current Key
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(72.dp)
                        .background(Color(0xFF151412))
                        .border(1.dp, Color.DarkGray)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E3D3A)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = "∮ D#m",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Scrolling Right lane containing upcoming prediction cards and dotted divider line
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    
                    // Background representation of the scrolling timeline (continuous dotted line + vertical measure grid)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(scrollState)
                            .width(600.dp) // Large scrollable span for look-ahead
                    ) {
                        val midY = size.height / 2f
                        
                        // Draw continuous high-fidelity timeline dotted center line
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(0f, midY),
                            end = Offset(size.width, midY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Draw upcoming musical waves and bars
                        for (x in 20..1200 step 40) {
                            val barHeight = when {
                                x % 3 == 0 -> 25f
                                x % 2 == 0 -> 15f
                                else -> 8f
                            }
                            // Highlight waves around player cursor
                            val waveColor = if (x < 300) CyanAccent.copy(alpha = 1.0f) else CyanAccent.copy(alpha = 0.25f)
                            
                            drawLine(
                                color = waveColor,
                                start = Offset(x.toFloat(), midY - barHeight),
                                end = Offset(x.toFloat(), midY + barHeight),
                                strokeWidth = 3f
                            )
                        }
                    }

                    // Floating Cards for upcoming prediction look-ahead chords
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(scrollState)
                            .width(650.dp)
                    ) {
                        // Positioned steps matching predicted chord timestamps
                        uiState.detectedChords.forEachIndexed { idx, chord ->
                            val xOffset = 20.dp + (idx * 75).dp
                            val isActive = idx == activeIdx

                            Card(
                                modifier = Modifier
                                    .offset(x = xOffset, y = 8.dp)
                                    .width(62.dp)
                                    .border(
                                        width = if (isActive) 1.5.dp else 0.dp,
                                        color = if (isActive) CyanAccent else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { viewModel.seekTo(idx * 15000L) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) Color(0xFFFFF9C4) else Color(0xFF1E1C1A)
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = chord.chordSymbol,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isActive) Color.Black else Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = when(idx % 4) {
                                            0 -> "III"
                                            1 -> "VI"
                                            2 -> "VII"
                                            else -> "i"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.DarkGray else Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    // Floating Right-aligned BPM Tempo Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF33312E))
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "BPM ${uiState.recommendedTempoBpm}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Rolling Timeline timestamp marker (e.g. 0:01) at top-right
                    Text(
                        text = "0:${String.format("%02d", uiState.currentProgressMs / 1000)}",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Display in the center
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

        // Matching Keyboard View underneath the look-ahead timeline
        InteractivePianoRoll(activeChord = activeChord.chordSymbol)
    }
}
