package com.example.prowd_android_template.repository.database_room.tables

import androidx.room.*
import androidx.room.ForeignKey.CASCADE

// CameraId 테이블의 Uid 별 카메라 정보 테이블
class ActivityBasicCamera2ApiSampleCameraDetailConfigTable {
    // (테이블 구조)
    @Entity(tableName = "activity_basic_camera2_api_sample_camera_detail_config",
        foreignKeys = [
            ForeignKey(
                entity = ActivityBasicCamera2ApiSampleCameraModeConfigTable.TableVo::class,
                parentColumns = ["uid"],
                childColumns = ["activity_basic_camera2_api_sample_camera_mode_config_uid"],
                onDelete = CASCADE
            )
        ])
    data class TableVo(
        // 외례키
        @ColumnInfo(name = "activity_basic_camera2_api_sample_camera_mode_config_uid")
        val activityBasicCamera2ApiSampleCameraModeConfigUid: Long,

        // 0 : 안함
        // 1 : 촬영시
        // 2 : 항상
        @ColumnInfo(name = "flash_mode", defaultValue = "0")
        val flashMode: Int,

        // 촬영 시작 시간
        // 0초, 2초, 5초, 10초
        @ColumnInfo(name = "timer_sec", defaultValue = "0")
        val timerSec: Int,

        // 카메라 방향의 서페이스 비율
        // "1:3", "2:4" 와 같이 최대공약수로 나눈 값을 사용
        // 카메라 방향을 기준으로 저장 하기에 표시할 때엔 디바이스 방향으로 변환
        @ColumnInfo(name = "camera_orient_surface_ratio")
        val cameraOrientSurfaceRatio: String,

        // 카메라 방향의 서페이스 사이즈
        // 서페이스 비율에 따라 제공되는 서페이스 사이즈(사실상 비율에 대한 해상도) 결정
        // 카메라 방향을 기준으로 저장 하기에 표시할 때엔 디바이스 방향으로 변환
        // ex : "1000:1000"
        @ColumnInfo(name = "camera_orient_surface_size")
        val cameraOrientSurfaceSize: String

    ) {
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "uid")
        var uid: Long = 0
    }

    // (테이블 Dao)
    @Dao
    interface TableDao {
//        @Query(
//            "SELECT " +
//                    "* " +
//                    "FROM " +
//                    "test_info " +
//                    "where " +
//                    "uid = :uid"
//        )
//        fun selectColAll(uid: Int): TableVo
//
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