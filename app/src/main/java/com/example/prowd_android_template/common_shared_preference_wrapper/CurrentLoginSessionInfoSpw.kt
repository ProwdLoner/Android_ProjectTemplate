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
    // 자동 로그인 설정
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

    // (loginType)
    // 코드
    // 0 : 비회원, 1 : 이메일 회원, 2 : google, 3 : kakao, 4 : naver
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

    // (loginId)
    // 자체서버라면 서버 아이디, SNS 로그인이라면 각 식별 아이디
    var loginId: String?
        get(): String? {
            return spMbr.getString(
                "loginId",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "loginId",
                    value
                )
                apply()
            }
        }

    // (loginPw)
    // 서버 로그인 처럼 필요한 곳이 아니라면 null
    var loginPw: String?
        get(): String? {
            return spMbr.getString(
                "loginPw",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "loginPw",
                    value
                )
                apply()
            }
        }

    // (sessionToken)
    // 서버에서 발급한 고유 식별자. 비회원 상태라면 null
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


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (현 앱 상태를 로그아웃으로 만드는 함수)
    fun setLogout() {
        isAutoLogin = false
        loginType = 0
        userNickName = null
        loginId = null
        loginPw = null
        sessionToken = null
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}
