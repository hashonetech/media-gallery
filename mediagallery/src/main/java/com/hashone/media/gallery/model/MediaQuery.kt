package com.hashone.media.gallery.model

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor.FIELD_TYPE_NULL
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.hashone.media.gallery.MediaActivity
import com.hashone.media.gallery.enums.MediaType

fun fetchMediaBucketsAsync(
    context: Context,
    mediaType: MediaType,
    allowAllMedia: Boolean
): ArrayList<MediaBucketData> {
    val bucketIdList = ArrayList<Long>()
    val bucketList = ArrayList<MediaBucketData>()
    context.contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        arrayOf(
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
//            MediaStore.Files.FileColumns.RESOLUTION
        ),
        Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                when (mediaType) {
                    MediaType.IMAGE, MediaType.VIDEO -> "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
                    else -> "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
                } + " AND ${MediaStore.Files.FileColumns.SIZE} > 0"
            )

            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                when (mediaType) {
                    MediaType.IMAGE -> {
                        arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
                    }

                    MediaType.VIDEO -> {
                        arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
                    }

                    else -> {
                        arrayOf(
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                        )
                    }
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                putString(
                    ContentResolver.QUERY_ARG_SQL_GROUP_BY,
                    MediaStore.Files.FileColumns.BUCKET_ID
                )
            }
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )
        },
        null
    ).use {
        it?.let {
            if (it.moveToFirst()) {
                val bucketIdIndex = it.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
                val bucketDisplayNameIndex =
                    it.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val bucketMediaTypeIndex =
                    it.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                do {
                    val bucketId = it.getLong(bucketIdIndex)
                    val bucketDisplayName = it.getString(bucketDisplayNameIndex)
//                    val bucketMediaTypeName = it.getInt(bucketMediaTypeIndex)


                    if (!bucketIdList.contains(bucketId)) {
                        bucketIdList.add(bucketId)
                        val mediaList = fetchMediaAsync(
                            context,
                            mediaType,//if (bucketMediaTypeName == 1) MediaType.IMAGE else MediaType.VIDEO, // image video checking
                            bucketId
                        )

                        (context as MediaActivity).mMediaPref.storeMediaToPref(bucketId, mediaList)
                        if (mediaList.size > 0)
                            bucketList.add(
                                MediaBucketData(
                                    bucketId,
                                    if (bucketDisplayName.isNullOrEmpty()) "0" else bucketDisplayName,
                                    mediaList[0].path,
                                    mediaList.size,
                                    mediaType = mediaType //if (bucketMediaTypeName == 1) MediaType.IMAGE else MediaType.VIDEO
                                )
                            )
                    }
                } while (it.moveToNext())
            }
        }
        it?.close()
    }
    if (allowAllMedia) {
        val mediaList = fetchMediaAsync(
            context,
            mediaType,
            -1L
        )
        (context as MediaActivity).mMediaPref.storeMediaToPref(-1L, mediaList)
        if (mediaList.size > 0)
            bucketList.add(
                0, MediaBucketData(
                    -1L,
                    "",
                    mediaList[0].path,
                    mediaList.size,
                    mediaType = mediaType
                )
            )
    }
    return bucketList
}

fun fetchMediaAsync(
    context: Context,
    mediaType: MediaType,
    bucketId: Long
): ArrayList<MediaItem> {
    val mediaList = ArrayList<MediaItem>()
    val contentUri = MediaStore.Files.getContentUri("external")
    val cursor = context.contentResolver.query(
        contentUri,
        arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DURATION,
//            MediaStore.Files.FileColumns.RESOLUTION,
        ),
        Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                (if (bucketId != -1L) {
                    "${MediaStore.Files.FileColumns.BUCKET_ID}='$bucketId' AND "
                } else "") +
                        when (mediaType) {
                            MediaType.IMAGE -> {
                                "${MediaStore.Files.FileColumns.MEDIA_TYPE}='${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}'"
                            }

                            MediaType.VIDEO -> {
                                "${MediaStore.Files.FileColumns.MEDIA_TYPE}='${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}'"
                            }

                            else -> {
                                "(${MediaStore.Files.FileColumns.MEDIA_TYPE}='${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}' OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}='${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}')"
                            }
                        }
                        + " AND ${MediaStore.Files.FileColumns.SIZE}>0 "
            )
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.Files.FileColumns.DATE_ADDED} ASC"
            )
        },
        null
    )

    if (cursor != null) {
        if (cursor.moveToFirst()) {
            val mediaIdIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val mediaNameIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mediaDataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val mediaSizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val mediaDurationIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION)
            val mediaTypeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val mediaResolution = cursor.getColumnIndex(MediaStore.Files.FileColumns.RESOLUTION)
            do {
                if (cursor.getType(mediaIdIndex) != FIELD_TYPE_NULL && cursor.getType(mediaNameIndex) != FIELD_TYPE_NULL && cursor.getType(mediaDataIndex) != FIELD_TYPE_NULL && cursor.getType(mediaSizeIndex) != FIELD_TYPE_NULL) {
                    mediaList.add(
                        MediaItem(
                            mediaId = cursor.getLong(mediaIdIndex),
                            bucketId = bucketId,
                            name = cursor.getString(mediaNameIndex),
                            path = cursor.getString(mediaDataIndex),
                            mediaSize = cursor.getLong(mediaSizeIndex),
                            mediaDuration = cursor.getLong(mediaDurationIndex),
                            mediaType = if (cursor.getInt(mediaTypeIndex) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) MediaType.VIDEO else MediaType.IMAGE,
                            mediaResolution = if (mediaResolution != -1) cursor.getString(
                                mediaResolution
                            ) else ""
                        )
                    )
                }
            } while (cursor.moveToNext())
        }
    }
    cursor?.close()
    mediaList.reverse()
    return mediaList
}