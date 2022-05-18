package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter

data class ActivityBasicBottomSheetNavigationSampleAdapterSet(
    val screenViewPagerFragmentStateAdapter: ScreenViewPagerFragmentStateAdapter
) {
    // <내부 클래스 공간>
    // [플래그먼트 변경 어뎁터]
    class ScreenViewPagerFragmentStateAdapter(
        parentViewMbr: ActivityBasicBottomSheetNavigationSample
    ) : FragmentStateAdapter(parentViewMbr) {
        // <멤버 변수 공간>
        private val adapterMainDataMbr: ArrayList<Fragment> = ArrayList()


        // ---------------------------------------------------------------------------------------------
        // <메소드 오버라이딩 공간>
        // 전체 프레그먼트 아이템 개수
        override fun getItemCount(): Int {
            return adapterMainDataMbr.size
        }

        // 각 프레그먼트 생성시의 콜백
        override fun createFragment(position: Int): Fragment {
            return adapterMainDataMbr[position]
        }


        // ---------------------------------------------------------------------------------------------
        // <공개 메소드 공간>
        fun setItems(itemList: List<Fragment>) {
            // 기존 플래그먼트 제거
            adapterMainDataMbr.clear()
            adapterMainDataMbr.addAll(itemList)

            // 화면 반영
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = adapterMainDataMbr.size

                override fun getNewListSize() = itemList.size

                // 객체 id 를 가지고 판별
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return adapterMainDataMbr[oldItemPosition].id == itemList[newItemPosition].id
                }

                // 고유 값을 가지고 판별 (없으면 전체를 비교)
                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return adapterMainDataMbr[oldItemPosition] == itemList[newItemPosition]
                }

            }).dispatchUpdatesTo(this)
        }
    }
}