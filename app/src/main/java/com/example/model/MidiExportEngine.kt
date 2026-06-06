package com.example.model

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Pure-Kotlin binary compiler implementing Standard MIDI File (SMF Type 1) format.
 * Structures musical chords and analytical intervals into dual track structures:
 * - Track 0: Global master metadata (Tempo, Time Signature, Key Signature, Developer Attribution).
 * - Track 1: Harmonic note-on/off events sequence with developer metadata embedded in MIDI track name meta events.
 */
class MidiExportEngine {

    companion object {
        const val DEVELOPER_ATTRIBUTION = "HZ CHORD AI - Designed and Built by Joseph Hilary Zulukwa"
        private const val TICKS_PER_QUARTER_NOTE = 480 // Standard PPQ resolution
    }

    /**
     * Compiles chord snapshots into a valid binary SMF Type 1 multi-track file.
     */
    fun compileChordsToMidi(
        chords: List<DetectedChord>,
        rootKey: String,
        tempoBpm: Int
    ): ByteArray {
        val stream = ByteArrayOutputStream()

        // 1. Write MThd header chunk
        stream.write("MThd".toByteArray(StandardCharsets.US_ASCII))
        // Chunk size: 6 bytes
        stream.write(byteArrayOf(0, 0, 0, 6))
        // Format: SMF Type 1 (00 01), Track Count: 2 Tracks (00 02), Division: PPQ (01 E0 = 480)
        stream.write(byteArrayOf(0, 1, 0, 2, 0x01, 0xE0.toByte()))

        // 2. Compile TRACK 0 (Master Metadata Track)
        val track0Stream = ByteArrayOutputStream()
        
        // Track Name event (FF 03) embedding Developer Signature
        writeTrackNameMeta(track0Stream, DEVELOPER_ATTRIBUTION)

        // Time Signature event (FF 58 04 04 02 18 08 => 4/4 time signature)
        writeVarLengthCount(track0Stream, 0)
        track0Stream.write(byteArrayOf(0xFF.toByte(), 0x58, 0x04, 0x04, 0x02, 0x18, 0x08))

        // Key Signature event (FF 59 02 sf mi)
        val keySig = getKeySignatureValue(rootKey)
        writeVarLengthCount(track0Stream, 0)
        track0Stream.write(byteArrayOf(0xFF.toByte(), 0x59, 0x02, keySig[0], keySig[1]))

        // Tempo event (FF 51 03 ttt)
        // Microseconds per quarter note = 60,000,000 / BPM
        val microsecondsPerQuarter = (60000000L / tempoBpm).coerceIn(1L, 16777215L)
        writeVarLengthCount(track0Stream, 0)
        track0Stream.write(0xFF)
        track0Stream.write(0x51)
        track0Stream.write(0x03)
        track0Stream.write(((microsecondsPerQuarter shr 16) and 0xFF).toInt())
        track0Stream.write(((microsecondsPerQuarter shr 8) and 0xFF).toInt())
        track0Stream.write((microsecondsPerQuarter and 0xFF).toInt())

        // End of Track 0 metadata
        writeVarLengthCount(track0Stream, 0)
        track0Stream.write(byteArrayOf(0xFF.toByte(), 0x2F, 0x00))

        writeTrackChunk(stream, track0Stream.toByteArray())

        // 3. Compile TRACK 1 (Harmonic Chords Performance Track)
        val track1Stream = ByteArrayOutputStream()

        // Track Name event (FF 03) embedding Developer Signature
        writeTrackNameMeta(track1Stream, DEVELOPER_ATTRIBUTION)

        // Calculate ticks scale: At BMP, 1 beat (quarter note) = 480 ticks
        // Beats per second = BPM / 60
        // Beats per millisecond = BPM / 60000
        // Ticks per millisecond = (BPM / 60000.0) * 480.0
        val ticksPerMs = (tempoBpm / 60000.0) * TICKS_PER_QUARTER_NOTE
        
        var lastEventTick = 0L

        for (i in chords.indices) {
            val chord = chords[i]
            val chordNotes = parseChordToMidiNotes(chord.chordSymbol)
            if (chordNotes.isEmpty()) continue

            // Determine chronological location of this chord
            val startTick = (chord.timestampMs * ticksPerMs).toLong()
            
            // Determine duration: stretch to next chord, or 960 ticks (2 beats) for ending chord
            val endTick = if (i < chords.size - 1) {
                (chords[i + 1].timestampMs * ticksPerMs).toLong()
            } else {
                startTick + (TICKS_PER_QUARTER_NOTE * 2)
            }

            // A. Trigger Note-On events at startTick
            for (noteIndex in chordNotes.indices) {
                val note = chordNotes[noteIndex]
                val delta = if (noteIndex == 0) startTick - lastEventTick else 0L
                writeVarLengthCount(track1Stream, delta)
                
                track1Stream.write(0x90) // MIDI Note On (Channel Index 0)
                track1Stream.write(note)  // Key value
                track1Stream.write(90)   // Comfortable Velocity (accentual key leverage)
                
                if (noteIndex == 0) {
                    lastEventTick = startTick
                }
            }

            // B. Trigger Note-Off events at endTick
            for (noteIndex in chordNotes.indices) {
                val note = chordNotes[noteIndex]
                val delta = if (noteIndex == 0) endTick - lastEventTick else 0L
                writeVarLengthCount(track1Stream, delta)
                
                track1Stream.write(0x80) // MIDI Note Off (Channel Index 0)
                track1Stream.write(note)  // Key value
                track1Stream.write(0)    // Zero Off Velocity
                
                if (noteIndex == 0) {
                    lastEventTick = endTick
                }
            }
        }

        // End of Track 1 performance data
        writeVarLengthCount(track1Stream, 0)
        track1Stream.write(byteArrayOf(0xFF.toByte(), 0x2F, 0x00))

        writeTrackChunk(stream, track1Stream.toByteArray())

        return stream.toByteArray()
    }

