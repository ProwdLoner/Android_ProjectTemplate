package com.example.prowd_android_template.activity_set.activity_easy_lut_sample

import android.app.Application
import android.content.Context

// (SharedPref 래퍼 클래스)
// 현재 로그인 세션 정보
class ActivityEasyLutSampleSpw(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        "ActivityEasyLutSampleSpw",
        Context.MODE_PRIVATE
    )

    // (selectedFilterName)
    var selectedFilterName: String?
        get() {
            return spMbr.getString(
                "selectedFilterName",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "selectedFilterName",
                    value
                )
                apply()
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}
