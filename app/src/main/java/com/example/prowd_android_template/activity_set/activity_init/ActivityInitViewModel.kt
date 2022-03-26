package com.example.prowd_android_template.activity_set.activity_init

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.common_shared_preference_wrapper.CustomDevicePermissionInfoSpw
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class ActivityInitViewModel(application: Application) : AndroidViewModel(application) {
    // <멤버 변수 공간>
    private val applicationMbr = application

    // (repository 모델)
    private val repositorySetMbr: RepositorySet = RepositorySet.getInstance(applicationMbr)

    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (SharedPreference 객체)
    // 현 화면 정보 접근 객체
    val thisSpwMbr: ActivityInitSpw = ActivityInitSpw(application)

    // 현 로그인 정보 접근 객체
    val currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw =
        CurrentLoginSessionInfoSpw(application)

    // 디바이스 커스텀 권한 정보 접근 객체
    val customDevicePermissionInfoSpwMbr: CustomDevicePermissionInfoSpw =
        CustomDevicePermissionInfoSpw(application)

    // (데이터)
    // 대기시간 (밀리초)
    var countDownRestMilliSecMbr = 1000L
    val countDownIntervalMbr = 100L

    // (플래그 데이터)
    // 설정 변경 여부 : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
    var isChangingConfigurationsMbr = false

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr = true

    // 앱 기본 대기 시간이 끝났을 때
    var delayGoToNextActivityAsyncCompletedOnceMbr = false

    var isCheckAppPermissionsCompletedOnceMbr: Boolean = false
    var isCheckAppPermissionsOnProgressMbr: Boolean = false

    var isSendDeviceInfoToServerCompletedOnceMbr: Boolean = false
    var isSendDeviceInfoToServerOnProgressMbr: Boolean = false

    // (뷰 세마포어)
    val goToNextActivitySemaphoreMbr = Semaphore(1)


    // ---------------------------------------------------------------------------------------------
    // <뷰모델 라이브데이터 공간>
    // 로딩 다이얼로그 출력 정보
    val progressLoadingDialogInfoLiveDataMbr: MutableLiveData<DialogProgressLoading.DialogInfoVO?> =
        MutableLiveData(null)

    // 선택 다이얼로그 출력 정보
    val binaryChooseDialogInfoLiveDataMbr: MutableLiveData<DialogBinaryChoose.DialogInfoVO?> =
        MutableLiveData(null)

    // 확인 다이얼로그 출력 정보
    val confirmDialogInfoLiveDataMbr: MutableLiveData<DialogConfirm.DialogInfoVO?> =
        MutableLiveData(null)

    // 카운트 다운 숫자 데이터
    val countDownNumberLiveDataMbr: MutableLiveData<Int> =
        MutableLiveData((countDownRestMilliSecMbr.toFloat() / 1000f).toInt())


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCleared() {
        executorServiceMbr?.shutdown()
        executorServiceMbr = null
        super.onCleared()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // 앱 버전 체크
    private val checkAppVersionAsyncSemaphoreMbr = Semaphore(1)
    var checkAppVersionAsyncOnProgressedMbr = false
        private set

    var checkAppVersionAsyncCompletedOnceMbr = false
        private set
    var checkAppVersionAsyncResultMbr: Boolean? = null
        private set

    fun checkAppVersionAsync(
        onComplete: (Boolean) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            checkAppVersionAsyncSemaphoreMbr.acquire()
            checkAppVersionAsyncOnProgressedMbr = true

            if (checkAppVersionAsyncCompletedOnceMbr) { // 이전에 완료된 경우
                onComplete(checkAppVersionAsyncResultMbr!!)
                checkAppVersionAsyncOnProgressedMbr = false
                checkAppVersionAsyncSemaphoreMbr.release()
                return@execute
            }

            // 현재 버전 = ex : "1.0.0"
            val currentVersion = applicationMbr
                .packageManager
                .getPackageInfo(applicationMbr.packageName, 0)
                .versionName

            val currVersionSplit = currentVersion.split(".")

            try {
                // todo : 서버에서 받아오기 (강제 업데이트가 필요한 버전)
                val networkResponseCode = 200
                val serverMinUpdateVersion = "1.0.0" // 업데이트 최소 버전 = ex : "1.0.0"

                when (networkResponseCode) {
                    200 -> { // 정상 응답이라 결정한 코드
                        val minUpdateVersionSplit = serverMinUpdateVersion.split(".")

                        // 현재 버전이 서버 업데이트 기준 미달일 때에 강제 업데이트 수행
                        checkAppVersionAsyncResultMbr =
                            (currVersionSplit[0].toInt() < minUpdateVersionSplit[0].toInt() // 앞 버전 확인
                                    || currVersionSplit[1].toInt() < minUpdateVersionSplit[1].toInt() // 중간 버전 확인
                                    || currVersionSplit[2].toInt() < minUpdateVersionSplit[2].toInt())

                        onComplete(checkAppVersionAsyncResultMbr!!)
                        checkAppVersionAsyncCompletedOnceMbr = true
                        checkAppVersionAsyncOnProgressedMbr = false
                        checkAppVersionAsyncSemaphoreMbr.release()
                    }
                    else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                        throw Throwable("$networkResponseCode")
                    }
                }
            } catch (t: Throwable) {
                onError(t)
                checkAppVersionAsyncOnProgressedMbr = false
                checkAppVersionAsyncSemaphoreMbr.release()
            }
        }
    }

    // 로그인 체크
    private val checkLoginSessionAsyncSemaphoreMbr = Semaphore(1)
    var checkLoginSessionAsyncOnProgressedMbr = false
        private set

    var checkLoginSessionAsyncCompletedOnceMbr = false
        private set
    var checkLoginSessionAsyncResultMbr:
            CheckLoginSessionResultVO? = null
        private set

    fun checkLoginSessionAsync(
        parameterVO: CheckLoginSessionParameterVO,
        onComplete: (CheckLoginSessionResultVO) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            checkLoginSessionAsyncSemaphoreMbr.acquire()
            checkLoginSessionAsyncOnProgressedMbr = true

            if (checkLoginSessionAsyncCompletedOnceMbr) { // 이전에 완료된 경우
                onComplete(checkLoginSessionAsyncResultMbr!!)
                checkLoginSessionAsyncOnProgressedMbr = false
                checkLoginSessionAsyncSemaphoreMbr.release()

                return@execute
            }

            when (parameterVO.loginType) {
                0 -> { // 비회원 상태
                    // 로그인 세션 변경
                    checkLoginSessionAsyncResultMbr =
                        CheckLoginSessionResultVO(
                            0,
                            null,
                            null,
                            null,
                            null
                        )
                    onComplete(checkLoginSessionAsyncResultMbr!!)
                    checkLoginSessionAsyncCompletedOnceMbr = true
                    checkLoginSessionAsyncOnProgressedMbr = false
                    checkLoginSessionAsyncSemaphoreMbr.release()
                }

                1 -> { // 자체 서버 로그인 상태
                    // 로그인 요청 후 세션 토큰을 받아오기
                    try {
                        // todo : 실제 서버 요청
                        val networkResponseCode = 200

                        // 서버 반환 정보
                        val isLoginConfirm = true
                        val serverToken = "test_token_1234"
                        val serverUserNickname = "test_user_my_server"

                        when (networkResponseCode) {
                            200 -> { // 정상 응답이라 결정한 코드
                                if (isLoginConfirm) { // 검증 완료
                                    // 로그인 세션 변경
                                    checkLoginSessionAsyncResultMbr =
                                        CheckLoginSessionResultVO(
                                            1,
                                            serverToken,
                                            serverUserNickname,
                                            parameterVO.serverId,
                                            parameterVO.serverPw
                                        )
                                    onComplete(checkLoginSessionAsyncResultMbr!!)
                                    checkLoginSessionAsyncCompletedOnceMbr = true
                                    checkLoginSessionAsyncOnProgressedMbr = false
                                    checkLoginSessionAsyncSemaphoreMbr.release()

                                } else { // 검증 실패
                                    // 로그인 세션 변경
                                    checkLoginSessionAsyncResultMbr =
                                        CheckLoginSessionResultVO(
                                            0,
                                            null,
                                            null,
                                            null,
                                            null
                                        )
                                    onComplete(checkLoginSessionAsyncResultMbr!!)
                                    checkLoginSessionAsyncCompletedOnceMbr = true
                                    checkLoginSessionAsyncOnProgressedMbr = false
                                    checkLoginSessionAsyncSemaphoreMbr.release()

                                }
                            }
                            else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                                throw Throwable("$networkResponseCode")
                            }
                        }
                    } catch (t: Throwable) {
                        onError(t)
                        checkLoginSessionAsyncOnProgressedMbr = false
                        checkLoginSessionAsyncSemaphoreMbr.release()
                    }
                }

                2 -> { // 구글 로그인 상태
                    // 로그인 요청 후 세션 토큰을 받아오기
                    try {
                        // todo : 실제 서버 요청
                        val networkResponseCode = 200

                        // 서버 반환 정보
                        val isLoginConfirm = true
                        val serverToken = "test_token_1234"
                        val serverUserNickname = "test_user_my_server"

                        when (networkResponseCode) {
                            200 -> { // 정상 응답이라 결정한 코드
                                if (isLoginConfirm) { // 검증 완료
                                    // 로그인 세션 변경
                                    checkLoginSessionAsyncResultMbr =
                                        CheckLoginSessionResultVO(
                                            2,
                                            serverToken,
                                            serverUserNickname,
                                            parameterVO.serverId,
                                            parameterVO.serverPw
                                        )
                                    onComplete(checkLoginSessionAsyncResultMbr!!)
                                    checkLoginSessionAsyncCompletedOnceMbr = true
                                    checkLoginSessionAsyncOnProgressedMbr = false
                                    checkLoginSessionAsyncSemaphoreMbr.release()

                                } else { // 검증 실패
                                    // 로그인 세션 변경
                                    checkLoginSessionAsyncResultMbr =
                                        CheckLoginSessionResultVO(
                                            0,
                                            null,
                                            null,
                                            null,
                                            null
                                        )
                                    onComplete(checkLoginSessionAsyncResultMbr!!)
                                    checkLoginSessionAsyncCompletedOnceMbr = true
                                    checkLoginSessionAsyncOnProgressedMbr = false
                                    checkLoginSessionAsyncSemaphoreMbr.release()

                                }
                            }
                            else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                                throw Throwable("$networkResponseCode")
                            }
                        }
                    } catch (t: Throwable) {
                        onError(t)
                        checkLoginSessionAsyncOnProgressedMbr = false
                        checkLoginSessionAsyncSemaphoreMbr.release()
                    }
                }

                3 -> { // 카카오 로그인 상태
                    // 로그인 요청 후 세션 토큰을 받아오기
                    try {
                        // todo : 실제 서버 요청
                        val networkResponseCode = 200

                        // 서버 반환 정보
                        val isLoginConfirm = true
                        val serverToken = "test_token_1234"
                        val serverUserNickname = "test_user_my_server"

                        when (networkResponseCode) {
                            200 -> { // 정상 응답이라 결정한 코드
                                if (isLoginConfirm) { // 검증 완료
                                    // 로그인 세션 변경
                                    checkLoginSessionAsyncResultMbr =
                                        CheckLoginSessionResultVO(
                                            3,
                                            serverToken,
                                            serverUserNickname,
                                            parameterVO.serverId,
                                            parameterVO.serverPw
                                        )
                                    onComplete(checkLoginSessionAsyncResultMbr!!)
                                    checkLoginSessionAsyncCompletedOnceMbr = true
                                    checkLoginSessionAsyncOnProgressedMbr = false
                                    checkLoginSessionAsyncSemaphoreMbr.release()

                                } else { // 검증 실패
                                    // 로그인 세션 변경
                                    checkLoginSessionAsyncResultMbr =
                                        CheckLoginSessionResultVO(
                                            0,
                                            null,
                                            null,
                                            null,
                                            null
                                        )
                                    onComplete(checkLoginSessionAsyncResultMbr!!)
                                    checkLoginSessionAsyncCompletedOnceMbr = true
                                    checkLoginSessionAsyncOnProgressedMbr = false
                                    checkLoginSessionAsyncSemaphoreMbr.release()

                                }
                            }
                            else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                                throw Throwable("$networkResponseCode")
                            }
                        }
                    } catch (t: Throwable) {
                        onError(t)
                        checkLoginSessionAsyncOnProgressedMbr = false
                        checkLoginSessionAsyncSemaphoreMbr.release()
                    }
                }

                4 -> { // 네이버 로그인 상태
                    // 로그인 요청 후 세션 토큰을 받아오기
                    try {
                        // todo : 실제 서버 요청
                        val networkResponseCode = 200

                        when (networkResponseCode) {
                            200 -> { // 정상 응답이라 결정한 코드
                                // 서버 반환 정보
                                val isLoginConfirm = true
                                val serverToken = "test_token_1234"
                                val serverUserNickname = "test_user_my_server"

                                if (isLoginConfirm) { // 검증 완료
                                    // 로그인 세션 변경
                                    checkLoginSessionAsyncResultMbr =
                                        CheckLoginSessionResultVO(
                                            4,
                                            serverToken,
                                            serverUserNickname,
                                            parameterVO.serverId,
                                            parameterVO.serverPw
                                        )
                                    onComplete(checkLoginSessionAsyncResultMbr!!)
                                    checkLoginSessionAsyncCompletedOnceMbr = true
                                    checkLoginSessionAsyncOnProgressedMbr = false
                                    checkLoginSessionAsyncSemaphoreMbr.release()

                                } else { // 검증 실패
                                    // 로그인 세션 변경
                                    checkLoginSessionAsyncResultMbr =
                                        CheckLoginSessionResultVO(
                                            0,
                                            null,
                                            null,
                                            null,
                                            null
                                        )
                                    onComplete(checkLoginSessionAsyncResultMbr!!)
                                    checkLoginSessionAsyncCompletedOnceMbr = true
                                    checkLoginSessionAsyncOnProgressedMbr = false
                                    checkLoginSessionAsyncSemaphoreMbr.release()

                                }
                            }
                            else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                                throw Throwable("$networkResponseCode")
                            }
                        }
                    } catch (t: Throwable) {
                        onError(t)
                        checkLoginSessionAsyncOnProgressedMbr = false
                        checkLoginSessionAsyncSemaphoreMbr.release()
                    }
                }
            }
        }
    }

    // todo
    // 디바이스 정보 서버 반영
    private val postDeviceInfoAsyncSemaphoreMbr = Semaphore(1)
    var postDeviceInfoAsyncOnProgressedMbr = false
        private set

    var postDeviceInfoAsyncCompletedOnceMbr = false
        private set

    fun postDeviceInfoAsync(
        pushPermissionGranted: Boolean,
        cameraPermissionGranted: Boolean,
        readExternalStoragePermissionGranted: Boolean,
        locationPermissionType: Int,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        Log.e("pushPermissionGranted", pushPermissionGranted.toString())
        Log.e("cameraPermissionGranted", cameraPermissionGranted.toString())
        Log.e(
            "readExternalStoragePermissionGranted",
            readExternalStoragePermissionGranted.toString()
        )
        Log.e("locationPermissionType", locationPermissionType.toString())

        executorServiceMbr?.execute {
            postDeviceInfoAsyncSemaphoreMbr.acquire()
            postDeviceInfoAsyncOnProgressedMbr = true

            if (postDeviceInfoAsyncCompletedOnceMbr) { // 이전에 완료된 경우
                onComplete()
                postDeviceInfoAsyncOnProgressedMbr = false
                postDeviceInfoAsyncSemaphoreMbr.release()

                return@execute
            }

            try {
                // todo : 서버에 정보 전달
                // 서버 반환값
                val networkResponseCode = 200

                when (networkResponseCode) {
                    200 -> { // 정상 응답이라 결정한 코드


                        onComplete()
                        postDeviceInfoAsyncCompletedOnceMbr = true
                        postDeviceInfoAsyncOnProgressedMbr = false
                        postDeviceInfoAsyncSemaphoreMbr.release()
                    }
                    else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                        throw Throwable("$networkResponseCode")
                    }
                }
            } catch (t: Throwable) {
                onError(t)
                postDeviceInfoAsyncOnProgressedMbr = false
                postDeviceInfoAsyncSemaphoreMbr.release()
            }

            // todo

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    data class CheckLoginSessionParameterVO(
        var loginType: Int,
        var serverId: String?,
        var serverPw: String?
    )

    data class CheckLoginSessionResultVO(
        var loginType: Int,
        var sessionToken: String?,
        var userNickName: String?,
        var userServerId: String?,
        var userServerPw: String?
    )
}