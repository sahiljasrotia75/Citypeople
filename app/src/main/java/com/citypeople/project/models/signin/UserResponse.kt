package com.citypeople.project.models.signin

data class UserResponse(
    val status: Boolean,
    val user: User
)
data class User(
    val created_at: String,
    val id: Int,
    var name: String,
    val phone: String,
    val updated_at: String,
    var isSelected :Boolean = false,
    var is_group : Boolean? = null

)