package com.example.prowd_android_template.activity_set.activity_basic_notification_sample

import android.app.Application
import android.app.Dialog
import android.app.NotificationManager
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.R
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

class ActivityBasicNotificationSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicNotificationSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ViewModel

    // (다이얼로그 객체)
    var dialogMbr: Dialog? = null

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((MutableMap<String, Boolean>) -> Unit))? = null

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
        bindingMbr = ActivityBasicNotificationSampleBinding.inflate(layoutInflater)
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
        }
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