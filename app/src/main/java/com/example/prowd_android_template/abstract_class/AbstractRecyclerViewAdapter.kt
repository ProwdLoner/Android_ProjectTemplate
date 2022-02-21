package com.example.prowd_android_template.abstract_class

import androidx.recyclerview.widget.RecyclerView

// 주의 : 데이터 변경을 하고 싶을때는 Shallow Copy 로 인해 변경사항이 반영되지 않을 수 있으므로 이에 주의할 것
abstract class AbstractRecyclerViewAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // <멤버 변수 공간>
    // 현 화면에 표시된 어뎁터 데이터 리스트
    val currentItemListMbr: ArrayList<AdapterItemAbstractVO> =
        arrayListOf()

    // todo : 안전 처리 = 만약 이미 존재하는 uid 라면 중복 금지(정수값 최소에서 최대로 갈 때까지 진행되므로, 그때까지 아이템 리스트에 남아있을 가능성은 매우 드뭄)
    // todo : 또한 모든 정수값이 가득 찼을 때에 대한 처리
    var maxUid = Int.MIN_VALUE
        get() {
            return field++
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

    // 화면을 갱신
    fun setNewItemList(newItemList: ArrayList<AdapterItemAbstractVO>) {
        // (newItemList 에서 순환하며, 가장 앞에서부터 비교하며 싱크를 맞추기)

        // 새 아이템 리스트 마지막 인덱스
        val newItemListLastIdx = newItemList.size - 1

        for (newItemListCurrentIdx in 0..newItemListLastIdx) { // newItemList 와 싱크 맞추기 시작
            // 비교된 아이템은 유지(내용 변경, 변동 무), 이동, 삭제, 추가를 실행
            val newItem = newItemList[newItemListCurrentIdx]

            if (newItemListCurrentIdx > currentItemListMbr.size - 1) { // currentItemListMbr 해당 위치에 데이터가 없을 때
                // currentItemListMbr 에 새로운 아이템 추가
                val changePosition = currentItemListMbr.size

                currentItemListMbr.add(getDeepCopyReplica(newItem))

                notifyItemInserted(changePosition)
            } else { // 해당 위치에 비교용 데이터가 존재할 떼
                // 아이템 유지 여부 확인
                if (isItemSame(
                        currentItemListMbr[newItemListCurrentIdx],
                        newItem
                    )
                ) { // 두 아이템이 일치시
                    // 아이템 수정 여부 확인
                    if (!isContentSame(
                            currentItemListMbr[newItemListCurrentIdx],
                            newItem
                        )
                    ) {
                        // 수정 사항이 존재
                        currentItemListMbr[newItemListCurrentIdx] =
                            getDeepCopyReplica(newItem)

                        notifyItemChanged(newItemListCurrentIdx)
                    }
                } else { // 두 아이템이 일치하지 않을 때
                    // currentItem 이동, 추가 (삭제 아이템은 뒤로 밀려나서 한꺼번에 해소)
                    // 이번 newItem 인덱스에서 싱크를 맞추고 다음으로 이동
                    var searchedIdx = -1

                    // 현완료된 인덱스 뒤 + 1 (일치성 검사 끝난 다음부터)
                    for (oldItemIdx in newItemListCurrentIdx + 1 until currentItemListMbr.size) {
                        val oldItem = currentItemListMbr[oldItemIdx]

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
                        currentItemListMbr.add(
                            newItemListCurrentIdx,
                            getDeepCopyReplica(newItem)
                        )

                        notifyItemInserted(newItemListCurrentIdx)
                    } else {
                        // searchedIdx 에 있는 아이템을 newItemIdx 위치로 이동
                        val removedData =
                            currentItemListMbr.removeAt(searchedIdx)

                        currentItemListMbr.add(
                            newItemListCurrentIdx,
                            getDeepCopyReplica(removedData)
                        )

                        notifyItemMoved(searchedIdx, newItemListCurrentIdx)

                        // 컨텐츠 변경
                        if (!isContentSame(
                                currentItemListMbr[newItemListCurrentIdx],
                                newItem
                            )
                        ) {
                            // 이동된 아이템 내용이 새로운 아이템과 다를 때 = 수정

                            currentItemListMbr[newItemListCurrentIdx] =
                                getDeepCopyReplica(newItem)

                            notifyItemChanged(newItemListCurrentIdx)
                        }
                    }
                }
            }

            // 마지막 비교 시점에 처리를 완료하고, currentItemListMbr 의 남은 데이터를 소멸
            if (newItemListLastIdx == newItemListCurrentIdx && // newItemList 마지막 인덱스
                newItemListCurrentIdx <= currentItemListMbr.size - 1
            ) { // 비교 마지막인데 아직 아이템이 남아있을 경우
                // currentItemListMbr 나머지 데이터를 지우기
                val deleteStartIdx = newItemListCurrentIdx + 1
                val deleteEndIdx = currentItemListMbr.size - 1
                var numOfChange = 0

                // 뒤에서부터 지우기
                for (i in deleteEndIdx downTo deleteStartIdx) {
                    currentItemListMbr.removeAt(i)
                    numOfChange++
                }

                notifyItemRangeRemoved(deleteStartIdx - 1, numOfChange)
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
    // <내부 클래스 공간>
    abstract class AdapterItemAbstractVO(open val itemUid: Int)
}