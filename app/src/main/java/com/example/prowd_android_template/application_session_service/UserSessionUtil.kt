package com.example.prowd_android_template.application_session_service

import android.app.Activity
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.repository.database_room.tables.TestUserInfoTable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// (유저 세션 관련 테스트 유틸)
// : 유저 세션 관련 함수들을 모아둔 유틸.
//     실제 서비스시엔 로그인 시스템에 맞게 새로운 유틸을 만들어 사용
//     기본적으로 본 템플릿 앱 세션은 한 서버에 대응하도록 만들어졌지만,
//     멀티 서버 로그인시엔 UserSessionUtil, CurrentLoginSessionInfoSpw 를 하나 더 만들고,
//     각 액티비티별 userUid 식별 처리 부분을 커스텀하면 됨
object UserSessionUtil {
    // (현 어플리케이션 세션 로그인 함수)
    // 입력 정보대로 로그인 요청 후 로그인 spw 에 결과 저장
    // 알고리즘 :
    //     1. SNS 로그인시 Oauth 검증
    //         loginId 가 비어있다면 최초 로그인 선택 시점.
    //         loginId 가 비어있지 않다면 기존 로그인 확인
    //         이후 id 와 access token 을 준비,
    //         SNS 로그인이 아니라면 입력받은 id 와 pw 사용
    //     2. 서버에 loginType, loginId(SNS 시엔 sns id), loginPw(SNS 시엔 sns access token) 를 가지고 로그인 요청
    //     3. 에러시 각 콜백 사용. 에러가 나지 않고 로그인 검증이 완료되면 spw 에 로그인 정보 저장
    fun sessionLogIn(
        activity: Activity,
        loginType: Int,
        loginId: String?,
        loginPw: String?,
        onLoginComplete: () -> Unit,
        onLoginFailed: () -> Unit,
        onNetworkError: () -> Unit,
        onServerError: () -> Unit
    ) {
        // (repository 모델)
        val repositorySet: RepositorySet = RepositorySet.getInstance(activity)

        // (스레드 풀)
        val executorService: ExecutorService = Executors.newCachedThreadPool()

        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpw = CurrentLoginSessionInfoSpw(activity.application)

        // 타입별 pw 의 의미가 다르므로 처리
        // 로그인 타입은 위의 CurrentLoginSessionInfoSpw 의 설정을 따름
        when (loginType) {
            1 -> { // 이메일 회원
                val id = loginId!!
                val pw = loginPw!!

                sessionLogInOnIdReady(
                    activity,
                    currentLoginSessionInfoSpw,
                    executorService,
                    repositorySet,
                    loginType,
                    id,
                    pw,
                    onLoginComplete,
                    onLoginFailed,
                    onNetworkError,
                    onServerError
                )
            }
            // 아래부터는 SNS 로그인용 토큰 수집
            // SNS 의 pw 는 Oauth 로그인 후 나오는 액세스 토큰을 서버로 넘겨주는 용도
            // 서버는 해당 액세스 토큰으로 현재 로그인 여부를 확인하는 용도.
            // 액세스 토큰을 SNS 에서 발급받아 건내줄 것
            2 -> { // 구글 회원
                // loginId 가 비어있다면 최초 로그인, 비어있지 않다면 기존 로그인 확인과 id 동일성 검증

                val id = "실제 구현시 OAuth SNS id 발급"
                val pw = "실제 구현시 OAuth access token 발급"

                sessionLogInOnIdReady(
                    activity,
                    currentLoginSessionInfoSpw,
                    executorService,
                    repositorySet,
                    loginType,
                    id,
                    pw,
                    onLoginComplete,
                    onLoginFailed,
                    onNetworkError,
                    onServerError
                )
            }
            3 -> { // 카카오 회원
                // loginId 가 비어있다면 최초 로그인, 비어있지 않다면 기존 로그인 확인과 id 동일성 검증

                val id = "실제 구현시 OAuth SNS id 발급"
                val pw = "실제 구현시 OAuth access token 발급"

                sessionLogInOnIdReady(
                    activity,
                    currentLoginSessionInfoSpw,
                    executorService,
                    repositorySet,
                    loginType,
                    id,
                    pw,
                    onLoginComplete,
                    onLoginFailed,
                    onNetworkError,
                    onServerError
                )
            }
            4 -> { // 네이버 회원
                // loginId 가 비어있다면 최초 로그인, 비어있지 않다면 기존 로그인 확인과 id 동일성 검증

                val id = "실제 구현시 OAuth SNS id 발급"
                val pw = "실제 구현시 OAuth access token 발급"

                sessionLogInOnIdReady(
                    activity,
                    currentLoginSessionInfoSpw,
                    executorService,
                    repositorySet,
                    loginType,
                    id,
                    pw,
                    onLoginComplete,
                    onLoginFailed,
                    onNetworkError,
                    onServerError
                )
            }
            else -> { // 비회원
                // 그냥 로그인 통과
                onLoginComplete()
                return
            }
        }
    }

