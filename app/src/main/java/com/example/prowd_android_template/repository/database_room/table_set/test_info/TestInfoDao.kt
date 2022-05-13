package com.example.prowd_android_template.repository.database_room.table_set.test_info

import androidx.room.*

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
    fun selectTestInfoColAll(uid: Int): TestInfoTable

    @Query(
        "SELECT " +
                "* " +
                "FROM " +
                "test_info"
    )
    fun selectTestInfoColAll2(): List<TestInfoTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeviceConfigInfo(vararg inputTable: TestInfoTable)

    @Update
    fun updateDeviceConfigInfo(vararg inputTable: TestInfoTable)

    @Delete
    fun deleteDeviceConfigInfo(vararg inputTable: TestInfoTable)
}