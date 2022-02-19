package com.citypeople.project.models.signin

import androidx.recyclerview.widget.DiffUtil

data class StoryDataResponse(val videos: ArrayList<StoryModel>)

data class StoryModel(val id: Int, val user_id: Int, val name: String, var url: String, val location:String)


class CreatePostDiffUtil: DiffUtil.ItemCallback<StoryModel>(){

    override fun areItemsTheSame(oldItem: StoryModel, newItem: StoryModel): Boolean {
        return oldItem.id==newItem.id
    }

    override fun areContentsTheSame(oldItem: StoryModel, newItem: StoryModel): Boolean {
        return oldItem==newItem
    }

}


/*
data class PostsDiffUtils(
    private val oldList:List<StoryModel>,
    private val newList: ArrayList<String>
):DiffUtil.Callback(){
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id==newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

}*/
