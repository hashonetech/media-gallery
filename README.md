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
	        implementation 'com.github.hashonetech:media-gallery:v1.0.3'
	}

In AndroidManifest.xml

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
	 
 Implementation

	if (checkPermissions()) {
            mActivityLauncher.launch(
                MediaGallery.open(activity = mActivity, MediaGallery.build(
                    mediaType = MediaType.IMAGE,
                    mediaCount = 1,
                    allowMultiMedia = false,
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
                    backPressIcon = R.drawable.ic_back
                    backPressIconDescription = ""
                    toolBarTitle = ""
                    toolBarTitleColor = R.color.black
                    toolBarTitleFont = R.font.roboto_medium
                    toolBarTitleSize = 16F
                    //TODO: Camera Icon
                    cameraIcon = R.drawable.ic_camera_media_gallery
                    //TODO: Google Photos Icon
                    googlePhotosIcon = R.drawable.ic_google_photos_media_gallery
                    //TODO: Bucket Contents
                    bucketTitleColor = com.hashone.commons.R.color.pure_black
                    bucketTitleFont = com.hashone.commons.R.font.roboto_medium
                    bucketTitleSize = 16F
                    bucketSubTitleColor = com.hashone.commons.R.color.pure_black
                    bucketSubTitleFont = com.hashone.commons.R.font.roboto_regular
                    bucketSubTitleSize = 14F
                    selectedCountBackgroundColor = com.hashone.commons.R.color.pure_black
                    selectedCountColor = com.hashone.commons.R.color.pure_black
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
                                    val filePath = intent.getStringExtra(KEY_MEDIA_PATH)
                                    Toast.makeText(
                                        mActivity,
                                        "Selected: $filePath",
                                        Toast.LENGTH_LONG
                                    ).show()
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
