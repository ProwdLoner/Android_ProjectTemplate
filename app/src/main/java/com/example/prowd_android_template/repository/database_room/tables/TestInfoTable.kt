package com.example.prowd_android_template.repository.database_room.tables

import androidx.room.*

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
        var uid: Int = 0
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