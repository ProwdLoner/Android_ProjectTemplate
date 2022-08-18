package com.example.prowd_android_template.activity_set.activity_aar_module_caller_sample

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityAarModuleCallerSampleBinding
import com.example.prowd_android_template.repository.RepositorySet
import com.example.test_module_for_aar_compile.activity_set.ActivityTestModuleForAarCompile
import com.example.test_module_for_aar_compile.util_object.CustomUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


// AAR 파일로 컴파일된 모듈을 호출하는 샘플
// 모듈로 작성한 액티비티를 실행시킨 후 바로 종료되는 액티비티
// 현 모듈 최상단의 libs 폴더 안에 컴파일된 aar 파일을 넣고,
// build.gradle(:app) 안에 implementation fileTree(dir: 'libs', include: ['*.aar']) 를 추가하여 실행
// aar 파일이 추가되었는데 코드상 해당 모듈 기능이 검색되지 않으면, build.gradle 을 새로 빌드하면 됩니다.
class ActivityAarModuleCallerSample : AppCompatActivity() {
    // <멤버 상수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityAarModuleCallerSampleBinding

    // (repository 모델)
    lateinit var repositorySetMbr: RepositorySet

    // (SharedPreference 객체)
    // 현 로그인 정보 접근 객체
    lateinit var currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw

    // (스레드 풀)
    val executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    // (앱 진입 필수 권한 배열)
    // : 앱 진입에 필요한 권한 배열.
    //     ex : Manifest.permission.INTERNET
    val activityPermissionArrayMbr: Array<String> = arrayOf()

    // (최초 실행 플래그) : 액티비티가 실행되고, 권한 체크가 끝난 후의 최초 로직이 실행되었는지 여부
    var doItAlreadyMbr = false

