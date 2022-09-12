package com.example.prowd_android_template.repository.database_room.tables

import androidx.room.*

// (DB에 속하는 테이블 명세)
// 크게 테이블 구조를 나타내는 Entity 와, 해당 테이블에 대한 쿼리문들을 저장하는 Dao 로 나뉨
// Dao 작성은 아래 샘플을 참고하여 SQL 어노테이션을 단 추상함수를 생성하여 사용
class TestInfoTable {
    // (테이블 구조)
    @Entity(tableName = "test_info")
    data class TableVo(
        @ColumnInfo(name = "name")
        val name: String,

        @ColumnInfo(name = "age")
        val age: Int
    ) {
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "uid")
        var uid: Long = 0
    }

    // (테이블 Dao)
    @Dao
    interface TableDao {
        @Query(
            "SELECT " +
                    "* " +
                    "FROM " +
                    "test_info " +
                    "where " +
                    "uid = :uid"
        )
        fun selectColAll(uid: Int): TableVo

        @Query(
            "SELECT " +
                    "* " +
                    "FROM " +
                    "test_info"
        )
        fun selectColAll2(): List<TableVo>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(vararg inputTable: TableVo)

        @Update
        fun update(vararg inputTable: TableVo)

        @Delete
        fun delete(vararg inputTable: TableVo)
    }

}