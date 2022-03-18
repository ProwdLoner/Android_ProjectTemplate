#include <jni.h>
#include <string>
#include <utility>

// Android
#include <android/log.h>
//#include <android/asset_manager_jni.h>
//#include <android/bitmap.h>

// (서드 라이브러리)
#include "asset_manager_test_lib.h"

// LOG define 함수 모음
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)

// [global variables]


// [classes]


// [functions]
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperAssetManagerTest_getAssetTextString(
        JNIEnv *env, jobject thiz,
        jobject asset_manager,
        jstring file_name) {
    LOGI("getAssetTextString_start");

    // 서드 라이브러리 요청
    std::string testInt = getAssetTextString();

    LOGI("getAssetTextString_end");
    return env->NewStringUTF(testInt.c_str());
}