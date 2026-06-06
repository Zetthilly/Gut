package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.example.model.NoteEvent
import com.example.model.AnalyzerUiState
import com.example.model.AnalyzerViewModel
import com.example.model.DetectedChord
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAnalyzerScreen(
    uiState: AnalyzerUiState,
    viewModel: AnalyzerViewModel,
    modifier: Modifier = Modifier
) {
    var isGridViewEnabled by remember { mutableStateOf(true) }
    var selectedComplexity by remember { mutableStateOf("Precise") } // "Basic" or "Precise"
    var isVoicingsEnabled by remember { mutableStateOf(true) }
    val activeChordIndex = uiState.activeChordIndex
    val activeChord = uiState.detectedChords.getOrNull(activeChordIndex) ?: DetectedChord(
        id = 1, stemId = "stem_harmonic", timestampMs = 0L, chordSymbol = "F#add9", confidence = 0.95f
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030A16)) // Cosmic Deep Navy
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. VIEW CONTROLLER TOOLBAR (DYNAMIC SWITCHER) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D1726)) // Cosmic Midnight Slate
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chords Complexity Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "CHORDS:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC9D1D9)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF030A16))
                        .padding(2.dp)
                ) {
                    listOf("Basic", "Precise").forEach { complexity ->
                        val isSelected = selectedComplexity == complexity
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSelected) Color(0xFF00B7FF) else Color.Transparent)
                                .clickable { selectedComplexity = complexity }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = complexity,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF030A16) else Color(0xFFC9D1D9)
                            )
                        }
                    }
                }
            }

            // Voicings Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "VOICINGS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC9D1D9)
                )
                Switch(
                    checked = isVoicingsEnabled,
                    onCheckedChange = { isVoicingsEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF030A16),
                        checkedTrackColor = Color(0xFF00F0FF),
                        uncheckedThumbColor = Color(0xFFC9D1D9),
                        uncheckedTrackColor = Color(0xFF0D1726)
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            // Grid View Toggle (Swapping between Matrix and Waveform Mode)
            Button(
                onClick = { isGridViewEnabled = !isGridViewEnabled },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGridViewEnabled) Color(0xFF6D4CFF) else Color(0xFF00F0FF)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (isGridViewEnabled) "WAVEFORM VIEW" else "MATRIX VIEW",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF030A16)
                )
            }
        }

        // --- 2. ACTIVE VIEWPORT (MATRIX PAD GRID vs ROLLING WAVEFORM TIMELINE) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isGridViewEnabled) {
                // VISUAL LAYOUT A: CHORD PAD MATRIX GRID (Referencing Image 1)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CHORD PAD MATRIX WORKspace",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC9D1D9),
                        letterSpacing = 1.sp
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // 4x4 Grid of Chord Pads
                        val pads = listOf(
                            "F#add9" to "III", "" to "", "" to "", "Badd9" to "VI",
                            "Badd9" to "VI", "" to "", "" to "", "" to "",
                            "Badd9" to "VI", "" to "", "" to "", "" to "",
                            "Badd9" to "VI", "" to "", "" to "", "C#" to "VII",
                            "D#m7" to "i", "" to "", "" to "", "" to ""
                        )

                        items(16) { index ->
                            val pad = pads.getOrNull(index) ?: ("" to "")
                            val padSymbol = pad.first
                            val padDegree = pad.second
                            val isEmpty = padSymbol.isEmpty()

                            val isActive = !isEmpty && (
                                padSymbol == activeChord.chordSymbol ||
                                (activeChord.chordSymbol.startsWith("F#") && padSymbol.startsWith("F#"))
                            )

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isActive) Color(0xFF00F0FF).copy(alpha = 0.15f)
                                        else if (isEmpty) Color(0xFF0D1726).copy(alpha = 0.4f)
                                        else Color(0xFF0D1726)
                                    )
                                    .border(
                                        width = if (isActive) 1.5.dp else 1.dp,
                                        color = if (isActive) Color(0xFF00F0FF) else Color(0xFF1E2D4A),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable(!isEmpty) {
                                        // Strike MIDI notes dynamically for quick listening simulation
                                        strikeMidiChord(padSymbol, viewModel)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isEmpty) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = padSymbol,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isActive) Color(0xFF00F0FF) else Color(0xFFC9D1D9)
                                        )
                                        Text(
                                            text = padDegree,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) Color(0xFFFFD54A) else Color(0xFFC9D1D9).copy(alpha = 0.5f)
                                        )
                                    }
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Color(0xFF00D68F))
                                        )
                                    }
                                } else {
                                    // Symmetrical structural silent pad
                                    Text(
                                        text = "-",
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E2D4A)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // VISUAL LAYOUT B: ROLLING WAVEFORM TIMELINE (Referencing Image 2)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ROLLING WAVEFORM TIMELINE PREDICTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC9D1D9),
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF030A16))
                            .border(1.dp, Color(0xFF1E2D4A)),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Anchor Panel displaying major key signature
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(80.dp)
                                .background(Color(0xFF0D1726))
                                .border(BorderStroke(1.dp, Color(0xFF1E2D4A)))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "KEY SIG",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54A)
                                )
                                Text(
                                    text = "∮ D#m",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00F0FF)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF00B7FF).copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CAPO 1",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00B7FF)
                                    )
                                }
                            }
                        }

                        // Right Panel displaying scrolling audio waveform towards static vertical "NOW" bar
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            // Custom scrolling waveform drawing
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(scrollState)
                                    .width(800.dp)
                            ) {
                                val midY = size.height / 2f
                                val totalWidth = size.width

                                // Continuous dotted line playhead horizontal guide
                                drawLine(
                                    color = Color(0xFF1E2D4A),
                                    start = Offset(0f, midY),
                                    end = Offset(totalWidth, midY),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                )

                                // Custom-drawn horizontal waveform segments scrolling leftwards
                                var stepX = 20f
                                while (stepX < totalWidth) {
                                    val isPast = stepX < 240f
                                    val barHeight = when {
                                        stepX % 80 == 0f -> 45f
                                        stepX % 40 == 0f -> 25f
                                        else -> 12f
                                    }
                                    val waveColor = if (isPast) Color(0xFF00B7FF).copy(alpha = 0.4f) else Color(0xFF00F0FF)

                                    drawLine(
                                        color = waveColor,
                                        start = Offset(stepX, midY - barHeight),
                                        end = Offset(stepX, midY + barHeight),
                                        strokeWidth = 4f,
                                        cap = StrokeCap.Round
                                    )
                                    stepX += 16f
                                }
                            }

                            // Horizontal scroll of Approaching look-ahead floating cards
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(scrollState)
                                    .width(800.dp)
                            ) {
                                uiState.detectedChords.forEachIndexed { i, chord ->
                                    val xOffset = 30.dp + (i * 85).dp
                                    val isActive = i == activeChordIndex

                                    Card(
                                        modifier = Modifier
                                            .offset(x = xOffset, y = 14.dp)
                                            .width(70.dp)
                                            .border(
                                                width = if (isActive) 1.5.dp else 1.dp,
                                                color = if (isActive) Color(0xFF00F0FF) else Color(0xFF1E2D4A),
                                                shape = RoundedCornerShape(4.dp)
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isActive) Color(0xFFFFD54A) else Color(0xFF0D1726)
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
                                                fontWeight = FontWeight.Black,
                                                color = if (isActive) Color(0xFF030A16) else Color(0xFFC9D1D9)
                                            )
                                            Text(
                                                text = when (i % 4) {
                                                    0 -> "III"
                                                    1 -> "VI"
                                                    2 -> "VII"
                                                    else -> "i"
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) Color(0xFF030A16).copy(alpha = 0.7f) else Color(0xFF00B7FF)
                                            )
                                        }
                                    }
                                }
                            }

                            // Static vertical "NOW" Playhead bar overlay on top right screen
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(3.dp)
                                    .offset(x = 180.dp)
                                    .background(Color(0xFFFF5A5A))
                            )

                            // Digital BPM Display badge at lower-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0D1726))
                                    .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "BPM ${uiState.recommendedTempoBpm}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD54A),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. HIGH PRECISION NEEDLE DIAL TUNER HUD (±1 Cent Calibration) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1726)),
            border = BorderStroke(1.dp, Color(0xFF1E2D4A))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HIGH-PRECISION ACCURACY DIAL TUNER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFC9D1D9).copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "REF A=440Hz",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00F0FF)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Needle dial visual canvas
                    val centOffset = if (uiState.livePitchHz > 0f) {
                        // Calculate cent offset from nearest semitone
                        val cents = ((12 * kotlin.math.log2(uiState.livePitchHz / 440.0)) * 100) % 100
                        cents.toFloat()
                    } else {
                        4f
                    }

                    Box(
                        modifier = Modifier
                            .size(100.dp, 50.dp)
                            .background(Color(0xFF030A16), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw semi-circular dial arc
                            drawArc(
                                color = Color(0xFF1E2D4A),
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                style = Stroke(width = 4f, cap = StrokeCap.Round),
                                topLeft = Offset(w * 0.15f, h * 0.3f),
                                size = Size(w * 0.7f, h * 1.2f)
                            )

                            // Center in-tune green bracket highlight
                            drawArc(
                                color = Color(0xFF00D68F),
                                startAngle = 260f,
                                sweepAngle = 20f,
                                useCenter = false,
                                style = Stroke(width = 4f, cap = StrokeCap.Round),
                                topLeft = Offset(w * 0.15f, h * 0.3f),
                                size = Size(w * 0.7f, h * 1.2f)
                            )

                            // Draw tick lines
                            for (tick in -50..50 step 25) {
                                val angleDeg = 270f + (tick * 1.8f) // scale from -50..50 to -90..90
                                val angleRad = angleDeg * PI / 180f
                                val startFactor = 0.75f
                                val endFactor = 0.9f
                                drawLine(
                                    color = if (tick == 0) Color(0xFF00D68F) else Color(0xFFC9D1D9).copy(alpha = 0.5f),
                                    start = Offset(w / 2f + (w * 0.35f * startFactor * cos(angleRad)).toFloat(), h * 0.9f + (h * 0.9f * startFactor * sin(angleRad)).toFloat()),
                                    end = Offset(w / 2f + (w * 0.35f * endFactor * cos(angleRad)).toFloat(), h * 0.9f + (h * 0.9f * endFactor * sin(angleRad)).toFloat()),
                                    strokeWidth = 2f
                                )
                            }

                            // Draw needle rotating based on pitch centOffset
                            val needleAngleDeg = 270f + (centOffset.coerceIn(-50f, 50f) * 1.8f)
                            val needleAngleRad = needleAngleDeg * PI / 180f
                            val needleLen = h * 0.75f

                            drawLine(
                                color = Color(0xFFFF5A5A),
                                start = Offset(w / 2f, h * 0.9f),
                                end = Offset(w / 2f + (needleLen * cos(needleAngleRad)).toFloat(), h * 0.9f + (needleLen * sin(needleAngleRad)).toFloat()),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }

                        Text(
                            text = "${if (centOffset >= 0) "+" else ""}${centOffset.toInt()} Cents",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (kotlin.math.abs(centOffset) <= 4) Color(0xFF00D68F) else Color(0xFFFFD54A),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                        )
                    }

                    // Key details display
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE: ${uiState.liveChordSymbol}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00F0FF)
                            )

                            Text(
                                text = "${String.format("%.1f", uiState.livePitchHz)} Hz",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54A)
                            )
                        }

                        // African style tags / Engine outputs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = Color(0xFF6D4CFF)
                            )
                            Text(
                                text = "STYLE: ${uiState.styleName} (${(uiState.styleConfidence * 100).toInt()}% Conf)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC9D1D9).copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }

                        // FLAGSHIP ARPEGGIO INTELLIGENCE ENGINE TAGS (African Style Recognition Outputs)
                        val arpeggioTag = if (uiState.activeArpeggioTag == "Undecided" || uiState.activeArpeggioTag.isEmpty()) "⚡ Sebene Syncopation" else "⚡ ${uiState.activeArpeggioTag}"
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF6D4CFF).copy(alpha = 0.15f))
                                    .border(0.5.dp, Color(0xFF6D4CFF), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = arpeggioTag,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00F0FF)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF00D68F).copy(alpha = 0.15f))
                                    .border(0.5.dp, Color(0xFF00D68F), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "⚡ Sungura Roll Enabled",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00D68F)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. THE VISUAL VOICING DECK (PIANO ROLL) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1726)),
            border = BorderStroke(1.dp, Color(0xFF1E2D4A))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "VISUAL VOICING DECK (PIANO ROLL)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFC9D1D9).copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )

                // Render virtual interactive piano with color highlighted pitches
                PianoRollComponent(
                    activeChord = activeChord.chordSymbol,
                    activeNotes = uiState.activeNotes
                )

                // Chord interval labels displaying direct educational feedback underneath the keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rootText = extractRoot(activeChord.chordSymbol)
                    listOf(
                        "ROOT: $rootText" to Color(0xFF00F0FF),
                        "3rd: ${getThird(rootText)}" to Color(0xFFFFD54A),
                        "5th: ${getFifth(rootText)}" to Color(0xFF6D4CFF),
                        "EXT: 7th/9th" to Color(0xFF00D68F)
                    ).forEach { (label, color) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(color)
                            )
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC9D1D9)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard chord helpers to parse root and intervals for labeling.
 */
