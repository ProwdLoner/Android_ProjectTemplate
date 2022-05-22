package com.example.prowd_android_template.abstract_class

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Semaphore

// 주의 : 데이터 변경을 하고 싶을때는 Shallow Copy 로 인해 변경사항이 반영되지 않을 수 있으므로 이에 주의할 것
// itemUid 는 화면 반영 방식에 영향을 주기에 유의해서 다룰것. (애니메이션, 스크롤, 반영여부 등)
// 내부 동기화 처리는 되어있음. 데이터 리스트 조회, 조작 기능의 뮤텍스. 다만 조회와 조작을 동시에 실행하는 비동기 기능의 경우 외부적 뮤텍스를 적용할것
abstract class ProwdRecyclerViewAdapter(
    parentView: AppCompatActivity,
    targetView: RecyclerView,
    isVertical: Boolean,
    adapterVmData: AdapterVmData,
    onScrollReachTheEnd: (() -> Unit)?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // <멤버 변수 공간>
    // 현 화면에 표시된 어뎁터 데이터 리스트 (헤더, 푸터를 포함하지 않는 서브 리스트는 아이템 리스트라고 명명)
    private val currentDataListMbr: ArrayList<AdapterDataAbstractVO> = ArrayList()

    // 내부 데이터 리스트 접근 세마포어
    // 데이터 조회, 데이터 변경
    // 헤더, 푸터 및 아이템 정보를 비동기적으로 가져와 반영해도 라이브 데이터를 거친다면 싱크를 맞춰주는 역할
    private val currentDataSemaphoreMbr: Semaphore = Semaphore(1)

    // 데이터 리스트의 클론
    val currentDataListCloneMbr: ArrayList<AdapterDataAbstractVO>
        get() {
            currentDataSemaphoreMbr.acquire()
            val result: ArrayList<AdapterDataAbstractVO> = ArrayList()

            for (currentItem in currentDataListMbr) {
                result.add(getDeepCopyReplica(currentItem))
            }
            currentDataSemaphoreMbr.release()
            return result
        }

    // 아이템 리스트의 클론
    val currentItemListCloneMbr: ArrayList<AdapterItemAbstractVO>
        get() {
            currentDataSemaphoreMbr.acquire()
            if (currentDataListMbr.isEmpty()) {
                return ArrayList()
            }

            val onlyItemSubList =
                currentDataListMbr.subList(1, currentDataListMbr.lastIndex)

            val result: ArrayList<AdapterItemAbstractVO> = ArrayList()

            for (currentItem in onlyItemSubList) {
                result.add(getDeepCopyReplica(currentItem) as AdapterItemAbstractVO)
            }

            currentDataSemaphoreMbr.release()
            return result
        }

    val currentDataListLastIndexMbr: Int
        get() {
            currentDataSemaphoreMbr.acquire()
            currentDataSemaphoreMbr.release()
            return currentDataListMbr.lastIndex
        }


    // 잠재적 오동작 : 값은 오버플로우로 순환함, 만약 Long 타입 아이디가 전부 소모되고 순환될 때까지 이전 아이디가 남아있으면 아이디 중복 현상 발생
    // Long 값 최소에서 최대까지의 범위이므로 매우 드문 현상.(Long 범위의 모든 아이디가 소모된 상황)
    // 오동작 유형 : setNewItemList 를 했을 때, 동일 id로 인하여 아이템 변경 애니메이션이 잘못 실행될 가능성 존재
    var maxUidMbr = Long.MIN_VALUE
        get() {
            currentDataSemaphoreMbr.acquire()
            val firstIssue = ++field
            if (currentDataListMbr.indexOfFirst { it.itemUid == firstIssue } != -1) {
                // 발행 uid 가 리스트에 존재하면,
                while (true) {
                    // 다음 숫자들을 대입해보기
                    val uid = ++field
                    if (firstIssue == uid) {
                        // 순회해서 한바퀴를 돌았다면(== 리스트에 아이템 Long 개수만큼 아이디가 꽉 찼을 경우) 그냥 현재 필드를 반환
                        currentDataSemaphoreMbr.release()
                        return field
                    }

                    if (currentDataListMbr.indexOfFirst { it.itemUid == uid } == -1) {
                        currentDataSemaphoreMbr.release()
                        return field
                    }
                }
            } else {
                // 발행 uid 가 리스트에 존재하지 않는 것이라면
                currentDataSemaphoreMbr.release()
                return field
            }
        }


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    init {
        // (라이브 데이터 바인딩)
        // 외부에서 뷰를 변경할 때에는 각 라이브 데이터에 원하는 값을 주입만 해주면 됨
        adapterVmData.headerLiveData.observe(parentView) {
            // 헤더 갱신
            setHeader(it)
        }

        adapterVmData.footerLiveData.observe(parentView) {
            // 푸터 갱신
            setFooter(it)
        }

        adapterVmData.itemListLiveData.observe(parentView) {
            // 아이템 리스트 갱신
            setItemList(it)
        }

        // (필수 초기 데이터 생성)
        // 헤더 푸터 데이터는 필수로 존재 : 사용을 안 할 것이라면 화면단 어뎁터에서 empty container 를 사용
        if (adapterVmData.headerLiveData.value == null) {
            adapterVmData.headerLiveData.value = AdapterHeaderAbstractVO()
        }

        if (adapterVmData.footerLiveData.value == null) {
            adapterVmData.footerLiveData.value = AdapterFooterAbstractVO()
        }

        if (adapterVmData.itemListLiveData.value == null) {
            adapterVmData.itemListLiveData.value = ArrayList()
        }

        targetView.adapter = this

        val scrollAdapterLayoutManager = LinearLayoutManager(parentView)
        if (isVertical) {
            scrollAdapterLayoutManager.orientation = LinearLayoutManager.VERTICAL
        } else {
            scrollAdapterLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        }
        targetView.layoutManager = scrollAdapterLayoutManager

        // 리사이클러 뷰 스크롤 설정
        targetView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 || dx > 0) { // 스크롤 이동을 했을 때에만 발동
                    val layoutManager =
                        LinearLayoutManager::class.java.cast(recyclerView.layoutManager)

                    if (null != layoutManager) {
                        val totalItemCount = layoutManager.itemCount
                        val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
                        if (lastVisible >= totalItemCount - 1) {
                            if (onScrollReachTheEnd != null) {
                                onScrollReachTheEnd()
                            }
                        }
                    }
                }
            }
        })
    }


    // ---------------------------------------------------------------------------------------------
    // <메소드 오버라이딩 공간>
    override fun getItemCount(): Int {
        return currentDataListMbr.size
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 객체 동일성을 비교하는 함수 (아이템의 이동, 삭제 여부 파악을 위해 필요)
    // 객체 고유값을 설정해 객체 고유성을 유지시키는 것이 중요
    // 새로 생성되어 비교될 수 있으니 주소로 비교시 의도치 않게 아이템이 지워졌다 생길 수 있음
    // 같은 객체에 내용만 변할수 있으니 값 전체로 비교시 무조건 아이템이 지워졌다가 다시 생김
    // 되도록 객체 동일성을 보장하는 고유값을 객체에 넣어서 사용 할것.
    private fun isItemSame(
        oldItem: AdapterDataAbstractVO,
        newItem: AdapterDataAbstractVO
    ): Boolean {
        return oldItem.itemUid == newItem.itemUid
    }

    // (화면 갱신 함수)
    // 헤더만 갱신
    private fun setHeader(headerItem: AdapterHeaderAbstractVO) {
        currentDataSemaphoreMbr.acquire()
        if (currentDataListMbr.isEmpty() ||
            currentDataListMbr.first() !is AdapterHeaderAbstractVO
        ) {
            currentDataListMbr.add(0, headerItem)
            notifyItemInserted(0)
        } else {
            currentDataListMbr[0] = getDeepCopyReplica(headerItem)
            notifyItemChanged(0)
        }
        currentDataSemaphoreMbr.release()
    }

    // 푸터만 갱신
    private fun setFooter(footerItem: AdapterFooterAbstractVO) {
        currentDataSemaphoreMbr.acquire()
        if (currentDataListMbr.isEmpty() ||
            currentDataListMbr.last() !is AdapterFooterAbstractVO
        ) {
            currentDataListMbr.add(footerItem)
            notifyItemInserted(currentDataListMbr.lastIndex)
        } else {
            currentDataListMbr[currentDataListMbr.lastIndex] = getDeepCopyReplica(footerItem)
            notifyItemChanged(currentDataListMbr.lastIndex)
        }
        currentDataSemaphoreMbr.release()
    }

    // 아이템 리스트 갱신 (헤더, 푸터는 제외한 아이템만 갱신)
    private fun setItemList(
        newItemList: ArrayList<AdapterItemAbstractVO>
    ) {
        currentDataSemaphoreMbr.acquire()
        // 요청 리스트 사이즈가 0 일 때에는 모든 아이템을 제거

        if (newItemList.size == 0) {
            if (currentDataListMbr.size == 2
            ) {
                currentDataSemaphoreMbr.release()
                return
            } else { // 아이템이 3개 이상일 때
                // 헤더 푸터만 유지하고 제거
                val removeItemCount = currentDataListMbr.size - 2
                currentDataListMbr.subList(1, currentDataListMbr.lastIndex).clear()

                notifyItemRangeRemoved(1, removeItemCount)

                currentDataSemaphoreMbr.release()
                return
            }
        }

        // 현재 리스트 아이템이 비어있다면 무조건 Add
        if (currentDataListMbr.size == 2) {
            // index 1 부터 모두 add
            val newItemListSize = newItemList.size

            currentDataListMbr.addAll(1, newItemList)

            notifyItemRangeInserted(1, newItemListSize)

            currentDataSemaphoreMbr.release()
            return
        }

        // 여기까지, newList 가 비어있을 때, currentList 가 비어있을 때의 처리 (이제 두 리스트는 1개 이상의 아이템을 지님)
        // 위에서 걸러지지 못했다면 본격적인 아이템 비교가 필요
        val currentItemListOnlyItemSubList = currentDataListMbr.subList(
            1,
            currentDataListMbr.lastIndex
        )

        // 각 리스트의 동위 아이템을 비교하는 개념
        var idx = 0
        while (true) {
            // 위치 확인
            if (idx > newItemList.lastIndex &&
                idx > currentItemListOnlyItemSubList.lastIndex
            ) {
                currentDataSemaphoreMbr.release()
                return
            }

            if (idx > newItemList.lastIndex) {
                // 현재 인덱스가 뉴 리스트 마지막 인덱스를 넘어설 때,
                // 여기부터 현 리스트 뒤를 날려버려 뉴 리스트와 맞추기
                val deleteEndIdx = currentItemListOnlyItemSubList.size
                currentItemListOnlyItemSubList.subList(idx, currentItemListOnlyItemSubList.size)
                    .clear()
                notifyItemRangeRemoved(
                    idx + 1,
                    deleteEndIdx - idx
                )

                currentDataSemaphoreMbr.release()
                return
            }

            if (idx > currentItemListOnlyItemSubList.lastIndex) {
                // 현재 인덱스가 구 리스트 마지막 인덱스를 넘어설 때,
                // 여기부터 현 리스트 뒤에 뉴 리스트 남은 아이템들을 추가시키기
                val deleteEndIdx = newItemList.size
                currentItemListOnlyItemSubList.addAll(newItemList.subList(idx, deleteEndIdx))
                notifyItemRangeInserted(idx + 1, deleteEndIdx - idx)

                currentDataSemaphoreMbr.release()
                return
            }

            // 여기부턴 해당 위치에 old, new 아이템 2개가 쌍으로 존재함
            val oldItem = currentItemListOnlyItemSubList[idx]
            val newItem = newItemList[idx]

            // 동일성 비교
            if (isItemSame(oldItem, newItem)) {
                // 두 아이템이 동일함 = 이동할 필요가 없음, 내용이 바뀌었을 가능성이 있음
                if (!isContentSame(oldItem, newItem)) {
                    // 아이템 내용이 수정된 상태
                    currentItemListOnlyItemSubList[idx] = newItem

                    notifyItemChanged(idx + 1)
                }
                // 위를 통과했다면 현 위치 아이템은 변경 필요가 없는 상태
            } else {
                // 두 아이템이 동일하지 않음 = old 아이템 이동/제거, new 아이템 생성 가능성이 있음

                // 이동 확인
                // 현 인덱스부터 뒤로 구 리스트에 newItem 과 동일한 아이템이 있는지를 확인.
                // 있다면 구 리스트의 해당 아이템과 현 위치 아이템을 스위칭 후 이동 처리
                // 없다면 검색 인덱스는 -1 로 하고, new Item add
                var searchedIdx = -1
                val nextSearchIdx = idx + 1 // 앞 인덱스는 뉴, 올드 상호간 동기화 된 상태이기에 현 인덱스 뒤에서 검색
                if (nextSearchIdx <= currentItemListOnlyItemSubList.lastIndex) {
                    for (searchIdx in nextSearchIdx..currentItemListOnlyItemSubList.lastIndex) {
                        val searchOldItem = currentItemListOnlyItemSubList[searchIdx]
                        if (isItemSame(searchOldItem, newItem)) {
                            searchedIdx = searchIdx
                            break
                        }
                    }
                }

                if (-1 == searchedIdx) {
                    // 동일 아이템이 검색되지 않았다면 newItem 을 해당 위치에 생성
                    currentItemListOnlyItemSubList.add(idx, newItem)
                    notifyItemInserted(idx + 1)
                } else {
                    // 동일 아이템이 검색되었다면,
                    // newItem 을 해당 위치로 이동시키고, 내용 동일성 검증

                    // newItem 을 해당 위치로 이동
                    // oldItem 은 newItem 위치(뒤쪽 인덱스)로 이동을 하다가 제자리로 찾아가거나,
                    // 혹은 지워진 상태라면 나중에 한번에 삭제됨
                    val searchedItem =
                        getDeepCopyReplica(currentItemListOnlyItemSubList[searchedIdx])

                    currentItemListOnlyItemSubList.removeAt(searchedIdx)
                    currentItemListOnlyItemSubList.add(idx, searchedItem)

                    notifyItemMoved(searchedIdx + 1, idx + 1)

                    // 내용 동일성 검증
                    if (!isContentSame(searchedItem, newItem)) {
                        // 내용이 변경된 경우
                        currentItemListOnlyItemSubList[idx] = newItem

                        notifyItemChanged(idx + 1)
                    }
                }
            }

            ++idx
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <추상 메소드 공간>
    // 아이템 내용 변화 여부를 파악하는 함수 (아이템의 수정 여부 파악을 위해 필요)
    // 화면을 변화시킬 필요가 있는 요소들을 전부 비교시킴
    protected abstract fun isContentSame(
        oldItem: AdapterDataAbstractVO,
        newItem: AdapterDataAbstractVO
    ): Boolean

    // newItem 의 깊은 복사 객체 반환
    // 어뎁터 내부 데이터(oldData)와 어뎁터 외부 데이터(일반적으로 뷰모델)를 분리하기 위한 것
    // 만약 어뎁터 내부 데이터에 newData 객체를 그대로 add 해주면, 둘의 주소값이 같아지므로,
    // 어뎁터 데이터를 수정시에 비교 대상과 새로운 데이터 내부 데이터가 항상 같아지므로,
    // isContentSame 를 제대로 동작 시키기 위해 내부 데이터는 깊은 복사를 사용
    protected abstract fun getDeepCopyReplica(
        newItem: AdapterDataAbstractVO
    ): AdapterDataAbstractVO


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // 헤더 데이터 itemUid 는 1로 고정
    open class AdapterHeaderAbstractVO :
        AdapterDataAbstractVO(1)

    // 헤더 데이터 itemUid 는 2로 고정
    open class AdapterFooterAbstractVO :
        AdapterDataAbstractVO(2)

    open class AdapterItemAbstractVO(override val itemUid: Long) :
        AdapterDataAbstractVO(itemUid)

    abstract class AdapterDataAbstractVO(open val itemUid: Long)

    open class AdapterVmData {
        val itemListLiveData: MutableLiveData<ArrayList<AdapterItemAbstractVO>> = MutableLiveData()
        val headerLiveData: MutableLiveData<AdapterHeaderAbstractVO> = MutableLiveData()
        val footerLiveData: MutableLiveData<AdapterFooterAbstractVO> = MutableLiveData()
    }
}