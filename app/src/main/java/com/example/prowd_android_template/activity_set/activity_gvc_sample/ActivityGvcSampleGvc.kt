package com.example.prowd_android_template.activity_set.activity_gvc_sample

import android.app.Application
import android.content.Context

// (글로벌 변수로 사용할 SharedPref 래퍼 클래스)
// 액티비티에 종속된 것으로, 해당 액티비티의 onResume 에서 해석되어 동작을 제어하는 용도
class ActivityGvcSampleGvc(application: Application) {
    // <멤버 변수 공간>
    // SharedPreference 파일명
    private val spNameMbr = "ActivityGvcSampleGvc"

    // SharedPreference 접근 객체
    private val spMbr = application.getSharedPreferences(
        spNameMbr,
        Context.MODE_PRIVATE
    )


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    fun getData(): ColumnVo {
        val message1 = spMbr.getString(
            colMessage1Mbr,
            null
        )

        val message2 = spMbr.getString(
            colMessage2Mbr,
            null
        )

        return ColumnVo(
            message1,
            message2
        )
    }

    fun setData(inputData: ColumnVo) {
        with(spMbr.edit()) {
            putString(
                colMessage1Mbr,
                inputData.message1
            )
            putString(
                colMessage2Mbr,
                inputData.message2
            )

            apply()
        }
    }

    fun setMessage1(message1: String?) {
        with(spMbr.edit()) {
            putString(
                colMessage1Mbr,
                message1
            )
            apply()
        }
    }

    fun setMessage2(message2: String?) {
        with(spMbr.edit()) {
            putString(
                colMessage2Mbr,
                message2
            )
            apply()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // 로그인 정보 객체
    data class ColumnVo(
        val message1: String?,
        val message2: String?
    )

    // SharedPreference 컬럼명
    private val colMessage1Mbr = "ActivityGvcSampleGvcMessage1"
    private val colMessage2Mbr = "ActivityGvcSampleGvcMessage2"

}
