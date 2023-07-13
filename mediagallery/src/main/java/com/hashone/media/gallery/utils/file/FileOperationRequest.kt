package com.hashone.media.gallery.utils.file


data class FileOperationRequest(
    val storageType: StorageType,
    var projectDir: String = "",
    val fileName: String,
    val fileExtension: FileExtension = FileExtension.PNG
)