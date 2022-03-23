package com.example.prowd_android_template.activity_set.activity_gvc_sample

import android.app.Application
import android.content.Context

// (글로벌 변수로 사용할 SharedPref 래퍼 클래스)
// 액티비티에 종속된 것으로, 해당 액티비티의 onResume 에서 해석되어 동작을 제어하는 용도
class ActivityGvcSampleGvc(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        "ActivityGvcSampleGvc",
        Context.MODE_PRIVATE
    )

    // (message1)
    var message1: String?
        get(): String? {
            return spMbr.getString(
                "ActivityGvcSampleGvc_message1",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "ActivityGvcSampleGvc_message1",
                    value
                )
                apply()
            }
        }

    // (message2)
    var message2: String?
        get(): String? {
            return spMbr.getString(
                "ActivityGvcSampleGvc_message2KeyMbr",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "ActivityGvcSampleGvc_message2KeyMbr",
                    value
                )
                apply()
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}
