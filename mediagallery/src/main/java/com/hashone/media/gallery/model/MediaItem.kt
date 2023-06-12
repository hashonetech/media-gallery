package com.hashone.media.gallery.model

import com.hashone.media.gallery.enums.MediaType
import java.io.Serializable

data class MediaItem(
    var mediaId: Long = -1L,
    var bucketId: Long = -1L,
    var name: String = "",
    var path: String = "",
    var mediaSize: Long = 0L,
    var mediaDuration: Long = 0L,
    var mediaType: MediaType = MediaType.IMAGE_VIDEO,
) : Serializable