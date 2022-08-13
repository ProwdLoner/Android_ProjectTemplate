package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment1

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.ActivityBasicBottomSheetNavigationSample
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.databinding.FragmentActivityBasicBottomSheetNavigationSampleFragment1Binding
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService

class FragmentActivityBasicBottomSheetNavigationSampleFragment1 : Fragment() {
    // <멤버 변수 공간>
    // (부모 객체) : 뷰 모델 구조 구현 및 부모 및 플래그먼트 간의 통신용
    lateinit var parentActivityMbr: ActivityBasicBottomSheetNavigationSample

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
        setLiveData()

        return bindingMbr.root
    }

    override fun onResume() {
        super.onResume()

        // (데이터 갱신 시점 적용)
//        if (!parentActivityMbr.viewModelMbr.isChangingConfigurationsMbr && // 화면 회전이 아니면서,
//            isVisible // 현재 보이는 상황일 때
//        ) {
//            val sessionToken =
//                parentActivityMbr.viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken
//
//            if (parentActivityMbr.viewModelMbr.fragment1DataMbr.isDataFirstLoadingMbr || // 데이터 최초 로딩 시점일 때 혹은,
//                sessionToken != parentActivityMbr.viewModelMbr.fragment1DataMbr.currentUserSessionTokenMbr // 액티비티 유저와 세션 유저가 다를 때
//            ) {
//                // 진입 플래그 변경
//                parentActivityMbr.viewModelMbr.fragment1DataMbr.isDataFirstLoadingMbr = false
//                parentActivityMbr.viewModelMbr.fragment1DataMbr.currentUserSessionTokenMbr =
//                    sessionToken
//
//                //  데이터 로딩
//            }
//        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.e("dd", "efs")

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

    // 초기 뷰 설정
    private fun onCreateViewInitView() {
        bindingMbr.fragmentClickBtn.setOnClickListener {
            parentActivityMbr.viewModelMbr.fragmentClickedPositionLiveDataMbr.value = 1
        }

    }

    // 라이브 데이터 설정
    private fun setLiveData() {
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
        // <멤버 변수 공간>
        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw =
            CurrentLoginSessionInfoSpw(application)

        // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
        var currentUserSessionTokenMbr: String? = null

        // 데이터 수집 등, 첫번째에만 발동
        var isDataFirstLoadingMbr: Boolean = true


        // ---------------------------------------------------------------------------------------------
        // <뷰모델 라이브데이터 공간>


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>
    }
}