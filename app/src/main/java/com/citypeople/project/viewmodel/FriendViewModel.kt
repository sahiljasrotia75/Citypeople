package com.citypeople.project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.citypeople.project.models.signin.ContactsResponse
import com.citypeople.project.models.signin.FriendResponse
import com.citypeople.project.models.signin.UserResponse
import com.citypeople.project.repo.AuthRepo
import com.citypeople.project.retrofit.ErrorResponse
import com.citypeople.project.retrofit.Resources
import com.citypeople.project.utilities.common.BaseViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

class FriendViewModel(private val authRepo: AuthRepo): BaseViewModel() {
    private val _contactsList= MutableLiveData<Resources<ContactsResponse>>()
    var contactsList: LiveData<Resources<ContactsResponse>>?=_contactsList

    private val _addFriend= MutableLiveData<Resources<FriendResponse>>()
    var addFriend: LiveData<Resources<FriendResponse>>?=_addFriend

    fun contacts(jsonObject: JSONObject) {
        viewModelScope.launch {
            runCatching {
                _contactsList.postValue(Resources.loading())
                authRepo.contactList(jsonObject)
            }.onSuccess{
                _contactsList.postValue(it)
            }.onFailure {
                _contactsList.postValue(
                    Resources.error(
                    ErrorResponse(903,it.message.toString()),null)
                )
            }
        }
    }

    fun addFriend(jsonObject: JSONObject) {
        viewModelScope.launch {
            runCatching {
                _addFriend.postValue(Resources.loading())
                authRepo.addFriend(jsonObject)
            }.onSuccess{
                _addFriend.postValue(it)
            }.onFailure {
                _addFriend.postValue(
                    Resources.error(
                        ErrorResponse(903,it.message.toString()),null)
                )
            }
        }
    }

}