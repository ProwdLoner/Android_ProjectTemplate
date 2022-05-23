package com.example.prowd_android_template.util_object

import android.content.Context
import android.database.Cursor
import android.net.Uri
import java.io.File

object UriAndPath {
    // Uri 객체를 Path 로 변환
    fun getPathFromUri(context: Context, uri: Uri): String {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.moveToNext()
        val colIdx = cursor!!.getColumnIndex("_data")
        val path = cursor.getString(colIdx)
        cursor.close()
        return path
    }

    // Path 를 Uri 객체로 변환
    fun getUriFromPath(fileAbsolutePath: String): Uri {
        return Uri.parse(fileAbsolutePath)
    }
}