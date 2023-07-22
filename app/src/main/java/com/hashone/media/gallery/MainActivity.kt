package com.hashone.media.gallery

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.hashone.commons.base.BaseActivity
import com.hashone.commons.base.BetterActivityResult
import com.hashone.commons.extensions.getLocaleString
import com.hashone.commons.extensions.isPermissionGranted
import com.hashone.commons.extensions.serializable
import com.hashone.cropper.CropActivity
import com.hashone.cropper.model.CropDataSaved
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.databinding.ActivityMainBinding
import com.hashone.media.gallery.enums.MediaType
import com.hashone.media.gallery.model.MediaItem
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_PATH
import com.hashone.media.gallery.utils.KEY_IMAGE_PATH
import com.hashone.media.gallery.utils.KEY_MEDIA_PATHS
import java.util.Locale

class MainActivity : BaseActivity() {

    private lateinit var mBinding: ActivityMainBinding

    private val REQUEST_CODE_IMAGE = 101
    private val REQUEST_CODE_VIDEO = 102
    private val REQUEST_CODE_IMAGE_VIDEO = 103
    private val REQUEST_CODE_CAMERA = 104

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.spCount.adapter = ArrayAdapter(mActivity,
            android.R.layout.simple_spinner_dropdown_item,
            ArrayList<Int>().apply {
                for (i in 1 until 51) {
                    add(i)
                }
            })

        mBinding.spFont.adapter = ArrayAdapter(
            mActivity,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Default", "Nunito Bold", "Roboto Medium", "Tinos Bold")
        )

        mBinding.image.setOnClickListener {
            openMediaGallery(REQUEST_CODE_IMAGE)
        }

        mBinding.video.setOnClickListener {
            openMediaGallery(REQUEST_CODE_VIDEO)
        }

