package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * High-fidelity dynamic start screen utilizing a signature real-time custom waveform spectrum animation.
 * After lasting 2.5 seconds, it returns via onSplashComplete callback to transition to the main dashboard.
 */
@Composable
fun HzSplashScreen(
    onSplashComplete: () -> Unit
) {
    // 2.5 seconds timer delay
    LaunchedEffect(Unit) {
        delay(2500L)
        onSplashComplete()
    }

    // Interactive continuous frequency oscillations loop
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformOscillation")
    
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Layer1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Layer2"
    )

    val scaleState by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScaling"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B10)), // Core Deep Navy Cosmic Canvas
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing Ambient Visualizer Spectrum Sphere
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Waveform spectrum graphics drawn mathematically
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f
                    val waveCount = 48
                    val spacing = width / waveCount

                    val electricBlue = Color(0xFF00E5FF)
                    val cosmicPurple = Color(0xFFD500F9)
                    val royalGold = Color(0xFFFFD700)

                    // Draw Background Golden Glow Resonance
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(royalGold.copy(alpha = 0.12f * scaleState), Color.Transparent),
                            center = Offset(width / 2, height / 2),
                            radius = (width / 2) * scaleState
                        ),
                        radius = (width / 2) * scaleState
                    )

                    // Wave Layer A (Electric Blue)
                    for (i in 0 until waveCount) {
                        val x = i * spacing + (spacing / 2)
                        val progress = i.toFloat() / waveCount
                        // Generate sine curve with offset phase
                        val sinVal = kotlin.math.sin(progress * 2 * kotlin.math.PI * 2 + phase1).toFloat()
                        val barHeight = (height * 0.35f) * sinVal * kotlin.math.sin(progress * kotlin.math.PI).toFloat()

                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(electricBlue, cosmicPurple.copy(alpha = 0.4f))
                            ),
                            start = Offset(x, centerY - barHeight),
                            end = Offset(x, centerY + barHeight),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Wave Layer B (Cosmic Purple - out of phase)
                    for (i in 0 until waveCount) {
                        val x = i * spacing + (spacing / 2)
                        val progress = i.toFloat() / waveCount
                        val cosVal = kotlin.math.cos(progress * 2 * kotlin.math.PI * 3 + phase2).toFloat()
                        val barHeight = (height * 0.22f) * cosVal * kotlin.math.sin(progress * kotlin.math.PI).toFloat()

                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(cosmicPurple, electricBlue.copy(alpha = 0.3f))
                            ),
                            start = Offset(x, centerY - barHeight),
                            end = Offset(x, centerY + barHeight),
                            strokeWidth = 3.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Professional Core Application Typography pairing
            Text(
                text = "HZ CHORD AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Dynamic Key Detection & Acoustic Restoration",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFA0A5C0),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Attribution details
            Text(
                text = "OFFLINE-FIRST MUSIC THEORY ENGINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A6D80),
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "HZ CHORD AI - Generated",
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                letterSpacing = 1.sp
            )
        }
    }
}
