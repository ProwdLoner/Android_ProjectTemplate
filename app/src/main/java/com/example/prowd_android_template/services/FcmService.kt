package com.example.prowd_android_template.services

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.example.prowd_android_template.R
import com.example.prowd_android_template.util_object.NotificationWrapper
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


// [FCM 푸시 메세지 수신 서비스]
// FCM 관련 설정은 build.gradle 에서의 종속성 설정과 AndroidManifest.xml 의 service 설정이 있고,
// 파이어베이스 설정은 app 디렉토리 안의 google-services.json 파일을 발급받은 파일로 바꾸어 변경가능.
// FCM 메세지는 앱 상태에 상관 없이 수신 가능함.
// 예를들어 디바이스 종료 상태에 n번의 메세지를 받으면, 디바이스가 켜지고 시간이 지나고 한꺼번에 수신됨.
// 단, 밀린 메세지의 수신은 디바이스가 앱이 켜진다고 바로 받는 것이 아니라 시간차가 존재하므로 이를 주의할것.
class FcmService : FirebaseMessagingService() {

    companion object {
        // FCM 토큰을 가져오는 함수
        fun getFcmTokenAsync(onSuccess: (token: String) -> Unit, onFailed: () -> Unit) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess(task.result)
                } else {
                    onFailed()
                }
            }
        }
    }

    // token 이 생성되거나 변하면 실행되는 함수
    override fun onNewToken(token: String) {
        super.onNewToken(token)

    }

    // PUSH 메세지가 수신되면 실행되는 함수
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // 수신한 메시지를 처리
        // notification 이 아니라 data 로만 온 정보를 처리하기로 결정.

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
                it.setContentTitle(remoteMessage.data["title"])
                it.setContentText(remoteMessage.data["body"])
                it.priority = NotificationCompat.PRIORITY_DEFAULT
                it.setAutoCancel(true)
            },
            1
        )
    }
}