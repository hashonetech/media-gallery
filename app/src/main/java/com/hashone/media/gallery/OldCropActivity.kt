package com.hashone.media.gallery

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.hashone.commons.extensions.serializable
import com.hashone.media.gallery.model.MediaItem
import com.hashone.media.gallery.test.databinding.ActivityOldCropBinding
import com.hashone.media.gallery.utils.KEY_CROP_DESTINATION_PATH
import com.hashone.media.gallery.utils.KEY_CROP_FILET
import com.hashone.media.gallery.utils.KEY_CROP_PROJECT_DIRECTORY
import com.hashone.media.gallery.utils.KEY_CROP_URI
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_PATH
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_REPLACE
import com.hashone.media.gallery.utils.KEY_IMAGE_PATH
import com.hashone.media.gallery.utils.KEY_MEDIA_PATHS

class OldCropActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityOldCropBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_old_crop)
        mBinding = ActivityOldCropBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        if (intent.hasExtra(KEY_MEDIA_PATHS)) {
            val selectedMedia: ArrayList<MediaItem>? =
                intent.serializable(KEY_MEDIA_PATHS)
            selectedMedia?.let {
                Toast.makeText(
                    this,
                    "Selected: ${selectedMedia.size}",
                    Toast.LENGTH_LONG
                ).show()
                Glide.with(this).load(selectedMedia[0].path)
                    .into(mBinding.cropedImage)
            }

        val filePath = selectedMedia?.get(0)!!.path
        mBinding.cropTextView.text = "filePath:$filePath"

//        val uriPath = intent.extras!!.getString(KEY_CROP_URI)!!
//        val filePath = intent.extras!!.getString(KEY_CROP_FILET)!!
//        val destinationPath = intent.extras!!.getString(KEY_CROP_DESTINATION_PATH)!!
//        val projectDirectoryPath = intent.extras!!.getString(KEY_CROP_PROJECT_DIRECTORY)!!
//        mBinding.cropTextView.text = "uriPath:$uriPath \nfilePath:$filePath \ndestinationPath:$destinationPath \nprojectDirectoryPath:$projectDirectoryPath"
//
//        Glide.with(this@OldCropActivity).load(filePath).into(mBinding.cropedImage)

        mBinding.btnSubmit.setOnClickListener {
                setResult(
                    Activity.RESULT_OK,
                    Intent()
                        .putExtra(KEY_IMAGE_PATH, filePath)
                        .putExtra(KEY_IMAGE_ORIGINAL_PATH, filePath)
                        .putExtra(KEY_IMAGE_ORIGINAL_REPLACE, false)
                )
            finish()
        }
        }
    }
}