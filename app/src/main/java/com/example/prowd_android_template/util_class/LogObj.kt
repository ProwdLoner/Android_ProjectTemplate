package com.example.prowd_android_template.util_class

import android.content.Context
import android.util.Log
import com.example.prowd_android_template.BuildConfig
import com.example.prowd_android_template.util_object.CustomUtil

// [잘림 방지용 커스텀 로그 객체]
class LogObj constructor(val logName: String) {
    // (일반 로그 함수)
    fun i(message: String) {
        if (message.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
            Log.i(logName, message.substring(0, 3000))
            i(message.substring(3000))
        } else {
            Log.i(logName, message)
        }
    }

    fun v(message: String) {
        if (message.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
            Log.v(logName, message.substring(0, 3000))
            v(message.substring(3000))
        } else {
            Log.v(logName, message)
        }
    }

    fun w(message: String) {
        if (message.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
            Log.w(logName, message.substring(0, 3000))
            w(message.substring(3000))
        } else {
            Log.w(logName, message)
        }
    }

    fun e(message: String) {
        if (message.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
            Log.e(logName, message.substring(0, 3000))
            e(message.substring(3000))
        } else {
            Log.e(logName, message)
        }
    }


    // (디버그 모드에만 사용되는 로그)
    fun di(context: Context, message: String) {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (message.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
            Log.i(logName, message.substring(0, 3000))
            di(context, message.substring(3000))
        } else {
            Log.i(logName, message)
        }
    }

    fun dv(context: Context, message: String) {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (message.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
            Log.v(logName, message.substring(0, 3000))
            dv(context, message.substring(3000))
        } else {
            Log.v(logName, message)
        }
    }

    fun dw(context: Context, message: String) {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (message.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
            Log.w(logName, message.substring(0, 3000))
            dw(context, message.substring(3000))
        } else {
            Log.w(logName, message)
        }
    }

    fun de(context: Context, message: String) {
        if (!BuildConfig.DEBUG) {
            return
        }
        if (message.length > 3000) {    // 텍스트가 3000자 이상이 넘어가면 줄
            Log.e(logName, message.substring(0, 3000))
            de(context, message.substring(3000))
        } else {
            Log.e(logName, message)
        }
    }
}