    private fun sessionLogInOnIdReady(
        activity: Activity,
        currentLoginSessionInfoSpw: CurrentLoginSessionInfoSpw,
        executorService: ExecutorService,
        repositorySet: RepositorySet,
        loginType: Int,
        loginId: String,
        loginPw: String,
        onLoginComplete: () -> Unit,
        onLoginFailed: () -> Unit,
        onNetworkError: () -> Unit,
        onServerError: () -> Unit
    ) {
        // (정보 요청 콜백)
        val loginCompleteCallback =
            { statusCode: Int,
              userUid: String?,
              userNickName: String?,
              accessToken: String?,
              accessTokenExpireDate: String?,
              refreshToken: String?,
              refreshTokenExpireDate: String? ->
                activity.runOnUiThread {
                    when (statusCode) {
                        1 -> {// 로그인 완료
                            // 회원 처리
                            currentLoginSessionInfoSpw.setLogin(
                                currentLoginSessionInfoSpw.isAutoLogin,
                                loginType,
                                loginId,
                                loginPw,
                                userUid!!,
                                userNickName!!,
                                accessToken!!,
                                accessTokenExpireDate,
                                refreshToken,
                                refreshTokenExpireDate
                            )

                            onLoginComplete()
                        }
                        2 -> { // 로그인 정보 불일치
                            onLoginFailed()
                        }
                        -1 -> { // 네트워크 에러
                            onNetworkError()
                        }
                        else -> { // 그외 서버 에러
                            onServerError()
                        }
                    }
                }
            }

        // (로그인 요청)
        executorService.execute {
            // 아래는 원래 네트워크 서버에서 처리하는 로직
            val userInfoList: List<TestUserInfoTable.TableDao.GetUserInfoForLoginOutput>
            when (loginType) {
                1 -> {
                    // 이메일과 비번으로 검색
                    userInfoList =
                        repositorySet.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao()
                            .getUserInfoForLogin(loginId, loginPw, loginType)
                }
                2 -> { // 구글
                    // SNS 로그인시 pw를 액세스 토큰으로 사용하여, 각 SNS 로그인이 되어있는지 여부를 판단

                    // 해당 sns 회원가입이 되어있는지를 파악
                    userInfoList =
                        repositorySet.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao()
                            .getUserInfoForLogin(loginId, loginType)
                }
                3 -> { // 카카오
                    // SNS 로그인시 pw를 액세스 토큰으로 사용하여, 각 SNS 로그인이 되어있는지 여부를 판단

                    // 해당 sns 회원가입이 되어있는지를 파악
                    userInfoList =
                        repositorySet.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao()
                            .getUserInfoForLogin(loginId, loginType)
                }
                4 -> { // 네이버
                    // SNS 로그인시 pw를 액세스 토큰으로 사용하여, 각 SNS 로그인이 되어있는지 여부를 판단

                    // 해당 sns 회원가입이 되어있는지를 파악
                    userInfoList =
                        repositorySet.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao()
                            .getUserInfoForLogin(loginId, loginType)
                }
                else -> {
                    throw Exception("login Type Input Error")
                }
            }

            if (userInfoList.isEmpty()) { // 일치하는 정보가 없음
                loginCompleteCallback(
                    2,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            } else {
                val uid = userInfoList[0].uid
                val nickname = userInfoList[0].nickName

                // jwt 사용시 access token, refresh token 발행
                // 여기선 jwt 를 사용하지 않고, 단순히 uid 로 정보 요청을 한다고 가정
                loginCompleteCallback(
                    1,
                    uid.toString(),
                    nickname,
                    uid.toString(),
                    null,
                    null,
                    null
                )
            }
        }
    }

    // (현 어플리케이션 세션 로그아웃 함수)
    // 알고리즘 :
    //     1. SNS 로그아웃
    //     2. SPW 정보 로그아웃 처리
    fun sessionLogOut(activity: Activity) {
        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpw = CurrentLoginSessionInfoSpw(activity.application)

        // 서버에 로그아웃을 알릴 필요는 없음.
        // 로그아웃 함수를 사용하지 않고, 앱 방치 및 앱 삭제를 할 경우가 있기에 서버에선 이를 감안할것

        // SNS 로그인시 각 OAuth 로그아웃 처리
        when (currentLoginSessionInfoSpw.loginType) {
            2 -> { // 구글 회원

            }
            3 -> { // 카카오 회원

            }
            4 -> { // 네이버 회원

            }
        }

        currentLoginSessionInfoSpw.setLogout()
    }

    // (서버 액세스 토큰 재발급 함수)
    // 리플래시 토큰이 만료되었으면 재로그인 필요
    // 알고리즘 :
    //     1. 리플래시 토큰 없을시 status 2
    //     2. 리플래시 토큰 만료 여부 확인
    //     3. 리플래시 토큰 만료시 status 3
    //     4. 리플래시 토큰 사용 가능시 이를 이용해 서버에 액세스 토큰 요청
    //     5. 발급받은 액세스 토큰과 만료시간을 spw 에 저장해서 status 1
    // onComplete status :
    //     1 = 액세스 토큰 갱신 완료
    //     2 = 리플래시 토큰이 없음
    //     3 = 리플래시 토큰 만료
    fun refreshAccessToken(
        activity: Activity,
        onComplete: (status: Int) -> Unit,
        onNetworkError: () -> Unit,
        onServerError: () -> Unit
    ) {
        // (스레드 풀)
        val executorService: ExecutorService = Executors.newCachedThreadPool()

        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpw = CurrentLoginSessionInfoSpw(activity.application)

        val refreshToken = currentLoginSessionInfoSpw.serverRefreshToken

        if (refreshToken == null) {
            onComplete(2)
            return
        }

        val serverRefreshTokenExpireDateString =
            currentLoginSessionInfoSpw.serverRefreshTokenExpireDate
        if (serverRefreshTokenExpireDateString != null) { // 리플래시 토큰 만료 시간이 설정되어 있을 때
            val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS", Locale.KOREA)
            val serverRefreshTokenExpireDate = dateFormat.parse(serverRefreshTokenExpireDateString)

            val today = Date()
            if (today.after(serverRefreshTokenExpireDate)) {
                // 지금 시간이 리플래시 토큰 만료 시간을 넘었을 때
                onComplete(3)
                return
            }
        }

        // 토큰 요청
        // (정보 요청 콜백)
        val getAccessTokenCallback =
            { statusCode: Int,
              accessToken: String?,
              accessTokenExpireDate: String? ->
                activity.runOnUiThread {
                    when (statusCode) {
                        1 -> {// 로그인 완료
                            // 회원 처리
                            currentLoginSessionInfoSpw.serverAccessToken = accessToken
                            currentLoginSessionInfoSpw.serverAccessTokenExpireDate =
                                accessTokenExpireDate

                            onComplete(1)
                        }
                        2 -> { // 토큰 만료
                            onComplete(3)
                        }
                        -1 -> { // 네트워크 에러
                            onNetworkError()
                        }
                        else -> { // 그외 서버 에러
                            onServerError()
                        }
                    }
                }
            }

        // (재발급 요청)
        executorService.execute {
            // 서버에서 리플래시 토큰으로 JWT 재발급
            // 아래에선 임의로 userUid 를 발행하도록 함
            getAccessTokenCallback(1, currentLoginSessionInfoSpw.userUid, null)
        }
    }
}