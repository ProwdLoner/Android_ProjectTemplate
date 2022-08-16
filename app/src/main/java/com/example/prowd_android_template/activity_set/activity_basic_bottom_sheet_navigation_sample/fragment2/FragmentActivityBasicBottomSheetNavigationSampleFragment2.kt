package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.ActivityBasicBottomSheetNavigationSample
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.databinding.FragmentActivityBasicBottomSheetNavigationSampleFragment2Binding

class FragmentActivityBasicBottomSheetNavigationSampleFragment2 : Fragment() {
    // <멤버 상수 공간>
    // (부모 객체) : 뷰 모델 구조 구현 및 부모 및 플래그먼트 간의 통신용
    lateinit var parentActivityMbr: ActivityBasicBottomSheetNavigationSample

    // (뷰 바인더 객체) : 뷰 조작에 관련된 바인더는 밖에서 조작 금지
    private lateinit var bindingMbr: FragmentActivityBasicBottomSheetNavigationSampleFragment2Binding

    // (Ui 스레드 핸들러 객체) handler.post{}
    val uiThreadHandlerMbr: Handler = Handler(Looper.getMainLooper())

    // (SharedPreference 객체)
    // 현 로그인 정보 접근 객체
    lateinit var currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (최초 실행 플래그) : 액티비티가 실행되고, 권한 체크가 끝난 후의 최초 로직이 실행되었는지 여부
    var doItAlreadyMbr = false

    // (이 화면에 도달한 유저 계정 고유값) : 세션 토큰이 없다면 비회원 상태
    var currentUserSessionTokenMbr: String? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    // : 플래그먼트 실행  = onAttach() → onCreate() → onCreateView() → onActivityCreated() → onStart() → onResume()
    //     플래그먼트 일시정지 후 재실행 = onPause() → ... -> onResume()
    //     플래그먼트 정지 후 재실행 = onPause() → onStop() -> ... -> onStart() → onResume()
    //     플래그먼트 종료 = onPause() → onStop() → onDestroyView() → onDestroy() → onDetach()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // (초기 객체 생성)
        onCreateViewInitObject()

        // (초기 뷰 설정)
        onCreateViewInitView()

        return bindingMbr.root
    }

    override fun onResume() {
        super.onResume()

        if (!doItAlreadyMbr) {
            // (onCreate + permissionGrant)
            doItAlreadyMbr = true

            // (초기 데이터 수집)

            // (알고리즘)
        } else {
            // (onResume - (onCreate + permissionGrant)) : 권한 클리어
            // (뷰 데이터 로딩)
            // : 유저가 변경되면 해당 유저에 대한 데이터로 재구축
            val sessionToken = currentLoginSessionInfoSpwMbr.sessionToken
            if (sessionToken != currentUserSessionTokenMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                currentUserSessionTokenMbr = sessionToken

                // 데이터 수집

                // (알고리즘)
            }

            // (알고리즘)
        }

        // (onResume 로직)
        // (알고리즘)
        if (parentActivityMbr.fragmentClickedPositionMbr == null){
            bindingMbr.clickedByValueTxt.text = "클릭 없음"
        }else{
            val textMsg = "${parentActivityMbr.fragmentClickedPositionMbr}번 플래그먼트"
            bindingMbr.clickedByValueTxt.text = textMsg
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    private fun onCreateViewInitObject() {
        // (부모 객체 저장)
        parentActivityMbr = requireActivity() as ActivityBasicBottomSheetNavigationSample

        currentLoginSessionInfoSpwMbr = CurrentLoginSessionInfoSpw(parentActivityMbr.application)

        // (뷰 바인딩)
        bindingMbr =
            FragmentActivityBasicBottomSheetNavigationSampleFragment2Binding.inflate(layoutInflater)
    }

    // (초기 뷰 설정)
    private fun onCreateViewInitView() {
        bindingMbr.fragmentClickBtn.setOnClickListener {
            parentActivityMbr.fragmentClickedPositionMbr = 2
            if (parentActivityMbr.fragmentClickedPositionMbr == null){
                bindingMbr.clickedByValueTxt.text = "클릭 없음"
            }else{
                val textMsg = "${parentActivityMbr.fragmentClickedPositionMbr}번 플래그먼트"
                bindingMbr.clickedByValueTxt.text = textMsg
            }
        }

    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}