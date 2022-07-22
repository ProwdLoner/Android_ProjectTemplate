package com.example.prowd_android_template.activity_set.activity_basic_notification_sample

import android.app.NotificationManager
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.R
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityBasicNotificationSampleBinding
import com.example.prowd_android_template.util_object.NotificationWrapper

class ActivityBasicNotificationSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicNotificationSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityBasicNotificationSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null

    // 라디오 버튼 선택 다이얼로그
    var radioButtonChooseDialogMbr: DialogRadioButtonChoose? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBasicNotificationSampleBinding.inflate(layoutInflater)
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
        radioButtonChooseDialogMbr?.dismiss()

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ActivityBasicNotificationSampleViewModel::class.java]

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

        // progressSample2 진행도
        viewModelMbr.progressDialogSample2ProgressValue.observe(this) {
            if (it != -1) {
                val loadingText = "로딩중 $it%"
                progressLoadingDialogMbr?.bindingMbr?.progressMessageTxt?.text = loadingText
                progressLoadingDialogMbr?.bindingMbr?.progressBar?.visibility = View.VISIBLE
                progressLoadingDialogMbr?.bindingMbr?.progressBar?.progress = it
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

        // 라디오 버튼 선택 다이얼로그 출력 플래그
        viewModelMbr.radioButtonChooseDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                radioButtonChooseDialogMbr?.dismiss()

                radioButtonChooseDialogMbr = DialogRadioButtonChoose(
                    this,
                    it
                )
                radioButtonChooseDialogMbr?.show()
            } else {
                radioButtonChooseDialogMbr?.dismiss()
                radioButtonChooseDialogMbr = null
            }
        }
    }
}