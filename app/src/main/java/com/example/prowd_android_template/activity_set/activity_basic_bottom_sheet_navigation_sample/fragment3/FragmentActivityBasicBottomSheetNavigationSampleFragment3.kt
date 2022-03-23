package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment3

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.ActivityBasicBottomSheetNavigationSample
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.ActivityBasicBottomSheetNavigationSampleViewModel
import com.example.prowd_android_template.databinding.FragmentActivityBasicBottomSheetNavigationSampleFragment3Binding

class FragmentActivityBasicBottomSheetNavigationSampleFragment3 : Fragment() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: FragmentActivityBasicBottomSheetNavigationSampleFragment3Binding

    // (부모 객체) : 뷰 모델 구조 구현 및 부모 및 플래그먼트 간의 통신용
    private lateinit var parentActivity: ActivityBasicBottomSheetNavigationSample
    private lateinit var parentViewModel: ActivityBasicBottomSheetNavigationSampleViewModel


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // (뷰 바인딩)
        bindingMbr =
            FragmentActivityBasicBottomSheetNavigationSampleFragment3Binding.inflate(layoutInflater)

        // (부모 객체 저장)
        parentActivity = requireActivity() as ActivityBasicBottomSheetNavigationSample
        parentViewModel = parentActivity.viewModelMbr

        // (초기 객체 생성)
        createMemberObjects()
        // 뷰모델 저장 객체 생성 = 뷰모델 내에 저장되어 destroy 까지 쭉 유지되는 데이터 초기화
        createViewModelDataObjects()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()

        // (초기 뷰 설정)
        viewSetting()

        return bindingMbr.root
    }

    override fun onResume() {
        super.onResume()

        // (데이터 갱신 시점 적용)
        if (!parentViewModel.isChangingConfigurationsMbr && // 화면 회전이 아니면서,
            isVisible // 현재 보이는 상황일 때
        ) {
            val sessionToken = parentViewModel.currentLoginSessionInfoSpwMbr.sessionToken

            if (parentViewModel.fragment3Data.isDataFirstLoadingMbr || // 데이터 최초 로딩 시점일 때 혹은,
                sessionToken != parentViewModel.fragment3Data.currentUserSessionTokenMbr // 액티비티 유저와 세션 유저가 다를 때
            ) {
                // 진입 플래그 변경
                parentViewModel.fragment3Data.isDataFirstLoadingMbr = false
                parentViewModel.fragment3Data.currentUserSessionTokenMbr = sessionToken

                //  데이터 로딩
            }
        }

    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // ex : 어뎁터 셋 생성
    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!parentViewModel.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            // 현 액티비티 진입 유저 저장
            parentViewModel.fragment3Data.currentUserSessionTokenMbr = parentViewModel.currentLoginSessionInfoSpwMbr.sessionToken
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        bindingMbr.fragmentClickBtn.setOnClickListener {
            parentViewModel.fragmentClickedPositionLiveDataMbr.value = 3
        }

    }

    // 라이브 데이터 설정
    private fun setLiveData() {
        parentViewModel.fragmentClickedPositionLiveDataMbr.observe(parentActivity){
            if (null == it){
                bindingMbr.clickedByValueTxt.text = "클릭 없음"
            }else{
                bindingMbr.clickedByValueTxt.text = "${it}번 플래그먼트"
            }
        }

    }
}