        mBinding.imageVideo.setOnClickListener {
            openMediaGallery(REQUEST_CODE_IMAGE_VIDEO)
        }
    }

    private fun openMediaGallery(requestCode: Int) {
        if (checkPermissions(requestCode)) {
            val mediaCount = mBinding.spCount.selectedItemPosition + 1

            mActivityLauncher.launch(MediaGallery.open(activity = mActivity, MediaGallery.build(
                mediaType = when (requestCode) {
                    REQUEST_CODE_IMAGE -> MediaType.IMAGE
                    REQUEST_CODE_VIDEO -> MediaType.VIDEO
                    else -> MediaType.IMAGE_VIDEO
                },
                mediaCount = mediaCount,
                allowCamera = true,
                allowGooglePhotos = true,
                allowAllMedia = true,
                enableCropMode = mBinding.switchIsCrop.isChecked,
                mediaGridCount = 3
            ) {
                //TODO: Screen
                screenBuilder = MediaGallery.ScreenBuilder(
                    isFullScreen = true,
                    windowBackgroundColor = R.color.white,
                    statusBarColor = R.color.white,
                    navigationBarColor = R.color.white,
                    //TODO: Google Photos Icon
//                    googlePhotosIcon = R.drawable.ic_google_photos_media_gallery
                )

                //TODO: Toolbar
                toolBarBuilder = MediaGallery.ToolBarBuilder(
                    toolBarColor = R.color.white,
                    backIconDescription = "",
                    title = "",
                    titleColor = R.color.black,
                    titleFont = R.font.roboto_medium,
                    titleSize = 16F,
                    //TODO: Camera Icon
//                    cameraIcon = R.drawable.ic_camera_media_gallery
                )


                //TODO: Bucket Contents
                bucketBuilder = MediaGallery.BucketBuilder(
                    backgroundColor = com.hashone.commons.R.color.white,
                    titleColor = com.hashone.commons.R.color.pure_black,
                    titleFont = com.hashone.commons.R.font.roboto_medium,
                    titleSize = 16F,
                    subTitleColor = com.hashone.commons.R.color.pure_black,
                    subTitleFont = com.hashone.commons.R.font.roboto_regular,
                    subTitleSize = 14F,
                    countBackgroundColor = com.hashone.commons.R.color.pure_black,
                    countColor = com.hashone.commons.R.color.white,
                    countFont = com.hashone.commons.R.font.roboto_regular,
                    countSize = 14F,
                    //TODO: Media Content
                    countBackgroundRes = R.drawable.ic_photo_count
                )

                //TODO: Action button
                actionButtonBuilder = MediaGallery.ActionButtonBuilder(
                    backgroundColor = com.hashone.commons.R.color.black,
                    backgroundSelectorColor = com.hashone.commons.R.color.dark_gray,
                    radius = 16F,
                    text = "",
                    textColor = com.hashone.commons.R.color.white,
                    textFont = com.hashone.commons.R.font.roboto_bold,
                    textSize = 14F,
                )


                isForceClose = mBinding.switchIsForceClose.isChecked

                if (mBinding.switchIsOldCrop.isChecked || !mBinding.switchIsForceClose.isChecked) {
                    mediaCropBuilder = MediaGallery.MediaCropBuilder(
                        appPackageName = packageName,
                        cropClassName = "OldCropActivity",
                        projectDirectoryPath = getInternalFileDir(this@MainActivity).absolutePath
                    )
                }
            }),
                onActivityResult = object : BetterActivityResult.OnActivityResult<ActivityResult> {
                    override fun onActivityResult(result: ActivityResult) {
                        if (result.resultCode == Activity.RESULT_OK) {
                            result.data?.let { intent ->
                                if (intent.hasExtra(CropActivity.KEY_RETURN_CROP_DATA)) {
                                    val myCropDataSaved =
                                        intent.extras?.serializable<CropDataSaved>(
                                            CropActivity.KEY_RETURN_CROP_DATA
                                        )
                                    Glide.with(this@MainActivity).load(myCropDataSaved!!.cropImg)
                                        .into(mBinding.cropedImage)
                                } else if (intent.hasExtra(KEY_IMAGE_PATH)) {
                                    val filePath = intent.extras!!.getString(KEY_IMAGE_PATH)!!
                                    if (intent.hasExtra(KEY_IMAGE_ORIGINAL_PATH)) {
                                        val originalImagePath =
                                            intent.extras!!.getString(KEY_IMAGE_ORIGINAL_PATH)!!
                                    }
                                    Glide.with(this@MainActivity).load(filePath)
                                        .into(mBinding.cropedImage)
                                } else if (intent.hasExtra(KEY_MEDIA_PATHS)) {
                                    val selectedMedia: ArrayList<MediaItem>? =
                                        intent.serializable(KEY_MEDIA_PATHS)
                                    selectedMedia?.let {
                                        Toast.makeText(
                                            mActivity,
                                            "Selected: ${selectedMedia.size}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Glide.with(this@MainActivity).load(selectedMedia[0].path)
                                            .into(mBinding.cropedImage)
                                    }
                                } else {
                                }
                            }
                        }
                    }
                })
        }
    }

    private var mCurrentRequestCode = -1
    private fun checkPermissions(requestCode: Int): Boolean {
        mCurrentRequestCode = requestCode
        val permissions = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (mCurrentRequestCode == REQUEST_CODE_IMAGE) {
                if (!isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES)) permissions.add(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            } else if (mCurrentRequestCode == REQUEST_CODE_VIDEO) {
                if (!isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO)) permissions.add(
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else if (mCurrentRequestCode == REQUEST_CODE_CAMERA) {
                if (!isPermissionGranted(Manifest.permission.CAMERA)) permissions.add(Manifest.permission.CAMERA)
            } else {
                if (!isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES)) permissions.add(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
                if (!isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO)) permissions.add(
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
        } else {
            if (mCurrentRequestCode == REQUEST_CODE_CAMERA) {
                if (!isPermissionGranted(Manifest.permission.CAMERA)) permissions.add(Manifest.permission.CAMERA)
            } else if (!isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) permissions.add(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            return true
        }
        return false
    }

    private var requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (!it.isNullOrEmpty()) {
                var mIsGranted: Boolean = false
                it.forEach { (permission, isGranted) ->
                    when (mCurrentRequestCode) {
                        REQUEST_CODE_IMAGE -> {
                            mIsGranted = isGranted
                        }

                        REQUEST_CODE_VIDEO -> {
                            mIsGranted = isGranted
                        }

                        else -> {
                            mIsGranted = isGranted
                        }
                    }
                }
                checkPermissionStatus(mIsGranted)
            }
        }

    private fun checkPermissionStatus(isGranted: Boolean) {
        if (isGranted) {
            openMediaGallery(mCurrentRequestCode)
        } else {
            showCustomAlertDialog(message = getLocaleString(R.string.allow_permission),
                negativeButtonText = getLocaleString(R.string.label_cancel).uppercase(Locale.getDefault()),
                positionButtonText = getLocaleString(R.string.label_grant).uppercase(Locale.getDefault()),
                negativeCallback = {
                    alertDialog?.cancel()
                },
                positiveCallback = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    alertDialog?.cancel()
                },
                onDismissListener = {},
                onCancelListener = {})
        }
    }
}