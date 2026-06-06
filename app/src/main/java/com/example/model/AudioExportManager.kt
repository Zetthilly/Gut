package com.example.model

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Robust export management engine for multi-stem acoustic components.
 * Operates offline to copy, modify volume gain levels, mute, and package individual separated stems
 * directly to public system destinations using modern Android MediaStore and storage frameworks.
 */
class AudioExportManager(private val context: Context) {

    /**
     * Modifies the raw PCM samples of a WAV file to apply exact workstation mixer gains (volume / mute).
     */
    fun processWavWithGain(inputFile: File, outputFile: File, volume: Float, isMuted: Boolean) {
        if (!inputFile.exists()) return
        
        try {
            inputFile.inputStream().use { input ->
                outputFile.outputStream().use { output ->
                    val header = ByteArray(44)
                    val readHeader = input.read(header)
                    if (readHeader < 44) {
                        // Not a standard formatted WAV or truncated, copy direct as fallback
                        output.write(header, 0, readHeader)
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                        return
                    }
                    
                    // Write header directly
                    output.write(header)

                    // Process 16-bit PCM samples with corresponding mixing board levels
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    val factor = if (isMuted) 0.0f else volume
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        for (i in 0 until bytesRead step 2) {
                            if (i + 1 < bytesRead) {
                                // Extract signed little-endian short from consecutive bytes
                                val sample = ((buffer[i + 1].toInt() and 0xFF) shl 8) or (buffer[i].toInt() and 0xFF)
                                val shortVal = sample.toShort().toFloat()
                                val tweaked = (shortVal * factor).coerceIn(-32768f, 32767f).toInt().toShort()
                                
                                buffer[i] = (tweaked.toInt() and 0xFF).toByte()
                                buffer[i + 1] = ((tweaked.toInt() shr 8) and 0xFF).toByte()
                            }
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
        } catch (e: Exception) {
            // Unchecked errors fallback: direct replica copy
            try {
                inputFile.copyTo(outputFile, overwrite = true)
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Writes processed binary storage elements directly to the public Downloads folder.
     */
    fun writeToPublicDownloads(fileName: String, mimeType: String, dataWriter: (OutputStream) -> Unit): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HzChordAi_Exports")
            }
        }
        
        var uri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        }
        
        // Secondary fallback sequence for legacy or restricted environments
        if (uri == null) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destDir = File(downloadsDir, "HzChordAi_Exports_Legacy")
                if (!destDir.exists()) destDir.mkdirs()
                val file = File(destDir, fileName)
                FileOutputStream(file).use { fos ->
                    dataWriter(fos)
                }
                return Uri.fromFile(file)
            } catch (e: Exception) {
                return null
            }
        }

        try {
            resolver.openOutputStream(uri)?.use { os ->
                dataWriter(os)
            }
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }
    }

    /**
     * Exports a single specific audio stem directly to public Downloads, applying gain and mute modifications.
     */
    fun exportSingleStem(stem: AudioStem): Uri? {
        val srcFile = File(stem.filePath)
        if (!srcFile.exists()) return null

        val fileName = "HZ_Stem_${stem.name.replace(" ", "_")}.wav"
        return writeToPublicDownloads(fileName, "audio/wav") { outputStream ->
            val tempProcessedFile = File(context.cacheDir, "temp_single_${stem.id}.wav")
            processWavWithGain(srcFile, tempProcessedFile, stem.volume, stem.isMuted)
            if (tempProcessedFile.exists()) {
                tempProcessedFile.inputStream().use { input ->
                    input.copyTo(outputStream)
                }
                tempProcessedFile.delete()
            } else {
                srcFile.inputStream().use { input ->
                    input.copyTo(outputStream)
                }
            }
        }
    }

    /**
     * Compiles all 12 individual stems into a single, compact ZIP archive containing mixed WAV files.
     */
    fun exportAllStemsAsZip(stems: List<AudioStem>, zipFileName: String = "HZ_Chord_AI_12Stems.zip"): Uri? {
        return writeToPublicDownloads(zipFileName, "application/zip") { outputStream ->
            ZipOutputStream(outputStream).use { zipOut ->
                for (stem in stems) {
                    val file = File(stem.filePath)
                    if (file.exists()) {
                        val tempProcessedFile = File(context.cacheDir, "temp_zip_${stem.id}.wav")
                        processWavWithGain(file, tempProcessedFile, stem.volume, stem.isMuted)
                        
                        val entryName = "stems/${stem.name.replace(" ", "_")}.wav"
                        zipOut.putNextEntry(ZipEntry(entryName))
                        if (tempProcessedFile.exists()) {
                            tempProcessedFile.inputStream().use { input ->
                                input.copyTo(zipOut)
                            }
                            tempProcessedFile.delete()
                        } else {
                            file.inputStream().use { input ->
                                input.copyTo(zipOut)
                            }
                        }
                        zipOut.closeEntry()
                    }
                }
            }
        }
    }
}
