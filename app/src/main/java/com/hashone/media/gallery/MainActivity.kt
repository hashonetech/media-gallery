package com.hashone.media.gallery

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.hashone.commons.base.BaseActivity
import com.hashone.commons.base.BetterActivityResult
import com.hashone.commons.extensions.isPermissionGranted
import com.hashone.commons.extensions.serializable
import com.hashone.cropper.CropActivity
import com.hashone.cropper.model.CropDataSaved
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.enums.MediaType
import com.hashone.media.gallery.model.MediaItem
import com.hashone.media.gallery.test.databinding.ActivityMainBinding
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_PATH
import com.hashone.media.gallery.utils.KEY_IMAGE_PATH
import com.hashone.media.gallery.utils.KEY_MEDIA_PATHS
import com.hashone.media.gallery.utils.REQUEST_CODE_CAMERA
import com.hashone.media.gallery.utils.REQUEST_CODE_IMAGE
import com.hashone.media.gallery.utils.REQUEST_CODE_IMAGE_VIDEO
import com.hashone.media.gallery.utils.REQUEST_CODE_VIDEO
import com.hashone.media.gallery.utils.isVideoFile
import java.util.Locale


class MainActivity : BaseActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var mBinding: ActivityMainBinding
    var mediaTypeNames = arrayOf("IMAGE", "VIDEO", "IMAGE & VIDEO")
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

        mBinding.spMediaType.adapter = ArrayAdapter(
            mActivity,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("IMAGE", "VIDEO", "IMAGE & VIDEO")
        )

        mBinding.switchNewCrop.setOnToggledListener { buttonView, isChecked ->
            if (isChecked) {
                mBinding.switchForceClose.isOn = true
                mBinding.spMediaType.setSelection(0)
                mBinding.spCount.setSelection(0)
                mBinding.switchOldCrop.isOn = false

            }
            mBinding.spMediaType.selectedView.isEnabled =  !isChecked
            mBinding.spCount.selectedView.isEnabled =  !isChecked
            mBinding.spMediaType.isEnabled =  !isChecked
            mBinding.spCount.isEnabled =  !isChecked

            mBinding.lyNavigateToOther.isVisible = !isChecked
            mBinding.textOldCrop.isVisible = !isChecked
            mBinding.switchOldCrop.isVisible = !isChecked
        }

        mBinding.switchOldCrop.setOnToggledListener { buttonView, isChecked ->
            if (isChecked) {
                mBinding.switchForceClose.isOn = true
                mBinding.spMediaType.setSelection(0)
                mBinding.spCount.setSelection(0)
                mBinding.spCount.setSelection(0)
                mBinding.switchNewCrop.isOn = false

            }

            mBinding.spMediaType.selectedView.isEnabled =  !isChecked
            mBinding.spCount.selectedView.isEnabled =  !isChecked
            mBinding.spMediaType.isEnabled =  !isChecked
            mBinding.spCount.isEnabled =  !isChecked

            mBinding.textNewCrop.isVisible = !isChecked
            mBinding.switchNewCrop.isVisible = !isChecked
            mBinding.switchNavigateToOther.isVisible = !isChecked
            mBinding.textNavigateToOther.isVisible = !isChecked
        }

        mBinding.switchNavigateToOther.setOnToggledListener { buttonView, isChecked ->
            if (isChecked) {
                mBinding.switchOldCrop.isOn = false
                mBinding.switchNewCrop.isOn = false
            }
            mBinding.switchForceClose.isOn = !isChecked
            mBinding.lyCrop.isVisible = !isChecked
        }

        mBinding.btnOpen.setOnClickListener {
            openMediaGallery(when (mBinding.spMediaType.selectedItemPosition) {
                0 -> REQUEST_CODE_IMAGE
                1 -> REQUEST_CODE_VIDEO
                2 -> REQUEST_CODE_IMAGE_VIDEO
                else -> REQUEST_CODE_IMAGE
            })
        }
      /*  mBinding.image.setOnClickListener {
            openMediaGallery(REQUEST_CODE_IMAGE)
        }

        mBinding.video.setOnClickListener {
            openMediaGallery(REQUEST_CODE_VIDEO)
        }

        mBinding.imageVideo.setOnClickListener {
            openMediaGallery(REQUEST_CODE_IMAGE_VIDEO)
        }*/
        Glide.with(this@MainActivity).load(com.hashone.media.gallery.test.R.drawable.hashone_device_wallpaper).into(mBinding.cropedImage)
    }

    //Performing action onItemSelected and onNothing selected
    override fun onItemSelected(arg0: AdapterView<*>?, arg1: View?, position: Int, id: Long) {
        Toast.makeText(applicationContext, mediaTypeNames.get(position), Toast.LENGTH_LONG).show()
    }

    override fun onNothingSelected(arg0: AdapterView<*>?) {
        // TODO Auto-generated method stub
    }

    private fun openMediaGallery(requestCode: Int) {
//        if (checkPermissions(requestCode)) {
        val mediaCount = mBinding.spCount.selectedItemPosition + 1

        mActivityLauncher.launch(
            MediaGallery.open(
                activity = mActivity, MediaGallery.build(
                    mediaType = when (requestCode) {
                        REQUEST_CODE_IMAGE -> MediaType.IMAGE
                        REQUEST_CODE_VIDEO -> MediaType.VIDEO
                        else -> MediaType.IMAGE_VIDEO
                    },
                    mediaCount = mediaCount,
                    allowCamera = true,
                    allowGooglePhotos = true,
                    allowAllMedia = true,
                    enableCropMode = mBinding.switchNewCrop.isOn,
                    mediaGridCount = 3,
                    //TODO video Duration Limit in second
                    videoValidationBuilder = MediaGallery.VideoValidationBuilder(
                        checkValidation = true,
                        //TODO video Duration Limit in second
                        checkDuration = true,
                        durationLimit = 30,
                        durationLimitMessage = getString(R.string.duration_error),
                        //TODO video Size Limit in MB
                        checkFileSize = true,
                        sizeLimit = 100,
                        sizeLimitMessage = getString(R.string.file_size_error),
                        //TODO video Resolution Size Limit px
                        checkResolution = true,
                        maxResolution = 1920,
                        maxResolutionMessage = getString(R.string.size_error),
                        //TODO video Validation Dialog UI
                        durationDialogBuilder = MediaGallery.VideoValidationDialogBuilder(
                            titleColor = com.hashone.commons.R.color.dark_gray,
                            titleFont = com.hashone.commons.R.font.roboto_regular,
                            titleSize = 14F,
                            positiveText = getString(R.string.okay),
                            positiveColor = com.hashone.commons.R.color.black,
                            positiveFont = com.hashone.commons.R.font.roboto_regular,
                            positiveSize = 16F,
                        ),
                         //TODO video Validation Dialog UI
                        sizeDialogBuilder = MediaGallery.VideoValidationDialogBuilder(
                            titleColor = com.hashone.commons.R.color.dark_gray,
                            titleFont = com.hashone.commons.R.font.roboto_regular,
                            titleSize = 14F,
                            positiveText = getString(R.string.okay),
                            positiveColor = com.hashone.commons.R.color.black,
                            positiveFont = com.hashone.commons.R.font.roboto_regular,
                            positiveSize = 16F,
                        ),
                         //TODO video Validation Dialog UI
                        resolutionDialogBuilder = MediaGallery.VideoValidationDialogBuilder(
                            titleColor = com.hashone.commons.R.color.dark_gray,
                            titleFont = com.hashone.commons.R.font.roboto_regular,
                            titleSize = 14F,
                            positiveText = getString(R.string.no),
                            positiveColor = com.hashone.commons.R.color.black,
                            positiveFont = com.hashone.commons.R.font.roboto_regular,
                            positiveSize = 16F,
                            negativeText = getString(R.string.convert),
                            negativeColor = com.hashone.commons.R.color.black,
                            negativeFont = com.hashone.commons.R.font.roboto_regular,
                            negativeSize = 16F,

                        )

                    ),
                    cameraActionTitle = getString(com.hashone.media.gallery.R.string.camera_action_title),
                ) {
                    //TODO: Screen
                    screenBuilder = MediaGallery.ScreenBuilder(
                        isFullScreen = false,
                        windowBackgroundColor = com.hashone.media.gallery.test.R.color.white,
                        statusBarColor = com.hashone.media.gallery.test.R.color.white,
                        navigationBarColor = com.hashone.media.gallery.test.R.color.white,
                        //TODO: Google Photos Icon
//                    googlePhotosIcon = R.drawable.ic_google_photos_media_gallery
                    )

                    //TODO: Toolbar
                    toolBarBuilder = MediaGallery.ToolBarBuilder(
                        toolBarColor = com.hashone.media.gallery.test.R.color.white,
                        backIconDescription = "",
                        title = "",
                        titleColor = com.hashone.media.gallery.test.R.color.black,
                        titleFont = com.hashone.media.gallery.test.R.font.roboto_medium,
                        titleSize = 16F,
                        //TODO: Camera Icon
//                    cameraIcon = R.drawable.ic_camera_media_gallery
                    )

                    //TODO: Warning Ui
                    warningUiBuilder = MediaGallery.WarningUiBuilder(
                        message = getString(com.hashone.commons.R.string.allow_permission),
                        messageColor = com.hashone.commons.R.color.black,
                        messageFont = com.hashone.commons.R.font.roboto_regular,
                        messageSize = 14F,
                        settingText = getString(R.string.setting_text),
                        settingColor = com.hashone.media.gallery.R.color.positive_blue,
                        settingFont = com.hashone.commons.R.font.roboto_bold,
                        settingSize = 16F,
                    )

                    //TODO: Permission
                    permissionBuilder = MediaGallery.PermissionBuilder(
                        message = getString(com.hashone.commons.R.string.allow_permission),
                        messageColor = com.hashone.commons.R.color.black,
                        messageFont = com.hashone.commons.R.font.roboto_regular,
                        messageSize = 14F,
                        positiveText = getString(R.string.label_grant),
                        positiveColor = com.hashone.commons.R.color.black,
                        positiveFont = com.hashone.commons.R.font.roboto_bold,
                        positiveSize = 16F,
                        positiveIsCap = true,
                        negativeText = getString(R.string.label_cancel),
                        negativeColor = com.hashone.commons.R.color.black,
                        negativeFont = com.hashone.commons.R.font.roboto_regular,
                        negativeSize = 16F,
                        negativeIsCap = true,
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


                    isForceClose = mBinding.switchForceClose.isOn

                    if (mBinding.switchOldCrop.isOn || mBinding.switchNavigateToOther.isOn) {
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
                                Toast.makeText(
                                    mActivity,
                                    "Croped: \n${myCropDataSaved!!.cropImg}",
                                    Toast.LENGTH_LONG
                                ).show()
                                Glide.with(this@MainActivity).load(myCropDataSaved!!.cropImg).centerCrop()
                                    .into(mBinding.cropedImage)
                            } else if (intent.hasExtra(KEY_IMAGE_PATH)) {
                                val filePath = intent.extras!!.getString(KEY_IMAGE_PATH)!!
                                if (intent.hasExtra(KEY_IMAGE_ORIGINAL_PATH)) {
                                    val originalImagePath =
                                        intent.extras!!.getString(KEY_IMAGE_ORIGINAL_PATH)!!
                                }
                                Toast.makeText(
                                    mActivity,
                                    "Selected: filePath:\n${filePath}",
                                    Toast.LENGTH_LONG
                                ).show()
                                mBinding.videoPlayIcon.isVisible = isVideoFile(filePath)
                                Glide.with(this@MainActivity).load(filePath).centerCrop()
                                    .into(mBinding.cropedImage)
                            } else if (intent.hasExtra(KEY_MEDIA_PATHS)) {
                                val selectedMedia: ArrayList<MediaItem>? =
                                    intent.serializable(KEY_MEDIA_PATHS)
                                selectedMedia?.let {
                                    Toast.makeText(
                                        mActivity,
                                        "Selected File List Size: ${selectedMedia.size} \n filePath:${selectedMedia[0].path}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    mBinding.videoPlayIcon.isVisible = isVideoFile(selectedMedia[0].path)

                                    Glide.with(this@MainActivity).load(selectedMedia[0].path).centerCrop()
                                        .into(mBinding.cropedImage)
                                }
                            } else {
                            }
                        }
                    }
                }
            })
//        }
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
                    mIsGranted = when (mCurrentRequestCode) {
                        REQUEST_CODE_IMAGE -> {
                            isGranted
                        }

                        REQUEST_CODE_VIDEO -> {
                            isGranted
                        }

                        else -> {
                            isGranted
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
            showCustomAlertDialog(message = getString(R.string.allow_permission),
                negativeButtonText = getString(R.string.label_cancel).uppercase(Locale.getDefault()),
                positionButtonText = getString(R.string.label_grant).uppercase(Locale.getDefault()),
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