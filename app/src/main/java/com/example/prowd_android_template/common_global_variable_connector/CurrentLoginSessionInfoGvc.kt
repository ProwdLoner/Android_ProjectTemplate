package com.example.prowd_android_template.common_global_variable_connector

import android.app.Application
import android.content.Context

// (글로벌 변수로 사용할 SharedPref 래퍼 클래스)
// 현재 로그인 세션 정보
class CurrentLoginSessionInfoGvc(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 파일명
    private val spNameMbr = "GvcCurrentLoginSessionInfo"

    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        spNameMbr,
        Context.MODE_PRIVATE
    )


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    fun getData(): ColumnVo {
        val sessionToken = spMbr.getString(
            colNameSessionTokenMbr,
            null
        )

        val userNickName = spMbr.getString(
            colNameUserNickNameMbr,
            null
        )

        val loginType = spMbr.getInt(
            colNameLoginTypeMbr,
            0
        )

        val userServerId = spMbr.getString(
            colNameUserServerIdMbr,
            null
        )

        val userServerPw = spMbr.getString(
            colNameUserServerPwMbr,
            null
        )

        return ColumnVo(
            sessionToken,
            userNickName,
            loginType,
            userServerId,
            userServerPw
        )
    }

    fun setData(inputData: ColumnVo) {
        with(spMbr.edit()) {
            putString(
                colNameSessionTokenMbr,
                inputData.sessionToken
            )
            putString(
                colNameUserNickNameMbr,
                inputData.userNickName
            )
            putInt(
                colNameLoginTypeMbr,
                inputData.loginType
            )
            putString(
                colNameUserServerIdMbr,
                inputData.userServerId
            )
            putString(
                colNameUserServerPwMbr,
                inputData.userServerPw
            )

            apply()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // 로그인 정보 객체
    data class ColumnVo(
        val sessionToken: String?,
        val userNickName: String?,
        val loginType: Int,
        // loginType :
        //  0 = un_login,
        //  1 = my_server,
        //  2 = google,
        //  3 = kakao,
        //  4 = naver
        val userServerId: String?,
        val userServerPw: String?
    )

    // SharedPreference 컬럼명
    private val colNameSessionTokenMbr = "GvcCurrentLoginSessionInfoSessionToken"
    private val colNameUserNickNameMbr = "GvcCurrentLoginSessionInfoUserNickName"
    private val colNameLoginTypeMbr = "GvcCurrentLoginSessionInfoLoginType"
    private val colNameUserServerIdMbr = "GvcCurrentLoginSessionInfoUserServerId"
    private val colNameUserServerPwMbr = "GvcCurrentLoginSessionInfoUserServerPw"

}
