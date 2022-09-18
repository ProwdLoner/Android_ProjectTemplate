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
//     = 서버 회원가입, 서버 로그인, 서버 로그아웃, 서버 회원탈퇴, SNS 로그인, SNS 로그아웃, JWT 토큰 재발행

//     서버 검증 및 유저 세션 관련 SPW 정보 변경을 담당.

//     실제 서비스시엔 로그인 시스템에 맞게 커스텀 하여 사용 할것.

//     기본적으로 본 템플릿 앱 세션은 한 서버에 대응하도록 만들어졌지만,
//     중계 앱 같은 멀티 서버 로그인시엔 UserSessionUtil, CurrentLoginSessionInfoSpw 세트를 따로 더 만들고,
//     각 액티비티별 userUid 식별 처리 부분을 커스텀하면 됨

//     아래 예시는 JWT 를 가정. FCM 을 사용하고 싶다면,
//     sessionLogIn, sessionLogOut, userJoin 시 FCM 토큰을 반환하여 서버에서 처리하도록 할것
object UserSessionUtil {
    // (서버 회원가입)
    // : 서버에 회원 가입 요청

    // 회원가입 필요 정보 객체
    // : API 요구 정보로 커스텀
    data class UserJoinInputVo(
        val logInType: Int, // 0 : 비회원, 1 : 이메일 회원, 2 : google, 3 : kakao, 4 : naver
        val id: String, // SNS 로그인시 SNS id
        val password: String, // SNS 로그인시 Access Token
        val nickName: String // 사용할 닉네임
    )

    //  onComplete statusCode :
    //     -1 = 네트워크 에러
    //     0 = 서버 에러
    //     1 = 가입 완료
    //     2 = 입력값 에러
    //     3 = 이미 가입된 회원
    fun userJoinToServer(
        activity: Activity,
        userJoinVo: UserJoinInputVo,
        onComplete: (status: Int) -> Unit
    ) {
        // (repository 모델)
        val repositorySet: RepositorySet = RepositorySet.getInstance(activity)

        // (스레드 풀)
        val executorService: ExecutorService = Executors.newCachedThreadPool()

        // 회원가입 콜백
        val signInCallback = { statusCode: Int ->
            activity.runOnUiThread {
                when (statusCode) {
                    -1 -> { // 네트워크 에러
                        onComplete(-1)
                    }
                    1 -> { // 회원 가입 완료
                        onComplete(1)
                    }
                    2 -> { // 입력값 에러
                        onComplete(2)
                    }
                    3 -> { // 아이디 중복
                        onComplete(3)
                    }
                    else -> { // 그외 서버 에러
                        onComplete(0)
                    }
                }
            }
        }

        // 회원가입 요청
        executorService.execute {
            // 아래는 원래 네트워크 서버에서 처리하는 로직
            // 이메일 중복검사
            val emailCount =
                repositorySet.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao()
                    .getIdCount(userJoinVo.id, userJoinVo.logInType)

            if (emailCount != 0) { // 아이디 중복
                signInCallback(3)
            } else {
                repositorySet.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao().insert(
                    TestUserInfoTable.TableVo(
                        userJoinVo.logInType,
                        userJoinVo.id,
                        userJoinVo.nickName,
                        userJoinVo.password
                    )
                )
                signInCallback(1)
            }
        }
    }


    // (현 어플리케이션 세션 로그인 함수)
    // : 입력 정보대로 로그인 요청 후 로그인 spw 에 결과 저장
    //     로그인 타입, 아이디, 비밀번호를 입력하면 서버에서 로그인 검증을 하고 결과를 LoginSpw 에 저장

    // 회원가입 필요 정보 객체
    // : API 요구 정보로 커스텀
    data class SessionLogInInputVo(
        val logInType: Int, // 1 : 이메일 회원, 2 : google, 3 : kakao, 4 : naver
        val logInId: String, // SNS 로그인시 ID는 API 에서 가져옴
        val logInPw: String // SNS 로그인시 PW는 API 에서 가져온 액세스 토큰을 사용
    )

