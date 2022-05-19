package com.example.prowd_android_template.activity_set.activity_easy_lut_sample

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.example.prowd_android_template.R
import com.example.prowd_android_template.abstract_class.ProwdRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ItemActivityEasyLutSampleAdapterRecyclerViewItem1Binding
import com.example.prowd_android_template.databinding.ItemActivityEasyLutSampleAdapterRecyclerViewItemLoaderBinding
import com.example.prowd_android_template.databinding.ItemEmptyBinding
import com.example.prowd_android_template.util_object.CustomUtil
import com.example.prowd_android_template.util_object.easylut.EasyLUT
import com.example.prowd_android_template.util_object.easylut.lutimage.LutAlignment

class ActivityEasyLutSampleAdapterSet(
    val recyclerViewAdapter: RecyclerViewAdapter
) {
    // 어뎁터 #1
    class RecyclerViewAdapter(
        private val parentViewMbr: ActivityEasyLutSample,
        private val parentViewModel: ActivityEasyLutSampleViewModel,
        targetView: RecyclerView,
        isVertical: Boolean,
        adapterVmData : AdapterVmData,
        onScrollHitBottom: (() -> Unit)?
    ) : ProwdRecyclerViewAdapter(
        parentViewMbr,
        targetView,
        isVertical,
        adapterVmData,
        onScrollHitBottom
    ) {
        // <멤버 변수 공간>
        var selectedItemPosition: Int = -1
            set(value) {
                val oldPosition = selectedItemPosition
                val newPosition = value
                field = value
                notifyItemChanged(oldPosition)
                notifyItemChanged(newPosition)
            }


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
                                R.layout.item_activity_easy_lut_sample_adapter_recycler_view_item_loader,
                                parent,
                                false
                            )
                    )
                }

                Item1::class.hashCode() -> {
                    Item1.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.item_activity_easy_lut_sample_adapter_recycler_view_item1,
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

                    if (position == selectedItemPosition) {
                        // 선택된 경우
                        binding.root.setBackgroundColor(Color.parseColor("#FF3D7DDC"))
                        binding.title.setTextColor(Color.parseColor("#FFFFFFFF"))

                        // 필터 적용
                        // 다이얼로그 표시
                        parentViewModel.progressLoadingDialogInfoLiveDataMbr.value =
                            DialogProgressLoading.DialogInfoVO(
                                false,
                                "필터 적용중입니다.",
                                onCanceled = {}
                            )

                        parentViewModel.executorServiceMbr?.execute {
                            // LUT 필터 생성
                            val filter = CustomUtil.getBitmapFromAssets(
                                parentViewMbr,
                                "lut_filters_wide/${entity.title}"
                            )
                            val filterKind = LutAlignment.Mode.SQUARE

                            val lutFilter = EasyLUT
                                .fromBitmap()
                                .withBitmap(filter!!)
                                .withAlignmentMode(filterKind)
                                ?.createFilter()

                            // 원본 이미지1
                            val originBitmap1 = ((AppCompatResources.getDrawable(
                                parentViewMbr,
                                R.drawable.img_activity_easy_lut_sample_image1
                            )) as BitmapDrawable).bitmap

                            val filterBitmap1 = lutFilter?.apply(originBitmap1)

                            // 원본 이미지2
                            val originBitmap2 = ((AppCompatResources.getDrawable(
                                parentViewMbr,
                                R.drawable.img_activity_easy_lut_sample_image2
                            )) as BitmapDrawable).bitmap

                            val filterBitmap2 = lutFilter?.apply(originBitmap2)

                            // 원본 이미지3
                            val originBitmap3 = ((AppCompatResources.getDrawable(
                                parentViewMbr,
                                R.drawable.img_activity_easy_lut_sample_image3
                            )) as BitmapDrawable).bitmap

                            val filterBitmap3 = lutFilter?.apply(originBitmap3)

                            parentViewMbr.runOnUiThread {
                                parentViewModel.filteredImage1LiveDataMbr.value = filterBitmap1
                                parentViewModel.filteredImage2LiveDataMbr.value = filterBitmap2
                                parentViewModel.filteredImage3LiveDataMbr.value = filterBitmap3

                                parentViewModel.progressLoadingDialogInfoLiveDataMbr.value = null
                            }
                        }
                    } else {
                        binding.root.setBackgroundColor(Color.parseColor("#FFEEEEEE"))
                        binding.title.setTextColor(Color.parseColor("#FF000000"))
                    }

                    binding.root.setOnClickListener {
                        // 선택 필터 파일명 저장
                        parentViewModel.thisSpw.selectedFilterName = entity.title

                        // 선택 아이템 변경 반영
                        selectedItemPosition = position
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
            ) : AdapterAbstractVO(itemUid)
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
            ) : AdapterAbstractVO(itemUid)
        }

        class ItemLoader {
            data class ViewHolder(
                val view: View,
                val binding: ItemActivityEasyLutSampleAdapterRecyclerViewItemLoaderBinding =
                    ItemActivityEasyLutSampleAdapterRecyclerViewItemLoaderBinding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                override val itemUid: Long
            ) : AdapterAbstractVO(itemUid)
        }

        class Item1 {
            data class ViewHolder(
                val view: View,
                val binding: ItemActivityEasyLutSampleAdapterRecyclerViewItem1Binding =
                    ItemActivityEasyLutSampleAdapterRecyclerViewItem1Binding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                override val itemUid: Long,
                val title: String
            ) : AdapterAbstractVO(itemUid)
        }

        // 아이템이 늘어나면 추가

    }
}