private fun extractRoot(symbol: String): String {
    if (symbol == "None") return "F#"
    val normalized = symbol.replace("maj", "").replace("min", "").replace("m", "")
    if (normalized.length >= 2 && (normalized[1] == '#' || normalized[1] == 'b')) {
        return normalized.take(2)
    }
    return normalized.take(1)
}

private fun getThird(root: String): String {
    return when (root) {
        "F#" -> "A#"
        "C#" -> "E#"
        "B" -> "D#"
        "D#" -> "F#"
        else -> "E"
    }
}

private fun getFifth(root: String): String {
    return when (root) {
        "F#" -> "C#"
        "C#" -> "G#"
        "B" -> "F#"
        "D#" -> "A#"
        else -> "G"
    }
}

@Composable
fun PianoRollComponent(
    activeChord: String,
    activeNotes: List<NoteEvent>,
    modifier: Modifier = Modifier
) {
    val highlightedKeys = remember(activeChord) {
        val root = extractRoot(activeChord)
        when (root) {
            "F#" -> setOf(1, 6, 8, 10, 13, 18, 20, 22)
            "C#" -> setOf(1, 5, 8, 13, 17, 20)
            "B" -> setOf(3, 6, 11, 15, 18, 23)
            "D#" -> setOf(3, 6, 10, 15, 18, 22)
            else -> setOf(0, 4, 7, 11, 12, 16, 19, 23) // C fallback
        }
    }

    val baseNote = 48 // C3
    val whiteSemitoneOffsets = listOf(0, 2, 4, 5, 7, 9, 11)
    val blackPatterns = listOf(0, 1, 3, 4, 5)
    val blackSemitoneOffsets = mapOf(
        0 to 1,  // C -> C#
        1 to 3,  // D -> D#
        3 to 6,  // F -> F#
        4 to 8,  // G -> G#
        5 to 10  // A -> A#
    )

    // Collect spring animated glow intensity for all 21 white keys active status
    val whiteGlows = (0 until 21).map { i ->
        val octave = i / 7
        val step = i % 7
        val midi = baseNote + (octave * 12) + whiteSemitoneOffsets[step]
        val isActive = activeNotes.any { it.midiNote == midi }
        animateFloatAsState(
            targetValue = if (isActive) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "white_glow_$i"
        ).value
    }

    // Collect spring animated glow intensity for all 21 potential black keys spaces (15 actual ones)
    val blackGlows = (0 until 21).map { i ->
        val step = i % 7
        val octave = i / 7
        val blackOffset = blackSemitoneOffsets[step]
        if (blackOffset != null) {
            val midi = baseNote + (octave * 12) + blackOffset
            val isActive = activeNotes.any { it.midiNote == midi }
            animateFloatAsState(
                targetValue = if (isActive) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "black_glow_$i"
            ).value
        } else {
            0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .border(BorderStroke(1.dp, Color(0xFF1E2D4A)), RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val keyCount = 21 // 3 white octaves
            val w = size.width
            val keyW = w / keyCount
            val h = size.height
            val blackKeyW = keyW * 0.62f
            val blackKeyH = h * 0.58f

            // Helper to interpolate colors manual way safely
            fun lerpCol(start: Color, end: Color, fraction: Float): Color {
                val r = start.red + (end.red - start.red) * fraction
                val g = start.green + (end.green - start.green) * fraction
                val b = start.blue + (end.blue - start.blue) * fraction
                val a = start.alpha + (end.alpha - start.alpha) * fraction
                return Color(red = r, green = g, blue = b, alpha = a)
            }

            // 1. Draw White Keys
            for (i in 0 until keyCount) {
                val isActive = highlightedKeys.contains(i % 12)
                var color = if (isActive) Color(0xFF00F0FF).copy(alpha = 0.35f) else Color(0xFF0D1726)
                
                val whiteGlow = whiteGlows[i]
                if (whiteGlow > 0.01f) {
                    color = lerpCol(color, Color(0xFF00F0FF).copy(alpha = 0.5f), whiteGlow)
                }

                drawRect(
                    color = color,
                    topLeft = Offset(i * keyW, 0f),
                    size = Size(keyW - 1f, h)
                )

                // Render dynamic neon gradient glow overlays for struck white keys using Brush
                if (whiteGlow > 0.01f) {
                    val glowBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00F0FF).copy(alpha = 0.1f * whiteGlow),
                            Color(0xFF00F0FF).copy(alpha = 0.8f * whiteGlow)
                        ),
                        startY = 0f,
                        endY = h
                    )
                    drawRect(
                        brush = glowBrush,
                        topLeft = Offset(i * keyW, 0f),
                        size = Size(keyW - 1f, h)
                    )

                    // Draw a glowing bottom-edge neon indicator bar that changes with intensity
                    drawRect(
                        color = Color(0xFF00F0FF).copy(alpha = whiteGlow),
                        topLeft = Offset(i * keyW + 2f, h - (5f * whiteGlow)),
                        size = Size(keyW - 5f, 3f * whiteGlow)
                    )
                }

                // White key separating lines
                drawLine(
                    color = Color(0xFF1E2D4A).copy(alpha = 0.4f),
                    start = Offset(i * keyW, 0f),
                    end = Offset(i * keyW, h),
                    strokeWidth = 1f
                )
            }

            // 2. Draw Black Keys
            for (i in 0 until keyCount) {
                val step = i % 7
                val octaveOffset = (i / 7) * 12
                if (blackPatterns.contains(step)) {
                    val isActiveBlack = highlightedKeys.contains((step + octaveOffset) % 12)
                    var bkColor = if (isActiveBlack) Color(0xFFFFD54A) else Color(0xFF030A16)
                    
                    val blackGlow = blackGlows[i]
                    if (blackGlow > 0.01f) {
                        bkColor = lerpCol(bkColor, Color(0xFFFFD54A).copy(alpha = 0.9f), blackGlow)
                    }

                    drawRect(
                        color = bkColor,
                        topLeft = Offset((i + 1) * keyW - (blackKeyW / 2f), 0f),
                        size = Size(blackKeyW, blackKeyH)
                    )

                    // Draw neon top glow on active black keys using vertical high quality gradient Brush
                    if (blackGlow > 0.01f) {
                        val blackGlowBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFD54A).copy(alpha = 0.9f * blackGlow),
                                Color(0xFFFFD54A).copy(alpha = 0.15f * blackGlow)
                            ),
                            startY = 0f,
                            endY = blackKeyH
                        )
                        drawRect(
                            brush = blackGlowBrush,
                            topLeft = Offset((i + 1) * keyW - (blackKeyW / 2f), 0f),
                            size = Size(blackKeyW, blackKeyH)
                        )

                        // Glowing capped tip for premium depth
                        drawRect(
                            color = Color(0xFFFFD54A).copy(alpha = blackGlow),
                            topLeft = Offset((i + 1) * keyW - (blackKeyW / 2f) + 1f, blackKeyH - 4f),
                            size = Size(blackKeyW - 2f, 2.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun strikeMidiChord(chord: String, viewModel: AnalyzerViewModel) {
    when {
        chord.startsWith("F#") -> {
            viewModel.strikeSimulationNote(54)
            viewModel.strikeSimulationNote(58)
            viewModel.strikeSimulationNote(61)
            viewModel.strikeSimulationNote(68)
        }
        chord.startsWith("B") -> {
            viewModel.strikeSimulationNote(59)
            viewModel.strikeSimulationNote(63)
            viewModel.strikeSimulationNote(66)
            viewModel.strikeSimulationNote(73)
        }
        chord.startsWith("C#") -> {
            viewModel.strikeSimulationNote(49)
            viewModel.strikeSimulationNote(53)
            viewModel.strikeSimulationNote(56)
            viewModel.strikeSimulationNote(61)
        }
        chord.startsWith("D#") -> {
            viewModel.strikeSimulationNote(51)
            viewModel.strikeSimulationNote(54)
            viewModel.strikeSimulationNote(58)
            viewModel.strikeSimulationNote(65)
        }
    }
}
