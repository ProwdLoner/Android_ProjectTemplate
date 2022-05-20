package com.example.prowd_android_template.activity_set.activity_basic_vertical_recycler_view_sample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.prowd_android_template.R
import com.example.prowd_android_template.abstract_class.ProwdRecyclerViewAdapter
import com.example.prowd_android_template.databinding.*

class ActivityBasicVerticalRecyclerViewSampleAdapterSet(
    val recyclerViewAdapter: RecyclerViewAdapter
) {
    // 어뎁터 #1
    class RecyclerViewAdapter(
        private val parentViewMbr: ActivityBasicVerticalRecyclerViewSample,
        targetView: RecyclerView,
        isVertical: Boolean,
        val adapterVmData: AdapterVmData,
        onScrollReachTheEnd: (() -> Unit)?
    ) : ProwdRecyclerViewAdapter(
        parentViewMbr,
        targetView,
        isVertical,
        adapterVmData,
        onScrollReachTheEnd
    ) {
        // <멤버 변수 공간>


        // ---------------------------------------------------------------------------------------------
        // <생성자 공간>


        // ---------------------------------------------------------------------------------------------
        // <메소드 오버라이딩 공간>
        // 아이템 뷰 타입 결정
        override fun getItemViewType(position: Int): Int {
            return when (getCurrentDataListDeepCopyReplica()[position]) {
                is AdapterHeaderAbstractVO -> {
                    Header::class.hashCode()
                }

                is AdapterFooterAbstractVO -> {
                    Footer::class.hashCode()
                }

                // 여기서부터 아래로는 아이템 유형에 따른 중복 클래스를 사용하여 설정
                // 아이템 로더 클래스 역시 아이템에 해당하여, 종류를 바꾸어 뷰를 변경
                is ItemLoader.ItemVO -> {
                    ItemLoader::class.hashCode()
                }

                is Item1.ItemVO -> {
                    Item1::class.hashCode()
                }

                else -> {
                    Item1::class.hashCode()
                }
            }
        }

        // 아이템 뷰타입에 따른 xml 화면 반환
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                // 헤더 / 푸터를 사용하지 않을 것이라면 item_empty 를 사용
                Header::class.hashCode() -> {
                    Header.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.item_empty,
                                parent,
                                false
                            )
                    )
                }

                Footer::class.hashCode() -> {
                    Footer.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.item_empty,
                                parent,
                                false
                            )
                    )
                }

                // 아래로는 사용할 아이템 타입에 따른 뷰를 설정
                ItemLoader::class.hashCode() -> {
                    ItemLoader.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.item_activity_basic_vertical_recycler_view_sample_adapter_recycler_view_item_loader,
                                parent,
                                false
                            )
                    )
                }

                Item1::class.hashCode() -> {
                    Item1.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.item_activity_basic_vertical_recycler_view_sample_adapter_recycler_view_item1,
                                parent,
                                false
                            )
                    )
                }

                // 아이템이 늘어나면 추가

                else -> {
                    Item1.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.item_activity_basic_vertical_recycler_view_sample_adapter_recycler_view_item1,
                                parent,
                                false
                            )
                    )
                }
            }
        }

        // 아이템 뷰 생성 시점 로직
        // 주의 : 반환되는 position 이 currentDataList 인덱스와 같지 않을 수 있음.
        //     최초 실행시에는 같지만 아이템이 지워질 경우 position 을 0 부터 재정렬하는게 아님.
        //     고로 데이터 조작시 주의할것.
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is Header.ViewHolder -> { // 헤더 아이템 바인딩
//                    val binding = holder.binding
//                    val entity = getCurrentDataListDeepCopyReplica()[position] as Header.ItemVO
                }

                is Footer.ViewHolder -> { // 푸터 아이템 바인딩
//                    val binding = holder.binding
//                    val entity = getCurrentDataListDeepCopyReplica()[position] as Footer.ItemVO
                }

                is ItemLoader.ViewHolder -> { // 아이템 로더 아이템 바인딩
//                    val binding = holder.binding
//                    val entity = getCurrentDataListDeepCopyReplica()[position] as ItemLoader.ItemVO
                }

                is Item1.ViewHolder -> { // 아이템1 아이템 바인딩
                    val binding = holder.binding
                    val copyEntity = getCurrentDataListDeepCopyReplica()[position] as Item1.ItemVO

                    binding.title.text = copyEntity.title

                    binding.deleteBtn.setOnClickListener {
                        parentViewMbr.viewModelMbr.executorServiceMbr?.execute {

                            val itemListCopy = getCurrentItemListDeepCopyReplica()

                            // position 이 달라졌을 수가 있기에 itemUid 를 사용해 조작 위치를 검색
                            val thisItemListIdx =
                                itemListCopy.indexOfFirst { it.itemUid == copyEntity.itemUid }

                            if (-1 == thisItemListIdx){
                                return@execute
                            }

                            itemListCopy.removeAt(thisItemListIdx)

                            parentViewMbr.runOnUiThread {
                                parentViewMbr.viewModelMbr.recyclerViewAdapterVmDataMbr.itemListLiveData.value =
                                    itemListCopy

                            }
                        }
                    }

                    binding.root.setOnClickListener {

                        parentViewMbr.viewModelMbr.executorServiceMbr?.execute {

                            val itemListCopy = getCurrentItemListDeepCopyReplica()

                            copyEntity.title = "(Item Clicked!)"

                            // position 이 달라졌을 수가 있기에 itemUid 를 사용해 조작 위치를 검색
                            val thisItemListIdx =
                                itemListCopy.indexOfFirst { it.itemUid == copyEntity.itemUid }

                            if (-1 == thisItemListIdx){
                                return@execute
                            }

                            itemListCopy[thisItemListIdx] = copyEntity

                            parentViewMbr.runOnUiThread {
                                parentViewMbr.viewModelMbr.recyclerViewAdapterVmDataMbr.itemListLiveData.value =
                                    itemListCopy

                            }
                        }
                    }
                }

                // 아이템이 늘어나면 추가
            }
        }

        // 아이템 내용 동일성 비교(아이템 내용/화면 변경시 사용될 기준)
        override fun isContentSame(
            oldItem: AdapterDataAbstractVO,
            newItem: AdapterDataAbstractVO
        ): Boolean {
            return when (oldItem) {
                is Header.ItemVO -> {
                    if (newItem is Header.ItemVO) { // 아이템 서로 타입이 같으면,
                        // 내용 비교
                        oldItem == newItem
                    } else { // 아이템 서로 타입이 다르면,
                        // 무조건 다른 아이템
                        false
                    }
                }

                is Footer.ItemVO -> {
                    if (newItem is Footer.ItemVO) { // 아이템 서로 타입이 같으면,
                        // 내용 비교
                        oldItem == newItem
                    } else { // 아이템 서로 타입이 다르면,
                        // 무조건 다른 아이템
                        false
                    }
                }

                is ItemLoader.ItemVO -> {
                    if (newItem is ItemLoader.ItemVO) { // 아이템 서로 타입이 같으면,
                        // 내용 비교
                        oldItem == newItem
                    } else { // 아이템 서로 타입이 다르면,
                        // 무조건 다른 아이템
                        false
                    }
                }

                is Item1.ItemVO -> {
                    if (newItem is Item1.ItemVO) { // 아이템 서로 타입이 같으면,
                        // 내용 비교
                        oldItem == newItem
                    } else { // 아이템 서로 타입이 다르면,
                        // 무조건 다른 아이템
                        false
                    }
                }

                // 아이템이 늘어나면 추가

                else -> {
                    oldItem == newItem
                }
            }
        }

        // 아이템 복제 로직 (서로 다른 타입에 대응하기 위해 구현이 필요)
        override fun getDeepCopyReplica(newItem: AdapterDataAbstractVO): AdapterDataAbstractVO {
            return when (newItem) {
                is Header.ItemVO -> {
                    newItem.copy()
                }

                is Footer.ItemVO -> {
                    newItem.copy()
                }

                is ItemLoader.ItemVO -> {
                    newItem.copy()
                }

                is Item1.ItemVO -> {
                    newItem.copy()
                }

                // 아이템이 늘어나면 추가

                else -> {
                    newItem
                }
            }
        }


        // ---------------------------------------------------------------------------------------------
        // <공개 메소드 공간>


        // ---------------------------------------------------------------------------------------------
        // <비공개 메소드 공간>


        // ---------------------------------------------------------------------------------------------
        // <내부 클래스 공간>
        // (Vm 저장 클래스)
        class AdapterVmData() : ProwdRecyclerViewAdapter.AdapterVmData() {
            // 뷰모델에 저장해서 사용해야 하는 데이터들은 여기에 선언
        }

        // (아이템 클래스)
        // 헤더 / 푸터를 사용하지 않을 것이라면 item_empty 를 사용 및 ItemVO 데이터를 임시 데이터로 채우기
        class Header {
            data class ViewHolder(
                val view: View,
                val binding: ItemEmptyBinding =
                    ItemEmptyBinding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                val emptyData: Int?
            ) : AdapterHeaderAbstractVO()
        }

        class Footer {
            data class ViewHolder(
                val view: View,
                val binding: ItemEmptyBinding =
                    ItemEmptyBinding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                val emptyData: Int?
            ) : AdapterFooterAbstractVO()
        }

        class ItemLoader {
            data class ViewHolder(
                val view: View,
                val binding: ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItemLoaderBinding =
                    ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItemLoaderBinding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                override val itemUid: Long
            ) : AdapterItemAbstractVO(itemUid)
        }

        class Item1 {
            data class ViewHolder(
                val view: View,
                val binding: ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItem1Binding =
                    ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItem1Binding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                override val itemUid: Long,
                val serverItemUid: Long,
                var title: String
            ) : AdapterItemAbstractVO(itemUid)
        }

        // 아이템이 늘어나면 추가

    }
}