// NDK
#include <jni.h>
#include <string>
#include <utility>

// Android
#include <android/log.h>
#include <android/bitmap.h>

// (서드 라이브러리)
// opencv
#include <opencv2/opencv.hpp>
#include <opencv2/core.hpp>


// LOG define 함수 모음
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "<native_wrapper_opencv_test.cpp>", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "<native_wrapper_opencv_test.cpp>", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "<native_wrapper_opencv_test.cpp>", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "<native_wrapper_opencv_test.cpp>", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "<native_wrapper_opencv_test.cpp>", __VA_ARGS__)

// [global variables]


// [classes]
struct MyException : public std::exception {
    std::string errorMessage;

    explicit MyException(std::string errorMessage) {
        this->errorMessage = std::move(errorMessage);
    }

    const char *what() const noexcept override {
        return errorMessage.c_str();
    }
};


// [functions]
// Bitmap jobject(RGBA8888) 을 bgrMatrix 로 변환
void BitmapToBGRMatrix(JNIEnv *env, jobject obj_bitmap, cv::Mat &matrix) {
    assert(obj_bitmap);

    // 중간 결과 코드
    int resultCode;

    // 비트맵 정보
    AndroidBitmapInfo bitmapInfo;
    resultCode = AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo);

    // 정보 추출 이상 체크
    if (resultCode != ANDROID_BITMAP_RESULT_SUCCESS) {
        throw MyException("get Bitmap Info failure");
    }

    // 이미지 타입 체크
    // RGBA8888 (JAVA ARGB8888 Bitmap)
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        throw MyException("only ARGB888 format support");
    }

    // 픽셀 값 추출
    void *bitmapPixels;
    resultCode = AndroidBitmap_lockPixels(env, obj_bitmap, (void **) &bitmapPixels);

    // 비트맵 추출 결과 확인
    if (resultCode != ANDROID_BITMAP_RESULT_SUCCESS) {
        throw MyException("lockPixels failure");
    }

    // 비트맵 RGBA8888 to cv::mat
    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        // 8 bit 4 channel matrix 생성
        cv::Mat tmp(bitmapInfo.width, bitmapInfo.height, CV_8UC4, bitmapPixels);
        // convert
        cv::cvtColor(tmp, matrix, cv::COLOR_RGBA2BGR);
    }

    if (bitmapPixels) {
        AndroidBitmap_unlockPixels(env, obj_bitmap);
    }
}

// Bitmap jobject(RGBA8888) 을 grayMatrix 로 변환
void BitmapToGrayMatrix(JNIEnv *env, jobject obj_bitmap, cv::Mat &matrix) {
    assert(obj_bitmap);

    // 중간 결과 코드
    int resultCode;

    // 비트맵 정보
    AndroidBitmapInfo bitmapInfo;
    resultCode = AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo);

    // 정보 추출 이상 체크
    if (resultCode != ANDROID_BITMAP_RESULT_SUCCESS) {
        throw MyException("get Bitmap Info failure");
    }

    // 이미지 타입 체크
    // RGBA8888 (JAVA ARGB8888 Bitmap)
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        throw MyException("only ARGB888 format support");
    }

    // 픽셀 값 추출
    void *bitmapPixels;
    resultCode = AndroidBitmap_lockPixels(env, obj_bitmap, (void **) &bitmapPixels);

    // 비트맵 추출 결과 확인
    if (resultCode != ANDROID_BITMAP_RESULT_SUCCESS) {
        throw MyException("lockPixels failure");
    }

    // 비트맵 RGBA8888 to cv::mat
    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        // 8 bit 4 channel matrix 생성
        cv::Mat tmp(bitmapInfo.width, bitmapInfo.height, CV_8UC4, bitmapPixels);
        // convert
        cv::cvtColor(tmp, matrix, cv::COLOR_RGBA2GRAY);
    }

    if (bitmapPixels) {
        AndroidBitmap_unlockPixels(env, obj_bitmap);
    }
}

