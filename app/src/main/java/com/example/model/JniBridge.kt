package com.example.model

import android.util.Log

/**
 * JNI Bridge interfacing with the low-latency native Oboe / C++ KissFFT audio engine.
 * Includes a graceful pure-Kotlin fallback block if the JNI native library is not present or loaded.
 */
object JniBridge {
    private const val TAG = "JniBridge"
    private var isNativeLibraryLoaded = false

    init {
        try {
            System.loadLibrary("hzchordai_dsp")
            isNativeLibraryLoaded = true
            Log.i(TAG, "Native HZ Chord DSP dynamic library loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library 'hzchordai_dsp' not found. Cascading to optimized JVM DSP Engine.")
            isNativeLibraryLoaded = false
        }
    }

    /**
     * Checks if the low-latency native JNI audio engine is active.
     */
    fun isNativeActive(): Boolean = isNativeLibraryLoaded

    /**
     * Starts the native Oboe audio capturing and DSP pipelines.
     * @param sampleRate Target audio capturing rate (e.g., 22050 Hz)
     * @param windowSize Processing FFT slice size (e.g., 8192)
     */
    external fun startNativeEngine(sampleRate: Int, windowSize: Int): Boolean

    /**
     * Stops the Oboe recording stream and cleans up native resources.
     */
    external fun stopNativeEngine()

    /**
     * Returns the live 12-dimensional chroma vector (Chromagram) computed on the native thread.
     */
    external fun getNativeChromagram(): FloatArray
}
