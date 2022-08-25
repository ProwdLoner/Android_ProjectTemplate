package com.example.prowd_android_template.activity_set.activity_basic_notification_sample

import android.app.Dialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.example.prowd_android_template.R
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityBasicNotificationSampleBinding
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.util_object.NotificationWrapper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// todo : 노티 유형 완성
class ActivityBasicNotificationSample : AppCompatActivity() {
    // <설정 변수 공간>
    // (앱 진입 필수 권한 배열)
    // : 앱 진입에 필요한 권한 배열.
    //     ex : Manifest.permission.INTERNET
    private val activityPermissionArrayMbr: Array<String> = arrayOf()


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicNotificationSampleBinding

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
        bindingMbr = ActivityBasicNotificationSampleBinding.inflate(layoutInflater)
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
        bindingMbr.basicNotificationBtn.setOnClickListener {
            NotificationWrapper.showNotification(
                this,
                "$packageName-${getString(R.string.app_name)}",
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW,
                setChannelSpace = {
                    it.description = "App notification channel"
                    it.setShowBadge(false)
                },
                setNotificationBuilderSpace = {
                    it.setSmallIcon(R.mipmap.ic_launcher)
                    it.setContentTitle("기본 노티")
                    it.setContentText("기본 노티 본문입니다.")
                    it.priority = NotificationCompat.PRIORITY_DEFAULT
                    it.setAutoCancel(true)
                },
                1
            )
        }

        bindingMbr.bigTextNotificationBtn.setOnClickListener {
            NotificationWrapper.showNotification(
                this,
                "$packageName-${getString(R.string.app_name)}",
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW,
                setChannelSpace = {
                    it.description = "App notification channel"
                    it.setShowBadge(false)
                },
                setNotificationBuilderSpace = {
                    it.setSmallIcon(R.mipmap.ic_launcher)
                    it.setContentTitle("긴 본문 노티")
                    it.setContentText("긴 본문 노티 본문입니다.")
                    val bigText = "Android 9 introduces several enhancements to notifications," +
                            " all of which are available to developers targeting API level 28 and above."
                    val style = NotificationCompat.BigTextStyle()
                    style.bigText(bigText)
                    it.setStyle(style)
                    it.priority = NotificationCompat.PRIORITY_DEFAULT
                    it.setAutoCancel(true)
                },
                2
            )
        }

        bindingMbr.bigPictureNotificationBtn.setOnClickListener {
            NotificationWrapper.showNotification(
                this,
                "$packageName-${getString(R.string.app_name)}",
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW,
                setChannelSpace = {
                    it.description = "App notification channel"
                    it.setShowBadge(false)
                },
                setNotificationBuilderSpace = {
                    it.setSmallIcon(R.mipmap.ic_launcher)
                    it.setContentTitle("큰 사진 노티")
                    it.setContentText("큰 사진 노티 본문입니다.")
                    val style = NotificationCompat.BigPictureStyle()
                    style.bigPicture(
                        BitmapFactory.decodeResource(
                            resources,
                            R.drawable.img_activity_basic_notification_sample_big_picture_notification
                        )
                    )
                    it.setStyle(style)
                    it.priority = NotificationCompat.PRIORITY_DEFAULT
                    it.setAutoCancel(true)
                },
                3
            )
        }

        bindingMbr.inboxNotificationBtn.setOnClickListener {
            NotificationWrapper.showNotification(
                this,
                "$packageName-${getString(R.string.app_name)}",
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW,
                setChannelSpace = {
                    it.description = "App notification channel"
                    it.setShowBadge(false)
                },
                setNotificationBuilderSpace = {
                    it.setSmallIcon(R.mipmap.ic_launcher)
                    it.setContentTitle("인박스 노티")
                    it.setContentText("인박스 노티 본문입니다.")
                    val style = NotificationCompat.InboxStyle()   // 3
                    style.addLine("Mail1 ...")    // 4
                    style.addLine("Mail2 ...")
                    style.addLine("Mail3 ...")
                    it.setStyle(style)
                    it.priority = NotificationCompat.PRIORITY_DEFAULT
                    it.setAutoCancel(true)
                },
                4
            )
        }

