package com.example.model

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.db.SessionEntity
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * High-fidelity client-side compilation engine for HZ CHORD AI.
 * Handles on-device compilation of track summaries, chords, and style classifications into:
 * 1. PDF (via native Android PdfDocument with styled headers and standard attribution footer).
 * 2. CSV (clean comma-separated values).
 * 3. JSON (standard formatted string serialization).
 * 4. MIDI (custom binary generator outputting valid Standard MIDI Files (SMF Type 0) containing matching chord root notes).
 */
class DataExportEngine {

    companion object {
        private const val ATTRIBUTION = "HZ CHORD AI - Generated"
    }

    /**
     * Renders a styled vector-based visual document representation of the analysis session
     * and saves it as a PDF byte stream using Android's native PdfDocument engine.
     */
    fun exportToPdf(session: SessionEntity, chords: List<DetectedChord>): ByteArray {
        return PdfExportEngine().genChordSheetPdf(
            sessionLabel = session.title,
            rootKey = session.rootKey,
            tempoBpm = session.tempoBpm,
            chords = chords
        )
    }

    /**
     * Formats chords in standard comma-separated values layout, embedding required branding info.
     */
    fun exportToCsv(session: SessionEntity, chords: List<DetectedChord>): String {
        val builder = StringBuilder()
        // Systematic Metadata Headers
        builder.append("# $ATTRIBUTION\n")
        builder.append("# Session title: ${session.title}\n")
        builder.append("# Harmonic Key: ${session.rootKey}\n")
        builder.append("# Tempo: ${session.tempoBpm} BPM\n")
        builder.append("TimestampMs,ChordSymbol,ScaleDegree,Confidence\n")
        
        for (chord in chords) {
            val degreeText = MusicTheoryUtils.convertToRomanNumeral(chord.chordSymbol, session.rootKey)
            builder.append("${chord.timestampMs},\"${chord.chordSymbol}\",\"${degreeText}\",${chord.confidence}\n")
        }
        return builder.toString()
    }

    /**
     * Formats details as a standard indented JSON string representation.
     */
    fun exportToJson(session: SessionEntity, chords: List<DetectedChord>): String {
        val builder = StringBuilder()
        builder.append("{\n")
        builder.append("  \"attribution\": \"$ATTRIBUTION\",\n")
        builder.append("  \"sessionTitle\": \"${session.title.replace("\"", "\\\"")}\",\n")
        builder.append("  \"timestamp\": ${session.timestamp},\n")
        builder.append("  \"durationMs\": ${session.durationMs},\n")
        builder.append("  \"tempoBpm\": ${session.tempoBpm},\n")
        builder.append("  \"rootKey\": \"${session.rootKey}\",\n")
        builder.append("  \"timeline\": [\n")
        for (i in chords.indices) {
            val chord = chords[i]
            val degreeText = MusicTheoryUtils.convertToRomanNumeral(chord.chordSymbol, session.rootKey)
            builder.append("    {\n")
            builder.append("      \"timestampMs\": ${chord.timestampMs},\n")
            builder.append("      \"chordSymbol\": \"${chord.chordSymbol}\",\n")
            builder.append("      \"scaleDegree\": \"${degreeText}\",\n")
            builder.append("      \"confidence\": ${chord.confidence}\n")
            builder.append("    }${if (i < chords.size - 1) "," else ""}\n")
        }
        builder.append("  ]\n")
        builder.append("}")
        return builder.toString()
    }

    /**
     * Generates a fully binary valid Standard MIDI File (SMF Type 1, multi track)
     * containing note-on and note-off events matching the chord root changes over time.
     */
    fun exportToMidi(session: SessionEntity, chords: List<DetectedChord>): ByteArray {
        return MidiExportEngine().compileChordsToMidi(
            chords = chords,
            rootKey = session.rootKey,
            tempoBpm = session.tempoBpm
        )
    }

    @Deprecated("Use overloaded version with SessionEntity")
    fun exportToMidi(chords: List<DetectedChord>): ByteArray {
        return MidiExportEngine().compileChordsToMidi(
            chords = chords,
            rootKey = "C",
            tempoBpm = 120
        )
    }
}
