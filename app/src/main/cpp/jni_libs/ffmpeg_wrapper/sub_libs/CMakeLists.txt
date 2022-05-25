# cmake 최소 버전
cmake_minimum_required(VERSION 3.18.1)

# [서드 라이브러리 추가]
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

# [유저 라이브러리 추가]
# test1
add_library(
        ffmpeg_wrapper_lib # 자바용 라이브러리 이름
        SHARED # 종류
        src/ffmpeg_wrapper_lib.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        ffmpeg_wrapper_lib

        lib_avcodec
        lib_avdevice
        lib_avfilter
        lib_avformat
        lib_avutil
        lib_postproc
        lib_swresample
        lib_swscale
)