        bindingMbr.messagingNotificationBtn.setOnClickListener {
            NotificationWrapper.showNotification(
                this,
                "$packageName-${getString(R.string.app_name)}",
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW,
                setChannelSpace = {
                    it.description = "App notification channel"
                    it.setShowBadge(false)
                },
                setNotificationBuilderSpace = {
                    it.setSmallIcon(R.mipmap.ic_launcher)
                    it.setContentTitle("메시지 노티")
                    it.setContentText("메시지 노티 본문입니다.")

                    val sender = Person.Builder()
                        .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                        .setName("JS").build()
                    val style = NotificationCompat.MessagingStyle(sender)
                    style.conversationTitle = "Messenger"

                    val user1 = Person.Builder()
                        .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                        .setName("Chacha").build()
                    style.addMessage(
                        "You can get great deals there",
                        System.currentTimeMillis(),
                        user1
                    )

                    val user2 = Person.Builder()
                        .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                        .setName("Android").build()
                    style.addMessage(
                        "I know what to get",
                        System.currentTimeMillis(),
                        user2
                    )

                    it.setStyle(style)
                    it.priority = NotificationCompat.PRIORITY_DEFAULT
                    it.setAutoCancel(true)
                },
                5
            )
        }

        bindingMbr.mediaNotificationBtn.setOnClickListener {
            // todo
//            val NOTIFICATION_ID = 1001;
//            createNotificationChannel(this, NotificationManagerCompat.IMPORTANCE_DEFAULT, false,
//                getString(R.string.app_name), "App notification channel")
//
//            val channelId = "$packageName-${getString(R.string.app_name)}"
//            val title = "Don't Say a Word"
//            val content = "Ellie Goulding"
//
//            val intent = Intent(baseContext, NewActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            val pendingIntent = PendingIntent.getActivity(baseContext, 0,
//                intent, PendingIntent.FLAG_UPDATE_CURRENT)
//
//            val builder = NotificationCompat.Builder(this, channelId)
//            builder.setSmallIcon(R.drawable.ic_codechacha)
//            builder.setContentTitle(title)  // 1
//            builder.setContentText(content)  // 2
//            builder.setLargeIcon(
//                BitmapFactory.decodeResource(resources, R.drawable.castle)) // 3
//            builder.addAction(NotificationCompat.Action(
//                R.drawable.ic_thumb_down,"skip prev", pendingIntent))   // 4
//            builder.addAction(NotificationCompat.Action(
//                R.drawable.ic_skip_prev,"skip prev", pendingIntent))
//            builder.addAction(NotificationCompat.Action(
//                R.drawable.ic_pause,"pause", pendingIntent))
//            builder.addAction(NotificationCompat.Action(
//                R.drawable.ic_skip_next,"skip next", pendingIntent))
//            builder.addAction(NotificationCompat.Action(
//                R.drawable.ic_thumb_up,"skip prev", pendingIntent))
//            builder.setStyle(MediaStyle().setShowActionsInCompactView(1, 2, 3)) // 5
//            builder.priority = NotificationCompat.PRIORITY_DEFAULT
//            builder.setAutoCancel(true)
//            builder.setContentIntent(pendingIntent)
//
//            val notificationManager = NotificationManagerCompat.from(this)
//            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }

        bindingMbr.inlineNotificationBtn.setOnClickListener {
            // todo
//            NotificationWrapper.showNotification(
//                this,
//                "$packageName-${getString(R.string.app_name)}",
//                getString(R.string.app_name),
//                NotificationManager.IMPORTANCE_LOW,
//                setChannelSpace = {
//                    it.description = "App notification channel"
//                    it.setShowBadge(false)
//                },
//                setNotificationBuilderSpace = {
//                    val intent = Intent(baseContext, ActivityBasicNotificationSample::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                    val pendingIntent = PendingIntent.getActivity(
//                        baseContext, 0,
//                        intent, PendingIntent.FLAG_IMMUTABLE
//                    )
//
//                    it.setSmallIcon(R.mipmap.ic_launcher)
//                    it.setContentTitle("인라인 노티")
//                    it.setContentText("인라인 노티 본문입니다.")
//
//                    val userIcon1 = IconCompat.createWithResource(this, R.mipmap.ic_launcher)
//                    val userIcon2 = IconCompat.createWithResource(this, R.mipmap.ic_launcher)
//                    val userIcon3 = IconCompat.createWithResource(this, R.mipmap.ic_launcher)
//                    val userName1 = "Chacha"
//                    val userName2 = "Android"
//                    val userName3 = "JS"
//                    val timestamp = System.currentTimeMillis()
//                    val user1 = Person.Builder().setIcon(userIcon1).setName(userName1).build()
//                    val user2 = Person.Builder().setIcon(userIcon2).setName(userName2).build()
//                    val user3 = Person.Builder().setIcon(userIcon3).setName(userName3).build()
//                    val style = NotificationCompat.MessagingStyle(user3)
//                    style.addMessage("You can get great deals there", timestamp, user1)
//                    style.addMessage("I know what to get", timestamp, user2)
//                    it.setStyle(style)
//                    it.priority = NotificationCompat.PRIORITY_DEFAULT
//                    it.setAutoCancel(true)
//
//                    val replyLabel = "Enter your reply here"
//                    val remoteInput = RemoteInput.Builder("key_reply")
//                        .setLabel(replyLabel)
//                        .build()
//                    val replyAction = NotificationCompat.Action.Builder(
//                        android.R.drawable.sym_action_chat, "REPLY", pendingIntent
//                    )
//                        .addRemoteInput(remoteInput)
//                        .setAllowGeneratedReplies(true)
//                        .build()
//                    it.addAction(replyAction)
//                    it.addAction(
//                        android.R.drawable.ic_menu_close_clear_cancel,
//                        "DISMISS", pendingIntent
//                    );
//                },
//                7
//            )
        }

        bindingMbr.headUpNotificationBtn.setOnClickListener {
            // todo
//            val NOTIFICATION_ID = 1001;
//            createNotificationChannel(this, NotificationManagerCompat.IMPORTANCE_HIGH, false,
//                getString(R.string.app_name), "App notification channel")   // 1
//
//            val channelId = "$packageName-${getString(R.string.app_name)}"
//            val title = "Android Developer"
//            val content = "Notifications in Android P"
//
//            val intent = Intent(baseContext, NewActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            val fullScreenPendingIntent = PendingIntent.getActivity(baseContext, 0,
//                intent, PendingIntent.FLAG_UPDATE_CURRENT)    // 2
//
//            val builder = NotificationCompat.Builder(this, channelId)
//            builder.setSmallIcon(R.drawable.ic_codechacha)
//            builder.setContentTitle(title)
//            builder.setContentText(content)
//            builder.priority = NotificationCompat.PRIORITY_HIGH   // 3
//            builder.setAutoCancel(true)
//            builder.setFullScreenIntent(fullScreenPendingIntent, true)   // 4
//
//            val notificationManager = NotificationManagerCompat.from(this)
//            notificationManager.notify(NOTIFICATION_ID, builder.build())
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
            currentUserUidMbr = currentLoginSessionInfoSpwMbr.sessionToken
            getScreenDataAndShow()

        } else {
            // (onResume - (권한이 충족된 onCreate))

            // (유저별 데이터 갱신)
            // : 유저 정보가 갱신된 상태에서 다시 현 액티비티로 복귀하면 자동으로 데이터를 다시 갱신합니다.
            val sessionToken = currentLoginSessionInfoSpwMbr.sessionToken
            if (sessionToken != currentUserUidMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                currentUserUidMbr = sessionToken

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


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}