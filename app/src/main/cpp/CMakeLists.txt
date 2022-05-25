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
include_directories(${OpenCV_DIR}/jni/include)
add_library(lib_opencv SHARED IMPORTED)
set_target_properties(
        lib_opencv
        PROPERTIES IMPORTED_LOCATION
        ${OpenCV_DIR}/libs/${ANDROID_ABI}/libopencv_java4.so
)

# (dlib 라이브러리)
include_directories(${dlib_DIR}/include)
add_library(dlib STATIC IMPORTED)
set_target_properties(
        dlib
        PROPERTIES IMPORTED_LOCATION
        ${dlib_DIR}/lib/${ANDROID_ABI}/libdlib.a
)


# [JNI 라이브러리 추가]
# (jni_string_test)
# 서브 라이브러리
include_directories(${CMAKE_SOURCE_DIR}/jni_libs/jni_string_test/sub_libs/include)
add_subdirectory(${CMAKE_SOURCE_DIR}/jni_libs/jni_string_test/sub_libs)

# JNI 래퍼
add_library(
        native_wrapper_jni_string_test # 자바용 라이브러리 이름
        SHARED # 종류
        jni_libs/jni_string_test/native_wrapper_jni_string_test.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_jni_string_test
        jni_string_test_lib
        ${log-lib}
        ${android-lib}
)


# (asset_manager_test)
# 서브 라이브러리
include_directories(${CMAKE_SOURCE_DIR}/jni_libs/asset_manager_test/sub_libs/include)
add_subdirectory(${CMAKE_SOURCE_DIR}/jni_libs/asset_manager_test/sub_libs)

# JNI 래퍼
add_library(
        native_wrapper_asset_manager_test # 자바용 라이브러리 이름
        SHARED # 종류
        jni_libs/asset_manager_test/native_wrapper_asset_manager_test.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_asset_manager_test
        asset_manager_test_lib
        ${log-lib}
        ${android-lib}
)


# (ffmpeg_wrapper)
# 서브 라이브러리
include_directories(${CMAKE_SOURCE_DIR}/jni_libs/ffmpeg_wrapper/sub_libs/include)
add_subdirectory(${CMAKE_SOURCE_DIR}/jni_libs/ffmpeg_wrapper/sub_libs)

# JNI 래퍼
add_library(
        native_wrapper_ffmpeg_wrapper # 자바용 라이브러리 이름
        SHARED # 종류
        jni_libs/ffmpeg_wrapper/native_wrapper_ffmpeg_wrapper.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_ffmpeg_wrapper
        ffmpeg_wrapper_lib
        ${log-lib}
        ${android-lib}
)