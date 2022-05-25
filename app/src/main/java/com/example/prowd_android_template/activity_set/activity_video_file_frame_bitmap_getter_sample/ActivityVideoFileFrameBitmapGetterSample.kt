package com.example.prowd_android_template.activity_set.activity_video_file_frame_bitmap_getter_sample

import android.content.res.AssetFileDescriptor
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.R
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityVideoFileFrameBitmapGetterSampleBinding
import com.example.prowd_android_template.native_wrapper.NativeWrapperFFMpegWrapper


class ActivityVideoFileFrameBitmapGetterSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityVideoFileFrameBitmapGetterSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityVideoFileFrameBitmapGetterSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null

    // 비디오 뷰에 띄워줄 영상 관련 플레이어 객체
    private var videoViewMediaPlayerMbr: MediaPlayer? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityVideoFileFrameBitmapGetterSampleBinding.inflate(layoutInflater)
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

        // 영상 플레이어 설정
        val assetFileDescriptor: AssetFileDescriptor = application.resources.openRawResourceFd(
            R.raw.video_activity_video_file_frame_bitmap_getter_sample_test
        )

        videoViewMediaPlayerMbr = MediaPlayer()
        videoViewMediaPlayerMbr!!.setDataSource(
            assetFileDescriptor.fileDescriptor,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.length
        )
        assetFileDescriptor.close()
        videoViewMediaPlayerMbr!!.isLooping = true

        bindingMbr.sampleVideoView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                videoViewMediaPlayerMbr!!.setDisplay(holder)
                videoViewMediaPlayerMbr!!.prepareAsync()

                videoViewMediaPlayerMbr!!.setOnPreparedListener {
                    // 뷰 모델에 저장된 이전 위치 설정
                    videoViewMediaPlayerMbr!!.seekTo(viewModelMbr.videoViewMediaPlayerPositionMsMbr)
                    videoViewMediaPlayerMbr!!.setOnSeekCompleteListener { videoViewMediaPlayerMbr!!.start() }
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        })

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

    override fun onPause() {
        super.onPause()
        viewModelMbr.videoViewMediaPlayerPositionMsMbr = videoViewMediaPlayerMbr!!.currentPosition
        videoViewMediaPlayerMbr!!.pause()
        videoViewMediaPlayerMbr!!.stop()
        videoViewMediaPlayerMbr!!.reset()
        videoViewMediaPlayerMbr!!.release()
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
        progressLoadingDialogMbr?.dismiss()

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
            ViewModelProvider(this)[ActivityVideoFileFrameBitmapGetterSampleViewModel::class.java]
    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr =
                viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken

            // 영상 크기 구하기
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, viewModelMbr.sampleVideoFileUriMbr)
            viewModelMbr.sampleVideoWidthMbr =
                Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!)
            viewModelMbr.sampleVideoHeightMbr =
                Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!)
            retriever.release()

        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 비디오 뷰 비율을 비디오 파일 크기에 맞추기
        val videoLayoutParams =
            bindingMbr.sampleVideoView.layoutParams as ConstraintLayout.LayoutParams
        videoLayoutParams.dimensionRatio =
            "${viewModelMbr.sampleVideoWidthMbr}:${viewModelMbr.sampleVideoHeightMbr}"
        bindingMbr.sampleVideoView.layoutParams = videoLayoutParams

        // 추출 프레임 이미지 뷰 비율을 비디오 파일 크기에 맞추기
        val imageLayoutParams =
            bindingMbr.videoFrameImageView.layoutParams as ConstraintLayout.LayoutParams
        imageLayoutParams.dimensionRatio =
            "${viewModelMbr.sampleVideoWidthMbr}:${viewModelMbr.sampleVideoHeightMbr}"
        bindingMbr.videoFrameImageView.layoutParams = imageLayoutParams


        NativeWrapperFFMpegWrapper.libTest()
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