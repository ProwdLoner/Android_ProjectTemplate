package com.example.prowd_android_template.activity_set.activity_camera_sample_list

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.activity_set.activity_basic_camera2_api_sample.ActivityBasicCamera2ApiSample
import com.example.prowd_android_template.activity_set.activity_system_camera_photo_sample.ActivitySystemCameraPhotoSample
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityCameraSampleListBinding

class ActivityCameraSampleList : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityCameraSampleListBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityCameraSampleListViewModel

    // (권한 요청 객체)
    private lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    private var permissionRequestCallbackMbr: (((MutableMap<String, Boolean>) -> Unit))? = null

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    private var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    private var confirmDialogMbr: DialogConfirm? = null

    // (ActivityResultLauncher 객체)
    // 권한 설정 화면 이동 복귀 객체
    private lateinit var permissionResultLauncherMbr: ActivityResultLauncher<Intent>
    private var permissionResultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityCameraSampleListBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 객체 생성)
        createMemberObjects()
        // 뷰모델 저장 객체 생성 = 뷰모델 내에 저장되어 destroy 까지 쭉 유지되는 데이터 초기화
        createViewModelDataObjects()

        // (초기 뷰 설정)
        viewSetting()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()
    }

    override fun onResume() {
        super.onResume()

        // (데이터 갱신 시점 적용)
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 화면 회전이 아닐 때
            val sessionToken = viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken

            if (viewModelMbr.isDataFirstLoadingMbr || // 데이터 최초 로딩 시점일 때 혹은,
                sessionToken != viewModelMbr.currentUserSessionTokenMbr // 액티비티 유저와 세션 유저가 다를 때
            ) {
                // 진입 플래그 변경
                viewModelMbr.isDataFirstLoadingMbr = false
                viewModelMbr.currentUserSessionTokenMbr = sessionToken

                //  데이터 로딩
            }
        }

    }

    override fun onStop() {
        // 설정 변경(화면회전)을 했는지 여부를 초기화
        viewModelMbr.isChangingConfigurationsMbr = false
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // 설정 변경(화면회전)을 했는지 여부를 반영
        viewModelMbr.isChangingConfigurationsMbr = true
    }

    override fun onDestroy() {
        // 다이얼로그 객체 해소
        progressLoadingDialogMbr?.dismiss()
        binaryChooseDialogMbr?.dismiss()
        confirmDialogMbr?.dismiss()

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ActivityCameraSampleListViewModel::class.java]

        // 권한 요청 객체 생성
        permissionRequestMbr =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissionRequestCallbackMbr?.let { it1 -> it1(it) }
                permissionRequestCallbackMbr = null
            }

        // 앱 권한 설정 ActivityResultLauncher 생성
        permissionResultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            permissionResultLauncherCallbackMbr?.let { it1 -> it1(it) }
            permissionResultLauncherCallbackMbr = null
        }
    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr =
                viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 시스템 카메라 샘플 이동 버튼
        bindingMbr.goToSystemCameraPhotoSampleBtn.setOnClickListener {
            // 시스템 카메라 액티비티 필요 권한
            permissionRequestCallbackMbr = { permissions ->
                // 외부 저장소 읽기 권한
                val isGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE]!!
                val neverAskAgain =
                    !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)

                if (isGranted) { // 권한 승인
                    // 액티비티 이동
                    val intent =
                        Intent(
                            this,
                            ActivitySystemCameraPhotoSample::class.java
                        )
                    startActivity(intent)
                } else { // 권한 거부
                    if (!neverAskAgain) {
                        // 단순 거부
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "권한 요청",
                                "해당 서비스를 이용하기 위해선\n외부 저장장치 접근 권한이 필요합니다.",
                                null,
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                                }
                            )
                    } else {
                        // 다시 묻지 않기 선택
                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                            DialogBinaryChoose.DialogInfoVO(
                                true,
                                "권한 요청",
                                "해당 서비스를 이용하기 위해선\n" +
                                        "외부 저장장치 접근 권한이 필요합니다.\n" +
                                        "권한 설정 화면으로 이동하시겠습니까?",
                                null,
                                null,
                                onPosBtnClicked = {
                                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                    // 권한 설정 화면으로 이동
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.data = Uri.fromParts("package", packageName, null)
                                    permissionResultLauncherCallbackMbr = {
                                        // 권한 확인
                                        if (ActivityCompat.checkSelfPermission(
                                                this,
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) { // 권한 승인
                                            // 액티비티 이동
                                            val goToIntent =
                                                Intent(
                                                    this,
                                                    ActivitySystemCameraPhotoSample::class.java
                                                )
                                            startActivity(goToIntent)
                                        } else { // 권한 비승인
                                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                DialogConfirm.DialogInfoVO(
                                                    true,
                                                    "권한 요청",
                                                    "해당 서비스를 이용하기 위해선\n외부 저장장치 접근 권한이 필요합니다.",
                                                    null,
                                                    onCheckBtnClicked = {

                                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                            null
                                                    },
                                                    onCanceled = {

                                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                            null
                                                    }
                                                )
                                        }
                                    }
                                    permissionResultLauncherMbr.launch(
                                        intent
                                    )
                                },
                                onNegBtnClicked = {
                                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        DialogConfirm.DialogInfoVO(
                                            true,
                                            "권한 요청",
                                            "해당 서비스를 이용하기 위해선\n외부 저장장치 접근 권한이 필요합니다.",
                                            null,
                                            onCheckBtnClicked = {

                                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                    null
                                            },
                                            onCanceled = {

                                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                    null
                                            }
                                        )
                                },
                                onCanceled = {
                                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        DialogConfirm.DialogInfoVO(
                                            true,
                                            "권한 요청",
                                            "해당 서비스를 이용하기 위해선\n외부 저장장치 접근 권한이 필요합니다.",
                                            null,
                                            onCheckBtnClicked = {

                                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                    null
                                            },
                                            onCanceled = {

                                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                    null
                                            }
                                        )
                                }
                            )
                    }
                }
            }
            permissionRequestMbr.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }

        // 기본 Camera2 api 샘플 이동 버튼
        bindingMbr.goToBasicCamera2ApiSampleBtn.setOnClickListener {
            // 시스템 카메라 액티비티 필요 권한
            permissionRequestCallbackMbr = { permissions ->
                // 외부 저장소 읽기 권한
                val isGranted = permissions[Manifest.permission.CAMERA]!!
                val neverAskAgain =
                    !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

                if (isGranted) { // 권한 승인
                    // 액티비티 이동
                    val intent =
                        Intent(
                            this,
                            ActivityBasicCamera2ApiSample::class.java
                        )
                    startActivity(intent)
                } else { // 권한 거부
                    if (!neverAskAgain) {
                        // 단순 거부
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "권한 요청",
                                "해당 서비스를 이용하기 위해선\n카메라 디바이스 사용 권한이 필요합니다.",
                                null,
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                                }
                            )
                    } else {
                        // 다시 묻지 않기 선택
                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                            DialogBinaryChoose.DialogInfoVO(
                                true,
                                "권한 요청",
                                "해당 서비스를 이용하기 위해선\n" +
                                        "카메라 디바이스 사용 권한이 필요합니다.\n" +
                                        "권한 설정 화면으로 이동하시겠습니까?",
                                null,
                                null,
                                onPosBtnClicked = {
                                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                    // 권한 설정 화면으로 이동
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.data = Uri.fromParts("package", packageName, null)
                                    permissionResultLauncherCallbackMbr = {
                                        // 권한 확인
                                        if (ActivityCompat.checkSelfPermission(
                                                this,
                                                Manifest.permission.CAMERA
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) { // 권한 승인
                                            // 액티비티 이동
                                            val goToIntent =
                                                Intent(
                                                    this,
                                                    ActivitySystemCameraPhotoSample::class.java
                                                )
                                            startActivity(goToIntent)
                                        } else { // 권한 비승인
                                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                DialogConfirm.DialogInfoVO(
                                                    true,
                                                    "권한 요청",
                                                    "해당 서비스를 이용하기 위해선\n카메라 디바이스 사용 권한이 필요합니다.",
                                                    null,
                                                    onCheckBtnClicked = {

                                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                            null
                                                    },
                                                    onCanceled = {

                                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                            null
                                                    }
                                                )
                                        }
                                    }
                                    permissionResultLauncherMbr.launch(
                                        intent
                                    )
                                },
                                onNegBtnClicked = {
                                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        DialogConfirm.DialogInfoVO(
                                            true,
                                            "권한 요청",
                                            "해당 서비스를 이용하기 위해선\n카메라 디바이스 사용 권한이 필요합니다.",
                                            null,
                                            onCheckBtnClicked = {

                                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                    null
                                            },
                                            onCanceled = {

                                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                    null
                                            }
                                        )
                                },
                                onCanceled = {
                                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        DialogConfirm.DialogInfoVO(
                                            true,
                                            "권한 요청",
                                            "해당 서비스를 이용하기 위해선\n카메라 디바이스 사용 권한이 필요합니다.",
                                            null,
                                            onCheckBtnClicked = {

                                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                    null
                                            },
                                            onCanceled = {

                                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                    null
                                            }
                                        )
                                }
                            )
                    }
                }
            }
            permissionRequestMbr.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    // 라이브 데이터 설정
    private fun setLiveData() {
        // 로딩 다이얼로그 출력 플래그
        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                progressLoadingDialogMbr?.dismiss()

                progressLoadingDialogMbr = DialogProgressLoading(
                    this,
                    it
                )
                progressLoadingDialogMbr?.show()
            } else {
                progressLoadingDialogMbr?.dismiss()
                progressLoadingDialogMbr = null
            }
        }

        // 선택 다이얼로그 출력 플래그
        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                binaryChooseDialogMbr?.dismiss()

                binaryChooseDialogMbr = DialogBinaryChoose(
                    this,
                    it
                )
                binaryChooseDialogMbr?.show()
            } else {
                binaryChooseDialogMbr?.dismiss()
                binaryChooseDialogMbr = null
            }
        }

        // 확인 다이얼로그 출력 플래그
        viewModelMbr.confirmDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                confirmDialogMbr?.dismiss()

                confirmDialogMbr = DialogConfirm(
                    this,
                    it
                )
                confirmDialogMbr?.show()
            } else {
                confirmDialogMbr?.dismiss()
                confirmDialogMbr = null
            }
        }
    }
}