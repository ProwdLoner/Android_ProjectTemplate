package com.example.prowd_android_template.abstract_class

import android.app.Activity

// (유저 세션 관련 유틸 인터페이스)
// : 유저 세션 유틸은 본 인터페이스를 구현하며,
//     연결되는 로그인 서비스에 맞도록 아래 함수들을 구현하여 사용
interface InterfaceUserSessionUtil {
    // (현 어플리케이션 세션 로그인 함수)
    // 입력 정보대로 로그인 요청 후 로그인 spw 에 결과 저장
    // 알고리즘 :
    //     1. SNS 로그인시 Oauth 검증 후 id 와 access token 을 준비, 아니라면 입력받은 id 와 pw 사용
    //     2. 서버에 loginType, loginId(SNS 시엔 sns id), loginPw(SNS 시엔 sns access token) 를 가지고 로그인 요청
    //     3. 에러시 각 콜백 사용. 에러가 나지 않고 로그인 검증이 완료되면 spw 에 로그인 정보 저장
    fun sessionLogIn(
        activity: Activity,
        loginType: Int,
        loginId: String?, // SNS 는 아무값을 넣어도 상관없기에 nullable. OAuth 에서 id 를 받아와 사용
        loginPw: String?, // SNS 는 아무값을 넣어도 상관없기에 nullable. OAuth 에서 access token 을 받아와 사용
        onLoginComplete: () -> Unit,
        onLoginFailed: () -> Unit,
        onNetworkError: () -> Unit,
        onServerError: () -> Unit
    )

    // (현 어플리케이션 세션 로그아웃 함수)
    // 알고리즘 :
    //     1. SNS 로그아웃
    //     2. SPW 정보 로그아웃 처리
    fun sessionLogOut(activity: Activity)

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
    )
}