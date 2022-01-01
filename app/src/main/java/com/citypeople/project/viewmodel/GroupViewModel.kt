package com.citypeople.project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.citypeople.project.models.signin.ContactsResponse
import com.citypeople.project.models.signin.FriendResponse
import com.citypeople.project.repo.AuthRepo
import com.citypeople.project.retrofit.ErrorResponse
import com.citypeople.project.retrofit.Resources
import com.citypeople.project.utilities.common.BaseViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

class GroupViewModel(private val authRepo: AuthRepo): BaseViewModel() {
    private val _groupsList= MutableLiveData<Resources<ContactsResponse>>()
    var groupList: LiveData<Resources<ContactsResponse>>?=_groupsList

    private val _addGroup= MutableLiveData<Resources<FriendResponse>>()
    var addGroup: LiveData<Resources<FriendResponse>>?=_addGroup

    fun groupContacts(jsonObject: JSONObject) {
        viewModelScope.launch {
            runCatching {
                _groupsList.postValue(Resources.loading())
                authRepo.groupContactList(jsonObject)
            }.onSuccess{
                _groupsList.postValue(it)
            }.onFailure {
                _groupsList.postValue(
                    Resources.error(
                        ErrorResponse(903,it.message.toString()),null)
                )
            }
        }
    }

    fun addGroup(jsonObject: JSONObject) {
        viewModelScope.launch {
            runCatching {
                _addGroup.postValue(Resources.loading())
                authRepo.addGroup(jsonObject)
            }.onSuccess{
                _addGroup.postValue(it)
            }.onFailure {
                _addGroup.postValue(
                    Resources.error(
                        ErrorResponse(903,it.message.toString()),null)
                )
            }
        }
    }

}