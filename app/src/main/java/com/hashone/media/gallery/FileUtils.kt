package com.hashone.media.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import java.io.File
import java.io.FileWriter

private const val INTERNAL_DIR_TEMP = "temp"

@SuppressLint("SetWorldWritable")
fun getInternalFileDir(context: Context): File {
    val contextWrapper = ContextWrapper(context)
    val rootDir = contextWrapper.getDir(context.filesDir.name, Context.MODE_PRIVATE)
    val imageDir = File(rootDir.absolutePath, "")
    imageDir.setReadable(true)
    imageDir.setWritable(true, false)
    if (!imageDir.exists()) {
        imageDir.mkdirs()
        imageDir.mkdir()
        val gpxfile = File(imageDir, ".nomedia")
        val writer = FileWriter(gpxfile)
        writer.flush()
        writer.close()
    }
    return imageDir.canonicalFile
}

@SuppressLint("SetWorldWritable")
fun getInternalTempDir(context: Context): File {
    val imageDir = File(getInternalFileDir(context), INTERNAL_DIR_TEMP)
    imageDir.setReadable(true)
    imageDir.setWritable(true, false)
    if (!imageDir.exists()) {
        imageDir.mkdirs()
        imageDir.mkdir()
        val gpxfile = File(imageDir, ".nomedia")
        val writer = FileWriter(gpxfile)
        writer.flush()
        writer.close()
    }
    return imageDir.canonicalFile
}