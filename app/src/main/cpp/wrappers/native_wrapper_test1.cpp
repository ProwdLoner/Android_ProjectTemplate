// NDK
#include <jni.h>
#include <string>
#include <utility>

// Android
#include <android/log.h>
#include <android/bitmap.h>
#include <android/asset_manager_jni.h>

// (서드 라이브러리)
#include "test1_lib.h"

// LOG define 함수 모음
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "<native_wrapper_test1.cpp>", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "<native_wrapper_test1.cpp>", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "<native_wrapper_test1.cpp>", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "<native_wrapper_test1.cpp>", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "<native_wrapper_test1.cpp>", __VA_ARGS__)

// [global variables]


// [classes]


// [functions]
extern "C"
JNIEXPORT jstring
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperTest1_stringFromJNI(
        JNIEnv *env, jobject thiz) {
    LOGI("stringFromJNI_start");

    // test1 서드 라이브러리 테스트
    int testInt = getLibraryInt();

    std::string hello =
            "Hello from test1\nand\n" + std::to_string(testInt) + " From test1_third_lib";

    LOGI("stringFromJNI_end");
    return env->NewStringUTF(hello.c_str());
}