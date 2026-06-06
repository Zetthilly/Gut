package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalyzerUiState
import com.example.model.AnalyzerViewModel
import com.example.model.AudioStem
import com.example.model.StemMixerManager

@Composable
fun StemMixerScreen(
    uiState: AnalyzerUiState,
    viewModel: AnalyzerViewModel,
    modifier: Modifier = Modifier
) {
    var isNnapiAccelerated by remember { mutableStateOf(true) }
    var panValues by remember { mutableStateOf(mutableMapOf<String, Float>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030A16)) // Cosmic Deep Navy
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. LOCAL NNAPI HARDWARE ACCELERATION HEADER CONSOLE ---
        HardwareAccelerationConsoleCard(
            isSeparating = uiState.isSeparating,
            progress = uiState.separationProgress,
            isNnapiAccelerated = isNnapiAccelerated,
            onToggleNnapi = { isNnapiAccelerated = it },
            onTriggerSeparation = { viewModel.triggerStemSeparation() }
        )

        // --- 2. 12-CHANNEL MIXING CONSOLE TITLE & MULTI-TRACK SELECTION HUD ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "12-CHANNEL STEMS MIXER BOARD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFC9D1D9),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Gain-Staged Isolated Demixing Channels",
                    fontSize = 9.sp,
                    color = Color(0xFFC9D1D9).copy(alpha = 0.5f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0D1726))
                    .border(0.5.dp, Color(0xFF1E2D4A), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "12 STEMS / OFFLINE ONNX",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F0FF)
                )
            }
        }

        // --- 3. 12 INDIVIDUAL CHANNEL STRIPS PANEL ---
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF030812), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(8.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.stems.forEach { stem ->
                    val stemPan = panValues[stem.id] ?: 0.5f
                    
                    ChannelStripItem(
                        stem = stem,
                        pan = stemPan,
                        activeChordSymbol = uiState.liveChordSymbol,
                        onVolumeChange = { vol ->
                            StemMixerManager.updateVolume(stem.id, vol)
                        },
                        onMuteToggle = {
                            StemMixerManager.toggleMute(stem.id)
                        },
                        onSoloToggle = {
                            StemMixerManager.toggleSolo(stem.id)
                        },
                        onPanChange = { newPan ->
                            val updatedMap = panValues.toMutableMap()
                            updatedMap[stem.id] = newPan
                            panValues = updatedMap
                        }
                    )
                }
            }
        }
    }
}

/**
 * Renders a localized master acceleration and offline compiling dashboard.
 */
@Composable
fun HardwareAccelerationConsoleCard(
    isSeparating: Boolean,
    progress: Float,
    isNnapiAccelerated: Boolean,
    onToggleNnapi: (Boolean) -> Unit,
    onTriggerSeparation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1726)),
        border = BorderStroke(1.dp, Color(0xFF1E2D4A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isNnapiAccelerated) Color(0xFF00D68F) else Color(0xFFFFD54A))
                    )
                    Text(
                        text = "HARDWARE ENGINE STATUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Acceleration switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "NNAPI STAGE DETECTOR",
                        fontSize = 8.sp,
                        color = Color(0xFFC9D1D9).copy(alpha = 0.5f)
                    )
                    Switch(
                        checked = isNnapiAccelerated,
                        onCheckedChange = onToggleNnapi,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF030A16),
                            checkedTrackColor = Color(0xFF00D68F),
                            uncheckedThumbColor = Color(0xFFC9D1D9),
                            uncheckedTrackColor = Color(0xFF162235)
                        ),
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }

            if (isSeparating) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "De-constructing WAV waveform layers with NNAPI acceleration...",
                            fontSize = 10.sp,
                            color = Color(0xFF00F0FF)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00F0FF)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00F0FF),
                        trackColor = Color(0xFF030A16)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Stem Separator is currently idle. Ready to decouple backing instruments.",
                        fontSize = 11.sp,
                        color = Color(0xFFC9D1D9).copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onTriggerSeparation,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B7FF)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "EXTRACT 12 STEMS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF030A16)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Highly immersive single-channel strip item mimicking a professional hardware channel strip card.
 */
