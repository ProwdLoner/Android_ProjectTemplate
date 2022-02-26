package com.example.prowd_android_template.custom_view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.TouchDelegate
import android.view.View
import com.example.prowd_android_template.databinding.DialogConfirmBinding

class DialogConfirm constructor(
    context: Context,
    var dialogInfoMbr : DialogInfoVO
) : Dialog(context) {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: DialogConfirmBinding


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // (뷰 바인딩)
        bindingMbr = DialogConfirmBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 뷰 설정)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 취소 불가 설정
        setCancelable(dialogInfoMbr.isCancelable)
        setOnCancelListener {
            dialogInfoMbr.onCanceled?.run()
        }

        // 확인 버튼 텍스트 설정
        if (null != dialogInfoMbr.checkBtnTxt) {
            bindingMbr.confirmButton.text = dialogInfoMbr.checkBtnTxt
        }

        // 타이틀 텍스트 설정
        bindingMbr.title.text = dialogInfoMbr.title
        bindingMbr.title.isSelected = true // marque 적용을 위한 설정

        // 본문 텍스트 설정
        bindingMbr.contentTxt.text = dialogInfoMbr.content

        // (버튼 클릭 크기 조정)
        (bindingMbr.confirmButton.parent as View).post {
            val rect = Rect()
            bindingMbr.confirmButton.getHitRect(rect)
            rect.top -= 100 // increase top hit area
            rect.left -= 100 // increase left hit area
            rect.bottom += 100 // increase bottom hit area
            rect.right += 100 // increase right hit area
            (bindingMbr.confirmButton.parent as View).touchDelegate =
                TouchDelegate(rect, bindingMbr.confirmButton)
        }

        // (리스너 설정)
        // 확인 버튼 클릭
        bindingMbr.confirmButton.setOnClickListener {
            dialogInfoMbr.onCheckBtnClicked?.run()
            this.dismiss()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    data class DialogInfoVO(
        var isCancelable: Boolean,
        var title: String,
        var content: String,
        var checkBtnTxt: String?,
        var onCheckBtnClicked: Runnable?,
        var onCanceled: Runnable?
    )
}