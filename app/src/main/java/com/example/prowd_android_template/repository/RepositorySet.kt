package com.example.prowd_android_template.repository

import android.content.Context
import com.example.prowd_android_template.repository.database_room.RepositoryDatabaseRoom
import com.example.prowd_android_template.repository.network_retrofit2.RepositoryNetworkRetrofit2
import java.util.concurrent.Semaphore

// 네트워크, DB 레포지토리 접근 객체 묶음
// 커스텀 할 것 없이 생성된 각 레포지토리 객체를 사용하면 됨
class RepositorySet private constructor(context: Context) {
    // <멤버 변수 공간>
    // (리포지토리 객체들)
    val networkRetrofit2Mbr: RepositoryNetworkRetrofit2 =
        RepositoryNetworkRetrofit2.getInstance()

    val databaseRoomMbr: RepositoryDatabaseRoom =
        RepositoryDatabaseRoom.getInstance(context)


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    // (싱글톤 설정)
    companion object {
        private val singletonSemaphore = Semaphore(1)
        private var instance: RepositorySet? = null

        fun getInstance(context: Context): RepositorySet {
            singletonSemaphore.acquire()

            if (null == instance) {
                instance = RepositorySet(context)
            }

            singletonSemaphore.release()

            return instance!!
        }
    }
}
