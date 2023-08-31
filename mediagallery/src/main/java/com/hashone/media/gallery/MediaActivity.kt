package com.hashone.media.gallery

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.hashone.commons.base.BaseActivity
import com.hashone.commons.base.BetterActivityResult
import com.hashone.commons.base.CoroutineAsyncTask
import com.hashone.commons.databinding.DialogConfirmationBinding
import com.hashone.commons.extensions.applyTextStyle
import com.hashone.commons.extensions.checkCameraHardware
import com.hashone.commons.extensions.getColorCode
import com.hashone.commons.extensions.getLocaleString
import com.hashone.commons.extensions.hideSystemUI
import com.hashone.commons.extensions.isGooglePhotosAppInstalled
import com.hashone.commons.extensions.isPermissionGranted
import com.hashone.commons.extensions.navigationUI
import com.hashone.commons.extensions.onClick
import com.hashone.commons.extensions.parcelable
import com.hashone.commons.extensions.registerBroadCastReceiver
import com.hashone.commons.extensions.saveToFile
import com.hashone.commons.extensions.serializable
import com.hashone.commons.extensions.setStatusBarColor
import com.hashone.commons.extensions.toFilePath
import com.hashone.commons.utils.EXTENSION_PNG
import com.hashone.commons.utils.PACKAGE_NAME_GOOGLE_PHOTOS
import com.hashone.commons.utils.dpToPx
import com.hashone.commons.utils.getInternalCameraDir
import com.hashone.commons.utils.showSnackBar
import com.hashone.cropper.CropActivity
import com.hashone.cropper.builder.Crop
import com.hashone.cropper.model.CropDataSaved
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.databinding.ActivityMediaBinding
import com.hashone.media.gallery.databinding.DialogWarningBinding
import com.hashone.media.gallery.enums.MediaType
import com.hashone.media.gallery.fragment.BucketsFragment
import com.hashone.media.gallery.model.MediaItem
import com.hashone.media.gallery.utils.ACTION_FINISH_GALLERY
import com.hashone.media.gallery.utils.KEY_CROP_DESTINATION_PATH
import com.hashone.media.gallery.utils.KEY_CROP_FILET
import com.hashone.media.gallery.utils.KEY_CROP_PROJECT_DIRECTORY
import com.hashone.media.gallery.utils.KEY_CROP_URI
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_PATH
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_REPLACE
import com.hashone.media.gallery.utils.KEY_IMAGE_PATH
import com.hashone.media.gallery.utils.KEY_MEDIA_GALLERY
import com.hashone.media.gallery.utils.KEY_MEDIA_PATHS
import com.hashone.media.gallery.utils.MediaPref
import com.hashone.media.gallery.utils.REQUEST_CODE_CAMERA
import com.hashone.media.gallery.utils.REQUEST_CODE_IMAGE
import com.hashone.media.gallery.utils.REQUEST_CODE_IMAGE_VIDEO
import com.hashone.media.gallery.utils.REQUEST_CODE_VIDEO
import com.hashone.media.gallery.utils.byteToMB
import com.hashone.media.gallery.utils.file.FileCreator
import com.hashone.media.gallery.utils.file.FileExtension
import com.hashone.media.gallery.utils.file.FileOperationRequest
import com.hashone.media.gallery.utils.file.StorageType
import com.hashone.media.gallery.utils.getApplicationInfoCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class MediaActivity : BaseActivity() {

    private var isFromSetting: Boolean = false
    val mSelectedImagesList = ArrayList<MediaItem>()
    private lateinit var mBinding: ActivityMediaBinding
    lateinit var mMediaPref: MediaPref

    lateinit var builder: MediaGallery.Builder

    private var mOptionMenu: Menu? = null

    companion object {
        fun newIntent(context: Context, mediaGallery: MediaGallery): Intent {
            return Intent(context, MediaActivity::class.java).apply {
                Bundle().apply { putSerializable(KEY_MEDIA_GALLERY, mediaGallery) }
                    .also { this.putExtras(it) }
            }
        }
    }

    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent != null) {
                    if (intent.action != null && intent.action == ACTION_FINISH_GALLERY) {
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        builder = (intent!!.extras!!.serializable<MediaGallery>(KEY_MEDIA_GALLERY)!!).builder

        setWindowUI()

        super.onCreate(savedInstanceState)

        mActivity = this
        mBinding = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mMediaPref = MediaPref(mActivity)
        mMediaPref.clearMediaPref()

        WarningScreenUi()
        setScreenUI()
        setToolbarUI()
        setCameraUI()
        setGooglePhotosUI()
        setActionButtonUI()
        if (checkPermissions(if (builder.mediaType == MediaType.IMAGE) REQUEST_CODE_IMAGE else if (builder.mediaType == MediaType.VIDEO) REQUEST_CODE_VIDEO else REQUEST_CODE_IMAGE_VIDEO)) {
            initViews()
        }
        mBinding.settingText.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            isFromSetting = true
            startActivity(intent)
        }

        mActivity.registerBroadCastReceiver(
            mBroadcastReceiver, IntentFilter().apply {
                addAction(ACTION_FINISH_GALLERY)
            }
        )
    }

    private fun WarningScreenUi() {
        mBinding.permissionMessage.text = builder.warningUiBuilder.message
        mBinding.permissionMessage.applyTextStyle(
            getColorCode(builder.warningUiBuilder.messageColor),
            builder.warningUiBuilder.messageFont, builder.warningUiBuilder.messageSize
        )
        mBinding.settingText.text = builder.warningUiBuilder.settingText
        mBinding.settingText.applyTextStyle(
            getColorCode(builder.warningUiBuilder.settingColor),
            builder.warningUiBuilder.settingFont, builder.warningUiBuilder.settingSize
        )
    }

    //TODO: Screen UI - Start
    private fun setWindowUI() {
        if (builder.screenBuilder.isFullScreen) {
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            setStatusBarColor(getColorCode(builder.screenBuilder.statusBarColor))
            navigationUI(false, getColorCode(builder.screenBuilder.navigationBarColor))
            hideSystemUI()
        } else {
            if (builder.screenBuilder.statusBarColor != -1) {
                setStatusBarColor(getColorCode(builder.screenBuilder.statusBarColor))
                navigationUI(true, getColorCode(builder.screenBuilder.navigationBarColor))
            }
        }
    }

    private fun setScreenUI() {
        //TODO: Screen
        if (builder.screenBuilder.windowBackgroundColor != -1) mBinding.layoutMediaParent.setBackgroundColor(
            getColorCode(builder.screenBuilder.windowBackgroundColor)
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (builder.screenBuilder.isFullScreen) {
            hideSystemUI()
            setStatusBarColor(getColorCode(builder.screenBuilder.statusBarColor))
            navigationUI(false, getColorCode(builder.screenBuilder.statusBarColor))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (builder.screenBuilder.isFullScreen) {
            if (hasFocus) {
                hideSystemUI()
                setStatusBarColor(getColorCode(builder.screenBuilder.statusBarColor))
                navigationUI(false, getColorCode(builder.screenBuilder.statusBarColor))
            }
        }
    }
    //TODO: Screen UI - End

    //TODO: Toolbar UI - Start
    private fun setToolbarUI() {
        mBinding.toolBarMedia.apply {
            setSupportActionBar(this)
            navigationContentDescription = getLocaleString(R.string.label_back)
        }
        supportActionBar!!.apply {
            title = ""
            subtitle = ""
        }

        if (builder.toolBarBuilder.toolBarColor != -1) mBinding.toolBarMedia.setBackgroundColor(
            getColorCode(builder.toolBarBuilder.toolBarColor)
        )
        if (builder.toolBarBuilder.backIcon != -1) mBinding.toolBarMedia.setNavigationIcon(
            builder.toolBarBuilder.backIcon
        )
        mBinding.toolBarMedia.navigationContentDescription =
            builder.toolBarBuilder.backIconDescription

        if (builder.toolBarBuilder.title.isNotEmpty()) mBinding.textViewTitle.text =
            builder.toolBarBuilder.title
        if (builder.toolBarBuilder.titleColor != -1) mBinding.textViewTitle.setTextColor(
            getColorCode(builder.toolBarBuilder.titleColor)
        )
        if (builder.toolBarBuilder.titleFont != -1) mBinding.textViewTitle.typeface =
            ResourcesCompat.getFont(mActivity, builder.toolBarBuilder.titleFont)
        if (builder.toolBarBuilder.titleSize != -1F) mBinding.textViewTitle.setTextSize(
            TypedValue.COMPLEX_UNIT_SP, builder.toolBarBuilder.titleSize
        )

        updateMediaCount()
    }

    fun updateTitle(title: String) {
        mBinding.textViewTitle.text = title
    }

    override fun onResume() {
        updateHeaderOptionsUI(mSelectedImagesList.size > 0)
        if (isFromSetting) {
            isFromSetting = false
            if (checkPermissions(if (builder.mediaType == MediaType.IMAGE) REQUEST_CODE_IMAGE else if (builder.mediaType == MediaType.VIDEO) REQUEST_CODE_VIDEO else REQUEST_CODE_IMAGE_VIDEO)) {
                initViews()
            }
        }
        super.onResume()
    }

    fun updateHeaderOptionsUI(isVisible: Boolean) {
        mBinding.cardViewDone.isVisible = isVisible
        updateCameraUI(!isVisible)
    }

    fun updateMediaCount() {
        mBinding.textViewTotalCount.text = (if (builder.mediaCount > 1) {
            " (" + mSelectedImagesList.size + "/" + builder.mediaCount + ")"
        } else "")
    }
    //TODO: Toolbar UI - End

    //TODO: Google Photos - Start
    private fun setGooglePhotosUI() {
        mBinding.fabGooglePhotos.isVisible = builder.allowGooglePhotos

        mBinding.fabGooglePhotos.setImageResource(builder.screenBuilder.googlePhotosIcon)

        mBinding.fabGooglePhotos.onClick {
            if (isGooglePhotosAppInstalled(PACKAGE_NAME_GOOGLE_PHOTOS)) {
                if (packageManager.getApplicationInfoCompat(
                        PACKAGE_NAME_GOOGLE_PHOTOS, 0
                    ).enabled
                ) {
                    mGooglePhotoActivityResult.launch(Intent().apply {
                        action = Intent.ACTION_PICK
                        type = when (builder.mediaType) {
                            MediaType.IMAGE -> "image/*"
                            MediaType.VIDEO -> "video/*"
                            else -> "*/*"
                        }
                        setPackage(PACKAGE_NAME_GOOGLE_PHOTOS)
                    })
                } else {
                    showEnableGooglePhotosSnackBar()
                }
            } else {
                showSnackBar(
                    mActivity,
                    mBinding.layoutMediaParent,
                    getLocaleString(R.string.google_photos_not_installed)
                )
            }
        }
    }

    fun updateGooglePhotosUI(isVisible: Boolean) {
        mBinding.fabGooglePhotos.isVisible = if (builder.allowGooglePhotos) {
            isVisible
        } else {
            false
        }
    }

    private var mSnackBar: Snackbar? = null
    private fun showEnableGooglePhotosSnackBar() {
        try {
            mBinding.layoutMediaParent.post {
                if (mSnackBar == null) {
                    Snackbar.make(
                        mBinding.layoutMediaParent,
                        getLocaleString(R.string.google_photos_disable),
                        Snackbar.LENGTH_LONG
                    ).apply {
                        setActionTextColor(Color.YELLOW)
                        setAction(getLocaleString(R.string.label_enable)) { }
                        addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onShown(transientBottomBar: Snackbar?) {
                                super.onShown(transientBottomBar)
                                transientBottomBar!!.view.findViewById<View>(com.google.android.material.R.id.snackbar_action)
                                    .onClick { view ->
                                        view.isEnabled = false
                                        mSnackBar!!.dismiss()
                                        startActivity(Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts(
                                                "package", PACKAGE_NAME_GOOGLE_PHOTOS, null
                                            )
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            view.isEnabled = true
                                        }, 2500L)
                                    }
                            }
                        })
                        view.apply {
                            val txtView =
                                this.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                            txtView.setPadding(32, 16, 32, 16)
                            ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                                v.updatePadding(bottom = 0)
                                // Return the insets so that they keep going down the view hierarchy
                                insets
                            }
                        }
                    }.also {
                        mSnackBar = it
                        if (!mSnackBar!!.isShown) {
                            mSnackBar!!.show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var mGooglePhotoActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data != null) {
                    CopyFileTaskGoogle(applicationContext, result.data?.data!!).execute()
                }
            }
        }

    private inner class CopyFileTaskGoogle(val context: Context, val uri: Uri) :
        CoroutineAsyncTask<Void, Void, String>() {

        private var width: Int = 0
        private var height: Int = 0
        private var timeInSec: Long = 0
        private var fileSizeMB: Long = 0

        override fun doInBackground(vararg params: Void?): String? {
            try {
                val resultFile = createCopyAndReturnRealPath(context, uri)
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(resultFile)
                    width = Integer.valueOf(
                        retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                        )
                    )
                    height = Integer.valueOf(
                        retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                        )
                    )
                    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    timeInSec = TimeUnit.MILLISECONDS.toSeconds(time!!.toLong())
                    retriever.release()
                    fileSizeMB = byteToMB(File(resultFile).length())


                } catch (e:Exception){
                    e.printStackTrace()
                }

                return resultFile
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                if (result != null) {
                    Log.d(
                        "TestData",
                        "CopyFileTaskGoogle width:$width height:$height timeInSec:$timeInSec fileSizeMB:$fileSizeMB"
                    )
                    if (fileSizeMB.toInt() != 0 && builder.videoValidationBuilder.checkValidation && builder.mediaType != MediaType.IMAGE) {
                        var message = ""
                        if (timeInSec > builder.videoValidationBuilder.durationLimit
                        ) {
                            message = builder.videoValidationBuilder.durationLimitMessage
                        } else if (timeInSec > builder.videoValidationBuilder.sizeLimit) {
                            message = builder.videoValidationBuilder.sizeLimitMessage
                        } else if (width > builder.videoValidationBuilder.maxResolution || height > builder.videoValidationBuilder.maxResolution) {
                            message = builder.videoValidationBuilder.maxResolutionMessage
                        } else {
                            val imageItem = MediaItem().apply {
                                path = File(result).absolutePath
                            }
                            val arrayList = ArrayList<MediaItem>().apply {
                                add(imageItem)
                            }
                            finishPickImages(arrayList)
                        }
                        Log.d(
                            "TestData",
                            "CopyFileTaskGoogle width:$width height:$height timeInSec:$timeInSec fileSizeMB:$fileSizeMB :::::message:$message"
                        )
                        if (message.isNotEmpty()) {
                            showWarningDialog(title = message,
                                positionButtonText = builder.videoValidationBuilder.videoValidationDialogBuilder.positiveText,
                                positiveCallback = {
                                    alertDialog?.cancel()
                                },
                                onDismissListener = {},
                                onCancelListener = {})
                        }
                    } else {
                        val imageItem = MediaItem().apply {
                            path = File(result).absolutePath
                        }
                        val arrayList = ArrayList<MediaItem>().apply {
                            add(imageItem)
                        }
                        finishPickImages(arrayList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        fun createCopyAndReturnRealPath(context: Context, uri: Uri?): String? {
            val contentResolver = context.contentResolver ?: return null

            var fileName = if (uri!!.toString().startsWith("file:", ignoreCase = true)) {
                val splitString = uri.encodedPath!!.split("/")
                splitString[splitString.size - 1]
            } else {
                uri.toFilePath(activity = mActivity)
            }
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1)
            // Create file path inside app's data dir
            val filePath = (context.applicationInfo.dataDir.toString() + File.separator + fileName)

            val file = File(filePath)
            if (!file.exists()) {
                file.createNewFile()
            }
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return null
                val outputStream: OutputStream = FileOutputStream(file)
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
                outputStream.close()
                inputStream.close()
            } catch (ignore: IOException) {
                return null
            }
            return file.absolutePath
        }
    }
    //TODO: Google Photos - End


    fun showWarningDialog(
        title: String = "",
        positionButtonText: String = "",
        isCancelable: Boolean = true,
        positiveCallback: View.OnClickListener? = null,
        keyEventCallback: DialogInterface.OnKeyListener? = null,
        onDismissListener: DialogInterface.OnDismissListener? = null,
        onCancelListener: DialogInterface.OnCancelListener? = null,
    ) {
        try {
            val dialogBuilder =
                AlertDialog.Builder(mActivity, com.hashone.commons.R.style.CustomAlertDialog)
            val dialogBinding =
                DialogWarningBinding.inflate(LayoutInflater.from(mActivity), null, false)
            dialogBinding.textViewTitle.text = title
            dialogBinding.textViewYes.text = positionButtonText
            dialogBinding.textViewTitle.isVisible = title.isNotEmpty()
            dialogBinding.textViewYes.isVisible = positionButtonText.isNotEmpty()

            dialogBinding.textViewTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                builder.videoValidationBuilder.videoValidationDialogBuilder.titleSize
            )
            dialogBinding.textViewTitle.setTextColor(
                ContextCompat.getColor(
                    mActivity,
                    builder.videoValidationBuilder.videoValidationDialogBuilder.titleColor
                )
            )
            dialogBinding.textViewTitle.typeface = ResourcesCompat.getFont(
                mActivity,
                builder.videoValidationBuilder.videoValidationDialogBuilder.titleFont
            )

            dialogBinding.textViewYes.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                builder.videoValidationBuilder.videoValidationDialogBuilder.positiveSize
            )
            dialogBinding.textViewYes.setTextColor(
                ContextCompat.getColor(
                    mActivity,
                    builder.videoValidationBuilder.videoValidationDialogBuilder.positiveColor
                )
            )
            dialogBinding.textViewYes.typeface = ResourcesCompat.getFont(
                mActivity,
                builder.videoValidationBuilder.videoValidationDialogBuilder.positiveFont
            )

            dialogBuilder.setView(dialogBinding.root)
            alertDialog = dialogBuilder.create()
            if (!mActivity.isDestroyed) if (alertDialog != null && !alertDialog!!.isShowing) alertDialog!!.show()
            alertDialog!!.setCancelable(isCancelable)
            dialogBinding.textViewYes.setOnClickListener(positiveCallback)
            alertDialog!!.setOnDismissListener(onDismissListener)
            alertDialog!!.setOnCancelListener(onCancelListener)
            alertDialog!!.setOnKeyListener(keyEventCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    //TODO: Action Button UI
    private fun setActionButtonUI() {

        if (builder.actionButtonBuilder.backgroundColor != -1) mBinding.cardViewDone.setCardBackgroundColor(
            getColorCode(builder.actionButtonBuilder.backgroundColor)
        )
        mBinding.cardViewDone.radius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            builder.actionButtonBuilder.radius,
            mActivity.resources.displayMetrics
        )

        if (builder.actionButtonBuilder.text.isNotEmpty()) mBinding.textViewDone.text =
            builder.actionButtonBuilder.text
        mBinding.textViewDone.applyTextStyle(
            getColorCode(builder.actionButtonBuilder.textColor),
            builder.actionButtonBuilder.textFont,
            builder.actionButtonBuilder.textSize
        )

        mBinding.textViewDone.onClick {
            finishPickImages(mSelectedImagesList)
        }
    }

    private fun initViews() {
        try {
            noPermission(false)
            loadFragment(BucketsFragment(), Bundle().apply {
                putSerializable(KEY_MEDIA_GALLERY, builder)
            }, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadFragment(fragment: Fragment, bundle: Bundle? = null, toAdd: Boolean = false) {
        try {
            if (bundle != null) {
                fragment.arguments = bundle
            }
            supportFragmentManager.beginTransaction().apply {
                if (toAdd) {
                    add(R.id.frameContainer, fragment)
                    addToBackStack("New Content")
                } else {
                    replace(R.id.frameContainer, fragment)
                }
                commit()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectedIndex(mImageItem: MediaItem): Int {
        mSelectedImagesList.forEachIndexed { index, imageItem ->
            if (imageItem.mediaId == mImageItem.mediaId) {
                return index
            }
        }
        return -1
    }

    fun addItem(imageItem: MediaItem) = mSelectedImagesList.add(imageItem)

    fun removeItem(position: Int) = mSelectedImagesList.removeAt(position)
    private fun getNewCopModuleIntent(mediaItem: MediaItem): Intent {
        val cropIntent = Crop.build(
            originalImageFilePath = mediaItem.path,
            cropDataSaved = null,
            cropState = null,
            croppedImageBitmap = null
        ) {

            //TODO: Screen
            screenBuilder = Crop.ScreenBuilder(
                windowBackgroundColor = com.hashone.cropper.R.color.window_bg_color,
                statusBarColor = com.hashone.cropper.R.color.white,
                navigationBarColor = com.hashone.cropper.R.color.white,
                cropOuterBorderColor = com.hashone.cropper.R.color.un_select_color,
                borderWidth = 1f,
                borderSpacing = 2f,
            )

            //TODO: Toolbar
            toolBarBuilder = Crop.ToolBarBuilder(
                toolBarColor = com.hashone.cropper.R.color.white,
                backIcon = com.hashone.cropper.R.drawable.ic_back,
                title = "Crop",
                titleColor = com.hashone.cropper.R.color.black,
                titleFont = com.hashone.cropper.R.font.roboto_medium,
                titleSize = 16F,
            )


            //TODO: AspectRatio
            aspectRatioBuilder = Crop.AspectRatioBuilder(
                backgroundColor = com.hashone.cropper.R.color.white,
                selectedColor = com.hashone.cropper.R.color.black,
                unSelectedColor = com.hashone.cropper.R.color.un_select_color,
                titleFont = com.hashone.cropper.R.font.roboto_medium,
            )


            //TODO: Bottom Icon & Text
            bottomPanelBuilder = Crop.BottomPanelBuilder(
                cropBottomBackgroundColor = com.hashone.cropper.R.color.white,
                dividerColor = com.hashone.cropper.R.color.white,
                doneButtonBuilder = Crop.ButtonBuilder(
                    textColor = com.hashone.cropper.R.color.black,
                    icon = com.hashone.cropper.R.drawable.ic_check_croppy_selected,
                    buttonText = "Crop",
                    textFont = com.hashone.cropper.R.font.roboto_medium,
                    textSize = 16F,
                ),
                cancelButtonBuilder = Crop.ButtonBuilder(
                    textColor = com.hashone.cropper.R.color.black,
                    icon = com.hashone.cropper.R.drawable.ic_cancel,
                    buttonText = "Skip",
                    textFont = com.hashone.cropper.R.font.roboto_medium,
                    textSize = 16F,
                ),
            )
        }

        return Crop.open(activity = this@MediaActivity, cropIntent)
    }

    fun finishPickImages(images: ArrayList<MediaItem>) {
        if (builder.enableCropMode && builder.mediaType == MediaType.IMAGE && builder.mediaCropBuilder.cropClassName.isEmpty() && builder.mediaCropBuilder.appPackageName.isEmpty()) {
            getNewCopModuleIntent(images[0]).let {
                mActivityLauncher.launch(it,
                    onActivityResult = object :
                        BetterActivityResult.OnActivityResult<ActivityResult> {
                        override fun onActivityResult(result: ActivityResult) {
                            if (result.resultCode == Activity.RESULT_OK) {
                                result.data?.let { intentData ->
                                    if (intentData.hasExtra(CropActivity.KEY_RETURN_CROP_DATA)) {
                                        val myCropDataSaved =
                                            intentData.extras?.serializable<CropDataSaved>(
                                                CropActivity.KEY_RETURN_CROP_DATA
                                            )
                                        setResultData(images, myCropDataSaved)
                                    } else {
                                        setResultData(images)
                                    }
                                    if (builder.isForceClose) finish()
                                }
                            }
                        }
                    })
            }
        } else if (!builder.enableCropMode && /* && builder.mediaType == MediaType.IMAGE &&*/ builder.mediaCropBuilder.cropClassName.isNotEmpty() && builder.mediaCropBuilder.appPackageName.isNotEmpty()) {
            if (images.size > 0) {
                val className =
                    Class.forName("${builder.mediaCropBuilder.appPackageName}.${builder.mediaCropBuilder.cropClassName}")
                val intent = Intent(this, className)
                val file = File(images[0].path)
                val uri = Uri.fromFile(file)
                val fileOperationRequest = FileOperationRequest(
                    StorageType.INTERNAL,
                    builder.mediaCropBuilder.projectDirectoryPath,
                    file.nameWithoutExtension + System.currentTimeMillis(),
                    getExtensionOfFile(file)
                )
                val destinationPath = FileCreator.createFile(
                    fileOperationRequest, this
                )
                intent.apply {
                    Bundle().apply {
                        putString(KEY_CROP_URI, uri.path)
                        putString(KEY_CROP_FILET, file.absolutePath)
                        putString(KEY_CROP_DESTINATION_PATH, destinationPath.absolutePath)
                        putString(
                            KEY_CROP_PROJECT_DIRECTORY,
                            builder.mediaCropBuilder.projectDirectoryPath
                        )
                        if (images.isNotEmpty())
                            putExtra(KEY_MEDIA_PATHS, images)
                    }.also { this.putExtras(it) }
                }

                intent.let {
                    mActivityLauncher.launch(it,
                        onActivityResult = object :
                            BetterActivityResult.OnActivityResult<ActivityResult> {
                            override fun onActivityResult(result: ActivityResult) {
                                if (result.resultCode == Activity.RESULT_OK) {
                                    result.data?.let { intentData ->

                                        if (intentData.hasExtra(KEY_IMAGE_PATH) && intentData.hasExtra(
                                                KEY_IMAGE_ORIGINAL_PATH
                                            ) && intentData.hasExtra(KEY_IMAGE_ORIGINAL_REPLACE)
                                        ) {
                                            val filePath =
                                                intentData.extras!!.getString(KEY_IMAGE_PATH)!!
                                            val originalImagePath =
                                                intentData.extras!!.getString(
                                                    KEY_IMAGE_ORIGINAL_PATH
                                                )!!
                                            val isReplace =
                                                if (intentData.hasExtra(KEY_IMAGE_ORIGINAL_REPLACE)) {
                                                    intentData.extras!!.getBoolean(
                                                        KEY_IMAGE_ORIGINAL_REPLACE, false
                                                    )
                                                } else {
                                                    false
                                                }
                                            setResultData(

                                                ArrayList(),
                                                null,
                                                filePath,
                                                originalImagePath,
                                                isReplace
                                            )

                                        } else {
                                            setResultData(images)
                                        }
                                        if (builder.isForceClose) finish()
                                    }
                                }
                            }
                        })
                }
            }
        } else if (builder.mediaType == MediaType.IMAGE && builder.mediaCropBuilder.cropClassName.isNotEmpty() && builder.mediaCropBuilder.appPackageName.isNotEmpty()) {
            if (images.size > 0) {
                val className =
                    Class.forName("${builder.mediaCropBuilder.appPackageName}.${builder.mediaCropBuilder.cropClassName}")
                val intent = Intent(this, className)
                val file = File(images[0].path)
                val uri = Uri.fromFile(file)
                val fileOperationRequest = FileOperationRequest(
                    StorageType.INTERNAL,
                    builder.mediaCropBuilder.projectDirectoryPath,
                    file.nameWithoutExtension + System.currentTimeMillis(),
                    getExtensionOfFile(file)
                )
                val destinationPath = FileCreator.createFile(
                    fileOperationRequest, this
                )
                intent.apply {
                    Bundle().apply {
                        putString(KEY_CROP_URI, uri.path)
                        putString(KEY_CROP_FILET, file.absolutePath)
                        putString(KEY_CROP_DESTINATION_PATH, destinationPath.absolutePath)
                        putString(
                            KEY_CROP_PROJECT_DIRECTORY,
                            builder.mediaCropBuilder.projectDirectoryPath
                        )
                        if (images.isNotEmpty())
                            putExtra(KEY_MEDIA_PATHS, images)
                    }.also { this.putExtras(it) }
                }

                intent.let {
                    mActivityLauncher.launch(it,
                        onActivityResult = object :
                            BetterActivityResult.OnActivityResult<ActivityResult> {
                            override fun onActivityResult(result: ActivityResult) {
                                if (result.resultCode == Activity.RESULT_OK) {
                                    result.data?.let { intentData ->

                                        if (intentData.hasExtra(KEY_IMAGE_PATH) && intentData.hasExtra(
                                                KEY_IMAGE_ORIGINAL_PATH
                                            ) && intentData.hasExtra(KEY_IMAGE_ORIGINAL_REPLACE)
                                        ) {
                                            val filePath =
                                                intentData.extras!!.getString(KEY_IMAGE_PATH)!!
                                            val originalImagePath =
                                                intentData.extras!!.getString(
                                                    KEY_IMAGE_ORIGINAL_PATH
                                                )!!
                                            val isReplace =
                                                if (intentData.hasExtra(KEY_IMAGE_ORIGINAL_REPLACE)) {
                                                    intentData.extras!!.getBoolean(
                                                        KEY_IMAGE_ORIGINAL_REPLACE, false
                                                    )
                                                } else {
                                                    false
                                                }

                                            setResultData(
                                                ArrayList(),
                                                null,
                                                filePath,
                                                originalImagePath,
                                                isReplace
                                            )

                                        } else {
                                            setResultData(images)
                                        }
                                        if (builder.isForceClose) finish()
                                    }
                                }
                            }
                        })
                }
            }
        } else {
            setResultData(images)
            if (builder.isForceClose) finish()
        }
    }

    private fun setResultData(
        images: ArrayList<MediaItem>,
        cropData: CropDataSaved? = null,
        filePath: String = "",
        originalImagePath: String = " ",
        isReplace: Boolean = false
    ) {
        setResult(RESULT_OK, Intent().apply {
            if (cropData != null)
                putExtra(CropActivity.KEY_RETURN_CROP_DATA, cropData)
            if (images.isNotEmpty())
                putExtra(KEY_MEDIA_PATHS, images)
            if (filePath.isNotEmpty())
                putExtra(KEY_IMAGE_PATH, filePath)
            if (originalImagePath.isNotEmpty())
                putExtra(
                    KEY_IMAGE_ORIGINAL_PATH, originalImagePath
                )
            putExtra(KEY_IMAGE_ORIGINAL_REPLACE, isReplace)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mBinding.toolBarMedia.inflateMenu(R.menu.menu_media)
        if (mOptionMenu == null) {
            mOptionMenu = menu
            mOptionMenu?.findItem(R.id.action_camera)?.isVisible = builder.allowCamera
            mOptionMenu?.findItem(R.id.action_camera)?.icon =
                ContextCompat.getDrawable(this, builder.toolBarBuilder.cameraIcon)
        } else updateHeaderOptionsUI(mSelectedImagesList.size > 0)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }

            R.id.action_camera -> {
                openCamera()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //TODO: Camera - Start
    private var mCurrentRequestCode = -1

    private fun setCameraUI() {

    }

    private fun updateCameraUI(isVisible: Boolean) {
        mOptionMenu?.let {
            it.findItem(R.id.action_camera).isVisible = if (builder.allowCamera) {
                isVisible
            } else {
                false
            }
        }
    }

    private fun checkCameraPermission(requestCode: Int): Boolean {
        mCurrentRequestCode = requestCode
        val permissions = ArrayList<String>()
        if (mCurrentRequestCode == REQUEST_CODE_CAMERA) {
            if (!isPermissionGranted(Manifest.permission.CAMERA)) permissions.add(Manifest.permission.CAMERA)
        }
        if (permissions.isNotEmpty()) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            return true
        }
        return false
    }

    private fun checkCameraPermissionStatus(isGranted: Boolean) {
        if (isGranted) {
            openCamera()
        } else {
            showGalleryCustomAlertDialog(message = getLocaleString(R.string.allow_camera_permission),
                negativeButtonText = getLocaleString(R.string.label_cancel).uppercase(Locale.getDefault()),
                positionButtonText = getLocaleString(R.string.label_grant).uppercase(Locale.getDefault()),
                negativeCallback = {
                    galleryAlertDialog?.cancel()
                },
                positiveCallback = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    galleryAlertDialog?.cancel()
                },
                onDismissListener = {},
                onCancelListener = {})
        }
    }

    private var requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            checkCameraPermissionStatus(isGranted)
        }

    private fun openCamera() {
        if (checkCameraPermission(REQUEST_CODE_CAMERA)) {
            if (checkCameraHardware()) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0) //Low Quality
                takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (builder.videoValidationBuilder.sizeLimit * 1048576L))  //builder.videoValidationBuilder.sizeLimit MB
                takeVideoIntent.putExtra(
                    MediaStore.EXTRA_DURATION_LIMIT,
                    builder.videoValidationBuilder.durationLimit
                ) //30Seconds
                val chooserIntent = Intent.createChooser(
                    if (builder.mediaType == MediaType.IMAGE) takePictureIntent else if (builder.mediaType == MediaType.VIDEO) takeVideoIntent else takePictureIntent,
                    builder.cameraActionTitle
                )
                if (builder.mediaType == MediaType.IMAGE_VIDEO) {
                    chooserIntent.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        arrayOf(takePictureIntent, takeVideoIntent)
                    )
                }

                mActivityLauncher.launch(chooserIntent,
                    onActivityResult = object :
                        BetterActivityResult.OnActivityResult<ActivityResult> {
                        override fun onActivityResult(result: ActivityResult) {
                            if (result.resultCode == Activity.RESULT_OK) {
                                if (result.data?.data != null) {
                                    val videoUri = result.data?.data
                                    videoUri?.let {
                                        val videoFilePath = it.toFilePath(mActivity)
                                        val imageItem = MediaItem().apply {
                                            path = videoFilePath
                                        }
                                        val arrayList = ArrayList<MediaItem>().apply {
                                            add(imageItem)
                                        }

                                        MediaScannerConnection.scanFile(
                                            mActivity, arrayOf(videoFilePath), null
                                        ) { _, _ ->
                                            setResultData(arrayList)
                                            finish()
                                        }
                                    }
                                } else {
                                    val bitmap = result.data?.extras?.parcelable<Bitmap>("data")
                                    bitmap?.let {
                                        val savedFile = bitmap.saveToFile(
                                            fileName = "Camera_${System.currentTimeMillis()}.$EXTENSION_PNG",
                                            saveDir = getInternalCameraDir(mActivity),
                                            compressFormat = EXTENSION_PNG
                                        )
                                        val imageItem = MediaItem().apply {
                                            path = savedFile.absolutePath
                                        }
                                        val arrayList = ArrayList<MediaItem>().apply {
                                            add(imageItem)
                                        }
                                        MediaScannerConnection.scanFile(
                                            mActivity, arrayOf(savedFile.absolutePath), null
                                        ) { _, _ ->
                                            setResultData(arrayList)
                                            finish()
                                        }
                                    }
                                }
                            }
                        }
                    })
            }
        }
    }
    //TODO: Camera - End

    private fun getExtensionOfFile(file: File): FileExtension {
        return when (file.extension.lowercase()) {
            "webp" -> {
                FileExtension.WEBP
            }

            "jpg", "jpeg" -> {
                FileExtension.JPEG
            }

            else -> FileExtension.PNG
        }
    }

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
            initViews()
        } else {
            showGalleryCustomAlertDialog(message = builder.permissionBuilder.message,
                negativeButtonText = builder.permissionBuilder.negativeText.uppercase(Locale.getDefault()),
                positionButtonText = builder.permissionBuilder.positiveText.uppercase(Locale.getDefault()),
                negativeCallback = {
                    noPermission()
                    galleryAlertDialog?.cancel()
                },
                positiveCallback = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    isFromSetting = true
                    startActivity(intent)
                    galleryAlertDialog?.cancel()
                },
                onDismissListener = { noPermission() },
                onCancelListener = {

                })
        }
    }

    private fun noPermission(isVisible: Boolean = true) {
        mBinding.permissionContainer.isVisible = isVisible
        mBinding.fabGooglePhotos.isVisible = !isVisible
    }

    override fun onDestroy() {
        unregisterReceiver(mBroadcastReceiver)
        super.onDestroy()
    }

    var galleryAlertDialog: AlertDialog? = null
    fun showGalleryCustomAlertDialog(
        title: String = "",
        message: String = "",
        positionButtonText: String = "",
        negativeButtonText: String = "",
        neutralButtonText: String = "",
        isCancelable: Boolean = true,
        negativeCallback: View.OnClickListener? = null,
        positiveCallback: View.OnClickListener? = null,
        neutralCallback: View.OnClickListener? = null,
        keyEventCallback: DialogInterface.OnKeyListener? = null,
        onDismissListener: DialogInterface.OnDismissListener? = null,
        onCancelListener: DialogInterface.OnCancelListener? = null
    ) {
        try {
            val alertBuilder = AlertDialog.Builder(mActivity, com.hashone.commons.R.style.CustomAlertDialog)
            val dialogBinding =
                DialogConfirmationBinding.inflate(LayoutInflater.from(mActivity), null, false)
            dialogBinding.textViewTitle.text = title
            dialogBinding.textViewMessage.text = message
            dialogBinding.textViewYes.text = positionButtonText
            dialogBinding.textViewNo.text = negativeButtonText
            dialogBinding.textViewNeutral.text = neutralButtonText
            dialogBinding.textViewTitle.isVisible = title.isNotEmpty()

            if (title.isEmpty()) {
                val mLayoutParams =
                    dialogBinding.textViewMessage.layoutParams as ConstraintLayout.LayoutParams
                mLayoutParams.setMargins(0, dpToPx(24F).roundToInt(), 0, 0)
                dialogBinding.textViewMessage.layoutParams = mLayoutParams

                dialogBinding.textViewMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)

                dialogBinding.textViewMessage.applyTextStyle(
                    ContextCompat.getColor(
                        mActivity,
                        builder.permissionBuilder.messageColor
                    ),
                    builder.permissionBuilder.messageFont,
                    builder.permissionBuilder.messageSize
                )

            }

            dialogBinding.textViewYes.isVisible = positionButtonText.isNotEmpty()
            dialogBinding.textViewNo.isVisible = negativeButtonText.isNotEmpty()
            dialogBinding.view4.isVisible = neutralButtonText.isNotEmpty()
            dialogBinding.textViewNeutral.isVisible = neutralButtonText.isNotEmpty()


            dialogBinding.textViewYes.typeface = ResourcesCompat.getFont(mActivity, builder.permissionBuilder.positiveFont)
            dialogBinding.textViewYes.setTextColor(ContextCompat.getColor(mActivity, builder.permissionBuilder.positiveColor))
            dialogBinding.textViewYes.setTextSize(TypedValue.COMPLEX_UNIT_SP, builder.permissionBuilder.positiveSize)
            dialogBinding.textViewYes.isAllCaps = builder.permissionBuilder.positiveIsCap

            dialogBinding.textViewNo.typeface = ResourcesCompat.getFont(mActivity, builder.permissionBuilder.negativeFont)
            dialogBinding.textViewNo.setTextColor(ContextCompat.getColor(mActivity, builder.permissionBuilder.negativeColor))
            dialogBinding.textViewNo.setTextSize(TypedValue.COMPLEX_UNIT_SP, builder.permissionBuilder.negativeSize)
            dialogBinding.textViewNo.isAllCaps = builder.permissionBuilder.negativeIsCap

            alertBuilder.setView(dialogBinding.root)
            galleryAlertDialog = alertBuilder.create()
            if (!mActivity.isDestroyed) if (galleryAlertDialog != null && !galleryAlertDialog!!.isShowing) galleryAlertDialog!!.show()
            galleryAlertDialog!!.setCancelable(isCancelable)
            dialogBinding.textViewYes.setOnClickListener(positiveCallback)
            dialogBinding.textViewNo.setOnClickListener(negativeCallback)
            dialogBinding.textViewNeutral.setOnClickListener(neutralCallback)
            galleryAlertDialog!!.setOnDismissListener(onDismissListener)
            galleryAlertDialog!!.setOnCancelListener(onCancelListener)
            galleryAlertDialog!!.setOnKeyListener(keyEventCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}