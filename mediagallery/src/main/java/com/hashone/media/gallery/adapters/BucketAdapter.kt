package com.hashone.media.gallery.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.hashone.commons.extensions.applyTextStyle
import com.hashone.commons.extensions.getColorCode
import com.hashone.commons.extensions.onClick
import com.hashone.media.gallery.R
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.databinding.GalleryItemFolderBinding
import com.hashone.media.gallery.model.MediaBucketData


class BucketAdapter(
    private var mContext: Context,
    private var builder: MediaGallery.Builder,
    private var mFoldersList: ArrayList<MediaBucketData>,
    private var mOnItemClickListener: AdapterView.OnItemClickListener? = null
) : RecyclerView.Adapter<BucketAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
        ItemViewHolder(
            GalleryItemFolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        try {
            with(holder) {
                with(mFoldersList[position]) {
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
                        .into(mBinding.imageViewFolderItem)

                    mBinding.listItem.setBackgroundColor(mContext.getColorCode(builder.bucketBuilder.backgroundColor))

                    mBinding.textViewFolderName.text =  if (this.bucketId == -1L) {
                        builder.allMediaTitle.ifEmpty {
                            mContext.getString(R.string.media_gallery_label_all)
                        }
                    } else {
                        this.name
                    }

                    mBinding.textViewFilesCount.text = "${this.mediaCount}"
                    mBinding.textViewSelectedCount.isVisible = this.selectedCount > 0
                    mBinding.textViewSelectedCount.text = "${this.selectedCount}"

                    mBinding.textViewFolderName.applyTextStyle(
                        mContext.getColorCode(builder.bucketBuilder.titleColor),
                        builder.bucketBuilder.titleFont,
                        builder.bucketBuilder.titleSize
                    )
                    mBinding.textViewFilesCount.applyTextStyle(
                        mContext.getColorCode(builder.bucketBuilder.subTitleColor),
                        builder.bucketBuilder.subTitleFont,
                        builder.bucketBuilder.subTitleSize
                    )
                    mBinding.textViewSelectedCount.applyTextStyle(
                        mContext.getColorCode(builder.bucketBuilder.countColor),
                        builder.bucketBuilder.countFont,
                        builder.bucketBuilder.countSize
                    )
                    mBinding.textViewSelectedCount.backgroundTintList =
                        ColorStateList.valueOf(mContext.getColorCode(builder.bucketBuilder.countBackgroundColor))

                    mBinding.root.onClick {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener!!.onItemClick(
                                null,
                                it,
                                position,
                                getItemId(position)
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        return mFoldersList.size
    }

    inner class ItemViewHolder(itemViewBinding: GalleryItemFolderBinding) :
        RecyclerView.ViewHolder(itemViewBinding.root) {
        val mBinding = GalleryItemFolderBinding.bind(itemViewBinding.root)
    }
}