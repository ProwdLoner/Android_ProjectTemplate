package com.example.prowd_android_template.activity_set.activity_easy_lut_sample

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.example.prowd_android_template.abstract_class.AbstractRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityEasyLutSampleBinding
import com.example.prowd_android_template.util_object.UriAndPath
import java.io.File
import java.io.FileOutputStream


// Todo : wide 외의 필터도 적용
// todo : easy lut 라이브러리 코드 정리
// todo : 선택된 필터 화면 회전 처리
class ActivityEasyLutSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityEasyLutSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityEasyLutSampleViewModel

    // (어뎁터 객체)
    private lateinit var adapterSetMbr: ActivityEasyLutSampleAdapterSet

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    private var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    private var confirmDialogMbr: DialogConfirm? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityEasyLutSampleBinding.inflate(layoutInflater)
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
                // 로딩 아이템 생성
                val adapterDataList: ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
                    arrayListOf(
                        ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
                            adapterSetMbr.recyclerViewAdapter.maxUidMbr
                        )
                    )
                viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.value = adapterDataList

                viewModelMbr.executorServiceMbr?.execute {
                    val filterFileList = assets.list("lut_filters_wide")!!.toCollection(ArrayList())
                    adapterDataList.clear()

                    for (filterFile in filterFileList) {
                        adapterDataList.add(
                            ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                filterFile
                            )
                        )
                    }

                    // 숫자 타이틀에 따른 정렬
                    val numTitleComp =
                        Comparator { a: AbstractRecyclerViewAdapter.AdapterItemAbstractVO,
                                     b: AbstractRecyclerViewAdapter.AdapterItemAbstractVO ->
                            val title1 = (a as ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).title
                            val title2 = (b as ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).title

                            val num1 = title1.replace("[^0-9]".toRegex(), "").toInt()
                            val num2 = title2.replace("[^0-9]".toRegex(), "").toInt()

                            num1 - num2
                        }

                    adapterDataList.sortWith(numTitleComp)

                    runOnUiThread {
                        // 로딩 아이템 제거 및 아이템 추가
                        viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.value =
                            ArrayList()

                        viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.value =
                            adapterDataList

                        val selectedFilterName = viewModelMbr.thisSpw.selectedFilterName

                        if (null != selectedFilterName) {
                            val selectedFilterIdx =
                                adapterDataList.indexOfFirst { (it as ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).title == selectedFilterName }

                            if (selectedFilterIdx != -1) {
                                adapterSetMbr.recyclerViewAdapter.selectedItemPosition =
                                    selectedFilterIdx
                                bindingMbr.filterList.scrollToPosition(selectedFilterIdx)

                            }
                        }
                    }
                }
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
        viewModelMbr =
            ViewModelProvider(this)[ActivityEasyLutSampleViewModel::class.java]

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityEasyLutSampleAdapterSet(
            ActivityEasyLutSampleAdapterSet.RecyclerViewAdapter(
                this,
                viewModelMbr,
                bindingMbr.filterList,
                false,
                null
            )
        )
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
        bindingMbr.image1OriginImage.setOnClickListener {
            val imageBitmap = (bindingMbr.image1OriginImage.drawable as BitmapDrawable).bitmap

            val imageFileName =
                "ActivityMainIntentExtraTemp.jpg"
            val imageFile =
                File(cacheDir, imageFileName)
            if (imageFile.exists()) {
                imageFile.delete()
            }

            imageFile.createNewFile()
            val warpFileOut =
                FileOutputStream(imageFile)
            imageBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                warpFileOut
            )
            warpFileOut.close()

            val gotoImageViewerIntent = Intent()
            gotoImageViewerIntent.action = Intent.ACTION_VIEW
            gotoImageViewerIntent.setDataAndType(
                UriAndPath.getUriFromPath(imageFile.absolutePath),
                "image/*"
            )
            startActivity(gotoImageViewerIntent)

        }

        bindingMbr.image1FilteredImage.setOnClickListener {
            val imageBitmap = (bindingMbr.image1FilteredImage.drawable as BitmapDrawable).bitmap

            val imageFileName =
                "ActivityMainIntentExtraTemp.jpg"
            val imageFile =
                File(cacheDir, imageFileName)
            if (imageFile.exists()) {
                imageFile.delete()
            }

            imageFile.createNewFile()
            val warpFileOut =
                FileOutputStream(imageFile)
            imageBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                warpFileOut
            )
            warpFileOut.close()

            val gotoImageViewerIntent = Intent()
            gotoImageViewerIntent.action = Intent.ACTION_VIEW
            gotoImageViewerIntent.setDataAndType(
                UriAndPath.getUriFromPath(imageFile.absolutePath),
                "image/*"
            )
            startActivity(gotoImageViewerIntent)

        }

        bindingMbr.image2OriginImage.setOnClickListener {
            val imageBitmap = (bindingMbr.image2OriginImage.drawable as BitmapDrawable).bitmap

            val imageFileName =
                "ActivityMainIntentExtraTemp.jpg"
            val imageFile =
                File(cacheDir, imageFileName)
            if (imageFile.exists()) {
                imageFile.delete()
            }

            imageFile.createNewFile()
            val warpFileOut =
                FileOutputStream(imageFile)
            imageBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                warpFileOut
            )
            warpFileOut.close()

            val gotoImageViewerIntent = Intent()
            gotoImageViewerIntent.action = Intent.ACTION_VIEW
            gotoImageViewerIntent.setDataAndType(
                UriAndPath.getUriFromPath(imageFile.absolutePath),
                "image/*"
            )
            startActivity(gotoImageViewerIntent)

        }

        bindingMbr.image2FilteredImage.setOnClickListener {
            val imageBitmap = (bindingMbr.image2FilteredImage.drawable as BitmapDrawable).bitmap

            val imageFileName =
                "ActivityMainIntentExtraTemp.jpg"
            val imageFile =
                File(cacheDir, imageFileName)
            if (imageFile.exists()) {
                imageFile.delete()
            }

            imageFile.createNewFile()
            val warpFileOut =
                FileOutputStream(imageFile)
            imageBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                warpFileOut
            )
            warpFileOut.close()

            val gotoImageViewerIntent = Intent()
            gotoImageViewerIntent.action = Intent.ACTION_VIEW
            gotoImageViewerIntent.setDataAndType(
                UriAndPath.getUriFromPath(imageFile.absolutePath),
                "image/*"
            )
            startActivity(gotoImageViewerIntent)

        }

        bindingMbr.image3OriginImage.setOnClickListener {
            val imageBitmap = (bindingMbr.image3OriginImage.drawable as BitmapDrawable).bitmap

            val imageFileName =
                "ActivityMainIntentExtraTemp.jpg"
            val imageFile =
                File(cacheDir, imageFileName)
            if (imageFile.exists()) {
                imageFile.delete()
            }

            imageFile.createNewFile()
            val warpFileOut =
                FileOutputStream(imageFile)
            imageBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                warpFileOut
            )
            warpFileOut.close()

            val gotoImageViewerIntent = Intent()
            gotoImageViewerIntent.action = Intent.ACTION_VIEW
            gotoImageViewerIntent.setDataAndType(
                UriAndPath.getUriFromPath(imageFile.absolutePath),
                "image/*"
            )
            startActivity(gotoImageViewerIntent)

        }

        bindingMbr.image3FilteredImage.setOnClickListener {
            val imageBitmap = (bindingMbr.image3FilteredImage.drawable as BitmapDrawable).bitmap

            val imageFileName =
                "ActivityMainIntentExtraTemp.jpg"
            val imageFile =
                File(cacheDir, imageFileName)
            if (imageFile.exists()) {
                imageFile.delete()
            }

            imageFile.createNewFile()
            val warpFileOut =
                FileOutputStream(imageFile)
            imageBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                warpFileOut
            )
            warpFileOut.close()

            val gotoImageViewerIntent = Intent()
            gotoImageViewerIntent.action = Intent.ACTION_VIEW
            gotoImageViewerIntent.setDataAndType(
                UriAndPath.getUriFromPath(imageFile.absolutePath),
                "image/*"
            )
            startActivity(gotoImageViewerIntent)

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

        // recyclerViewAdapter 데이터 리스트
        viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.observe(this) {
            adapterSetMbr.recyclerViewAdapter.setNewItemListAll(it)
        }

        viewModelMbr.filteredImage1LiveDataMbr.observe(this) {
            if (it == null) {
                if (!isFinishing) {
                    bindingMbr.image1FilteredImage.setImageResource(android.R.color.transparent)
                }
            } else {
                if (!isFinishing) {
                    Glide.with(this)
                        .load(it)
                        .transform(CenterCrop())
                        .into(bindingMbr.image1FilteredImage)
                }
            }
        }

        viewModelMbr.filteredImage2LiveDataMbr.observe(this) {
            if (it == null) {
                if (!isFinishing) {
                    bindingMbr.image2FilteredImage.setImageResource(android.R.color.transparent)
                }
            } else {
                if (!isFinishing) {
                    Glide.with(this)
                        .load(it)
                        .transform(CenterCrop())
                        .into(bindingMbr.image2FilteredImage)
                }
            }
        }

        viewModelMbr.filteredImage3LiveDataMbr.observe(this) {
            if (it == null) {
                if (!isFinishing) {
                    bindingMbr.image3FilteredImage.setImageResource(android.R.color.transparent)
                }
            } else {
                if (!isFinishing) {
                    Glide.with(this)
                        .load(it)
                        .transform(CenterCrop())
                        .into(bindingMbr.image3FilteredImage)
                }
            }
        }
    }
}