package com.example.prowd_android_template.repository.database_room.database_set.app.table_set.test_info

import androidx.room.*

@Entity(tableName = "test_info")
data class TestInfoTable(
    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "age")
    val age: Int
){
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uid")
    var uid: Int = 0
}