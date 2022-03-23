package com.example.prowd_android_template.common_global_variable_connector

import android.app.Application
import android.content.Context

// (글로벌 변수로 사용할 SharedPref 래퍼 클래스)
// 현재 로그인 세션 정보
class CurrentLoginSessionInfoGvc(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        "CurrentLoginSessionInfoGvc",
        Context.MODE_PRIVATE
    )

    // (sessionToken)
    var sessionToken: String?
        get() {
            return spMbr.getString(
                "CurrentLoginSessionInfoGvc_sessionToken",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "CurrentLoginSessionInfoGvc_sessionToken",
                    value
                )
                apply()
            }
        }

    // (userNickName)
    var userNickName: String?
        get() {
            return spMbr.getString(
                "CurrentLoginSessionInfoGvc_userNickName",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "CurrentLoginSessionInfoGvc_userNickName",
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
                "CurrentLoginSessionInfoGvc_loginType",
                0
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putInt(
                    "CurrentLoginSessionInfoGvc_loginType",
                    value
                )
                apply()
            }
        }

    // (userServerId)
    var userServerId: String?
        get(): String? {
            return spMbr.getString(
                "CurrentLoginSessionInfoGvc_userServerId",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "CurrentLoginSessionInfoGvc_userServerId",
                    value
                )
                apply()
            }
        }

    // (userServerPw)
    var userServerPw: String?
        get(): String? {
            return spMbr.getString(
                "CurrentLoginSessionInfoGvc_userServerPw",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "CurrentLoginSessionInfoGvc_userServerPw",
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
