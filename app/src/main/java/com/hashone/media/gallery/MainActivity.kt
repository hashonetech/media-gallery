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
import com.hashone.commons.base.BaseActivity
import com.hashone.commons.base.BetterActivityResult
import com.hashone.commons.extensions.getLocaleString
import com.hashone.commons.extensions.isPermissionGranted
import com.hashone.commons.extensions.serializable
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.databinding.ActivityMainBinding
import com.hashone.media.gallery.enums.MediaType
import com.hashone.media.gallery.model.MediaItem
import com.hashone.media.gallery.utils.KEY_MEDIA_PATH
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

        mBinding.spCount.adapter =
            ArrayAdapter(
                mActivity,
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

            mActivityLauncher.launch(
                MediaGallery.open(activity = mActivity, MediaGallery.build(
                    mediaType = when (requestCode) {
                        REQUEST_CODE_IMAGE -> MediaType.IMAGE
                        REQUEST_CODE_VIDEO -> MediaType.VIDEO
                        else -> MediaType.IMAGE_VIDEO
                    },
                    mediaCount = mediaCount,
                    allowCamera = true,
                    allowGooglePhotos = true,
                    allowAllMedia = true,
                    mediaGridCount = 3
                ) {
                    //TODO: Screen
                    isFullScreen = false
                    windowBackgroundColor = R.color.white
                    statusBarColor = R.color.white
                    navigationBarColor = R.color.white
                    //TODO: Toolbar
                    toolBarColor = R.color.white
//                    backPressIcon = R.drawable.ic_back_contact_us
                    backPressIconDescription = ""
                    toolBarTitle = ""
                    toolBarTitleColor = R.color.black
                    toolBarTitleFont = R.font.roboto_medium
                    toolBarTitleSize = 16F
                    //TODO: Camera Icon
//                    cameraIcon = R.drawable.ic_camera_media_gallery
                    //TODO: Google Photos Icon
//                    googlePhotosIcon = R.drawable.ic_google_photos_media_gallery
                    //TODO: Bucket Contents
                    bucketTitleColor = com.hashone.commons.R.color.pure_black
                    bucketTitleFont = com.hashone.commons.R.font.roboto_medium
                    bucketTitleSize = 16F
                    bucketSubTitleColor = com.hashone.commons.R.color.pure_black
                    bucketSubTitleFont = com.hashone.commons.R.font.roboto_regular
                    bucketSubTitleSize = 14F
                    selectedCountBackgroundColor = com.hashone.commons.R.color.pure_black
                    selectedCountColor = com.hashone.commons.R.color.white
                    selectedCountFont = com.hashone.commons.R.font.roboto_regular
                    selectedCountSize = 14F
                    //TODO: Action button
                    buttonBackgroundColor = com.hashone.commons.R.color.black
                    buttonRadius = 16F
                    buttonText = ""
                    buttonTextColor = com.hashone.commons.R.color.white
                    buttonTextFont = com.hashone.commons.R.font.roboto_bold
                    buttonTextSize = 14F
                }),
                onActivityResult = object : BetterActivityResult.OnActivityResult<ActivityResult> {
                    override fun onActivityResult(activityResult: ActivityResult) {
                        if (activityResult.resultCode == Activity.RESULT_OK) {
                            activityResult.data?.let { intent ->
                                if (intent.hasExtra(KEY_MEDIA_PATH)) {
                                    val selectedMedia: ArrayList<MediaItem>? =
                                        intent.serializable(KEY_MEDIA_PATH)
                                    selectedMedia?.let {
                                        Toast.makeText(
                                            mActivity,
                                            "Selected: ${it[0].path}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    val selectedMedia: ArrayList<MediaItem>? =
                                        intent.serializable(KEY_MEDIA_PATHS)
                                    selectedMedia?.let {
                                        Toast.makeText(
                                            mActivity,
                                            "Selected: ${selectedMedia.size}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    private var mCurrentRequestCode = -1
    private fun checkPermissions(requestCode: Int): Boolean {
        mCurrentRequestCode = requestCode
        val permissions = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (mCurrentRequestCode == REQUEST_CODE_IMAGE) {
                if (!isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES))
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else if (mCurrentRequestCode == REQUEST_CODE_VIDEO) {
                if (!isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO))
                    permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            } else if (mCurrentRequestCode == REQUEST_CODE_CAMERA) {
                if (!isPermissionGranted(Manifest.permission.CAMERA))
                    permissions.add(Manifest.permission.CAMERA)
            } else {
                if (!isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES))
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                if (!isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO))
                    permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (mCurrentRequestCode == REQUEST_CODE_CAMERA) {
                if (!isPermissionGranted(Manifest.permission.CAMERA))
                    permissions.add(Manifest.permission.CAMERA)
            } else if (!isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
                negativeButtonText = getLocaleString(R.string.label_cancel)
                    .uppercase(Locale.getDefault()),
                positionButtonText = getLocaleString(R.string.label_grant)
                    .uppercase(Locale.getDefault()),
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