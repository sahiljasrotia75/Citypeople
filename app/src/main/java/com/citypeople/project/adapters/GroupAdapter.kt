package com.citypeople.project.adapters

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.citypeople.project.R
import com.citypeople.project.databinding.FriendItemLayoutBinding
import com.citypeople.project.databinding.GroupsItemLayoutBinding
import com.citypeople.project.models.signin.User
import com.citypeople.project.views.FriendListener
import com.citypeople.project.views.FriendModel
import kotlinx.android.synthetic.main.friend_item_layout.view.*
import java.util.*

class GroupAdapter(var listener: FriendItemListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    private var nList = mutableListOf<User>()
    private var mList = mutableListOf<User>()

    fun clearList() {
        this.mList.clear()
        this.nList.clear()
    }

    fun setDataList(mList: MutableList<User>) {
        this.mList.addAll(mList)
        this.nList.addAll(mList)
    }

    fun getCurrentItems(): MutableList<User> {
        return mList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binderObject = DataBindingUtil.inflate<GroupsItemLayoutBinding>(
            LayoutInflater.from(parent.context),
            R.layout.groups_item_layout,
            parent,
            false
        )
        return ViewHolder(binderObject)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {

        if (viewHolder is ViewHolder) {

            viewHolder.bindingObj.listener = listener
            viewHolder.bindingObj.bindingObj = mList[i]
            viewHolder.bindingObj.executePendingBindings()
            if (mList[i].isSelected)
           //     viewHolder.bindingObj.txtInvite.visibility = View.GONE
              viewHolder.bindingObj.checkBox.setBackgroundResource(R.drawable.check)
            else
            //    viewHolder.bindingObj.txtInvite.visibility = View.VISIBLE
                 viewHolder.bindingObj.checkBox.setBackgroundResource(R.drawable.unchecked)

            viewHolder.itemView.setOnClickListener {
                mList[i].isSelected = !mList[i].isSelected
                notifyItemChanged(i)
            }

        }
    }

    /*private fun sendSMS(phoneNumber: String, message: String) {
        val sentPI: PendingIntent = PendingIntent.getBroadcast(listener, 0, Intent("SMS_SENT"), 0)
        SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, sentPI, null)
    }*/

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
            mList.addAll(results.values as MutableList<User>)
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(var bindingObj: GroupsItemLayoutBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(bindingObj.root)

    interface FriendItemListener : FriendListener {
        fun onSelectName(breed: String, is_selected: Boolean, is_other: Boolean)
        fun onEmptySearch(boolean: Boolean)
        fun setOtherFieldTextBreed(isOtherBreedText: String, user: User)
        fun onSelection(item: User, position: Int)
        fun invite(item: User, position: Int)

    }

    override fun getFilter(): Filter {
        return filter
    }


}
