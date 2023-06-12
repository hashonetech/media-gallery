package com.hashone.media.gallery.model

import com.hashone.media.gallery.enums.MediaType
import java.io.Serializable

data class MediaBucketData(
    var bucketId: Long = -1L,
    var name: String = "",
    var path: String = "",
    var mediaCount: Int = 0,
    var selectedCount: Int = 0,
    var mediaType: MediaType = MediaType.IMAGE_VIDEO
) : Serializable