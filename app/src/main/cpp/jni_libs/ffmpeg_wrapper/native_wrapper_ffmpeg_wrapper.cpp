#include <jni.h>
#include <string>
#include <utility>

// Android
#include <android/log.h>
#include <android/asset_manager_jni.h>
//#include <android/bitmap.h>

// (서드 라이브러리)

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfilter.h>
}

// LOG define 함수 모음
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "<native_wrapper_ffmpeg_wrapper.cpp>", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "<native_wrapper_ffmpeg_wrapper.cpp>", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "<native_wrapper_ffmpeg_wrapper.cpp>", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "<native_wrapper_ffmpeg_wrapper.cpp>", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "<native_wrapper_ffmpeg_wrapper.cpp>", __VA_ARGS__)

// [global variables]


// [classes]


// [functions]
extern "C"
JNIEXPORT void JNICALL
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperFFMpegWrapper_libTest(
        JNIEnv *env, jobject thiz) {
    LOGE("ffmpeg_test");

    av_register_all();
}