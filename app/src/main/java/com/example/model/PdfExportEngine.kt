package com.example.model

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

/**
 * Professional programmatic PDF rendering engine for HZ CHORD AI.
 * Uses native Android Graphics and PdfDocument canvas frameworks to build clean, printable multi-page sheets
 * featuring scaled timeline structures, Roman numeral scale degrees, and prominent centered developer attributions.
 */
class PdfExportEngine {

    companion object {
        const val DEVELOPER_FOOTER = "HZ CHORD AI - Designed and Built by Joseph Hilary Zulukwa"
    }

    /**
     * Programmatically generates and structures a multi-page PDF chord sheet from chronological timeline values.
     */
    fun genChordSheetPdf(
        sessionLabel: String,
        rootKey: String,
        tempoBpm: Int,
        chords: List<DetectedChord>
    ): ByteArray {
        val pdfDocument = PdfDocument()
        val pageWidth = 595  // A4 Standard Width in points
        val pageHeight = 842 // A4 Standard Height in points

        // Setup paints with specific styles and densities
        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 21f
            isFakeBoldText = true
        }

        val metaLeftPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 10f
            isFakeBoldText = false
        }

        val metaRightPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }

        val partitionPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 1.5f
        }

        val tableHeaderPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }

        val gridLinePaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 0.5f
        }

        val itemTextPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }

        val footerTextPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 9f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val rowHeight = 22f
        val itemsPerPage = 28
        val totalPages = if (chords.isEmpty()) 1 else (chords.size + itemsPerPage - 1) / itemsPerPage

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Page Header Top Margin Buffer
            var yPos = 60f

            // App branding and title
            canvas.drawText("HZ CHORD AI - HARMONIC SHEET REPORT", 50f, yPos, titlePaint)
            yPos += 22f

            // Metadata row mapping
            canvas.drawText("Session: $sessionLabel  |  Implied Key: $rootKey", 50f, yPos, metaLeftPaint)
            canvas.drawText("Tempo: $tempoBpm BPM  |  Offline Inference Report", (pageWidth - 50).toFloat(), yPos, metaRightPaint)
            yPos += 14f

            canvas.drawLine(50f, yPos, (pageWidth - 50).toFloat(), yPos, partitionPaint)
            yPos += 25f

            // Data column coordinates
            val colX1 = 50f   // Chronological Timestamp
            val colX2 = 180f  // Chord notation
            val colX3 = 300f  // Scale degree roman recognition
            val colX4 = 440f  // Confidence score

            // Drawing column indicators
            canvas.drawText("Timeline Offset", colX1, yPos, tableHeaderPaint)
            canvas.drawText("Detected Chord", colX2, yPos, tableHeaderPaint)
            canvas.drawText("Scale Degree", colX3, yPos, tableHeaderPaint)
            canvas.drawText("Confidence Index", colX4, yPos, tableHeaderPaint)
            yPos += 10f

            canvas.drawLine(50f, yPos, (pageWidth - 50).toFloat(), yPos, gridLinePaint)
            yPos += 18f

            // Draw chronological records
            val startIdx = pageIndex * itemsPerPage
            val endIdx = minOf(startIdx + itemsPerPage, chords.size)

            for (i in startIdx until endIdx) {
                val chord = chords[i]
                val seconds = chord.timestampMs / 1000f
                val timestampStr = String.format("%.2f s", seconds)
                val degree = MusicTheoryUtils.convertToRomanNumeral(chord.chordSymbol, rootKey)
                val confidencePercentage = String.format("%.1f %%", chord.confidence * 100)

                canvas.drawText(timestampStr, colX1, yPos, itemTextPaint)
                canvas.drawText(chord.chordSymbol, colX2, yPos, itemTextPaint)
                canvas.drawText(degree, colX3, yPos, itemTextPaint)
                canvas.drawText(confidencePercentage, colX4, yPos, itemTextPaint)

                yPos += 8f
                canvas.drawLine(50f, yPos, (pageWidth - 50).toFloat(), yPos, gridLinePaint)
                yPos += 14f
            }

            // Draw prominent footer at the center bottom of every page
            val footerY = pageHeight - 45f
            canvas.drawText(DEVELOPER_FOOTER, (pageWidth / 2).toFloat(), footerY, footerTextPaint)

            // Draw numeric index reference
            val numbersPaint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 8.5f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Page ${pageIndex + 1} of $totalPages", (pageWidth - 50).toFloat(), footerY, numbersPaint)

            pdfDocument.finishPage(page)
        }

        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        return outputStream.toByteArray()
    }
}
