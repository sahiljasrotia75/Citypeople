package com.citypeople.project.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.citypeople.project.R
import com.citypeople.project.databinding.GroupItemLayoutBinding

class GroupListAdapter(var context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // private var nList= mutableListOf<PetInfoEntity>()
    private var mList = mutableListOf<String>()

    fun clearList() {
        this.mList.clear()
        //this.nList.clear()
    }

    fun setDataList(mList: MutableList<String>) {
        this.mList.addAll(mList)
        //  this.nList.addAll(mList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binderObject = DataBindingUtil.inflate<GroupItemLayoutBinding>(
            LayoutInflater.from(parent.context),
            R.layout.group_item_layout,
            parent,
            false
        )
        return ViewHolder(binderObject)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {

        if (viewHolder is ViewHolder) {
            viewHolder.bindingObj.bindingObj = mList[i]
            viewHolder.bindingObj.executePendingBindings()

        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }


    fun updateData(list: ArrayList<String>) {
        mList.clear()
        mList.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }


    inner class ViewHolder(var bindingObj: GroupItemLayoutBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(bindingObj.root)



}

