package com.example.prowd_android_template.activity_set.activity_init

import android.app.Application
import android.content.Context

// (SharedPref 래퍼 클래스)
// 현재 로그인 세션 정보
class ActivityInitSpw(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        "ActivityInitSpw",
        Context.MODE_PRIVATE
    )

    // (isPermissionInitShownBefore)
    var isPermissionInitShownBefore: Boolean
        get() {
            return spMbr.getBoolean(
                "isPermissionInitShownBefore",
                false
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putBoolean(
                    "isPermissionInitShownBefore",
                    value
                )
                apply()
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}
