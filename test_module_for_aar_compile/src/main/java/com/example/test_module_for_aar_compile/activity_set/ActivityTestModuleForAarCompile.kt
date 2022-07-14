package com.example.test_module_for_aar_compile.activity_set

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.test_module_for_aar_compile.R

// 모듈 호출 테스트용 액티비티
// 모듈 생성은, file -> new -> New Module -> Android Library 를 통해 할 것
// 레이아웃 파일명, string 아이디 등 리소스 아이디와 관련된 것은
// 호출하는 모듈의 리소스 아이디와 겹치지 않도록 이름 앞에 패키지명 추가
// AAR 파일로 컴파일은, 안드로이드 스튜디오 우측 Gradle 탭을 누르고,
// Execute Gradle Task -> assembleRelease 검색 후, gradle :모듈명:assembleRelease 를  실행
// 결과물은, 프로젝트 폴더 - 모듈 폴더 - build - outputs - aar 안의, 모듈명-release.aar 이름으로 존재
// 위 결과물을 사용하려는 모듈 내의 libs 폴더 안에 붙여넣고
//  build.gradle 에 implementation fileTree(dir: 'libs', include: ['*.aar']) 이렇게 설정하여 사용하면 됨
class ActivityTestModuleForAarCompile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.com_example_test_module_for_aar_compile_activity_test_module_for_aar_compile)
    }
}