package com.hashone.media.gallery.builder

import android.app.Activity
import android.content.Intent
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.FontRes
import androidx.annotation.IntRange
import com.hashone.commons.R
import com.hashone.commons.extensions.getLocaleString
import com.hashone.media.gallery.MediaActivity
import com.hashone.media.gallery.enums.MediaType
import java.io.Serializable

open class MediaGallery(val builder: Builder) : Serializable {

    companion object {
        inline fun build(
            mediaType: MediaType,
            mediaCount: Int = 1,
            allowCamera: Boolean,
            allowGooglePhotos: Boolean,
            allowAllMedia: Boolean,
            enableCropMode: Boolean = false,
            mediaGridCount: Int = 3,
            videoValidationBuilder: VideoValidationBuilder = VideoValidationBuilder(),
            cameraActionTitle: String = getLocaleString(com.hashone.media.gallery.R.string.camera_action_title),
            block: Builder.() -> Unit
        ) = Builder(
            mediaType,
            mediaCount = if (!enableCropMode) mediaCount else 1,
            allowMultiMedia = if (!enableCropMode) mediaCount > 1 else false,
            allowCamera,
            allowGooglePhotos,
            allowAllMedia,
            enableCropMode,
            mediaGridCount,
            cameraActionTitle,
            videoValidationBuilder
        ).apply(block).build()

        fun open(activity: Activity, mediaGallery: MediaGallery): Intent =
            MediaActivity.newIntent(context = activity, mediaGallery = mediaGallery)
    }

    class Builder(
        val mediaType: MediaType = MediaType.IMAGE,
        @IntRange
        val mediaCount: Int = 1,
        val allowMultiMedia: Boolean = false,
        val allowCamera: Boolean = false,
        val allowGooglePhotos: Boolean = true,
        val allowAllMedia: Boolean = false,
        val enableCropMode: Boolean = false,
        @IntRange
        val mediaGridCount: Int = 3,
        val cameraActionTitle: String = getLocaleString(com.hashone.media.gallery.R.string.camera_action_title),

        //TODO: Video Validation Builder
        val videoValidationBuilder: VideoValidationBuilder = VideoValidationBuilder(),

        ) : Serializable {
        //TODO: Screen
        var screenBuilder = ScreenBuilder()

        //TODO: Toolbar
        var toolBarBuilder = ToolBarBuilder()

        //TODO: Warning Ui
        var warningUiBuilder = WarningUiBuilder()

        //TODO: Permission
        var permissionBuilder = PermissionBuilder()

        //TODO: Bucket Contents
        var bucketBuilder = BucketBuilder()

        //TODO: Action button
        var actionButtonBuilder = ActionButtonBuilder()

        //TODO: crop module
        var mediaCropBuilder = MediaCropBuilder()

        //TODO: gallery module close or not after image select
        var isForceClose: Boolean = true

        fun build() = MediaGallery(this)
    }

    class MediaCropBuilder(
        var appPackageName: String = "",
        var cropClassName: String = "",
        var projectDirectoryPath: String = ""
    ) : Serializable

    class BucketBuilder(
        @ColorRes
        var backgroundColor: Int = R.color.white,
        @ColorRes
        var titleColor: Int = R.color.pure_black,
        @FontRes
        var titleFont: Int = R.font.roboto_medium,
        @FloatRange
        var titleSize: Float = 16F,
        @ColorRes
        var subTitleColor: Int = R.color.pure_black,
        @FontRes
        var subTitleFont: Int = R.font.roboto_regular,
        @FloatRange
        var subTitleSize: Float = 14F,
        @ColorRes
        var countBackgroundColor: Int = R.color.pure_black,
        @ColorRes
        var countColor: Int = R.color.pure_black,
        @FontRes
        var countFont: Int = R.font.roboto_regular,
        @FloatRange
        var countSize: Float = 14F,
        //TODO: Media Content
        @DrawableRes
        var countBackgroundRes: Int = -1,
    ) : Serializable

    class ActionButtonBuilder(
        @ColorRes
        var backgroundColor: Int = R.color.black,
        var backgroundSelectorColor: Int = -1,
        @FloatRange
        var radius: Float = 30F,
        var text: String = "",
        @ColorRes
        var textColor: Int = R.color.white,
        @FontRes
        var textFont: Int = R.font.outfit_bold,
        @FloatRange
        var textSize: Float = 16F,
    ) : Serializable

