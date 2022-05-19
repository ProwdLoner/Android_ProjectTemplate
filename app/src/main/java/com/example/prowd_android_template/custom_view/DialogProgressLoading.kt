package com.example.prowd_android_template.custom_view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.example.prowd_android_template.R
import com.example.prowd_android_template.databinding.DialogProgressLoadingBinding

class DialogProgressLoading constructor(
    context: Context,
    var dialogInfoMbr: DialogInfoVO
) : Dialog(context) {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: DialogProgressLoadingBinding


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // (뷰 바인딩)
        bindingMbr = DialogProgressLoadingBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 뷰 설정)
        // 취소 불가 설정
        setCancelable(dialogInfoMbr.isCancelable)
        setOnCancelListener {
            dialogInfoMbr.onCanceled.run()
        }

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // gif 아이콘 붙이기
        Glide.with(context).asGif()
            .load(R.drawable.img_dialog_progress_loading_loading_gif)
            .centerCrop()
            .into(bindingMbr.loadingGifImg)

        // 진행 텍스트 설정
        if (null != dialogInfoMbr.progressMsg) { // 설정 메시지 출력
            bindingMbr.progressMessageTxt.visibility = View.VISIBLE
            bindingMbr.progressMessageTxt.text = dialogInfoMbr.progressMsg
        } else { // 기본 메시지 출력
            bindingMbr.progressMessageTxt.visibility = View.GONE
        }
        bindingMbr.progressMessageTxt.isSelected = true // marque 적용을 위한 설정
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    data class DialogInfoVO(
        var isCancelable: Boolean,
        var progressMsg: String?,
        var onCanceled: Runnable
    )
}