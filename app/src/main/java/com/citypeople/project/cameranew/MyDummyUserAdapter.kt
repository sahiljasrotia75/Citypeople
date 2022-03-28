package com.citypeople.project.cameranew

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.citypeople.project.databinding.ItemDummyUserBinding
import com.citypeople.project.models.signin.DummyUserModel

class MyDummyUserAdapter(
    val requireActivity: FragmentActivity,
    val items: MutableList<DummyUserModel>
): RecyclerView.Adapter<MyDummyUserAdapter.MyDummyViewHolder>() {

    class MyDummyViewHolder(val binding: ItemDummyUserBinding) :
        RecyclerView.ViewHolder(binding.root)



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyDummyViewHolder {
        return MyDummyViewHolder(
            ItemDummyUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: MyDummyViewHolder, position: Int) {
        holder.binding.item = items[position]
        holder.binding.executePendingBindings()
        holder.binding.imageView2.setImageResource(items[position].image)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
