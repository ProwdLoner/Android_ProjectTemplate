package com.example.prowd_android_template.repository.database_room.tables

import androidx.room.*
import androidx.room.ForeignKey.CASCADE

// CameraId 테이블의 Uid 별 카메라 정보 테이블
class ActivityBasicCamera2ApiSampleCameraConfigTable {
    // (테이블 구조)
    @Entity(
        tableName = "activity_basic_camera2_api_sample_camera_config",
        primaryKeys = ["camera_id", "camera_mode"]
    )
    data class TableVo(
        // camera_id 와 camera_mode 가 고유값 역할을 맡음
        @ColumnInfo(name = "camera_id")
        val cameraId: String,

        // 1 : 사진
        // 2 : 동영상
        @ColumnInfo(name = "camera_mode")
        val cameraMode: Int,

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
    )

    // (테이블 Dao)
    @Dao
    interface TableDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(vararg inputTable: TableVo)

        @Update
        fun update(vararg inputTable: TableVo)

        @Query(
            "SELECT " +
                    "* " +
                    "FROM " +
                    "activity_basic_camera2_api_sample_camera_config " +
                    "where " +
                    "camera_id = :cameraId and " +
                    "camera_mode = :cameraMode"
        )
        fun getCameraModeConfig(cameraId: String, cameraMode: Int): TableVo?

//        @Query(
//            "SELECT " +
//                    "* " +
//                    "FROM " +
//                    "test_info"
//        )
//        fun selectColAll2(): List<TableVo>
//
//        @Delete
//        fun delete(vararg inputTable: TableVo)
    }
}