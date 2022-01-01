package com.citypeople.project.viewmodel

import androidx.lifecycle.*
import com.citypeople.project.models.signin.UserResponse
import com.citypeople.project.repo.AuthRepo
import com.citypeople.project.retrofit.ErrorResponse
import com.citypeople.project.retrofit.Resources
import com.citypeople.project.utilities.common.BaseViewModel
import kotlinx.coroutines.launch

class OtpViewModel (private val authRepo: AuthRepo): BaseViewModel() {

    private val _generateUser= MutableLiveData<Resources<UserResponse>>()
    var generateUser: LiveData<Resources<UserResponse>>?=_generateUser

    fun signIn(myPhoneNumber: String?, firstName: String?, lastName: String?) {
        viewModelScope.launch {
            runCatching {
                val map= hashMapOf<String,String>()
                map["phone"]= myPhoneNumber.toString()
                map["name"]= firstName.toString() +" "+ lastName.toString()
                _generateUser.postValue(Resources.loading())
                authRepo.requestUser(map)
            }.onSuccess{
                _generateUser.postValue(it)
            }.onFailure {
                _generateUser.postValue(Resources.error(
                    ErrorResponse(903,it.message.toString()),null)
                )
            }
        }
    }

}


