package com.hashone.media.gallery

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.hashone.media.gallery.test.databinding.ActivityOldCropBinding
import com.hashone.media.gallery.utils.KEY_CROP_DESTINATION_PATH
import com.hashone.media.gallery.utils.KEY_CROP_FILET
import com.hashone.media.gallery.utils.KEY_CROP_PROJECT_DIRECTORY
import com.hashone.media.gallery.utils.KEY_CROP_URI
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_PATH
import com.hashone.media.gallery.utils.KEY_IMAGE_ORIGINAL_REPLACE
import com.hashone.media.gallery.utils.KEY_IMAGE_PATH

class OldCropActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityOldCropBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_old_crop)
        mBinding = ActivityOldCropBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val uriPath = intent.extras!!.getString(KEY_CROP_URI)!!
        val filePath = intent.extras!!.getString(KEY_CROP_FILET)!!
        val destinationPath = intent.extras!!.getString(KEY_CROP_DESTINATION_PATH)!!
        val projectDirectoryPath = intent.extras!!.getString(KEY_CROP_PROJECT_DIRECTORY)!!
        mBinding.cropTextView.text = "uriPath:$uriPath \nfilePath:$filePath \ndestinationPath:$destinationPath \nprojectDirectoryPath:$projectDirectoryPath"

        Glide.with(this@OldCropActivity).load(filePath).into(mBinding.cropedImage)

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