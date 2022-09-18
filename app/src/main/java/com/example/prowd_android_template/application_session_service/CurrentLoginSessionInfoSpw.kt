package com.example.prowd_android_template.application_session_service

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

    // (loginId)
    // 이메일 회원이라면 이메일, SNS 로그인이라면 sns id, 비회원이라면 null
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
    // 이메일 회원이라면 비밀번호, SNS 로그인의 경우는 access token, 비회원이라면 null
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

    // (userUid)
    // : 서버에서 발급한 유저 고유 식별자.
    //     비회원 상태라면 null
    //     화면 갱신 여부 판단용으로 사용
    var userUid: String?
        get() {
            return spMbr.getString(
                "userUid",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "userUid",
                    value
                )
                apply()
            }
        }

    // (userNickName)
    // : 유저 닉네임
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

    // [아래부턴 서버토큰]
    // : 실 서비스에서 JWT 를 사용시 아래와 같은 정보를 받아오기
    //     액세스 토큰을 받아와서 각 서버 요청시마다 전송하여 인증
    //     액세스 토큰 발송 전, 만료기간을 살펴서 만료되면 리플래시 토큰으로 재발급 후 요청
    //     리플래시 토큰도 만료시 리플래시 토큰을 요청하여 액세스 토큰과 리플래시 토큰을 내부에 갱신하고, 갱신된 액세스 토큰으로 요청

    // (serverAccessToken)
    // : 서버에서 발급한 요청 토큰
    //     서버 요청시 인증 및 인가 판단에 사용
    var serverAccessToken: String?
        get() {
            return spMbr.getString(
                "serverAccessToken",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "serverAccessToken",
                    value
                )
                apply()
            }
        }

    // (serverAccessTokenExpireDate)
    // : 서버에서 발급한 요청 토큰의 만료일(null 이라면 무한정)
    //     format = "yyyy-MM-dd hh:mm:ss.SSS", Locale = KOREAN
    //     만료일이 지나면 리플래시 토큰으로 서버에 요청.
    var serverAccessTokenExpireDate: String?
        get() {
            return spMbr.getString(
                "serverAccessTokenExpireDate",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "serverAccessTokenExpireDate",
                    value
                )
                apply()
            }
        }

    // (serverRefreshToken)
    // : 서버에서 발급한 액세스 토큰의 리플래시 토큰
    //     서버 요청 토큰을 간편하게 다시 받아오는 토큰
    var serverRefreshToken: String?
        get() {
            return spMbr.getString(
                "serverRefreshToken",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "serverRefreshToken",
                    value
                )
                apply()
            }
        }

    // (serverRefreshTokenExpireDate)
    // : 액세스 토큰 만료시 다시 요청할 때 사용하는 리플래시 토큰의 만료일 (null 이라면 무한정)
    //     format = "yyyy-MM-dd hh:mm:ss.SSS", Locale = KOREAN
    //     이것마저 만료일이 지나면 재로그인 필요
    var serverRefreshTokenExpireDate: String?
        get() {
            return spMbr.getString(
                "serverRefreshTokenExpireDate",
                null
            )
        }
        set(value) {
            with(spMbr.edit()) {
                putString(
                    "serverRefreshTokenExpireDate",
                    value
                )
                apply()
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    fun setLogin(
        isAutoLogin: Boolean,
        loginType: Int,
        loginId: String,
        loginPw: String,
        userUid: String,
        userNickName: String,
        serverAccessToken: String?,
        serverAccessTokenExpireDate: String?,
        serverRefreshToken: String?,
        serverRefreshTokenExpireDate: String?
    ) {
        this.isAutoLogin = isAutoLogin
        this.loginType = loginType
        this.loginId = loginId
        this.loginPw = loginPw
        this.userUid = userUid
        this.userNickName = userNickName
        this.serverAccessToken = serverAccessToken
        this.serverAccessTokenExpireDate = serverAccessTokenExpireDate
        this.serverRefreshToken = serverRefreshToken
        this.serverRefreshTokenExpireDate = serverRefreshTokenExpireDate
    }

    // (현 앱 상태를 로그아웃으로 만드는 함수)
    fun setLogout() {
        this.isAutoLogin = false
        this.loginType = 0
        this.loginId = null
        this.loginPw = null
        this.userUid = null
        this.userNickName = null
        this.serverAccessToken = null
        this.serverAccessTokenExpireDate = null
        this.serverRefreshToken = null
        this.serverRefreshTokenExpireDate = null
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>

}
