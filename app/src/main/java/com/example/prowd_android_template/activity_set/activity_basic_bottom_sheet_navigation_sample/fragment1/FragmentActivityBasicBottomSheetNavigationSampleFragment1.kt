package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment1

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.ActivityBasicBottomSheetNavigationSample
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.databinding.FragmentActivityBasicBottomSheetNavigationSampleFragment1Binding
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService

// 플래그먼트 필수 권한은 이를 사용하는 액티비티에서 처리를 할 것 = 권한 충족이 되었다고 가정
class FragmentActivityBasicBottomSheetNavigationSampleFragment1 : Fragment() {
    // <멤버 변수 공간>
    // (부모 객체) : 뷰 모델 구조 구현 및 부모 및 플래그먼트 간의 통신용
    private lateinit var parentActivityMbr: ActivityBasicBottomSheetNavigationSample

    // (Ui 스레드 핸들러 객체) handler.post{}
    private val uiThreadHandlerMbr: Handler = Handler(Looper.getMainLooper())

    // (뷰 바인더 객체)
    lateinit var bindingMbr: FragmentActivityBasicBottomSheetNavigationSampleFragment1Binding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: FragmentViewModel


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    // : 플래그먼트 실행  = onAttach() → onCreate() → onCreateView() → onActivityCreated() → onStart() → onResume()
    //     플래그먼트 일시정지 후 재실행 = onPause() → ... -> onResume()
    //     플래그먼트 정지 후 재실행 = onPause() → onStop() -> ... -> onStart() → onResume()
    //     플래그먼트 종료 = onPause() → onStop() → onDestroyView() → onDestroy() → onDetach()
    //     플래그먼트 화면 회전 = onPause() → onSaveInstanceState() → onStop() → onDestroyView() → onDestroy() →
    //         onDetach() → onAttach() → onCreate() → onCreateView() → onActivityCreated() → onStart() → onResume()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // (초기 객체 생성)
        onCreateViewInitObject()

        // (초기 뷰 설정)
        onCreateViewInitView()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        onCreateViewSetLiveData()

        return bindingMbr.root
    }

    override fun onResume() {
        super.onResume()

        if (!viewModelMbr.isActivityRecreatedMbr) { // 화면 회전이 아닐때
            if (!viewModelMbr.doItAlreadyMbr) {
                viewModelMbr.doItAlreadyMbr = true

                // ---------------------------------------------------------------------------------
                // (실질적인 onCreate 로직) : 권한 클리어 + 처음 실행

            }

            // -------------------------------------------------------------------------------------
            // (실질적인 onResume 로직) : 권한 클리어
            // (뷰 데이터 로딩)
            // : 유저가 변경되면 해당 유저에 대한 데이터로 재구축
            val sessionToken = viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken
            if (sessionToken != viewModelMbr.currentUserSessionTokenMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                viewModelMbr.currentUserSessionTokenMbr = sessionToken

                // 데이터 수집
            }

        } else { // 화면 회전일 때

        }

        // onResume 의 가장 마지막엔 설정 변경(화면회전) 여부를 초기화
        viewModelMbr.isActivityRecreatedMbr = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // 설정 변경(화면회전)을 했는지 여부를 반영
        viewModelMbr.isActivityRecreatedMbr = true

        super.onSaveInstanceState(outState)
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    private fun onCreateViewInitObject() {
        // (부모 객체 저장)
        parentActivityMbr = requireActivity() as ActivityBasicBottomSheetNavigationSample

        // (뷰 바인딩)
        bindingMbr =
            FragmentActivityBasicBottomSheetNavigationSampleFragment1Binding.inflate(layoutInflater)

        // (플래그먼트 뷰모델)
        viewModelMbr = parentActivityMbr.viewModelMbr.fragment1DataMbr
    }

    // (초기 뷰 설정)
    private fun onCreateViewInitView() {
        bindingMbr.fragmentClickBtn.setOnClickListener {
            parentActivityMbr.viewModelMbr.fragmentClickedPositionLiveDataMbr.value = 1
        }

    }

    // (라이브 데이터 설정)
    private fun onCreateViewSetLiveData() {
        // 공유되는 부모 뷰모델 정보 반영 예시
        parentActivityMbr.viewModelMbr.fragmentClickedPositionLiveDataMbr.observe(parentActivityMbr) {
            if (null == it) {
                bindingMbr.clickedByValueTxt.text = "클릭 없음"
            } else {
                val textMsg = "${it}번 플래그먼트"
                bindingMbr.clickedByValueTxt.text = textMsg
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    class FragmentViewModel(
        application: Application,
        val repositorySetMbr: RepositorySet,
        val executorServiceMbr: ExecutorService?
    ) {
        // <멤버 상수 공간>
        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw =
            CurrentLoginSessionInfoSpw(application)


        // ---------------------------------------------------------------------------------------------
        // <멤버 변수 공간>
        // (최초 실행 플래그) : 액티비티가 실행되고, 권한 체크가 끝난 후의 최초 로직이 실행되었는지 여부
        var doItAlreadyMbr = false

        // (설정 변경 여부) : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
        var isActivityRecreatedMbr = false

        // (이 화면에 도달한 유저 계정 고유값) : 세션 토큰이 없다면 비회원 상태
        var currentUserSessionTokenMbr: String? = null


        // ---------------------------------------------------------------------------------------------
        // <뷰모델 라이브데이터 공간>


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>
    }
}