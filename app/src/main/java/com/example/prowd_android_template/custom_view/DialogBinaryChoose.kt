package com.example.prowd_android_template.custom_view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.TouchDelegate
import android.view.View
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
import com.example.prowd_android_template.databinding.DialogBinaryChooseBinding

class DialogBinaryChoose constructor(
    context: Context,
    var dialogInfoMbr: DialogInfoVO
) : Dialog(context) {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: DialogBinaryChooseBinding


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // (뷰 바인딩)
        bindingMbr = DialogBinaryChooseBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 뷰 설정)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 취소 불가 설정
        setCancelable(dialogInfoMbr.isCancelable)
        setOnCancelListener {
            dialogInfoMbr.onCanceled.run()
        }

        if (null != dialogInfoMbr.posBtnTxt) {
            bindingMbr.positiveBtn.text = dialogInfoMbr.posBtnTxt
        }

        if (null != dialogInfoMbr.negBtnTxt) {
            bindingMbr.negativeBtn.text = dialogInfoMbr.negBtnTxt
        }

        bindingMbr.title.text = dialogInfoMbr.title
        bindingMbr.contentTxt.text = dialogInfoMbr.content

        // (버튼 클릭 크기 조정)
        (bindingMbr.negativeBtn.parent as View).post {
            val rect = Rect()
            bindingMbr.negativeBtn.getHitRect(rect)
            rect.top -= 100 // increase top hit area
            rect.left -= 100 // increase left hit area
            rect.bottom += 100 // increase bottom hit area
            rect.right += 100 // increase right hit area
            (bindingMbr.negativeBtn.parent as View).touchDelegate =
                TouchDelegate(rect, bindingMbr.negativeBtn)
        }

        (bindingMbr.positiveBtn.parent as View).post {
            val rect = Rect()
            bindingMbr.positiveBtn.getHitRect(rect)
            rect.top -= 100 // increase top hit area
            rect.left -= 100 // increase left hit area
            rect.bottom += 100 // increase bottom hit area
            rect.right += 100 // increase right hit area
            (bindingMbr.positiveBtn.parent as View).touchDelegate =
                TouchDelegate(rect, bindingMbr.positiveBtn)
        }

        // (리스너 설정)
        bindingMbr.positiveBtn.setOnClickListener {
            dialogInfoMbr.onPosBtnClicked.run()
            this.dismiss()
        }

        bindingMbr.negativeBtn.setOnClickListener {
            dialogInfoMbr.onNegBtnClicked.run()
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
        var posBtnTxt: String?,
        var negBtnTxt: String?,
        var onPosBtnClicked: Runnable,
        var onNegBtnClicked: Runnable,
        var onCanceled: Runnable
    ) : InterfaceDialogInfoVO
}