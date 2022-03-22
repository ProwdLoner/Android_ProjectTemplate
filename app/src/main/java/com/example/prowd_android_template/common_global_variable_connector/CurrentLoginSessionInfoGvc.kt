package com.example.prowd_android_template.common_global_variable_connector

import android.app.Application
import android.content.Context

// (글로벌 변수로 사용할 SharedPref 래퍼 클래스)
// 현재 로그인 세션 정보
class CurrentLoginSessionInfoGvc(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 파일명
    private val spNameMbr = "CurrentLoginSessionInfoGvc"

    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        spNameMbr,
        Context.MODE_PRIVATE
    )

    // SharedPreference 키 네임
    private val sessionTokenKeyMbr = "CurrentLoginSessionInfoGvc_sessionTokenKeyMbr"
    private val userNickNameKeyMbr = "CurrentLoginSessionInfoGvc_userNickNameKeyMbr"
    private val nameLoginTypeKeyMbr = "CurrentLoginSessionInfoGvc_nameLoginTypeKeyMbr"
    private val nameUserServerIdKeyMbr = "CurrentLoginSessionInfoGvc_nameUserServerIdKeyMbr"
    private val nameUserServerPwKeyMbr = "CurrentLoginSessionInfoGvc_nameUserServerPwKeyMbr"

    // (sessionToken)
    var sessionToken: String?
        get() {
            return spMbr.getString(
                sessionTokenKeyMbr,
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    sessionTokenKeyMbr,
                    value
                )
                apply()
            }
        }

    // (userNickName)
    var userNickName: String?
        get() {
            return spMbr.getString(
                userNickNameKeyMbr,
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    userNickNameKeyMbr,
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
                nameLoginTypeKeyMbr,
                0
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putInt(
                    nameLoginTypeKeyMbr,
                    value
                )

                apply()
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>

    // (userServerId)
    fun getUserServerId(): String? {
        return spMbr.getString(
            nameUserServerIdKeyMbr,
            null
        )
    }

    fun setUserServerId(inputData: String?) {
        with(spMbr.edit()) {
            putString(
                nameUserServerIdKeyMbr,
                inputData
            )

            apply()
        }
    }

    // (userServerPw)
    fun getUserServerPw(): String? {
        return spMbr.getString(
            nameUserServerPwKeyMbr,
            null
        )
    }

    fun setUserServerPw(inputData: String?) {
        with(spMbr.edit()) {
            putString(
                nameUserServerPwKeyMbr,
                inputData
            )

            apply()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}
