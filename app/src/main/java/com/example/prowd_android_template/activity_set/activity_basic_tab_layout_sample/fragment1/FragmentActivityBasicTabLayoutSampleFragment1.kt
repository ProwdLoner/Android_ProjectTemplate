package com.example.prowd_android_template.activity_set.activity_basic_tab_layout_sample.fragment1

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.prowd_android_template.activity_set.activity_basic_tab_layout_sample.ActivityBasicTabLayoutSample
import com.example.prowd_android_template.databinding.FragmentActivityBasicTabLayoutSampleFragment1Binding

class FragmentActivityBasicTabLayoutSampleFragment1 : Fragment() {
    // <멤버 변수 공간>
    // (부모 객체) : 뷰 모델 구조 구현 및 부모 및 플래그먼트 간의 통신용
    lateinit var parentActivityMbr: ActivityBasicTabLayoutSample

    // (뷰 바인더 객체) : 뷰 조작에 관련된 바인더는 밖에서 조작 금지
    private lateinit var bindingMbr: FragmentActivityBasicTabLayoutSampleFragment1Binding

    // (Ui 스레드 핸들러 객체) handler.post{}
    val uiThreadHandlerMbr: Handler = Handler(Looper.getMainLooper())

    // (SharedPreference 객체)


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

    private var doItAlreadyMbr = false
    private var currentUserUidMbr: String? = null // 유저 식별가능 정보 - null 이라면 비회원
    override fun onResume() {
        super.onResume()

        if (!doItAlreadyMbr) {
            doItAlreadyMbr = true

            // (초기 데이터 수집)
            currentUserUidMbr = parentActivityMbr.currentLoginSessionInfoSpwMbr.userUid
            getScreenDataAndShow()

            // (알고리즘)
        } else {
            // (onResume - (onCreate + permissionGrant)) : 권한 클리어

            // (유저별 데이터 갱신)
            // : 유저 정보가 갱신된 상태에서 다시 현 액티비티로 복귀하면 자동으로 데이터를 다시 갱신합니다.
            val userUid = parentActivityMbr.currentLoginSessionInfoSpwMbr.userUid
            if (userUid != currentUserUidMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                currentUserUidMbr = userUid

                // (데이터 수집)
                getScreenDataAndShow()
            }
        }

        // (onResume)
        if (parentActivityMbr.fragmentClickedPositionMbr == null){
            bindingMbr.clickedByValueTxt.text = "클릭 없음"
        }else{
            val textMsg = "${parentActivityMbr.fragmentClickedPositionMbr}번 플래그먼트"
            bindingMbr.clickedByValueTxt.text = textMsg
        }
    }

    // (AndroidManifest.xml 에서 configChanges 에 설정된 요소에 변경 사항이 존재할 때 실행되는 콜백)
    // : 해당 이벤트 발생시 처리할 로직을 작성
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // 화면회전 landscape

        } else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { // 화면회전 portrait

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    private fun onCreateViewInitObject() {
        // (부모 객체 저장)
        parentActivityMbr = requireActivity() as ActivityBasicTabLayoutSample

        // (뷰 바인딩)
        bindingMbr =
            FragmentActivityBasicTabLayoutSampleFragment1Binding.inflate(layoutInflater)
    }

    // (초기 뷰 설정)
    private fun onCreateViewInitView() {
        bindingMbr.fragmentClickBtn.setOnClickListener {
            parentActivityMbr.fragmentClickedPositionMbr = 1
            if (parentActivityMbr.fragmentClickedPositionMbr == null){
                bindingMbr.clickedByValueTxt.text = "클릭 없음"
            }else{
                val textMsg = "${parentActivityMbr.fragmentClickedPositionMbr}번 플래그먼트"
                bindingMbr.clickedByValueTxt.text = textMsg
            }
        }

    }

    // (화면 구성용 데이터를 가져오기)
    // : 네트워크 등 레포지토리에서 데이터를 가져오고 이를 뷰에 반영
    private fun getScreenDataAndShow() {

    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}