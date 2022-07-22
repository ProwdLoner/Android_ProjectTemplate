#include <jni.h>
#include <string>
#include <utility>

// Android
#include <android/log.h>
#include <android/asset_manager_jni.h>
//#include <android/bitmap.h>

// (서드 라이브러리)

// LOG define 함수 모음
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "<native_wrapper_asset_manager_test.cpp>", __VA_ARGS__)

// [global variables]


// [classes]


// [functions]


// [jni functions]
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperAssetManagerTest_getAssetTextString(
        JNIEnv *env, jobject thiz,
        jobject asset_manager,
        jstring file_name) {
    LOGI("getAssetTextString_start");

    // 파일명 파싱
    const char *asset_file_name = env->GetStringUTFChars(file_name, nullptr);
    env->ReleaseStringUTFChars(file_name, asset_file_name);

    // aaset 파일 접근
    AAssetManager *native_asset = AAssetManager_fromJava(env, asset_manager);
    AAsset *assetFile = AAssetManager_open(native_asset, asset_file_name, AASSET_MODE_BUFFER);

    // 파일 데이터 길이
    auto file_length = static_cast<size_t>(AAsset_getLength(assetFile));

    // 파일 데이터 to Char 변수 생성 (malloc 사용 -> 해소 필요)
    char *file_data = (char *) malloc(file_length);

    // 파일 데이터 가져오기
    AAsset_read(assetFile, file_data, file_length);

    // 객체 해소
    AAsset_close(assetFile);

    // char to string
    std::string asset_text(file_data);

    //free malloc
    free(file_data);

    LOGI("getAssetTextString_end");
    return env->NewStringUTF(asset_text.c_str());
}