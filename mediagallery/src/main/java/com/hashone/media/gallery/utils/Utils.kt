package com.hashone.media.gallery.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection


fun getVideoWidthHeight(imageUri: String, mediaResolution: String): Pair<Int, Int> {
    try {
        if (mediaResolution.isEmpty()) {
            val retriever = MediaMetadataRetriever()

            retriever.setDataSource(imageUri)
            val width =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
            val height =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
            retriever.release()

            return  try {
                var bmp: Bitmap? = null
                var inputStream: FileInputStream? = null
                val retrieverImage = MediaMetadataRetriever()
                inputStream = FileInputStream(File(imageUri).absolutePath)
                retrieverImage.setDataSource(inputStream.fd)
                bmp = retrieverImage.frameAtTime
                retrieverImage.release()
                if (bmp!=null) {
                    val mWidth = bmp.width
                    val mHeight = bmp.height
                    Pair(mWidth, mHeight)
                } else  Pair(width, height)
            }catch (e:Exception){
                e.printStackTrace()
                Pair(width, height)
            }

          /*  retriever.setDataSource(imageUri)
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
            return result*/
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