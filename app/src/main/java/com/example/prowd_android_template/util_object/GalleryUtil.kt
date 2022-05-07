package com.example.prowd_android_template.util_object

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// 갤러리와 같은 공유 공간에서 미디어 리스트를 가져오는 유틸
object GalleryUtil {
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

    // 이미지 파일을 앨범에 저장
    fun addImageFileToGallery(context: Context, imageFile: File, folderName: String) {
        // 카메라에서 가져온 파일
        var fileBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

        // 파일 이미지를 정방향으로 회전
        val orientation: Int = ExifInterface(imageFile.path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        fileBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> TransformationUtils.rotateImage(
                fileBitmap,
                90
            )
            ExifInterface.ORIENTATION_ROTATE_180 -> TransformationUtils.rotateImage(
                fileBitmap,
                180
            )
            ExifInterface.ORIENTATION_ROTATE_270 -> TransformationUtils.rotateImage(
                fileBitmap,
                270
            )
            ExifInterface.ORIENTATION_NORMAL -> fileBitmap
            else -> fileBitmap
        }

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = sdf.format(System.currentTimeMillis()) + ".png"

        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)

        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
            values.put(MediaStore.Images.Media.IS_PENDING, true)

            val uri: Uri? =
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val fos = context.contentResolver.openOutputStream(uri)

                if (null != fos) {
                    try {
                        fileBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val directory =
                File(
                    Environment.getExternalStorageDirectory()
                        .toString() + File.separator + folderName
                )

            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            val fos = FileOutputStream(file)

            try {
                fileBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
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