// RGBA Matrixt 를 RGBA8888 Bitmap 으로 변환
jobject
RgbaMatrixToBitmap(JNIEnv *env, cv::Mat &src, bool needPremultiplyAlpha, jobject bitmap_config) {
    jclass java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");
    jmethodID mid = env->GetStaticMethodID(java_bitmap_class,
                                           "createBitmap",
                                           "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jobject bitmap = env->CallStaticObjectMethod(java_bitmap_class,
                                                 mid, src.size().width, src.size().height,
                                                 bitmap_config);
    AndroidBitmapInfo info;
    void *pixels = 0;

    try {
        //validate
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(src.type() == CV_8UC4);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);

        //type mat
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if (src.type() == CV_8UC4) {
                if (needPremultiplyAlpha) {
                    cvtColor(src, tmp, cv::COLOR_RGBA2mRGBA);
                } else {
                    src.copyTo(tmp);
                }
            }

        } else {
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if (src.type() == CV_8UC4) {
                cvtColor(src, tmp, cv::COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return bitmap;
    } catch (cv::Exception e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        return bitmap;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return bitmap;
    }
}


// [jni functions]
extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperOpenCvTest_getBitmapRGB(
        JNIEnv *env, jobject thiz, jobject bitmap) {
    // (bitmap to BGR matrix)
    cv::Mat imgBGRMat;
    BitmapToBGRMatrix(env, bitmap, imgBGRMat);

    // (BGR matrix split)
    std::vector<cv::Mat> temp0;
    split(imgBGRMat, temp0);
    cv::Scalar b_mean = mean(temp0[0]);
    cv::Scalar g_mean = mean(temp0[1]);
    cv::Scalar r_mean = mean(temp0[2]);
    temp0.clear();

    // (RGB Array 반환)
    int arrayListSize = 3;
    jintArray result = (*env).NewIntArray(arrayListSize);

    if (result == nullptr) {
        throw MyException("result array convert erro");
    }

    jint fill[arrayListSize];
    fill[0] = (int) r_mean.val[0];
    fill[1] = (int) g_mean.val[0];
    fill[2] = (int) b_mean.val[0];

    (*env).SetIntArrayRegion(result, 0, arrayListSize, fill);

    return result;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperOpenCvTest_getGrayBitmap(
        JNIEnv *env, jobject thiz, jobject bitmap) {
    // (bitmap to Gray matrix)
    cv::Mat imgGrayMat;
    BitmapToGrayMatrix(env, bitmap, imgGrayMat);

    cv::Mat imgRGBAMat;
    cv::cvtColor(imgGrayMat, imgRGBAMat, cv::COLOR_GRAY2RGBA);

    // (matrix to Bitmap)
    jobject bitmap_config = env->CallObjectMethod(bitmap, env->GetMethodID(
            (jclass) env->FindClass("android/graphics/Bitmap"), "getConfig",
            "()Landroid/graphics/Bitmap$Config;"));

    jobject _bitmap = RgbaMatrixToBitmap(env, imgRGBAMat, false, bitmap_config);

    AndroidBitmap_unlockPixels(env, bitmap);

    return _bitmap;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperOpenCvTest_getCopyBitmap(
        JNIEnv *env, jobject thiz, jobject bitmap) {
    // (bitmap to Gray matrix)
    cv::Mat imgBgrMat;
    BitmapToBGRMatrix(env, bitmap, imgBgrMat);

    cv::Mat imgRGBAMat;
    cv::cvtColor(imgBgrMat, imgRGBAMat, cv::COLOR_BGR2RGBA);

    // (matrix to Bitmap)
    jobject bitmap_config = env->CallObjectMethod(bitmap, env->GetMethodID(
            (jclass) env->FindClass("android/graphics/Bitmap"), "getConfig",
            "()Landroid/graphics/Bitmap$Config;"));

    jobject _bitmap = RgbaMatrixToBitmap(env, imgRGBAMat, false, bitmap_config);

    AndroidBitmap_unlockPixels(env, bitmap);

    return _bitmap;
}