package com.example.prowd_android_template.common_shared_preference_wrapper

import android.app.Application
import android.content.Context

// (SharedPref 래퍼 클래스)
// 현재 로그인 세션 정보
class CustomDevicePermissionInfoSpw(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        "CustomDevicePermissionInfoSpw",
        Context.MODE_PRIVATE
    )

    // (isPushPermissionGranted)
    var isPushPermissionGranted: Boolean
        get() {
            return spMbr.getBoolean(
                "isPushPermissionGranted",
                false
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putBoolean(
                    "isPushPermissionGranted",
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
