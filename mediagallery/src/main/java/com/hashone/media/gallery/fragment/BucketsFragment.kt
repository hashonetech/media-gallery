package com.hashone.media.gallery.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hashone.commons.base.CoroutineAsyncTask
import com.hashone.commons.extensions.getLocaleString
import com.hashone.commons.extensions.registerBroadCastReceiver
import com.hashone.media.gallery.MediaActivity
import com.hashone.media.gallery.R
import com.hashone.media.gallery.adapters.BucketAdapter
import com.hashone.media.gallery.builder.MediaGallery
import com.hashone.media.gallery.databinding.GalleryFoldersBinding
import com.hashone.media.gallery.model.MediaBucketData
import com.hashone.media.gallery.model.fetchMediaBucketsAsync
import com.hashone.media.gallery.utils.ACTION_UPDATE_FOLDER_COUNT
import com.hashone.media.gallery.utils.KEY_BUCKET_ID
import com.hashone.media.gallery.utils.KEY_BUCKET_NAME
import com.hashone.media.gallery.utils.KEY_BUCKET_PATH

class BucketsFragment : Fragment() {

    private lateinit var mActivity: Activity
    private lateinit var mBinding: GalleryFoldersBinding

    private lateinit var builder: MediaGallery.Builder

    private val mBucketsList = ArrayList<MediaBucketData>()

    private var mFolderAdapter: BucketAdapter? = null

    private var mIsHandled: Int = 0
    private val mHandlerLoadingWait = Handler()
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

    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent != null) {
                    if (intent.action != null && intent.action == ACTION_UPDATE_FOLDER_COUNT) {
                        if (intent.extras != null) {
                            val bucketId = intent.extras!!.getLong(KEY_BUCKET_ID, -1L)
                            val add = intent.extras!!.getBoolean("add", false)

                            if (mFolderAdapter != null) {
                                for (i in 0 until mBucketsList.size) {
                                    if (mBucketsList[i].bucketId == bucketId) {
                                        mBucketsList[i].selectedCount =
                                            if (add) mBucketsList[i].selectedCount + 1 else mBucketsList[i].selectedCount - 1
                                        mFolderAdapter!!.notifyItemChanged(i)
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = GalleryFoldersBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity = requireActivity()
        builder = (mActivity as MediaActivity).builder

        initViews()
        mActivity.registerBroadCastReceiver(
            mBroadcastReceiver, IntentFilter().apply {
                addAction(ACTION_UPDATE_FOLDER_COUNT)
            }
        )
    }

    private fun initViews() {
        try {
            mBinding.layoutContentLoading.visibility = View.VISIBLE
            mHandlerLoadingWait.postDelayed(mRunnableLoadingWait, 3 * 1000L)

            LoadBucketsTask().execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class LoadBucketsTask : CoroutineAsyncTask<Void, Void, Void>() {
        override fun onPreExecute() {
            super.onPreExecute()
            mBucketsList.clear()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                mBucketsList.addAll(
                    fetchMediaBucketsAsync(
                        mActivity,
                        builder.mediaType,
                        builder.allowAllMedia
                    )
                )
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
            mBinding.recyclerViewFolders.apply {
                layoutManager =
                    LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
                setHasFixedSize(true)

                mFolderAdapter = BucketAdapter(
                    mActivity, builder, mBucketsList
                ) { _, _, position, _ ->
                    (mActivity as MediaActivity).loadFragment(MediaFragment(), Bundle().apply {
                        putLong(KEY_BUCKET_ID, mBucketsList[position].bucketId)
                        putString(KEY_BUCKET_NAME, mBucketsList[position].name)
                        putString(KEY_BUCKET_PATH, mBucketsList[position].path)
                    }, true)
                }
                adapter = mFolderAdapter
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        if (mIsHandled == 0)
            mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait)
        else if (mIsHandled == 1)
            mHandlerLoadingWait.removeCallbacks(mRunnableLoadingWait1)

        mActivity.unregisterReceiver(mBroadcastReceiver)
        super.onDestroyView()
    }
}