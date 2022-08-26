package com.example.prowd_android_template.repository.database_room

import android.content.Context
import com.example.prowd_android_template.util_class.LogObj
import java.util.concurrent.Semaphore

// 데이터베이스 객체를 생성하고 제공하는 역할
// : 데이터베이스 1개당 Database 객체 1개를 생성하여 (Database 객체) 공간에 추가하여 사용
class RepositoryDatabaseRoom private constructor(context: Context) {
    // <멤버 변수 공간>
    // (Database 객체)
    val appDatabaseMbr = AppDatabase.getInstance(context)


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    // (싱글톤 설정)
    companion object {
        private val singletonSemaphore = Semaphore(1)
        private var instanceRepository: RepositoryDatabaseRoom? = null

        fun getInstance(context: Context): RepositoryDatabaseRoom {
            singletonSemaphore.acquire()

            if (null == instanceRepository) {
                instanceRepository = RepositoryDatabaseRoom(context)
            }

            singletonSemaphore.release()

            return instanceRepository!!
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
}