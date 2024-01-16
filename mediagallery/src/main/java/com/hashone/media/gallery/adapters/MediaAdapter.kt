package com.hashone.media.gallery.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.hashone.commons.extensions.getColorCode
import com.hashone.commons.utils.showSnackBar
import com.hashone.media.gallery.MediaActivity
import com.hashone.media.gallery.R
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.callback.OnSelectionChangeListener
import com.hashone.media.gallery.databinding.GalleryItemImageBinding
import com.hashone.media.gallery.enums.MediaType
import com.hashone.media.gallery.model.MediaItem
import com.hashone.media.gallery.utils.ACTION_UPDATE_FOLDER_COUNT
import com.hashone.media.gallery.utils.KEY_BUCKET_ID
import com.hashone.media.gallery.utils.byteToMB
import com.hashone.media.gallery.utils.getVideoWidthHeight
import java.io.File
import java.util.concurrent.TimeUnit


class MediaAdapter(
    private var mContext: Context,
    private var mImagesList: ArrayList<MediaItem>,
    var builder: MediaGallery.Builder,
    private var mIsMultipleMode: Boolean = false,
    var mMaxSize: Int = 1,
    var mOnItemClickListener: AdapterView.OnItemClickListener?,
    var mOnSelectionChangeListener: OnSelectionChangeListener?
) : RecyclerView.Adapter<MediaAdapter.ItemViewHolder>() {

    private val corruptedMediaList = ArrayList<String>()

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
                                    if (builder.bucketBuilder.countBackgroundRes != -1)
                                        mBinding.textViewImageCount.setBackgroundResource(builder.bucketBuilder.countBackgroundRes)
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
                    var selectedIndex = (mContext as MediaActivity).selectedIndex(this)
                    var isSelected = mIsMultipleMode && selectedIndex != -1
                    var isLoadingFail = true
                    mBinding.root.isEnabled = false
                    if (!corruptedMediaList.contains(this.path)) {
                        Glide.with(mContext)
                            .load(Uri.fromFile(File(this.path)))
                            .apply(
                                RequestOptions().centerCrop().dontAnimate()
                                    .format(DecodeFormat.PREFER_RGB_565)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .dontAnimate()
                                    .dontTransform()
                                    .downsample(DownsampleStrategy.CENTER_INSIDE)
                            )
                            .error(R.drawable.ic_broken)
                            .listener(object : RequestListener<Drawable?> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable?>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    isLoadingFail = true
                                    mBinding.root.isEnabled = true
                                    corruptedMediaList.add(this@with.path)
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable?>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    isLoadingFail = false
                                    mBinding.root.isEnabled = true
                                    return false
                                }

                            })
                            .into(mBinding.imageViewImageItem)
                    } else {
                        mBinding.imageViewImageItem.setImageResource(R.drawable.ic_broken)
                    }

                    setupItemForeground(
                        mBinding.imageViewImageItem,
                        isSelected || (mContext as MediaActivity).mSelectedImagesList.size >= mMaxSize
                    )
                    mBinding.textViewImageCount.isVisible = isSelected && mIsMultipleMode
                    if (mBinding.textViewImageCount.isVisible) {
                        if (builder.bucketBuilder.countBackgroundRes != -1)
                            mBinding.textViewImageCount.setBackgroundResource(builder.bucketBuilder.countBackgroundRes)
                        mBinding.textViewImageCount.text = "${selectedIndex + 1}"
                    }

                    mBinding.layoutVideoDetails.isVisible = if (this.mediaType == MediaType.VIDEO) {
                        val hh = TimeUnit.MILLISECONDS.toHours(mediaDuration)
                        val mm =
                            TimeUnit.MILLISECONDS.toMinutes(mediaDuration) - TimeUnit.HOURS.toMinutes(
                                TimeUnit.MILLISECONDS.toHours(mediaDuration)
                            )
                        val ss =
                            TimeUnit.MILLISECONDS.toSeconds(mediaDuration) - TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(mediaDuration)
                            )
                        mBinding.textViewVideoDuration.text = if (hh.toInt() != 0) (String.format(
                            "%02d:%02d:%02d",
                            hh,
                            mm,
                            ss
                        )) else (String.format("%02d:%02d", mm, ss))
                        true
                    } else {
                        false
                    }
                    mBinding.root.setOnClickListener {
                        selectedIndex = (mContext as MediaActivity).selectedIndex(this)
                        isSelected = mIsMultipleMode && selectedIndex != -1
                        if (builder.videoValidationBuilder.checkValidation && this.mediaType != MediaType.IMAGE && (mContext as MediaActivity).mSelectedImagesList.size < mMaxSize && !isSelected) {
                            val videoWidthHeight =
                                getVideoWidthHeight(this.path, this.mediaResolution)
                            val width = videoWidthHeight.first
                            val height = videoWidthHeight.second
                            if (width > 0 && height > 0 && !isLoadingFail) {
                                if (TimeUnit.MILLISECONDS.toSeconds(this.mediaDuration) > builder.videoValidationBuilder.durationLimit || byteToMB(
                                        mediaSize
                                    ) > builder.videoValidationBuilder.sizeLimit || width > builder.videoValidationBuilder.maxResolution || height > builder.videoValidationBuilder.maxResolution
                                ) {
                                    mOnSelectionChangeListener!!.onNotValidVideo(this, position)
                                } else selectOrRemoveImage(this, position)
                            } else {
                                showSnackBar(mContext, mBinding.root, builder.corruptedMediaMessage.ifEmpty {
                                    mContext.getString(R.string.media_gallery_corrupted_media)
                                })
                            }
                        } else {
                            if (corruptedMediaList.contains(this.path)) {
                                showSnackBar(mContext, mBinding.root, builder.corruptedMediaMessage.ifEmpty {
                                    mContext.getString(R.string.media_gallery_corrupted_media)
                                })
                            } else {
                                selectOrRemoveImage(this, position)
                            }
                        }
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
    fun selectOrRemoveImage(image: MediaItem, position: Int) {
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
            if (mOnSelectionChangeListener != null) {
                mOnSelectionChangeListener!!.onSelectedImagesChanged((mContext as MediaActivity).mSelectedImagesList)
                if ((mContext as MediaActivity).mSelectedImagesList.size >= mMaxSize) {
                    notifyDataSetChanged()
                }
            }
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