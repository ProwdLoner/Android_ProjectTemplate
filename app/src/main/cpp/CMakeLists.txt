# cmake 최소 버전
cmake_minimum_required(VERSION 3.18.1)

# 프로젝트명
project("native_wrapper")


# [NDK 라이브러리 추가]
# 로그 라이브러리
find_library(
        log-lib
        log
)

# 안드로이드 라이브러리
find_library(android-lib
        android)


# [서드 라이브러리 추가]
# (opencv 라이브러리)
include_directories(${CMAKE_SOURCE_DIR}/third_libs/opencv_4_5_5/native/jni/include)
add_library(lib_opencv SHARED IMPORTED)
set_target_properties(
        lib_opencv
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/third_libs/opencv_4_5_5/native/libs/${ANDROID_ABI}/libopencv_java4.so
)

# (dlib 라이브러리)
include_directories(${CMAKE_SOURCE_DIR}/third_libs/dlib_19_23/include)
add_library(dlib STATIC IMPORTED)
set_target_properties(
        dlib
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/third_libs/dlib_19_23/lib/${ANDROID_ABI}/libdlib.a
)

# (ffmpeg 라이브러리)
include_directories(${CMAKE_SOURCE_DIR}/third_libs/ffmpeg_3_3_2/include)

add_library(lib_avcodec SHARED IMPORTED)
set_target_properties(lib_avcodec PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/third_libs/ffmpeg_3_3_2/lib/${ANDROID_ABI}/libavcodec-57.so)

add_library(lib_avdevice SHARED IMPORTED)
set_target_properties(lib_avdevice PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/third_libs/ffmpeg_3_3_2/lib/${ANDROID_ABI}/libavdevice-57.so)

add_library(lib_avfilter SHARED IMPORTED)
set_target_properties(lib_avfilter PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/third_libs/ffmpeg_3_3_2/lib/${ANDROID_ABI}/libavfilter-6.so)

add_library(lib_avformat SHARED IMPORTED)
set_target_properties(lib_avformat PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/third_libs/ffmpeg_3_3_2/lib/${ANDROID_ABI}/libavformat-57.so)

add_library(lib_avutil SHARED IMPORTED)
set_target_properties(lib_avutil PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/third_libs/ffmpeg_3_3_2/lib/${ANDROID_ABI}/libavutil-55.so)

add_library(lib_postproc SHARED IMPORTED)
set_target_properties(lib_postproc PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/third_libs/ffmpeg_3_3_2/lib/${ANDROID_ABI}/libpostproc-54.so)

add_library(lib_swresample SHARED IMPORTED)
set_target_properties(lib_swresample PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/third_libs/ffmpeg_3_3_2/lib/${ANDROID_ABI}/libswresample-2.so)

add_library(lib_swscale SHARED IMPORTED)
set_target_properties(lib_swscale PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/third_libs/ffmpeg_3_3_2/lib/${ANDROID_ABI}/libswscale-4.so)


# [JNI 라이브러리 추가]
# (jni_string_test)
# JNI 래퍼
add_library(
        native_wrapper_jni_string_test # 자바용 라이브러리 이름
        SHARED # 종류
        jni_libs/jni_string_test/native_wrapper_jni_string_test.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_jni_string_test
        ${log-lib}
        ${android-lib}
)


# (asset_manager_test)
# JNI 래퍼
add_library(
        native_wrapper_asset_manager_test # 자바용 라이브러리 이름
        SHARED # 종류
        jni_libs/asset_manager_test/native_wrapper_asset_manager_test.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_asset_manager_test
        ${log-lib}
        ${android-lib}
)


# (ffmpeg_wrapper)
# JNI 래퍼
add_library(
        native_wrapper_ffmpeg_wrapper # 자바용 라이브러리 이름
        SHARED # 종류
        jni_libs/ffmpeg_wrapper/native_wrapper_ffmpeg_wrapper.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_ffmpeg_wrapper
        ${log-lib}
        ${android-lib}

        lib_avcodec
        lib_avdevice
        lib_avfilter
        lib_avformat
        lib_avutil
        lib_postproc
        lib_swresample
        lib_swscale
)