package com.citypeople.project.adapters.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerViewScrollListener : RecyclerView.OnScrollListener() {
    private var firstVisibleItem = 0
    private var visibleItemCount = 0


    @Volatile
    private var mEnabled = true
    private var mPreLoadCount = 0
     var scrollDirection = 0
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (mEnabled) {
            val manager = recyclerView.layoutManager
            require(manager is LinearLayoutManager) { "Expected recyclerview to have linear layout manager" }
            val mLayoutManager = manager
            visibleItemCount = mLayoutManager.childCount
            firstVisibleItem = mLayoutManager.findFirstCompletelyVisibleItemPosition()
            onItemIsFirstVisibleItem(firstVisibleItem)

            if (dx > 0) {
                System.out.println("Scrolled Right");
                scrollDirection=1
            } else if (dx < 0) {
                System.out.println("Scrolled Left");
                scrollDirection= -1
            } else{
                scrollDirection= 0
            }
        }


    }




    /**
     * Called when end of scroll is reached.
     *
     * @param recyclerView - related recycler view.
     */
    abstract fun onItemIsFirstVisibleItem(index: Int)
    fun disableScrollListener() {
        mEnabled = false
    }

    fun enableScrollListener() {
        mEnabled = true
    }

    fun setPreLoadCount(mPreLoadCount: Int) {
        this.mPreLoadCount = mPreLoadCount
    }
}