@Composable
fun ChannelStripItem(
    stem: AudioStem,
    pan: Float,
    activeChordSymbol: String,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit,
    onPanChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .width(86.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1726).copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, Color(0xFF1E2D4A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Channel Name & Chord output display specifically for this track
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stem.name.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                // Track chord detection badge
                val stemChord = if (stem.isMuted) "-" else if (stem.id == "vocals" || stem.id == "electric_guitar" || stem.id == "keyboard") activeChordSymbol else "C"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (stem.isMuted) Color(0xFF162235) else Color(0xFF6D4CFF).copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = stemChord,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (stem.isMuted) Color.Gray else Color(0xFF00F0FF)
                    )
                }
            }

            // Panning custom drawing knob encoder
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "PAN",
                    fontSize = 7.sp,
                    color = Color(0xFFC9D1D9).copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clickable {
                            val nextPan = if (pan >= 0.9f) 0.1f else pan + 0.2f
                            onPanChange(nextPan)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Outer grey circular outline ring
                        drawCircle(
                            color = Color(0xFF1E2D4A),
                            radius = r * 0.8f,
                            center = center,
                            style = Stroke(width = 3f)
                        )

                        // Neon cyan encoder value sweep path arc
                        val sweepAngle = (pan * 280f)
                        drawArc(
                            color = Color(0xFF00F0FF),
                            startAngle = 130f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - r * 0.8f, center.y - r * 0.8f),
                            size = Size(r * 1.6f, r * 1.6f),
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )

                        // Encoder tick line
                        val needleAngle = 130f + sweepAngle
                        val needleLen = r * 0.7f
                        val endX = center.x + needleLen * cos(needleAngle * PI / 180f).toFloat()
                        val endY = center.y + needleLen * sin(needleAngle * PI / 180f).toFloat()

                        drawLine(
                            color = Color.White,
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                
                val panLabel = if (pan < 0.45f) "L${((0.5 - pan) * 200).toInt()}" else if (pan > 0.55f) "R${((pan - 0.5) * 200).toInt()}" else "C"
                Text(
                    text = panLabel,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54A)
                )
            }

            // High Decibel vertical meter layout + Level Fader Volume Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High precision LED VU input volume indicator meter on the left
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(14.dp)
                        .background(Color(0xFF030A16), RoundedCornerShape(2.dp))
                        .border(0.5.dp, Color(0xFF1E2D4A), RoundedCornerShape(2.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cellH = size.height / 10f
                        val rawVol = if (stem.isMuted) 0f else stem.volume

                        for (i in 0 until 10) {
                            val valTarget = (10 - i) / 10f
                            val isLit = rawVol >= valTarget
                            
                            val litColor = when {
                                i < 2 -> Color(0xFFFF5A5A)  // Red Peak
                                i < 4 -> Color(0xFFFFD54A)  // Yellow Warning
                                else -> Color(0xFF00D68F)   // Green standard
                            }

                            val color = if (isLit) litColor else Color(0xFF1E2D4A).copy(alpha = 0.25f)
                            drawRect(
                                color = color,
                                topLeft = Offset(1.5f, i * cellH + 1f),
                                size = Size(size.width - 3f, cellH - 2f)
                            )
                        }
                    }
                }

                // Decibel slider fader controller track
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = stem.volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00B7FF),
                            inactiveTrackColor = Color(0xFF162235),
                            thumbColor = Color.White
                        ),
                        modifier = Modifier
                            .rotate(270f)
                            .width(120.dp)
                            .height(30.dp)
                    )
                }
            }

            // S / M buttons (Solo & Mute) at bottom of strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // SOLO Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (stem.isSoloed) Color(0xFFFFD54A) else Color(0xFF162235))
                        .clickable { onSoloToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (stem.isSoloed) Color(0xFF030A16) else Color.White
                    )
                }

                // MUTE Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (stem.isMuted) Color(0xFFFF5A5A) else Color(0xFF162235))
                        .clickable { onMuteToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (stem.isMuted) Color(0xFF030A16) else Color.White
                    )
                }
            }
        }
    }
}
