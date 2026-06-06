package com.example.model

import kotlin.math.absoluteValue
import kotlin.math.log2

/**
 * Universal Offline-First Music Theory Engine.
 * Handles key transposition and converts chord symbols into functional Roman Numeral analysis,
 * suitable for the HZ CHORD AI production suite.
 */
object MusicTheoryUtils {

    val sharps = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val flats = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    private val CHROMATIC_SIZE = 12

    /**
     * Converts a raw frequency in Hz into its closest scientific note name with octave decoration.
     * e.g., 440f -> "A4"
     */
    fun getNoteNameWithOctave(frequencyHz: Float): String {
        if (frequencyHz <= 0f) return "N/A"
        val midiNote = kotlin.math.round(12 * log2(frequencyHz.toDouble() / 440.0) + 69.0).toInt()
        val index = (midiNote % 12).let { if (it < 0) it + 12 else it }
        val octave = (midiNote / 12) - 1
        val noteName = sharps[index]
        return "$noteName$octave"
    }

    /**
     * Splits a chord symbol like "F#maj7/A#" into its Root Note ("F#") and Quality/Extension ("maj7/A#").
     */
    fun parseChord(chordName: String): Pair<String, String>? {
        if (chordName.isEmpty()) return null
        val trimmed = chordName.trim()
        
        // Handle chords with flat or sharp root note
        if (trimmed.length >= 2) {
            val secondChar = trimmed[1]
            if (secondChar == '#' || secondChar == 'b') {
                return Pair(trimmed.substring(0, 2), trimmed.substring(2))
            }
        }
        return Pair(trimmed.substring(0, 1), trimmed.substring(1))
    }

    /**
     * Transposes a chord by a specific number of semitones.
     * e.g. transposeChord("Cmaj7", 2) -> "Dmaj7"
     */
    fun transposeChord(chordName: String, semitones: Int): String {
        // Support slash chords like C/G by transposing both parts
        if (chordName.contains("/")) {
            val parts = chordName.split("/")
            if (parts.size == 2) {
                val baseChord = transposeChord(parts[0], semitones)
                val bassNote = transposeNote(parts[1], semitones)
                return "$baseChord/$bassNote"
            }
        }

        val parsed = parseChord(chordName) ?: return chordName
        val (root, quality) = parsed
        val transposedRoot = transposeNote(root, semitones)
        return "$transposedRoot$quality"
    }

    /**
     * Helper to transpose a single note by semitones.
     */
    fun transposeNote(note: String, semitones: Int): String {
        val noteTrim = note.trim()
        val isFlat = noteTrim.contains("b")
        val scale = if (isFlat) flats else sharps
        
        var index = scale.indexOf(noteTrim)
        if (index == -1) {
            // Find in alternative scale
            val altScale = if (isFlat) sharps else flats
            index = altScale.indexOf(noteTrim)
            if (index == -1) return note // fallback if unrecognized Note
        }

        val newIndex = (index + semitones).mod(CHROMATIC_SIZE)
        // Prefer original scale representation if possible
        return if (isFlat) flats[newIndex] else sharps[newIndex]
    }

    /**
     * Converts a chord symbol into its Roman Numeral functional step (e.g., "Am" in key "C" -> "vi").
     * Supports both major and minor keys.
     */
    fun convertToRomanNumeral(chordName: String, keyOf: String): String {
        val cleanKey = keyOf.trim()
        val isMinorKey = cleanKey.endsWith("m") || cleanKey.endsWith("min")
        val keyRoot = if (isMinorKey) {
            cleanKey.replace(Regex("(m|min)$"), "")
        } else {
            cleanKey
        }

        val parsedChord = parseChord(chordName) ?: return chordName
        val (chordRoot, quality) = parsedChord

        // Determine semitone interval index from scale key root
        val rootIndex = sharps.indexOf(keyRoot).let {
            if (it == -1) flats.indexOf(keyRoot) else it
        }
        val chordIndex = sharps.indexOf(chordRoot).let {
            if (it == -1) flats.indexOf(chordRoot) else it
        }

        if (rootIndex == -1 || chordIndex == -1) return chordName // theory fallback

        val interval = (chordIndex - rootIndex).mod(CHROMATIC_SIZE)
        
        // Define Roman numeral descriptors of the diatonic steps (Major vs Minor keys)
        val isMinorChord = quality.startsWith("m") || 
                           quality.startsWith("min") || 
                           quality.startsWith("dim") ||
                           quality.startsWith("o")

        val romanDegrees = if (isMinorKey) {
            // Minor Key Scale degrees (Natural Minor: i, ii°, bIII, iv, v, bVI, bVII)
            mapOf(
                0 to Pair("I", "i"),
                1 to Pair("bII", "bii"),
                2 to Pair("II", "ii"),
                3 to Pair("bIII", "biii"),
                4 to Pair("III", "iii"),
                5 to Pair("IV", "iv"),
                6 to Pair("#IV", "#iv"),
                7 to Pair("V", "v"),
                8 to Pair("bVI", "bvi"),
                9 to Pair("VI", "vi"),
                10 to Pair("bVII", "bvii"),
                11 to Pair("VII", "vii°")
            )
        } else {
            // Major Key Scale degrees (I, ii, iii, IV, V, vi, vii°)
            mapOf(
                0 to Pair("I", "i"),
                1 to Pair("bII", "bii"),
                2 to Pair("II", "ii"),
                3 to Pair("bIII", "biii"),
                4 to Pair("III", "iii"),
                5 to Pair("IV", "iv"),
                6 to Pair("#IV", "#iv"),
                7 to Pair("V", "v"),
                8 to Pair("bVI", "bvi"),
                9 to Pair("VI", "vi"),
                10 to Pair("bVII", "bvii"),
                11 to Pair("VII", "vii°")
            )
        }

        val romanPair = romanDegrees[interval] ?: return chordName
        var numeral = if (isMinorChord) romanPair.second else romanPair.first

        // Append custom chord modifiers for rich notation
        if (quality.contains("7") && !numeral.endsWith("7")) {
            numeral += "7"
        } else if (quality.contains("maj7") && !numeral.contains("maj")) {
            numeral += "maj7"
        } else if (quality.contains("dim") || quality.contains("o")) {
            if (!numeral.contains("°") && !numeral.contains("dim")) {
                numeral += "°"
            }
        } else if (quality.contains("sus")) {
            numeral += quality.substring(quality.indexOf("sus"))
        }

        return numeral
    }
}
