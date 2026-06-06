package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalyzerUiState
import com.example.model.AnalyzerViewModel

/**
 * A hardware style playback controller mimicking Layout Feature C from 1000036522.jpg.
 * Features:
 * - Playback time seeker slider bar
 * - Fast Speed scaling (Speed configuration selection dialog)
 * - Quick semitone transposition adjustment override buttons (e.g. "-1" step override)
 * - Timeline loop toggles
 */
@Composable
fun PlaybackDspController(
    uiState: AnalyzerUiState,
    viewModel: AnalyzerViewModel,
    modifier: Modifier = Modifier
) {
    var isLoopEnabled by remember { mutableStateOf(true) }
    var speedMultiplier by remember { mutableStateOf("1.0x") }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val formattedProgMs = uiState.currentProgressMs
    val formattedDurationMs = uiState.totalDurationMs

    // Seconds and minutes formatted
    val progMin = (formattedProgMs / 1000) / 60
    val progSec = (formattedProgMs / 1000) % 60
    val durMin = (formattedDurationMs / 1000) / 60
    val durSec = (formattedDurationMs / 1000) % 60

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E2D4A)) // Deep hardware blue container
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Interactive quick semitone transpose step indicator
            OutlinedButton(
                onClick = {
                    viewModel.updateTranspose(uiState.transposeSemitones - 1)
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.weight(1.2f)
            ) {
                Text(
                    text = "${if (uiState.transposeSemitones >= 0) "+" else ""}${uiState.transposeSemitones}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Playback Speed adjustments
            OutlinedButton(
                onClick = { showSpeedDialog = true },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.weight(1.5f)
            ) {
                Text(
                    text = "Speed $speedMultiplier",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
            }

            // Persistent Timeline Loop Action Key
            Button(
                onClick = { isLoopEnabled = !isLoopEnabled },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoopEnabled) Color(0xFF00E5FF) else Color(0xFF2C3E50)
                ),
                modifier = Modifier.weight(1.4f)
            ) {
                Text(
                    text = "Loop",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLoopEnabled) Color.Black else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Seeking slide rail bar matching look-ahead timeline controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = uiState.currentProgressMs.toFloat().coerceIn(0f, uiState.totalDurationMs.toFloat()),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..uiState.totalDurationMs.toFloat(),
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color.DarkGray,
                    thumbColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Progress Timestamp representation (e.g. 0:00 / 2:00)
            Text(
                text = "${String.format("%02d", progMin)}:${String.format("%02d", progSec)} / ${String.format("%02d", durMin)}:${String.format("%02d", durSec)}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.wrapContentWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Transport Command line (Play/Pause, Seek forward, Seek backward buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.seekTo((uiState.currentProgressMs - 5000L).coerceAtLeast(0L)) }) {
                Text("◀◀", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Large digital Play/Pause Center button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(if (uiState.isPlaying) Color(0xFFFF8A80) else Color(0xFF00E5FF))
                    .clickable { viewModel.togglePlayback() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.isPlaying) "⏸" else "▶",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            IconButton(onClick = { viewModel.seekTo((uiState.currentProgressMs + 5000L).coerceAtMost(uiState.totalDurationMs)) }) {
                Text("▶▶", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Select Playback Speed") },
            text = {
                Column {
                    val speeds = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")
                    speeds.forEach { speed ->
                        Text(
                            text = speed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    speedMultiplier = speed
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}
