package com.hashone.media.gallery.utils

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.hashone.commons.extensions.getGson
import com.hashone.commons.utils.StoreUserData
import com.hashone.media.gallery.model.MediaItem

class MediaPref(context: Context) : StoreUserData(context) {

    private val keyMediaBuckets = "key_bucket_"
    fun storeMediaToPref(bucketId: Long, mediaList: ArrayList<MediaItem>) {
        setString("$keyMediaBuckets$bucketId", getGson().toJson(mediaList))
    }

    fun getMediaByBucketId(bucketId: Long): ArrayList<MediaItem> {
        val mediaItems = getString("$keyMediaBuckets$bucketId")
        if (!mediaItems.isNullOrEmpty()) {
            return getGson().fromJson(
                mediaItems,
                object : TypeToken<ArrayList<MediaItem>>() {}.type
            )
        }
        return arrayListOf()
    }

    fun clearMediaPref() {
        val allEntries: MutableMap<String, *>? = getAllKeys()
        if (allEntries != null) {
            for ((key, _) in allEntries.entries) {
                if (key.startsWith(keyMediaBuckets)) {
                    setString(key, "")
                }
            }
        }
    }
}