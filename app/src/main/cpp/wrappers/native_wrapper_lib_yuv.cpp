#include <jni.h>
#include <string>
#include <utility>

// Android
#include <android/log.h>
#include <android/asset_manager_jni.h>
//#include <android/bitmap.h>

// (서드 라이브러리)
#include <libyuv.h>

// LOG define 함수 모음
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "<native_wrapper_lib_yuv.cpp>", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "<native_wrapper_lib_yuv.cpp>", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "<native_wrapper_lib_yuv.cpp>", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "<native_wrapper_lib_yuv.cpp>", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "<native_wrapper_lib_yuv.cpp>", __VA_ARGS__)

// [global variables]


// [classes]


// [functions]