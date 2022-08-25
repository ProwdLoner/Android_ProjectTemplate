package com.example.prowd_android_template.activity_set.activity_system_camera_photo_sample

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivitySystemCameraPhotoSampleBinding
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.util_object.GalleryUtil
import com.example.prowd_android_template.util_object.UriAndPath
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// todo : 시스템 카메라 코드 정리
class ActivitySystemCameraPhotoSample : AppCompatActivity() {
    // <설정 변수 공간>
    // (앱 진입 필수 권한 배열)
    // : 앱 진입에 필요한 권한 배열.
    //     ex : Manifest.permission.INTERNET
    private val activityPermissionArrayMbr: Array<String> = arrayOf()


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivitySystemCameraPhotoSampleBinding

    // (repository 모델)
    lateinit var repositorySetMbr: RepositorySet

    // (SharedPreference 객체)
    // 현 로그인 정보 접근 객체
    lateinit var currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw

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
    private var permissionRequestOnProgressMbr = false
    private val permissionRequestOnProgressSemaphoreMbr = Semaphore(1)

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
                        var isPermissionAllGranted = true
                        var neverAskAgain = false
                        for (activityPermission in activityPermissionArrayMbr) {
                            if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                                // 권한 클리어 플래그를 변경하고 break
                                neverAskAgain =
                                    !shouldShowRequestPermissionRationale(activityPermission)
                                isPermissionAllGranted = false
                                break
                            }
                        }

                        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
                            permissionRequestOnProgressSemaphoreMbr.acquire()
                            permissionRequestOnProgressMbr = false
                            permissionRequestOnProgressSemaphoreMbr.release()

                            allPermissionsGranted()
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
                                                permissionRequestOnProgressSemaphoreMbr.acquire()
                                                permissionRequestOnProgressMbr = false
                                                permissionRequestOnProgressSemaphoreMbr.release()

                                                allPermissionsGranted()
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

    override fun onDestroy() {
        // 다이얼로그 객체 해소
        dialogMbr?.dismiss()

        super.onDestroy()
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
        bindingMbr = ActivitySystemCameraPhotoSampleBinding.inflate(layoutInflater)
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
    // : 뷰 리스너 바인딩, 초기 뷰 사이즈, 위치 조정 등
    private fun onCreateInitView() {
        // 시스템 카메라 테스트 버튼
        bindingMbr.systemCameraTestBtn.setOnClickListener {
            // 카메라 디바이스 사용 가능 여부 확인
            if (this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {

                // 권한 요청 콜백
                val permissionArray: Array<String> = arrayOf(Manifest.permission.CAMERA)
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
                        startSystemCamera()
                    } else if (!neverAskAgain) { // 단순 거부
                        shownDialogInfoVOMbr =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "권한 요청",
                                "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
                                null,
                                onCheckBtnClicked = {
                                    shownDialogInfoVOMbr = null
                                },
                                onCanceled = {
                                    shownDialogInfoVOMbr = null
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
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                        }

                                    resultLauncherCallbackMbr = {
                                        // 설정 페이지 복귀시 콜백
                                        var isPermissionAllGranted1 = true
                                        for (activityPermission in permissionArray) {
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
                                            startSystemCamera()
                                        } else { // 권한 거부
                                            shownDialogInfoVOMbr =
                                                DialogConfirm.DialogInfoVO(
                                                    true,
                                                    "권한 요청",
                                                    "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
                                                    null,
                                                    onCheckBtnClicked = {
                                                        shownDialogInfoVOMbr = null
                                                    },
                                                    onCanceled = {
                                                        shownDialogInfoVOMbr = null
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
                                            "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
                                            null,
                                            onCheckBtnClicked = {
                                                shownDialogInfoVOMbr = null
                                            },
                                            onCanceled = {
                                                shownDialogInfoVOMbr = null
                                            }
                                        )
                                },
                                onCanceled = {}
                            )
                    }
                }

                // 권한 요청
                permissionRequestMbr.launch(permissionArray)
            } else {
                // 디바이스 장치에 카메라가 없는 상태
                shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                    true,
                    "장치 접근",
                    "카메라를 사용할 수 없습니다.\n장치 상태를 확인해주세요.",
                    null,
                    onCheckBtnClicked = {
                        shownDialogInfoVOMbr = null
                    },
                    onCanceled = {
                        shownDialogInfoVOMbr = null
                    }
                )
            }
        }

        bindingMbr.addToGalleryBtn.setOnClickListener {
            if (cameraImageFileMbr != null) {

                shownDialogInfoVOMbr =
                    DialogProgressLoading.DialogInfoVO(
                        false,
                        "갤러리에 사진을 저장중입니다.",
                        onCanceled = {}
                    )

                executorServiceMbr.execute {

                    GalleryUtil.addImageFileToGallery(
                        this,
                        cameraImageFileMbr!!,
                        "ProwdTemplate"
                    )

                    runOnUiThread {
                        shownDialogInfoVOMbr = null
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

    // (액티비티 진입 권한이 클리어 된 시점)
    // : 실질적인 액티비티 로직 실행구역
    private var doItAlreadyMbr = false
    private var currentUserSessionTokenMbr: String? = null
    private fun allPermissionsGranted() {
        if (!doItAlreadyMbr) {
            // (권한이 충족된 onCreate)
            doItAlreadyMbr = true

            // (초기 데이터 수집)
            currentUserSessionTokenMbr = currentLoginSessionInfoSpwMbr.sessionToken
            getScreenDataAndShow()

        } else {
            // (onResume - (권한이 충족된 onCreate))

            // (유저별 데이터 갱신)
            // : 유저 정보가 갱신된 상태에서 다시 현 액티비티로 복귀하면 자동으로 데이터를 다시 갱신합니다.
            val sessionToken = currentLoginSessionInfoSpwMbr.sessionToken
            if (sessionToken != currentUserSessionTokenMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                currentUserSessionTokenMbr = sessionToken

                // (데이터 수집)
                getScreenDataAndShow()
            }

        }

        // (onResume)
    }

    // (화면 구성용 데이터를 가져오기)
    // : 네트워크 등 레포지토리에서 데이터를 가져오고 이를 뷰에 반영
    private fun getScreenDataAndShow() {

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

                    Glide.with(this)
                        .load(UriAndPath.getUriFromPath(file.absolutePath))
                        .transform(CenterCrop())
                        .into(bindingMbr.fileImg)
                }
            }

            resultLauncherMbr.launch(takePictureIntent)
        } else {
            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                true,
                "시스템 카메라 사용 불가",
                "기본 카메라 앱을\n실행할 수 없습니다.\n설치 여부를 확인해주세요.",
                null,
                onCheckBtnClicked = {
                    shownDialogInfoVOMbr = null
                },
                onCanceled = {
                    shownDialogInfoVOMbr = null
                }
            )
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}