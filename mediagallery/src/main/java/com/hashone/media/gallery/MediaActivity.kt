package com.hashone.media.gallery

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.hashone.commons.extensions.saveToFile
import com.hashone.commons.extensions.serializable
import com.hashone.commons.extensions.setStatusBarColor
import com.hashone.commons.extensions.toFilePath
import com.hashone.commons.utils.EXTENSION_PNG
import com.hashone.commons.utils.PACKAGE_NAME_GOOGLE_PHOTOS
import com.hashone.commons.utils.getInternalCameraDir
import com.hashone.commons.utils.showSnackBar
import com.hashone.cropper.CropActivity
import com.hashone.cropper.builder.Crop
import com.hashone.cropper.model.CropDataSaved
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.databinding.ActivityMediaBinding
import com.hashone.media.gallery.enums.MediaType
import com.hashone.media.gallery.fragment.BucketsFragment
import com.hashone.media.gallery.model.MediaItem
import com.hashone.media.gallery.utils.KEY_CROP_DESTINATION_PATH
import com.hashone.media.gallery.utils.KEY_CROP_FILET
import com.hashone.media.gallery.utils.KEY_CROP_PROJECT_DIRECTORY
import com.hashone.media.gallery.utils.KEY_CROP_URI
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_PATH
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_REPLACE
import com.hashone.media.gallery.utils.KEY_IMAGE_PATH
import com.hashone.media.gallery.utils.KEY_MEDIA_GALLERY
import com.hashone.media.gallery.utils.KEY_MEDIA_PATH
import com.hashone.media.gallery.utils.KEY_MEDIA_PATHS
import com.hashone.media.gallery.utils.MediaPref
import com.hashone.media.gallery.utils.file.FileCreator
import com.hashone.media.gallery.utils.file.FileExtension
import com.hashone.media.gallery.utils.file.FileOperationRequest
import com.hashone.media.gallery.utils.file.StorageType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Locale

class MediaActivity : BaseActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {

        builder =
            (intent!!.extras!!.serializable<MediaGallery>(KEY_MEDIA_GALLERY)!!).builder

        setWindowUI()

        super.onCreate(savedInstanceState)

        mActivity = this
        mBinding = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mMediaPref = MediaPref(mActivity)
        mMediaPref.clearMediaPref()

