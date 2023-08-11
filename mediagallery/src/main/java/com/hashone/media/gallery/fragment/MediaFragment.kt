package com.hashone.media.gallery.fragment

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hashone.commons.base.CoroutineAsyncTask
import com.hashone.commons.extensions.getLocaleString
import com.hashone.media.gallery.MediaActivity
import com.hashone.media.gallery.R
import com.hashone.media.gallery.adapters.MediaAdapter
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.callback.OnSelectionChangeListener
import com.hashone.media.gallery.databinding.DialogConfirmationBinding
import com.hashone.media.gallery.databinding.FragmentImagesBinding
import com.hashone.media.gallery.model.MediaItem
import com.hashone.media.gallery.model.fetchMediaAsync
import com.hashone.media.gallery.utils.KEY_BUCKET_ID
import com.hashone.media.gallery.utils.KEY_BUCKET_NAME
import com.hashone.media.gallery.utils.KEY_BUCKET_PATH
import com.hashone.media.gallery.utils.byteToMB
import com.hashone.media.gallery.utils.getVideoWidthHeight

class MediaFragment : Fragment() {

    private lateinit var mActivity: Activity
    private lateinit var mBinding: FragmentImagesBinding
    private lateinit var builder: MediaGallery.Builder

    private var mBucketId: Long = -1L
    private var mFolderName: String = ""
    private var mFolderPath: String = ""

    private val mMediaList = ArrayList<MediaItem>()

    private var mIsHandled: Int = 0
    private val mHandlerLoadingWait = Handler(Looper.getMainLooper())
    private val mRunnableLoadingWait =
        Runnable {
            mBinding.textViewProgressMessage.text =
                getLocaleString(R.string.photos_taking_long_time)
            mIsHandled = 1
            mHandlerLoadingWait.postDelayed(mRunnableLoadingWait1, 7 * 1000L)
        }
    private val mRunnableLoadingWait1 =
        Runnable {
            mIsHandled = 2
            mBinding.textViewProgressMessage.text =
                getLocaleString(R.string.photos_taking_more_time)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentImagesBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = requireActivity()
        builder = (mActivity as MediaActivity).builder

        //mBinding.layoutContentLoading.visibility = View.VISIBLE
        //mHandlerLoadingWait.postDelayed(mRunnableLoadingWait, 3 * 1000L)

        initViews()
    }

    private fun initViews() {
        try {
            requireArguments().let {
                mBucketId = it.getLong(KEY_BUCKET_ID, -1L)
                mFolderName = it.getString(KEY_BUCKET_NAME, "")
                mFolderPath = it.getString(KEY_BUCKET_PATH, "")
            }
            (mActivity as MediaActivity).updateTitle(mFolderName)
            (mActivity as MediaActivity).updateGooglePhotosUI(false)

            val mediaPref = (mActivity as MediaActivity).mMediaPref.getMediaByBucketId(mBucketId)
            mMediaList.addAll(mediaPref)
            setAdapter()
            if (mMediaList.isEmpty()) {
                LoadMediaTask().execute()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class LoadMediaTask : CoroutineAsyncTask<Void, Void, Void>() {
        override fun onPreExecute() {
            super.onPreExecute()
            mBinding.layoutContentLoading.visibility = View.VISIBLE
            mHandlerLoadingWait.postDelayed(mRunnableLoadingWait, 3 * 1000L)
            mMediaList.clear()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                val mediaPref =
                    (mActivity as MediaActivity).mMediaPref.getMediaByBucketId(mBucketId)
                mMediaList.addAll(
                    mediaPref.ifEmpty {
                        fetchMediaAsync(
                            mActivity,
                            mediaType = builder.mediaType,
                            mBucketId
                        )
                    }
                )
                if (mediaPref.isEmpty())
                    (mActivity as MediaActivity).mMediaPref.storeMediaToPref(mBucketId, mMediaList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            try {
                if (mIsHandled == 0)
                    mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait)
                else if (mIsHandled == 1)
                    mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait1)
                mBinding.layoutContentLoading.visibility = View.GONE
                setAdapter()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setAdapter() {
        try {
            mBinding.recyclerViewImages.apply {
                layoutManager =
                    GridLayoutManager(
                        mActivity,
                        builder.mediaGridCount,
                        RecyclerView.VERTICAL,
                        false
                    )
                (mActivity as MediaActivity).let {
                    val imageAdapter = MediaAdapter(
                        mActivity,
                        mMediaList,
                        builder = builder,
                        mIsMultipleMode = it.builder.allowMultiMedia,
                        mMaxSize = builder.mediaCount,
                        mOnItemClickListener = { _, _, _, _ -> },
                        mOnSelectionChangeListener = object : OnSelectionChangeListener {
                            override fun onSelectedImagesChanged(selectedImages: ArrayList<MediaItem>) {
                                try {
                                    it.updateHeaderOptionsUI(selectedImages.size > 0)
                                    it.updateMediaCount()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onSingleModeImageSelected(imageItem: MediaItem) {
                                try {
                                    it.finishPickImages(arrayListOf(imageItem))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onNotValidVideo(imageItem: MediaItem) {
                                val videoWidthHeight = getVideoWidthHeight(imageItem.path , imageItem.mediaResolution)
                                val width = videoWidthHeight.first
                                val height = videoWidthHeight.second
                                val message =
                                    if (java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(
                                            imageItem.mediaDuration
                                        ) > builder.videoValidationBuilder.durationLimit
                                    ) {
                                        builder.videoValidationBuilder.durationLimitMessage
                                    } else if (byteToMB(imageItem.mediaSize) > builder.videoValidationBuilder.sizeLimit) {
                                        builder.videoValidationBuilder.sizeLimitMessage
                                    } else if (width > builder.videoValidationBuilder.maxResolution || height > builder.videoValidationBuilder.maxResolution) {
                                        builder.videoValidationBuilder.maxResolutionMessage
                                    } else {
                                        "${builder.videoValidationBuilder.durationLimitMessage}\n${builder.videoValidationBuilder.sizeLimitMessage}\n${builder.videoValidationBuilder.maxResolutionMessage}"
                                    }
                                showCustomAlertDialog(title = message,
                                    positionButtonText = builder.videoValidationBuilder.videoValidationDialogBuilder.positiveText,
                                    positiveCallback = {
                                        alertDialog?.cancel()
                                    },
                                    onDismissListener = {},
                                    onCancelListener = {})
                            }
                        }
                    )

                    setHasFixedSize(true)
                    adapter = imageAdapter
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var alertDialog: AlertDialog? = null
    fun showCustomAlertDialog(
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
                DialogConfirmationBinding.inflate(LayoutInflater.from(mActivity), null, false)
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

    override fun onDestroyView() {
        (mActivity as MediaActivity).updateTitle(getLocaleString(R.string.label_gallery))
        (mActivity as MediaActivity).updateGooglePhotosUI(true)
        if (mIsHandled == 0)
            mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait)
        else if (mIsHandled == 1)
            mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait1)
        super.onDestroyView()
    }
}