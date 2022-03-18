# cmake 최소 버전
cmake_minimum_required(VERSION 3.18.1)

# 프로젝트명
project("prowd_android_template_native_wrapper")

# (기본 라이브러리 탐색)
# 로그 라이브러리
find_library(
        log-lib
        log
)

# (서드 라이브러리 추가)
# test1_lib
include_directories(${CMAKE_SOURCE_DIR}/libs/test1_lib/include)
add_subdirectory(${CMAKE_SOURCE_DIR}/libs/test1_lib)

# test2_lib
include_directories(${CMAKE_SOURCE_DIR}/libs/test2_lib/include)
add_subdirectory(${CMAKE_SOURCE_DIR}/libs/test2_lib)

# (래퍼 라이브러리 추가)
# test1
add_library(
        native_wrapper_test1 # 자바용 라이브러리 이름
        SHARED # 종류
        wrappers/native_wrapper_test1.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_test1
        test1_lib
        ${log-lib}
)

# test2
add_library(
        native_wrapper_test2 # 자바용 라이브러리 이름
        SHARED # 종류
        wrappers/native_wrapper_test2.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        native_wrapper_test2
        test2_lib
        ${log-lib}
)