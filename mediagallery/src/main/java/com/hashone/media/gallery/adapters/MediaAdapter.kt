package com.hashone.media.gallery.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.hashone.commons.extensions.getColorCode
import com.hashone.commons.extensions.getScreenWidth
import com.hashone.commons.extensions.onClick
import com.hashone.media.gallery.MediaActivity
import com.hashone.media.gallery.R
import com.hashone.media.gallery.callback.OnSelectionChangeListener
import com.hashone.media.gallery.databinding.GalleryItemImageBinding
import com.hashone.media.gallery.enums.MediaType
import com.hashone.media.gallery.model.MediaItem
import com.hashone.media.gallery.utils.ACTION_UPDATE_FOLDER_COUNT
import com.hashone.media.gallery.utils.KEY_BUCKET_ID

class MediaAdapter(
    var mContext: Context,
    private var mImagesList: ArrayList<MediaItem>,
    private var mIsMultipleMode: Boolean = false,
    var mMaxSize: Int = 1,
    var mOnItemClickListener: AdapterView.OnItemClickListener?,
    var mOnSelectionChangeListener: OnSelectionChangeListener?
) : RecyclerView.Adapter<MediaAdapter.ItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
        ItemViewHolder(
            GalleryItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) = run {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            when {
                payloads.any { it is ImageSelectedOrUpdated } -> {
                    with(holder) {
                        with(mImagesList[position]) {
                            if (mIsMultipleMode) {
                                val selectedIndex = (mContext as MediaActivity).selectedIndex(this)
                                if (selectedIndex != -1) {
                                    mBinding.textViewImageCount.text = "${selectedIndex + 1}"
                                    mBinding.textViewImageCount.isVisible = true
                                    setupItemForeground(mBinding.imageViewImageItem, true)
                                } else {
                                    mBinding.textViewImageCount.isVisible = false
                                    setupItemForeground(mBinding.imageViewImageItem, false)
                                }
                            } else {
                                mBinding.textViewImageCount.isVisible = false
                                setupItemForeground(mBinding.imageViewImageItem, false)
                            }
                        }
                    }
                }

                payloads.any { it is ImageUnselected } -> {
                    with(holder) {
                        if (mIsMultipleMode) mBinding.textViewImageCount.isVisible = false
                        setupItemForeground(mBinding.imageViewImageItem, false)
                    }
                }

                else -> {
                    onBindViewHolder(holder, position)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) = run {
        try {
            with(holder) {
                with(mImagesList[position]) {
                    val selectedIndex = (mContext as MediaActivity).selectedIndex(this)
                    val isSelected = mIsMultipleMode && selectedIndex != -1
                    Glide.with(mContext)
                        .load(this.path)
                        .apply(
                            RequestOptions().centerCrop().dontAnimate()
                                .format(DecodeFormat.PREFER_RGB_565)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .dontAnimate()
                                .dontTransform()
                                .downsample(DownsampleStrategy.CENTER_INSIDE)
                        )
                        .into(mBinding.imageViewImageItem)

                    setupItemForeground(mBinding.imageViewImageItem, isSelected)
                    mBinding.textViewImageCount.isVisible = isSelected && mIsMultipleMode
                    if (mBinding.textViewImageCount.isVisible) {
                        mBinding.textViewImageCount.text = "${selectedIndex + 1}"
                    }

                    mBinding.layoutVideoDetails.isVisible = if (this.mediaType == MediaType.VIDEO) {
                        mBinding.textViewVideoDuration.text = String.format(
                            "%02d:%02d",
                            java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(this.mediaDuration),
                            java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(this.mediaDuration) -
                                    java.util.concurrent.TimeUnit.MINUTES.toSeconds(
                                        java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(
                                            this.mediaDuration
                                        )
                                    )
                        )
                        true
                    } else {
                        false
                    }

                    mBinding.root.onClick {
                        selectOrRemoveImage(this, position)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupItemForeground(view: View, isSelected: Boolean) {
        view.foreground = if (isSelected) ColorDrawable(
            mContext.getColorCode(
                R.color.imagepicker_black_alpha_30
            )
        ) else null
    }

    override fun getItemCount(): Int {
        return mImagesList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun selectOrRemoveImage(image: MediaItem, position: Int) {
        if (mIsMultipleMode) {
            val selectedIndex = (mContext as MediaActivity).selectedIndex(image)
            if (selectedIndex != -1) {
                (mContext as MediaActivity).removeItem(selectedIndex)
                notifyItemChanged(position, ImageUnselected())
                notifyDataSetChanged()

                mContext.sendBroadcast(Intent().apply {
                    action = ACTION_UPDATE_FOLDER_COUNT
                    putExtra(KEY_BUCKET_ID, image.bucketId)
                    putExtra("add", false)
                })
            } else {
                if ((mContext as MediaActivity).mSelectedImagesList.size >= mMaxSize) {
                    return
                } else {
                    (mContext as MediaActivity).addItem(image)
                    notifyItemChanged(position, ImageSelectedOrUpdated())
                    mContext.sendBroadcast(Intent().apply {
                        action = ACTION_UPDATE_FOLDER_COUNT
                        putExtra(KEY_BUCKET_ID, image.bucketId)
                        putExtra("add", true)
                    })
                }
            }
            if (mOnSelectionChangeListener != null)
                mOnSelectionChangeListener!!.onSelectedImagesChanged((mContext as MediaActivity).mSelectedImagesList)
        } else {
            if (mOnSelectionChangeListener != null)
                mOnSelectionChangeListener!!.onSingleModeImageSelected(image)
        }
    }

    inner class ItemViewHolder(binding: GalleryItemImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val mBinding = GalleryItemImageBinding.bind(binding.root)
    }

    class ImageSelectedOrUpdated

    class ImageUnselected
}