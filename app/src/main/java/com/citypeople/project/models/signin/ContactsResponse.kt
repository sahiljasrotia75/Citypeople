package com.citypeople.project.models.signin

data class ContactsResponse(
    val users: List<User>
)

data class SelectedUser(var id: Int, var name: String)