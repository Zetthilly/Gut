package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalyzerUiState
import com.example.model.AnalyzerViewModel
import com.example.model.DataExportEngine
import com.example.model.AudioExportManager
import com.example.db.SessionEntity
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    uiState: AnalyzerUiState,
    viewModel: AnalyzerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportEngine = remember { DataExportEngine() }

    var showExportDialog by remember { mutableStateOf(false) }
    var selectedSessionToExport by remember { mutableStateOf<SessionEntity?>(null) }
    var exportDetailsText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E0C)) // Cosmic Slate Black backing
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CREATIVE STUDIO WORKSPACE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1F1E1B))
                    .border(1.dp, Color(0xFF2C2A27), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (uiState.isRecording) Color(0xFF00E5FF) else Color(0xFF2E7D32))
                    )
                    Text(
                        text = if (uiState.isRecording) "RECORDING" else "MONITOR ON",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                }
            }
        }

        // Section 1: Current Environment summaries
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1D1A)),
            border = BorderStroke(1.dp, Color(0xFF2C2A27)),
            modifier = Modifier.fillMaxWidth().testTag("env_summary_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CURRENT COGNITIVE ENVIRONMENT STATUS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summary Badge 1: Tuning Frequency
                    SummaryItemBadge(
                        title = "Calibration Reference",
                        value = uiState.tuningChoice,
                        iconColor = Color(0xFFFFD700)
                    )

                    // Summary Badge 2: Spelling Naming
                    SummaryItemBadge(
                        title = "Spelling Model",
                        value = if (uiState.useFlatNaming) "Enharmonic Flats" else "Enharmonic Sharps",
                        iconColor = Color(0xFF00E5FF)
                    )

                    // Summary Badge 3: Active Key
                    SummaryItemBadge(
                        title = "Implied Musical Key",
                        value = uiState.impliedHarmony.ifBlank { "Unidentified" },
                        iconColor = Color(0xFFD500F9)
                    )

                    // Summary Badge 4: Guitar Phrase articulations count
                    SummaryItemBadge(
                        title = "Detected Articulations",
                        value = "${uiState.guitarPhraseHistory.size} in log",
                        iconColor = Color(0xFFFF8A80)
                    )
                }
            }
        }

        // Section 2: Rapid action shortcuts
        Text(
            text = "RAPID AUDIO AND STYLE SHORTCUTS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 1.2.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Shortcut A: Quick Record
            OutlinedButton(
                onClick = {
                    viewModel.toggleMicrophone {
                        Toast.makeText(context, "Microphone access configured successfully", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("shortcut_quick_record"),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = "Quick Mic Toggle",
                        tint = if (uiState.isRecording) Color(0xFFFF8A80) else Color(0xFF00E5FF)
                    )
                    Text(
                        text = if (uiState.isRecording) "Stop Rec" else "Quick Rec",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Shortcut B: Quick Analyze
            Button(
                onClick = {
                    // Strike comfortable sequence of root C4 and F4 keys to trigger instant harmonic analysis Style update
                    viewModel.strikeSimulationNote(60) // C4
                    viewModel.strikeSimulationNote(65) // F4
                    Toast.makeText(context, "Synthesizer sequence injected", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("shortcut_quick_analyze"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2A27))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Simulate Pitch Analysis",
                        tint = Color(0xFFFFD700)
                    )
                    Text(
                        text = "Quick Analyze",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Section 3: List hooks for accessing recent local sessions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT AUDIO SESSIONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "${uiState.savedSessions.size} Folders Stored",
                fontSize = 10.sp,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Medium
            )
        }

        if (uiState.savedSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1D1A))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your Offline SQLite database is empty.",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Use the core analysis tabs to map chord progressions and save your active folders to view them compiled locally.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.savedSessions.size) { index ->
                    val session = uiState.savedSessions[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1D1A))
                            .clickable {
                                viewModel.loadSavedSession(session.id)
                                Toast.makeText(context, "Loaded session: ${session.title}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Key: ${session.rootKey}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${session.tempoBpm} BPM",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Len: ${session.durationMs / 1000}s",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Launch localized data serialization routines export panel
                            IconButton(
                                onClick = {
                                    selectedSessionToExport = session
                                    showExportDialog = true
                                },
                                modifier = Modifier.size(36.dp).testTag("export_btn_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export Session",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteSavedSession(session.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Session",
                                    tint = Color(0xFFFF8A80),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Center Dialog Panel
    if (showExportDialog && selectedSessionToExport != null) {
        val session = selectedSessionToExport!!
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportDetailsText = ""
            },
            title = {
                Text(
                    text = "LOCAL EXPORT CENTER",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Compile session: \"${session.title}\" directly on-device into professional audio-theory formats.",
                        fontSize = 12.sp,
                        color = Color.White
                    )

                    if (exportDetailsText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F0E0C))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = exportDetailsText,
                                fontSize = 10.sp,
                                color = Color(0xFF81C784),
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Export PDF
                        Button(
                            onClick = {
                                try {
                                    viewModel.startExportTracking("PDF", "Compiling professional PDF chord sheet and layout structures...")
                                    val pdfBytes = exportEngine.exportToPdf(session, uiState.detectedChords)
                                    val file = writeSandboxFile(context, "${session.title}_report.pdf", pdfBytes)
                                    val resultMsg = "Successfully written Document format!\nPath: ${file.absolutePath}\n\nAttribution systematically embedded:\n© HZ CHORD AI - Generated"
                                    exportDetailsText = resultMsg
                                    viewModel.completeExportTracking("PDF", "Saved PDF report: ${file.name}. Copyright and centered developer annotations appended.")
                                } catch (e: Exception) {
                                    exportDetailsText = "PDF rendering failed: ${e.message}"
                                    viewModel.completeExportTracking("PDF", "PDF rendering failed: ${e.message}", success = false)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3C39)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("PDF", fontSize = 11.sp, color = Color.White)
                        }

                        // Export CSV
                        Button(
                            onClick = {
                                try {
                                    viewModel.startExportTracking("CSV", "Structuring chronological records to flat CSV format...")
                                    val csvStr = exportEngine.exportToCsv(session, uiState.detectedChords)
                                    val file = writeSandboxFile(context, "${session.title}_tracker.csv", csvStr.toByteArray())
                                    val resultMsg = "Successfully written flat CSV!\nPath: ${file.absolutePath}\n\nFirst lines:\n${csvStr.lineSequence().take(4).joinToString("\n")}"
                                    exportDetailsText = resultMsg
                                    viewModel.completeExportTracking("CSV", "Saved flat sheet tracker CSV.")
                                } catch (e: Exception) {
                                    exportDetailsText = "CSV dump failed: ${e.message}"
                                    viewModel.completeExportTracking("CSV", "CSV dump failed: ${e.message}", success = false)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3C39)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("CSV", fontSize = 11.sp, color = Color.White)
                        }

                        // Export JSON
                        Button(
                            onClick = {
                                try {
                                    viewModel.startExportTracking("JSON", "Serializing timeline intervals and metadata to JSON schema...")
                                    val jsonStr = exportEngine.exportToJson(session, uiState.detectedChords)
                                    val file = writeSandboxFile(context, "${session.title}_data.json", jsonStr.toByteArray())
                                    val resultMsg = "Successfully compiled raw JSON!\nPath: ${file.absolutePath}\n\nOutput segment:\n${jsonStr.take(180)}..."
                                    exportDetailsText = resultMsg
                                    viewModel.completeExportTracking("JSON", "Saved structured JSON session schema.")
                                } catch (e: Exception) {
                                    exportDetailsText = "JSON compilation failed: ${e.message}"
                                    viewModel.completeExportTracking("JSON", "JSON compilation failed: ${e.message}", success = false)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3C39)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("JSON", fontSize = 11.sp, color = Color.White)
                        }

                        // Export MIDI
                        Button(
                            onClick = {
                                try {
                                    viewModel.startExportTracking("MIDI", "Synthesizing SMF Type 1 multi-track binary events with developer text tags...")
                                    val midiBytes = exportEngine.exportToMidi(session, uiState.detectedChords)
                                    val file = writeSandboxFile(context, "${session.title}_sequence.mid", midiBytes)
                                    val resultMsg = "Successfully synthesized binary MIDI Track!\nPath: ${file.absolutePath}\nBytes: ${midiBytes.size}\nFormat: SMF Type 1 (Multi-Track, Joseph Hilary Zulukwa metadata embedded)"
                                    exportDetailsText = resultMsg
                                    viewModel.completeExportTracking("MIDI", "Saved Standard MIDI File (SMF Type 1). Developer attribution embedded inside MIDI Track Name meta tags.")
                                } catch (e: Exception) {
                                    exportDetailsText = "MIDI generation failed: ${e.message}"
                                    viewModel.completeExportTracking("MIDI", "MIDI generation failed: ${e.message}", success = false)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3C39)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("MIDI", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "WAV STEM EXPORTS (100% OFFLINE)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Export All Stems as ZIP
                        Button(
                            onClick = {
                                try {
                                    viewModel.startExportTracking("AUDIO_ZIP", "Mixing volumes and packaging all 12 audio stems into a compressed ZIP file...")
                                    val audioExporter = AudioExportManager(context)
                                    val uri = audioExporter.exportAllStemsAsZip(uiState.stems, "HZ_12Stems_${session.title.replace(" ", "_")}.zip")
                                    if (uri != null) {
                                        val resultMsg = "Successfully mixed & compressed all 12 stems to Public Downloads!\nFormat: ZIP Archive of WAV files (Workstation volume levels applied)\nUri: $uri"
                                        exportDetailsText = resultMsg
                                        viewModel.completeExportTracking("AUDIO_ZIP", "Zipped 12 stems successfully! Copy stored directly to public downloads.")
                                    } else {
                                        val resultMsg = "Failed to export stems to Public Downloads. Verify file system permissions."
                                        exportDetailsText = resultMsg
                                        viewModel.completeExportTracking("AUDIO_ZIP", "ZIP export failed to locate destination folder.", success = false)
                                    }
                                } catch (e: Exception) {
                                    exportDetailsText = "Stem ZIP Export failed: ${e.message}"
                                    viewModel.completeExportTracking("AUDIO_ZIP", "ZIP export failed: ${e.message}", success = false)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("ALL STEMS (ZIP)", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        // Export Lead/Active Stem
                        Button(
                            onClick = {
                                try {
                                    val audioExporter = AudioExportManager(context)
                                    val stemToExport = uiState.stems.find { !it.isMuted && it.filePath.isNotEmpty() } ?: uiState.stems.firstOrNull { it.filePath.isNotEmpty() }
                                    if (stemToExport != null) {
                                        viewModel.startExportTracking("AUDIO_SINGLE", "Filtering noise, gate levels, and scaling gain parameters for: ${stemToExport.name}...")
                                        val uri = audioExporter.exportSingleStem(stemToExport)
                                        if (uri != null) {
                                            val resultMsg = "Successfully exported single active stem to Public Downloads!\nStem Track: ${stemToExport.name}\nFormat: Gain-adjusted WAV\nUri: $uri"
                                            exportDetailsText = resultMsg
                                            viewModel.completeExportTracking("AUDIO_SINGLE", "Saved single mixed WAV stem: ${stemToExport.name} directly to public downloads folder.")
                                        } else {
                                            val resultMsg = "Failed to write stem file. Please make sure the stem file is loaded."
                                            exportDetailsText = resultMsg
                                            viewModel.completeExportTracking("AUDIO_SINGLE", "Single stem export failed: File writing rejected.", success = false)
                                        }
                                    } else {
                                        exportDetailsText = "No active individual stem files extracted yet. Try separating an audio track first!"
                                        viewModel.completeExportTracking("AUDIO_SINGLE", "No active stems loaded yet.", success = false)
                                    }
                                } catch (e: Exception) {
                                    exportDetailsText = "Single Stem Export failed: ${e.message}"
                                    viewModel.completeExportTracking("AUDIO_SINGLE", "Single stem export failed: ${e.message}", success = false)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E3C39)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("ACTIVE STEM", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportDetailsText = ""
                }) {
                    Text("DONE", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SummaryItemBadge(
    title: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F0E0C))
            .border(0.5.dp, Color(0xFF2C2A27), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(iconColor)
        )
        Column {
            Text(title, fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun writeSandboxFile(context: Context, filename: String, data: ByteArray): File {
    val dir = File(context.filesDir, "exports")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val file = File(dir, filename.replace(" ", "_"))
    FileOutputStream(file).use { out ->
        out.write(data)
    }
    return file
}
