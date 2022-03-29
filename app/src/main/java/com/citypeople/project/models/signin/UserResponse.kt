package com.citypeople.project.models.signin

data class UserResponse(
    val status: Boolean,
    val user: User
)
data class User(
    var id: Int = 0,
    var name: String,
    val phone: String,
    val updated_at: String="",
    var isSelected:Boolean = false,
    var is_group: Boolean? = null,
    var is_registered: Boolean = false,
    var request_status: Int = 0,
    var already_friend: Boolean = false,
)