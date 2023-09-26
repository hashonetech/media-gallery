package com.hashone.media.gallery.utils

import android.media.MediaMetadataRetriever
import java.net.URLConnection


fun getVideoWidthHeight(imageUri: String, mediaResolution: String): Pair<Int, Int> {
    try {
        if (mediaResolution.isEmpty()) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(imageUri)
            retriever.release()

            return Pair(retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.let {
                Integer.valueOf(
                    it
                )
            } ?: 0, retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.let {
                Integer.valueOf(
                    it
                )
            } ?: 0)
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