    //  onComplete statusCode :
    //     -1 = 네트워크 에러
    //     0 = 서버 에러
    //     1 = 로그인 검증됨
    //     2 = 입력값 에러
    //     3 = 가입된 회원이 아님
    //     4 = 검증 불일치
    fun sessionLogIn(
        activity: Activity,
        sessionLogInInputVo: SessionLogInInputVo,
        onComplete: (status: Int) -> Unit
    ) {
        // (repository 모델)
        val repositorySet: RepositorySet = RepositorySet.getInstance(activity)

        // (스레드 풀)
        val executorService: ExecutorService = Executors.newCachedThreadPool()

        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpw = CurrentLoginSessionInfoSpw(activity.application)

        // (정보 요청 콜백)
        val loginCompleteCallback =
            { statusCode: Int,
              userUid: Long?, // 클라이언트 내 현 유저 구분을 위한 고유 값
              userNickName: String?, // 닉네임은 다른 디바이스에서 변경이 가능하니 받아오기
              accessToken: String?,
              accessTokenExpireDate: String?,
              refreshToken: String?,
              refreshTokenExpireDate: String? ->
                activity.runOnUiThread {
                    when (statusCode) {
                        -1 -> { // 네트워크 에러
                            onComplete(-1)
                        }
                        1 -> {// 로그인 완료
                            // 회원 처리
                            currentLoginSessionInfoSpw.setLogin(
                                currentLoginSessionInfoSpw.isAutoLogin,
                                sessionLogInInputVo.logInType,
                                sessionLogInInputVo.logInId,
                                sessionLogInInputVo.logInPw,
                                userUid!!.toString(),
                                userNickName!!,
                                accessToken,
                                accessTokenExpireDate,
                                refreshToken,
                                refreshTokenExpireDate
                            )

                            onComplete(1)
                        }
                        2 -> { // 입력값 에러
                            onComplete(2)
                        }
                        3 -> { // 가입된 회원이 아님
                            onComplete(3)
                        }
                        4 -> { // 로그인 정보 불일치
                            onComplete(4)
                        }
                        else -> { // 그외 서버 에러
                            onComplete(0)
                        }
                    }
                }
            }

        // (로그인 요청)
        // 서버에선 보내준 id, pw 를 가지고 적절한 검증 과정을 거치고 정보 반환
        executorService.execute {
            // 아래는 원래 네트워크 서버에서 처리하는 로직
            val userInfoList =
                repositorySet.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao()
                    .getUserInfoForLogin(
                        sessionLogInInputVo.logInId,
                        sessionLogInInputVo.logInType
                    )

            if (userInfoList.isEmpty()) { // 일치하는 정보가 없음
                loginCompleteCallback(
                    3,
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
                val password = userInfoList[0].password

                if (sessionLogInInputVo.logInType == 1 &&
                    password != sessionLogInInputVo.logInPw
                ) { // 이메일 로그인 비밀번호 불일치
                    loginCompleteCallback(
                        4,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                } else { // 검증 완료
                    // jwt 사용시 access token, refresh token 발행
                    // 여기선 jwt 를 구현하지 않았기에 null 반환
                    loginCompleteCallback(
                        1,
                        uid,
                        nickname,
                        null,
                        null,
                        null,
                        null
                    )
                }
            }
        }
    }

    // (현 어플리케이션 세션 로그아웃 함수)
    // : SNS 로그아웃은 정상 로그아웃이 된 이후에 처리할것
    //  onComplete statusCode :
    //     -1 = 네트워크 에러
    //     0 = 서버 에러
    //     1 = 로그아웃 완료
    fun sessionLogOut(
        activity: Activity,
        onComplete: (status: Int) -> Unit
    ) {
        // (스레드 풀)
        val executorService: ExecutorService = Executors.newCachedThreadPool()

        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpw = CurrentLoginSessionInfoSpw(activity.application)

        val loginType = currentLoginSessionInfoSpw.loginType

        if (loginType == 0) {
            onComplete(1)
        }

        // 별다른 이유가 없다면 서버에 로그아웃을 알릴 필요는 없음.
        // 로그아웃 함수를 사용하지 않고, 앱 방치 및 앱 삭제를 할 경우가 있기에 서버에선 이를 감안할것

        val serverLogoutCompleteCallback =
            { statusCode: Int ->
                activity.runOnUiThread {
                    when (statusCode) {
                        -1 -> { // 네트워크 에러
                            onComplete(-1)
                        }
                        1 -> { // 로그아웃 완료
                            // 내부 정보 로그아웃 처리
                            currentLoginSessionInfoSpw.setLogout()

                            onComplete(1)
                        }
                        else -> { // 그외 에러
                            onComplete(0)
                        }
                    }
                }
            }

        executorService.execute {
            serverLogoutCompleteCallback(1)
        }
    }

    // (회원탈퇴)
    // : 로그인과 동일한 정보를 보내면 서버에서 검증 후 검증 정보가 맞다면 회원탈퇴처리 후 앱 내부 로그인 정보 비우기
    //  onComplete statusCode :
    //     -1 = 네트워크 에러
    //     0 = 서버 에러
    //     1 = 회원탈퇴
    //     2 = 입력값 에러
    //     3 = 존재하지 않는 유저
    fun signOut(
        activity: Activity,
        logInType: Int, // 1 : 이메일 회원, 2 : google, 3 : kakao, 4 : naver
        logInId: String,
        onComplete: (status: Int) -> Unit
    ) {
        // (repository 모델)
        val repositorySet: RepositorySet = RepositorySet.getInstance(activity)

        // (스레드 풀)
        val executorService: ExecutorService = Executors.newCachedThreadPool()

        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpw = CurrentLoginSessionInfoSpw(activity.application)

        // (정보 요청 콜백)
        val signOutCompleteCallback =
            { statusCode: Int ->
                activity.runOnUiThread {
                    when (statusCode) {
                        1 -> {// 회원탈퇴 완료
                            // 회원 처리
                            currentLoginSessionInfoSpw.setLogout()

                            onComplete(1)
                        }
                        2 -> { // 입력값 에러
                            onComplete(2)
                        }
                        3 -> { // 존재하지 않는 유저
                            // 회원 처리
                            currentLoginSessionInfoSpw.setLogout()

                            onComplete(3)
                        }
                        -1 -> { // 네트워크 에러
                            onComplete(-1)
                        }
                        else -> { // 그외 서버 에러
                            onComplete(0)
                        }
                    }
                }
            }

        // (회원탈퇴 요청)
        executorService.execute {
            // 아래는 원래 네트워크 서버에서 처리하는 로직
            val userInfoList: List<TestUserInfoTable.TableDao.GetUserInfoForLoginOutput> =
                repositorySet.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao()
                    .getUserInfoForLogin(
                        logInId,
                        logInType
                    )

            if (userInfoList.isEmpty()) { // 일치하는 정보가 없음
                signOutCompleteCallback(3)
            } else { // 회원정보 검증 완료
                // 회원 정보 삭제
                repositorySet.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao()
                    .delete(userInfoList[0].uid)

                signOutCompleteCallback(1)
            }
        }
    }

    // (SNS 로그인)
    // : SNS 로그인을 완료한 후 결과값으로 서버에 넘겨줄 access token 을 반환
    //     서버는 클라이언트로부터 액세스 토큰을 받아서 SNS 서버에 해당 토큰 검증 요청으로 SNS 로그인 검증을 진행

    // 회원가입 필요 정보 객체
    // : API 요구 정보로 커스텀
    data class SnsLoginOutputVo(
        val snsId: String,
        val accessToken: String
    )

    //  onComplete statusCode :
    //     -1 = 네트워크 에러
    //     0 = 서버 에러
    //     1 = SNS 로그인 완료
    //     2 = 입력값 에러
    fun snsLogin(
        activity: Activity,
        logInType: Int, // 2 : google, 3 : kakao, 4 : naver
        onComplete: (status: Int, snsLoginOutputVo: SnsLoginOutputVo?) -> Unit
    ) {
        when (logInType) {
            2 -> { // 구글 로그인
                // 기존 SNS 로그인이 되어있다면 정보만 가져오기, 아니라면 SNS 로그인 후 가져오기
                val snsId = "sns api login and get id"
                val accessToken = "sns api login and get access token"
                onComplete(
                    1,
                    SnsLoginOutputVo(
                        snsId,
                        accessToken
                    )
                )
            }
            3 -> { // 카카오 로그인
                // 기존 SNS 로그인이 되어있다면 정보만 가져오기, 아니라면 SNS 로그인 후 가져오기
                val snsId = "sns api login and get id"
                val accessToken = "sns api login and get access token"
                onComplete(
                    1,
                    SnsLoginOutputVo(
                        snsId,
                        accessToken
                    )
                )
            }
            4 -> { // 네이버 로그인
                // 기존 SNS 로그인이 되어있다면 정보만 가져오기, 아니라면 SNS 로그인 후 가져오기
                val snsId = "sns api login and get id"
                val accessToken = "sns api login and get access token"
                onComplete(
                    1,
                    SnsLoginOutputVo(
                        snsId,
                        accessToken
                    )
                )
            }
            else -> {
                onComplete(2, null)
            }
        }
    }

    // (SNS 로그아웃)
    // : SNS 로그아웃
    // SNS 로그인 상태를 초기화하는 개념으로, 기존에 SNS 로그인이 안 되어있다면 바로 onComplete 1 반환
    //     단순 로그아웃과 SignOut 이라는 개념이 있을텐데,
    //     되도록 로그아웃시엔 다시 로그인 요청을 했을 때 다른 유저 선택이 가능하도록 처리

    //  onComplete statusCode :
    //     -1 = 네트워크 에러
    //     0 = 서버 에러
    //     1 = SNS 로그아웃 완료
    fun snsLogout(
        activity: Activity,
        logInType: Int, // 2 : google, 3 : kakao, 4 : naver
        onComplete: (status: Int) -> Unit
    ) {
        when (logInType) {
            2 -> { // 구글 로그인
                // SNS 로그아웃 요청
                onComplete(1)
            }
            3 -> { // 카카오 로그인
                // SNS 로그아웃 요청
                onComplete(1)
            }
            4 -> { // 네이버 로그인
                // SNS 로그아웃 요청
                onComplete(1)
            }
            else -> {
                onComplete(1)
            }
        }
    }

    // (서버 액세스 토큰 재발급 함수)
    // LoginSpw 에 저장된 리플래시 토큰으로 액세스 토큰 리플레시 요청.
    // 리플래시 토큰이 만료되었으면 재로그인 필요
    // 만료되지 않았다면 새로운 액세스 토큰과 새로운 리플래시 토큰을 받아와 LoginSpw 에 저장
    // 알고리즘 :
    //     1. 리플래시 토큰 없을시 status 2
    //     2. 리플래시 토큰 만료 여부 확인
    //     3. 리플래시 토큰 만료시 status 3
    //     4. 리플래시 토큰 사용 가능시 이를 이용해 서버에 액세스 토큰 요청
    //     5. 발급받은 액세스 토큰과 만료시간을 spw 에 저장해서 status 1

    // onComplete status :
    //     -1 = 네트워크 에러
    //     0 = 서버 에러
    //     1 = 액세스 토큰 갱신 완료
    //     2 = 리플래시 토큰이 없음
    //     3 = 리플래시 토큰 만료
    fun refreshAccessToken(
        activity: Activity,
        onComplete: (status: Int) -> Unit
    ) {
        // (스레드 풀)
        val executorService: ExecutorService = Executors.newCachedThreadPool()

        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpw = CurrentLoginSessionInfoSpw(activity.application)

        val refreshToken = currentLoginSessionInfoSpw.serverRefreshToken

        if (refreshToken == null) { // 저장된 리플래시 토큰이 없음
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
              newAccessToken: String?,
              newAccessTokenExpireDate: String?,
              newRefreshToken: String?,
              newRefreshTokenExpireDate: String? ->
                activity.runOnUiThread {
                    when (statusCode) {
                        -1 -> { // 네트워크 에러
                            onComplete(-1)
                        }
                        1 -> {// 로그인 완료
                            // 회원 처리
                            currentLoginSessionInfoSpw.serverAccessToken = newAccessToken
                            currentLoginSessionInfoSpw.serverAccessTokenExpireDate =
                                newAccessTokenExpireDate
                            currentLoginSessionInfoSpw.serverRefreshToken = newRefreshToken
                            currentLoginSessionInfoSpw.serverRefreshTokenExpireDate =
                                newRefreshTokenExpireDate

                            onComplete(1)
                        }
                        3 -> { // 토큰 만료
                            onComplete(3)
                        }
                        else -> { // 그외 서버 에러
                            onComplete(0)
                        }
                    }
                }
            }

        // (재발급 요청)
        executorService.execute {
            // 서버에서 리플래시 토큰으로 JWT 재발급
            // 아래에선 임의로 userUid 를 발행하도록 함
            // 여기선 jwt 를 구현하지 않았기에 null 반환
            getAccessTokenCallback(1, null, null, null, null)
        }
    }
}