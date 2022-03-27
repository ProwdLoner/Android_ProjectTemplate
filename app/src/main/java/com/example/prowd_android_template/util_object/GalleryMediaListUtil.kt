package com.example.prowd_android_template.util_object

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.util.*
import java.util.concurrent.TimeUnit

// 갤러리와 같은 공유 공간에서 미디어 리스트를 가져오는 유틸
object GalleryMediaListUtil {
    // <멤버 변수 공간>


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    fun getGalleryImages(context: Context): MutableList<ImageItem> {
        val imageList = mutableListOf<ImageItem>()

        // 가져올 정보 선택
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )

        // 정렬 방식 선정
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        // contentResolver를 사용하여 query로 이미지 정보 호출
        val query = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateTaken = Date(cursor.getLong(dateTakenColumn))
                val displayName = cursor.getString(displayNameColumn)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                imageList += ImageItem(id, displayName, dateTaken, contentUri)
            }
        }
        return imageList
    }

    fun getGalleryVideos(context: Context): MutableList<VideoItem> {
        val videoList = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        val selection = "${MediaStore.Video.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString())

        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
        val query = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val contentUri: Uri =
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                videoList += VideoItem(contentUri, name, duration, size)
            }
        }

        return videoList
    }

    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    data class ImageItem(
        val id: Long,
        val displayName: String,
        val dateTaken: Date,
        val contentUri: Uri
    )

    data class VideoItem(
        val uri: Uri,
        val name: String,
        val duration: Int,
        val size: Int
    )
}