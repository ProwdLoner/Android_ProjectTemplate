package com.example.prowd_android_template.activity_set.activity_permission_sample

import android.Manifest
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
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityPermissionSampleBinding

// 권한 규칙 :
// 1. 앱 전체 권한은 앱 처음 실행시 스플래시 화면에서 한번 요청하기
// 2. 서비스 필수 권한은 해당 서비스 사용 이전에 클리어하기(위치 정보 사용 액티비티 진입시, 진입 시점에 위치권한이 승인이 되어야 진입 되도록 하기)
// 3. 권한이 승인되어 이동이 되었어도, 해당 액티비티에서 해당 권한을 직접 사용하는 부분에서 처리하기
//    잠깐 설정에 가서 권한을 취소하고 오더라도 에러가 없도록. 이와 같은 경우, 권한이 없을 때는 다이얼로그 표시 후 뒤로가기
// 4. 서버에 권한상태를 전할 필요는 없음.
//    1계정 여러 디바이스가 존재할수 있으니 저장할 데이터도 많아짐.
//    카메라 권한과 같은건 서버에서 알 필요가 없고,
//    위치 서비스를 위한 위치 권한은, 좌표값을 null 로 보내주는 방법으로 서버에서 데이터 추려오는 기준으로 사용.
//    푸시 권한은 내부적으로 처리를 하면 됨.
// 5. 계정만 바뀌었을 때에는 수동으로 변경하지 않는 이상 권한 변경은 없음. (기존 기기에 저장된 권한이 계속 이어짐)
//    고로 설정 화면에서 이를 조정하는 스위치를 준비해주는 것도 좋은 방법.
class ActivityPermissionSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityPermissionSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityPermissionSampleViewModel

    // (권한 요청 객체)
    private lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>

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

    // 복귀 후 실행 콜백
    private var permissionResultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityPermissionSampleBinding.inflate(layoutInflater)
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
            // 권한 스위치 상태 변경
            setSwitchView()

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
        viewModelMbr = ViewModelProvider(this)[ActivityPermissionSampleViewModel::class.java]

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
                    permissions.containsKey(Manifest.permission.READ_EXTERNAL_STORAGE)
                ) { // 외부 저장소 읽기 권한
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
                                        permissionResultLauncherCallbackMbr = { }
                                        permissionResultLauncherMbr.launch(intent)
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
                } else if (permissions.size == 1 && // 개별 권한 요청
                    permissions.containsKey(Manifest.permission.CAMERA)
                ) { // 카메라 권한
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
                                        permissionResultLauncherCallbackMbr = { }
                                        permissionResultLauncherMbr.launch(intent)
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

                } else if (permissions.size == 2 && // 개별 권한 요청
                    permissions.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    permissions.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)
                ) { // 위치 권한
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
                                        permissionResultLauncherCallbackMbr = { }
                                        permissionResultLauncherMbr.launch(intent)
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
                } else if (permissions.size == 1 && // 개별 권한 요청
                    permissions.containsKey(Manifest.permission.ACCESS_FINE_LOCATION)
                ) { // 위치 권한 정확성 향상
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
                                        permissionResultLauncherCallbackMbr = { }
                                        permissionResultLauncherMbr.launch(intent)
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
            }
    }

    // (ActivityResultLauncher 객체 생성)
    private fun createActivityResultLauncher() {
        // 앱 권한 설정 후 복귀
        permissionResultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            permissionResultLauncherCallbackMbr?.let { it1 -> it1(it) }
            permissionResultLauncherCallbackMbr = null
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {

        // (리스너 설정)
        // 푸시 권한
        bindingMbr.pushPermissionSwitch.setOnClickListener {
            if (bindingMbr.pushPermissionSwitch.isChecked) { // 체크시
                // 로컬 저장소에 저장
                viewModelMbr.customDevicePermissionInfoSpwMbr.isPushPermissionGranted = true

                // 권한 설정 메시지
                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                    DialogConfirm.DialogInfoVO(
                        true,
                        "푸시 권한",
                        "푸시 권한이 승인 되었습니다.",
                        null,
                        onCheckBtnClicked = {
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value = null

                        },
                        onCanceled = {
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value = null

                        }
                    )
            } else {
                // 체크 해제시
                // 권한 해제 다이얼로그
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                    DialogBinaryChoose.DialogInfoVO(
                        false,
                        "푸시 알림 해제",
                        "푸시 알림을 해제하시겠습니까?",
                        null,
                        null,
                        onPosBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            // 로컬 저장소에 저장
                            viewModelMbr.customDevicePermissionInfoSpwMbr.isPushPermissionGranted =
                                false

                            // 권한 설정 메시지
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                DialogConfirm.DialogInfoVO(
                                    true,
                                    "푸시 권한",
                                    "푸시 권한이 해제 되었습니다.",
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
                        onNegBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            bindingMbr.pushPermissionSwitch.isChecked = true
                        },
                        onCanceled = {
                            // 취소 불가
                        }
                    )
            }
        }

        // 외부 저장소 읽기 권한
        bindingMbr.externalStorageReadPermissionSwitch.setOnClickListener {
            if (bindingMbr.externalStorageReadPermissionSwitch.isChecked) { // 체크시
                // 권한 요청
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
                            permissionResultLauncherCallbackMbr = { }
                            permissionResultLauncherMbr.launch(intent)
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
                            permissionResultLauncherCallbackMbr = { }
                            permissionResultLauncherMbr.launch(intent)
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

        // 위치 정보 조회 권한
        bindingMbr.locationPermissionSwitch.setOnClickListener {
            if (bindingMbr.locationPermissionSwitch.isChecked) { // 체크시
                // 권한 요청
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
                            permissionResultLauncherCallbackMbr = { }
                            permissionResultLauncherMbr.launch(intent)
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

    private fun setSwitchView() {
        // 푸시 권한 설정 여부 반영
        bindingMbr.pushPermissionSwitch.isChecked =
            viewModelMbr.customDevicePermissionInfoSpwMbr.isPushPermissionGranted

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

        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

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
    }
}