        setScreenUI()
        setToolbarUI()
        setCameraUI()
        setGooglePhotosUI()
        setActionButtonUI()
        initViews()
    }

    //TODO: Screen UI - Start
    private fun setWindowUI() {
        if (builder.isFullScreen) {
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            setStatusBarColor(getColorCode(builder.statusBarColor))
            navigationUI(false, getColorCode(builder.navigationBarColor))
            hideSystemUI()
        } else {
            if (builder.statusBarColor != -1) {
                setStatusBarColor(getColorCode(builder.statusBarColor))
                navigationUI(true, getColorCode(builder.navigationBarColor))
            }
        }
    }

    private fun setScreenUI() {
        //TODO: Screen
        if (builder.windowBackgroundColor != -1)
            mBinding.layoutMediaParent.setBackgroundColor(getColorCode(builder.windowBackgroundColor))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (builder.isFullScreen) {
            hideSystemUI()
            setStatusBarColor(getColorCode(builder.statusBarColor))
            navigationUI(false, getColorCode(builder.statusBarColor))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (builder.isFullScreen) {
            if (hasFocus) {
                hideSystemUI()
                setStatusBarColor(getColorCode(builder.statusBarColor))
                navigationUI(false, getColorCode(builder.statusBarColor))
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

        if (builder.toolBarColor != -1)
            mBinding.toolBarMedia.setBackgroundColor(getColorCode(builder.toolBarColor))
        if (builder.backPressIcon != -1)
            mBinding.toolBarMedia.setNavigationIcon(builder.backPressIcon)
        mBinding.toolBarMedia.navigationContentDescription = builder.backPressIconDescription

        if (builder.toolBarTitle.isNotEmpty())
            mBinding.textViewTitle.text = builder.toolBarTitle
        if (builder.toolBarTitleColor != -1)
            mBinding.textViewTitle.setTextColor(getColorCode(builder.toolBarTitleColor))
        if (builder.toolBarTitleFont != -1)
            mBinding.textViewTitle.typeface =
                ResourcesCompat.getFont(mActivity, builder.toolBarTitleFont)
        if (builder.toolBarTitleSize != -1F)
            mBinding.textViewTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                builder.toolBarTitleSize
            )

        updateMediaCount()
    }

    fun updateTitle(title: String) {
        mBinding.textViewTitle.text = title
    }

    override fun onResume() {
        updateHeaderOptionsUI(mSelectedImagesList.size > 0)
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

        mBinding.fabGooglePhotos.setImageResource(builder.googlePhotosIcon)

        mBinding.fabGooglePhotos.onClick {
            if (isGooglePhotosAppInstalled(PACKAGE_NAME_GOOGLE_PHOTOS)) {
                if (packageManager.getApplicationInfo(PACKAGE_NAME_GOOGLE_PHOTOS, 0).enabled) {
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
                        addCallback(object :
                            BaseTransientBottomBar.BaseCallback<Snackbar>() {
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

        override fun doInBackground(vararg params: Void?): String? {
            try {
                return createCopyAndReturnRealPath(context, uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                if (result != null) {
                    val imageItem = MediaItem().apply {
                        path = File(result).absolutePath
                    }
                    val arrayList = ArrayList<MediaItem>().apply {
                        add(imageItem)
                    }
                    finishPickImages(arrayList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun createCopyAndReturnRealPath(context: Context, uri: Uri?): String? {
            val contentResolver = context.contentResolver ?: return null

            var fileName =
                if (uri!!.toString().startsWith("file:", ignoreCase = true)) {
                    val splitString = uri.encodedPath!!.split("/")
                    splitString[splitString.size - 1]
                } else {
                    uri.toFilePath(activity = mActivity)
                }
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            // Create file path inside app's data dir
            val filePath =
                (context.applicationInfo.dataDir.toString() + File.separator + fileName)

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

    //TODO: Action Button UI
    private fun setActionButtonUI() {

        if (builder.buttonBackgroundColor != -1)
            mBinding.cardViewDone.setCardBackgroundColor(getColorCode(builder.buttonBackgroundColor))
        mBinding.cardViewDone.radius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            builder.buttonRadius,
            mActivity.resources.displayMetrics
        )

        if (builder.buttonText.isNotEmpty())
            mBinding.textViewDone.text = builder.buttonText
        mBinding.textViewDone.applyTextStyle(
            getColorCode(builder.buttonTextColor),
            builder.buttonTextFont,
            builder.buttonTextSize
        )

        mBinding.textViewDone.onClick {
            finishPickImages(mSelectedImagesList)
        }
    }

    private fun initViews() {
        try {
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
        return Crop.open(activity = this@MediaActivity, Crop.build(
            originalImageFilePath = mediaItem.path,
        ) {
            cropState = null
            croppedImageBitmap = null
            //TODO: Screen
            windowBackgroundColor = com.hashone.cropper.R.color.extra_extra_light_gray_color
            statusBarColor = com.hashone.cropper.R.color.extra_extra_light_gray_color
            navigationBarColor = com.hashone.cropper.R.color.white

            //TODO: Toolbar
            toolBarColor = com.hashone.cropper.R.color.white
            backPressIcon = com.hashone.cropper.R.drawable.ic_back
            backPressIconDescription = ""
            toolBarTitle = "Crop"
            toolBarTitleColor = com.hashone.cropper.R.color.black
            toolBarTitleFont = com.hashone.cropper.R.font.outfit_regular
            toolBarTitleSize = 16F

            //TODO: AspectRatio
            aspectRatioBackgroundColor = com.hashone.cropper.R.color.white
            aspectRatioSelectedColor = com.hashone.cropper.R.color.black
            aspectRatioUnSelectedColor = com.hashone.cropper.R.color.dark_gray_color_2
            aspectRatioTitleFont = com.hashone.cropper.R.font.roboto_medium

            //TODO: Bottom Icon & Text
            cropDoneTextColor = com.hashone.cropper.R.color.black
            cropDoneIcon = com.hashone.cropper.R.drawable.ic_check_croppy_selected
            cropDoneText = "Crop"
            cropDoneTextFont = com.hashone.cropper.R.font.roboto_medium
            cropDoneTextSize = 16F

            cropCancelTextColor = com.hashone.cropper.R.color.black
            cropCancelIcon = com.hashone.cropper.R.drawable.ic_cancel
            cropCancelText = "Skip"
            cropCancelTextFont = com.hashone.cropper.R.font.roboto_medium
            cropCancelTextSize = 16F

            cropBottomBackgroundColor = com.hashone.cropper.R.color.white
            dividerColor = com.hashone.cropper.R.color.extra_extra_light_gray_color

        })
    }

    fun finishPickImages(images: ArrayList<MediaItem>) {
        if (builder.enableCropMode && builder.mediaType == MediaType.IMAGE && builder.cropClassName.isEmpty() && builder.appPackageName.isEmpty()) {
            getNewCopModuleIntent(images[0]).let {
                mActivityLauncher.launch(
                    it,
                    onActivityResult = object :
                        BetterActivityResult.OnActivityResult<ActivityResult> {
                        override fun onActivityResult(activityResult: ActivityResult) {
                            if (activityResult.resultCode == Activity.RESULT_OK) {
                                activityResult.data?.let { intentData ->
                                    if (intentData.hasExtra(CropActivity.KEY_RETURN_CROP_DATA)) {
                                        val myCropDataSaved =
                                            intentData.extras?.serializable<CropDataSaved>(
                                                CropActivity.KEY_RETURN_CROP_DATA
                                            )
                                        setResult(RESULT_OK, Intent().apply {
                                            putExtra(
                                                CropActivity.KEY_RETURN_CROP_DATA,
                                                myCropDataSaved
                                            )
                                        })
                                    } else {
                                        setResult(RESULT_OK, Intent().apply {
                                            putExtra(CropActivity.KEY_RETURN_CROP_DATA, images)
                                            putExtra(KEY_MEDIA_PATHS, images)
                                        })
                                    }
                                    if (builder.isForceClose) finish()
                                }
                            }
                        }
                    }
                )
            }
        }
        else if (builder.enableCropMode && builder.mediaType == MediaType.IMAGE && builder.cropClassName.isNotEmpty() && builder.appPackageName.isNotEmpty()) {
            val c = Class.forName("${builder.appPackageName}.${builder.cropClassName}")
            val intent = Intent(this, c)
            val file = File(images[0].path)
            val uri = Uri.fromFile(file)
            val fileOperationRequest = FileOperationRequest(
                StorageType.INTERNAL,
                builder.projectDirectoryPath,
                file.nameWithoutExtension + System.currentTimeMillis(),
                getExtensionOfFile(file)
            )
            val destinationPath =
                FileCreator
                    .createFile(
                        fileOperationRequest,
                        this
                    )
            intent.apply {
                Bundle().apply {
                    putString(KEY_CROP_URI, uri.path)
                    putString(KEY_CROP_FILET, file.absolutePath)
                    putString(KEY_CROP_DESTINATION_PATH, destinationPath.absolutePath)
                    putString(KEY_CROP_PROJECT_DIRECTORY, builder.projectDirectoryPath)
                }.also { this.putExtras(it) }
            }

            intent.let {
                mActivityLauncher.launch(
                    it,
                    onActivityResult = object :
                        BetterActivityResult.OnActivityResult<ActivityResult> {
                        override fun onActivityResult(activityResult: ActivityResult) {
                            if (activityResult.resultCode == Activity.RESULT_OK) {
                                activityResult.data?.let { intentData ->

                                    if (intentData.hasExtra(KEY_IMAGE_PATH) && intentData.hasExtra(
                                            KEY_IMAGE_ORIGINAL_PATH
                                        ) && intentData.hasExtra(KEY_IMAGE_ORIGINAL_REPLACE)
                                    ) {
                                        val filePath =
                                            intentData.extras!!.getString(KEY_IMAGE_PATH)!!
                                        val originalImagePath =
                                            intentData.extras!!.getString(KEY_IMAGE_ORIGINAL_PATH)!!
                                        val isReplace =
                                            if (intentData.hasExtra(KEY_IMAGE_ORIGINAL_REPLACE)) {
                                                intentData.extras!!.getBoolean(
                                                    KEY_IMAGE_ORIGINAL_REPLACE,
                                                    false
                                                )
                                            } else {
                                                false
                                            }
                                        setResult(
                                            Activity.RESULT_OK,
                                            Intent()
                                                .putExtra(KEY_IMAGE_PATH, filePath)
                                                .putExtra(
                                                    KEY_IMAGE_ORIGINAL_PATH,
                                                    originalImagePath
                                                )
                                                .putExtra(KEY_IMAGE_ORIGINAL_REPLACE, isReplace)
                                        )
                                    } else {
                                        setResult(RESULT_OK, Intent().apply {
                                            putExtra(CropActivity.KEY_RETURN_CROP_DATA, images)
                                            putExtra(KEY_MEDIA_PATHS, images)
                                        })
                                    }
                                    if (builder.isForceClose) finish()
                                }
                            }
                        }
                    }
                )
            }
        }
        else if (builder.mediaType == MediaType.IMAGE && builder.cropClassName.isNotEmpty() && builder.appPackageName.isNotEmpty()) {
            val c = Class.forName("${builder.appPackageName}.${builder.cropClassName}")
            val intent = Intent(this, c)
            val file = File(images[0].path)
            val uri = Uri.fromFile(file)
            val fileOperationRequest = FileOperationRequest(
                StorageType.INTERNAL,
                builder.projectDirectoryPath,
                file.nameWithoutExtension + System.currentTimeMillis(),
                getExtensionOfFile(file)
            )
            val destinationPath =
                FileCreator
                    .createFile(
                        fileOperationRequest,
                        this
                    )
            intent.apply {
                Bundle().apply {
                    putString(KEY_CROP_URI, uri.path)
                    putString(KEY_CROP_FILET, file.absolutePath)
                    putString(KEY_CROP_DESTINATION_PATH, destinationPath.absolutePath)
                    putString(KEY_CROP_PROJECT_DIRECTORY, builder.projectDirectoryPath)
                }.also { this.putExtras(it) }
            }

            intent.let {
                mActivityLauncher.launch(
                    it,
                    onActivityResult = object :
                        BetterActivityResult.OnActivityResult<ActivityResult> {
                        override fun onActivityResult(activityResult: ActivityResult) {
                            if (activityResult.resultCode == Activity.RESULT_OK) {
                                activityResult.data?.let { intentData ->

                                    if (intentData.hasExtra(KEY_IMAGE_PATH) && intentData.hasExtra(
                                            KEY_IMAGE_ORIGINAL_PATH
                                        ) && intentData.hasExtra(KEY_IMAGE_ORIGINAL_REPLACE)
                                    ) {
                                        val filePath =
                                            intentData.extras!!.getString(KEY_IMAGE_PATH)!!
                                        val originalImagePath =
                                            intentData.extras!!.getString(KEY_IMAGE_ORIGINAL_PATH)!!
                                        val isReplace =
                                            if (intentData.hasExtra(KEY_IMAGE_ORIGINAL_REPLACE)) {
                                                intentData.extras!!.getBoolean(
                                                    KEY_IMAGE_ORIGINAL_REPLACE,
                                                    false
                                                )
                                            } else {
                                                false
                                            }
                                        setResult(
                                            Activity.RESULT_OK,
                                            Intent()
                                                .putExtra(KEY_IMAGE_PATH, filePath)
                                                .putExtra(
                                                    KEY_IMAGE_ORIGINAL_PATH,
                                                    originalImagePath
                                                )
                                                .putExtra(KEY_IMAGE_ORIGINAL_REPLACE, isReplace)
                                        )
                                    } else {
                                        setResult(RESULT_OK, Intent().apply {
                                            putExtra(CropActivity.KEY_RETURN_CROP_DATA, images)
                                            putExtra(KEY_MEDIA_PATHS, images)
                                        })
                                    }
                                    if (builder.isForceClose) finish()
                                }
                            }
                        }
                    }
                )
            }
        }
        else {
            setResult(RESULT_OK, Intent().apply {
                putExtra(KEY_MEDIA_PATHS, images)
//                putExtra(KEY_MEDIA_PATH, images[0].path)
            })
            if (builder.isForceClose) finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mBinding.toolBarMedia.inflateMenu(R.menu.menu_media)
        if (mOptionMenu == null) {
            mOptionMenu = menu
            mOptionMenu?.findItem(R.id.action_camera)?.isVisible = builder.allowCamera
            mOptionMenu?.findItem(R.id.action_camera)?.icon = getDrawable(builder.cameraIcon)
        } else
            updateHeaderOptionsUI(mSelectedImagesList.size > 0)
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
    private val REQUEST_CODE_CAMERA = 104
    private var mCurrentRequestCode = -1

    private fun setCameraUI() {

    }

    fun updateCameraUI(isVisible: Boolean) {
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
            if (!isPermissionGranted(Manifest.permission.CAMERA))
                permissions.add(Manifest.permission.CAMERA)
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
            showCustomAlertDialog(message = getLocaleString(R.string.allow_camera_permission),
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

    private var requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            checkCameraPermissionStatus(isGranted)
        }

    private fun openCamera() {
        if (checkCameraPermission(REQUEST_CODE_CAMERA)) {
            if (checkCameraHardware()) {

                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                val chooserIntent =
                    Intent.createChooser(takePictureIntent, "Capture Image or Video")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takeVideoIntent))

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
                                            mActivity,
                                            arrayOf(videoFilePath), null
                                        ) { _, _ ->
                                            setResult(Activity.RESULT_OK, Intent().apply {
                                                putExtra(KEY_MEDIA_PATHS, arrayList)
                                            })
                                            finish()
                                        }
                                    }
                                } else {
                                    val bitmap =
                                        result.data?.extras?.parcelable<Bitmap>("data")
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
                                            mActivity,
                                            arrayOf(savedFile.absolutePath), null
                                        ) { _, _ ->
                                            setResult(Activity.RESULT_OK, Intent().apply {
                                                putExtra(KEY_MEDIA_PATHS, arrayList)
                                            })
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

    fun getExtensionOfFile(file: File): FileExtension {
        return when (file.extension.toLowerCase()) {
            "webp" -> {
                FileExtension.WEBP
            }

            "jpg", "jpeg" -> {
                FileExtension.JPEG
            }

            else -> FileExtension.PNG
        }
    }
}