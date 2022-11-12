package com.example.prowd_android_template.activity_set.activity_pinch_image_list_viewer

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.example.prowd_android_template.R
import com.example.prowd_android_template.databinding.ActivityPinchImageListViewerBinding
import com.example.prowd_android_template.databinding.ItemActivityPinchImageViewListViewerAdapterItem1Binding

class ActivityPinchImageListViewer : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityPinchImageListViewerBinding

    // (어뎁터 객체)
    private lateinit var adapterMbr: ActivityPinchImageListViewerAdapterSet

    // (인텐트 엑스트라 데이터)
    private var extraPetImgListMbr: ArrayList<String>? = null
    private var currentImgIndexMbr: Int = -1

    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityPinchImageListViewerBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (인텐트 엑스트라 데이터 수집)
        extraPetImgListMbr = intent.getStringArrayListExtra("img_list")!!
        currentImgIndexMbr = intent.getIntExtra("current_img_index", -1)

        adapterMbr = ActivityPinchImageListViewerAdapterSet(
            ActivityPinchImageListViewerAdapterSet.PetImgListAdapter(
                this
            )
        )

        // 뷰 페이저 방향 설정
        // 뷰 페이저 방향 설정
        bindingMbr.petImgList.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // (펫 이미지 뷰 페이저 어뎁터 설정)
        bindingMbr.petImgList.adapter = adapterMbr.petImgListAdapter

        adapterMbr.petImgListAdapter.addItemListAtLast(extraPetImgListMbr!!)

        bindingMbr.petImgList.setCurrentItem(currentImgIndexMbr, false)


        bindingMbr.pageTxt.text = "${currentImgIndexMbr + 1}/${extraPetImgListMbr!!.size}"

        bindingMbr.petImgList.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bindingMbr.pageTxt.text = "${position + 1}/${extraPetImgListMbr!!.size}"

            }
        })
    }


    data class ActivityPinchImageListViewerAdapterSet(
        val petImgListAdapter: PetImgListAdapter
    ) {
        // <내부 클래스 공간>
        // [펫 이미지 어뎁터]
        class PetImgListAdapter(
            private val activityMbr: Activity
        ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            // <멤버 변수 공간>
            // (리사이클러 뷰 주요 리스트 데이터)
            private val adapterItemListDataMbr: ArrayList<Pair<ItemType, String>> =
                ArrayList()


            // ---------------------------------------------------------------------------------------------
            // <메소드 오버라이딩 공간>
            override fun getItemCount(): Int {
                return adapterItemListDataMbr.size
            }

            override fun getItemViewType(position: Int): Int {
                return adapterItemListDataMbr[position].first.itemCode
            }

            override fun onCreateViewHolder(
                viewGroup: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                return when (viewType) {
                    ItemType.ITEM1.itemCode -> {
                        Item1ViewHolder(
                            LayoutInflater.from(viewGroup.context)
                                .inflate(
                                    R.layout.item_activity_pinch_image_view_list_viewer_adapter_item1,
                                    viewGroup,
                                    false
                                )
                        )
                    }
                    else -> {
                        Item1ViewHolder(
                            LayoutInflater.from(viewGroup.context)
                                .inflate(
                                    R.layout.item_activity_pinch_image_view_list_viewer_adapter_item1,
                                    viewGroup,
                                    false
                                )
                        )
                    }
                }
            }

            override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
                if (viewHolder is Item1ViewHolder) {
                    val binding = viewHolder.binding

                    // 해당 아이템에 속하는 정보 객체 가져오기
                    val entity = adapterItemListDataMbr[position].second

                    if (!activityMbr.isFinishing) {
                        Glide.with(activityMbr)
                            .load(entity)
                            .transform(FitCenter())
                            .into(binding.img)
                    }

                }
            }


            // ---------------------------------------------------------------------------------------------
            // <공개 메소드 공간>
            fun addItemListAtLast(itemList: ArrayList<String>) {

                val changePosition = adapterItemListDataMbr.size
                val insertItemCount = itemList.size

                val tempItemList = ArrayList<Pair<ItemType, String>>()
                for (item in itemList) {
                    tempItemList.add(Pair(ItemType.ITEM1, item))
                }

                this.adapterItemListDataMbr.addAll(tempItemList)

                notifyItemRangeInserted(changePosition, insertItemCount)
            }

            fun clearAllItem() {
                val size: Int = adapterItemListDataMbr.size
                if (size > 0) {
                    adapterItemListDataMbr.clear()
                    notifyItemRangeRemoved(0, size)
                }
            }


            // ---------------------------------------------------------------------------------------------
            // <내부 클래스 공간>
            data class Item1ViewHolder(
                val view: View,
                val binding: ItemActivityPinchImageViewListViewerAdapterItem1Binding =
                    ItemActivityPinchImageViewListViewerAdapterItem1Binding.bind(
                        view
                    )
            ) :
                RecyclerView.ViewHolder(view)

            enum class ItemType(val itemCode: Int) {
                ITEM1(1)
            }
        }
    }
}