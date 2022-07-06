package com.example.prowd_android_template.custom_view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.TouchDelegate
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.constraintlayout.widget.ConstraintSet
import com.example.prowd_android_template.databinding.DialogRadioButtonChooseBinding

class DialogRadioButtonChoose constructor(
    context: Context,
    var dialogInfoMbr: DialogInfoVO
) : Dialog(context) {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: DialogRadioButtonChooseBinding

    var checkedItemIndexMbr: Int = 0

    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // (뷰 바인딩)
        bindingMbr = DialogRadioButtonChooseBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 뷰 설정)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 취소 불가 설정
        setCancelable(dialogInfoMbr.isCancelable)
        setOnCancelListener {
            dialogInfoMbr.onCanceled.run()
        }

        if (null != dialogInfoMbr.cancelBtnTxt) {
            bindingMbr.cancelBtn.text = dialogInfoMbr.cancelBtnTxt
        }

        bindingMbr.title.text = dialogInfoMbr.title
        if (dialogInfoMbr.contentMsg != null) {
            bindingMbr.contentTxt.visibility = View.VISIBLE
            bindingMbr.contentTxt.text = dialogInfoMbr.contentMsg
        } else {
            bindingMbr.contentTxt.visibility = View.GONE
        }

        // 라디오 버튼 추가
        val radioGroup = RadioGroup(context)
        radioGroup.orientation = RadioGroup.VERTICAL

        val radioButtonList = ArrayList<RadioButton>()
        for (report in dialogInfoMbr.radioButtonContentList.withIndex()) {
            val btn = RadioButton(context)
            btn.text = report.value
            btn.id = View.generateViewId()
            radioGroup.addView(btn)
            radioButtonList.add(btn)
        }

        if (dialogInfoMbr.checkedItemIdx != null) {
            if (dialogInfoMbr.radioButtonContentList.lastIndex >= dialogInfoMbr.checkedItemIdx!!) {
                radioGroup.check(dialogInfoMbr.checkedItemIdx!!)
            } else {
                radioGroup.check(dialogInfoMbr.radioButtonContentList.lastIndex)
            }
        }

        radioGroup.id = View.generateViewId()

        bindingMbr.contentContainerInnerScroll.addView(radioGroup, 0)

        val set = ConstraintSet()
        set.clone(bindingMbr.contentContainerInnerScroll)
        set.connect(
            radioGroup.id,
            ConstraintSet.START,
            bindingMbr.contentContainerInnerScroll.id,
            ConstraintSet.START,
            0
        )
        set.connect(
            radioGroup.id,
            ConstraintSet.TOP,
            bindingMbr.contentTxt.id,
            ConstraintSet.BOTTOM,
            0
        )
        set.applyTo(bindingMbr.contentContainerInnerScroll)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val checkedIdx = radioButtonList.indexOfFirst {
                it.id == checkedId
            }
            checkedItemIndexMbr = checkedIdx
            dialogInfoMbr.onRadioItemClicked(checkedIdx)
        }

        // (버튼 클릭 크기 조정)
        (bindingMbr.cancelBtn.parent as View).post {
            val rect = Rect()
            bindingMbr.cancelBtn.getHitRect(rect)
            rect.top -= 100 // increase top hit area
            rect.left -= 100 // increase left hit area
            rect.bottom += 100 // increase bottom hit area
            rect.right += 100 // increase right hit area
            (bindingMbr.cancelBtn.parent as View).touchDelegate =
                TouchDelegate(rect, bindingMbr.cancelBtn)
        }

        // (리스너 설정)
        bindingMbr.cancelBtn.setOnClickListener {
            dialogInfoMbr.onCancelBtnClicked.run()
            this.dismiss()
        }

        bindingMbr.selectBtn.setOnClickListener {
            dialogInfoMbr.onSelectBtnClicked(checkedItemIndexMbr)
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
        var contentMsg: String?,
        val radioButtonContentList: ArrayList<String>,
        val checkedItemIdx: Int?,
        var cancelBtnTxt: String?,
        var onRadioItemClicked: (Int) -> Unit,
        var onSelectBtnClicked: (Int) -> Unit,
        var onCancelBtnClicked: Runnable,
        var onCanceled: Runnable
    )
}