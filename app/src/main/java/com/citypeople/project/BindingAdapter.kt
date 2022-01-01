package com.citypeople.project

import android.annotation.SuppressLint
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("setError")
fun TextInputLayout.setError(error: String?) {
    this.error = error
    this.isErrorEnabled = error != null
}
