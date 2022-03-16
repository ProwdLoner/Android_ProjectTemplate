# cmake 최소 버전
cmake_minimum_required(VERSION 3.18.1)

# 프로젝트명
project("basic_project_structure_sample_android_kotlin")

# 라이브러리 추가
add_library(
        native_lib # 자바용 라이브러리 이름
        SHARED # 종류
        user_lib/native_lib.cpp # CPP 라이브러리 파일
)

# 라이브러리 탐색
# 로그 라이브러리
find_library(
        log-lib
        log
)

# 라이브러리 링크 설정
target_link_libraries(
        native_lib
        ${log-lib}
)