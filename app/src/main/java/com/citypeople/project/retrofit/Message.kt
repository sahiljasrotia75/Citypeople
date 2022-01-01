package com.citypeople.project.retrofit


import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("message")
    val message: String,
    @SerializedName("token_class")
    val tokenClass: String,
    @SerializedName("token_type")
    val tokenType: String
)