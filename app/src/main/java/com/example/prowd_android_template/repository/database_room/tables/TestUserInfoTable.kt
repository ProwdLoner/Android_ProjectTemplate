package com.example.prowd_android_template.repository.database_room.tables

import androidx.room.*

// 유저 샘플 관련 테스트용 샘플 테이블
class TestUserInfoTable {
    // (테이블 구조)
    @Entity(tableName = "test_user")
    data class TableVo(
        @ColumnInfo(name = "user_type")
        val userType: Int, // 0 : 비회원, 1 : 이메일 회원, 2 : google, 3 : kakao, 4 : naver

        @ColumnInfo(name = "id")
        val id: String, // 이메일 로그인이면 이메일, SNS 로그인이면 SNS Id

        @ColumnInfo(name = "nick_name")
        val nickName: String,

        @ColumnInfo(name = "password")
        val password: String // SNS 로그인이면 토큰
    ) {
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "uid")
        var uid: Long = 0
    }

    // (테이블 Dao)
    @Dao
    interface TableDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(vararg inputTable: TableVo)

        @Query(
            "DELETE " +
                    "FROM " +
                    "test_user " +
                    "where " +
                    "uid = :uid"
        )
        fun delete(uid: Long)

        @Query(
            "SELECT " +
                    "COUNT(*) " +
                    "FROM " +
                    "test_user " +
                    "where " +
                    "id = :id " +
                    "and " +
                    "user_type = :userType"
        )
        fun getIdCount(id: String, userType: Int): Int

        @Query(
            "SELECT " +
                    "uid, nick_name " +
                    "FROM " +
                    "test_user " +
                    "where " +
                    "id = :id " +
                    "and " +
                    "password = :password " +
                    "and " +
                    "user_type = :userType"
        )
        fun getUserInfoForLogin(
            id: String,
            password: String,
            userType: Int
        ): List<GetUserInfoForLoginOutput>

        @Query(
            "SELECT " +
                    "uid, nick_name " +
                    "FROM " +
                    "test_user " +
                    "where " +
                    "id = :id " +
                    "and " +
                    "user_type = :userType"
        )
        fun getUserInfoForLogin(
            id: String,
            userType: Int
        ): List<GetUserInfoForLoginOutput>

        data class GetUserInfoForLoginOutput(
            @ColumnInfo(name = "uid")
            val uid: Long,
            @ColumnInfo(name = "nick_name")
            val nickName: String
        )

//        @Update
//        fun updateDeviceConfigInfo(vararg inputTable: TestInfoTableVO)
    }

}