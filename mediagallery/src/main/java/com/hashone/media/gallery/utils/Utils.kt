package com.hashone.media.gallery.utils

import android.media.MediaMetadataRetriever
import android.util.Log
import java.net.URLConnection


fun getVideoWidthHeight(imageUri: String, mediaResolution: String): Pair<Int, Int> {
    try {
        if (mediaResolution.isEmpty()) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(imageUri)
            val result = Pair(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.let {
                it.toInt()
            } ?: 0, retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.let {
                it.toInt()
            } ?: 0)
            retriever.release()
            return result
        } else {
            val width = mediaResolution.split("×").first()
            val height = mediaResolution.split("×").last()
            return Pair(width.toInt(), height.toInt())
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return Pair(0, 0)
    }
}

fun isVideoFile(path: String?): Boolean {
    val mimeType = URLConnection.guessContentTypeFromName(path)
    return mimeType != null && mimeType.startsWith("video")
}

fun byteToMB(fileSizeInBytes: Long): Long {
    val fileSizeInKB = fileSizeInBytes / 1024
    return fileSizeInKB / 1024
}