# media-gallery
Media Gallery module, used to select Photo, Video, Capture Photo or Video from Camera.

[![](https://jitpack.io/v/hashonetech/media-gallery.svg)](https://jitpack.io/#hashonetech/media-gallery)

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
	dependencies {
	        implementation 'com.github.hashonetech:media-gallery:Tag'
	}
 
## 📸 Screenshot

<div style="display:flex;">
 <img alt="App image" src="https://github.com/hashonetech/media-gallery/assets/104345897/e752f7c9-b0da-4e10-a982-53f230ed47c0" width="30%"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; 
 <img alt="App image" src="https://github.com/hashonetech/media-gallery/assets/104345897/ef7c2008-e7be-43e8-ae54-1de2f6f7b3f6" width="30%"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; 
 <img alt="App image" src="https://github.com/hashonetech/media-gallery/assets/104345897/338277c0-ca58-4b74-98ee-31e04ce32ec7" width="30%">
</div>

### AndroidManifest.xml 

    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />
    <queries>
        <intent>
            <action android:name="android.intent.action.GET_CONTENT" />

            <data android:mimeType="*/*" />
        </intent>
        <intent>
            <action android:name="android.intent.action.MAIN" />
        </intent>
    </queries>

    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.CAMERA" />

	<application
		...
		tools:replace="android:theme,android:name">
		...

		<provider
		    android:name="androidx.core.content.FileProvider"
		    android:authorities="${applicationId}.provider"
		    ...
		    tools:replace="android:resource"
		    ...>
		</provider>
	 </application>
	 
## Implementation

	if (checkPermissions()) {
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
                    enableCropMode = mBinding.switchIsCrop.isChecked,
                    mediaGridCount = 3
                ) {
                    //TODO: Screen
	            screenBuilder = MediaGallery.ScreenBuilder(
	                isFullScreen = false,
	                windowBackgroundColor = com.hashone.media.gallery.test.R.color.white,
	                statusBarColor = com.hashone.media.gallery.test.R.color.white,
	                navigationBarColor = com.hashone.media.gallery.test.R.color.white,
	                //TODO: Google Photos Icon
	                googlePhotosIcon = R.drawable.ic_google_photos_media_gallery
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
                    	cameraIcon = R.drawable.ic_camera_media_gallery
	            )
	
	            //TODO: Warning Ui
	            warningUiBuilder = MediaGallery.WarningUiBuilder(
	                message = getLocaleString(com.hashone.commons.R.string.allow_permission),
	                settingText = getLocaleString(R.string.setting_text),
	            )
	
	            //TODO: Permission
	            permissionBuilder = MediaGallery.PermissionBuilder(
	                message = getLocaleString(com.hashone.commons.R.string.allow_permission),
	                positiveText = getLocaleString(R.string.label_grant),
	                negativeText = getLocaleString(R.string.label_cancel),
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
                    override fun onActivityResult(activityResult: ActivityResult) {
                        if (activityResult.resultCode == Activity.RESULT_OK) {
                            activityResult.data?.let { intent ->
                                if (intent.hasExtra(KEY_MEDIA_PATHS)){
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
                                    if (intent.hasExtra(CropActivity.KEY_RETURN_CROP_DATA)) {
                                        val myCropDataSaved =
                                            intent.extras?.serializable<CropDataSaved>(
                                                CropActivity.KEY_RETURN_CROP_DATA
                                            )
                                        Glide.with(this@MainActivity).load(myCropDataSaved!!.cropImg)
                                            .into(mBinding.cropedImage)
                                    } else {
                                        val filePath =
                                            intent.extras!!.getString(KEY_IMAGE_PATH)!!
                                        val originalImagePath =
                                            intent.extras!!.getString(KEY_IMAGE_ORIGINAL_PATH)!!
                                        Glide.with(this@MainActivity).load(filePath)
                                            .into(mBinding.cropedImage)
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
	
