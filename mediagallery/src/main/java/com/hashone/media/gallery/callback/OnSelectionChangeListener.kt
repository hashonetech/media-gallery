package com.hashone.media.gallery.callback

import com.hashone.media.gallery.model.MediaItem

interface OnSelectionChangeListener {
    fun onSelectedImagesChanged(selectedImages: ArrayList<MediaItem>)
    fun onSingleModeImageSelected(imageItem: MediaItem)
    fun onNotValidVideo(imageItem: MediaItem, position: Int)
}