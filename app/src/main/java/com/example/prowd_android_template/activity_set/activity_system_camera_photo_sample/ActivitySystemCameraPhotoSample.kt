package com.example.prowd_android_template.activity_set.activity_system_camera_photo_sample

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivitySystemCameraPhotoSampleBinding
import com.example.prowd_android_template.util_object.GalleryUtil
import com.example.prowd_android_template.util_object.UriAndPath
import java.io.File


class ActivitySystemCameraPhotoSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivitySystemCameraPhotoSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivitySystemCameraPhotoSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null

    // 라디오 버튼 다이얼로그
    var radioBtnDialogMbr: DialogRadioButtonChoose? = null

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((MutableMap<String, Boolean>) -> Unit))? = null

    // (ResultLauncher 객체)
    // 액티비티 이동 복귀 객체
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivitySystemCameraPhotoSampleBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 객체 생성)
        createMemberObjects()

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

        // 설정 변경(화면회전)을 했는지 여부를 초기화
        // onResume 의 가장 마지막
        viewModelMbr.isChangingConfigurationsMbr = false
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
        radioBtnDialogMbr?.dismiss()

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ActivitySystemCameraPhotoSampleViewModel::class.java]

        // 권한 요청 객체 생성
        permissionRequestMbr = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionRequestCallbackMbr?.let { it1 -> it1(it) }
            permissionRequestCallbackMbr = null
        }

        // 액티비티 이동 복귀 객체 생성
        resultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            resultLauncherCallbackMbr?.let { it1 -> it1(it) }
            resultLauncherCallbackMbr = null
        }

    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 시스템 카메라 테스트 버튼
        bindingMbr.systemCameraTestBtn.setOnClickListener {
            // 카메라 디바이스 사용 가능 여부 확인
            if (this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                permissionRequestCallbackMbr = { permissions ->
                    // 카메라 권한
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

                                        resultLauncherCallbackMbr = {
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
                                        resultLauncherMbr.launch(intent)
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
                permissionRequestMbr.launch(
                    arrayOf(
                        Manifest.permission.CAMERA
                    )
                )
            } else {
                // 디바이스 장치에 카메라가 없는 상태
                viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                    true,
                    "장치 접근",
                    "카메라를 사용할 수 없습니다.\n장치 상태를 확인해주세요.",
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

        bindingMbr.addToGalleryBtn.setOnClickListener {
            if (cameraImageFileMbr != null) {

                viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value =
                    DialogProgressLoading.DialogInfoVO(
                        false,
                        "갤러리에 사진을 저장중입니다.",
                        onCanceled = {}
                    )

                viewModelMbr.executorServiceMbr!!.execute {

                    GalleryUtil.addImageFileToGallery(
                        this,
                        cameraImageFileMbr!!,
                        "ProwdTemplate"
                    )

                    runOnUiThread {
                        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null
                        Toast.makeText(this, "갤러리에 사진을 저장했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "갤러리에 저장할 사진이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        bindingMbr.goToGalleryBtn.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ).setType("image/*")

            val mimeTypes = arrayOf("image/jpeg", "image/png", "image/jpg")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

            resultLauncherCallbackMbr = { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val goToGalleryIntent = result.data

                    val selectedUri: Uri = goToGalleryIntent?.data!!

                    val gotoImageViewerIntent = Intent()
                    gotoImageViewerIntent.action = Intent.ACTION_VIEW
                    gotoImageViewerIntent.setDataAndType(selectedUri, "image/*")
                    startActivity(gotoImageViewerIntent)
                }
            }
            resultLauncherMbr.launch(intent)
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

        // 라디오 버튼 다이얼로그 출력 플래그
        viewModelMbr.radioButtonDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                radioBtnDialogMbr?.dismiss()

                radioBtnDialogMbr = DialogRadioButtonChoose(
                    this,
                    it
                )
                radioBtnDialogMbr?.show()
            } else {
                radioBtnDialogMbr?.dismiss()
                radioBtnDialogMbr = null
            }
        }

        viewModelMbr.selectedPhotoUriMbr.observe(this){
            if (it == null){
                if (!isDestroyed && !isFinishing) {
                    bindingMbr.fileImg.setImageResource(android.R.color.transparent)
                }
            }else{
                if (!isDestroyed && !isFinishing) {
                    Glide.with(this)
                        .load(it)
                        .transform(CenterCrop())
                        .into(bindingMbr.fileImg)
                }
            }
        }
    }

    // 시스템 카메라 시작 : 카메라 관련 권한이 충족된 상태
    private var cameraImageFileMbr: File? = null
    private fun startSystemCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // 내부 저장소 사진 파일 생성 (갤러리 추가)
            val file = File.createTempFile("photo_", ".jpg", externalCacheDir)

            // 카메라 앱에 내부 저장소 사진 파일 공유
            val photoUri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            resultLauncherCallbackMbr = {
                if (it.resultCode == RESULT_OK) {
                    cameraImageFileMbr = file

                    viewModelMbr.selectedPhotoUriMbr.value = UriAndPath.getUriFromPath(file.absolutePath)
                }
            }

            resultLauncherMbr.launch(takePictureIntent)
        } else {
            viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                true,
                "시스템 카메라 사용 불가",
                "기본 카메라 앱을\n실행할 수 없습니다.\n설치 여부를 확인해주세요.",
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