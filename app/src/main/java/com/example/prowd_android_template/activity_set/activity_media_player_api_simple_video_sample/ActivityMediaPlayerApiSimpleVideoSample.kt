package com.example.prowd_android_template.activity_set.activity_media_player_api_simple_video_sample

import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.R
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityMediaPlayerApiSimpleVideoSampleBinding

class ActivityMediaPlayerApiSimpleVideoSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityMediaPlayerApiSimpleVideoSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityMediaPlayerApiSimpleVideoSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null

    // 라디오 버튼 다이얼로그
    var radioBtnDialogMbr: DialogRadioButtonChoose? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityMediaPlayerApiSimpleVideoSampleBinding.inflate(layoutInflater)
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

        viewModelMbr.simpleVideoMediaPlayerMbr?.start()

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

    override fun onPause() {
        viewModelMbr.simpleVideoMediaPlayerMbr?.pause()

        super.onPause()
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
        progressLoadingDialogMbr?.dismiss()
        radioBtnDialogMbr?.dismiss()

        // 화면 회전시 재생 시간 저장
        viewModelMbr.simpleVideoMediaPlayerPositionMbr =
            viewModelMbr.simpleVideoMediaPlayerMbr?.currentPosition
        viewModelMbr.simpleVideoMediaPlayerMbr?.release()
        viewModelMbr.simpleVideoMediaPlayerMbr = null

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr =
            ViewModelProvider(this)[ActivityMediaPlayerApiSimpleVideoSampleViewModel::class.java]
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 초기 미디어 소스 설정
        viewModelMbr.simpleVideoMediaPlayerMbr = MediaPlayer()
        val videoUri: Uri =
            Uri.parse("android.resource://" + packageName + "/" + R.raw.video_common_video_sample)
        viewModelMbr.simpleVideoMediaPlayerMbr!!.setDataSource(
            this,
            videoUri
        )

        // 영상 크기 구하기
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, videoUri)
        val videoWidth =
            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!)
        val videoHeight =
            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!)
        retriever.release()

        // 영상 사이즈에 맞게 뷰 크기를 변경
        val videoLayoutParams =
            bindingMbr.simpleVideo.layoutParams as ConstraintLayout.LayoutParams
        videoLayoutParams.dimensionRatio =
            "${videoWidth}:${videoHeight}"
        bindingMbr.simpleVideo.layoutParams = videoLayoutParams

        viewModelMbr.simpleVideoMediaPlayerMbr!!.prepare()

        // 화면 회전시 기존 저장된 재생 시간부터 시작
        if (viewModelMbr.simpleVideoMediaPlayerPositionMbr != null) {
            viewModelMbr.simpleVideoMediaPlayerMbr!!.seekTo(viewModelMbr.simpleVideoMediaPlayerPositionMbr!!)
            viewModelMbr.simpleVideoMediaPlayerPositionMbr = null
        }
        viewModelMbr.simpleVideoMediaPlayerMbr!!.isLooping = true

        // MediaPlayer 출력 뷰 설정
        bindingMbr.simpleVideo.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    viewModelMbr.simpleVideoMediaPlayerMbr!!.setSurface(Surface(p0))
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) =
                    Unit

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) = Unit
            }

//        bindingMbr.simpleVideo.alpha = 0.2f
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
    }
}