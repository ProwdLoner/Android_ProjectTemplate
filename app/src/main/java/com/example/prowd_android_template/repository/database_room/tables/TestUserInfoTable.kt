package com.example.prowd_android_template.repository.database_room.tables

import androidx.room.*

// 유저 샘플 관련 테스트용 샘플 테이블
class TestUserInfoTable {
    // (테이블 구조)
    @Entity(tableName = "test_user")
    data class TableVo(
        @ColumnInfo(name = "id")
        val id: String,

        @ColumnInfo(name = "nick_name")
        val nickName: String,

        @ColumnInfo(name = "password")
        val password: String
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
                    "COUNT(*) " +
                    "FROM " +
                    "test_user " +
                    "where " +
                    "id = :id"
        )
        fun getIdCount(id: String): Int

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(vararg inputTable: TableVo)

//        @Query(
//            "SELECT " +
//                    "* " +
//                    "FROM " +
//                    "test_info"
//        )
//        fun selectTestInfoColAll2(): List<TestInfoTableVO>
//
//        @Update
//        fun updateDeviceConfigInfo(vararg inputTable: TestInfoTableVO)
//
//        @Delete
//        fun deleteDeviceConfigInfo(vararg inputTable: TestInfoTableVO)
    }

}