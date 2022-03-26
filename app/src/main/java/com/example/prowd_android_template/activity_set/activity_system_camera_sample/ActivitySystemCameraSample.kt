package com.example.prowd_android_template.activity_set.activity_system_camera_sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivitySystemCameraSampleBinding

class ActivitySystemCameraSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivitySystemCameraSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivitySystemCameraSampleViewModel

    // (권한 요청 객체)
    private lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    private var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    private var confirmDialogMbr: DialogConfirm? = null

    // (ResultLauncher 객체)
    // 카메라 권한 설정 액티비티 이동 객체
    private lateinit var goToCameraPermissionConfigResultLauncherMbr: ActivityResultLauncher<Intent>


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivitySystemCameraSampleBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 객체 생성)
        createMemberObjects()
        // 뷰모델 저장 객체 생성 = 뷰모델 내에 저장되어 destroy 까지 쭉 유지되는 데이터 초기화
        createViewModelDataObjects()
        // (권한 요청 객체 생성)
        createPermissionObjects()
        // (ActivityResultLauncher 객체 생성)
        createActivityResultLauncher()

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
        viewModelMbr = ViewModelProvider(this)[ActivitySystemCameraSampleViewModel::class.java]

    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr =
                viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken
        }
    }

    // 권한 요청 객체 생성
    private fun createPermissionObjects() {
        permissionRequestMbr =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.size == 1 && // 개별 권한 요청
                    permissions.containsKey(Manifest.permission.CAMERA)
                ) { // 카메라 권한
                    val isGranted = permissions[Manifest.permission.CAMERA]!!
                    val neverAskAgain =
                        !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

                    if (isGranted) { // 권한 승인
                        startSystemCamera()
                    } else { // 권한 거부
                        if (!neverAskAgain) {
                            // 단순 거부
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                DialogConfirm.DialogInfoVO(
                                    true,
                                    "권한 요청",
                                    "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
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
                                            "카메라 장치 사용 권한이 필요합니다.\n" +
                                            "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 권한 설정 화면으로 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        intent.data = Uri.fromParts("package", packageName, null)
                                        goToCameraPermissionConfigResultLauncherMbr.launch(
                                            intent
                                        )
                                    },
                                    onNegBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                            DialogConfirm.DialogInfoVO(
                                                true,
                                                "권한 요청",
                                                "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
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
                                                "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
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
            }
    }

    private fun createActivityResultLauncher() {
        // 카메라 권한 설정 액티비티 이동 객체 생성
        goToCameraPermissionConfigResultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // 설정 페이지 복귀시 콜백

            // 권한을 승인했는지 확인
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) { // 권한이 승인 상태
                // 카메라 실행
                startSystemCamera()
            } else { // 권한 비승인 상태
                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                    DialogConfirm.DialogInfoVO(
                        true,
                        "권한 요청",
                        "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
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
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 시스템 카메라 테스트 버튼
        bindingMbr.systemCameraTestBtn.setOnClickListener {
            // 카메라 디바이스 사용 가능 여부 확인
            if (this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                permissionRequestMbr.launch(arrayOf(Manifest.permission.CAMERA))
            } else {
                // 디바이스 장치에 카메라가 없는 상태
                viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                    true,
                    "장치 접근",
                    "현재 카메라 장치에 접근할 수 없습니다.\n장치 상태를 확인해주세요.",
                    null,
                    onCheckBtnClicked = {
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    },
                    onCanceled = {
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    }
                )
            }
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

    private fun startSystemCamera() {
        // todo
        Log.e("start", "camera")
    }
}