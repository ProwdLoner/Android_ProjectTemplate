package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.R
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment1.FragmentActivityBasicBottomSheetNavigationSampleFragment1
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment2.FragmentActivityBasicBottomSheetNavigationSampleFragment2
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment3.FragmentActivityBasicBottomSheetNavigationSampleFragment3
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicBottomSheetNavigationSampleBinding

class ActivityBasicBottomSheetNavigationSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityBasicBottomSheetNavigationSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityBasicBottomSheetNavigationSampleViewModel

    // (어뎁터 객체)
    private lateinit var adapterSetMbr: ActivityBasicBottomSheetNavigationSampleAdapterSet

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 네트워크 에러 다이얼로그(타임아웃 등 retrofit 반환 에러)
    private var networkErrorDialogMbr: DialogConfirm? = null

    // 서버 에러 다이얼로그(정해진 서버 반환 코드 외의 상황)
    private var serverErrorDialogMbr: DialogConfirm? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBasicBottomSheetNavigationSampleBinding.inflate(layoutInflater)
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
            val loginInfo = viewModelMbr.gvcCurrentLoginSessionInfoMbr.getData()

            val sessionToken =loginInfo.sessionToken

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
        networkErrorDialogMbr?.dismiss()
        serverErrorDialogMbr?.dismiss()
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
            ViewModelProvider(this)[ActivityBasicBottomSheetNavigationSampleViewModel::class.java]

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityBasicBottomSheetNavigationSampleAdapterSet(
            ActivityBasicBottomSheetNavigationSampleAdapterSet.ScreenViewPagerFragmentStateAdapter(
                this
            )
        )
    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동
            val loginInfo = viewModelMbr.gvcCurrentLoginSessionInfoMbr.getData()

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr =loginInfo.sessionToken
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 프레그먼트 컨테이너 조작 금지
        bindingMbr.screenViewPager.isUserInputEnabled = false

        // 프레그먼트 어뎁터 연결
        bindingMbr.screenViewPager.adapter = adapterSetMbr.screenViewPagerFragmentStateAdapter

        // 플래그먼트는 화면 회전시 초기화 수준이 아니라 다시 생성
        // 부모 뷰 모델에 화면 정보를 저장하고, 동작 플래그도 저장하여 반영

        // 플래그먼트 생성 (화면 회전시 에러가 안나기 위하여 기본 생성자를 사용할 것)
        val fragment1 = FragmentActivityBasicBottomSheetNavigationSampleFragment1()
        val fragment2 = FragmentActivityBasicBottomSheetNavigationSampleFragment2()
        val fragment3 = FragmentActivityBasicBottomSheetNavigationSampleFragment3()

        adapterSetMbr.screenViewPagerFragmentStateAdapter.setItems(
            listOf(
                fragment1,
                fragment2,
                fragment3
            )
        )

        // bottom navigator 버튼에 따라 화면 프레그먼트 변경 리스너
        bindingMbr.bottomNav.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.fragment1 -> {
                    bindingMbr.screenViewPager.currentItem = 0
                    return@setOnItemSelectedListener true
                }
                R.id.fragment2 -> {
                    bindingMbr.screenViewPager.currentItem = 1
                    return@setOnItemSelectedListener true
                }
                R.id.fragment3 -> {
                    bindingMbr.screenViewPager.currentItem = 2
                    return@setOnItemSelectedListener true
                }
            }
            false
        }

    }

    // 라이브 데이터 설정
    private fun setLiveData() {
        // 로딩 다이얼로그 출력 플래그
        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
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

        // 네트워크 에러 다이얼로그 출력 플래그
        viewModelMbr.networkErrorDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                networkErrorDialogMbr = DialogConfirm(
                    this,
                    it
                )
                networkErrorDialogMbr?.show()
            } else {
                networkErrorDialogMbr?.dismiss()
                networkErrorDialogMbr = null
            }
        }

        // 서버 에러 다이얼로그 출력 플래그
        viewModelMbr.serverErrorDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                serverErrorDialogMbr = DialogConfirm(
                    this,
                    it
                )
                serverErrorDialogMbr?.show()
            } else {
                serverErrorDialogMbr?.dismiss()
                serverErrorDialogMbr = null
            }
        }
    }
}