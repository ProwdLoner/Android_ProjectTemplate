package com.example.prowd_android_template.repository.database_room

import android.content.Context
import java.util.concurrent.Semaphore

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