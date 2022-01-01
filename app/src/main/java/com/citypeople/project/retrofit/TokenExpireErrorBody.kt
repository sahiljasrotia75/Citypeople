package com.citypeople.project.retrofit


import com.citypeople.project.retrofit.Message
import com.google.gson.annotations.SerializedName

data class TokenExpireErrorBody(
    @SerializedName("code")
    val code: String?,
    @SerializedName("detail")
    val detail: String?,
    @SerializedName("messages")
    val messages: List<Message>?
)