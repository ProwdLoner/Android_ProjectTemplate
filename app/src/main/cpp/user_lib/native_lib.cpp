// basic
#include <jni.h>
#include <string>
#include <utility>

// android
#include <android/log.h>
#include <android/bitmap.h>
#include <android/asset_manager_jni.h>

// LOG 함수 모음
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "<native_lib.cpp>", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "<native_lib.cpp>", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "<native_lib.cpp>", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "<native_lib.cpp>", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "<native_lib.cpp>", __VA_ARGS__)

// [global variables]


// [classes]


// [functions]
extern "C"
JNIEXPORT jstring Java_com_example_prowd_1android_1template_native_1wrapper_NativeLib_stringFromJNI(
        JNIEnv *env, jobject thiz) {
    LOGI("stringFromJNI_start");

    std::string hello = "Hello from C++";

    LOGI("stringFromJNI_end");
    return env->NewStringUTF(hello.c_str());
}