    // (이 화면에 도달한 유저 계정 고유값) : 세션 토큰이 없다면 비회원 상태
    var currentUserSessionTokenMbr: String? = null


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (다이얼로그 객체)
    var dialogMbr: Dialog? = null
    var shownDialogClassNameMbr: InterfaceDialogInfoVO? = null
        set(value) {
            when (value) {
                is DialogBinaryChoose.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogBinaryChoose(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                is DialogConfirm.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogConfirm(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                is DialogProgressLoading.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogProgressLoading(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                is DialogRadioButtonChoose.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogRadioButtonChoose(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                else -> {
                    dialogMbr?.dismiss()
                    dialogMbr = null

                    field = null
                    return
                }
            }
            field = value
        }

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((Map<String, Boolean>) -> Unit))? = null

    // (ActivityResultLauncher 객체)
    // : 액티비티 결과 받아오기 객체. 사용법은 permissionRequestMbr 와 동일
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    // : 액티비티 실행  = onCreate() → onStart() → onResume()
    //     액티비티 일시정지 후 재실행 = onPause() → ... -> onResume()
    //     액티비티 정지 후 재실행 = onPause() → onStop() -> ... -> onStart() → onResume()
    //     액티비티 종료 = onPause() → onStop() → onDestroy()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (초기 객체 생성)
        onCreateInitObject()

        // (초기 뷰 설정)
        onCreateInitView()
    }

    override fun onResume() {
        super.onResume()

        // (액티비티 진입 필수 권한 확인)
        // 진입 필수 권한이 클리어 되어야 로직이 실행
        permissionRequestCallbackMbr = { permissions ->
            var isPermissionAllGranted = true
            var neverAskAgain = false
            for (activityPermission in activityPermissionArrayMbr) {
                if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                    // 권한 클리어 플래그를 변경하고 break
                    neverAskAgain = !shouldShowRequestPermissionRationale(activityPermission)
                    isPermissionAllGranted = false
                    break
                }
            }

            if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
                allPermissionsGranted()
            } else if (!neverAskAgain) { // 단순 거부
                shownDialogClassNameMbr = DialogConfirm.DialogInfoVO(
                    true,
                    "권한 필요",
                    "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                    "뒤로가기",
                    onCheckBtnClicked = {
                        shownDialogClassNameMbr = null

                        finish()
                    },
                    onCanceled = {
                        shownDialogClassNameMbr = null

                        finish()
                    }
                )

            } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                shownDialogClassNameMbr =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "권한 요청",
                        "해당 서비스를 이용하기 위해선\n" +
                                "필수 권한 승인이 필요합니다.\n" +
                                "권한 설정 화면으로 이동하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = {
                            shownDialogClassNameMbr = null

                            // 권한 설정 화면으로 이동
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", packageName, null)

                            resultLauncherCallbackMbr = {
                                // 설정 페이지 복귀시 콜백
                                var isPermissionAllGranted1 = true
                                for (activityPermission in activityPermissionArrayMbr) {
                                    if (ActivityCompat.checkSelfPermission(
                                            this,
                                            activityPermission
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) { // 거부된 필수 권한이 존재
                                        // 권한 클리어 플래그를 변경하고 break
                                        isPermissionAllGranted1 = false
                                        break
                                    }
                                }

                                if (isPermissionAllGranted1) { // 권한 승인
                                    allPermissionsGranted()
                                } else { // 권한 거부
                                    shownDialogClassNameMbr =
                                        DialogConfirm.DialogInfoVO(
                                            true,
                                            "권한 요청",
                                            "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                            "뒤로가기",
                                            onCheckBtnClicked = {
                                                shownDialogClassNameMbr =
                                                    null
                                                finish()
                                            },
                                            onCanceled = {
                                                shownDialogClassNameMbr =
                                                    null
                                                finish()
                                            }
                                        )
                                }
                            }
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            shownDialogClassNameMbr = null

                            shownDialogClassNameMbr =
                                DialogConfirm.DialogInfoVO(
                                    true,
                                    "권한 요청",
                                    "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                    "뒤로가기",
                                    onCheckBtnClicked = {
                                        shownDialogClassNameMbr =
                                            null
                                        finish()
                                    },
                                    onCanceled = {
                                        shownDialogClassNameMbr =
                                            null
                                        finish()
                                    }
                                )
                        },
                        onCanceled = {
                            shownDialogClassNameMbr = null

                            shownDialogClassNameMbr =
                                DialogConfirm.DialogInfoVO(
                                    true,
                                    "권한 요청",
                                    "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                    "뒤로가기",
                                    onCheckBtnClicked = {
                                        shownDialogClassNameMbr =
                                            null
                                        finish()
                                    },
                                    onCanceled = {
                                        shownDialogClassNameMbr =
                                            null
                                        finish()
                                    }
                                )
                        }
                    )

            }
        }

        permissionRequestMbr.launch(activityPermissionArrayMbr)
    }

    override fun onDestroy() {
        // 다이얼로그 객체 해소
        dialogMbr?.dismiss()

        super.onDestroy()
    }

    // (AndroidManifest.xml 에서 configChanges 에 설정된 요소에 변경 사항이 존재할 때 실행되는 콜백)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // 화면회전 landscape

        } else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { // 화면회전 portrait

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    private fun onCreateInitObject() {
        // 뷰 객체
        bindingMbr = ActivityAarModuleCallerSampleBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 레포지토리 객체
        repositorySetMbr = RepositorySet.getInstance(application)

        currentLoginSessionInfoSpwMbr = CurrentLoginSessionInfoSpw(application)

        // 권한 요청 객체 생성
        permissionRequestMbr =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                permissionRequestCallbackMbr?.let { it1 -> it1(it) }
                permissionRequestCallbackMbr = null
            }

        // ActivityResultLauncher 생성
        resultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            resultLauncherCallbackMbr?.let { it1 -> it1(it) }
            resultLauncherCallbackMbr = null
        }

    }

    // (초기 뷰 설정)
    private fun onCreateInitView() {

    }

    // (액티비티 진입 권한이 클리어 된 시점)
    private fun allPermissionsGranted() {
        if (!doItAlreadyMbr) {
            // (onCreate + permissionGrant)
            doItAlreadyMbr = true

            // (알고리즘)
            // 테스트 모듈의 함수 실행 결과 로깅
            Log.e("function result called from test module for aar compile", CustomUtil.test())

            // 테스트 모듈 액티비티 실행
            val intent =
                Intent(
                    this,
                    ActivityTestModuleForAarCompile::class.java
                )
            startActivity(intent)

            finish()
        } else {
            // (onResume - (onCreate + permissionGrant)) : 권한 클리어

            // (알고리즘)
        }

        // (onResume)
        // (알고리즘)
        // (뷰 데이터 로딩)
        // : 데이터 갱신은 유저 정보가 변경된 것을 기준으로 함.
        val sessionToken = currentLoginSessionInfoSpwMbr.sessionToken
        if (sessionToken != currentUserSessionTokenMbr) { // 액티비티 유저와 세션 유저가 다를 때
            // 진입 플래그 변경
            currentUserSessionTokenMbr = sessionToken

            // (데이터 수집)

            // (알고리즘)
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}