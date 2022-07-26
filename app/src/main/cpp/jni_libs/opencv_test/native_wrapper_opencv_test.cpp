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

#define ASSERT(status, ret)     if (!(status)) { return ret; }
#define ASSERT_FALSE(status)    ASSERT(status, false)

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
// todo rgb565 비트맵 동작 확인
// Bitmap jobject(RGBA_8888 or RGB_565) 을 rgbMatrix 로 변환
bool jBitmapToRgbCvMatrix(JNIEnv *env, jobject obj_bitmap, cv::Mat &matrix) {
    void *bitmapPixels;
    AndroidBitmapInfo bitmapInfo;

    ASSERT_FALSE(AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo) >= 0)
    ASSERT_FALSE(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888
                 || bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565)
    ASSERT_FALSE(AndroidBitmap_lockPixels(env, obj_bitmap, &bitmapPixels) >= 0)
    ASSERT_FALSE(bitmapPixels)

    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
        cv::cvtColor(tmp, matrix, cv::COLOR_RGBA2RGB);
        tmp.release();
    } else if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        cv::cvtColor(tmp, matrix, cv::COLOR_BGR5652RGB);
        tmp.release();
    }

    AndroidBitmap_unlockPixels(env, obj_bitmap);
    return true;
}

// todo rgb565 비트맵 동작 확인
// Bitmap jobject(RGBA_8888 or RGB_565) 을 grayMatrix 로 변환
bool jBitmapToGrayCvMatrix(JNIEnv *env, jobject obj_bitmap, cv::Mat &matrix) {
    void *bitmapPixels;
    AndroidBitmapInfo bitmapInfo;

    ASSERT_FALSE(AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo) >= 0)
    ASSERT_FALSE(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888
                 || bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565)
    ASSERT_FALSE(AndroidBitmap_lockPixels(env, obj_bitmap, &bitmapPixels) >= 0)
    ASSERT_FALSE(bitmapPixels)

    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
        cv::cvtColor(tmp, matrix, cv::COLOR_RGBA2GRAY);
        tmp.release();
    } else if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        cv::cvtColor(tmp, matrix, cv::COLOR_BGR5652GRAY);
        tmp.release();
    }

    AndroidBitmap_unlockPixels(env, obj_bitmap);
    return true;
}

bool matrixToJbitmap(JNIEnv *env, cv::Mat &matrix, jobject obj_bitmap) {
    void *bitmapPixels;                                            // Save picture pixel data
    AndroidBitmapInfo bitmapInfo;                                   // Save picture parameters

    ASSERT_FALSE(AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo) >=
                 0);        // Get picture parameters
    ASSERT_FALSE(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888
                 || bitmapInfo.format ==
                    ANDROID_BITMAP_FORMAT_RGB_565);          // Only ARGB? 8888 and RGB? 565 are supported
    ASSERT_FALSE(matrix.dims == 2
                 && bitmapInfo.height == (uint32_t) matrix.rows
                 && bitmapInfo.width ==
                    (uint32_t) matrix.cols);                   // It must be a 2-dimensional matrix with the same length and width
    ASSERT_FALSE(matrix.type() == CV_8UC1 || matrix.type() == CV_8UC3 || matrix.type() == CV_8UC4);
    ASSERT_FALSE(AndroidBitmap_lockPixels(env, obj_bitmap, &bitmapPixels) >=
                 0);  // Get picture pixels (lock memory block)
    ASSERT_FALSE(bitmapPixels);

    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
        switch (matrix.type()) {
            case CV_8UC1:
                cv::cvtColor(matrix, tmp, cv::COLOR_GRAY2RGBA);
                break;
            case CV_8UC3:
                cv::cvtColor(matrix, tmp, cv::COLOR_RGB2RGBA);
                break;
            case CV_8UC4:
                matrix.copyTo(tmp);
                break;
            default:
                AndroidBitmap_unlockPixels(env, obj_bitmap);
                return false;
        }
        tmp.release();
    } else {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        switch (matrix.type()) {
            case CV_8UC1:
                cv::cvtColor(matrix, tmp, cv::COLOR_GRAY2BGR565);
                break;
            case CV_8UC3:
                cv::cvtColor(matrix, tmp, cv::COLOR_RGB2BGR565);
                break;
            case CV_8UC4:
                cv::cvtColor(matrix, tmp, cv::COLOR_RGBA2BGR565);
                break;
            default:
                AndroidBitmap_unlockPixels(env, obj_bitmap);
                return false;
        }
        tmp.release();
    }
    AndroidBitmap_unlockPixels(env, obj_bitmap);                // Unlock
    return true;
}


// [jni functions]
extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperOpenCvTest_getBitmapRGB(
        JNIEnv *env, jobject thiz, jobject bitmap) {
    // (jBitmap to RGB matrix)
    cv::Mat imgRgbMat;
    jBitmapToRgbCvMatrix(env, bitmap, imgRgbMat);

    // (RGB matrix split)
    std::vector<cv::Mat> temp0;
    split(imgRgbMat, temp0);
    imgRgbMat.release();

    cv::Scalar r_mean = mean(temp0[0]);
    cv::Scalar g_mean = mean(temp0[1]);
    cv::Scalar b_mean = mean(temp0[2]);
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
JNIEXPORT void JNICALL
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperOpenCvTest_getGrayBitmap(
        JNIEnv *env, jobject thiz, jobject inputBitmap, jobject outputBitmap) {
    // (jBitmap to Gray matrix)
    cv::Mat imgGrayMat;
    jBitmapToGrayCvMatrix(env, inputBitmap, imgGrayMat);

    // (matrix to Bitmap)
    matrixToJbitmap(env, imgGrayMat, outputBitmap);
    imgGrayMat.release();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_prowd_1android_1template_native_1wrapper_NativeWrapperOpenCvTest_getCopyBitmap(
        JNIEnv *env, jobject thiz, jobject inputBitmap, jobject outputBitmap) {
    // (jBitmap to RGB matrix)
    cv::Mat imgRgbMat;
    jBitmapToRgbCvMatrix(env, inputBitmap, imgRgbMat);

    // (matrix to Bitmap)
    matrixToJbitmap(env, imgRgbMat, outputBitmap);
    imgRgbMat.release();
}