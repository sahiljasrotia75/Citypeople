package com.citypeople.project.utilities.extensions

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.URLConnection

fun String.isImageFile(): Boolean {
    val mimeType: String = URLConnection.guessContentTypeFromName(this)
    return mimeType.startsWith("image")
}

 fun RecyclerView.getCurrentPosition() : Int {
     return (this.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
 }