    class ToolBarBuilder(
        @ColorRes
        var toolBarColor: Int = R.color.white,
        @DrawableRes
        var backIcon: Int = com.hashone.media.gallery.R.drawable.ic_back_media_gallery,
        var backIconDescription: String = "",
        var title: String = "",
        @ColorRes
        var titleColor: Int = R.color.black,
        @FontRes
        var titleFont: Int = R.font.roboto_medium,
        @FloatRange
        var titleSize: Float = 16F,
        //TODO: Cameras
        @DrawableRes
        var cameraIcon: Int = com.hashone.media.gallery.R.drawable.ic_camera_media_gallery
    ) : Serializable

    class ScreenBuilder(
        var isFullScreen: Boolean = false,
        @ColorRes
        var windowBackgroundColor: Int = R.color.white,
        @ColorRes
        var statusBarColor: Int = R.color.white,
        @ColorRes
        var navigationBarColor: Int = R.color.white,
        //TODO: Google Photos
        @DrawableRes
        var googlePhotosIcon: Int =
            com.hashone.media.gallery.R.drawable.ic_google_photos_media_gallery
    ) : Serializable


    class WarningUiBuilder(
        var message: String = getLocaleString(R.string.allow_permission),
        @ColorRes
        var messageColor: Int = R.color.black,
        @FontRes
        var messageFont: Int = R.font.roboto_regular,
        @FloatRange
        var messageSize: Float = 14F,
        var settingText: String = getLocaleString(com.hashone.media.gallery.R.string.setting_text),
        @ColorRes
        var settingColor: Int = com.hashone.media.gallery.R.color.positive_blue,
        @FontRes
        var settingFont: Int = R.font.roboto_bold,
        @FloatRange
        var settingSize: Float = 16F,
    ) : Serializable


    class PermissionBuilder(
        var message: String = getLocaleString(R.string.allow_permission),
        @ColorRes
        var messageColor: Int = R.color.black,
        @FontRes
        var messageFont: Int = R.font.roboto_medium,
        @FloatRange
        var messageSize: Float = 16F,
        var positiveText: String = getLocaleString(com.hashone.media.gallery.R.string.label_grant),
        @ColorRes
        var positiveColor: Int = R.color.black,
        @FontRes
        var positiveFont: Int = R.font.roboto_medium,
        @FloatRange
        var positiveSize: Float = 16F,
        var negativeText: String = getLocaleString(com.hashone.media.gallery.R.string.label_cancel),
        @ColorRes
        var negativeColor: Int = R.color.black,
        @FontRes
        var negativeFont: Int = R.font.roboto_medium,
        @FloatRange
        var negativeSize: Float = 16F,

        ) : Serializable

    class VideoValidationBuilder(
        var checkValidation: Boolean = false,
        // TODO video Duration Limit in second
        @IntRange
        var durationLimit: Int = 30,
        var durationLimitMessage: String = getLocaleString(com.hashone.media.gallery.R.string.duration_error),
        // TODO video Size Limit in MB
        @IntRange
        var sizeLimit: Int = 100,
        var sizeLimitMessage: String = getLocaleString(com.hashone.media.gallery.R.string.file_size_error),
        // TODO video Resolution Size Limit px
        @IntRange
        var maxResolution: Int = 1920,
        var maxResolutionMessage: String = getLocaleString(com.hashone.media.gallery.R.string.size_error),

        val videoValidationDialogBuilder: VideoValidationDialogBuilder = VideoValidationDialogBuilder(),
        ) : Serializable

    class VideoValidationDialogBuilder(
        @ColorRes
        var titleColor: Int = R.color.dark_gray,
        @FontRes
        var titleFont: Int = R.font.roboto_regular,
        @FloatRange
        var titleSize: Float = 14F,
        var positiveText: String = getLocaleString(com.hashone.media.gallery.R.string.okay),
        @ColorRes
        var positiveColor: Int = R.color.black,
        @FontRes
        var positiveFont: Int = R.font.roboto_regular,
        @FloatRange
        var positiveSize: Float = 16F,
    ) : Serializable
}