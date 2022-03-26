package com.example.prowd_android_template.activity_set.activity_permission_sample

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityPermissionSampleBinding


// 권한 규칙 :
// 앱 실행 화면에서 전체 앱 사용 권한 모두 요청하기
// 다음에 나올 메인 화면에서는 필수 권한이 없도록 할것. (= 필요 권한이 없어도 동작이 가능하도록 할것)
// 필수 권한은 해당 기능이 필수로 필요한 액티비티로 이동하기 직전에 클리어하고 진입하도록.
// 권한의 서버 반영은 권한 변경시와 init 화면에서 실행 (앱 설정을 바꾸고, 앱 실행 화면을 안 거칠 가능성이 있으므로, 권한이 필수인 서비스는 무조건 권한체크)
// 디바이스 권한 설정을 권한 취소 용도로 접근했다가 복귀하면 크래시가 나므로, 권한 취소를 막거나, 혹은 권한 취소 접근시 공지를 하고 앱을 종료할것.
class ActivityPermissionSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityPermissionSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityPermissionSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    private var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    private var confirmDialogMbr: DialogConfirm? = null


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

    // 초기 뷰 설정
    private fun viewSetting() {
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

        // todo : 위치 정보 정확과 대략 연동 (정확에 체크되었을 때에는 대략도 같이 체크, 대략을 체크 해제하면 정확도 체크 해제)
        // 위치 정보 조회 권한 (정확) 설정 여부 반영
        bindingMbr.fineLocationPermissionSwitch.isChecked =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // 위치 정보 조회 권한 (대략) 설정 여부 반영
        bindingMbr.coarseLocationPermissionSwitch.isChecked =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        // (리스너 설정)
        // 푸시 권한
        bindingMbr.pushPermissionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 체크시
                // todo 묻기 다이얼로그

                // 서버에 반영 (완료시 아래 로직 실행, 에러시 체크 상태 복구)
                // 추후 푸시 권한이 OS 필수 권한이 된다면 spw 가 아닌 권한 요청으로 처리
                // OS 필수 권한이 되었을 때라면, 서버 반영시,

                // 로컬 저장소에 저장
                viewModelMbr.customDevicePermissionInfoSpwMbr.isPushPermissionGranted = true

                // 권한 설정 메시지
                viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
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
                // todo 묻기 다이얼로그

                // 서버에 반영 (완료시 아래 로직 실행, 에러시 체크 상태 복구)

                // 로컬 저장소에 저장
                viewModelMbr.customDevicePermissionInfoSpwMbr.isPushPermissionGranted = false

                // 권한 설정 메시지
                viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                    true,
                    "푸시 권한",
                    "푸시 권한이 해제 되었습니다.",
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

        // 외부 저장소 읽기 권한
        bindingMbr.externalStorageReadPermissionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // 체크시

                // 시스템 권한은 설정에서 독자적으로 변경이 가능하므로 서버에 반영하지 않는 방향으로 서비스를 만들것.
                // 서버의 정보 제공에 필요한 권한이라면, 파라미터로 조절할 것.
                // 예를들어 위치 반영 정보라면, 위치권한 승인 상태라면 파라미터로 좌표값을 보내고, 미승인 상태라면 좌표값을 null 로 보내어 구분

                // 로컬 저장소에 저장

                // 권한 설정 메시지
                viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                    true,
                    "외부 저장소 읽기 권한",
                    "외부 저장소 읽기 권한이 승인 되었습니다.",
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

                // 서버에 반영 (완료시 아래 로직 실행, 에러시 체크 상태 복구)

                // 로컬 저장소에 저장

                // 권한 설정 메시지


            }
        }

        // 카메라 접근 권한
        bindingMbr.cameraPermissionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // 체크시

                // 서버에 반영 (완료시 아래 로직 실행, 에러시 체크 상태 복구)

                // 로컬 저장소에 저장

                // 권한 설정 메시지
                viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                    true,
                    "카메라 접근 권한",
                    "카메라 접근 권한이 승인 되었습니다.",
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

                // 서버에 반영 (완료시 아래 로직 실행, 에러시 체크 상태 복구)

                // 로컬 저장소에 저장

                // 권한 설정 메시지


            }
        }

        // 위치 정보 조회 권한 (정확)
        bindingMbr.fineLocationPermissionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // 체크시

                // 서버에 반영 (완료시 아래 로직 실행, 에러시 체크 상태 복구)

                // 로컬 저장소에 저장

                // 권한 설정 메시지
                viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                    true,
                    "위치 정보 조회 권한 (정확)",
                    "위치 정보 조회 권한 (정확)이 승인 되었습니다.",
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

                // 서버에 반영 (완료시 아래 로직 실행, 에러시 체크 상태 복구)

                // 로컬 저장소에 저장

                // 권한 설정 메시지


            }
        }

        // 위치 정보 조회 권한 (대략)
        bindingMbr.coarseLocationPermissionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // 체크시

                // 서버에 반영 (완료시 아래 로직 실행, 에러시 체크 상태 복구)

                // 로컬 저장소에 저장

                // 권한 설정 메시지
                viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                    true,
                    "위치 정보 조회 권한 (대략)",
                    "위치 정보 조회 권한 (대략)이 승인 되었습니다.",
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

                // 서버에 반영 (완료시 아래 로직 실행, 에러시 체크 상태 복구)

                // 로컬 저장소에 저장

                // 권한 설정 메시지


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
}