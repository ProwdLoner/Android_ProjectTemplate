package com.example.prowd_android_template.activity_set.activity_easy_lut_sample

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.example.prowd_android_template.abstract_class.AbstractProwdRecyclerViewAdapter
import com.example.prowd_android_template.activity_set.activity_pinch_image_viewer.ActivityPinchImageViewer
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityEasyLutSampleBinding
import java.io.File
import java.io.FileOutputStream

// todo : 신코드 적용
// todo : easy lut 라이브러리 코드 정리
// todo : recyclerview 정리
class ActivityEasyLutSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityEasyLutSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityEasyLutSampleViewModel

    // (어뎁터 객체)
    lateinit var adapterSetMbr: ActivityEasyLutSampleAdapterSet

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

        // (초기 객체 생성)
        createMemberObjects()

        // (초기 뷰 설정)
        viewSetting()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()

        // (뷰 객체 바인딩)
        // 여기까지는 화면이 나오지 않으니 앞의 작업은 가벼워야함
        setContentView(bindingMbr.root)

        // (이외 생명주기 로직)
        onCreateLogic()
    }

    override fun onResume() {
        super.onResume()

        // (데이터 갱신 시점 적용)
        val sessionToken = viewModelMbr.currentLoginSessionInfoSpwMbr.userUid

        if (viewModelMbr.isDataFirstLoadingMbr || // 데이터 최초 로딩 시점일 때 혹은,
            sessionToken != viewModelMbr.currentUserSessionTokenMbr // 액티비티 유저와 세션 유저가 다를 때
        ) {
            // 진입 플래그 변경
            viewModelMbr.isDataFirstLoadingMbr = false
            viewModelMbr.currentUserSessionTokenMbr = sessionToken

            // [데이터 로딩]
            // (리사이클러 뷰 어뎁터)
            viewModelMbr.isRecyclerViewItemLoadingMbr = true
            // 세마포어 acquire 를 위한 별도 스레드 실행
            viewModelMbr.executorServiceMbr?.execute {
                viewModelMbr.recyclerViewAdapterItemSemaphore.acquire()
                runOnUiThread {
                    // (로딩 처리)
                    // 화면을 비우고 로더 추가
                    viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                        arrayListOf(
                            ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
                                adapterSetMbr.recyclerViewAdapter.nextItemUidMbr
                            )
                        )

                    // (데이터 준비)
                    // 숫자 타이틀에 따른 정렬 콜백
                    val numTitleComp =
                        Comparator { a: String,
                                     b: String ->
                            val num1 = a.replace("[^0-9]".toRegex(), "").toInt()
                            val num2 = b.replace("[^0-9]".toRegex(), "").toInt()
                            num1 - num2
                        }

                    val wideFilterFileList = assets.list("lut_filters_wide")!!
                    wideFilterFileList.sortWith(
                        numTitleComp
                    )

                    val haldFilterFileList = assets.list("lut_filters_hald")!!
                    haldFilterFileList.sortWith(
                        numTitleComp
                    )

                    val squareFilterFileList = assets.list("lut_filters_square")!!
                    squareFilterFileList.sortWith(
                        numTitleComp
                    )

                    val adapterDataList =
                        ArrayList<ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO>()

                    for (filterFile in wideFilterFileList) {
                        adapterDataList.add(
                            ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                                filterFile.split(".")[0]
                            )
                        )
                    }

                    for (filterFile in haldFilterFileList) {
                        adapterDataList.add(
                            ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                                filterFile.split(".")[0]
                            )
                        )
                    }

                    for (filterFile in squareFilterFileList) {
                        adapterDataList.add(
                            ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                                filterFile.split(".")[0]
                            )
                        )
                    }

                    // 로더 제거
                    viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value = ArrayList()

                    // 아이템 반영
                    viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                        adapterDataList as ArrayList<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>

                    // 이전에 선택되었던 필터명을 가져오기
                    val selectedFilterName = viewModelMbr.thisSpw.selectedFilterName

                    if (null != selectedFilterName) {
                        // 이전에 선택된 필터명의 위치에 따른 처리 및 해당 위치 스크롤
                        val selectedFilterIdx =
                            adapterDataList.indexOfFirst { it.title == selectedFilterName }

                        if (selectedFilterIdx != -1) {
                            adapterSetMbr.recyclerViewAdapter.selectedItemPosition =
                                selectedFilterIdx
                            bindingMbr.filterList.scrollToPosition(selectedFilterIdx)

                        }
                    }

                    viewModelMbr.recyclerViewAdapterItemSemaphore.release()
                    viewModelMbr.isRecyclerViewItemLoadingMbr = false
                }
            }
        }
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
        // 뷰 객체
        bindingMbr = ActivityEasyLutSampleBinding.inflate(layoutInflater)

        // 뷰 모델 객체 생성
        viewModelMbr =
            ViewModelProvider(this)[ActivityEasyLutSampleViewModel::class.java]

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityEasyLutSampleAdapterSet(
            ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter(
                this,
                bindingMbr.filterList,
                false,
                1,
                null
            )
        )
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        bindingMbr.image1OriginImage.setOnClickListener {
            val imageDrawable = bindingMbr.image1OriginImage.drawable

            if (imageDrawable is BitmapDrawable) {
                val imageBitmap = imageDrawable.bitmap

                // 비트맵을 파일로 저장
                val tempFile = File(cacheDir, "temp.jpg")
                tempFile.createNewFile()
                val out = FileOutputStream(tempFile)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()

                val intent =
                    Intent(
                        this,
                        ActivityPinchImageViewer::class.java
                    )
                intent.putExtra("image_file_path", tempFile.absolutePath)

                startActivity(intent)
            }
        }

        bindingMbr.image1FilteredImage.setOnClickListener {
            val imageDrawable = bindingMbr.image1FilteredImage.drawable

            if (imageDrawable is BitmapDrawable) {
                val imageBitmap = imageDrawable.bitmap

                // 비트맵을 파일로 저장
                val tempFile = File(cacheDir, "temp.jpg")
                tempFile.createNewFile()
                val out = FileOutputStream(tempFile)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()

                val intent =
                    Intent(
                        this,
                        ActivityPinchImageViewer::class.java
                    )
                intent.putExtra("image_file_path", tempFile.absolutePath)

                startActivity(intent)
            }
        }

        bindingMbr.image2OriginImage.setOnClickListener {
            val imageDrawable = bindingMbr.image2OriginImage.drawable

            if (imageDrawable is BitmapDrawable) {
                val imageBitmap = imageDrawable.bitmap

                // 비트맵을 파일로 저장
                val tempFile = File(cacheDir, "temp.jpg")
                tempFile.createNewFile()
                val out = FileOutputStream(tempFile)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()

                val intent =
                    Intent(
                        this,
                        ActivityPinchImageViewer::class.java
                    )
                intent.putExtra("image_file_path", tempFile.absolutePath)

                startActivity(intent)
            }
        }

        bindingMbr.image2FilteredImage.setOnClickListener {
            val imageDrawable = bindingMbr.image2FilteredImage.drawable

            if (imageDrawable is BitmapDrawable) {
                val imageBitmap = imageDrawable.bitmap

                // 비트맵을 파일로 저장
                val tempFile = File(cacheDir, "temp.jpg")
                tempFile.createNewFile()
                val out = FileOutputStream(tempFile)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()

                val intent =
                    Intent(
                        this,
                        ActivityPinchImageViewer::class.java
                    )
                intent.putExtra("image_file_path", tempFile.absolutePath)

                startActivity(intent)
            }
        }

        bindingMbr.image3OriginImage.setOnClickListener {
            val imageDrawable = bindingMbr.image3OriginImage.drawable

            if (imageDrawable is BitmapDrawable) {
                val imageBitmap = imageDrawable.bitmap

                // 비트맵을 파일로 저장
                val tempFile = File(cacheDir, "temp.jpg")
                tempFile.createNewFile()
                val out = FileOutputStream(tempFile)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()

                val intent =
                    Intent(
                        this,
                        ActivityPinchImageViewer::class.java
                    )
                intent.putExtra("image_file_path", tempFile.absolutePath)

                startActivity(intent)
            }
        }

        bindingMbr.image3FilteredImage.setOnClickListener {
            val imageDrawable = bindingMbr.image3FilteredImage.drawable

            if (imageDrawable is BitmapDrawable) {
                val imageBitmap = imageDrawable.bitmap

                // 비트맵을 파일로 저장
                val tempFile = File(cacheDir, "temp.jpg")
                tempFile.createNewFile()
                val out = FileOutputStream(tempFile)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()

                val intent =
                    Intent(
                        this,
                        ActivityPinchImageViewer::class.java
                    )
                intent.putExtra("image_file_path", tempFile.absolutePath)

                startActivity(intent)
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

        viewModelMbr.filteredImage1LiveDataMbr.observe(this) {
            if (it == null) {
                if (!isFinishing && !isDestroyed) {
                    bindingMbr.image1FilteredImage.setImageResource(android.R.color.transparent)
                }
            } else {
                if (!isFinishing && !isDestroyed) {
                    Glide.with(this)
                        .load(it)
                        .transform(CenterCrop())
                        .into(bindingMbr.image1FilteredImage)
                }
            }
        }

        viewModelMbr.filteredImage2LiveDataMbr.observe(this) {
            if (it == null) {
                if (!isFinishing && !isDestroyed) {
                    bindingMbr.image2FilteredImage.setImageResource(android.R.color.transparent)
                }
            } else {
                if (!isFinishing && !isDestroyed) {
                    Glide.with(this)
                        .load(it)
                        .transform(CenterCrop())
                        .into(bindingMbr.image2FilteredImage)
                }
            }
        }

        viewModelMbr.filteredImage3LiveDataMbr.observe(this) {
            if (it == null) {
                if (!isFinishing && !isDestroyed) {
                    bindingMbr.image3FilteredImage.setImageResource(android.R.color.transparent)
                }
            } else {
                if (!isFinishing && !isDestroyed) {
                    Glide.with(this)
                        .load(it)
                        .transform(CenterCrop())
                        .into(bindingMbr.image3FilteredImage)
                }
            }
        }

        // 리사이클러 뷰 어뎁터 데이터 바인딩
        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.observe(this) {
            adapterSetMbr.recyclerViewAdapter.setItemList(it)
        }
    }

    private fun onCreateLogic() {

    }
}