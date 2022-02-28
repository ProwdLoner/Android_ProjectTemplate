package com.example.prowd_android_template.activity_set.activity_basic_recycler_view_sample

import android.app.Activity
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.prowd_android_template.R
import com.example.prowd_android_template.abstract_class.AbstractRecyclerViewAdapter
import com.example.prowd_android_template.databinding.*
import java.text.SimpleDateFormat
import java.util.*


class ActivityBasicRecyclerViewSampleAdapterSet(
    private val parentViewMbr: Activity,
    private val parentViewModelMbr: ActivityBasicRecyclerViewSampleViewModel,
    val screenVerticalRecyclerViewAdapter: ScreenVerticalRecyclerViewAdapter = ScreenVerticalRecyclerViewAdapter(
        parentViewMbr,
        parentViewModelMbr
    )
) {
    // 어뎁터 #1
    class ScreenVerticalRecyclerViewAdapter(
        private val parentViewMbr: Activity,
        private val parentViewModel: ActivityBasicRecyclerViewSampleViewModel
    ) : AbstractRecyclerViewAdapter() {
        // <멤버 변수 공간>
        var blinkIdx: Int? = null


        // ---------------------------------------------------------------------------------------------
        // <메소드 오버라이딩 공간>
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                Header::class.hashCode() -> {
                    Header.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_header,
                                parent,
                                false
                            )
                    )
                }

                Footer::class.hashCode() -> {
                    Footer.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_footer,
                                parent,
                                false
                            )
                    )
                }

                ItemLoader::class.hashCode() -> {
                    ItemLoader.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_item_loader,
                                parent,
                                false
                            )
                    )
                }

                Item1::class.hashCode() -> {
                    Item1.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_item1,
                                parent,
                                false
                            )
                    )
                }

                // todo 아이템이 늘어나면 추가

                else -> {
                    Header.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(
                                R.layout.activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_header,
                                parent,
                                false
                            )
                    )
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is Header.ViewHolder -> { // 헤더 아이템 바인딩
                    val binding = holder.binding
                    val entity = currentItemListMbr[position] as Header.ItemVO

                    binding.content.text = entity.content
                }

                is Footer.ViewHolder -> { // 푸터 아이템 바인딩
                    val binding = holder.binding
                    val entity = currentItemListMbr[position] as Footer.ItemVO

                    binding.content.text = entity.content
                }

                is ItemLoader.ViewHolder -> { // 아이템 로더 아이템 바인딩
                    val binding = holder.binding
                    val entity = currentItemListMbr[position] as ItemLoader.ItemVO

                }

                is Item1.ViewHolder -> { // 아이템1 아이템 바인딩
                    val binding = holder.binding
                    val entity = currentItemListMbr[position] as Item1.ItemVO

                    // 추가된 아이템을 2초간 반짝거리게 만드는 로직
                    if (blinkIdx != null && blinkIdx == position) {
                        blinkIdx = null

                        val startAnimation: Animation = AnimationUtils.loadAnimation(
                            parentViewMbr.applicationContext,
                            R.anim.anim_blink_at_item_added
                        )

                        binding.root.startAnimation(startAnimation)

                        parentViewModel.executorServiceMbr?.execute {
                            Thread.sleep(2000)

                            parentViewMbr.runOnUiThread {
                                binding.root.clearAnimation()
                            }
                        }
                    }

                    // 삭제 버튼 크기 조정
                    (binding.deleteBtn.parent as View).post {
                        val rect = Rect()
                        binding.deleteBtn.getHitRect(rect)
                        rect.top -= 100 // increase top hit area
                        rect.left -= 50 // increase left hit area
                        rect.bottom += 100 // increase bottom hit area
                        rect.right += 100 // increase right hit area
                        (binding.deleteBtn.parent as View).touchDelegate =
                            TouchDelegate(rect, binding.deleteBtn)
                    }

                    binding.title.text = entity.title
                    binding.content.text = entity.content


                    // date 메시지 변환
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    val cal = Calendar.getInstance()
                    cal.time = format.parse(entity.writeDate)!!
                    cal.add(Calendar.HOUR, 9)

                    val dateObj = cal.time

                    val today = Calendar.getInstance()

                    val timeDiffMillSec = (today.time.time - dateObj.time) // 지금 시간과의 차이(밀리 초 단위)

                    val dateString = when {
                        60 > timeDiffMillSec / 1000 -> { // 1분(60초) 미만일 때
                            "방금 전"
                        }
                        60 > timeDiffMillSec / (1000 * 60) -> { // 1시간(60분) 미만일 때
                            "${timeDiffMillSec / (1000 * 60)} 분 전"
                        }
                        24 > timeDiffMillSec / (1000 * 60 * 60) -> { // 1일(24시간) 미만일 때
                            "${timeDiffMillSec / (1000 * 60 * 60)} 시간 전"
                        }
                        30 > timeDiffMillSec / (1000 * 60 * 60 * 24) -> { // 1달(30일) 미만일 때
                            "${timeDiffMillSec / (1000 * 60 * 60 * 24)} 일 전"
                        }
                        12 > timeDiffMillSec / (1000L * 60L * 60L * 24L * 30L) -> { // 1년(12달) 미만일 때
                            val yearFormat =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            yearFormat.format(dateObj)
                        }
                        else -> { // 1년 이상 일 때
                            val yearFormat =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            yearFormat.format(dateObj)
                        }
                    }

                    binding.writeDate.text = dateString

                    // 아이템 제거 버튼 클릭
                    binding.deleteBtn.setOnClickListener {
                        if (parentViewModel.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!) {
                            return@setOnClickListener
                        }

                        parentViewModel.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                            true
                        // todo 아이템을 리포지토리에서 제거 후 완료 여부에 따라 화면에서 제거

                        // 해당 아이템 인덱스를 구하고 화면 반영
                        val dataListReplica = getCurrentItemDeepCopyReplica()

                        val itemIdx =
                            dataListReplica.indexOfFirst {
                                it.itemUid == entity.itemUid
                            }

                        dataListReplica.removeAt(
                            itemIdx
                        )

                        setNewItemList(dataListReplica)

                        parentViewModel.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                            false

                        // 중복 클릭 방지
                        binding.deleteBtn.setOnClickListener { }
                    }
                }

                // todo 아이템이 늘어나면 추가
            }
        }

        override fun isContentSame(
            oldItem: AdapterItemAbstractVO,
            newItem: AdapterItemAbstractVO
        ): Boolean {
            return when (oldItem) {
                is Header.ItemVO -> {
                    oldItem == newItem as Header.ItemVO
                }

                is Footer.ItemVO -> {
                    oldItem == newItem as Footer.ItemVO
                }

                is ItemLoader.ItemVO -> {
                    oldItem == newItem as ItemLoader.ItemVO
                }

                is Item1.ItemVO -> {
                    oldItem == newItem as Item1.ItemVO
                }

                // todo 아이템이 늘어나면 추가

                else -> {
                    oldItem == newItem
                }
            }
        }

        override fun getDeepCopyReplica(newItem: AdapterItemAbstractVO): AdapterItemAbstractVO {
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

                // todo 아이템이 늘어나면 추가

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
                val binding: ActivityBasicRecyclerViewSampleScreenVerticalRecyclerViewAdapterHeaderBinding =
                    ActivityBasicRecyclerViewSampleScreenVerticalRecyclerViewAdapterHeaderBinding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                override val itemUid: Int,
                val content: String
            ) : AdapterItemAbstractVO(itemUid)
        }

        class Footer {
            data class ViewHolder(
                val view: View,
                val binding: ActivityBasicRecyclerViewSampleScreenVerticalRecyclerViewAdapterFooterBinding =
                    ActivityBasicRecyclerViewSampleScreenVerticalRecyclerViewAdapterFooterBinding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                override val itemUid: Int,
                val content: String
            ) : AdapterItemAbstractVO(itemUid)
        }

        class ItemLoader {
            data class ViewHolder(
                val view: View,
                val binding: ActivityBasicRecyclerViewSampleScreenVerticalRecyclerViewAdapterItemLoaderBinding =
                    ActivityBasicRecyclerViewSampleScreenVerticalRecyclerViewAdapterItemLoaderBinding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                override val itemUid: Int
            ) : AdapterItemAbstractVO(itemUid)
        }

        class Item1 {
            data class ViewHolder(
                val view: View,
                val binding: ActivityBasicRecyclerViewSampleScreenVerticalRecyclerViewAdapterItem1Binding =
                    ActivityBasicRecyclerViewSampleScreenVerticalRecyclerViewAdapterItem1Binding.bind(
                        view
                    )
            ) : RecyclerView.ViewHolder(view)

            data class ItemVO(
                override val itemUid: Int,
                val uid: Int,
                val title: String,
                val content: String,
                val writeDate: String
            ) : AdapterItemAbstractVO(itemUid)
        }


        // todo 아이템이 늘어나면 추가

    }
}