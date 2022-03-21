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


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (sessionToken)
    fun getSessionToken(): String? {
        return spMbr.getString(
            sessionTokenKeyMbr,
            null
        )
    }

    fun setSessionToken(inputData: String?) {
        with(spMbr.edit()) {
            putString(
                sessionTokenKeyMbr,
                inputData
            )

            apply()
        }
    }

    // (userNickName)
    fun getUserNickName(): String? {
        return spMbr.getString(
            userNickNameKeyMbr,
            null
        )
    }

    fun setUserNickName(inputData: String?) {
        with(spMbr.edit()) {
            putString(
                userNickNameKeyMbr,
                inputData
            )

            apply()
        }
    }

    // (loginType)
    // 코드
    // 0 : 비회원, 1 : 자체 서버, 2 : google, 3 : kakao, 4 : naver
    fun getLoginType(): Int {
        return spMbr.getInt(
            nameLoginTypeKeyMbr,
            0
        )
    }

    fun setLoginType(inputData: Int) {
        with(spMbr.edit()) {
            putInt(
                nameLoginTypeKeyMbr,
                inputData
            )

            apply()
        }
    }

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
