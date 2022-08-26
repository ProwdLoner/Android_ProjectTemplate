package com.example.prowd_android_template.repository.database_room.tables

import androidx.room.*

class TestInfoTable {
    // (테이블 구조)
    @Entity(tableName = "test_info")
    data class TestInfoTableVO(
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
    interface TestInfoDao {
        @Query(
            "SELECT " +
                    "* " +
                    "FROM " +
                    "test_info " +
                    "where " +
                    "uid = :uid"
        )
        fun selectTestInfoColAll(uid: Int): TestInfoTableVO

        @Query(
            "SELECT " +
                    "* " +
                    "FROM " +
                    "test_info"
        )
        fun selectTestInfoColAll2(): List<TestInfoTableVO>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertDeviceConfigInfo(vararg inputTable: TestInfoTableVO)

        @Update
        fun updateDeviceConfigInfo(vararg inputTable: TestInfoTableVO)

        @Delete
        fun deleteDeviceConfigInfo(vararg inputTable: TestInfoTableVO)
    }

}