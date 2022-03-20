package com.example.prowd_android_template.activity_set.activity_gvc_sample

import android.app.Application
import android.content.Context

// (글로벌 변수로 사용할 SharedPref 래퍼 클래스)
// 액티비티에 종속된 것으로, 해당 액티비티의 onResume 에서 해석되어 동작을 제어하는 용도
class ActivityGvcSampleGvc(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 파일명
    private val spNameMbr = "ActivityGvcSampleGvc"

    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        spNameMbr,
        Context.MODE_PRIVATE
    )

    // SharedPreference 키 네임
    private val message1KeyMbr = "ActivityGvcSampleGvc_message1KeyMbr"
    private val message2KeyMbr = "ActivityGvcSampleGvc_message2KeyMbr"


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (message1)
    fun getMessage1(): String? {
        return spMbr.getString(
            message1KeyMbr,
            null
        )
    }

    fun setMessage1(inputData: String?) {
        with(spMbr.edit()) {
            putString(
                message1KeyMbr,
                inputData
            )

            apply()
        }
    }

    // (message2)
    fun getMessage2(): String? {
        return spMbr.getString(
            message2KeyMbr,
            null
        )
    }

    fun setMessage2(inputData: String?) {
        with(spMbr.edit()) {
            putString(
                message2KeyMbr,
                inputData
            )

            apply()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}
