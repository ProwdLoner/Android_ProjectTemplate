# cmake 최소 버전
cmake_minimum_required(VERSION 3.18.1)

# (서드 라이브러리 추가)

# (유저 라이브러리 추가)
# test1
add_library(
        test2_lib # 자바용 라이브러리 이름
        SHARED # 종류
        src/test2_lib.cpp # CPP 라이브러리 파일
)

target_link_libraries(
        test2_lib
)