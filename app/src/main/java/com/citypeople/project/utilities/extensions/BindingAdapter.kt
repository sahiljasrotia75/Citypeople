package com.citypeople.project.utilities.extensions

import android.R.anim
import android.graphics.Color
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

import java.util.*


@BindingAdapter("animateView")
fun View.animate(boolean: Boolean) {
    val animation: Animation = AnimationUtils.loadAnimation(
        this.context,
        anim.slide_in_left
    )
    this.startAnimation(animation)
}

@BindingAdapter("setError")
fun TextInputLayout.setError(error: String?) {
    this.error = error
    this.isErrorEnabled = error != null
}

fun setNumberAge(age: String): String {
    return if (age == "1") {
        "$age Year"
    } else if (age.length == 1 && age.toInt() > 1) {
        "$age Years"
    } else if (age.length > 1) {
        "$age Years"
    } else {
        "$age Year"
    }
}


@BindingAdapter("setTextColor")
fun TextView.setTextColor(color: String?) {
    if (color.isNullOrEmpty()) return
    if (color.toString() == "#ffffff") setTextColor(Color.parseColor("#979797"))
    else setTextColor(Color.parseColor(color))
}

@BindingAdapter("setSniff")
fun TextView.setSniff(value: Int?) {
    text = if (value == 1){
        "$value Sniff"
    }else{
        "$value Sniffs"
    }
}

@BindingAdapter("setWolf")
fun TextView.setWolf(value: Int?) {
    text = if (value == 1){
        "$value Woof"
    }else{
        "$value Woofs"
    }
}

@BindingAdapter("setDrawableColorFilter")
fun ImageView.setDrawableColorFilter(color: String?) {
    if (color.isNullOrEmpty()) return
    setColorFilter(Color.parseColor(color))
}



@BindingAdapter("setGender")
fun TextView.setGender(type: String?) {
    if (type == "0") this.text = "Male"
    else this.text = "Female"
}


@BindingAdapter("showHideView")
fun View.showHideView(res: Int) {
    visibility = if (res == 0) View.GONE else View.VISIBLE
}

@BindingAdapter("hideShowView")
fun View.hideShowView(res: Int) {
    visibility = if (res == 0) View.VISIBLE else View.GONE
}

@BindingAdapter("makeVisibleOrGone")
fun View.makeVisibleOrGone(value: Boolean) {
    visibility = if (value) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("makeVisibleOrGone")
fun View.makeVisibleAndGone(value: Boolean) {
    visibility = if (value) View.VISIBLE else View.GONE
}

@BindingAdapter("visibility")
fun View.visibility(value: Boolean) {
    visibility = if (value) View.VISIBLE else View.GONE
}

@BindingAdapter("setTextFromData")
fun TextView.setTextFromData(res: Int) {
    text = res.toString()
}







//}
//@BindingAdapter("android:onClick")

