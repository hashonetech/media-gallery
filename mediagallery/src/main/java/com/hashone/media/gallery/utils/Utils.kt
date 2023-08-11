package com.hashone.media.gallery.utils

import android.media.MediaMetadataRetriever


fun getVideoWidthHeight(imageUri: String, mediaResolution: String): Pair<Int, Int> {
    if (mediaResolution.isEmpty()) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(imageUri)
        val width = Integer.valueOf(
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )
        )
        val height = Integer.valueOf(
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )
        )
        retriever.release()

        return Pair(width, height)
    } else {
        val width = mediaResolution.split("×").first()
        val height = mediaResolution.split("×").last()
        return Pair(width.toInt(), height.toInt())
    }
}

fun byteToMB(fileSizeInBytes: Long): Long {
    val fileSizeInKB = fileSizeInBytes / 1024
    return fileSizeInKB / 1024
}