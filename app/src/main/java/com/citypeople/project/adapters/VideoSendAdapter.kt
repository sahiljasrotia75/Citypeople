package com.citypeople.project.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.citypeople.project.R
import com.citypeople.project.databinding.VideoSendItemLayoutBinding
import com.citypeople.project.models.signin.User
import java.util.*

class VideoSendAdapter(var listener: VideoSendAdapter.VideoSendItemListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {
    private var nList= mutableListOf<User>()
    private var mList = mutableListOf<User>()

    fun clearList() {
        this.mList.clear()
        this.nList.clear()
    }

    fun setDataList(mList: MutableList<User>) {
        this.mList.addAll(mList)
          this.nList.addAll(mList)
    }
    fun getCurrentItems():MutableList<User>{
        return mList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binderObject = DataBindingUtil.inflate<VideoSendItemLayoutBinding>(
            LayoutInflater.from(parent.context),
            R.layout.video_send_item_layout,
            parent,
            false
        )
        return ViewHolder(binderObject)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {

        if (viewHolder is ViewHolder) {
            viewHolder.bindingObj.bindingObj = mList[i]
            viewHolder.bindingObj.executePendingBindings()

            if (mList[i].is_group == true)
                viewHolder.bindingObj.userImage.setBackgroundResource(R.drawable.add_group)
            else
                viewHolder.bindingObj.userImage.setBackgroundResource(R.drawable.add_friends)

            if (mList[i].isSelected)
                viewHolder.bindingObj.checkBox.setBackgroundResource(R.drawable.check)
            else
                viewHolder.bindingObj.checkBox.setBackgroundResource(R.drawable.unchecked)

            viewHolder.itemView.setOnClickListener {
                listener.onSelectName(mList[i].name, mList[i].id.toString(), mList[i].is_group == true)
                mList[i].isSelected = !mList[i].isSelected
                notifyItemChanged(i)
            }

        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }


    fun updateData() {
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }

    private val filter: Filter = object : Filter() {
        @Override
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<User> = ArrayList()
            if (constraint == null || constraint.isEmpty()) {
                filteredList.addAll(nList)
                listener.onEmptySearch(filteredList.isEmpty())
            } else {
                val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim { it <= ' ' }
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
              mList.addAll(results.values as MutableList<User>)
                notifyDataSetChanged()
        }
    }

    inner class ViewHolder(var bindingObj: VideoSendItemLayoutBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(bindingObj.root)

    interface VideoSendItemListener {
        fun onSelectName(name: String, id: String, isGroup: Boolean)
        fun onEmptySearch(boolean: Boolean)
        fun setOtherFieldTextBreed(isOtherBreedText : String, user: User)
    }

    override fun getFilter(): Filter {
        return filter
    }
}
