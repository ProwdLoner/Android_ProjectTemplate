package com.example.prowd_android_template.custom_view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prowd_android_template.R
import com.example.prowd_android_template.abstract_class.AbstractProwdRecyclerViewAdapter
import com.example.prowd_android_template.databinding.BottomSheetDialogFragmentItemSelector1Binding
import com.example.prowd_android_template.databinding.ItemBottomSheetDialogFragmentItemSelector1AdapterRecyclerViewItem1Binding
import com.example.prowd_android_template.databinding.ItemEmptyBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetDialogFragmentItemSelector1 constructor(
    private val title: String?,
    private val itemList: List<Item>
) : BottomSheetDialogFragment(
) {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: BottomSheetDialogFragmentItemSelector1Binding

    // (어뎁터 객체)
    lateinit var adapterSetMbr: BottomSheetDialogFragmentItemSelector1AdapterSet


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 객체
        bindingMbr = BottomSheetDialogFragmentItemSelector1Binding.inflate(layoutInflater)

        if (title == null) {
            bindingMbr.title.visibility = View.GONE
            bindingMbr.titleLine.visibility = View.GONE
        } else {
            bindingMbr.title.visibility = View.VISIBLE
            bindingMbr.titleLine.visibility = View.VISIBLE
            bindingMbr.title.text = title
        }

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr =
            BottomSheetDialogFragmentItemSelector1AdapterSet(
                BottomSheetDialogFragmentItemSelector1AdapterSet.RecyclerViewAdapter(
                    this,
                    bindingMbr.recyclerView,
                    false,
                    true, // 세로 스크롤인지 가로 스크롤인지
                    1, // 이 개수를 늘리면 그리드 레이아웃으로 변화
                    onScrollReachTheEnd = {}
                ))

        val adapterItemList = ArrayList<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>()
        for (item in itemList) {
            adapterItemList.add(
                BottomSheetDialogFragmentItemSelector1AdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                    adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                    item.itemName,
                    item.itemClickListener
                )
            )
        }

        adapterSetMbr.recyclerViewAdapter.setItemList(adapterItemList)

        return bindingMbr.root
    }

    override fun getTheme(): Int = R.style.BottomSheetDialog


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <내부 클래스 공간>
    data class Item(
        val itemName: String,
        val itemClickListener: () -> Unit
    )

    // (액티비티 내 사용 어뎁터 모음)
    // : 액티비티 내 사용할 어뎁터가 있다면 본문에 클래스 추가 후 인자로 해당 클래스의 인스턴스를 받도록 하기
    class BottomSheetDialogFragmentItemSelector1AdapterSet(
        val recyclerViewAdapter: RecyclerViewAdapter
    ) {
        // 어뎁터 #1
        class RecyclerViewAdapter(
            private val parentViewMbr: BottomSheetDialogFragmentItemSelector1,
            targetView: RecyclerView,
            targetViewLayoutManagerStackFromEnd : Boolean,
            isVertical: Boolean,
            oneRowItemCount: Int,
            onScrollReachTheEnd: (() -> Unit)?
        ) : AbstractProwdRecyclerViewAdapter(
            parentViewMbr.requireContext(),
            targetView,
            targetViewLayoutManagerStackFromEnd,
            isVertical,
            oneRowItemCount,
            onScrollReachTheEnd
        ) {
            // <멤버 변수 공간>


            // ---------------------------------------------------------------------------------------------
            // <메소드 오버라이딩 공간>
            // 아이템 뷰 타입 결정
            override fun getItemViewType(position: Int): Int {
                return when (currentDataListCloneMbr[position]) {
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
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
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
                                    R.layout.item_empty,
                                    parent,
                                    false
                                )
                        )
                    }

                    Item1::class.hashCode() -> {
                        Item1.ViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(
                                    R.layout.item_bottom_sheet_dialog_fragment_item_selector1_adapter_recycler_view_item1,
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
                                    R.layout.item_bottom_sheet_dialog_fragment_item_selector1_adapter_recycler_view_item1,
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
//                    val copyEntity = currentDataListCloneMbr[position] as Header.ItemVO
                    }

                    is Footer.ViewHolder -> { // 푸터 아이템 바인딩
//                    val binding = holder.binding
//                    val copyEntity = currentDataListCloneMbr[position] as Footer.ItemVO
                    }

                    is ItemLoader.ViewHolder -> { // 아이템 로더 아이템 바인딩
//                    val binding = holder.binding
//                    val copyEntity = currentDataListCloneMbr[position] as ItemLoader.ItemVO
                    }

                    is Item1.ViewHolder -> { // 아이템1 아이템 바인딩
                        val binding = holder.binding
                        val copyEntity = currentDataListCloneMbr[position] as Item1.ItemVO

                        binding.itemName.text = copyEntity.title

                        binding.root.setOnClickListener {
                            copyEntity.itemClickListener()
                            parentViewMbr.dismiss()
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

                class ItemVO : AdapterHeaderAbstractVO() {
                    fun copy(): Footer.ItemVO {
                        return Footer.ItemVO()
                    }
                }
            }

            class Footer {
                data class ViewHolder(
                    val view: View,
                    val binding: ItemEmptyBinding =
                        ItemEmptyBinding.bind(
                            view
                        )
                ) : RecyclerView.ViewHolder(view)

                class ItemVO : AdapterFooterAbstractVO() {
                    fun copy(): ItemVO {
                        return ItemVO()
                    }
                }
            }

            class ItemLoader {
                data class ViewHolder(
                    val view: View,
                    val binding: ItemEmptyBinding =
                        ItemEmptyBinding.bind(
                            view
                        )
                ) : RecyclerView.ViewHolder(view)

                class ItemVO : AdapterFooterAbstractVO() {
                    fun copy(): ItemVO {
                        return ItemVO()
                    }
                }
            }

            class Item1 {
                data class ViewHolder(
                    val view: View,
                    val binding: ItemBottomSheetDialogFragmentItemSelector1AdapterRecyclerViewItem1Binding =
                        ItemBottomSheetDialogFragmentItemSelector1AdapterRecyclerViewItem1Binding.bind(
                            view
                        )
                ) : RecyclerView.ViewHolder(view)

                data class ItemVO(
                    override val itemUid: Long,
                    var title: String,
                    var itemClickListener: () -> Unit
                ) : AdapterItemAbstractVO(itemUid)
            }

            // 아이템이 늘어나면 추가

        }
    }
}