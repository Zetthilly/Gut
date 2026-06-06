package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AnalyzerViewModel
import com.example.model.AnalyzerUiState

/**
 * Flagship Master Orchestrator UI Screen for HZ CHORD AI.
 * Reconciles:
 * - A unified Top Action Bar with interactive status counters.
 * - Tabbed navigation states (MATRIX / TIMELINE / COGNITIVE / SETTINGS).
 * - Real-time active voicing indicators.
 * - Hardware playback controller at the persistent base footer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HzChordAiMainScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyzerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E5FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                        Column {
                            Text(
                                "HZ CHORD AI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                "Next-gen Pitch & African Style Analysis",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Main menu drawer",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Quick Status Counter Badges
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (uiState.isSeparating) Color(0xFFFF8A80) else Color(0xFF2E7D32))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (uiState.isSeparating) "Separating..." else "AI Ready",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF141311)
                )
            )
        },
        bottomBar = {
            // Hardware Controller HUD permanently seated at the base footer
            PlaybackDspController(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hz_playback_dsp_controller")
            )
        },
        containerColor = Color(0xFF141311)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            
            // Sub-navigation workstation selector deck (Matrix / Timeline Scroll / Cognitive Details)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1D1B18))
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val menuTabs = listOf(
                    "DASHBOARD" to "Home",
                    "ANALYZER" to "Live",
                    "STEMS" to "Mixer",
                    "REDUCE" to "Cleanup",
                    "SESSIONS" to "History"
                )

                menuTabs.forEach { (tabId, label) ->
                    val isActive = uiState.currentTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { viewModel.setVisualLayoutMode(tabId) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.Black else Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Display segments based on selected Tab configuration
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.currentTab) {
                    "DASHBOARD" -> {
                        HomeDashboardScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "ANALYZER" -> {
                        LiveAnalyzerScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "STEMS" -> {
                        StemMixerScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "REDUCE" -> {
                        AudioRestorationTab(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "SESSIONS" -> {
                        SessionManagerTab(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "MATRIX" -> {
                        ChordPadMatrix(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "TIMELINE" -> {
                        RollingWaveformTimeline(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "VOICINGS" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1D1A))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "LIVE ACTIVE VOICING CONSOL",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00E5FF),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val activeSymbol = uiState.detectedChords.getOrNull(uiState.activeChordIndex)?.chordSymbol ?: "F#add9"
                                ActiveVoicingPianoRoll(
                                    activeChordSymbol = activeSymbol,
                                    modifier = Modifier.height(200.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Highlighting optimal keyboard inversion voicings supporting the active chord progression sequence.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                    "THEORY" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1D1A))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "African Ethnomusicology Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF9E9580),
                                    fontWeight = FontWeight.Bold
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A27)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "DETECTED HARMONIC STYLE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = uiState.styleName,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF00E5FF)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Confidence Level: ${String.format("%.1f", uiState.styleConfidence * 100)}%",
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = uiState.styleDescription,
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A27)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "IMPLIED KEY / ROOTS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = uiState.impliedHarmony,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A27)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "PHRASE RECOGNITION AI (LOCATED LISTENER)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Last Detected Articulation",
                                                    fontSize = 11.sp,
                                                    color = Color.LightGray
                                                )
                                                Text(
                                                    text = uiState.lastDetectedGuitarPhrase,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF00E5FF)
                                                )
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF141311))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (uiState.lastDetectedGuitarPhrase.contains("No dynamic")) "IDLE" else "TRIGGERED",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (uiState.lastDetectedGuitarPhrase.contains("No dynamic")) Color.Gray else Color(0xFFFF8A80)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "HISTORICAL ORNAMENTAL LOGGER",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        if (uiState.guitarPhraseHistory.isEmpty()) {
                                            Text(
                                                text = "No guitar ornamental phrases detected yet. Strike keys above or start microphone to analyze live Hammer-ons, Pull-offs, Slides, and String Bends.",
                                                fontSize = 11.sp,
                                                color = Color.LightGray
                                            )
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                uiState.guitarPhraseHistory.take(5).forEach { phrase ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF141311))
                                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(phrase, fontSize = 11.sp, color = Color.White)
                                                        Text("🎸", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "SESSIONS" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1D1A))
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "SESSION MANAGER & SYSTEM SETTINGS",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF9E9580),
                                    fontWeight = FontWeight.Bold
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A27)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "PREFERENCES (PERSISTENT DATASTORE)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Spelling: ${if (uiState.useFlatNaming) "Flats" else "Sharps"}",
                                                    fontSize = 13.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Choose Flats (Db/Eb) vs Sharps (C#/D#)",
                                                    fontSize = 10.sp,
                                                    color = Color.LightGray
                                                )
                                            }
                                            Switch(
                                                checked = uiState.useFlatNaming,
                                                onCheckedChange = { viewModel.toggleNamingConvention() },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color(0xFF00E5FF),
                                                    checkedTrackColor = Color(0xFF1E2D4A)
                                                )
                                            )
                                        }

                                        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

                                        var showTuningDialog by remember { mutableStateOf(false) }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Tuning Reference",
                                                    fontSize = 13.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = uiState.tuningChoice,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF00E5FF)
                                                )
                                            }
                                            Button(
                                                onClick = { showTuningDialog = true },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3C39)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text("Adjust", fontSize = 11.sp, color = Color.White)
                                            }
                                        }

                                        if (showTuningDialog) {
                                            AlertDialog(
                                                onDismissRequest = { showTuningDialog = false },
                                                title = { Text("Select Reference Tuning") },
                                                text = {
                                                    Column {
                                                        val tunings = listOf("Standard A=440Hz", "Pythagorean A=432Hz", "Baroque A=415Hz", "Scientific A=430.5Hz")
                                                        tunings.forEach { tune ->
                                                            Text(
                                                                text = tune,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        viewModel.setTuningChoice(tune)
                                                                        showTuningDialog = false
                                                                    }
                                                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                },
                                                confirmButton = {}
                                            )
                                        }
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A27)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "OFFLINE AUDIO RESTORATION SUITE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        
                                        Text(
                                            text = "Run native digital signal processing (DSP) algorithms directly on local waveform files to remove ground-loop hum and field noise.",
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )

                                        if (uiState.isRestoring) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                LinearProgressIndicator(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = Color(0xFF00E5FF),
                                                    trackColor = Color(0xFF1E1D1A)
                                                )
                                                Text(
                                                    text = "Executing DSP restoration convolutions...",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF00E5FF),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Button(
                                                    onClick = { viewModel.runAudioRestoration("HUM_REMOVAL") },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3C39)),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                ) {
                                                    Text("60Hz Hum", fontSize = 10.sp, color = Color.White)
                                                }
                                                
                                                Button(
                                                    onClick = { viewModel.runAudioRestoration("NOISE_REDUCTION") },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3C39)),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Denoise", fontSize = 10.sp, color = Color.White)
                                                }
                                                
                                                Button(
                                                    onClick = { viewModel.runAudioRestoration("CLIPPING_REPAIR") },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3C39)),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Declipping", fontSize = 10.sp, color = Color.White)
                                                }
                                            }
                                        }

                                        if (uiState.restorationsApplied.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "COMPLETED CONVOLUTION HISTORY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                uiState.restorationsApplied.takeLast(3).reversed().forEach { rest ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(Color(0xFF141311))
                                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(rest, fontSize = 10.sp, color = Color(0xFF81C784))
                                                        Text("✓ DSP", fontSize = 9.sp, color = Color.Gray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A27)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "ARCHIVE CURRENT CHORD SHEET",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )

                                        var sessionTitle by remember { mutableStateOf("") }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = sessionTitle,
                                                onValueChange = { sessionTitle = it },
                                                placeholder = { Text("Track / Session Name", fontSize = 12.sp, color = Color.Gray) },
                                                modifier = Modifier.weight(1f),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color(0xFF1E1D1A),
                                                    unfocusedContainerColor = Color(0xFF1E1D1A),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                ),
                                                singleLine = true
                                            )

                                            Button(
                                                onClick = {
                                                    if (sessionTitle.trim().isNotEmpty()) {
                                                        viewModel.saveCurrentSession(sessionTitle.trim())
                                                        sessionTitle = ""
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                            ) {
                                                Text("Save", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A27)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxSize()
                                    ) {
                                        Text(
                                            text = "LOCAL SQLITE HISTORICAL DATABASE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (uiState.savedSessions.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "No historical analysis folders yet.",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        } else {
                                            androidx.compose.foundation.lazy.LazyColumn(
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                items(uiState.savedSessions.size) { index ->
                                                    val session = uiState.savedSessions[index]
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFF1E1D1A))
                                                            .clickable { viewModel.loadSavedSession(session.id) }
                                                            .padding(8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = session.title,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Text("Key: ${session.rootKey}", fontSize = 10.sp, color = Color(0xFF00E5FF))
                                                                Text("Tempo: ${session.tempoBpm} BPM", fontSize = 10.sp, color = Color.LightGray)
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = { viewModel.deleteSavedSession(session.id) }
                                                        ) {
                                                            Text("🗑", color = Color(0xFFFF8A80), fontSize = 14.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Overlay our magnificent floating Export Status Tracker
                if (uiState.showExportIndicator) {
                    ExportStatusIndicator(
                        exportType = uiState.exportType,
                        progress = uiState.exportProgress,
                        statusMessage = uiState.exportStatusMessage,
                        onDismiss = { viewModel.dismissExportIndicator() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .testTag("export_status_indicator")
                    )
                }
            }
        }
    }
}

/**
 * Clean, modern Material 3 status monitoring panel presenting file synthesis telemetry.
 */
@Composable
fun ExportStatusIndicator(
    exportType: String?,
    progress: Float,
    statusMessage: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (exportType == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A27)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00E5FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = exportType,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "ON-DEVICE SYNTHESIS PROGRESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("✕", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = statusMessage,
                fontSize = 11.sp,
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth()
            )

            // Progress bar and numeric tracking
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF141311)
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}
