package com.citypeople.project.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.citypeople.project.R
import com.citypeople.project.databinding.UserPostMediaItemLayoutBinding
import com.citypeople.project.hide
import com.citypeople.project.models.signin.StoryModel
import com.citypeople.project.models.signin.User
import com.citypeople.project.show
import com.citypeople.project.utilities.extensions.MediaConstants
import java.util.*

class UserPostMediaAdapter(
    var listener: ProfileMediaItemListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {
    private var nList= mutableListOf<StoryModel>()
    private var mList = mutableListOf<StoryModel>()

    fun clearList() {
        this.mList.clear()
        this.nList.clear()
    }

    fun setDataList(mList: MutableList<StoryModel>) {
        this.mList.clear()
        this.nList.clear()
        this.mList.addAll(mList)
        this.nList.addAll(mList)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binderObject = DataBindingUtil.inflate<UserPostMediaItemLayoutBinding>(
            LayoutInflater.from(parent.context),
            R.layout.user_post_media_item_layout,
            parent,
            false
        )
        return ViewHolder(binderObject)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        Log.d("position", i.toString())
        if (viewHolder is ViewHolder) {
            viewHolder.setIsRecyclable(false)

           // viewHolder.bindingObj.bindingObj = mList[i]

            mList.mapIndexed { index, user ->
                if (index == 0) {
                    //mMediaList.add(
                 //   val id: Int, val user_id: Int, val name: String, var url: String,val location:String
                    viewHolder.bindingObj.bindingObj = StoryModel(
                        id = user.id,
                        user_id = user.user_id,
                        name = user.name,
                        url = user.url,
                        location = user.location
                    )

                    if (user.url?.isNullOrEmpty()!!)
                        viewHolder.bindingObj.mediaBtn.hide()
                    else if (MediaConstants.videoFiles.contains(getExtension(user.url.toString()))) {
                        viewHolder.bindingObj.mediaBtn.show()
                        viewHolder.bindingObj.mediaBtn.setImageResource(R.drawable.ic_videocamera_icon)
                    } else
                        viewHolder.bindingObj.mediaBtn.hide()

                    viewHolder.bindingObj.executePendingBindings()
                }
                // )
                else {
                    viewHolder.bindingObj.mediaBtn.show()
                    viewHolder.bindingObj.mediaBtn.setImageResource(R.drawable.ic_baseline_content_copy_24)
                    return@mapIndexed
                }
            }

            viewHolder.itemView.setOnClickListener { listener.onMediaThumbnailClick(position = i) }

        }
    }

    fun getExtension(url: String): String {
        if (url.isNotEmpty())
            return url.substring(
                url.lastIndexOf(
                    "."
                )
            )
        return url
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    private val filter: Filter = object : Filter() {
        @Override
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<StoryModel> = ArrayList()
            if (constraint == null || constraint.isEmpty()) {

                filteredList.addAll(nList)
                listener.onEmptySearch(filteredList.isEmpty())
            } else {

                val filterPattern =
                    constraint.toString().toLowerCase(Locale.ROOT).trim { it <= ' ' }
                for (item in nList) {
                    when {
                        item.name.toLowerCase(Locale.ROOT).contains(filterPattern) -> {
                            filteredList.add(item)
                        }
                    }
                    listener.onEmptySearch(filteredList.isEmpty())
                }
            }
            val results = FilterResults()
            results.values = filteredList
            return results
        }

        @Suppress("UNCHECKED_CAST")
        @Override
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            mList.clear()
            mList.addAll(results.values as MutableList<StoryModel>)
            notifyDataSetChanged()
        }
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }


    inner class ViewHolder(var bindingObj: UserPostMediaItemLayoutBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(bindingObj.root)

    interface ProfileMediaItemListener {
        fun onMediaThumbnailClick(position: Int)
        fun onMentionedUserClick(username: String)
        fun onEmptySearch(boolean: Boolean)

    }

    override fun getFilter(): Filter {
        return filter
    }
}