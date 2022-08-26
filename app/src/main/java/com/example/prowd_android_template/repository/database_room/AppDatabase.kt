package com.example.prowd_android_template.repository.database_room

import android.content.Context
import androidx.room.*
import com.example.prowd_android_template.repository.database_room.tables.TestInfoTable
import java.util.concurrent.Semaphore

// 테이블 객체를 생성하고 제공하는 역할
// : 테이블 1개당 Table 객체를 하나 만들어서, VO 객체를 @Database 어노테이션에,
//     DAO 객체를 (Table DAO 객체) 공간에 추가하기
@Database(
    entities = [
        TestInfoTable.TestInfoTableVO::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // <멤버 변수 공간>
    // (Table DAO 객체)
    abstract fun deviceConfigTableDao(): TestInfoTable.TestInfoDao


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    // (싱글톤 설정)
    companion object {
        // <멤버 변수 공간>
        // 데이터베이스 이름
        private const val databaseNameMbr = "app"

        // 싱글톤 객체 생성
        private val singletonSemaphore = Semaphore(1)
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            singletonSemaphore.acquire()

            if (null == instance) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    databaseNameMbr
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }

            singletonSemaphore.release()
            return instance!!
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>

}