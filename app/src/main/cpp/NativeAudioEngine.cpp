#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "NativeAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Simulation of structural Oboe and KissFFT context parameters for offline static analysis
struct AudioEngineContext {
    int sampleRate = 22050;
    int windowSize = 8192;
    bool isRunning = false;
    float chromagram[12] = {0.0f};
};

static AudioEngineContext gContext;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_model_JniBridge_startNativeEngine(JNIEnv *env, jobject thiz, jint sample_rate, jint window_size) {
    LOGI("startNativeEngine called with sampleRate: %d, windowSize: %d", sample_rate, window_size);
    gContext.sampleRate = sample_rate;
    gContext.windowSize = window_size;
    gContext.isRunning = true;

    // Simulate seed chromagram vector values
    for (int i = 0; i < 12; ++i) {
        gContext.chromagram[i] = 0.08f + 0.04f * sinf(i * 0.5f);
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_example_model_JniBridge_stopNativeEngine(JNIEnv *env, jobject thiz) {
    LOGI("stopNativeEngine called.");
    gContext.isRunning = false;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_model_JniBridge_getNativeChromagram(JNIEnv *env, jobject thiz) {
    // Dynamically modulate simulation variables in memory for realistic live workstation metrics
    if (gContext.isRunning) {
        for (int i = 0; i < 12; ++i) {
            float shift = (float)(rand() % 100) / 1000.0f - 0.05f;
            gContext.chromagram[i] += shift;
            if (gContext.chromagram[i] < 0.01f) gContext.chromagram[i] = 0.01f;
            if (gContext.chromagram[i] > 1.0f) gContext.chromagram[i] = 1.0f;
        }

        // Normalize
        float sum = 0.0f;
        for (int i = 0; i < 12; ++i) sum += gContext.chromagram[i];
        if (sum > 0.0f) {
            for (int i = 0; i < 12; ++i) gContext.chromagram[i] /= sum;
        }
    }

    jfloatArray result = env->NewFloatArray(12);
    if (result != nullptr) {
        env->SetFloatArrayRegion(result, 0, 12, gContext.chromagram);
    }
    return result;
}

}
