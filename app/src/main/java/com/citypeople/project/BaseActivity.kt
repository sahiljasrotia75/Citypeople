package com.citypeople.project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.citypeople.project.utilities.common.BaseViewModel

abstract class BaseActivity : AppCompatActivity() {

    private var loaderView: View? = null
    var baseViewModel: BaseViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseViewModel = bindViewModel()
        initLoader()
        observeData()
    }

    abstract fun bindViewModel(): BaseViewModel

    private fun observeData() {
        baseViewModel?.loader?.observe(this, {
            when (it) {
                true -> {
                    showLoader(true)
                }
                false -> {
                    showLoader(false)
                }
            }
        })
    }

    fun initLoader() {
        loaderView = LayoutInflater.from(this).inflate(R.layout.layout_loader, null)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        loaderView!!.layoutParams = params

    }

    fun showLoader(aBoolean: Boolean) {
        if (aBoolean) {
            val window = this@BaseActivity.window
            if (window != null && window.decorView != null && window.decorView.rootView != null) {
                if (loaderView!!.parent != null) (window.decorView.rootView as ViewGroup).removeView(loaderView)
                (window.decorView.rootView as ViewGroup).addView(loaderView)
            }
        } else {
            val window = this@BaseActivity.window
            if (window != null && window.decorView != null && window.decorView.rootView != null) {
                (window.decorView.rootView as ViewGroup).removeView(loaderView)
            }
        }

    }

}