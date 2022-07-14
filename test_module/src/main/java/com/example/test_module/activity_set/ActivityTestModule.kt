package com.example.test_module.activity_set

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.test_module.R

// 모듈 호출 테스트용 액티비티
// 모듈 생성은, file -> new -> New Module -> Android Library 를 통해 할 것
// 레이아웃 파일명, string 아이디 등 리소스 아이디와 관련된 것은
// 호출하는 모듈의 리소스 아이디와 겹치지 않도록 이름 앞에 패키지명 추가
class ActivityTestModule : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.com_example_test_module_activity_test_module)
    }
}