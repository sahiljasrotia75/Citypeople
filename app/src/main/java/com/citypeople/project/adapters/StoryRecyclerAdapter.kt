package com.citypeople.project.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.citypeople.project.R
import com.citypeople.project.adapters.utils.PlayerStateCallback
import com.citypeople.project.adapters.utils.PlayerViewAdapter
import com.citypeople.project.databinding.TiktokTimelineItemRecyclerBinding
import com.citypeople.project.models.signin.StoryModel
import com.citypeople.project.models.signin.User
import com.google.android.exoplayer2.Player
import java.util.*

/**
 * A custom adapter to use with the RecyclerView widget.
 */
class StoryRecyclerAdapter(
    private val mContext: Context,
    private var modelList: MutableList<StoryModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    PlayerStateCallback {
    private var mItemClickListener: OnItemClickListener? = null

    fun updateList(modelList: MutableList<StoryModel>) {
        this.modelList = modelList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): VideoPlayerViewHolder {
        val binding: TiktokTimelineItemRecyclerBinding = DataBindingUtil.inflate(LayoutInflater.from(viewGroup.context)
            , R.layout.tiktok_timeline_item_recycler, viewGroup, false)
        return VideoPlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        //Here you can fill your row view
        if (holder is VideoPlayerViewHolder) {
            val model = getItem(position)
            val genericViewHolder = holder

            // send data to view holder
            genericViewHolder.onBind(model)
        }
    }

    override fun getItemCount(): Int {
        return modelList.size
    }

    fun getItem(position: Int): StoryModel {
        return modelList[position]
    }
    fun getCurrentItems():MutableList<StoryModel>{
        return modelList
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val position = holder.adapterPosition
        PlayerViewAdapter.releaseRecycledPlayers(position)
        super.onViewRecycled(holder)
    }

    fun SetOnItemClickListener(mItemClickListener: OnItemClickListener?) {
        this.mItemClickListener = mItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(
            view: View?,
            position: Int,
            model: StoryModel?
        )
    }

    inner class VideoPlayerViewHolder(private val binding: TiktokTimelineItemRecyclerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(model: StoryModel) {
            // handel on item click
            binding.root.setOnClickListener {
                mItemClickListener!!.onItemClick(it, adapterPosition, model)
            }

            binding.apply {
                dataModel = model
                callback = this@StoryRecyclerAdapter
                index = adapterPosition
                executePendingBindings()
            }
        }
    }

    override fun onVideoDurationRetrieved(duration: Long, player: Player) {
    }

    override fun onVideoBuffering(player: Player) {
    }

    override fun onStartedPlaying(player: Player) {

    }

    override fun onFinishedPlaying(player: Player) {
    }
}