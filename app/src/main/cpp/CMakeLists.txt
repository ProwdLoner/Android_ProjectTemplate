# cmake 최소 버전
cmake_minimum_required(VERSION 3.18.1)

# 프로젝트명
project("prowd_android_template_native_wrapper")

# (NDK 라이브러리 탐색)
# 로그 라이브러리
find_library(
        log-lib
        log
)

# 안드로이드 라이브러리
find_library(android-lib
        android)

# (서드 라이브러리 추가)
# jni_string_test_lib
include_directories(${CMAKE_SOURCE_DIR}/libs/jni_string_test_lib/include)
add_subdirectory(${CMAKE_SOURCE_DIR}/libs/jni_string_test_lib)

# asset_manager_test_lib
include_directories(${CMAKE_SOURCE_DIR}/libs/asset_manager_test_lib/include)
add_subdirectory(${CMAKE_SOURCE_DIR}/libs/asset_manager_test_lib)

# (래퍼 라이브러리 추가)
# native_wrapper_test1.cpp
add_library(
        native_wrapper_jni_string_test # 자바용 라이브러리 이름
        SHARED # 종류
        wrappers/native_wrapper_jni_string_test.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_jni_string_test
        jni_string_test_lib
        ${log-lib}
        ${android-lib}
)

# native_wrapper_asset_manager_test.cpp
add_library(
        native_wrapper_asset_manager_test # 자바용 라이브러리 이름
        SHARED # 종류
        wrappers/native_wrapper_asset_manager_test.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_asset_manager_test
        asset_manager_test_lib
        ${log-lib}
        ${android-lib}
)