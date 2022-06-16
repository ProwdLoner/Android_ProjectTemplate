package com.example.prowd_android_template.activity_set.activity_basic_camera2_api_sample

import android.app.Application
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.ScriptC_rotator
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.util_class.CameraObj
import com.example.prowd_android_template.util_class.HandlerThreadObj
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ActivityBasicCamera2ApiSampleViewModel(application: Application) :
    AndroidViewModel(application) {
    // <멤버 변수 공간>
    private val applicationMbr = application

    // (repository 모델)
    private val repositorySetMbr: RepositorySet = RepositorySet.getInstance(applicationMbr)

    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (SharedPreference 객체)
    // 현 로그인 정보 접근 객체
    val currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw =
        CurrentLoginSessionInfoSpw(application)

    // (데이터)
    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // 카메라 실행 객체
    lateinit var backCameraObjMbr: CameraObj

    // Camera2 api 핸들러 스레드
    val cameraHandlerThreadMbr = HandlerThreadObj("back_camera").apply {
        this.startHandlerThread()
    }

    // 이미지 리더 핸들러 스레드
    val imageReaderHandlerThreadMbr = HandlerThreadObj("back_camera_image_reader").apply {
        this.startHandlerThread()
    }

    // 랜더 스크립트
    var renderScriptMbr: RenderScript = RenderScript.create(application)

    // intrinsic yuv to rgb
    var scriptIntrinsicYuvToRGBMbr: ScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(
        renderScriptMbr,
        Element.U8_4(renderScriptMbr)
    )

    var scriptCRotatorMbr: ScriptC_rotator = ScriptC_rotator(renderScriptMbr)

    // 카메라 핀치 줌 변수
    var beforeFingerSpacingMbr = 0f
    var zoomLevelMbr = 1f

    // (플래그 데이터)
    // 설정 변경 여부 : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
    var isChangingConfigurationsMbr = false

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr = true
    // 액티비티 진입 필수 권한 요청 여부
    var isActivityPermissionClearMbr = false

    // 카메라 이미지 프로세싱 여부
    var doImageProcessing = true


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


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCleared() {
        executorServiceMbr?.shutdown()
        executorServiceMbr = null

        // 랜더 스크립트 객체 해소
        scriptIntrinsicYuvToRGBMbr.destroy()
        scriptCRotatorMbr.destroy()
        renderScriptMbr.finish()
        renderScriptMbr.destroy()

        // 카메라 스레드 해소
        cameraHandlerThreadMbr.stopHandlerThread()
        imageReaderHandlerThreadMbr.stopHandlerThread()

        backCameraObjMbr.clearCameraObject()

        super.onCleared()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
}