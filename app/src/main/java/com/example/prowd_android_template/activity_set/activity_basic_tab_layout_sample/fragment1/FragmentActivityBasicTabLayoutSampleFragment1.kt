package com.example.prowd_android_template.activity_set.activity_basic_tab_layout_sample.fragment1

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.prowd_android_template.activity_set.activity_basic_tab_layout_sample.ActivityBasicTabLayoutSample
import com.example.prowd_android_template.databinding.FragmentActivityBasicTabLayoutSample1Binding

class FragmentActivityBasicTabLayoutSampleFragment1 : Fragment() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: FragmentActivityBasicTabLayoutSample1Binding

    // (부모 객체) : 뷰 모델 구조 구현 및 부모 및 플래그먼트 간의 통신용
    lateinit var parentActivityMbr: ActivityBasicTabLayoutSample

    // (뷰 모델 객체)
    lateinit var viewModelMbr: FragmentActivityBasicTabLayoutSampleFragment1VmData

    // (Ui 스레드 핸들러 객체) handler.post{}
    var uiThreadHandlerMbr: Handler = Handler(Looper.getMainLooper())


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // (초기 객체 생성)
        createMemberObjects()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()

        // (초기 뷰 설정)
        viewSetting()

        return bindingMbr.root
    }

    override fun onResume() {
        super.onResume()

        // (데이터 갱신 시점 적용)
        if (!parentActivityMbr.viewModelMbr.isChangingConfigurationsMbr && // 화면 회전이 아니면서,
            isVisible // 현재 보이는 상황일 때
        ) {
            val sessionToken =
                parentActivityMbr.viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken

            if (parentActivityMbr.viewModelMbr.fragment1DataMbr.isDataFirstLoadingMbr || // 데이터 최초 로딩 시점일 때 혹은,
                sessionToken != parentActivityMbr.viewModelMbr.fragment1DataMbr.currentUserSessionTokenMbr // 액티비티 유저와 세션 유저가 다를 때
            ) {
                // 진입 플래그 변경
                parentActivityMbr.viewModelMbr.fragment1DataMbr.isDataFirstLoadingMbr = false
                parentActivityMbr.viewModelMbr.fragment1DataMbr.currentUserSessionTokenMbr =
                    sessionToken

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
        // (뷰 바인딩)
        bindingMbr =
            FragmentActivityBasicTabLayoutSample1Binding.inflate(layoutInflater)

        // (부모 객체 저장)
        parentActivityMbr = requireActivity() as ActivityBasicTabLayoutSample

        // (플래그먼트 뷰모델)
        viewModelMbr = parentActivityMbr.viewModelMbr.fragment1DataMbr
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        bindingMbr.fragmentClickBtn.setOnClickListener {
            parentActivityMbr.viewModelMbr.fragmentClickedPositionLiveDataMbr.value = 1
        }

    }

    // 라이브 데이터 설정
    private fun setLiveData() {
        parentActivityMbr.viewModelMbr.fragmentClickedPositionLiveDataMbr.observe(parentActivityMbr) {
            if (null == it) {
                bindingMbr.clickedByValueTxt.text = "클릭 없음"
            } else {
                val textMsg = "${it}번 플래그먼트"
                bindingMbr.clickedByValueTxt.text = textMsg
            }
        }
    }
}