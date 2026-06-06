package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.draw.scale
import com.example.model.AnalyzerUiState
import com.example.model.AnalyzerViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioRestorationTab(
    uiState: AnalyzerUiState,
    viewModel: AnalyzerViewModel,
    modifier: Modifier = Modifier
) {
    var isDenoiserActive by remember { mutableStateOf(true) }
    var isHumFilterActive by remember { mutableStateOf(false) }
    var isDeclipperActive by remember { mutableStateOf(true) }
    var isStereoActive by remember { mutableStateOf(false) }

    // Bounce VU needle slightly when playing
    val infiniteTransition = rememberInfiniteTransition(label = "VU needle bounce")
    val vuRotationOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VU rotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030A16)) // Cosmic Deep Navy
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. PRO RESPONSIVE RMS/VU ANALOG LEVEL METERS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1726)),
            border = BorderStroke(1.dp, Color(0xFF1E2D4A))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DUAL VINTAGE RMS / VU RESPONSE MONITOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // LEFT VU Needle Meter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .background(Color(0xFF030A16), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw analog dial background arc
                            drawArc(
                                color = Color(0xFF1E2D4A),
                                startAngle = 200f,
                                sweepAngle = 140f,
                                useCenter = false,
                                style = Stroke(width = 6f, cap = StrokeCap.Round),
                                topLeft = Offset(w * 0.1f, h * 0.2f),
                                size = Size(w * 0.8f, h * 1.3f)
                            )

                            // Peak threshold zone arc (Red zone)
                            drawArc(
                                color = Color(0xFFFF5A5A),
                                startAngle = 295f,
                                sweepAngle = 45f,
                                useCenter = false,
                                style = Stroke(width = 6f, cap = StrokeCap.Round),
                                topLeft = Offset(w * 0.1f, h * 0.2f),
                                size = Size(w * 0.8f, h * 1.3f)
                            )

                            // Tick marks
                            for (tick in -4..4) {
                                val tickAngle = 270f + (tick * 15f)
                                val angleRad = tickAngle * PI / 180f
                                val startR = w * 0.34f
                                val endR = w * 0.39f
                                drawLine(
                                    color = if (tick >= 2) Color(0xFFFF5A5A) else Color(0xFFC9D1D9).copy(alpha = 0.5f),
                                    start = Offset(w/2f + startR * cos(angleRad).toFloat(), h * 0.9f + startR * sin(angleRad).toFloat()),
                                    end = Offset(w/2f + endR * cos(angleRad).toFloat(), h * 0.9f + endR * sin(angleRad).toFloat()),
                                    strokeWidth = 2.5f
                                )
                            }

                            // Moving physical colored needle
                            val needleAngle = 270f + (if (uiState.isPlaying) vuRotationOffset else -25f)
                            val needleRad = needleAngle * PI / 180f
                            val needleLen = h * 0.8f

                            drawLine(
                                color = Color(0xFFFFD54A), // Royal Gold physical needle
                                start = Offset(w / 2f, h * 0.9f),
                                end = Offset(w / 2f + needleLen * cos(needleRad).toFloat(), h * 0.9f + needleLen * sin(needleRad).toFloat()),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }

                        Text(
                            text = "CH. 1 LEFT VU (RMS)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC9D1D9).copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                        )
                    }

                    // RIGHT VU Needle Meter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .background(Color(0xFF030A16), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            drawArc(
                                color = Color(0xFF1E2D4A),
                                startAngle = 200f,
                                sweepAngle = 140f,
                                useCenter = false,
                                style = Stroke(width = 6f, cap = StrokeCap.Round),
                                topLeft = Offset(w * 0.1f, h * 0.2f),
                                size = Size(w * 0.8f, h * 1.3f)
                            )

                            drawArc(
                                color = Color(0xFFFF5A5A),
                                startAngle = 295f,
                                sweepAngle = 45f,
                                useCenter = false,
                                style = Stroke(width = 6f, cap = StrokeCap.Round),
                                topLeft = Offset(w * 0.1f, h * 0.2f),
                                size = Size(w * 0.8f, h * 1.3f)
                            )

                            for (tick in -4..4) {
                                val tickAngle = 270f + (tick * 15f)
                                val angleRad = tickAngle * PI / 180f
                                val startR = w * 0.34f
                                val endR = w * 0.39f
                                drawLine(
                                    color = if (tick >= 2) Color(0xFFFF5A5A) else Color(0xFFC9D1D9).copy(alpha = 0.5f),
                                    start = Offset(w/2f + startR * cos(angleRad).toFloat(), h * 0.9f + startR * sin(angleRad).toFloat()),
                                    end = Offset(w/2f + endR * cos(angleRad).toFloat(), h * 0.9f + endR * sin(angleRad).toFloat()),
                                    strokeWidth = 2.5f
                                )
                            }

                            // Moving physical colored needle slightly out of sync for authentic stereo feel
                            val offsetR = vuRotationOffset * 0.88f + (if (uiState.isPlaying) -2f else -35f)
                            val needleAngle = 270f + offsetR
                            val needleRad = needleAngle * PI / 180f
                            val needleLen = h * 0.8f

                            drawLine(
                                color = Color(0xFF00F0FF), // Neon cyan needle for Ch. 2
                                start = Offset(w / 2f, h * 0.9f),
                                end = Offset(w / 2f + needleLen * cos(needleRad).toFloat(), h * 0.9f + needleLen * sin(needleRad).toFloat()),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }

                        Text(
                            text = "CH. 2 RIGHT VU (RMS)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC9D1D9).copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }

        // --- 2. AUDIO RESTORATION CLEANUP CONTROL LIST ---
        Text(
            text = "LOCAL DIRECT-SIGNAL-PROCESSING (DSP) BYPASS SWITCHBOARD",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFC9D1D9),
            letterSpacing = 1.sp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0D1726), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Option 1: Noise Denoiser
            RestorationControlRow(
                title = "1. LOCAL NOISE DENOISER",
                description = "Filters dynamic continuous studio high-frequency sizzles, ambient room hiss, & microphone pre-amp noise.",
                isActive = isDenoiserActive,
                onCheckedChange = { isDenoiserActive = it },
                onTriggerAction = { viewModel.runAudioRestoration("NOISE_REDUCTION") }
            )

            HorizontalDivider(color = Color(0xFF1E2D4A), thickness = 0.5.dp)

            // Option 2: Dynamic 60Hz Ground Hum Filter
            RestorationControlRow(
                title = "2. DYNAMIC HUM FILTER (60Hz / 120Hz)",
                description = "Notches precise electrical ground-loops, unbalanced line buzzes, & low hum frequencies from instrument cables.",
                isActive = isHumFilterActive,
                onCheckedChange = { isHumFilterActive = it },
                onTriggerAction = { viewModel.runAudioRestoration("HUM_REMOVAL") }
            )

            HorizontalDivider(color = Color(0xFF1E2D4A), thickness = 0.5.dp)

            // Option 3: Transient De-clipper
            RestorationControlRow(
                title = "3. TRANSIENT DE-CLIPPER",
                description = "Algorithmic repairs for digital peak clipping of wave samples during high input audio spikes.",
                isActive = isDeclipperActive,
                onCheckedChange = { isDeclipperActive = it },
                onTriggerAction = { viewModel.runAudioRestoration("CLIPPING_REPAIR") }
            )

            HorizontalDivider(color = Color(0xFF1E2D4A), thickness = 0.5.dp)

            // Option 4: Broad Stereo Enhancer
            RestorationControlRow(
                title = "4. BROAD STEREO ENHANCER",
                description = "Spreads isolated mono signals into full modern soundscapes mapping cross-panned instrument phase profiles.",
                isActive = isStereoActive,
                onCheckedChange = { isStereoActive = it },
                onTriggerAction = { viewModel.runAudioRestoration("STEREO_ENHANCER") }
            )
        }

        // --- 3. CONVOLUTION HISTORY MONITOR ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1726)),
            border = BorderStroke(1.dp, Color(0xFF1E2D4A))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ACTIVE CONVOLUTION STATUS HISTORY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54A),
                    letterSpacing = 0.5.sp
                )

                if (uiState.isRestoring) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF00F0FF),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Executing DSP restoration convolutions on filesystem buffers...",
                            fontSize = 11.sp,
                            color = Color(0xFF00F0FF)
                        )
                    }
                } else if (uiState.restorationsApplied.isEmpty()) {
                    Text(
                        text = "No real-time processes compiled yet. Toggle and run switches above to clear audio buffers.",
                        fontSize = 11.sp,
                        color = Color(0xFFC9D1D9).copy(alpha = 0.6f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.restorationsApplied.takeLast(3).reversed().forEach { restorationText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF030A16))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = restorationText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF00D68F)
                                )
                                Text(
                                    text = "✓ OK (LOCAL DSP)",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFC9D1D9).copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestorationControlRow(
    title: String,
    description: String,
    isActive: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTriggerAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) Color(0xFF00F0FF) else Color.White
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF00D68F).copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "BYPASS Bypassed",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00D68F)
                        )
                    }
                }
            }
            Text(
                text = description,
                fontSize = 10.sp,
                color = Color(0xFFC9D1D9).copy(alpha = 0.62f),
                lineHeight = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Apply DSP Run Action Button
            Button(
                onClick = onTriggerAction,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D4A)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("RUN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Toggle switch to activate/deactivate bypass status
            Switch(
                checked = isActive,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF030A16),
                    checkedTrackColor = Color(0xFF00B7FF),
                    uncheckedThumbColor = Color(0xFFC9D1D9),
                    uncheckedTrackColor = Color(0xFF162235)
                ),
                modifier = Modifier.scale(0.82f)
            )
        }
    }
}
