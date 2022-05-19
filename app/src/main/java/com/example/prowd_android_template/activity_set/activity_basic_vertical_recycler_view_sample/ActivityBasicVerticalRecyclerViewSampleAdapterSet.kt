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
        onScrollHitBottom: (() -> Unit)?
    ) : ProwdRecyclerViewAdapter(
        parentViewMbr,
        targetView,
        isVertical,
        adapterVmData,
        onScrollHitBottom
    ) {
        // <멤버 변수 공간>


        // ---------------------------------------------------------------------------------------------
        // <생성자 공간>


        // ---------------------------------------------------------------------------------------------
        // <메소드 오버라이딩 공간>
        // 아이템 뷰 타입 결정
        override fun getItemViewType(position: Int): Int {
            return when (currentItemListMbr[position]) {
                is Header.ItemVO -> {
                    Header::class.hashCode()
                }

                is Footer.ItemVO -> {
                    Footer::class.hashCode()
                }

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
                    Header.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.item_empty,
                                parent,
                                false
                            )
                    )
                }
            }
        }

        // 아이템 뷰 생성 시점 로직
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is Header.ViewHolder -> { // 헤더 아이템 바인딩
//                    val binding = holder.binding
//                    val entity = currentItemListMbr[position] as Header.ItemVO
                }

                is Footer.ViewHolder -> { // 푸터 아이템 바인딩
//                    val binding = holder.binding
//                    val entity = currentItemListMbr[position] as Footer.ItemVO
                }

                is ItemLoader.ViewHolder -> { // 아이템 로더 아이템 바인딩
//                    val binding = holder.binding
//                    val entity = currentItemListMbr[position] as ItemLoader.ItemVO
                }

                is Item1.ViewHolder -> { // 아이템1 아이템 바인딩
                    val binding = holder.binding
                    val entity = currentItemListMbr[position] as Item1.ItemVO

                    binding.title.text = entity.title

                    binding.deleteBtn.setOnClickListener {
                        parentViewMbr.viewModelMbr.executorServiceMbr?.execute {
                            parentViewMbr.viewModelMbr.recyclerViewAdapterVmDataMbr.semaphore.acquire()

                            val item = getCurrentItemListDeepCopyReplicaOnlyItem()
                            val change = item.indexOfFirst {
                                it.itemUid == entity.itemUid
                            }

                            item.removeAt(change)

                            parentViewMbr.runOnUiThread {
                                parentViewMbr.viewModelMbr.recyclerViewAdapterVmDataMbr.itemListLiveData.value =
                                    item

                                parentViewMbr.viewModelMbr.recyclerViewAdapterVmDataMbr.semaphore.release()
                            }
                        }
                    }

                    binding.root.setOnClickListener {

                        parentViewMbr.viewModelMbr.executorServiceMbr?.execute {
                            // todo
//                            parentViewMbr.viewModelMbr.recyclerViewAdapterVmDataMbr.semaphore.acquire()
//
//                            val item = getCurrentItemListDeepCopyReplicaOnlyItem()
//
//                            val cloneEntity = getDeepCopyReplica(entity) as Item1.ItemVO
//                            cloneEntity.title = "(Item Clicked!)"
//
//                            item[position - getCurrentItemListOnlyItemFirstIndex()] = cloneEntity
//
//                            parentViewMbr.runOnUiThread {
//                                parentViewMbr.viewModelMbr.recyclerViewAdapterVmDataMbr.itemListLiveData.value =
//                                    item
//
//                                parentViewMbr.viewModelMbr.recyclerViewAdapterVmDataMbr.semaphore.release()
//                            }
                        }
                    }
                }

                // 아이템이 늘어나면 추가
            }
        }

        // 아이템 내용 동일성 비교(아이템 내용/화면 변경시 사용될 기준)
        override fun isContentSame(
            oldItem: AdapterAbstractVO,
            newItem: AdapterAbstractVO
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
        override fun getDeepCopyReplica(newItem: AdapterAbstractVO): AdapterAbstractVO {
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
        class AdapterVmData(
            override val itemListLiveData: MutableLiveData<ArrayList<AdapterItemAbstractVO>>,
            override val headerLiveData: MutableLiveData<AdapterHeaderAbstractVO>?,
            override val footerLiveData: MutableLiveData<AdapterFooterAbstractVO>?
        ) : ProwdRecyclerViewAdapter.AdapterVmData(
            itemListLiveData,
            headerLiveData,
            footerLiveData
        ) {
            // 뷰모델에 저장해서 사용해야 하는 데이터들은 여기에 선언
        }

        // (아이템 클래스)
        class Header {
            data class ViewHolder(
                val view: View,
                val binding: ItemEmptyBinding =
                    ItemEmptyBinding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                override val itemUid: Long
            ) : AdapterHeaderAbstractVO(itemUid)
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
                override val itemUid: Long
            ) : AdapterFooterAbstractVO(itemUid)
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