    private fun writeTrackNameMeta(stream: ByteArrayOutputStream, text: String) {
        writeVarLengthCount(stream, 0) // Delta time 0
        stream.write(0xFF) // Meta marker
        stream.write(0x03) // Track Name marker
        val textBytes = text.toByteArray(StandardCharsets.US_ASCII)
        writeVarLengthCount(stream, textBytes.size.toLong())
        stream.write(textBytes)
    }

    private fun writeTrackChunk(outStream: ByteArrayOutputStream, trackData: ByteArray) {
        outStream.write("MTrk".toByteArray(StandardCharsets.US_ASCII))
        val len = trackData.size
        outStream.write(
            byteArrayOf(
                ((len shr 24) and 0xFF).toByte(),
                ((len shr 16) and 0xFF).toByte(),
                ((len shr 8) and 0xFF).toByte(),
                (len and 0xFF).toByte()
            )
        )
        outStream.write(trackData)
    }

    private fun writeVarLengthCount(stream: ByteArrayOutputStream, value: Long) {
        var buffer = value
        val bytes = java.util.Stack<Byte>()
        bytes.push((buffer and 0x7F).toByte())
        buffer = buffer ushr 7
        while (buffer > 0) {
            bytes.push(((buffer and 0x7F) or 0x80).toByte())
            buffer = buffer ushr 7
        }
        while (!bytes.isEmpty()) {
            stream.write(bytes.pop().toInt())
        }
    }

    private fun getKeySignatureValue(rootKey: String): ByteArray {
        val clean = rootKey.trim()
        val isMinor = clean.endsWith("m") || clean.endsWith("min") || clean.lowercase().contains("minor")
        val keyName = clean.substringBefore("m").substringBefore("min").substringBefore(" ").trim()

        val sf = when (keyName) {
            "C", "A" -> 0
            "G", "E" -> 1
            "D", "B" -> 2
            "A", "F#" -> 3
            "E", "C#" -> 4
            "B", "G#" -> 5
            "F#", "D#" -> 6
            "C#", "A#" -> 7
            
            "F", "D" -> -1
            "Bb", "G" -> -2
            "Eb", "C" -> -3
            "Ab", "F" -> -4
            "Db", "Bb" -> -5
            "Gb", "Eb" -> -6
            "Cb", "Ab" -> -7
            else -> 0
        }
        val mi = if (isMinor) 1 else 0
        return byteArrayOf(sf.toByte(), mi.toByte())
    }

    private fun parseChordToMidiNotes(chordSymbol: String): List<Int> {
        if (chordSymbol.isBlank() || chordSymbol.uppercase() == "NONE") return emptyList()

        val clean = chordSymbol.trim()
        var skip = 1
        if (clean.length > 1 && (clean[1] == '#' || clean[1] == 'b')) {
            skip = 2
        }
        val rootStr = clean.take(skip)
        val rootOfs = when (rootStr) {
            "C" -> 0
            "C#", "Db" -> 1
            "D" -> 2
            "D#", "Eb" -> 3
            "E" -> 4
            "F" -> 5
            "F#", "Gb" -> 6
            "G" -> 7
            "G#", "Ab" -> 8
            "A" -> 9
            "A#", "Bb" -> 10
            "B" -> 11
            else -> 0
        }
        val rootMidi = 60 + rootOfs // Piano C4 Register octaves
        val quality = clean.drop(skip).lowercase()

        val intervals = when {
            quality.contains("dim") -> listOf(0, 3, 6)
            quality.contains("aug") -> listOf(0, 4, 8)
            quality.contains("m") || quality.contains("min") -> {
                if (quality.contains("7")) listOf(0, 3, 7, 10) else listOf(0, 3, 7)
            }
            else -> {
                if (quality.contains("maj7")) listOf(0, 4, 7, 11)
                else if (quality.contains("7")) listOf(0, 4, 7, 10)
                else listOf(0, 4, 7)
            }
        }
        return intervals.map { rootMidi + it }
    }
}
