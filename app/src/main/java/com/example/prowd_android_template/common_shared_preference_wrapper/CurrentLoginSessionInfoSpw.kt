package com.example.prowd_android_template.common_shared_preference_wrapper

import android.app.Application
import android.content.Context

// (SharedPref 래퍼 클래스)
// 현재 로그인 세션 정보
class CurrentLoginSessionInfoSpw(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        "CurrentLoginSessionInfoSpw",
        Context.MODE_PRIVATE
    )

    // (autoLogin)
    var isAutoLogin: Boolean
        get() {
            return spMbr.getBoolean(
                "isAutoLogin",
                false
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putBoolean(
                    "isAutoLogin",
                    value
                )
                apply()
            }
        }

    // (sessionToken)
    var sessionToken: String?
        get() {
            return spMbr.getString(
                "sessionToken",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "sessionToken",
                    value
                )
                apply()
            }
        }

    // (userNickName)
    var userNickName: String?
        get() {
            return spMbr.getString(
                "userNickName",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "userNickName",
                    value
                )
                apply()
            }
        }

    // (loginType)
    // 코드
    // 0 : 비회원, 1 : 자체 서버, 2 : google, 3 : kakao, 4 : naver
    var loginType: Int
        get() {
            return spMbr.getInt(
                "loginType",
                0
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putInt(
                    "loginType",
                    value
                )
                apply()
            }
        }

    // (userServerId)
    var userServerId: String?
        get(): String? {
            return spMbr.getString(
                "userServerId",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "userServerId",
                    value
                )
                apply()
            }
        }

    // (userServerPw)
    var userServerPw: String?
        get(): String? {
            return spMbr.getString(
                "userServerPw",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "userServerPw",
                    value
                )
                apply()
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}
