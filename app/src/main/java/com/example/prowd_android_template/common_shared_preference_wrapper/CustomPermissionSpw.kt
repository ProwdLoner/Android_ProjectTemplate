package com.example.prowd_android_template.common_shared_preference_wrapper

import android.app.Application
import android.content.Context

// (SharedPref 래퍼 클래스)
// 현재 로그인 세션 정보
class CustomPermissionSpw(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        "CustomPermissionSpw",
        Context.MODE_PRIVATE
    )

    // (pushPermissionGranted)
    // 푸시 알림 권한 승인여부
    var pushPermissionGranted: Boolean
        get() {
            return spMbr.getBoolean(
                "pushPermissionGranted",
                false
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putBoolean(
                    "pushPermissionGranted",
                    value
                )
                apply()
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}
