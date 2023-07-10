package com.hashone.media.gallery.builder

import android.app.Activity
import android.content.Intent
import com.hashone.commons.R
import com.hashone.media.gallery.MediaActivity
import com.hashone.media.gallery.enums.MediaType
import java.io.Serializable

open class MediaGallery constructor(val builder: Builder) : Serializable {

    companion object {
        inline fun build(
            mediaType: MediaType,
            mediaCount: Int = 1,
            allowCamera: Boolean,
            allowGooglePhotos: Boolean,
            allowAllMedia: Boolean,
//            enableCropMode: Boolean = false,
            mediaGridCount: Int = 3,
            block: Builder.() -> Unit
        ) = Builder(
            mediaType,
            mediaCount,
            allowMultiMedia = mediaCount > 1,
            allowCamera,
            allowGooglePhotos,
            allowAllMedia,
//            enableCropMode,
            mediaGridCount
        ).apply(block).build()

        fun open(activity: Activity, mediaGallery: MediaGallery): Intent =
            MediaActivity.newIntent(context = activity, mediaGallery = mediaGallery)
    }

    class Builder(
        val mediaType: MediaType = MediaType.IMAGE,
        val mediaCount: Int = 1,
        val allowMultiMedia: Boolean = false,
        val allowCamera: Boolean = false,
        val allowGooglePhotos: Boolean = true,
        val allowAllMedia: Boolean = false,
//        val enableCropMode: Boolean = false,
        val mediaGridCount: Int = 3
    ) : Serializable {
        //TODO: Screen
        var isFullScreen: Boolean = false
        var windowBackgroundColor: Int = R.color.white
        var statusBarColor: Int = R.color.white
        var navigationBarColor: Int = R.color.white

        //TODO: Toolbar
        var toolBarColor: Int = R.color.white
        var backPressIcon: Int = com.hashone.media.gallery.R.drawable.ic_back_media_gallery
        var backPressIconDescription: String = ""
        var toolBarTitle: String = ""
        var toolBarTitleColor: Int = R.color.black
        var toolBarTitleFont: Int = R.font.roboto_medium
        var toolBarTitleSize: Float = 16F

        //TODO: Cameras
        var cameraIcon: Int = com.hashone.media.gallery.R.drawable.ic_camera_media_gallery

        //TODO: Google Photos
        var googlePhotosIcon: Int =
            com.hashone.media.gallery.R.drawable.ic_google_photos_media_gallery

        //TODO: Bucket Contents
        var bucketTitleColor: Int = R.color.pure_black
        var bucketTitleFont: Int = R.font.roboto_medium
        var bucketTitleSize: Float = 16F
        var bucketSubTitleColor: Int = R.color.pure_black
        var bucketSubTitleFont: Int = R.font.roboto_regular
        var bucketSubTitleSize: Float = 14F
        var selectedCountBackgroundColor: Int = R.color.pure_black
        var selectedCountColor: Int = R.color.pure_black
        var selectedCountFont: Int = R.font.roboto_regular
        var selectedCountSize: Float = 14F

        //TODO: Action button
        var buttonBackgroundColor: Int = R.color.black
        var buttonRadius: Float = 30F
        var buttonText: String = ""
        var buttonTextColor: Int = R.color.white
        var buttonTextFont: Int = R.font.outfit_bold
        var buttonTextSize: Float = 16F

        fun build() = MediaGallery(this)
    }
}