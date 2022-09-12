package com.example.prowd_android_template.repository.database_room.tables

import androidx.room.*

// 해당 카메라가 무슨 모드를 사용하는지에 대한 테이블
class ActivityBasicCamera2ApiSampleCameraModeConfigTable {
    // (테이블 구조)
    @Entity(tableName = "activity_basic_camera2_api_sample_camera_mode_config")
    data class TableVo(
        @ColumnInfo(name = "camera_id")
        val cameraId: String,

        // 1 : 사진
        // 2 : 동영상
        @ColumnInfo(name = "camera_mode", defaultValue = "1")
        val cameraMode: Int,

        // 현재 설정인지
        @ColumnInfo(name = "mode_on")
        val modeOn: Boolean

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
                    "camera_mode " +
                    "FROM " +
                    "activity_basic_camera2_api_sample_camera_mode_config " +
                    "where " +
                    "camera_id = :cameraId and " +
                    "mode_on = TRUE"
        )
        fun getCurrentCameraMode(cameraId: String): Int?

//        @Query(
//            "SELECT " +
//                    "* " +
//                    "FROM " +
//                    "test_info"
//        )
//        fun selectColAll2(): List<TableVo>
//
//        @Insert(onConflict = OnConflictStrategy.REPLACE)
//        fun insert(vararg inputTable: TableVo)
//
//        @Update
//        fun update(vararg inputTable: TableVo)
//
//        @Delete
//        fun delete(vararg inputTable: TableVo)
    }
}