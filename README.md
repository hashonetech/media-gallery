# media-gallery
Media gallery module used to Select Single or Multiple Photo, Video or Both, Capture Photo and Video and also allow to select photo and video from Google Photos application.

With gallery module integrated [Photo Crop](https://github.com/hashonetech/crop-module) feature, to allow crop photo in various ratio and shape size.

### Features
<!--ts-->
* Select Single Photo, Video or Both
* Capture Photo and Video
* Allow to select Photo and Video from Google Photos
* Integration of [Photo Crop](https://github.com/hashonetech/crop-module) with various ratio and shape size
* Support to navigate from module to other screen
* Video Selection validation limits
    * Bytes size
    * Duration
    * Resolution
* Supported Multiple languages including RTL languages
* Dynamic UI theme, resources and Color changes
<!--ts-->

[![](https://jitpack.io/v/hashonetech/media-gallery.svg)](https://jitpack.io/#hashonetech/media-gallery)

```gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
	dependencies {
	        implementation 'com.github.hashonetech:media-gallery:v1.0.47'
	}
 ```

## 📸 Screenshot

<div style="display:flex;">
 <img alt="App image" src="https://github.com/hashonetech/media-gallery/blob/master/screenshots/Screenshot_1.png" width="30%"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; 
 <img alt="App image" src="https://github.com/hashonetech/media-gallery/blob/master/screenshots/Screenshot_2.png" width="30%"> &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; 
 <img alt="App image" src="https://github.com/hashonetech/media-gallery/blob/master/screenshots/Screenshot_3.png" width="30%">
</div>


Table of contents
=================

<!--ts-->
* [AndroidManifest](#androidmanifest)
* [Open Gallery](#open-gallery)
* [Old Crop](#old-crop)
* [New Crop](#new-crop)
* [Screen Navigation](#screen-navigation)
<!--te-->

## AndroidManifest

```xml
//TODO: When allow to use camera feature
<uses-feature
    android:name="android.hardware.camera"
    android:required="false" />
<queries>
    <intent>
        <action android:name="android.intent.action.GET_CONTENT" />
        //TODO: For Image Only
        <data android:mimeType="image/*" />

        //TODO: For Video Only
        <data android:mimeType="video/*" />

        //TODO: For Image and Video both
        <data android:mimeType="*/*" />
    </intent>
    <intent>
        <action android:name="android.intent.action.MAIN" />
    </intent>
</queries>

//TODO: Below 33 SDK version
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
//TODO: 33 and above SDK version
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
//TODO: 33 and above SDK version
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
//TODO: Allow only when use Camera feature
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.any" />

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
```

## Open Gallery

   ```kotlin
	if (checkPermissions()) {
            mActivityLauncher.launch(
                MediaGallery.open(activity = mActivity, MediaGallery.build(
                    mediaType = when (requestCode) {
                        REQUEST_CODE_IMAGE -> MediaType.IMAGE
                        REQUEST_CODE_VIDEO -> MediaType.VIDEO
                        else -> MediaType.IMAGE_VIDEO
                    },
   		//TODO: How much media file you want to choose
                    mediaCount = mediaCount,
   		//TODO: if you want to show camear option
                    allowCamera = true/false,
                    allowGooglePhotos = true/false,
   		//TODO: if you want to show all midea file folder 
                    allowAllMedia = true/false,
   		//TODO: if you want to start new crop module
                    enableCropMode = true/false,
                    mediaGridCount = 3,
		 	allMediaTitle = "All",
			corruptedMediaMessage = "Corrupted media.",

      		//TODO video Duration Limit in second
                    videoValidationBuilder = MediaGallery.VideoValidationBuilder(
                        checkValidation = true/false,
                        //TODO video Duration Limit in second
                        checkDuration = true/false,
                        durationLimit = 30, //TODO: In Seconds
                        durationLimitMessage = getString(R.string.duration_error),
                        //TODO video Size Limit in MB
                        checkFileSize = true/false,
                        sizeLimit = 100, //TODO: In MB
                        sizeLimitMessage = getString(R.string.file_size_error),
                        //TODO video Resolution Size Limit px
                        checkResolution = true/false,
                        maxResolution = 1920, //TODO: In Pixel
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
                    supportedMediaTypes = arrayListOf<SupportedMediaType>().apply {
			//TODO: If this list isEmpty - It will assign Default List of MediaTypes Listed Below
                        //TODO: Photo MediaTypes
                        add(SupportedMediaType.TYPE_PNG)
                        add(SupportedMediaType.TYPE_JPG)
                        add(SupportedMediaType.TYPE_JPEG)
                        add(SupportedMediaType.TYPE_WEBP)
                        add(SupportedMediaType.TYPE_GIF)
                        //TODO: Video MediaTypes
                        add(SupportedMediaType.TYPE_3G2)
                        add(SupportedMediaType.TYPE_3GP)
                        add(SupportedMediaType.TYPE_MP4)
                        add(SupportedMediaType.TYPE_AVI)
                        add(SupportedMediaType.TYPE_FLV)
                        add(SupportedMediaType.TYPE_MKV)
                        add(SupportedMediaType.TYPE_MOV)
                        add(SupportedMediaType.TYPE_MPG)
                        add(SupportedMediaType.TYPE_WEBM)
                        add(SupportedMediaType.TYPE_WMV)
                    }
                ) {
                    //TODO: Screen
	            screenBuilder = MediaGallery.ScreenBuilder(
	                isFullScreen = true/false,
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
                        positiveIsCap = true/false,
                        negativeText = getString(R.string.label_cancel),
                        negativeColor = com.hashone.commons.R.color.black,
                        negativeFont = com.hashone.commons.R.font.roboto_regular,
                        negativeSize = 16F,
                        negativeIsCap = true/false,
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

			//TODO: Bucket Loading
                    bucketProgressDialogBuilder = MediaGallery.BucketProgressDialogBuilder(
                        loadingMessage = "Loading photos.",
                        loadingLongTimeMessage = "It is taking bit long.",
                        loadingMoreTimeMessage = "Looks like you have too many photos!",
                        messageColor = com.hashone.commons.R.color.pure_black,
                        messageFont = com.hashone.commons.R.font.roboto_medium,
                        messageSize = 16F,
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
	
	            //TODO: If you want to close gallery after selection media file
	            isForceClose = true/false
	
	            //TODO: Bellow Builder pass only if you want to start any next activity after media select
	            mediaCropBuilder = MediaGallery.MediaCropBuilder(
	                    appPackageName = packageName,
	                    cropClassName = "OldCropActivity",
	                    projectDirectoryPath = getInternalFileDir(this@MainActivity).absolutePath
	                )
	            
                }),
                onActivityResult = object : BetterActivityResult.OnActivityResult<ActivityResult> {
                    override fun onActivityResult(activityResult: ActivityResult) {
                        if (activityResult.resultCode == Activity.RESULT_OK) {
                            activityResult.data?.let { intent ->
	            		//TODO: Returns Selected Media files data
                                if (intent.hasExtra(KEY_MEDIA_PATHS)) {
	                                val selectedMedia: ArrayList<MediaItem>? =
	                                    intent.serializable(KEY_MEDIA_PATHS)
	                                selectedMedia?.let {
	                                    mBinding.videoPlayIcon.isVisible = isVideoFile(selectedMedia[0].path)
	                                    Glide.with(this@MainActivity).load(selectedMedia[0].path).centerCrop()
	                                        .into(mBinding.cropedImage)
	                                }
	                            }
	            		//TODO: Returns New Crop module cropped Image file data
   				if (intent.hasExtra(CropActivity.KEY_RETURN_CROP_DATA)) {
	                                val myCropDataSaved =
	                                    intent.extras?.serializable<CropDataSaved>(
	                                        CropActivity.KEY_RETURN_CROP_DATA
	                                    )
	                                Glide.with(this@MainActivity).load(myCropDataSaved!!.cropImg).centerCrop()
	                                    .into(mBinding.cropedImage)
	                            }
                                }
                            }
                        }
                    }
                }
            )
        }
```

## Old Crop

   ```kotlin
	enableCropMode = false
	isForceClose = true
	mediaCropBuilder = MediaGallery.MediaCropBuilder(
	                    appPackageName = packageName,
	                    cropClassName = "OldCropActivity",
	                    projectDirectoryPath = getInternalFileDir(this@MainActivity).absolutePath
	                )

   ```
	
## New Crop
Github - [crop-module](https://github.com/hashonetech/crop-module)
   ```kotlin
	enableCropMode = true
	isForceClose = true
   ```
	
## Screen Navigation

   ```kotlin
 //TODO: Bellow Builder pass only if you want to start any next activity after media select
mediaCropBuilder = MediaGallery.MediaCropBuilder(
		appPackageName = packageName,
		cropClassName = "OldCropActivity",
		projectDirectoryPath = getInternalFileDir(this@MainActivity).absolutePath
)
   ```
		 
### License


```
Copyright 2024 Hashone Tech LLP

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements. See the NOTICE file distributed with this work for
additional information regarding copyright ownership. The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```

