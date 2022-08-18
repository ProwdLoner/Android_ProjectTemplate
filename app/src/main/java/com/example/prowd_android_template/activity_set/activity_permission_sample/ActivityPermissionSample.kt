package com.example.prowd_android_template.activity_set.activity_permission_sample

import android.Manifest
import android.app.Application
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityPermissionSampleBinding
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// todo : 신코드 적용
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
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityPermissionSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ViewModel

    // (다이얼로그 객체)
    var dialogMbr: Dialog? = null

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
    //     앨티비티 화면 회전 = onPause() → onSaveInstanceState() → onStop() → onDestroy() →
    //         onCreate(savedInstanceState) → onStart() → onResume()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (초기 객체 생성)
        onCreateInitObject()

        // (초기 뷰 설정)
        onCreateInitView()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        onCreateSetLiveData()
    }

    override fun onResume() {
        super.onResume()

        // (액티비티 진입 필수 권한 확인)
        // 진입 필수 권한이 클리어 되어야 로직이 실행
        permissionRequestCallbackMbr = { permissions ->
            var isPermissionAllGranted = true
            for (activityPermission in viewModelMbr.activityPermissionArrayMbr) {
                if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                        true,
                        "권한 필요",
                        "서비스를 실행하기 위해 필요한 권한이 거부되었습니다.",
                        "뒤로가기",
                        onCheckBtnClicked = {
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value = null

                            finish()
                        },
                        onCanceled = {
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value = null

                            finish()
                        }
                    )

                    // 권한 클리어 플래그를 변경하고 break
                    isPermissionAllGranted = false
                    break
                }
            }

            if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
                allPermissionsGranted()
            }
        }

        permissionRequestMbr.launch(viewModelMbr.activityPermissionArrayMbr)
    }

    override fun onDestroy() {
        // 다이얼로그 객체 해소
        dialogMbr?.dismiss()

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    private fun onCreateInitObject() {
        // 뷰 객체
        bindingMbr = ActivityPermissionSampleBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ViewModel::class.java]

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

        // (리스너 설정)
        // 외부 저장소 읽기 권한
        bindingMbr.externalStorageReadPermissionSwitch.setOnClickListener {
            if (bindingMbr.externalStorageReadPermissionSwitch.isChecked) { // 체크시
                // 권한 요청
                permissionRequestCallbackMbr = { permissions ->
                    // 외부 저장소 읽기 권한
                    val isGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE]!!
                    val neverAskAgain =
                        !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)

                    if (isGranted) { // 권한 승인
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "외부 저장소 읽기 권한",
                                "외부 저장소 읽기 권한이 승인 되었습니다.",
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

                    } else { // 권한 거부
                        if (!neverAskAgain) {
                            // 단순 거부

                            // 뷰 상태 되돌리기
                            bindingMbr.externalStorageReadPermissionSwitch.isChecked = false

                        } else {
                            // 다시 묻지 않기 선택
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 권한 설정 페이지 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", packageName, null)
                                        intent.data = uri
                                        resultLauncherCallbackMbr = {
                                            setSwitchView()
                                        }
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 뷰 상태 되돌리기
                                        bindingMbr.externalStorageReadPermissionSwitch.isChecked =
                                            false
                                    },
                                    onCanceled = {
                                        // 취소 불가
                                    }
                                )
                        }
                    }
                }
                permissionRequestMbr.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            } else {
                // 체크 해제시
                // 권한 해제 다이얼로그
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "외부 저장소 읽기 권한 해제",
                        "외부 저장소 읽기 권한을 해제하시겠습니까?\n(권한 설정 화면으로 이동합니다.)",
                        null,
                        null,
                        onPosBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            // 권한 설정 페이지 이동
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            resultLauncherCallbackMbr = {
                                setSwitchView()
                            }
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            bindingMbr.externalStorageReadPermissionSwitch.isChecked = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            }
        }

        // 카메라 사용 권한
        bindingMbr.cameraPermissionSwitch.setOnClickListener {
            if (bindingMbr.cameraPermissionSwitch.isChecked) { // 체크시
                // 권한 요청
                permissionRequestCallbackMbr = { permissions ->
                    // 카메라 권한
                    val isGranted = permissions[Manifest.permission.CAMERA]!!
                    val neverAskAgain =
                        !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

                    if (isGranted) { // 권한 승인
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "카메라 사용 권한",
                                "카메라 사용 권한이 승인 되었습니다.",
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

                    } else { // 권한 거부
                        if (!neverAskAgain) {
                            // 단순 거부

                            // 뷰 상태 되돌리기
                            bindingMbr.cameraPermissionSwitch.isChecked = false

                        } else {
                            // 다시 묻지 않기 선택
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 권한 설정 페이지 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", packageName, null)
                                        intent.data = uri
                                        resultLauncherCallbackMbr = {
                                            setSwitchView()
                                        }
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 뷰 상태 되돌리기
                                        bindingMbr.cameraPermissionSwitch.isChecked =
                                            false
                                    },
                                    onCanceled = {
                                        // 취소 불가
                                    }
                                )
                        }
                    }
                }
                permissionRequestMbr.launch(arrayOf(Manifest.permission.CAMERA))
            } else {
                // 체크 해제시
                // 권한 해제 다이얼로그
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "카메라 사용 권한 해제",
                        "카메라 사용 권한을 해제하시겠습니까?\n(권한 설정 화면으로 이동합니다.)",
                        null,
                        null,
                        onPosBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            // 권한 설정 페이지 이동
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            resultLauncherCallbackMbr = {
                                setSwitchView()
                            }
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            bindingMbr.cameraPermissionSwitch.isChecked = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            }
        }

        // 오디오 녹음 권한
        bindingMbr.audioRecordPermissionSwitch.setOnClickListener {
            if (bindingMbr.audioRecordPermissionSwitch.isChecked) { // 체크시
                // 권한 요청
                permissionRequestCallbackMbr = { permissions ->
                    // 카메라 권한
                    val isGranted = permissions[Manifest.permission.RECORD_AUDIO]!!
                    val neverAskAgain =
                        !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)

                    if (isGranted) { // 권한 승인
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "오디오 녹음 권한",
                                "오디오 녹음 권한이 승인 되었습니다.",
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
                    } else { // 권한 거부
                        if (!neverAskAgain) {
                            // 단순 거부

                            // 뷰 상태 되돌리기
                            bindingMbr.audioRecordPermissionSwitch.isChecked = false
                        } else {
                            // 다시 묻지 않기 선택
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 권한 설정 페이지 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", packageName, null)
                                        intent.data = uri
                                        resultLauncherCallbackMbr = {
                                            setSwitchView()
                                        }
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 뷰 상태 되돌리기
                                        bindingMbr.audioRecordPermissionSwitch.isChecked =
                                            false
                                    },
                                    onCanceled = {
                                        // 취소 불가
                                    }
                                )
                        }
                    }
                }
                permissionRequestMbr.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            } else {
                // 체크 해제시
                // 권한 해제 다이얼로그
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "오디오 녹음 권한 해제",
                        "오디오 녹음 권한을 해제하시겠습니까?\n(권한 설정 화면으로 이동합니다.)",
                        null,
                        null,
                        onPosBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            // 권한 설정 페이지 이동
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            resultLauncherCallbackMbr = {
                                setSwitchView()
                            }
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            bindingMbr.audioRecordPermissionSwitch.isChecked = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            }
        }

        // 위치 정보 조회 권한
        bindingMbr.locationPermissionSwitch.setOnClickListener {
            if (bindingMbr.locationPermissionSwitch.isChecked) { // 체크시
                // 권한 요청
                permissionRequestCallbackMbr = { permissions ->
                    // 위치 권한
                    val isFineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]!!
                    val isCoarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION]!!

                    val neverAskAgain =
                        !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

                    if (isFineGranted || isCoarseGranted) { // 권한 승인

                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "위치 정보 조회 권한",
                                "위치 정보 조회 권한이 승인 되었습니다.",
                                null,
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null

                                    when {
                                        isFineGranted -> {// 위치 권한 fine 을 승인하면 자동으로 모두 승인한 것과 같음
                                            bindingMbr.locationPermissionSwitch.isChecked = true
                                            bindingMbr.locationPermissionDetailSwitch.isChecked =
                                                true
                                            bindingMbr.locationPermissionDetailContainer.visibility =
                                                View.GONE
                                        }
                                        isCoarseGranted -> {// 위치 권한 coarse 만 승인
                                            // 정확도 보정 설정 보여주기
                                            bindingMbr.locationPermissionSwitch.isChecked = true
                                            bindingMbr.locationPermissionDetailSwitch.isChecked =
                                                false
                                            bindingMbr.locationPermissionDetailContainer.visibility =
                                                View.VISIBLE
                                        }
                                    }
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null

                                    when {
                                        isFineGranted -> {// 위치 권한 fine 을 승인하면 자동으로 모두 승인한 것과 같음
                                            bindingMbr.locationPermissionSwitch.isChecked = true
                                            bindingMbr.locationPermissionDetailSwitch.isChecked =
                                                true
                                            bindingMbr.locationPermissionDetailContainer.visibility =
                                                View.GONE
                                        }
                                        isCoarseGranted -> {// 위치 권한 coarse 만 승인
                                            // 정확도 보정 설정 보여주기
                                            bindingMbr.locationPermissionSwitch.isChecked = true
                                            bindingMbr.locationPermissionDetailSwitch.isChecked =
                                                false
                                            bindingMbr.locationPermissionDetailContainer.visibility =
                                                View.VISIBLE
                                        }
                                    }
                                }
                            )

                    } else { // 권한 거부
                        if (!neverAskAgain) {
                            // 단순 거부
                            bindingMbr.locationPermissionSwitch.isChecked = false
                            bindingMbr.locationPermissionDetailSwitch.isChecked = false
                            bindingMbr.locationPermissionDetailContainer.visibility = View.GONE

                        } else {
                            // 다시 묻지 않기 선택
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 권한 설정 페이지 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", packageName, null)
                                        intent.data = uri
                                        resultLauncherCallbackMbr = {
                                            setSwitchView()
                                        }
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        bindingMbr.locationPermissionSwitch.isChecked = false
                                        bindingMbr.locationPermissionDetailSwitch.isChecked = false
                                        bindingMbr.locationPermissionDetailContainer.visibility =
                                            View.GONE
                                    },
                                    onCanceled = {
                                        // 취소 불가
                                    }
                                )
                        }
                    }
                }
                permissionRequestMbr.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else {
                // 체크 해제시
                // 권한 해제 다이얼로그
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "위치 정보 조회 권한 해제",
                        "위치 정보 조회 권한을 해제하시겠습니까?\n(권한 설정 화면으로 이동합니다.)",
                        null,
                        null,
                        onPosBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            // 권한 설정 페이지 이동
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            resultLauncherCallbackMbr = {
                                setSwitchView()
                            }
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            bindingMbr.locationPermissionSwitch.isChecked = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            }
        }

        // 위치 정보 조회 권한 (정확)
        bindingMbr.locationPermissionDetailSwitch.setOnClickListener {
            if (bindingMbr.locationPermissionDetailSwitch.isChecked) { // 체크시
                // 권한 요청
                permissionRequestCallbackMbr = { permissions ->
                    // 위치 권한 정확성 향상
                    val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]!!
                    val neverAskAgain =
                        !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

                    if (isGranted) { // 권한 승인
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "위치 권한 정확성 향상",
                                "위치 권한 정확성이 향상 되었습니다.",
                                null,
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null

                                    // 향상 메뉴 숨기기
                                    bindingMbr.locationPermissionDetailContainer.visibility =
                                        View.GONE
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null

                                    // 향상 메뉴 숨기기
                                    bindingMbr.locationPermissionDetailContainer.visibility =
                                        View.GONE
                                }
                            )

                    } else { // 권한 거부
                        if (!neverAskAgain) {
                            // 단순 거부

                            // 뷰 상태 되돌리기
                            bindingMbr.locationPermissionDetailSwitch.isChecked = false
                            bindingMbr.locationPermissionDetailContainer.visibility = View.VISIBLE

                        } else {
                            // 다시 묻지 않기 선택
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 권한 설정 페이지 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", packageName, null)
                                        intent.data = uri
                                        resultLauncherCallbackMbr = {
                                            setSwitchView()
                                        }
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                        // 뷰 상태 되돌리기
                                        bindingMbr.locationPermissionDetailSwitch.isChecked = false
                                        bindingMbr.locationPermissionDetailContainer.visibility =
                                            View.VISIBLE
                                    },
                                    onCanceled = {
                                        // 취소 불가
                                    }
                                )
                        }
                    }
                }
                permissionRequestMbr.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else {
                // 체크 해제시
                // 정확도 향상시엔 항목을 숨길 것이기에 해제 불가
            }
        }

        // 시스템 설정 변경 권한
        bindingMbr.writeSettingPermissionSwitch.setOnClickListener {
            if (bindingMbr.writeSettingPermissionSwitch.isChecked) { // 체크시
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "권한 요청",
                        "권한 설정 화면으로 이동하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            // 권한 설정 페이지 이동
                            val intent =
                                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = Uri.parse("package:" + this.packageName)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            resultLauncherCallbackMbr = {
                                setSwitchView()
                            }
                            resultLauncherMbr.launch(intent)
                        },
                        onNegBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            // 뷰 상태 되돌리기
                            bindingMbr.writeSettingPermissionSwitch.isChecked = false
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            } else {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "권한 요청",
                        "권한 설정 화면으로 이동하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

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
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            // 뷰 상태 되돌리기
                            bindingMbr.writeSettingPermissionSwitch.isChecked = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )

            }
        }
    }

    // (라이브 데이터 설정)
    private fun onCreateSetLiveData() {
        // 로딩 다이얼로그 출력 플래그
        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogProgressLoading) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogProgressLoading(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // progressSample2 진행도
        viewModelMbr.progressDialogSample2ProgressValue.observe(this) {
            if (it != -1) {
                val loadingText = "로딩중 $it%"
                if (dialogMbr != null) {
                    (dialogMbr as DialogProgressLoading).bindingMbr.progressMessageTxt.text =
                        loadingText
                    (dialogMbr as DialogProgressLoading).bindingMbr.progressBar.visibility =
                        View.VISIBLE
                    (dialogMbr as DialogProgressLoading).bindingMbr.progressBar.progress = it
                }
            }
        }

        // 선택 다이얼로그 출력 플래그
        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogBinaryChoose) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogBinaryChoose(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // 확인 다이얼로그 출력 플래그
        viewModelMbr.confirmDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogConfirm) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogConfirm(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // 라디오 버튼 선택 다이얼로그 출력 플래그
        viewModelMbr.radioButtonChooseDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogRadioButtonChoose) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogRadioButtonChoose(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }
    }

    // (액티비티 진입 권한이 클리어 된 시점)
    private fun allPermissionsGranted() {
            if (!viewModelMbr.doItAlreadyMbr) {
                // (액티비티 실행시 처음 한번만 실행되는 로직)
                viewModelMbr.doItAlreadyMbr = true

                // (초기 데이터 수집)

                // (알고리즘)
                // 권한 스위치 상태 변경
                setSwitchView()
            } else {
                // (회전이 아닌 onResume 로직) : 권한 클리어
                // (뷰 데이터 로딩)
                // : 유저가 변경되면 해당 유저에 대한 데이터로 재구축
                val sessionToken = viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken
                if (sessionToken != viewModelMbr.currentUserSessionTokenMbr) { // 액티비티 유저와 세션 유저가 다를 때
                    // 진입 플래그 변경
                    viewModelMbr.currentUserSessionTokenMbr = sessionToken

                    // (데이터 수집)

                    // (알고리즘)
                }

                // 권한 스위치 상태 변경
                setSwitchView()
            }
    }

    // (권한에 따라 스위치 반영)
    private fun setSwitchView() {
        // 외부 저장소 읽기 권한 설정 여부 반영
        bindingMbr.externalStorageReadPermissionSwitch.isChecked =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

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
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // (뷰모델 객체)
    // : 액티비티 reCreate 이후에도 남아있는 데이터 묶음 = 뷰의 데이터 모델
    //     뷰모델이 맡은 것은 화면 회전시에도 불변할 데이터의 저장
    class ViewModel(application: Application) : AndroidViewModel(application) {
        // <멤버 상수 공간>
        // (repository 모델)
        val repositorySetMbr: RepositorySet = RepositorySet.getInstance(application)

        // (스레드 풀)
        val executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw =
            CurrentLoginSessionInfoSpw(application)

        // (앱 진입 필수 권한 배열)
        // : 앱 진입에 필요한 권한 배열.
        //     ex : Manifest.permission.INTERNET
        val activityPermissionArrayMbr: Array<String> = arrayOf()


        // ---------------------------------------------------------------------------------------------
        // <멤버 변수 공간>
        // (최초 실행 플래그) : 액티비티가 실행되고, 권한 체크가 끝난 후의 최초 로직이 실행되었는지 여부
        var doItAlreadyMbr = false

        // (이 화면에 도달한 유저 계정 고유값) : 세션 토큰이 없다면 비회원 상태
        var currentUserSessionTokenMbr: String? = null


        // ---------------------------------------------------------------------------------------------
        // <뷰모델 라이브데이터 공간>
        // 로딩 다이얼로그 출력 정보
        val progressLoadingDialogInfoLiveDataMbr: MutableLiveData<DialogProgressLoading.DialogInfoVO?> =
            MutableLiveData(null)

        val progressDialogSample2ProgressValue: MutableLiveData<Int> =
            MutableLiveData(-1)

        // 선택 다이얼로그 출력 정보
        val binaryChooseDialogInfoLiveDataMbr: MutableLiveData<DialogBinaryChoose.DialogInfoVO?> =
            MutableLiveData(null)

        // 확인 다이얼로그 출력 정보
        val confirmDialogInfoLiveDataMbr: MutableLiveData<DialogConfirm.DialogInfoVO?> =
            MutableLiveData(null)

        // 라디오 버튼 선택 다이얼로그 출력 정보
        val radioButtonChooseDialogInfoLiveDataMbr: MutableLiveData<DialogRadioButtonChoose.DialogInfoVO?> =
            MutableLiveData(null)


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>
    }
}