package com.example.prowd_android_template.activity_set.activity_system_camera_sample

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
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
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivitySystemCameraSampleBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class ActivitySystemCameraSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivitySystemCameraSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivitySystemCameraSampleViewModel

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    private var permissionRequestCallbackMbr: (((MutableMap<String, Boolean>) -> Unit))? = null

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    private var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    private var confirmDialogMbr: DialogConfirm? = null

    // (ResultLauncher 객체)
    // 권한 설정 화면 이동 복귀 객체
    private lateinit var permissionResultLauncherMbr: ActivityResultLauncher<Intent>
    private var permissionResultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null

    // 시스템 카메라 이동 복귀 객체
    private lateinit var systemCameraResultLauncherMbr: ActivityResultLauncher<Intent>
    private var systemCameraResultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null


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

        // 권한 요청 객체 생성
        permissionRequestMbr =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissionRequestCallbackMbr?.let { it1 -> it1(it) }
                permissionRequestCallbackMbr = null
            }

        // 앱 권한 설정 후 복귀
        permissionResultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            permissionResultLauncherCallbackMbr?.let { it1 -> it1(it) }
            permissionResultLauncherCallbackMbr = null
        }

        // 시스템 카메라 이동 복귀
        systemCameraResultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            systemCameraResultLauncherCallbackMbr?.let { it1 -> it1(it) }
            systemCameraResultLauncherCallbackMbr = null
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
                                        permissionResultLauncherCallbackMbr = {
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
                                        permissionResultLauncherMbr.launch(intent)
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
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
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
            if (cameraImagePathMbr != null) {
                addImageFileToGallery(File(cameraImagePathMbr!!))
            } else {
                Toast.makeText(this, "갤러리에 저장할 사진이 없습니다.", Toast.LENGTH_SHORT).show()
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

    // 시스템 카메라 시작 : 카메라 관련 권한이 충족된 상태
    // todo 갤러리 이미지 보여주기, 갤러리 이미지 지우기, 이미지 클릭시 상세보기 지원
    private var cameraImagePathMbr: String? = null
    private fun startSystemCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // 내부 저장소 사진 파일 생성
            val imageFileName = "JPEG_" + "camera_img_temp_file" + "_"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val photoFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )

            cameraImagePathMbr = photoFile.absolutePath

            // 카메라 앱에 내부 저장소 사진 파일 공유
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.example.prowd_android_template",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            systemCameraResultLauncherCallbackMbr = {
                if (it.resultCode == RESULT_OK) {
                    // 저장된 파일 이미지 보기
                    Glide.with(this)
                        .load(File(cameraImagePathMbr!!))
                        .transform(CenterCrop())
                        .into(bindingMbr.fileImg)
                }
            }

            systemCameraResultLauncherMbr.launch(takePictureIntent)
        }
    }

    // todo : 커스텀 앨범 저장
    private fun addImageFileToGallery(imageFile: File) {
        // 카메라에서 가져온 파일
        var fileBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

        val orientation: Int = ExifInterface(imageFile.path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        fileBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> TransformationUtils.rotateImage(
                fileBitmap,
                90
            )
            ExifInterface.ORIENTATION_ROTATE_180 -> TransformationUtils.rotateImage(
                fileBitmap,
                180
            )
            ExifInterface.ORIENTATION_ROTATE_270 -> TransformationUtils.rotateImage(
                fileBitmap,
                270
            )
            ExifInterface.ORIENTATION_NORMAL -> fileBitmap
            else -> fileBitmap
        }

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = sdf.format(System.currentTimeMillis()) + ".jpg"

        // 카메라 파일을 갤러리에 저장
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        try {
            if (uri != null) {
                val descriptor = contentResolver.openFileDescriptor(uri, "w")
                if (descriptor != null) {
                    val fos = FileOutputStream(descriptor.fileDescriptor)
                    fileBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.close()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }
                }
            }

            Toast.makeText(this, "갤러리에 사진을 저장했습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: java.lang.Exception) {
            Log.e("File", "error=${e.localizedMessage}")
        }
    }
}