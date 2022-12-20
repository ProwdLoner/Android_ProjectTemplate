package com.example.prowd_android_template.activity_set.activity_permission_sample

import android.Manifest
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.prowd_android_template.BuildConfig
import com.example.prowd_android_template.abstract_class.AbstractProwdRecyclerViewAdapter
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.common_shared_preference_wrapper.CustomPermissionSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityPermissionSampleBinding
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.util_class.ThreadConfluenceObj
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// 권한 규칙 :
// 1. 서비스 필수 권한은 해당 서비스 사용 액티비티 진입시 요청하기 (모듈단위 개발을 위해)
// 2. 서버에 권한상태를 전할 필요는 없음.
//    1계정 여러 디바이스가 존재할수 있으니 저장할 데이터도 많아짐.
//    카메라 권한과 같은건 서버에서 알 필요가 없고,
//    위치 서비스를 위한 위치 권한은, 좌표값을 null 로 보내주는 방법으로 서버에서 데이터 추려오는 기준으로 사용.
//    푸시 권한은 내부적으로 처리를 하면 됨.
// 3. 계정만 바뀌었을 때에는 수동으로 변경하지 않는 이상 권한 변경은 없음. (기존 기기에 저장된 권한이 계속 이어짐)
//    고로 설정 화면에서 이를 조정하는 스위치를 준비해주는 것도 좋은 방법.
class ActivityPermissionSample : AppCompatActivity() {
    // <설정 변수 공간>
    // (앱 진입 필수 권한 배열)
    // : 앱 진입에 필요한 권한 배열.
    //     ex : Manifest.permission.INTERNET
    private val activityPermissionArrayMbr: Array<String> = arrayOf()


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityPermissionSampleBinding

    // (repository 모델)
    lateinit var repositorySetMbr: RepositorySet

    // (어뎁터 객체)
    lateinit var adapterSetMbr: ActivityPermissionSampleAdapterSet

    // (SharedPreference 객체)
    // 클래스 비휘발성 저장객체
    lateinit var classSpwMbr: ActivityPermissionSampleSpw

    // 현 로그인 정보 접근 객체
    lateinit var currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw

    // 커스텀 권한 정보 접근 객체
    lateinit var customPermissionSpwMbr: CustomPermissionSpw

    // (스레드 풀)
    val executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    // (다이얼로그 객체)
    var dialogMbr: Dialog? = null
    var shownDialogInfoVOMbr: InterfaceDialogInfoVO? = null
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
    var permissionRequestOnProgressMbr = false
    val permissionRequestOnProgressSemaphoreMbr = Semaphore(1)

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

