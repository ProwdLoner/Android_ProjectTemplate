package com.example.prowd_android_template.abstract_class

import android.app.Activity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.sync.Semaphore

// 주의 : 데이터 변경을 하고 싶을때는 Shallow Copy 로 인해 변경사항이 반영되지 않을 수 있으므로 이에 주의할 것
// itemUid 는 화면 반영 방식에 영향을 주기에 유의해서 다룰것. (애니메이션, 스크롤, 반영여부 등)
// 비동기 처리시 뮤텍스와 싱크에 주의할 것.
abstract class AbstractRecyclerViewAdapter(
    parentView: Activity,
    targetView: RecyclerView,
    isVertical: Boolean,
    initialList: ArrayList<AdapterItemAbstractVO>,
    onScrollHitBottom: (() -> Unit)?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // <멤버 변수 공간>
    // 현 화면에 표시된 어뎁터 데이터 리스트
    val currentItemListMbr: ArrayList<AdapterItemAbstractVO> = initialList


    // todo 헤더 푸터 고정 아이디 존재로 인해 개선 필요
    // 잠재적 오동작 : 값은 오버플로우로 순환함, 만약 Long 타입 아이디가 전부 소모되고 순환될 때까지 이전 아이디가 남아있으면 아이디 중복 현상 발생
    // Long 값 최소에서 최대로 갈 때까지 진행되므로, 그때까지 아이템 리스트에 남아있을 가능성은 매우 드뭄
    // 오동작 유형 : setNewItemList 를 했을 때, 동일 id로 인하여 아이템 변경 애니메이션이 잘못 실행될 가능성 존재(심각한 에러는 아님)
    var maxUidMbr = Long.MIN_VALUE
        get() {
            return field++
        }

    val headerUidMbr = maxUidMbr
    val footerUidMbr = maxUidMbr


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    init {
        targetView.adapter = this

        val scrollAdapterLayoutManager = LinearLayoutManager(parentView)
        if (isVertical) {
            scrollAdapterLayoutManager.orientation = LinearLayoutManager.VERTICAL
        } else {
            scrollAdapterLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        }
        targetView.layoutManager = scrollAdapterLayoutManager

        // 리사이클러 뷰 스크롤 설정
        targetView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) { // 스크롤 다운을 했을 때에만 발동
                    val visibleItemCount = scrollAdapterLayoutManager.childCount
                    val totalItemCount = scrollAdapterLayoutManager.itemCount
                    val pastVisibleItems =
                        scrollAdapterLayoutManager.findFirstVisibleItemPosition()

                    if (visibleItemCount + pastVisibleItems >= totalItemCount) {
                        if (onScrollHitBottom != null) {
                            onScrollHitBottom()
                        }
                    }
                }
            }
        })
    }


    // ---------------------------------------------------------------------------------------------
    // <메소드 오버라이딩 공간>
    override fun getItemCount(): Int {
        return currentItemListMbr.size
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // 현재 아이템 리스트의 클론을 생성하여 반환
    fun getCurrentItemDeepCopyReplica(): ArrayList<AdapterItemAbstractVO> {
        val result: ArrayList<AdapterItemAbstractVO> = ArrayList()

        for (currentItem in currentItemListMbr) {
            result.add(getDeepCopyReplica(currentItem))
        }

        return result
    }

    // 헤더, 푸터를 제외한 아이템 리스트 클론만 반환
    fun getCurrentItemDeepCopyReplicaOnlyItem(): ArrayList<AdapterItemAbstractVO> {
        if (currentItemListMbr.isEmpty()) {
            return ArrayList()
        } else if (currentItemListMbr.size == 1) {
            return if (currentItemListMbr[0] is AdapterHeaderAbstractVO ||
                currentItemListMbr[0] is AdapterFooterAbstractVO
            ) {
                ArrayList()
            } else {
                currentItemListMbr
            }
        } else if (currentItemListMbr.size == 2) {
            return if (currentItemListMbr.first() is AdapterHeaderAbstractVO &&
                currentItemListMbr.last() is AdapterFooterAbstractVO
            ) {
                ArrayList()
            } else {
                if (currentItemListMbr.first() is AdapterHeaderAbstractVO) {
                    arrayListOf(currentItemListMbr.last())
                } else {
                    arrayListOf(currentItemListMbr.first())
                }
            }
        }

        // 헤더를 제외한 아이템 시작 인덱스
        val currentOnlyItemStartIdx =
            if (currentItemListMbr.first() is AdapterHeaderAbstractVO) {
                1
            } else {
                0
            }

        // 푸터를 제외한 아이템 끝 인덱스
        val currentOnlyItemEndIdx = if (currentItemListMbr.last() is AdapterFooterAbstractVO) {
            currentItemListMbr.lastIndex - 1
        } else {
            currentItemListMbr.lastIndex
        }


        val onlyItemSubList =
            currentItemListMbr.subList(currentOnlyItemStartIdx, currentOnlyItemEndIdx + 1)

        val result: ArrayList<AdapterItemAbstractVO> = ArrayList()

        for (currentItem in onlyItemSubList) {
            result.add(getDeepCopyReplica(currentItem))
        }

        return result
    }

    // (화면 갱신 함수)
    // 헤더만 갱신
    fun updateHeader(headerItem: AdapterHeaderAbstractVO) {
        currentItemListMbr[0] = getDeepCopyReplica(headerItem)

        notifyItemChanged(0)
    }

    // 푸터만 갱신
    fun updateFooter(footerItem: AdapterFooterAbstractVO) {
        currentItemListMbr[currentItemListMbr.lastIndex] = getDeepCopyReplica(footerItem)

        notifyItemChanged(currentItemListMbr.lastIndex)
    }

    // todo 검증
    // 아이템 리스트 갱신 (헤더, 푸터는 제외한 아이템만 갱신)
    fun updateItemList(
        newItemList: ArrayList<AdapterItemAbstractVO>
    ) {
        // (newItemList 에서 순환하며, 가장 앞에서부터 비교하며 싱크를 맞추기)
        // 반영 리스트 사이즈가 0 일 때에는 모든 아이템을 제거
        if (newItemList.size == 0) {
            if (currentItemListMbr.isEmpty()) {
                // 현재 리스트도 비어있는 상태라면 return
                return
            } else if (currentItemListMbr.size == 1) {
                // 리스트 아이템이 하나
                if (currentItemListMbr[0] is AdapterHeaderAbstractVO ||
                    currentItemListMbr[0] is AdapterFooterAbstractVO
                ) { // 하나 남은 아이템이 헤더 혹은 푸터이면 return
                    return
                } else { // 하나 남은 아이템이 헤더 혹은 푸터가 아닐 때,
                    currentItemListMbr.removeAt(0)

                    notifyItemRemoved(0)

                    return
                }
            } else if (currentItemListMbr.size == 2) {
                // 리스트 아이템이 두개
                if (currentItemListMbr.first() is AdapterHeaderAbstractVO &&
                    currentItemListMbr.last() is AdapterFooterAbstractVO
                ) { // 아이템 두개가 모두 헤더와 푸터일 때
                    return
                } else if (currentItemListMbr.first() is AdapterHeaderAbstractVO) {
                    // 아이템 헤더만 유지
                    currentItemListMbr.removeAt(1)

                    notifyItemRemoved(1)

                    return
                } else if (currentItemListMbr.last() is AdapterFooterAbstractVO) {
                    // 아이템 푸터만 유지
                    currentItemListMbr.removeAt(0)

                    notifyItemRemoved(0)

                    return
                } else {
                    // 아이템 두개를 모두 제거해야 할 때
                    currentItemListMbr.clear()

                    notifyItemRangeRemoved(0, 2)

                    return
                }
            } else {
                // 아이템이 3개 이상일 때
                if (currentItemListMbr.first() is AdapterHeaderAbstractVO &&
                    currentItemListMbr.last() is AdapterFooterAbstractVO
                ) { // 헤더 푸터 모두 유지
                    val itemStartIdx = 1
                    val itemEndIdx = currentItemListMbr.lastIndex - 1
                    val changeItemCount = currentItemListMbr.size - 2
                    currentItemListMbr.subList(itemStartIdx, itemEndIdx + 1).clear()

                    notifyItemRangeRemoved(itemStartIdx, changeItemCount)

                    return
                } else if (currentItemListMbr.first() is AdapterHeaderAbstractVO) {
                    // 헤더를 유지
                    val itemStartIdx = 1
                    val itemEndIdx = currentItemListMbr.lastIndex
                    val changeItemCount = currentItemListMbr.size - 1
                    currentItemListMbr.subList(itemStartIdx, itemEndIdx + 1).clear()

                    notifyItemRangeRemoved(itemStartIdx, changeItemCount)

                    return
                } else if (currentItemListMbr.last() is AdapterFooterAbstractVO) {
                    // 푸터를 유지
                    val itemStartIdx = 0
                    val itemEndIdx = currentItemListMbr.lastIndex - 1
                    val changeItemCount = currentItemListMbr.size - 1
                    currentItemListMbr.subList(itemStartIdx, itemEndIdx + 1).clear()

                    notifyItemRangeRemoved(itemStartIdx, changeItemCount)

                    return
                } else {
                    // 헤더, 푸터 모두 유지하지 않음
                    val changeItemCount = currentItemListMbr.size
                    currentItemListMbr.clear()

                    notifyItemRangeRemoved(0, changeItemCount)
                    return
                }
            }
        }

        if (currentItemListMbr.isEmpty()) {
            // 현재 리스트가 비어있다면 모두 add
            val newItemListSize = newItemList.size

            currentItemListMbr.addAll(newItemList)

            notifyItemRangeInserted(0, newItemListSize)
            return
        } else if ((currentItemListMbr.size == 2 &&
                    currentItemListMbr.first() is AdapterHeaderAbstractVO &&
                    currentItemListMbr.last() is AdapterFooterAbstractVO) ||
            (currentItemListMbr.size == 1 &&
                    currentItemListMbr.first() is AdapterHeaderAbstractVO)
        ) {
            // 현재 리스트가 아이템이 2개고 모두 헤더와 푸터일 때와, 현재 리스트가 아이템이 1개고 헤더가 존재할 때,
            // index 1 부터 모두 add
            val newItemListSize = newItemList.size

            currentItemListMbr.addAll(1, newItemList)

            notifyItemRangeInserted(1, newItemListSize)
            return
        } else if (currentItemListMbr.size == 1 &&
            currentItemListMbr.last() is AdapterFooterAbstractVO
        ) {
            // 현재 리스트가 아이템이 1개고 유일한 아이템이 푸터일 때
            val newItemListSize = newItemList.size

            currentItemListMbr.addAll(0, newItemList)

            notifyItemRangeInserted(0, newItemListSize)
            return
        }

        // (아이템 리스트를 순회하며 현재 아이템 리스트와 비교하여 반영)
        // 새 아이템 리스트 마지막 인덱스
        val newItemListLastIdx = newItemList.size - 1

        for (newItemListCurrentIdx in 0..newItemListLastIdx) { // newItemList 와 싱크 맞추기 시작
            // 비교된 아이템은 유지(내용 변경, 변동 무), 이동, 삭제, 추가를 실행
            val newItem = newItemList[newItemListCurrentIdx]

            // 헤더를 제외한 아이템 시작 인덱스
            val currentOnlyItemStartIdx =
                if (currentItemListMbr.first() is AdapterHeaderAbstractVO) {
                    1
                } else {
                    0
                }

            // 푸터를 제외한 아이템 끝 인덱스
            val currentOnlyItemEndIdx = if (currentItemListMbr.last() is AdapterFooterAbstractVO) {
                currentItemListMbr.lastIndex - 1
            } else {
                currentItemListMbr.lastIndex
            }

            val onlyItemSubList =
                currentItemListMbr.subList(currentOnlyItemStartIdx, currentOnlyItemEndIdx + 1)

            if (newItemListCurrentIdx > onlyItemSubList.lastIndex) {
                // currentItemListMbr 마지막 아이템부터 뒤로 아이템이 없을 때,
                // currentItemListMbr 에 새로운 아이템 추가
                val changePosition = currentOnlyItemEndIdx + 1

                onlyItemSubList.add(getDeepCopyReplica(newItem))

                notifyItemInserted(changePosition)
            } else { // 해당 위치에 비교용 데이터가 존재할 때,
                // 아이템 비교 실행.
                // 아이템 유지 여부 확인
                if (isItemSame(
                        onlyItemSubList[newItemListCurrentIdx],
                        newItem
                    )
                ) { // 두 아이템이 일치시(위치가 동일)
                    // 아이템 수정 여부 확인
                    if (!isContentSame(
                            onlyItemSubList[newItemListCurrentIdx],
                            newItem
                        )
                    ) {
                        // 수정 사항이 존재
                        onlyItemSubList[newItemListCurrentIdx] =
                            getDeepCopyReplica(newItem)

                        notifyItemChanged(newItemListCurrentIdx + currentOnlyItemStartIdx)
                    }
                } else { // 두 아이템이 일치하지 않을 때 (위치가 다르거나 새로 생성되었을 가능성이 존재)
                    // currentItem 이동 및, newItem 추가
                    // 이번 newItem 인덱스에서 싱크를 맞추고 다음으로 이동
                    var searchedIdx = -1

                    // 현 완료된 인덱스 뒤 + 1 (일치성 검사 끝난 다음부터) 비교
                    // 인덱스가 존재하면 searchedIdx 에 해당 인덱스가 저장되어서 이동시키고,
                    // 존재하지 않아서 searchedIdx 가 끝까지 -1 이라면 아이템 생성
                    for (oldItemIdx in newItemListCurrentIdx + 1 until onlyItemSubList.size) {
                        val oldItem = onlyItemSubList[oldItemIdx]

                        // 동일한 아이템 인덱스를 발견시 searchedIdx 에 인덱스 저장 후 빠져나오기.
                        // 없다면 -1
                        if (isItemSame(oldItem, newItem)) {
                            searchedIdx = oldItemIdx
                            break
                        }
                    }

                    if (-1 == searchedIdx) {
                        // newItem 과 동일한 아이템이 currentItemListMbr 에 없을 때
                        // = 아이템을 이동할 필요가 없으므로 newItem 을 생성
                        onlyItemSubList.add(
                            newItemListCurrentIdx,
                            getDeepCopyReplica(newItem)
                        )

                        notifyItemInserted(newItemListCurrentIdx + currentOnlyItemStartIdx)
                    } else {
                        // searchedIdx 에 있는 아이템을 newItemIdx 위치로 이동
                        val removedData =
                            onlyItemSubList.removeAt(searchedIdx)

                        onlyItemSubList.add(
                            newItemListCurrentIdx,
                            getDeepCopyReplica(removedData)
                        )

                        notifyItemMoved(
                            searchedIdx + currentOnlyItemStartIdx,
                            newItemListCurrentIdx + currentOnlyItemStartIdx
                        )

                        // 이동하여 위치를 동일하게 맞췄으니 컨텐츠가 동일한지 판단
                        if (!isContentSame(
                                onlyItemSubList[newItemListCurrentIdx],
                                newItem
                            )
                        ) {
                            // 이동된 아이템 내용이 새로운 아이템과 다를 때 = 수정
                            onlyItemSubList[newItemListCurrentIdx] =
                                getDeepCopyReplica(newItem)

                            notifyItemChanged(newItemListCurrentIdx + currentOnlyItemStartIdx)
                        }
                    }
                }
            }

            // 마지막 비교 시점에 처리를 완료하고, currentItemListMbr 의
            // 남은 데이터(새로운 리스트에 존재하지 않아 뒤로 밀려난 데이터들)를 소멸
            if (newItemListLastIdx == newItemListCurrentIdx && // newItemList 마지막 인덱스
                newItemListCurrentIdx <= onlyItemSubList.size - 1
            ) { // 비교 마지막인데 아직 아이템이 남아있을 경우
                // currentItemListMbr 나머지 데이터를 지우기
                val deleteStartIdx = newItemListCurrentIdx + 1
                val deleteEndIdx = onlyItemSubList.size - 1
                var numOfChange = 0

                // 뒤에서부터 지우기
                for (i in deleteEndIdx downTo deleteStartIdx) {
                    onlyItemSubList.removeAt(i)
                    numOfChange++
                }

                notifyItemRangeRemoved(
                    deleteStartIdx - 1 + currentOnlyItemStartIdx,
                    numOfChange + currentOnlyItemStartIdx
                )
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 객체 동일성을 비교하는 함수 (아이템의 이동, 삭제 여부 파악을 위해 필요)
    // 객체 고유값을 설정해 객체 고유성을 유지시키는 것이 중요
    // 새로 생성되어 비교될 수 있으니 주소로 비교시 의도치 않게 아이템이 지워졌다 생길 수 있음
    // 같은 객체에 내용만 변할수 있으니 값 전체로 비교시 무조건 아이템이 지워졌다가 다시 생김
    // 되도록 객체 동일성을 보장하는 고유값을 객체에 넣어서 사용 할것.
    private fun isItemSame(
        oldItem: AdapterItemAbstractVO,
        newItem: AdapterItemAbstractVO
    ): Boolean {
        return oldItem.itemUid == newItem.itemUid
    }


    // ---------------------------------------------------------------------------------------------
    // <추상 메소드 공간>
    // 아이템 내용 변화 여부를 파악하는 함수 (아이템의 수정 여부 파악을 위해 필요)
    // 화면을 변화시킬 필요가 있는 요소들을 전부 비교시킴
    protected abstract fun isContentSame(
        oldItem: AdapterItemAbstractVO,
        newItem: AdapterItemAbstractVO
    ): Boolean

    // newItem 의 깊은 복사 객체 반환
    // 어뎁터 내부 데이터(oldData)와 어뎁터 외부 데이터(일반적으로 뷰모델)를 분리하기 위한 것
    // 만약 어뎁터 내부 데이터에 newData 객체를 그대로 add 해주면, 둘의 주소값이 같아지므로,
    // 어뎁터 데이터를 수정시에 비교 대상과 새로운 데이터 내부 데이터가 항상 같아지므로,
    // isContentSame 를 제대로 동작 시키기 위해 내부 데이터는 깊은 복사를 사용
    protected abstract fun getDeepCopyReplica(
        newItem: AdapterItemAbstractVO
    ): AdapterItemAbstractVO


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    abstract class AdapterHeaderAbstractVO(override val itemUid: Long) :
        AdapterItemAbstractVO(itemUid)

    abstract class AdapterFooterAbstractVO(override val itemUid: Long) :
        AdapterItemAbstractVO(itemUid)

    abstract class AdapterItemAbstractVO(open val itemUid: Long)
}