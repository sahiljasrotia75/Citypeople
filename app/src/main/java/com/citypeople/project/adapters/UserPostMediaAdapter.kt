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
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class UserPostMediaAdapter(
    var listener: ProfileMediaItemListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {
//    private var nList= mutableListOf<StoryModel>()
//    private var mList = mutableListOf<StoryModel>()

    private var toMutableList: MutableList<Int> = ArrayList()
    private var mhashmapStories: HashMap<Int, MutableList<StoryModel>> = HashMap()
    // private var  nhashmapStories: HashMap<Int, MutableList<StoryModel>> = HashMap()

    fun clearList() {
        this.mhashmapStories.clear()
        // this.nList.clear()
    }

    /* fun setDataList(mList: MutableList<StoryModel>,mListStories: MutableList<StoryModel>) {
         this.mList.addAll(mList)
         this.nList.addAll(mList)
     }*/

    fun setDataList(
        toMutableList: MutableList<Int>,
        hashmapStories: HashMap<Int, MutableList<StoryModel>>
    ) {
        this.toMutableList = toMutableList
        this.mhashmapStories = hashmapStories
        //this.nhashmapStories = hashmapStories
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

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, pos: Int) {
        Log.d("position", pos.toString())
        if (viewHolder is ViewHolder) {
            viewHolder.setIsRecyclable(false)
            viewHolder.bindingObj.bindingObj = mhashmapStories[toMutableList[pos]]?.get(0)
            viewHolder.bindingObj.executePendingBindings()
            viewHolder.itemView.setOnClickListener { listener.onMediaThumbnailClick(position = pos) }

            val contentList = mhashmapStories[toMutableList[pos]]
            val size = contentList?.size
            if (size == 1){
                viewHolder.bindingObj.mediaBtn.show()
                viewHolder.bindingObj.mediaBtn.setImageResource(R.drawable.ic_videocamera_icon)
            }else{
                viewHolder.bindingObj.mediaBtn.show()
                viewHolder.bindingObj.mediaBtn.setImageResource(R.drawable.ic_baseline_content_copy_24)
            }
        }
    }


    override fun getItemCount(): Int = toMutableList.size

    /*  private val filter: Filter = object : Filter() {
          @Override
          override fun performFiltering(constraint: CharSequence?): FilterResults {
              var filteredList:  HashMap<Int, MutableList<StoryModel>> = HashMap()
              if (constraint == null || constraint.isEmpty()) {

                  filteredList = nhashmapStories
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
      }*/

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