        // (권한 체크 후 함수 실행)
        // : requestPermission 시에 onPause 되고, onResume 이 다시 실행되므로 리퀘스트 복귀 시엔 여기를 지나게 되어있음
        var isPermissionAllGranted = true
        for (activityPermission in activityPermissionArrayMbr) {
            if (checkSelfPermission(activityPermission)
                == PackageManager.PERMISSION_DENIED
            ) { // 거부된 필수 권한이 존재
                // 권한 클리어 플래그를 변경하고 break
                isPermissionAllGranted = false
                break
            }
        }

        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
            allPermissionsGranted()
            return
        }

        // (권한 비충족으로 인한 권한 요청)
        // : 권한 요청시엔 onPause 되었다가 다시 onResume 으로 복귀함
        executorServiceMbr.execute {
            permissionRequestOnProgressSemaphoreMbr.acquire()
            runOnUiThread {
                if (!permissionRequestOnProgressMbr) { // 현재 권한 요청중이 아님
                    permissionRequestOnProgressMbr = true
                    permissionRequestOnProgressSemaphoreMbr.release()
                    // (액티비티 진입 필수 권한 확인)
                    // 진입 필수 권한이 클리어 되어야 로직이 실행

                    // 권한 요청 콜백
                    permissionRequestCallbackMbr = { permissions ->
                        var isPermissionAllGranted1 = true
                        var neverAskAgain = false
                        for (activityPermission in activityPermissionArrayMbr) {
                            if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                                // 권한 클리어 플래그를 변경하고 break
                                neverAskAgain =
                                    !shouldShowRequestPermissionRationale(activityPermission)
                                isPermissionAllGranted1 = false
                                break
                            }
                        }

                        if (isPermissionAllGranted1) { // 모든 권한이 클리어된 상황
                            permissionRequestOnProgressSemaphoreMbr.acquire()
                            permissionRequestOnProgressMbr = false
                            permissionRequestOnProgressSemaphoreMbr.release()

                        } else if (!neverAskAgain) { // 단순 거부
                            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                true,
                                "권한 필요",
                                "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                "뒤로가기",
                                onCheckBtnClicked = {
                                    shownDialogInfoVOMbr = null
                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                    permissionRequestOnProgressMbr = false
                                    permissionRequestOnProgressSemaphoreMbr.release()

                                    finish()
                                },
                                onCanceled = {
                                    shownDialogInfoVOMbr = null
                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                    permissionRequestOnProgressMbr = false
                                    permissionRequestOnProgressSemaphoreMbr.release()

                                    finish()
                                }
                            )

                        } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                            shownDialogInfoVOMbr =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "해당 서비스를 이용하기 위해선\n" +
                                            "필수 권한 승인이 필요합니다.\n" +
                                            "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 권한 설정 화면으로 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        intent.data = Uri.fromParts("package", packageName, null)

                                        resultLauncherCallbackMbr = {
                                            // 설정 페이지 복귀시 콜백
                                            var isPermissionAllGranted2 = true
                                            for (activityPermission in activityPermissionArrayMbr) {
                                                if (ActivityCompat.checkSelfPermission(
                                                        this,
                                                        activityPermission
                                                    ) != PackageManager.PERMISSION_GRANTED
                                                ) { // 거부된 필수 권한이 존재
                                                    // 권한 클리어 플래그를 변경하고 break
                                                    isPermissionAllGranted2 = false
                                                    break
                                                }
                                            }

                                            if (isPermissionAllGranted2) { // 권한 승인
                                                permissionRequestOnProgressSemaphoreMbr.acquire()
                                                permissionRequestOnProgressMbr = false
                                                permissionRequestOnProgressSemaphoreMbr.release()

                                            } else { // 권한 거부
                                                shownDialogInfoVOMbr =
                                                    DialogConfirm.DialogInfoVO(
                                                        true,
                                                        "권한 요청",
                                                        "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                                        "뒤로가기",
                                                        onCheckBtnClicked = {
                                                            shownDialogInfoVOMbr =
                                                                null
                                                            permissionRequestOnProgressSemaphoreMbr.acquire()
                                                            permissionRequestOnProgressMbr = false
                                                            permissionRequestOnProgressSemaphoreMbr.release()

                                                            finish()
                                                        },
                                                        onCanceled = {
                                                            shownDialogInfoVOMbr =
                                                                null
                                                            permissionRequestOnProgressSemaphoreMbr.acquire()
                                                            permissionRequestOnProgressMbr = false
                                                            permissionRequestOnProgressSemaphoreMbr.release()

                                                            finish()
                                                        }
                                                    )
                                            }
                                        }
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        shownDialogInfoVOMbr =
                                            DialogConfirm.DialogInfoVO(
                                                true,
                                                "권한 요청",
                                                "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                                "뒤로가기",
                                                onCheckBtnClicked = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                },
                                                onCanceled = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                }
                                            )
                                    },
                                    onCanceled = {
                                        shownDialogInfoVOMbr = null

                                        shownDialogInfoVOMbr =
                                            DialogConfirm.DialogInfoVO(
                                                true,
                                                "권한 요청",
                                                "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                                "뒤로가기",
                                                onCheckBtnClicked = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                },
                                                onCanceled = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                }
                                            )
                                    }
                                )

                        }
                    }

                    // 권한 요청
                    permissionRequestMbr.launch(activityPermissionArrayMbr)
                } else { // 현재 권한 요청중
                    permissionRequestOnProgressSemaphoreMbr.release()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        permissionRequestOnProgressSemaphoreMbr.acquire()
        if (permissionRequestOnProgressMbr) {
            permissionRequestOnProgressSemaphoreMbr.release()
            // 권한 요청중엔 onPause 가 실행될 수 있기에 아래에 위치할 정상 pause 로직 도달 방지
            return
        }
        permissionRequestOnProgressSemaphoreMbr.release()

        // (onPause 알고리즘)
    }

    override fun onDestroy() {
        super.onDestroy()

        // 다이얼로그 객체 해소
        dialogMbr?.dismiss()
    }

    // (AndroidManifest.xml 에서 configChanges 에 설정된 요소에 변경 사항이 존재할 때 실행되는 콜백)
    // : 해당 이벤트 발생시 처리할 로직을 작성
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
    // : 클래스에서 사용할 객체를 초기 생성
    private fun onCreateInitObject() {
        // 뷰 객체
        bindingMbr = ActivityPermissionSampleBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 레포지토리 객체
        repositorySetMbr = RepositorySet.getInstance(application)

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityPermissionSampleAdapterSet()

        // SPW 객체 생성
        classSpwMbr = ActivityPermissionSampleSpw(application)
        currentLoginSessionInfoSpwMbr = CurrentLoginSessionInfoSpw(application)
        customPermissionSpwMbr = CustomPermissionSpw(application)

        // 권한 요청 객체 생성
        permissionRequestMbr =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                permissionRequestCallbackMbr?.let { it1 -> it1(it) }
            }

        // ActivityResultLauncher 생성
        resultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            resultLauncherCallbackMbr?.let { it1 -> it1(it) }
        }

    }

    // (초기 뷰 설정)
    // : 뷰 리스너 바인딩, 초기 뷰 사이즈, 위치 조정 등
    private fun onCreateInitView() {
        // (리스너 설정)
        // 외부 저장소 읽기 권한
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 버전 별 표시할 버튼 컨테이너 설정
            bindingMbr.externalStorageReadImagesAndVideoPermissionContainer.visibility =
                View.VISIBLE
            bindingMbr.externalStorageReadAudioPermissionContainer.visibility = View.VISIBLE
            bindingMbr.externalStorageReadPermissionContainer.visibility = View.GONE

            // 외부 저장소 이미지 및 비디오 읽기 권한 스위치
            bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.setOnClickListener {
                bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isEnabled = false
                bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isClickable = false

                if (bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isChecked) { // 체크시
                    // 권한 요청
                    // 권한 요청 콜백
                    val permissionArray: Array<String> =
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                        )
                    permissionRequestCallbackMbr = { permissions ->
                        // (거부된 권한 리스트)
                        var isPermissionAllGranted = true // 모든 권한 승인여부
                        var neverAskAgain = false // 다시묻지 않기 체크 여부
                        for (permission in permissionArray) {
                            if (!permissions[permission]!!) { // 필수 권한 거부
                                // 모든 권한 승인여부 플래그를 변경하고 break
                                isPermissionAllGranted = false
                                neverAskAgain = !shouldShowRequestPermissionRationale(permission)
                                break
                            }
                        }

                        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황

                            bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isEnabled =
                                true
                            bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isClickable =
                                true
                        } else if (!neverAskAgain) { // 단순 거부
                            // 뷰 상태 되돌리기
                            bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isChecked =
                                false

                            bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isEnabled =
                                true
                            bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isClickable =
                                true
                        } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                            shownDialogInfoVOMbr =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 권한 설정 화면으로 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", packageName, null)
                                            }

                                        bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isEnabled =
                                            true
                                        bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isClickable =
                                            true
                                        resultLauncherCallbackMbr = {}
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 뷰 상태 되돌리기
                                        bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isChecked =
                                            false

                                        bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isEnabled =
                                            true
                                        bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isClickable =
                                            true
                                    },
                                    onCanceled = {}
                                )
                        }
                    }

                    // 권한 요청
                    permissionRequestMbr.launch(permissionArray)
                } else {
                    // 체크 해제시
                    // 권한 해제 다이얼로그
                    shownDialogInfoVOMbr =
                        DialogBinaryChoose.DialogInfoVO(
                            false,
                            "권한 해제",
                            "권한 해제를 위해 설정 화면으로 이동하시겠습니까?",
                            null,
                            null,
                            onPosBtnClicked = {
                                shownDialogInfoVOMbr = null

                                // 권한 설정 페이지 이동
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri

                                bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isEnabled =
                                    true
                                bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isClickable =
                                    true
                                resultLauncherCallbackMbr = {}
                                resultLauncherMbr.launch(intent)
                            },
                            onNegBtnClicked = {
                                shownDialogInfoVOMbr = null

                                bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isChecked =
                                    true

                                bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isEnabled =
                                    true
                                bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isClickable =
                                    true
                            },
                            onCanceled = {
                                // 취소 불가
                            }
                        )
                }
            }

            // 외부 저장소 오디오 읽기 권한 스위치
            bindingMbr.externalStorageReadAudioPermissionSwitch.setOnClickListener {
                bindingMbr.externalStorageReadAudioPermissionSwitch.isEnabled = false
                bindingMbr.externalStorageReadAudioPermissionSwitch.isClickable = false

                if (bindingMbr.externalStorageReadAudioPermissionSwitch.isChecked) { // 체크시
                    // 권한 요청
                    // 권한 요청 콜백
                    val permissionArray: Array<String> =
                        arrayOf(
                            Manifest.permission.READ_MEDIA_AUDIO
                        )
                    permissionRequestCallbackMbr = { permissions ->
                        // (거부된 권한 리스트)
                        var isPermissionAllGranted = true // 모든 권한 승인여부
                        var neverAskAgain = false // 다시묻지 않기 체크 여부
                        for (permission in permissionArray) {
                            if (!permissions[permission]!!) { // 필수 권한 거부
                                // 모든 권한 승인여부 플래그를 변경하고 break
                                isPermissionAllGranted = false
                                neverAskAgain = !shouldShowRequestPermissionRationale(permission)
                                break
                            }
                        }

                        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황

                            bindingMbr.externalStorageReadAudioPermissionSwitch.isEnabled = true
                            bindingMbr.externalStorageReadAudioPermissionSwitch.isClickable = true
                        } else if (!neverAskAgain) { // 단순 거부
                            // 뷰 상태 되돌리기
                            bindingMbr.externalStorageReadAudioPermissionSwitch.isChecked = false

                            bindingMbr.externalStorageReadAudioPermissionSwitch.isEnabled = true
                            bindingMbr.externalStorageReadAudioPermissionSwitch.isClickable = true
                        } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                            shownDialogInfoVOMbr =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 권한 설정 화면으로 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", packageName, null)
                                            }

                                        bindingMbr.externalStorageReadAudioPermissionSwitch.isEnabled =
                                            true
                                        bindingMbr.externalStorageReadAudioPermissionSwitch.isClickable =
                                            true
                                        resultLauncherCallbackMbr = {}
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 뷰 상태 되돌리기
                                        bindingMbr.externalStorageReadAudioPermissionSwitch.isChecked =
                                            false

                                        bindingMbr.externalStorageReadAudioPermissionSwitch.isEnabled =
                                            true
                                        bindingMbr.externalStorageReadAudioPermissionSwitch.isClickable =
                                            true
                                    },
                                    onCanceled = {}
                                )
                        }
                    }

                    // 권한 요청
                    permissionRequestMbr.launch(permissionArray)
                } else {
                    // 체크 해제시
                    // 권한 해제 다이얼로그
                    shownDialogInfoVOMbr =
                        DialogBinaryChoose.DialogInfoVO(
                            false,
                            "권한 해제",
                            "권한 해제를 위해 설정 화면으로 이동하시겠습니까?",
                            null,
                            null,
                            onPosBtnClicked = {
                                shownDialogInfoVOMbr = null

                                // 권한 설정 페이지 이동
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri

                                bindingMbr.externalStorageReadAudioPermissionSwitch.isEnabled = true
                                bindingMbr.externalStorageReadAudioPermissionSwitch.isClickable =
                                    true
                                resultLauncherCallbackMbr = {}
                                resultLauncherMbr.launch(intent)
                            },
                            onNegBtnClicked = {
                                shownDialogInfoVOMbr = null

                                bindingMbr.externalStorageReadAudioPermissionSwitch.isChecked = true

                                bindingMbr.externalStorageReadAudioPermissionSwitch.isEnabled = true
                                bindingMbr.externalStorageReadAudioPermissionSwitch.isClickable =
                                    true
                            },
                            onCanceled = {
                                // 취소 불가
                            }
                        )
                }
            }
        } else {
            // 버전 별 표시할 버튼 컨테이너 설정
            bindingMbr.externalStorageReadImagesAndVideoPermissionContainer.visibility = View.GONE
            bindingMbr.externalStorageReadAudioPermissionContainer.visibility = View.GONE
            bindingMbr.externalStorageReadPermissionContainer.visibility = View.VISIBLE

            // 외부 저장소 읽기 권한 스위치
            bindingMbr.externalStorageReadPermissionSwitch.setOnClickListener {
                bindingMbr.externalStorageReadPermissionSwitch.isEnabled = false
                bindingMbr.externalStorageReadPermissionSwitch.isClickable = false

                if (bindingMbr.externalStorageReadPermissionSwitch.isChecked) { // 체크시
                    // 권한 요청
                    // 권한 요청 콜백
                    val permissionArray: Array<String> =
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    permissionRequestCallbackMbr = { permissions ->
                        // (거부된 권한 리스트)
                        var isPermissionAllGranted = true // 모든 권한 승인여부
                        var neverAskAgain = false // 다시묻지 않기 체크 여부
                        for (permission in permissionArray) {
                            if (!permissions[permission]!!) { // 필수 권한 거부
                                // 모든 권한 승인여부 플래그를 변경하고 break
                                isPermissionAllGranted = false
                                neverAskAgain = !shouldShowRequestPermissionRationale(permission)
                                break
                            }
                        }

                        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황

                            bindingMbr.externalStorageReadPermissionSwitch.isEnabled = true
                            bindingMbr.externalStorageReadPermissionSwitch.isClickable = true
                        } else if (!neverAskAgain) { // 단순 거부
                            // 뷰 상태 되돌리기
                            bindingMbr.externalStorageReadPermissionSwitch.isChecked = false

                            bindingMbr.externalStorageReadPermissionSwitch.isEnabled = true
                            bindingMbr.externalStorageReadPermissionSwitch.isClickable = true
                        } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                            shownDialogInfoVOMbr =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 권한 설정 화면으로 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", packageName, null)
                                            }

                                        bindingMbr.externalStorageReadPermissionSwitch.isEnabled =
                                            true
                                        bindingMbr.externalStorageReadPermissionSwitch.isClickable =
                                            true
                                        resultLauncherCallbackMbr = {}
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 뷰 상태 되돌리기
                                        bindingMbr.externalStorageReadPermissionSwitch.isChecked =
                                            false

                                        bindingMbr.externalStorageReadPermissionSwitch.isEnabled =
                                            true
                                        bindingMbr.externalStorageReadPermissionSwitch.isClickable =
                                            true
                                    },
                                    onCanceled = {}
                                )
                        }
                    }

                    // 권한 요청
                    permissionRequestMbr.launch(permissionArray)
                } else {
                    // 체크 해제시
                    // 권한 해제 다이얼로그
                    shownDialogInfoVOMbr =
                        DialogBinaryChoose.DialogInfoVO(
                            false,
                            "권한 해제",
                            "권한 해제를 위해 설정 화면으로 이동하시겠습니까?",
                            null,
                            null,
                            onPosBtnClicked = {
                                shownDialogInfoVOMbr = null

                                // 권한 설정 페이지 이동
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri

                                bindingMbr.externalStorageReadPermissionSwitch.isEnabled = true
                                bindingMbr.externalStorageReadPermissionSwitch.isClickable = true
                                resultLauncherCallbackMbr = {}
                                resultLauncherMbr.launch(intent)
                            },
                            onNegBtnClicked = {
                                shownDialogInfoVOMbr = null

                                bindingMbr.externalStorageReadPermissionSwitch.isChecked = true

                                bindingMbr.externalStorageReadPermissionSwitch.isEnabled = true
                                bindingMbr.externalStorageReadPermissionSwitch.isClickable = true
                            },
                            onCanceled = {
                                // 취소 불가
                            }
                        )
                }
            }
        }

        // 카메라 사용 권한
        bindingMbr.cameraPermissionSwitch.setOnClickListener {
            bindingMbr.cameraPermissionSwitch.isEnabled = false
            bindingMbr.cameraPermissionSwitch.isClickable = false

            if (bindingMbr.cameraPermissionSwitch.isChecked) { // 체크시
                // 권한 요청
                // 권한 요청 콜백
                val permissionArray: Array<String> =
                    arrayOf(Manifest.permission.CAMERA)
                permissionRequestCallbackMbr = { permissions ->
                    // (거부된 권한 리스트)
                    var isPermissionAllGranted = true // 모든 권한 승인여부
                    var neverAskAgain = false // 다시묻지 않기 체크 여부
                    for (permission in permissionArray) {
                        if (!permissions[permission]!!) { // 필수 권한 거부
                            // 모든 권한 승인여부 플래그를 변경하고 break
                            isPermissionAllGranted = false
                            neverAskAgain = !shouldShowRequestPermissionRationale(permission)
                            break
                        }
                    }

                    if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황

                        bindingMbr.cameraPermissionSwitch.isEnabled = true
                        bindingMbr.cameraPermissionSwitch.isClickable = true
                    } else if (!neverAskAgain) { // 단순 거부
                        // 뷰 상태 되돌리기
                        bindingMbr.cameraPermissionSwitch.isChecked = false

                        bindingMbr.cameraPermissionSwitch.isEnabled = true
                        bindingMbr.cameraPermissionSwitch.isClickable = true
                    } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                        shownDialogInfoVOMbr =
                            DialogBinaryChoose.DialogInfoVO(
                                false,
                                "권한 요청",
                                "권한 설정 화면으로 이동하시겠습니까?",
                                null,
                                null,
                                onPosBtnClicked = {
                                    shownDialogInfoVOMbr = null

                                    // 권한 설정 화면으로 이동
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                        }

                                    bindingMbr.cameraPermissionSwitch.isEnabled = true
                                    bindingMbr.cameraPermissionSwitch.isClickable = true
                                    resultLauncherCallbackMbr = {}
                                    resultLauncherMbr.launch(intent)
                                },
                                onNegBtnClicked = {
                                    shownDialogInfoVOMbr = null

                                    // 뷰 상태 되돌리기
                                    bindingMbr.cameraPermissionSwitch.isChecked = false

                                    bindingMbr.cameraPermissionSwitch.isEnabled = true
                                    bindingMbr.cameraPermissionSwitch.isClickable = true
                                },
                                onCanceled = {}
                            )
                    }
                }

                // 권한 요청
                permissionRequestMbr.launch(permissionArray)
            } else {
                // 체크 해제시
                // 권한 해제 다이얼로그
                shownDialogInfoVOMbr =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "권한 해제",
                        "권한 해제를 위해 설정 화면으로 이동하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = {
                            shownDialogInfoVOMbr = null

                            // 권한 설정 페이지 이동
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri

                            bindingMbr.cameraPermissionSwitch.isEnabled = true
                            bindingMbr.cameraPermissionSwitch.isClickable = true
                            resultLauncherCallbackMbr = {}
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            shownDialogInfoVOMbr = null

                            bindingMbr.cameraPermissionSwitch.isChecked = true

                            bindingMbr.cameraPermissionSwitch.isEnabled = true
                            bindingMbr.cameraPermissionSwitch.isClickable = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            }
        }

        // 오디오 녹음 권한
        bindingMbr.audioRecordPermissionSwitch.setOnClickListener {
            bindingMbr.audioRecordPermissionSwitch.isEnabled = false
            bindingMbr.audioRecordPermissionSwitch.isClickable = false

            if (bindingMbr.audioRecordPermissionSwitch.isChecked) { // 체크시
                // 권한 요청
                // 권한 요청 콜백
                val permissionArray: Array<String> =
                    arrayOf(Manifest.permission.RECORD_AUDIO)
                permissionRequestCallbackMbr = { permissions ->
                    // (거부된 권한 리스트)
                    var isPermissionAllGranted = true // 모든 권한 승인여부
                    var neverAskAgain = false // 다시묻지 않기 체크 여부
                    for (permission in permissionArray) {
                        if (!permissions[permission]!!) { // 필수 권한 거부
                            // 모든 권한 승인여부 플래그를 변경하고 break
                            isPermissionAllGranted = false
                            neverAskAgain = !shouldShowRequestPermissionRationale(permission)
                            break
                        }
                    }

                    if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황

                        bindingMbr.audioRecordPermissionSwitch.isEnabled = true
                        bindingMbr.audioRecordPermissionSwitch.isClickable = true
                    } else if (!neverAskAgain) { // 단순 거부
                        // 뷰 상태 되돌리기
                        bindingMbr.audioRecordPermissionSwitch.isChecked = false

                        bindingMbr.audioRecordPermissionSwitch.isEnabled = true
                        bindingMbr.audioRecordPermissionSwitch.isClickable = true
                    } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                        shownDialogInfoVOMbr =
                            DialogBinaryChoose.DialogInfoVO(
                                false,
                                "권한 요청",
                                "권한 설정 화면으로 이동하시겠습니까?",
                                null,
                                null,
                                onPosBtnClicked = {
                                    shownDialogInfoVOMbr = null

                                    // 권한 설정 화면으로 이동
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                        }

                                    bindingMbr.audioRecordPermissionSwitch.isEnabled = true
                                    bindingMbr.audioRecordPermissionSwitch.isClickable = true
                                    resultLauncherCallbackMbr = {}
                                    resultLauncherMbr.launch(intent)
                                },
                                onNegBtnClicked = {
                                    shownDialogInfoVOMbr = null

                                    // 뷰 상태 되돌리기
                                    bindingMbr.audioRecordPermissionSwitch.isChecked = false

                                    bindingMbr.audioRecordPermissionSwitch.isEnabled = true
                                    bindingMbr.audioRecordPermissionSwitch.isClickable = true
                                },
                                onCanceled = {}
                            )
                    }
                }

                // 권한 요청
                permissionRequestMbr.launch(permissionArray)
            } else {
                // 체크 해제시
                // 권한 해제 다이얼로그
                shownDialogInfoVOMbr =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "권한 해제",
                        "권한 해제를 위해 설정 화면으로 이동하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = {
                            shownDialogInfoVOMbr = null

                            // 권한 설정 페이지 이동
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri

                            bindingMbr.audioRecordPermissionSwitch.isEnabled = true
                            bindingMbr.audioRecordPermissionSwitch.isClickable = true
                            resultLauncherCallbackMbr = {}
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            shownDialogInfoVOMbr = null

                            bindingMbr.audioRecordPermissionSwitch.isChecked = true

                            bindingMbr.audioRecordPermissionSwitch.isEnabled = true
                            bindingMbr.audioRecordPermissionSwitch.isClickable = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            }
        }

        // 위치 정보 조회 권한
        bindingMbr.locationPermissionSwitch.setOnClickListener {
            bindingMbr.locationPermissionSwitch.isEnabled = false
            bindingMbr.locationPermissionSwitch.isClickable = false

            if (bindingMbr.locationPermissionSwitch.isChecked) { // 체크시
                // 권한 요청
                val permissionArray: Array<String> =
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                permissionRequestCallbackMbr = { permissions ->
                    // 위치 권한
                    val isFineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]!!
                    val isCoarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION]!!

                    val neverAskAgain =
                        !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

                    if (isFineGranted || isCoarseGranted) { // 권한 승인

                        when {
                            isFineGranted -> {// 위치 권한 fine 을 승인하면 자동으로 모두 승인한 것과 같음
                                bindingMbr.locationPermissionSwitch.isChecked = true
                                bindingMbr.locationPermissionDetailSwitch.isChecked =
                                    true
                                bindingMbr.locationPermissionDetailContainer.visibility =
                                    View.GONE

                                bindingMbr.locationPermissionSwitch.isEnabled = true
                                bindingMbr.locationPermissionSwitch.isClickable = true
                            }
                            isCoarseGranted -> {// 위치 권한 coarse 만 승인
                                // 정확도 보정 설정 보여주기
                                bindingMbr.locationPermissionSwitch.isChecked = true
                                bindingMbr.locationPermissionDetailSwitch.isChecked =
                                    false
                                bindingMbr.locationPermissionDetailContainer.visibility =
                                    View.VISIBLE

                                bindingMbr.locationPermissionSwitch.isEnabled = true
                                bindingMbr.locationPermissionSwitch.isClickable = true
                            }
                        }
                    } else { // 권한 거부
                        if (!neverAskAgain) {
                            // 단순 거부
                            bindingMbr.locationPermissionSwitch.isChecked = false
                            bindingMbr.locationPermissionDetailSwitch.isChecked = false
                            bindingMbr.locationPermissionDetailContainer.visibility = View.GONE

                            bindingMbr.locationPermissionSwitch.isEnabled = true
                            bindingMbr.locationPermissionSwitch.isClickable = true
                        } else {
                            // 다시 묻지 않기 선택
                            shownDialogInfoVOMbr =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 권한 설정 페이지 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", packageName, null)
                                        intent.data = uri

                                        bindingMbr.locationPermissionSwitch.isEnabled = true
                                        bindingMbr.locationPermissionSwitch.isClickable = true
                                        resultLauncherCallbackMbr = {}
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        bindingMbr.locationPermissionSwitch.isChecked = false
                                        bindingMbr.locationPermissionDetailSwitch.isChecked = false
                                        bindingMbr.locationPermissionDetailContainer.visibility =
                                            View.GONE

                                        bindingMbr.locationPermissionSwitch.isEnabled = true
                                        bindingMbr.locationPermissionSwitch.isClickable = true
                                    },
                                    onCanceled = {
                                        // 취소 불가
                                    }
                                )
                        }
                    }
                }
                permissionRequestMbr.launch(permissionArray)
            } else {
                // 체크 해제시
                // 권한 해제 다이얼로그
                shownDialogInfoVOMbr =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "권한 요청",
                        "권한 설정 화면으로 이동하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = {
                            shownDialogInfoVOMbr = null

                            // 권한 설정 페이지 이동
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri

                            bindingMbr.locationPermissionSwitch.isEnabled = true
                            bindingMbr.locationPermissionSwitch.isClickable = true
                            resultLauncherCallbackMbr = {}
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            shownDialogInfoVOMbr = null

                            bindingMbr.locationPermissionSwitch.isChecked = true

                            bindingMbr.locationPermissionSwitch.isEnabled = true
                            bindingMbr.locationPermissionSwitch.isClickable = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            }
        }

        // 위치 정보 조회 권한 (정확)
        bindingMbr.locationPermissionDetailSwitch.setOnClickListener {
            bindingMbr.locationPermissionDetailSwitch.isEnabled = false
            bindingMbr.locationPermissionDetailSwitch.isClickable = false

            if (bindingMbr.locationPermissionDetailSwitch.isChecked) { // 체크시
                // 권한 요청
                val permissionArray: Array<String> =
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionRequestCallbackMbr = { permissions ->
                    // 위치 권한 정확성 향상
                    val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]!!
                    val neverAskAgain =
                        !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

                    if (isGranted) { // 권한 승인
                        // 향상 메뉴 숨기기
                        bindingMbr.locationPermissionDetailContainer.visibility =
                            View.GONE

                        bindingMbr.locationPermissionDetailSwitch.isEnabled = true
                        bindingMbr.locationPermissionDetailSwitch.isClickable = true
                    } else { // 권한 거부
                        if (!neverAskAgain) {
                            // 단순 거부

                            // 뷰 상태 되돌리기
                            bindingMbr.locationPermissionDetailSwitch.isChecked = false
                            bindingMbr.locationPermissionDetailContainer.visibility = View.VISIBLE

                            bindingMbr.locationPermissionDetailSwitch.isEnabled = true
                            bindingMbr.locationPermissionDetailSwitch.isClickable = true
                        } else {
                            // 다시 묻지 않기 선택
                            shownDialogInfoVOMbr =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 권한 설정 페이지 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", packageName, null)
                                        intent.data = uri

                                        bindingMbr.locationPermissionDetailSwitch.isEnabled = true
                                        bindingMbr.locationPermissionDetailSwitch.isClickable = true
                                        resultLauncherCallbackMbr = {}
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 뷰 상태 되돌리기
                                        bindingMbr.locationPermissionDetailSwitch.isChecked = false
                                        bindingMbr.locationPermissionDetailContainer.visibility =
                                            View.VISIBLE

                                        bindingMbr.locationPermissionDetailSwitch.isEnabled = true
                                        bindingMbr.locationPermissionDetailSwitch.isClickable = true
                                    },
                                    onCanceled = {
                                        // 취소 불가
                                    }
                                )
                        }
                    }
                }
                permissionRequestMbr.launch(permissionArray)
            } else {
                // 체크 해제시
                // 정확도 향상시엔 항목을 숨길 것이기에 해제 불가
                bindingMbr.locationPermissionDetailSwitch.isEnabled = true
                bindingMbr.locationPermissionDetailSwitch.isClickable = true
            }
        }

        // 시스템 설정 변경 권한
        bindingMbr.writeSettingPermissionSwitch.setOnClickListener {
            bindingMbr.writeSettingPermissionSwitch.isEnabled = false
            bindingMbr.writeSettingPermissionSwitch.isClickable = false

            if (bindingMbr.writeSettingPermissionSwitch.isChecked) { // 체크시
                shownDialogInfoVOMbr =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "권한 요청",
                        "권한 설정 화면으로 이동하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = {
                            shownDialogInfoVOMbr = null

                            // 권한 설정 페이지 이동
                            val intent =
                                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = Uri.parse("package:" + this.packageName)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                            bindingMbr.writeSettingPermissionSwitch.isEnabled = true
                            bindingMbr.writeSettingPermissionSwitch.isClickable = true
                            resultLauncherCallbackMbr = {}
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            shownDialogInfoVOMbr = null

                            // 뷰 상태 되돌리기
                            bindingMbr.writeSettingPermissionSwitch.isChecked = false

                            bindingMbr.writeSettingPermissionSwitch.isEnabled = true
                            bindingMbr.writeSettingPermissionSwitch.isClickable = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            } else {
                shownDialogInfoVOMbr =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "권한 요청",
                        "권한 설정 화면으로 이동하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = {
                            shownDialogInfoVOMbr = null

                            bindingMbr.writeSettingPermissionSwitch.isEnabled = true
                            bindingMbr.writeSettingPermissionSwitch.isClickable = true
                            // 권한 설정 페이지 이동
                            // ACTION_MANAGE_WRITE_SETTINGS 는 ActivityResultLauncher 가 통하지 않으므로,
                            // onResume 시에 체크해서 판단하도록
                            val intent =
                                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = Uri.parse("package:" + this.packageName)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)

                        },
                        onNegBtnClicked = {
                            shownDialogInfoVOMbr = null

                            // 뷰 상태 되돌리기
                            bindingMbr.writeSettingPermissionSwitch.isChecked = true

                            bindingMbr.writeSettingPermissionSwitch.isEnabled = true
                            bindingMbr.writeSettingPermissionSwitch.isClickable = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            }
        }

        // API 레벨 30이상 외부 저장소 읽기 쓰기 권한
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bindingMbr.manageAllFilesPermissionContainer.visibility = View.VISIBLE

            bindingMbr.manageAllFilesPermissionSwitch.setOnClickListener {
                bindingMbr.manageAllFilesPermissionSwitch.isEnabled = false
                bindingMbr.manageAllFilesPermissionSwitch.isClickable = false

                if (bindingMbr.manageAllFilesPermissionSwitch.isChecked) { // 체크시
                    shownDialogInfoVOMbr =
                        DialogBinaryChoose.DialogInfoVO(
                            false,
                            "권한 요청",
                            "권한 설정 화면으로 이동하시겠습니까?",
                            null,
                            null,
                            onPosBtnClicked = {
                                shownDialogInfoVOMbr = null

                                // 권한 설정 페이지 이동
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                                )

                                bindingMbr.manageAllFilesPermissionSwitch.isEnabled = true
                                bindingMbr.manageAllFilesPermissionSwitch.isClickable = true
                                resultLauncherCallbackMbr = {}
                                resultLauncherMbr.launch(intent)
                            },
                            onNegBtnClicked = {
                                shownDialogInfoVOMbr = null

                                // 뷰 상태 되돌리기
                                bindingMbr.manageAllFilesPermissionSwitch.isChecked = false

                                bindingMbr.manageAllFilesPermissionSwitch.isEnabled = true
                                bindingMbr.manageAllFilesPermissionSwitch.isClickable = true
                            },
                            onCanceled = {
                                // 취소 불가
                            }
                        )
                } else {
                    shownDialogInfoVOMbr =
                        DialogBinaryChoose.DialogInfoVO(
                            false,
                            "권한 요청",
                            "권한 설정 화면으로 이동하시겠습니까?",
                            null,
                            null,
                            onPosBtnClicked = {
                                shownDialogInfoVOMbr = null

                                bindingMbr.manageAllFilesPermissionSwitch.isEnabled = true
                                bindingMbr.manageAllFilesPermissionSwitch.isClickable = true
                                // 권한 설정 페이지 이동
                                // onResume 시에 체크해서 판단하도록
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                                )
                                resultLauncherCallbackMbr = {}
                                resultLauncherMbr.launch(intent)

                            },
                            onNegBtnClicked = {
                                shownDialogInfoVOMbr = null

                                // 뷰 상태 되돌리기
                                bindingMbr.manageAllFilesPermissionSwitch.isChecked = true

                                bindingMbr.manageAllFilesPermissionSwitch.isEnabled = true
                                bindingMbr.manageAllFilesPermissionSwitch.isClickable = true
                            },
                            onCanceled = {
                                // 취소 불가
                            }
                        )
                }
            }
        } else {
            bindingMbr.manageAllFilesPermissionContainer.visibility = View.GONE
            bindingMbr.manageAllFilesPermissionSwitch.setOnClickListener { }
        }


        // 알람 표시 권한
        bindingMbr.notificationPermissionSwitch.setOnClickListener {
            bindingMbr.notificationPermissionSwitch.isEnabled = false
            bindingMbr.notificationPermissionSwitch.isClickable = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (bindingMbr.notificationPermissionSwitch.isChecked) { // 체크시
                    // 권한 요청
                    // 권한 요청 콜백
                    val permissionArray: Array<String> =
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                    permissionRequestCallbackMbr = { permissions ->
                        // (거부된 권한 리스트)
                        var isPermissionAllGranted = true // 모든 권한 승인여부
                        var neverAskAgain = false // 다시묻지 않기 체크 여부
                        for (permission in permissionArray) {
                            if (!permissions[permission]!!) { // 필수 권한 거부
                                // 모든 권한 승인여부 플래그를 변경하고 break
                                isPermissionAllGranted = false
                                neverAskAgain = !shouldShowRequestPermissionRationale(permission)
                                break
                            }
                        }

                        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황

                            bindingMbr.notificationPermissionSwitch.isEnabled = true
                            bindingMbr.notificationPermissionSwitch.isClickable = true
                        } else if (!neverAskAgain) { // 단순 거부
                            // 뷰 상태 되돌리기
                            bindingMbr.notificationPermissionSwitch.isChecked = false

                            bindingMbr.notificationPermissionSwitch.isEnabled = true
                            bindingMbr.notificationPermissionSwitch.isClickable = true
                        } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                            shownDialogInfoVOMbr =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 권한 설정 화면으로 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", packageName, null)
                                            }

                                        bindingMbr.notificationPermissionSwitch.isEnabled = true
                                        bindingMbr.notificationPermissionSwitch.isClickable = true
                                        resultLauncherCallbackMbr = {}
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 뷰 상태 되돌리기
                                        bindingMbr.notificationPermissionSwitch.isChecked = false

                                        bindingMbr.notificationPermissionSwitch.isEnabled = true
                                        bindingMbr.notificationPermissionSwitch.isClickable = true
                                    },
                                    onCanceled = {}
                                )
                        }
                    }

                    // 권한 요청
                    permissionRequestMbr.launch(permissionArray)
                } else {
                    // 체크 해제시
                    // 권한 해제 다이얼로그
                    shownDialogInfoVOMbr =
                        DialogBinaryChoose.DialogInfoVO(
                            false,
                            "권한 해제",
                            "권한 해제를 위해 설정 화면으로 이동하시겠습니까?",
                            null,
                            null,
                            onPosBtnClicked = {
                                shownDialogInfoVOMbr = null

                                // 권한 설정 페이지 이동
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri

                                bindingMbr.notificationPermissionSwitch.isEnabled = true
                                bindingMbr.notificationPermissionSwitch.isClickable = true
                                resultLauncherCallbackMbr = {}
                                resultLauncherMbr.launch(intent)
                            },
                            onNegBtnClicked = {
                                shownDialogInfoVOMbr = null

                                bindingMbr.notificationPermissionSwitch.isChecked = true

                                bindingMbr.notificationPermissionSwitch.isEnabled = true
                                bindingMbr.notificationPermissionSwitch.isClickable = true
                            },
                            onCanceled = {
                                // 취소 불가
                            }
                        )
                }
            } else {
                if (bindingMbr.notificationPermissionSwitch.isChecked) { // 체크시
                    // 권한 승인 여부 다이얼로그
                    shownDialogInfoVOMbr = DialogBinaryChoose.DialogInfoVO(
                        false,
                        "알림 수신 권한",
                        "알림을 수신하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = { // 권한 승인
                            shownDialogInfoVOMbr = null

                            // 변경 상태저장
                            customPermissionSpwMbr.pushPermissionGranted = true

                            bindingMbr.notificationPermissionSwitch.isEnabled = true
                            bindingMbr.notificationPermissionSwitch.isClickable = true
                        },
                        onNegBtnClicked = {
                            shownDialogInfoVOMbr = null

                            // 뷰 상태 되돌리기
                            bindingMbr.notificationPermissionSwitch.isChecked = false

                            bindingMbr.notificationPermissionSwitch.isEnabled = true
                            bindingMbr.notificationPermissionSwitch.isClickable = true
                        },
                        onCanceled = {}
                    )
                } else {
                    // 체크 해제시
                    // 권한 해제 다이얼로그
                    shownDialogInfoVOMbr =
                        DialogBinaryChoose.DialogInfoVO(
                            false,
                            "알림 수신 권한 해제",
                            "알림 수신 권한을 해제하시겠습니까?",
                            null,
                            null,
                            onPosBtnClicked = {
                                shownDialogInfoVOMbr = null

                                // 변경 상태저장
                                customPermissionSpwMbr.pushPermissionGranted = false

                                bindingMbr.notificationPermissionSwitch.isEnabled = true
                                bindingMbr.notificationPermissionSwitch.isClickable = true
                            },
                            onNegBtnClicked = {
                                shownDialogInfoVOMbr = null

                                // 뷰 상태 되돌리기
                                bindingMbr.notificationPermissionSwitch.isChecked = true

                                bindingMbr.notificationPermissionSwitch.isEnabled = true
                                bindingMbr.notificationPermissionSwitch.isClickable = true
                            },
                            onCanceled = {}
                        )
                }
            }
        }

    }

    // (액티비티 진입 권한이 클리어 된 시점)
    // : 실질적인 액티비티 로직 실행구역
    private var doItAlreadyMbr = false
    private var currentUserUidMbr: String? = null // 유저 식별가능 정보 - null 이라면 비회원
    private fun allPermissionsGranted() {
        if (!doItAlreadyMbr) {
            // (권한이 충족된 onCreate)
            doItAlreadyMbr = true

            // (초기 데이터 수집)
            currentUserUidMbr = currentLoginSessionInfoSpwMbr.userUid
            refreshWholeScreenData(onComplete = {})

        } else {
            // (onResume - (권한이 충족된 onCreate))

            // (유저별 데이터 갱신)
            // : 유저 정보가 갱신된 상태에서 다시 현 액티비티로 복귀하면 자동으로 데이터를 다시 갱신합니다.
            val userUid = currentLoginSessionInfoSpwMbr.userUid
            if (userUid != currentUserUidMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                currentUserUidMbr = userUid

                // (데이터 수집)
                refreshWholeScreenData(onComplete = {})
            }

        }

        // (onResume)
        // 권한 스위치 정렬
        setSwitchView()
    }


    // (권한에 따라 스위치 반영)
    private fun setSwitchView() {
        // 외부 저장소 읽기 권한 설정 여부 반영
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bindingMbr.externalStorageReadImagesAndVideoPermissionSwitch.isChecked =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_MEDIA_VIDEO
                        ) == PackageManager.PERMISSION_GRANTED

            bindingMbr.externalStorageReadAudioPermissionSwitch.isChecked =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

        } else {
            bindingMbr.externalStorageReadPermissionSwitch.isChecked =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
        }

        // 카메라 접근 권한 설정 여부 반영
        bindingMbr.cameraPermissionSwitch.isChecked =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        // 오디오 녹음 권한 설정 여부 반영
        bindingMbr.audioRecordPermissionSwitch.isChecked =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        // 상세 위치 정보 권한 설정 여부 반영
        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 대략 위치 정보 권한 설정 여부 반영
        val coarseLocationGranted =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // 두 위치 정보의 상호 상태에 따른 뷰 처리
        when {
            fineLocationGranted -> {// 위치 권한 fine 을 승인하면 자동으로 모두 승인한 것과 같음
                bindingMbr.locationPermissionSwitch.isChecked = true
                bindingMbr.locationPermissionDetailSwitch.isChecked = true
                bindingMbr.locationPermissionDetailContainer.visibility = View.GONE
            }
            coarseLocationGranted -> {// 위치 권한 coarse 만 승인
                // 정확도 보정 설정 보여주기
                bindingMbr.locationPermissionSwitch.isChecked = true
                bindingMbr.locationPermissionDetailSwitch.isChecked = false
                bindingMbr.locationPermissionDetailContainer.visibility = View.VISIBLE
            }
            else -> {// 위치 권한 모두 거부
                bindingMbr.locationPermissionSwitch.isChecked = false
                bindingMbr.locationPermissionDetailSwitch.isChecked = false
                bindingMbr.locationPermissionDetailContainer.visibility = View.GONE
            }
        }

        bindingMbr.writeSettingPermissionSwitch.isChecked = Settings.System.canWrite(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bindingMbr.manageAllFilesPermissionSwitch.isChecked =
                Environment.isExternalStorageManager()
        }

        // 알람 표시 권한 설정 여부 반영
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bindingMbr.notificationPermissionSwitch.isChecked =
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        } else {
            bindingMbr.notificationPermissionSwitch.isChecked =
                customPermissionSpwMbr.pushPermissionGranted

        }
    }

    // 화면 데이터 갱신관련 세마포어
    private val screenDataSemaphoreMbr = Semaphore(1)

    // (화면 구성용 데이터를 가져오기)
    // : 네트워크 등 레포지토리에서 데이터를 가져오고 이를 뷰에 반영
    //     onComplete = 네트워크 실패든 성공이든 데이터 요청 후 응답을 받아와 해당 상태에 따라 스크린 뷰 처리를 완료한 시점
    //     'c숫자' 로 표기된 부분은 원하는대로 커스텀
    private fun refreshWholeScreenData(onComplete: () -> Unit) {
        executorServiceMbr.execute {
            screenDataSemaphoreMbr.acquire()

            runOnUiThread {
                // (c1. 리스트 초기화)

                // (c2. 로더 추가)
            }

            // (스레드 합류 객체 생성)
            // : 헤더, 푸터, 아이템 리스트의 각 데이터를 비동기적으로 요청했을 때, 그 합류용으로 사용되는 객체
            //     numberOfThreadsBeingJoinedMbr 에 비동기 처리 개수를 적고,
            //     각 처리 완료시마다 threadComplete 를 호출하면 됨
            val threadConfluenceObj =
                ThreadConfluenceObj(
                    3,
                    onComplete = {
                        screenDataSemaphoreMbr.release()
                        onComplete()
                    }
                )

            // (정보 요청 콜백)
            // 아이템 리스트
            // : statusCode
            //     서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 itemList 는 null
            val getItemListOnComplete: (statusCode: Int, itemList: ArrayList<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>?) -> Unit =
                { statusCode, itemList ->
                    runOnUiThread {
                        // (c3. 로더 제거)
                    }

                    when (statusCode) {
                        1 -> { // 완료
                            if (itemList!!.isEmpty()) { // 받아온 리스트가 비어있을 때
                                // (c4. 빈 리스트 처리)

                                threadConfluenceObj.threadComplete()
                            } else {
                                runOnUiThread {
                                    // (c5. 받아온 아이템 추가)

                                    // (c6. 스크롤을 가장 앞으로 이동)
                                }

                                threadConfluenceObj.threadComplete()
                            }
                        }
                        -1 -> { // 네트워크 에러
                            // (c7. 네트워크 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                        else -> { // 그외 서버 에러그외 서버 에러
                            // (c8. 그외 서버 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                    }
                }

            // 헤더 아이템
            // : statusCode
            //     서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 item 은 null
            val getHeaderItemOnComplete: (statusCode: Int, item: AbstractProwdRecyclerViewAdapter.AdapterHeaderAbstractVO?) -> Unit =
                { statusCode, item ->
                    runOnUiThread {
                        // (c9. 로더 제거)
                    }

                    when (statusCode) {
                        1 -> { // 완료
                            runOnUiThread {
                                // (c10. 받아온 아이템 추가)
                            }

                            threadConfluenceObj.threadComplete()
                        }
                        -1 -> { // 네트워크 에러
                            // (c11. 네트워크 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                        else -> { // 그외 서버 에러
                            // (c12. 그외 서버 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                    }
                }

            // 푸터 아이템
            // : statusCode
            //     서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 item 은 null
            val getFooterItemOnComplete: (statusCode: Int, item: AbstractProwdRecyclerViewAdapter.AdapterFooterAbstractVO?) -> Unit =
                { statusCode, item ->
                    runOnUiThread {
                        // (c13. 로더 제거)
                    }

                    when (statusCode) {
                        1 -> {// 완료
                            runOnUiThread {
                                // (c14. 받아온 아이템 추가)
                            }

                            threadConfluenceObj.threadComplete()
                        }
                        -1 -> { // 네트워크 에러
                            // (c15. 네트워크 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                        else -> { // 그외 서버 에러
                            // (c16. 그외 서버 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                    }
                }

            // (네트워크 요청)
            // (c17. 아이템 리스트 가져오기)
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                getItemListOnComplete(-2, null)
            }

            // (c18. 헤더 데이터 가져오기)
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                getHeaderItemOnComplete(-2, null)
            }

            // (c19. 푸터 데이터 가져오기)
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                getFooterItemOnComplete(-2, null)
            }

            // (c20. 그외 스크린 데이터 가져오기)

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // (클래스 비휘발 저장 객체)
    class ActivityPermissionSampleSpw(application: Application) {
        // <멤버 변수 공간>
        // SharedPreference 접근 객체
        private val spMbr = application.getSharedPreferences(
            "ActivityPermissionSampleSpw",
            Context.MODE_PRIVATE
        )

//        var testData: String?
//            get() {
//                return spMbr.getString(
//                    "testData",
//                    null
//                )
//            }
//            set(value) {
//                with(spMbr.edit()) {
//                    putString(
//                        "testData",
//                        value
//                    )
//                    apply()
//                }
//            }


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>

    }

    // (액티비티 내 사용 어뎁터 모음)
    // : 액티비티 내 사용할 어뎁터가 있다면 본문에 클래스 추가 후 인자로 해당 클래스의 인스턴스를 받도록 하기
    class ActivityPermissionSampleAdapterSet {
        // 어뎁터 #1

    }
}