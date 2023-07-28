package com.hashone.media.gallery.utils.file

import android.content.Context
import android.content.ContextWrapper
import java.io.File

object FileCreator {

    fun createFile(fileOperationRequest: FileOperationRequest, context: Context): File {
        return createInternalFile(fileOperationRequest, context)
    }

    private fun createInternalFile(
        fileOperationRequest: FileOperationRequest, context: Context
    ): File {
//        val contextWrapper = ContextWrapper(context)
//        val path = contextWrapper.getDir(context.filesDir.name, Context.MODE_PRIVATE)
        val parentFolder = File(fileOperationRequest.projectDir).also { it.mkdirs() }

        return File(
            parentFolder,
            "Crop${fileOperationRequest.fileName}${fileOperationRequest.fileExtension.fileExtensionName}"
        )
    }
}