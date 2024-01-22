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
import com.hashone.media.gallery.MediaActivity
import com.hashone.media.gallery.R
import com.hashone.media.gallery.adapters.MediaAdapter
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.callback.OnSelectionChangeListener
import com.hashone.media.gallery.databinding.DialogWarningBinding
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
            //TODO: Language translation require
            mBinding.textViewProgressMessage.text =
                builder.bucketProgressDialogBuilder.loadingLongTimeMessage.ifEmpty {
                    getString(R.string.media_gallery_photos_taking_long_time)
                }
            mIsHandled = 1
            mHandlerLoadingWait.postDelayed(mRunnableLoadingWait1, 7 * 1000L)
        }
    private val mRunnableLoadingWait1 =
        Runnable {
            mIsHandled = 2
            //TODO: Language translation require
            mBinding.textViewProgressMessage.text =
                builder.bucketProgressDialogBuilder.loadingMoreTimeMessage.ifEmpty {
                    getString(R.string.media_gallery_photos_taking_more_time)
                }
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

        initViews()
        setupLoadingUI()
    }

    private fun setupLoadingUI() {
        mBinding.textViewProgressMessage.text =
            builder.bucketProgressDialogBuilder.loadingMessage.ifEmpty {
                getString(R.string.media_gallery_loading_photos)
            }

        mBinding.textViewProgressMessage.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            builder.bucketProgressDialogBuilder.messageSize
        )
        mBinding.textViewProgressMessage.typeface = ResourcesCompat.getFont(
            mActivity,
            builder.bucketProgressDialogBuilder.messageFont
        )
        mBinding.textViewProgressMessage.setTextColor(
            ContextCompat.getColor(
                mActivity,
                builder.bucketProgressDialogBuilder.messageColor
            )
        )
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
                            mediaBuilder = builder,
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
                mBinding.layoutContentLoading.visibility = View.GONE
                if (mIsHandled == 0)
                    mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait)
                else if (mIsHandled == 1)
                    mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait1)
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
                    mBinding.layoutContentLoading.isVisible = (mMediaList.size == 0)
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

                            override fun onNotValidVideo(
                                imageItem: MediaItem,
                                position: Int
                            ) {
                                val videoWidthHeight =
                                    getVideoWidthHeight(imageItem.path, imageItem.mediaResolution)
                                val width = videoWidthHeight.first
                                val height = videoWidthHeight.second
                                var message = ""
                                var isLargeResolution = false
                                var dialogBuilder =
                                    builder.videoValidationBuilder.durationDialogBuilder
                                if (builder.videoValidationBuilder.checkDuration && (java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(
                                        imageItem.mediaDuration
                                    ) > builder.videoValidationBuilder.durationLimit)
                                ) {
                                    dialogBuilder =
                                        builder.videoValidationBuilder.durationDialogBuilder
                                    message =
                                        builder.videoValidationBuilder.durationLimitMessage.ifEmpty {
                                            getString(R.string.media_gallery_duration_error)
                                        }
                                } else if (builder.videoValidationBuilder.checkFileSize && (byteToMB(
                                        imageItem.mediaSize
                                    ) > builder.videoValidationBuilder.sizeLimit)
                                ) {
                                    dialogBuilder = builder.videoValidationBuilder.sizeDialogBuilder
                                    message =
                                        builder.videoValidationBuilder.sizeLimitMessage.ifEmpty {
                                            getString(R.string.media_gallery_file_size_error)
                                        }
                                } else if (builder.videoValidationBuilder.checkResolution && (width > builder.videoValidationBuilder.maxResolution || height > builder.videoValidationBuilder.maxResolution)) {
                                    isLargeResolution = true
                                    dialogBuilder =
                                        builder.videoValidationBuilder.resolutionDialogBuilder
                                    message =
                                        builder.videoValidationBuilder.maxResolutionMessage.ifEmpty {
                                            getString(R.string.media_gallery_size_error)
                                        }
                                } else {
                                    "${builder.videoValidationBuilder.durationLimitMessage.ifEmpty { 
                                        getString(R.string.media_gallery_duration_error)
                                    }}\n${builder.videoValidationBuilder.sizeLimitMessage.ifEmpty { 
                                        getString(R.string.media_gallery_file_size_error)
                                    }}\n${builder.videoValidationBuilder.maxResolutionMessage.ifEmpty { 
                                        getString(R.string.media_gallery_size_error)
                                    }}"
                                }

                                if (message.isNotEmpty()) {
                                    showWarningDialog(
                                        title = message,
                                        positionButtonText = dialogBuilder.positiveText.ifEmpty {
                                            getString(R.string.media_gallery_okay)
                                        },
                                        negativeButtonText = if (isLargeResolution) dialogBuilder.negativeText else "",
                                        dialogBuilder,
                                        positiveCallback = {
                                            alertDialog?.cancel()
                                        },
                                        negativeCallback = {
                                            alertDialog?.cancel()
                                            if (builder.mediaCount > 1) {
                                                if (mBinding.recyclerViewImages.adapter != null) {
                                                    if (isSelected && (mActivity as MediaActivity).mSelectedImagesList.size >= builder.mediaCount) {
                                                        mBinding.recyclerViewImages.adapter!!.notifyDataSetChanged()
                                                    } else {
                                                        (mBinding.recyclerViewImages.adapter!! as MediaAdapter).selectOrRemoveImage(
                                                            imageItem,
                                                            position
                                                        )
                                                    }
                                                }
                                            } else {
                                                (mActivity as MediaActivity).finishPickImages(
                                                    arrayListOf(imageItem)
                                                )
                                            }
                                        },
                                        onDismissListener = {},
                                        onCancelListener = {},
                                    )
                                }
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
    fun showWarningDialog(
        title: String = "",
        positionButtonText: String = "",
        negativeButtonText: String = "",
        mDialogBuilder: MediaGallery.VideoValidationDialogBuilder = MediaGallery.VideoValidationDialogBuilder(),
        isCancelable: Boolean = true,
        positiveCallback: View.OnClickListener? = null,
        negativeCallback: View.OnClickListener? = null,
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
            dialogBinding.textViewConfirm.text = negativeButtonText
            dialogBinding.textViewTitle.isVisible = title.isNotEmpty()
            dialogBinding.textViewYes.isVisible = positionButtonText.isNotEmpty()
            dialogBinding.textViewConfirm.isVisible = negativeButtonText.isNotEmpty()
            dialogBinding.view3.isVisible = negativeButtonText.isNotEmpty()

            dialogBinding.textViewTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                mDialogBuilder.titleSize
            )
            dialogBinding.textViewTitle.setTextColor(
                ContextCompat.getColor(
                    mActivity,
                    mDialogBuilder.titleColor
                )
            )
            dialogBinding.textViewTitle.typeface = ResourcesCompat.getFont(
                mActivity,
                mDialogBuilder.titleFont
            )

            dialogBinding.textViewYes.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                mDialogBuilder.positiveSize
            )
            dialogBinding.textViewYes.setTextColor(
                ContextCompat.getColor(
                    mActivity,
                    mDialogBuilder.positiveColor
                )
            )
            dialogBinding.textViewYes.typeface = ResourcesCompat.getFont(
                mActivity,
                mDialogBuilder.positiveFont
            )

            dialogBinding.textViewConfirm.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                mDialogBuilder.negativeSize
            )
            dialogBinding.textViewConfirm.setTextColor(
                ContextCompat.getColor(
                    mActivity,
                    mDialogBuilder.negativeColor
                )
            )
            dialogBinding.textViewConfirm.typeface = ResourcesCompat.getFont(
                mActivity,
                mDialogBuilder.negativeFont
            )

            dialogBuilder.setView(dialogBinding.root)
            alertDialog = dialogBuilder.create()
            if (!mActivity.isDestroyed) if (alertDialog != null && !alertDialog!!.isShowing) alertDialog!!.show()
            alertDialog!!.setCancelable(isCancelable)
            dialogBinding.textViewYes.setOnClickListener(positiveCallback)
            dialogBinding.textViewConfirm.setOnClickListener(negativeCallback)
            alertDialog!!.setOnDismissListener(onDismissListener)
            alertDialog!!.setOnCancelListener(onCancelListener)
            alertDialog!!.setOnKeyListener(keyEventCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        (mActivity as MediaActivity).updateTitle(getString(R.string.media_gallery_label_gallery))
        (mActivity as MediaActivity).updateGooglePhotosUI(true)
        if (mIsHandled == 0)
            mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait)
        else if (mIsHandled == 1)
            mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait1)
        super.onDestroyView()
    }
}